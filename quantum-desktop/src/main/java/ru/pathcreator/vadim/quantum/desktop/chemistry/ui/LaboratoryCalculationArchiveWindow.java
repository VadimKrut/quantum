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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryCalculationArchive;

final class LaboratoryCalculationArchiveWindow {
  static void show(
      final Window owner,
      final LaboratoryEditor editor,
      final LaboratoryCalculationArchive archive,
      final String source) {
    final Dialog<Void> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Сохранённый расчёт · " + archive.title());
    LaboratoryDialogStyle.apply(dialog, owner);
    dialog.getDialogPane().setContent(view(editor, archive, source, dialog::close));
    dialog.getDialogPane().setPrefSize(960, 750);
    dialog.setResizable(true);
    dialog
        .getDialogPane()
        .getButtonTypes()
        .add(new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE));
    dialog.showAndWait();
  }

  static javafx.scene.Node view(
      final LaboratoryEditor editor,
      final LaboratoryCalculationArchive archive,
      final String source,
      final Runnable close) {
    final var relation = archive.relation(editor.document());
    final Label state = new Label(relation.text());
    state.setWrapText(true);
    state.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    state.setStyle("-fx-font-weight: bold; -fx-text-fill: -app-accent;");
    final Label note =
        new Label(
            "Сохранённый снимок: просмотр не запускает расчёт и не изменяет опыт. Числа относятся к входу этого запуска.");
    note.setWrapText(true);
    final TabPane tabs =
        new TabPane(
            new Tab("Отчёт", text(LaboratoryCalculationPresentation.report(archive.report()))));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    if (archive.input() != null) {
      tabs.getTabs().add(geometry("Входная структура", archive.input()));
    }
    if (archive.output() != null) {
      tabs.getTabs().add(geometry("Геометрия результата", archive.output()));
    }
    tabs.getTabs().add(new Tab("QCHEM", text(source)));
    VBox.setVgrow(tabs, Priority.ALWAYS);
    final Button back = new Button("← К опыту");
    back.setId("chemistry-calculation-archive-back");
    back.setOnAction(event -> close.run());
    final Label title = new Label("Сохранённый расчёт · " + archive.title());
    title.setWrapText(true);
    title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    note.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    state.setId("chemistry-calculation-archive-state");
    tabs.setId("chemistry-calculation-archive-tabs");
    final VBox page =
        new VBox(10, new javafx.scene.layout.FlowPane(12, 8, back, title), state, note, tabs);
    page.setPadding(new javafx.geometry.Insets(16));
    page.setId("chemistry-calculation-archive-page");
    return page;
  }

  private static TextArea text(final String value) {
    final var area = new TextArea(value);
    area.setEditable(false);
    area.setWrapText(true);
    return area;
  }

  private static Tab geometry(
      final String name,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule molecule) {
    final var preview = new LaboratoryGeometryPane(atom -> {});
    preview.load(molecule, null);
    final var scroll = new ScrollPane(preview);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    return new Tab(name, scroll);
  }
}
