/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.tapering.QubitZ2TaperingService;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering.QubitTaperingResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.circuit.PauliEvolutionOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/** Строит Quantum IR evolution program из chemistry Hamiltonian. */
public final class ElectronicHamiltonianProgramGenerator {

  private final ElectronicHamiltonianQubitMapper qubitMapper;
  private final QubitZ2TaperingService taperingService;
  private final PauliEvolutionCircuitGenerator circuitGenerator;

  public ElectronicHamiltonianProgramGenerator() {
    this(
        new ElectronicHamiltonianQubitMapper(),
        new QubitZ2TaperingService(),
        new PauliEvolutionCircuitGenerator());
  }

  public ElectronicHamiltonianProgramGenerator(
      final ElectronicHamiltonianQubitMapper qubitMapper,
      final QubitZ2TaperingService taperingService,
      final PauliEvolutionCircuitGenerator circuitGenerator) {
    if (qubitMapper == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian qubit mapper must not be null.");
    }
    if (taperingService == null) {
      throw new IllegalArgumentException("Qubit tapering service must not be null.");
    }
    if (circuitGenerator == null) {
      throw new IllegalArgumentException("Pauli evolution circuit generator must not be null.");
    }
    this.qubitMapper = qubitMapper;
    this.taperingService = taperingService;
    this.circuitGenerator = circuitGenerator;
  }

  public QuantumProgram generateEvolutionProgram(
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping mapping,
      final double evolutionTime,
      final PauliEvolutionOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    final QubitPauliOperator qubitHamiltonian = this.qubitMapper.map(hamiltonian, mapping);
    return this.circuitGenerator.generate(qubitHamiltonian, evolutionTime, options);
  }

  public QuantumProgram generateEvolutionProgram(
      final QubitPauliOperator qubitHamiltonian,
      final double evolutionTime,
      final PauliEvolutionOptions options) {
    if (qubitHamiltonian == null) {
      throw new IllegalArgumentException("Mapped qubit Hamiltonian must not be null.");
    }
    return this.circuitGenerator.generate(qubitHamiltonian, evolutionTime, options);
  }

  public VerifiedElectronicHamiltonianProgram generateVerifiedEvolutionProgram(
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping mapping,
      final double evolutionTime,
      final PauliEvolutionOptions options,
      final double verificationTolerance) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    final VerifiedElectronicHamiltonianMapping verifiedMapping =
        this.qubitMapper.mapVerified(hamiltonian, mapping, verificationTolerance);
    final QuantumProgram program =
        this.circuitGenerator.generate(verifiedMapping.qubitOperator(), evolutionTime, options);
    return VerifiedElectronicHamiltonianProgram.of(verifiedMapping, program);
  }

  public VerifiedElectronicHamiltonianProgram generateVerifiedEvolutionProgram(
      final VerifiedElectronicHamiltonianMapping verifiedMapping,
      final double evolutionTime,
      final PauliEvolutionOptions options) {
    if (verifiedMapping == null) {
      throw new IllegalArgumentException(
          "Verified electronic Hamiltonian mapping must not be null.");
    }
    final QuantumProgram program =
        this.circuitGenerator.generate(verifiedMapping.qubitOperator(), evolutionTime, options);
    return VerifiedElectronicHamiltonianProgram.of(verifiedMapping, program);
  }

  public QuantumProgram generateSpinBlockParityTaperedEvolutionProgram(
      final ElectronicHamiltonian hamiltonian,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double evolutionTime,
      final PauliEvolutionOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    final QubitPauliOperator qubitHamiltonian =
        this.qubitMapper.map(hamiltonian, FermionToQubitMapping.SPIN_BLOCK_PARITY);
    final QubitTaperingResult taperedHamiltonian =
        this.taperingService.taperSpinBlockParity(
            qubitHamiltonian,
            hamiltonian.spinOrbitalCount(),
            alphaElectronCount,
            betaElectronCount);
    return this.circuitGenerator.generate(taperedHamiltonian.operator(), evolutionTime, options);
  }

  /**
   * Quantum IR program вместе с доказательством корректности fermion-to-qubit mapping.
   */
  public static final class VerifiedElectronicHamiltonianProgram {

    private final VerifiedElectronicHamiltonianMapping mapping;
    private final QuantumProgram program;

    private VerifiedElectronicHamiltonianProgram(
        final VerifiedElectronicHamiltonianMapping mapping, final QuantumProgram program) {
      this.mapping = mapping;
      this.program = program;
    }

    private static VerifiedElectronicHamiltonianProgram of(
        final VerifiedElectronicHamiltonianMapping mapping, final QuantumProgram program) {
      if (mapping == null) {
        throw new IllegalArgumentException(
            "Verified electronic Hamiltonian mapping must not be null.");
      }
      if (program == null) {
        throw new IllegalArgumentException(
            "Verified electronic Hamiltonian program must not be null.");
      }
      return new VerifiedElectronicHamiltonianProgram(mapping, program);
    }

    public VerifiedElectronicHamiltonianMapping mapping() {
      return this.mapping;
    }

    public QuantumProgram program() {
      return this.program;
    }

    public boolean verified() {
      return this.mapping.verified();
    }
  }
}
