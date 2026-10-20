/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryExcitationRank;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitationPool;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariationalChemistryProgramGeneratorTest {

  @Test
  void generatesHartreeFockReferenceForActiveSpace() {
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.HARTREE_FOCK_REFERENCE,
                    "hf_reference",
                    "spin",
                    "readout",
                    "theta",
                    1,
                    8,
                    true));
    final QuantumCircuit circuit = onlyCircuit(program);

    assertEquals(1, program.circuitCount());
    assertEquals(4, circuit.quantumRegisters().getFirst().size());
    assertEquals(4, circuit.classicalRegisters().getFirst().size());
    assertEquals(6, circuit.operationCount());
    assertTrue(Quantum.validate(program).isValid());
  }

  @Test
  void generatesSpinConservingUccsdWithDeterministicPoolOrder() {
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(ActiveSpace.of(2, 2), VariationalChemistryOptions.defaults());
    final QuantumCircuit circuit = onlyCircuit(program);
    final UccsdExcitationPool pool = UccsdExcitationPool.spinConserving(ActiveSpace.of(2, 2));

    assertEquals(3, pool.excitationCount());
    assertEquals(2, pool.singleExcitationCount());
    assertEquals(1, pool.doubleExcitationCount());
    assertEquals(ChemistryExcitationRank.SINGLE, pool.excitation(0).rank());
    assertEquals(0, pool.excitation(0).occupiedSpinOrbital(0));
    assertEquals(2, pool.excitation(0).virtualSpinOrbital(0));
    assertEquals(ChemistryExcitationRank.DOUBLE, pool.excitation(2).rank());
    assertTrue(circuit.operationCount() > 100);
    assertGate(circuit, 0, StandardGate.X);
    assertTrue(countGate(circuit, StandardGate.CX) > 0);
    assertTrue(countGate(circuit, StandardGate.RZ) > 0);
    assertTrue(countGate(circuit, StandardGate.RX) > 0);
    assertTrue(Quantum.validate(program).isValid());
    assertExportableToOpenQasm3(program);
  }

  @Test
  void adaptOperatorPoolRespectsOperatorLimit() {
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL,
                    "adapt_pool",
                    "q",
                    "c",
                    "a",
                    1,
                    2,
                    false));
    final QuantumCircuit circuit = onlyCircuit(program);

    assertTrue(circuit.operationCount() > 30);
    assertTrue(circuit.operationCount() < 60);
    assertTrue(Quantum.validate(program).isValid());
  }

  @Test
  void mappedFermionicExcitationsProduceHermitianPauliGenerators() {
    final FermionicExcitationEvolutionGenerator generator =
        new FermionicExcitationEvolutionGenerator();
    final QubitPauliOperator single =
        generator.excitationHamiltonian(
            UccsdExcitation.single(0, 2), FermionToQubitMapping.JORDAN_WIGNER, 4);
    final QubitPauliOperator doubles =
        generator.excitationHamiltonian(
            UccsdExcitation.doubleExcitation(0, 1, 2, 3), FermionToQubitMapping.JORDAN_WIGNER, 4);

    assertTrue(single.hermitianWithinTolerance(1.0E-12));
    assertTrue(doubles.hermitianWithinTolerance(1.0E-12));
    assertTrue(single.termCount() > 0);
    assertTrue(doubles.termCount() > single.termCount());
  }

  @Test
  void variationalAnsatzUsesExplicitFermionToQubitMappingOption() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 2);
    final QuantumProgram jordanWignerProgram =
        new VariationalChemistryProgramGenerator()
            .generate(activeSpace, VariationalChemistryOptions.defaults());
    final QuantumProgram bravyiKitaevProgram =
        new VariationalChemistryProgramGenerator()
            .generate(
                activeSpace,
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.UCCSD,
                    "bk_ansatz",
                    "q",
                    "c",
                    "theta",
                    1,
                    8,
                    true,
                    FermionToQubitMapping.BRAVYI_KITAEV));
    final QuantumProgram parityProgram =
        new VariationalChemistryProgramGenerator()
            .generate(
                activeSpace,
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL,
                    "parity_ansatz",
                    "q",
                    "c",
                    "theta",
                    1,
                    2,
                    true,
                    FermionToQubitMapping.PARITY));

    assertTrue(Quantum.validate(jordanWignerProgram).isValid());
    assertTrue(Quantum.validate(bravyiKitaevProgram).isValid());
    assertTrue(Quantum.validate(parityProgram).isValid());
    assertTrue(onlyCircuit(bravyiKitaevProgram).operationCount() > 0);
    assertTrue(onlyCircuit(parityProgram).operationCount() > 0);
    assertFalse(
        Quantum.exportProgram(IntegrationFormat.OPENQASM_3, jordanWignerProgram)
            .content()
            .equals(
                Quantum.exportProgram(IntegrationFormat.OPENQASM_3, bravyiKitaevProgram)
                    .content()));
    assertExportableToOpenQasm3(bravyiKitaevProgram);
    assertExportableToOpenQasm3(parityProgram);
  }

  @Test
  void hardwareEfficientLayersScaleWithQubitsAndLayers() {
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
                    "hardware_efficient",
                    "q",
                    "c",
                    "beta",
                    2,
                    8,
                    false));
    final QuantumCircuit circuit = onlyCircuit(program);

    assertEquals(24, circuit.operationCount());
    assertTrue(Quantum.validate(program).isValid());
  }

  @Test
  void quantumPhaseEstimationUsesSeparatePhaseRegisterAndExports() {
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION,
                    "qpe_program",
                    "system",
                    "phase_readout",
                    "tau",
                    1,
                    8,
                    true,
                    "phase",
                    3,
                    1,
                    true));
    final QuantumCircuit circuit = onlyCircuit(program);

    assertEquals(2, circuit.quantumRegisters().size());
    assertEquals(4, circuit.quantumRegisters().getFirst().size());
    assertEquals(3, circuit.quantumRegisters().get(1).size());
    assertEquals(3, circuit.classicalRegisters().getFirst().size());
    assertEquals(105, circuit.operationCount());
    assertGate(circuit, 0, StandardGate.X);
    assertGate(circuit, 2, StandardGate.H);
    assertTrue(Quantum.validate(program).isValid());
    assertExportableToOpenQasm3(program);
  }

  @Test
  void optionsRejectInvalidIdentifiersBeforeQuantumIrBuilderRuns() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.UCCSD, "bad-circuit-name", "q", "c", "theta", 1, 8, true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.UCCSD, "ansatz", "1q", "c", "theta", 1, 8, true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.UCCSD, "ansatz", "q", "c", "theta.value", 1, 8, true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.UCCSD, "ansatz", "q", "c", "theta", 1, 8, true, null));
  }

  @Test
  void qpeOptionsRejectUnsafeControlledEvolutionExpansion() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION,
                "qpe_large_phase",
                "system",
                "readout",
                "tau",
                1,
                8,
                true,
                "phase",
                31,
                1,
                true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION,
                "qpe_expansion",
                "system",
                "readout",
                "tau",
                1,
                8,
                true,
                "phase",
                20,
                3,
                true));
  }

  @Test
  void spinConservingPoolRejectsInvalidManualExcitations() {
    assertThrows(IllegalArgumentException.class, () -> UccsdExcitation.single(0, 3));
    assertThrows(
        IllegalArgumentException.class, () -> UccsdExcitation.doubleExcitation(0, 0, 2, 4));
    assertThrows(
        IllegalArgumentException.class, () -> UccsdExcitation.doubleExcitation(0, 2, 3, 5));
  }

  @Test
  void largerUccsdPoolKeepsExpectedSpinConservingCardinality() {
    final UccsdExcitationPool pool = UccsdExcitationPool.spinConserving(ActiveSpace.of(4, 4));

    assertEquals(26, pool.excitationCount());
    assertEquals(8, pool.singleExcitationCount());
    assertEquals(18, pool.doubleExcitationCount());
    assertEquals(ChemistryExcitationRank.SINGLE, pool.excitation(7).rank());
    assertEquals(ChemistryExcitationRank.DOUBLE, pool.excitation(8).rank());
  }

  @Test
  void occupiedFullActiveSpaceDoesNotCreateVirtualExcitations() {
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(ActiveSpace.of(4, 2), VariationalChemistryOptions.defaults());
    final QuantumCircuit circuit = onlyCircuit(program);
    final UccsdExcitationPool pool = UccsdExcitationPool.spinConserving(ActiveSpace.of(4, 2));

    assertEquals(0, pool.excitationCount());
    assertEquals(8, circuit.operationCount());
    assertTrue(Quantum.validate(program).isValid());
  }

  private static QuantumCircuit onlyCircuit(final QuantumProgram program) {
    assertEquals(1, program.circuitCount());
    return program.circuits().getFirst();
  }

  private static void assertExportableToOpenQasm3(final QuantumProgram program) {
    final ExportResult result = Quantum.exportProgram(IntegrationFormat.OPENQASM_3, program);

    assertTrue(result.isSuccess(), result.diagnostics().toString());
    assertFalse(result.content().isBlank());
  }

  private static void assertGate(
      final QuantumCircuit circuit, final int operationIndex, final StandardGate expectedGate) {
    final GateOperation operation = (GateOperation) circuit.operations().get(operationIndex);

    assertEquals(expectedGate, operation.gate());
  }

  private static int countGate(final QuantumCircuit circuit, final StandardGate gate) {
    int result = 0;
    for (int i = 0; i < circuit.operationCount(); ++i) {
      if (circuit.operations().get(i) instanceof GateOperation) {
        final GateOperation operation = (GateOperation) circuit.operations().get(i);
        if (operation.gate() == gate) {
          ++result;
        }
      }
    }
    return result;
  }
}
