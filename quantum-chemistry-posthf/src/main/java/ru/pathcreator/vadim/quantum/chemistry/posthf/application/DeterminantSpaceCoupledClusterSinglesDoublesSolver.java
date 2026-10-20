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
import java.util.Arrays;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianFermionMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant.DeterminantSpaceGenerator;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant.DeterminantSpaceHamiltonianProjector;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterIterationRecord;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

public final class DeterminantSpaceCoupledClusterSinglesDoublesSolver {

  private static final int DEFAULT_MAX_DETERMINANTS = 50_000;
  private static final double SERIES_TOLERANCE = 1.0E-13;
  private final ElectronicHamiltonianFermionMapper fermionMapper;
  private final DeterminantSpaceHamiltonianProjector hamiltonianProjector;

  public DeterminantSpaceCoupledClusterSinglesDoublesSolver() {
    this(new ElectronicHamiltonianFermionMapper(), new DeterminantSpaceHamiltonianProjector());
  }

  public DeterminantSpaceCoupledClusterSinglesDoublesSolver(
      final ElectronicHamiltonianFermionMapper fermionMapper,
      final DeterminantSpaceHamiltonianProjector hamiltonianProjector) {
    if (fermionMapper == null) {
      throw new IllegalArgumentException("Projective CCSD fermion mapper must not be null.");
    }
    if (hamiltonianProjector == null) {
      throw new IllegalArgumentException("Projective CCSD Hamiltonian projector must not be null.");
    }
    this.fermionMapper = fermionMapper;
    this.hamiltonianProjector = hamiltonianProjector;
  }

