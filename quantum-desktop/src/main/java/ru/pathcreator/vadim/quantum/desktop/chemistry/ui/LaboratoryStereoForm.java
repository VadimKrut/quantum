/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileCodec;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStereoEditing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** One explicit assignment per transaction; drawing wedges remain separate from core descriptors. */
final class LaboratoryStereoForm extends BorderPane {
  private record AtomChoice(AtomId id, String label) {
    public String toString() {
      return this.label;
    }
  }

  private final MoleculeDrawing drawing;
  private final ListView<Stereocenter> existing = new ListView<>();
  private final ComboBox<StereocenterKind> kind = new ComboBox<>();
  private final ComboBox<StereochemicalDescriptor> descriptor = new ComboBox<>();
  private final ComboBox<AtomChoice> primary = new ComboBox<>(), secondary = new ComboBox<>();
  private final List<ComboBox<AtomChoice>> references =
      List.of(new ComboBox<>(), new ComboBox<>(), new ComboBox<>(), new ComboBox<>());
  private final List<VBox> referenceFields = new ArrayList<>();
  private final CheckBox reviewed = new CheckBox("Все стереозаписи проверены для текущего графа");
  private final javafx.beans.property.StringProperty loadedFields =
      new javafx.beans.property.SimpleStringProperty("");
  private boolean loading;
  private final LaboratoryEditor editor;
  private final String componentId, experimentId;
  private final Runnable close;
  private final Label error = new Label();
  private final FlowPane confirmation = new FlowPane(8, 8);
  private final Map<AtomId, AtomChoice> atomsById = new LinkedHashMap<>();
  private final Map<AtomId, Integer> atomNumbers = new LinkedHashMap<>();
  private final javafx.scene.canvas.Canvas preview = new javafx.scene.canvas.Canvas(420, 280);
  private final Label picking = new Label();
  private ComboBox<AtomChoice> target;
  private final Map<ComboBox<AtomChoice>, String> targetNames = new LinkedHashMap<>();
  private final Map<AtomId, javafx.geometry.Point2D> screenAtoms = new LinkedHashMap<>();
  private final CheckBox numbers = new CheckBox("Номера атомов");
  private AtomId hovered;
  private double zoom = 1, panX, panY, pressX, pressY, startPanX, startPanY;
  private String initial;
  private boolean applied, recoveredConflict;
  private VBox secondaryField;

