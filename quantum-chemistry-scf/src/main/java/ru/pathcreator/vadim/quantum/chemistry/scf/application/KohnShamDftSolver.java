/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalFamily;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamFunctionalFamily;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;

/** Closed-shell self-consistent Kohn-Sham DFT solver for AO integral tensors. */
public final class KohnShamDftSolver {

  private static final double LDA_EXCHANGE_SCALE = 0.0300;
  private static final double GGA_EXCHANGE_SCALE = 0.0270;
  private static final double HYBRID_EXCHANGE_SCALE = 0.0235;
  private static final double CORRELATION_SCALE = 0.0040;

  private final JacobiSymmetricEigenSolver eigenSolver;
  private final GridLocalDensityXcIntegrator gridXcIntegrator;

  public KohnShamDftSolver() {
    this(new JacobiSymmetricEigenSolver(), new GridLocalDensityXcIntegrator());
  }

  public KohnShamDftSolver(
      final JacobiSymmetricEigenSolver eigenSolver,
      final GridLocalDensityXcIntegrator gridXcIntegrator) {
    if (eigenSolver == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT eigen solver must not be null.");
    }
    if (gridXcIntegrator == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT grid XC integrator must not be null.");
    }
    this.eigenSolver = eigenSolver;
    this.gridXcIntegrator = gridXcIntegrator;
  }

  public KohnShamDftResult solve(
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final String functionalName) {
    return this.solve(
        tensors,
        electronCount,
        DensityFunctionalDefinition.resolve(functionalName),
        KohnShamDftOptions.DEFAULT);
  }

  public KohnShamDftResult solve(
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final String functionalName,
      final KohnShamDftOptions options) {
    return this.solve(
        tensors, electronCount, DensityFunctionalDefinition.resolve(functionalName), options);
  }

  public KohnShamDftResult solve(
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final DensityFunctionalDefinition functionalDefinition) {
    return this.solve(tensors, electronCount, functionalDefinition, KohnShamDftOptions.DEFAULT);
  }

  public KohnShamDftResult solve(
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final DensityFunctionalDefinition functionalDefinition,
      final KohnShamDftOptions options) {
    return this.solve(null, null, tensors, electronCount, functionalDefinition, options);
  }

  public KohnShamDftResult solve(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final String functionalName,
      final KohnShamDftOptions options) {
    return this.solve(
        molecule,
        basisSet,
        tensors,
        electronCount,
        DensityFunctionalDefinition.resolve(functionalName),
        options);
  }

