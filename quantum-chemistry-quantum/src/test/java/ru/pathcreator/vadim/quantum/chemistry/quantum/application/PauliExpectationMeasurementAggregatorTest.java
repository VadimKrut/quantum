/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.measurement.PauliExpectationMeasurementResult;

final class PauliExpectationMeasurementAggregatorTest {

  @Test
  void aggregatorReconstructsEnergyFromPauliTermCountsWithMsbFirstBitstrings() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.IDENTITY, 1.25)
            .add(QubitPauliTerm.of(0, PauliAxis.Z), 0.5)
            .add(QubitPauliTerm.of(1, PauliAxis.Z), -0.25)
            .add(
                QubitPauliTerm.of(new int[] {0, 2}, new PauliAxis[] {PauliAxis.Z, PauliAxis.Z}),
                0.75)
            .build();
    final PauliExpectationMeasurementResult result =
        new PauliExpectationMeasurementAggregator()
            .aggregate(
                hamiltonian,
                List.of(
                    Map.of("0", 75L, "1", 25L),
                    Map.of("00", 20L, "10", 80L),
                    Map.of("000", 70L, "100", 30L)));

    assertEquals(1.25, result.identityOffset(), 1.0E-12);
    assertEquals(1.95, result.energy(), 1.0E-12);
    assertArrayEquals(new double[] {0.5, -0.6, 0.4}, result.termExpectationsCopy(), 1.0E-12);
    assertArrayEquals(new double[] {0.25, 0.15, 0.3}, result.termContributionsCopy(), 1.0E-12);
    assertEquals(100L, result.termShots(0));
    assertEquals(100L, result.termShots(1));
    assertEquals(100L, result.termShots(2));
  }

  @Test
  void aggregatorAcceptsSuccessfulSimulationResultsAsMeasurementSource() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder().add(QubitPauliTerm.of(0, PauliAxis.Z), 2.0).build();
    final SimulationResult simulation =
        SimulationResult.of(1, 1, 100, Map.of("0", 80L, "1", 20L), List.of(), List.of());
    final PauliExpectationMeasurementResult result =
        new PauliExpectationMeasurementAggregator()
            .aggregateSimulationResults(hamiltonian, List.of(simulation));

    assertEquals(1.2, result.energy(), 1.0E-12);
    assertEquals(0.6, result.termExpectation(0), 1.0E-12);
    assertEquals(1.2, result.termContribution(0), 1.0E-12);
  }

  @Test
  void aggregatorRejectsCountSetCountDifferentFromNonIdentityTerms() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Z), 1.0)
            .add(QubitPauliTerm.of(1, PauliAxis.Z), 1.0)
            .build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PauliExpectationMeasurementAggregator()
                .aggregate(hamiltonian, List.of(Map.of("0", 1L))));
  }

  @Test
  void aggregatorRejectsComplexHamiltonianCoefficient() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Z), ComplexCoefficient.of(1.0, 0.01))
            .build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PauliExpectationMeasurementAggregator()
                .aggregate(hamiltonian, List.of(Map.of("0", 1L))));
  }

  @Test
  void aggregatorRejectsMalformedCountsBeforeReturningEnergy() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder().add(QubitPauliTerm.of(2, PauliAxis.Z), 1.0).build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PauliExpectationMeasurementAggregator()
                .aggregate(hamiltonian, List.of(Map.of("01", 10L))));
  }
}
