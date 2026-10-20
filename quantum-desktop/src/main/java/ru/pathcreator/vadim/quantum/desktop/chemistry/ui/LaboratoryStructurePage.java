/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Map;
import java.util.Properties;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** Local raw input, one graph commit, and a recoverable draft bound to its source. */
abstract class LaboratoryStructurePage extends BorderPane {
  protected final LaboratoryEditor editor;
  protected final String componentId;
  protected final MoleculeDrawing source;
  protected final Label error = note("");
  protected final Button apply = new Button("Применить структуру");
  protected boolean disposed;
  protected boolean restoring;
  private final String experimentId;
  private final String sourceFingerprint;
  private final String kind;
  private final Runnable close;
  private final FlowPane confirmation = new FlowPane(8, 6);
  private Map<String, String> initial = Map.of();
  private boolean applied;
  private boolean conflict;

  LaboratoryStructurePage(
      final LaboratoryEditor editor,
      final String componentId,
      final String kind,
      final String title,
      final Runnable close) {
    this.editor = editor;
    this.componentId = componentId;
    this.kind = kind;
    this.close = close;
    this.source = java.util.Objects.requireNonNull(editor.document().layouts().get(componentId));
    this.experimentId = editor.document().experiment().id();
    this.sourceFingerprint = this.fingerprint();
    this.setMinSize(0, 0);
    this.setId("chemistry-structure-form");
    this.getStyleClass().add("laboratory-structure-page");
    final Button back =
        button(
            "← К структурам",
            () -> {
              if (this.canClose()) {
                this.finish();
              }
            });
    back.setId("chemistry-structure-back");
    back.getStyleClass().add("laboratory-navigation-back");
    final Label heading =
        note("Проекты › " + editor.document().experiment().title() + " › " + title);
    heading.getStyleClass().add("laboratory-navigation-title");
    heading.maxWidthProperty().bind(this.widthProperty().subtract(40));
    final FlowPane top = new FlowPane(12, 8, back, heading);
    top.getStyleClass().add("laboratory-navigation-header");
    this.setTop(top);
    this.error.setId("chemistry-structure-error");
    this.error.managedProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.visibleProperty().bind(this.error.managedProperty());
    this.error.getStyleClass().add("laboratory-inline-message");
    this.apply.setId("chemistry-structure-apply");
    this.apply.setOnAction(event -> this.commit());
    final Button discard =
        button(
            "Отменить ввод",
            () -> {
              this.confirmation.setVisible(true);
              this.confirmation.setManaged(true);
            });
    discard.setId("chemistry-structure-cancel");
    this.confirmation
        .getChildren()
        .setAll(
            note("Отменить введённые изменения?"),
            button("Да, отменить ввод", this::finish),
            button(
                "Продолжить редактирование",
                () -> {
                  this.confirmation.setVisible(false);
                  this.confirmation.setManaged(false);
                }));
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
    this.confirmation.setId("chemistry-structure-confirm-cancel");
    final VBox bottom =
        new VBox(7, this.error, new FlowPane(8, 6, this.apply, discard), this.confirmation);
    bottom.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(bottom);
  }

  protected final void content(final javafx.scene.Node node) {
    final ScrollPane scroll = new ScrollPane(node);
    scroll.setId("chemistry-structure-scroll");
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setMinSize(0, 0);
    this.setCenter(scroll);
  }

  protected abstract Map<String, String> values();

  protected abstract void restoreValues(Map<String, String> values);

  protected abstract MoleculeDrawing build();

  protected void flushInput() {}

  protected final void seal() {
    this.initial = Map.copyOf(this.values());
  }

  protected final boolean current() {
    return !this.conflict
        && this.experimentId.equals(this.editor.document().experiment().id())
        && this.source.equals(this.editor.document().layouts().get(this.componentId));
  }

  private void commit() {
    if (this.applied || this.disposed) {
      return;
    }
    try {
      this.flushInput();
      if (!this.current()) {
        throw new IllegalArgumentException(
            "Исходная структура изменилась. Ввод сохранён; вернитесь к опыту и откройте форму заново.");
      }
      final var result = this.build();
      final var component =
          this.editor.document().experiment().components().stream()
              .filter(c -> c.id().equals(this.componentId))
              .findFirst()
              .orElseThrow();
      if (!this.source.equals(result)) {
        this.editor.replace(this.componentId, result, component.role());
      }
      this.applied = true;
      this.finish();
    } catch (final RuntimeException exception) {
      this.error.setText(message(exception));
    }
  }

  boolean pending() {
    this.flushInput();
    return !this.applied && (this.conflict || !this.values().equals(this.initial));
  }

  boolean canClose() {
    if (!this.pending()) {
      return true;
    }
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
    this.error.setText("Примените структуру или явно отмените ввод.");
    return false;
  }

  protected final void finish() {
    this.dispose();
    this.close.run();
  }

  void dispose() {
    this.disposed = true;
  }

  private String fingerprint() {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(
                      new LaboratoryFileCodec()
                          .write(this.editor.document())
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  void draft(final Properties fields, final boolean restore) {
    this.flushInput();
    if (!restore) {
      fields.setProperty("structureForm.kind", this.kind);
      fields.setProperty("structureForm.component", this.componentId);
      fields.setProperty("structureForm.experiment", this.experimentId);
      fields.setProperty("structureForm.source", this.sourceFingerprint);
      fields.setProperty("structureForm.conflict", Boolean.toString(!this.current()));
      this.values()
          .forEach((key, value) -> fields.setProperty("structureForm.input." + key, value));
      return;
    }
    if (!this.kind.equals(fields.getProperty("structureForm.kind"))
        || !this.componentId.equals(fields.getProperty("structureForm.component"))
        || !this.experimentId.equals(fields.getProperty("structureForm.experiment"))) {
      throw new IllegalArgumentException(
          "Черновик относится к другому опыту, компоненту или форме.");
    }
    final String conflict = fields.getProperty("structureForm.conflict");
    if (!"true".equals(conflict) && !"false".equals(conflict)) {
      throw new IllegalArgumentException("Повреждён признак состояния черновика.");
    }
    final var values = new java.util.LinkedHashMap<String, String>();
    fields.stringPropertyNames().stream()
        .filter(key -> key.startsWith("structureForm.input."))
        .sorted()
        .forEach(
            key ->
                values.put(
                    key.substring("structureForm.input.".length()), fields.getProperty(key)));
    this.restoring = true;
    try {
      this.restoreValues(values);
    } finally {
      this.restoring = false;
    }
    this.conflict =
        Boolean.parseBoolean(conflict)
            || !this.sourceFingerprint.equals(fields.getProperty("structureForm.source"));
    this.error.setText(
        this.conflict
            ? "Ввод восстановлен для просмотра: исходная структура изменилась."
            : "Восстановлен незавершённый ввод. Проверьте и примените структуру.");
  }

  protected static String message(final RuntimeException exception) {
    return exception.getMessage() == null
        ? "Проверьте введённые значения."
        : exception.getMessage();
  }

  protected static Button button(final String text, final Runnable action) {
    final Button b = new Button(text);
    b.setOnAction(event -> action.run());
    return b;
  }

  protected static Label note(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }
}
