/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.transform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Explicit H2 addition to aldehydes and ketones; acyl derivatives are separate transformations. */
public final class CarbonylHydrogenationTransformer {
  private static final BondAdditionTransform TRANSFORM =
      BondAdditionTransform.of(
          "carbonyl_hydrogenation",
          "hydrogenated alcohol",
          BondType.DOUBLE,
          BondType.SINGLE,
          ReactantAtomSelector.element("C"),
          ReactantAtomSelector.element("O"),
          ElementSymbol.of("H"),
          ElementSymbol.of("H"),
          BondType.SINGLE,
          BondType.SINGLE,
          "C=O becomes C-O; both hydrogen atoms come from the explicit H-H reagent.");

  public boolean acceptsSubstrate(final Molecule source) {
    return !sites(source).isEmpty();
  }

  public List<MolecularTransformProduct> transformAll(
      final Molecule source, final Molecule hydrogen) {
    final var sites = sites(source);
    if (sites.isEmpty()) {
      return List.of();
    }
    return new HydrogenAdditionTransformer()
        .transformAll(source, TRANSFORM, hydrogen, HydrogenAdditionDonor.HYDROGEN).stream()
            .filter(product -> sites.contains(product.anchorAtomId()))
            .toList();
  }

  private static Set<AtomId> sites(final Molecule source) {
    if (source.charge().value() != 0
        || source.spinMultiplicity().value() != 1
        || !source.graph().connected()
        || source.atoms().stream()
            .anyMatch(
                a ->
                    a.formalCharge().value() != 0
                        || !a.radicalState().equals(RadicalState.CLOSED_SHELL))) {
      return Set.of();
    }
    final var result = new HashSet<AtomId>();
    for (final var carbon : source.atoms()) {
      if (!element(carbon, "C") || source.aromaticity().isAromaticAtom(carbon.id())) {
        continue;
      }
      final var bonds = bonds(source, carbon.id());
      if (bonds.size() != 3) {
        continue;
      }
      final long carbonyls =
          bonds.stream()
              .filter(
                  b ->
                      b.type() == BondType.DOUBLE
                          && element(atom(source, other(b, carbon.id())), "O")
                          && bonds(source, other(b, carbon.id())).size() == 1)
              .count();
      final long substituents =
          bonds.stream()
              .filter(
                  b ->
                      b.type() == BondType.SINGLE
                          && (element(atom(source, other(b, carbon.id())), "C")
                              || element(atom(source, other(b, carbon.id())), "H")))
              .count();
      if (carbonyls == 1 && substituents == 2) {
        result.add(carbon.id());
      }
    }
    return Set.copyOf(result);
  }

  private static List<Bond> bonds(final Molecule molecule, final AtomId id) {
    return molecule.bonds().stream()
        .filter(b -> b.firstAtomId().equals(id) || b.secondAtomId().equals(id))
        .toList();
  }

  private static AtomId other(final Bond bond, final AtomId id) {
    return bond.firstAtomId().equals(id) ? bond.secondAtomId() : bond.firstAtomId();
  }

  private static Atom atom(final Molecule molecule, final AtomId id) {
    return molecule.atoms().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
  }

  private static boolean element(final Atom atom, final String symbol) {
    return atom.element().symbol().value().equals(symbol);
  }
}
