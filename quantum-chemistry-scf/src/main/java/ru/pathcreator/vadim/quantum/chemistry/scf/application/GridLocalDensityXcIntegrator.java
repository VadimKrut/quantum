/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/**
 * Считает LDA exchange-correlation энергию и матрицу потенциала на atom-centered численной сетке.
 */
public final class GridLocalDensityXcIntegrator {

  private static final double FOUR_PI = 4.0 * Math.PI;
  private static final double SLATER_EXCHANGE_COEFFICIENT =
      -0.75 * Math.pow(3.0 / Math.PI, 1.0 / 3.0);
  private static final double SLATER_POTENTIAL_COEFFICIENT = -Math.pow(3.0 / Math.PI, 1.0 / 3.0);
  private static final double VWN_A = 0.0310907;
  private static final double VWN_B = 3.72744;
  private static final double VWN_C = 12.9352;
  private static final double VWN_X0 = -0.10498;
  private static final double VWN_DERIVATIVE_STEP = 1.0E-5;
  private static final double PBE_KAPPA = 0.804;
  private static final double PBE_MU = 0.2195149727645171;
  private static final double PBE_BETA = 0.06672455060314922;
  private static final double PBE_GAMMA = 0.031090690869654895;
  private static final double B88_BETA = 0.0042;
  private static final double B3LYP_SLATER_EXCHANGE_FRACTION = 0.08;
  private static final double B3LYP_B88_EXCHANGE_CORRECTION_FRACTION = 0.72;
  private static final double B3LYP_VWN_CORRELATION_FRACTION = 0.19;
  private static final double B3LYP_LYP_CORRELATION_FRACTION = 0.81;
  private static final double LYP_A = 0.04918;
  private static final double LYP_B = 0.132;
  private static final double LYP_C = 0.2533;
  private static final double LYP_D = 0.349;
  private static final double MINIMUM_DENSITY = 1.0E-14;

  private final int radialPointCount;
  private final double[][] angularPoints;

  public GridLocalDensityXcIntegrator() {
    this(80, GridLocalDensityXcIntegrator.fibonacciAngularPoints(146));
  }

  public GridLocalDensityXcIntegrator(final int radialPointCount, final double[][] angularPoints) {
    if (radialPointCount < 8) {
      throw new IllegalArgumentException("DFT radial grid point count must be at least 8.");
    }
    if (angularPoints == null || angularPoints.length < 6) {
      throw new IllegalArgumentException("DFT angular grid must contain at least 6 points.");
    }
    this.radialPointCount = radialPointCount;
    this.angularPoints = GridLocalDensityXcIntegrator.copyAngularPoints(angularPoints);
  }

  public GridLocalDensityXcResult integrateSlaterExchange(
      final IntegralMolecule molecule, final GaussianBasisSet basisSet, final double[] density) {
    return this.integrateLocalDensity(molecule, basisSet, density, "LDA_X");
  }

  public GridLocalDensityXcResult integrateLocalDensity(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final double[] density,
      final String functionalName) {
    return this.integrateLocalDensity(molecule, basisSet, density, functionalName, 1.0);
  }

