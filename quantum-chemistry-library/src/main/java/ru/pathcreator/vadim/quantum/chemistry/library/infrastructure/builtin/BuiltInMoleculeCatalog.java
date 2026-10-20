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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

/**
 * Встроенные молекулы с явной структурой, координатами и metadata для библиотеки.
 */
final class BuiltInMoleculeCatalog {

  private BuiltInMoleculeCatalog() {}

  static Molecule hydrogenRadical() {
    final AtomId h = AtomId.of("h");
    return Molecule.of(
        MoleculeId.of("hydrogen_radical"),
        "Hydrogen radical",
        List.of(
            Atom.of(
                h,
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0000, 0.0000, 0.0000, LengthUnit.ANGSTROM),
                FormalCharge.NEUTRAL,
                null,
                RadicalState.of(1),
                ChemistryMetadata.of(
                    null,
                    null,
                    Map.of(
                        "formula",
                        "H.",
                        "library_note",
                        "Atomic hydrogen radical for recombination and mechanism probes.")))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2),
        ChemistryMetadata.of(
            null,
            null,
            Map.of(
                "formula",
                "H.",
                "library_note",
                "Atomic hydrogen radical for recombination and mechanism probes.")));
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
        List.of(Bond.of(o, h1, BondType.SINGLE), Bond.of(o, h2, BondType.SINGLE)),
        "H2O",
        "Small polar solvent and benchmark molecule.");
  }

  static Molecule peraceticAcid() {
    final String[] elements = {"C", "C", "O", "O", "O", "H", "H", "H", "H"};
    final var atoms = new java.util.ArrayList<Atom>();
    for (int i = 0; i < elements.length; i++) {
      atoms.add(
          Atom.of(
              AtomId.of("a" + i),
              ElementSymbol.of(elements[i]),
              null,
              FormalCharge.NEUTRAL,
              null,
              RadicalState.CLOSED_SHELL,
              ChemistryMetadata.EMPTY));
    }
    final int[][] edges = {{0, 1}, {1, 2, 2}, {1, 3}, {3, 4}, {0, 5}, {0, 6}, {0, 7}, {4, 8}};
    final var bonds = new java.util.ArrayList<Bond>();
    for (final var edge : edges) {
      bonds.add(
          Bond.of(
              atoms.get(edge[0]).id(),
              atoms.get(edge[1]).id(),
              edge.length == 3 ? BondType.DOUBLE : BondType.SINGLE));
    }
    return Molecule.of(
        MoleculeId.of("peracetic_acid"),
        "Peracetic acid",
        atoms,
        bonds,
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        ChemistryMetadata.of(
            null,
            null,
            Map.of(
                "formula",
                "C2H4O3",
                "library_note",
                "Explicit CH3-C(=O)-O-O-H graph for peracid oxygen-transfer hypotheses. No 3D geometry assigned.")));
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
        List.of(Bond.of(o1, c, BondType.DOUBLE), Bond.of(c, o2, BondType.DOUBLE)),
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

  static Molecule dimethylEther() {
    final AtomId c1 = AtomId.of("c1");
    final AtomId o = AtomId.of("o");
    final AtomId c2 = AtomId.of("c2");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    final AtomId h3 = AtomId.of("h3");
    final AtomId h4 = AtomId.of("h4");
    final AtomId h5 = AtomId.of("h5");
    final AtomId h6 = AtomId.of("h6");
    return BuiltInMoleculeCatalog.molecule(
        "dimethyl_ether",
        "Dimethyl ether",
        List.of(
            BuiltInMoleculeCatalog.atom(c1, "C", -1.1800, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(o, "O", 0.0000, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(c2, "C", 1.1800, 0.0000, 0.0000),
            BuiltInMoleculeCatalog.atom(h1, "H", -1.5300, 0.9800, 0.0000),
            BuiltInMoleculeCatalog.atom(h2, "H", -1.5300, -0.4900, 0.8500),
            BuiltInMoleculeCatalog.atom(h3, "H", -1.5300, -0.4900, -0.8500),
            BuiltInMoleculeCatalog.atom(h4, "H", 1.5300, 0.9800, 0.0000),
            BuiltInMoleculeCatalog.atom(h5, "H", 1.5300, -0.4900, 0.8500),
            BuiltInMoleculeCatalog.atom(h6, "H", 1.5300, -0.4900, -0.8500)),
        List.of(
            Bond.of(c1, o, BondType.SINGLE),
            Bond.of(o, c2, BondType.SINGLE),
            Bond.of(c1, h1, BondType.SINGLE),
            Bond.of(c1, h2, BondType.SINGLE),
            Bond.of(c1, h3, BondType.SINGLE),
            Bond.of(c2, h4, BondType.SINGLE),
            Bond.of(c2, h5, BondType.SINGLE),
            Bond.of(c2, h6, BondType.SINGLE)),
        "C2H6O",
        "Ether isomer of ethanol for balanced product-channel and isomerization examples.");
  }

  static Molecule twoChloroFourSixDimethylThreePyridinecarbonitrile() {
    final AtomId n1 = AtomId.of("n1");
    final AtomId c2 = AtomId.of("c2");
    final AtomId c3 = AtomId.of("c3");
    final AtomId c4 = AtomId.of("c4");
    final AtomId c5 = AtomId.of("c5");
    final AtomId c6 = AtomId.of("c6");
    final AtomId cl2 = AtomId.of("cl2");
    final AtomId cn3 = AtomId.of("cn3");
    final AtomId nn3 = AtomId.of("nn3");
    final AtomId m4 = AtomId.of("m4");
    final AtomId m6 = AtomId.of("m6");
    final AtomId h5 = AtomId.of("h5");
    final AtomId h4a = AtomId.of("h4a");
    final AtomId h4b = AtomId.of("h4b");
    final AtomId h4c = AtomId.of("h4c");
    final AtomId h6a = AtomId.of("h6a");
    final AtomId h6b = AtomId.of("h6b");
    final AtomId h6c = AtomId.of("h6c");
    return BuiltInMoleculeCatalog.molecule(
        "two_chloro_four_six_dimethyl_three_pyridinecarbonitrile",
        "2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile",
        List.of(
            BuiltInMoleculeCatalog.atom(n1, "N", 1.40, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(c2, "C", 0.70, 1.21, 0.0),
            BuiltInMoleculeCatalog.atom(c3, "C", -0.70, 1.21, 0.0),
            BuiltInMoleculeCatalog.atom(c4, "C", -1.40, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(c5, "C", -0.70, -1.21, 0.0),
            BuiltInMoleculeCatalog.atom(c6, "C", 0.70, -1.21, 0.0),
            BuiltInMoleculeCatalog.atom(cl2, "Cl", 1.55, 2.67, 0.0),
            BuiltInMoleculeCatalog.atom(cn3, "C", -1.55, 2.67, 0.0),
            BuiltInMoleculeCatalog.atom(nn3, "N", -2.18, 3.76, 0.0),
            BuiltInMoleculeCatalog.atom(m4, "C", -2.90, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(m6, "C", 1.45, -2.51, 0.0),
            BuiltInMoleculeCatalog.atom(h5, "H", -1.24, -2.15, 0.0),
            BuiltInMoleculeCatalog.atom(h4a, "H", -3.26, 0.00, 1.02),
            BuiltInMoleculeCatalog.atom(h4b, "H", -3.26, 0.88, -0.51),
            BuiltInMoleculeCatalog.atom(h4c, "H", -3.26, -0.88, -0.51),
            BuiltInMoleculeCatalog.atom(h6a, "H", 1.81, -2.51, 1.02),
            BuiltInMoleculeCatalog.atom(h6b, "H", 2.06, -3.27, -0.51),
            BuiltInMoleculeCatalog.atom(h6c, "H", 0.82, -3.27, -0.51)),
        List.of(
            BuiltInMoleculeCatalog.ringBond(n1, c2),
            BuiltInMoleculeCatalog.ringBond(c2, c3),
            BuiltInMoleculeCatalog.ringBond(c3, c4),
            BuiltInMoleculeCatalog.ringBond(c4, c5),
            BuiltInMoleculeCatalog.ringBond(c5, c6),
            BuiltInMoleculeCatalog.ringBond(c6, n1),
            Bond.of(c2, cl2, BondType.SINGLE),
            Bond.of(c3, cn3, BondType.SINGLE),
            Bond.of(cn3, nn3, BondType.TRIPLE),
            Bond.of(c4, m4, BondType.SINGLE),
            Bond.of(c6, m6, BondType.SINGLE),
            Bond.of(c5, h5, BondType.SINGLE),
            Bond.of(m4, h4a, BondType.SINGLE),
            Bond.of(m4, h4b, BondType.SINGLE),
            Bond.of(m4, h4c, BondType.SINGLE),
            Bond.of(m6, h6a, BondType.SINGLE),
            Bond.of(m6, h6b, BondType.SINGLE),
            Bond.of(m6, h6c, BondType.SINGLE)),
        "C8H7ClN2",
        "Activated chloro-nicotinonitrile heteroaryl substrate for SNAr and ANRORC prediction.");
  }

  static Molecule benzylamine() {
    final AtomId p1 = AtomId.of("p1");
    final AtomId p2 = AtomId.of("p2");
    final AtomId p3 = AtomId.of("p3");
    final AtomId p4 = AtomId.of("p4");
    final AtomId p5 = AtomId.of("p5");
    final AtomId p6 = AtomId.of("p6");
    final AtomId ch2 = AtomId.of("ch2");
    final AtomId n = AtomId.of("n");
    final AtomId hp2 = AtomId.of("hp2");
    final AtomId hp3 = AtomId.of("hp3");
    final AtomId hp4 = AtomId.of("hp4");
    final AtomId hp5 = AtomId.of("hp5");
    final AtomId hp6 = AtomId.of("hp6");
    final AtomId hch2a = AtomId.of("hch2a");
    final AtomId hch2b = AtomId.of("hch2b");
    final AtomId hn1 = AtomId.of("hn1");
    final AtomId hn2 = AtomId.of("hn2");
    return BuiltInMoleculeCatalog.molecule(
        "benzylamine",
        "Benzylamine",
        List.of(
            BuiltInMoleculeCatalog.atom(p1, "C", 1.40, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(p2, "C", 0.70, 1.21, 0.0),
            BuiltInMoleculeCatalog.atom(p3, "C", -0.70, 1.21, 0.0),
            BuiltInMoleculeCatalog.atom(p4, "C", -1.40, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(p5, "C", -0.70, -1.21, 0.0),
            BuiltInMoleculeCatalog.atom(p6, "C", 0.70, -1.21, 0.0),
            BuiltInMoleculeCatalog.atom(ch2, "C", 2.90, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(n, "N", 4.20, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(hp2, "H", 1.24, 2.15, 0.0),
            BuiltInMoleculeCatalog.atom(hp3, "H", -1.24, 2.15, 0.0),
            BuiltInMoleculeCatalog.atom(hp4, "H", -2.48, 0.00, 0.0),
            BuiltInMoleculeCatalog.atom(hp5, "H", -1.24, -2.15, 0.0),
            BuiltInMoleculeCatalog.atom(hp6, "H", 1.24, -2.15, 0.0),
            BuiltInMoleculeCatalog.atom(hch2a, "H", 3.08, 0.88, 0.56),
            BuiltInMoleculeCatalog.atom(hch2b, "H", 3.08, -0.88, -0.56),
            BuiltInMoleculeCatalog.atom(hn1, "H", 4.55, 0.82, 0.45),
            BuiltInMoleculeCatalog.atom(hn2, "H", 4.55, -0.82, -0.45)),
        List.of(
            BuiltInMoleculeCatalog.ringBond(p1, p2),
            BuiltInMoleculeCatalog.ringBond(p2, p3),
            BuiltInMoleculeCatalog.ringBond(p3, p4),
            BuiltInMoleculeCatalog.ringBond(p4, p5),
            BuiltInMoleculeCatalog.ringBond(p5, p6),
            BuiltInMoleculeCatalog.ringBond(p6, p1),
            Bond.of(p1, ch2, BondType.SINGLE),
            Bond.of(ch2, n, BondType.SINGLE),
            Bond.of(p2, hp2, BondType.SINGLE),
            Bond.of(p3, hp3, BondType.SINGLE),
            Bond.of(p4, hp4, BondType.SINGLE),
            Bond.of(p5, hp5, BondType.SINGLE),
            Bond.of(p6, hp6, BondType.SINGLE),
            Bond.of(ch2, hch2a, BondType.SINGLE),
            Bond.of(ch2, hch2b, BondType.SINGLE),
            Bond.of(n, hn1, BondType.SINGLE),
            Bond.of(n, hn2, BondType.SINGLE)),
        "C7H9N",
        "Primary benzylic amine nucleophile for amination and heteroaryl substitution prediction.");
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
      final AtomId id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(id, ElementSymbol.of(symbol), Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM));
  }

  private static Bond ringBond(final AtomId first, final AtomId second) {
    return Bond.of(first, second, BondType.AROMATIC);
  }
}
