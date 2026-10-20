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
import static ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MulticomponentReactantSites.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Formal intermolecular Ugi graph with explicit water; not an isotope-exchange mechanism. */
public final class UgiTransformer {
  public boolean acceptsCarbonyl(final Molecule molecule) {
    return !carbonyls(molecule).isEmpty();
  }

  public List<MolecularTransformProduct> transformAll(
      final Molecule carbonyl,
      final Molecule acid,
      final Molecule isocyanide,
      final Molecule amine) {
    final var result = new ArrayList<MolecularTransformProduct>();
    for (final var c : carbonyls(carbonyl)) {
      for (final var a : acids(acid)) {
        for (final var i : isocyanides(isocyanide)) {
          for (final var n : primaryAmines(amine)) {
            result.add(combine(carbonyl, acid, isocyanide, amine, c, a, i, n));
          }
        }
      }
    }
    return List.copyOf(result);
  }

  private static MolecularTransformProduct combine(
      final Molecule carbonyl,
      final Molecule acid,
      final Molecule isocyanide,
      final Molecule amine,
      final Carbonyl c,
      final Acid a,
      final Isocyanide i,
      final PrimaryAmine n) {
    final var inputs = List.of(carbonyl, acid, isocyanide, amine);
    final var atoms = new ArrayList<Atom>();
    final var bonds = new ArrayList<Bond>();
    final var maps = new ArrayList<Map<AtomId, AtomId>>();
    final Set<AtomId> used = new HashSet<>();
    carbonyl.atoms().forEach(atom -> used.add(atom.id()));
    for (int index = 0; index < inputs.size(); index++) {
      final var input = inputs.get(index);
      final Map<AtomId, AtomId> map = new LinkedHashMap<>();
      for (final var atom : input.atoms()) {
        String name = index == 0 ? atom.id().value() : "ugi_" + index + "_" + atom.id().value();
        if (index != 0) {
          while (used.contains(AtomId.of(name))) {
            name += "_";
          }
        }
        final var id = AtomId.of(name);
        used.add(id);
        map.put(atom.id(), id);
        final var charge =
            index == 2 && (atom.id().equals(i.carbon()) || atom.id().equals(i.nitrogen()))
                ? FormalCharge.NEUTRAL
                : atom.formalCharge();
        atoms.add(
            Atom.of(
                id,
                atom.element().symbol(),
                null,
                charge,
                atom.isotope(),
                atom.radicalState(),
                atom.metadata()));
      }
      maps.add(map);
      for (final var bond : input.bonds()) {
        bonds.add(Bond.of(map.get(bond.firstAtomId()), map.get(bond.secondAtomId()), bond.type()));
      }
    }
    final AtomId cc = maps.get(0).get(c.carbon()), co = maps.get(0).get(c.oxygen());
    final AtomId ac = maps.get(1).get(a.carbon()),
        ao = maps.get(1).get(a.hydroxyl()),
        ah = maps.get(1).get(a.hydrogen());
    final AtomId ic = maps.get(2).get(i.carbon()), in = maps.get(2).get(i.nitrogen());
    final AtomId nn = maps.get(3).get(n.nitrogen()),
        nh1 = maps.get(3).get(n.firstHydrogen()),
        nh2 = maps.get(3).get(n.secondHydrogen());
    bonds.removeIf(
        b ->
            b.connects(cc, co)
                || b.connects(ac, ao)
                || b.connects(ao, ah)
                || b.connects(ic, in)
                || b.connects(nn, nh1)
                || b.connects(nn, nh2));
    bonds.add(Bond.of(cc, nn, BondType.SINGLE));
    bonds.add(Bond.of(cc, ic, BondType.SINGLE));
    bonds.add(Bond.of(nn, ac, BondType.SINGLE));
    bonds.add(Bond.of(ic, ao, BondType.DOUBLE));
    bonds.add(Bond.of(ic, in, BondType.SINGLE));
    bonds.add(Bond.of(in, ah, BondType.SINGLE));

    final Set<AtomId> waterAtoms = Set.of(co, nh1, nh2);
    final Set<AtomId> changed = Set.of(cc, co, ac, ao, ah, ic, in, nn, nh1, nh2);
    final var centers = new ArrayList<Stereocenter>();
    for (int index = 0; index < inputs.size(); index++) {
      final var map = maps.get(index);
      for (final var center : inputs.get(index).stereochemistry().centers()) {
        final var first = map.get(center.primaryAtomId());
        final var second =
            center.secondaryAtomId() == null ? null : map.get(center.secondaryAtomId());
        if (changed.contains(first) || second != null && changed.contains(second)) {
          continue;
        }
        centers.add(
            Stereocenter.of(
                center.kind(),
                center.descriptor(),
                first,
                second,
                center.referenceAtomIds().stream().map(map::get).toList()));
      }
    }
    final var attributes = new HashMap<>(carbonyl.metadata().attributes());
    attributes.keySet().removeIf(key -> key.startsWith("laboratory.geometry."));
    attributes.remove("formula");
    attributes.remove("library_note");
    attributes.put("reaction.carboxylic_acid", acid.id().value());
    attributes.put("reaction.isocyanide", isocyanide.id().value());
    attributes.put("reaction.primary_amine", amine.id().value());
    final String review =
        "Формальная схема Уги. Проверьте стереохимию и изотопное соответствие; новая конфигурация, обмен протонов/кислорода, геометрия и кинетика не рассчитаны.";
    attributes.put(Stereochemistry.REVIEW_ATTRIBUTE, review);
    final String id =
        carbonyl.id().value()
            + "_ugi_"
            + c.carbon().value()
            + "_"
            + acid.id().value()
            + "_"
            + a.carbon().value()
            + "_"
            + isocyanide.id().value()
            + "_"
            + i.carbon().value()
            + "_"
            + amine.id().value()
            + "_"
            + n.nitrogen().value();
    final var product =
        Molecule.of(
            MoleculeId.of(id),
            carbonyl.displayName() + " · Ugi α-acylaminoamide",
            atoms.stream().filter(atom -> !waterAtoms.contains(atom.id())).toList(),
            bonds,
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET,
            Stereochemistry.of(centers),
            ChemistryMetadata.of(
                carbonyl.metadata().source(), carbonyl.metadata().location(), attributes));
    final var water =
        Molecule.of(
            MoleculeId.of(id + "_water"),
            "Вода · сопродукт Уги",
            atoms.stream().filter(atom -> waterAtoms.contains(atom.id())).toList(),
            List.of(Bond.of(co, nh1, BondType.SINGLE), Bond.of(co, nh2, BondType.SINGLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET,
            Stereochemistry.of(List.of()),
            ChemistryMetadata.of(null, null, Map.of(Stereochemistry.REVIEW_ATTRIBUTE, review)));
    return MolecularTransformProduct.of(product, List.of(water), cc, null);
  }
}
