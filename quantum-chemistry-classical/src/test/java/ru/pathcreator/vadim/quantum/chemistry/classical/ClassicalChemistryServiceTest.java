/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalSubjectSize;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend.ClassicalBackendExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.PlanningOnlyClassicalBackend;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ClassicalChemistryServiceTest {

  @Test
  void supportsWaterEnergyOptimizationAndFrequencyPlan() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request = ClassicalChemistryServiceTest.waterRequest(
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
            ClassicalCalculationKind.FREQUENCY_ANALYSIS));

    final ClassicalPreflightResult preflight = service.preflight(
        request,
        BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationPlan plan = service.plan(
        request,
        BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalResourceEstimate estimate = plan.totalResourceEstimate();

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertEquals(3, plan.steps().size());
    Assertions.assertEquals(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION, plan.steps().get(1).kind());
    Assertions.assertTrue(plan.steps().get(1).requiredCapabilities().contains(
        ClassicalCapability.GEOMETRY_GRADIENT));
    Assertions.assertEquals(3L, estimate.atomCount());
    Assertions.assertEquals(2L, estimate.bondCount());
    Assertions.assertEquals(10L, estimate.electronCount());
    Assertions.assertTrue(estimate.estimatedMemoryBytes() > 0L);
  }

  @Test
  void rejectsUnsupportedBasisAndMethodWithStableDiagnostics() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request = ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("bad_method"),
        ClassicalChemistryServiceTest.water(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.COUPLED_CLUSTER,
            ElectronicStructureMethodName.of("CCSD")),
        BasisSet.of(BasisSetName.of("cc-pVQZ")),
        List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalPreflightResult preflight = service.preflight(
        request,
        BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertEquals(2, preflight.diagnostics().diagnostics().size());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
        preflight.diagnostics().diagnostics().get(0).code());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
        preflight.diagnostics().diagnostics().get(1).code());
  }

  @Test
  void requiresCompleteGeometryForGeometryTasks() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule molecule = Molecule.of(
        MoleculeId.of("bare_h2"),
        "Bare H2",
        List.of(
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                null),
            Atom.of(
                AtomId.of("h2"),
                ElementSymbol.of("H"),
                null)),
        List.of(
            Bond.of(
                AtomId.of("h1"),
                AtomId.of("h2"),
                BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
    final ClassicalCalculationRequest request = ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("geometry_without_coordinates"),
        molecule,
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));

    final ClassicalPreflightResult preflight = service.preflight(
        request,
        BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
        preflight.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void requiresActiveSpaceForHamiltonianPreparation() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request = ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("hamiltonian_without_active_space"),
        ClassicalChemistryServiceTest.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalPreflightResult preflight = service.preflight(
        request,
        BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.ACTIVE_SPACE_REQUIRED,
        preflight.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void supportsActiveSpaceHamiltonianPreparationWhenProfileAllowsIt() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request = ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("water_active_space"),
        ClassicalChemistryServiceTest.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        ActiveSpace.of(
            2,
            2),
        List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(
            ClassicalCalculationOption.bool(
                "freeze_core",
                true)),
        null);

    final ClassicalCalculationPlan plan = service.plan(
        request,
        BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, plan.preflightResult().status());
    Assertions.assertEquals(2L, plan.totalResourceEstimate().activeElectronCount());
    Assertions.assertEquals(2L, plan.totalResourceEstimate().activeOrbitalCount());
    Assertions.assertTrue(plan.steps().get(0).requiredCapabilities().contains(
        ClassicalCapability.ELECTRONIC_HAMILTONIAN));
  }

  @Test
  void analyzesReactionSizeAcrossReactantsAndProducts() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule hydrogen = ClassicalChemistryServiceTest.hydrogen();
    final Molecule water = ClassicalChemistryServiceTest.water();
    final Reaction reaction = Reaction.of(
        ReactionId.of("probe_reaction"),
        "Probe reaction",
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    hydrogen,
                    StoichiometricCoefficient.of(2)))),
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    water,
                    StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request = ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("reaction_energy"),
        reaction,
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalSubjectSize size = service.analyzeSubject(request);
    final ClassicalPreflightResult preflight = service.preflight(
        request,
        BuiltInClassicalBackendProfiles.universalPlanning());

    Assertions.assertEquals(7L, size.atomCount());
    Assertions.assertEquals(4L, size.bondCount());
    Assertions.assertEquals(14L, size.electronCount());
    Assertions.assertEquals(2L, size.participantCount());
    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
  }

  @Test
  void planningOnlyBackendReturnsPlannedResultWithoutPretendingToCompute() {
    final PlanningOnlyClassicalBackend backend = new PlanningOnlyClassicalBackend();
    final ClassicalCalculationRequest request = ClassicalChemistryServiceTest.waterRequest(
        List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationPlan plan = backend.plan(request);
    final ClassicalCalculationResult result = backend.execute(plan);

    Assertions.assertEquals(ClassicalCalculationStatus.PLANNED, result.status());
    Assertions.assertEquals(request.id(), result.calculationId());
    Assertions.assertEquals(ClassicalDiagnosticCode.PLAN_READY, result.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void requestCollectionsAreImmutableAndRejectDuplicates() {
    final ClassicalCalculationRequest request = ClassicalChemistryServiceTest.waterRequest(
        List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final Executable mutateRequestKinds = new Executable() {

      public final void execute() {
        request.calculationKinds().add(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS);
      }
    };
    final Executable createDuplicateKinds = new Executable() {

      public final void execute() {
        ClassicalChemistryServiceTest.waterRequest(
            List.of(
                ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                ClassicalCalculationKind.SINGLE_POINT_ENERGY));
      }
    };

    Assertions.assertThrows(
        UnsupportedOperationException.class,
        mutateRequestKinds);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        createDuplicateKinds);
  }

  @Test
  void profileCanRepresentNarrowDescriptorOnlyBackends() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile = ClassicalBackendProfile.of(
        "descriptor.only",
        "Descriptor Only",
        "1.0",
        ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
        List.of(ChemistrySubjectKind.MOLECULE),
        List.of(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS),
        List.of(ElectronicStructureMethodType.HARTREE_FOCK),
        List.of(),
        List.of(
            ClassicalCapability.MOLECULE_INPUT,
            ClassicalCapability.DESCRIPTOR_ANALYSIS),
        100L,
        200L,
        1000L,
        1L);
    final ClassicalCalculationRequest request = ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("descriptor_probe"),
        ClassicalChemistryServiceTest.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        List.of(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));

    final ClassicalPreflightResult preflight = service.preflight(request, profile);

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
  }

  private static ClassicalCalculationRequest waterRequest(
      final List<ClassicalCalculationKind> kinds
  ) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("water_calculation"),
        ClassicalChemistryServiceTest.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        kinds);
  }

  private static Molecule water() {
    return Molecule.of(
        MoleculeId.of("water"),
        "Water",
        List.of(
            Atom.of(
                AtomId.of("o"),
                ElementSymbol.of("O"),
                Coordinate3D.of(
                    0.0,
                    0.0,
                    0.0,
                    LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                Coordinate3D.of(
                    0.95,
                    0.0,
                    0.0,
                    LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h2"),
                ElementSymbol.of("H"),
                Coordinate3D.of(
                    -0.24,
                    0.92,
                    0.0,
                    LengthUnit.ANGSTROM))),
        List.of(
            Bond.of(
                AtomId.of("o"),
                AtomId.of("h1"),
                BondType.SINGLE),
            Bond.of(
                AtomId.of("o"),
                AtomId.of("h2"),
                BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        MoleculeId.of("hydrogen"),
        "Hydrogen",
        List.of(
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                Coordinate3D.of(
                    0.0,
                    0.0,
                    0.0,
                    LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h2"),
                ElementSymbol.of("H"),
                Coordinate3D.of(
                    0.74,
                    0.0,
                    0.0,
                    LengthUnit.ANGSTROM))),
        List.of(
            Bond.of(
                AtomId.of("h1"),
                AtomId.of("h2"),
                BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}