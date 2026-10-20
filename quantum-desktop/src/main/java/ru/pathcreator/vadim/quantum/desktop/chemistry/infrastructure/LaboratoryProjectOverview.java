/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Read-only project snapshots retain the original core objects and separate each reaction stage. */
public final class LaboratoryProjectOverview {
  public record Row(
      Path path, LaboratoryDrawing drawing, LaboratoryReactionStage stage, String search) {
    public String key() {
      return this.path + "#" + (this.stage == null ? "" : this.stage.id());
    }

    public String title() {
      return this.drawing.experiment().title();
    }

    public String stageTitle() {
      return this.stage == null ? "Без стадии" : this.stage.title();
    }

    public List<LaboratoryComponent> components(final ReactionComponentRole role) {
      return this.drawing.experiment().components().stream()
          .filter(
              component ->
                  this.stage == null
                      ? component.role() == role
                      : this.stage.participants().stream()
                          .anyMatch(
                              p -> p.componentId().equals(component.id()) && p.role() == role))
          .toList();
    }

    public boolean hasResult() {
      return this.components(ReactionComponentRole.PRODUCT).stream()
          .anyMatch(c -> c.evidence() == LaboratoryEvidence.OBSERVED && c.actual() != null);
    }

    public ReactionConditions conditions() {
      return this.stage == null ? this.drawing.experiment().conditions() : this.stage.conditions();
    }

    public String products() {
      return String.join(
          " · ",
          this.components(ReactionComponentRole.PRODUCT).stream()
              .map(c -> c.molecule().displayName())
              .toList());
    }

    public String media() {
      final List<String> names =
          new ArrayList<>(
              this.conditions().components().stream()
                  .map(ReactionConditionComponent::name)
                  .toList());
      for (final var role :
          List.of(ReactionComponentRole.SOLVENT, ReactionComponentRole.CATALYST)) {
        this.components(role).forEach(c -> names.add(c.molecule().displayName()));
      }
      return String.join(" · ", new LinkedHashSet<>(names));
    }

    public String temperature() {
      final var t = this.conditions().temperature();
      return t == null
          ? "—"
          : t.value()
              + " "
              + (t.unit()
                      == ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit.CELSIUS
                  ? "°C"
                  : "K");
    }

    public String conditionsSummary() {
      if (this.conditions().temperature() != null || this.conditions().duration() != null) {
        return this.temperature() + " · " + this.duration();
      }
      final var common = this.drawing.experiment().conditions();
      return this.stage != null && !common.equals(ReactionConditions.EMPTY)
          ? "Общие: " + conditionsText(common)
          : "Условия не указаны";
    }

    public String duration() {
      final var t = this.conditions().duration();
      return t == null
          ? "—"
          : t.value()
              + " "
              + switch (t.unit()) {
                case SECOND -> "с";
                case MINUTE -> "мин";
                case HOUR -> "ч";
                case DAY -> "сут";
              };
    }
  }

  public record Snapshot(List<Row> rows, List<String> diagnostics) {
    public Snapshot {
      rows = List.copyOf(rows);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  public static String conditionsText(final ReactionConditions conditions) {
    final List<String> parts = new ArrayList<>();
    if (conditions.temperature() != null) {
      final var t = conditions.temperature();
      parts.add(
          t.value()
              + " "
              + (t.unit()
                      == ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit.CELSIUS
                  ? "°C"
                  : "K"));
    }
    if (conditions.duration() != null) {
      final var t = conditions.duration();
      parts.add(
          t.value()
              + " "
              + switch (t.unit()) {
                case SECOND -> "с";
                case MINUTE -> "мин";
                case HOUR -> "ч";
                case DAY -> "сут";
              });
    }
    if (conditions.pressure() != null) {
      final var p = conditions.pressure();
      parts.add(p.value() + " " + p.unit());
    }
    conditions.components().forEach(c -> parts.add(c.name()));
    if (conditions.note() != null && !conditions.note().isBlank()) {
      parts.add(conditions.note());
    }
    return String.join(" · ", parts);
  }

  public Snapshot scan(final Path folder) throws IOException {
    final List<Row> rows = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    final int[] documents = {0};
    final var index =
        new LaboratoryExperimentIndex()
            .scan(
                folder,
                new LaboratoryExperimentIndex.Limits(
                    4000, 24, LaboratoryExperimentIndex.PROJECT_SCAN_BYTES),
                (path, drawing) -> {
                  if (++documents[0] > 300) {
                    return;
                  }
                  final var experiment = drawing.experiment();
                  final String common =
                      path
                          + " "
                          + experiment.title()
                          + " "
                          + experiment.notes()
                          + " "
                          + conditionsText(experiment.conditions())
                          + " "
                          + String.join(
                              " ",
                              experiment.components().stream()
                                  .map(
                                      c ->
                                          c.molecule().displayName()
                                              + " "
                                              + c.molecule().formula().hillNotation())
                                  .toList());
                  if (experiment.stages().isEmpty()) {
                    rows.add(
                        new Row(path, drawing, null, LaboratoryExperimentIndex.normalize(common)));
                  } else {
                    for (final var stage : experiment.stages()) {
                      final Row row = new Row(path, drawing, stage, "");
                      rows.add(
                          new Row(
                              path,
                              drawing,
                              stage,
                              LaboratoryExperimentIndex.normalize(
                                  common
                                      + " "
                                      + stage.title()
                                      + " "
                                      + row.media()
                                      + " "
                                      + stage.conditions().note())));
                    }
                  }
                });
    notes.addAll(index.diagnostics());
    index.entries().stream()
        .filter(e -> e.problem() != null)
        .forEach(e -> notes.add(e.path().getFileName() + ": " + e.problem()));
    if (documents[0] > 300) {
      notes.add(
          "Обзор ограничен 300 опытами. Выберите более узкую папку; остальные файлы не изменены.");
    }
    rows.sort(
        Comparator.comparing(Row::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Row::key));
    return new Snapshot(rows, notes);
  }
}