  public KohnShamDftResult solve(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final DensityFunctionalDefinition functionalDefinition,
      final KohnShamDftOptions options) {
    if (tensors == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT tensors must not be null.");
    }
    if (functionalDefinition == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT functional definition must not be null.");
    }
    final KohnShamDftOptions checkedOptions =
        options == null ? KohnShamDftOptions.DEFAULT : options;
    if (electronCount <= 0 || (electronCount & 1) != 0) {
      throw new IllegalArgumentException("Kohn-Sham DFT electron count must be positive and even.");
    }
    final int size = tensors.orbitalCount();
    final int occupiedCount = electronCount / 2;
    if (occupiedCount > size) {
      throw new IllegalArgumentException(
          "Kohn-Sham DFT occupied orbital count exceeds basis size.");
    }
    final KohnShamFunctionalFamily family =
        KohnShamDftSolver.functionalFamily(functionalDefinition);
    final double exactExchangeFraction = functionalDefinition.exactExchangeFraction();
    final double[] overlap = tensors.overlapCopy();
    final double[] core = tensors.coreHamiltonianCopy();
    final double[] orthogonalizer = this.symmetricOrthogonalizer(overlap, size);
    final boolean useGridXc =
        molecule != null
            && basisSet != null
            && basisSet.orbitalCount() == size
            && KohnShamDftSolver.gridFunctionalSupported(functionalDefinition);
    double[] density = new double[size * size];
    double[] ksMatrix = core.clone();
    double previousEnergy = Double.POSITIVE_INFINITY;
    double electronicEnergy = 0.0;
    double exchangeEnergy = 0.0;
    double correlationEnergy = 0.0;
    double exchangeCorrelationEnergy = 0.0;
    double densityTrace = 0.0;
    double xcGridIntegratedElectronCount = 0.0;
    double[] coefficients = DenseSquareMatrix.identity(size).copyValues();
    double[] orbitalEnergies = new double[size];
    boolean converged = false;
    int iterations = 0;
    final ArrayList<ScfIterationRecord> iterationHistory = new ArrayList<ScfIterationRecord>();
    for (int iteration = 0; iteration < checkedOptions.maxIterations(); ++iteration) {
      iterations = iteration + 1;
      final double[] localXcPotential = new double[size * size];
      final double[] coulombExchangePotential = new double[size * size];
      KohnShamDftSolver.buildCoulombExchangePotential(
          size,
          tensors::electronRepulsionAt,
          density,
          exactExchangeFraction,
          coulombExchangePotential);
      final double[] xcEnergies =
          this.buildXcPotential(
              molecule,
              basisSet,
              size,
              overlap,
              density,
              family,
              functionalDefinition,
              useGridXc,
              localXcPotential);
      KohnShamDftSolver.buildKohnShamMatrix(
          core, coulombExchangePotential, localXcPotential, ksMatrix);
      final double[] transformed =
          KohnShamDftSolver.transformSymmetric(size, orthogonalizer, ksMatrix);
      final SymmetricEigenDecomposition decomposition =
          this.eigenSolver.solve(DenseSquareMatrix.of(size, transformed));
      orbitalEnergies = decomposition.eigenvaluesCopy();
      coefficients =
          KohnShamDftSolver.multiply(size, orthogonalizer, decomposition.eigenvectorsCopy());
      final double[] nextDensity =
          KohnShamDftSolver.buildDensity(size, occupiedCount, coefficients);
      if (checkedOptions.densityMixing() > 0.0 && iteration > 0) {
        KohnShamDftSolver.mixDensity(density, nextDensity, checkedOptions.densityMixing());
      }
      densityTrace = KohnShamDftSolver.densityTrace(size, nextDensity, overlap);
      exchangeEnergy = xcEnergies[0];
      correlationEnergy = xcEnergies[1];
      xcGridIntegratedElectronCount = xcEnergies[2];
      exchangeCorrelationEnergy = exchangeEnergy + correlationEnergy;
      electronicEnergy =
          KohnShamDftSolver.electronicEnergy(
              size,
              nextDensity,
              core,
              tensors::electronRepulsionAt,
              exactExchangeFraction,
              exchangeCorrelationEnergy);
      final double deltaEnergy = Math.abs(electronicEnergy - previousEnergy);
      final double densityChange = KohnShamDftSolver.maxAbsoluteDifference(density, nextDensity);
      final boolean iterationConverged =
          deltaEnergy <= checkedOptions.convergenceTolerance()
              && densityChange <= checkedOptions.densityTolerance();
      iterationHistory.add(
          ScfIterationRecord.of(
              iterations,
              electronicEnergy,
              KohnShamDftSolver.finiteDelta(deltaEnergy),
              densityChange,
              iterationConverged));
      density = nextDensity;
      if (iterationConverged) {
        converged = true;
        break;
      }
      previousEnergy = electronicEnergy;
    }
    return KohnShamDftResult.of(
        converged,
        iterations,
        family,
        electronicEnergy,
        electronicEnergy + tensors.nuclearRepulsionEnergy(),
        exchangeCorrelationEnergy,
        exchangeEnergy,
        correlationEnergy,
        exactExchangeFraction,
        densityTrace,
        xcGridIntegratedElectronCount,
        orbitalEnergies,
        coefficients,
        density,
        List.copyOf(iterationHistory));
  }

