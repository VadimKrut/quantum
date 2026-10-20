/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryPredictionJob;

final class LaboratoryPredictionWindow extends LaboratoryComputationPage {
  private LaboratoryPredictionJob activeJob;
  private LaboratoryPredictionBrowser activeBrowser;

  @Override
  void dispose() {
    super.dispose();
    if (this.activeJob != null && this.running) {
      this.activeJob.cancel();
    }
    if (this.activeBrowser != null) {
      this.activeBrowser.dispose();
    }
  }

  LaboratoryPredictionWindow(
      final LaboratoryEditor editor,
      final LaboratoryReactionStage stage,
      final Path directory,
      final Consumer<LaboratoryPredictionJob.Result> completed,
      final Runnable close) {
    super("Прогноз продуктов · " + stage.title(), close);
    this.setId("chemistry-prediction-page");
    final var snapshot = editor.document();
    final Label input =
        new Label("Исходники и условия — из выбранной стадии. Коэффициенты задаются явно.");
    input.setWrapText(true);
    input.setMinHeight(Region.USE_PREF_SIZE);
    final ComboBox<OrganicReactionDomain> domain = new ComboBox<>();
    domain.getItems().setAll(OrganicReactionDomain.values());
    domain.setValue(OrganicReactionDomain.UNKNOWN);
    domain.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final OrganicReactionDomain value) {
            return domainName(value);
          }

