/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

/** A stored calculation is evidence about its input, never automatically about the current document. */
public final class LaboratoryCalculationArchive {
  public enum Relation {
    CURRENT_INPUT("Вход расчёта соответствует текущей структуре"),
    CURRENT_OUTPUT("Текущая структура соответствует сохранённой геометрии результата"),
    STALE("Устаревший расчёт: структура или координаты изменились"),
    OTHER_EXPERIMENT("Расчёт относится к другому опыту"),
    MISSING_COMPONENT("Исходный компонент отсутствует в опыте"),
    UNKNOWN_INPUT("Не удалось однозначно определить входной снимок");
    private final String text;

    Relation(final String text) {
      this.text = text;
    }

    public String text() {
      return this.text;
    }
  }

  private final ChemistryStorageDocument document;
  private final Molecule input;
  private final Molecule output;

  public LaboratoryCalculationArchive(final ChemistryStorageDocument document) {
    this.document = Objects.requireNonNull(document);
    if (!document.metadata().containsKey("calculation.kind")) {
      throw new IllegalArgumentException("Вложение не является снимком расчёта.");
    }
    this.output = this.molecule(document.metadata().get("calculation.output_molecule"));
    final String inputId = document.metadata().get("calculation.input_molecule");
    final var candidates =
        document.molecules().stream()
            .filter(
                value ->
                    !value
                        .id()
                        .value()
                        .equals(document.metadata().get("calculation.output_molecule")))
            .toList();
    this.input =
        inputId != null
            ? this.molecule(inputId)
            : candidates.size() == 1 ? candidates.getFirst() : null;
  }

  private Molecule molecule(final String id) {
    return id == null
        ? null
        : this.document.molecules().stream()
            .filter(value -> value.id().value().equals(id))
            .findFirst()
            .orElse(null);
  }

  public String title() {
    return this.document.displayName();
  }

  public String status() {
    return this.document.metadata().getOrDefault("calculation.status", "UNKNOWN");
  }

  public Molecule input() {
    return this.input;
  }

  public Molecule output() {
    return this.output;
  }

  public Relation relation(final LaboratoryDrawing drawing) {
    if (!drawing.experiment().id().equals(this.document.metadata().get("calculation.experiment"))) {
      return Relation.OTHER_EXPERIMENT;
    }
    final var component =
        drawing.experiment().components().stream()
            .filter(
                value -> value.id().equals(this.document.metadata().get("calculation.component")))
            .findFirst()
            .orElse(null);
    if (component == null) {
      return Relation.MISSING_COMPONENT;
    }
    if (this.input == null) {
      return Relation.UNKNOWN_INPUT;
    }
    if (sameState(component.molecule(), this.input)) {
      return Relation.CURRENT_INPUT;
    }
    if (this.output != null && sameState(component.molecule(), this.output)) {
      return Relation.CURRENT_OUTPUT;
    }
    return Relation.STALE;
  }

  /** Molecule names and metadata may change; atom records, bonds and scientific state must match exactly. */
  private static boolean sameState(final Molecule first, final Molecule second) {
    return first.atoms().equals(second.atoms())
        && first.bonds().equals(second.bonds())
        && first.charge().equals(second.charge())
        && first.spinMultiplicity().equals(second.spinMultiplicity())
        && first.stereochemistry().equals(second.stereochemistry())
        && first.conformation().equals(second.conformation())
        && first.opticalRotation().equals(second.opticalRotation())
        && first.symmetry().equals(second.symmetry());
  }

  public String report() {
    final StringBuilder report =
        new StringBuilder(this.title())
            .append("\nСтатус выполнения: ")
            .append(this.status())
            .append("\n");
    if (this.status().equals("RUNNING") || this.status().equals("PLANNED")) {
      report.append(
          "Сохранён незавершённый снимок. Наличие файла не означает, что процесс сейчас работает.\n");
    }
    if (this.output != null) {
      report.append(
          this.document.metadata().getOrDefault("calculation.kind", "").equals("INITIAL_GEOMETRY")
              ? "Начальная раскладка по графу; без оптимизации.\n"
              : "Сходимость геометрии: "
                  + ("true".equals(this.document.metadata().get("calculation.geometry_converged"))
                      ? "достигнута; модель приближённая.\n"
                      : "НЕ подтверждена. Последняя итерация не является сошедшейся геометрией.\n"));
    }
    final String stored = this.document.metadata().get("calculation.report");
    if (stored != null) {
      report.append('\n').append(stored);
    } else {
      for (final var result : this.document.classicalResultRecords()) {
        report.append("\nПокрытие: ").append(result.coverageStatus()).append('\n');
        for (final var section : result.sections()) {
          report.append('\n').append(section.title()).append('\n');
          section
              .values()
              .forEach(
                  value ->
                      report
                          .append(value.name())
                          .append(" = ")
                          .append(
                              String.join(
                                  value.kind().equals("TABLE") ? "\n" : ", ", value.values()))
                          .append(' ')
                          .append(value.unit())
                          .append('\n'));
        }
      }
      this.document.extensions().stream()
          .filter(extension -> extension.kind().equals("laboratory-calculation-diagnostic-v1"))
          .forEach(
              extension ->
                  report
                      .append('\n')
                      .append(extension.properties().get("severity"))
                      .append(" · ")
                      .append(extension.properties().get("code"))
                      .append(" · ")
                      .append(extension.properties().get("message")));
    }
    report.append("\n\nПараметры сохранённого запуска\n");
    this.document
        .metadata()
        .forEach(
            (key, value) -> {
              if (!key.equals("calculation.report")) {
                report.append(key).append(": ").append(value).append('\n');
              }
            });
    return report.toString();
  }
}
