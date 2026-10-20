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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOptionKind;

/**
 * Настройки автоматического выбора active-space окна вокруг frontier orbitals.
 */
public final class ActiveSpaceSelectionOptions {

  public static final String MAX_ORBITALS_OPTION = "active_space_max_orbitals";
  public static final String FRONTIER_PAIR_COUNT_OPTION = "active_space_frontier_pair_count";
  public static final String ENERGY_WINDOW_HARTREE_OPTION = "active_space_energy_window_hartree";
  public static final String MIN_ORBITALS_OPTION = "active_space_min_orbitals";
  public static final ActiveSpaceSelectionOptions DEFAULT =
      new ActiveSpaceSelectionOptions(12, 2, 0.75, 2);

  private final int maxOrbitalCount;
  private final int frontierPairCount;
  private final double energyWindowHartree;
  private final int minOrbitalCount;

  private ActiveSpaceSelectionOptions(
      final int maxOrbitalCount,
      final int frontierPairCount,
      final double energyWindowHartree,
      final int minOrbitalCount) {
    this.maxOrbitalCount = maxOrbitalCount;
    this.frontierPairCount = frontierPairCount;
    this.energyWindowHartree = energyWindowHartree;
    this.minOrbitalCount = minOrbitalCount;
  }

  public static ActiveSpaceSelectionOptions of(
      final int maxOrbitalCount,
      final int frontierPairCount,
      final double energyWindowHartree,
      final int minOrbitalCount) {
    if (maxOrbitalCount <= 0) {
      throw new IllegalArgumentException("Active-space max orbital count must be positive.");
    }
    if (frontierPairCount <= 0) {
      throw new IllegalArgumentException("Active-space frontier pair count must be positive.");
    }
    if (!Double.isFinite(energyWindowHartree) || energyWindowHartree < 0.0) {
      throw new IllegalArgumentException(
          "Active-space energy window must be finite and non-negative.");
    }
    if (minOrbitalCount <= 0 || minOrbitalCount > maxOrbitalCount) {
      throw new IllegalArgumentException("Active-space min orbital count must be in [1, max].");
    }
    return new ActiveSpaceSelectionOptions(
        maxOrbitalCount, frontierPairCount, energyWindowHartree, minOrbitalCount);
  }

  public static ActiveSpaceSelectionOptions fromOptions(
      final List<ClassicalCalculationOption> options) {
    int maxOrbitalCount = DEFAULT.maxOrbitalCount;
    int frontierPairCount = DEFAULT.frontierPairCount;
    double energyWindowHartree = DEFAULT.energyWindowHartree;
    int minOrbitalCount = DEFAULT.minOrbitalCount;
    if (options == null) {
      return DEFAULT;
    }
    for (int optionIndex = 0; optionIndex < options.size(); ++optionIndex) {
      final ClassicalCalculationOption option = options.get(optionIndex);
      if (MAX_ORBITALS_OPTION.equals(option.name())) {
        ActiveSpaceSelectionOptions.requireKind(option, ClassicalCalculationOptionKind.INTEGER);
        maxOrbitalCount = Integer.parseInt(option.value());
      } else if (FRONTIER_PAIR_COUNT_OPTION.equals(option.name())) {
        ActiveSpaceSelectionOptions.requireKind(option, ClassicalCalculationOptionKind.INTEGER);
        frontierPairCount = Integer.parseInt(option.value());
      } else if (ENERGY_WINDOW_HARTREE_OPTION.equals(option.name())) {
        ActiveSpaceSelectionOptions.requireKind(option, ClassicalCalculationOptionKind.DOUBLE);
        energyWindowHartree = Double.parseDouble(option.value());
      } else if (MIN_ORBITALS_OPTION.equals(option.name())) {
        ActiveSpaceSelectionOptions.requireKind(option, ClassicalCalculationOptionKind.INTEGER);
        minOrbitalCount = Integer.parseInt(option.value());
      }
    }
    return ActiveSpaceSelectionOptions.of(
        maxOrbitalCount, frontierPairCount, energyWindowHartree, minOrbitalCount);
  }

  public int maxOrbitalCount() {
    return this.maxOrbitalCount;
  }

  public int frontierPairCount() {
    return this.frontierPairCount;
  }

  public double energyWindowHartree() {
    return this.energyWindowHartree;
  }

  public int minOrbitalCount() {
    return this.minOrbitalCount;
  }

  private static void requireKind(
      final ClassicalCalculationOption option, final ClassicalCalculationOptionKind expectedKind) {
    if (option.kind() != expectedKind) {
      throw new IllegalArgumentException(
          "Active-space option " + option.name() + " must have kind " + expectedKind.name() + ".");
    }
  }
}