  public CoupledClusterSinglesDoublesResult solve(
      final ElectronicHamiltonian hamiltonian,
      final RhfResult rhfResult,
      final CoupledClusterSinglesDoublesOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Projective CCSD Hamiltonian must not be null.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException("Projective CCSD RHF result must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("Projective CCSD options must not be null.");
    }
    if ((hamiltonian.electronCount() & 1) != 0) {
      throw new IllegalArgumentException(
          "Projective CCSD currently requires a closed-shell electron count.");
    }
    final int spatialOrbitalCount = hamiltonian.spinOrbitalCount() / 2;
    final int occupiedSpatialCount = hamiltonian.electronCount() / 2;
    final int virtualSpatialCount = spatialOrbitalCount - occupiedSpatialCount;
    if (virtualSpatialCount <= 0) {
      throw new IllegalArgumentException("Projective CCSD requires at least one virtual orbital.");
    }
    final double[] orbitalEnergies = rhfResult.orbitalEnergiesCopy();
    if (orbitalEnergies.length != spatialOrbitalCount) {
      throw new IllegalArgumentException(
          "Projective CCSD orbital energy count must match Hamiltonian.");
    }
    final long[] determinants =
        DeterminantSpaceGenerator.spinSector(
            hamiltonian.spinOrbitalCount(),
            hamiltonian.electronCount(),
            occupiedSpatialCount,
            occupiedSpatialCount,
            DEFAULT_MAX_DETERMINANTS);
    final LongDeterminantIndex determinantIndex = LongDeterminantIndex.of(determinants);
    final int referenceIndex =
        determinantIndex.indexOf(
            DeterminantSpaceCoupledClusterSinglesDoublesSolver.referenceDeterminant(
                hamiltonian.electronCount()));
    if (referenceIndex < 0) {
      throw new IllegalArgumentException(
          "Projective CCSD reference determinant is outside determinant sector.");
    }
    final Excitation[] excitations =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.generateSpinConservingExcitations(
            hamiltonian.spinOrbitalCount(), hamiltonian.electronCount(), orbitalEnergies);
    final double[] hamiltonianMatrix =
        this.hamiltonianProjector.projectSymmetric(
            this.fermionMapper.map(hamiltonian), determinants);
    final double[] amplitudes = new double[excitations.length];
    if (options.startFromMp2()) {
      DeterminantSpaceCoupledClusterSinglesDoublesSolver.initializeFirstOrderAmplitudes(
          amplitudes,
          excitations,
          hamiltonianMatrix,
          referenceIndex,
          determinants,
          determinantIndex);
    }
    final ArrayList<CoupledClusterIterationRecord> history =
        new ArrayList<CoupledClusterIterationRecord>(options.maxIterations());
    final double[] nextAmplitudes = new double[amplitudes.length];
    double previousEnergy =
        this.similarityTransformedState(
                hamiltonianMatrix,
                determinants,
                determinantIndex,
                referenceIndex,
                excitations,
                amplitudes)
            .energy();
    boolean converged = false;
    int iteration = 0;
    for (; iteration < options.maxIterations(); ++iteration) {
      final ProjectiveState projectiveState =
          this.similarityTransformedState(
              hamiltonianMatrix,
              determinants,
              determinantIndex,
              referenceIndex,
              excitations,
              amplitudes);
      final double[] residuals =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.residuals(
              determinantIndex, excitations, projectiveState.vector());
      final double currentResidualNorm =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.norm(residuals);
      final double[] rawDelta = new double[amplitudes.length];
      for (int i = 0; i < excitations.length; ++i) {
        rawDelta[i] = residuals[i] / excitations[i].denominator();
      }
      double step = options.damping();
      double[] acceptedAmplitudes = null;
      ProjectiveState acceptedState = null;
      double acceptedDeltaNorm = Double.NaN;
      while (step >= 1.0E-6) {
        double squaredDelta = 0.0;
        for (int i = 0; i < amplitudes.length; ++i) {
          final double delta = step * rawDelta[i];
          nextAmplitudes[i] = amplitudes[i] + delta;
          squaredDelta += delta * delta;
        }
        final ProjectiveState candidateState =
            this.similarityTransformedState(
                hamiltonianMatrix,
                determinants,
                determinantIndex,
                referenceIndex,
                excitations,
                nextAmplitudes);
        final double candidateResidualNorm =
            DeterminantSpaceCoupledClusterSinglesDoublesSolver.norm(
                DeterminantSpaceCoupledClusterSinglesDoublesSolver.residuals(
                    determinantIndex, excitations, candidateState.vector()));
        if (Double.isFinite(candidateState.energy())
            && Double.isFinite(candidateResidualNorm)
            && candidateResidualNorm <= currentResidualNorm * (1.0 + 1.0E-8)) {
          acceptedAmplitudes = nextAmplitudes.clone();
          acceptedState = candidateState;
          acceptedDeltaNorm = Math.sqrt(squaredDelta);
          break;
        }
        step *= 0.5;
      }
      if (acceptedAmplitudes == null) {
        for (int i = 0; i < amplitudes.length; ++i) {
          final double delta = 1.0E-6 * rawDelta[i];
          nextAmplitudes[i] = amplitudes[i] + delta;
        }
        acceptedAmplitudes = nextAmplitudes.clone();
        acceptedState =
            this.similarityTransformedState(
                hamiltonianMatrix,
                determinants,
                determinantIndex,
                referenceIndex,
                excitations,
                acceptedAmplitudes);
        acceptedDeltaNorm =
            DeterminantSpaceCoupledClusterSinglesDoublesSolver.deltaNorm(
                amplitudes, acceptedAmplitudes);
      }
      System.arraycopy(acceptedAmplitudes, 0, amplitudes, 0, amplitudes.length);
      final double correlationEnergy = acceptedState.energy() - rhfResult.totalEnergy();
      final double energyDelta = Math.abs(acceptedState.energy() - previousEnergy);
      history.add(
          CoupledClusterIterationRecord.of(
              iteration + 1, correlationEnergy, energyDelta, acceptedDeltaNorm));
      previousEnergy = acceptedState.energy();
      if (energyDelta <= options.energyTolerance()
          && acceptedDeltaNorm <= Math.sqrt(options.energyTolerance())
          && currentResidualNorm <= Math.sqrt(options.energyTolerance())) {
        converged = true;
        ++iteration;
        break;
      }
    }
    if (!converged) {
      iteration = options.maxIterations();
    }
    final double[] singles =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.spatialSinglesAmplitudes(
            occupiedSpatialCount, virtualSpatialCount, excitations, amplitudes);
    final double[] doubles =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.spatialDoublesAmplitudes(
            occupiedSpatialCount, virtualSpatialCount, excitations, amplitudes);
    final double correlationEnergy = history.get(history.size() - 1).correlationEnergy();
    return CoupledClusterSinglesDoublesResult.of(
        converged,
        iteration,
        correlationEnergy,
        rhfResult.totalEnergy() + correlationEnergy,
        occupiedSpatialCount,
        virtualSpatialCount,
        singles,
        doubles,
        history);
  }

