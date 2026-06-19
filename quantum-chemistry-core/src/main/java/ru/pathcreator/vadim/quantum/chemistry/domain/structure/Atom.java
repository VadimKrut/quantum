/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ChemicalElement;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;

public final class Atom {

  private final AtomId id;
  private final ChemicalElement element;
  private final Coordinate3D coordinate;
  private final FormalCharge formalCharge;
  private final Isotope isotope;
  private final RadicalState radicalState;
  private final ChemistryMetadata metadata;

  private Atom(
      final AtomId id,
      final ChemicalElement element,
      final Coordinate3D coordinate,
      final FormalCharge formalCharge,
      final Isotope isotope,
      final RadicalState radicalState,
      final ChemistryMetadata metadata) {
    this.id = id;
    this.element = element;
    this.coordinate = coordinate;
    this.formalCharge = formalCharge;
    this.isotope = isotope;
    this.radicalState = radicalState;
    this.metadata = metadata;
  }

  public static Atom of(
      final AtomId id,
      final ElementSymbol symbol,
      final Coordinate3D coordinate
  ) {
    return Atom.of(
        id,
        symbol,
        coordinate,
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.EMPTY);
  }

  public static Atom of(
      final AtomId id,
      final ElementSymbol symbol,
      final Coordinate3D coordinate,
      final FormalCharge formalCharge,
      final Isotope isotope,
      final RadicalState radicalState,
      final ChemistryMetadata metadata) {
    if (id == null) {
      throw new IllegalArgumentException("Atom id must not be null.");
    }
    final ChemicalElement checkedElement = PeriodicTable.require(symbol);
    final FormalCharge checkedFormalCharge = formalCharge == null ? FormalCharge.NEUTRAL : formalCharge;
    final RadicalState checkedRadicalState =
        radicalState == null ? RadicalState.CLOSED_SHELL : radicalState;
    final ChemistryMetadata checkedMetadata = metadata == null ? ChemistryMetadata.EMPTY : metadata;
    Atom.requireIsotope(isotope, checkedElement);
    return new Atom(
        id,
        checkedElement,
        coordinate,
        checkedFormalCharge,
        isotope,
        checkedRadicalState,
        checkedMetadata);
  }

  public AtomId id() {
    return this.id;
  }

  public ChemicalElement element() {
    return this.element;
  }

  public Coordinate3D coordinate() {
    return this.coordinate;
  }

  public FormalCharge formalCharge() {
    return this.formalCharge;
  }

  public Isotope isotope() {
    return this.isotope;
  }

  public boolean hasIsotope() {
    return this.isotope != null;
  }

  public RadicalState radicalState() {
    return this.radicalState;
  }

  public ChemistryMetadata metadata() {
    return this.metadata;
  }

  public boolean hasCoordinate() {
    return this.coordinate != null;
  }

  private static void requireIsotope(
      final Isotope isotope,
      final ChemicalElement element
  ) {
    if (isotope == null) {
      return;
    }
    if (isotope.massNumber() < element.atomicNumber()) {
      throw new IllegalArgumentException(
          "Isotope mass number must not be smaller than atomic number.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Atom)) {
      return false;
    }
    final Atom atom = (Atom) other;
    return Objects.equals(this.id, atom.id)
        && Objects.equals(this.element, atom.element)
        && Objects.equals(this.coordinate, atom.coordinate)
        && Objects.equals(this.formalCharge, atom.formalCharge)
        && Objects.equals(this.isotope, atom.isotope)
        && Objects.equals(this.radicalState, atom.radicalState)
        && Objects.equals(this.metadata, atom.metadata);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.element,
        this.coordinate,
        this.formalCharge,
        this.isotope,
        this.radicalState,
        this.metadata);
  }
}