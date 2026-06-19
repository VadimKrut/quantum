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
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ArrheniusParameters;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ConcentrationPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.EyringParameters;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.KineticMeasurement;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.KineticUnitConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.RateConstant;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.RateConstantUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ReactionKineticProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ReactionOrderTerm;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ReactionRateLaw;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ReactionRateUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ReactionRateValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreKineticsTest {

  private static final Temperature STANDARD_TEMPERATURE =
      Temperature.of((double) 298.15, (TemperatureUnit) TemperatureUnit.KELVIN);

  ChemistryCoreKineticsTest() {}

  @Test
  void secondOrderRateLawComputesRateAndProfileResidual() {
    ReactionRateLaw law =
        ReactionRateLaw.of(
            (String) "kinetics.hcl.rate_law",
            (RateConstant)
                RateConstant.of(
                    (double) 0.5, (RateConstantUnit) RateConstantUnit.LITER_PER_MOLE_SECOND),
            List.of(
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0),
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.chlorine().id(), (double) 1.0)));
    KineticMeasurement measurement =
        KineticMeasurement.of(
            (ReactionConditions) ChemistryCoreKineticsTest.measuredConditions(),
            List.of(
                ConcentrationPoint.of(
                    (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(),
                    (MolarConcentration)
                        MolarConcentration.of(
                            (double) 0.2,
                            (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER)),
                ConcentrationPoint.of(
                    (MoleculeId) ChemistryCoreKineticsTest.chlorine().id(),
                    (MolarConcentration)
                        MolarConcentration.of(
                            (double) 100.0,
                            (MolarConcentrationUnit) MolarConcentrationUnit.MILLIMOLE_PER_LITER))),
            (ReactionRateValue)
                ReactionRateValue.of(
                    (double) 0.6, (ReactionRateUnit) ReactionRateUnit.MOLE_PER_LITER_MINUTE));
    final ReactionKineticProfile profile =
        ReactionKineticProfile.of(
            (String) "kinetics.hcl",
            (Reaction) ChemistryCoreKineticsTest.hydrogenChlorideReaction(),
            (ReactionRateLaw) law,
            List.of(measurement));
    Assertions.assertEquals((double) 2.0, (double) law.totalOrder(), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.01,
        (double) law.rateMolePerLiterSecond(measurement.concentrations()),
        (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.01, (double) measurement.observedRateMolePerLiterSecond(), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.0, (double) profile.residualMolePerLiterSecond(0), (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 0.01,
        (double) profile.predictedRateMolePerLiterSecond(measurement.concentrations()),
        (double) 1.0E-12);
  }

  @Test
  void arrheniusAndEyringParametersComputeTemperatureDependentRateConstants() {
    final ArrheniusParameters arrhenius =
        ArrheniusParameters.of(
            (double) 1.0E12,
            (RateConstantUnit) RateConstantUnit.PER_SECOND,
            (EnergyValue)
                EnergyValue.of((double) 75.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE));
    final EyringParameters eyring =
        EyringParameters.of(
            (EnergyValue) EnergyValue.of((double) 75.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
            (EntropyValue)
                EntropyValue.of((double) -50.0, (EntropyUnit) EntropyUnit.JOULE_PER_MOLE_KELVIN));
    Assertions.assertEquals(
        (double) 0.072538,
        (double) arrhenius.rateConstantAt(STANDARD_TEMPERATURE).value(),
        (double) 1.0E-6);
    Assertions.assertEquals(
        (double) 0.001101,
        (double) eyring.rateConstantAt(STANDARD_TEMPERATURE).value(),
        (double) 1.0E-6);
    Assertions.assertTrue(
        (arrhenius
                        .rateConstantAt(
                            Temperature.of(
                                (double) 350.0, (TemperatureUnit) TemperatureUnit.KELVIN))
                        .value()
                    > arrhenius.rateConstantAt(STANDARD_TEMPERATURE).value()
                ? 1
                : 0)
            != 0);
  }

  @Test
  void kineticConvertersNormalizeConcentrationsAndRates() {
    Assertions.assertEquals(
        (double) 0.001,
        (double)
            KineticUnitConverter.concentrationMolePerLiter(
                (MolarConcentration)
                    MolarConcentration.of(
                        (double) 1000.0,
                        (MolarConcentrationUnit) MolarConcentrationUnit.MICROMOLE_PER_LITER)),
        (double) 1.0E-15);
    Assertions.assertEquals(
        (double) 0.01,
        (double)
            KineticUnitConverter.rateMolePerLiterSecond(
                (ReactionRateValue)
                    ReactionRateValue.of(
                        (double) 36.0, (ReactionRateUnit) ReactionRateUnit.MOLE_PER_LITER_HOUR)),
        (double) 1.0E-15);
  }

  @Test
  void rateLawRejectsWrongUnitDuplicateTermsAndMissingConcentration() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionRateLaw.of(
                (String) "bad.unit",
                (RateConstant)
                    RateConstant.of((double) 0.5, (RateConstantUnit) RateConstantUnit.PER_SECOND),
                List.of(
                    ReactionOrderTerm.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0),
                    ReactionOrderTerm.of(
                        (MoleculeId) ChemistryCoreKineticsTest.chlorine().id(), (double) 1.0))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionRateLaw.of(
                (String) "bad.duplicate",
                (RateConstant)
                    RateConstant.of(
                        (double) 0.5, (RateConstantUnit) RateConstantUnit.LITER_PER_MOLE_SECOND),
                List.of(
                    ReactionOrderTerm.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0),
                    ReactionOrderTerm.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0))));
    ReactionRateLaw law =
        ReactionRateLaw.of(
            (String) "missing.concentration",
            (RateConstant)
                RateConstant.of(
                    (double) 0.5, (RateConstantUnit) RateConstantUnit.LITER_PER_MOLE_SECOND),
            List.of(
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0),
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.chlorine().id(), (double) 1.0)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            law.rateMolePerLiterSecond(
                List.of(
                    ConcentrationPoint.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(),
                        (MolarConcentration)
                            MolarConcentration.of(
                                (double) 0.2,
                                (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER)))));
  }

  @Test
  void kineticProfileRejectsUnbalancedReactionAndForeignOrderMolecule() {
    final ReactionRateLaw foreignLaw =
        ReactionRateLaw.of(
            (String) "foreign.order",
            (RateConstant)
                RateConstant.of((double) 1.0, (RateConstantUnit) RateConstantUnit.PER_SECOND),
            List.of(
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.water().id(), (double) 1.0)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionKineticProfile.of(
                (String) "foreign",
                (Reaction) ChemistryCoreKineticsTest.hydrogenChlorideReaction(),
                (ReactionRateLaw) foreignLaw,
                List.of()));
    final ReactionRateLaw firstOrderHydrogen =
        ReactionRateLaw.of(
            (String) "hydrogen.order",
            (RateConstant)
                RateConstant.of((double) 1.0, (RateConstantUnit) RateConstantUnit.PER_SECOND),
            List.of(
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionKineticProfile.of(
                (String) "unbalanced",
                (Reaction)
                    Reaction.of(
                        (ReactionId) ReactionId.of((String) "reaction.bad"),
                        (String) "Bad reaction",
                        (ReactionSide)
                            ReactionSide.of(
                                List.of(
                                    ReactionParticipant.of(
                                        (Molecule) ChemistryCoreKineticsTest.hydrogen(),
                                        (StoichiometricCoefficient)
                                            StoichiometricCoefficient.ONE))),
                        (ReactionSide)
                            ReactionSide.of(
                                List.of(
                                    ReactionParticipant.of(
                                        (Molecule) ChemistryCoreKineticsTest.hydrogenChloride(),
                                        (StoichiometricCoefficient)
                                            StoichiometricCoefficient.ONE)))),
                (ReactionRateLaw) firstOrderHydrogen,
                List.of()));
  }

  @Test
  void kineticMeasurementRejectsMissingTemperatureInvalidRateAndDuplicateConcentrations() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            KineticMeasurement.of(
                null,
                List.of(
                    ConcentrationPoint.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(),
                        (MolarConcentration)
                            MolarConcentration.of(
                                (double) 0.2,
                                (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER))),
                (ReactionRateValue)
                    ReactionRateValue.of(
                        (double) 0.01, (ReactionRateUnit) ReactionRateUnit.MOLE_PER_LITER_SECOND)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionRateValue.of(
                (double) 0.0, (ReactionRateUnit) ReactionRateUnit.MOLE_PER_LITER_SECOND));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            KineticMeasurement.of(
                (ReactionConditions) ChemistryCoreKineticsTest.measuredConditions(),
                List.of(
                    ConcentrationPoint.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(),
                        (MolarConcentration)
                            MolarConcentration.of(
                                (double) 0.2,
                                (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER)),
                    ConcentrationPoint.of(
                        (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(),
                        (MolarConcentration)
                            MolarConcentration.of(
                                (double) 0.3,
                                (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER))),
                (ReactionRateValue)
                    ReactionRateValue.of(
                        (double) 0.01, (ReactionRateUnit) ReactionRateUnit.MOLE_PER_LITER_SECOND)));
  }

  @Test
  void kineticTemperatureModelsRejectInvalidInput() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrheniusParameters.of(
                (double) Double.NaN,
                (RateConstantUnit) RateConstantUnit.PER_SECOND,
                (EnergyValue)
                    EnergyValue.of((double) 75.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            EyringParameters.of(
                null,
                (EntropyValue)
                    EntropyValue.of(
                        (double) 0.0, (EntropyUnit) EntropyUnit.JOULE_PER_MOLE_KELVIN)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionOrderTerm.of(
                (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) -1.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ConcentrationPoint.of((MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), null));
  }

  @Test
  void validatorReportsKineticProfileWithoutMeasurementsAndResiduals() {
    final ReactionRateLaw law =
        ReactionRateLaw.of(
            (String) "kinetics.validator.rate_law",
            (RateConstant)
                RateConstant.of(
                    (double) 0.5, (RateConstantUnit) RateConstantUnit.LITER_PER_MOLE_SECOND),
            List.of(
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(), (double) 1.0),
                ReactionOrderTerm.of(
                    (MoleculeId) ChemistryCoreKineticsTest.chlorine().id(), (double) 1.0)));
    final ReactionKineticProfile emptyProfile =
        ReactionKineticProfile.of(
            (String) "kinetics.empty",
            (Reaction) ChemistryCoreKineticsTest.hydrogenChlorideReaction(),
            (ReactionRateLaw) law,
            List.of());
    final ChemistryValidationResult emptyResult =
        new ChemistryCoreValidator().validateReactionKineticProfile(emptyProfile);
    Assertions.assertTrue((boolean) emptyResult.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreKineticsTest.contains(
                emptyResult.diagnostics(),
                ChemistryDiagnosticCode.KINETIC_PROFILE_HAS_NO_MEASUREMENTS,
                ChemistryDiagnosticSeverity.WARNING));
    final KineticMeasurement measurement =
        KineticMeasurement.of(
            (ReactionConditions) ChemistryCoreKineticsTest.measuredConditions(),
            List.of(
                ConcentrationPoint.of(
                    (MoleculeId) ChemistryCoreKineticsTest.hydrogen().id(),
                    (MolarConcentration)
                        MolarConcentration.of(
                            (double) 0.2,
                            (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER)),
                ConcentrationPoint.of(
                    (MoleculeId) ChemistryCoreKineticsTest.chlorine().id(),
                    (MolarConcentration)
                        MolarConcentration.of(
                            (double) 0.1,
                            (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER))),
            (ReactionRateValue)
                ReactionRateValue.of(
                    (double) 0.02, (ReactionRateUnit) ReactionRateUnit.MOLE_PER_LITER_SECOND));
    final ReactionKineticProfile residualProfile =
        ReactionKineticProfile.of(
            (String) "kinetics.residual",
            (Reaction) ChemistryCoreKineticsTest.hydrogenChlorideReaction(),
            (ReactionRateLaw) law,
            List.of(measurement));
    final ChemistryValidationResult residualResult =
        new ChemistryCoreValidator().validateReactionKineticProfile(residualProfile);
    Assertions.assertTrue((boolean) residualResult.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreKineticsTest.contains(
                residualResult.diagnostics(),
                ChemistryDiagnosticCode.KINETIC_PROFILE_MEASUREMENT_RESIDUAL_DETECTED,
                ChemistryDiagnosticSeverity.WARNING));
  }

  private static Reaction hydrogenChlorideReaction() {
    return Reaction.of(
        (ReactionId) ReactionId.of((String) "reaction.hydrogen_chloride"),
        (String) "Hydrogen chloride formation",
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreKineticsTest.hydrogen(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreKineticsTest.chlorine(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreKineticsTest.hydrogenChloride(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.of((int) 2)))));
  }

  private static ReactionConditions measuredConditions() {
    return ReactionConditions.of(
        (Temperature) STANDARD_TEMPERATURE, null, List.of(), (String) "measured rate");
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "h2"),
        (String) "Hydrogen",
        List.of(
            ChemistryCoreKineticsTest.atom("h1", "H", 0.0),
            ChemistryCoreKineticsTest.atom("h2", "H", 0.74)),
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
            ChemistryCoreKineticsTest.atom("cl1", "Cl", 0.0),
            ChemistryCoreKineticsTest.atom("cl2", "Cl", 1.99)),
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
            ChemistryCoreKineticsTest.atom("h", "H", 0.0),
            ChemistryCoreKineticsTest.atom("cl", "Cl", 1.27)),
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
            ChemistryCoreKineticsTest.atom("o", "O", 0.0),
            ChemistryCoreKineticsTest.atom("h1", "H", 0.95),
            ChemistryCoreKineticsTest.atom("h2", "H", -0.95)),
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