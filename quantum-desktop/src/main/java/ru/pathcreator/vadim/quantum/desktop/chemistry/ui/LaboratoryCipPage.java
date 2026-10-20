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
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkCipCalculator;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkCipCalculator.Source;

/** Explicit source selection prevents a newly inferred drawing configuration replacing a declaration silently. */
final class LaboratoryCipPage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final String component;
  private final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing
      expected;
  private final MoleculeDrawing drawing;
  private final ComboBox<Source> source = new ComboBox<>();
  private final Canvas picture = new Canvas(650, 360);
  private final ListView<String> labels = new ListView<>();
  private final Label warnings = note("");
  private final Button apply;
  private Task<CdkCipCalculator.Result> task;
  private CdkCipCalculator.Result result;

  LaboratoryCipPage(final LaboratoryEditor editor, final String component, final Runnable close) {
    super("Расчёт R/S и E/Z", close);
    this.editor = editor;
    this.component = component;
    this.expected = editor.document();
    this.drawing =
        Objects.requireNonNull(
            this.expected.layouts().get(component), "Выберите вещество для расчёта CIP.");
    this.setId("chemistry-cip-page");
    this.source.getItems().setAll(Source.values());
    this.source.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final Source s) {
            return s == null
                ? ""
                : s == Source.STORED_CONFIGURATION
                    ? "Сохранённая конфигурация импорта"
                    : "Клинья и связи текущего 2D-рисунка";
          }

          public Source fromString(final String s) {
            throw new UnsupportedOperationException();
          }
        });
    this.source.setValue(
        this.drawing
                .molecule()
                .metadata()
                .attributes()
                .containsKey(Stereochemistry.ORDERED_PARITY_ATTRIBUTE)
            ? Source.STORED_CONFIGURATION
            : Source.DRAWING_2D);
    this.source.setId("cip-source");
    final StackPane image = new StackPane(this.picture);
    this.picture.setManaged(false);
    image.setMinWidth(0);
    image.setPrefWidth(650);
    image.setMinHeight(240);
    image.setPrefHeight(360);
    image.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 10;");
    LaboratoryPreviewTheme.track(this.picture, this::paint);
    this.picture.widthProperty().bind(image.widthProperty());
    this.picture.heightProperty().bind(image.heightProperty());
    this.picture.widthProperty().addListener(o -> this.paint());
    this.picture.heightProperty().addListener(o -> this.paint());
    this.labels.setPrefHeight(130);
    this.labels.setPlaceholder(note("Вычисленные дескрипторы появятся после расчёта."));
    this.labels.setId("cip-labels");
    this.apply = this.button("Сохранить вычисленные дескрипторы", "apply", this::apply);
    this.apply.setDisable(true);
    final VBox body =
        new VBox(
            12,
            note(this.drawing.molecule().displayName()),
            note(
                "Выберите источник конфигурации. Расчётные метки сохраняются с подписью исходной структуры и рисунка; ранее введённые стереозаписи сохраняют своё происхождение."),
            new FlowPane(
                8,
                6,
                this.source,
                this.button("Рассчитать CIP", "calculate", this::calculate),
                this.button(
                    "Остановить",
                    "stop",
                    () -> {
                      if (this.task != null) {
                        this.task.cancel();
                      }
                    })),
            image,
            this.labels,
            this.warnings);
    body.setPadding(new Insets(12, 16, 8, 16));
    VBox.setVgrow(image, Priority.ALWAYS);
    body.setMinWidth(0);
    body.setMinHeight(Region.USE_PREF_SIZE);
    final var bodyScroll = scroll(body);
    bodyScroll.setFitToHeight(false);
    this.setCenter(bodyScroll);
    final VBox footer = new VBox(8, this.apply, this.status);
    footer.setPadding(new Insets(10, 16, 14, 16));
    this.setBottom(footer);
    this.source
        .valueProperty()
        .addListener(
            o -> {
              this.result = null;
              this.apply.setDisable(true);
              this.labels.getItems().clear();
              this.paint();
            });
    try {
      final var saved = MolecularCipLabels.read(this.drawing.molecule());
      if (saved.isPresent()) {
        final var value = saved.get();
        final boolean current =
            value
                .inputSignature()
                .equals(CdkCipCalculator.signature(this.drawing, this.source.getValue()));
        if (current) {
          this.result =
              new CdkCipCalculator.Result(
                  value, List.of("Показан сохранённый расчёт: " + value.method()));
          this.render();
        } else {
          this.status.setText(
              "Сохранённый расчёт относится к другой версии структуры или источнику конфигурации. Выполните новый расчёт.");
        }
      }
    } catch (final RuntimeException ex) {
      this.status.setText(ex.getMessage());
    }
    this.paint();
  }

  private void calculate() {
    if (this.running) {
      return;
    }
    final Source selected = this.source.getValue();
    this.result = null;
    this.apply.setDisable(true);
    this.source.setDisable(true);
    this.busy(true);
    this.task =
        new Task<>() {
          @Override
          protected CdkCipCalculator.Result call() {
            return new CdkCipCalculator().calculate(drawing, selected);
          }
        };
    this.task.setOnSucceeded(
        e -> {
          if (this.disposed) {
            return;
          }
          this.busy(false);
          this.source.setDisable(false);
          this.result = this.task.getValue();
          this.render();
          this.apply.setDisable(this.result.labels().labels().isEmpty());
          this.status.setText("Расчёт завершён. Просмотрите найденные дескрипторы и ограничения.");
        });
    this.task.setOnFailed(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.source.setDisable(false);
            this.status.setText(this.task.getException().getMessage());
          }
        });
    this.task.setOnCancelled(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.source.setDisable(false);
            this.status.setText("Расчёт отменён.");
          }
        });
    final Thread worker = new Thread(this.task, "laboratory-cip");
    worker.setDaemon(true);
    worker.start();
  }

  private void render() {
    this.labels
        .getItems()
        .setAll(
            this.result.labels().labels().stream()
                .map(
                    l ->
                        atom(l.first())
                            + (l.second() == null ? "" : " — " + atom(l.second()))
                            + "   "
                            + l.descriptor())
                .toList());
    this.warnings.setText(String.join("\n", this.result.warnings()));
    this.paint();
  }

  private String atom(final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId id) {
    final var atoms = this.drawing.molecule().atoms();
    for (int i = 0; i < atoms.size(); i++) {
      if (atoms.get(i).id().equals(id)) {
        return atoms.get(i).element().symbol().value() + " · " + (i + 1);
      }
    }
    return id.value();
  }

  private void apply() {
    if (this.result == null || this.running) {
      return;
    }
    if (!this.editor.document().equals(this.expected)) {
      throw new IllegalArgumentException(
          "Опыт изменился. Повторите расчёт для актуальной структуры.");
    }
    final var component =
        this.expected.experiment().components().stream()
            .filter(c -> c.id().equals(this.component))
            .findFirst()
            .orElseThrow();
    final var molecule = this.result.labels().apply(this.drawing.molecule());
    this.editor.replace(
        this.component,
        MoleculeDrawing.of(
            molecule, this.drawing.atoms(), this.drawing.bonds(), this.drawing.manualFields()),
        component.role());
    this.editor.record(
        ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry.Kind.ACTION,
        "Сохранены вычисленные дескрипторы CIP: " + this.result.labels().method(),
        null);
    this.finish();
  }

  private void paint() {
    final var g = this.picture.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(g);
    final var b = LaboratoryCanvas.previewBounds(this.drawing);
    final double
        scale =
            Math.min(
                1.2,
                Math.min(
                    Math.max(1, this.picture.getWidth() - 60) / Math.max(1, b.getWidth()),
                    Math.max(1, this.picture.getHeight() - 60) / Math.max(1, b.getHeight()))),
        tx = this.picture.getWidth() / 2 - scale * (b.getMinX() + b.getMaxX()) / 2,
        ty = this.picture.getHeight() / 2 - scale * (b.getMinY() + b.getMaxY()) / 2;
    g.save();
    g.translate(tx, ty);
    g.scale(scale, scale);
    LaboratoryPreviewTheme.paint(g, this.drawing, this.result == null);
    g.restore();
    if (this.result == null) {
      return;
    }
    g.setFill(
        LaboratoryPreviewTheme.palette(this.picture).dark()
            ? LaboratoryPreviewTheme.DARK.stereo(false)
            : Color.web("#8c3c73"));
    g.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
    for (final var l : this.result.labels().labels()) {
      final var first =
          this.drawing.atoms().stream()
              .filter(a -> a.atomId().equals(l.first()))
              .findFirst()
              .orElseThrow()
              .point2D();
      final var second =
          l.second() == null
              ? first
              : this.drawing.atoms().stream()
                  .filter(a -> a.atomId().equals(l.second()))
                  .findFirst()
                  .orElseThrow()
                  .point2D();
      g.fillText(
          l.descriptor(),
          tx + scale * (first.x() + second.x()) / 2 + 10,
          ty + scale * (first.y() + second.y()) / 2 - 10);
    }
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText("Остановите расчёт или дождитесь завершения.");
      return false;
    }
    return true;
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
    b.setId("cip-" + id);
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
}
