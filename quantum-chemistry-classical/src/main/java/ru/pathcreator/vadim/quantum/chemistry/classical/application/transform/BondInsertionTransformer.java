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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondInsertionTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Создает продукты вставки атома внутрь существующей связи молекулярного графа. */
public final class BondInsertionTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule molecule, final BondInsertionTransform transform) {
    if (molecule == null) {
      throw new IllegalArgumentException("Bond-insertion molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Bond-insertion transform must not be null.");
    }
    final List<InsertionSite> sites = BondInsertionTransformer.matchingSites(molecule, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(sites.size());
    for (int i = 0; i < sites.size(); ++i) {
      products.add(BondInsertionTransformer.transformAt(molecule, transform, sites.get(i), i));
    }
    return List.copyOf(products);
  }

  private static MolecularTransformProduct transformAt(
      final Molecule molecule,
      final BondInsertionTransform transform,
      final InsertionSite site,
      final int siteIndex) {
    final AtomId insertedAtomId =
        AtomId.of(
            "insert_"
                + transform.idSuffix()
                + "_"
                + site.firstAnchorId().value()
                + "_"
                + site.secondAnchorId().value()
                + "_"
                + siteIndex);
    final ArrayList<Atom> atoms = new ArrayList<Atom>(molecule.atoms().size() + 1);
    atoms.addAll(molecule.atoms());
    atoms.add(
        Atom.of(
            insertedAtomId,
            transform.insertedElement(),
            null,
            FormalCharge.NEUTRAL,
            null,
            RadicalState.CLOSED_SHELL,
            ChemistryMetadata.EMPTY));
    final ArrayList<Bond> bonds = new ArrayList<Bond>(molecule.bonds().size() + 1);
    final List<Bond> sourceBonds = molecule.bonds();
    for (int i = 0; i < sourceBonds.size(); ++i) {
      final Bond bond = sourceBonds.get(i);
      if (bond.connects(site.firstAnchorId(), site.secondAnchorId())) {
        continue;
      }
      bonds.add(bond);
    }
    bonds.add(Bond.of(site.firstAnchorId(), insertedAtomId, transform.firstInsertedBondType()));
    bonds.add(Bond.of(insertedAtomId, site.secondAnchorId(), transform.secondInsertedBondType()));
    final Molecule product =
        Molecule.of(
            MoleculeId.of(
                molecule.id().value()
                    + "_"
                    + transform.idSuffix()
                    + "_"
                    + site.firstAnchorId().value()
                    + "_"
                    + site.secondAnchorId().value()
                    + "_"
                    + siteIndex),
            molecule.displayName() + " " + transform.productNameSuffix(),
            atoms,
            bonds,
            molecule.charge(),
            molecule.spinMultiplicity(),
            molecule.stereochemistry(),
            molecule.conformation(),
            molecule.opticalRotation(),
            molecule.symmetry(),
            molecule.metadata());
    return MolecularTransformProduct.of(product, site.firstAnchorId(), site.secondAnchorId());
  }

  private static List<InsertionSite> matchingSites(
      final Molecule molecule, final BondInsertionTransform transform) {
    final ArrayList<InsertionSite> sites = new ArrayList<InsertionSite>();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final BondType sourceType =
          molecule.aromaticity().isAromaticBond(bond) ? BondType.AROMATIC : bond.type();
      if (sourceType != transform.sourceBondType()) {
        continue;
      }
      final Atom first = BondInsertionTransformer.atom(molecule, bond.firstAtomId());
      final Atom second = BondInsertionTransformer.atom(molecule, bond.secondAtomId());
      if (transform.firstAnchorSelector().matches(first)
          && transform.secondAnchorSelector().matches(second)) {
        sites.add(new InsertionSite(first.id(), second.id()));
      }
      if (transform.firstAnchorSelector().matches(second)
          && transform.secondAnchorSelector().matches(first)
          && !BondInsertionTransformer.containsUnordered(sites, first.id(), second.id())) {
        sites.add(new InsertionSite(second.id(), first.id()));
      }
    }
    return List.copyOf(sites);
  }

  private static boolean containsUnordered(
      final List<InsertionSite> sites, final AtomId firstAnchorId, final AtomId secondAnchorId) {
    for (int i = 0; i < sites.size(); ++i) {
      final InsertionSite site = sites.get(i);
      if ((site.firstAnchorId().equals(firstAnchorId)
              && site.secondAnchorId().equals(secondAnchorId))
          || (site.firstAnchorId().equals(secondAnchorId)
              && site.secondAnchorId().equals(firstAnchorId))) {
        return true;
      }
    }
    return false;
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Bond-insertion molecule bond references unknown atom.");
  }

  private static final class InsertionSite {

    private final AtomId firstAnchorId;
    private final AtomId secondAnchorId;

    private InsertionSite(final AtomId firstAnchorId, final AtomId secondAnchorId) {
      this.firstAnchorId = firstAnchorId;
      this.secondAnchorId = secondAnchorId;
    }

    private AtomId firstAnchorId() {
      return this.firstAnchorId;
    }

    private AtomId secondAnchorId() {
      return this.secondAnchorId;
    }
  }
}
