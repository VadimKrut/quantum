/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Saved coordinate alternatives share the same explicit atoms, graph and stereochemical declaration. */
public record LaboratoryConformerSet(
    String experimentId,
    String componentId,
    Molecule source,
    List<Frame> frames,
    String referenceId) {
  public LaboratoryConformerSet(
      final String experimentId,
      final String componentId,
      final Molecule source,
      final List<Frame> frames) {
    this(experimentId, componentId, source, frames, null);
  }

  /** Calculated potential energy for a converged geometry, never an experimental or free energy. */
  public record Energy(double kcalPerMol, String forceField, String implementation) {
    public Energy {
      Objects.requireNonNull(forceField);
      Objects.requireNonNull(implementation);
      if (!Double.isFinite(kcalPerMol)
          || Math.abs(kcalPerMol) > 1e9
          || forceField.isBlank()
          || forceField.length() > 100
          || implementation.isBlank()
          || implementation.length() > 200) {
        throw new IllegalArgumentException("Недопустимая расчётная энергия или её метод.");
      }
    }

    public boolean comparable(final Energy other) {
      return other != null
          && this.forceField.equals(other.forceField)
          && this.implementation.equals(other.implementation);
    }
  }

  public record View(
      double yaw,
      double pitch,
      double zoom,
      double panX,
      double panY,
      String representation,
      boolean labels) {
    public static final View DEFAULT = new View(25, 15, 1, 0, 0, "Шары и связи", false);

    public View {
      if (!Double.isFinite(yaw)
          || !Double.isFinite(pitch)
          || !Double.isFinite(zoom)
          || !Double.isFinite(panX)
          || !Double.isFinite(panY)
          || Math.abs(yaw) > 180
          || Math.abs(pitch) > 90
          || zoom < .3
          || zoom > 3
          || Math.abs(panX) > 100000
          || Math.abs(panY) > 100000) {
        throw new IllegalArgumentException("Недопустимый сохранённый вид конформера.");
      }
      Objects.requireNonNull(representation);
      if (!Set.of("Шары и связи", "Стержни").contains(representation)) {
        throw new IllegalArgumentException("Недопустимый вид модели.");
      }
    }
  }

  public record Frame(String title, String method, Molecule geometry, View view, Energy energy) {
    public Frame(
        final String title, final String method, final Molecule geometry, final View view) {
      this(title, method, geometry, view, null);
    }

    public Frame {
      Objects.requireNonNull(title);
      Objects.requireNonNull(method);
      Objects.requireNonNull(geometry);
      Objects.requireNonNull(view);
      if (title.isBlank() || title.length() > 500 || method.length() > 4000) {
        throw new IllegalArgumentException(
            "Укажите короткое название и метод построения конформера.");
      }
    }

    public Frame withView(final View next) {
      return new Frame(this.title, this.method, this.geometry, next, this.energy);
    }
  }

  public LaboratoryConformerSet {
    ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue.requireIdentifier(
        experimentId, "Conformer experiment");
    ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue.requireIdentifier(
        componentId, "Conformer component");
    Objects.requireNonNull(source);
    frames = List.copyOf(frames);
    if (frames.isEmpty() || frames.size() > 128 || source.atomCount() > 256) {
      throw new IllegalArgumentException(
          "Сохраните от 1 до 128 геометрий структуры не более 256 явных атомов.");
    }
    referenceId = referenceId == null ? frames.getFirst().geometry().id().value() : referenceId;
    final String selectedReference = referenceId;
    if (frames.stream()
        .noneMatch(frame -> frame.geometry().id().value().equals(selectedReference))) {
      throw new IllegalArgumentException("Опорная конформация отсутствует в наборе.");
    }
    final Set<String> ids = new HashSet<>();
    ids.add(source.id().value());
    for (final var frame : frames) {
      final var m = frame.geometry();
      if (!ids.add(m.id().value())) {
        throw new IllegalArgumentException(
            "Снимки конформеров должны иметь разные идентификаторы.");
      }
      if (!source.bonds().equals(m.bonds())
          || source.atomCount() != m.atomCount()
          || !source.charge().equals(m.charge())
          || !source.spinMultiplicity().equals(m.spinMultiplicity())
          || !source.stereochemistry().equals(m.stereochemistry())
          || !source.conformation().equals(m.conformation())) {
        throw new IllegalArgumentException(
            "Конформер должен иметь тот же состав, связи и стереозаписи.");
      }
      for (int i = 0; i < source.atomCount(); i++) {
        final var a = source.atoms().get(i);
        final var b = m.atoms().get(i);
        if (b.coordinate() == null
            || !ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom.of(
                    a.id(),
                    a.element().symbol(),
                    b.coordinate(),
                    a.formalCharge(),
                    a.isotope(),
                    a.radicalState(),
                    a.metadata())
                .equals(b)) {
          throw new IllegalArgumentException(
              "Конформер изменил атомы или не содержит полных координат.");
        }
      }
    }
  }
}
