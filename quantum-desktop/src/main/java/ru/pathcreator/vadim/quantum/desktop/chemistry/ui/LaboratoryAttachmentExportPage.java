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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryAssetStore;

/** Copy to a temporary file and verify before atomically replacing the selected destination. */
final class LaboratoryAttachmentExportPage extends LaboratoryComputationPage {
  private final javafx.concurrent.Task<Void> task;

  LaboratoryAttachmentExportPage(
      final Path document,
      final LaboratoryAttachment attachment,
      final Path destination,
      final Runnable close) {
    super("Сохранить вложение отдельно", close);
    this.setId("chemistry-attachment-export-page");
    this.setMinSize(0, 0);
    final TextArea location = new TextArea(destination.toString());
    location.setEditable(false);
    location.setWrapText(true);
    location.setPrefRowCount(3);
    final VBox body =
        new VBox(12, note(attachment.name()), note("Куда сохранить"), location, this.status);
    body.setPadding(new javafx.geometry.Insets(8, 16, 16, 16));
    this.setCenter(scroll(body));
    this.busy(true);
    this.status.setText("Копирование и проверка файла в фоне…");
    this.task =
        new javafx.concurrent.Task<>() {
          protected Void call() throws Exception {
            LaboratoryAssetStore.extract(document, attachment, destination);
            return null;
          }
        };
    this.task.setOnSucceeded(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.status.setText("Вложение сохранено и проверено. Опыт не изменён.");
          }
        });
    this.task.setOnFailed(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.status.setText(
                "Файл не сохранён: "
                    + LaboratoryAttachmentImportPage.failure(this.task.getException()));
          }
        });
    final Thread worker = new Thread(this.task, "laboratory-attachment-export");
    worker.setDaemon(true);
    worker.start();
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.task.cancel(true);
    this.busy(false);
  }
}
