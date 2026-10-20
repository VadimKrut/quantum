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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.ReactantAtomSelector;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.SubstituentAttachmentTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;

/** Применяет добавление одноатомного заместителя без мутации исходной молекулы. */
public final class SubstituentAttachmentTransformer {

  public Molecule transform(
      final Molecule reactant, final SubstituentAttachmentTransform transform) {
    if (reactant == null) {
      throw new IllegalArgumentException("Substituent transform reactant must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Substituent transform specification must not be null.");
    }
    return this.transformAt(
            reactant, transform, this.targetAtom(reactant, transform.targetSelector()))
        .product();
  }

  public List<MolecularTransformProduct> transformAll(
      final Molecule reactant, final SubstituentAttachmentTransform transform) {
    if (reactant == null) {
      throw new IllegalArgumentException("Substituent transform reactant must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Substituent transform specification must not be null.");
    }
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>();
    final List<Atom> atoms = reactant.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!transform.targetSelector().matches(atom)) {
        continue;
      }
      products.add(this.transformAt(reactant, transform, atom));
    }
    return List.copyOf(products);
  }

  private MolecularTransformProduct transformAt(
      final Molecule reactant, final SubstituentAttachmentTransform transform, final Atom target) {
    final String siteSuffix = transform.idSuffix() + "_" + target.id().value();
    final AtomId substituentAtomId = AtomId.of(reactant.id().value() + "_" + siteSuffix);
    final ArrayList<Atom> atoms = new ArrayList<Atom>(reactant.atoms().size() + 1);
    atoms.addAll(reactant.atoms());
    atoms.add(
        Atom.of(
            substituentAtomId,
            transform.substituentElement(),
            SubstituentAttachmentTransformer.shiftedCoordinate(target.coordinate()),
            FormalCharge.NEUTRAL,
            (Isotope) null,
            RadicalState.CLOSED_SHELL,
            ChemistryMetadata.EMPTY));
    final ArrayList<Bond> bonds = new ArrayList<Bond>(reactant.bonds().size() + 1);
    bonds.addAll(reactant.bonds());
    bonds.add(Bond.of(target.id(), substituentAtomId, transform.bondType()));
    final Molecule product =
        Molecule.of(
            MoleculeId.of(reactant.id().value() + "_" + siteSuffix),
            reactant.displayName() + " " + transform.productNameSuffix(),
            atoms,
            bonds,
            reactant.charge(),
            reactant.spinMultiplicity(),
            reactant.stereochemistry(),
            reactant.conformation(),
            reactant.opticalRotation(),
            reactant.symmetry(),
            reactant.metadata());
    return MolecularTransformProduct.of(product, target.id(), null);
  }

  private Atom targetAtom(final Molecule reactant, final ReactantAtomSelector selector) {
    final List<Atom> atoms = reactant.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (selector.matches(atom)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("No reactant atom matches substituent transform selector.");
  }

  private static Coordinate3D shiftedCoordinate(final Coordinate3D coordinate) {
    if (coordinate == null) {
      return null;
    }
    return Coordinate3D.of(
        coordinate.x() + 1.45, coordinate.y(), coordinate.z(), coordinate.unit());
  }
}
