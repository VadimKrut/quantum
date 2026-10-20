/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.api.QuantumProgramBuilder;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionSlaterDeterminantExpectationEstimator;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/** Строит Quantum IR подготовку Slater determinant reference из occupation vector. */
public final class HartreeFockReferenceProgramGenerator {

  public QuantumProgram generateClosedPrefixReference(
      final ActiveSpace activeSpace,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final boolean measure) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Hartree-Fock reference active space must not be null.");
    }
    return this.generateOccupationReference(
        FermionSlaterDeterminantExpectationEstimator.closedPrefixOccupation(
            activeSpace.spinOrbitalCount(), activeSpace.electronCount()),
        circuitName,
        quantumRegisterName,
        classicalRegisterName,
        measure);
  }

  public QuantumProgram generateOccupationReference(
      final boolean[] occupation,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final boolean measure) {
    HartreeFockReferenceProgramGenerator.requireOccupation(occupation);
    final String checkedCircuitName =
        circuitName == null || circuitName.isBlank() ? "hartree_fock_reference" : circuitName;
    final String checkedQuantumRegisterName =
        quantumRegisterName == null || quantumRegisterName.isBlank() ? "q" : quantumRegisterName;
    final String checkedClassicalRegisterName =
        classicalRegisterName == null || classicalRegisterName.isBlank()
            ? "c"
            : classicalRegisterName;
    final QuantumProgramBuilder programBuilder = Quantum.programBuilder();
    final QuantumCircuitBuilder circuitBuilder =
        programBuilder
            .circuit(checkedCircuitName)
            .qreg(checkedQuantumRegisterName, occupation.length)
            .creg(checkedClassicalRegisterName, occupation.length);
    for (int i = 0; i < occupation.length; ++i) {
      if (occupation[i]) {
        circuitBuilder.x(checkedQuantumRegisterName + "[" + i + "]");
      }
    }
    if (measure) {
      for (int i = 0; i < occupation.length; ++i) {
        circuitBuilder.measure(
            checkedQuantumRegisterName + "[" + i + "]",
            checkedClassicalRegisterName + "[" + i + "]");
      }
    }
    return circuitBuilder.build();
  }

  private static void requireOccupation(final boolean[] occupation) {
    if (occupation == null || occupation.length == 0) {
      throw new IllegalArgumentException("Hartree-Fock reference occupation must not be empty.");
    }
  }
}
