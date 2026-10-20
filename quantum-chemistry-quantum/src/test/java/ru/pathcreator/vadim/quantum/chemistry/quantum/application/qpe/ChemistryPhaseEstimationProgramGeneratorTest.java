/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.qpe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.qpe.ChemistryPhaseEstimationOptions;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

final class ChemistryPhaseEstimationProgramGeneratorTest {

  @Test
  void generatesHamiltonianDrivenQpeCircuitWithPhaseAndSystemRegisters() {
    final QuantumProgram program =
        new ChemistryPhaseEstimationProgramGenerator()
            .generate(
                ChemistryPhaseEstimationProgramGeneratorTest.hydrogenActiveSpaceHamiltonian(),
                ChemistryPhaseEstimationOptions.of(
                    "h2_qpe",
                    "system",
                    "phase",
                    "readout",
                    FermionToQubitMapping.JORDAN_WIGNER,
                    3,
                    1,
                    0.25,
                    true,
                    true,
                    true));
    final QuantumCircuit circuit =
        ChemistryPhaseEstimationProgramGeneratorTest.onlyCircuit(program);

    assertEquals(2, circuit.quantumRegisterCount());
    assertEquals(4, circuit.quantumRegister(0).size());
    assertEquals(3, circuit.quantumRegister(1).size());
    assertEquals(1, circuit.classicalRegisterCount());
    assertEquals(3, circuit.classicalRegister(0).size());
    assertTrue(circuit.operationCount() > 100);
    ChemistryPhaseEstimationProgramGeneratorTest.assertGate(circuit, 0, StandardGate.X);
    ChemistryPhaseEstimationProgramGeneratorTest.assertGate(circuit, 2, StandardGate.H);
    assertTrue(Quantum.validate(program).isValid());
    ChemistryPhaseEstimationProgramGeneratorTest.assertExportableToOpenQasm3(program);
  }

  @Test
  void optionsRejectAmbiguousRegistersAndUnsafePhaseWidth() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryPhaseEstimationOptions.of(
                "bad_qpe",
                "q",
                "q",
                "c",
                FermionToQubitMapping.JORDAN_WIGNER,
                2,
                1,
                1.0,
                true,
                true,
                true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryPhaseEstimationOptions.of(
                "bad_qpe",
                "q",
                "phase",
                "c",
                FermionToQubitMapping.JORDAN_WIGNER,
                31,
                1,
                1.0,
                true,
                true,
                true));
  }

  @Test
  void optionsRejectInvalidIdentifiersAndUnsafeControlledEvolutionExpansion() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryPhaseEstimationOptions.of(
                "bad-qpe",
                "q",
                "phase",
                "c",
                FermionToQubitMapping.JORDAN_WIGNER,
                2,
                1,
                1.0,
                true,
                true,
                true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryPhaseEstimationOptions.of(
                "qpe",
                "q",
                "phase",
                "c",
                FermionToQubitMapping.JORDAN_WIGNER,
                20,
                2,
                1.0,
                true,
                true,
                true));
  }

  private static ElectronicHamiltonian hydrogenActiveSpaceHamiltonian() {
    return ElectronicHamiltonian.of(
        ActiveSpace.of(2, 2),
        0.713753993687618,
        List.of(
            OneElectronIntegral.of(0, 0, -1.252463573565),
            OneElectronIntegral.of(1, 1, -0.475948715221)),
        List.of(
            TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
            TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
            TwoElectronIntegral.of(0, 1, 0, 1, 0.181288808211),
            TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
  }

  private static QuantumCircuit onlyCircuit(final QuantumProgram program) {
    assertEquals(1, program.circuitCount());
    return program.circuit(0);
  }

  private static void assertExportableToOpenQasm3(final QuantumProgram program) {
    final ExportResult result = Quantum.exportProgram(IntegrationFormat.OPENQASM_3, program);

    assertTrue(result.isSuccess(), result.diagnostics().toString());
    assertFalse(result.content().isBlank());
  }

  private static void assertGate(
      final QuantumCircuit circuit, final int operationIndex, final StandardGate expectedGate) {
    final GateOperation operation = (GateOperation) circuit.operation(operationIndex);

    assertEquals(expectedGate, operation.gate());
  }
}
