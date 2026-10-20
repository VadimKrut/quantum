/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Rigid placement of a reusable 2D fragment followed by the existing core graph join. */
public final class MoleculeFragmentPlacement {
  private MoleculeFragmentPlacement() {}

  /** Choose an unoccupied direction around the target atom; the user can override it. */
  public static int suggestedDirection(final MoleculeDrawing target, final AtomId targetAtom) {
    final var center =
        target.atoms().stream()
            .filter(atom -> atom.atomId().equals(targetAtom))
            .findFirst()
            .orElseThrow()
            .point2D();
    final var angles = new java.util.ArrayList<Double>();
    for (final var bond : target.molecule().bonds()) {
      final var other =
          bond.firstAtomId().equals(targetAtom)
              ? bond.secondAtomId()
              : bond.secondAtomId().equals(targetAtom) ? bond.firstAtomId() : null;
      if (other == null) {
        continue;
      }
      final var position =
          target.atoms().stream()
              .filter(atom -> atom.atomId().equals(other))
              .findFirst()
              .orElseThrow()
              .point2D();
      angles.add(Math.atan2(position.y() - center.y(), position.x() - center.x()));
    }
    int best = 0;
    double bestClearance = -1;
    for (int direction = 0; direction < 360; direction += 45) {
      final double radians = Math.toRadians(direction);
      final double clearance =
          angles.stream()
              .mapToDouble(
                  angle ->
                      Math.abs(Math.atan2(Math.sin(angle - radians), Math.cos(angle - radians))))
              .min()
              .orElse(Math.PI);
      if (clearance > bestClearance + 1e-8) {
        bestClearance = clearance;
        best = direction;
      }
    }
    return best;
  }

  /** Detect new overlaps with the original scaffold, without treating pre-existing overlaps as new. */
  public static boolean overlaps(final MoleculeDrawing original, final MoleculeDrawing joined) {
    final var oldIds =
        original.molecule().atoms().stream()
            .map(atom -> atom.id())
            .collect(java.util.stream.Collectors.toSet());
    final var positions =
        joined.atoms().stream()
            .collect(
                java.util.stream.Collectors.toMap(atom -> atom.atomId(), atom -> atom.point2D()));
    final var oldBonds =
        joined.molecule().bonds().stream()
            .filter(
                bond -> oldIds.contains(bond.firstAtomId()) && oldIds.contains(bond.secondAtomId()))
            .toList();
    for (final var atom : joined.atoms()) {
      if (oldIds.contains(atom.atomId())) {
        continue;
      }
      for (final var oldId : oldIds) {
        final var old = positions.get(oldId);
        if (Math.hypot(atom.point2D().x() - old.x(), atom.point2D().y() - old.y()) < 22) {
          return true;
        }
      }
    }
    for (final var bond : joined.molecule().bonds()) {
      if (oldIds.contains(bond.firstAtomId()) && oldIds.contains(bond.secondAtomId())) {
        continue;
      }
      for (final var old : oldBonds) {
        if (bond.firstAtomId().equals(old.firstAtomId())
            || bond.firstAtomId().equals(old.secondAtomId())
            || bond.secondAtomId().equals(old.firstAtomId())
            || bond.secondAtomId().equals(old.secondAtomId())) {
          continue;
        }
        final var a = positions.get(bond.firstAtomId());
        final var b = positions.get(bond.secondAtomId());
        final var c = positions.get(old.firstAtomId());
        final var d = positions.get(old.secondAtomId());
        if (cross(a, b, c) * cross(a, b, d) <= 0
            && cross(c, d, a) * cross(c, d, b) <= 0
            && Math.max(Math.min(a.x(), b.x()), Math.min(c.x(), d.x()))
                <= Math.min(Math.max(a.x(), b.x()), Math.max(c.x(), d.x())) + 1e-8
            && Math.max(Math.min(a.y(), b.y()), Math.min(c.y(), d.y()))
                <= Math.min(Math.max(a.y(), b.y()), Math.max(c.y(), d.y())) + 1e-8) {
          return true;
        }
      }
    }
    return false;
  }

  private static double cross(
      final DrawingPoint2D a, final DrawingPoint2D b, final DrawingPoint2D c) {
    return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
  }

  public static MoleculeDrawing attach(
      final MoleculeDrawing target,
      final AtomId targetAtom,
      final MoleculeDrawing fragment,
      final AtomId attachmentAtom,
      final BondType type,
      final double degrees) {
    if (!Double.isFinite(degrees)) {
      throw new IllegalArgumentException("Направление должно быть конечным числом.");
    }
    if (type != BondType.SINGLE && type != BondType.DOUBLE && type != BondType.TRIPLE) {
      throw new IllegalArgumentException("Новая связь должна быть одинарной, двойной или тройной.");
    }
    final var anchor =
        fragment.atoms().stream()
            .filter(atom -> atom.atomId().equals(attachmentAtom))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Точка присоединения отсутствует во фрагменте."))
            .point2D();
    final var destination =
        target.atoms().stream()
            .filter(atom -> atom.atomId().equals(targetAtom))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Выбранный атом отсутствует в структуре."))
            .point2D();
    double sumX = 0;
    double sumY = 0;
    for (final var bond : fragment.molecule().bonds()) {
      final AtomId other =
          bond.firstAtomId().equals(attachmentAtom)
              ? bond.secondAtomId()
              : bond.secondAtomId().equals(attachmentAtom) ? bond.firstAtomId() : null;
      if (other == null) {
        continue;
      }
      final var position =
          fragment.atoms().stream()
              .filter(atom -> atom.atomId().equals(other))
              .findFirst()
              .orElseThrow()
              .point2D();
      final double dx = position.x() - anchor.x();
      final double dy = position.y() - anchor.y();
      final double length = Math.hypot(dx, dy);
      if (length > 1e-8) {
        sumX += dx / length;
        sumY += dy / length;
      }
    }
    final double angle = Math.toRadians(degrees % 360);
    final double outgoing = Math.hypot(sumX, sumY) < 1e-8 ? Math.PI : Math.atan2(-sumY, -sumX);
    final double rotation = angle + Math.PI - outgoing;
    final double x = destination.x() + 58 * Math.cos(angle);
    final double y = destination.y() + 58 * Math.sin(angle);
    final var placed =
        MoleculeDrawing.of(
            fragment.molecule(),
            fragment.atoms().stream()
                .map(
                    atom -> {
                      final double dx = atom.point2D().x() - anchor.x();
                      final double dy = atom.point2D().y() - anchor.y();
                      return atom.withPoint2D(
                          DrawingPoint2D.of(
                              x + dx * Math.cos(rotation) - dy * Math.sin(rotation),
                              y + dx * Math.sin(rotation) + dy * Math.cos(rotation)));
                    })
                .toList(),
            fragment.bonds(),
            fragment.manualFields());
    return new MoleculeSketchService()
        .translate(MoleculeDraftJoin.join(target, targetAtom, placed, attachmentAtom, type), 0, 0);
  }
}
