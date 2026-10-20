/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionKineticEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ArrheniusParameters;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.EyringParameters;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.RateConstant;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.RateConstantUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.EntropyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.EntropyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;

/**
 * Считает кинетические величины из оцененного реакционного пути и экспериментальных условий.
 */
public final class ReactionKineticsModel {

  private static final double DEFAULT_UNIMOLECULAR_PRE_EXPONENTIAL = 1.0E12;
  private static final double DEFAULT_ACTIVATION_ENTROPY_JOULE_PER_MOLE_KELVIN = -50.0;
  private static final double LOW_POLARITY_SOLVENT_FACTOR = 0.75;
  private static final double POLAR_SOLVENT_FACTOR = 1.18;
  private static final double PROTIC_SOLVENT_FACTOR = 0.82;
  private static final double VISCOUS_POLYETHER_FACTOR = 0.88;
  private static final double MASS_QUANTIFIED_RECIPE_FACTOR = 1.08;
  private static final double CATALYST_FACTOR = 4.0;
  private static final double MICROWAVE_FACTOR = 2.2;
  private static final double FLOW_FACTOR = 1.25;

  public ReactionKineticEstimate estimate(
      final ReactionPathwayEstimate pathwayEstimate, final ReactionConditions conditions) {
    if (pathwayEstimate == null) {
      throw new IllegalArgumentException("Reaction kinetics pathway estimate must not be null.");
    }
    if (conditions == null) {
      throw new IllegalArgumentException("Reaction kinetics conditions must not be null.");
    }
    final Temperature temperature =
        Temperature.of(pathwayEstimate.temperatureKelvin(), TemperatureUnit.KELVIN);
    final EnergyValue forwardBarrier =
        EnergyValue.of(pathwayEstimate.forwardBarrier(), EnergyUnit.KILOJOULE_PER_MOLE);
    final KineticContext context =
        ReactionKineticsModel.kineticContext(pathwayEstimate, conditions);
    final ArrheniusParameters arrhenius =
        ArrheniusParameters.of(
            context.preExponentialFactor, RateConstantUnit.PER_SECOND, forwardBarrier);
    final EyringParameters eyring =
        EyringParameters.of(
            forwardBarrier,
            EntropyValue.of(context.activationEntropy, EntropyUnit.JOULE_PER_MOLE_KELVIN));
    final RateConstant arrheniusRate = arrhenius.rateConstantAt(temperature);
    final RateConstant eyringRate = eyring.rateConstantAt(temperature);
    return ReactionKineticEstimate.of(
        pathwayEstimate.forwardBarrier(),
        pathwayEstimate.reverseBarrier(),
        pathwayEstimate.forwardRate(),
        pathwayEstimate.reverseRate(),
        ReactionKineticsModel.positiveRateRatio(pathwayEstimate.forwardReverseRateRatio()),
        arrhenius.preExponentialFactor(),
        arrhenius.unit().name(),
        context.model,
        context.description,
        arrheniusRate.value(),
        context.activationEntropy,
        eyringRate.value(),
        Math.log(2.0) / Math.max(1.0E-300, arrheniusRate.value()),
        ReactionKineticsModel.durationConversion(conditions, arrheniusRate.value()));
  }

  private static double positiveRateRatio(final double ratio) {
    if (!Double.isFinite(ratio)) {
      return ratio > 0.0 ? Double.MAX_VALUE : Double.MIN_NORMAL;
    }
    return Math.max(Double.MIN_NORMAL, ratio);
  }

  private static KineticContext kineticContext(
      final ReactionPathwayEstimate pathwayEstimate, final ReactionConditions conditions) {
    double preExponential = DEFAULT_UNIMOLECULAR_PRE_EXPONENTIAL;
    double activationEntropy = DEFAULT_ACTIVATION_ENTROPY_JOULE_PER_MOLE_KELVIN;
    final StringBuilder description = new StringBuilder(96);
    description.append("base=unimolecular_transition_state");
    if (pathwayEstimate.hasMechanism()) {
      final ReactionMechanismFamily family = pathwayEstimate.mechanism().family();
      final int eventCount = pathwayEstimate.mechanism().eventCount();
      preExponential *= ReactionKineticsModel.mechanismPreExponentialFactor(family, eventCount);
      activationEntropy += ReactionKineticsModel.mechanismEntropyShift(family, eventCount);
      description.append(";mechanism=").append(family.name());
      description.append(";events=").append(eventCount);
    } else {
      description.append(";mechanism=none");
    }
    final double conditionFactor = ReactionKineticsModel.conditionPreExponentialFactor(conditions);
    final double solventEntropyShift = ReactionKineticsModel.solventEntropyShift(conditions);
    preExponential *= conditionFactor;
    activationEntropy += solventEntropyShift;
    description.append(";condition_factor=").append(conditionFactor);
    description.append(";solvent_entropy_shift=").append(solventEntropyShift);
    return new KineticContext(
        Math.max(1.0E3, Math.min(1.0E16, preExponential)),
        Math.max(-220.0, Math.min(120.0, activationEntropy)),
        "contextual_arrhenius_eyring",
        description.toString());
  }

