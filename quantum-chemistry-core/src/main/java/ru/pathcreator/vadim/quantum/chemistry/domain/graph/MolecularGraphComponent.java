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

/** Связная компонента молекулярного графа. */
public final class MolecularGraphComponent {

  private final List<AtomId> atomIds;

  private MolecularGraphComponent(final List<AtomId> atomIds) {
    this.atomIds = atomIds;
  }

  public static MolecularGraphComponent of(final List<AtomId> atomIds) {
    if (atomIds == null || atomIds.isEmpty()) {
      throw new IllegalArgumentException("Molecular graph component atom ids must not be empty.");
    }
    for (int i = 0; i < atomIds.size(); ++i) {
      final AtomId atomId = atomIds.get(i);
      if (atomId == null) {
        throw new IllegalArgumentException("Molecular graph component atom id must not be null.");
      }
      for (int j = i + 1; j < atomIds.size(); ++j) {
        if (!atomId.equals(atomIds.get(j))) continue;
        throw new IllegalArgumentException("Molecular graph component atom ids must be unique.");
      }
    }
    return new MolecularGraphComponent(List.copyOf(atomIds));
  }

  public List<AtomId> atomIds() {
    return this.atomIds;
  }

  public int atomCount() {
    return this.atomIds.size();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularGraphComponent)) {
      return false;
    }
    final MolecularGraphComponent component = (MolecularGraphComponent) other;
    return Objects.equals(this.atomIds, component.atomIds);
  }

  public int hashCode() {
    return this.atomIds.hashCode();
  }
}