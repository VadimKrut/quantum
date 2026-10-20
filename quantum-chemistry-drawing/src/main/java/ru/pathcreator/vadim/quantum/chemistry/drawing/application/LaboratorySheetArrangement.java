/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Packs component drawings in document order using translations only; chemistry and stereo stay intact. */
public final class LaboratorySheetArrangement {
  public Map<String, MoleculeDrawing> arrange(
      final LaboratoryDrawing document, final double width) {
    if (!Double.isFinite(width) || width <= 0) {
      throw new IllegalArgumentException("Ширина листа должна быть положительной.");
    }
    final double limit = Math.max(240, width);
    final var result = new LinkedHashMap<String, MoleculeDrawing>();
    final var sketch = new MoleculeSketchService();
    double left = 48, top = 48, rowHeight = 0;
    for (final var component : document.experiment().components()) {
      final var drawing = document.layouts().get(component.id());
      if (drawing.atoms().isEmpty()) {
        continue;
      }
      final var xs =
          drawing.atoms().stream().mapToDouble(atom -> atom.point2D().x()).summaryStatistics();
      final var ys =
          drawing.atoms().stream().mapToDouble(atom -> atom.point2D().y()).summaryStatistics();
      final double nameWidth =
          Math.min(
              280,
              component
                      .molecule()
                      .displayName()
                      .codePointCount(0, component.molecule().displayName().length())
                  * 14.0);
      final double cellWidth = Math.max(Math.max(96, nameWidth), xs.getMax() - xs.getMin() + 48);
      final double cellHeight = Math.max(44, ys.getMax() - ys.getMin()) + 64;
      if (left > 48 && left + cellWidth > limit - 48) {
        left = 48;
        top += rowHeight + 40;
        rowHeight = 0;
      }
      final double dx = left + cellWidth / 2 - (xs.getMin() + xs.getMax()) / 2;
      final double dy = top - ys.getMin();
      if (Math.abs(dx) > 1e-9 || Math.abs(dy) > 1e-9) {
        result.put(
            component.id(),
            sketch.translateAtoms(
                drawing,
                drawing.atoms().stream().map(atom -> atom.atomId()).collect(Collectors.toSet()),
                dx,
                dy));
      }
      left += cellWidth + 40;
      rowHeight = Math.max(rowHeight, cellHeight);
    }
    return Map.copyOf(result);
  }
}
