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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.library.application.ChemistryLibraryService;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryQuery;
import ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.filesystem.ChemistryLibraryFileRepository;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryLibraryStore;

final class LaboratoryLibraryWindow {
  private final LaboratoryLibraryStore store = new LaboratoryLibraryStore();
  private final BorderPane page = new BorderPane();
  private Window owner;
  private Runnable close;
  private boolean active;
  private final ListView<ChemistryLibraryEntry> entries = new ListView<>();
  private final ComboBox<Molecule> molecules = new ComboBox<>();
  private final ComboBox<String> scope = new ComboBox<>();
  private final TextArea description = area(6);
  private final TextArea warnings = area(3);
  private final TextField search = new TextField();
  private final Label status = new Label();
  private final Label previewStatus = new Label();
  private final Label count = new Label();
  private final Canvas preview = new Canvas(420, 220);
  private final Button saveStructure = new Button("Сохранить структуру…");
  private final Button saveTemplate = new Button("Сохранить опыт как шаблон…");
  private final Button saveFragment = new Button("Сохранить фрагмент…");
  private final Button reload = new Button("Обновить");
  private List<ChemistryLibraryEntry> all = List.of();
  private Set<String> builtInIds = Set.of();
  private Task<?> loading;
  private boolean saving;
  private MoleculeDrawing selectedDrawing;
  private Button insertButton;
  private Button templateButton;
  private Button attachButton;
  private ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId targetAtom;
  private Path folder;
  private MoleculeDrawing sourceStructure;
  private LaboratoryDrawing current;
  private LaboratoryLibraryEntryPage entryPage;
  private LaboratoryFragmentPage fragmentPage;
  private javafx.scene.Node browserTop, browserCenter, browserBottom;

