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
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStructureExchange;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Review before atomic import; export remains independent from the experiment save format. */
final class LaboratoryExchangePage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final LaboratoryDrawing source;
  private final Window owner;
  private final boolean exporting;
  private final TextArea input = new TextArea();
  private final ComboBox<StructureExchange.Format> format = new ComboBox<>();
  private final ComboBox<String> encoding = new ComboBox<>();
  private final ListView<Row> rows = new ListView<>();
  private final Canvas picture = new Canvas(380, 210);
  private final VBox detail = new VBox(8);
  private final Label count = note("Вещества появятся после чтения структуры");
  private final Button parse, apply, save, copy, stop;
  private final FlowPane discard = new FlowPane(8, 6);
  private final TitledPane sourcePane = new TitledPane();
  private final SplitPane split = new SplitPane();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "structure-exchange");
            t.setDaemon(true);
            return t;
          });
  private Future<?> task;
  private long generation;
  private boolean applied, restoring, conflict, writing;
  private String sourceName = "Вставленный текст", output = "", restoredSelection = "";

  private static final class Row {
    final StructureExchange.Entry entry;
    boolean selected = true;
    ReactionComponentRole role = ReactionComponentRole.REACTANT;

    Row(final StructureExchange.Entry entry) {
      this.entry = entry;
    }

    @Override
    public String toString() {
      return this.entry.drawing().molecule().displayName();
    }
  }

  LaboratoryExchangePage(
      final LaboratoryEditor editor,
      final String selected,
      final boolean exporting,
      final Window owner,
      final Runnable close) {
    super(exporting ? "Экспорт структур" : "Добавить структуры", close);
    this.editor = editor;
    this.source = editor.document();
    this.owner = owner;
    this.exporting = exporting;
    this.setId("chemistry-exchange-page");
    this.input.setId("chemistry-exchange-text");
    this.input.setWrapText(false);
    this.input.setPromptText(
        "Вставьте SMILES, MOL или SDF.\nДля нескольких SMILES — по одному веществу на строку; после пробела можно указать название.");
    this.input.setPrefRowCount(9);
    this.format.getItems().setAll(StructureExchange.Format.values());
    if (exporting) {
      this.format.getItems().remove(StructureExchange.Format.AUTO);
    }
    this.format.setValue(exporting ? StructureExchange.Format.SDF : StructureExchange.Format.AUTO);
    this.format.setId("chemistry-exchange-format");
    this.format.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(StructureExchange.Format f) {
            return f == StructureExchange.Format.AUTO ? "Определить" : f.name();
          }

          public StructureExchange.Format fromString(String s) {
            throw new UnsupportedOperationException();
          }
        });
    this.encoding.getItems().setAll("UTF-8", "Windows-1251");
    this.encoding.setValue("UTF-8");
    this.parse =
        button(exporting ? "Подготовить экспорт" : "Прочитать структуры", "parse", this::parse);
    this.apply = button("Добавить выбранные в опыт", "apply", this::commit);
    this.apply.setDisable(true);
    this.apply.getStyleClass().add("primary-button");
    this.save = button("Сохранить файл…", "save", this::save);
    this.copy =
        button(
            "Копировать текст",
            "copy",
            () -> {
              if (!this.output.isEmpty()) {
                final ClipboardContent content = new ClipboardContent();
                content.putString(this.output);
                Clipboard.getSystemClipboard().setContent(content);
                this.status.setText("Текст экспорта скопирован.");
              }
            });
    this.stop = button("Остановить", "stop", this::cancel);
    this.stop.setDisable(true);
    this.rows.setId("chemistry-exchange-rows");
    this.rows.setPrefHeight(220);
    this.rows.setMinHeight(120);
    this.rows.setCellFactory(
        list ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Row row, final boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                  this.setGraphic(null);
                  this.setText(null);
                  return;
                }
                final CheckBox pick = new CheckBox();
                pick.setSelected(row.selected);
                pick.setOnAction(
                    e -> {
                      row.selected = pick.isSelected();
                      changedSelection();
                    });
                final Label title = new Label(row.toString());
                title.setMaxWidth(Double.MAX_VALUE);
                title.setTooltip(new Tooltip(row.toString()));
                final Label formula =
                    new Label(row.entry.drawing().molecule().formula().hillNotation());
                formula.setStyle("-fx-opacity: 0.7;");
                final VBox caption = new VBox(3, title, formula);
                caption.setMinWidth(0);
                HBox.setHgrow(caption, Priority.ALWAYS);
                final ComboBox<ReactionComponentRole> role = new ComboBox<>();
                role.getItems().setAll(ReactionComponentRole.values());
                role.setValue(row.role);
                role.setPrefWidth(130);
                role.setConverter(
                    new javafx.util.StringConverter<>() {
                      public String toString(ReactionComponentRole r) {
                        return roleName(r);
                      }

                      public ReactionComponentRole fromString(String s) {
                        throw new UnsupportedOperationException();
                      }
                    });
                role.setOnAction(e -> row.role = role.getValue());
                final HBox line =
                    exporting ? new HBox(10, pick, caption) : new HBox(10, pick, caption, role);
                line.setAlignment(Pos.CENTER_LEFT);
                line.setPadding(new Insets(7, 2, 7, 2));
                this.setGraphic(line);
              }
            });
    this.rows.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> this.preview());
    this.picture.widthProperty().addListener(o -> this.paint());
    final StackPane image = new StackPane(this.picture);
    image.setMinSize(0, 210);
    image.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
    this.picture.widthProperty().bind(image.widthProperty());
    final var clip = new javafx.scene.shape.Rectangle();
    clip.widthProperty().bind(image.widthProperty());
    clip.heightProperty().bind(image.heightProperty());
    image.setClip(clip);
    final FlowPane choices =
        new FlowPane(
            8,
            6,
            button("Выбрать все", "all", () -> this.select(true)),
            button("Снять выбор", "none", () -> this.select(false)));
    final VBox list = new VBox(8, this.count, choices, this.rows);
    list.setMinWidth(0);
    VBox.setVgrow(this.rows, Priority.ALWAYS);
    final VBox preview = new VBox(10, image, this.detail);
    preview.setPadding(new Insets(0, 12, 0, 12));
    preview.setMinWidth(0);
    this.split.getItems().setAll(list, scroll(preview));
    this.split.setDividerPositions(.52);
    this.split.setMinHeight(180);
    VBox.setVgrow(this.split, Priority.ALWAYS);
    final FlowPane actions = new FlowPane(8, 8, note("Формат"), this.format);
    if (!exporting) {
      actions
          .getChildren()
          .addAll(
              this.encoding,
              button("Открыть файл…", "open", this::open),
              button(
                  "Вставить текст",
                  "paste",
                  () -> {
                    final var text = Clipboard.getSystemClipboard().getString();
                    if (text != null) {
                      this.sourceName = "Буфер обмена";
                      this.input.setText(text);
                    }
                  }));
    }
    actions.getChildren().addAll(this.parse, this.stop);
    final VBox body = new VBox(12, actions);
    body.setPadding(new Insets(4, 16, 8, 16));
    body.setMinSize(0, 0);
    if (exporting) {
      body.getChildren()
          .addAll(
              note(
                  "MOL — одно вещество; SDF и SMILES — выбранный набор. Экспорт включает структуры и исходные поля SDF. Для полного опыта с количествами, названиями, анализами и журналом используйте QCHEM."),
              this.split,
              this.outputPane());
      this.input.setEditable(false);
    } else {
      this.sourcePane.setAnimated(false);
      this.sourcePane.setText("Исходный текст");
      this.sourcePane.setContent(this.input);
      body.getChildren().addAll(this.sourcePane, this.split);
    }
    body.setMinHeight(Region.USE_PREF_SIZE);
    this.setCenter(scroll(body));
    this.discard
        .getChildren()
        .setAll(
            note("Ввод ещё не добавлен в опыт."),
            button(
                "Отбросить ввод и выйти",
                "discard",
                () -> {
                  this.cancel();
                  this.applied = true;
                  this.finish();
                }),
            button("Продолжить ввод", "continue", () -> this.showDiscard(false)));
    this.showDiscard(false);
    final FlowPane bottom = new FlowPane(8, 6);
    bottom.getChildren().addAll(exporting ? List.of(this.save, this.copy) : List.of(this.apply));
    final VBox footer = new VBox(8, this.discard, this.status, bottom);
    footer.setPadding(new Insets(8, 16, 14, 16));
    this.setBottom(footer);
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
    this.widthProperty()
        .addListener(
            o -> {
              this.split.setOrientation(
                  this.getWidth() < 850 ? Orientation.VERTICAL : Orientation.HORIZONTAL);
              this.split.setDividerPositions(this.getWidth() < 850 ? .47 : .52);
            });
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.isShortcutDown() && e.getCode() == KeyCode.ENTER) {
            if (!this.apply.isDisabled() && !exporting) {
              this.commit();
            } else {
              this.parse();
            }
            e.consume();
          }
        });
    this.input.setOnDragOver(
        e -> {
          if (!exporting && (e.getDragboard().hasFiles() || e.getDragboard().hasString())) {
            e.acceptTransferModes(TransferMode.COPY);
          }
          e.consume();
        });
    this.input.setOnDragDropped(
        e -> {
          boolean done = false;
          if (!exporting) {
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
      for (final var component : this.source.experiment().components()) {
        final Row row =
            new Row(
                new StructureExchange.Entry(
                    this.source.layouts().get(component.id()),
                    CdkStructureExchange.fields(component.molecule()),
                    List.of()));
        row.role = component.role();
        row.selected = selected == null || selected.equals(component.id());
        this.rows.getItems().add(row);
      }
      if (!this.rows.getItems().isEmpty()) {
        this.rows.getSelectionModel().selectFirst();
      }
    }
    this.changedSelection();
    this.status.setText(
        exporting
            ? "Выберите вещества и подготовьте файл. Исходный опыт не изменяется."
            : "Откройте файл или вставьте текст. Сначала появятся структуры для просмотра; затем выберите их роли в опыте.");
  }

  private TitledPane outputPane() {
    final TitledPane pane = new TitledPane("Текст готового файла", this.input);
    pane.setAnimated(false);
    pane.setExpanded(false);
    return pane;
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

  private void showDiscard(final boolean show) {
    this.discard.setVisible(show);
    this.discard.setManaged(show);
  }

  private Button button(final String label, final String id, final Runnable action) {
    final Button b = new Button(label);
    b.setId("chemistry-exchange-" + id);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException failure) {
            this.status.setText(failure.getMessage());
          }
        });
    return b;
  }

  private void select(final boolean value) {
    this.rows.getItems().forEach(r -> r.selected = value);
    this.rows.refresh();
    this.changedSelection();
  }

  private void changedSelection() {
    final long n = this.rows.getItems().stream().filter(r -> r.selected).count();
    this.count.setText(
        this.rows.getItems().isEmpty()
            ? "Пока нет прочитанных структур"
            : "Выбрано " + n + " из " + this.rows.getItems().size());
    this.apply.setDisable(this.running || this.conflict || n == 0);
    if (this.exporting) {
      this.cancel();
      this.output = "";
      this.input.clear();
    }
    this.save.setDisable(this.output.isEmpty());
    this.copy.setDisable(this.output.isEmpty());
  }

  private void invalidate() {
    this.cancel();
    this.output = "";
    this.restoredSelection = "";
    if (!this.exporting) {
      this.rows.getItems().clear();
    }
    this.changedSelection();
    this.status.setText("Ввод изменён. Прочитайте структуры заново.");
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
    this.parse.setDisable(false);
    this.stop.setDisable(true);
    this.status.setText("Операция остановлена. Ввод сохранён.");
  }

  private void parse() {
    if (this.running || this.disposed) {
      return;
    }
    this.output = "";
    this.save.setDisable(true);
    this.copy.setDisable(true);
    this.apply.setDisable(true);
    final var f = this.format.getValue();
    final String text = this.input.getText();
    final String origin = this.sourceName;
    final var selected =
        this.rows.getItems().stream().filter(r -> r.selected).map(r -> r.entry.drawing()).toList();
    if (!this.exporting) {
      this.rows.getItems().clear();
    }
    this.submit(
        () ->
            this.exporting
                ? new CdkStructureExchange().write(selected, f)
                : new CdkStructureExchange().read(text, f, origin),
        result -> {
          if (this.exporting) {
            this.output = (String) result;
            this.input.setText(this.output);
            this.save.setDisable(false);
            this.copy.setDisable(false);
            this.status.setText("Файл подготовлен · UTF-8. Можно сохранить или скопировать.");
          } else {
            for (final var entry : (List<StructureExchange.Entry>) result) {
              this.rows.getItems().add(new Row(entry));
            }
            this.sourcePane.setExpanded(false);
            this.restoreChoices();
            this.rows.getSelectionModel().selectFirst();
            this.changedSelection();
            this.status.setText(
                "Просмотрите структуры и роли. Добавление набора отменяется одним Ctrl+Z.");
          }
        });
  }

  private void submit(final Callable<?> action, final java.util.function.Consumer<Object> ready) {
    this.cancel();
    final long request = this.generation;
    this.busy(true);
    this.parse.setDisable(true);
    this.stop.setDisable(false);
    this.status.setText("Подготавливаю структуры…");
    this.task =
        this.worker.submit(
            () -> {
              try {
                final Object value = action.call();
                Platform.runLater(
                    () -> {
                      if (!this.disposed && request == this.generation) {
                        this.writing = false;
                        this.getCenter().setDisable(false);
                        this.busy(false);
                        this.parse.setDisable(false);
                        this.stop.setDisable(true);
                        try {
                          ready.accept(value);
                        } catch (final RuntimeException e) {
                          this.status.setText(e.getMessage());
                        }
                      }
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && request == this.generation) {
                        this.writing = false;
                        this.getCenter().setDisable(false);
                        this.busy(false);
                        this.parse.setDisable(false);
                        this.stop.setDisable(true);
                        this.status.setText(
                            e.getMessage() == null
                                ? "Не удалось обработать структуры."
                                : e.getMessage());
                      }
                    });
              }
            });
  }

  private void preview() {
    this.detail.getChildren().clear();
    final Row row = this.rows.getSelectionModel().getSelectedItem();
    if (row != null) {
      final var m = row.entry.drawing().molecule();
      final Label name = note(m.displayName());
      name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
      this.detail
          .getChildren()
          .addAll(
              name,
              note(
                  m.formula().hillNotation()
                      + " · ≈ "
                      + String.format(
                          Locale.ROOT, "%.3f", m.composition().mass().averageAtomicMass())
                      + " г/моль"));
      row.entry.notes().forEach(n -> this.detail.getChildren().add(note(n)));
      if (!row.entry.fields().isEmpty()) {
        final VBox props = new VBox(6);
        new TreeMap<>(row.entry.fields())
            .forEach((k, v) -> props.getChildren().add(note(k + ": " + v)));
        final TitledPane fields =
            new TitledPane("Исходные поля SDF · " + row.entry.fields().size(), props);
        fields.setExpanded(false);
        this.detail.getChildren().add(fields);
      }
    }
    this.paint();
  }

  private void paint() {
    final var row = this.rows.getSelectionModel().getSelectedItem();
    final var g = this.picture.getGraphicsContext2D();
    g.setFill(javafx.scene.paint.Color.WHITE);
    g.fillRect(0, 0, this.picture.getWidth(), this.picture.getHeight());
    if (row != null && this.picture.getWidth() > 20) {
      final var bounds = LaboratoryCanvas.previewBounds(row.entry.drawing());
      final double scale =
          Math.min(
              1.5,
              Math.min(
                  (this.picture.getWidth() - 30) / Math.max(1, bounds.getWidth()),
                  (this.picture.getHeight() - 30) / Math.max(1, bounds.getHeight())));
      g.save();
      g.translate(
          (this.picture.getWidth() - bounds.getWidth() * scale) / 2 - bounds.getMinX() * scale,
          (this.picture.getHeight() - bounds.getHeight() * scale) / 2 - bounds.getMinY() * scale);
      g.scale(scale, scale);
      LaboratoryCanvas.paintPreview(g, row.entry.drawing());
      g.restore();
    }
  }

  private void commit() {
    if (this.exporting || this.running || this.apply.isDisabled()) {
      return;
    }
    if (this.conflict || !this.editor.document().equals(this.source)) {
      this.conflict = true;
      this.apply.setDisable(true);
      this.status.setText(
          "Опыт изменился после открытия формы. Сохраните текст и откройте импорт заново.");
      return;
    }
    final var selected = this.rows.getItems().stream().filter(r -> r.selected).toList();
    this.editor.importStructures(
        selected.stream().map(r -> r.entry.drawing()).toList(),
        selected.stream().map(r -> r.role).toList());
    this.applied = true;
    this.finish();
  }

  private void open() {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Открыть структуры");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "MOL, SDF, SMILES", "*.mol", "*.sdf", "*.smi", "*.smiles", "*.txt"));
    final var file = chooser.showOpenDialog(this.owner);
    if (file != null) {
      this.load(file.toPath());
    }
  }

  void load(final Path path) {
    final Charset charset = Charset.forName(this.encoding.getValue());
    this.submit(
        () -> LaboratoryExchangeFiles.read(path, charset),
        value -> {
          this.sourceName = path.getFileName().toString();
          this.input.setText((String) value);
          this.status.setText("Открыт «" + this.sourceName + "». Нажмите «Прочитать структуры».");
        });
  }

  private void save() {
    if (this.output.isEmpty()) {
      return;
    }
    final FileChooser chooser = new FileChooser();
    final String suffix =
        this.format.getValue() == StructureExchange.Format.SMILES
            ? "smi"
            : this.format.getValue().name().toLowerCase(Locale.ROOT);
    chooser.setInitialFileName("structures." + suffix);
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(suffix.toUpperCase(Locale.ROOT), "*." + suffix));
    final var file = chooser.showSaveDialog(this.owner);
    if (file != null) {
      final String text = this.output;
      this.submit(
          () -> {
            LaboratoryExchangeFiles.write(file.toPath(), text);
            return file.getName();
          },
          name -> {
            this.save.setDisable(false);
            this.copy.setDisable(false);
            this.status.setText("Сохранён «" + name + "» · UTF-8.");
          });
      this.writing = true;
      this.getCenter().setDisable(true);
      this.stop.setDisable(true);
      this.save.setDisable(true);
      this.copy.setDisable(true);
    }
  }

  boolean pending() {
    return !this.exporting && !this.applied && !this.input.getText().isBlank();
  }

  @Override
  boolean canClose() {
    if (this.writing) {
      this.status.setText("Завершается запись файла. Дождитесь результата.");
      return false;
    }
    if (this.pending() || this.running) {
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
    if (restore) {
      this.restoring = true;
      try {
        this.input.setText(fields.getProperty("exchange.text", ""));
        this.format.setValue(
            StructureExchange.Format.valueOf(fields.getProperty("exchange.format", "AUTO")));
        this.sourceName = fields.getProperty("exchange.source", "Восстановленный текст");
        this.restoredSelection = fields.getProperty("exchange.selection", "");
        this.conflict =
            Boolean.parseBoolean(fields.getProperty("exchange.conflict", "false"))
                || !this.fingerprint().equals(fields.getProperty("exchange.fingerprint"));
        this.status.setText(
            this.conflict
                ? "Ввод восстановлен, но исходный опыт изменился. Применение отключено."
                : "Ввод восстановлен. Прочитайте структуры и проверьте выбор.");
      } finally {
        this.restoring = false;
      }
      return;
    }
    fields.setProperty(
        "exchange.conflict",
        Boolean.toString(this.conflict || !this.editor.document().equals(this.source)));
    fields.setProperty("exchange.text", this.input.getText());
    fields.setProperty("exchange.format", this.format.getValue().name());
    fields.setProperty("exchange.source", this.sourceName);
    fields.setProperty("exchange.fingerprint", this.fingerprint());
    fields.setProperty(
        "exchange.selection",
        this.rows.getItems().isEmpty()
            ? this.restoredSelection
            : String.join(
                ";",
                this.rows.getItems().stream().map(r -> r.selected + ":" + r.role.name()).toList()));
  }

  private String fingerprint() {
    try {
      return java.util.HexFormat.of()
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

  private void restoreChoices() {
    if (this.restoredSelection.isEmpty()) {
      return;
    }
    final String[] saved = this.restoredSelection.split(";");
    if (saved.length == this.rows.getItems().size()) {
      for (int i = 0; i < saved.length; i++) {
        final String[] values = saved[i].split(":");
        this.rows.getItems().get(i).selected = Boolean.parseBoolean(values[0]);
        this.rows.getItems().get(i).role = ReactionComponentRole.valueOf(values[1]);
      }
    }
    this.restoredSelection = "";
    this.rows.refresh();
  }
}
