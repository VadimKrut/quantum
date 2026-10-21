/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryCompoundRegistry.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Local compound cards with immutable graphs, explicit relationships and native CAS persistence. */
final class LaboratoryRegistryPage extends LaboratoryComputationPage {
  private record Choice(String id, String title) {
    @Override
    public String toString() {
      return title;
    }
  }

  @FunctionalInterface
  private interface Work<T> {
    T run() throws Exception;
  }

  private final LaboratoryEditor editor;
  private final Window owner;
  private final LaboratoryRegistryRepository repository = new LaboratoryRegistryRepository();
  private LaboratoryRegistryCodec.Document document = LaboratoryRegistryCodec.Document.empty();
  private Path file;
  private String fingerprint, selectedId, deferredComponent;
  private Consumer<String> openParticipant;
  private final Label context = note(""), linkState = note("");
  private final Label filterContext = note("");
  private final javafx.beans.property.BooleanProperty cardAvailable =
      new javafx.beans.property.SimpleBooleanProperty();
  private final FlowPane uses = new FlowPane(8, 8);
  private final VBox duplicate = new VBox(8);
  private final TitledPane cardFields = new TitledPane();
  private DuplicateReview duplicateReview;

  private record DuplicateReview(
      LaboratoryRegistryCodec.Document document,
      String fingerprint,
      LaboratoryDrawing experiment,
      String componentId,
      MoleculeDrawing drawing,
      String id,
      String name,
      List<String> aliases,
      String source) {}

  private boolean changing, dirty, registrationDirty, relationDirty, initialized, failedLoad;
  private Properties deferredDraft;
  private Thread worker;
  private final Label path = note("Выберите папку проекта"), identity = note("Выберите соединение");
  private final TextField search = new TextField(),
      name = new TextField(),
      registrationName = new TextField();
  private final TextArea aliases = new TextArea(),
      source = new TextArea(),
      registrationAliases = new TextArea(),
      registrationSource = new TextArea(),
      relationNote = new TextArea();
  private final ComboBox<Choice> component = new ComboBox<>();
  private final ComboBox<Compound> relationTarget = new ComboBox<>();
  private final ComboBox<Kind> relationKind = new ComboBox<>();
  private final ComboBox<ReactionComponentRole> role = new ComboBox<>();
  private final CheckBox includeNames = new CheckBox("С названиями и источником карточки");
  private final ListView<Compound> compounds = new ListView<>();
  private final ListView<Relation> relations = new ListView<>();
  private final ListView<Path> history = new ListView<>();
  private final Canvas canvas = new Canvas(520, 300);
  private final StackPane viewport = new StackPane(canvas);
  private final TabPane tabs = new TabPane();
  private double zoom = 1, panX, panY, dragX, dragY;

  LaboratoryRegistryPage(
      final LaboratoryEditor editor, final Path folder, final Window owner, final Runnable close) {
    super("Реестр соединений проекта", close);
    this.editor = editor;
    this.owner = owner;
    this.setId("chemistry-registry-page");
    this.changing = true;
    this.search.setPromptText("Номер, название, синоним или источник");
    this.name.setPromptText("Предпочтительное название");
    this.registrationName.setPromptText("Название новой карточки");
    this.aliases.setPromptText("По одному названию на строку: IUPAC, обычное имя, внутренний код…");
    this.registrationAliases.setPromptText(this.aliases.getPromptText());
    this.relationNote.setPromptText("Основание связи: источник или ваше химическое заключение");
    for (final var area :
        List.of(aliases, source, registrationAliases, registrationSource, relationNote)) {
      area.setWrapText(true);
      area.setPrefRowCount(3);
    }
    this.component
        .getItems()
        .setAll(
            editor.document().experiment().components().stream()
                .map(
                    c ->
                        new Choice(
                            c.id(),
                            c.molecule().displayName()
                                + " · "
                                + LaboratoryInspector.roleText(c.role())))
                .toList());
    this.component.setMaxWidth(Double.MAX_VALUE);
    this.role.getItems().setAll(ReactionComponentRole.values());
    this.role.setValue(ReactionComponentRole.REACTANT);
    this.role.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final ReactionComponentRole value) {
            return value == null ? "" : LaboratoryInspector.roleText(value);
          }

