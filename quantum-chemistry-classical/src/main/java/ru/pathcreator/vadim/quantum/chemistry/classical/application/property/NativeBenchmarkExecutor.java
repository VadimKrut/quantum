/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPreflight;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;

/** Выполняет Java-native basis/method benchmark поверх локального single-point energy executor. */
public final class NativeBenchmarkExecutor {

  private static final String BASIS_LIST_OPTION = "basis_set_list";
  private static final String METHOD_LIST_OPTION = "method_list";
  private static final String MAX_ORBITALS_OPTION = "native_benchmark_max_orbitals";
  private static final int DEFAULT_MAX_ORBITALS = 8;

  private final SinglePointEnergyExecutor singlePointEnergyExecutor;
  private final ElectronicStructurePreparationPipeline preparationPipeline;

  public NativeBenchmarkExecutor() {
    this(new SinglePointEnergyExecutor(), new ElectronicStructurePreparationPipeline());
  }

  public NativeBenchmarkExecutor(final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    this(singlePointEnergyExecutor, new ElectronicStructurePreparationPipeline());
  }

  public NativeBenchmarkExecutor(
      final SinglePointEnergyExecutor singlePointEnergyExecutor,
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException(
          "Native benchmark single-point executor must not be null.");
    }
    if (preparationPipeline == null) {
      throw new IllegalArgumentException("Native benchmark preparation pipeline must not be null.");
    }
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    this.preparationPipeline = preparationPipeline;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Native benchmark request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Native benchmark kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Native benchmark diagnostics target must not be null.");
    }
    if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE) {
      return this.basisSetConvergence(request, diagnostics);
    }
    if (kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
      return this.methodBenchmark(request, diagnostics);
    }
    throw new IllegalArgumentException("Native benchmark kind is not supported: " + kind.name());
  }

  private ClassicalResultSection basisSetConvergence(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    final List<BasisSet> basisSets =
        NativeBenchmarkExecutor.basisSets(
            NativeBenchmarkExecutor.textOption(
                request.options(), BASIS_LIST_OPTION, "STO-3G,6-31G"));
    final ArrayList<String> rows = new ArrayList<String>(basisSets.size());
    final ArrayList<BasisConvergencePoint> successfulPoints =
        new ArrayList<BasisConvergencePoint>(basisSets.size());
    double firstEnergy = Double.NaN;
    for (int i = 0; i < basisSets.size(); ++i) {
      final BasisSet basisSet = basisSets.get(i);
      final BenchmarkSinglePoint point =
          this.singlePoint(request, request.method(), basisSet, diagnostics, "basis_" + i);
      if (point.successful()) {
        if (Double.isNaN(firstEnergy)) {
          firstEnergy = point.energy();
        }
        successfulPoints.add(
            BasisConvergencePoint.of(
                basisSet.name().value(),
                NativeBenchmarkExecutor.basisCardinalNumber(basisSet.name().value()),
                point.energy(),
                point.converged()));
      }
      rows.add(
          "basis\t"
              + request.method().name().value()
              + "\t"
              + basisSet.name().value()
              + "\t"
              + NativeBenchmarkExecutor.energyCell(point)
              + "\t"
              + NativeBenchmarkExecutor.deltaCell(point, firstEnergy)
              + "\t"
              + point.converged()
              + "\t"
              + point.message());
    }
    final BasisConvergenceSummary summary =
        NativeBenchmarkExecutor.basisConvergenceSummary(successfulPoints);
    return NativeBenchmarkExecutor.section(
        ClassicalCalculationKind.BASIS_SET_CONVERGENCE,
        "Java-native basis set convergence",
        "java_native_basis_convergence",
        "java_native_basis_convergence_rows",
        "basis_convergence_rows",
        rows,
        summary,
        null);
  }

  private ClassicalResultSection methodBenchmark(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    final List<ElectronicStructureMethod> methods =
        NativeBenchmarkExecutor.methods(
            NativeBenchmarkExecutor.textOption(
                request.options(), METHOD_LIST_OPTION, "hf,lda,mp2,ccd,ccsd"));
    final ArrayList<String> rows = new ArrayList<String>(methods.size());
    final ArrayList<MethodBenchmarkPoint> successfulPoints =
        new ArrayList<MethodBenchmarkPoint>(methods.size());
    double firstEnergy = Double.NaN;
    for (int i = 0; i < methods.size(); ++i) {
      final ElectronicStructureMethod method = methods.get(i);
      final BenchmarkSinglePoint point =
          this.singlePoint(request, method, request.basisSet(), diagnostics, "method_" + i);
      if (point.successful()) {
        if (Double.isNaN(firstEnergy)) {
          firstEnergy = point.energy();
        }
        successfulPoints.add(
            MethodBenchmarkPoint.of(method.name().value(), point.energy(), point.converged()));
      }
      rows.add(
          "method\t"
              + method.name().value()
              + "\t"
              + request.basisSet().name().value()
              + "\t"
              + NativeBenchmarkExecutor.energyCell(point)
              + "\t"
              + NativeBenchmarkExecutor.deltaCell(point, firstEnergy)
              + "\t"
              + point.converged()
              + "\t"
              + point.message());
    }
    final MethodBenchmarkSummary summary =
        NativeBenchmarkExecutor.methodBenchmarkSummary(successfulPoints);
    return NativeBenchmarkExecutor.section(
        ClassicalCalculationKind.METHOD_BENCHMARK,
        "Java-native method benchmark",
        "java_native_method_benchmark",
        "java_native_method_benchmark_rows",
        "method_benchmark_rows",
        rows,
        null,
        summary);
  }

  private BenchmarkSinglePoint singlePoint(
      final ClassicalCalculationRequest request,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final List<ClassicalDiagnostic> diagnostics,
      final String suffix) {
    final ArrayList<ClassicalDiagnostic> localDiagnostics = new ArrayList<ClassicalDiagnostic>();
    final ClassicalCalculationRequest singlePointRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(request.id().value() + "_" + suffix),
            request.subject(),
            method,
            basisSet,
            request.activeSpace(),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            NativeBenchmarkExecutor.filteredOptions(request.options()),
            request.metadata());
    final BenchmarkSinglePoint preflightBoundary =
        this.preflightBoundary(request, singlePointRequest, diagnostics);
    if (preflightBoundary != null) {
      return preflightBoundary;
    }
    final ClassicalResultSection section =
        this.singlePointEnergyExecutor.execute(singlePointRequest, localDiagnostics);
    if (section == null) {
      diagnostics.addAll(localDiagnostics);
      return BenchmarkSinglePoint.failed("single-point failed");
    }
    final double energy = NativeBenchmarkExecutor.scalar(section, "total_energy");
    final boolean converged = NativeBenchmarkExecutor.booleanValue(section, "converged", true);
    return BenchmarkSinglePoint.succeeded(energy, converged);
  }

  private BenchmarkSinglePoint preflightBoundary(
      final ClassicalCalculationRequest sourceRequest,
      final ClassicalCalculationRequest singlePointRequest,
      final List<ClassicalDiagnostic> diagnostics) {
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(singlePointRequest);
    if (!preflight.hasFullTensorPreflight()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of(
                  "basisSet", singlePointRequest.basisSet().name().value()),
              "Native benchmark skipped this point because full ERI capability preflight is unavailable."));
      return BenchmarkSinglePoint.failed("full ERI preflight unavailable");
    }
    final int maxOrbitals =
        NativeBenchmarkExecutor.integerOption(
            sourceRequest.options(), MAX_ORBITALS_OPTION, DEFAULT_MAX_ORBITALS);
    if (preflight.fullTensorPreflight().orbitalCount() > maxOrbitals) {
      final String message =
          "native benchmark resource boundary: orbital count "
              + preflight.fullTensorPreflight().orbitalCount()
              + " exceeds configured "
              + MAX_ORBITALS_OPTION
              + "="
              + maxOrbitals;
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of(
                  "basisSet", singlePointRequest.basisSet().name().value()),
              message));
      return BenchmarkSinglePoint.failed(message);
    }
    if (!preflight.supported()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of(
                  "basisSet", singlePointRequest.basisSet().name().value()),
              preflight.unsupportedReason()));
      return BenchmarkSinglePoint.failed(preflight.unsupportedReason());
    }
    return null;
  }

  private static ClassicalResultSection section(
      final ClassicalCalculationKind kind,
      final String title,
      final String method,
      final String rowsName,
      final String legacyRowsName,
      final List<String> rows,
      final BasisConvergenceSummary basisConvergenceSummary,
      final MethodBenchmarkSummary methodBenchmarkSummary) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(
        ClassicalResultValue.text("model", "java_native_" + kind.name().toLowerCase(Locale.ROOT)));
    values.add(ClassicalResultValue.text("method", method));
    values.add(ClassicalResultValue.text("backend", "java_native"));
    values.add(
        ClassicalResultValue.text("backend_contract", "java_native_batch_single_point_benchmark"));
    values.add(ClassicalResultValue.text("java_native_calculation", "true"));
    values.add(ClassicalResultValue.text("external_reference_route", "false"));
    values.add(ClassicalResultValue.text("external_package_parity_claimed", "false"));
    values.add(ClassicalResultValue.scalar("row_count", rows.size(), "count"));
    if (basisConvergenceSummary != null) {
      values.add(
          ClassicalResultValue.scalar(
              "basis_sensitivity_score",
              Math.max(1.0, basisConvergenceSummary.successfulPointCount()),
              "score"));
      values.add(
          ClassicalResultValue.scalar(
              "basis_convergence_successful_point_count",
              basisConvergenceSummary.successfulPointCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "basis_convergence_converged_point_count",
              basisConvergenceSummary.convergedPointCount(),
              "count"));
      values.add(
          ClassicalResultValue.text(
              "basis_convergence_monotonic_nonincreasing",
              Boolean.toString(basisConvergenceSummary.monotonicNonIncreasing())));
      values.add(
          ClassicalResultValue.text(
              "basis_convergence_cbs_extrapolation_available",
              Boolean.toString(basisConvergenceSummary.cbsExtrapolationAvailable())));
      values.add(
          NativeBenchmarkExecutor.finiteOrUnavailable(
              "basis_convergence_last_energy", basisConvergenceSummary.lastEnergy(), "hartree"));
      values.add(
          NativeBenchmarkExecutor.finiteOrUnavailable(
              "basis_convergence_cbs_energy_estimate",
              basisConvergenceSummary.cbsEnergyEstimate(),
              "hartree"));
      values.add(
          NativeBenchmarkExecutor.finiteOrUnavailable(
              "basis_convergence_estimated_basis_error",
              basisConvergenceSummary.estimatedBasisError(),
              "hartree"));
    }
    if (methodBenchmarkSummary != null) {
      values.add(
          ClassicalResultValue.scalar(
              "method_sensitivity_score",
              NativeBenchmarkExecutor.methodSensitivityScore(methodBenchmarkSummary),
              "score"));
      values.add(
          ClassicalResultValue.scalar(
              "method_ladder_size",
              Math.max(1.0, methodBenchmarkSummary.successfulMethodCount()),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "method_benchmark_successful_method_count",
              methodBenchmarkSummary.successfulMethodCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "method_benchmark_converged_method_count",
              methodBenchmarkSummary.convergedMethodCount(),
              "count"));
      values.add(
          ClassicalResultValue.text(
              "method_benchmark_lowest_energy_method",
              methodBenchmarkSummary.lowestEnergyMethod()));
      values.add(
          NativeBenchmarkExecutor.finiteOrUnavailable(
              "method_benchmark_lowest_energy", methodBenchmarkSummary.lowestEnergy(), "hartree"));
      values.add(
          NativeBenchmarkExecutor.finiteOrUnavailable(
              "method_benchmark_energy_span", methodBenchmarkSummary.energySpan(), "hartree"));
      values.add(
          ClassicalResultValue.text(
              "method_benchmark_all_converged",
              Boolean.toString(methodBenchmarkSummary.allConverged())));
    }
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
            ? "java_native_basis_convergence"
            : "java_native_method_benchmark",
        kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
            ? "basis_set_convergence"
            : "method_benchmark",
        "Native benchmark compares available internal electronic routes and is not an experimental validation dataset.");
    values.add(
        ClassicalResultValue.of(
            rowsName,
            ClassicalResultValueKind.TABLE,
            "axis,method_or_basis,basis,total_energy,delta_from_first,converged,note",
            List.copyOf(rows)));
    values.add(
        ClassicalResultValue.of(
            legacyRowsName,
            ClassicalResultValueKind.TABLE,
            "axis,method_or_basis,basis,total_energy,delta_from_first,converged,note",
            List.copyOf(rows)));
    return ClassicalResultSection.of(kind, title, values);
  }

  private static ClassicalResultValue finiteOrUnavailable(
      final String name, final double value, final String unit) {
    if (Double.isFinite(value)) {
      return ClassicalResultValue.scalar(name, value, unit);
    }
    return ClassicalResultValue.text(name, "unavailable");
  }

  private static double methodSensitivityScore(final MethodBenchmarkSummary summary) {
    if (summary.successfulMethodCount() == 0 || !Double.isFinite(summary.energySpan())) {
      return 0.0;
    }
    return Math.max(1.0, summary.energySpan());
  }

  private static List<ClassicalCalculationOption> filteredOptions(
      final List<ClassicalCalculationOption> options) {
    final ArrayList<ClassicalCalculationOption> filtered =
        new ArrayList<ClassicalCalculationOption>(options.size());
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (BASIS_LIST_OPTION.equals(option.name()) || METHOD_LIST_OPTION.equals(option.name())) {
        continue;
      }
      if ("benchmark_backend".equals(option.name())
          || "basis_convergence_backend".equals(option.name())
          || "method_benchmark_backend".equals(option.name())) {
        continue;
      }
      filtered.add(option);
    }
    return List.copyOf(filtered);
  }

  private static List<BasisSet> basisSets(final String csv) {
    final String[] tokens = csv.split(",");
    final ArrayList<BasisSet> basisSets = new ArrayList<BasisSet>(tokens.length);
    for (int i = 0; i < tokens.length; ++i) {
      final String value = tokens[i].trim();
      if (value.isEmpty()) {
        continue;
      }
      if ("sto-3g".equalsIgnoreCase(value)) {
        basisSets.add(BasisSet.STO_3G);
      } else if ("6-31g".equalsIgnoreCase(value)) {
        basisSets.add(BasisSet.SIX_31G);
      } else {
        basisSets.add(BasisSet.of(BasisSetName.of(value)));
      }
    }
    if (basisSets.isEmpty()) {
      throw new IllegalArgumentException("Basis set benchmark list must not be empty.");
    }
    return List.copyOf(basisSets);
  }

  private static List<ElectronicStructureMethod> methods(final String csv) {
    final String[] tokens = csv.split(",");
    final ArrayList<ElectronicStructureMethod> methods =
        new ArrayList<ElectronicStructureMethod>(tokens.length);
    for (int i = 0; i < tokens.length; ++i) {
      final String value = tokens[i].trim();
      if (value.isEmpty()) {
        continue;
      }
      methods.add(NativeBenchmarkExecutor.method(value));
    }
    if (methods.isEmpty()) {
      throw new IllegalArgumentException("Method benchmark list must not be empty.");
    }
    return List.copyOf(methods);
  }

  private static BasisConvergenceSummary basisConvergenceSummary(
      final List<BasisConvergencePoint> points) {
    if (points.isEmpty()) {
      return BasisConvergenceSummary.unavailable(0, 0);
    }
    int convergedPointCount = 0;
    boolean monotonicNonIncreasing = true;
    double previousEnergy = Double.NaN;
    BasisConvergencePoint previousCardinalPoint = null;
    BasisConvergencePoint lastCardinalPoint = null;
    for (int i = 0; i < points.size(); ++i) {
      final BasisConvergencePoint point = points.get(i);
      if (point.converged()) {
        ++convergedPointCount;
      }
      if (!Double.isNaN(previousEnergy) && point.energy() > previousEnergy) {
        monotonicNonIncreasing = false;
      }
      previousEnergy = point.energy();
      if (point.cardinalNumber() > 0) {
        previousCardinalPoint = lastCardinalPoint;
        lastCardinalPoint = point;
      }
    }
    final BasisConvergencePoint lastPoint = points.get(points.size() - 1);
    if (previousCardinalPoint == null || lastCardinalPoint == null) {
      return BasisConvergenceSummary.partial(
          points.size(), convergedPointCount, monotonicNonIncreasing, lastPoint.energy());
    }
    final double cbsEnergy =
        NativeBenchmarkExecutor.twoPointCbsEnergy(previousCardinalPoint, lastCardinalPoint);
    return BasisConvergenceSummary.available(
        points.size(), convergedPointCount, monotonicNonIncreasing, lastPoint.energy(), cbsEnergy);
  }

  private static int basisCardinalNumber(final String basisName) {
    final String normalized = basisName.toLowerCase(Locale.ROOT).replace("_", "-");
    if (normalized.contains("sto-3g") || normalized.contains("minimal")) {
      return 1;
    }
    if (normalized.contains("6-31g")
        || normalized.contains("double")
        || normalized.contains("vdz")) {
      return 2;
    }
    if (normalized.contains("triple")
        || normalized.contains("vtz")
        || normalized.contains("tzvp")) {
      return 3;
    }
    if (normalized.contains("quadruple")
        || normalized.contains("vqz")
        || normalized.contains("qzvp")) {
      return 4;
    }
    if (normalized.contains("quintuple")
        || normalized.contains("v5z")
        || normalized.contains("5z")) {
      return 5;
    }
    return 0;
  }

  private static String energyCell(final BenchmarkSinglePoint point) {
    if (!point.successful()) {
      return "unavailable";
    }
    return Double.toString(point.energy());
  }

  private static String deltaCell(final BenchmarkSinglePoint point, final double firstEnergy) {
    if (!point.successful() || Double.isNaN(firstEnergy)) {
      return "unavailable";
    }
    return Double.toString(point.energy() - firstEnergy);
  }

  private static double twoPointCbsEnergy(
      final BasisConvergencePoint first, final BasisConvergencePoint second) {
    final double firstPower = Math.pow(first.cardinalNumber(), 3.0);
    final double secondPower = Math.pow(second.cardinalNumber(), 3.0);
    return (second.energy() * secondPower - first.energy() * firstPower)
        / (secondPower - firstPower);
  }

  private static MethodBenchmarkSummary methodBenchmarkSummary(
      final List<MethodBenchmarkPoint> points) {
    if (points.isEmpty()) {
      return MethodBenchmarkSummary.unavailable();
    }
    int convergedMethodCount = 0;
    double minimumEnergy = Double.POSITIVE_INFINITY;
    double maximumEnergy = Double.NEGATIVE_INFINITY;
    String minimumEnergyMethod = "";
    for (int i = 0; i < points.size(); ++i) {
      final MethodBenchmarkPoint point = points.get(i);
      if (point.converged()) {
        ++convergedMethodCount;
      }
      if (point.energy() < minimumEnergy) {
        minimumEnergy = point.energy();
        minimumEnergyMethod = point.methodName();
      }
      if (point.energy() > maximumEnergy) {
        maximumEnergy = point.energy();
      }
    }
    return MethodBenchmarkSummary.available(
        points.size(),
        convergedMethodCount,
        minimumEnergyMethod,
        minimumEnergy,
        maximumEnergy - minimumEnergy);
  }

  private static ElectronicStructureMethod method(final String rawName) {
    final String normalized = rawName.trim().toLowerCase(Locale.ROOT);
    if ("hf".equals(normalized) || "hartree-fock".equals(normalized)) {
      return ElectronicStructureMethod.HARTREE_FOCK;
    }
    if ("lda".equals(normalized) || "b3lyp".equals(normalized) || "pbe".equals(normalized)) {
      return ElectronicStructureMethod.densityFunctional(rawName);
    }
    if ("mp2".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.PERTURBATION_THEORY,
          ElectronicStructureMethodName.of("MP2"));
    }
    if ("ccd".equals(normalized) || "ccsd".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.COUPLED_CLUSTER,
          ElectronicStructureMethodName.of(rawName.toUpperCase(Locale.ROOT)));
    }
    if ("cisd".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.CONFIGURATION_INTERACTION,
          ElectronicStructureMethodName.of("CISD"));
    }
    if ("casci".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
          ElectronicStructureMethodName.of("CASCI"));
    }
    if ("casscf".equals(normalized) || "mcscf".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
          ElectronicStructureMethodName.of(rawName.toUpperCase(Locale.ROOT)));
    }
    if ("fci".equals(normalized) || "full-ci".equals(normalized) || "full ci".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
          ElectronicStructureMethodName.of("FCI"));
    }
    return ElectronicStructureMethod.of(
        ElectronicStructureMethodType.CUSTOM, ElectronicStructureMethodName.of(rawName));
  }

  private static String textOption(
      final List<ClassicalCalculationOption> options,
      final String name,
      final String defaultValue) {
    for (int i = 0; i < options.size(); ++i) {
      if (name.equals(options.get(i).name())) {
        return options.get(i).value();
      }
    }
    return defaultValue;
  }

  private static int integerOption(
      final List<ClassicalCalculationOption> options, final String name, final int defaultValue) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!name.equals(option.name())) {
        continue;
      }
      try {
        final int value = Integer.parseInt(option.value());
        if (value <= 0) {
          throw new IllegalArgumentException(name + " option must be positive.");
        }
        return value;
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(name + " option must be a positive integer.");
      }
    }
    return defaultValue;
  }

  private static double scalar(final ClassicalResultSection section, final String name) {
    final List<ClassicalResultValue> values = section.values();
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalResultValue value = values.get(i);
      if (name.equals(value.name())) {
        return Double.parseDouble(value.values().get(0));
      }
    }
    return Double.NaN;
  }

  private static boolean booleanValue(
      final ClassicalResultSection section, final String name, final boolean defaultValue) {
    final List<ClassicalResultValue> values = section.values();
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalResultValue value = values.get(i);
      if (name.equals(value.name())) {
        return Boolean.parseBoolean(value.values().get(0));
      }
    }
    return defaultValue;
  }

  private static final class BenchmarkSinglePoint {

    private final boolean successful;
    private final double energy;
    private final boolean converged;
    private final String message;

    private BenchmarkSinglePoint(
        final boolean successful,
        final double energy,
        final boolean converged,
        final String message) {
      this.successful = successful;
      this.energy = energy;
      this.converged = converged;
      this.message = message;
    }

    static BenchmarkSinglePoint succeeded(final double energy, final boolean converged) {
      return new BenchmarkSinglePoint(true, energy, converged, "java_native");
    }

    static BenchmarkSinglePoint failed(final String message) {
      return new BenchmarkSinglePoint(false, Double.NaN, false, message);
    }

    boolean successful() {
      return this.successful;
    }

    double energy() {
      return this.energy;
    }

    boolean converged() {
      return this.converged;
    }

    String message() {
      return this.message;
    }
  }

  private static final class BasisConvergencePoint {

    private final int cardinalNumber;
    private final double energy;
    private final boolean converged;

    private BasisConvergencePoint(
        final int cardinalNumber, final double energy, final boolean converged) {
      this.cardinalNumber = cardinalNumber;
      this.energy = energy;
      this.converged = converged;
    }

    static BasisConvergencePoint of(
        final String basisName,
        final int cardinalNumber,
        final double energy,
        final boolean converged) {
      if (basisName == null || basisName.isBlank()) {
        throw new IllegalArgumentException("Basis convergence point basis name must not be blank.");
      }
      if (!Double.isFinite(energy)) {
        throw new IllegalArgumentException("Basis convergence point energy must be finite.");
      }
      return new BasisConvergencePoint(cardinalNumber, energy, converged);
    }

    int cardinalNumber() {
      return this.cardinalNumber;
    }

    double energy() {
      return this.energy;
    }

    boolean converged() {
      return this.converged;
    }
  }

  private static final class BasisConvergenceSummary {

    private final int successfulPointCount;
    private final int convergedPointCount;
    private final boolean monotonicNonIncreasing;
    private final boolean cbsExtrapolationAvailable;
    private final double lastEnergy;
    private final double cbsEnergyEstimate;
    private final double estimatedBasisError;

    private BasisConvergenceSummary(
        final int successfulPointCount,
        final int convergedPointCount,
        final boolean monotonicNonIncreasing,
        final boolean cbsExtrapolationAvailable,
        final double lastEnergy,
        final double cbsEnergyEstimate,
        final double estimatedBasisError) {
      this.successfulPointCount = successfulPointCount;
      this.convergedPointCount = convergedPointCount;
      this.monotonicNonIncreasing = monotonicNonIncreasing;
      this.cbsExtrapolationAvailable = cbsExtrapolationAvailable;
      this.lastEnergy = lastEnergy;
      this.cbsEnergyEstimate = cbsEnergyEstimate;
      this.estimatedBasisError = estimatedBasisError;
    }

    static BasisConvergenceSummary unavailable(
        final int successfulPointCount, final int convergedPointCount) {
      return new BasisConvergenceSummary(
          successfulPointCount,
          convergedPointCount,
          true,
          false,
          Double.NaN,
          Double.NaN,
          Double.NaN);
    }

    static BasisConvergenceSummary partial(
        final int successfulPointCount,
        final int convergedPointCount,
        final boolean monotonicNonIncreasing,
        final double lastEnergy) {
      return new BasisConvergenceSummary(
          successfulPointCount,
          convergedPointCount,
          monotonicNonIncreasing,
          false,
          lastEnergy,
          Double.NaN,
          Double.NaN);
    }

    static BasisConvergenceSummary available(
        final int successfulPointCount,
        final int convergedPointCount,
        final boolean monotonicNonIncreasing,
        final double lastEnergy,
        final double cbsEnergyEstimate) {
      return new BasisConvergenceSummary(
          successfulPointCount,
          convergedPointCount,
          monotonicNonIncreasing,
          true,
          lastEnergy,
          cbsEnergyEstimate,
          Math.abs(lastEnergy - cbsEnergyEstimate));
    }

    int successfulPointCount() {
      return this.successfulPointCount;
    }

    int convergedPointCount() {
      return this.convergedPointCount;
    }

    boolean monotonicNonIncreasing() {
      return this.monotonicNonIncreasing;
    }

    boolean cbsExtrapolationAvailable() {
      return this.cbsExtrapolationAvailable;
    }

    double lastEnergy() {
      return this.lastEnergy;
    }

    double cbsEnergyEstimate() {
      return this.cbsEnergyEstimate;
    }

    double estimatedBasisError() {
      return this.estimatedBasisError;
    }
  }

  private static final class MethodBenchmarkPoint {

    private final String methodName;
    private final double energy;
    private final boolean converged;

    private MethodBenchmarkPoint(
        final String methodName, final double energy, final boolean converged) {
      this.methodName = methodName;
      this.energy = energy;
      this.converged = converged;
    }

    static MethodBenchmarkPoint of(
        final String methodName, final double energy, final boolean converged) {
      if (methodName == null || methodName.isBlank()) {
        throw new IllegalArgumentException("Method benchmark point method name must not be blank.");
      }
      if (!Double.isFinite(energy)) {
        throw new IllegalArgumentException("Method benchmark point energy must be finite.");
      }
      return new MethodBenchmarkPoint(methodName, energy, converged);
    }

    String methodName() {
      return this.methodName;
    }

    double energy() {
      return this.energy;
    }

    boolean converged() {
      return this.converged;
    }
  }

  private static final class MethodBenchmarkSummary {

    private final int successfulMethodCount;
    private final int convergedMethodCount;
    private final String lowestEnergyMethod;
    private final double lowestEnergy;
    private final double energySpan;

    private MethodBenchmarkSummary(
        final int successfulMethodCount,
        final int convergedMethodCount,
        final String lowestEnergyMethod,
        final double lowestEnergy,
        final double energySpan) {
      this.successfulMethodCount = successfulMethodCount;
      this.convergedMethodCount = convergedMethodCount;
      this.lowestEnergyMethod = lowestEnergyMethod;
      this.lowestEnergy = lowestEnergy;
      this.energySpan = energySpan;
    }

    static MethodBenchmarkSummary unavailable() {
      return new MethodBenchmarkSummary(0, 0, "none", Double.NaN, Double.NaN);
    }

    static MethodBenchmarkSummary available(
        final int successfulMethodCount,
        final int convergedMethodCount,
        final String lowestEnergyMethod,
        final double lowestEnergy,
        final double energySpan) {
      return new MethodBenchmarkSummary(
          successfulMethodCount,
          convergedMethodCount,
          lowestEnergyMethod,
          lowestEnergy,
          energySpan);
    }

    int successfulMethodCount() {
      return this.successfulMethodCount;
    }

    int convergedMethodCount() {
      return this.convergedMethodCount;
    }

    String lowestEnergyMethod() {
      return this.lowestEnergyMethod;
    }

    double lowestEnergy() {
      return this.lowestEnergy;
    }

    double energySpan() {
      return this.energySpan;
    }

    boolean allConverged() {
      return this.successfulMethodCount == this.convergedMethodCount;
    }
  }
}
