/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.validation.ElectronicHamiltonianReadinessAuditor;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification.MappingAlgebraVerification;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification.MappingSpectralVerification;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianAuditReport;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnostic;

/** Facade для полного пути ElectronicHamiltonian -> FermionOperator -> QubitPauliOperator. */
public final class ElectronicHamiltonianQubitMapper {

  private final ElectronicHamiltonianFermionMapper fermionMapper;
  private final FermionToQubitMapper qubitMapper;
  private final ElectronicHamiltonianReadinessAuditor hamiltonianAuditor;
  private final MappingAlgebraVerification algebraVerification;
  private final MappingSpectralVerification spectralVerification;

  public ElectronicHamiltonianQubitMapper() {
    this(
        new ElectronicHamiltonianFermionMapper(),
        new FermionToQubitMapper(),
        new ElectronicHamiltonianReadinessAuditor(),
        new MappingAlgebraVerification(),
        new MappingSpectralVerification());
  }

  public ElectronicHamiltonianQubitMapper(
      final ElectronicHamiltonianFermionMapper fermionMapper,
      final FermionToQubitMapper qubitMapper,
      final ElectronicHamiltonianReadinessAuditor hamiltonianAuditor,
      final MappingAlgebraVerification algebraVerification,
      final MappingSpectralVerification spectralVerification) {
    if (fermionMapper == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian fermion mapper must not be null.");
    }
    if (qubitMapper == null) {
      throw new IllegalArgumentException("Fermion-to-qubit mapper must not be null.");
    }
    if (hamiltonianAuditor == null) {
      throw new IllegalArgumentException("Hamiltonian readiness auditor must not be null.");
    }
    if (algebraVerification == null) {
      throw new IllegalArgumentException("Mapping algebra verification must not be null.");
    }
    if (spectralVerification == null) {
      throw new IllegalArgumentException("Mapping spectral verification must not be null.");
    }
    this.fermionMapper = fermionMapper;
    this.qubitMapper = qubitMapper;
    this.hamiltonianAuditor = hamiltonianAuditor;
    this.algebraVerification = algebraVerification;
    this.spectralVerification = spectralVerification;
  }

