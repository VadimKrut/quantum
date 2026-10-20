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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionMechanismCatalogCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismCatalogCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionRuleLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

public final class ReactionMechanismCatalogCoverageAnalyzerTest {

  @Test
  public void builtInCatalogCoversEveryDeclaredFamilyAndEventKind() {
    final ReactionMechanismCatalogCoverageReport report =
        new ReactionMechanismCatalogCoverageAnalyzer()
            .analyze(BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());

    Assertions.assertTrue(report.taxonomyComplete(), report.missingFamilies().toString());
    Assertions.assertTrue(report.patternCount() >= report.familyCount());
    Assertions.assertTrue(report.eventKindCount() > 100);
    Assertions.assertTrue(report.missingFamilies().isEmpty());
    Assertions.assertTrue(report.duplicateFamilies().isEmpty());
    Assertions.assertTrue(report.unusedEventKinds().isEmpty());
    Assertions.assertTrue(report.syntheticGenericPatternIds().isEmpty());
    Assertions.assertTrue(report.underSpecifiedPatternIds().isEmpty());
    Assertions.assertEquals(1.0, report.chemicallyUsableCoverageRatio(), 1.0E-12);
    Assertions.assertEquals(1.0, report.productionCoverageRatio(), 1.0E-12);
    Assertions.assertTrue(report.productionDetailedComplete());
  }

  @Test
  public void builtInCatalogFindsEveryDeclaredFamilyThroughIndexedLookup() {
    final ReactionMechanismPatternCatalog catalog =
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry();
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      Assertions.assertNotNull(catalog.find(families[i]), families[i].name());
    }
  }

  @Test
  public void catalogRejectsDuplicateMechanismFamilies() {
    final ReactionMechanismPattern pattern =
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry().patterns().get(0);

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ReactionMechanismPatternCatalog.of(java.util.List.of(pattern, pattern)));
  }

  @Test
  public void productRulesExposeSpecificMechanismFamiliesWhenIdentityIsSpecific() {
    Assertions.assertEquals(
        ReactionMechanismFamily.SUZUKI_COUPLING,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "replace_heteroaryl_bromide_suzuki_carbon"));
    Assertions.assertEquals(
        ReactionMechanismFamily.NITRATION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily("transform_aromatic_nitration"));
    Assertions.assertEquals(
        ReactionMechanismFamily.REDUCTIVE_AMINATION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_carbonyl_reductive_amination"));
    Assertions.assertEquals(
        ReactionMechanismFamily.TRANSESTERIFICATION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_ester_transesterification"));
    Assertions.assertEquals(
        ReactionMechanismFamily.HANTZSCH_SYNTHESIS,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_hantzsch_like_pyridine_construction"));
    Assertions.assertEquals(
        ReactionMechanismFamily.BIGINELLI_REACTION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_biginelli_dihydropyrimidinone_construction"));
    Assertions.assertEquals(
        ReactionMechanismFamily.SKRAUP_SYNTHESIS,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_skraup_quinoline_construction"));
    Assertions.assertEquals(
        ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_combes_quinoline_annulation"));
    // Mechanism documentation does not imply a supported product graph without a structural H
    // donor.
    Assertions.assertNotNull(
        BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry()
            .find(ReactionMechanismFamily.HYDRIDE_REDUCTION));
    Assertions.assertTrue(
        BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction().rules().stream()
            .noneMatch(
                rule ->
                    java.util.Set.of(
                            "transform_carbonyl_hydride_reduction",
                            "bond_addition_carbonyl_hydride_reduction")
                        .contains(rule.id().value())));
    Assertions.assertEquals(
        ReactionMechanismFamily.CANNIZZARO_REACTION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_carbonyl_cannizzaro_disproportionation"));
    Assertions.assertEquals(
        ReactionMechanismFamily.ARYNE_SUBSTITUTION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily("transform_aryne_substitution"));
    Assertions.assertEquals(
        ReactionMechanismFamily.OLEFIN_METATHESIS,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily("transform_olefin_metathesis"));
    Assertions.assertEquals(
        ReactionMechanismFamily.RING_CLOSING_METATHESIS,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_ring_closing_metathesis"));
    Assertions.assertEquals(
        ReactionMechanismFamily.THIOL_ENE_CLICK,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily("transform_thiol_ene_click"));
    Assertions.assertEquals(
        ReactionMechanismFamily.STRECKER_SYNTHESIS,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_strecker_aminonitrile_synthesis"));
    Assertions.assertEquals(
        ReactionMechanismFamily.UGI_REACTION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_ugi_multicomponent_amide"));
    Assertions.assertEquals(
        ReactionMechanismFamily.PASSERINI_REACTION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_passerini_multicomponent_ester"));
    Assertions.assertEquals(
        ReactionMechanismFamily.ROBINSON_ANNULATION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily("transform_robinson_annulation"));
    Assertions.assertEquals(
        ReactionMechanismFamily.NAZAROV_CYCLIZATION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily("transform_nazarov_cyclization"));
    Assertions.assertEquals(
        ReactionMechanismFamily.PAUSON_KHAND_REACTION,
        ReactionMechanismCatalogCoverageAnalyzerTest.ruleFamily(
            "transform_pauson_khand_cyclopentenone"));
  }

  private static ReactionMechanismFamily ruleFamily(final String ruleId) {
    final java.util.List<OrganicReactionRule> rules =
        BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction().rules();
    for (int i = 0; i < rules.size(); ++i) {
      final OrganicReactionRule rule = rules.get(i);
      if (rule.id().value().equals(ruleId)) {
        return rule.mechanismFamily();
      }
    }
    throw new IllegalArgumentException("Missing built-in organic reaction rule: " + ruleId + ".");
  }
}
