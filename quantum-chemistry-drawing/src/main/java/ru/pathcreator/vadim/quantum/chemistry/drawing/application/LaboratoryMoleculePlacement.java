/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Graph layout affects display coordinates only. It is never a generated molecular conformation. */
public final class LaboratoryMoleculePlacement {
  private static final List<MoleculeLayoutProvider> PROVIDERS =
      java.util.ServiceLoader.load(MoleculeLayoutProvider.class).stream()
          .map(java.util.ServiceLoader.Provider::get)
          .toList();

  private static double[] coordinates(final Molecule molecule) {
    for (final var provider : PROVIDERS) {
      final var result = provider.coordinates(molecule, 44);
      if (result.isPresent()
          && result.get().length == molecule.atomCount() * 2
          && java.util.Arrays.stream(result.get()).allMatch(Double::isFinite)) {
        return result.get();
      }
    }
    return ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEmbedding
        .planarCoordinates(molecule, 44);
  }

  public MoleculeDrawing place(
      final Molecule molecule, final double centerX, final double centerY) {
    final int count = molecule.atoms().size();
    final double[] x = new double[count];
    final double[] y = new double[count];
    if (count <= 500) {
      final double[] points = coordinates(molecule);
      LaboratoryDrawingUntangler.improve(molecule, points, 44);
      for (int i = 0; i < count; i++) {
        x[i] = points[i * 2];
        y[i] = points[i * 2 + 1];
      }
    } else {
      // Large documents retain a bounded initial display; no atoms or chemical properties are
      // dropped.
      for (int i = 0; i < count; i++) {
        x[i] = 65 * Math.cos(2 * Math.PI * i / count);
        y[i] = 65 * Math.sin(2 * Math.PI * i / count);
      }
    }
    final double minX = java.util.Arrays.stream(x).min().orElse(0);
    final double maxX = java.util.Arrays.stream(x).max().orElse(0);
    final double minY = java.util.Arrays.stream(y).min().orElse(0);
    final double maxY = java.util.Arrays.stream(y).max().orElse(0);
    // Keep bond spacing consistent with label sizes. Viewports fit the complete drawing,
    // including text, rather than shrinking atom coordinates independently of labels.
    final List<AtomDrawing> atoms = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      atoms.add(
          AtomDrawing.of(
              molecule.atoms().get(i).id(),
              DrawingPoint2D.of(
                  centerX + x[i] - (minX + maxX) / 2, centerY + y[i] - (minY + maxY) / 2),
              null,
              true));
    }
    return MoleculeDrawing.of(
        molecule,
        atoms,
        molecule.bonds().stream()
            .map(
                bond ->
                    BondDrawing.of(bond.firstAtomId(), bond.secondAtomId(), BondDrawingStyle.PLAIN))
            .toList(),
        List.of());
  }
}
