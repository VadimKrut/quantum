/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalCalculationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationMode;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPreflight;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceSelectionOptions;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceSelectionResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceSelector;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.ElectronicStructureFiniteDifferenceEnergyModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryCoordinateSupport;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionCandidateScorer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionPredictionService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeBenchmarkEvaluator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionElectronicReranker;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionProductPredictionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.RuleBasedOrganicReactionCandidateGenerator;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.GeneratedReactionCandidateSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInOrganicReactionRuleLibraries;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionOutcomeCalibrationLibraries;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShellAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralCapabilityStatus;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.OrbitalRotationOptimizer;

final class ClassicalCalculationExecutorTest {

  @Test
  void executorDeclaresSupportForEveryCalculationKind() {
    final ClassicalCalculationExecutor executor = new ClassicalCalculationExecutor();
    final ClassicalCalculationKind[] kinds = ClassicalCalculationKind.values();
    for (int i = 0; i < kinds.length; ++i) {
      final ClassicalCalculationKind kind = kinds[i];
      Assertions.assertTrue(
          executor.supports(kind),
          kind.name() + " is declared in ClassicalCalculationKind but not supported by executor.");
    }
  }

  @Test
  void serviceExecutesMoleculeDescriptorAnalysis() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("ethanol_descriptors"),
            ClassicalCalculationExecutorTest.ethanol(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        "sections="
            + result.sections().size()
            + ", diagnostics="
            + result.diagnostics().diagnostics());
    Assertions.assertTrue(result.successful());
    Assertions.assertFalse(result.partial());
    Assertions.assertEquals(1, result.sectionCount());
    Assertions.assertTrue(result.hasSections());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));
    Assertions.assertEquals(1, result.sectionCount(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));
    Assertions.assertEquals(
        result.sections(), result.sections(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));
    Assertions.assertEquals(
        "Molecular descriptors",
        result.requireSection(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS).title());
    Assertions.assertTrue(
        result.requireSection(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS).hasValue("formula"));
    Assertions.assertEquals(
        "C2H6O",
        result
            .requireSection(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS)
            .requireFirstValue("formula"));
    Assertions.assertEquals("C2H6O", result.requireFirstValue("formula"));
    Assertions.assertEquals(9.0, result.requireScalar("atom_count"), 1.0E-12);
    Assertions.assertEquals("C2H6O", ClassicalCalculationExecutorTest.value(result, "formula"));
    Assertions.assertEquals("9.0", ClassicalCalculationExecutorTest.value(result, "atom_count"));
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "heavy_atom_count"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "features").contains("HYDROXYL_OR_ALKOXY"));
  }

  @Test
  void serviceSinglePointEnergyReportsScfValidationInPortableResult() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_rhf_with_scf_validation"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertTrue(Double.isFinite(result.requireScalar("total_energy")));
    Assertions.assertEquals("true", result.requireFirstValue("scf_validation_valid"));
    Assertions.assertEquals(0.0, result.requireScalar("scf_diagnostic_count"), 0.0);
    Assertions.assertTrue(result.requireScalar("scf_density_trace_deviation") <= 1.0E-8);
    Assertions.assertTrue(result.requireScalar("scf_mo_orthonormality_deviation") <= 1.0E-8);
    Assertions.assertEquals(List.of("NONE"), result.requireValues("scf_diagnostic_codes"));
  }

  @Test
  void serviceExecutesReactionDescriptorAndBalanceSummary() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("ethanol_identity"),
            "Ethanol identity",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.ethanol(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.ethanol(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_descriptors"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(
                ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
                ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        "sections="
            + result.sections().size()
            + ", diagnostics="
            + result.diagnostics().diagnostics());
    Assertions.assertTrue(result.successful());
    Assertions.assertEquals(2, result.sectionCount());
    Assertions.assertTrue(result.hasValue("delta_gibbs"));
    Assertions.assertEquals(1, result.sectionCount(ClassicalCalculationKind.REACTION_ENERGY));
    Assertions.assertEquals(
        result
            .requireSection(ClassicalCalculationKind.REACTION_ENERGY)
            .requireScalar("delta_gibbs"),
        result.requireScalar("delta_gibbs"),
        1.0E-12);
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "atom_balanced"));
    Assertions.assertEquals(
        "1*C2H6O", ClassicalCalculationExecutorTest.value(result, "reactant_formulas"));
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
  }

  @Test
  void serviceExecutesConditionAwareReactionEnergetics() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule pyridine =
        ClassicalCalculationExecutorTest.ringMolecule("energetics_pyridine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("pyridine_condition_energetics"),
            "Pyridine condition energetics",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(4.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "DMF",
                        null,
                        null,
                        ReactionPhase.SOLUTION,
                        "polar solvent"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "Pd(OAc)2",
                        null,
                        null,
                        null,
                        ReactionComponentLoading.percent(3.0),
                        null,
                        ReactionPhase.SOLID,
                        "catalyst")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "heated catalytic probe"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_energetics"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(
                ClassicalCalculationKind.REACTION_ENERGY,
                ClassicalCalculationKind.KINETIC_ANALYSIS));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "identical_reactant_product_state",
        ClassicalCalculationExecutorTest.value(result, "thermodynamic_model"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "atom_balanced"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "charge_balanced"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_enthalpy"), 1.0E-12);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "feasibility_score"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "kinetic_forward_barrier"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "kinetic_reverse_barrier"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "kinetic_relative_forward_rate"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "kinetic_relative_reverse_rate"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "kinetic_forward_reverse_ratio"))
            > 0.0);
    Assertions.assertEquals(
        "contextual_arrhenius_eyring",
        ClassicalCalculationExecutorTest.value(result, "kinetic_model"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "kinetic_context")
            .contains("condition_factor="));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "arrhenius_forward_rate_constant"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "eyring_forward_rate_constant"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "first_order_half_life"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "duration_conversion_fraction"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "duration_conversion_fraction"))
            <= 1.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_summary")
            .contains("catalyst=Pd(OAc)2"));
    final Reaction roomTemperatureReference =
        Reaction.of(
            ReactionId.of("pyridine_reference_kinetics"),
            "Pyridine reference kinetics",
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))));
    final ClassicalCalculationResult reference =
        service.execute(
            ClassicalCalculationRequest.of(
                ClassicalCalculationId.of("reaction_energetics_reference"),
                roomTemperatureReference,
                ElectronicStructureMethod.HARTREE_FOCK,
                BasisSet.STO_3G,
                List.of(ClassicalCalculationKind.REACTION_ENERGY)));
    Assertions.assertTrue(
        result.requireScalar("arrhenius_pre_exponential")
            > reference.requireScalar("arrhenius_pre_exponential"));
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
  }

  @Test
  void serviceExecutesConditionAwareReactionProductPrediction() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule pyridine =
        ClassicalCalculationExecutorTest.ringMolecule("prediction_pyridine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("condition_prediction_reaction"),
            "Condition prediction reaction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.carbonAtom(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(85.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "DMF",
                        null,
                        null,
                        ReactionPhase.SOLUTION,
                        "polar solvent"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "Pd(PPh3)4",
                        null,
                        null,
                        null,
                        ReactionComponentLoading.percent(5.0),
                        null,
                        ReactionPhase.SOLID,
                        "catalyst")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
                "condition-aware prediction"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("condition_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "HYBRID", ClassicalCalculationExecutorTest.value(result, "method_type"));
    Assertions.assertEquals(
        "reactants_conditions_staged_mechanisms_stoichiometry_and_optional_native_electronic_rerank",
        ClassicalCalculationExecutorTest.value(result, "prediction_source_contract"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "prediction_outcome_lookup_used"));
    Assertions.assertEquals(
        "disabled", ClassicalCalculationExecutorTest.value(result, "prediction_benchmark_role"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "candidate_count"))
            > 1.0);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "seed_candidate_count")),
        1.0E-12);
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "product_side_seed_used"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "generated_candidate_count"))
            > 0.0);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "user_seed_candidate_count")),
        1.0E-12);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "surrogate_candidate_count")),
        1.0E-12);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "mechanism_scored_candidate_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "candidate_with_score_breakdown_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "candidate_with_mechanism_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "candidate_with_risk_evidence_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "positive_candidate_evidence_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "negative_candidate_evidence_count"))
            > 0.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "candidate_evidence_kind_histogram")
            .contains("CATALYST_EFFECT="));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "predicted_mechanism_family_histogram")
            .contains(ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING.name()));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "activated_reaction_domains")
            .contains("HETEROCYCLE_FUNCTIONALIZATION"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "reactant_heterocycle_feature_count"))
            > 0.0);
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "reactant_radical_center_count"));
    Assertions.assertEquals(
        "DFT_REACTION_ENERGETICS_AND_OPTIONAL_HAMILTONIAN_PREPARATION",
        ClassicalCalculationExecutorTest.value(result, "recommended_calculation_followup"));
    Assertions.assertEquals(
        "FULLY_EXPLAINED",
        ClassicalCalculationExecutorTest.value(result, "prediction_coverage_status"));
    Assertions.assertEquals(
        1.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "probability_mass")),
        1.0E-12);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "missing_probability_mass")),
        1.0E-12);
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "confidence_level").isBlank());
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "condition_aware"));
    Assertions.assertEquals(
        "CROSS_COUPLING", ClassicalCalculationExecutorTest.value(result, "mechanism_class"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "regioselectivity_class").isBlank());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_window").contains("signals="));
    Assertions.assertEquals(
        "1*C6N", ClassicalCalculationExecutorTest.value(result, "best_products"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence")
            .contains("TEMPERATURE_EFFECT"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence")
            .contains("CATALYST_EFFECT"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence").contains("SOLVENT_EFFECT"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence")
            .contains("cross-coupling evidence is compatible"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "candidates").stream()
            .anyMatch(row -> row.contains("transform_heteroaryl_borylation")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "unmaterialized_mechanism_channels").stream()
            .anyMatch(
                row ->
                    row.contains("transform_heteroaryl_borylation")
                        && row.contains("No explicit reactant source of B")));
    Assertions.assertEquals(
        "participant_index\tmolecule_id\tfeature\tcount\tconfidence\tcalculation_followup",
        ClassicalCalculationExecutorTest.table(result, "reactant_feature_bridge").get(0));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reactant_feature_bridge").stream()
            .anyMatch(
                row ->
                    row.contains("HETEROCYCLE")
                        && row.contains(
                            "DFT_REACTION_ENERGETICS_AND_OPTIONAL_HAMILTONIAN_PREPARATION")));
  }

  @Test
  void reactionPredictionExecutorActivatesDomainsFromFunctionalGroups() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule probe = ClassicalCalculationExecutorTest.carbonylAlcoholNitrileProbe();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("functional_group_domain_reaction"),
            "Functional group domain reaction",
            ReactionSide.of(List.of(ReactionParticipant.of(probe, StoichiometricCoefficient.ONE))),
            ReactionSide.of(List.of(ReactionParticipant.of(probe, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "triethylamine")),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "functional-group inferred domain routing"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("functional_group_domain_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));

    final ClassicalCalculationResult result = service.execute(request);
    final String domains =
        ClassicalCalculationExecutorTest.value(result, "activated_reaction_domains");

    Assertions.assertTrue(domains.contains(OrganicReactionDomain.ADDITION.name()), domains);
    Assertions.assertTrue(domains.contains(OrganicReactionDomain.ACID_BASE.name()), domains);
    Assertions.assertTrue(
        domains.contains(OrganicReactionDomain.OXIDATION_REDUCTION.name()), domains);
  }

  @Test
  void reactionProductPredictionKeepsNamedMechanismForReactionSubjectSeed() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule substrate = ClassicalCalculationExecutorTest.iodobenzene();
    final Molecule product = ClassicalCalculationExecutorTest.ringMolecule("suzuki_product", "B");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("suzuki_subject_seed"),
            "Suzuki subject seed",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(80.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(12.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(PPh3)4"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "phenyl boronic acid"),
                    ReactionConditionComponent.named(ReactionComponentRole.BASE, "K2CO3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
                "Named Suzuki condition set"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("suzuki_subject_seed_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_use_product_side_as_seed", true)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        "sections="
            + result.sections().size()
            + ", diagnostics="
            + result.diagnostics().diagnostics());
    Assertions.assertTrue(result.successful());
    Assertions.assertEquals(
        "reactants_conditions_staged_mechanisms_stoichiometry_and_optional_native_electronic_rerank",
        ClassicalCalculationExecutorTest.value(result, "prediction_source_contract"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "prediction_outcome_lookup_used"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "product_side_seed_used"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "user_seed_candidate_count"))
            > 0.0);
    Assertions.assertEquals(
        ReactionMechanismClass.CROSS_COUPLING.name(),
        ClassicalCalculationExecutorTest.value(result, "mechanism_class"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_predicted_mechanisms")
            .contains(ReactionMechanismFamily.SUZUKI_COUPLING.name()));
    Assertions.assertEquals(
        "FULLY_EXPLAINED",
        ClassicalCalculationExecutorTest.value(result, "prediction_coverage_status"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "predicted_mechanism_family_histogram")
            .contains(ReactionMechanismFamily.SUZUKI_COUPLING.name()));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "candidate_evidence_kind_histogram")
            .contains(ReactionPredictionEvidenceKind.USER_RULE.name()));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "best_predicted_mechanisms")
            .contains(ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING.name()));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_window")
            .contains("mechanism:" + ReactionMechanismFamily.SUZUKI_COUPLING.name()));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_window")
            .contains("requires:oxygen-control"));
  }

  @Test
  void reactionProductPredictionDoesNotInferDomainsFromProductSideWithoutSeed() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("product_side_domain_leak_guard"),
            "Product-side domain leak guard",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.ringMolecule("product_only_pyridine", "N"),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("product_side_domain_leak_guard"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "product_side_seed_used"));
    Assertions.assertEquals(
        OrganicReactionDomain.UNKNOWN.name(),
        ClassicalCalculationExecutorTest.value(result, "activated_reaction_domains"));
  }

  @Test
  void reactionProductPredictionCanInferDomainsFromProductSideWhenSeedIsExplicit() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("product_side_domain_seed"),
            "Product-side domain seed",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.ringMolecule("seed_product_pyridine", "N"),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("product_side_domain_seed"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_use_product_side_as_seed", true)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "product_side_seed_used"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "activated_reaction_domains")
            .contains(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION.name()));
  }

  @Test
  void reactionProductPredictionCanRerankSeedCandidatesWithJavaNativeElectronicEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_prediction_electronic_rerank"),
            "Hydrogen prediction electronic rerank",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_prediction_electronic_rerank"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.text(
                    "reaction_prediction_use_product_side_as_seed", "true"),
                ClassicalCalculationOption.text("reaction_prediction_electronic_rerank", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertTrue(
        result.status() == ClassicalCalculationStatus.SUCCEEDED
            || result.status() == ClassicalCalculationStatus.PARTIAL,
        result.diagnostics().toString());
    Assertions.assertEquals(
        "HYBRID", ClassicalCalculationExecutorTest.value(result, "method_type"));
    Assertions.assertEquals(
        1.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "probability_mass")),
        1.0E-12);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "candidate_evidence_kind_histogram")
            .contains("ELECTRONIC_FACTOR="));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence")
            .contains("Java-native candidate electronic rerank delta_gibbs="));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "candidates").stream()
            .anyMatch(row -> row.contains("ELECTRONIC_FACTOR")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").stream()
            .anyMatch(row -> row.contains("components=") && row.contains("ELECTRONIC:")),
        "Electronic rerank must be visible in score breakdown, not only in raw evidence.");
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("electronic rerank was unavailable")),
        result.diagnostics().toString());
  }

  @Test
  void
      reactionProductPredictionFallsBackToSemiEmpiricalRerankWhenNamedBasisCannotCoverOrganicHalogen() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("heteroaryl_bromide_prediction_electronic_rerank"),
            "Heteroaryl bromide prediction electronic rerank",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.heteroarylBromideProbe(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.heteroarylBromideProbeCopy(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heteroaryl_bromide_prediction_electronic_rerank"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.text(
                    "reaction_prediction_use_product_side_as_seed", "true"),
                ClassicalCalculationOption.text("reaction_prediction_electronic_rerank", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertTrue(
        result.status() == ClassicalCalculationStatus.SUCCEEDED
            || result.status() == ClassicalCalculationStatus.PARTIAL,
        result.diagnostics().toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence")
            .contains("Java-native candidate electronic rerank delta_gibbs="));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_evidence")
            .contains("fallback was used: method=Native semiempirical valence model"),
        ClassicalCalculationExecutorTest.value(result, "best_evidence"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "candidate_evidence_kind_histogram")
            .contains("ELECTRONIC_FACTOR="));
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("electronic rerank was unavailable")),
        result.diagnostics().toString());
  }

  @Test
  void serviceModelsRealNicotinonitrileBenzylamineReactionBenchmark() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("nicotinonitrile_benzylamine_benchmark_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
                ClassicalCalculationKind.REACTION_ENERGY,
                ClassicalCalculationKind.KINETIC_ANALYSIS,
                ClassicalCalculationKind.REACTION_PATH),
            List.of(
                ClassicalCalculationOption.text("reaction_energy_model", "bond_energy"),
                ClassicalCalculationOption.text(
                    "reaction_prediction_include_benchmark_audit", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(reaction.balance().atomBalanced());
    Assertions.assertTrue(reaction.balance().chargeBalanced());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_window").contains("HEATING"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "condition_suitability_score"))
            > 0.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "activated_reaction_domains")
            .contains("HETEROCYCLE_FUNCTIONALIZATION"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "activated_reaction_domains")
            .contains("NUCLEOPHILIC_SUBSTITUTION"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_products").contains("C15H15N3"),
        ClassicalCalculationExecutorTest.value(result, "best_products"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_products").contains("ClH"),
        ClassicalCalculationExecutorTest.value(result, "best_products"));
    Assertions.assertEquals(
        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION.name(),
        ClassicalCalculationExecutorTest.value(result, "mechanism_class"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_predicted_mechanisms")
            .contains(ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION.name()),
        ClassicalCalculationExecutorTest.value(result, "best_predicted_mechanisms"));
    Assertions.assertFalse(result.hasValue("calibration_evidence_count"));
    Assertions.assertFalse(result.hasValue("matched_calibration_channels"));
    Assertions.assertEquals(
        "reactants_conditions_staged_mechanisms_stoichiometry_and_optional_native_electronic_rerank",
        ClassicalCalculationExecutorTest.value(result, "prediction_source_contract"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "prediction_outcome_lookup_used"));
    Assertions.assertEquals(
        "audit_only_after_prediction",
        ClassicalCalculationExecutorTest.value(result, "prediction_benchmark_role"));
    Assertions.assertEquals(
        "evaluate_finished_prediction_without_reweighting",
        ClassicalCalculationExecutorTest.value(result, "prediction_benchmark_data_flow"));
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "prediction_calibration_prior_weight")),
        0.0);
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "benchmark_audit_enabled"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "benchmark_audit_only"));
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "benchmark_audit_probability_effect")),
        0.0);
    Assertions.assertTrue(result.hasValue("benchmark_exact_observation_count"));
    Assertions.assertTrue(result.hasValue("benchmark_mae"));
    Assertions.assertTrue(result.hasValue("benchmark_weighted_mae"));
    Assertions.assertTrue(result.hasValue("benchmark_observation_trace"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "benchmark_observation_trace")
            .get(0)
            .contains("observed_probability"),
        ClassicalCalculationExecutorTest.table(result, "benchmark_observation_trace").toString());
    Assertions.assertTrue(result.hasValue("candidate_distribution_trace"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace")
            .get(0)
            .contains("risk_count"),
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").toString());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace")
            .get(0)
            .contains("calibration_trace"),
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").stream()
            .anyMatch(row -> row.contains("positive=") && row.contains("normalized=")),
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").stream()
            .anyMatch(row -> row.contains("positive=") && row.contains("normalized=")),
        ClassicalCalculationExecutorTest.table(result, "candidate_distribution_trace").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "best_mechanism_steps")
            .contains("NUCLEOPHILIC_ATTACK"),
        ClassicalCalculationExecutorTest.value(result, "best_mechanism_steps"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "reactant_heterocycle_feature_count"))
            > 0.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reactant_feature_bridge").stream()
            .anyMatch(row -> row.contains("HETEROCYCLE")));
    Assertions.assertEquals(
        "condition_adjusted_bond_energy", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "internal_bond_energy_thermochemistry",
        ClassicalCalculationExecutorTest.value(result, "model_scope"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_summary")
            .contains("solvent=PEG4000"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "condition_summary").contains("T=393.15K"),
        ClassicalCalculationExecutorTest.value(result, "condition_summary"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "delta_gibbs"))));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "kinetic_forward_barrier"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "forward_barrier"))
            > 0.0);
    Assertions.assertEquals(
        ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION.name(),
        ClassicalCalculationExecutorTest.value(result, "mechanism_family"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch(row -> row.contains("nucleophilic_attack")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch(row -> row.contains("sigma_complex_formation")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch(row -> row.contains("leaving_group_departure")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reaction_path_points").size() >= 4);
  }

  @Test
  void reactionPredictionExecutorDoesNotUseBenchmarkAuditByDefault() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("default_prediction_without_benchmark_audit"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "benchmark_audit_enabled"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "benchmark_audit_only"));
    Assertions.assertEquals(
        "not_read",
        ClassicalCalculationExecutorTest.value(result, "prediction_benchmark_data_flow"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "electronic_rerank_enabled"));
    Assertions.assertEquals(
        "FAST_MECHANISTIC",
        ClassicalCalculationExecutorTest.value(result, "prediction_execution_mode"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "electronic_rerank_opt_in_required"));
    Assertions.assertTrue(result.hasValue("product_graph_coverage_status"));
    Assertions.assertTrue(result.hasValue("product_graph_followup_reason"));
    Assertions.assertEquals(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "candidate_count")),
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(
                result, "electronic_rerank_skipped_by_limit_count")),
        1.0E-12);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "prediction_calibration_prior_weight")),
        0.0);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "benchmark_audit_probability_effect")),
        0.0);
    Assertions.assertFalse(result.hasValue("benchmark_exact_observation_count"));
    Assertions.assertFalse(result.hasValue("benchmark_mae"));
    Assertions.assertFalse(result.hasValue("benchmark_weighted_mae"));
    Assertions.assertFalse(result.hasValue("benchmark_observation_trace"));
    Assertions.assertFalse(result.hasValue("calibration_evidence_count"));
  }

  @Test
  void predictionGeneratesRealNicotinonitrileBenzylamineProductWithoutSeed() {
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final Reaction observedReaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "nicotinonitrile_benzylamine_prediction_without_seed",
            observedReaction.reactants(),
            observedReaction.conditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "Predict products only from drawn reactants and reaction conditions.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(result.probabilityMassComplete(1.0E-12));
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                ClassicalCalculationExecutorTest::isGeneratedBenzylaminoNicotinonitrileCandidate),
        result.candidates().toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.isGeneratedBenzylaminoNicotinonitrileCandidate(
            result.candidates().get(0)),
        result.candidates().toString());
    Assertions.assertTrue(
        result.candidates().size() == 1
            || result.candidates().get(0).probability() > result.candidates().get(1).probability(),
        "Expected the complete amine-fragment product to remain the top generated channel.");
    Assertions.assertTrue(
        result.candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.predictedMechanisms().stream()
                        .anyMatch(
                            mechanism ->
                                mechanism.family()
                                    == ReactionMechanismFamily
                                        .NUCLEOPHILIC_AROMATIC_SUBSTITUTION)));
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .anyMatch(
                evidence ->
                    evidence.description().contains("complete amine substituent fragment")));
  }

  @Test
  void predictionReportsOutcomeBenchmarkThroughSeparateAuditEvaluator() {
    final MolecularFeatureAnalyzer featureAnalyzer = new MolecularFeatureAnalyzer();
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()),
            new OrganicReactionCandidateScorer(featureAnalyzer));
    final Reaction observedReaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "calibrated_nicotinonitrile_benzylamine_prediction",
            observedReaction.reactants(),
            observedReaction.conditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "Predict with literature/lab outcome benchmark enabled.");

    final ReactionPredictionResult result = service.predict(request);
    final ReactionOutcomeBenchmarkReport benchmarkReport =
        new ReactionOutcomeBenchmarkEvaluator()
            .evaluate(
                result,
                BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());

    Assertions.assertTrue(result.hasCandidates());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.isGeneratedBenzylaminoNicotinonitrileCandidate(
            result.candidates().get(0)),
        result.candidates().toString());
    Assertions.assertTrue(
        benchmarkReport.exactObservationCount() > 0L
            || benchmarkReport.comparableObservationCount() > 0L,
        benchmarkReport.matches().toString());
    Assertions.assertTrue(Double.isFinite(benchmarkReport.meanAbsoluteError()));
    Assertions.assertTrue(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
  }

  @Test
  void predictionExecutorUsesInjectedBenchmarkLibraryOnlyForAuditValues() {
    final MolecularFeatureAnalyzer featureAnalyzer = new MolecularFeatureAnalyzer();
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(
            service,
            featureAnalyzer,
            new ReactionPredictionExplainer(),
            BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
            new ReactionPredictionElectronicReranker(),
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("injected_benchmark_audit_only"),
            ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.text(
                    "reaction_prediction_include_benchmark_audit", "true")),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection section = executor.execute(request, diagnostics);

    Assertions.assertNotNull(section);
    Assertions.assertTrue(
        diagnostics.stream()
            .noneMatch(diagnostic -> diagnostic.severity() == ClassicalDiagnosticSeverity.ERROR),
        diagnostics.toString());
    Assertions.assertEquals("true", section.requireFirstValue("benchmark_audit_enabled"));
    Assertions.assertEquals("true", section.requireFirstValue("benchmark_audit_only"));
    Assertions.assertEquals(0.0, section.requireScalar("benchmark_audit_probability_effect"), 0.0);
    Assertions.assertTrue(section.requireScalar("benchmark_exact_observation_count") >= 0.0);
    Assertions.assertTrue(section.requireScalar("benchmark_comparable_observation_count") >= 0.0);
    Assertions.assertFalse(section.hasValue("calibration_evidence_count"));
    Assertions.assertTrue(!section.requireValues("benchmark_observation_trace").isEmpty());
  }

  @Test
  void predictionExecutorDoesNotApplyCalibrationPriorsByDefault() {
    final MolecularFeatureAnalyzer featureAnalyzer = new MolecularFeatureAnalyzer();
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(
            service,
            featureAnalyzer,
            new ReactionPredictionExplainer(),
            BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
            new ReactionPredictionElectronicReranker(),
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("prediction_executor_default_without_calibration_prior"),
            ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection section = executor.execute(request, diagnostics);

    Assertions.assertNotNull(section);
    Assertions.assertTrue(
        diagnostics.stream()
            .noneMatch(diagnostic -> diagnostic.severity() == ClassicalDiagnosticSeverity.ERROR),
        diagnostics.toString());
    Assertions.assertEquals(
        "false", section.requireFirstValue("prediction_calibration_prior_enabled"));
    Assertions.assertEquals(
        0.0, section.requireScalar("prediction_calibration_prior_evidence_count"), 0.0);
    Assertions.assertEquals(0.0, section.requireScalar("prediction_calibration_prior_weight"), 0.0);
    Assertions.assertTrue(
        section.requireValues("candidate_distribution_trace").stream()
            .noneMatch(row -> row.contains("CALIBRATION_PRIOR")),
        section.requireValues("candidate_distribution_trace").toString());
  }

  @Test
  void predictionExecutorBenchmarkAuditDoesNotChangeCandidateProbabilities() {
    final MolecularFeatureAnalyzer featureAnalyzer = new MolecularFeatureAnalyzer();
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(
            service,
            featureAnalyzer,
            new ReactionPredictionExplainer(),
            BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
            new ReactionPredictionElectronicReranker(),
            BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest withoutAudit =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("benchmark_audit_probability_baseline"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    final ClassicalCalculationRequest withAudit =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("benchmark_audit_probability_audited"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.text(
                    "reaction_prediction_include_benchmark_audit", "true")),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> baselineDiagnostics = new ArrayList<ClassicalDiagnostic>();
    final ArrayList<ClassicalDiagnostic> auditedDiagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection baseline = executor.execute(withoutAudit, baselineDiagnostics);
    final ClassicalResultSection audited = executor.execute(withAudit, auditedDiagnostics);

    Assertions.assertNotNull(baseline);
    Assertions.assertNotNull(audited);
    Assertions.assertEquals("false", baseline.requireFirstValue("benchmark_audit_enabled"));
    Assertions.assertEquals("true", audited.requireFirstValue("benchmark_audit_enabled"));
    Assertions.assertEquals("false", baseline.requireFirstValue("benchmark_audit_only"));
    Assertions.assertEquals("true", audited.requireFirstValue("benchmark_audit_only"));
    Assertions.assertEquals(0.0, baseline.requireScalar("benchmark_audit_probability_effect"), 0.0);
    Assertions.assertEquals(0.0, audited.requireScalar("benchmark_audit_probability_effect"), 0.0);
    Assertions.assertEquals(
        baseline.requireScalar("best_probability"),
        audited.requireScalar("best_probability"),
        1.0E-12);
    Assertions.assertEquals(
        baseline.requireFirstValue("best_products"), audited.requireFirstValue("best_products"));
    Assertions.assertEquals(
        baseline.requireValues("candidate_distribution_trace"),
        audited.requireValues("candidate_distribution_trace"));
    Assertions.assertFalse(baseline.hasValue("benchmark_observation_trace"));
    Assertions.assertTrue(audited.hasValue("benchmark_observation_trace"));
  }

  @Test
  void predictionProbabilitiesAreIndependentFromOutcomeBenchmarkRows() {
    final MolecularFeatureAnalyzer featureAnalyzer = new MolecularFeatureAnalyzer();
    final Reaction observedReaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "benchmark_independence_nicotinonitrile_benzylamine",
            observedReaction.reactants(),
            observedReaction.conditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            "Benchmark rows must validate predictions without becoming lookup answers.");
    final OrganicReactionPredictionService baseline =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()));
    final OrganicReactionPredictionService benchmarked =
        new OrganicReactionPredictionService(
            featureAnalyzer,
            new RuleBasedOrganicReactionCandidateGenerator(
                BuiltInOrganicReactionRuleLibraries.heterocycleProductPrediction()),
            new OrganicReactionCandidateScorer(featureAnalyzer));

    final ReactionPredictionResult baselineResult = baseline.predict(request);
    final ReactionPredictionResult benchmarkedResult = benchmarked.predict(request);

    Assertions.assertEquals(
        baselineResult.candidates().size(), benchmarkedResult.candidates().size());
    for (int i = 0; i < baselineResult.candidates().size(); ++i) {
      Assertions.assertTrue(
          baselineResult
              .candidates()
              .get(i)
              .products()
              .sameParticipantsAs(benchmarkedResult.candidates().get(i).products()));
      Assertions.assertEquals(
          baselineResult.candidates().get(i).probability(),
          benchmarkedResult.candidates().get(i).probability(),
          1.0E-12);
    }
    Assertions.assertFalse(
        benchmarkedResult.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .anyMatch(
                evidence ->
                    evidence
                            .description()
                            .contains(
                                "Benchmark observation matched without changing prediction probability")
                        && evidence.weight() == 0.0));
  }

  @Test
  void reactionPredictionExecutorReportsMergedDuplicateProductChannels() {
    final Molecule substrate = ClassicalCalculationExecutorTest.chloroDimethylNicotinonitrile();
    final Molecule product =
        ClassicalCalculationExecutorTest.ringMolecule("executor_duplicate_product", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("executor_duplicate_channel_reaction"),
            "Executor duplicate channel reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(substrate, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(95.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(2.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
                "controlled duplicate channel executor check"),
            ChemistryMetadata.EMPTY);
    final ReactionProductCandidate generatedCandidate =
        ReactionProductCandidate.of(
            reaction.products(),
            0.40,
            44.0,
            "generated duplicate product channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
                    "generated duplicate evidence",
                    0.16)));
    final OrganicReactionRule duplicateRule =
        new OrganicReactionRule() {
          public OrganicReactionRuleId id() {
            return OrganicReactionRuleId.of("executor_duplicate_generated_rule");
          }

          public String displayName() {
            return "Executor duplicate generated product rule";
          }

          public OrganicReactionRuleCategory category() {
            return OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION;
          }

          public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
            return OrganicReactionRuleMatch.matched(
                0.72, "duplicate executor product channel matched");
          }

          public List<ReactionProductCandidate> generate(
              final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
            return List.of(generatedCandidate);
          }
        };
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new RuleBasedOrganicReactionCandidateGenerator(
                OrganicReactionRuleLibrary.of(List.of(duplicateRule))));
    final ReactionProductPredictionExecutor executor =
        new ReactionProductPredictionExecutor(service, new MolecularFeatureAnalyzer());
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("executor_duplicate_channel_request"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.text(
                    "reaction_prediction_use_product_side_as_seed", "true")),
            ChemistryMetadata.EMPTY);
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();

    final ClassicalResultSection section = executor.execute(request, diagnostics);

    Assertions.assertNotNull(section);
    Assertions.assertTrue(
        diagnostics.stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.code()
                        == ClassicalDiagnosticCode.PREDICTION_REQUIRES_REACTION_SUBJECT),
        diagnostics.toString());
    Assertions.assertEquals(1.0, section.requireScalar("candidate_count"), 1.0E-12);
    Assertions.assertEquals(
        1.0, section.requireScalar("merged_duplicate_product_channel_count"), 1.0E-12);
    Assertions.assertTrue(
        section
            .requireFirstValue("candidate_evidence_kind_histogram")
            .contains(ReactionPredictionEvidenceKind.COMPETING_PATHWAY.name()),
        section.requireFirstValue("candidate_evidence_kind_histogram"));
    Assertions.assertTrue(
        section.requireFirstValue("best_evidence").contains("Merged duplicate product channel"),
        section.requireFirstValue("best_evidence"));
  }

  @Test
  void predictionServiceDropsFallbackSurrogatesWhenGraphDerivedCandidatesExist() {
    final Molecule graphProduct =
        ClassicalCalculationExecutorTest.ringMolecule("graph_product", "N");
    final Molecule fallbackProduct =
        ClassicalCalculationExecutorTest.ringMolecule("fallback_product", "O");
    final ReactionProductCandidate graphCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(graphProduct, StoichiometricCoefficient.ONE))),
            0.42,
            50.0,
            "graph-derived channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
                    "Generated from actual graph molecular graph changes.",
                    0.16)));
    final ReactionProductCandidate fallbackCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(fallbackProduct, StoichiometricCoefficient.ONE))),
            0.48,
            55.0,
            "fallback surrogate channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Fallback single-site surrogate retained only when graph-level channel cannot be used.",
                    0.22)));
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            request ->
                GeneratedReactionCandidateSet.of(
                    List.of(graphCandidate, fallbackCandidate), List.of()));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "graph_candidate_suppresses_fallback",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.chloroDimethylNicotinonitrile(),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            "Graph-derived products should suppress fallback surrogate products.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertEquals(1, result.candidates().size());
    Assertions.assertTrue(
        result.candidates().get(0).products().sameParticipantsAs(graphCandidate.products()));
    Assertions.assertFalse(
        result.candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.SURROGATE_PRODUCT));
  }

  @Test
  void predictionServiceDoesNotReturnFallbackSurrogateAsProductWhenNoGraphProductExists() {
    final Molecule fallbackProduct =
        ClassicalCalculationExecutorTest.ringMolecule("fallback_only_product", "O");
    final ReactionProductCandidate fallbackCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(fallbackProduct, StoichiometricCoefficient.ONE))),
            0.48,
            55.0,
            "fallback surrogate channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Fallback single-site surrogate retained only when graph-level channel cannot be used.",
                    0.22)));
    final OrganicReactionPredictionService service =
        new OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            request -> GeneratedReactionCandidateSet.of(List.of(fallbackCandidate), List.of()));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "fallback_surrogate_is_diagnostic_only",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.chloroDimethylNicotinonitrile(),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            "Fallback surrogate candidates must not masquerade as predicted products.");

    final ReactionPredictionResult result = service.predict(request);

    Assertions.assertTrue(result.candidates().isEmpty());
    Assertions.assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                evidence ->
                    evidence.description().contains("Surrogate product candidate was removed")),
        result.diagnostics().toString());
  }

  @Test
  void serviceUsesDirectScfRouteForExplicitDirectSinglePoint() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("explicit_direct_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(ClassicalCalculationOption.text("scf_integral_mode", "direct")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    Assertions.assertEquals(
        "DIRECT_HARTREE_FOCK", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "gaussian_screened_cached",
        ClassicalCalculationExecutorTest.value(result, "direct_eri_provider"));
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        "direct_gaussian_restricted_hartree_fock", result.coverageReport().items().get(0).model());
    Assertions.assertTrue(Double.isFinite(result.requireScalar("total_energy")));
  }

  @Test
  void serviceExecutesReactionPathAndTransitionStateAnalysisForConditionRichReaction() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule pyridine = ClassicalCalculationExecutorTest.ringMolecule("path_pyridine", "N");
    final Molecule functionalizedPyridine =
        ClassicalCalculationExecutorTest.functionalizedRingMolecule(
            "path_functionalized_pyridine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("path_condition_reaction"),
            "Condition-aware reaction path",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(pyridine, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.carbonAtom(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(functionalizedPyridine, StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(90.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(5.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "Pd(PPh3)4",
                        null,
                        null,
                        null,
                        ReactionComponentLoading.percent(5.0),
                        null,
                        ReactionPhase.SOLID,
                        "cross-coupling catalyst"),
                    ReactionConditionComponent.named(ReactionComponentRole.LIGAND, "PPh3")),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
                "reaction path probe"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_path_and_ts"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.REACTION_PATH,
                ClassicalCalculationKind.TRANSITION_STATE_SEARCH),
            List.of(ClassicalCalculationOption.text("reaction_path_energy_source", "bond_change")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "mechanistic_bond_energy_pathway",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "atom_balanced"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "charge_balanced"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "forward_barrier"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "forward_rate")) >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "reverse_rate")) >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "forward_reverse_rate_ratio"))
            > 0.0);
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "mechanism_available"));
    Assertions.assertEquals(
        "CATALYTIC_CYCLE", ClassicalCalculationExecutorTest.value(result, "mechanism_family"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "mechanism_step_count"))
            >= 5.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "mechanism_event_count"))
            >= 7.0);
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "transition_state_count"));
    Assertions.assertEquals(
        "path_condition_reaction_mechanism",
        ClassicalCalculationExecutorTest.value(result, "mechanism_id"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "highest_relative_energy"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "condition_activation_shift"))
            > 0.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reaction_path_points").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reaction_path_points").stream()
            .anyMatch((final String row) -> row.contains("transition_state")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reaction_path_points").stream()
            .anyMatch((final String row) -> row.contains("intermediate")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reaction_path_points").stream()
            .anyMatch(
                (final String row) -> row.contains("catalytic_cycle_productive_transition_state")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").size() >= 8);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch((final String row) -> row.contains("solvation_reorganization")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch((final String row) -> row.contains("catalyst_association")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch((final String row) -> row.contains("reductive_elimination")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanistic_events").stream()
            .anyMatch((final String row) -> row.contains("conformational_reorganization")));
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.message().contains("reaction_path_energy_source=bond_change")),
        result.diagnostics().toString());
  }

  @Test
  void reactionPathRequiresReactionSubject() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("molecule_reaction_path"),
            ClassicalCalculationExecutorTest.ethanol(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PATH));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.FAILED, result.status());
    Assertions.assertTrue(result.sections().isEmpty());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED));
  }

  @Test
  void reactionPathUsesJavaNativeElectronicReactionEnergyWhenAvailable() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_path_identity_native"),
            "Hydrogen path identity native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_path_identity_native"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PATH));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "mechanistic_electronic_energy_pathway",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "reaction_energy_source"));
    Assertions.assertEquals(0.0, result.requireScalar("electronic_reaction_energy"), 1.0E-10);
    Assertions.assertEquals(0.0, result.requireScalar("reaction_energy"), 1.0E-10);
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "mechanism_available"));
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceExecutesHydrogenSto3gSinglePointEnergyThroughJavaNativeRhf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("RHF", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        -1.116659596565565,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        5.0E-5);
    Assertions.assertEquals(
        -1.829982464592738,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "electronic_energy")),
        5.0E-5);
  }

  @Test
  void serviceExecutesHydrogenSix31gSinglePointEnergyThroughJavaNativeRhf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_631g_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("RHF", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("6-31G", ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        -1.1267265673723554,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        1.0E-6);
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void electronicPreparationPreflightAcceptsHydrogenChlorideSix31gThirdPeriodBasis() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_chloride_631g_preflight"),
            ClassicalCalculationExecutorTest.hydrogenChloride(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ElectronicStructurePreparationPreflight preflight = pipeline.preflight(request);

    Assertions.assertTrue(preflight.supported());
    Assertions.assertTrue(preflight.hasFullTensorPreflight());
    Assertions.assertTrue(preflight.hasOneElectronPreflight());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.fullTensorPreflight().status());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.oneElectronPreflight().status());
    Assertions.assertEquals(15, preflight.fullTensorPreflight().orbitalCount());
    Assertions.assertEquals(15, preflight.oneElectronPreflight().orbitalCount());
  }

  @Test
  void
      serviceExecutesSemiEmpiricalSinglePointForHeteroHalogenatedOrganicMoleculeWithoutBasisData() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heteroaryl_bromide_semiempirical_single_point"),
            ClassicalCalculationExecutorTest.heteroarylBromideProbe(),
            ElectronicStructureMethod.SEMI_EMPIRICAL,
            BasisSet.of(BasisSetName.of("native-valence")),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertEquals(
        "native_valence_graph_semiempirical",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "not_required", ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("total_energy")));
    Assertions.assertTrue(result.requireScalar("aromatic_bond_count") > 0.0);
    Assertions.assertTrue(result.requireScalar("conjugation_energy") < 0.0);
  }

  @Test
  void serviceExecutesSemiEmpiricalReactionEnergyWithoutClaimingQuantumProgramPreparation() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("heteroaryl_bromide_semiempirical_identity"),
            "Heteroaryl bromide semiempirical identity",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.heteroarylBromideProbe(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.heteroarylBromideProbeCopy(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heteroaryl_bromide_semiempirical_reaction_energy"),
            reaction,
            ElectronicStructureMethod.SEMI_EMPIRICAL,
            BasisSet.of(BasisSetName.of("native-valence")),
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "Native semiempirical valence model",
        ClassicalCalculationExecutorTest.value(result, "electronic_energy_basis"));
    Assertions.assertEquals(
        "NO_PARTICIPANT_QUANTUM_PROGRAM_PREPARABLE",
        ClassicalCalculationExecutorTest.value(
            result, "reaction_quantum_program_preparation_status"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_electronic_energy"), 1.0E-12);
  }

  @Test
  void serviceKeepsElectronicStructurePreparationSectionKind() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_electronic_structure_preparation"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertTrue(
        result.hasSection(ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION));
    Assertions.assertFalse(result.hasSection(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    Assertions.assertEquals(
        ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION,
        result.requireSection(ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION).kind());
    Assertions.assertTrue(
        result
            .requireSection(ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION)
            .hasValue("spin_orbital_count"));
  }

  @Test
  void servicePreparesHalogenMoleculeWithGeneratedEvenTemperedBasis() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_bromide_generated_basis_preparation"),
            ClassicalCalculationExecutorTest.hydrogenBromide(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.of(BasisSetName.of("AUTO-EVEN-TEMPERED")),
            List.of(ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertTrue(
        result.hasSection(ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION));
    Assertions.assertEquals(
        "AUTO-EVEN-TEMPERED", ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "basis_orbital_count"))
            > 20.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"))
            > 40.0);
  }

  @Test
  void serviceRoutesQuantumSinglePointMethodToQuantumProgramPreparationContract() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_vqe_single_point_contract"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.QUANTUM_VARIATIONAL,
                ElectronicStructureMethodName.of("VQE"),
                ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.UNSUPPORTED, result.status());
    Assertions.assertFalse(result.hasSection(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED
                        && "method".equals(diagnostic.target().kind())
                        && diagnostic
                            .message()
                            .contains(
                                ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION.name())));
    Assertions.assertEquals(ClassicalCoverageStatus.UNSUPPORTED, result.coverageReport().status());
    final ClassicalCalculationCoverageItem item = result.coverageReport().items().get(0);
    Assertions.assertEquals(ClassicalCalculationKind.SINGLE_POINT_ENERGY, item.kind());
    Assertions.assertEquals(ClassicalCoverageStatus.UNSUPPORTED, item.status());
    Assertions.assertEquals("quantum_backend_execution_required", item.model());
  }

  @Test
  void serviceExecutesHydrogenAtomSto3gSinglePointEnergyThroughJavaNativeUhf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_uhf_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.HARTREE_FOCK,
                ElectronicStructureMethodName.of("UHF"),
                ElectronicStructureSpinTreatment.UNRESTRICTED),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("UHF", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertEquals(
        0.75,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "spin_squared")),
        1.0E-12);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "spin_contamination")),
        1.0E-12);
    Assertions.assertEquals(
        -0.46658184955727533,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        5.0E-5);
  }

  @Test
  void serviceExecutesHydrogenAtomSto3gSinglePointEnergyThroughJavaNativeRohf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_rohf_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.HARTREE_FOCK,
                ElectronicStructureMethodName.of("ROHF"),
                ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("ROHF", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "closed_shell_pair_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "open_shell_electron_count"));
    Assertions.assertEquals(
        0.75,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "spin_squared")),
        1.0E-12);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "spin_contamination")),
        1.0E-12);
    Assertions.assertEquals(
        -0.46658184955727533,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        5.0E-5);
  }

  @Test
  void serviceExecutesHydrogenAtomSto3gSinglePointEnergyThroughDirectJavaNativeRohf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_direct_rohf_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.HARTREE_FOCK,
                ElectronicStructureMethodName.of("ROHF"),
                ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(ClassicalCalculationOption.text("scf_integral_mode", "direct")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("ROHF", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "gaussian_screened_cached",
        ClassicalCalculationExecutorTest.value(result, "direct_eri_provider"));
    Assertions.assertEquals(
        -0.46658184955727533,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        5.0E-5);
  }

  @Test
  void
      serviceRoutesRestrictedOpenShellReferenceThroughHamiltonianPropertiesAndActiveSpaceWorkflows() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ElectronicStructureMethod rohfMethod =
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodName.of("ROHF"),
            ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL);
    final ClassicalCalculationRequest hamiltonianRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_rohf_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            rohfMethod,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    final ClassicalCalculationRequest activeSpaceRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_rohf_active_space"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            rohfMethod,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.ACTIVE_SPACE_SELECTION));
    final ClassicalCalculationRequest propertyRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_rohf_properties"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            rohfMethod,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));

    final ClassicalCalculationResult hamiltonianResult = service.execute(hamiltonianRequest);
    final ClassicalCalculationResult activeSpaceResult = service.execute(activeSpaceRequest);
    final ClassicalCalculationResult propertyResult = service.execute(propertyRequest);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        hamiltonianResult.status(),
        hamiltonianResult.diagnostics().diagnostics().toString());
    Assertions.assertEquals(
        "ROHF", ClassicalCalculationExecutorTest.value(hamiltonianResult, "reference_method"));
    Assertions.assertEquals(
        "RESTRICTED_OPEN_SHELL",
        ClassicalCalculationExecutorTest.value(hamiltonianResult, "spin_sector"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(hamiltonianResult, "alpha_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(hamiltonianResult, "beta_electron_count"));
    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, activeSpaceResult.status());
    Assertions.assertEquals(
        "ROHF", ClassicalCalculationExecutorTest.value(activeSpaceResult, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(activeSpaceResult, "open_shell_reference"));
    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, propertyResult.status());
    Assertions.assertEquals(
        "ROHF", ClassicalCalculationExecutorTest.value(propertyResult, "reference"));
    Assertions.assertEquals(
        -0.46658184955727533,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(propertyResult, "total_energy")),
        5.0E-5);
  }

  @Test
  void servicePromotesOpenShellGeneralHartreeFockRequestToUhfReference() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_general_hf_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("UHF", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
  }

  @Test
  void serviceExecutesHydrogenSto3gKohnShamDftSinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_dft_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("B3LYP"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "DFT_KOHN_SHAM", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("B3LYP", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "HYBRID_GGA", ClassicalCalculationExecutorTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "ATOM_CENTERED_GRID_B3LYP_B88_LYP_HYBRID_KOHN_SHAM",
        ClassicalCalculationExecutorTest.value(result, "functional_solver_model"));
    Assertions.assertEquals(
        "JAVA_NATIVE_HYBRID_APPROXIMATE",
        ClassicalCalculationExecutorTest.value(result, "parity_level"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "grid_model_available"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "gradient_corrected"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "dispersion_corrected"));
    Assertions.assertEquals(
        "KOHN_SHAM_DFT", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        "0.2", ClassicalCalculationExecutorTest.value(result, "exact_exchange_fraction"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "perturbative_correlation_fraction"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        2.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "density_trace")),
        1.0E-8);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "xc_grid_integrated_electron_count"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_electron_count_error"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy"))));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "scf_validation_valid"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_density_trace_deviation"))
            <= 1.0E-7);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_density_symmetry_deviation"))
            <= 1.0E-10);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_mo_orthonormality_deviation"))
            <= 1.0E-8);
    Assertions.assertEquals(
        "NONE", ClassicalCalculationExecutorTest.table(result, "scf_diagnostic_codes").get(0));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "exchange_correlation_energy"))
            < 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL));
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().items().get(0).status());
    Assertions.assertEquals(
        "Kohn-Sham DFT single-point energy", result.coverageReport().items().get(0).model());
  }

  @Test
  void serviceExecutesDirectRestrictedHartreeFockWithoutMaterializedEriTensor() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest packedRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_rhf_packed_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final ClassicalCalculationRequest directRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_rhf_direct_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.text("scf_integral_mode", "direct"),
                ClassicalCalculationOption.text("direct_eri_screening_tolerance", "0.0")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult packedResult = service.execute(packedRequest);
    final ClassicalCalculationResult directResult = service.execute(directRequest);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, packedResult.status());
    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, directResult.status());
    Assertions.assertEquals(
        "DIRECT_HARTREE_FOCK",
        ClassicalCalculationExecutorTest.value(directResult, "preparation_mode"));
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, directResult.coverageReport().status());
    Assertions.assertEquals(
        "direct_gaussian_restricted_hartree_fock",
        directResult.coverageReport().items().get(0).model());
    Assertions.assertEquals(
        "gaussian_screened_cached",
        ClassicalCalculationExecutorTest.value(directResult, "direct_eri_provider"));
    Assertions.assertEquals(
        "0.0",
        ClassicalCalculationExecutorTest.value(directResult, "direct_eri_screening_tolerance"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(directResult, "direct_eri_value_queries"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    directResult, "direct_eri_computed_integrals"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(directResult, "direct_eri_cache_hit_ratio"))
            >= 0.0);
    Assertions.assertEquals(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(packedResult, "total_energy")),
        Double.parseDouble(ClassicalCalculationExecutorTest.value(directResult, "total_energy")),
        1.0E-10);
  }

  @Test
  void serviceExecutesDirectUnrestrictedHartreeFockWithoutMaterializedEriTensor() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ElectronicStructureMethod uhf =
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodName.of("UHF"),
            ElectronicStructureSpinTreatment.UNRESTRICTED);
    final ClassicalCalculationRequest packedRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_uhf_packed_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            uhf,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final ClassicalCalculationRequest directRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_uhf_direct_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            uhf,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(ClassicalCalculationOption.text("scf_integral_mode", "direct")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult packedResult = service.execute(packedRequest);
    final ClassicalCalculationResult directResult = service.execute(directRequest);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, packedResult.status());
    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, directResult.status());
    Assertions.assertEquals("UHF", ClassicalCalculationExecutorTest.value(directResult, "method"));
    Assertions.assertEquals(
        "DIRECT_HARTREE_FOCK",
        ClassicalCalculationExecutorTest.value(directResult, "preparation_mode"));
    Assertions.assertEquals(
        "gaussian_screened_cached",
        ClassicalCalculationExecutorTest.value(directResult, "direct_eri_provider"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(directResult, "direct_eri_value_queries"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    directResult, "direct_eri_computed_integrals"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(directResult, "direct_eri_cache_capacity"))
            > 0.0);
    Assertions.assertEquals(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(packedResult, "total_energy")),
        Double.parseDouble(ClassicalCalculationExecutorTest.value(directResult, "total_energy")),
        1.0E-10);
    Assertions.assertEquals(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(packedResult, "spin_squared")),
        Double.parseDouble(ClassicalCalculationExecutorTest.value(directResult, "spin_squared")),
        1.0E-10);
  }

  @Test
  void openShellHamiltonianPreparationUsesExplicitUhfOrbitalChannelPolicy() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ElectronicStructureMethod uhf =
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodName.of("UHF"),
            ElectronicStructureSpinTreatment.UNRESTRICTED);
    final ClassicalCalculationRequest alphaRequest =
        ClassicalCalculationExecutorTest.uhfHydrogenAtomHamiltonianRequest(
            "hydrogen_atom_alpha_channel_hamiltonian", uhf, "alpha");
    final ClassicalCalculationRequest betaRequest =
        ClassicalCalculationExecutorTest.uhfHydrogenAtomHamiltonianRequest(
            "hydrogen_atom_beta_channel_hamiltonian", uhf, "beta");
    final ClassicalCalculationRequest averageRequest =
        ClassicalCalculationExecutorTest.uhfHydrogenAtomHamiltonianRequest(
            "hydrogen_atom_average_channel_hamiltonian", uhf, "average");

    final double alphaFirstIntegral =
        pipeline.prepare(alphaRequest).hamiltonian().oneElectronIntegrals().get(0).value();
    final double betaFirstIntegral =
        pipeline.prepare(betaRequest).hamiltonian().oneElectronIntegrals().get(0).value();
    final double averageFirstIntegral =
        pipeline.prepare(averageRequest).hamiltonian().oneElectronIntegrals().get(0).value();

    Assertions.assertTrue(Double.isFinite(alphaFirstIntegral));
    Assertions.assertTrue(Double.isFinite(betaFirstIntegral));
    Assertions.assertTrue(Double.isFinite(averageFirstIntegral));
    Assertions.assertEquals(alphaFirstIntegral, betaFirstIntegral, 1.0E-8);
    Assertions.assertEquals(alphaFirstIntegral, averageFirstIntegral, 1.0E-8);
  }

  @Test
  void openShellHamiltonianPreparationSupportsUnrestrictedKohnShamDft() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_unrestricted_pbe_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.STO_3G,
            ActiveSpace.of(1, 1),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.integer("active_space_first_orbital", 0),
                ClassicalCalculationOption.text("uhf_hamiltonian_orbital_channel", "alpha")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "UNRESTRICTED_KOHN_SHAM_DFT",
        ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertEquals(
        "OPEN_SHELL_DFT", ClassicalCalculationExecutorTest.value(result, "spin_sector"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "fermion_operator_ready"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "qubit_mapping_ready"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "reference_total_energy"))));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "exchange_correlation_energy"))
            < 0.0);
    Assertions.assertEquals("1.0", ClassicalCalculationExecutorTest.value(result, "density_trace"));
  }

  @Test
  void serviceExecutesOpenShellDftSinglePointThroughUnrestrictedKohnSham() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_unrestricted_pbe_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "DFT_KOHN_SHAM", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("PBE", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "UNRESTRICTED_KOHN_SHAM_DFT",
        ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "unrestricted_reference"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertEquals(
        "OPEN_SHELL_DFT", ClassicalCalculationExecutorTest.value(result, "spin_sector"));
    Assertions.assertEquals("1.0", ClassicalCalculationExecutorTest.value(result, "density_trace"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "xc_grid_integrated_electron_count"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_electron_count_error"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy"))));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "scf_validation_valid"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_density_trace_deviation"))
            <= 1.0E-7);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_density_symmetry_deviation"))
            <= 1.0E-10);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_mo_orthonormality_deviation"))
            <= 1.0E-8);
    Assertions.assertEquals(
        "NONE", ClassicalCalculationExecutorTest.table(result, "scf_diagnostic_codes").get(0));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "alpha_orbital_energies").isEmpty());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "beta_orbital_energies").isEmpty());
  }

  @Test
  void serviceExecutesHydrogenSto3gGridLdaSinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_lda_grid_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("LDA_X"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "DFT_KOHN_SHAM", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("LDA_X", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "LOCAL_DENSITY", ClassicalCalculationExecutorTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "ATOM_CENTERED_GRID_SLATER_EXCHANGE_KOHN_SHAM",
        ClassicalCalculationExecutorTest.value(result, "functional_solver_model"));
    Assertions.assertEquals(
        "JAVA_NATIVE_LOCAL_DENSITY_GRID_COMPLETE",
        ClassicalCalculationExecutorTest.value(result, "parity_level"));
    Assertions.assertEquals(
        "complete_local_density_grid",
        ClassicalCalculationExecutorTest.value(result, "java_native_grid_contract"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "grid_model_available"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "scf_validation_valid"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_density_trace_deviation"))
            <= 1.0E-7);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_mo_orthonormality_deviation"))
            <= 1.0E-8);
    Assertions.assertEquals(
        2.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "density_trace")),
        1.0E-7);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_integrated_electron_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_electron_count_error"))
            < 2.0);
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "correlation_energy"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "exchange_energy"))
            < 0.0);
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, result.coverageReport().items().get(0).status());
  }

  @Test
  void serviceExecutesHydrogenSto3gGridSvwnSinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_svwn_grid_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("SVWN"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "DFT_KOHN_SHAM", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("SVWN", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "LOCAL_DENSITY", ClassicalCalculationExecutorTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "ATOM_CENTERED_GRID_SVWN_KOHN_SHAM",
        ClassicalCalculationExecutorTest.value(result, "functional_solver_model"));
    Assertions.assertEquals(
        "JAVA_NATIVE_LOCAL_DENSITY_GRID_COMPLETE",
        ClassicalCalculationExecutorTest.value(result, "parity_level"));
    Assertions.assertEquals(
        "complete_local_density_grid",
        ClassicalCalculationExecutorTest.value(result, "java_native_grid_contract"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "grid_model_available"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "scf_validation_valid"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_density_trace_deviation"))
            <= 1.0E-7);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "scf_mo_orthonormality_deviation"))
            <= 1.0E-8);
    Assertions.assertEquals(
        2.0,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "density_trace")),
        1.0E-7);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_integrated_electron_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_electron_count_error"))
            < 2.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "exchange_energy"))
            < 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "correlation_energy"))
            < 0.0);
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, result.coverageReport().items().get(0).status());
  }

  @Test
  void serviceExecutesHydrogenSto3gGridPbeSinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_pbe_grid_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "DFT_KOHN_SHAM", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("PBE", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "GENERALIZED_GRADIENT",
        ClassicalCalculationExecutorTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "ATOM_CENTERED_GRID_PBE_GGA_KOHN_SHAM",
        ClassicalCalculationExecutorTest.value(result, "functional_solver_model"));
    Assertions.assertEquals(
        "JAVA_NATIVE_GGA_APPROXIMATE",
        ClassicalCalculationExecutorTest.value(result, "parity_level"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "grid_model_available"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_integrated_electron_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "xc_grid_electron_count_error"))));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "exchange_energy"))
            < 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "correlation_energy"))
            < 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL));
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().items().get(0).status());
  }

  @Test
  void serviceExecutesHydrogenSto3gGridPbe0SinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_pbe0_grid_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("PBE0"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "DFT_KOHN_SHAM", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("PBE0", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "HYBRID_GGA", ClassicalCalculationExecutorTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "ATOM_CENTERED_GRID_PBE_GGA_KOHN_SHAM",
        ClassicalCalculationExecutorTest.value(result, "functional_solver_model"));
    Assertions.assertEquals(
        "JAVA_NATIVE_HYBRID_APPROXIMATE",
        ClassicalCalculationExecutorTest.value(result, "parity_level"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "grid_model_available"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "converged"));
    Assertions.assertEquals(
        "0.25", ClassicalCalculationExecutorTest.value(result, "exact_exchange_fraction"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "exchange_energy"))
            < 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "correlation_energy"))
            < 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL));
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().items().get(0).status());
  }

  @Test
  void serviceCarriesCustomDensityFunctionalDefinitionIntoDftResult() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final DensityFunctionalDefinition functionalDefinition =
        DensityFunctionalDefinition.of(
            "Custom-B2",
            DensityFunctionalFamily.DOUBLE_HYBRID,
            0.53,
            0.27,
            true,
            true,
            "Custom double-hybrid functional for regression checks");
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_custom_dft_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional(functionalDefinition),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "Custom-B2", ClassicalCalculationExecutorTest.value(result, "functional"));
    Assertions.assertEquals(
        "DOUBLE_HYBRID", ClassicalCalculationExecutorTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "gradient_corrected"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "dispersion_corrected"));
    Assertions.assertEquals(
        "0.53", ClassicalCalculationExecutorTest.value(result, "exact_exchange_fraction"));
    Assertions.assertEquals(
        "0.27",
        ClassicalCalculationExecutorTest.value(result, "perturbative_correlation_fraction"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy"))));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL));
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().items().get(0).status());
  }

  @Test
  void kohnShamDftDoesNotBypassUnsupportedBasisValidation() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_dft_unsupported_basis"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.of(
                ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName.of("def2-TZVP")),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.UNSUPPORTED, result.status());
    Assertions.assertTrue(result.sections().isEmpty());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED));
    Assertions.assertEquals(ClassicalCoverageStatus.UNSUPPORTED, result.coverageReport().status());
    Assertions.assertEquals(
        "not_executed_single_point_energy", result.coverageReport().items().get(0).model());
  }

  @Test
  void singlePointEnergyDoesNotFakeUnsupportedBasisResult() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_unsupported_basis"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.of(
                ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName.of("def2-TZVP")),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.UNSUPPORTED, result.status());
    Assertions.assertTrue(result.sections().isEmpty());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED));
  }

  @Test
  void serviceExecutesHydrogenSto3gMp2SinglePointEnergyThroughJavaNativePostHf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_mp2_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.PERTURBATION_THEORY,
                ElectronicStructureMethodName.of("MP2")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("MP2", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "closed_shell_mp2", ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "MP2", ClassicalCalculationExecutorTest.value(result, "posthf_solver_family"));
    Assertions.assertEquals(
        "PERTURBATIVE", ClassicalCalculationExecutorTest.value(result, "posthf_correlation_level"));
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "posthf_reference_family"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "posthf_variational_bound"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "posthf_exact_diagonalization"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "java_native_posthf"));
    Assertions.assertEquals(
        "java_native_mp2",
        ClassicalCalculationExecutorTest.value(result, "posthf_solver_contract"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        -0.013170766469969,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "correlation_energy")),
        2.0E-5);
    Assertions.assertEquals(
        -1.129855153555309,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        5.0E-5);
  }

  @Test
  void serviceExecutesCarbonAtomSto3gMp2ThroughJavaNativeUnrestrictedPostHf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_atom_ump2_single_point"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.PERTURBATION_THEORY,
                ElectronicStructureMethodName.of("MP2")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("MP2", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "unrestricted_spin_orbital_mp2",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "UMP2", ClassicalCalculationExecutorTest.value(result, "posthf_solver_family"));
    Assertions.assertEquals(
        "PERTURBATIVE", ClassicalCalculationExecutorTest.value(result, "posthf_correlation_level"));
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "posthf_reference_family"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "posthf_variational_bound"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "posthf_exact_diagonalization"));
    Assertions.assertEquals(
        "java_native_ump2",
        ClassicalCalculationExecutorTest.value(result, "posthf_solver_contract"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        2.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "reference_spin_squared")),
        1.0E-10);
    Assertions.assertEquals(
        0.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "reference_spin_contamination")),
        1.0E-10);
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "alpha_occupied_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "beta_occupied_orbital_count"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "correlation_energy"))
            < 0.0);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy"))));
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, result.coverageReport().items().get(0).status());
  }

  @Test
  void serviceRejectsUnrestrictedMp2WhenDenseSpinBlockKernelExceedsConfiguredLimit() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_atom_ump2_dense_guard"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.PERTURBATION_THEORY,
                ElectronicStructureMethodName.of("MP2")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(ClassicalCalculationOption.integer("max_ump2_dense_spin_orbitals", 1)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertTrue(result.unsupported(), result.toString());
    Assertions.assertEquals(0, result.sectionCount());
    Assertions.assertTrue(result.diagnostics().hasWarnings());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
        result.diagnostics().diagnostics().get(0).code());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().get(0).message().contains("dense spin-block kernel"),
        result.diagnostics().diagnostics().get(0).message());
    Assertions.assertEquals(ClassicalCoverageStatus.UNSUPPORTED, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.UNSUPPORTED, result.coverageReport().items().get(0).status());
  }

  @Test
  void serviceExecutesHydrogenSto3gCcdSinglePointEnergyThroughJavaNativePostHf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_ccd_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCD")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("ccd_max_iterations", 32),
                ClassicalCalculationOption.floating("ccd_energy_tolerance", 1.0E-8),
                ClassicalCalculationOption.floating("ccd_damping", 0.25)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("CCD", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "closed_shell_ccd_two_electron_fci_equivalent",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "correlation_energy"))));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "determinant_count"))
            >= 1.0);
  }

  @Test
  void serviceExecutesHydrogenSto3gCcsdSinglePointEnergyThroughTwoElectronExactShortcut() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_ccsd_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("CCSD", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "closed_shell_ccsd_two_electron_fci_equivalent",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "FCI_EQUIVALENT_CC",
        ClassicalCalculationExecutorTest.value(result, "posthf_solver_family"));
    Assertions.assertEquals(
        "EXACT_DIAGONALIZATION",
        ClassicalCalculationExecutorTest.value(result, "posthf_correlation_level"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "posthf_variational_bound"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "posthf_exact_diagonalization"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "determinant_count"));
    Assertions.assertEquals("1.0", ClassicalCalculationExecutorTest.value(result, "root_count"));
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, result.coverageReport().items().get(0).status());
  }

  @Test
  void openShellSmallSpaceCcsdUsesExactDeterminantEquivalence() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_open_shell_ccsd_single_point"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        "Open-shell small-space CCSD exact single-point energy",
        result.coverageReport().items().get(0).model());
    Assertions.assertEquals("CCSD", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "open_shell_small_space_ccsd_fci_equivalent",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "EXACT_DIAGONALIZATION",
        ClassicalCalculationExecutorTest.value(result, "posthf_correlation_level"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "posthf_exact_diagonalization"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "determinant_count"));
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED),
        result.diagnostics().diagnostics().toString());
  }

  @Test
  void serviceExecutesBerylliumSto3gCcsdWithIterationTrace() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("beryllium_ccsd_single_point"),
            ClassicalCalculationExecutorTest.berylliumAtom(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("ccd_max_iterations", 24),
                ClassicalCalculationOption.floating("ccd_energy_tolerance", 1.0E-8),
                ClassicalCalculationOption.floating("ccd_damping", 0.25)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertTrue(
        result.status() == ClassicalCalculationStatus.SUCCEEDED
            || result.status() == ClassicalCalculationStatus.PARTIAL);
    Assertions.assertEquals("CCSD", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "determinant_space_projective_ccsd",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "PROJECTIVE_COUPLED_CLUSTER",
        ClassicalCalculationExecutorTest.value(result, "posthf_correlation_level"));
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "java_native_ccsd",
        ClassicalCalculationExecutorTest.value(result, "posthf_solver_contract"));
    Assertions.assertEquals(
        "determinant_space_projective_solution",
        ClassicalCalculationExecutorTest.value(result, "singles_amplitude_policy"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "production_parity"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "iterations")) >= 1.0);
    Assertions.assertEquals(
        ClassicalCalculationExecutorTest.value(result, "iterations"),
        ClassicalCalculationExecutorTest.value(result, "iteration_history_count"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "last_energy_delta"))));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "last_amplitude_delta_norm"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_amplitude_delta_norm"))
            >= 0.0);
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "virtual_orbital_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "singles_amplitude_count"));
    Assertions.assertEquals(
        "36.0", ClassicalCalculationExecutorTest.value(result, "doubles_amplitude_count"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "ccsd_iteration_history").size() >= 2);
  }

  @Test
  void serviceExecutesHydrogenSto3gFciSinglePointEnergyThroughJavaNativePostHf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_fci_single_point"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
                ElectronicStructureMethodName.of("FCI")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("FCI", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "full_configuration_interaction",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "FCI", ClassicalCalculationExecutorTest.value(result, "posthf_solver_family"));
    Assertions.assertEquals(
        "EXACT_DIAGONALIZATION",
        ClassicalCalculationExecutorTest.value(result, "posthf_correlation_level"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "posthf_variational_bound"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "posthf_exact_diagonalization"));
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "determinant_count"));
    Assertions.assertEquals("1.0", ClassicalCalculationExecutorTest.value(result, "root_count"));
    Assertions.assertEquals(
        -1.137265043874881,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        1.0E-5);
    Assertions.assertEquals(
        -1.850587911902053,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "electronic_energy")),
        1.0E-5);
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "total_energies").size());
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "electronic_energies").size());
  }

  @Test
  void serviceExecutesExplicitPostHfCorrelationSuiteThroughCalculationKinds() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_explicit_posthf_suite"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(
                ClassicalCalculationKind.MP2_CORRELATION,
                ClassicalCalculationKind.CISD_CORRELATION,
                ClassicalCalculationKind.CCSD_CORRELATION,
                ClassicalCalculationKind.FCI_DIAGONALIZATION,
                ClassicalCalculationKind.POST_HF_CORRELATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.MP2_CORRELATION));
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.CISD_CORRELATION));
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.CCSD_CORRELATION));
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.FCI_DIAGONALIZATION));
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.POST_HF_CORRELATION));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.MP2_CORRELATION)
            .requireFirstValue("mp2_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.CISD_CORRELATION)
            .requireFirstValue("cisd_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.CCSD_CORRELATION)
            .requireFirstValue("ccsd_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.FCI_DIAGONALIZATION)
            .requireFirstValue("fci_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.POST_HF_CORRELATION)
            .requireFirstValue("mp2_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.POST_HF_CORRELATION)
            .requireFirstValue("ccsd_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.POST_HF_CORRELATION)
            .requireFirstValue("cisd_available"));
    Assertions.assertEquals(
        "true",
        result
            .requireSection(ClassicalCalculationKind.POST_HF_CORRELATION)
            .requireFirstValue("fci_available"));
    Assertions.assertEquals(
        "exact",
        result
            .requireSection(ClassicalCalculationKind.MP2_CORRELATION)
            .requireFirstValue("quality_level"));
    Assertions.assertEquals(
        "closed_shell_mp2",
        result
            .requireSection(ClassicalCalculationKind.MP2_CORRELATION)
            .requireFirstValue("quality_model"));
    Assertions.assertEquals(
        "exact",
        result
            .requireSection(ClassicalCalculationKind.CCSD_CORRELATION)
            .requireFirstValue("quality_level"));
    Assertions.assertEquals(
        "determinant_space_cisd",
        result
            .requireSection(ClassicalCalculationKind.CISD_CORRELATION)
            .requireFirstValue("quality_model"));
    Assertions.assertEquals(
        "determinant_space_fci",
        result
            .requireSection(ClassicalCalculationKind.FCI_DIAGONALIZATION)
            .requireFirstValue("quality_model"));
    Assertions.assertEquals(
        "exact",
        result
            .requireSection(ClassicalCalculationKind.POST_HF_CORRELATION)
            .requireFirstValue("quality_level"));
    Assertions.assertEquals(
        "post_hartree_fock_suite",
        result
            .requireSection(ClassicalCalculationKind.POST_HF_CORRELATION)
            .requireFirstValue("quality_model"));
    Assertions.assertTrue(
        result
                .requireSection(ClassicalCalculationKind.MP2_CORRELATION)
                .requireScalar("mp2_correlation_energy")
            < 0.0);
    Assertions.assertTrue(
        result
                .requireSection(ClassicalCalculationKind.FCI_DIAGONALIZATION)
                .requireScalar("fci_ground_total_energy")
            < 0.0);
  }

  @Test
  void serviceExecutesSpinSectorMultiRootFciThroughTypedOptions() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_fci_spin_sector_roots"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
                ElectronicStructureMethodName.of("FCI")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("fci_root_count", 2),
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "determinant_count"));
    Assertions.assertEquals("2.0", ClassicalCalculationExecutorTest.value(result, "root_count"));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "total_energies").size());
    Assertions.assertEquals(
        -1.137265043874881,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        1.0E-5);
  }

  @Test
  void serviceExecutesCasscfWithOrbitalOptimizationTrace() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_casscf_orbital_trace"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
                ElectronicStructureMethodName.of("CASSCF")),
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "active_space_casscf_orbital_optimization",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "ACTIVE_EXTERNAL",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_space"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "first_active_orbital"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_space_electrons"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_space_orbitals"));
    Assertions.assertEquals(
        "0.0",
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_energy_improvement"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_orbital_orthonormality_error"))
            < 1.0E-8);
    Assertions.assertEquals(
        "0.0",
        ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_energy_improvement"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_gradient"));
    Assertions.assertEquals(
        "iteration|evaluated_rotations|accepted_rotations|step_rad|energy_hartree|delta_hartree|converged",
        ClassicalCalculationExecutorTest.table(result, "orbital_optimization_history").get(0));
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        -1.137265043874881,
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")),
        1.0E-5);
  }

  @Test
  void serviceExecutesCasscfWithAllNonRedundantOrbitalRotations() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_631g_casscf_all_rotations"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
                ElectronicStructureMethodName.of("CASSCF")),
            BasisSet.SIX_31G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 64),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
                ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
                ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
                ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "ALL_NON_REDUNDANT",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_space"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "evaluated_orbital_rotations"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "orbital_optimization_energy_improvement"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_orbital_orthonormality_error"))
            < 1.0E-8);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "max_residual_orbital_energy_improvement"))
            < 1.0E-6,
        "residualEnergy="
            + ClassicalCalculationExecutorTest.value(
                result, "max_residual_orbital_energy_improvement")
            + ", residualGradient="
            + ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_gradient")
            + ", history="
            + ClassicalCalculationExecutorTest.table(result, "orbital_optimization_history"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_gradient"))
            < 2.0E-3,
        "residualGradient="
            + ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_gradient")
            + ", residualEnergy="
            + ClassicalCalculationExecutorTest.value(
                result, "max_residual_orbital_energy_improvement")
            + ", history="
            + ClassicalCalculationExecutorTest.table(result, "orbital_optimization_history"));
    Assertions.assertEquals(
        "PRODUCTION",
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_quality"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_stop_reason")
            .isBlank());
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_pair_coverage_ratio"));
    Assertions.assertEquals(
        "rotation_space|inactive_orbitals|active_orbitals|external_orbitals|available_pairs|evaluated_rotations|accepted_rotations|coverage_ratio|quality|stop_reason",
        ClassicalCalculationExecutorTest.table(result, "orbital_rotation_coverage").get(0));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "orbital_rotation_coverage")
            .get(1)
            .contains("ALL_NON_REDUNDANT"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "orbital_optimization_history").size() > 1);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")) < -1.13);
  }

  @Test
  void serviceExecutesCasscfWithPairLimitedAllNonRedundantOrbitalRotations() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_631g_casscf_pair_limited_all_rotations"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
                ElectronicStructureMethodName.of("CASSCF")),
            BasisSet.SIX_31G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 32),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 1),
                ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
                ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
                ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "ALL_NON_REDUNDANT",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_space"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "evaluated_orbital_rotations"))
            > 6.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "accepted_orbital_rotations"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_orbital_orthonormality_error"))
            < 1.0E-8);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "max_residual_orbital_energy_improvement"))
            < 1.0E-6);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_gradient"))
            < 1.0E-4,
        "max_residual_orbital_gradient="
            + ClassicalCalculationExecutorTest.value(result, "max_residual_orbital_gradient")
            + ", max_residual_orbital_energy_improvement="
            + ClassicalCalculationExecutorTest.value(
                result, "max_residual_orbital_energy_improvement")
            + ", history="
            + ClassicalCalculationExecutorTest.table(result, "orbital_optimization_history"));
    Assertions.assertEquals(
        "PRODUCTION",
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_quality"));
    Assertions.assertEquals(
        "RESIDUAL_AND_ENERGY_CONVERGED",
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_stop_reason"));
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_pair_coverage_ratio"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "orbital_rotation_coverage")
            .get(1)
            .contains("ALL_NON_REDUNDANT"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "orbital_optimization_history").size() > 3);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_energy")) < -1.13);
  }

  @Test
  void servicePreparesHydrogenElectronicHamiltonianThroughJavaNativePipeline() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_hamiltonian_preparation"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "total_integral_count"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertEquals(
        "fermion_qubit_mapping",
        ClassicalCalculationExecutorTest.value(result, "quantum_preparation_contract"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "fermion_operator_ready"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "qubit_mapping_ready"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_contains_one_electron_terms"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_contains_two_electron_terms"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "mapping_electron_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "mapping_qubit_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "mapping_integral_count"));
    Assertions.assertEquals(
        "CLOSED_SHELL", ClassicalCalculationExecutorTest.value(result, "spin_sector"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "active_space_requested"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "fermion_slater_reference_energy"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "fermion_slater_reference_energy_deviation"))));
  }

  @Test
  void servicePreparesSemiEmpiricalValenceHamiltonianForHeteroHalogenatedOrganicMolecule() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heteroaryl_semiempirical_hamiltonian"),
            ClassicalCalculationExecutorTest.heteroarylBromideProbe(),
            ElectronicStructureMethod.SEMI_EMPIRICAL,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "SEMI_EMPIRICAL_VALENCE",
        ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "native-valence-semiempirical",
        ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "fermion_operator_ready"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "qubit_mapping_ready"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "basis_orbital_count"))
            > 6.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "electron_count")) > 2.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "one_electron_integral_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "two_electron_integral_count"))
            > 0.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "preparation_limitation_reason")
            .contains("molecular graph"));
  }

  @Test
  void servicePreparesSemiEmpiricalValenceQuantumProgramForHeteroHalogenatedOrganicMolecule() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heteroaryl_semiempirical_quantum_program"),
            ClassicalCalculationExecutorTest.heteroarylBromideProbe(),
            ElectronicStructureMethod.SEMI_EMPIRICAL,
            BasisSet.STO_3G,
            ActiveSpace.of(4, 4),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("quantum_program_kinds", "PAULI_EVOLUTION"),
                ClassicalCalculationOption.floating(
                    "quantum_program_verification_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION));
    Assertions.assertEquals(
        "SEMI_EMPIRICAL_VALENCE",
        ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "native-valence-semiempirical",
        ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "requested_mapping_verified"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "fermion_term_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "requested_mapping_pauli_term_count"))
            > 0.0);
  }

  @Test
  @Disabled(
      "Manual heavy direct-ERI regression; requires Schwarz screening/density fitting before default Maven.")
  void servicePreparesLargeClosedShellActiveSpaceHamiltonianThroughDirectWindowTransform() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("large_heterocycle_direct_active_hamiltonian"),
            ClassicalCalculationExecutorTest.largeClosedShellCarbonNitrogenHeterocycle(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            ActiveSpace.of(4, 4),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 38)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "preparation_limitation_reason")
            .contains("Direct active-space Hamiltonian skipped full AO ERI tensor"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "two_electron_integral_count"))
            > 0.0);
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL));
  }

  @Test
  void preflightPromotesResourceLimitedActiveSpaceToExactDirectHamiltonianRoute() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("resource_limited_direct_active_preflight"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            ClassicalCalculationExecutorTest.largeHydrogenStructuredBasis(64),
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.integer("active_space_first_orbital", 1),
                ClassicalCalculationOption.integer("max_direct_mo_pair_products", 250_000_000)),
            ChemistryMetadata.EMPTY);

    final ElectronicStructurePreparationPreflight preflight = pipeline.preflight(request);

    Assertions.assertTrue(preflight.supported(), preflight.unsupportedReason());
    Assertions.assertTrue(preflight.directActiveSpaceSupported());
    Assertions.assertTrue(preflight.hasFullTensorPreflight());
    Assertions.assertTrue(preflight.hasOneElectronPreflight());
    Assertions.assertEquals("", preflight.unsupportedReason());
    Assertions.assertNotEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.fullTensorPreflight().status());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.oneElectronPreflight().status());
    Assertions.assertTrue(
        preflight.directActiveSpaceDescription().contains("Exact direct active-space Hamiltonian"),
        preflight.directActiveSpaceDescription());
    Assertions.assertTrue(pipeline.canPrepareDirectActiveSpaceHamiltonian(request));
  }

  @Test
  void servicePreparesExplicitDirectActiveSpaceHamiltonianWithoutFullTensorMaterialization() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("resource_limited_direct_active_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            ClassicalCalculationExecutorTest.largeHydrogenStructuredBasis(4),
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("scf_integral_mode", "direct"),
                ClassicalCalculationOption.integer("active_space_first_orbital", 1),
                ClassicalCalculationOption.integer("rhf_max_iterations", 1)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        "sections="
            + result.sections().size()
            + ", diagnostics="
            + result.diagnostics().diagnostics());
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "reference_converged"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "preparation_limitation_reason")
            .contains("Direct active-space Hamiltonian skipped full AO ERI tensor"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "fermion_qubit_mapping",
        ClassicalCalculationExecutorTest.value(result, "quantum_preparation_contract"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "active_space_requested"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_space_electrons"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_space_orbitals"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "active_space_spin_orbitals"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "active_space_first_orbital"));
  }

  @Test
  void serviceDoesNotUseOneElectronHamiltonianFallbackWithoutExplicitOption() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("resource_limited_hamiltonian_requires_explicit_fallback"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            ClassicalCalculationExecutorTest.largeHydrogenStructuredBasis(64),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.UNSUPPORTED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.UNSUPPORTED, result.coverageReport().status());
    Assertions.assertFalse(result.hasSection(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ClassicalDiagnosticCode.RESULT_FAILED
                        && diagnostic
                            .message()
                            .contains("hamiltonian_allow_one_electron_fallback=true")),
        result.diagnostics().toString());
  }

  @Test
  void serviceMarksExplicitOneElectronHamiltonianFallbackAsIncompleteCorrelationModel() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(
                "resource_limited_hamiltonian_explicit_one_electron_fallback"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            ClassicalCalculationExecutorTest.largeHydrogenStructuredBasis(64),
            null,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.bool("hamiltonian_allow_one_electron_fallback", true)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        "ONE_ELECTRON_ONLY",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "one_electron_fallback_explicitly_allowed"));
    Assertions.assertEquals(
        "false",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_contains_two_electron_terms"));
    Assertions.assertEquals(
        "false",
        ClassicalCalculationExecutorTest.value(result, "electron_correlation_terms_complete"));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL
                        && diagnostic
                            .message()
                            .contains("Full two-electron Hamiltonian preparation is unavailable")),
        result.diagnostics().toString());
  }

  @Test
  void servicePreparesHydrogenHamiltonianFromSelfContainedCustomBasis() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final BasisSet customBasis =
        BasisSet.defined(
            BasisSetName.of("custom-h2-single-s"),
            String.join(
                "\n",
                "H S",
                "  3.42525091D+00  0.1543289673",
                "  0.62391373D+00  0.5353281423",
                "  0.16885540D+00  0.4446345422",
                "****"));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_custom_basis_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            customBasis,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "custom-h2-single-s", ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "total_integral_count"));
  }

  @Test
  void servicePreparesHydrogenHamiltonianFromStructuredCustomBasis() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final BasisSet customBasis =
        BasisSet.structured(
            BasisSetName.of("structured-h-single-s"),
            BasisSetDefinition.of(
                List.of(
                    BasisAtomDefinition.of(
                        ElementSymbol.of("H"),
                        List.of(
                            BasisShell.of(
                                BasisShellAngularMomentum.S,
                                List.of(
                                    BasisPrimitive.of(3.42525091, 0.1543289673),
                                    BasisPrimitive.of(0.62391373, 0.5353281423),
                                    BasisPrimitive.of(0.16885540, 0.4446345422))))))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_structured_basis_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            customBasis,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "structured-h-single-s", ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "basis_atom_definition_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "basis_shell_count"));
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "basis_primitive_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "basis_cartesian_function_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "basis_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
  }

  @Test
  void servicePreparesDOrbitalFullTwoElectronHamiltonian() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final BasisSet customBasis =
        BasisSet.defined(
            BasisSetName.of("custom-carbon-sd-shell"),
            String.join(
                "\n",
                "C S",
                "  0.7100000000  1.0000000000",
                "C D",
                "  0.5500000000  1.0000000000",
                "****"));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_custom_sd_one_electron_hamiltonian"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            customBasis,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "7.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "14.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "OPEN_SHELL", ClassicalCalculationExecutorTest.value(result, "spin_sector"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "fermion_operator_ready"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "qubit_mapping_ready"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "two_electron_integral_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_integral_count"))
            > 0.0);
  }

  @Test
  @Disabled("Manual heavy Java-native regression; keep outside default Maven test.")
  void servicePreparesGOrbitalFullTwoElectronHamiltonian() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final BasisSet customBasis =
        BasisSet.defined(
            BasisSetName.of("custom-carbon-g-shell"),
            String.join("\n", "C G", "  0.2900000000  1.0000000000", "****"));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_custom_g_one_electron_hamiltonian"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            customBasis,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "15.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "30.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "two_electron_integral_count"))
            > 0.0);
  }

  @Test
  void serviceSelectsActiveSpaceFromHartreeFockFrontierOrbitals() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_active_space_selection"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.ACTIVE_SPACE_SELECTION),
            List.of(
                ClassicalCalculationOption.integer("active_space_min_orbitals", 2),
                ClassicalCalculationOption.integer("active_space_max_orbitals", 2),
                ClassicalCalculationOption.integer("active_space_frontier_pair_count", 1),
                ClassicalCalculationOption.floating("active_space_energy_window_hartree", 0.5)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "frontier_energy_window", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "deterministic_frontier_orbital_selector_from_java_native_orbital_energies",
        ClassicalCalculationExecutorTest.value(result, "selection_contract"));
    Assertions.assertEquals(
        "computed_orbital_spectrum_not_experimental_outcome_lookup",
        ClassicalCalculationExecutorTest.value(result, "selection_source"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "manual_review_required"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "active_space_selected"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "open_shell_reference"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "total_electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "total_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_orbital_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "first_active_orbital"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "exclusive_last_active_orbital"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_orbital_window_width"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "frontier_window_contains_homo"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "frontier_window_contains_lumo"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "selected_occupied_orbital_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "selected_virtual_orbital_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "active_electron_fraction"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "active_orbital_fraction"));
    Assertions.assertEquals(
        "HIGH", ClassicalCalculationExecutorTest.value(result, "selection_quality_band"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "selection_quality_score"))
            >= 0.9);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "homo_lumo_gap"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "selection_window_energy_span"))));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "selection_diagnostic_count"))
            >= 2.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "selection_diagnostics")
            .contains("frontier_occupied_coverage=1/1"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "selection_diagnostics")
            .contains("frontier_virtual_coverage=1/1"));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "orbital_window").size());
  }

  @Test
  void advancedSideProductAnalysisSeparatesBackgroundRiskFromProductCandidates() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("side_product_background_risk_contract"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS),
            List.of(),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertTrue(result.hasValue("side_product_rows"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "side_product_rows").stream()
            .anyMatch(row -> row.contains("unassigned_background_risk")),
        ClassicalCalculationExecutorTest.table(result, "side_product_rows").toString());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "side_product_rows").stream()
            .anyMatch(row -> row.contains("fallback_background")),
        ClassicalCalculationExecutorTest.table(result, "side_product_rows").toString());
  }

  @Test
  void advancedProductDistributionDoesNotUseDeclaredProductAsPredictionSeedByDefault() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_distribution_without_product_seed"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "product_side_seed_used"));
    Assertions.assertTrue(result.hasValue("product_distribution_rows"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(row -> row.contains("USER_SEED")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
  }

  @Test
  void advancedPredictionDrivenSectionsNeverUseDeclaredProductSeedByDefault() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final List<ClassicalCalculationKind> kinds =
        List.of(
            ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS,
            ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION,
            ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS,
            ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS,
            ClassicalCalculationKind.RETROSYNTHESIS_PLANNING);

    final ClassicalCalculationResult result =
        service.execute(
            ClassicalCalculationRequest.of(
                ClassicalCalculationId.of("advanced_prediction_no_product_seed_invariant"),
                reaction,
                ElectronicStructureMethod.HARTREE_FOCK,
                BasisSet.STO_3G,
                null,
                kinds,
                List.of(),
                ChemistryMetadata.EMPTY));

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalResultSection section = result.requireSection(kinds.get(i));
      Assertions.assertEquals(
          "false",
          section.requireFirstValue("product_side_seed_used"),
          kinds.get(i).name()
              + " must predict from reactants, conditions and generic rules by default.");
    }
  }

  @Test
  void advancedProductDistributionUsesDeclaredProductSeedOnlyWhenExplicitlyRequested() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_distribution_with_product_seed"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text(
                    "advanced_prediction_use_product_side_as_seed", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "product_side_seed_used"));
    Assertions.assertTrue(result.hasValue("product_distribution_rows"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(row -> row.contains("USER_SEED")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
  }

  @Test
  void advancedProductDistributionExposesElectronicCalibrationAndMechanismFactors() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_distribution_factor_trace"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text("advanced_prediction_electronic_rerank", "true"),
                ClassicalCalculationOption.text(
                    "advanced_prediction_apply_calibration_priors", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows =
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows");

    Assertions.assertTrue(rows.get(0).contains("electronic_weight"), rows.toString());
    Assertions.assertTrue(rows.get(0).contains("calibration_weight"), rows.toString());
    Assertions.assertTrue(rows.get(0).contains("primary_mechanism"), rows.toString());
    Assertions.assertTrue(rows.get(0).contains("source"), rows.toString());
    Assertions.assertTrue(
        rows.stream().skip(1).anyMatch(row -> row.contains("CALIBRATION_PRIOR")), rows.toString());
    Assertions.assertTrue(
        rows.stream().skip(1).anyMatch(row -> row.contains("ELECTRONIC_FACTOR")), rows.toString());
    Assertions.assertTrue(
        rows.stream().skip(1).anyMatch(row -> row.contains("rule_generated_graph")),
        rows.toString());
    Assertions.assertTrue(
        rows.stream().skip(1).anyMatch(row -> row.contains("NUCLEOPHILIC_AROMATIC_SUBSTITUTION")),
        rows.toString());
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_prediction_electronic_rerank_enabled"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_prediction_calibration_priors_enabled"));
    Assertions.assertTrue(
        result.requireScalar("advanced_calibration_library_observation_count") >= 0.0);
    Assertions.assertTrue(
        result.requireScalar("advanced_calibration_library_exact_observation_count") >= 0.0);
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_calibration_library_applied_to_prediction"));
    Assertions.assertTrue(
        result.requireScalar("advanced_prediction_calibration_evidence_weight") > 0.0);
    Assertions.assertTrue(
        result.requireScalar("advanced_prediction_explicit_product_graph_candidate_count") > 0.0);
    Assertions.assertTrue(
        result.requireScalar("advanced_prediction_mechanism_only_candidate_count") >= 0.0);
    Assertions.assertTrue(
        result.requireScalar("advanced_prediction_explicit_product_graph_coverage_ratio") > 0.0);
    Assertions.assertTrue(
        result.requireScalar("advanced_prediction_candidate_ready_for_electronic_rerank_count")
            > 0.0);
    Assertions.assertTrue(
        result.requireScalar("advanced_prediction_candidate_ready_for_quantum_workflow_count")
            > 0.0);
    final List<String> followupRows =
        ClassicalCalculationExecutorTest.table(result, "advanced_prediction_followup_rows");
    Assertions.assertTrue(followupRows.get(0).contains("next_action"), followupRows.toString());
    Assertions.assertTrue(
        followupRows.stream()
            .skip(1)
            .anyMatch(row -> row.contains("HAMILTONIAN_AND_QUANTUM_PROGRAM_PREP_READY")),
        followupRows.toString());
    Assertions.assertTrue(
        followupRows.stream().skip(1).anyMatch(row -> row.contains("REACTION_ENERGY_RERANK_READY")),
        followupRows.toString());
    Assertions.assertEquals(
        "ELECTRONIC_RERANK",
        ClassicalCalculationExecutorTest.value(result, "advanced_prediction_execution_mode"));
    Assertions.assertEquals(
        "false",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_prediction_electronic_rerank_opt_in_required"));
    Assertions.assertTrue(result.hasValue("advanced_prediction_product_graph_coverage_status"));
    Assertions.assertTrue(result.hasValue("advanced_prediction_product_graph_followup_reason"));
    Assertions.assertTrue(result.requireScalar("prediction_entropy") >= 0.0);
    Assertions.assertTrue(result.requireScalar("prediction_major_minor_gap") >= 0.0);
    Assertions.assertTrue(result.hasValue("prediction_distribution_shape"));
    Assertions.assertTrue(result.hasValue("prediction_close_product_competitor_count"));
  }

  @Test
  void advancedProductDistributionCanPrepareQuantumWorkflowForProductSideSeed() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("advanced_hydrogen_seed_quantum_workflow"),
            "Advanced hydrogen product-side quantum workflow",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogenAtom(),
                        StoichiometricCoefficient.of(2)))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(1.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.REDUCING_MEDIUM)),
                "small product-side seed quantum workflow preparation"),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_hydrogen_seed_quantum_workflow"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text(
                    "advanced_prediction_use_product_side_as_seed", "true"),
                ClassicalCalculationOption.text(
                    "advanced_prediction_prepare_quantum_workflows", "true"),
                ClassicalCalculationOption.text(
                    "advanced_prediction_prepare_quantum_workflows_max_candidates", "1"),
                ClassicalCalculationOption.text(
                    "advanced_prediction_prepare_quantum_workflows_max_participants", "1"),
                ClassicalCalculationOption.text(
                    "advanced_prediction_execute_quantum_workflow_vqe", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> quantumRows =
        ClassicalCalculationExecutorTest.table(
            result, "advanced_candidate_quantum_workflow_preparation");

    Assertions.assertFalse(result.failed(), result.diagnostics().toString());
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_candidate_quantum_workflow_preparation_enabled"));
    Assertions.assertTrue(
        quantumRows.get(0).contains("vqe_status")
            && quantumRows.get(0).contains("vqe_energy")
            && quantumRows.get(0).contains("workflow_status"),
        quantumRows.toString());
    Assertions.assertTrue(
        quantumRows.stream()
            .skip(1)
            .anyMatch(
                row ->
                    row.contains("\tprepared\t")
                        && row.contains("\ttrue\t")
                        && row.contains("\tCONVERGED\t")
                        && row.contains("VQE")
                        && !row.contains("\tmissing\t")),
        quantumRows.toString());
  }

  @Test
  void advancedProductDistributionDoesNotApplyCalibrationPriorsByDefault() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_distribution_default_without_calibration_prior"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows =
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows");

    Assertions.assertEquals(
        "false",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_prediction_calibration_priors_enabled"));
    Assertions.assertEquals(
        0.0, result.requireScalar("advanced_prediction_calibration_evidence_weight"), 0.0);
    Assertions.assertFalse(
        rows.stream().skip(1).anyMatch(row -> row.contains("CALIBRATION_PRIOR")), rows.toString());
  }

  @Test
  void advancedProductDistributionCanDisableSoftCalibrationPriors() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_distribution_without_calibration_prior"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text(
                    "advanced_prediction_apply_calibration_priors", "false")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows =
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows");

    Assertions.assertEquals(
        "false",
        ClassicalCalculationExecutorTest.value(
            result, "advanced_prediction_calibration_priors_enabled"));
    Assertions.assertEquals(
        0.0, result.requireScalar("advanced_prediction_calibration_evidence_weight"), 0.0);
    Assertions.assertFalse(
        rows.stream().skip(1).anyMatch(row -> row.contains("CALIBRATION_PRIOR")), rows.toString());
  }

  @Test
  void serviceSelectsActiveSpaceFromOpenShellKohnShamSpinResolvedOrbitals() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_atom_open_shell_dft_active_space_selection"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.ACTIVE_SPACE_SELECTION),
            List.of(
                ClassicalCalculationOption.integer("active_space_min_orbitals", 2),
                ClassicalCalculationOption.integer("active_space_max_orbitals", 4),
                ClassicalCalculationOption.integer("active_space_frontier_pair_count", 1),
                ClassicalCalculationOption.floating("active_space_energy_window_hartree", 0.25)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "frontier_energy_window", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "UNRESTRICTED_KOHN_SHAM_DFT",
        ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "spin_resolved_reference"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "open_shell_reference"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spin_imbalance"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "frontier_window_contains_homo"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "frontier_window_contains_lumo"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "active_orbital_count"))
            >= 2.0);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "homo_lumo_gap"))));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "spin_resolved_orbital_energies").isEmpty());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "orbital_window").isEmpty());
  }

  @Test
  void activeSpaceSelectorRetainsSinglyOccupiedOpenShellOrbitalUnderWindowLimit() {
    final ActiveSpaceSelector selector = new ActiveSpaceSelector();
    final ActiveSpaceSelectionResult result =
        selector.selectSpinResolved(
            5,
            new double[] {-2.0, -1.3, -0.4, -0.39, 0.2, 0.9},
            3,
            2,
            ActiveSpaceSelectionOptions.of(2, 1, 0.01, 2));

    Assertions.assertEquals(2, result.firstActiveOrbital());
    Assertions.assertEquals(4, result.exclusiveLastActiveOrbital());
    Assertions.assertEquals(1, result.activeSpace().electronCount());
    Assertions.assertEquals(2, result.activeSpace().orbitalCount());
    Assertions.assertTrue(
        result.selectionDiagnostics().contains("singly_occupied_orbital_window=2..2"));
    Assertions.assertTrue(
        result.selectionDiagnostics().contains("singly_occupied_orbitals_retained"));
    Assertions.assertTrue(result.orbitalRows().get(2).contains("SINGLY_OCCUPIED_ALPHA"));
  }

  @Test
  void activeSpaceSelectorPullsNearDegenerateAdjacentFrontierOrbitals() {
    final ActiveSpaceSelector selector = new ActiveSpaceSelector();
    final ActiveSpaceSelectionResult result =
        selector.select(
            6,
            new double[] {-1.5, -0.8, -0.20, -0.19, -0.185, 0.9},
            ActiveSpaceSelectionOptions.of(4, 1, 0.02, 2));

    Assertions.assertEquals(2, result.firstActiveOrbital());
    Assertions.assertEquals(5, result.exclusiveLastActiveOrbital());
    Assertions.assertEquals(2, result.activeSpace().electronCount());
    Assertions.assertEquals(3, result.activeSpace().orbitalCount());
    Assertions.assertTrue(result.orbitalRows().get(3).contains("LUMO"));
    Assertions.assertTrue(result.orbitalRows().get(4).contains("ACTIVE"));
  }

  @Test
  void activeSpaceSelectionReportsTruncatedFrontierCoverage() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_truncated_active_space_selection"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.ACTIVE_SPACE_SELECTION),
            List.of(
                ClassicalCalculationOption.integer("active_space_min_orbitals", 1),
                ClassicalCalculationOption.integer("active_space_max_orbitals", 1),
                ClassicalCalculationOption.integer("active_space_frontier_pair_count", 3),
                ClassicalCalculationOption.floating("active_space_energy_window_hartree", 0.0)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "LOW", ClassicalCalculationExecutorTest.value(result, "selection_quality_band"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "selection_quality_score"))
            < 0.65);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "selection_diagnostics")
            .contains("active_space_truncated_below_homo"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "selection_diagnostics")
            .contains("active_space_truncated_above_lumo"));
  }

  @Test
  void servicePreparesReducedActiveSpaceHamiltonianFromProjectedMolecularOrbitals() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_reduced_active_space_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 1),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("scf_integral_mode", "direct"),
                ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "RHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "one_electron_integral_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "two_electron_integral_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "nuclear_repulsion_energy"))));
    Assertions.assertEquals(
        "gaussian_screened_cached",
        ClassicalCalculationExecutorTest.value(result, "direct_eri_provider"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "direct_eri_value_queries"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "direct_eri_pair_bound_integrals"))
            > 0.0);
  }

  @Test
  void servicePreparesHamiltonianFromCasscfOptimizedOrbitals() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_casscf_optimized_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("hamiltonian_orbital_optimization", "casscf"),
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 4),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 2)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "CASSCF_ORBITAL_OPTIMIZED_RHF",
        result
            .requireSection(ClassicalCalculationKind.HAMILTONIAN_PREPARATION)
            .requireFirstValue("reference_method"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "orbital_optimized_reference"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_quality").isBlank());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_stop_reason")
            .isBlank());
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_pair_coverage_ratio"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "available_orbital_rotation_pair_count"))
            >= 0.0);
    Assertions.assertEquals(
        "fermion_qubit_mapping",
        ClassicalCalculationExecutorTest.value(result, "quantum_preparation_contract"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "fermion_operator_ready"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "qubit_mapping_ready"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "active_space_requested"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_space_electrons"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "active_space_orbitals"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "active_space_spin_orbitals"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "active_space_first_orbital"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "evaluated_orbital_rotations"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_orbital_orthonormality_error"))
            < 1.0E-8);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "max_residual_orbital_energy_improvement"))
            < 1.0E-6);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_integral_count"))
            > 0.0);
  }

  @Test
  void electronicPreparationCarriesCasscfOptimizedCoefficientsIntoHamiltonian() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_631g_pipeline_casscf_optimized_hamiltonian"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("hamiltonian_orbital_optimization", "casscf"),
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 12),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
                ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
                ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
                ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ElectronicStructurePreparationResult result = pipeline.prepare(request);

    Assertions.assertEquals(ElectronicStructurePreparationMode.FULL_TWO_ELECTRON, result.mode());
    Assertions.assertTrue(result.hasRhfReference());
    Assertions.assertTrue(result.hasCasscfResult());
    Assertions.assertTrue(result.hasAtomicOrbitalTensors());
    Assertions.assertTrue(result.hasMolecularOrbitalTensors());
    Assertions.assertEquals(2, result.hamiltonian().activeSpace().electronCount());
    Assertions.assertEquals(2, result.hamiltonian().activeSpace().orbitalCount());
    Assertions.assertEquals(4, result.hamiltonian().spinOrbitalCount());
    Assertions.assertFalse(result.hamiltonian().oneElectronIntegrals().isEmpty());
    Assertions.assertFalse(result.hamiltonian().twoElectronIntegrals().isEmpty());
    Assertions.assertTrue(result.casscfResult().evaluatedRotationCount() > 0);
    Assertions.assertTrue(result.casscfResult().orbitalOptimizationHistory().size() > 1);
    Assertions.assertTrue(result.casscfResult().totalEnergyImprovement() >= 0.0);
    Assertions.assertTrue(result.casscfResult().maxResidualOrbitalEnergyImprovement() < 1.0E-6);
    Assertions.assertTrue(result.casscfResult().maxResidualOrbitalGradient() < 2.0E-3);
    Assertions.assertTrue(
        OrbitalRotationOptimizer.maxOrthonormalityError(
                result.casscfResult().optimizedOrbitalCoefficientsCopy(),
                result.atomicOrbitalTensors().overlapCopy(),
                result.basisOrbitalCount())
            < 1.0E-8);
    Assertions.assertTrue(result.limitationReason().contains("CASSCF orbital optimization"));
  }

  @Test
  void serviceRunsCasscfOrbitalOptimizationAsFirstClassCalculation() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_casscf_explicit_orbital_optimization"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
                ElectronicStructureMethodName.of("CASSCF")),
            BasisSet.SIX_31G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 2),
            List.of(
                ClassicalCalculationKind.ORBITAL_OPTIMIZATION,
                ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 24),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
                ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
                ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
                ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8),
                ClassicalCalculationOption.text("hamiltonian_orbital_optimization", "casscf")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.ORBITAL_OPTIMIZATION));
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    Assertions.assertEquals(
        "active_space_casscf_orbital_optimization",
        ClassicalCalculationExecutorTest.value(result, "implemented_kernel"));
    Assertions.assertEquals(
        "ALL_NON_REDUNDANT",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_space"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "full_orbital_count"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "inactive_orbital_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "external_orbital_count"));
    Assertions.assertEquals(
        "4.0",
        ClassicalCalculationExecutorTest.value(result, "available_orbital_rotation_pair_count"));
    Assertions.assertEquals(
        "0 inactive / 2 active / 2 external",
        ClassicalCalculationExecutorTest.value(result, "orbital_partition"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "evaluated_orbital_rotations"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "accepted_orbital_rotations"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "max_orbital_orthonormality_error"))
            < 1.0E-8);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "optimized_total_energy"))));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "orbital_energy_monotonic_nonincreasing"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "orbital_rotation_acceptance_ratio"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "orbital_history_row_count"))
            >= 1.0);
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "orbital_residual_status").isBlank());
    Assertions.assertEquals(
        "CASSCF_ORBITAL_OPTIMIZED_RHF",
        result
            .requireSection(ClassicalCalculationKind.HAMILTONIAN_PREPARATION)
            .requireFirstValue("reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "orbital_optimized_reference"));
    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, result.coverageReport().items().get(0).status());
  }

  @Test
  void servicePreparesQuantumProgramPackageFromCasscfOptimizedHamiltonian() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_casscf_quantum_program_package"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("hamiltonian_orbital_optimization", "casscf"),
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 12),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
                ClassicalCalculationOption.text("fermion_to_qubit_mapping", "BRAVYI_KITAEV"),
                ClassicalCalculationOption.text(
                    "quantum_program_kinds",
                    "PAULI_EVOLUTION,VERIFIED_PAULI_EVOLUTION,UCCSD_ANSATZ,QUANTUM_PHASE_ESTIMATION"),
                ClassicalCalculationOption.floating(
                    "quantum_program_verification_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> artifacts = ClassicalCalculationExecutorTest.table(result, "artifacts");
    final List<String> vqeArtifacts =
        ClassicalCalculationExecutorTest.table(result, "vqe_artifacts");

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION));
    Assertions.assertEquals("6-31G", ClassicalCalculationExecutorTest.value(result, "basis_set"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "CASSCF_ORBITAL_OPTIMIZED_RHF",
        ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "orbital_optimized_reference"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_quality").isBlank());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "orbital_optimization_stop_reason")
            .isBlank());
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "orbital_rotation_pair_coverage_ratio"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "available_orbital_rotation_pair_count"))
            > 0.0);
    Assertions.assertEquals(
        "BRAVYI_KITAEV", ClassicalCalculationExecutorTest.value(result, "requested_mapping"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "hamiltonian_audit_available"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "hamiltonian_ready_for_mapping"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "hamiltonian_max_one_electron_integral"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "hamiltonian_max_two_electron_integral"))
            > 0.0);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "hamiltonian_audit_diagnostics")
            .get(0)
            .contains("severity\tcode\ttarget\tmessage"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "generation_complete"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "skipped_artifact_count"));
    Assertions.assertEquals(
        "FULL_VQE_QPE_VERIFIED_PACKAGE",
        ClassicalCalculationExecutorTest.value(result, "quantum_workflow_coverage_status"));
    Assertions.assertEquals("true", ClassicalCalculationExecutorTest.value(result, "vqe_ready"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "uccsd_ansatz_available"));
    Assertions.assertEquals(
        "false",
        ClassicalCalculationExecutorTest.value(result, "hardware_efficient_vqe_available"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "adapt_vqe_ansatz_available"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "pauli_evolution_available"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "verified_pauli_evolution_available"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "qpe_available"));
    Assertions.assertEquals(
        "7.0", ClassicalCalculationExecutorTest.value(result, "mapping_variant_count"));
    Assertions.assertEquals(
        "7.0", ClassicalCalculationExecutorTest.value(result, "verified_mapping_variant_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "vqe_ansatz_operation_count"))
            > 0.0);
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "fermion_term_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "requested_mapping_pauli_term_count"))
            > 0.0);
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "requested_mapping_required_qubits"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "requested_mapping_hermitian"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "requested_mapping_verified"));
    Assertions.assertEquals(
        "EXHAUSTIVE_SPECTRAL_MOMENTS",
        ClassicalCalculationExecutorTest.value(result, "mapping_verification_mode"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "mapping_algebra_max_residual"))
            <= 1.0E-8);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "mapping_spectral_max_difference"))
            <= 1.0E-8);
    Assertions.assertEquals(
        "mapping\tfermion_terms\tpauli_terms\trequired_qubits\talgebra_max_residual\tspectral_max_difference\tmode\tverified",
        ClassicalCalculationExecutorTest.table(result, "requested_mapping_verification").get(0));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "requested_mapping_verification")
            .get(1)
            .contains("BRAVYI_KITAEV"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "artifact_count"))
            >= 7.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_operation_count"))
            > 0.0);
    Assertions.assertEquals("id\tkind\tmapping\toperations\tdescription", artifacts.get(0));
    Assertions.assertTrue(
        artifacts.stream()
            .anyMatch(row -> row.contains("PAULI_EVOLUTION") && row.contains("BRAVYI_KITAEV")));
    Assertions.assertTrue(
        artifacts.stream()
            .anyMatch(
                row -> row.contains("VERIFIED_PAULI_EVOLUTION") && row.contains("JORDAN_WIGNER")));
    Assertions.assertTrue(artifacts.stream().anyMatch(row -> row.contains("UCCSD_ANSATZ")));
    Assertions.assertTrue(
        artifacts.stream().anyMatch(row -> row.contains("QUANTUM_PHASE_ESTIMATION")));
    Assertions.assertEquals("id\tkind\tmapping\toperations\tvqe_role", vqeArtifacts.get(0));
    Assertions.assertTrue(
        vqeArtifacts.stream()
            .anyMatch(
                row ->
                    row.contains("UCCSD_ANSATZ") && row.contains("chemistry_excitation_ansatz")));
    Assertions.assertTrue(
        vqeArtifacts.stream()
            .anyMatch(
                row ->
                    row.contains("PAULI_EVOLUTION")
                        && row.contains("hamiltonian_measurement_evolution")));
    Assertions.assertTrue(
        vqeArtifacts.stream()
            .anyMatch(
                row ->
                    row.contains("VERIFIED_PAULI_EVOLUTION")
                        && row.contains("verified_hamiltonian_evolution")));
  }

  @Test
  void servicePreparesQuantumProgramFromResourceLimitedDirectActiveSpaceHamiltonian() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("resource_limited_direct_active_quantum_program"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            ClassicalCalculationExecutorTest.largeHydrogenStructuredBasis(4),
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("scf_integral_mode", "direct"),
                ClassicalCalculationOption.integer("active_space_first_orbital", 1),
                ClassicalCalculationOption.integer("rhf_max_iterations", 1),
                ClassicalCalculationOption.text("fermion_to_qubit_mapping", "JORDAN_WIGNER"),
                ClassicalCalculationOption.text(
                    "quantum_program_kinds",
                    "HARTREE_FOCK_REFERENCE,PAULI_EVOLUTION,VERIFIED_PAULI_EVOLUTION,UCCSD_ANSATZ"),
                ClassicalCalculationOption.floating(
                    "quantum_program_verification_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "native_java_preparation"));
    Assertions.assertEquals(
        "JORDAN_WIGNER", ClassicalCalculationExecutorTest.value(result, "requested_mapping"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "requested_mapping_verified"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "hamiltonian_ready_for_mapping"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "pauli_evolution_available"));
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(result, "verified_pauli_evolution_available"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "uccsd_ansatz_available"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "preparation_limitation_reason")
            .contains("Direct active-space Hamiltonian skipped full AO ERI tensor"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON",
        ClassicalCalculationExecutorTest.value(result, "hamiltonian_completeness"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "fermion_term_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "requested_mapping_pauli_term_count"))
            > 0.0);
    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, result.coverageReport().items().get(0).status());
  }

  @Test
  void servicePreparesOpenShellHamiltonianWithUhfReference() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_open_shell_hamiltonian"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "reference_converged"));
    Assertions.assertEquals(
        "5.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "10.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "reference_total_energy"))));
  }

  @Test
  void servicePreparesReducedOpenShellActiveSpaceHamiltonianWithUhfReference() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_open_shell_reduced_active_space_hamiltonian"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 2)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "two_electron_integrals_available"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "spatial_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "alpha_electron_count"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "beta_electron_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "two_electron_integral_count"))
            > 0.0);
  }

  @Test
  void electronicPreparationCarriesOpenShellUhfReference() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_open_shell_preparation"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ElectronicStructurePreparationResult result = pipeline.prepare(request);

    Assertions.assertTrue(result.hasUhfReference());
    Assertions.assertFalse(result.hasRhfReference());
    Assertions.assertEquals(ElectronicStructurePreparationMode.FULL_TWO_ELECTRON, result.mode());
    Assertions.assertFalse(result.hasLimitationReason());
    Assertions.assertEquals(6, result.electronCount());
    Assertions.assertEquals(4, result.uhfResult().alphaElectronCount());
    Assertions.assertEquals(2, result.uhfResult().betaElectronCount());
    Assertions.assertTrue(result.uhfResult().converged());
    Assertions.assertTrue(result.hasAtomicOrbitalTensors());
    Assertions.assertTrue(result.hasMolecularOrbitalTensors());
    Assertions.assertEquals(
        result.atomicOrbitalTensors().orbitalCount(),
        result.molecularOrbitalTensors().orbitalCount());
    Assertions.assertEquals(
        result.atomicOrbitalTensors().electronRepulsionPackedElementCount(),
        result.molecularOrbitalTensors().electronRepulsionPackedElementCount());
    Assertions.assertTrue(
        result.molecularOrbitalTensors().electronRepulsionPackedElementCount() > 0);
    Assertions.assertTrue(
        Double.isFinite(result.molecularOrbitalTensors().coreHamiltonianAt(0, 0)));
  }

  @Test
  void electronicPreparationCarriesReducedOpenShellActiveSpaceHamiltonian() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_open_shell_reduced_active_space_preparation"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 2)),
            ChemistryMetadata.EMPTY);

    final ElectronicStructurePreparationResult result = pipeline.prepare(request);

    Assertions.assertTrue(result.hasUhfReference());
    Assertions.assertEquals(2, result.electronCount());
    Assertions.assertEquals(2, result.molecularOrbitalTensors().orbitalCount());
    Assertions.assertEquals(2, result.hamiltonian().activeSpace().electronCount());
    Assertions.assertEquals(2, result.hamiltonian().activeSpace().orbitalCount());
    Assertions.assertTrue(result.hamiltonian().twoElectronIntegrals().size() > 0);
  }

  @Test
  void electronicPreparationPreflightReportsStructuredHighAngularFullTensorResourceBoundary() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final BasisSet highAngularBasis =
        BasisSet.structured(
            BasisSetName.of("fe-hik-structured"),
            BasisSetDefinition.of(
                List.of(
                    BasisAtomDefinition.of(
                        ElementSymbol.of("Fe"),
                        List.of(
                            ClassicalCalculationExecutorTest.shell(BasisShellAngularMomentum.H),
                            ClassicalCalculationExecutorTest.shell(BasisShellAngularMomentum.I),
                            ClassicalCalculationExecutorTest.shell(
                                BasisShellAngularMomentum.K))))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("fe_high_angular_preflight"),
            ClassicalCalculationExecutorTest.ironAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            highAngularBasis,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ElectronicStructurePreparationPreflight preflight = pipeline.preflight(request);

    Assertions.assertFalse(preflight.supported());
    Assertions.assertTrue(preflight.hasFullTensorPreflight());
    Assertions.assertTrue(preflight.hasOneElectronPreflight());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.UNSUPPORTED_RESOURCE_LIMIT,
        preflight.fullTensorPreflight().status());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.oneElectronPreflight().status());
    Assertions.assertEquals(85, preflight.fullTensorPreflight().orbitalCount());
    Assertions.assertEquals(7, preflight.fullTensorPreflight().maxAngularMomentum());
    Assertions.assertEquals(28, preflight.fullTensorPreflight().maxFullEriDerivativeOrder());
    Assertions.assertTrue(preflight.unsupportedReason().contains("packed double elements"));
  }

  @Test
  void serviceExecutesLocalGeometryOptimizationWithEnergyDecrease() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("geometry_optimization"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION),
            List.of(
                ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field"),
                ClassicalCalculationOption.integer("geometry_max_iterations", 96),
                ClassicalCalculationOption.floating("geometry_step_angstrom", 0.02)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "bonded_force_field_bfgs_line_search",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals(
        "bonded_force_field", ClassicalCalculationExecutorTest.value(result, "energy_model"));
    Assertions.assertEquals("2.0", ClassicalCalculationExecutorTest.value(result, "atom_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "coordinate_count"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "final_energy"))
            < Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "initial_energy")));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "energy_decrease"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_rms")) > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "accepted_step_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "zero_step_count"))
            >= 0.0);
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "energy_monotonic_nonincreasing"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "optimized_coordinate_count"));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "optimized_coordinates").size());
    Assertions.assertEquals(
        "iteration|energy|gradient_norm|max_gradient_component|accepted_step_angstrom|converged",
        ClassicalCalculationExecutorTest.table(result, "optimization_history").get(0));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "optimization_history").size() > 1);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesStandaloneGeometryGradientWithoutOptimizingCoordinates() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("geometry_gradient"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 1.0E-4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "gradient_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "bonded_force_field_finite_difference_gradient",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals(
        "bonded_force_field", ClassicalCalculationExecutorTest.value(result, "energy_model"));
    Assertions.assertEquals("2.0", ClassicalCalculationExecutorTest.value(result, "atom_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "coordinate_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "gradient_component_count"));
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_norm")) > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_rms")) > 0.0);
    Assertions.assertTrue(
        Math.abs(
                ClassicalCalculationExecutorTest.gradientZ(rows.get(0))
                    + ClassicalCalculationExecutorTest.gradientZ(rows.get(1)))
            < 1.0E-6);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceAutoSelectsElectronicGeometryGradientForSmallMolecule() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("auto_electronic_geometry_gradient"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 5.0E-4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "gradient_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_gradient",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals(
        "electronic_structure_finite_difference",
        ClassicalCalculationExecutorTest.value(result, "energy_model"));
    Assertions.assertEquals("2.0", ClassicalCalculationExecutorTest.value(result, "atom_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "coordinate_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "gradient_component_count"));
    Assertions.assertEquals(
        "central_finite_difference_java_native_single_point",
        ClassicalCalculationExecutorTest.value(result, "derivative_stencil"));
    Assertions.assertEquals(13.0, result.requireScalar("energy_evaluation_count"));
    Assertions.assertEquals(0.0, result.requireScalar("energy_cache_hit_count"));
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_norm")) > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_rms")) > 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void electronicFiniteDifferenceEnergyModelCachesRepeatedSinglePointCoordinates() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("electronic_geometry_energy_cache"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference")),
            ChemistryMetadata.EMPTY);
    final ElectronicStructureFiniteDifferenceEnergyModel energyModel =
        new ElectronicStructureFiniteDifferenceEnergyModel(
            request, new SinglePointEnergyExecutor());
    final double[] coordinates =
        GeometryCoordinateSupport.coordinatesAngstrom(
            ClassicalCalculationExecutorTest.stretchedHydrogen());

    final double firstEnergy =
        energyModel.energy(ClassicalCalculationExecutorTest.stretchedHydrogen(), coordinates);
    final double secondEnergy =
        energyModel.energy(
            ClassicalCalculationExecutorTest.stretchedHydrogen(), coordinates.clone());

    Assertions.assertEquals(firstEnergy, secondEnergy, 0.0);
    Assertions.assertEquals(1, energyModel.energyEvaluationCount());
    Assertions.assertEquals(1, energyModel.energyCacheHitCount());
  }

  @Test
  void serviceExecutesElectronicStructureGeometryGradientThroughSinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("electronic_geometry_gradient"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 5.0E-4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "gradient_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_gradient",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "energy"))));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_norm")) > 0.0);
    Assertions.assertTrue(
        Math.abs(
                ClassicalCalculationExecutorTest.gradientZ(rows.get(0))
                    + ClassicalCalculationExecutorTest.gradientZ(rows.get(1)))
            < 1.0E-5);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void electronicGeometryGradientForcesJavaNativeSinglePointEvenWhenPyscfOptionLeaksIn() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("electronic_geometry_gradient_java_native"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.text("single_point_backend", "pyscf"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 5.0E-4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "gradient_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_gradient",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "central_finite_difference_java_native_single_point",
        ClassicalCalculationExecutorTest.value(result, "derivative_stencil"));
    Assertions.assertTrue(result.requireScalar("energy_evaluation_count") > 0.0);
    Assertions.assertTrue(result.requireScalar("energy_cache_hit_count") >= 0.0);
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "energy"))));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void serviceExecutesMp2GeometryGradientThroughJavaNativePostHfEnergySurface() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("mp2_electronic_geometry_gradient_java_native"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.PERTURBATION_THEORY,
                ElectronicStructureMethodName.of("MP2")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.text("single_point_backend", "pyscf"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 1.0E-3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "gradient_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_gradient",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "energy"))));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_norm")) > 0.0);
    Assertions.assertTrue(
        Math.abs(
                ClassicalCalculationExecutorTest.gradientZ(rows.get(0))
                    + ClassicalCalculationExecutorTest.gradientZ(rows.get(1)))
            < 1.0E-5);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void serviceExecutesCcsdGeometryGradientThroughJavaNativePostHfEnergySurface() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("ccsd_electronic_geometry_gradient_java_native"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.text("single_point_backend", "pyscf"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 1.0E-3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "gradient_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_gradient",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "energy"))));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_norm")) > 0.0);
    Assertions.assertTrue(
        Math.abs(
                ClassicalCalculationExecutorTest.gradientZ(rows.get(0))
                    + ClassicalCalculationExecutorTest.gradientZ(rows.get(1)))
            < 1.0E-5);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  @Disabled(
      "Manual heavy Java-native electronic optimization regression; local single run passed but costs minutes.")
  void serviceOptimizesGeometryAgainstElectronicSinglePointEnergy() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("electronic_geometry_optimization"),
            ClassicalCalculationExecutorTest.stretchedHydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.integer("geometry_max_iterations", 1),
                ClassicalCalculationOption.integer("geometry_max_line_search_attempts", 1),
                ClassicalCalculationOption.floating("geometry_step_angstrom", 0.01),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 5.0E-4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_bfgs_line_search",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "optimized_coordinates").size());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "optimization_history").size() >= 1);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "final_energy"))
            <= Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "initial_energy")));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "gradient_norm")) > 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesLocalConformerSearchForRotatableSingleBonds() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("ethanol_conformer_search"),
            ClassicalCalculationExecutorTest.ethanol(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.CONFORMER_SEARCH));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "discrete_torsion_semiempirical_scan",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "native_valence_graph_semiempirical",
        ClassicalCalculationExecutorTest.value(result, "energy_model"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "rotatable_bond_count"))
            > 0.0);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "conformer_rows");
    Assertions.assertTrue(rows.size() > 1);
    Assertions.assertTrue(rows.get(0).contains("electronic_energy_hartree"));
    Assertions.assertTrue(rows.get(0).contains("boltzmann_weight"));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesFeatureBasedSpectroscopyPrediction() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("ethanol_spectroscopy"),
            ClassicalCalculationExecutorTest.ethanol(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SPECTROSCOPY_PREDICTION));

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "spectroscopy_rows");

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "feature_based_ir_nmr_uv_prediction",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertTrue(rows.size() > 1);
    Assertions.assertTrue(rows.stream().anyMatch(row -> row.contains("O-H or C-O stretch")));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesJavaNativeExcitedStateAnalysisWithoutPyScfBackend() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_excited_states"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.EXCITED_STATE_ANALYSIS),
            List.of(ClassicalCalculationOption.integer("excited_state_count", 3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "excited_state_rows");

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_cis_tda_singlet", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "cis_tda_matrix_diagonalization",
        ClassicalCalculationExecutorTest.value(result, "excitation_model"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "single_excitation_dimension"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "excited_states_sorted_by_energy"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "bright_state_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "lowest_excitation_energy_hartree"))
            > 0.0);
    Assertions.assertEquals(
        ClassicalCalculationExecutorTest.value(result, "lowest_excitation_energy_hartree"),
        ClassicalCalculationExecutorTest.value(
            result, "highest_reported_excitation_energy_hartree"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "oscillator_strength_sum"))
            > 0.0);
    Assertions.assertFalse(rows.isEmpty());
    Assertions.assertTrue(rows.get(0).contains("0->1"));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void serviceExecutesJavaNativeExcitedStateMatrixForSplitValenceBasis() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_631g_native_excited_states"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            null,
            List.of(ClassicalCalculationKind.EXCITED_STATE_ANALYSIS),
            List.of(ClassicalCalculationOption.integer("excited_state_count", 3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "excited_state_rows");

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "cis_tda_matrix_diagonalization",
        ClassicalCalculationExecutorTest.value(result, "excitation_model"));
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "single_excitation_dimension"));
    Assertions.assertEquals("3.0", ClassicalCalculationExecutorTest.value(result, "state_count"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "excited_states_sorted_by_energy"));
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "bright_state_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "lowest_excitation_energy_ev"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "highest_reported_excitation_energy_hartree"))
            >= Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "lowest_excitation_energy_hartree")));
    Assertions.assertEquals(3, rows.size());
    Assertions.assertTrue(
        Double.parseDouble(rows.get(0).split("\\t")[4])
            <= Double.parseDouble(rows.get(1).split("\\t")[4]));
    Assertions.assertTrue(
        Double.parseDouble(rows.get(1).split("\\t")[4])
            <= Double.parseDouble(rows.get(2).split("\\t")[4]));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void serviceExecutesJavaNativeUnrestrictedExcitedStateMatrixForRadical() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_atom_native_unrestricted_excited_states"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.EXCITED_STATE_ANALYSIS),
            List.of(ClassicalCalculationOption.integer("excited_state_count", 4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    final List<String> rows = ClassicalCalculationExecutorTest.table(result, "excited_state_rows");

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_unrestricted_spin_block_tda",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "unrestricted_spin_block_tda_matrix_diagonalization",
        ClassicalCalculationExecutorTest.value(result, "excitation_model"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "single_excitation_dimension"))
            > 2.0);
    Assertions.assertEquals("4.0", ClassicalCalculationExecutorTest.value(result, "state_count"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "excited_states_sorted_by_energy"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "bright_state_count"))
            >= 1.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "oscillator_strength_sum"))
            > 0.0);
    Assertions.assertEquals(4, rows.size());
    Assertions.assertTrue(rows.stream().anyMatch(row -> row.contains("\tA\t")));
    Assertions.assertTrue(
        rows.stream().allMatch(row -> Double.parseDouble(row.split("\\t")[4]) > 0.0));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void preflightSizesHeterocyclicRadicalHamiltonianThroughJavaNativeUhfPipeline() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("azirine_radical_native_hamiltonian"),
            ClassicalCalculationExecutorTest.azirineRadicalProbe(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(3, 3),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 7)),
            ChemistryMetadata.EMPTY);

    final ElectronicStructurePreparationPreflight preflight = pipeline.preflight(request);

    Assertions.assertTrue(preflight.supported(), preflight.unsupportedReason());
    Assertions.assertTrue(preflight.hasFullTensorPreflight());
    Assertions.assertTrue(preflight.hasOneElectronPreflight());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.fullTensorPreflight().status());
    Assertions.assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED, preflight.oneElectronPreflight().status());
    Assertions.assertTrue(preflight.fullTensorPreflight().orbitalCount() >= 14);
    Assertions.assertTrue(preflight.fullTensorPreflight().packedEriElementCount() > 0L);
    Assertions.assertFalse(
        preflight.unsupportedReason().toLowerCase(java.util.Locale.ROOT).contains("pyscf"));
  }

  @Test
  void servicePreparesOpenShellQuantumProgramThroughJavaNativeUhfPipeline() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_open_shell_native_quantum_program"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("scf_integral_mode", "direct"),
                ClassicalCalculationOption.integer("active_space_first_orbital", 2),
                ClassicalCalculationOption.text("fermion_to_qubit_mapping", "JORDAN_WIGNER"),
                ClassicalCalculationOption.text(
                    "quantum_program_kinds",
                    "PAULI_EVOLUTION,VERIFIED_PAULI_EVOLUTION,HARDWARE_EFFICIENT_VQE_ANSATZ"),
                ClassicalCalculationOption.floating(
                    "quantum_program_verification_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION));
    final List<String> artifacts = ClassicalCalculationExecutorTest.table(result, "artifacts");
    final List<String> vqeArtifacts =
        ClassicalCalculationExecutorTest.table(result, "vqe_artifacts");
    Assertions.assertEquals(
        "UHF", ClassicalCalculationExecutorTest.value(result, "reference_method"));
    Assertions.assertEquals(
        "FULL_TWO_ELECTRON", ClassicalCalculationExecutorTest.value(result, "preparation_mode"));
    Assertions.assertEquals(
        "JORDAN_WIGNER", ClassicalCalculationExecutorTest.value(result, "requested_mapping"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "generation_complete"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "requested_mapping_verified"));
    Assertions.assertEquals(
        "2.0", ClassicalCalculationExecutorTest.value(result, "electron_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "spin_orbital_count"));
    Assertions.assertEquals(
        "4.0", ClassicalCalculationExecutorTest.value(result, "requested_mapping_required_qubits"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "fermion_term_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "requested_mapping_pauli_term_count"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "mapping_algebra_max_residual"))
            <= 1.0E-8);
    Assertions.assertEquals("id\tkind\tmapping\toperations\tdescription", artifacts.get(0));
    Assertions.assertTrue(
        artifacts.stream()
            .anyMatch(row -> row.contains("PAULI_EVOLUTION") && row.contains("JORDAN_WIGNER")));
    Assertions.assertTrue(
        artifacts.stream()
            .anyMatch(
                row -> row.contains("VERIFIED_PAULI_EVOLUTION") && row.contains("JORDAN_WIGNER")));
    Assertions.assertTrue(
        vqeArtifacts.stream().anyMatch(row -> row.contains("HARDWARE_EFFICIENT_VQE_ANSATZ")));
  }

  @Test
  void serviceRejectsOverLimitOpenShellHeterocycleRadicalDirectCoreQuantumProgram() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("azirine_radical_native_quantum_program"),
            ClassicalCalculationExecutorTest.azirineRadicalProbe(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(1, 1),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(
                ClassicalCalculationOption.text("scf_integral_mode", "direct"),
                ClassicalCalculationOption.text("scf_reference_mode", "core_hamiltonian"),
                ClassicalCalculationOption.integer("max_direct_mo_pair_products", 1),
                ClassicalCalculationOption.integer("active_space_first_orbital", 8),
                ClassicalCalculationOption.text("fermion_to_qubit_mapping", "JORDAN_WIGNER"),
                ClassicalCalculationOption.text(
                    "quantum_program_kinds",
                    "PAULI_EVOLUTION,VERIFIED_PAULI_EVOLUTION,HARDWARE_EFFICIENT_VQE_ANSATZ"),
                ClassicalCalculationOption.floating(
                    "quantum_program_verification_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);
    Assertions.assertEquals(
        ClassicalCalculationStatus.UNSUPPORTED, result.status(), result.diagnostics().toString());
    Assertions.assertFalse(result.hasSection(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ClassicalDiagnosticCode.RESULT_FAILED
                        && diagnostic.message().contains("max_direct_mo_pair_products")));
  }

  @Test
  void serviceExecutesLocalHarmonicFrequencyAnalysisWithThermalCorrections() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_frequency"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.FREQUENCY_ANALYSIS,
                ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(
        "bonded_harmonic_force_field", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals("1.0", ClassicalCalculationExecutorTest.value(result, "mode_count"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "zero_point_energy"))
            > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "estimated_enthalpy_correction"))
            > 0.0);
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "frequencies").size());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void molecularThermodynamicsUsesExplicitTemperatureWithoutChangingFrequencies() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest roomTemperatureRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_thermo_room"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.FREQUENCY_ANALYSIS,
                ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field")),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest hotRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_thermo_hot"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.FREQUENCY_ANALYSIS,
                ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field"),
                ClassicalCalculationOption.floating("thermodynamics_temperature_kelvin", 600.0)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult roomResult = service.execute(roomTemperatureRequest);
    final ClassicalCalculationResult hotResult = service.execute(hotRequest);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, roomResult.status());
    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, hotResult.status());
    Assertions.assertEquals(
        "298.15", ClassicalCalculationExecutorTest.value(roomResult, "temperature"));
    Assertions.assertEquals(
        "600.0", ClassicalCalculationExecutorTest.value(hotResult, "temperature"));
    Assertions.assertEquals(
        ClassicalCalculationExecutorTest.value(roomResult, "zero_point_energy"),
        ClassicalCalculationExecutorTest.value(hotResult, "zero_point_energy"));
    Assertions.assertEquals(
        ClassicalCalculationExecutorTest.table(roomResult, "frequencies"),
        ClassicalCalculationExecutorTest.table(hotResult, "frequencies"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(hotResult, "thermal_correction_enthalpy"))
            > Double.parseDouble(
                ClassicalCalculationExecutorTest.value(roomResult, "thermal_correction_enthalpy")));
  }

  @Test
  void molecularThermodynamicsRejectsNonPhysicalTemperatureOption() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_thermo_bad_temperature"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(ClassicalCalculationOption.floating("thermodynamics_temperature_kelvin", 0.0)),
            ChemistryMetadata.EMPTY);

    final IllegalArgumentException exception =
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.execute(request));

    Assertions.assertEquals(
        "Thermodynamics temperature must be finite and positive.", exception.getMessage());
  }

  @Test
  void serviceExecutesElectronicStructureHessianAnalysisThroughJavaNativeFiniteDifferences() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_electronic_hessian"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.HESSIAN_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 2.0E-3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    final List<String> hessianRows = ClassicalCalculationExecutorTest.table(result, "hessian_rows");
    Assertions.assertEquals(
        "electronic_structure_finite_difference_finite_difference_hessian",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "coordinate_count"));
    Assertions.assertEquals(9, hessianRows.size());
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "energy"))));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "zero_point_energy"))
            >= 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceAutoSelectsElectronicHessianForSmallMolecule() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_atom_auto_electronic_hessian"),
            ClassicalCalculationExecutorTest.hydrogenAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.HESSIAN_ANALYSIS),
            List.of(
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 2.0E-3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_finite_difference_hessian",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "3.0", ClassicalCalculationExecutorTest.value(result, "coordinate_count"));
    Assertions.assertEquals(
        9, ClassicalCalculationExecutorTest.table(result, "hessian_rows").size());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesMolecularElectronicFrequencyAnalysisThroughJavaNativeFiniteDifferences() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_molecule_electronic_frequency"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.FREQUENCY_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 2.0E-3)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_finite_difference_hessian",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "electronic_structure_finite_difference",
        ClassicalCalculationExecutorTest.value(result, "hessian_model"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "coordinate_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "vibrational_mode_count"));
    Assertions.assertEquals(
        "first_order_saddle",
        ClassicalCalculationExecutorTest.value(result, "stationary_point_kind"));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "real_mode_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "imaginary_mode_count"));
    Assertions.assertEquals(
        36, ClassicalCalculationExecutorTest.table(result, "hessian_rows").size());
    final List<String> frequencies = ClassicalCalculationExecutorTest.table(result, "frequencies");
    Assertions.assertEquals(1, frequencies.size());
    final List<String> frequencyModes =
        ClassicalCalculationExecutorTest.table(result, "frequency_modes");
    Assertions.assertEquals(2, frequencyModes.size());
    Assertions.assertEquals(
        "index|kind|signed_wavenumber_cm^-1|absolute_wavenumber_cm^-1", frequencyModes.get(0));
    Assertions.assertTrue(frequencyModes.get(1).contains("|imaginary|"));
    final double frequency = Double.parseDouble(frequencies.get(0));
    Assertions.assertTrue(Double.isFinite(frequency));
    Assertions.assertTrue(frequency < 0.0);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void frequencyAnalysisRequiresBondedMolecule() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_frequency"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.FREQUENCY_ANALYSIS));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.FAILED, result.status());
    Assertions.assertTrue(result.sections().isEmpty());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.GEOMETRY_REQUIRED));
  }

  @Test
  void serviceExecutesPeriodicBoundaryAnalysisForSupercell() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("ethanol_periodic_boundary"),
            ClassicalCalculationExecutorTest.ethanol(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text("periodic_model", "minimum-image"),
                ClassicalCalculationOption.floating("periodic_a_angstrom", 12.0),
                ClassicalCalculationOption.floating("periodic_b_angstrom", 13.0),
                ClassicalCalculationOption.floating("periodic_c_angstrom", 14.0),
                ClassicalCalculationOption.integer("periodic_supercell_a", 2),
                ClassicalCalculationOption.integer("periodic_supercell_b", 3),
                ClassicalCalculationOption.integer("periodic_supercell_c", 1),
                ClassicalCalculationOption.integer("periodic_k_mesh_a", 2),
                ClassicalCalculationOption.integer("periodic_k_mesh_b", 3),
                ClassicalCalculationOption.integer("periodic_k_mesh_c", 4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "MINIMUM_IMAGE", ClassicalCalculationExecutorTest.value(result, "model"));
    Assertions.assertEquals(
        "2184.0", ClassicalCalculationExecutorTest.value(result, "cell_volume"));
    Assertions.assertEquals("6.0", ClassicalCalculationExecutorTest.value(result, "image_count"));
    Assertions.assertEquals(
        "54.0", ClassicalCalculationExecutorTest.value(result, "expanded_atom_count"));
    Assertions.assertEquals(
        "6.0", ClassicalCalculationExecutorTest.value(result, "minimum_image_cutoff"));
    Assertions.assertEquals("2.0", ClassicalCalculationExecutorTest.value(result, "k_mesh_a"));
    Assertions.assertEquals("3.0", ClassicalCalculationExecutorTest.value(result, "k_mesh_b"));
    Assertions.assertEquals("4.0", ClassicalCalculationExecutorTest.value(result, "k_mesh_c"));
    Assertions.assertEquals(
        "24.0", ClassicalCalculationExecutorTest.value(result, "k_point_count"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "reciprocal_a")) > 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "free_electron_band_width"))
            > 0.0);
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "finite_molecule_fits_cell"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "density_atoms_per_angstrom3"))
            > 0.0);
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
  }

  @Test
  void reactionPathAutoModeReportsWhenElectronicBaselineIsSkippedBySizeGuard() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_path_auto_guard"),
            "Hydrogen path auto guard",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_path_auto_guard"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PATH),
            List.of(
                ClassicalCalculationOption.integer("reaction_path_max_auto_electronic_atoms", 1)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "mechanistic_bond_energy_pathway",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "bond_change_model",
        ClassicalCalculationExecutorTest.value(result, "reaction_energy_source"));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("auto electronic baseline skipped")),
        result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().items().get(0).status());
  }

  @Test
  void reactionPathForcedElectronicModeBypassesAutoSizeGuard() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_path_forced_electronic"),
            "Hydrogen path forced electronic",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_path_forced_electronic"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PATH),
            List.of(
                ClassicalCalculationOption.integer("reaction_path_max_auto_electronic_atoms", 1),
                ClassicalCalculationOption.text("reaction_path_energy_source", "electronic")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "mechanistic_electronic_energy_pathway",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "reaction_energy_source"));
    Assertions.assertEquals(0.0, result.requireScalar("electronic_reaction_energy"), 1.0E-10);
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("auto electronic baseline skipped")),
        result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceExecutesPeriodicBoundaryAnalysisForReactionSides() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("periodic_hydrogen_exchange"),
            "Periodic hydrogen exchange",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_periodic_boundary"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text("periodic_model", "crystal_cell"),
                ClassicalCalculationOption.floating("periodic_a_angstrom", 8.0),
                ClassicalCalculationOption.floating("periodic_b_angstrom", 8.0),
                ClassicalCalculationOption.floating("periodic_c_angstrom", 8.0),
                ClassicalCalculationOption.integer("periodic_supercell_a", 2),
                ClassicalCalculationOption.integer("periodic_supercell_b", 2),
                ClassicalCalculationOption.integer("periodic_supercell_c", 2),
                ClassicalCalculationOption.integer("periodic_k_mesh_a", 2),
                ClassicalCalculationOption.integer("periodic_k_mesh_b", 2),
                ClassicalCalculationOption.integer("periodic_k_mesh_c", 2)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals(
        "CRYSTAL_CELL", ClassicalCalculationExecutorTest.value(result, "reactant_model"));
    Assertions.assertEquals(
        "16.0", ClassicalCalculationExecutorTest.value(result, "reactant_expanded_atom_count"));
    Assertions.assertEquals(
        "16.0", ClassicalCalculationExecutorTest.value(result, "product_expanded_atom_count"));
    Assertions.assertEquals(
        "8.0", ClassicalCalculationExecutorTest.value(result, "reactant_k_point_count"));
    Assertions.assertEquals(
        "8.0", ClassicalCalculationExecutorTest.value(result, "product_k_point_count"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "reactant_free_electron_band_minimum"))
            >= 0.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "reactant_free_electron_band_maximum"))
            >= Double.parseDouble(
                ClassicalCalculationExecutorTest.value(
                    result, "reactant_free_electron_band_minimum")));
    Assertions.assertEquals(
        "0.0", ClassicalCalculationExecutorTest.value(result, "delta_expanded_atom_count"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "reaction_cell_balanced"));
  }

  @Test
  void serviceExecutesRelativisticCorrectionForHeavyAtomMolecule() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("iodobenzene_relativistic"),
            ClassicalCalculationExecutorTest.iodobenzene(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.RELATIVISTIC_CORRECTION),
            List.of(ClassicalCalculationOption.text("relativistic_model", "DKH2_SCALAR")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals("DKH2_SCALAR", ClassicalCalculationExecutorTest.value(result, "model"));
    Assertions.assertEquals(
        "scalar_spin_free_atomic_screening",
        ClassicalCalculationExecutorTest.value(result, "model_scope"));
    Assertions.assertEquals(
        "53.0", ClassicalCalculationExecutorTest.value(result, "max_atomic_number"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_correction"))
            < 0.0);
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "heavy_atom_fraction"))
            > 0.0);
    Assertions.assertEquals(
        7, ClassicalCalculationExecutorTest.table(result, "atom_contribution_rows").size());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesRelativisticCorrectionForReactionDelta() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("iodination_relativistic"),
            "Iodination relativistic probe",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.ringMolecule("benzene_probe", "C"),
                        StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.iodineAtom(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.iodobenzene(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_relativistic"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.RELATIVISTIC_CORRECTION),
            List.of(ClassicalCalculationOption.text("relativistic_model", "scalar_zora")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals("SCALAR_ZORA", ClassicalCalculationExecutorTest.value(result, "model"));
    Assertions.assertEquals(
        "scalar_spin_free_reaction_correction",
        ClassicalCalculationExecutorTest.value(result, "model_scope"));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "reactant_correction"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "product_correction"))));
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "delta_correction"))));
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  private static String value(final ClassicalCalculationResult result, final String name) {
    final List<ClassicalResultSection> sections = result.sections();
    for (int i = 0; i < sections.size(); ++i) {
      final List<ClassicalResultValue> values = sections.get(i).values();
      for (int j = 0; j < values.size(); ++j) {
        final ClassicalResultValue value = values.get(j);
        if (value.name().equals(name)) {
          return value.values().get(0);
        }
      }
    }
    throw new AssertionError("Result value not found: " + name);
  }

  private static List<String> table(final ClassicalCalculationResult result, final String name) {
    final List<ClassicalResultSection> sections = result.sections();
    for (int i = 0; i < sections.size(); ++i) {
      final List<ClassicalResultValue> values = sections.get(i).values();
      for (int j = 0; j < values.size(); ++j) {
        final ClassicalResultValue value = values.get(j);
        if (value.name().equals(name)) {
          return value.values();
        }
      }
    }
    throw new AssertionError("Result table not found: " + name);
  }

  private static boolean isGeneratedBenzylaminoNicotinonitrileCandidate(
      final ReactionProductCandidate candidate) {
    boolean hasCoupledProduct = false;
    boolean hasHydrogenChloride = false;
    final List<ReactionParticipant> participants = candidate.products().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final String formula = participants.get(i).molecule().formula().hillNotation();
      if ("C15H15N3".equals(formula)) {
        hasCoupledProduct = true;
      }
      if ("ClH".equals(formula)) {
        hasHydrogenChloride = true;
      }
    }
    return hasCoupledProduct && hasHydrogenChloride;
  }

  private static double gradientZ(final String row) {
    final String[] columns = row.split(",");
    return Double.parseDouble(columns[7]);
  }

  @Test
  void serviceExecutesAdvancedMoleculePropertyMatrix() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final List<ClassicalCalculationKind> kinds =
        List.of(
            ClassicalCalculationKind.HESSIAN_ANALYSIS,
            ClassicalCalculationKind.NMR_PREDICTION,
            ClassicalCalculationKind.IR_RAMAN_PREDICTION,
            ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION,
            ClassicalCalculationKind.EXCITED_STATE_ANALYSIS,
            ClassicalCalculationKind.SOLVATION_MODELING,
            ClassicalCalculationKind.PKA_PREDICTION,
            ClassicalCalculationKind.REDOX_POTENTIAL,
            ClassicalCalculationKind.BASIS_SET_CONVERGENCE,
            ClassicalCalculationKind.METHOD_BENCHMARK,
            ClassicalCalculationKind.UNCERTAINTY_ANALYSIS,
            ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS,
            ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_molecule_properties"),
            ClassicalCalculationExecutorTest.ethanol(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            kinds);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(kinds.size(), result.sectionCount());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    for (final ClassicalCalculationKind kind : kinds) {
      Assertions.assertTrue(result.hasSection(kind), kind.name());
      Assertions.assertTrue(result.requireSection(kind).hasValue("model"), kind.name());
    }
    Assertions.assertTrue(result.hasValue("polarity_index"));
    Assertions.assertTrue(result.hasValue("nominal_parent_mass"));
    Assertions.assertTrue(result.hasValue("stereo_complexity_score"));
    Assertions.assertTrue(result.hasValue("basis_sensitivity_score"));
    Assertions.assertTrue(result.hasValue("method_sensitivity_score"));
    Assertions.assertTrue(result.hasValue("uncertainty_axis_count"));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows").size());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows").stream()
            .anyMatch(row -> row.contains("STO-3G")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows").stream()
            .anyMatch(row -> row.contains("6-31G")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "method_benchmark_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "uncertainty_rows").size() >= 5);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void serviceExecutesAdvancedReactionPlanningMatrix() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    final List<ClassicalCalculationKind> kinds =
        List.of(
            ClassicalCalculationKind.IRC_ANALYSIS,
            ClassicalCalculationKind.SELECTIVITY_ANALYSIS,
            ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS,
            ClassicalCalculationKind.REACTION_CONDITION_SCREENING,
            ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION,
            ClassicalCalculationKind.SOLVENT_SCREENING,
            ClassicalCalculationKind.CATALYST_SCREENING,
            ClassicalCalculationKind.CONCENTRATION_SCREENING,
            ClassicalCalculationKind.BASIS_SET_CONVERGENCE,
            ClassicalCalculationKind.METHOD_BENCHMARK,
            ClassicalCalculationKind.UNCERTAINTY_ANALYSIS,
            ClassicalCalculationKind.MECHANISM_ENUMERATION,
            ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS,
            ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS,
            ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION,
            ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS,
            ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS,
            ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS,
            ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS,
            ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS,
            ClassicalCalculationKind.MICROKINETIC_MODELING,
            ClassicalCalculationKind.RETROSYNTHESIS_PLANNING,
            ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS,
            ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_reaction_properties"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            kinds);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertEquals(kinds.size(), result.sectionCount());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    for (final ClassicalCalculationKind kind : kinds) {
      Assertions.assertTrue(result.hasSection(kind), kind.name());
      Assertions.assertTrue(result.requireSection(kind).hasValue("model"), kind.name());
    }
    Assertions.assertTrue(result.hasValue("condition_sensitivity"));
    Assertions.assertTrue(result.hasValue("condition_evaluated_scenario_count"));
    Assertions.assertTrue(result.hasValue("condition_optimization_best_score"));
    Assertions.assertTrue(result.hasValue("condition_best_scenario_id"));
    Assertions.assertTrue(result.hasValue("condition_best_scenario_kind"));
    Assertions.assertTrue(result.hasValue("condition_best_probability"));
    Assertions.assertTrue(result.hasValue("condition_best_expected_yield"));
    Assertions.assertTrue(result.hasValue("condition_best_primary_mechanism"));
    Assertions.assertTrue(result.hasValue("condition_best_electronic_weight"));
    Assertions.assertTrue(result.hasValue("condition_best_calibration_weight"));
    Assertions.assertTrue(result.hasValue("network_node_estimate"));
    Assertions.assertTrue(result.hasValue("network_predicted_branch_count"));
    Assertions.assertTrue(result.hasValue("disconnection_candidate_count"));
    Assertions.assertTrue(result.hasValue("condition_matrix_size_estimate"));
    Assertions.assertTrue(result.hasValue("mechanism_trigger_count"));
    Assertions.assertTrue(result.hasValue("competing_mechanism_trigger_count"));
    Assertions.assertTrue(result.hasValue("competing_pathway_count_estimate"));
    Assertions.assertTrue(result.hasValue("condition_optimization_design_size"));
    Assertions.assertTrue(result.hasValue("solvent_screen_axis_count"));
    Assertions.assertTrue(result.hasValue("catalyst_family_count"));
    Assertions.assertTrue(result.hasValue("concentration_regime_count"));
    Assertions.assertTrue(result.hasValue("basis_sensitivity_score"));
    Assertions.assertTrue(result.hasValue("method_ladder_size"));
    Assertions.assertTrue(result.hasValue("uncertainty_axis_count"));
    Assertions.assertTrue(result.hasValue("mechanism_candidate_count"));
    Assertions.assertTrue(result.hasValue("hazard_signal_count"));
    Assertions.assertTrue(result.hasValue("major_product_probability_estimate"));
    Assertions.assertTrue(result.hasValue("prediction_candidate_count"));
    Assertions.assertTrue(result.hasValue("prediction_probability_mass"));
    Assertions.assertTrue(result.hasValue("prediction_best_products"));
    Assertions.assertEquals(
        "reaction_prediction_service",
        ClassicalCalculationExecutorTest.value(result, "distribution_model_source"));
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "prediction_candidate_count"))
            >= 1.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "prediction_probability_mass"))
            > 0.0);
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "prediction_best_products").isBlank());
    Assertions.assertTrue(result.hasValue("conservative_yield_floor"));
    Assertions.assertTrue(result.hasValue("prediction_major_product_probability"));
    Assertions.assertTrue(result.hasValue("impurity_family_count_estimate"));
    Assertions.assertTrue(result.hasValue("thermal_runaway_proxy"));
    Assertions.assertTrue(result.hasValue("total_quantified_mass"));
    Assertions.assertTrue(result.hasValue("solvent_quantified_mass"));
    Assertions.assertTrue(result.hasValue("largest_component_mass"));
    Assertions.assertTrue(result.hasValue("viscosity_solvent_proxy"));
    Assertions.assertTrue(result.hasValue("heat_transfer_proxy"));
    Assertions.assertTrue(
        Double.parseDouble(ClassicalCalculationExecutorTest.value(result, "total_quantified_mass"))
            >= 86.5);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "solvent_quantified_mass"))
            >= 70.0);
    Assertions.assertTrue(
        Double.parseDouble(
                ClassicalCalculationExecutorTest.value(result, "viscosity_solvent_proxy"))
            > 0.0);
    Assertions.assertTrue(result.hasValue("workup_axis_count"));
    Assertions.assertTrue(result.hasValue("timeline_event_count_estimate"));
    Assertions.assertTrue(result.hasValue("elementary_step_count_estimate"));
    Assertions.assertTrue(result.hasValue("transition_state_candidate_count"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").size() >= 4);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows")
            .get(0)
            .contains("best_probability"));
    Assertions.assertTrue(result.requireScalar("condition_best_probability") >= 0.0);
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "condition_best_scenario_id").isBlank());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").stream()
            .anyMatch(row -> row.contains("BASELINE")),
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").stream()
            .anyMatch(row -> row.contains("HEATED") || row.contains("COOLED")),
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").stream()
            .anyMatch(row -> row.contains("POLAR_APROTIC_SOLVENT")),
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").stream()
            .anyMatch(row -> row.contains("DILUTE_REGIME") || row.contains("CONCENTRATED_REGIME")),
        ClassicalCalculationExecutorTest.table(result, "condition_screen_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_optimization_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "condition_optimization_rows")
            .get(0)
            .contains("ranking_score"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "solvent_screen_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "solvent_screen_rows").stream()
            .anyMatch(row -> row.contains("prediction_best_scenario")),
        ClassicalCalculationExecutorTest.table(result, "solvent_screen_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "catalyst_screen_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "catalyst_screen_rows").stream()
            .anyMatch(row -> row.contains("prediction_best_scenario")),
        ClassicalCalculationExecutorTest.table(result, "catalyst_screen_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "concentration_screen_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "concentration_screen_rows").stream()
            .anyMatch(row -> row.contains("prediction_best_scenario")),
        ClassicalCalculationExecutorTest.table(result, "concentration_screen_rows").toString());
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows").size());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows").stream()
            .anyMatch(row -> row.contains("STO-3G")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows").stream()
            .anyMatch(row -> row.contains("6-31G")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "method_benchmark_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "uncertainty_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "mechanism_enumeration_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "hazard_stability_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(row -> row.startsWith("1\t")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(row -> row.contains("desired-route")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(
                row -> row.contains("FUNCTIONAL_GROUP_MATCH") && row.contains("ELECTRONIC_FACTOR")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(row -> row.contains("CALIBRATION_PRIOR")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows")
            .get(0)
            .contains("primary_mechanism"));
    Assertions.assertTrue(result.hasValue("product_competing_pathway_rows"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "yield_window_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "yield_window_rows").stream()
            .anyMatch(row -> row.contains("normalized candidate distribution")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "side_product_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "side_product_rows").stream()
            .anyMatch(
                row ->
                    row.contains("prediction uncertainty") || row.contains("condition_triggered")));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "scale_up_risk_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "quench_workup_rows").size() >= 4);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "reaction_timeline_rows").size() >= 4);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "competing_pathway_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "microkinetic_step_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "transition_state_ensemble_rows").size()
            >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "network_node_rows").size() >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "network_node_rows").stream()
            .anyMatch(row -> row.contains("predicted-product-")),
        ClassicalCalculationExecutorTest.table(result, "network_node_rows").toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "retrosynthesis_disconnection_rows").size()
            >= 5);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "retrosynthesis_disconnection_rows").stream()
            .anyMatch(row -> row.contains("predicted-product-disconnection")),
        ClassicalCalculationExecutorTest.table(result, "retrosynthesis_disconnection_rows")
            .toString());
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "selectivity_axes").size() >= 4);
  }

  @Test
  void advancedProductDistributionUsesJavaNativeElectronicRerank() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("advanced_h2_product_distribution_rerank"),
            "Advanced hydrogen product distribution rerank",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_h2_product_distribution_rerank"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text("reaction_prediction_electronic_rerank", "true"),
                ClassicalCalculationOption.text(
                    "advanced_prediction_use_product_side_as_seed", "true")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(
        result.hasSection(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS));
    Assertions.assertEquals(
        1.0,
        Double.parseDouble(
            ClassicalCalculationExecutorTest.value(result, "prediction_probability_mass")),
        1.0E-12);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.value(result, "prediction_best_products").contains("H2"),
        ClassicalCalculationExecutorTest.value(result, "prediction_best_products"));
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").stream()
            .anyMatch(row -> row.contains("ELECTRONIC_FACTOR")),
        ClassicalCalculationExecutorTest.table(result, "product_distribution_rows").toString());
    Assertions.assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("electronic rerank was unavailable")),
        result.diagnostics().toString());
  }

  @Test
  void serviceExecutesHydrogenSto3gMolecularPropertiesThroughJavaNativeScf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_properties"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertFalse(result.successful());
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals("RHF", ClassicalCalculationExecutorTest.value(result, "reference"));
    Assertions.assertEquals(
        "koopmans_frontier_estimate",
        ClassicalCalculationExecutorTest.value(result, "vertical_energy_model"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("total_energy")));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("homo_energy")));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("lumo_energy")));
    Assertions.assertTrue(result.requireScalar("homo_lumo_gap") >= 0.0);
    Assertions.assertTrue(Double.isFinite(result.requireScalar("dipole_norm")));
    Assertions.assertEquals(
        "ao_dipole_integral",
        ClassicalCalculationExecutorTest.value(result, "ab_initio_dipole_model"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("ab_initio_dipole_norm")));
    Assertions.assertTrue(result.requireScalar("ab_initio_dipole_norm") < 1.0E-8);
    Assertions.assertEquals(
        "ao_density_overlap_center_moment",
        ClassicalCalculationExecutorTest.value(result, "density_moment_dipole_model"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("density_moment_dipole_norm")));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "mulliken_charge_rows").size());
    Assertions.assertEquals(2.0, result.requireScalar("mulliken_total_population"), 1.0E-8);
    Assertions.assertEquals(0.0, result.requireScalar("mulliken_total_charge"), 1.0E-8);
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.message().contains("Koopmans frontier IP/EA")),
        result.diagnostics().toString());
  }

  @Test
  void serviceExecutesPolarThirdPeriodMoleculeDensityMomentPropertiesThroughJavaNativeScf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_chloride_native_properties"),
            ClassicalCalculationExecutorTest.hydrogenChloride(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_scf_molecular_properties",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "ao_density_overlap_center_moment",
        ClassicalCalculationExecutorTest.value(result, "density_moment_dipole_model"));
    Assertions.assertEquals(
        "ao_dipole_integral",
        ClassicalCalculationExecutorTest.value(result, "ab_initio_dipole_model"));
    Assertions.assertEquals(
        2, ClassicalCalculationExecutorTest.table(result, "mulliken_charge_rows").size());
    Assertions.assertTrue(Double.isFinite(result.requireScalar("ab_initio_dipole_z")));
    Assertions.assertTrue(result.requireScalar("ab_initio_dipole_norm") > 0.0);
    Assertions.assertTrue(Double.isFinite(result.requireScalar("density_moment_dipole_z")));
    Assertions.assertTrue(result.requireScalar("density_moment_dipole_norm") > 0.0);
    Assertions.assertEquals(18.0, result.requireScalar("mulliken_total_population"), 1.0E-7);
    Assertions.assertEquals(0.0, result.requireScalar("mulliken_total_charge"), 1.0E-7);
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void serviceExecutesCarbonAtomSto3gMolecularPropertiesThroughJavaNativeUhf() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("carbon_native_properties"),
            ClassicalCalculationExecutorTest.carbonAtom(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.PARTIAL, result.status(), result.diagnostics().toString());
    Assertions.assertFalse(result.successful());
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals("UHF", ClassicalCalculationExecutorTest.value(result, "reference"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("total_energy")));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("homo_energy")));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("lumo_energy")));
    Assertions.assertTrue(
        Double.isFinite(result.requireScalar("vertical_ionization_potential_ev")));
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "mulliken_charge_rows").size());
    Assertions.assertEquals(6.0, result.requireScalar("mulliken_total_population"), 1.0E-7);
    Assertions.assertEquals(0.0, result.requireScalar("mulliken_total_charge"), 1.0E-7);
  }

  @Test
  void serviceExecutesJavaNativeBasisSetConvergenceWithoutPyScfBackend() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_basis_convergence"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.BASIS_SET_CONVERGENCE),
            List.of(ClassicalCalculationOption.text("basis_set_list", "STO-3G,6-31G")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_basis_convergence", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals(
        "java_native_batch_single_point_benchmark",
        ClassicalCalculationExecutorTest.value(result, "backend_contract"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "java_native_calculation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals(2.0, result.requireScalar("row_count"), 0.0);
    Assertions.assertEquals(
        2.0, result.requireScalar("basis_convergence_successful_point_count"), 0.0);
    Assertions.assertEquals(
        2.0, result.requireScalar("basis_convergence_converged_point_count"), 0.0);
    Assertions.assertEquals(
        "true",
        ClassicalCalculationExecutorTest.value(
            result, "basis_convergence_cbs_extrapolation_available"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("basis_convergence_last_energy")));
    Assertions.assertTrue(
        Double.isFinite(result.requireScalar("basis_convergence_cbs_energy_estimate")));
    Assertions.assertTrue(
        Double.isFinite(result.requireScalar("basis_convergence_estimated_basis_error")));
    Assertions.assertTrue(result.requireScalar("basis_convergence_estimated_basis_error") >= 0.0);
    final List<String> nativeRows =
        ClassicalCalculationExecutorTest.table(result, "java_native_basis_convergence_rows");
    Assertions.assertEquals(2, nativeRows.size());
    Assertions.assertEquals(
        nativeRows, ClassicalCalculationExecutorTest.table(result, "basis_convergence_rows"));
    for (final String row : nativeRows) {
      final String[] columns = row.split("\\t");
      Assertions.assertEquals("basis", columns[0]);
      Assertions.assertTrue(Double.isFinite(Double.parseDouble(columns[3])), row);
      Assertions.assertEquals("true", columns[5], row);
      Assertions.assertEquals("java_native", columns[6], row);
    }
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        "Java-native basis set convergence", result.coverageReport().items().get(0).model());
  }

  @Test
  void serviceReportsMixedCoverageForPartiallyUnsupportedNativeBasisBenchmark() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_basis_convergence_mixed_coverage"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.BASIS_SET_CONVERGENCE),
            List.of(ClassicalCalculationOption.text("basis_set_list", "STO-3G,cc-pVDZ")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().status());
    Assertions.assertEquals(
        ClassicalCoverageStatus.APPROXIMATE, result.coverageReport().items().get(0).status());
    Assertions.assertTrue(
        result.coverageReport().diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.message().contains("mixed")),
        result.coverageReport().diagnostics().toString());
    final List<String> rows =
        ClassicalCalculationExecutorTest.table(result, "java_native_basis_convergence_rows");
    Assertions.assertEquals(2, rows.size());
    Assertions.assertTrue(rows.get(0).contains("java_native"), rows.toString());
    Assertions.assertTrue(rows.get(1).contains("unavailable"), rows.toString());
  }

  @Test
  void serviceExecutesJavaNativeMethodBenchmarkWithoutPyScfBackend() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_method_benchmark"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.METHOD_BENCHMARK),
            List.of(ClassicalCalculationOption.text("method_list", "hf,mp2")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_method_benchmark", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "java_native", ClassicalCalculationExecutorTest.value(result, "backend"));
    Assertions.assertEquals(
        "java_native_batch_single_point_benchmark",
        ClassicalCalculationExecutorTest.value(result, "backend_contract"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "java_native_calculation"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_reference_route"));
    Assertions.assertEquals(
        "false", ClassicalCalculationExecutorTest.value(result, "external_package_parity_claimed"));
    Assertions.assertEquals(2.0, result.requireScalar("row_count"), 0.0);
    Assertions.assertEquals(
        2.0, result.requireScalar("method_benchmark_successful_method_count"), 0.0);
    Assertions.assertEquals(
        2.0, result.requireScalar("method_benchmark_converged_method_count"), 0.0);
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "method_benchmark_all_converged"));
    Assertions.assertFalse(
        ClassicalCalculationExecutorTest.value(result, "method_benchmark_lowest_energy_method")
            .isBlank());
    Assertions.assertTrue(Double.isFinite(result.requireScalar("method_benchmark_lowest_energy")));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("method_benchmark_energy_span")));
    Assertions.assertTrue(result.requireScalar("method_benchmark_energy_span") >= 0.0);
    final List<String> nativeRows =
        ClassicalCalculationExecutorTest.table(result, "java_native_method_benchmark_rows");
    Assertions.assertEquals(2, nativeRows.size());
    Assertions.assertEquals(
        nativeRows, ClassicalCalculationExecutorTest.table(result, "method_benchmark_rows"));
    for (final String row : nativeRows) {
      final String[] columns = row.split("\\t");
      Assertions.assertEquals("method", columns[0]);
      Assertions.assertTrue(Double.isFinite(Double.parseDouble(columns[3])), row);
      Assertions.assertEquals("true", columns[5], row);
      Assertions.assertEquals("java_native", columns[6], row);
    }
    final ClassicalCalculationRequest singlePointRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_method_benchmark_direct_hf"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final ClassicalCalculationResult singlePointResult = service.execute(singlePointRequest);
    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, singlePointResult.status());
    Assertions.assertEquals(
        singlePointResult.requireScalar("total_energy"),
        Double.parseDouble(nativeRows.get(0).split("\\t")[3]),
        1.0E-12);
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
    Assertions.assertEquals(
        "Java-native method benchmark", result.coverageReport().items().get(0).model());
  }

  @Test
  void serviceExecutesJavaNativePostHfMethodBenchmarkAliasesWithoutPyScfBackend() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_posthf_method_benchmark"),
            ClassicalCalculationExecutorTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.METHOD_BENCHMARK),
            List.of(ClassicalCalculationOption.text("method_list", "hf,mp2,cisd,fci")),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_method_benchmark", ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(4.0, result.requireScalar("row_count"), 0.0);
    Assertions.assertEquals(
        4.0, result.requireScalar("method_benchmark_successful_method_count"), 0.0);
    final List<String> rows =
        ClassicalCalculationExecutorTest.table(result, "java_native_method_benchmark_rows");
    Assertions.assertEquals(4, rows.size(), rows.toString());
    Assertions.assertTrue(
        rows.stream().anyMatch(row -> row.contains("\tCISD\t") && row.contains("\tjava_native")),
        rows.toString());
    Assertions.assertTrue(
        rows.stream().anyMatch(row -> row.contains("\tFCI\t") && row.contains("\tjava_native")),
        rows.toString());
  }

  @Test
  void serviceComputesReactionEnergyFromJavaNativeParticipantSinglePoints() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_dissociation_native"),
            "Hydrogen dissociation native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogenAtom(),
                        StoichiometricCoefficient.of(2)))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_dissociation_native_energy"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "first_principles_route_complete"));
    Assertions.assertEquals(
        "2.0",
        ClassicalCalculationExecutorTest.value(result, "first_principles_participant_count"));
    Assertions.assertEquals(
        "2.0",
        ClassicalCalculationExecutorTest.value(
            result, "quantum_program_preparable_participant_count"));
    Assertions.assertEquals(
        "ALL_PARTICIPANTS_HAMILTONIAN_AND_QUANTUM_PROGRAM_PREPARABLE",
        ClassicalCalculationExecutorTest.value(
            result, "reaction_quantum_program_preparation_status"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "reactant_participant_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "product_participant_count"));
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "reactant_stoichiometric_coefficient_sum"));
    Assertions.assertEquals(
        "2.0",
        ClassicalCalculationExecutorTest.value(result, "product_stoichiometric_coefficient_sum"));
    Assertions.assertEquals(
        0.18349599668600945, result.requireScalar("delta_electronic_energy"), 2.0E-5);
    Assertions.assertEquals(
        0.0, result.requireScalar("delta_electronic_energy_consistency_error"), 1.0E-12);
    Assertions.assertEquals(
        481.768672873567, result.requireScalar("delta_electronic_energy_kj_mol"), 6.0E-2);
    Assertions.assertTrue(
        ClassicalCalculationExecutorTest.table(result, "participant_energy_rows").size() >= 3);
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void
      serviceShortCircuitsStoichiometricallyIdenticalReactionWithoutPretendingToSolveParticipants() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_identity_native_shortcut"),
            "Hydrogen identity native shortcut",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_identity_native_shortcut_energy"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "stoichiometrically_identical_reaction_shortcut",
        ClassicalCalculationExecutorTest.value(result, "model_scope"));
    Assertions.assertEquals(
        "IDENTICAL_REACTION_NO_PARTICIPANT_SOLVE_REQUIRED",
        ClassicalCalculationExecutorTest.value(
            result, "reaction_quantum_program_preparation_status"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "first_principles_route_complete"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_electronic_energy"), 1.0E-12);
    Assertions.assertEquals(0.0, result.requireScalar("delta_gibbs"), 1.0E-12);
    Assertions.assertEquals(
        "0.0",
        ClassicalCalculationExecutorTest.value(
            result, "quantum_program_preparable_participant_count"));
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "participant_energy_rows").size());
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceComputesReactionEnergyFromCasscfOrbitalOptimizedParticipantSinglePoints() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_casscf_identity_native"),
            "Hydrogen CASSCF identity native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_casscf_identity_native_energy"),
            reaction,
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
                ElectronicStructureMethodName.of("CASSCF")),
            BasisSet.SIX_31G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.REACTION_ENERGY),
            List.of(
                ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
                ClassicalCalculationOption.integer("fci_beta_electrons", 1),
                ClassicalCalculationOption.text(
                    "casscf_orbital_rotation_space", "all_non_redundant"),
                ClassicalCalculationOption.integer("casscf_max_iterations", 12),
                ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
                ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
                ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
                ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "true", ClassicalCalculationExecutorTest.value(result, "first_principles_route_complete"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "reactant_participant_count"));
    Assertions.assertEquals(
        "1.0", ClassicalCalculationExecutorTest.value(result, "product_participant_count"));
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "reactant_stoichiometric_coefficient_sum"));
    Assertions.assertEquals(
        "1.0",
        ClassicalCalculationExecutorTest.value(result, "product_stoichiometric_coefficient_sum"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_electronic_energy"), 1.0E-12);
    Assertions.assertEquals(
        0.0, result.requireScalar("delta_electronic_energy_consistency_error"), 1.0E-12);
    final List<String> rows =
        ClassicalCalculationExecutorTest.table(result, "participant_energy_rows");
    Assertions.assertEquals(
        "side\tcoefficient\tmolecule\tmethod\timplemented_kernel\treference_method\tconverged"
            + "\telectron_count\tcorrelation_energy\texchange_correlation_energy"
            + "\tparticipant_total_energy\tweighted_total_energy"
            + "\tactive_space_used\torbital_optimized\tquantum_program_preparable\thamiltonian_preparable",
        rows.get(0));
    Assertions.assertTrue(
        rows.get(1).contains("active_space_casscf_orbital_optimization"), rows.get(1));
    Assertions.assertTrue(
        rows.get(2).contains("active_space_casscf_orbital_optimization"), rows.get(2));
    Assertions.assertEquals("true", rows.get(1).split("\\t")[14], rows.get(1));
    Assertions.assertEquals("true", rows.get(2).split("\\t")[14], rows.get(2));
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceComputesReactionEnergyFromDftParticipantSinglePoints() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_dft_identity_native"),
            "Hydrogen DFT identity native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_dft_identity_native_energy"),
            reaction,
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "0.0",
        ClassicalCalculationExecutorTest.value(
            result, "quantum_program_preparable_participant_count"));
    Assertions.assertEquals(
        "IDENTICAL_REACTION_NO_PARTICIPANT_SOLVE_REQUIRED",
        ClassicalCalculationExecutorTest.value(
            result, "reaction_quantum_program_preparation_status"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_electronic_energy"), 1.0E-12);
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "participant_energy_rows").size());
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceComputesReactionEnergyFromMp2ParticipantSinglePoints() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_mp2_identity_native"),
            "Hydrogen MP2 identity native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_mp2_identity_native_energy"),
            reaction,
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.PERTURBATION_THEORY,
                ElectronicStructureMethodName.of("MP2")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_electronic_energy"), 1.0E-12);
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "participant_energy_rows").size());
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceComputesReactionEnergyFromCcsdParticipantSinglePoints() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_ccsd_identity_native"),
            "Hydrogen CCSD identity native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_ccsd_identity_native_energy"),
            reaction,
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_electronic_energy"), 1.0E-12);
    Assertions.assertEquals(
        1, ClassicalCalculationExecutorTest.table(result, "participant_energy_rows").size());
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, result.coverageReport().status());
  }

  @Test
  void serviceComputesReactionThermodynamicsFromJavaNativeParticipantSinglePoints() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("h2_thermodynamic_identity_native"),
            "Hydrogen thermodynamic identity native",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalCalculationExecutorTest.hydrogen(),
                        StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_thermodynamic_identity_native_energy"),
            reaction,
            ElectronicStructureMethod.densityFunctional("PBE"),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(
                ClassicalCalculationOption.floating("thermodynamics_temperature_kelvin", 350.0)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    Assertions.assertEquals(
        "java_native_single_point_reaction_energy",
        ClassicalCalculationExecutorTest.value(result, "method"));
    Assertions.assertEquals(
        "identical_reactant_product_state",
        ClassicalCalculationExecutorTest.value(result, "thermodynamic_model"));
    Assertions.assertEquals(
        "PBE", ClassicalCalculationExecutorTest.value(result, "electronic_energy_basis"));
    Assertions.assertEquals("350.0", ClassicalCalculationExecutorTest.value(result, "temperature"));
    Assertions.assertEquals(0.0, result.requireScalar("delta_enthalpy"), 1.0E-12);
    Assertions.assertEquals(0.0, result.requireScalar("delta_gibbs"), 1.0E-12);
    Assertions.assertEquals(
        result.requireScalar("delta_thermal_gibbs_correction"),
        result.requireScalar("temperature_entropy_correction")
            + result.requireScalar("condition_bias"),
        1.0E-12);
    Assertions.assertEquals(
        "IDENTICAL_REACTION_NO_PARTICIPANT_SOLVE_REQUIRED",
        ClassicalCalculationExecutorTest.value(
            result, "reaction_quantum_program_preparation_status"));
    Assertions.assertFalse(result.diagnostics().hasWarnings(), result.diagnostics().toString());
  }

  private static Molecule ethanol() {
    return Molecule.of(
        MoleculeId.of("ethanol"),
        "Ethanol",
        List.of(
            ClassicalCalculationExecutorTest.atom("a0", "C", 0.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a1", "C", 1.5, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a2", "O", 2.7, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a3", "H", -0.5, 0.9, 0.0),
            ClassicalCalculationExecutorTest.atom("a4", "H", -0.5, -0.9, 0.0),
            ClassicalCalculationExecutorTest.atom("a5", "H", 0.0, 0.0, 1.0),
            ClassicalCalculationExecutorTest.atom("a6", "H", 1.9, 0.9, 0.0),
            ClassicalCalculationExecutorTest.atom("a7", "H", 1.9, -0.9, 0.0),
            ClassicalCalculationExecutorTest.atom("a8", "H", 3.2, 0.7, 0.0)),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a3"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a4"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a5"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a7"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("a8"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject
      subjectFor(final ClassicalCalculationKind kind) {
    if (ClassicalCalculationExecutorTest.reactionCalculation(kind)) {
      return ClassicalCalculationExecutorTest.nicotinonitrileBenzylamineBenchmarkReaction();
    }
    return ClassicalCalculationExecutorTest.ethanol();
  }

  private static boolean reactionCalculation(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.KINETIC_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION
        || kind == ClassicalCalculationKind.REACTION_ENERGY
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.IRC_ANALYSIS
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH
        || kind == ClassicalCalculationKind.SELECTIVITY_ANALYSIS
        || kind == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_CONDITION_SCREENING
        || kind == ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION
        || kind == ClassicalCalculationKind.SOLVENT_SCREENING
        || kind == ClassicalCalculationKind.CATALYST_SCREENING
        || kind == ClassicalCalculationKind.CONCENTRATION_SCREENING
        || kind == ClassicalCalculationKind.MECHANISM_ENUMERATION
        || kind == ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS
        || kind == ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS
        || kind == ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION
        || kind == ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS
        || kind == ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS
        || kind == ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS
        || kind == ClassicalCalculationKind.MICROKINETIC_MODELING
        || kind == ClassicalCalculationKind.RETROSYNTHESIS_PLANNING
        || kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS;
  }

  private static boolean hasGenericCalculationFallback(final ClassicalCalculationResult result) {
    final List<ClassicalDiagnostic> diagnostics = result.diagnostics().diagnostics();
    for (int i = 0; i < diagnostics.size(); ++i) {
      final ClassicalDiagnostic diagnostic = diagnostics.get(i);
      if (diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED
          && "calculationKind".equals(diagnostic.target().kind())
          && diagnostic.message().contains("no first-principles implementation")) {
        return true;
      }
    }
    return false;
  }

  private static Molecule carbonylAlcoholNitrileProbe() {
    return Molecule.of(
        MoleculeId.of("carbonyl_alcohol_nitrile_probe"),
        "Carbonyl alcohol nitrile probe",
        List.of(
            ClassicalCalculationExecutorTest.atom("a0", "C", 0.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a1", "O", 1.2, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a2", "H", -0.8, 0.8, 0.0),
            ClassicalCalculationExecutorTest.atom("a3", "C", -1.2, -0.2, 0.0),
            ClassicalCalculationExecutorTest.atom("a4", "C", -2.4, -0.2, 0.0),
            ClassicalCalculationExecutorTest.atom("a5", "N", -3.5, -0.2, 0.0),
            ClassicalCalculationExecutorTest.atom("a6", "C", -1.2, -1.5, 0.0),
            ClassicalCalculationExecutorTest.atom("a7", "O", -1.2, -2.7, 0.0),
            ClassicalCalculationExecutorTest.atom("a8", "H", -0.4, -3.1, 0.0)),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a3"), BondType.SINGLE),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.TRIPLE),
            Bond.of(AtomId.of("a3"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a7"), BondType.SINGLE),
            Bond.of(AtomId.of("a7"), AtomId.of("a8"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        MoleculeId.of("hydrogen"),
        "Hydrogen",
        List.of(
            ClassicalCalculationExecutorTest.atom("h0", "H", 0.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("h1", "H", 0.0, 0.0, 0.741848095192207)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static ClassicalCalculationRequest uhfHydrogenAtomHamiltonianRequest(
      final String id, final ElectronicStructureMethod method, final String channel) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(id),
        ClassicalCalculationExecutorTest.hydrogenAtom(),
        method,
        BasisSet.STO_3G,
        ActiveSpace.of(1, 1),
        List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(
            ClassicalCalculationOption.integer("active_space_first_orbital", 0),
            ClassicalCalculationOption.text("uhf_hamiltonian_orbital_channel", channel)),
        ChemistryMetadata.EMPTY);
  }

  private static Molecule hydrogenChloride() {
    return Molecule.of(
        MoleculeId.of("hydrogen_chloride"),
        "Hydrogen chloride",
        List.of(
            ClassicalCalculationExecutorTest.atom("h0", "H", 0.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("cl0", "Cl", 0.0, 0.0, 1.27455)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("cl0"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenBromide() {
    return Molecule.of(
        MoleculeId.of("hydrogen_bromide"),
        "Hydrogen bromide",
        List.of(
            ClassicalCalculationExecutorTest.atom("h0", "H", 0.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("br0", "Br", 0.0, 0.0, 1.414)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("br0"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule heteroarylBromideProbe() {
    return Molecule.of(
        MoleculeId.of("heteroaryl_bromide_probe"),
        "Heteroaryl bromide probe",
        List.of(
            ClassicalCalculationExecutorTest.atom("n1", "N", 0.0, 1.40, 0.0),
            ClassicalCalculationExecutorTest.atom("c2", "C", 1.21, 0.70, 0.0),
            ClassicalCalculationExecutorTest.atom("c3", "C", 1.21, -0.70, 0.0),
            ClassicalCalculationExecutorTest.atom("c4", "C", 0.0, -1.40, 0.0),
            ClassicalCalculationExecutorTest.atom("c5", "C", -1.21, -0.70, 0.0),
            ClassicalCalculationExecutorTest.atom("c6", "C", -1.21, 0.70, 0.0),
            ClassicalCalculationExecutorTest.atom("cl7", "Cl", 2.72, 1.55, 0.0),
            ClassicalCalculationExecutorTest.atom("br8", "Br", -2.95, -1.70, 0.0),
            ClassicalCalculationExecutorTest.atom("c9", "C", 0.0, -2.78, 0.0),
            ClassicalCalculationExecutorTest.atom("n10", "N", 0.0, -3.94, 0.0)),
        List.of(
            Bond.of(AtomId.of("n1"), AtomId.of("c2"), BondType.AROMATIC),
            Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
            Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
            Bond.of(AtomId.of("c4"), AtomId.of("c5"), BondType.AROMATIC),
            Bond.of(AtomId.of("c5"), AtomId.of("c6"), BondType.AROMATIC),
            Bond.of(AtomId.of("c6"), AtomId.of("n1"), BondType.AROMATIC),
            Bond.of(AtomId.of("c2"), AtomId.of("cl7"), BondType.SINGLE),
            Bond.of(AtomId.of("c5"), AtomId.of("br8"), BondType.SINGLE),
            Bond.of(AtomId.of("c4"), AtomId.of("c9"), BondType.SINGLE),
            Bond.of(AtomId.of("c9"), AtomId.of("n10"), BondType.TRIPLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule heteroarylBromideProbeCopy() {
    return Molecule.of(
        MoleculeId.of("heteroaryl_bromide_probe_copy"),
        "Heteroaryl bromide probe copy",
        ClassicalCalculationExecutorTest.heteroarylBromideProbe().atoms(),
        ClassicalCalculationExecutorTest.heteroarylBromideProbe().bonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenAtom() {
    return Molecule.of(
        MoleculeId.of("hydrogen_atom"),
        "Hydrogen atom",
        List.of(
            Atom.of(
                AtomId.of("h0"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM),
                FormalCharge.NEUTRAL,
                null,
                RadicalState.of(1),
                ChemistryMetadata.EMPTY)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule stretchedHydrogen() {
    return Molecule.of(
        MoleculeId.of("stretched_hydrogen"),
        "Stretched hydrogen",
        List.of(
            ClassicalCalculationExecutorTest.atom("h0", "H", 0.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("h1", "H", 0.0, 0.0, 1.6)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static BasisSet largeHydrogenStructuredBasis(final int shellCount) {
    final java.util.ArrayList<BasisShell> shells = new java.util.ArrayList<BasisShell>(shellCount);
    for (int i = 0; i < shellCount; ++i) {
      final double exponent = 0.045 * Math.pow(1.32, i);
      shells.add(
          BasisShell.of(BasisShellAngularMomentum.S, List.of(BasisPrimitive.of(exponent, 1.0))));
    }
    return BasisSet.structured(
        BasisSetName.of("large-hydrogen-s-" + shellCount),
        BasisSetDefinition.of(
            List.of(BasisAtomDefinition.of(ElementSymbol.of("H"), List.copyOf(shells)))));
  }

  private static Molecule ringMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            ClassicalCalculationExecutorTest.atom("a0", firstAtomSymbol, 1.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a1", "C", 0.5, 0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a2", "C", -0.5, 0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a3", "C", -1.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a4", "C", -0.5, -0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a5", "C", 0.5, -0.8660254037844386, 0.0)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("a0", "a1"),
            ClassicalCalculationExecutorTest.ringBond("a1", "a2"),
            ClassicalCalculationExecutorTest.ringBond("a2", "a3"),
            ClassicalCalculationExecutorTest.ringBond("a3", "a4"),
            ClassicalCalculationExecutorTest.ringBond("a4", "a5"),
            ClassicalCalculationExecutorTest.ringBond("a5", "a0")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule largeClosedShellCarbonNitrogenHeterocycle() {
    return Molecule.of(
        MoleculeId.of("large_closed_shell_carbon_nitrogen_heterocycle"),
        "Large closed-shell carbon-nitrogen heterocycle",
        List.of(
            ClassicalCalculationExecutorTest.atom("n1", "N", 2.10, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c2", "C", 1.05, 1.82, 0.0),
            ClassicalCalculationExecutorTest.atom("c3", "C", -1.05, 1.82, 0.0),
            ClassicalCalculationExecutorTest.atom("n4", "N", -2.10, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c5", "C", -1.05, -1.82, 0.0),
            ClassicalCalculationExecutorTest.atom("c6", "C", 1.05, -1.82, 0.0),
            ClassicalCalculationExecutorTest.atom("c7", "C", 3.55, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c8", "C", 4.28, 1.26, 0.0),
            ClassicalCalculationExecutorTest.atom("c9", "C", 5.74, 1.26, 0.0),
            ClassicalCalculationExecutorTest.atom("c10", "C", 6.47, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c11", "C", 5.74, -1.26, 0.0),
            ClassicalCalculationExecutorTest.atom("c12", "C", 4.28, -1.26, 0.0)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("n1", "c2"),
            ClassicalCalculationExecutorTest.ringBond("c2", "c3"),
            ClassicalCalculationExecutorTest.ringBond("c3", "n4"),
            ClassicalCalculationExecutorTest.ringBond("n4", "c5"),
            ClassicalCalculationExecutorTest.ringBond("c5", "c6"),
            ClassicalCalculationExecutorTest.ringBond("c6", "n1"),
            ClassicalCalculationExecutorTest.ringBond("c7", "c8"),
            ClassicalCalculationExecutorTest.ringBond("c8", "c9"),
            ClassicalCalculationExecutorTest.ringBond("c9", "c10"),
            ClassicalCalculationExecutorTest.ringBond("c10", "c11"),
            ClassicalCalculationExecutorTest.ringBond("c11", "c12"),
            ClassicalCalculationExecutorTest.ringBond("c12", "c7"),
            Bond.of(AtomId.of("n1"), AtomId.of("c7"), BondType.SINGLE),
            Bond.of(AtomId.of("c5"), AtomId.of("c11"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule functionalizedRingMolecule(
      final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            ClassicalCalculationExecutorTest.atom("a0", firstAtomSymbol, 1.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a1", "C", 0.5, 0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a2", "C", -0.5, 0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a3", "C", -1.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a4", "C", -0.5, -0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a5", "C", 0.5, -0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a6", "C", 1.8, 0.0, 0.0)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("a0", "a1"),
            ClassicalCalculationExecutorTest.ringBond("a1", "a2"),
            ClassicalCalculationExecutorTest.ringBond("a2", "a3"),
            ClassicalCalculationExecutorTest.ringBond("a3", "a4"),
            ClassicalCalculationExecutorTest.ringBond("a4", "a5"),
            ClassicalCalculationExecutorTest.ringBond("a5", "a0"),
            Bond.of(AtomId.of("a0"), AtomId.of("a6"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule furanRadical() {
    return Molecule.of(
        MoleculeId.of("furan_radical"),
        "Furan radical",
        List.of(
            ClassicalCalculationExecutorTest.radicalAtom("o0", "O", 1.16, 0.0, 0.0, 1),
            ClassicalCalculationExecutorTest.atom("c1", "C", 0.358, 1.102, 0.0),
            ClassicalCalculationExecutorTest.atom("c2", "C", -0.938, 0.681, 0.0),
            ClassicalCalculationExecutorTest.atom("c3", "C", -0.938, -0.681, 0.0),
            ClassicalCalculationExecutorTest.atom("c4", "C", 0.358, -1.102, 0.0),
            ClassicalCalculationExecutorTest.atom("h1", "H", 1.260, 1.951, 0.0),
            ClassicalCalculationExecutorTest.atom("h2", "H", -1.889, 1.173, 0.0),
            ClassicalCalculationExecutorTest.atom("h3", "H", -1.889, -1.173, 0.0)),
        ClassicalCalculationExecutorTest.furanRadicalBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule azirineRadicalProbe() {
    return Molecule.of(
        MoleculeId.of("azirine_radical_probe"),
        "Azirine radical probe",
        List.of(
            ClassicalCalculationExecutorTest.radicalAtom("n0", "N", 0.0, 1.02, 0.0, 1),
            ClassicalCalculationExecutorTest.atom("c1", "C", -0.86, -0.52, 0.0),
            ClassicalCalculationExecutorTest.atom("c2", "C", 0.86, -0.52, 0.0),
            ClassicalCalculationExecutorTest.atom("h1", "H", -1.78, -1.08, 0.0),
            ClassicalCalculationExecutorTest.atom("h2", "H", 1.78, -1.08, 0.0)),
        List.of(
            Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.AROMATIC),
            Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
            Bond.of(AtomId.of("c2"), AtomId.of("n0"), BondType.AROMATIC),
            Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
            Bond.of(AtomId.of("c2"), AtomId.of("h2"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static List<Bond> furanRadicalBonds() {
    return List.of(
        Bond.of(AtomId.of("o0"), AtomId.of("c1"), BondType.AROMATIC),
        Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
        Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
        Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
        Bond.of(AtomId.of("c4"), AtomId.of("o0"), BondType.AROMATIC),
        Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
        Bond.of(AtomId.of("c2"), AtomId.of("h2"), BondType.SINGLE),
        Bond.of(AtomId.of("c3"), AtomId.of("h3"), BondType.SINGLE));
  }

  private static Bond ringBond(final String first, final String second) {
    return Bond.of(AtomId.of(first), AtomId.of(second), BondType.AROMATIC);
  }

  private static Reaction nicotinonitrileBenzylamineBenchmarkReaction() {
    return Reaction.of(
        ReactionId.of("snar_nicotinonitrile_benzylamine_peg4000"),
        "2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile benzylamine substitution",
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ClassicalCalculationExecutorTest.chloroDimethylNicotinonitrile(),
                    StoichiometricCoefficient.ONE),
                ReactionParticipant.of(
                    ClassicalCalculationExecutorTest.benzylamine(),
                    StoichiometricCoefficient.ONE))),
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ClassicalCalculationExecutorTest.benzylaminoDimethylNicotinonitrile(),
                    StoichiometricCoefficient.ONE),
                ReactionParticipant.of(
                    ClassicalCalculationExecutorTest.hydrogenChloride(),
                    StoichiometricCoefficient.ONE))),
        ReactionConditions.of(
            Temperature.of(120.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.of(
                    ReactionComponentRole.SOLVENT,
                    "PEG4000",
                    SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                    null,
                    ReactionPhase.SOLUTION,
                    "70 g PEG4000, atmospheric pressure"),
                ReactionConditionComponent.of(
                    ReactionComponentRole.REACTANT,
                    "2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile charge",
                    SubstanceAmount.of(10.0, SubstanceAmountUnit.GRAM),
                    null,
                    ReactionPhase.SOLID,
                    "10 g substrate"),
                ReactionConditionComponent.of(
                    ReactionComponentRole.REACTANT,
                    "Benzylamine charge",
                    SubstanceAmount.of(6.5, SubstanceAmountUnit.GRAM),
                    null,
                    ReactionPhase.LIQUID,
                    "6.5 g benzylamine")),
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING)),
            "SNAr-like amination benchmark: 10 g substrate, 6.5 g benzylamine, PEG4000 70 g, 120 C, 3 h, atmospheric pressure."),
        ChemistryMetadata.EMPTY);
  }

  private static Molecule chloroDimethylNicotinonitrile() {
    return Molecule.of(
        MoleculeId.of("two_chloro_four_six_dimethyl_three_pyridinecarbonitrile"),
        "2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile",
        List.of(
            ClassicalCalculationExecutorTest.atom("n1", "N", 1.40, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c2", "C", 0.70, 1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("c3", "C", -0.70, 1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("c4", "C", -1.40, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c5", "C", -0.70, -1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("c6", "C", 0.70, -1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("cl2", "Cl", 1.55, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("cn3", "C", -1.55, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("nn3", "N", -2.18, 3.76, 0.0),
            ClassicalCalculationExecutorTest.atom("m4", "C", -2.90, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("m6", "C", 1.45, -2.51, 0.0),
            ClassicalCalculationExecutorTest.atom("h5", "H", -1.24, -2.15, 0.0),
            ClassicalCalculationExecutorTest.atom("h4a", "H", -3.26, 0.00, 1.02),
            ClassicalCalculationExecutorTest.atom("h4b", "H", -3.26, 0.88, -0.51),
            ClassicalCalculationExecutorTest.atom("h4c", "H", -3.26, -0.88, -0.51),
            ClassicalCalculationExecutorTest.atom("h6a", "H", 1.81, -2.51, 1.02),
            ClassicalCalculationExecutorTest.atom("h6b", "H", 2.06, -3.27, -0.51),
            ClassicalCalculationExecutorTest.atom("h6c", "H", 0.82, -3.27, -0.51)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("n1", "c2"),
            ClassicalCalculationExecutorTest.ringBond("c2", "c3"),
            ClassicalCalculationExecutorTest.ringBond("c3", "c4"),
            ClassicalCalculationExecutorTest.ringBond("c4", "c5"),
            ClassicalCalculationExecutorTest.ringBond("c5", "c6"),
            ClassicalCalculationExecutorTest.ringBond("c6", "n1"),
            Bond.of(AtomId.of("c2"), AtomId.of("cl2"), BondType.SINGLE),
            Bond.of(AtomId.of("c3"), AtomId.of("cn3"), BondType.SINGLE),
            Bond.of(AtomId.of("cn3"), AtomId.of("nn3"), BondType.TRIPLE),
            Bond.of(AtomId.of("c4"), AtomId.of("m4"), BondType.SINGLE),
            Bond.of(AtomId.of("c6"), AtomId.of("m6"), BondType.SINGLE),
            Bond.of(AtomId.of("c5"), AtomId.of("h5"), BondType.SINGLE),
            Bond.of(AtomId.of("m4"), AtomId.of("h4a"), BondType.SINGLE),
            Bond.of(AtomId.of("m4"), AtomId.of("h4b"), BondType.SINGLE),
            Bond.of(AtomId.of("m4"), AtomId.of("h4c"), BondType.SINGLE),
            Bond.of(AtomId.of("m6"), AtomId.of("h6a"), BondType.SINGLE),
            Bond.of(AtomId.of("m6"), AtomId.of("h6b"), BondType.SINGLE),
            Bond.of(AtomId.of("m6"), AtomId.of("h6c"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule benzylamine() {
    return Molecule.of(
        MoleculeId.of("benzylamine"),
        "Benzylamine",
        List.of(
            ClassicalCalculationExecutorTest.atom("p1", "C", 1.40, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("p2", "C", 0.70, 1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("p3", "C", -0.70, 1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("p4", "C", -1.40, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("p5", "C", -0.70, -1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("p6", "C", 0.70, -1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("ch2", "C", 2.90, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("n", "N", 4.20, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("hp2", "H", 1.24, 2.15, 0.0),
            ClassicalCalculationExecutorTest.atom("hp3", "H", -1.24, 2.15, 0.0),
            ClassicalCalculationExecutorTest.atom("hp4", "H", -2.48, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("hp5", "H", -1.24, -2.15, 0.0),
            ClassicalCalculationExecutorTest.atom("hp6", "H", 1.24, -2.15, 0.0),
            ClassicalCalculationExecutorTest.atom("hch2a", "H", 3.08, 0.88, 0.56),
            ClassicalCalculationExecutorTest.atom("hch2b", "H", 3.08, -0.88, -0.56),
            ClassicalCalculationExecutorTest.atom("hn1", "H", 4.55, 0.82, 0.45),
            ClassicalCalculationExecutorTest.atom("hn2", "H", 4.55, -0.82, -0.45)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("p1", "p2"),
            ClassicalCalculationExecutorTest.ringBond("p2", "p3"),
            ClassicalCalculationExecutorTest.ringBond("p3", "p4"),
            ClassicalCalculationExecutorTest.ringBond("p4", "p5"),
            ClassicalCalculationExecutorTest.ringBond("p5", "p6"),
            ClassicalCalculationExecutorTest.ringBond("p6", "p1"),
            Bond.of(AtomId.of("p1"), AtomId.of("ch2"), BondType.SINGLE),
            Bond.of(AtomId.of("ch2"), AtomId.of("n"), BondType.SINGLE),
            Bond.of(AtomId.of("p2"), AtomId.of("hp2"), BondType.SINGLE),
            Bond.of(AtomId.of("p3"), AtomId.of("hp3"), BondType.SINGLE),
            Bond.of(AtomId.of("p4"), AtomId.of("hp4"), BondType.SINGLE),
            Bond.of(AtomId.of("p5"), AtomId.of("hp5"), BondType.SINGLE),
            Bond.of(AtomId.of("p6"), AtomId.of("hp6"), BondType.SINGLE),
            Bond.of(AtomId.of("ch2"), AtomId.of("hch2a"), BondType.SINGLE),
            Bond.of(AtomId.of("ch2"), AtomId.of("hch2b"), BondType.SINGLE),
            Bond.of(AtomId.of("n"), AtomId.of("hn1"), BondType.SINGLE),
            Bond.of(AtomId.of("n"), AtomId.of("hn2"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule benzylaminoDimethylNicotinonitrile() {
    return Molecule.of(
        MoleculeId.of("two_benzylamino_four_six_dimethylnicotinonitrile"),
        "2-(Benzylamino)-4,6-dimethylnicotinonitrile",
        List.of(
            ClassicalCalculationExecutorTest.atom("n1", "N", 1.40, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c2", "C", 0.70, 1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("c3", "C", -0.70, 1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("c4", "C", -1.40, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("c5", "C", -0.70, -1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("c6", "C", 0.70, -1.21, 0.0),
            ClassicalCalculationExecutorTest.atom("nam", "N", 1.55, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("hnam", "H", 1.94, 3.45, 0.52),
            ClassicalCalculationExecutorTest.atom("cn3", "C", -1.55, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("nn3", "N", -2.18, 3.76, 0.0),
            ClassicalCalculationExecutorTest.atom("m4", "C", -2.90, 0.00, 0.0),
            ClassicalCalculationExecutorTest.atom("m6", "C", 1.45, -2.51, 0.0),
            ClassicalCalculationExecutorTest.atom("h5", "H", -1.24, -2.15, 0.0),
            ClassicalCalculationExecutorTest.atom("h4a", "H", -3.26, 0.00, 1.02),
            ClassicalCalculationExecutorTest.atom("h4b", "H", -3.26, 0.88, -0.51),
            ClassicalCalculationExecutorTest.atom("h4c", "H", -3.26, -0.88, -0.51),
            ClassicalCalculationExecutorTest.atom("h6a", "H", 1.81, -2.51, 1.02),
            ClassicalCalculationExecutorTest.atom("h6b", "H", 2.06, -3.27, -0.51),
            ClassicalCalculationExecutorTest.atom("h6c", "H", 0.82, -3.27, -0.51),
            ClassicalCalculationExecutorTest.atom("ch2", "C", 2.86, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("hch2a", "H", 3.05, 3.54, 0.56),
            ClassicalCalculationExecutorTest.atom("hch2b", "H", 3.05, 1.80, -0.56),
            ClassicalCalculationExecutorTest.atom("p1", "C", 4.25, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("p2", "C", 4.95, 3.88, 0.0),
            ClassicalCalculationExecutorTest.atom("p3", "C", 6.35, 3.88, 0.0),
            ClassicalCalculationExecutorTest.atom("p4", "C", 7.05, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("p5", "C", 6.35, 1.46, 0.0),
            ClassicalCalculationExecutorTest.atom("p6", "C", 4.95, 1.46, 0.0),
            ClassicalCalculationExecutorTest.atom("hp2", "H", 4.41, 4.82, 0.0),
            ClassicalCalculationExecutorTest.atom("hp3", "H", 6.89, 4.82, 0.0),
            ClassicalCalculationExecutorTest.atom("hp4", "H", 8.13, 2.67, 0.0),
            ClassicalCalculationExecutorTest.atom("hp5", "H", 6.89, 0.52, 0.0),
            ClassicalCalculationExecutorTest.atom("hp6", "H", 4.41, 0.52, 0.0)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("n1", "c2"),
            ClassicalCalculationExecutorTest.ringBond("c2", "c3"),
            ClassicalCalculationExecutorTest.ringBond("c3", "c4"),
            ClassicalCalculationExecutorTest.ringBond("c4", "c5"),
            ClassicalCalculationExecutorTest.ringBond("c5", "c6"),
            ClassicalCalculationExecutorTest.ringBond("c6", "n1"),
            Bond.of(AtomId.of("c2"), AtomId.of("nam"), BondType.SINGLE),
            Bond.of(AtomId.of("nam"), AtomId.of("hnam"), BondType.SINGLE),
            Bond.of(AtomId.of("nam"), AtomId.of("ch2"), BondType.SINGLE),
            Bond.of(AtomId.of("c3"), AtomId.of("cn3"), BondType.SINGLE),
            Bond.of(AtomId.of("cn3"), AtomId.of("nn3"), BondType.TRIPLE),
            Bond.of(AtomId.of("c4"), AtomId.of("m4"), BondType.SINGLE),
            Bond.of(AtomId.of("c6"), AtomId.of("m6"), BondType.SINGLE),
            Bond.of(AtomId.of("c5"), AtomId.of("h5"), BondType.SINGLE),
            Bond.of(AtomId.of("m4"), AtomId.of("h4a"), BondType.SINGLE),
            Bond.of(AtomId.of("m4"), AtomId.of("h4b"), BondType.SINGLE),
            Bond.of(AtomId.of("m4"), AtomId.of("h4c"), BondType.SINGLE),
            Bond.of(AtomId.of("m6"), AtomId.of("h6a"), BondType.SINGLE),
            Bond.of(AtomId.of("m6"), AtomId.of("h6b"), BondType.SINGLE),
            Bond.of(AtomId.of("m6"), AtomId.of("h6c"), BondType.SINGLE),
            Bond.of(AtomId.of("ch2"), AtomId.of("hch2a"), BondType.SINGLE),
            Bond.of(AtomId.of("ch2"), AtomId.of("hch2b"), BondType.SINGLE),
            Bond.of(AtomId.of("ch2"), AtomId.of("p1"), BondType.SINGLE),
            ClassicalCalculationExecutorTest.ringBond("p1", "p2"),
            ClassicalCalculationExecutorTest.ringBond("p2", "p3"),
            ClassicalCalculationExecutorTest.ringBond("p3", "p4"),
            ClassicalCalculationExecutorTest.ringBond("p4", "p5"),
            ClassicalCalculationExecutorTest.ringBond("p5", "p6"),
            ClassicalCalculationExecutorTest.ringBond("p6", "p1"),
            Bond.of(AtomId.of("p2"), AtomId.of("hp2"), BondType.SINGLE),
            Bond.of(AtomId.of("p3"), AtomId.of("hp3"), BondType.SINGLE),
            Bond.of(AtomId.of("p4"), AtomId.of("hp4"), BondType.SINGLE),
            Bond.of(AtomId.of("p5"), AtomId.of("hp5"), BondType.SINGLE),
            Bond.of(AtomId.of("p6"), AtomId.of("hp6"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule carbonAtom() {
    return Molecule.of(
        MoleculeId.of("carbon_atom"),
        "Carbon atom",
        List.of(ClassicalCalculationExecutorTest.atom("c0", "C", 0.0, 0.0, 0.0)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(3));
  }

  private static Molecule berylliumAtom() {
    return Molecule.of(
        MoleculeId.of("beryllium_atom"),
        "Beryllium atom",
        List.of(ClassicalCalculationExecutorTest.atom("be0", "Be", 0.0, 0.0, 0.0)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule ironAtom() {
    return Molecule.of(
        MoleculeId.of("iron_atom"),
        "Iron atom",
        List.of(ClassicalCalculationExecutorTest.atom("fe0", "Fe", 0.0, 0.0, 0.0)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule iodineAtom() {
    return Molecule.of(
        MoleculeId.of("iodine_atom"),
        "Iodine atom",
        List.of(ClassicalCalculationExecutorTest.atom("i0", "I", 0.0, 0.0, 0.0)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static BasisShell shell(final BasisShellAngularMomentum angularMomentum) {
    return BasisShell.of(
        angularMomentum,
        List.of(BasisPrimitive.of(0.25 + (double) angularMomentum.angularMomentum(), 1.0)));
  }

  private static Molecule iodobenzene() {
    return Molecule.of(
        MoleculeId.of("iodobenzene"),
        "Iodobenzene",
        List.of(
            ClassicalCalculationExecutorTest.atom("a0", "C", 1.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a1", "C", 0.5, 0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a2", "C", -0.5, 0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a3", "C", -1.0, 0.0, 0.0),
            ClassicalCalculationExecutorTest.atom("a4", "C", -0.5, -0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a5", "C", 0.5, -0.8660254037844386, 0.0),
            ClassicalCalculationExecutorTest.atom("a6", "I", 2.2, 0.0, 0.0)),
        List.of(
            ClassicalCalculationExecutorTest.ringBond("a0", "a1"),
            ClassicalCalculationExecutorTest.ringBond("a1", "a2"),
            ClassicalCalculationExecutorTest.ringBond("a2", "a3"),
            ClassicalCalculationExecutorTest.ringBond("a3", "a4"),
            ClassicalCalculationExecutorTest.ringBond("a4", "a5"),
            ClassicalCalculationExecutorTest.ringBond("a5", "a0"),
            Bond.of(AtomId.of("a0"), AtomId.of("a6"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Atom atom(
      final String id, final String symbol, final double x, final double y, final double z) {
    return ClassicalCalculationExecutorTest.radicalAtom(id, symbol, x, y, z, 0);
  }

  private static Atom radicalAtom(
      final String id,
      final String symbol,
      final double x,
      final double y,
      final double z,
      final int unpairedElectronCount) {
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.of(unpairedElectronCount),
        ChemistryMetadata.EMPTY);
  }
}
