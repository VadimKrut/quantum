/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import java.util.ArrayList;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterIterationRecord;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

/**
 * Решает closed-shell CCD корреляцию поверх RHF/MO integrals.
 */
public final class ClosedShellCoupledClusterDoublesSolver {

  private static final double SINGULAR_DENOMINATOR_TOLERANCE = 1.0E-14;

  public CoupledClusterDoublesResult solve(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final RhfResult rhfResult,
      final int electronCount) {
    return this.solve(
        molecularOrbitalTensors, rhfResult, electronCount, CoupledClusterDoublesOptions.DEFAULT);
  }

  public CoupledClusterDoublesResult solve(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final RhfResult rhfResult,
      final int electronCount,
      final CoupledClusterDoublesOptions options) {
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException("CCD MO tensors must not be null.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException("CCD RHF result must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("CCD options must not be null.");
    }
    if (electronCount <= 0 || (electronCount & 1) != 0) {
      throw new IllegalArgumentException("CCD electron count must be positive and even.");
    }
    final int orbitalCount = molecularOrbitalTensors.orbitalCount();
    final int occupiedCount = electronCount / 2;
    if (occupiedCount >= orbitalCount) {
      throw new IllegalArgumentException("CCD requires at least one virtual orbital.");
    }
    final int virtualCount = orbitalCount - occupiedCount;
    final double[] orbitalEnergies = rhfResult.orbitalEnergiesCopy();
    if (orbitalEnergies.length != orbitalCount) {
      throw new IllegalArgumentException("CCD orbital energy count must match MO tensor size.");
    }
    final int amplitudeCount =
        Math.multiplyExact(
            Math.multiplyExact(occupiedCount, occupiedCount),
            Math.multiplyExact(virtualCount, virtualCount));
    final double[] amplitudes = new double[amplitudeCount];
    final double[] nextAmplitudes = new double[amplitudeCount];
    this.initializeAmplitudes(
        molecularOrbitalTensors, orbitalEnergies, occupiedCount, virtualCount, options, amplitudes);
    final ArrayList<CoupledClusterIterationRecord> history =
        new ArrayList<CoupledClusterIterationRecord>(options.maxIterations());
    double previousEnergy =
        this.correlationEnergy(molecularOrbitalTensors, occupiedCount, virtualCount, amplitudes);
    boolean converged = false;
    int iteration = 0;
    for (; iteration < options.maxIterations(); ++iteration) {
      final double amplitudeDeltaNorm =
          this.updateAmplitudes(
              molecularOrbitalTensors,
              orbitalEnergies,
              occupiedCount,
              virtualCount,
              options,
              amplitudes,
              nextAmplitudes);
      final double correlationEnergy =
          this.correlationEnergy(
              molecularOrbitalTensors, occupiedCount, virtualCount, nextAmplitudes);
      final double energyDelta = Math.abs(correlationEnergy - previousEnergy);
      history.add(
          CoupledClusterIterationRecord.of(
              iteration + 1, correlationEnergy, energyDelta, amplitudeDeltaNorm));
      System.arraycopy(nextAmplitudes, 0, amplitudes, 0, amplitudes.length);
      previousEnergy = correlationEnergy;
      if (energyDelta <= options.energyTolerance()
          && amplitudeDeltaNorm <= Math.sqrt(options.energyTolerance())) {
        converged = true;
        ++iteration;
        break;
      }
    }
    if (!converged) {
      iteration = options.maxIterations();
    }
    final double correlationEnergy = history.get(history.size() - 1).correlationEnergy();
    return CoupledClusterDoublesResult.of(
        converged,
        iteration,
        correlationEnergy,
        rhfResult.totalEnergy() + correlationEnergy,
        occupiedCount,
        virtualCount,
        amplitudes,
        history);
  }

  private void initializeAmplitudes(
      final MolecularIntegralTensors tensors,
      final double[] orbitalEnergies,
      final int occupiedCount,
      final int virtualCount,
      final CoupledClusterDoublesOptions options,
      final double[] amplitudes) {
    if (!options.startFromMp2()) {
      return;
    }
    for (int i = 0; i < occupiedCount; ++i) {
      for (int j = 0; j < occupiedCount; ++j) {
        for (int a = 0; a < virtualCount; ++a) {
          for (int b = 0; b < virtualCount; ++b) {
            final int virtualA = occupiedCount + a;
            final int virtualB = occupiedCount + b;
            amplitudes[
                    CoupledClusterDoublesResult.amplitudeIndex(
                        occupiedCount, virtualCount, i, j, a, b)] =
                this.antisymmetrizedIntegral(tensors, i, j, virtualA, virtualB)
                    / this.doublesDenominator(orbitalEnergies, i, j, virtualA, virtualB, options);
          }
        }
      }
    }
  }