  private ProjectiveState similarityTransformedState(
      final double[] hamiltonianMatrix,
      final long[] determinants,
      final LongDeterminantIndex determinantIndex,
      final int referenceIndex,
      final Excitation[] excitations,
      final double[] amplitudes) {
    final double[] right =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.exponentialAppliedToReference(
            determinants, determinantIndex, referenceIndex, excitations, amplitudes, 1.0);
    final double[] hRight =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.multiply(
            hamiltonianMatrix, determinants.length, right);
    final double[] transformed =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.exponentialAppliedToVector(
            determinants, determinantIndex, excitations, amplitudes, hRight, -1.0);
    return new ProjectiveState(transformed[referenceIndex], transformed);
  }

  private static void initializeFirstOrderAmplitudes(
      final double[] amplitudes,
      final Excitation[] excitations,
      final double[] hamiltonianMatrix,
      final int referenceIndex,
      final long[] determinants,
      final LongDeterminantIndex determinantIndex) {
    for (int i = 0; i < excitations.length; ++i) {
      final int row = determinantIndex.indexOf(excitations[i].applyToReference());
      if (row >= 0) {
        amplitudes[i] =
            excitations[i].referencePhase()
                * hamiltonianMatrix[row * determinants.length + referenceIndex]
                / excitations[i].denominator();
      }
    }
  }

  private static double[] residuals(
      final LongDeterminantIndex determinantIndex,
      final Excitation[] excitations,
      final double[] projectiveVector) {
    final double[] residuals = new double[excitations.length];
    for (int i = 0; i < excitations.length; ++i) {
      final int residualIndex = determinantIndex.indexOf(excitations[i].applyToReference());
      residuals[i] =
          residualIndex < 0
              ? 0.0
              : excitations[i].referencePhase() * projectiveVector[residualIndex];
    }
    return residuals;
  }

  private static double norm(final double[] values) {
    double squaredNorm = 0.0;
    for (int i = 0; i < values.length; ++i) {
      squaredNorm += values[i] * values[i];
    }
    return Math.sqrt(squaredNorm);
  }

  private static double deltaNorm(final double[] first, final double[] second) {
    double squaredNorm = 0.0;
    for (int i = 0; i < first.length; ++i) {
      final double delta = second[i] - first[i];
      squaredNorm += delta * delta;
    }
    return Math.sqrt(squaredNorm);
  }

  private static double[] exponentialAppliedToReference(
      final long[] determinants,
      final LongDeterminantIndex determinantIndex,
      final int referenceIndex,
      final Excitation[] excitations,
      final double[] amplitudes,
      final double sign) {
    final double[] vector = new double[determinants.length];
    vector[referenceIndex] = 1.0;
    return DeterminantSpaceCoupledClusterSinglesDoublesSolver.exponentialAppliedToVector(
        determinants, determinantIndex, excitations, amplitudes, vector, sign);
  }

  private static double[] exponentialAppliedToVector(
      final long[] determinants,
      final LongDeterminantIndex determinantIndex,
      final Excitation[] excitations,
      final double[] amplitudes,
      final double[] source,
      final double sign) {
    final double[] result = source.clone();
    double[] term = source.clone();
    for (int order = 1; order <= Long.SIZE; ++order) {
      final double[] applied =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.applyClusterOperator(
              determinants, determinantIndex, excitations, amplitudes, term, sign / order);
      final double norm =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.addAndNorm(result, applied);
      term = applied;
      if (norm <= SERIES_TOLERANCE) {
        break;
      }
    }
    return result;
  }

