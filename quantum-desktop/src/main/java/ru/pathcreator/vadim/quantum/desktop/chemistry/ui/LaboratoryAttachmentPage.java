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
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;

/** Read-only attachment navigation with bounded, cancellable loading. */
final class LaboratoryAttachmentPage extends LaboratoryComputationPage {
  private record Loaded(LaboratoryArtifactViewer.Preview preview, Image image) {}

  private final List<LaboratoryAttachment> attachments;
  private final Path document;
  private final Consumer<LaboratoryArtifactViewer.Preview> archive;
  private final BorderPane content = new BorderPane();
  private final Label title = note(""), position = note(""), scaleLabel = note("");
  private final TextArea details = new TextArea();
  private final FlowPane imageTools = new FlowPane(8, 6);
  private final ImageView picture = new ImageView();
  private final ScrollPane imageScroll = new ScrollPane(this.picture);
  private final Button previous = new Button("← Предыдущее"),
      next = new Button("Следующее →"),
      save = new Button("Сохранить как…"),
      retry = new Button("Повторить загрузку");
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          task -> {
            final Thread t = new Thread(task, "laboratory-attachment-preview");
            t.setDaemon(true);
            return t;
          });
  private javafx.concurrent.Task<Loaded> task;
  private long generation;
  private int index;
  private double zoom = 1;
  private boolean fit = true;

  LaboratoryAttachmentPage(
      final Path document,
      final List<LaboratoryAttachment> attachments,
      final LaboratoryAttachment selected,
      final Consumer<LaboratoryAttachment> save,
      final Consumer<LaboratoryArtifactViewer.Preview> archive,
      final Runnable close) {
    super("Вложение", close);
    this.setId("chemistry-attachment-page");
    this.setMinSize(0, 0);
    this.document = document;
    this.attachments = List.copyOf(attachments);
    this.archive = archive;
    this.index = this.attachments.indexOf(selected);
    if (this.index < 0) {
      throw new IllegalArgumentException("Вложение больше не принадлежит этому опыту.");
    }
    this.previous.setId("chemistry-attachment-previous");
    this.next.setId("chemistry-attachment-next");
    this.save.setId("chemistry-attachment-extract");
    this.retry.setId("chemistry-attachment-retry");
    this.previous.setOnAction(e -> this.move(-1));
    this.next.setOnAction(e -> this.move(1));
    this.save.setOnAction(e -> save.accept(this.attachments.get(this.index)));
    this.retry.setOnAction(e -> this.load());
    this.title.setId("chemistry-attachment-title");
    this.title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    this.title.maxWidthProperty().bind(this.widthProperty().subtract(36));
    this.details.setEditable(false);
    this.details.setWrapText(true);
    this.details.setPrefRowCount(3);
    this.details.setId("chemistry-attachment-details");
    final TitledPane metadata = new TitledPane("Сведения о файле", this.details);
    metadata.setId("chemistry-attachment-metadata");
    metadata.setExpanded(false);
    metadata.setAnimated(false);
    this.imageScroll.setPannable(true);
    this.imageScroll.setMinSize(0, 0);
    this.imageScroll.setId("chemistry-attachment-image-scroll");
    this.picture.setId("chemistry-attachment-image");
    this.picture.setPreserveRatio(true);
    this.imageScroll.viewportBoundsProperty().addListener((o, a, b) -> this.resizeImage());
    final Button smaller = new Button("−"),
        larger = new Button("+"),
        fitting = new Button("Вписать"),
        actual = new Button("100%");
    smaller.setId("chemistry-attachment-zoom-out");
    larger.setId("chemistry-attachment-zoom-in");
    fitting.setId("chemistry-attachment-fit");
    actual.setId("chemistry-attachment-actual");
    smaller.setOnAction(e -> this.zoom(1 / 1.25));
    larger.setOnAction(e -> this.zoom(1.25));
    fitting.setOnAction(
        e -> {
          this.fit = true;
          this.resizeImage();
        });
    actual.setOnAction(
        e -> {
          this.fit = false;
          this.zoom = 1;
          this.resizeImage();
        });
    this.imageScroll.setOnScroll(
        e -> {
          if (e.isControlDown()) {
            this.zoom(e.getDeltaY() > 0 ? 1.25 : 1 / 1.25);
            e.consume();
          }
        });
    this.imageTools.getChildren().setAll(smaller, this.scaleLabel, larger, fitting, actual);
    this.imageTools.setId("chemistry-attachment-image-tools");
    final VBox body =
        new VBox(
            8,
            this.title,
            new FlowPane(8, 6, this.previous, this.position, this.next, this.save, this.retry),
            metadata,
            this.imageTools,
            this.content);
    body.setPadding(new javafx.geometry.Insets(0, 16, 8, 16));
    body.setMinSize(0, 0);
    this.content.setMinSize(0, 100);
    VBox.setVgrow(this.content, Priority.ALWAYS);
    this.setCenter(body);
    this.status.setPadding(new javafx.geometry.Insets(8, 16, 12, 16));
    this.setBottom(this.status);
    this.load();
  }

  private void move(final int direction) {
    final int next = this.index + direction;
    if (next < 0 || next >= this.attachments.size()) {
      return;
    }
    this.index = next;
    this.load();
  }

  private void load() {
    if (this.disposed) {
      return;
    }
    final long version = ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    final var item = this.attachments.get(this.index);
    this.title.setText(item.name());
    this.position.setText((this.index + 1) + " / " + this.attachments.size());
    this.previous.setDisable(this.index == 0);
    this.next.setDisable(this.index + 1 == this.attachments.size());
    this.save.setDisable(true);
    this.retry.setDisable(true);
    this.details.setText(
        "Название: "
            + item.name()
            + "\nРазмер: "
            + item.size()
            + " байт\nSHA-256: "
            + item.sha256());
    this.picture.setImage(null);
    this.imageTools.setVisible(false);
    this.imageTools.setManaged(false);
    this.content.setCenter(note("Проверка и загрузка файла…"));
    this.status.setText("Чтение в фоне. Можно перейти к другому файлу или вернуться к опыту.");
    this.busy(true);
    final var loading =
        new javafx.concurrent.Task<Loaded>() {
          protected Loaded call() throws Exception {
            final var preview = LaboratoryArtifactViewer.read(document, item);
            Image image = null;
            if (preview.image() != null) {
              int width = 2048, height = 2048;
              try (final var input =
                  javax.imageio.ImageIO.createImageInputStream(
                      new java.io.ByteArrayInputStream(preview.image()))) {
                final var readers = javax.imageio.ImageIO.getImageReaders(input);
                if (readers.hasNext()) {
                  final var reader = readers.next();
                  try {
                    reader.setInput(input);
                    width = Math.min(2048, reader.getWidth(0));
                    height = Math.min(2048, reader.getHeight(0));
                  } finally {
                    reader.dispose();
                  }
                }
              }
              image =
                  new Image(
                      new java.io.ByteArrayInputStream(preview.image()), width, height, true, true);
              if (image.isError()) {
                throw new java.io.IOException(
                    "Не удалось прочитать изображение.", image.getException());
              }
            }
            return new Loaded(preview, image);
          }
        };
    this.task = loading;
    loading.setOnSucceeded(
        e -> {
          if (this.disposed || version != this.generation) {
            return;
          }
          this.task = null;
          this.busy(false);
          this.save.setDisable(false);
          this.retry.setDisable(false);
          final var result = loading.getValue();
          final var preview = result.preview();
          if (preview.calculation() != null || preview.prediction() != null) {
            this.archive.accept(preview);
            return;
          }
          this.status.setText(preview.notice());
          if (result.image() != null) {
            this.picture.setImage(result.image());
            this.content.setCenter(this.imageScroll);
            this.imageTools.setVisible(true);
            this.imageTools.setManaged(true);
            this.fit = true;
            this.resizeImage();
          } else {
            final TextArea text = new TextArea(preview.text());
            text.setId("chemistry-attachment-text");
            text.setEditable(false);
            text.setWrapText(true);
            text.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
            this.content.setCenter(text);
          }
        });
    loading.setOnFailed(
        e -> {
          if (this.disposed || version != this.generation) {
            return;
          }
          this.task = null;
          this.busy(false);
          this.retry.setDisable(false);
          this.content.setCenter(note("Файл не открыт. Данные опыта не изменены."));
          this.status.setText(LaboratoryAttachmentImportPage.failure(loading.getException()));
        });
    this.worker.submit(loading);
  }

  private void zoom(final double factor) {
    if (this.picture.getImage() == null) {
      return;
    }
    this.fit = false;
    this.zoom = Math.max(0.1, Math.min(8, this.zoom * factor));
    this.resizeImage();
  }

  private void resizeImage() {
    final Image image = this.picture.getImage();
    if (image == null) {
      return;
    }
    if (this.fit) {
      final var bounds = this.imageScroll.getViewportBounds();
      this.zoom =
          Math.max(
              0.01,
              Math.min(
                  1,
                  Math.min(
                      (bounds.getWidth() - 6) / image.getWidth(),
                      (bounds.getHeight() - 6) / image.getHeight())));
    }
    this.picture.setFitWidth(image.getWidth() * this.zoom);
    this.picture.setFitHeight(image.getHeight() * this.zoom);
    this.scaleLabel.setText(Math.round(this.zoom * 100) + "%");
  }

  @Override
  boolean canClose() {
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
    this.busy(false);
  }
}