  public QubitPauliOperator map(
      final ElectronicHamiltonian hamiltonian, final FermionToQubitMapping mapping) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    this.requireReadyForMapping(this.hamiltonianAuditor.audit(hamiltonian));
    final FermionOperator fermionOperator = this.fermionMapper.map(hamiltonian);
    return this.qubitMapper.map(fermionOperator, mapping, hamiltonian.spinOrbitalCount());
  }

  public QubitPauliOperator mapNormalOrdered(
      final ElectronicHamiltonian hamiltonian, final FermionToQubitMapping mapping) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    this.requireReadyForMapping(this.hamiltonianAuditor.audit(hamiltonian));
    final FermionOperator fermionOperator = this.fermionMapper.mapNormalOrdered(hamiltonian);
    return this.qubitMapper.map(fermionOperator, mapping, hamiltonian.spinOrbitalCount());
  }

  public FermionOperator mapToNormalOrderedFermionOperator(
      final ElectronicHamiltonian hamiltonian) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    this.requireReadyForMapping(this.hamiltonianAuditor.audit(hamiltonian));
    return this.fermionMapper.mapNormalOrdered(hamiltonian);
  }

  public QubitPauliOperator mapNormalOrdered(
      final FermionOperator normalOrderedFermionOperator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount) {
    ElectronicHamiltonianQubitMapper.requirePreparedFermionOperator(normalOrderedFermionOperator);
    ElectronicHamiltonianQubitMapper.requireMapping(mapping);
    ElectronicHamiltonianQubitMapper.requireSpinOrbitalCount(spinOrbitalCount);
    return this.qubitMapper.map(normalOrderedFermionOperator, mapping, spinOrbitalCount);
  }

  public VerifiedElectronicHamiltonianMapping mapVerified(
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping mapping,
      final double tolerance) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("Fermion-to-qubit mapping must not be null.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Verified mapping tolerance must be finite and non-negative.");
    }
    final FermionHamiltonianAuditReport hamiltonianAudit =
        this.hamiltonianAuditor.audit(hamiltonian);
    this.requireReadyForMapping(hamiltonianAudit);
    final FermionOperator fermionOperator = this.fermionMapper.map(hamiltonian);
    final QubitPauliOperator qubitOperator =
        this.qubitMapper.map(fermionOperator, mapping, hamiltonian.spinOrbitalCount());
    final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult =
        this.algebraVerification.verify(mapping, hamiltonian.spinOrbitalCount(), tolerance);
    final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult =
        this.verifySpectralOrStructural(
            fermionOperator, qubitOperator, mapping, hamiltonian.spinOrbitalCount(), tolerance);
    return VerifiedElectronicHamiltonianMapping.of(
        mapping, hamiltonianAudit, fermionOperator, qubitOperator, algebraResult, spectralResult);
  }

  public VerifiedElectronicHamiltonianMapping mapNormalOrderedVerified(
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping mapping,
      final double tolerance) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("Fermion-to-qubit mapping must not be null.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Verified mapping tolerance must be finite and non-negative.");
    }
    final FermionHamiltonianAuditReport hamiltonianAudit =
        this.hamiltonianAuditor.audit(hamiltonian);
    this.requireReadyForMapping(hamiltonianAudit);
    final FermionOperator fermionOperator = this.fermionMapper.mapNormalOrdered(hamiltonian);
    final QubitPauliOperator qubitOperator =
        this.qubitMapper.map(fermionOperator, mapping, hamiltonian.spinOrbitalCount());
    final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult =
        this.algebraVerification.verify(mapping, hamiltonian.spinOrbitalCount(), tolerance);
    final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult =
        this.verifySpectralOrStructural(
            fermionOperator, qubitOperator, mapping, hamiltonian.spinOrbitalCount(), tolerance);
    return VerifiedElectronicHamiltonianMapping.of(
        mapping, hamiltonianAudit, fermionOperator, qubitOperator, algebraResult, spectralResult);
  }

  public VerifiedElectronicHamiltonianMapping mapNormalOrderedVerified(
      final FermionOperator normalOrderedFermionOperator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount,
      final double tolerance) {
    ElectronicHamiltonianQubitMapper.requirePreparedFermionOperator(normalOrderedFermionOperator);
    ElectronicHamiltonianQubitMapper.requireMapping(mapping);
    ElectronicHamiltonianQubitMapper.requireSpinOrbitalCount(spinOrbitalCount);
    ElectronicHamiltonianQubitMapper.requireTolerance(tolerance);
    final QubitPauliOperator qubitOperator =
        this.qubitMapper.map(normalOrderedFermionOperator, mapping, spinOrbitalCount);
    final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult =
        this.algebraVerification.verify(mapping, spinOrbitalCount, tolerance);
    final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult =
        this.verifySpectralOrStructural(
            normalOrderedFermionOperator, qubitOperator, mapping, spinOrbitalCount, tolerance);
    return VerifiedElectronicHamiltonianMapping.of(
        mapping, normalOrderedFermionOperator, qubitOperator, algebraResult, spectralResult);
  }

  public List<VerifiedElectronicHamiltonianMapping> mapNormalOrderedVerified(
      final ElectronicHamiltonian hamiltonian,
      final List<FermionToQubitMapping> mappings,
      final double tolerance) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    if (mappings == null || mappings.isEmpty()) {
      throw new IllegalArgumentException("Fermion-to-qubit mappings must not be empty.");
    }
    ElectronicHamiltonianQubitMapper.requireTolerance(tolerance);
    final FermionOperator fermionOperator = this.fermionMapper.mapNormalOrdered(hamiltonian);
    final ArrayList<VerifiedElectronicHamiltonianMapping> verifiedMappings =
        new ArrayList<VerifiedElectronicHamiltonianMapping>(mappings.size());
    for (int i = 0; i < mappings.size(); ++i) {
      verifiedMappings.add(
          this.mapNormalOrderedVerified(
              fermionOperator, mappings.get(i), hamiltonian.spinOrbitalCount(), tolerance));
    }
    return List.copyOf(verifiedMappings);
  }

  private static void requirePreparedFermionOperator(
      final FermionOperator normalOrderedFermionOperator) {
    if (normalOrderedFermionOperator == null) {
      throw new IllegalArgumentException("Normal-ordered fermion operator must not be null.");
    }
  }

  private static void requireMapping(final FermionToQubitMapping mapping) {
    if (mapping == null) {
      throw new IllegalArgumentException("Fermion-to-qubit mapping must not be null.");
    }
  }

  private static void requireSpinOrbitalCount(final int spinOrbitalCount) {
    if (spinOrbitalCount <= 0) {
      throw new IllegalArgumentException("Spin orbital count must be positive.");
    }
  }

  private static void requireTolerance(final double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Verified mapping tolerance must be finite and non-negative.");
    }
  }

  private void requireReadyForMapping(final FermionHamiltonianAuditReport report) {
    if (report.readyForFermionMapping()) {
      return;
    }
    throw new IllegalArgumentException(
        "Electronic Hamiltonian is not ready for fermion/qubit mapping: "
            + ElectronicHamiltonianQubitMapper.firstErrorDiagnostic(report));
  }

  private static String firstErrorDiagnostic(final FermionHamiltonianAuditReport report) {
    final List<FermionHamiltonianDiagnostic> diagnostics = report.diagnostics();
    for (int i = 0; i < diagnostics.size(); ++i) {
      final FermionHamiltonianDiagnostic diagnostic = diagnostics.get(i);
      if (diagnostic.severity().name().equals("ERROR")) {
        return diagnostic.code().name()
            + " at "
            + diagnostic.target()
            + ": "
            + diagnostic.message();
      }
    }
    return "unknown Hamiltonian audit error";
  }

  private MappingSpectralVerification.MappingSpectralVerificationResult verifySpectralOrStructural(
      final FermionOperator fermionOperator,
      final QubitPauliOperator qubitOperator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount,
      final double tolerance) {
    if (spinOrbitalCount <= 8) {
      return this.spectralVerification.verifyExhaustiveSmallSpace(
          fermionOperator, mapping, spinOrbitalCount, tolerance);
    }
    return this.spectralVerification.verifyStructuralLargeSpace(
        fermionOperator, qubitOperator, mapping, spinOrbitalCount, tolerance);
  }

  /**
   * Результат построения qubit Hamiltonian вместе с независимыми проверками mapping.
   */
  public static final class VerifiedElectronicHamiltonianMapping {

    private final FermionToQubitMapping mapping;
    private final FermionHamiltonianAuditReport hamiltonianAudit;
    private final FermionOperator fermionOperator;
    private final QubitPauliOperator qubitOperator;
    private final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult;
    private final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult;

    private VerifiedElectronicHamiltonianMapping(
        final FermionToQubitMapping mapping,
        final FermionHamiltonianAuditReport hamiltonianAudit,
        final FermionOperator fermionOperator,
        final QubitPauliOperator qubitOperator,
        final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult,
        final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult) {
      this.mapping = mapping;
      this.hamiltonianAudit = hamiltonianAudit;
      this.fermionOperator = fermionOperator;
      this.qubitOperator = qubitOperator;
      this.algebraResult = algebraResult;
      this.spectralResult = spectralResult;
    }

    private static VerifiedElectronicHamiltonianMapping of(
        final FermionToQubitMapping mapping,
        final FermionHamiltonianAuditReport hamiltonianAudit,
        final FermionOperator fermionOperator,
        final QubitPauliOperator qubitOperator,
        final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult,
        final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult) {
      if (mapping == null) {
        throw new IllegalArgumentException("Verified Hamiltonian mapping must not be null.");
      }
      if (hamiltonianAudit == null) {
        throw new IllegalArgumentException("Verified Hamiltonian audit report must not be null.");
      }
      if (fermionOperator == null) {
        throw new IllegalArgumentException(
            "Verified Hamiltonian fermion operator must not be null.");
      }
      if (qubitOperator == null) {
        throw new IllegalArgumentException("Verified Hamiltonian qubit operator must not be null.");
      }
      if (algebraResult == null) {
        throw new IllegalArgumentException("Verified Hamiltonian algebra result must not be null.");
      }
      if (spectralResult == null) {
        throw new IllegalArgumentException(
            "Verified Hamiltonian spectral result must not be null.");
      }
      return new VerifiedElectronicHamiltonianMapping(
          mapping, hamiltonianAudit, fermionOperator, qubitOperator, algebraResult, spectralResult);
    }

    private static VerifiedElectronicHamiltonianMapping of(
        final FermionToQubitMapping mapping,
        final FermionOperator fermionOperator,
        final QubitPauliOperator qubitOperator,
        final MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult,
        final MappingSpectralVerification.MappingSpectralVerificationResult spectralResult) {
      if (mapping == null) {
        throw new IllegalArgumentException("Verified Hamiltonian mapping must not be null.");
      }
      if (fermionOperator == null) {
        throw new IllegalArgumentException(
            "Verified Hamiltonian fermion operator must not be null.");
      }
      if (qubitOperator == null) {
        throw new IllegalArgumentException("Verified Hamiltonian qubit operator must not be null.");
      }
      if (algebraResult == null) {
        throw new IllegalArgumentException("Verified Hamiltonian algebra result must not be null.");
      }
      if (spectralResult == null) {
        throw new IllegalArgumentException(
            "Verified Hamiltonian spectral result must not be null.");
      }
      return new VerifiedElectronicHamiltonianMapping(
          mapping, null, fermionOperator, qubitOperator, algebraResult, spectralResult);
    }

    public FermionToQubitMapping mapping() {
      return this.mapping;
    }

    public FermionHamiltonianAuditReport hamiltonianAudit() {
      if (this.hamiltonianAudit == null) {
        throw new IllegalStateException(
            "Verified mapping does not contain source Hamiltonian audit.");
      }
      return this.hamiltonianAudit;
    }

    public boolean hasHamiltonianAudit() {
      return this.hamiltonianAudit != null;
    }

    public FermionOperator fermionOperator() {
      return this.fermionOperator;
    }

    public QubitPauliOperator qubitOperator() {
      return this.qubitOperator;
    }

    public MappingAlgebraVerification.MappingAlgebraVerificationResult algebraResult() {
      return this.algebraResult;
    }

    public MappingSpectralVerification.MappingSpectralVerificationResult spectralResult() {
      return this.spectralResult;
    }

    public boolean verified() {
      return (this.hamiltonianAudit == null || this.hamiltonianAudit.readyForFermionMapping())
          && this.algebraResult.validWithinTolerance()
          && this.spectralResult.equivalentWithinTolerance();
    }
  }
}
