/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryPredictionGroups;

/** Shared browser for fresh and archived candidates; grouping never replaces an original rank. */
final class LaboratoryPredictionBrowser extends BorderPane {
  private final ListView<List<Integer>> groups = new ListView<>();
  private final ComboBox<Integer> mappings = new ComboBox<>();
  private final HBox mappingControls = new HBox(6);
  private final Label status = new Label(), mappingNotice = new Label();
  private final VBox preview = new VBox(12);
  private final IntConsumer selected;
  private final BiConsumer<VBox, Integer> render;
  private List<Double> weights = List.of();
  private Thread worker;
  private long generation;

  LaboratoryPredictionBrowser(
      final String id, final IntConsumer selected, final BiConsumer<VBox, Integer> render) {
    this.selected = selected;
    this.render = render;
    this.setMinWidth(0);
    this.groups.setId(id);
    this.groups.setMinWidth(150);
    this.groups.setMaxWidth(240);
    this.groups
        .prefWidthProperty()
        .bind(
            javafx.beans.binding.Bindings.max(
                150, javafx.beans.binding.Bindings.min(240, this.widthProperty().multiply(0.27))));
    this.groups.setPlaceholder(new Label("Пока нет кандидатов"));
    this.groups.setStyle("-fx-font-family: Segoe UI; -fx-font-size: 13px;");
    this.groups.setCellFactory(
        view ->
            new ListCell<>() {
              protected void updateItem(final List<Integer> group, final boolean empty) {
                super.updateItem(group, empty);
                setText(
                    empty || group == null
                        ? null
                        : "Вариант "
                            + (getIndex() + 1)
                            + (group.size() == 1 ? "" : "\nСоответствий атомов: " + group.size()));
                setTooltip(
                    empty || group == null
                        ? null
                        : new Tooltip(
                            "Вес модели: "
                                + weights.get(group.getFirst())
                                + ". Не экспериментальная вероятность; веса соответствий не суммируются."));
              }
            });
    this.status.setWrapText(true);
    this.status.setMinHeight(Region.USE_PREF_SIZE);
    this.status.setId(id + "-grouping-status");
    this.mappingNotice.setWrapText(true);
    this.mappingNotice.setMinHeight(Region.USE_PREF_SIZE);
    this.mappings.setId(id + "-mapping");
    this.mappings.setMaxWidth(Double.MAX_VALUE);
    this.mappings.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final Integer rank) {
            return rank == null
                ? ""
                : "Исходный вариант "
                    + (rank + 1)
                    + String.format(Locale.ROOT, " · вес модели %.4f", weights.get(rank));
          }

