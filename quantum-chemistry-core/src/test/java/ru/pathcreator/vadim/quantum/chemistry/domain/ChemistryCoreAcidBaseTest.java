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
import ru.pathcreator.vadim.quantum.chemistry.domain.acidbase.AcidBaseModel;
import ru.pathcreator.vadim.quantum.chemistry.domain.acidbase.AcidBaseSite;
import ru.pathcreator.vadim.quantum.chemistry.domain.acidbase.AcidBaseSiteKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.acidbase.PKaValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.acidbase.ProtonationTransition;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.PHValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.SolutionEnvironment;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostate;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreAcidBaseTest {

  ChemistryCoreAcidBaseTest() {}

  @Test
  void protonationTransitionComputesHendersonHasselbalchFractions() {
    final ProtonationTransition transition = ChemistryCoreAcidBaseTest.transition();
    Assertions.assertEquals(
        (double) 1.0,
        (double) transition.deprotonatedToProtonatedRatio(PHValue.of((double) 4.76)),
        (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.5,
        (double) transition.deprotonatedFraction(PHValue.of((double) 4.76)),
        (double) 1.0E-12);
    Assertions.assertTrue(
        (transition.deprotonatedFraction(PHValue.of((double) 7.0)) > 0.99 ? 1 : 0) != 0);
    Assertions.assertTrue(
        (transition.protonatedFraction(PHValue.of((double) 2.0)) > 0.99 ? 1 : 0) != 0);
  }

  @Test
  void acidBaseModelAcceptsPhysicallyConsistentDeprotonationTransition() {
    AcidBaseModel model =
        AcidBaseModel.of(
            (String) "acetic.acid.model",
            (MolecularMicrostateSet) ChemistryCoreAcidBaseTest.microstates(),
            List.of(ChemistryCoreAcidBaseTest.transition()));
    Assertions.assertEquals((Object) "acetic.acid.model", (Object) model.id());
    Assertions.assertEquals((int) 1, (int) model.transitions().size());
  }

  @Test
  void validatorWarnsWhenAcidBaseModelHasNoPhContext() {
    final MolecularMicrostateSet microstatesWithoutEnvironment =
        MolecularMicrostateSet.of(
            (String) "acetic.acid.microstates.no_environment",
            List.of(
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                    (Molecule) ChemistryCoreAcidBaseTest.aceticAcid("acid"),
                    (String) "acid"),
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                    (Molecule) ChemistryCoreAcidBaseTest.acetate("acetate"),
                    (String) "acetate")));
    final AcidBaseModel model =
        AcidBaseModel.of(
            (String) "acetic.acid.no_ph",
            (MolecularMicrostateSet) microstatesWithoutEnvironment,
            List.of(ChemistryCoreAcidBaseTest.transition()));
    final ChemistryValidationResult result = new ChemistryCoreValidator().validateAcidBaseModel(model);
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreAcidBaseTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.ACID_BASE_MODEL_HAS_NO_PH_CONTEXT,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void acidBaseModelRejectsUnknownStateLabelsAndDuplicateTransitions() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            AcidBaseModel.of(
                (String) "unknown.label",
                (MolecularMicrostateSet) ChemistryCoreAcidBaseTest.microstates(),
                List.of(
                    ProtonationTransition.of(
                        (AcidBaseSite) ChemistryCoreAcidBaseTest.site(),
                        (String) "missing",
                        (String) "acetate",
                        (PKaValue) PKaValue.of((double) 4.76)))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            AcidBaseModel.of(
                (String) "duplicate",
                (MolecularMicrostateSet) ChemistryCoreAcidBaseTest.microstates(),
                List.of(
                    ChemistryCoreAcidBaseTest.transition(),
                    ChemistryCoreAcidBaseTest.transition())));
  }

  @Test
  void acidBaseModelRejectsMissingSiteAtom() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            AcidBaseModel.of(
                (String) "missing.site",
                (MolecularMicrostateSet) ChemistryCoreAcidBaseTest.microstates(),
                List.of(
                    ProtonationTransition.of(
                        (AcidBaseSite)
                            AcidBaseSite.of(
                                (String) "missing oxygen",
                                (AtomId) AtomId.of((String) "missing"),
                                (AcidBaseSiteKind) AcidBaseSiteKind.ACIDIC),
                        (String) "acid",
                        (String) "acetate",
                        (PKaValue) PKaValue.of((double) 4.76)))));
  }

  @Test
  void acidBaseModelRejectsReversedProtonatedAndDeprotonatedLabels() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            AcidBaseModel.of(
                (String) "reversed.model",
                (MolecularMicrostateSet) ChemistryCoreAcidBaseTest.microstates(),
                List.of(
                    ProtonationTransition.of(
                        (AcidBaseSite) ChemistryCoreAcidBaseTest.site(),
                        (String) "acetate",
                        (String) "acid",
                        (PKaValue) PKaValue.of((double) 4.76)))));
  }

  @Test
  void pkaRejectsNonFiniteOrUnsupportedValues() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> PKaValue.of((double) Double.NaN));
    Assertions.assertThrows(IllegalArgumentException.class, () -> PKaValue.of((double) 101.0));
  }

  private static MolecularMicrostateSet microstates() {
    return MolecularMicrostateSet.of(
        (String) "acetic.acid.microstates",
        List.of(
            MolecularMicrostate.of(
                (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                (Molecule) ChemistryCoreAcidBaseTest.aceticAcid("acid"),
                (String) "acid"),
            MolecularMicrostate.of(
                (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                (Molecule) ChemistryCoreAcidBaseTest.acetate("acetate"),
                (String) "acetate")),
        (SolutionEnvironment) SolutionEnvironment.aqueous((PHValue) PHValue.of((double) 4.76)));
  }

  private static ProtonationTransition transition() {
    return ProtonationTransition.of(
        (AcidBaseSite) ChemistryCoreAcidBaseTest.site(),
        (String) "acid",
        (String) "acetate",
        (PKaValue) PKaValue.of((double) 4.76));
  }

  private static AcidBaseSite site() {
    return AcidBaseSite.of(
        (String) "carboxyl oxygen",
        (AtomId) AtomId.of((String) "o2"),
        (AcidBaseSiteKind) AcidBaseSiteKind.ACIDIC);
  }

  private static Molecule aceticAcid(final String id) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreAcidBaseTest.atom("c1", "C"),
            ChemistryCoreAcidBaseTest.atom("c2", "C"),
            ChemistryCoreAcidBaseTest.atom("o1", "O"),
            ChemistryCoreAcidBaseTest.atom("o2", "O"),
            ChemistryCoreAcidBaseTest.atom("h1", "H"),
            ChemistryCoreAcidBaseTest.atom("h2", "H"),
            ChemistryCoreAcidBaseTest.atom("h3", "H"),
            ChemistryCoreAcidBaseTest.atom("h4", "H")),
        List.of(
            ChemistryCoreAcidBaseTest.bond("c1", "c2", BondType.SINGLE),
            ChemistryCoreAcidBaseTest.bond("c2", "o1", BondType.DOUBLE),
            ChemistryCoreAcidBaseTest.bond("c2", "o2", BondType.SINGLE),
            ChemistryCoreAcidBaseTest.bond("o2", "h4", BondType.SINGLE),
            ChemistryCoreAcidBaseTest.bond("c1", "h1", BondType.SINGLE),
            ChemistryCoreAcidBaseTest.bond("c1", "h2", BondType.SINGLE),
            ChemistryCoreAcidBaseTest.bond("c1", "h3", BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule acetate(final String id) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        ChemistryCoreAcidBaseTest.acetateAtoms(),
        ChemistryCoreAcidBaseTest.acetateBonds(),
        (MolecularCharge) MolecularCharge.of((int) -1),
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static List<Atom> acetateAtoms() {
    return List.of(
        ChemistryCoreAcidBaseTest.atom("c1", "C"),
        ChemistryCoreAcidBaseTest.atom("c2", "C"),
        ChemistryCoreAcidBaseTest.atom("o1", "O"),
        ChemistryCoreAcidBaseTest.atom("o2", "O"),
        ChemistryCoreAcidBaseTest.atom("h1", "H"),
        ChemistryCoreAcidBaseTest.atom("h2", "H"),
        ChemistryCoreAcidBaseTest.atom("h3", "H"));
  }

  private static List<Bond> acetateBonds() {
    return List.of(
        ChemistryCoreAcidBaseTest.bond("c1", "c2", BondType.SINGLE),
        ChemistryCoreAcidBaseTest.bond("c2", "o1", BondType.DOUBLE),
        ChemistryCoreAcidBaseTest.bond("c2", "o2", BondType.SINGLE),
        ChemistryCoreAcidBaseTest.bond("c1", "h1", BondType.SINGLE),
        ChemistryCoreAcidBaseTest.bond("c1", "h2", BondType.SINGLE),
        ChemistryCoreAcidBaseTest.bond("c1", "h3", BondType.SINGLE));
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static Bond bond(final String first, final String second, final BondType type) {
    return Bond.of(
        (AtomId) AtomId.of((String) first), (AtomId) AtomId.of((String) second), (BondType) type);
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