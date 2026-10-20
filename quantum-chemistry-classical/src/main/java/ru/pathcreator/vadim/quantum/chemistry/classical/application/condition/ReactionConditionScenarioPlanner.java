/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricEquivalent;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;

/**
 * Строит небольшую, но химически осмысленную матрицу условий для сравнения прогноза реакции.
 */
public final class ReactionConditionScenarioPlanner {

  private static final double ROOM_TEMPERATURE_CELSIUS = 25.0;
  private static final double HEATED_TEMPERATURE_CELSIUS = 80.0;
  private static final double HOT_HETEROARYL_TEMPERATURE_CELSIUS = 120.0;
  private static final double COOLED_TEMPERATURE_CELSIUS = 0.0;
  private final ReactionStoichiometryAnalyzer stoichiometryAnalyzer;

  public ReactionConditionScenarioPlanner() {
    this(new ReactionStoichiometryAnalyzer());
  }

  public ReactionConditionScenarioPlanner(
      final ReactionStoichiometryAnalyzer stoichiometryAnalyzer) {
    if (stoichiometryAnalyzer == null) {
      throw new IllegalArgumentException("Reaction stoichiometry analyzer must not be null.");
    }
    this.stoichiometryAnalyzer = stoichiometryAnalyzer;
  }

  public ReactionConditionScenarioPlan plan(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final ArrayList<ReactionConditionScenario> scenarios =
        new ArrayList<ReactionConditionScenario>(24);
    final ReactionConditions baseline = request.conditions();
    final ReactionStoichiometryProfile stoichiometry = this.stoichiometryAnalyzer.analyze(request);
    this.addScenario(
        scenarios,
        request.id(),
        ReactionConditionScenarioKind.BASELINE,
        baseline,
        "Preserve the exact user-specified reaction conditions.");
    this.addTemperatureScenarios(scenarios, request.id(), baseline);
    this.addAtmosphereScenario(scenarios, request.id(), baseline);
    this.addCatalystSignalScenario(scenarios, request.id(), baseline);
    this.addAcidBaseSplitScenarios(scenarios, request.id(), baseline);
    this.addAcidBaseControlScenarios(scenarios, request.id(), baseline);
    this.addRedoxAndActivationScenarios(scenarios, request.id(), baseline);
    this.addMoistureAndDehydrationScenarios(scenarios, request.id(), baseline);
    this.addSolventClassScenarios(scenarios, request.id(), baseline);
    this.addOperationalModeScenarios(scenarios, request.id(), baseline, stoichiometry);
    this.addConcentrationRegimeScenarios(scenarios, request.id(), baseline);
    this.addEquivalentSweepScenarios(scenarios, request.id(), baseline, stoichiometry);
    this.addDurationScenarios(scenarios, request.id(), baseline);
    return ReactionConditionScenarioPlan.of(request.id(), scenarios);
  }

