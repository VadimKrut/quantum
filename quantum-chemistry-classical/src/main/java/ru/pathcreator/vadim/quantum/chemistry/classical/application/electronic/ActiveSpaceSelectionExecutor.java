/* * Copyright 2026 Vadim Aleksandrovich Zaletaev * * This Source Code Form is subject to the terms of the Mozilla Public * License, v. 2.0. If a copy of the MPL was not distributed with this * file, You can obtain one at https://mozilla.org/MPL/2.0/. * SPDX-License-Identifier: MPL-2.0 */ package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** * Выполняет автоматический выбор active space на основе Java-native SCF reference. */
public final class ActiveSpaceSelectionExecutor {
  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ActiveSpaceSelector selector;

  public ActiveSpaceSelectionExecutor() {
    this(new ElectronicStructurePreparationPipeline(), new ActiveSpaceSelector());
  }

  public ActiveSpaceSelectionExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ActiveSpaceSelector selector) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Active-space selection preparation pipeline must not be null.");
    }
    if (selector == null) {
      throw new IllegalArgumentException("Active-space selector must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.selector = selector;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Active-space selection request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Active-space selection diagnostics target must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Active-space selection requires molecule subject."));
      return null;
    }
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    try {
      final ElectronicStructurePreparationResult preparationResult =
          unsupportedReason == null
              ? this.preparationPipeline.prepare(request)
              : this.directReferencePreparation(request, unsupportedReason, diagnostics);
      final double[] orbitalEnergies =
          ActiveSpaceSelectionExecutor.orbitalEnergies(preparationResult);
      final ActiveSpaceSelectionResult selection =
          this.selectActiveSpace(
              preparationResult,
              orbitalEnergies,
              ActiveSpaceSelectionOptions.fromOptions(request.options()));
      return ClassicalResultSection.of(
          ClassicalCalculationKind.ACTIVE_SPACE_SELECTION,
          "Automatic active-space selection",
          ActiveSpaceSelectionExecutor.values(
              preparationResult, selection, orbitalEnergies.length));
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ACTIVE_SPACE_SELECTION.name()),
              exception.getMessage()));
      return null;
    }
  }

  private ElectronicStructurePreparationResult directReferencePreparation(
      final ClassicalCalculationRequest request,
      final String unsupportedReason,
      final List<ClassicalDiagnostic> diagnostics) {
    if (this.preparationPipeline.oneElectronUnsupportedReason(request) != null) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ACTIVE_SPACE_SELECTION.name()),
              unsupportedReason));
      throw new IllegalArgumentException(unsupportedReason);
    }
    diagnostics.add(
        ClassicalDiagnostic.info(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.ACTIVE_SPACE_SELECTION.name()),
            "Active-space selection uses direct SCF reference because full ERI tensor preparation is unavailable: "
                + unsupportedReason));
    final Molecule molecule = (Molecule) request.subject();
    if (molecule.electronicConfiguration().closedShell()) {
      return this.preparationPipeline.prepareDirectRestrictedHartreeFock(request);
    }
    return this.preparationPipeline.prepareDirectUnrestrictedHartreeFock(request);
  }

  private static double[] orbitalEnergies(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasRhfReference()) {
      return preparationResult.rhfResult().orbitalEnergiesCopy();
    }
    if (preparationResult.hasRohfReference()) {
      return preparationResult.rohfResult().orbitalEnergiesCopy();
    }
    if (preparationResult.hasKohnShamDftReference()) {
      if (preparationResult.kohnShamDftResult().unrestricted()) {
        return ActiveSpaceSelectionExecutor.averageSpinOrbitalEnergies(
            preparationResult.kohnShamDftResult().alphaOrbitalEnergiesCopy(),
            preparationResult.kohnShamDftResult().betaOrbitalEnergiesCopy());
      }
      return preparationResult.kohnShamDftResult().orbitalEnergiesCopy();
    }
    if (preparationResult.hasUhfReference()) {
      return ActiveSpaceSelectionExecutor.averageSpinOrbitalEnergies(
          preparationResult.uhfResult().alphaOrbitalEnergiesCopy(),
          preparationResult.uhfResult().betaOrbitalEnergiesCopy());
    }
    throw new IllegalArgumentException(
        "Active-space selection requires RHF, ROHF, UHF or Kohn-Sham DFT orbital energies.");
  }

  private ActiveSpaceSelectionResult selectActiveSpace(
      final ElectronicStructurePreparationResult preparationResult,
      final double[] orbitalEnergies,
      final ActiveSpaceSelectionOptions options) {
    if (preparationResult.hasUhfReference()) {
      return this.selector.selectSpinResolved(
          preparationResult.electronCount(),
          orbitalEnergies,
          preparationResult.uhfResult().alphaElectronCount(),
          preparationResult.uhfResult().betaElectronCount(),
          options);
    }
    if (preparationResult.hasRohfReference()) {
      return this.selector.selectSpinResolved(
          preparationResult.electronCount(),
          orbitalEnergies,
          preparationResult.rohfResult().alphaElectronCount(),
          preparationResult.rohfResult().betaElectronCount(),
          options);
    }
    if (preparationResult.hasKohnShamDftReference()
        && preparationResult.kohnShamDftResult().unrestricted()) {
      return this.selector.selectSpinResolved(
          preparationResult.electronCount(),
          orbitalEnergies,
          preparationResult.kohnShamDftResult().alphaElectronCount(),
          preparationResult.kohnShamDftResult().betaElectronCount(),
          options);
    }
    return this.selector.select(preparationResult.electronCount(), orbitalEnergies, options);
  }

  private static double[] averageSpinOrbitalEnergies(final double[] alpha, final double[] beta) {
    final int count = Math.min(alpha.length, beta.length);
    final double[] averaged = new double[count];
    for (int orbitalIndex = 0; orbitalIndex < count; ++orbitalIndex) {
      averaged[orbitalIndex] = (alpha[orbitalIndex] + beta[orbitalIndex]) * 0.5;
    }
    return averaged;
  }

  private static List<ClassicalResultValue> values(
      final ElectronicStructurePreparationResult preparationResult,
      final ActiveSpaceSelectionResult selection,
      final int totalOrbitalCount) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(ClassicalResultValue.text("method", "frontier_energy_window"));
    values.add(
        ClassicalResultValue.text(
            "selection_contract",
            "deterministic_frontier_orbital_selector_from_java_native_orbital_energies"));
    values.add(
        ClassicalResultValue.text(
            "selection_source", "computed_orbital_spectrum_not_experimental_outcome_lookup"));
    values.add(ClassicalResultValue.text("manual_review_required", Boolean.toString(true)));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    ActiveSpaceSelectionExecutor.addReferenceValues(values, preparationResult);
    values.add(ClassicalResultValue.text("active_space_selected", Boolean.toString(true)));
    values.add(
        ClassicalResultValue.scalar(
            "total_electron_count", preparationResult.electronCount(), "count"));
    values.add(ClassicalResultValue.scalar("total_orbital_count", totalOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "active_electron_count", selection.activeSpace().electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "active_orbital_count", selection.activeSpace().orbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "first_active_orbital", selection.firstActiveOrbital(), "index"));
    values.add(
        ClassicalResultValue.scalar(
            "exclusive_last_active_orbital", selection.exclusiveLastActiveOrbital(), "index"));
    values.add(
        ClassicalResultValue.scalar(
            "active_orbital_window_width",
            selection.exclusiveLastActiveOrbital() - selection.firstActiveOrbital(),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "frontier_window_contains_homo",
            Boolean.toString(
                ActiveSpaceSelectionExecutor.containsOrbital(
                    selection.firstActiveOrbital(),
                    selection.exclusiveLastActiveOrbital(),
                    selection.homoOrbital()))));
    values.add(
        ClassicalResultValue.text(
            "frontier_window_contains_lumo",
            Boolean.toString(
                ActiveSpaceSelectionExecutor.containsOrbital(
                    selection.firstActiveOrbital(),
                    selection.exclusiveLastActiveOrbital(),
                    selection.lumoOrbital()))));
    values.add(
        ClassicalResultValue.scalar("core_orbital_count", selection.coreOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "virtual_orbital_count", selection.virtualOrbitalCount(totalOrbitalCount), "count"));
    values.add(ClassicalResultValue.scalar("homo_orbital", selection.homoOrbital(), "index"));
    values.add(ClassicalResultValue.scalar("lumo_orbital", selection.lumoOrbital(), "index"));
    values.add(
        ClassicalResultValue.scalar(
            "selected_occupied_orbital_count", selection.selectedOccupiedOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "selected_virtual_orbital_count", selection.selectedVirtualOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "active_electron_fraction", selection.activeElectronFraction(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "active_orbital_fraction", selection.activeOrbitalFraction(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "selection_quality_score", selection.selectionQualityScore(), "fraction"));
    values.add(
        ClassicalResultValue.text("selection_quality_band", selection.selectionQualityBand()));
    values.add(
        ClassicalResultValue.scalar("homo_energy", selection.homoEnergyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("lumo_energy", selection.lumoEnergyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("homo_lumo_gap", selection.homoLumoGapHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "selection_window_energy_span",
            ActiveSpaceSelectionExecutor.selectionEnergySpan(selection),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "selection_diagnostic_count", selection.selectionDiagnostics().size(), "count"));
    values.add(
        ClassicalResultValue.of(
            "selection_diagnostics",
            ClassicalResultValueKind.TABLE,
            "diagnostic",
            selection.selectionDiagnostics()));
    ActiveSpaceSelectionExecutor.addSpinResolvedOrbitalValues(values, preparationResult);
    values.add(
        ClassicalResultValue.of(
            "orbital_window",
            ClassicalResultValueKind.TABLE,
            "index|role|frontier|occupation|energy_hartree",
            selection.orbitalRows()));
    return values;
  }

  private static void addReferenceValues(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasRhfReference()) {
      values.add(ClassicalResultValue.text("reference_method", "RHF"));
      values.add(ClassicalResultValue.text("spin_resolved_reference", Boolean.toString(false)));
      values.add(ClassicalResultValue.text("open_shell_reference", Boolean.toString(false)));
      return;
    }
    if (preparationResult.hasUhfReference()) {
      values.add(ClassicalResultValue.text("reference_method", "UHF"));
      values.add(ClassicalResultValue.text("spin_resolved_reference", Boolean.toString(true)));
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count", preparationResult.uhfResult().alphaElectronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", preparationResult.uhfResult().betaElectronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "spin_imbalance",
              Math.abs(
                  preparationResult.uhfResult().alphaElectronCount()
                      - preparationResult.uhfResult().betaElectronCount()),
              "count"));
      values.add(ClassicalResultValue.text("open_shell_reference", Boolean.toString(true)));
      return;
    }
    if (preparationResult.hasRohfReference()) {
      values.add(ClassicalResultValue.text("reference_method", "ROHF"));
      values.add(ClassicalResultValue.text("spin_resolved_reference", Boolean.toString(true)));
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count",
              preparationResult.rohfResult().alphaElectronCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", preparationResult.rohfResult().betaElectronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "spin_imbalance",
              Math.abs(
                  preparationResult.rohfResult().alphaElectronCount()
                      - preparationResult.rohfResult().betaElectronCount()),
              "count"));
      values.add(ClassicalResultValue.text("open_shell_reference", Boolean.toString(true)));
      return;
    }
    if (preparationResult.hasKohnShamDftReference()) {
      final boolean unrestricted = preparationResult.kohnShamDftResult().unrestricted();
      values.add(
          ClassicalResultValue.text(
              "reference_method", unrestricted ? "UNRESTRICTED_KOHN_SHAM_DFT" : "KOHN_SHAM_DFT"));
      values.add(
          ClassicalResultValue.text("spin_resolved_reference", Boolean.toString(unrestricted)));
      values.add(ClassicalResultValue.text("open_shell_reference", Boolean.toString(unrestricted)));
      if (unrestricted) {
        values.add(
            ClassicalResultValue.scalar(
                "alpha_electron_count",
                preparationResult.kohnShamDftResult().alphaElectronCount(),
                "count"));
        values.add(
            ClassicalResultValue.scalar(
                "beta_electron_count",
                preparationResult.kohnShamDftResult().betaElectronCount(),
                "count"));
        values.add(
            ClassicalResultValue.scalar(
                "spin_imbalance",
                Math.abs(
                    preparationResult.kohnShamDftResult().alphaElectronCount()
                        - preparationResult.kohnShamDftResult().betaElectronCount()),
                "count"));
      }
      return;
    }
    values.add(ClassicalResultValue.text("reference_method", "UNKNOWN"));
    values.add(ClassicalResultValue.text("spin_resolved_reference", Boolean.toString(false)));
    values.add(ClassicalResultValue.text("open_shell_reference", Boolean.toString(false)));
  }

  private static void addSpinResolvedOrbitalValues(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasUhfReference()) {
      ActiveSpaceSelectionExecutor.addSpinRows(
          values,
          preparationResult.uhfResult().alphaOrbitalEnergiesCopy(),
          preparationResult.uhfResult().betaOrbitalEnergiesCopy());
      return;
    }
    if (preparationResult.hasRohfReference()) {
      ActiveSpaceSelectionExecutor.addSpinRows(
          values,
          preparationResult.rohfResult().orbitalEnergiesCopy(),
          preparationResult.rohfResult().orbitalEnergiesCopy());
      return;
    }
    if (preparationResult.hasKohnShamDftReference()
        && preparationResult.kohnShamDftResult().unrestricted()) {
      ActiveSpaceSelectionExecutor.addSpinRows(
          values,
          preparationResult.kohnShamDftResult().alphaOrbitalEnergiesCopy(),
          preparationResult.kohnShamDftResult().betaOrbitalEnergiesCopy());
    }
  }

  private static void addSpinRows(
      final ArrayList<ClassicalResultValue> values, final double[] alpha, final double[] beta) {
    values.add(
        ClassicalResultValue.of(
            "spin_resolved_orbital_energies",
            ClassicalResultValueKind.TABLE,
            "index|alpha_energy_hartree|beta_energy_hartree|average_energy_hartree",
            ActiveSpaceSelectionExecutor.spinRows(alpha, beta)));
  }

  private static ArrayList<String> spinRows(final double[] alpha, final double[] beta) {
    final int count = Math.min(alpha.length, beta.length);
    final ArrayList<String> rows = new ArrayList<String>(count);
    for (int orbitalIndex = 0; orbitalIndex < count; ++orbitalIndex) {
      rows.add(
          orbitalIndex
              + "|"
              + alpha[orbitalIndex]
              + "|"
              + beta[orbitalIndex]
              + "|"
              + ((alpha[orbitalIndex] + beta[orbitalIndex]) * 0.5));
    }
    return rows;
  }

  private static boolean containsOrbital(
      final int firstOrbital, final int exclusiveLastOrbital, final int orbital) {
    return orbital >= firstOrbital && orbital < exclusiveLastOrbital;
  }

  private static double selectionEnergySpan(final ActiveSpaceSelectionResult selection) {
    return Math.max(0.0, Math.abs(selection.lumoEnergyHartree() - selection.homoEnergyHartree()));
  }
}
