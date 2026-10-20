/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.OpsinChemicalName;

/** A local chemical-name input with a reviewed 2D result before adding a new participant. */
final class LaboratoryNameStructurePage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final String experiment;
  private final TextArea input = new TextArea();
  private final ComboBox<ReactionComponentRole> role = new ComboBox<>();
  private final TextArea details = new TextArea();
  private final Label resultHeading = note("");
  private final Label warning = note("");
  private final Label empty = note("Здесь появится структура по введённому названию.");
  private final TitledPane technical = new TitledPane();
  private final Button stop;
  private final Button parse;
  private final FlowPane viewControls = new FlowPane(8, 6);
  private final Canvas picture = new Canvas(700, 360);
  private final CheckBox reviewed = new CheckBox("Предупреждение прочитано; структура проверена");
  private final Button apply;
  private final VBox discard = new VBox(8);
  private final Slider zoom = new Slider(.25, 2, 1);
  private Task<OpsinChemicalName.Result> task;
  private OpsinChemicalName.Result result;
  private boolean added;
  private double panX, panY, pressX, pressY, startX, startY;

  LaboratoryNameStructurePage(final LaboratoryEditor editor, final Runnable close) {
    super("Структура по названию", close);
    this.editor = editor;
    this.experiment = editor.document().experiment().id();
    this.setId("chemistry-name-structure");
    this.input.setPrefRowCount(2);
    this.input.setWrapText(true);
    this.input.setPromptText("Например: 1H-indazole");
    this.input.setId("chemical-name-input");
    this.role.getItems().setAll(ReactionComponentRole.values());
    this.role.setValue(ReactionComponentRole.REACTANT);
    this.role.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final ReactionComponentRole r) {
            return r == null ? "" : LaboratoryInspector.roleText(r);
          }

          public ReactionComponentRole fromString(final String s) {
            throw new UnsupportedOperationException();
          }
        });
    this.details.setEditable(false);
    this.details.setWrapText(true);
    this.details.setPrefRowCount(5);
    this.details.setId("chemical-name-details");
    this.reviewed.setVisible(false);
    this.reviewed.setManaged(false);
    this.apply = this.button("Добавить в опыт", "apply", this::apply);
    this.apply.setDisable(true);
    this.apply.getStyleClass().add("laboratory-primary-action");
    this.apply.setTooltip(
        new Tooltip(
            "Добавить отдельного участника со структурой и введённым названием. Введённое имя сохраняется как ручной синоним с источником OPSIN."));
    this.parse = this.button("Построить структуру", "parse", this::parse);
    this.parse.getStyleClass().add("laboratory-primary-action");
    this.stop =
        this.button(
            "Остановить",
            "stop",
            () -> {
              if (this.task != null) {
                this.task.cancel();
              }
            });
    visible(this.stop, false);
    this.resultHeading.setId("chemical-name-result-heading");
    this.warning.setId("chemical-name-warning");
    this.empty.setId("chemical-name-empty");
    this.empty.setMaxWidth(440);
    this.empty.setMouseTransparent(true);
    this.picture.setManaged(false);
    final StackPane image = new StackPane(this.picture, this.empty);
    image.setMinWidth(0);
    image.setPrefSize(640, 300);
    image.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 10;");
    image.setMinHeight(240);
    image.setMaxHeight(360);
    LaboratoryPreviewTheme.track(this.picture, this::paint);
    this.picture.widthProperty().bind(image.widthProperty());
    this.picture.heightProperty().bind(image.heightProperty());
    this.picture.widthProperty().addListener(o -> this.paint());
    this.picture.heightProperty().addListener(o -> this.paint());
    this.zoom.setPrefWidth(200);
    this.zoom.valueProperty().addListener(o -> this.paint());
    this.picture.setOnScroll(
        e -> {
          this.zoom.setValue(this.zoom.getValue() * (e.getDeltaY() > 0 ? 1.12 : .89));
          e.consume();
        });
    this.picture.setOnMousePressed(
        e -> {
          this.pressX = e.getX();
          this.pressY = e.getY();
          this.startX = this.panX;
          this.startY = this.panY;
        });
    this.picture.setOnMouseDragged(
        e -> {
          this.panX = this.startX + e.getX() - this.pressX;
          this.panY = this.startY + e.getY() - this.pressY;
          this.paint();
        });
    this.picture.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            this.fit();
          }
        });
    this.input
        .textProperty()
        .addListener(
            o -> {
              final boolean hadResult = this.result != null;
              this.result = null;
              this.reviewed.setSelected(false);
              this.details.clear();
              this.controls();
              this.paint();
              this.added = false;
              if (hadResult) {
                this.status.setText(
                    "Название изменено. Нажмите «Построить структуру» для нового просмотра.");
              }
            });
    this.reviewed.selectedProperty().addListener(o -> this.controls());
    this.technical.setText("Распознавание и SMILES");
    this.technical.setId("chemical-name-technical");
    this.technical.setExpanded(false);
    this.technical.setAnimated(false);
    this.technical.setMinHeight(Region.USE_PREF_SIZE);
    this.technical.setContent(
        new VBox(
            8,
            note(
                "OPSIN работает локально: поддерживает английскую номенклатуру и часть распространённых названий. Введённое имя сохраняется с источником; оно не становится автоматически подтверждённым IUPAC-названием."),
            this.details));
    this.viewControls
        .getChildren()
        .setAll(note("Масштаб"), this.zoom, this.button("Вписать", "fit", this::fit));
    final VBox body =
        new VBox(
            12,
            note("Название на английском"),
            this.input,
            new FlowPane(8, 6, this.parse, this.stop),
            this.resultHeading,
            image,
            this.viewControls,
            this.warning,
            this.reviewed,
            this.technical);
    body.setPadding(new Insets(12, 16, 8, 16));
    body.setMaxWidth(1080);
    VBox.setVgrow(image, Priority.ALWAYS);
    body.setMinWidth(0);
    body.setMinHeight(Region.USE_PREF_SIZE);
    final StackPane sheet = new StackPane(body);
    StackPane.setAlignment(body, javafx.geometry.Pos.TOP_CENTER);
    this.setCenter(scroll(sheet));
    this.discard
        .getChildren()
        .setAll(
            note("Название ещё не добавлено в опыт."),
            new FlowPane(
                8,
                6,
                this.button(
                    "Отбросить и выйти",
                    "discard",
                    () -> {
                      this.added = true;
                      this.finish();
                    }),
                this.button("Остаться", "continue", () -> visible(this.discard, false))));
    visible(this.discard, false);
    final VBox footer =
        new VBox(
            8,
            this.discard,
            new FlowPane(10, 6, note("Добавить как"), this.role, this.apply),
            this.status);
    footer.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(footer);
    this.controls();
  }

  private void parse() {
    if (this.running) {
      return;
    }
    final String text = this.input.getText();
    this.result = null;
    this.details.clear();
    this.reviewed.setSelected(false);
    this.input.setDisable(true);
    this.busy(true);
    this.controls();
    this.paint();
    this.task =
        new Task<>() {
          @Override
          protected OpsinChemicalName.Result call() {
            return new OpsinChemicalName().parse(text);
          }
        };
    this.task.setOnSucceeded(
        e -> {
          if (this.disposed) {
            return;
          }
          this.busy(false);
          this.input.setDisable(false);
          this.result = this.task.getValue();
          this.details.setText(
              "SMILES: " + this.result.smiles() + "\n" + String.join("\n", this.result.notes()));
          this.fit();
          this.controls();
          this.status.setText(
              "Проверьте структуру. В опыт будут добавлены структура и введённое имя.");
        });
    this.task.setOnFailed(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.input.setDisable(false);
            this.controls();
            this.status.setText(this.task.getException().getMessage());
          }
        });
    this.task.setOnCancelled(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.input.setDisable(false);
            this.controls();
            this.status.setText("Построение отменено; структура опыта не изменена.");
          }
        });
    final Thread worker = new Thread(this.task, "chemical-name-parser");
    worker.setDaemon(true);
    worker.start();
  }

  private void apply() {
    if (this.result == null || this.running) {
      return;
    }
    if (this.result.warning() && !this.reviewed.isSelected()) {
      throw new IllegalArgumentException("Проверьте предупреждение распознавания.");
    }
    if (!this.editor.document().experiment().id().equals(this.experiment)) {
      throw new IllegalArgumentException("Открыт другой опыт.");
    }
    this.editor.addPlaced(this.result.drawing(), this.role.getValue());
    this.added = true;
    this.finish();
  }

  private void controls() {
    this.apply.setDisable(
        this.result == null
            || this.running
            || this.result.warning() && !this.reviewed.isSelected());
    this.parse.setDisable(this.running);
    visible(this.stop, this.running);
    visible(this.empty, this.result == null);
    this.empty.setText(
        this.running ? "Строится структура…" : "Здесь появится структура по введённому названию.");
    visible(this.resultHeading, this.result != null);
    visible(this.viewControls, this.result != null);
    visible(this.details, this.result != null);
    this.resultHeading.setText(
        this.result == null
            ? ""
            : this.result.drawing().molecule().formula().hillNotation() + " · проверьте структуру");
    final boolean hasWarning = this.result != null && this.result.warning();
    visible(this.warning, hasWarning);
    visible(this.reviewed, hasWarning);
    this.warning.setText(hasWarning ? String.join("\n", this.result.notes()) : "");
  }

  private void fit() {
    this.zoom.setValue(1);
    this.panX = 0;
    this.panY = 0;
    this.paint();
  }

  private void paint() {
    final var g = this.picture.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(g);
    if (this.result == null) {
      return;
    }
    final var d = this.result.drawing();
    final var b = LaboratoryCanvas.previewBounds(d);
    final double scale =
        this.zoom.getValue()
            * Math.min(
                1.2,
                Math.min(
                    Math.max(1, this.picture.getWidth() - 40) / Math.max(1, b.getWidth()),
                    Math.max(1, this.picture.getHeight() - 40) / Math.max(1, b.getHeight())));
    g.save();
    g.translate(
        this.picture.getWidth() / 2 + this.panX - scale * (b.getMinX() + b.getMaxX()) / 2,
        this.picture.getHeight() / 2 + this.panY - scale * (b.getMinY() + b.getMaxY()) / 2);
    g.scale(scale, scale);
    LaboratoryPreviewTheme.paint(g, d);
    g.restore();
  }

  boolean pending() {
    return !this.added && !this.input.getText().isBlank();
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText("Дождитесь результата или остановите распознавание.");
      return false;
    }
    if (this.pending()) {
      visible(this.discard, true);
      return false;
    }
    return true;
  }

  void draft(final Properties p, final boolean restore) {
    if (!restore) {
      p.setProperty("chemicalName.text", this.input.getText());
      p.setProperty("chemicalName.role", this.role.getValue().name());
    } else {
      this.input.setText(p.getProperty("chemicalName.text", ""));
      this.role.setValue(
          ReactionComponentRole.valueOf(p.getProperty("chemicalName.role", "REACTANT")));
      this.status.setText("Название восстановлено. Постройте структуру и проверьте результат.");
    }
  }

  @Override
  void dispose() {
    if (this.task != null) {
      this.task.cancel();
    }
    super.dispose();
  }

  private Button button(final String text, final String id, final Runnable run) {
    final Button b = new Button(text);
    b.setId("chemical-name-" + id);
    b.setOnAction(
        e -> {
          try {
            run.run();
          } catch (final RuntimeException ex) {
            this.status.setText(ex.getMessage());
          }
        });
    return b;
  }

  private static void visible(final javafx.scene.Node n, final boolean value) {
    n.setVisible(value);
    n.setManaged(value);
  }
}
