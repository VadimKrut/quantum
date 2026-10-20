/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicStructureProblem;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackageOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;

/**
 * Полная постановка chemistry workflow для API, CLI и desktop.
 */
public final class ChemistryWorkflowJob {

  private final ChemistryWorkflowJobId id;
  private final ChemistryWorkflowMode mode;
  private final ChemistrySubject subject;
  private final ClassicalCalculationRequest classicalRequest;
  private final ClassicalBackendProfile classicalProfile;
  private final ReactionPredictionRequest reactionPredictionRequest;
  private final ElectronicStructureProblem electronicProblem;
  private final FermionToQubitMapping fermionToQubitMapping;
  private final ErrorMitigationRequest errorMitigationRequest;
  private final VqeOptimizationRequest vqeOptimizationRequest;
  private final ChemistryQuantumProgramPackageOptions quantumProgramPackageOptions;

  private ChemistryWorkflowJob(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ReactionPredictionRequest reactionPredictionRequest,
      final ElectronicStructureProblem electronicProblem,
      final FermionToQubitMapping fermionToQubitMapping,
      final ErrorMitigationRequest errorMitigationRequest,
      final VqeOptimizationRequest vqeOptimizationRequest,
      final ChemistryQuantumProgramPackageOptions quantumProgramPackageOptions) {
    this.id = id;
    this.mode = mode;
    this.subject = subject;
    this.classicalRequest = classicalRequest;
    this.classicalProfile = classicalProfile;
    this.reactionPredictionRequest = reactionPredictionRequest;
    this.electronicProblem = electronicProblem;
    this.fermionToQubitMapping = fermionToQubitMapping;
    this.errorMitigationRequest = errorMitigationRequest;
    this.vqeOptimizationRequest = vqeOptimizationRequest;
    this.quantumProgramPackageOptions = quantumProgramPackageOptions;
  }

  public static ChemistryWorkflowJob of(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ElectronicStructureProblem electronicProblem) {
    return ChemistryWorkflowJob.of(
        id,
        mode,
        subject,
        classicalRequest,
        classicalProfile,
        null,
        electronicProblem,
        FermionToQubitMapping.JORDAN_WIGNER,
        null,
        null,
        null);
  }

  public static ChemistryWorkflowJob of(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ReactionPredictionRequest reactionPredictionRequest,
      final ElectronicStructureProblem electronicProblem) {
    return ChemistryWorkflowJob.of(
        id,
        mode,
        subject,
        classicalRequest,
        classicalProfile,
        reactionPredictionRequest,
        electronicProblem,
        FermionToQubitMapping.JORDAN_WIGNER,
        null,
        null,
        null);
  }

  public static ChemistryWorkflowJob of(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ReactionPredictionRequest reactionPredictionRequest,
      final ElectronicStructureProblem electronicProblem,
      final FermionToQubitMapping fermionToQubitMapping) {
    return ChemistryWorkflowJob.of(
        id,
        mode,
        subject,
        classicalRequest,
        classicalProfile,
        reactionPredictionRequest,
        electronicProblem,
        fermionToQubitMapping,
        null,
        null,
        null);
  }

  public static ChemistryWorkflowJob of(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ReactionPredictionRequest reactionPredictionRequest,
      final ElectronicStructureProblem electronicProblem,
      final FermionToQubitMapping fermionToQubitMapping,
      final ErrorMitigationRequest errorMitigationRequest) {
    return ChemistryWorkflowJob.of(
        id,
        mode,
        subject,
        classicalRequest,
        classicalProfile,
        reactionPredictionRequest,
        electronicProblem,
        fermionToQubitMapping,
        errorMitigationRequest,
        null,
        null);
  }

  public static ChemistryWorkflowJob of(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ReactionPredictionRequest reactionPredictionRequest,
      final ElectronicStructureProblem electronicProblem,
      final FermionToQubitMapping fermionToQubitMapping,
      final ErrorMitigationRequest errorMitigationRequest,
      final VqeOptimizationRequest vqeOptimizationRequest) {
    return ChemistryWorkflowJob.of(
        id,
        mode,
        subject,
        classicalRequest,
        classicalProfile,
        reactionPredictionRequest,
        electronicProblem,
        fermionToQubitMapping,
        errorMitigationRequest,
        vqeOptimizationRequest,
        null);
  }

