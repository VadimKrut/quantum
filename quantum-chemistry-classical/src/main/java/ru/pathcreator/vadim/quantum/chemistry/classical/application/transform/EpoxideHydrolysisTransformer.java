/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.transform;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Opens an explicit C-O-C three-membered ring with one explicit water molecule. */
public final class EpoxideHydrolysisTransformer {
  private record Site(AtomId oxygen, AtomId attacked, AtomId retained) {}

  public boolean acceptsSubstrate(final Molecule molecule) {
    return !sites(molecule).isEmpty();
  }

  public List<MolecularTransformProduct> transformAll(final Molecule source, final Molecule water) {
    if (!neutralClosed(water)) {
      return List.of();
    }
    final var products = new ArrayList<MolecularTransformProduct>();
    for (final var oxygen : water.atoms()) {
      for (final var hydrogen : water.atoms()) {
        if (!HydrogenAdditionDonor.WATER.matches(water, oxygen, hydrogen)) {
          continue;
        }
        for (final var site : sites(source)) {
          products.add(open(source, water, oxygen, hydrogen, site));
        }
      }
    }
    return List.copyOf(products);
  }

  private static List<Site> sites(final Molecule molecule) {
    if (!neutralClosed(molecule)) {
      return List.of();
    }
    final var sites = new ArrayList<Site>();
    for (final var oxygen : molecule.atoms()) {
      if (!oxygen.element().symbol().value().equals("O")) {
        continue;
      }
      final var bonds = adjacent(molecule, oxygen.id());
      if (bonds.size() != 2 || bonds.stream().anyMatch(b -> b.type() != BondType.SINGLE)) {
        continue;
      }
      final var first = atom(molecule, other(bonds.get(0), oxygen.id()));
      final var second = atom(molecule, other(bonds.get(1), oxygen.id()));
      if (!first.element().symbol().value().equals("C")
          || !second.element().symbol().value().equals("C")
          || valence(molecule, first.id()) != 4
          || valence(molecule, second.id()) != 4
          || molecule.bonds().stream()
              .noneMatch(b -> b.type() == BondType.SINGLE && b.connects(first.id(), second.id()))) {
        continue;
      }
      sites.add(new Site(oxygen.id(), first.id(), second.id()));
      sites.add(new Site(oxygen.id(), second.id(), first.id()));
    }
    return List.copyOf(sites);
  }

  private static MolecularTransformProduct open(
      final Molecule source,
      final Molecule water,
      final Atom oxygen,
      final Atom hydrogen,
      final Site site) {
    final var atoms = new ArrayList<>(source.atoms().stream().map(a -> copy(a, a.id())).toList());
    final var used = new HashSet<AtomId>();
    source.atoms().forEach(a -> used.add(a.id()));
    final var ids = new HashMap<AtomId, AtomId>();
    for (final var atom : water.atoms()) {
      String name = "water_" + water.id().value() + "_" + atom.id().value();
      while (used.contains(AtomId.of(name))) {
        name += "_";
      }
      final var id = AtomId.of(name);
      used.add(id);
      ids.put(atom.id(), id);
      atoms.add(copy(atom, id));
    }
    final var bonds =
        new ArrayList<>(
            source.bonds().stream()
                .filter(b -> !b.connects(site.oxygen(), site.attacked()))
                .toList());
    water.bonds().stream()
        .filter(b -> !b.connects(oxygen.id(), hydrogen.id()))
        .forEach(
            b -> bonds.add(Bond.of(ids.get(b.firstAtomId()), ids.get(b.secondAtomId()), b.type())));
    bonds.add(Bond.of(site.attacked(), ids.get(oxygen.id()), BondType.SINGLE));
    bonds.add(Bond.of(site.oxygen(), ids.get(hydrogen.id()), BondType.SINGLE));
    final var changed = Set.of(site.oxygen(), site.attacked(), site.retained());
    final var stereo =
        Stereochemistry.of(
            source.stereochemistry().centers().stream()
                .filter(
                    c ->
                        !changed.contains(c.primaryAtomId())
                            && (c.secondaryAtomId() == null
                                || !changed.contains(c.secondaryAtomId())))
                .toList());
    final var attributes = new HashMap<>(source.metadata().attributes());
    attributes.keySet().removeIf(key -> key.startsWith("laboratory.geometry."));
    attributes.remove("formula");
    attributes.remove("library_note");
    attributes.put(
        Stereochemistry.REVIEW_ATTRIBUTE,
        "Раскрыто эпоксидное кольцо. Проверьте конфигурации и приоритеты; стереоселективность и новые координаты не рассчитаны.");
    attributes.put("reaction.donor", water.id().value());
    attributes.put("reaction.donor.oxygen", oxygen.id().value());
    attributes.put("reaction.opened-bond", site.oxygen().value() + "-" + site.attacked().value());
    final var product =
        Molecule.of(
            MoleculeId.of(
                source.id().value()
                    + "_hydrolysis_"
                    + site.oxygen().value()
                    + "_"
                    + site.attacked().value()
                    + "_from_"
                    + water.id().value()
                    + "_"
                    + hydrogen.id().value()),
            source.displayName() + " · vicinal diol",
            atoms,
            bonds,
            source.charge(),
            source.spinMultiplicity(),
            stereo,
            ChemistryMetadata.of(
                source.metadata().source(), source.metadata().location(), attributes));
    return MolecularTransformProduct.ofWithPartner(product, site.attacked(), site.oxygen());
  }

  private static boolean neutralClosed(final Molecule molecule) {
    return molecule.charge().value() == 0
        && molecule.spinMultiplicity().value() == 1
        && molecule.graph().connected()
        && molecule.atoms().stream()
            .allMatch(
                a ->
                    a.formalCharge().value() == 0
                        && a.radicalState().equals(RadicalState.CLOSED_SHELL));
  }

  private static Atom copy(final Atom atom, final AtomId id) {
    return Atom.of(
        id,
        atom.element().symbol(),
        null,
        atom.formalCharge(),
        atom.isotope(),
        atom.radicalState(),
        atom.metadata());
  }

  private static Atom atom(final Molecule molecule, final AtomId id) {
    return molecule.atoms().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
  }

  private static AtomId other(final Bond bond, final AtomId id) {
    return bond.firstAtomId().equals(id) ? bond.secondAtomId() : bond.firstAtomId();
  }

  private static List<Bond> adjacent(final Molecule molecule, final AtomId id) {
    return molecule.bonds().stream()
        .filter(b -> b.firstAtomId().equals(id) || b.secondAtomId().equals(id))
        .toList();
  }

  private static int valence(final Molecule molecule, final AtomId id) {
    return adjacent(molecule, id).stream()
        .mapToInt(
            b ->
                switch (b.type()) {
                  case SINGLE -> 1;
                  case DOUBLE -> 2;
                  case TRIPLE -> 3;
                  default -> 100;
                })
        .sum();
  }
}
