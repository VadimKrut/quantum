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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.FragmentTransferTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Собирает продукт переносом целого фрагмента из второго реагента в первый реагент. */
public final class FragmentTransferTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule scaffold, final Molecule fragment, final FragmentTransferTransform transform) {
    if (scaffold == null) {
      throw new IllegalArgumentException("Fragment transfer scaffold molecule must not be null.");
    }
    if (fragment == null) {
      throw new IllegalArgumentException("Fragment transfer fragment molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Fragment transfer specification must not be null.");
    }
    if (scaffold.equals(fragment)) {
      return List.of();
    }
    final List<AtomPair> scaffoldSites =
        FragmentTransferTransformer.scaffoldSites(scaffold, transform);
    final List<AtomPair> fragmentSites =
        FragmentTransferTransformer.fragmentSites(fragment, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(scaffoldSites.size() * fragmentSites.size());
    for (int scaffoldIndex = 0; scaffoldIndex < scaffoldSites.size(); ++scaffoldIndex) {
      final AtomPair scaffoldSite = scaffoldSites.get(scaffoldIndex);
      for (int fragmentIndex = 0; fragmentIndex < fragmentSites.size(); ++fragmentIndex) {
        products.add(
            FragmentTransferTransformer.transformAt(
                scaffold,
                fragment,
                transform,
                scaffoldSite,
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
      final FragmentTransferTransform transform,
      final AtomPair scaffoldSite,
      final AtomPair fragmentSite,
      final int scaffoldIndex,
      final int fragmentIndex) {
    final String suffix =
        transform.idSuffix()
            + "_"
            + scaffoldSite.anchorId().value()
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
    final Set<AtomId> scaffoldRemovedAtoms =
        FragmentTransferTransformer.removedHandleAtoms(scaffold, scaffoldSite.leavingId());
    final Set<AtomId> fragmentRemovedAtoms =
        FragmentTransferTransformer.removedHandleAtoms(fragment, fragmentSite.leavingId());
    FragmentTransferTransformer.copyAtoms(
        scaffold, "scaffold", scaffoldRemovedAtoms, scaffoldIdMap, atoms);
    FragmentTransferTransformer.copyAtoms(
        fragment, "fragment", fragmentRemovedAtoms, fragmentIdMap, atoms);
    final ArrayList<Bond> bonds =
        new ArrayList<Bond>(scaffold.bonds().size() + fragment.bonds().size() + 1);
    FragmentTransferTransformer.copyBonds(scaffold, scaffoldRemovedAtoms, scaffoldIdMap, bonds);
    FragmentTransferTransformer.copyBonds(fragment, fragmentRemovedAtoms, fragmentIdMap, bonds);
    bonds.add(
        Bond.of(
            scaffoldIdMap.get(scaffoldSite.anchorId()),
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
            FragmentTransferTransformer.chargeOf(atoms),
            FragmentTransferTransformer.spinMultiplicityOf(atoms),
            ChemistryMetadata.EMPTY);
    return MolecularTransformProduct.of(
        product,
        FragmentTransferTransformer.coproducts(
            scaffold,
            fragment,
            transform,
            scaffoldSite,
            fragmentSite,
            scaffoldRemovedAtoms,
            fragmentRemovedAtoms,
            suffix),
        scaffoldSite.anchorId(),
        scaffoldSite.leavingId());
  }

  private static List<Molecule> coproducts(
      final Molecule scaffold,
      final Molecule fragment,
      final FragmentTransferTransform transform,
      final AtomPair scaffoldSite,
      final AtomPair fragmentSite,
      final Set<AtomId> scaffoldRemovedAtoms,
      final Set<AtomId> fragmentRemovedAtoms,
      final String suffix) {
    if (!transform.hasCoproduct()) {
      return List.of();
    }
    final HashMap<AtomId, AtomId> scaffoldIdMap = new HashMap<AtomId, AtomId>();
    final HashMap<AtomId, AtomId> fragmentIdMap = new HashMap<AtomId, AtomId>();
    final ArrayList<Atom> atoms =
        new ArrayList<Atom>(scaffoldRemovedAtoms.size() + fragmentRemovedAtoms.size());
    FragmentTransferTransformer.copyRemovedAtoms(
        scaffold, "coproduct_scaffold", scaffoldRemovedAtoms, scaffoldIdMap, atoms);
    FragmentTransferTransformer.copyRemovedAtoms(
        fragment, "coproduct_fragment", fragmentRemovedAtoms, fragmentIdMap, atoms);
    final ArrayList<Bond> bonds =
        new ArrayList<Bond>(scaffold.bonds().size() + fragment.bonds().size() + 1);
    FragmentTransferTransformer.copyRemovedBonds(
        scaffold, scaffoldRemovedAtoms, scaffoldIdMap, bonds);
    FragmentTransferTransformer.copyRemovedBonds(
        fragment, fragmentRemovedAtoms, fragmentIdMap, bonds);
    final AtomId scaffoldLeavingId = scaffoldIdMap.get(scaffoldSite.leavingId());
    final AtomId fragmentLeavingId = fragmentIdMap.get(fragmentSite.leavingId());
    if (scaffoldLeavingId == null || fragmentLeavingId == null) {
      throw new IllegalStateException("Fragment transfer coproduct handles were not copied.");
    }
    bonds.add(Bond.of(scaffoldLeavingId, fragmentLeavingId, transform.coproductBondType()));
    final Molecule coproduct =
        Molecule.of(
            MoleculeId.of(
                scaffold.id().value() + "_" + fragment.id().value() + "_" + suffix + "_coproduct"),
            scaffold.displayName()
                + " "
                + fragment.displayName()
                + " "
                + transform.coproductNameSuffix(),
            atoms,
            bonds,
            FragmentTransferTransformer.chargeOf(atoms),
            FragmentTransferTransformer.spinMultiplicityOf(atoms),
            ChemistryMetadata.EMPTY);
    return List.of(coproduct);
  }

  private static void copyRemovedAtoms(
      final Molecule molecule,
      final String prefix,
      final Set<AtomId> removedAtomIds,
      final Map<AtomId, AtomId> idMap,
      final List<Atom> target) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!removedAtomIds.contains(atom.id())) {
        continue;
      }
      final AtomId copiedId = AtomId.of(prefix + "_" + atom.id().value());
      idMap.put(atom.id(), copiedId);
      target.add(FragmentTransferTransformer.copyAtom(atom, copiedId));
    }
  }

  private static void copyRemovedBonds(
      final Molecule molecule,
      final Set<AtomId> removedAtomIds,
      final Map<AtomId, AtomId> idMap,
      final List<Bond> target) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!removedAtomIds.contains(bond.firstAtomId())
          || !removedAtomIds.contains(bond.secondAtomId())) {
        continue;
      }
      target.add(
          Bond.of(idMap.get(bond.firstAtomId()), idMap.get(bond.secondAtomId()), bond.type()));
    }
  }

  private static List<AtomPair> scaffoldSites(
      final Molecule molecule, final FragmentTransferTransform transform) {
    return FragmentTransferTransformer.bondedPairs(
        molecule, transform.scaffoldAnchorSelector(), transform.scaffoldLeavingSelector());
  }

  private static List<AtomPair> fragmentSites(
      final Molecule molecule, final FragmentTransferTransform transform) {
    return FragmentTransferTransformer.bondedPairs(
        molecule, transform.fragmentAnchorSelector(), transform.fragmentLeavingSelector());
  }

  private static List<AtomPair> bondedPairs(
      final Molecule molecule,
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.ReactantAtomSelector
          anchorSelector,
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.ReactantAtomSelector
          leavingSelector) {
    final ArrayList<AtomPair> sites = new ArrayList<AtomPair>();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Atom first = FragmentTransferTransformer.atom(molecule, bond.firstAtomId());
      final Atom second = FragmentTransferTransformer.atom(molecule, bond.secondAtomId());
      if (anchorSelector.matches(first) && leavingSelector.matches(second)) {
        sites.add(new AtomPair(first.id(), second.id()));
      }
      if (anchorSelector.matches(second) && leavingSelector.matches(first)) {
        sites.add(new AtomPair(second.id(), first.id()));
      }
    }
    return List.copyOf(sites);
  }

  private static void copyAtoms(
      final Molecule molecule,
      final String prefix,
      final Set<AtomId> removedAtomIds,
      final Map<AtomId, AtomId> idMap,
      final List<Atom> target) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (removedAtomIds.contains(atom.id())) {
        continue;
      }
      final AtomId copiedId = AtomId.of(prefix + "_" + atom.id().value());
      idMap.put(atom.id(), copiedId);
      target.add(FragmentTransferTransformer.copyAtom(atom, copiedId));
    }
  }

  private static Atom copyAtom(final Atom atom, final AtomId copiedId) {
    return Atom.of(
        copiedId,
        atom.element().symbol(),
        atom.coordinate(),
        atom.formalCharge(),
        atom.isotope(),
        atom.radicalState(),
        atom.metadata());
  }

  private static void copyBonds(
      final Molecule molecule,
      final Set<AtomId> removedAtomIds,
      final Map<AtomId, AtomId> idMap,
      final List<Bond> target) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (removedAtomIds.contains(bond.firstAtomId())
          || removedAtomIds.contains(bond.secondAtomId())) {
        continue;
      }
      target.add(
          Bond.of(idMap.get(bond.firstAtomId()), idMap.get(bond.secondAtomId()), bond.type()));
    }
  }

  private static Set<AtomId> removedHandleAtoms(
      final Molecule molecule, final AtomId leavingAtomId) {
    final HashSet<AtomId> removedAtoms = new HashSet<AtomId>();
    removedAtoms.add(leavingAtomId);
    final Atom leavingAtom = FragmentTransferTransformer.atom(molecule, leavingAtomId);
    if (!"B".equals(leavingAtom.element().symbol().value())) {
      return Set.copyOf(removedAtoms);
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final AtomId neighborId = FragmentTransferTransformer.otherEndpoint(bond, leavingAtomId);
      if (neighborId == null || FragmentTransferTransformer.degree(molecule, neighborId) != 1) {
        continue;
      }
      final Atom neighbor = FragmentTransferTransformer.atom(molecule, neighborId);
      if ("O".equals(neighbor.element().symbol().value())) {
        removedAtoms.add(neighborId);
      }
    }
    return Set.copyOf(removedAtoms);
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

  private static int degree(final Molecule molecule, final AtomId atomId) {
    int count = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.firstAtomId().equals(atomId) || bond.secondAtomId().equals(atomId)) {
        ++count;
      }
    }
    return count;
  }

  private static Atom atom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Fragment transfer molecule bond references unknown atom.");
  }

  private static MolecularCharge chargeOf(final List<Atom> atoms) {
    int charge = 0;
    for (int i = 0; i < atoms.size(); ++i) {
      charge = Math.addExact(charge, atoms.get(i).formalCharge().value());
    }
    return MolecularCharge.of(charge);
  }

  private static SpinMultiplicity spinMultiplicityOf(final List<Atom> atoms) {
    int unpairedElectrons = 0;
    for (int i = 0; i < atoms.size(); ++i) {
      unpairedElectrons =
          Math.addExact(unpairedElectrons, atoms.get(i).radicalState().unpairedElectrons());
    }
    return SpinMultiplicity.of(unpairedElectrons + 1);
  }

  private static final class AtomPair {

    private final AtomId anchorId;
    private final AtomId leavingId;

    private AtomPair(final AtomId anchorId, final AtomId leavingId) {
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
