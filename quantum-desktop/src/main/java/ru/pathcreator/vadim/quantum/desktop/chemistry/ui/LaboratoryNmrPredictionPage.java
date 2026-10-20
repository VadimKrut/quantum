/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.NmrMixtureSimulation;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.OfflineNmrPrediction;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.OfflineNmrPrediction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryExchangeFiles;

/** Local model workspace. Predictions never become measured spectra or experiment facts. */
final class LaboratoryNmrPredictionPage extends LaboratoryComputationPage {
  record Choice(String id, String name) {
    @Override
    public String toString() {
      return name;
    }
  }

  record CacheKey(String id, Nucleus nucleus, Solvent solvent) {}

  record Portion(Choice choice, double parts) {}

  record Calculated(Portion portion, Prediction prediction) {}

  record Request(List<Portion> portions, Nucleus nucleus, Solvent solvent) {}

  private record Impurity(Choice choice, CheckBox enabled, TextField parts, Label fraction) {}

  private static final String LIMITS =
      "Прогноз химических сдвигов по nmrshiftdb2. Линии показаны без J-расщепления: мультиплетность и сильная связь не рассчитаны. Смесь — наложение по заданным мольным долям, а не определение чистоты образца.";
  private final LaboratoryEditor editor;
  private final LaboratoryDrawing snapshot;
  private final ComboBox<Choice> compound = new ComboBox<>();
  private final ComboBox<Nucleus> nucleus = new ComboBox<>();
  private final ComboBox<Solvent> solvent = new ComboBox<>();
  private final TextField frequency = field("400", "nmr-model-frequency"),
      width = field("2", "nmr-model-width"),
      low = field("-1", "nmr-model-low"),
      high = field("12", "nmr-model-high");
  private final Button calculate = new Button("Рассчитать офлайн"),
      stop = new Button("Остановить"),
      saveText = new Button("Отчёт TXT…"),
      saveCsv = new Button("Данные CSV…"),
      saveSvg = new Button("Спектр SVG…");
  private final CheckBox pure = new CheckBox("Чистое вещество"),
      mixture = new CheckBox("Смесь"),
      contributions = new CheckBox("Вклад примесей"),
      sticks = new CheckBox("Штрихи");
  private final Canvas chart = new Canvas(800, 380), structure = new Canvas(260, 160);
  private final Label overview = note("Выберите вещество и нажмите «Рассчитать офлайн»."),
      cursor = note("Наведите на график, чтобы увидеть δ."),
      coverage = note(""),
      notice = note(LIMITS);
  private final VBox impurities = new VBox(9), details = new VBox(8);
  private final List<Impurity> impurityRows = new ArrayList<>();
  private final Map<CacheKey, Prediction> cache = new ConcurrentHashMap<>();
  private Task<List<Calculated>> task;
  private List<Calculated> calculated = List.of();
  private Request request;
  private Instant calculatedAt;
  private NmrMixtureSimulation.Result simulation;
  private long generation;
  private boolean initializing = true;

  LaboratoryNmrPredictionPage(LaboratoryEditor editor, String componentId, Runnable close) {
    super("ЯМР · прогноз и смеси", close);
    this.editor = Objects.requireNonNull(editor);
    this.snapshot = editor.document();
    setId("chemistry-nmr-model");
    for (var c : snapshot.experiment().components())
      if (snapshot.layouts().containsKey(c.id()))
        compound.getItems().add(new Choice(c.id(), c.molecule().displayName()));
    compound.setId("nmr-model-compound");
    compound.setMaxWidth(Double.MAX_VALUE);
    compound.setMinWidth(0);
    compound.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Choice c) {
            return c == null ? "" : c.name();
          }

