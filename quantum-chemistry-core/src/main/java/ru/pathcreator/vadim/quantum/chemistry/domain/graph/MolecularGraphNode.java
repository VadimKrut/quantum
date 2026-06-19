/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.graph;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Узел молекулярного графа: атом и упорядоченный список соседних атомов. */
public final class MolecularGraphNode {

  private final AtomId atomId;
  private final List<AtomId> neighborAtomIds;

  private MolecularGraphNode(
      final AtomId atomId,
      final List<AtomId> neighborAtomIds
  ) {
    this.atomId = atomId;
    this.neighborAtomIds = neighborAtomIds;
  }

  public static MolecularGraphNode of(
      final AtomId atomId,
      final List<AtomId> neighborAtomIds
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException("Molecular graph node atom id must not be null.");
    }
    if (neighborAtomIds == null) {
      throw new IllegalArgumentException("Molecular graph node neighbors must not be null.");
    }
    for (int i = 0; i < neighborAtomIds.size(); ++i) {
      final AtomId neighborAtomId = neighborAtomIds.get(i);
      if (neighborAtomId == null) {
        throw new IllegalArgumentException("Molecular graph node neighbor must not be null.");
      }
      if (neighborAtomId.equals(atomId)) {
        throw new IllegalArgumentException(
            "Molecular graph node must not reference itself as neighbor.");
      }
      for (int j = i + 1; j < neighborAtomIds.size(); ++j) {
        if (!neighborAtomId.equals(neighborAtomIds.get(j))) continue;
        throw new IllegalArgumentException("Molecular graph node neighbors must be unique.");
      }
    }
    return new MolecularGraphNode(atomId, List.copyOf(neighborAtomIds));
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public List<AtomId> neighborAtomIds() {
    return this.neighborAtomIds;
  }

  public int degree() {
    return this.neighborAtomIds.size();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularGraphNode)) {
      return false;
    }
    final MolecularGraphNode node = (MolecularGraphNode) other;
    return Objects.equals(this.atomId, node.atomId)
        && Objects.equals(this.neighborAtomIds, node.neighborAtomIds);
  }

  public int hashCode() {
    int result = this.atomId.hashCode();
    result = 31 * result + this.neighborAtomIds.hashCode();
    return result;
  }
}