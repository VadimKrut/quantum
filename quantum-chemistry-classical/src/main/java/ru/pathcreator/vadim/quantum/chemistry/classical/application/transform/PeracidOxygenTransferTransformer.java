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
import java.util.Set;
import java.util.stream.Collectors;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Transfers the terminal peroxide O of R-C(=O)-O-O-H and explicitly retains the acid coproduct. */
public final class PeracidOxygenTransferTransformer {
  public enum Kind {
    EPOXIDATION,
    BAEYER_VILLIGER
  }

  private record Site(AtomId first, AtomId second) {}

  private record Donor(AtomId proximal, AtomId transferred, AtomId hydrogen) {}

  public boolean acceptsSubstrate(final Molecule molecule, final Kind kind) {
    return !sites(molecule, kind).isEmpty();
  }

  public List<MolecularTransformProduct> transformAll(
      final Molecule source, final Molecule reagent, final Kind kind) {
    final var products = new ArrayList<MolecularTransformProduct>();
    final var sites = sites(source, kind);
    if (sites.isEmpty()) {
      return List.of();
    }
    for (final var donor : donors(reagent)) {
      for (final var site : sites) {
        products.add(transfer(source, reagent, kind, site, donor));
      }
    }
    return List.copyOf(products);
  }

  private static boolean neutralClosed(final Molecule molecule) {
    return molecule.charge().value() == 0
        && molecule.spinMultiplicity().value() == 1
        && molecule.graph().connected()
        && molecule.atoms().stream()
            .allMatch(
                atom ->
                    atom.formalCharge().value() == 0
                        && atom.radicalState().equals(RadicalState.CLOSED_SHELL));
  }

  private static List<Site> sites(final Molecule molecule, final Kind kind) {
    java.util.Objects.requireNonNull(kind);
    if (!neutralClosed(molecule)) {
      return List.of();
    }
    final var sites = new ArrayList<Site>();
    for (final var bond : molecule.bonds()) {
      if (molecule.aromaticity().isAromaticBond(bond)) {
        continue;
      }
      final var first = atom(molecule, bond.firstAtomId());
      final var second = atom(molecule, bond.secondAtomId());
      if (!element(first, "C") || !element(second, "C")) {
        continue;
      }
      if (kind == Kind.EPOXIDATION
          && bond.type() == BondType.DOUBLE
          && valence(molecule, first.id()) == 4
          && valence(molecule, second.id()) == 4) {
        sites.add(new Site(first.id(), second.id()));
      } else if (kind == Kind.BAEYER_VILLIGER && bond.type() == BondType.SINGLE) {
        if (ketoneCenter(molecule, first.id()) && valence(molecule, second.id()) == 4) {
          sites.add(new Site(first.id(), second.id()));
        }
        if (ketoneCenter(molecule, second.id()) && valence(molecule, first.id()) == 4) {
          sites.add(new Site(second.id(), first.id()));
        }
      }
    }
    return List.copyOf(sites);
  }

  private static boolean ketoneCenter(final Molecule molecule, final AtomId id) {
    final var bonds = bonds(molecule, id);
    return bonds.size() == 3
        && bonds.stream()
                .filter(
                    bond ->
                        bond.type() == BondType.DOUBLE
                            && element(atom(molecule, other(bond, id)), "O")
                            && bonds(molecule, other(bond, id)).size() == 1)
                .count()
            == 1
        && bonds.stream()
                .filter(
                    bond ->
                        bond.type() == BondType.SINGLE
                            && element(atom(molecule, other(bond, id)), "C"))
                .count()
            == 2;
  }

