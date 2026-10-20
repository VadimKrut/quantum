/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Explicit stereochemical assignments and layout transforms; no inferred CIP priorities. */
public final class MoleculeStereoEditing {
  public static final String REVIEW = Stereochemistry.REVIEW_ATTRIBUTE;

  public enum Transform {
    CLOCKWISE,
    COUNTERCLOCKWISE,
    HALF_TURN,
    MIRROR_HORIZONTAL,
    MIRROR_VERTICAL
  }

  private MoleculeStereoEditing() {}

  public static MoleculeDrawing assign(
      final MoleculeDrawing drawing,
      final Stereocenter previous,
      final Stereocenter next,
      final boolean reviewed) {
    final var centers = new ArrayList<>(drawing.molecule().stereochemistry().centers());
    final int index = previous == null ? centers.size() : centers.indexOf(previous);
    if (index < 0) {
      throw new IllegalArgumentException("Стереозапись уже изменена или отсутствует.");
    }
    if (previous != null) {
      centers.remove(index);
    }
    if (centers.stream().anyMatch(center -> center.sameLocusAs(next))) {
      throw new IllegalArgumentException(
          "Для этого атома или оси уже есть запись. Выберите её в списке.");
    }
    validateAssignment(drawing.molecule(), next);
    centers.add(index, next);
    return replace(drawing, Stereochemistry.of(centers), reviewed);
  }

  public static MoleculeDrawing remove(
      final MoleculeDrawing drawing, final Stereocenter selected, final boolean reviewed) {
    final var centers = new ArrayList<>(drawing.molecule().stereochemistry().centers());
    if (!centers.remove(selected)) {
      throw new IllegalArgumentException("Выберите существующую стереозапись.");
    }
    return replace(drawing, Stereochemistry.of(centers), reviewed || centers.isEmpty());
  }

  private static MoleculeDrawing replace(
      final MoleculeDrawing drawing, final Stereochemistry stereo, final boolean reviewed) {
    final var source = drawing.molecule();
    final var attributes = new LinkedHashMap<>(source.metadata().attributes());
    if (reviewed) {
      attributes.remove(REVIEW);
    }
    final var metadata =
        ChemistryMetadata.of(source.metadata().source(), source.metadata().location(), attributes);
    final var molecule =
        Molecule.of(
            source.id(),
            source.displayName(),
            source.atoms(),
            source.bonds(),
            source.charge(),
            source.spinMultiplicity(),
            stereo,
            source.conformation(),
            source.opticalRotation(),
            source.symmetry(),
            metadata);
    return MoleculeDrawing.of(molecule, drawing.atoms(), drawing.bonds(), drawing.manualFields());
  }

  private static void validateAssignment(final Molecule molecule, final Stereocenter center) {
    if (center.kind() == StereocenterKind.TETRAHEDRAL_ATOM) {
      final var bonds =
          molecule.bonds().stream()
              .filter(
                  bond ->
                      bond.firstAtomId().equals(center.primaryAtomId())
                          || bond.secondAtomId().equals(center.primaryAtomId()))
              .toList();
      if (bonds.size() != 4 || bonds.stream().anyMatch(bond -> bond.type() != BondType.SINGLE)) {
        throw new IllegalArgumentException(
            "Для тетраэдрической записи нужны четыре явно заданных соседа с одинарными связями. При необходимости добавьте явный H.");
      }
    }
    // The molecule constructor validates descriptor families, reference atoms and graph
    // connectivity.
  }

  public static boolean chemicalStateChanged(final Molecule before, final Molecule after) {
    if (!before.charge().equals(after.charge())
        || !before.spinMultiplicity().equals(after.spinMultiplicity())
        || before.atoms().size() != after.atoms().size()
        || !bondKeys(before).equals(bondKeys(after))) {
      return true;
    }
    final Map<AtomId, Atom> old = new HashMap<>();
    before.atoms().forEach(atom -> old.put(atom.id(), atom));
    for (final var atom : after.atoms()) {
      final var previous = old.get(atom.id());
      if (previous == null
          || !previous.element().equals(atom.element())
          || !previous.formalCharge().equals(atom.formalCharge())
          || !Objects.equals(previous.isotope(), atom.isotope())
          || !previous.radicalState().equals(atom.radicalState())) {
        return true;
      }
    }
    return false;
  }

  private static Set<String> bondKeys(final Molecule molecule) {
    final Set<String> result = new HashSet<>();
    for (final var bond : molecule.bonds()) {
      final String first = bond.firstAtomId().value(), second = bond.secondAtomId().value();
      result.add(
          (first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first)
              + ":"
              + bond.type().name());
    }
    return result;
  }

  public static MoleculeDrawing confirmReviewed(final MoleculeDrawing drawing) {
    return replace(drawing, drawing.molecule().stereochemistry(), true);
  }

  /** Explicit cleanup within the selected component's existing position, not the insertion origin. */
  public static MoleculeDrawing relayout(final MoleculeDrawing drawing) {
    final var before = center(drawing);
    final var cleaned = relayout(drawing, 0, 0);
    final var after = center(cleaned);
    final double dx = before.x() - after.x(), dy = before.y() - after.y();
    return MoleculeDrawing.of(
        cleaned.molecule(),
        cleaned.atoms().stream()
            .map(
                atom ->
                    atom.withPoint2D(
                        DrawingPoint2D.of(atom.point2D().x() + dx, atom.point2D().y() + dy)))
            .toList(),
        cleaned.bonds(),
        cleaned.manualFields());
  }

