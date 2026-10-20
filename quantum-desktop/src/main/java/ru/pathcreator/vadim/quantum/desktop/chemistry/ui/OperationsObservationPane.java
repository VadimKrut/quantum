/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryReactionOperationsService.AnalysisChoice;

/** Origin belongs to the operation; linked analyses retain their own independent provenance. */
final class OperationsObservationPane extends VBox {
  private static final ButtonType CLOSE =
      new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
  private final Supplier<LaboratoryExperiment> experiment;
  private final Supplier<String> sample;
  private final Runnable changed;
  private final boolean linkable;
  private final ComboBox<Origin> origin = new ComboBox<>();
  private final ComboBox<AnalysisChoice> candidates = new ComboBox<>();
  private final CheckBox previous = new CheckBox("Показать предыдущие версии");
  private final VBox selected = new VBox(7);
  private final Label hint = text("");
  private final TitledPane links;
  private final List<AnalysisRef> references = new ArrayList<>();
  private boolean loading;

  OperationsObservationPane(
      final Supplier<LaboratoryExperiment> experiment,
      final Supplier<String> sample,
      final boolean linkable,
      final Runnable changed) {
    super(6);
    this.experiment = experiment;
    this.sample = sample;
    this.linkable = linkable;
    this.changed = changed;
    this.setMinWidth(0);
    this.origin.getItems().setAll(Origin.values());
    this.origin.setValue(Origin.UNSPECIFIED);
    this.origin.setPrefWidth(190);
    this.origin.setId("operations-observation-origin");
    this.origin.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Origin value) {
            return value == null ? "" : LaboratoryReactionOperationsService.originLabel(value);
          }

