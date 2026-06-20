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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Immutable-план классического расчета после preflight.
 */
public final class ClassicalCalculationPlan {

  private final ClassicalCalculationRequest request;
  private final ClassicalPreflightResult preflightResult;
  private final List<ClassicalCalculationStep> steps;
  private final ClassicalResourceEstimate totalResourceEstimate;

  private ClassicalCalculationPlan(
      final ClassicalCalculationRequest request,
      final ClassicalPreflightResult preflightResult,
      final List<ClassicalCalculationStep> steps,
      final ClassicalResourceEstimate totalResourceEstimate
  ) {
    this.request = request;
    this.preflightResult = preflightResult;
    this.steps = steps;
    this.totalResourceEstimate = totalResourceEstimate;
  }

  public static ClassicalCalculationPlan of(
      final ClassicalCalculationRequest request,
      final ClassicalPreflightResult preflightResult,
      final List<ClassicalCalculationStep> steps,
      final ClassicalResourceEstimate totalResourceEstimate
  ) {
    if (request == null) {
      throw new IllegalArgumentException("Classical calculation plan request must not be null.");
    }
    if (preflightResult == null) {
      throw new IllegalArgumentException("Classical calculation plan preflight must not be null.");
    }
    if (totalResourceEstimate == null) {
      throw new IllegalArgumentException("Classical calculation plan resource estimate must not be null.");
    }
    return new ClassicalCalculationPlan(
        request,
        preflightResult,
        List.copyOf(ClassicalCalculationPlan.requireSteps(steps)),
        totalResourceEstimate);
  }

  public ClassicalCalculationRequest request() {
    return this.request;
  }

  public ClassicalPreflightResult preflightResult() {
    return this.preflightResult;
  }

  public List<ClassicalCalculationStep> steps() {
    return this.steps;
  }

  public ClassicalResourceEstimate totalResourceEstimate() {
    return this.totalResourceEstimate;
  }

  private static List<ClassicalCalculationStep> requireSteps(
      final List<ClassicalCalculationStep> steps
  ) {
    if (steps == null || steps.isEmpty()) {
      throw new IllegalArgumentException("Classical calculation plan steps must not be empty.");
    }
    for (int i = 0; i < steps.size(); ++i) {
      final ClassicalCalculationStep step = steps.get(i);
      if (step == null) {
        throw new IllegalArgumentException("Classical calculation plan step must not be null.");
      }
      if (step.index() == i) {
        continue;
      }
      throw new IllegalArgumentException("Classical calculation plan step index must match order.");
    }
    return steps;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationPlan)) {
      return false;
    }
    final ClassicalCalculationPlan plan = (ClassicalCalculationPlan) other;
    return Objects.equals(this.request, plan.request)
        && Objects.equals(this.preflightResult, plan.preflightResult)
        && Objects.equals(this.steps, plan.steps)
        && Objects.equals(this.totalResourceEstimate, plan.totalResourceEstimate);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.request);
    result = ChemistryHash.include(result, this.preflightResult);
    result = ChemistryHash.include(result, this.steps);
    result = ChemistryHash.include(result, this.totalResourceEstimate);
    return result;
  }
}