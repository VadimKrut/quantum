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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Immutable sketch commands operating directly on the existing chemical graph. */
public final class MoleculeSketchService {

  public MoleculeDrawing create(final boolean ring, final double x, final double y) {
    final ArrayList<Atom> atoms = new ArrayList<>();
    final ArrayList<AtomDrawing> positions = new ArrayList<>();
    final ArrayList<Bond> bonds = new ArrayList<>();
    final int count = ring ? 6 : 1;
    for (int index = 0; index < count; index++) {
      final AtomId id = AtomId.of("a" + index);
      atoms.add(Atom.of(id, ElementSymbol.of("C"), null));
      final double angle = Math.PI / 3 * index;
      positions.add(
          AtomDrawing.of(
              id,
              DrawingPoint2D.of(
                  x + (ring ? 48 * Math.cos(angle) : 0), y + (ring ? 48 * Math.sin(angle) : 0)),
              null,
              true));
      if (index > 0) {
        bonds.add(Bond.of(atoms.get(index - 1).id(), id, BondType.SINGLE));
      }
    }
    if (ring) {
      bonds.add(Bond.of(atoms.getLast().id(), atoms.getFirst().id(), BondType.SINGLE));
    }
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("m_" + UUID.randomUUID()),
            ring ? "Шестичленное кольцо" : "Новая структура",
            atoms,
            bonds,
            null,
            null);
    return MoleculeSkeletonHydrogens.normalize(
        MoleculeDrawing.of(molecule, positions, bondLayouts(bonds), List.of()));
  }

  public MoleculeDrawing ring(
      final int size, final boolean alternating, final double x, final double y) {
    if (size < 3 || size > 8 || alternating && size != 6) {
      throw new IllegalArgumentException("Неподдерживаемый шаблон кольца.");
    }
    final ArrayList<Atom> atoms = new ArrayList<>();
    final ArrayList<AtomDrawing> positions = new ArrayList<>();
    final ArrayList<Bond> bonds = new ArrayList<>();
    final double radius = 44 / (2 * Math.sin(Math.PI / size));
    for (int index = 0; index < size; index++) {
      final AtomId id = AtomId.of("a" + index);
      atoms.add(Atom.of(id, ElementSymbol.of("C"), null));
      final double angle = -Math.PI / 2 + 2 * Math.PI * index / size;
      positions.add(
          AtomDrawing.of(
              id,
              DrawingPoint2D.of(
                  Math.max(radius + 24, x) + radius * Math.cos(angle),
                  Math.max(radius + 24, y) + radius * Math.sin(angle)),
              null,
              true));
      bonds.add(
          Bond.of(
              id,
              AtomId.of("a" + (index + 1) % size),
              alternating && index % 2 == 0 ? BondType.DOUBLE : BondType.SINGLE));
    }
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("m_" + UUID.randomUUID()),
            "Кольцо · " + size + " атомов",
            atoms,
            bonds,
            null,
            null);
    return MoleculeSkeletonHydrogens.normalize(
        MoleculeDrawing.of(molecule, positions, bondLayouts(bonds), List.of()));
  }

  public MoleculeDrawing rename(final MoleculeDrawing drawing, final String name) {
    return rebuild(
        drawing,
        name,
        drawing.molecule().atoms(),
        drawing.molecule().bonds(),
        drawing.atoms(),
        drawing.bonds());
  }

  public MoleculeDrawing independentCopy(final MoleculeDrawing drawing) {
    final Molecule source = drawing.molecule();
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("m_" + UUID.randomUUID()),
            source.displayName(),
            source.atoms(),
            source.bonds(),
            source.charge(),
            source.spinMultiplicity(),
            source.stereochemistry(),
            source.conformation(),
            source.opticalRotation(),
            source.symmetry(),
            source.metadata());
    return MoleculeDrawing.of(molecule, drawing.atoms(), drawing.bonds(), drawing.manualFields());
  }

  public MoleculeDrawing move(
      final MoleculeDrawing drawing, final AtomId atomId, final double x, final double y) {
    final List<AtomDrawing> positions =
        drawing.atoms().stream()
            .map(
                atom ->
                    atom.atomId().equals(atomId) ? atom.withPoint2D(DrawingPoint2D.of(x, y)) : atom)
            .toList();
    return MoleculeDrawing.of(
        drawing.molecule(), positions, drawing.bonds(), drawing.manualFields());
  }

  public MoleculeDrawing changeAtom(
      final MoleculeDrawing drawing,
      final AtomId id,
      final String symbol,
      final int charge,
      final Integer isotope) {
    final List<Atom> atoms =
        drawing.molecule().atoms().stream()
            .map(
                atom ->
                    atom.id().equals(id)
                        ? MoleculeSkeletonHydrogens.manual(
                            Atom.of(
                                id,
                                ElementSymbol.of(symbol),
                                atom.coordinate(),
                                FormalCharge.of(charge),
                                isotope == null ? null : Isotope.of(isotope),
                                atom.radicalState(),
                                atom.metadata()))
                        : atom)
            .toList();
    return rebuild(
        drawing,
        drawing.molecule().displayName(),
        atoms,
        drawing.molecule().bonds(),
        drawing.atoms(),
        drawing.bonds());
  }

  public MoleculeDrawing spatialAtom(
      final MoleculeDrawing drawing,
      final AtomId id,
      final ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D coordinate,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState radical) {
    final List<Atom> atoms =
        drawing.molecule().atoms().stream()
            .map(
                atom ->
                    atom.id().equals(id)
                        ? MoleculeSkeletonHydrogens.manual(
                            Atom.of(
                                id,
                                atom.element().symbol(),
                                coordinate,
                                atom.formalCharge(),
                                atom.isotope(),
                                radical,
                                atom.metadata()))
                        : atom)
            .toList();
    return rebuild(
        drawing,
        drawing.molecule().displayName(),
        atoms,
        drawing.molecule().bonds(),
        drawing.atoms(),
        drawing.bonds());
  }

  public MoleculeDrawing extend(
      final MoleculeDrawing drawing,
      final AtomId from,
      final String symbol,
      final BondType type,
      final double x,
      final double y) {
    final AtomId id = AtomId.of("a_" + UUID.randomUUID());
    final ArrayList<Atom> atoms = new ArrayList<>(drawing.molecule().atoms());
    atoms.add(Atom.of(id, ElementSymbol.of(symbol), null));
    final ArrayList<AtomDrawing> positions = new ArrayList<>(drawing.atoms());
    positions.add(AtomDrawing.of(id, DrawingPoint2D.of(x, y), null, true));
    final ArrayList<Bond> bonds = new ArrayList<>(drawing.molecule().bonds());
    bonds.add(Bond.of(from, id, type));
    final ArrayList<BondDrawing> styles = new ArrayList<>(drawing.bonds());
    styles.add(BondDrawing.of(from, id, BondDrawingStyle.PLAIN));
    return rebuild(drawing, drawing.molecule().displayName(), atoms, bonds, positions, styles);
  }

  public MoleculeDrawing connect(
      final MoleculeDrawing drawing, final AtomId first, final AtomId second, final BondType type) {
    if (drawing.molecule().bonds().stream()
        .anyMatch(bond -> bond.connects(first, second) && bond.type() == type)) {
      return drawing;
    }
    final ArrayList<Bond> bonds = new ArrayList<>(drawing.molecule().bonds());
    bonds.removeIf(bond -> bond.connects(first, second));
    bonds.add(Bond.of(first, second, type));
    final ArrayList<BondDrawing> styles =
        new ArrayList<>(
            drawing.bonds().stream()
                .map(
                    style ->
                        style.connects(first, second)
                            ? BondDrawing.of(first, second, BondDrawingStyle.PLAIN)
                            : style)
                .toList());
    if (styles.stream().noneMatch(bond -> bond.connects(first, second))) {
      styles.add(BondDrawing.of(first, second, BondDrawingStyle.PLAIN));
    }
    return rebuild(
        drawing,
        drawing.molecule().displayName(),
        drawing.molecule().atoms(),
        bonds,
        drawing.atoms(),
        styles);
  }

  public MoleculeDrawing styleBond(
      final MoleculeDrawing drawing,
      final AtomId first,
      final AtomId second,
      final BondDrawingStyle style,
      final boolean reverse) {
    final Bond bond =
        drawing.molecule().bonds().stream()
            .filter(value -> value.connects(first, second))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Связь отсутствует."));
    if ((style == BondDrawingStyle.WEDGE_UP
            || style == BondDrawingStyle.WEDGE_DOWN
            || style == BondDrawingStyle.WAVY)
        && bond.type() != BondType.SINGLE) {
      throw new IllegalArgumentException(
          "Клин или волнистое начертание применяется к одинарной связи.");
    }
    final List<BondDrawing> styles =
        drawing.bonds().stream()
            .map(
                value ->
                    !value.connects(first, second)
                        ? value
                        : BondDrawing.of(
                            reverse ? value.secondAtomId() : value.firstAtomId(),
                            reverse ? value.firstAtomId() : value.secondAtomId(),
                            style))
            .toList();
    return MoleculeDrawing.of(drawing.molecule(), drawing.atoms(), styles, drawing.manualFields());
  }

  public MoleculeDrawing translate(
      final MoleculeDrawing drawing, final double dx, final double dy) {
    final double shiftX =
        Math.max(
            dx,
            24 - drawing.atoms().stream().mapToDouble(atom -> atom.point2D().x()).min().orElse(24));
    final double shiftY =
        Math.max(
            dy,
            24 - drawing.atoms().stream().mapToDouble(atom -> atom.point2D().y()).min().orElse(24));
    return MoleculeDrawing.of(
        drawing.molecule(),
        drawing.atoms().stream()
            .map(
                atom ->
                    atom.withPoint2D(
                        DrawingPoint2D.of(
                            atom.point2D().x() + shiftX, atom.point2D().y() + shiftY)))
            .toList(),
        drawing.bonds(),
        drawing.manualFields());
  }

  public MoleculeDrawing disconnect(
      final MoleculeDrawing drawing, final AtomId first, final AtomId second) {
    return rebuild(
        drawing,
        drawing.molecule().displayName(),
        drawing.molecule().atoms(),
        drawing.molecule().bonds().stream().filter(bond -> !bond.connects(first, second)).toList(),
        drawing.atoms(),
        drawing.bonds().stream().filter(bond -> !bond.connects(first, second)).toList());
  }

  public java.util.Set<AtomId> connected(final MoleculeDrawing drawing, final AtomId start) {
    if (drawing.molecule().atoms().stream().noneMatch(atom -> atom.id().equals(start))) {
      throw new IllegalArgumentException("Атом отсутствует в структуре.");
    }
    final java.util.LinkedHashSet<AtomId> selected = new java.util.LinkedHashSet<>();
    final java.util.ArrayDeque<AtomId> pending = new java.util.ArrayDeque<>();
    pending.add(start);
    while (!pending.isEmpty()) {
      final AtomId current = pending.removeFirst();
      if (!selected.add(current)) {
        continue;
      }
      for (final Bond bond : drawing.molecule().bonds()) {
        if (bond.firstAtomId().equals(current)) {
          pending.add(bond.secondAtomId());
        } else if (bond.secondAtomId().equals(current)) {
          pending.add(bond.firstAtomId());
        }
      }
    }
    return java.util.Set.copyOf(selected);
  }

  public MoleculeDrawing translateAtoms(
      final MoleculeDrawing drawing,
      final java.util.Set<AtomId> selected,
      final double dx,
      final double dy) {
    return MoleculeDrawing.of(
        drawing.molecule(),
        drawing.atoms().stream()
            .map(
                atom ->
                    selected.contains(atom.atomId())
                        ? atom.withPoint2D(
                            DrawingPoint2D.of(atom.point2D().x() + dx, atom.point2D().y() + dy))
                        : atom)
            .toList(),
        drawing.bonds(),
        drawing.manualFields());
  }

  public MoleculeDrawing molecularState(
      final MoleculeDrawing drawing, final int charge, final int multiplicity) {
    final Molecule source = drawing.molecule();
    final Molecule changed =
        Molecule.of(
            source.id(),
            source.displayName(),
            source.atoms(),
            source.bonds(),
            ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge.of(charge),
            ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity.of(
                multiplicity),
            source.stereochemistry(),
            source.conformation(),
            source.opticalRotation(),
            source.symmetry(),
            source.metadata());
    return MoleculeSkeletonHydrogens.ifManaged(
        MoleculeDrawing.of(
            MoleculeStereoEditing.markChanged(source, changed),
            drawing.atoms(),
            drawing.bonds(),
            drawing.manualFields()));
  }

  public MoleculeDrawing fragment(
      final MoleculeDrawing drawing, final java.util.Set<AtomId> selected) {
    final List<Atom> atoms =
        drawing.molecule().atoms().stream()
            .filter(atom -> selected.contains(atom.id()))
            .map(
                atom ->
                    MoleculeSkeletonHydrogens.automatic(atom)
                            && drawing.molecule().bonds().stream()
                                .noneMatch(
                                    bond ->
                                        (bond.firstAtomId().equals(atom.id())
                                                && selected.contains(bond.secondAtomId()))
                                            || (bond.secondAtomId().equals(atom.id())
                                                && selected.contains(bond.firstAtomId())))
                        ? MoleculeSkeletonHydrogens.manual(atom)
                        : atom)
            .toList();
    if (atoms.isEmpty() || atoms.size() != selected.size()) {
      throw new IllegalArgumentException("Выделите атомы существующей структуры.");
    }
    if (atoms.size() == drawing.atoms().size()) {
      return this.independentCopy(drawing);
    }
    final List<Bond> bonds =
        drawing.molecule().bonds().stream()
            .filter(
                bond ->
                    selected.contains(bond.firstAtomId()) && selected.contains(bond.secondAtomId()))
            .toList();
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("m_" + UUID.randomUUID()),
            drawing.molecule().displayName() + " — фрагмент",
            atoms,
            bonds,
            ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge.of(
                atoms.stream().mapToInt(atom -> atom.formalCharge().value()).sum()),
            ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity.SINGLET,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.of(
                null,
                null,
                java.util.Map.of(
                    "laboratory.fragment",
                    "partial",
                    "laboratory.fragment.source",
                    drawing.moleculeId().value())));
    final var styles =
        drawing.bonds().stream()
            .filter(
                bond ->
                    selected.contains(bond.firstAtomId()) && selected.contains(bond.secondAtomId()))
            .toList();
    final var centers =
        new ArrayList<ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter>();
    for (final var center : drawing.molecule().stereochemistry().centers()) {
      if (!selected.contains(center.primaryAtomId())
          || center.hasSecondaryAtom() && !selected.contains(center.secondaryAtomId())
          || !selected.containsAll(center.referenceAtomIds())) {
        continue;
      }
      try {
        // Axial and helical records also need the retained connecting path, not just reference IDs.
        Molecule.of(
            molecule.id(),
            molecule.displayName(),
            atoms,
            bonds,
            molecule.charge(),
            molecule.spinMultiplicity(),
            ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry.of(
                List.of(center)),
            molecule.metadata());
        centers.add(center);
      } catch (final IllegalArgumentException invalidatedCenter) {
        // The review message below records the number excluded by cutting the source graph.
      }
    }
    final var attributes = new java.util.LinkedHashMap<>(molecule.metadata().attributes());
    if (!drawing.molecule().stereochemistry().empty()
        || styles.stream()
            .anyMatch(
                style ->
                    style.style() == BondDrawingStyle.WEDGE_UP
                        || style.style() == BondDrawingStyle.WEDGE_DOWN
                        || style.style() == BondDrawingStyle.WAVY)) {
      attributes.put(
          MoleculeStereoEditing.REVIEW,
          "Скопирована часть структуры. Проверьте конфигурации и приоритеты. Сохранено стереозаписей: "
              + centers.size()
              + "; исключено записей с отсутствующими атомами или связями: "
              + (drawing.molecule().stereochemistry().centers().size() - centers.size())
              + ".");
    }
    final Molecule fragment =
        Molecule.of(
            molecule.id(),
            molecule.displayName(),
            atoms,
            bonds,
            molecule.charge(),
            molecule.spinMultiplicity(),
            ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry.of(centers),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.of(
                null, null, attributes));
    final var result =
        MoleculeDrawing.of(
            fragment,
            drawing.atoms().stream().filter(atom -> selected.contains(atom.atomId())).toList(),
            styles,
            List.of());
    return MoleculeSkeletonHydrogens.managed(drawing.molecule())
        ? MoleculeSkeletonHydrogens.normalize(result)
        : result;
  }

  public MoleculeDrawing removeAtom(final MoleculeDrawing drawing, final AtomId id) {
    return this.removeAtoms(drawing, java.util.Set.of(id));
  }

  public MoleculeDrawing removeAtoms(
      final MoleculeDrawing drawing, final java.util.Set<AtomId> ids) {
    final List<Atom> atoms =
        drawing.molecule().atoms().stream().filter(atom -> !ids.contains(atom.id())).toList();
    final List<Bond> bonds =
        drawing.molecule().bonds().stream()
            .filter(bond -> !ids.contains(bond.firstAtomId()) && !ids.contains(bond.secondAtomId()))
            .toList();
    final List<AtomDrawing> positions =
        drawing.atoms().stream().filter(atom -> !ids.contains(atom.atomId())).toList();
    final List<BondDrawing> styles =
        drawing.bonds().stream()
            .filter(bond -> !ids.contains(bond.firstAtomId()) && !ids.contains(bond.secondAtomId()))
            .toList();
    return rebuild(drawing, drawing.molecule().displayName(), atoms, bonds, positions, styles);
  }

  private static MoleculeDrawing rebuild(
      final MoleculeDrawing drawing,
      final String name,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final List<AtomDrawing> positions,
      final List<BondDrawing> styles) {
    final Molecule source = drawing.molecule();
    // Core validation rejects edits invalidating existing stereocentres instead of erasing them.
    final Molecule molecule =
        Molecule.of(
            source.id(),
            name,
            atoms,
            bonds,
            source.charge(),
            source.spinMultiplicity(),
            source.stereochemistry(),
            source.conformation(),
            source.opticalRotation(),
            source.symmetry(),
            source.metadata());
    return MoleculeSkeletonHydrogens.ifManaged(
        MoleculeDrawing.of(
            MoleculeStereoEditing.markChanged(source, molecule),
            positions,
            styles,
            drawing.manualFields()));
  }

  public MoleculeDrawing reviseGraph(
      final MoleculeDrawing drawing,
      final String name,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final List<AtomDrawing> positions,
      final List<BondDrawing> styles,
      final int charge,
      final int multiplicity) {
    return this.molecularState(
        rebuild(drawing, name, atoms, bonds, positions, styles), charge, multiplicity);
  }

  private static List<BondDrawing> bondLayouts(final List<Bond> bonds) {
    return bonds.stream()
        .map(
            bond -> BondDrawing.of(bond.firstAtomId(), bond.secondAtomId(), BondDrawingStyle.PLAIN))
        .toList();
  }
}