  private static double mechanismPreExponentialFactor(
      final ReactionMechanismFamily family, final int eventCount) {
    double factor = 1.0;
    if (family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION
        || family == ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION) {
      factor *= 0.45;
    } else if (family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING) {
      factor *= 0.25;
    } else if (family == ReactionMechanismFamily.PHOTOCHEMICAL
        || family == ReactionMechanismFamily.ELECTROCHEMICAL) {
      factor *= 1.55;
    }
    if (eventCount > 1) {
      factor *= Math.max(0.12, Math.pow(0.72, Math.min(eventCount - 1, 8)));
    }
    return factor;
  }

  private static double mechanismEntropyShift(
      final ReactionMechanismFamily family, final int eventCount) {
    double shift = 0.0;
    if (family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION
        || family == ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2) {
      shift -= 35.0;
    } else if (family == ReactionMechanismFamily.SUZUKI_COUPLING
        || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
        || family == ReactionMechanismFamily.NEGISHI_COUPLING
        || family == ReactionMechanismFamily.STILLE_COUPLING
        || family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING
        || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION) {
      shift -= 55.0;
    } else if (family == ReactionMechanismFamily.PHOTOCHEMICAL) {
      shift += 15.0;
    }
    return shift - 6.0 * Math.max(0, Math.min(eventCount - 1, 8));
  }

  private static double conditionPreExponentialFactor(final ReactionConditions conditions) {
    double factor = 1.0;
    if (conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        || !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      factor *= CATALYST_FACTOR;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.MICROWAVE)) {
      factor *= MICROWAVE_FACTOR;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.FLOW_REACTOR)) {
      factor *= FLOW_FACTOR;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)) {
      factor *= 0.55;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)) {
      factor *= 1.12;
    }
    if (ReactionKineticsModel.hasMassQuantifiedReactants(conditions)) {
      factor *= MASS_QUANTIFIED_RECIPE_FACTOR;
    }
    return factor * ReactionKineticsModel.solventPreExponentialFactor(conditions);
  }

  private static double solventPreExponentialFactor(final ReactionConditions conditions) {
    final List<ReactionConditionComponent> solvents =
        conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    if (solvents.isEmpty()) {
      return 1.0;
    }
    double factor = 1.0;
    for (int i = 0; i < solvents.size(); ++i) {
      final String name = solvents.get(i).name().toLowerCase(java.util.Locale.ROOT);
      if (name.contains("dmf")
          || name.contains("dmso")
          || name.contains("nmp")
          || name.contains("peg")
          || name.contains("acetonitrile")
          || name.contains("methanol")
          || name.contains("ethanol")
          || name.contains("water")) {
        factor *= POLAR_SOLVENT_FACTOR;
      } else if (name.contains("toluene")
          || name.contains("benzene")
          || name.contains("hexane")
          || name.contains("dioxane")) {
        factor *= LOW_POLARITY_SOLVENT_FACTOR;
      }
      if (name.contains("water")
          || name.contains("methanol")
          || name.contains("ethanol")
          || name.contains("propanol")) {
        factor *= PROTIC_SOLVENT_FACTOR;
      }
      if (ReactionKineticsModel.polyetherOrPolyolMedium(name)) {
        factor *= VISCOUS_POLYETHER_FACTOR;
      }
    }
    return Math.max(0.25, Math.min(2.5, factor));
  }

  private static boolean hasMassQuantifiedReactants(final ReactionConditions conditions) {
    final List<ReactionConditionComponent> reactants =
        conditions.componentsByRole(ReactionComponentRole.REACTANT);
    for (int i = 0; i < reactants.size(); ++i) {
      final ReactionConditionComponent component = reactants.get(i);
      if (component.hasAmount() && component.amount().massQuantity()) {
        return true;
      }
    }
    return false;
  }

  private static boolean polyetherOrPolyolMedium(final String name) {
    return name.contains("peg")
        || name.contains("polyethylene glycol")
        || name.contains("poly(ethylene glycol)")
        || name.contains("polyol")
        || name.contains("polyether")
        || name.contains("glyme")
        || name.contains("diglyme")
        || name.contains("triglyme")
        || name.contains("tetraglyme");
  }

  private static double solventEntropyShift(final ReactionConditions conditions) {
    final List<ReactionConditionComponent> solvents =
        conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    double shift = 0.0;
    for (int i = 0; i < solvents.size(); ++i) {
      final String name = solvents.get(i).name().toLowerCase(java.util.Locale.ROOT);
      if (name.contains("peg")
          || name.contains("dmso")
          || name.contains("dmf")
          || name.contains("nmp")) {
        shift += 10.0;
      }
      if (name.contains("water")
          || name.contains("methanol")
          || name.contains("ethanol")
          || name.contains("propanol")) {
        shift -= 12.0;
      }
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)) {
      shift += 6.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ADDITION_RATE)) {
      shift += 4.0;
    }
    return shift;
  }

  private static Double durationConversion(
      final ReactionConditions conditions, final double rateConstant) {
    if (!conditions.hasDuration()) {
      return null;
    }
    return Double.valueOf(1.0 - Math.exp(-rateConstant * conditions.duration().seconds()));
  }

  private static final class KineticContext {

    private final double preExponentialFactor;
    private final double activationEntropy;
    private final String model;
    private final String description;

    private KineticContext(
        final double preExponentialFactor,
        final double activationEntropy,
        final String model,
        final String description) {
      this.preExponentialFactor = preExponentialFactor;
      this.activationEntropy = activationEntropy;
      this.model = model;
      this.description = description;
    }
  }
}
