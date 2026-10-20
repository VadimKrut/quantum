/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkInitialGeometry;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.OclConformerGenerator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation.ConformerSuperposition;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation.ConformerCoordinateEnsemble;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryConformerSet.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Coordinate candidates, two independent interactive views, measurements and durable local collections. */
final class LaboratoryConformerPage extends LaboratoryComputationPage {
  private record AxisChoice(ConformerCoordinateEnsemble.Axis axis, String name) {
    @Override
    public String toString() {
      return this.name;
    }
  }

  private final LaboratoryEditor editor;
  private final LaboratoryComponent component;
  private final MoleculeDrawing sourceDrawing;
  private final String experimentId;
  private final Path directory;
  private final Window owner;
  private final List<Frame> frames = new ArrayList<>();
  private final ListView<Frame> list = new ListView<>();
  private final LaboratoryGeometryPane left = new LaboratoryGeometryPane(a -> {}),
      right = new LaboratoryGeometryPane(a -> {});
  private final Label detail = note("");
  private final TextField count = new TextField("12"),
      seed = new TextField("2026"),
      angle = new TextField("60"),
      name = new TextField();
  private final ComboBox<AxisChoice> axis = new ComboBox<>();
  private final TextArea coordinates = new TextArea();
  private final VBox discard = new VBox(8);
  private Task<OclConformerGenerator.Result> generation;
  private final ComboBox<Frame> reference = new ComboBox<>();
  private final Label comparison = note(""), generationNotes = note("");
  private final TitledPane generationReport =
      new TitledPane("Отчёт построения", this.generationNotes);
  private String referenceId;
  private boolean choosingReference;
  private boolean dirty, conflict;
  private Properties conflictDraft;
  private Path saved;
  private Molecule collectionSource;
  private boolean switching;