          public ReactionComponentRole fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    this.relationKind.getItems().setAll(Kind.values());
    this.relationKind.setValue(Kind.PARENT_GROUP);
    this.relationTarget.setMaxWidth(Double.MAX_VALUE);
    this.viewport.setMinSize(260, 220);
    this.viewport.setPrefHeight(320);
    this.viewport.setStyle(
        "-fx-background-color: -app-surface; -fx-border-color: -app-border; -fx-border-radius: 8;");
    LaboratoryPreviewTheme.track(this.canvas, this::paint);
    this.viewport
        .widthProperty()
        .addListener(
            (o, a, b) -> {
              canvas.setWidth(Math.max(260, b.doubleValue()));
              paint();
            });
    this.viewport
        .heightProperty()
        .addListener(
            (o, a, b) -> {
              canvas.setHeight(Math.max(220, b.doubleValue()));
              paint();
            });
    this.canvas.setOnScroll(
        e -> {
          zoom = Math.max(0.3, Math.min(6, zoom * (e.getDeltaY() > 0 ? 1.12 : 1 / 1.12)));
          paint();
          e.consume();
        });
    this.canvas.setOnMousePressed(
        e -> {
          dragX = e.getX();
          dragY = e.getY();
        });
    this.canvas.setOnMouseDragged(
        e -> {
          panX += e.getX() - dragX;
          panY += e.getY() - dragY;
          dragX = e.getX();
          dragY = e.getY();
          paint();
        });
    this.canvas.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            fit();
          }
        });
    this.cardFields.setText("Названия и источник · изменить");
    this.cardFields.setExpanded(false);
    this.cardFields.setContent(
        new VBox(
            10,
            field("Предпочтительное название", this.name),
            field("Другие названия и обозначения", this.aliases),
            field("Происхождение и источники", this.source),
            new FlowPane(
                8,
                8,
                button("Сохранить карточку", this::saveCard),
                button("Сбросить поля", this::resetCard))));
    this.includeNames.setSelected(true);
    this.includeNames.setId("registry-include-names");
    this.includeNames.setWrapText(true);
    this.includeNames.setTooltip(
        new Tooltip(
            "Сохранить актуальные названия и полный источник этой карточки в новом участнике. Они остаются пользовательскими сведениями, а не автоматически подтверждённым названием IUPAC. Без отметки добавляется структура со ссылкой на реестр."));
    final var use = button("Добавить в опыт", this::useCompound);
    use.getStyleClass().add("laboratory-primary-action");
    use.setId("registry-use-card");
    use.disableProperty().bind(this.cardAvailable.not());
    final var link = button("Связать с выбранным участником", this::linkParticipant);
    link.setId("registry-link-participant");
    link.disableProperty().bind(this.cardAvailable.not());
    this.filterContext.setId("registry-filter-context");
    this.filterContext.visibleProperty().bind(this.filterContext.textProperty().isNotEmpty());
    this.filterContext.managedProperty().bind(this.filterContext.visibleProperty());
    final var card =
        new VBox(
            10,
            this.filterContext,
            this.identity,
            this.linkState,
            this.uses,
            this.viewport,
            new FlowPane(
                8,
                8,
                button("Вписать структуру", this::fit),
                note("Колесо — масштаб · перетаскивание — сдвиг")),
            this.includeNames,
            new FlowPane(8, 8, this.role, use),
            this.context,
            link,
            this.cardFields);
    card.setPadding(new Insets(12));
    final var registration =
        new VBox(
            12,
            note(
                "Выберите компонент текущего опыта. Сначала проверяется точное совпадение графа; соли, сольваты, таутомеры и стереоизомеры сохраняются отдельно. Неопределённое совпадение будет явно указано."),
            field("Компонент опыта", this.component),
            button("Взять сохранённые названия", this::takeSavedNames),
            field("Предпочтительное название *", this.registrationName),
            field("Другие названия", this.registrationAliases),
            field("Происхождение *", this.registrationSource),
            new FlowPane(
                8,
                8,
                button("Проверить совпадения", this::checkMatches),
                button("Зарегистрировать соединение", this::register),
                button("Сбросить поля регистрации", this::resetRegistration)),
            this.duplicate,
            note(
                "Название IUPAC не вычисляется из произвольного графа автоматически. Сохраните подтверждённое имя или синоним; структурная идентичность проверяется независимо от названия."));
    registration.setPadding(new Insets(16));
    this.duplicate.setVisible(false);
    this.duplicate.setManaged(false);
    final var relationForm =
        new VBox(
            10,
            note(
                "Связи относятся к выбранной слева карточке. Для соли или сольвата она является формой выбранного справа родительского соединения. Связь не заменяет структуру и не объединяет номера."),
            field("Связать с соединением", this.relationTarget),
            field("Отношение", this.relationKind),
            field("Основание связи *", this.relationNote),
            new FlowPane(
                8,
                8,
                button("Добавить связь", this::addRelation),
                button("Удалить выбранную связь", this::removeRelation),
                button("Сбросить поля связи", this::resetRelation)),
            this.relations);
    relationForm.setPadding(new Insets(16));
    this.relations.setPrefHeight(260);
    final var historyForm =
        new VBox(
            12,
            note(
                "Предыдущие версии реестра сохраняются перед изменением. Здесь их можно просмотреть; номера и структуры существующих записей не переиспользуются."),
            button("Обновить историю", this::loadHistory),
            this.history,
            button("Посмотреть выбранную версию", this::showHistory));
    historyForm.setPadding(new Insets(16));
    this.history.setPrefHeight(350);
    for (final var tab :
        List.of(
            new Tab("Карточка", scroll(card)),
            new Tab("Из опыта", scroll(registration)),
            new Tab("Связи", scroll(relationForm)),
            new Tab("История", scroll(historyForm)))) {
      tab.setClosable(false);
      tabs.getTabs().add(tab);
    }
    final var sidebar = new VBox(8, this.search, this.compounds);
    sidebar.setPadding(new Insets(10));
    sidebar.setMinWidth(200);
    sidebar.setPrefWidth(280);
    VBox.setVgrow(this.compounds, Priority.ALWAYS);
    this.compounds.setPlaceholder(
        note("Реестр пока пуст. Откройте «Из опыта» и зарегистрируйте компонент."));
    this.compounds.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Compound c, final boolean empty) {
                super.updateItem(c, empty);
                setText(null);
                setGraphic(empty || c == null ? null : note(c.id() + "\n" + c.displayName()));
              }
            });
    this.relations.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Relation r, final boolean empty) {
                super.updateItem(r, empty);
                setText(null);
                setGraphic(
                    empty || r == null
                        ? null
                        : note(
                            r.firstId()
                                + " → "
                                + r.secondId()
                                + " · "
                                + r.kind()
                                + "\n"
                                + r.note()));
              }
            });
    this.history.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Path p, final boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                  setText(null);
                } else {
                  try {
                    setText(
                        Files.getLastModifiedTime(p).toInstant().toString()
                            + " · "
                            + p.getFileName().toString().substring(0, 12));
                  } catch (final Exception exception) {
                    setText(p.getFileName().toString());
                  }
                }
              }
            });
    final var split = new SplitPane(sidebar, this.tabs);
    split.setDividerPositions(0.27);
    sidebar.setMinHeight(0);
    this.compounds.setMinHeight(70);
    this.widthProperty()
        .addListener(
            o -> {
              final boolean narrow = getWidth() < 720;
              final var direction =
                  narrow
                      ? javafx.geometry.Orientation.VERTICAL
                      : javafx.geometry.Orientation.HORIZONTAL;
              if (split.getOrientation() != direction) {
                split.setOrientation(direction);
                split.setDividerPositions(narrow ? .22 : .27);
              }
            });
    final var center = new BorderPane(split);
    final var project =
        new FlowPane(
            8,
            8,
            button("Папка проекта…", this::chooseFolder),
            this.path,
            button("Обновить реестр", this::reload));
    project.setPadding(new Insets(8, 16, 8, 16));
    center.setTop(project);
    this.setCenter(center);
    this.status.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(status);
    this.search
        .textProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                this.filter();
              }
            });
    this.compounds
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing && b != null) {
                if (dirty || relationDirty) {
                  status.setText(
                      "Сохраните или сбросьте изменения карточки и связи перед выбором другого соединения.");
                  changing = true;
                  compounds.getSelectionModel().select(a);
                  changing = false;
                } else {
                  select(b.id());
                }
              }
            });
    this.component
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing && b != null) {
                if (registrationDirty) {
                  status.setText(
                      "Сохраните или сбросьте поля регистрации перед выбором другого компонента.");
                  changing = true;
                  component.setValue(a);
                  changing = false;
                } else {
                  clearDuplicate();
                  fillRegistration();
                }
              }
            });
    for (final var input : List.of(name, aliases, source)) {
      input
          .textProperty()
          .addListener(
              (o, a, b) -> {
                if (!changing) {
                  dirty = true;
                }
              });
    }
    for (final var input : List.of(registrationName, registrationAliases, registrationSource)) {
      input
          .textProperty()
          .addListener(
              (o, a, b) -> {
                if (!changing) {
                  registrationDirty = true;
                  clearDuplicate();
                }
              });
    }
    relationNote
        .textProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                relationDirty = true;
              }
            });
    relationTarget
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                relationDirty = true;
              }
            });
    relationKind
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                relationDirty = true;
              }
            });
    this.changing = false;
    if (!component.getItems().isEmpty()) {
      component.setValue(component.getItems().getFirst());
    }
    if (folder != null) {
      this.file = LaboratoryRegistryRepository.file(folder);
      this.reload();
    } else {
      this.initialized = true;
      this.status.setText(
          "Выберите папку проекта. Реестр будет храниться в compound-registry.qchem рядом с вашими опытами.");
    }
  }

  private void filter() {
    final boolean before = changing;
    changing = true;
    try {
      compounds
          .getItems()
          .setAll(LaboratoryRegistryService.search(document.registry(), search.getText()));
      compounds.setPlaceholder(
          note(
              document.registry().compounds().isEmpty()
                  ? "Реестр пока пуст. Откройте «Из опыта» и зарегистрируйте компонент."
                  : "Ничего не найдено. Измените или очистите поиск."));
      final var visible =
          selectedId == null
              ? null
              : compounds.getItems().stream()
                  .filter(c -> c.id().equals(selectedId))
                  .findFirst()
                  .orElse(null);
      compounds.getSelectionModel().select(visible);
      cardAvailable.set(visible != null);
      if (visible != null) {
        filterContext.setText("");
      } else if (dirty || relationDirty) {
        filterContext.setText(
            selectedId == null
                ? "Восстановленный ввод не связан с доступной карточкой. Поля сохранены для проверки."
                : "Несохранённая карточка "
                    + selectedId
                    + " не входит в результаты поиска. Сохраните или сбросьте её поля перед выбором другой.");
      } else {
        clearCard();
      }
    } finally {
      changing = before;
    }
  }

  private void clearCard() {
    final boolean before = changing;
    changing = true;
    try {
      selectedId = null;
      cardAvailable.set(false);
      compounds.getSelectionModel().clearSelection();
      name.clear();
      aliases.clear();
      source.clear();
      relationTarget.getItems().clear();
      relations.getItems().clear();
      relationNote.clear();
      relationKind.setValue(Kind.PARENT_GROUP);
      cardFields.setExpanded(false);
      filterContext.setText("");
      identity.setText(
          document.registry().compounds().isEmpty()
              ? "Выберите или зарегистрируйте соединение."
              : compounds.getItems().isEmpty()
                  ? "Ничего не найдено. Измените поиск."
                  : "Выберите соединение в результатах поиска.");
      dirty = relationDirty = false;
    } finally {
      changing = before;
    }
    refreshLinks();
    paint();
  }

  private void select(final String id) {
    final var c = document.registry().compound(id);
    final String target =
        Objects.equals(selectedId, id) && relationTarget.getValue() != null
            ? relationTarget.getValue().id()
            : null;
    final boolean before = changing;
    changing = true;
    selectedId = id;
    if (LaboratoryRegistryService.search(document.registry(), search.getText()).stream()
        .noneMatch(found -> found.id().equals(id))) {
      search.clear();
    }
    name.setText(c.displayName());
    aliases.setText(String.join("\n", c.aliases()));
    source.setText(c.source());
    identity.setText(
        c.id()
            + " · "
            + c.displayName()
            + "\nЗарегистрировано "
            + c.registeredAt()
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            + "\n"
            + c.molecule().atomCount()
            + " атомов · "
            + c.molecule().bondCount()
            + " связей");
    relationTarget
        .getItems()
        .setAll(
            document.registry().compounds().stream()
                .filter(other -> !other.id().equals(id))
                .toList());
    relationTarget.setValue(
        relationTarget.getItems().stream()
            .filter(v -> v.id().equals(target))
            .findFirst()
            .orElse(null));
    relations
        .getItems()
        .setAll(
            document.registry().relations().stream()
                .filter(r -> r.firstId().equals(id) || r.secondId().equals(id))
                .toList());
    dirty = false;
    changing = before;
    filter();
    refreshLinks();
    fit();
  }

  private void resetCard() {
    if (selectedId != null) {
      select(selectedId);
    } else {
      final boolean before = changing;
      changing = true;
      name.clear();
      aliases.clear();
      source.clear();
      dirty = false;
      changing = before;
      filter();
      refreshLinks();
      paint();
    }
  }

  private void fillRegistration() {
    final var choice = component.getValue();
    if (choice == null) {
      return;
    }
    final var c =
        editor.document().experiment().components().stream()
            .filter(v -> v.id().equals(choice.id()))
            .findFirst()
            .orElse(null);
    if (c == null) {
      return;
    }
    changing = true;
    registrationName.setText(c.molecule().displayName());
    registrationAliases.clear();
    registrationSource.setText(
        "Опыт «"
            + editor.document().experiment().title()
            + "»; ID опыта: "
            + editor.document().experiment().id()
            + "; компонент: "
            + c.id()
            + ".");
    changing = false;
    registrationDirty = false;
    context.setText(
        "Выбранный участник: "
            + c.molecule().displayName()
            + " · "
            + LaboratoryInspector.roleText(c.role()));
    context.setTooltip(new Tooltip(c.id()));
    refreshLinks();
  }

  private void resetRegistration() {
    clearDuplicate();
    fillRegistration();
    status.setText("Поля регистрации сброшены.");
  }

  private void resetRelation() {
    final boolean before = changing;
    changing = true;
    relationNote.clear();
    relationTarget.setValue(null);
    relationKind.setValue(Kind.PARENT_GROUP);
    changing = before;
    relationDirty = false;
    if (selectedId != null && !dirty) {
      select(selectedId);
    } else {
      filter();
    }
  }

  private MoleculeDrawing sourceDrawing() {
    if (component.getValue() == null) {
      throw new IllegalArgumentException("Выберите компонент опыта.");
    }
    final String id = component.getValue().id();
    final var c =
        editor.document().experiment().components().stream()
            .filter(v -> v.id().equals(id))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Компонент изменён или удалён; откройте реестр заново из актуального опыта."));
    return editor.document().layouts().containsKey(id)
        ? editor.document().layouts().get(id)
        : new LaboratoryMoleculePlacement().place(c.molecule(), 200, 200);
  }

  private void chooseFolder() {
    if (pending()) {
      status.setText("Сначала сохраните или сбросьте незавершённые поля.");
      return;
    }
    final var chooser = new DirectoryChooser();
    chooser.setTitle("Папка проекта для реестра соединений");
    if (file != null && Files.isDirectory(file.getParent())) {
      chooser.setInitialDirectory(file.getParent().toFile());
    }
    final var selected = chooser.showDialog(owner);
    if (selected != null) {
      file = LaboratoryRegistryRepository.file(selected.toPath());
      fingerprint = null;
      selectedId = null;
      reload();
    }
  }

  private void reload() {
    if (running || dirty || registrationDirty || relationDirty) {
      status.setText("Сохраните или сбросьте незавершённые поля перед обновлением.");
      return;
    }
    if (file == null) {
      return;
    }
    path.setText("Папка проекта: " + file.getParent().getFileName());
    path.setTooltip(new Tooltip(file.getParent().toString()));
    failedLoad = true;
    job(
        "Открываю реестр…",
        () ->
            Files.exists(file)
                ? repository.read(file)
                : new LaboratoryRegistryRepository.Opened(
                    LaboratoryRegistryCodec.Document.empty(), null),
        opened -> {
          document = opened.document();
          fingerprint = opened.fingerprint();
          failedLoad = false;
          initialized = true;
          filter();
          if (selectedId != null
              && document.registry().compounds().stream()
                  .anyMatch(c -> c.id().equals(selectedId))) {
            select(selectedId);
          } else if (!compounds.getItems().isEmpty()) {
            select(compounds.getItems().getFirst().id());
          } else {
            clearCard();
          }
          status.setText(
              "В проекте: "
                  + document.registry().compounds().size()
                  + " соединений. Структуры сохраняются под постоянными номерами.");
          if (deferredDraft != null) {
            final var draft = deferredDraft;
            deferredDraft = null;
            draft(draft, true);
          } else if (deferredComponent != null) {
            final String id = deferredComponent;
            deferredComponent = null;
            forComponent(id);
          }
        });
  }

  private void ready() {
    if (file == null) {
      throw new IllegalArgumentException("Сначала выберите папку проекта.");
    }
    if (!initialized || failedLoad) {
      throw new IllegalArgumentException("Реестр не загружен; исправьте ошибку и обновите файл.");
    }
  }

  private void checkMatches() {
    attempt(
        () -> {
          ready();
          final var drawing = sourceDrawing();
          job(
              "Сравниваю структуры…",
              () ->
                  new LaboratoryRegistryService()
                      .matches(document.registry(), drawing, document.layouts()),
              matches -> {
                status.setText(matchSummary(matches));
                if (!matches.exact().isEmpty() && !dirty && !relationDirty) {
                  select(matches.exact().getFirst().id());
                  filter();
                  tabs.getSelectionModel().select(0);
                }
              });
        });
  }

  private static String matchSummary(final LaboratoryRegistryService.Matches matches) {
    return !matches.exact().isEmpty()
        ? "Точное совпадение: "
            + matches.exact().stream().map(Compound::id).toList()
            + ". Добавьте синоним или источник в существующую карточку."
        : "Точных совпадений не найдено."
            + (matches.unresolved().isEmpty()
                ? ""
                : " Требуется ручное сравнение (стереохимия или предел поиска): "
                    + matches.unresolved().stream().limit(20).map(Compound::id).toList()
                    + ". Такие записи не объединяются автоматически.");
  }

  private void register() {
    attempt(
        () -> {
          ready();
          if (dirty || relationDirty) {
            throw new IllegalArgumentException(
                "Сохраните или сбросьте поля существующей карточки и связи перед регистрацией.");
          }
          final var drawing = sourceDrawing();
          final String preferred = registrationName.getText(),
              provenance = registrationSource.getText();
          final var names = names(registrationAliases.getText());
          final var snapshot = document;
          final String expectedFingerprint = fingerprint;
          final var expectedExperiment = editor.document();
          final String componentId = component.getValue().id();
          job(
              "Проверяю структуру и сохраняю запись…",
              () -> {
                final var service = new LaboratoryRegistryService();
                final var matches =
                    service.matches(snapshot.registry(), drawing, snapshot.layouts());
                if (!matches.exact().isEmpty()) {
                  return new RegistrationResult(null, null, matches);
                }
                final String note =
                    matches.unresolved().isEmpty()
                        ? ""
                        : "\nПри регистрации осталось неопределённое сравнение с "
                            + matches.unresolved().stream().limit(50).map(Compound::id).toList()
                            + "; записи не объединены.";
                final var result =
                    service.register(
                        snapshot.registry(),
                        snapshot.layouts(),
                        drawing,
                        preferred,
                        names,
                        provenance + note,
                        Instant.now());
                final var layouts = new LinkedHashMap<>(snapshot.layouts());
                layouts.put(result.compound().id(), result.drawing());
                final var next = new LaboratoryRegistryCodec.Document(result.registry(), layouts);
                final String hash = repository.write(file, next, expectedFingerprint);
                return new RegistrationResult(
                    new LaboratoryRegistryRepository.Opened(next, hash),
                    result.compound().id(),
                    matches);
              },
              result -> {
                if (result.opened() == null) {
                  showDuplicate(
                      snapshot,
                      expectedFingerprint,
                      expectedExperiment,
                      componentId,
                      drawing,
                      result.matches().exact().getFirst().id(),
                      preferred,
                      names,
                      provenance);
                  return;
                }
                document = result.opened().document();
                fingerprint = result.opened().fingerprint();
                registrationDirty = false;
                clearDuplicate();
                select(result.id());
                filter();
                tabs.getSelectionModel().select(0);
                final String linked = linkAfterWrite(expectedExperiment, componentId, result.id());
                status.setText(
                    "Зарегистрировано "
                        + result.id()
                        + ". "
                        + linked
                        + (result.matches().unresolved().isEmpty()
                            ? ""
                            : " " + matchSummary(result.matches())));
              });
        });
  }

  private record RegistrationResult(
      LaboratoryRegistryRepository.Opened opened,
      String id,
      LaboratoryRegistryService.Matches matches) {}

  private void saveCard() {
    attempt(
        () -> {
          ready();
          if (selectedId == null) {
            throw new IllegalArgumentException("Выберите карточку.");
          }
          final var old = document.registry().compound(selectedId);
          final var replacement =
              new Compound(
                  old.id(),
                  name.getText(),
                  old.molecule(),
                  names(aliases.getText()),
                  source.getText(),
                  old.registeredAt());
          final var compounds =
              document.registry().compounds().stream()
                  .map(c -> c.id().equals(old.id()) ? replacement : c)
                  .toList();
          persist(
              new LaboratoryCompoundRegistry(
                  document.registry().nextNumber(), compounds, document.registry().relations()),
              "Карточка сохранена; предыдущая версия находится в истории.",
              () -> {
                dirty = false;
                select(replacement.id());
              });
        });
  }

  private void addRelation() {
    attempt(
        () -> {
          ready();
          requireCleanCard();
          if (selectedId == null || relationTarget.getValue() == null) {
            throw new IllegalArgumentException("Выберите две карточки для связи.");
          }
          final var next = new ArrayList<>(document.registry().relations());
          next.add(
              new Relation(
                  "relation_" + UUID.randomUUID(),
                  selectedId,
                  relationTarget.getValue().id(),
                  relationKind.getValue(),
                  relationNote.getText()));
          persist(
              new LaboratoryCompoundRegistry(
                  document.registry().nextNumber(), document.registry().compounds(), next),
              "Связь сохранена. Номера и структуры остались самостоятельными.",
              () -> {
                resetRelation();
                select(selectedId);
              });
        });
  }

  private void removeRelation() {
    attempt(
        () -> {
          ready();
          requireCleanCard();
          final var selected = relations.getSelectionModel().getSelectedItem();
          if (selected == null) {
            throw new IllegalArgumentException("Выберите связь в списке.");
          }
          persist(
              new LaboratoryCompoundRegistry(
                  document.registry().nextNumber(),
                  document.registry().compounds(),
                  document.registry().relations().stream()
                      .filter(r -> !r.id().equals(selected.id()))
                      .toList()),
              "Связь удалена; предыдущая версия доступна в истории.",
              () -> select(selectedId));
        });
  }

  private void persist(
      final LaboratoryCompoundRegistry registry, final String message, final Runnable done) {
    final var next = new LaboratoryRegistryCodec.Document(registry, document.layouts());
    final String expected = fingerprint;
    job(
        "Сохраняю реестр…",
        () -> repository.write(file, next, expected),
        hash -> {
          document = next;
          fingerprint = hash;
          done.run();
          filter();
          status.setText(message);
        });
  }

  private void useCompound() {
    attempt(
        () -> {
          ready();
          requireCleanCard();
          if (registrationDirty) {
            tabs.getSelectionModel().select(1);
            throw new IllegalArgumentException(
                "Сохраните или сбросьте поля регистрации перед добавлением другого участника.");
          }
          requireSelectedCard();
          final var c = document.registry().compound(selectedId);
          final var sketch = new MoleculeSketchService();
          var drawing =
              sketch.rename(
                  sketch.independentCopy(document.layouts().get(selectedId)), c.displayName());
          final var service = new LaboratoryRegistryService();
          if (includeNames.isSelected()) {
            drawing =
                service.withCardNames(
                    file, c, document.layouts().get(selectedId), drawing, Instant.now());
          }
          final var linked = service.linked(file, c, document.layouts().get(selectedId), drawing);
          final String id = editor.addPlaced(linked, Objects.requireNonNull(role.getValue()));
          refreshComponentChoices(id);
          fillRegistration();
          refreshLinks();
          status.setText(
              c.id()
                  + " добавлено в опыт"
                  + (includeNames.isSelected()
                      ? " с названиями, их источником и ссылкой на карточку."
                      : " со ссылкой на карточку; новые названия не переносились.")
                  + " Количество и свойства задайте в опыте; сохраните опыт.");
        });
  }

  private void requireCleanCard() {
    if (dirty) {
      tabs.getSelectionModel().select(0);
      cardFields.setExpanded(true);
      throw new IllegalArgumentException(
          "В карточке есть незаписанные имя, названия или источник. Сохраните карточку либо сбросьте её поля; ввод связи сохранён.");
    }
  }

  private void requireSelectedCard() {
    if (selectedId == null || !cardAvailable.get()) {
      throw new IllegalArgumentException("Выберите карточку в текущих результатах поиска.");
    }
  }

  void participant(final Consumer<String> open) {
    this.openParticipant = open;
    refreshLinks();
  }

  void forComponent(final String id) {
    if (id == null) {
      return;
    }
    if (running) {
      deferredComponent = id;
      return;
    }
    attempt(
        () -> {
          if (dirty || registrationDirty || relationDirty) {
            throw new IllegalArgumentException(
                "Сохраните или сбросьте поля перед переходом к участнику.");
          }
          final var participant =
              editor.document().experiment().components().stream()
                  .filter(c -> c.id().equals(id))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Участник больше не находится в этом опыте."));
          refreshComponentChoices(id);
          fillRegistration();
          final var saved = MolecularRegistryReference.read(participant.molecule());
          if (saved.isEmpty()) {
            tabs.getSelectionModel().select(1);
            status.setText(
                "У участника пока нет ссылки на реестр. Проверьте совпадения или зарегистрируйте его структуру.");
            return;
          }
          final var reference = saved.get();
          final Path exact = Path.of(reference.registryPath());
          if (!exact.equals(file)) {
            if (!Files.isRegularFile(exact)) {
              selectedId = null;
              resetCard();
              paint();
              linkState.setText("Карточка недоступна: " + exact + " · " + reference.compoundId());
              linkState.setVisible(true);
              linkState.setManaged(true);
              tabs.getSelectionModel().select(0);
              status.setText(
                  "Сохранённый реестр отсутствует или перемещён. Другой файл с таким же QC-номером не выбран.");
              return;
            }
            file = exact;
            fingerprint = null;
            selectedId = null;
            deferredComponent = id;
            reload();
            return;
          }
          ready();
          final var card =
              document.registry().compounds().stream()
                  .filter(c -> c.id().equals(reference.compoundId()))
                  .findFirst()
                  .orElse(null);
          if (card == null || !new LaboratoryRegistryService().sameCard(reference, file, card)) {
            selectedId = null;
            resetCard();
            paint();
            linkState.setText(
                "Сохранённая карточка "
                    + reference.compoundId()
                    + " не найдена в этой версии реестра.");
            linkState.setVisible(true);
            linkState.setManaged(true);
            tabs.getSelectionModel().select(0);
            status.setText(
                "Номер, дата регистрации или структура карточки не совпали. Ссылка не переназначена.");
            return;
          }
          select(card.id());
          filter();
          tabs.getSelectionModel().select(0);
          status.setText("Открыта сохранённая карточка " + card.id() + ". " + linkState.getText());
        });
  }

  private void refreshComponentChoices(final String selectId) {
    final boolean old = changing;
    changing = true;
    component
        .getItems()
        .setAll(
            editor.document().experiment().components().stream()
                .map(
                    c ->
                        new Choice(
                            c.id(),
                            c.molecule().displayName()
                                + " · "
                                + LaboratoryInspector.roleText(c.role())))
                .toList());
    component.setValue(
        component.getItems().stream()
            .filter(c -> c.id().equals(selectId))
            .findFirst()
            .orElse(null));
    changing = old;
  }

  private void refreshLinks() {
    uses.getChildren().clear();
    uses.setVisible(false);
    uses.setManaged(false);
    linkState.setText("");
    linkState.setVisible(false);
    linkState.setManaged(false);
    if (selectedId == null || file == null) {
      return;
    }
    final var card = document.registry().compound(selectedId);
    final var service = new LaboratoryRegistryService();
    for (final var c : editor.document().experiment().components()) {
      try {
        final var reference = MolecularRegistryReference.read(c.molecule()).orElse(null);
        if (reference == null || !service.sameCard(reference, file, card)) {
          continue;
        }
        boolean current = reference.matches(c.molecule());
        if (current) {
          try {
            service.requireExact(drawing(c.id()), document.layouts().get(selectedId));
          } catch (final RuntimeException mismatch) {
            current = false;
          }
        }
        final String state =
            current ? "Связь актуальна" : "Структура участника изменена — связь требует проверки";
        if (component.getValue() != null && c.id().equals(component.getValue().id())) {
          linkState.setText(state);
        }
        final var open =
            button(
                c.molecule().displayName()
                    + (current ? " · в этом опыте" : " · структура изменена"),
                () -> {
                  if (canClose() && openParticipant != null) {
                    openParticipant.accept(c.id());
                  }
                });
        open.setTooltip(new Tooltip(c.id() + "\n" + state));
        open.setDisable(openParticipant == null);
        uses.getChildren().add(open);
      } catch (final RuntimeException invalid) {
        if (component.getValue() != null && c.id().equals(component.getValue().id())) {
          linkState.setText("Ссылка участника повреждена: " + invalid.getMessage());
        }
      }
    }
    uses.setVisible(!uses.getChildren().isEmpty());
    uses.setManaged(uses.isVisible());
    linkState.setVisible(!linkState.getText().isBlank());
    linkState.setManaged(linkState.isVisible());
  }

  private MoleculeDrawing drawing(final String id) {
    final var c =
        editor.document().experiment().components().stream()
            .filter(v -> v.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Участник удалён из опыта."));
    return editor
        .document()
        .layouts()
        .getOrDefault(id, new LaboratoryMoleculePlacement().place(c.molecule(), 200, 200));
  }

  private void linkParticipant() {
    attempt(
        () -> {
          ready();
          requireCleanCard();
          requireSelectedCard();
          if (component.getValue() == null) {
            throw new IllegalArgumentException("Выберите участника на вкладке «Из опыта».");
          }
          final var expected = editor.document();
          final String id = component.getValue().id();
          final var linked =
              new LaboratoryRegistryService()
                  .linked(
                      file,
                      document.registry().compound(selectedId),
                      document.layouts().get(selectedId),
                      drawing(id));
          if (editor.document() != expected) {
            throw new IllegalArgumentException("Опыт изменился; проверьте участника заново.");
          }
          if (!linked.equals(drawing(id))) {
            editor.editComponents(Map.of(id, linked), Set.of());
          }
          refreshLinks();
          status.setText(
              "Участник связан с "
                  + selectedId
                  + ". Сохраните опыт. Количество, свойства и факты не изменены.");
        });
  }

  private String linkAfterWrite(
      final LaboratoryDrawing expected, final String id, final String cardId) {
    if (editor.document() != expected) {
      return "Реестр сохранён. Опыт за это время изменился; ссылка не добавлена. Свяжите участника отдельно после проверки.";
    }
    try {
      final var linked =
          new LaboratoryRegistryService()
              .linked(
                  file,
                  document.registry().compound(cardId),
                  document.layouts().get(cardId),
                  drawing(id));
      if (!linked.equals(drawing(id))) {
        editor.editComponents(Map.of(id, linked), Set.of());
      }
      refreshLinks();
      return "Участник связан с карточкой; сохраните опыт. Количества не изменены.";
    } catch (final RuntimeException failure) {
      return "Реестр сохранён, но ссылка в опыте не записана: " + failure.getMessage();
    }
  }

  private void takeSavedNames() {
    attempt(
        () -> {
          final var saved = new LaboratoryRegistryService().savedNames(sourceDrawing());
          final var combined = new ArrayList<>(names(registrationAliases.getText()));
          combined.addAll(saved.values());
          final var values =
              LaboratoryRegistryService.distinctNames(combined).stream()
                  .filter(v -> !v.equalsIgnoreCase(registrationName.getText().strip()))
                  .toList();
          final String provenance =
              LaboratoryRegistryService.appendSource(registrationSource.getText(), saved.source());
          if (values.size() > 100
              || values.stream().anyMatch(v -> v.length() > 1000)
              || provenance.length() > 20000) {
            throw new IllegalArgumentException(
                "Названия не помещаются в карточку реестра. Перенесите выбранные названия вручную; исходные поля сохранены.");
          }
          registrationAliases.setText(String.join("\n", values));
          registrationSource.setText(provenance);
          registrationDirty = true;
          status.setText(
              "Сохранённые названия и их источник перенесены в видимые поля. Проверьте их перед регистрацией; новый сетевой поиск не выполнялся.");
        });
  }

  private void clearDuplicate() {
    duplicateReview = null;
    duplicate.getChildren().clear();
    duplicate.setVisible(false);
    duplicate.setManaged(false);
  }

  private void showDuplicate(
      final LaboratoryRegistryCodec.Document snapshot,
      final String hash,
      final LaboratoryDrawing experiment,
      final String componentId,
      final MoleculeDrawing drawing,
      final String id,
      final String preferred,
      final List<String> aliases,
      final String source) {
    final var merged =
        new LaboratoryRegistryService()
            .mergeNames(
                snapshot.registry(), snapshot.layouts(), id, drawing, preferred, aliases, source);
    final var before = snapshot.registry().compound(id);
    final var after = merged.compound(id);
    duplicateReview =
        new DuplicateReview(
            snapshot, hash, experiment, componentId, drawing, id, preferred, aliases, source);
    final var additions =
        after.aliases().stream()
            .filter(v -> before.aliases().stream().noneMatch(old -> old.equalsIgnoreCase(v)))
            .toList();
    final var heading =
        note("Точная структура уже зарегистрирована: " + before + ". Новый номер не нужен.");
    final var changes =
        note(
            "Добавить названия: "
                + (additions.isEmpty() ? "новых нет" : String.join("; ", additions))
                + "\nПредпочтительное имя останется: "
                + before.displayName());
    final var provenance = new TitledPane("Источник для добавления", note(source));
    provenance.setExpanded(false);
    duplicate
        .getChildren()
        .setAll(
            heading,
            changes,
            provenance,
            new FlowPane(
                8,
                8,
                button("Добавить сведения и связать участника", this::mergeDuplicate),
                button(
                    "Отмена объединения",
                    () -> {
                      clearDuplicate();
                      status.setText("Объединение отменено. Поля регистрации сохранены.");
                    })));
    duplicate.setVisible(true);
    duplicate.setManaged(true);
    tabs.getSelectionModel().select(1);
    status.setText(
        "Найдено точное совпадение "
            + id
            + ". Просмотрите добавляемые сведения ниже; объединение выполняется только вашей кнопкой.");
  }

  private void mergeDuplicate() {
    attempt(
        () -> {
          ready();
          requireCleanCard();
          if (relationDirty) {
            throw new IllegalArgumentException(
                "Сохраните или сбросьте ввод связи форм перед объединением.");
          }
          final var review = duplicateReview;
          if (review == null) {
            throw new IllegalArgumentException("Сначала проверьте точное совпадение.");
          }
          if (document != review.document()
              || !Objects.equals(fingerprint, review.fingerprint())
              || editor.document() != review.experiment()) {
            throw new IllegalArgumentException(
                "Реестр или опыт изменился. Проверьте совпадение заново; введённые поля сохранены.");
          }
          final var registry =
              new LaboratoryRegistryService()
                  .mergeNames(
                      document.registry(),
                      document.layouts(),
                      review.id(),
                      review.drawing(),
                      review.name(),
                      review.aliases(),
                      review.source());
          final var next = new LaboratoryRegistryCodec.Document(registry, document.layouts());
          final Path target = file;
          job(
              "Добавляю сведения в существующую карточку…",
              () -> repository.write(target, next, review.fingerprint()),
              hash -> {
                document = next;
                fingerprint = hash;
                registrationDirty = false;
                clearDuplicate();
                select(review.id());
                filter();
                tabs.getSelectionModel().select(0);
                status.setText(
                    "Сведения добавлены в "
                        + review.id()
                        + "; новый номер не создан. "
                        + linkAfterWrite(review.experiment(), review.componentId(), review.id()));
              });
        });
  }

  private void loadHistory() {
    attempt(
        () -> {
          ready();
          job(
              "Читаю историю реестра…",
              () -> repository.history(file),
              paths -> {
                history.getItems().setAll(paths);
                status.setText("Сохранённых версий: " + paths.size());
              });
        });
  }

  private void showHistory() {
    attempt(
        () -> {
          final Path selected = history.getSelectionModel().getSelectedItem();
          if (selected == null) {
            throw new IllegalArgumentException("Выберите версию реестра.");
          }
          job(
              "Открываю сохранённую версию…",
              () -> repository.read(selected),
              opened -> {
                final var text =
                    new TextArea(
                        "Версия "
                            + selected.getFileName()
                            + "\n\n"
                            + opened.document().registry().compounds().stream()
                                .map(
                                    c ->
                                        c
                                            + "\nНазвания: "
                                            + String.join("; ", c.aliases())
                                            + "\nИсточник: "
                                            + c.source())
                                .collect(java.util.stream.Collectors.joining("\n\n")));
                text.setEditable(false);
                text.setWrapText(true);
                text.setPrefSize(740, 500);
                final var dialog = new Dialog<Void>();
                dialog.initOwner(owner);
                dialog.setTitle("История реестра · только чтение");
                dialog.getDialogPane().setContent(text);
                dialog
                    .getDialogPane()
                    .getButtonTypes()
                    .add(new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE));
                dialog.setResizable(true);
                LaboratoryDialogStyle.apply(dialog, owner);
                dialog.show();
              });
        });
  }

  private void fit() {
    zoom = 1;
    panX = panY = 0;
    paint();
  }

  private void paint() {
    final var g = canvas.getGraphicsContext2D();
    g.setTransform(1, 0, 0, 1, 0, 0);
    LaboratoryPreviewTheme.clear(g);
    if (selectedId == null || !document.layouts().containsKey(selectedId)) {
      return;
    }
    final var drawing = document.layouts().get(selectedId);
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final double scale =
        Math.min(
                (canvas.getWidth() - 30) / Math.max(1, bounds.getWidth()),
                (canvas.getHeight() - 30) / Math.max(1, bounds.getHeight()))
            * zoom;
    g.save();
    g.translate(canvas.getWidth() / 2 + panX, canvas.getHeight() / 2 + panY);
    g.scale(scale, scale);
    g.translate(
        -bounds.getMinX() - bounds.getWidth() / 2, -bounds.getMinY() - bounds.getHeight() / 2);
    LaboratoryPreviewTheme.paint(g, drawing);
    g.restore();
  }

  private <T> void job(final String message, final Work<T> work, final Consumer<T> done) {
    busy(true);
    getCenter().setDisable(true);
    status.setText(message);
    worker =
        Thread.ofVirtual()
            .start(
                () -> {
                  try {
                    final T result = work.run();
                    Platform.runLater(
                        () -> {
                          if (!disposed) {
                            busy(false);
                            getCenter().setDisable(false);
                            try {
                              done.accept(result);
                            } catch (final RuntimeException exception) {
                              status.setText(
                                  Objects.toString(
                                      exception.getMessage(), "Ошибка обновления карточки."));
                            }
                          }
                        });
                  } catch (final Exception exception) {
                    Platform.runLater(
                        () -> {
                          if (!disposed) {
                            busy(false);
                            getCenter().setDisable(false);
                            if (!initialized) {
                              initialized = true;
                              failedLoad = true;
                            }
                            status.setText(
                                Objects.toString(exception.getMessage(), "Операция остановлена."));
                          }
                        });
                  }
                });
  }

  private void attempt(final Runnable action) {
    if (running) {
      status.setText("Дождитесь завершения текущей операции.");
      return;
    }
    try {
      action.run();
    } catch (final RuntimeException exception) {
      status.setText(Objects.toString(exception.getMessage(), "Проверьте поля карточки."));
    }
  }

  boolean pending() {
    return running || dirty || registrationDirty || relationDirty;
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) {
      return false;
    }
    if (dirty || registrationDirty || relationDirty) {
      status.setText("Сохраните или сбросьте незавершённые поля карточки, регистрации или связи.");
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    if (worker != null) {
      worker.interrupt();
    }
  }

  void draft(final Properties data, final boolean restore) {
    if (restore && !initialized) {
      deferredDraft = new Properties();
      deferredDraft.putAll(data);
      return;
    }
    if (restore && data.containsKey("registry.folder")) {
      final Path stored =
          LaboratoryRegistryRepository.file(Path.of(data.getProperty("registry.folder")));
      if (!stored.equals(file)) {
        deferredDraft = new Properties();
        deferredDraft.putAll(data);
        file = stored;
        fingerprint = null;
        selectedId = null;
        dirty = registrationDirty = relationDirty = false;
        reload();
        return;
      }
    }
    changing = true;
    LaboratoryFormDraft.fields(
        data,
        "registry",
        restore,
        Map.of(
            "search",
            search,
            "name",
            name,
            "aliases",
            aliases,
            "source",
            source,
            "registrationName",
            registrationName,
            "registrationAliases",
            registrationAliases,
            "registrationSource",
            registrationSource,
            "relationNote",
            relationNote,
            "relationKind",
            relationKind,
            "role",
            role));
    if (restore) {
      includeNames.setSelected(
          Boolean.parseBoolean(data.getProperty("registry.includeNames", "true")));
      final String stored = data.getProperty("registry.fingerprint", "");
      if (!Objects.equals(stored, Objects.toString(fingerprint, ""))) {
        status.setText(
            "Реестр изменился после сохранения черновика. Поля сохранены, но перед записью сравните их с актуальной карточкой.");
        fingerprint = stored.isBlank() ? null : stored;
      }
      selectedId = data.getProperty("registry.selected", "");
      if (selectedId.isBlank()
          || document.registry().compounds().stream().noneMatch(c -> c.id().equals(selectedId))) {
        selectedId = null;
      } else {
        final var c = document.registry().compound(selectedId);
        identity.setText(c.id() + " · восстановленные поля карточки");
        relationTarget
            .getItems()
            .setAll(
                document.registry().compounds().stream()
                    .filter(v -> !v.id().equals(selectedId))
                    .toList());
        relations
            .getItems()
            .setAll(
                document.registry().relations().stream()
                    .filter(r -> r.firstId().equals(selectedId) || r.secondId().equals(selectedId))
                    .toList());
      }
      component.setValue(
          component.getItems().stream()
              .filter(c -> c.id().equals(data.getProperty("registry.component")))
              .findFirst()
              .orElse(component.getValue()));
      relationTarget.setValue(
          relationTarget.getItems().stream()
              .filter(c -> c.id().equals(data.getProperty("registry.target")))
              .findFirst()
              .orElse(null));
      dirty = Boolean.parseBoolean(data.getProperty("registry.dirty"));
      registrationDirty = Boolean.parseBoolean(data.getProperty("registry.registrationDirty"));
      relationDirty = Boolean.parseBoolean(data.getProperty("registry.relationDirty"));
      final int tab = Integer.parseInt(data.getProperty("registry.tab", "0"));
      tabs.getSelectionModel().select(Math.max(0, Math.min(3, tab)));
      filter();
      paint();
    } else {
      data.setProperty("registry.includeNames", Boolean.toString(includeNames.isSelected()));
      data.setProperty("registry.selected", Objects.toString(selectedId, ""));
      data.setProperty("registry.fingerprint", Objects.toString(fingerprint, ""));
      data.setProperty(
          "registry.component", component.getValue() == null ? "" : component.getValue().id());
      data.setProperty(
          "registry.target",
          relationTarget.getValue() == null ? "" : relationTarget.getValue().id());
      data.setProperty("registry.dirty", Boolean.toString(dirty));
      data.setProperty("registry.registrationDirty", Boolean.toString(registrationDirty));
      data.setProperty("registry.relationDirty", Boolean.toString(relationDirty));
      data.setProperty(
          "registry.tab", Integer.toString(tabs.getSelectionModel().getSelectedIndex()));
      if (file != null) {
        data.setProperty("registry.folder", file.getParent().toString());
      }
    }
    changing = false;
    if (dirty) {
      cardFields.setExpanded(true);
    }
    refreshLinks();
  }

  private static List<String> names(final String text) {
    return text.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
  }

  private static VBox field(final String title, final Node content) {
    final var box = new VBox(5, note(title), content);
    if (content instanceof Region region) {
      region.setMaxWidth(Double.MAX_VALUE);
    }
    return box;
  }

  private static Button button(final String title, final Runnable action) {
    final var button = new Button(title);
    button.setOnAction(e -> action.run());
    return button;
  }
}
