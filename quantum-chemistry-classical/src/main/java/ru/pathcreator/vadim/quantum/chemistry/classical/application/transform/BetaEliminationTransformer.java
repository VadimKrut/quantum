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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BetaEliminationTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/** Создает продукты beta-elimination из молекулярного графа без мутации исходной молекулы. */
public final class BetaEliminationTransformer {

  public List<MolecularTransformProduct> transformAll(
      final Molecule molecule, final BetaEliminationTransform transform) {
    if (molecule == null) {
      throw new IllegalArgumentException("Beta-elimination molecule must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Beta-elimination transform must not be null.");
    }
    final List<EliminationSite> sites =
        BetaEliminationTransformer.matchingSites(molecule, transform);
    final ArrayList<MolecularTransformProduct> products =
        new ArrayList<MolecularTransformProduct>(sites.size());
    for (int i = 0; i < sites.size(); ++i) {
      products.add(BetaEliminationTransformer.transformAt(molecule, transform, sites.get(i), i));
    }
    return List.copyOf(products);
  }

  private static MolecularTransformProduct transformAt(
      final Molecule molecule,
      final BetaEliminationTransform transform,
      final EliminationSite site,
      final int siteIndex) {
    final ArrayList<Atom> atoms = new ArrayList<Atom>(molecule.atoms().size() - 1);
    final List<Atom> sourceAtoms = molecule.atoms();
    for (int i = 0; i < sourceAtoms.size(); ++i) {
      final Atom atom = sourceAtoms.get(i);
      if (!atom.id().equals(site.leavingGroupId())) {
        atoms.add(atom);
      }
    }
    final ArrayList<Bond> bonds = new ArrayList<Bond>(molecule.bonds().size() - 1);
    final List<Bond> sourceBonds = molecule.bonds();
    for (int i = 0; i < sourceBonds.size(); ++i) {
      final Bond bond = sourceBonds.get(i);
      if (bond.firstAtomId().equals(site.leavingGroupId())
          || bond.secondAtomId().equals(site.leavingGroupId())) {
        continue;
      }
      if (bond.connects(site.alphaAnchorId(), site.betaAnchorId())) {
        bonds.add(
            Bond.of(
                site.alphaAnchorId(), site.betaAnchorId(), transform.alphaBetaProductBondType()));
        continue;
      }
      bonds.add(bond);
    }
    final Molecule product =
        Molecule.of(
            MoleculeId.of(
                molecule.id().value()
                    + "_"
                    + transform.idSuffix()
                    + "_"
                    + site.alphaAnchorId().value()
                    + "_"
                    + site.betaAnchorId().value()
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
            ChemistryMetadata.EMPTY);
    return MolecularTransformProduct.of(
        product,
        List.of(BetaEliminationTransformer.leavingCoproduct(molecule, transform, site)),
        site.alphaAnchorId(),
        site.leavingGroupId());
  }

  private static Molecule leavingCoproduct(
      final Molecule molecule,
      final BetaEliminationTransform transform,
      final EliminationSite site) {
    final Atom leavingAtom = BetaEliminationTransformer.atom(molecule, site.leavingGroupId());
    final Atom coproductAtom =
        Atom.of(
            AtomId.of("leaving_" + leavingAtom.id().value()),
            leavingAtom.element().symbol(),
            leavingAtom.coordinate(),
            leavingAtom.formalCharge(),
            leavingAtom.isotope(),
            leavingAtom.radicalState(),
            ChemistryMetadata.EMPTY);
    return Molecule.of(
        MoleculeId.of(
            molecule.id().value()
                + "_"
                + transform.idSuffix()
                + "_beta_leaving_coproduct_"
                + site.leavingGroupId().value()),
        molecule.displayName() + " beta-elimination leaving-group coproduct",
        List.of(coproductAtom),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.EMPTY);
  }

  private static List<EliminationSite> matchingSites(
      final Molecule molecule, final BetaEliminationTransform transform) {
    final ArrayList<EliminationSite> sites = new ArrayList<EliminationSite>();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond leavingBond = bonds.get(i);
      final Atom first = BetaEliminationTransformer.atom(molecule, leavingBond.firstAtomId());
      final Atom second = BetaEliminationTransformer.atom(molecule, leavingBond.secondAtomId());
      BetaEliminationTransformer.addSitesForDirectedLeavingBond(
          molecule, transform, sites, first, second);
      BetaEliminationTransformer.addSitesForDirectedLeavingBond(
          molecule, transform, sites, second, first);
    }
    return List.copyOf(sites);
  }

  private static void addSitesForDirectedLeavingBond(
      final Molecule molecule,
      final BetaEliminationTransform transform,
      final List<EliminationSite> sites,
      final Atom alpha,
      final Atom leaving) {
    if (!transform.alphaAnchorSelector().matches(alpha)
        || !transform.leavingGroupSelector().matches(leaving)) {
      return;
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond alphaBetaBond = bonds.get(i);
      if (alphaBetaBond.type() != transform.alphaBetaSourceBondType()) {
        continue;
      }
      final AtomId betaId = BetaEliminationTransformer.otherEndpoint(alphaBetaBond, alpha.id());
      if (betaId == null || betaId.equals(leaving.id())) {
        continue;
      }
      final Atom beta = BetaEliminationTransformer.atom(molecule, betaId);
      if (transform.betaAnchorSelector().matches(beta)) {
        sites.add(new EliminationSite(alpha.id(), beta.id(), leaving.id()));
      }
    }
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
    throw new IllegalArgumentException("Beta-elimination molecule bond references unknown atom.");
  }

  private static final class EliminationSite {

    private final AtomId alphaAnchorId;
    private final AtomId betaAnchorId;
    private final AtomId leavingGroupId;

    private EliminationSite(
        final AtomId alphaAnchorId, final AtomId betaAnchorId, final AtomId leavingGroupId) {
      this.alphaAnchorId = alphaAnchorId;
      this.betaAnchorId = betaAnchorId;
      this.leavingGroupId = leavingGroupId;
    }

    private AtomId alphaAnchorId() {
      return this.alphaAnchorId;
    }

    private AtomId betaAnchorId() {
      return this.betaAnchorId;
    }

    private AtomId leavingGroupId() {
      return this.leavingGroupId;
    }
  }
}
