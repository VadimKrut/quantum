/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.analysis.ReactionPathwayAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionBondChangeSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismEnergyPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionBalance;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ThermodynamicUnitConverter;

/**
 * Выполняет Java-native анализ пути реакции и строит доменный профиль механизма.
 */
public final class ReactionPathExecutor {

  private static final String JAVA_NATIVE_REACTION_ENERGY_METHOD =
      "java_native_single_point_reaction_energy";
  private static final String ENERGY_SOURCE_OPTION = "reaction_path_energy_source";
  private static final String MAX_AUTO_ELECTRONIC_ATOMS_OPTION =
      "reaction_path_max_auto_electronic_atoms";
  private static final String ENERGY_SOURCE_ELECTRONIC = "electronic";
  private static final String ENERGY_SOURCE_BOND_CHANGE = "bond_change";
  private static final int DEFAULT_MAX_AUTOMATIC_ELECTRONIC_BASELINE_ATOMS = 48;
  private final ReactionPathwayAnalyzer pathwayAnalyzer;
  private final ReactionEnergeticsExecutor reactionEnergeticsExecutor;

  public ReactionPathExecutor() {
    this(new ReactionPathwayAnalyzer(), new ReactionEnergeticsExecutor());
  }

  public ReactionPathExecutor(final ReactionPathwayAnalyzer pathwayAnalyzer) {
    this(pathwayAnalyzer, new ReactionEnergeticsExecutor());
  }