  private static double[] applyClusterOperator(
      final long[] determinants,
      final LongDeterminantIndex determinantIndex,
      final Excitation[] excitations,
      final double[] amplitudes,
      final double[] source,
      final double scale) {
    final double[] target = new double[source.length];
    for (int column = 0; column < source.length; ++column) {
      final double coefficient = source[column];
      if (coefficient == 0.0) {
        continue;
      }
      final long determinant = determinants[column];
      for (int excitationIndex = 0; excitationIndex < excitations.length; ++excitationIndex) {
        final double amplitude = amplitudes[excitationIndex];
        if (amplitude == 0.0) {
          continue;
        }
        final AppliedTerm applied = excitations[excitationIndex].apply(determinant);
        if (!applied.nonZero()) {
          continue;
        }
        final int row = determinantIndex.indexOf(applied.determinant());
        if (row >= 0) {
          target[row] += scale * amplitude * coefficient * applied.phase();
        }
      }
    }
    return target;
  }

  private static double addAndNorm(final double[] target, final double[] increment) {
    double squaredNorm = 0.0;
    for (int i = 0; i < target.length; ++i) {
      target[i] += increment[i];
      squaredNorm += increment[i] * increment[i];
    }
    return Math.sqrt(squaredNorm);
  }

  private static double[] multiply(final double[] matrix, final int size, final double[] vector) {
    final double[] result = new double[size];
    for (int row = 0; row < size; ++row) {
      double value = 0.0;
      final int rowOffset = row * size;
      for (int column = 0; column < size; ++column) {
        value += matrix[rowOffset + column] * vector[column];
      }
      result[row] = value;
    }
    return result;
  }

  private static Excitation[] generateSpinConservingExcitations(
      final int spinOrbitalCount, final int electronCount, final double[] orbitalEnergies) {
    final ExcitationBuilder builder = new ExcitationBuilder();
    final long reference =
        DeterminantSpaceCoupledClusterSinglesDoublesSolver.referenceDeterminant(electronCount);
    for (int occupied = 0; occupied < electronCount; ++occupied) {
      for (int virtual = electronCount; virtual < spinOrbitalCount; ++virtual) {
        if (DeterminantSpaceCoupledClusterSinglesDoublesSolver.spin(occupied)
            == DeterminantSpaceCoupledClusterSinglesDoublesSolver.spin(virtual)) {
          builder.add(
              Excitation.single(
                  reference,
                  occupied,
                  virtual,
                  orbitalEnergies[occupied / 2] - orbitalEnergies[virtual / 2]));
        }
      }
    }
    for (int firstOccupied = 0; firstOccupied < electronCount; ++firstOccupied) {
      for (int secondOccupied = firstOccupied + 1;
          secondOccupied < electronCount;
          ++secondOccupied) {
        for (int firstVirtual = electronCount; firstVirtual < spinOrbitalCount; ++firstVirtual) {
          for (int secondVirtual = firstVirtual + 1;
              secondVirtual < spinOrbitalCount;
              ++secondVirtual) {
            if (DeterminantSpaceCoupledClusterSinglesDoublesSolver.spinSum(
                    firstOccupied, secondOccupied)
                == DeterminantSpaceCoupledClusterSinglesDoublesSolver.spinSum(
                    firstVirtual, secondVirtual)) {
              builder.add(
                  Excitation.doubleExcitation(
                      reference,
                      firstOccupied,
                      secondOccupied,
                      firstVirtual,
                      secondVirtual,
                      orbitalEnergies[firstOccupied / 2]
                          + orbitalEnergies[secondOccupied / 2]
                          - orbitalEnergies[firstVirtual / 2]
                          - orbitalEnergies[secondVirtual / 2]));
            }
          }
        }
      }
    }
    return builder.toArray();
  }

  private static double[] spatialSinglesAmplitudes(
      final int occupiedSpatialCount,
      final int virtualSpatialCount,
      final Excitation[] excitations,
      final double[] amplitudes) {
    final double[] singles =
        new double[Math.multiplyExact(occupiedSpatialCount, virtualSpatialCount)];
    for (int i = 0; i < excitations.length; ++i) {
      final Excitation excitation = excitations[i];
      if (!excitation.single()
          || excitation.firstOccupied() % 2 != 0
          || excitation.firstVirtual() % 2 != 0) {
        continue;
      }
      final int occupied = excitation.firstOccupied() / 2;
      final int virtual = excitation.firstVirtual() / 2 - occupiedSpatialCount;
      if (occupied >= 0
          && occupied < occupiedSpatialCount
          && virtual >= 0
          && virtual < virtualSpatialCount) {
        singles[
                CoupledClusterSinglesDoublesResult.singlesAmplitudeIndex(
                    virtualSpatialCount, occupied, virtual)] =
            amplitudes[i];
      }
    }
    return singles;
  }

