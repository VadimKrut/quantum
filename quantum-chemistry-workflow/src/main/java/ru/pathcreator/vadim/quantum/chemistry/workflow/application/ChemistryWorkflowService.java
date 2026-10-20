/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.application;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceRequestResolver;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceSelector;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicProblemId;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicStructureProblem;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ChemistryQuantumProgramPackageGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ElectronicHamiltonianProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation.ErrorMitigationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.StateVectorPauliVqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeOptimizer;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumPreparationProvenance;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.followup.ChemistryFollowUpCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowExecutionBundle;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStage;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.storage.ChemistryWorkflowStoragePlan;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Оркестрирует Java workflow от chemistry model до classical results, Hamiltonian,
 * Quantum IR и плана error mitigation.
 */
public final class ChemistryWorkflowService {

  private final ChemistryCoreValidator coreValidator;
  private final ClassicalChemistryService classicalChemistryService;
  private final ElectronicHamiltonianQubitMapper qubitMapper;
  private final ChemistryQuantumProgramPackageGenerator programPackageGenerator;
  private final ErrorMitigationPlanner errorMitigationPlanner;
  private final ReactionPredictionExplainer reactionPredictionExplainer;
  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ReactionPredictionFollowUpPlanner followUpPlanner;
  private final ReactionConditionScenarioPlanner conditionScenarioPlanner;
  private final ChemistryComputationParityPlanner parityPlanner;
  private final ChemistryCapabilityAnalyzer capabilityAnalyzer;
  private final ChemistryWorkflowStoragePlanner storagePlanner;
  private final ChemistryFollowUpCalculationPlanner followUpCalculationPlanner;
  private final VqeEnergyBackend vqeEnergyBackend;

  public ChemistryWorkflowService() {
    this(new ElectronicStructurePreparationPipeline());
  }

  private ChemistryWorkflowService(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(
        new ChemistryCoreValidator(),
        new ClassicalChemistryService(preparationPipeline),
        new ElectronicHamiltonianQubitMapper(),
        new ElectronicHamiltonianProgramGenerator(),
        new ChemistryQuantumProgramPackageGenerator(),
        new ErrorMitigationPlanner(),
        new ReactionPredictionExplainer(),
        preparationPipeline,
        new ReactionPredictionFollowUpPlanner(),
        new ReactionConditionScenarioPlanner(),
        new ChemistryComputationParityPlanner(),
        new ChemistryCapabilityAnalyzer(preparationPipeline),
        new ChemistryWorkflowStoragePlanner(),
        new ChemistryFollowUpCalculationPlanner(),
        null);
  }

  public ChemistryWorkflowService(
      final ChemistryCoreValidator coreValidator,
      final ClassicalChemistryService classicalChemistryService,
      final ElectronicHamiltonianQubitMapper qubitMapper,
      final ElectronicHamiltonianProgramGenerator programGenerator,
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ReactionPredictionFollowUpPlanner followUpPlanner) {
    this(
        coreValidator,
        classicalChemistryService,
        qubitMapper,
        programGenerator,
        new ChemistryQuantumProgramPackageGenerator(),
        new ErrorMitigationPlanner(),
        new ReactionPredictionExplainer(),
        preparationPipeline,
        followUpPlanner,
        new ReactionConditionScenarioPlanner(),
        new ChemistryComputationParityPlanner(),
        new ChemistryCapabilityAnalyzer(preparationPipeline),
        new ChemistryWorkflowStoragePlanner(),
        new ChemistryFollowUpCalculationPlanner(),
        null);
  }

  public ChemistryWorkflowService(
      final ChemistryCoreValidator coreValidator,
      final ClassicalChemistryService classicalChemistryService,
      final ElectronicHamiltonianQubitMapper qubitMapper,
      final ElectronicHamiltonianProgramGenerator programGenerator,
      final ChemistryQuantumProgramPackageGenerator programPackageGenerator,
      final ErrorMitigationPlanner errorMitigationPlanner,
      final ReactionPredictionExplainer reactionPredictionExplainer,
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ReactionPredictionFollowUpPlanner followUpPlanner,
      final ReactionConditionScenarioPlanner conditionScenarioPlanner) {
    this(
        coreValidator,
        classicalChemistryService,
        qubitMapper,
        programGenerator,
        programPackageGenerator,
        errorMitigationPlanner,
        reactionPredictionExplainer,
        preparationPipeline,
        followUpPlanner,
        conditionScenarioPlanner,
        new ChemistryComputationParityPlanner(),
        new ChemistryCapabilityAnalyzer(preparationPipeline),
        new ChemistryWorkflowStoragePlanner(),
        new ChemistryFollowUpCalculationPlanner(),
        null);
  }

  public ChemistryWorkflowService(
      final ChemistryCoreValidator coreValidator,
      final ClassicalChemistryService classicalChemistryService,
      final ElectronicHamiltonianQubitMapper qubitMapper,
      final ElectronicHamiltonianProgramGenerator programGenerator,
      final ChemistryQuantumProgramPackageGenerator programPackageGenerator,
      final ErrorMitigationPlanner errorMitigationPlanner,
      final ReactionPredictionExplainer reactionPredictionExplainer,
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ReactionPredictionFollowUpPlanner followUpPlanner,
      final ReactionConditionScenarioPlanner conditionScenarioPlanner,
      final ChemistryComputationParityPlanner parityPlanner,
      final ChemistryCapabilityAnalyzer capabilityAnalyzer,
      final ChemistryWorkflowStoragePlanner storagePlanner) {
    this(
        coreValidator,
        classicalChemistryService,
        qubitMapper,
        programGenerator,
        programPackageGenerator,
        errorMitigationPlanner,
        reactionPredictionExplainer,
        preparationPipeline,
        followUpPlanner,
        conditionScenarioPlanner,
        parityPlanner,
        capabilityAnalyzer,
        storagePlanner,
        new ChemistryFollowUpCalculationPlanner(),
        null);
  }

  public ChemistryWorkflowService(
      final ChemistryCoreValidator coreValidator,
      final ClassicalChemistryService classicalChemistryService,
      final ElectronicHamiltonianQubitMapper qubitMapper,
      final ElectronicHamiltonianProgramGenerator programGenerator,
      final ChemistryQuantumProgramPackageGenerator programPackageGenerator,
      final ErrorMitigationPlanner errorMitigationPlanner,
      final ReactionPredictionExplainer reactionPredictionExplainer,
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ReactionPredictionFollowUpPlanner followUpPlanner,
      final ReactionConditionScenarioPlanner conditionScenarioPlanner,
      final ChemistryComputationParityPlanner parityPlanner,
      final ChemistryCapabilityAnalyzer capabilityAnalyzer,
      final ChemistryWorkflowStoragePlanner storagePlanner,
      final VqeEnergyBackend vqeEnergyBackend) {
    this(
        coreValidator,
        classicalChemistryService,
        qubitMapper,
        programGenerator,
        programPackageGenerator,
        errorMitigationPlanner,
        reactionPredictionExplainer,
        preparationPipeline,
        followUpPlanner,
        conditionScenarioPlanner,
        parityPlanner,
        capabilityAnalyzer,
        storagePlanner,
        new ChemistryFollowUpCalculationPlanner(),
        vqeEnergyBackend);
  }

