/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.analysis.ReactionPathwayAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;

final class ReactionPathwayAnalyzerTest {

  @Test
  void snarPathwayUsesAdditionEliminationStagesInsteadOfSingleGenericBondStep() {
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("snar_pathway_probe"),
            "SNAr pathway probe",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ReactionPathwayAnalyzerTest.chloroAzine(), StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        ReactionPathwayAnalyzerTest.ammonia(), StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ReactionPathwayAnalyzerTest.aminoAzine(), StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(
                        ReactionPathwayAnalyzerTest.hydrogenChloride(),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                null,
                List.of(),
                List.of(
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                    ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
                "heated basic SNAr pathway validation"),
            ChemistryMetadata.EMPTY);

    final ReactionPathwayEstimate estimate = new ReactionPathwayAnalyzer().analyze(reaction);
    final ReactionMechanism mechanism = estimate.mechanism();

    assertNotNull(mechanism);
    assertEquals(ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION, mechanism.family());
    assertTrue(mechanism.steps().size() >= 4);
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(
            mechanism.steps(), "nucleophile_association_sigma_complex"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(
            mechanism.steps(), "leaving_group_departure_rearomatization"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.SIGMA_COMPLEX_FORMATION));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.LEAVING_GROUP_DEPARTURE));
    assertTrue(
        mechanism.energyProfile().stream()
            .anyMatch(point -> point.id().equals("sigma_complex_intermediate")));
    assertTrue(
        mechanism.energyProfile().stream()
            .anyMatch(
                point ->
                    point.id().equals("product_complex")
                        && point.representativeMoleculeIds().size() == 2
                        && point
                            .representativeMoleculeIds()
                            .contains(ReactionPathwayAnalyzerTest.aminoAzine().id())
                        && point
                            .representativeMoleculeIds()
                            .contains(ReactionPathwayAnalyzerTest.hydrogenChloride().id())));
  }

  @Test
  void catalyticPathwayUsesAssociationTurnoverAndRegenerationStages() {
    final Reaction reaction =
        ReactionPathwayAnalyzerTest.hydrogenHalogenReaction(
            "catalytic_probe",
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)));

    final ReactionMechanism mechanism = new ReactionPathwayAnalyzer().analyze(reaction).mechanism();

    assertNotNull(mechanism);
    assertEquals(ReactionMechanismFamily.CATALYTIC_CYCLE, mechanism.family());
    assertTrue(ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "catalyst_association"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "catalytic_elementary_conversion"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(
            mechanism.steps(), "catalyst_dissociation_regeneration"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.CATALYST_ASSOCIATION));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.REDUCTIVE_ELIMINATION));
    assertTrue(
        mechanism.energyProfile().stream()
            .anyMatch(point -> point.id().contains("catalytic_cycle_productive_transition_state")));
  }

  @Test
  void photochemicalPathwaySeparatesExcitationAndConversionStages() {
    final Reaction reaction =
        ReactionPathwayAnalyzerTest.hydrogenHalogenReaction(
            "photochemical_probe",
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.PHOTOCHEMICAL)));

    final ReactionMechanism mechanism = new ReactionPathwayAnalyzer().analyze(reaction).mechanism();

    assertNotNull(mechanism);
    assertEquals(ReactionMechanismFamily.PHOTOCHEMICAL, mechanism.family());
    assertTrue(ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "photoexcitation"));
    assertTrue(ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "excited_state_conversion"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.PHOTOCHEMICAL_EXCITATION));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.TRIPLET_ENERGY_TRANSFER));
    assertTrue(
        mechanism.energyProfile().stream()
            .anyMatch(point -> point.id().contains("photochemical_activation_complex")));
  }

  @Test
  void electrochemicalPathwaySeparatesElectronTransferFromBondReorganization() {
    final Reaction reaction =
        ReactionPathwayAnalyzerTest.hydrogenHalogenReaction(
            "electrochemical_probe",
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.ELECTROCHEMICAL)));

    final ReactionMechanism mechanism = new ReactionPathwayAnalyzer().analyze(reaction).mechanism();

    assertNotNull(mechanism);
    assertEquals(ReactionMechanismFamily.ELECTROCHEMICAL, mechanism.family());
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "electron_transfer_activation"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(
            mechanism.steps(), "redox_coupled_bond_reorganization"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.ELECTRON_TRANSFER));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.ANODIC_COUPLING));
    assertTrue(
        mechanism.energyProfile().stream()
            .anyMatch(point -> point.id().contains("electrochemical_productive_transition_state")));
  }

  @Test
  void additionPathwayUsesEncounterAndAdditionStages() {
    final Reaction reaction = ReactionPathwayAnalyzerTest.additionProbeReaction();

    final ReactionMechanism mechanism = new ReactionPathwayAnalyzer().analyze(reaction).mechanism();

    assertNotNull(mechanism);
    assertEquals(ReactionMechanismFamily.ADDITION, mechanism.family());
    assertTrue(ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "encounter_complex"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasStep(mechanism.steps(), "addition_to_reactive_center"));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.DIFFUSION_LIMITED_ENCOUNTER));
    assertTrue(
        ReactionPathwayAnalyzerTest.hasEvent(
            mechanism.steps(), MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND));
    assertTrue(
        mechanism.energyProfile().stream()
            .anyMatch(point -> point.id().contains("addition_activation_complex")));
  }

  @Test
  void numericalThermalDoseAndPressureLowerPathwayBarrierWithoutOutcomeLookup() {
    final Reaction roomTemperatureReaction =
        ReactionPathwayAnalyzerTest.additionProbeReactionWithConditions(
            ReactionConditions.of(
                Temperature.of(25.0, TemperatureUnit.CELSIUS),
                Pressure.of(1.0, PressureUnit.ATMOSPHERE),
                TimeDuration.of(0.25, TimeDurationUnit.HOUR),
                List.of(),
                List.of(),
                "room condition pathway probe"));
    final Reaction forcedConditionReaction =
        ReactionPathwayAnalyzerTest.additionProbeReactionWithConditions(
            ReactionConditions.of(
                Temperature.of(145.0, TemperatureUnit.CELSIUS),
                Pressure.of(4.0, PressureUnit.BAR),
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(),
                List.of(),
                "heated pressurized pathway probe"));

    final ReactionPathwayAnalyzer analyzer = new ReactionPathwayAnalyzer();
    final ReactionPathwayEstimate roomEstimate = analyzer.analyze(roomTemperatureReaction);
    final ReactionPathwayEstimate forcedEstimate = analyzer.analyze(forcedConditionReaction);

    assertTrue(forcedEstimate.conditionActivationShift() > roomEstimate.conditionActivationShift());
    assertTrue(forcedEstimate.forwardBarrier() < roomEstimate.forwardBarrier());
    assertTrue(forcedEstimate.forwardRate() > roomEstimate.forwardRate());
  }

  private static boolean hasStep(
      final List<ElementaryReactionStep> steps, final String idFragment) {
    for (int i = 0; i < steps.size(); ++i) {
      if (steps.get(i).id().contains(idFragment)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasEvent(
      final List<ElementaryReactionStep> steps, final MechanisticEventKind eventKind) {
    for (int i = 0; i < steps.size(); ++i) {
      for (int j = 0; j < steps.get(i).events().size(); ++j) {
        if (steps.get(i).events().get(j).kind() == eventKind) {
          return true;
        }
      }
    }
    return false;
  }

  private static Molecule chloroAzine() {
    return ReactionPathwayAnalyzerTest.molecule(
        "chloro_azine",
        "Chloro azine",
        List.of("N", "C", "C", "C", "C", "C", "Cl", "H", "H", "H", "H"),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("a7"), BondType.SINGLE),
            Bond.of(AtomId.of("a3"), AtomId.of("a8"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a9"), BondType.SINGLE),
            Bond.of(AtomId.of("a5"), AtomId.of("a10"), BondType.SINGLE)));
  }

  private static Molecule aminoAzine() {
    return ReactionPathwayAnalyzerTest.molecule(
        "amino_azine",
        "Amino azine",
        List.of("N", "C", "C", "C", "C", "C", "N", "H", "H", "H", "H", "H", "H"),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a2"), AtomId.of("a7"), BondType.SINGLE),
            Bond.of(AtomId.of("a3"), AtomId.of("a8"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("a9"), BondType.SINGLE),
            Bond.of(AtomId.of("a5"), AtomId.of("a10"), BondType.SINGLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a11"), BondType.SINGLE),
            Bond.of(AtomId.of("a6"), AtomId.of("a12"), BondType.SINGLE)));
  }

  private static Molecule ammonia() {
    return ReactionPathwayAnalyzerTest.molecule(
        "ammonia",
        "Ammonia",
        List.of("N", "H", "H", "H"),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a3"), BondType.SINGLE)));
  }

  private static Molecule hydrogenChloride() {
    return ReactionPathwayAnalyzerTest.molecule(
        "hydrogen_chloride",
        "Hydrogen chloride",
        List.of("H", "Cl"),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE)));
  }

  private static Reaction hydrogenHalogenReaction(
      final String id, final List<ReactionConditionFeature> features) {
    return Reaction.of(
        ReactionId.of(id),
        "Hydrogen halogen pathway probe",
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionPathwayAnalyzerTest.hydrogen(), StoichiometricCoefficient.ONE),
                ReactionParticipant.of(
                    ReactionPathwayAnalyzerTest.chlorine(), StoichiometricCoefficient.ONE))),
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionPathwayAnalyzerTest.hydrogenChloride(),
                    StoichiometricCoefficient.of(2)))),
        ReactionConditions.of(
            Temperature.of(80.0, TemperatureUnit.CELSIUS),
            null,
            null,
            List.of(),
            features,
            "generic condition-controlled pathway probe"),
        ChemistryMetadata.EMPTY);
  }

  private static Reaction additionProbeReaction() {
    return ReactionPathwayAnalyzerTest.additionProbeReactionWithConditions(
        ReactionConditions.of(
            Temperature.of(40.0, TemperatureUnit.CELSIUS),
            null,
            null,
            List.of(),
            List.of(),
            "balanced addition probe"));
  }

  private static Reaction additionProbeReactionWithConditions(final ReactionConditions conditions) {
    return Reaction.of(
        ReactionId.of("addition_probe"),
        "Addition pathway probe",
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionPathwayAnalyzerTest.hydrogen(), StoichiometricCoefficient.ONE),
                ReactionParticipant.of(
                    ReactionPathwayAnalyzerTest.ethene(), StoichiometricCoefficient.ONE))),
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    ReactionPathwayAnalyzerTest.ethane(), StoichiometricCoefficient.ONE))),
        conditions,
        ChemistryMetadata.EMPTY);
  }

  private static Molecule hydrogen() {
    return ReactionPathwayAnalyzerTest.molecule(
        "hydrogen",
        "Hydrogen",
        List.of("H", "H"),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE)));
  }

  private static Molecule chlorine() {
    return ReactionPathwayAnalyzerTest.molecule(
        "chlorine",
        "Chlorine",
        List.of("Cl", "Cl"),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE)));
  }

  private static Molecule ethene() {
    return ReactionPathwayAnalyzerTest.molecule(
        "ethene",
        "Ethene",
        List.of("C", "C", "H", "H", "H", "H"),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a3"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a4"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a5"), BondType.SINGLE)));
  }

  private static Molecule ethane() {
    return ReactionPathwayAnalyzerTest.molecule(
        "ethane",
        "Ethane",
        List.of("C", "C", "H", "H", "H", "H", "H", "H"),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a2"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a3"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("a4"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a5"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("a7"), BondType.SINGLE)));
  }

  private static Molecule molecule(
      final String id,
      final String displayName,
      final List<String> symbols,
      final List<Bond> bonds) {
    final Atom[] atoms = new Atom[symbols.size()];
    for (int i = 0; i < symbols.size(); ++i) {
      atoms[i] = Atom.of(AtomId.of("a" + i), ElementSymbol.of(symbols.get(i)), null);
    }
    return Molecule.of(
        MoleculeId.of(id),
        displayName,
        List.of(atoms),
        bonds,
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}
