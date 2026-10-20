/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.relativistic;

import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Вклад одного атома в скалярную релятивистскую поправку. */
public final class RelativisticAtomContribution {

  private final String atomId;
  private final String elementSymbol;
  private final int atomicNumber;
  private final double electronWeight;
  private final double correctionHartree;

  private RelativisticAtomContribution(
      final String atomId,
      final String elementSymbol,
      final int atomicNumber,
      final double electronWeight,
      final double correctionHartree) {
    this.atomId = atomId;
    this.elementSymbol = elementSymbol;
    this.atomicNumber = atomicNumber;
    this.electronWeight = electronWeight;
    this.correctionHartree = correctionHartree;
  }

  public static RelativisticAtomContribution of(
      final String atomId,
      final String elementSymbol,
      final int atomicNumber,
      final double electronWeight,
      final double correctionHartree) {
    if (atomicNumber <= 0) {
      throw new IllegalArgumentException("Relativistic atom atomic number must be positive.");
    }
    if (!Double.isFinite(electronWeight) || electronWeight < 0.0) {
      throw new IllegalArgumentException(
          "Relativistic atom electron weight must be finite and non-negative.");
    }
    if (!Double.isFinite(correctionHartree)) {
      throw new IllegalArgumentException("Relativistic atom correction must be finite.");
    }
    return new RelativisticAtomContribution(
        TextValue.requireText(atomId, "Relativistic atom id"),
        TextValue.requireText(elementSymbol, "Relativistic atom element symbol"),
        atomicNumber,
        electronWeight,
        correctionHartree);
  }

  public String atomId() {
    return this.atomId;
  }

  public String elementSymbol() {
    return this.elementSymbol;
  }

  public int atomicNumber() {
    return this.atomicNumber;
  }

  public double electronWeight() {
    return this.electronWeight;
  }

  public double correctionHartree() {
    return this.correctionHartree;
  }
}
