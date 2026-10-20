/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.MoleculeGeometryRegularizer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.MoleculeGeometryRegularizer.MoleculeGeometryRegularization;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.ReactionEnergeticsExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreBreakdown;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralCapability;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.IntegralMoleculeFactory;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/** Переранжирует кандидаты продуктов через Java-native расчетную энергию, не используя справочные исходы реакций. */
public final class ReactionPredictionElectronicReranker {

  private static final String ENABLED_OPTION = "reaction_prediction_electronic_rerank";
  private static final String ADVANCED_ENABLED_OPTION = "advanced_prediction_electronic_rerank";
  private static final String MAX_CANDIDATES_OPTION =
      "reaction_prediction_electronic_rerank_max_candidates";
  private static final String ADVANCED_MAX_CANDIDATES_OPTION =
      "advanced_prediction_electronic_rerank_max_candidates";
  private static final String MAX_ATOMS_OPTION = "reaction_prediction_electronic_rerank_max_atoms";
  private static final String ADVANCED_MAX_ATOMS_OPTION =
      "advanced_prediction_electronic_rerank_max_atoms";
  private static final String REACTION_ENERGY_MODEL_OPTION = "reaction_energy_model";
  private static final String QUANTUM_GEOMETRY_SOURCE_OPTION =
      "quantum_preparation_geometry_source";
  private static final String REGULARIZED_PREDICTION_GEOMETRY_SOURCE =
      "prediction_regularized_graph_layout";
  private static final String SCF_INTEGRAL_MODE_OPTION = "scf_integral_mode";
  private static final String SCF_REFERENCE_MODE_OPTION = "scf_reference_mode";
  private static final String DIRECT_INTEGRAL_MODE = "direct";
  private static final String CORE_HAMILTONIAN_REFERENCE_MODE = "core_hamiltonian";
  private static final String ACTIVE_SPACE_FIRST_ORBITAL_OPTION = "active_space_first_orbital";
  private static final int FULL_TENSOR_WORKFLOW_ORBITAL_LIMIT = 24;
  private static final int DEFAULT_MAX_CANDIDATES = 6;
  private static final int DEFAULT_MAX_ATOMS = 48;
  private static final double MIN_LOG_WEIGHT = -2.5;
  private static final double MAX_LOG_WEIGHT = 2.5;
  private static final double GIBBS_SCALE_KILOJOULE_PER_MOLE = 55.0;
  private final ReactionEnergeticsExecutor energeticsExecutor;

  public ReactionPredictionElectronicReranker() {
    this(new ReactionEnergeticsExecutor());
  }

  public ReactionPredictionElectronicReranker(final ReactionEnergeticsExecutor energeticsExecutor) {
    if (energeticsExecutor == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic reranker energetics executor must not be null.");
    }
    this.energeticsExecutor = energeticsExecutor;
  }