  public GridLocalDensityXcResult integrateLocalDensity(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final double[] density,
      final String functionalName,
      final double semilocalExchangeFraction) {
    if (molecule == null) {
      throw new IllegalArgumentException("DFT grid molecule must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("DFT grid basis set must not be null.");
    }
    final int size = basisSet.orbitalCount();
    if (density == null || density.length != size * size) {
      throw new IllegalArgumentException("DFT grid density matrix size does not match basis.");
    }
    if (!Double.isFinite(semilocalExchangeFraction)
        || semilocalExchangeFraction < 0.0
        || semilocalExchangeFraction > 1.0) {
      throw new IllegalArgumentException(
          "DFT semilocal exchange fraction must be finite and in [0, 1].");
    }
    final boolean includeB3LypCompatible =
        GridLocalDensityXcIntegrator.includesB3LypCompatible(functionalName);
    final boolean includeVwnCorrelation =
        GridLocalDensityXcIntegrator.includesVwnCorrelation(functionalName);
    final boolean includePbe = GridLocalDensityXcIntegrator.includesPbe(functionalName);
    final double[] potential = new double[size * size];
    final double[] values = new double[size];
    final double[] gradientX = new double[size];
    final double[] gradientY = new double[size];
    final double[] gradientZ = new double[size];
    double exchangeEnergy = 0.0;
    double correlationEnergy = 0.0;
    double integratedElectronCount = 0.0;
    final List<IntegralAtom> atoms = molecule.atoms();
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      final IntegralAtom atom = atoms.get(atomIndex);
      for (int radialIndex = 0; radialIndex < this.radialPointCount; ++radialIndex) {
        final double t = ((double) radialIndex + 0.5) / (double) this.radialPointCount;
        final double radius = t / (1.0 - t);
        final double radialWeight =
            radius * radius / ((1.0 - t) * (1.0 - t) * (double) this.radialPointCount);
        for (int angularIndex = 0; angularIndex < this.angularPoints.length; ++angularIndex) {
          final double[] direction = this.angularPoints[angularIndex];
          final double x = atom.xBohr() + radius * direction[0];
          final double y = atom.yBohr() + radius * direction[1];
          final double z = atom.zBohr() + radius * direction[2];
          final double weight =
              FOUR_PI
                  * radialWeight
                  / (double) this.angularPoints.length
                  * this.partitionWeight(molecule, atomIndex, x, y, z);
          this.evaluateBasisValuesAndGradients(
              molecule, basisSet, x, y, z, values, gradientX, gradientY, gradientZ);
          final double rho =
              Math.max(GridLocalDensityXcIntegrator.density(size, density, values), 0.0);
          integratedElectronCount += weight * rho;
          if (rho <= MINIMUM_DENSITY) {
            continue;
          }
          final double rhoOneThird = Math.cbrt(rho);
          final double gradientNorm =
              GridLocalDensityXcIntegrator.densityGradientNorm(
                  size, density, values, gradientX, gradientY, gradientZ);
          final double exchangeEnhancement =
              includePbe
                  ? GridLocalDensityXcIntegrator.pbeExchangeEnhancement(rho, gradientNorm)
                  : 1.0;
          final double ldaExchangeEnergyDensity = SLATER_EXCHANGE_COEFFICIENT * rho * rhoOneThird;
          double exchangeEnergyDensity =
              semilocalExchangeFraction * ldaExchangeEnergyDensity * exchangeEnhancement;
          double exchangeCorrelationPotential =
              semilocalExchangeFraction * SLATER_POTENTIAL_COEFFICIENT * rhoOneThird;
          if (includeB3LypCompatible) {
            final double b88Correction =
                GridLocalDensityXcIntegrator.b88ExchangeCorrectionEnergyDensity(rho, gradientNorm);
            exchangeEnergyDensity =
                B3LYP_SLATER_EXCHANGE_FRACTION * ldaExchangeEnergyDensity
                    + B3LYP_B88_EXCHANGE_CORRECTION_FRACTION * b88Correction;
            exchangeCorrelationPotential =
                B3LYP_SLATER_EXCHANGE_FRACTION * SLATER_POTENTIAL_COEFFICIENT * rhoOneThird
                    + B3LYP_B88_EXCHANGE_CORRECTION_FRACTION
                        * GridLocalDensityXcIntegrator.b88ExchangeCorrectionPotential(
                            rho, gradientNorm);
          }
          exchangeEnergy += weight * exchangeEnergyDensity;
          if (includeB3LypCompatible) {
            final double vwnEnergy =
                GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectron(rho);
            final double vwnPotential = GridLocalDensityXcIntegrator.vwnCorrelationPotential(rho);
            final double lypEnergy =
                GridLocalDensityXcIntegrator.lypCorrelationEnergyPerElectron(rho, gradientNorm);
            final double lypPotential =
                GridLocalDensityXcIntegrator.lypCorrelationPotential(rho, gradientNorm);
            correlationEnergy +=
                weight
                    * rho
                    * (B3LYP_VWN_CORRELATION_FRACTION * vwnEnergy
                        + B3LYP_LYP_CORRELATION_FRACTION * lypEnergy);
            exchangeCorrelationPotential +=
                B3LYP_VWN_CORRELATION_FRACTION * vwnPotential
                    + B3LYP_LYP_CORRELATION_FRACTION * lypPotential;
          } else if (includePbe) {
            final double epsilonCorrelation =
                GridLocalDensityXcIntegrator.pbeCorrelationEnergyPerElectron(rho, gradientNorm);
            final double correlationPotential =
                GridLocalDensityXcIntegrator.pbeCorrelationPotential(rho, gradientNorm);
            correlationEnergy += weight * rho * epsilonCorrelation;
            exchangeCorrelationPotential += correlationPotential;
          } else if (includeVwnCorrelation) {
            final double epsilonCorrelation =
                GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectron(rho);
            final double correlationPotential =
                GridLocalDensityXcIntegrator.vwnCorrelationPotential(rho);
            correlationEnergy += weight * rho * epsilonCorrelation;
            exchangeCorrelationPotential += correlationPotential;
          }
          GridLocalDensityXcIntegrator.addPotential(
              size, potential, values, weight * exchangeCorrelationPotential);
        }
      }
    }
    return GridLocalDensityXcResult.of(
        exchangeEnergy, correlationEnergy, integratedElectronCount, potential);
  }

