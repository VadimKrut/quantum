/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Skeletal-editing hydrogens are explicit atoms; only unmodified owned H may be reconciled. */
public final class MoleculeSkeletonHydrogens {
  private static final String MODE = "drawing.skeletal-hydrogens.v1";
  private static final String AUTO = "drawing.skeletal-hydrogen.v1";
  private static final String NOTICE = "drawing.skeletal-hydrogens.notice";
  private static final String UNSUPPORTED = "drawing.skeletal-hydrogens.unsupported";
  private static final int MAX_ATOMS = 4096;

  private MoleculeSkeletonHydrogens() {}

  public static boolean managed(final Molecule molecule) {
    return "managed".equals(molecule.metadata().attributes().get(MODE));
  }

  public static boolean automatic(final Atom atom) {
    return atom.element().symbol().value().equals("H")
        && "automatic".equals(atom.metadata().attributes().get(AUTO));
  }

  public static AtomId lastEditableAtom(final MoleculeDrawing drawing) {
    return drawing.molecule().atoms().stream()
        .filter(atom -> !automatic(atom))
        .reduce((a, b) -> b)
        .orElseThrow(() -> new IllegalArgumentException("Нет редактируемого атома."))
        .id();
  }

  public static List<String> diagnostics(final MoleculeDrawing drawing) {
    final String value = drawing.molecule().metadata().attributes().get(NOTICE);
    return value == null ? List.of() : List.of(value);
  }

  public static int unsupportedCount(final MoleculeDrawing drawing) {
    try {
      return Integer.parseInt(
          drawing.molecule().metadata().attributes().getOrDefault(UNSUPPORTED, "0"));
    } catch (final NumberFormatException invalid) {
      return 0;
    }
  }

  static Atom manual(final Atom atom) {
    if (!atom.metadata().attributes().containsKey(AUTO)) {
      return atom;
    }
    final var attributes = new LinkedHashMap<>(atom.metadata().attributes());
    attributes.remove(AUTO);
    return Atom.of(
        atom.id(),
        atom.element().symbol(),
        atom.coordinate(),
        atom.formalCharge(),
        atom.isotope(),
        atom.radicalState(),
        ChemistryMetadata.of(atom.metadata().source(), atom.metadata().location(), attributes));
  }

  static MoleculeDrawing ifManaged(final MoleculeDrawing drawing) {
    return managed(drawing.molecule()) ? normalize(drawing) : drawing;
  }