  javafx.scene.Node view(
      final Window owner,
      final Path folder,
      final MoleculeDrawing selected,
      final LaboratoryDrawing current,
      final Consumer<MoleculeDrawing> insert,
      final Predicate<LaboratoryDrawing> start,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId targetAtom,
      final Consumer<MoleculeDrawing> attach,
      final Runnable close) {
    this.owner = owner;
    this.close = close;
    this.active = true;
    this.page.setId("chemistry-library-page");
    this.targetAtom = targetAtom;
    this.folder = folder;
    this.sourceStructure = selected;
    this.current = current;
    this.page.setMinSize(0, 0);

    this.search.setId("chemistry-library-search");
    this.entries.setId("chemistry-library-entries");
    this.scope.setId("chemistry-library-scope");
    this.warnings.setId("chemistry-library-warnings");
    this.search.setPromptText("Название, описание или метка / синоним");
    this.scope
        .getItems()
        .setAll("Все записи", "Мои структуры", "Шаблоны опытов", "Встроенные", "Фрагменты");
    this.scope.getSelectionModel().selectFirst();
    this.search.textProperty().addListener((o, a, b) -> this.filter());
    this.scope.valueProperty().addListener((o, a, b) -> this.filter());
    this.entries.setPlaceholder(new Label("Нет записей по этому запросу"));
    this.entries.setCellFactory(
        list ->
            new ListCell<>() {
              protected void updateItem(final ChemistryLibraryEntry entry, final boolean empty) {
                super.updateItem(entry, empty);
                this.setWrapText(true);
                this.prefWidthProperty().bind(list.widthProperty().subtract(24));
                this.setMaxWidth(Region.USE_PREF_SIZE);
                this.setText(
                    empty || entry == null
                        ? null
                        : entry.displayName()
                            + "\n"
                            + kind(entry)
                            + (builtInIds.contains(entry.id())
                                ? " · встроенная"
                                : " · пользовательская"));
              }
            });
    this.molecules.setMaxWidth(Double.MAX_VALUE);
    this.molecules.setConverter(
        new StringConverter<>() {
          public String toString(final Molecule value) {
            return value == null ? "Нет структуры" : value.displayName();
          }

          public Molecule fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.warnings.setVisible(false);
    this.warnings.setManaged(false);
    this.status.setWrapText(true);
    this.status.setMinHeight(Region.USE_PREF_SIZE);
    this.insertButton = new Button("Добавить структуру");
    this.templateButton = new Button("Новый опыт из шаблона");
    this.attachButton = new Button("Присоединить фрагмент…");
    this.attachButton.setDisable(true);
    this.insertButton.setId("chemistry-library-insert");
    this.templateButton.setId("chemistry-library-template");
    this.attachButton.setId("chemistry-library-attach");
    final Button back = new Button("← К структурам");
    back.setId("chemistry-library-back");
    back.getStyleClass().add("laboratory-navigation-back");
    back.setOnAction(event -> this.finish());
    this.insertButton.setDisable(true);
    this.templateButton.setDisable(true);
    this.insertButton.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          if (this.saving) {
            event.consume();
            return;
          }
          try {
            insert.accept(this.selectedDrawing);
            this.finish();
          } catch (final RuntimeException exception) {
            event.consume();
            this.status.setText(exception.getMessage());
          }
        });
    this.templateButton.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          if (this.saving) {
            event.consume();
            return;
          }
          try {
            if (start.test(
                this.store.instantiate(this.entries.getSelectionModel().getSelectedItem()))) {
              this.finish();
            } else {
              event.consume();
            }
          } catch (final RuntimeException exception) {
            event.consume();
            this.status.setText(exception.getMessage());
          }
        });
    this.attachButton.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          if (this.saving || attach == null) {
            event.consume();
            return;
          }
          try {
            final var points =
                this.store.attachmentPoints(this.entries.getSelectionModel().getSelectedItem());
            this.fragmentPage =
                new LaboratoryFragmentPage(
                    this.sourceStructure,
                    this.targetAtom,
                    this.selectedDrawing,
                    points,
                    result -> {
                      attach.accept(result);
                      this.finish();
                    },
                    this::backToBrowser);
            this.showForm(this.fragmentPage);
          } catch (final RuntimeException exception) {
            event.consume();
            this.status.setText(exception.getMessage());
          }
        });
    this.entries
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((o, a, entry) -> this.select(entry));
    this.molecules.valueProperty().addListener((o, a, b) -> this.draw());
    this.saveStructure.setId("chemistry-library-save-structure");
    this.saveTemplate.setId("chemistry-library-save-template");
    this.saveFragment.setId("chemistry-library-save-fragment");
    this.saveStructure.setOnAction(event -> this.describe("structure"));
    this.saveTemplate.setOnAction(event -> this.describe("template"));
    this.saveFragment.setOnAction(event -> this.describe("fragment"));
    this.reload.setOnAction(event -> this.load(null));
    this.updateSaving();
    this.search.setMinWidth(120);
    final HBox searchBar = new HBox(8, this.search, this.scope, this.reload);
    HBox.setHgrow(this.search, Priority.ALWAYS);
    this.previewStatus.setWrapText(true);
    this.previewStatus.setMinHeight(Region.USE_PREF_SIZE);
    this.previewStatus.setId("chemistry-library-preview-status");
    final VBox details =
        new VBox(8, this.molecules, this.preview, this.previewStatus, this.description);
    details.setMinWidth(260);
    this.description.setMinWidth(0);
    this.molecules.setMinWidth(0);
    this.entries.setMinWidth(200);
    this.preview.widthProperty().bind(details.widthProperty());
    this.preview.widthProperty().addListener((o, a, b) -> this.draw());
    VBox.setVgrow(this.description, Priority.ALWAYS);
    final ScrollPane previewScroll = new ScrollPane(details);
    previewScroll.setFitToWidth(true);
    previewScroll.setMinWidth(260);
    previewScroll.setMinHeight(0);
    previewScroll.setId("chemistry-library-details-scroll");
    details.setMinHeight(Region.USE_PREF_SIZE);
    this.description.setMinHeight(100);
    this.preview
        .heightProperty()
        .bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () ->
                    Math.max(
                        140, Math.min(220, previewScroll.getViewportBounds().getHeight() - 58)),
                previewScroll.viewportBoundsProperty()));
    this.preview.heightProperty().addListener((o, a, b) -> this.draw());
    final SplitPane panes = new SplitPane(this.entries, previewScroll);
    panes.setDividerPositions(0.43);
    final Label hint =
        new Label(
            "Структура добавляется без количеств. Шаблон создаёт новый план со стадиями и количествами; факты, журнал, заметки и вложения прежнего опыта не переносятся.");
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    final Label location =
        new Label(
            this.folder == null
                ? "Папка библиотеки ещё не выбрана"
                : "Библиотека проекта: " + this.folder.getFileName());
    location.setId("chemistry-library-folder");
    if (this.folder != null) {
      location.setTooltip(new Tooltip(this.folder.toAbsolutePath().normalize().toString()));
    }
    location.setWrapText(true);
    location.setMinHeight(Region.USE_PREF_SIZE);
    final VBox content =
        new VBox(
            10,
            location,
            searchBar,
            this.count,
            panes,
            new FlowPane(8, 8, this.saveStructure, this.saveFragment, this.saveTemplate),
            hint,
            this.warnings,
            this.status);
    VBox.setVgrow(panes, Priority.ALWAYS);
    content.setPadding(new javafx.geometry.Insets(16));
    content.setMinWidth(0);
    panes.setMinHeight(240);
    final Label heading =
        new Label(
            (current == null ? "" : "Проекты › " + current.experiment().title() + " › ")
                + "Библиотека структур");
    heading.setWrapText(true);
    heading.getStyleClass().add("laboratory-navigation-title");
    final FlowPane header = new FlowPane(12, 8, back, heading);
    header.getStyleClass().add("laboratory-navigation-header");
    final FlowPane actions =
        new FlowPane(10, 8, this.insertButton, this.attachButton, this.templateButton);
    actions.setPadding(new javafx.geometry.Insets(12, 16, 14, 16));
    this.browserTop = header;
    this.browserCenter = content;
    this.browserBottom = actions;
    this.page.setTop(header);
    this.page.setCenter(content);
    this.page.setBottom(actions);
    this.load(null);
    return this.page;
  }

  boolean canClose() {
    if (this.saving) {
      this.status.setText("Дождитесь завершения сохранения записи.");
      return false;
    }
    return this.entryPage == null || this.entryPage.canClose();
  }

  void dispose() {
    this.active = false;
    if (this.loading != null) {
      this.loading.cancel();
    }
    if (this.fragmentPage != null) {
      this.fragmentPage.dispose();
    }
  }

  private void finish() {
    if (this.canClose()) {
      this.dispose();
      this.close.run();
    }
  }

  private record Loaded(
      List<ChemistryLibraryEntry> entries, Set<String> builtInIds, String warnings) {}

  private void load(final String selectId) {
    if (this.loading != null) {
      this.loading.cancel();
    }
    this.status.setText("Чтение библиотеки…");
    this.reload.setDisable(true);
    final Task<Loaded> task =
        new Task<>() {
          protected Loaded call() {
            final var built = new ChemistryLibraryService().search(ChemistryLibraryQuery.ALL);
            final var list = new java.util.ArrayList<>(built);
            final var ids =
                built.stream()
                    .map(ChemistryLibraryEntry::id)
                    .collect(java.util.stream.Collectors.toSet());
            final var messages = new java.util.ArrayList<String>();
            if (folder != null) {
              final var scanned =
                  new ChemistryLibraryFileRepository(folder.resolve(".chemistry-library")).scan();
              scanned
                  .issues()
                  .forEach(
                      issue -> messages.add(issue.path().getFileName() + ": " + issue.message()));
              for (final var entry : scanned.entries()) {
                if (ids.contains(entry.id())) {
                  messages.add(
                      entry.displayName() + ": идентификатор совпадает со встроенной записью.");
                } else {
                  list.add(entry);
                }
              }
            }
            return new Loaded(List.copyOf(list), Set.copyOf(ids), String.join("\n", messages));
          }
        };
    this.loading = task;
    task.setOnSucceeded(
        event -> {
          if (this.loading != task || !this.active) {
            return;
          }
          this.all = task.getValue().entries();
          this.builtInIds = task.getValue().builtInIds();
          this.warnings.setText(task.getValue().warnings());
          this.warnings.setVisible(!this.warnings.getText().isEmpty());
          this.warnings.setManaged(this.warnings.isVisible());
          this.reload.setDisable(false);
          if (selectId != null) {
            this.search.clear();
            this.scope.getSelectionModel().selectFirst();
          }
          this.filter();
          if (selectId != null) {
            this.entries
                .getSelectionModel()
                .select(
                    this.all.stream()
                        .filter(entry -> entry.id().equals(selectId))
                        .findFirst()
                        .orElse(null));
          }
          this.status.setText(
              selectId == null
                  ? (this.folder == null
                      ? "Выберите папку опытов для пользовательской библиотеки."
                      : "Библиотека прочитана.")
                  : "Запись сохранена и повторно прочитана.");
        });
    task.setOnFailed(
        event -> {
          if (this.loading == task && this.active) {
            this.reload.setDisable(false);
            this.status.setText(
                "Не удалось прочитать библиотеку: " + task.getException().getMessage());
          }
        });
    launch(task, "chemistry-library-load");
  }

  private void filter() {
    final var previous = this.entries.getSelectionModel().getSelectedItem();
    final String query = this.search.getText();
    final int filter = this.scope.getSelectionModel().getSelectedIndex();
    this.entries
        .getItems()
        .setAll(
            this.all.stream()
                .filter(
                    entry ->
                        LaboratoryTextSearch.matches(
                            entry.id()
                                + " "
                                + entry.displayName()
                                + " "
                                + entry.summary()
                                + " "
                                + String.join(" ", entry.tags()),
                            query))
                .filter(
                    entry ->
                        switch (filter) {
                          case 1 ->
                              !this.builtInIds.contains(entry.id())
                                  && entry.kind() != ChemistryLibraryEntryKind.WORKFLOW_TEMPLATE;
                          case 2 -> entry.kind() == ChemistryLibraryEntryKind.WORKFLOW_TEMPLATE;
                          case 3 -> this.builtInIds.contains(entry.id());
                          default -> true;
                          case 4 -> entry.kind() == ChemistryLibraryEntryKind.FRAGMENT;
                        })
                .sorted(
                    java.util.Comparator.comparing(
                        ChemistryLibraryEntry::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList());
    if (this.entries.getItems().contains(previous)) {
      this.entries.getSelectionModel().select(previous);
    } else {
      this.entries.getSelectionModel().selectFirst();
    }
    this.count.setText(
        "Показано записей: " + this.entries.getItems().size() + " из " + this.all.size());
  }

  private void select(final ChemistryLibraryEntry entry) {
    this.attachButton.setDisable(true);
    this.molecules.getItems().setAll(entry == null ? List.of() : entry.document().molecules());
    this.molecules.getSelectionModel().selectFirst();
    this.description.setText(
        entry == null
            ? ""
            : entry.summary()
                + "\n\nМетки / синонимы: "
                + String.join(", ", entry.tags())
                + "\nИсточники: "
                + String.join("; ", entry.references())
                + "\nТип: "
                + kind(entry)
                + (entry.document().laboratoryExperiment() == null
                    ? ""
                    : "\nКомпонентов: "
                        + entry.document().laboratoryExperiment().components().size()
                        + " · стадий: "
                        + entry.document().laboratoryExperiment().stages().size()));
    this.templateButton.setDisable(
        entry == null
            || entry.kind() != ChemistryLibraryEntryKind.WORKFLOW_TEMPLATE
            || entry.document().laboratoryExperiment() == null);
    this.draw();
    if (entry != null && entry.document().laboratoryExperiment() != null) {
      try {
        this.store.notebook(entry);
      } catch (final RuntimeException exception) {
        this.templateButton.setDisable(true);
        this.previewStatus.setText("Шаблон недоступен: " + exception.getMessage());
      }
    }
    if (entry != null && entry.kind() == ChemistryLibraryEntryKind.FRAGMENT) {
      try {
        final var points = this.store.attachmentPoints(entry);
        this.description.appendText(
            "\nТочки присоединения: "
                + (points.isEmpty()
                    ? "не заданы"
                    : points.stream()
                        .map(point -> point.label() + " · " + point.bondType())
                        .collect(java.util.stream.Collectors.joining("; "))));
        this.attachButton.setDisable(
            this.targetAtom == null
                || this.sourceStructure == null
                || this.selectedDrawing == null
                || points.isEmpty());
      } catch (final RuntimeException exception) {
        this.previewStatus.setText("Точки присоединения недоступны: " + exception.getMessage());
      }
    }
  }

  private void draw() {
    this.selectedDrawing = null;
    this.insertButton.setDisable(true);
    this.previewStatus.setText("");
    final var graphics = this.preview.getGraphicsContext2D();
    graphics.setFill(Color.WHITE);
    graphics.fillRect(0, 0, this.preview.getWidth(), this.preview.getHeight());
    final var entry = this.entries.getSelectionModel().getSelectedItem();
    final var molecule = this.molecules.getValue();
    if (entry == null || molecule == null) {
      return;
    }
    try {
      this.selectedDrawing = this.store.drawing(entry, molecule);
      final var atoms = LaboratoryCanvas.previewAtoms(this.selectedDrawing);
      final double minX = atoms.stream().mapToDouble(atom -> atom.point2D().x()).min().orElse(0);
      final double maxX = atoms.stream().mapToDouble(atom -> atom.point2D().x()).max().orElse(0);
      final double minY = atoms.stream().mapToDouble(atom -> atom.point2D().y()).min().orElse(0);
      final double maxY = atoms.stream().mapToDouble(atom -> atom.point2D().y()).max().orElse(0);
      final double scale =
          Math.min(
              1.4,
              Math.min(
                  Math.max(1, this.preview.getWidth() - 50) / (maxX - minX + 60),
                  Math.max(1, this.preview.getHeight() - 40) / (maxY - minY + 60)));
      graphics.save();
      try {
        graphics.translate(
            this.preview.getWidth() / 2 - (minX + maxX) * scale / 2,
            this.preview.getHeight() / 2 - (minY + maxY) * scale / 2);
        graphics.scale(scale, scale);
        LaboratoryCanvas.paintPreview(graphics, this.selectedDrawing);
      } finally {
        graphics.restore();
      }
      this.insertButton.setDisable(false);
    } catch (final RuntimeException exception) {
      this.selectedDrawing = null;
      this.templateButton.setDisable(true);
      this.previewStatus.setText("Рисунок недоступен: " + exception.getMessage());
    }
  }

  private void showForm(final javafx.scene.Node form) {
    this.page.setTop(null);
    this.page.setBottom(null);
    this.page.setCenter(form);
  }

  private void backToBrowser() {
    if (this.fragmentPage != null) {
      this.fragmentPage.dispose();
      this.fragmentPage = null;
    }
    this.entryPage = null;
    this.page.setTop(this.browserTop);
    this.page.setCenter(this.browserCenter);
    this.page.setBottom(this.browserBottom);
  }

  private void describe(final String kind) {
    if (this.saving) {
      return;
    }
    this.entryPage =
        new LaboratoryLibraryEntryPage(
            kind, this.sourceStructure, this.current, this::save, this::backToBrowser);
    this.showForm(this.entryPage);
  }

  boolean pending() {
    return this.entryPage != null && this.entryPage.pending();
  }

  void draft(final java.util.Properties fields, final boolean restore) {
    if (!restore) {
      if (this.entryPage != null) {
        this.entryPage.draft(fields, false);
        fields.setProperty(
            "libraryForm.folder",
            this.folder == null ? "" : this.folder.toAbsolutePath().normalize().toString());
      }
      return;
    }
    final var restored =
        new LaboratoryLibraryEntryPage(
            fields.getProperty("libraryForm.kind"),
            this.sourceStructure,
            this.current,
            this::save,
            this::backToBrowser);
    final var copy = new java.util.Properties();
    copy.putAll(fields);
    if (!java.util.Objects.equals(
        fields.getProperty("libraryForm.folder"),
        this.folder == null ? "" : this.folder.toAbsolutePath().normalize().toString())) {
      copy.setProperty("libraryForm.conflict", "true");
    }
    restored.draft(copy, true);
    this.entryPage = restored;
    this.showForm(restored);
  }

  private void save(final java.util.function.Supplier<ChemistryLibraryEntry> prepare) {
    this.saving = true;
    this.updateSaving();
    this.status.setText("Сохранение записи…");
    if (this.entryPage != null) {
      this.entryPage.busy(true);
    }
    final Task<ChemistryLibraryEntry> task =
        new Task<>() {
          protected ChemistryLibraryEntry call() throws Exception {
            final var entry = prepare.get();
            store.save(folder, entry);
            return entry;
          }
        };
    task.setOnSucceeded(
        event -> {
          this.saving = false;
          this.updateSaving();
          if (this.active) {
            this.backToBrowser();
            this.load(task.getValue().id());
          }
        });
    task.setOnFailed(
        event -> {
          this.saving = false;
          this.updateSaving();
          if (this.active) {
            final String message =
                task.getException() instanceof java.nio.file.FileAlreadyExistsException
                    ? "На месте папки библиотеки уже существует файл."
                    : task.getException().getMessage();
            if (this.entryPage != null) {
              this.entryPage.failed(message);
            } else {
              this.status.setText("Не удалось сохранить: " + message);
            }
          }
        });
    launch(task, "chemistry-library-save");
  }

  private void updateSaving() {
    this.saveStructure.setDisable(
        this.saving || this.folder == null || this.sourceStructure == null);
    this.saveTemplate.setDisable(this.saving || this.folder == null);
    this.saveFragment.setDisable(
        this.saving || this.folder == null || this.sourceStructure == null);
  }

  private static void launch(final Task<?> task, final String name) {
    final Thread thread = new Thread(task, name);
    thread.setDaemon(true);
    thread.start();
  }

  private static TextArea area(final int rows) {
    final TextArea field = new TextArea();
    field.setEditable(false);
    field.setWrapText(true);
    field.setPrefRowCount(rows);
    return field;
  }

  private static List<String> lines(final TextArea field) {
    return field
        .getText()
        .lines()
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .distinct()
        .toList();
  }

  private static String kind(final ChemistryLibraryEntry entry) {
    return switch (entry.kind()) {
      case MOLECULE -> "Структура";
      case FRAGMENT -> "Фрагмент";
      case REACTION -> "Схема реакции";
      case WORKFLOW_TEMPLATE -> "Шаблон опыта";
      case DATASET -> "Набор данных";
      case MIXED_PROJECT -> "Химический проект";
    };
  }
}
