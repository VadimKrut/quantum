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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySampleTransferRepository;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;

/** Review an actual specimen transfer, then reconcile its independently saved source and destination. */
final class LaboratorySampleTransferPage extends LaboratoryComputationPage {

  private final LaboratoryEditor editor;
  private final Path sourcePath;
  private final Window owner;
  private final Consumer<Path> open;
  private Runnable sourceSaved = () -> {};
  private final LaboratorySampleTransferRepository repository =
      new LaboratorySampleTransferRepository();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread thread = new Thread(r, "sample-transfer");
            thread.setDaemon(true);
            return thread;
          });
  private LaboratoryDrawing expected;
  private LaboratorySampleTransfer.Prepared reviewed;
  private volatile LaboratorySampleTransferRepository.Intent active;
  private boolean complete, changing, leaving, restored, conflict;
  private Input baseline;
  private String unavailableSample = "";
  private Path targetPath;
  private Path recoveryIntent;
  private final ComboBox<LaboratorySample> sample = new ComboBox<>();
  private final TextField amount = new TextField(), title = new TextField();
  private final ComboBox<LaboratoryQuantityUnit> unit = new ComboBox<>();
  private final Label available = note(""),
      identity = note(""),
      destination = note("Выберите новый файл .qchem"),
      phase = note("");
  private final LaboratoryStructurePreview structure = new LaboratoryStructurePreview();
  private final VBox form = new VBox(12), review = new VBox(10), recovery = new VBox(8);
  private final VBox leave = new VBox(8);
  private final Label leaveMessage = note("");
  private final Button discardInput, discardAndLeave;
  private final Button preview, perform, retry, openTarget, another;

  LaboratorySampleTransferPage(
      final LaboratoryEditor editor,
      final Path sourcePath,
      final Window owner,
      final Consumer<Path> open,
      final Runnable close) {
    super("Образец → следующий опыт", close);
    this.editor = editor;
    this.expected = editor.document();
    this.sourcePath = sourcePath.toAbsolutePath().normalize();
    this.owner = owner;
    this.open = open;
    this.setId("chemistry-sample-transfer-page");
    this.sample.setId("chemistry-sample-transfer-sample");
    this.sample.setMaxWidth(Double.MAX_VALUE);
    this.sample.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final LaboratorySample value) {
            return value == null
                ? "Выберите образец"
                : value.code() + " · " + value.balance().label();
          }

          public LaboratorySample fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.sample.valueProperty().addListener(o -> this.selected());
    this.amount.setPromptText("Сколько образца передать");
    this.amount.setId("chemistry-sample-transfer-amount");
    this.unit.setId("chemistry-sample-transfer-unit");
    this.title.setPromptText("Название следующего опыта");
    this.title.setId("chemistry-sample-transfer-title");
    this.amount.textProperty().addListener(o -> this.changed());
    this.title.textProperty().addListener(o -> this.changed());
    this.unit.valueProperty().addListener(o -> this.changed());
    this.preview = button("Проверить передачу", "preview", this::preview);
    this.perform = button("Передать образец и создать опыт", "perform", this::perform);
    this.perform.getStyleClass().add("primary-button");
    this.retry = button("Продолжить эту передачу", "retry", this::perform);
    this.openTarget =
        button(
            "Открыть новый опыт",
            "open",
            () -> {
              if (this.complete && this.active != null) {
                this.open.accept(this.active.target());
              }
            });
    this.another = button("Следующая передача", "another", this::reset);
    this.discardInput = button("Сбросить ввод", "discard", this::reset);
    this.discardAndLeave =
        button(
            "Отбросить ввод и выйти",
            "leave",
            () -> {
              if (!this.running) {
                this.leaving = true;
                this.finish();
              }
            });
    this.leave
        .getChildren()
        .setAll(
            this.leaveMessage,
            new FlowPane(
                8,
                8,
                button(
                    "Продолжить ввод",
                    "stay",
                    () -> {
                      visible(this.leave, false);
                      this.status.setText(
                          this.active == null
                              ? "Продолжайте ввод; передача ещё не записана."
                              : "Продолжите ту же передачу, чтобы проверить обе стороны.");
                    }),
                this.discardInput,
                this.discardAndLeave));
    this.leave.setPadding(new Insets(12, 20, 12, 20));
    visible(this.leave, false);
    this.setBottom(this.leave);
    final Button choose = button("Выбрать новый файл…", "choose", this::choose);
    final Button all =
        button(
            "Весь доступный остаток",
            "all",
            () -> {
              final var value = this.sample.getValue();
              if (value != null && value.balance().remaining() != null) {
                this.unit.setValue(value.balance().remaining().unit());
                this.amount.setText(value.balance().remaining().value().toPlainString());
              }
            });
    final HBox quantity = new HBox(8, this.amount, this.unit);
    HBox.setHgrow(this.amount, Priority.ALWAYS);
    this.form
        .getChildren()
        .setAll(
            heading("Передаваемый образец"),
            this.sample,
            this.available,
            quantity,
            all,
            field("Новый опыт", this.title),
            new VBox(6, choose, this.destination),
            this.preview);
    final VBox sourceCard =
        card(heading("Сохранённая структура образца"), this.structure, this.identity);
    this.structure.setPrefHeight(270);
    this.structure.hint("Снимок физического образца; он сохраняется при изменении исходной схемы.");
    final FlowPane columns = new FlowPane(16, 16, card(this.form), sourceCard);
    columns.setPrefWrapLength(1000);
    columns.prefWrapLengthProperty().bind(this.widthProperty().subtract(48));
    sourceCard.setPrefWidth(430);
    ((Region) columns.getChildren().getFirst()).setPrefWidth(430);
    this.review.setId("chemistry-sample-transfer-review");
    this.review.getStyleClass().add("laboratory-search-card");
    this.review.setPadding(new Insets(16));
    final TitledPane recoveries =
        new TitledPane("Незавершённые передачи этого опыта", this.recovery);
    recoveries.setAnimated(false);
    recoveries.setExpanded(true);
    final VBox content =
        new VBox(
            16,
            note(
                "Выберите полученный ранее образец и явно укажите количество для следующего опыта."),
            columns,
            this.review,
            new FlowPane(8, 8, this.perform, this.retry, this.openTarget, this.another),
            this.phase,
            recoveries,
            this.status);
    content.setPadding(new Insets(8, 20, 20, 20));
    content.setMinWidth(0);
    final var scroll = scroll(content);
    scroll.setFitToHeight(false);
    this.setCenter(scroll);
    this.reset();
    this.loadPending();
  }

  void sourceSaved(final Runnable callback) {
    this.sourceSaved = callback;
  }

  private Button button(final String label, final String id, final Runnable action) {
    final Button button = new Button(label);
    button.setId("chemistry-sample-transfer-" + id);
    button.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final Exception failure) {
            this.status.setText(message(failure));
          }
        });
    return button;
  }

  private static Label heading(final String text) {
    final Label label = note(text);
    label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    return label;
  }

  private static VBox field(final String label, final Node value) {
    return new VBox(5, note(label), value);
  }

  private static VBox card(final Node... nodes) {
    final VBox card = new VBox(10, nodes);
    card.setMinWidth(0);
    card.setPadding(new Insets(16));
    card.getStyleClass().add("laboratory-search-card");
    return card;
  }

  private static String message(final Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }

  void selectSample(final String sampleId) {
    if (sampleId == null) return;
    if (this.running
        || this.active != null
        || this.reviewed != null
        || this.targetPath != null
        || !this.amount.getText().isBlank()
        || !this.title
            .getText()
            .equals(this.expected.experiment().title() + " — следующая стадия")) {
      throw new IllegalStateException(
          "Сначала завершите или сбросьте текущую передачу; введённые данные сохранены.");
    }
    final var chosen =
        this.sample.getItems().stream()
            .filter(s -> s.id().equals(sampleId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Выбранный образец недоступен или его остаток не известен."));
    this.sample.setValue(chosen);
    this.baseline = this.input();
  }

  private void reset() {
    if (this.running) {
      return;
    }
    if (this.hasIntent() && !this.complete) {
      throw new IllegalArgumentException(
          "Передача уже подготовлена. Продолжите её либо оставьте восстановление и выйдите; записанный расход нельзя сбросить.");
    }
    if (!this.expected.experiment().id().equals(this.editor.document().experiment().id())) {
      throw new IllegalArgumentException(
          "Открыт другой опыт. Ввод сохранён; выйдите и откройте передачу из нужного опыта.");
    }
    this.changing = true;
    this.expected = this.editor.document();
    this.active = null;
    this.recoveryIntent = null;
    this.reviewed = null;
    this.complete = false;
    this.targetPath = null;
    this.conflict = false;
    this.unavailableSample = "";
    this.leaving = false;
    this.sample
        .getItems()
        .setAll(
            this.expected.experiment().samples().stream()
                .filter(
                    s ->
                        s.balance().status() == LaboratorySample.Status.AVAILABLE
                            && s.balance().remaining() != null
                            && s.balance().remaining().value().signum() > 0)
                .toList());
    this.sample.getSelectionModel().selectFirst();
    this.amount.clear();
    this.title.setText(this.expected.experiment().title() + " — следующая стадия");
    this.destination.setText("Выберите новый файл .qchem");
    this.review.getChildren().clear();
    this.phase.setText("");
    this.changing = false;
    this.selected();
    this.baseline = this.input();
    visible(this.leave, false);
    this.controls();
    this.status.setText(
        this.sample.getItems().isEmpty()
            ? "В этом опыте нет доступных образцов с известным остатком. Зарегистрируйте образец или уточните его количество."
            : "Источник: " + this.sourcePath);
  }

  private void selected() {
    if (this.changing) {
      return;
    }
    this.unavailableSample = "";
    final var selected = this.sample.getValue();
    if (selected == null) {
      this.available.setText("Нет доступного образца");
      this.identity.setText("");
      this.structure.show(null, null);
      return;
    }
    final var balance = selected.balance();
    this.changing = true;
    this.unit
        .getItems()
        .setAll(
            java.util.Arrays.stream(LaboratoryQuantityUnit.values())
                .filter(u -> u.dimension().equals(balance.remaining().unit().dimension()))
                .toList());
    this.unit.setValue(balance.remaining().unit());
    this.amount.clear();
    this.changing = false;
    this.sampleDetails();
    this.changed();
  }

  private void sampleDetails() {
    final var selected = this.sample.getValue();
    if (selected == null) {
      this.available.setText(
          this.unavailableSample.isBlank()
              ? "Нет доступного образца"
              : "Образец из черновика недоступен: " + this.unavailableSample);
      this.identity.setText("");
      this.structure.show(null, null);
      return;
    }
    this.available.setText("Доступно: " + selected.balance().remaining());
    this.available.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
    final long analyses =
        this.expected.experiment().analyses().stream()
                .filter(a -> selected.id().equals(a.sampleId()))
                .count()
            + this.expected.experiment().analyticalEvidence().entries().stream()
                .filter(a -> selected.id().equals(a.sampleId()))
                .count();
    this.identity.setText(
        selected.molecule().displayName()
            + " · "
            + selected.molecule().formula().hillNotation()
            + "\n"
            + selected.kind()
            + " · собственных анализов: "
            + analyses
            + (selected.origin() != null || !selected.parents().isEmpty()
                ? " · есть аналитическое происхождение"
                : "")
            + (selected.balance().location().isBlank()
                ? ""
                : "\nХранение: " + selected.balance().location()));
    this.structure.show(new LaboratoryMoleculePlacement().place(selected.molecule(), 0, 0), null);
  }

  /** Refresh confirmed balance without changing the reviewed amount or creating another transfer. */
  private void refreshAvailable() {
    final String id =
        this.active != null
            ? this.active.received().experiment().samples().getFirst().origin().source().sampleId()
            : this.sample.getValue() == null ? this.unavailableSample : this.sample.getValue().id();
    this.changing = true;
    try {
      this.sample
          .getItems()
          .setAll(
              this.expected.experiment().samples().stream()
                  .filter(s -> s.balance().remaining() != null)
                  .toList());
      this.sample.setValue(
          this.sample.getItems().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null));
      this.unavailableSample = this.sample.getValue() == null ? id : "";
    } finally {
      this.changing = false;
    }
    this.sampleDetails();
  }

  private void changed() {
    if (!this.changing && this.active == null) {
      this.reviewed = null;
      this.review.getChildren().clear();
      this.controls();
    }
  }

  private void choose() {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Новый опыт из образца");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Опыт Quantum (*.qchem)", "*.qchem"));
    if (Files.isDirectory(this.sourcePath.getParent())) {
      chooser.setInitialDirectory(this.sourcePath.getParent().toFile());
    }
    chooser.setInitialFileName("Следующий опыт.qchem");
    final var selected = chooser.showSaveDialog(this.owner);
    if (selected == null) {
      return;
    }
    final Path path = selected.toPath().toAbsolutePath().normalize();
    if (Files.exists(path)) {
      throw new IllegalArgumentException(
          "Файл уже существует. Выберите новое имя: существующий опыт не будет перезаписан.");
    }
    if (!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".qchem")) {
      throw new IllegalArgumentException("Имя нового файла должно оканчиваться на .qchem.");
    }
    this.targetPath = path;
    this.destination.setText(path.toString());
    this.changed();
  }

  private void preview() {
    if (this.conflict
        || !this.editor.document().equals(this.expected)
        || this.editor.dirty() && !this.restored) {
      throw new IllegalArgumentException(
          "Сначала сохраните изменённый опыт и откройте передачу заново. Введённые данные остаются в черновике.");
    }
    if (this.sample.getValue() == null || this.unit.getValue() == null) {
      throw new IllegalArgumentException("Выберите доступный образец.");
    }
    if (this.targetPath == null) {
      throw new IllegalArgumentException("Выберите новый файл опыта.");
    }
    final var quantity =
        new LaboratoryQuantity(
            new BigDecimal(this.amount.getText().strip().replace(',', '.')), this.unit.getValue());
    this.reviewed =
        LaboratorySampleTransfer.prepare(
            this.expected,
            this.sample.getValue().id(),
            quantity,
            this.title.getText(),
            this.sourcePath.toString(),
            Instant.now());
    this.showReview(
        this.reviewed.sourceBefore(),
        this.reviewed.sourceAfter(),
        this.reviewed.target(),
        this.targetPath,
        this.reviewed.warnings());
    this.controls();
    this.status.setText("Проверьте обе стороны передачи перед записью.");
  }

  private void showReview(
      final LaboratoryDrawing before,
      final LaboratoryDrawing after,
      final LaboratoryDrawing target,
      final Path path,
      final List<String> warnings) {
    final var received = target.experiment().samples().getFirst();
    final var origin = received.origin();
    final var source =
        LaboratorySampleService.sample(before.experiment(), origin.source().sampleId());
    final var left = LaboratorySampleService.sample(after.experiment(), source.id());
    final VBox from =
        card(
            heading("Исходный опыт"),
            note(before.experiment().title() + "\nОбразец «" + source.code() + "»"),
            note("Доступно до передачи: " + source.balance().remaining()),
            note("Передать: " + received.events().getFirst().quantity()),
            note("Останется: " + left.balance().remaining()));
    final VBox to =
        card(
            heading("Новый опыт"),
            note(target.experiment().title()),
            note("Получить образец: " + received.events().getFirst().quantity()),
            note("Исходник в плане: " + target.experiment().components().getFirst().planned()),
            note("Фактическая загрузка реакции: не назначена"));
    from.setPrefWidth(380);
    to.setPrefWidth(380);
    final FlowPane pair = new FlowPane(12, 12, from, to);
    pair.prefWrapLengthProperty().bind(this.widthProperty().subtract(88));
    this.review
        .getChildren()
        .setAll(
            heading("Проверка передачи"),
            pair,
            note("Файл нового опыта: " + path),
            note(
                "Сохраняется источник и ссылки на "
                    + origin.analysisIds().size()
                    + " собственных анализов исходного образца. Анализы его родителей доступны через цепочку происхождения; это не измерения нового образца. Данные и вложения остаются в исходных опытах."));
    warnings.forEach(w -> this.review.getChildren().add(note(w)));
    this.review
        .getChildren()
        .add(
            note(
                "Сначала резервируется новый файл, затем записывается расход и получение. При сбое запись восстановления позволяет продолжить эту же передачу без повторного списания."));
  }

  private void perform() {
    if (this.running || this.complete) {
      return;
    }
    if (this.conflict
        || !this.editor.document().equals(this.expected)
        || this.editor.dirty() && !this.restored) {
      throw new IllegalArgumentException(
          "Открытый опыт изменился. Сохраните его и заново откройте передачу; незавершённая операция будет доступна для продолжения.");
    }
    if (this.active == null && this.reviewed == null) {
      throw new IllegalArgumentException("Сначала проверьте передачу.");
    }
    this.busy(true);
    this.controls();
    this.phase.setText("Сохраняю передачу. Дождитесь подтверждения обеих сторон.");
    final var prepared = this.reviewed;
    final Path destination = this.targetPath;
    this.worker.submit(
        () -> {
          LaboratorySampleTransferRepository.Progress progress = null;
          Exception failure = null;
          try {
            if (this.active == null) {
              this.active = this.repository.stage(this.sourcePath, destination, prepared);
            }
            progress = this.repository.resume(this.active);
          } catch (final LaboratorySampleTransferRepository.TransferException error) {
            this.active = error.intent();
            progress = error.progress();
            failure = error;
          } catch (final Exception error) {
            failure = error;
            if (this.active != null) {
              progress = this.repository.inspect(this.active);
            }
          }
          final var result = progress;
          final var error = failure;
          Platform.runLater(
              () -> {
                this.accept(result, error);
              });
        });
  }

  private void accept(
      final LaboratorySampleTransferRepository.Progress result, final Exception error) {
    if (this.disposed) {
      return;
    }
    this.busy(false);
    this.complete = result != null && result.complete();
    this.phase.setText(
        result == null ? "Документы передачи не изменялись этой попыткой." : result.summary());
    if (result != null
        && result.sourceSaved() == LaboratorySampleTransferRepository.Saved.YES
        && result.sourceDrawing() != null) {
      final boolean current = !this.conflict && this.editor.document().equals(this.expected);
      this.expected = result.sourceDrawing();
      this.refreshAvailable();
      if (!current) {
        this.conflict = true;
        this.status.setText(
            "Расход сохранён в файле, но открытый опыт изменился. Его ввод не заменён; откройте актуальный исходник после сохранения своих правок.");
        this.controls();
        return;
      }
      try {
        this.sourceSaved.run();
        this.restored = false;
      } catch (final RuntimeException refresh) {
        this.status.setText(
            "Расход записан, но обновить открытый опыт не удалось: " + message(refresh));
        this.controls();
        return;
      }
    }
    this.status.setText(
        this.complete
            ? "Передача сохранена. Новый опыт готов к дальнейшему планированию."
            : error != null
                ? message(error)
                : "Передача не завершена. Проверьте состояние обеих сторон и продолжите эту же операцию.");
    this.controls();
    if (this.complete) {
      visible(this.leave, false);
      this.loadPending();
    }
  }

  private void loadPending() {
    if (this.disposed) {
      return;
    }
    this.worker.submit(
        () -> {
          try {
            final var pending = this.repository.pending(this.sourcePath);
            Platform.runLater(
                () -> {
                  if (this.disposed) {
                    return;
                  }
                  this.recovery.getChildren().clear();
                  if (pending.isEmpty()) {
                    this.recovery.getChildren().add(note("Незавершённых передач нет."));
                  }
                  for (final var intent : pending) {
                    final Button resume =
                        button(
                            "Проверить и продолжить",
                            "resume-" + intent.id(),
                            () -> {
                              if (this.running) {
                                return;
                              }
                              if (this.pending()
                                  && (this.active == null
                                      || !this.active.id().equals(intent.id()))) {
                                this.status.setText(
                                    "Сначала сбросьте текущий ввод или продолжите его; сохранённая передача не заменит черновик.");
                                this.canClose();
                                return;
                              }
                              this.active = intent;
                              this.reviewed = null;
                              this.complete = false;
                              this.targetPath = intent.target();
                              this.showReview(
                                  intent.before(),
                                  intent.after(),
                                  intent.received(),
                                  intent.target(),
                                  List.of());
                              this.phase.setText(
                                  "Это сохранённая ранее операция. Продолжение проверит обе стороны и не спишет образец повторно.");
                              this.controls();
                            });
                    this.recovery
                        .getChildren()
                        .add(
                            card(
                                note(
                                    intent.received().experiment().title()
                                        + "\n"
                                        + intent.target()),
                                resume));
                  }
                });
          } catch (final Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed) {
                    this.recovery
                        .getChildren()
                        .setAll(
                            note("Не удалось прочитать восстановление передач: " + message(error)));
                  }
                });
          }
        });
  }

  private void controls() {
    final boolean locked = this.running || this.hasIntent();
    this.form.getChildren().forEach(node -> node.setDisable(locked && node != this.available));
    this.recovery.setDisable(this.running);
    visible(this.perform, !this.hasIntent() && this.reviewed != null);
    this.perform.setDisable(this.running);
    visible(this.retry, this.active != null && !this.complete);
    this.retry.setDisable(this.running);
    visible(this.openTarget, this.complete);
    visible(this.another, this.complete);
    visible(this.review, this.reviewed != null || this.active != null);
    this.discardInput.setDisable(this.running || this.hasIntent() && !this.complete);
  }

  private record Input(String sample, String amount, String unit, String title, String target) {
    void write(final Properties fields, final String prefix) {
      fields.setProperty(prefix + "sample", this.sample);
      fields.setProperty(prefix + "amount", this.amount);
      fields.setProperty(prefix + "unit", this.unit);
      fields.setProperty(prefix + "title", this.title);
      fields.setProperty(prefix + "target", this.target);
    }

    static Input read(final Properties fields, final String prefix) {
      return new Input(
          fields.getProperty(prefix + "sample", ""),
          fields.getProperty(prefix + "amount", ""),
          fields.getProperty(prefix + "unit", ""),
          fields.getProperty(prefix + "title", ""),
          fields.getProperty(prefix + "target", ""));
    }
  }

  private Input input() {
    return new Input(
        this.sample.getValue() == null ? this.unavailableSample : this.sample.getValue().id(),
        this.amount.getText(),
        this.unit.getValue() == null ? "" : this.unit.getValue().name(),
        this.title.getText(),
        this.targetPath == null ? "" : this.targetPath.toString());
  }

  private boolean hasIntent() {
    return this.active != null || this.recoveryIntent != null;
  }

  boolean pending() {
    return !this.leaving
        && !this.complete
        && (this.hasIntent()
            || this.conflict
            || this.baseline != null && !this.baseline.equals(this.input()));
  }

  void draft(final Properties fields, final boolean restore) {
    final String prefix = "sampleTransfer.";
    if (!restore) {
      fields.setProperty(prefix + "source", this.sourcePath.toString());
      fields.setProperty(prefix + "snapshot", new LaboratoryFileCodec().write(this.expected));
      this.input().write(fields, prefix);
      (this.baseline == null ? this.input() : this.baseline).write(fields, prefix + "baseline.");
      if (this.hasIntent()) {
        fields.setProperty(
            prefix + "intent",
            (this.active == null ? this.recoveryIntent : this.active.directory()).toString());
      }
      return;
    }
    if (!this.sourcePath.equals(
        Path.of(fields.getProperty(prefix + "source")).toAbsolutePath().normalize())) {
      throw new IllegalArgumentException("Черновик передачи относится к другому исходному файлу.");
    }
    final LaboratoryDrawing snapshot =
        new LaboratoryFileCodec().read(fields.getProperty(prefix + "snapshot"));
    if (!snapshot.experiment().id().equals(this.editor.document().experiment().id())) {
      throw new IllegalArgumentException("Черновик передачи относится к другому опыту.");
    }
    final Input raw = Input.read(fields, prefix);
    final String intent = fields.getProperty(prefix + "intent", "");
    this.expected = snapshot;
    this.conflict = !snapshot.equals(this.editor.document());
    this.restored = true;
    this.reviewed = null;
    this.active = null;
    this.complete = false;
    this.leaving = false;
    this.changing = true;
    try {
      this.sample
          .getItems()
          .setAll(
              snapshot.experiment().samples().stream()
                  .filter(s -> s.balance().remaining() != null)
                  .toList());
      this.sample.setValue(
          this.sample.getItems().stream()
              .filter(s -> s.id().equals(raw.sample()))
              .findFirst()
              .orElse(null));
      this.unavailableSample = this.sample.getValue() == null ? raw.sample() : "";
      if (!this.unavailableSample.isBlank()) {
        this.conflict = true;
      }
      final var selected = this.sample.getValue();
      this.unit
          .getItems()
          .setAll(
              java.util.Arrays.stream(LaboratoryQuantityUnit.values())
                  .filter(
                      u ->
                          selected == null
                              || u.dimension()
                                  .equals(selected.balance().remaining().unit().dimension()))
                  .toList());
      this.unit.setValue(raw.unit().isBlank() ? null : LaboratoryQuantityUnit.valueOf(raw.unit()));
      if (this.unit.getValue() != null && !this.unit.getItems().contains(this.unit.getValue())) {
        this.conflict = true;
      }
      this.amount.setText(raw.amount());
      this.title.setText(raw.title());
      this.targetPath =
          raw.target().isBlank() ? null : Path.of(raw.target()).toAbsolutePath().normalize();
      this.destination.setText(
          this.targetPath == null ? "Выберите новый файл .qchem" : this.targetPath.toString());
      this.baseline = Input.read(fields, prefix + "baseline.");
      this.recoveryIntent = intent.isBlank() ? null : Path.of(intent).toAbsolutePath().normalize();
    } finally {
      this.changing = false;
    }
    this.sampleDetails();
    this.controls();
    this.status.setText(
        this.conflict
            ? "Исходный опыт отличается от черновика. Ввод сохранён; перед записью откройте актуальный исходник и проверьте передачу заново."
            : "Ввод восстановлен без записи передачи. Проверьте количество и новый файл, затем нажмите «Проверить передачу».");
    if (this.recoveryIntent == null) {
      return;
    }
    final Path savedIntent = this.recoveryIntent;
    this.busy(true);
    this.controls();
    this.worker.submit(
        () -> {
          try {
            final var loaded = this.repository.read(savedIntent, this.sourcePath);
            final var progress = this.repository.inspect(loaded);
            Platform.runLater(
                () -> {
                  if (this.disposed) {
                    return;
                  }
                  this.active = loaded;
                  this.targetPath = loaded.target();
                  this.destination.setText(loaded.target().toString());
                  this.showReview(
                      loaded.before(),
                      loaded.after(),
                      loaded.received(),
                      loaded.target(),
                      List.of());
                  this.accept(
                      progress,
                      progress.complete()
                          ? null
                          : new IllegalStateException(
                              "Передача восстановлена. Продолжите ту же операцию; уже сохранённое количество повторно не спишется."));
                });
          } catch (final Exception error) {
            Platform.runLater(
                () -> {
                  if (!this.disposed) {
                    this.busy(false);
                    this.status.setText(
                        "Не удалось прочитать сохранённую передачу: " + message(error));
                    this.controls();
                  }
                });
          }
        });
  }

  private static void visible(final Node node, final boolean value) {
    node.setVisible(value);
    node.setManaged(value);
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText(
          "Передача сохраняется. Дождитесь результата; обе стороны будут показаны отдельно.");
      return false;
    }
    if (this.pending()) {
      this.leaveMessage.setText(
          !this.hasIntent()
              ? "Количество, название или файл следующего опыта ещё не записаны. Продолжить ввод или отбросить его?"
              : "Передача не завершена. Уже записанные расход и получение не отменяются выходом; эта же операция останется в восстановлении.");
      this.discardAndLeave.setText(
          !this.hasIntent() ? "Отбросить ввод и выйти" : "Оставить восстановление и выйти");
      visible(this.leave, true);
      this.controls();
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.worker.shutdown();
  }
}