          public Origin fromString(String value) {
            return null;
          }
        });
    this.candidates.setMaxWidth(Double.MAX_VALUE);
    this.candidates.setPrefWidth(440);
    this.candidates.setId("operations-analysis-candidates");
    this.candidates.setPromptText("Выберите анализ этого образца");
    this.candidates.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(AnalysisChoice value) {
            return value == null ? "" : label(value);
          }

          public AnalysisChoice fromString(String value) {
            return null;
          }
        });
    this.candidates.setCellFactory(v -> this.analysisCell(true));
    this.candidates.setButtonCell(this.analysisCell(false));
    final Button add = new Button("Связать");
    add.setId("operations-analysis-add");
    add.setOnAction(
        e -> {
          final var value = this.candidates.getValue();
          if (value == null) {
            this.showHint("Выберите анализ в списке.");
            return;
          }
          if (this.references.contains(value.reference())) {
            this.showHint("Этот анализ уже связан.");
            return;
          }
          if (this.references.size() >= 64) {
            this.showHint("Поддерживается до 64 связанных анализов.");
            return;
          }
          this.references.add(value.reference());
          this.rebuild();
          this.changed.run();
        });
    final var versions =
        new TitledPane(
            "Версии и правила связи",
            new VBox(
                8,
                this.previous,
                text(
                    "Ссылка сохраняет выбранную версию. Площадь пика, Rf и чистота не подставляются в конверсию.")));
    versions.setId("operations-analysis-options");
    versions.setAnimated(false);
    versions.setExpanded(false);
    final VBox body =
        new VBox(8, new FlowPane(8, 6, this.candidates, add), this.hint, this.selected, versions);
    body.setPadding(new Insets(8, 0, 4, 0));
    this.links = new TitledPane("Связанные анализы", body);
    this.links.setAnimated(false);
    this.links.setExpanded(false);
    this.getChildren().setAll(new FlowPane(10, 5, text("Происхождение"), this.origin), this.links);
    this.links.setVisible(linkable);
    this.links.setManaged(linkable);
    this.origin
        .valueProperty()
        .addListener(
            o -> {
              if (!this.loading) {
                this.changed.run();
              }
            });
    this.previous.selectedProperty().addListener(o -> this.refreshCandidates());
    this.rebuild();
  }

  Observation value() {
    final var value = new Observation(this.origin.getValue(), this.references);
    if (this.linkable) {
      LaboratoryReactionOperationsService.validateObservation(
          this.experiment.get(), this.sample.get(), value);
    }
    return value;
  }

  Observation rawValue() {
    return new Observation(this.origin.getValue(), this.references);
  }

  void set(final Observation value) {
    this.loading = true;
    this.origin.setValue(value.origin());
    this.references.clear();
    this.references.addAll(value.analyses());
    this.previous.setSelected(false);
    this.links.setExpanded(false);
    this.loading = false;
    this.rebuild();
  }

  void sampleChanged() {
    this.rebuild();
  }

  private void refreshCandidates() {
    this.candidates
        .getItems()
        .setAll(
            LaboratoryReactionOperationsService.analysisCandidates(
                this.experiment.get(), this.sample.get(), this.previous.isSelected()));
    this.candidates.setValue(null);
  }

  private void rebuild() {
    this.refreshCandidates();
    this.selected.getChildren().clear();
    String warning = "";
    for (final var reference : this.references) {
      String title, description;
      try {
        final var value =
            LaboratoryReactionOperationsService.resolveAnalysis(this.experiment.get(), reference);
        title = label(value);
        description = this.description(value);
        if (this.linkable && !Objects.equals(value.sampleId(), this.sample.get())) {
          warning =
              "Образец изменён: есть несовместимые ссылки. Уберите их явно или верните исходный образец.";
        }
      } catch (final RuntimeException error) {
        title = "Недоступная связанная запись";
        description = reference.kind() + " / " + reference.id();
        warning = error.getMessage();
      }
      final int separator = title.indexOf('\n');
      final String heading = separator < 0 ? title : title.substring(0, separator),
          context = separator < 0 ? "" : title.substring(separator + 1);
      final Hyperlink open = new Hyperlink(heading);
      open.setId("operations-analysis-open");
      open.setWrapText(true);
      open.setMinWidth(0);
      open.setMaxWidth(Double.MAX_VALUE);
      open.setPadding(Insets.EMPTY);
      open.setTooltip(new Tooltip(description));
      open.setAccessibleText("Открыть анализ: " + title);
      final Button remove = new Button("×");
      remove.setId("operations-analysis-remove");
      remove.setTooltip(new Tooltip("Убрать связь с анализом; сам анализ сохранится"));
      remove.setAccessibleText("Убрать связь: " + heading);
      remove.setMinSize(28, 28);
      remove.setStyle("-fx-padding: 2 6;");
      open.setOnAction(
          e -> {
            try {
              this.show(reference);
            } catch (final RuntimeException error) {
              this.showHint(error.getMessage());
            }
          });
      remove.setOnAction(
          e -> {
            this.references.remove(reference);
            this.rebuild();
            this.changed.run();
          });
      final Label metadata = text(context);
      metadata.setMaxWidth(Double.MAX_VALUE);
      metadata.setTooltip(new Tooltip(description));
      final VBox record = new VBox(3, open, metadata);
      record.setMinWidth(0);
      HBox.setHgrow(record, Priority.ALWAYS);
      final HBox row = new HBox(10, record, remove);
      row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
      this.selected.getChildren().add(row);
    }
    this.links.setText(
        "Связанные анализы"
            + (this.references.isEmpty() ? " · необязательно" : " · " + this.references.size()));
    if (!warning.isBlank()) {
      this.showHint(warning);
    } else {
      this.showHint(
          this.candidates.getItems().isEmpty()
              ? this.sample.get() == null
                  ? "Выберите образец, чтобы связать его анализы."
                  : "У образца пока нет сохранённых анализов."
              : "",
          false);
    }
  }

  private void showHint(final String message) {
    this.showHint(message, true);
  }

  private void showHint(final String message, final boolean reveal) {
    this.hint.setText(Objects.requireNonNullElse(message, "Не удалось открыть связанную запись."));
    final boolean present = !this.hint.getText().isBlank();
    this.hint.setVisible(present);
    this.hint.setManaged(present);
    if (present && reveal) {
      this.links.setExpanded(true);
    }
  }

  private ListCell<AnalysisChoice> analysisCell(final boolean popup) {
    return new ListCell<>() {
      protected void updateItem(AnalysisChoice value, boolean empty) {
        super.updateItem(value, empty);
        this.setText(empty || value == null ? null : label(value));
        this.setWrapText(true);
        this.setMinWidth(0);
        if (popup) {
          this.setPrefWidth(420);
        }
        final Tooltip tooltip = empty || value == null ? null : new Tooltip(description(value));
        if (tooltip != null) {
          tooltip.setWrapText(true);
          tooltip.setMaxWidth(640);
        }
        this.setTooltip(tooltip);
      }
    };
  }

  private String label(final AnalysisChoice value) {
    final String sampleName =
        value.sampleId() == null
            ? "без образца"
            : this.experiment.get().samples().stream()
                .filter(s -> s.id().equals(value.sampleId()))
                .map(s -> s.code())
                .findFirst()
                .orElse("образец недоступен");
    return brief(value.title(), 88)
        + " · "
        + brief(value.source(), 36)
        + "\n"
        + value.method()
        + " · "
        + sampleName
        + " · "
        + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(value.recordedAt())
        + " · "
        + value.origin()
        + (value.historical() ? " · предыдущая версия" : "");
  }

  private String description(final AnalysisChoice value) {
    return this.label(value)
        + "\n\nНазвание: "
        + value.title()
        + "\nИсточник: "
        + (value.source().isBlank() ? "не указан" : value.source())
        + "\nСсылка: "
        + value.reference().kind()
        + " / "
        + value.reference().id();
  }

  private static String brief(final String text, final int limit) {
    final String value = text.replaceAll("\\s+", " ").strip();
    return value.isEmpty()
        ? "не указан"
        : value.codePointCount(0, value.length()) > limit
            ? value.substring(0, value.offsetByCodePoints(0, limit - 1)) + "…"
            : value;
  }

  /** Read-only content does not navigate away or commit the enclosing operation draft. */
  Node analysisView(final AnalysisRef reference) {
    final var choice =
        LaboratoryReactionOperationsService.resolveAnalysis(this.experiment.get(), reference);
    final VBox content = new VBox(10);
    content.setPadding(new Insets(12));
    content.setMinWidth(0);
    final String description;
    if (reference.kind() == AnalysisKind.LEGACY) {
      final var analysis =
          this.experiment.get().analyses().stream()
              .filter(a -> a.id().equals(reference.id()))
              .findFirst()
              .orElseThrow();
      final Label title = text(analysis.title());
      title.setStyle("-fx-font-weight: bold;");
      content.getChildren().add(title);
      final String sampleName =
          choice.sampleId() == null
              ? analysis.sample().isBlank() ? "без образца" : analysis.sample()
              : this.experiment.get().samples().stream()
                  .filter(s -> s.id().equals(choice.sampleId()))
                  .map(s -> s.code())
                  .findFirst()
                  .orElse(choice.sampleId());
      content
          .getChildren()
          .add(
              text(
                  choice.method()
                      + " · "
                      + choice.origin()
                      + " · "
                      + sampleName
                      + " · "
                      + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                          .withZone(ZoneId.systemDefault())
                          .format(choice.recordedAt())));
      if (choice.historical()) {
        final Label version =
            text("Предыдущая версия анализа · ссылка сохраняет именно эту запись");
        version.setId("operations-analysis-historical-version");
        content.getChildren().add(version);
      }
      content
          .getChildren()
          .add(text("Источник: " + (choice.source().isBlank() ? "не указан" : choice.source())));
      if (analysis.data() instanceof LaboratoryAnalysis.Nmr n) {
        content
            .getChildren()
            .add(
                text(
                    (n.frequencyMHz() == null
                            ? "Частота не указана"
                            : LaboratoryAnalysisSummary.number(n.frequencyMHz()) + " МГц")
                        + " · "
                        + (n.solvent().isBlank() ? "растворитель не указан" : n.solvent())));
      }
      final var plot = new LaboratoryAnalysisPlot();
      plot.data(analysis.data());
      content.getChildren().add(plot);
      description = LaboratoryAnalysisSummary.text(analysis);
    } else {
      final var entry =
          this.experiment.get().analyticalEvidence().entries().stream()
              .filter(a -> a.id().equals(reference.id()))
              .findFirst()
              .orElseThrow();
      if (choice.historical()) {
        final Label version =
            text("Предыдущая версия анализа · ссылка сохраняет именно эту запись");
        version.setId("operations-analysis-historical-version");
        content.getChildren().add(version);
      }
      content
          .getChildren()
          .add(new LaboratoryEvidenceResultCard(this.experiment.get(), entry, false));
      if (graphical(entry.data())) {
        final var plot = new LaboratoryEvidencePlot();
        plot.show(entry.data());
        content.getChildren().add(plot);
      }
      description = LaboratoryEvidenceService.description(entry);
    }
    final TextArea details = new TextArea(description);
    details.setEditable(false);
    details.setWrapText(true);
    details.setPrefRowCount(12);
    details.positionCaret(0);
    final TitledPane report =
        new TitledPane(
            reference.kind() == AnalysisKind.EVIDENCE
                ? "Подробные данные и расчёт"
                : "Полные данные анализа",
            details);
    report.setId("operations-analysis-full-report");
    report.setAnimated(false);
    report.setExpanded(false);
    report.setMinWidth(0);
    content.getChildren().add(report);
    content
        .getChildren()
        .add(text("Только просмотр сохранённой версии. Ввод текущей операции остаётся в форме."));
    return content;
  }

  private static boolean graphical(final LaboratoryAnalyticalEvidence.Data value) {
    return value instanceof LaboratoryAnalyticalEvidence.Correlations c
            && !c.correlations().isEmpty()
        || value instanceof LaboratoryAnalyticalEvidence.Multiplets m && !m.multiplets().isEmpty()
        || value instanceof LaboratoryAnalyticalEvidence.Chromatogram c
            && (!c.trace().isEmpty() || !c.peaks().isEmpty());
  }

  private void show(final AnalysisRef reference) {
    final Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Связанный анализ · сохранённая версия");
    if (this.getScene() != null && this.getScene().getWindow() != null) {
      dialog.initOwner(this.getScene().getWindow());
    }
    final ScrollPane scroll = new ScrollPane(this.analysisView(reference));
    scroll.setFitToWidth(true);
    scroll.setPrefViewportWidth(700);
    scroll.setPrefViewportHeight(590);
    dialog.getDialogPane().setContent(scroll);
    dialog.getDialogPane().getButtonTypes().setAll(CLOSE);
    LaboratoryDialogStyle.apply(dialog, dialog.getOwner());
    dialog.showAndWait();
  }

  private static Label text(final String value) {
    final Label label = new Label(value);
    label.setWrapText(true);
    label.setMinWidth(0);
    return label;
  }
}
