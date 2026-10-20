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
 * Считает локальный градиент текущей молекулярной геометрии без запуска оптимизации.
 */
public final class GeometryGradientExecutor {

  private static final String GEOMETRY_GRADIENT_BACKEND_OPTION = "geometry_gradient_backend";
  private static final String GRADIENT_BACKEND_OPTION = "gradient_backend";
  private static final String PYSCF_BACKEND = "pyscf";

  private final BondedForceFieldEnergyModel forceFieldEnergyModel;
  private final SinglePointEnergyExecutor singlePointEnergyExecutor;
  private final PyScfGeometryGradientExecutor pyScfGeometryGradientExecutor;

  public GeometryGradientExecutor() {
    this(
        new BondedForceFieldEnergyModel(),
        new SinglePointEnergyExecutor(),
        new PyScfGeometryGradientExecutor());
  }

  public GeometryGradientExecutor(
      final BondedForceFieldEnergyModel forceFieldEnergyModel,
      final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    this(forceFieldEnergyModel, singlePointEnergyExecutor, new PyScfGeometryGradientExecutor());
  }

  public GeometryGradientExecutor(
      final BondedForceFieldEnergyModel forceFieldEnergyModel,
      final SinglePointEnergyExecutor singlePointEnergyExecutor,
      final PyScfGeometryGradientExecutor pyScfGeometryGradientExecutor) {
    if (forceFieldEnergyModel == null) {
      throw new IllegalArgumentException(
          "Geometry gradient executor energy model must not be null.");
    }
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException(
          "Geometry gradient single-point executor must not be null.");
    }
    if (pyScfGeometryGradientExecutor == null) {
      throw new IllegalArgumentException("PySCF geometry gradient executor must not be null.");
    }
    this.forceFieldEnergyModel = forceFieldEnergyModel;
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    this.pyScfGeometryGradientExecutor = pyScfGeometryGradientExecutor;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Geometry gradient requires molecule subject."));
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
    if (GeometryGradientExecutor.pyScfBackendRequested(request)) {
      return this.pyScfGeometryGradientExecutor.execute(request, molecule, diagnostics);
    }
    final GeometryEnergyModel energyModel = this.energyModel(request, options);
    final double[] coordinates = GeometryCoordinateSupport.coordinatesAngstrom(molecule);
    final double[] gradient = new double[coordinates.length];
    energyModel.gradient(molecule, coordinates, options.finiteDifferenceStepAngstrom(), gradient);
    GeometryGradientExecutor.addModelDiagnosticIfNeeded(energyModel, diagnostics);
    return ClassicalResultSection.of(
        ClassicalCalculationKind.GEOMETRY_GRADIENT,
        "Local Java-native geometry gradient",
        GeometryGradientExecutor.values(
            molecule,
            coordinates,
            gradient,
            energyModel.energy(molecule, coordinates),
            energyModel));
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
      final double[] coordinates,
      final double[] gradient,
      final double energy,
      final GeometryEnergyModel energyModel) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    GeometryGradientExecutor.addQualityValues(values, energyModel);
    values.add(ClassicalResultValue.text("backend", "java_native"));
    values.add(ClassicalResultValue.text("energy_model", energyModel.name()));
    values.add(
        ClassicalResultValue.text("method", GeometryGradientExecutor.methodName(energyModel)));
    values.add(ClassicalResultValue.text("derivative_stencil", energyModel.derivativeStencil()));
    values.add(ClassicalResultValue.scalar("atom_count", molecule.atomCount(), "count"));
    values.add(ClassicalResultValue.scalar("coordinate_count", coordinates.length, "count"));
    values.add(ClassicalResultValue.scalar("gradient_component_count", gradient.length, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "energy_evaluation_count", energyModel.energyEvaluationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "energy_cache_hit_count", energyModel.energyCacheHitCount(), "count"));
    values.add(ClassicalResultValue.scalar("energy", energy, energyModel.energyUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "gradient_norm", GeometryCoordinateSupport.norm(gradient), energyModel.gradientUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "gradient_rms",
            GeometryGradientExecutor.rootMeanSquare(gradient),
            energyModel.gradientUnit()));
    values.add(
        ClassicalResultValue.scalar(
            "max_gradient_component",
            GeometryCoordinateSupport.maxAbsolute(gradient),
            energyModel.gradientUnit()));
    values.add(
        ClassicalResultValue.of(
            "gradient_rows",
            ClassicalResultValueKind.TABLE,
            energyModel.gradientUnit(),
            GeometryGradientExecutor.gradientRows(molecule, coordinates, gradient)));
    return List.copyOf(values);
  }

  private static void addQualityValues(
      final List<ClassicalResultValue> values, final GeometryEnergyModel energyModel) {
    if ("bonded_force_field".equals(energyModel.name())) {
      ClassicalResultQualityValues.addApproximate(
          values,
          "java_native",
          "bonded_force_field_gradient",
          "geometry_gradient",
          "Bonded force-field gradients are fast structural approximations, not electronic-structure gradients.");
      return;
    }
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "electronic_structure_finite_difference_gradient",
        "geometry_gradient");
  }

  private static String methodName(final GeometryEnergyModel energyModel) {
    if ("bonded_force_field".equals(energyModel.name())) {
      return "bonded_force_field_finite_difference_gradient";
    }
    return energyModel.name() + "_gradient";
  }

  private static double rootMeanSquare(final double[] values) {
    if (values.length == 0) {
      return 0.0;
    }
    double sumSquares = 0.0;
    for (int i = 0; i < values.length; ++i) {
      sumSquares += values[i] * values[i];
    }
    return Math.sqrt(sumSquares / (double) values.length);
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
                "calculationKind", ClassicalCalculationKind.GEOMETRY_GRADIENT.name()),
            energyModel.diagnosticMessage()));
  }

  private static List<String> gradientRows(
      final Molecule molecule, final double[] coordinates, final double[] gradient) {
    final ArrayList<String> rows = new ArrayList<String>(molecule.atomCount());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final int offset = i * 3;
      final double atomNorm =
          Math.sqrt(
              gradient[offset] * gradient[offset]
                  + gradient[offset + 1] * gradient[offset + 1]
                  + gradient[offset + 2] * gradient[offset + 2]);
      rows.add(
          atoms.get(i).id().value()
              + ","
              + atoms.get(i).element().symbol().value()
              + ","
              + coordinates[offset]
              + ","
              + coordinates[offset + 1]
              + ","
              + coordinates[offset + 2]
              + ","
              + gradient[offset]
              + ","
              + gradient[offset + 1]
              + ","
              + gradient[offset + 2]
              + ","
              + atomNorm);
    }
    return List.copyOf(rows);
  }

  private static boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    return GeometryGradientExecutor.optionEquals(
            request, GEOMETRY_GRADIENT_BACKEND_OPTION, PYSCF_BACKEND)
        || GeometryGradientExecutor.optionEquals(request, GRADIENT_BACKEND_OPTION, PYSCF_BACKEND);
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
