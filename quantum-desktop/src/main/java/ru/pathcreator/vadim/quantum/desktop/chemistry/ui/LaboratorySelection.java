/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryFragment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeAbbreviations;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** Selection is UI state; every multi-component mutation is a single editor transaction. */
final class LaboratorySelection {
  private final LaboratoryEditor editor;
  private final MoleculeSketchService sketch = new MoleculeSketchService();
  private final LinkedHashSet<LaboratoryCanvas.Hit> hits = new LinkedHashSet<>();
  private int pasteCount;
  private String pastedText;
  private boolean compactHydrogens;

  LaboratorySelection(final LaboratoryEditor editor) {
    this.editor = editor;
  }

  void compactHydrogens(final boolean enabled) {
    this.compactHydrogens = enabled;
  }

  Set<LaboratoryCanvas.Hit> hits() {
    return java.util.Collections.unmodifiableSet(this.hits);
  }

  LaboratoryCanvas.Hit first() {
    return this.hits.stream().findFirst().orElse(null);
  }

  void sync(final LaboratoryCanvas.Hit primary) {
    this.hits.removeIf(
        hit ->
            !this.editor.document().layouts().containsKey(hit.componentId())
                || this.editor.document().layouts().get(hit.componentId()).atoms().stream()
                    .noneMatch(atom -> atom.atomId().equals(hit.atomId())));
    if (primary == null) {
      this.hits.clear();
    } else if (!this.hits.contains(primary)) {
      this.hits.clear();
      this.hits.add(primary);
    }
  }

  LaboratoryCanvas.Hit gesture(final LaboratoryCanvas.Gesture gesture, final boolean connected) {
    if (gesture.region() != null) {
      if (!gesture.additive()) {
        this.hits.clear();
      }
      this.hits.addAll(gesture.region());
      return this.first();
    }
    final var hit = gesture.hit();
    if (hit == null) {
      if (!gesture.additive()) {
        this.hits.clear();
      }
      return this.first();
    }
    final LinkedHashSet<LaboratoryCanvas.Hit> group = new LinkedHashSet<>();
    if (connected) {
      this.sketch
          .connected(this.editor.document().layouts().get(hit.componentId()), hit.atomId())
          .forEach(id -> group.add(new LaboratoryCanvas.Hit(hit.componentId(), id)));
    } else {
      group.add(hit);
    }
    MoleculeAbbreviations.selection(
            this.editor.document().layouts().get(hit.componentId()),
            group.stream()
                .map(LaboratoryCanvas.Hit::atomId)
                .collect(java.util.stream.Collectors.toSet()))
        .forEach(id -> group.add(new LaboratoryCanvas.Hit(hit.componentId(), id)));
    if (gesture.moved()) {
      if (!this.hits.contains(hit)) {
        if (!gesture.additive()) {
          this.hits.clear();
        }
        this.hits.addAll(group);
      } else if (connected) {
        this.hits.addAll(group);
      }
      this.move(gesture.dx(), gesture.dy());
    } else if (gesture.additive()) {
      if (this.hits.containsAll(group)) {
        this.hits.removeAll(group);
      } else {
        this.hits.addAll(group);
      }
    } else {
      this.hits.clear();
      this.hits.addAll(group);
    }
    return this.hits.contains(hit) ? hit : this.first();
  }

  LaboratoryCanvas.Hit all(final String componentId) {
    this.hits.clear();
    this.editor.document().experiment().components().stream()
        .filter(component -> componentId == null || component.id().equals(componentId))
        .forEach(
            component ->
                component
                    .molecule()
                    .atoms()
                    .forEach(
                        atom ->
                            this.hits.add(new LaboratoryCanvas.Hit(component.id(), atom.id()))));
    return this.first();
  }

  LaboratoryCanvas.Hit connected(final String componentId, final AtomId atomId) {
    this.hits.clear();
    this.sketch
        .connected(this.editor.document().layouts().get(componentId), atomId)
        .forEach(id -> this.hits.add(new LaboratoryCanvas.Hit(componentId, id)));
    return this.first();
  }

  void move(final double dx, final double dy) {
    if (this.hits.isEmpty()) {
      return;
    }
    final var groups = this.groups();
    final Map<String, MoleculeDrawing> replacements = new LinkedHashMap<>();
    groups.forEach(
        (id, atoms) ->
            replacements.put(
                id,
                this.sketch.translateAtoms(
                    this.editor.document().layouts().get(id), atoms, dx, dy)));
    this.editor.editComponents(replacements, Set.of());
  }

  void remove() {
    final Map<String, MoleculeDrawing> replacements = new LinkedHashMap<>();
    final Set<String> removed = new LinkedHashSet<>();
    this.groups()
        .forEach(
            (id, atoms) -> {
              final var drawing = this.editor.document().layouts().get(id);
              if (atoms.size() == drawing.atoms().size()) {
                removed.add(id);
              } else {
                replacements.put(id, this.sketch.removeAtoms(drawing, atoms));
              }
            });
    this.editor.editComponents(replacements, removed);
    this.hits.clear();
  }

  void copy() {
    if (this.hits.isEmpty()) {
      return;
    }
    final LaboratoryEditor clipboard = new LaboratoryEditor();
    this.groups()
        .forEach(
            (id, atoms) -> {
              final var component =
                  this.editor.document().experiment().components().stream()
                      .filter(item -> item.id().equals(id))
                      .findFirst()
                      .orElseThrow();
              final var fragment =
                  this.sketch.fragment(this.editor.document().layouts().get(id), atoms);
              clipboard.add(
                  fragment,
                  component.role(),
                  atoms.size() == component.molecule().atoms().size() ? component.planned() : null);
            });
    final ClipboardContent content = new ClipboardContent();
    content.putString(new LaboratoryFileCodec().write(clipboard.document()));
    if (!Clipboard.getSystemClipboard().setContent(content)) {
      throw new IllegalStateException("Не удалось записать фрагмент в буфер обмена.");
    }
    this.pasteCount = 0;
  }

