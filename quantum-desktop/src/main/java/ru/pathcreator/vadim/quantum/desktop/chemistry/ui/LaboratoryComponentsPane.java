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
import java.util.Locale;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** One component table shared by the drawing dock and the expanded workspace. */
final class LaboratoryComponentsPane extends BorderPane {
  private final TableView<LaboratoryComponent> table;
  private final ObservableList<LaboratoryComponent> source = FXCollections.observableArrayList();
  private final FilteredList<LaboratoryComponent> filtered = new FilteredList<>(this.source);
  private final TextField search = new TextField();
  private final ComboBox<String> roles = new ComboBox<>();
  private final CheckBox allColumns = new CheckBox("Все столбцы");
  private final Label count = new Label();
  private final TextArea detail = new TextArea();
  private final Button amount = new Button("Количество"),
      structure = new Button("2D"),
      expand = new Button("Развернуть");
  private final MenuButton loading = new MenuButton("Загрузка");
  private final FlowPane filters = new FlowPane(8, 6);
  private final VBox header = new VBox(7);
  private final TableColumn<LaboratoryComponent, String> name = new TableColumn<>("Компонент");
  private final TableColumn<LaboratoryComponent, String> plan = new TableColumn<>("План"),
      fact = new TableColumn<>("Записано");
  private final TableColumn<LaboratoryComponent, String> evidence = new TableColumn<>("Источник"),
      atoms = new TableColumn<>("Атомов / связей");
  private final Consumer<String> quantify, select, showStructure, showLoading;
  private final Consumer<Boolean> expandAction;
  private List<LaboratoryReactionStage> stages = List.of();
  private boolean expanded, changing;
  private String lastSelectedId;
  private final javafx.animation.PauseTransition reveal =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(30));

  LaboratoryComponentsPane(
      final TableView<LaboratoryComponent> table,
      final Consumer<String> quantify,
      final Consumer<String> select,
      final Consumer<String> showStructure,
      final Consumer<String> showLoading,
      final Consumer<Boolean> expandAction) {
    this.table = table;
    this.quantify = quantify;
    this.select = select;
    this.showStructure = showStructure;
    this.showLoading = showLoading;
    this.expandAction = expandAction;
    this.setId("chemistry-components-pane");
    this.setMinSize(0, 80);
    this.table.setId("chemistry-components");
    this.table.setFixedCellSize(76);
    this.table.setMinSize(0, 70);
    final SortedList<LaboratoryComponent> sorted = new SortedList<>(this.filtered);
    sorted.comparatorProperty().bind(this.table.comparatorProperty());
    this.table.setItems(sorted);
    this.name.setCellValueFactory(
        cell -> new ReadOnlyStringWrapper(cell.getValue().molecule().displayName()));
    this.name.setCellFactory(
        column ->
            new TableCell<>() {
              private final Label title = new Label(), role = new Label();
              private final VBox content = new VBox(2, this.title, this.role);

              {
                this.title.setWrapText(true);
                this.title.setMaxHeight(34);
                this.title.setMinHeight(Region.USE_PREF_SIZE);
                this.title.setMinWidth(0);
                this.content.prefWidthProperty().bind(this.widthProperty().subtract(16));
                this.content.maxWidthProperty().bind(this.content.prefWidthProperty());
                this.title.prefWidthProperty().bind(this.widthProperty().subtract(16));
                this.role.setStyle("-fx-font-size: 11; -fx-opacity: .78;");
                this.content.setPadding(Insets.EMPTY);
              }

              @Override
              protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(null);
                this.setGraphic(null);
                this.setTooltip(null);
                if (!empty
                    && value != null
                    && this.getIndex() >= 0
                    && this.getIndex() < table.getItems().size()) {
                  final LaboratoryComponent component = table.getItems().get(this.getIndex());
                  this.title.setText(value);
                  this.role.setText(LaboratoryComponentsPane.this.roleText(component));
                  this.setGraphic(this.content);
                  this.setTooltip(new Tooltip(value + "\n" + this.role.getText()));
                }
              }
            });
    this.plan.setCellValueFactory(
        cell -> new ReadOnlyStringWrapper(quantity(cell.getValue().planned())));
    this.fact.setCellValueFactory(
        cell -> new ReadOnlyStringWrapper(quantity(cell.getValue().actual())));
    this.evidence.setCellValueFactory(
        cell -> new ReadOnlyStringWrapper(cell.getValue().evidence().toString()));
    this.atoms.setCellValueFactory(
        cell ->
            new ReadOnlyStringWrapper(
                cell.getValue().molecule().atoms().size()
                    + " / "
                    + cell.getValue().molecule().bonds().size()));
    this.plan.setCellFactory(unused -> this.quantityCell(true));
    this.fact.setCellFactory(unused -> this.quantityCell(false));
    for (final var column : List.of(this.evidence, this.atoms)) {
      column.setCellFactory(unused -> wrappedCell());
    }
    this.table
        .getColumns()
        .setAll(List.of(this.name, this.plan, this.fact, this.evidence, this.atoms));
    this.table.setPlaceholder(new Label("Нет компонентов для выбранного фильтра"));
    this.table.setRowFactory(
        unused -> {
          final TableRow<LaboratoryComponent> row = new TableRow<>();
          row.setOnMouseClicked(
              event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                  this.quantify.accept(row.getItem().id());
                  event.consume();
                }
              });
          return row;
        });
    this.table.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (!event.isAltDown()
              && (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.F2)) {
            final var component = this.table.getSelectionModel().getSelectedItem();
            if (component != null) {
              if (event.isShortcutDown()) {
                this.showStructure.accept(component.id());
              } else {
                this.quantify.accept(component.id());
              }
            }
            event.consume();
          }
        });
    this.table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, before, component) -> {
              this.selection();
              if (!this.changing && component != null) {
                this.select.accept(component.id());
              }
            });
    this.search.setId("chemistry-components-search");
    this.search.setPromptText("Название или роль компонента");
    this.search.setPrefWidth(260);
    this.roles.setId("chemistry-components-role");
    this.roles.getItems().setAll("Все компоненты", "Исходники", "Продукты", "Реагенты и среда");
    this.roles.getSelectionModel().selectFirst();
    this.allColumns.setId("chemistry-components-all-columns");
    this.allColumns.setOnAction(event -> this.columns());
    this.search.textProperty().addListener((observable, before, after) -> this.filter());
    this.roles.valueProperty().addListener((observable, before, after) -> this.filter());
    this.search.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ESCAPE && !this.search.getText().isEmpty()) {
            this.search.clear();
            event.consume();
          }
        });
    this.amount.setId("chemistry-components-quantity");
    this.amount.setTooltip(
        new Tooltip("План, записанное количество, источник и свойства компонента · Enter / F2"));
    this.structure.setId("chemistry-components-structure");
    this.structure.setTooltip(new Tooltip("Показать выбранную молекулу крупно в 2D · Ctrl+Enter"));
    this.expand.setId("chemistry-components-expand");
    this.loading.setId("chemistry-components-loading");
    this.amount.setOnAction(event -> this.selected(this.quantify));
    this.structure.setOnAction(event -> this.selected(this.showStructure));
    this.expand.setOnAction(event -> this.expandAction.accept(!this.expanded));
    final FlowPane actions =
        new FlowPane(5, 4, this.amount, this.structure, this.loading, this.expand);
    actions.setPadding(new Insets(5));
    this.filters.getChildren().setAll(this.search, this.roles, this.allColumns, this.count);
    this.filters.setPadding(new Insets(5));
    this.header.getChildren().setAll(actions, this.filters);
    this.setTop(this.header);
    this.setCenter(this.table);
    this.detail.setId("chemistry-components-detail");
    this.detail.setEditable(false);
    this.detail.setWrapText(true);
    this.detail.setPrefRowCount(4);
    this.detail.setMinHeight(65);
    this.detail.setMaxHeight(110);
    this.widthProperty().addListener((observable, before, after) -> this.columns());
    this.reveal.setOnFinished(
        event -> {
          final var component = this.table.getSelectionModel().getSelectedItem();
          if (component != null && this.table.getScene() != null) {
            this.table.getScene().getRoot().applyCss();
            this.table.getScene().getRoot().layout();
            if (this.table.lookup(".virtual-flow")
                instanceof javafx.scene.control.skin.VirtualFlow<?> flow) {
              // Equal-height rows give an exact position even when scrollTo retains pre-resize
              // cells.
              final int index = this.table.getItems().indexOf(component);
              flow.setPosition(
                  Math.max(0, index) / (double) Math.max(1, this.table.getItems().size() - 1));
              flow.requestLayout();
            } else {
              this.table.scrollTo(component);
            }
          }
        });
    this.table.heightProperty().addListener((observable, before, after) -> this.revealSelected());
    this.table.sceneProperty().addListener((observable, before, after) -> this.revealSelected());
    this.expanded(false);
    this.selection();
  }

  private TableCell<LaboratoryComponent, String> quantityCell(final boolean planned) {
    return new TableCell<>() {
      private final Label number = new Label(), unit = new Label(), origin = new Label();
      private final VBox content = new VBox(2, this.number, this.unit, this.origin);

      {
        this.unit.setStyle("-fx-font-size: 11; -fx-opacity: .8;");
        this.number.setMinWidth(0);
        this.origin.setStyle("-fx-font-size: 10;");
        this.origin.setWrapText(true);
        this.origin.setMinWidth(0);
        this.origin.setMaxHeight(28);
        this.origin.prefWidthProperty().bind(this.widthProperty().subtract(12));
        this.origin.setManaged(!planned);
        this.origin.setVisible(!planned);
        this.number.prefWidthProperty().bind(this.widthProperty().subtract(12));
        this.content.setPadding(new Insets(4, 0, 4, 0));
      }

      @Override
      protected void updateItem(final String text, final boolean empty) {
        super.updateItem(text, empty);
        this.setText(null);
        this.setGraphic(null);
        this.setTooltip(null);
        if (!empty && this.getIndex() >= 0 && this.getIndex() < table.getItems().size()) {
          final var component = table.getItems().get(this.getIndex());
          final var value = planned ? component.planned() : component.actual();
          if (value == null) {
            this.number.setText("—");
            this.unit.setText("");
            this.origin.setText("");
            this.setTooltip(new Tooltip("Не указано"));
          } else {
            final var rounded =
                value
                    .value()
                    .round(new java.math.MathContext(6, java.math.RoundingMode.HALF_UP))
                    .stripTrailingZeros();
            final boolean approximate = rounded.compareTo(value.value()) != 0;
            final String display =
                rounded.precision() - rounded.scale() > 8 || rounded.scale() > 6
                    ? rounded.toEngineeringString()
                    : rounded.toPlainString();
            this.number.setText((approximate ? "≈ " : "") + display);
            this.unit.setText(value.unit().toString());
            this.origin.setText(
                planned
                    ? ""
                    : component.evidence()
                            == ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                .LaboratoryEvidence.PLANNED
                        ? "Без подтверждения"
                        : component.evidence().quantityLabel());
            this.setTooltip(
                new Tooltip(
                    "Точное значение: "
                        + value
                        + (planned ? "" : "\n" + component.evidence())
                        + (approximate
                            ? "\nВ таблице округлено; расчёт и файл сохраняют полную точность."
                            : "")));
          }
          this.setGraphic(this.content);
        }
      }
    };
  }

  private static TableCell<LaboratoryComponent, String> wrappedCell() {
    return new TableCell<>() {
      private final Label value = new Label();

      {
        this.value.setWrapText(true);
        this.value.setMinWidth(0);
        this.value.prefWidthProperty().bind(this.widthProperty().subtract(14));
      }

      @Override
      protected void updateItem(final String text, final boolean empty) {
        super.updateItem(text, empty);
        this.setText(null);
        this.setGraphic(null);
        this.setTooltip(null);
        if (!empty && text != null) {
          this.value.setText(text);
          this.setGraphic(this.value);
          this.setTooltip(new Tooltip(text.equals("—") ? "Не указано" : text));
        }
      }
    };
  }

  private static String quantity(final LaboratoryQuantity quantity) {
    return quantity == null ? "—" : quantity.toString();
  }

  private static String normalized(final String value) {
    return value.toLowerCase(Locale.ROOT).replace('ё', 'е').replace('\u00a0', ' ').strip();
  }

  private void selected(final Consumer<String> action) {
    final var component = this.table.getSelectionModel().getSelectedItem();
    if (component != null) {
      action.accept(component.id());
    }
  }

  void load(
      final List<LaboratoryComponent> values,
      final String selectedId,
      final List<LaboratoryReactionStage> stages) {
    this.changing = true;
    try {
      this.stages = List.copyOf(stages);
      if (!this.source.equals(values)) {
        this.source.setAll(values);
      }
      this.filter();
      this.table.refresh();
      this.table.getSelectionModel().clearSelection();
      this.table.getItems().stream()
          .filter(component -> component.id().equals(selectedId))
          .findFirst()
          .ifPresent(this.table.getSelectionModel()::select);
      this.selection();
      this.count.setText(this.filtered.size() + " из " + this.source.size());
      if (!java.util.Objects.equals(this.lastSelectedId, selectedId)) {
        this.revealSelected();
      }
      this.lastSelectedId = selectedId;
    } finally {
      this.changing = false;
    }
  }

  void expanded(final boolean expanded) {
    this.expanded = expanded;
    this.expand.setText(expanded ? "К рисунку" : "Развернуть");
    this.filters.setVisible(expanded);
    this.filters.setManaged(expanded);
    this.setBottom(expanded ? this.detail : null);
    this.columns();
    this.filter();
    this.revealSelected();
  }

  /** VirtualFlow must finish resizing before it can reveal a row against its new viewport. */
  void revealSelected() {
    this.reveal.playFromStart();
  }

  void resetFilters() {
    this.search.clear();
    this.roles.getSelectionModel().selectFirst();
  }

  boolean clearSearch() {
    if (this.search.getText().isBlank()) {
      return false;
    }
    this.search.clear();
    return true;
  }

  void focusSearch() {
    this.search.requestFocus();
    this.search.selectAll();
  }

  private void filter() {
    final var selected = this.table.getSelectionModel().getSelectedItem();
    final boolean wasChanging = this.changing;
    this.changing = true;
    try {
      final String query = normalized(this.search.getText());
      final int role = this.roles.getSelectionModel().getSelectedIndex();
      this.filtered.setPredicate(
          component -> {
            if (!this.expanded) {
              return true;
            }
            final var roles = this.roles(component);
            if (role == 1 && !roles.contains(ReactionComponentRole.REACTANT)
                || role == 2 && !roles.contains(ReactionComponentRole.PRODUCT)
                || role == 3
                    && roles.stream()
                        .noneMatch(
                            r ->
                                r != ReactionComponentRole.REACTANT
                                    && r != ReactionComponentRole.PRODUCT)) {
              return false;
            }
            final String haystack =
                normalized(component.molecule().displayName() + " " + this.roleText(component));
            return query.isBlank()
                || java.util.Arrays.stream(query.split("\\s+")).allMatch(haystack::contains);
          });
      if (selected != null && this.filtered.contains(selected)) {
        this.table.getSelectionModel().select(selected);
      } else {
        this.table.getSelectionModel().clearSelection();
      }
      this.count.setText(this.filtered.size() + " из " + this.source.size());
      this.selection();
    } finally {
      this.changing = wasChanging;
    }
  }

  private List<ReactionComponentRole> roles(final LaboratoryComponent component) {
    final var roles =
        this.stages.stream()
            .flatMap(stage -> stage.participants().stream())
            .filter(participant -> participant.componentId().equals(component.id()))
            .map(participant -> participant.role())
            .distinct()
            .toList();
    return roles.isEmpty() ? List.of(component.role()) : roles;
  }

  private String roleText(final LaboratoryComponent component) {
    return this.roles(component).stream()
        .map(LaboratoryInspector::roleText)
        .collect(java.util.stream.Collectors.joining(" / "));
  }

  private void columns() {
    final boolean all = this.expanded && this.allColumns.isSelected();
    this.evidence.setVisible(all);
    this.atoms.setVisible(all);
    this.table.setColumnResizePolicy(
        all
            ? TableView.UNCONSTRAINED_RESIZE_POLICY
            : TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    this.name.setMinWidth(140);
    this.name.setPrefWidth(all ? 300 : Math.max(140, this.getWidth() - 200));
    for (final var column : List.of(this.plan, this.fact)) {
      column.setMinWidth(84);
      column.setPrefWidth(all ? 130 : 100);
    }
    this.evidence.setMinWidth(160);
    this.evidence.setPrefWidth(180);
    this.atoms.setMinWidth(120);
    this.atoms.setPrefWidth(120);
  }

  private void selection() {
    final var component = this.table.getSelectionModel().getSelectedItem();
    this.amount.setDisable(component == null);
    this.structure.setDisable(component == null);
    this.detail.setText(
        component == null
            ? "Выберите компонент для полных значений. Enter — количество; Ctrl+Enter — рисунок."
            : component.molecule().displayName()
                + "\n"
                + this.roleText(component)
                + " · "
                + component.evidence()
                + "\nПлан: "
                + quantity(component.planned())
                + "    "
                + component.evidence().quantityLabel()
                + ": "
                + quantity(component.actual())
                + "\nАтомов: "
                + component.molecule().atoms().size()
                + " · Связей: "
                + component.molecule().bonds().size());
    this.loading.getItems().clear();
    for (final var stage : this.stages) {
      if (component == null
          || stage.participants().stream()
              .anyMatch(participant -> participant.componentId().equals(component.id()))) {
        final MenuItem item = new MenuItem(stage.title());
        item.setOnAction(event -> this.showLoading.accept(stage.id()));
        this.loading.getItems().add(item);
      }
    }
    this.loading.setDisable(this.loading.getItems().isEmpty());
    this.loading.setTooltip(
        new Tooltip(
            this.loading.isDisabled()
                ? "Для расчёта загрузки нужна стадия реакции с этим компонентом"
                : "Загрузка, лимитирующий реагент и выход выбранной стадии"));
  }
}
