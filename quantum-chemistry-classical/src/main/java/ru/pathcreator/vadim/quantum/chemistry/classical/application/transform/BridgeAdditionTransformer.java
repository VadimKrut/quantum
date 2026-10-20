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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BridgeAdditionTransform;
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

/** Создает bridge-addition продукты, например эпоксид через один атом кислорода поверх C=C. */
public final class BridgeAdditionTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule molecule, final BridgeAdditionTransform transform) {
    if (molecule == null) {
      throw new IllegalArgumentException("Bridge-addition molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Bridge-addition transform must not be null.");
    }
    final List<BridgeSite> sites = BridgeAdditionTransformer.matchingSites(molecule, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(sites.size());
    for (int i = 0; i < sites.size(); ++i) {
      products.add(BridgeAdditionTransformer.transformAt(molecule, transform, sites.get(i), i));
    }
    return List.copyOf(products);
  }

  private static MolecularTransformProduct transformAt(
      final Molecule molecule,
      final BridgeAdditionTransform transform,
      final BridgeSite site,
      final int siteIndex) {
    final AtomId bridgeAtomId =
        AtomId.of(
            "bridge_"
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
            bridgeAtomId,
            transform.bridgeElement(),
            null,
            FormalCharge.NEUTRAL,
            null,
            RadicalState.CLOSED_SHELL,
            ChemistryMetadata.EMPTY));
    final ArrayList<Bond> bonds = new ArrayList<Bond>(molecule.bonds().size() + 2);
    final List<Bond> sourceBonds = molecule.bonds();
    for (int i = 0; i < sourceBonds.size(); ++i) {
      final Bond bond = sourceBonds.get(i);
      if (bond.connects(site.firstAnchorId(), site.secondAnchorId())) {
        bonds.add(
            Bond.of(site.firstAnchorId(), site.secondAnchorId(), transform.productBondType()));
        continue;
      }
      bonds.add(bond);
    }
    bonds.add(Bond.of(site.firstAnchorId(), bridgeAtomId, transform.firstBridgeBondType()));
    bonds.add(Bond.of(site.secondAnchorId(), bridgeAtomId, transform.secondBridgeBondType()));
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

  private static List<BridgeSite> matchingSites(
      final Molecule molecule, final BridgeAdditionTransform transform) {
    final ArrayList<BridgeSite> sites = new ArrayList<BridgeSite>();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final BondType sourceType =
          molecule.aromaticity().isAromaticBond(bond) ? BondType.AROMATIC : bond.type();
      if (sourceType != transform.sourceBondType()) {
        continue;
      }
      final Atom first = BridgeAdditionTransformer.atom(molecule, bond.firstAtomId());
      final Atom second = BridgeAdditionTransformer.atom(molecule, bond.secondAtomId());
      if (transform.firstAnchorSelector().matches(first)
          && transform.secondAnchorSelector().matches(second)) {
        sites.add(new BridgeSite(first.id(), second.id()));
      }
      if (transform.firstAnchorSelector().matches(second)
          && transform.secondAnchorSelector().matches(first)
          && !BridgeAdditionTransformer.containsUnordered(sites, first.id(), second.id())) {
        sites.add(new BridgeSite(second.id(), first.id()));
      }
    }
    return List.copyOf(sites);
  }

  private static boolean containsUnordered(
      final List<BridgeSite> sites, final AtomId firstAnchorId, final AtomId secondAnchorId) {
    for (int i = 0; i < sites.size(); ++i) {
      final BridgeSite site = sites.get(i);
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
    throw new IllegalArgumentException("Bridge-addition molecule bond references unknown atom.");
  }

  private static final class BridgeSite {

    private final AtomId firstAnchorId;
    private final AtomId secondAnchorId;

    private BridgeSite(final AtomId firstAnchorId, final AtomId secondAnchorId) {
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
