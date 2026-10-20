/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryStoichiometry;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;

/** Table projection: exact values for sorting and tooltips, rounded values for reading. */
final class LaboratoryStoichiometryTable extends TableView<LaboratoryStoichiometry.Row> {
  private final List<TableColumn<LaboratoryStoichiometry.Row, ?>> additional;
  private boolean entered;
  private final TableColumn<LaboratoryStoichiometry.Row, LaboratoryQuantity> input =
      new TableColumn<>("План");

  LaboratoryStoichiometryTable(final Consumer<String> edit, final Consumer<String> reference) {
    this.setId("chemistry-stoichiometry-table");
    this.setMinWidth(0);
    this.setMinHeight(240);
    this.setPlaceholder(new Label("Добавьте участников стадии"));
    this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    final var component =
        text(
            "Компонент",
            "component",
            280,
            200,
            row ->
                row.component().molecule().displayName()
                    + (this.entered ? "\n" + row.component().evidence().quantityLabel() : ""),
            true);
    final var role =
        text(
            "Роль",
            "role",
            105,
            92,
            row -> LaboratoryInspector.roleText(row.participant().role()),
            false);
    configure(input, "input", 140, 126);
    input.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().input()));
    input.setSortable(false); // Different physical dimensions have no common ordering.
    input.setCellFactory(
        column ->
            new TableCell<>() {
              @Override
              protected void updateItem(final LaboratoryQuantity value, final boolean empty) {
                super.updateItem(value, empty);
                this.setAlignment(Pos.CENTER_RIGHT);
                this.setText(
                    empty
                        ? null
                        : value == null ? "—" : display(value.value()) + " " + value.unit());
                final String exact =
                    value == null
                        ? "Не указано"
                        : value.value().stripTrailingZeros().toPlainString() + " " + value.unit();
                this.setTooltip(empty ? null : new Tooltip(exact));
                this.setAccessibleText(empty ? null : exact);
              }
            });
    final var amount = decimal("ммоль", "amount", 100, 90, row -> milli(row.amount().moles()));
    final var equivalents =
        decimal("Экв.", "equivalents", 90, 74, LaboratoryStoichiometry.Row::equivalents);
    final var coefficient =
        decimal(
            "Коэфф.",
            "coefficient",
            80,
            74,
            row ->
                row.participant().coefficient() == null
                    ? null
                    : BigDecimal.valueOf(row.participant().coefficient().value()));
    final var theoryAmount =
        decimal("Теория, ммоль", "theory-amount", 130, 122, row -> milli(row.theoreticalMoles()));
    final var theoryMass =
        decimal(
            "Теория, мг",
            "theory-mass",
            120,
            108,
            row ->
                row.theoreticalMoles() == null
                        || !row.component().properties().matches(row.component().molecule())
                        || row.component().properties().molarMass() == null
                    ? null
                    : milli(
                        row.theoreticalMoles().multiply(row.component().properties().molarMass())));
    final var yield =
        decimal("Выход, %", "yield", 110, 96, LaboratoryStoichiometry.Row::yieldPercent);
    this.additional = List.of(coefficient, theoryAmount, theoryMass, yield);
    this.getColumns()
        .setAll(
            List.of(
                component,
                role,
                input,
                amount,
                equivalents,
                coefficient,
                theoryAmount,
                theoryMass,
                yield));
    this.details(false);
    this.setTooltip(
        new Tooltip(
            "Двойной щелчок или Enter — количество и свойства. ≈ — округлено; точное значение доступно в подсказке."));
    this.setRowFactory(
        table -> {
          final TableRow<LaboratoryStoichiometry.Row> row = new TableRow<>();
          row.setOnMouseClicked(
              event -> {
                if (!row.isEmpty()
                    && event.getButton() == MouseButton.PRIMARY
                    && event.getClickCount() == 2) {
                  edit.accept(row.getItem().component().id());
                  event.consume();
                }
              });
          final MenuItem change = new MenuItem("Количество и свойства…");
          change.setOnAction(
              event -> {
                if (!row.isEmpty()) {
                  edit.accept(row.getItem().component().id());
                }
              });
          final MenuItem anchor = new MenuItem("Принять за 1 эквивалент");
          anchor.setOnAction(
              event -> {
                if (!row.isEmpty()
                    && row.getItem().participant().role() == ReactionComponentRole.REACTANT) {
                  reference.accept(row.getItem().component().id());
                }
              });
          final ContextMenu menu = new ContextMenu(change, anchor);
          row.itemProperty()
              .addListener(
                  (observable, before, after) -> {
                    row.setContextMenu(after == null ? null : menu);
                    anchor.setDisable(
                        after == null
                            || after.participant().role() != ReactionComponentRole.REACTANT);
                  });
          return row;
        });
    this.setOnKeyPressed(
        event -> {
          if ((event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.F2)
              && !event.isControlDown()
              && !event.isAltDown()
              && !event.isMetaDown()
              && !event.isShiftDown()) {
            final var selected = this.getSelectionModel().getSelectedItem();
            if (selected != null) {
              edit.accept(selected.component().id());
              event.consume();
            }
          }
        });
  }

  void basis(final LaboratoryStoichiometry.Basis basis) {
    this.entered = basis == LaboratoryStoichiometry.Basis.ACTUAL;
    this.input.setText(this.entered ? "Внесено" : "План");
  }

  void details(final boolean show) {
    this.setColumnResizePolicy(
        show
            ? TableView.UNCONSTRAINED_RESIZE_POLICY
            : TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.additional.forEach(column -> column.setVisible(show));
  }

  static String display(final BigDecimal value) {
    if (value == null) {
      return "—";
    }
    final BigDecimal rounded = value.round(new MathContext(6, RoundingMode.HALF_UP));
    return (rounded.compareTo(value) == 0 ? "" : "≈ ")
        + rounded.stripTrailingZeros().toPlainString();
  }

  private static BigDecimal milli(final BigDecimal value) {
    return value == null ? null : value.movePointRight(3);
  }

  private static TableColumn<LaboratoryStoichiometry.Row, String> text(
      final String name,
      final String id,
      final double width,
      final double minimum,
      final Function<LaboratoryStoichiometry.Row, String> value,
      final boolean wrap) {
    final TableColumn<LaboratoryStoichiometry.Row, String> column = new TableColumn<>(name);
    configure(column, id, width, minimum);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setCellFactory(
        unused ->
            new TableCell<>() {
              private final Label wrapped = new Label();

              {
                if (wrap) {
                  this.wrapped.setWrapText(true);
                  this.wrapped.prefWidthProperty().bind(column.widthProperty().subtract(24));
                  this.wrapped.setMinSize(
                      javafx.scene.layout.Region.USE_PREF_SIZE,
                      javafx.scene.layout.Region.USE_PREF_SIZE);
                  this.wrapped.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                  this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                  this.prefHeightProperty()
                      .bind(
                          javafx.beans.binding.Bindings.createDoubleBinding(
                              () ->
                                  this.isEmpty()
                                      ? 32.0
                                      : Math.max(
                                          32,
                                          this.wrapped.prefHeight(
                                                  Math.max(1, column.getWidth() - 24))
                                              + 16),
                              this.wrapped.textProperty(),
                              column.widthProperty(),
                              this.wrapped.fontProperty(),
                              this.emptyProperty()));
                }
              }

              @Override
              protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(empty ? null : item);
                this.setWrapText(wrap);
                if (wrap) {
                  this.wrapped.setText(empty ? null : item);
                  this.setGraphic(empty ? null : this.wrapped);
                }
                this.setAlignment(Pos.CENTER_LEFT);
                this.setTooltip(empty || item == null ? null : new Tooltip(item));
                this.setAccessibleText(empty ? null : item);
              }
            });
    return column;
  }

  private static TableColumn<LaboratoryStoichiometry.Row, BigDecimal> decimal(
      final String name,
      final String id,
      final double width,
      final double minimum,
      final Function<LaboratoryStoichiometry.Row, BigDecimal> value) {
    final TableColumn<LaboratoryStoichiometry.Row, BigDecimal> column = new TableColumn<>(name);
    configure(column, id, width, minimum);
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
    column.setComparator(Comparator.nullsLast(BigDecimal::compareTo));
    column.setCellFactory(
        unused ->
            new TableCell<>() {
              @Override
              protected void updateItem(final BigDecimal item, final boolean empty) {
                super.updateItem(item, empty);
                this.setText(empty ? null : display(item));
                this.setAlignment(Pos.CENTER_RIGHT);
                final String exact =
                    item == null ? "Не определено" : item.stripTrailingZeros().toPlainString();
                this.setTooltip(empty ? null : new Tooltip(exact));
                this.setAccessibleText(empty ? null : exact);
              }
            });
    return column;
  }

  private static void configure(
      final TableColumn<?, ?> column, final String id, final double width, final double minimum) {
    column.setId("chemistry-stoichiometry-" + id);
    column.setPrefWidth(width);
    column.setMinWidth(minimum);
  }
}
