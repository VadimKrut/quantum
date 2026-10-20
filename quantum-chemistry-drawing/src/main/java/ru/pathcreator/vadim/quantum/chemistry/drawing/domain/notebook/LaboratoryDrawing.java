/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook;

import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Component layouts reference the exact molecules used by the laboratory document. */
public record LaboratoryDrawing(
    LaboratoryExperiment experiment, Map<String, MoleculeDrawing> layouts) {

  public LaboratoryDrawing {
    Objects.requireNonNull(experiment, "Laboratory experiment");
    layouts = Map.copyOf(layouts);
    if (layouts.size() != experiment.components().size()) {
      throw new IllegalArgumentException("Every component requires one layout.");
    }
    for (final LaboratoryComponent component : experiment.components()) {
      final MoleculeDrawing layout = layouts.get(component.id());
      if (layout == null || !component.molecule().equals(layout.molecule())) {
        throw new IllegalArgumentException("Layout must reference its component's core molecule.");
      }
      if (layout.atoms().size() != component.molecule().atoms().size()
          || layout.atoms().stream().map(atom -> atom.atomId()).distinct().count()
              != layout.atoms().size()
          || !layout.atoms().stream()
              .allMatch(
                  atom ->
                      component.molecule().atoms().stream()
                          .anyMatch(core -> core.id().equals(atom.atomId())))) {
        throw new IllegalArgumentException("Layout must contain every core atom exactly once.");
      }
      if (layout.bonds().size() != component.molecule().bonds().size()) {
        throw new IllegalArgumentException("Layout must contain every core bond.");
      }
      for (final var bond : component.molecule().bonds()) {
        if (layout.bonds().stream()
                .filter(style -> style.connects(bond.firstAtomId(), bond.secondAtomId()))
                .count()
            != 1) {
          throw new IllegalArgumentException("Layout must contain every core bond exactly once.");
        }
      }
    }
  }
}
