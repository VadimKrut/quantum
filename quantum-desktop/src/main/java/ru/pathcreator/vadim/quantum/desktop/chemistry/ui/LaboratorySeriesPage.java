/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySeriesPlan;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySeriesRepository;

/** Editable series drafts retain raw input; only verified independent plans are published. */
final class LaboratorySeriesPage extends LaboratoryComputationPage {
  private record Choice(String id, String title) {
    @Override
    public String toString() {
      return this.title;
    }
  }

  static final class Row {
    final StringProperty title = new SimpleStringProperty(),
        amount = new SimpleStringProperty(),
        temperature = new SimpleStringProperty(),
        hours = new SimpleStringProperty(),
        equivalents = new SimpleStringProperty(),
        note = new SimpleStringProperty(),
        conditionsNote = new SimpleStringProperty();

    List<StringProperty> fields() {
      return List.of(
          this.title,
          this.amount,
          this.temperature,
          this.hours,
          this.equivalents,
          this.note,
          this.conditionsNote);
    }
  }

  private final LaboratoryDrawing source;
  private final BiConsumer<Path, List<LaboratoryOverviewPage.ReferenceHint>> open;
  private List<LaboratoryOverviewPage.ReferenceHint> publishedReferences = List.of();
  private final ComboBox<Choice> stages = new ComboBox<>(),
      reference = new ComboBox<>(),
      varied = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> unit = new ComboBox<>();
  private final CheckBox solvents = new CheckBox("Масштабировать растворители");
  private final TextField parent = new TextField(), folder = new TextField("Новая серия");
  private final TableView<Row> table = new TableView<>();
  private final VBox preview = new VBox(8), form = new VBox(12);
  private final Button create = new Button("Создать серию"),
      stop = new Button("Остановить"),
      openFolder = new Button("Открыть серию");
  private final Label summary = note(""), context = note("");
  private final TextField rowEquivalents = new TextField();
  private final TextArea rowNote = new TextArea(), rowConditions = new TextArea();
  private final TitledPane rowDetails = new TitledPane(), destination = new TitledPane();
  private Row editingRow;
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private List<LaboratorySeriesPlan.Prepared> prepared = List.of();
  private boolean changing, dirty, created;
  private int sequence;
  private Path result;

  LaboratorySeriesPage(
      final LaboratoryDrawing source,
      final Path parent,
      final Consumer<Path> open,
      final Runnable close) {
    this(source, parent, (path, hints) -> open.accept(path), close);
  }