  public ChemistryWorkflowService(
      final ChemistryCoreValidator coreValidator,
      final ClassicalChemistryService classicalChemistryService,
      final ElectronicHamiltonianQubitMapper qubitMapper,
      final ElectronicHamiltonianProgramGenerator programGenerator,
      final ChemistryQuantumProgramPackageGenerator programPackageGenerator,
      final ErrorMitigationPlanner errorMitigationPlanner,
      final ReactionPredictionExplainer reactionPredictionExplainer,
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ReactionPredictionFollowUpPlanner followUpPlanner,
      final ReactionConditionScenarioPlanner conditionScenarioPlanner,
      final ChemistryComputationParityPlanner parityPlanner,
      final ChemistryCapabilityAnalyzer capabilityAnalyzer,
      final ChemistryWorkflowStoragePlanner storagePlanner,
      final ChemistryFollowUpCalculationPlanner followUpCalculationPlanner,
      final VqeEnergyBackend vqeEnergyBackend) {
    if (coreValidator == null) {
      throw new IllegalArgumentException("Chemistry workflow core validator must not be null.");
    }
    if (classicalChemistryService == null) {
      throw new IllegalArgumentException("Chemistry workflow classical service must not be null.");
    }
    if (qubitMapper == null) {
      throw new IllegalArgumentException("Chemistry workflow qubit mapper must not be null.");
    }
    if (programGenerator == null) {
      throw new IllegalArgumentException("Chemistry workflow program generator must not be null.");
    }
    if (programPackageGenerator == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow program package generator must not be null.");
    }
    if (errorMitigationPlanner == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow error mitigation planner must not be null.");
    }
    if (reactionPredictionExplainer == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow reaction prediction explainer must not be null.");
    }
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow preparation pipeline must not be null.");
    }
    if (followUpPlanner == null) {
      throw new IllegalArgumentException("Chemistry workflow follow-up planner must not be null.");
    }
    if (conditionScenarioPlanner == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow condition scenario planner must not be null.");
    }
    if (parityPlanner == null) {
      throw new IllegalArgumentException("Chemistry workflow parity planner must not be null.");
    }
    if (capabilityAnalyzer == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow capability analyzer must not be null.");
    }
    if (storagePlanner == null) {
      throw new IllegalArgumentException("Chemistry workflow storage planner must not be null.");
    }
    if (followUpCalculationPlanner == null) {
      throw new IllegalArgumentException(
          "Chemistry follow-up calculation planner must not be null.");
    }
    this.coreValidator = coreValidator;
    this.classicalChemistryService = classicalChemistryService;
    this.qubitMapper = qubitMapper;
    this.programPackageGenerator = programPackageGenerator;
    this.errorMitigationPlanner = errorMitigationPlanner;
    this.reactionPredictionExplainer = reactionPredictionExplainer;
    this.preparationPipeline = preparationPipeline;
    this.followUpPlanner = followUpPlanner;
    this.conditionScenarioPlanner = conditionScenarioPlanner;
    this.parityPlanner = parityPlanner;
    this.capabilityAnalyzer = capabilityAnalyzer;
    this.storagePlanner = storagePlanner;
    this.followUpCalculationPlanner = followUpCalculationPlanner;
    this.vqeEnergyBackend = vqeEnergyBackend;
  }

  public ChemistryWorkflowResult preflight(final ChemistryWorkflowJob job) {
    return this.run(job, false);
  }

  public ChemistryWorkflowResult run(final ChemistryWorkflowJob job) {
    return this.run(job, true);
  }

  public ChemistryWorkflowExecutionBundle runWithAllFollowUpCalculations(
      final ChemistryWorkflowJob job) {
    final ChemistryWorkflowResult result = this.run(job);
    final List<ClassicalCalculationResult> followUpResults =
        this.executeAllFollowUpCalculations(result);
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(result, followUpResults);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, followUpResults, computationParityPlan);
    return ChemistryWorkflowExecutionBundle.of(
        result.withComputationParityAndStoragePlan(
            computationParityPlan, this.storagePlanner.plan(bundle)),
        followUpResults,
        computationParityPlan);
  }

  public ChemistryWorkflowExecutionBundle runWithAllFollowUpCalculations(
      final ChemistryWorkflowJob job, final int maxParallelFollowUpCount) {
    final ChemistryWorkflowResult result = this.run(job);
    final List<ClassicalCalculationResult> followUpResults =
        this.executeAllFollowUpCalculations(result, maxParallelFollowUpCount);
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(result, followUpResults);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, followUpResults, computationParityPlan);
    return ChemistryWorkflowExecutionBundle.of(
        result.withComputationParityAndStoragePlan(
            computationParityPlan, this.storagePlanner.plan(bundle)),
        followUpResults,
        computationParityPlan);
  }

  public ChemistryWorkflowExecutionBundle runWithFollowUpCalculationLimit(
      final ChemistryWorkflowJob job, final int maxFollowUpRequestCount) {
    final ChemistryWorkflowResult result = this.run(job, true, maxFollowUpRequestCount);
    final List<ClassicalCalculationResult> followUpResults =
        this.executeFollowUpCalculations(result.followUpCalculationPlan());
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(result, followUpResults);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, followUpResults, computationParityPlan);
    return ChemistryWorkflowExecutionBundle.of(
        result.withComputationParityAndStoragePlan(
            computationParityPlan, this.storagePlanner.plan(bundle)),
        followUpResults,
        computationParityPlan);
  }

  public ChemistryWorkflowExecutionBundle runWithFollowUpCalculationLimit(
      final ChemistryWorkflowJob job,
      final int maxFollowUpRequestCount,
      final int maxParallelFollowUpCount) {
    final ChemistryWorkflowResult result = this.run(job, true, maxFollowUpRequestCount);
    final List<ClassicalCalculationResult> followUpResults =
        this.executeFollowUpCalculations(
            result.followUpCalculationPlan(), maxParallelFollowUpCount);
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(result, followUpResults);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, followUpResults, computationParityPlan);
    return ChemistryWorkflowExecutionBundle.of(
        result.withComputationParityAndStoragePlan(
            computationParityPlan, this.storagePlanner.plan(bundle)),
        followUpResults,
        computationParityPlan);
  }

  public ChemistryWorkflowExecutionBundle runWithoutQuantumProgramAndExecuteAllFollowUps(
      final ChemistryWorkflowJob job) {
    final ChemistryWorkflowResult result = this.run(job, false);
    final List<ClassicalCalculationResult> followUpResults =
        this.executeAllFollowUpCalculations(result);
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(result, followUpResults);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, followUpResults, computationParityPlan);
    return ChemistryWorkflowExecutionBundle.of(
        result.withComputationParityAndStoragePlan(
            computationParityPlan, this.storagePlanner.plan(bundle)),
        followUpResults,
        computationParityPlan);
  }

  public ChemistryWorkflowExecutionBundle runWithoutQuantumProgramAndExecuteAllFollowUps(
      final ChemistryWorkflowJob job, final int maxParallelFollowUpCount) {
    final ChemistryWorkflowResult result = this.run(job, false);
    final List<ClassicalCalculationResult> followUpResults =
        this.executeAllFollowUpCalculations(result, maxParallelFollowUpCount);
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(result, followUpResults);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, followUpResults, computationParityPlan);
    return ChemistryWorkflowExecutionBundle.of(
        result.withComputationParityAndStoragePlan(
            computationParityPlan, this.storagePlanner.plan(bundle)),
        followUpResults,
        computationParityPlan);
  }

  public ChemistryFollowUpCalculationPlan planFollowUpCalculations(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return this.followUpCalculationPlanner.planHighPriority(job, recommendationPlan);
  }

  public ChemistryFollowUpCalculationPlan planAllFollowUpCalculations(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return this.followUpCalculationPlanner.planAll(job, recommendationPlan);
  }

  public ChemistryFollowUpCalculationPlan planFollowUpCalculations(
      final ChemistryWorkflowResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    if (result.hasFollowUpCalculationPlan()) {
      return result.followUpCalculationPlan();
    }
    return this.followUpCalculationPlanner.planHighPriority(
        result.job(), result.recommendationPlan(), result.reactionPredictionResult());
  }

  public ChemistryFollowUpCalculationPlan planAllFollowUpCalculations(
      final ChemistryWorkflowResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.followUpCalculationPlanner.planAll(
        result.job(), result.recommendationPlan(), result.reactionPredictionResult());
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryFollowUpCalculationPlan plan) {
    if (plan == null) {
      throw new IllegalArgumentException("Chemistry follow-up calculation plan must not be null.");
    }
    if (plan.empty()) {
      return List.of();
    }
    final List<ClassicalCalculationRequest> requests = plan.requests();
    if (requests.size() == 1) {
      return List.of(this.classicalChemistryService.execute(requests.get(0)));
    }
    return this.executeFollowUpCalculationsInParallel(requests);
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryFollowUpCalculationPlan plan, final int maxParallelFollowUpCount) {
    if (plan == null) {
      throw new IllegalArgumentException("Chemistry follow-up calculation plan must not be null.");
    }
    if (maxParallelFollowUpCount < 1) {
      throw new IllegalArgumentException("Chemistry follow-up parallelism must be positive.");
    }
    if (plan.empty()) {
      return List.of();
    }
    final List<ClassicalCalculationRequest> requests = plan.requests();
    if (requests.size() == 1 || maxParallelFollowUpCount == 1) {
      return this.executeFollowUpCalculationsSequentially(plan);
    }
    if (requests.size() <= maxParallelFollowUpCount) {
      return this.executeFollowUpCalculationsInParallel(requests);
    }
    return this.executeFollowUpCalculationsInBatches(requests, maxParallelFollowUpCount);
  }

  private List<ClassicalCalculationResult> executeFollowUpCalculationsInBatches(
      final List<ClassicalCalculationRequest> requests, final int maxParallelFollowUpCount) {
    final ArrayList<ClassicalCalculationResult> results =
        new ArrayList<ClassicalCalculationResult>(requests.size());
    int start = 0;
    while (start < requests.size()) {
      final int end = Math.min(start + maxParallelFollowUpCount, requests.size());
      final ArrayList<ClassicalCalculationRequest> batch =
          new ArrayList<ClassicalCalculationRequest>(end - start);
      for (int i = start; i < end; ++i) {
        batch.add(requests.get(i));
      }
      results.addAll(this.executeFollowUpCalculationsInParallel(batch));
      start = end;
    }
    return List.copyOf(results);
  }

  private List<ClassicalCalculationResult> executeFollowUpCalculationsInParallel(
      final List<ClassicalCalculationRequest> requests) {
    final ArrayList<Future<ClassicalCalculationResult>> futures =
        new ArrayList<Future<ClassicalCalculationResult>>(requests.size());
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < requests.size(); ++i) {
        final ClassicalCalculationRequest request = requests.get(i);
        futures.add(executor.submit(() -> this.classicalChemistryService.execute(request)));
      }
      final ArrayList<ClassicalCalculationResult> results =
          new ArrayList<ClassicalCalculationResult>(requests.size());
      for (int i = 0; i < futures.size(); ++i) {
        results.add(ChemistryWorkflowService.completedFollowUpResult(futures.get(i)));
      }
      return List.copyOf(results);
    }
  }

  private static ClassicalCalculationResult completedFollowUpResult(
      final Future<ClassicalCalculationResult> future) {
    try {
      return future.get();
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Chemistry follow-up calculation was interrupted.", exception);
    } catch (final ExecutionException exception) {
      throw new IllegalStateException(
          "Chemistry follow-up calculation failed.", exception.getCause());
    }
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculationsSequentially(
      final ChemistryFollowUpCalculationPlan plan) {
    if (plan == null) {
      throw new IllegalArgumentException("Chemistry follow-up calculation plan must not be null.");
    }
    if (plan.empty()) {
      return List.of();
    }
    final List<ClassicalCalculationRequest> requests = plan.requests();
    final ArrayList<ClassicalCalculationResult> results =
        new ArrayList<ClassicalCalculationResult>(requests.size());
    for (int i = 0; i < requests.size(); ++i) {
      results.add(this.classicalChemistryService.execute(requests.get(i)));
    }
    return List.copyOf(results);
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryWorkflowResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.executeFollowUpCalculations(result.followUpCalculationPlan());
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryWorkflowResult result, final List<ClassicalCalculationKind> calculationKinds) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.executeFollowUpCalculations(
        result.followUpCalculationPlan().filteredByKinds(calculationKinds));
  }

  public List<ClassicalCalculationResult> executeAllFollowUpCalculations(
      final ChemistryWorkflowResult result, final List<ClassicalCalculationKind> calculationKinds) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.executeFollowUpCalculations(
        this.planAllFollowUpCalculations(result).filteredByKinds(calculationKinds));
  }

  public List<ClassicalCalculationResult> executeAllFollowUpCalculations(
      final ChemistryWorkflowResult result,
      final List<ClassicalCalculationKind> calculationKinds,
      final int maxParallelFollowUpCount) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.executeFollowUpCalculations(
        this.planAllFollowUpCalculations(result).filteredByKinds(calculationKinds),
        maxParallelFollowUpCount);
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return this.executeFollowUpCalculations(this.planFollowUpCalculations(job, recommendationPlan));
  }

  public List<ClassicalCalculationResult> executeAllFollowUpCalculations(
      final ChemistryWorkflowResult result) {
    return this.executeFollowUpCalculations(this.planAllFollowUpCalculations(result));
  }

  public List<ClassicalCalculationResult> executeAllFollowUpCalculations(
      final ChemistryWorkflowResult result, final int maxParallelFollowUpCount) {
    return this.executeFollowUpCalculations(
        this.planAllFollowUpCalculations(result), maxParallelFollowUpCount);
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryWorkflowResult result, final int maxFollowUpRequestCount) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.executeFollowUpCalculations(
        this.planAllFollowUpCalculations(result).limitedTo(maxFollowUpRequestCount));
  }

  public List<ClassicalCalculationResult> executeFollowUpCalculations(
      final ChemistryWorkflowResult result,
      final int maxFollowUpRequestCount,
      final int maxParallelFollowUpCount) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.executeFollowUpCalculations(
        this.planAllFollowUpCalculations(result).limitedTo(maxFollowUpRequestCount),
        maxParallelFollowUpCount);
  }

  public ChemistryComputationParityPlan planComputationParity(
      final ChemistryWorkflowResult result,
      final List<ClassicalCalculationResult> executedFollowUpResults) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.parityPlanner.planWithClassicalResults(
        result.job().id().value(),
        result.reactionPredictionResult(),
        result.reactionPredictionExplanation(),
        result.recommendationPlan(),
        executedFollowUpResults,
        result.quantumProgramPackage());
  }

  public ChemistryComputationParityPlan executeAllFollowUpCalculationsAndPlanParity(
      final ChemistryWorkflowResult result) {
    final List<ClassicalCalculationResult> executedFollowUps =
        this.executeAllFollowUpCalculations(result);
    return this.planComputationParity(result, executedFollowUps);
  }

  public ChemistryComputationParityPlan executeAllFollowUpCalculationsAndPlanParity(
      final ChemistryWorkflowResult result, final int maxParallelFollowUpCount) {
    final List<ClassicalCalculationResult> executedFollowUps =
        this.executeAllFollowUpCalculations(result, maxParallelFollowUpCount);
    return this.planComputationParity(result, executedFollowUps);
  }

  public ChemistryComputationParityPlan executeFollowUpCalculationsAndPlanParity(
      final ChemistryWorkflowResult result, final List<ClassicalCalculationKind> calculationKinds) {
    final List<ClassicalCalculationResult> executedFollowUps =
        this.executeFollowUpCalculations(result, calculationKinds);
    return this.planComputationParity(result, executedFollowUps);
  }

  private ChemistryWorkflowResult run(
      final ChemistryWorkflowJob job, final boolean generateQuantumProgram) {
    return this.run(job, generateQuantumProgram, Integer.MAX_VALUE);
  }

  private ChemistryWorkflowResult run(
      final ChemistryWorkflowJob job,
      final boolean generateQuantumProgram,
      final int maxFollowUpPlanRequestCount) {
    if (job == null) {
      throw new IllegalArgumentException("Chemistry workflow job must not be null.");
    }
    if (maxFollowUpPlanRequestCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow follow-up plan limit must be non-negative.");
    }
    final ArrayList<ChemistryWorkflowStageResult> stages =
        new ArrayList<ChemistryWorkflowStageResult>();
    final ArrayList<ChemistryWorkflowDiagnostic> diagnostics =
        new ArrayList<ChemistryWorkflowDiagnostic>();

    final ChemistryValidationResult validationResult = this.validateCore(job, stages, diagnostics);
    if (!validationResult.valid()) {
      return this.result(
          job,
          validationResult,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          ChemistryComputationParityPlan.EMPTY,
          ChemistryWorkflowRecommendationPlan.EMPTY,
          ChemistryFollowUpCalculationPlan.EMPTY,
          null,
          null,
          null,
          null,
          null,
          stages,
          diagnostics);
    }

    final ChemistryCapabilityReport capabilityReport = this.capabilityReport(job);
    final ClassicalPreflightResult classicalPreflightResult =
        this.classicalPreflight(job, stages, diagnostics);
    final ClassicalCalculationPlan classicalCalculationPlan =
        this.classicalPlan(job, classicalPreflightResult, stages);
    final ClassicalCalculationResult classicalCalculationResult =
        this.executeClassical(job, capabilityReport, classicalPreflightResult, stages, diagnostics);
    final ReactionPredictionResult reactionPredictionResult =
        this.predictReactionProducts(job, stages, diagnostics);
    final ReactionPredictionExplanation reactionPredictionExplanation =
        this.explainReactionPrediction(reactionPredictionResult);
    final ReactionConditionEffectProfile conditionEffectProfile =
        this.analyzeConditionEffects(job, reactionPredictionResult, stages);
    final ReactionConditionScenarioPlan conditionScenarioPlan =
        this.planConditionScenarios(job, reactionPredictionResult, stages);
    final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan =
        this.evaluateConditionScenarios(
            job, conditionScenarioPlan, reactionPredictionResult, stages);
    final ChemistryWorkflowRecommendationPlan recommendationPlan =
        this.planReactionFollowUp(job, reactionPredictionResult, stages);
    final ChemistryFollowUpCalculationPlan followUpCalculationPlan =
        this.followUpCalculationPlanner.planAll(
            job, recommendationPlan, reactionPredictionResult, maxFollowUpPlanRequestCount);
    final ElectronicStructureProblem electronicProblem =
        this.prepareHamiltonian(job, capabilityReport, stages, diagnostics);
    final QubitPauliOperator qubitOperator =
        this.mapHamiltonian(job, electronicProblem, stages, diagnostics);
    final ChemistryQuantumProgramPackage quantumProgramPackage =
        this.prepareQuantumProgramPackage(
            job, electronicProblem, qubitOperator, generateQuantumProgram, stages, diagnostics);
    final QuantumProgram quantumProgram =
        quantumProgramPackage != null && quantumProgramPackage.hasArtifacts()
            ? quantumProgramPackage.primaryProgram()
            : null;
    final ErrorMitigationPlan errorMitigationPlan =
        this.planErrorMitigation(job, quantumProgram, stages, diagnostics);
    final VqeOptimizationResult vqeOptimizationResult =
        this.optimizeVqe(job, quantumProgramPackage, qubitOperator, stages, diagnostics);
    final ChemistryComputationParityPlan computationParityPlan =
        this.planComputationParity(
            job,
            reactionPredictionResult,
            reactionPredictionExplanation,
            recommendationPlan,
            classicalCalculationResult,
            quantumProgramPackage,
            stages);

    return this.result(
        job,
        validationResult,
        capabilityReport,
        classicalPreflightResult,
        classicalCalculationPlan,
        classicalCalculationResult,
        reactionPredictionResult,
        reactionPredictionExplanation,
        conditionEffectProfile,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        computationParityPlan,
        recommendationPlan,
        followUpCalculationPlan,
        electronicProblem,
        quantumProgram,
        quantumProgramPackage,
        errorMitigationPlan,
        vqeOptimizationResult,
        stages,
        diagnostics);
  }

  private ChemistryCapabilityReport capabilityReport(final ChemistryWorkflowJob job) {
    final ClassicalCalculationRequest request = job.classicalRequest();
    if (request == null) {
      return null;
    }
    return this.capabilityAnalyzer.analyze(request);
  }

  private ChemistryValidationResult validateCore(
      final ChemistryWorkflowJob job,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    final ChemistryValidationResult result;
    if (job.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      result = this.coreValidator.validateMolecule((Molecule) job.subject());
    } else if (job.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      result = this.coreValidator.validateReaction((Reaction) job.subject());
    } else {
      result = ChemistryValidationResult.of(List.of());
    }
    if (job.hasReactionPredictionRequest()) {
      final ArrayList<ChemistryDiagnostic> combinedDiagnostics =
          new ArrayList<ChemistryDiagnostic>(result.diagnostics());
      combinedDiagnostics.addAll(
          this.coreValidator
              .validateReactionPredictionRequest(job.reactionPredictionRequest())
              .diagnostics());
      this.addCoreDiagnostics(ChemistryValidationResult.of(combinedDiagnostics), diagnostics);
      final ChemistryValidationResult combinedResult =
          ChemistryValidationResult.of(combinedDiagnostics);
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CORE_VALIDATION,
              combinedResult.valid()
                  ? ChemistryWorkflowStageStatus.COMPLETED
                  : ChemistryWorkflowStageStatus.FAILED,
              combinedResult.valid()
                  ? "Core chemistry validation completed."
                  : "Core validation failed."));
      return combinedResult;
    }
    this.addCoreDiagnostics(result, diagnostics);
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.CORE_VALIDATION,
            result.valid()
                ? ChemistryWorkflowStageStatus.COMPLETED
                : ChemistryWorkflowStageStatus.FAILED,
            result.valid() ? "Core chemistry validation completed." : "Core validation failed."));
    return result;
  }

  private ClassicalPreflightResult classicalPreflight(
      final ChemistryWorkflowJob job,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.needsClassical()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_PREFLIGHT,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Classical stage is not requested."));
      return null;
    }
    final ClassicalPreflightResult result =
        this.classicalChemistryService.preflight(job.classicalRequest(), job.classicalProfile());
    if (result.diagnostics().hasErrors()) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.CLASSICAL_PREFLIGHT_FAILED,
              ChemistryWorkflowStage.CLASSICAL_PREFLIGHT,
              result.status().name(),
              job.classicalRequest().id().value(),
              "Classical preflight returned errors."));
    }
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.CLASSICAL_PREFLIGHT,
            result.diagnostics().hasErrors()
                ? ChemistryWorkflowStageStatus.FAILED
                : ChemistryWorkflowStageStatus.COMPLETED,
            result.status().name()));
    return result;
  }

  private ClassicalCalculationPlan classicalPlan(
      final ChemistryWorkflowJob job,
      final ClassicalPreflightResult classicalPreflightResult,
      final List<ChemistryWorkflowStageResult> stages) {
    if (!job.needsClassical() || classicalPreflightResult == null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Classical planning is not requested."));
      return null;
    }
    if (classicalPreflightResult.diagnostics().hasErrors()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Classical planning skipped because preflight failed."));
      return null;
    }
    final ClassicalCalculationPlan plan =
        this.classicalChemistryService.plan(job.classicalRequest(), job.classicalProfile());
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.CLASSICAL_PLANNING,
            ChemistryWorkflowStageStatus.COMPLETED,
            "Classical calculation plan prepared."));
    return plan;
  }

  private ClassicalCalculationResult executeClassical(
      final ChemistryWorkflowJob job,
      final ChemistryCapabilityReport capabilityReport,
      final ClassicalPreflightResult classicalPreflightResult,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.needsClassical() || classicalPreflightResult == null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_EXECUTION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Classical execution is not requested."));
      return null;
    }
    if (classicalPreflightResult.diagnostics().hasErrors()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_EXECUTION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Classical execution skipped because preflight failed."));
      return null;
    }
    if (ChemistryWorkflowService.hasUnsupportedNativeHamiltonianCapability(job, capabilityReport)) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_EXECUTION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Classical execution skipped because native Hamiltonian capability preflight failed."));
      return null;
    }
    try {
      final ClassicalCalculationResult result =
          this.classicalChemistryService.execute(job.classicalRequest());
      final ChemistryWorkflowStageStatus stageStatus =
          ChemistryWorkflowService.classicalExecutionStageStatus(result.status());
      if (stageStatus == ChemistryWorkflowStageStatus.FAILED) {
        diagnostics.add(
            ChemistryWorkflowDiagnostic.error(
                ChemistryWorkflowDiagnosticCode.CLASSICAL_EXECUTION_FAILED,
                ChemistryWorkflowStage.CLASSICAL_EXECUTION,
                result.status().name(),
                result.calculationId().value(),
                "Classical calculation execution failed."));
      }
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_EXECUTION,
              stageStatus,
              "Classical execution returned " + result.status().name() + "."));
      return result;
    } catch (final RuntimeException exception) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.CLASSICAL_EXECUTION_FAILED,
              ChemistryWorkflowStage.CLASSICAL_EXECUTION,
              "exception",
              job.id().value(),
              exception.getMessage()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CLASSICAL_EXECUTION,
              ChemistryWorkflowStageStatus.FAILED,
              "Classical execution failed."));
      return null;
    }
  }

  private static ChemistryWorkflowStageStatus classicalExecutionStageStatus(
      final ClassicalCalculationStatus status) {
    if (status == ClassicalCalculationStatus.SUCCEEDED) {
      return ChemistryWorkflowStageStatus.COMPLETED;
    }
    if (status == ClassicalCalculationStatus.PARTIAL) {
      return ChemistryWorkflowStageStatus.WARNING;
    }
    if (status == ClassicalCalculationStatus.PLANNED
        || status == ClassicalCalculationStatus.RUNNING) {
      return ChemistryWorkflowStageStatus.READY;
    }
    return ChemistryWorkflowStageStatus.FAILED;
  }

  private static boolean hasUnsupportedNativeHamiltonianCapability(
      final ChemistryWorkflowJob job, final ChemistryCapabilityReport capabilityReport) {
    if (!job.needsHamiltonian() || capabilityReport == null) {
      return false;
    }
    if (job.needsQuantumProgram()
        && job.classicalRequest() != null
        && !job.classicalRequest().hasActiveSpace()) {
      return false;
    }
    return capabilityReport.requireItem("integrals.full-eri").status()
        == ChemistryCapabilityStatus.UNSUPPORTED;
  }

  private static String hamiltonianPreparationMessage(final ElectronicHamiltonian hamiltonian) {
    final String completeness =
        hamiltonian.twoElectronIntegrals().isEmpty() ? "one-electron" : "full two-electron";
    return "Hamiltonian prepared by Java-native electronic-structure pipeline: "
        + completeness
        + ", electrons="
        + hamiltonian.electronCount()
        + ", qubits="
        + hamiltonian.spinOrbitalCount()
        + ", integrals="
        + hamiltonian.integralCount()
        + ".";
  }

  private ReactionPredictionResult predictReactionProducts(
      final ChemistryWorkflowJob job,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.hasReactionPredictionRequest()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.REACTION_PREDICTION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Reaction prediction is not requested."));
      return null;
    }
    try {
      final ArrayList<ClassicalDiagnostic> predictionDiagnostics =
          new ArrayList<ClassicalDiagnostic>();
      final ReactionPredictionResult result =
          job.hasClassicalRequest()
              ? this.classicalChemistryService.predictReactionProducts(
                  job.classicalRequest(), job.reactionPredictionRequest(), predictionDiagnostics)
              : this.classicalChemistryService.predictReactionProducts(
                  job.reactionPredictionRequest());
      this.addClassicalPredictionDiagnostics(predictionDiagnostics, diagnostics);
      this.addReactionPredictionWarningDiagnostics(result, diagnostics);
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.REACTION_PREDICTION,
              result.hasCandidates()
                  ? ChemistryWorkflowStageStatus.COMPLETED
                  : ChemistryWorkflowStageStatus.WARNING,
              result.hasCandidates()
                  ? "Reaction product candidates ranked."
                  : "Reaction prediction has no product candidates."));
      return result;
    } catch (final RuntimeException exception) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.REACTION_PREDICTION_FAILED,
              ChemistryWorkflowStage.REACTION_PREDICTION,
              "reaction_prediction",
              job.id().value(),
              exception.getMessage()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.REACTION_PREDICTION,
              ChemistryWorkflowStageStatus.FAILED,
              "Reaction prediction failed."));
      return null;
    }
  }

  private void addClassicalPredictionDiagnostics(
      final List<ClassicalDiagnostic> classicalDiagnostics,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    for (int i = 0; i < classicalDiagnostics.size(); ++i) {
      final ClassicalDiagnostic diagnostic = classicalDiagnostics.get(i);
      diagnostics.add(
          ChemistryWorkflowDiagnostic.warning(
              ChemistryWorkflowDiagnosticCode.REACTION_PREDICTION_WARNING,
              ChemistryWorkflowStage.REACTION_PREDICTION,
              diagnostic.code().name(),
              diagnostic.target().kind() + ":" + diagnostic.target().id(),
              diagnostic.message()));
    }
  }

  private void addReactionPredictionWarningDiagnostics(
      final ReactionPredictionResult result, final List<ChemistryWorkflowDiagnostic> diagnostics) {
    final List<ReactionPredictionEvidence> resultDiagnostics = result.diagnostics();
    for (int diagnosticIndex = 0; diagnosticIndex < resultDiagnostics.size(); ++diagnosticIndex) {
      final ReactionPredictionEvidence evidence = resultDiagnostics.get(diagnosticIndex);
      if (ChemistryWorkflowService.isReactionPredictionWarning(evidence.kind())) {
        diagnostics.add(
            ChemistryWorkflowDiagnostic.warning(
                ChemistryWorkflowDiagnosticCode.REACTION_PREDICTION_WARNING,
                ChemistryWorkflowStage.REACTION_PREDICTION,
                evidence.kind().name(),
                result.request().id(),
                evidence.description()));
      }
    }
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final List<ReactionPredictionEvidence> evidenceList =
          candidates.get(candidateIndex).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidenceList.size(); ++evidenceIndex) {
        final ReactionPredictionEvidence evidence = evidenceList.get(evidenceIndex);
        if (ChemistryWorkflowService.isReactionPredictionWarning(evidence.kind())) {
          diagnostics.add(
              ChemistryWorkflowDiagnostic.warning(
                  ChemistryWorkflowDiagnosticCode.REACTION_PREDICTION_WARNING,
                  ChemistryWorkflowStage.REACTION_PREDICTION,
                  evidence.kind().name(),
                  result.request().id() + "/candidate/" + candidateIndex,
                  evidence.description()));
        }
      }
    }
  }

  private static boolean isReactionPredictionWarning(final ReactionPredictionEvidenceKind kind) {
    return kind == ReactionPredictionEvidenceKind.WARNING
        || kind == ReactionPredictionEvidenceKind.COMPETING_PATHWAY;
  }

  private ChemistryWorkflowRecommendationPlan planReactionFollowUp(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult reactionPredictionResult,
      final List<ChemistryWorkflowStageResult> stages) {
    if (reactionPredictionResult == null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.FOLLOW_UP_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Follow-up planning skipped because reaction prediction has no result."));
      return ChemistryWorkflowRecommendationPlan.EMPTY;
    }
    final ChemistryWorkflowRecommendationPlan plan =
        this.followUpPlanner.plan(job, reactionPredictionResult);
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.FOLLOW_UP_PLANNING,
            plan.empty()
                ? ChemistryWorkflowStageStatus.WARNING
                : ChemistryWorkflowStageStatus.COMPLETED,
            plan.empty()
                ? "Follow-up planning has no recommendations."
                : "Follow-up calculation plan prepared."));
    return plan;
  }

  private ReactionPredictionExplanation explainReactionPrediction(
      final ReactionPredictionResult reactionPredictionResult) {
    if (reactionPredictionResult == null) {
      return null;
    }
    return this.reactionPredictionExplainer.explain(reactionPredictionResult);
  }

  private ReactionConditionEffectProfile analyzeConditionEffects(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult reactionPredictionResult,
      final List<ChemistryWorkflowStageResult> stages) {
    if (reactionPredictionResult == null || !job.hasReactionPredictionRequest()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CONDITION_EFFECT_ANALYSIS,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Condition effect analysis skipped because reaction prediction has no result."));
      return null;
    }
    final ReactionConditionEffectProfile profile =
        this.classicalChemistryService.analyzeReactionConditionEffects(
            job.reactionPredictionRequest());
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.CONDITION_EFFECT_ANALYSIS,
            ChemistryWorkflowStageStatus.COMPLETED,
            profile.empty()
                ? "Condition effect analysis has no explicit effects."
                : "Condition effects analyzed from solvent, catalyst, temperature, time and atmosphere."));
    return profile;
  }

  private ReactionConditionScenarioPlan planConditionScenarios(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult reactionPredictionResult,
      final List<ChemistryWorkflowStageResult> stages) {
    if (reactionPredictionResult == null || !job.hasReactionPredictionRequest()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CONDITION_SCENARIO_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Condition scenario planning skipped because reaction prediction has no result."));
      return null;
    }
    final ReactionConditionScenarioPlan plan =
        this.conditionScenarioPlanner.plan(job.reactionPredictionRequest());
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.CONDITION_SCENARIO_PLANNING,
            plan.empty()
                ? ChemistryWorkflowStageStatus.WARNING
                : ChemistryWorkflowStageStatus.COMPLETED,
            plan.empty()
                ? "Condition scenario planning has no scenarios."
                : "Condition scenario matrix prepared."));
    return plan;
  }

  private ReactionConditionScenarioEvaluationPlan evaluateConditionScenarios(
      final ChemistryWorkflowJob job,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionPredictionResult reactionPredictionResult,
      final List<ChemistryWorkflowStageResult> stages) {
    if (reactionPredictionResult == null
        || conditionScenarioPlan == null
        || !job.hasReactionPredictionRequest()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.CONDITION_SCENARIO_EVALUATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Condition scenario evaluation skipped because scenario planning has no result."));
      return null;
    }
    final ReactionConditionScenarioEvaluationPlan plan =
        this.classicalChemistryService.evaluateReactionConditionScenarios(
            conditionScenarioPlan, job.reactionPredictionRequest());
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.CONDITION_SCENARIO_EVALUATION,
            plan.empty()
                ? ChemistryWorkflowStageStatus.WARNING
                : ChemistryWorkflowStageStatus.COMPLETED,
            plan.empty()
                ? "Condition scenario evaluation has no evaluated scenarios."
                : "Condition scenario predictions evaluated."));
    return plan;
  }

  private ElectronicStructureProblem prepareHamiltonian(
      final ChemistryWorkflowJob job,
      final ChemistryCapabilityReport capabilityReport,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.needsHamiltonian()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Hamiltonian preparation is not requested."));
      return null;
    }
    final ElectronicStructureProblem problem = job.electronicProblem();
    if (problem != null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.COMPLETED,
              "Hamiltonian is available in native Java model."));
      return problem;
    }
    if (job.classicalRequest() == null) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.HAMILTONIAN_PREPARATION_FAILED,
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              "missing",
              job.id().value(),
              "Electronic problem or classical request is required for Hamiltonian preparation."));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.FAILED,
              "Hamiltonian preparation failed."));
      return null;
    }
    if (ChemistryWorkflowService.hasUnsupportedNativeHamiltonianCapability(job, capabilityReport)) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.HAMILTONIAN_CAPABILITY_UNSUPPORTED,
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              "native_java",
              job.id().value(),
              capabilityReport.requireItem("integrals.full-eri").message()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.FAILED,
              "Hamiltonian preparation skipped because native Hamiltonian capability preflight failed."));
      return null;
    }
    final String capabilityError =
        this.nativeHamiltonianCapabilityError(job, job.classicalRequest());
    if (capabilityError != null) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.HAMILTONIAN_CAPABILITY_UNSUPPORTED,
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              "native_java",
              job.id().value(),
              capabilityError));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.FAILED,
              "Hamiltonian preparation is not supported by the current native Java capability profile."));
      return null;
    }
    try {
      final ElectronicStructureProblem generatedProblem =
          this.generateElectronicProblem(job, job.classicalRequest());
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.COMPLETED,
              ChemistryWorkflowService.hamiltonianPreparationMessage(
                  generatedProblem.hamiltonian())));
      return generatedProblem;
    } catch (final RuntimeException exception) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.HAMILTONIAN_PREPARATION_FAILED,
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              "native_java",
              job.id().value(),
              exception.getMessage()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
              ChemistryWorkflowStageStatus.FAILED,
              "Hamiltonian preparation failed."));
      return null;
    }
  }

  private String nativeHamiltonianCapabilityError(
      final ChemistryWorkflowJob job, final ClassicalCalculationRequest request) {
    if (job.needsQuantumProgram() && !request.hasActiveSpace()) {
      return null;
    }
    return this.preparationPipeline.unsupportedReason(request);
  }

  private ElectronicStructureProblem generateElectronicProblem(
      final ChemistryWorkflowJob job, final ClassicalCalculationRequest request) {
    final ClassicalCalculationRequest effectiveRequest =
        ChemistryWorkflowService.effectiveHamiltonianRequest(
            job, request, this.preparationPipeline);
    final Molecule molecule = ChemistryWorkflowService.hamiltonianMolecule(job, request);
    final ElectronicStructurePreparationResult preparationResult =
        this.preparationPipeline.prepare(effectiveRequest);
    final ElectronicHamiltonian hamiltonian = preparationResult.hamiltonian();
    final ActiveSpace activeSpace = hamiltonian.activeSpace();
    final ChemistryTask task =
        ChemistryTask.of(
            ChemistryTaskType.HAMILTONIAN_PREPARATION,
            effectiveRequest.basisSet(),
            activeSpace,
            effectiveRequest.method(),
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ConvergenceCriteria.DEFAULT);
    return ElectronicStructureProblem.of(
        ElectronicProblemId.of(job.id().value() + "_electronic_problem"),
        molecule,
        task,
        hamiltonian);
  }

  private static ClassicalCalculationRequest effectiveHamiltonianRequest(
      final ChemistryWorkflowJob job,
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    if (!job.needsQuantumProgram() || request.hasActiveSpace()) {
      return request;
    }
    return new ActiveSpaceRequestResolver(preparationPipeline, new ActiveSpaceSelector())
        .resolve(
            request,
            new ArrayList<ClassicalDiagnostic>(),
            ChemistryWorkflowStage.HAMILTONIAN_PREPARATION.name())
        .request();
  }

  private static Molecule hamiltonianMolecule(
      final ChemistryWorkflowJob job, final ClassicalCalculationRequest request) {
    final ChemistrySubject requestSubject = request.subject();
    if (requestSubject.subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return (Molecule) requestSubject;
    }
    if (job.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return (Molecule) job.subject();
    }
    throw new IllegalArgumentException(
        "Hamiltonian preparation requires a molecule subject in the classical request.");
  }

  private QubitPauliOperator mapHamiltonian(
      final ChemistryWorkflowJob job,
      final ElectronicStructureProblem problem,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.needsHamiltonian() || problem == null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.FERMION_MAPPING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Fermion mapping is not requested."));
      return null;
    }
    try {
      final QubitPauliOperator operator =
          this.qubitMapper.map(problem.hamiltonian(), job.fermionToQubitMapping());
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.FERMION_MAPPING,
              ChemistryWorkflowStageStatus.COMPLETED,
              "Hamiltonian mapped to qubit Pauli operator with "
                  + job.fermionToQubitMapping().name()
                  + "."));
      return operator;
    } catch (final RuntimeException exception) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.FERMION_MAPPING_FAILED,
              ChemistryWorkflowStage.FERMION_MAPPING,
              job.fermionToQubitMapping().name(),
              job.id().value(),
              exception.getMessage()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.FERMION_MAPPING,
              ChemistryWorkflowStageStatus.FAILED,
              "Fermion mapping failed."));
      return null;
    }
  }

  private ChemistryQuantumProgramPackage prepareQuantumProgramPackage(
      final ChemistryWorkflowJob job,
      final ElectronicStructureProblem problem,
      final QubitPauliOperator operator,
      final boolean generateQuantumProgram,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.needsQuantumProgram()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Quantum program generation is not requested."));
      return null;
    }
    if (problem == null || operator == null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Quantum program generation skipped until Hamiltonian is ready."));
      return null;
    }
    if (!generateQuantumProgram) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
              ChemistryWorkflowStageStatus.READY,
              "Quantum program package generation is ready."));
      return null;
    }
    try {
      final ChemistryQuantumProgramPackage programPackage =
          this.programPackageGenerator.generate(
              job.id().value() + "_quantum",
              problem.hamiltonian(),
              job.fermionToQubitMapping(),
              ChemistryWorkflowService.quantumPreparationProvenance(job, problem),
              job.hasQuantumProgramPackageOptions()
                  ? job.quantumProgramPackageOptions()
                  : ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program
                      .ChemistryQuantumProgramPackageOptions.FULL);
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
              programPackage.hasArtifacts()
                  ? ChemistryWorkflowStageStatus.COMPLETED
                  : ChemistryWorkflowStageStatus.WARNING,
              "Quantum IR program package generated with "
                  + programPackage.artifacts().size()
                  + " artifacts and "
                  + programPackage.skippedArtifacts().size()
                  + " skipped variants."));
      return programPackage;
    } catch (final RuntimeException exception) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.QUANTUM_PROGRAM_GENERATION_FAILED,
              ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
              "quantum-ir",
              job.id().value(),
              exception.getMessage()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
              ChemistryWorkflowStageStatus.FAILED,
              "Quantum program generation failed."));
      return null;
    }
  }

  private static ChemistryQuantumPreparationProvenance quantumPreparationProvenance(
      final ChemistryWorkflowJob job, final ElectronicStructureProblem problem) {
    if (job.classicalRequest() == null) {
      return ChemistryQuantumPreparationProvenance.of(
          "NATIVE_ELECTRONIC_PROBLEM",
          "PREBUILT_HAMILTONIAN",
          "UNKNOWN",
          "UNKNOWN",
          ChemistryWorkflowService.activeSpaceDescription(problem.hamiltonian().activeSpace()),
          job.id().value(),
          problem.hamiltonian().activeSpace().electronCount(),
          problem.hamiltonian().spinOrbitalCount(),
          problem.hamiltonian().activeSpace().orbitalCount(),
          job.fermionToQubitMapping(),
          false);
    }
    final ClassicalCalculationRequest request = job.classicalRequest();
    final ActiveSpace activeSpace = problem.hamiltonian().activeSpace();
    return ChemistryQuantumPreparationProvenance.of(
        ChemistryWorkflowService.referenceMethod(request),
        "JAVA_NATIVE_ELECTRONIC_STRUCTURE",
        request.method().spinTreatment().name(),
        request.basisSet().name().value(),
        ChemistryWorkflowService.activeSpaceDescription(activeSpace),
        request.id().value(),
        activeSpace.electronCount(),
        problem.hamiltonian().spinOrbitalCount(),
        activeSpace.orbitalCount(),
        job.fermionToQubitMapping(),
        ChemistryWorkflowService.orbitalOptimizationRequested(request));
  }

  private static String referenceMethod(final ClassicalCalculationRequest request) {
    if (ChemistryWorkflowService.orbitalOptimizationRequested(request)
        && !"CASSCF".equalsIgnoreCase(request.method().name().value())
        && !"MCSCF".equalsIgnoreCase(request.method().name().value())) {
      return "CASSCF_ORBITAL_OPTIMIZED:" + request.method().name().value();
    }
    return request.method().type().name() + ":" + request.method().name().value();
  }

  private static String activeSpaceDescription(final ActiveSpace activeSpace) {
    return activeSpace.electronCount() + "e/" + activeSpace.orbitalCount() + "o";
  }

  private static boolean orbitalOptimizationRequested(final ClassicalCalculationRequest request) {
    for (int i = 0; i < request.options().size(); ++i) {
      final String name = request.options().get(i).name();
      final String value = request.options().get(i).value();
      if ("hamiltonian_orbital_optimization".equals(name)) {
        return "casscf".equalsIgnoreCase(value)
            || "true".equalsIgnoreCase(value)
            || "enabled".equalsIgnoreCase(value);
      }
    }
    return "CASSCF".equalsIgnoreCase(request.method().name().value())
        || "MCSCF".equalsIgnoreCase(request.method().name().value());
  }

  private ErrorMitigationPlan planErrorMitigation(
      final ChemistryWorkflowJob job,
      final QuantumProgram quantumProgram,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.hasErrorMitigationRequest()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.ERROR_MITIGATION_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Error mitigation planning is not requested."));
      return null;
    }
    if (!job.needsQuantumProgram()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.ERROR_MITIGATION_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Error mitigation planning requires a quantum program workflow mode."));
      return null;
    }
    if (quantumProgram == null) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.ERROR_MITIGATION_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Error mitigation planning skipped until Quantum IR is ready."));
      return null;
    }
    final ErrorMitigationPlan plan = this.errorMitigationPlanner.plan(job.errorMitigationRequest());
    if (plan.hasErrors()) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.ERROR_MITIGATION_PLANNING_FAILED,
              ChemistryWorkflowStage.ERROR_MITIGATION_PLANNING,
              plan.status().name(),
              job.id().value(),
              "Error mitigation planner returned blocking diagnostics."));
    }
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.ERROR_MITIGATION_PLANNING,
            plan.hasErrors()
                ? ChemistryWorkflowStageStatus.FAILED
                : ChemistryWorkflowStageStatus.COMPLETED,
            plan.status().name()));
    return plan;
  }

  private VqeOptimizationResult optimizeVqe(
      final ChemistryWorkflowJob job,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final QubitPauliOperator qubitOperator,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (!job.hasVqeOptimizationRequest()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "VQE optimization is not requested."));
      return null;
    }
    if (!job.needsQuantumProgram()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "VQE optimization requires a quantum program workflow mode."));
      return null;
    }
    if (quantumProgramPackage == null || !quantumProgramPackage.hasArtifacts()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              ChemistryWorkflowStageStatus.SKIPPED,
              "VQE optimization skipped until Quantum IR package is ready."));
      return null;
    }
    final VqeEnergyBackend backend =
        this.vqeEnergyBackend == null && qubitOperator != null
            ? new StateVectorPauliVqeEnergyBackend(qubitOperator)
            : this.vqeEnergyBackend;
    if (backend == null) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.VQE_BACKEND_UNAVAILABLE,
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              "missing_backend",
              job.id().value(),
              "VQE optimization request is present, but ChemistryWorkflowService has no VQE energy backend."));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              ChemistryWorkflowStageStatus.FAILED,
              "VQE optimization backend is unavailable."));
      return null;
    }
    try {
      final VqeOptimizationResult result =
          new VqeOptimizer(backend).optimize(job.vqeOptimizationRequest());
      final ChemistryWorkflowStageStatus stageStatus =
          result.backendFailed()
              ? ChemistryWorkflowStageStatus.FAILED
              : result.converged()
                  ? ChemistryWorkflowStageStatus.COMPLETED
                  : ChemistryWorkflowStageStatus.WARNING;
      if (stageStatus == ChemistryWorkflowStageStatus.FAILED) {
        diagnostics.add(
            ChemistryWorkflowDiagnostic.error(
                ChemistryWorkflowDiagnosticCode.VQE_OPTIMIZATION_FAILED,
                ChemistryWorkflowStage.VQE_OPTIMIZATION,
                result.status().name(),
                job.id().value(),
                result.diagnostic()));
      }
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              stageStatus,
              "VQE optimization returned "
                  + result.status().name()
                  + " with "
                  + result.evaluationCount()
                  + " energy evaluations."));
      return result;
    } catch (final RuntimeException exception) {
      diagnostics.add(
          ChemistryWorkflowDiagnostic.error(
              ChemistryWorkflowDiagnosticCode.VQE_OPTIMIZATION_FAILED,
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              "exception",
              job.id().value(),
              exception.getMessage()));
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.VQE_OPTIMIZATION,
              ChemistryWorkflowStageStatus.FAILED,
              "VQE optimization failed."));
      return null;
    }
  }

  private ChemistryComputationParityPlan planComputationParity(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult reactionPredictionResult,
      final ReactionPredictionExplanation reactionPredictionExplanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final List<ChemistryWorkflowStageResult> stages) {
    if (reactionPredictionResult == null || !reactionPredictionResult.hasCandidates()) {
      stages.add(
          ChemistryWorkflowStageResult.of(
              ChemistryWorkflowStage.COMPUTATION_PARITY_PLANNING,
              ChemistryWorkflowStageStatus.SKIPPED,
              "Computation parity planning skipped because reaction prediction has no candidates."));
      return ChemistryComputationParityPlan.EMPTY;
    }
    final ChemistryComputationParityPlan plan =
        this.parityPlanner.plan(
            job.id().value(),
            reactionPredictionResult,
            reactionPredictionExplanation,
            recommendationPlan,
            classicalCalculationResult,
            quantumProgramPackage);
    stages.add(
        ChemistryWorkflowStageResult.of(
            ChemistryWorkflowStage.COMPUTATION_PARITY_PLANNING,
            !job.needsQuantumProgram() || plan.hasReadyItem()
                ? ChemistryWorkflowStageStatus.COMPLETED
                : ChemistryWorkflowStageStatus.WARNING,
            plan.hasReadyItem()
                ? "Classical and quantum computation parity is ready for at least one reaction candidate."
                : "Computation parity recorded available follow-up work for the requested workflow mode."));
    return plan;
  }

  private void addCoreDiagnostics(
      final ChemistryValidationResult result, final List<ChemistryWorkflowDiagnostic> diagnostics) {
    final List<ChemistryDiagnostic> coreDiagnostics = result.diagnostics();
    for (int i = 0; i < coreDiagnostics.size(); ++i) {
      final ChemistryDiagnostic diagnostic = coreDiagnostics.get(i);
      diagnostics.add(
          ChemistryWorkflowDiagnostic.of(
              this.severity(diagnostic.severity()),
              ChemistryWorkflowDiagnosticCode.CORE_VALIDATION_FAILED,
              ChemistryWorkflowStage.CORE_VALIDATION,
              diagnostic.code().name(),
              diagnostic.hasTarget() ? diagnostic.target().toString() : "",
              diagnostic.message()));
    }
  }

  private ChemistryWorkflowDiagnosticSeverity severity(final ChemistryDiagnosticSeverity severity) {
    if (severity == ChemistryDiagnosticSeverity.ERROR) {
      return ChemistryWorkflowDiagnosticSeverity.ERROR;
    }
    if (severity == ChemistryDiagnosticSeverity.WARNING) {
      return ChemistryWorkflowDiagnosticSeverity.WARNING;
    }
    return ChemistryWorkflowDiagnosticSeverity.INFO;
  }

  private ChemistryWorkflowResult result(
      final ChemistryWorkflowJob job,
      final ChemistryValidationResult validationResult,
      final ChemistryCapabilityReport capabilityReport,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionResult reactionPredictionResult,
      final ReactionPredictionExplanation reactionPredictionExplanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryComputationParityPlan computationParityPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryFollowUpCalculationPlan followUpCalculationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final ErrorMitigationPlan errorMitigationPlan,
      final VqeOptimizationResult vqeOptimizationResult,
      final List<ChemistryWorkflowStageResult> stages,
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    final ChemistryWorkflowDiagnosticSet diagnosticSet =
        ChemistryWorkflowDiagnosticSet.of(diagnostics);
    final ChemistryWorkflowStoragePlan storagePlan =
        this.storagePlanner.plan(
            job,
            reactionPredictionResult,
            reactionPredictionExplanation,
            recommendationPlan,
            classicalCalculationResult == null
                ? null
                : classicalCalculationResult.calculationId().value(),
            quantumProgramPackage == null ? List.of() : quantumProgramPackage.artifactIds(),
            conditionScenarioPlan == null ? 0 : conditionScenarioPlan.scenarios().size(),
            conditionScenarioEvaluationPlan == null
                ? 0
                : conditionScenarioEvaluationPlan.evaluations().size(),
            conditionScenarioPlan,
            conditionScenarioEvaluationPlan,
            classicalCalculationResult == null ? List.of() : List.of(classicalCalculationResult),
            quantumProgramPackage == null
                ? ChemistryQuantumProgramPackage.EMPTY
                : quantumProgramPackage,
            vqeOptimizationResult,
            computationParityPlan,
            List.of());
    return ChemistryWorkflowResult.of(
        job,
        this.status(stages, diagnosticSet),
        validationResult,
        capabilityReport,
        classicalPreflightResult,
        classicalCalculationPlan,
        classicalCalculationResult,
        reactionPredictionResult,
        reactionPredictionExplanation,
        conditionEffectProfile,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        recommendationPlan,
        followUpCalculationPlan,
        electronicProblem,
        quantumProgram,
        quantumProgramPackage,
        computationParityPlan,
        errorMitigationPlan,
        vqeOptimizationResult,
        storagePlan,
        stages,
        diagnosticSet);
  }

  private ChemistryWorkflowStatus status(
      final List<ChemistryWorkflowStageResult> stages,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    if (diagnostics.hasErrors()) {
      return ChemistryWorkflowStatus.FAILED;
    }
    for (int i = 0; i < stages.size(); ++i) {
      if (stages.get(i).status() == ChemistryWorkflowStageStatus.WARNING) {
        return ChemistryWorkflowStatus.PARTIAL;
      }
    }
    return ChemistryWorkflowStatus.SUCCESS;
  }
}