  private double updateAmplitudes(
      final MolecularIntegralTensors tensors,
      final double[] orbitalEnergies,
      final int occupiedCount,
      final int virtualCount,
      final CoupledClusterDoublesOptions options,
      final double[] amplitudes,
      final double[] nextAmplitudes) {
    double squaredDelta = 0.0;
    for (int i = 0; i < occupiedCount; ++i) {
      for (int j = 0; j < occupiedCount; ++j) {
        for (int a = 0; a < virtualCount; ++a) {
          for (int b = 0; b < virtualCount; ++b) {
            final int virtualA = occupiedCount + a;
            final int virtualB = occupiedCount + b;
            final int index =
                CoupledClusterDoublesResult.amplitudeIndex(occupiedCount, virtualCount, i, j, a, b);
            final double residual =
                this.doublesResidual(tensors, occupiedCount, virtualCount, amplitudes, i, j, a, b);
            final double rawAmplitude =
                residual
                    / this.doublesDenominator(orbitalEnergies, i, j, virtualA, virtualB, options);
            final double updatedAmplitude =
                options.damping() * rawAmplitude + (1.0 - options.damping()) * amplitudes[index];
            nextAmplitudes[index] = updatedAmplitude;
            final double delta = updatedAmplitude - amplitudes[index];
            squaredDelta += delta * delta;
          }
        }
      }
    }
    return Math.sqrt(squaredDelta);
  }

  private double doublesResidual(
      final MolecularIntegralTensors tensors,
      final int occupiedCount,
      final int virtualCount,
      final double[] amplitudes,
      final int i,
      final int j,
      final int a,
      final int b) {
    final int virtualA = occupiedCount + a;
    final int virtualB = occupiedCount + b;
    double residual = this.antisymmetrizedIntegral(tensors, i, j, virtualA, virtualB);
    for (int c = 0; c < virtualCount; ++c) {
      for (int d = 0; d < virtualCount; ++d) {
        residual +=
            0.5
                * this.antisymmetrizedIntegral(
                    tensors, virtualA, virtualB, occupiedCount + c, occupiedCount + d)
                * amplitudes[
                    CoupledClusterDoublesResult.amplitudeIndex(
                        occupiedCount, virtualCount, i, j, c, d)];
      }
    }
    for (int k = 0; k < occupiedCount; ++k) {
      for (int l = 0; l < occupiedCount; ++l) {
        residual +=
            0.5
                * this.antisymmetrizedIntegral(tensors, k, l, i, j)
                * amplitudes[
                    CoupledClusterDoublesResult.amplitudeIndex(
                        occupiedCount, virtualCount, k, l, a, b)];
      }
    }
    for (int k = 0; k < occupiedCount; ++k) {
      for (int c = 0; c < virtualCount; ++c) {
        final double exchange =
            this.antisymmetrizedIntegral(tensors, virtualA, k, i, occupiedCount + c)
                    * amplitudes[
                        CoupledClusterDoublesResult.amplitudeIndex(
                            occupiedCount, virtualCount, k, j, c, b)]
                - this.antisymmetrizedIntegral(tensors, virtualB, k, i, occupiedCount + c)
                    * amplitudes[
                        CoupledClusterDoublesResult.amplitudeIndex(
                            occupiedCount, virtualCount, k, j, c, a)];
        residual += exchange;
      }
    }
    return residual;
  }

  private double correlationEnergy(
      final MolecularIntegralTensors tensors,
      final int occupiedCount,
      final int virtualCount,
      final double[] amplitudes) {
    double energy = 0.0;
    for (int i = 0; i < occupiedCount; ++i) {
      for (int j = 0; j < occupiedCount; ++j) {
        for (int a = 0; a < virtualCount; ++a) {
          for (int b = 0; b < virtualCount; ++b) {
            final int virtualA = occupiedCount + a;
            final int virtualB = occupiedCount + b;
            energy +=
                0.25
                    * this.antisymmetrizedIntegral(tensors, i, j, virtualA, virtualB)
                    * amplitudes[
                        CoupledClusterDoublesResult.amplitudeIndex(
                            occupiedCount, virtualCount, i, j, a, b)];
          }
        }
      }
    }
    return energy;
  }

  private double doublesDenominator(
      final double[] orbitalEnergies,
      final int firstOccupied,
      final int secondOccupied,
      final int firstVirtual,
      final int secondVirtual,
      final CoupledClusterDoublesOptions options) {
    final double denominator =
        orbitalEnergies[firstOccupied]
            + orbitalEnergies[secondOccupied]
            - orbitalEnergies[firstVirtual]
            - orbitalEnergies[secondVirtual];
    final double shiftedDenominator =
        denominator < 0.0
            ? denominator - options.denominatorShift()
            : denominator + options.denominatorShift();
    if (Math.abs(shiftedDenominator) <= SINGULAR_DENOMINATOR_TOLERANCE) {
      throw new IllegalArgumentException("CCD denominator is numerically singular.");
    }
    return shiftedDenominator;
  }

  private double antisymmetrizedIntegral(
      final MolecularIntegralTensors tensors,
      final int first,
      final int second,
      final int third,
      final int fourth) {
    return tensors.electronRepulsionAt(first, third, second, fourth)
        - tensors.electronRepulsionAt(first, fourth, second, third);
  }
}
