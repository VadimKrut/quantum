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

/** Простое кольцо молекулярного графа без повторяющихся атомов. */
public final class MolecularRing {

  private final List<AtomId> atomIds;

  private MolecularRing(final List<AtomId> atomIds) {
    this.atomIds = atomIds;
  }

  public static MolecularRing of(final List<AtomId> atomIds) {
    if (atomIds == null || atomIds.size() < 3) {
      throw new IllegalArgumentException("Molecular ring must contain at least three atoms.");
    }
    for (int i = 0; i < atomIds.size(); ++i) {
      final AtomId atomId = atomIds.get(i);
      if (atomId == null) {
        throw new IllegalArgumentException("Molecular ring atom id must not be null.");
      }
      for (int j = i + 1; j < atomIds.size(); ++j) {
        if (!atomId.equals(atomIds.get(j))) continue;
        throw new IllegalArgumentException("Molecular ring atom ids must be unique.");
      }
    }
    return new MolecularRing(List.copyOf(atomIds));
  }

  public List<AtomId> atomIds() {
    return this.atomIds;
  }

  public int size() {
    return this.atomIds.size();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularRing)) {
      return false;
    }
    final MolecularRing ring = (MolecularRing) other;
    return Objects.equals(this.atomIds, ring.atomIds);
  }

  public int hashCode() {
    return this.atomIds.hashCode();
  }
}