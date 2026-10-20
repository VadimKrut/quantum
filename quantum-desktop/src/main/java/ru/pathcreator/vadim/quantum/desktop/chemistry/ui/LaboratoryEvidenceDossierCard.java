/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.Dossier;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.Entry;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEvidenceService;

/** Read-only overview of explicit dossier links; detailed source records stay available below. */
final class LaboratoryEvidenceDossierCard extends VBox {
  LaboratoryEvidenceDossierCard(final LaboratoryExperiment experiment, final Entry entry) {
    super(10);
    this.getStyleClass().add("section-card");
    if (!(entry.data() instanceof Dossier dossier)) {
      throw new IllegalArgumentException("Карточка предназначена для досье продукта.");
    }
    final Label title = text(entry.title());
    title.getStyleClass().add("section-title");
    final String sample =
        entry.sampleId() == null
            ? "Без отдельного физического образца"
            : experiment.samples().stream()
                .filter(s -> s.id().equals(entry.sampleId()))
                .map(s -> "Образец «" + s.code() + "»")
                .findFirst()
                .orElse("Образец из сохранённой записи");
    this.getChildren()
        .addAll(
            title,
            text(sample + " · " + entry.molecule().displayName()),
            text("Статус: " + dossier.conclusion()),
            text("Происхождение: " + entry.origin()));
    if (!dossier.conclusionText().isBlank())
      this.getChildren().add(shortText(dossier.conclusionText(), 280));

    final var open = dossier.findings().stream().filter(f -> !f.resolved()).toList();
    this.getChildren().add(text("Открытых замечаний пользователя: " + open.size()));
    open.stream()
        .limit(3)
        .forEach(
            f ->
                this.getChildren()
                    .add(
                        shortText(
                            LaboratoryEvidenceService.severityLabel(f.severity())
                                + " · "
                                + f.category()
                                + ": "
                                + f.text(),
                            220)));
    if (open.size() > 3)
      this.getChildren().add(text("Ещё " + (open.size() - 3) + " — в подробном отчёте."));

    final Map<String, Integer> sources = new LinkedHashMap<>();
    final VBox provenance = new VBox(9);
    provenance.getChildren().add(text("Досье: " + entry.id()));
    if (!entry.source().isBlank())
      provenance.getChildren().add(text("Источник досье: " + entry.source()));
    final int count = dossier.analysisIds().size() + dossier.evidenceIds().size();
    this.getChildren().add(text("Подтверждающие записи: " + count));
    for (final String id : dossier.analysisIds()) {
      final var record =
          experiment.analyses().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
      this.getChildren()
          .add(
              record(
                  record.title(),
                  record.data().method(),
                  record.origin().toString(),
                  record.source(),
                  "Анализ: " + record.id(),
                  sources,
                  provenance));
    }
    for (final String id : dossier.evidenceIds()) {
      final var record =
          experiment.analyticalEvidence().entries().stream()
              .filter(a -> a.id().equals(id))
              .findFirst()
              .orElseThrow();
      this.getChildren()
          .add(
              record(
                  record.title(),
                  record.data().method(),
                  record.origin().toString(),
                  record.source(),
                  "Аналитическая запись: " + record.id(),
                  sources,
                  provenance));
    }
    if (count == 0) this.getChildren().add(text("Подтверждающие аналитические записи не выбраны."));
    sources.forEach(
        (source, number) ->
            provenance.getChildren().add(text("Источник " + number + ":\n" + source)));
    final TitledPane sourceDetails =
        new TitledPane("Источники и точные ссылки · " + count + " записей", provenance);
    sourceDetails.setId("evidence-dossier-provenance");
    sourceDetails.setAnimated(false);
    sourceDetails.setExpanded(false);
    this.getChildren().add(sourceDetails);
    final var warnings = new LinkedHashSet<>(LaboratoryEvidenceService.dossier(experiment, entry));
    final VBox checks = new VBox(7);
    warnings.forEach(w -> checks.getChildren().add(text(w)));
    if (warnings.isEmpty())
      checks
          .getChildren()
          .add(
              text(
                  "Выполненные проверки не выявили замечаний. Идентичность продукта подтверждает пользователь."));
    final TitledPane diagnostics =
        new TitledPane("Проверки и предупреждения · " + warnings.size(), checks);
    diagnostics.setAnimated(false);
    diagnostics.setExpanded(false);
    this.getChildren().add(diagnostics);
  }

  private static VBox record(
      final String title,
      final String method,
      final String origin,
      final String source,
      final String reference,
      final Map<String, Integer> sources,
      final VBox provenance) {
    final String sourceLabel =
        source.isBlank()
            ? "Источник не указан"
            : "Источник " + sources.computeIfAbsent(source, key -> sources.size() + 1);
    final Label heading = shortText(title + " · " + method + " · " + origin, 200);
    heading.setTooltip(new Tooltip(title + "\n" + method + " · " + origin + "\n" + reference));
    provenance
        .getChildren()
        .add(
            text(title + " · " + method + " · " + origin + "\n" + reference + " · " + sourceLabel));
    return new VBox(3, heading, text(sourceLabel));
  }

  private static Label shortText(final String value, final int limit) {
    final Label label = text(value.length() <= limit ? value : value.substring(0, limit) + "…");
    if (value.length() > limit) label.setTooltip(new Tooltip(value));
    return label;
  }

  private static Label text(final String value) {
    final Label label = new Label(value);
    label.setWrapText(true);
    label.setMaxWidth(Double.MAX_VALUE);
    label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    return label;
  }
}