  public KohnShamDftResult solveUnrestricted(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final int spinMultiplicity,
      final DensityFunctionalDefinition functionalDefinition,
      final KohnShamDftOptions options) {
    if (tensors == null) {
      throw new IllegalArgumentException("Unrestricted Kohn-Sham DFT tensors must not be null.");
    }
    if (functionalDefinition == null) {
      throw new IllegalArgumentException(
          "Unrestricted Kohn-Sham DFT functional definition must not be null.");
    }
    final KohnShamDftOptions checkedOptions =
        options == null ? KohnShamDftOptions.DEFAULT : options;
    final int size = tensors.orbitalCount();
    final int[] spinCounts =
        KohnShamDftSolver.spinElectronCounts(electronCount, spinMultiplicity, size);
    final int alphaElectronCount = spinCounts[0];
    final int betaElectronCount = spinCounts[1];
    final KohnShamFunctionalFamily family =
        KohnShamDftSolver.functionalFamily(functionalDefinition);
    final double exactExchangeFraction = functionalDefinition.exactExchangeFraction();
    final double[] overlap = tensors.overlapCopy();
    final double[] core = tensors.coreHamiltonianCopy();
    final double[] orthogonalizer = this.symmetricOrthogonalizer(overlap, size);
    final boolean useGridXc =
        molecule != null
            && basisSet != null
            && basisSet.orbitalCount() == size
            && KohnShamDftSolver.gridFunctionalSupported(functionalDefinition);
    double[] alphaDensity = new double[size * size];
    double[] betaDensity = new double[size * size];
    double[] totalDensity = new double[size * size];
    double[] alphaMatrix = core.clone();
    double[] betaMatrix = core.clone();
    double previousEnergy = Double.POSITIVE_INFINITY;
    double electronicEnergy = 0.0;
    double exchangeEnergy = 0.0;
    double correlationEnergy = 0.0;
    double exchangeCorrelationEnergy = 0.0;
    double densityTrace = 0.0;
    double xcGridIntegratedElectronCount = 0.0;
    double[] alphaCoefficients = DenseSquareMatrix.identity(size).copyValues();
    double[] betaCoefficients = DenseSquareMatrix.identity(size).copyValues();
    double[] alphaOrbitalEnergies = new double[size];
    double[] betaOrbitalEnergies = new double[size];
    boolean converged = false;
    int iterations = 0;
    final ArrayList<ScfIterationRecord> iterationHistory = new ArrayList<ScfIterationRecord>();
    for (int iteration = 0; iteration < checkedOptions.maxIterations(); ++iteration) {
      iterations = iteration + 1;
      KohnShamDftSolver.sumDensity(alphaDensity, betaDensity, totalDensity);
      final double[] localXcPotential = new double[size * size];
      final double[] alphaCoulombExchangePotential = new double[size * size];
      final double[] betaCoulombExchangePotential = new double[size * size];
      KohnShamDftSolver.buildSpinCoulombExchangePotential(
          size,
          tensors::electronRepulsionAt,
          totalDensity,
          alphaDensity,
          exactExchangeFraction,
          alphaCoulombExchangePotential);
      KohnShamDftSolver.buildSpinCoulombExchangePotential(
          size,
          tensors::electronRepulsionAt,
          totalDensity,
          betaDensity,
          exactExchangeFraction,
          betaCoulombExchangePotential);
      final double[] xcEnergies =
          this.buildXcPotential(
              molecule,
              basisSet,
              size,
              overlap,
              totalDensity,
              family,
              functionalDefinition,
              useGridXc,
              localXcPotential);
      KohnShamDftSolver.buildKohnShamMatrix(
          core, alphaCoulombExchangePotential, localXcPotential, alphaMatrix);
      KohnShamDftSolver.buildKohnShamMatrix(
          core, betaCoulombExchangePotential, localXcPotential, betaMatrix);
      final SymmetricEigenDecomposition alphaDecomposition =
          this.eigenSolver.solve(
              DenseSquareMatrix.of(
                  size, KohnShamDftSolver.transformSymmetric(size, orthogonalizer, alphaMatrix)));
      final SymmetricEigenDecomposition betaDecomposition =
          this.eigenSolver.solve(
              DenseSquareMatrix.of(
                  size, KohnShamDftSolver.transformSymmetric(size, orthogonalizer, betaMatrix)));
      alphaOrbitalEnergies = alphaDecomposition.eigenvaluesCopy();
      betaOrbitalEnergies = betaDecomposition.eigenvaluesCopy();
      alphaCoefficients =
          KohnShamDftSolver.multiply(size, orthogonalizer, alphaDecomposition.eigenvectorsCopy());
      betaCoefficients =
          KohnShamDftSolver.multiply(size, orthogonalizer, betaDecomposition.eigenvectorsCopy());
      final double[] nextAlphaDensity =
          KohnShamDftSolver.buildSpinDensity(size, alphaElectronCount, alphaCoefficients);
      final double[] nextBetaDensity =
          KohnShamDftSolver.buildSpinDensity(size, betaElectronCount, betaCoefficients);
      if (checkedOptions.densityMixing() > 0.0 && iteration > 0) {
        KohnShamDftSolver.mixDensity(
            alphaDensity, nextAlphaDensity, checkedOptions.densityMixing());
        KohnShamDftSolver.mixDensity(betaDensity, nextBetaDensity, checkedOptions.densityMixing());
      }
      final double[] nextTotalDensity = new double[size * size];
      KohnShamDftSolver.sumDensity(nextAlphaDensity, nextBetaDensity, nextTotalDensity);
      densityTrace = KohnShamDftSolver.densityTrace(size, nextTotalDensity, overlap);
      exchangeEnergy = xcEnergies[0];
      correlationEnergy = xcEnergies[1];
      xcGridIntegratedElectronCount = xcEnergies[2];
      exchangeCorrelationEnergy = exchangeEnergy + correlationEnergy;
      electronicEnergy =
          KohnShamDftSolver.unrestrictedElectronicEnergy(
              size,
              nextAlphaDensity,
              nextBetaDensity,
              nextTotalDensity,
              core,
              tensors::electronRepulsionAt,
              exactExchangeFraction,
              exchangeCorrelationEnergy);
      final double deltaEnergy = Math.abs(electronicEnergy - previousEnergy);
      final double densityChange =
          Math.max(
              KohnShamDftSolver.maxAbsoluteDifference(alphaDensity, nextAlphaDensity),
              KohnShamDftSolver.maxAbsoluteDifference(betaDensity, nextBetaDensity));
      final boolean iterationConverged =
          deltaEnergy <= checkedOptions.convergenceTolerance()
              && densityChange <= checkedOptions.densityTolerance();
      iterationHistory.add(
          ScfIterationRecord.of(
              iterations,
              electronicEnergy,
              KohnShamDftSolver.finiteDelta(deltaEnergy),
              densityChange,
              iterationConverged));
      alphaDensity = nextAlphaDensity;
      betaDensity = nextBetaDensity;
      totalDensity = nextTotalDensity;
      if (iterationConverged) {
        converged = true;
        break;
      }
      previousEnergy = electronicEnergy;
    }
    return KohnShamDftResult.unrestricted(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        family,
        electronicEnergy,
        electronicEnergy + tensors.nuclearRepulsionEnergy(),
        exchangeCorrelationEnergy,
        exchangeEnergy,
        correlationEnergy,
        exactExchangeFraction,
        densityTrace,
        xcGridIntegratedElectronCount,
        alphaOrbitalEnergies,
        betaOrbitalEnergies,
        alphaCoefficients,
        betaCoefficients,
        alphaDensity,
        betaDensity,
        List.copyOf(iterationHistory));
  }