  /** Re-layout is not a stereochemical depiction algorithm. Keep all manual styles and require visible review. */
  public static MoleculeDrawing relayout(
      final MoleculeDrawing drawing, final double x, final double y) {
    final var placement = new LaboratoryMoleculePlacement().place(drawing.molecule(), x, y);
    final Map<AtomId, DrawingPoint2D> points = new HashMap<>();
    placement.atoms().forEach(atom -> points.put(atom.atomId(), atom.point2D()));
    Molecule molecule = drawing.molecule();
    if (!molecule.stereochemistry().empty()
        || drawing.bonds().stream()
            .anyMatch(
                bond ->
                    bond.style() == BondDrawingStyle.WEDGE_UP
                        || bond.style() == BondDrawingStyle.WEDGE_DOWN
                        || bond.style() == BondDrawingStyle.WAVY)) {
      final var attributes = new LinkedHashMap<>(molecule.metadata().attributes());
      attributes.put(
          REVIEW,
          "2D-раскладка перестроена. Проверьте соответствие клиньев и расположения заместителей стереозаписям; конфигурация автоматически не вычислялась.");
      molecule =
          Molecule.of(
              molecule.id(),
              molecule.displayName(),
              molecule.atoms(),
              molecule.bonds(),
              molecule.charge(),
              molecule.spinMultiplicity(),
              molecule.stereochemistry(),
              molecule.conformation(),
              molecule.opticalRotation(),
              molecule.symmetry(),
              ChemistryMetadata.of(
                  molecule.metadata().source(), molecule.metadata().location(), attributes));
    }
    return MoleculeDrawing.of(
        molecule,
        drawing.atoms().stream().map(atom -> atom.withPoint2D(points.get(atom.atomId()))).toList(),
        drawing.bonds(),
        drawing.manualFields());
  }

  public static Molecule markChanged(final Molecule before, final Molecule after) {
    if (before.stereochemistry().empty() || !chemicalStateChanged(before, after)) {
      return after;
    }
    final var attributes = new LinkedHashMap<>(after.metadata().attributes());
    attributes.put(
        REVIEW,
        "Граф или электронное состояние изменены после назначения стереохимии. Проверьте конфигурации и приоритеты.");
    return Molecule.of(
        after.id(),
        after.displayName(),
        after.atoms(),
        after.bonds(),
        after.charge(),
        after.spinMultiplicity(),
        after.stereochemistry(),
        after.conformation(),
        after.opticalRotation(),
        after.symmetry(),
        ChemistryMetadata.of(after.metadata().source(), after.metadata().location(), attributes));
  }

  /** Rigid 2D rotation only: molecular graph, absolute stereo and 3D coordinates stay unchanged. */
  public static MoleculeDrawing rotate(final MoleculeDrawing drawing, final double radians) {
    if (!Double.isFinite(radians)) {
      throw new IllegalArgumentException("Угол поворота должен быть конечным.");
    }
    final var center = center(drawing);
    final double cos = Math.cos(radians), sin = Math.sin(radians);
    return MoleculeDrawing.of(
        drawing.molecule(),
        drawing.atoms().stream()
            .map(
                atom -> {
                  final double x = atom.point2D().x() - center.x(),
                      y = atom.point2D().y() - center.y();
                  return atom.withPoint2D(
                      DrawingPoint2D.of(
                          center.x() + x * cos - y * sin, center.y() + x * sin + y * cos));
                })
            .toList(),
        drawing.bonds(),
        drawing.manualFields());
  }

  public static DrawingPoint2D center(final MoleculeDrawing drawing) {
    final double
        minX = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().x()).min().orElse(0),
        maxX = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().x()).max().orElse(0);
    final double
        minY = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().y()).min().orElse(0),
        maxY = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().y()).max().orElse(0);
    return DrawingPoint2D.of((minX + maxX) / 2, (minY + maxY) / 2);
  }

  public static MoleculeDrawing transform(
      final MoleculeDrawing drawing, final Transform transform) {
    final double
        minX = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().x()).min().orElse(0),
        maxX = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().x()).max().orElse(0);
    final double
        minY = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().y()).min().orElse(0),
        maxY = drawing.atoms().stream().mapToDouble(atom -> atom.point2D().y()).max().orElse(0);
    final double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
    final List<AtomDrawing> positions =
        drawing.atoms().stream()
            .map(
                atom -> {
                  final double x = atom.point2D().x() - cx, y = atom.point2D().y() - cy;
                  final var point =
                      switch (transform) {
                        case CLOCKWISE -> DrawingPoint2D.of(cx - y, cy + x);
                        case COUNTERCLOCKWISE -> DrawingPoint2D.of(cx + y, cy - x);
                        case HALF_TURN -> DrawingPoint2D.of(cx - x, cy - y);
                        case MIRROR_HORIZONTAL -> DrawingPoint2D.of(cx - x, cy + y);
                        case MIRROR_VERTICAL -> DrawingPoint2D.of(cx + x, cy - y);
                      };
                  return atom.withPoint2D(point);
                })
            .toList();
    final boolean mirrored =
        transform == Transform.MIRROR_HORIZONTAL || transform == Transform.MIRROR_VERTICAL;
    final List<BondDrawing> bonds =
        drawing.bonds().stream()
            .map(
                bond ->
                    BondDrawing.of(
                        bond.firstAtomId(),
                        bond.secondAtomId(),
                        mirrored
                            ? switch (bond.style()) {
                              case WEDGE_UP -> BondDrawingStyle.WEDGE_DOWN;
                              case WEDGE_DOWN -> BondDrawingStyle.WEDGE_UP;
                              default -> bond.style();
                            }
                            : bond.style()))
            .toList();
    return MoleculeDrawing.of(drawing.molecule(), positions, bonds, drawing.manualFields());
  }
}
