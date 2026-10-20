/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Editable tables stage changes locally, then validate and apply one complete core graph. */
final class LaboratoryStructureTable {
  static final class Row {
    final List<StringProperty> cells;

    Row(final String... values) {
      this.cells =
          Arrays.stream(values)
              .map(value -> (StringProperty) new SimpleStringProperty(value))
              .toList();
    }

    String text(final int index) {
      return this.cells.get(index).get();
    }
  }

  static MoleculeDrawing build(
      final MoleculeDrawing source,
      final String name,
      final int charge,
      final int spin,
      final List<Row> atomRows,
      final List<Row> bondRows) {
    final ArrayList<Atom> atoms = new ArrayList<>();
    final ArrayList<AtomDrawing> positions = new ArrayList<>();
    for (final Row row : atomRows) {
      final AtomId id = AtomId.of(row.text(0));
      final String label = row.text(1) + " · " + (atoms.size() + 1);
      final Atom previous =
          source.molecule().atoms().stream()
              .filter(atom -> atom.id().equals(id))
              .findFirst()
              .orElse(null);
      final boolean empty = row.text(5).isBlank() && row.text(6).isBlank() && row.text(7).isBlank();
      final Coordinate3D point;
      try {
        point =
            empty
                ? null
                : Coordinate3D.of(
                    number(row.text(5)),
                    number(row.text(6)),
                    number(row.text(7)),
                    row.text(8).equals("Å") ? LengthUnit.ANGSTROM : LengthUnit.BOHR);
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(
            "У атома «" + label + "» нужны все три координаты или три пустых поля.");
      }
      atoms.add(
          Atom.of(
              id,
              ElementSymbol.of(row.text(1)),
              point,
              FormalCharge.of(integer(row.text(2), "Заряд атома «" + label + "»")),
              row.text(3).isBlank()
                  ? null
                  : Isotope.of(integer(row.text(3), "Изотоп «" + label + "»")),
              RadicalState.of(integer(row.text(4), "Радикал «" + label + "»")),
              previous == null ? null : previous.metadata()));
      positions.add(
          source.atoms().stream()
              .filter(atom -> atom.atomId().equals(id))
              .findFirst()
              .orElseGet(
                  () ->
                      AtomDrawing.of(
                          id,
                          DrawingPoint2D.of(
                              120 + positions.size() % 8 * 70, 120 + positions.size() / 8 * 70),
                          null,
                          true)));
    }
    final ArrayList<Bond> bonds = new ArrayList<>();
    final ArrayList<BondDrawing> styles = new ArrayList<>();
    for (final Row row : bondRows) {
      if (row.text(0).isBlank() || row.text(1).isBlank()) {
        throw new IllegalArgumentException(
            "В строке связи " + (bonds.size() + 1) + " выберите оба атома.");
      }
      final AtomId first = AtomId.of(row.text(0));
      final AtomId second = AtomId.of(row.text(1));
      bonds.add(
          Bond.of(
              first,
              second,
              Arrays.stream(BondType.values())
                  .filter(value -> bondName(value).equals(row.text(2)))
                  .findFirst()
                  .orElseThrow()));
      styles.add(
          BondDrawing.of(
              first,
              second,
              Arrays.stream(BondDrawingStyle.values())
                  .filter(value -> styleName(value).equals(row.text(3)))
                  .findFirst()
                  .orElseThrow()));
    }
    return new MoleculeSketchService()
        .reviseGraph(source, name, atoms, bonds, positions, styles, charge, spin);
  }

  static TableView<Row> table(final String id, final String... names) {
    final TableView<Row> table = new TableView<>();
    table.setId(id);
    table.setEditable(true);
    table.setPrefHeight(430);
    table.getSelectionModel().setCellSelectionEnabled(true);
    for (int index = 0; index < names.length; index++) {
      final int cellIndex = index;
      final TableColumn<Row, String> column = new TableColumn<>(names[index]);
      column.setPrefWidth(index == 0 ? 180 : 100);
      column.setCellValueFactory(cell -> cell.getValue().cells.get(cellIndex));
      column.setCellFactory(
          ignored ->
              new TextFieldTableCell<>() {
                private boolean installed;

                {
                  this.setConverter(new javafx.util.converter.DefaultStringConverter());
                }

                public void startEdit() {
                  super.startEdit();
                  if (!this.installed && this.getGraphic() instanceof TextField field) {
                    this.installed = true;
                    field
                        .focusedProperty()
                        .addListener(
                            (observable, old, focused) -> {
                              if (!focused && this.isEditing()) {
                                this.commitEdit(field.getText());
                              }
                            });
                  }
                }
              });
      column.setOnEditCommit(
          event -> event.getRowValue().cells.get(cellIndex).set(event.getNewValue()));
      table.getColumns().add(column);
    }
    return table;
  }

  @SuppressWarnings("unchecked")
  static void choices(final TableView<Row> table, final int index, final String... values) {
    ((TableColumn<Row, String>) table.getColumns().get(index))
        .setCellFactory(ComboBoxTableCell.forTableColumn(values));
  }

  static int integer(final String text, final String field) {
    try {
      return Integer.parseInt(text.trim());
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(field + ": введите целое число.");
    }
  }

  private static double number(final String text) {
    return Double.parseDouble(text.trim().replace(',', '.'));
  }

  static String bondName(final BondType type) {
    return switch (type) {
      case SINGLE -> "Одинарная";
      case DOUBLE -> "Двойная";
      case TRIPLE -> "Тройная";
      case AROMATIC -> "Ароматическая";
      case COORDINATE -> "Координационная";
      case UNKNOWN -> "Не определена";
    };
  }

  static String styleName(final BondDrawingStyle style) {
    return switch (style) {
      case PLAIN -> "Обычная";
      case WEDGE_UP -> "Клин";
      case WEDGE_DOWN -> "Штриховой клин";
      case DASHED -> "Штриховая";
      case WAVY -> "Волнистая";
      case AROMATIC_RING -> "Ароматическое обозначение";
      case COORDINATE_ARROW -> "Стрелка";
      case UNKNOWN -> "Не определено";
    };
  }
}