  LaboratoryCanvas.Hit paste(final double centerX, final double centerY) {
    return this.paste(Clipboard.getSystemClipboard().getString(), centerX, centerY);
  }

  LaboratoryCanvas.Hit paste(final String text, final double centerX, final double centerY) {
    if (text == null
        || !text.stripLeading().startsWith("QCHEM")
        || text.length() > 16 * 1024 * 1024) {
      throw new IllegalArgumentException(
          "Буфер обмена должен содержать фрагмент QCHEM, скопированный из Studio.");
    }
    if (!Double.isFinite(centerX) || !Double.isFinite(centerY)) {
      throw new IllegalArgumentException("Не удалось определить место вставки.");
    }
    final var copied = new LaboratoryFileCodec().read(text);
    final var atoms =
        copied.layouts().values().stream().flatMap(layout -> layout.atoms().stream()).toList();
    if (atoms.isEmpty()) {
      throw new IllegalArgumentException("В буфере обмена нет атомов для вставки.");
    }
    final double minX = atoms.stream().mapToDouble(atom -> atom.point2D().x()).min().orElseThrow();
    final double maxX = atoms.stream().mapToDouble(atom -> atom.point2D().x()).max().orElseThrow();
    final double minY = atoms.stream().mapToDouble(atom -> atom.point2D().y()).min().orElseThrow();
    final double maxY = atoms.stream().mapToDouble(atom -> atom.point2D().y()).max().orElseThrow();
    final int repetition = text.equals(this.pastedText) ? this.pasteCount : 0;
    final double offset = 36.0 * repetition;
    final double[] placement =
        this.freePlacement(
            copied.layouts().values(),
            centerX + offset - (minX + maxX) / 2,
            centerY + offset - (minY + maxY) / 2);
    final double dx = placement[0], dy = placement[1];
    final ArrayList<LaboratoryFragment> fragments = new ArrayList<>();
    for (final var component : copied.experiment().components()) {
      final var drawing = copied.layouts().get(component.id());
      final var ids =
          drawing.atoms().stream()
              .map(atom -> atom.atomId())
              .collect(java.util.stream.Collectors.toSet());
      fragments.add(
          new LaboratoryFragment(
              this.sketch.translateAtoms(drawing, ids, dx, dy),
              component.role(),
              component.planned()));
    }
    final List<String> added = this.editor.paste(fragments);
    this.hits.clear();
    this.pastedText = text;
    this.pasteCount = repetition + 1;
    for (final String id : added) {
      this.editor
          .document()
          .layouts()
          .get(id)
          .atoms()
          .forEach(atom -> this.hits.add(new LaboratoryCanvas.Hit(id, atom.atomId())));
    }
    return this.first();
  }

  /** Move the copied group as a unit; prefer a nearby gap instead of covering existing structures. */
  private double[] freePlacement(
      final java.util.Collection<MoleculeDrawing> drawings, final double x, final double y) {
    final var incoming = drawings.stream().map(LaboratorySelection::bounds).toList();
    final var occupied =
        this.editor.document().layouts().values().stream()
            .map(LaboratorySelection::bounds)
            .toList();
    if (clear(incoming, occupied, x, y)) {
      return new double[] {x, y};
    }
    final int[][] directions = {
      {0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };
    for (int ring = 1; ring <= 32; ring++) {
      for (final var direction : directions) {
        final double dx = x + direction[0] * ring * 48, dy = y + direction[1] * ring * 48;
        if (clear(incoming, occupied, dx, dy)) {
          return new double[] {dx, dy};
        }
      }
    }
    final double right = occupied.stream().mapToDouble(b -> b[2]).max().orElse(0);
    final double left = incoming.stream().mapToDouble(b -> b[0]).min().orElse(0);
    return new double[] {right - left + 48, y};
  }

  private static double[] bounds(final MoleculeDrawing drawing) {
    return new double[] {
      drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).min().orElse(0),
      drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).min().orElse(0),
      drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).max().orElse(0),
      drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).max().orElse(0)
    };
  }

  private static boolean clear(
      final List<double[]> incoming,
      final List<double[]> occupied,
      final double dx,
      final double dy) {
    for (final var a : incoming) {
      for (final var b : occupied) {
        if (a[0] + dx < b[2] + 48
            && a[2] + dx > b[0] - 48
            && a[1] + dy < b[3] + 48
            && a[3] + dy > b[1] - 48) {
          return false;
        }
      }
    }
    return true;
  }

  private Map<String, Set<AtomId>> groups() {
    final Map<String, Set<AtomId>> groups = new LinkedHashMap<>();
    this.hits.forEach(
        hit ->
            groups
                .computeIfAbsent(hit.componentId(), ignored -> new LinkedHashSet<>())
                .add(hit.atomId()));
    groups.replaceAll(
        (id, atoms) ->
            MoleculeAbbreviations.selection(this.editor.document().layouts().get(id), atoms));
    if (this.compactHydrogens) {
      groups.replaceAll(
          (id, atoms) ->
              LaboratoryCanvas.includeCarbonHydrogens(
                  this.editor.document().layouts().get(id), atoms));
    }
    return groups;
  }
}
