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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.NmrProcessing;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text.QchemNmrTraceCodec;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNmrReader;

/** Import and processing are cancellable; accepted data travels through the existing analysis form. */
final class LaboratorySpectrumPane extends BorderPane {
  interface Listener {
    void changed();

    void imported(NmrTrace trace);

    void processed(NmrTrace previous, NmrTrace next, NmrProcessing.Spectrum spectrum);

    void integral(Signal signal);

    void scale(double value);
  }

  private final Listener listener;
  private final Supplier<Nucleus> nucleus;
  private final DoubleSupplier frequency;
  private final Supplier<String> solvent;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "nmr-processing");
            t.setDaemon(true);
            return t;
          });
  private Future<?> job;
  private long generation;
  private boolean busy, changing, disposed;
  private final TabPane views = new TabPane();
  private final Tab view = new Tab("Спектр"),
      input = new Tab("Источник и обработка"),
      regionsView = new Tab("Интегралы");
  private NmrTrace trace;
  private NmrProcessing.Spectrum result;
  private final LaboratorySpectrumPlot plot = new LaboratorySpectrumPlot();
  private final TextField path = new TextField();
  private final ComboBox<LaboratoryNmrReader.Format> format = new ComboBox<>();
  private final Label status =
      LaboratoryComputationPage.note(
          "Bruker FID: выберите fid/acqus. Готовый спектр: 1r/procs. CSV: ppm и intensity.");
  private final Map<String, TextField> fields = new LinkedHashMap<>();
  private final CheckBox baseline = new CheckBox("Линия по краям · по 1 % точек"),
      delay = new CheckBox("Убрать цифровую задержку");
  private final Button load, process, cancel;
  private final TitledPane settings;
  private double cursor = Double.NaN;

  LaboratorySpectrumPane(
      final Listener listener,
      final Supplier<Nucleus> nucleus,
      final DoubleSupplier frequency,
      final Supplier<String> solvent) {
    this.listener = listener;
    this.nucleus = nucleus;
    this.frequency = frequency;
    this.solvent = solvent;
    this.setId("chemistry-spectrum-pane");
    this.setMinSize(0, 0);
    this.path.setId("chemistry-spectrum-path");
    this.path.setPromptText("Путь к файлу или папке эксперимента Bruker");
    this.format.getItems().setAll(LaboratoryNmrReader.Format.values());
    this.format.setValue(LaboratoryNmrReader.Format.BRUKER_FID);
    this.format.setId("chemistry-spectrum-format");
    this.format.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final LaboratoryNmrReader.Format f) {
            return f == null
                ? ""
                : switch (f) {
                  case BRUKER_FID -> "Bruker · FID";
                  case BRUKER_SPECTRUM -> "Bruker · спектр";
                  case CSV -> "CSV · ppm / intensity";
                };
          }

          public LaboratoryNmrReader.Format fromString(final String s) {
            return null;
          }
        });
    this.load = button("Загрузить / заменить спектр", "load", this::load);
    this.process = button("Обработать", "process", this::process);
    this.cancel = button("Остановить", "cancel", this::cancel);
    final Button browse =
        button(
            "Файл…",
            "browse",
            () -> {
              final var chooser = new javafx.stage.FileChooser();
              chooser.setTitle("Выберите fid, acqus, 1r, procs или CSV");
              final var file =
                  chooser.showOpenDialog(
                      this.getScene() == null ? null : this.getScene().getWindow());
              if (file != null) {
                this.path.setText(file.toPath().toString());
              }
            });
    final HBox file = new HBox(8, this.path, browse);
    HBox.setHgrow(this.path, Priority.ALWAYS);
    final FlowPane controls = new FlowPane(10, 8);
    for (final String key : List.of("zf", "lb", "p0", "p1", "pivot", "reference", "first")) {
      final TextField input = new TextField();
      input.setId("chemistry-spectrum-" + key);
      input.setPrefColumnCount(7);
      input.textProperty().addListener((o, a, b) -> this.changed());
      this.fields.put(key, input);
      final String label =
          switch (key) {
            case "zf" -> "Точек после FFT";
            case "lb" -> "Уширение, Гц";
            case "p0" -> "Фаза 0, °";
            case "p1" -> "Фаза 1, °";
            case "pivot" -> "Опора фазы · 0…1";
            case "reference" -> "Сдвиг шкалы, ppm";
            default -> "Вес первой точки";
          };
      controls.getChildren().add(new VBox(4, LaboratoryComputationPage.note(label), input));
    }
    this.baseline.setWrapText(true);
    this.delay.setWrapText(true);
    this.baseline.selectedProperty().addListener((o, a, b) -> this.changed());
    this.delay.selectedProperty().addListener((o, a, b) -> this.changed());
    final VBox options =
        new VBox(
            8,
            controls,
            new FlowPane(
                10,
                6,
                this.delay,
                this.baseline,
                button("Ф0 −5°", "phase-minus", () -> this.phase(-5)),
                button("Ф0 +5°", "phase-plus", () -> this.phase(5))),
            LaboratoryComputationPage.note(
                "Фаза 1 меняется слева направо относительно опоры. Коррекция фона по краям допустима, только если края свободны от сигналов."));
    options.setPadding(new Insets(10));
    this.settings = new TitledPane("Обработка и шкала", options);
    this.settings.setAnimated(false);
    this.settings.setExpanded(true);
    this.settings.setId("chemistry-spectrum-settings");
    final ScrollPane topScroll =
        new ScrollPane(
            new VBox(8, file, new FlowPane(8, 6, this.format, this.load), this.settings));
    topScroll.setFitToWidth(true);
    topScroll.setMinHeight(0);
    topScroll.setMaxHeight(245);
    topScroll.setPrefHeight(140);
    topScroll.setId("chemistry-spectrum-settings-scroll");
    this.settings
        .expandedProperty()
        .addListener((o, a, b) -> topScroll.setPrefHeight(b ? 245 : 140));
    topScroll.setMaxHeight(Double.MAX_VALUE);
    this.input.setContent(topScroll);
    this.view.setContent(this.plot);
    this.views.setId("chemistry-spectrum-views");
    this.views.setMinSize(0, 0);
    this.views.getTabs().setAll(this.view, this.input, this.regionsView);
    this.views.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.setCenter(this.views);
    this.views.getSelectionModel().select(this.input);
    final ToggleButton zoom = new ToggleButton("Масштаб"), integral = new ToggleButton("Интеграл");
    final ToggleGroup modes = new ToggleGroup();
    zoom.setToggleGroup(modes);
    integral.setToggleGroup(modes);
    zoom.setSelected(true);
    integral.setId("chemistry-spectrum-integral-mode");
    modes.selectedToggleProperty().addListener((o, a, b) -> this.plot.integration(b == integral));
    for (final String key : List.of("low", "high", "target", "normal")) {
      final TextField input = new TextField();
      input.setPrefColumnCount(6);
      input.setId("chemistry-spectrum-" + key);
      this.fields.put(key, input);
      input.textProperty().addListener((o, a, b) -> this.changed());
    }
    this.fields.get("target").setText("0");
    this.fields.get("normal").setText("1");
    this.plot.callbacks(
        (low, high) -> {
          this.fields.get("low").setText(number(low));
          this.fields.get("high").setText(number(high));
          this.showArea();
          this.views.getSelectionModel().select(this.regionsView);
        },
        value -> {
          this.cursor = value;
          this.status.setText(
              "Выбран пик δ "
                  + number(value)
                  + ". Для привязки укажите его известный сдвиг и нажмите «Опорный пик».");
        });
    final FlowPane selection =
        new FlowPane(
            8,
            6,
            LaboratoryComputationPage.note("Область ppm"),
            this.fields.get("low"),
            this.fields.get("high"),
            button("+ Интеграл в таблицу", "integrate", this::integrate),
            LaboratoryComputationPage.note("Норма"),
            this.fields.get("normal"),
            button("Нормировать", "normalize", this::normalize));
    final TitledPane regions =
        new TitledPane(
            "Интегралы и опорный пик",
            new VBox(
                8,
                selection,
                new FlowPane(
                    8,
                    6,
                    this.fields.get("target"),
                    button("Опорный пик", "reference-peak", this::reference))));
    regions.setAnimated(false);
    regions.setExpanded(true);
    regions.setId("chemistry-spectrum-regions");
    final ScrollPane regionScroll = new ScrollPane(regions);
    regionScroll.setFitToWidth(true);
    regionScroll.setMinSize(0, 0);
    this.regionsView.setContent(regionScroll);
    final VBox footer =
        new VBox(
            7,
            new FlowPane(
                8,
                6,
                zoom,
                integral,
                button("Весь спектр", "fit", this.plot::fit),
                this.process,
                this.cancel,
                button("Экспорт CSV…", "export", this::export)),
            this.status);
    footer.setPadding(new Insets(8, 12, 10, 12));
    this.status.setMinHeight(0);
    this.status.setMaxHeight(36);
    final Tooltip tip = new Tooltip();
    tip.textProperty().bind(this.status.textProperty());
    this.status.setTooltip(tip);
    this.setBottom(footer);
    this.path.textProperty().addListener((o, a, b) -> this.changed());
    this.format.valueProperty().addListener((o, a, b) -> this.changed());
    this.controls();
    this.path.setOnDragOver(
        e -> {
          if (e.getDragboard().hasFiles()) {
            e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
          }
          e.consume();
        });
    this.path.setOnDragDropped(
        e -> {
          if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().size() == 1) {
            this.path.setText(e.getDragboard().getFiles().getFirst().getPath());
            e.setDropCompleted(true);
          } else {
            e.setDropCompleted(false);
          }
          e.consume();
        });
  }

  private static String number(final double value) {
    return BigDecimal.valueOf(value)
        .round(new java.math.MathContext(9))
        .stripTrailingZeros()
        .toPlainString();
  }

  private Button button(final String label, final String id, final Runnable action) {
    final Button b = new Button(label);
    b.setId("chemistry-spectrum-" + id);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException ex) {
            this.status.setText(
                Objects.toString(ex.getMessage(), "Не удалось выполнить операцию."));
          }
        });
    return b;
  }

  private void changed() {
    if (!this.changing) {
      this.listener.changed();
    }
  }

  private double value(final String key) {
    try {
      final String text = this.fields.get(key).getText().strip().replace(',', '.');
      if (text.length() > 80) {
        throw new NumberFormatException();
      }
      final double value = Double.parseDouble(text);
      if (!Double.isFinite(value)) {
        throw new NumberFormatException();
      }
      return value;
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Введите число в поле «"
              + switch (key) {
                case "zf" -> "Точек после FFT";
                case "lb" -> "Уширение";
                case "p0" -> "Фаза 0";
                case "p1" -> "Фаза 1";
                case "pivot" -> "Опора фазы";
                case "reference" -> "Сдвиг шкалы";
                case "first" -> "Вес первой точки";
                case "low", "high" -> "Границы области";
                case "target" -> "Опорный сдвиг";
                default -> "Нормировка";
              }
              + "».");
    }
  }

  private NmrTrace.Processing recipe() {
    final double zf = this.value("zf");
    if (zf != Math.rint(zf)) {
      throw new IllegalArgumentException("Число точек должно быть целым.");
    }
    return new NmrTrace.Processing(
        1,
        (int) zf,
        this.value("lb"),
        this.value("p0"),
        this.value("p1"),
        this.value("pivot"),
        this.value("reference"),
        this.baseline.isSelected(),
        this.delay.isSelected(),
        this.value("first"));
  }

  NmrTrace current() {
    if (this.busy) {
      throw new IllegalArgumentException("Дождитесь обработки спектра или остановите её.");
    }
    if (this.trace != null && !this.trace.processing().equals(this.recipe())) {
      throw new IllegalArgumentException(
          "Параметры изменены. Нажмите «Обработать» перед записью анализа.");
    }
    if (this.trace != null && this.result == null) {
      throw new IllegalArgumentException(
          "Спектр ещё не обработан. Нажмите «Обработать» перед записью анализа.");
    }
    return this.trace;
  }

  boolean hasTrace() {
    return this.trace != null;
  }

  boolean busy() {
    return this.busy;
  }

  private void controls() {
    this.load.setDisable(this.busy);
    this.process.setDisable(this.busy || this.trace == null);
    this.cancel.setDisable(!this.busy);
  }

  private void fill(final NmrTrace trace) {
    this.changing = true;
    try {
      final var p = trace.processing();
      this.fields.get("zf").setText(Integer.toString(p.zeroFill()));
      this.fields.get("lb").setText(Double.toString(p.lineBroadeningHz()));
      this.fields.get("p0").setText(Double.toString(p.phase0()));
      this.fields.get("p1").setText(Double.toString(p.phase1()));
      this.fields.get("pivot").setText(Double.toString(p.pivot()));
      this.fields.get("reference").setText(Double.toString(p.referencePpm()));
      this.fields.get("first").setText(Double.toString(p.firstPoint()));
      this.baseline.setSelected(p.baselineEdges());
      this.delay.setSelected(p.removeDelay());
      final boolean fid = trace.domain() == NmrTrace.Domain.FID;
      for (final String key : List.of("zf", "lb", "first")) {
        this.fields.get(key).setDisable(!fid);
      }
      this.delay.setDisable(!fid);
      for (final String key : List.of("p0", "p1", "pivot")) {
        this.fields.get(key).setDisable(trace.imaginary().isEmpty());
      }
    } finally {
      this.changing = false;
    }
  }

  void set(final NmrTrace value) {
    this.cancel();
    this.trace = value;
    this.views.getSelectionModel().select(value == null ? this.input : this.view);
    this.result = null;
    this.plot.data(null);
    if (value != null) {
      this.fill(value);
      this.submit(() -> value, false, false);
    }
    this.controls();
  }

  private void load() {
    final Path path = Path.of(this.path.getText().strip());
    if (this.path.getText().isBlank()) {
      throw new IllegalArgumentException("Выберите файл или папку спектра.");
    }
    final var format = this.format.getValue();
    final Nucleus nucleus = this.nucleus.get();
    final double frequency =
        format == LaboratoryNmrReader.Format.CSV ? this.frequency.getAsDouble() : 1;
    final String solvent = this.solvent.get();
    this.submit(
        () -> new LaboratoryNmrReader().read(path, format, nucleus, frequency, solvent),
        true,
        true);
  }

  private void phase(final double step) {
    if (this.trace == null) {
      throw new IllegalArgumentException("Сначала загрузите спектр.");
    }
    this.fields.get("p0").setText(Double.toString(this.value("p0") + step));
    this.process();
  }

  private void process() {
    if (this.trace == null) {
      throw new IllegalArgumentException("Сначала загрузите данные.");
    }
    final var next = this.trace.withProcessing(this.recipe());
    this.submit(() -> next, false, true);
  }

  private void submit(final Callable<NmrTrace> task, final boolean imported, final boolean notify) {
    this.cancel();
    final long ticket = ++this.generation;
    this.busy = true;
    this.controls();
    this.status.setText("Чтение и обработка…");
    this.job =
        this.worker.submit(
            () -> {
              try {
                final NmrTrace next = task.call();
                final var result = NmrProcessing.process(next);
                Platform.runLater(
                    () -> {
                      if (this.disposed || ticket != this.generation) {
                        return;
                      }
                      final var previous = this.trace;
                      this.trace = next;
                      this.result = result;
                      this.busy = false;
                      if (imported) {
                        this.fill(next);
                      }
                      this.plot.data(result);
                      if (imported) {
                        this.views.getSelectionModel().select(this.view);
                      }
                      this.controls();
                      if (notify) {
                        if (imported) {
                          this.listener.imported(next);
                        } else {
                          this.listener.processed(previous, next, result);
                        }
                        this.listener.changed();
                      }
                      this.status.setText(
                          next.format()
                              + " · "
                              + next.nucleus()
                              + " · "
                              + next.real().size()
                              + " исходных точек. Колесо — масштаб; Shift+колесо — высота; перетаскивание — область.");
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (this.disposed || ticket != this.generation) {
                        return;
                      }
                      this.busy = false;
                      this.controls();
                      this.status.setText(
                          "Спектр не изменён: "
                              + Objects.toString(e.getMessage(), e.getClass().getSimpleName()));
                    });
              }
            });
  }

  private void cancel() {
    this.generation++;
    if (this.job != null) {
      this.job.cancel(true);
    }
    this.busy = false;
    this.controls();
  }

  double area(final double low, final double high) {
    this.current();
    if (this.result == null) {
      throw new IllegalArgumentException("Нет обработанного спектра.");
    }
    return this.result.area(low, high);
  }

  private double area() {
    return this.area(this.value("low"), this.value("high"));
  }

  private void showArea() {
    try {
      this.status.setText(
          "Площадь области: "
              + number(this.area())
              + " · произвольные единицы × ppm. Число H задаётся нормировкой.");
    } catch (final RuntimeException e) {
      this.status.setText(e.getMessage());
    }
  }

  private void integrate() {
    final double area = this.area();
    if (area <= 0) {
      throw new IllegalArgumentException(
          "Интеграл неположителен. Проверьте фазу, фон и границы области.");
    }
    this.listener.integral(
        new Signal(
            BigDecimal.valueOf(this.value("low")),
            BigDecimal.valueOf(this.value("high")),
            "",
            BigDecimal.valueOf(area),
            List.of(),
            Set.of(),
            "Интеграл спектра · обработка v1",
            true));
    this.status.setText(
        "Область добавлена в таблицу сигналов. Мультиплетность и назначения задаются отдельно.");
  }

  private void normalize() {
    final double area = this.area(), n = this.value("normal");
    if (area <= 0 || n <= 0) {
      throw new IllegalArgumentException(
          "Для нормировки нужны положительный интеграл и заданное число.");
    }
    this.listener.scale(n / area);
    this.status.setText("Множитель всех интегралов установлен по выбранной области.");
  }

  private void reference() {
    this.current();
    if (!Double.isFinite(this.cursor)) {
      throw new IllegalArgumentException("Сначала щёлкните опорный пик на спектре.");
    }
    this.fields
        .get("reference")
        .setText(Double.toString(this.value("reference") + this.value("target") - this.cursor));
    this.process();
  }

  private void export() {
    this.current();
    if (this.result == null) {
      throw new IllegalArgumentException("Сначала обработайте спектр.");
    }
    final var chooser = new javafx.stage.FileChooser();
    chooser.setInitialFileName("spectrum.csv");
    chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
    final var file =
        chooser.showSaveDialog(this.getScene() == null ? null : this.getScene().getWindow());
    if (file != null) {
      try {
        this.export(file.toPath());
        this.status.setText(
            "Сохранён обработанный спектр CSV. Исходные данные и рецепт хранятся в QCHEM.");
      } catch (final java.io.IOException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }
  }

  void export(final Path file) throws java.io.IOException {
    this.current();
    if (this.result == null || !file.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new IllegalArgumentException("Выберите новый файл CSV.");
    }
    final StringBuilder text = new StringBuilder("ppm,intensity\n");
    for (int i = 0; i < this.result.real().size(); i++) {
      text.append(this.result.ppm(i)).append(',').append(this.result.real().get(i)).append('\n');
    }
    final Path target = file.toAbsolutePath(),
        temp = Files.createTempFile(target.getParent(), ".spectrum-", ".tmp");
    try {
      Files.writeString(temp, text);
      Files.move(temp, target);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  void draft(final Properties p, final boolean restore) {
    this.changing = true;
    try {
      if (!restore) {
        p.setProperty(
            "spectrum.view", Integer.toString(this.views.getSelectionModel().getSelectedIndex()));
        p.setProperty("spectrum.path", this.path.getText());
        p.setProperty("spectrum.format", this.format.getValue().name());
        this.fields.forEach((k, v) -> p.setProperty("spectrum." + k, v.getText()));
        p.setProperty("spectrum.baseline", Boolean.toString(this.baseline.isSelected()));
        p.setProperty("spectrum.delay", Boolean.toString(this.delay.isSelected()));
        if (this.trace != null) {
          p.setProperty("spectrum.trace", QchemNmrTraceCodec.encode(this.trace));
        }
      } else {
        this.set(
            p.containsKey("spectrum.trace")
                ? QchemNmrTraceCodec.decode(p.getProperty("spectrum.trace"))
                : null);
        this.path.setText(p.getProperty("spectrum.path", ""));
        this.format.setValue(
            LaboratoryNmrReader.Format.valueOf(p.getProperty("spectrum.format", "BRUKER_FID")));
        this.fields.forEach((k, v) -> v.setText(p.getProperty("spectrum." + k, v.getText())));
        this.baseline.setSelected(Boolean.parseBoolean(p.getProperty("spectrum.baseline")));
        this.delay.setSelected(Boolean.parseBoolean(p.getProperty("spectrum.delay")));
        this.views
            .getSelectionModel()
            .select(
                Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("spectrum.view", "0")))));
      }
    } finally {
      this.changing = false;
    }
  }

  void dispose() {
    this.disposed = true;
    this.cancel();
    this.worker.shutdownNow();
  }
}
