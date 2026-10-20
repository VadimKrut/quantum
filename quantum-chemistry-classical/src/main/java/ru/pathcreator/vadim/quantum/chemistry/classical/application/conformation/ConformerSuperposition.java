/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryCoordinateSupport;

/** Least-squares rigid fit by fixed atom identities. Horn's quaternion solution; no mirror or scale. */
public final class ConformerSuperposition {
  public record Result(Molecule geometry, double rmsdAngstrom, int atomCount) {}

  private ConformerSuperposition() {}

  public static Result align(final Molecule reference, final Molecule moving) {
    Objects.requireNonNull(reference);
    Objects.requireNonNull(moving);
    if (reference.atomCount() < 2
        || reference.atomCount() > 256
        || reference.atomCount() != moving.atomCount()
        || !reference.bonds().equals(moving.bonds())
        || !reference.stereochemistry().equals(moving.stereochemistry())
        || !reference.charge().equals(moving.charge())
        || !reference.spinMultiplicity().equals(moving.spinMultiplicity())) {
      throw new IllegalArgumentException(
          "Для сравнения нужны конформеры одной структуры с теми же атомами и связями.");
    }
    final List<Integer> selected = new ArrayList<>();
    for (int i = 0; i < reference.atomCount(); i++) {
      final var a = reference.atoms().get(i);
      final var b = moving.atoms().get(i);
      if (a.coordinate() == null
          || b.coordinate() == null
          || !a.id().equals(b.id())
          || !a.element().equals(b.element())
          || !a.formalCharge().equals(b.formalCharge())
          || !Objects.equals(a.isotope(), b.isotope())
          || !a.radicalState().equals(b.radicalState())) {
        throw new IllegalArgumentException(
            "Не совпадают ID/состав атомов или отсутствуют координаты.");
      }
      if (a.element().atomicNumber() != 1) {
        selected.add(i);
      }
    }
    if (selected.isEmpty()) {
      throw new IllegalArgumentException("Нет тяжёлых атомов для сравнения конформаций.");
    }
    final double[] target = GeometryCoordinateSupport.coordinatesAngstrom(reference),
        source = GeometryCoordinateSupport.coordinatesAngstrom(moving);
    for (final double[] xyz : List.of(target, source)) {
      for (final double value : xyz) {
        if (!Double.isFinite(value) || Math.abs(value) > 100000) {
          throw new IllegalArgumentException("Недопустимые координаты для сравнения.");
        }
      }
    }
    final double[] ct = new double[3], cs = new double[3];
    for (final int atom : selected) {
      for (int j = 0; j < 3; j++) {
        ct[j] += target[3 * atom + j] / selected.size();
        cs[j] += source[3 * atom + j] / selected.size();
      }
    }
    final double[][] covariance = new double[3][3];
    for (final int atom : selected) {
      for (int j = 0; j < 3; j++) {
        for (int k = 0; k < 3; k++) {
          covariance[j][k] += (source[3 * atom + j] - cs[j]) * (target[3 * atom + k] - ct[k]);
        }
      }
    }
    final var s = covariance;
    final double[][] n = {
      {s[0][0] + s[1][1] + s[2][2], s[1][2] - s[2][1], s[2][0] - s[0][2], s[0][1] - s[1][0]},
      {s[1][2] - s[2][1], s[0][0] - s[1][1] - s[2][2], s[0][1] + s[1][0], s[2][0] + s[0][2]},
      {s[2][0] - s[0][2], s[0][1] + s[1][0], -s[0][0] + s[1][1] - s[2][2], s[1][2] + s[2][1]},
      {s[0][1] - s[1][0], s[2][0] + s[0][2], s[1][2] + s[2][1], -s[0][0] - s[1][1] + s[2][2]}
    };
    final double[] q = largestEigenvector(n);
    final double w = q[0], x = q[1], y = q[2], z = q[3];
    final double[][] rotation = {
      {1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)},
      {2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)},
      {2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)}
    };
    final double[] aligned = new double[source.length];
    for (int atom = 0; atom < moving.atomCount(); atom++) {
      for (int j = 0; j < 3; j++) {
        aligned[3 * atom + j] = ct[j];
        for (int k = 0; k < 3; k++) {
          aligned[3 * atom + j] += rotation[j][k] * (source[3 * atom + k] - cs[k]);
        }
      }
    }
    double squared = 0;
    for (final int atom : selected) {
      for (int j = 0; j < 3; j++) {
        final double delta = aligned[3 * atom + j] - target[3 * atom + j];
        squared += delta * delta;
      }
    }
    return new Result(
        GeometryCoordinateSupport.withCoordinatesAngstrom(moving, aligned),
        Math.sqrt(Math.max(0, squared / selected.size())),
        selected.size());
  }

  private static double[] largestEigenvector(final double[][] a) {
    final double[][] v = new double[4][4];
    for (int i = 0; i < 4; i++) {
      v[i][i] = 1;
    }
    for (int iteration = 0; iteration < 100; iteration++) {
      int p = 0, q = 1;
      for (int i = 0; i < 4; i++)
        for (int j = i + 1; j < 4; j++)
          if (Math.abs(a[i][j]) > Math.abs(a[p][q])) {
            p = i;
            q = j;
          }
      double norm = 0;
      for (int i = 0; i < 4; i++) norm = Math.max(norm, Math.abs(a[i][i]));
      if (Math.abs(a[p][q]) <= 1e-14 * Math.max(1, norm)) break;
      final double phi = .5 * Math.atan2(2 * a[p][q], a[q][q] - a[p][p]),
          c = Math.cos(phi),
          s = Math.sin(phi),
          app = a[p][p],
          aqq = a[q][q],
          apq = a[p][q];
      for (int k = 0; k < 4; k++)
        if (k != p && k != q) {
          final double akp = a[k][p], akq = a[k][q];
          a[k][p] = a[p][k] = c * akp - s * akq;
          a[k][q] = a[q][k] = s * akp + c * akq;
        }
      a[p][p] = c * c * app - 2 * s * c * apq + s * s * aqq;
      a[q][q] = s * s * app + 2 * s * c * apq + c * c * aqq;
      a[p][q] = a[q][p] = 0;
      for (int k = 0; k < 4; k++) {
        final double vp = v[k][p], vq = v[k][q];
        v[k][p] = c * vp - s * vq;
        v[k][q] = s * vp + c * vq;
      }
    }
    int maximum = 0;
    for (int i = 1; i < 4; i++) if (a[i][i] > a[maximum][maximum]) maximum = i;
    final double[] result = new double[4];
    double norm = 0;
    for (int i = 0; i < 4; i++) {
      result[i] = v[i][maximum];
      norm += result[i] * result[i];
    }
    for (int i = 0; i < 4; i++) result[i] /= Math.sqrt(norm);
    return result;
  }
}
