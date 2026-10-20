/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.circuit.PauliEvolutionOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

final class ElectronicHamiltonianProgramGeneratorTest {

  @Test
  void verifiedEvolutionProgramContainsValidQuantumIrAndMappingProof() {
    final ElectronicHamiltonianProgramGenerator generator =
        new ElectronicHamiltonianProgramGenerator();
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonianProgramGeneratorTest.hydrogenActiveSpaceHamiltonian();
    final PauliEvolutionOptions options =
        PauliEvolutionOptions.of("h2_verified_evolution", "q", "c", 1, true, 1024);

    final ElectronicHamiltonianProgramGenerator.VerifiedElectronicHamiltonianProgram result =
        generator.generateVerifiedEvolutionProgram(
            hamiltonian, FermionToQubitMapping.BRAVYI_KITAEV, 0.125, options, 1.0E-8);

    assertTrue(result.verified());
    assertTrue(result.mapping().algebraResult().validWithinTolerance());
    assertTrue(result.mapping().spectralResult().equivalentWithinTolerance());
    assertTrue(Quantum.validate(result.program()).isValid());
    assertEquals(
        4, ElectronicHamiltonianProgramGeneratorTest.firstQubitRegisterSize(result.program()));
  }

  @Test
  void spinBlockParityTaperedEvolutionProgramKeepsValidQuantumIrAndReducesQubits() {
    final ElectronicHamiltonianProgramGenerator generator =
        new ElectronicHamiltonianProgramGenerator();
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonianProgramGeneratorTest.hydrogenActiveSpaceHamiltonian();
    final PauliEvolutionOptions options =
        PauliEvolutionOptions.of("h2_evolution", "q", "c", 1, true, 1024);

    final QuantumProgram untaperedProgram =
        generator.generateEvolutionProgram(
            hamiltonian, FermionToQubitMapping.SPIN_BLOCK_PARITY, 0.125, options);
    final QuantumProgram taperedProgram =
        generator.generateSpinBlockParityTaperedEvolutionProgram(hamiltonian, 1, 1, 0.125, options);

    assertTrue(Quantum.validate(untaperedProgram).isValid());
    assertTrue(Quantum.validate(taperedProgram).isValid());
    assertEquals(
        4, ElectronicHamiltonianProgramGeneratorTest.firstQubitRegisterSize(untaperedProgram));
    assertEquals(
        2, ElectronicHamiltonianProgramGeneratorTest.firstQubitRegisterSize(taperedProgram));
    assertTrue(
        ElectronicHamiltonianProgramGeneratorTest.firstCircuit(taperedProgram).operationCount()
            > 0);
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

  private static int firstQubitRegisterSize(final QuantumProgram program) {
    return ElectronicHamiltonianProgramGeneratorTest.firstCircuit(program)
        .quantumRegister(0)
        .size();
  }

  private static QuantumCircuit firstCircuit(final QuantumProgram program) {
    assertEquals(1, program.circuitCount());
    return program.circuit(0);
  }
}
