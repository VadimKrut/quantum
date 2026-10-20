/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Derived aromatic membership without rewriting the stored bond orders.
 * Recognizes fully annotated cycles and closed-shell five/six-membered Kekule rings
 * with six pi electrons (C/N and one N/O/S lone-pair donor in a five-membered ring).
 * This conservative local model does not infer general fused-system aromaticity.
 */
public final class MolecularAromaticity {

  private final Set<MolecularRing> rings;
  private final Set<AtomId> atoms;
  private final Set<Bond> bonds;

  private MolecularAromaticity(
      final Set<MolecularRing> rings, final Set<AtomId> atoms, final Set<Bond> bonds) {
    this.rings = Set.copyOf(rings);
    this.atoms = Set.copyOf(atoms);
    this.bonds = Set.copyOf(bonds);
  }

  public static MolecularAromaticity analyze(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Aromaticity analysis requires a molecule.");
    }
    final Map<AtomId, Atom> atomById = new HashMap<>();
    final Map<AtomId, List<Bond>> incident = new HashMap<>();
    molecule
        .atoms()
        .forEach(
            atom -> {
              atomById.put(atom.id(), atom);
              incident.put(atom.id(), new ArrayList<>());
            });
    molecule
        .bonds()
        .forEach(
            bond -> {
              incident.get(bond.firstAtomId()).add(bond);
              incident.get(bond.secondAtomId()).add(bond);
            });
    final Set<MolecularRing> rings = new HashSet<>();
    final Set<AtomId> atoms = new HashSet<>();
    final Set<Bond> bonds = new HashSet<>();
    for (final MolecularRing ring : molecule.graph().rings()) {
      final List<Bond> edges = edges(ring, incident);
      if (edges.size() != ring.size()) {
        continue;
      }
      if (edges.stream().allMatch(bond -> bond.type() == BondType.AROMATIC)
          || kekule(ring, edges, atomById, incident)) {
        rings.add(ring);
        atoms.addAll(ring.atomIds());
        bonds.addAll(edges);
      }
    }
    return new MolecularAromaticity(rings, atoms, bonds);
  }

  public boolean isAromaticRing(final MolecularRing ring) {
    return this.rings.contains(ring);
  }

  public boolean isAromaticAtom(final AtomId atom) {
    return this.atoms.contains(atom);
  }

  public boolean isAromaticBond(final Bond bond) {
    return this.bonds.contains(bond);
  }

  private static List<Bond> edges(
      final MolecularRing ring, final Map<AtomId, List<Bond>> incident) {
    final List<Bond> result = new ArrayList<>();
    final List<AtomId> ids = ring.atomIds();
    for (int index = 0; index < ids.size(); index++) {
      final AtomId first = ids.get(index);
      final AtomId second = ids.get((index + 1) % ids.size());
      final Bond bond =
          incident.get(first).stream()
              .filter(value -> value.connects(first, second))
              .findFirst()
              .orElse(null);
      if (bond == null) {
        return List.of();
      }
      result.add(bond);
    }
    return result;
  }

  private static boolean kekule(
      final MolecularRing ring,
      final List<Bond> edges,
      final Map<AtomId, Atom> atoms,
      final Map<AtomId, List<Bond>> incident) {
    if (ring.size() != 5 && ring.size() != 6) {
      return false;
    }
    if (edges.stream()
        .anyMatch(bond -> bond.type() != BondType.SINGLE && bond.type() != BondType.DOUBLE)) {
      return false;
    }
    int electrons = 0;
    for (final AtomId id : ring.atomIds()) {
      final Atom atom = atoms.get(id);
      final String symbol = atom.element().symbol().value();
      final int charge = atom.formalCharge().value();
      if (atom.radicalState().radical()
          || !(symbol.equals("C")
              || symbol.equals("N")
              || symbol.equals("O")
              || symbol.equals("S"))) {
        return false;
      }
      if (charge != 0 && !(ring.size() == 6 && symbol.equals("N") && charge == 1)) {
        return false;
      }
      int doubleBonds = 0;
      int valence = 0;
      for (final Bond bond : incident.get(id)) {
        if (bond.type() != BondType.SINGLE && bond.type() != BondType.DOUBLE) {
          return false;
        }
        valence += bond.type() == BondType.DOUBLE ? 2 : 1;
        if (bond.type() == BondType.DOUBLE) {
          if (!edges.contains(bond)) {
            return false;
          }
          doubleBonds++;
        }
      }
      final int maxValence =
          symbol.equals("C") ? 4 : symbol.equals("N") ? (charge == 1 ? 4 : 3) : 2;
      if (valence > maxValence) {
        return false;
      }
      if (doubleBonds == 1 && (symbol.equals("C") || symbol.equals("N"))) {
        electrons++;
      } else if (doubleBonds == 0 && ring.size() == 5 && !symbol.equals("C")) {
        electrons += 2;
      } else {
        return false;
      }
    }
    return electrons == 6;
  }
}
