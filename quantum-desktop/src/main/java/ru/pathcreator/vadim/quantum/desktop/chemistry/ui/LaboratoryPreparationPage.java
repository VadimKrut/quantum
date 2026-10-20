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
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Review a whole stage, then persist a single allocation packet with the stock ledger's CAS guard. */
final class LaboratoryPreparationPage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final LaboratoryFiles files;
  private final Path ledger;
  private final String experimentId;
  private final LaboratoryInventoryRepository repository = new LaboratoryInventoryRepository();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "stock-preparation");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean saving, dirty, restoring, sourceChanged, uncertain, ledgerExisted;
  private LaboratoryInventory inventory = LaboratoryInventory.empty();
  private Map<String, LaboratoryInventory.Stock> stock = Map.of();
  private String fingerprint;
  private Properties conflictDraft;
  private LaboratoryStockPreparation.Command command;
  private Properties deferred;
  private final ComboBox<LaboratoryReactionStage> stage = new ComboBox<>();
  private final FlowPane cards = new FlowPane(12, 12);
  private final VBox reserveRows = new VBox(10), review = new VBox(8), discard = new VBox(8);
  private final TabPane tabs = new TabPane();
  private final ListView<String> history = new ListView<>();
  private final CheckBox allProject = new CheckBox("Все резервы проекта");
  private final TextField reason = new TextField("Подготовка реагентов к стадии");
  private final Label summary = note("Читаю запасы проекта…");
  private final Button reserve, consume, release, reload, post, cancel, transfer;
  private TitledPane reasonPane;
  private final List<NeedRow> needs = new ArrayList<>();
  private final List<ReservedRow> held = new ArrayList<>();
  private Map<String, Set<String>> matches = Map.of();
  private Consumer<String> openSolutions;
  private Runnable afterDiscard;

  void solutions(final Consumer<String> callback) {
    this.openSolutions = callback;
  }

  LaboratoryPreparationPage(
      final LaboratoryEditor editor,
      final LaboratoryFiles files,
      final Path ledger,
      final String stageId,
      final Runnable close) {
    super("Подготовка реагентов", close);
    this.editor = editor;
    this.files = files;
    this.ledger = ledger.toAbsolutePath().normalize();
    this.experimentId = editor.document().experiment().id();
    this.setId("chemistry-preparation-page");
    this.stage.getItems().setAll(editor.document().experiment().stages());
    this.stage.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(LaboratoryReactionStage s) {
            return s == null ? "Выберите стадию" : s.title();
          }

          public LaboratoryReactionStage fromString(String s) {
            throw new UnsupportedOperationException();
          }
        });
    this.stage.setValue(
        this.stage.getItems().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElse(this.stage.getItems().isEmpty() ? null : this.stage.getItems().getFirst()));
    this.stage.setMaxWidth(480);
    this.stage.setId("preparation-stage");
    this.reserve = button("Проверить выбранные партии", "reserve", this::reserve);
    this.reserve.getStyleClass().add("laboratory-save");
    this.consume = button("Проверить фактический расход", "consume", () -> this.change(true));
    this.consume.getStyleClass().add("laboratory-save");
    this.release = button("Снять выбранное количество", "release", () -> this.change(false));
    this.reload = button("Обновить запасы", "reload", () -> this.load(this.capture()));
    this.transfer = button("Перенести расход в факт опыта…", "transfer", this::transfer);
    this.post = button("Провести изменения", "post", this::post);
    this.post.getStyleClass().add("laboratory-save");
    this.cancel =
        button(
            "Вернуться к выбору",
            "cancel",
            () -> {
              if (this.uncertain) {
                throw new IllegalArgumentException(
                    "Сначала обновите запасы и проверьте результат предыдущей записи.");
              }
              this.command = null;
              this.showReview(false);
            });
    this.cards.setAlignment(javafx.geometry.Pos.TOP_LEFT);
    this.cards.setRowValignment(javafx.geometry.VPos.TOP);
    this.cards.setPadding(new Insets(8));
    this.cards.setMinHeight(Region.USE_PREF_SIZE);
    this.cards.prefWrapLengthProperty().bind(this.cards.widthProperty());
    this.reserveRows.setPadding(new Insets(8));
    this.reserveRows.setMinHeight(Region.USE_PREF_SIZE);
    final ScrollPane preparationScroll = scroll(this.cards),
        reservationsScroll = scroll(this.reserveRows);
    preparationScroll.setFitToHeight(false);
    reservationsScroll.setFitToHeight(false);
    final VBox preparation =
        new VBox(
            10,
            note("Выберите партии и количество для стадии"),
            preparationScroll,
            this.reserve,
            disclosure(
                "Заполнение и список для работы",
                "list-tools",
                new FlowPane(
                    8,
                    6,
                    button("Заполнить все строки из плана", "fill", this::fill),
                    button("Копировать список", "copy", this::copy))));
    VBox.setVgrow(preparationScroll, Priority.ALWAYS);
    final VBox reservations =
        new VBox(
            10,
            note("Запишите, сколько действительно взяли из резерва"),
            reservationsScroll,
            this.consume,
            disclosure(
                "Снять резерв или перенести расход в опыт",
                "reservation-actions",
                new FlowPane(8, 6, this.release, this.transfer)),
            disclosure(
                "Фильтр и массовый выбор",
                "reservation-selection",
                new FlowPane(
                    8,
                    6,
                    this.allProject,
                    button(
                        "Выбрать видимые",
                        "select",
                        () -> this.held.forEach(h -> h.select.setSelected(true))),
                    button(
                        "Весь остаток",
                        "remaining",
                        () ->
                            this.held.stream()
                                .filter(h -> h.select.isSelected())
                                .forEach(
                                    h ->
                                        h.amount.setText(
                                            h.state.remaining().value().toPlainString()))))));
    VBox.setVgrow(reservationsScroll, Priority.ALWAYS);
    this.history.setCellFactory(
        v ->
            new ListCell<>() {
              {
                this.setWrapText(true);
                this.prefWidthProperty().bind(v.widthProperty().subtract(24));
                this.setMaxWidth(USE_PREF_SIZE);
              }

              protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                this.setText(empty ? null : s);
              }
            });
    this.tabs
        .getTabs()
        .setAll(
            new Tab("Выбрать партии", preparation),
            new Tab("Записать расход", reservations),
            new Tab("История", this.history));
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setId("preparation-tabs");
    VBox.setVgrow(this.tabs, Priority.ALWAYS);
    this.reason.setPromptText("Основание операции");
    this.reason.setId("preparation-reason");
    this.reason
        .textProperty()
        .addListener(
            o -> {
              this.touch();
              if (this.reasonPane != null) {
                this.reasonPane.setText(
                    this.reason.getText().isBlank()
                        ? "Укажите основание записи"
                        : "Основание записи: " + shortText(this.reason.getText(), 80));
              }
            });
    this.reasonPane =
        disclosure("Основание записи: " + this.reason.getText(), "reason-details", this.reason);
    final VBox body =
        new VBox(
            10,
            new FlowPane(8, 6, note("Стадия"), this.stage, this.reload),
            this.summary,
            this.tabs,
            this.reasonPane,
            this.review);
    body.setPadding(new Insets(0, 16, 8, 16));
    body.setMinSize(0, 0);
    this.setCenter(body);
    this.review.setId("preparation-review");
    this.review.setPadding(new Insets(12));
    this.review.getStyleClass().add("laboratory-reaction-card");
    this.showReview(false);
    this.discard
        .getChildren()
        .setAll(
            note("Выбор партий и введённые количества ещё не проведены."),
            new FlowPane(
                8,
                6,
                button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      if (this.saving || this.uncertain) {
                        throw new IllegalArgumentException(
                            "Сначала дождитесь записи или обновите запасы.");
                      }
                      this.dirty = false;
                      this.command = null;
                      this.deferred = null;
                      this.conflictDraft = null;
                      if (this.afterDiscard != null) {
                        final var next = this.afterDiscard;
                        this.afterDiscard = null;
                        next.run();
                      } else {
                        this.finish();
                      }
                    }),
                button("Продолжить работу", "continue", () -> visible(this.discard, false))));
    visible(this.discard, false);
    final VBox bottom = new VBox(8, this.discard, this.status);
    bottom.setPadding(new Insets(8, 16, 14, 16));
    this.setBottom(bottom);
    this.stage
        .valueProperty()
        .addListener(
            (o, old, selected) -> {
              if (this.restoring) {
                return;
              }
              if (this.pending()) {
                this.restoring = true;
                this.stage.setValue(old);
                this.restoring = false;
                this.status.setText("Сначала завершите или отбросьте текущий выбор партий.");
                return;
              }
              this.load(null);
            });
    this.allProject.setOnAction(
        e -> {
          if (this.held.stream()
              .anyMatch(h -> h.select.isSelected() || !h.amount.getText().isBlank())) {
            this.allProject.setSelected(!this.allProject.isSelected());
            this.status.setText("Завершите текущий выбор резервов перед сменой фильтра.");
          } else {
            this.renderHeld();
          }
        });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            if (this.command != null && !this.post.isDisabled()) {
              this.post();
            } else if (this.tabs.getSelectionModel().getSelectedIndex() == 1
                && !this.consume.isDisabled()) {
              this.change(true);
            } else if (this.tabs.getSelectionModel().getSelectedIndex() == 0
                && !this.reserve.isDisabled()) {
              this.reserve();
            }
            e.consume();
          }
        });
    this.load(null);
  }

  private Button button(final String text, final String id, final Runnable action) {
    final Button b = new Button(text);
    b.setId("preparation-" + id);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException x) {
            this.status.setText(message(x));
          }
        });
    return b;
  }

  private static TitledPane disclosure(
      final String title, final String id, final javafx.scene.Node content) {
    final var pane = new TitledPane(title, content);
    pane.setId("preparation-" + id);
    pane.setExpanded(false);
    pane.setAnimated(false);
    return pane;
  }

  private static String shortText(final String text, final int length) {
    return text.length() <= length ? text : text.substring(0, length - 1) + "…";
  }

  private static String message(final Throwable e) {
    return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
  }

  private static void visible(final javafx.scene.Node n, final boolean v) {
    n.setVisible(v);
    n.setManaged(v);
  }

  private void touch() {
    if (!this.restoring) {
      this.dirty = true;
    }
  }

  private String stageId() {
    return this.stage.getValue() == null ? "" : this.stage.getValue().id();
  }

  private String quantity(final LaboratoryQuantity q) {
    return q == null ? "не задано" : LaboratoryQuantityText.format(q, true);
  }

  private LaboratoryBatch batch(final String id) {
    return this.inventory.batches().stream()
        .filter(b -> b.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Партия больше не найдена."));
  }

  private String lot(final LaboratoryBatch b) {
    final var m =
        this.inventory.materials().stream()
            .filter(v -> v.id().equals(b.materialId()))
            .findFirst()
            .orElseThrow();
    return m.name() + " · " + b.label() + (b.location() == null ? "" : " · " + b.location());
  }

  private static String localTime(final Instant time) {
    return java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
        .format(time);
  }

  private static String concise(final String text, final int limit) {
    return shortText(
        Objects.requireNonNullElse(text, "").replace('\n', ' ').replace('\r', ' ').strip(), limit);
  }

  private static String batchCode(final String id) {
    try {
      return HexFormat.of()
          .withUpperCase()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
              0,
              5);
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private String batchTitle(final LaboratoryBatch batch) {
    final boolean duplicate =
        this.inventory.batches().stream()
            .anyMatch(
                other ->
                    !other.id().equals(batch.id())
                        && other.label().strip().equalsIgnoreCase(batch.label().strip()));
    return concise(batch.label(), 100) + (duplicate ? " · #" + batchCode(batch.id()) : "");
  }

  private String batchContext(final LaboratoryBatch batch, final boolean source) {
    final List<String> parts = new ArrayList<>();
    this.inventory.operations().stream()
        .filter(
            o ->
                o.batchId().equals(batch.id()) && o.kind() == LaboratoryStockOperation.Kind.RECEIPT)
        .map(LaboratoryStockOperation::recordedAt)
        .min(Comparator.naturalOrder())
        .ifPresent(time -> parts.add("Приход " + localTime(time)));
    if (!batch.location().isBlank()) {
      parts.add(concise(batch.location(), 70));
    }
    if (source && !batch.source().isBlank()) {
      parts.add("Источник: " + concise(batch.source(), 130));
    }
    return String.join(" · ", parts);
  }

  private ListCell<LaboratoryBatch> batchCell(final boolean selected) {
    return new ListCell<>() {
      {
        this.setWrapText(true);
        if (!selected) {
          this.setPrefWidth(430);
          this.setMaxWidth(430);
        }
      }

      @Override
      protected void updateItem(final LaboratoryBatch batch, final boolean empty) {
        super.updateItem(batch, empty);
        this.setText(null);
        this.setGraphic(null);
        this.setTooltip(null);
        if (empty || batch == null) {
          return;
        }
        final String context = batchContext(batch, !selected);
        final String material =
            inventory.materials().stream()
                .filter(m -> m.id().equals(batch.materialId()))
                .map(LaboratoryMaterial::name)
                .findFirst()
                .orElse("");
        this.setText(
            batchTitle(batch)
                + (!selected && !material.equals(batch.label())
                    ? "\n" + concise(material, 120)
                    : "")
                + (!selected ? "\nДоступно " + quantity(stock.get(batch.id()).available()) : "")
                + (context.isBlank() ? "" : "\n" + context));
        this.setTooltip(
            new Tooltip(
                lot(batch)
                    + "\nID партии: "
                    + batch.id()
                    + "\nИсточник: "
                    + (batch.source().isBlank() ? "не указан" : batch.source())));
      }
    };
  }

  private static Label decimalLabel(
      final String prefix, final BigDecimal value, final String unit) {
    final BigDecimal shown =
        value.round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP));
    final Label label =
        note(
            prefix
                + (shown.compareTo(value) == 0 ? "" : "≈ ")
                + shown.stripTrailingZeros().toPlainString()
                + " "
                + unit);
    label.setTooltip(new Tooltip(prefix + value.toPlainString() + " " + unit));
    return label;
  }

  private VBox concentrationCard(
      final LaboratoryInventory inventory,
      final LaboratoryComponent component,
      final LaboratorySolutionUse.Prepared prepared,
      final LaboratoryQuantity volume,
      final String warning) {
    final var standard = prepared.standard();
    final var batch = prepared.batch();
    final Label heading = note(volume == null ? "Концентрация партии" : "Отобрать для загрузки");
    final Label result =
        volume == null ? note("Объём пока не рассчитан") : LaboratoryQuantityText.label("", volume);
    result.setId("preparation-volume-result");
    result.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
    final Label plan =
        component.planned() == null
            ? note("Плановое количество не задано")
            : LaboratoryQuantityText.label("План: ", component.planned());
    plan.setId("preparation-concentration-plan");
    final Label concentration = decimalLabel("c = ", prepared.properties().molarity(), "моль/л");
    concentration.setId("preparation-concentration-value");
    final VBox resultCard = new VBox(6, heading, result, new FlowPane(20, 6, plan, concentration));
    resultCard.getStyleClass().add("laboratory-reaction-card");
    resultCard.setPadding(new Insets(12));
    if (standard.measured().unit().family().equals("mass")) {
      resultCard
          .getChildren()
          .add(
              new FlowPane(
                  16,
                  6,
                  decimalLabel(
                      "Записанная массовая концентрация: ",
                      standard.measured().value(),
                      standard.measured().unit().toString()),
                  decimalLabel("M = ", prepared.properties().molarMass(), "г/моль")));
    }
    if (volume == null) {
      final Label missing =
          note(
              "Для объёма: "
                  + Objects.requireNonNullElse(warning, "проверьте исходные количества"));
      missing.setId("preparation-volume-warning");
      resultCard.getChildren().add(missing);
    }
    final Label batchName = note("Партия: " + this.batchTitle(batch));
    batchName.setTooltip(new Tooltip(batch.label() + "\n" + batch.id()));
    final Label measured =
        note(
            "Концентрация записана "
                + localTime(standard.time())
                + " · "
                + concise(standard.method(), 100));
    measured.setTooltip(new Tooltip(standard.method()));
    final Label source = note("Источник: " + concise(standard.source(), 200));
    source.setId("preparation-concentration-source");
    source.setTooltip(new Tooltip(standard.source()));
    final TextArea full =
        new TextArea(
            prepared.explanation()
                + "\n\nID партии: "
                + batch.id()
                + "\nID записи концентрации: "
                + standard.id()
                + "\nИсточник партии: "
                + (batch.source().isBlank() ? "не указан" : batch.source())
                + "\n\nРасчёт записи концентрации:\n"
                + (standard.calculation().isBlank() ? "не задан" : standard.calculation())
                + (volume == null
                    ? ""
                    : "\n\nТочный объём: " + LaboratoryQuantityText.format(volume, false)));
    full.setId("preparation-concentration-provenance");
    full.setEditable(false);
    full.setWrapText(true);
    full.setPrefRowCount(8);
    final TitledPane details =
        disclosure(
            "Полное происхождение и исходные записи",
            "concentration-details",
            new VBox(8, full, new LaboratoryBatchOriginPane(inventory, batch.id())));
    return new VBox(
        10,
        note(component.molecule().displayName()),
        resultCard,
        batchName,
        measured,
        source,
        details,
        note(
            volume == null
                ? "В свойствах компонента сохранятся концентрация и источник. Количество подготовки останется прежним; остаток и факт загрузки не меняются."
                : "Объём заполнит план подготовки; концентрация и источник сохранятся в опыте. Остаток и факт загрузки не меняются."));
  }

  private record Loaded(
      LaboratoryInventoryRepository.Opened opened, Map<String, Set<String>> matches) {}

  private void load(final Properties restore) {
    if (this.saving || this.disposed) {
      return;
    }
    final long version = ++this.generation;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.deferred = restore;
    this.showReview(false);
    this.busy(true);
    this.controls(true);
    this.status.setText("Читаю запасы и сопоставляю структуры…");
    final var experiment = this.editor.document().experiment();
    final String stageId = this.stageId();
    final boolean existed =
        this.ledgerExisted
            || restore != null && Boolean.parseBoolean(restore.getProperty("preparation.existed"));
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var opened = this.repository.read(this.ledger);
                if (existed && opened.fingerprint() == null) {
                  throw new IllegalStateException(
                      "Прежний файл запасов отсутствует. Восстановите materials.qchem; пустой учёт не будет создан.");
                }
                final Map<String, Set<String>> matches = new HashMap<>();
                if (!stageId.isEmpty()) {
                  for (final var n : LaboratoryStockPreparation.needs(experiment, stageId)) {
                    final Set<String> ids = new HashSet<>();
                    for (final var b : opened.inventory().batches()) {
                      if (Thread.currentThread().isInterrupted()) {
                        return;
                      }
                      if (LaboratoryStockPreparation.matches(opened.inventory(), b, n)) {
                        ids.add(b.id());
                      }
                    }
                    matches.put(n.component().id(), Set.copyOf(ids));
                  }
                }
                final var loaded = new Loaded(opened, matches);
                Platform.runLater(
                    () -> {
                      if (this.disposed || version != this.generation) {
                        return;
                      }
                      try {
                        this.inventory = loaded.opened().inventory();
                        this.stock = this.inventory.stock();
                        this.fingerprint = loaded.opened().fingerprint();
                        this.ledgerExisted |= this.fingerprint != null;
                        this.matches = loaded.matches();
                        this.busy(false);
                        this.controls(false);
                        this.restoring = true;
                        this.render();
                        this.restoring = false;
                        if (restore != null) {
                          this.restore(restore);
                        }
                        this.deferred = null;
                        this.syncJournal();
                        this.summary();
                        if (this.command != null && this.command.committed(this.inventory)) {
                          this.completed(
                              "Предыдущая операция уже записана. Повторное списание не выполнено.");
                        } else if (this.command != null) {
                          this.uncertain = false;
                          this.preview();
                        } else {
                          this.uncertain = false;
                          this.status.setText(
                              this.sourceChanged
                                  ? "План изменился. Проверьте восстановленный ввод; откройте подготовку заново для нового плана."
                                  : "Запасы загружены. Резервирование не уменьшает физический остаток.");
                        }
                      } catch (final RuntimeException e) {
                        this.restoring = false;
                        this.status.setText(message(e));
                        this.controls(true);
                        this.reload.setDisable(false);
                      }
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && version == this.generation) {
                        this.busy(false);
                        this.controls(true);
                        this.reload.setDisable(false);
                        this.status.setText(message(e));
                      }
                    });
              }
            });
  }

  private void controls(final boolean disabled) {
    this.tabs.setDisable(disabled || this.command != null);
    this.stage.setDisable(disabled || this.command != null);
    this.reason.setDisable(disabled || this.command != null);
    this.reload.setDisable(this.saving);
    this.reserve.setDisable(disabled || this.stage.getValue() == null);
    this.post.setDisable(disabled);
    this.cancel.setDisable(this.saving);
  }

  private void render() {
    this.needs.clear();
    this.cards.getChildren().clear();
    if (this.stage.getValue() != null) {
      LaboratoryStockPreparation.needs(this.editor.document().experiment(), this.stageId())
          .forEach(n -> this.add(n, null));
    }
    if (this.needs.isEmpty()) {
      this.cards
          .getChildren()
          .add(
              note(
                  "В выбранной стадии нет исходников или вспомогательных веществ. Добавьте участников в схему реакции."));
    }
    this.renderHeld();
    this.summary();
    this.history
        .getItems()
        .setAll(
            this.inventory.reservations().reversed().stream()
                .map(
                    e ->
                        e.recordedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(
                                    java.time.format.DateTimeFormatter.ofPattern(
                                        "dd.MM.yyyy HH:mm"))
                            + " · "
                            + kind(e.kind())
                            + " · "
                            + this.quantity(e.quantity())
                            + "\n"
                            + e.target().description()
                            + "\n"
                            + this.lot(this.batch(e.batchId()))
                            + " · "
                            + e.reason())
                .toList());
  }

  private static String kind(final LaboratoryReservationEvent.Kind kind) {
    return switch (kind) {
      case RESERVE -> "Зарезервировано";
      case RELEASE -> "Снято с резерва";
      case CONSUME -> "Фактический расход";
    };
  }

  private void add(final LaboratoryStockPreparation.Need need, final String key) {
    final var row = new NeedRow(need, key == null ? UUID.randomUUID().toString() : key);
    this.needs.add(row);
    this.cards.getChildren().add(row.view);
  }

  private void summary() {
    final long active =
        this.inventory.reservationStates().stream()
            .filter(LaboratoryReservationBook.State::active)
            .count();
    this.summary.setText(
        "Партий: " + this.inventory.batches().size() + " · открытых резервов: " + active);
  }

  private final class NeedRow {
    final LaboratoryStockPreparation.Need need;
    final String key;
    final VBox view = new VBox(8);
    final CheckBox select = new CheckBox("В резерв"),
        reviewed = new CheckBox("Состав партии и количество проверены");
    final ComboBox<LaboratoryBatch> lot = new ComboBox<>();
    final TextField amount = new TextField();
    final Label info = note(""), availability = note(""), unit = note("—");
    final FlowPane solutionActions = new FlowPane(8, 6);

    NeedRow(final LaboratoryStockPreparation.Need need, final String key) {
      this.need = need;
      this.key = key;
      this.view.getStyleClass().add("laboratory-reaction-card");
      this.view.setPadding(new Insets(12));
      this.view
          .prefWidthProperty()
          .bind(
              javafx.beans.binding.Bindings.createDoubleBinding(
                  () -> {
                    final double width = Math.max(300, cards.getWidth() - 16);
                    final int columns = Math.max(1, (int) ((width + 12) / 432));
                    return Math.floor((width - (columns - 1) * 12) / columns) - 1;
                  },
                  cards.widthProperty()));
      this.view.maxWidthProperty().bind(this.view.prefWidthProperty());
      this.view.setMinWidth(0);
      this.view.setMinHeight(Region.USE_PREF_SIZE);
      final Label title = note(need.component().molecule().displayName());
      title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
      this.lot
          .getItems()
          .setAll(
              inventory.batches().stream()
                  .sorted(
                      Comparator.comparing(
                              (LaboratoryBatch b) ->
                                  !matches
                                      .getOrDefault(need.component().id(), Set.of())
                                      .contains(b.id()))
                          .thenComparing(LaboratoryBatch::label))
                  .toList());
      this.lot.setMaxWidth(Double.MAX_VALUE);
      this.lot.setPromptText("Выберите партию");
      this.lot.setId("preparation-lot-" + key);
      this.lot.setConverter(
          new javafx.util.StringConverter<>() {
            public String toString(LaboratoryBatch b) {
              return b == null ? "" : batchTitle(b);
            }

            public LaboratoryBatch fromString(String s) {
              throw new UnsupportedOperationException();
            }
          });
      this.lot.setCellFactory(v -> batchCell(false));
      this.lot.setButtonCell(batchCell(true));
      this.amount.setPromptText("Количество");
      this.amount.setPrefColumnCount(12);
      this.amount.setId("preparation-amount-" + key);
      this.select.setId("preparation-select-" + key);
      this.reviewed.setId("preparation-reviewed-" + key);
      this.reviewed.setWrapText(true);
      final Button plan =
          button(
              "Из плана",
              "plan-" + key,
              () -> {
                final var b = this.lot.getValue();
                if (b == null) {
                  throw new IllegalArgumentException("Сначала выберите партию.");
                }
                if (!matches.getOrDefault(need.component().id(), Set.of()).contains(b.id())) {
                  throw new IllegalArgumentException(
                      "Соответствие структур не подтверждено. Введите количество после ручной проверки материала.");
                }
                final var q = LaboratoryStockPreparation.suggested(need, b);
                if (q == null) {
                  throw new IllegalArgumentException(
                      "План не задан или размерность отличается. Введите известное количество вручную.");
                }
                this.amount.setText(q.value().toPlainString());
                this.select.setSelected(true);
              });
      this.lot
          .valueProperty()
          .addListener(
              o -> {
                this.amount.clear();
                this.reviewed.setSelected(false);
                this.update();
                touch();
              });
      this.amount
          .textProperty()
          .addListener(
              o -> {
                this.reviewed.setSelected(false);
                this.update();
                touch();
              });
      this.select
          .selectedProperty()
          .addListener(
              o -> {
                this.update();
                touch();
              });
      this.reviewed.selectedProperty().addListener(o -> touch());
      this.solutionActions
          .getChildren()
          .setAll(
              button(
                  "Рассчитать объём по концентрации…",
                  "concentration-" + key,
                  () -> LaboratoryPreparationPage.this.concentration(this)));
      final FlowPane otherActions =
          new FlowPane(
              8,
              6,
              button(
                  "Измерения / раствор…",
                  "standards-" + key,
                  () -> {
                    if (this.lot.getValue() == null)
                      throw new IllegalArgumentException("Выберите партию.");
                    if (openSolutions == null)
                      throw new IllegalArgumentException(
                          "Откройте раздел «Растворы и разведения» для записи концентрации.");
                    final String selected = this.lot.getValue().id();
                    afterDiscard = () -> openSolutions.accept(selected);
                    if (LaboratoryPreparationPage.this.canClose()) {
                      afterDiscard = null;
                      openSolutions.accept(selected);
                    } else {
                      status.setText(
                          "Для перехода к измерениям партии завершите подготовку либо явно отбросьте текущий ввод. После подтверждения откроются растворы выбранной партии.");
                    }
                  }),
              button(
                  "Убрать строку",
                  "remove-" + key,
                  () -> {
                    if (needs.stream()
                            .filter(r -> r.need.component().id().equals(need.component().id()))
                            .count()
                        <= 1) {
                      throw new IllegalArgumentException(
                          "Последняя строка вещества остаётся в списке; снимите её выбор.");
                    }
                    needs.remove(this);
                    cards.getChildren().remove(this.view);
                    touch();
                  }),
              button(
                  "Ещё партия",
                  "split-" + key,
                  () -> {
                    if (needs.size() >= 100) {
                      throw new IllegalArgumentException("Не более 100 строк за один раз.");
                    }
                    add(need, null);
                    touch();
                  }));
      this.view
          .getChildren()
          .setAll(
              title,
              note(
                  LaboratoryInspector.roleText(need.participant().role())
                      + " · план "
                      + quantity(need.component().planned())),
              picture(need, this.view),
              this.lot,
              this.availability,
              new FlowPane(8, 6, this.amount, this.unit, plan),
              this.solutionActions,
              this.reviewed,
              this.select,
              disclosure(
                  "Сведения о партии и другие действия",
                  "lot-details-" + key,
                  new VBox(8, this.info, otherActions)));
      final Set<String> ids = matches.getOrDefault(need.component().id(), Set.of());
      if (ids.size() == 1) {
        this.lot.setValue(
            this.lot.getItems().stream()
                .filter(b -> ids.contains(b.id()))
                .findFirst()
                .orElse(null));
      }
      this.update();
    }

    void update() {
      final var b = this.lot.getValue();
      this.unit.setText(b == null ? "—" : b.unit().toString());
      this.amount.setDisable(b == null);
      this.reviewed.setDisable(b == null);
      this.select.setDisable(b == null);
      visible(this.solutionActions, b != null && b.unit().dimension().equals("volume"));
      if (b == null) {
        this.availability.setText("Выберите исходную партию");
        this.info.setText(
            "Если подходит несколько партий, выберите нужную по её составу и происхождению.");
        return;
      }
      final String match =
          matches.getOrDefault(this.need.component().id(), Set.of()).contains(b.id())
              ? "Структура совпадает"
              : "Структура не подтверждена — проверьте материал вручную";
      String after = "";
      try {
        final BigDecimal value = new BigDecimal(this.amount.getText().trim().replace(',', '.'));
        final BigDecimal rest = stock.get(b.id()).available().value().subtract(value);
        after =
            "\nПосле этой строки: "
                + (rest.signum() < 0 ? "не хватает " : "свободно ")
                + quantity(new LaboratoryQuantity(rest.abs(), b.unit()));
      } catch (final RuntimeException ignored) {
      }
      this.availability.setText(
          (matches.getOrDefault(this.need.component().id(), Set.of()).contains(b.id())
                  ? "Доступно "
                  : "Проверьте соответствие структуры · доступно ")
              + quantity(stock.get(b.id()).available())
              + after);
      this.info.setText(
          match
              + "\nID партии: "
              + b.id()
              + "\nИсточник: "
              + (b.source().isBlank() ? "не указан" : b.source())
              + "\nФизически "
              + quantity(stock.get(b.id()).physical())
              + " · резерв "
              + quantity(stock.get(b.id()).reserved())
              + " · свободно "
              + quantity(stock.get(b.id()).available())
              + "\nЕдиница: "
              + b.unit()
              + " · чистота: "
              + (b.purityPercent() == null ? "не задана" : b.purityPercent() + " %")
              + " · запись о концентрации при создании партии: "
              + (b.concentration() == null || b.concentration().isBlank()
                  ? "не задана"
                  : b.concentration())
              + after);
    }
  }

  private Canvas picture(final LaboratoryStockPreparation.Need need, final VBox card) {
    final Canvas canvas = new Canvas(380, 140);
    canvas.widthProperty().bind(card.widthProperty().subtract(24));
    final var drawing = this.editor.document().layouts().get(need.component().id());
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final Runnable paint =
        () -> {
          final double w = Math.max(1, canvas.getWidth());
          final double scale =
              Math.min(
                  1.7,
                  Math.min(
                      (w - 40) / Math.max(1, bounds.getWidth()),
                      112 / Math.max(1, bounds.getHeight())));
          final var g = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          g.save();
          g.translate(
              w / 2 - (bounds.getMinX() + bounds.getWidth() / 2) * scale,
              70 - (bounds.getMinY() + bounds.getHeight() / 2) * scale);
          g.scale(scale, scale);
          LaboratoryPreviewTheme.paint(g, drawing);
          g.restore();
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener(o -> paint.run());
    paint.run();
    canvas.setAccessibleText(need.component().molecule().displayName());
    canvas.setCursor(javafx.scene.Cursor.HAND);
    Tooltip.install(canvas, new Tooltip("Нажмите, чтобы рассмотреть структуру крупно"));
    canvas.setOnMouseClicked(e -> this.enlarge(need));
    return canvas;
  }

  private void enlarge(final LaboratoryStockPreparation.Need need) {
    final var drawing = this.editor.document().layouts().get(need.component().id());
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final Canvas picture = new Canvas();
    final ScrollPane viewport = new ScrollPane(new StackPane(picture));
    viewport.setPannable(true);
    final Slider zoom = new Slider(0.5, 5, 1);
    zoom.setPrefWidth(180);
    final Runnable paint =
        () -> {
          final double w = Math.max(240, viewport.getViewportBounds().getWidth()),
              h = Math.max(180, viewport.getViewportBounds().getHeight());
          final double scale =
              Math.min(
                      3,
                      Math.min(
                          (w - 50) / Math.max(1, bounds.getWidth()),
                          (h - 50) / Math.max(1, bounds.getHeight())))
                  * zoom.getValue();
          picture.setWidth(Math.max(w, bounds.getWidth() * scale + 50));
          picture.setHeight(Math.max(h, bounds.getHeight() * scale + 50));
          final var g = picture.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          g.save();
          g.translate(
              picture.getWidth() / 2 - (bounds.getMinX() + bounds.getWidth() / 2) * scale,
              picture.getHeight() / 2 - (bounds.getMinY() + bounds.getHeight() / 2) * scale);
          g.scale(scale, scale);
          LaboratoryPreviewTheme.paint(g, drawing);
          g.restore();
        };
    LaboratoryPreviewTheme.track(picture, paint);
    zoom.valueProperty()
        .addListener(
            o -> {
              paint.run();
              Platform.runLater(
                  () -> {
                    viewport.applyCss();
                    for (int i = 0; i < 3; i++) {
                      viewport.layout();
                    }
                    viewport.setHvalue(0.5);
                    viewport.setVvalue(0.5);
                  });
            });
    viewport.viewportBoundsProperty().addListener(o -> paint.run());
    picture.setOnScroll(
        e -> {
          if (e.isControlDown()) {
            zoom.setValue(zoom.getValue() * (e.getDeltaY() > 0 ? 1.15 : 1 / 1.15));
            e.consume();
          }
        });
    final BorderPane content = new BorderPane(viewport);
    content.setTop(
        new FlowPane(
            8,
            6,
            note(need.component().molecule().displayName()),
            zoom,
            this.button("Вписать", "fit", () -> zoom.setValue(1)),
            note("Перетаскивание мышью · Ctrl + колесо")));
    if (this.tabs.getTabs().size() > 3) {
      this.tabs.getTabs().remove(3);
    }
    final Tab tab = new Tab("Структура крупно", content);
    this.tabs.getTabs().add(tab);
    this.tabs.getSelectionModel().select(tab);
    Platform.runLater(paint);
  }

  private void fill() {
    final Set<String> seen = new HashSet<>();
    int filled = 0;
    for (final var row : this.needs) {
      final var b = row.lot.getValue();
      if (!seen.add(row.need.component().id())
          || b == null
          || !row.amount.getText().isBlank()
          || !this.matches.getOrDefault(row.need.component().id(), Set.of()).contains(b.id())) {
        continue;
      }
      final var q = LaboratoryStockPreparation.suggested(row.need, b);
      if (q != null && q.value().signum() > 0) {
        row.amount.setText(q.value().toPlainString());
        row.select.setSelected(true);
        filled++;
      }
    }
    this.status.setText(
        "Заполнено строк: "
            + filled
            + ". Проверьте состав и количество каждой партии, затем просмотрите весь резерв.");
  }

  private final class ReservedRow {
    final LaboratoryReservationBook.State state;
    final CheckBox select = new CheckBox("Выбрать"),
        releaseRest = new CheckBox("Снять оставшийся резерв после расхода");
    final TextField amount = new TextField();
    final VBox view = new VBox(6);

    ReservedRow(final LaboratoryReservationBook.State state) {
      this.state = state;
      final var e = state.opened();
      final var b = batch(e.batchId());
      this.amount.setPromptText("Количество, " + b.unit());
      this.amount.setPrefColumnCount(12);
      this.select.setId("preparation-held-" + e.reservationId());
      this.amount.setId("preparation-use-" + e.reservationId());
      this.releaseRest.setWrapText(true);
      final boolean currentExperiment = e.target().experimentId().equals(experimentId);
      final String changed =
          currentExperiment
                  && !LaboratoryStockPreparation.current(editor.document().experiment(), e.target())
              ? " · план изменён"
              : "";
      final Label heading = note(e.target().description() + changed);
      heading.setStyle("-fx-font-weight: bold;");
      this.view
          .getChildren()
          .setAll(
              heading,
              note(
                  lot(b)
                      + "\nВ резерве "
                      + quantity(state.remaining())
                      + " · использовано "
                      + quantity(state.consumed())
                      + " · снято "
                      + quantity(state.released())),
              new FlowPane(10, 6, this.select, this.amount, this.releaseRest));
      this.view.getStyleClass().add("laboratory-reaction-card");
      this.view.setPadding(new Insets(12));
      this.select.selectedProperty().addListener(o -> touch());
      this.amount.textProperty().addListener(o -> touch());
      this.releaseRest.selectedProperty().addListener(o -> touch());
    }
  }

  private void renderHeld() {
    this.held.clear();
    this.reserveRows.getChildren().clear();
    this.inventory.reservationStates().stream()
        .filter(LaboratoryReservationBook.State::active)
        .filter(
            s ->
                this.allProject.isSelected()
                    || s.opened().target().experimentId().equals(this.experimentId)
                        && s.opened().target().stageId().equals(this.stageId()))
        .forEach(
            s -> {
              final var row = new ReservedRow(s);
              this.held.add(row);
              this.reserveRows.getChildren().add(row.view);
            });
    if (this.held.isEmpty()) {
      this.reserveRows
          .getChildren()
          .add(
              note(
                  "Открытых резервов по выбранному фильтру нет. Подберите партии на первой вкладке."));
    }
  }

  private LaboratoryQuantity entered(final String text, final LaboratoryQuantityUnit unit) {
    try {
      final var q = new LaboratoryQuantity(new BigDecimal(text.trim().replace(',', '.')), unit);
      if (q.value().signum() <= 0) {
        throw new IllegalArgumentException();
      }
      return q;
    } catch (final RuntimeException e) {
      throw new IllegalArgumentException(
          "Введите положительное количество в единице партии: " + unit + ".");
    }
  }

  private void reserve() {
    if (this.running || this.command != null) {
      return;
    }
    if (this.sourceChanged) {
      throw new IllegalArgumentException(
          "План изменён. Сохраните нужный ввод и откройте подготовку заново.");
    }
    final List<LaboratoryStockPreparation.Allocation> rows = new ArrayList<>();
    for (final var row : this.needs) {
      if (row.select.isSelected()) {
        final var b = row.lot.getValue();
        if (b == null) {
          throw new IllegalArgumentException(
              "Выберите партию для «" + row.need.component().molecule().displayName() + "».");
        }
        if (!LaboratoryStockPreparation.current(
            this.editor.document().experiment(), row.need.target())) {
          throw new IllegalArgumentException("Исходный план изменился.");
        }
        rows.add(
            new LaboratoryStockPreparation.Allocation(
                row.need,
                b.id(),
                this.entered(row.amount.getText(), b.unit()),
                row.reviewed.isSelected()));
      }
    }
    this.command =
        LaboratoryStockPreparation.reserve(
            this.inventory,
            rows,
            "prepare_" + UUID.randomUUID(),
            Instant.now(),
            this.reason.getText());
    this.preview();
  }

  private void change(final boolean consume) {
    if (this.running || this.command != null) {
      return;
    }
    final List<LaboratoryStockPreparation.Change> rows = new ArrayList<>();
    for (final var row : this.held) {
      if (row.select.isSelected()) {
        if (consume && !row.state.opened().target().experimentId().equals(this.experimentId)) {
          throw new IllegalArgumentException(
              "Для фактического расхода откройте опыт, которому принадлежит резерв. Здесь можно снять чужой резерв с указанием основания.");
        }
        rows.add(
            new LaboratoryStockPreparation.Change(
                row.state.opened().reservationId(),
                this.entered(row.amount.getText(), row.state.remaining().unit()),
                row.releaseRest.isSelected()));
      }
    }
    this.command =
        LaboratoryStockPreparation.change(
            this.inventory,
            rows,
            consume,
            "prepare_" + UUID.randomUUID(),
            Instant.now(),
            this.reason.getText());
    this.preview();
  }

  private void preview() {
    this.showReview(true);
    this.review.getChildren().clear();
    this.cancel.setDisable(false);
    this.post.setText("Провести изменения");
    this.post.setOnAction(
        e -> {
          try {
            this.post();
          } catch (final RuntimeException x) {
            this.status.setText(message(x));
          }
        });
    try {
      final var next = this.command.apply(this.inventory);
      final VBox lines =
          new VBox(
              6,
              note("Проверьте изменения перед записью"),
              note("Основание: " + this.reason.getText()));
      for (final var e : this.command.events()) {
        lines
            .getChildren()
            .add(
                note(
                    kind(e.kind())
                        + " · "
                        + this.quantity(e.quantity())
                        + " · "
                        + this.lot(this.batch(e.batchId()))
                        + "\n"
                        + e.target().description()));
      }
      final Set<String> affected = new LinkedHashSet<>();
      this.command.events().forEach(e -> affected.add(e.batchId()));
      for (final String id : affected) {
        final var b = this.batch(id);
        lines
            .getChildren()
            .add(
                note(
                    b.label()
                        + " после всей операции: физически "
                        + this.quantity(next.balance(b))
                        + " · резерв "
                        + this.quantity(next.reserved(b))
                        + " · свободно "
                        + this.quantity(next.available(b))));
      }
      final ScrollPane scroll = scroll(lines);
      scroll.setFitToHeight(false);
      scroll.setPrefViewportHeight(180);
      scroll.setMaxHeight(230);
      this.review.getChildren().setAll(scroll, new FlowPane(8, 6, this.post, this.cancel));
      this.post.setDisable(false);
      this.cancel.setDisable(false);
      this.status.setText("Изменения ещё не проведены. Запись сохранит весь пакет сразу.");
    } catch (final RuntimeException e) {
      this.review.getChildren().setAll(note("Учёт изменился: " + message(e)), this.cancel);
      this.post.setDisable(true);
      this.status.setText("Вернитесь к выбору и пересмотрите количества по обновлённым запасам.");
    }
  }

  private void showReview(final boolean value) {
    visible(this.review, value);
    visible(this.tabs, !value);
    if (this.reasonPane != null) {
      visible(this.reasonPane, !value);
    }
    this.tabs.setDisable(value);
    this.stage.setDisable(value);
    this.reason.setDisable(value);
  }

  private void post() {
    if (this.running || this.command == null || this.uncertain) {
      return;
    }
    for (final var e : this.command.events()) {
      if (e.kind() == LaboratoryReservationEvent.Kind.RESERVE
          && !LaboratoryStockPreparation.current(this.editor.document().experiment(), e.target())) {
        throw new IllegalArgumentException(
            "План изменился после просмотра. Откройте подготовку заново.");
      }
    }
    final var submitted = this.command;
    final var next = submitted.apply(this.inventory);
    final String expected = this.fingerprint;
    this.saving = true;
    this.busy(true);
    this.controls(true);
    this.status.setText("Сохраняю весь пакет в учёт проекта…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final String fingerprint = this.repository.write(this.ledger, next, expected);
                Platform.runLater(
                    () -> {
                      this.saving = false;
                      if (this.disposed) {
                        return;
                      }
                      this.busy(false);
                      this.inventory = next;
                      this.stock = this.inventory.stock();
                      this.fingerprint = fingerprint;
                      this.ledgerExisted = true;
                      this.completed(
                          "Операция записана. Фактическую загрузку можно перенести в опыт отдельным действием.");
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      this.saving = false;
                      if (this.disposed) {
                        return;
                      }
                      this.busy(false);
                      this.uncertain = true;
                      this.controls(true);
                      this.reload.setDisable(false);
                      this.status.setText(
                          "Запись не подтверждена: "
                              + message(e)
                              + " Обновите запасы — приложение проверит, была ли операция проведена.");
                    });
              }
            });
  }

  private void completed(final String text) {
    this.command = null;
    this.uncertain = false;
    this.dirty = false;
    this.deferred = null;
    this.syncJournal();
    this.restoring = true;
    this.render();
    this.restoring = false;
    this.dirty = false;
    this.showReview(false);
    this.controls(false);
    this.tabs.getSelectionModel().select(1);
    this.status.setText(text);
  }

  private void syncJournal() {
    if (!this.editor.document().experiment().id().equals(this.experimentId)) {
      throw new IllegalArgumentException("Открыт другой опыт.");
    }
    for (final var e : this.inventory.reservations()) {
      final String marker = "; резерв-событие " + e.id() + ";";
      if (e.target().experimentId().equals(this.experimentId)
          && this.editor.document().experiment().journal().stream()
              .noneMatch(j -> j.text().contains(marker))) {
        this.editor.record(
            LaboratoryEntry.Kind.ACTION,
            kind(e.kind())
                + ": "
                + this.quantity(e.quantity())
                + " · "
                + e.target().description()
                + " · "
                + this.lot(this.batch(e.batchId()))
                + " · "
                + e.reason()
                + marker
                + (e.useOperationId() == null ? "" : " операция " + e.useOperationId()),
            null);
      }
    }
  }

  private void concentration(final NeedRow row) {
    if (this.running || this.command != null || this.sourceChanged) return;
    final var batch = row.lot.getValue();
    if (batch == null) throw new IllegalArgumentException("Выберите физическую партию раствора.");
    final var expected = this.editor.document();
    final var component =
        expected.experiment().components().stream()
            .filter(c -> c.id().equals(row.need.component().id()))
            .findFirst()
            .orElseThrow();
    final String expectedStock = this.fingerprint;
    final var book = LaboratoryWorkbenchBooks.solutions(this.ledger.getParent());
    this.busy(true);
    this.controls(true);
    this.review.setDisable(true);
    this.reload.setDisable(true);
    this.status.setText("Читаю измеренную концентрацию выбранной партии…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final var fresh = this.repository.read(this.ledger);
                if (!Objects.equals(expectedStock, fresh.fingerprint()))
                  throw new IllegalArgumentException(
                      "Учёт материалов изменился. Обновите партии перед расчётом.");
                final var standards = book.read();
                final var prepared =
                    LaboratorySolutionUse.prepare(
                        fresh.inventory(), standards.value(), batch.id(), component);
                LaboratoryQuantity volume = null;
                String volumeWarning = null;
                try {
                  volume = LaboratorySolutionUse.plannedVolume(component, prepared);
                } catch (IllegalArgumentException missing) {
                  volumeWarning = missing.getMessage();
                }
                final var calculated = volume;
                final var warning = volumeWarning;
                Platform.runLater(
                    () -> {
                      if (this.disposed) return;
                      this.busy(false);
                      this.controls(false);
                      this.review.setDisable(false);
                      this.reload.setDisable(false);
                      if (!this.editor.document().equals(expected)) {
                        this.status.setText("Опыт изменился. Запросите концентрацию заново.");
                        return;
                      }
                      final var lines =
                          this.concentrationCard(
                              fresh.inventory(), component, prepared, calculated, warning);
                      final var apply =
                          this.button(
                              "Применить концентрацию" + (calculated == null ? "" : " и объём"),
                              "apply-concentration",
                              () -> {
                                this.busy(true);
                                this.review.setDisable(true);
                                this.reload.setDisable(true);
                                this.task =
                                    this.worker.submit(
                                        () -> {
                                          try {
                                            if (!Objects.equals(
                                                    fresh.fingerprint(),
                                                    this.repository.read(this.ledger).fingerprint())
                                                || !Objects.equals(
                                                    standards.fingerprint(),
                                                    book.read().fingerprint()))
                                              throw new IllegalArgumentException(
                                                  "Партии или измерения концентрации изменились. Повторите расчёт перед применением.");
                                            Platform.runLater(
                                                () -> {
                                                  if (this.disposed) return;
                                                  this.busy(false);
                                                  this.review.setDisable(false);
                                                  this.reload.setDisable(false);
                                                  try {
                                                    if (!this.editor.document().equals(expected))
                                                      throw new IllegalArgumentException(
                                                          "Опыт изменился. Концентрация не применена.");
                                                    final Properties keep = this.capture();
                                                    this.editor.quantify(
                                                        component.id(),
                                                        component.planned(),
                                                        component.actual(),
                                                        component.evidence(),
                                                        prepared.properties());
                                                    final var updated =
                                                        LaboratoryStockPreparation.needs(
                                                            this.editor.document().experiment(),
                                                            this.stageId());
                                                    final int count =
                                                        Integer.parseInt(
                                                            keep.getProperty(
                                                                "preparation.rows", "0"));
                                                    for (int i = 0; i < count; i++) {
                                                      final String key =
                                                          "preparation.row." + i + ".";
                                                      if (keep.getProperty(key + "component", "")
                                                          .equals(component.id())) {
                                                        keep.setProperty(
                                                            key + "signature",
                                                            updated.stream()
                                                                .filter(
                                                                    n ->
                                                                        n.component()
                                                                            .id()
                                                                            .equals(component.id()))
                                                                .findFirst()
                                                                .orElseThrow()
                                                                .target()
                                                                .signature());
                                                        keep.setProperty(key + "reviewed", "false");
                                                      }
                                                      if (keep.getProperty(key + "key", "")
                                                              .equals(row.key)
                                                          && calculated != null) {
                                                        keep.setProperty(
                                                            key + "amount",
                                                            calculated
                                                                .value()
                                                                .stripTrailingZeros()
                                                                .toPlainString());
                                                        keep.setProperty(key + "selected", "true");
                                                      }
                                                    }
                                                    keep.setProperty("preparation.dirty", "true");
                                                    this.restore(keep);
                                                    this.showReview(false);
                                                    this.status.setText(
                                                        this.files.save(false)
                                                            ? "Концентрация и источник сохранены. Проверьте объём и состав партии перед резервированием."
                                                            : "Концентрация применена в открытом опыте; файл ещё не сохранён. Проверьте объём перед резервированием.");
                                                  } catch (RuntimeException error) {
                                                    this.showReview(false);
                                                    this.status.setText(message(error));
                                                  }
                                                });
                                          } catch (Exception error) {
                                            Platform.runLater(
                                                () -> {
                                                  if (!this.disposed) {
                                                    this.busy(false);
                                                    this.review.setDisable(false);
                                                    this.reload.setDisable(false);
                                                    this.showReview(false);
                                                    this.status.setText(message(error));
                                                  }
                                                });
                                          }
                                        });
                              });
                      apply.getStyleClass().add("laboratory-save");
                      final var back =
                          this.button(
                              "Вернуться к партиям",
                              "cancel-concentration",
                              () -> this.showReview(false));
                      final var scroll = scroll(lines);
                      scroll.setFitToHeight(false);
                      scroll.setPrefViewportHeight(440);
                      scroll.setMaxHeight(Double.MAX_VALUE);
                      VBox.setVgrow(scroll, Priority.ALWAYS);
                      VBox.setVgrow(this.review, Priority.ALWAYS);
                      this.review.setMinHeight(0);
                      this.review.getChildren().setAll(scroll, new FlowPane(8, 6, apply, back));
                      this.showReview(true);
                      this.status.setText(
                          "Проверьте источник концентрации и рассчитанный объём перед применением.");
                    });
              } catch (Exception error) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed) {
                        this.busy(false);
                        this.review.setDisable(false);
                        this.controls(false);
                        this.reload.setDisable(false);
                        this.status.setText(message(error));
                      }
                    });
              }
            });
  }

  private void transfer() {
    if (this.running || this.command != null) {
      return;
    }
    final var amounts =
        LaboratoryStockPreparation.actuals(
            this.inventory, this.editor.document().experiment(), this.stageId());
    final LaboratoryDrawing expected = this.editor.document();
    final VBox lines =
        new VBox(
            6,
            note(
                "Подтвердите, что этот расход действительно загружен в реакцию. Значения заменят прежние введённые количества; резерв и склад не изменятся."));
    amounts.forEach(
        (id, q) -> {
          final var c =
              expected.experiment().components().stream()
                  .filter(v -> v.id().equals(id))
                  .findFirst()
                  .orElseThrow();
          lines
              .getChildren()
              .add(
                  note(
                      c.molecule().displayName()
                          + ": "
                          + c.evidence().quantityLabel()
                          + " "
                          + this.quantity(c.actual())
                          + " → наблюдённая загрузка "
                          + this.quantity(q)));
        });
    final Button apply =
        this.button(
            "Подтвердить фактическую загрузку",
            "apply-actual",
            () -> {
              final String expectedStock = this.fingerprint;
              this.busy(true);
              this.review.setDisable(true);
              this.reload.setDisable(true);
              this.task =
                  this.worker.submit(
                      () -> {
                        try {
                          final var fresh = this.repository.read(this.ledger);
                          if (!Objects.equals(fresh.fingerprint(), expectedStock)) {
                            throw new IllegalArgumentException(
                                "Запасы изменены другим окном. Обновите их и просмотрите перенос заново.");
                          }
                          Platform.runLater(
                              () -> {
                                if (this.disposed) {
                                  return;
                                }
                                this.busy(false);
                                this.review.setDisable(false);
                                this.reload.setDisable(false);
                                try {
                                  this.editor.actualBatch(expected, amounts);
                                  this.showReview(false);
                                  this.status.setText(
                                      this.files.save(false)
                                          ? "Фактическая загрузка сохранена в опыте. Расход повторно не проводится."
                                          : "Факт перенесён в открытый опыт; сохранение не завершено. Учёт партий уже записан отдельно.");
                                } catch (final RuntimeException e) {
                                  this.status.setText(message(e));
                                }
                              });
                        } catch (final Exception e) {
                          Platform.runLater(
                              () -> {
                                if (!this.disposed) {
                                  this.busy(false);
                                  this.review.setDisable(false);
                                  this.reload.setDisable(false);
                                  this.showReview(false);
                                  this.status.setText(message(e));
                                }
                              });
                        }
                      });
            });
    final Button back = this.button("Вернуться", "cancel-actual", () -> this.showReview(false));
    final ScrollPane scroll = scroll(lines);
    scroll.setFitToHeight(false);
    scroll.setPrefViewportHeight(170);
    scroll.setMaxHeight(230);
    this.review.getChildren().setAll(scroll, new FlowPane(8, 6, apply, back));
    this.showReview(true);
  }

  private void copy() {
    final StringBuilder text =
        new StringBuilder("Вещество\tПлан\tПартия\tКоличество\tЕдиница\tСвободно\n");
    for (final var r : this.needs) {
      final var b = r.lot.getValue();
      text.append(r.need.component().molecule().displayName().replace('\t', ' ').replace('\n', ' '))
          .append('\t')
          .append(this.quantity(r.need.component().planned()))
          .append('\t')
          .append(b == null ? "" : this.lot(b).replace('\t', ' ').replace('\n', ' '))
          .append('\t')
          .append(r.amount.getText())
          .append('\t')
          .append(b == null ? "" : b.unit())
          .append('\t')
          .append(b == null ? "" : this.quantity(this.stock.get(b.id()).available()))
          .append('\n');
    }
    final ClipboardContent content = new ClipboardContent();
    content.putString(text.toString());
    Clipboard.getSystemClipboard().setContent(content);
    this.status.setText("Список подготовки скопирован; запасы не изменены.");
  }

  boolean pending() {
    return this.dirty || this.command != null || this.deferred != null;
  }

  private Properties capture() {
    final Properties p = new Properties();
    this.draft(p, false);
    return p;
  }

  void draft(final Properties p, final boolean restore) {
    if (restore) {
      if (!this.ledger.toString().equals(p.getProperty("preparation.ledger"))
          || !this.experimentId.equals(p.getProperty("preparation.experiment"))) {
        throw new IllegalArgumentException("Черновик относится к другому опыту или учёту.");
      }
      this.restoring = true;
      this.stage.setValue(
          this.stage.getItems().stream()
              .filter(s -> s.id().equals(p.getProperty("preparation.stage")))
              .findFirst()
              .orElse(null));
      this.restoring = false;
      this.load(p);
      return;
    }
    if (this.deferred != null) {
      p.putAll(this.deferred);
      return;
    }
    if (this.sourceChanged && this.conflictDraft != null) {
      p.putAll(this.conflictDraft);
      p.setProperty("preparation.conflict", "true");
      return;
    }
    p.setProperty("preparation.ledger", this.ledger.toString());
    p.setProperty("preparation.experiment", this.experimentId);
    p.setProperty("preparation.stage", this.stageId());
    p.setProperty("preparation.existed", "" + this.ledgerExisted);
    p.setProperty("preparation.reason", this.reason.getText());
    p.setProperty("preparation.dirty", "" + this.dirty);
    p.setProperty("preparation.all", "" + this.allProject.isSelected());
    p.setProperty("preparation.tab", "" + this.tabs.getSelectionModel().getSelectedIndex());
    p.setProperty("preparation.rows", "" + this.needs.size());
    int i = 0;
    for (final var row : this.needs) {
      final String k = "preparation.row." + i++ + ".";
      p.setProperty(k + "key", row.key);
      p.setProperty(k + "component", row.need.component().id());
      p.setProperty(k + "signature", row.need.target().signature());
      p.setProperty(k + "batch", row.lot.getValue() == null ? "" : row.lot.getValue().id());
      p.setProperty(k + "amount", row.amount.getText());
      p.setProperty(k + "selected", "" + row.select.isSelected());
      p.setProperty(k + "reviewed", "" + row.reviewed.isSelected());
    }
    for (final var row : this.held) {
      final String k = "preparation.held." + row.state.opened().reservationId() + ".";
      p.setProperty(k + "selected", "" + row.select.isSelected());
      p.setProperty(k + "amount", row.amount.getText());
      p.setProperty(k + "rest", "" + row.releaseRest.isSelected());
    }
    LaboratoryPreparationDraft.write(p, this.command);
  }

  private void restore(final Properties p) {
    this.restoring = true;
    try {
      this.reason.setText(p.getProperty("preparation.reason", "Подготовка реагентов к стадии"));
      this.allProject.setSelected(Boolean.parseBoolean(p.getProperty("preparation.all")));
      this.renderHeld();
      final int count = Integer.parseInt(p.getProperty("preparation.rows", "0"));
      if (count > 100 || count < 0) {
        throw new IllegalArgumentException("Повреждён список подготовки.");
      }
      final var available =
          this.stage.getValue() == null
              ? List.<LaboratoryStockPreparation.Need>of()
              : LaboratoryStockPreparation.needs(
                  this.editor.document().experiment(), this.stageId());
      this.needs.clear();
      this.cards.getChildren().clear();
      this.sourceChanged = Boolean.parseBoolean(p.getProperty("preparation.conflict"));
      this.conflictDraft = new Properties();
      this.conflictDraft.putAll(p);
      for (int i = 0; i < count; i++) {
        final String k = "preparation.row." + i + ".";
        final var n =
            available.stream()
                .filter(v -> v.component().id().equals(p.getProperty(k + "component")))
                .findFirst()
                .orElse(null);
        if (n == null) {
          this.sourceChanged = true;
          this.cards
              .getChildren()
              .add(
                  note(
                      "Компонент удалён из стадии; сохранённое количество: "
                          + p.getProperty(k + "amount")
                          + " · партия "
                          + p.getProperty(k + "batch")));
          continue;
        }
        this.add(n, p.getProperty(k + "key"));
        final var row = this.needs.getLast();
        row.lot.setValue(
            row.lot.getItems().stream()
                .filter(b -> b.id().equals(p.getProperty(k + "batch")))
                .findFirst()
                .orElse(null));
        row.amount.setText(p.getProperty(k + "amount", ""));
        row.select.setSelected(Boolean.parseBoolean(p.getProperty(k + "selected")));
        row.reviewed.setSelected(Boolean.parseBoolean(p.getProperty(k + "reviewed")));
        this.sourceChanged |= !n.target().signature().equals(p.getProperty(k + "signature"));
      }
      for (final var row : this.held) {
        final String k = "preparation.held." + row.state.opened().reservationId() + ".";
        row.select.setSelected(Boolean.parseBoolean(p.getProperty(k + "selected")));
        row.amount.setText(p.getProperty(k + "amount", ""));
        row.releaseRest.setSelected(Boolean.parseBoolean(p.getProperty(k + "rest")));
      }
      this.command = LaboratoryPreparationDraft.read(p);
      this.dirty = Boolean.parseBoolean(p.getProperty("preparation.dirty"));
      this.tabs.getSelectionModel().select(Integer.parseInt(p.getProperty("preparation.tab", "0")));
    } finally {
      this.restoring = false;
    }
  }

  @Override
  boolean canClose() {
    if (this.running && this.review.isDisabled()) {
      this.status.setText("Дождитесь проверки переноса факта.");
      return false;
    }
    if (this.saving) {
      this.status.setText("Дождитесь завершения записи учёта.");
      return false;
    }
    if (this.uncertain) {
      this.status.setText("Обновите запасы, чтобы проверить результат записи.");
      return false;
    }
    if (this.pending()) {
      visible(this.discard, true);
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    this.generation++;
    if (!this.saving && this.task != null) {
      this.task.cancel(true);
    }
    if (this.saving) {
      this.worker.shutdown();
    } else {
      this.worker.shutdownNow();
    }
    super.dispose();
  }
}
