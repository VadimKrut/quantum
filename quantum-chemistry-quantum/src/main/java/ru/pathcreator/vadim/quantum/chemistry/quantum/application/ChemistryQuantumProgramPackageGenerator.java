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
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz.VariationalChemistryProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.qpe.ChemistryPhaseEstimationProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.circuit.PauliEvolutionOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackageOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumPreparationProvenance;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramSkip;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.qpe.ChemistryPhaseEstimationOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * ???????? ?????? ????? native Quantum IR ???????? ??? chemistry Hamiltonian ??? ??????? ?????????.
 */
public final class ChemistryQuantumProgramPackageGenerator {

  private static final double DEFAULT_EVOLUTION_TIME = 1.0;
  private static final double DEFAULT_VERIFICATION_TOLERANCE = 1.0E-8;

  private final ElectronicHamiltonianProgramGenerator evolutionGenerator;
  private final PauliExpectationMeasurementProgramGenerator measurementGenerator;
  private final HartreeFockReferenceProgramGenerator referenceGenerator;
  private final VariationalChemistryProgramGenerator ansatzGenerator;
  private final ChemistryPhaseEstimationProgramGenerator qpeGenerator;
  private final ElectronicHamiltonianQubitMapper qubitMapper;

  public ChemistryQuantumProgramPackageGenerator() {
    this(
        new ElectronicHamiltonianProgramGenerator(),
        new PauliExpectationMeasurementProgramGenerator(),
        new HartreeFockReferenceProgramGenerator(),
        new VariationalChemistryProgramGenerator(),
        new ChemistryPhaseEstimationProgramGenerator(),
        new ElectronicHamiltonianQubitMapper());
  }

  public ChemistryQuantumProgramPackageGenerator(
      final ElectronicHamiltonianProgramGenerator evolutionGenerator,
      final VariationalChemistryProgramGenerator ansatzGenerator,
      final ChemistryPhaseEstimationProgramGenerator qpeGenerator) {
    this(
        evolutionGenerator,
        new PauliExpectationMeasurementProgramGenerator(),
        new HartreeFockReferenceProgramGenerator(),
        ansatzGenerator,
        qpeGenerator,
        new ElectronicHamiltonianQubitMapper());
  }

  public ChemistryQuantumProgramPackageGenerator(
      final ElectronicHamiltonianProgramGenerator evolutionGenerator,
      final PauliExpectationMeasurementProgramGenerator measurementGenerator,
      final HartreeFockReferenceProgramGenerator referenceGenerator,
      final VariationalChemistryProgramGenerator ansatzGenerator,
      final ChemistryPhaseEstimationProgramGenerator qpeGenerator,
      final ElectronicHamiltonianQubitMapper qubitMapper) {
    if (evolutionGenerator == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package evolution generator must not be null.");
    }
    if (measurementGenerator == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package measurement generator must not be null.");
    }
    if (referenceGenerator == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package reference generator must not be null.");
    }
    if (ansatzGenerator == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package ansatz generator must not be null.");
    }
    if (qpeGenerator == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package QPE generator must not be null.");
    }
    if (qubitMapper == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package qubit mapper must not be null.");
    }
    this.evolutionGenerator = evolutionGenerator;
    this.measurementGenerator = measurementGenerator;
    this.referenceGenerator = referenceGenerator;
    this.ansatzGenerator = ansatzGenerator;
    this.qpeGenerator = qpeGenerator;
    this.qubitMapper = qubitMapper;
  }

  public ChemistryQuantumProgramPackage generate(
      final String sourceId,
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping requestedMapping) {
    return this.generate(
        sourceId,
        hamiltonian,
        requestedMapping,
        ChemistryQuantumProgramPackageOptions.of(
            ChemistryQuantumProgramPackageGenerator.orderedMappings(requestedMapping),
            java.util.EnumSet.allOf(ChemistryQuantumProgramKind.class),
            DEFAULT_VERIFICATION_TOLERANCE));
  }

