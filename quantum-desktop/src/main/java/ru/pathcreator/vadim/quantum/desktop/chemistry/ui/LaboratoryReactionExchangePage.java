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
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkReactionExchange;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** One complete reaction, reviewed before the single document mutation. */
final class LaboratoryReactionExchangePage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final LaboratoryDrawing source;
  private final boolean exporting;
  private final Window owner;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "reaction-exchange");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean writing, applied, conflict, restoring;
  private final TextArea input = new TextArea();
  private final TextField title = new TextField();
  private final ComboBox<ReactionExchange.Format> format = new ComboBox<>();
  private final ComboBox<String> encoding = new ComboBox<>();
  private final ComboBox<LaboratoryReactionStage> stages = new ComboBox<>();
  private final CheckBox maps = new CheckBox("Сохранить атомную карту"),
      showMaps = new CheckBox("Показать номера карты");
  private final TabPane tabs = new TabPane();
  private final VBox scheme = new VBox(12),
      notes = new VBox(8),
      mapping = new VBox(6),
      discard = new VBox(6);
  private final Button parse, apply, save, copy, stop;
  private final Label count = note("Откройте RXN или вставьте реакционный SMILES.");
  private ReactionExchange.Entry entry;
  private final List<ReactionComponentRole> roles = new ArrayList<>();
  private List<String> exportWarnings = List.of();
  private final List<String> names = new ArrayList<>();
  private final Map<Integer, String> restoredNames = new HashMap<>();
  private String autoTitle = "";
  private final Tab moleculeTab = new Tab("Структура крупно");
  private String output = "", sourceName = "Вставленный текст", restoredRoles = "";

  LaboratoryReactionExchangePage(
      final LaboratoryEditor editor,
      final String stageId,
      final boolean exporting,
      final Window owner,
      final Runnable close) {
    super(exporting ? "Экспорт реакции" : "Импорт реакции", close);
    this.editor = editor;
    this.source = editor.document();
    this.exporting = exporting;
    this.owner = owner;
    this.setId("chemistry-reaction-exchange");
    this.input.setId("reaction-exchange-input");
    this.input.setPromptText("[CH3:1][Br:2].[OH-:3]>>[CH3:1][OH:3].[Br-:2]\tПример схемы");
    this.input.setWrapText(false);
    this.input.setPrefRowCount(10);
    this.input.setEditable(!exporting);
    this.title.setPromptText("Название стадии");
    this.title.setId("reaction-exchange-title");
    this.title.setEditable(!exporting);
    this.format.getItems().setAll(ReactionExchange.Format.values());
    if (exporting) {
      this.format.getItems().remove(ReactionExchange.Format.AUTO);
    }
    this.format.setValue(
        exporting ? ReactionExchange.Format.RXN_V3000 : ReactionExchange.Format.AUTO);
    this.format.setId("reaction-exchange-format");
    this.encoding.getItems().setAll("UTF-8", "Windows-1251");
    this.encoding.setValue("UTF-8");
    this.maps.setSelected(true);
    this.parse = button(exporting ? "Подготовить файл" : "Прочитать реакцию", "parse", this::parse);
    this.parse.getStyleClass().add("primary-button");
    this.apply = button("Добавить стадию в опыт", "apply", this::commit);
    this.apply.getStyleClass().add("primary-button");
    this.apply.setDisable(true);
    this.save = button("Сохранить копию…", "save", this::save);
    this.copy =
        button(
            "Копировать текст",
            "copy",
            () -> {
              final ClipboardContent content = new ClipboardContent();
              content.putString(this.output);
              Clipboard.getSystemClipboard().setContent(content);
              this.status.setText("Реакция скопирована.");
            });
    this.save.setDisable(true);
    this.copy.setDisable(true);
    this.stop = button("Остановить", "stop", this::cancel);
    this.stop.setDisable(true);
    final FlowPane actions = new FlowPane(8, 8, note("Формат"), this.format);
    if (exporting) {
      this.stages.getItems().setAll(this.source.experiment().stages());
      this.stages.setConverter(
          new javafx.util.StringConverter<>() {
            public String toString(LaboratoryReactionStage s) {
              return s == null ? "Выберите стадию" : s.title();
            }

            public LaboratoryReactionStage fromString(String s) {
              throw new UnsupportedOperationException();
            }
          });
      this.stages.setMaxWidth(420);
      this.stages.setId("reaction-exchange-stage");
      this.stages.setValue(
          this.stages.getItems().stream()
              .filter(s -> s.id().equals(stageId))
              .findFirst()
              .orElse(this.stages.getItems().isEmpty() ? null : this.stages.getItems().getFirst()));
      actions.getChildren().addAll(this.stages, this.maps);
    } else {
      actions
          .getChildren()
          .addAll(
              this.encoding,
              button("Открыть файл…", "open", this::open),
              button(
                  "Вставить",
                  "paste",
                  () -> {
                    final String s = Clipboard.getSystemClipboard().getString();
                    if (s != null) {
                      this.sourceName = "Буфер обмена";
                      this.input.setText(s);
                      this.tabs.getSelectionModel().select(2);
                    }
                  }));
    }
    actions.getChildren().addAll(this.parse, this.stop);
    this.scheme.setPadding(new Insets(10));
    this.scheme.setMinWidth(0);
    this.scheme.setMinHeight(Region.USE_PREF_SIZE);
    this.notes.setPadding(new Insets(12));
    this.mapping.setPadding(new Insets(12));
    final Tab diagram = new Tab("Схема", scroll(this.scheme)),
        map = new Tab("Атомная карта", scroll(this.mapping)),
        text = new Tab(exporting ? "Готовый текст" : "Исходный текст", this.input),
        details = new Tab("Что переносится", scroll(this.notes));
    this.moleculeTab.setContent(note("Нажмите на рисунок вещества на вкладке «Схема»."));
    this.tabs.getTabs().setAll(diagram, map, text, details, this.moleculeTab);
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs.setMinHeight(210);
    this.tabs.setId("reaction-exchange-tabs");
    VBox.setVgrow(this.tabs, Priority.ALWAYS);
    final VBox body =
        new VBox(
            10, actions, this.title, new FlowPane(12, 6, this.count, this.showMaps), this.tabs);
    body.setPadding(new Insets(0, 16, 8, 16));
    body.setMinSize(0, 0);
    this.setCenter(body);
    this.discard
        .getChildren()
        .setAll(
            note("Текст ещё не добавлен в опыт."),
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
                button("Продолжить", "continue", () -> this.discardVisible(false))));
    this.discardVisible(false);
    final FlowPane footer = new FlowPane(8, 6);
    footer.getChildren().setAll(exporting ? List.of(this.save, this.copy) : List.of(this.apply));
    final VBox bottom = new VBox(8, this.discard, this.status, footer);
    bottom.setPadding(new Insets(8, 16, 14, 16));
    this.setBottom(bottom);
    this.input
        .textProperty()
        .addListener(
            o -> {
              if (!this.exporting && !this.restoring) {
                this.invalidate();
              }
            });
    this.format
        .valueProperty()
        .addListener(
            o -> {
              if (!this.restoring) {
                this.invalidate();
              }
            });
    this.stages
        .valueProperty()
        .addListener(
            o -> {
              this.invalidate();
              this.exportPreview();
            });
    this.maps.selectedProperty().addListener(o -> this.invalidate());
    this.showMaps.selectedProperty().addListener(o -> this.render());
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
          if (!this.exporting) {
            if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().size() == 1) {
              this.load(e.getDragboard().getFiles().getFirst().toPath());
              done = true;
            } else if (e.getDragboard().hasString()) {
              this.input.setText(e.getDragboard().getString());
              done = true;
            }
          }
          e.setDropCompleted(done);
          e.consume();
        });
    if (exporting) {
      this.exportPreview();
      this.status.setText(
          "Просмотрите стадию и подготовьте файл. Полный опыт сохраняется в QCHEM.");
    } else {
      this.tabs.getSelectionModel().select(text);
      this.status.setText(
          "Один файл → вся стадия. Сначала просмотрите схему; добавление отменяется одним Ctrl+Z.");
    }
  }

  private Button button(final String text, final String id, final Runnable action) {
    final Button b = new Button(text);
    b.setMinWidth(Region.USE_PREF_SIZE);
    b.setId("reaction-exchange-" + id);
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

  private void discardVisible(final boolean value) {
    this.discard.setVisible(value);
    this.discard.setManaged(value);
  }

  private void invalidate() {
    this.cancel();
    this.output = "";
    this.exportWarnings = List.of();
    this.save.setDisable(true);
    this.copy.setDisable(true);
    this.apply.setDisable(true);
    if (!this.exporting) {
      this.entry = null;
      this.roles.clear();
      this.names.clear();
      this.render();
    } else {
      this.input.clear();
    }
    this.status.setText(
        "Ввод изменён. "
            + (this.exporting ? "Подготовьте файл заново." : "Прочитайте реакцию заново."));
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
    if (this.parse != null) {
      this.parse.setDisable(false);
      this.stop.setDisable(true);
    }
    this.status.setText("Операция остановлена; исходный ввод сохранён.");
  }

  private void submit(final Callable<?> action, final java.util.function.Consumer<Object> done) {
    this.cancel();
    final long version = this.generation;
    this.busy(true);
    this.parse.setDisable(true);
    this.apply.setDisable(true);
    this.stop.setDisable(false);
    this.status.setText("Подготавливаю реакцию…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final Object result = action.call();
                Platform.runLater(
                    () -> {
                      if (!this.disposed && version == this.generation) {
                        this.completed();
                        try {
                          done.accept(result);
                        } catch (final RuntimeException e) {
                          this.status.setText(e.getMessage());
                        }
                      }
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && version == this.generation) {
                        this.completed();
                        this.status.setText(
                            e.getMessage() == null
                                ? "Не удалось обработать реакцию."
                                : e.getMessage());
                      }
                    });
              }
            });
  }

  private void completed() {
    this.writing = false;
    this.getCenter().setDisable(false);
    this.busy(false);
    this.parse.setDisable(false);
    this.stop.setDisable(true);
    this.save.setDisable(this.output.isEmpty());
    this.copy.setDisable(this.output.isEmpty());
  }

  private void parse() {
    if (this.running || this.disposed) {
      return;
    }
    this.output = "";
    this.save.setDisable(true);
    this.copy.setDisable(true);
    final var f = this.format.getValue();
    final var selected = this.stages.getValue();
    final String text = this.input.getText(), origin = this.sourceName;
    final boolean keepMap = this.maps.isSelected();
    if (this.exporting) {
      if (selected == null) {
        this.status.setText("В опыте ещё нет стадий.");
        return;
      }
      this.submit(
          () -> new CdkReactionExchange().write(this.source, selected.id(), f, keepMap),
          value -> {
            final var result = (ReactionExchange.Export) value;
            this.output = result.text();
            this.exportWarnings = result.warnings();
            this.input.setText(this.output);
            this.notes
                .getChildren()
                .setAll(result.warnings().stream().map(LaboratoryComputationPage::note).toList());
            this.save.setDisable(false);
            this.copy.setDisable(false);
            this.status.setText(
                "Файл готов · UTF-8. Ограничения формата указаны на вкладке «Что переносится».");
          });
    } else {
      this.entry = null;
      this.roles.clear();
      this.names.clear();
      this.render();
      this.submit(
          () -> new CdkReactionExchange().read(text, f, origin),
          value -> {
            this.entry = (ReactionExchange.Entry) value;
            this.names.addAll(
                this.entry.participants().stream()
                    .map(p -> p.structure().drawing().molecule().displayName())
                    .toList());
            this.restoredNames.forEach(
                (i, name) -> {
                  if (i < this.names.size()) {
                    this.names.set(i, name);
                  }
                });
            this.restoredNames.clear();
            this.roles.addAll(
                this.entry.participants().stream()
                    .map(ReactionExchange.Participant::role)
                    .toList());
            if (!this.restoredRoles.isEmpty()) {
              final String[] saved = this.restoredRoles.split(",");
              if (saved.length == this.roles.size()) {
                for (int i = 0; i < saved.length; i++) {
                  this.roles.set(i, ReactionComponentRole.valueOf(saved[i]));
                }
              }
              this.restoredRoles = "";
            }
            if (this.title.getText().isBlank() || this.title.getText().equals(this.autoTitle)) {
              this.title.setText(this.entry.title());
            }
            this.autoTitle = this.entry.title();
            this.render();
            this.tabs.getSelectionModel().select(0);
            this.apply.setDisable(this.conflict);
            this.status.setText(
                this.conflict
                    ? "Исходный опыт изменился. Скопируйте текст и откройте импорт заново."
                    : "Проверьте схему и роли агентов, затем добавьте стадию. Количества задаются в «Загрузка и выход».");
          });
    }
  }

  private void exportPreview() {
    final var stage = this.stages.getValue();
    if (stage == null) {
      this.entry = null;
      this.render();
      return;
    }
    final List<ReactionExchange.Participant> participants = new ArrayList<>();
    this.roles.clear();
    this.names.clear();
    for (final var p : stage.participants()) {
      final Map<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId, Integer> numbers =
          new LinkedHashMap<>();
      stage.atomMapping().labels().stream()
          .filter(l -> l.componentId().equals(p.componentId()))
          .forEach(l -> numbers.put(l.atomId(), l.number()));
      participants.add(
          new ReactionExchange.Participant(
              new StructureExchange.Entry(
                  this.source.layouts().get(p.componentId()), Map.of(), List.of()),
              p.role(),
              numbers));
      this.roles.add(p.role());
      this.names.add(this.source.layouts().get(p.componentId()).molecule().displayName());
    }
    if (participants.isEmpty()) {
      this.entry = null;
      this.render();
      return;
    }
    this.entry =
        new ReactionExchange.Entry(
            stage.title(),
            participants,
            stage.atomMapping().problems(this.source.experiment(), stage));
    this.title.setText(stage.title());
    this.render();
  }

  private void render() {
    this.scheme.getChildren().clear();
    this.mapping.getChildren().clear();
    this.notes.getChildren().clear();
    if (this.entry == null) {
      this.count.setText("Схема появится после чтения реакции");
      return;
    }
    final FlowPane left = new FlowPane(12, 12),
        right = new FlowPane(12, 12),
        agents = new FlowPane(12, 12);
    for (final FlowPane side : List.of(left, right, agents)) {
      side.prefWrapLengthProperty().bind(side.widthProperty());
      side.setRowValignment(VPos.TOP);
      side.setMinHeight(Region.USE_PREF_SIZE);
      side.setMaxHeight(Region.USE_PREF_SIZE);
    }
    int r = 0, p = 0, a = 0, labels = 0;
    for (int i = 0; i < this.entry.participants().size(); i++) {
      final var participant = this.entry.participants().get(i);
      final var role = this.roles.get(i);
      final VBox card =
          this.card(
              participant,
              i,
              role == ReactionComponentRole.REACTANT
                  ? left
                  : role == ReactionComponentRole.PRODUCT ? right : agents);
      if (role == ReactionComponentRole.REACTANT) {
        left.getChildren().add(card);
        r++;
      } else if (role == ReactionComponentRole.PRODUCT) {
        right.getChildren().add(card);
        p++;
      } else {
        agents.getChildren().add(card);
        a++;
      }
      labels += participant.atomNumbers().size();
      if (!participant.atomNumbers().isEmpty()) {
        final Label name = note(this.names.get(i) + " · " + LaboratoryInspector.roleText(role));
        name.setStyle("-fx-font-weight: bold;");
        this.mapping.getChildren().add(name);
        final String text =
            participant.atomNumbers().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(
                    e -> {
                      final var atom =
                          participant.structure().drawing().molecule().atoms().stream()
                              .filter(x -> x.id().equals(e.getKey()))
                              .findFirst();
                      return e.getValue()
                          + " → "
                          + atom.map(x -> x.element().symbol().value()).orElse("атом удалён")
                          + " ("
                          + e.getKey().value()
                          + ")";
                    })
                .collect(java.util.stream.Collectors.joining("    ·    "));
        this.mapping.getChildren().add(note(text));
      }
    }
    this.count.setText(
        r + " исходн.  →  " + p + " прод." + (a == 0 ? "" : "  ·  " + a + " агент."));
    final VBox reactants = new VBox(10, note("ИСХОДНЫЕ ВЕЩЕСТВА"), left),
        products = new VBox(10, note("ПРОДУКТЫ"), right);
    reactants.setMinWidth(0);
    products.setMinWidth(0);
    reactants.setMinHeight(Region.USE_PREF_SIZE);
    products.setMinHeight(Region.USE_PREF_SIZE);
    left.setMinWidth(0);
    right.setMinWidth(0);
    final Label arrow = note("→");
    arrow.setStyle("-fx-font-size: 28px; -fx-text-fill: #246f88;");
    arrow.setPadding(new Insets(75, 0, 0, 0));
    final GridPane equation = new GridPane();
    equation.setHgap(8);
    equation.setVgap(12);
    equation.setMinWidth(0);
    equation.setMinHeight(Region.USE_PREF_SIZE);
    equation.setMaxHeight(Region.USE_PREF_SIZE);
    GridPane.setValignment(arrow, VPos.TOP);
    final Runnable arrange =
        () -> {
          final boolean wide = this.scheme.getWidth() >= 960;
          if (Boolean.valueOf(wide).equals(equation.getUserData())) {
            return;
          }
          equation.setUserData(wide);
          equation.getChildren().clear();
          equation.getColumnConstraints().clear();
          if (wide) {
            for (final double width : new double[] {47, 6, 47}) {
              final ColumnConstraints c = new ColumnConstraints();
              c.setPercentWidth(width);
              equation.getColumnConstraints().add(c);
            }
            arrow.setText("→");
            arrow.setPadding(new Insets(75, 0, 0, 0));
            reactants.prefWidthProperty().bind(equation.widthProperty().subtract(16).multiply(.47));
            products.prefWidthProperty().bind(equation.widthProperty().subtract(16).multiply(.47));
            equation.addRow(0, reactants, arrow, products);
          } else {
            final ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100);
            equation.getColumnConstraints().add(c);
            arrow.setText("↓");
            arrow.setPadding(Insets.EMPTY);
            reactants.prefWidthProperty().unbind();
            products.prefWidthProperty().unbind();
            reactants.setPrefWidth(Region.USE_COMPUTED_SIZE);
            products.setPrefWidth(Region.USE_COMPUTED_SIZE);
            equation.addColumn(0, reactants, arrow, products);
          }
        };
    equation.widthProperty().addListener(o -> arrange.run());
    this.scheme.getChildren().add(equation);
    arrange.run();
    if (a > 0) {
      this.scheme
          .getChildren()
          .addAll(note(this.exporting ? "АГЕНТЫ" : "АГЕНТЫ · уточните их роль"), agents);
    }
    this.mapping
        .getChildren()
        .addFirst(
            note(
                labels == 0
                    ? "В исходной схеме нет атомной карты. Соответствия автоматически не придумываются."
                    : "Метки из исходного файла: "
                        + labels
                        + ". Одинаковые номера связывают атомы исходника и продукта; непарные номера допустимы. Это не предсказание механизма."));
    this.notes
        .getChildren()
        .setAll(
            (this.output.isEmpty() ? this.entry.notes() : this.exportWarnings)
                .stream().map(LaboratoryComputationPage::note).toList());
    this.notes
        .getChildren()
        .add(
            note(
                "Отдельные названия веществ из RXN сохраняются, если они есть в файле. SMILES не задаёт имена веществ; систематические названия здесь не вычисляются."));
  }

  private VBox card(
      final ReactionExchange.Participant participant, final int index, final FlowPane side) {
    final var drawing = participant.structure().drawing();
    final LaboratoryAdaptivePreview viewport =
        new LaboratoryAdaptivePreview(
            () -> drawing,
            (g, scale) -> {
              if (this.showMaps.isSelected()) {
                g.setFill(LaboratoryPreviewTheme.palette(g.getCanvas()).stereo(true));
                g.setFont(javafx.scene.text.Font.font(10 / scale));
                drawing
                    .atoms()
                    .forEach(
                        atom -> {
                          final var number = participant.atomNumbers().get(atom.atomId());
                          if (number != null) {
                            g.fillText(
                                number.toString(),
                                atom.point2D().x() + 7 / scale,
                                atom.point2D().y() - 7 / scale);
                          }
                        });
              }
            });
    viewport.setId("reaction-exchange-preview-" + index);
    final Canvas canvas = viewport.canvas();
    final javafx.scene.Node name;
    if (this.exporting) {
      final Label label = note(this.names.get(index));
      label.setMaxWidth(Double.MAX_VALUE);
      label.setStyle("-fx-font-weight: bold;");
      name = label;
    } else {
      final TextField field = new TextField(this.names.get(index));
      field.setPromptText("Название вещества");
      field.setTooltip(
          new Tooltip(
              "Название из файла или ваше обозначение. IUPAC автоматически не вычисляется."));
      field.textProperty().addListener((o, a, b2) -> this.names.set(index, b2));
      field.setId("reaction-exchange-name-" + index);
      name = field;
    }
    final Label formula = note(drawing.molecule().formula().hillNotation());
    final Hyperlink enlarge = new Hyperlink("Структура крупно");
    enlarge.setId("reaction-exchange-enlarge-" + index);
    enlarge.setOnAction(e -> this.enlarge(participant));
    final VBox card = new VBox(7, viewport, name, formula, enlarge);
    card.setPadding(new Insets(10));
    card.setMinWidth(0);
    card.setMinHeight(Region.USE_PREF_SIZE);
    card.getStyleClass().add("laboratory-reaction-card");
    final boolean complex = LaboratoryAdaptivePreview.complex(drawing);
    card.prefWidthProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(180, Math.min(complex ? 600 : 262, side.getWidth())),
                side.widthProperty()));
    card.maxWidthProperty().bind(card.prefWidthProperty());
    viewport
        .prefHeightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> viewport.computePrefHeight(card.getPrefWidth() - 22),
                card.prefWidthProperty()));
    if (!this.exporting
        && this.roles.get(index) != ReactionComponentRole.REACTANT
        && this.roles.get(index) != ReactionComponentRole.PRODUCT) {
      final ComboBox<ReactionComponentRole> role = new ComboBox<>();
      role.getItems()
          .setAll(
              Arrays.stream(ReactionComponentRole.values())
                  .filter(
                      x ->
                          x != ReactionComponentRole.REACTANT && x != ReactionComponentRole.PRODUCT)
                  .toList());
      role.setValue(this.roles.get(index));
      role.setConverter(
          new javafx.util.StringConverter<>() {
            public String toString(ReactionComponentRole r) {
              return LaboratoryInspector.roleText(r);
            }

            public ReactionComponentRole fromString(String s) {
              throw new UnsupportedOperationException();
            }
          });
      role.setOnAction(e -> this.roles.set(index, role.getValue()));
      card.getChildren().add(role);
    } else {
      card.getChildren().add(note(LaboratoryInspector.roleText(this.roles.get(index))));
    }
    canvas.setCursor(javafx.scene.Cursor.HAND);
    canvas.setOnMouseClicked(e -> this.enlarge(participant));
    Tooltip.install(canvas, new Tooltip("Нажмите, чтобы рассмотреть структуру крупно"));
    canvas.setAccessibleText(
        drawing.molecule().displayName() + ", " + drawing.molecule().formula().hillNotation());
    return card;
  }

  private void enlarge(final ReactionExchange.Participant participant) {
    final var drawing = participant.structure().drawing();
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final Canvas picture = new Canvas();
    final ScrollPane viewport = new ScrollPane(new StackPane(picture));
    viewport.setPannable(true);
    viewport.setFitToWidth(false);
    viewport.setFitToHeight(false);
    final Slider zoom = new Slider(0.5, 4, 1);
    zoom.setPrefWidth(180);
    final Runnable paint =
        () -> {
          final double w = Math.max(240, viewport.getViewportBounds().getWidth()),
              h = Math.max(180, viewport.getViewportBounds().getHeight());
          final double
              fit =
                  Math.min(
                      (w - 50) / Math.max(1, bounds.getWidth()),
                      (h - 50) / Math.max(1, bounds.getHeight())),
              scale = Math.min(3, fit) * zoom.getValue();
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
    zoom.valueProperty().addListener(o -> paint.run());
    viewport.viewportBoundsProperty().addListener(o -> paint.run());
    picture.setOnScroll(
        e -> {
          if (e.isControlDown()) {
            zoom.setValue(zoom.getValue() * (e.getDeltaY() > 0 ? 1.15 : 1 / 1.15));
            e.consume();
          }
        });
    final BorderPane view = new BorderPane(viewport);
    view.setTop(
        new FlowPane(
            10,
            6,
            note("Масштаб"),
            zoom,
            button("Вписать", "fit", () -> zoom.setValue(1)),
            note("Перетаскивание мышью · Ctrl + колесо")));
    this.moleculeTab.setContent(view);
    this.tabs.getSelectionModel().select(this.moleculeTab);
    Platform.runLater(paint);
  }

  private void commit() {
    if (this.exporting || this.running || this.entry == null || this.apply.isDisabled()) {
      return;
    }
    if (this.conflict || !this.editor.document().equals(this.source)) {
      this.conflict = true;
      this.apply.setDisable(true);
      this.status.setText("Опыт изменился. Скопируйте исходный текст и откройте импорт заново.");
      return;
    }
    final List<ReactionExchange.Participant> participants = new ArrayList<>();
    for (int i = 0; i < this.entry.participants().size(); i++) {
      final var p = this.entry.participants().get(i);
      final var renamed =
          new MoleculeSketchService().rename(p.structure().drawing(), this.names.get(i));
      participants.add(
          new ReactionExchange.Participant(
              new StructureExchange.Entry(renamed, p.structure().fields(), p.structure().notes()),
              this.roles.get(i),
              p.atomNumbers()));
    }
    this.editor.importReaction(
        new ReactionExchange.Entry(this.title.getText(), participants, this.entry.notes()));
    this.applied = true;
    this.finish();
  }

  private void open() {
    final FileChooser c = new FileChooser();
    c.setTitle("Открыть реакцию");
    c.getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "RXN / реакционный SMILES", "*.rxn", "*.rsmi", "*.smi", "*.smiles", "*.txt"));
    final var file = c.showOpenDialog(this.owner);
    if (file != null) {
      this.load(file.toPath());
    }
  }

  void load(final Path path) {
    final var charset = Charset.forName(this.encoding.getValue());
    this.submit(
        () -> LaboratoryExchangeFiles.read(path, charset),
        value -> {
          this.sourceName = path.getFileName().toString();
          this.title.clear();
          this.input.setText((String) value);
          this.tabs.getSelectionModel().select(2);
          this.status.setText(
              "Открыт «" + this.sourceName + "». Прочитайте реакцию для просмотра.");
        });
  }

  private void save() {
    if (this.output.isEmpty()) {
      return;
    }
    final String extension =
        this.format.getValue() == ReactionExchange.Format.SMILES ? "rsmi" : "rxn";
    final FileChooser c = new FileChooser();
    c.setTitle("Сохранить новую копию реакции");
    c.setInitialFileName("reaction." + extension);
    c.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(extension.toUpperCase(Locale.ROOT), "*." + extension));
    final var file = c.showSaveDialog(this.owner);
    if (file != null) {
      final String text = this.output;
      this.submit(
          () -> {
            LaboratoryExchangeFiles.writeReactionCopy(file.toPath(), text);
            return file.getName();
          },
          name -> this.status.setText("Сохранён «" + name + "» · UTF-8."));
      this.writing = true;
      this.getCenter().setDisable(true);
      this.stop.setDisable(true);
      this.save.setDisable(true);
      this.copy.setDisable(true);
    }
  }

  boolean pending() {
    return !this.exporting
        && !this.applied
        && (!this.input.getText().isBlank() || !this.title.getText().isBlank());
  }

  @Override
  boolean canClose() {
    if (this.writing) {
      this.status.setText("Дождитесь завершения записи файла.");
      return false;
    }
    if (this.pending() || this.running) {
      this.discardVisible(true);
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
    final String prefix = "reaction-exchange.";
    if (restore) {
      this.restoring = true;
      try {
        this.input.setText(fields.getProperty(prefix + "text", ""));
        this.title.setText(fields.getProperty(prefix + "title", ""));
        this.format.setValue(
            ReactionExchange.Format.valueOf(fields.getProperty(prefix + "format", "AUTO")));
        this.sourceName = fields.getProperty(prefix + "source", "Восстановленный текст");
        this.restoredRoles = fields.getProperty(prefix + "roles", "");
        for (int i = 0; i < 100; i++) {
          if (fields.containsKey(prefix + "name." + i)) {
            this.restoredNames.put(i, fields.getProperty(prefix + "name." + i));
          }
        }
        this.conflict =
            Boolean.parseBoolean(fields.getProperty(prefix + "conflict", "false"))
                || !this.fingerprint().equals(fields.getProperty(prefix + "fingerprint"));
        this.status.setText(
            this.conflict
                ? "Ввод восстановлен; исходный опыт изменился. Применение отключено."
                : "Ввод восстановлен. Прочитайте реакцию и проверьте схему.");
      } finally {
        this.restoring = false;
      }
      return;
    }
    for (int i = 0; i < this.names.size(); i++) {
      fields.setProperty(prefix + "name." + i, this.names.get(i));
    }
    if (this.names.isEmpty()) {
      this.restoredNames.forEach((i, name) -> fields.setProperty(prefix + "name." + i, name));
    }
    fields.setProperty(prefix + "text", this.input.getText());
    fields.setProperty(prefix + "title", this.title.getText());
    fields.setProperty(prefix + "format", this.format.getValue().name());
    fields.setProperty(prefix + "source", this.sourceName);
    fields.setProperty(
        prefix + "roles",
        this.roles.isEmpty()
            ? this.restoredRoles
            : String.join(",", this.roles.stream().map(Enum::name).toList()));
    fields.setProperty(
        prefix + "conflict",
        Boolean.toString(this.conflict || !this.source.equals(this.editor.document())));
    fields.setProperty(prefix + "fingerprint", this.fingerprint());
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
