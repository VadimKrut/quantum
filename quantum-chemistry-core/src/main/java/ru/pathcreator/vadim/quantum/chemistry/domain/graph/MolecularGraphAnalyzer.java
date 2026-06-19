/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Строит производное графовое представление молекулы: соседей, компоненты связности и простые
 * циклы.
 */
public final class MolecularGraphAnalyzer {

  private MolecularGraphAnalyzer() {}

  public static MolecularGraph analyze(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular graph molecule must not be null.");
    }
    final MolecularGraphIndex index = MolecularGraphIndex.of(molecule.atoms(), molecule.bonds());
    final List<MolecularGraphNode> nodes = MolecularGraphAnalyzer.nodesOf(index);
    return MolecularGraph.of(
        nodes, MolecularGraphAnalyzer.componentsOf(index), MolecularGraphAnalyzer.ringsOf(index));
  }

  private static List<MolecularGraphNode> nodesOf(final MolecularGraphIndex index) {
    final ArrayList<MolecularGraphNode> nodes =
        new ArrayList<MolecularGraphNode>(index.atomCount());
    for (int atomIndex = 0; atomIndex < index.atomCount(); ++atomIndex) {
      nodes.add(
          MolecularGraphNode.of(index.atomIdAt(atomIndex), index.neighborAtomIdsAt(atomIndex)));
    }
    return List.copyOf(nodes);
  }

  private static List<MolecularGraphComponent> componentsOf(final MolecularGraphIndex index) {
    final boolean[] visited = new boolean[index.atomCount()];
    final ArrayList<MolecularGraphComponent> components = new ArrayList<MolecularGraphComponent>();
    for (int atomIndex = 0; atomIndex < index.atomCount(); ++atomIndex) {
      if (visited[atomIndex]) {
        continue;
      }
      components.add(MolecularGraphAnalyzer.componentFrom(index, visited, atomIndex));
    }
    return List.copyOf(components);
  }

  private static MolecularGraphComponent componentFrom(
      final MolecularGraphIndex index, final boolean[] visited, final int startIndex) {
    final ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
    final ArrayList<AtomId> atomIds = new ArrayList<AtomId>();
    visited[startIndex] = true;
    queue.add(startIndex);
    while (!queue.isEmpty()) {
      final int atomIndex = queue.remove().intValue();
      atomIds.add(index.atomIdAt(atomIndex));
      final int[] neighbors = index.neighborIndexesAt(atomIndex);
      for (int neighborOffset = 0; neighborOffset < neighbors.length; ++neighborOffset) {
        final int neighborIndex = neighbors[neighborOffset];
        if (visited[neighborIndex]) {
          continue;
        }
        visited[neighborIndex] = true;
        queue.add(neighborIndex);
      }
    }
    return MolecularGraphComponent.of(atomIds);
  }

  private static List<MolecularRing> ringsOf(final MolecularGraphIndex index) {
    final ArrayList<MolecularRing> rings = new ArrayList<MolecularRing>();
    final HashSet<String> canonicalKeys = new HashSet<String>();
    for (int atomIndex = 0; atomIndex < index.atomCount(); ++atomIndex) {
      final ArrayList<AtomId> path = new ArrayList<AtomId>();
      final boolean[] visited = new boolean[index.atomCount()];
      MolecularGraphAnalyzer.findRingsFrom(
          index, atomIndex, atomIndex, visited, path, canonicalKeys, rings);
    }
    return List.copyOf(rings);
  }

  private static void findRingsFrom(
      final MolecularGraphIndex index,
      final int startIndex,
      final int currentIndex,
      final boolean[] visited,
      final List<AtomId> path,
      final Set<String> canonicalKeys,
      final List<MolecularRing> rings) {
    visited[currentIndex] = true;
    path.add(index.atomIdAt(currentIndex));
    final int[] neighbors = index.neighborIndexesAt(currentIndex);
    for (int neighborOffset = 0; neighborOffset < neighbors.length; ++neighborOffset) {
      final int neighborIndex = neighbors[neighborOffset];
      if (neighborIndex == startIndex && path.size() >= 3) {
        MolecularGraphAnalyzer.addRingIfNew(path, canonicalKeys, rings);
        continue;
      }
      if (visited[neighborIndex]) {
        continue;
      }
      MolecularGraphAnalyzer.findRingsFrom(
          index, startIndex, neighborIndex, visited, path, canonicalKeys, rings);
    }
    path.remove(path.size() - 1);
    visited[currentIndex] = false;
  }

  private static void addRingIfNew(
      final List<AtomId> path, final Set<String> canonicalKeys, final List<MolecularRing> rings) {
    final String key = MolecularGraphAnalyzer.canonicalKey(path);
    if (canonicalKeys.add(key)) {
      rings.add(MolecularRing.of(path));
    }
  }

  private static String canonicalKey(final List<AtomId> path) {
    final String[] values = new String[path.size()];
    for (int i = 0; i < path.size(); ++i) {
      values[i] = path.get(i).value();
    }
    String best = null;
    for (int direction = 0; direction < 2; ++direction) {
      for (int offset = 0; offset < values.length; ++offset) {
        final String candidate = MolecularGraphAnalyzer.rotatedKey(values, offset, direction == 1);
        if (best != null && candidate.compareTo(best) >= 0) continue;
        best = candidate;
      }
    }
    return best;
  }

  private static String rotatedKey(
      final String[] values,
      final int offset,
      final boolean reverse
  ) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.length; ++i) {
      final int index =
          reverse ? Math.floorMod(offset - i, values.length) : (offset + i) % values.length;
      if (i > 0) {
        builder.append('|');
      }
      builder.append(values[index]);
    }
    return builder.toString();
  }

  /** Индексирует атомы и связи один раз, чтобы графовые обходы не искали соседей линейно. */
  private static final class MolecularGraphIndex {

    private final AtomId[] atomIds;
    private final int[][] neighborIndexes;
    private final List<List<AtomId>> neighborAtomIds;

    private MolecularGraphIndex(
        final AtomId[] atomIds,
        final int[][] neighborIndexes,
        final List<List<AtomId>> neighborAtomIds) {
      this.atomIds = atomIds;
      this.neighborIndexes = neighborIndexes;
      this.neighborAtomIds = neighborAtomIds;
    }

    private static MolecularGraphIndex of(
        final List<Atom> atoms,
        final List<Bond> bonds
    ) {
      final AtomId[] atomIds = new AtomId[atoms.size()];
      final Map<AtomId, Integer> atomIndexes = new HashMap<AtomId, Integer>(atoms.size() * 2);
      final ArrayList<ArrayList<Integer>> neighborIndexes =
          new ArrayList<ArrayList<Integer>>(atoms.size());
      for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
        final AtomId atomId = atoms.get(atomIndex).id();
        atomIds[atomIndex] = atomId;
        atomIndexes.put(atomId, Integer.valueOf(atomIndex));
        neighborIndexes.add(new ArrayList<Integer>());
      }
      for (int bondIndex = 0; bondIndex < bonds.size(); ++bondIndex) {
        final Bond bond = bonds.get(bondIndex);
        final int firstIndex = MolecularGraphIndex.requiredIndex(atomIndexes, bond.firstAtomId());
        final int secondIndex = MolecularGraphIndex.requiredIndex(atomIndexes, bond.secondAtomId());
        neighborIndexes.get(firstIndex).add(Integer.valueOf(secondIndex));
        neighborIndexes.get(secondIndex).add(Integer.valueOf(firstIndex));
      }
      final int[][] compactNeighborIndexes = new int[atoms.size()][];
      final ArrayList<List<AtomId>> compactNeighborAtomIds =
          new ArrayList<List<AtomId>>(atoms.size());
      for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
        final ArrayList<Integer> neighbors = neighborIndexes.get(atomIndex);
        final int[] compactNeighbors = new int[neighbors.size()];
        final ArrayList<AtomId> compactNeighborAtomIdsForAtom =
            new ArrayList<AtomId>(neighbors.size());
        for (int neighborOffset = 0; neighborOffset < neighbors.size(); ++neighborOffset) {
          final int neighborIndex = neighbors.get(neighborOffset).intValue();
          compactNeighbors[neighborOffset] = neighborIndex;
          compactNeighborAtomIdsForAtom.add(atomIds[neighborIndex]);
        }
        compactNeighborIndexes[atomIndex] = compactNeighbors;
        compactNeighborAtomIds.add(List.copyOf(compactNeighborAtomIdsForAtom));
      }
      return new MolecularGraphIndex(
          atomIds, compactNeighborIndexes, List.copyOf(compactNeighborAtomIds));
    }

    private static int requiredIndex(
        final Map<AtomId, Integer> atomIndexes,
        final AtomId atomId
    ) {
      final Integer index = atomIndexes.get(atomId);
      if (index == null) {
        throw new IllegalArgumentException("Molecular graph bond atom id is not present.");
      }
      return index.intValue();
    }

    private int atomCount() {
      return this.atomIds.length;
    }

    private AtomId atomIdAt(final int atomIndex) {
      return this.atomIds[atomIndex];
    }

    private int[] neighborIndexesAt(final int atomIndex) {
      return this.neighborIndexes[atomIndex];
    }

    private List<AtomId> neighborAtomIdsAt(final int atomIndex) {
      return this.neighborAtomIds.get(atomIndex);
    }
  }
}