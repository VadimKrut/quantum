/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.transform;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Explicit neutral reactant sites shared by bounded multicomponent graph transformations. */
final class MulticomponentReactantSites {
  private MulticomponentReactantSites() {}

  record Carbonyl(AtomId carbon, AtomId oxygen) {}

  record Acid(AtomId carbon, AtomId hydroxyl, AtomId hydrogen) {}

  record Isocyanide(AtomId carbon, AtomId nitrogen) {}

  record PrimaryAmine(AtomId nitrogen, AtomId firstHydrogen, AtomId secondHydrogen) {}

  static List<PrimaryAmine> primaryAmines(final Molecule molecule) {
    if (!neutral(molecule)) {
      return List.of();
    }
    final var result = new ArrayList<PrimaryAmine>();
    for (final var nitrogen : molecule.atoms()) {
      if (!element(nitrogen, "N")) {
        continue;
      }
      final var nb = bonds(molecule, nitrogen.id());
      if (nb.size() != 3 || nb.stream().anyMatch(b -> b.type() != BondType.SINGLE)) {
        continue;
      }
      final var hydrogens =
          nb.stream()
              .map(b -> atom(molecule, other(b, nitrogen.id())))
              .filter(a -> element(a, "H") && bonds(molecule, a.id()).size() == 1)
              .toList();
      final var carbon =
          nb.stream()
              .map(b -> atom(molecule, other(b, nitrogen.id())))
              .filter(a -> element(a, "C"))
              .findFirst()
              .orElse(null);
      if (hydrogens.size() != 2 || carbon == null) {
        continue;
      }
      // Amides, thioamides and amidines are not ordinary primary amine components.
      if (bonds(molecule, carbon.id()).stream()
          .anyMatch(
              b ->
                  b.type() != BondType.SINGLE
                      && b.type() != BondType.AROMATIC
                      && !element(atom(molecule, other(b, carbon.id())), "C"))) {
        continue;
      }
      result.add(new PrimaryAmine(nitrogen.id(), hydrogens.get(0).id(), hydrogens.get(1).id()));
    }
    return result;
  }

  static boolean closed(final Molecule molecule) {
    return molecule.charge().value() == 0
        && molecule.spinMultiplicity().value() == 1
        && molecule.graph().connected()
        && molecule.atoms().stream()
            .allMatch(atom -> atom.radicalState().equals(RadicalState.CLOSED_SHELL));
  }

  static boolean neutral(final Molecule molecule) {
    return closed(molecule)
        && molecule.atoms().stream().allMatch(atom -> atom.formalCharge().value() == 0);
  }

  static List<Carbonyl> carbonyls(final Molecule molecule) {
    if (!neutral(molecule)) {
      return List.of();
    }
    final var result = new ArrayList<Carbonyl>();
    for (final var carbon : molecule.atoms()) {
      if (!element(carbon, "C")) {
        continue;
      }
      final var bonds = bonds(molecule, carbon.id());
      if (bonds.size() != 3) {
        continue;
      }
      final var oxygen =
          bonds.stream()
              .filter(b -> b.type() == BondType.DOUBLE)
              .map(b -> atom(molecule, other(b, carbon.id())))
              .filter(a -> element(a, "O") && bonds(molecule, a.id()).size() == 1)
              .findFirst()
              .orElse(null);
      if (oxygen != null
          && bonds.stream()
                  .filter(
                      b ->
                          b.type() == BondType.SINGLE
                              && (element(atom(molecule, other(b, carbon.id())), "C")
                                  || element(atom(molecule, other(b, carbon.id())), "H")))
                  .count()
              == 2) {
        result.add(new Carbonyl(carbon.id(), oxygen.id()));
      }
    }
    return result;
  }

  static List<Acid> acids(final Molecule molecule) {
    if (!neutral(molecule)) {
      return List.of();
    }
    final var result = new ArrayList<Acid>();
    for (final var carbon : molecule.atoms()) {
      if (!element(carbon, "C")) {
        continue;
      }
      final var cb = bonds(molecule, carbon.id());
      if (cb.size() != 3
          || cb.stream()
                  .filter(
                      b ->
                          b.type() == BondType.DOUBLE
                              && element(atom(molecule, other(b, carbon.id())), "O")
                              && bonds(molecule, other(b, carbon.id())).size() == 1)
                  .count()
              != 1) {
        continue;
      }
      if (cb.stream()
          .noneMatch(
              b ->
                  b.type() == BondType.SINGLE
                      && (element(atom(molecule, other(b, carbon.id())), "C")
                          || element(atom(molecule, other(b, carbon.id())), "H")))) {
        continue;
      }
      for (final var bond : cb) {
        final var oxygen = atom(molecule, other(bond, carbon.id()));
        if (bond.type() != BondType.SINGLE || !element(oxygen, "O")) {
          continue;
        }
        final var ob = bonds(molecule, oxygen.id());
        if (ob.size() != 2 || ob.stream().anyMatch(b -> b.type() != BondType.SINGLE)) {
          continue;
        }
        final var hydrogen =
            ob.stream()
                .map(b -> atom(molecule, other(b, oxygen.id())))
                .filter(a -> element(a, "H") && bonds(molecule, a.id()).size() == 1)
                .findFirst()
                .orElse(null);
        if (hydrogen != null) {
          result.add(new Acid(carbon.id(), oxygen.id(), hydrogen.id()));
        }
      }
    }
    return result;
  }

  static List<Isocyanide> isocyanides(final Molecule molecule) {
    if (!closed(molecule)) {
      return List.of();
    }
    final var result = new ArrayList<Isocyanide>();
    for (final var carbon : molecule.atoms()) {
      if (!element(carbon, "C") || carbon.formalCharge().value() != -1) {
        continue;
      }
      final var cb = bonds(molecule, carbon.id());
      if (cb.size() != 1 || cb.getFirst().type() != BondType.TRIPLE) {
        continue;
      }
      final var nitrogen = atom(molecule, other(cb.getFirst(), carbon.id()));
      if (!element(nitrogen, "N") || nitrogen.formalCharge().value() != 1) {
        continue;
      }
      final var nb = bonds(molecule, nitrogen.id());
      if (nb.size() != 2
          || nb.stream()
              .noneMatch(
                  b ->
                      b.type() == BondType.SINGLE
                          && element(atom(molecule, other(b, nitrogen.id())), "C"))) {
        continue;
      }
      if (molecule.atoms().stream()
          .anyMatch(
              a ->
                  !a.id().equals(carbon.id())
                      && !a.id().equals(nitrogen.id())
                      && a.formalCharge().value() != 0)) {
        continue;
      }
      result.add(new Isocyanide(carbon.id(), nitrogen.id()));
    }
    return result;
  }

  static boolean element(final Atom atom, final String symbol) {
    return atom.element().symbol().value().equals(symbol);
  }

  static Atom atom(final Molecule molecule, final AtomId id) {
    return molecule.atoms().stream().filter(atom -> atom.id().equals(id)).findFirst().orElseThrow();
  }

  static AtomId other(final Bond bond, final AtomId id) {
    return bond.firstAtomId().equals(id) ? bond.secondAtomId() : bond.firstAtomId();
  }

  static List<Bond> bonds(final Molecule molecule, final AtomId id) {
    return molecule.bonds().stream()
        .filter(b -> b.firstAtomId().equals(id) || b.secondAtomId().equals(id))
        .toList();
  }
}
