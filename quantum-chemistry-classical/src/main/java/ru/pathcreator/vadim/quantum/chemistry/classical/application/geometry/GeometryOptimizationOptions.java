/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOptionKind;

/**
 * Настройки локального Java-native расчета градиентов и релаксации геометрии.
 */
public final class GeometryOptimizationOptions {

  public static final GeometryOptimizationOptions DEFAULT =
      new GeometryOptimizationOptions(
          128, 0.015, 1.0E-5, 1.0E-4, GeometryEnergyModelKind.AUTO, 6, 12);

  private final int maxIterations;
  private final double stepAngstrom;
  private final double finiteDifferenceStepAngstrom;
  private final double gradientTolerance;
  private final GeometryEnergyModelKind energyModelKind;
  private final int autoElectronicMaxAtoms;
  private final int maxLineSearchAttempts;

  private GeometryOptimizationOptions(
      final int maxIterations,
      final double stepAngstrom,
      final double finiteDifferenceStepAngstrom,
      final double gradientTolerance,
      final GeometryEnergyModelKind energyModelKind,
      final int autoElectronicMaxAtoms,
      final int maxLineSearchAttempts) {
    this.maxIterations = maxIterations;
    this.stepAngstrom = stepAngstrom;
    this.finiteDifferenceStepAngstrom = finiteDifferenceStepAngstrom;
    this.gradientTolerance = gradientTolerance;
    this.energyModelKind = energyModelKind;
    this.autoElectronicMaxAtoms = autoElectronicMaxAtoms;
    this.maxLineSearchAttempts = maxLineSearchAttempts;
  }

  public static GeometryOptimizationOptions of(
      final int maxIterations,
      final double stepAngstrom,
      final double finiteDifferenceStepAngstrom,
      final double gradientTolerance,
      final GeometryEnergyModelKind energyModelKind,
      final int autoElectronicMaxAtoms,
      final int maxLineSearchAttempts) {
    if (maxIterations < 1) {
      throw new IllegalArgumentException("Geometry max iterations must be positive.");
    }
    if (!Double.isFinite(stepAngstrom) || stepAngstrom <= 0.0) {
      throw new IllegalArgumentException("Geometry step must be finite and positive.");
    }
    if (!Double.isFinite(finiteDifferenceStepAngstrom) || finiteDifferenceStepAngstrom <= 0.0) {
      throw new IllegalArgumentException(
          "Geometry finite-difference step must be finite and positive.");
    }
    if (!Double.isFinite(gradientTolerance) || gradientTolerance <= 0.0) {
      throw new IllegalArgumentException(
          "Geometry gradient tolerance must be finite and positive.");
    }
    if (energyModelKind == null) {
      throw new IllegalArgumentException("Geometry energy model kind must not be null.");
    }
    if (autoElectronicMaxAtoms < 1) {
      throw new IllegalArgumentException("Geometry auto electronic max atoms must be positive.");
    }
    if (maxLineSearchAttempts < 1) {
      throw new IllegalArgumentException("Geometry max line-search attempts must be positive.");
    }
    return new GeometryOptimizationOptions(
        maxIterations,
        stepAngstrom,
        finiteDifferenceStepAngstrom,
        gradientTolerance,
        energyModelKind,
        autoElectronicMaxAtoms,
        maxLineSearchAttempts);
  }

  public static GeometryOptimizationOptions fromOptions(
      final List<ClassicalCalculationOption> options) {
    int maxIterations = DEFAULT.maxIterations;
    double stepAngstrom = DEFAULT.stepAngstrom;
    double finiteDifferenceStepAngstrom = DEFAULT.finiteDifferenceStepAngstrom;
    double gradientTolerance = DEFAULT.gradientTolerance;
    GeometryEnergyModelKind energyModelKind = DEFAULT.energyModelKind;
    int autoElectronicMaxAtoms = DEFAULT.autoElectronicMaxAtoms;
    int maxLineSearchAttempts = DEFAULT.maxLineSearchAttempts;
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if ("geometry_max_iterations".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.INTEGER);
        maxIterations = Integer.parseInt(option.value());
        continue;
      }
      if ("geometry_step_angstrom".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.DOUBLE);
        stepAngstrom = Double.parseDouble(option.value());
        continue;
      }
      if ("geometry_finite_difference_step_angstrom".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.DOUBLE);
        finiteDifferenceStepAngstrom = Double.parseDouble(option.value());
        continue;
      }
      if ("geometry_gradient_tolerance".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.DOUBLE);
        gradientTolerance = Double.parseDouble(option.value());
        continue;
      }
      if ("geometry_energy_model".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.TEXT);
        energyModelKind = GeometryEnergyModelKind.fromOption(option.value());
        continue;
      }
      if ("geometry_auto_electronic_max_atoms".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.INTEGER);
        autoElectronicMaxAtoms = Integer.parseInt(option.value());
        continue;
      }
      if ("geometry_max_line_search_attempts".equals(option.name())) {
        GeometryOptimizationOptions.requireKind(option, ClassicalCalculationOptionKind.INTEGER);
        maxLineSearchAttempts = Integer.parseInt(option.value());
      }
    }
    return GeometryOptimizationOptions.of(
        maxIterations,
        stepAngstrom,
        finiteDifferenceStepAngstrom,
        gradientTolerance,
        energyModelKind,
        autoElectronicMaxAtoms,
        maxLineSearchAttempts);
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  public double stepAngstrom() {
    return this.stepAngstrom;
  }

  public double finiteDifferenceStepAngstrom() {
    return this.finiteDifferenceStepAngstrom;
  }

  public double gradientTolerance() {
    return this.gradientTolerance;
  }

  public GeometryEnergyModelKind energyModelKind() {
    return this.energyModelKind;
  }

  public int autoElectronicMaxAtoms() {
    return this.autoElectronicMaxAtoms;
  }

  public int maxLineSearchAttempts() {
    return this.maxLineSearchAttempts;
  }

  private static void requireKind(
      final ClassicalCalculationOption option, final ClassicalCalculationOptionKind expectedKind) {
    if (option.kind() != expectedKind) {
      throw new IllegalArgumentException(
          "Geometry option " + option.name() + " must have kind " + expectedKind.name() + ".");
    }
  }
}