  private static List<Donor> donors(final Molecule molecule) {
    if (!neutralClosed(molecule)) {
      return List.of();
    }
    final var donors = new ArrayList<Donor>();
    for (final var oxygen : molecule.atoms()) {
      if (!element(oxygen, "O")) {
        continue;
      }
      final var terminalBonds = bonds(molecule, oxygen.id());
      if (terminalBonds.size() != 2
          || terminalBonds.stream().anyMatch(bond -> bond.type() != BondType.SINGLE)) {
        continue;
      }
      final var hydrogen =
          terminalBonds.stream()
              .map(bond -> atom(molecule, other(bond, oxygen.id())))
              .filter(atom -> element(atom, "H") && bonds(molecule, atom.id()).size() == 1)
              .findFirst()
              .orElse(null);
      final var proximal =
          terminalBonds.stream()
              .map(bond -> atom(molecule, other(bond, oxygen.id())))
              .filter(atom -> element(atom, "O") && bonds(molecule, atom.id()).size() == 2)
              .findFirst()
              .orElse(null);
      if (hydrogen == null || proximal == null) {
        continue;
      }
      final var carbon =
          bonds(molecule, proximal.id()).stream()
              .filter(bond -> bond.type() == BondType.SINGLE)
              .map(bond -> atom(molecule, other(bond, proximal.id())))
              .filter(atom -> element(atom, "C"))
              .findFirst()
              .orElse(null);
      if (carbon == null
          || valence(molecule, carbon.id()) != 4
          || bonds(molecule, carbon.id()).size() != 3) {
        continue;
      }
      if (bonds(molecule, carbon.id()).stream()
          .noneMatch(
              bond ->
                  bond.type() == BondType.DOUBLE
                      && element(atom(molecule, other(bond, carbon.id())), "O")
                      && bonds(molecule, other(bond, carbon.id())).size() == 1)) {
        continue;
      }
      // A carboxylic peracid, not a peroxycarbonate or a disconnected O/H inventory.
      if (bonds(molecule, carbon.id()).stream()
          .noneMatch(
              bond ->
                  bond.type() == BondType.SINGLE
                      && (element(atom(molecule, other(bond, carbon.id())), "C")
                          || element(atom(molecule, other(bond, carbon.id())), "H")))) {
        continue;
      }
      donors.add(new Donor(proximal.id(), oxygen.id(), hydrogen.id()));
    }
    return List.copyOf(donors);
  }

  private static MolecularTransformProduct transfer(
      final Molecule source,
      final Molecule reagent,
      final Kind kind,
      final Site site,
      final Donor donor) {
    final var transferred = atom(reagent, donor.transferred());
    final Set<AtomId> sourceIds = source.atoms().stream().map(Atom::id).collect(Collectors.toSet());
    String name = "donor_" + reagent.id().value() + "_" + transferred.id().value();
    while (sourceIds.contains(AtomId.of(name))) {
      name += "_";
    }
    final AtomId inserted = AtomId.of(name);
    final var atoms =
        new ArrayList<>(
            source.atoms().stream().map(atom -> withoutCoordinates(atom, atom.id())).toList());
    atoms.add(withoutCoordinates(transferred, inserted));
    final var bonds =
        new ArrayList<>(
            source.bonds().stream()
                .filter(bond -> !bond.connects(site.first(), site.second()))
                .toList());
    if (kind == Kind.EPOXIDATION) {
      bonds.add(Bond.of(site.first(), site.second(), BondType.SINGLE));
    }
    bonds.add(Bond.of(site.first(), inserted, BondType.SINGLE));
    bonds.add(Bond.of(inserted, site.second(), BondType.SINGLE));
    final String suffix =
        kind.name().toLowerCase(java.util.Locale.ROOT)
            + "_"
            + site.first().value()
            + "_"
            + site.second().value()
            + "_from_"
            + reagent.id().value()
            + "_"
            + donor.transferred().value();
    final var product =
        product(
            source,
            source.id().value() + "_" + suffix,
            source.displayName() + (kind == Kind.EPOXIDATION ? " · epoxide" : " · ester/lactone"),
            atoms,
            bonds,
            Set.of(site.first(), site.second()),
            reagent,
            donor);
    final var acidAtoms =
        reagent.atoms().stream()
            .filter(atom -> !atom.id().equals(donor.transferred()))
            .map(atom -> withoutCoordinates(atom, atom.id()))
            .toList();
    final var acidBonds =
        new ArrayList<>(
            reagent.bonds().stream()
                .filter(
                    bond ->
                        !bond.firstAtomId().equals(donor.transferred())
                            && !bond.secondAtomId().equals(donor.transferred()))
                .toList());
    acidBonds.add(Bond.of(donor.proximal(), donor.hydrogen(), BondType.SINGLE));
    final var acid =
        product(
            reagent,
            reagent.id().value() + "_acid_" + donor.transferred().value(),
            "Acid coproduct from " + reagent.displayName(),
            acidAtoms,
            acidBonds,
            Set.of(donor.proximal(), donor.transferred(), donor.hydrogen()),
            reagent,
            donor);
    return MolecularTransformProduct.of(product, List.of(acid), site.first(), null);
  }