  private void addTemperatureScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    if (baseline.temperature() == null
        || Math.abs(baseline.temperature().kelvin() - 298.15) > 10.0) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.ROOM_TEMPERATURE,
          ReactionConditionScenarioPlanner.withTemperature(
              baseline,
              Temperature.of(ROOM_TEMPERATURE_CELSIUS, TemperatureUnit.CELSIUS),
              ReactionConditionScenarioPlanner.withoutFeature(
                  baseline.features(),
                  ReactionConditionFeatureKind.COOLING,
                  ReactionConditionFeatureKind.HEATING)),
          "Compare against a room-temperature condition point.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.HEATING)
        || baseline.temperature() == null
        || baseline.temperature().kelvin() < 323.15) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.HEATED,
          ReactionConditionScenarioPlanner.withTemperature(
              baseline,
              Temperature.of(HEATED_TEMPERATURE_CELSIUS, TemperatureUnit.CELSIUS),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.HEATING)),
          "Check whether thermal activation changes ranking or feasibility.");
    }
    if (baseline.temperature() == null || baseline.temperature().kelvin() < 373.15) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.REFLUX_CONDITION,
          ReactionConditionScenarioPlanner.withTemperature(
              baseline,
              Temperature.of(HOT_HETEROARYL_TEMPERATURE_CELSIUS, TemperatureUnit.CELSIUS),
              ReactionConditionScenarioPlanner.withFeature(
                  ReactionConditionScenarioPlanner.withFeature(
                      baseline.features(), ReactionConditionFeatureKind.HEATING),
                  ReactionConditionFeatureKind.REFLUX)),
          "Probe high-boiling thermal window often needed for slow heteroaryl substitutions and condensations.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.COOLING)
        || baseline.temperature() == null
        || baseline.temperature().kelvin() > 283.15) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.COOLED,
          ReactionConditionScenarioPlanner.withTemperature(
              baseline,
              Temperature.of(COOLED_TEMPERATURE_CELSIUS, TemperatureUnit.CELSIUS),
              ReactionConditionScenarioPlanner.withFeature(
                  ReactionConditionScenarioPlanner.withoutFeature(
                      baseline.features(), ReactionConditionFeatureKind.HEATING),
                  ReactionConditionFeatureKind.COOLING)),
          "Check whether low-temperature control changes selectivity or suppresses side paths.");
    }
  }

  private void addAcidBaseControlScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    if (!baseline.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        && baseline.componentsByRole(ReactionComponentRole.BASE).isEmpty()) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.MILD_BASE_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.BASE,
              "mild organic base screening point",
              StoichiometricEquivalent.of(1.2),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.BASIC_MEDIUM)),
          "Probe mild base promotion for deprotonation, amine activation and acid scavenging without forcing a strong-base path.");
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.STRONG_BASE_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.BASE,
              "strong non-nucleophilic base screening point",
              StoichiometricEquivalent.of(1.5),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.BASIC_MEDIUM)),
          "Probe strong-base selectivity and elimination/overreaction risk when weak base activation is insufficient.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        && baseline.componentsByRole(ReactionComponentRole.ACID).isEmpty()) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.ACID_SCAVENGER_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.ADDITIVE,
              "acid scavenger buffer screening point",
              StoichiometricEquivalent.of(1.1),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.BASIC_MEDIUM)),
          "Probe whether trapping generated acid improves conversion or suppresses protonation-driven side paths.");
    }
  }

  private void addRedoxAndActivationScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    if (!baseline.hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)
        && !ReactionConditionScenarioPlanner.hasComponentSignal(
            baseline,
            "oxid",
            "peroxide",
            "mcpba",
            "nbs",
            "kmno4",
            "chrom",
            "pcc",
            "dess",
            "martin")) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.OXIDANT_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.ADDITIVE,
              "oxidant screening point",
              StoichiometricEquivalent.of(1.1),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.OXIDIZING_MEDIUM)),
          "Probe oxidative branch for alcohol, aldehyde, heteroatom and radical-sensitive product distributions.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM)
        && !ReactionConditionScenarioPlanner.hasComponentSignal(
            baseline,
            "reduc",
            "hydride",
            "borohydride",
            "lialh4",
            "dibal",
            "samarium",
            "tin hydride")) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.REDUCTANT_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.ADDITIVE,
              "reductant screening point",
              StoichiometricEquivalent.of(1.1),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.REDUCING_MEDIUM)),
          "Probe reductive branch for imine, carbonyl, nitrile, nitro, radical and dehalogenation-sensitive routes.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.HYDROGENATION)
        && !ReactionConditionScenarioPlanner.hasComponentSignal(
            baseline, "hydrogen", "h2", "palladium", "platinum", "raney", "nickel")) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.HYDROGENATION_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.CATALYST,
              "hydrogenation catalyst screening point",
              null,
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.HYDROGENATION)),
          "Probe catalytic hydrogenation for alkene, alkyne, nitro, nitrile, imine and dehalogenation side paths.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.PHOTOCHEMICAL_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.CATALYST,
              "photoredox catalyst screening point",
              null,
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.PHOTOCHEMICAL)),
          "Probe photochemical or photoredox branch for radical, electron-transfer and C-H functionalization hypotheses.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.ELECTROCHEMICAL_CONTROL,
          ReactionConditionScenarioPlanner.withNamedAdditive(
              ReactionConditionScenarioPlanner.withFeatures(
                  baseline,
                  ReactionConditionScenarioPlanner.withFeature(
                      baseline.features(), ReactionConditionFeatureKind.ELECTROCHEMICAL)),
              "electrolyte screening point",
              MolarConcentration.of(0.1, MolarConcentrationUnit.MOLE_PER_LITER)),
          "Probe electrochemical oxidation/reduction branch without committing to a concrete electrode setup.");
    }
  }

  private void addMoistureAndDehydrationScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    if (!baseline.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.MOISTURE_CONTROL,
          ReactionConditionScenarioPlanner.withFeatures(
              baseline,
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.ANHYDROUS)),
          "Probe dry-condition control for acyl, imine, enolate, nitrile, organometallic and heteroaryl-sensitive pathways.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.DEHYDRATING_AGENT)
        && !ReactionConditionScenarioPlanner.hasComponentSignal(
            baseline, "molecular sieve", "dehydrat", "p2o5", "thionyl", "ms 4a", "sieves")) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.DEHYDRATION_CONTROL,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.ADDITIVE,
              "dehydrating agent screening point",
              null,
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.DEHYDRATING_AGENT)),
          "Probe water-removal branch for condensations, imine formation, acetalization and hydrolysis-sensitive equilibria.");
    }
  }

  private void addAtmosphereScenario(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    if (baseline.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        && baseline.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)) {
      return;
    }
    List<ReactionConditionFeature> features =
        ReactionConditionScenarioPlanner.withFeature(
            baseline.features(), ReactionConditionFeatureKind.ANHYDROUS);
    features =
        ReactionConditionScenarioPlanner.withFeature(
            features, ReactionConditionFeatureKind.INERT_ATMOSPHERE);
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.INERT_ANHYDROUS,
        ReactionConditionScenarioPlanner.withFeatures(baseline, features),
        "Add inert and anhydrous controls for moisture- or oxygen-sensitive organic routes.");
  }

  private void addCatalystSignalScenario(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    final List<ReactionConditionComponent> catalysts =
        baseline.componentsByRole(ReactionComponentRole.CATALYST);
    if (catalysts.isEmpty() || baseline.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)) {
      return;
    }
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.CATALYST_SIGNAL,
        ReactionConditionScenarioPlanner.withFeatures(
            baseline,
            ReactionConditionScenarioPlanner.withFeature(
                baseline.features(), ReactionConditionFeatureKind.CATALYST_PRESENT)),
        "Make the catalyst signal explicit so rule scoring and workflow diagnostics agree.");
  }

  private void addAcidBaseSplitScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    if (!baseline.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || !baseline.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      return;
    }
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.ACID_ONLY,
        ReactionConditionScenarioPlanner.withFeatures(
            baseline,
            ReactionConditionScenarioPlanner.withoutFeature(
                baseline.features(), ReactionConditionFeatureKind.BASIC_MEDIUM)),
        "Resolve contradictory acid/base setup by checking the acid-only branch.");
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.BASE_ONLY,
        ReactionConditionScenarioPlanner.withFeatures(
            baseline,
            ReactionConditionScenarioPlanner.withoutFeature(
                baseline.features(), ReactionConditionFeatureKind.ACIDIC_MEDIUM)),
        "Resolve contradictory acid/base setup by checking the base-only branch.");
  }

  private void addDurationScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    final double baselineHours = baseline.hasDuration() ? baseline.duration().hours() : 8.0;
    if (baselineHours > 0.5) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.SHORT_DURATION,
          ReactionConditionScenarioPlanner.withDuration(
              baseline, TimeDuration.of(0.5, TimeDurationUnit.HOUR)),
          "Compare a short reaction time to detect fast kinetic selectivity.");
    }
    if (baselineHours < 24.0) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.LONG_DURATION,
          ReactionConditionScenarioPlanner.withDuration(
              baseline, TimeDuration.of(24.0, TimeDurationUnit.HOUR)),
          "Compare an extended reaction time for slow conversion or side-product growth.");
    }
  }

  private void addSolventClassScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.POLAR_APROTIC_SOLVENT,
        ReactionConditionScenarioPlanner.withNamedRoleComponent(
            baseline, ReactionComponentRole.SOLVENT, "polar aprotic solvent class"),
        "Probe polar aprotic solvation for nucleophile strength, ion separation and heteroaryl substitution.");
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.POLAR_PROTIC_SOLVENT,
        ReactionConditionScenarioPlanner.withNamedRoleComponent(
            baseline, ReactionComponentRole.SOLVENT, "polar protic solvent class"),
        "Probe protic solvation for proton-transfer, solvolysis and ionic intermediate stabilization.");
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.NONPOLAR_SOLVENT,
        ReactionConditionScenarioPlanner.withNamedRoleComponent(
            baseline, ReactionComponentRole.SOLVENT, "nonpolar solvent class"),
        "Probe nonpolar solvent control for neutral pathways, aggregation and solubility limits.");
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.POLYETHER_HIGH_BOILING_SOLVENT,
        ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
            baseline,
            ReactionComponentRole.SOLVENT,
            "polyether high-boiling solvent class",
            null,
            ReactionConditionScenarioPlanner.withFeature(
                baseline.features(), ReactionConditionFeatureKind.HEATING)),
        "Probe PEG/polyether-like high-boiling medium for ion pairing, viscosity, mass transfer and sustained heterocycle activation.");
  }

  private void addOperationalModeScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline,
      final ReactionStoichiometryProfile stoichiometry) {
    if (!baseline.hasFeature(ReactionConditionFeatureKind.SEALED_VESSEL)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.SEALED_HEATED_CONDITION,
          ReactionConditionScenarioPlanner.withTemperature(
              baseline,
              Temperature.of(HOT_HETEROARYL_TEMPERATURE_CELSIUS, TemperatureUnit.CELSIUS),
              ReactionConditionScenarioPlanner.withFeature(
                  ReactionConditionScenarioPlanner.withFeature(
                      baseline.features(), ReactionConditionFeatureKind.HEATING),
                  ReactionConditionFeatureKind.SEALED_VESSEL)),
          "Probe sealed heating for volatile reagents, pressure-sensitive conversion and high-temperature solvent windows.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.MICROWAVE)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.MICROWAVE_HEATED_CONDITION,
          ReactionConditionScenarioPlanner.withTemperature(
              baseline,
              Temperature.of(HOT_HETEROARYL_TEMPERATURE_CELSIUS, TemperatureUnit.CELSIUS),
              ReactionConditionScenarioPlanner.withFeature(
                  ReactionConditionScenarioPlanner.withFeature(
                      baseline.features(), ReactionConditionFeatureKind.HEATING),
                  ReactionConditionFeatureKind.MICROWAVE)),
          "Probe microwave-style thermal profile for polar media and slow heteroaryl or condensation pathways.");
    }
    if (!baseline.hasFeature(ReactionConditionFeatureKind.FLOW_REACTOR)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.FLOW_CONDITION,
          ReactionConditionScenarioPlanner.withFeatures(
              baseline,
              ReactionConditionScenarioPlanner.withFeature(
                  ReactionConditionScenarioPlanner.withFeature(
                      baseline.features(), ReactionConditionFeatureKind.FLOW_REACTOR),
                  ReactionConditionFeatureKind.ADDITION_RATE)),
          "Probe flow/addition-rate control for heat transfer, local concentration and unstable intermediate management.");
    }
    if (stoichiometry.totalGramScale() >= 20.0) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.LARGE_SCALE_MASS_TRANSFER,
          ReactionConditionScenarioPlanner.withFeatures(
              baseline,
              ReactionConditionScenarioPlanner.withFeature(
                  ReactionConditionScenarioPlanner.withFeature(
                      baseline.features(), ReactionConditionFeatureKind.ADDITION_RATE),
                  ReactionConditionFeatureKind.FLOW_REACTOR)),
          "Probe scale-up mass-transfer control because quantified recipe inputs indicate a gram-scale reaction.");
    }
  }

  private void addConcentrationRegimeScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline) {
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.DILUTE_REGIME,
        ReactionConditionScenarioPlanner.withNamedAdditive(
            baseline,
            "high dilution regime",
            MolarConcentration.of(0.01, MolarConcentrationUnit.MOLE_PER_LITER)),
        "Probe high dilution to suppress intermolecular side paths and favor intramolecular closure.");
    this.addScenario(
        scenarios,
        requestId,
        ReactionConditionScenarioKind.CONCENTRATED_REGIME,
        ReactionConditionScenarioPlanner.withNamedAdditive(
            baseline,
            "concentrated regime",
            MolarConcentration.of(2.0, MolarConcentrationUnit.MOLE_PER_LITER)),
        "Probe concentrated conditions for collision-driven conversion and oligomer or polymer side risk.");
  }

  private void addEquivalentSweepScenarios(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditions baseline,
      final ReactionStoichiometryProfile stoichiometry) {
    if (!stoichiometry.hasEquivalentExcess(1.25)) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.NUCLEOPHILE_EXCESS,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.REACTANT,
              "nucleophile excess screening point",
              StoichiometricEquivalent.of(1.5),
              baseline.features()),
          "Probe nucleophile excess for conversion of electrophilic sites and incomplete-substitution suppression.");
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.ELECTROPHILE_EXCESS,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.REACTANT,
              "electrophile excess screening point",
              StoichiometricEquivalent.of(1.2),
              baseline.features()),
          "Probe electrophile excess to expose overreaction, bis-substitution and limiting-reagent sensitivity.");
    }
    if (baseline.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || !baseline.componentsByRole(ReactionComponentRole.BASE).isEmpty()) {
      this.addScenario(
          scenarios,
          requestId,
          ReactionConditionScenarioKind.BASE_EXCESS,
          ReactionConditionScenarioPlanner.withRoleComponentAndFeatures(
              baseline,
              ReactionComponentRole.BASE,
              "base excess screening point",
              StoichiometricEquivalent.of(2.0),
              ReactionConditionScenarioPlanner.withFeature(
                  baseline.features(), ReactionConditionFeatureKind.BASIC_MEDIUM)),
          "Probe base excess for conversion acceleration, deprotonation state and competing elimination or hydrolysis risk.");
    }
  }

  private void addScenario(
      final List<ReactionConditionScenario> scenarios,
      final String requestId,
      final ReactionConditionScenarioKind kind,
      final ReactionConditions conditions,
      final String rationale) {
    scenarios.add(
        ReactionConditionScenario.of(
            requestId + "/" + kind.name().toLowerCase(Locale.ROOT), kind, conditions, rationale));
  }

  private static ReactionConditions withTemperature(
      final ReactionConditions conditions,
      final Temperature temperature,
      final List<ReactionConditionFeature> features) {
    return ReactionConditions.of(
        temperature,
        conditions.pressure(),
        conditions.duration(),
        conditions.components(),
        features,
        conditions.note());
  }

  private static ReactionConditions withDuration(
      final ReactionConditions conditions, final TimeDuration duration) {
    return ReactionConditions.of(
        conditions.temperature(),
        conditions.pressure(),
        duration,
        conditions.components(),
        conditions.features(),
        conditions.note());
  }

  private static ReactionConditions withFeatures(
      final ReactionConditions conditions, final List<ReactionConditionFeature> features) {
    return ReactionConditions.of(
        conditions.temperature(),
        conditions.pressure(),
        conditions.duration(),
        conditions.components(),
        features,
        conditions.note());
  }

  private static ReactionConditions withNamedRoleComponent(
      final ReactionConditions conditions, final ReactionComponentRole role, final String name) {
    final ArrayList<ReactionConditionComponent> components =
        new ArrayList<ReactionConditionComponent>(conditions.components().size() + 1);
    for (int i = 0; i < conditions.components().size(); ++i) {
      final ReactionConditionComponent component = conditions.components().get(i);
      if (component.role() != role) {
        components.add(component);
      }
    }
    components.add(ReactionConditionComponent.named(role, name));
    return ReactionConditions.of(
        conditions.temperature(),
        conditions.pressure(),
        conditions.duration(),
        components,
        conditions.features(),
        conditions.note());
  }

  private static ReactionConditions withNamedAdditive(
      final ReactionConditions conditions,
      final String name,
      final MolarConcentration concentration) {
    final ArrayList<ReactionConditionComponent> components =
        new ArrayList<ReactionConditionComponent>(conditions.components().size() + 1);
    for (int i = 0; i < conditions.components().size(); ++i) {
      final ReactionConditionComponent component = conditions.components().get(i);
      if (!name.equals(component.name())) {
        components.add(component);
      }
    }
    components.add(
        ReactionConditionComponent.of(
            ReactionComponentRole.ADDITIVE,
            name,
            null,
            concentration,
            null,
            "Scenario marker for concentration screening."));
    return ReactionConditions.of(
        conditions.temperature(),
        conditions.pressure(),
        conditions.duration(),
        components,
        conditions.features(),
        conditions.note());
  }

  private static ReactionConditions withRoleComponentAndFeatures(
      final ReactionConditions conditions,
      final ReactionComponentRole role,
      final String name,
      final StoichiometricEquivalent equivalent,
      final List<ReactionConditionFeature> features) {
    final ArrayList<ReactionConditionComponent> components =
        new ArrayList<ReactionConditionComponent>(conditions.components().size() + 1);
    for (int i = 0; i < conditions.components().size(); ++i) {
      final ReactionConditionComponent component = conditions.components().get(i);
      if (name.equals(component.name())) {
        continue;
      }
      components.add(component);
    }
    components.add(
        ReactionConditionComponent.of(
            role,
            name,
            null,
            null,
            equivalent,
            null,
            null,
            ReactionPhase.UNKNOWN,
            "Scenario marker for condition and equivalent screening."));
    return ReactionConditions.of(
        conditions.temperature(),
        conditions.pressure(),
        conditions.duration(),
        components,
        features,
        conditions.note());
  }

  private static List<ReactionConditionFeature> withFeature(
      final List<ReactionConditionFeature> features, final ReactionConditionFeatureKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException(
          "Reaction condition scenario feature kind must not be null.");
    }
    final ArrayList<ReactionConditionFeature> result =
        new ArrayList<ReactionConditionFeature>(features.size() + 1);
    boolean present = false;
    for (int i = 0; i < features.size(); ++i) {
      final ReactionConditionFeature feature = features.get(i);
      result.add(feature);
      if (feature.kind() == kind) {
        present = true;
      }
    }
    if (!present) {
      result.add(ReactionConditionFeature.named(kind));
    }
    return List.copyOf(result);
  }

  private static List<ReactionConditionFeature> withoutFeature(
      final List<ReactionConditionFeature> features,
      final ReactionConditionFeatureKind firstKind,
      final ReactionConditionFeatureKind... restKinds) {
    final ArrayList<ReactionConditionFeature> result =
        new ArrayList<ReactionConditionFeature>(features.size());
    for (int i = 0; i < features.size(); ++i) {
      final ReactionConditionFeature feature = features.get(i);
      if (feature.kind() == firstKind
          || ReactionConditionScenarioPlanner.contains(restKinds, feature.kind())) {
        continue;
      }
      result.add(feature);
    }
    return List.copyOf(result);
  }

  private static boolean contains(
      final ReactionConditionFeatureKind[] kinds, final ReactionConditionFeatureKind kind) {
    for (int i = 0; i < kinds.length; ++i) {
      if (kinds[i] == kind) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasComponentSignal(
      final ReactionConditions conditions, final String... signals) {
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final String name = components.get(i).name().toLowerCase(Locale.ROOT);
      for (int j = 0; j < signals.length; ++j) {
        if (name.contains(signals[j])) {
          return true;
        }
      }
    }
    return false;
  }
}
