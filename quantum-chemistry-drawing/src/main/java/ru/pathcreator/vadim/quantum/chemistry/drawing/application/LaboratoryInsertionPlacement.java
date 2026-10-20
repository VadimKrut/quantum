/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Places one imported participant in a free row; existing drawings and chemical data are untouched. */
final class LaboratoryInsertionPlacement {
  private static final double LEFT = 24, TOP = 24, ROW_WIDTH = 1120, MARGIN_X = 48, MARGIN_Y = 56;

  private record Bounds(double left, double top, double right, double bottom) {
    double width() {
      return this.right - this.left;
    }

    double height() {
      return this.bottom - this.top;
    }
  }

  private LaboratoryInsertionPlacement() {}

  static MoleculeDrawing place(
      final MoleculeDrawing drawing, final Collection<MoleculeDrawing> existing) {
    Objects.requireNonNull(drawing);
    Objects.requireNonNull(existing);
    if (drawing.atoms().isEmpty()) {
      throw new IllegalArgumentException("Для размещения новой структуры нужны координаты 2D.");
    }
    final Bounds source = bounds(drawing);
    final List<Bounds> occupied =
        existing.stream()
            .filter(d -> !d.atoms().isEmpty())
            .map(LaboratoryInsertionPlacement::bounds)
            .sorted(Comparator.comparingDouble(Bounds::left))
            .toList();
    final double right = Math.max(LEFT + ROW_WIDTH, LEFT + source.width());
    double y = TOP;
    // Each occupied row advances to a strictly later bottom edge; no arbitrary retry or grid limit.
    for (int row = 0; row <= occupied.size(); row++) {
      double x = LEFT, nextY = Double.POSITIVE_INFINITY;
      for (final Bounds other : occupied) {
        if (other.bottom() <= y || other.top() >= y + source.height()) {
          continue;
        }
        nextY = Math.min(nextY, other.bottom());
        if (other.right() <= x) {
          continue;
        }
        if (x + source.width() <= other.left()) {
          break;
        }
        x = Math.max(x, other.right());
      }
      if (Double.isFinite(x + source.width()) && x + source.width() <= right) {
        return translate(drawing, x - source.left(), y - source.top());
      }
      if (!Double.isFinite(nextY) || nextY <= y) {
        break;
      }
      y = nextY;
    }
    throw new IllegalArgumentException(
        "Не удалось найти свободное место для структуры. Проверьте диапазон координат 2D.");
  }

  private static Bounds bounds(final MoleculeDrawing drawing) {
    final double minX =
        drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).min().orElseThrow();
    final double maxX =
        drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).max().orElseThrow();
    final double minY =
        drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).min().orElseThrow();
    final double maxY =
        drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).max().orElseThrow();
    final var bounds =
        new Bounds(minX - MARGIN_X, minY - MARGIN_Y, maxX + MARGIN_X, maxY + MARGIN_Y);
    if (!Double.isFinite(bounds.left())
        || !Double.isFinite(bounds.top())
        || !Double.isFinite(bounds.right())
        || !Double.isFinite(bounds.bottom())
        || !Double.isFinite(bounds.width())
        || !Double.isFinite(bounds.height())
        || bounds.width() <= 0
        || bounds.height() <= 0) {
      throw new IllegalArgumentException(
          "Координаты 2D выходят за допустимый диапазон размещения.");
    }
    return bounds;
  }

  private static MoleculeDrawing translate(
      final MoleculeDrawing drawing, final double dx, final double dy) {
    if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
      throw new IllegalArgumentException(
          "Координаты 2D выходят за допустимый диапазон размещения.");
    }
    return MoleculeDrawing.of(
        drawing.molecule(),
        drawing.atoms().stream()
            .map(a -> a.withPoint2D(DrawingPoint2D.of(a.point2D().x() + dx, a.point2D().y() + dy)))
            .toList(),
        drawing.bonds(),
        drawing.manualFields());
  }
}
