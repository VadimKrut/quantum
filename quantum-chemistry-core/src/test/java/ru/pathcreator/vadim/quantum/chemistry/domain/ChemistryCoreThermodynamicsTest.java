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
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.EntropyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.EntropyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.MolecularThermodynamicData;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ReactionThermodynamicProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ThermodynamicUnitConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreThermodynamicsTest {

  private static final Temperature STANDARD_TEMPERATURE =
      Temperature.of((double) 298.15, (TemperatureUnit) TemperatureUnit.KELVIN);

  ChemistryCoreThermodynamicsTest() {}

  @Test
  void reactionThermodynamicsComputesDeltaValuesAndEquilibriumConstant() {
    ReactionThermodynamicProfile profile =
        ReactionThermodynamicProfile.of(
            (String) "thermo.hcl",
            (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
            (Temperature) STANDARD_TEMPERATURE,
            List.of(
                ChemistryCoreThermodynamicsTest.hydrogenData(),
                ChemistryCoreThermodynamicsTest.chlorineData(),
                ChemistryCoreThermodynamicsTest.hydrogenChlorideData()));
    Assertions.assertEquals(
        (double) -184.6, (double) profile.enthalpyDeltaKiloJoulePerMole(), (double) 1.0E-9);
    Assertions.assertEquals(
        (double) 19.62, (double) profile.entropyDeltaJoulePerMoleKelvin(), (double) 1.0E-9);
    Assertions.assertEquals(
        (double) -190.449703,
        (double) profile.gibbsFreeEnergyDeltaKiloJoulePerMole(),
        (double) 1.0E-6);
    Assertions.assertEquals(
        (double) 76.826544, (double) profile.logEquilibriumConstant(), (double) 1.0E-6);
    Assertions.assertTrue((profile.equilibriumConstant() > 1.0E30 ? 1 : 0) != 0);
  }

  @Test
  void reactionThermodynamicsPrefersDirectGibbsWhenPresentForEveryParticipant() {
    ReactionThermodynamicProfile profile =
        ReactionThermodynamicProfile.of(
            (String) "thermo.direct_gibbs",
            (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
            (Temperature) STANDARD_TEMPERATURE,
            List.of(
                ChemistryCoreThermodynamicsTest.data(
                    ChemistryCoreThermodynamicsTest.hydrogen().id(), 0.0, 0.0, 130.68),
                ChemistryCoreThermodynamicsTest.data(
                    ChemistryCoreThermodynamicsTest.chlorine().id(), 0.0, 0.0, 223.08),
                ChemistryCoreThermodynamicsTest.data(
                    ChemistryCoreThermodynamicsTest.hydrogenChloride().id(),
                    -92.3,
                    -95.25,
                    186.69)));
    Assertions.assertEquals(
        (double) -190.5, (double) profile.gibbsFreeEnergyDeltaKiloJoulePerMole(), (double) 1.0E-9);
  }

  @Test
  void thermodynamicConvertersNormalizeEnergyEntropyAndTemperature() {
    Assertions.assertEquals(
        (double) 2625.4996394799,
        (double)
            ThermodynamicUnitConverter.energyKiloJoulePerMole(
                (EnergyValue) EnergyValue.of((double) 1.0, (EnergyUnit) EnergyUnit.HARTREE)),
        (double) 1.0E-10);
    Assertions.assertEquals(
        (double) 96.4853321233,
        (double)
            ThermodynamicUnitConverter.energyKiloJoulePerMole(
                (EnergyValue) EnergyValue.of((double) 1.0, (EnergyUnit) EnergyUnit.ELECTRON_VOLT)),
        (double) 1.0E-10);
    Assertions.assertEquals(
        (double) 4.184,
        (double)
            ThermodynamicUnitConverter.entropyJoulePerMoleKelvin(
                (EntropyValue)
                    EntropyValue.of(
                        (double) 1.0, (EntropyUnit) EntropyUnit.CALORIE_PER_MOLE_KELVIN)),
        (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 298.15,
        (double)
            ThermodynamicUnitConverter.temperatureKelvin(
                (Temperature)
                    Temperature.of((double) 25.0, (TemperatureUnit) TemperatureUnit.CELSIUS)),
        (double) 1.0E-12);
  }

  @Test
  void thermodynamicProfileRejectsUnbalancedReactionsMissingDataAndExtraData() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionThermodynamicProfile.of(
                (String) "thermo.unbalanced",
                (Reaction)
                    Reaction.of(
                        (ReactionId) ReactionId.of((String) "reaction.bad_hcl"),
                        (String) "Bad hydrogen chloride",
                        (ReactionSide)
                            ReactionSide.of(
                                List.of(
                                    ReactionParticipant.of(
                                        (Molecule) ChemistryCoreThermodynamicsTest.hydrogen(),
                                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE),
                                    ReactionParticipant.of(
                                        (Molecule) ChemistryCoreThermodynamicsTest.chlorine(),
                                        (StoichiometricCoefficient)
                                            StoichiometricCoefficient.ONE))),
                        (ReactionSide)
                            ReactionSide.of(
                                List.of(
                                    ReactionParticipant.of(
                                        (Molecule)
                                            ChemistryCoreThermodynamicsTest.hydrogenChloride(),
                                        (StoichiometricCoefficient)
                                            StoichiometricCoefficient.ONE)))),
                (Temperature) STANDARD_TEMPERATURE,
                List.of(
                    ChemistryCoreThermodynamicsTest.hydrogenData(),
                    ChemistryCoreThermodynamicsTest.chlorineData(),
                    ChemistryCoreThermodynamicsTest.hydrogenChlorideData())));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionThermodynamicProfile.of(
                (String) "thermo.missing",
                (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
                (Temperature) STANDARD_TEMPERATURE,
                List.of(ChemistryCoreThermodynamicsTest.hydrogenData())));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionThermodynamicProfile.of(
                (String) "thermo.extra",
                (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
                (Temperature) STANDARD_TEMPERATURE,
                List.of(
                    ChemistryCoreThermodynamicsTest.hydrogenData(),
                    ChemistryCoreThermodynamicsTest.chlorineData(),
                    ChemistryCoreThermodynamicsTest.hydrogenChlorideData(),
                    ChemistryCoreThermodynamicsTest.waterData())));
  }

  @Test
  void thermodynamicProfileRejectsDuplicateMoleculeDataAndTemperatureMismatch() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionThermodynamicProfile.of(
                (String) "thermo.duplicate",
                (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
                (Temperature) STANDARD_TEMPERATURE,
                List.of(
                    ChemistryCoreThermodynamicsTest.hydrogenData(),
                    ChemistryCoreThermodynamicsTest.hydrogenData(),
                    ChemistryCoreThermodynamicsTest.chlorineData(),
                    ChemistryCoreThermodynamicsTest.hydrogenChlorideData())));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionThermodynamicProfile.of(
                (String) "thermo.temperature",
                (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
                (Temperature) STANDARD_TEMPERATURE,
                List.of(
                    MolecularThermodynamicData.of(
                        (MoleculeId) ChemistryCoreThermodynamicsTest.hydrogen().id(),
                        (Temperature)
                            Temperature.of(
                                (double) 300.0, (TemperatureUnit) TemperatureUnit.KELVIN),
                        (EnergyValue)
                            EnergyValue.of(
                                (double) 0.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                        null,
                        (EntropyValue)
                            EntropyValue.of(
                                (double) 130.68, (EntropyUnit) EntropyUnit.JOULE_PER_MOLE_KELVIN),
                        null),
                    ChemistryCoreThermodynamicsTest.chlorineData(),
                    ChemistryCoreThermodynamicsTest.hydrogenChlorideData())));
  }

  @Test
  void thermodynamicProfileReportsMissingQuantityOnlyWhenRequested() {
    ReactionThermodynamicProfile profile =
        ReactionThermodynamicProfile.of(
            (String) "thermo.no_entropy",
            (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
            (Temperature) STANDARD_TEMPERATURE,
            List.of(
                MolecularThermodynamicData.of(
                    (MoleculeId) ChemistryCoreThermodynamicsTest.hydrogen().id(),
                    (Temperature) STANDARD_TEMPERATURE,
                    (EnergyValue)
                        EnergyValue.of((double) 0.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                    null,
                    null,
                    null),
                MolecularThermodynamicData.of(
                    (MoleculeId) ChemistryCoreThermodynamicsTest.chlorine().id(),
                    (Temperature) STANDARD_TEMPERATURE,
                    (EnergyValue)
                        EnergyValue.of((double) 0.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                    null,
                    null,
                    null),
                MolecularThermodynamicData.of(
                    (MoleculeId) ChemistryCoreThermodynamicsTest.hydrogenChloride().id(),
                    (Temperature) STANDARD_TEMPERATURE,
                    (EnergyValue)
                        EnergyValue.of((double) -92.3, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                    null,
                    null,
                    null)));
    Assertions.assertEquals(
        (double) -184.6, (double) profile.enthalpyDeltaKiloJoulePerMole(), (double) 1.0E-9);
    Assertions.assertThrows(
        IllegalStateException.class, () -> profile.gibbsFreeEnergyDeltaKiloJoulePerMole());
  }

  @Test
  void validatorReportsMissingThermodynamicQuantitiesAsWarnings() {
    final ReactionThermodynamicProfile profile =
        ReactionThermodynamicProfile.of(
            (String) "thermo.validator.no_entropy",
            (Reaction) ChemistryCoreThermodynamicsTest.hydrogenChlorideReaction(),
            (Temperature) STANDARD_TEMPERATURE,
            List.of(
                MolecularThermodynamicData.of(
                    (MoleculeId) ChemistryCoreThermodynamicsTest.hydrogen().id(),
                    (Temperature) STANDARD_TEMPERATURE,
                    (EnergyValue)
                        EnergyValue.of((double) 0.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                    null,
                    null,
                    null),
                MolecularThermodynamicData.of(
                    (MoleculeId) ChemistryCoreThermodynamicsTest.chlorine().id(),
                    (Temperature) STANDARD_TEMPERATURE,
                    (EnergyValue)
                        EnergyValue.of((double) 0.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                    null,
                    null,
                    null),
                MolecularThermodynamicData.of(
                    (MoleculeId) ChemistryCoreThermodynamicsTest.hydrogenChloride().id(),
                    (Temperature) STANDARD_TEMPERATURE,
                    (EnergyValue)
                        EnergyValue.of((double) -92.3, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                    null,
                    null,
                    null)));
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateReactionThermodynamicProfile(profile);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreThermodynamicsTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.THERMODYNAMIC_PROFILE_HAS_NO_DIRECT_GIBBS_DATA,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreThermodynamicsTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.THERMODYNAMIC_PROFILE_HAS_NO_ENTROPY_DATA,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreThermodynamicsTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.THERMODYNAMIC_PROFILE_HAS_NO_ZERO_POINT_ENERGY_DATA,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void thermodynamicValuesRejectInvalidInput() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            EntropyValue.of(
                (double) Double.POSITIVE_INFINITY,
                (EntropyUnit) EntropyUnit.JOULE_PER_MOLE_KELVIN));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> EntropyValue.of((double) 1.0, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularThermodynamicData.of(
                (MoleculeId) ChemistryCoreThermodynamicsTest.hydrogen().id(),
                (Temperature) STANDARD_TEMPERATURE,
                null,
                null,
                null,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ThermodynamicUnitConverter.energyKiloJoulePerMole(null));
  }

  private static Reaction hydrogenChlorideReaction() {
    return Reaction.of(
        (ReactionId) ReactionId.of((String) "reaction.hydrogen_chloride"),
        (String) "Hydrogen chloride formation",
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreThermodynamicsTest.hydrogen(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreThermodynamicsTest.chlorine(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreThermodynamicsTest.hydrogenChloride(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.of((int) 2)))));
  }

  private static MolecularThermodynamicData hydrogenData() {
    return ChemistryCoreThermodynamicsTest.data(
        ChemistryCoreThermodynamicsTest.hydrogen().id(), 0.0, null, 130.68);
  }

  private static MolecularThermodynamicData chlorineData() {
    return ChemistryCoreThermodynamicsTest.data(
        ChemistryCoreThermodynamicsTest.chlorine().id(), 0.0, null, 223.08);
  }

  private static MolecularThermodynamicData hydrogenChlorideData() {
    return ChemistryCoreThermodynamicsTest.data(
        ChemistryCoreThermodynamicsTest.hydrogenChloride().id(), -92.3, null, 186.69);
  }

  private static MolecularThermodynamicData waterData() {
    return ChemistryCoreThermodynamicsTest.data(
        ChemistryCoreThermodynamicsTest.water().id(), -285.83, null, 69.91);
  }

  private static MolecularThermodynamicData data(
      final MoleculeId moleculeId,
      final double enthalpyKiloJoulePerMole,
      final Double gibbsKiloJoulePerMole,
      final double entropyJoulePerMoleKelvin) {
    final EnergyValue gibbs =
        gibbsKiloJoulePerMole == null
            ? null
            : EnergyValue.of(
                (double) gibbsKiloJoulePerMole, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE);
    return MolecularThermodynamicData.of(
        (MoleculeId) moleculeId,
        (Temperature) STANDARD_TEMPERATURE,
        (EnergyValue)
            EnergyValue.of(
                (double) enthalpyKiloJoulePerMole, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
        (EnergyValue) gibbs,
        (EntropyValue)
            EntropyValue.of(
                (double) entropyJoulePerMoleKelvin,
                (EntropyUnit) EntropyUnit.JOULE_PER_MOLE_KELVIN),
        null);
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "h2"),
        (String) "Hydrogen",
        List.of(
            ChemistryCoreThermodynamicsTest.atom("h1", "H", 0.0),
            ChemistryCoreThermodynamicsTest.atom("h2", "H", 0.74)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "h1"),
                (AtomId) AtomId.of((String) "h2"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule chlorine() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "cl2"),
        (String) "Chlorine",
        List.of(
            ChemistryCoreThermodynamicsTest.atom("cl1", "Cl", 0.0),
            ChemistryCoreThermodynamicsTest.atom("cl2", "Cl", 1.99)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "cl1"),
                (AtomId) AtomId.of((String) "cl2"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenChloride() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hcl"),
        (String) "Hydrogen chloride",
        List.of(
            ChemistryCoreThermodynamicsTest.atom("h", "H", 0.0),
            ChemistryCoreThermodynamicsTest.atom("cl", "Cl", 1.27)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "h"),
                (AtomId) AtomId.of((String) "cl"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule water() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "water"),
        (String) "Water",
        List.of(
            ChemistryCoreThermodynamicsTest.atom("o", "O", 0.0),
            ChemistryCoreThermodynamicsTest.atom("h1", "H", 0.95),
            ChemistryCoreThermodynamicsTest.atom("h2", "H", -0.95)),
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

  private static Atom atom(final String id, final String symbol, final double x) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) x, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
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