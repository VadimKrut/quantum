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
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ConvergenceCriteria;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonianSummary;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicProblemId;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicStructureProblem;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.MolecularOrbital;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.MolecularOrbitalBasis;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.MolecularOrbitalOccupation;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.MolecularOrbitalRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.SpatialOrbitalIndex;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.SpinOrbital;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.SpinProjection;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreElectronicProblemTest {

  ChemistryCoreElectronicProblemTest() {}

  @Test
  void spinOrbitalUsesStableAlphaBetaIndexing() {
    final SpinOrbital alpha = SpinOrbital.alpha((int) 3);
    final SpinOrbital beta = SpinOrbital.beta((int) 3);
    Assertions.assertEquals((Object) SpinProjection.ALPHA, (Object) alpha.spinProjection());
    Assertions.assertEquals((int) 6, (int) alpha.canonicalSpinOrbitalIndex());
    Assertions.assertEquals((int) 7, (int) beta.canonicalSpinOrbitalIndex());
    Assertions.assertThrows(IllegalArgumentException.class, () -> SpinOrbital.alpha((int) -1));
  }

  @Test
  void electronicHamiltonianStoresIntegralTensorsAndResourceShape() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            (ActiveSpace) activeSpace,
            (double) 0.7137539936876182,
            List.of(
                OneElectronIntegral.of((int) 0, (int) 0, (double) -1.252477495),
                OneElectronIntegral.of((int) 0, (int) 1, (double) -0.475934275)),
            List.of(
                TwoElectronIntegral.of((int) 0, (int) 0, (int) 0, (int) 0, (double) 0.674493166),
                TwoElectronIntegral.of((int) 0, (int) 1, (int) 0, (int) 1, (double) 0.181287518)));
    Assertions.assertEquals((Object) activeSpace, (Object) hamiltonian.activeSpace());
    Assertions.assertEquals((int) 4, (int) hamiltonian.spinOrbitalCount());
    Assertions.assertEquals((int) 2, (int) hamiltonian.electronCount());
    Assertions.assertEquals((int) 4, (int) hamiltonian.integralCount());
    Assertions.assertFalse((boolean) hamiltonian.emptyElectronicTerms());
  }

  @Test
  void electronicHamiltonianSummaryDescribesIntegralShapeAndEnergyScale() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            (ActiveSpace) activeSpace,
            (double) 0.7,
            List.of(
                OneElectronIntegral.of((int) 0, (int) 0, (double) -1.2),
                OneElectronIntegral.of((int) 0, (int) 1, (double) 0.3),
                OneElectronIntegral.of((int) 1, (int) 1, (double) 0.0)),
            List.of(
                TwoElectronIntegral.of((int) 0, (int) 0, (int) 0, (int) 0, (double) 0.5),
                TwoElectronIntegral.of((int) 0, (int) 0, (int) 1, (int) 1, (double) 0.2),
                TwoElectronIntegral.of((int) 0, (int) 1, (int) 1, (int) 0, (double) -0.1)));
    ElectronicHamiltonianSummary summary = hamiltonian.summary();
    Assertions.assertEquals((int) 2, (int) summary.spatialOrbitalCount());
    Assertions.assertEquals((int) 4, (int) summary.spinOrbitalCount());
    Assertions.assertEquals((int) 2, (int) summary.electronCount());
    Assertions.assertEquals((int) 3, (int) summary.oneElectronTermCount());
    Assertions.assertEquals((int) 3, (int) summary.twoElectronTermCount());
    Assertions.assertEquals((int) 6, (int) summary.totalElectronicTermCount());
    Assertions.assertEquals((int) 2, (int) summary.oneElectronDiagonalTermCount());
    Assertions.assertEquals((int) 1, (int) summary.oneElectronCouplingTermCount());
    Assertions.assertEquals((int) 1, (int) summary.twoElectronSamePairTermCount());
    Assertions.assertEquals((int) 1, (int) summary.twoElectronExchangeLikeTermCount());
    Assertions.assertEquals((int) 1, (int) summary.zeroIntegralTermCount());
    Assertions.assertEquals((int) 5, (int) summary.nonZeroIntegralTermCount());
    Assertions.assertEquals((int) 1, (int) summary.maxReferencedSpatialOrbitalIndex());
    Assertions.assertEquals((long) 20L, (long) summary.rawSpatialTensorSlotCount());
    Assertions.assertEquals((double) 1.5, (double) summary.oneElectronL1Norm(), (double) 1.0E-12);
    Assertions.assertEquals((double) 0.8, (double) summary.twoElectronL1Norm(), (double) 1.0E-12);
    Assertions.assertEquals((double) 2.3, (double) summary.electronicL1Norm(), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 3.0, (double) summary.totalAbsoluteEnergyScale(), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 1.2, (double) summary.maxAbsoluteOneElectronIntegral(), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.5, (double) summary.maxAbsoluteTwoElectronIntegral(), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.3, (double) summary.rawSpatialTensorDensity(), (double) 1.0E-12);
    Assertions.assertTrue((boolean) summary.hasNuclearRepulsion());
    Assertions.assertTrue((boolean) summary.hasElectronicTerms());
    Assertions.assertTrue((boolean) summary.hasOrbitalCouplings());
    Assertions.assertEquals(
        (Object) summary,
        (Object) ElectronicHamiltonianSummary.of((ElectronicHamiltonian) hamiltonian));
    Assertions.assertEquals((int) summary.hashCode(), (int) hamiltonian.summary().hashCode());
  }

  @Test
  void electronicHamiltonianSummaryHandlesNuclearOnlyHamiltonian() {
    ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            (ActiveSpace) ActiveSpace.of((int) 2, (int) 2),
            (double) 0.7137539936876182,
            List.of(),
            List.of());
    final ElectronicHamiltonianSummary summary = hamiltonian.summary();
    Assertions.assertEquals((int) 0, (int) summary.totalElectronicTermCount());
    Assertions.assertEquals((int) -1, (int) summary.maxReferencedSpatialOrbitalIndex());
    Assertions.assertEquals((double) 0.0, (double) summary.rawSpatialTensorDensity());
    Assertions.assertEquals((double) 0.0, (double) summary.electronicL1Norm());
    Assertions.assertEquals(
        (double) 0.7137539936876182, (double) summary.totalAbsoluteEnergyScale(), (double) 1.0E-12);
    Assertions.assertTrue((boolean) summary.hasNuclearRepulsion());
    Assertions.assertFalse((boolean) summary.hasElectronicTerms());
    Assertions.assertFalse((boolean) summary.hasOrbitalCouplings());
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ElectronicHamiltonianSummary.of(null));
  }

  @Test
  void electronicHamiltonianRejectsOutOfRangeAndSymmetryDuplicateIntegrals() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronicHamiltonian.of(
                (ActiveSpace) activeSpace,
                (double) 0.0,
                List.of(OneElectronIntegral.of((int) 0, (int) 2, (double) 1.0)),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronicHamiltonian.of(
                (ActiveSpace) activeSpace,
                (double) 0.0,
                List.of(
                    OneElectronIntegral.of((int) 0, (int) 1, (double) 1.0),
                    OneElectronIntegral.of((int) 1, (int) 0, (double) 1.0)),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronicHamiltonian.of(
                (ActiveSpace) activeSpace,
                (double) 0.0,
                List.of(),
                List.of(
                    TwoElectronIntegral.of((int) 0, (int) 1, (int) 0, (int) 1, (double) 0.2),
                    TwoElectronIntegral.of((int) 1, (int) 0, (int) 1, (int) 0, (double) 0.2))));
  }

  @Test
  void electronicIntegralSymmetryKeysPreservePhysicalSlotsWithoutBoxing() {
    final OneElectronIntegral oneElectron = OneElectronIntegral.of(0, 3, -0.25);
    final OneElectronIntegral reversedOneElectron = OneElectronIntegral.of(3, 0, -0.25);
    final TwoElectronIntegral twoElectron = TwoElectronIntegral.of(0, 3, 1, 2, 0.125);
    final TwoElectronIntegral reversedInsidePairs = TwoElectronIntegral.of(3, 0, 2, 1, 0.125);
    final TwoElectronIntegral swappedPairs = TwoElectronIntegral.of(1, 2, 0, 3, 0.125);
    final TwoElectronIntegral differentSlot = TwoElectronIntegral.of(0, 2, 1, 3, 0.125);
    Assertions.assertEquals(oneElectron.symmetrySlotKey(), reversedOneElectron.symmetrySlotKey());
    Assertions.assertEquals(twoElectron.symmetrySlotKey(), reversedInsidePairs.symmetrySlotKey());
    Assertions.assertEquals(twoElectron.symmetrySlotKey(), swappedPairs.symmetrySlotKey());
    Assertions.assertNotEquals(twoElectron.symmetrySlotKey(), differentSlot.symmetrySlotKey());
    Assertions.assertTrue(oneElectron.sameSymmetrySlot(reversedOneElectron));
    Assertions.assertTrue(twoElectron.sameSymmetrySlot(swappedPairs));
  }

  @Test
  void molecularOrbitalBasisStoresRolesOccupationAndSymmetryLabels() {
    MolecularOrbitalBasis basis = ChemistryCoreElectronicProblemTest.hydrogenOrbitalBasis();
    Assertions.assertEquals((int) 3, (int) basis.orbitalCount());
    Assertions.assertEquals((int) 2, (int) basis.activeOrbitalCount());
    Assertions.assertEquals((int) 1, (int) basis.virtualOrbitalCount());
    Assertions.assertEquals((int) 0, (int) basis.coreOrbitalCount());
    Assertions.assertEquals((double) 2.0, (double) basis.occupiedElectronCount());
    Assertions.assertEquals(
        (Object) "sigma_g",
        (Object) basis.orbitalAt(SpatialOrbitalIndex.of((int) 0)).symmetryLabel());
    Assertions.assertFalse((boolean) basis.hasFractionalOccupation());
  }

  @Test
  void molecularOrbitalBasisRejectsNonContiguousIndexesAndWrongActiveCount() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularOrbitalBasis.of(
                List.of(
                    MolecularOrbital.of(
                        (int) 0,
                        (double) -0.5,
                        (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                        (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE),
                    MolecularOrbital.of(
                        (int) 2,
                        (double) 0.3,
                        (MolecularOrbitalOccupation) MolecularOrbitalOccupation.EMPTY,
                        (MolecularOrbitalRole) MolecularOrbitalRole.VIRTUAL))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularOrbitalBasis.of(
                    List.of(
                        MolecularOrbital.of(
                            (int) 0,
                            (double) -0.5,
                            (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                            (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE)))
                .requireCompatibleWith(ActiveSpace.of((int) 2, (int) 2)));
  }

  @Test
  void molecularOrbitalBasisMapsFrozenCoreGlobalIndexesToLocalActiveSpace() {
    final MolecularOrbitalBasis basis =
        MolecularOrbitalBasis.of(
            List.of(
                MolecularOrbital.of(
                    (int) 0,
                    (double) -20.0,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                    (MolecularOrbitalRole) MolecularOrbitalRole.CORE),
                MolecularOrbital.of(
                    (int) 1,
                    (int) 0,
                    (double) -0.8,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                    (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE,
                    (String) "a1"),
                MolecularOrbital.of(
                    (int) 2,
                    (int) 1,
                    (double) 0.2,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.EMPTY,
                    (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE,
                    (String) "b1"),
                MolecularOrbital.of(
                    (int) 3,
                    (double) 1.4,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.EMPTY,
                    (MolecularOrbitalRole) MolecularOrbitalRole.VIRTUAL)));
    basis.requireCompatibleWith(ActiveSpace.of((int) 2, (int) 2));
    Assertions.assertEquals((int) 1, (int) basis.coreOrbitalCount());
    Assertions.assertEquals(
        (Object) SpatialOrbitalIndex.of((int) 1),
        (Object) basis.activeOrbitalAt(SpatialOrbitalIndex.of((int) 0)).index());
    Assertions.assertEquals(
        (Object) SpatialOrbitalIndex.of((int) 2),
        (Object) basis.activeOrbitalAt(SpatialOrbitalIndex.of((int) 1)).index());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularOrbitalBasis.of(
                    List.of(
                        MolecularOrbital.of(
                            (int) 0,
                            (int) 1,
                            (double) -0.8,
                            (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                            (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE,
                            (String) "a1"),
                        MolecularOrbital.of(
                            (int) 1,
                            (int) 1,
                            (double) 0.2,
                            (MolecularOrbitalOccupation) MolecularOrbitalOccupation.EMPTY,
                            (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE,
                            (String) "b1")))
                .requireCompatibleWith(ActiveSpace.of((int) 2, (int) 2)));
  }

  @Test
  void electronicStructureProblemRequiresTaskAndHamiltonianActiveSpaceMatch() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    final ActiveSpace otherActiveSpace = ActiveSpace.of((int) 2, (int) 3);
    ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) activeSpace);
    ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            (ActiveSpace) otherActiveSpace,
            (double) 0.0,
            List.of(OneElectronIntegral.of((int) 0, (int) 0, (double) -1.0)),
            List.of());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronicStructureProblem.of(
                (ElectronicProblemId) ElectronicProblemId.of((String) "h2.problem"),
                (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
                (ChemistryTask) task,
                (ElectronicHamiltonian) hamiltonian));
  }

  @Test
  void validatorAcceptsPreparedHydrogenElectronicProblem() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    ElectronicStructureProblem problem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.sto3g.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask)
                ChemistryTask.of(
                    (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
                    (BasisSet) BasisSet.STO_3G,
                    (ActiveSpace) activeSpace),
            (ElectronicHamiltonian)
                ElectronicHamiltonian.of(
                    (ActiveSpace) activeSpace,
                    (double) 0.7137539936876182,
                    List.of(OneElectronIntegral.of((int) 0, (int) 0, (double) -1.252477495)),
                    List.of(
                        TwoElectronIntegral.of(
                            (int) 0, (int) 0, (int) 0, (int) 0, (double) 0.674493166))),
            (MolecularOrbitalBasis) ChemistryCoreElectronicProblemTest.hydrogenOrbitalBasis());
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateElectronicStructureProblem(problem);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertEquals((int) 4, (int) problem.requiredQubitCount());
  }

  @Test
  void validatorReportsMissingAndFractionalOrbitalBasisMetadata() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            (ActiveSpace) activeSpace,
            (double) 0.7137539936876182,
            List.of(OneElectronIntegral.of((int) 0, (int) 0, (double) -1.252477495)),
            List.of());
    final ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) activeSpace);
    final ElectronicStructureProblem withoutBasis =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.no_basis.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask) task,
            (ElectronicHamiltonian) hamiltonian);
    final MolecularOrbitalBasis fractionalBasis =
        MolecularOrbitalBasis.of(
            List.of(
                MolecularOrbital.of(
                    (int) 0,
                    (double) -0.6,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.of((double) 1.5),
                    (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE),
                MolecularOrbital.of(
                    (int) 1,
                    (double) 0.2,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.of((double) 0.5),
                    (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE)));
    final ElectronicStructureProblem withFractionalBasis =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.fractional_basis.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask) task,
            (ElectronicHamiltonian) hamiltonian,
            (MolecularOrbitalBasis) fractionalBasis);
    final ChemistryValidationResult withoutBasisResult =
        new ChemistryCoreValidator().validateElectronicStructureProblem(withoutBasis);
    final ChemistryValidationResult fractionalBasisResult =
        new ChemistryCoreValidator().validateElectronicStructureProblem(withFractionalBasis);
    Assertions.assertTrue((boolean) withoutBasisResult.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicProblemTest.contains(
                withoutBasisResult.diagnostics(),
                ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_HAS_NO_ORBITAL_BASIS,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue((boolean) fractionalBasisResult.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicProblemTest.contains(
                fractionalBasisResult.diagnostics(),
                ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_HAS_FRACTIONAL_ORBITAL_OCCUPATION,
            ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void validatorReportsElectronicProblemTaskMethodAndOrbitalOccupationCrossChecks() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            (ActiveSpace) activeSpace,
            (double) 0.7137539936876182,
            List.of(OneElectronIntegral.of((int) 0, (int) 0, (double) -1.252477495)),
            List.of(
                TwoElectronIntegral.of(
                    (int) 0, (int) 0, (int) 0, (int) 0, (double) 0.674493166)));
    final ChemistryTask reactionTask =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.REACTION_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) activeSpace);
    final ElectronicStructureProblem reactionScopedProblem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.reaction_task.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask) reactionTask,
            (ElectronicHamiltonian) hamiltonian,
            (MolecularOrbitalBasis) ChemistryCoreElectronicProblemTest.hydrogenOrbitalBasis());
    final ChemistryValidationResult reactionTaskResult =
        new ChemistryCoreValidator().validateElectronicStructureProblem(reactionScopedProblem);
    ChemistryCoreElectronicProblemTest.assertDiagnostic(
        reactionTaskResult.diagnostics(),
        ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_TASK_REQUIRES_MOLECULE_SCOPE,
        ChemistryDiagnosticSeverity.ERROR,
        "ELECTRONIC_STRUCTURE_PROBLEM",
        "h2.reaction_task.problem");

    final ChemistryTask restrictedOpenShellTask =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 1, (int) 1),
            (ElectronicStructureMethod)
                ElectronicStructureMethod.of(
                    (ElectronicStructureMethodType) ElectronicStructureMethodType.HARTREE_FOCK,
                    (ElectronicStructureMethodName)
                        ElectronicStructureMethodName.of((String) "Restricted HF"),
                    (ElectronicStructureSpinTreatment)
                        ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL),
            (ConvergenceCriteria) ConvergenceCriteria.DEFAULT);
    final ElectronicStructureProblem openShellProblem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h.open_shell.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenAtom(),
            (ChemistryTask) restrictedOpenShellTask,
            (ElectronicHamiltonian)
                ElectronicHamiltonian.of(
                    (ActiveSpace) ActiveSpace.of((int) 1, (int) 1),
                    (double) 0.0,
                    List.of(OneElectronIntegral.of((int) 0, (int) 0, (double) -0.5)),
                    List.of()));
    final ChemistryValidationResult openShellResult =
        new ChemistryCoreValidator().validateElectronicStructureProblem(openShellProblem);
    ChemistryCoreElectronicProblemTest.assertDiagnostic(
        openShellResult.diagnostics(),
        ChemistryDiagnosticCode.METHOD_SPIN_TREATMENT_INCOMPATIBLE_WITH_SUBJECT,
        ChemistryDiagnosticSeverity.ERROR,
        "MOLECULE",
        "hydrogen.atom");

    final MolecularOrbitalBasis mismatchedBasis =
        MolecularOrbitalBasis.of(
            List.of(
                MolecularOrbital.of(
                    (int) 0,
                    (double) -0.6,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                    (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE),
                MolecularOrbital.of(
                    (int) 1,
                    (double) 0.2,
                    (MolecularOrbitalOccupation) MolecularOrbitalOccupation.SINGLE,
                    (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE)));
    final ElectronicStructureProblem occupationMismatchProblem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.occupation_mismatch.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask)
                ChemistryTask.of(
                    (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
                    (BasisSet) BasisSet.STO_3G,
                    (ActiveSpace) activeSpace),
            (ElectronicHamiltonian) hamiltonian,
            (MolecularOrbitalBasis) mismatchedBasis);
    final ChemistryValidationResult occupationMismatchResult =
        new ChemistryCoreValidator().validateElectronicStructureProblem(occupationMismatchProblem);
    ChemistryCoreElectronicProblemTest.assertDiagnostic(
        occupationMismatchResult.diagnostics(),
        ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_ORBITAL_OCCUPATION_MISMATCH,
        ChemistryDiagnosticSeverity.WARNING,
        "ELECTRONIC_STRUCTURE_PROBLEM",
        "h2.occupation_mismatch.problem");
  }

  @Test
  void validatorRejectsElectronicProblemWithoutElectronicTerms() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    final ElectronicStructureProblem problem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.empty.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask)
                ChemistryTask.of(
                    (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
                    (BasisSet) BasisSet.STO_3G,
                    (ActiveSpace) activeSpace),
            (ElectronicHamiltonian)
                ElectronicHamiltonian.of(
                    (ActiveSpace) activeSpace, (double) 0.7137539936876182, List.of(), List.of()));
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateElectronicStructureProblem(problem);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicProblemTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_HAS_NO_ELECTRONIC_TERMS,
                ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void validatorReportsHamiltonianShapeWarnings() {
    ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    final ElectronicStructureProblem sparseProblem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.shape.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask)
                ChemistryTask.of(
                    (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
                    (BasisSet) BasisSet.STO_3G,
                    (ActiveSpace) activeSpace),
            (ElectronicHamiltonian)
                ElectronicHamiltonian.of(
                    (ActiveSpace) activeSpace,
                    (double) 0.7,
                    List.of(
                        OneElectronIntegral.of((int) 0, (int) 0, (double) -1.0),
                        OneElectronIntegral.of((int) 1, (int) 1, (double) 0.0)),
                    List.of(
                        TwoElectronIntegral.of((int) 0, (int) 0, (int) 0, (int) 0, (double) 0.5))));
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateElectronicStructureProblem(sparseProblem);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicProblemTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ELECTRONIC_HAMILTONIAN_HAS_ZERO_INTEGRAL_TERMS,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicProblemTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ELECTRONIC_HAMILTONIAN_HAS_NO_ORBITAL_COUPLINGS,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void validatorReportsDenseHamiltonianWarning() {
    final ActiveSpace activeSpace = ActiveSpace.of((int) 1, (int) 1);
    final ElectronicStructureProblem denseProblem =
        ElectronicStructureProblem.of(
            (ElectronicProblemId) ElectronicProblemId.of((String) "h2.dense.problem"),
            (Molecule) ChemistryCoreElectronicProblemTest.hydrogenMolecule(),
            (ChemistryTask)
                ChemistryTask.of(
                    (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
                    (BasisSet) BasisSet.STO_3G,
                    (ActiveSpace) activeSpace),
            (ElectronicHamiltonian)
                ElectronicHamiltonian.of(
                    (ActiveSpace) activeSpace,
                    (double) 0.7,
                    List.of(OneElectronIntegral.of((int) 0, (int) 0, (double) -1.0)),
                    List.of(
                        TwoElectronIntegral.of((int) 0, (int) 0, (int) 0, (int) 0, (double) 0.5))));
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateElectronicStructureProblem(denseProblem);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicProblemTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ELECTRONIC_HAMILTONIAN_IS_DENSE_TENSOR,
                ChemistryDiagnosticSeverity.WARNING));
  }

  private static Molecule hydrogenMolecule() {
    final AtomId firstAtomId = AtomId.of((String) "h1");
    final AtomId secondAtomId = AtomId.of((String) "h2");
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hydrogen.molecule"),
        (String) "Hydrogen molecule",
        List.of(
            ChemistryCoreElectronicProblemTest.atom(firstAtomId, 0.0),
            ChemistryCoreElectronicProblemTest.atom(secondAtomId, 0.7414)),
        List.of(Bond.of((AtomId) firstAtomId, (AtomId) secondAtomId, (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenAtom() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hydrogen.atom"),
        (String) "Hydrogen atom",
        List.of(
            Atom.of(
                (AtomId) AtomId.of((String) "h"),
                (ElementSymbol) ElementSymbol.of((String) "H"),
                (Coordinate3D)
                    Coordinate3D.of(
                        (double) 0.0,
                        (double) 0.0,
                        (double) 0.0,
                        (LengthUnit) LengthUnit.ANGSTROM),
                (FormalCharge) FormalCharge.NEUTRAL,
                null,
                (RadicalState) RadicalState.of((int) 1),
                ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY)),
        List.of(),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.of((int) 2));
  }

  private static Atom atom(final AtomId id, final double x) {
    return Atom.of(
        (AtomId) id,
        (ElementSymbol) ElementSymbol.of((String) "H"),
        (Coordinate3D)
            Coordinate3D.of(
                (double) x, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static MolecularOrbitalBasis hydrogenOrbitalBasis() {
    return MolecularOrbitalBasis.of(
        List.of(
            MolecularOrbital.of(
                (int) 0,
                (double) -0.578202,
                (MolecularOrbitalOccupation) MolecularOrbitalOccupation.DOUBLE,
                (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE,
                (String) "sigma_g"),
            MolecularOrbital.of(
                (int) 1,
                (double) 0.671143,
                (MolecularOrbitalOccupation) MolecularOrbitalOccupation.EMPTY,
                (MolecularOrbitalRole) MolecularOrbitalRole.ACTIVE,
                (String) "sigma_u"),
            MolecularOrbital.of(
                (int) 2,
                (double) 1.25,
                (MolecularOrbitalOccupation) MolecularOrbitalOccupation.EMPTY,
                (MolecularOrbitalRole) MolecularOrbitalRole.VIRTUAL,
                (String) "sigma_g_star")));
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

  private static void assertDiagnostic(
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticCode code,
      final ChemistryDiagnosticSeverity severity,
      final String targetKind,
      final String targetId) {
    for (int i = 0; i < diagnostics.size(); ++i) {
      final ChemistryDiagnostic diagnostic = diagnostics.get(i);
      if (diagnostic.code() != code || diagnostic.severity() != severity) {
        continue;
      }
      Assertions.assertTrue((boolean) diagnostic.hasTarget());
      Assertions.assertEquals((Object) targetKind, (Object) diagnostic.target().kind());
      Assertions.assertEquals((Object) targetId, (Object) diagnostic.target().id());
      Assertions.assertFalse((boolean) diagnostic.message().isBlank());
      return;
    }
    Assertions.fail("Expected diagnostic " + code + " was not reported.");
  }
}