  private static double[] spatialDoublesAmplitudes(
      final int occupiedSpatialCount,
      final int virtualSpatialCount,
      final Excitation[] excitations,
      final double[] amplitudes) {
    final double[] doubles =
        new double
            [Math.multiplyExact(
                Math.multiplyExact(occupiedSpatialCount, occupiedSpatialCount),
                Math.multiplyExact(virtualSpatialCount, virtualSpatialCount))];
    for (int i = 0; i < excitations.length; ++i) {
      final Excitation excitation = excitations[i];
      if (excitation.single()
          || excitation.firstOccupied() % 2 != 0
          || excitation.secondOccupied() % 2 != 1
          || excitation.firstVirtual() % 2 != 0
          || excitation.secondVirtual() % 2 != 1) {
        continue;
      }
      final int firstOccupied = excitation.firstOccupied() / 2;
      final int secondOccupied = excitation.secondOccupied() / 2;
      final int firstVirtual = excitation.firstVirtual() / 2 - occupiedSpatialCount;
      final int secondVirtual = excitation.secondVirtual() / 2 - occupiedSpatialCount;
      if (firstOccupied >= 0
          && firstOccupied < occupiedSpatialCount
          && secondOccupied >= 0
          && secondOccupied < occupiedSpatialCount
          && firstVirtual >= 0
          && firstVirtual < virtualSpatialCount
          && secondVirtual >= 0
          && secondVirtual < virtualSpatialCount) {
        doubles[
                CoupledClusterDoublesResult.amplitudeIndex(
                    occupiedSpatialCount,
                    virtualSpatialCount,
                    firstOccupied,
                    secondOccupied,
                    firstVirtual,
                    secondVirtual)] =
            amplitudes[i];
      }
    }
    return doubles;
  }

  private static double[] buildHamiltonianMatrix(
      final FermionOperator operator,
      final long[] determinants,
      final LongDeterminantIndex determinantIndex) {
    final int size = determinants.length;
    final double[] matrix = new double[size * size];
    for (int column = 0; column < size; ++column) {
      final long ket = determinants[column];
      for (int termIndex = 0; termIndex < operator.termCount(); ++termIndex) {
        final FermionTerm term = operator.term(termIndex);
        final ComplexCoefficient coefficient = operator.coefficient(termIndex);
        if (!coefficient.effectivelyReal()) {
          throw new IllegalArgumentException(
              "Projective CCSD requires a real Hermitian Hamiltonian.");
        }
        final AppliedTerm applied =
            DeterminantSpaceCoupledClusterSinglesDoublesSolver.apply(term, ket);
        if (!applied.nonZero()) {
          continue;
        }
        final int row = determinantIndex.indexOf(applied.determinant());
        if (row >= 0) {
          matrix[row * size + column] += applied.phase() * coefficient.real();
        }
      }
    }
    return matrix;
  }

  private static AppliedTerm apply(final FermionTerm term, final long determinant) {
    long state = determinant;
    double phase = 1.0;
    for (int i = term.length() - 1; i >= 0; --i) {
      final long encodedOperator = term.operator(i);
      final int orbital = FermionTerm.spinOrbital(encodedOperator);
      final long mask = 1L << orbital;
      final boolean occupied = (state & mask) != 0L;
      if (FermionTerm.action(encodedOperator) == FermionLadderAction.CREATION) {
        if (occupied) {
          return AppliedTerm.zero();
        }
        phase *= DeterminantSpaceCoupledClusterSinglesDoublesSolver.fermionPhase(state, orbital);
        state |= mask;
      } else {
        if (!occupied) {
          return AppliedTerm.zero();
        }
        phase *= DeterminantSpaceCoupledClusterSinglesDoublesSolver.fermionPhase(state, orbital);
        state &= ~mask;
      }
    }
    return AppliedTerm.of(state, phase);
  }