  LaboratorySeriesPage(
      final LaboratoryDrawing source,
      final Path parent,
      final BiConsumer<Path, List<LaboratoryOverviewPage.ReferenceHint>> open,
      final Runnable close) {
    super("Серия опытов из шаблона", close);
    this.source = source;
    this.open = open;
    this.setId("chemistry-series-page");
    this.changing = true;
    this.parent.setText(parent == null ? "" : parent.toString());
    this.parent.setPromptText("Папка проекта — полный путь");
    this.parent.setId("chemistry-series-parent");
    this.folder.setId("chemistry-series-folder");
    this.stages.setId("chemistry-series-stage");
    this.reference.setId("chemistry-series-reference");
    this.varied.setId("chemistry-series-varied");
    this.unit.setId("chemistry-series-unit");
    this.stages.setPrefWidth(240);
    this.reference.setPrefWidth(280);
    this.varied.setPrefWidth(280);
    this.unit.getItems().setAll(LaboratoryQuantityUnit.values());
    source
        .experiment()
        .stages()
        .forEach(s -> this.stages.getItems().add(new Choice(s.id(), s.title())));
    this.stages.getSelectionModel().selectFirst();
    this.solvents.setSelected(true);
    this.stages
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (this.changing) {
                return;
              }
              if (this.pending()) {
                this.changing = true;
                this.stages.setValue(a);
                this.changing = false;
                this.status.setText(
                    "Сначала сохраните серию или сбросьте варианты, чтобы сменить стадию.");
                return;
              }
              this.setup();
            });
    this.reference
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (this.changing) {
                return;
              }
              if (this.pending()) {
                this.changing = true;
                this.reference.setValue(a);
                this.changing = false;
                this.status.setText(
                    "Сначала сохраните серию или сбросьте варианты, чтобы сменить опорное вещество.");
                return;
              }
              this.defaults();
            });
    this.unit.valueProperty().addListener((o, a, b) -> this.convert(a, b));
    this.varied
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (this.changing) {
                return;
              }
              if (this.table.getItems().stream().anyMatch(r -> !r.equivalents.get().isBlank())) {
                this.changing = true;
                this.varied.setValue(a);
                this.changing = false;
                this.status.setText(
                    "Очистите столбец эквивалентов перед сменой вещества: значения относятся к прежнему участнику.");
                return;
              }
              this.changed();
            });
    this.solvents.selectedProperty().addListener((o, a, b) -> this.changed());
    this.parent.textProperty().addListener((o, a, b) -> this.changed());
    this.folder.textProperty().addListener((o, a, b) -> this.changed());
    final FlowPane selectors =
        new FlowPane(
            12,
            8,
            labeled("Стадия шаблона", this.stages),
            labeled("Опорный исходник", this.reference),
            labeled("Единица загрузки", this.unit));
    final VBox setup =
        new VBox(
            10,
            note(source.experiment().title()),
            selectors,
            this.solvents,
            note(
                "Создаются отдельные планы. Фактические результаты, журнал и вложения в новые опыты не переносятся."));
    final TitledPane settings = new TitledPane("Исходный опыт и единицы", setup);
    settings.setAnimated(false);
    settings.setExpanded(false);
    settings.setId("chemistry-series-settings");
    this.table.setId("chemistry-series-table");
    this.table.setMinHeight(100);
    this.table.setPrefHeight(140);
    this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.table
        .getColumns()
        .addAll(
            column("Вариант", r -> r.title, 260),
            column("Загрузка", r -> r.amount, 140),
            column("°C", r -> r.temperature, 100),
            column("Часы", r -> r.hours, 100));
    this.table.setPlaceholder(note("В шаблоне нужна стадия с исходным веществом."));
    this.table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              this.editRow(b);
              this.preview();
            });
    this.rowEquivalents.setId("chemistry-series-row-equivalents");
    this.rowNote.setId("chemistry-series-row-note");
    this.rowConditions.setId("chemistry-series-row-conditions");
    this.rowNote.setPrefRowCount(2);
    this.rowNote.setWrapText(true);
    this.rowConditions.setPrefRowCount(3);
    this.rowConditions.setWrapText(true);
    this.rowDetails.setText("Эквиваленты и заметки выбранного варианта");
    this.rowDetails.setId("chemistry-series-row-details");
    this.rowDetails.setAnimated(false);
    this.rowDetails.setExpanded(false);
    this.rowDetails.setContent(
        new VBox(
            10,
            new FlowPane(
                12,
                8,
                labeled("Вещество для изменения эквивалентов", this.varied),
                labeled("Эквиваленты в этом варианте", this.rowEquivalents)),
            note(
                "Пустые эквиваленты сохраняют пропорцию шаблона. Выбранное вещество относится ко всей серии."),
            labeled("Заметка к варианту", this.rowNote),
            labeled(
                "Описание условий — проверьте после изменения °C и часов", this.rowConditions)));
    final MenuButton more = new MenuButton("Ещё");
    more.setId("chemistry-series-more");
    more.getItems()
        .addAll(
            menu("Загрузка ×½", () -> this.scale(new BigDecimal("0.5"))),
            menu("Загрузка ×2", () -> this.scale(new BigDecimal("2"))),
            menu("Удалить выбранный вариант", this::remove),
            new SeparatorMenuItem(),
            menu(
                "Сбросить варианты",
                () -> {
                  this.created = false;
                  this.defaults();
                }));
    final FlowPane actions =
        new FlowPane(
            8,
            8,
            action("Добавить вариант", "add", () -> this.add(null)),
            action(
                "Дублировать выбранный",
                "duplicate",
                () -> this.add(this.table.getSelectionModel().getSelectedItem())),
            more);
    final TitledPane detail = new TitledPane("Количества выбранного варианта", this.preview);
    detail.setAnimated(false);
    detail.setExpanded(true);
    detail.setMinHeight(Region.USE_PREF_SIZE);
    this.preview.setMinHeight(Region.USE_PREF_SIZE);
    detail.setId("chemistry-series-preview");
    this.destination.setText("Куда сохранить серию");
    this.destination.setContent(
        new VBox(
            8,
            labeled("Папка проекта", this.parent),
            labeled("Новая подпапка серии", this.folder)));
    this.destination.setAnimated(false);
    this.destination.setExpanded(this.parent.getText().isBlank());
    this.destination.setId("chemistry-series-destination");
    this.form
        .getChildren()
        .setAll(
            this.context,
            settings,
            note("Каждая строка — отдельный опыт. Измените загрузку или условия прямо в таблице."),
            actions,
            this.table,
            this.summary,
            this.rowDetails,
            detail,
            this.destination);
    this.form.setPadding(new Insets(8, 16, 12, 16));
    this.form.setMinHeight(Region.USE_PREF_SIZE);
    final ScrollPane scroll = new ScrollPane(this.form);
    scroll.setFitToWidth(true);
    scroll.setId("chemistry-series-scroll");
    this.setCenter(scroll);
    this.create.setId("chemistry-series-create");
    this.create.getStyleClass().add("laboratory-primary-action");
    this.create.setOnAction(e -> this.write());
    this.stop.setOnAction(
        e -> {
          this.cancelled.set(true);
          this.stop.setDisable(true);
          this.status.setText("Останавливаю создание серии…");
        });
    this.stop.setDisable(true);
    this.stop.visibleProperty().bind(this.stop.disabledProperty().not());
    this.stop.managedProperty().bind(this.stop.visibleProperty());
    this.openFolder.setId("chemistry-series-open");
    this.openFolder.setDisable(true);
    this.openFolder.visibleProperty().bind(this.openFolder.disabledProperty().not());
    this.openFolder.managedProperty().bind(this.openFolder.visibleProperty());
    this.openFolder.setOnAction(
        e -> {
          if (this.result != null) {
            this.open.accept(this.result, this.publishedReferences);
          }
        });
    final VBox footer =
        new VBox(8, new FlowPane(8, 8, this.create, this.stop, this.openFolder), this.status);
    footer.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(footer);
    this.changing = false;
    this.setup();
  }

  private static VBox labeled(final String text, final javafx.scene.Node node) {
    return new VBox(4, note(text), node);
  }

  private Button action(final String title, final String id, final Runnable action) {
    final Button b = new Button(title);
    b.setId("chemistry-series-" + id);
    b.setOnAction(
        e -> {
          if (!this.running && !this.created) {
            try {
              action.run();
            } catch (final RuntimeException x) {
              this.status.setText(x.getMessage());
            }
          }
        });
    return b;
  }

  private MenuItem menu(final String title, final Runnable action) {
    final MenuItem item = new MenuItem(title);
    item.setOnAction(
        e -> {
          if (!this.running && !this.created) {
            try {
              action.run();
            } catch (final RuntimeException x) {
              this.status.setText(x.getMessage());
            }
          }
        });
    return item;
  }

  private void editRow(final Row row) {
    if (this.editingRow != null) {
      this.rowEquivalents.textProperty().unbindBidirectional(this.editingRow.equivalents);
      this.rowNote.textProperty().unbindBidirectional(this.editingRow.note);
      this.rowConditions.textProperty().unbindBidirectional(this.editingRow.conditionsNote);
    }
    this.editingRow = row;
    this.rowDetails.setDisable(row == null);
    this.rowEquivalents.setText(row == null ? "" : row.equivalents.get());
    this.rowNote.setText(row == null ? "" : row.note.get());
    this.rowConditions.setText(row == null ? "" : row.conditionsNote.get());
    if (row != null) {
      this.rowEquivalents.textProperty().bindBidirectional(row.equivalents);
      this.rowNote.textProperty().bindBidirectional(row.note);
      this.rowConditions.textProperty().bindBidirectional(row.conditionsNote);
    }
  }

  private TableColumn<Row, String> column(
      final String title, final Function<Row, StringProperty> value, final double width) {
    final TableColumn<Row, String> c = new TableColumn<>(title);
    c.setPrefWidth(width);
    c.setSortable(false);
    c.setCellValueFactory(v -> value.apply(v.getValue()));
    c.setCellFactory(
        v ->
            new TableCell<>() {
              private final TextField input = new TextField();
              private StringProperty bound;

              {
                this.input.setStyle("-fx-background-color: transparent; -fx-padding: 7 4;");
                this.input.setOnMousePressed(
                    e ->
                        LaboratorySeriesPage.this
                            .table
                            .getSelectionModel()
                            .select(this.getIndex()));
                this.input.setAccessibleText(title);
              }

              @Override
              protected void updateItem(final String text, final boolean empty) {
                super.updateItem(text, empty);
                if (this.bound != null) {
                  this.input.textProperty().unbindBidirectional(this.bound);
                  this.bound = null;
                }
                if (empty
                    || this.getIndex() < 0
                    || this.getIndex() >= this.getTableView().getItems().size()) {
                  this.setGraphic(null);
                } else {
                  this.bound = value.apply(this.getTableView().getItems().get(this.getIndex()));
                  this.input.setText(this.bound.get());
                  this.input.textProperty().bindBidirectional(this.bound);
                  this.setGraphic(this.input);
                }
              }
            });
    return c;
  }

  private LaboratoryReactionStage stage() {
    return this.source.experiment().stages().stream()
        .filter(s -> this.stages.getValue() != null && s.id().equals(this.stages.getValue().id()))
        .findFirst()
        .orElse(null);
  }

  private LaboratoryComponent component(final String id) {
    return this.source.experiment().components().stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private void setup() {
    this.changing = true;
    this.reference.getItems().clear();
    this.varied.getItems().setAll(new Choice("", "Не варьировать"));
    final var stage = this.stage();
    if (stage != null) {
      for (final var p : stage.participants()) {
        final var c = this.component(p.componentId());
        if (p.role() == ReactionComponentRole.REACTANT) {
          this.reference.getItems().add(new Choice(c.id(), c.molecule().displayName()));
        }
        if (p.role() != ReactionComponentRole.PRODUCT
            && p.role() != ReactionComponentRole.SOLVENT) {
          this.varied.getItems().add(new Choice(c.id(), c.molecule().displayName()));
        }
      }
    }
    this.reference.getSelectionModel().selectFirst();
    this.varied.getSelectionModel().selectFirst();
    this.changing = false;
    this.defaults();
  }

  private void defaults() {
    this.changing = true;
    this.table.getItems().clear();
    this.sequence = 0;
    final var ref = this.reference.getValue();
    this.unit.setValue(
        ref == null || this.component(ref.id()).planned() == null
            ? LaboratoryQuantityUnit.MILLIMOLE
            : this.component(ref.id()).planned().unit());
    this.changing = false;
    if (ref != null) {
      this.add(null);
    }
    this.dirty = false;
    this.refresh();
  }

  private void add(final Row copy) {
    if (this.reference.getValue() == null || this.table.getItems().size() >= 48) {
      this.status.setText("Выберите исходник; в серии допускается до 48 вариантов.");
      return;
    }
    final Row row = new Row();
    final var cond = LaboratorySeriesPlan.defaults(this.source, this.stage());
    final var ref = this.component(this.reference.getValue().id());
    final List<String> values =
        copy == null
            ? List.of(
                "Вариант " + (++this.sequence),
                this.defaultAmount(ref),
                cond.temperature() == null ? "" : decimal(cond.temperature().kelvin() - 273.15),
                cond.duration() == null ? "" : decimal(cond.duration().seconds() / 3600),
                "",
                "",
                Objects.toString(cond.note(), ""))
            : copy.fields().stream().map(StringProperty::get).toList();
    for (int i = 0; i < values.size(); i++) {
      row.fields().get(i).set(values.get(i));
      row.fields().get(i).addListener((o, a, b) -> this.changed());
    }
    if (copy != null) {
      row.title.set(
          copy.title.get().replaceFirst(" — копия(?: \\d+)?$", "")
              + " — копия "
              + (++this.sequence));
    }
    this.table.getItems().add(row);
    this.table.getSelectionModel().select(row);
    this.changed();
  }

  private void remove() {
    final Row row = this.table.getSelectionModel().getSelectedItem();
    if (row != null) {
      this.table.getItems().remove(row);
      this.table.getSelectionModel().selectFirst();
      this.changed();
    }
  }

  private void scale(final BigDecimal factor) {
    final Row row = this.table.getSelectionModel().getSelectedItem();
    if (row != null) {
      row.amount.set(
          number(row.amount.get(), "Загрузка")
              .multiply(factor)
              .stripTrailingZeros()
              .toPlainString());
    }
  }

  private String defaultAmount(final LaboratoryComponent ref) {
    if (ref.planned() == null) {
      return "";
    }
    final var q = ref.planned();
    try {
      return (q.unit().dimension().equals(this.unit.getValue().dimension())
              ? q.in(this.unit.getValue())
              : LaboratoryBatchPlan.fromMoles(
                  ref, LaboratoryBatchPlan.moles(ref, q), this.unit.getValue()))
          .value()
          .toPlainString();
    } catch (final RuntimeException x) {
      throw new IllegalArgumentException(
          "Не удалось подготовить загрузку нового варианта: " + x.getMessage());
    }
  }

  private static String shown(final BigDecimal value) {
    final var shortValue = value.round(new MathContext(6));
    return (shortValue.compareTo(value) == 0 ? "" : "≈ ")
        + shortValue.stripTrailingZeros().toPlainString();
  }

  private static String decimal(final double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  private static BigDecimal number(final String text, final String label) {
    try {
      if (text.length() > 100) {
        throw new NumberFormatException();
      }
      final BigDecimal n = new BigDecimal(text.strip().replace(',', '.'));
      if (n.precision() > 50 || Math.abs(n.scale()) > 50) {
        throw new NumberFormatException();
      }
      return n;
    } catch (final NumberFormatException x) {
      throw new IllegalArgumentException(label + ": введите число.");
    }
  }

  private void convert(final LaboratoryQuantityUnit before, final LaboratoryQuantityUnit after) {
    if (this.changing || before == null || after == null || this.reference.getValue() == null) {
      return;
    }
    try {
      final var c = this.component(this.reference.getValue().id());
      final List<String> values = new ArrayList<>();
      for (final Row row : this.table.getItems()) {
        final var q = new LaboratoryQuantity(number(row.amount.get(), "Загрузка"), before);
        values.add(
            (before.dimension().equals(after.dimension())
                    ? q.in(after)
                    : LaboratoryBatchPlan.fromMoles(c, LaboratoryBatchPlan.moles(c, q), after))
                .value()
                .toPlainString());
      }
      this.changing = true;
      for (int i = 0; i < values.size(); i++) {
        this.table.getItems().get(i).amount.set(values.get(i));
      }
      this.changing = false;
      this.changed();
    } catch (final RuntimeException x) {
      this.changing = true;
      this.unit.setValue(before);
      this.changing = false;
      this.status.setText("Единица не изменена: " + x.getMessage());
    }
  }

  private void changed() {
    if (this.changing) {
      return;
    }
    this.dirty = true;
    this.refresh();
  }

  private void refresh() {
    this.context.setText(
        this.stage() == null
            ? "Выберите стадию в настройках исходного опыта."
            : this.stage().title()
                + " · "
                + (this.reference.getValue() == null
                    ? "исходник не выбран"
                    : this.reference.getValue().title()));
    this.table.getColumns().get(1).setText("Загрузка, " + this.unit.getValue());
    this.table.setPrefHeight(Math.max(100, Math.min(360, 48 * this.table.getItems().size() + 42)));
    this.prepared = List.of();
    if (this.created) {
      return;
    }
    try {
      if (this.stage() == null
          || this.reference.getValue() == null
          || this.table.getItems().isEmpty()) {
        throw new IllegalArgumentException("Нужна стадия, исходник и хотя бы один вариант.");
      }
      final List<LaboratorySeriesPlan.Prepared> plans = new ArrayList<>();
      final var defaults = LaboratorySeriesPlan.defaults(this.source, this.stage());
      int index = 0;
      for (final Row row : this.table.getItems()) {
        index++;
        try {
          final var temperature =
              row.temperature.get().isBlank()
                  ? null
                  : Temperature.of(
                      number(row.temperature.get(), "Температура").doubleValue(),
                      TemperatureUnit.CELSIUS);
          final var duration =
              row.hours.get().isBlank()
                  ? null
                  : TimeDuration.of(
                      number(row.hours.get(), "Часы").doubleValue(), TimeDurationUnit.HOUR);
          final Map<String, BigDecimal> eq = new HashMap<>();
          if (!row.equivalents.get().isBlank()) {
            if (this.varied.getValue() == null || this.varied.getValue().id().isEmpty()) {
              throw new IllegalArgumentException("Выберите вещество для столбца эквивалентов.");
            }
            eq.put(this.varied.getValue().id(), number(row.equivalents.get(), "Эквиваленты"));
          }
          final var cond =
              ReactionConditions.of(
                  temperature,
                  defaults.pressure(),
                  duration,
                  defaults.components(),
                  defaults.features(),
                  row.conditionsNote.get());
          plans.add(
              LaboratorySeriesPlan.prepare(
                  this.source,
                  this.stage().id(),
                  this.reference.getValue().id(),
                  new LaboratorySeriesPlan.Variant(
                      row.title.get(),
                      new LaboratoryQuantity(
                          number(row.amount.get(), "Загрузка"), this.unit.getValue()),
                      cond,
                      eq,
                      row.note.get()),
                  this.solvents.isSelected()));
        } catch (final RuntimeException x) {
          throw new IllegalArgumentException("Вариант " + index + ": " + x.getMessage());
        }
      }
      this.prepared = List.copyOf(plans);
      this.summary.setText(
          "Готово к созданию: " + plans.size() + " · единица загрузки: " + this.unit.getValue());
      this.status.setText("Проверьте количества и создайте серию, когда варианты готовы.");
    } catch (final RuntimeException x) {
      this.summary.setText(x.getMessage());
      this.status.setText("Исправьте отмеченное значение; введённый текст сохранён.");
    }
    this.create.setDisable(this.prepared.isEmpty() || this.running);
    this.preview();
  }

  private void preview() {
    this.preview.getChildren().clear();
    final int index = this.table.getSelectionModel().getSelectedIndex();
    if (index < 0 || index >= this.prepared.size()) {
      this.preview
          .getChildren()
          .add(note("Пересчёт появится, когда все варианты заполнены корректно."));
      return;
    }
    final var plan = this.prepared.get(index);
    this.preview.getChildren().add(note(plan.drawing().experiment().title()));
    for (final var row : plan.theory().rows()) {
      final var c = row.component();
      final String value =
          row.participant().role() == ReactionComponentRole.PRODUCT
              ? (row.theoreticalMoles() == null
                  ? "Теория не определена"
                  : "Теория: " + shown(row.theoreticalMoles().movePointRight(3)) + " ммоль")
              : (c.planned() == null
                  ? "Количество не задано"
                  : shown(c.planned().value()) + " " + c.planned().unit());
      final Label line = note(c.molecule().displayName() + " · " + value);
      line.setTooltip(
          new Tooltip(
              "Точное значение: "
                  + (c.planned() == null
                      ? Objects.toString(row.theoreticalMoles(), "не определено") + " моль"
                      : c.planned().toString())));
      this.preview.getChildren().add(line);
    }
    final var product =
        plan.drawing().experiment().components().stream()
            .filter(c -> c.role() == ReactionComponentRole.PRODUCT)
            .findFirst();
    if (product.isPresent()) {
      final var drawing = plan.drawing().layouts().get(product.get().id());
      final var viewport = new LaboratoryAdaptivePreview(() -> drawing);
      viewport.setId("chemistry-series-structure-preview");
      this.preview.getChildren().add(viewport);
    }
    final String conditions = plan.drawing().experiment().stages().getFirst().conditions().note();
    if (conditions != null && !conditions.isBlank()) {
      this.preview
          .getChildren()
          .add(note("Текст условий не пересчитывается при изменении загрузки, °C и часов."));
      final TextArea text = new TextArea(conditions);
      text.setEditable(false);
      text.setWrapText(true);
      text.setPrefRowCount(4);
      final TitledPane description = new TitledPane("Полное описание условий", text);
      description.setAnimated(false);
      description.setExpanded(false);
      this.preview.getChildren().add(description);
    }
    plan.warnings().forEach(w -> this.preview.getChildren().add(note(w)));
    plan.theory().diagnostics().forEach(w -> this.preview.getChildren().add(note(w)));
  }

  private void write() {
    if (this.running || this.created) {
      return;
    }
    this.refresh();
    if (this.prepared.isEmpty()) {
      return;
    }
    final Path destination;
    final String name;
    try {
      if (this.parent.getText().isBlank()) {
        throw new IllegalArgumentException("Укажите папку проекта.");
      }
      destination = Path.of(this.parent.getText().strip());
      if (!destination.isAbsolute()) {
        throw new IllegalArgumentException("Укажите полный путь к папке проекта.");
      }
      name = LaboratorySeriesRepository.folderName(this.folder.getText());
    } catch (final RuntimeException x) {
      this.destination.setExpanded(true);
      this.status.setText(x.getMessage());
      return;
    }
    final var plans = this.prepared;
    final String selectedStage = this.stage().id(),
        selectedReference = this.reference.getValue().id();
    final var hints =
        plans.stream()
            .map(
                p ->
                    LaboratoryOverviewPage.ReferenceHint.from(
                        p.drawing(), selectedStage, selectedReference))
            .toList();
    this.cancelled.set(false);
    this.busy(true);
    this.form.setDisable(true);
    this.create.setDisable(true);
    this.stop.setDisable(false);
    this.status.setText("Готовлю серию…");
    final Thread thread =
        new Thread(
            () -> {
              try {
                final var saved =
                    new LaboratorySeriesRepository()
                        .write(
                            destination,
                            name,
                            plans,
                            this.cancelled::get,
                            message ->
                                Platform.runLater(
                                    () -> {
                                      if (!this.disposed) {
                                        this.status.setText(message);
                                      }
                                    }));
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.result = saved.folder();
                        this.publishedReferences = hints;
                        this.created = true;
                        this.dirty = false;
                        this.done();
                        this.openFolder.setDisable(false);
                        this.status.setText(
                            "Создано опытов: " + saved.documents().size() + " · " + saved.folder());
                      }
                    });
              } catch (final Exception x) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.done();
                        this.status.setText("Серия не создана: " + x.getMessage());
                      }
                    });
              }
            },
            "laboratory-series-writer");
    thread.setDaemon(true);
    thread.start();
  }

  private void done() {
    this.busy(false);
    this.form.setDisable(this.created);
    this.stop.setDisable(true);
    this.create.setDisable(this.created || this.prepared.isEmpty());
  }

  boolean pending() {
    return this.dirty && !this.created;
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) {
      return false;
    }
    if (this.pending()) {
      this.status.setText(
          "Есть несохранённые варианты. Создайте серию или нажмите «Сбросить варианты», чтобы вернуться к опыту.");
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    this.editRow(null);
    this.cancelled.set(true);
    super.dispose();
  }

  void draft(final Properties fields, final boolean restore) {
    if (!restore) {
      fields.setProperty("series.details", Boolean.toString(this.rowDetails.isExpanded()));
      fields.setProperty("series.destination", Boolean.toString(this.destination.isExpanded()));
      fields.setProperty(
          "series.selected", Integer.toString(this.table.getSelectionModel().getSelectedIndex()));
      fields.setProperty(
          "series.stage", this.stages.getValue() == null ? "" : this.stages.getValue().id());
      fields.setProperty(
          "series.reference",
          this.reference.getValue() == null ? "" : this.reference.getValue().id());
      fields.setProperty(
          "series.varied", this.varied.getValue() == null ? "" : this.varied.getValue().id());
      fields.setProperty("series.unit", this.unit.getValue().name());
      fields.setProperty("series.solvents", Boolean.toString(this.solvents.isSelected()));
      fields.setProperty("series.parent", this.parent.getText());
      fields.setProperty("series.folder", this.folder.getText());
      fields.setProperty("series.count", Integer.toString(this.table.getItems().size()));
      for (int i = 0; i < this.table.getItems().size(); i++) {
        for (int j = 0; j < 7; j++) {
          fields.setProperty(
              "series." + i + "." + j, this.table.getItems().get(i).fields().get(j).get());
        }
      }
      return;
    }
    this.changing = true;
    this.stages.setValue(
        this.stages.getItems().stream()
            .filter(c -> c.id().equals(fields.getProperty("series.stage")))
            .findFirst()
            .orElseThrow());
    this.changing = false;
    this.setup();
    this.changing = true;
    try {
      this.reference.setValue(
          this.reference.getItems().stream()
              .filter(c -> c.id().equals(fields.getProperty("series.reference")))
              .findFirst()
              .orElseThrow());
      this.varied.setValue(
          this.varied.getItems().stream()
              .filter(c -> c.id().equals(fields.getProperty("series.varied")))
              .findFirst()
              .orElseThrow());
      this.unit.setValue(LaboratoryQuantityUnit.valueOf(fields.getProperty("series.unit")));
      this.solvents.setSelected(Boolean.parseBoolean(fields.getProperty("series.solvents")));
      this.parent.setText(fields.getProperty("series.parent"));
      this.folder.setText(fields.getProperty("series.folder"));
      final int count = Integer.parseInt(fields.getProperty("series.count"));
      if (count < 0 || count > 48) {
        throw new IllegalArgumentException("Неверное число вариантов в черновике.");
      }
      this.table.getItems().clear();
      for (int i = 0; i < count; i++) {
        final Row row = new Row();
        for (int j = 0; j < 7; j++) {
          row.fields().get(j).set(fields.getProperty("series." + i + "." + j, ""));
          row.fields().get(j).addListener((o, a, b) -> this.changed());
        }
        this.table.getItems().add(row);
      }
      this.sequence = count;
      this.table
          .getSelectionModel()
          .select(
              Math.max(
                  0,
                  Math.min(
                      count - 1, Integer.parseInt(fields.getProperty("series.selected", "0")))));
      this.rowDetails.setExpanded(
          Boolean.parseBoolean(fields.getProperty("series.details", "false")));
      this.destination.setExpanded(
          Boolean.parseBoolean(
              fields.getProperty(
                  "series.destination", Boolean.toString(this.parent.getText().isBlank()))));
    } finally {
      this.changing = false;
    }
    this.dirty = true;
    this.refresh();
  }
}