  LaboratoryStereoForm(
      final LaboratoryEditor editor,
      final String componentId,
      final AtomId selected,
      final AtomId second,
      final Runnable close) {
    this.editor = editor;
    this.componentId = componentId;
    this.experimentId = editor.document().experiment().id();
    this.close = close;
    this.drawing =
        Objects.requireNonNull(
            editor.document().layouts().get(componentId), "Компонент отсутствует.");
    this.setId("chemistry-stereo-form");
    final Label explanation =
        label(
            "Здесь конфигурация задаётся вручную; клинья и 3D остаются без изменений. Для проверки откройте «Расчёт R/S и E/Z». R*/S*, рацемат и неизвестная абсолютная конфигурация задаются в «Относительной стереохимии».");
    if (this.drawing
        .molecule()
        .metadata()
        .attributes()
        .containsKey(Stereochemistry.ORDERED_PARITY_ATTRIBUTE)) {
      explanation.setText(
          "Импортированная тетраэдрическая конфигурация сохранена вместе со структурой. UNKNOWN в этом списке означает, что ручной CIP-дескриптор не назначен; конфигурация сохранена. Расчёт доступен через «Расчёт R/S и E/Z». Ручное изменение стереозаписи потребует перепроверки экспорта.");
    }
    this.existing.setId("chemistry-stereo-list");
    this.existing.getItems().setAll(this.drawing.molecule().stereochemistry().centers());
    this.existing.setPrefHeight(this.existing.getItems().isEmpty() ? 76 : 112);
    this.existing.setPlaceholder(new Label("Стереозаписей пока нет"));
    this.existing.setCellFactory(
        list ->
            new ListCell<>() {
              protected void updateItem(final Stereocenter value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null
                        ? null
                        : kindName(value.kind())
                            + " · "
                            + value.descriptor()
                            + " · "
                            + thisLabel(value.primaryAtomId())
                            + (value.hasSecondaryAtom()
                                ? " — " + thisLabel(value.secondaryAtomId())
                                : ""));
              }
            });
    this.kind.getItems().setAll(StereocenterKind.values());
    this.kind.setId("chemistry-stereo-kind");
    this.kind.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final StereocenterKind value) {
            return value == null ? "" : kindName(value);
          }

          public StereocenterKind fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    this.descriptor.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final StereochemicalDescriptor value) {
            return value == null
                ? ""
                : value == StereochemicalDescriptor.UNKNOWN ? "Неизвестна" : value.name();
          }

          public StereochemicalDescriptor fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    this.primary.setId("chemistry-stereo-primary");
    this.secondary.setId("chemistry-stereo-secondary");
    this.descriptor.setId("chemistry-stereo-descriptor");
    final List<AtomChoice> atoms = new ArrayList<>();
    for (final var atom : this.drawing.molecule().atoms()) {
      final var choice =
          new AtomChoice(
              atom.id(),
              atom.element().symbol().value()
                  + (atom.hasIsotope() ? "-" + atom.isotope().massNumber() : "")
                  + " · "
                  + (atoms.size() + 1));
      atoms.add(choice);
      this.atomsById.put(atom.id(), choice);
      this.atomNumbers.put(atom.id(), atoms.size());
    }
    this.primary.getItems().setAll(atoms);
    this.secondary.getItems().setAll(atoms);
    for (int index = 0; index < this.references.size(); index++) {
      final var box = this.references.get(index);
      box.getItems().setAll(atoms);
      box.setId("chemistry-stereo-reference-" + index);
      this.referenceFields.add(field("Опорный атом " + (index + 1), box));
    }
    this.secondaryField = field("Второй атом оси / связи", this.secondary);
    this.kind.setOnAction(
        event -> {
          if (!this.loading) {
            this.configure();
            this.suggest();
          }
        });
    this.primary.setOnAction(
        event -> {
          if (!this.loading) {
            this.suggest();
          }
        });
    this.secondary.setOnAction(
        event -> {
          if (!this.loading) {
            this.suggest();
          }
        });
    this.existing
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, before, value) -> {
              if (this.loading || value == null) {
                return;
              }
              if (this.initial != null
                  && (!this.fields().equals(this.loadedFields.get())
                      || this.reviewed.isSelected())) {
                this.loading = true;
                this.existing.getSelectionModel().select(before);
                this.loading = false;
                this.error.setText(
                    "Сначала примените текущую запись или отмените ввод. Несохранённые поля оставлены без изменений.");
                return;
              }
              this.load(value);
              this.reviewed.setSelected(false);
              this.initial = this.state();
            });
    final Button create = new Button("Новая стереозапись");
    create.setId("chemistry-stereo-new");
    create.setOnAction(
        event -> {
          if (!this.canClose()) {
            return;
          }
          this.existing.getSelectionModel().clearSelection();
          this.descriptor.setValue(StereochemicalDescriptor.UNKNOWN);
          this.suggest();
          this.loadedFields.set(this.fields());
          this.initial = this.state();
        });
    final Label hint =
        new Label(
            "Опорные атомы задаются явно и сохраняются в указанном порядке. Для тетраэдра нужны четыре одинарные связи с явными атомами (включая H). Для двойной связи — один опорный заместитель на каждом конце. Стереогенность и эквивалентность заместителей автоматически не доказываются.");
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    final Label review =
        new Label(
            this.drawing
                .molecule()
                .metadata()
                .attributes()
                .getOrDefault(
                    MoleculeStereoEditing.REVIEW,
                    "Назначение конфигурации — введённые пользователем данные."));
    review.setWrapText(true);
    review.setMinHeight(Region.USE_PREF_SIZE);
    review.setId("chemistry-stereo-review");
    this.reviewed.setVisible(
        this.drawing.molecule().metadata().attributes().containsKey(MoleculeStereoEditing.REVIEW));
    this.reviewed.setManaged(this.reviewed.isVisible());
    this.reviewed.setId("chemistry-stereo-reviewed");
    this.error.setWrapText(true);
    this.error.setId("chemistry-stereo-error");
    this.error.setStyle("-fx-text-fill: -app-danger-text;");
    this.error.visibleProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.managedProperty().bind(this.error.visibleProperty());
    final Label heading = label("Стереохимия · " + this.drawing.molecule().displayName());
    heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    final VBox fields =
        new VBox(
            10,
            label("Сохранённые назначения"),
            this.existing,
            create,
            new FlowPane(
                12, 8, field("Тип центра", this.kind), field("Конфигурация", this.descriptor)),
            new FlowPane(
                12, 8, field("Первый / центральный атом", this.primary), this.secondaryField));
    final FlowPane references = new FlowPane(12, 8);
    references.getChildren().setAll(this.referenceFields);
    fields.getChildren().addAll(references, review, this.reviewed);
    fields.setMinWidth(0);
    fields.setPrefWidth(490);
    fields.setMaxWidth(Double.MAX_VALUE);
    this.preview.setId("chemistry-stereo-preview");
    final StackPane drawingPane = new StackPane(this.preview);
    this.preview.setManaged(false);
    drawingPane.setMinWidth(0);
    drawingPane.setMinHeight(280);
    drawingPane.setPrefHeight(300);
    drawingPane.setStyle(
        "-fx-background-color: white; -fx-border-color: #dce3eb; -fx-background-radius: 8; -fx-border-radius: 8;");
    this.preview.widthProperty().bind(drawingPane.widthProperty());
    this.preview.heightProperty().bind(drawingPane.heightProperty());
    this.preview.widthProperty().addListener((o, a, b) -> this.paint());
    this.preview.heightProperty().addListener((o, a, b) -> this.paint());
    this.picking.setWrapText(true);
    this.picking.setMinHeight(Region.USE_PREF_SIZE);
    this.picking.setId("chemistry-stereo-picking");
    final Button fit = new Button("Вписать"), larger = new Button("+"), smaller = new Button("−");
    fit.setId("chemistry-stereo-fit");
    larger.setId("chemistry-stereo-zoom-in");
    smaller.setId("chemistry-stereo-zoom-out");
    fit.setOnAction(
        event -> {
          this.zoom = 1;
          this.panX = 0;
          this.panY = 0;
          this.paint();
        });
    larger.setOnAction(
        event -> this.zoom(1.25, this.preview.getWidth() / 2, this.preview.getHeight() / 2));
    smaller.setOnAction(
        event -> this.zoom(0.8, this.preview.getWidth() / 2, this.preview.getHeight() / 2));
    larger.setAccessibleText("Увеличить рисунок");
    smaller.setAccessibleText("Уменьшить рисунок");
    this.numbers.setId("chemistry-stereo-numbers");
    this.numbers.setOnAction(event -> this.paint());
    final VBox picture =
        new VBox(
            8,
            drawingPane,
            new FlowPane(8, 6, fit, smaller, larger, this.numbers),
            this.picking,
            label(
                "Выберите поле, затем атом на рисунке. Колесо — масштаб; средняя кнопка — перемещение. Показана сохранённая структура, ввод применяется отдельно."));
    picture.setMinWidth(0);
    picture.setPrefWidth(380);
    final HBox columns = new HBox(20, picture, fields);
    HBox.setHgrow(picture, Priority.ALWAYS);
    HBox.setHgrow(fields, Priority.ALWAYS);
    final TitledPane help = new TitledPane("Правила назначения", hint);
    help.setExpanded(false);
    help.setAnimated(false);
    help.setId("chemistry-stereo-help");
    final VBox body = new VBox(14, heading, explanation, columns, help);
    body.setPadding(new javafx.geometry.Insets(18));
    body.setMinHeight(Region.USE_PREF_SIZE);
    body.setMaxWidth(1120);
    final StackPane sheet = new StackPane(body);
    StackPane.setAlignment(body, javafx.geometry.Pos.TOP_CENTER);
    final ScrollPane scroll = new ScrollPane(sheet);
    scroll.setFitToWidth(true);
    scroll.setId("chemistry-stereo-scroll");
    this.setCenter(scroll);
    final Button apply = new Button("Применить запись"),
        remove = new Button("Удалить запись"),
        back = new Button("Назад");
    apply.setId("chemistry-stereo-apply");
    remove.setId("chemistry-stereo-remove");
    back.setId("chemistry-stereo-back");
    apply.getStyleClass().add("laboratory-save");
    remove
        .disableProperty()
        .bind(this.existing.getSelectionModel().selectedItemProperty().isNull());
    apply.setOnAction(
        event ->
            this.commit(
                () ->
                    MoleculeStereoEditing.assign(
                        this.drawing,
                        this.existing.getSelectionModel().getSelectedItem(),
                        this.center(),
                        this.reviewed.isSelected())));
    remove.setOnAction(
        event ->
            this.commit(
                () ->
                    MoleculeStereoEditing.remove(
                        this.drawing,
                        this.existing.getSelectionModel().getSelectedItem(),
                        this.reviewed.isSelected())));
    back.setOnAction(
        event -> {
          if (this.canClose()) {
            this.close.run();
          }
        });
    final FlowPane actions = new FlowPane(10, 8, apply, remove, back);
    if (this.reviewed.isVisible()) {
      final Button confirm = new Button("Подтвердить существующие записи");
      confirm.setId("chemistry-stereo-confirm");
      actions.getChildren().add(confirm);
      final List<javafx.beans.Observable> dependencies =
          new ArrayList<>(
              List.of(
                  this.reviewed.selectedProperty(),
                  this.loadedFields,
                  this.kind.valueProperty(),
                  this.descriptor.valueProperty(),
                  this.primary.valueProperty(),
                  this.secondary.valueProperty()));
      this.references.forEach(box -> dependencies.add(box.valueProperty()));
      confirm
          .disableProperty()
          .bind(
              javafx.beans.binding.Bindings.createBooleanBinding(
                  () ->
                      !this.reviewed.isSelected() || !this.fields().equals(this.loadedFields.get()),
                  dependencies.toArray(javafx.beans.Observable[]::new)));
      confirm.setOnAction(
          event -> this.commit(() -> MoleculeStereoEditing.confirmReviewed(this.drawing)));
    }
    final Button discard = new Button("Отменить ввод"), keep = new Button("Продолжить заполнение");
    discard.setId("chemistry-stereo-discard");
    discard.setOnAction(
        event -> {
          this.applied = true;
          this.close.run();
        });
    keep.setOnAction(
        event -> {
          this.confirmation.setVisible(false);
          this.confirmation.setManaged(false);
          this.error.setText("");
        });
    this.confirmation.getChildren().setAll(label("Изменения ещё не применены."), discard, keep);
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
    final VBox footer = new VBox(8, this.error, this.confirmation, actions);
    footer.setPadding(new javafx.geometry.Insets(12, 18, 12, 18));
    this.setBottom(footer);
    this.targetNames.put(this.primary, "Первый / центральный атом");
    this.targetNames.put(this.secondary, "Второй атом оси / связи");
    for (int i = 0; i < this.references.size(); i++) {
      this.targetNames.put(this.references.get(i), "Опорный атом " + (i + 1));
    }
    this.targetNames.forEach(
        (box, name) -> {
          box.focusedProperty()
              .addListener(
                  (o, a, focused) -> {
                    if (focused) {
                      this.target = box;
                      this.paint();
                    }
                  });
          box.valueProperty().addListener((o, a, b) -> this.paint());
          box.setOnMousePressed(
              event -> {
                this.target = box;
                this.paint();
              });
        });
    this.target = this.primary;
    this.preview.setOnScroll(
        event -> {
          if (event.getDeltaY() != 0) {
            this.zoom(
                Math.exp(Math.max(-0.5, Math.min(0.5, event.getDeltaY() * 0.004))),
                event.getX(),
                event.getY());
          }
          event.consume();
        });
    this.preview.setOnMouseMoved(
        event -> {
          this.hovered = this.nearest(event.getX(), event.getY());
          this.paint();
        });
    this.preview.setOnMouseExited(
        event -> {
          this.hovered = null;
          this.paint();
        });
    this.preview.setOnMousePressed(
        event -> {
          if (event.getButton() == javafx.scene.input.MouseButton.MIDDLE) {
            this.pressX = event.getX();
            this.pressY = event.getY();
            this.startPanX = this.panX;
            this.startPanY = this.panY;
            event.consume();
          }
        });
    this.preview.setOnMouseDragged(
        event -> {
          if (event.isMiddleButtonDown()) {
            this.panX = this.startPanX + event.getX() - this.pressX;
            this.panY = this.startPanY + event.getY() - this.pressY;
            this.paint();
            event.consume();
          }
        });
    this.preview.setOnMouseClicked(
        event -> {
          if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
            return;
          }
          final var atom = this.nearest(event.getX(), event.getY());
          if (atom != null) {
            choose(this.target, atom);
          }
          this.paint();
        });
    final var selectedCenter =
        this.existing.getItems().stream()
            .filter(
                center ->
                    center.primaryAtomId().equals(selected)
                        || Objects.equals(center.secondaryAtomId(), selected))
            .findFirst();
    if (selectedCenter.isPresent()) {
      this.existing.getSelectionModel().select(selectedCenter.get());
    } else {
      this.loading = true;
      this.kind.setValue(
          second == null ? StereocenterKind.TETRAHEDRAL_ATOM : StereocenterKind.DOUBLE_BOND);
      choose(this.primary, selected);
      choose(this.secondary, second);
      this.loading = false;
      this.configure();
      this.suggest();
    }
    this.loadedFields.set(this.fields());
    this.initial = this.state();
    this.paint();
  }

  private String thisLabel(final AtomId atom) {
    final var value = this.atomsById.get(atom);
    return value == null ? "Атом отсутствует" : value.label();
  }

  private AtomId nearest(final double x, final double y) {
    return this.screenAtoms.entrySet().stream()
        .filter(entry -> entry.getValue().distance(x, y) <= 18)
        .min(Comparator.comparingDouble(entry -> entry.getValue().distance(x, y)))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  private void zoom(final double factor, final double x, final double y) {
    final double next = Math.max(0.25, Math.min(12, this.zoom * factor)), ratio = next / this.zoom;
    this.panX =
        x - this.preview.getWidth() / 2 - ratio * (x - this.preview.getWidth() / 2 - this.panX);
    this.panY =
        y - this.preview.getHeight() / 2 - ratio * (y - this.preview.getHeight() / 2 - this.panY);
    this.zoom = next;
    this.paint();
  }

  private void paint() {
    if (this.target == null) {
      return;
    }
    final var graphics = this.preview.getGraphicsContext2D();
    final double width = this.preview.getWidth(), height = this.preview.getHeight();
    graphics.setTransform(1, 0, 0, 1, 0, 0);
    graphics.clearRect(0, 0, width, height);
    this.screenAtoms.clear();
    final double
        minX = this.drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).min().orElse(0),
        maxX = this.drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).max().orElse(0);
    final double
        minY = this.drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).min().orElse(0),
        maxY = this.drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).max().orElse(0);
    final double scale =
        Math.max(
                0.01,
                Math.min(
                    1.3,
                    Math.min(
                        (width - 84) / Math.max(80, maxX - minX),
                        (height - 84) / Math.max(80, maxY - minY))))
            * this.zoom;
    final double dx = width / 2 + this.panX - scale * (minX + maxX) / 2,
        dy = height / 2 + this.panY - scale * (minY + maxY) / 2;
    graphics.save();
    graphics.translate(dx, dy);
    graphics.scale(scale, scale);
    LaboratoryCanvas.paintMolecule(graphics, this.drawing, id(this.target));
    graphics.restore();
    graphics.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
    graphics.setFill(javafx.scene.paint.Color.web("#52728e"));
    for (final var atom : this.drawing.atoms()) {
      final var point =
          new javafx.geometry.Point2D(
              dx + scale * atom.point2D().x(), dy + scale * atom.point2D().y());
      this.screenAtoms.put(atom.atomId(), point);
      final int number = this.atomNumbers.get(atom.atomId());
      if (this.numbers.isSelected()
          || atom.atomId().equals(id(this.target))
          || atom.atomId().equals(this.hovered)) {
        graphics.fillText(Integer.toString(number), point.getX() + 12, point.getY() - 10);
      }
      if (atom.atomId().equals(this.hovered)) {
        graphics.setStroke(javafx.scene.paint.Color.web("#52728e"));
        graphics.strokeOval(point.getX() - 13, point.getY() - 13, 26, 26);
      }
    }
    this.picking.setText(
        "Выбор на рисунке: "
            + this.targetNames.get(this.target)
            + " · "
            + (id(this.target) == null ? "не выбран" : this.thisLabel(id(this.target))));
  }

  private void commit(final java.util.function.Supplier<MoleculeDrawing> operation) {
    if (this.applied) {
      return;
    }
    try {
      if (this.recoveredConflict || !this.current()) {
        throw new IllegalArgumentException(
            "Структура изменена во время ввода. Вернитесь к опыту и откройте стереохимию заново.");
      }
      final var result = operation.get();
      final var component =
          this.editor.document().experiment().components().stream()
              .filter(c -> c.id().equals(this.componentId))
              .findFirst()
              .orElseThrow();
      this.editor.replace(this.componentId, result, component.role());
      this.applied = true;
      this.close.run();
    } catch (final RuntimeException exception) {
      this.error.setText(exception.getMessage());
    }
  }

  private boolean current() {
    return this.experimentId.equals(this.editor.document().experiment().id())
        && this.drawing.equals(this.editor.document().layouts().get(this.componentId));
  }

  private String state() {
    return this.fields()
        + ":"
        + this.existing.getSelectionModel().getSelectedIndex()
        + ":"
        + this.reviewed.isSelected();
  }

  boolean pending() {
    return !this.applied && (this.recoveredConflict || !this.state().equals(this.initial));
  }

  boolean canClose() {
    if (!this.pending()) {
      return true;
    }
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
    this.error.setText("Примените запись или явно отмените ввод перед закрытием.");
    return false;
  }

  private String fingerprint() {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(
                      new LaboratoryFileCodec()
                          .write(this.editor.document())
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  void draft(final Properties fields, final boolean restore) {
    if (!restore) {
      fields.setProperty("stereo.component", this.componentId);
      fields.setProperty("stereo.experiment", this.experimentId);
      fields.setProperty("stereo.source", this.fingerprint());
      fields.setProperty(
          "stereo.conflict", Boolean.toString(this.recoveredConflict || !this.current()));
      fields.setProperty("stereo.kind", this.kind.getValue().name());
      fields.setProperty("stereo.descriptor", this.descriptor.getValue().name());
      fields.setProperty(
          "stereo.existing",
          Integer.toString(this.existing.getSelectionModel().getSelectedIndex()));
      fields.setProperty("stereo.reviewed", Boolean.toString(this.reviewed.isSelected()));
      final var boxes = new ArrayList<>(this.targetNames.keySet());
      for (int i = 0; i < boxes.size(); i++) {
        fields.setProperty(
            "stereo.atom." + i, id(boxes.get(i)) == null ? "" : id(boxes.get(i)).value());
      }
      return;
    }
    if (!this.componentId.equals(fields.getProperty("stereo.component"))
        || !this.experimentId.equals(fields.getProperty("stereo.experiment"))) {
      throw new IllegalArgumentException(
          "Черновик стереохимии относится к другому компоненту или опыту.");
    }
    final var restoredKind = StereocenterKind.valueOf(fields.getProperty("stereo.kind"));
    final var restoredDescriptor =
        StereochemicalDescriptor.valueOf(fields.getProperty("stereo.descriptor"));
    if (!descriptors(restoredKind).contains(restoredDescriptor)) {
      throw new IllegalArgumentException("Конфигурация не соответствует типу центра в черновике.");
    }
    for (final String key : List.of("stereo.reviewed", "stereo.conflict")) {
      if (!"true".equals(fields.getProperty(key)) && !"false".equals(fields.getProperty(key))) {
        throw new IllegalArgumentException("Повреждено поле черновика: " + key);
      }
    }
    final int selected = Integer.parseInt(fields.getProperty("stereo.existing"));
    final var boxes = new ArrayList<>(this.targetNames.keySet());
    final List<AtomId> ids = new ArrayList<>();
    for (int i = 0; i < boxes.size(); i++) {
      final String value = Objects.requireNonNull(fields.getProperty("stereo.atom." + i));
      final AtomId id = value.isEmpty() ? null : AtomId.of(value);
      if (id != null && !this.atomsById.containsKey(id)) {
        throw new IllegalArgumentException(
            "В черновике есть отсутствующий атом. Исходная структура необходима для восстановления.");
      }
      ids.add(id);
    }
    if (selected < -1 || selected >= this.existing.getItems().size()) {
      throw new IllegalArgumentException("Исходная стереозапись черновика отсутствует.");
    }
    this.loading = true;
    try {
      this.existing.getSelectionModel().select(selected);
      if (selected >= 0) {
        this.load(this.existing.getItems().get(selected));
        this.loading = true;
      }
      this.kind.setValue(restoredKind);
      this.configure();
      this.descriptor.setValue(restoredDescriptor);
      for (int i = 0; i < boxes.size(); i++) {
        choose(boxes.get(i), ids.get(i));
      }
      this.reviewed.setSelected(Boolean.parseBoolean(fields.getProperty("stereo.reviewed")));
    } finally {
      this.loading = false;
    }
    this.recoveredConflict =
        Boolean.parseBoolean(fields.getProperty("stereo.conflict"))
            || !this.fingerprint().equals(fields.getProperty("stereo.source"));
    this.error.setText(
        this.recoveredConflict
            ? "Черновик восстановлен для просмотра: исходная структура изменилась."
            : "Восстановлен незавершённый ввод. Проверьте и примените запись.");
    this.paint();
  }

  private static Label label(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  private void load(final Stereocenter center) {
    this.loading = true;
    this.kind.setValue(center.kind());
    this.configure();
    this.descriptor.setValue(center.descriptor());
    choose(this.primary, center.primaryAtomId());
    choose(this.secondary, center.secondaryAtomId());
    for (int index = 0; index < this.references.size(); index++) {
      choose(
          this.references.get(index),
          index < center.referenceAtomIds().size() ? center.referenceAtomIds().get(index) : null);
    }
    this.loading = false;
    this.loadedFields.set(this.fields());
  }

  private String fields() {
    final List<String> fields =
        new ArrayList<>(
            List.of(
                Objects.toString(this.kind.getValue(), ""),
                Objects.toString(this.descriptor.getValue(), ""),
                Objects.toString(id(this.primary), ""),
                Objects.toString(id(this.secondary), "")));
    this.references.forEach(box -> fields.add(Objects.toString(id(box), "")));
    return fields.toString();
  }

  private void configure() {
    final var type = this.kind.getValue();
    this.descriptor.getItems().setAll(descriptors(type));
    this.descriptor.setValue(StereochemicalDescriptor.UNKNOWN);
    final boolean second =
        type == StereocenterKind.DOUBLE_BOND
            || type == StereocenterKind.AXIAL
            || type == StereocenterKind.HELICAL;
    this.secondaryField.setVisible(second);
    this.secondaryField.setManaged(second);
    for (int index = 0; index < this.referenceFields.size(); index++) {
      this.referenceFields.get(index).setVisible(index < count(type));
      this.referenceFields.get(index).setManaged(index < count(type));
    }
    if (this.target != null
        && (this.target == this.secondary && !this.secondaryField.isManaged()
            || this.references.contains(this.target)
                && !this.referenceFields.get(this.references.indexOf(this.target)).isManaged())) {
      this.target = this.primary;
    }
  }

  private static List<StereochemicalDescriptor> descriptors(final StereocenterKind type) {
    return switch (type) {
      case TETRAHEDRAL_ATOM ->
          List.of(
              StereochemicalDescriptor.R,
              StereochemicalDescriptor.S,
              StereochemicalDescriptor.D,
              StereochemicalDescriptor.L,
              StereochemicalDescriptor.UNKNOWN);
      case DOUBLE_BOND ->
          List.of(
              StereochemicalDescriptor.E,
              StereochemicalDescriptor.Z,
              StereochemicalDescriptor.CIS,
              StereochemicalDescriptor.TRANS,
              StereochemicalDescriptor.UNKNOWN);
      case AXIAL ->
          List.of(
              StereochemicalDescriptor.R_A,
              StereochemicalDescriptor.S_A,
              StereochemicalDescriptor.P,
              StereochemicalDescriptor.M,
              StereochemicalDescriptor.UNKNOWN);
      case HELICAL ->
          List.of(
              StereochemicalDescriptor.P,
              StereochemicalDescriptor.M,
              StereochemicalDescriptor.DELTA,
              StereochemicalDescriptor.LAMBDA,
              StereochemicalDescriptor.UNKNOWN);
      case PLANAR ->
          List.of(
              StereochemicalDescriptor.R,
              StereochemicalDescriptor.S,
              StereochemicalDescriptor.UNKNOWN);
    };
  }

  private void suggest() {
    final var type = this.kind.getValue();
    if (type != StereocenterKind.TETRAHEDRAL_ATOM && type != StereocenterKind.DOUBLE_BOND) {
      return;
    }
    final AtomId first = id(this.primary), second = id(this.secondary);
    if (first == null) {
      return;
    }
    final var neighbors =
        this.neighbors(first, type == StereocenterKind.TETRAHEDRAL_ATOM ? null : second);
    if (type == StereocenterKind.TETRAHEDRAL_ATOM) {
      for (int index = 0; index < this.references.size(); index++) {
        choose(this.references.get(index), index < neighbors.size() ? neighbors.get(index) : null);
      }
    } else {
      choose(this.references.get(0), neighbors.isEmpty() ? null : neighbors.getFirst());
      final var other = second == null ? List.<AtomId>of() : this.neighbors(second, first);
      choose(this.references.get(1), other.isEmpty() ? null : other.getFirst());
    }
  }

  private List<AtomId> neighbors(final AtomId atom, final AtomId excluded) {
    return this.drawing.molecule().bonds().stream()
        .filter(bond -> bond.firstAtomId().equals(atom) || bond.secondAtomId().equals(atom))
        .map(bond -> bond.firstAtomId().equals(atom) ? bond.secondAtomId() : bond.firstAtomId())
        .filter(id -> !id.equals(excluded))
        .sorted(Comparator.comparing(AtomId::value))
        .toList();
  }

  private Stereocenter center() {
    final List<AtomId> refs = new ArrayList<>();
    for (int index = 0; index < count(this.kind.getValue()); index++) {
      if (id(this.references.get(index)) == null) {
        throw new IllegalArgumentException("Выберите опорный атом " + (index + 1) + ".");
      }
      refs.add(id(this.references.get(index)));
    }
    return Stereocenter.of(
        this.kind.getValue(),
        this.descriptor.getValue(),
        id(this.primary),
        this.secondaryField.isManaged() ? id(this.secondary) : null,
        refs);
  }

  private static int count(final StereocenterKind kind) {
    return switch (kind) {
      case TETRAHEDRAL_ATOM -> 4;
      case DOUBLE_BOND, AXIAL -> 2;
      case HELICAL, PLANAR -> 3;
    };
  }

  private static AtomId id(final ComboBox<AtomChoice> box) {
    return box.getValue() == null ? null : box.getValue().id();
  }

  private static void choose(final ComboBox<AtomChoice> box, final AtomId id) {
    box.setValue(
        box.getItems().stream().filter(atom -> atom.id().equals(id)).findFirst().orElse(null));
  }

  private static VBox field(final String name, final Control control) {
    control.setPrefWidth(230);
    return new VBox(4, new Label(name), control);
  }

  private static String kindName(final StereocenterKind kind) {
    return switch (kind) {
      case TETRAHEDRAL_ATOM -> "Тетраэдрический атом";
      case DOUBLE_BOND -> "Двойная связь";
      case AXIAL -> "Аксиальная";
      case HELICAL -> "Спиральная";
      case PLANAR -> "Плоскостная";
    };
  }
}
