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
import java.util.function.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Recoverable library metadata and explicit attachment points over the current core snapshot. */
final class LaboratoryLibraryEntryPage extends BorderPane {
  private final String kind;
  private final MoleculeDrawing source;
  private final LaboratoryDrawing current;
  private final String fingerprint;
  private final TextField name = new TextField(), label = new TextField("Точка 1");
  private final TextArea summary = text(3), tags = text(2), references = text(2);
  private final ComboBox<AtomId> atom = new ComboBox<>();
  private final ComboBox<BondType> bond = new ComboBox<>();
  private final ListView<ChemistryLibraryAttachmentPoint> points = new ListView<>();
  private final LaboratoryStructurePreview preview = new LaboratoryStructurePreview();
  private final Label error = note("");
  private final FlowPane confirmation = new FlowPane(8, 6);
  private final Button save = new Button("Сохранить в библиотеку");
  private final VBox content = new VBox(8);
  private final Runnable close;
  private final Map<String, String> initial;
  private boolean busy, conflict;

  LaboratoryLibraryEntryPage(
      final String kind,
      final MoleculeDrawing source,
      final LaboratoryDrawing current,
      final Consumer<Supplier<ChemistryLibraryEntry>> save,
      final Runnable close) {
    if (!Set.of("structure", "fragment", "template").contains(kind)) {
      throw new IllegalArgumentException("Неизвестный тип библиотечной формы.");
    }
    this.kind = kind;
    this.source = source;
    this.current = current;
    this.close = close;
    this.fingerprint = fingerprint(current);
    this.setId("chemistry-library-entry-page");
    this.setMinSize(0, 0);
    this.name.setText(
        kind.equals("template")
            ? current.experiment().title()
            : Objects.requireNonNull(source).molecule().displayName());
    this.name.setId("chemistry-library-name");
    this.summary.setId("chemistry-library-summary");
    this.tags.setId("chemistry-library-tags");
    this.references.setId("chemistry-library-references");
    this.summary.setPromptText("Назначение структуры или инструкция к плану опыта");
    this.tags.setPromptText("Одна метка или синоним на строку");
    this.references.setPromptText("Один источник на строку");
    final Button back =
        button(
            "← К библиотеке",
            () -> {
              if (this.canClose()) {
                this.close.run();
              }
            });
    back.setId("chemistry-library-entry-back");
    final Label title =
        note(
            kind.equals("template")
                ? "Сохранить шаблон опыта"
                : kind.equals("fragment") ? "Сохранить фрагмент" : "Сохранить структуру");
    title.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
    final FlowPane header = new FlowPane(12, 8, back, title);
    header.setPadding(new javafx.geometry.Insets(12, 16, 8, 16));
    this.setTop(header);
    final VBox fields =
        new VBox(
            6,
            note("Название"),
            this.name,
            note("Описание / инструкция"),
            this.summary,
            note("Метки и синонимы"),
            this.tags,
            note("Источники"),
            this.references);
    fields.setMinWidth(0);
    if (kind.equals("template")) {
      this.content
          .getChildren()
          .setAll(
              note(
                  "Сохранится план со стадиями и количествами. Фактические результаты, журнал и вложения не копируются."),
              fields);
    } else {
      this.preview.hint("Нажмите на атом, чтобы выделить его.");
      this.preview.onPick(id -> this.preview.show(source, id));
      this.preview.show(source, null);
      final TabPane tabs = new TabPane(new Tab("Сведения", fields));
      tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
      tabs.setId("chemistry-library-entry-tabs");
      tabs.setMinSize(0, 250);
      if (kind.equals("fragment")) {
        tabs.getTabs().add(new Tab("Точки присоединения", this.pointEditor()));
      }
      final SplitPane split = new SplitPane(tabs, this.preview);
      split.setDividerPositions(0.53);
      split
          .widthProperty()
          .addListener(
              (o, a, b) ->
                  split.setOrientation(
                      b.doubleValue() < 650
                          ? javafx.geometry.Orientation.VERTICAL
                          : javafx.geometry.Orientation.HORIZONTAL));
      VBox.setVgrow(split, Priority.ALWAYS);
      this.content
          .getChildren()
          .setAll(
              note(
                  kind.equals("fragment")
                      ? "Сохраняется весь выбранный компонент. Выберите атом на рисунке и явно добавьте точку. Водороды автоматически не удаляются."
                      : "Сохранится выбранная структура без количеств и фактических записей опыта."),
              split);
    }
    this.content.setPadding(new javafx.geometry.Insets(0, 16, 8, 16));
    final ScrollPane scroll = new ScrollPane(this.content);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setMinSize(0, 0);
    this.setCenter(scroll);
    this.save.setId("chemistry-library-entry-save");
    this.save.setOnAction(
        e -> {
          if (this.busy) {
            return;
          }
          try {
            if (this.conflict) {
              throw new IllegalArgumentException(
                  "Исходный опыт изменился. Ввод доступен для просмотра; откройте новую форму из текущего опыта.");
            }
            final var values = Map.copyOf(this.values());
            if (values.get("name").isBlank()) {
              throw new IllegalArgumentException("Укажите название библиотечной записи.");
            }
            this.validatePoints(values);
            save.accept(() -> this.build(values));
          } catch (final RuntimeException failure) {
            this.error.setText(failure.getMessage());
          }
        });
    final Button discard =
        button(
            "Отменить ввод",
            () -> {
              this.confirmation.setManaged(true);
              this.confirmation.setVisible(true);
            });
    discard.disableProperty().bind(this.save.disabledProperty());
    this.confirmation
        .getChildren()
        .setAll(
            note("Отменить введённые изменения?"),
            button(
                "Да, отменить",
                () -> {
                  if (!this.busy) {
                    this.close.run();
                  }
                }),
            button(
                "Продолжить ввод",
                () -> {
                  this.confirmation.setManaged(false);
                  this.confirmation.setVisible(false);
                }));
    this.confirmation.setManaged(false);
    this.confirmation.setVisible(false);
    this.error.setId("chemistry-library-entry-error");
    this.error.setMinHeight(Region.USE_PREF_SIZE);
    this.error.managedProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.visibleProperty().bind(this.error.managedProperty());
    final VBox footer =
        new VBox(7, this.error, new FlowPane(8, 6, this.save, discard), this.confirmation);
    footer.setPadding(new javafx.geometry.Insets(10, 16, 12, 16));
    this.setBottom(footer);
    this.initial = Map.copyOf(this.values());
  }