  private static boolean includesVwnCorrelation(final String functionalName) {
    if (functionalName == null) {
      return false;
    }
    final String normalized =
        functionalName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    return normalized.equals("svwn") || normalized.equals("vwn") || normalized.equals("lda");
  }

  private static boolean includesPbe(final String functionalName) {
    if (functionalName == null) {
      return false;
    }
    final String normalized =
        functionalName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    return normalized.equals("pbe") || normalized.equals("pbe0") || normalized.equals("pbeh");
  }

  private static boolean includesB3LypCompatible(final String functionalName) {
    if (functionalName == null) {
      return false;
    }
    final String normalized =
        functionalName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    return normalized.equals("b3lyp");
  }

  private static double pbeExchangeEnhancement(final double density, final double gradientNorm) {
    final double reducedGradient =
        GridLocalDensityXcIntegrator.reducedExchangeGradient(density, gradientNorm);
    final double argument = PBE_MU * reducedGradient * reducedGradient / PBE_KAPPA;
    return 1.0 + PBE_KAPPA - PBE_KAPPA / (1.0 + argument);
  }

  private static double b88ExchangeCorrectionPotential(
      final double density, final double gradientNorm) {
    final double step = Math.max(1.0E-6, density * 1.0E-5);
    final double lowDensity = Math.max(MINIMUM_DENSITY, density - step);
    final double highDensity = density + step;
    final double lowEnergy =
        GridLocalDensityXcIntegrator.b88ExchangeCorrectionEnergyDensity(lowDensity, gradientNorm);
    final double highEnergy =
        GridLocalDensityXcIntegrator.b88ExchangeCorrectionEnergyDensity(highDensity, gradientNorm);
    return (highEnergy - lowEnergy) / (highDensity - lowDensity);
  }

  private static double b88ExchangeCorrectionEnergyDensity(
      final double density, final double gradientNorm) {
    final double safeDensity = Math.max(density, MINIMUM_DENSITY);
    final double densityFourThirds = safeDensity * Math.cbrt(safeDensity);
    final double reducedGradient = gradientNorm / Math.max(densityFourThirds, MINIMUM_DENSITY);
    final double denominator =
        1.0
            + 6.0
                * B88_BETA
                * reducedGradient
                * GridLocalDensityXcIntegrator.inverseHyperbolicSine(reducedGradient);
    return -B88_BETA * densityFourThirds * reducedGradient * reducedGradient / denominator;
  }

  private static double lypCorrelationPotential(final double density, final double gradientNorm) {
    final double step = Math.max(1.0E-6, density * 1.0E-5);
    final double lowDensity = Math.max(MINIMUM_DENSITY, density - step);
    final double highDensity = density + step;
    final double lowEnergy =
        lowDensity
            * GridLocalDensityXcIntegrator.lypCorrelationEnergyPerElectron(
                lowDensity, gradientNorm);
    final double highEnergy =
        highDensity
            * GridLocalDensityXcIntegrator.lypCorrelationEnergyPerElectron(
                highDensity, gradientNorm);
    return (highEnergy - lowEnergy) / (highDensity - lowDensity);
  }

