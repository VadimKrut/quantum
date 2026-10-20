/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Measurements use the core's spatial coordinates, independently of view projection. */
final class LaboratoryGeometryMeasurement {
  private LaboratoryGeometryMeasurement() {}

  static String describe(final Molecule molecule, final List<AtomId> selected) {
    final double[][] p = new double[selected.size()][3];
    for (int i = 0; i < selected.size(); i++) {
      final AtomId id = selected.get(i);
      final var atom =
          molecule.atoms().stream()
              .filter(value -> value.id().equals(id))
              .findFirst()
              .orElseThrow();
      if (atom.coordinate() == null) {
        return "Для измерения нужны координаты всех выбранных атомов.";
      }
      final var c = atom.coordinate();
      p[i] =
          new double[] {
            c.unit().toAngstrom(c.x()), c.unit().toAngstrom(c.y()), c.unit().toAngstrom(c.z())
          };
    }
    final String distance = String.format(Locale.ROOT, "1–2: %.3f Å", norm(subtract(p[0], p[1])));
    if (p.length == 2) {
      return distance;
    }
    final double[] a = subtract(p[0], p[1]), b = subtract(p[2], p[1]);
    final double denominator = norm(a) * norm(b);
    if (denominator < 1e-12) {
      return distance + " · Угол не определён: совпадающие точки.";
    }
    final double angle = Math.toDegrees(Math.acos(Math.clamp(dot(a, b) / denominator, -1, 1)));
    final String result = distance + String.format(Locale.ROOT, " · 1–2–3: %.2f°", angle);
    if (p.length == 3) {
      return result;
    }
    final double[] n1 = cross(a, b), n2 = cross(subtract(p[1], p[2]), subtract(p[3], p[2]));
    if (norm(n1) * norm(n2) < 1e-12) {
      return result + " · Торсион не определён: коллинеарные точки.";
    }
    final double torsion = Math.toDegrees(Math.atan2(dot(cross(n1, n2), b) / norm(b), dot(n1, n2)));
    return result + String.format(Locale.ROOT, " · 1–2–3–4: %.2f°", torsion);
  }

  private static double[] subtract(final double[] a, final double[] b) {
    return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
  }

  private static double dot(final double[] a, final double[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  private static double norm(final double[] a) {
    return Math.sqrt(dot(a, a));
  }

  private static double[] cross(final double[] a, final double[] b) {
    return new double[] {
      a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]
    };
  }
}