  private javafx.scene.Node pointEditor() {
    this.atom.setId("chemistry-fragment-atom");
    this.atom.getItems().setAll(this.source.molecule().atoms().stream().map(a -> a.id()).toList());
    this.atom.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final AtomId id) {
            return atomName(id);
          }

          public AtomId fromString(final String s) {
            throw new UnsupportedOperationException();
          }
        });
    this.atom.getSelectionModel().selectFirst();
    this.bond.setId("chemistry-fragment-bond");
    this.bond.getItems().setAll(BondType.SINGLE, BondType.DOUBLE, BondType.TRIPLE);
    this.bond.setValue(BondType.SINGLE);
    this.bond.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final BondType b) {
            return b == null ? "" : LaboratoryStructureTable.bondName(b);
          }

          public BondType fromString(final String s) {
            throw new UnsupportedOperationException();
          }
        });
    this.label.setId("chemistry-fragment-label");
    this.points.setId("chemistry-fragment-points");
    this.points.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    this.points.setMinSize(0, 100);
    this.points.setPrefHeight(170);
    this.points.setCellFactory(
        v ->
            new ListCell<>() {
              protected void updateItem(
                  final ChemistryLibraryAttachmentPoint p, final boolean empty) {
                super.updateItem(p, empty);
                this.setText(
                    empty || p == null
                        ? null
                        : p.label()
                            + " · "
                            + atomName(p.atomId())
                            + " · "
                            + LaboratoryStructureTable.bondName(p.bondType()));
              }
            });
    this.points
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (b != null) {
                this.atom.setValue(b.atomId());
                this.label.setText(b.label());
                this.bond.setValue(b.bondType());
                this.preview.show(this.source, b.atomId());
              }
            });
    this.atom.valueProperty().addListener((o, a, b) -> this.preview.show(this.source, b));
    this.preview.onPick(this.atom::setValue);
    final Button add = button("Добавить точку", () -> this.point(false)),
        update = button("Изменить точку", () -> this.point(true)),
        remove =
            button(
                "Удалить",
                () ->
                    this.points
                        .getItems()
                        .remove(this.points.getSelectionModel().getSelectedItem()));
    add.setId("chemistry-fragment-add");
    update.setId("chemistry-fragment-update");
    remove.setId("chemistry-fragment-remove");
    update.disableProperty().bind(this.points.getSelectionModel().selectedItemProperty().isNull());
    remove.disableProperty().bind(this.points.getSelectionModel().selectedItemProperty().isNull());
    this.preview.show(this.source, this.atom.getValue());
    return new VBox(
        8,
        this.points,
        new FlowPane(8, 6, this.atom, this.bond),
        this.label,
        new FlowPane(8, 6, add, update, remove),
        note("Перед сохранением записи добавьте или измените точку, чтобы зафиксировать поля."));
  }

  private String atomName(final AtomId id) {
    if (id == null) {
      return "Выберите атом";
    }
    for (int i = 0; i < this.source.molecule().atoms().size(); i++) {
      final var a = this.source.molecule().atoms().get(i);
      if (a.id().equals(id)) {
        return a.element().symbol().value() + " · " + (i + 1);
      }
    }
    return "Атом отсутствует";
  }

  private void point(final boolean update) {
    try {
      if (this.label.getText().isBlank()) {
        throw new IllegalArgumentException("Укажите подпись точки присоединения.");
      }
      final var p =
          new ChemistryLibraryAttachmentPoint(
              this.atom.getValue(), this.label.getText(), this.bond.getValue());
      final int index = update ? this.points.getSelectionModel().getSelectedIndex() : -1;
      for (int i = 0; i < this.points.getItems().size(); i++) {
        final var other = this.points.getItems().get(i);
        if (i != index && (other.atomId().equals(p.atomId()) || other.label().equals(p.label()))) {
          throw new IllegalArgumentException("Атом или подпись уже используются другой точкой.");
        }
      }
      if (index >= 0) {
        this.points.getItems().set(index, p);
      } else if (this.points.getItems().size() < 64) {
        this.points.getItems().add(p);
      } else {
        throw new IllegalArgumentException("Допустимо не более 64 точек.");
      }
      this.points.getSelectionModel().select(p);
      this.error.setText("");
    } catch (final RuntimeException failure) {
      this.error.setText(failure.getMessage());
    }
  }

  private void validatePoints(final Map<String, String> values) {
    if (!this.kind.equals("fragment")) {
      return;
    }
    final var points = readPoints(values);
    final int selected = Integer.parseInt(values.get("selected"));
    if (points.isEmpty()
        || selected < 0
        || selected >= points.size()
        || !points.get(selected).atomId().value().equals(values.get("atom"))
        || !points.get(selected).label().equals(values.get("label").trim())
        || !points.get(selected).bondType().name().equals(values.get("bond"))) {
      throw new IllegalArgumentException(
          "Нажмите «Добавить точку» или «Изменить точку» перед сохранением.");
    }
  }

  private ChemistryLibraryEntry build(final Map<String, String> values) {
    final var store = new LaboratoryLibraryStore();
    final String description =
        values.get("summary").isBlank()
            ? (this.kind.equals("template")
                ? "План опыта, сохранённый пользователем."
                : "Пользовательская структура.")
            : values.get("summary").trim();
    return this.kind.equals("template")
        ? store.template(
            this.current,
            values.get("name"),
            description,
            lines(values.get("tags")),
            lines(values.get("references")))
        : this.kind.equals("fragment")
            ? store.fragment(
                this.source,
                values.get("name"),
                description,
                lines(values.get("tags")),
                lines(values.get("references")),
                readPoints(values))
            : store.structure(
                this.source,
                values.get("name"),
                description,
                lines(values.get("tags")),
                lines(values.get("references")));
  }

  void busy(final boolean value) {
    this.busy = value;
    this.content.setDisable(value);
    this.save.setDisable(value);
    if (value) {
      this.error.setText("Сохранение записи…");
    }
  }

  void failed(final String message) {
    this.busy(false);
    this.error.setText("Не удалось сохранить: " + message);
  }

  boolean pending() {
    return this.conflict || !this.values().equals(this.initial);
  }

  boolean canClose() {
    if (this.busy) {
      this.error.setText("Дождитесь завершения сохранения записи.");
      return false;
    }
    if (!this.pending()) {
      return true;
    }
    this.error.setText("Сохраните запись или явно отмените ввод.");
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
    return false;
  }

  private Map<String, String> values() {
    final Map<String, String> v = new LinkedHashMap<>();
    v.put("name", this.name.getText());
    v.put("summary", this.summary.getText());
    v.put("tags", this.tags.getText());
    v.put("references", this.references.getText());
    v.put("atom", this.atom.getValue() == null ? "" : this.atom.getValue().value());
    v.put("bond", this.bond.getValue() == null ? "" : this.bond.getValue().name());
    v.put("label", this.label.getText());
    v.put("selected", Integer.toString(this.points.getSelectionModel().getSelectedIndex()));
    v.put("count", Integer.toString(this.points.getItems().size()));
    for (int i = 0; i < this.points.getItems().size(); i++) {
      final var p = this.points.getItems().get(i);
      v.put("point." + i + ".atom", p.atomId().value());
      v.put("point." + i + ".label", p.label());
      v.put("point." + i + ".bond", p.bondType().name());
    }
    return v;
  }

  private static List<ChemistryLibraryAttachmentPoint> readPoints(
      final Map<String, String> values) {
    final int count = Integer.parseInt(values.get("count"));
    if (count < 0 || count > 64) {
      throw new IllegalArgumentException("Недопустимое число точек в черновике.");
    }
    final var result = new ArrayList<ChemistryLibraryAttachmentPoint>();
    for (int i = 0; i < count; i++) {
      result.add(
          new ChemistryLibraryAttachmentPoint(
              AtomId.of(values.get("point." + i + ".atom")),
              values.get("point." + i + ".label"),
              BondType.valueOf(values.get("point." + i + ".bond"))));
    }
    if (result.stream().map(ChemistryLibraryAttachmentPoint::atomId).distinct().count()
            != result.size()
        || result.stream().map(ChemistryLibraryAttachmentPoint::label).distinct().count()
            != result.size()) {
      throw new IllegalArgumentException("В черновике повторяются атомы или подписи точек.");
    }
    return result;
  }

  void draft(final Properties fields, final boolean restore) {
    if (!restore) {
      fields.setProperty("libraryForm.kind", this.kind);
      fields.setProperty("libraryForm.source", this.fingerprint);
      fields.setProperty("libraryForm.conflict", Boolean.toString(this.conflict));
      this.values().forEach((key, value) -> fields.setProperty("libraryForm.input." + key, value));
      return;
    }
    if (!this.kind.equals(fields.getProperty("libraryForm.kind"))
        || !Set.of("true", "false").contains(fields.getProperty("libraryForm.conflict", ""))) {
      throw new IllegalArgumentException("Повреждено состояние библиотечного черновика.");
    }
    final Map<String, String> v = new LinkedHashMap<>();
    fields.stringPropertyNames().stream()
        .filter(k -> k.startsWith("libraryForm.input."))
        .forEach(k -> v.put(k.substring(18), fields.getProperty(k)));
    final var restored = readPoints(v);
    final var expected =
        new HashSet<>(
            List.of(
                "name",
                "summary",
                "tags",
                "references",
                "atom",
                "bond",
                "label",
                "selected",
                "count"));
    for (int i = 0; i < restored.size(); i++) {
      for (final String key : List.of("atom", "label", "bond")) {
        expected.add("point." + i + "." + key);
      }
    }
    if (!expected.equals(v.keySet())) {
      throw new IllegalArgumentException(
          "Неполный или неизвестный набор полей библиотечного черновика.");
    }
    final int selected = Integer.parseInt(v.get("selected"));
    if (selected < -1 || selected >= restored.size()) {
      throw new IllegalArgumentException("Недопустимое выделение точки.");
    }
    final AtomId atom = v.get("atom").isEmpty() ? null : AtomId.of(v.get("atom"));
    final BondType bond = v.get("bond").isEmpty() ? null : BondType.valueOf(v.get("bond"));
    this.points.getItems().setAll(restored);
    this.points.getSelectionModel().select(selected);
    this.name.setText(v.get("name"));
    this.summary.setText(v.get("summary"));
    this.tags.setText(v.get("tags"));
    this.references.setText(v.get("references"));
    this.atom.setValue(atom);
    this.bond.setValue(bond);
    this.label.setText(v.get("label"));
    this.conflict =
        Boolean.parseBoolean(fields.getProperty("libraryForm.conflict"))
            || !this.fingerprint.equals(fields.getProperty("libraryForm.source"));
    this.error.setText(
        this.conflict
            ? "Ввод восстановлен для просмотра: исходный опыт изменился."
            : "Незавершённый ввод восстановлен. Проверьте поля перед сохранением.");
  }

  private static String fingerprint(final LaboratoryDrawing document) {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(
                      new LaboratoryFileCodec()
                          .write(document)
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static List<String> lines(final String value) {
    return value.lines().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
  }

  private static TextArea text(final int rows) {
    final TextArea a = new TextArea();
    a.setWrapText(true);
    a.setPrefRowCount(rows);
    a.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    return a;
  }

  private static Label note(final String text) {
    return LaboratoryStructurePage.note(text);
  }

  private static Button button(final String text, final Runnable action) {
    return LaboratoryStructurePage.button(text, action);
  }
}
