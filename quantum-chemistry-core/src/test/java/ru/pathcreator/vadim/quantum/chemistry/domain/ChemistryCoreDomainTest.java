/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperimentId;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormulaTerm;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySource;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySourceLocation;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotationDirection;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreDomainTest {

  ChemistryCoreDomainTest() {}

  @Test
  void elementSymbolIsCanonicalAndResolvedThroughPeriodicTable() {
    final ElementSymbol symbol = ElementSymbol.of((String) "cl");
    Assertions.assertEquals((Object) "Cl", (Object) symbol.value());
    Assertions.assertTrue((boolean) PeriodicTable.contains((ElementSymbol) symbol));
    Assertions.assertEquals(
        (int) 17, (int) PeriodicTable.require((ElementSymbol) symbol).atomicNumber());
    Assertions.assertEquals(
        (int) 118,
        (int)
            PeriodicTable.require((ElementSymbol) ElementSymbol.of((String) "Og")).atomicNumber());
  }

  @Test
  void elementSymbolRejectsUnknownOrInvalidValues() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ElementSymbol.of((String) "C1"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> PeriodicTable.require((ElementSymbol) ElementSymbol.of((String) "Uuq")));
  }

  @Test
  void atomKeepsElectronicStateIsotopeAndMetadata() {
    final Atom atom =
        Atom.of(
            (AtomId) AtomId.of((String) "n15"),
            (ElementSymbol) ElementSymbol.of((String) "N"),
            (Coordinate3D) ChemistryCoreDomainTest.coordinate(0.0),
            (FormalCharge) FormalCharge.of((int) 1),
            (Isotope) Isotope.of((int) 15),
            (RadicalState) RadicalState.of((int) 1),
            (ChemistryMetadata)
                ChemistryMetadata.of(
                    (ChemistrySource) ChemistrySource.of((String) "manual", (String) "unit test"),
                    null,
                    Map.of("role", "spin-label")));
    Assertions.assertEquals((int) 1, (int) atom.formalCharge().value());
    Assertions.assertTrue((boolean) atom.hasIsotope());
    Assertions.assertEquals((int) 15, (int) atom.isotope().massNumber());
    Assertions.assertTrue((boolean) atom.radicalState().radical());
    Assertions.assertEquals((Object) "spin-label", atom.metadata().attributes().get("role"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Atom.of(
                (AtomId) AtomId.of((String) "c5"),
                (ElementSymbol) ElementSymbol.of((String) "C"),
                (Coordinate3D) ChemistryCoreDomainTest.coordinate(0.0),
                (FormalCharge) FormalCharge.NEUTRAL,
                (Isotope) Isotope.of((int) 5),
                (RadicalState) RadicalState.CLOSED_SHELL,
                (ChemistryMetadata) ChemistryMetadata.EMPTY));
  }

  @Test
  void physicalValueObjectsRejectImpossibleValues() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> Temperature.of((double) -1.0, (TemperatureUnit) TemperatureUnit.KELVIN));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> Temperature.of((double) -274.0, (TemperatureUnit) TemperatureUnit.CELSIUS));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> Pressure.of((double) -1.0, (PressureUnit) PressureUnit.ATMOSPHERE));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> EnergyValue.of((double) Double.NaN, (EnergyUnit) EnergyUnit.HARTREE));
  }

  @Test
  void moleculeCopiesInputListsAndRejectsBrokenGraph() {
    Atom first = ChemistryCoreDomainTest.atom("h1", "H", 0.0);
    Atom second = ChemistryCoreDomainTest.atom("h2", "H", 0.74);
    final ArrayList<Atom> atoms = new ArrayList<Atom>();
    atoms.add(first);
    atoms.add(second);
    final ArrayList<Bond> bonds = new ArrayList<Bond>();
    bonds.add(Bond.of((AtomId) first.id(), (AtomId) second.id(), (BondType) BondType.SINGLE));
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "builtin.h2"),
            (String) "Hydrogen",
            atoms,
            bonds,
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    atoms.clear();
    bonds.clear();
    Assertions.assertEquals((int) 2, (int) molecule.atomCount());
    Assertions.assertEquals((int) 1, (int) molecule.bondCount());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> molecule.atoms().add(first));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "broken"),
                (String) "Broken",
                List.of(first),
                List.of(
                    Bond.of((AtomId) first.id(), (AtomId) second.id(), (BondType) BondType.SINGLE)),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET));
  }

  @Test
  void moleculeRejectsDuplicateAtomsAndDuplicateBonds() {
    Atom first = ChemistryCoreDomainTest.atom("h1", "H", 0.0);
    Atom second = ChemistryCoreDomainTest.atom("h2", "H", 0.74);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "duplicate_atoms"),
                (String) "Duplicate atoms",
                List.of(first, first),
                List.of(),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "duplicate_bonds"),
                (String) "Duplicate bonds",
                List.of(first, second),
                List.of(
                    Bond.of((AtomId) first.id(), (AtomId) second.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) second.id(), (AtomId) first.id(), (BondType) BondType.DOUBLE)),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET));
  }

  @Test
  void moleculeKeepsStereochemistryAndRejectsUnknownStereocenterAtoms() {
    Atom carbon = ChemistryCoreDomainTest.atom("c", "C", 0.0);
    Atom fluorine = ChemistryCoreDomainTest.atom("f", "F", 1.0);
    Atom chlorine = ChemistryCoreDomainTest.atom("cl", "Cl", -1.0);
    Atom bromine = ChemistryCoreDomainTest.atom("br", "Br", 2.0);
    Atom hydrogen = ChemistryCoreDomainTest.atom("h", "H", -2.0);
    Stereochemistry stereochemistry =
        Stereochemistry.of(
            List.of(
                Stereocenter.ofTetrahedralAtom(
                    (AtomId) carbon.id(),
                    (StereochemicalDescriptor) StereochemicalDescriptor.R,
                    (AtomId) fluorine.id(),
                    (AtomId) chlorine.id(),
                    (AtomId) bromine.id(),
                    (AtomId) hydrogen.id())));
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "stereo.probe"),
            (String) "Stereo probe",
            List.of(carbon, fluorine, chlorine, bromine, hydrogen),
            List.of(
                Bond.of((AtomId) carbon.id(), (AtomId) fluorine.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) carbon.id(), (AtomId) chlorine.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) carbon.id(), (AtomId) bromine.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) carbon.id(), (AtomId) hydrogen.id(), (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry) stereochemistry,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals((Object) stereochemistry, (Object) molecule.stereochemistry());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "bad.stereo"),
                (String) "Bad stereo",
                List.of(carbon),
                List.of(),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET,
                (Stereochemistry)
                    Stereochemistry.of(
                        List.of(
                            Stereocenter.ofTetrahedralAtom(
                                (AtomId) carbon.id(),
                                (StereochemicalDescriptor) StereochemicalDescriptor.S,
                                (AtomId) AtomId.of((String) "missing"),
                                (AtomId) fluorine.id(),
                                (AtomId) chlorine.id(),
                                (AtomId) hydrogen.id()))),
                (ChemistryMetadata) ChemistryMetadata.EMPTY));
  }

  @Test
  void moleculeKeepsConfigurationalStereoAndOpticalRotationIndependent() {
    final Atom carbon = ChemistryCoreDomainTest.atom("c", "C", 0.0);
    final Atom fluorine = ChemistryCoreDomainTest.atom("f", "F", 1.0);
    final Atom chlorine = ChemistryCoreDomainTest.atom("cl", "Cl", -1.0);
    final Atom bromine = ChemistryCoreDomainTest.atom("br", "Br", 2.0);
    final Atom hydrogen = ChemistryCoreDomainTest.atom("h", "H", -2.0);
    Stereochemistry stereochemistry =
        Stereochemistry.of(
            List.of(
                Stereocenter.ofTetrahedralAtom(
                    (AtomId) carbon.id(),
                    (StereochemicalDescriptor) StereochemicalDescriptor.R,
                    (AtomId) fluorine.id(),
                    (AtomId) chlorine.id(),
                    (AtomId) bromine.id(),
                    (AtomId) hydrogen.id())));
    final OpticalRotation opticalRotation =
        OpticalRotation.of(
            (OpticalRotationDirection) OpticalRotationDirection.LEVOROTATORY,
            java.lang.Double.valueOf(-13.5),
            (Temperature) Temperature.of((double) 20.0, (TemperatureUnit) TemperatureUnit.CELSIUS),
            java.lang.Double.valueOf(589.0));
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "stereo.rotation_probe"),
            (String) "Stereo rotation probe",
            List.of(carbon, fluorine, chlorine, bromine, hydrogen),
            List.of(
                Bond.of((AtomId) carbon.id(), (AtomId) fluorine.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) carbon.id(), (AtomId) chlorine.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) carbon.id(), (AtomId) bromine.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) carbon.id(), (AtomId) hydrogen.id(), (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry) stereochemistry,
            (MolecularConformation) MolecularConformation.EMPTY,
            (OpticalRotation) opticalRotation,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals(
        (Object) StereochemicalDescriptor.R,
        (Object) ((Stereocenter) molecule.stereochemistry().centers().get(0)).descriptor());
    Assertions.assertEquals(
        (Object) OpticalRotationDirection.LEVOROTATORY,
        (Object) molecule.opticalRotation().direction());
    Assertions.assertEquals((double) -13.5, molecule.opticalRotation().degrees());
  }

  @Test
  void validatorReportsMoleculeGraphStereoConformationAndRotationContext() {
    final Molecule disconnected =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "disconnected.probe"),
            (String) "Disconnected probe",
            List.of(
                ChemistryCoreDomainTest.atom("h1", "H", 0.0),
                ChemistryCoreDomainTest.atom("h2", "H", 1.0),
                ChemistryCoreDomainTest.atom("cl", "Cl", 2.0)),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "h1"),
                    (AtomId) AtomId.of((String) "h2"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final ChemistryValidationResult disconnectedResult =
        new ChemistryCoreValidator().validateMolecule(disconnected);
    ChemistryCoreDomainTest.assertDiagnostic(
        disconnectedResult.diagnostics(),
        ChemistryDiagnosticCode.MOLECULE_GRAPH_IS_DISCONNECTED,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULE",
        "disconnected.probe");

    final Molecule incompleteStereoConformation =
        ChemistryCoreDomainTest.incompleteStereoConformationMolecule();
    final ChemistryValidationResult contextResult =
        new ChemistryCoreValidator().validateMolecule(incompleteStereoConformation);
    Assertions.assertTrue((boolean) contextResult.valid());
    ChemistryCoreDomainTest.assertDiagnostic(
        contextResult.diagnostics(),
        ChemistryDiagnosticCode.MOLECULE_STEREOCHEMISTRY_NEEDS_GEOMETRY,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULE",
        "stereo.conformation.no_geometry");
    ChemistryCoreDomainTest.assertDiagnostic(
        contextResult.diagnostics(),
        ChemistryDiagnosticCode.MOLECULE_CONFORMATION_NEEDS_GEOMETRY,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULE",
        "stereo.conformation.no_geometry");

    final Molecule rotationWithoutStereo =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "rotation.no_stereo"),
            (String) "Rotation without stereo",
            List.of(
                ChemistryCoreDomainTest.atom("c", "C", 0.0),
                ChemistryCoreDomainTest.atom("h", "H", 1.0)),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry) Stereochemistry.EMPTY,
            (MolecularConformation) MolecularConformation.EMPTY,
            (OpticalRotation)
                OpticalRotation.of(
                    (OpticalRotationDirection) OpticalRotationDirection.DEXTROROTATORY,
                    java.lang.Double.valueOf(4.2),
                    null,
                    java.lang.Double.valueOf(589.0)),
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    final ChemistryValidationResult rotationResult =
        new ChemistryCoreValidator().validateMolecule(rotationWithoutStereo);
    ChemistryCoreDomainTest.assertDiagnostic(
        rotationResult.diagnostics(),
        ChemistryDiagnosticCode.MOLECULE_OPTICAL_ROTATION_WITHOUT_STEREOCHEMISTRY,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULE",
        "rotation.no_stereo");
  }

  @Test
  void validatorChecksFormalChargeAndRadicalSpinConsistency() {
    final Atom nitrogen =
        Atom.of(
            (AtomId) AtomId.of((String) "n"),
            (ElementSymbol) ElementSymbol.of((String) "N"),
            (Coordinate3D) ChemistryCoreDomainTest.coordinate(0.0),
            (FormalCharge) FormalCharge.of((int) 1),
            null,
            (RadicalState) RadicalState.of((int) 1),
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "charged.radical"),
            (String) "Charged radical",
            List.of(nitrogen),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    ChemistryValidationResult result = new ChemistryCoreValidator().validateMolecule(molecule);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreDomainTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_CHARGE_DOES_NOT_MATCH_ATOMS,
                ChemistryDiagnosticSeverity.ERROR));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreDomainTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_RADICAL_SPIN_NEEDS_REVIEW,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void moleculeExposesElectronicConfigurationAndValidatorChecksSpinParity() {
    final Molecule hydrogenCation =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "hydrogen.cation"),
            (String) "Hydrogen cation",
            List.of(ChemistryCoreDomainTest.atom("h", "H", 0.0)),
            List.of(),
            (MolecularCharge) MolecularCharge.of((int) 1),
            (SpinMultiplicity) SpinMultiplicity.of((int) 2));
    final Molecule incompatibleMethane =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "methane.bad_spin"),
            (String) "Methane bad spin",
            List.of(
                ChemistryCoreDomainTest.atom("c", "C", 0.0),
                ChemistryCoreDomainTest.atom("h1", "H", 1.0),
                ChemistryCoreDomainTest.atom("h2", "H", -1.0),
                ChemistryCoreDomainTest.atom("h3", "H", 2.0),
                ChemistryCoreDomainTest.atom("h4", "H", -2.0)),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h1"),
                    (BondType) BondType.SINGLE),
                Bond.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h2"),
                    (BondType) BondType.SINGLE),
                Bond.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h3"),
                    (BondType) BondType.SINGLE),
                Bond.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h4"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.of((int) 2));
    Assertions.assertEquals(
        (int) 1, (int) hydrogenCation.electronicConfiguration().nuclearCharge());
    Assertions.assertEquals(
        (int) 0, (int) hydrogenCation.electronicConfiguration().electronCount());
    Assertions.assertFalse(
        (boolean) hydrogenCation.electronicConfiguration().spinMultiplicityPossible());
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecule(incompatibleMethane);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreDomainTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_SPIN_MULTIPLICITY_INCOMPATIBLE_WITH_ELECTRONS,
                ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void activeSpaceRejectsPhysicallyImpossibleElectronCount() {
    final ActiveSpace activeSpace = ActiveSpace.of((int) 2, (int) 2);
    Assertions.assertEquals((int) 4, (int) activeSpace.spinOrbitalCount());
    Assertions.assertThrows(IllegalArgumentException.class, () -> ActiveSpace.of((int) 5, (int) 2));
  }

  @Test
  void validatorRejectsActiveSpaceWithMoreElectronsThanMoleculeHas() {
    ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 3, (int) 3));
    ChemistryExperiment experiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId)
                ChemistryExperimentId.of((String) "experiment.invalid_active_space"),
            (ChemistrySubject) ChemistryCoreDomainTest.h2(),
            (ChemistryTask) task,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_AND_QUANTUM);
    ChemistryValidationResult result = new ChemistryCoreValidator().validateExperiment(experiment);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreDomainTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ACTIVE_SPACE_HAS_TOO_MANY_ELECTRONS,
                ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void validatorRequiresActiveSpaceForQuantumRoute() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "single_h"),
            (String) "Hydrogen atom",
            List.of(ChemistryCoreDomainTest.atom("h1", "H", 0.0)),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.of((int) 2));
    ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            null);
    ChemistryExperiment experiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId) ChemistryExperimentId.of((String) "experiment.h"),
            (ChemistrySubject) molecule,
            (ChemistryTask) task,
            (ChemistryExecutionMode) ChemistryExecutionMode.QUANTUM_PROGRAM_ONLY);
    final ChemistryCoreValidator validator = new ChemistryCoreValidator();
    ChemistryValidationResult result = validator.validateExperiment(experiment);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreDomainTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.QUANTUM_ROUTE_REQUIRES_ACTIVE_SPACE,
                ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void validatorAcceptsCompleteQuantumExperimentWithWarningsAbsent() {
    final Molecule molecule = ChemistryCoreDomainTest.h2();
    final ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.GROUND_STATE_ENERGY,
            (BasisSet) BasisSet.of((BasisSetName) BasisSetName.of((String) "STO-3G")),
            (ActiveSpace) ActiveSpace.of((int) 2, (int) 2));
    final ChemistryExperiment experiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId) ChemistryExperimentId.of((String) "experiment.h2"),
            (ChemistrySubject) molecule,
            (ChemistryTask) task,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_AND_QUANTUM);
    final ChemistryValidationResult result = new ChemistryCoreValidator().validateExperiment(experiment);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue((boolean) result.diagnostics().isEmpty());
    Assertions.assertNotNull((Object) experiment.task().activeSpace());
  }

  @Test
  void molecularFormulaUsesHillNotationAndImmutableTerms() {
    final Molecule water = ChemistryCoreDomainTest.water();
    final MolecularFormula formula = water.formula();
    Assertions.assertEquals((Object) "H2O", (Object) formula.hillNotation());
    Assertions.assertEquals((int) 2, (int) formula.countOf(ElementSymbol.of((String) "H")));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> formula.terms().clear());
    Assertions.assertEquals(
        (Object) "ClH",
        (Object)
            MolecularFormula.of(
                    List.of(
                        MolecularFormulaTerm.of(
                            (ElementSymbol) ElementSymbol.of((String) "H"), (int) 1),
                        MolecularFormulaTerm.of(
                            (ElementSymbol) ElementSymbol.of((String) "Cl"), (int) 1)))
                .hillNotation());
  }

  @Test
  void metadataCopiesAttributesAndRejectsBlankValues() {
    final HashMap<String, String> attributes = new HashMap<String, String>();
    attributes.put("author", "test");
    final ChemistryMetadata metadata =
        ChemistryMetadata.of(
            (ChemistrySource) ChemistrySource.of((String) "qmol", (String) "unit test"),
            (ChemistrySourceLocation) ChemistrySourceLocation.of((int) 3, (int) 5),
            attributes);
    attributes.clear();
    Assertions.assertEquals((Object) "test", metadata.attributes().get("author"));
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> metadata.attributes().put("x", "y"));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ChemistryMetadata.of(null, null, Map.of(" ", "bad")));
  }

  @Test
  void textValueNormalizesRequiredAndOptionalDomainText() {
    Assertions.assertEquals(
        (Object) "domain text",
        (Object) TextValue.requireText((String) "  domain text  ", (String) "Test text"));
    Assertions.assertEquals(
        (Object) "optional", (Object) TextValue.optionalText((String) " optional "));
    Assertions.assertNull((Object) TextValue.optionalText(null));
    Assertions.assertNull((Object) TextValue.optionalText((String) "   "));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> TextValue.requireText(null, (String) "Test text"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> TextValue.requireText((String) " ", (String) "Test text"));
  }

  @Test
  void chemistryHashAndDiagnosticTargetKeepStableValueSemantics() {
    int first = ChemistryHash.seed();
    first = ChemistryHash.include(first, (Object) "kind");
    first = ChemistryHash.include(first, (long) 17L);
    first = ChemistryHash.include(first, (double) 1.25);
    first = ChemistryHash.include(first, true);
    int second = ChemistryHash.seed();
    second = ChemistryHash.include(second, (Object) "kind");
    second = ChemistryHash.include(second, (long) 17L);
    second = ChemistryHash.include(second, (double) 1.25);
    second = ChemistryHash.include(second, true);
    final ChemistryDiagnosticTarget target =
        ChemistryDiagnosticTarget.of((String) "  molecule  ", (String) " h2 ");
    Assertions.assertEquals((int) first, (int) second);
    Assertions.assertEquals((Object) "molecule", (Object) target.kind());
    Assertions.assertEquals((Object) "h2", (Object) target.id());
    Assertions.assertEquals(
        (Object) target,
        (Object) ChemistryDiagnosticTarget.of((String) "molecule", (String) "h2"));
    Assertions.assertEquals(
        (int) target.hashCode(),
        (int) ChemistryDiagnosticTarget.of((String) "molecule", (String) "h2").hashCode());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ChemistryDiagnosticTarget.of((String) " ", (String) "h2"));
  }

  private static Atom atom(final String id, final String symbol, final double x) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) ChemistryCoreDomainTest.coordinate(x));
  }

  private static Atom atomWithoutCoordinate(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        null);
  }

  private static Molecule incompleteStereoConformationMolecule() {
    final Stereochemistry stereochemistry =
        Stereochemistry.of(
            List.of(
                Stereocenter.ofTetrahedralAtom(
                    (AtomId) AtomId.of((String) "c"),
                    (StereochemicalDescriptor) StereochemicalDescriptor.R,
                    (AtomId) AtomId.of((String) "f"),
                    (AtomId) AtomId.of((String) "cl"),
                    (AtomId) AtomId.of((String) "br"),
                    (AtomId) AtomId.of((String) "h"))));
    final MolecularConformation conformation =
        MolecularConformation.of(
            List.of(
                TorsionAngle.of(
                    (AtomId) AtomId.of((String) "f"),
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h"),
                    (AtomId) AtomId.of((String) "x"),
                    (double) 60.0)));
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "stereo.conformation.no_geometry"),
        (String) "Stereo conformation without complete geometry",
        List.of(
            ChemistryCoreDomainTest.atomWithoutCoordinate("c", "C"),
            ChemistryCoreDomainTest.atomWithoutCoordinate("f", "F"),
            ChemistryCoreDomainTest.atomWithoutCoordinate("cl", "Cl"),
            ChemistryCoreDomainTest.atomWithoutCoordinate("br", "Br"),
            ChemistryCoreDomainTest.atomWithoutCoordinate("h", "H"),
            ChemistryCoreDomainTest.atomWithoutCoordinate("x", "O")),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "f"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "cl"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "br"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "h"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "h"),
                (AtomId) AtomId.of((String) "x"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET,
        (Stereochemistry) stereochemistry,
        (MolecularConformation) conformation,
        (ChemistryMetadata) ChemistryMetadata.EMPTY);
  }

  private static Coordinate3D coordinate(final double x) {
    return Coordinate3D.of(
        (double) x, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM);
  }

  private static Molecule h2() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "h2"),
        (String) "Hydrogen",
        List.of(
            ChemistryCoreDomainTest.atom("h1", "H", 0.0),
            ChemistryCoreDomainTest.atom("h2", "H", 0.74)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "h1"),
                (AtomId) AtomId.of((String) "h2"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule water() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "water"),
        (String) "Water",
        List.of(
            ChemistryCoreDomainTest.atom("o", "O", 0.0),
            ChemistryCoreDomainTest.atom("h1", "H", 0.95),
            ChemistryCoreDomainTest.atom("h2", "H", -0.95)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "o"),
                (AtomId) AtomId.of((String) "h1"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "o"),
                (AtomId) AtomId.of((String) "h2"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
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