  public ChemistryQuantumProgramPackage generate(
      final String sourceId,
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping requestedMapping,
      final ChemistryQuantumProgramPackageOptions options) {
    return this.generate(
        sourceId,
        hamiltonian,
        requestedMapping,
        ChemistryQuantumPreparationProvenance.UNKNOWN,
        options);
  }

  public ChemistryQuantumProgramPackage generate(
      final String sourceId,
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping requestedMapping,
      final ChemistryQuantumPreparationProvenance provenance,
      final ChemistryQuantumProgramPackageOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Chemistry quantum package Hamiltonian must not be null.");
    }
    if (requestedMapping == null) {
      throw new IllegalArgumentException("Chemistry quantum package mapping must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("Chemistry quantum package options must not be null.");
    }
    if (provenance == null) {
      throw new IllegalArgumentException("Chemistry quantum package provenance must not be null.");
    }
    final String checkedSourceId =
        sourceId == null || sourceId.isBlank() ? "chemistry_quantum_package" : sourceId;
    final ArrayList<ChemistryQuantumProgramArtifact> artifacts =
        new ArrayList<ChemistryQuantumProgramArtifact>();
    final ArrayList<ChemistryQuantumProgramSkip> skipped =
        new ArrayList<ChemistryQuantumProgramSkip>();
    final List<FermionToQubitMapping> mappings = options.mappings();
    final boolean mappedHamiltonianRequired =
        options.includes(ChemistryQuantumProgramKind.PAULI_EVOLUTION)
            || options.includes(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION)
            || options.includes(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION)
            || options.includes(ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT);
    final FermionOperator normalOrderedFermionOperator =
        mappedHamiltonianRequired
            ? this.qubitMapper.mapToNormalOrderedFermionOperator(hamiltonian)
            : null;
    for (int i = 0; i < mappings.size(); ++i) {
      final FermionToQubitMapping mapping = mappings.get(i);
      final VerifiedElectronicHamiltonianMapping verifiedMapping =
          options.includes(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION)
              ? this.qubitMapper.mapNormalOrderedVerified(
                  normalOrderedFermionOperator,
                  mapping,
                  hamiltonian.spinOrbitalCount(),
                  options.verificationTolerance())
              : null;
      final QubitPauliOperator mappedHamiltonian =
          mappedHamiltonianRequired && verifiedMapping == null
              ? this.qubitMapper.mapNormalOrdered(
                  normalOrderedFermionOperator, mapping, hamiltonian.spinOrbitalCount())
              : mappedHamiltonian(verifiedMapping);
      if (options.includes(ChemistryQuantumProgramKind.PAULI_EVOLUTION)) {
        this.addEvolution(checkedSourceId, mappedHamiltonian, mapping, artifacts, skipped);
      }
      if (options.includes(ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT)) {
        this.addMeasurement(checkedSourceId, mappedHamiltonian, mapping, artifacts, skipped);
      }
      if (options.includes(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION)) {
        this.addVerifiedEvolution(checkedSourceId, verifiedMapping, artifacts, skipped);
      }
      if (options.includes(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION)) {
        this.addQpe(checkedSourceId, hamiltonian, mappedHamiltonian, mapping, artifacts, skipped);
      }
    }
    if (options.includes(ChemistryQuantumProgramKind.SPIN_BLOCK_PARITY_TAPERED_EVOLUTION)) {
      this.addTaperedEvolution(checkedSourceId, hamiltonian, artifacts, skipped);
    }
    if (options.includes(ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE)) {
      this.addHartreeFockReference(checkedSourceId, hamiltonian.activeSpace(), artifacts, skipped);
    }
    this.addVariationalPrograms(
        checkedSourceId, hamiltonian.activeSpace(), options, artifacts, skipped);
    return ChemistryQuantumProgramPackage.of(
        checkedSourceId,
        requestedMapping,
        hamiltonian.spinOrbitalCount(),
        hamiltonian.activeSpace().electronCount(),
        provenance,
        artifacts,
        skipped);
  }

