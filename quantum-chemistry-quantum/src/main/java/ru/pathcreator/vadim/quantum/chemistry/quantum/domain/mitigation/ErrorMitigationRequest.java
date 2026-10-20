/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/**
 * Запрос на построение mitigation plan для quantum chemistry программы.
 */
public final class ErrorMitigationRequest {

  private final ActiveSpace activeSpace;
  private final QuantumNoiseModel noiseModel;
  private final List<ErrorMitigationTechnique> allowedTechniques;
  private final int baseShots;
  private final boolean requireParticleNumberConservation;
  private final boolean requireSpinParityConservation;

  private ErrorMitigationRequest(
      final ActiveSpace activeSpace,
      final QuantumNoiseModel noiseModel,
      final List<ErrorMitigationTechnique> allowedTechniques,
      final int baseShots,
      final boolean requireParticleNumberConservation,
      final boolean requireSpinParityConservation) {
    this.activeSpace = activeSpace;
    this.noiseModel = noiseModel;
    this.allowedTechniques = allowedTechniques;
    this.baseShots = baseShots;
    this.requireParticleNumberConservation = requireParticleNumberConservation;
    this.requireSpinParityConservation = requireSpinParityConservation;
  }

  public static ErrorMitigationRequest of(
      final ActiveSpace activeSpace,
      final QuantumNoiseModel noiseModel,
      final List<ErrorMitigationTechnique> allowedTechniques,
      final int baseShots,
      final boolean requireParticleNumberConservation,
      final boolean requireSpinParityConservation) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Error mitigation active space must not be null.");
    }
    if (noiseModel == null) {
      throw new IllegalArgumentException("Error mitigation noise model must not be null.");
    }
    if (baseShots <= 0) {
      throw new IllegalArgumentException("Error mitigation base shot count must be positive.");
    }
    return new ErrorMitigationRequest(
        activeSpace,
        noiseModel,
        List.copyOf(ErrorMitigationRequest.requireTechniques(allowedTechniques)),
        baseShots,
        requireParticleNumberConservation,
        requireSpinParityConservation);
  }

  public ActiveSpace activeSpace() {
    return this.activeSpace;
  }

  public QuantumNoiseModel noiseModel() {
    return this.noiseModel;
  }

  public List<ErrorMitigationTechnique> allowedTechniques() {
    return this.allowedTechniques;
  }

  public int baseShots() {
    return this.baseShots;
  }

  public boolean requireParticleNumberConservation() {
    return this.requireParticleNumberConservation;
  }

  public boolean requireSpinParityConservation() {
    return this.requireSpinParityConservation;
  }

  private static List<ErrorMitigationTechnique> requireTechniques(
      final List<ErrorMitigationTechnique> techniques) {
    if (techniques == null || techniques.isEmpty()) {
      throw new IllegalArgumentException("Error mitigation technique list must not be empty.");
    }
    for (int i = 0; i < techniques.size(); ++i) {
      if (techniques.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Error mitigation technique list contains null value.");
    }
    return techniques;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ErrorMitigationRequest)) {
      return false;
    }
    final ErrorMitigationRequest request = (ErrorMitigationRequest) other;
    return this.baseShots == request.baseShots
        && this.requireParticleNumberConservation == request.requireParticleNumberConservation
        && this.requireSpinParityConservation == request.requireSpinParityConservation
        && Objects.equals(this.activeSpace, request.activeSpace)
        && Objects.equals(this.noiseModel, request.noiseModel)
        && Objects.equals(this.allowedTechniques, request.allowedTechniques);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.activeSpace);
    result = ChemistryHash.include(result, this.noiseModel);
    result = ChemistryHash.include(result, this.allowedTechniques);
    result = ChemistryHash.include(result, this.baseShots);
    result = ChemistryHash.include(result, this.requireParticleNumberConservation);
    result = ChemistryHash.include(result, this.requireSpinParityConservation);
    return result;
  }
}