  private static long[] generateSpinSectorDeterminants(
      final int spatialOrbitalCount,
      final int alphaElectronCount,
      final int betaElectronCount,
      final int maxDeterminants) {
    final long determinantCount =
        Math.multiplyExact(
            DeterminantSpaceCoupledClusterSinglesDoublesSolver.combination(
                spatialOrbitalCount, alphaElectronCount),
            DeterminantSpaceCoupledClusterSinglesDoublesSolver.combination(
                spatialOrbitalCount, betaElectronCount));
    if (determinantCount > maxDeterminants) {
      throw new IllegalArgumentException(
          "Projective CCSD determinant space exceeds configured limit.");
    }
    final long[] determinants = new long[(int) determinantCount];
    final int[] cursor = new int[] {0};
    DeterminantSpaceCoupledClusterSinglesDoublesSolver.generateSpinSectorRecursive(
        spatialOrbitalCount, alphaElectronCount, betaElectronCount, 0, 0L, determinants, cursor);
    Arrays.sort(determinants);
    return determinants;
  }

  private static void generateSpinSectorRecursive(
      final int spatialOrbitalCount,
      final int remainingAlphaElectrons,
      final int remainingBetaElectrons,
      final int spatialOrbital,
      final long current,
      final long[] determinants,
      final int[] cursor) {
    if (spatialOrbital == spatialOrbitalCount) {
      if (remainingAlphaElectrons == 0 && remainingBetaElectrons == 0) {
        determinants[cursor[0]] = current;
        ++cursor[0];
      }
      return;
    }
    final int remainingSpatialOrbitals = spatialOrbitalCount - spatialOrbital - 1;
    DeterminantSpaceCoupledClusterSinglesDoublesSolver.generateSpinSectorRecursive(
        spatialOrbitalCount,
        remainingAlphaElectrons,
        remainingBetaElectrons,
        spatialOrbital + 1,
        current,
        determinants,
        cursor);
    if (remainingAlphaElectrons > 0 && remainingAlphaElectrons - 1 <= remainingSpatialOrbitals) {
      DeterminantSpaceCoupledClusterSinglesDoublesSolver.generateSpinSectorRecursive(
          spatialOrbitalCount,
          remainingAlphaElectrons - 1,
          remainingBetaElectrons,
          spatialOrbital + 1,
          current | (1L << (spatialOrbital * 2)),
          determinants,
          cursor);
    }
    if (remainingBetaElectrons > 0 && remainingBetaElectrons - 1 <= remainingSpatialOrbitals) {
      DeterminantSpaceCoupledClusterSinglesDoublesSolver.generateSpinSectorRecursive(
          spatialOrbitalCount,
          remainingAlphaElectrons,
          remainingBetaElectrons - 1,
          spatialOrbital + 1,
          current | (1L << (spatialOrbital * 2 + 1)),
          determinants,
          cursor);
    }
    if (remainingAlphaElectrons > 0
        && remainingBetaElectrons > 0
        && remainingAlphaElectrons - 1 <= remainingSpatialOrbitals
        && remainingBetaElectrons - 1 <= remainingSpatialOrbitals) {
      DeterminantSpaceCoupledClusterSinglesDoublesSolver.generateSpinSectorRecursive(
          spatialOrbitalCount,
          remainingAlphaElectrons - 1,
          remainingBetaElectrons - 1,
          spatialOrbital + 1,
          current | (1L << (spatialOrbital * 2)) | (1L << (spatialOrbital * 2 + 1)),
          determinants,
          cursor);
    }
  }

  private static long referenceDeterminant(final int electronCount) {
    long determinant = 0L;
    for (int orbital = 0; orbital < electronCount; ++orbital) {
      determinant |= 1L << orbital;
    }
    return determinant;
  }

  private static long combination(final int n, final int k) {
    if (k < 0 || k > n) {
      return 0L;
    }
    final int selected = Math.min(k, n - k);
    long result = 1L;
    for (int i = 1; i <= selected; ++i) {
      result = Math.multiplyExact(result, n - selected + i) / i;
    }
    return result;
  }

  private static int spin(final int spinOrbital) {
    return spinOrbital & 1;
  }

  private static int spinSum(final int first, final int second) {
    return DeterminantSpaceCoupledClusterSinglesDoublesSolver.spin(first)
        + DeterminantSpaceCoupledClusterSinglesDoublesSolver.spin(second);
  }