  private void addEvolution(
      final String sourceId,
      final QubitPauliOperator mappedHamiltonian,
      final FermionToQubitMapping mapping,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    final PauliEvolutionOptions options =
        PauliEvolutionOptions.of(sourceId + "_evolution", "q", "c", 1, true, 1024);
    this.addProgram(
        sourceId
            + "_"
            + ChemistryQuantumProgramPackageGenerator.mappingId(mapping)
            + "_pauli_evolution",
        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
        mapping,
        "Trotterized Pauli evolution program for mapped electronic Hamiltonian.",
        () ->
            this.evolutionGenerator.generateEvolutionProgram(
                mappedHamiltonian, DEFAULT_EVOLUTION_TIME, options),
        artifacts,
        skipped);
  }

  private void addHartreeFockReference(
      final String sourceId,
      final ActiveSpace activeSpace,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    this.addProgram(
        sourceId + "_hartree_fock_reference",
        ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE,
        null,
        "Hartree-Fock Slater determinant reference state for the active-space occupation.",
        () ->
            this.referenceGenerator.generateClosedPrefixReference(
                activeSpace, sourceId + "_hartree_fock_reference", "q", "c", false),
        artifacts,
        skipped);
  }

  private void addMeasurement(
      final String sourceId,
      final QubitPauliOperator mappedHamiltonian,
      final FermionToQubitMapping mapping,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    this.addProgram(
        sourceId
            + "_"
            + ChemistryQuantumProgramPackageGenerator.mappingId(mapping)
            + "_pauli_measurements",
        ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT,
        mapping,
        "Pauli-term measurement circuits for Hamiltonian expectation and VQE energy evaluation.",
        () ->
            this.measurementGenerator.generate(
                mappedHamiltonian, sourceId + "_pauli_measurement", "q", "c"),
        artifacts,
        skipped);
  }

  private void addVerifiedEvolution(
      final String sourceId,
      final VerifiedElectronicHamiltonianMapping verifiedMapping,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    final PauliEvolutionOptions options =
        PauliEvolutionOptions.of(sourceId + "_verified_evolution", "q", "c", 1, true, 1024);
    this.addProgram(
        sourceId
            + "_"
            + ChemistryQuantumProgramPackageGenerator.mappingId(verifiedMapping.mapping())
            + "_verified_pauli_evolution",
        ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION,
        verifiedMapping.mapping(),
        "Pauli evolution program with algebra and spectrum mapping verification.",
        () ->
            this.evolutionGenerator
                .generateVerifiedEvolutionProgram(verifiedMapping, DEFAULT_EVOLUTION_TIME, options)
                .program(),
        artifacts,
        skipped);
  }

  private void addQpe(
      final String sourceId,
      final ElectronicHamiltonian hamiltonian,
      final QubitPauliOperator mappedHamiltonian,
      final FermionToQubitMapping mapping,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    final ChemistryPhaseEstimationOptions options =
        ChemistryPhaseEstimationOptions.of(
            sourceId + "_qpe",
            "q",
            "phase",
            "c",
            mapping,
            4,
            1,
            DEFAULT_EVOLUTION_TIME,
            true,
            true,
            true);
    this.addProgram(
        sourceId
            + "_"
            + ChemistryQuantumProgramPackageGenerator.mappingId(mapping)
            + "_quantum_phase_estimation",
        ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION,
        mapping,
        "Hamiltonian-driven phase estimation program with Hartree-Fock reference.",
        () -> this.qpeGenerator.generate(hamiltonian, mappedHamiltonian, options),
        artifacts,
        skipped);
  }

  private void addTaperedEvolution(
      final String sourceId,
      final ElectronicHamiltonian hamiltonian,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    final int alphaElectronCount = (hamiltonian.activeSpace().electronCount() + 1) / 2;
    final int betaElectronCount = hamiltonian.activeSpace().electronCount() / 2;
    final PauliEvolutionOptions options =
        PauliEvolutionOptions.of(
            sourceId + "_spin_block_tapered_evolution", "q", "c", 1, true, 1024);
    this.addProgram(
        sourceId + "_spin_block_parity_tapered_evolution",
        ChemistryQuantumProgramKind.SPIN_BLOCK_PARITY_TAPERED_EVOLUTION,
        FermionToQubitMapping.SPIN_BLOCK_PARITY,
        "Spin-block parity tapered evolution with electron-parity sectors fixed.",
        () ->
            this.evolutionGenerator.generateSpinBlockParityTaperedEvolutionProgram(
                hamiltonian,
                alphaElectronCount,
                betaElectronCount,
                DEFAULT_EVOLUTION_TIME,
                options),
        artifacts,
        skipped);
  }

