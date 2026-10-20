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
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Atom-mapped net model: one alkene + BH3 + 3 H2O2 -> alcohol + B(OH)3 + 2 H2O.
 * The neutral products represent a workup hypothesis, not a simulation of the mechanism or solution speciation. */
public final class HydroborationOxidationTransformer {
  public record Product(ReactionSide products, String mapping) {}

  private record Site(AtomId oxygenated, AtomId hydrogenated) {}

  public boolean acceptsSubstrate(final Molecule source) {
    return !sites(source).isEmpty();
  }

  public boolean acceptsBorane(final Molecule molecule) {
    if (!neutralClosed(molecule) || molecule.atomCount() != 4 || molecule.bondCount() != 3) {
      return false;
    }
    final var boron = molecule.atoms().stream().filter(a -> symbol(a).equals("B")).toList();
    return boron.size() == 1
        && molecule.atoms().stream().filter(a -> symbol(a).equals("H")).count() == 3
        && molecule.bonds().stream()
            .allMatch(b -> b.type() == BondType.SINGLE && touches(b, boron.getFirst().id()))
        && molecule.atoms().stream()
            .filter(a -> symbol(a).equals("H"))
            .allMatch(a -> valence(molecule, a.id()) == 1);
  }

  public boolean acceptsPeroxide(final Molecule molecule) {
    return neutralClosed(molecule)
        && molecule.atomCount() == 4
        && molecule.bondCount() == 3
        && molecule.atoms().stream().filter(a -> symbol(a).equals("O")).count() == 2
        && molecule.atoms().stream().filter(a -> symbol(a).equals("H")).count() == 2
        && molecule.bonds().stream().allMatch(b -> b.type() == BondType.SINGLE)
        && molecule.atoms().stream()
            .allMatch(a -> valence(molecule, a.id()) == (symbol(a).equals("O") ? 2 : 1));
  }

