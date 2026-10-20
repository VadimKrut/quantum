/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import java.util.function.BooleanSupplier;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Search is a read-only projection of the core graph, never a source of chemical edits. */
public interface MoleculeStructureSearch {
  enum Mode {
    EXACT,
    SUBSTRUCTURE,
    SIMILARITY
  }

  record Options(Mode mode, boolean stereo, double threshold) {
    public Options {
      java.util.Objects.requireNonNull(mode);
      if (!Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
        throw new IllegalArgumentException("Порог сходства должен быть от 0 до 1.");
      }
    }
  }

  record Match(boolean matched, double score, Set<AtomId> atoms) {
    public Match {
      atoms = Set.copyOf(atoms);
    }
  }

  interface Query {
    Match match(Molecule target);

    /** Implementations must not silently discard the depiction when checking configuration. */
    default Match match(MoleculeDrawing target) {
      throw new IllegalArgumentException(
          "Конфигурация не сопоставлена: поисковый движок не поддерживает рисунок.");
    }
  }

  default Query compile(MoleculeDrawing drawing, Options options, BooleanSupplier cancelled) {
    return this.compile(drawing, Set.of(), options, cancelled);
  }

  default Query compile(
      MoleculeDrawing drawing, Set<AtomId> selected, Options options, BooleanSupplier cancelled) {
    throw new IllegalArgumentException(
        "Конфигурация не сопоставлена: поисковый движок не поддерживает рисунок.");
  }

  default Query compile(Molecule molecule, Options options, BooleanSupplier cancelled) {
    return this.compile(molecule, Set.of(), options, cancelled);
  }

  Query compile(
      Molecule molecule, Set<AtomId> selected, Options options, BooleanSupplier cancelled);
}
