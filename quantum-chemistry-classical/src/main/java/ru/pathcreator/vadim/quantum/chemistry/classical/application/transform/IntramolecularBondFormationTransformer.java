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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.IntramolecularBondFormationTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Строит продукты внутримолекулярной циклизации через добавление новой связи в существующий граф. */
public final class IntramolecularBondFormationTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule molecule, final IntramolecularBondFormationTransform transform) {
    if (molecule == null) {
      throw new IllegalArgumentException(
          "Intramolecular bond formation molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException(
          "Intramolecular bond formation transform must not be null.");
    }
    final List<AtomPair> sites =
        IntramolecularBondFormationTransformer.matchingSites(molecule, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(sites.size());
    for (int i = 0; i < sites.size(); ++i) {
      products.add(
          IntramolecularBondFormationTransformer.transformAt(molecule, transform, sites.get(i), i));
    }
    return List.copyOf(products);
  }

  private static MolecularTransformProduct transformAt(
      final Molecule molecule,
      final IntramolecularBondFormationTransform transform,
      final AtomPair site,
      final int siteIndex) {
    final ArrayList<Bond> bonds = new ArrayList<Bond>(molecule.bonds().size() + 1);
    bonds.addAll(molecule.bonds());
    bonds.add(Bond.of(site.firstAtomId(), site.secondAtomId(), transform.formedBondType()));
    final Molecule product =
        Molecule.of(
            MoleculeId.of(
                molecule.id().value()
                    + "_"
                    + transform.idSuffix()
                    + "_"
                    + site.firstAtomId().value()
                    + "_"
                    + site.secondAtomId().value()
                    + "_"
                    + siteIndex),
            molecule.displayName() + " " + transform.productNameSuffix(),
            molecule.atoms(),
            bonds,
            molecule.charge(),
            molecule.spinMultiplicity(),
            molecule.stereochemistry(),
            molecule.conformation(),
            molecule.opticalRotation(),
            molecule.symmetry(),
            molecule.metadata());
    return MolecularTransformProduct.of(product, site.firstAtomId(), site.secondAtomId());
  }

  private static List<AtomPair> matchingSites(
      final Molecule molecule, final IntramolecularBondFormationTransform transform) {
    final List<Atom> atoms = molecule.atoms();
    final ArrayList<AtomPair> sites = new ArrayList<AtomPair>();
    for (int firstIndex = 0; firstIndex < atoms.size(); ++firstIndex) {
      final Atom first = atoms.get(firstIndex);
      if (!transform.firstAtomSelector().matches(first)) {
        continue;
      }
      for (int secondIndex = 0; secondIndex < atoms.size(); ++secondIndex) {
        if (firstIndex == secondIndex) {
          continue;
        }
        final Atom second = atoms.get(secondIndex);
        if (transform.secondAtomSelector().matches(second)
            && !IntramolecularBondFormationTransformer.hasBond(molecule, first.id(), second.id())) {
          sites.add(new AtomPair(first.id(), second.id()));
        }
      }
    }
    return List.copyOf(sites);
  }

  private static boolean hasBond(
      final Molecule molecule, final AtomId firstAtomId, final AtomId secondAtomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      if (bonds.get(i).connects(firstAtomId, secondAtomId)) {
        return true;
      }
    }
    return false;
  }

  private static final class AtomPair {

    private final AtomId firstAtomId;
    private final AtomId secondAtomId;

    private AtomPair(final AtomId firstAtomId, final AtomId secondAtomId) {
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
