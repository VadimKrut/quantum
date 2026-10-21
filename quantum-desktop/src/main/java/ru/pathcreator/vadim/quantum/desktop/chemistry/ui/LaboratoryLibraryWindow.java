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
  private final ComboBox<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent>
      source = new ComboBox<>();
  private final ComboBox<String> destination = new ComboBox<>();
  private final Path personalFolder;

  LaboratoryLibraryWindow() {
    this(Path.of(System.getProperty("user.home"), "Documents", "Quantum IR Studio"));
  }

  LaboratoryLibraryWindow(final Path personalFolder) {
    this.personalFolder = personalFolder;
  }

  private final TextArea description = area(6);
  private final TextArea warnings = area(3);
  private final TextField search = new TextField();
  private final Label status = new Label();
  private final Label previewStatus = new Label();
  private final Label count = new Label();
  private final Label entryTitle = new Label(), entryKind = new Label();
  private final TabPane tabs = new TabPane();
  private final LaboratoryStructurePreview ownPreview = new LaboratoryStructurePreview();
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
  private MoleculeDrawing attachmentStructure;
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
    this.attachmentStructure = selected;
    this.current = current;
    this.source.setId("chemistry-library-source");
    this.source.setMaxWidth(Double.MAX_VALUE);
    this.source.setConverter(
        new StringConverter<>() {
          public String toString(
              ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent
                  component) {
            return component == null
                ? "Выберите вещество из опыта"
                : (source.getItems().indexOf(component) + 1)
                    + " · "
                    + component.molecule().displayName();
          }

          public ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent
              fromString(String text) {
            return null;
          }
        });
    this.source.getItems().setAll(current == null ? List.of() : current.experiment().components());
    this.source
        .getSelectionModel()
        .select(
            this.source.getItems().stream()
                .filter(c -> selected != null && current.layouts().get(c.id()).equals(selected))
                .findFirst()
                .orElse(
                    this.source.getItems().isEmpty() ? null : this.source.getItems().getFirst()));
    this.sourceStructure =
        this.source.getValue() == null ? null : current.layouts().get(this.source.getValue().id());
    this.source
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              this.sourceStructure = b == null ? null : this.current.layouts().get(b.id());
              if (this.sourceStructure != null) this.ownPreview.show(this.sourceStructure, null);
              // Fragment attachment still refers to the atom selected in the drawing editor.
              this.updateSaving();
            });
    this.destination.setId("chemistry-library-destination");
    this.destination.getItems().setAll("Моя библиотека · для всех проектов");
    if (folder != null
        && !folder
            .toAbsolutePath()
            .normalize()
            .equals(this.personalFolder.toAbsolutePath().normalize()))
      this.destination.getItems().add("Только этот проект");
    this.destination.getSelectionModel().selectFirst();
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
            return value == null
                ? "Нет структуры"
                : value.displayName() == null || value.displayName().isBlank()
                    ? "Структура " + (molecules.getItems().indexOf(value) + 1)
                    : value.displayName();
          }

          public Molecule fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.molecules.setButtonCell(
        new ListCell<>() {
          @Override
          protected void updateItem(final Molecule item, final boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : molecules.getConverter().toString(item));
          }
        });
    this.molecules
        .visibleProperty()
        .bind(javafx.beans.binding.Bindings.size(this.molecules.getItems()).greaterThan(1));
    this.molecules.managedProperty().bind(this.molecules.visibleProperty());
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
    final Button back = new Button("← К опыту");
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
                    this.attachmentStructure,
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
    final FlowPane searchFilters = new FlowPane(8, 8, this.scope, this.reload, this.count);
    final VBox searchBar = new VBox(8, this.search, searchFilters);
    this.previewStatus.setWrapText(true);
    this.previewStatus.setMinHeight(Region.USE_PREF_SIZE);
    this.previewStatus.setId("chemistry-library-preview-status");
    this.entryTitle.getStyleClass().add("laboratory-section-title");
    this.entryTitle.setWrapText(true);
    this.entryKind.getStyleClass().add("laboratory-page-context");
    final TitledPane metadata = new TitledPane("Описание, синонимы и источники", this.description);
    metadata.setExpanded(false);
    metadata.setAnimated(false);
    final VBox details =
        new VBox(
            10,
            this.entryTitle,
            this.entryKind,
            this.molecules,
            this.preview,
            this.previewStatus,
            metadata);
    details.getStyleClass().add("laboratory-library-detail");
    details.setMinWidth(0);
    this.description.setMinWidth(0);
    this.molecules.setMinWidth(0);
    this.entries.setMinWidth(0);
    this.preview
        .widthProperty()
        .bind(javafx.beans.binding.Bindings.max(1, details.widthProperty().subtract(32)));
    this.preview.widthProperty().addListener((o, a, b) -> this.draw());
    VBox.setVgrow(this.description, Priority.ALWAYS);
    final ScrollPane previewScroll = new ScrollPane(details);
    previewScroll.setFitToWidth(true);
    previewScroll.setMinWidth(0);
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
                        110, Math.min(220, previewScroll.getViewportBounds().getHeight() - 150)),
                previewScroll.viewportBoundsProperty()));
    this.preview.heightProperty().addListener((o, a, b) -> this.draw());
    LaboratoryPreviewTheme.track(this.preview, this::draw);
    final VBox results = new VBox(10, searchBar, this.entries);
    results.setMinWidth(0);
    VBox.setVgrow(this.entries, Priority.ALWAYS);
    final SplitPane panes = new SplitPane(results, previewScroll);
    panes.setId("chemistry-library-split");
    panes.setDividerPositions(0.38);
    this.page
        .widthProperty()
        .addListener(
            (o, a, b) -> {
              final var direction =
                  b.doubleValue() < 860
                      ? javafx.geometry.Orientation.VERTICAL
                      : javafx.geometry.Orientation.HORIZONTAL;
              if (panes.getOrientation() != direction) {
                panes.setOrientation(direction);
                panes.setDividerPositions(
                    direction == javafx.geometry.Orientation.VERTICAL ? .42 : .38);
              }
            });
    final Label hint =
        new Label(
            "Структура добавляется без количеств. Шаблон создаёт новый план со стадиями и количествами; факты, журнал, заметки и вложения прежнего опыта не переносятся.");
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    final Label location =
        new Label(
            this.folder == null
                ? "Личная библиотека · доступна во всех проектах"
                : "Личная библиотека и записи текущего проекта");
    location.getStyleClass().add("laboratory-page-context");
    location.setId("chemistry-library-folder");
    if (this.folder != null) {
      location.setTooltip(new Tooltip(this.folder.toAbsolutePath().normalize().toString()));
    }
    location.setWrapText(true);
    location.setMinHeight(Region.USE_PREF_SIZE);
    final Label sourceHint =
        new Label(
            this.source.getItems().isEmpty()
                ? "В опыте пока нет веществ. Вернитесь к опыту и нарисуйте или импортируйте структуру."
                : "Выберите своё вещество из опыта и сохраните его под нужным названием.");
    sourceHint.setWrapText(true);
    this.saveStructure.setText("Далее: название и сохранение →");
    this.saveStructure.getStyleClass().add("laboratory-primary-action");
    this.insertButton.setText("Вставить выбранное в опыт");
    this.insertButton.getStyleClass().add("laboratory-primary-action");
    final Label ownHeading = new Label("Сохранить своё вещество");
    ownHeading.getStyleClass().add("laboratory-section-title");
    final Label destinationHint =
        new Label(
            "Личная библиотека будет доступна из любого опыта. Сохранение не меняет исходную структуру.");
    destinationHint.setWrapText(true);
    destinationHint.getStyleClass().add("laboratory-page-context");
    this.ownPreview.hint("Предпросмотр выбранного вещества");
    this.ownPreview.setMaxHeight(210);
    this.ownPreview.setPrefHeight(210);
    this.ownPreview.setMinHeight(160);
    if (this.sourceStructure != null) this.ownPreview.show(this.sourceStructure, null);
    final VBox own =
        new VBox(
            12,
            ownHeading,
            sourceHint,
            new Label("1. Вещество из текущего опыта"),
            this.source,
            this.ownPreview,
            new Label("2. Где хранить"),
            this.destination,
            destinationHint);
    own.setMaxWidth(760);
    own.setMinWidth(0);
    own.getStyleClass().add("laboratory-library-own");
    final TitledPane advanced =
        new TitledPane(
            "Фрагменты и шаблоны опыта", new FlowPane(8, 8, this.saveFragment, this.saveTemplate));
    advanced.setExpanded(false);
    advanced.setAnimated(false);
    own.getChildren().add(advanced);
    final VBox content = new VBox(10, location, panes);
    VBox.setVgrow(panes, Priority.ALWAYS);
    content.setPadding(new javafx.geometry.Insets(16));
    content.setMinWidth(0);
    panes.setMinHeight(0);
    final StackPane ownSheet = new StackPane(own);
    ownSheet.setAlignment(javafx.geometry.Pos.TOP_CENTER);
    ownSheet.setPadding(new javafx.geometry.Insets(18));
    final ScrollPane ownScroll = new ScrollPane(ownSheet);
    ownScroll.setFitToWidth(true);
    ownScroll.setMinSize(0, 0);
    this.tabs.setId("chemistry-library-tabs");
    this.tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabs
        .getTabs()
        .setAll(new Tab("Найти и вставить", content), new Tab("Добавить своё вещество", ownScroll));
    final Label heading = new Label("Библиотека веществ");
    heading.setWrapText(true);
    heading.getStyleClass().add("laboratory-navigation-title");
    final FlowPane header = new FlowPane(12, 8, back, heading);
    header.getStyleClass().add("laboratory-navigation-header");
    final FlowPane actions =
        new FlowPane(10, 8, this.insertButton, this.attachButton, this.templateButton);
    actions
        .visibleProperty()
        .bind(this.tabs.getSelectionModel().selectedIndexProperty().isEqualTo(0));
    actions.managedProperty().bind(actions.visibleProperty());
    final FlowPane ownActions = new FlowPane(10, 8, this.saveStructure);
    ownActions
        .visibleProperty()
        .bind(this.tabs.getSelectionModel().selectedIndexProperty().isEqualTo(1));
    ownActions.managedProperty().bind(ownActions.visibleProperty());
    final VBox footer = new VBox(8, this.warnings, this.status, actions, ownActions);
    footer.setPadding(new javafx.geometry.Insets(10, 16, 14, 16));
    footer.getStyleClass().add("laboratory-page-footer");
    this.status.getStyleClass().add("laboratory-page-status");
    this.status.visibleProperty().bind(this.status.textProperty().isNotEmpty());
    this.status.managedProperty().bind(this.status.visibleProperty());
    this.insertButton.setTooltip(
        new Tooltip("Вставить структуру в текущий опыт без количеств и прежних измерений."));
    this.templateButton.setTooltip(new Tooltip(hint.getText()));
    this.browserTop = header;
    this.browserCenter = this.tabs;
    this.browserBottom = footer;
    this.page.setTop(header);
    this.page.setCenter(this.tabs);
    this.page.setBottom(footer);
    this.load(null);
    return this.page;
  }

  boolean tutorialBusy() {
    return this.saving || this.loading != null && !this.loading.isDone();
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
            final var folders = new java.util.LinkedHashSet<Path>();
            folders.add(personalFolder.toAbsolutePath().normalize());
            if (folder != null) folders.add(folder.toAbsolutePath().normalize());
            final var seen = new java.util.HashSet<>(ids);
            for (final var libraryFolder : folders) {
              final var scanned =
                  new ChemistryLibraryFileRepository(libraryFolder.resolve(".chemistry-library"))
                      .scan();
              scanned
                  .issues()
                  .forEach(
                      issue -> messages.add(issue.path().getFileName() + ": " + issue.message()));
              for (final var entry : scanned.entries()) {
                if (ids.contains(entry.id())) {
                  messages.add(
                      entry.displayName() + ": идентификатор совпадает со встроенной записью.");
                } else if (seen.add(entry.id())) {
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
                  ? "Библиотека загружена. Выберите запись или откройте «Добавить своё вещество»."
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
    this.entryTitle.setText(entry == null ? "Выберите запись в списке" : entry.displayName());
    this.entryKind.setText(
        entry == null
            ? "Поиск по названию и синонимам"
            : kind(entry)
                + (builtInIds.contains(entry.id()) ? " · Встроенная" : " · Пользовательская"));
    this.attachButton.setVisible(
        entry != null && entry.kind() == ChemistryLibraryEntryKind.FRAGMENT);
    this.attachButton.setManaged(this.attachButton.isVisible());
    this.templateButton.setVisible(
        entry != null && entry.kind() == ChemistryLibraryEntryKind.WORKFLOW_TEMPLATE);
    this.templateButton.setManaged(this.templateButton.isVisible());
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
                || this.attachmentStructure == null
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
    LaboratoryPreviewTheme.clear(graphics);
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
        LaboratoryPreviewTheme.paint(graphics, this.selectedDrawing);
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
    if (!"template".equals(kind) && this.sourceStructure == null) {
      this.status.setText("Выберите вещество из текущего опыта в списке вверху.");
      this.source.requestFocus();
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
            "libraryForm.sourceComponent",
            this.source.getValue() == null ? "" : this.source.getValue().id());
        fields.setProperty(
            "libraryForm.destination",
            Integer.toString(this.destination.getSelectionModel().getSelectedIndex()));
        fields.setProperty(
            "libraryForm.folder",
            this.folder == null ? "" : this.folder.toAbsolutePath().normalize().toString());
      }
      return;
    }
    this.source
        .getSelectionModel()
        .select(
            this.source.getItems().stream()
                .filter(c -> c.id().equals(fields.getProperty("libraryForm.sourceComponent")))
                .findFirst()
                .orElse(this.source.getValue()));
    this.destination
        .getSelectionModel()
        .select(
            "1".equals(fields.getProperty("libraryForm.destination"))
                    && this.destination.getItems().size() > 1
                ? 1
                : 0);
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
    final Path destinationFolder =
        this.destination.getSelectionModel().getSelectedIndex() == 1
            ? this.folder
            : this.personalFolder;
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
            store.save(destinationFolder, entry);
            return entry;
          }
        };
    task.setOnSucceeded(
        event -> {
          this.saving = false;
          this.updateSaving();
          if (this.active) {
            this.backToBrowser();
            this.tabs.getSelectionModel().selectFirst();
            this.search.clear();
            this.scope.getSelectionModel().selectFirst();
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
    this.saveStructure.setDisable(this.saving || this.sourceStructure == null);
    this.saveTemplate.setDisable(this.saving || this.current == null);
    this.saveFragment.setDisable(this.saving || this.sourceStructure == null);
    this.source.setDisable(this.saving);
    this.destination.setDisable(this.saving);
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
