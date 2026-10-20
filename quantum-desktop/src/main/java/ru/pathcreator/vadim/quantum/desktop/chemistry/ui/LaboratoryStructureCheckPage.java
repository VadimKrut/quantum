/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

final class LaboratoryStructureCheckPage extends LaboratoryComputationPage {
  LaboratoryStructureCheckPage(
      final LaboratoryDrawing document,
      final java.util.function.Consumer<String> select,
      final java.util.function.Consumer<String> hydrogens,
      final Runnable close) {
    super("Проверка структуры", close);
    this.setId("chemistry-structure-check-page");
    final var issues = LaboratoryStructureCheck.inspect(document);
    final long errors =
        issues.stream()
            .filter(i -> i.diagnostic().severity() == ChemistryDiagnosticSeverity.ERROR)
            .count();
    final Label summary =
        note(
            document.experiment().components().isEmpty()
                ? "Добавьте структуру на холст."
                : "Структур: "
                    + document.experiment().components().size()
                    + " · ошибок: "
                    + errors
                    + " · замечаний: "
                    + (issues.size() - errors));
    final ListView<LaboratoryStructureCheck.Issue> list = new ListView<>();
    list.setId("chemistry-structure-issues");
    list.setStyle("-fx-font-family: Segoe UI; -fx-font-size: 13px;");
    list.setMinHeight(100);
    list.setPlaceholder(note("Проверка ядра не обнаружила замечаний."));
    final CheckBox actionable = new CheckBox("Только ошибки и предупреждения");
    actionable.setSelected(true);
    actionable.setId("chemistry-structure-actionable");
    final Runnable filter =
        () -> {
          list.getItems()
              .setAll(
                  issues.stream()
                      .filter(
                          i ->
                              !actionable.isSelected()
                                  || i.diagnostic().severity() != ChemistryDiagnosticSeverity.INFO)
                      .toList());
          list.setPlaceholder(
              note(
                  issues.isEmpty()
                      ? "Проверка ядра не обнаружила замечаний."
                      : "Для этого фильтра замечаний нет. Снимите фильтр, чтобы увидеть сведения."));
        };
    actionable.setOnAction(event -> filter.run());
    list.setCellFactory(
        view ->
            new ListCell<>() {
              protected void updateItem(
                  final LaboratoryStructureCheck.Issue item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                if (empty || item == null) {
                  this.setGraphic(null);
                  return;
                }
                final Label title = note(item.level() + " · " + item.name());
                title.setStyle("-fx-font-weight: bold;");
                title.maxWidthProperty().bind(list.widthProperty().subtract(45));
                final Label text = note(item.title());
                text.maxWidthProperty().bind(list.widthProperty().subtract(45));
                this.setGraphic(new VBox(4, title, text));
              }
            });
    final TextArea detail = new TextArea();
    detail.setEditable(false);
    detail.setWrapText(true);
    detail.setPrefRowCount(3);
    detail.setId("chemistry-structure-diagnostic-detail");
    final Button reveal = new Button("Показать структуру");
    reveal.setId("chemistry-structure-reveal");
    reveal.setDisable(true);
    final Button hydrogen = new Button("Проверить водороды…");
    hydrogen.setId("chemistry-structure-hydrogens");
    hydrogen.setDisable(true);
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, before, issue) -> {
              reveal.setDisable(issue == null);
              hydrogen.setDisable(issue == null);
              detail.setText(issue == null ? "" : issue.diagnostic().message());
            });
    reveal.setOnAction(
        event -> {
          final var issue = list.getSelectionModel().getSelectedItem();
          if (issue != null) {
            this.finish();
            select.accept(issue.componentId());
          }
        });
    hydrogen.setOnAction(
        event -> {
          final var issue = list.getSelectionModel().getSelectedItem();
          if (issue != null) {
            this.finish();
            hydrogens.accept(issue.componentId());
          }
        });
    list.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !reveal.isDisabled()) {
            reveal.fire();
            event.consume();
          }
        });
    final VBox body =
        new VBox(
            9,
            summary,
            note(
                "Проверяется снимок текущего опыта. Черновик можно сохранить с замечаниями. Отсутствие 3D нормально для 2D-рисунка; водороды не добавляются автоматически."),
            actionable,
            list,
            detail);
    body.setPadding(new javafx.geometry.Insets(4, 16, 8, 16));
    VBox.setVgrow(list, Priority.ALWAYS);
    this.setCenter(body);
    final FlowPane actions = new FlowPane(8, 6, reveal, hydrogen);
    actions.setPadding(new javafx.geometry.Insets(8, 16, 12, 16));
    this.setBottom(actions);
    filter.run();
  }
}