          public OrganicReactionDomain fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    domain.setId("chemistry-prediction-domain");
    domain.setMaxWidth(Double.MAX_VALUE);
    final Label limits =
        new Label("Плановые гипотезы · Фактические количества и расход реагентов не меняются.");
    limits.setWrapText(true);
    limits.setMinHeight(Region.USE_PREF_SIZE);
    final Button start = new Button("Выполнить прогноз"),
        cancel = new Button("Остановить"),
        apply = new Button("Добавить вариант в план");
    cancel.setDisable(true);
    apply.setDisable(true);
    final Label status = this.status;
    status.setText("Выберите область реакции и запустите прогноз.");
    final TextArea report = new TextArea();
    report.setEditable(false);
    report.setWrapText(true);
    report.setId("chemistry-prediction-report");
    final LaboratoryPredictionJob[] job = {null};
    final LaboratoryPredictionJob.Result[] result = {null};
    final int[] selected = {-1};
    final LaboratoryPredictionBrowser browser =
        new LaboratoryPredictionBrowser(
            "chemistry-prediction-candidates",
            rank -> {
              selected[0] = rank;
              apply.setDisable(
                  rank < 0
                      || result[0] == null
                      || result[0].status().equals("FAILED")
                      || result[0].status().equals("CANCELLED"));
            },
            (preview, rank) -> showCandidate(preview, result[0].candidates().get(rank), rank));
    final TabPane tabs = new TabPane(new Tab("Кандидаты", browser), new Tab("Диагностика", report));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.activeBrowser = browser;
    start.setId("chemistry-prediction-start");
    cancel.setId("chemistry-prediction-cancel");
    apply.setId("chemistry-prediction-apply");
    tabs.setId("chemistry-prediction-tabs");
    final TextArea reviewText = new TextArea();
    reviewText.setEditable(false);
    reviewText.setWrapText(true);
    reviewText.setPrefRowCount(5);
    reviewText.setId("chemistry-prediction-input-review");
    final Tab review = new Tab("Проверка входа", reviewText);
    tabs.getTabs().add(1, review);
    reviewText.setText(
        "После прогноза здесь появится сводка ограничений и недостающих входных данных.");
    start.setOnAction(
        event -> {
          if (this.running || this.disposed) {
            return;
          }
          try {
            job[0] = new LaboratoryPredictionJob(snapshot, stage, domain.getValue());
            this.activeJob = job[0];
            result[0] = null;
            browser.clear();
            report.clear();
            reviewText.setText("Проверяю входные данные…");
            this.busy(true);
            start.setDisable(true);
            domain.setDisable(true);
            cancel.setDisable(false);
            apply.setDisable(true);
            status.setText("Ядро строит и оценивает кандидатов…");
            final Thread worker =
                new Thread(
                    () -> {
                      try {
                        final var value = job[0].execute(directory);
                        Platform.runLater(
                            () -> {
                              if (this.disposed) {
                                return;
                              }
                              report.setText(value.report());
                              reviewText.setText(value.inputReview());
                              try {
                                completed.accept(value);
                                result[0] = value;
                                browser.load(
                                    value.candidates().stream()
                                        .map(ReactionProductCandidate::products)
                                        .toList(),
                                    value.candidates().stream()
                                        .map(ReactionProductCandidate::probability)
                                        .toList());
                                tabs.getSelectionModel()
                                    .select(value.candidates().isEmpty() ? 1 : 0);
                                status.setText(
                                    switch (value.status()) {
                                      case "SUCCESS" ->
                                          "Прогноз сохранён и приложен к опыту. Выберите вариант для просмотра.";
                                      case "CANCELLED" ->
                                          "Прогноз остановлен. Кандидаты не применены; запись остановки сохранена.";
                                      case "PARTIAL" ->
                                          "Прогноз частичный. Проверьте ограничения и диагностику; снимок сохранён.";
                                      default ->
                                          "Ядро не завершило прогноз. Диагностика и входной снимок сохранены.";
                                    });
                                if (!value.candidates().isEmpty()) {
                                  status.setText(
                                      status.getText()
                                          + " Уверенность модели: "
                                          + confidenceName(value.confidence())
                                          + ".");
                                }
                              } catch (final RuntimeException exception) {
                                status.setText(
                                    "Результат сохранён в "
                                        + value.file()
                                        + ", но не приложен: "
                                        + exception.getMessage());
                              }
                            });
                      } catch (final Exception exception) {
                        Platform.runLater(
                            () -> {
                              if (!this.disposed) {
                                status.setText("Прогноз не завершён: " + exception.getMessage());
                              }
                            });
                      } finally {
                        Platform.runLater(
                            () -> {
                              if (this.disposed) {
                                return;
                              }
                              this.busy(false);
                              start.setDisable(false);
                              domain.setDisable(false);
                              cancel.setDisable(true);
                            });
                      }
                    },
                    "laboratory-prediction");
            worker.setDaemon(true);
            worker.start();
          } catch (final RuntimeException exception) {
            status.setText(exception.getMessage());
          }
        });
    cancel.setOnAction(
        event -> {
          job[0].cancel();
          cancel.setDisable(true);
          status.setText(
              "Остановка запрошена. Ожидаю завершения текущего шага ядра; кандидаты не будут применены.");
        });
    apply.setOnAction(
        event -> {
          try {
            job[0].addToPlan(editor, result[0], selected[0]);
            this.finish();
          } catch (final RuntimeException exception) {
            status.setText(exception.getMessage());
          }
        });
    final FlowPane actions = new FlowPane(8, 6, start, cancel, apply);
    final VBox content = new VBox(8, input, domain, limits, tabs);
    VBox.setVgrow(tabs, Priority.ALWAYS);
    content.setPadding(new javafx.geometry.Insets(0, 16, 0, 16));
    tabs.setMinHeight(130);
    this.setCenter(content);
    final VBox footer = new VBox(8, status, actions);
    footer.setPadding(new javafx.geometry.Insets(12, 16, 14, 16));
    this.setBottom(footer);
  }

  static javafx.scene.Node savedView(
      final LaboratoryEditor editor,
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryPredictionArchive
          archive,
      final String source,
      final Runnable close) {
    final Label status = new Label();
    status.setWrapText(true);
    status.setMinHeight(Region.USE_PREF_SIZE);
    status.setId("chemistry-saved-prediction-status");
    final boolean current = archive.current(editor.document());
    final String availability =
        !current
            ? "Исходники или стадия отличаются от снимка. Доступен просмотр; применение заблокировано."
            : archive.applicable(editor.document())
                ? "Исходная стадия соответствует снимку. Можно добавить выбранный вариант в план."
                : "Прогноз не содержит доступных для применения вариантов.";
    status.setText(
        availability
            + "\nСтатус: "
            + statusName(archive.status())
            + " · Уверенность модели: "
            + (archive.hasConfidence() ? confidenceName(archive.confidence()) : "не сохранена")
            + ".");
    if (!current) {
      status.setStyle("-fx-text-fill: -app-text;");
    }
    final Button apply = new Button("Добавить вариант в план");
    apply.setDisable(true);
    final int[] selected = {-1};
    final LaboratoryPredictionBrowser browser =
        new LaboratoryPredictionBrowser(
            "chemistry-saved-prediction-candidates",
            rank -> {
              selected[0] = rank;
              apply.setDisable(rank < 0 || !archive.applicable(editor.document()));
            },
            (preview, rank) -> {
              final var candidate = archive.candidates().get(rank);
              final var record = candidate.record();
              final StringBuilder details = new StringBuilder();
              if (record.note() != null) {
                details.append(record.note()).append("\n\n");
              }
              details.append(
                  String.format(
                      Locale.ROOT,
                      "Условный выход модели: %.2f%%. Это эвристическая оценка, не измеренный выход.\n\n",
                      record.expectedYieldPercent()));
              details
                  .append("Сохранённые основания:\n")
                  .append(String.join("\n", record.evidence()));
              details.append("\n\nОценки:\n").append(String.join("\n", record.scoreComponents()));
              details
                  .append("\n\nМеханизмы:\n")
                  .append(String.join("\n", record.mechanismDescriptors()));
              showCandidate(preview, candidate.products(), details.toString(), rank);
            });

    apply.setOnAction(
        event -> {
          try {
            archive.addToPlan(editor, selected[0]);
            browser.dispose();
            close.run();
          } catch (final RuntimeException exception) {
            status.setText(exception.getMessage());
            apply.setDisable(true);
          }
        });
    final TextArea report = new TextArea(archive.report());
    report.setEditable(false);
    report.setWrapText(true);
    final TextArea input = new TextArea(savedInput(archive));
    input.setEditable(false);
    input.setWrapText(true);
    input.setId("chemistry-saved-prediction-input");
    final TextArea raw = new TextArea(source);
    raw.setEditable(false);
    raw.setWrapText(false);
    final TabPane tabs =
        new TabPane(
            new Tab("Кандидаты", browser),
            new Tab("Проверка входа", reviewArea(archive.inputReview())),
            new Tab("Исходная стадия", input),
            new Tab("Диагностика", report),
            new Tab("Исходный QCHEM", raw));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    final Label notice =
        new Label(
            "Сохранённый результат · Вес модели не является экспериментальной вероятностью. Добавляемые продукты — плановые гипотезы.");
    notice.setWrapText(true);
    notice.setMinHeight(Region.USE_PREF_SIZE);
    final Button back = new Button("← К опыту");
    back.setId("chemistry-saved-prediction-back");
    back.setOnAction(
        event -> {
          browser.dispose();
          close.run();
        });
    apply.setId("chemistry-saved-prediction-apply");
    tabs.setId("chemistry-saved-prediction-tabs");
    final BorderPane page = new BorderPane();
    page.setId("chemistry-saved-prediction-page");
    final Label title = new Label(archive.title());
    title.setWrapText(true);
    title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    final VBox top = new VBox(8, new FlowPane(12, 8, back, title), notice, status);
    top.setPadding(new javafx.geometry.Insets(16));
    page.setTop(top);
    page.setCenter(tabs);
    BorderPane.setMargin(tabs, new javafx.geometry.Insets(0, 16, 0, 16));
    final FlowPane footer = new FlowPane(apply);
    footer.setPadding(new javafx.geometry.Insets(12, 16, 14, 16));
    page.setBottom(footer);
    browser.load(
        archive.candidates().stream().map(candidate -> candidate.products()).toList(),
        archive.candidates().stream().map(candidate -> candidate.record().probability()).toList());
    if (archive.candidates().isEmpty()) {
      tabs.getSelectionModel().select(1);
    }
    page.sceneProperty()
        .addListener(
            (o, a, b) -> {
              if (a != null && b == null) {
                browser.dispose();
              }
            });
    return page;
  }

  private static TextArea reviewArea(final String text) {
    final TextArea area = new TextArea(text);
    area.setEditable(false);
    area.setWrapText(true);
    return area;
  }

  private static String savedInput(
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryPredictionArchive
          archive) {
    final var stage = archive.stage();
    final var conditions = stage.conditions();
    final StringBuilder text =
        new StringBuilder("Снимок стадии: ").append(stage.title()).append("\n\nУчастники:\n");
    for (final var participant : stage.participants()) {
      final var component =
          archive.input().components().stream()
              .filter(item -> item.id().equals(participant.componentId()))
              .findFirst()
              .orElseThrow();
      text.append(LaboratoryInspector.roleText(participant.role()))
          .append(" · ")
          .append(
              participant.coefficient() == null
                  ? "коэффициент не указан"
                  : participant.coefficient().value() + " ×")
          .append(' ')
          .append(component.molecule().displayName())
          .append(" · ")
          .append(component.molecule().formula().hillNotation())
          .append('\n');
    }
    text.append("\nТемпература: ")
        .append(
            conditions.temperature() == null
                ? "не указана"
                : conditions.temperature().value()
                    + (conditions.temperature().unit()
                            == ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit
                                .CELSIUS
                        ? " °C"
                        : " K"));
    text.append("\nДавление: ")
        .append(
            conditions.pressure() == null
                ? "не указано"
                : conditions.pressure().value() + " " + conditions.pressure().unit());
    text.append("\nДлительность: ")
        .append(
            conditions.duration() == null
                ? "не указана"
                : conditions.duration().value()
                    + " "
                    + switch (conditions.duration().unit()) {
                      case SECOND -> "с";
                      case MINUTE -> "мин";
                      case HOUR -> "ч";
                      case DAY -> "сут";
                    });
    text.append("\n\nКомпоненты условий:\n");
    if (conditions.components().isEmpty()) {
      text.append("Не указаны\n");
    }
    conditions
        .components()
        .forEach(
            component ->
                text.append(LaboratoryInspector.roleText(component.role()))
                    .append(" · ")
                    .append(component.name())
                    .append(component.note() == null ? "" : " · " + component.note())
                    .append('\n'));
    text.append("\nОсобенности условий:\n");
    if (conditions.features().isEmpty()) {
      text.append("Не указаны\n");
    }
    conditions
        .features()
        .forEach(
            feature ->
                text.append(feature.kind())
                    .append(feature.hasValue() ? " · " + feature.value() : "")
                    .append(feature.hasNote() ? " · " + feature.note() : "")
                    .append('\n'));
    if (conditions.note() != null) {
      text.append("\nПримечание:\n").append(conditions.note());
    }
    text.append(
        "\n\nПолный снимок с дополнительными полями компонентов доступен во вкладке «Исходный QCHEM».");
    return text.toString();
  }

  private static void showCandidate(
      final VBox preview, final ReactionProductCandidate candidate, final int rank) {
    final StringBuilder details = new StringBuilder();
    if (candidate.hasSelectivityNote()) {
      details.append(candidate.selectivityNote()).append("\n\n");
    }
    details.append(
        String.format(
            Locale.ROOT,
            "Условный выход модели: %.2f%%. Это эвристическая оценка, не измеренный выход.\n\n",
            candidate.expectedYieldPercent()));
    candidate
        .evidence()
        .forEach(
            evidence ->
                details
                    .append(evidence.kind())
                    .append(" · ")
                    .append(evidence.description())
                    .append('\n'));
    showCandidate(preview, candidate.products(), details.toString(), rank);
  }

  private static void showCandidate(
      final VBox preview,
      final ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide products,
      final String details,
      final int rank) {
    final Label title = new Label("Исходный вариант " + (rank + 1) + " · плановая гипотеза");
    title.setWrapText(true);
    title.setMinHeight(Region.USE_PREF_SIZE);
    title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    preview.getChildren().add(title);
    final LaboratoryMoleculePlacement placement = new LaboratoryMoleculePlacement();
    for (final var product : products.participants()) {
      final var drawing = placement.place(product.molecule(), 180, 120);
      final var bounds = LaboratoryCanvas.previewBounds(drawing);
      final double minX = bounds.getMinX(),
          maxX = bounds.getMaxX(),
          minY = bounds.getMinY(),
          maxY = bounds.getMaxY();
      final Canvas canvas = new Canvas(420, 280);
      final Runnable paint =
          () -> {
            final double width = canvas.getWidth();
            final double height =
                Math.max(
                    120,
                    Math.min(
                        220,
                        16 + bounds.getHeight() * Math.min(2, (width - 16) / bounds.getWidth())));
            canvas.setHeight(height);
            final double scale =
                Math.min(
                    2,
                    Math.min((width - 16) / bounds.getWidth(), (height - 16) / bounds.getHeight()));
            final var graphics = canvas.getGraphicsContext2D();
            graphics.setTransform(1, 0, 0, 1, 0, 0);
            graphics.setFill(javafx.scene.paint.Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.translate(
                width / 2 - scale * (minX + maxX) / 2, height / 2 - scale * (minY + maxY) / 2);
            graphics.scale(scale, scale);
            LaboratoryCanvas.paintPreview(graphics, drawing);
          };
      canvas.widthProperty().addListener((observable, old, width) -> paint.run());
      canvas
          .widthProperty()
          .bind(
              javafx.beans.binding.Bindings.max(
                  100, javafx.beans.binding.Bindings.min(420, preview.widthProperty())));
      paint.run();
      final Label name =
          new Label(
              product.coefficient().value()
                  + " × "
                  + product.molecule().displayName()
                  + " · "
                  + product.molecule().formula().hillNotation());
      name.setWrapText(true);
      name.setMaxWidth(Double.MAX_VALUE);
      name.setMinHeight(Region.USE_PREF_SIZE);
      preview.getChildren().addAll(name, canvas);
    }
    final Label plan =
        new Label(
            "Добавление создаст отдельную стадию и плановые компоненты без фактических количеств. Исходная стадия сохранится.");
    plan.setWrapText(true);
    plan.setMaxWidth(Double.MAX_VALUE);
    plan.setMinHeight(Region.USE_PREF_SIZE);
    preview.getChildren().add(plan);
    final TextArea evidence = new TextArea(details);
    evidence.setEditable(false);
    evidence.setWrapText(true);
    evidence.setPrefRowCount(8);
    final TitledPane explanation = new TitledPane("Основания и ограничения модели", evidence);
    explanation.setExpanded(false);
    preview.getChildren().add(explanation);
  }

  private static String statusName(final String status) {
    return switch (status) {
      case "SUCCESS" -> "выполнен";
      case "PARTIAL" -> "частичный";
      case "FAILED" -> "ошибка";
      case "CANCELLED" -> "остановлен";
      default -> status;
    };
  }

  private static String confidenceName(final ReactionPredictionConfidenceLevel confidence) {
    return switch (confidence) {
      case NONE -> "не определена";
      case LOW -> "низкая";
      case MEDIUM -> "средняя";
      case HIGH -> "высокая";
      case DECISIVE -> "очень высокая";
    };
  }

  private static String domainName(final OrganicReactionDomain domain) {
    if (domain == null) {
      return "";
    }
    return switch (domain) {
      case HETEROCYCLE_SYNTHESIS -> "Синтез гетероциклов";
      case HETEROCYCLE_FUNCTIONALIZATION -> "Функционализация гетероциклов";
      case CROSS_COUPLING -> "Кросс-сочетание";
      case RADICAL_FUNCTIONALIZATION -> "Радикальная функционализация";
      case NUCLEOPHILIC_SUBSTITUTION -> "Нуклеофильное замещение";
      case ELECTROPHILIC_SUBSTITUTION -> "Электрофильное замещение";
      case ADDITION -> "Присоединение";
      case ELIMINATION -> "Элиминирование";
      case CYCLIZATION -> "Циклизация";
      case OXIDATION_REDUCTION -> "Окисление / восстановление";
      case ACID_BASE -> "Кислотно-основные превращения";
      case PROTECTION_DEPROTECTION -> "Защита / снятие защиты";
      case REARRANGEMENT -> "Перегруппировка";
      case UNKNOWN -> "Область не указана";
    };
  }
}
