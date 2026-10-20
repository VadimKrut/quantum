/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Pickable scaffold palette with explicit attachment/H replacement and immutable previews. */
final class LaboratoryScaffoldPage extends LaboratoryComputationPage {
  private record AtomChoice(AtomId id, int index, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private final TextField search = new TextField();
  private final ComboBox<LaboratoryScaffolds.Family> family = new ComboBox<>();
  private final ListView<LaboratoryScaffolds.Scaffold> palette = new ListView<>();
  private final ComboBox<AtomChoice> anchor = new ComboBox<>();
  private final ComboBox<Integer> direction = new ComboBox<>();
  private final CheckBox
      replaceTargetH = new CheckBox("Заменить один явный H в исходной структуре"),
      replaceScaffoldH = new CheckBox("Заменить один явный H в каркасе");
  private final LaboratoryStructurePreview scaffoldPreview = new LaboratoryStructurePreview(),
      resultPreview = new LaboratoryStructurePreview();
  private final TextArea diagnostics = new TextArea();
  private final Label source = note("");
  private final Button apply;
  private final MoleculeDrawing target;
  private final AtomId targetAtom;
  private final Consumer<MoleculeDrawing> accept;
  private final Map<String, LaboratoryScaffolds.Prepared> cache = new HashMap<>();
  private final java.util.concurrent.ExecutorService worker =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          r -> {
            final Thread thread = new Thread(r, "chemistry-scaffold-preview");
            thread.setDaemon(true);
            return thread;
          });
  private javafx.concurrent.Task<?> task;
  private LaboratoryScaffolds.Prepared scaffold;
  private MoleculeDrawing proposed;
  private String activeId;
  private long generation;
  private boolean changing;
  private int restoredAnchor = -1;

