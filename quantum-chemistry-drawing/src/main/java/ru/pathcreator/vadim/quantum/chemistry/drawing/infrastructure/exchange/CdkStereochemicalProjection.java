/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.util.*;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Disposable representative projection; original chemical and relative-group records remain untouched. */
public final class CdkStereochemicalProjection {
  private CdkStereochemicalProjection() {}

  public record Depiction(MoleculeDrawing drawing, String annotation) {}

  public static String annotation(final Molecule m) {
    return MolecularRelativeStereo.read(m)
        .map(
            r ->
                String.join(
                        "; ", r.groups().stream().map(g -> g.kind().toString()).distinct().toList())
                    + ". "
                    + (r.groups().stream()
                            .anyMatch(
                                g -> g.kind() != MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE)
                        ? "Для относительных групп показан представитель; абсолютная конфигурация не утверждается. "
                        : "")
                    + (r.groups().stream()
                            .anyMatch(
                                g -> g.kind() == MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE)
                        ? "Неопределённые центры показаны без абсолютных клиньев."
                        : ""))
        .orElse("")
        .strip();
  }

  static IAtomContainer representative(final MoleculeDrawing drawing) {
    final Molecule original = drawing.molecule();
    Molecule m = original;
    final var relative = MolecularRelativeStereo.read(original);
    if (relative.isPresent()) {
      relative.get().validate(original);
      if (relative.get().groups().stream()
          .anyMatch(g -> g.kind() == MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE)) {
        throw new IllegalArgumentException(
            "Абсолютная конфигурация не установлена: однозначный стереопредставитель не выбран.");
      }
      m = MolecularRelativeStereo.clear(original);
      final String saved = m.metadata().attributes().get("exchange.v1.signature");
      if (saved != null && saved.equals(MolecularIdentity.signature(original))) {
        final var attributes = new LinkedHashMap<>(m.metadata().attributes());
        attributes.put("exchange.v1.signature", MolecularIdentity.signature(m));
        m =
            Molecule.of(
                m.id(),
                m.displayName(),
                m.atoms(),
                m.bonds(),
                m.charge(),
                m.spinMultiplicity(),
                m.stereochemistry(),
                m.conformation(),
                m.opticalRotation(),
                m.symmetry(),
                ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), attributes));
      }
    }
    final var graph =
        CdkStereoIdentity.identityGraph(
            MoleculeDrawing.of(m, drawing.atoms(), drawing.bonds(), drawing.manualFields()));
    if (relative.isPresent()) {
      final var labels = CdkInitialGeometry.stereoLabels(graph);
      for (final var group : relative.get().groups()) {
        Boolean inverted = null;
        for (final var member : group.members()) {
          int index = -1;
          for (int i = 0; i < m.atomCount(); i++) {
            if (m.atoms().get(i).id().equals(member.atom())) {
              index = i;
              break;
            }
          }
          final String actual = labels.get("T:" + index),
              wanted = member.descriptor() == MolecularRelativeStereo.Descriptor.R_STAR ? "R" : "S";
          if (actual == null) {
            throw new IllegalArgumentException(
                "У стереогруппы нет проверяемого представителя. Задайте согласованную тетраэдрическую конфигурацию и рисунок.");
          }
          final boolean opposite = !actual.equals(wanted);
          if (inverted != null && inverted != opposite) {
            throw new IllegalArgumentException(
                "Сохранённый представитель не согласован с взаимной R*/S*-конфигурацией группы.");
          }
          inverted = opposite;
        }
      }
    }
    for (int i = 0; i < m.atomCount(); i++) {
      graph.getAtom(i).setID(m.atoms().get(i).id().value());
    }
    return graph;
  }

  public static Depiction depict(final Molecule molecule) {
    try {
      // Display may omit unknown orientations; the strict 3D/export representative above never
      // does.
      final Molecule display = displayMolecule(molecule);
      final var graph =
          representative(MoleculeDrawing.of(display, List.of(), List.of(), List.of()));
      final var expected = CdkInitialGeometry.stereoLabels(graph);
      new StructureDiagramGenerator().generateCoordinates(graph);
      final List<AtomDrawing> atoms = new ArrayList<>();
      final List<BondDrawing> bonds = new ArrayList<>();
      for (final var a : graph.atoms()) {
        final var p = a.getPoint2d();
        if (p == null || !Double.isFinite(p.x) || !Double.isFinite(p.y)) {
          throw new IllegalArgumentException("Не удалось построить полный стереорисунок.");
        }
        atoms.add(
            AtomDrawing.of(
                AtomId.of(a.getID()), DrawingPoint2D.of(p.x * 28, -p.y * 28), null, true));
      }
      for (final var b : graph.bonds()) {
        final var s = b.getStereo();
        final boolean reversed = s == IBond.Stereo.UP_INVERTED || s == IBond.Stereo.DOWN_INVERTED;
        bonds.add(
            BondDrawing.of(
                AtomId.of((reversed ? b.getEnd() : b.getBegin()).getID()),
                AtomId.of((reversed ? b.getBegin() : b.getEnd()).getID()),
                s == IBond.Stereo.UP || s == IBond.Stereo.UP_INVERTED
                    ? BondDrawingStyle.WEDGE_UP
                    : s == IBond.Stereo.DOWN || s == IBond.Stereo.DOWN_INVERTED
                        ? BondDrawingStyle.WEDGE_DOWN
                        : BondDrawingStyle.PLAIN));
      }
      final var drawing = MoleculeDrawing.of(molecule, atoms, bonds, List.of());
      final var checked = representative(MoleculeDrawing.of(display, atoms, bonds, List.of()));
      if (!expected.equals(CdkInitialGeometry.stereoLabels(checked))) {
        throw new IllegalArgumentException("Стереорисунок не подтвердил сохранённую конфигурацию.");
      }
      return new Depiction(drawing, annotation(molecule));
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException("Стереорисунок: " + e.getMessage(), e);
    }
  }

  private static Molecule displayMolecule(final Molecule original) {
    final var optional = MolecularRelativeStereo.read(original);
    if (optional.isEmpty()) {
      return original;
    }
    final var relative = optional.get();
    relative.validate(original);
    final Set<AtomId> unknown =
        relative.groups().stream()
            .filter(g -> g.kind() == MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE)
            .flatMap(g -> g.members().stream())
            .map(MolecularRelativeStereo.Member::atom)
            .collect(java.util.stream.Collectors.toSet());
    if (unknown.isEmpty()) {
      return original;
    }
    final var parity = MolecularStereoParity.read(original);
    final var clean = MolecularRelativeStereo.clear(original);
    final Map<String, String> attributes = new LinkedHashMap<>(clean.metadata().attributes());
    if (!parity.isEmpty()) {
      if (!MolecularIdentity.signature(original).equals(attributes.get("exchange.v1.signature"))
          && !MolecularIdentity.signature(clean).equals(attributes.get("exchange.v1.signature"))) {
        throw new IllegalArgumentException(
            "Сохранённая стереоконфигурация устарела; 2D-рисунок требует проверки.");
      }
      final String retained =
          parity.values().stream()
              .filter(p -> !unknown.contains(p.center()))
              .sorted(Comparator.comparing(p -> p.center().value()))
              .map(
                  p ->
                      p.center().value()
                          + ":"
                          + (p.sign() < 0 ? "ANTI_CLOCKWISE" : "CLOCKWISE")
                          + ":"
                          + String.join(",", p.references().stream().map(AtomId::value).toList()))
              .collect(java.util.stream.Collectors.joining(";"));
      if (retained.isEmpty()) {
        attributes.remove(Stereochemistry.ORDERED_PARITY_ATTRIBUTE);
      } else {
        attributes.put(Stereochemistry.ORDERED_PARITY_ATTRIBUTE, retained);
      }
    }
    final var centers =
        clean.stereochemistry().centers().stream()
            .filter(
                c ->
                    c.kind() != StereocenterKind.TETRAHEDRAL_ATOM
                        || !unknown.contains(c.primaryAtomId()))
            .toList();
    Molecule display =
        Molecule.of(
            clean.id(),
            clean.displayName(),
            clean.atoms(),
            clean.bonds(),
            clean.charge(),
            clean.spinMultiplicity(),
            Stereochemistry.of(centers),
            clean.conformation(),
            clean.opticalRotation(),
            clean.symmetry(),
            ChemistryMetadata.of(
                clean.metadata().source(), clean.metadata().location(), attributes));
    if (!parity.isEmpty()) {
      attributes.put("exchange.v1.signature", MolecularIdentity.signature(display));
      display =
          Molecule.of(
              display.id(),
              display.displayName(),
              display.atoms(),
              display.bonds(),
              display.charge(),
              display.spinMultiplicity(),
              display.stereochemistry(),
              display.conformation(),
              display.opticalRotation(),
              display.symmetry(),
              ChemistryMetadata.of(
                  display.metadata().source(), display.metadata().location(), attributes));
    }
    final var groups =
        relative.groups().stream()
            .filter(g -> g.kind() != MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE)
            .toList();
    if (!groups.isEmpty()) {
      display =
          new MolecularRelativeStereo(MolecularRelativeStereo.signature(display), groups)
              .apply(display);
      if (!parity.isEmpty()) {
        final var withRelative = new LinkedHashMap<>(display.metadata().attributes());
        withRelative.put("exchange.v1.signature", MolecularIdentity.signature(display));
        display =
            Molecule.of(
                display.id(),
                display.displayName(),
                display.atoms(),
                display.bonds(),
                display.charge(),
                display.spinMultiplicity(),
                display.stereochemistry(),
                display.conformation(),
                display.opticalRotation(),
                display.symmetry(),
                ChemistryMetadata.of(
                    display.metadata().source(), display.metadata().location(), withRelative));
      }
    }
    return display;
  }
}
