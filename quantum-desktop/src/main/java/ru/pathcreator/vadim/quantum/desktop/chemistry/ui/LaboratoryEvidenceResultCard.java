/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEvidenceService;

/** Typed, read-only result hierarchy; the original complete report remains available separately. */
final class LaboratoryEvidenceResultCard extends VBox {
  LaboratoryEvidenceResultCard(
      final LaboratoryExperiment experiment, final Entry entry, final boolean preview) {
    super(7);
    this.setId("evidence-result-summary");
    this.setMinWidth(0);
    final Label state = text(preview ? "Предпросмотр · ещё не добавлено в опыт" : "Запись в опыте");
    state.setId("evidence-result-state");
    state.getStyleClass().add("workflow-card-subtitle");
    if (entry.data() instanceof Dossier) {
      this.getChildren().addAll(state, source(entry));
      return; // The existing dossier card owns its conclusion, identity and diagnostics.
    }
    this.getStyleClass().add("section-card");
    final Label title = text(entry.title());
    title.getStyleClass().add("section-title");
    this.getChildren().add(new FlowPane(16, 4, title, state));
    final String sample =
        entry.sampleId() == null
            ? "Без отдельного физического образца"
            : experiment.samples().stream()
                .filter(s -> s.id().equals(entry.sampleId()))
                .map(s -> "Образец «" + s.code() + "»")
                .findFirst()
                .orElse("Образец из сохранённой записи");
    final Label identity = text(sample + " · " + entry.molecule().displayName());
    identity.setId("evidence-result-identity");
    final String origin =
        "Происхождение: "
            + entry.origin()
            + (entry.origin() == LaboratoryAnalysis.Origin.MEASURED
                ? ""
                : " · не измерение данного образца");
    final Label provenance = text(origin);
    provenance.setId("evidence-result-origin");
    provenance.getStyleClass().add("section-title");
    this.getChildren().addAll(identity, provenance);
    switch (entry.data()) {
      case QuantitativeNmr q -> {
        final var value = LaboratoryEvidenceService.quantitativeNmr(q);
        this.getChildren()
            .add(
                metric(
                    LaboratoryEvidenceService.format(value.purityPercent()) + "%",
                    "Массовая доля по qNMR",
                    "qnmr"));
        this.getChildren()
            .add(
                text(
                    "Навеска образца: "
                        + q.sampleMass()
                        + " · внутренний стандарт: "
                        + q.standardName()
                        + ", "
                        + q.standardMass()));
      }
      case Chromatogram c -> this.chromatogram(c);
      case Correlations c -> {
        this.getChildren()
            .add(
                text(
                    c.experiment()
                        + " · корреляций: "
                        + c.correlations().size()
                        + (c.spectrum() == null
                            ? ""
                            : " · сетка " + c.spectrum().width() + " × " + c.spectrum().height())));
        this.getChildren()
            .add(
                text(
                    "Оси: "
                        + c.xNucleus()
                        + " / "
                        + c.yNucleus()
                        + (c.solvent().isBlank() ? "" : " · растворитель: " + c.solvent())));
      }
      case Multiplets m -> {
        this.getChildren().add(text(m.nucleus() + " · мультиплетов: " + m.multiplets().size()));
        if (m.analysisId() != null)
          this.getChildren()
              .add(
                  text(
                      "Связанный спектр: "
                          + LaboratoryEvidenceService.exactSource(experiment, entry)
                              .orElseThrow()
                              .title()
                          + " · сохранённая версия"));
        this.getChildren()
            .add(text("Частота: " + LaboratoryEvidenceService.format(m.frequencyMHz()) + " МГц"));
      }
      case Dossier ignored -> {}
    }
    // Only the exact generated origin notice is already fully represented in the header.
    final String originNotice =
        entry.origin() == LaboratoryAnalysis.Origin.MEASURED
            ? null
            : "Происхождение данных: " + entry.origin() + ". Это не измерение данного образца.";
    final var diagnostics =
        LaboratoryEvidenceService.diagnostics(entry).stream()
            .filter(w -> !w.equals(originNotice))
            .toList();
    final var warnings = diagnostics.stream().filter(w -> !generalGuidance(w)).toList();
    if (!warnings.isEmpty()) {
      final VBox checks = new VBox(5);
      checks.setId("evidence-result-warnings");
      final Label heading = text("При оценке результата");
      heading.getStyleClass().add("section-title");
      checks.getChildren().add(heading);
      warnings.forEach(w -> checks.getChildren().add(text("• " + w)));
      this.getChildren().add(checks);
    }
    this.getChildren()
        .add(
            details(
                entry,
                preview,
                diagnostics.stream()
                    .filter(LaboratoryEvidenceResultCard::generalGuidance)
                    .toList()));
  }