  public List<Product> transformAll(
      final Molecule source, final Molecule borane, final Molecule peroxide) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(borane);
    Objects.requireNonNull(peroxide);
    if (!this.acceptsBorane(borane) || !this.acceptsPeroxide(peroxide)) {
      return List.of();
    }
    final var result = new ArrayList<Product>();
    final var hydrogens = borane.atoms().stream().filter(a -> symbol(a).equals("H")).toList();
    for (final var site : sites(source)) {
      for (final var hydrogen : hydrogens) {
        // Each explicit peroxide equivalent can contribute either end; isotope labels are never
        // erased.
        for (int orientation = 0; orientation < 8; orientation++) {
          result.add(transform(source, borane, peroxide, site, hydrogen, orientation));
        }
      }
    }
    return List.copyOf(result);
  }

  private static Product transform(
      final Molecule source,
      final Molecule borane,
      final Molecule peroxide,
      final Site site,
      final Atom hydrogen,
      final int orientation) {
    final var atoms = new ArrayList<Atom>();
    final var used = new HashSet<AtomId>();
    for (final var atom : source.atoms()) {
      atoms.add(copy(atom, atom.id()));
      used.add(atom.id());
    }
    final var boraneIds = append(atoms, used, borane, "borane_");
    final Atom boron =
        borane.atoms().stream().filter(a -> symbol(a).equals("B")).findFirst().orElseThrow();
    final var bonds = new ArrayList<Bond>();
    for (final var bond : source.bonds()) {
      bonds.add(
          bond.connects(site.oxygenated(), site.hydrogenated())
              ? Bond.of(bond.firstAtomId(), bond.secondAtomId(), BondType.SINGLE)
              : bond);
    }
    bonds.add(Bond.of(site.hydrogenated(), boraneIds.get(hydrogen.id()), BondType.SINGLE));
    final var residualHydrogens =
        borane.atoms().stream()
            .filter(a -> symbol(a).equals("H") && !a.id().equals(hydrogen.id()))
            .toList();
    final var oxygens = peroxide.atoms().stream().filter(a -> symbol(a).equals("O")).toList();
    final var alcoholAtoms = new HashSet<>(source.atoms().stream().map(Atom::id).toList());
    alcoholAtoms.add(boraneIds.get(hydrogen.id()));
    final var boricAtoms = new HashSet<AtomId>();
    boricAtoms.add(boraneIds.get(boron.id()));
    final var waters = new ArrayList<Set<AtomId>>();
    for (int equivalent = 0; equivalent < 3; equivalent++) {
      final var ids = append(atoms, used, peroxide, "peroxide" + equivalent + "_");
      final Atom transferred = oxygens.get((orientation >> equivalent) & 1);
      final Atom retained = oxygens.get(1 - ((orientation >> equivalent) & 1));
      final AtomId transferredH = attachedHydrogen(peroxide, transferred.id());
      final AtomId retainedH = attachedHydrogen(peroxide, retained.id());
      for (final var bond : peroxide.bonds()) {
        if (!bond.connects(transferred.id(), retained.id())) {
          bonds.add(
              Bond.of(ids.get(bond.firstAtomId()), ids.get(bond.secondAtomId()), bond.type()));
        }
      }
      bonds.add(Bond.of(boraneIds.get(boron.id()), ids.get(retained.id()), BondType.SINGLE));
      boricAtoms.add(ids.get(retained.id()));
      boricAtoms.add(ids.get(retainedH));
      if (equivalent == 0) {
        bonds.add(Bond.of(site.oxygenated(), ids.get(transferred.id()), BondType.SINGLE));
        alcoholAtoms.add(ids.get(transferred.id()));
        alcoholAtoms.add(ids.get(transferredH));
      } else {
        final AtomId residual = boraneIds.get(residualHydrogens.get(equivalent - 1).id());
        bonds.add(Bond.of(residual, ids.get(transferred.id()), BondType.SINGLE));
        waters.add(Set.of(residual, ids.get(transferred.id()), ids.get(transferredH)));
      }
    }
    final String suffix =
        "_hydroboration_"
            + site.oxygenated().value()
            + "_"
            + site.hydrogenated().value()
            + "_"
            + borane.id().value()
            + "_"
            + hydrogen.id().value()
            + "_"
            + peroxide.id().value()
            + "_"
            + orientation;
    final var attributes = new HashMap<>(source.metadata().attributes());
    attributes.keySet().removeIf(key -> key.startsWith("laboratory.geometry."));
    attributes.remove("formula");
    attributes.remove("library_note");
    attributes.put(
        Stereochemistry.REVIEW_ATTRIBUTE,
        "Гипотеза гидроборирования–окисления. Проверьте конфигурации и приоритеты; syn-стереоселективность и новые центры не рассчитаны.");
    attributes.put("reaction.borane", borane.id().value());
    attributes.put("reaction.peroxide", peroxide.id().value());
    attributes.put("reaction.peroxide.equivalents", "3");
    attributes.put("reaction.borane.hydrogen", hydrogen.id().value());
    attributes.put("reaction.peroxide.orientation", Integer.toString(orientation));
    final var stereo =
        Stereochemistry.of(
            source.stereochemistry().centers().stream()
                .filter(
                    c ->
                        !c.primaryAtomId().equals(site.oxygenated())
                            && !c.primaryAtomId().equals(site.hydrogenated())
                            && (c.secondaryAtomId() == null
                                || !c.secondaryAtomId().equals(site.oxygenated())
                                    && !c.secondaryAtomId().equals(site.hydrogenated())))
                .toList());
    final var metadata =
        ChemistryMetadata.of(source.metadata().source(), source.metadata().location(), attributes);
    final var products = new ArrayList<ReactionParticipant>();
    products.add(
        part(
            source.id().value() + suffix,
            source.displayName() + " · alcohol hypothesis",
            atoms,
            bonds,
            alcoholAtoms,
            stereo,
            metadata));
    products.add(
        part(
            source.id().value() + suffix + "_boric",
            "B(OH)3 · neutral workup",
            atoms,
            bonds,
            boricAtoms,
            Stereochemistry.EMPTY,
            metadata));
    for (int index = 0; index < waters.size(); index++) {
      products.add(
          part(
              source.id().value() + suffix + "_water" + index,
              "H2O · workup coproduct",
              atoms,
              bonds,
              waters.get(index),
              Stereochemistry.EMPTY,
              metadata));
    }
    return new Product(
        ReactionSide.of(products),
        "C-O at "
            + site.oxygenated().value()
            + "; C-H at "
            + site.hydrogenated().value()
            + "; BH3 hydrogen "
            + hydrogen.id().value()
            + "; peroxide end mapping "
            + orientation);
  }

  private static ReactionParticipant part(
      final String id,
      final String name,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final Set<AtomId> included,
      final Stereochemistry stereo,
      final ChemistryMetadata metadata) {
    return ReactionParticipant.of(
        Molecule.of(
            MoleculeId.of(id),
            name,
            atoms.stream().filter(a -> included.contains(a.id())).toList(),
            bonds.stream()
                .filter(
                    b -> included.contains(b.firstAtomId()) && included.contains(b.secondAtomId()))
                .toList(),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET,
            stereo,
            metadata),
        StoichiometricCoefficient.ONE);
  }

  private static Map<AtomId, AtomId> append(
      final List<Atom> atoms, final Set<AtomId> used, final Molecule source, final String prefix) {
    final var ids = new HashMap<AtomId, AtomId>();
    for (final var atom : source.atoms()) {
      String value = prefix + source.id().value() + "_" + atom.id().value();
      while (used.contains(AtomId.of(value))) {
        value += "_";
      }
      final var id = AtomId.of(value);
      used.add(id);
      ids.put(atom.id(), id);
      atoms.add(copy(atom, id));
    }
    return ids;
  }

  private static List<Site> sites(final Molecule source) {
    if (!neutralClosed(source)) {
      return List.of();
    }
    final var result = new ArrayList<Site>();
    for (final var bond : source.bonds()) {
      if (bond.type() != BondType.DOUBLE || source.aromaticity().isAromaticBond(bond)) {
        continue;
      }
      final var first = atom(source, bond.firstAtomId());
      final var second = atom(source, bond.secondAtomId());
      if (symbol(first).equals("C")
          && symbol(second).equals("C")
          && valence(source, first.id()) == 4
          && valence(source, second.id()) == 4) {
        result.add(new Site(first.id(), second.id()));
        result.add(new Site(second.id(), first.id()));
      }
    }
    return List.copyOf(result);
  }

  private static AtomId attachedHydrogen(final Molecule molecule, final AtomId oxygen) {
    return molecule.atoms().stream()
        .filter(
            a ->
                symbol(a).equals("H")
                    && molecule.bonds().stream().anyMatch(b -> b.connects(a.id(), oxygen)))
        .findFirst()
        .orElseThrow()
        .id();
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

  private static String symbol(final Atom atom) {
    return atom.element().symbol().value();
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

  private static boolean touches(final Bond bond, final AtomId id) {
    return bond.firstAtomId().equals(id) || bond.secondAtomId().equals(id);
  }

  private static int valence(final Molecule molecule, final AtomId id) {
    return molecule.bonds().stream()
        .filter(b -> touches(b, id))
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
