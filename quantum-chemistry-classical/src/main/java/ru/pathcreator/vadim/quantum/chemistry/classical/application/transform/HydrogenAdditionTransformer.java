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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondAdditionTransform;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Присоединяет обе части явного H-X реагента, сохраняя группу X, изотопы и атомный состав. */
public final class HydrogenAdditionTransformer {
  public List<MolecularTransformProduct> transformAll(
      final Molecule source,
      final BondAdditionTransform transform,
      final Molecule donor,
      final HydrogenAdditionDonor kind) {
    java.util.Objects.requireNonNull(kind);
    if (!connected(donor)) {
      return List.of();
    }
    final var products = new ArrayList<MolecularTransformProduct>();
    for (final var bond : donor.bonds()) {
      final Atom first = atom(donor, bond.firstAtomId());
      final Atom second = atom(donor, bond.secondAtomId());
      if (kind.matches(donor, first, second)) {
        add(products, source, transform, donor, first, second);
      }
      if (kind.matches(donor, second, first)
          && (!kind.matches(donor, first, second)
              || !java.util.Objects.equals(first.isotope(), second.isotope()))) {
        add(products, source, transform, donor, second, first);
      }
    }
    return List.copyOf(products);
  }

  private static void add(
      final List<MolecularTransformProduct> products,
      final Molecule source,
      final BondAdditionTransform transform,
      final Molecule donor,
      final Atom group,
      final Atom hydrogen) {
    final var sourceIds = new HashSet<AtomId>();
    source.atoms().forEach(atom -> sourceIds.add(atom.id()));
    for (final var skeleton : new BondAdditionTransformer().transformAll(source, transform)) {
      final var atoms =
          new ArrayList<>(
              source.atoms().stream()
                  .map(
                      atom ->
                          Atom.of(
                              atom.id(),
                              atom.element().symbol(),
                              null,
                              atom.formalCharge(),
                              atom.isotope(),
                              atom.radicalState(),
                              atom.metadata()))
                  .toList());
      final var bonds = new ArrayList<Bond>();
      skeleton.product().bonds().stream()
          .filter(
              bond ->
                  sourceIds.contains(bond.firstAtomId()) && sourceIds.contains(bond.secondAtomId()))
          .forEach(bonds::add);
      final var ids = new HashMap<AtomId, AtomId>();
      final var used = new HashSet<>(sourceIds);
      for (final var atom : donor.atoms()) {
        String id = "donor_" + donor.id().value() + "_" + atom.id().value();
        while (used.contains(AtomId.of(id))) {
          id += "_";
        }
        final var mapped = AtomId.of(id);
        used.add(mapped);
        ids.put(atom.id(), mapped);
        atoms.add(
            Atom.of(
                mapped,
                atom.element().symbol(),
                null,
                atom.formalCharge(),
                atom.isotope(),
                atom.radicalState(),
                atom.metadata()));
      }
      for (final var bond : donor.bonds()) {
        if (!bond.connects(group.id(), hydrogen.id())) {
          bonds.add(
              Bond.of(ids.get(bond.firstAtomId()), ids.get(bond.secondAtomId()), bond.type()));
        }
      }
      bonds.add(
          Bond.of(skeleton.anchorAtomId(), ids.get(group.id()), transform.firstAdditionBondType()));
      bonds.add(
          Bond.of(
              skeleton.partnerAtomId(),
              ids.get(hydrogen.id()),
              transform.secondAdditionBondType()));
      final var centers = new ArrayList<Stereocenter>();
      source.stereochemistry().centers().stream()
          .filter(
              center ->
                  center.secondaryAtomId() == null
                      || !Bond.of(
                              skeleton.anchorAtomId(), skeleton.partnerAtomId(), BondType.SINGLE)
                          .connects(center.primaryAtomId(), center.secondaryAtomId()))
          .forEach(centers::add);
      for (final var center : donor.stereochemistry().centers()) {
        centers.add(
            Stereocenter.of(
                center.kind(),
                center.descriptor(),
                ids.get(center.primaryAtomId()),
                center.secondaryAtomId() == null ? null : ids.get(center.secondaryAtomId()),
                center.referenceAtomIds().stream().map(ids::get).toList()));
      }
      final var attributes = new HashMap<>(source.metadata().attributes());
      attributes.keySet().removeIf(key -> key.startsWith("laboratory.geometry."));
      attributes.remove("formula");
      attributes.remove("library_note");
      attributes.put(
          Stereochemistry.REVIEW_ATTRIBUTE,
          "После присоединения проверьте конфигурации и приоритеты заместителей. Стереохимия новых центров автоматически не назначалась.");
      attributes.put("reaction.donor", donor.id().value());
      attributes.put(
          "reaction.donor.broken-bond", group.id().value() + "-" + hydrogen.id().value());
      final var molecule =
          Molecule.of(
              MoleculeId.of(
                  skeleton.product().id().value()
                      + "_from_"
                      + donor.id().value()
                      + "_"
                      + hydrogen.id().value()),
              skeleton.product().displayName(),
              atoms,
              bonds,
              source.charge(),
              source.spinMultiplicity(),
              Stereochemistry.of(centers),
              ChemistryMetadata.of(
                  source.metadata().source(), source.metadata().location(), attributes));
      products.add(
          MolecularTransformProduct.ofWithPartner(
              molecule, skeleton.anchorAtomId(), skeleton.partnerAtomId()));
    }
  }

  private static Atom atom(final Molecule molecule, final AtomId id) {
    return molecule.atoms().stream().filter(atom -> atom.id().equals(id)).findFirst().orElseThrow();
  }

  private static boolean connected(final Molecule molecule) {
    if (molecule.atoms().isEmpty()) {
      return false;
    }
    final var reached = new HashSet<AtomId>();
    reached.add(molecule.atoms().getFirst().id());
    boolean changed;
    do {
      changed = false;
      for (final var bond : molecule.bonds()) {
        if (reached.contains(bond.firstAtomId())) {
          changed |= reached.add(bond.secondAtomId());
        }
        if (reached.contains(bond.secondAtomId())) {
          changed |= reached.add(bond.firstAtomId());
        }
      }
    } while (changed);
    return reached.size() == molecule.atomCount();
  }
}