  private void chromatogram(final Chromatogram c) {
    this.getChildren().add(text(c.technique() + " · детектор: " + c.detector()));
    if (c.calibration() != null) {
      final var quantity = LaboratoryEvidenceService.quantify(c);
      final FlowPane amounts = new FlowPane(28, 8);
      amounts
          .getChildren()
          .add(
              metric(
                  LaboratoryEvidenceService.format(quantity.mass().value())
                      + " "
                      + quantity.mass().unit(),
                  "Масса аналита по калибровке",
                  "chrom-mass"));
      if (quantity.purityPercent() != null) {
        amounts
            .getChildren()
            .add(
                metric(
                    LaboratoryEvidenceService.format(quantity.purityPercent()) + "%",
                    "Массовая доля по калибровке",
                    "chrom-purity"));
      }
      final var peak =
          c.peaks().stream()
              .filter(p -> p.id().equals(c.quantifiedPeakId()))
              .findFirst()
              .orElseThrow();
      final var area = LaboratoryEvidenceService.areaPercent(c).get(peak.id());
      if (peak.included() && area != null) {
        final VBox signal =
            metric(
                LaboratoryEvidenceService.format(area) + "%",
                "Доля площади выбранного пика",
                "chrom-area");
        signal.getChildren().add(text("Сигнал детектора, не массовая доля"));
        amounts.getChildren().add(signal);
      }
      this.getChildren().add(amounts);
      this.getChildren()
          .add(
              text(
                  "Аналит: "
                      + peak.label()
                      + " · tR "
                      + LaboratoryEvidenceService.format(peak.retentionMin())
                      + " мин · калибровка относится только к этому аналиту"));
      if (!peak.included() || area == null) {
        this.getChildren()
            .add(
                text(
                    "Доля площади выбранного пика: "
                        + (!peak.included()
                            ? "пик исключён из суммы"
                            : "не определена: сумма площадей равна нулю")));
      }
    } else {
      final Label heading = text("Профиль хроматограммы");
      heading.getStyleClass().add("section-title");
      this.getChildren().add(heading);
      this.getChildren()
          .add(text("Массовая доля по калибровке не рассчитана: калибровка не задана."));
    }
    if (c.calibration() == null) {
      this.getChildren().add(text("Доля площади отражает сигнал детектора, а не массовую долю."));
    }
    if (!c.peaks().isEmpty() && LaboratoryEvidenceService.areaPercent(c).isEmpty()) {
      this.getChildren()
          .add(text("Доли площадей не определены: сумма включённых площадей равна нулю."));
    }
    if (c.reportedPurityPercent() != null) {
      this.getChildren()
          .add(
              text(
                  "Отдельно сообщённая массовая доля: "
                      + LaboratoryEvidenceService.format(c.reportedPurityPercent())
                      + "%"));
    }
  }

  private static boolean generalGuidance(final String message) {
    return message.startsWith("Расстояния между линиями показаны отдельно от введённых J:")
        || message.equals(
            "Доля площади — доля сигнала детектора; массовая чистота не определяется из неё автоматически.")
        || message.equals(
            "Калибровка относится только к выбранному аналиту и той же подготовке и регистрации. Доли площадей других пиков не превращаются в массовые доли.");
  }

  private static TitledPane details(
      final Entry entry, final boolean preview, final List<String> guidance) {
    final StringBuilder content =
        new StringBuilder("Источник записи:\n")
            .append(entry.source().isBlank() ? "Не указан" : entry.source())
            .append(preview ? "\n\nПодготовлено: " : "\n\nДата записи: ")
            .append(
                DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(entry.recordedAt()));
    if (entry.data() instanceof Chromatogram c) {
      final long included = c.peaks().stream().filter(ChromPeak::included).count();
      content
          .append("\n\nПиков в сумме площадей: ")
          .append(included)
          .append(" · исключено: ")
          .append(c.peaks().size() - included);
      if (c.calibration() != null)
        content.append("\nИсточник калибровки: ").append(c.calibration().source());
      if (c.reportedPurityPercent() != null)
        content
            .append("\nИсточник отдельно сообщённой массовой доли: ")
            .append(c.reportedPuritySource());
    }
    for (final String message : guidance) content.append("\n\n").append(message);
    final TextArea full = new TextArea(content.toString());
    full.setId("evidence-result-source");
    full.setEditable(false);
    full.setWrapText(true);
    full.setPrefRowCount(6);
    full.setMinHeight(100);
    final Button copy = new Button("Копировать сведения");
    copy.setId("evidence-result-source-copy");
    copy.setOnAction(
        event -> {
          final var data = new ClipboardContent();
          data.putString(content.toString());
          Clipboard.getSystemClipboard().setContent(data);
        });
    final TitledPane pane = new TitledPane("Источник и условия расчёта", new VBox(8, full, copy));
    pane.setId("evidence-result-provenance-details");
    pane.setExpanded(false);
    pane.setAnimated(false);
    return pane;
  }

  private static VBox metric(final String value, final String caption, final String id) {
    final Label number = text(value);
    number.setId("evidence-result-" + id);
    number.setStyle("-fx-font-size: 28px; -fx-font-weight: 700;");
    final Label meaning = text(caption);
    meaning.getStyleClass().add("section-title");
    final VBox box = new VBox(2, number, meaning);
    box.setMinWidth(0);
    return box;
  }

  private static Label source(final Entry entry) {
    final Label label =
        shortText(entry.source().isBlank() ? "Источник не указан" : "Источник: " + entry.source());
    label.setId("evidence-result-source");
    return label;
  }

  private static Label shortText(final String value) {
    final Label label =
        text(
            value.length() > 220
                ? value.substring(0, 220) + "… (полностью — в подробном отчёте)"
                : value);
    if (value.length() > 220) label.setTooltip(new Tooltip(value));
    return label;
  }

  private static Label text(final String value) {
    final Label label = new Label(value);
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }
}
