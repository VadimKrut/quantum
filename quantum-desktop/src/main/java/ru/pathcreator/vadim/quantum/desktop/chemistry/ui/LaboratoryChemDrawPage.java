/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.ChemDrawXmlExchange;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** A whole ChemDraw document is inspected and imported as one edit. */
final class LaboratoryChemDrawPage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final LaboratoryDrawing source;
  private final Window owner;
  private final boolean exporting;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final var t = new Thread(r, "chemdraw-exchange");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean applied, writing, restoring, conflict;
  private byte[] original = new byte[0];
  private String filename = "Вставленный документ.cdxml", output = "";
  private ChemDrawExchange.Document document;
  private final Map<String, ReactionComponentRole> roleChoices = new LinkedHashMap<>(),
      stageRoles = new LinkedHashMap<>();
  private final Map<String, ReactionComponentRole> selected = new LinkedHashMap<>();
  private final Set<String> selectedStages = new LinkedHashSet<>();
  private final Map<String, String> names = new LinkedHashMap<>();
  private Properties restored = new Properties();
  private final TextArea input = new TextArea();
  private final TextField search = new TextField();
  private final Label count = note("Откройте CDXML или вставьте текст из ChemDraw.");
  private final TabPane tabs = new TabPane();
  private final VBox compounds = new VBox(12),
      schemes = new VBox(12),
      details = new VBox(8),
      discard = new VBox(8);
  private final Tab large = new Tab("Структура крупно");
  private final Button parse, apply, save, copy, stop;
  private final FlowPane actions;

  LaboratoryChemDrawPage(
      final LaboratoryEditor editor,
      final boolean exporting,
      final Window owner,
      final Runnable close) {
    super(exporting ? "Экспорт для ChemDraw" : "Документ ChemDraw", close);
    this.editor = editor;
    this.source = editor.document();
    this.exporting = exporting;
    this.owner = owner;
    this.setId("chemistry-chemdraw-page");
    this.input.setId("chemdraw-text");
    this.input.setPromptText(
        "В ChemDraw: Edit → Copy As → CDXML.\nВставьте документ сюда или откройте файл .cdxml.");
    this.input.setEditable(!exporting);
    this.parse =
        button(exporting ? "Подготовить CDXML" : "Прочитать документ", "parse", this::parse);
    this.parse.getStyleClass().add("primary-button");
    this.apply = button("Добавить выбранное в опыт", "apply", this::commit);
    this.apply.getStyleClass().add("primary-button");
    this.apply.setDisable(true);
    this.save = button("Сохранить новую копию…", "save", this::save);
    this.save.setDisable(true);
    this.copy =
        button(
            "Копировать CDXML",
            "copy",
            () -> {
              if (!this.output.isEmpty()) {
                final var c = new ClipboardContent();
                c.putString(this.output);
                Clipboard.getSystemClipboard().setContent(c);
                this.status.setText(
                    "CDXML скопирован. В принимающей программе используйте импорт текста CDXML.");
              }
            });
    this.copy.setDisable(true);
    this.stop = button("Остановить", "stop", this::cancel);
    this.stop.setDisable(true);
    this.actions = new FlowPane(8, 8);
    if (!exporting) {
      this.actions
          .getChildren()
          .addAll(
              button("Открыть CDXML…", "open", this::open),
              button(
                  "Вставить из буфера",
                  "paste",
                  () -> {
                    final String text = Clipboard.getSystemClipboard().getString();
                    if (text != null) {
                      this.filename = "Буфер обмена.cdxml";
                      this.input.setText(text);
                      this.parse();
                    }
                  }));
    }
    this.actions.getChildren().addAll(this.parse);
    this.search.setPromptText("Найти вещество или стадию по названию");
    this.search.setId("chemdraw-search");
    this.search.textProperty().addListener(o -> this.render());
    for (final VBox box : List.of(this.compounds, this.schemes, this.details)) {
      box.setPadding(new Insets(12));
      box.setMinHeight(Region.USE_PREF_SIZE);
      box.setMinWidth(0);
    }
    final FlowPane selection =
        new FlowPane(
            8,
            6,
            button("Все вещества", "all-compounds", () -> this.selectAll(false)),
            button("Все стадии", "all-stages", () -> this.selectAll(true)),
            button(
                "Снять выбор",
                "none",
                () -> {
                  this.selected.clear();
                  this.selectedStages.clear();
                  this.selectionChanged();
                  this.render();
                }));
    this.large.setContent(note("Нажмите на рисунок вещества для увеличения."));
    this.tabs
        .getTabs()
        .setAll(
            new Tab("Вещества", scroll(this.compounds)),
            new Tab("Стадии", scroll(this.schemes)),
            new Tab("Что переносится", scroll(this.details)),
            new Tab(exporting ? "Готовый CDXML" : "Исходный CDXML", this.input),
            this.large);
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setMinHeight(180);
    this.tabs.setId("chemdraw-tabs");
    this.tabs
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener(
            (o, a, b) -> {
              if (b.intValue() < 2) {
                this.render();
              }
            });
    VBox.setVgrow(this.tabs, Priority.ALWAYS);
    final VBox body = new VBox(10, this.actions, this.search, selection, this.count, this.tabs);
    body.setPadding(new Insets(0, 16, 8, 16));
    body.setMinSize(0, 0);
    this.setCenter(body);
    this.discard
        .getChildren()
        .setAll(
            note("Документ ещё не добавлен в опыт. Исходник и выбор сохранены в авточерновике."),
            new FlowPane(
                8,
                6,
                button(
                    "Отбросить ввод и выйти",
                    "discard",
                    () -> {
                      this.cancel();
                      this.applied = true;
                      this.finish();
                    }),
                button("Продолжить", "continue", () -> this.showDiscard(false))));
    this.showDiscard(false);
    final VBox footer =
        new VBox(
            8,
            this.discard,
            new FlowPane(10, 6, this.status, this.stop),
            new FlowPane(
                8,
                6,
                exporting
                    ? new javafx.scene.Node[] {this.save, this.copy}
                    : new javafx.scene.Node[] {this.apply}));
    footer.setPadding(new Insets(8, 16, 14, 16));
    this.setBottom(footer);
    this.input
        .textProperty()
        .addListener(
            o -> {
              if (!this.exporting && !this.restoring) {
                this.cancel();
                this.original = this.input.getText().getBytes(StandardCharsets.UTF_8);
                this.document = null;
                this.restored.clear();
                this.selected.clear();
                this.selectedStages.clear();
                this.names.clear();
                this.roleChoices.clear();
                this.stageRoles.clear();
                this.render();
                this.selectionChanged();
                this.status.setText("Текст изменён. Прочитайте документ для просмотра.");
              }
            });
    this.input.setOnDragOver(
        e -> {
          if (!this.exporting && (e.getDragboard().hasFiles() || e.getDragboard().hasString())) {
            e.acceptTransferModes(TransferMode.COPY);
          }
          e.consume();
        });
    this.input.setOnDragDropped(
        e -> {
          boolean done = false;
          if (!this.exporting && !this.running) {
            if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().size() == 1) {
              this.load(e.getDragboard().getFiles().getFirst().toPath());
              done = true;
            } else if (e.getDragboard().hasString()) {
              this.filename = "Перенесённый текст.cdxml";
              this.input.setText(e.getDragboard().getString());
              this.parse();
              done = true;
            }
          }
          e.setDropCompleted(done);
          e.consume();
        });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            if (!this.exporting && !this.apply.isDisabled()) {
              this.commit();
            } else {
              this.parse();
            }
            e.consume();
          }
        });
    if (exporting) {
      this.source.experiment().components().forEach(c -> this.selected.put(c.id(), c.role()));
      this.source.experiment().stages().forEach(s -> this.selectedStages.add(s.id()));
      this.render();
      this.selectionChanged();
      this.status.setText(
          "Выберите вещества и стадии. Подготовка покажет точное содержимое и ограничения файла.");
    } else {
      this.tabs.getSelectionModel().select(3);
      this.status.setText(
          "Весь документ → просмотр → одно добавление. Исходный CDXML сохранится вложением; Ctrl+Z отменит добавление.");
    }
  }

  private Button button(final String text, final String id, final Runnable action) {
    final Button b = new Button(text);
    b.setId("chemdraw-" + id);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException x) {
            this.status.setText(x.getMessage());
          }
        });
    return b;
  }

  private void showDiscard(final boolean value) {
    this.discard.setVisible(value);
    this.discard.setManaged(value);
  }

  private void selectionChanged() {
    this.output = "";
    this.save.setDisable(true);
    this.copy.setDisable(true);
    if (this.exporting) {
      this.input.clear();
      this.details
          .getChildren()
          .setAll(note("Подготовьте файл, чтобы увидеть замечания именно к текущему выбору."));
    }
    final Set<String> unique = new HashSet<>(this.selected.keySet());
    if (this.document != null) {
      this.document.steps().stream()
          .filter(s -> this.selectedStages.contains(s.id()))
          .forEach(s -> s.members().forEach(m -> unique.add(m.fragmentId())));
    }
    this.count.setText(
        "Выбрано веществ: "
            + unique.size()
            + " · стадий: "
            + this.selectedStages.size()
            + (this.exporting ? "" : " · участники выбранных стадий добавляются автоматически"));
    this.apply.setDisable(
        this.running || this.conflict || this.document == null || unique.isEmpty());
  }

  private void selectAll(final boolean stages) {
    this.selected.clear();
    this.selectedStages.clear();
    if (this.exporting) {
      if (stages) {
        this.source.experiment().stages().forEach(s -> this.selectedStages.add(s.id()));
      } else {
        this.source.experiment().components().forEach(c -> this.selected.put(c.id(), c.role()));
      }
    } else if (this.document != null) {
      if (stages) {
        this.document.steps().stream()
            .filter(ChemDrawExchange.Step::supported)
            .forEach(s -> this.selectedStages.add(s.id()));
      } else {
        this.document.fragments().stream()
            .filter(ChemDrawExchange.Fragment::supported)
            .forEach(
                f ->
                    this.selected.put(
                        f.id(),
                        this.roleChoices.getOrDefault(f.id(), ReactionComponentRole.REACTANT)));
      }
    }
    this.selectionChanged();
    this.render();
  }

  private void cancel() {
    if (this.writing) {
      return;
    }
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.busy(false);
    this.getCenter().setDisable(false);
    if (this.parse != null) {
      this.parse.setDisable(false);
      this.stop.setDisable(true);
    }
  }

  private void submit(
      final Callable<?> action,
      final java.util.function.Consumer<Object> done,
      final boolean writing) {
    if (this.running || this.disposed) {
      return;
    }
    this.cancel();
    final long generation = this.generation;
    this.writing = writing;
    this.stop.setDisable(writing);
    this.busy(true);
    this.getCenter().setDisable(true);
    this.apply.setDisable(true);
    this.save.setDisable(true);
    this.copy.setDisable(true);
    this.status.setText(writing ? "Сохраняю подготовленные данные…" : "Читаю весь документ…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final Object value = action.call();
                Platform.runLater(
                    () -> {
                      if (generation == this.generation && !this.disposed) {
                        this.writing = false;
                        this.stop.setDisable(true);
                        this.busy(false);
                        this.getCenter().setDisable(false);
                        try {
                          done.accept(value);
                        } catch (final RuntimeException e) {
                          this.status.setText(e.getMessage());
                        }
                        this.refreshButtons();
                      }
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (generation == this.generation && !this.disposed) {
                        this.writing = false;
                        this.stop.setDisable(true);
                        this.busy(false);
                        this.getCenter().setDisable(false);
                        this.status.setText(
                            e.getMessage() == null
                                ? "Не удалось прочитать документ."
                                : e.getMessage());
                        this.refreshButtons();
                      }
                    });
              }
            });
  }

  private void refreshButtons() {
    this.save.setDisable(this.running || this.output.isEmpty());
    this.copy.setDisable(this.running || this.output.isEmpty());
    this.apply.setDisable(
        this.running
            || this.conflict
            || this.document == null
            || this.selected.isEmpty() && this.selectedStages.isEmpty());
  }

  private void parse() {
    if (this.running) {
      return;
    }
    if (this.exporting) {
      final var components = new LinkedHashSet<>(this.selected.keySet());
      final var stages = new LinkedHashSet<>(this.selectedStages);
      this.output = "";
      this.submit(
          () -> new ChemDrawXmlExchange().write(this.source, components, stages),
          result -> {
            final var export = (ChemDrawExchange.Export) result;
            this.output = export.text();
            this.input.setText(this.output);
            this.details
                .getChildren()
                .setAll(export.notes().stream().map(LaboratoryComputationPage::note).toList());
            this.tabs.getSelectionModel().select(2);
            this.status.setText(
                "CDXML подготовлен. Просмотрите замечания и сохраните новую копию.");
          },
          false);
      return;
    }
    final byte[] bytes = this.original.clone();
    final String name = this.filename;
    this.submit(
        () -> new ChemDrawXmlExchange().read(bytes, name),
        result -> this.loaded((ChemDrawExchange.Document) result),
        false);
  }

  private void loaded(final ChemDrawExchange.Document document) {
    this.document = document;
    this.roleChoices.clear();
    this.stageRoles.clear();
    this.selected.clear();
    this.selectedStages.clear();
    this.names.clear();
    if (!this.restored.isEmpty()) {
      this.restoreChoices();
    } else {
      document.steps().stream()
          .filter(ChemDrawExchange.Step::supported)
          .forEach(s -> this.selectedStages.add(s.id()));
      if (this.selectedStages.isEmpty()) {
        document.fragments().stream()
            .filter(ChemDrawExchange.Fragment::supported)
            .forEach(
                f ->
                    this.selected.put(
                        f.id(),
                        this.roleChoices.getOrDefault(f.id(), ReactionComponentRole.REACTANT)));
      }
    }
    this.selectionChanged();
    this.render();
    this.tabs.getSelectionModel().select(this.document.steps().isEmpty() ? 0 : 1);
    this.status.setText(
        "Документ прочитан: "
            + document.fragments().size()
            + " веществ, "
            + document.steps().size()
            + " стадий. Проверьте выбор; непрочитанные объекты отмечены.");
  }

  private boolean visible(final String name) {
    return this.search.getText().isBlank()
        || name.toLowerCase(Locale.ROOT)
            .contains(this.search.getText().strip().toLowerCase(Locale.ROOT));
  }

  private void render() {
    this.compounds.getChildren().clear();
    this.schemes.getChildren().clear();
    if (this.document == null && !this.exporting) {
      this.compounds.getChildren().add(note("Здесь появятся структуры после чтения документа."));
      this.schemes.getChildren().add(note("Стадии читаются из явных данных CDXML."));
      return;
    }
    final FlowPane grid = new FlowPane(12, 12);
    grid.setMinWidth(0);
    grid.setPrefWrapLength(850);
    this.compounds.getChildren().add(grid);
    if (this.exporting) {
      for (final var c : this.source.experiment().components()) {
        if (this.visible(c.molecule().displayName())) {
          grid.getChildren()
              .add(
                  this.card(
                      c.id(),
                      c.molecule().displayName(),
                      this.source.layouts().get(c.id()),
                      "",
                      true));
        }
      }
      for (final var s : this.source.experiment().stages()) {
        if (this.visible(s.title())) {
          final CheckBox pick = new CheckBox(s.title());
          pick.setSelected(this.selectedStages.contains(s.id()));
          pick.setOnAction(
              e -> {
                if (pick.isSelected()) {
                  this.selectedStages.add(s.id());
                } else {
                  this.selectedStages.remove(s.id());
                }
                this.selectionChanged();
              });
          this.schemes
              .getChildren()
              .add(
                  new VBox(
                      8, pick, note("Участников: " + s.participants().size() + " · " + s.arrow())));
        }
      }
    } else {
      for (final var f : this.document.fragments()) {
        if (this.visible(this.names.getOrDefault(f.id(), f.title()))) {
          grid.getChildren()
              .add(
                  this.card(
                      f.id(),
                      f.title(),
                      f.supported() ? f.entry().drawing() : null,
                      f.problem(),
                      true));
        }
      }
      final Map<String, ChemDrawExchange.Fragment> byId = new LinkedHashMap<>();
      this.document.fragments().forEach(f -> byId.put(f.id(), f));
      for (final var s : this.document.steps()) {
        if (!this.visible(this.names.getOrDefault("stage:" + s.id(), s.title()))) {
          continue;
        }
        final CheckBox pick = new CheckBox("Добавить стадию");
        pick.setSelected(this.selectedStages.contains(s.id()));
        pick.setDisable(!s.supported());
        pick.setOnAction(
            e -> {
              if (pick.isSelected()) {
                this.selectedStages.add(s.id());
              } else {
                this.selectedStages.remove(s.id());
              }
              this.selectionChanged();
            });
        final TextField title =
            new TextField(this.names.getOrDefault("stage:" + s.id(), s.title()));
        title.setPromptText("Название стадии");
        title.textProperty().addListener((o, a, b) -> this.names.put("stage:" + s.id(), b));
        final VBox box = new VBox(10, new HBox(12, pick, title));
        HBox.setHgrow(title, Priority.ALWAYS);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-border-color: #c8d5db; -fx-border-radius: 9;");
        if (!s.supported()) {
          box.getChildren().add(note("Не перенесена: " + s.problem()));
        } else {
          final FlowPane row = new FlowPane(10, 10);
          for (final var m : s.members()) {
            final var f = byId.get(m.fragmentId());
            final VBox card = this.card(f.id(), f.title(), f.entry().drawing(), "", false);
            final String key = s.id() + ":" + f.id();
            final ComboBox<ReactionComponentRole> role =
                this.roleSelector(this.stageRoles.getOrDefault(key, m.role()));
            role.setOnAction(e -> this.stageRoles.put(key, role.getValue()));
            card.getChildren().add(0, role);
            row.getChildren().add(card);
          }
          box.getChildren()
              .addAll(
                  note(
                      "Роли участников читаются из схемы. Уточните растворитель, катализатор и другие вспомогательные вещества."),
                  row);
          if (!s.note().isBlank()) {
            box.getChildren().add(note("Условия из исходника: " + s.note()));
          }
          final long maps = s.members().stream().mapToLong(m -> m.atomNumbers().size()).sum();
          if (maps > 0) {
            box.getChildren()
                .add(
                    note(
                        "Атомная карта: "
                            + maps / 2
                            + " пар. Просмотр номеров доступен в реакции после добавления."));
          }
        }
        this.schemes.getChildren().add(box);
      }
      this.details
          .getChildren()
          .setAll(this.document.notes().stream().map(LaboratoryComputationPage::note).toList());
      this.details
          .getChildren()
          .add(
              note(
                  "Не поддержаны: сокращения без вложенного графа или с несколькими точками связи, R-/S-группы, полимеры, запросы, радикалы и относительная стереохимия. Исходные подписи не используются как автоматически вычисленные IUPAC-названия. Двоичный CDX предварительно сохраните как CDXML."));
    }
    if (this.schemes.getChildren().isEmpty()) {
      this.schemes
          .getChildren()
          .add(note("Подходящих стадий нет. Можно добавить вещества и собрать стадию в опыте."));
    }
  }

  private VBox card(
      final String id,
      final String title,
      final MoleculeDrawing drawing,
      final String error,
      final boolean selectable) {
    final VBox box = new VBox(8);
    box.setPrefWidth(300);
    box.setMinWidth(240);
    box.setMaxWidth(340);
    box.setPadding(new Insets(12));
    box.setStyle("-fx-border-color: #c8d5db; -fx-border-radius: 9;");
    if (selectable) {
      final CheckBox pick = new CheckBox(this.exporting ? "Включить в файл" : "Добавить вещество");
      pick.setSelected(this.selected.containsKey(id));
      pick.setDisable(drawing == null);
      pick.setOnAction(
          e -> {
            if (pick.isSelected()) {
              this.selected.put(
                  id,
                  this.roleChoices.getOrDefault(
                      id,
                      this.exporting
                          ? this.source.experiment().components().stream()
                              .filter(c -> c.id().equals(id))
                              .findFirst()
                              .orElseThrow()
                              .role()
                          : ReactionComponentRole.REACTANT));
            } else {
              this.selected.remove(id);
            }
            this.selectionChanged();
          });
      box.getChildren().add(pick);
    }
    if (!this.exporting && drawing != null) {
      final TextField name = new TextField(this.names.getOrDefault(id, title));
      name.setPromptText("Название вещества");
      name.textProperty().addListener((o, a, b) -> this.names.put(id, b));
      box.getChildren().add(name);
      if (selectable) {
        final ComboBox<ReactionComponentRole> role =
            this.roleSelector(
                this.roleChoices.getOrDefault(
                    id, this.selected.getOrDefault(id, ReactionComponentRole.REACTANT)));
        role.setOnAction(
            e -> {
              this.roleChoices.put(id, role.getValue());
              if (this.selected.containsKey(id)) {
                this.selected.put(id, role.getValue());
              }
            });
        box.getChildren().add(role);
      }
    } else {
      final Label name = note(this.names.getOrDefault(id, title));
      name.setStyle("-fx-font-weight: bold;");
      box.getChildren().add(name);
    }
    if (drawing == null) {
      box.getChildren().add(note("Не прочитано: " + error));
    } else {
      final Canvas picture = new Canvas(276, 175);
      picture.setOnMouseClicked(e -> this.large(drawing));
      picture.setCursor(javafx.scene.Cursor.HAND);
      box.getChildren()
          .addAll(
              picture,
              note(
                  drawing.molecule().formula().hillNotation()
                      + " · нажмите рисунок для увеличения"));
      paint(picture, drawing, 1);
    }
    return box;
  }

  private ComboBox<ReactionComponentRole> roleSelector(final ReactionComponentRole value) {
    final ComboBox<ReactionComponentRole> role = new ComboBox<>();
    role.getItems().setAll(ReactionComponentRole.values());
    role.setValue(value);
    role.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(ReactionComponentRole r) {
            return roleName(r);
          }

          public ReactionComponentRole fromString(String s) {
            throw new UnsupportedOperationException();
          }
        });
    return role;
  }

  private ChemDrawExchange.Document reviewed() {
    final List<ChemDrawExchange.Step> steps = new ArrayList<>();
    for (final var s : this.document.steps()) {
      final var members =
          s.members().stream()
              .map(
                  m ->
                      new ChemDrawExchange.Member(
                          m.fragmentId(),
                          this.stageRoles.getOrDefault(s.id() + ":" + m.fragmentId(), m.role()),
                          m.atomNumbers()))
              .toList();
      steps.add(
          new ChemDrawExchange.Step(s.id(), s.title(), members, s.arrow(), s.note(), s.problem()));
    }
    return new ChemDrawExchange.Document(this.document.fragments(), steps, this.document.notes());
  }

  private static String roleName(final ReactionComponentRole r) {
    return switch (r) {
      case REACTANT -> "Исходник";
      case PRODUCT -> "Продукт";
      case SOLVENT -> "Растворитель";
      case CATALYST -> "Катализатор";
      case ADDITIVE -> "Добавка";
      case ACID -> "Кислота";
      case BASE -> "Основание";
      case LIGAND -> "Лиганд";
      case ATMOSPHERE -> "Атмосфера";
      case WORKUP -> "Обработка";
      case OTHER -> "Другое";
    };
  }

  private static void paint(final Canvas canvas, final MoleculeDrawing drawing, final double zoom) {
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final var g = canvas.getGraphicsContext2D();
    g.setFill(javafx.scene.paint.Color.WHITE);
    g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    final double scale =
        Math.min(
                3,
                Math.min(
                    (canvas.getWidth() - 40) / Math.max(1, bounds.getWidth()),
                    (canvas.getHeight() - 40) / Math.max(1, bounds.getHeight())))
            * zoom;
    g.save();
    g.translate(
        canvas.getWidth() / 2 - (bounds.getMinX() + bounds.getWidth() / 2) * scale,
        canvas.getHeight() / 2 - (bounds.getMinY() + bounds.getHeight() / 2) * scale);
    g.scale(scale, scale);
    LaboratoryCanvas.paintPreview(g, drawing);
    g.restore();
  }

  private void large(final MoleculeDrawing drawing) {
    final Slider zoom = new Slider(.4, 4, 1);
    final Canvas canvas = new Canvas(800, 600);
    final StackPane holder = new StackPane(canvas);
    final ScrollPane viewport = new ScrollPane(holder);
    viewport.setPannable(true);
    viewport.setMinSize(0, 0);
    final Runnable redraw =
        () -> {
          final double w = Math.max(300, viewport.getViewportBounds().getWidth()),
              h = Math.max(220, viewport.getViewportBounds().getHeight());
          canvas.setWidth(w * Math.max(1, zoom.getValue()));
          canvas.setHeight(h * Math.max(1, zoom.getValue()));
          final var bounds = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              Math.min(
                      3,
                      Math.min(
                          (w - 40) / Math.max(1, bounds.getWidth()),
                          (h - 40) / Math.max(1, bounds.getHeight())))
                  * zoom.getValue();
          final var g = canvas.getGraphicsContext2D();
          g.setFill(javafx.scene.paint.Color.WHITE);
          g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
          g.save();
          g.translate(
              canvas.getWidth() / 2 - (bounds.getMinX() + bounds.getWidth() / 2) * scale,
              canvas.getHeight() / 2 - (bounds.getMinY() + bounds.getHeight() / 2) * scale);
          g.scale(scale, scale);
          LaboratoryCanvas.paintPreview(g, drawing);
          g.restore();
        };
    viewport.viewportBoundsProperty().addListener(o -> redraw.run());
    zoom.valueProperty()
        .addListener(
            o -> {
              redraw.run();
              Platform.runLater(
                  () -> {
                    viewport.applyCss();
                    for (int i = 0; i < 3; i++) {
                      viewport.layout();
                    }
                    viewport.setHvalue(.5);
                    viewport.setVvalue(.5);
                  });
            });
    canvas.setOnScroll(
        e -> {
          if (e.isControlDown()) {
            zoom.setValue(zoom.getValue() * (e.getDeltaY() > 0 ? 1.15 : 1 / 1.15));
            e.consume();
          }
        });
    final BorderPane body = new BorderPane(viewport);
    body.setTop(
        new FlowPane(
            10,
            8,
            note(drawing.molecule().displayName()),
            note("Масштаб"),
            zoom,
            button(
                "Вписать",
                "fit",
                () -> {
                  zoom.setValue(1);
                  redraw.run();
                }),
            note("Перетаскивание · Ctrl + колесо")));
    this.large.setContent(body);
    this.tabs.getSelectionModel().select(this.large);
    Platform.runLater(redraw);
  }

  private void commit() {
    if (this.exporting || this.running || this.document == null || this.apply.isDisabled()) {
      return;
    }
    if (this.conflict || !this.source.equals(this.editor.document())) {
      this.conflict = true;
      this.refreshButtons();
      this.status.setText("Опыт изменился. Сохраните исходный текст и откройте импорт заново.");
      return;
    }
    final var document = this.reviewed();
    final var chosen = new LinkedHashMap<>(this.selected);
    final var stages = new LinkedHashSet<>(this.selectedStages);
    final var names = new LinkedHashMap<>(this.names);
    final byte[] bytes = this.original.clone();
    final String filename = this.filename;
    this.submit(
        () -> {
          ChemDrawExchange.importDocument(this.source, document, chosen, stages, names, null);
          return new LaboratoryAssetStore().stage(bytes, filename);
        },
        value -> {
          this.editor.importChemDraw(
              this.source, document, chosen, stages, names, (LaboratoryAttachment) value);
          this.applied = true;
          this.finish();
        },
        true);
  }

  private void open() {
    final FileChooser c = new FileChooser();
    c.setTitle("Открыть документ ChemDraw");
    c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Документ CDXML", "*.cdxml"));
    final var file = c.showOpenDialog(this.owner);
    if (file != null) {
      this.load(file.toPath());
    }
  }

  void load(final Path path) {
    this.submit(
        () -> {
          try (final var stream = Files.newInputStream(path)) {
            final byte[] bytes = stream.readNBytes(ChemDrawXmlExchange.MAX_BYTES + 1);
            if (bytes.length > ChemDrawXmlExchange.MAX_BYTES) {
              throw new IllegalArgumentException("Файл больше 8 МБ.");
            }
            final String text = sourceText(bytes);
            return new Object[] {
              bytes, text, new ChemDrawXmlExchange().read(bytes, path.getFileName().toString())
            };
          }
        },
        value -> {
          final var result = (Object[]) value;
          this.restoring = true;
          try {
            this.input.setText((String) result[1]);
          } finally {
            this.restoring = false;
          }
          this.original = (byte[]) result[0];
          this.filename = path.getFileName().toString();
          this.restored.clear();
          this.loaded((ChemDrawExchange.Document) result[2]);
        },
        false);
  }

  private static String sourceText(final byte[] bytes) {
    Charset charset = StandardCharsets.UTF_8;
    if (bytes.length >= 2
        && ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 254
            || (bytes[0] & 255) == 254 && (bytes[1] & 255) == 255)) {
      charset = StandardCharsets.UTF_16;
    } else {
      final String head =
          new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.US_ASCII);
      final var match =
          java.util.regex.Pattern.compile("encoding=[\"']([^\"']+)[\"']").matcher(head);
      if (match.find()) {
        charset = Charset.forName(match.group(1));
      }
    }
    try {
      return charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .decode(java.nio.ByteBuffer.wrap(bytes))
          .toString()
          .replaceFirst("encoding=[\"'][^\"']+[\"']", "encoding=\"UTF-8\"");
    } catch (final CharacterCodingException e) {
      throw new IllegalArgumentException(
          "Не удалось прочитать кодировку CDXML. Сохраните копию в UTF-8.", e);
    }
  }

  private void save() {
    if (this.output.isEmpty()) {
      return;
    }
    final FileChooser c = new FileChooser();
    c.setTitle("Сохранить новую копию CDXML");
    c.setInitialFileName("scheme.cdxml");
    c.getExtensionFilters().add(new FileChooser.ExtensionFilter("CDXML", "*.cdxml"));
    final var file = c.showSaveDialog(this.owner);
    if (file == null) {
      return;
    }
    final String text = this.output;
    this.submit(
        () -> {
          final Path path = file.toPath().toAbsolutePath().normalize();
          if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cdxml")) {
            throw new IllegalArgumentException("Выберите расширение .cdxml.");
          }
          final Path temp = Files.createTempFile(path.getParent(), ".cdxml-", ".tmp");
          try {
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            Files.move(temp, path);
          } finally {
            Files.deleteIfExists(temp);
          }
          return path.getFileName();
        },
        name -> this.status.setText("Сохранён «" + name + "» · UTF-8."),
        true);
  }

  boolean pending() {
    return !this.exporting && !this.applied && this.original.length > 0;
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText("Дождитесь завершения текущей операции.");
      return false;
    }
    if (this.pending()) {
      this.showDiscard(true);
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    this.cancel();
    this.worker.shutdownNow();
    super.dispose();
  }

  void draft(final Properties fields, final boolean restore) {
    final String p = "chemdraw.";
    if (restore) {
      this.restoring = true;
      try {
        if (fields.getProperty(p + "bytes", "").length() > 10_666_668) {
          throw new IllegalArgumentException("Черновик CDXML превышает 8 МБ.");
        }
        this.original = Base64.getDecoder().decode(fields.getProperty(p + "bytes", ""));
        this.input.setText(sourceText(this.original));
        this.filename = fields.getProperty(p + "name", "Восстановленный документ.cdxml");
        this.conflict =
            Boolean.parseBoolean(fields.getProperty(p + "conflict", "false"))
                || !this.fingerprint().equals(fields.getProperty(p + "fingerprint"));
        this.restored = new Properties();
        fields.forEach(
            (k, v) -> {
              if (k.toString().startsWith(p)) {
                this.restored.put(k, v);
              }
            });
        this.status.setText(
            this.conflict
                ? "Ввод восстановлен; опыт изменился, добавление отключено."
                : "Ввод восстановлен. Прочитайте документ и проверьте выбор.");
      } finally {
        this.restoring = false;
      }
      return;
    }
    fields.putAll(this.restored);
    fields.setProperty(p + "bytes", Base64.getEncoder().encodeToString(this.original));
    fields.setProperty(p + "name", this.filename);
    fields.setProperty(p + "fingerprint", this.fingerprint());
    fields.setProperty(
        p + "conflict",
        Boolean.toString(this.conflict || !this.source.equals(this.editor.document())));
    if (this.document != null) {
      fields
          .keySet()
          .removeIf(
              k ->
                  k.toString().startsWith(p + "pick.")
                      || k.toString().startsWith(p + "stage.")
                      || k.toString().startsWith(p + "caption."));
      this.selected.forEach((id, role) -> fields.setProperty(p + "pick." + id, role.name()));
      this.selectedStages.forEach(id -> fields.setProperty(p + "stage." + id, "true"));
      this.names.forEach((id, name) -> fields.setProperty(p + "caption." + id, name));
      this.roleChoices.forEach((id, role) -> fields.setProperty(p + "role." + id, role.name()));
      this.stageRoles.forEach((id, role) -> fields.setProperty(p + "stageRole." + id, role.name()));
      fields.setProperty(p + "reviewed", "true");
    }
  }

  private void restoreChoices() {
    this.restored.forEach(
        (k, v) -> {
          if (k.toString().startsWith("chemdraw.role.")) {
            this.roleChoices.put(
                k.toString().substring("chemdraw.role.".length()),
                ReactionComponentRole.valueOf(v.toString()));
          }
          if (k.toString().startsWith("chemdraw.stageRole.")) {
            this.stageRoles.put(
                k.toString().substring("chemdraw.stageRole.".length()),
                ReactionComponentRole.valueOf(v.toString()));
          }
        });
    for (final var f : this.document.fragments()) {
      final String value = this.restored.getProperty("chemdraw.pick." + f.id());
      if (value != null && f.supported()) {
        this.selected.put(f.id(), ReactionComponentRole.valueOf(value));
      }
    }
    for (final var s : this.document.steps()) {
      if (s.supported()
          && Boolean.parseBoolean(this.restored.getProperty("chemdraw.stage." + s.id()))) {
        this.selectedStages.add(s.id());
      }
    }
    this.restored.forEach(
        (k, v) -> {
          if (k.toString().startsWith("chemdraw.caption.")) {
            this.names.put(k.toString().substring("chemdraw.caption.".length()), v.toString());
          }
        });
    if (!Boolean.parseBoolean(this.restored.getProperty("chemdraw.reviewed"))) {
      this.document.steps().stream()
          .filter(ChemDrawExchange.Step::supported)
          .forEach(step -> this.selectedStages.add(step.id()));
      if (this.selectedStages.isEmpty()) {
        this.document.fragments().stream()
            .filter(ChemDrawExchange.Fragment::supported)
            .forEach(
                f ->
                    this.selected.put(
                        f.id(),
                        this.roleChoices.getOrDefault(f.id(), ReactionComponentRole.REACTANT)));
      }
    }
  }

  private String fingerprint() {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(
                      new LaboratoryFileCodec()
                          .write(this.source)
                          .getBytes(StandardCharsets.UTF_8)));
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
