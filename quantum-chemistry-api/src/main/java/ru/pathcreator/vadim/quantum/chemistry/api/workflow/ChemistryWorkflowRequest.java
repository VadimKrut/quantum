/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJobId;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowMode;

/**
 * Пользовательский запрос, который собирает полный chemistry workflow из предмета исследования,
 * условий реакции, классического расчета и подготовки Quantum IR.
 */
public final class ChemistryWorkflowRequest {

  private final String id;
  private final String calculationId;
  private final String predictionId;
  private final ChemistrySubject subject;
  private final ChemistryWorkflowMode mode;
  private final ElectronicStructureMethod method;
  private final BasisSet basisSet;
  private final ActiveSpace activeSpace;
  private final List<ClassicalCalculationKind> calculationKinds;
  private final List<ClassicalCalculationOption> calculationOptions;
  private final ClassicalBackendProfile classicalProfile;
  private final ReactionConditions reactionConditions;
  private final List<OrganicReactionDomain> domains;
  private final List<ReactionProductCandidate> seedCandidates;
  private final FermionToQubitMapping mapping;
  private final ErrorMitigationRequest errorMitigationRequest;
  private final VqeOptimizationRequest vqeOptimizationRequest;
  private final String note;

  private ChemistryWorkflowRequest(final Builder builder) {
    this.id = builder.id;
    this.calculationId = builder.calculationId;
    this.predictionId = builder.predictionId;
    this.subject = builder.subject;
    this.mode = builder.mode;
    this.method = builder.method;
    this.basisSet = builder.basisSet;
    this.activeSpace = builder.activeSpace;
    this.calculationKinds = List.copyOf(builder.calculationKinds);
    this.calculationOptions = List.copyOf(builder.calculationOptions);
    this.classicalProfile = builder.classicalProfile;
    this.reactionConditions = builder.reactionConditions;
    this.domains = List.copyOf(builder.domains);
    this.seedCandidates = List.copyOf(builder.seedCandidates);
    this.mapping = builder.mapping;
    this.errorMitigationRequest = builder.errorMitigationRequest;
    this.vqeOptimizationRequest = builder.vqeOptimizationRequest;
    this.note = builder.note;
  }

  public static Builder builder(final String id, final Molecule subject) {
    return new Builder(id, subject);
  }

  public static Builder builder(final String id, final ChemistrySubject subject) {
    return new Builder(id, subject);
  }

  public String id() {
    return this.id;
  }

  public String calculationId() {
    return this.calculationId;
  }

  public String predictionId() {
    return this.predictionId;
  }

  public ChemistrySubject subject() {
    return this.subject;
  }

  public ChemistryWorkflowMode mode() {
    return this.mode;
  }

  public ElectronicStructureMethod method() {
    return this.method;
  }

  public BasisSet basisSet() {
    return this.basisSet;
  }

  public ActiveSpace activeSpace() {
    return this.activeSpace;
  }

  public boolean hasActiveSpace() {
    return this.activeSpace != null;
  }

  public List<ClassicalCalculationKind> calculationKinds() {
    return this.calculationKinds;
  }

  public List<ClassicalCalculationOption> calculationOptions() {
    return this.calculationOptions;
  }

  public ClassicalBackendProfile classicalProfile() {
    return this.classicalProfile;
  }

  public ReactionConditions reactionConditions() {
    return this.reactionConditions;
  }

  public List<OrganicReactionDomain> domains() {
    return this.domains;
  }

  public List<ReactionProductCandidate> seedCandidates() {
    return this.seedCandidates;
  }

  public FermionToQubitMapping mapping() {
    return this.mapping;
  }

  public ErrorMitigationRequest errorMitigationRequest() {
    return this.errorMitigationRequest;
  }

  public VqeOptimizationRequest vqeOptimizationRequest() {
    return this.vqeOptimizationRequest;
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  public ClassicalCalculationRequest toClassicalCalculationRequest() {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(this.calculationId),
        this.subject,
        this.method,
        this.basisSet,
        this.activeSpace,
        this.calculationKinds,
        this.calculationOptions,
        ChemistryMetadata.EMPTY);
  }