  private double[] buildXcPotential(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final int size,
      final double[] overlap,
      final double[] density,
      final KohnShamFunctionalFamily family,
      final DensityFunctionalDefinition functionalDefinition,
      final boolean useGridXc,
      final double[] potential) {
    if (useGridXc) {
      final GridLocalDensityXcResult result =
          this.gridXcIntegrator.integrateLocalDensity(
              molecule,
              basisSet,
              density,
              functionalDefinition.name(),
              1.0 - functionalDefinition.exactExchangeFraction());
      final double[] gridPotential = result.potentialMatrixCopy();
      System.arraycopy(gridPotential, 0, potential, 0, potential.length);
      return new double[] {
        result.exchangeEnergy(), result.correlationEnergy(), result.integratedElectronCount()
      };
    }
    final double[] localXcEnergies =
        KohnShamDftSolver.buildLocalXcPotential(size, overlap, density, family, potential);
    return new double[] {
      localXcEnergies[0], localXcEnergies[1], KohnShamDftSolver.densityTrace(size, density, overlap)
    };
  }

  private double[] symmetricOrthogonalizer(final double[] overlap, final int size) {
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(size, overlap));
    final double[] vectors = decomposition.eigenvectorsCopy();
    final double[] scaled = new double[size * size];
    for (int column = 0; column < size; ++column) {
      final double eigenvalue = decomposition.eigenvalue(column);
      if (eigenvalue <= 1.0E-12) {
        throw new IllegalArgumentException("Overlap matrix is not positive definite.");
      }
      final double factor = 1.0 / Math.sqrt(eigenvalue);
      for (int row = 0; row < size; ++row) {
        scaled[row * size + column] = vectors[row * size + column] * factor;
      }
    }
    return KohnShamDftSolver.multiplyTransposedRight(size, scaled, vectors);
  }

  private static KohnShamFunctionalFamily functionalFamily(
      final DensityFunctionalDefinition functionalDefinition) {
    final DensityFunctionalFamily family = functionalDefinition.family();
    if (family == DensityFunctionalFamily.HYBRID_GGA
        || family == DensityFunctionalFamily.HYBRID_META_GGA
        || family == DensityFunctionalFamily.RANGE_SEPARATED_HYBRID
        || family == DensityFunctionalFamily.DOUBLE_HYBRID) {
      return KohnShamFunctionalFamily.HYBRID;
    }
    if (family == DensityFunctionalFamily.GENERALIZED_GRADIENT
        || family == DensityFunctionalFamily.META_GGA
        || family == DensityFunctionalFamily.CUSTOM) {
      return KohnShamFunctionalFamily.GGA;
    }
    return KohnShamFunctionalFamily.LDA;
  }

  private static boolean gridFunctionalSupported(
      final DensityFunctionalDefinition functionalDefinition) {
    final String normalized =
        functionalDefinition
            .name()
            .toLowerCase(java.util.Locale.ROOT)
            .replace("-", "")
            .replace("_", "");
    return normalized.equals("ldax")
        || normalized.equals("slater")
        || normalized.equals("svwn")
        || normalized.equals("vwn")
        || normalized.equals("lda")
        || normalized.equals("pbe")
        || normalized.equals("pbe0")
        || normalized.equals("pbeh")
        || normalized.equals("b3lyp");
  }

  private static void buildCoulombExchangePotential(
      final int size,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final double[] density,
      final double exactExchangeFraction,
      final double[] potential) {
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            final double densityValue = density[r * size + s];
            final double coulomb = electronRepulsion.valueAt(p, q, r, s);
            final double exchange = electronRepulsion.valueAt(p, r, q, s);
            value += densityValue * (coulomb - 0.5 * exactExchangeFraction * exchange);
          }
        }
        potential[p * size + q] = value;
      }
    }
  }

  private static void buildSpinCoulombExchangePotential(
      final int size,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final double[] totalDensity,
      final double[] spinDensity,
      final double exactExchangeFraction,
      final double[] potential) {
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            final int densityIndex = r * size + s;
            final double coulomb = electronRepulsion.valueAt(p, q, r, s);
            final double exchange = electronRepulsion.valueAt(p, r, q, s);
            value +=
                totalDensity[densityIndex] * coulomb
                    - exactExchangeFraction * spinDensity[densityIndex] * exchange;
          }
        }
        potential[p * size + q] = value;
      }
    }
  }

  private static double[] buildLocalXcPotential(
      final int size,
      final double[] overlap,
      final double[] density,
      final KohnShamFunctionalFamily family,
      final double[] potential) {
    final double trace = Math.max(KohnShamDftSolver.densityTrace(size, density, overlap), 1.0E-12);
    final double normalizedDensity = trace / Math.max(1, size);
    final double exchangeScale = KohnShamDftSolver.exchangeScale(family);
    final double correlationScale = KohnShamDftSolver.correlationScale(family);
    final double exchangeEnergy =
        -exchangeScale * Math.pow(normalizedDensity, 4.0 / 3.0) * Math.max(1, size);
    final double correlationEnergy =
        -correlationScale * Math.log1p(normalizedDensity) * Math.max(1, size);
    final double derivative =
        -(4.0 / 3.0) * exchangeScale * Math.cbrt(normalizedDensity)
            - correlationScale / (1.0 + normalizedDensity);
    for (int i = 0; i < potential.length; ++i) {
      potential[i] = derivative * overlap[i];
    }
    return new double[] {exchangeEnergy, correlationEnergy};
  }

  private static double exchangeScale(final KohnShamFunctionalFamily family) {
    if (family == KohnShamFunctionalFamily.HYBRID) {
      return KohnShamDftSolver.HYBRID_EXCHANGE_SCALE;
    }
    if (family == KohnShamFunctionalFamily.GGA) {
      return KohnShamDftSolver.GGA_EXCHANGE_SCALE;
    }
    return KohnShamDftSolver.LDA_EXCHANGE_SCALE;
  }

  private static double correlationScale(final KohnShamFunctionalFamily family) {
    if (family == KohnShamFunctionalFamily.GGA) {
      return KohnShamDftSolver.CORRELATION_SCALE * 1.15;
    }
    if (family == KohnShamFunctionalFamily.HYBRID) {
      return KohnShamDftSolver.CORRELATION_SCALE * 0.85;
    }
    return KohnShamDftSolver.CORRELATION_SCALE;
  }

  private static void buildKohnShamMatrix(
      final double[] core,
      final double[] coulombExchangePotential,
      final double[] localXcPotential,
      final double[] target) {
    for (int i = 0; i < target.length; ++i) {
      target[i] = core[i] + coulombExchangePotential[i] + localXcPotential[i];
    }
  }

  private static double[] buildDensity(
      final int size, final int occupiedCount, final double[] coefficients) {
    final double[] density = new double[size * size];
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int i = 0; i < occupiedCount; ++i) {
          value += 2.0 * coefficients[p * size + i] * coefficients[q * size + i];
        }
        density[p * size + q] = value;
      }
    }
    return density;
  }

  private static double[] buildSpinDensity(
      final int size, final int occupiedCount, final double[] coefficients) {
    final double[] density = new double[size * size];
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int i = 0; i < occupiedCount; ++i) {
          value += coefficients[p * size + i] * coefficients[q * size + i];
        }
        density[p * size + q] = value;
      }
    }
    return density;
  }

  private static void sumDensity(
      final double[] alphaDensity, final double[] betaDensity, final double[] totalDensity) {
    for (int i = 0; i < totalDensity.length; ++i) {
      totalDensity[i] = alphaDensity[i] + betaDensity[i];
    }
  }

  private static void mixDensity(
      final double[] oldDensity, final double[] nextDensity, final double mixing) {
    for (int i = 0; i < nextDensity.length; ++i) {
      nextDensity[i] = (1.0 - mixing) * nextDensity[i] + mixing * oldDensity[i];
    }
  }

  private static double densityTrace(
      final int size, final double[] density, final double[] overlap) {
    double trace = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        trace += density[p * size + q] * overlap[p * size + q];
      }
    }
    return Math.max(trace, 0.0);
  }

  private static double electronicEnergy(
      final int size,
      final double[] density,
      final double[] core,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final double exactExchangeFraction,
      final double exchangeCorrelationEnergy) {
    double oneElectronEnergy = 0.0;
    double coulombEnergy = 0.0;
    double exactExchangeEnergy = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        final double densityPq = density[p * size + q];
        oneElectronEnergy += densityPq * core[p * size + q];
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            final double densityRs = density[r * size + s];
            coulombEnergy += 0.5 * densityPq * densityRs * electronRepulsion.valueAt(p, q, r, s);
            exactExchangeEnergy +=
                -0.25
                    * exactExchangeFraction
                    * densityPq
                    * densityRs
                    * electronRepulsion.valueAt(p, r, q, s);
          }
        }
      }
    }
    return oneElectronEnergy + coulombEnergy + exactExchangeEnergy + exchangeCorrelationEnergy;
  }

  private static double unrestrictedElectronicEnergy(
      final int size,
      final double[] alphaDensity,
      final double[] betaDensity,
      final double[] totalDensity,
      final double[] core,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final double exactExchangeFraction,
      final double exchangeCorrelationEnergy) {
    double oneElectronEnergy = 0.0;
    double coulombEnergy = 0.0;
    double exactExchangeEnergy = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        final double densityPq = totalDensity[p * size + q];
        oneElectronEnergy += densityPq * core[p * size + q];
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            final double densityRs = totalDensity[r * size + s];
            final double alphaPq = alphaDensity[p * size + q];
            final double alphaRs = alphaDensity[r * size + s];
            final double betaPq = betaDensity[p * size + q];
            final double betaRs = betaDensity[r * size + s];
            coulombEnergy += 0.5 * densityPq * densityRs * electronRepulsion.valueAt(p, q, r, s);
            exactExchangeEnergy +=
                -0.5
                    * exactExchangeFraction
                    * (alphaPq * alphaRs + betaPq * betaRs)
                    * electronRepulsion.valueAt(p, r, q, s);
          }
        }
      }
    }
    return oneElectronEnergy + coulombEnergy + exactExchangeEnergy + exchangeCorrelationEnergy;
  }

  private static int[] spinElectronCounts(
      final int electronCount, final int spinMultiplicity, final int orbitalCount) {
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Unrestricted Kohn-Sham DFT electron count must be positive.");
    }
    if (spinMultiplicity <= 0) {
      throw new IllegalArgumentException(
          "Unrestricted Kohn-Sham DFT spin multiplicity must be positive.");
    }
    final int unpaired = spinMultiplicity - 1;
    final int alphaNumerator = Math.addExact(electronCount, unpaired);
    final int betaNumerator = electronCount - unpaired;
    if ((alphaNumerator & 1) != 0 || (betaNumerator & 1) != 0 || betaNumerator < 0) {
      throw new IllegalArgumentException(
          "Unrestricted Kohn-Sham DFT spin multiplicity is incompatible with electron count.");
    }
    final int alpha = alphaNumerator / 2;
    final int beta = betaNumerator / 2;
    if (alpha > orbitalCount || beta > orbitalCount) {
      throw new IllegalArgumentException(
          "Unrestricted Kohn-Sham DFT occupied orbital count exceeds basis size.");
    }
    return new int[] {alpha, beta};
  }

  private static double finiteDelta(final double deltaEnergy) {
    if (Double.isFinite(deltaEnergy)) {
      return deltaEnergy;
    }
    return Double.MAX_VALUE;
  }

  private static double maxAbsoluteDifference(final double[] first, final double[] second) {
    double result = 0.0;
    for (int i = 0; i < first.length; ++i) {
      result = Math.max(result, Math.abs(first[i] - second[i]));
    }
    return result;
  }

  private static double[] transformSymmetric(
      final int size, final double[] transform, final double[] matrix) {
    final double[] temporary = KohnShamDftSolver.multiply(size, matrix, transform);
    return KohnShamDftSolver.multiplyTransposedLeft(size, transform, temporary);
  }

  private static double[] multiply(final int size, final double[] left, final double[] right) {
    final double[] result = new double[size * size];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[row * size + k] * right[k * size + column];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private static double[] multiplyTransposedLeft(
      final int size, final double[] left, final double[] right) {
    final double[] result = new double[size * size];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[k * size + row] * right[k * size + column];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private static double[] multiplyTransposedRight(
      final int size, final double[] left, final double[] right) {
    final double[] result = new double[size * size];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[row * size + k] * right[column * size + k];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }
}
