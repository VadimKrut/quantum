/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.transform;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondCleavageTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Строит продукты разрыва связи, удаляя выбранную связь из молекулярного графа. */
public final class BondCleavageTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule molecule, final BondCleavageTransform transform) {
    if (molecule == null) {
      throw new IllegalArgumentException("Bond cleavage molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Bond cleavage transform must not be null.");
    }
    final List<CleavageSite> sites = BondCleavageTransformer.matchingSites(molecule, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(sites.size());
    for (int i = 0; i < sites.size(); ++i) {
      products.add(BondCleavageTransformer.transformAt(molecule, transform, sites.get(i), i));
    }
    return List.copyOf(products);
  }

  private static MolecularTransformProduct transformAt(
      final Molecule molecule,
      final BondCleavageTransform transform,
      final CleavageSite site,
      final int siteIndex) {
    final ArrayList<Bond> bonds = new ArrayList<Bond>(molecule.bonds().size() - 1);
    final List<Bond> sourceBonds = molecule.bonds();
    for (int i = 0; i < sourceBonds.size(); ++i) {
      final Bond bond = sourceBonds.get(i);
      if (!bond.connects(site.firstAtomId(), site.secondAtomId())) {
        bonds.add(bond);
      }
    }
    final List<Molecule> fragments =
        BondCleavageTransformer.fragments(molecule, transform, site, siteIndex, bonds);
    if (fragments.isEmpty()) {
      throw new IllegalStateException("Bond cleavage produced no molecular fragments.");
    }
    return MolecularTransformProduct.of(
        fragments.get(0),
        fragments.subList(1, fragments.size()),
        site.firstAtomId(),
        site.secondAtomId());
  }

  private static List<Molecule> fragments(
      final Molecule molecule,
      final BondCleavageTransform transform,
      final CleavageSite site,
      final int siteIndex,
      final List<Bond> productBonds) {
    final ArrayList<Molecule> fragments = new ArrayList<Molecule>();
    final ArrayList<AtomId> visited = new ArrayList<AtomId>(molecule.atomCount());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom seed = atoms.get(i);
      if (visited.contains(seed.id())) {
        continue;
      }
      final List<AtomId> componentIds =
          BondCleavageTransformer.connectedComponent(seed.id(), productBonds, visited);
      fragments.add(
          BondCleavageTransformer.fragment(
              molecule, transform, site, siteIndex, fragments.size(), componentIds, productBonds));
    }
    return List.copyOf(fragments);
  }

  private static List<AtomId> connectedComponent(
      final AtomId seed, final List<Bond> bonds, final List<AtomId> visited) {
    final ArrayList<AtomId> component = new ArrayList<AtomId>();
    final ArrayList<AtomId> queue = new ArrayList<AtomId>();
    queue.add(seed);
    visited.add(seed);
    for (int cursor = 0; cursor < queue.size(); ++cursor) {
      final AtomId current = queue.get(cursor);
      component.add(current);
      for (int i = 0; i < bonds.size(); ++i) {
        final AtomId neighbor = BondCleavageTransformer.otherEndpoint(bonds.get(i), current);
        if (neighbor != null && !visited.contains(neighbor)) {
          visited.add(neighbor);
          queue.add(neighbor);
        }
      }
    }
    return List.copyOf(component);
  }

  private static Molecule fragment(
      final Molecule molecule,
      final BondCleavageTransform transform,
      final CleavageSite site,
      final int siteIndex,
      final int componentIndex,
      final List<AtomId> componentIds,
      final List<Bond> productBonds) {
    final ArrayList<Atom> fragmentAtoms = new ArrayList<Atom>(componentIds.size());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (componentIds.contains(atom.id())) {
        fragmentAtoms.add(atom);
      }
    }
    final ArrayList<Bond> fragmentBonds = new ArrayList<Bond>();
    for (int i = 0; i < productBonds.size(); ++i) {
      final Bond bond = productBonds.get(i);
      if (componentIds.contains(bond.firstAtomId()) && componentIds.contains(bond.secondAtomId())) {
        fragmentBonds.add(bond);
      }
    }
    return Molecule.of(
        MoleculeId.of(
            molecule.id().value()
                + "_"
                + transform.idSuffix()
                + "_"
                + site.firstAtomId().value()
                + "_"
                + site.secondAtomId().value()
                + "_"
                + siteIndex
                + "_fragment_"
                + componentIndex),
        molecule.displayName()
            + " "
            + transform.productNameSuffix()
            + " fragment "
            + componentIndex,
        fragmentAtoms,
        fragmentBonds,
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.EMPTY);
  }

  private static List<CleavageSite> matchingSites(
      final Molecule molecule, final BondCleavageTransform transform) {
    final ArrayList<CleavageSite> sites = new ArrayList<CleavageSite>();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final BondType sourceType =
          molecule.aromaticity().isAromaticBond(bond) ? BondType.AROMATIC : bond.type();
      if (sourceType != transform.sourceBondType()) {
        continue;
      }
      final Atom first = BondCleavageTransformer.atom(molecule, bond.firstAtomId());
      final Atom second = BondCleavageTransformer.atom(molecule, bond.secondAtomId());
      if (transform.firstAtomSelector().matches(first)
          && transform.secondAtomSelector().matches(second)) {
        sites.add(new CleavageSite(first.id(), second.id()));
      }
      if (transform.firstAtomSelector().matches(second)
          && transform.secondAtomSelector().matches(first)
          && !BondCleavageTransformer.containsUnordered(sites, first.id(), second.id())) {
        sites.add(new CleavageSite(second.id(), first.id()));
      }
    }
    return List.copyOf(sites);
  }

  private static boolean containsUnordered(
      final List<CleavageSite> sites, final AtomId firstAtomId, final AtomId secondAtomId) {
    for (int i = 0; i < sites.size(); ++i) {
      final CleavageSite site = sites.get(i);
      if ((site.firstAtomId().equals(firstAtomId) && site.secondAtomId().equals(secondAtomId))
          || (site.firstAtomId().equals(secondAtomId) && site.secondAtomId().equals(firstAtomId))) {
        return true;
      }
    }
    return false;
  }

  private static AtomId otherEndpoint(final Bond bond, final AtomId atomId) {
    if (bond.firstAtomId().equals(atomId)) {
      return bond.secondAtomId();
    }
    if (bond.secondAtomId().equals(atomId)) {
      return bond.firstAtomId();
    }
    return null;
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Bond cleavage molecule bond references unknown atom.");
  }

  private static final class CleavageSite {

    private final AtomId firstAtomId;
    private final AtomId secondAtomId;

    private CleavageSite(final AtomId firstAtomId, final AtomId secondAtomId) {
      this.firstAtomId = firstAtomId;
      this.secondAtomId = secondAtomId;
    }

    private AtomId firstAtomId() {
      return this.firstAtomId;
    }

    private AtomId secondAtomId() {
      return this.secondAtomId;
    }
  }
}
