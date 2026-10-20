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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Строит геометрическую энергию через Java-native single-point electronic calculation.
 */
public final class ElectronicStructureFiniteDifferenceEnergyModel implements GeometryEnergyModel {

  private static final long COORDINATE_CACHE_SCALE = 1_000_000_000L;
  private static final String PYSCF_BACKEND = "pyscf";

  private final ClassicalCalculationRequest sourceRequest;
  private final SinglePointEnergyExecutor singlePointEnergyExecutor;
  private final List<ClassicalCalculationOption> javaNativeSinglePointOptions;
  private final Map<CoordinateKey, Double> energyCache;
  private final AtomicInteger energyEvaluationCount;
  private final AtomicInteger energyCacheHitCount;

  public ElectronicStructureFiniteDifferenceEnergyModel(
      final ClassicalCalculationRequest sourceRequest,
      final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    if (sourceRequest == null) {
      throw new IllegalArgumentException("Electronic geometry source request must not be null.");
    }
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException(
          "Electronic geometry single-point executor must not be null.");
    }
    this.sourceRequest = sourceRequest;
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    this.javaNativeSinglePointOptions =
        ElectronicStructureFiniteDifferenceEnergyModel.javaNativeSinglePointOptions(
            sourceRequest.options());
    this.energyCache = new ConcurrentHashMap<CoordinateKey, Double>();
    this.energyEvaluationCount = new AtomicInteger();
    this.energyCacheHitCount = new AtomicInteger();
  }

  @Override
  public String name() {
    return "electronic_structure_finite_difference";
  }

  @Override
  public String energyUnit() {
    return "hartree";
  }

  @Override
  public String gradientUnit() {
    return "hartree/angstrom";
  }

  @Override
  public String diagnosticMessage() {
    return "Geometry derivative uses finite differences over Java-native electronic single-point energies.";
  }

  @Override
  public String derivativeStencil() {
    return "central_finite_difference_java_native_single_point";
  }

  @Override
  public int energyEvaluationCount() {
    return this.energyEvaluationCount.get();
  }

  @Override
  public int energyCacheHitCount() {
    return this.energyCacheHitCount.get();
  }

  @Override
  public double energy(final Molecule molecule, final double[] coordinatesAngstrom) {
    ElectronicStructureFiniteDifferenceEnergyModel.requireCoordinates(coordinatesAngstrom);
    final CoordinateKey coordinateKey = CoordinateKey.of(coordinatesAngstrom);
    final Double cachedEnergy = this.energyCache.get(coordinateKey);
    if (cachedEnergy != null) {
      this.energyCacheHitCount.incrementAndGet();
      return cachedEnergy.doubleValue();
    }
    return this.energyCache
        .computeIfAbsent(
            coordinateKey,
            ignored -> Double.valueOf(this.evaluateUncachedEnergy(molecule, coordinatesAngstrom)))
        .doubleValue();
  }

  @Override
  public void gradient(
      final Molecule molecule,
      final double[] coordinatesAngstrom,
      final double finiteDifferenceStepAngstrom,
      final double[] gradient) {
    if (!Double.isFinite(finiteDifferenceStepAngstrom) || finiteDifferenceStepAngstrom <= 0.0) {
      throw new IllegalArgumentException(
          "Electronic geometry finite difference step must be positive.");
    }
    ElectronicStructureFiniteDifferenceEnergyModel.requireCoordinates(coordinatesAngstrom);
    if (gradient == null || gradient.length != coordinatesAngstrom.length) {
      throw new IllegalArgumentException(
          "Electronic geometry gradient shape must match coordinates.");
    }
    IntStream.range(0, coordinatesAngstrom.length)
        .parallel()
        .forEach(
            i -> {
              final double[] shifted = coordinatesAngstrom.clone();
              final double original = shifted[i];
              shifted[i] = original + finiteDifferenceStepAngstrom;
              final double plusEnergy = this.energy(molecule, shifted);
              shifted[i] = original - finiteDifferenceStepAngstrom;
              final double minusEnergy = this.energy(molecule, shifted);
              gradient[i] = (plusEnergy - minusEnergy) / (2.0 * finiteDifferenceStepAngstrom);
            });
  }

  private double evaluateUncachedEnergy(
      final Molecule molecule, final double[] coordinatesAngstrom) {
    final Molecule displacedMolecule =
        GeometryCoordinateSupport.withCoordinatesAngstrom(molecule, coordinatesAngstrom);
    final ClassicalCalculationRequest energyRequest =
        ClassicalCalculationRequest.of(
            this.sourceRequest.id(),
            displacedMolecule,
            this.sourceRequest.method(),
            this.sourceRequest.basisSet(),
            this.sourceRequest.activeSpace(),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            this.javaNativeSinglePointOptions,
            this.sourceRequest.metadata());
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();
    final ClassicalResultSection section =
        this.singlePointEnergyExecutor.execute(energyRequest, diagnostics);
    if (section == null) {
      throw new IllegalArgumentException(
          "Electronic geometry energy failed: "
              + ElectronicStructureFiniteDifferenceEnergyModel.diagnosticSummary(diagnostics));
    }
    this.energyEvaluationCount.incrementAndGet();
    return ElectronicStructureFiniteDifferenceEnergyModel.totalEnergy(section);
  }

  private static List<ClassicalCalculationOption> javaNativeSinglePointOptions(
      final List<ClassicalCalculationOption> options) {
    final ArrayList<ClassicalCalculationOption> filteredOptions =
        new ArrayList<ClassicalCalculationOption>();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (ElectronicStructureFiniteDifferenceEnergyModel.geometryOnlyOption(option.name())) {
        continue;
      }
      if (ElectronicStructureFiniteDifferenceEnergyModel.pyScfBackendOption(option)) {
        continue;
      }
      filteredOptions.add(option);
    }
    return List.copyOf(filteredOptions);
  }

  private static boolean geometryOnlyOption(final String name) {
    return name.startsWith("geometry_")
        || name.equals("gradient_backend")
        || name.equals("optimization_backend");
  }

  private static boolean pyScfBackendOption(final ClassicalCalculationOption option) {
    return option.name().endsWith("_backend") && option.value().equalsIgnoreCase(PYSCF_BACKEND);
  }

  private static void requireCoordinates(final double[] coordinatesAngstrom) {
    if (coordinatesAngstrom == null
        || coordinatesAngstrom.length == 0
        || coordinatesAngstrom.length % 3 != 0) {
      throw new IllegalArgumentException(
          "Electronic geometry coordinates must contain atom xyz triples.");
    }
    for (int i = 0; i < coordinatesAngstrom.length; ++i) {
      if (!Double.isFinite(coordinatesAngstrom[i])) {
        throw new IllegalArgumentException(
            "Electronic geometry coordinates contain non-finite value.");
      }
    }
  }

  private static double totalEnergy(final ClassicalResultSection section) {
    final List<ClassicalResultValue> values = section.values();
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalResultValue value = values.get(i);
      if (!"total_energy".equals(value.name())) {
        continue;
      }
      return Double.parseDouble(value.values().get(0));
    }
    throw new IllegalArgumentException(
        "Electronic geometry energy section does not contain total_energy.");
  }

  private static String diagnosticSummary(final List<ClassicalDiagnostic> diagnostics) {
    if (diagnostics.isEmpty()) {
      return "no diagnostics";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (i > 0) {
        builder.append("; ");
      }
      builder.append(diagnostics.get(i).code().name());
      builder.append(": ");
      builder.append(diagnostics.get(i).message());
    }
    return builder.toString();
  }

  private static final class CoordinateKey {

    private final long[] coordinates;

    private CoordinateKey(final long[] coordinates) {
      this.coordinates = coordinates;
    }

    static CoordinateKey of(final double[] coordinatesAngstrom) {
      final long[] roundedCoordinates = new long[coordinatesAngstrom.length];
      for (int i = 0; i < coordinatesAngstrom.length; ++i) {
        roundedCoordinates[i] = Math.round(coordinatesAngstrom[i] * COORDINATE_CACHE_SCALE);
      }
      return new CoordinateKey(roundedCoordinates);
    }

    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof CoordinateKey)) {
        return false;
      }
      final CoordinateKey key = (CoordinateKey) other;
      if (this.coordinates.length != key.coordinates.length) {
        return false;
      }
      for (int i = 0; i < this.coordinates.length; ++i) {
        if (this.coordinates[i] != key.coordinates[i]) {
          return false;
        }
      }
      return true;
    }

    public int hashCode() {
      int result = 1;
      for (int i = 0; i < this.coordinates.length; ++i) {
        final long value = this.coordinates[i];
        result = 31 * result + Long.hashCode(value);
      }
      return result;
    }
  }
}