  /** Explicit opt-in for old drawings. Existing manual/isotope/stereo H and scientific metadata remain intact. */
  public static MoleculeDrawing normalize(final MoleculeDrawing drawing) {
    Objects.requireNonNull(drawing);
    final Molecule source = drawing.molecule();
    if (source.atoms().size() > MAX_ATOMS) {
      throw new IllegalArgumentException("Автоматический учёт H ограничен 4096 атомами.");
    }
    final Map<AtomId, Atom> atoms = new LinkedHashMap<>();
    final Map<AtomId, AtomDrawing> positions = new LinkedHashMap<>();
    final Map<AtomId, List<Bond>> incident = new LinkedHashMap<>();
    for (final Atom atom : source.atoms()) {
      atoms.put(atom.id(), atom);
      incident.put(atom.id(), new ArrayList<>());
    }
    for (final AtomDrawing atom : drawing.atoms()) {
      positions.put(atom.atomId(), atom);
    }
    for (final Bond bond : source.bonds()) {
      incident.get(bond.firstAtomId()).add(bond);
      incident.get(bond.secondAtomId()).add(bond);
    }
    final Set<AtomId> protectedAtoms = new HashSet<>();
    source
        .stereochemistry()
        .centers()
        .forEach(
            center -> {
              protectedAtoms.add(center.primaryAtomId());
              if (center.hasSecondaryAtom()) {
                protectedAtoms.add(center.secondaryAtomId());
              }
              protectedAtoms.addAll(center.referenceAtomIds());
            });
    source
        .conformation()
        .torsionAngles()
        .forEach(
            t -> {
              protectedAtoms.add(t.firstAtomId());
              protectedAtoms.add(t.secondAtomId());
              protectedAtoms.add(t.thirdAtomId());
              protectedAtoms.add(t.fourthAtomId());
            });
    final Set<AtomId> stereoAnchors = new HashSet<>();
    MolecularRelativeStereo.read(source)
        .ifPresent(relative -> stereoAnchors.addAll(relative.atoms()));
    for (final BondDrawing style : drawing.bonds()) {
      if (style.style() == BondDrawingStyle.WEDGE_UP
          || style.style() == BondDrawingStyle.WEDGE_DOWN
          || style.style() == BondDrawingStyle.WAVY) {
        stereoAnchors.add(style.firstAtomId());
        stereoAnchors.add(style.secondAtomId());
      }
    }
    for (final AtomId anchor : stereoAnchors) {
      for (final Bond bond : incident.getOrDefault(anchor, List.of())) {
        protectedAtoms.add(other(bond, anchor));
      }
    }
    final Map<AtomId, List<AtomId>> owned = new LinkedHashMap<>();
    final Set<AtomId> removable = new HashSet<>(), remove = new HashSet<>();
    for (final Atom atom : source.atoms()) {
      final var edges = incident.get(atom.id());
      if (!automatic(atom)
          || atom.hasIsotope()
          || atom.hasCoordinate()
          || atom.formalCharge().value() != 0
          || atom.radicalState().radical()
          || protectedAtoms.contains(atom.id())) {
        continue;
      }
      if (edges.isEmpty()) {
        remove.add(atom.id());
        continue;
      }
      if (edges.size() != 1 || edges.getFirst().type() != BondType.SINGLE) {
        continue;
      }
      final Bond edge = edges.getFirst();
      final AtomId anchor = other(edge, atom.id());
      if (automatic(atoms.get(anchor))
          || drawing.bonds().stream()
              .anyMatch(
                  style ->
                      style.connects(atom.id(), anchor)
                          && style.style() != BondDrawingStyle.PLAIN)) {
        continue;
      }
      removable.add(atom.id());
      owned.computeIfAbsent(anchor, key -> new ArrayList<>()).add(atom.id());
    }
    final List<Atom> addedAtoms = new ArrayList<>();
    final List<AtomDrawing> addedPositions = new ArrayList<>();
    final List<Bond> addedBonds = new ArrayList<>();
    final List<BondDrawing> addedStyles = new ArrayList<>();
    final List<String> problems = new ArrayList<>();
    int unsupported = 0;
    final boolean unresolvedMolecularState =
        source.spinMultiplicity().value() != 1
            || source.charge().value()
                != source.atoms().stream().mapToInt(atom -> atom.formalCharge().value()).sum();
    for (final Atom atom : source.atoms()) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException("Учёт водородов отменён.");
      }
      if (automatic(atom)) {
        continue;
      }
      final var existing = owned.getOrDefault(atom.id(), List.of());
      if (atom.element().symbol().value().equals("H")) {
        remove.addAll(existing);
        continue;
      }
      final int normal =
          switch (atom.element().symbol().value()) {
            case "C", "Si" -> 4;
            case "B", "N" -> 3;
            case "O", "S" -> 2;
            case "F", "Cl", "Br", "I" -> 1;
            default -> -1;
          };
      int used = 0;
      boolean ordinary = true;
      for (final Bond bond : incident.get(atom.id())) {
        if (removable.contains(other(bond, atom.id()))) {
          continue;
        }
        final int order =
            switch (bond.type()) {
              case SINGLE -> 1;
              case DOUBLE -> 2;
              case TRIPLE -> 3;
              default -> -1;
            };
        if (order < 0) {
          ordinary = false;
        } else {
          used += order;
        }
      }
      if (unresolvedMolecularState
          || atom.formalCharge().value() != 0
          || atom.radicalState().radical()
          || normal < 0
          || !ordinary
          || used > normal) {
        unsupported++;
        if (problems.size() < 8) {
          problems.add(atom.element().symbol().value() + " (" + atom.id().value() + ")");
        }
        continue;
      }
      final int required = normal - used;
      // Replace H facing a new drawn substituent first, so surviving H do not lie on its bond.
      final var retained =
          existing.stream()
              .sorted(
                  Comparator.comparingDouble(
                      id -> crowding(id, atom.id(), incident.get(atom.id()), positions, removable)))
              .toList();
      for (int i = required; i < retained.size(); i++) {
        remove.add(retained.get(i));
      }
      for (int i = existing.size(); i < required; i++) {
        if (source.atoms().size() - remove.size() + addedAtoms.size() >= MAX_ATOMS) {
          throw new IllegalArgumentException("После дополнения H структура превысит 4096 атомов.");
        }
        final AtomId id = AtomId.of("h_auto_" + UUID.randomUUID());
        addedAtoms.add(
            Atom.of(
                id,
                ElementSymbol.of("H"),
                null,
                FormalCharge.NEUTRAL,
                null,
                RadicalState.CLOSED_SHELL,
                ChemistryMetadata.of(null, null, Map.of(AUTO, "automatic"))));
        final DrawingPoint2D point =
            position(positions.get(atom.id()).point2D(), drawing.atoms(), addedPositions, remove);
        addedPositions.add(AtomDrawing.of(id, point, null, true));
        addedBonds.add(Bond.of(atom.id(), id, BondType.SINGLE));
        addedStyles.add(BondDrawing.of(atom.id(), id, BondDrawingStyle.PLAIN));
      }
    }
    final var resultAtoms =
        new ArrayList<>(
            source.atoms().stream().filter(atom -> !remove.contains(atom.id())).toList());
    resultAtoms.addAll(addedAtoms);
    final var resultBonds =
        new ArrayList<>(
            source.bonds().stream()
                .filter(
                    bond ->
                        !remove.contains(bond.firstAtomId())
                            && !remove.contains(bond.secondAtomId()))
                .toList());
    resultBonds.addAll(addedBonds);
    final var resultPositions =
        new ArrayList<>(
            drawing.atoms().stream().filter(atom -> !remove.contains(atom.atomId())).toList());
    resultPositions.addAll(addedPositions);
    final var resultStyles =
        new ArrayList<>(
            drawing.bonds().stream()
                .filter(
                    bond ->
                        !remove.contains(bond.firstAtomId())
                            && !remove.contains(bond.secondAtomId()))
                .toList());
    resultStyles.addAll(addedStyles);
    final var attributes = new LinkedHashMap<>(source.metadata().attributes());
    attributes.put(MODE, "managed");
    attributes.remove(NOTICE);
    attributes.remove(UNSUPPORTED);
    if (unsupported > 0) {
      attributes.put(UNSUPPORTED, Integer.toString(unsupported));
      attributes.put(
          NOTICE,
          "H не пересчитаны для "
              + unsupported
              + " атомов: "
              + String.join(", ", problems)
              + (unsupported > problems.size() ? "…" : "")
              + ". Проверьте заряд, радикал, тип связи и валентность; имеющиеся явные H сохранены.");
    }
    final Molecule molecule =
        Molecule.of(
            source.id(),
            source.displayName(),
            resultAtoms,
            resultBonds,
            source.charge(),
            source.spinMultiplicity(),
            source.stereochemistry(),
            source.conformation(),
            source.opticalRotation(),
            source.symmetry(),
            ChemistryMetadata.of(
                source.metadata().source(), source.metadata().location(), attributes));
    final var result =
        MoleculeDrawing.of(
            MoleculeStereoEditing.markChanged(source, molecule),
            resultPositions,
            resultStyles,
            drawing.manualFields());
    return result.equals(drawing) ? drawing : result;
  }

  private static double crowding(
      final AtomId hydrogen,
      final AtomId anchor,
      final List<Bond> bonds,
      final Map<AtomId, AtomDrawing> positions,
      final Set<AtomId> automatic) {
    final var center = positions.get(anchor).point2D();
    final var h = positions.get(hydrogen).point2D();
    double maximum = -1;
    final double hx = h.x() - center.x(),
        hy = h.y() - center.y(),
        length = Math.max(1e-9, Math.hypot(hx, hy));
    for (final Bond bond : bonds) {
      final AtomId target = other(bond, anchor);
      if (automatic.contains(target)) {
        continue;
      }
      final var p = positions.get(target).point2D();
      final double dx = p.x() - center.x(), dy = p.y() - center.y();
      maximum =
          Math.max(maximum, (hx * dx + hy * dy) / (length * Math.max(1e-9, Math.hypot(dx, dy))));
    }
    return maximum;
  }

  private static AtomId other(final Bond bond, final AtomId id) {
    return bond.firstAtomId().equals(id) ? bond.secondAtomId() : bond.firstAtomId();
  }

  private static DrawingPoint2D position(
      final DrawingPoint2D center,
      final List<AtomDrawing> existing,
      final List<AtomDrawing> added,
      final Set<AtomId> removed) {
    double best = -Double.MAX_VALUE;
    DrawingPoint2D result = center;
    for (int step = 0; step < 12; step++) {
      final double angle = step * Math.PI / 6,
          x = center.x() + 28 * Math.cos(angle),
          y = center.y() + 28 * Math.sin(angle);
      double score = 0;
      for (final var atom : existing) {
        if (!removed.contains(atom.atomId())) {
          final var p = atom.point2D();
          score -= 1 / Math.max(1, Math.pow(p.x() - x, 2) + Math.pow(p.y() - y, 2));
        }
      }
      for (final var atom : added) {
        final var p = atom.point2D();
        score -= 1 / Math.max(1, Math.pow(p.x() - x, 2) + Math.pow(p.y() - y, 2));
      }
      if (score > best) {
        best = score;
        result = DrawingPoint2D.of(x, y);
      }
    }
    return result;
  }
}
