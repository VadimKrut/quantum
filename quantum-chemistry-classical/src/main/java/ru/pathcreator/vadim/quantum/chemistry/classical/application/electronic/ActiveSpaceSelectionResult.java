/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/**
 * Результат выбора active space с объяснимыми границами orbital window.
 */
public final class ActiveSpaceSelectionResult {

  private final ActiveSpace activeSpace;
  private final int firstActiveOrbital;
  private final int exclusiveLastActiveOrbital;
  private final int homoOrbital;
  private final int lumoOrbital;
  private final double homoEnergyHartree;
  private final double lumoEnergyHartree;
  private final double homoLumoGapHartree;
  private final int selectedOccupiedOrbitalCount;
  private final int selectedVirtualOrbitalCount;
  private final double activeElectronFraction;
  private final double activeOrbitalFraction;
  private final double selectionQualityScore;
  private final String selectionQualityBand;
  private final List<String> selectionDiagnostics;
  private final List<String> orbitalRows;

  private ActiveSpaceSelectionResult(
      final ActiveSpace activeSpace,
      final int firstActiveOrbital,
      final int exclusiveLastActiveOrbital,
      final int homoOrbital,
      final int lumoOrbital,
      final double homoEnergyHartree,
      final double lumoEnergyHartree,
      final double homoLumoGapHartree,
      final int selectedOccupiedOrbitalCount,
      final int selectedVirtualOrbitalCount,
      final double activeElectronFraction,
      final double activeOrbitalFraction,
      final double selectionQualityScore,
      final String selectionQualityBand,
      final List<String> selectionDiagnostics,
      final List<String> orbitalRows) {
    this.activeSpace = activeSpace;
    this.firstActiveOrbital = firstActiveOrbital;
    this.exclusiveLastActiveOrbital = exclusiveLastActiveOrbital;
    this.homoOrbital = homoOrbital;
    this.lumoOrbital = lumoOrbital;
    this.homoEnergyHartree = homoEnergyHartree;
    this.lumoEnergyHartree = lumoEnergyHartree;
    this.homoLumoGapHartree = homoLumoGapHartree;
    this.selectedOccupiedOrbitalCount = selectedOccupiedOrbitalCount;
    this.selectedVirtualOrbitalCount = selectedVirtualOrbitalCount;
    this.activeElectronFraction = activeElectronFraction;
    this.activeOrbitalFraction = activeOrbitalFraction;
    this.selectionQualityScore = selectionQualityScore;
    this.selectionQualityBand = selectionQualityBand;
    this.selectionDiagnostics = selectionDiagnostics;
    this.orbitalRows = orbitalRows;
  }

  public static ActiveSpaceSelectionResult of(
      final ActiveSpace activeSpace,
      final int firstActiveOrbital,
      final int exclusiveLastActiveOrbital,
      final int homoOrbital,
      final int lumoOrbital,
      final double homoEnergyHartree,
      final double lumoEnergyHartree,
      final double homoLumoGapHartree,
      final int selectedOccupiedOrbitalCount,
      final int selectedVirtualOrbitalCount,
      final double activeElectronFraction,
      final double activeOrbitalFraction,
      final double selectionQualityScore,
      final String selectionQualityBand,
      final List<String> selectionDiagnostics,
      final List<String> orbitalRows) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Active-space selection active space must not be null.");
    }
    if (firstActiveOrbital < 0 || exclusiveLastActiveOrbital <= firstActiveOrbital) {
      throw new IllegalArgumentException("Active-space selection orbital window is invalid.");
    }
    ActiveSpaceSelectionResult.requireFinite(homoEnergyHartree, "HOMO energy");
    ActiveSpaceSelectionResult.requireFinite(lumoEnergyHartree, "LUMO energy");
    ActiveSpaceSelectionResult.requireFinite(homoLumoGapHartree, "HOMO-LUMO gap");
    if (selectedOccupiedOrbitalCount < 1 || selectedVirtualOrbitalCount < 1) {
      throw new IllegalArgumentException(
          "Active-space selection must include occupied and virtual orbitals.");
    }
    ActiveSpaceSelectionResult.requireFraction(activeElectronFraction, "active electron fraction");
    ActiveSpaceSelectionResult.requireFraction(activeOrbitalFraction, "active orbital fraction");
    ActiveSpaceSelectionResult.requireFraction(selectionQualityScore, "selection quality score");
    if (selectionQualityBand == null || selectionQualityBand.isBlank()) {
      throw new IllegalArgumentException("Active-space selection quality band must not be blank.");
    }
    return new ActiveSpaceSelectionResult(
        activeSpace,
        firstActiveOrbital,
        exclusiveLastActiveOrbital,
        homoOrbital,
        lumoOrbital,
        homoEnergyHartree,
        lumoEnergyHartree,
        homoLumoGapHartree,
        selectedOccupiedOrbitalCount,
        selectedVirtualOrbitalCount,
        activeElectronFraction,
        activeOrbitalFraction,
        selectionQualityScore,
        selectionQualityBand,
        List.copyOf(selectionDiagnostics),
        List.copyOf(orbitalRows));
  }

  public ActiveSpace activeSpace() {
    return this.activeSpace;
  }

  public int firstActiveOrbital() {
    return this.firstActiveOrbital;
  }

  public int exclusiveLastActiveOrbital() {
    return this.exclusiveLastActiveOrbital;
  }

  public int coreOrbitalCount() {
    return this.firstActiveOrbital;
  }

  public int virtualOrbitalCount(final int totalOrbitalCount) {
    return totalOrbitalCount - this.exclusiveLastActiveOrbital;
  }

  public int homoOrbital() {
    return this.homoOrbital;
  }

  public int lumoOrbital() {
    return this.lumoOrbital;
  }

  public double homoEnergyHartree() {
    return this.homoEnergyHartree;
  }

  public double lumoEnergyHartree() {
    return this.lumoEnergyHartree;
  }

  public double homoLumoGapHartree() {
    return this.homoLumoGapHartree;
  }

  public int selectedOccupiedOrbitalCount() {
    return this.selectedOccupiedOrbitalCount;
  }

  public int selectedVirtualOrbitalCount() {
    return this.selectedVirtualOrbitalCount;
  }

  public double activeElectronFraction() {
    return this.activeElectronFraction;
  }

  public double activeOrbitalFraction() {
    return this.activeOrbitalFraction;
  }

  public double selectionQualityScore() {
    return this.selectionQualityScore;
  }

  public String selectionQualityBand() {
    return this.selectionQualityBand;
  }

  public List<String> selectionDiagnostics() {
    return this.selectionDiagnostics;
  }

  public List<String> orbitalRows() {
    return this.orbitalRows;
  }

  private static void requireFinite(final double value, final String subjectName) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subjectName + " must be finite.");
    }
  }

  private static void requireFraction(final double value, final String subjectName) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(subjectName + " must be in [0, 1].");
    }
  }
}
