/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Выполняет локальную geometry optimization и gradient estimation для молекулы.
 */
public final class GeometryOptimizationExecutor {

  private static final String GEOMETRY_OPTIMIZATION_BACKEND_OPTION =
      "geometry_optimization_backend";
  private static final String OPTIMIZATION_BACKEND_OPTION = "optimization_backend";
  private static final String PYSCF_BACKEND = "pyscf";

  private final LineSearchGeometryOptimizer optimizer;
  private final BondedForceFieldEnergyModel forceFieldEnergyModel;
  private final SinglePointEnergyExecutor singlePointEnergyExecutor;
  private final PyScfGeometryOptimizationExecutor pyScfGeometryOptimizationExecutor;

  public GeometryOptimizationExecutor() {
    this(
        new LineSearchGeometryOptimizer(),
        new BondedForceFieldEnergyModel(),
        new SinglePointEnergyExecutor(),
        new PyScfGeometryOptimizationExecutor());
  }

  public GeometryOptimizationExecutor(
      final LineSearchGeometryOptimizer optimizer,
      final BondedForceFieldEnergyModel forceFieldEnergyModel,
      final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    this(
        optimizer,
        forceFieldEnergyModel,
        singlePointEnergyExecutor,
        new PyScfGeometryOptimizationExecutor());
  }

