/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** A reviewed physical relocation to the project's sole stock ledger, with visible recovery state. */
final class LaboratorySampleStockPage extends LaboratoryComputationPage {
  private final LaboratoryEditor editor;
  private final Path source, ledger;
  private final Runnable sourceSaved;
  private final Consumer<String> openSolutions;
  private LaboratoryDrawing expected;
  private final LaboratorySampleStockRepository repository = new LaboratorySampleStockRepository();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final var t = new Thread(r, "sample-stock-transfer");
            t.setDaemon(true);
            return t;
          });
  private final ComboBox<LaboratorySample> sample = new ComboBox<>();
  private final ComboBox<LaboratoryQuantityUnit> unit = new ComboBox<>();
  private final ComboBox<LaboratoryMaterial.Kind> kind = new ComboBox<>();
  private final TextField amount = new TextField(),
      label = new TextField(),
      location = new TextField();
  private final Label remaining = note(""), review = note(""), phase = note("");
  private final VBox recoveries = new VBox(8), form = new VBox(12);
  private final Button preview, perform, next;
  private LaboratorySampleStockTransfer.Prepared prepared;
  private LaboratorySampleStockRepository.Intent active;
  private boolean complete, dirty, restoring;
  private final VBox leave = new VBox(8);

  LaboratorySampleStockPage(
      final LaboratoryEditor editor,
      final Path sourcePath,
      final Path projectFolder,
      final String selectedSampleId,
      final Runnable sourceSaved,
      final Consumer<String> openSolutionsForBatch,
      final Runnable close) {
    super("Образец → партия для раствора", close);
    this.editor = editor;
    this.expected = editor.document();
    this.source = sourcePath.toAbsolutePath().normalize();
    this.ledger = projectFolder.toAbsolutePath().normalize().resolve("materials.qchem");
    this.sourceSaved = sourceSaved;
    this.openSolutions = openSolutionsForBatch;
    this.setId("chemistry-sample-stock-page");
    this.sample.setId("sample-stock-source");
    this.sample.setMaxWidth(Double.MAX_VALUE);
    this.sample.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(LaboratorySample s) {
            return s == null ? "" : s.code() + " · " + s.balance().label();
          }

          public LaboratorySample fromString(String s) {
            return null;
          }
        });
    this.kind
        .getItems()
        .setAll(
            LaboratoryMaterial.Kind.SUBSTANCE,
            LaboratoryMaterial.Kind.SOLUTION,
            LaboratoryMaterial.Kind.MIXTURE,
            LaboratoryMaterial.Kind.UNKNOWN);
    this.kind.setValue(LaboratoryMaterial.Kind.SUBSTANCE);
    this.amount.setPromptText("Количество, переходящее в партию");
    this.amount.setId("sample-stock-amount");
    this.label.setPromptText("Название физической партии");
    this.label.setId("sample-stock-label");
    this.preview = button("Проверить передачу", "preview", this::preview);
    this.perform = button("Перенести количество в учёт партий", "perform", this::perform);
    this.perform.setDisable(true);
    this.perform.getStyleClass().add("primary-button");
    this.next =
        button(
            "Приготовить раствор из этой партии",
            "solution",
            () -> {
              if (this.complete) this.openSolutions.accept(this.active.batch().id());
            });
    this.next.setDisable(true);
    final var all =
        button(
            "Весь доступный остаток",
            "all",
            () -> {
              final var s = this.sample.getValue();
              if (s != null && s.balance().remaining() != null) {
                this.unit.setValue(s.balance().remaining().unit());
                this.amount.setText(s.balance().remaining().value().toPlainString());
              }
            });
    this.form
        .getChildren()
        .setAll(
            note(
                "Перенос уменьшит остаток образца и зарегистрирует ровно это количество в новой партии. После передачи расходуйте его только из учёта партий."),
            field("Исходный физический образец", this.sample),
            this.remaining,
            new FlowPane(8, 8, field("Передать", new HBox(8, this.amount, this.unit)), all),
            new FlowPane(
                12, 8, field("Название партии", this.label), field("Вид материала", this.kind)),
            field("Место хранения", this.location),
            this.preview);
    final var content =
        new VBox(
            16,
            this.form,
            this.review,
            this.perform,
            this.next,
            this.phase,
            new TitledPane("Незавершённые передачи этого опыта", this.recoveries),
            this.leave,
            note("Файл материалов: " + this.ledger),
            this.status);
    content.setPadding(new Insets(12, 20, 20, 20));
    final var sc = scroll(content);
    sc.setFitToHeight(false);
    this.setCenter(sc);
    this.sample.valueProperty().addListener(o -> this.selected());
    for (final var text : List.of(this.amount, this.label, this.location))
      text.textProperty().addListener(o -> this.changed());
    this.unit.valueProperty().addListener(o -> this.changed());
    this.kind.valueProperty().addListener(o -> this.changed());
    this.leave
        .getChildren()
        .setAll(
            note(
                "Ввод ещё не завершён. Уже записанная передача останется доступна в восстановлении; её расход нельзя отменить закрытием страницы."),
            new FlowPane(
                8,
                8,
                button(
                    "Оставить восстановление / отбросить ввод и выйти",
                    "leave",
                    () -> {
                      this.dirty = false;
                      this.complete = true;
                      this.finish();
                    }),
                button(
                    "Продолжить работу",
                    "stay",
                    () -> {
                      this.leave.setVisible(false);
                      this.leave.setManaged(false);
                    })));
    this.leave.setVisible(false);
    this.leave.setManaged(false);
    this.restoring = true;
    this.sample
        .getItems()
        .setAll(
            this.expected.experiment().samples().stream()
                .filter(
                    s ->
                        s.balance().status() == LaboratorySample.Status.AVAILABLE
                            && s.balance().remaining() != null)
                .toList());
    this.sample.getItems().stream()
        .filter(s -> s.id().equals(selectedSampleId))
        .findFirst()
        .ifPresentOrElse(
            this.sample::setValue, () -> this.sample.getSelectionModel().selectFirst());
    this.restoring = false;
    this.dirty = false;
    this.pendingTransfers();
  }

  private static VBox field(String title, javafx.scene.Node node) {
    return new VBox(5, note(title), node);
  }

  private Button button(String text, String id, Runnable action) {
    final var b = new Button(text);
    b.setId("sample-stock-" + id);
    b.setOnAction(
        e -> {
          if (this.running) return;
          try {
            action.run();
          } catch (RuntimeException error) {
            this.status.setText(error.getMessage());
          }
        });
    return b;
  }

  private void changed() {
    if (this.active != null) return;
    if (!this.restoring) this.dirty = true;
    this.prepared = null;
    this.perform.setDisable(true);
    this.review.setText("");
  }

  private void selected() {
    final var s = this.sample.getValue();
    this.amount.clear();
    this.unit.getItems().clear();
    if (s == null) {
      this.remaining.setText("Нет доступных образцов с известным остатком.");
      return;
    }
    final var q = s.balance().remaining();
    this.unit
        .getItems()
        .setAll(
            Arrays.stream(LaboratoryQuantityUnit.values())
                .filter(u -> u.dimension().equals(q.unit().dimension()))
                .toList());
    this.unit.setValue(q.unit());
    this.label.setText(s.code() + " · партия");
    this.location.setText(s.balance().location());
    this.remaining.setText(
        "Доступно: "
            + q
            + "\n"
            + s.molecule().displayName()
            + " · "
            + s.molecule().formula().hillNotation());
    this.changed();
  }

  private void preview() {
    if (this.active != null) return;
    if (!this.editor.document().equals(this.expected))
      throw new IllegalArgumentException("Опыт изменился. Откройте передачу заново.");
    if (this.sample.getValue() == null)
      throw new IllegalArgumentException("Выберите физический образец.");
    final BigDecimal value;
    try {
      value = new BigDecimal(this.amount.getText().strip().replace(',', '.'));
    } catch (NumberFormatException invalid) {
      throw new IllegalArgumentException("Укажите количество числом, например 125,5.");
    }
    final var q = new LaboratoryQuantity(value, this.unit.getValue());
    this.prepared =
        LaboratorySampleStockTransfer.prepare(
            this.expected,
            this.sample.getValue().id(),
            q,
            this.label.getText(),
            this.kind.getValue(),
            this.location.getText(),
            this.source.toString(),
            Instant.now());
    final var balance = this.sample.getValue().balance().remaining();
    final var left =
        new LaboratoryQuantity(
            balance.value().subtract(q.in(balance.unit()).value()), balance.unit());
    final long analyses =
        this.prepared.batch().origin().sourceSamples().stream()
            .mapToLong(o -> o.analysisIds().size())
            .sum();
    this.review.setText(
        "Проверьте передачу\nОбразец: "
            + this.sample.getValue().code()
            + "\nПередать: "
            + q
            + " · останется в образце: "
            + left
            + "\nНовая партия: "
            + this.prepared.batch().label()
            + "\nСсылок на исходные аналитические записи: "
            + analyses
            + ". Структура и цепочка происхождения сохраняются.\nСначала сохраняется расход образца, затем приход партии. При частичном сохранении продолжите ту же передачу.");
    this.perform.setDisable(false);
  }

  private void perform() {
    if (this.running || this.complete) return;
    if (!this.editor.document().equals(this.expected) || this.editor.dirty())
      throw new IllegalArgumentException(
          "Открытый опыт изменился. Сохраните его и откройте передачу заново; незавершённая операция останется доступна для продолжения.");
    if (this.active == null && this.prepared == null)
      throw new IllegalArgumentException("Сначала проверьте передачу.");
    final var draft = this.prepared;
    final var previous = this.active;
    this.busy(true);
    this.form.setDisable(true);
    this.perform.setDisable(true);
    this.next.setDisable(true);
    this.recoveries.setDisable(true);
    this.status.setText("Сохраняю физическую передачу…");
    this.worker.submit(
        () -> {
          LaboratorySampleStockRepository.Intent intent = previous;
          try {
            if (intent == null) intent = this.repository.stage(this.source, this.ledger, draft);
            final var savedIntent = intent;
            final var progress = this.repository.resume(intent);
            Platform.runLater(() -> this.accept(savedIntent, progress, null));
          } catch (final Exception error) {
            final var savedIntent = intent;
            final var progress =
                error instanceof LaboratorySampleStockRepository.TransferException x
                    ? x.progress()
                    : intent == null ? null : this.repository.inspect(intent);
            Platform.runLater(() -> this.accept(savedIntent, progress, error));
          }
        });
  }

  private void accept(
      LaboratorySampleStockRepository.Intent intent,
      LaboratorySampleStockRepository.Progress progress,
      Exception error) {
    if (this.disposed) return;
    this.busy(false);
    this.active = intent;
    String refreshFailure = null;
    this.recoveries.setDisable(false);
    if (progress != null) {
      this.phase.setText(progress.summary());
      this.complete = progress.complete();
      if (progress.sourceSaved() == LaboratorySampleStockRepository.Saved.YES
          && progress.source() != null) {
        this.expected = progress.source();
        try {
          this.sourceSaved.run();
        } catch (RuntimeException refresh) {
          refreshFailure =
              "Расход сохранён, но обновить открытый опыт не удалось: " + refresh.getMessage();
        }
      }
    }
    this.form.setDisable(intent != null);
    this.perform.setDisable(this.complete || intent == null && this.prepared == null);
    this.perform.setText(
        intent == null ? "Перенести количество в учёт партий" : "Продолжить эту передачу");
    this.next.setDisable(!this.complete);
    if (this.complete) this.dirty = false;
    this.status.setText(
        refreshFailure != null
            ? refreshFailure
            : error == null
                ? "Передача завершена. Дальнейший расход ведётся в партии; исходный образец повторно не списывается."
                : Objects.toString(
                    error.getMessage(), "Передача не завершена. Повторите эту операцию."));
    this.pendingTransfers();
  }

  private void pendingTransfers() {
    this.worker.submit(
        () -> {
          try {
            final var discovered = this.repository.discover(this.source);
            final var intents = discovered.intents();
            Platform.runLater(
                () -> {
                  if (this.disposed) return;
                  this.recoveries.getChildren().clear();
                  for (final var intent : intents)
                    this.recoveries
                        .getChildren()
                        .add(
                            button(
                                "Продолжить: " + intent.batch().label(),
                                "resume-" + intent.id(),
                                () -> {
                                  this.active = intent;
                                  this.complete = false;
                                  this.prepared = null;
                                  this.perform();
                                }));
                  for (final var error : discovered.errors())
                    this.recoveries.getChildren().add(note(error));
                  if (intents.isEmpty() && discovered.errors().isEmpty())
                    this.recoveries.getChildren().add(note("Незавершённых передач нет."));
                });
          } catch (Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed)
                    this.status.setText(
                        "Не удалось прочитать восстановления: " + error.getMessage());
                });
          }
        });
  }

  boolean pending() {
    return this.dirty || this.active != null && !this.complete;
  }

  void draft(final Properties data, final boolean restore) {
    final String prefix = "sampleStock.";
    if (!restore) {
      data.setProperty(prefix + "source", this.source.toString());
      data.setProperty(prefix + "ledger", this.ledger.toString());
      data.setProperty(prefix + "snapshot", new LaboratoryFileCodec().write(this.expected));
      data.setProperty(
          prefix + "sample", this.sample.getValue() == null ? "" : this.sample.getValue().id());
      data.setProperty(prefix + "amount", this.amount.getText());
      data.setProperty(
          prefix + "unit", this.unit.getValue() == null ? "" : this.unit.getValue().name());
      data.setProperty(prefix + "label", this.label.getText());
      data.setProperty(prefix + "location", this.location.getText());
      data.setProperty(prefix + "kind", this.kind.getValue().name());
      data.setProperty(prefix + "dirty", "" + this.dirty);
      if (this.active != null)
        data.setProperty(prefix + "intent", this.active.directory().toString());
      return;
    }
    if (!this.source.toString().equals(data.getProperty(prefix + "source"))
        || !this.ledger.toString().equals(data.getProperty(prefix + "ledger")))
      throw new IllegalArgumentException(
          "Черновик передачи относится к другому опыту или проекту.");
    this.expected = new LaboratoryFileCodec().read(data.getProperty(prefix + "snapshot"));
    this.restoring = true;
    try {
      this.sample
          .getItems()
          .setAll(
              this.expected.experiment().samples().stream()
                  .filter(
                      v ->
                          v.balance().status() == LaboratorySample.Status.AVAILABLE
                              && v.balance().remaining() != null)
                  .toList());
      this.sample.setValue(
          this.sample.getItems().stream()
              .filter(v -> v.id().equals(data.getProperty(prefix + "sample")))
              .findFirst()
              .orElse(null));
      this.amount.setText(data.getProperty(prefix + "amount", ""));
      final String u = data.getProperty(prefix + "unit", "");
      if (!u.isBlank()) this.unit.setValue(LaboratoryQuantityUnit.valueOf(u));
      this.label.setText(data.getProperty(prefix + "label", ""));
      this.location.setText(data.getProperty(prefix + "location", ""));
      this.kind.setValue(
          LaboratoryMaterial.Kind.valueOf(data.getProperty(prefix + "kind", "SUBSTANCE")));
      this.dirty = Boolean.parseBoolean(data.getProperty(prefix + "dirty"));
    } finally {
      this.restoring = false;
    }
    final String intent = data.getProperty(prefix + "intent");
    if (intent == null) {
      this.status.setText("Ввод восстановлен. Проверьте передачу перед записью.");
      return;
    }
    this.busy(true);
    this.form.setDisable(true);
    this.worker.submit(
        () -> {
          try {
            final var loaded = this.repository.read(Path.of(intent), this.source);
            final var progress = this.repository.inspect(loaded);
            Platform.runLater(
                () ->
                    this.accept(
                        loaded,
                        progress,
                        progress.complete()
                            ? null
                            : new IllegalStateException(
                                "Передача восстановлена. Продолжите ту же операцию; уже сохранённое количество повторно не спишется.")));
          } catch (Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed) {
                    this.busy(false);
                    this.status.setText("Восстановление не прочитано: " + error.getMessage());
                  }
                });
          }
        });
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) return false;
    if (this.pending()) {
      this.leave.setVisible(true);
      this.leave.setManaged(true);
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    if (this.running) this.worker.shutdown();
    else this.worker.shutdownNow();
  }
}