  private static Molecule product(
      final Molecule source,
      final String id,
      final String name,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final Set<AtomId> changed,
      final Molecule reagent,
      final Donor donor) {
    final var present = atoms.stream().map(Atom::id).collect(Collectors.toSet());
    final var stereo =
        Stereochemistry.of(
            source.stereochemistry().centers().stream()
                .filter(
                    center ->
                        present.contains(center.primaryAtomId())
                            && !changed.contains(center.primaryAtomId())
                            && (center.secondaryAtomId() == null
                                || present.contains(center.secondaryAtomId())
                                    && !changed.contains(center.secondaryAtomId()))
                            && present.containsAll(center.referenceAtomIds()))
                .toList());
    final var attributes = new HashMap<>(source.metadata().attributes());
    attributes.keySet().removeIf(key -> key.startsWith("laboratory.geometry."));
    attributes.remove("formula");
    attributes.remove("library_note");
    attributes.put(
        Stereochemistry.REVIEW_ATTRIBUTE,
        "Граф изменён переносом O. Проверьте стереохимию; новая конфигурация не назначалась. Координаты и конформация требуют нового расчёта.");
    attributes.put("reaction.donor", reagent.id().value());
    attributes.put("reaction.donor.oxygen", donor.transferred().value());
    return Molecule.of(
        MoleculeId.of(id),
        name,
        atoms,
        bonds,
        source.charge(),
        source.spinMultiplicity(),
        stereo,
        ChemistryMetadata.of(source.metadata().source(), source.metadata().location(), attributes));
  }

  private static Atom withoutCoordinates(final Atom atom, final AtomId id) {
    return Atom.of(
        id,
        atom.element().symbol(),
        null,
        atom.formalCharge(),
        atom.isotope(),
        atom.radicalState(),
        atom.metadata());
  }

  private static boolean element(final Atom atom, final String symbol) {
    return atom.element().symbol().value().equals(symbol);
  }

  private static Atom atom(final Molecule molecule, final AtomId id) {
    return molecule.atoms().stream().filter(atom -> atom.id().equals(id)).findFirst().orElseThrow();
  }

  private static AtomId other(final Bond bond, final AtomId id) {
    return bond.firstAtomId().equals(id) ? bond.secondAtomId() : bond.firstAtomId();
  }

  private static List<Bond> bonds(final Molecule molecule, final AtomId id) {
    return molecule.bonds().stream()
        .filter(bond -> bond.firstAtomId().equals(id) || bond.secondAtomId().equals(id))
        .toList();
  }

  private static int valence(final Molecule molecule, final AtomId id) {
    return bonds(molecule, id).stream()
        .mapToInt(
            bond ->
                switch (bond.type()) {
                  case SINGLE -> 1;
                  case DOUBLE -> 2;
                  case TRIPLE -> 3;
                  default -> 100;
                })
        .sum();
  }
}
