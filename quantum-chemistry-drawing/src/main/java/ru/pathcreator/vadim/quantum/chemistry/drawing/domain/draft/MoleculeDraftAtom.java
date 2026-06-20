/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.draft;

import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint3D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;

/**
 * Черновик атома: объединяет химические свойства, 2D-позицию и 3D-позицию.
 */
public final class MoleculeDraftAtom {

  private final AtomId atomId;
  private final ElementSymbol symbol;
  private final DrawingPoint2D point2D;
  private final DrawingPoint3D point3D;
  private final FormalCharge formalCharge;
  private final Isotope isotope;
  private final RadicalState radicalState;
  private final ChemistryMetadata metadata;

  private MoleculeDraftAtom(
      final AtomId atomId,
      final ElementSymbol symbol,
      final DrawingPoint2D point2D,
      final DrawingPoint3D point3D,
      final FormalCharge formalCharge,
      final Isotope isotope,
      final RadicalState radicalState,
      final ChemistryMetadata metadata
  ) {
    this.atomId = atomId;
    this.symbol = symbol;
    this.point2D = point2D;
    this.point3D = point3D;
    this.formalCharge = formalCharge;
    this.isotope = isotope;
    this.radicalState = radicalState;
    this.metadata = metadata;
  }

  public static MoleculeDraftAtom of(
      final String atomId,
      final String symbol,
      final DrawingPoint2D point2D,
      final DrawingPoint3D point3D
  ) {
    return MoleculeDraftAtom.of(
        AtomId.of(atomId),
        ElementSymbol.of(symbol),
        point2D,
        point3D,
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.EMPTY
    );
  }

  public static MoleculeDraftAtom of(
      final AtomId atomId,
      final ElementSymbol symbol,
      final DrawingPoint2D point2D,
      final DrawingPoint3D point3D,
      final FormalCharge formalCharge,
      final Isotope isotope,
      final RadicalState radicalState,
      final ChemistryMetadata metadata
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException("Draft atom id must not be null.");
    }
    if (symbol == null) {
      throw new IllegalArgumentException("Draft atom symbol must not be null.");
    }
    return new MoleculeDraftAtom(
        atomId,
        symbol,
        point2D == null ? DrawingPoint2D.ORIGIN : point2D,
        point3D == null ? DrawingPoint3D.ORIGIN : point3D,
        formalCharge == null ? FormalCharge.NEUTRAL : formalCharge,
        isotope,
        radicalState == null ? RadicalState.CLOSED_SHELL : radicalState,
        metadata == null ? ChemistryMetadata.EMPTY : metadata
    );
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public DrawingPoint2D point2D() {
    return this.point2D;
  }

  public DrawingPoint3D point3D() {
    return this.point3D;
  }

  public Atom toAtom(final LengthUnit unit) {
    return Atom.of(
        this.atomId,
        this.symbol,
        this.point3D.toCoordinate(unit),
        this.formalCharge,
        this.isotope,
        this.radicalState,
        this.metadata
    );
  }

  public AtomDrawing toDrawing() {
    return AtomDrawing.of(
        this.atomId,
        this.point2D,
        this.point3D,
        true
    );
  }

  public MoleculeDraftAtom withPoint2D(final DrawingPoint2D point) {
    return MoleculeDraftAtom.of(
        this.atomId,
        this.symbol,
        point,
        this.point3D,
        this.formalCharge,
        this.isotope,
        this.radicalState,
        this.metadata
    );
  }

  public MoleculeDraftAtom withPoint3D(final DrawingPoint3D point) {
    return MoleculeDraftAtom.of(
        this.atomId,
        this.symbol,
        this.point2D,
        point,
        this.formalCharge,
        this.isotope,
        this.radicalState,
        this.metadata
    );
  }
}