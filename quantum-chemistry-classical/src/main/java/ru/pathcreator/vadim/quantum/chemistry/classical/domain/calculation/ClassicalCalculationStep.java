/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Один шаг исполнимого плана: подготовка, запуск backend, анализ или подготовка Hamiltonian.
 */
public final class ClassicalCalculationStep {

  private final int index;
  private final ClassicalCalculationKind kind;
  private final String name;
  private final List<ClassicalCapability> requiredCapabilities;
  private final ClassicalResourceEstimate resourceEstimate;

  private ClassicalCalculationStep(
      final int index,
      final ClassicalCalculationKind kind,
      final String name,
      final List<ClassicalCapability> requiredCapabilities,
      final ClassicalResourceEstimate resourceEstimate
  ) {
    this.index = index;
    this.kind = kind;
    this.name = name;
    this.requiredCapabilities = requiredCapabilities;
    this.resourceEstimate = resourceEstimate;
  }

  public static ClassicalCalculationStep of(
      final int index,
      final ClassicalCalculationKind kind,
      final String name,
      final List<ClassicalCapability> requiredCapabilities,
      final ClassicalResourceEstimate resourceEstimate
  ) {
    if (index < 0) {
      throw new IllegalArgumentException("Classical calculation step index must be non-negative.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Classical calculation step kind must not be null.");
    }
    if (resourceEstimate == null) {
      throw new IllegalArgumentException("Classical calculation step resource estimate must not be null.");
    }
    return new ClassicalCalculationStep(
        index,
        kind,
        TextValue.requireText(
            name,
            "Classical calculation step name"),
        List.copyOf(ClassicalCalculationStep.requireCapabilities(requiredCapabilities)),
        resourceEstimate);
  }

  public int index() {
    return this.index;
  }

  public ClassicalCalculationKind kind() {
    return this.kind;
  }

  public String name() {
    return this.name;
  }

  public List<ClassicalCapability> requiredCapabilities() {
    return this.requiredCapabilities;
  }

  public ClassicalResourceEstimate resourceEstimate() {
    return this.resourceEstimate;
  }

  private static List<ClassicalCapability> requireCapabilities(
      final List<ClassicalCapability> capabilities
  ) {
    if (capabilities == null) {
      return List.of();
    }
    for (int i = 0; i < capabilities.size(); ++i) {
      if (capabilities.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Classical calculation step capability must not be null.");
    }
    return capabilities;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationStep)) {
      return false;
    }
    final ClassicalCalculationStep step = (ClassicalCalculationStep) other;
    return this.index == step.index
        && this.kind == step.kind
        && Objects.equals(this.name, step.name)
        && Objects.equals(this.requiredCapabilities, step.requiredCapabilities)
        && Objects.equals(this.resourceEstimate, step.resourceEstimate);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.index);
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.name);
    result = ChemistryHash.include(result, this.requiredCapabilities);
    result = ChemistryHash.include(result, this.resourceEstimate);
    return result;
  }
}