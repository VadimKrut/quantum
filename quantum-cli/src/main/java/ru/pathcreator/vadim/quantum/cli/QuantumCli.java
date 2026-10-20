/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.backend.BackendDiagnostic;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobOptions;
import ru.pathcreator.vadim.quantum.application.backend.BackendJobRecord;
import ru.pathcreator.vadim.quantum.application.backend.InMemoryBackendJobRegistry;
import ru.pathcreator.vadim.quantum.application.backend.QuantumBackend;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerOptions;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerResult;
import ru.pathcreator.vadim.quantum.application.compiler.CompilerStageRecord;
import ru.pathcreator.vadim.quantum.application.benchmark.BenchmarkStageResult;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.compatibility.CompatibilityCheckResult;
import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.compatibility.TargetCompatibilityReport;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorCheck;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionBundleResult;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerificationIssue;
import ru.pathcreator.vadim.quantum.application.distribution.ProductDistributionVerificationResult;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCaseReport;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessCheck;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.report.ProductReportBundleResult;
import ru.pathcreator.vadim.quantum.application.resource.CircuitResourceEstimate;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnostic;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationOptions;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatTargetVerification;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimelineStep;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowReport;
import ru.pathcreator.vadim.quantum.chemistry.api.QuantumChemistry;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionCandidateRoadmap;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionComputationRoadmap;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionPredictionWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.api.workflow.ChemistryWorkflowRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeCalibrationCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismCatalogCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryCategory;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryDifficulty;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryQuery;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramSkip;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionSnapshot;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionTimeline;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowMode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityItem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowExecutionBundle;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationError;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class QuantumCli {

  public static final int EXIT_SUCCESS = 0;
  public static final int EXIT_WORKFLOW_ERROR = 1;
  public static final int EXIT_USAGE_ERROR = 2;
  public static final int EXIT_INTERNAL_ERROR = 3;

  private static final ObjectMapper JSON =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private QuantumCli() {}

  public static int run(final String[] args, final PrintStream out, final PrintStream err) {
    try {
      final CliArguments arguments = CliArguments.parse(args);
      if (arguments.help()) {
        out.println(helpText());
        return EXIT_SUCCESS;
      }
      return execute(arguments, out, err);
    } catch (final CliUsageException exception) {
      err.println(exception.getMessage());
      err.println("Use `quantum help` for command syntax.");
      return EXIT_USAGE_ERROR;
    } catch (final IOException exception) {
      err.println("I/O error: " + exception.getMessage());
      return EXIT_INTERNAL_ERROR;
    } catch (final RuntimeException exception) {
      err.println("Internal error: " + exception.getMessage());
      return EXIT_INTERNAL_ERROR;
    }
  }

  private static int execute(
      final CliArguments arguments, final PrintStream out, final PrintStream err)
      throws IOException {
    return switch (arguments.command()) {
      case "help" -> {
        out.println(helpText());
        yield EXIT_SUCCESS;
      }
      case "validate" -> validate(arguments, out);
      case "inspect" -> inspect(arguments, out);
      case "resources" -> resources(arguments, out);
      case "circuit" -> circuit(arguments, out);
      case "preflight" -> preflight(arguments, out);
      case "transform" -> transform(arguments, out);
      case "compile" -> compile(arguments, out);
      case "convert" -> convert(arguments, out);
      case "simulate" -> simulate(arguments, out);
      case "backend-dry-run" -> backendDryRun(arguments, out);
      case "workflow" -> workflow(arguments, out);
      case "benchmark" -> benchmark(arguments, out);
      case "compatibility" -> compatibility(arguments, out);
      case "verify-cross-format" -> verifyCrossFormat(arguments, out);
      case "regress-corpus" -> regressCorpus(arguments, out);
      case "release-readiness" -> releaseReadiness(arguments, out);
      case "target-profile" -> targetProfile(arguments, out);
      case "doctor" -> doctor(arguments, out);
      case "product-audit" -> productAudit(arguments, out);
      case "product-report" -> productReport(arguments, out);
      case "product-distribution" -> productDistribution(arguments, out);
      case "product-verify-distribution" -> productVerifyDistribution(arguments, out);
      case "chemistry-library" -> chemistryLibrary(arguments, out);
      case "chemistry-predict" -> chemistryPredict(arguments, out);
      case "chemistry-workflow" -> chemistryWorkflow(arguments, out);
      case "chemistry-calibration-coverage" -> chemistryCalibrationCoverage(arguments, out);
      case "chemistry-mechanism-coverage" -> chemistryMechanismCoverage(arguments, out);
      case "chemistry-storage-summary" -> chemistryStorageSummary(arguments, out);
      default -> {
        err.println("Unknown command: " + arguments.command() + ".");
        yield EXIT_USAGE_ERROR;
      }
    };
  }

  private static int validate(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = loadProgram(arguments);
    if (!load.success()) {
      printResult(
          arguments,
          out,
          Map.of(
              "command", "validate", "status", "import_failed", "diagnostics", load.diagnostics()),
          "Import failed.");
      return EXIT_WORKFLOW_ERROR;
    }
    final ValidationResult validation = Quantum.validate(load.program());
    final Map<String, Object> payload = validationPayload(validation);
    payload.put("command", "validate");
    printResult(
        arguments,
        out,
        payload,
        validation.isValid()
            ? "Validation: valid"
            : "Validation: invalid (" + validation.errorCount() + " errors)");
    return validation.isValid() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int inspect(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "inspect", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final ProgramInspectionResult inspection = Quantum.inspect(load.program());
    final Map<String, Object> payload = inspectionPayload(inspection);
    payload.put("command", "inspect");
    printResult(
        arguments,
        out,
        payload,
        "Inspection: circuits="
            + inspection.circuitCount()
            + ", qubits="
            + inspection.qubitCount()
            + ", operations="
            + inspection.operationCount());
    return EXIT_SUCCESS;
  }

  private static int circuit(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "circuit", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final ProgramTimeline timeline = Quantum.timeline(load.program());
    final Map<String, Object> payload = timelinePayload(timeline);
    payload.put("command", "circuit");
    printResult(
        arguments, out, payload, "Circuit timeline: circuits=" + timeline.circuits().size());
    return EXIT_SUCCESS;
  }

  private static int preflight(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "preflight", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final IntegrationFormat format = arguments.requiredOutputFormat();
    final CapabilityPreflightResult preflight = Quantum.preflight(format, load.program());
    final Map<String, Object> payload = preflightPayload(preflight);
    payload.put("command", "preflight");
    payload.put("targetFormat", format.name());
    printResult(arguments, out, payload, "Preflight: " + preflight.status());
    return preflight.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int resources(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "resources", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final ResourceEstimate estimate =
        Quantum.estimateResources(load.program(), arguments.intOption("max-qubits", 20));
    final Map<String, Object> payload = resourcePayload(estimate);
    payload.put("command", "resources");
    printResult(
        arguments,
        out,
        payload,
        "Resources: qubits="
            + estimate.qubitCount()
            + ", operations="
            + estimate.operationCount()
            + ", stateVectorBytes="
            + estimate.estimatedStateVectorBytes());
    return EXIT_SUCCESS;
  }

  private static int chemistryLibrary(final CliArguments arguments, final PrintStream out)
      throws JsonProcessingException {
    final ChemistryLibraryQuery query =
        ChemistryLibraryQuery.builder()
            .text(arguments.stringOption("query", null))
            .kind(
                chemistryEnumOption(
                    ChemistryLibraryEntryKind.class, arguments.stringOption("kind", null), "kind"))
            .category(
                chemistryEnumOption(
                    ChemistryLibraryCategory.class,
                    arguments.stringOption("category", null),
                    "category"))
            .difficulty(
                chemistryEnumOption(
                    ChemistryLibraryDifficulty.class,
                    arguments.stringOption("difficulty", null),
                    "difficulty"))
            .limit(arguments.intOption("limit", 0))
            .build();
    final List<ChemistryLibraryEntry> entries = QuantumChemistry.searchLibrary(query);
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "chemistry-library");
    payload.put("count", entries.size());
    payload.put("entries", chemistryLibraryEntryPayloads(entries));
    printResult(arguments, out, payload, "Chemistry library: entries=" + entries.size());
    return EXIT_SUCCESS;
  }

  private static int chemistryPredict(final CliArguments arguments, final PrintStream out)
      throws JsonProcessingException {
    final ReactionSide reactants = chemistryReactants(arguments);
    final List<ReactionProductCandidate> seedCandidates = chemistrySeedCandidates(arguments);
    final ReactionConditions conditions = chemistryReactionConditions(arguments);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            arguments.stringOption("id", "cli-organic-reaction-prediction"),
            reactants,
            conditions,
            List.of(
                chemistryEnumOption(
                    OrganicReactionDomain.class,
                    arguments.stringOption("domain", "unknown"),
                    "domain")),
            seedCandidates,
            arguments.stringOption("note", "CLI organic reaction prediction"));
    final List<ClassicalCalculationOption> calculationOptions =
        chemistryCalculationOptions(arguments);
    final OrganicReactionPredictionWorkflowResult result =
        chemistryPredictionWorkflowResult(
            arguments, request, reactants, conditions, seedCandidates, calculationOptions);
    final Map<String, Object> payload = chemistryPredictionWorkflowPayload(result);
    payload.put("command", "chemistry-predict");
    if (arguments.flag("benchmark") && result.hasPredictionResult()) {
      payload.put(
          "benchmark",
          chemistryReactionBenchmarkPayload(
              QuantumChemistry.reactionPredictionBenchmark(
                  result.predictionResult(),
                  BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions())));
    }
    printResult(
        arguments,
        out,
        payload,
        "Chemistry prediction: "
            + result.status()
            + ", candidates="
            + chemistryCandidateCount(result)
            + ", recommendations="
            + result.recommendationPlan().size());
    return result.status().name().equals("FAILED") ? EXIT_WORKFLOW_ERROR : EXIT_SUCCESS;
  }

  private static OrganicReactionPredictionWorkflowResult chemistryPredictionWorkflowResult(
      final CliArguments arguments,
      final ReactionPredictionRequest request,
      final ReactionSide reactants,
      final ReactionConditions conditions,
      final List<ReactionProductCandidate> seedCandidates,
      final List<ClassicalCalculationOption> calculationOptions) {
    if (calculationOptions.isEmpty()) {
      return QuantumChemistry.predictOrganicReaction(request);
    }
    if (seedCandidates.isEmpty()) {
      return QuantumChemistry.predictOrganicReactionWithGeneratedCandidateCalculations(
          request,
          arguments.stringOption("calculation-id", "cli_chemistry_prediction_calculation"),
          chemistryElectronicStructureMethod(arguments.stringOption("method", "hartree-fock")),
          chemistryBasisSet(arguments.stringOption("basis", "STO-3G")),
          calculationOptions);
    }
    final ReactionSide calculationProducts = seedCandidates.get(0).products();
    return QuantumChemistry.predictOrganicReaction(
        chemistryPredictionCalculationRequest(
            arguments, reactants, conditions, calculationProducts, calculationOptions),
        request);
  }

  private static int chemistryWorkflow(final CliArguments arguments, final PrintStream out)
      throws JsonProcessingException {
    final Molecule subject =
        chemistryMoleculeFromEntry(arguments.requiredStringOption("subject-entry"));
    final List<ClassicalCalculationKind> calculationKinds =
        chemistryCalculationKinds(
            arguments.stringOption("calculation-kinds", "hamiltonian_preparation"));
    final List<ReactionProductCandidate> seedCandidates = chemistrySeedCandidates(arguments);
    final String workflowId = arguments.stringOption("id", "cli_chemistry_workflow");
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder(workflowId, subject)
            .calculationId(arguments.stringOption("calculation-id", "cli_chemistry_calculation"))
            .predictionId(arguments.stringOption("prediction-id", workflowId + "_prediction"))
            .mode(
                chemistryEnumOption(
                    ChemistryWorkflowMode.class,
                    arguments.stringOption("mode", "full_quantum_preparation"),
                    "mode"))
            .method(
                chemistryElectronicStructureMethod(
                    arguments.stringOption("method", "hartree-fock")))
            .basisSet(chemistryBasisSet(arguments.stringOption("basis", "STO-3G")))
            .activeSpace(chemistryActiveSpace(arguments, calculationKinds))
            .calculationKinds(calculationKinds)
            .calculationOptions(chemistryCalculationOptions(arguments))
            .reactionConditions(chemistryReactionConditions(arguments))
            .domains(
                List.of(
                    chemistryEnumOption(
                        OrganicReactionDomain.class,
                        arguments.stringOption("domain", "unknown"),
                        "domain")))
            .seedCandidates(seedCandidates)
            .mapping(
                chemistryEnumOption(
                    FermionToQubitMapping.class,
                    arguments.stringOption("mapping", "jordan_wigner"),
                    "mapping"))
            .note(arguments.stringOption("note", "CLI chemistry workflow prediction"))
            .build();
    final ChemistryWorkflowExecutionBundle bundle =
        arguments.flag("execute-followups")
            ? chemistryWorkflowExecutionBundle(arguments, request)
            : null;
    final ChemistryWorkflowResult result =
        bundle == null ? QuantumChemistry.runWorkflow(request) : bundle.workflowResult();
    final Map<String, Object> payload =
        bundle == null
            ? chemistryWorkflowPayload(result)
            : chemistryWorkflowExecutionBundlePayload(bundle);
    payload.put("command", "chemistry-workflow");
    printResult(
        arguments,
        out,
        payload,
        "Chemistry workflow: "
            + result.status()
            + ", stages="
            + result.stageResults().size()
            + ", quantumArtifacts="
            + result.quantumProgramPackage().artifacts().size()
            + ", followUps="
            + (bundle == null ? 0 : bundle.followUpResultCount()));
    return result.diagnostics().hasErrors() ? EXIT_WORKFLOW_ERROR : EXIT_SUCCESS;
  }

  private static ChemistryWorkflowExecutionBundle chemistryWorkflowExecutionBundle(
      final CliArguments arguments, final ChemistryWorkflowRequest request) {
    final int followUpLimit = arguments.intOption("followup-limit", -1);
    return followUpLimit < 0
        ? QuantumChemistry.runWorkflowWithAllFollowUpCalculations(request)
        : QuantumChemistry.runWorkflowWithFollowUpCalculationLimit(request, followUpLimit);
  }

  private static int chemistryCalibrationCoverage(
      final CliArguments arguments, final PrintStream out) throws JsonProcessingException {
    final ReactionOutcomeCalibrationCoverageReport report =
        new ReactionOutcomeCalibrationCoverageAnalyzer()
            .analyze(BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "chemistry-calibration-coverage");
    payload.put("observationCount", report.observationCount());
    payload.put("datasetCount", report.datasetCount());
    payload.put("exactObservationCount", report.exactObservationCount());
    payload.put(
        "externallyVerifiedExactObservationCount",
        report.externallyVerifiedExactObservationCount());
    payload.put("classPriorObservationCount", report.classPriorObservationCount());
    payload.put(
        "sourceIdentifiedExactObservationCount", report.sourceIdentifiedExactObservationCount());
    payload.put("sourceTracedPriorObservationCount", report.sourceTracedPriorObservationCount());
    payload.put(
        "productionReadyForEmpiricalPrediction", report.productionReadyForEmpiricalPrediction());
    payload.put("missingDomains", report.missingDomains());
    payload.put("missingMechanismFamilies", report.missingMechanismFamilies());
    payload.put("incompleteDatasetIds", report.incompleteDatasetIds());
    payload.put("weakDatasetIds", report.weakDatasetIds());
    payload.put("unreferencedExactObservationIds", report.unreferencedExactObservationIds());
    payload.put("sourceTracedPriorObservationIds", report.sourceTracedPriorObservationIds());
    payload.put("domainCounts", report.domainCounts());
    payload.put("mechanismFamilyCounts", report.mechanismCounts());
    printResult(
        arguments,
        out,
        payload,
        "Chemistry calibration coverage: externallyVerifiedExact="
            + report.externallyVerifiedExactObservationCount()
            + ", exact="
            + report.exactObservationCount()
            + ", productionReady="
            + report.productionReadyForEmpiricalPrediction());
    return EXIT_SUCCESS;
  }

  private static int chemistryMechanismCoverage(final CliArguments arguments, final PrintStream out)
      throws JsonProcessingException {
    final ReactionMechanismCatalogCoverageReport report =
        QuantumChemistry.builtInReactionMechanismCatalogCoverage();
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "chemistry-mechanism-coverage");
    payload.put("patternCount", report.patternCount());
    payload.put("familyCount", report.familyCount());
    payload.put("eventKindCount", report.eventKindCount());
    payload.put("syntheticGenericPatternCount", report.syntheticGenericPatternCount());
    payload.put("chemicallyUsablePatternCount", report.chemicallyUsablePatternCount());
    payload.put("productionDetailedPatternCount", report.productionDetailedPatternCount());
    payload.put("underSpecifiedPatternCount", report.underSpecifiedPatternCount());
    payload.put("complete", report.complete());
    payload.put("taxonomyComplete", report.taxonomyComplete());
    payload.put("productionDetailedComplete", report.productionDetailedComplete());
    payload.put("productionCoverageRatio", report.productionCoverageRatio());
    payload.put("chemicallyUsableCoverageRatio", report.chemicallyUsableCoverageRatio());
    payload.put("missingFamilies", report.missingFamilies());
    payload.put("duplicateFamilies", report.duplicateFamilies());
    payload.put("unusedEventKinds", report.unusedEventKinds());
    payload.put("weakPatternIds", report.weakPatternIds());
    payload.put("syntheticGenericPatternIds", report.syntheticGenericPatternIds());
    payload.put("underSpecifiedPatternIds", report.underSpecifiedPatternIds());
    printResult(
        arguments,
        out,
        payload,
        "Chemistry mechanism coverage: patterns="
            + report.patternCount()
            + ", productionDetailed="
            + report.productionDetailedPatternCount()
            + ", complete="
            + report.complete());
    return EXIT_SUCCESS;
  }

  private static Map<String, Object> chemistryReactionBenchmarkPayload(
      final ReactionOutcomeBenchmarkReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("exactObservationCount", report.exactObservationCount());
    payload.put("comparableObservationCount", report.comparableObservationCount());
    payload.put("unmatchedObservationCount", report.unmatchedObservationCount());
    payload.put("comparableCoverage", report.comparableCoverage());
    payload.put("meanAbsoluteError", report.meanAbsoluteError());
    payload.put("weightedMeanAbsoluteError", report.weightedMeanAbsoluteError());
    final ArrayList<Map<String, Object>> matches = new ArrayList<>(report.matches().size());
    for (int i = 0; i < report.matches().size(); ++i) {
      matches.add(chemistryReactionBenchmarkMatchPayload(report.matches().get(i)));
    }
    payload.put("matches", matches);
    return payload;
  }

  private static Map<String, Object> chemistryReactionBenchmarkMatchPayload(
      final ReactionOutcomeBenchmarkMatch match) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("observationId", match.observationId());
    payload.put("channelLabel", match.channelLabel());
    payload.put("matched", match.matched());
    payload.put("observedProbability", match.observedProbability());
    payload.put("predictedProbability", match.predictedProbability());
    payload.put("absoluteError", match.absoluteError());
    payload.put("reason", match.reason());
    return payload;
  }

  private static int chemistryStorageSummary(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<ChemistryStorageDocument> read =
        service.read(Files.readString(arguments.requiredInputPath()));
    if (!read.success()) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("command", "chemistry-storage-summary");
      payload.put("status", "read_failed");
      payload.put("diagnostics", chemistryStorageDiagnosticPayloads(read.diagnostics()));
      printResult(
          arguments,
          out,
          payload,
          "Chemistry storage read failed: diagnostics=" + read.diagnostics().size());
      return EXIT_WORKFLOW_ERROR;
    }
    final ChemistryStorageDocument document = read.value();
    final Map<String, Object> payload = chemistryStorageDocumentPayload(document);
    payload.put("command", "chemistry-storage-summary");
    if (arguments.hasOption("reaction-id")) {
      final String reactionId = arguments.requiredStringOption("reaction-id");
      final ChemistryStorageReactionTimeline timeline =
          service.reactionTimeline(document, reactionId);
      payload.put("timeline", chemistryStorageTimelinePayload(timeline));
      if (arguments.hasOption("history-index")) {
        payload.put(
            "snapshot",
            chemistryStorageSnapshotPayload(
                service.reactionSnapshot(
                    document, reactionId, arguments.intOption("history-index", 0))));
      }
    }
    printResult(
        arguments,
        out,
        payload,
        "Chemistry storage: molecules="
            + document.molecules().size()
            + ", reactions="
            + document.reactions().size()
            + ", roadmapRecords="
            + document.roadmapRecords().size());
    return EXIT_SUCCESS;
  }

  private static int transform(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "transform", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final TransformationResult transform =
        Quantum.transform(load.program(), transformationOptions(arguments));
    if (arguments.hasOutputPath()) {
      Quantum.writeJson(arguments.outputPath(), transform.transformedProgram());
    }
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "transform");
    payload.put("success", transform.isSuccess());
    payload.put("appliedStepCount", transform.appliedSteps().size());
    payload.put("skippedStepCount", transform.skippedSteps().size());
    payload.put("diagnosticCount", transform.diagnostics().size());
    printResult(
        arguments,
        out,
        payload,
        "Transform: applied="
            + transform.appliedSteps().size()
            + ", skipped="
            + transform.skippedSteps().size());
    return transform.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int compile(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "compile", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final IntegrationFormat format = arguments.requiredOutputFormat();
    final CompilerResult result =
        Quantum.compile(format, load.program(), compilerOptions(arguments));
    if (result.isSuccess() && arguments.hasOutputPath()) {
      Files.writeString(arguments.outputPath(), result.exportResult().content());
    }
    final Map<String, Object> payload = compilerPayload(result);
    payload.put("command", "compile");
    printResult(arguments, out, payload, "Compile: " + result.status());
    return result.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int convert(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "convert", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final String content;
    if (arguments.outputFormatName().equals("json")) {
      final QuantumIrWriteResult write = Quantum.writeJson(load.program());
      if (!write.isSuccess()) {
        printResult(
            arguments,
            out,
            Map.of("command", "convert", "success", false),
            "Convert: failed to write JSON.");
        return EXIT_WORKFLOW_ERROR;
      }
      content = write.content();
    } else {
      final IntegrationFormat format = arguments.requiredOutputFormat();
      final ExportResult export = Quantum.exportProgram(format, load.program());
      if (!export.isSuccess()) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", "convert");
        payload.put("success", false);
        payload.put("diagnostics", diagnosticsPayload(export.diagnostics()));
        printResult(arguments, out, payload, "Convert: export failed.");
        return EXIT_WORKFLOW_ERROR;
      }
      content = export.content();
    }
    if (arguments.hasOutputPath()) {
      Files.writeString(arguments.outputPath(), content);
      printResult(
          arguments,
          out,
          Map.of(
              "command", "convert", "success", true, "output", arguments.outputPath().toString()),
          "Convert: wrote " + arguments.outputPath());
    } else {
      out.print(content);
    }
    return EXIT_SUCCESS;
  }

  private static int simulate(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "simulate", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final SimulationResult simulation =
        Quantum.simulate(
            load.program(),
            SimulationOptions.builder()
                .shots(arguments.intOption("shots", 1024))
                .seed(arguments.longOption("seed", 1L))
                .maxQubits(arguments.intOption("max-qubits", 20))
                .captureStateVector(arguments.booleanOption("state-vector", true))
                .build());
    final Map<String, Object> payload = simulationPayload(simulation);
    payload.put("command", "simulate");
    printResult(
        arguments,
        out,
        payload,
        "Simulation: "
            + (simulation.isSuccess() ? "success" : "failed")
            + ", shots="
            + simulation.shots()
            + ", outcomes="
            + simulation.counts().size());
    return simulation.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int backendDryRun(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "backend-dry-run", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final IntegrationFormat format = arguments.requiredOutputFormat();
    final QuantumBackend backend = Quantum.dryRunBackend(format);
    final InMemoryBackendJobRegistry registry = Quantum.backendJobRegistry();
    final BackendJobRecord record =
        registry.submit(
            backend,
            load.program(),
            BackendJobOptions.builder()
                .simulationOptions(
                    SimulationOptions.builder()
                        .shots(arguments.intOption("shots", 1024))
                        .seed(arguments.longOption("seed", 1L))
                        .maxQubits(arguments.intOption("max-qubits", 20))
                        .captureStateVector(arguments.booleanOption("state-vector", true))
                        .build())
                .metadata("origin", "quantum-cli")
                .build());
    final Map<String, Object> payload = backendJobRecordPayload(record);
    payload.put("command", "backend-dry-run");
    payload.put("historyCount", registry.history().count());
    printResult(
        arguments,
        out,
        payload,
        "Backend dry-run: "
            + record.status().name()
            + ", accepted="
            + record.isAccepted()
            + ", trackingId="
            + record.trackingId().value());
    return record.isAccepted() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int workflow(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "workflow", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final IntegrationFormat format = arguments.requiredOutputFormat();
    final ProductWorkflowOptions.Builder options =
        ProductWorkflowOptions.builder()
            .simulationOptions(
                SimulationOptions.builder()
                    .shots(arguments.intOption("shots", 1024))
                    .seed(arguments.longOption("seed", 1L))
                    .maxQubits(arguments.intOption("max-qubits", 20))
                    .captureStateVector(arguments.booleanOption("state-vector", true))
                    .build())
            .compilerOptions(compilerOptions(arguments))
            .resourceMaxQubits(
                arguments.intOption("resource-max-qubits", arguments.intOption("max-qubits", 20)))
            .runValidation(!arguments.flag("skip-validation"))
            .runInspection(!arguments.flag("skip-inspection"))
            .runResources(!arguments.flag("skip-resources"))
            .runTimeline(!arguments.flag("skip-timeline"))
            .runPreflight(!arguments.flag("skip-preflight"))
            .runSimulation(!arguments.flag("skip-simulation"))
            .runCompiler(!arguments.flag("skip-compiler"))
            .runBackendDryRun(!arguments.flag("skip-backend"));
    if (arguments.flag("fast")) {
      options.fastWorkflow().runValidation(!arguments.flag("skip-validation"));
    }
    final ProductWorkflowReport report =
        Quantum.runProductWorkflow(format, load.program(), options.build());
    final Map<String, Object> payload = workflowPayload(report);
    payload.put("command", "workflow");
    printResult(
        arguments,
        out,
        payload,
        "Workflow: "
            + report.status()
            + ", operations="
            + (report.inspection() == null ? "skipped" : report.inspection().operationCount())
            + ", target="
            + format.name());
    return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static CompilerOptions compilerOptions(final CliArguments arguments) {
    final CompilerOptions.Builder options = CompilerOptions.builder();
    if (arguments.flag("fast")) {
      options.fastExportOnly();
    }
    if (arguments.flag("skip-validation")) {
      options.skipInitialValidation().skipTransformedValidation();
    }
    if (arguments.flag("skip-inspection")) {
      options.skipInitialInspection().skipTransformedInspection();
    }
    if (arguments.flag("skip-preflight")) {
      options.skipInitialPreflight().skipTransformedPreflight();
    }
    if (arguments.flag("skip-transformation")) {
      options.skipTransformation();
    }
    if (arguments.flag("skip-transformed-validation")) {
      options.skipTransformedValidation();
    }
    if (arguments.flag("skip-transformed-inspection")) {
      options.skipTransformedInspection();
    }
    if (arguments.flag("skip-transformed-preflight")) {
      options.skipTransformedPreflight();
    }
    return options.build();
  }

  private static int benchmark(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <path>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    final String inputFormat =
        arguments.inputFormatName().equals("auto")
            ? detectInputFormat(inputPath)
            : arguments.inputFormatName();
    final IntegrationFormat targetFormat = arguments.requiredOutputFormat();
    final ProductBenchmarkOptions options =
        ProductBenchmarkOptions.builder()
            .warmupIterations(arguments.intOption("warmup", 1))
            .measurementIterations(arguments.intOption("iterations", 3))
            .includeImport(!arguments.flag("skip-import"))
            .workflowOptions(
                ProductWorkflowOptions.builder()
                    .simulationOptions(
                        SimulationOptions.builder()
                            .shots(arguments.intOption("shots", 1024))
                            .seed(arguments.longOption("seed", 1L))
                            .maxQubits(arguments.intOption("max-qubits", 20))
                            .captureStateVector(arguments.booleanOption("state-vector", true))
                            .build())
                    .resourceMaxQubits(
                        arguments.intOption(
                            "resource-max-qubits", arguments.intOption("max-qubits", 20)))
                    .runBackendDryRun(!arguments.flag("skip-backend"))
                    .build())
            .build();
    final ProductBenchmarkReport report;
    if (inputFormat.equals("json")) {
      final ProgramLoadResult load = requireLoadedProgram(arguments);
      if (!load.success()) {
        printImportFailure(arguments, out, "benchmark", load);
        return EXIT_WORKFLOW_ERROR;
      }
      report = Quantum.benchmark(targetFormat, load.program(), options);
    } else {
      report =
          Quantum.benchmarkExternal(
              parseIntegrationFormat(inputFormat),
              Files.readString(inputPath),
              targetFormat,
              options);
    }
    final Map<String, Object> payload = benchmarkPayload(report);
    payload.put("command", "benchmark");
    printResult(
        arguments,
        out,
        payload,
        "Benchmark: stages="
            + report.stageCount()
            + ", totalAverage="
            + report.totalAverageNanos()
            + " ns");
    return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int compatibility(final CliArguments arguments, final PrintStream out)
      throws IOException {
    final ProgramLoadResult load = requireLoadedProgram(arguments);
    if (!load.success()) {
      printImportFailure(arguments, out, "compatibility", load);
      return EXIT_WORKFLOW_ERROR;
    }
    final ProductCompatibilityMatrix matrix =
        Quantum.compatibilityMatrix(
            load.program(),
            ProductWorkflowOptions.builder()
                .simulationOptions(
                    SimulationOptions.builder()
                        .shots(arguments.intOption("shots", 1024))
                        .seed(arguments.longOption("seed", 1L))
                        .maxQubits(arguments.intOption("max-qubits", 20))
                        .captureStateVector(arguments.booleanOption("state-vector", true))
                        .build())
                .resourceMaxQubits(
                    arguments.intOption(
                        "resource-max-qubits", arguments.intOption("max-qubits", 20)))
                .runBackendDryRun(!arguments.flag("skip-backend"))
                .build());
    final Map<String, Object> payload = compatibilityPayload(matrix);
    payload.put("command", "compatibility");
    printResult(
        arguments,
        out,
        payload,
        "Compatibility: targets=" + matrix.targets().size() + ", success=" + matrix.isSuccess());
    return matrix.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int verifyCrossFormat(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <path>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    final String inputFormat =
        arguments.inputFormatName().equals("auto")
            ? detectInputFormat(inputPath)
            : arguments.inputFormatName();
    if (inputFormat.equals("json")) {
      throw new CliUsageException(
          "verify-cross-format requires an external input format, not native JSON.");
    }
    final CrossFormatVerificationReport report =
        Quantum.verifyCrossFormat(
            parseIntegrationFormat(inputFormat),
            Files.readString(inputPath),
            SimulationOptions.builder()
                .shots(arguments.intOption("shots", 1024))
                .seed(arguments.longOption("seed", 1L))
                .maxQubits(arguments.intOption("max-qubits", 20))
                .captureStateVector(arguments.booleanOption("state-vector", false))
                .build());
    final Map<String, Object> payload = crossFormatPayload(report);
    payload.put("command", "verify-cross-format");
    printResult(
        arguments,
        out,
        payload,
        "Cross-format verification: targets="
            + report.targets().size()
            + ", success="
            + report.isSuccess());
    return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int regressCorpus(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <directory>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    if (!Files.isDirectory(inputPath)) {
      throw new CliUsageException("regress-corpus requires --input to point to a directory.");
    }
    final CorpusRegressionReport report =
        Quantum.runCorpusRegression(
            collectCorpusCases(inputPath),
            ProductWorkflowOptions.builder()
                .simulationOptions(
                    SimulationOptions.builder()
                        .shots(arguments.intOption("shots", 1024))
                        .seed(arguments.longOption("seed", 1L))
                        .maxQubits(arguments.intOption("max-qubits", 20))
                        .captureStateVector(arguments.booleanOption("state-vector", false))
                        .build())
                .resourceMaxQubits(
                    arguments.intOption(
                        "resource-max-qubits", arguments.intOption("max-qubits", 20)))
                .runBackendDryRun(!arguments.flag("skip-backend"))
                .build());
    final Map<String, Object> payload = corpusRegressionPayload(report);
    payload.put("command", "regress-corpus");
    printResult(
        arguments,
        out,
        payload,
        "Corpus regression: cases=" + report.caseCount() + ", failures=" + report.failureCount());
    return report.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int releaseReadiness(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <directory>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    if (!Files.isDirectory(inputPath)) {
      throw new CliUsageException("release-readiness requires --input to point to a directory.");
    }
    final ReleaseReadinessReport report =
        Quantum.releaseReadiness(
            collectCorpusCases(inputPath),
            arguments.requiredOutputFormat(),
            ProductBenchmarkOptions.builder()
                .warmupIterations(arguments.intOption("warmup", 0))
                .measurementIterations(arguments.intOption("iterations", 1))
                .workflowOptions(
                    ProductWorkflowOptions.builder()
                        .simulationOptions(
                            SimulationOptions.builder()
                                .shots(arguments.intOption("shots", 1024))
                                .seed(arguments.longOption("seed", 1L))
                                .maxQubits(arguments.intOption("max-qubits", 20))
                                .captureStateVector(arguments.booleanOption("state-vector", false))
                                .build())
                        .resourceMaxQubits(
                            arguments.intOption(
                                "resource-max-qubits", arguments.intOption("max-qubits", 20)))
                        .runBackendDryRun(!arguments.flag("skip-backend"))
                        .build())
                .build());
    final Map<String, Object> payload = releaseReadinessPayload(report);
    payload.put("command", "release-readiness");
    printResult(
        arguments,
        out,
        payload,
        "Release readiness: "
            + report.status()
            + ", failedChecks="
            + report.failedCheckCount()
            + ", warnings="
            + report.warningCheckCount());
    return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int productAudit(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <directory>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    if (!Files.isDirectory(inputPath)) {
      throw new CliUsageException("product-audit requires --input to point to a directory.");
    }
    final ProductAuditReport report = productAuditReport(arguments, inputPath);
    final Map<String, Object> payload = productAuditPayload(report);
    payload.put("command", "product-audit");
    printResult(
        arguments,
        out,
        payload,
        "Product audit: "
            + report.status()
            + ", failedChecks="
            + report.failedCheckCount()
            + ", warnings="
            + report.warningCheckCount());
    return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static ProductAuditReport productAuditReport(
      final CliArguments arguments, final Path inputPath) throws IOException {
    return Quantum.productAudit(
        arguments.pathOption("project-root", Path.of("").toAbsolutePath().normalize()),
        collectCorpusCases(inputPath),
        arguments.requiredOutputFormat(),
        ProductBenchmarkOptions.builder()
            .warmupIterations(arguments.intOption("warmup", 0))
            .measurementIterations(arguments.intOption("iterations", 1))
            .workflowOptions(
                ProductWorkflowOptions.builder()
                    .simulationOptions(
                        SimulationOptions.builder()
                            .shots(arguments.intOption("shots", 1024))
                            .seed(arguments.longOption("seed", 1L))
                            .maxQubits(arguments.intOption("max-qubits", 20))
                            .captureStateVector(arguments.booleanOption("state-vector", false))
                            .build())
                    .resourceMaxQubits(
                        arguments.intOption(
                            "resource-max-qubits", arguments.intOption("max-qubits", 20)))
                    .runBackendDryRun(!arguments.flag("skip-backend"))
                    .build())
            .build());
  }

  private static int productReport(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <directory>.");
    }
    if (!arguments.hasOutputPath()) {
      throw new CliUsageException("Command requires --output <directory>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    if (!Files.isDirectory(inputPath)) {
      throw new CliUsageException("product-report requires --input to point to a directory.");
    }
    final ProductAuditReport report = productAuditReport(arguments, inputPath);
    final Map<String, Object> auditPayload = productAuditPayload(report);
    auditPayload.put("command", "product-audit");
    final ProductReportBundleResult bundle =
        Quantum.writeProductReportBundle(
            arguments.outputPath(), report, JSON.writeValueAsString(auditPayload));
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "product-report");
    payload.put("status", report.status().name());
    payload.put("acceptable", report.isAcceptable());
    payload.put("outputDirectory", bundle.outputDirectory().toString());
    payload.put("auditJsonPath", bundle.auditJsonPath().toString());
    payload.put("summaryMarkdownPath", bundle.summaryMarkdownPath().toString());
    payload.put("manifestPath", bundle.manifestPath().toString());
    payload.put("audit", auditPayload);
    printResult(
        arguments,
        out,
        payload,
        "Product report: " + report.status() + ", output=" + bundle.outputDirectory());
    return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int productDistribution(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <directory>.");
    }
    if (!arguments.hasOutputPath()) {
      throw new CliUsageException("Command requires --output <directory>.");
    }
    final Path inputPath = arguments.requiredInputPath();
    if (!Files.isDirectory(inputPath)) {
      throw new CliUsageException("product-distribution requires --input to point to a directory.");
    }
    final Path projectRoot =
        arguments.pathOption("project-root", Path.of(".")).toAbsolutePath().normalize();
    final Path output = arguments.outputPath();
    final Path stagingParent =
        output.toAbsolutePath().normalize().getParent() == null
            ? Path.of(".").toAbsolutePath().normalize()
            : output.toAbsolutePath().normalize().getParent();
    final Path reportDirectory =
        Files.createTempDirectory(stagingParent, "quantum-product-report-");
    final ProductAuditReport report = productAuditReport(arguments, inputPath);
    final Map<String, Object> auditPayload = productAuditPayload(report);
    auditPayload.put("command", "product-audit");
    final ProductDistributionBundleResult bundle;
    try {
      Quantum.writeProductReportBundle(
          reportDirectory, report, JSON.writeValueAsString(auditPayload));
      bundle = Quantum.writeProductDistributionBundle(output, projectRoot, reportDirectory);
    } finally {
      deleteDirectoryIfExists(reportDirectory);
    }
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "product-distribution");
    payload.put("status", report.status().name());
    payload.put("acceptable", report.isAcceptable());
    payload.put("outputDirectory", bundle.outputDirectory().toString());
    payload.put("archivePath", bundle.archivePath().toString());
    payload.put("archiveSha256", bundle.archiveSha256());
    payload.put("manifestPath", bundle.manifestPath().toString());
    payload.put("quickstartPath", bundle.quickstartPath().toString());
    payload.put("librariesDirectory", bundle.librariesDirectory().toString());
    payload.put("toolsDirectory", bundle.toolsDirectory().toString());
    payload.put("examplesDirectory", bundle.examplesDirectory().toString());
    payload.put("reportDirectory", bundle.reportDirectory().toString());
    payload.put("packagedFileCount", bundle.packagedFileCount());
    payload.put("packagedFiles", productDistributionFilesPayload(bundle));
    payload.put("audit", auditPayload);
    printResult(
        arguments,
        out,
        payload,
        "Product distribution: " + report.status() + ", output=" + bundle.outputDirectory());
    return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int productVerifyDistribution(final CliArguments arguments, final PrintStream out)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <distribution-directory>.");
    }
    final ProductDistributionVerificationResult verification =
        Quantum.verifyProductDistributionBundle(arguments.requiredInputPath());
    final Map<String, Object> payload = productDistributionVerificationPayload(verification);
    payload.put("command", "product-verify-distribution");
    printResult(
        arguments,
        out,
        payload,
        verification.isSuccess()
            ? "Product distribution verification: ok"
            : "Product distribution verification: failed");
    return verification.isSuccess() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static int targetProfile(final CliArguments arguments, final PrintStream out)
      throws JsonProcessingException {
    final IntegrationFormat format = arguments.requiredOutputFormat();
    final IntegrationCapabilityProfile profile = Quantum.targetProfile(format);
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("command", "target-profile");
    payload.put("targetName", profile.targetName());
    payload.put("targetVersion", profile.targetVersion());
    payload.put("nativeGates", profile.nativeGateNames());
    payload.put("parameterKinds", enumNames(profile.supportedParameterKinds()));
    payload.put("metadata", profile.metadata());
    printResult(
        arguments,
        out,
        payload,
        "Target profile: " + profile.targetName() + " " + profile.targetVersion());
    return EXIT_SUCCESS;
  }

  private static int doctor(final CliArguments arguments, final PrintStream out)
      throws JsonProcessingException {
    final Path projectRoot =
        arguments.hasInputPath()
            ? arguments.requiredInputPath()
            : Path.of("").toAbsolutePath().normalize();
    final ProductDoctorReport report = Quantum.productDoctor(projectRoot);
    final Map<String, Object> payload = productDoctorPayload(report);
    payload.put("command", "doctor");
    printResult(
        arguments,
        out,
        payload,
        "Product doctor: "
            + report.status()
            + ", failedChecks="
            + report.failedCheckCount()
            + ", warnings="
            + report.warningCheckCount());
    return report.isAcceptable() ? EXIT_SUCCESS : EXIT_WORKFLOW_ERROR;
  }

  private static ProgramLoadResult requireLoadedProgram(final CliArguments arguments)
      throws IOException {
    if (!arguments.hasInputPath()) {
      throw new CliUsageException("Command requires --input <path>.");
    }
    return loadProgram(arguments);
  }

  private static ProgramLoadResult loadProgram(final CliArguments arguments) throws IOException {
    final Path inputPath = arguments.requiredInputPath();
    final String inputFormat =
        arguments.inputFormatName().equals("auto")
            ? detectInputFormat(inputPath)
            : arguments.inputFormatName();
    if (inputFormat.equals("json")) {
      final QuantumIrReadResult read = Quantum.readJson(inputPath);
      if (read.isSuccess()) {
        return ProgramLoadResult.success(read.program());
      }
      return ProgramLoadResult.failure(
          List.of(Map.of("severity", "ERROR", "message", "Native JSON read failed.")));
    }
    final IntegrationFormat format = parseIntegrationFormat(inputFormat);
    final ImportResult imported = Quantum.importProgram(format, Files.readString(inputPath));
    if (imported.isSuccess()) {
      return ProgramLoadResult.success(imported.program());
    }
    return ProgramLoadResult.failure(diagnosticsPayload(imported.diagnostics()));
  }

  private static void deleteDirectoryIfExists(final Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(final Path current, final IOException exception)
              throws IOException {
            if (exception != null) {
              throw exception;
            }
            Files.delete(current);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static List<CorpusRegressionCase> collectCorpusCases(final Path root) throws IOException {
    final ArrayList<CorpusRegressionCase> cases = new ArrayList<>();
    final ArrayList<Path> files = new ArrayList<>();
    collectCorpusFiles(root, files);
    for (int index = 0; index < files.size(); index++) {
      final Path file = files.get(index);
      final String formatName = detectInputFormat(file);
      if (formatName.equals("json")) {
        continue;
      }
      cases.add(
          CorpusRegressionCase.of(
              root.relativize(file).toString(),
              Files.readString(file),
              Quantum.integration(parseIntegrationFormat(formatName))));
    }
    if (cases.isEmpty()) {
      throw new CliUsageException("Corpus directory does not contain supported external files.");
    }
    return cases;
  }

  private static void collectCorpusFiles(final Path root, final ArrayList<Path> files)
      throws IOException {
    try (final java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
      for (Path entry : entries) {
        if (Files.isDirectory(entry)) {
          collectCorpusFiles(entry, files);
        } else if (isCorpusFile(entry)) {
          files.add(entry);
        }
      }
    }
  }

  private static boolean isCorpusFile(final Path path) {
    final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return fileName.endsWith(".qasm") || fileName.endsWith(".quil");
  }

  private static String detectInputFormat(final Path path) throws IOException {
    final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    if (fileName.endsWith(".json")
        || fileName.endsWith(".qir.json")
        || fileName.endsWith(".quantum.json")) {
      return "json";
    }
    if (fileName.endsWith(".quil")) {
      return "quil";
    }
    if (fileName.endsWith(".qasm")) {
      final String source = Files.readString(path);
      if (source.contains("OPENQASM 3")) {
        return "openqasm3";
      }
      return "openqasm2";
    }
    throw new CliUsageException("Cannot detect input format for file: " + path + ".");
  }

  private static IntegrationFormat parseIntegrationFormat(final String value) {
    return switch (value) {
      case "openqasm2", "qasm2", "openqasm_2" -> IntegrationFormat.OPENQASM_2;
      case "openqasm3", "qasm3", "openqasm_3" -> IntegrationFormat.OPENQASM_3;
      case "quil" -> IntegrationFormat.QUIL;
      default -> throw new CliUsageException("Unsupported integration format: " + value + ".");
    };
  }

  private static TransformationOptions transformationOptions(final CliArguments arguments) {
    final TransformationOptions.Builder builder = TransformationOptions.builder();
    if (arguments.flag("remove-identity")) {
      builder.removeIdentityGates();
    }
    if (arguments.flag("inline-composite")) {
      builder.inlineCompositeGates();
    }
    if (arguments.flag("canonicalize-parameters")) {
      builder.canonicalizeParameterExpressions();
    }
    if (arguments.hasOption("target-lowering")) {
      builder.targetAwareLowering(Quantum.targetProfile(arguments.requiredOutputFormat()));
    }
    return builder.build();
  }

  private static Map<String, Object> validationPayload(final ValidationResult validation) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("valid", validation.isValid());
    payload.put("errorCount", validation.errorCount());
    payload.put("errors", validationErrorsPayload(validation.errors()));
    return payload;
  }

  private static List<Map<String, Object>> validationErrorsPayload(
      final List<ValidationError> errors) {
    final ArrayList<Map<String, Object>> payload = new ArrayList<>();
    for (ValidationError error : errors) {
      payload.add(
          Map.of(
              "code",
              error.code().name(),
              "message",
              error.message(),
              "circuitIndex",
              error.circuitIndex(),
              "operationIndex",
              error.operationIndex()));
    }
    return payload;
  }

  private static Map<String, Object> inspectionPayload(final ProgramInspectionResult inspection) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("computationModel", inspection.computationModel().name());
    payload.put("circuitCount", inspection.circuitCount());
    payload.put("qubitCount", inspection.qubitCount());
    payload.put("classicalBitCount", inspection.classicalBitCount());
    payload.put("operationCount", inspection.operationCount());
    payload.put("gateCount", inspection.gateCount());
    payload.put("measurementCount", inspection.measurementCount());
    payload.put("gateHistogram", inspection.gateHistogram());
    payload.put("operationKindHistogram", inspection.operationKindHistogram());
    payload.put("diagnosticCount", inspection.diagnosticCount());
    return payload;
  }

  private static Map<String, Object> resourcePayload(final ResourceEstimate estimate) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("computationModel", estimate.computationModel().name());
    payload.put("circuitCount", estimate.circuitCount());
    payload.put("qubitCount", estimate.qubitCount());
    payload.put("classicalBitCount", estimate.classicalBitCount());
    payload.put("operationCount", estimate.operationCount());
    payload.put("gateCount", estimate.gateCount());
    payload.put("measurementCount", estimate.measurementCount());
    payload.put("estimatedStateVectorAmplitudes", estimate.estimatedStateVectorAmplitudes());
    payload.put("estimatedStateVectorBytes", estimate.estimatedStateVectorBytes());
    payload.put("localSimulationFeasible", estimate.isLocalSimulationFeasible());
    payload.put("localSimulationMaxQubits", estimate.localSimulationMaxQubits());
    payload.put("gateHistogram", estimate.gateHistogram());
    payload.put("circuits", circuitResourcePayloads(estimate.circuits()));
    return payload;
  }

  private static Map<String, Object> timelinePayload(final ProgramTimeline timeline) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("circuits", timelineCircuitPayloads(timeline.circuits()));
    return payload;
  }

  private static Map<String, Object> timelineCircuitPayload(final CircuitTimeline timeline) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", timeline.circuitName());
    payload.put("quantumWires", timeline.quantumWires());
    payload.put("classicalWires", timeline.classicalWires());
    payload.put("steps", timelineStepPayloads(timeline.steps()));
    return payload;
  }

  private static Map<String, Object> timelineStepPayload(final CircuitTimelineStep step) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("operationIndex", step.operationIndex());
    payload.put("operationKind", step.operationKind().name());
    payload.put("label", step.label());
    payload.put("quantumWires", step.quantumWires());
    payload.put("classicalWires", step.classicalWires());
    payload.put("staticPlacement", step.isStaticPlacement());
    return payload;
  }

  private static Map<String, Object> circuitResourcePayload(
      final CircuitResourceEstimate estimate) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", estimate.name());
    payload.put("qubitCount", estimate.qubitCount());
    payload.put("classicalBitCount", estimate.classicalBitCount());
    payload.put("operationCount", estimate.operationCount());
    payload.put("gateCount", estimate.gateCount());
    payload.put("measurementCount", estimate.measurementCount());
    payload.put("twoQubitGateCount", estimate.twoQubitGateCount());
    payload.put("multiQubitGateCount", estimate.multiQubitGateCount());
    payload.put("approximateDepth", estimate.approximateDepth());
    payload.put("depthPrecise", estimate.isDepthPrecise());
    payload.put("neverMeasuredQubits", estimate.neverMeasuredQubits());
    payload.put("overwrittenClassicalBits", estimate.overwrittenClassicalBits());
    return payload;
  }

  private static Map<String, Object> preflightPayload(final CapabilityPreflightResult preflight) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", preflight.status().name());
    payload.put("success", preflight.isSuccess());
    payload.put("requiresLowering", preflight.requiresLowering());
    payload.put("diagnostics", diagnosticsPayload(preflight.diagnostics()));
    return payload;
  }

  private static Map<String, Object> simulationPayload(final SimulationResult simulation) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", simulation.isSuccess());
    payload.put("qubitCount", simulation.qubitCount());
    payload.put("classicalBitCount", simulation.classicalBitCount());
    payload.put("shots", simulation.shots());
    payload.put("counts", simulation.counts());
    payload.put("stateVectorSize", simulation.stateVector().size());
    payload.put("diagnostics", simulationDiagnosticsPayload(simulation.diagnostics()));
    return payload;
  }

  private static Map<String, Object> backendJobRecordPayload(final BackendJobRecord record) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", record.isAccepted());
    payload.put("accepted", record.isAccepted());
    payload.put("trackingId", record.trackingId().value());
    payload.put("backendId", record.backendDescriptor().backendId());
    payload.put("backendName", record.backendDescriptor().displayName());
    payload.put("status", record.status().name());
    payload.put("submittedAt", record.submittedAt().toString());
    payload.put("updatedAt", record.updatedAt().toString());
    payload.put("queueMetadata", record.queueMetadata());
    payload.put("costMetadata", record.costMetadata());
    payload.put("providerMetadata", record.providerMetadata());
    payload.put("diagnostics", backendDiagnosticsPayload(record.diagnostics()));
    if (record.hasBackendJobId()) {
      payload.put("backendJobId", record.backendJobId().value());
    }
    if (record.hasExecutionResult()) {
      payload.put(
          "compilerSuccess",
          record.executionResult().hasCompilerResult()
              && record.executionResult().compilerResult().isSuccess());
      payload.put(
          "simulationSuccess",
          record.executionResult().hasSimulationResult()
              && record.executionResult().simulationResult().isSuccess());
      payload.put(
          "counts",
          record.executionResult().hasSimulationResult()
              ? record.executionResult().simulationResult().counts()
              : Map.of());
    }
    return payload;
  }

  private static Map<String, Object> workflowPayload(final ProductWorkflowReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("targetFormat", report.targetFormat().name());
    payload.put("status", report.status().name());
    payload.put("success", report.isSuccess());
    payload.put("validation", validationPayload(report.validation()));
    payload.put("inspection", inspectionPayload(report.inspection()));
    if (report.preflight() != null) {
      payload.put("preflight", preflightPayload(report.preflight()));
    }
    payload.put("resources", resourcePayload(report.resources()));
    payload.put("circuit", timelinePayload(report.timeline()));
    if (report.simulation() != null) {
      payload.put("simulation", simulationPayload(report.simulation()));
    }
    if (report.compiler() != null) {
      payload.put("compile", compilerPayload(report.compiler()));
    }
    if (report.hasBackendSubmission()) {
      final Map<String, Object> backend = new LinkedHashMap<>();
      backend.put("accepted", report.backendSubmission().isAccepted());
      backend.put("status", report.backendSubmission().status().name());
      if (report.hasBackendExecution()) {
        backend.put("resultStatus", report.backendExecution().status().name());
        backend.put(
            "compilerSuccess",
            report.backendExecution().hasCompilerResult()
                && report.backendExecution().compilerResult().isSuccess());
        backend.put(
            "simulationSuccess",
            report.backendExecution().hasSimulationResult()
                && report.backendExecution().simulationResult().isSuccess());
      }
      payload.put("backendDryRun", backend);
    }
    payload.put(
        "targetProfile",
        Map.of(
            "targetName",
            report.targetProfile().targetName(),
            "targetVersion",
            report.targetProfile().targetVersion(),
            "nativeGates",
            report.targetProfile().nativeGateNames()));
    return payload;
  }

  private static Map<String, Object> benchmarkPayload(final ProductBenchmarkReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", report.isSuccess());
    payload.put("targetFormat", report.targetFormat().name());
    if (report.hasInputFormat()) {
      payload.put("inputFormat", report.inputFormat().name());
    }
    payload.put("stageCount", report.stageCount());
    payload.put("totalAverageNanos", report.totalAverageNanos());
    payload.put("stages", benchmarkStagePayloads(report.stages()));
    return payload;
  }

  private static Map<String, Object> compatibilityPayload(final ProductCompatibilityMatrix matrix) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", matrix.isSuccess());
    payload.put("validation", validationPayload(matrix.validation()));
    payload.put("inspection", inspectionPayload(matrix.inspection()));
    payload.put("resources", resourcePayload(matrix.resources()));
    payload.put("simulation", simulationPayload(matrix.simulation()));
    payload.put("targets", targetCompatibilityPayloads(matrix.targets()));
    return payload;
  }

  private static Map<String, Object> targetCompatibilityPayload(
      final TargetCompatibilityReport target) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("targetFormat", target.targetFormat().name());
    payload.put("targetName", target.targetProfile().targetName());
    payload.put("targetVersion", target.targetProfile().targetVersion());
    payload.put("status", target.status().name());
    payload.put("success", target.isSuccess());
    payload.put(
        "preflightStatus",
        target.preflightStatus() == null ? null : target.preflightStatus().name());
    payload.put(
        "compilerStatus", target.compilerStatus() == null ? null : target.compilerStatus().name());
    payload.put(
        "workflowStatus", target.workflowStatus() == null ? null : target.workflowStatus().name());
    payload.put("nativeGateCount", target.targetProfile().nativeGateNames().size());
    payload.put("capabilities", enumNames(target.targetProfile().capabilities()));
    payload.put("checks", compatibilityCheckPayloads(target.checks()));
    return payload;
  }

  private static Map<String, Object> compatibilityCheckPayload(
      final CompatibilityCheckResult check) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", check.name());
    payload.put("status", check.status().name());
    payload.put("success", check.isSuccess());
    payload.put("message", check.message());
    payload.put("diagnosticCount", check.diagnosticCount());
    payload.put("elapsedNanos", check.elapsedNanos());
    return payload;
  }

  private static Map<String, Object> crossFormatPayload(
      final CrossFormatVerificationReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", report.isSuccess());
    payload.put("inputFormat", report.inputFormat().name());
    payload.put("importSuccess", report.importSuccess());
    payload.put("importDiagnosticCount", report.importDiagnosticCount());
    if (report.validation() != null) {
      payload.put("validation", validationPayload(report.validation()));
    }
    if (report.simulation() != null) {
      payload.put("simulation", simulationPayload(report.simulation()));
    }
    payload.put("targets", crossFormatTargetPayloads(report.targets()));
    return payload;
  }

  private static Map<String, Object> crossFormatTargetPayload(
      final CrossFormatTargetVerification target) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("targetFormat", target.targetFormat().name());
    payload.put("success", target.isSuccess());
    payload.put("exportSuccess", target.exportSuccess());
    payload.put("reimportSuccess", target.reimportSuccess());
    payload.put("validationSuccess", target.validationSuccess());
    payload.put("simulationSuccess", target.simulationSuccess());
    payload.put("simulationEquivalent", target.simulationEquivalent());
    payload.put("exportDiagnosticCount", target.exportDiagnosticCount());
    payload.put("reimportDiagnosticCount", target.reimportDiagnosticCount());
    payload.put("validationErrorCount", target.validationErrorCount());
    return payload;
  }

  private static Map<String, Object> corpusRegressionPayload(final CorpusRegressionReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", report.isSuccess());
    payload.put("caseCount", report.caseCount());
    payload.put("failureCount", report.failureCount());
    payload.put("cases", corpusRegressionCasePayloads(report.cases()));
    return payload;
  }

  private static Map<String, Object> corpusRegressionCasePayload(
      final CorpusRegressionCaseReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", report.name());
    payload.put("inputFormat", report.inputFormat().name());
    payload.put("success", report.isSuccess());
    payload.put("importSuccess", report.importSuccess());
    payload.put("importDiagnosticCount", report.importDiagnosticCount());
    if (report.validation() != null) {
      payload.put("validation", validationPayload(report.validation()));
    }
    if (report.compatibilityMatrix() != null) {
      payload.put("compatibility", compatibilityPayload(report.compatibilityMatrix()));
    }
    if (report.crossFormatVerification() != null) {
      payload.put("crossFormat", crossFormatPayload(report.crossFormatVerification()));
    }
    return payload;
  }

  private static Map<String, Object> releaseReadinessPayload(final ReleaseReadinessReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", report.status().name());
    payload.put("ready", report.isReady());
    payload.put("acceptable", report.isAcceptable());
    payload.put("failedCheckCount", report.failedCheckCount());
    payload.put("warningCheckCount", report.warningCheckCount());
    payload.put("checks", releaseReadinessCheckPayloads(report.checks()));
    payload.put("targetProfiles", targetProfilePayloads(report.targetProfiles()));
    payload.put("corpusRegression", corpusRegressionPayload(report.corpusRegression()));
    if (report.hasBenchmark()) {
      payload.put("benchmark", benchmarkPayload(report.benchmark()));
    }
    return payload;
  }

  private static Map<String, Object> releaseReadinessCheckPayload(
      final ReleaseReadinessCheck check) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", check.name());
    payload.put("status", check.status().name());
    payload.put("message", check.message());
    return payload;
  }

  private static Map<String, Object> productDoctorPayload(final ProductDoctorReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("projectRoot", report.projectRoot().toString());
    payload.put("status", report.status().name());
    payload.put("success", report.isSuccess());
    payload.put("healthy", report.isHealthy());
    payload.put("acceptable", report.isAcceptable());
    payload.put("checkCount", report.checkCount());
    payload.put("failedCheckCount", report.failedCheckCount());
    payload.put("warningCheckCount", report.warningCheckCount());
    payload.put("checks", productDoctorCheckPayloads(report.checks()));
    return payload;
  }

  private static List<String> enumNames(final Iterable<? extends Enum<?>> values) {
    final ArrayList<String> names = new ArrayList<>();
    for (final Enum<?> value : values) {
      names.add(value.name());
    }
    return names;
  }

  private static List<Map<String, Object>> circuitResourcePayloads(
      final List<CircuitResourceEstimate> estimates) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(estimates.size());
    for (int i = 0; i < estimates.size(); i++) {
      payloads.add(circuitResourcePayload(estimates.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> timelineCircuitPayloads(
      final List<CircuitTimeline> timelines) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(timelines.size());
    for (int i = 0; i < timelines.size(); i++) {
      payloads.add(timelineCircuitPayload(timelines.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> timelineStepPayloads(
      final List<CircuitTimelineStep> steps) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(steps.size());
    for (int i = 0; i < steps.size(); i++) {
      payloads.add(timelineStepPayload(steps.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> benchmarkStagePayloads(
      final List<BenchmarkStageResult> stages) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(stages.size());
    for (int i = 0; i < stages.size(); i++) {
      payloads.add(benchmarkStagePayload(stages.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> targetCompatibilityPayloads(
      final List<TargetCompatibilityReport> targets) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(targets.size());
    for (int i = 0; i < targets.size(); i++) {
      payloads.add(targetCompatibilityPayload(targets.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> compatibilityCheckPayloads(
      final List<CompatibilityCheckResult> checks) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(checks.size());
    for (int i = 0; i < checks.size(); i++) {
      payloads.add(compatibilityCheckPayload(checks.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> crossFormatTargetPayloads(
      final List<CrossFormatTargetVerification> targets) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(targets.size());
    for (int i = 0; i < targets.size(); i++) {
      payloads.add(crossFormatTargetPayload(targets.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> corpusRegressionCasePayloads(
      final List<CorpusRegressionCaseReport> cases) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(cases.size());
    for (int i = 0; i < cases.size(); i++) {
      payloads.add(corpusRegressionCasePayload(cases.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> releaseReadinessCheckPayloads(
      final List<ReleaseReadinessCheck> checks) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(checks.size());
    for (int i = 0; i < checks.size(); i++) {
      payloads.add(releaseReadinessCheckPayload(checks.get(i)));
    }
    return payloads;
  }

  private static List<Map<String, Object>> targetProfilePayloads(
      final List<IntegrationCapabilityProfile> profiles) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(profiles.size());
    for (int i = 0; i < profiles.size(); i++) {
      final IntegrationCapabilityProfile profile = profiles.get(i);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("format", profile.format().name());
      payload.put("targetName", profile.targetName());
      payload.put("targetVersion", profile.targetVersion());
      payload.put("capabilityCount", profile.capabilities().size());
      payload.put("nativeGateCount", profile.nativeGateNames().size());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> productDoctorCheckPayloads(
      final List<ProductDoctorCheck> checks) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(checks.size());
    for (int i = 0; i < checks.size(); i++) {
      payloads.add(productDoctorCheckPayload(checks.get(i)));
    }
    return payloads;
  }

  private static Map<String, Object> productAuditPayload(final ProductAuditReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("projectRoot", report.projectRoot().toString());
    payload.put("status", report.status().name());
    payload.put("ready", report.isReady());
    payload.put("acceptable", report.isAcceptable());
    payload.put("failedCheckCount", report.failedCheckCount());
    payload.put("warningCheckCount", report.warningCheckCount());
    payload.put("doctor", productDoctorPayload(report.doctor()));
    payload.put("readiness", releaseReadinessPayload(report.readiness()));
    return payload;
  }

  private static List<String> productDistributionFilesPayload(
      final ProductDistributionBundleResult bundle) {
    final ArrayList<String> files = new ArrayList<>(bundle.packagedFiles().size());
    for (int index = 0; index < bundle.packagedFiles().size(); index++) {
      files.add(
          bundle
              .outputDirectory()
              .relativize(bundle.packagedFiles().get(index))
              .toString()
              .replace('\\', '/'));
    }
    return files;
  }

  private static Map<String, Object> productDistributionVerificationPayload(
      final ProductDistributionVerificationResult verification) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("success", verification.isSuccess());
    payload.put("distributionDirectory", verification.distributionDirectory().toString());
    payload.put("archivePath", verification.archivePath().toString());
    payload.put("archivePresent", verification.archivePresent());
    payload.put("verifiedFileCount", verification.verifiedFileCount());
    payload.put("issueCount", verification.issueCount());
    final ArrayList<Map<String, Object>> issues = new ArrayList<>(verification.issues().size());
    for (int index = 0; index < verification.issues().size(); index++) {
      issues.add(productDistributionVerificationIssuePayload(verification.issues().get(index)));
    }
    payload.put("issues", issues);
    return payload;
  }

  private static Map<String, Object> productDistributionVerificationIssuePayload(
      final ProductDistributionVerificationIssue issue) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("code", issue.code());
    payload.put("path", issue.path());
    payload.put("message", issue.message());
    return payload;
  }

  private static Map<String, Object> productDoctorCheckPayload(final ProductDoctorCheck check) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", check.name());
    payload.put("status", check.status().name());
    payload.put("message", check.message());
    return payload;
  }

  private static Molecule chemistryMoleculeFromEntry(final String entryId) {
    final ChemistryLibraryEntry entry = QuantumChemistry.requireLibraryEntry(entryId);
    final ChemistryStorageDocument document = entry.document();
    if (document.molecules().isEmpty()) {
      throw new CliUsageException(
          "Chemistry library entry does not contain molecules: " + entryId + ".");
    }
    return document.molecules().get(0);
  }

  private static List<ReactionProductCandidate> chemistrySeedCandidates(
      final CliArguments arguments) {
    final List<String> entries = arguments.stringOptions("candidate-entry");
    final boolean hasCandidateSide = arguments.hasOption("candidate-side-entry");
    if (entries.isEmpty() && !hasCandidateSide) {
      return List.of();
    }
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>(entries.size() + (hasCandidateSide ? 1 : 0));
    if (hasCandidateSide) {
      candidates.add(chemistrySeedCandidate(chemistryProductSide(arguments)));
    }
    for (int index = 0; index < entries.size(); index++) {
      candidates.add(chemistrySeedCandidate(entries.get(index)));
    }
    return List.copyOf(candidates);
  }

  private static ReactionProductCandidate chemistrySeedCandidate(final ReactionSide productSide) {
    return ReactionProductCandidate.of(
        productSide,
        0.5,
        50.0,
        "Seed product candidate supplied through CLI.",
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE,
                "Candidate product side was supplied explicitly through --candidate-side-entry.",
                0.1)));
  }

  private static ReactionProductCandidate chemistrySeedCandidate(final String entryId) {
    final Molecule molecule = chemistryMoleculeFromEntry(entryId);
    return ReactionProductCandidate.of(
        ReactionSide.of(List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))),
        0.5,
        50.0,
        "Seed product candidate supplied through CLI.",
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE,
                "Candidate molecule was supplied explicitly through --candidate-entry.",
                0.1)));
  }

  private static ReactionSide chemistryProductSide(final CliArguments arguments) {
    final List<String> entries = arguments.stringOptions("candidate-side-entry");
    if (entries.isEmpty()) {
      throw new CliUsageException(
          "Chemistry --candidate-side-entry must contain at least one library entry.");
    }
    final List<String> coefficients = arguments.stringOptions("candidate-side-coefficient");
    if (!coefficients.isEmpty() && coefficients.size() != entries.size()) {
      throw new CliUsageException(
          "Chemistry --candidate-side-coefficient must be omitted or repeated exactly once per --candidate-side-entry.");
    }
    return chemistrySideFromEntries(entries, coefficients, "candidate-side-coefficient");
  }

  private static ReactionSide chemistryReactants(final CliArguments arguments) {
    final List<String> entries = arguments.stringOptions("reactant-entry");
    if (entries.isEmpty()) {
      return ReactionSide.of(
          List.of(
              ReactionParticipant.of(
                  chemistryMoleculeFromEntry(arguments.requiredStringOption("reactant-entry")),
                  StoichiometricCoefficient.of(arguments.intOption("reactant-coefficient", 1)))));
    }
    final List<String> coefficients = arguments.stringOptions("reactant-coefficient");
    if (!coefficients.isEmpty() && coefficients.size() != entries.size()) {
      throw new CliUsageException(
          "Chemistry --reactant-coefficient must be omitted or repeated exactly once per --reactant-entry.");
    }
    final LinkedHashMap<String, ReactantAccumulator> accumulators =
        new LinkedHashMap<String, ReactantAccumulator>(entries.size());
    for (int index = 0; index < entries.size(); index++) {
      final Molecule molecule = chemistryMoleculeFromEntry(entries.get(index));
      final int coefficient = chemistryReactantCoefficient(coefficients, index);
      final String moleculeId = molecule.id().value();
      final ReactantAccumulator existing = accumulators.get(moleculeId);
      if (existing == null) {
        accumulators.put(moleculeId, new ReactantAccumulator(molecule, coefficient));
        continue;
      }
      existing.add(coefficient);
    }
    return chemistrySideFromAccumulators(accumulators);
  }

  private static int chemistryReactantCoefficient(
      final List<String> coefficients, final int index) {
    if (coefficients.isEmpty()) {
      return 1;
    }
    try {
      return Integer.parseInt(coefficients.get(index).trim());
    } catch (final NumberFormatException exception) {
      throw new CliUsageException("Chemistry --reactant-coefficient must be an integer.");
    }
  }

  private static ReactionSide chemistrySideFromEntries(
      final List<String> entries, final List<String> coefficients, final String coefficientOption) {
    final LinkedHashMap<String, ReactantAccumulator> accumulators =
        new LinkedHashMap<String, ReactantAccumulator>(entries.size());
    for (int index = 0; index < entries.size(); index++) {
      final Molecule molecule = chemistryMoleculeFromEntry(entries.get(index));
      final int coefficient = chemistryCoefficient(coefficients, index, coefficientOption);
      final String moleculeId = molecule.id().value();
      final ReactantAccumulator existing = accumulators.get(moleculeId);
      if (existing == null) {
        accumulators.put(moleculeId, new ReactantAccumulator(molecule, coefficient));
        continue;
      }
      existing.add(coefficient);
    }
    return chemistrySideFromAccumulators(accumulators);
  }

  private static ReactionSide chemistrySideFromAccumulators(
      final LinkedHashMap<String, ReactantAccumulator> accumulators) {
    final ArrayList<ReactionParticipant> participants =
        new ArrayList<ReactionParticipant>(accumulators.size());
    for (final ReactantAccumulator accumulator : accumulators.values()) {
      participants.add(
          ReactionParticipant.of(
              accumulator.molecule, StoichiometricCoefficient.of(accumulator.coefficient)));
    }
    return ReactionSide.of(participants);
  }

  private static int chemistryCoefficient(
      final List<String> coefficients, final int index, final String optionName) {
    if (coefficients.isEmpty()) {
      return 1;
    }
    try {
      return Integer.parseInt(coefficients.get(index).trim());
    } catch (final NumberFormatException exception) {
      throw new CliUsageException("Chemistry --" + optionName + " must be an integer.");
    }
  }

  private static final class ReactantAccumulator {

    private final Molecule molecule;
    private int coefficient;

    private ReactantAccumulator(final Molecule molecule, final int coefficient) {
      this.molecule = molecule;
      this.coefficient = coefficient;
    }

    private void add(final int value) {
      try {
        this.coefficient = Math.addExact(this.coefficient, value);
      } catch (final ArithmeticException exception) {
        throw new CliUsageException("Chemistry reactant coefficient sum is too large.");
      }
    }
  }

  private static ReactionConditions chemistryReactionConditions(final CliArguments arguments) {
    final ArrayList<ReactionConditionComponent> components = new ArrayList<>();
    final ArrayList<ReactionConditionFeature> features = new ArrayList<>();
    final Temperature temperature =
        arguments.hasOption("temperature-celsius")
            ? Temperature.of(
                arguments.doubleOption("temperature-celsius", 25.0), TemperatureUnit.CELSIUS)
            : null;
    final TimeDuration duration =
        arguments.hasOption("duration-hours")
            ? TimeDuration.of(arguments.doubleOption("duration-hours", 1.0), TimeDurationUnit.HOUR)
            : null;
    final Pressure pressure =
        arguments.hasOption("pressure-atm")
            ? Pressure.of(arguments.doubleOption("pressure-atm", 1.0), PressureUnit.ATMOSPHERE)
            : null;
    final String solvent = arguments.stringOption("solvent", null);
    if (solvent != null) {
      components.add(ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, solvent));
    }
    final String catalyst = arguments.stringOption("catalyst", null);
    if (catalyst != null) {
      components.add(ReactionConditionComponent.named(ReactionComponentRole.CATALYST, catalyst));
      features.add(ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT));
    }
    final List<String> componentValues = arguments.stringOptions("condition-component");
    for (int index = 0; index < componentValues.size(); index++) {
      final ReactionConditionComponent component =
          chemistryConditionComponent(componentValues.get(index));
      components.add(component);
      if (component.role() == ReactionComponentRole.CATALYST) {
        features.add(ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT));
      }
    }
    if (temperature != null && temperature.kelvin() > 323.15) {
      features.add(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING));
    }
    return ReactionConditions.of(
        temperature,
        pressure,
        duration,
        components,
        features,
        arguments.stringOption("condition-note", null));
  }

  private static ReactionConditionComponent chemistryConditionComponent(final String rawValue) {
    final String[] parts = rawValue.split(":", 6);
    if (parts.length < 2) {
      throw new CliUsageException(
          "Chemistry --condition-component must use role:name[:amount:unit:phase:note].");
    }
    final ReactionComponentRole role = chemistryComponentRole(parts[0]);
    final String name = requiredInlineValue(parts[1], "condition component name");
    final SubstanceAmount amount =
        parts.length >= 4 && !parts[2].trim().isEmpty()
            ? SubstanceAmount.of(
                parseFiniteDouble(parts[2], "condition component amount"),
                chemistrySubstanceAmountUnit(parts[3]))
            : null;
    if (parts.length == 3 && !parts[2].trim().isEmpty()) {
      throw new CliUsageException("Chemistry --condition-component amount requires a unit.");
    }
    final ReactionPhase phase =
        parts.length >= 5 && !parts[4].trim().isEmpty()
            ? chemistryReactionPhase(parts[4])
            : ReactionPhase.UNKNOWN;
    final String note = parts.length >= 6 && !parts[5].trim().isEmpty() ? parts[5].trim() : null;
    return ReactionConditionComponent.of(role, name, amount, null, phase, note);
  }

  private static ReactionComponentRole chemistryComponentRole(final String rawValue) {
    final String normalized = normalizedEnumValue(rawValue);
    try {
      return ReactionComponentRole.valueOf(normalized);
    } catch (final IllegalArgumentException exception) {
      throw new CliUsageException("Unknown chemistry condition component role: " + rawValue + ".");
    }
  }

  private static SubstanceAmountUnit chemistrySubstanceAmountUnit(final String rawValue) {
    final String normalized = normalizedEnumValue(rawValue);
    final String canonical =
        switch (normalized) {
          case "MOL", "MOLES" -> "MOLE";
          case "MMOL", "MILLIMOLES" -> "MILLIMOLE";
          case "UMOL", "MICROMOLES" -> "MICROMOLE";
          case "KG", "KILOGRAMS" -> "KILOGRAM";
          case "G", "GRAMS" -> "GRAM";
          case "MG", "MILLIGRAMS" -> "MILLIGRAM";
          case "UG", "MICROGRAMS" -> "MICROGRAM";
          default -> normalized;
        };
    try {
      return SubstanceAmountUnit.valueOf(canonical);
    } catch (final IllegalArgumentException exception) {
      throw new CliUsageException(
          "Unknown chemistry condition component amount unit: " + rawValue + ".");
    }
  }

  private static ReactionPhase chemistryReactionPhase(final String rawValue) {
    final String normalized = normalizedEnumValue(rawValue);
    try {
      return ReactionPhase.valueOf(normalized);
    } catch (final IllegalArgumentException exception) {
      throw new CliUsageException("Unknown chemistry condition component phase: " + rawValue + ".");
    }
  }

  private static String normalizedEnumValue(final String rawValue) {
    return requiredInlineValue(rawValue, "enum value").replace('-', '_').toUpperCase(Locale.ROOT);
  }

  private static String requiredInlineValue(final String rawValue, final String subjectName) {
    if (rawValue == null || rawValue.trim().isEmpty()) {
      throw new CliUsageException("Chemistry " + subjectName + " must not be blank.");
    }
    return rawValue.trim();
  }

  private static double parseFiniteDouble(final String rawValue, final String subjectName) {
    try {
      final double value = Double.parseDouble(rawValue.trim());
      if (Double.isFinite(value)) {
        return value;
      }
    } catch (final NumberFormatException exception) {
      throw new CliUsageException("Chemistry " + subjectName + " must be a finite decimal number.");
    }
    throw new CliUsageException("Chemistry " + subjectName + " must be a finite decimal number.");
  }

  private static ElectronicStructureMethod chemistryElectronicStructureMethod(final String value) {
    final String normalized = value.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    if (normalized.equals("hartree-fock") || normalized.equals("hf") || normalized.equals("rhf")) {
      return ElectronicStructureMethod.HARTREE_FOCK;
    }
    if (normalized.startsWith("dft:")) {
      return ElectronicStructureMethod.densityFunctional(value.substring("dft:".length()));
    }
    if (normalized.startsWith("dft-")) {
      return ElectronicStructureMethod.densityFunctional(value.substring("dft-".length()));
    }
    return ElectronicStructureMethod.densityFunctional(value);
  }

  private static BasisSet chemistryBasisSet(final String value) {
    final String normalized = value.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    if (normalized.equals("sto-3g")) {
      return BasisSet.STO_3G;
    }
    return BasisSet.of(BasisSetName.of(value));
  }

  private static ActiveSpace chemistryActiveSpace(
      final CliArguments arguments, final List<ClassicalCalculationKind> calculationKinds) {
    if (!arguments.hasOption("active-electrons") && !arguments.hasOption("active-orbitals")) {
      if (chemistryRequiresActiveSpace(calculationKinds)) {
        throw new CliUsageException(
            "Chemistry workflow with quantum-preparation calculation requires "
                + "--active-electrons and --active-orbitals.");
      }
      return null;
    }
    if (!arguments.hasOption("active-electrons") || !arguments.hasOption("active-orbitals")) {
      throw new CliUsageException(
          "Chemistry active space requires both --active-electrons and --active-orbitals.");
    }
    return ActiveSpace.of(
        arguments.intOption("active-electrons", 0), arguments.intOption("active-orbitals", 0));
  }

  private static boolean chemistryRequiresActiveSpace(
      final List<ClassicalCalculationKind> calculationKinds) {
    for (int index = 0; index < calculationKinds.size(); index++) {
      final ClassicalCalculationKind kind = calculationKinds.get(index);
      if (kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
          || kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
        return true;
      }
    }
    return false;
  }

  private static List<ClassicalCalculationKind> chemistryCalculationKinds(final String value) {
    final String[] parts = value.split(",");
    final ArrayList<ClassicalCalculationKind> kinds = new ArrayList<>(parts.length);
    for (int index = 0; index < parts.length; index++) {
      final String part = parts[index].trim();
      if (part.isEmpty()) {
        continue;
      }
      kinds.add(chemistryEnumOption(ClassicalCalculationKind.class, part, "calculation-kinds"));
    }
    if (kinds.isEmpty()) {
      throw new CliUsageException(
          "Chemistry workflow requires at least one --calculation-kinds value.");
    }
    return List.copyOf(kinds);
  }

  private static ClassicalCalculationRequest chemistryPredictionCalculationRequest(
      final CliArguments arguments,
      final ReactionSide reactants,
      final ReactionConditions conditions,
      final ReactionSide calculationProducts,
      final List<ClassicalCalculationOption> calculationOptions) {
    if (calculationProducts == null || calculationProducts.participants().isEmpty()) {
      throw new CliUsageException(
          "Chemistry prediction calculation request requires product candidates.");
    }
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(
            arguments.stringOption("calculation-id", "cli_chemistry_prediction_calculation")),
        Reaction.of(
            ReactionId.of(arguments.stringOption("reaction-id", "cli_prediction_reaction")),
            "CLI prediction reaction",
            reactants,
            calculationProducts,
            conditions,
            ChemistryMetadata.EMPTY),
        chemistryElectronicStructureMethod(arguments.stringOption("method", "hartree-fock")),
        chemistryBasisSet(arguments.stringOption("basis", "STO-3G")),
        null,
        List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
        calculationOptions,
        ChemistryMetadata.EMPTY);
  }

  private static List<ClassicalCalculationOption> chemistryCalculationOptions(
      final CliArguments arguments) {
    final List<String> values = arguments.stringOptions("calculation-option");
    final boolean electronicRerank = arguments.flag("electronic-rerank");
    final boolean quantumPreparation = arguments.flag("quantum-preparation");
    final boolean quantumWorkflowVqe = arguments.flag("quantum-workflow-vqe");
    final boolean hasNamedOptions =
        electronicRerank
            || quantumPreparation
            || quantumWorkflowVqe
            || arguments.hasOption("electronic-rerank-max-candidates")
            || arguments.hasOption("electronic-rerank-max-atoms")
            || arguments.hasOption("quantum-preparation-max-candidates")
            || arguments.hasOption("quantum-preparation-max-participants");
    if (values.isEmpty() && !hasNamedOptions) {
      return List.of();
    }
    final ArrayList<ClassicalCalculationOption> options =
        new ArrayList<ClassicalCalculationOption>(values.size() + 8);
    for (int index = 0; index < values.size(); index++) {
      options.add(chemistryCalculationOption(values.get(index)));
    }
    if (electronicRerank) {
      options.add(ClassicalCalculationOption.bool("reaction_prediction_electronic_rerank", true));
    }
    if (arguments.hasOption("electronic-rerank-max-candidates")) {
      options.add(
          ClassicalCalculationOption.integer(
              "reaction_prediction_electronic_rerank_max_candidates",
              positiveIntOption(arguments, "electronic-rerank-max-candidates")));
    }
    if (arguments.hasOption("electronic-rerank-max-atoms")) {
      options.add(
          ClassicalCalculationOption.integer(
              "reaction_prediction_electronic_rerank_max_atoms",
              positiveIntOption(arguments, "electronic-rerank-max-atoms")));
    }
    if (quantumPreparation) {
      options.add(
          ClassicalCalculationOption.bool("reaction_prediction_prepare_quantum_workflows", true));
    }
    if (quantumWorkflowVqe) {
      options.add(
          ClassicalCalculationOption.bool(
              "reaction_prediction_execute_quantum_workflow_vqe", true));
      options.add(
          ClassicalCalculationOption.bool(
              "advanced_prediction_execute_quantum_workflow_vqe", true));
    }
    if (arguments.hasOption("quantum-preparation-max-candidates")) {
      options.add(
          ClassicalCalculationOption.integer(
              "reaction_prediction_prepare_quantum_workflows_max_candidates",
              positiveIntOption(arguments, "quantum-preparation-max-candidates")));
    }
    if (arguments.hasOption("quantum-preparation-max-participants")) {
      options.add(
          ClassicalCalculationOption.integer(
              "reaction_prediction_prepare_quantum_workflows_max_participants",
              positiveIntOption(arguments, "quantum-preparation-max-participants")));
    }
    return List.copyOf(options);
  }

  private static int positiveIntOption(final CliArguments arguments, final String name) {
    final int value = arguments.intOption(name, 1);
    if (value < 1) {
      throw new CliUsageException("--" + name + " must be positive.");
    }
    return value;
  }

  private static ClassicalCalculationOption chemistryCalculationOption(final String rawValue) {
    final int separator = rawValue.indexOf('=');
    if (separator <= 0 || separator == rawValue.length() - 1) {
      throw new CliUsageException("Chemistry --calculation-option must use name=value syntax.");
    }
    final String name = rawValue.substring(0, separator).trim();
    final String value = rawValue.substring(separator + 1).trim();
    if (name.isEmpty() || value.isEmpty()) {
      throw new CliUsageException(
          "Chemistry --calculation-option must contain non-empty name and value.");
    }
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
      return ClassicalCalculationOption.bool(name, Boolean.parseBoolean(value));
    }
    try {
      return ClassicalCalculationOption.integer(name, Integer.parseInt(value));
    } catch (final NumberFormatException ignored) {
      // Значение не помещается в int или не является int; пробуем более широкие числовые формы.
    }
    try {
      return ClassicalCalculationOption.longValue(name, Long.parseLong(value));
    } catch (final NumberFormatException ignored) {
      // Значение не является long; пробуем double.
    }
    try {
      final double doubleValue = Double.parseDouble(value);
      if (Double.isFinite(doubleValue)) {
        return ClassicalCalculationOption.floating(name, doubleValue);
      }
    } catch (final NumberFormatException ignored) {
      // Значение является текстом.
    }
    return ClassicalCalculationOption.text(name, value);
  }

  private static <T extends Enum<T>> T chemistryEnumOption(
      final Class<T> type, final String value, final String optionName) {
    if (value == null) {
      return null;
    }
    final String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    try {
      return Enum.valueOf(type, normalized);
    } catch (final IllegalArgumentException exception) {
      throw new CliUsageException(
          "Unsupported chemistry --" + optionName + " value: " + value + ".");
    }
  }

  private static int chemistryCandidateCount(final OrganicReactionPredictionWorkflowResult result) {
    if (!result.hasPredictionResult()) {
      return 0;
    }
    return result.predictionResult().candidates().size();
  }

  private static Map<String, Object> chemistryStorageDocumentPayload(
      final ChemistryStorageDocument document) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("projectId", document.projectId());
    payload.put("displayName", document.displayName());
    payload.put("version", document.version().number());
    payload.put("counts", chemistryStorageCountsPayload(document));
    payload.put("molecules", chemistryStorageMoleculePayloads(document.molecules()));
    payload.put("reactions", chemistryStorageReactionPayloads(document.reactions()));
    payload.put(
        "reactionRecords", chemistryStorageReactionRecordPayloads(document.reactionRecords()));
    payload.put(
        "predictionRecords",
        chemistryStoragePredictionRecordPayloads(document.predictionRecords()));
    payload.put(
        "computationPlanRecords",
        chemistryStorageComputationPlanRecordPayloads(document.computationPlanRecords()));
    payload.put(
        "classicalResultRecords",
        chemistryStorageClassicalResultRecordPayloads(document.classicalResultRecords()));
    payload.put("roadmapRecords", chemistryStorageRoadmapRecordPayloads(document.roadmapRecords()));
    payload.put(
        "historyEvents", chemistryStorageHistoryEventPayloads(document.reactionHistoryEvents()));
    return payload;
  }

  private static Map<String, Object> chemistryStorageCountsPayload(
      final ChemistryStorageDocument document) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("molecules", document.molecules().size());
    payload.put("reactions", document.reactions().size());
    payload.put("mechanisms", document.mechanisms().size());
    payload.put("reactionRecords", document.reactionRecords().size());
    payload.put("conditionSnapshots", document.conditionSnapshotRecords().size());
    payload.put("outcomes", document.reactionOutcomeRecords().size());
    payload.put("predictions", document.predictionRecords().size());
    payload.put("computationPlans", document.computationPlanRecords().size());
    payload.put("classicalResults", document.classicalResultRecords().size());
    payload.put("roadmaps", document.roadmapRecords().size());
    payload.put("historyEvents", document.reactionHistoryEvents().size());
    payload.put("extensions", document.extensions().size());
    return payload;
  }

  private static List<Map<String, Object>> chemistryStorageMoleculePayloads(
      final List<Molecule> molecules) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(molecules.size());
    for (final Molecule molecule : molecules) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", molecule.id().value());
      payload.put("displayName", molecule.displayName());
      payload.put("atoms", molecule.atoms().size());
      payload.put("bonds", molecule.bonds().size());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageReactionPayloads(
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction> reactions) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(reactions.size());
    for (final ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction reaction :
        reactions) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", reaction.id().value());
      payload.put("displayName", reaction.displayName());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageReactionRecordPayloads(
      final List<ChemistryStorageReactionRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageReactionRecord record : records) {
      payloads.add(chemistryStorageReactionRecordPayload(record));
    }
    return payloads;
  }

  private static Map<String, Object> chemistryStorageReactionRecordPayload(
      final ChemistryStorageReactionRecord record) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", record.id());
    payload.put("reactionId", record.reaction().id().value());
    payload.put("status", record.status().name());
    payload.put("historyIndex", record.historyIndex());
    payload.put("observedYieldPercent", record.observedYieldPercent());
    payload.put("note", record.note());
    return payload;
  }

  private static List<Map<String, Object>> chemistryStoragePredictionRecordPayloads(
      final List<ChemistryStoragePredictionRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStoragePredictionRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("requestId", record.requestId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("methodType", record.methodType().name());
      payload.put("status", record.status().name());
      payload.put("candidates", chemistryStoragePredictionCandidatePayloads(record.candidates()));
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStoragePredictionCandidatePayloads(
      final List<ChemistryStoragePredictionCandidateRecord> candidates) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(candidates.size());
    for (final ChemistryStoragePredictionCandidateRecord candidate : candidates) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("rank", candidate.rank());
      payload.put("productMoleculeIds", candidate.productMoleculeIds());
      payload.put("productFormulaSignatures", candidate.productFormulaSignatures());
      payload.put("productStoichiometrySignatures", candidate.productStoichiometrySignatures());
      payload.put("probability", candidate.probability());
      payload.put("expectedYieldPercent", candidate.expectedYieldPercent());
      payload.put("mechanismIds", candidate.mechanismIds());
      payload.put("mechanismDescriptors", candidate.mechanismDescriptors());
      payload.put("scoreComponents", candidate.scoreComponents());
      payload.put("evidence", candidate.evidence());
      payload.put("note", candidate.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageComputationPlanRecordPayloads(
      final List<ChemistryStorageComputationPlanRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageComputationPlanRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("reactionRecordId", record.reactionRecordId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("status", record.status().name());
      payload.put("classicalPlanIds", record.classicalPlanIds());
      payload.put("classicalResultIds", record.classicalResultIds());
      payload.put("quantumProgramIds", record.quantumProgramIds());
      payload.put("deferredExecution", record.deferredExecution());
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageClassicalResultRecordPayloads(
      final List<ChemistryStorageClassicalResultRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageClassicalResultRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("calculationId", record.calculationId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("status", record.status());
      payload.put("diagnosticCount", record.diagnosticCount());
      payload.put("errorCount", record.errorCount());
      payload.put("warningCount", record.warningCount());
      payload.put("sections", chemistryStorageClassicalResultSectionPayloads(record.sections()));
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageClassicalResultSectionPayloads(
      final List<ChemistryStorageClassicalResultSectionRecord> sections) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(sections.size());
    for (final ChemistryStorageClassicalResultSectionRecord section : sections) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("kind", section.kind());
      payload.put("title", section.title());
      payload.put("values", chemistryStorageClassicalResultValuePayloads(section.values()));
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageClassicalResultValuePayloads(
      final List<ChemistryStorageClassicalResultValueRecord> values) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(values.size());
    for (final ChemistryStorageClassicalResultValueRecord value : values) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("name", value.name());
      payload.put("kind", value.kind());
      payload.put("unit", value.unit());
      payload.put("values", value.values());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageQuantumProgramRecordPayloads(
      final List<ChemistryStorageQuantumProgramRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageQuantumProgramRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("sourceRecordId", record.sourceRecordId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("kind", record.kind());
      payload.put("target", record.target());
      payload.put("hasNativePayload", record.hasNativePayload());
      payload.put("hasExportedPayload", record.hasExportedPayload());
      payload.put("diagnostics", record.diagnostics());
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageRoadmapRecordPayloads(
      final List<ChemistryStorageRoadmapRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageRoadmapRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("requestId", record.requestId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("status", record.status());
      payload.put("conditionScenarioCount", record.conditionScenarioCount());
      payload.put("evaluatedConditionScenarioCount", record.evaluatedConditionScenarioCount());
      payload.put("candidates", chemistryStorageRoadmapCandidatePayloads(record.candidates()));
      payload.put("programLevelRecommendationTargets", record.programLevelRecommendationTargets());
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageRoadmapCandidatePayloads(
      final List<ChemistryStorageRoadmapCandidateRecord> candidates) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(candidates.size());
    for (final ChemistryStorageRoadmapCandidateRecord candidate : candidates) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("candidateIndex", candidate.candidateIndex());
      payload.put("probability", candidate.probability());
      payload.put("expectedYieldPercent", candidate.expectedYieldPercent());
      payload.put("productMoleculeIds", candidate.productMoleculeIds());
      payload.put("productFormulaSignatures", candidate.productFormulaSignatures());
      payload.put("productStoichiometrySignatures", candidate.productStoichiometrySignatures());
      payload.put("mechanismIds", candidate.mechanismIds());
      payload.put("classicalCalculationKinds", candidate.classicalCalculationKinds());
      payload.put("quantumPreparationTargets", candidate.quantumPreparationTargets());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageHistoryEventPayloads(
      final List<ChemistryStorageReactionHistoryEvent> events) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(events.size());
    for (final ChemistryStorageReactionHistoryEvent event : events) {
      payloads.add(chemistryStorageHistoryEventPayload(event));
    }
    return payloads;
  }

  private static Map<String, Object> chemistryStorageHistoryEventPayload(
      final ChemistryStorageReactionHistoryEvent event) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", event.id());
    payload.put("reactionId", event.reactionId());
    payload.put("historyIndex", event.historyIndex());
    payload.put("kind", event.kind().name());
    payload.put("title", event.title());
    payload.put("description", event.description());
    payload.put("reactionRecordIds", event.reactionRecordIds());
    payload.put("conditionSnapshotRecordIds", event.conditionSnapshotRecordIds());
    payload.put("predictionRecordIds", event.predictionRecordIds());
    payload.put("computationPlanRecordIds", event.computationPlanRecordIds());
    payload.put("classicalResultIds", event.classicalResultIds());
    payload.put("quantumProgramIds", event.quantumProgramIds());
    payload.put("referencesWork", event.referencesWork());
    return payload;
  }

  private static Map<String, Object> chemistryStorageTimelinePayload(
      final ChemistryStorageReactionTimeline timeline) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("reactionId", timeline.reactionId());
    payload.put("empty", timeline.empty());
    payload.put("recordCount", timeline.size());
    payload.put("eventCount", timeline.eventCount());
    payload.put("records", chemistryStorageReactionRecordPayloads(timeline.records()));
    payload.put("events", chemistryStorageHistoryEventPayloads(timeline.events()));
    return payload;
  }

  private static Map<String, Object> chemistryStorageSnapshotPayload(
      final ChemistryStorageReactionSnapshot snapshot) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("reactionId", snapshot.reactionId());
    payload.put("historyIndex", snapshot.historyIndex());
    payload.put("reactionRecord", chemistryStorageReactionRecordPayload(snapshot.reactionRecord()));
    payload.put(
        "conditionSnapshotRecords",
        chemistryStorageConditionSnapshotPayloads(snapshot.conditionSnapshotRecords()));
    payload.put("outcomeRecords", chemistryStorageOutcomeRecordPayloads(snapshot.outcomeRecords()));
    payload.put(
        "predictionRecords",
        chemistryStoragePredictionRecordPayloads(snapshot.predictionRecords()));
    payload.put("roadmapRecords", chemistryStorageRoadmapRecordPayloads(snapshot.roadmapRecords()));
    payload.put(
        "computationPlanRecords",
        chemistryStorageComputationPlanRecordPayloads(snapshot.computationPlanRecords()));
    payload.put(
        "classicalResultRecords",
        chemistryStorageClassicalResultRecordPayloads(snapshot.classicalResultRecords()));
    payload.put(
        "quantumProgramRecords",
        chemistryStorageQuantumProgramRecordPayloads(snapshot.quantumProgramRecords()));
    payload.put("historyEvents", chemistryStorageHistoryEventPayloads(snapshot.historyEvents()));
    return payload;
  }

  private static List<Map<String, Object>> chemistryStorageConditionSnapshotPayloads(
      final List<ChemistryStorageConditionSnapshotRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageConditionSnapshotRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("reactionId", record.reactionId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("label", record.label());
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageOutcomeRecordPayloads(
      final List<ChemistryStorageReactionOutcomeRecord> records) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(records.size());
    for (final ChemistryStorageReactionOutcomeRecord record : records) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", record.id());
      payload.put("reactionId", record.reactionId());
      payload.put("historyIndex", record.historyIndex());
      payload.put("kind", record.kind().name());
      payload.put("productMoleculeIds", record.productMoleculeIds());
      payload.put("probability", record.probability());
      payload.put("yieldPercent", record.yieldPercent());
      payload.put("mechanismIds", record.mechanismIds());
      payload.put("classicalResultIds", record.classicalResultIds());
      payload.put("quantumProgramIds", record.quantumProgramIds());
      payload.put("conditionSummary", record.conditionSummary());
      payload.put("note", record.note());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryStorageDiagnosticPayloads(
      final List<ChemistryStorageDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(diagnostics.size());
    for (final ChemistryStorageDiagnostic diagnostic : diagnostics) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("severity", diagnostic.severity().name());
      payload.put("code", diagnostic.code().name());
      payload.put("message", diagnostic.message());
      payload.put("line", diagnostic.line());
      payloads.add(payload);
    }
    return payloads;
  }

  private static Map<String, Object> chemistryPredictionWorkflowPayload(
      final OrganicReactionPredictionWorkflowResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", result.status().name());
    payload.put("success", result.success());
    payload.put("validation", chemistryValidationPayload(result.validationResult()));
    payload.put("candidateCount", chemistryCandidateCount(result));
    if (result.hasPredictionResult()) {
      payload.put("prediction", chemistryPredictionPayload(result.predictionResult()));
    }
    if (result.hasClassicalCalculationResult()) {
      payload.put(
          "classicalCalculation",
          chemistryClassicalResultPayload(result.classicalCalculationResult()));
    }
    payload.put(
        "recommendationPlan", chemistryRecommendationPlanPayload(result.recommendationPlan()));
    payload.put(
        "computationRoadmap", chemistryComputationRoadmapPayload(result.computationRoadmap()));
    return payload;
  }

  private static Map<String, Object> chemistryWorkflowPayload(
      final ChemistryWorkflowResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", result.status().name());
    payload.put("success", result.success());
    payload.put("jobId", result.job().id().value());
    payload.put("mode", result.job().mode().name());
    payload.put("stages", chemistryWorkflowStagePayloads(result.stageResults()));
    payload.put("stageSummary", chemistryWorkflowStageSummaryPayload(result));
    payload.put(
        "diagnostics", chemistryWorkflowDiagnosticPayloads(result.diagnostics().diagnostics()));
    if (result.coreValidationResult() != null) {
      payload.put("coreValidation", chemistryValidationPayload(result.coreValidationResult()));
    }
    if (result.classicalPreflightResult() != null) {
      payload.put(
          "classicalPreflight",
          chemistryClassicalPreflightPayload(result.classicalPreflightResult()));
    }
    if (result.hasClassicalCalculationResult()) {
      payload.put(
          "classicalResult", chemistryClassicalResultPayload(result.classicalCalculationResult()));
    }
    if (result.hasReactionPredictionResult()) {
      payload.put("prediction", chemistryPredictionPayload(result.reactionPredictionResult()));
    }
    payload.put(
        "recommendationPlan", chemistryRecommendationPlanPayload(result.recommendationPlan()));
    payload.put(
        "quantumProgramPackage",
        chemistryQuantumProgramPackagePayload(result.quantumProgramPackage()));
    payload.put(
        "computationParity", chemistryComputationParityPayload(result.computationParityPlan()));
    if (result.hasStoragePlan()) {
      payload.put(
          "storageDocument",
          chemistryStorageDocumentPayload(
              result
                  .storagePlan()
                  .toDocument(
                      result.job().id().value() + "_storage", "CLI chemistry workflow storage")));
    }
    return payload;
  }

  private static Map<String, Object> chemistryWorkflowExecutionBundlePayload(
      final ChemistryWorkflowExecutionBundle bundle) {
    final Map<String, Object> payload = chemistryWorkflowPayload(bundle.workflowResult());
    final ArrayList<Map<String, Object>> followUps =
        new ArrayList<>(bundle.followUpResults().size());
    for (int index = 0; index < bundle.followUpResults().size(); index++) {
      followUps.add(chemistryClassicalResultPayload(bundle.followUpResults().get(index)));
    }
    payload.put("executedFollowUpResults", followUps);
    payload.put("executedFollowUpCount", bundle.followUpResultCount());
    payload.put(
        "computationParity", chemistryComputationParityPayload(bundle.computationParityPlan()));
    payload.put(
        "storageDocument",
        chemistryStorageDocumentPayload(
            QuantumChemistry.workflowStorageDocument(
                bundle,
                bundle.workflowResult().job().id().value() + "_storage",
                "CLI chemistry workflow storage")));
    return payload;
  }

  private static Map<String, Object> chemistryWorkflowStageSummaryPayload(
      final ChemistryWorkflowResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("completed", result.completedStageCount());
    payload.put("warnings", result.warningStageCount());
    payload.put("failed", result.failedStageCount());
    payload.put("skipped", result.skippedStageCount());
    return payload;
  }

  private static List<Map<String, Object>> chemistryWorkflowStagePayloads(
      final List<ChemistryWorkflowStageResult> stages) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(stages.size());
    for (int index = 0; index < stages.size(); index++) {
      final ChemistryWorkflowStageResult stage = stages.get(index);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("stage", stage.stage().name());
      payload.put("status", stage.status().name());
      payload.put("message", stage.message());
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryWorkflowDiagnosticPayloads(
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(diagnostics.size());
    for (int index = 0; index < diagnostics.size(); index++) {
      final ChemistryWorkflowDiagnostic diagnostic = diagnostics.get(index);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("severity", diagnostic.severity().name());
      payload.put("code", diagnostic.code().name());
      payload.put("stage", diagnostic.stage().name());
      payload.put("sourceCode", diagnostic.sourceCode());
      payload.put("target", diagnostic.target());
      payload.put("message", diagnostic.message());
      payloads.add(payload);
    }
    return payloads;
  }

  private static Map<String, Object> chemistryClassicalPreflightPayload(
      final ClassicalPreflightResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", result.status().name());
    payload.put("profileId", result.profile().id());
    payload.put("profileName", result.profile().displayName());
    payload.put(
        "diagnostics", chemistryClassicalDiagnosticPayloads(result.diagnostics().diagnostics()));
    return payload;
  }

  private static List<Map<String, Object>> chemistryClassicalDiagnosticPayloads(
      final List<ClassicalDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(diagnostics.size());
    for (int index = 0; index < diagnostics.size(); index++) {
      final ClassicalDiagnostic diagnostic = diagnostics.get(index);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("severity", diagnostic.severity().name());
      payload.put("code", diagnostic.code().name());
      payload.put("targetKind", diagnostic.target().kind());
      payload.put("targetId", diagnostic.target().id());
      payload.put("message", diagnostic.message());
      payloads.add(payload);
    }
    return payloads;
  }

  private static Map<String, Object> chemistryClassicalResultPayload(
      final ClassicalCalculationResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("calculationId", result.calculationId().value());
    payload.put("status", result.status().name());
    payload.put("sectionCount", result.sections().size());
    payload.put(
        "quality",
        chemistryClassicalQualityPayload(QuantumChemistry.classicalResultQuality(result)));
    final ArrayList<Map<String, Object>> sections = new ArrayList<>(result.sections().size());
    for (int index = 0; index < result.sections().size(); index++) {
      sections.add(chemistryClassicalSectionPayload(result.sections().get(index)));
    }
    payload.put("sections", sections);
    return payload;
  }

  private static Map<String, Object> chemistryClassicalQualityPayload(
      final ClassicalResultQualityReport report) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sectionCount", report.sectionCount());
    payload.put("exactCount", report.exactCount());
    payload.put("approximateCount", report.approximateCount());
    payload.put("externalCount", report.externalCount());
    payload.put("fallbackCount", report.fallbackCount());
    payload.put("unsupportedCount", report.unsupportedCount());
    payload.put("unknownCount", report.unknownCount());
    payload.put("fullyExact", report.fullyExact());
    payload.put("hasApproximationOrFallback", report.hasApproximationOrFallback());
    payload.put("rows", report.rows());
    return payload;
  }

  private static Map<String, Object> chemistryClassicalSectionPayload(
      final ClassicalResultSection section) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("kind", section.kind().name());
    payload.put("title", section.title());
    final ArrayList<Map<String, Object>> values = new ArrayList<>(section.values().size());
    for (int index = 0; index < section.values().size(); index++) {
      values.add(chemistryClassicalValuePayload(section.values().get(index)));
    }
    payload.put("values", values);
    return payload;
  }

  private static Map<String, Object> chemistryClassicalValuePayload(
      final ClassicalResultValue value) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", value.name());
    payload.put("kind", value.kind().name());
    payload.put("unit", value.unit());
    payload.put("values", value.values());
    return payload;
  }

  private static Map<String, Object> chemistryQuantumProgramPackagePayload(
      final ChemistryQuantumProgramPackage programPackage) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sourceId", programPackage.sourceId());
    payload.put(
        "requestedMapping",
        programPackage.requestedMapping() == null ? "" : programPackage.requestedMapping().name());
    payload.put("spinOrbitalCount", programPackage.spinOrbitalCount());
    payload.put("electronCount", programPackage.electronCount());
    payload.put("artifactCount", programPackage.artifacts().size());
    payload.put("skippedCount", programPackage.skippedArtifacts().size());
    payload.put("totalOperationCount", programPackage.totalOperationCount());
    final ArrayList<Map<String, Object>> artifacts =
        new ArrayList<>(programPackage.artifacts().size());
    for (int index = 0; index < programPackage.artifacts().size(); index++) {
      artifacts.add(chemistryQuantumProgramArtifactPayload(programPackage.artifacts().get(index)));
    }
    payload.put("artifacts", artifacts);
    final ArrayList<Map<String, Object>> skipped =
        new ArrayList<>(programPackage.skippedArtifacts().size());
    for (int index = 0; index < programPackage.skippedArtifacts().size(); index++) {
      skipped.add(chemistryQuantumProgramSkipPayload(programPackage.skippedArtifacts().get(index)));
    }
    payload.put("skippedArtifacts", skipped);
    return payload;
  }

  private static Map<String, Object> chemistryQuantumProgramArtifactPayload(
      final ChemistryQuantumProgramArtifact artifact) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", artifact.id());
    payload.put("kind", artifact.kind().name());
    payload.put("mapping", artifact.hasMapping() ? artifact.mapping().name() : "");
    payload.put("description", artifact.description());
    payload.put("circuitCount", artifact.circuitCount());
    payload.put("operationCount", artifact.operationCount());
    return payload;
  }

  private static Map<String, Object> chemistryQuantumProgramSkipPayload(
      final ChemistryQuantumProgramSkip skip) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("kind", skip.kind().name());
    payload.put("reason", skip.reason());
    return payload;
  }

  private static Map<String, Object> chemistryComputationParityPayload(
      final ChemistryComputationParityPlan plan) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("workflowJobId", plan.workflowJobId());
    payload.put("itemCount", plan.items().size());
    payload.put("ready", plan.hasReadyItem());
    payload.put("blockingDiagnostics", plan.hasBlockingDiagnostics());
    final ArrayList<Map<String, Object>> items = new ArrayList<>(plan.items().size());
    for (int index = 0; index < plan.items().size(); index++) {
      items.add(chemistryComputationParityItemPayload(plan.items().get(index)));
    }
    payload.put("items", items);
    return payload;
  }

  private static Map<String, Object> chemistryComputationParityItemPayload(
      final ChemistryComputationParityItem item) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("candidateIndex", item.candidateIndex());
    payload.put("mechanismIds", item.mechanismIds());
    payload.put("pathwayStepCount", item.pathwayStepCount());
    payload.put("classicalPlanIds", item.classicalPlanIds());
    payload.put("classicalResultIds", item.classicalResultIds());
    payload.put("quantumProgramIds", item.quantumProgramIds());
    payload.put("status", item.status().name());
    payload.put("explanation", item.explanation());
    final ArrayList<Map<String, Object>> diagnostics = new ArrayList<>(item.diagnostics().size());
    for (int index = 0; index < item.diagnostics().size(); index++) {
      diagnostics.add(chemistryComputationParityDiagnosticPayload(item.diagnostics().get(index)));
    }
    payload.put("diagnostics", diagnostics);
    return payload;
  }

  private static Map<String, Object> chemistryComputationParityDiagnosticPayload(
      final ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity
              .ChemistryComputationParityDiagnostic
          diagnostic) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("severity", diagnostic.severity().name());
    payload.put("code", diagnostic.code().name());
    payload.put("message", diagnostic.message());
    return payload;
  }

  private static Map<String, Object> chemistryPredictionPayload(
      final ReactionPredictionResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("methodType", result.methodType().name());
    payload.put("candidateCount", result.candidates().size());
    payload.put("candidates", chemistryCandidatePayloads(result.candidates()));
    payload.put("diagnostics", chemistryEvidencePayloads(result.diagnostics()));
    return payload;
  }

  private static List<Map<String, Object>> chemistryLibraryEntryPayloads(
      final List<ChemistryLibraryEntry> entries) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(entries.size());
    for (int index = 0; index < entries.size(); index++) {
      payloads.add(chemistryLibraryEntryPayload(entries.get(index)));
    }
    return payloads;
  }

  private static Map<String, Object> chemistryLibraryEntryPayload(
      final ChemistryLibraryEntry entry) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", entry.id());
    payload.put("displayName", entry.displayName());
    payload.put("kind", entry.kind().name());
    payload.put("category", entry.category().name());
    payload.put("difficulty", entry.difficulty().name());
    payload.put("summary", entry.summary());
    payload.put("tags", entry.tags());
    payload.put("references", entry.references());
    payload.put("moleculeCount", entry.document().molecules().size());
    payload.put("reactionCount", entry.document().reactions().size());
    return payload;
  }

  private static Map<String, Object> chemistryValidationPayload(
      final ChemistryValidationResult validation) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("valid", validation.valid());
    payload.put("diagnosticCount", validation.diagnostics().size());
    payload.put("diagnostics", chemistryDiagnosticPayloads(validation.diagnostics()));
    return payload;
  }

  private static List<Map<String, Object>> chemistryDiagnosticPayloads(
      final List<ChemistryDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(diagnostics.size());
    for (int index = 0; index < diagnostics.size(); index++) {
      final ChemistryDiagnostic diagnostic = diagnostics.get(index);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("severity", diagnostic.severity().name());
      payload.put("code", diagnostic.code().name());
      payload.put("message", diagnostic.message());
      if (diagnostic.hasTarget()) {
        payload.put("targetKind", diagnostic.target().kind());
        payload.put("targetId", diagnostic.target().id());
      }
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryCandidatePayloads(
      final List<ReactionProductCandidate> candidates) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(candidates.size());
    for (int index = 0; index < candidates.size(); index++) {
      final ReactionProductCandidate candidate = candidates.get(index);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("index", index);
      payload.put("probability", candidate.probability());
      payload.put("expectedYieldPercent", candidate.expectedYieldPercent());
      payload.put("selectivityNote", candidate.selectivityNote());
      payload.put("productCount", candidate.products().participantCount());
      payload.put("evidence", chemistryEvidencePayloads(candidate.evidence()));
      payloads.add(payload);
    }
    return payloads;
  }

  private static List<Map<String, Object>> chemistryEvidencePayloads(
      final List<ReactionPredictionEvidence> evidence) {
    final ArrayList<Map<String, Object>> payloads = new ArrayList<>(evidence.size());
    for (int index = 0; index < evidence.size(); index++) {
      final ReactionPredictionEvidence item = evidence.get(index);
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("kind", item.kind().name());
      payload.put("description", item.description());
      payload.put("weight", item.weight());
      payloads.add(payload);
    }
    return payloads;
  }

  private static Map<String, Object> chemistryRecommendationPlanPayload(
      final ChemistryWorkflowRecommendationPlan plan) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("workflowJobId", plan.workflowJobId());
    payload.put("count", plan.size());
    final ArrayList<Map<String, Object>> recommendations = new ArrayList<>(plan.size());
    for (int index = 0; index < plan.recommendations().size(); index++) {
      recommendations.add(chemistryRecommendationPayload(plan.recommendations().get(index)));
    }
    payload.put("recommendations", recommendations);
    return payload;
  }

  private static Map<String, Object> chemistryRecommendationPayload(
      final ChemistryWorkflowRecommendation recommendation) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("kind", recommendation.kind().name());
    payload.put("priority", recommendation.priority().name());
    payload.put("candidateIndex", recommendation.candidateIndex());
    if (recommendation.hasCalculationKind()) {
      payload.put("calculationKind", recommendation.calculationKind().name());
    }
    payload.put("target", recommendation.target());
    payload.put("reason", recommendation.reason());
    return payload;
  }

  private static Map<String, Object> chemistryComputationRoadmapPayload(
      final OrganicReactionComputationRoadmap roadmap) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("requestId", roadmap.requestId());
    payload.put("status", roadmap.status().name());
    payload.put("conditionScenarioCount", roadmap.conditionScenarioCount());
    payload.put("evaluatedConditionScenarioCount", roadmap.evaluatedConditionScenarioCount());
    payload.put("readyForFollowUpWork", roadmap.readyForFollowUpWork());
    payload.put("hasClassicalCalculations", roadmap.hasClassicalCalculations());
    payload.put("hasQuantumPreparation", roadmap.hasQuantumPreparation());
    final ArrayList<Map<String, Object>> candidates = new ArrayList<>(roadmap.candidates().size());
    for (int index = 0; index < roadmap.candidates().size(); index++) {
      candidates.add(chemistryCandidateRoadmapPayload(roadmap.candidates().get(index)));
    }
    payload.put("candidates", candidates);
    final ArrayList<String> programTargets =
        new ArrayList<>(roadmap.programLevelRecommendations().size());
    for (int index = 0; index < roadmap.programLevelRecommendations().size(); index++) {
      programTargets.add(roadmap.programLevelRecommendations().get(index).target());
    }
    payload.put("programLevelRecommendationTargets", programTargets);
    return payload;
  }

  private static Map<String, Object> chemistryCandidateRoadmapPayload(
      final OrganicReactionCandidateRoadmap roadmap) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("candidateIndex", roadmap.candidateIndex());
    payload.put("probability", roadmap.probability());
    payload.put("expectedYieldPercent", roadmap.expectedYieldPercent());
    payload.put("productMoleculeIds", roadmap.productMoleculeIds());
    payload.put("mechanismIds", roadmap.mechanismIds());
    final ArrayList<String> calculationKinds =
        new ArrayList<>(roadmap.classicalCalculationKinds().size());
    for (int index = 0; index < roadmap.classicalCalculationKinds().size(); index++) {
      calculationKinds.add(roadmap.classicalCalculationKinds().get(index).name());
    }
    payload.put("classicalCalculationKinds", calculationKinds);
    payload.put("quantumPreparationTargets", roadmap.quantumPreparationTargets());
    payload.put("highPriorityWork", roadmap.hasHighPriorityWork());
    return payload;
  }

  private static Map<String, Object> benchmarkStagePayload(final BenchmarkStageResult stage) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("stage", stage.stage());
    payload.put("success", stage.isSuccess());
    payload.put("status", stage.status());
    payload.put("warmupIterations", stage.warmupIterations());
    payload.put("measurementIterations", stage.measurementIterations());
    payload.put("minNanos", stage.minNanos());
    payload.put("averageNanos", stage.averageNanos());
    payload.put("maxNanos", stage.maxNanos());
    payload.put("memoryDeltaBytes", stage.memoryDeltaBytes());
    return payload;
  }

  private static Map<String, Object> compilerPayload(final CompilerResult result) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("targetFormat", result.format().name());
    payload.put("status", result.status().name());
    payload.put("success", result.isSuccess());
    payload.put("stageCount", result.stageRecords().size());
    payload.put("hasContent", result.hasExportResult() && result.exportResult().hasContent());
    final ArrayList<Map<String, Object>> stages = new ArrayList<>(result.stageRecords().size());
    for (final CompilerStageRecord stage : result.stageRecords()) {
      stages.add(
          Map.of(
              "stage",
              stage.stage().name(),
              "status",
              stage.status().name(),
              "message",
              stage.message()));
    }
    payload.put("stages", stages);
    return payload;
  }

  private static List<Map<String, Object>> diagnosticsPayload(
      final List<IntegrationDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payload = new ArrayList<>();
    for (IntegrationDiagnostic diagnostic : diagnostics) {
      payload.add(
          Map.of(
              "severity",
              diagnostic.severity().name(),
              "code",
              diagnostic.code().name(),
              "message",
              diagnostic.message(),
              "line",
              diagnostic.line(),
              "column",
              diagnostic.column()));
    }
    return payload;
  }

  private static List<Map<String, Object>> simulationDiagnosticsPayload(
      final List<SimulationDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payload = new ArrayList<>();
    for (SimulationDiagnostic diagnostic : diagnostics) {
      payload.add(
          Map.of(
              "severity",
              diagnostic.severity().name(),
              "code",
              diagnostic.code().name(),
              "message",
              diagnostic.message(),
              "circuitIndex",
              diagnostic.circuitIndex(),
              "operationIndex",
              diagnostic.operationIndex()));
    }
    return payload;
  }

  private static List<Map<String, Object>> backendDiagnosticsPayload(
      final List<BackendDiagnostic> diagnostics) {
    final ArrayList<Map<String, Object>> payload = new ArrayList<>();
    for (BackendDiagnostic diagnostic : diagnostics) {
      payload.add(
          Map.of(
              "severity",
              diagnostic.severity().name(),
              "code",
              diagnostic.code().name(),
              "message",
              diagnostic.message()));
    }
    return payload;
  }

  private static void printImportFailure(
      final CliArguments arguments,
      final PrintStream out,
      final String command,
      final ProgramLoadResult load)
      throws JsonProcessingException {
    printResult(
        arguments,
        out,
        Map.of("command", command, "status", "import_failed", "diagnostics", load.diagnostics()),
        "Import failed.");
  }

  private static void printResult(
      final CliArguments arguments,
      final PrintStream out,
      final Map<String, Object> payload,
      final String text)
      throws JsonProcessingException {
    if (arguments.outputMode().equals("json")) {
      out.println(JSON.writeValueAsString(payload));
    } else {
      out.println(text);
    }
  }

  private static String helpText() {
    return """
            quantum <command> [options]

            Commands:
              validate          Validate a program.
              inspect           Print program metrics.
              resources         Estimate memory, depth, and simulation resource pressure.
              circuit           Print circuit timeline data for visualization.
              preflight         Check export capability for a target format.
              transform         Apply explicit conservative transformations.
              compile           Run validation, inspection, preflight, transformation, and export.
              convert           Convert between native JSON and supported external formats.
              simulate          Run local state-vector simulation where supported.
              backend-dry-run   Submit a tracked local backend dry-run job.
              workflow          Run validate, inspect, resources, circuit, simulate, compile, and dry-run backend.
              benchmark         Measure import, validation, inspection, resources, simulation, compile, and workflow.
              compatibility     Build product compatibility matrix for all supported targets.
              verify-cross-format
                                Verify external format -> IR -> every supported target -> IR round-trips.
              regress-corpus    Run compatibility and cross-format regression for a directory corpus.
              release-readiness Run release readiness audit for a directory corpus.
              target-profile    Print target profile metadata.
              doctor            Check local product structure, scripts, examples, and packaged jars.
              product-audit     Run doctor plus release readiness as one product gate.
              product-report    Write product-audit.json and summary.md bundle.
              product-distribution
                                Write a local release distribution bundle.
              product-verify-distribution
                                Verify an unpacked release distribution bundle.
              chemistry-library
                                Search built-in chemistry molecules, reactions, and workflow templates.
              chemistry-predict
                                Predict organic reaction products from library molecules and conditions.
              chemistry-workflow
                                Run chemistry validation, classical planning, prediction, Quantum IR, parity, and storage plan.
              chemistry-calibration-coverage
                                Print heterocycle reaction outcome calibration coverage and empirical readiness.
              chemistry-mechanism-coverage
                                Print built-in organic and physical mechanism catalog coverage.
              chemistry-storage-summary
                                Read .qchem storage, print project, roadmap, timeline, and snapshot summary.

            Common options:
              --input <path>
              --project-root <path>
              --input-format auto|json|openqasm2|openqasm3|quil
              --output <path>
              --output-format json|openqasm2|openqasm3|quil
              --format text|json

            Simulation options:
              --shots <count>
              --seed <long>
              --max-qubits <count>

            Chemistry prediction options:
              --query <text>
              --kind molecule|reaction|fragment|dataset|workflow_template|mixed_project
              --reactant-entry <library-id>
                                May be repeated. Repeated values define a multi-reactant side.
              --reactant-coefficient <positive integer>
                                Optional repeated stoichiometric coefficient aligned with --reactant-entry.
              --subject-entry <library-id>
              --candidate-entry <library-id>
                                May be repeated for multiple product-side hypotheses.
              --candidate-side-entry <library-id>
                                May be repeated to build one multi-product seed candidate side.
              --candidate-side-coefficient <positive integer>
                                Optional repeated stoichiometric coefficient aligned with --candidate-side-entry.
              --domain heterocycle_synthesis|heterocycle_functionalization|cross_coupling|unknown
              --temperature-celsius <value>
              --pressure-atm <value>
              --duration-hours <value>
              --solvent <name>
              --catalyst <name>
              --condition-component <role:name[:amount:unit:phase:note]>
                                May be repeated. Example: solvent:PEG4000:70:g:solution:70 g PEG4000.
              --mode validate_only|classical_only|hamiltonian_only|classical_and_hamiltonian|full_quantum_preparation
              --method hartree-fock|hf|dft:<functional>
              --basis <basis-name>
              --mapping jordan_wigner|parity|bravyi_kitaev|tapered_parity
              --calculation-kinds <comma-separated calculation kinds>
              --electronic-rerank
                                Re-rank generated product candidates through native electronic scoring.
              --electronic-rerank-max-candidates <count>
              --electronic-rerank-max-atoms <count>
              --quantum-preparation
                                Prepare Quantum IR workflows for generated product candidates.
              --quantum-workflow-vqe
                                Execute local Java-native VQE for prepared candidate quantum workflows.
              --quantum-preparation-max-candidates <count>
              --quantum-preparation-max-participants <count>
              --calculation-option <name=value>
                                Advanced calculation option escape hatch.
              --active-electrons <count>
              --active-orbitals <count>
              --benchmark
              --execute-followups
              --followup-limit <count>

            Chemistry storage options:
              --reaction-id <id>
              --history-index <number>
            """;
  }
}
