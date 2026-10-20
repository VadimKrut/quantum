/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Locale;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Local drawing guidance only: choosing or expanding help never edits a document. */
final class LaboratoryDrawingGuide extends VBox {
  private final Label context = text("");
  private final Label gestures =
      text("Пробел + перетаскивание — сдвиг листа · Ctrl + колесо — масштаб · Ctrl+Z — отмена");

  LaboratoryDrawingGuide(final Runnable fullHelp) {
    Objects.requireNonNull(fullHelp, "fullHelp");
    this.setId("chemistry-drawing-guide");
    this.getStyleClass().add("laboratory-drawing-guide");
    this.setSpacing(4);
    this.setMinWidth(0);
    this.setMaxWidth(Double.MAX_VALUE);
    this.context.setId("chemistry-drawing-guide-context");
    this.context.getStyleClass().add("laboratory-drawing-guide-title");
    this.context.setMinWidth(0);
    this.context.setMaxWidth(Double.MAX_VALUE);
    this.gestures.getStyleClass().add("laboratory-drawing-guide-tip");
    final ToggleButton toggle = new ToggleButton("Управление и клавиши");
    toggle.setId("chemistry-drawing-guide-toggle");
    toggle.getStyleClass().add("laboratory-drawing-guide-toggle");
    toggle.setMinWidth(Region.USE_PREF_SIZE);
    final Button help = new Button("Подробнее");
    help.setId("chemistry-drawing-guide-help");
    help.getStyleClass().add("laboratory-drawing-guide-help");
    help.setMinWidth(Region.USE_PREF_SIZE);
    help.setTooltip(new Tooltip("Открыть полную справку по рисованию"));
    help.setOnAction(event -> fullHelp.run());
    final HBox header = new HBox(10, this.context, toggle, help);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setMinWidth(0);
    header.getStyleClass().add("laboratory-drawing-guide-header");
    HBox.setHgrow(this.context, Priority.ALWAYS);
    final GridPane keys = new GridPane();
    keys.setHgap(16);
    keys.setVgap(6);
    keys.getStyleClass().add("laboratory-drawing-guide-keys");
    final ColumnConstraints keyColumn = new ColumnConstraints();
    keyColumn.setMinWidth(Region.USE_PREF_SIZE);
    final ColumnConstraints actionColumn = new ColumnConstraints();
    actionColumn.setHgrow(Priority.ALWAYS);
    actionColumn.setFillWidth(true);
    keys.getColumnConstraints().addAll(keyColumn, actionColumn);
    row(
        keys,
        "Элемент в палитре",
        "Выбирает инструмент. Нажмите на атом для замены; на пустой лист — для отдельного атома. Связь добавляется перетаскиванием от атома.");
    row(
        keys,
        "Тянуть от атома",
        "В режиме «Атом» или «Связь» — продолжить структуру; к другому атому — соединить их.");
    row(
        keys,
        "Щелчок по связи",
        "Режим «Атом» или обычный «Связь»: 1 → 2 → 3 → 1. Инструменты 2 и 3 задают кратность. Ароматическая связь не входит в цикл; стереосвязь автоматически не преобразуется.");
    row(keys, "V", "Выбор и перемещение. Рамка — несколько атомов; Shift — добавить к выделению.");
    row(
        keys,
        "C / N / O / S / F / P / H / I",
        "Выбрать элемент для следующего действия на рисунке.");
    row(keys, "L / B", "Выбрать Cl / Br (хлор / бром).");
    row(keys, "Ctrl+C / Ctrl+V", "Скопировать / вставить выделенный фрагмент.");
    row(keys, "Ctrl+S", "Сохранить опыт.");
    row(keys, "1 / 2 / 3", "Инструмент одинарной / двойной / тройной связи.");
    row(keys, "Ctrl+Z", "Отменить последнее изменение.");
    row(keys, "Ctrl+Shift+Z / Ctrl+Y", "Повторить отменённое изменение.");
    row(keys, "Delete", "Удалить выделенное.");
    row(keys, "Esc", "Отменить незавершённый жест.");
    row(keys, "Пробел + перетаскивание", "Сдвинуть лист без изменения структуры.");
    row(keys, "Ctrl + колесо", "Изменить масштаб около курсора.");
    final ScrollPane shortcuts = new ScrollPane(keys);
    shortcuts.setId("chemistry-drawing-guide-shortcuts");
    shortcuts.setFitToWidth(true);
    shortcuts.setPrefViewportHeight(176);
    shortcuts.setMinHeight(0);
    shortcuts.setMaxHeight(196);
    shortcuts.setVisible(false);
    shortcuts.setManaged(false);
    toggle
        .selectedProperty()
        .addListener(
            (observable, before, expanded) -> {
              shortcuts.setVisible(expanded);
              shortcuts.setManaged(expanded);
            });
    this.getChildren().addAll(header, this.gestures, shortcuts);
    this.tool("Выбор", "C", "");
  }

  void tool(final String tool, final String element, final String bondDescription) {
    final String selected = tool == null || tool.isBlank() ? "Выбор" : tool;
    final String atom = element == null || element.isBlank() ? "C" : element;
    final String bond = Objects.toString(bondDescription, "").toLowerCase(Locale.ROOT);
    final String tip =
        switch (selected) {
          case "Атом" ->
              "Атом "
                  + atom
                  + " · щелчок: замена или отдельный атом; тяните от атома для продолжения.";
          case "Связь" ->
              bond.contains("двойн") || bond.contains("double") || bond.equals("2")
                  ? "Двойная связь · нажмите на связь для порядка 2 или протяните от атома."
                  : bond.contains("тройн") || bond.contains("triple") || bond.equals("3")
                      ? "Тройная связь · нажмите на связь для порядка 3 или протяните от атома."
                      : "Связь · щелчок по обычной связи: 1 → 2 → 3 → 1; тяните от атома для продолжения.";
          case "Кольцо" -> "Кольцо · нажмите на свободный лист, атом или связь.";
          case "Рука" -> "Рука · перетаскивайте лист без изменения структуры.";
          case "Разрыв" -> "Разрыв · нажмите на связь, которую хотите удалить.";
          case "Фрагмент" -> "Фрагмент · нажмите на атом, чтобы выбрать связную часть.";
          case "Поворот" ->
              "Поворот · тяните вокруг центра компонента; Alt — свободный угол, Esc — отмена.";
          default ->
              "Выбор · нажмите на атом или выделите рамкой; перетаскивание перемещает выделенное.";
        };
    this.context.setText(tip);
    this.context.setTooltip(new Tooltip(tip));
  }

  private static void row(final GridPane table, final String keys, final String description) {
    final Label shortcut = text(keys);
    shortcut.getStyleClass().add("laboratory-drawing-guide-key");
    final Label action = text(description);
    action.setMinWidth(0);
    action.setMaxWidth(Double.MAX_VALUE);
    table.addRow(table.getRowCount(), shortcut, action);
  }

  private static Label text(final String value) {
    final Label label = new Label(value);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }
}
