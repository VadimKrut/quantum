/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.redox;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ElectronTransferTransition {

  private final RedoxCenter center;
  private final String reducedStateLabel;
  private final String oxidizedStateLabel;
  private final int electronCount;
  private final Double formalPotentialVolts;

  private ElectronTransferTransition(
      final RedoxCenter center,
      final String reducedStateLabel,
      final String oxidizedStateLabel,
      final int electronCount,
      final Double formalPotentialVolts) {
    this.center = center;
    this.reducedStateLabel = reducedStateLabel;
    this.oxidizedStateLabel = oxidizedStateLabel;
    this.electronCount = electronCount;
    this.formalPotentialVolts = formalPotentialVolts;
  }

  public static ElectronTransferTransition of(
      final RedoxCenter center,
      final String reducedStateLabel,
      final String oxidizedStateLabel,
      final int electronCount,
      final Double formalPotentialVolts) {
    String checkedOxidizedLabel;
    if (center == null) {
      throw new IllegalArgumentException("Electron-transfer center must not be null.");
    }
    final String checkedReducedLabel = TextValue.requireText(reducedStateLabel, "Reduced state label");
    if (checkedReducedLabel.equals(
        checkedOxidizedLabel = TextValue.requireText(oxidizedStateLabel, "Oxidized state label"))) {
      throw new IllegalArgumentException("Electron-transfer state labels must be different.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electron-transfer electron count must be positive.");
    }
    if (formalPotentialVolts != null && !Double.isFinite(formalPotentialVolts)) {
      throw new IllegalArgumentException("Formal redox potential must be finite.");
    }
    return new ElectronTransferTransition(
        center, checkedReducedLabel, checkedOxidizedLabel, electronCount, formalPotentialVolts);
  }

  public RedoxCenter center() {
    return this.center;
  }

  public String reducedStateLabel() {
    return this.reducedStateLabel;
  }

  public String oxidizedStateLabel() {
    return this.oxidizedStateLabel;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public Double formalPotentialVolts() {
    return this.formalPotentialVolts;
  }

  public boolean hasFormalPotential() {
    return this.formalPotentialVolts != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronTransferTransition)) {
      return false;
    }
    final ElectronTransferTransition transition = (ElectronTransferTransition) other;
    return this.electronCount == transition.electronCount
        && Objects.equals(this.center, transition.center)
        && Objects.equals(this.reducedStateLabel, transition.reducedStateLabel)
        && Objects.equals(this.oxidizedStateLabel, transition.oxidizedStateLabel)
        && Objects.equals(this.formalPotentialVolts, transition.formalPotentialVolts);
  }

  public int hashCode() {
    return Objects.hash(
        this.center,
        this.reducedStateLabel,
        this.oxidizedStateLabel,
        this.electronCount,
        this.formalPotentialVolts);
  }
}