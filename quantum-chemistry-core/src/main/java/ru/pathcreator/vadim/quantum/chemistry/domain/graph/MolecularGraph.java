/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Граф молекулы с неизменяемыми узлами, компонентами, кольцами и быстрым поиском по atom id. */
public final class MolecularGraph {

  private final List<MolecularGraphNode> nodes;
  private final List<MolecularGraphComponent> components;
  private final List<MolecularRing> rings;
  private final Map<AtomId, MolecularGraphNode> nodeByAtomId;

  private MolecularGraph(
      final List<MolecularGraphNode> nodes,
      final List<MolecularGraphComponent> components,
      final List<MolecularRing> rings,
      final Map<AtomId, MolecularGraphNode> nodeByAtomId) {
    this.nodes = nodes;
    this.components = components;
    this.rings = rings;
    this.nodeByAtomId = nodeByAtomId;
  }

  public static MolecularGraph of(
      final List<MolecularGraphNode> nodes,
      final List<MolecularGraphComponent> components,
      final List<MolecularRing> rings) {
    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException("Molecular graph nodes must not be empty.");
    }
    if (components == null || components.isEmpty()) {
      throw new IllegalArgumentException("Molecular graph components must not be empty.");
    }
    if (rings == null) {
      throw new IllegalArgumentException("Molecular graph rings must not be null.");
    }
    final HashMap<AtomId, MolecularGraphNode> nodeByAtomId =
        new HashMap<AtomId, MolecularGraphNode>(nodes.size() * 2);
    for (int i = 0; i < nodes.size(); ++i) {
      final MolecularGraphNode node = nodes.get(i);
      if (node == null) {
        throw new IllegalArgumentException("Molecular graph node must not be null.");
      }
      if (nodeByAtomId.put(node.atomId(), node) == null) {
        continue;
      }
      throw new IllegalArgumentException("Molecular graph node atom ids must be unique.");
    }
    for (int i = 0; i < components.size(); ++i) {
      final MolecularGraphComponent component = components.get(i);
      if (component == null) {
        throw new IllegalArgumentException("Molecular graph component must not be null.");
      }
      MolecularGraph.requirePresentAtomIds(nodeByAtomId, component.atomIds());
    }
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (ring == null) {
        throw new IllegalArgumentException("Molecular graph ring must not be null.");
      }
      MolecularGraph.requirePresentAtomIds(nodeByAtomId, ring.atomIds());
    }
    return new MolecularGraph(
        List.copyOf(nodes), List.copyOf(components), List.copyOf(rings), Map.copyOf(nodeByAtomId));
  }

  private static void requirePresentAtomIds(
      final Map<AtomId, MolecularGraphNode> nodeByAtomId, final List<AtomId> atomIds) {
    for (int i = 0; i < atomIds.size(); ++i) {
      if (nodeByAtomId.containsKey(atomIds.get(i))) {
        continue;
      }
      throw new IllegalArgumentException("Molecular graph atom id is not present.");
    }
  }

  public List<MolecularGraphNode> nodes() {
    return this.nodes;
  }

  public List<MolecularGraphComponent> components() {
    return this.components;
  }

  public List<MolecularRing> rings() {
    return this.rings;
  }

  public boolean connected() {
    return this.components.size() == 1;
  }

  public int degreeOf(final AtomId atomId) {
    return this.nodeOf(atomId).degree();
  }

  public List<AtomId> neighborsOf(final AtomId atomId) {
    return this.nodeOf(atomId).neighborAtomIds();
  }

  public boolean cyclic() {
    return !this.rings.isEmpty();
  }

  private MolecularGraphNode nodeOf(final AtomId atomId) {
    if (atomId == null) {
      throw new IllegalArgumentException("Molecular graph atom id must not be null.");
    }
    final MolecularGraphNode node = this.nodeByAtomId.get(atomId);
    if (node != null) {
      return node;
    }
    throw new IllegalArgumentException("Molecular graph atom id is not present.");
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularGraph)) {
      return false;
    }
    final MolecularGraph graph = (MolecularGraph) other;
    return Objects.equals(this.nodes, graph.nodes)
        && Objects.equals(this.components, graph.components)
        && Objects.equals(this.rings, graph.rings);
  }

  public int hashCode() {
    int result = this.nodes.hashCode();
    result = 31 * result + this.components.hashCode();
    result = 31 * result + this.rings.hashCode();
    return result;
  }
}