  public ReactionPathExecutor(
      final ReactionPathwayAnalyzer pathwayAnalyzer,
      final ReactionEnergeticsExecutor reactionEnergeticsExecutor) {
    if (pathwayAnalyzer == null) {
      throw new IllegalArgumentException("Reaction pathway analyzer must not be null.");
    }
    if (reactionEnergeticsExecutor == null) {
      throw new IllegalArgumentException("Reaction pathway energetics executor must not be null.");
    }
    this.pathwayAnalyzer = pathwayAnalyzer;
    this.reactionEnergeticsExecutor = reactionEnergeticsExecutor;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction path request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Reaction path calculation kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Reaction path diagnostics must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.REACTION) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Reaction path analysis requires reaction subject."));
      return null;
    }
    final Reaction reaction = (Reaction) request.subject();
    final ReactionBalance balance = reaction.balance();
    if (!balance.atomBalanced() || !balance.chargeBalanced()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_PARTIAL,
              ClassicalDiagnosticTarget.of("reaction", reaction.id().value()),
              "Reaction path is partial because reaction is not fully balanced."));
    }
    final ArrayList<ClassicalDiagnostic> electronicDiagnostics =
        new ArrayList<ClassicalDiagnostic>();
    final ClassicalResultSection electronicSection =
        this.electronicReactionEnergySection(request, reaction, electronicDiagnostics);
    final Double electronicReactionEnergy =
        ReactionPathExecutor.electronicReactionEnergyKilojoulePerMole(electronicSection);
    diagnostics.addAll(electronicDiagnostics);
    final ReactionPathwayEstimate estimate =
        this.pathwayAnalyzer.analyze(reaction, electronicReactionEnergy);
    if (electronicReactionEnergy == null) {
      diagnostics.add(
          ClassicalDiagnostic.info(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              "Reaction path uses mechanistic bond-change energetics because no Java-native"
                  + " participant electronic baseline is available for this request."));
    }
    final ReactionBondChangeSet bondChanges = estimate.bondChanges();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(
        ClassicalResultValue.text(
            "method",
            electronicReactionEnergy == null
                ? "mechanistic_bond_energy_pathway"
                : "mechanistic_electronic_energy_pathway"));
    values.add(
        ClassicalResultValue.text(
            "reaction_energy_source",
            electronicReactionEnergy == null
                ? "bond_change_model"
                : "java_native_single_point_reaction_energy"));
    if (electronicReactionEnergy != null) {
      values.add(
          ClassicalResultValue.scalar(
              "electronic_reaction_energy", electronicReactionEnergy.doubleValue(), "kJ/mol"));
    }
    values.add(
        ClassicalResultValue.text("atom_balanced", Boolean.toString(balance.atomBalanced())));
    values.add(
        ClassicalResultValue.text("charge_balanced", Boolean.toString(balance.chargeBalanced())));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_bond_count", bondChanges.reactantBondCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("product_bond_count", bondChanges.productBondCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("broken_bond_events", bondChanges.brokenBondEvents(), "count"));
    values.add(
        ClassicalResultValue.scalar("formed_bond_events", bondChanges.formedBondEvents(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "broken_bond_energy", bondChanges.brokenBondEnergy(), "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "formed_bond_energy", bondChanges.formedBondEnergy(), "kJ/mol"));
    values.add(ClassicalResultValue.scalar("reaction_energy", estimate.reactionEnergy(), "kJ/mol"));
    values.add(ClassicalResultValue.scalar("temperature", estimate.temperatureKelvin(), "K"));
    values.add(
        ClassicalResultValue.scalar(
            "condition_activation_shift", estimate.conditionActivationShift(), "kJ/mol"));
    values.add(ClassicalResultValue.scalar("forward_barrier", estimate.forwardBarrier(), "kJ/mol"));
    values.add(ClassicalResultValue.scalar("reverse_barrier", estimate.reverseBarrier(), "kJ/mol"));
    values.add(ClassicalResultValue.scalar("thermal_energy", estimate.thermalEnergy(), "kJ/mol"));
    values.add(ClassicalResultValue.scalar("forward_rate", estimate.forwardRate(), "relative"));
    values.add(ClassicalResultValue.scalar("reverse_rate", estimate.reverseRate(), "relative"));
    values.add(
        ClassicalResultValue.scalar(
            "forward_reverse_rate_ratio", estimate.forwardReverseRateRatio(), "ratio"));
    values.add(ClassicalResultValue.text("pathway_band", estimate.pathwayBand()));
    ReactionPathExecutor.appendMechanismValues(values, estimate);
    values.add(
        ClassicalResultValue.of(
            "reaction_path_points",
            ClassicalResultValueKind.TABLE,
            "dimensionless,kJ/mol",
            ReactionPathExecutor.reactionPathRows(estimate)));
    values.add(
        ClassicalResultValue.of(
            "mechanistic_events",
            ClassicalResultValueKind.TABLE,
            "index,kind,weight,description",
            ReactionPathExecutor.mechanisticEventRows(estimate)));
    return ClassicalResultSection.of(kind, ReactionPathExecutor.title(kind), values);
  }

  private ClassicalResultSection electronicReactionEnergySection(
      final ClassicalCalculationRequest request,
      final Reaction reaction,
      final List<ClassicalDiagnostic> diagnostics) {
    final String energySource = ReactionPathExecutor.energySource(request);
    if (ENERGY_SOURCE_BOND_CHANGE.equals(energySource)) {
      diagnostics.add(
          ClassicalDiagnostic.info(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("reactionPath", reaction.id().value()),
              "Reaction path electronic baseline is disabled by reaction_path_energy_source=bond_change."));
      return null;
    }
    if (!reaction.balance().atomBalanced() || !reaction.balance().chargeBalanced()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("reactionPath", reaction.id().value()),
              "Reaction path electronic baseline requires balanced atoms and charge."));
      return null;
    }
    final int totalAtoms = ReactionPathExecutor.totalStoichiometricAtomCount(reaction);
    final int maxAutoAtoms = ReactionPathExecutor.maxAutoElectronicAtoms(request);
    if (!ENERGY_SOURCE_ELECTRONIC.equals(energySource) && totalAtoms > maxAutoAtoms) {
      diagnostics.add(
          ClassicalDiagnostic.info(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("reactionPath", reaction.id().value()),
              "Reaction path auto electronic baseline skipped for "
                  + totalAtoms
                  + " stoichiometric atoms; set reaction_path_energy_source=electronic"
                  + " to force Java-native participant single-point energetics."));
      return null;
    }
    return this.reactionEnergeticsExecutor.execute(
        request, ClassicalCalculationKind.REACTION_ENERGY, diagnostics);
  }

  private static int totalStoichiometricAtomCount(final Reaction reaction) {
    return Math.addExact(
        ReactionPathExecutor.sideAtomCount(reaction.reactants()),
        ReactionPathExecutor.sideAtomCount(reaction.products()));
  }

  private static int sideAtomCount(
      final ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide side) {
    int count = 0;
    final List<ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant>
        participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant participant =
          participants.get(i);
      count += participant.coefficient().value() * participant.molecule().atomCount();
    }
    return count;
  }

  private static String energySource(final ClassicalCalculationRequest request) {
    for (int i = 0; i < request.options().size(); ++i) {
      final String name = request.options().get(i).name();
      if (!ENERGY_SOURCE_OPTION.equals(name)) {
        continue;
      }
      final String value = request.options().get(i).value().toLowerCase(Locale.ROOT);
      if ("auto".equals(value)
          || ENERGY_SOURCE_ELECTRONIC.equals(value)
          || ENERGY_SOURCE_BOND_CHANGE.equals(value)) {
        return value;
      }
      throw new IllegalArgumentException(
          "Reaction path energy source must be auto, electronic or bond_change.");
    }
    return "auto";
  }

  private static int maxAutoElectronicAtoms(final ClassicalCalculationRequest request) {
    for (int i = 0; i < request.options().size(); ++i) {
      final String name = request.options().get(i).name();
      if (!MAX_AUTO_ELECTRONIC_ATOMS_OPTION.equals(name)) {
        continue;
      }
      final int value;
      try {
        value = Integer.parseInt(request.options().get(i).value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(
            "Reaction path max auto electronic atom count must be an integer.");
      }
      if (value <= 0) {
        throw new IllegalArgumentException(
            "Reaction path max auto electronic atom count must be positive.");
      }
      return value;
    }
    return DEFAULT_MAX_AUTOMATIC_ELECTRONIC_BASELINE_ATOMS;
  }

  private static Double electronicReactionEnergyKilojoulePerMole(
      final ClassicalResultSection section) {
    if (section == null
        || !section.hasValue("method")
        || !section.hasValue("delta_electronic_energy_kj_mol")) {
      return null;
    }
    if (!JAVA_NATIVE_REACTION_ENERGY_METHOD.equals(section.requireFirstValue("method"))) {
      return null;
    }
    return Double.valueOf(section.requireScalar("delta_electronic_energy_kj_mol"));
  }

  private static void appendMechanismValues(
      final List<ClassicalResultValue> values, final ReactionPathwayEstimate estimate) {
    if (!estimate.hasMechanism()) {
      values.add(ClassicalResultValue.text("mechanism_available", "false"));
      values.add(ClassicalResultValue.scalar("transition_state_count", 0, "count"));
      return;
    }
    final ReactionMechanism mechanism = estimate.mechanism();
    values.add(ClassicalResultValue.text("mechanism_available", "true"));
    values.add(ClassicalResultValue.text("mechanism_id", mechanism.id()));
    values.add(ClassicalResultValue.text("mechanism_family", mechanism.family().name()));
    values.add(
        ClassicalResultValue.scalar("mechanism_step_count", mechanism.steps().size(), "count"));
    values.add(
        ClassicalResultValue.scalar("mechanism_event_count", mechanism.eventCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "transition_state_count", mechanism.transitionStateCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "highest_relative_energy",
            ThermodynamicUnitConverter.energyKiloJoulePerMole(mechanism.highestRelativeEnergy()),
            "kJ/mol"));
  }

  private static List<String> reactionPathRows(final ReactionPathwayEstimate estimate) {
    if (estimate.hasMechanism()) {
      return ReactionPathExecutor.mechanismRows(estimate.mechanism());
    }
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("coordinate\tenergy\tpoint_id\tkind");
    rows.add("0.0\t0.0\treactants\treactant_complex");
    rows.add("0.5\t" + estimate.forwardBarrier() + "\ttransition_state\ttransition_state");
    rows.add("1.0\t" + estimate.reactionEnergy() + "\tproducts\tproduct_complex");
    return List.copyOf(rows);
  }

  private static List<String> mechanismRows(final ReactionMechanism mechanism) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("coordinate\tenergy\tpoint_id\tkind");
    final List<MechanismEnergyPoint> points = mechanism.energyProfile();
    for (int i = 0; i < points.size(); ++i) {
      final MechanismEnergyPoint point = points.get(i);
      rows.add(
          point.coordinate().value()
              + "\t"
              + ThermodynamicUnitConverter.energyKiloJoulePerMole(point.relativeEnergy())
              + "\t"
              + point.id()
              + "\t"
              + point.kind().name().toLowerCase(Locale.ROOT));
    }
    return List.copyOf(rows);
  }

  private static List<String> mechanisticEventRows(final ReactionPathwayEstimate estimate) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("index\tstep_index\tstep_id\tkind\tweight\tdescription");
    if (!estimate.hasMechanism()) {
      return List.copyOf(rows);
    }
    final List<ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep>
        steps = estimate.mechanism().steps();
    int globalIndex = 0;
    for (int i = 0; i < steps.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep step =
          steps.get(i);
      final List<MechanisticEvent> events = step.events();
      for (int j = 0; j < events.size(); ++j) {
        final MechanisticEvent event = events.get(j);
        rows.add(
            globalIndex
                + "\t"
                + i
                + "\t"
                + step.id()
                + "\t"
                + event.kind().name().toLowerCase(Locale.ROOT)
                + "\t"
                + event.probabilityWeight()
                + "\t"
                + event.description());
        ++globalIndex;
      }
    }
    return List.copyOf(rows);
  }

  private static String title(final ClassicalCalculationKind kind) {
    if (kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      return "Transition-state analysis";
    }
    return "Reaction path analysis";
  }
}
