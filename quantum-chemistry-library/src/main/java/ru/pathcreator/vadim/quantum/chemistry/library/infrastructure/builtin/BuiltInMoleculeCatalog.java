/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.builtin;

import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/**
 * Встроенные молекулы с явной структурой, координатами и metadata для библиотеки.
 */
final class BuiltInMoleculeCatalog {

  private BuiltInMoleculeCatalog() {
  }

  static Molecule water() {
    final AtomId o = AtomId.of("o");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    return BuiltInMoleculeCatalog.molecule(
        "water",
        "Water",
        List.of(
            BuiltInMoleculeCatalog.atom(o, "O", 0.0000, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(h1, "H", 0.9572, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(h2, "H", -0.2390, 0.9270, 0.0000)),
        List.of(
            Bond.of(o, h1, BondType.SINGLE),
            Bond.of(o, h2, BondType.SINGLE)),
        "H2O",
        "Small polar solvent and benchmark molecule.");
  }

  static Molecule methane() {
    final AtomId c = AtomId.of("c");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    final AtomId h3 = AtomId.of("h3");
    final AtomId h4 = AtomId.of("h4");
    return BuiltInMoleculeCatalog.molecule(
        "methane",
        "Methane",
        List.of(
            BuiltInMoleculeCatalog.atom(c, "C", 0.0000, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(h1, "H", 0.6291, 0.6291, 0.6291),
            BuiltInMoleculeCatalog.atom(h2, "H", -0.6291, -0.6291, 0.6291),
            BuiltInMoleculeCatalog.atom(h3, "H", -0.6291, 0.6291, -0.6291),
            BuiltInMoleculeCatalog.atom(h4, "H", 0.6291, -0.6291, -0.6291)),
        List.of(
            Bond.of(c, h1, BondType.SINGLE),
            Bond.of(c, h2, BondType.SINGLE),
            Bond.of(c, h3, BondType.SINGLE),
            Bond.of(c, h4, BondType.SINGLE)),
        "CH4",
        "Tetrahedral hydrocarbon used for geometry and combustion examples.");
  }

  static Molecule ammonia() {
    final AtomId n = AtomId.of("n");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    final AtomId h3 = AtomId.of("h3");
    return BuiltInMoleculeCatalog.molecule(
        "ammonia",
        "Ammonia",
        List.of(
            BuiltInMoleculeCatalog.atom(n, "N", 0.0000, 0.0000, 0.1200),
            BuiltInMoleculeCatalog.atom(h1, "H", 0.9400, 0.0000, -0.2800),
            BuiltInMoleculeCatalog.atom(h2, "H", -0.4700, 0.8140, -0.2800),
            BuiltInMoleculeCatalog.atom(h3, "H", -0.4700, -0.8140, -0.2800)),
        List.of(
            Bond.of(n, h1, BondType.SINGLE),
            Bond.of(n, h2, BondType.SINGLE),
            Bond.of(n, h3, BondType.SINGLE)),
        "H3N",
        "Trigonal-pyramidal base and simple nitrogen-containing molecule.");
  }

  static Molecule carbonDioxide() {
    final AtomId c = AtomId.of("c");
    final AtomId o1 = AtomId.of("o1");
    final AtomId o2 = AtomId.of("o2");
    return BuiltInMoleculeCatalog.molecule(
        "carbon_dioxide",
        "Carbon dioxide",
        List.of(
            BuiltInMoleculeCatalog.atom(o1, "O", -1.1600, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(c, "C", 0.0000, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(o2, "O", 1.1600, 0.0000, 0.0000)),
        List.of(
            Bond.of(o1, c, BondType.DOUBLE),
            Bond.of(c, o2, BondType.DOUBLE)),
        "CO2",
        "Linear molecule for bond-order, symmetry and reaction examples.");
  }

  static Molecule oxygen() {
    final AtomId o1 = AtomId.of("o1");
    final AtomId o2 = AtomId.of("o2");
    return Molecule.of(
        MoleculeId.of("oxygen"),
        "Oxygen",
        List.of(
            BuiltInMoleculeCatalog.atom(o1, "O", -0.6050, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(o2, "O", 0.6050, 0.0000, 0.0000)),
        List.of(Bond.of(o1, o2, BondType.DOUBLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(3),
        ChemistryMetadata.of(
            null,
            null,
            Map.of(
                "formula",
                "O2",
                "library_note",
                "Triplet oxygen reference for combustion-style reactions.")));
  }

  static Molecule acetone() {
    final AtomId c1 = AtomId.of("c1");
    final AtomId c2 = AtomId.of("c2");
    final AtomId c3 = AtomId.of("c3");
    final AtomId o1 = AtomId.of("o1");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    final AtomId h3 = AtomId.of("h3");
    final AtomId h4 = AtomId.of("h4");
    final AtomId h5 = AtomId.of("h5");
    final AtomId h6 = AtomId.of("h6");
    return BuiltInMoleculeCatalog.molecule(
        "acetone",
        "Acetone",
        List.of(
            BuiltInMoleculeCatalog.atom(c1, "C", -1.4500, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(c2, "C", 0.0000, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(c3, "C", 1.4500, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(o1, "O", 0.0000, 1.2200, 0.0000),
            BuiltInMoleculeCatalog.atom(h1, "H", -1.8200, 0.5500, 0.8900),
            BuiltInMoleculeCatalog.atom(h2, "H", -1.8200, 0.5500, -0.8900),
            BuiltInMoleculeCatalog.atom(h3, "H", -1.8200, -1.0500, 0.0000),
            BuiltInMoleculeCatalog.atom(h4, "H", 1.8200, 0.5500, 0.8900),
            BuiltInMoleculeCatalog.atom(h5, "H", 1.8200, 0.5500, -0.8900),
            BuiltInMoleculeCatalog.atom(h6, "H", 1.8200, -1.0500, 0.0000)),
        List.of(
            Bond.of(c1, c2, BondType.SINGLE),
            Bond.of(c2, c3, BondType.SINGLE),
            Bond.of(c2, o1, BondType.DOUBLE),
            Bond.of(c1, h1, BondType.SINGLE),
            Bond.of(c1, h2, BondType.SINGLE),
            Bond.of(c1, h3, BondType.SINGLE),
            Bond.of(c3, h4, BondType.SINGLE),
            Bond.of(c3, h5, BondType.SINGLE),
            Bond.of(c3, h6, BondType.SINGLE)),
        "C3H6O",
        "Carbonyl solvent and organic chemistry benchmark molecule.");
  }

  static Molecule ethanol() {
    final AtomId c1 = AtomId.of("c1");
    final AtomId c2 = AtomId.of("c2");
    final AtomId o = AtomId.of("o");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    final AtomId h3 = AtomId.of("h3");
    final AtomId h4 = AtomId.of("h4");
    final AtomId h5 = AtomId.of("h5");
    final AtomId h6 = AtomId.of("h6");
    return BuiltInMoleculeCatalog.molecule(
        "ethanol",
        "Ethanol",
        List.of(
            BuiltInMoleculeCatalog.atom(c1, "C", -0.7500, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(c2, "C", 0.7500, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(o, "O", 1.4300, 1.1800, 0.0000),
            BuiltInMoleculeCatalog.atom(h1, "H", -1.1200, 0.5500, 0.8900),
            BuiltInMoleculeCatalog.atom(h2, "H", -1.1200, 0.5500, -0.8900),
            BuiltInMoleculeCatalog.atom(h3, "H", -1.1200, -1.0500, 0.0000),
            BuiltInMoleculeCatalog.atom(h4, "H", 1.1200, -0.5500, 0.8900),
            BuiltInMoleculeCatalog.atom(h5, "H", 1.1200, -0.5500, -0.8900),
            BuiltInMoleculeCatalog.atom(h6, "H", 2.3300, 1.1300, 0.0000)),
        List.of(
            Bond.of(c1, c2, BondType.SINGLE),
            Bond.of(c2, o, BondType.SINGLE),
            Bond.of(c1, h1, BondType.SINGLE),
            Bond.of(c1, h2, BondType.SINGLE),
            Bond.of(c1, h3, BondType.SINGLE),
            Bond.of(c2, h4, BondType.SINGLE),
            Bond.of(c2, h5, BondType.SINGLE),
            Bond.of(o, h6, BondType.SINGLE)),
        "C2H6O",
        "Small alcohol for solvent, conformer and oxidation examples.");
  }

  private static Molecule molecule(
      final String id,
      final String name,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final String formula,
      final String note) {
    return Molecule.of(
        MoleculeId.of(id),
        name,
        atoms,
        bonds,
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.of(null, null, Map.of("formula", formula, "library_note", note)));
  }

  private static Atom atom(
      final AtomId id,
      final String symbol,
      final double x,
      final double y,
      final double z) {
    return Atom.of(
        id,
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM));
  }
}