          public Integer fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    final Button previous = new Button("←"), next = new Button("→");
    previous.setTooltip(new Tooltip("Предыдущее соответствие атомов"));
    next.setTooltip(new Tooltip("Следующее соответствие атомов"));
    previous.setAccessibleText("Предыдущее соответствие атомов");
    next.setAccessibleText("Следующее соответствие атомов");
    previous.setId(id + "-previous");
    next.setId(id + "-next");
    previous
        .disableProperty()
        .bind(this.mappings.getSelectionModel().selectedIndexProperty().lessThanOrEqualTo(0));
    next.disableProperty()
        .bind(
            javafx.beans.binding.Bindings.createBooleanBinding(
                () ->
                    this.mappings.getSelectionModel().getSelectedIndex() < 0
                        || this.mappings.getSelectionModel().getSelectedIndex()
                            >= this.mappings.getItems().size() - 1,
                this.mappings.getSelectionModel().selectedIndexProperty(),
                this.mappings.getItems()));
    previous.setOnAction(event -> this.mappings.getSelectionModel().selectPrevious());
    next.setOnAction(event -> this.mappings.getSelectionModel().selectNext());
    HBox.setHgrow(this.mappings, Priority.ALWAYS);
    this.mappings.setMinWidth(0);
    this.mappingControls.getChildren().addAll(previous, this.mappings, next);
    this.preview.setMinWidth(0);
    this.preview.setFillWidth(true);
    final VBox content = new VBox(10, this.mappingNotice, this.mappingControls, this.preview);
    content.setPadding(new Insets(12));
    content.setMinWidth(0);
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setMinWidth(0);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    this.setTop(this.status);
    BorderPane.setMargin(this.status, new Insets(0, 0, 8, 0));
    this.setLeft(this.groups);
    BorderPane.setMargin(this.groups, new Insets(0, 10, 0, 0));
    this.setCenter(scroll);
    this.groups
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, old, group) -> {
              this.mappings.getItems().clear();
              final boolean multiple = group != null && group.size() > 1;
              this.mappingNotice.setManaged(multiple);
              this.mappingNotice.setVisible(multiple);
              this.mappingControls.setManaged(multiple);
              this.mappingControls.setVisible(multiple);
              if (group == null) {
                return;
              }
              this.mappingNotice.setText(
                  "Одинаковые структуры объединены. Основания и соответствия атомов сохранены в "
                      + group.size()
                      + " исходных вариантах. В план добавится выбранный ниже вариант; веса не суммируются.");
              this.mappings.getItems().setAll(group);
              this.mappings.getSelectionModel().selectFirst();
            });
    this.mappings
        .valueProperty()
        .addListener(
            (observable, old, rank) -> {
              this.preview.getChildren().clear();
              this.selected.accept(rank == null ? -1 : rank);
              if (rank != null) {
                this.render.accept(this.preview, rank);
              }
            });
    this.clear();
  }

  void clear() {
    this.dispose();
    this.groups.getItems().clear();
    this.mappings.getItems().clear();
    this.preview.getChildren().clear();
    this.status.setText("Кандидаты появятся после выполнения прогноза.");
    this.mappingNotice.setManaged(false);
    this.mappingNotice.setVisible(false);
    this.mappingControls.setManaged(false);
    this.mappingControls.setVisible(false);
    this.selected.accept(-1);
  }

  void load(final List<ReactionSide> products, final List<Double> weights) {
    if (products.size() != weights.size()) {
      throw new IllegalArgumentException("Candidate products and weights must have the same size.");
    }
    this.clear();
    this.weights = List.copyOf(weights);
    final long current = this.generation;
    if (products.isEmpty()) {
      this.status.setText("Прогноз не содержит кандидатов.");
      return;
    }
    this.status.setText("Сопоставляю структуры продуктов…");
    this.worker =
        new Thread(
            () -> {
              try {
                final var grouped = LaboratoryPredictionGroups.of(products);
                Platform.runLater(
                    () -> {
                      if (current != this.generation) {
                        return;
                      }
                      this.groups.getItems().setAll(grouped.groups());
                      this.status.setText(
                          "Групп структур: "
                              + grouped.groups().size()
                              + " · исходных вариантов: "
                              + products.size()
                              + (grouped.unresolvedComparisons() == 0
                                  ? ""
                                  : ". Часть сравнений не завершена; неподтверждённые совпадения оставлены отдельно."));
                      this.groups.getSelectionModel().selectFirst();
                    });
              } catch (final java.util.concurrent.CancellationException ignored) {
                // Closing or replacing the result invalidates this display-only computation.
              } catch (final RuntimeException exception) {
                Platform.runLater(
                    () -> {
                      if (current != this.generation) {
                        return;
                      }
                      this.status.setText(
                          "Группировка не завершена: "
                              + exception.getMessage()
                              + ". Показаны все исходные варианты.");
                      for (int rank = 0; rank < products.size(); rank++) {
                        this.groups.getItems().add(List.of(rank));
                      }
                      this.groups.getSelectionModel().selectFirst();
                    });
              }
            },
            "laboratory-prediction-groups");
    this.worker.setDaemon(true);
    this.worker.start();
  }

  void dispose() {
    this.generation++;
    if (this.worker != null) {
      this.worker.interrupt();
      this.worker = null;
    }
  }
}
