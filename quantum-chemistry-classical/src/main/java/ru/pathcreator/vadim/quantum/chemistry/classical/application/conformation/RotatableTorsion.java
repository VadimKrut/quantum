/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation;

import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Описывает один вращаемый torsion path A-B-C-D для локального conformer scan. */
final class RotatableTorsion {

  private final Atom firstOuterAtom;
  private final Atom firstCentralAtom;
  private final Atom secondCentralAtom;
  private final Atom secondOuterAtom;

  private RotatableTorsion(
      final Atom firstOuterAtom,
      final Atom firstCentralAtom,
      final Atom secondCentralAtom,
      final Atom secondOuterAtom) {
    this.firstOuterAtom = firstOuterAtom;
    this.firstCentralAtom = firstCentralAtom;
    this.secondCentralAtom = secondCentralAtom;
    this.secondOuterAtom = secondOuterAtom;
  }

  static RotatableTorsion of(
      final Atom firstOuterAtom,
      final Atom firstCentralAtom,
      final Atom secondCentralAtom,
      final Atom secondOuterAtom) {
    if (firstOuterAtom == null
        || firstCentralAtom == null
        || secondCentralAtom == null
        || secondOuterAtom == null) {
      throw new IllegalArgumentException("Rotatable torsion atoms must not be null.");
    }
    return new RotatableTorsion(
        firstOuterAtom, firstCentralAtom, secondCentralAtom, secondOuterAtom);
  }

  Atom oppositeOuterAtom() {
    return this.secondOuterAtom;
  }

  Atom firstCentralAtom() {
    return this.firstCentralAtom;
  }

  Atom secondCentralAtom() {
    return this.secondCentralAtom;
  }

  boolean referencesCentralAtom(final AtomId atomId) {
    return this.firstCentralAtom.id().equals(atomId) || this.secondCentralAtom.id().equals(atomId);
  }

  String bondName() {
    return this.firstCentralAtom.id().value() + "-" + this.secondCentralAtom.id().value();
  }

  String atomPathName() {
    return this.firstOuterAtom.id().value()
        + "-"
        + this.firstCentralAtom.id().value()
        + "-"
        + this.secondCentralAtom.id().value()
        + "-"
        + this.secondOuterAtom.id().value();
  }
}
