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
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryStructureTable.Row;

final class LaboratoryAtomTablePage extends LaboratoryStructurePage {
  private final TextField name = new TextField();
  private final TextField charge = new TextField();
  private final TextField spin = new TextField();
  private final TableView<Row> atoms =
      LaboratoryStructureTable.table(
          "chemistry-atoms-table",
          "Атом",
          "Элемент",
          "Заряд",
          "Изотоп",
          "Радикал",
          "X",
          "Y",
          "Z",
          "Единица");
  private final TableView<Row> bonds =
      LaboratoryStructureTable.table(
          "chemistry-bonds-table", "Первый атом", "Второй атом", "Тип", "Отображение");
  private final LaboratoryStructurePreview preview = new LaboratoryStructurePreview();
  private final Label previewStatus = note("Рисунок исходной структуры");
  private final TabPane tabs = new TabPane();
  private MoleculeDrawing shown;

  @SuppressWarnings("unchecked")
  LaboratoryAtomTablePage(
      final LaboratoryEditor editor, final String componentId, final Runnable close) {
    super(editor, componentId, "atoms", "Атомы и связи", close);
    this.shown = this.source;
    this.name.setText(this.source.molecule().displayName());
    this.name.setId("chemistry-structure-name");
    this.name.setPrefColumnCount(24);
    this.charge.setText(Integer.toString(this.source.molecule().charge().value()));
    this.charge.setId("chemistry-structure-charge");
    this.charge.setPrefColumnCount(4);
    this.spin.setText(Integer.toString(this.source.molecule().spinMultiplicity().value()));
    this.spin.setId("chemistry-structure-spin");
    this.spin.setPrefColumnCount(4);
    this.atoms.getColumns().getFirst().setEditable(false);
    ((TableColumn<Row, String>) this.atoms.getColumns().getFirst())
        .setCellFactory(
            column ->
                new TableCell<>() {
                  protected void updateItem(final String id, final boolean empty) {
                    super.updateItem(id, empty);
                    this.setText(empty || id == null ? null : atomLabel(id));
                    this.setTooltip(empty || id == null ? null : new Tooltip(id));
                  }
                });
    LaboratoryStructureTable.choices(this.atoms, 8, "Å", "bohr");
    LaboratoryStructureTable.choices(
        this.bonds,
        2,
        Arrays.stream(BondType.values())
            .map(LaboratoryStructureTable::bondName)
            .toArray(String[]::new));
    LaboratoryStructureTable.choices(
        this.bonds,
        3,
        Arrays.stream(BondDrawingStyle.values())
            .map(LaboratoryStructureTable::styleName)
            .toArray(String[]::new));
    for (final var atom : this.source.molecule().atoms()) {
      final var point = atom.coordinate();
      this.addAtom(
          new Row(
              atom.id().value(),
              atom.element().symbol().value(),
              Integer.toString(atom.formalCharge().value()),
              atom.hasIsotope() ? Integer.toString(atom.isotope().massNumber()) : "",
              Integer.toString(atom.radicalState().unpairedElectrons()),
              point == null ? "" : Double.toString(point.x()),
              point == null ? "" : Double.toString(point.y()),
              point == null ? "" : Double.toString(point.z()),
              point == null || point.unit() == LengthUnit.ANGSTROM ? "Å" : "bohr"));
    }
    for (final var bond : this.source.molecule().bonds()) {
      final var drawing =
          this.source.bonds().stream()
              .filter(b -> b.connects(bond.firstAtomId(), bond.secondAtomId()))
              .findFirst()
              .orElseThrow();
      this.addBond(
          new Row(
              drawing.firstAtomId().value(),
              drawing.secondAtomId().value(),
              LaboratoryStructureTable.bondName(bond.type()),
              LaboratoryStructureTable.styleName(drawing.style())));
    }
    for (final var table : List.of(this.atoms, this.bonds)) {
      table.setMinSize(0, 150);
      table
          .getColumns()
          .forEach(
              c -> {
                c.setSortable(false);
                c.setPrefWidth(90);
                c.setMinWidth(60);
              });
      table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
    this.atoms.getColumns().get(1).setPrefWidth(75);
    for (int i = 5; i < 9; i++) {
      this.atoms.getColumns().get(i).setVisible(false);
    }
    final CheckBox coordinates = new CheckBox("3D-координаты");
    coordinates.setId("chemistry-structure-coordinates");
    coordinates.setOnAction(
        event -> {
          for (int i = 5; i < 9; i++) {
            this.atoms.getColumns().get(i).setVisible(coordinates.isSelected());
          }
        });
    final VBox atomPane =
        new VBox(
            8,
            new FlowPane(
                8,
                6,
                button(
                    "Добавить атом",
                    () -> {
                      this.flushInput();
                      final Row row =
                          new Row("a_" + UUID.randomUUID(), "C", "0", "", "0", "", "", "", "Å");
                      this.addAtom(row);
                      this.atoms.getSelectionModel().select(row);
                      this.atoms.scrollTo(row);
                      this.atomChoices();
                      this.changed();
                    }),
                button(
                    "Удалить атом",
                    () -> {
                      this.flushInput();
                      final Row row = this.atoms.getSelectionModel().getSelectedItem();
                      if (row == null) {
                        return;
                      }
                      this.bonds
                          .getItems()
                          .removeIf(
                              b -> b.text(0).equals(row.text(0)) || b.text(1).equals(row.text(0)));
                      this.atoms.getItems().remove(row);
                      this.atoms.refresh();
                      this.atomChoices();
                      this.changed();
                    }),
                coordinates),
            this.atoms);
    VBox.setVgrow(this.atoms, Priority.ALWAYS);
    final VBox bondPane =
        new VBox(
            8,
            new FlowPane(
                8,
                6,
                button(
                    "Добавить связь",
                    () -> {
                      this.flushInput();
                      final var atom = this.atoms.getSelectionModel().getSelectedItem();
                      final Row row =
                          new Row(
                              atom == null ? "" : atom.text(0),
                              "",
                              LaboratoryStructureTable.bondName(BondType.SINGLE),
                              LaboratoryStructureTable.styleName(BondDrawingStyle.PLAIN));
                      this.addBond(row);
                      this.bonds.getSelectionModel().select(row);
                      this.changed();
                    }),
                button(
                    "Удалить связь",
                    () -> {
                      this.flushInput();
                      this.bonds
                          .getItems()
                          .remove(this.bonds.getSelectionModel().getSelectedItem());
                      this.changed();
                    })),
            this.bonds);
    VBox.setVgrow(this.bonds, Priority.ALWAYS);
    this.tabs.setId("chemistry-structure-table-tabs");
    this.tabs.getTabs().setAll(new Tab("Атомы", atomPane), new Tab("Связи", bondPane));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setMinSize(0, 190);
    final VBox drawing =
        new VBox(
            6,
            new FlowPane(8, 6, button("Обновить рисунок", this::refreshPreview)),
            this.previewStatus,
            this.preview);
    drawing.setMinSize(0, 220);
    VBox.setVgrow(this.preview, Priority.ALWAYS);
    final SplitPane split = new SplitPane(this.tabs, drawing);
    split.setDividerPositions(0.56);
    split
        .widthProperty()
        .addListener(
            (o, a, b) ->
                split.setOrientation(
                    b.doubleValue() < 740
                        ? javafx.geometry.Orientation.VERTICAL
                        : javafx.geometry.Orientation.HORIZONTAL));
    final FlowPane fields =
        new FlowPane(
            10,
            6,
            new VBox(3, note("Название"), this.name),
            new VBox(3, note("Заряд молекулы"), this.charge),
            new VBox(3, note("Мультиплетность"), this.spin));
    final VBox body =
        new VBox(
            8,
            fields,
            note(
                "Двойной щелчок или F2 — ввод. Выберите атом в строке или на рисунке. Все три пустые координаты означают отсутствие 3D. Все изменения можно отменить одним действием."),
            split);
    body.setPadding(new javafx.geometry.Insets(4, 16, 4, 16));
    VBox.setVgrow(split, Priority.ALWAYS);
    this.content(body);
    this.atoms
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, row) ->
                this.preview.show(this.shown, row == null ? null : AtomId.of(row.text(0))));
    this.bonds
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, row) ->
                this.preview.show(
                    this.shown,
                    row == null || row.text(0).isBlank() ? null : AtomId.of(row.text(0))));
    this.preview.onPick(
        id -> {
          this.flushInput();
          this.tabs.getSelectionModel().selectFirst();
          this.atoms.getItems().stream()
              .filter(r -> r.text(0).equals(id.value()))
              .findFirst()
              .ifPresent(
                  r -> {
                    this.atoms.getSelectionModel().select(r);
                    this.atoms.scrollTo(r);
                  });
        });
    List.of(this.name, this.charge, this.spin)
        .forEach(field -> field.textProperty().addListener((o, a, b) -> this.changed()));
    this.atomChoices();
    this.seal();
    this.preview.show(this.shown, null);
  }

  private void changed() {
    this.previewStatus.setText("Есть изменения. «Обновить рисунок» покажет текущий ввод.");
  }

  private void addAtom(final Row row) {
    row.cells.forEach(cell -> cell.addListener((o, a, b) -> this.changed()));
    this.atoms.getItems().add(row);
  }

  private void addBond(final Row row) {
    row.cells.forEach(cell -> cell.addListener((o, a, b) -> this.changed()));
    this.bonds.getItems().add(row);
  }

  private String atomLabel(final String id) {
    for (int i = 0; i < this.atoms.getItems().size(); i++) {
      final Row row = this.atoms.getItems().get(i);
      if (row.text(0).equals(id)) {
        return row.text(1) + " · " + (i + 1);
      }
    }
    return id.isEmpty() ? "Выберите атом" : "Отсутствующий атом";
  }

  @SuppressWarnings("unchecked")
  private void atomChoices() {
    final var converter =
        new javafx.util.StringConverter<String>() {
          public String toString(final String id) {
            return id == null ? "Выберите атом" : atomLabel(id);
          }

          public String fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        };
    final var ids =
        javafx.collections.FXCollections.observableArrayList(
            this.atoms.getItems().stream().map(r -> r.text(0)).toList());
    for (int i = 0; i < 2; i++) {
      ((TableColumn<Row, String>) this.bonds.getColumns().get(i))
          .setCellFactory(ComboBoxTableCell.forTableColumn(converter, ids));
    }
    this.bonds.refresh();
  }

  @SuppressWarnings("unchecked")
  protected void flushInput() {
    for (final var table : List.of(this.atoms, this.bonds)) {
      for (final var node : table.lookupAll(".table-cell")) {
        if (node instanceof TableCell<?, ?> cell
            && cell.isEditing()
            && cell.getGraphic() instanceof TextField field) {
          ((TableCell<Row, String>) cell).commitEdit(field.getText());
        }
      }
    }
  }

  private void refreshPreview() {
    try {
      this.flushInput();
      this.shown = this.build();
      final var row = this.atoms.getSelectionModel().getSelectedItem();
      this.preview.show(this.shown, row == null ? null : AtomId.of(row.text(0)));
      this.previewStatus.setText("Рисунок учитывает текущий ввод. Применение — отдельной кнопкой.");
      this.error.setText("");
    } catch (final RuntimeException exception) {
      this.error.setText(message(exception));
      this.previewStatus.setText(
          "Показана последняя корректная структура; введённые поля сохранены.");
    }
  }

  protected MoleculeDrawing build() {
    return LaboratoryStructureTable.build(
        this.source,
        this.name.getText(),
        LaboratoryStructureTable.integer(this.charge.getText(), "Заряд молекулы"),
        LaboratoryStructureTable.integer(this.spin.getText(), "Мультиплетность"),
        this.atoms.getItems(),
        this.bonds.getItems());
  }

  protected Map<String, String> values() {
    final var values = new LinkedHashMap<String, String>();
    values.put("name", this.name.getText());
    values.put("charge", this.charge.getText());
    values.put("spin", this.spin.getText());
    this.writeRows(values, "atom", this.atoms);
    this.writeRows(values, "bond", this.bonds);
    return values;
  }

  private void writeRows(
      final Map<String, String> values, final String name, final TableView<Row> rows) {
    values.put(name + ".count", Integer.toString(rows.getItems().size()));
    for (int i = 0; i < rows.getItems().size(); i++) {
      final Row row = rows.getItems().get(i);
      for (int j = 0; j < row.cells.size(); j++) {
        values.put(name + "." + i + "." + j, row.text(j));
      }
    }
  }

  protected void restoreValues(final Map<String, String> values) {
    final String name = Objects.requireNonNull(values.get("name")),
        charge = Objects.requireNonNull(values.get("charge")),
        spin = Objects.requireNonNull(values.get("spin"));
    final var atoms = readRows(values, "atom", 9, 4096);
    final var bonds = readRows(values, "bond", 4, 16384);
    final var expected =
        new java.util.HashSet<String>(
            java.util.List.of("name", "charge", "spin", "atom.count", "bond.count"));
    for (int i = 0; i < atoms.size(); i++) {
      for (int j = 0; j < 9; j++) {
        expected.add("atom." + i + "." + j);
      }
    }
    for (int i = 0; i < bonds.size(); i++) {
      for (int j = 0; j < 4; j++) {
        expected.add("bond." + i + "." + j);
      }
    }
    if (!expected.equals(values.keySet())) {
      throw new IllegalArgumentException("Неполный или неизвестный набор полей черновика таблицы.");
    }
    this.name.setText(name);
    this.charge.setText(charge);
    this.spin.setText(spin);
    this.atoms.getItems().clear();
    this.bonds.getItems().clear();
    atoms.forEach(this::addAtom);
    bonds.forEach(this::addBond);
    this.atomChoices();
    this.changed();
  }

  private static List<Row> readRows(
      final Map<String, String> values, final String name, final int columns, final int maximum) {
    final int count = Integer.parseInt(values.get(name + ".count"));
    if (count < 0 || count > maximum) {
      throw new IllegalArgumentException("Недопустимое число строк черновика.");
    }
    final List<Row> rows = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final String[] cells = new String[columns];
      for (int j = 0; j < columns; j++) {
        cells[j] = Objects.requireNonNull(values.get(name + "." + i + "." + j));
      }
      if (name.equals("atom")) {
        AtomId.of(cells[0]);
      }
      rows.add(new Row(cells));
    }
    return rows;
  }
}