  private static double fermionPhase(final long determinant, final int orbital) {
    final long lowerMask = (1L << orbital) - 1L;
    return (Long.bitCount(determinant & lowerMask) & 1) == 0 ? 1.0 : -1.0;
  }

  private static final class ProjectiveState {

    private final double energy;
    private final double[] vector;

    private ProjectiveState(final double energy, final double[] vector) {
      this.energy = energy;
      this.vector = vector;
    }

    private double energy() {
      return this.energy;
    }

    private double[] vector() {
      return this.vector;
    }
  }

  private static final class Excitation {

    private final long referenceImage;
    private final int firstOccupied;
    private final int secondOccupied;
    private final int firstVirtual;
    private final int secondVirtual;
    private final double denominator;
    private final double referencePhase;
    private final boolean single;

    private Excitation(
        final long referenceImage,
        final int firstOccupied,
        final int secondOccupied,
        final int firstVirtual,
        final int secondVirtual,
        final double denominator,
        final double referencePhase,
        final boolean single) {
      this.referenceImage = referenceImage;
      this.firstOccupied = firstOccupied;
      this.secondOccupied = secondOccupied;
      this.firstVirtual = firstVirtual;
      this.secondVirtual = secondVirtual;
      this.denominator = denominator;
      this.referencePhase = referencePhase;
      this.single = single;
    }

    private static Excitation single(
        final long reference, final int occupied, final int virtual, final double denominator) {
      final AppliedTerm referenceImage = Excitation.applySingleRaw(reference, occupied, virtual);
      return new Excitation(
          referenceImage.determinant(),
          occupied,
          -1,
          virtual,
          -1,
          denominator,
          referenceImage.phase(),
          true);
    }

    private static Excitation doubleExcitation(
        final long reference,
        final int firstOccupied,
        final int secondOccupied,
        final int firstVirtual,
        final int secondVirtual,
        final double denominator) {
      final AppliedTerm referenceImage =
          Excitation.applyDoubleRaw(
              reference, firstOccupied, secondOccupied, firstVirtual, secondVirtual);
      return new Excitation(
          referenceImage.determinant(),
          firstOccupied,
          secondOccupied,
          firstVirtual,
          secondVirtual,
          denominator,
          referenceImage.phase(),
          false);
    }

    private AppliedTerm apply(final long determinant) {
      return this.single ? this.applySingle(determinant) : this.applyDouble(determinant);
    }

    private AppliedTerm applySingle(final long determinant) {
      return Excitation.applySingleRaw(determinant, this.firstOccupied, this.firstVirtual);
    }

    private AppliedTerm applyDouble(final long determinant) {
      return Excitation.applyDoubleRaw(
          determinant,
          this.firstOccupied,
          this.secondOccupied,
          this.firstVirtual,
          this.secondVirtual);
    }

    private static AppliedTerm applySingleRaw(
        final long determinant, final int occupied, final int virtual) {
      AppliedTerm applied =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.annihilate(determinant, occupied, 1.0);
      if (!applied.nonZero()) {
        return AppliedTerm.zero();
      }
      return DeterminantSpaceCoupledClusterSinglesDoublesSolver.create(
          applied.determinant(), virtual, applied.phase());
    }

    private static AppliedTerm applyDoubleRaw(
        final long determinant,
        final int firstOccupied,
        final int secondOccupied,
        final int firstVirtual,
        final int secondVirtual) {
      AppliedTerm applied =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.annihilate(
              determinant, firstOccupied, 1.0);
      if (!applied.nonZero()) {
        return AppliedTerm.zero();
      }
      applied =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.annihilate(
              applied.determinant(), secondOccupied, applied.phase());
      if (!applied.nonZero()) {
        return AppliedTerm.zero();
      }
      applied =
          DeterminantSpaceCoupledClusterSinglesDoublesSolver.create(
              applied.determinant(), secondVirtual, applied.phase());
      if (!applied.nonZero()) {
        return AppliedTerm.zero();
      }
      return DeterminantSpaceCoupledClusterSinglesDoublesSolver.create(
          applied.determinant(), firstVirtual, applied.phase());
    }

    private long applyToReference() {
      return this.referenceImage;
    }

    private int firstOccupied() {
      return this.firstOccupied;
    }

    private int secondOccupied() {
      return this.secondOccupied;
    }

    private int firstVirtual() {
      return this.firstVirtual;
    }

