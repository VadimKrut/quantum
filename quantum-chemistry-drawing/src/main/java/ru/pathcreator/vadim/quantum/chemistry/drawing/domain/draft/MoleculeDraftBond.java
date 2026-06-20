/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.draft;

import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;

/**
 * Черновик связи: химический тип связи и стиль ее отображения.
 */
public final class MoleculeDraftBond {

  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final BondType type;
  private final BondDrawingStyle style;

  private MoleculeDraftBond(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final BondType type,
      final BondDrawingStyle style
  ) {
    this.firstAtomId = firstAtomId;
    this.secondAtomId = secondAtomId;
    this.type = type;
    this.style = style;
  }

  public static MoleculeDraftBond of(
      final String firstAtomId,
      final String secondAtomId,
      final BondType type,
      final BondDrawingStyle style
  ) {
    return MoleculeDraftBond.of(
        AtomId.of(firstAtomId),
        AtomId.of(secondAtomId),
        type,
        style
    );
  }

  public static MoleculeDraftBond of(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final BondType type,
      final BondDrawingStyle style
  ) {
    if (
        firstAtomId == null
        || secondAtomId == null
    ) {
      throw new IllegalArgumentException("Draft bond atom ids must not be null.");
    }
    return new MoleculeDraftBond(
        firstAtomId,
        secondAtomId,
        type == null ? BondType.SINGLE : type,
        style == null ? BondDrawingStyle.PLAIN : style
    );
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId secondAtomId() {
    return this.secondAtomId;
  }

  public Bond toBond() {
    return Bond.of(
        this.firstAtomId,
        this.secondAtomId,
        this.type
    );
  }

  public BondDrawing toDrawing() {
    return BondDrawing.of(
        this.firstAtomId,
        this.secondAtomId,
        this.style
    );
  }
}