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
import java.util.Comparator;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.GeneratedReactionCandidateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreBreakdown;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponentKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;

/**
 * Оркестрирует генерацию, scoring и нормализацию кандидатов органических продуктов.
 */
public final class OrganicReactionPredictionService {

  private final OrganicReactionCandidateGenerator candidateGenerator;
  private final OrganicReactionCandidateScorer candidateScorer;
  private final ReactionCandidateEnsembleNormalizer ensembleNormalizer;
  private final ReactionPredictionRequestAuditor requestAuditor;
  private final MolecularFeatureAnalyzer featureAnalyzer;

  public OrganicReactionPredictionService() {
    this(new MolecularFeatureAnalyzer(), RuleBasedOrganicReactionCandidateGenerator.EMPTY);
  }

  public OrganicReactionPredictionService(final MolecularFeatureAnalyzer featureAnalyzer) {
    this(featureAnalyzer, RuleBasedOrganicReactionCandidateGenerator.EMPTY);
  }

  public OrganicReactionPredictionService(
      final MolecularFeatureAnalyzer featureAnalyzer,
      final OrganicReactionCandidateGenerator candidateGenerator) {
    this(featureAnalyzer, candidateGenerator, new OrganicReactionCandidateScorer(featureAnalyzer));
  }

  public OrganicReactionPredictionService(
      final MolecularFeatureAnalyzer featureAnalyzer,
      final OrganicReactionCandidateGenerator candidateGenerator,
      final OrganicReactionCandidateScorer candidateScorer) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Molecular feature analyzer must not be null.");
    }
    if (candidateGenerator == null) {
      throw new IllegalArgumentException("Organic reaction candidate generator must not be null.");
    }
    if (candidateScorer == null) {
      throw new IllegalArgumentException("Organic reaction candidate scorer must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
    this.candidateGenerator = candidateGenerator;
    this.candidateScorer = candidateScorer;
    this.ensembleNormalizer = new ReactionCandidateEnsembleNormalizer();
    this.requestAuditor = new ReactionPredictionRequestAuditor(featureAnalyzer);
  }

  public ReactionPredictionResult predict(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Organic reaction prediction request must not be null.");
    }
    final ReactionPredictionRequest effectiveRequest = this.domainEnrichedRequest(request);
    final List<ReactionPredictionEvidence> requestDiagnostics =
        this.requestAuditor.audit(effectiveRequest);
    final GeneratedReactionCandidateSet generatedCandidates =
        this.candidateGenerator.generate(effectiveRequest);
    final CandidateMergeResult mergedCandidates =
        OrganicReactionPredictionService.mergeCandidates(
            effectiveRequest.seedCandidates(), generatedCandidates.candidates());
    final List<ReactionProductCandidate> candidates = mergedCandidates.candidates();
    if (candidates.isEmpty()) {
      return ReactionPredictionResult.of(
          effectiveRequest,
          ReactionPredictionMethodType.RULE_BASED,
          List.of(),
          generatedCandidates.mechanismChannels(),
          OrganicReactionPredictionService.emptyCandidateDiagnostics(
              generatedCandidates, requestDiagnostics, mergedCandidates.diagnostics()));
    }
    final ArrayList<ReactionCandidateScore> scoredCandidates =
        new ArrayList<ReactionCandidateScore>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      scoredCandidates.add(this.candidateScorer.score(effectiveRequest, candidates.get(i), i));
    }
    final ArrayList<ReactionCandidateScore> ensembleCandidates =
        new ArrayList<ReactionCandidateScore>(this.ensembleNormalizer.adjust(scoredCandidates));
    ensembleCandidates.sort(Comparator.comparingDouble(ReactionCandidateScore::score).reversed());
    return ReactionPredictionResult.of(
        effectiveRequest,
        ReactionPredictionMethodType.HYBRID,
        OrganicReactionPredictionService.normalizedCandidates(ensembleCandidates),
        generatedCandidates.mechanismChannels(),
        OrganicReactionPredictionService.mergeDiagnostics(
            OrganicReactionPredictionService.mergeDiagnostics(
                requestDiagnostics, generatedCandidates.diagnostics()),
            mergedCandidates.diagnostics()));
  }

  private ReactionPredictionRequest domainEnrichedRequest(final ReactionPredictionRequest request) {
    if (!(request.domains().size() == 1 && request.hasDomain(OrganicReactionDomain.UNKNOWN))) {
      return request;
    }
    final ArrayList<OrganicReactionDomain> domains = new ArrayList<OrganicReactionDomain>();
    this.addStructureInferredDomains(request, domains);
    OrganicReactionPredictionService.addConditionInferredDomains(request.conditions(), domains);
    if (domains.isEmpty()) {
      return request;
    }
    return ReactionPredictionRequest.of(
        request.id(),
        request.reactants(),
        request.conditions(),
        domains,
        request.seedCandidates(),
        request.note());
  }

  private void addStructureInferredDomains(
      final ReactionPredictionRequest request, final List<OrganicReactionDomain> domains) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    final boolean crossCouplingPartnerPresent =
        this.reactantsContainAnyFeature(
            request,
            MolecularFeatureKind.BORONIC_ACID_OR_BORONATE,
            MolecularFeatureKind.ORGANOMETALLIC_REAGENT);
    final boolean crossCouplingConditionPresent =
        OrganicReactionPredictionService.crossCouplingConditionSignal(request.conditions());
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      if (features.contains(MolecularFeatureKind.HETEROCYCLE)
          || features.contains(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.OXYGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.SULFUR_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.ELECTRON_POOR_HETEROARENE)) {
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.HETEROCYCLE_SYNTHESIS);
      }
      if (features.contains(MolecularFeatureKind.HETEROARYL_HALIDE)
          || features.contains(MolecularFeatureKind.ARYL_HALIDE)) {
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
        if (crossCouplingPartnerPresent || crossCouplingConditionPresent) {
          OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.CROSS_COUPLING);
        }
      }
      if (features.contains(MolecularFeatureKind.AMINE)
          || features.contains(MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE)
          || features.contains(MolecularFeatureKind.ACTIVATED_NUCLEOPHILE)
          || features.contains(MolecularFeatureKind.HYDROXYL_OR_ALKOXY)
          || features.contains(MolecularFeatureKind.THIOL_OR_THIOETHER)) {
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
        OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.ADDITION);
      }
      if (features.contains(MolecularFeatureKind.CARBONYL)
          || features.contains(MolecularFeatureKind.ALDEHYDE)
          || features.contains(MolecularFeatureKind.KETONE)
          || features.contains(MolecularFeatureKind.IMINE)
          || features.contains(MolecularFeatureKind.NITRILE)
          || features.contains(MolecularFeatureKind.EPOXIDE)
          || features.contains(MolecularFeatureKind.ALKENE)
          || features.contains(MolecularFeatureKind.ALKYNE)) {
        OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.ADDITION);
      }
      if (features.contains(MolecularFeatureKind.CARBOXYL_DERIVATIVE)
          || features.contains(MolecularFeatureKind.ACYL_HALIDE)
          || features.contains(MolecularFeatureKind.ANHYDRIDE)
          || features.contains(MolecularFeatureKind.ESTER)
          || features.contains(MolecularFeatureKind.AMIDE)) {
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
      }
      if (features.contains(MolecularFeatureKind.NITRO)
          || features.contains(MolecularFeatureKind.SULFUR_OXIDE)) {
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.OXIDATION_REDUCTION);
      }
      if (features.contains(MolecularFeatureKind.RADICAL_CENTER)) {
        OrganicReactionPredictionService.addDomain(
            domains, OrganicReactionDomain.RADICAL_FUNCTIONALIZATION);
      }
    }
  }

  private boolean reactantsContainAnyFeature(
      final ReactionPredictionRequest request, final MolecularFeatureKind... featureKinds) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      for (int featureIndex = 0; featureIndex < featureKinds.length; ++featureIndex) {
        if (features.contains(featureKinds[featureIndex])) {
          return true;
        }
      }
    }
    return false;
  }

  private static void addConditionInferredDomains(
      final ReactionConditions conditions, final List<OrganicReactionDomain> domains) {
    if (OrganicReactionPredictionService.crossCouplingConditionSignal(conditions)) {
      OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.CROSS_COUPLING);
      OrganicReactionPredictionService.addDomain(
          domains, OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || OrganicReactionPredictionService.hasComponentRole(conditions, ReactionComponentRole.ACID)
        || OrganicReactionPredictionService.hasComponentRole(conditions, ReactionComponentRole.BASE)
        || OrganicReactionPredictionService.conditionTextContains(
            conditions,
            "acid",
            "base",
            "hcl",
            "tfa",
            "koh",
            "naoh",
            "k2co3",
            "dipea",
            "triethylamine")) {
      OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.ACID_BASE);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)
        || conditions.hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM)
        || conditions.hasFeature(ReactionConditionFeatureKind.HYDROGENATION)
        || conditions.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)
        || OrganicReactionPredictionService.conditionTextContains(
            conditions,
            "oxid",
            "reduc",
            "hydrogen",
            "h2",
            "nabh4",
            "lialh4",
            "mcpba",
            "peroxide",
            "electro")) {
      OrganicReactionPredictionService.addDomain(
          domains, OrganicReactionDomain.OXIDATION_REDUCTION);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || OrganicReactionPredictionService.conditionTextContains(
            conditions, "photo", "uv", "led", "irradiation", "radical", "aibn")) {
      OrganicReactionPredictionService.addDomain(
          domains, OrganicReactionDomain.RADICAL_FUNCTIONALIZATION);
      OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.REARRANGEMENT);
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.DEHYDRATING_AGENT)
        || conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        || conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)
        || OrganicReactionPredictionService.heated(conditions)
        || OrganicReactionPredictionService.conditionTextContains(
            conditions, "heat", "heated", "reflux", "dehydrat", "molecular sieve", "socl2")) {
      OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.ELIMINATION);
      OrganicReactionPredictionService.addDomain(domains, OrganicReactionDomain.CYCLIZATION);
    }
    if (OrganicReactionPredictionService.conditionTextContains(
        conditions, "peg", "dmf", "dmso", "dmac", "nmp", "ionic liquid", "polar aprotic")) {
      OrganicReactionPredictionService.addDomain(
          domains, OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION);
    }
  }

  private static boolean hasComponentRole(
      final ReactionConditions conditions, final ReactionComponentRole role) {
    return !conditions.componentsByRole(role).isEmpty();
  }

  private static boolean crossCouplingConditionSignal(final ReactionConditions conditions) {
    return conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        || OrganicReactionPredictionService.hasComponentRole(
            conditions, ReactionComponentRole.CATALYST)
        || OrganicReactionPredictionService.hasComponentRole(
            conditions, ReactionComponentRole.LIGAND)
        || OrganicReactionPredictionService.conditionTextContains(
            conditions,
            "pd",
            "palladium",
            "nickel",
            "copper",
            "ligand",
            "phosphine",
            "xphos",
            "sphos",
            "buchwald",
            "suzuki",
            "heck",
            "sonogashira",
            "kumada",
            "negishi",
            "stille");
  }

  private static boolean heated(final ReactionConditions conditions) {
    return conditions.temperature() != null && conditions.temperature().kelvin() >= 323.15;
  }

  private static void addDomain(
      final List<OrganicReactionDomain> domains, final OrganicReactionDomain domain) {
    if (!domains.contains(domain)) {
      domains.add(domain);
    }
  }

  private static boolean conditionTextContains(
      final ReactionConditions conditions, final String... fragments) {
    final String text = OrganicReactionPredictionService.conditionSearchText(conditions);
    for (int i = 0; i < fragments.length; ++i) {
      if (text.contains(fragments[i])) {
        return true;
      }
    }
    return false;
  }

  private static String conditionSearchText(final ReactionConditions conditions) {
    final StringBuilder text = new StringBuilder();
    if (conditions.note() != null) {
      text.append(conditions.note()).append(' ');
    }
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      text.append(component.role().name()).append(' ');
      text.append(component.name()).append(' ');
      if (component.hasNote()) {
        text.append(component.note()).append(' ');
      }
    }
    return text.toString().toLowerCase(java.util.Locale.ROOT);
  }

  private static CandidateMergeResult mergeCandidates(
      final List<ReactionProductCandidate> first, final List<ReactionProductCandidate> second) {
    if (first.isEmpty() && second.isEmpty()) {
      return CandidateMergeResult.empty();
    }
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>(first.size() + second.size());
    OrganicReactionPredictionService.addMergedCandidates(candidates, first, true);
    OrganicReactionPredictionService.addMergedCandidates(candidates, second, false);
    return OrganicReactionPredictionService.withoutFallbackSurrogates(candidates);
  }

  private static CandidateMergeResult withoutFallbackSurrogates(
      final List<ReactionProductCandidate> candidates) {
    final ArrayList<ReactionProductCandidate> filtered =
        new ArrayList<ReactionProductCandidate>(candidates.size());
    final ArrayList<ReactionPredictionEvidence> diagnostics =
        new ArrayList<ReactionPredictionEvidence>();
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (!OrganicReactionPredictionService.surrogateProductCandidate(candidate)) {
        filtered.add(candidate);
        continue;
      }
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Surrogate product candidate was removed from prediction output because "
                  + "it is not an explicit molecular-graph product. Mechanism-only hypotheses from "
                  + "matched declarative rules are still retained and penalized separately, while this "
                  + "candidate is only a site-local or mechanism-only placeholder.",
              -0.12));
    }
    if (diagnostics.isEmpty()) {
      return CandidateMergeResult.of(candidates, List.of());
    }
    if (filtered.isEmpty()) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Only surrogate or mechanism-only products were generated; they were removed because "
                  + "they are not explicit molecular-graph products.",
              -0.12));
      return CandidateMergeResult.of(filtered, diagnostics);
    }
    return CandidateMergeResult.of(filtered, diagnostics);
  }

  private static final class CandidateMergeResult {

    private final List<ReactionProductCandidate> candidates;
    private final List<ReactionPredictionEvidence> diagnostics;

    private CandidateMergeResult(
        final List<ReactionProductCandidate> candidates,
        final List<ReactionPredictionEvidence> diagnostics) {
      this.candidates = candidates;
      this.diagnostics = diagnostics;
    }

    static CandidateMergeResult empty() {
      return CandidateMergeResult.of(List.of(), List.of());
    }

    static CandidateMergeResult of(
        final List<ReactionProductCandidate> candidates,
        final List<ReactionPredictionEvidence> diagnostics) {
      return new CandidateMergeResult(List.copyOf(candidates), List.copyOf(diagnostics));
    }

    List<ReactionProductCandidate> candidates() {
      return this.candidates;
    }

    List<ReactionPredictionEvidence> diagnostics() {
      return this.diagnostics;
    }
  }

  private static boolean surrogateProductCandidate(final ReactionProductCandidate candidate) {
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).kind() == ReactionPredictionEvidenceKind.SURROGATE_PRODUCT) {
        return true;
      }
    }
    return false;
  }

  private static void addMergedCandidates(
      final ArrayList<ReactionProductCandidate> candidates,
      final List<ReactionProductCandidate> incoming,
      final boolean userSeedSource) {
    for (int i = 0; i < incoming.size(); ++i) {
      final ReactionProductCandidate incomingCandidate = incoming.get(i);
      if (incomingCandidate == null) {
        throw new IllegalArgumentException("Reaction product candidate must not be null.");
      }
      final ReactionProductCandidate candidate =
          OrganicReactionPredictionService.withSourceEvidence(
              OrganicReactionPredictionService.withoutBenchmarkEvidence(incomingCandidate),
              userSeedSource);
      final int existingIndex =
          OrganicReactionPredictionService.indexOfSameProductSide(candidates, candidate);
      if (existingIndex < 0) {
        candidates.add(candidate);
        continue;
      }
      candidates.set(
          existingIndex,
          OrganicReactionPredictionService.mergeSameProductCandidates(
              candidates.get(existingIndex), candidate));
    }
  }

  private static ReactionProductCandidate withSourceEvidence(
      final ReactionProductCandidate candidate, final boolean userSeedSource) {
    if (!userSeedSource || candidate.hasEvidence(ReactionPredictionEvidenceKind.USER_SEED)) {
      return candidate;
    }
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(candidate.evidence().size() + 1);
    evidence.addAll(candidate.evidence());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_SEED,
            "User supplied product-side seed candidate; this is an explicit hypothesis, not a generated graph transform.",
            0.0));
    return ReactionProductCandidate.of(
        candidate.products(),
        candidate.probability(),
        candidate.expectedYieldPercent(),
        candidate.selectivityNote(),
        evidence,
        candidate.predictedMechanisms(),
        candidate.scoreBreakdown());
  }

  private static int indexOfSameProductSide(
      final List<ReactionProductCandidate> candidates, final ReactionProductCandidate candidate) {
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).products().sameParticipantsAs(candidate.products())) {
        return i;
      }
    }
    return -1;
  }

  private static ReactionProductCandidate mergeSameProductCandidates(
      final ReactionProductCandidate first, final ReactionProductCandidate second) {
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(
            first.evidence().size() + second.evidence().size() + 1);
    OrganicReactionPredictionService.addUniqueEvidence(evidence, first.evidence());
    OrganicReactionPredictionService.addUniqueEvidence(evidence, second.evidence());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Merged duplicate product channel from multiple reaction rules or seed candidates.",
            0.04));
    final ArrayList<PredictedReactionMechanism> mechanisms =
        new ArrayList<PredictedReactionMechanism>(
            first.predictedMechanismCount() + second.predictedMechanismCount());
    OrganicReactionPredictionService.addUniqueMechanisms(mechanisms, first.predictedMechanisms());
    OrganicReactionPredictionService.addUniqueMechanisms(mechanisms, second.predictedMechanisms());
    return ReactionProductCandidate.of(
        first.products(),
        Math.min(1.0, first.probability() + second.probability()),
        Math.min(100.0, Math.max(first.expectedYieldPercent(), second.expectedYieldPercent())),
        OrganicReactionPredictionService.mergeSelectivityNotes(first, second),
        evidence,
        mechanisms);
  }

  private static ReactionProductCandidate withoutBenchmarkEvidence(
      final ReactionProductCandidate candidate) {
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    boolean containsBenchmarkEvidence = false;
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i).kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR) {
        containsBenchmarkEvidence = true;
        break;
      }
    }
    if (!containsBenchmarkEvidence) {
      return candidate;
    }
    final ArrayList<ReactionPredictionEvidence> filtered =
        new ArrayList<ReactionPredictionEvidence>(evidence.size());
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (item.kind() != ReactionPredictionEvidenceKind.CALIBRATION_PRIOR) {
        filtered.add(item);
      }
    }
    return ReactionProductCandidate.of(
        candidate.products(),
        candidate.probability(),
        candidate.expectedYieldPercent(),
        candidate.selectivityNote(),
        filtered,
        candidate.predictedMechanisms());
  }

  private static void addUniqueEvidence(
      final ArrayList<ReactionPredictionEvidence> target,
      final List<ReactionPredictionEvidence> source) {
    for (int i = 0; i < source.size(); ++i) {
      final ReactionPredictionEvidence item = source.get(i);
      if (!target.contains(item)) {
        target.add(item);
      }
    }
  }

  private static void addUniqueMechanisms(
      final ArrayList<PredictedReactionMechanism> target,
      final List<PredictedReactionMechanism> source) {
    for (int i = 0; i < source.size(); ++i) {
      final PredictedReactionMechanism mechanism = source.get(i);
      if (!target.contains(mechanism)) {
        target.add(mechanism);
      }
    }
  }

  private static String mergeSelectivityNotes(
      final ReactionProductCandidate first, final ReactionProductCandidate second) {
    if (!first.hasSelectivityNote()) {
      return second.selectivityNote();
    }
    if (!second.hasSelectivityNote() || first.selectivityNote().equals(second.selectivityNote())) {
      return first.selectivityNote();
    }
    return first.selectivityNote() + " | " + second.selectivityNote();
  }

  private static List<ReactionPredictionEvidence> emptyCandidateDiagnostics(
      final GeneratedReactionCandidateSet generatedCandidates,
      final List<ReactionPredictionEvidence> requestDiagnostics,
      final List<ReactionPredictionEvidence> mergeDiagnostics) {
    final ArrayList<ReactionPredictionEvidence> diagnostics =
        new ArrayList<ReactionPredictionEvidence>(
            requestDiagnostics.size()
                + generatedCandidates.diagnostics().size()
                + mergeDiagnostics.size()
                + 1);
    diagnostics.addAll(requestDiagnostics);
    diagnostics.addAll(generatedCandidates.diagnostics());
    diagnostics.addAll(mergeDiagnostics);
    diagnostics.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.WARNING,
            "No product candidates were supplied by rule library or user input.",
            -1.0));
    return List.copyOf(diagnostics);
  }

  private static List<ReactionPredictionEvidence> mergeDiagnostics(
      final List<ReactionPredictionEvidence> first, final List<ReactionPredictionEvidence> second) {
    if (first.isEmpty()) {
      return second;
    }
    if (second.isEmpty()) {
      return first;
    }
    final ArrayList<ReactionPredictionEvidence> diagnostics =
        new ArrayList<ReactionPredictionEvidence>(first.size() + second.size());
    diagnostics.addAll(first);
    diagnostics.addAll(second);
    return List.copyOf(diagnostics);
  }

  private static List<ReactionProductCandidate> normalizedCandidates(
      final List<ReactionCandidateScore> scoredCandidates) {
    double total = 0.0;
    for (int i = 0; i < scoredCandidates.size(); ++i) {
      total += scoredCandidates.get(i).score();
    }
    final boolean uniformFallback = !Double.isFinite(total) || total <= 0.0;
    final ArrayList<ReactionProductCandidate> result =
        new ArrayList<ReactionProductCandidate>(scoredCandidates.size());
    for (int i = 0; i < scoredCandidates.size(); ++i) {
      final ReactionCandidateScore scoredCandidate = scoredCandidates.get(i);
      final ReactionProductCandidate candidate = scoredCandidate.candidate();
      final double probability =
          uniformFallback ? 1.0 / scoredCandidates.size() : scoredCandidate.score() / total;
      result.add(
          ReactionProductCandidate.of(
              candidate.products(),
              probability,
              OrganicReactionPredictionService.normalizedExpectedYield(candidate, probability),
              candidate.selectivityNote(),
              scoredCandidate.evidence(),
              candidate.predictedMechanisms(),
              ReactionPredictionScoreBreakdown.of(
                  scoredCandidate.score(),
                  probability,
                  OrganicReactionPredictionService.scoreComponents(scoredCandidate))));
    }
    return List.copyOf(result);
  }

  private static double normalizedExpectedYield(
      final ReactionProductCandidate candidate, final double normalizedProbability) {
    final double intrinsicYield = candidate.expectedYieldPercent() / 100.0;
    final double boundedYield = Math.max(0.0, Math.min(1.0, intrinsicYield));
    return 100.0 * normalizedProbability * boundedYield;
  }

  private static List<ReactionPredictionScoreComponent> scoreComponents(
      final ReactionCandidateScore score) {
    final List<ReactionPredictionEvidence> evidence = score.evidence();
    final ArrayList<ReactionPredictionScoreComponent> components =
        new ArrayList<ReactionPredictionScoreComponent>(evidence.size() + 2);
    components.add(
        ReactionPredictionScoreComponent.of(
            ReactionPredictionScoreComponentKind.USER_PRIOR,
            "Source candidate prior probability before rule-based normalization.",
            score.candidate().probability()));
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      components.add(
          ReactionPredictionScoreComponent.of(
              OrganicReactionPredictionService.scoreComponentKind(item),
              item.description(),
              OrganicReactionCandidateScorer.predictionWeight(item)));
    }
    if (score.sourceIndex() > 0) {
      components.add(
          ReactionPredictionScoreComponent.of(
              ReactionPredictionScoreComponentKind.TIE_BREAKER,
              "Stable deterministic ordering penalty for equal-score candidates.",
              -score.sourceIndex() * 1.0E-12));
    }
    return List.copyOf(components);
  }

  private static ReactionPredictionScoreComponentKind scoreComponentKind(
      final ReactionPredictionEvidence evidence) {
    final ReactionPredictionEvidenceKind kind = evidence.kind();
    if (OrganicReactionPredictionService.mechanismConditionEvidence(evidence)) {
      return ReactionPredictionScoreComponentKind.CONDITION_MATCH;
    }
    if (OrganicReactionPredictionService.stoichiometryEvidence(evidence)) {
      return ReactionPredictionScoreComponentKind.STOICHIOMETRY;
    }
    if (OrganicReactionPredictionService.selectivityEvidence(evidence)) {
      return ReactionPredictionScoreComponentKind.SELECTIVITY;
    }
    switch (kind) {
      case FUNCTIONAL_GROUP_MATCH:
        return ReactionPredictionScoreComponentKind.FUNCTIONAL_GROUP;
      case HETEROCYCLE_PATTERN:
        return ReactionPredictionScoreComponentKind.HETEROCYCLE;
      case STERIC_FACTOR:
        return ReactionPredictionScoreComponentKind.STERIC;
      case ELECTRONIC_FACTOR:
        return ReactionPredictionScoreComponentKind.ELECTRONIC;
      case SOLVENT_EFFECT:
        return ReactionPredictionScoreComponentKind.SOLVENT;
      case TEMPERATURE_EFFECT:
        return ReactionPredictionScoreComponentKind.TEMPERATURE;
      case CATALYST_EFFECT:
        return ReactionPredictionScoreComponentKind.CATALYST;
      case KINETIC_FACTOR:
        return ReactionPredictionScoreComponentKind.KINETIC;
      case THERMODYNAMIC_FACTOR:
        return ReactionPredictionScoreComponentKind.THERMODYNAMIC;
      case COMPETING_PATHWAY:
        return ReactionPredictionScoreComponentKind.WARNING_PENALTY;
      case QUANTUM_ENERGY_MODEL:
        return ReactionPredictionScoreComponentKind.QUANTUM_MODEL;
      case CALIBRATION_PRIOR:
        return ReactionPredictionScoreComponentKind.CALIBRATION;
      case USER_RULE:
        return ReactionPredictionScoreComponentKind.MECHANISM_MATCH;
      case USER_SEED:
        return ReactionPredictionScoreComponentKind.USER_PRIOR;
      case SURROGATE_PRODUCT:
        return ReactionPredictionScoreComponentKind.WARNING_PENALTY;
      case WARNING:
        return ReactionPredictionScoreComponentKind.WARNING_PENALTY;
      default:
        throw new IllegalArgumentException("Unsupported reaction prediction evidence kind.");
    }
  }

  private static boolean mechanismConditionEvidence(final ReactionPredictionEvidence evidence) {
    return evidence
        .description()
        .toLowerCase(java.util.Locale.ROOT)
        .startsWith("condition mechanism profile ");
  }

  private static boolean stoichiometryEvidence(final ReactionPredictionEvidence evidence) {
    final String description = evidence.description().toLowerCase(java.util.Locale.ROOT);
    return description.contains("quantified")
        || description.contains("stoichiometric")
        || description.contains("equivalent")
        || description.contains("gram-scale")
        || description.contains("limiting-reagent")
        || description.contains("mass-transfer");
  }

  private static boolean selectivityEvidence(final ReactionPredictionEvidence evidence) {
    final String description = evidence.description().toLowerCase(java.util.Locale.ROOT);
    return description.contains("product distribution")
        || description.contains("selectivity")
        || description.contains("regioisomer")
        || description.contains("over-substitution")
        || description.contains("side-product probability")
        || description.contains("competing product")
        || description.contains("side reaction")
        || description.contains("orthogonal")
        || description.contains("unchanged starting material")
        || description.contains("single looked-up product");
  }
}
