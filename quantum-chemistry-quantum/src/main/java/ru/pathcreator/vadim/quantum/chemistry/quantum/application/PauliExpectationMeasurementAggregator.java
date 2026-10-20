/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.measurement.PauliExpectationMeasurementResult;

/**
 * Восстанавливает энергию Pauli Hamiltonian из counts измерительных circuits.
 */
public final class PauliExpectationMeasurementAggregator {

  private static final double REAL_COEFFICIENT_TOLERANCE = 1.0E-10;

  public PauliExpectationMeasurementResult aggregate(
      final QubitPauliOperator hamiltonian, final List<Map<String, Long>> termCounts) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException(
          "Pauli measurement aggregation Hamiltonian must not be null.");
    }
    if (termCounts == null) {
      throw new IllegalArgumentException("Pauli measurement aggregation counts must not be null.");
    }
    final int measuredTermCount =
        PauliExpectationMeasurementAggregator.nonIdentityTermCount(hamiltonian);
    if (termCounts.size() != measuredTermCount) {
      throw new IllegalArgumentException(
          "Pauli measurement aggregation count set size must match non-identity term count.");
    }
    final double[] expectations = new double[measuredTermCount];
    final double[] contributions = new double[measuredTermCount];
    final long[] shots = new long[measuredTermCount];
    double energy = 0.0;
    double identityOffset = 0.0;
    int measurementIndex = 0;
    for (int i = 0; i < hamiltonian.termCount(); ++i) {
      final QubitPauliTerm term = hamiltonian.term(i);
      final ComplexCoefficient coefficient = hamiltonian.coefficient(i);
      PauliExpectationMeasurementAggregator.requireReal(coefficient);
      if (term.identity()) {
        identityOffset += coefficient.real();
        energy += coefficient.real();
        continue;
      }
      final TermMeasurementExpectation expectation =
          PauliExpectationMeasurementAggregator.expectation(term, termCounts.get(measurementIndex));
      final double contribution = coefficient.real() * expectation.value;
      expectations[measurementIndex] = expectation.value;
      contributions[measurementIndex] = contribution;
      shots[measurementIndex] = expectation.shots;
      energy += contribution;
      ++measurementIndex;
    }
    return PauliExpectationMeasurementResult.of(
        energy, identityOffset, expectations, contributions, shots);
  }

  public PauliExpectationMeasurementResult aggregateSimulationResults(
      final QubitPauliOperator hamiltonian, final List<SimulationResult> simulationResults) {
    if (simulationResults == null) {
      throw new IllegalArgumentException("Pauli measurement simulation results must not be null.");
    }
    final ArrayList<Map<String, Long>> termCounts = new ArrayList<>(simulationResults.size());
    for (int i = 0; i < simulationResults.size(); ++i) {
      final SimulationResult result = simulationResults.get(i);
      if (result == null) {
        throw new IllegalArgumentException("Pauli measurement simulation result must not be null.");
      }
      if (!result.isSuccess()) {
        throw new IllegalArgumentException(
            "Pauli measurement simulation result must be successful.");
      }
      termCounts.add(result.counts());
    }
    return this.aggregate(hamiltonian, termCounts);
  }

  private static int nonIdentityTermCount(final QubitPauliOperator hamiltonian) {
    int count = 0;
    for (int i = 0; i < hamiltonian.termCount(); ++i) {
      if (!hamiltonian.term(i).identity()) {
        ++count;
      }
    }
    return count;
  }

  private static TermMeasurementExpectation expectation(
      final QubitPauliTerm term, final Map<String, Long> counts) {
    if (counts == null || counts.isEmpty()) {
      throw new IllegalArgumentException("Pauli measurement counts must not be empty.");
    }
    long shots = 0L;
    double weightedParity = 0.0;
    for (final Map.Entry<String, Long> entry : counts.entrySet()) {
      final String bitString = entry.getKey();
      final long count = entry.getValue() == null ? -1L : entry.getValue();
      if (count < 0L) {
        throw new IllegalArgumentException("Pauli measurement count must not be negative.");
      }
      if (count == 0L) {
        continue;
      }
      final int parity = PauliExpectationMeasurementAggregator.parity(term, bitString);
      shots += count;
      weightedParity += (double) parity * (double) count;
    }
    if (shots <= 0L) {
      throw new IllegalArgumentException("Pauli measurement counts must contain positive shots.");
    }
    return new TermMeasurementExpectation(weightedParity / (double) shots, shots);
  }

  private static int parity(final QubitPauliTerm term, final String bitString) {
    if (bitString == null || bitString.isBlank()) {
      throw new IllegalArgumentException("Pauli measurement bitstring must not be blank.");
    }
    int oneCount = 0;
    for (int i = 0; i < term.length(); ++i) {
      final int qubit = term.qubit(i);
      if (qubit >= bitString.length()) {
        throw new IllegalArgumentException(
            "Pauli measurement bitstring is shorter than required qubit index.");
      }
      final char bit = bitString.charAt(bitString.length() - 1 - qubit);
      if (bit == '1') {
        ++oneCount;
      } else if (bit != '0') {
        throw new IllegalArgumentException(
            "Pauli measurement bitstring contains non-binary character.");
      }
    }
    return (oneCount & 1) == 0 ? 1 : -1;
  }

  private static void requireReal(final ComplexCoefficient coefficient) {
    if (coefficient == null) {
      throw new IllegalArgumentException(
          "Pauli measurement aggregation coefficient must not be null.");
    }
    if (Math.abs(coefficient.imaginary()) > REAL_COEFFICIENT_TOLERANCE) {
      throw new IllegalArgumentException(
          "Pauli measurement aggregation requires real Hamiltonian coefficients.");
    }
  }

  private static final class TermMeasurementExpectation {

    private final double value;
    private final long shots;

    private TermMeasurementExpectation(final double value, final long shots) {
      this.value = value;
      this.shots = shots;
    }
  }
}
