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
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryCalculationJob;

final class LaboratoryCalculationWindow extends LaboratoryComputationPage {
  private LaboratoryCalculationJob job;
  private LaboratoryCalculationJob.Result latest;
  private final ComboBox<ClassicalCalculationKind> kind = new ComboBox<>();
  private final TextField iterations = new TextField("128");
  private final TextArea report = new TextArea();
  private final Button start = new Button("Выполнить"),
      cancel = new Button("Остановить"),
      apply = new Button("Применить геометрию");
  private final Label eligibility = note("");
  private final LaboratoryGeometryPane preview = new LaboratoryGeometryPane(atom -> {});
  private final LaboratoryComponent component;
  private final String experimentId;

  LaboratoryCalculationWindow(
      final LaboratoryComponent component,
      final String experimentId,
      final Path directory,
      final Consumer<LaboratoryCalculationJob.Result> completed,
      final Consumer<Molecule> applied,
      final ClassicalCalculationKind initialKind,
      final Runnable close) {
    super("Расчёт структуры", close);
    this.component = component;
    this.experimentId = experimentId;
    this.setId("chemistry-calculation-page");
    this.kind
        .getItems()
        .setAll(
            ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.GEOMETRY_OPTIMIZATION);
    this.kind.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final ClassicalCalculationKind value) {
            return value == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
                ? "Состав и дескрипторы"
                : value == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
                    ? "Релаксация 3D · приближённая модель"
                    : "Энергия HF / STO-3G";
          }

