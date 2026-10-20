/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeCalibrationCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationObservation;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeDistributionDataset;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeSourceQuality;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionRegioselectivityClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

public final class ReactionOutcomeCalibrationCoverageAnalyzerTest {

  @Test
  public void builtInHeterocycleCalibrationReportsExactRowsAndBroadTaxonomyCoverage() {
    final ReactionOutcomeCalibrationCoverageReport report =
        new ReactionOutcomeCalibrationCoverageAnalyzer()
            .analyze(BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());

    Assertions.assertTrue(report.observationCount() >= 55);
    Assertions.assertTrue(report.datasetCount() >= 160);
    Assertions.assertTrue(report.exactObservationCount() > 0);
    Assertions.assertTrue(report.externallyVerifiedExactObservationCount() > 0);
    Assertions.assertTrue(
        report.exactObservationCount() > report.externallyVerifiedExactObservationCount());
    Assertions.assertTrue(report.classPriorObservationCount() > 0);
    Assertions.assertEquals(
        report.sourceTracedPriorObservationIds().size(),
        report.sourceTracedPriorObservationCount());
    Assertions.assertTrue(
        report.domainCounts().containsKey(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION));
    Assertions.assertTrue(
        report
            .mechanismCounts()
            .containsKey(ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION));
    Assertions.assertTrue(
        report
            .mechanismCounts()
            .containsKey(ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE));
    Assertions.assertTrue(
        report
            .mechanismCounts()
            .containsKey(ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION));
    Assertions.assertTrue(
        report.mechanismCounts().containsKey(ReactionMechanismFamily.ARYNE_SUBSTITUTION));
    Assertions.assertFalse(report.productionReadyForEmpiricalPrediction());
    Assertions.assertFalse(report.weakDatasetIds().isEmpty());
    Assertions.assertTrue(report.missingDomains().isEmpty());
    Assertions.assertTrue(report.missingMechanismFamilies().isEmpty());
    Assertions.assertFalse(report.unreferencedExactObservationIds().isEmpty());
    Assertions.assertFalse(report.sourceTracedPriorObservationIds().isEmpty());
  }

  @Test
  public void exactLiteratureRowsRequireSourceIdentifierAndTableReferenceForCoverage() {
    final ReactionOutcomeCalibrationObservation exactRow =
        ReactionOutcomeCalibrationObservation.of(
            "exact_table_row",
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.HEATING),
            "C10H10N2",
            "major",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("chloro_heteroarene"),
            List.of("snar", "major"),
            0.80,
            80.0,
            0.95,
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            "doi:10.example/heterocycle-distribution",
            "table_2_entry_4",
            true,
            "Synthetic exact product-distribution table used to verify source metadata handling.",
            "Exact row has DOI-like identifier, table reference and row flag.");
    final ReactionOutcomeDistributionDataset dataset =
        ReactionOutcomeDistributionDataset.of(
            "exact_dataset",
            "Exact heterocycle product distribution",
            "Synthetic source metadata regression dataset.",
            ReactionOutcomeSourceQuality.EXACT_EXPERIMENTAL_PRODUCT_DISTRIBUTION,
            List.of(exactRow));
    final ReactionOutcomeCalibrationCoverageReport report =
        new ReactionOutcomeCalibrationCoverageAnalyzer()
            .analyze(ReactionOutcomeCalibrationLibrary.ofDatasets(List.of(dataset)));

    Assertions.assertEquals(1, report.exactObservationCount());
    Assertions.assertEquals(1, report.externallyVerifiedExactObservationCount());
    Assertions.assertEquals(1, report.sourceIdentifiedExactObservationCount());
    Assertions.assertTrue(report.unreferencedExactObservationIds().isEmpty());
  }

  @Test
  public void localBenchmarksDoNotProveExternalEmpiricalReadiness() {
    final ReactionOutcomeCalibrationObservation localRow =
        ReactionOutcomeCalibrationObservation.of(
            "local_table_row",
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            List.of(ReactionConditionFeatureKind.HEATING),
            "C10H10N2",
            "local_major",
            ReactionRegioselectivityClass.LEAVING_GROUP_DIRECTED,
            List.of("chloro_heteroarene"),
            List.of("snar", "major"),
            0.80,
            80.0,
            0.95,
            ReactionOutcomeSourceQuality.LOCAL_BENCHMARK,
            "local:manual-benchmark",
            "local_case_1",
            true,
            "Local development benchmark.",
            "Local benchmark is exact for regression, but not external literature evidence.");
    final ReactionOutcomeDistributionDataset dataset =
        ReactionOutcomeDistributionDataset.of(
            "local_dataset",
            "Local heterocycle product distribution",
            "Local benchmark metadata regression dataset.",
            ReactionOutcomeSourceQuality.LOCAL_BENCHMARK,
            List.of(localRow));
    final ReactionOutcomeCalibrationCoverageReport report =
        new ReactionOutcomeCalibrationCoverageAnalyzer()
            .analyze(ReactionOutcomeCalibrationLibrary.ofDatasets(List.of(dataset)));

    Assertions.assertEquals(1, report.exactObservationCount());
    Assertions.assertEquals(0, report.externallyVerifiedExactObservationCount());
    Assertions.assertFalse(report.productionReadyForEmpiricalPrediction());
  }

  @Test
  public void sourceTracedLiteraturePriorDoesNotCountAsExactCalibration() {
    final ReactionOutcomeCalibrationObservation prior =
        ReactionOutcomeCalibrationObservation.of(
            "source_traced_prior",
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
            List.of(ReactionConditionFeatureKind.HEATING),
            "quinoline_variable_formula",
            "regioselectivity_prior",
            ReactionRegioselectivityClass.COMPETING_REGIOISOMERS,
            List.of("aniline", "diketone"),
            List.of("quinoline", "regioisomer"),
            0.55,
            55.0,
            0.60,
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            "doi:10.1002/poc.1433",
            "source description without exact table row",
            false,
            "Source-traced literature prior.",
            "A DOI-backed prior is useful evidence, but not an exact product-distribution row.");
    final ReactionOutcomeDistributionDataset dataset =
        ReactionOutcomeDistributionDataset.of(
            "source_traced_prior_dataset",
            "Source traced non-exact prior",
            "Synthetic prior metadata regression dataset.",
            ReactionOutcomeSourceQuality.LITERATURE_REVIEW_CLASS_PRIOR,
            List.of(prior));
    final ReactionOutcomeCalibrationCoverageReport report =
        new ReactionOutcomeCalibrationCoverageAnalyzer()
            .analyze(ReactionOutcomeCalibrationLibrary.ofDatasets(List.of(dataset)));

    Assertions.assertEquals(0, report.exactObservationCount());
    Assertions.assertEquals(0, report.externallyVerifiedExactObservationCount());
    Assertions.assertEquals(0, report.sourceIdentifiedExactObservationCount());
    Assertions.assertEquals(1, report.sourceTracedPriorObservationCount());
    Assertions.assertEquals(
        List.of("source_traced_prior"), report.sourceTracedPriorObservationIds());
    Assertions.assertFalse(report.productionReadyForEmpiricalPrediction());
  }
}