  LaboratoryConformerPage(
      final LaboratoryEditor editor,
      final String componentId,
      final Path directory,
      final Window owner,
      final Runnable close) {
    super("3D-конформации", close);
    this.editor = editor;
    this.directory = directory;
    this.owner = owner;
    this.experimentId = editor.document().experiment().id();
    this.component =
        editor.document().experiment().components().stream()
            .filter(c -> c.id().equals(componentId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Выберите структуру для сравнения конформеров."));
    this.sourceDrawing =
        editor
            .document()
            .layouts()
            .getOrDefault(
                this.component.id(),
                MoleculeDrawing.of(this.component.molecule(), List.of(), List.of(), List.of()));
    this.setId("conformer-page");
    this.collectionSource = this.component.molecule();
    this.list.setId("conformer-list");
    this.list.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Frame f, final boolean empty) {
                super.updateItem(f, empty);
                this.setText(empty || f == null ? null : f.title() + "\n" + energyText(f));
              }
            });
    this.list.setPlaceholder(note("Постройте варианты или откройте сохранённый набор."));
    this.list.setPrefWidth(235);
    this.list.setMinWidth(150);
    this.list
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (!this.switching && a != null && !this.name.getText().equals(a.title())) {
                this.switching = true;
                this.list.getSelectionModel().select(a);
                this.switching = false;
                this.status.setText(
                    "Сохраните новое название кнопкой «Назвать» перед сменой варианта.");
                return;
              }
              if (!this.switching) {
                this.selected(b);
              }
            });
    this.count.setPrefColumnCount(3);
    this.seed.setPrefColumnCount(8);
    this.angle.setPrefColumnCount(5);
    this.name.setPromptText("Название выбранной конформации");
    this.name.setId("conformer-name");
    this.axis.setPrefWidth(240);
    this.left.emptyHint("Нет 3D-модели", "Постройте варианты или откройте сохранённый набор.");
    this.right.emptyHint(
        "Нет выбранного варианта", "Постройте варианты или откройте сохранённый набор.");
    this.left.setId("conformer-reference-view");
    this.right.setId("conformer-selected-view");
    this.left.compact(true);
    this.right.compact(true);
    this.left.setMinWidth(0);
    this.right.setMinWidth(0);
    if (this.component.molecule().atoms().stream().allMatch(a -> a.coordinate() != null)) {
      this.left.load(this.component.molecule(), null);
    }
    final VBox baseline = new VBox(8, note("Для сравнения"), this.left),
        chosen = new VBox(8, note("Выбранный вариант"), this.right);
    VBox.setVgrow(this.left, Priority.ALWAYS);
    VBox.setVgrow(this.right, Priority.ALWAYS);
    baseline.setMinWidth(0);
    chosen.setMinWidth(0);
    final SplitPane viewers = new SplitPane(baseline, chosen);
    viewers.setDividerPositions(.5);
    viewers.setMinWidth(0);
    viewers.setMinHeight(340);
    viewers
        .widthProperty()
        .addListener(
            o -> {
              final boolean stacked = viewers.getWidth() < 620;
              final var orientation =
                  stacked
                      ? javafx.geometry.Orientation.VERTICAL
                      : javafx.geometry.Orientation.HORIZONTAL;
              if (viewers.getOrientation() != orientation) {
                viewers.setOrientation(orientation);
                viewers.setMinHeight(stacked ? 620 : 340);
                viewers.setDividerPositions(.5);
              }
            });
    final Runnable sizeViews =
        () -> {
          this.left.available(this.left.getWidth(), this.left.getHeight(), true);
          this.right.available(this.right.getWidth(), this.right.getHeight(), true);
        };
    this.left.widthProperty().addListener(o -> sizeViews.run());
    this.right.widthProperty().addListener(o -> sizeViews.run());
    this.left.heightProperty().addListener(o -> sizeViews.run());
    this.right.heightProperty().addListener(o -> sizeViews.run());
    this.coordinates.setId("conformer-coordinates");
    this.coordinates.setPrefRowCount(6);
    this.coordinates.setPromptText("ID элемент X Y Z — координаты в Å");
    final TitledPane input =
        new TitledPane(
            "Готовые 3D-координаты · явное соответствие атомов",
            new VBox(
                6,
                note(
                    "Вставьте координаты в Å: ID, элемент, X, Y, Z. Все атомы, включая H; порядок строк свободный. Граф и конфигурация сохраняются и проверяются."),
                this.coordinates,
                new FlowPane(
                    8,
                    6,
                    this.button("Шаблон атомов", "coordinate-template", this::coordinateTemplate),
                    this.button(
                        "Добавить проверенную геометрию",
                        "coordinate-import",
                        this::importCoordinates),
                    this.button("Очистить ввод", "coordinate-clear", this.coordinates::clear))));
    input.setAnimated(false);
    input.setExpanded(false);
    input.setId("conformer-coordinate-input");
    final Button generate = this.button("Построить варианты", "generate", this::generate);
    generate.getStyleClass().add("laboratory-primary-action");
    generate.disableProperty().bind(this.progress.visibleProperty());
    this.count.disableProperty().bind(this.progress.visibleProperty());
    final Button cancel =
        this.button(
            "Остановить",
            "cancel",
            () -> {
              if (this.generation != null) {
                this.generation.cancel();
              }
            });
    cancel.visibleProperty().bind(this.progress.visibleProperty());
    cancel.managedProperty().bind(cancel.visibleProperty());
    final Button open = this.button("Открыть набор…", "open", this::open);
    open.disableProperty().bind(this.progress.visibleProperty());
    final FlowPane build =
        new FlowPane(8, 6, note("Вариантов"), this.count, generate, cancel, open);
    final Button rotate =
        this.selectionButton("Добавить повёрнутый вариант", "rotate", this::rotate);
    rotate.disableProperty().unbind();
    rotate
        .disableProperty()
        .bind(this.axis.valueProperty().isNull().or(this.progress.visibleProperty()));
    final VBox controls =
        new VBox(
            9,
            new FlowPane(8, 6, note("Число для воспроизводимого построения"), this.seed),
            note(
                "Построение учитывает варианты колец и оптимизирует геометрию MMFF94s+. Ниже можно вручную повернуть одиночную связь вне кольца: энергия такого нового варианта будет неизвестна."),
            new FlowPane(8, 6, this.axis, note("Поворот, °"), this.angle, rotate),
            input);
    controls.setPadding(new Insets(10));
    controls.setMinWidth(0);
    controls.setMinHeight(Region.USE_PREF_SIZE);
    final TitledPane settings =
        new TitledPane("Дополнительно · повороты связей и готовые координаты", controls);
    settings.setAnimated(false);
    settings.setExpanded(false);
    settings.setId("conformer-settings");
    settings.setMinHeight(Region.USE_PREF_SIZE);
    final TitledPane provenance = new TitledPane("Как получен выбранный вариант", this.detail);
    provenance.setAnimated(false);
    provenance.setExpanded(false);
    provenance.setId("conformer-provenance");
    provenance.setMinHeight(Region.USE_PREF_SIZE);
    provenance
        .visibleProperty()
        .bind(this.list.getSelectionModel().selectedItemProperty().isNotNull());
    provenance.managedProperty().bind(provenance.visibleProperty());
    this.reference.disableProperty().bind(this.progress.visibleProperty());
    this.name.disableProperty().bind(this.progress.visibleProperty());
    this.reference.setId("conformer-comparison-reference");
    this.reference.setPrefWidth(230);
    this.reference.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(Frame value) {
            return value == null ? "" : value.title();
          }

          public Frame fromString(String value) {
            return null;
          }
        });
    this.reference
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!choosingReference && !this.running && b != null) {
                this.referenceId = b.geometry().id().value();
                this.left.load(b.geometry(), null);
                this.left.view(b.view());
                this.dirty = true;
                this.comparison();
              }
            });
    final FlowPane selectedTools =
        new FlowPane(
            8,
            6,
            this.name,
            this.selectionButton("Назвать", "rename", this::rename),
            this.selectionButton("Запомнить виды", "view", this::rememberView),
            this.selectionButton(
                "Одинаковый ракурс", "sync", () -> this.right.view(this.left.view())));
    selectedTools
        .visibleProperty()
        .bind(this.list.getSelectionModel().selectedItemProperty().isNotNull());
    selectedTools.managedProperty().bind(selectedTools.visibleProperty());
    final FlowPane compareTools =
        new FlowPane(
            8,
            6,
            note("Сравнить с"),
            this.reference,
            this.selectionButton("Совместить положения", "align", this::align));
    compareTools
        .visibleProperty()
        .bind(this.list.getSelectionModel().selectedItemProperty().isNotNull());
    compareTools.managedProperty().bind(compareTools.visibleProperty());
    this.comparison.setId("conformer-comparison-summary");
    this.comparison.setTooltip(
        new Tooltip(
            "RMSD после жёсткого совмещения: одинаковые ID тяжёлых атомов, без зеркального отражения и перестановки симметричных атомов. Энергии сравниваются только при одинаковом методе и реализации."));
    this.generationReport.setId("conformer-generation-report");
    this.generationReport.setExpanded(false);
    this.generationReport.setAnimated(false);
    visible(this.generationReport, false);
    final Label substance = note(this.component.molecule().displayName());
    substance.setStyle("-fx-font-weight: bold;");
    final VBox content =
        new VBox(
            10,
            substance,
            build,
            note("Построение: MMFF94s+ · вакуум, без растворителя."),
            compareTools,
            this.comparison,
            viewers,
            selectedTools,
            provenance,
            this.generationReport,
            settings);
    content.setPadding(new Insets(12));
    content.setMinWidth(0);
    content.setMinHeight(Region.USE_PREF_SIZE);
    VBox.setVgrow(viewers, Priority.ALWAYS);
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setMinWidth(0);
    scroll.setMinHeight(0);
    final SplitPane body = new SplitPane(this.list, scroll);
    body.setDividerPositions(.21);
    body.setMinWidth(0);
    this.setCenter(body);
    body.widthProperty()
        .addListener(
            o -> {
              final boolean narrow = body.getWidth() < 980;
              final var orientation =
                  narrow
                      ? javafx.geometry.Orientation.VERTICAL
                      : javafx.geometry.Orientation.HORIZONTAL;
              if (body.getOrientation() != orientation) {
                body.setOrientation(orientation);
                this.list.setMinHeight(narrow ? 100 : 0);
                this.list.setMaxHeight(narrow ? 140 : Double.MAX_VALUE);
                body.setDividerPositions(narrow ? .2 : .21);
              }
            });
    this.discard
        .getChildren()
        .setAll(
            note("Набор ещё не сохранён."),
            new FlowPane(
                8,
                6,
                this.button(
                    "Отбросить и выйти",
                    "discard",
                    () -> {
                      this.dirty = false;
                      this.conflict = false;
                      this.coordinates.clear();
                      this.count.setText("12");
                      this.seed.setText("2026");
                      this.angle.setText("60");
                      final var selected = this.list.getSelectionModel().getSelectedItem();
                      this.name.setText(selected == null ? "" : selected.title());
                      this.finish();
                    }),
                this.button("Остаться", "continue", () -> visible(this.discard, false))));
    visible(this.discard, false);
    final Button apply = this.selectionButton("Использовать в опыте", "apply", this::apply);
    apply.getStyleClass().add("laboratory-primary-action");
    apply.setTooltip(
        new Tooltip("Сохранить набор и применить выбранные 3D-координаты к веществу в опыте."));
    final VBox footer =
        new VBox(
            8,
            this.discard,
            new FlowPane(
                8, 6, this.selectionButton("Сохранить набор и виды", "save", this::save), apply),
            this.status);
    footer.setPadding(new Insets(10, 16, 14, 16));
    this.setBottom(footer);
  }

  private void generate() {
    this.idle();
    if (this.dirty
        || !this.coordinates.getText().isBlank()
        || !this.frames.isEmpty() && !this.name.getText().equals(this.chosen().title())) {
      throw new IllegalArgumentException("Сохраните текущий набор перед построением нового.");
    }
    final int amount;
    final long random;
    try {
      amount = Integer.parseInt(this.count.getText().strip());
    } catch (final NumberFormatException ex) {
      throw new IllegalArgumentException("Укажите целое число вариантов от 1 до 64.");
    }
    if (amount < 1 || amount > 64) {
      throw new IllegalArgumentException("Выберите от 1 до 64 вариантов.");
    }
    try {
      random = Long.parseLong(this.seed.getText().strip());
    } catch (final NumberFormatException ex) {
      throw new IllegalArgumentException(
          "В дополнительных настройках укажите целое число для воспроизводимого построения, например 2026.");
    }
    this.generation =
        new Task<>() {
          @Override
          protected OclConformerGenerator.Result call() {
            return OclConformerGenerator.generate(sourceDrawing, amount, random);
          }
        };
    this.generation.setOnSucceeded(
        e -> {
          if (this.disposed) {
            return;
          }
          this.busy(false);
          this.generated(this.generation.getValue());
        });
    this.generation.setOnFailed(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.status.setText(this.generation.getException().getMessage());
          }
        });
    this.generation.setOnCancelled(
        e -> {
          if (!this.disposed) {
            this.busy(false);
            this.status.setText("Построение отменено; опыт не изменён.");
          }
        });
    this.busy(true);
    final Thread worker = new Thread(this.generation, "laboratory-conformers");
    worker.setDaemon(true);
    worker.start();
  }

  private void generated(final OclConformerGenerator.Result result) {
    this.generationNotes.setText(String.join("\n", result.diagnostics()));
    visible(this.generationReport, !result.diagnostics().isEmpty());
    this.generationReport.setExpanded(false);
    if (result.candidates().isEmpty()) {
      this.status.setText(
          "Подходящих конформаций не получено. Предыдущий набор сохранён; причины — в отчёте построения.");
      return;
    }
    this.frames.clear();
    int i = 1;
    for (final var c : result.candidates()) {
      this.frames.add(
          new Frame(
              "Конформация " + i++,
              c.method(),
              snapshot(c.geometry()),
              View.DEFAULT,
              new Energy(
                  c.energyKcalMol(),
                  OclConformerGenerator.FORCE_FIELD,
                  OclConformerGenerator.IMPLEMENTATION)));
    }
    this.saved = null;
    this.collectionSource = this.component.molecule();
    this.dirty = true;
    this.referenceId = null;
    this.switching = true;
    this.list.getItems().setAll(this.frames);
    this.list.getSelectionModel().selectFirst();
    this.switching = false;
    this.references();
    this.selected(this.list.getSelectionModel().getSelectedItem());
    this.status.setText(
        "Получено конформаций: "
            + this.frames.size()
            + " из "
            + result.requested()
            + ". Энергии расчётные; минимум среди найденных. Сохраните набор.");
  }

  private void coordinateTemplate() {
    this.idle();
    if (!this.coordinates.getText().isBlank()) {
      throw new IllegalArgumentException(
          "Сначала очистите текущий ввод координат, чтобы заменить его шаблоном.");
    }
    final StringBuilder text =
        new StringBuilder("# ID element X Y Z; angstrom; replace ? with coordinates\n");
    for (final var atom : this.component.molecule().atoms()) {
      final var p = atom.coordinate();
      text.append(atom.id().value())
          .append(' ')
          .append(atom.element().symbol().value())
          .append(' ');
      if (p == null) {
        text.append("? ? ?");
      } else {
        text.append(p.unit().toAngstrom(p.x()))
            .append(' ')
            .append(p.unit().toAngstrom(p.y()))
            .append(' ')
            .append(p.unit().toAngstrom(p.z()));
      }
      text.append('\n');
    }
    this.coordinates.setText(text.toString());
  }

  private void importCoordinates() {
    this.idle();
    if (this.frames.size() >= 128) {
      throw new IllegalArgumentException("В наборе уже 128 вариантов.");
    }
    final var geometry =
        CdkInitialGeometry.readCoordinates(this.sourceDrawing, this.coordinates.getText());
    if (!this.frames.isEmpty() && !this.name.getText().equals(this.chosen().title())) {
      this.rename();
    }
    final var frame =
        new Frame(
            "Введённая геометрия " + (this.frames.size() + 1),
            "Координаты введены пользователем в Å; явные ID атомов; конфигурация проверена по 3D; без оптимизации. "
                + ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange
                    .CdkStereochemicalProjection.annotation(this.component.molecule()),
            snapshot(geometry),
            View.DEFAULT);
    this.frames.add(frame);
    this.list.getItems().add(frame);
    this.references();
    this.list.getSelectionModel().select(frame);
    this.coordinates.clear();
    this.dirty = true;
    this.status.setText(
        "Готовая геометрия проверена и добавлена в набор. Сохраните набор перед применением.");
  }

  private void selected(final Frame f) {
    this.axis.getItems().clear();
    if (f == null) {
      this.name.clear();
      this.detail.setText("");
      this.right.load(null, null);
      return;
    }
    this.right.load(f.geometry(), null);
    this.right.view(f.view());
    this.name.setText(f.title());
    this.detail.setText(
        f.method()
            + (f.energy() == null
                ? ""
                : "\nПотенциальная энергия: "
                    + String.format(Locale.ROOT, "%.4f ккал/моль", f.energy().kcalPerMol())
                    + " · "
                    + f.energy().forceField()
                    + " · "
                    + f.energy().implementation()
                    + ". Это не свободная энергия и не экспериментальное измерение."));
    this.comparison();
    final var m = f.geometry();
    final Map<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId, Integer> numbers =
        new HashMap<>();
    for (int i = 0; i < m.atomCount(); i++) {
      numbers.put(m.atoms().get(i).id(), i + 1);
    }
    for (final var a : ConformerCoordinateEnsemble.axes(m)) {
      this.axis
          .getItems()
          .add(
              new AxisChoice(
                  a, "Связь " + numbers.get(a.first()) + " — " + numbers.get(a.second())));
    }
    this.axis.getSelectionModel().selectFirst();
  }

  private String energyText(final Frame frame) {
    if (frame.energy() == null) {
      return "Энергия не рассчитана";
    }
    final double minimum =
        this.frames.stream()
            .filter(f -> frame.energy().comparable(f.energy()))
            .mapToDouble(f -> f.energy().kcalPerMol())
            .min()
            .orElse(frame.energy().kcalPerMol());
    return String.format(
        Locale.ROOT, "ΔE %.2f ккал/моль", Math.max(0, frame.energy().kcalPerMol() - minimum));
  }

  private void references() {
    this.choosingReference = true;
    this.reference.getItems().setAll(this.frames);
    final var selected =
        this.frames.stream()
            .filter(f -> f.geometry().id().value().equals(this.referenceId))
            .findFirst()
            .orElse(this.frames.isEmpty() ? null : this.frames.getFirst());
    this.reference.setValue(selected);
    this.referenceId = selected == null ? null : selected.geometry().id().value();
    this.choosingReference = false;
    if (selected != null) {
      this.left.load(selected.geometry(), null);
      this.left.view(selected.view());
    }
    this.list.refresh();
  }

  private void comparison() {
    final var selected = this.list.getSelectionModel().getSelectedItem();
    final var ref = this.reference.getValue();
    if (selected == null || ref == null) {
      this.comparison.setText("");
      return;
    }
    try {
      final var fit = ConformerSuperposition.align(ref.geometry(), selected.geometry());
      this.comparison.setText(
          energyText(selected)
              + " · "
              + String.format(Locale.ROOT, "RMSD %.3f Å", fit.rmsdAngstrom())
              + " · "
              + fit.atomCount()
              + " тяжёлых атомов"
              + (selected.energy() != null && selected.energy().comparable(ref.energy())
                  ? String.format(
                      Locale.ROOT,
                      " · к опорной ΔE %+.2f ккал/моль",
                      selected.energy().kcalPerMol() - ref.energy().kcalPerMol())
                  : ""));
    } catch (final IllegalArgumentException invalid) {
      this.comparison.setText("Сравнение недоступно: " + invalid.getMessage());
    }
  }

  private void align() {
    this.idle();
    final var selected = this.chosen();
    if (!this.name.getText().equals(selected.title())) {
      throw new IllegalArgumentException("Сначала сохраните введённое название.");
    }
    final var ref = this.reference.getValue();
    if (ref == null) {
      throw new IllegalArgumentException("Выберите опорную конформацию.");
    }
    final var fit = ConformerSuperposition.align(ref.geometry(), selected.geometry());
    CdkInitialGeometry.verify(this.sourceDrawing, fit.geometry());
    this.replace(
        selected,
        new Frame(
            selected.title(),
            selected.method(),
            fit.geometry(),
            this.left.view(),
            selected.energy()));
    this.status.setText(
        "Положение совмещено по одинаковым тяжёлым атомам; длины связей, конфигурация и энергия сохранены. Сохраните набор.");
  }

  private Frame chosen() {
    return Objects.requireNonNull(
        this.list.getSelectionModel().getSelectedItem(), "Выберите конформер.");
  }

  private void rotate() {
    this.idle();
    if (!this.name.getText().equals(this.chosen().title())) {
      this.rename();
    }
    if (this.frames.size() >= 128) {
      throw new IllegalArgumentException("В наборе уже 128 вариантов.");
    }
    final var previous = this.chosen();
    if (this.axis.getValue() == null) {
      throw new IllegalArgumentException("Нет подходящей одиночной связи вне кольца.");
    }
    final double degrees = Double.parseDouble(this.angle.getText().replace(',', '.').strip());
    final var rotated =
        ConformerCoordinateEnsemble.rotate(
            previous.geometry(), this.axis.getValue().axis(), degrees);
    CdkInitialGeometry.verify(this.sourceDrawing, rotated);
    final var f =
        new Frame(
            "Поворот " + (this.frames.size() + 1),
            this.axis.getValue().name()
                + ": "
                + degrees
                + "° от «"
                + previous.title()
                + "»; без оптимизации",
            snapshot(rotated),
            this.right.view());
    this.frames.add(f);
    this.list.getItems().add(f);
    this.references();
    this.list.getSelectionModel().select(f);
    this.dirty = true;
  }

  private void rename() {
    this.idle();
    final var old = this.chosen();
    this.replace(
        old,
        new Frame(
            this.name.getText().strip(),
            old.method(),
            old.geometry(),
            this.right.view(),
            old.energy()));
  }

  private void rememberView() {
    this.idle();
    final var f = this.chosen();
    final var ref = this.reference.getValue();
    if (ref != null && !ref.geometry().id().equals(f.geometry().id())) {
      final int index = this.frames.indexOf(ref);
      this.frames.set(index, ref.withView(this.left.view()));
      this.list.getItems().set(index, this.frames.get(index));
    }
    this.replace(f, f.withView(this.right.view()));
    this.status.setText(
        "Виды и опорная конформация запомнены. Сохраните набор для следующего запуска.");
  }

  private void replace(final Frame previous, final Frame next) {
    final int index = this.frames.indexOf(previous);
    this.frames.set(index, next);
    this.switching = true;
    this.list.getItems().set(index, next);
    this.list.getSelectionModel().select(index);
    this.switching = false;
    this.references();
    this.selected(next);
    this.dirty = true;
  }

  private LaboratoryConformerSet collection() {
    return new LaboratoryConformerSet(
        this.experimentId,
        this.component.id(),
        this.collectionSource,
        this.frames,
        this.referenceId);
  }

  private void save() {
    this.idle();
    try {
      if (!this.name.getText().equals(this.chosen().title())) {
        this.rename();
      }
      this.rememberView();
      this.saved = new LaboratoryConformerCodec().save(this.directory, this.collection());
      this.dirty = false;
      this.status.setText("Набор и виды сохранены: " + this.saved.getFileName());
    } catch (final java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private void open() {
    this.idle();
    if (this.pending()) {
      throw new IllegalArgumentException("Сохраните текущий набор перед открытием другого.");
    }
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Открыть набор конформеров QCHEM");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Набор конформеров QCHEM", "*.qchem"));
    if (java.nio.file.Files.isDirectory(this.directory)) {
      chooser.setInitialDirectory(this.directory.toFile());
    }
    final var file = chooser.showOpenDialog(this.owner);
    if (file == null) {
      return;
    }
    try {
      final var set = new LaboratoryConformerCodec().read(file.toPath());
      this.accept(set);
      this.saved = file.toPath();
      this.dirty = false;
      this.status.setText("Набор загружен; исходная структура совпадает.");
    } catch (final java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private void accept(final LaboratoryConformerSet set) {
    final Molecule current = this.component.molecule();
    if (!set.experimentId().equals(this.experimentId)
        || !set.componentId().equals(this.component.id())
        || !(set.source().equals(current)
            || set.frames().stream().anyMatch(f -> f.geometry().equals(current)))) {
      throw new IllegalArgumentException(
          "Набор относится к другой исходной структуре или её версии.");
    }
    for (final var frame : set.frames()) {
      CdkInitialGeometry.verify(this.sourceDrawing, frame.geometry());
    }
    this.collectionSource = set.source();
    this.frames.clear();
    this.frames.addAll(set.frames());
    this.switching = true;
    this.list.getItems().setAll(this.frames);
    this.list.getSelectionModel().selectFirst();
    this.switching = false;
    this.referenceId = set.referenceId();
    this.references();
    this.selected(this.list.getSelectionModel().getSelectedItem());
  }

  private void apply() {
    this.idle();
    final var frame = this.chosen();
    CdkInitialGeometry.verify(this.sourceDrawing, frame.geometry());
    if (!this.editor.document().experiment().id().equals(this.experimentId)) {
      throw new IllegalArgumentException("Открыт другой опыт.");
    }
    this.save();
    this.editor.applyGeometry(
        this.component.id(),
        this.component.molecule(),
        frame.geometry(),
        "Применена расчётная/введённая конформация «"
            + frame.title()
            + "»; "
            + frame.method()
            + ". Набор: "
            + this.saved.getFileName());
    this.finish();
  }

  private void idle() {
    if (this.running) {
      throw new IllegalArgumentException("Дождитесь построения или остановите его.");
    }
    if (this.conflict) {
      throw new IllegalArgumentException(
          "Черновик относится к другой структуре. Закройте его без применения после сохранения исходного текста.");
    }
  }

  boolean pending() {
    final Frame chosen = this.list.getSelectionModel().getSelectedItem();
    return this.dirty
        || this.conflict
        || !this.coordinates.getText().isBlank()
        || chosen != null && !this.name.getText().equals(chosen.title())
        || this.frames.isEmpty()
            && (!"12".equals(this.count.getText())
                || !"2026".equals(this.seed.getText())
                || !"60".equals(this.angle.getText()));
  }

  @Override
  boolean canClose() {
    if (this.running) {
      this.status.setText("Остановите построение и дождитесь его завершения.");
      return false;
    }
    if (this.pending()) {
      visible(this.discard, true);
      return false;
    }
    return true;
  }

  void draft(final Properties p, final boolean restore) {
    if (!restore && this.conflictDraft != null) {
      this.conflictDraft.forEach(p::put);
      return;
    }
    if (!restore) {
      p.setProperty("conformers.coordinates", this.coordinates.getText());
      p.setProperty("conformers.component", this.component.id());
      p.setProperty("conformers.experiment", this.experimentId);
      p.setProperty("conformers.count", this.count.getText());
      p.setProperty("conformers.seed", this.seed.getText());
      p.setProperty("conformers.angle", this.angle.getText());
      p.setProperty(
          "conformers.selected",
          Integer.toString(this.list.getSelectionModel().getSelectedIndex()));
      p.setProperty("conformers.name", this.name.getText());
      if (!this.frames.isEmpty()) {
        final var encoded =
            new ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService()
                .write(
                    new LaboratoryConformerCodec().document(this.collection(), "conformer_draft"));
        if (!encoded.success()) {
          throw new IllegalArgumentException("Не удалось сохранить черновик конформеров.");
        }
        p.setProperty("conformers.collection", encoded.value());
      }
      return;
    }
    this.coordinates.setText(p.getProperty("conformers.coordinates", ""));
    this.count.setText(p.getProperty("conformers.count", "12"));
    this.seed.setText(p.getProperty("conformers.seed", "2026"));
    this.angle.setText(p.getProperty("conformers.angle", "60"));
    try {
      if (p.containsKey("conformers.collection")) {
        this.accept(new LaboratoryConformerCodec().read(p.getProperty("conformers.collection")));
        final int index = Integer.parseInt(p.getProperty("conformers.selected", "0"));
        if (index >= 0 && index < this.frames.size()) {
          this.list.getSelectionModel().select(index);
        }
        this.name.setText(p.getProperty("conformers.name", this.chosen().title()));
        this.dirty = true;
      }
    } catch (final RuntimeException ex) {
      this.conflict = true;
      this.conflictDraft = new Properties();
      p.stringPropertyNames().stream()
          .filter(k -> k.startsWith("conformers."))
          .forEach(k -> this.conflictDraft.setProperty(k, p.getProperty(k)));
      this.status.setText("Конфликт черновика: " + ex.getMessage());
    }
  }

  @Override
  void dispose() {
    if (this.generation != null) {
      this.generation.cancel();
    }
    super.dispose();
  }

  private Button button(final String text, final String id, final Runnable run) {
    final Button b = new Button(text);
    b.setId("conformer-" + id);
    b.setOnAction(
        e -> {
          try {
            run.run();
          } catch (final RuntimeException ex) {
            this.status.setText(
                ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
          }
        });
    return b;
  }

  private Button selectionButton(final String text, final String id, final Runnable run) {
    final Button b = this.button(text, id, run);
    b.disableProperty()
        .bind(
            this.list
                .getSelectionModel()
                .selectedItemProperty()
                .isNull()
                .or(this.progress.visibleProperty()));
    return b;
  }

  private static Molecule snapshot(final Molecule m) {
    return LaboratoryComputationFiles.geometrySnapshot(
        m, "conformer_" + UUID.randomUUID(), "CONFORMER_COORDINATE_SNAPSHOT");
  }

  private static void visible(final javafx.scene.Node n, final boolean value) {
    n.setVisible(value);
    n.setManaged(value);
  }
}