  public ReactionPredictionRequest toReactionPredictionRequest() {
    return ReactionPredictionRequest.of(
        this.predictionId,
        this.predictionReactants(),
        this.predictionConditions(),
        this.domains,
        this.seedCandidates,
        this.note);
  }

  public ChemistryWorkflowJob toJob() {
    return ChemistryWorkflowJob.of(
        ChemistryWorkflowJobId.of(this.id),
        this.mode,
        this.subject,
        this.toClassicalCalculationRequest(),
        this.classicalProfile,
        this.toReactionPredictionRequest(),
        null,
        this.mapping,
        this.errorMitigationRequest,
        this.vqeOptimizationRequest);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowRequest)) {
      return false;
    }
    final ChemistryWorkflowRequest request = (ChemistryWorkflowRequest) other;
    return Objects.equals(this.id, request.id)
        && Objects.equals(this.calculationId, request.calculationId)
        && Objects.equals(this.predictionId, request.predictionId)
        && Objects.equals(this.subject, request.subject)
        && this.mode == request.mode
        && Objects.equals(this.method, request.method)
        && Objects.equals(this.basisSet, request.basisSet)
        && Objects.equals(this.activeSpace, request.activeSpace)
        && Objects.equals(this.calculationKinds, request.calculationKinds)
        && Objects.equals(this.calculationOptions, request.calculationOptions)
        && Objects.equals(this.classicalProfile, request.classicalProfile)
        && Objects.equals(this.reactionConditions, request.reactionConditions)
        && Objects.equals(this.domains, request.domains)
        && Objects.equals(this.seedCandidates, request.seedCandidates)
        && this.mapping == request.mapping
        && Objects.equals(this.errorMitigationRequest, request.errorMitigationRequest)
        && Objects.equals(this.vqeOptimizationRequest, request.vqeOptimizationRequest)
        && Objects.equals(this.note, request.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.calculationId,
        this.predictionId,
        this.subject,
        this.mode,
        this.method,
        this.basisSet,
        this.activeSpace,
        this.calculationKinds,
        this.calculationOptions,
        this.classicalProfile,
        this.reactionConditions,
        this.domains,
        this.seedCandidates,
        this.mapping,
        this.errorMitigationRequest,
        this.vqeOptimizationRequest,
        this.note);
  }

  /**
   * Builder с безопасными значениями по умолчанию для полного Java-native chemistry workflow.
   */
  public static final class Builder {

    private final String id;
    private String calculationId;
    private String predictionId;
    private final ChemistrySubject subject;
    private ChemistryWorkflowMode mode;
    private ElectronicStructureMethod method;
    private BasisSet basisSet;
    private ActiveSpace activeSpace;
    private List<ClassicalCalculationKind> calculationKinds;
    private List<ClassicalCalculationOption> calculationOptions;
    private ClassicalBackendProfile classicalProfile;
    private ReactionConditions reactionConditions;
    private List<OrganicReactionDomain> domains;
    private List<ReactionProductCandidate> seedCandidates;
    private FermionToQubitMapping mapping;
    private ErrorMitigationRequest errorMitigationRequest;
    private VqeOptimizationRequest vqeOptimizationRequest;
    private String note;

    private Builder(final String id, final ChemistrySubject subject) {
      this.id = IdentifierValue.requireIdentifier(id, "Chemistry workflow request id");
      if (subject == null) {
        throw new IllegalArgumentException("Chemistry workflow request subject must not be null.");
      }
      this.subject = subject;
      this.calculationId = this.id + "_classical";
      this.predictionId = this.id + "_prediction";
      this.mode = ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION;
      this.method = ElectronicStructureMethod.HARTREE_FOCK;
      this.basisSet = BasisSet.STO_3G;
      this.calculationKinds = List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION);
      this.calculationOptions = List.of();
      this.classicalProfile = BuiltInClassicalBackendProfiles.universalPlanning();
      this.reactionConditions = ReactionConditions.EMPTY;
      this.domains = List.of(OrganicReactionDomain.UNKNOWN);
      this.seedCandidates = List.of();
      this.mapping = FermionToQubitMapping.JORDAN_WIGNER;
      this.note = null;
    }

    public Builder calculationId(final String value) {
      this.calculationId =
          IdentifierValue.requireIdentifier(value, "Chemistry workflow calculation id");
      return this;
    }

    public Builder predictionId(final String value) {
      this.predictionId =
          IdentifierValue.requireIdentifier(value, "Chemistry workflow prediction id");
      return this;
    }

    public Builder mode(final ChemistryWorkflowMode value) {
      if (value == null) {
        throw new IllegalArgumentException("Chemistry workflow request mode must not be null.");
      }
      this.mode = value;
      return this;
    }

    public Builder method(final ElectronicStructureMethod value) {
      if (value == null) {
        throw new IllegalArgumentException("Chemistry workflow request method must not be null.");
      }
      this.method = value;
      return this;
    }

    public Builder basisSet(final BasisSet value) {
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request basis set must not be null.");
      }
      this.basisSet = value;
      return this;
    }

    public Builder activeSpace(final ActiveSpace value) {
      this.activeSpace = value;
      return this;
    }

    public Builder calculationKinds(final List<ClassicalCalculationKind> values) {
      this.calculationKinds = List.copyOf(ChemistryWorkflowRequest.requireCalculationKinds(values));
      return this;
    }

    public Builder calculationOptions(final List<ClassicalCalculationOption> values) {
      this.calculationOptions =
          List.copyOf(ChemistryWorkflowRequest.requireCalculationOptions(values));
      return this;
    }

    public Builder calculationOption(final ClassicalCalculationOption value) {
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request calculation option must not be null.");
      }
      final ArrayList<ClassicalCalculationOption> updated =
          new ArrayList<ClassicalCalculationOption>(this.calculationOptions.size() + 1);
      updated.addAll(this.calculationOptions);
      updated.add(value);
      this.calculationOptions =
          List.copyOf(ChemistryWorkflowRequest.requireCalculationOptions(updated));
      return this;
    }

    public Builder replaceCalculationOption(final ClassicalCalculationOption value) {
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request calculation option must not be null.");
      }
      final ArrayList<ClassicalCalculationOption> updated =
          new ArrayList<ClassicalCalculationOption>(this.calculationOptions.size() + 1);
      for (int i = 0; i < this.calculationOptions.size(); ++i) {
        final ClassicalCalculationOption option = this.calculationOptions.get(i);
        if (!option.name().equals(value.name())) {
          updated.add(option);
        }
      }
      updated.add(value);
      this.calculationOptions =
          List.copyOf(ChemistryWorkflowRequest.requireCalculationOptions(updated));
      return this;
    }

    public Builder boolOption(final String name, final boolean value) {
      return this.calculationOption(ClassicalCalculationOption.bool(name, value));
    }

    public Builder textOption(final String name, final String value) {
      return this.calculationOption(ClassicalCalculationOption.text(name, value));
    }

    public Builder integerOption(final String name, final int value) {
      return this.calculationOption(ClassicalCalculationOption.integer(name, value));
    }

    public Builder floatingOption(final String name, final double value) {
      return this.calculationOption(ClassicalCalculationOption.floating(name, value));
    }

    public Builder prepareCandidateQuantumWorkflows(
        final int maxCandidates, final int maxParticipants) {
      if (maxCandidates <= 0) {
        throw new IllegalArgumentException(
            "Candidate quantum workflow candidate limit must be positive.");
      }
      if (maxParticipants <= 0) {
        throw new IllegalArgumentException(
            "Candidate quantum workflow participant limit must be positive.");
      }
      return this.replaceCalculationOption(
              ClassicalCalculationOption.bool(
                  "reaction_prediction_prepare_quantum_workflows", true))
          .replaceCalculationOption(
              ClassicalCalculationOption.integer(
                  "reaction_prediction_prepare_quantum_workflows_max_candidates", maxCandidates))
          .replaceCalculationOption(
              ClassicalCalculationOption.integer(
                  "reaction_prediction_prepare_quantum_workflows_max_participants",
                  maxParticipants));
    }

    public Builder executeCandidateQuantumWorkflowVqe() {
      return this.replaceCalculationOption(
              ClassicalCalculationOption.bool(
                  "reaction_prediction_execute_quantum_workflow_vqe", true))
          .replaceCalculationOption(
              ClassicalCalculationOption.bool(
                  "advanced_prediction_execute_quantum_workflow_vqe", true));
    }

    public Builder classicalProfile(final ClassicalBackendProfile value) {
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request classical profile must not be null.");
      }
      this.classicalProfile = value;
      return this;
    }

    public Builder reactionConditions(final ReactionConditions value) {
      this.reactionConditions = value == null ? ReactionConditions.EMPTY : value;
      return this;
    }

    public Builder domains(final List<OrganicReactionDomain> values) {
      this.domains = List.copyOf(ChemistryWorkflowRequest.requireDomains(values));
      return this;
    }

    public Builder seedCandidates(final List<ReactionProductCandidate> values) {
      this.seedCandidates = List.copyOf(ChemistryWorkflowRequest.requireSeedCandidates(values));
      return this;
    }

    public Builder mapping(final FermionToQubitMapping value) {
      if (value == null) {
        throw new IllegalArgumentException("Chemistry workflow request mapping must not be null.");
      }
      this.mapping = value;
      return this;
    }

    public Builder errorMitigationRequest(final ErrorMitigationRequest value) {
      this.errorMitigationRequest = value;
      return this;
    }

    public Builder vqeOptimizationRequest(final VqeOptimizationRequest value) {
      this.vqeOptimizationRequest = value;
      return this;
    }

    public Builder note(final String value) {
      this.note = TextValue.optionalText(value);
      return this;
    }

    public ChemistryWorkflowRequest build() {
      return new ChemistryWorkflowRequest(this);
    }
  }

  private ReactionSide predictionReactants() {
    if (this.subject.subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return ReactionSide.of(
          List.of(ReactionParticipant.of((Molecule) this.subject, StoichiometricCoefficient.ONE)));
    }
    if (this.subject.subjectKind() == ChemistrySubjectKind.REACTION) {
      return ((Reaction) this.subject).reactants();
    }
    throw new IllegalArgumentException(
        "Chemistry workflow request subject cannot build reaction prediction.");
  }

  private ReactionConditions predictionConditions() {
    if (!this.reactionConditions.empty()) {
      return this.reactionConditions;
    }
    if (this.subject.subjectKind() == ChemistrySubjectKind.REACTION) {
      return ((Reaction) this.subject).conditions();
    }
    return this.reactionConditions;
  }

  private static List<ClassicalCalculationKind> requireCalculationKinds(
      final List<ClassicalCalculationKind> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException(
          "Chemistry workflow request calculation kinds must not be empty.");
    }
    final ArrayList<ClassicalCalculationKind> checked =
        new ArrayList<ClassicalCalculationKind>(values.size());
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalCalculationKind value = values.get(i);
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request calculation kind must not be null.");
      }
      if (!checked.contains(value)) {
        checked.add(value);
      }
    }
    return checked;
  }

  private static List<OrganicReactionDomain> requireDomains(
      final List<OrganicReactionDomain> values) {
    if (values == null || values.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    final ArrayList<OrganicReactionDomain> checked =
        new ArrayList<OrganicReactionDomain>(values.size());
    for (int i = 0; i < values.size(); ++i) {
      final OrganicReactionDomain value = values.get(i);
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request reaction domain must not be null.");
      }
      if (!checked.contains(value)) {
        checked.add(value);
      }
    }
    return checked;
  }

  private static List<ClassicalCalculationOption> requireCalculationOptions(
      final List<ClassicalCalculationOption> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final ArrayList<ClassicalCalculationOption> checked =
        new ArrayList<ClassicalCalculationOption>(values.size());
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalCalculationOption value = values.get(i);
      if (value == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request calculation option must not be null.");
      }
      if (ChemistryWorkflowRequest.hasOption(checked, value.name())) {
        throw new IllegalArgumentException(
            "Chemistry workflow request calculation option must be unique: " + value.name() + ".");
      }
      checked.add(value);
    }
    return checked;
  }

  private static boolean hasOption(
      final List<ClassicalCalculationOption> options, final String name) {
    for (int i = 0; i < options.size(); ++i) {
      if (options.get(i).name().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static List<ReactionProductCandidate> requireSeedCandidates(
      final List<ReactionProductCandidate> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < values.size(); ++i) {
      if (values.get(i) == null) {
        throw new IllegalArgumentException(
            "Chemistry workflow request seed candidate must not be null.");
      }
    }
    return values;
  }
}