          public ClassicalCalculationKind fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.kind.setValue(initialKind);
    this.kind.setId("chemistry-calculation-kind");
    this.kind.setMaxWidth(Double.MAX_VALUE);
    this.iterations.setId("chemistry-calculation-iterations");
    this.iterations.setPrefColumnCount(6);
    final FlowPane settings = new FlowPane(8, 6, note("Лимит итераций (1–4096)"), this.iterations);
    settings
        .visibleProperty()
        .bind(this.kind.valueProperty().isEqualTo(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));
    settings.managedProperty().bind(settings.visibleProperty());
    this.report.setEditable(false);
    this.report.setWrapText(true);
    this.report.setId("chemistry-calculation-report");
    this.report.setPromptText("Здесь появится отчёт выбранного запуска.");
    final TabPane results =
        new TabPane(
            new Tab("Отчёт", this.report), new Tab("Геометрия результата", scroll(this.preview)));
    results.setId("chemistry-calculation-results");
    results.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    results.setMinSize(0, 160);
    final TitledPane parameters =
        new TitledPane(
            "Метод и входные данные",
            new VBox(
                8,
                this.kind,
                settings,
                note(
                    "Расчёт использует явные атомы, заряд, спин и координаты. Проверьте водороды. Начальная раскладка и релаксация выполняются отдельно."),
                this.eligibility));
    parameters.setId("chemistry-calculation-parameters");
    parameters.setAnimated(false);
    parameters.setExpanded(true);
    results
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener(
            (o, a, b) -> {
              if (b.intValue() == 1) {
                parameters.setExpanded(false);
              }
            });
    final VBox content =
        new VBox(
            8,
            note(
                component.molecule().displayName()
                    + " · "
                    + component.molecule().atoms().size()
                    + " явных атомов"),
            parameters,
            results);
    content.setPadding(new javafx.geometry.Insets(0, 16, 0, 16));
    VBox.setVgrow(results, Priority.ALWAYS);
    this.setCenter(content);
    this.start.setId("chemistry-calculation-start");
    this.cancel.setId("chemistry-calculation-cancel");
    this.apply.setId("chemistry-calculation-apply");
    this.start.getStyleClass().add("laboratory-save");
    this.cancel.setDisable(true);
    this.apply.setDisable(true);
    final VBox footer =
        new VBox(8, this.status, new FlowPane(8, 6, this.start, this.cancel, this.apply));
    footer.setPadding(new javafx.geometry.Insets(12, 16, 14, 16));
    this.setBottom(footer);
    this.kind.valueProperty().addListener((o, a, b) -> this.validate());
    this.iterations.textProperty().addListener((o, a, b) -> this.validate());
    this.start.setOnAction(
        event -> {
          if (this.running || this.disposed) {
            return;
          }
          try {
            final LaboratoryCalculationJob submitted = this.createJob();
            this.job = submitted;
            this.latest = null;
            this.apply.setDisable(true);
            this.preview.load(null, null);
            this.report.clear();
            this.busy(true);
            this.start.setDisable(true);
            this.kind.setDisable(true);
            this.iterations.setDisable(true);
            this.cancel.setDisable(false);
            this.status.setText("Расчёт выполняется…");
            results.getSelectionModel().selectFirst();
            final Thread worker =
                new Thread(
                    () -> {
                      try {
                        final var result = submitted.execute(directory);
                        Platform.runLater(
                            () -> {
                              if (this.disposed || this.job != submitted) {
                                return;
                              }
                              this.report.setText(
                                  LaboratoryCalculationPresentation.report(result.report()));
                              try {
                                completed.accept(result);
                                this.latest = result;
                                this.preview.load(result.geometry(), null);
                                this.apply.setDisable(result.geometry() == null);
                                this.apply.setText(
                                    result.geometry() == null || result.geometryConverged()
                                        ? "Применить геометрию"
                                        : "Применить последнюю итерацию (не сошлось)");
                                this.status.setText(
                                    statusText(result.status())
                                        + ". Результат приложен к опыту."
                                        + (result.geometry() == null
                                            ? ""
                                            : result.geometryConverged()
                                                ? " Сходимость достигнута; модель приближённая."
                                                : " Сходимость не достигнута: доступна последняя итерация."));
                              } catch (final RuntimeException exception) {
                                this.status.setText(
                                    "Снимок сохранён: "
                                        + result.file()
                                        + ". Не приложен: "
                                        + exception.getMessage());
                              }
                            });
                      } catch (final Exception exception) {
                        Platform.runLater(
                            () -> {
                              if (!this.disposed) {
                                this.status.setText(
                                    "Расчёт не завершён: " + exception.getMessage());
                              }
                            });
                      } finally {
                        Platform.runLater(
                            () -> {
                              if (!this.disposed && this.job == submitted) {
                                this.busy(false);
                                this.kind.setDisable(false);
                                this.iterations.setDisable(false);
                                this.cancel.setDisable(true);
                                this.validate();
                              }
                            });
                      }
                    },
                    "laboratory-calculation");
            worker.setDaemon(true);
            worker.start();
          } catch (final RuntimeException exception) {
            this.status.setText(exception.getMessage());
            this.validate();
          }
        });
    this.cancel.setOnAction(
        event -> {
          if (this.running && this.job != null) {
            this.job.cancel();
            this.cancel.setDisable(true);
            this.status.setText("Остановка запрошена. Ожидаю завершения текущего шага ядра…");
          }
        });
    this.apply.setOnAction(
        event -> {
          if (this.running || this.latest == null || this.latest.geometry() == null) {
            return;
          }
          try {
            applied.accept(this.latest.geometry());
            this.finish();
          } catch (final RuntimeException exception) {
            this.status.setText(exception.getMessage());
          }
        });
    this.status.setText("Выберите расчёт и нажмите «Выполнить».");
    this.validate();
  }

  private LaboratoryCalculationJob createJob() {
    final int limit;
    try {
      limit =
          this.kind.getValue() == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
              ? Integer.parseInt(this.iterations.getText().trim())
              : 128;
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Лимит итераций — целое число от 1 до 4096.");
    }
    return new LaboratoryCalculationJob(
        this.component.molecule(),
        this.experimentId,
        this.component.id(),
        this.kind.getValue(),
        limit);
  }

  private void validate() {
    if (this.running) {
      return;
    }
    try {
      this.createJob();
      this.eligibility.setText(
          "Входные данные приняты. Покрытие метода и сходимость будут указаны в отчёте.");
      this.start.setDisable(false);
    } catch (final IllegalArgumentException exception) {
      this.eligibility.setText(exception.getMessage());
      this.start.setDisable(true);
    }
  }

  private static String statusText(final String status) {
    return switch (status) {
      case "SUCCEEDED" -> "Расчёт завершён";
      case "CANCELLED" -> "Расчёт остановлен";
      case "FAILED" -> "Расчёт завершился ошибкой";
      case "PARTIAL" -> "Расчёт выполнен с ограничениями";
      default -> "Статус: " + status;
    };
  }

  @Override
  void dispose() {
    super.dispose();
    if (this.running && this.job != null) {
      this.job.cancel();
    }
  }
}