  LaboratoryScaffoldPage(
      final LaboratoryDrawing snapshot,
      final String targetComponentId,
      final AtomId targetAtom,
      final Consumer<MoleculeDrawing> accept,
      final Runnable close) {
    super("Каркасы для построения структур", close);
    this.setId("chemistry-scaffold-page");
    this.targetAtom = targetAtom;
    this.accept = accept;
    if ((targetComponentId == null) != (targetAtom == null)) {
      throw new IllegalArgumentException(
          "Для присоединения каркаса выберите один атом исходной структуры.");
    }
    this.target = targetComponentId == null ? null : snapshot.layouts().get(targetComponentId);
    if (targetComponentId != null
        && (this.target == null
            || this.target.molecule().atoms().stream()
                .noneMatch(
                    a -> a.id().equals(targetAtom) && !a.element().symbol().value().equals("H")))) {
      throw new IllegalArgumentException(
          "Для присоединения выберите тяжёлый атом существующей структуры.");
    }
    this.apply =
        new Button(
            target == null ? "Добавить отдельную структуру" : "Присоединить выбранный каркас");
    this.apply.setId("chemistry-scaffold-apply");
    this.apply.setDisable(true);
    this.apply.setOnAction(
        e -> {
          if (proposed == null || running || disposed) {
            return;
          }
          try {
            accept.accept(proposed);
            dispose();
          } catch (final RuntimeException exception) {
            status.setText(exception.getMessage());
          }
        });
    this.family.getItems().setAll(LaboratoryScaffolds.Family.values());
    this.family.setPromptText("Все семейства");
    this.search.setPromptText("Поиск каркаса по названию");
    this.search.setId("chemistry-scaffold-search");
    final var all = new Button("Все");
    all.setOnAction(e -> family.setValue(null));
    final var sidebar = new VBox(8, this.search, new HBox(8, this.family, all), this.palette);
    sidebar.setPadding(new Insets(10));
    sidebar.setPrefWidth(300);
    sidebar.setMinWidth(200);
    VBox.setVgrow(this.palette, Priority.ALWAYS);
    this.palette.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(
                  final LaboratoryScaffolds.Scaffold value, final boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(
                    empty || value == null ? null : note(value.title() + "\n" + value.family()));
              }
            });
    this.palette.setPlaceholder(note("Нет каркасов по этому фильтру"));
    this.search.textProperty().addListener((o, a, b) -> filter());
    this.family.valueProperty().addListener((o, a, b) -> filter());
    this.palette
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing && b != null) {
                load(b);
              }
            });
    this.scaffoldPreview.hint(
        target == null
            ? "Каркас содержит явный граф и все атомы H."
            : "Нажмите на тяжёлый атом каркаса, к которому нужно присоединить исходную структуру.");
    this.scaffoldPreview.onPick(
        id -> {
          if (target != null) {
            anchor.setValue(
                anchor.getItems().stream()
                    .filter(a -> a.id().equals(id))
                    .findFirst()
                    .orElse(anchor.getValue()));
          }
        });
    this.resultPreview.hint("Предпросмотр объединённой структуры до применения");
    this.anchor.setId("chemistry-scaffold-anchor");
    this.anchor.setMaxWidth(Double.MAX_VALUE);
    this.anchor
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                compose();
              }
            });
    this.direction.getItems().setAll(0, 45, 90, 135, 180, 225, 270, 315);
    this.direction.setValue(
        target == null ? 0 : MoleculeFragmentPlacement.suggestedDirection(target, targetAtom));
    this.direction
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                compose();
              }
            });
    this.replaceTargetH.setSelected(true);
    this.replaceScaffoldH.setSelected(true);
    this.replaceTargetH
        .selectedProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                compose();
              }
            });
    this.replaceScaffoldH
        .selectedProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                compose();
              }
            });
    final Button rotate = new Button("Направление ↻");
    rotate.setOnAction(e -> direction.setValue((direction.getValue() + 45) % 360));
    final var controls =
        new VBox(
            8,
            note(
                target == null
                    ? "Каркас добавляется новым компонентом. Положение, замещение и стереохимию можно доработать в основном редакторе."
                    : "Исходная структура: "
                        + target.molecule().displayName()
                        + ". Новая одинарная связь соединит выбранный атом с точкой каркаса."),
            this.source,
            this.scaffoldPreview);
    if (target != null) {
      controls
          .getChildren()
          .addAll(
              new FlowPane(
                  8,
                  8,
                  note("Точка каркаса"),
                  this.anchor,
                  note("Направление, °"),
                  this.direction,
                  rotate),
              this.replaceTargetH,
              this.replaceScaffoldH,
              this.resultPreview);
    }
    this.diagnostics.setEditable(false);
    this.diagnostics.setWrapText(true);
    this.diagnostics.setPrefRowCount(4);
    controls
        .getChildren()
        .addAll(
            this.diagnostics,
            note(
                "Это построение явного графа, а не прогноз реакции. Неуказанная стереохимия не назначается автоматически. Собственные фрагменты доступны в библиотеке."));
    controls.setPadding(new Insets(12));
    final var split = new SplitPane(sidebar, scroll(controls));
    split.setDividerPositions(0.3);
    sidebar.setMinHeight(0);
    this.palette.setMinHeight(80);
    this.widthProperty()
        .addListener(
            o -> {
              final boolean narrow = getWidth() < 900;
              final var direction =
                  narrow
                      ? javafx.geometry.Orientation.VERTICAL
                      : javafx.geometry.Orientation.HORIZONTAL;
              if (split.getOrientation() != direction) {
                split.setOrientation(direction);
                split.setDividerPositions(narrow ? .26 : .3);
              }
            });
    this.setCenter(split);
    final var footer = new VBox(8, this.status, this.apply);
    footer.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(footer);
    this.filter();
    this.palette.getSelectionModel().selectFirst();
  }

  private void filter() {
    final String query = search.getText().strip().toLowerCase(Locale.ROOT);
    final String selected =
        palette.getSelectionModel().getSelectedItem() == null
            ? null
            : palette.getSelectionModel().getSelectedItem().id();
    changing = true;
    palette
        .getItems()
        .setAll(
            LaboratoryScaffolds.PALETTE.stream()
                .filter(
                    s ->
                        (family.getValue() == null || s.family() == family.getValue())
                            && (s.title() + " " + s.chemicalName())
                                .toLowerCase(Locale.ROOT)
                                .contains(query))
                .toList());
    palette
        .getSelectionModel()
        .select(
            palette.getItems().stream()
                .filter(s -> s.id().equals(selected))
                .findFirst()
                .orElse(null));
    changing = false;
  }

  private void load(final LaboratoryScaffolds.Scaffold selected) {
    cancel();
    scaffold = null;
    proposed = null;
    apply.setDisable(true);
    activeId = selected.id();
    source.setText(selected.title() + " · " + selected.chemicalName());
    busy(true);
    status.setText("Строю явный граф и 2D-раскладку каркаса…");
    final long token = generation;
    final var cached = cache.get(selected.id());
    if (cached != null) {
      loaded(cached);
      return;
    }
    final var loading =
        new javafx.concurrent.Task<LaboratoryScaffolds.Prepared>() {
          @Override
          protected LaboratoryScaffolds.Prepared call() {
            return LaboratoryScaffolds.prepare(selected);
          }
        };
    task = loading;
    loading.setOnSucceeded(
        e -> {
          if (!disposed && token == generation) {
            cache.put(selected.id(), loading.getValue());
            loaded(loading.getValue());
          }
        });
    loading.setOnFailed(
        e -> {
          if (!disposed && token == generation) {
            busy(false);
            status.setText(
                "Не удалось построить этот каркас: " + loading.getException().getMessage());
            diagnostics.setText(
                "Исходная структура не изменена. Выберите другой каркас или введите название через инструмент «Название → структура».");
          }
        });
    worker.submit(loading);
  }

  private void loaded(final LaboratoryScaffolds.Prepared result) {
    task = null;
    scaffold = result;
    changing = true;
    final var values = new ArrayList<AtomChoice>();
    for (int i = 0; i < result.drawing().molecule().atoms().size(); i++) {
      final var atom = result.drawing().molecule().atoms().get(i);
      if (!atom.element().symbol().value().equals("H")) {
        values.add(
            new AtomChoice(
                atom.id(),
                i,
                atom.element().symbol().value()
                    + " · атом "
                    + (i + 1)
                    + (LaboratoryScaffolds.hasReplaceableHydrogen(result.drawing(), atom.id())
                        ? " · есть H"
                        : "")));
      }
    }
    anchor.getItems().setAll(values);
    anchor.setValue(
        values.stream()
            .filter(
                a ->
                    restoredAnchor >= 0
                        ? a.index() == restoredAnchor
                        : LaboratoryScaffolds.hasReplaceableHydrogen(result.drawing(), a.id()))
            .findFirst()
            .orElse(values.isEmpty() ? null : values.getFirst()));
    restoredAnchor = -1;
    changing = false;
    scaffoldPreview.show(
        result.drawing(),
        target == null || anchor.getValue() == null ? null : anchor.getValue().id());
    compose();
  }

  private void compose() {
    if (disposed || scaffold == null) {
      return;
    }
    cancel();
    proposed = null;
    apply.setDisable(true);
    final var current = scaffold;
    final var selected = anchor.getValue();
    scaffoldPreview.show(
        current.drawing(), target == null || selected == null ? null : selected.id());
    if (target == null) {
      busy(false);
      proposed = current.drawing();
      diagnostics.setText(String.join("\n", current.notes()));
      status.setText("Каркас готов. Проверьте рисунок и добавьте его в опыт.");
      apply.setDisable(false);
      return;
    }
    if (selected == null) {
      busy(false);
      status.setText("Выберите тяжёлый атом каркаса.");
      return;
    }
    final boolean firstH = replaceTargetH.isSelected(), secondH = replaceScaffoldH.isSelected();
    final int angle = direction.getValue();
    final long token = generation;
    busy(true);
    status.setText("Готовлю присоединение каркаса…");
    final var loading =
        new javafx.concurrent.Task<LaboratoryScaffolds.Prepared>() {
          @Override
          protected LaboratoryScaffolds.Prepared call() {
            return LaboratoryScaffolds.attach(
                target, targetAtom, current.drawing(), selected.id(), firstH, secondH, angle);
          }
        };
    task = loading;
    loading.setOnSucceeded(
        e -> {
          if (disposed || token != generation) {
            return;
          }
          busy(false);
          task = null;
          proposed = loading.getValue().drawing();
          resultPreview.show(proposed, targetAtom);
          diagnostics.setText(String.join("\n", loading.getValue().notes()));
          status.setText(
              "Предпросмотр готов. Изменение применится одной операцией с возможностью отмены.");
          apply.setDisable(false);
        });
    loading.setOnFailed(
        e -> {
          if (disposed || token != generation) {
            return;
          }
          busy(false);
          task = null;
          resultPreview.show(target, targetAtom);
          status.setText("Присоединение пока недоступно.");
          diagnostics.setText(loading.getException().getMessage());
        });
    worker.submit(loading);
  }

  private void cancel() {
    generation++;
    if (task != null) {
      task.cancel(true);
      task = null;
    }
  }

  boolean pending() {
    return running;
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    cancel();
    worker.shutdownNow();
    busy(false);
  }

  void draft(final Properties data, final boolean restore) {
    changing = true;
    LaboratoryFormDraft.fields(
        data,
        "scaffolds",
        restore,
        Map.of(
            "search",
            search,
            "family",
            family,
            "targetH",
            replaceTargetH,
            "scaffoldH",
            replaceScaffoldH));
    if (restore) {
      final int angle = Integer.parseInt(data.getProperty("scaffolds.direction", "0"));
      direction.setValue(direction.getItems().contains(angle) ? angle : 0);
      restoredAnchor = Integer.parseInt(data.getProperty("scaffolds.anchorIndex", "-1"));
      filter();
      final var selected =
          palette.getItems().stream()
              .filter(s -> s.id().equals(data.getProperty("scaffolds.id")))
              .findFirst()
              .orElse(palette.getItems().isEmpty() ? null : palette.getItems().getFirst());
      changing = false;
      if (selected != null) {
        palette.getSelectionModel().select(selected);
        load(selected);
      }
    } else {
      data.setProperty("scaffolds.id", Objects.toString(activeId, ""));
      data.setProperty("scaffolds.direction", Integer.toString(direction.getValue()));
      data.setProperty(
          "scaffolds.anchorIndex",
          Integer.toString(anchor.getValue() == null ? -1 : anchor.getValue().index()));
      changing = false;
    }
  }
}
