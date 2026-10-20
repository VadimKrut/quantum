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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.LeavingGroupReplacementTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;

/** Выполняет замену уходящей группы без мутации исходной молекулы. */
public final class LeavingGroupReplacementTransformer {

  public Molecule transform(
      final Molecule reactant, final LeavingGroupReplacementTransform transform) {
    if (reactant == null) {
      throw new IllegalArgumentException("Leaving-group transform reactant must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Leaving-group transform specification must not be null.");
    }
    return this.transformAt(reactant, transform, this.requiredMatches(reactant, transform).get(0))
        .product();
  }

  public List<MolecularTransformProduct> transformAll(
      final Molecule reactant, final LeavingGroupReplacementTransform transform) {
    if (reactant == null) {
      throw new IllegalArgumentException("Leaving-group transform reactant must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Leaving-group transform specification must not be null.");
    }
    final List<LeavingGroupMatch> matches = this.matches(reactant, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(matches.size());
    for (int i = 0; i < matches.size(); ++i) {
      products.add(this.transformAt(reactant, transform, matches.get(i)));
    }
    return List.copyOf(products);
  }

  private MolecularTransformProduct transformAt(
      final Molecule reactant,
      final LeavingGroupReplacementTransform transform,
      final LeavingGroupMatch match) {
    final String siteSuffix =
        transform.idSuffix()
            + "_"
            + match.anchor().id().value()
            + "_"
            + match.leavingGroup().id().value();
    final AtomId replacementAtomId = AtomId.of(reactant.id().value() + "_" + siteSuffix);
    final ArrayList<Atom> atoms = new ArrayList<Atom>(reactant.atoms().size());
    final List<Atom> reactantAtoms = reactant.atoms();
    for (int i = 0; i < reactantAtoms.size(); ++i) {
      final Atom atom = reactantAtoms.get(i);
      if (atom.id().equals(match.leavingGroup().id())) {
        continue;
      }
      atoms.add(atom);
    }
    atoms.add(
        Atom.of(
            replacementAtomId,
            transform.replacementElement(),
            LeavingGroupReplacementTransformer.replacementCoordinate(match),
            FormalCharge.NEUTRAL,
            (Isotope) null,
            RadicalState.CLOSED_SHELL,
            ChemistryMetadata.EMPTY));
    final ArrayList<Bond> bonds = new ArrayList<Bond>(reactant.bonds().size());
    final List<Bond> reactantBonds = reactant.bonds();
    for (int i = 0; i < reactantBonds.size(); ++i) {
      final Bond bond = reactantBonds.get(i);
      if (bond.connects(match.anchor().id(), match.leavingGroup().id())) {
        continue;
      }
      if (bond.firstAtomId().equals(match.leavingGroup().id())
          || bond.secondAtomId().equals(match.leavingGroup().id())) {
        continue;
      }
      bonds.add(bond);
    }
    bonds.add(Bond.of(match.anchor().id(), replacementAtomId, transform.replacementBondType()));
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
    return MolecularTransformProduct.of(
        product,
        List.of(
            LeavingGroupReplacementTransformer.leavingGroupCoproduct(reactant, match, transform)),
        match.anchor().id(),
        match.leavingGroup().id());
  }

  private static Molecule leavingGroupCoproduct(
      final Molecule reactant,
      final LeavingGroupMatch match,
      final LeavingGroupReplacementTransform transform) {
    final Atom leavingGroup =
        Atom.of(
            AtomId.of(
                reactant.id().value()
                    + "_"
                    + transform.idSuffix()
                    + "_leaving_"
                    + match.leavingGroup().id().value()),
            match.leavingGroup().element().symbol(),
            match.leavingGroup().coordinate(),
            match.leavingGroup().formalCharge(),
            match.leavingGroup().isotope(),
            match.leavingGroup().radicalState(),
            match.leavingGroup().metadata());
    return Molecule.of(
        MoleculeId.of(
            reactant.id().value()
                + "_"
                + transform.idSuffix()
                + "_leaving_group_coproduct_"
                + match.leavingGroup().id().value()),
        reactant.displayName() + " leaving-group coproduct",
        List.of(leavingGroup),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        Stereochemistry.EMPTY,
        MolecularConformation.EMPTY,
        OpticalRotation.UNKNOWN,
        MolecularSymmetry.C1,
        reactant.metadata());
  }

  private List<LeavingGroupMatch> requiredMatches(
      final Molecule reactant, final LeavingGroupReplacementTransform transform) {
    final List<LeavingGroupMatch> matches = this.matches(reactant, transform);
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No leaving group matches replacement transform.");
    }
    return matches;
  }

  private List<LeavingGroupMatch> matches(
      final Molecule reactant, final LeavingGroupReplacementTransform transform) {
    final ArrayList<LeavingGroupMatch> matches = new ArrayList<LeavingGroupMatch>();
    final List<Bond> bonds = reactant.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE) {
        continue;
      }
      final Atom first =
          LeavingGroupReplacementTransformer.requiredAtom(reactant, bond.firstAtomId());
      final Atom second =
          LeavingGroupReplacementTransformer.requiredAtom(reactant, bond.secondAtomId());
      final LeavingGroupMatch firstAsLeaving = this.match(reactant, first, second, transform);
      if (firstAsLeaving != null) {
        matches.add(firstAsLeaving);
      }
      final LeavingGroupMatch secondAsLeaving = this.match(reactant, second, first, transform);
      if (secondAsLeaving != null) {
        matches.add(secondAsLeaving);
      }
    }
    return List.copyOf(matches);
  }

  private LeavingGroupMatch match(
      final Molecule reactant,
      final Atom possibleLeavingGroup,
      final Atom possibleAnchor,
      final LeavingGroupReplacementTransform transform) {
    if (!possibleLeavingGroup.element().symbol().equals(transform.leavingGroupElement())) {
      return null;
    }
    if (!transform.anchorSelector().matches(possibleAnchor)) {
      return null;
    }
    // This transform replaces one terminal atom, not a ring atom or a connected multi-atom group.
    if (reactant.graph().degreeOf(possibleLeavingGroup.id()) != 1) {
      return null;
    }
    return new LeavingGroupMatch(possibleAnchor, possibleLeavingGroup);
  }

  private static Atom requiredAtom(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Molecule bond references unknown atom.");
  }

  private static Coordinate3D replacementCoordinate(final LeavingGroupMatch match) {
    if (match.leavingGroup().coordinate() != null) {
      return match.leavingGroup().coordinate();
    }
    if (match.anchor().coordinate() == null) {
      return null;
    }
    return Coordinate3D.of(
        match.anchor().coordinate().x() + 1.45,
        match.anchor().coordinate().y(),
        match.anchor().coordinate().z(),
        match.anchor().coordinate().unit());
  }

  /** Пара атомов, где anchor остается в молекуле, а leaving group удаляется. */
  private static final class LeavingGroupMatch {

    private final Atom anchor;
    private final Atom leavingGroup;

    private LeavingGroupMatch(final Atom anchor, final Atom leavingGroup) {
      this.anchor = anchor;
      this.leavingGroup = leavingGroup;
    }

    private Atom anchor() {
      return this.anchor;
    }

    private Atom leavingGroup() {
      return this.leavingGroup;
    }
  }
}
