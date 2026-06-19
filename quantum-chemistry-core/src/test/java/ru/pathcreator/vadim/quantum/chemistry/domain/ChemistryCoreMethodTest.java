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
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.CombinatorialCount;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ConvergenceCriteria;
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
        IllegalArgumentException.class,
        () -> ElectronicStructureMethodName.of((String) "   "));
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
    Assertions.assertEquals((Object) criteria, (Object) task.convergenceCriteria());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ConvergenceCriteria.of((double) 0.0, (double) 1.0E-9, (int) 100));
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
    final ChemistryValidationResult result = new ChemistryCoreValidator().validateExperiment(experiment);
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