/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraph;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphNode;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularRing;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreGraphTest {

  ChemistryCoreGraphTest() {}

  @Test
  void graphAnalyzerBuildsNeighborAndDegreeViewForConnectedChain() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "graph.chain"),
            (String) "Graph chain",
            List.of(
                ChemistryCoreGraphTest.atom("a"),
                ChemistryCoreGraphTest.atom("b"),
                ChemistryCoreGraphTest.atom("c")),
            List.of(ChemistryCoreGraphTest.bond("a", "b"), ChemistryCoreGraphTest.bond("b", "c")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    MolecularGraph graph = MolecularGraphAnalyzer.analyze((Molecule) molecule);
    Assertions.assertTrue((boolean) graph.connected());
    Assertions.assertEquals((Object) graph, (Object) molecule.graph());
    Assertions.assertFalse((boolean) graph.cyclic());
    Assertions.assertEquals((int) 1, (int) graph.degreeOf(AtomId.of((String) "a")));
    Assertions.assertEquals((int) 2, (int) graph.degreeOf(AtomId.of((String) "b")));
    Assertions.assertEquals(
        List.of(AtomId.of((String) "a"), AtomId.of((String) "c")),
        (Object) graph.neighborsOf(AtomId.of((String) "b")));
  }

  @Test
  void graphAnalyzerFindsDisconnectedComponents() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "graph.disconnected"),
            (String) "Graph disconnected",
            List.of(
                ChemistryCoreGraphTest.atom("a"),
                ChemistryCoreGraphTest.atom("b"),
                ChemistryCoreGraphTest.atom("c"),
                ChemistryCoreGraphTest.atom("d")),
            List.of(ChemistryCoreGraphTest.bond("a", "b"), ChemistryCoreGraphTest.bond("c", "d")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    MolecularGraph graph = MolecularGraphAnalyzer.analyze((Molecule) molecule);
    Assertions.assertFalse((boolean) graph.connected());
    Assertions.assertEquals((int) 2, (int) graph.components().size());
    Assertions.assertEquals(
        List.of(AtomId.of((String) "a"), AtomId.of((String) "b")),
        (Object) ((MolecularGraphComponent) graph.components().get(0)).atomIds());
    Assertions.assertEquals(
        List.of(AtomId.of((String) "c"), AtomId.of((String) "d")),
        (Object) ((MolecularGraphComponent) graph.components().get(1)).atomIds());
  }

  @Test
  void graphAnalyzerFindsSimpleRingWithoutDirectionalDuplicates() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "graph.ring"),
            (String) "Graph ring",
            List.of(
                ChemistryCoreGraphTest.atom("a"),
                ChemistryCoreGraphTest.atom("b"),
                ChemistryCoreGraphTest.atom("c"),
                ChemistryCoreGraphTest.atom("d")),
            List.of(
                ChemistryCoreGraphTest.bond("a", "b"),
                ChemistryCoreGraphTest.bond("b", "c"),
                ChemistryCoreGraphTest.bond("c", "d"),
                ChemistryCoreGraphTest.bond("d", "a")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    MolecularGraph graph = MolecularGraphAnalyzer.analyze((Molecule) molecule);
    Assertions.assertTrue((boolean) graph.connected());
    Assertions.assertTrue((boolean) graph.cyclic());
    Assertions.assertEquals((int) 1, (int) graph.rings().size());
    Assertions.assertEquals((int) 4, (int) ((MolecularRing) graph.rings().get(0)).size());
  }

  @Test
  void graphAnalyzerHandlesRingWithBranchAndSeparateComponent() {
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "graph.ring.branch.component"),
            (String) "Graph ring with branch and component",
            List.of(
                ChemistryCoreGraphTest.atom("a"),
                ChemistryCoreGraphTest.atom("b"),
                ChemistryCoreGraphTest.atom("c"),
                ChemistryCoreGraphTest.atom("d"),
                ChemistryCoreGraphTest.atom("e"),
                ChemistryCoreGraphTest.atom("f"),
                ChemistryCoreGraphTest.atom("g")),
            List.of(
                ChemistryCoreGraphTest.bond("a", "b"),
                ChemistryCoreGraphTest.bond("b", "c"),
                ChemistryCoreGraphTest.bond("c", "d"),
                ChemistryCoreGraphTest.bond("d", "a"),
                ChemistryCoreGraphTest.bond("b", "e"),
                ChemistryCoreGraphTest.bond("f", "g")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);

    MolecularGraph graph = MolecularGraphAnalyzer.analyze((Molecule) molecule);

    Assertions.assertFalse((boolean) graph.connected());
    Assertions.assertTrue((boolean) graph.cyclic());
    Assertions.assertEquals((int) 2, (int) graph.components().size());
    Assertions.assertEquals((int) 1, (int) graph.rings().size());
    Assertions.assertEquals((int) 3, (int) graph.degreeOf(AtomId.of((String) "b")));
    Assertions.assertEquals((int) 1, (int) graph.degreeOf(AtomId.of((String) "e")));
    Assertions.assertEquals(
        List.of(AtomId.of((String) "a"), AtomId.of((String) "c"), AtomId.of((String) "e")),
        (Object) graph.neighborsOf(AtomId.of((String) "b")));
    Assertions.assertEquals(
        List.of(
            AtomId.of((String) "a"),
            AtomId.of((String) "b"),
            AtomId.of((String) "d"),
            AtomId.of((String) "c"),
            AtomId.of((String) "e")),
        (Object) ((MolecularGraphComponent) graph.components().get(0)).atomIds());
    Assertions.assertEquals(
        List.of(AtomId.of((String) "f"), AtomId.of((String) "g")),
        (Object) ((MolecularGraphComponent) graph.components().get(1)).atomIds());
  }

  @Test
  void graphResultIsImmutableAndRejectsUnknownAtomQueries() {
    final MolecularGraph graph =
        MolecularGraphAnalyzer.analyze(
            (Molecule)
                Molecule.of(
                    (MoleculeId) MoleculeId.of((String) "graph.single"),
                    (String) "Graph single",
                    List.of(ChemistryCoreGraphTest.atom("a")),
                    List.of(),
                    (MolecularCharge) MolecularCharge.NEUTRAL,
                    (SpinMultiplicity) SpinMultiplicity.SINGLET));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> ((MolecularGraphComponent) graph.components().get(0)).atomIds().clear());
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> graph.degreeOf(AtomId.of((String) "missing")));
  }

  @Test
  void molecularGraphNodeCopiesNeighborsAndRejectsBrokenNeighborLists() {
    final ArrayList<AtomId> neighbors = new ArrayList<AtomId>();
    neighbors.add(AtomId.of((String) "b"));
    neighbors.add(AtomId.of((String) "c"));
    final MolecularGraphNode node =
        MolecularGraphNode.of((AtomId) AtomId.of((String) "a"), (List<AtomId>) neighbors);
    neighbors.clear();
    Assertions.assertEquals((int) 2, (int) node.degree());
    Assertions.assertEquals(
        (Object) node,
        (Object)
            MolecularGraphNode.of(
                (AtomId) AtomId.of((String) "a"),
                List.of(AtomId.of((String) "b"), AtomId.of((String) "c"))));
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> node.neighborAtomIds().clear());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularGraphNode.of(
                (AtomId) AtomId.of((String) "a"), List.of(AtomId.of((String) "a"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularGraphNode.of(
                (AtomId) AtomId.of((String) "a"),
                List.of(AtomId.of((String) "b"), AtomId.of((String) "b"))));
  }

  private static Atom atom(final String id) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) "C"),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static Bond bond(final String firstAtomId, final String secondAtomId) {
    return Bond.of(
        (AtomId) AtomId.of((String) firstAtomId),
        (AtomId) AtomId.of((String) secondAtomId),
        (BondType) BondType.SINGLE);
  }
}