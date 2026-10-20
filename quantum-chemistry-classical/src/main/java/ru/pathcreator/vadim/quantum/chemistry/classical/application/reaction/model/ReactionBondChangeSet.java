/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model;

/**
 * Сводка разрыва и образования связей между сторонами реакции.
 */
public final class ReactionBondChangeSet {

  private final int reactantBondCount;
  private final int productBondCount;
  private final int brokenBondEvents;
  private final int formedBondEvents;
  private final double brokenBondEnergy;
  private final double formedBondEnergy;

  private ReactionBondChangeSet(
      final int reactantBondCount,
      final int productBondCount,
      final int brokenBondEvents,
      final int formedBondEvents,
      final double brokenBondEnergy,
      final double formedBondEnergy) {
    this.reactantBondCount = reactantBondCount;
    this.productBondCount = productBondCount;
    this.brokenBondEvents = brokenBondEvents;
    this.formedBondEvents = formedBondEvents;
    this.brokenBondEnergy = brokenBondEnergy;
    this.formedBondEnergy = formedBondEnergy;
  }

  public static ReactionBondChangeSet of(
      final int reactantBondCount,
      final int productBondCount,
      final int brokenBondEvents,
      final int formedBondEvents,
      final double brokenBondEnergy,
      final double formedBondEnergy) {
    if (reactantBondCount < 0 || productBondCount < 0) {
      throw new IllegalArgumentException("Reaction bond counts must not be negative.");
    }
    if (brokenBondEvents < 0 || formedBondEvents < 0) {
      throw new IllegalArgumentException("Reaction bond event counts must not be negative.");
    }
    if (!Double.isFinite(brokenBondEnergy) || brokenBondEnergy < 0.0) {
      throw new IllegalArgumentException("Broken bond energy must be finite and non-negative.");
    }
    if (!Double.isFinite(formedBondEnergy) || formedBondEnergy < 0.0) {
      throw new IllegalArgumentException("Formed bond energy must be finite and non-negative.");
    }
    return new ReactionBondChangeSet(
        reactantBondCount,
        productBondCount,
        brokenBondEvents,
        formedBondEvents,
        brokenBondEnergy,
        formedBondEnergy);
  }

  public int reactantBondCount() {
    return this.reactantBondCount;
  }

  public int productBondCount() {
    return this.productBondCount;
  }

  public int brokenBondEvents() {
    return this.brokenBondEvents;
  }

  public int formedBondEvents() {
    return this.formedBondEvents;
  }

  public double brokenBondEnergy() {
    return this.brokenBondEnergy;
  }

  public double formedBondEnergy() {
    return this.formedBondEnergy;
  }
}