          public Choice fromString(String s) {
            return null;
          }
        });
    compound.setCellFactory(l -> choiceCell());
    compound.setButtonCell(choiceCell());
    compound
        .getSelectionModel()
        .select(
            compound.getItems().stream()
                .filter(c -> c.id().equals(componentId))
                .findFirst()
                .orElse(compound.getItems().isEmpty() ? null : compound.getItems().getFirst()));
    nucleus.getItems().setAll(Nucleus.values());
    nucleus.setValue(Nucleus.PROTON);
    nucleus.setId("nmr-model-nucleus");
    nucleus.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Nucleus n) {
            return n == Nucleus.CARBON ? "¹³C" : "¹H";
          }

          public Nucleus fromString(String s) {
            return Nucleus.PROTON;
          }
        });
    solvent.getItems().setAll(Solvent.values());
    solvent.setValue(Solvent.UNSPECIFIED);
    solvent.setId("nmr-model-solvent");
    solvent.setMaxWidth(Double.MAX_VALUE);
    solvent.setMinWidth(0);
    solvent.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Solvent s) {
            if (s == null) return "";
            return switch (s) {
              case UNSPECIFIED -> "Растворитель не задан";
              case CDCL3 -> "CDCl₃";
              case DMSO_D6 -> "DMSO-d₆";
              case CD3OD -> "CD₃OD";
            };
          }

          public Solvent fromString(String s) {
            return Solvent.UNSPECIFIED;
          }
        });
    structure.setId("nmr-model-structure");
    StackPane preview = new StackPane(structure);
    preview.setMinWidth(0);
    preview.setPrefWidth(1);
    structure.widthProperty().bind(preview.widthProperty());
    structure.widthProperty().addListener(o -> paintStructure());
    LaboratoryPreviewTheme.track(structure, this::paintStructure);
    Label offline = note("ОФЛАЙН · РАСЧЁТНАЯ МОДЕЛЬ");
    offline.getStyleClass().add("nmr-model-eyebrow");
    VBox molecule =
        card(
            "Вещество",
            offline,
            compound,
            preview,
            note("Исходный рисунок и фактические результаты опыта не меняются."));
    GridPane settings = new GridPane();
    settings.setHgap(10);
    settings.setVgap(9);
    settings.addRow(0, note("Ядро"), nucleus);
    settings.addRow(1, note("Частота ядра, МГц"), frequency);
    settings.addRow(2, note("Ширина линии, Гц"), width);
    ColumnConstraints a = new ColumnConstraints(), b = new ColumnConstraints();
    a.setPercentWidth(57);
    b.setPercentWidth(43);
    settings.getColumnConstraints().setAll(a, b);
    nucleus.setMaxWidth(Double.MAX_VALUE);
    FlowPane presets = new FlowPane(6, 6);
    for (int mhz : new int[] {300, 400, 500, 600, 800, 1000}) {
      Button preset = new Button(Integer.toString(mhz));
      preset.setId("nmr-model-frequency-" + mhz);
      preset.setTooltip(
          new Tooltip(
              "Прибор "
                  + mhz
                  + " МГц по ¹H; для ¹³C — приблизительная частота ядра. Можно ввести точное значение вручную."));
      preset.setOnAction(
          e ->
              frequency.setText(
                  nucleus.getValue() == Nucleus.CARBON
                      ? String.format(Locale.ROOT, "%.1f", mhz * .25145)
                      : Integer.toString(mhz)));
      presets.getChildren().add(preset);
    }
    VBox conditions =
        card(
            "Условия просмотра",
            settings,
            note("Прибор по ¹H, МГц · или своё значение выше"),
            presets,
            note("Растворитель спектра"),
            solvent,
            note(
                "Выбор ограничивает базу сдвигов. Остаточный сигнал растворителя и обменные эффекты автоматически не добавляются."));
    VBox composition =
        card(
            "Состав смеси",
            note(
                "Основное вещество: 100 мольных частей. Добавьте примеси из структур опыта и задайте их количество относительно этих 100 частей."),
            impurities);
    VBox side = new VBox(14, molecule, conditions, composition);
    side.setMinWidth(0);
    side.setMaxWidth(Double.MAX_VALUE);
    calculate.setId("nmr-model-calculate");
    calculate.getStyleClass().add("primary-button");
    calculate.setOnAction(e -> start());
    stop.setId("nmr-model-stop");
    stop.setDisable(true);
    stop.setOnAction(e -> cancel("Расчёт остановлен. Можно изменить состав и повторить."));
    FlowPane actions = new FlowPane(8, 8, calculate, stop);
    pure.setSelected(true);
    mixture.setSelected(true);
    contributions.setSelected(true);
    pure.setId("nmr-model-show-pure");
    mixture.setId("nmr-model-show-mixture");
    contributions.setId("nmr-model-show-contributions");
    sticks.setId("nmr-model-sticks");
    for (CheckBox check : List.of(pure, mixture, contributions, sticks))
      check.selectedProperty().addListener(o -> paint());
    FlowPane legend = new FlowPane(16, 8, pure, mixture, contributions, sticks);
    pure.getStyleClass().add("nmr-model-pure");
    mixture.getStyleClass().add("nmr-model-mixture");
    chart.setId("nmr-model-chart");
    chart.setAccessibleText(
        "Расчётный ЯМР: сдвиги nmrshiftdb2 без J-расщепления. Синяя линия — чистое вещество, фиолетовая — смесь, остальные — вклады примесей.");
    StackPane plot = new StackPane(chart);
    plot.setMinWidth(0);
    plot.setPrefWidth(1);
    plot.setMinHeight(380);
    plot.setMaxHeight(380);
    chart.widthProperty().bind(plot.widthProperty());
    chart.widthProperty().addListener(o -> paint());
    LaboratoryPreviewTheme.track(chart, this::paint);
    chart.setOnMouseMoved(
        e -> {
          if (simulation != null) {
            double ppm =
                simulation.mixture().firstPpm()
                    + (e.getX() - 48)
                        / Math.max(1, chart.getWidth() - 72)
                        * (simulation.mixture().ppm(simulation.mixture().intensity().size() - 1)
                            - simulation.mixture().firstPpm());
            cursor.setText(
                String.format(
                    Locale.ROOT, "δ %.3f ppm · колесо — масштаб, «Весь спектр» — сброс", ppm));
          }
        });
    chart.setOnScroll(
        e -> {
          if (simulation == null) return;
          try {
            double lo = number(low),
                hi = number(high),
                center =
                    hi
                        - Math.max(
                                0,
                                Math.min(1, (e.getX() - 48) / Math.max(1, chart.getWidth() - 72)))
                            * (hi - lo),
                factor = e.getDeltaY() > 0 ? .8 : 1.25,
                span = Math.max(.02, Math.min(1000, (hi - lo) * factor));
            low.setText(fmt(center - span / 2));
            high.setText(fmt(center + span / 2));
            render();
            e.consume();
          } catch (RuntimeException ex) {
            status.setText(ex.getMessage());
          }
        });
    Button fit = new Button("Весь спектр");
    fit.setId("nmr-model-fit");
    fit.setOnAction(
        e -> {
          fitRange();
          render();
        });
    Button applyRange = new Button("Показать");
    applyRange.setOnAction(e -> render());
    low.setPrefColumnCount(5);
    high.setPrefColumnCount(5);
    FlowPane range =
        new FlowPane(8, 8, note("Диапазон δ:"), low, note("—"), high, note("ppm"), applyRange, fit);
    VBox spectrum =
        card("Прогноз спектра", overview, actions, legend, plot, range, cursor, coverage);
    overview.setId("nmr-model-overview");
    coverage.setId("nmr-model-coverage");
    notice.getStyleClass().add("nmr-model-notice");
    notice.setId("nmr-model-limitations");
    TitledPane assignments = new TitledPane("Сдвиги атомов, покрытие и происхождение", details);
    assignments.setAnimated(false);
    assignments.setExpanded(false);
    assignments.setId("nmr-model-assignments");
    assignments.setMinWidth(0);
    saveText.setId("nmr-model-export-txt");
    saveCsv.setId("nmr-model-export-csv");
    saveSvg.setId("nmr-model-export-svg");
    saveText.setOnAction(e -> export("txt"));
    saveCsv.setOnAction(e -> export("csv"));
    saveSvg.setOnAction(e -> export("svg"));
    exports(false);
    VBox content =
        new VBox(
            14,
            spectrum,
            notice,
            assignments,
            card(
                "Сохранить расчёт",
                new FlowPane(8, 8, saveText, saveCsv, saveSvg),
                note(
                    "Отдельные файлы с пометкой «Расчёт». Прогноз не подтверждает структуру и не заменяет экспериментальный ЯМР.")));
    content.setMinWidth(0);
    content.setMaxWidth(Double.MAX_VALUE);
    GridPane layout = new GridPane();
    layout.setHgap(18);
    layout.setVgap(18);
    layout.setPadding(new Insets(18));
    layout.setMinWidth(0);
    layout.setMaxWidth(Double.MAX_VALUE);
    Runnable arrange =
        () -> {
          layout.getChildren().clear();
          layout.getColumnConstraints().clear();
          if (getWidth() >= 900) {
            ColumnConstraints left = new ColumnConstraints(260, 280, 310),
                right = new ColumnConstraints();
            right.setHgrow(Priority.ALWAYS);
            layout.getColumnConstraints().addAll(left, right);
            layout.add(side, 0, 0);
            layout.add(content, 1, 0);
          } else {
            ColumnConstraints single = new ColumnConstraints();
            single.setHgrow(Priority.ALWAYS);
            layout.getColumnConstraints().add(single);
            layout.add(side, 0, 0);
            layout.add(content, 0, 1);
          }
          GridPane.setValignment(side, javafx.geometry.VPos.TOP);
          GridPane.setValignment(content, javafx.geometry.VPos.TOP);
          GridPane.setFillHeight(side, false);
          GridPane.setFillHeight(content, false);
        };
    widthProperty()
        .addListener(
            (o, before, after) -> {
              if ((before.doubleValue() >= 900) != (after.doubleValue() >= 900)) arrange.run();
            });
    arrange.run();
    setCenter(scroll(layout));
    VBox footer = new VBox(status);
    footer.setPadding(new Insets(10, 18, 10, 18));
    setBottom(footer);
    compound
        .valueProperty()
        .addListener(
            o -> {
              rebuildImpurities();
              paintStructure();
              invalidate();
            });
    nucleus
        .valueProperty()
        .addListener(
            o -> {
              invalidate();
              frequency.setText(nucleus.getValue() == Nucleus.CARBON ? "100.6" : "400");
            });
    solvent.valueProperty().addListener(o -> invalidate());
    frequency.setOnAction(e -> render());
    width.setOnAction(e -> render());
    frequency
        .textProperty()
        .addListener(
            o -> {
              if (!initializing) render();
            });
    width
        .textProperty()
        .addListener(
            o -> {
              if (!initializing) render();
            });
    low.textProperty()
        .addListener(
            o -> {
              if (!initializing) render();
            });
    high.textProperty()
        .addListener(
            o -> {
              if (!initializing) render();
            });
    rebuildImpurities();
    initializing = false;
    paintStructure();
    paint();
    status.setText("Все вычисления локальные. Выбранные структуры не отправляются в интернет.");
  }

  private static ListCell<Choice> choiceCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(Choice item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item.name());
        setTooltip(empty || item == null ? null : new Tooltip(item.name()));
        setMaxWidth(Double.MAX_VALUE);
      }
    };
  }

  private static TextField field(String value, String id) {
    TextField field = new TextField(value);
    field.setId(id);
    field.setPrefColumnCount(7);
    field.setMinWidth(50);
    field.setMaxWidth(Double.MAX_VALUE);
    return field;
  }

  private static VBox card(String title, javafx.scene.Node... children) {
    Label heading = note(title);
    heading.getStyleClass().add("nmr-model-card-title");
    VBox box = new VBox(10, heading);
    box.getChildren().addAll(children);
    box.getStyleClass().add("nmr-model-card");
    box.setMinWidth(0);
    box.setMaxWidth(Double.MAX_VALUE);
    return box;
  }

  private void rebuildImpurities() {
    impurityRows.clear();
    impurities.getChildren().clear();
    Choice main = compound.getValue();
    for (Choice c : compound.getItems()) {
      if (c.equals(main)) continue;
      CheckBox enabled = new CheckBox();
      enabled.setId("nmr-model-impurity-" + c.id());
      Label name = LaboratoryComponentNaming.label(c.name());
      name.setMinWidth(0);
      HBox caption = new HBox(7, enabled, name);
      HBox.setHgrow(name, Priority.ALWAYS);
      TextField parts = field("5", "nmr-model-parts-" + c.id());
      parts.setDisable(true);
      Label fraction = note("");
      Impurity row = new Impurity(c, enabled, parts, fraction);
      impurityRows.add(row);
      VBox box = new VBox(5, caption, new HBox(8, parts, note("мольных частей")), fraction);
      box.setMinWidth(0);
      impurities.getChildren().add(box);
      enabled
          .selectedProperty()
          .addListener(
              o -> {
                parts.setDisable(!enabled.isSelected());
                fractions();
                invalidate();
              });
      parts
          .textProperty()
          .addListener(
              o -> {
                fractions();
                invalidate();
              });
    }
    if (impurityRows.isEmpty())
      impurities
          .getChildren()
          .add(
              note(
                  "В опыте пока одна структура. Добавьте другую структуру, чтобы моделировать примесь."));
    fractions();
  }

  private void fractions() {
    try {
      double total = 100;
      for (Impurity i : impurityRows) if (i.enabled().isSelected()) total += number(i.parts());
      for (Impurity i : impurityRows)
        i.fraction()
            .setText(
                i.enabled().isSelected()
                    ? String.format(
                        Locale.ROOT, "%.2f мол. %% в смеси", number(i.parts()) / total * 100)
                    : "");
    } catch (RuntimeException e) {
      for (Impurity i : impurityRows)
        i.fraction().setText(i.enabled().isSelected() ? "Введите положительное число частей." : "");
    }
  }

  private void invalidate() {
    if (initializing) return;
    cancel(null);
    calculated = List.of();
    request = null;
    simulation = null;
    details.getChildren().clear();
    coverage.setText("");
    overview.setText("Состав или условия изменены. Нажмите «Рассчитать офлайн». ");
    exports(false);
    paint();
  }

  private void cancel(String text) {
    generation++;
    if (task != null) {
      task.cancel(true);
      task = null;
    }
    busy(false);
    stop.setDisable(true);
    calculate.setDisable(compound.getValue() == null);
    if (text != null) status.setText(text);
  }

  private void start() {
    try {
      if (compound.getValue() == null)
        throw new IllegalArgumentException("Сначала нарисуйте структуру.");
      double f = number(frequency), w = number(width);
      if (f <= 0 || w <= 0)
        throw new IllegalArgumentException("Частота и ширина линии должны быть положительными.");
      List<Portion> portions = new ArrayList<>();
      portions.add(new Portion(compound.getValue(), 100));
      for (Impurity i : impurityRows)
        if (i.enabled().isSelected()) {
          double p = number(i.parts());
          if (p <= 0 || p > 100000)
            throw new IllegalArgumentException(
                "Количество примеси должно быть больше нуля и не больше 100000 частей.");
          portions.add(new Portion(i.choice(), p));
        }
      if (portions.size() > 16)
        throw new IllegalArgumentException("Выберите не больше 15 примесей.");
      cancel(null);
      final long ticket = generation;
      final Request submitted =
          new Request(List.copyOf(portions), nucleus.getValue(), solvent.getValue());
      request = null;
      calculated = List.of();
      simulation = null;
      exports(false);
      paint();
      busy(true);
      calculate.setDisable(true);
      stop.setDisable(false);
      status.setText("Локальный прогноз: загрузка базы и расчёт атомных окружений…");
      Task<List<Calculated>> next =
          new Task<>() {
            @Override
            protected List<Calculated> call() throws Exception {
              List<Calculated> results = new ArrayList<>();
              for (Portion p : submitted.portions()) {
                if (isCancelled() || Thread.currentThread().isInterrupted())
                  throw new java.util.concurrent.CancellationException();
                CacheKey key =
                    new CacheKey(p.choice().id(), submitted.nucleus(), submitted.solvent());
                Prediction prediction = cache.get(key);
                if (prediction == null) {
                  prediction =
                      OfflineNmrPrediction.predict(
                          snapshot.layouts().get(p.choice().id()),
                          submitted.nucleus(),
                          submitted.solvent(),
                          () -> isCancelled() || Thread.currentThread().isInterrupted());
                  if (isCancelled()) throw new java.util.concurrent.CancellationException();
                  cache.put(key, prediction);
                }
                results.add(new Calculated(p, prediction));
              }
              return List.copyOf(results);
            }
          };
      task = next;
      next.setOnSucceeded(
          e -> {
            if (disposed || ticket != generation) return;
            task = null;
            busy(false);
            calculate.setDisable(false);
            stop.setDisable(true);
            request = submitted;
            calculated = next.getValue();
            calculatedAt = Instant.now();
            fitRange();
            showAssignments();
            render();
          });
      next.setOnFailed(
          e -> {
            if (disposed || ticket != generation) return;
            task = null;
            busy(false);
            calculate.setDisable(false);
            stop.setDisable(true);
            overview.setText("Расчёт не завершён");
            status.setText(
                "Не удалось рассчитать: "
                    + Objects.toString(
                        next.getException().getMessage(),
                        next.getException().getClass().getSimpleName()));
          });
      Thread worker = new Thread(next, "offline-nmr-workspace");
      worker.setDaemon(true);
      worker.start();
    } catch (RuntimeException e) {
      status.setText(e.getMessage());
    }
  }

  private void fitRange() {
    if (calculated.isEmpty()) {
      low.setText(nucleus.getValue() == Nucleus.CARBON ? "-10" : "-1");
      high.setText(nucleus.getValue() == Nucleus.CARBON ? "220" : "12");
      return;
    }
    DoubleSummaryStatistics stats =
        calculated.stream()
            .flatMap(c -> c.prediction().shifts().stream())
            .mapToDouble(Shift::ppm)
            .summaryStatistics();
    double margin = request.nucleus() == Nucleus.CARBON ? 10 : 1;
    if (stats.getCount() == 0) {
      low.setText(request.nucleus() == Nucleus.CARBON ? "-10" : "-1");
      high.setText(request.nucleus() == Nucleus.CARBON ? "220" : "12");
    } else {
      low.setText(fmt(Math.min(0, stats.getMin() - margin)));
      high.setText(fmt(Math.max(margin, stats.getMax() + margin)));
    }
  }

  private static double number(TextField field) {
    try {
      double value = Double.parseDouble(field.getText().trim().replace(',', '.'));
      if (!Double.isFinite(value)) throw new NumberFormatException();
      return value;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Введите число: «" + field.getText() + "».");
    }
  }

  private static String fmt(double value) {
    return String.format(Locale.ROOT, "%.3f", value);
  }

  private void render() {
    if (calculated.isEmpty() || disposed) return;
    try {
      double lo = number(low), hi = number(high);
      if (hi <= lo || hi - lo > 1000)
        throw new IllegalArgumentException(
            "Верхняя граница δ должна быть больше нижней; диапазон не более 1000 ppm.");
      List<NmrMixtureSimulation.Component> components = new ArrayList<>();
      for (Calculated c : calculated) {
        List<NmrMixtureSimulation.Line> lines =
            c.prediction().shifts().stream()
                .map(s -> new NmrMixtureSimulation.Line(s.atomId(), s.ppm(), 1))
                .toList();
        components.add(
            new NmrMixtureSimulation.Component(
                c.portion().choice().id(),
                c.portion().choice().name(),
                c.portion().parts(),
                lines));
      }
      simulation =
          NmrMixtureSimulation.simulate(
              components, number(frequency), number(width), hi, lo, 16384);
      int covered = calculated.stream().mapToInt(c -> c.prediction().shifts().size()).sum(),
          total = calculated.stream().mapToInt(c -> c.prediction().requestedAtoms()).sum();
      boolean complete = covered == total;
      overview.setText(
          (request.nucleus() == Nucleus.CARBON ? "¹³C" : "¹H")
              + " · "
              + solvent.getConverter().toString(request.solvent())
              + " · "
              + calculated.size()
              + " компонент(а) · расчёт, не измерение");
      coverage.setText(
          "Покрытие: "
              + covered
              + " из "
              + total
              + " атомов. "
              + (complete ? "" : "НЕПОЛНЫЙ ПРОГНОЗ: отсутствующие сигналы не изображены. ")
              + "Без J-расщепления."
              + (request.nucleus() == Nucleus.CARBON
                  ? " Интенсивности ¹³C условные: по одному на атом, не количественный прогноз."
                  : ""));
      boolean uncertain =
          calculated.stream()
              .flatMap(c -> c.prediction().warnings().stream())
              .anyMatch(warning -> warning.contains("менее") || warning.contains("широк"));
      if (uncertain)
        coverage.setText(
            coverage.getText()
                + " Есть приблизительные совпадения — подробности в таблице сдвигов.");
      exports(covered > 0);
      status.setText(
          current()
              ? "Прогноз готов локально. План, методика и аналитические результаты не изменены."
              : "Структуры опыта изменились. Показан расчёт по прежнему снимку; откройте страницу заново.");
      paint();
    } catch (RuntimeException ex) {
      simulation = null;
      exports(false);
      paint();
      status.setText(ex.getMessage());
    }
  }

  private void showAssignments() {
    details.getChildren().clear();
    for (Calculated c : calculated) {
      Label name =
          note(
              c.portion().choice().name()
                  + " · "
                  + c.prediction().shifts().size()
                  + " / "
                  + c.prediction().requestedAtoms()
                  + " атомов");
      name.setStyle("-fx-font-weight: bold;");
      StringBuilder rows = new StringBuilder("Атом\tδ, ppm\tМинимум базы\tМаксимум базы\n");
      for (Shift s : c.prediction().shifts())
        rows.append(s.label())
            .append(" [")
            .append(s.atomId())
            .append("]\t")
            .append(fmt(s.ppm()))
            .append('\t')
            .append(fmt(s.minPpm()))
            .append('\t')
            .append(fmt(s.maxPpm()))
            .append('\n');
      TextArea table = new TextArea(rows.toString());
      table.setEditable(false);
      table.setPrefRowCount(Math.min(10, c.prediction().shifts().size() + 2));
      table.setWrapText(false);
      details
          .getChildren()
          .addAll(
              name,
              table,
              note(
                  "Минимум и максимум — диапазон совпавших примеров базы, не доверительный интервал."));
      for (String warning : c.prediction().warnings()) details.getChildren().add(note(warning));
      TextArea provenance = new TextArea(c.prediction().provenance());
      provenance.setEditable(false);
      provenance.setWrapText(true);
      provenance.setPrefRowCount(4);
      details.getChildren().add(provenance);
    }
  }

  private void paintStructure() {
    LaboratoryPreviewTheme.clear(structure.getGraphicsContext2D());
    Choice selected = compound.getValue();
    if (selected != null) {
      var drawing = snapshot.layouts().get(selected.id());
      if (drawing != null) {
        var bounds = LaboratoryCanvas.previewBounds(drawing);
        double scale =
            Math.min(
                1.5,
                Math.min(
                    Math.max(1, structure.getWidth() - 20) / Math.max(1, bounds.getWidth()),
                    Math.max(1, structure.getHeight() - 20) / Math.max(1, bounds.getHeight())));
        var graphics = structure.getGraphicsContext2D();
        graphics.save();
        try {
          graphics.translate(
              (structure.getWidth() - bounds.getWidth() * scale) / 2 - bounds.getMinX() * scale,
              (structure.getHeight() - bounds.getHeight() * scale) / 2 - bounds.getMinY() * scale);
          graphics.scale(scale, scale);
          LaboratoryPreviewTheme.paint(graphics, drawing);
        } finally {
          graphics.restore();
        }
      }
    }
  }

  private Color color(int index) {
    boolean dark = LaboratoryPreviewTheme.palette(chart).dark();
    String[] colors =
        dark
            ? new String[] {"#80b9ff", "#d2a2ff", "#4bd1bb", "#f3bc6b", "#f58f9c", "#b8cb78"}
            : new String[] {"#2472c8", "#7c3ec8", "#138b79", "#bd7311", "#c04f67", "#73882b"};
    return Color.web(colors[Math.floorMod(index, colors.length)]);
  }

  private void paint() {
    var g = chart.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(g);
    var palette = LaboratoryPreviewTheme.palette(chart);
    double left = 48,
        right = Math.max(left + 1, chart.getWidth() - 24),
        top = 24,
        bottom = chart.getHeight() - 44;
    g.setStroke(palette.selectedBackground());
    for (int n = 0; n <= 6; n++) {
      double x = left + (right - left) * n / 6;
      g.strokeLine(x, top, x, bottom);
    }
    g.setStroke(palette.ink());
    g.strokeLine(left, bottom, right, bottom);
    g.setFill(palette.ink());
    g.fillText("δ, ppm", right - 38, bottom + 33);
    if (simulation == null) {
      g.fillText(
          running ? "Рассчитываем атомные окружения…" : "Здесь появится расчётный спектр",
          left + 14,
          (top + bottom) / 2);
      return;
    }
    var trace = simulation.mixture();
    double first = trace.firstPpm(), last = trace.ppm(trace.intensity().size() - 1);
    for (int n = 0; n <= 6; n++)
      g.fillText(
          String.format(Locale.ROOT, "%.2f", first + (last - first) * n / 6),
          left + (right - left) * n / 6 - 12,
          bottom + 18);
    double max = maximum();
    if (max <= 0) return;
    if (pure.isSelected())
      drawTrace(
          g,
          simulation.components().getFirst().pure(),
          max,
          color(0),
          left,
          right,
          top,
          bottom,
          true);
    if (contributions.isSelected())
      for (int i = 1; i < simulation.components().size(); i++)
        drawTrace(
            g,
            simulation.components().get(i).contribution(),
            max,
            color(i + 1),
            left,
            right,
            top,
            bottom,
            false);
    if (mixture.isSelected()) drawTrace(g, trace, max, color(1), left, right, top, bottom, false);
    if (sticks.isSelected()) {
      g.setLineDashes();
      double total = calculated.stream().mapToDouble(c -> c.portion().parts()).sum();
      for (int i = 0; i < calculated.size(); i++) {
        g.setStroke(color(i == 0 ? 0 : i + 1));
        for (Shift s : calculated.get(i).prediction().shifts()) {
          double x = left + (s.ppm() - first) / (last - first) * (right - left);
          if (x >= left && x <= right)
            g.strokeLine(
                x, bottom, x, bottom - 24 - 36 * calculated.get(i).portion().parts() / total);
        }
      }
    }
    g.setFill(palette.ink());
    g.fillText("РАСЧЁТ · без J", left + 8, top + 12);
  }

  private double maximum() {
    double max = 0;
    if (mixture.isSelected())
      max =
          simulation.mixture().intensity().stream()
              .mapToDouble(Double::doubleValue)
              .max()
              .orElse(0);
    if (pure.isSelected())
      max =
          Math.max(
              max,
              simulation.components().getFirst().pure().intensity().stream()
                  .mapToDouble(Double::doubleValue)
                  .max()
                  .orElse(0));
    if (contributions.isSelected())
      for (int i = 1; i < simulation.components().size(); i++)
        max =
            Math.max(
                max,
                simulation.components().get(i).contribution().intensity().stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0));
    return max > 0 ? max : sticks.isSelected() ? 1 : 0;
  }

  private static void drawTrace(
      javafx.scene.canvas.GraphicsContext g,
      NmrMixtureSimulation.Trace trace,
      double max,
      Color color,
      double left,
      double right,
      double top,
      double bottom,
      boolean dashed) {
    g.setStroke(color);
    g.setLineWidth(dashed ? 1.4 : 1.8);
    if (dashed) g.setLineDashes(5, 3);
    else g.setLineDashes();
    g.beginPath();
    for (int i = 0; i < trace.intensity().size(); i++) {
      double x = left + (right - left) * i / (trace.intensity().size() - 1),
          y = bottom - (bottom - top - 24) * trace.intensity().get(i) / max;
      if (i == 0) g.moveTo(x, y);
      else g.lineTo(x, y);
    }
    g.stroke();
    g.setLineDashes();
  }

  private void exports(boolean enabled) {
    saveText.setDisable(!enabled);
    saveCsv.setDisable(!enabled);
    saveSvg.setDisable(!enabled);
  }

  private boolean current() {
    return editor.document().experiment().id().equals(snapshot.experiment().id())
        && snapshot.layouts().equals(editor.document().layouts());
  }

  String report() {
    if (simulation == null || request == null)
      throw new IllegalStateException("Сначала рассчитайте спектр.");
    StringBuilder text =
        new StringBuilder("РАСЧЁТНЫЙ ЯМР · НЕ ИЗМЕРЕНИЕ\n")
            .append(LIMITS)
            .append("\nДата: ")
            .append(calculatedAt)
            .append("\nОпыт: ")
            .append(snapshot.experiment().title())
            .append("\nЯдро: ")
            .append(request.nucleus())
            .append("\nРастворитель: ")
            .append(request.solvent())
            .append("\nЧастота наблюдаемого ядра, МГц: ")
            .append(simulation.observedMHz())
            .append("\nШирина линии FWHM, Гц: ")
            .append(simulation.widthHz())
            .append(
                "\nЛинии: нормированные по площади Лоренцианы; средняя плотность в бине. Интеграл атома = 1. Для 13C это модельная нормировка, не количественная интенсивность реального 13C спектра.\nОстаточный растворитель/вода не добавлены.\n");
    double total = calculated.stream().mapToDouble(c -> c.portion().parts()).sum();
    for (Calculated c : calculated) {
      text.append("\nВещество: ")
          .append(c.portion().choice().name())
          .append("\nID: ")
          .append(c.portion().choice().id())
          .append("\nФормула: ")
          .append(
              snapshot.layouts().get(c.portion().choice().id()).molecule().formula().hillNotation())
          .append("\nСтруктурная подпись: ")
          .append(
              ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.signature(
                  snapshot.layouts().get(c.portion().choice().id()).molecule()))
          .append("\nМольная доля: ")
          .append(c.portion().parts() / total)
          .append("\nПокрытие: ")
          .append(c.prediction().shifts().size())
          .append('/')
          .append(c.prediction().requestedAtoms())
          .append("\n");
      for (Shift s : c.prediction().shifts())
        text.append(s.atomId())
            .append('\t')
            .append(s.label())
            .append('\t')
            .append(s.ppm())
            .append('\t')
            .append(s.minPpm())
            .append('\t')
            .append(s.maxPpm())
            .append('\n');
      c.prediction().warnings().forEach(w -> text.append(w).append('\n'));
      text.append(c.prediction().provenance()).append('\n');
    }
    simulation.diagnostics().forEach(w -> text.append(w).append('\n'));
    if (!current()) text.append("ВНИМАНИЕ: текущий опыт отличается от снимка расчёта.\n");
    return text.toString();
  }

  String csv() {
    StringBuilder text = new StringBuilder();
    for (String line : report().split("\\R"))
      text.append("# ").append(line.replace('\t', ' ')).append('\n');
    text.append("ppm,pure,mixture");
    for (int j = 1; j < simulation.components().size(); j++) text.append(",impurity_").append(j);
    text.append('\n');
    for (int i = 0; i < simulation.mixture().intensity().size(); i++) {
      text.append(simulation.mixture().ppm(i))
          .append(',')
          .append(simulation.components().getFirst().pure().intensity().get(i))
          .append(',')
          .append(simulation.mixture().intensity().get(i));
      for (int j = 1; j < simulation.components().size(); j++)
        text.append(',').append(simulation.components().get(j).contribution().intensity().get(i));
      text.append('\n');
    }
    return text.toString();
  }

  private static String xml(String s) {
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  String svg() {
    StringBuilder out =
        new StringBuilder(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"540\" viewBox=\"0 0 1200 540\"><title>Расчётный ЯМР, не измерение</title><desc>")
            .append(xml(report()))
            .append(
                "</desc><rect width=\"1200\" height=\"540\" fill=\"white\"/><g font-family=\"sans-serif\" fill=\"#182638\"><text x=\"50\" y=\"28\" font-size=\"18\">Расчётный ЯМР · без J-расщепления · nmrshiftdb2</text><text x=\"50\" y=\"51\">Синий: чистое вещество. Фиолетовый: смесь. Не измерение.</text></g>");
    double max =
        Math.max(
            simulation.mixture().intensity().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0),
            simulation.components().getFirst().pure().intensity().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0));
    List<NmrMixtureSimulation.Trace> traces =
        List.of(simulation.components().getFirst().pure(), simulation.mixture());
    for (int j = 0; j < traces.size(); j++) {
      out.append("<polyline fill=\"none\" stroke=\"")
          .append(j == 0 ? "#2472c8" : "#7c3ec8")
          .append("\" stroke-width=\"1.5\" points=\"");
      var trace = traces.get(j);
      for (int i = 0; i < trace.intensity().size(); i++)
        out.append(
            String.format(
                Locale.ROOT,
                "%.2f,%.2f ",
                50 + 1100.0 * i / (trace.intensity().size() - 1),
                465 - (max > 0 ? 370 * trace.intensity().get(i) / max : 0)));
      out.append("\"/>");
    }
    out.append(
        "<path d=\"M50 465H1150\" stroke=\"#182638\"/><g font-family=\"sans-serif\" fill=\"#182638\">");
    for (int n = 0; n <= 10; n++)
      out.append("<text x=\"")
          .append(40 + 110 * n)
          .append("\" y=\"490\">")
          .append(
              fmt(
                  simulation.mixture().firstPpm()
                      + simulation.mixture().stepPpm()
                          * (simulation.mixture().intensity().size() - 1)
                          * n
                          / 10))
          .append("</text>");
    out.append(
        "<text x=\"1100\" y=\"520\">δ, ppm</text><text x=\"50\" y=\"520\">Источник: nmrshiftdb2 · параметры и лицензия в метаданных SVG</text></g></svg>");
    return out.toString();
  }

  private void export(String extension) {
    try {
      String text =
          switch (extension) {
            case "csv" -> csv();
            case "svg" -> svg();
            default -> report();
          };
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Сохранить расчётный ЯМР отдельно от измерений");
      chooser.setInitialFileName("calculated-nmr." + extension);
      chooser
          .getExtensionFilters()
          .add(
              new FileChooser.ExtensionFilter(
                  extension.toUpperCase(Locale.ROOT), "*." + extension));
      var selected = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
      if (selected == null) return;
      if (!selected.getName().toLowerCase(Locale.ROOT).endsWith("." + extension))
        throw new IllegalArgumentException("Выберите файл ." + extension);
      LaboratoryExchangeFiles.write(selected.toPath(), text);
      status.setText(
          "Расчёт сохранён отдельно: " + selected.getName() + ". Фактические анализы не изменены.");
    } catch (Exception e) {
      status.setText("Не удалось сохранить: " + e.getMessage());
    }
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    cancel(null);
    cache.clear();
    super.dispose();
  }
}