  private static double lypCorrelationEnergyPerElectron(
      final double density, final double gradientNorm) {
    final double safeDensity = Math.max(density, MINIMUM_DENSITY);
    final double densityMinusOneThird = 1.0 / Math.cbrt(safeDensity);
    final double localTerm = -LYP_A / (1.0 + LYP_D * densityMinusOneThird);
    final double damping = Math.exp(-LYP_C * densityMinusOneThird);
    final double gradientDenominator = Math.max(Math.pow(safeDensity, 8.0 / 3.0), MINIMUM_DENSITY);
    final double gradientTerm =
        -LYP_B * damping * gradientNorm * gradientNorm / gradientDenominator;
    return localTerm + gradientTerm;
  }

  private static double inverseHyperbolicSine(final double value) {
    return Math.log(value + Math.sqrt(value * value + 1.0));
  }

  private static double pbeCorrelationPotential(final double density, final double gradientNorm) {
    final double step = Math.max(1.0E-6, density * 1.0E-5);
    final double lowDensity = Math.max(MINIMUM_DENSITY, density - step);
    final double highDensity = density + step;
    final double lowEnergy =
        lowDensity
            * GridLocalDensityXcIntegrator.pbeCorrelationEnergyPerElectron(
                lowDensity, gradientNorm);
    final double highEnergy =
        highDensity
            * GridLocalDensityXcIntegrator.pbeCorrelationEnergyPerElectron(
                highDensity, gradientNorm);
    return (highEnergy - lowEnergy) / (highDensity - lowDensity);
  }

  private static double pbeCorrelationEnergyPerElectron(
      final double density, final double gradientNorm) {
    final double ldaCorrelation =
        GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectron(density);
    final double reducedGradient =
        GridLocalDensityXcIntegrator.reducedCorrelationGradient(density, gradientNorm);
    final double denominator = Math.exp(-ldaCorrelation / PBE_GAMMA) - 1.0;
    final double a = denominator <= 1.0E-14 ? 0.0 : (PBE_BETA / PBE_GAMMA) / denominator;
    final double t2 = reducedGradient * reducedGradient;
    final double numerator = (PBE_BETA / PBE_GAMMA) * t2 * (1.0 + a * t2);
    final double pbeDenominator = 1.0 + a * t2 + a * a * t2 * t2;
    return ldaCorrelation + PBE_GAMMA * Math.log1p(numerator / pbeDenominator);
  }

  private static double reducedExchangeGradient(final double density, final double gradientNorm) {
    final double fermiWaveVector =
        Math.cbrt(3.0 * Math.PI * Math.PI * Math.max(density, MINIMUM_DENSITY));
    return gradientNorm / (2.0 * fermiWaveVector * Math.max(density, MINIMUM_DENSITY));
  }

  private static double reducedCorrelationGradient(
      final double density, final double gradientNorm) {
    final double fermiWaveVector =
        Math.cbrt(3.0 * Math.PI * Math.PI * Math.max(density, MINIMUM_DENSITY));
    final double screeningWaveVector = Math.sqrt(4.0 * fermiWaveVector / Math.PI);
    return gradientNorm / (2.0 * screeningWaveVector * Math.max(density, MINIMUM_DENSITY));
  }

  private static double vwnCorrelationPotential(final double density) {
    final double radius = GridLocalDensityXcIntegrator.wignerSeitzRadius(density);
    final double step = Math.max(VWN_DERIVATIVE_STEP, Math.abs(radius) * VWN_DERIVATIVE_STEP);
    final double lowRadius = Math.max(1.0E-8, radius - step);
    final double highRadius = radius + step;
    final double lowEnergy =
        GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectronRadius(lowRadius);
    final double highEnergy =
        GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectronRadius(highRadius);
    final double derivative = (highEnergy - lowEnergy) / (highRadius - lowRadius);
    final double energy =
        GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectronRadius(radius);
    return energy - radius * derivative / 3.0;
  }

  private static double vwnCorrelationEnergyPerElectron(final double density) {
    return GridLocalDensityXcIntegrator.vwnCorrelationEnergyPerElectronRadius(
        GridLocalDensityXcIntegrator.wignerSeitzRadius(density));
  }

