/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.transform;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Делит молекулярный граф на связные компоненты после операций разрыва связи. */
public final class MolecularGraphComponentSplitter {

  public List<Molecule> split(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException(
          "Molecular graph component splitter molecule must not be null.");
    }
    final List<Atom> atoms = molecule.atoms();
    if (atoms.isEmpty()) {
      return List.of(molecule);
    }
    final HashSet<AtomId> visited = new HashSet<AtomId>();
    final ArrayList<Molecule> components = new ArrayList<Molecule>();
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      final AtomId atomId = atoms.get(atomIndex).id();
      if (visited.contains(atomId)) {
        continue;
      }
      final HashSet<AtomId> componentAtomIds =
          MolecularGraphComponentSplitter.componentAtomIds(molecule, atomId, visited);
      components.add(
          MolecularGraphComponentSplitter.componentMolecule(
              molecule, componentAtomIds, components.size()));
    }
    if (components.size() <= 1) {
      return List.of(molecule);
    }
    return List.copyOf(components);
  }

  private static HashSet<AtomId> componentAtomIds(
      final Molecule molecule, final AtomId startAtomId, final HashSet<AtomId> visited) {
    final HashSet<AtomId> component = new HashSet<AtomId>();
    final ArrayDeque<AtomId> queue = new ArrayDeque<AtomId>();
    queue.add(startAtomId);
    visited.add(startAtomId);
    while (!queue.isEmpty()) {
      final AtomId current = queue.removeFirst();
      component.add(current);
      final List<Bond> bonds = molecule.bonds();
      for (int bondIndex = 0; bondIndex < bonds.size(); ++bondIndex) {
        final Bond bond = bonds.get(bondIndex);
        final AtomId next = MolecularGraphComponentSplitter.nextAtomId(bond, current);
        if (next != null && !visited.contains(next)) {
          visited.add(next);
          queue.addLast(next);
        }
      }
    }
    return component;
  }

  private static AtomId nextAtomId(final Bond bond, final AtomId current) {
    if (bond.firstAtomId().equals(current)) {
      return bond.secondAtomId();
    }
    if (bond.secondAtomId().equals(current)) {
      return bond.firstAtomId();
    }
    return null;
  }

  private static Molecule componentMolecule(
      final Molecule molecule, final HashSet<AtomId> componentAtomIds, final int componentIndex) {
    final ArrayList<Atom> atoms = new ArrayList<Atom>(componentAtomIds.size());
    final List<Atom> sourceAtoms = molecule.atoms();
    for (int atomIndex = 0; atomIndex < sourceAtoms.size(); ++atomIndex) {
      final Atom atom = sourceAtoms.get(atomIndex);
      if (componentAtomIds.contains(atom.id())) {
        atoms.add(atom);
      }
    }
    final ArrayList<Bond> bonds = new ArrayList<Bond>();
    final List<Bond> sourceBonds = molecule.bonds();
    for (int bondIndex = 0; bondIndex < sourceBonds.size(); ++bondIndex) {
      final Bond bond = sourceBonds.get(bondIndex);
      if (componentAtomIds.contains(bond.firstAtomId())
          && componentAtomIds.contains(bond.secondAtomId())) {
        bonds.add(bond);
      }
    }
    return Molecule.of(
        MoleculeId.of(molecule.id().value() + "_component_" + componentIndex),
        molecule.displayName() + " component " + componentIndex,
        atoms,
        bonds,
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.EMPTY);
  }
}
