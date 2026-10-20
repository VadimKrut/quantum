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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

/** Editable scientific rows retain stable identities across corrections and draft recovery. */
final class LaboratoryEvidenceTable extends TableView<LaboratoryEvidenceTable.Row> {

  static final class Row {
    final String id;
    final List<StringProperty> values = new ArrayList<>();

    Row(final String id, final List<String> values) {
      this.id = id;
      values.forEach(v -> this.values.add(new SimpleStringProperty(v)));
    }

    String get(final int index) {
      return this.values.get(index).get();
    }

    void set(final int index, final String value) {
      this.values.get(index).set(value);
    }

    List<String> text() {
      return this.values.stream().map(StringProperty::get).toList();
    }
  }

  private final List<String> header;
  private final Map<Integer, javafx.util.StringConverter<String>> choices = new HashMap<>();
  private final Map<Integer, String> defaults = new HashMap<>();
  private final Runnable changed;

  LaboratoryEvidenceTable(
      final List<String> header, final List<String> labels, final Runnable changed) {
    this.header = List.copyOf(header);
    this.changed = changed;
    this.setEditable(true);
    this.setMinHeight(125);
    this.setPrefHeight(150);
    this.getItems()
        .addListener(
            (javafx.collections.ListChangeListener<Row>)
                c -> {
                  this.resizeRows();
                });
    this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    for (int i = 0; i < header.size(); i++) {
      final int column = i;
      final TableColumn<Row, String> c = new TableColumn<>(labels.get(i));
      c.setCellValueFactory(v -> v.getValue().values.get(column));
      c.setCellFactory(TextFieldTableCell.forTableColumn());
      c.setOnEditCommit(e -> e.getRowValue().set(column, e.getNewValue()));
      c.setPrefWidth(125);
      this.getColumns().add(c);
    }
    this.setPlaceholder(new Label("Добавьте строку или загрузите свою таблицу"));
  }

