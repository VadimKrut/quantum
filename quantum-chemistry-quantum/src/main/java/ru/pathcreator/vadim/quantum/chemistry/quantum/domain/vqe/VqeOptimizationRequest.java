/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe;

import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;

/**
 * Полная постановка VQE запуска: active space, ansatz, стартовые параметры и mitigation.
 */
public final class VqeOptimizationRequest {

  private final ActiveSpace activeSpace;
  private final VariationalChemistryOptions ansatzOptions;
  private final double[] initialParameters;
  private final VqeOptimizationOptions optimizationOptions;
  private final ErrorMitigationRequest mitigationRequest;

  private VqeOptimizationRequest(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final double[] initialParameters,
      final VqeOptimizationOptions optimizationOptions,
      final ErrorMitigationRequest mitigationRequest) {
    this.activeSpace = activeSpace;
    this.ansatzOptions = ansatzOptions;
    this.initialParameters = initialParameters;
    this.optimizationOptions = optimizationOptions;
    this.mitigationRequest = mitigationRequest;
  }

  public static VqeOptimizationRequest of(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final double[] initialParameters,
      final VqeOptimizationOptions optimizationOptions,
      final ErrorMitigationRequest mitigationRequest) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("VQE active space must not be null.");
    }
    if (ansatzOptions == null) {
      throw new IllegalArgumentException("VQE ansatz options must not be null.");
    }
    if (optimizationOptions == null) {
      throw new IllegalArgumentException("VQE optimization options must not be null.");
    }
    if (mitigationRequest == null) {
      throw new IllegalArgumentException("VQE mitigation request must not be null.");
    }
    return new VqeOptimizationRequest(
        activeSpace,
        ansatzOptions,
        VqeOptimizationRequest.requireParameters(initialParameters),
        optimizationOptions,
        mitigationRequest);
  }

  public ActiveSpace activeSpace() {
    return this.activeSpace;
  }

  public VariationalChemistryOptions ansatzOptions() {
    return this.ansatzOptions;
  }

  public double[] initialParametersCopy() {
    return this.initialParameters.clone();
  }

  public VqeOptimizationOptions optimizationOptions() {
    return this.optimizationOptions;
  }

  public ErrorMitigationRequest mitigationRequest() {
    return this.mitigationRequest;
  }

  private static double[] requireParameters(final double[] parameters) {
    if (parameters == null || parameters.length == 0) {
      throw new IllegalArgumentException("VQE initial parameters must not be empty.");
    }
    final double[] checked = parameters.clone();
    for (int i = 0; i < checked.length; ++i) {
      if (Double.isFinite(checked[i])) {
        continue;
      }
      throw new IllegalArgumentException("VQE initial parameter must be finite.");
    }
    return checked;
  }
}
