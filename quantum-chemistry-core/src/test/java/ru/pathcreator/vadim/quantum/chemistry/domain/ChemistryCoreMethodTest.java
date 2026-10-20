/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperimentId;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpaceResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShellAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.CombinatorialCount;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ConvergenceCriteria;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreMethodTest {

  ChemistryCoreMethodTest() {}

  @Test
  void activeSpaceResourceEstimateExposesQuantumAndClassicalScale() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    ActiveSpaceResourceEstimate estimate = activeSpace.resourceEstimate();
    Assertions.assertEquals((int) 4, (int) estimate.spinOrbitalCount());
    Assertions.assertEquals((int) 4, (int) estimate.qubitCount());
    Assertions.assertEquals((int) 2, (int) estimate.virtualSpinOrbitalCount());
    Assertions.assertEquals((long) 4L, (long) estimate.singleExcitationCount());
    Assertions.assertEquals((long) 1L, (long) estimate.doubleExcitationCount());
    Assertions.assertEquals((long) 5L, (long) estimate.uccsdParameterCount());
    Assertions.assertTrue((boolean) estimate.determinantCount().exact());
    Assertions.assertEquals((long) 6L, (long) estimate.determinantCount().value());
  }

  @Test
  void activeSpaceResourceEstimateSaturatesHugeDeterminantCount() {
    final ActiveSpace activeSpace = ActiveSpace.of((int) 1000, (int) 1000);
    final ActiveSpaceResourceEstimate estimate = activeSpace.resourceEstimate();
    Assertions.assertFalse((boolean) estimate.determinantCount().exact());
    Assertions.assertTrue((boolean) estimate.determinantCount().saturatedValue());
    Assertions.assertEquals((long) Long.MAX_VALUE, (long) estimate.determinantCount().value());
  }

  @Test
  void combinatorialCountKeepsExactAndSaturatedValuesDistinct() {
    final CombinatorialCount exact = CombinatorialCount.exact((long) Long.MAX_VALUE);
    final CombinatorialCount saturated = CombinatorialCount.saturated();
    Assertions.assertTrue((boolean) exact.exact());
    Assertions.assertFalse((boolean) saturated.exact());
    Assertions.assertEquals((long) Long.MAX_VALUE, (long) exact.value());
    Assertions.assertEquals((long) Long.MAX_VALUE, (long) saturated.value());
    Assertions.assertNotEquals((Object) exact, (Object) saturated);
    Assertions.assertNotEquals((int) exact.hashCode(), (int) saturated.hashCode());
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> CombinatorialCount.exact((long) -1L));
  }

  @Test
  void electronicStructureMethodNameNormalizesAndRejectsBlankText() {
    final ElectronicStructureMethodName name =
        ElectronicStructureMethodName.of((String) "  UCCSD(T)  ");
    Assertions.assertEquals((Object) "UCCSD(T)", (Object) name.value());
    Assertions.assertEquals(
        (Object) name, (Object) ElectronicStructureMethodName.of((String) "UCCSD(T)"));
    Assertions.assertEquals(
        (int) name.hashCode(),
        (int) ElectronicStructureMethodName.of((String) "UCCSD(T)").hashCode());
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ElectronicStructureMethodName.of((String) "   "));
  }

  @Test
  void chemistryTaskStoresMethodAndConvergenceCriteria() {
    final ConvergenceCriteria criteria =
        ConvergenceCriteria.of((double) 1.0E-10, (double) 1.0E-9, (int) 250);
    final ElectronicStructureMethod method =
        ElectronicStructureMethod.densityFunctional((String) "B3LYP");
    ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GEOMETRY_OPTIMIZATION,
            (BasisSet) BasisSet.of((BasisSetName) BasisSetName.of((String) "def2-SVP")),
            (ActiveSpace) ActiveSpace.of((int) 4, (int) 4),
            (ElectronicStructureMethod) method,
            (ConvergenceCriteria) criteria);
    Assertions.assertEquals(
        (Object) ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY,
        (Object) task.method().type());
    Assertions.assertEquals(
        (Object) ElectronicStructureSpinTreatment.UNSPECIFIED,
        (Object) task.method().spinTreatment());
    Assertions.assertEquals((Object) "B3LYP", (Object) task.method().name().value());
    Assertions.assertTrue(task.method().hasDensityFunctionalDefinition());
    Assertions.assertEquals(
        (Object) DensityFunctionalFamily.HYBRID_GGA,
        (Object) task.method().densityFunctionalDefinition().family());
    Assertions.assertEquals(
        (double) 0.20,
        (double) task.method().densityFunctionalDefinition().exactExchangeFraction(),
        (double) 1.0E-12);
    Assertions.assertEquals((Object) criteria, (Object) task.convergenceCriteria());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ConvergenceCriteria.of((double) 0.0, (double) 1.0E-9, (int) 100));
  }

  @Test
  void densityFunctionalDefinitionStoresStructuredApproximationData() {
    final DensityFunctionalDefinition pbe0 = DensityFunctionalDefinition.resolve("PBE0");
    final DensityFunctionalDefinition custom =
        DensityFunctionalDefinition.of(
            "Custom-Hybrid",
            DensityFunctionalFamily.HYBRID_META_GGA,
            0.37,
            0.08,
            true,
            true,
            "Custom hybrid meta-GGA with dispersion");
    final ElectronicStructureMethod method = ElectronicStructureMethod.densityFunctional(custom);

    Assertions.assertEquals((Object) DensityFunctionalFamily.HYBRID_GGA, (Object) pbe0.family());
    Assertions.assertEquals((double) 0.25, (double) pbe0.exactExchangeFraction(), (double) 1.0E-12);
    Assertions.assertEquals((Object) "Custom-Hybrid", (Object) method.name().value());
    Assertions.assertEquals((Object) custom, (Object) method.densityFunctionalDefinition());
    Assertions.assertTrue((boolean) custom.gradientCorrected());
    Assertions.assertTrue((boolean) custom.dispersionCorrected());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            DensityFunctionalDefinition.of(
                "Bad functional",
                DensityFunctionalFamily.HYBRID_GGA,
                1.2,
                0.0,
                true,
                false,
                "Invalid exact exchange"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            DensityFunctionalDefinition.of(
                "Bad double hybrid",
                DensityFunctionalFamily.DOUBLE_HYBRID,
                0.5,
                -0.1,
                true,
                false,
                "Invalid perturbative correlation"));
  }

  @Test
  void structuredBasisSetStoresHighAngularMomentumShellsAndCountsFunctions() {
    final BasisSetDefinition definition =
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("Fe"),
                    List.of(
                        BasisShell.of(
                            BasisShellAngularMomentum.S, List.of(BasisPrimitive.of(226.0, 0.15))),
                        BasisShell.of(
                            BasisShellAngularMomentum.P, List.of(BasisPrimitive.of(38.0, 0.22))),
                        BasisShell.of(
                            BasisShellAngularMomentum.D, List.of(BasisPrimitive.of(7.5, 0.41))),
                        BasisShell.of(
                            BasisShellAngularMomentum.F, List.of(BasisPrimitive.of(1.8, 0.32))),
                        BasisShell.of(
                            BasisShellAngularMomentum.G, List.of(BasisPrimitive.of(0.6, 0.18))),
                        BasisShell.of(
                            BasisShellAngularMomentum.H, List.of(BasisPrimitive.of(0.4, 0.12))),
                        BasisShell.of(
                            BasisShellAngularMomentum.I, List.of(BasisPrimitive.of(0.3, 0.08))),
                        BasisShell.of(
                            BasisShellAngularMomentum.K, List.of(BasisPrimitive.of(0.2, 0.05)))))));
    final BasisSet basisSet =
        BasisSet.structured(BasisSetName.of("custom-fe-s-p-d-f-g"), definition);

    Assertions.assertTrue(basisSet.hasStructuredDefinition());
    Assertions.assertEquals(1, basisSet.definition().atomDefinitionCount());
    Assertions.assertEquals(8, basisSet.definition().shellCount());
    Assertions.assertEquals(8, basisSet.definition().primitiveCount());
    Assertions.assertEquals(120, basisSet.definition().cartesianFunctionCount());
    Assertions.assertTrue(basisSet.definition().supports(ElementSymbol.of("Fe")));
  }

  @Test
  void structuredBasisSetRejectsDuplicateElementsEmptyShellsAndInvalidPrimitives() {
    final BasisAtomDefinition hydrogenBasis =
        BasisAtomDefinition.of(
            ElementSymbol.of("H"),
            List.of(
                BasisShell.of(
                    BasisShellAngularMomentum.S,
                    List.of(BasisPrimitive.of(3.42525091, 0.15432897)))));

    Assertions.assertThrows(IllegalArgumentException.class, () -> BasisPrimitive.of(0.0, 1.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> BasisShell.of(BasisShellAngularMomentum.S, List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> BasisSetDefinition.of(List.of(hydrogenBasis, hydrogenBasis)));
  }

  @Test
  void validatorChecksStructuredBasisCoverageForMoleculeAndReaction() {
    final BasisSet hydrogenOnlyBasis =
        BasisSet.structured(
            BasisSetName.of("hydrogen-only"),
            BasisSetDefinition.of(
                List.of(
                    BasisAtomDefinition.of(
                        ElementSymbol.of("H"),
                        List.of(
                            BasisShell.of(
                                BasisShellAngularMomentum.S,
                                List.of(BasisPrimitive.of(3.42525091, 0.15432897))))))));
    final ElectronicStructureMethod unrestrictedMethod =
        ElectronicStructureMethod.HARTREE_FOCK.withSpinTreatment(
            ElectronicStructureSpinTreatment.UNRESTRICTED);
    final ChemistryTask moleculeTask =
        ChemistryTask.of(
            ChemistryTaskType.GROUND_STATE_ENERGY,
            hydrogenOnlyBasis,
            ActiveSpace.of(1, 1),
            unrestrictedMethod,
            ConvergenceCriteria.DEFAULT);
    final ChemistryExperiment coveredExperiment =
        ChemistryExperiment.of(
            ChemistryExperimentId.of("experiment.structured_basis_h"),
            ChemistryCoreMethodTest.hydrogenAtom(),
            moleculeTask,
            ChemistryExecutionMode.CLASSICAL_AND_QUANTUM);
    final ChemistryExperiment missingElementExperiment =
        ChemistryExperiment.of(
            ChemistryExperimentId.of("experiment.structured_basis_missing_cl"),
            ChemistryCoreMethodTest.identityReaction(ChemistryCoreMethodTest.hydrogenChloride()),
            ChemistryTask.of(
                ChemistryTaskType.REACTION_ENERGY,
                hydrogenOnlyBasis,
                ActiveSpace.of(1, 1),
                unrestrictedMethod,
                ConvergenceCriteria.DEFAULT),
            ChemistryExecutionMode.CLASSICAL_AND_QUANTUM);

    final ChemistryValidationResult coveredResult =
        new ChemistryCoreValidator().validateExperiment(coveredExperiment);
    final ChemistryValidationResult missingElementResult =
        new ChemistryCoreValidator().validateExperiment(missingElementExperiment);

    Assertions.assertTrue(coveredResult.valid());
    Assertions.assertFalse(missingElementResult.valid());
    Assertions.assertTrue(
        ChemistryCoreMethodTest.contains(
            missingElementResult.diagnostics(),
            ChemistryDiagnosticCode.TASK_BASIS_SET_INCOMPATIBLE_WITH_SUBJECT,
            ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void defaultChemistryTaskUsesExplicitHartreeFockMethod() {
    ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 2, (int) 2));
    Assertions.assertEquals(
        (Object) ElectronicStructureMethod.HARTREE_FOCK, (Object) task.method());
    Assertions.assertEquals(
        (Object) ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL,
        (Object) task.method().spinTreatment());
    Assertions.assertEquals(
        (Object) ConvergenceCriteria.DEFAULT, (Object) task.convergenceCriteria());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryTask.of(
                (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
                (BasisSet) BasisSet.STO_3G,
                null,
                null,
                (ConvergenceCriteria) ConvergenceCriteria.DEFAULT));
  }

  @Test
  void validatorRejectsRestrictedClosedShellMethodForOpenShellMolecule() {
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "hydrogen.open_shell"),
            (String) "Hydrogen atom",
            List.of(ChemistryCoreMethodTest.atom("h", "H", 0.0)),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.of((int) 2));
    final ChemistryTask restrictedTask =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 1, (int) 1),
            (ElectronicStructureMethod) ElectronicStructureMethod.HARTREE_FOCK,
            (ConvergenceCriteria) ConvergenceCriteria.DEFAULT);
    final ChemistryExperiment restrictedExperiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId)
                ChemistryExperimentId.of((String) "experiment.open_shell_restricted"),
            (ChemistrySubject) molecule,
            (ChemistryTask) restrictedTask,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_ONLY);
    ElectronicStructureMethod unrestrictedMethod =
        ElectronicStructureMethod.HARTREE_FOCK.withSpinTreatment(
            ElectronicStructureSpinTreatment.UNRESTRICTED);
    final ChemistryTask unrestrictedTask =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 1, (int) 1),
            (ElectronicStructureMethod) unrestrictedMethod,
            (ConvergenceCriteria) ConvergenceCriteria.DEFAULT);
    final ChemistryExperiment unrestrictedExperiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId)
                ChemistryExperimentId.of((String) "experiment.open_shell_unrestricted"),
            (ChemistrySubject) molecule,
            (ChemistryTask) unrestrictedTask,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_ONLY);
    final ChemistryValidationResult restrictedResult =
        new ChemistryCoreValidator().validateExperiment(restrictedExperiment);
    final ChemistryValidationResult unrestrictedResult =
        new ChemistryCoreValidator().validateExperiment(unrestrictedExperiment);
    Assertions.assertFalse((boolean) restrictedResult.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreMethodTest.contains(
                restrictedResult.diagnostics(),
                ChemistryDiagnosticCode.METHOD_SPIN_TREATMENT_INCOMPATIBLE_WITH_SUBJECT,
                ChemistryDiagnosticSeverity.ERROR));
    Assertions.assertTrue((boolean) unrestrictedResult.valid());
  }

  @Test
  void validatorChecksSpinTreatmentForEachReactionParticipant() {
    Reaction reaction =
        ChemistryCoreMethodTest.identityReaction(ChemistryCoreMethodTest.hydrogenAtom());
    ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.REACTION_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 1, (int) 1),
            (ElectronicStructureMethod) ElectronicStructureMethod.HARTREE_FOCK,
            (ConvergenceCriteria) ConvergenceCriteria.DEFAULT);
    ChemistryExperiment experiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId)
                ChemistryExperimentId.of((String) "experiment.reaction_open_shell_restricted"),
            (ChemistrySubject) reaction,
            (ChemistryTask) task,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_ONLY);
    ChemistryValidationResult result = new ChemistryCoreValidator().validateExperiment(experiment);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreMethodTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.METHOD_SPIN_TREATMENT_INCOMPATIBLE_WITH_SUBJECT,
                ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void validatorChecksActiveSpaceAgainstEachReactionParticipant() {
    final Reaction reaction =
        ChemistryCoreMethodTest.identityReaction(ChemistryCoreMethodTest.hydrogenAtom());
    final ElectronicStructureMethod unrestrictedMethod =
        ElectronicStructureMethod.HARTREE_FOCK.withSpinTreatment(
            ElectronicStructureSpinTreatment.UNRESTRICTED);
    final ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.REACTION_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 2, (int) 1),
            (ElectronicStructureMethod) unrestrictedMethod,
            (ConvergenceCriteria) ConvergenceCriteria.DEFAULT);
    final ChemistryExperiment experiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId)
                ChemistryExperimentId.of((String) "experiment.reaction_invalid_active_space"),
            (ChemistrySubject) reaction,
            (ChemistryTask) task,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_AND_QUANTUM);
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateExperiment(experiment);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreMethodTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ACTIVE_SPACE_HAS_TOO_MANY_ELECTRONS,
                ChemistryDiagnosticSeverity.ERROR));
  }

  private static Atom atom(final String id, final String symbol, final double x) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) x, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static Molecule hydrogenAtom() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hydrogen.atom"),
        (String) "Hydrogen atom",
        List.of(ChemistryCoreMethodTest.atom("h", "H", 0.0)),
        List.of(),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.of((int) 2));
  }

  private static Molecule hydrogenChloride() {
    return Molecule.of(
        MoleculeId.of("hydrogen.chloride"),
        "Hydrogen chloride",
        List.of(
            ChemistryCoreMethodTest.atom("h", "H", 0.0),
            ChemistryCoreMethodTest.atom("cl", "Cl", 1.27)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Reaction identityReaction(final Molecule molecule) {
    final ReactionParticipant participant =
        ReactionParticipant.of(
            (Molecule) molecule, (StoichiometricCoefficient) StoichiometricCoefficient.ONE);
    return Reaction.of(
        (ReactionId) ReactionId.of((String) "reaction.identity_hydrogen_atom"),
        (String) "Identity hydrogen atom",
        (ReactionSide) ReactionSide.of(List.of(participant)),
        (ReactionSide) ReactionSide.of(List.of(participant)));
  }

  private static boolean contains(
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticCode code,
      final ChemistryDiagnosticSeverity severity) {
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i).code() != code || diagnostics.get(i).severity() != severity) continue;
      return true;
    }
    return false;
  }
}
