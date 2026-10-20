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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.analysis.ReactionPathwayAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionKineticEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionBalance;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Оценивает реакционную термохимию через внутреннюю bond-energy модель без внешнего backend.
 */
public final class ReactionEnergeticsExecutor {

  private static final double ROOM_TEMPERATURE_KELVIN = 298.15;
  private static final double GAS_CONSTANT_KJ_PER_MOLE_KELVIN = 0.00831446261815324;
  private static final double HARTREE_TO_KILOJOULE_PER_MOLE = 2625.499638;
  private static final String SINGLE_POINT_BACKEND_OPTION = "single_point_backend";
  private static final String REACTION_ENERGY_MODEL_OPTION = "reaction_energy_model";
  private static final String THERMODYNAMICS_TEMPERATURE_OPTION =
      "thermodynamics_temperature_kelvin";
  private final ReactionPathwayAnalyzer pathwayAnalyzer;
  private final ReactionKineticsModel kineticsModel;
  private final SinglePointEnergyExecutor singlePointEnergyExecutor;

  public ReactionEnergeticsExecutor() {
    this(
        new ReactionPathwayAnalyzer(),
        new ReactionKineticsModel(),
        new SinglePointEnergyExecutor());
  }

  public ReactionEnergeticsExecutor(final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    this(new ReactionPathwayAnalyzer(), new ReactionKineticsModel(), singlePointEnergyExecutor);
  }