  public ReactionPredictionResult rerank(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionPredictionResult predictionResult,
      final List<ClassicalDiagnostic> diagnostics) {
    if (calculationRequest == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic rerank calculation request must not be null.");
    }
    if (predictionResult == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic rerank result must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic rerank diagnostics must not be null.");
    }
    if (!ReactionPredictionElectronicReranker.enabled(calculationRequest)
        || !predictionResult.hasCandidates()) {
      return predictionResult;
    }
    final int maxCandidates =
        ReactionPredictionElectronicReranker.maxCandidates(calculationRequest);
    final ArrayList<RerankedCandidate> reranked =
        new ArrayList<RerankedCandidate>(predictionResult.candidates().size());
    boolean hasElectronicWeight = false;
    boolean hasRerankEvidence = false;
    final List<ReactionProductCandidate> candidates = predictionResult.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
        final RerankedCandidate skipped =
            RerankedCandidate.withEvidence(
                candidate,
                1.0,
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.WARNING,
                    "Java-native electronic rerank skipped candidate "
                        + i
                        + " because it is a mechanism-only product channel without an explicit product graph.",
                    -0.01));
        reranked.add(skipped);
        hasRerankEvidence = true;
        continue;
      }
      if (i >= maxCandidates) {
        reranked.add(
            ReactionPredictionElectronicReranker.skippedCandidate(
                candidate,
                i,
                "candidate index is outside reaction_prediction_electronic_rerank_max_candidates="
                    + maxCandidates));
        hasRerankEvidence = true;
        continue;
      }
      if (!this.withinAtomBudget(calculationRequest, candidate)) {
        reranked.add(
            ReactionPredictionElectronicReranker.skippedCandidate(
                candidate,
                i,
                "reactant plus product atom count exceeds reaction_prediction_electronic_rerank_max_atoms="
                    + ReactionPredictionElectronicReranker.maxAtoms(calculationRequest)));
        hasRerankEvidence = true;
        continue;
      }
      final RerankedCandidate scored =
          this.rerankedCandidate(calculationRequest, candidate, i, diagnostics);
      reranked.add(scored);
      hasElectronicWeight = hasElectronicWeight || scored.hasElectronicWeight;
      hasRerankEvidence = hasRerankEvidence || scored.rerankEvidence != null;
    }
    if (!hasElectronicWeight) {
      if (!hasRerankEvidence) {
        return predictionResult;
      }
      return ReactionPredictionResult.of(
          predictionResult.request(),
          predictionResult.methodType(),
          ReactionPredictionElectronicReranker.candidatesWithUpdatedEvidence(reranked),
          predictionResult.mechanismChannels(),
          predictionResult.diagnostics());
    }
    return ReactionPredictionResult.of(
        predictionResult.request(),
        predictionResult.methodType(),
        ReactionPredictionElectronicReranker.normalizedCandidates(reranked),
        predictionResult.mechanismChannels(),
        predictionResult.diagnostics());
  }

  public ClassicalCalculationRequest prepareEnergyRequest(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionProductCandidate candidate,
      final int index) {
    if (calculationRequest == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic rerank calculation request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic rerank candidate must not be null.");
    }
    if (!(calculationRequest.subject() instanceof Reaction)) {
      throw new IllegalArgumentException(
          "Reaction prediction electronic rerank subject must be a reaction.");
    }
    final SideRegularization reactants =
        ReactionPredictionElectronicReranker.regularizedSide(
            ((Reaction) calculationRequest.subject()).reactants());
    final SideRegularization products =
        ReactionPredictionElectronicReranker.regularizedSide(candidate.products());
    final Reaction reaction =
        Reaction.of(
            ReactionId.of(calculationRequest.id().value() + "_candidate_energy_" + index),
            "Predicted candidate electronic energy " + index,
            reactants.side,
            products.side,
            ((Reaction) calculationRequest.subject()).conditions(),
            ChemistryMetadata.EMPTY);
    final ElectronicStructureMethod rerankMethod =
        ReactionPredictionElectronicReranker.rerankMethod(calculationRequest, reaction);
    final BasisSet rerankBasisSet =
        ReactionPredictionElectronicReranker.rerankBasisSet(calculationRequest, reaction);
    final ArrayList<ClassicalCalculationOption> options =
        new ArrayList<ClassicalCalculationOption>(
            ReactionPredictionElectronicReranker.nativeOptions(calculationRequest.options()));
    if (reactants.regularized || products.regularized) {
      ReactionPredictionElectronicReranker.addOrReplace(
          options,
          ClassicalCalculationOption.text(
              QUANTUM_GEOMETRY_SOURCE_OPTION, REGULARIZED_PREDICTION_GEOMETRY_SOURCE));
    }
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(calculationRequest.id().value() + "_candidate_energy_" + index),
        reaction,
        rerankMethod,
        rerankBasisSet,
        calculationRequest.activeSpace(),
        List.of(ClassicalCalculationKind.REACTION_ENERGY),
        List.copyOf(options),
        calculationRequest.metadata());
  }

  public ClassicalCalculationRequest prepareQuantumPreparationRequest(
      final ClassicalCalculationRequest calculationRequest,
      final Molecule molecule,
      final int candidateIndex,
      final int participantIndex) {
    if (calculationRequest == null) {
      throw new IllegalArgumentException(
          "Reaction prediction quantum preparation calculation request must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException(
          "Reaction prediction quantum preparation molecule must not be null.");
    }
    final MoleculeGeometryRegularization regularization =
        MoleculeGeometryRegularizer.regularizeForElectronicPreparation(molecule);
    final Molecule preparedMolecule = regularization.molecule();
    final BasisSet basisSet =
        ReactionPredictionElectronicReranker.singleMoleculeBasisSet(
            calculationRequest, preparedMolecule);
    final ArrayList<ClassicalCalculationOption> options =
        new ArrayList<ClassicalCalculationOption>(
            ReactionPredictionElectronicReranker.nativeOptions(calculationRequest.options()));
    final boolean heavyWorkflow =
        ReactionPredictionElectronicReranker.heavyWorkflowPreparation(preparedMolecule, basisSet);
    final ActiveSpace activeSpace =
        heavyWorkflow
            ? ReactionPredictionElectronicReranker.workflowMinimalActiveSpace(preparedMolecule)
            : ReactionPredictionElectronicReranker.workflowActiveSpace(
                calculationRequest, preparedMolecule, basisSet, options);
    if (regularization.regularized()) {
      ReactionPredictionElectronicReranker.addOrReplace(
          options,
          ClassicalCalculationOption.text(
              QUANTUM_GEOMETRY_SOURCE_OPTION, REGULARIZED_PREDICTION_GEOMETRY_SOURCE));
    }
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(
            calculationRequest.id().value()
                + "_candidate_"
                + (candidateIndex + 1)
                + "_product_"
                + (participantIndex + 1)
                + "_quantum_preparation"),
        preparedMolecule,
        heavyWorkflow
            ? ElectronicStructureMethod.SEMI_EMPIRICAL
            : ReactionPredictionElectronicReranker.singleMoleculeMethod(
                calculationRequest, preparedMolecule),
        basisSet,
        activeSpace,
        List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
        List.copyOf(options),
        calculationRequest.metadata());
  }

  private RerankedCandidate rerankedCandidate(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionProductCandidate candidate,
      final int index,
      final List<ClassicalDiagnostic> diagnostics) {
    final ClassicalCalculationRequest energyRequest =
        this.prepareEnergyRequest(calculationRequest, candidate, index);
    final ArrayList<ClassicalDiagnostic> localDiagnostics = new ArrayList<ClassicalDiagnostic>();
    final ClassicalResultSection section =
        this.energeticsExecutor.execute(
            energyRequest, ClassicalCalculationKind.REACTION_ENERGY, localDiagnostics);
    diagnostics.addAll(localDiagnostics);
    if (section == null
        || !section.hasValue("method")
        || !"java_native_single_point_reaction_energy".equals(section.requireFirstValue("method"))
        || !section.hasValue("delta_gibbs")) {
      return RerankedCandidate.withEvidence(
          candidate,
          1.0,
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Java-native electronic rerank was unavailable for candidate "
                  + index
                  + ": "
                  + ReactionPredictionElectronicReranker.unavailableReason(
                      section, localDiagnostics),
              -0.02));
    }
    final double deltaGibbs = section.requireScalar("delta_gibbs");
    final ElectronicStructureMethod rerankMethod = energyRequest.method();
    final BasisSet rerankBasisSet = energyRequest.basisSet();
    final double logWeight =
        Math.max(
            MIN_LOG_WEIGHT, Math.min(MAX_LOG_WEIGHT, -deltaGibbs / GIBBS_SCALE_KILOJOULE_PER_MOLE));
    final ReactionPredictionEvidence rerankEvidence =
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Java-native candidate electronic rerank delta_gibbs="
                + deltaGibbs
                + " kJ/mol, log_weight="
                + logWeight
                + ".",
            Math.max(-1.0, Math.min(1.0, logWeight / MAX_LOG_WEIGHT)));
    if (!rerankMethod.equals(calculationRequest.method())
        || !rerankBasisSet.equals(calculationRequest.basisSet())) {
      return RerankedCandidate.withEvidence(
          candidate,
          Math.exp(logWeight),
          List.of(
              ReactionPredictionEvidence.of(
                  ReactionPredictionEvidenceKind.WARNING,
                  "Requested electronic rerank method or named basis was outside the native full-tensor path for candidate "
                      + index
                      + "; Java-native compatible method/basis fallback was used: method="
                      + rerankMethod.name().value()
                      + ", basis="
                      + rerankBasisSet.name().value()
                      + ".",
                  -0.03),
              rerankEvidence));
    }
    if (ReactionPredictionElectronicReranker.regularizedPredictionGeometry(energyRequest)) {
      return RerankedCandidate.withEvidence(
          candidate,
          Math.exp(logWeight),
          List.of(
              ReactionPredictionEvidence.of(
                  ReactionPredictionEvidenceKind.WARNING,
                  "Java-native electronic rerank used deterministic graph-derived coordinates because at least one participant had missing or overlapping geometry.",
                  -0.02),
              rerankEvidence));
    }
    return RerankedCandidate.withEvidence(candidate, Math.exp(logWeight), rerankEvidence);
  }

  private boolean withinAtomBudget(
      final ClassicalCalculationRequest request, final ReactionProductCandidate candidate) {
    final Reaction reaction = (Reaction) request.subject();
    final int total =
        ReactionPredictionElectronicReranker.sideAtomCount(reaction.reactants())
            + ReactionPredictionElectronicReranker.sideAtomCount(candidate.products());
    return total <= ReactionPredictionElectronicReranker.maxAtoms(request);
  }

  private static RerankedCandidate skippedCandidate(
      final ReactionProductCandidate candidate, final int index, final String reason) {
    return RerankedCandidate.withEvidence(
        candidate,
        1.0,
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.WARNING,
            "Java-native electronic rerank skipped candidate " + index + " because " + reason + ".",
            -0.01));
  }

  private static int sideAtomCount(
      final ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide side) {
    int total = 0;
    for (int i = 0; i < side.participants().size(); ++i) {
      total +=
          side.participants().get(i).coefficient().value()
              * side.participants().get(i).molecule().atomCount();
    }
    return total;
  }

  private static SideRegularization regularizedSide(final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    final ArrayList<ReactionParticipant> regularized =
        new ArrayList<ReactionParticipant>(participants.size());
    boolean changed = false;
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final MoleculeGeometryRegularization regularization =
          MoleculeGeometryRegularizer.regularizeForElectronicPreparation(participant.molecule());
      changed = changed || regularization.regularized();
      regularized.add(ReactionParticipant.of(regularization.molecule(), participant.coefficient()));
    }
    return new SideRegularization(ReactionSide.of(regularized), changed);
  }

  private static List<ReactionProductCandidate> normalizedCandidates(
      final List<RerankedCandidate> candidates) {
    double total = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      total += candidates.get(i).weightedProbability;
    }
    final boolean keepOriginalProbabilities = !Double.isFinite(total) || total <= 0.0;
    final ArrayList<ReactionProductCandidate> result =
        new ArrayList<ReactionProductCandidate>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final RerankedCandidate reranked = candidates.get(i);
      final double probability =
          keepOriginalProbabilities
              ? reranked.candidate.probability()
              : reranked.weightedProbability / total;
      final ReactionPredictionScoreBreakdown scoreBreakdown =
          ReactionPredictionElectronicReranker.updatedScoreBreakdown(reranked, probability);
      result.add(
          ReactionProductCandidate.of(
              reranked.candidate.products(),
              probability,
              ReactionPredictionElectronicReranker.normalizedExpectedYield(
                  reranked.candidate, probability),
              reranked.candidate.selectivityNote(),
              reranked.evidence,
              reranked.candidate.predictedMechanisms(),
              scoreBreakdown));
    }
    return List.copyOf(result);
  }

  private static List<ReactionProductCandidate> candidatesWithUpdatedEvidence(
      final List<RerankedCandidate> candidates) {
    final ArrayList<ReactionProductCandidate> result =
        new ArrayList<ReactionProductCandidate>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final RerankedCandidate reranked = candidates.get(i);
      result.add(
          ReactionProductCandidate.of(
              reranked.candidate.products(),
              reranked.candidate.probability(),
              reranked.candidate.expectedYieldPercent(),
              reranked.candidate.selectivityNote(),
              reranked.evidence,
              reranked.candidate.predictedMechanisms(),
              reranked.candidate.scoreBreakdown()));
    }
    return List.copyOf(result);
  }

  private static ReactionPredictionScoreBreakdown updatedScoreBreakdown(
      final RerankedCandidate candidate, final double probability) {
    final ReactionPredictionScoreBreakdown original = candidate.candidate.scoreBreakdown();
    if (candidate.rerankEvidence == null) {
      return ReactionPredictionScoreBreakdown.of(
          original.rawScore(), probability, original.components());
    }
    final ArrayList<ReactionPredictionScoreComponent> components =
        new ArrayList<ReactionPredictionScoreComponent>(original.components().size() + 1);
    components.addAll(original.components());
    components.add(
        ReactionPredictionScoreComponent.of(
            ReactionPredictionScoreComponentKind.ELECTRONIC,
            candidate.rerankEvidence.description(),
            candidate.rerankEvidence.weight()));
    return ReactionPredictionScoreBreakdown.of(
        original.rawScore() * candidate.multiplier, probability, components);
  }

  private static double normalizedExpectedYield(
      final ReactionProductCandidate candidate, final double normalizedProbability) {
    final double intrinsicYield = candidate.expectedYieldPercent() / 100.0;
    final double boundedYield = Math.max(0.0, Math.min(1.0, intrinsicYield));
    return 100.0 * normalizedProbability * boundedYield;
  }

  private static boolean enabled(final ClassicalCalculationRequest request) {
    for (int i = 0; i < request.options().size(); ++i) {
      final ClassicalCalculationOption option = request.options().get(i);
      if (ENABLED_OPTION.equals(option.name()) || ADVANCED_ENABLED_OPTION.equals(option.name())) {
        return Boolean.parseBoolean(option.value());
      }
    }
    return false;
  }

  private static boolean regularizedPredictionGeometry(final ClassicalCalculationRequest request) {
    for (int i = 0; i < request.options().size(); ++i) {
      final ClassicalCalculationOption option = request.options().get(i);
      if (!QUANTUM_GEOMETRY_SOURCE_OPTION.equals(option.name())) {
        continue;
      }
      return REGULARIZED_PREDICTION_GEOMETRY_SOURCE.equals(option.value());
    }
    return false;
  }

  private static int maxCandidates(final ClassicalCalculationRequest request) {
    return ReactionPredictionElectronicReranker.integerOption(
        request, MAX_CANDIDATES_OPTION, ADVANCED_MAX_CANDIDATES_OPTION, DEFAULT_MAX_CANDIDATES);
  }

  private static int maxAtoms(final ClassicalCalculationRequest request) {
    return ReactionPredictionElectronicReranker.integerOption(
        request, MAX_ATOMS_OPTION, ADVANCED_MAX_ATOMS_OPTION, DEFAULT_MAX_ATOMS);
  }

  private static int integerOption(
      final ClassicalCalculationRequest request,
      final String name,
      final String alias,
      final int defaultValue) {
    for (int i = 0; i < request.options().size(); ++i) {
      final ClassicalCalculationOption option = request.options().get(i);
      if (!name.equals(option.name()) && !alias.equals(option.name())) {
        continue;
      }
      final int value;
      try {
        value = Integer.parseInt(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(name + " must be an integer.");
      }
      if (value <= 0) {
        throw new IllegalArgumentException(name + " must be positive.");
      }
      return value;
    }
    return defaultValue;
  }

  private static List<ClassicalCalculationOption> nativeOptions(
      final List<ClassicalCalculationOption> options) {
    final ArrayList<ClassicalCalculationOption> result =
        new ArrayList<ClassicalCalculationOption>();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if ("single_point_backend".equals(option.name())
          || ENABLED_OPTION.equals(option.name())
          || ADVANCED_ENABLED_OPTION.equals(option.name())
          || MAX_CANDIDATES_OPTION.equals(option.name())
          || ADVANCED_MAX_CANDIDATES_OPTION.equals(option.name())
          || MAX_ATOMS_OPTION.equals(option.name())
          || ADVANCED_MAX_ATOMS_OPTION.equals(option.name())) {
        continue;
      }
      result.add(option);
    }
    ReactionPredictionElectronicReranker.addOrReplace(
        result, ClassicalCalculationOption.text(REACTION_ENERGY_MODEL_OPTION, "first_principles"));
    return List.copyOf(result);
  }

  private static ElectronicStructureMethod rerankMethod(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    if (ReactionPredictionElectronicReranker.namedBasisUnsupportedForReaction(
            request.basisSet(), reaction)
        && !ReactionPredictionElectronicReranker.canUseGeneratedEvenTemperedBasis(
            request, reaction)) {
      return ElectronicStructureMethod.SEMI_EMPIRICAL;
    }
    return request.method();
  }

  private static BasisSet rerankBasisSet(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    if (!ReactionPredictionElectronicReranker.namedBasisUnsupportedForReaction(
        request.basisSet(), reaction)) {
      return request.basisSet();
    }
    if (ReactionPredictionElectronicReranker.canUseGeneratedEvenTemperedBasis(request, reaction)) {
      return BasisSet.of(BasisSetName.of("AUTO-EVEN-TEMPERED-COMPACT"));
    }
    return request.basisSet();
  }

  private static boolean canUseGeneratedEvenTemperedBasis(
      final ClassicalCalculationRequest request, final Reaction reaction) {
    if (request.basisSet().hasStructuredDefinition() || request.basisSet().hasDefinitionText()) {
      return false;
    }
    if (ReactionPredictionElectronicReranker.sideAtomCount(reaction.reactants())
            + ReactionPredictionElectronicReranker.sideAtomCount(reaction.products())
        > ReactionPredictionElectronicReranker.maxAtoms(request)) {
      return false;
    }
    return ReactionPredictionElectronicReranker.compactGeneratedBasisSupported(reaction.reactants())
        && ReactionPredictionElectronicReranker.compactGeneratedBasisSupported(reaction.products());
  }

  private static boolean compactGeneratedBasisSupported(final ReactionSide side) {
    for (int i = 0; i < side.participants().size(); ++i) {
      final IntegralMolecule molecule =
          IntegralMoleculeFactory.fromMolecule(side.participants().get(i).molecule());
      final GaussianBasisSet basisSet = GaussianBasisLibrary.generatedCompactEvenTempered(molecule);
      if (!GaussianIntegralCapability.preflightFullTensor(basisSet).supported()) {
        return false;
      }
    }
    return true;
  }

  private static ElectronicStructureMethod singleMoleculeMethod(
      final ClassicalCalculationRequest request, final Molecule molecule) {
    if (ReactionPredictionElectronicReranker.namedBasisUnsupportedForMolecule(
            request.basisSet(), molecule)
        && !ReactionPredictionElectronicReranker.compactGeneratedBasisSupported(molecule)) {
      return ElectronicStructureMethod.SEMI_EMPIRICAL;
    }
    return request.method();
  }

  private static BasisSet singleMoleculeBasisSet(
      final ClassicalCalculationRequest request, final Molecule molecule) {
    if (!ReactionPredictionElectronicReranker.namedBasisUnsupportedForMolecule(
        request.basisSet(), molecule)) {
      return request.basisSet();
    }
    if (ReactionPredictionElectronicReranker.compactGeneratedBasisSupported(molecule)) {
      return BasisSet.of(BasisSetName.of("AUTO-EVEN-TEMPERED-COMPACT"));
    }
    return request.basisSet();
  }

  private static ActiveSpace workflowActiveSpace(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final BasisSet basisSet,
      final ArrayList<ClassicalCalculationOption> options) {
    if (request.hasActiveSpace()) {
      return request.activeSpace();
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet gaussianBasisSet =
        ReactionPredictionElectronicReranker.workflowBasisSet(basisSet, integralMolecule);
    if (gaussianBasisSet.orbitalCount() <= FULL_TENSOR_WORKFLOW_ORBITAL_LIMIT) {
      return null;
    }
    final int activeElectronCount = integralMolecule.electronCount() % 2 == 0 ? 2 : 1;
    final ActiveSpace activeSpace = ActiveSpace.of(activeElectronCount, 2);
    ReactionPredictionElectronicReranker.addOrReplace(
        options, ClassicalCalculationOption.text(SCF_INTEGRAL_MODE_OPTION, DIRECT_INTEGRAL_MODE));
    ReactionPredictionElectronicReranker.addOrReplace(
        options,
        ClassicalCalculationOption.text(
            SCF_REFERENCE_MODE_OPTION, CORE_HAMILTONIAN_REFERENCE_MODE));
    ReactionPredictionElectronicReranker.addOrReplace(
        options, ClassicalCalculationOption.integer(ACTIVE_SPACE_FIRST_ORBITAL_OPTION, 0));
    return activeSpace;
  }

  private static ActiveSpace workflowMinimalActiveSpace(final Molecule molecule) {
    final int electronCount = IntegralMoleculeFactory.fromMolecule(molecule).electronCount();
    return ActiveSpace.of(electronCount % 2 == 0 ? 2 : 1, 2);
  }

  private static boolean heavyWorkflowPreparation(
      final Molecule molecule, final BasisSet basisSet) {
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    return ReactionPredictionElectronicReranker.workflowBasisSet(basisSet, integralMolecule)
            .orbitalCount()
        > FULL_TENSOR_WORKFLOW_ORBITAL_LIMIT;
  }

  private static GaussianBasisSet workflowBasisSet(
      final BasisSet basisSet, final IntegralMolecule integralMolecule) {
    final String basisName = basisSet.name().value();
    if ("STO-3G".equalsIgnoreCase(basisName)) {
      return GaussianBasisLibrary.sto3g(integralMolecule);
    }
    if ("6-31G".equalsIgnoreCase(basisName)) {
      return GaussianBasisLibrary.six31g(integralMolecule);
    }
    if (GaussianBasisLibrary.generatedCompactEvenTemperedName(basisName)) {
      return GaussianBasisLibrary.generatedCompactEvenTempered(integralMolecule);
    }
    if (GaussianBasisLibrary.generatedEvenTemperedName(basisName)) {
      return GaussianBasisLibrary.generatedEvenTempered(integralMolecule);
    }
    return GaussianBasisLibrary.generatedCompactEvenTempered(integralMolecule);
  }

  private static boolean compactGeneratedBasisSupported(final Molecule molecule) {
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet =
        GaussianBasisLibrary.generatedCompactEvenTempered(integralMolecule);
    return GaussianIntegralCapability.preflightFullTensor(basisSet).supported();
  }

  private static boolean namedBasisUnsupportedForReaction(
      final BasisSet basisSet, final Reaction reaction) {
    if (basisSet.hasStructuredDefinition() || basisSet.hasDefinitionText()) {
      return false;
    }
    return ReactionPredictionElectronicReranker.namedBasisUnsupportedForSide(
            basisSet, reaction.reactants())
        || ReactionPredictionElectronicReranker.namedBasisUnsupportedForSide(
            basisSet, reaction.products());
  }

  private static boolean namedBasisUnsupportedForSide(
      final BasisSet basisSet, final ReactionSide side) {
    for (int i = 0; i < side.participants().size(); ++i) {
      if (ReactionPredictionElectronicReranker.namedBasisUnsupportedForMolecule(
          basisSet, side.participants().get(i).molecule())) {
        return true;
      }
    }
    return false;
  }

  private static boolean namedBasisUnsupportedForMolecule(
      final BasisSet basisSet, final Molecule molecule) {
    if (basisSet.hasStructuredDefinition() || basisSet.hasDefinitionText()) {
      return false;
    }
    final List<Atom> atoms = molecule.atoms();
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      if (!ReactionPredictionElectronicReranker.namedBasisSupportsAtom(
          basisSet, atoms.get(atomIndex))) {
        return true;
      }
    }
    return false;
  }

  private static boolean namedBasisSupportsAtom(final BasisSet basisSet, final Atom atom) {
    final String basisName = basisSet.name().value();
    final int atomicNumber = atom.element().atomicNumber();
    if ("STO-3G".equalsIgnoreCase(basisName)) {
      return atomicNumber >= 1 && atomicNumber <= 10;
    }
    if ("6-31G".equalsIgnoreCase(basisName)) {
      final String symbol = atom.element().symbol().value();
      return atomicNumber == 1
          || "C".equals(symbol)
          || "N".equals(symbol)
          || "O".equals(symbol)
          || "F".equals(symbol)
          || "P".equals(symbol)
          || "S".equals(symbol)
          || "Cl".equals(symbol);
    }
    return false;
  }

  private static void addOrReplace(
      final ArrayList<ClassicalCalculationOption> options,
      final ClassicalCalculationOption option) {
    for (int i = 0; i < options.size(); ++i) {
      if (!options.get(i).name().equals(option.name())) {
        continue;
      }
      options.set(i, option);
      return;
    }
    options.add(option);
  }

  private static String unavailableReason(
      final ClassicalResultSection section, final List<ClassicalDiagnostic> diagnostics) {
    if (section == null) {
      return ReactionPredictionElectronicReranker.diagnosticSummary(diagnostics);
    }
    if (!section.hasValue("method")) {
      return "result has no method field; "
          + ReactionPredictionElectronicReranker.diagnosticSummary(diagnostics);
    }
    if (!"java_native_single_point_reaction_energy".equals(section.requireFirstValue("method"))) {
      return "result method is "
          + section.requireFirstValue("method")
          + "; "
          + ReactionPredictionElectronicReranker.diagnosticSummary(diagnostics);
    }
    if (!section.hasValue("delta_gibbs")) {
      return "result has no delta_gibbs field; "
          + ReactionPredictionElectronicReranker.diagnosticSummary(diagnostics);
    }
    return ReactionPredictionElectronicReranker.diagnosticSummary(diagnostics);
  }

  private static String diagnosticSummary(final List<ClassicalDiagnostic> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return "no local diagnostic was produced";
    }
    final StringBuilder builder = new StringBuilder();
    final int limit = Math.min(3, diagnostics.size());
    for (int i = 0; i < limit; ++i) {
      if (i > 0) {
        builder.append(" | ");
      }
      builder.append(diagnostics.get(i).code().name());
      builder.append(": ");
      builder.append(diagnostics.get(i).message());
    }
    if (diagnostics.size() > limit) {
      builder.append(" | ");
      builder.append(diagnostics.size() - limit);
      builder.append(" more");
    }
    return builder.toString();
  }

  private static final class RerankedCandidate {

    private final ReactionProductCandidate candidate;
    private final List<ReactionPredictionEvidence> evidence;
    private final double weightedProbability;
    private final double multiplier;
    private final ReactionPredictionEvidence rerankEvidence;
    private final boolean hasElectronicWeight;

    private RerankedCandidate(
        final ReactionProductCandidate candidate,
        final List<ReactionPredictionEvidence> evidence,
        final double weightedProbability,
        final double multiplier,
        final ReactionPredictionEvidence rerankEvidence,
        final boolean hasElectronicWeight) {
      this.candidate = candidate;
      this.evidence = evidence;
      this.weightedProbability = weightedProbability;
      this.multiplier = multiplier;
      this.rerankEvidence = rerankEvidence;
      this.hasElectronicWeight = hasElectronicWeight;
    }

    private static RerankedCandidate unchanged(final ReactionProductCandidate candidate) {
      return new RerankedCandidate(
          candidate, candidate.evidence(), candidate.probability(), 1.0, null, false);
    }

    private static RerankedCandidate withEvidence(
        final ReactionProductCandidate candidate,
        final double multiplier,
        final ReactionPredictionEvidence evidence) {
      return RerankedCandidate.withEvidence(candidate, multiplier, List.of(evidence));
    }

    private static RerankedCandidate withEvidence(
        final ReactionProductCandidate candidate,
        final double multiplier,
        final List<ReactionPredictionEvidence> evidence) {
      final ArrayList<ReactionPredictionEvidence> updated =
          new ArrayList<ReactionPredictionEvidence>(candidate.evidence().size() + evidence.size());
      updated.addAll(candidate.evidence());
      updated.addAll(evidence);
      final ReactionPredictionEvidence lastEvidence = evidence.get(evidence.size() - 1);
      return new RerankedCandidate(
          candidate,
          List.copyOf(updated),
          candidate.probability() * multiplier,
          multiplier,
          lastEvidence,
          lastEvidence.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR);
    }
  }

  private static final class SideRegularization {

    private final ReactionSide side;
    private final boolean regularized;

    private SideRegularization(final ReactionSide side, final boolean regularized) {
      this.side = side;
      this.regularized = regularized;
    }
  }
}
