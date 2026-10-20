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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.FragmentAttachmentTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.ReactantAtomSelector;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Собирает продукт присоединением реального фрагмента второго реагента к выбранному атому первого реагента. */
public final class FragmentAttachmentTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule scaffold,
      final Molecule fragment,
      final FragmentAttachmentTransform transform) {
    if (scaffold == null) {
      throw new IllegalArgumentException("Fragment attachment scaffold molecule must not be null.");
    }
    if (fragment == null) {
      throw new IllegalArgumentException("Fragment attachment fragment molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Fragment attachment specification must not be null.");
    }
    if (scaffold.equals(fragment)) {
      return List.of();
    }
    final List<AtomId> scaffoldSites =
        FragmentAttachmentTransformer.anchorSites(scaffold, transform.scaffoldAnchorSelector());
    final List<FragmentSite> fragmentSites =
        FragmentAttachmentTransformer.fragmentSites(fragment, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(scaffoldSites.size() * fragmentSites.size());
    for (int scaffoldIndex = 0; scaffoldIndex < scaffoldSites.size(); ++scaffoldIndex) {
      final AtomId scaffoldAnchorId = scaffoldSites.get(scaffoldIndex);
      for (int fragmentIndex = 0; fragmentIndex < fragmentSites.size(); ++fragmentIndex) {
        products.add(
            FragmentAttachmentTransformer.transformAt(
                scaffold,
                fragment,
                transform,
                scaffoldAnchorId,
                fragmentSites.get(fragmentIndex),
                scaffoldIndex,
                fragmentIndex));
      }
    }
    return List.copyOf(products);
  }

  private static MolecularTransformProduct transformAt(
      final Molecule scaffold,
      final Molecule fragment,
      final FragmentAttachmentTransform transform,
      final AtomId scaffoldAnchorId,
      final FragmentSite fragmentSite,
      final int scaffoldIndex,
      final int fragmentIndex) {
    final String suffix =
        transform.idSuffix()
            + "_"
            + scaffoldAnchorId.value()
            + "_"
            + fragmentSite.anchorId().value()
            + "_"
            + scaffoldIndex
            + "_"
            + fragmentIndex;
    final HashMap<AtomId, AtomId> scaffoldIdMap = new HashMap<AtomId, AtomId>();
    final HashMap<AtomId, AtomId> fragmentIdMap = new HashMap<AtomId, AtomId>();
    final ArrayList<Atom> atoms =
        new ArrayList<Atom>(scaffold.atoms().size() + fragment.atoms().size());
    FragmentAttachmentTransformer.copyAtoms(scaffold, "scaffold", null, scaffoldIdMap, atoms);
    FragmentAttachmentTransformer.copyAtoms(
        fragment, "fragment", fragmentSite.leavingId(), fragmentIdMap, atoms);
    final ArrayList<Bond> bonds =
        new ArrayList<Bond>(scaffold.bonds().size() + fragment.bonds().size() + 1);
    FragmentAttachmentTransformer.copyBonds(scaffold, null, scaffoldIdMap, bonds);
    FragmentAttachmentTransformer.copyBonds(
        fragment, fragmentSite.leavingId(), fragmentIdMap, bonds);
    bonds.add(
        Bond.of(
            scaffoldIdMap.get(scaffoldAnchorId),
            fragmentIdMap.get(fragmentSite.anchorId()),
            transform.connectingBondType()));
    final Molecule product =
        Molecule.of(
            MoleculeId.of(scaffold.id().value() + "_" + fragment.id().value() + "_" + suffix),
            scaffold.displayName()
                + " "
                + fragment.displayName()
                + " "
                + transform.productNameSuffix(),
            atoms,
            bonds,
            scaffold.charge(),
            scaffold.spinMultiplicity(),
            scaffold.stereochemistry(),
            scaffold.conformation(),
            scaffold.opticalRotation(),
            scaffold.symmetry(),
            ChemistryMetadata.EMPTY);
    return MolecularTransformProduct.of(
        product,
        List.of(
            FragmentAttachmentTransformer.leavingCoproduct(
                scaffold, fragment, transform, fragmentSite)),
        scaffoldAnchorId,
        fragmentSite.leavingId());
  }

  private static Molecule leavingCoproduct(
      final Molecule scaffold,
      final Molecule fragment,
      final FragmentAttachmentTransform transform,
      final FragmentSite fragmentSite) {
    final Atom leavingAtom = FragmentAttachmentTransformer.atom(fragment, fragmentSite.leavingId());
    final Atom copiedLeavingAtom =
        Atom.of(
            AtomId.of(
                scaffold.id().value()
                    + "_"
                    + fragment.id().value()
                    + "_"
                    + transform.idSuffix()
                    + "_leaving_"
                    + leavingAtom.id().value()),
            leavingAtom.element().symbol(),
            leavingAtom.coordinate(),
            leavingAtom.formalCharge(),
            leavingAtom.isotope(),
            leavingAtom.radicalState(),
            leavingAtom.metadata());
    return Molecule.of(
        MoleculeId.of(
            scaffold.id().value()
                + "_"
                + fragment.id().value()
                + "_"
                + transform.idSuffix()
                + "_fragment_leaving_coproduct_"
                + leavingAtom.id().value()),
        fragment.displayName() + " leaving-group coproduct",
        List.of(copiedLeavingAtom),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.EMPTY);
  }

  private static List<AtomId> anchorSites(
      final Molecule molecule, final ReactantAtomSelector selector) {
    final ArrayList<AtomId> sites = new ArrayList<AtomId>();
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (selector.matches(atom)) {
        sites.add(atom.id());
      }
    }
    return List.copyOf(sites);
  }

  private static List<FragmentSite> fragmentSites(
      final Molecule molecule, final FragmentAttachmentTransform transform) {
    final ArrayList<FragmentSite> sites = new ArrayList<FragmentSite>();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Atom first = FragmentAttachmentTransformer.atom(molecule, bond.firstAtomId());
      final Atom second = FragmentAttachmentTransformer.atom(molecule, bond.secondAtomId());
      if (transform.fragmentAnchorSelector().matches(first)
          && transform.fragmentLeavingSelector().matches(second)) {
        sites.add(new FragmentSite(first.id(), second.id()));
      }
      if (transform.fragmentAnchorSelector().matches(second)
          && transform.fragmentLeavingSelector().matches(first)) {
        sites.add(new FragmentSite(second.id(), first.id()));
      }
    }
    return List.copyOf(sites);
  }

  private static void copyAtoms(
      final Molecule molecule,
      final String prefix,
      final AtomId removedAtomId,
      final Map<AtomId, AtomId> idMap,
      final List<Atom> target) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (removedAtomId != null && atom.id().equals(removedAtomId)) {
        continue;
      }
      final AtomId copiedId = AtomId.of(prefix + "_" + atom.id().value());
      idMap.put(atom.id(), copiedId);
      target.add(
          Atom.of(
              copiedId,
              atom.element().symbol(),
              atom.coordinate(),
              atom.formalCharge(),
              atom.isotope(),
              atom.radicalState(),
              atom.metadata()));
    }
  }

  private static void copyBonds(
      final Molecule molecule,
      final AtomId removedAtomId,
      final Map<AtomId, AtomId> idMap,
      final List<Bond> target) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if ((removedAtomId != null)
          && (bond.firstAtomId().equals(removedAtomId)
              || bond.secondAtomId().equals(removedAtomId))) {
        continue;
      }
      target.add(
          Bond.of(idMap.get(bond.firstAtomId()), idMap.get(bond.secondAtomId()), bond.type()));
    }
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException(
        "Fragment attachment molecule bond references unknown atom.");
  }

  private static final class FragmentSite {

    private final AtomId anchorId;
    private final AtomId leavingId;

    private FragmentSite(final AtomId anchorId, final AtomId leavingId) {
      this.anchorId = anchorId;
      this.leavingId = leavingId;
    }

    private AtomId anchorId() {
      return this.anchorId;
    }

    private AtomId leavingId() {
      return this.leavingId;
    }
  }
}
