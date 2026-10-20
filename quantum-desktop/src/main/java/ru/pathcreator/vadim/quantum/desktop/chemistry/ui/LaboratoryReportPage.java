/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryExperimentReport;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryExperimentReport.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Read-only snapshot report. Raster preview is rendered from the exact PDF bytes offered for export. */
final class LaboratoryReportPage extends LaboratoryComputationPage {
  private record Choice(String id, String name) {
    public String toString() {
      return this.name;
    }
  }

  private final LaboratoryDrawing drawing;
  private final CheckBox structures = option("Схемы и структуры", true, "structures"),
      quantities = option("Загрузки и выход", true, "quantities"),
      procedure = option("Процедура и заметки", true, "procedure"),
      analyses = option("Аналитика", true, "analyses"),
      journal = option("Полный журнал", false, "journal"),
      attachments = option("Перечень вложений", true, "attachments"),
      diagnostics = option("Технические подробности", false, "diagnostics"),
      superseded = option("История аналитики", false, "history"),
      strict = option("Требовать полный баланс реакции", false, "strict");
  private final Map<String, ComboBox<Choice>> references = new LinkedHashMap<>();
  private final Button generate = new Button("Обновить отчёт"),
      pdf = new Button("PDF…"),
      html = new Button("HTML…"),
      cancel = new Button("Отмена"),
      previous = new Button("←"),
      next = new Button("→");
  private final Label pageLabel = new Label("Предпросмотр"),
      source = note("PDF и HTML содержат данные открытого опыта.");
  private final ImageView image = new ImageView();
  private final TextArea text = new TextArea();
  private final java.util.concurrent.ExecutorService worker =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "laboratory-experiment-report");
            t.setDaemon(true);
            return t;
          });
  private java.util.concurrent.Future<?> task;
  private int generation, pageIndex;
  private boolean exporting;
  private LaboratoryReportFiles.Bundle bundle;

  LaboratoryReportPage(final LaboratoryDrawing drawing, final Runnable close) {
    this(drawing, Map.of(), close);
  }

  LaboratoryReportPage(
      final LaboratoryDrawing drawing,
      final Map<String, String> initialReferences,
      final Runnable close) {
    super("Отчёт по опыту", close);
    this.drawing = drawing;
    this.setId("chemistry-report-page");
    this.setMinSize(0, 0);
    final VBox settings = new VBox(8);
    settings.setPadding(new Insets(10));
    final FlowPane sections =
        new FlowPane(
            14,
            8,
            this.structures,
            this.quantities,
            this.procedure,
            this.analyses,
            this.journal,
            this.attachments,
            this.diagnostics,
            this.superseded,
            this.strict);
    settings.getChildren().add(sections);
    for (final var stage : drawing.experiment().stages()) {
      final var combo = new ComboBox<Choice>();
      combo.setId("chemistry-report-reference-" + stage.id());
      combo.setMaxWidth(360);
      for (final var participant : stage.participants()) {
        if (participant.role() == ReactionComponentRole.REACTANT) {
          final var c =
              drawing.experiment().components().stream()
                  .filter(v -> v.id().equals(participant.componentId()))
                  .findFirst()
                  .orElseThrow();
          combo.getItems().add(new Choice(c.id(), c.molecule().displayName()));
        }
      }
      final String initial = initialReferences.get(stage.id());
      if (initialReferences.containsKey(stage.id())) {
        combo
            .getSelectionModel()
            .select(
                combo.getItems().stream()
                    .filter(c -> c.id().equals(initial))
                    .findFirst()
                    .orElse(null));
      } else {
        final var auto =
            ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryYieldCalculation
                .automaticReference(
                    drawing.experiment(),
                    stage,
                    ru.pathcreator.vadim.quantum.chemistry.drawing.application
                        .LaboratoryPlanStructureMasses.resolve(drawing));
        combo
            .getSelectionModel()
            .select(
                combo.getItems().stream()
                    .filter(c -> c.id().equals(auto.id()))
                    .findFirst()
                    .orElse(null));
      }
      this.references.put(stage.id(), combo);
      combo.setOnAction(e -> this.invalidate());
      final Label label = note(stage.title() + " · за 1 эквивалент");
      label.setMaxWidth(280);
      settings.getChildren().add(new FlowPane(10, 6, label, combo));
    }
    final ScrollPane settingsScroll = new ScrollPane(settings);
    settingsScroll.setFitToWidth(true);
    settingsScroll.setPrefViewportHeight(160);
    settingsScroll.setMaxHeight(180);
    final TitledPane options = new TitledPane("Состав отчёта и основа выхода", settingsScroll);
    options.setAnimated(false);
    options.setMinHeight(Region.USE_PREF_SIZE);
    options.setExpanded(false);
    options.setId("chemistry-report-options");
    for (final var check :
        List.of(
            this.structures,
            this.quantities,
            this.procedure,
            this.analyses,
            this.journal,
            this.attachments,
            this.diagnostics,
            this.superseded,
            this.strict)) {
      check.setOnAction(e -> this.invalidate());
    }
    this.generate.setId("chemistry-report-generate");
    this.generate.setOnAction(e -> this.generate());
    this.pdf.setId("chemistry-report-pdf");
    this.html.setId("chemistry-report-html");
    this.pdf.setOnAction(e -> this.choose(false));
    this.html.setOnAction(e -> this.choose(true));
    this.cancel.setId("chemistry-report-cancel");
    this.cancel.setOnAction(
        e -> {
          if (!this.exporting) {
            this.invalidate();
            this.status.setText("Подготовка отменена. Снимок опыта не изменён.");
          }
        });
    this.previous.setId("chemistry-report-previous");
    this.next.setId("chemistry-report-next");
    this.previous.setAccessibleText("Предыдущая страница");
    this.next.setAccessibleText("Следующая страница");
    this.previous.setOnAction(e -> this.showPage(this.pageIndex - 1));
    this.next.setOnAction(e -> this.showPage(this.pageIndex + 1));
    this.image.setPreserveRatio(true);
    this.image.setSmooth(true);
    this.image.setId("chemistry-report-preview");
    final StackPane paper = new StackPane(this.image);
    paper.setPadding(new Insets(12));
    paper.setStyle("-fx-background-color: #dce3eb;");
    paper.setMinSize(0, 0);
    final ScrollPane preview = new ScrollPane(paper);
    preview.setFitToWidth(true);
    preview.setPannable(true);
    preview.setId("chemistry-report-scroll");
    this.image
        .fitWidthProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(150, Math.min(900, preview.getViewportBounds().getWidth() - 26)),
                preview.viewportBoundsProperty()));
    final BorderPane previewPage = new BorderPane(preview);
    final FlowPane navigation = new FlowPane(10, 6, this.previous, this.pageLabel, this.next);
    navigation.setPadding(new Insets(8, 12, 8, 12));
    previewPage.setTop(navigation);
    this.text.setEditable(false);
    this.text.setWrapText(true);
    this.text.setId("chemistry-report-text");
    final TabPane tabs =
        new TabPane(
            new Tab("Страницы PDF", previewPage), new Tab("Текст для копирования", this.text));
    tabs.setMinHeight(0);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.setId("chemistry-report-tabs");
    final VBox body = new VBox(8, options, tabs);
    VBox.setVgrow(tabs, Priority.ALWAYS);
    this.setCenter(body);
    final VBox footer =
        new VBox(
            7,
            new FlowPane(8, 6, this.generate, this.pdf, this.html, this.cancel),
            this.source,
            this.status);
    footer.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(footer);
    this.controls();
    Platform.runLater(this::generate);
  }

  private static CheckBox option(final String title, final boolean selected, final String id) {
    final CheckBox c = new CheckBox(title);
    c.setSelected(selected);
    c.setId("chemistry-report-" + id);
    return c;
  }

  private Options options() {
    final Map<String, String> refs = new LinkedHashMap<>();
    this.references.forEach(
        (id, combo) -> refs.put(id, combo.getValue() == null ? "" : combo.getValue().id()));
    return new Options(
        this.structures.isSelected(),
        this.quantities.isSelected(),
        this.procedure.isSelected(),
        this.analyses.isSelected(),
        this.journal.isSelected(),
        this.attachments.isSelected(),
        this.diagnostics.isSelected(),
        this.superseded.isSelected(),
        this.strict.isSelected(),
        refs);
  }

  private void invalidate() {
    if (this.exporting) {
      return;
    }
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.bundle = null;
    this.busy(false);
    this.image.setImage(null);
    this.text.clear();
    this.pageLabel.setText("Предпросмотр не сформирован");
    this.controls();
    this.status.setText("Состав изменён. Нажмите «Обновить отчёт».");
  }

  private void generate() {
    if (this.disposed || this.exporting) {
      return;
    }
    this.invalidate();
    final int version = this.generation;
    final var options = this.options();
    this.busy(true);
    this.controls();
    this.status.setText("Подготовка содержания…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var report = LaboratoryExperimentReport.create(this.drawing, options);
                final String qchem = new LaboratoryFileCodec().write(this.drawing);
                final String fingerprint =
                    HexFormat.of()
                        .formatHex(
                            java.security.MessageDigest.getInstance("SHA-256")
                                .digest(qchem.getBytes(StandardCharsets.UTF_8)));
                final var figures =
                    report.blocks().stream()
                        .filter(Figure.class::isInstance)
                        .map(Figure.class::cast)
                        .map(Figure::key)
                        .distinct()
                        .toList();
                if (figures.size() > 128) {
                  throw new IOException("Более 128 схем. Уменьшите состав отчёта.");
                }
                Platform.runLater(
                    () ->
                        this.capture(
                            report, figures, new LinkedHashMap<>(), 0, fingerprint, version));
              } catch (final Exception e) {
                this.failed(version, e);
              }
            });
  }

  private void capture(
      final Document report,
      final List<String> keys,
      final Map<String, byte[]> images,
      final int index,
      final String hash,
      final int version) {
    if (this.disposed || version != this.generation) {
      return;
    }
    try {
      if (index < keys.size()) {
        this.status.setText("Схемы: " + (index + 1) + " / " + keys.size());
        images.put(keys.get(index), LaboratoryReportFigures.create(this.drawing, keys.get(index)));
        if (images.values().stream().mapToLong(a -> a.length).sum() > 64L * 1024 * 1024) {
          throw new IOException("Схемы превышают 64 МиБ. Уменьшите состав отчёта.");
        }
        Platform.runLater(() -> this.capture(report, keys, images, index + 1, hash, version));
        return;
      }
      this.status.setText("Вёрстка страниц и проверка шрифтов…");
      this.task =
          this.worker.submit(
              () -> {
                try {
                  final var result =
                      LaboratoryReportFiles.generate(
                          report,
                          images,
                          hash,
                          Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
                  Platform.runLater(
                      () -> {
                        if (this.disposed || version != this.generation) {
                          return;
                        }
                        this.bundle = result;
                        this.text.setText(plain(report));
                        this.busy(false);
                        this.controls();
                        this.showPage(0);
                        this.source.setText(
                            "Снимок опыта · "
                                + result.pages()
                                + " стр. · "
                                + result.createdAt()
                                + " · экспорт не изменяет QCHEM");
                      });
                } catch (final Exception e) {
                  this.failed(version, e);
                }
              });
    } catch (final Exception e) {
      this.failed(version, e);
    }
  }

  private static String plain(final Document report) {
    final StringBuilder out = new StringBuilder(report.title()).append('\n');
    for (final var block : report.blocks()) {
      switch (block) {
        case Heading h -> out.append('\n').append(h.text()).append('\n');
        case Paragraph p -> out.append(p.text()).append('\n');
        case Figure f -> {
          if (!f.caption().isBlank()) {
            out.append('[').append(f.caption()).append("]\n");
          }
        }
        case Table t -> {
          out.append(String.join(" | ", t.headers())).append('\n');
          for (final var row : t.rows()) {
            out.append(String.join(" | ", row)).append('\n');
          }
        }
      }
    }
    return out.toString();
  }

  private void failed(final int version, final Exception e) {
    Platform.runLater(
        () -> {
          if (!this.disposed && version == this.generation) {
            this.busy(false);
            this.exporting = false;
            this.controls();
            this.status.setText(Objects.toString(e.getMessage(), e.getClass().getSimpleName()));
          }
        });
  }

  private void showPage(final int index) {
    if (this.bundle == null || this.exporting || index < 0 || index >= this.bundle.pages()) {
      return;
    }
    if (this.task != null) {
      this.task.cancel(true);
    }
    final int version = ++this.generation;
    final var snapshot = this.bundle;
    this.pageIndex = index;
    this.busy(true);
    this.controls();
    this.pageLabel.setText((index + 1) + " / " + snapshot.pages());
    this.task =
        this.worker.submit(
            () -> {
              try {
                final byte[] preview = LaboratoryReportFiles.preview(snapshot, index, 110);
                Platform.runLater(
                    () -> {
                      if (!this.disposed && version == this.generation) {
                        this.image.setImage(new Image(new ByteArrayInputStream(preview)));
                        this.busy(false);
                        this.controls();
                        this.status.setText(
                            "Предпросмотр готов. PDF сохраняется с этой же вёрсткой; HTML содержит тот же выбранный состав.");
                      }
                    });
              } catch (final Exception e) {
                this.failed(version, e);
              }
            });
  }

  private void controls() {
    this.pdf.setDisable(this.bundle == null || this.running);
    this.html.setDisable(this.bundle == null || this.running);
    this.generate.setDisable(this.exporting);
    this.cancel.setDisable(!this.running || this.exporting);
    this.previous.setDisable(this.bundle == null || this.running || this.pageIndex <= 0);
    this.next.setDisable(
        this.bundle == null || this.running || this.pageIndex + 1 >= this.bundle.pages());
    this.lookupAll(".check-box").forEach(n -> n.setDisable(this.exporting));
    this.references.values().forEach(n -> n.setDisable(this.exporting));
  }

  private void choose(final boolean html) {
    if (this.bundle == null || this.running) {
      return;
    }
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Сохранить отчёт по опыту");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                html ? "HTML со встроенными схемами" : "Документ PDF", html ? "*.html" : "*.pdf"));
    chooser.setInitialFileName("experiment-report" + (html ? ".html" : ".pdf"));
    final File selected =
        chooser.showSaveDialog(this.getScene() == null ? null : this.getScene().getWindow());
    if (selected != null) {
      this.exportTo(selected.toPath(), html);
    }
  }

  void exportTo(final Path path, final boolean html) {
    if (this.bundle == null || this.running) {
      return;
    }
    final var snapshot = this.bundle;
    final int version = ++this.generation;
    this.exporting = true;
    this.busy(true);
    this.controls();
    this.status.setText("Сохранение отчёта…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                LaboratoryReportFiles.export(path, snapshot, html);
                Platform.runLater(
                    () -> {
                      if (!this.disposed && version == this.generation) {
                        this.exporting = false;
                        this.busy(false);
                        this.controls();
                        this.status.setText("Сохранено: " + path.toAbsolutePath());
                      }
                    });
              } catch (final Exception e) {
                this.failed(version, e);
              }
            });
  }

  @Override
  boolean canClose() {
    if (this.exporting) {
      this.status.setText("Дождитесь окончания записи отчёта.");
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.worker.shutdownNow();
    this.bundle = null;
    this.image.setImage(null);
    this.busy(false);
  }
}
