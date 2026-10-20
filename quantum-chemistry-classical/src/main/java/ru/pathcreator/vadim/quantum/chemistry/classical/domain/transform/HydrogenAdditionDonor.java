/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Реальный реагент H-X: связь разрывается, обе части переходят в продукт. */
public enum HydrogenAdditionDonor {
  HYDROGEN("H2"),
  WATER("H2O"),
  HYDROGEN_HALIDE("HCl, HBr или HI"),
  THIOL("R-S-H");

  private final String description;

  HydrogenAdditionDonor(final String description) {
    this.description = description;
  }

  public String description() {
    return this.description;
  }

  public boolean matches(final Molecule molecule, final Atom group, final Atom hydrogen) {
    if (!hydrogen.element().symbol().value().equals("H")
        || molecule.charge().value() != 0
        || molecule.spinMultiplicity().value() != 1
        || molecule.atoms().stream()
            .anyMatch(
                atom ->
                    atom.formalCharge().value() != 0
                        || !atom.radicalState()
                            .equals(
                                ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState
                                    .CLOSED_SHELL))) {
      return false;
    }
    final var hBonds =
        molecule.bonds().stream()
            .filter(
                bond ->
                    bond.firstAtomId().equals(hydrogen.id())
                        || bond.secondAtomId().equals(hydrogen.id()))
            .toList();
    if (hBonds.size() != 1
        || hBonds.getFirst().type() != BondType.SINGLE
        || !hBonds.getFirst().connects(group.id(), hydrogen.id())) {
      return false;
    }
    final String element = group.element().symbol().value();
    return switch (this) {
      case HYDROGEN -> element.equals("H") && molecule.atomCount() == 2;
      case HYDROGEN_HALIDE ->
          List.of("Cl", "Br", "I").contains(element) && molecule.atomCount() == 2;
      case WATER ->
          element.equals("O")
              && molecule.atomCount() == 3
              && molecule.bondCount() == 2
              && molecule.atoms().stream()
                      .filter(atom -> atom.element().symbol().value().equals("H"))
                      .count()
                  == 2
              && molecule.bonds().stream()
                  .allMatch(
                      bond ->
                          bond.type() == BondType.SINGLE
                              && (bond.firstAtomId().equals(group.id())
                                  || bond.secondAtomId().equals(group.id())));
      case THIOL ->
          element.equals("S")
              && molecule.bonds().stream()
                      .filter(
                          bond ->
                              bond.firstAtomId().equals(group.id())
                                  || bond.secondAtomId().equals(group.id()))
                      .count()
                  == 2
              && molecule.bonds().stream()
                  .filter(
                      bond ->
                          bond.type() == BondType.SINGLE
                              && (bond.firstAtomId().equals(group.id())
                                  || bond.secondAtomId().equals(group.id())))
                  .anyMatch(
                      bond ->
                          molecule.atoms().stream()
                              .anyMatch(
                                  atom ->
                                      atom.element().symbol().value().equals("C")
                                          && bond.connects(group.id(), atom.id())));
    };
  }
}