  private static double vwnCorrelationEnergyPerElectronRadius(final double radius) {
    final double x = Math.sqrt(radius);
    final double q = Math.sqrt(4.0 * VWN_C - VWN_B * VWN_B);
    final double xFunction = x * x + VWN_B * x + VWN_C;
    final double x0Function = VWN_X0 * VWN_X0 + VWN_B * VWN_X0 + VWN_C;
    final double atan = Math.atan(q / (2.0 * x + VWN_B));
    final double logarithm = Math.log((x * x) / xFunction);
    final double poleLogarithm = Math.log(((x - VWN_X0) * (x - VWN_X0)) / xFunction);
    return VWN_A
        * (logarithm
            + 2.0 * VWN_B * atan / q
            - VWN_B
                * VWN_X0
                / x0Function
                * (poleLogarithm + 2.0 * (VWN_B + 2.0 * VWN_X0) * atan / q));
  }

  private static double wignerSeitzRadius(final double density) {
    return Math.cbrt(3.0 / (4.0 * Math.PI * Math.max(density, MINIMUM_DENSITY)));
  }

  private double partitionWeight(
      final IntegralMolecule molecule,
      final int atomIndex,
      final double x,
      final double y,
      final double z) {
    double selected = 0.0;
    double total = 0.0;
    final List<IntegralAtom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final IntegralAtom atom = atoms.get(i);
      final double dx = x - atom.xBohr();
      final double dy = y - atom.yBohr();
      final double dz = z - atom.zBohr();
      final double distanceSquared = dx * dx + dy * dy + dz * dz;
      final double value = Math.exp(-distanceSquared);
      total += value;
      if (i == atomIndex) {
        selected = value;
      }
    }
    if (total <= 0.0) {
      return 1.0 / (double) atoms.size();
    }
    return selected / total;
  }

  private void evaluateBasisValuesAndGradients(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final double x,
      final double y,
      final double z,
      final double[] values,
      final double[] gradientX,
      final double[] gradientY,
      final double[] gradientZ) {
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    for (int orbitalIndex = 0; orbitalIndex < orbitals.size(); ++orbitalIndex) {
      final ContractedGaussianOrbital orbital = orbitals.get(orbitalIndex);
      final IntegralAtom atom = molecule.atoms().get(orbital.atomIndex());
      final double dx = x - atom.xBohr();
      final double dy = y - atom.yBohr();
      final double dz = z - atom.zBohr();
      final GaussianAngularMomentum angularMomentum = orbital.angularMomentum();
      final double polynomial =
          GridLocalDensityXcIntegrator.power(dx, angularMomentum.xPower())
              * GridLocalDensityXcIntegrator.power(dy, angularMomentum.yPower())
              * GridLocalDensityXcIntegrator.power(dz, angularMomentum.zPower());
      final double polynomialX =
          GridLocalDensityXcIntegrator.polynomialDerivative(
              dx,
              dy,
              dz,
              angularMomentum.xPower(),
              angularMomentum.yPower(),
              angularMomentum.zPower(),
              0);
      final double polynomialY =
          GridLocalDensityXcIntegrator.polynomialDerivative(
              dx,
              dy,
              dz,
              angularMomentum.xPower(),
              angularMomentum.yPower(),
              angularMomentum.zPower(),
              1);
      final double polynomialZ =
          GridLocalDensityXcIntegrator.polynomialDerivative(
              dx,
              dy,
              dz,
              angularMomentum.xPower(),
              angularMomentum.yPower(),
              angularMomentum.zPower(),
              2);
      double value = 0.0;
      double derivativeX = 0.0;
      double derivativeY = 0.0;
      double derivativeZ = 0.0;
      final double radiusSquared = dx * dx + dy * dy + dz * dz;
      final List<GaussianPrimitive> primitives = orbital.primitives();
      for (int primitiveIndex = 0; primitiveIndex < primitives.size(); ++primitiveIndex) {
        final GaussianPrimitive primitive = primitives.get(primitiveIndex);
        final double gaussian =
            primitive.coefficient() * Math.exp(-primitive.exponent() * radiusSquared);
        value += gaussian;
        final double derivativeFactor = -2.0 * primitive.exponent() * gaussian;
        derivativeX += derivativeFactor * dx;
        derivativeY += derivativeFactor * dy;
        derivativeZ += derivativeFactor * dz;
      }
      values[orbitalIndex] = value * polynomial;
      gradientX[orbitalIndex] = derivativeX * polynomial + value * polynomialX;
      gradientY[orbitalIndex] = derivativeY * polynomial + value * polynomialY;
      gradientZ[orbitalIndex] = derivativeZ * polynomial + value * polynomialZ;
    }
  }

  private static double density(final int size, final double[] density, final double[] values) {
    double result = 0.0;
    for (int p = 0; p < size; ++p) {
      final double first = values[p];
      if (first == 0.0) {
        continue;
      }
      for (int q = 0; q < size; ++q) {
        result += density[p * size + q] * first * values[q];
      }
    }
    return result;
  }

  private static double densityGradientNorm(
      final int size,
      final double[] density,
      final double[] values,
      final double[] gradientX,
      final double[] gradientY,
      final double[] gradientZ) {
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        final double densityValue = density[p * size + q];
        x += densityValue * (gradientX[p] * values[q] + values[p] * gradientX[q]);
        y += densityValue * (gradientY[p] * values[q] + values[p] * gradientY[q]);
        z += densityValue * (gradientZ[p] * values[q] + values[p] * gradientZ[q]);
      }
    }
    return Math.sqrt(x * x + y * y + z * z);
  }

  private static void addPotential(
      final int size, final double[] potential, final double[] values, final double factor) {
    for (int p = 0; p < size; ++p) {
      final double first = values[p];
      if (first == 0.0) {
        continue;
      }
      for (int q = 0; q < size; ++q) {
        potential[p * size + q] += factor * first * values[q];
      }
    }
  }

  private static double power(final double value, final int power) {
    if (power == 0) {
      return 1.0;
    }
    if (power == 1) {
      return value;
    }
    double result = 1.0;
    for (int i = 0; i < power; ++i) {
      result *= value;
    }
    return result;
  }

  private static double polynomialDerivative(
      final double x,
      final double y,
      final double z,
      final int xPower,
      final int yPower,
      final int zPower,
      final int axis) {
    if (axis == 0) {
      return xPower
          * GridLocalDensityXcIntegrator.power(x, xPower - 1)
          * GridLocalDensityXcIntegrator.power(y, yPower)
          * GridLocalDensityXcIntegrator.power(z, zPower);
    }
    if (axis == 1) {
      return yPower
          * GridLocalDensityXcIntegrator.power(x, xPower)
          * GridLocalDensityXcIntegrator.power(y, yPower - 1)
          * GridLocalDensityXcIntegrator.power(z, zPower);
    }
    return zPower
        * GridLocalDensityXcIntegrator.power(x, xPower)
        * GridLocalDensityXcIntegrator.power(y, yPower)
        * GridLocalDensityXcIntegrator.power(z, zPower - 1);
  }

  private static double[][] fibonacciAngularPoints(final int pointCount) {
    if (pointCount < 6) {
      throw new IllegalArgumentException("Fibonacci angular grid must contain at least 6 points.");
    }
    final double[][] points = new double[pointCount][3];
    final double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
    for (int i = 0; i < pointCount; ++i) {
      final double y = 1.0 - 2.0 * ((double) i + 0.5) / (double) pointCount;
      final double radius = Math.sqrt(Math.max(0.0, 1.0 - y * y));
      final double angle = goldenAngle * (double) i;
      points[i][0] = Math.cos(angle) * radius;
      points[i][1] = y;
      points[i][2] = Math.sin(angle) * radius;
    }
    return points;
  }

  private static double[][] copyAngularPoints(final double[][] angularPoints) {
    final double[][] result = new double[angularPoints.length][3];
    for (int i = 0; i < angularPoints.length; ++i) {
      if (angularPoints[i] == null || angularPoints[i].length != 3) {
        throw new IllegalArgumentException("DFT angular grid point must have three coordinates.");
      }
      final double x = angularPoints[i][0];
      final double y = angularPoints[i][1];
      final double z = angularPoints[i][2];
      final double norm = Math.sqrt(x * x + y * y + z * z);
      if (!Double.isFinite(norm) || norm <= 0.0) {
        throw new IllegalArgumentException("DFT angular grid point must be finite and non-zero.");
      }
      result[i][0] = x / norm;
      result[i][1] = y / norm;
      result[i][2] = z / norm;
    }
    return result;
  }
}