  public static ChemistryWorkflowJob of(
      final ChemistryWorkflowJobId id,
      final ChemistryWorkflowMode mode,
      final ChemistrySubject subject,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ReactionPredictionRequest reactionPredictionRequest,
      final ElectronicStructureProblem electronicProblem,
      final FermionToQubitMapping fermionToQubitMapping,
      final ErrorMitigationRequest errorMitigationRequest,
      final VqeOptimizationRequest vqeOptimizationRequest,
      final ChemistryQuantumProgramPackageOptions quantumProgramPackageOptions) {
    if (id == null) {
      throw new IllegalArgumentException("Chemistry workflow job id must not be null.");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Chemistry workflow mode must not be null.");
    }
    if (subject == null) {
      throw new IllegalArgumentException("Chemistry workflow subject must not be null.");
    }
    if (fermionToQubitMapping == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow fermion-to-qubit mapping must not be null.");
    }
    ChemistryWorkflowJob.validateMode(mode, classicalRequest, classicalProfile, electronicProblem);
    return new ChemistryWorkflowJob(
        id,
        mode,
        subject,
        classicalRequest,
        classicalProfile,
        reactionPredictionRequest,
        electronicProblem,
        fermionToQubitMapping,
        errorMitigationRequest,
        vqeOptimizationRequest,
        quantumProgramPackageOptions);
  }

  public ChemistryWorkflowJobId id() {
    return this.id;
  }

  public ChemistryWorkflowMode mode() {
    return this.mode;
  }

  public ChemistrySubject subject() {
    return this.subject;
  }

  public ClassicalCalculationRequest classicalRequest() {
    return this.classicalRequest;
  }

  public boolean hasClassicalRequest() {
    return this.classicalRequest != null;
  }

  public ClassicalBackendProfile classicalProfile() {
    return this.classicalProfile;
  }

  public ReactionPredictionRequest reactionPredictionRequest() {
    return this.reactionPredictionRequest;
  }

  public boolean hasReactionPredictionRequest() {
    return this.reactionPredictionRequest != null;
  }

  public ElectronicStructureProblem electronicProblem() {
    return this.electronicProblem;
  }

  public FermionToQubitMapping fermionToQubitMapping() {
    return this.fermionToQubitMapping;
  }

  public ErrorMitigationRequest errorMitigationRequest() {
    return this.errorMitigationRequest;
  }

  public boolean hasErrorMitigationRequest() {
    return this.errorMitigationRequest != null;
  }

  public VqeOptimizationRequest vqeOptimizationRequest() {
    return this.vqeOptimizationRequest;
  }

  public boolean hasVqeOptimizationRequest() {
    return this.vqeOptimizationRequest != null;
  }

  public ChemistryQuantumProgramPackageOptions quantumProgramPackageOptions() {
    return this.quantumProgramPackageOptions;
  }

  public boolean hasQuantumProgramPackageOptions() {
    return this.quantumProgramPackageOptions != null;
  }

  public boolean needsClassical() {
    return this.mode == ChemistryWorkflowMode.CLASSICAL_ONLY
        || this.mode == ChemistryWorkflowMode.CLASSICAL_AND_HAMILTONIAN
        || this.mode == ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION;
  }

  public boolean needsHamiltonian() {
    return this.mode == ChemistryWorkflowMode.HAMILTONIAN_ONLY
        || this.mode == ChemistryWorkflowMode.CLASSICAL_AND_HAMILTONIAN
        || this.mode == ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION;
  }

  public boolean needsQuantumProgram() {
    return this.mode == ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION;
  }

  private static void validateMode(
      final ChemistryWorkflowMode mode,
      final ClassicalCalculationRequest classicalRequest,
      final ClassicalBackendProfile classicalProfile,
      final ElectronicStructureProblem electronicProblem) {
    if ((mode == ChemistryWorkflowMode.CLASSICAL_ONLY
            || mode == ChemistryWorkflowMode.CLASSICAL_AND_HAMILTONIAN
            || mode == ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION)
        && (classicalRequest == null || classicalProfile == null)) {
      throw new IllegalArgumentException(
          "Chemistry workflow classical mode requires request and backend profile.");
    }
    if (mode == ChemistryWorkflowMode.HAMILTONIAN_ONLY && electronicProblem == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow Hamiltonian mode requires electronic problem.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowJob)) {
      return false;
    }
    final ChemistryWorkflowJob job = (ChemistryWorkflowJob) other;
    return Objects.equals(this.id, job.id)
        && this.mode == job.mode
        && Objects.equals(this.subject, job.subject)
        && Objects.equals(this.classicalRequest, job.classicalRequest)
        && Objects.equals(this.classicalProfile, job.classicalProfile)
        && Objects.equals(this.reactionPredictionRequest, job.reactionPredictionRequest)
        && Objects.equals(this.electronicProblem, job.electronicProblem)
        && this.fermionToQubitMapping == job.fermionToQubitMapping
        && Objects.equals(this.errorMitigationRequest, job.errorMitigationRequest)
        && Objects.equals(this.vqeOptimizationRequest, job.vqeOptimizationRequest);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.mode,
        this.subject,
        this.classicalRequest,
        this.classicalProfile,
        this.reactionPredictionRequest,
        this.electronicProblem,
        this.fermionToQubitMapping,
        this.errorMitigationRequest,
        this.vqeOptimizationRequest);
  }
}