  public ReactionEnergeticsExecutor(
      final ReactionPathwayAnalyzer pathwayAnalyzer,
      final ReactionKineticsModel kineticsModel,
      final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    if (pathwayAnalyzer == null) {
      throw new IllegalArgumentException("Reaction energetics pathway analyzer must not be null.");
    }
    if (kineticsModel == null) {
      throw new IllegalArgumentException("Reaction energetics kinetics model must not be null.");
    }
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException(
          "Reaction energetics single-point executor must not be null.");
    }
    this.pathwayAnalyzer = pathwayAnalyzer;
    this.kineticsModel = kineticsModel;
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction energetics request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Reaction energetics kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Reaction energetics diagnostics must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.REACTION) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Reaction energetics requires reaction subject."));
      return null;
    }
    final Reaction reaction = (Reaction) request.subject();
    final ReactionBalance balance = reaction.balance();
    if (!balance.atomBalanced() || !balance.chargeBalanced()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("reaction", reaction.id().value()),
              "Reaction energetics is computed for an unbalanced reaction; result is diagnostic only."));
    }
    if (balance.atomBalanced()
        && balance.chargeBalanced()
        && !ReactionEnergeticsExecutor.forceBondEnergyModel(request)) {
      final ClassicalResultSection firstPrinciplesSection =
          this.firstPrinciplesSectionOrNull(request, kind, reaction, balance, diagnostics);
      if (firstPrinciplesSection != null) {
        return firstPrinciplesSection;
      }
      if (ReactionEnergeticsExecutor.requiresFirstPrinciplesModel(request)) {
        diagnostics.add(
            ClassicalDiagnostic.error(
                ClassicalDiagnosticCode.RESULT_FAILED,
                ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
                "Reaction energetics was requested in first-principles-only mode, but at least one participant "
                    + "could not be solved with the Java-native electronic structure pipeline."));
        return null;
      }
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "Reaction energetics uses internal bond-energy model; ab initio thermochemistry is not claimed."));
    final double reactantBondEnergy = this.sideBondEnergy(reaction.reactants());
    final double productBondEnergy = this.sideBondEnergy(reaction.products());
    final double deltaEnthalpy = reactantBondEnergy - productBondEnergy;
    final double temperatureKelvin = this.temperatureKelvin(request, reaction.conditions());
    final double entropyModel = this.entropyModel(reaction);
    final double temperatureCorrection = -temperatureKelvin * entropyModel;
    final double conditionBias = this.conditionBiasKilojoulePerMole(reaction.conditions());
    final double deltaGibbs = deltaEnthalpy + temperatureCorrection + conditionBias;
    final double feasibility = this.feasibilityScore(deltaGibbs, reaction.conditions(), balance);
    final ReactionPathwayEstimate pathwayEstimate = this.pathwayAnalyzer.analyze(reaction);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addFallback(
        values,
        "java_native",
        "condition_adjusted_bond_energy",
        "reaction_bond_energy_thermochemistry",
        "Used only when Java-native first-principles participant solve is unavailable or disabled.");
    values.add(ClassicalResultValue.text("method", "condition_adjusted_bond_energy"));
    values.add(ClassicalResultValue.text("model_scope", "internal_bond_energy_thermochemistry"));
    values.add(
        ClassicalResultValue.text(
            "thermodynamic_model", "bond_enthalpy_temperature_entropy_condition_bias"));
    values.add(ClassicalResultValue.text("electronic_energy_basis", "not_available"));
    values.add(
        ClassicalResultValue.text("atom_balanced", Boolean.toString(balance.atomBalanced())));
    values.add(
        ClassicalResultValue.text("charge_balanced", Boolean.toString(balance.chargeBalanced())));
    values.add(ClassicalResultValue.scalar("reactant_bond_energy", reactantBondEnergy, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("product_bond_energy", productBondEnergy, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("delta_enthalpy", deltaEnthalpy, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("temperature", temperatureKelvin, "K"));
    values.add(
        ClassicalResultValue.scalar(
            "temperature_entropy_correction", temperatureCorrection, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("condition_bias", conditionBias, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "delta_thermal_gibbs_correction", temperatureCorrection + conditionBias, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("delta_gibbs", deltaGibbs, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("feasibility_score", feasibility, "fraction"));
    values.add(
        ClassicalResultValue.text(
            "feasibility_band", ReactionEnergeticsExecutor.feasibilityBand(feasibility)));
    values.add(
        ClassicalResultValue.text(
            "condition_summary", this.conditionSummary(reaction.conditions())));
    this.appendKineticValues(
        values, this.kineticsModel.estimate(pathwayEstimate, reaction.conditions()));
    values.add(
        ClassicalResultValue.of(
            "bond_energy_rows",
            ClassicalResultValueKind.TABLE,
            "kJ/mol",
            this.bondEnergyRows(reaction)));
    return ClassicalResultSection.of(kind, "Reaction bond-energy thermochemistry", values);
  }

  private ClassicalResultSection firstPrinciplesSectionOrNull(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final Reaction reaction,
      final ReactionBalance balance,
      final List<ClassicalDiagnostic> diagnostics) {
    if (ReactionEnergeticsExecutor.stoichiometricallyEquivalentSides(
            reaction.reactants(), reaction.products())
        && !ReactionEnergeticsExecutor.requiresParticipantElectronicSolve(request)) {
      return this.zeroFirstPrinciplesSection(request, kind, reaction, balance);
    }
    final ArrayList<ClassicalDiagnostic> localDiagnostics = new ArrayList<ClassicalDiagnostic>();
    final ReactionSideEnergy reactants =
        this.sideFirstPrinciplesEnergy(request, reaction.reactants(), "reactant", localDiagnostics);
    final ReactionSideEnergy products =
        this.sideFirstPrinciplesEnergy(request, reaction.products(), "product", localDiagnostics);
    if (reactants == null || products == null) {
      diagnostics.addAll(localDiagnostics);
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              "Java-native first-principles reaction energy is unavailable for at least one"
                  + " participant; falling back to internal bond-energy thermochemistry."));
      return null;
    }
    final double deltaEnergyHartree = products.energyHartree - reactants.energyHartree;
    final double deltaEnergyKilojoulePerMole = deltaEnergyHartree * HARTREE_TO_KILOJOULE_PER_MOLE;
    final double temperatureKelvin = this.temperatureKelvin(request, reaction.conditions());
    final double entropyModel = this.entropyModel(reaction);
    final double temperatureCorrection = -temperatureKelvin * entropyModel;
    final double conditionBias = this.conditionBiasKilojoulePerMole(reaction.conditions());
    final double deltaGibbs = deltaEnergyKilojoulePerMole + temperatureCorrection + conditionBias;
    final ReactionPathwayEstimate pathwayEstimate = this.pathwayAnalyzer.analyze(reaction);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "single_point_reaction_energy",
        "stoichiometric_sum_of_java_native_single_point_energies");
    values.add(ClassicalResultValue.text("method", "java_native_single_point_reaction_energy"));
    values.add(
        ClassicalResultValue.text(
            "model_scope", "stoichiometric_sum_of_java_native_single_point_energies"));
    values.add(
        ClassicalResultValue.text(
            "thermodynamic_model", "electronic_energy_temperature_entropy_condition_bias"));
    values.add(
        ClassicalResultValue.text("electronic_energy_basis", request.method().name().value()));
    values.add(
        ClassicalResultValue.text("atom_balanced", Boolean.toString(balance.atomBalanced())));
    values.add(
        ClassicalResultValue.text("charge_balanced", Boolean.toString(balance.chargeBalanced())));
    values.add(ClassicalResultValue.text("first_principles_route_complete", "true"));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_participant_count", reactants.participantCount, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_participant_count", products.participantCount, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "first_principles_participant_count",
            Math.addExact(reactants.participantCount, products.participantCount),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "quantum_program_preparable_participant_count",
            Math.addExact(
                reactants.quantumProgramPreparableParticipantCount,
                products.quantumProgramPreparableParticipantCount),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "reaction_quantum_program_preparation_status",
            ReactionEnergeticsExecutor.reactionQuantumPreparationStatus(reactants, products)));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_stoichiometric_coefficient_sum", reactants.coefficientSum, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_stoichiometric_coefficient_sum", products.coefficientSum, "count"));
    values.add(
        ClassicalResultValue.scalar("reactant_total_energy", reactants.energyHartree, "hartree"));
    values.add(
        ClassicalResultValue.scalar("product_total_energy", products.energyHartree, "hartree"));
    values.add(
        ClassicalResultValue.scalar("delta_electronic_energy", deltaEnergyHartree, "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "delta_electronic_energy_consistency_error",
            Math.abs(deltaEnergyHartree - (products.energyHartree - reactants.energyHartree)),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "delta_electronic_energy_kj_mol", deltaEnergyKilojoulePerMole, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar("delta_enthalpy", deltaEnergyKilojoulePerMole, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("temperature", temperatureKelvin, "K"));
    values.add(
        ClassicalResultValue.scalar(
            "temperature_entropy_correction", temperatureCorrection, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("condition_bias", conditionBias, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "delta_thermal_gibbs_correction", temperatureCorrection + conditionBias, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("delta_gibbs", deltaGibbs, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "feasibility_score",
            this.feasibilityScore(deltaGibbs, reaction.conditions(), balance),
            "fraction"));
    values.add(
        ClassicalResultValue.text(
            "condition_summary", this.conditionSummary(reaction.conditions())));
    this.appendKineticValues(
        values, this.kineticsModel.estimate(pathwayEstimate, reaction.conditions()));
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add(
        "side\tcoefficient\tmolecule\tmethod\timplemented_kernel\treference_method\tconverged"
            + "\telectron_count\tcorrelation_energy\texchange_correlation_energy"
            + "\tparticipant_total_energy\tweighted_total_energy"
            + "\tactive_space_used\torbital_optimized\tquantum_program_preparable\thamiltonian_preparable");
    rows.addAll(reactants.rows);
    rows.addAll(products.rows);
    values.add(
        ClassicalResultValue.of(
            "participant_energy_rows", ClassicalResultValueKind.TABLE, "hartree", rows));
    return ClassicalResultSection.of(
        kind, "Java-native reaction first-principles energetics", values);
  }

  private ClassicalResultSection zeroFirstPrinciplesSection(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final Reaction reaction,
      final ReactionBalance balance) {
    final double temperatureKelvin = this.temperatureKelvin(request, reaction.conditions());
    final ReactionPathwayEstimate pathwayEstimate = this.pathwayAnalyzer.analyze(reaction);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "single_point_reaction_energy_shortcut",
        "stoichiometrically_identical_reaction_shortcut");
    values.add(ClassicalResultValue.text("method", "java_native_single_point_reaction_energy"));
    values.add(
        ClassicalResultValue.text("model_scope", "stoichiometrically_identical_reaction_shortcut"));
    values.add(
        ClassicalResultValue.text("thermodynamic_model", "identical_reactant_product_state"));
    values.add(
        ClassicalResultValue.text("electronic_energy_basis", request.method().name().value()));
    values.add(
        ClassicalResultValue.text("atom_balanced", Boolean.toString(balance.atomBalanced())));
    values.add(
        ClassicalResultValue.text("charge_balanced", Boolean.toString(balance.chargeBalanced())));
    values.add(ClassicalResultValue.text("first_principles_route_complete", "true"));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_participant_count", reaction.reactants().participants().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_participant_count", reaction.products().participants().size(), "count"));
    values.add(ClassicalResultValue.scalar("first_principles_participant_count", 0, "count"));
    values.add(
        ClassicalResultValue.scalar("quantum_program_preparable_participant_count", 0.0, "count"));
    values.add(
        ClassicalResultValue.text(
            "reaction_quantum_program_preparation_status",
            "IDENTICAL_REACTION_NO_PARTICIPANT_SOLVE_REQUIRED"));
    values.add(
        ClassicalResultValue.scalar(
            "reactant_stoichiometric_coefficient_sum",
            ReactionEnergeticsExecutor.coefficientSum(reaction.reactants()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "product_stoichiometric_coefficient_sum",
            ReactionEnergeticsExecutor.coefficientSum(reaction.products()),
            "count"));
    values.add(ClassicalResultValue.text("participant_energies_computed", "false"));
    values.add(ClassicalResultValue.scalar("delta_electronic_energy", 0.0, "hartree"));
    values.add(
        ClassicalResultValue.scalar("delta_electronic_energy_consistency_error", 0.0, "hartree"));
    values.add(ClassicalResultValue.scalar("delta_electronic_energy_kj_mol", 0.0, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("delta_enthalpy", 0.0, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("temperature", temperatureKelvin, "K"));
    values.add(ClassicalResultValue.scalar("temperature_entropy_correction", 0.0, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("condition_bias", 0.0, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("delta_thermal_gibbs_correction", 0.0, "kJ/mol"));
    values.add(ClassicalResultValue.scalar("delta_gibbs", 0.0, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "feasibility_score",
            this.feasibilityScore(0.0, reaction.conditions(), balance),
            "fraction"));
    values.add(
        ClassicalResultValue.text(
            "condition_summary", this.conditionSummary(reaction.conditions())));
    this.appendKineticValues(
        values, this.kineticsModel.estimate(pathwayEstimate, reaction.conditions()));
    values.add(
        ClassicalResultValue.of(
            "participant_energy_rows",
            ClassicalResultValueKind.TABLE,
            "hartree",
            List.of(
                "side\tcoefficient\tmolecule\tmethod\timplemented_kernel\treference_method\tconverged"
                    + "\telectron_count\tcorrelation_energy\texchange_correlation_energy"
                    + "\tparticipant_total_energy\tweighted_total_energy"
                    + "\tactive_space_used\torbital_optimized\tquantum_program_preparable\thamiltonian_preparable")));
    return ClassicalResultSection.of(
        kind, "Java-native reaction first-principles energetics", values);
  }

  private ReactionSideEnergy sideFirstPrinciplesEnergy(
      final ClassicalCalculationRequest request,
      final ReactionSide side,
      final String sideName,
      final List<ClassicalDiagnostic> diagnostics) {
    double totalEnergy = 0.0;
    long coefficientSum = 0L;
    int quantumProgramPreparableParticipantCount = 0;
    final ArrayList<String> rows = new ArrayList<String>();
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final ClassicalResultSection section =
          this.participantSinglePointSection(request, participant, sideName, i, diagnostics);
      if (section == null || !section.hasValue("total_energy")) {
        diagnostics.add(
            ClassicalDiagnostic.warning(
                ClassicalDiagnosticCode.RESULT_FAILED,
                ClassicalDiagnosticTarget.of("reactionParticipant", sideName + "[" + i + "]"),
                "Java-native single-point result did not contain total_energy."));
        return null;
      }
      final double participantEnergy = section.requireScalar("total_energy");
      final double weightedEnergy = (double) participant.coefficient().value() * participantEnergy;
      totalEnergy += weightedEnergy;
      coefficientSum += participant.coefficient().value();
      if (ReactionEnergeticsExecutor.hamiltonianPreparableMethod(request)) {
        ++quantumProgramPreparableParticipantCount;
      }
      rows.add(
          ReactionEnergeticsExecutor.participantEnergyRow(
              sideName,
              participant,
              section,
              ReactionEnergeticsExecutor.hamiltonianPreparableMethod(request),
              participantEnergy,
              weightedEnergy));
    }
    return new ReactionSideEnergy(
        totalEnergy,
        participants.size(),
        coefficientSum,
        quantumProgramPreparableParticipantCount,
        List.copyOf(rows));
  }

  private static boolean stoichiometricallyEquivalentSides(
      final ReactionSide first, final ReactionSide second) {
    return ReactionEnergeticsExecutor.canonicalSide(first)
        .equals(ReactionEnergeticsExecutor.canonicalSide(second));
  }

  private static Map<Molecule, Long> canonicalSide(final ReactionSide side) {
    final Map<Molecule, Long> result = new HashMap<>();
    for (final ReactionParticipant participant : side.participants()) {
      result.merge(
          participant.molecule(), (long) participant.coefficient().value(), Math::addExact);
    }
    return Map.copyOf(result);
  }

  private static long coefficientSum(final ReactionSide side) {
    long result = 0L;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      result += participants.get(i).coefficient().value();
    }
    return result;
  }

  private ClassicalResultSection participantSinglePointSection(
      final ClassicalCalculationRequest request,
      final ReactionParticipant participant,
      final String sideName,
      final int participantIndex,
      final List<ClassicalDiagnostic> diagnostics) {
    final ClassicalCalculationRequest participantRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(
                request.id().value() + "_" + sideName + "_" + participantIndex),
            participant.molecule(),
            request.method(),
            request.basisSet(),
            request.activeSpace(),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            ReactionEnergeticsExecutor.nativeSinglePointOptions(request.options()),
            request.metadata());
    final ArrayList<ClassicalDiagnostic> participantDiagnostics =
        new ArrayList<ClassicalDiagnostic>();
    final ClassicalResultSection section =
        this.singlePointEnergyExecutor.execute(participantRequest, participantDiagnostics);
    if (section == null) {
      diagnostics.addAll(participantDiagnostics);
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of(
                  "reactionParticipant", sideName + "[" + participantIndex + "]"),
              "Java-native single-point failed for reaction participant "
                  + participant.molecule().formula().hillNotation()
                  + "."));
      return null;
    }
    diagnostics.addAll(participantDiagnostics);
    return section;
  }

  private static String participantEnergyRow(
      final String sideName,
      final ReactionParticipant participant,
      final ClassicalResultSection section,
      final boolean hamiltonianPreparable,
      final double participantEnergy,
      final double weightedEnergy) {
    return sideName
        + "\t"
        + participant.coefficient().value()
        + "\t"
        + participant.molecule().formula().hillNotation()
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(section, "method", "unknown")
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(section, "implemented_kernel", "not_reported")
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(section, "reference_method", "not_reported")
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(section, "converged", "not_reported")
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(section, "electron_count", "not_reported")
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(section, "correlation_energy", "not_reported")
        + "\t"
        + ReactionEnergeticsExecutor.valueOrDefault(
            section, "exchange_correlation_energy", "not_reported")
        + "\t"
        + participantEnergy
        + "\t"
        + weightedEnergy
        + "\t"
        + ReactionEnergeticsExecutor.activeSpaceUsed(section)
        + "\t"
        + ReactionEnergeticsExecutor.orbitalOptimized(section)
        + "\t"
        + hamiltonianPreparable
        + "\t"
        + hamiltonianPreparable;
  }

  private static String activeSpaceUsed(final ClassicalResultSection section) {
    return Boolean.toString(section.hasValue("active_space_orbitals"));
  }

  private static String orbitalOptimized(final ClassicalResultSection section) {
    if (section.hasValue("orbital_optimized_reference")) {
      return section.requireFirstValue("orbital_optimized_reference");
    }
    return Boolean.toString(
        ReactionEnergeticsExecutor.valueOrDefault(section, "implemented_kernel", "")
            .contains("casscf_orbital_optimization"));
  }

  private static boolean hamiltonianPreparableMethod(final ClassicalCalculationRequest request) {
    return request.method().type() != ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        && request.method().type() != ElectronicStructureMethodType.SEMI_EMPIRICAL;
  }

  private static boolean requiresParticipantElectronicSolve(
      final ClassicalCalculationRequest request) {
    if (request.method().type()
        == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return true;
    }
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final String name = options.get(i).name();
      if (name.startsWith("hamiltonian_")
          || name.startsWith("casscf_")
          || name.startsWith("fci_")
          || name.startsWith("cc")
          || name.startsWith("mp2_")) {
        return true;
      }
    }
    return false;
  }

  private static String reactionQuantumPreparationStatus(
      final ReactionSideEnergy reactants, final ReactionSideEnergy products) {
    final int total = Math.addExact(reactants.participantCount, products.participantCount);
    if (total == 0) {
      return "NO_PARTICIPANTS";
    }
    final int preparable =
        Math.addExact(
            reactants.quantumProgramPreparableParticipantCount,
            products.quantumProgramPreparableParticipantCount);
    if (preparable == total) {
      return "ALL_PARTICIPANTS_HAMILTONIAN_AND_QUANTUM_PROGRAM_PREPARABLE";
    }
    if (preparable == 0) {
      return "NO_PARTICIPANT_QUANTUM_PROGRAM_PREPARABLE";
    }
    return "PARTIAL_PARTICIPANT_QUANTUM_PROGRAM_PREPARATION";
  }

  private static String valueOrDefault(
      final ClassicalResultSection section, final String name, final String defaultValue) {
    if (!section.hasValue(name)) {
      return defaultValue;
    }
    return section.requireFirstValue(name);
  }

  private static List<ClassicalCalculationOption> nativeSinglePointOptions(
      final List<ClassicalCalculationOption> options) {
    final ArrayList<ClassicalCalculationOption> result =
        new ArrayList<ClassicalCalculationOption>();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (SINGLE_POINT_BACKEND_OPTION.equals(option.name())) {
        continue;
      }
      result.add(option);
    }
    return List.copyOf(result);
  }

  private static boolean forceBondEnergyModel(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!REACTION_ENERGY_MODEL_OPTION.equals(option.name())) {
        continue;
      }
      return option.value().equalsIgnoreCase("bond")
          || option.value().equalsIgnoreCase("bond_energy")
          || option.value().equalsIgnoreCase("heuristic");
    }
    return false;
  }

  private static boolean requiresFirstPrinciplesModel(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!REACTION_ENERGY_MODEL_OPTION.equals(option.name())) {
        continue;
      }
      return option.value().equalsIgnoreCase("first_principles")
          || option.value().equalsIgnoreCase("ab_initio")
          || option.value().equalsIgnoreCase("native_first_principles")
          || option.value().equalsIgnoreCase("electronic_structure");
    }
    return false;
  }

  private void appendKineticValues(
      final List<ClassicalResultValue> values, final ReactionKineticEstimate estimate) {
    values.add(
        ClassicalResultValue.scalar(
            "kinetic_forward_barrier", estimate.forwardBarrierKilojoulePerMole(), "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "kinetic_reverse_barrier", estimate.reverseBarrierKilojoulePerMole(), "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "kinetic_relative_forward_rate", estimate.relativeForwardRate(), "relative"));
    values.add(
        ClassicalResultValue.scalar(
            "kinetic_relative_reverse_rate", estimate.relativeReverseRate(), "relative"));
    values.add(
        ClassicalResultValue.scalar(
            "kinetic_forward_reverse_ratio", estimate.forwardReverseRateRatio(), "ratio"));
    values.add(
        ClassicalResultValue.scalar(
            "arrhenius_pre_exponential",
            estimate.arrheniusPreExponentialFactor(),
            estimate.arrheniusUnit()));
    values.add(ClassicalResultValue.text("kinetic_model", estimate.kineticModel()));
    values.add(ClassicalResultValue.text("kinetic_context", estimate.kineticContext()));
    values.add(
        ClassicalResultValue.scalar(
            "arrhenius_forward_rate_constant",
            estimate.arrheniusForwardRateConstant(),
            estimate.arrheniusUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "eyring_activation_entropy",
            estimate.eyringActivationEntropyJoulePerMoleKelvin(),
            "J/mol/K"));
    values.add(
        ClassicalResultValue.scalar(
            "eyring_forward_rate_constant", estimate.eyringForwardRateConstant(), "PER_SECOND"));
    values.add(
        ClassicalResultValue.scalar(
            "first_order_half_life", estimate.firstOrderHalfLifeSeconds(), "s"));
    if (estimate.hasDurationConversion()) {
      values.add(
          ClassicalResultValue.scalar(
              "duration_conversion_fraction", estimate.durationConversionFraction(), "fraction"));
    }
  }

  private double sideBondEnergy(final ReactionSide side) {
    double energy = 0.0;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      energy +=
          (double) participant.coefficient().value()
              * this.moleculeBondEnergy(participant.molecule());
    }
    return energy;
  }

  private double moleculeBondEnergy(final Molecule molecule) {
    double energy = 0.0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      energy += ReactionPathwayAnalyzer.bondEnergyKilojoulePerMole(bonds.get(i).type());
    }
    return energy;
  }

  private double temperatureKelvin(final ReactionConditions conditions) {
    return ReactionPathwayAnalyzer.temperatureKelvin(conditions);
  }

  private double temperatureKelvin(
      final ClassicalCalculationRequest request, final ReactionConditions conditions) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!THERMODYNAMICS_TEMPERATURE_OPTION.equals(option.name())) {
        continue;
      }
      final double temperatureKelvin;
      try {
        temperatureKelvin = Double.parseDouble(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(
            "Reaction thermodynamics temperature option must be numeric.");
      }
      if (!Double.isFinite(temperatureKelvin) || temperatureKelvin <= 0.0) {
        throw new IllegalArgumentException(
            "Reaction thermodynamics temperature must be finite and positive.");
      }
      return temperatureKelvin;
    }
    return this.temperatureKelvin(conditions);
  }

  private double entropyModel(final Reaction reaction) {
    final int productCount = reaction.products().participantCount();
    final int reactantCount = reaction.reactants().participantCount();
    return GAS_CONSTANT_KJ_PER_MOLE_KELVIN * (double) (productCount - reactantCount);
  }

  private double conditionBiasKilojoulePerMole(final ReactionConditions conditions) {
    double bias = 0.0;
    if (conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        || !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      bias -= 8.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        || this.temperatureKelvin(conditions) > ROOM_TEMPERATURE_KELVIN + 20.0) {
      bias -= 4.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)) {
      bias += 2.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
        || conditions.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL)) {
      bias -= 6.0;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        || conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)) {
      bias -= 1.5;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      bias -= 2.0;
    }
    return bias;
  }

  private double feasibilityScore(
      final double deltaGibbs, final ReactionConditions conditions, final ReactionBalance balance) {
    double score = 1.0 / (1.0 + Math.exp(deltaGibbs / 40.0));
    if (!balance.atomBalanced() || !balance.chargeBalanced()) {
      score *= 0.35;
    }
    if (conditions.hasDuration()) {
      score = Math.min(0.98, score + 0.03);
    }
    return Math.max(0.0, Math.min(1.0, score));
  }

  private List<String> bondEnergyRows(final Reaction reaction) {
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("side\tcoefficient\tmolecule\tbond_count\tbond_energy");
    this.appendRows(rows, "reactant", reaction.reactants());
    this.appendRows(rows, "product", reaction.products());
    return List.copyOf(rows);
  }

  private void appendRows(final List<String> rows, final String sideName, final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      rows.add(
          sideName
              + "\t"
              + participant.coefficient().value()
              + "\t"
              + participant.molecule().formula().hillNotation()
              + "\t"
              + participant.molecule().bondCount()
              + "\t"
              + this.moleculeBondEnergy(participant.molecule()));
    }
  }

  private String conditionSummary(final ReactionConditions conditions) {
    if (conditions.empty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    if (conditions.temperature() != null) {
      builder.append("T=");
      builder.append(conditions.temperature().kelvin());
      builder.append("K");
    }
    final List<ReactionConditionComponent> catalysts =
        conditions.componentsByRole(ReactionComponentRole.CATALYST);
    if (!catalysts.isEmpty()) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append("catalyst=");
      builder.append(catalysts.get(0).name());
    }
    final String solvent = conditions.solvent();
    if (solvent != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append("solvent=");
      builder.append(solvent);
    }
    if (builder.length() == 0) {
      return "specified";
    }
    return builder.toString();
  }

  private static String feasibilityBand(final double feasibility) {
    if (feasibility >= 0.70) {
      return "favorable";
    }
    if (feasibility >= 0.40) {
      return "conditional";
    }
    return "unfavorable";
  }

  private static final class ReactionSideEnergy {

    private final double energyHartree;
    private final int participantCount;
    private final long coefficientSum;
    private final int quantumProgramPreparableParticipantCount;
    private final List<String> rows;

    private ReactionSideEnergy(
        final double energyHartree,
        final int participantCount,
        final long coefficientSum,
        final int quantumProgramPreparableParticipantCount,
        final List<String> rows) {
      this.energyHartree = energyHartree;
      this.participantCount = participantCount;
      this.coefficientSum = coefficientSum;
      this.quantumProgramPreparableParticipantCount = quantumProgramPreparableParticipantCount;
      this.rows = rows;
    }
  }
}
