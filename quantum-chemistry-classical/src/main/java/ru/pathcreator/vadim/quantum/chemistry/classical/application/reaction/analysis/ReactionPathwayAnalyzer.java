/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionBondChangeSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismEnergyPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismPointKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionCoordinateValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionBalance;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;

/**
 * Строит внутреннюю механистическую оценку пути реакции без внешних backend-ов.
 */
public final class ReactionPathwayAnalyzer {

  private static final double ROOM_TEMPERATURE_KELVIN = 298.15;
  private static final double ROOM_THERMAL_ENERGY_KJ_PER_MOLE = 2.4788191;
  private static final double MINIMUM_BARRIER_KJ_PER_MOLE = 4.0;
  private static final double HIGH_THERMAL_DOSE_KELVIN_HOUR = 240.0;

  public ReactionPathwayEstimate analyze(final Reaction reaction) {
    return this.analyze(reaction, null);
  }

  public ReactionPathwayEstimate analyze(
      final Reaction reaction, final Double reactionEnergyOverrideKilojoulePerMole) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction pathway reaction must not be null.");
    }
    if (reactionEnergyOverrideKilojoulePerMole != null
        && !Double.isFinite(reactionEnergyOverrideKilojoulePerMole)) {
      throw new IllegalArgumentException(
          "Reaction pathway electronic reaction energy must be finite.");
    }
    final ReactionBondChangeSet bondChanges = ReactionPathwayAnalyzer.bondChanges(reaction);
    final double temperatureKelvin =
        ReactionPathwayAnalyzer.temperatureKelvin(reaction.conditions());
    final double activationShift = ReactionPathwayAnalyzer.activationShift(reaction.conditions());
    final double reactionEnergy =
        reactionEnergyOverrideKilojoulePerMole == null
            ? bondChanges.brokenBondEnergy() - bondChanges.formedBondEnergy()
            : reactionEnergyOverrideKilojoulePerMole.doubleValue();
    final double forwardBarrier =
        Math.max(
            MINIMUM_BARRIER_KJ_PER_MOLE,
            bondChanges.brokenBondEnergy() * 0.35
                + bondChanges.formedBondEnergy() * 0.12
                + Math.max(0.0, reactionEnergy) * 0.20
                + 18.0
                - activationShift);
    final double reverseBarrier =
        Math.max(MINIMUM_BARRIER_KJ_PER_MOLE, forwardBarrier - reactionEnergy);
    final double thermalEnergy =
        ROOM_THERMAL_ENERGY_KJ_PER_MOLE * temperatureKelvin / ROOM_TEMPERATURE_KELVIN;
    final double forwardRate = Math.exp(-forwardBarrier / Math.max(1.0E-9, thermalEnergy));
    final double reverseRate = Math.exp(-reverseBarrier / Math.max(1.0E-9, thermalEnergy));
    return ReactionPathwayEstimate.of(
        bondChanges,
        ReactionPathwayAnalyzer.mechanismIfBalanced(
            reaction, bondChanges, activationShift, forwardBarrier, reverseBarrier, reactionEnergy),
        temperatureKelvin,
        activationShift,
        forwardBarrier,
        reverseBarrier,
        reactionEnergy,
        thermalEnergy,
        forwardRate,
        reverseRate,
        ReactionPathwayAnalyzer.pathwayBand(forwardBarrier, activationShift));
  }

  public static double bondEnergyKilojoulePerMole(final BondType bondType) {
    if (bondType == BondType.TRIPLE) {
      return 835.0;
    }
    if (bondType == BondType.DOUBLE) {
      return 614.0;
    }
    if (bondType == BondType.AROMATIC) {
      return 520.0;
    }
    if (bondType == BondType.UNKNOWN) {
      return 350.0;
    }
    return 348.0;
  }

  public static double temperatureKelvin(final ReactionConditions conditions) {
    if (conditions.temperature() == null) {
      return ROOM_TEMPERATURE_KELVIN;
    }
    return conditions.temperature().kelvin();
  }

  public static double activationShift(final ReactionConditions conditions) {
    double shift = 0.0;
    if (conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        || !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      shift += 18.0;
    }
    if (!conditions.componentsByRole(ReactionComponentRole.LIGAND).isEmpty()) {
      shift += 4.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        || conditions.hasFeature(ReactionConditionFeatureKind.MICROWAVE)) {
      shift += 6.0;
    }
    if (conditions.temperature() != null && conditions.temperature().kelvin() >= 323.15) {
      shift += conditions.temperature().kelvin() >= 373.15 ? 5.0 : 3.0;
    }
    if (conditions.temperature() != null && conditions.hasDuration()) {
      final double thermalDose =
          Math.max(0.0, conditions.temperature().kelvin() - ROOM_TEMPERATURE_KELVIN)
              * conditions.duration().hours();
      if (thermalDose >= HIGH_THERMAL_DOSE_KELVIN_HOUR) {
        shift += Math.min(8.0, 2.0 + thermalDose / HIGH_THERMAL_DOSE_KELVIN_HOUR);
      }
      if (thermalDose >= HIGH_THERMAL_DOSE_KELVIN_HOUR
          && ReactionPathwayAnalyzer.polyetherMediumPresent(conditions)) {
        shift += 1.5;
      }
    }
    if (conditions.pressure() != null
        && ReactionPathwayAnalyzer.atmospherePressure(
                conditions.pressure().value(), conditions.pressure().unit())
            >= 2.0) {
      shift += 1.5;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || conditions.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      shift += 12.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)) {
      shift -= 3.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        || conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)) {
      shift += 2.0;
    }
    return shift;
  }

  private static double atmospherePressure(final double value, final PressureUnit unit) {
    if (unit == PressureUnit.PASCAL) {
      return value / 101325.0;
    }
    if (unit == PressureUnit.BAR) {
      return value / 1.01325;
    }
    return value;
  }

  private static boolean polyetherMediumPresent(final ReactionConditions conditions) {
    final java.util.List<
            ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent>
        solvents = conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    for (int i = 0; i < solvents.size(); ++i) {
      final String name = solvents.get(i).name().toLowerCase(java.util.Locale.ROOT);
      if (name.contains("peg")
          || name.contains("polyethylene glycol")
          || name.contains("poly(ethylene glycol)")
          || name.contains("polyether")
          || name.contains("polyol")
          || name.contains("glyme")
          || name.contains("diglyme")
          || name.contains("triglyme")
          || name.contains("tetraglyme")) {
        return true;
      }
    }
    return false;
  }

  private static ReactionBondChangeSet bondChanges(final Reaction reaction) {
    final Map<String, Integer> reactantSignatures =
        ReactionPathwayAnalyzer.sideBondSignatures(reaction.reactants());
    final Map<String, Integer> productSignatures =
        ReactionPathwayAnalyzer.sideBondSignatures(reaction.products());
    int reactantBondCount = 0;
    int productBondCount = 0;
    int brokenEvents = 0;
    int formedEvents = 0;
    double brokenEnergy = 0.0;
    double formedEnergy = 0.0;
    for (final Map.Entry<String, Integer> entry : reactantSignatures.entrySet()) {
      reactantBondCount += entry.getValue();
      final int productCount = productSignatures.getOrDefault(entry.getKey(), 0);
      final int difference = entry.getValue() - productCount;
      if (difference <= 0) {
        continue;
      }
      brokenEvents += difference;
      brokenEnergy += difference * ReactionPathwayAnalyzer.signatureBondEnergy(entry.getKey());
    }
    for (final Map.Entry<String, Integer> entry : productSignatures.entrySet()) {
      productBondCount += entry.getValue();
      final int reactantCount = reactantSignatures.getOrDefault(entry.getKey(), 0);
      final int difference = entry.getValue() - reactantCount;
      if (difference <= 0) {
        continue;
      }
      formedEvents += difference;
      formedEnergy += difference * ReactionPathwayAnalyzer.signatureBondEnergy(entry.getKey());
    }
    return ReactionBondChangeSet.of(
        reactantBondCount,
        productBondCount,
        brokenEvents,
        formedEvents,
        brokenEnergy,
        formedEnergy);
  }

  private static Map<String, Integer> sideBondSignatures(final ReactionSide side) {
    final HashMap<String, Integer> signatures = new HashMap<String, Integer>();
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final int coefficient = participant.coefficient().value();
      final List<Bond> bonds = participant.molecule().bonds();
      for (int j = 0; j < bonds.size(); ++j) {
        final String signature =
            ReactionPathwayAnalyzer.bondSignature(participant.molecule(), bonds.get(j));
        signatures.merge(signature, coefficient, Integer::sum);
      }
    }
    return signatures;
  }

  private static String bondSignature(final Molecule molecule, final Bond bond) {
    final String first = ReactionPathwayAnalyzer.elementSymbol(molecule, bond.firstAtomId());
    final String second = ReactionPathwayAnalyzer.elementSymbol(molecule, bond.secondAtomId());
    if (first.compareTo(second) <= 0) {
      return first + "-" + second + ":" + bond.type().name();
    }
    return second + "-" + first + ":" + bond.type().name();
  }

  private static String elementSymbol(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom.element().symbol().value();
      }
    }
    throw new IllegalStateException("Bond references unknown atom id.");
  }

  private static double signatureBondEnergy(final String signature) {
    final int separator = signature.lastIndexOf(':');
    if (separator < 0 || separator == signature.length() - 1) {
      return ReactionPathwayAnalyzer.bondEnergyKilojoulePerMole(BondType.UNKNOWN);
    }
    return ReactionPathwayAnalyzer.bondEnergyKilojoulePerMole(
        BondType.valueOf(signature.substring(separator + 1)));
  }

  private static ReactionMechanism mechanismIfBalanced(
      final Reaction reaction,
      final ReactionBondChangeSet bondChanges,
      final double activationShift,
      final double forwardBarrier,
      final double reverseBarrier,
      final double reactionEnergy) {
    final ReactionBalance balance = reaction.balance();
    if (!balance.balanced()) {
      return null;
    }
    if (bondChanges.brokenBondEvents() == 0 && bondChanges.formedBondEvents() == 0) {
      return null;
    }
    final EnergyValue forwardBarrierEnergy =
        EnergyValue.of(forwardBarrier, EnergyUnit.KILOJOULE_PER_MOLE);
    final EnergyValue reverseBarrierEnergy =
        EnergyValue.of(reverseBarrier, EnergyUnit.KILOJOULE_PER_MOLE);
    final EnergyValue reactionEnergyValue =
        EnergyValue.of(reactionEnergy, EnergyUnit.KILOJOULE_PER_MOLE);
    final Reaction intermediateReaction =
        ReactionPathwayAnalyzer.stepReaction(
            reaction, "bond_reorganization", reaction.reactants(), reaction.products());
    final ReactionMechanismFamily family =
        ReactionPathwayAnalyzer.mechanismFamily(reaction, bondChanges);
    final List<ElementaryReactionStep> steps =
        ReactionPathwayAnalyzer.mechanismSteps(
            reaction,
            intermediateReaction,
            bondChanges,
            family,
            activationShift,
            forwardBarrierEnergy,
            reverseBarrierEnergy,
            reactionEnergyValue);
    return ReactionMechanism.of(
        reaction.id().value() + "_mechanism",
        family,
        reaction,
        steps,
        ReactionPathwayAnalyzer.energyProfile(
            reaction,
            bondChanges,
            family,
            activationShift,
            forwardBarrierEnergy,
            reactionEnergyValue,
            ReactionPathwayAnalyzer.sideMoleculeIds(reaction.products())));
  }

  private static List<ElementaryReactionStep> mechanismSteps(
      final Reaction reaction,
      final Reaction intermediateReaction,
      final ReactionBondChangeSet bondChanges,
      final ReactionMechanismFamily family,
      final double activationShift,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    final ArrayList<ElementaryReactionStep> steps = new ArrayList<ElementaryReactionStep>();
    if (ReactionPathwayAnalyzer.hasMediumOrganization(reaction.conditions(), activationShift)) {
      steps.add(
          ElementaryReactionStep.of(
              "step_0_medium_organization",
              ReactionPathwayAnalyzer.stepReaction(
                  reaction, "medium_organization", reaction.reactants(), reaction.reactants()),
              null,
              EnergyValue.of(
                  Math.max(1.0, Math.abs(activationShift) * 0.25), EnergyUnit.KILOJOULE_PER_MOLE),
              EnergyValue.of(
                  Math.max(1.0, Math.abs(activationShift) * 0.15), EnergyUnit.KILOJOULE_PER_MOLE),
              EnergyValue.of(0.0, EnergyUnit.KILOJOULE_PER_MOLE),
              ReactionPathwayAnalyzer.mediumEvents(reaction.conditions(), activationShift)));
    }
    if (family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION) {
      steps.addAll(
          ReactionPathwayAnalyzer.nucleophilicAromaticSubstitutionSteps(
              reaction,
              steps.size(),
              forwardBarrierEnergy,
              reverseBarrierEnergy,
              reactionEnergyValue));
      steps.add(
          ReactionPathwayAnalyzer.productRelaxationStep(
              reaction,
              steps.size(),
              "Product complex rearomatizes and relaxes after leaving-group departure."));
      return List.copyOf(steps);
    }
    final List<ElementaryReactionStep> familySteps =
        ReactionPathwayAnalyzer.familySpecificSteps(
            reaction,
            steps.size(),
            family,
            forwardBarrierEnergy,
            reverseBarrierEnergy,
            reactionEnergyValue);
    if (!familySteps.isEmpty()) {
      steps.addAll(familySteps);
      steps.add(
          ReactionPathwayAnalyzer.productRelaxationStep(
              reaction,
              steps.size(),
              "Product complex relaxes after the staged "
                  + family.name().toLowerCase(Locale.ROOT)
                  + " pathway."));
      return List.copyOf(steps);
    }
    steps.add(
        ElementaryReactionStep.of(
            "step_" + steps.size() + "_bond_reorganization",
            intermediateReaction,
            null,
            forwardBarrierEnergy,
            reverseBarrierEnergy,
            reactionEnergyValue,
            ReactionPathwayAnalyzer.bondEvents(bondChanges)));
    steps.add(
        ReactionPathwayAnalyzer.productRelaxationStep(
            reaction, steps.size(), "Product complex relaxes after bond reorganization."));
    return List.copyOf(steps);
  }

  private static List<ElementaryReactionStep> familySpecificSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final ReactionMechanismFamily family,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    if (family == ReactionMechanismFamily.CATALYTIC_CYCLE) {
      return ReactionPathwayAnalyzer.catalyticCycleSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (family == ReactionMechanismFamily.PHOTOCHEMICAL) {
      return ReactionPathwayAnalyzer.photochemicalSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (family == ReactionMechanismFamily.ELECTROCHEMICAL) {
      return ReactionPathwayAnalyzer.electrochemicalSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (ReactionPathwayAnalyzer.isRadicalFamily(family)) {
      return ReactionPathwayAnalyzer.radicalChainSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (ReactionPathwayAnalyzer.isPericyclicFamily(family)) {
      return ReactionPathwayAnalyzer.pericyclicSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (ReactionPathwayAnalyzer.isAcidBaseOrCarbonylFamily(family)) {
      return ReactionPathwayAnalyzer.polarAdditionEliminationSteps(
          reaction,
          firstStepIndex,
          family,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (family == ReactionMechanismFamily.FRAGMENTATION) {
      return ReactionPathwayAnalyzer.fragmentationSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    if (family == ReactionMechanismFamily.ADDITION) {
      return ReactionPathwayAnalyzer.additionSteps(
          reaction,
          firstStepIndex,
          forwardBarrierEnergy,
          reverseBarrierEnergy,
          reactionEnergyValue);
    }
    return List.of();
  }

  private static List<ElementaryReactionStep> catalyticCycleSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "catalyst_association",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.18),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.12),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "catalyst_association",
                    0,
                    MechanisticEventKind.CATALYST_ASSOCIATION,
                    "Catalyst or ligand environment binds and organizes the reactive partners.",
                    0.82),
                ReactionPathwayAnalyzer.event(
                    "ligand_exchange",
                    1,
                    MechanisticEventKind.LIGAND_EXCHANGE,
                    "Ligand/solvent exchange creates the active catalytic environment.",
                    0.70))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "catalytic_elementary_conversion",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.62),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.72),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "oxidative_or_associative_activation",
                    0,
                    MechanisticEventKind.OXIDATIVE_ADDITION,
                    "Catalyst activates a substrate bond or forms the key organometallic/reactive complex.",
                    0.68),
                ReactionPathwayAnalyzer.event(
                    "productive_catalytic_turnover",
                    1,
                    MechanisticEventKind.REDUCTIVE_ELIMINATION,
                    "Product-forming turnover step releases the new connectivity.",
                    0.68))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 2,
            "catalyst_dissociation_regeneration",
            reaction.products(),
            reaction.products(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.20),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.16),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "catalyst_dissociation",
                    0,
                    MechanisticEventKind.CATALYST_DISSOCIATION,
                    "Product dissociation regenerates the catalyst resting state.",
                    0.78))));
  }

  private static List<ElementaryReactionStep> photochemicalSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "photoexcitation",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(1.0, forwardBarrierEnergy.value() * 0.12),
            Math.max(1.0, reverseBarrierEnergy.value() * 0.08),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "photochemical_excitation",
                    0,
                    MechanisticEventKind.PHOTOCHEMICAL_EXCITATION,
                    "Photon absorption or photosensitizer transfer populates an excited-state surface.",
                    0.74),
                ReactionPathwayAnalyzer.event(
                    "intersystem_crossing",
                    1,
                    MechanisticEventKind.INTERSYSTEM_CROSSING,
                    "Spin-state or surface crossing competes with direct relaxation.",
                    0.55))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "excited_state_conversion",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.88),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.86),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "triplet_energy_transfer",
                    0,
                    MechanisticEventKind.TRIPLET_ENERGY_TRANSFER,
                    "Excited-state energy or electron density is transferred into the reactive coordinate.",
                    0.62),
                ReactionPathwayAnalyzer.event(
                    "photochemical_quenching",
                    1,
                    MechanisticEventKind.PHOTOCHEMICAL_QUENCHING,
                    "Quenching competes with productive conversion and controls quantum yield.",
                    0.52))));
  }

  private static List<ElementaryReactionStep> electrochemicalSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "electron_transfer_activation",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.30),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.22),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "electron_transfer",
                    0,
                    MechanisticEventKind.ELECTRON_TRANSFER,
                    "Electrode or redox mediator changes substrate electron count before bond reorganization.",
                    0.82),
                ReactionPathwayAnalyzer.event(
                    "electrochemical_oxidation",
                    1,
                    MechanisticEventKind.ELECTROCHEMICAL_OXIDATION,
                    "Anodic oxidation competes with cathodic reduction according to applied potential.",
                    0.60))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "redox_coupled_bond_reorganization",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.70),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.78),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "anodic_coupling",
                    0,
                    MechanisticEventKind.ANODIC_COUPLING,
                    "Redox-generated intermediate enters the productive coupling/fragmentation coordinate.",
                    0.64))));
  }

  private static List<ElementaryReactionStep> radicalChainSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "radical_initiation",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.24),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.16),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "radical_initiation",
                    0,
                    MechanisticEventKind.RADICAL_INITIATION,
                    "Initiator, light, heat, or redox input creates the first radical population.",
                    0.74))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "radical_propagation",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.58),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.68),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "radical_attack",
                    0,
                    MechanisticEventKind.RADICAL_ATTACK,
                    "Radical adds to or abstracts from the reactive center.",
                    0.70),
                ReactionPathwayAnalyzer.event(
                    "radical_propagation",
                    1,
                    MechanisticEventKind.RADICAL_PROPAGATION,
                    "Productive chain propagation transfers radical character to the next carrier.",
                    0.66))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 2,
            "radical_termination_competition",
            reaction.products(),
            reaction.products(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.18),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.12),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "radical_termination",
                    0,
                    MechanisticEventKind.RADICAL_TERMINATION,
                    "Termination channels consume radical concentration and reduce productive chain length.",
                    0.48))));
  }

  private static List<ElementaryReactionStep> pericyclicSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "orbital_alignment",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(1.0, forwardBarrierEnergy.value() * 0.20),
            Math.max(1.0, reverseBarrierEnergy.value() * 0.18),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "conformational_alignment",
                    0,
                    MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                    "Reactants adopt the orbital phase and geometry required for a concerted pathway.",
                    0.76))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "concerted_pericyclic_transition",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.80),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.82),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "pericyclic_concerted",
                    0,
                    MechanisticEventKind.PERICYCLIC_CONCERTED,
                    "Bonds reorganize in a concerted cyclic transition structure without discrete ionic intermediates.",
                    0.86))));
  }

  private static List<ElementaryReactionStep> polarAdditionEliminationSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final ReactionMechanismFamily family,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    final MechanisticEventKind activationKind =
        family == ReactionMechanismFamily.ACID_BASE
            ? MechanisticEventKind.PROTON_TRANSFER
            : MechanisticEventKind.NUCLEOPHILIC_ATTACK;
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "polar_activation",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.30),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.22),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "polar_activation",
                    0,
                    activationKind,
                    "Polarization, proton transfer, or nucleophilic approach creates the reactive ion-pair complex.",
                    0.82))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "polar_bond_reorganization",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.70),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.76),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "bond_formation",
                    0,
                    MechanisticEventKind.BOND_FORMATION,
                    "Polar intermediate proceeds through bond formation and charge redistribution.",
                    0.74),
                ReactionPathwayAnalyzer.event(
                    "leaving_group_or_proton_transfer",
                    1,
                    MechanisticEventKind.PROTON_TRANSFER,
                    "Proton transfer or leaving-group assistance restores a neutral product basin.",
                    0.64))));
  }

  private static List<ElementaryReactionStep> fragmentationSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "fragmentation_activation",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(2.0, forwardBarrierEnergy.value() * 0.35),
            Math.max(2.0, reverseBarrierEnergy.value() * 0.25),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "bond_polarization",
                    0,
                    MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                    "Reactive conformation or electronic polarization aligns the scissile bond.",
                    0.62))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "fragment_release",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.65),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.75),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "fragmentation",
                    0,
                    MechanisticEventKind.FRAGMENTATION,
                    "Scissile bond cleavage separates the product fragments.",
                    0.78))));
  }

  private static List<ElementaryReactionStep> additionSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    return List.of(
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex,
            "encounter_complex",
            reaction.reactants(),
            reaction.reactants(),
            Math.max(1.0, forwardBarrierEnergy.value() * 0.20),
            Math.max(1.0, reverseBarrierEnergy.value() * 0.16),
            0.0,
            List.of(
                ReactionPathwayAnalyzer.event(
                    "diffusion_limited_encounter",
                    0,
                    MechanisticEventKind.DIFFUSION_LIMITED_ENCOUNTER,
                    "Reactants form a productive encounter complex before bond construction.",
                    0.72))),
        ReactionPathwayAnalyzer.step(
            reaction,
            firstStepIndex + 1,
            "addition_to_reactive_center",
            reaction.reactants(),
            reaction.products(),
            Math.max(3.0, forwardBarrierEnergy.value() * 0.80),
            Math.max(3.0, reverseBarrierEnergy.value() * 0.84),
            reactionEnergyValue.value(),
            List.of(
                ReactionPathwayAnalyzer.event(
                    "addition_to_multiple_bond",
                    0,
                    MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                    "New sigma connectivity forms at the unsaturated or electrophilic center.",
                    0.78),
                ReactionPathwayAnalyzer.event(
                    "bond_formation",
                    1,
                    MechanisticEventKind.BOND_FORMATION,
                    "Final bond formation places the product in its connected basin.",
                    0.76))));
  }

  private static List<ElementaryReactionStep> nucleophilicAromaticSubstitutionSteps(
      final Reaction reaction,
      final int firstStepIndex,
      final EnergyValue forwardBarrierEnergy,
      final EnergyValue reverseBarrierEnergy,
      final EnergyValue reactionEnergyValue) {
    final ArrayList<ElementaryReactionStep> steps = new ArrayList<ElementaryReactionStep>();
    steps.add(
        ElementaryReactionStep.of(
            "step_" + firstStepIndex + "_nucleophile_association_sigma_complex",
            ReactionPathwayAnalyzer.stepReaction(
                reaction,
                "nucleophile_association_sigma_complex",
                reaction.reactants(),
                reaction.reactants()),
            null,
            EnergyValue.of(
                Math.max(2.0, forwardBarrierEnergy.value() * 0.35), EnergyUnit.KILOJOULE_PER_MOLE),
            EnergyValue.of(
                Math.max(2.0, reverseBarrierEnergy.value() * 0.20), EnergyUnit.KILOJOULE_PER_MOLE),
            EnergyValue.of(
                Math.min(0.0, reactionEnergyValue.value() * 0.20), EnergyUnit.KILOJOULE_PER_MOLE),
            List.of(
                MechanisticEvent.of(
                    "event_nucleophilic_attack",
                    0,
                    MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                    "Heteroatom nucleophile attacks the polarized heteroaryl carbon bearing the leaving group.",
                    List.of(),
                    0.92),
                MechanisticEvent.of(
                    "event_sigma_complex_formation",
                    1,
                    MechanisticEventKind.SIGMA_COMPLEX_FORMATION,
                    "A Meisenheimer-like sigma complex/resonance-stabilized anionic intermediate is formed.",
                    List.of(),
                    0.88))));
    steps.add(
        ElementaryReactionStep.of(
            "step_" + (firstStepIndex + 1) + "_leaving_group_departure_rearomatization",
            ReactionPathwayAnalyzer.stepReaction(
                reaction,
                "leaving_group_departure_rearomatization",
                reaction.reactants(),
                reaction.products()),
            null,
            EnergyValue.of(
                Math.max(2.0, forwardBarrierEnergy.value() * 0.65), EnergyUnit.KILOJOULE_PER_MOLE),
            EnergyValue.of(
                Math.max(2.0, reverseBarrierEnergy.value() * 0.80), EnergyUnit.KILOJOULE_PER_MOLE),
            reactionEnergyValue,
            List.of(
                MechanisticEvent.of(
                    "event_leaving_group_departure",
                    0,
                    MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                    "Leaving group departs from the sigma complex.",
                    List.of(),
                    0.86),
                MechanisticEvent.of(
                    "event_rearomatization",
                    1,
                    MechanisticEventKind.RESONANCE_REORGANIZATION,
                    "Aromaticity is restored after substitution.",
                    List.of(),
                    0.84))));
    return List.copyOf(steps);
  }

  private static ElementaryReactionStep step(
      final Reaction reaction,
      final int stepIndex,
      final String suffix,
      final ReactionSide reactants,
      final ReactionSide products,
      final double forwardBarrierKilojoulePerMole,
      final double reverseBarrierKilojoulePerMole,
      final double reactionEnergyKilojoulePerMole,
      final List<MechanisticEvent> events) {
    return ElementaryReactionStep.of(
        "step_" + stepIndex + "_" + suffix,
        ReactionPathwayAnalyzer.stepReaction(reaction, suffix, reactants, products),
        null,
        EnergyValue.of(forwardBarrierKilojoulePerMole, EnergyUnit.KILOJOULE_PER_MOLE),
        EnergyValue.of(reverseBarrierKilojoulePerMole, EnergyUnit.KILOJOULE_PER_MOLE),
        EnergyValue.of(reactionEnergyKilojoulePerMole, EnergyUnit.KILOJOULE_PER_MOLE),
        events);
  }

  private static MechanisticEvent event(
      final String id,
      final int orderIndex,
      final MechanisticEventKind kind,
      final String description,
      final double probabilityWeight) {
    return MechanisticEvent.of(
        "event_" + id, orderIndex, kind, description, List.of(), probabilityWeight);
  }

  private static ElementaryReactionStep productRelaxationStep(
      final Reaction reaction, final int stepIndex, final String description) {
    return ElementaryReactionStep.of(
        "step_" + stepIndex + "_product_relaxation",
        ReactionPathwayAnalyzer.stepReaction(
            reaction, "product_relaxation", reaction.products(), reaction.products()),
        null,
        EnergyValue.of(2.0, EnergyUnit.KILOJOULE_PER_MOLE),
        EnergyValue.of(2.0, EnergyUnit.KILOJOULE_PER_MOLE),
        EnergyValue.of(0.0, EnergyUnit.KILOJOULE_PER_MOLE),
        List.of(
            MechanisticEvent.of(
                "event_product_relaxation",
                0,
                MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                description,
                List.of(),
                0.80)));
  }

  private static Reaction stepReaction(
      final Reaction overallReaction,
      final String suffix,
      final ReactionSide reactants,
      final ReactionSide products) {
    return Reaction.of(
        ReactionId.of(overallReaction.id().value() + "_" + suffix),
        overallReaction.displayName() + " / " + suffix,
        reactants,
        products,
        overallReaction.conditions(),
        ChemistryMetadata.EMPTY);
  }

  private static ReactionMechanismFamily mechanismFamily(
      final Reaction reaction, final ReactionBondChangeSet bondChanges) {
    final ReactionConditions conditions = reaction.conditions();
    if (ReactionPathwayAnalyzer.looksLikeNucleophilicAromaticSubstitution(reaction)) {
      return ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      return ReactionMechanismFamily.PHOTOCHEMICAL;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      return ReactionMechanismFamily.ELECTROCHEMICAL;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        || !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      return ReactionMechanismFamily.CATALYTIC_CYCLE;
    }
    if (bondChanges.formedBondEvents() > bondChanges.brokenBondEvents()) {
      return ReactionMechanismFamily.ADDITION;
    }
    if (bondChanges.brokenBondEvents() > bondChanges.formedBondEvents()) {
      return ReactionMechanismFamily.FRAGMENTATION;
    }
    return ReactionMechanismFamily.UNKNOWN;
  }

  private static boolean looksLikeNucleophilicAromaticSubstitution(final Reaction reaction) {
    if (!ReactionPathwayAnalyzer.hasSubstitutionActivation(reaction.conditions())) {
      return false;
    }
    if (!ReactionPathwayAnalyzer.sideHasAromaticNitrogenHeterocycle(reaction.reactants())) {
      return false;
    }
    if (!ReactionPathwayAnalyzer.sideHasCarbonHalogenSingleBond(reaction.reactants())) {
      return false;
    }
    return ReactionPathwayAnalyzer.sideBondSignatureCount(reaction.products(), "C-N:SINGLE")
        > ReactionPathwayAnalyzer.sideBondSignatureCount(reaction.reactants(), "C-N:SINGLE");
  }

  private static boolean isRadicalFamily(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.RADICAL_SUBSTITUTION
        || family == ReactionMechanismFamily.RADICAL_CHAIN
        || family == ReactionMechanismFamily.RADICAL_CYCLIZATION
        || family == ReactionMechanismFamily.POLYMERIZATION_CHAIN_GROWTH;
  }

  private static boolean isPericyclicFamily(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.PERICYCLIC
        || family == ReactionMechanismFamily.CYCLOADDITION
        || family == ReactionMechanismFamily.ELECTROCYCLIC
        || family == ReactionMechanismFamily.SIGMATROPIC
        || family == ReactionMechanismFamily.CLICK_CYCLOADDITION
        || family == ReactionMechanismFamily.DIELS_ALDER
        || family == ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION
        || family == ReactionMechanismFamily.CLAISEN_REARRANGEMENT
        || family == ReactionMechanismFamily.COPE_REARRANGEMENT
        || family == ReactionMechanismFamily.PAUSON_KHAND_REACTION;
  }

  private static boolean isAcidBaseOrCarbonylFamily(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.ACID_BASE
        || family == ReactionMechanismFamily.CARBONYL_ADDITION
        || family == ReactionMechanismFamily.CARBONYL_SUBSTITUTION
        || family == ReactionMechanismFamily.IMINE_ENAMINE_FORMATION
        || family == ReactionMechanismFamily.ENOLATE_ALDOL
        || family == ReactionMechanismFamily.MICHAEL_ADDITION
        || family == ReactionMechanismFamily.REDUCTIVE_AMINATION
        || family == ReactionMechanismFamily.ESTERIFICATION
        || family == ReactionMechanismFamily.TRANSESTERIFICATION
        || family == ReactionMechanismFamily.HYDROLYSIS
        || family == ReactionMechanismFamily.SOLVOLYSIS
        || family == ReactionMechanismFamily.EPOXIDE_OPENING
        || family == ReactionMechanismFamily.MANNICH_REACTION
        || family == ReactionMechanismFamily.STRECKER_SYNTHESIS
        || family == ReactionMechanismFamily.UGI_REACTION
        || family == ReactionMechanismFamily.PASSERINI_REACTION
        || family == ReactionMechanismFamily.CLAISEN_CONDENSATION
        || family == ReactionMechanismFamily.DIECKMANN_CONDENSATION
        || family == ReactionMechanismFamily.KNOEVENAGEL_CONDENSATION
        || family == ReactionMechanismFamily.HENRY_REACTION
        || family == ReactionMechanismFamily.ACETAL_KETAL_FORMATION
        || family == ReactionMechanismFamily.ACETAL_HYDROLYSIS;
  }

  private static boolean hasSubstitutionActivation(final ReactionConditions conditions) {
    return conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        || conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || !conditions.componentsByRole(ReactionComponentRole.BASE).isEmpty();
  }

  private static boolean sideHasAromaticNitrogenHeterocycle(final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (ReactionPathwayAnalyzer.moleculeHasAromaticNitrogenHeterocycle(
          participants.get(i).molecule())) {
        return true;
      }
    }
    return false;
  }

  private static boolean moleculeHasAromaticNitrogenHeterocycle(final Molecule molecule) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!molecule.aromaticity().isAromaticBond(bond)) {
        continue;
      }
      final String first = ReactionPathwayAnalyzer.elementSymbol(molecule, bond.firstAtomId());
      final String second = ReactionPathwayAnalyzer.elementSymbol(molecule, bond.secondAtomId());
      if ("N".equals(first) || "N".equals(second)) {
        return true;
      }
    }
    return false;
  }

  private static boolean sideHasCarbonHalogenSingleBond(final ReactionSide side) {
    return ReactionPathwayAnalyzer.sideBondSignatureCount(side, "C-Cl:SINGLE") > 0
        || ReactionPathwayAnalyzer.sideBondSignatureCount(side, "C-Br:SINGLE") > 0
        || ReactionPathwayAnalyzer.sideBondSignatureCount(side, "C-I:SINGLE") > 0
        || ReactionPathwayAnalyzer.sideBondSignatureCount(side, "C-F:SINGLE") > 0;
  }

  private static int sideBondSignatureCount(final ReactionSide side, final String signature) {
    final Integer count = ReactionPathwayAnalyzer.sideBondSignatures(side).get(signature);
    return count == null ? 0 : count.intValue();
  }

  private static boolean hasMediumOrganization(
      final ReactionConditions conditions, final double activationShift) {
    return conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        || conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)
        || activationShift != 0.0;
  }

  private static List<MechanisticEvent> mediumEvents(
      final ReactionConditions conditions, final double activationShift) {
    final ArrayList<MechanisticEvent> events = new ArrayList<MechanisticEvent>();
    if (ReactionPathwayAnalyzer.hasMediumOrganization(conditions, activationShift)) {
      events.add(
          MechanisticEvent.of(
              "event_medium_organization",
              0,
              MechanisticEventKind.SOLVATION_REORGANIZATION,
              "Reaction medium organizes reactants and changes the activation profile.",
              List.of(),
              0.72));
    }
    return List.copyOf(events);
  }

  private static List<MechanisticEvent> bondEvents(final ReactionBondChangeSet bondChanges) {
    final ArrayList<MechanisticEvent> events = new ArrayList<MechanisticEvent>();
    int order = 0;
    if (bondChanges.brokenBondEvents() > 0) {
      events.add(
          MechanisticEvent.of(
              "event_" + order,
              order,
              MechanisticEventKind.BOND_CLEAVAGE,
              "Cleaves "
                  + bondChanges.brokenBondEvents()
                  + " bond event(s) along the reaction coordinate.",
              List.of(),
              ReactionPathwayAnalyzer.eventWeight(bondChanges.brokenBondEvents())));
      ++order;
    }
    if (bondChanges.formedBondEvents() > 0) {
      events.add(
          MechanisticEvent.of(
              "event_" + order,
              order,
              MechanisticEventKind.BOND_FORMATION,
              "Forms "
                  + bondChanges.formedBondEvents()
                  + " bond event(s) toward the product complex.",
              List.of(),
              ReactionPathwayAnalyzer.eventWeight(bondChanges.formedBondEvents())));
      ++order;
    }
    return List.copyOf(events);
  }

  private static double eventWeight(final int eventCount) {
    if (eventCount <= 1) {
      return 0.90;
    }
    if (eventCount == 2) {
      return 0.82;
    }
    return 0.70;
  }

  private static List<MechanismEnergyPoint> energyProfile(
      final Reaction reaction,
      final ReactionBondChangeSet bondChanges,
      final ReactionMechanismFamily family,
      final double activationShift,
      final EnergyValue forwardBarrier,
      final EnergyValue reactionEnergy,
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId>
          productMoleculeIds) {
    final ArrayList<MechanismEnergyPoint> points = new ArrayList<MechanismEnergyPoint>();
    points.add(
        MechanismEnergyPoint.of(
            "reactant_complex",
            MechanismPointKind.REACTANT_COMPLEX,
            ReactionCoordinateValue.of(0.0),
            EnergyValue.of(0.0, EnergyUnit.KILOJOULE_PER_MOLE),
            null));
    if (ReactionPathwayAnalyzer.hasMediumOrganization(reaction.conditions(), activationShift)) {
      points.add(
          MechanismEnergyPoint.of(
              "organized_reactant_complex",
              MechanismPointKind.INTERMEDIATE,
              ReactionCoordinateValue.of(0.20),
              EnergyValue.of(-Math.max(1.0, activationShift * 0.25), EnergyUnit.KILOJOULE_PER_MOLE),
              null));
    }
    if (family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION) {
      points.add(
          MechanismEnergyPoint.of(
              "sigma_complex_intermediate",
              MechanismPointKind.INTERMEDIATE,
              ReactionCoordinateValue.of(0.45),
              EnergyValue.of(
                  Math.max(1.0, forwardBarrier.value() * 0.35), EnergyUnit.KILOJOULE_PER_MOLE),
              null));
      points.add(
          MechanismEnergyPoint.of(
              "leaving_group_departure_transition_state",
              MechanismPointKind.TRANSITION_STATE,
              ReactionCoordinateValue.of(0.68),
              EnergyValue.of(forwardBarrier.value(), EnergyUnit.KILOJOULE_PER_MOLE),
              null));
      points.add(
          MechanismEnergyPoint.of(
              "rearomatized_product_complex",
              MechanismPointKind.INTERMEDIATE,
              ReactionCoordinateValue.of(0.84),
              EnergyValue.of(
                  reactionEnergy.value() + Math.max(1.0, forwardBarrier.value() * 0.08),
                  EnergyUnit.KILOJOULE_PER_MOLE),
              null));
      points.add(
          MechanismEnergyPoint.ofMany(
              "product_complex",
              MechanismPointKind.PRODUCT_COMPLEX,
              ReactionCoordinateValue.of(1.0),
              reactionEnergy,
              productMoleculeIds));
      return List.copyOf(points);
    }
    if (ReactionPathwayAnalyzer.hasFamilySpecificProfile(family)) {
      final String familyId = family.name().toLowerCase(Locale.ROOT);
      points.add(
          MechanismEnergyPoint.of(
              familyId + "_activation_complex",
              MechanismPointKind.INTERMEDIATE,
              ReactionCoordinateValue.of(0.32),
              EnergyValue.of(
                  Math.max(1.0, forwardBarrier.value() * 0.28), EnergyUnit.KILOJOULE_PER_MOLE),
              null));
      points.add(
          MechanismEnergyPoint.of(
              familyId + "_productive_transition_state",
              MechanismPointKind.TRANSITION_STATE,
              ReactionCoordinateValue.of(0.62),
              forwardBarrier,
              null));
      points.add(
          MechanismEnergyPoint.of(
              familyId + "_post_turnover_complex",
              MechanismPointKind.INTERMEDIATE,
              ReactionCoordinateValue.of(0.82),
              EnergyValue.of(
                  reactionEnergy.value() + Math.max(1.0, forwardBarrier.value() * 0.12),
                  EnergyUnit.KILOJOULE_PER_MOLE),
              null));
      points.add(
          MechanismEnergyPoint.ofMany(
              "product_complex",
              MechanismPointKind.PRODUCT_COMPLEX,
              ReactionCoordinateValue.of(1.0),
              reactionEnergy,
              productMoleculeIds));
      return List.copyOf(points);
    }
    final double transitionCoordinate =
        ReactionPathwayAnalyzer.hasMediumOrganization(reaction.conditions(), activationShift)
            ? 0.55
            : 0.50;
    points.add(
        MechanismEnergyPoint.of(
            "transition_state_0",
            MechanismPointKind.TRANSITION_STATE,
            ReactionCoordinateValue.of(transitionCoordinate),
            forwardBarrier,
            null));
    if (bondChanges.brokenBondEvents() > 0 && bondChanges.formedBondEvents() > 0) {
      points.add(
          MechanismEnergyPoint.of(
              "post_transition_intermediate",
              MechanismPointKind.INTERMEDIATE,
              ReactionCoordinateValue.of(0.78),
              EnergyValue.of(
                  reactionEnergy.value() + Math.max(2.0, forwardBarrier.value() * 0.10),
                  EnergyUnit.KILOJOULE_PER_MOLE),
              null));
    }
    points.add(
        MechanismEnergyPoint.ofMany(
            "product_complex",
            MechanismPointKind.PRODUCT_COMPLEX,
            ReactionCoordinateValue.of(1.0),
            reactionEnergy,
            productMoleculeIds));
    return List.copyOf(points);
  }

  private static List<ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId>
      sideMoleculeIds(final ReactionSide side) {
    final ArrayList<ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId> ids =
        new ArrayList<ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId>(
            side.participantCount());
    for (int i = 0; i < side.participants().size(); ++i) {
      ids.add(side.participants().get(i).molecule().id());
    }
    return List.copyOf(ids);
  }

  private static boolean hasFamilySpecificProfile(final ReactionMechanismFamily family) {
    return family == ReactionMechanismFamily.CATALYTIC_CYCLE
        || family == ReactionMechanismFamily.PHOTOCHEMICAL
        || family == ReactionMechanismFamily.ELECTROCHEMICAL
        || family == ReactionMechanismFamily.ADDITION
        || family == ReactionMechanismFamily.FRAGMENTATION
        || ReactionPathwayAnalyzer.isRadicalFamily(family)
        || ReactionPathwayAnalyzer.isPericyclicFamily(family)
        || ReactionPathwayAnalyzer.isAcidBaseOrCarbonylFamily(family);
  }

  private static String pathwayBand(final double forwardBarrier, final double activationShift) {
    if (forwardBarrier <= 20.0 && activationShift > 0.0) {
      return "activated_fast";
    }
    if (forwardBarrier <= 45.0) {
      return "accessible";
    }
    if (forwardBarrier <= 80.0) {
      return "demanding";
    }
    return "unlikely";
  }
}
