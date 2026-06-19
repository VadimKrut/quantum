/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.valence;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class MolecularValence {

  private final List<AtomValence> atomValences;

  private MolecularValence(final List<AtomValence> atomValences) {
    this.atomValences = atomValences;
  }

  public static MolecularValence of(final List<AtomValence> atomValences) {
    if (atomValences == null || atomValences.isEmpty()) {
      throw new IllegalArgumentException("Molecular valence atoms must not be empty.");
    }
    for (int i = 0; i < atomValences.size(); ++i) {
      AtomValence atomValence = atomValences.get(i);
      if (atomValence == null) {
        throw new IllegalArgumentException("Molecular valence atom must not be null.");
      }
      for (int j = i + 1; j < atomValences.size(); ++j) {
        if (!atomValence.atomId().equals(atomValences.get(j).atomId())) continue;
        throw new IllegalArgumentException("Molecular valence atom ids must be unique.");
      }
    }
    return new MolecularValence(List.copyOf(atomValences));
  }

  public List<AtomValence> atomValences() {
    return this.atomValences;
  }

  public AtomValence atomValenceOf(final AtomId atomId) {
    if (atomId == null) {
      throw new IllegalArgumentException("Molecular valence atom id must not be null.");
    }
    for (int i = 0; i < this.atomValences.size(); ++i) {
      final AtomValence atomValence = this.atomValences.get(i);
      if (!atomValence.atomId().equals(atomId)) continue;
      return atomValence;
    }
    throw new IllegalArgumentException("Molecular valence atom id is not present.");
  }

  public boolean hasUnknownBondOrder() {
    for (int i = 0; i < this.atomValences.size(); ++i) {
      if (!this.atomValences.get(i).hasUnknownBondOrder()) continue;
      return true;
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularValence)) {
      return false;
    }
    final MolecularValence valence = (MolecularValence) other;
    return Objects.equals(this.atomValences, valence.atomValences);
  }

  public int hashCode() {
    return this.atomValences.hashCode();
  }
}