  private void addVariationalPrograms(
      final String sourceId,
      final ActiveSpace activeSpace,
      final ChemistryQuantumProgramPackageOptions options,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    if (options.includes(ChemistryQuantumProgramKind.UCCSD_ANSATZ)) {
      this.addAnsatz(
          sourceId,
          activeSpace,
          options,
          ChemistryQuantumProgramKind.UCCSD_ANSATZ,
          ChemistryAnsatzKind.UCCSD,
          "UCCSD variational ansatz with spin-conserving excitation pool.",
          artifacts,
          skipped);
    }
    if (options.includes(ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ)) {
      this.addAnsatz(
          sourceId,
          activeSpace,
          options,
          ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ,
          ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
          "Hardware-efficient VQE ansatz for backend-oriented variational runs.",
          artifacts,
          skipped);
    }
    if (options.includes(ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ)) {
      this.addAnsatz(
          sourceId,
          activeSpace,
          options,
          ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ,
          ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL,
          "ADAPT-VQE operator-pool ansatz with bounded spin-conserving excitations.",
          artifacts,
          skipped);
    }
  }

  private void addAnsatz(
      final String sourceId,
      final ActiveSpace activeSpace,
      final ChemistryQuantumProgramPackageOptions options,
      final ChemistryQuantumProgramKind programKind,
      final ChemistryAnsatzKind ansatzKind,
      final String description,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    final String normalizedKind = ansatzKind.name().toLowerCase(java.util.Locale.ROOT);
    final VariationalChemistryOptions variationalOptions =
        VariationalChemistryOptions.of(
            ansatzKind,
            sourceId + "_" + normalizedKind,
            "q",
            "c",
            "theta",
            1,
            options.adaptOperatorLimit(),
            true);
    this.addProgram(
        sourceId + "_" + normalizedKind,
        programKind,
        null,
        description,
        () -> this.ansatzGenerator.generate(activeSpace, variationalOptions),
        artifacts,
        skipped);
  }

  private void addProgram(
      final String id,
      final ChemistryQuantumProgramKind kind,
      final FermionToQubitMapping mapping,
      final String description,
      final QuantumProgramSupplier supplier,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skipped) {
    try {
      final QuantumProgram program = supplier.get();
      artifacts.add(ChemistryQuantumProgramArtifact.of(id, kind, mapping, description, program));
    } catch (final RuntimeException exception) {
      skipped.add(
          ChemistryQuantumProgramSkip.of(
              id,
              kind,
              mapping,
              exception.getMessage() == null
                  ? exception.getClass().getSimpleName()
                  : exception.getMessage()));
    }
  }

  private static List<FermionToQubitMapping> orderedMappings(
      final FermionToQubitMapping requestedMapping) {
    final ArrayList<FermionToQubitMapping> mappings =
        new ArrayList<FermionToQubitMapping>(FermionToQubitMapping.values().length);
    mappings.add(requestedMapping);
    final FermionToQubitMapping[] values = FermionToQubitMapping.values();
    for (int i = 0; i < values.length; ++i) {
      if (values[i] != requestedMapping) {
        mappings.add(values[i]);
      }
    }
    return List.copyOf(mappings);
  }

  private static QubitPauliOperator mappedHamiltonian(
      final VerifiedElectronicHamiltonianMapping verifiedMapping) {
    if (verifiedMapping == null) {
      return null;
    }
    return verifiedMapping.qubitOperator();
  }

  private static String mappingId(final FermionToQubitMapping mapping) {
    return mapping.name().toLowerCase(java.util.Locale.ROOT);
  }

  private interface QuantumProgramSupplier {
    QuantumProgram get();
  }
}