    private int secondVirtual() {
      return this.secondVirtual;
    }

    private double denominator() {
      return this.denominator;
    }

    private double referencePhase() {
      return this.referencePhase;
    }

    private boolean single() {
      return this.single;
    }
  }

  private static AppliedTerm annihilate(
      final long determinant, final int orbital, final double phase) {
    final long mask = 1L << orbital;
    if ((determinant & mask) == 0L) {
      return AppliedTerm.zero();
    }
    return AppliedTerm.of(
        determinant & ~mask,
        phase
            * DeterminantSpaceCoupledClusterSinglesDoublesSolver.fermionPhase(
                determinant, orbital));
  }

  private static AppliedTerm create(final long determinant, final int orbital, final double phase) {
    final long mask = 1L << orbital;
    if ((determinant & mask) != 0L) {
      return AppliedTerm.zero();
    }
    return AppliedTerm.of(
        determinant | mask,
        phase
            * DeterminantSpaceCoupledClusterSinglesDoublesSolver.fermionPhase(
                determinant, orbital));
  }

  private static final class AppliedTerm {

    private static final AppliedTerm ZERO = new AppliedTerm(0L, 0.0, false);
    private final long determinant;
    private final double phase;
    private final boolean nonZero;

    private AppliedTerm(final long determinant, final double phase, final boolean nonZero) {
      this.determinant = determinant;
      this.phase = phase;
      this.nonZero = nonZero;
    }

    private static AppliedTerm zero() {
      return ZERO;
    }

    private static AppliedTerm of(final long determinant, final double phase) {
      return new AppliedTerm(determinant, phase, true);
    }

    private long determinant() {
      return this.determinant;
    }

    private double phase() {
      return this.phase;
    }

    private boolean nonZero() {
      return this.nonZero;
    }
  }

  private static final class ExcitationBuilder {

    private Excitation[] values = new Excitation[64];
    private int size;

    private void add(final Excitation value) {
      if (Math.abs(value.denominator()) <= 1.0E-14) {
        throw new IllegalArgumentException("Projective CCSD denominator is numerically singular.");
      }
      if (this.size == this.values.length) {
        this.values = Arrays.copyOf(this.values, this.values.length << 1);
      }
      this.values[this.size] = value;
      ++this.size;
    }

    private Excitation[] toArray() {
      return Arrays.copyOf(this.values, this.size);
    }
  }

  private static final class LongDeterminantIndex {

    private static final float LOAD_FACTOR = 0.65F;
    private final long[] keys;
    private final int[] values;
    private final boolean[] used;
    private final int mask;

    private LongDeterminantIndex(
        final long[] keys, final int[] values, final boolean[] used, final int mask) {
      this.keys = keys;
      this.values = values;
      this.used = used;
      this.mask = mask;
    }

    private static LongDeterminantIndex of(final long[] determinants) {
      int capacity = 1;
      while ((int) ((float) capacity * LOAD_FACTOR) < determinants.length) {
        capacity <<= 1;
      }
      final LongDeterminantIndex index =
          new LongDeterminantIndex(
              new long[capacity], new int[capacity], new boolean[capacity], capacity - 1);
      for (int i = 0; i < determinants.length; ++i) {
        index.put(determinants[i], i);
      }
      return index;
    }

    private void put(final long key, final int value) {
      int slot = this.slot(key);
      while (this.used[slot]) {
        if (this.keys[slot] == key) {
          this.values[slot] = value;
          return;
        }
        slot = (slot + 1) & this.mask;
      }
      this.used[slot] = true;
      this.keys[slot] = key;
      this.values[slot] = value;
    }

    private int indexOf(final long key) {
      int slot = this.slot(key);
      while (this.used[slot]) {
        if (this.keys[slot] == key) {
          return this.values[slot];
        }
        slot = (slot + 1) & this.mask;
      }
      return -1;
    }

    private int slot(final long key) {
      long mixed = key;
      mixed ^= mixed >>> 33;
      mixed *= 0xff51afd7ed558ccdL;
      mixed ^= mixed >>> 33;
      mixed *= 0xc4ceb9fe1a85ec53L;
      mixed ^= mixed >>> 33;
      return (int) mixed & this.mask;
    }
  }
}