  public GeometryOptimizationExecutor(
      final LineSearchGeometryOptimizer optimizer,
      final BondedForceFieldEnergyModel forceFieldEnergyModel,
      final SinglePointEnergyExecutor singlePointEnergyExecutor,
      final PyScfGeometryOptimizationExecutor pyScfGeometryOptimizationExecutor) {
    if (optimizer == null) {
      throw new IllegalArgumentException(
          "Geometry optimization executor optimizer must not be null.");
    }
    if (forceFieldEnergyModel == null) {
      throw new IllegalArgumentException("Geometry optimization energy model must not be null.");
    }
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException(
          "Geometry optimization single-point executor must not be null.");
    }
    if (pyScfGeometryOptimizationExecutor == null) {
      throw new IllegalArgumentException("PySCF geometry optimization executor must not be null.");
    }
    this.optimizer = optimizer;
    this.forceFieldEnergyModel = forceFieldEnergyModel;
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    this.pyScfGeometryOptimizationExecutor = pyScfGeometryOptimizationExecutor;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Geometry optimization requires molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    final String coordinateError = GeometryCoordinateSupport.coordinateError(molecule);
    if (coordinateError != null) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of("molecule", molecule.id().value()),
              coordinateError));
      return null;
    }
    final GeometryOptimizationOptions options =
        GeometryOptimizationOptions.fromOptions(request.options());
    if (GeometryOptimizationExecutor.pyScfBackendRequested(request)) {
      return this.pyScfGeometryOptimizationExecutor.execute(
          request, molecule, options, diagnostics);
    }
    final GeometryEnergyModel energyModel = this.energyModel(request, options);
    final GeometryOptimizationResult result =
        this.optimizer.optimize(molecule, options, energyModel);
    GeometryOptimizationExecutor.addModelDiagnosticIfNeeded(energyModel, diagnostics);
    return ClassicalResultSection.of(
        ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
        "Local Java-native geometry optimization",
        GeometryOptimizationExecutor.values(molecule, result, energyModel));
  }

  private GeometryEnergyModel energyModel(
      final ClassicalCalculationRequest request, final GeometryOptimizationOptions options) {
    if (options.energyModelKind() == GeometryEnergyModelKind.AUTO) {
      final Molecule molecule = (Molecule) request.subject();
      if (molecule.atomCount() <= options.autoElectronicMaxAtoms()) {
        return new ElectronicStructureFiniteDifferenceEnergyModel(
            request, this.singlePointEnergyExecutor);
      }
      return this.forceFieldEnergyModel;
    }
    if (options.energyModelKind() == GeometryEnergyModelKind.BONDED_FORCE_FIELD) {
      return this.forceFieldEnergyModel;
    }
    return new ElectronicStructureFiniteDifferenceEnergyModel(
        request, this.singlePointEnergyExecutor);
  }

  private static List<ClassicalResultValue> values(
      final Molecule molecule,
      final GeometryOptimizationResult result,
      final GeometryEnergyModel energyModel) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    GeometryOptimizationExecutor.addQualityValues(values, energyModel);
    values.add(ClassicalResultValue.text("backend", "java_native"));
    values.add(ClassicalResultValue.text("energy_model", energyModel.name()));
    values.add(ClassicalResultValue.text("method", energyModel.name() + "_bfgs_line_search"));
    values.add(ClassicalResultValue.text("derivative_stencil", energyModel.derivativeStencil()));
    values.add(ClassicalResultValue.scalar("atom_count", molecule.atomCount(), "count"));
    values.add(ClassicalResultValue.scalar("coordinate_count", molecule.atomCount() * 3, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "energy_evaluation_count", energyModel.energyEvaluationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "energy_cache_hit_count", energyModel.energyCacheHitCount(), "count"));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iteration_count", result.iterationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "initial_energy", result.initialEnergy(), energyModel.energyUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "final_energy", result.finalEnergy(), energyModel.energyUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "energy_decrease",
            result.initialEnergy() - result.finalEnergy(),
            energyModel.energyUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "gradient_norm", result.gradientNorm(), energyModel.gradientUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "gradient_rms",
            result.gradientNorm() / Math.sqrt((double) Math.max(1, molecule.atomCount() * 3)),
            energyModel.gradientUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "max_gradient_component", result.maxGradientComponent(), energyModel.gradientUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "accepted_step_count",
            GeometryOptimizationExecutor.acceptedStepCount(result.history()),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "zero_step_count",
            GeometryOptimizationExecutor.zeroStepCount(result.history()),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "energy_monotonic_nonincreasing",
            Boolean.toString(
                GeometryOptimizationExecutor.energyMonotonicNonIncreasing(result.history()))));
    values.add(
        ClassicalResultValue.scalar(
            "optimized_coordinate_count", result.coordinatesAngstromCopy().length, "count"));
    values.add(
        ClassicalResultValue.of(
            "optimization_history",
            ClassicalResultValueKind.TABLE,
            energyModel.energyUnit(),
            GeometryOptimizationExecutor.historyRows(result.history())));
    values.add(
        ClassicalResultValue.of(
            "optimized_coordinates",
            ClassicalResultValueKind.TABLE,
            "angstrom",
            GeometryOptimizationExecutor.coordinateRows(
                molecule, result.coordinatesAngstromCopy())));
    return List.copyOf(values);
  }

  private static void addQualityValues(
      final List<ClassicalResultValue> values, final GeometryEnergyModel energyModel) {
    if ("bonded_force_field".equals(energyModel.name())) {
      ClassicalResultQualityValues.addApproximate(
          values,
          "java_native",
          "bonded_force_field_geometry_optimization",
          "geometry_optimization",
          "Bonded force-field optimization is a fast structural approximation, not an electronic-structure optimizer.");
      return;
    }
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "electronic_structure_finite_difference_geometry_optimization",
        "geometry_optimization");
  }

  private static int acceptedStepCount(final List<GeometryOptimizationRecord> history) {
    int count = 0;
    for (int i = 0; i < history.size(); ++i) {
      if (history.get(i).acceptedStepAngstrom() > 0.0) {
        ++count;
      }
    }
    return count;
  }

  private static int zeroStepCount(final List<GeometryOptimizationRecord> history) {
    int count = 0;
    for (int i = 0; i < history.size(); ++i) {
      if (history.get(i).acceptedStepAngstrom() == 0.0) {
        ++count;
      }
    }
    return count;
  }

  private static boolean energyMonotonicNonIncreasing(
      final List<GeometryOptimizationRecord> history) {
    for (int i = 1; i < history.size(); ++i) {
      if (history.get(i).energy() > history.get(i - 1).energy()) {
        return false;
      }
    }
    return true;
  }

  private static List<String> historyRows(final List<GeometryOptimizationRecord> history) {
    final ArrayList<String> rows = new ArrayList<String>(history.size() + 1);
    rows.add(
        "iteration|energy|gradient_norm|max_gradient_component|accepted_step_angstrom|converged");
    for (int i = 0; i < history.size(); ++i) {
      final GeometryOptimizationRecord record = history.get(i);
      rows.add(
          record.iteration()
              + "|"
              + record.energy()
              + "|"
              + record.gradientNorm()
              + "|"
              + record.maxGradientComponent()
              + "|"
              + record.acceptedStepAngstrom()
              + "|"
              + record.converged());
    }
    return List.copyOf(rows);
  }

  private static void addModelDiagnosticIfNeeded(
      final GeometryEnergyModel energyModel, final List<ClassicalDiagnostic> diagnostics) {
    if (!"bonded_force_field".equals(energyModel.name())) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.GEOMETRY_OPTIMIZATION.name()),
            energyModel.diagnosticMessage()));
  }

  private static List<String> coordinateRows(
      final Molecule molecule, final double[] coordinatesAngstrom) {
    final ArrayList<String> rows = new ArrayList<String>(molecule.atomCount());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final int offset = i * 3;
      rows.add(
          atoms.get(i).id().value()
              + ","
              + atoms.get(i).element().symbol().value()
              + ","
              + coordinatesAngstrom[offset]
              + ","
              + coordinatesAngstrom[offset + 1]
              + ","
              + coordinatesAngstrom[offset + 2]);
    }
    return List.copyOf(rows);
  }

  private static boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    return GeometryOptimizationExecutor.optionEquals(
            request, GEOMETRY_OPTIMIZATION_BACKEND_OPTION, PYSCF_BACKEND)
        || GeometryOptimizationExecutor.optionEquals(
            request, OPTIMIZATION_BACKEND_OPTION, PYSCF_BACKEND);
  }

  private static boolean optionEquals(
      final ClassicalCalculationRequest request, final String name, final String expectedValue) {
    for (int i = 0; i < request.options().size(); ++i) {
      if (!request.options().get(i).name().equals(name)) {
        continue;
      }
      return request.options().get(i).value().equalsIgnoreCase(expectedValue);
    }
    return false;
  }
}
