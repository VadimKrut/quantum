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
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Adds a ring to an existing core graph; placement is exclusively two-dimensional. */
public final class MoleculeRingPlacement {

  public MoleculeDrawing atAtom(
      final MoleculeDrawing drawing,
      final AtomId anchor,
      final int size,
      final boolean alternating) {
    validateSize(size, alternating);
    final var origin = point(drawing, anchor);
    final double radius = 44 / (2 * Math.sin(Math.PI / size));
    List<DrawingPoint2D> best = null;
    double bestScore = Double.POSITIVE_INFINITY;
    for (int direction = 0; direction < 24; direction++) {
      final double angle = direction * Math.PI / 12;
      final double centerX = origin.x() + (44 + radius) * Math.cos(angle);
      final double centerY = origin.y() + (44 + radius) * Math.sin(angle);
      final List<DrawingPoint2D> candidate = new ArrayList<>();
      candidate.add(origin);
      for (int index = 0; index < size; index++) {
        final double vertex = angle + Math.PI + index * Math.PI * 2 / size;
        candidate.add(
            DrawingPoint2D.of(
                centerX + radius * Math.cos(vertex), centerY + radius * Math.sin(vertex)));
      }
      final double score = score(drawing, candidate, 1);
      if (score < bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    return append(drawing, List.of(anchor), best, alternating, null, 1);
  }

  public MoleculeDrawing atBond(
      final MoleculeDrawing drawing,
      final AtomId first,
      final AtomId second,
      final int size,
      final boolean alternating) {
    validateSize(size, alternating);
    final Bond shared =
        drawing.molecule().bonds().stream()
            .filter(bond -> bond.connects(first, second))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выбранная связь отсутствует."));
    if (shared.type() != BondType.SINGLE
        && shared.type() != BondType.DOUBLE
        && shared.type() != BondType.AROMATIC) {
      throw new IllegalArgumentException(
          "Кольцо присоединяется к одинарной, двойной или ароматической связи.");
    }
    final var a = point(drawing, first);
    final var b = point(drawing, second);
    if (Math.hypot(b.x() - a.x(), b.y() - a.y()) < 1) {
      throw new IllegalArgumentException(
          "Разнесите концы связи на рисунке перед присоединением кольца.");
    }
    List<DrawingPoint2D> best = null;
    double bestScore = Double.POSITIVE_INFINITY;
    for (final int side : new int[] {1, -1}) {
      final List<DrawingPoint2D> candidate = new ArrayList<>();
      candidate.add(a);
      candidate.add(b);
      double dx = b.x() - a.x();
      double dy = b.y() - a.y();
      final double turn = side * Math.PI * 2 / size;
      for (int index = 2; index < size; index++) {
        final double rotatedX = dx * Math.cos(turn) - dy * Math.sin(turn);
        dy = dx * Math.sin(turn) + dy * Math.cos(turn);
        dx = rotatedX;
        final var previous = candidate.getLast();
        candidate.add(DrawingPoint2D.of(previous.x() + dx, previous.y() + dy));
      }
      final double score = score(drawing, candidate, 2);
      if (score < bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    return append(drawing, List.of(first, second), best, alternating, shared.type(), 0);
  }

  private static MoleculeDrawing append(
      final MoleculeDrawing drawing,
      final List<AtomId> shared,
      final List<DrawingPoint2D> points,
      final boolean alternating,
      final BondType sharedType,
      final int ringStart) {
    final var atoms = new ArrayList<>(drawing.molecule().atoms());
    final var positions = new ArrayList<>(drawing.atoms());
    final var bonds = new ArrayList<>(drawing.molecule().bonds());
    final var styles = new ArrayList<>(drawing.bonds());
    final var ids = new ArrayList<>(shared);
    for (int index = shared.size(); index < points.size(); index++) {
      final AtomId id = AtomId.of("a_" + UUID.randomUUID());
      ids.add(id);
      atoms.add(Atom.of(id, ElementSymbol.of("C"), null));
      positions.add(AtomDrawing.of(id, points.get(index), null, true));
    }
    if (ringStart == 1) {
      bonds.add(Bond.of(ids.getFirst(), ids.get(1), BondType.SINGLE));
      styles.add(BondDrawing.of(ids.getFirst(), ids.get(1), BondDrawingStyle.PLAIN));
    }
    // A fused conjugated ring may have two new double bonds when its shared edge is single.
    // Do not create a second incident double bond on an already substituted junction.
    final var automaticHydrogens =
        drawing.molecule().atoms().stream()
            .filter(MoleculeSkeletonHydrogens::automatic)
            .map(Atom::id)
            .collect(java.util.stream.Collectors.toSet());
    final boolean occupiedJunction =
        shared.size() == 2
            && shared.stream()
                .anyMatch(
                    id ->
                        drawing.molecule().bonds().stream()
                                .filter(
                                    bond ->
                                        bond.firstAtomId().equals(id)
                                            || bond.secondAtomId().equals(id))
                                .filter(
                                    bond ->
                                        !automaticHydrogens.contains(bond.firstAtomId())
                                            && !automaticHydrogens.contains(bond.secondAtomId()))
                                .mapToInt(
                                    bond ->
                                        bond.type() == BondType.DOUBLE
                                            ? 2
                                            : bond.type() == BondType.TRIPLE ? 3 : 1)
                                .sum()
                            >= 3);
    for (int index = shared.size() == 2 ? 1 : ringStart; index < ids.size(); index++) {
      final AtomId first = ids.get(index);
      final AtomId second = ids.get(index + 1 == ids.size() ? ringStart : index + 1);
      final BondType type =
          !alternating
              ? BondType.SINGLE
              : sharedType == BondType.AROMATIC
                  ? BondType.AROMATIC
                  : (index
                                  - ringStart
                                  + (sharedType == BondType.SINGLE && !occupiedJunction ? 1 : 0))
                              % 2
                          == 0
                      ? BondType.DOUBLE
                      : BondType.SINGLE;
      bonds.add(Bond.of(first, second, type));
      styles.add(BondDrawing.of(first, second, BondDrawingStyle.PLAIN));
    }
    final var molecule = drawing.molecule();
    final MoleculeSketchService service = new MoleculeSketchService();
    final var result =
        service.reviseGraph(
            drawing,
            molecule.displayName(),
            atoms,
            bonds,
            positions,
            styles,
            molecule.charge().value(),
            molecule.spinMultiplicity().value());
    return service.translate(result, 0, 0);
  }

  private static DrawingPoint2D point(final MoleculeDrawing drawing, final AtomId id) {
    return drawing.atoms().stream()
        .filter(atom -> atom.atomId().equals(id))
        .map(AtomDrawing::point2D)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Выбранный атом отсутствует."));
  }

  private static double score(
      final MoleculeDrawing drawing, final List<DrawingPoint2D> points, final int shared) {
    double score = 0;
    for (int index = shared; index < points.size(); index++) {
      final var point = points.get(index);
      score += Math.max(0, 24 - point.x()) * 100 + Math.max(0, 24 - point.y()) * 100;
      for (final var existing : drawing.atoms()) {
        final double distance =
            Math.hypot(existing.point2D().x() - point.x(), existing.point2D().y() - point.y());
        score += 1000 / (1 + distance * distance);
        if (distance < 30) {
          score += (30 - distance) * 100;
        }
      }
    }
    return score;
  }

  private static void validateSize(final int size, final boolean alternating) {
    if (size < 3 || size > 8 || alternating && size != 6) {
      throw new IllegalArgumentException("Неподдерживаемый шаблон кольца.");
    }
  }
}
