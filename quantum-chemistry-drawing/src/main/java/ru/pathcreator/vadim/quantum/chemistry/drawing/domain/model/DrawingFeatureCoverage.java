/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Матрица покрытия: показывает, какие области chemistry-core поддержаны редактором.
 */
public final class DrawingFeatureCoverage {

  private final EnumSet<ChemistryDrawingFeature> features;

  private DrawingFeatureCoverage(final EnumSet<ChemistryDrawingFeature> features) {
    this.features = features;
  }

  public static DrawingFeatureCoverage full() {
    return new DrawingFeatureCoverage(EnumSet.allOf(ChemistryDrawingFeature.class));
  }

  public boolean supports(final ChemistryDrawingFeature feature) {
    return feature != null && this.features.contains(feature);
  }

  public Set<ChemistryDrawingFeature> features() {
    return Set.copyOf(this.features);
  }

  public boolean completeForCurrentCore() {
    return this.features.size() == ChemistryDrawingFeature.values().length;
  }
}