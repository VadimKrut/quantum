/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;

/** Named reaction-medium materials use the existing condition model, without fabricating a molecular graph. */
final class LaboratoryConditionMaterials extends VBox {
  private final ListView<ReactionConditionComponent> items = new ListView<>();
  private final TextField name = new TextField();
  private final TextArea note = new TextArea();
  private final ComboBox<ReactionComponentRole> role = new ComboBox<>();
  private final Label error = new Label();
  private ReactionConditionComponent selected;
  private boolean changing;

  LaboratoryConditionMaterials(final List<ReactionConditionComponent> original) {
    super(7);
    this.items.setId("chemistry-stage-materials");
    this.items.getItems().setAll(original);
    this.items.setFixedCellSize(34);
    this.items.setPlaceholder(new Label("Растворители и добавки пока не указаны"));
    this.items
        .prefHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.min(112.0, Math.max(50, this.items.getItems().size() * 34 + 4)),
                this.items.getItems()));
    this.items.setMinHeight(50);
    this.name.setId("chemistry-stage-material-name");
    this.note.setId("chemistry-stage-material-note");
    this.name.setPromptText("Название, например ПЭГ-400, этанол или вода");
    this.note.setPromptText("Источник, свойства или пояснение");
    this.note.setPrefRowCount(2);
    this.note.setWrapText(true);
    this.role.setId("chemistry-stage-material-role");
    this.role
        .getItems()
        .setAll(
            java.util.Arrays.stream(ReactionComponentRole.values())
                .filter(
                    value ->
                        value != ReactionComponentRole.REACTANT
                            && value != ReactionComponentRole.PRODUCT)
                .toList());
    this.role.setValue(ReactionComponentRole.SOLVENT);
    this.role.setConverter(
        new StringConverter<>() {
          public String toString(final ReactionComponentRole value) {
            return value == null ? "" : LaboratoryInspector.roleText(value);
          }

          public ReactionComponentRole fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.items.setCellFactory(
        list ->
            new ListCell<>() {
              protected void updateItem(
                  final ReactionConditionComponent value, final boolean empty) {
                super.updateItem(value, empty);
                final String text =
                    empty || value == null
                        ? null
                        : LaboratoryInspector.roleText(value.role())
                            + " · "
                            + value.name()
                            + (value.note() == null ? "" : " · " + value.note());
                this.setText(text);
                this.setTooltip(text == null ? null : new Tooltip(text));
                this.setTextOverrun(OverrunStyle.ELLIPSIS);
                this.setMinWidth(0);
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(Region.USE_PREF_SIZE);
              }
            });
    this.items
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, value) -> {
              if (this.changing) {
                return;
              }
              if (this.pending()) {
                this.changing = true;
                this.items.getSelectionModel().select(a);
                this.changing = false;
                this.error.setText("Сначала примените поля материала или нажмите «Очистить поля».");
                return;
              }
              this.load(value);
            });
    final Button clear = new Button("Очистить поля");
    clear.setTooltip(
        new Tooltip(
            "Отменить изменения полей и подготовить новый материал. Список материалов сохраняется."));
    clear.setOnAction(
        event -> {
          this.changing = true;
          this.items.getSelectionModel().clearSelection();
          this.changing = false;
          this.load(null);
          this.error.setText("");
        });
    final Button add = new Button("Добавить");
    final Button update = new Button("Сохранить изменения");
    final Button remove = new Button("Удалить");
    add.setId("chemistry-stage-material-add");
    update.setId("chemistry-stage-material-update");
    remove.setId("chemistry-stage-material-remove");
    update.disableProperty().bind(this.items.getSelectionModel().selectedItemProperty().isNull());
    remove.disableProperty().bind(this.items.getSelectionModel().selectedItemProperty().isNull());
    add.setOnAction(event -> this.apply(false));
    update.setOnAction(event -> this.apply(true));
    remove.setOnAction(
        event -> {
          this.changing = true;
          this.items.getItems().remove(this.selected);
          this.items.getSelectionModel().clearSelection();
          this.changing = false;
          this.load(null);
          this.error.setText("");
        });
    final Label hint =
        new Label(
            "Укажите название и роль. Введённый материал добавится при применении стадии. Объём и расход указываются в материалах опыта.");
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    this.error.setWrapText(true);
    final HBox fields = new HBox(8, this.name, this.role);
    HBox.setHgrow(this.name, Priority.ALWAYS);
    final TitledPane details = new TitledPane("Комментарий к материалу", this.note);
    details.setAnimated(false);
    details.setExpanded(false);
    details.getStyleClass().add("laboratory-desk-details");
    this.error.visibleProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.managedProperty().bind(this.error.visibleProperty());
    this.getChildren()
        .setAll(
            hint,
            fields,
            details,
            new FlowPane(8, 6, add, update, remove, clear),
            this.items,
            this.error);
  }

  private void load(final ReactionConditionComponent value) {
    this.selected = value;
    this.name.setText(value == null ? "" : value.name());
    this.note.setText(value == null || value.note() == null ? "" : value.note());
    this.role.setValue(value == null ? ReactionComponentRole.SOLVENT : value.role());
  }

  private boolean pending() {
    return this.selected == null
        ? !this.name.getText().isBlank() || !this.note.getText().isBlank()
        : !this.name.getText().trim().equals(this.selected.name())
            || !this.note
                .getText()
                .trim()
                .equals(this.selected.note() == null ? "" : this.selected.note())
            || this.role.getValue() != this.selected.role();
  }

  List<ReactionConditionComponent> appliedValues() {
    if (this.pending()) {
      this.apply(this.selected != null);
      if (this.pending()) {
        throw new IllegalArgumentException(
            this.error.getText().isBlank()
                ? "Проверьте название и роль растворителя или добавки."
                : this.error.getText());
      }
    }
    return this.values();
  }

  List<ReactionConditionComponent> values() {
    if (this.pending()) {
      throw new IllegalArgumentException(
          "Материал среды изменён: нажмите «Добавить материал» или «Изменить материал» перед сохранением стадии.");
    }
    return List.copyOf(this.items.getItems());
  }

  void draft(final java.util.Properties fields, final boolean restore) {
    final var codec =
        new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec();
    if (restore) {
      final String restoredRole = fields.getProperty("stageForm.materialFields.role");
      if (!fields.containsKey("stageForm.materialFields.name")
          || !fields.containsKey("stageForm.materialFields.note")
          || this.role.getItems().stream().noneMatch(value -> value.name().equals(restoredRole))) {
        throw new IllegalArgumentException("Повреждены поля материала в черновике стадии.");
      }
      final var saved = codec.read(fields.getProperty("stageForm.materials"));
      final int index = Integer.parseInt(fields.getProperty("stageForm.materialSelection", "-1"));
      final var values = saved.experiment().conditions().components();
      if (index < -1 || index >= values.size()) {
        throw new IllegalArgumentException("Материал черновика отсутствует в списке.");
      }
      this.changing = true;
      try {
        this.items.getItems().setAll(values);
        this.items.getSelectionModel().select(index);
        this.load(index < 0 ? null : values.get(index));
        LaboratoryFormDraft.fields(
            fields,
            "stageForm.materialFields",
            true,
            java.util.Map.of("name", this.name, "note", this.note, "role", this.role));
      } finally {
        this.changing = false;
      }
    } else {
      final var conditions =
          ReactionConditions.of(
              null, null, null, List.copyOf(this.items.getItems()), List.of(), null);
      final var experiment =
          new ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment(
              "stage_materials", "Материалы стадии", conditions, "", List.of());
      fields.setProperty(
          "stageForm.materials",
          codec.write(
              new ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing(
                  experiment, java.util.Map.of())));
      fields.setProperty(
          "stageForm.materialSelection",
          Integer.toString(this.items.getSelectionModel().getSelectedIndex()));
      LaboratoryFormDraft.fields(
          fields,
          "stageForm.materialFields",
          false,
          java.util.Map.of("name", this.name, "note", this.note, "role", this.role));
    }
  }

  private void apply(final boolean replace) {
    try {
      final var old = replace ? this.selected : null;
      if (this.role.getValue() == ReactionComponentRole.REACTANT
          || this.role.getValue() == ReactionComponentRole.PRODUCT) {
        throw new IllegalArgumentException(
            "Исходники и продукты задаются участниками стадии; здесь укажите материал среды.");
      }
      final var next =
          ReactionConditionComponent.of(
              this.role.getValue(),
              this.name.getText(),
              old == null ? null : old.amount(),
              old == null ? null : old.concentration(),
              old == null ? null : old.equivalent(),
              old == null ? null : old.loading(),
              old == null ? null : old.purity(),
              old == null ? ReactionPhase.UNKNOWN : old.phase(),
              this.note.getText().isBlank() ? null : this.note.getText());
      final int index = replace ? this.items.getSelectionModel().getSelectedIndex() : -1;
      this.changing = true;
      if (index < 0) {
        this.items.getItems().add(next);
      } else {
        this.items.getItems().set(index, next);
      }
      this.items.getSelectionModel().select(index < 0 ? this.items.getItems().size() - 1 : index);
      this.load(next);
      this.error.setText("");
    } catch (final RuntimeException exception) {
      this.error.setText(exception.getMessage());
    } finally {
      this.changing = false;
    }
  }
}
