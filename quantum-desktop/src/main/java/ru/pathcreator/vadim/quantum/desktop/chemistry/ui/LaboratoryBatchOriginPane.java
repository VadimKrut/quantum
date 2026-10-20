/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryInventory;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleOrigin;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileRepository;

/** Original observations remain linked read-only instead of becoming cloned measurements. */
final class LaboratoryBatchOriginPane extends VBox {
  LaboratoryBatchOriginPane(final LaboratoryInventory inventory, final String batchId) {
    super(6);
    final var origins = LaboratorySolutionUse.origins(inventory, batchId);
    if (origins.isEmpty()) {
      this.getChildren()
          .add(LaboratoryComputationPage.note("Происхождение от физического образца не задано."));
      return;
    }
    final var source = new ComboBox<LaboratorySampleOrigin>();
    source.getItems().setAll(origins);
    source.setMaxWidth(Double.MAX_VALUE);
    source.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(LaboratorySampleOrigin o) {
            return o == null
                ? ""
                : o.sourceTitle() + " · " + o.sourceCode() + " · ссылок " + o.analysisIds().size();
          }

          public LaboratorySampleOrigin fromString(String s) {
            return null;
          }
        });
    source.getSelectionModel().selectFirst();
    final var status = LaboratoryComputationPage.note("");
    final var show = new Button("Показать исходные анализы");
    show.setOnAction(
        event -> {
          final var origin = source.getValue();
          Path path = null;
          try {
            if (!origin.sourcePathHint().isBlank()
                && Files.isRegularFile(Path.of(origin.sourcePathHint())))
              path = Path.of(origin.sourcePathHint());
          } catch (RuntimeException ignored) {
          }
          if (path == null) {
            final var chooser = new FileChooser();
            chooser.setTitle("Исходный опыт «" + origin.sourceTitle() + "»");
            chooser
                .getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Опыт Quantum", "*.qchem"));
            final var file = chooser.showOpenDialog(this.getScene().getWindow());
            if (file == null) return;
            path = file.toPath();
          }
          final Path selected = path;
          show.setDisable(true);
          status.setText("Читаю исходные записи…");
          Thread.startVirtualThread(
              () -> {
                try {
                  if (Files.size(selected) > 134217728)
                    throw new IllegalArgumentException("Исходный опыт превышает128 МиБ.");
                  final var e =
                      new LaboratoryFileRepository().read(selected).drawing().experiment();
                  final String text = LaboratorySolutionUse.sourceAnalyses(e, origin);
                  Platform.runLater(
                      () -> {
                        show.setDisable(false);
                        if (this.getScene() == null) return;
                        final var dialog = new Dialog<Void>();
                        dialog.setTitle("Исходные анализы · только чтение");
                        if (this.getScene().getWindow() != null)
                          dialog.initOwner(this.getScene().getWindow());
                        final var content = new TextArea(text);
                        content.setEditable(false);
                        content.setWrapText(true);
                        content.setPrefSize(780, 520);
                        dialog.getDialogPane().setContent(content);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        LaboratoryDialogStyle.apply(dialog, dialog.getOwner());
                        dialog.show();
                        status.setText("Оригиналы прочитаны; новых измерений не создавалось.");
                      });
                } catch (Exception failure) {
                  Platform.runLater(
                      () -> {
                        show.setDisable(false);
                        status.setText(failure.getMessage());
                      });
                }
              });
        });
    this.getChildren()
        .setAll(
            LaboratoryComputationPage.note("Физическое происхождение партии"),
            source,
            show,
            status);
  }
}