  void displayColumn(final int index, final java.util.function.Function<String, String> display) {
    @SuppressWarnings("unchecked")
    final TableColumn<Row, String> column = (TableColumn<Row, String>) this.getColumns().get(index);
    column.setEditable(false);
    column.setCellFactory(
        c ->
            new TableCell<>() {
              protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                this.setText(empty || value == null ? null : display.apply(value));
              }
            });
  }

  /** Short display never replaces the original value: editing starts with the exact raw text. */
  void numericColumn(final int index, final int digits, final boolean list) {
    @SuppressWarnings("unchecked")
    final TableColumn<Row, String> column = (TableColumn<Row, String>) this.getColumns().get(index);
    column.setCellFactory(
        c ->
            new TableCell<>() {
              private final TextField editor = new TextField();
              private Row editingRow;
              private String originalRaw;
              private boolean filling;

              {
                this.editor
                    .textProperty()
                    .addListener(
                        (o, before, after) -> {
                          if (!this.filling && this.isEditing() && this.editingRow != null)
                            this.editingRow.set(index, after);
                        });
                this.editor.setOnAction(e -> this.finish());
                this.editor
                    .focusedProperty()
                    .addListener(
                        (o, before, after) -> {
                          if (!after && this.isEditing()) this.finish();
                        });
                this.editor.setOnKeyPressed(
                    e -> {
                      if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        if (this.editingRow != null) this.editingRow.set(index, this.originalRaw);
                        this.cancelEdit();
                        e.consume();
                      }
                    });
              }

              private void finish() {
                if (this.isEditing()) this.commitEdit(this.editor.getText());
              }

              private void render() {
                this.setGraphic(null);
                final String raw = this.getItem();
                final String display =
                    this.isEmpty() || raw == null ? null : compactNumbers(raw, digits, list);
                this.setText(display);
                this.setTooltip(
                    raw == null || Objects.equals(raw, display)
                        ? null
                        : new Tooltip("Сохранённое значение: " + raw));
              }

              @Override
              public void startEdit() {
                super.startEdit();
                if (!this.isEditing()) return;
                this.editingRow = this.getTableRow().getItem();
                this.originalRaw = this.editingRow.get(index);
                this.filling = true;
                try {
                  this.editor.setText(Objects.toString(this.originalRaw, ""));
                } finally {
                  this.filling = false;
                }
                this.setText(null);
                this.setGraphic(this.editor);
                this.editor.requestFocus();
                this.editor.selectAll();
              }

              @Override
              public void cancelEdit() {
                super.cancelEdit();
                this.editingRow = null;
                this.render();
              }

              @Override
              public void commitEdit(final String value) {
                super.commitEdit(value);
                this.editingRow = null;
                this.render();
              }

              @Override
              protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                if (!this.isEditing() || empty) this.render();
              }
            });
  }

  static String compactNumbers(final String raw, final int digits, final boolean list) {
    if (raw.isBlank()) return raw;
    try {
      final var cells = list ? raw.split(";", -1) : new String[] {raw};
      final List<String> values = new ArrayList<>();
      for (final String cell : cells) {
        final var number = new java.math.BigDecimal(cell.strip().replace(',', '.'));
        if (number.precision() > 64 || Math.abs((long) number.scale()) > 64) return raw;
        final var rounded =
            number.setScale(digits, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        values.add(
            rounded.signum() == 0 && number.signum() != 0
                    || number.abs().compareTo(new java.math.BigDecimal("1000000000")) >= 0
                ? number.round(new java.math.MathContext(6)).stripTrailingZeros().toString()
                : rounded.toPlainString());
      }
      return String.join("; ", values);
    } catch (final NumberFormatException invalid) {
      return raw;
    }
  }

  private void resizeRows() {
    this.setPrefHeight(
        Math.min(
            290,
            Math.max(
                150,
                34
                    + this.getItems().size()
                        * (this.getFixedCellSize() > 0 ? this.getFixedCellSize() : 30))));
  }

  /** Labels are localised; row values, drafts and copied tables retain stable machine keys. */
  void choiceColumn(
      final int index,
      final List<String> keys,
      final javafx.util.StringConverter<String> converter,
      final String initial) {
    this.choices.put(index, converter);
    this.defaults.put(index, initial);
    this.setFixedCellSize(48);
    this.resizeRows();
    @SuppressWarnings("unchecked")
    final TableColumn<Row, String> column = (TableColumn<Row, String>) this.getColumns().get(index);
    column.setEditable(false);
    column.setPrefWidth(185);
    column.setMinWidth(165);
    column.setCellFactory(
        c ->
            new TableCell<>() {
              private final ComboBox<String> choice = new ComboBox<>();
              private boolean updating;

              {
                this.choice.getItems().setAll(keys);
                this.choice.setConverter(converter);
                this.choice.setPromptText("Выберите уровень");
                this.choice.setAccessibleText(column.getText());
                this.choice.setMaxWidth(Double.MAX_VALUE);
                this.choice.setOnAction(
                    e -> {
                      if (!this.updating
                          && this.getTableRow() != null
                          && this.getTableRow().getItem() != null
                          && this.choice.getValue() != null) {
                        this.getTableRow().getItem().set(index, this.choice.getValue());
                      }
                    });
              }

              protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                this.updating = true;
                try {
                  this.setText(null);
                  this.choice.setValue(empty ? null : value);
                  this.setGraphic(empty ? null : this.choice);
                } finally {
                  this.updating = false;
                }
              }
            });
  }

  private List<String> normalized(final List<String> values) {
    if (values.size() != this.header.size())
      throw new IllegalArgumentException("Неверное число столбцов таблицы.");
    final var result = new ArrayList<>(values);
    this.choices.forEach(
        (index, converter) -> result.set(index, converter.fromString(values.get(index))));
    return result;
  }

  List<String> header() {
    return this.header;
  }

  Row add(final String id, final List<String> values) {
    if (values.size() != this.header.size() || this.getItems().size() >= 10000) {
      throw new IllegalArgumentException("Неверное число столбцов или превышены 10000 строк.");
    }
    final var row = new Row(id, this.normalized(values));
    row.values.forEach(p -> p.addListener(o -> this.changed.run()));
    this.getItems().add(row);
    this.changed.run();
    return row;
  }

  Row add() {
    final var values = new ArrayList<>(Collections.nCopies(this.header.size(), ""));
    this.defaults.forEach(values::set);
    return this.add("row_" + UUID.randomUUID(), values);
  }

  void removeSelected() {
    final var selected = this.getSelectionModel().getSelectedItem();
    if (selected != null) {
      this.getItems().remove(selected);
      this.changed.run();
    }
  }

  void load(final List<List<String>> values) {
    if (values.size() > 10000) {
      throw new IllegalArgumentException("Не более 10000 строк анализа.");
    }
    final List<List<String>> prepared = new ArrayList<>();
    for (int i = 0; i < values.size(); i++) {
      try {
        prepared.add(this.normalized(values.get(i)));
      } catch (final IllegalArgumentException invalid) {
        throw new IllegalArgumentException(
            "Строка " + (i + 2) + ": " + invalid.getMessage(), invalid);
      }
    }
    this.getItems().clear();
    for (final var value : prepared) {
      this.add("row_" + UUID.randomUUID(), value);
    }
    this.changed.run();
  }

  String text() {
    final StringBuilder out = new StringBuilder(String.join("\t", this.header)).append('\n');
    for (final var row : this.getItems()) {
      out.append(
              String.join(
                  "\t",
                  row.text().stream().map(v -> "\"" + v.replace("\"", "\"\"") + "\"").toList()))
          .append('\n');
    }
    return out.toString();
  }

  void draft(final Properties values, final String prefix, final boolean restore) {
    if (!restore) {
      values.setProperty(prefix + "count", Integer.toString(this.getItems().size()));
      for (int i = 0; i < this.getItems().size(); i++) {
        final var row = this.getItems().get(i);
        values.setProperty(prefix + i + ".id", row.id);
        for (int j = 0; j < this.header.size(); j++) {
          values.setProperty(prefix + i + "." + j, row.get(j));
        }
      }
    } else {
      final int count = Integer.parseInt(values.getProperty(prefix + "count", "0"));
      if (count < 0 || count > 10000) {
        throw new IllegalArgumentException("Неверный размер черновика таблицы.");
      }
      this.getItems().clear();
      for (int i = 0; i < count; i++) {
        final List<String> row = new ArrayList<>();
        for (int j = 0; j < this.header.size(); j++) {
          row.add(values.getProperty(prefix + i + "." + j, ""));
        }
        this.add(values.getProperty(prefix + i + ".id", "row_" + UUID.randomUUID()), row);
      }
    }
  }
}
