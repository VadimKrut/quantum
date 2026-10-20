/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz;

import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionToQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.PauliEvolutionCircuitGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryExcitationRank;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitation;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;

/** Строит UCC excitation evolution через fermion operator mapping, а не через ручной gate шаблон. */
public final class FermionicExcitationEvolutionGenerator {

  private static final ComplexCoefficient POSITIVE_I = ComplexCoefficient.of(0.0, 1.0);
  private static final ComplexCoefficient NEGATIVE_I = ComplexCoefficient.of(0.0, -1.0);

  private final FermionToQubitMapper mapper;
  private final PauliEvolutionCircuitGenerator evolutionCircuitGenerator;

  public FermionicExcitationEvolutionGenerator() {
    this(new FermionToQubitMapper(), new PauliEvolutionCircuitGenerator());
  }

  public FermionicExcitationEvolutionGenerator(
      final FermionToQubitMapper mapper,
      final PauliEvolutionCircuitGenerator evolutionCircuitGenerator) {
    if (mapper == null) {
      throw new IllegalArgumentException("Fermionic excitation mapper must not be null.");
    }
    if (evolutionCircuitGenerator == null) {
      throw new IllegalArgumentException(
          "Fermionic excitation evolution generator must not be null.");
    }
    this.mapper = mapper;
    this.evolutionCircuitGenerator = evolutionCircuitGenerator;
  }

  public QubitPauliOperator excitationHamiltonian(
      final UccsdExcitation excitation,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount) {
    if (excitation == null) {
      throw new IllegalArgumentException("UCC excitation must not be null.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("UCC fermion-to-qubit mapping must not be null.");
    }
    if (spinOrbitalCount <= 0) {
      throw new IllegalArgumentException("UCC spin-orbital count must be positive.");
    }
    final FermionOperator antiHermitianGenerator =
        FermionOperator.builder()
            .add(this.forwardExcitationTerm(excitation), POSITIVE_I)
            .add(this.reverseExcitationTerm(excitation), NEGATIVE_I)
            .build();
    final QubitPauliOperator hamiltonian =
        this.mapper.map(antiHermitianGenerator, mapping, spinOrbitalCount);
    if (!hamiltonian.hermitianWithinTolerance(1.0E-10)) {
      throw new IllegalArgumentException("Mapped UCC excitation Hamiltonian must be Hermitian.");
    }
    return hamiltonian;
  }

  public void appendExcitationEvolution(
      final QuantumCircuitBuilder circuitBuilder,
      final UccsdExcitation excitation,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount,
      final ParameterExpression parameter,
      final String quantumRegisterName) {
    this.evolutionCircuitGenerator.appendParameterizedEvolution(
        circuitBuilder,
        this.excitationHamiltonian(excitation, mapping, spinOrbitalCount),
        parameter,
        quantumRegisterName);
  }

  private FermionTerm forwardExcitationTerm(final UccsdExcitation excitation) {
    if (excitation.rank() == ChemistryExcitationRank.SINGLE) {
      return FermionTerm.of(
          FermionTerm.creation(excitation.virtualSpinOrbital(0)),
          FermionTerm.annihilation(excitation.occupiedSpinOrbital(0)));
    }
    return FermionTerm.of(
        FermionTerm.creation(excitation.virtualSpinOrbital(0)),
        FermionTerm.creation(excitation.virtualSpinOrbital(1)),
        FermionTerm.annihilation(excitation.occupiedSpinOrbital(1)),
        FermionTerm.annihilation(excitation.occupiedSpinOrbital(0)));
  }

  private FermionTerm reverseExcitationTerm(final UccsdExcitation excitation) {
    return this.forwardExcitationTerm(excitation).hermitianConjugate();
  }
}
