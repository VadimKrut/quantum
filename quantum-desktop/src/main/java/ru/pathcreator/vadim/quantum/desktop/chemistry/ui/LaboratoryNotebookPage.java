/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNotebookEvidence;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNotebookRepository;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNotebookRepository.*;

/** Organization is saved separately from scientific records. Every experiment remains a normal QCHEM. */
final class LaboratoryNotebookPage extends LaboratoryComputationPage {
  record ViewState(
      String projectId, String sectionId, String selectedItemId, String filter, String query) {}

  private enum Filter {
    ACTIVE("Активные"),
    WAITING("Ожидают"),
    COMPLETED("Завершённые"),
    ALL("Все опыты");
    private final String title;

    Filter(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return this.title;
    }
  }

  private enum Form {
    NONE,
    PROJECT,
    SECTION,
    EXPERIMENT,
    WORKFLOW
  }

  private record SectionChoice(String id, String title) {
    @Override
    public String toString() {
      return this.title;
    }
  }

  private static final int PAGE_SIZE = 12;
  private final LaboratoryNotebookRepository repository;
  private final Consumer<Path> openExperiment;
  private Supplier<LaboratoryDrawing> planSource;
  private BiConsumer<Path, String> quickAction;
  private final ComboBox<Project> projects = new ComboBox<>();
  private final TreeView<SectionChoice> sections = new TreeView<>();
  private final ComboBox<Filter> filter = new ComboBox<>();
  private final TextField query = new TextField(),
      projectName = new TextField(),
      sectionName = new TextField(),
      experimentName = new TextField();
  private final ComboBox<String> template = new ComboBox<>();
  private final ComboBox<Phase> phase = new ComboBox<>();
  private final ComboBox<SectionChoice> itemSection = new ComboBox<>();
  private final TextField waitingFor = new TextField(), nextAction = new TextField();
  private final Label location = note("Выберите или создайте проект"),
      counts = note(""),
      editHeading = note("");
  private final VBox cards = new VBox(10),
      projectForm = new VBox(8),
      sectionForm = new VBox(8),
      experimentForm = new VBox(8),
      workflowForm = new VBox(8);
  private final ScrollPane board = scroll(this.cards);
  private final VBox sidebar = new VBox(10), content = new VBox(10), footer = new VBox(8);
  private final Button newProject,
      newSection,
      newExperiment,
      refresh,
      saveWorkflow,
      discardWorkflow,
      cancelRead,
      previous,
      more;
  private final HBox columns = new HBox(14);
  private Snapshot snapshot;
  private String sectionId, selectedItemId;
  private Item editing;
  private Form form = Form.NONE;
  private boolean changing, writing, conflict;
  private int page;
  private long generation, evidenceGeneration;
  private Thread worker, evidenceWorker;
  private ViewState deferredView;
  private boolean newProjectRequested;
  private Properties deferredDraft;
  private final Map<Path, LaboratoryNotebookEvidence.Summary> summaries = new HashMap<>();
  private final Map<Path, String> readErrors = new HashMap<>();

  LaboratoryNotebookPage(
      final LaboratoryNotebookRepository repository,
      final Consumer<Path> openExperiment,
      final Runnable close) {
    super("Проекты и опыты", close);
    this.repository = Objects.requireNonNull(repository);
    this.openExperiment = Objects.requireNonNull(openExperiment);
    this.setId("chemistry-notebook-page");
    this.setMinSize(0, 0);
    this.changing = true;
    this.projects.setId("notebook-project");
    this.projects.setMaxWidth(Double.MAX_VALUE);
    this.projects.setPrefWidth(400);
    this.projects.setConverter(converter(Project::title));
    this.projects.setPromptText("Выберите проект");
    this.newProject = button("Новый проект", "new-project", () -> this.openForm(Form.PROJECT));
    this.refresh = button("Обновить", "refresh", this::refreshRequested);
    this.projectName.setId("notebook-project-name");
    this.projectName.setPromptText("Например, Весенние синтезы");
    this.projectForm
        .getChildren()
        .setAll(
            field("Название нового проекта", this.projectName),
            actions(
                button("Создать проект", "create-project", this::createProject),
                button("Отменить", "cancel-project", this::resetForm)));
    final HBox projectBar = new HBox(10, this.projects, this.newProject, this.refresh);
    HBox.setHgrow(this.projects, Priority.ALWAYS);
    this.sections.setId("notebook-sections");
    this.sections.setMinSize(0, 140);
    this.sections.setPrefHeight(420);
    VBox.setVgrow(this.sections, Priority.ALWAYS);
    this.sections.setCellFactory(
        tree ->
            new TreeCell<>() {
              {
                setWrapText(true);
                prefWidthProperty().bind(tree.widthProperty().subtract(18));
                setMaxWidth(USE_PREF_SIZE);
              }

              @Override
              protected void updateItem(final SectionChoice value, final boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.title());
                setTooltip(empty || value == null ? null : new Tooltip(value.title()));
              }
            });
    this.newSection = button("+ Раздел", "new-section", () -> this.openForm(Form.SECTION));
    this.sectionName.setId("notebook-section-name");
    this.sectionName.setPromptText("Например, Гетероциклы с серой");
    this.sectionForm
        .getChildren()
        .setAll(
            field("Новый подраздел выбранного раздела", this.sectionName),
            actions(
                button("Создать", "create-section", this::createSection),
                button("Отменить", "cancel-section", this::resetForm)));
    this.sidebar
        .getChildren()
        .setAll(note("Разделы проекта"), this.sections, this.newSection, this.sectionForm);
    this.sidebar.setMinWidth(180);
    this.sidebar.setPrefWidth(240);
    this.sidebar.setMaxWidth(260);
    this.location.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    this.location.setMinWidth(0);
    this.newExperiment =
        button("Новый опыт", "new-experiment", () -> this.openForm(Form.EXPERIMENT));
    this.newExperiment.getStyleClass().add("laboratory-primary-action");
    this.experimentName.setId("notebook-experiment-name");
    this.experimentName.setPromptText("Например, Тиазол · серия A · опыт 1");
    this.template.setId("notebook-template");
    this.template.getItems().setAll("Пустой опыт");
    this.template.getSelectionModel().selectFirst();
    this.template.setMaxWidth(Double.MAX_VALUE);
    this.experimentForm
        .getChildren()
        .setAll(
            field("Название опыта", this.experimentName),
            field("Начать", this.template),
            note(
                "Создаётся отдельный план. Выполнение, образцы и аналитические результаты не переносятся как факты."),
            actions(
                button("Создать опыт", "create-experiment", this::createExperiment),
                button("Отменить", "cancel-experiment", this::resetForm)));
    this.filter.getItems().setAll(Filter.values());
    this.filter.setValue(Filter.ACTIVE);
    this.filter.setId("notebook-filter");
    this.query.setId("notebook-query");
    this.query.setPromptText("Найти по названию или следующему действию");
    this.query.setPrefWidth(330);
    HBox.setHgrow(this.query, Priority.ALWAYS);
    final HBox heading = new HBox(12, this.location, new Region(), this.newExperiment);
    HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);
    final HBox filters = new HBox(8, this.filter, this.query);
    filters.setMinWidth(0);
    this.cards.setPadding(new Insets(2, 3, 8, 0));
    this.cards.setMinWidth(0);
    this.board.setFitToHeight(false);
    this.board.setMinSize(0, 150);
    VBox.setVgrow(this.board, Priority.ALWAYS);
    this.previous =
        button(
            "← Назад",
            "previous",
            () -> {
              this.page = Math.max(0, this.page - 1);
              this.renderCards(true);
            });
    this.more =
        button(
            "Далее →",
            "more",
            () -> {
              this.page++;
              this.renderCards(true);
            });
    this.content
        .getChildren()
        .setAll(
            heading,
            filters,
            this.counts,
            this.experimentForm,
            this.board,
            actions(this.previous, this.more));
    this.content.setMinSize(0, 0);
    HBox.setHgrow(this.content, Priority.ALWAYS);
    this.columns.getChildren().setAll(this.sidebar, this.content);
    this.columns.setMinSize(0, 0);
    VBox.setVgrow(this.columns, Priority.ALWAYS);
    final VBox body = new VBox(12, projectBar, this.projectForm, this.columns);
    body.setPadding(new Insets(0, 16, 10, 16));
    body.setMinSize(0, 0);
    this.setCenter(body);
    this.phase.getItems().setAll(Phase.values());
    this.phase.setConverter(converter(LaboratoryNotebookPage::phaseName));
    this.phase.setId("notebook-phase");
    this.phase.setPrefWidth(185);
    this.itemSection.setId("notebook-item-section");
    this.itemSection.setPrefWidth(250);
    this.itemSection.setConverter(converter(SectionChoice::title));
    this.waitingFor.setId("notebook-waiting");
    this.waitingFor.setPromptText("Например, ЯМР образца A-1");
    this.waitingFor.setPrefWidth(280);
    this.nextAction.setId("notebook-next");
    this.nextAction.setPromptText("Например, после ЯМР выбрать очистку");
    this.nextAction.setPrefWidth(350);
    this.saveWorkflow = button("Сохранить статус", "save-workflow", this::saveWorkflow);
    this.saveWorkflow.getStyleClass().add("laboratory-save");
    this.discardWorkflow = button("Отменить ввод", "discard-workflow", this::resetForm);
    this.workflowForm
        .getChildren()
        .setAll(
            this.editHeading,
            new FlowPane(
                12,
                8,
                field("Раздел", this.itemSection),
                field("Статус работы", this.phase),
                field("Что ожидаете", this.waitingFor),
                field("Следующее действие", this.nextAction)),
            actions(this.saveWorkflow, this.discardWorkflow),
            note(
                "Это организационные отметки. Они не подтверждают результат реакции или завершение анализа."));
    this.cancelRead = button("Остановить чтение", "cancel-read", this::cancelRead);
    this.footer.getChildren().setAll(this.workflowForm, this.status, this.cancelRead);
    this.footer.setPadding(new Insets(8, 16, 12, 16));
    this.setBottom(this.footer);
    this.projects
        .valueProperty()
        .addListener(
            (o, old, value) -> {
              if (this.changing || Objects.equals(old, value)) return;
              if (!this.mayNavigate()) {
                this.changing = true;
                this.projects.setValue(old);
                this.changing = false;
                return;
              }
              if (value != null) this.loadProject(value);
            });
    this.sections
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, old, value) -> {
              if (this.changing || value == null) return;
              if (!this.mayNavigate()) {
                this.changing = true;
                this.sections.getSelectionModel().select(old);
                this.changing = false;
                return;
              }
              this.sectionId = value.getValue().id();
              this.selectedItemId = null;
              this.page = 0;
              this.renderCards(true);
            });
    this.filter
        .valueProperty()
        .addListener(
            o -> {
              if (!this.changing) {
                this.page = 0;
                this.renderCards(true);
              }
            });
    this.query
        .textProperty()
        .addListener(
            o -> {
              if (!this.changing) {
                this.page = 0;
                this.renderCards(true);
              }
            });
    for (final TextField input :
        List.of(
            this.projectName,
            this.sectionName,
            this.experimentName,
            this.waitingFor,
            this.nextAction)) input.textProperty().addListener(o -> this.inputChanged());
    this.phase.valueProperty().addListener(o -> this.inputChanged());
    this.template.valueProperty().addListener(o -> this.inputChanged());
    this.itemSection.valueProperty().addListener(o -> this.inputChanged());
    this.widthProperty()
        .addListener(o -> this.sidebar.setPrefWidth(this.getWidth() < 1100 ? 200 : 240));
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (this.shortcut(e)) e.consume();
        });
    this.changing = false;
    this.updateControls();
    this.refreshProjects();
  }

  private static <T> StringConverter<T> converter(
      final java.util.function.Function<T, String> text) {
    return new StringConverter<>() {
      @Override
      public String toString(final T value) {
        return value == null ? "" : text.apply(value);
      }

      @Override
      public T fromString(final String value) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private Button button(final String text, final String id, final Runnable action) {
    final Button button = new Button(text);
    button.setId("notebook-" + id);
    button.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException error) {
            this.status.setText(message(error));
          }
        });
    return button;
  }

  private static FlowPane actions(final Node... nodes) {
    return new FlowPane(8, 6, nodes);
  }

  private static VBox field(final String title, final Node input) {
    final VBox box = new VBox(4, note(title), input);
    box.setMinWidth(0);
    return box;
  }

  private static void visible(final Node node, final boolean show) {
    node.setVisible(show);
    node.setManaged(show);
  }

  private static String message(final Throwable error) {
    return Objects.toString(error.getMessage(), error.getClass().getSimpleName());
  }

  private static String phaseName(final Phase phase) {
    return switch (phase) {
      case PLANNED -> "Запланирован";
      case IN_PROGRESS -> "В работе";
      case WORKUP -> "Обработка";
      case PURIFICATION -> "Очистка";
      case WAITING -> "Ожидает";
      case REVIEW -> "Разбор результатов";
      case COMPLETED -> "Завершён";
      case PAUSED -> "Приостановлен";
    };
  }

  void actions(final BiConsumer<Path, String> callback) {
    this.quickAction = callback;
    this.renderCards(false);
  }

  Snapshot tutorialSnapshot() {
    return this.snapshot;
  }

  void planSource(final Supplier<LaboratoryDrawing> source) {
    this.planSource = source;
    final String current = this.template.getValue();
    this.template
        .getItems()
        .setAll(
            source == null ? List.of("Пустой опыт") : List.of("Из текущего плана", "Пустой опыт"));
    this.template.setValue(
        this.template.getItems().contains(current) ? current : this.template.getItems().getFirst());
  }

  private boolean workflowPending() {
    return this.editing != null
        && (this.phase.getValue() != this.editing.phase()
            || !Objects.equals(
                this.itemSection.getValue() == null ? null : this.itemSection.getValue().id(),
                this.editing.sectionId())
            || !this.waitingFor.getText().equals(this.editing.waitingFor())
            || !this.nextAction.getText().equals(this.editing.nextAction()));
  }

  boolean pending() {
    return this.deferredDraft != null
        || !this.projectName.getText().isEmpty()
        || !this.sectionName.getText().isEmpty()
        || !this.experimentName.getText().isEmpty()
        || this.workflowPending()
        || this.conflict;
  }

  private void inputChanged() {
    if (this.changing) return;
    this.updateControls();
    if (this.pending())
      this.status.setText("Изменения ещё не сохранены. Сохраните эту форму или отмените ввод.");
  }

  private boolean mayNavigate() {
    if (this.running) {
      this.status.setText(
          this.writing
              ? "Дождитесь завершения записи проекта."
              : "Дождитесь чтения или остановите его.");
      return false;
    }
    if (this.pending()) {
      this.status.setText("Сначала сохраните изменения или отмените ввод. Поля остаются на месте.");
      return false;
    }
    return true;
  }

  private void openForm(final Form next) {
    if (!this.mayNavigate()) return;
    if (next != Form.PROJECT && this.snapshot == null) {
      this.status.setText("Сначала создайте или выберите проект.");
      return;
    }
    this.form = next;
    this.editing = null;
    this.conflict = false;
    this.updateControls();
    switch (next) {
      case PROJECT -> this.projectName.requestFocus();
      case SECTION -> this.sectionName.requestFocus();
      case EXPERIMENT -> this.experimentName.requestFocus();
      default -> {}
    }
  }

  private void resetForm() {
    if (this.running) return;
    this.changing = true;
    try {
      this.projectName.clear();
      this.sectionName.clear();
      this.experimentName.clear();
      this.waitingFor.clear();
      this.nextAction.clear();
      this.phase.setValue(null);
      this.editing = null;
      this.form = Form.NONE;
      this.conflict = false;
      this.deferredDraft = null;
    } finally {
      this.changing = false;
    }
    this.updateControls();
    this.status.setText("Незаписанный ввод отменён. Сохранённые данные не изменены.");
  }

  private void updateControls() {
    visible(this.projectForm, this.form == Form.PROJECT);
    visible(this.sectionForm, this.form == Form.SECTION);
    visible(this.experimentForm, this.form == Form.EXPERIMENT);
    visible(this.workflowForm, this.form == Form.WORKFLOW);
    visible(this.cancelRead, this.running && !this.writing);
    this.projects.setDisable(this.running);
    this.sections.setDisable(this.running);
    this.newProject.setDisable(this.running);
    this.newSection.setDisable(this.running || this.snapshot == null);
    this.newExperiment.setDisable(this.running || this.snapshot == null);
    this.refresh.setDisable(this.running);
    for (final Node n :
        List.of(this.projectForm, this.sectionForm, this.experimentForm, this.workflowForm))
      n.setDisable(this.running);
    this.saveWorkflow.setDisable(this.running || this.conflict || !this.workflowPending());
    this.discardWorkflow.setDisable(this.running);
    this.previous.setDisable(this.page == 0 || this.running);
  }

  private <T> void execute(
      final String title,
      final boolean mutation,
      final Callable<T> action,
      final Consumer<T> completed) {
    if (this.running || this.disposed) return;
    final long token = ++this.generation;
    this.writing = mutation;
    this.busy(true);
    this.updateControls();
    this.status.setText(title);
    this.worker =
        Thread.ofVirtual()
            .name("laboratory-notebook")
            .start(
                () -> {
                  try {
                    final T result = action.call();
                    Platform.runLater(
                        () -> {
                          if (this.disposed || token != this.generation) return;
                          this.busy(false);
                          this.writing = false;
                          this.worker = null;
                          this.updateControls();
                          completed.accept(result);
                        });
                  } catch (final Exception error) {
                    Platform.runLater(
                        () -> {
                          if (this.disposed || token != this.generation) return;
                          this.busy(false);
                          this.writing = false;
                          this.worker = null;
                          if (!mutation) {
                            this.changing = true;
                            this.projects.setValue(
                                this.snapshot == null ? null : this.snapshot.project());
                            this.changing = false;
                          }
                          this.updateControls();
                          this.status.setText(message(error));
                        });
                  }
                });
  }

  private void cancelRead() {
    if (!this.running || this.writing) return;
    ++this.generation;
    if (this.worker != null) this.worker.interrupt();
    this.worker = null;
    this.busy(false);
    this.changing = true;
    this.projects.setValue(this.snapshot == null ? null : this.snapshot.project());
    this.changing = false;
    this.updateControls();
    this.status.setText("Чтение остановлено. Данные не изменены.");
  }

  private void refreshRequested() {
    if (!this.mayNavigate()) return;
    this.deferredView = this.viewState();
    this.summaries.clear();
    this.readErrors.clear();
    this.refreshProjects();
  }

  void beginNewProject() {
    this.deferredView = null;
    this.newProjectRequested = true;
    if (!this.running) {
      this.newProjectRequested = false;
      this.openForm(Form.PROJECT);
    }
  }

  private void refreshProjects() {
    final String previous = this.snapshot == null ? null : this.snapshot.project().id();
    this.execute(
        "Читаю проекты…",
        false,
        this.repository::projects,
        list -> {
          this.changing = true;
          this.projects.getItems().setAll(list);
          this.projects.setValue(null);
          this.changing = false;
          if (this.newProjectRequested) {
            this.newProjectRequested = false;
            this.snapshot = null;
            this.sections.setRoot(null);
            this.cards.getChildren().clear();
            this.location.setText("Новый проект");
            this.counts.setText("");
            this.openForm(Form.PROJECT);
            return;
          }
          final String desired =
              this.deferredDraft != null
                  ? this.deferredDraft.getProperty("notebook.project")
                  : this.deferredView != null ? this.deferredView.projectId() : previous;
          final Project project =
              list.stream()
                  .filter(p -> p.id().equals(desired))
                  .findFirst()
                  .orElse(desired == null && !list.isEmpty() ? list.getFirst() : null);
          if (project != null) {
            this.loadProject(project);
            return;
          }
          this.snapshot = null;
          this.sections.setRoot(null);
          this.cards
              .getChildren()
              .setAll(
                  note(
                      list.isEmpty()
                          ? "Создайте проект, затем разделы и опыты."
                          : "Выберите проект из списка."));
          this.location.setText("Проекты");
          this.counts.setText("");
          this.form = list.isEmpty() ? Form.PROJECT : Form.NONE;
          this.updateControls();
          if (this.deferredDraft != null) this.restoreRawDraft();
          else
            this.status.setText(
                desired == null
                    ? "Проекты сохраняются в папке приложения; путь выбирать не требуется."
                    : "Сохранённый проект не найден. Данные не переназначены другому проекту.");
        });
  }

  private void loadProject(final Project project) {
    this.changing = true;
    this.projects.setValue(project);
    this.changing = false;
    this.execute(
        "Открываю проект «" + project.title() + "»…",
        false,
        () -> this.repository.load(project),
        value -> {
          this.snapshot = value;
          this.sectionId = null;
          this.selectedItemId = null;
          this.page = 0;
          this.form = Form.NONE;
          this.editing = null;
          this.conflict = false;
          this.summaries.clear();
          this.readErrors.clear();
          if (this.deferredView != null && project.id().equals(this.deferredView.projectId())) {
            this.sectionId = this.deferredView.sectionId();
            this.selectedItemId = this.deferredView.selectedItemId();
            this.setFilter(this.deferredView.filter(), this.deferredView.query());
            this.deferredView = null;
          }
          if (this.deferredDraft != null) {
            this.sectionId = nullable(this.deferredDraft.getProperty("notebook.section"));
            this.selectedItemId = nullable(this.deferredDraft.getProperty("notebook.item"));
          }
          this.rebuildSections();
          final List<Item> visible = this.filtered();
          for (int i = 0; i < visible.size(); i++)
            if (visible.get(i).id().equals(this.selectedItemId)) this.page = i / PAGE_SIZE;
          this.renderCards(true);
          this.updateControls();
          this.status.setText(
              "Проект открыт. Статусы работы сохраняются отдельно от результатов опытов.");
          if (this.deferredDraft != null) this.restoreRawDraft();
        });
  }

  private void accept(final Snapshot value, final String message) {
    this.snapshot = value;
    this.resetForm();
    this.rebuildSections();
    this.renderCards(true);
    this.updateControls();
    this.status.setText(message);
  }

  private void createProject() {
    if (this.conflict) return;
    final String title = this.projectName.getText();
    this.execute(
        "Создаю проект…",
        true,
        () -> this.repository.createProject(title),
        created -> {
          this.resetForm();
          this.deferredView = new ViewState(created.id(), null, null, Filter.ACTIVE.name(), "");
          this.refreshProjects();
        });
  }

  private void createSection() {
    if (this.snapshot == null || this.conflict) return;
    final Snapshot expected = this.snapshot;
    final String parent = this.sectionId, title = this.sectionName.getText();
    this.execute(
        "Сохраняю раздел…",
        true,
        () -> this.repository.addSection(expected, parent, title),
        value -> {
          this.sectionId =
              value.sections().stream()
                  .filter(
                      s -> expected.sections().stream().noneMatch(old -> old.id().equals(s.id())))
                  .map(Section::id)
                  .findFirst()
                  .orElse(parent);
          this.accept(value, "Раздел создан в проекте.");
        });
  }

  private void createExperiment() {
    if (this.snapshot == null || this.conflict) return;
    final Snapshot expected = this.snapshot;
    final String section = this.sectionId, title = this.experimentName.getText();
    if ("Из текущего плана".equals(this.template.getValue()) && this.planSource == null)
      throw new IllegalStateException(
          "Текущий план недоступен. Выберите пустой опыт либо вернитесь к исходному плану.");
    final LaboratoryDrawing source =
        "Из текущего плана".equals(this.template.getValue())
            ? Objects.requireNonNull(this.planSource.get(), "Текущий план недоступен.")
            : null;
    this.execute(
        "Создаю отдельный опыт…",
        true,
        () ->
            source == null
                ? this.repository.addExperiment(expected, section, title)
                : this.repository.addExperiment(expected, section, title, source),
        value -> {
          this.selectedItemId =
              value.items().stream()
                  .filter(i -> expected.items().stream().noneMatch(old -> old.id().equals(i.id())))
                  .map(Item::id)
                  .findFirst()
                  .orElse(null);
          this.accept(
              value,
              source == null
                  ? "Новый опыт создан. Откройте его, чтобы добавить схему и план."
                  : "Создан отдельный план; результаты исходного опыта не перенесены как факты. Вложения исходника не скопированы.");
        });
  }

  private void editWorkflow(final Item item) {
    if (!this.mayNavigate()) return;
    this.changing = true;
    this.editing = item;
    this.selectedItemId = item.id();
    this.form = Form.WORKFLOW;
    this.fillItemSections(item.sectionId());
    this.phase.setValue(item.phase());
    this.waitingFor.setText(item.waitingFor());
    this.nextAction.setText(item.nextAction());
    this.editHeading.setText("Работа по опыту · " + this.title(item));
    this.changing = false;
    this.updateControls();
    this.status.setText("Измените раздел, статус, ожидание или следующее действие и сохраните.");
  }

  private void fillItemSections(final String selected) {
    this.itemSection.getItems().setAll(new SectionChoice(null, "Весь проект"));
    if (this.snapshot != null)
      this.snapshot
          .sections()
          .forEach(
              s ->
                  this.itemSection
                      .getItems()
                      .add(new SectionChoice(s.id(), this.sectionPath(s.id()))));
    this.itemSection.setValue(
        this.itemSection.getItems().stream()
            .filter(s -> Objects.equals(s.id(), selected))
            .findFirst()
            .orElse(null));
  }

  private void saveWorkflow() {
    if (this.snapshot == null || this.editing == null || this.conflict || !this.workflowPending())
      return;
    if (this.itemSection.getValue() == null) {
      this.status.setText("Выберите раздел опыта.");
      return;
    }
    final Snapshot expected = this.snapshot;
    final String item = this.editing.id(),
        section = this.itemSection.getValue().id(),
        waiting = this.waitingFor.getText(),
        next = this.nextAction.getText();
    final Phase state = this.phase.getValue();
    this.execute(
        "Сохраняю организационный статус…",
        true,
        () -> this.repository.updateWorkflow(expected, item, section, state, waiting, next),
        value ->
            this.accept(
                value, "Раздел и статус работы сохранены. Научные записи опыта не изменены."));
  }

  void saveRequested() {
    if (this.running) {
      this.status.setText("Дождитесь завершения текущей операции.");
      return;
    }
    if (this.conflict) {
      this.status.setText(
          "Проект изменился после сохранения черновика. Ввод сохранён для просмотра; отмените его и обновите проект перед новой записью.");
      return;
    }
    switch (this.form) {
      case PROJECT -> this.createProject();
      case SECTION -> this.createSection();
      case EXPERIMENT -> this.createExperiment();
      case WORKFLOW -> this.saveWorkflow();
      case NONE ->
          this.status.setText(
              "Организационные изменения уже сохранены. Сам опыт сохраняется в его рабочем окне.");
    }
  }

  boolean shortcut(final KeyEvent event) {
    if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
      this.saveRequested();
      return true;
    }
    if (event.isShortcutDown() && event.getCode() == KeyCode.ENTER && this.form != Form.NONE) {
      this.saveRequested();
      return true;
    }
    return false;
  }

  private void open(final Item item) {
    if (!this.mayNavigate()) return;
    this.selectedItemId = item.id();
    this.openExperiment.accept(item.path());
  }

  private void quick(final Item item, final String action) {
    if (!this.mayNavigate() || this.quickAction == null) return;
    this.selectedItemId = item.id();
    this.quickAction.accept(item.path(), action);
  }

  private void rebuildSections() {
    if (this.snapshot == null) return;
    this.changing = true;
    try {
      final var root = new TreeItem<>(new SectionChoice(null, this.snapshot.project().title()));
      root.setExpanded(true);
      final Map<String, TreeItem<SectionChoice>> nodes = new HashMap<>();
      for (final Section s : this.snapshot.sections())
        nodes.put(s.id(), new TreeItem<>(new SectionChoice(s.id(), s.title())));
      for (final Section s : this.snapshot.sections()) {
        final var parent = s.parentId() == null ? root : nodes.get(s.parentId());
        if (parent != null) parent.getChildren().add(nodes.get(s.id()));
        nodes.get(s.id()).setExpanded(true);
      }
      this.sections.setRoot(root);
      final var selected = this.sectionId == null ? root : nodes.get(this.sectionId);
      if (selected == null) {
        this.sectionId = null;
        this.sections.getSelectionModel().select(root);
      } else this.sections.getSelectionModel().select(selected);
    } finally {
      this.changing = false;
    }
  }

  private Set<String> descendants() {
    final Set<String> ids = new HashSet<>();
    if (this.sectionId == null) return ids;
    ids.add(this.sectionId);
    for (int round = 0; round < this.snapshot.sections().size(); round++) {
      boolean added = false;
      for (final Section s : this.snapshot.sections())
        if (ids.contains(s.parentId())) added |= ids.add(s.id());
      if (!added) break;
    }
    return ids;
  }

  private List<Item> filtered() {
    if (this.snapshot == null) return List.of();
    final Set<String> ids = this.descendants();
    final String search = this.query.getText().strip().toLowerCase(Locale.ROOT);
    return this.snapshot.items().stream()
        .filter(i -> this.sectionId == null || ids.contains(i.sectionId()))
        .filter(
            i ->
                switch (this.filter.getValue()) {
                  case ALL -> true;
                  case ACTIVE -> i.phase() != Phase.COMPLETED;
                  case WAITING -> i.phase() == Phase.WAITING;
                  case COMPLETED -> i.phase() == Phase.COMPLETED;
                })
        .filter(
            i ->
                search.isEmpty()
                    || (this.title(i) + " " + i.waitingFor() + " " + i.nextAction())
                        .toLowerCase(Locale.ROOT)
                        .contains(search))
        .toList();
  }

  private String sectionPath(final String id) {
    if (this.snapshot == null) return "";
    final LinkedList<String> names = new LinkedList<>();
    String current = id;
    for (int n = 0; current != null && n < this.snapshot.sections().size(); n++) {
      final String key = current;
      final Section s =
          this.snapshot.sections().stream()
              .filter(x -> x.id().equals(key))
              .findFirst()
              .orElse(null);
      if (s == null) break;
      names.addFirst(s.title());
      current = s.parentId();
    }
    return names.isEmpty() ? this.snapshot.project().title() : String.join(" / ", names);
  }

  private String title(final Item item) {
    final var summary = this.summaries.get(item.path());
    if (summary != null) return summary.title();
    final String stem = item.path().getFileName().toString().replaceFirst("(?i)\\.qchem$", "");
    if (!this.readErrors.containsKey(item.path())) return stem;
    final String suffix = item.id().startsWith("item_") ? "-" + item.id().substring(5) : "";
    return !suffix.isEmpty() && stem.endsWith(suffix) && stem.length() > suffix.length()
        ? stem.substring(0, stem.length() - suffix.length())
        : stem;
  }

  private void renderCards(final boolean read) {
    if (this.snapshot == null) return;
    this.location.setText(this.sectionPath(this.sectionId));
    final List<Item> all = this.filtered();
    this.page = Math.min(this.page, Math.max(0, (all.size() - 1) / PAGE_SIZE));
    final int start = this.page * PAGE_SIZE, end = Math.min(all.size(), start + PAGE_SIZE);
    this.cards.getChildren().clear();
    this.counts.setText(
        "Показано "
            + (all.isEmpty() ? 0 : start + 1)
            + "–"
            + end
            + " из "
            + all.size()
            + " · организационные статусы");
    if (all.isEmpty())
      this.cards
          .getChildren()
          .add(
              note(
                  this.snapshot.items().isEmpty()
                      ? "Создайте первый опыт в этом проекте."
                      : this.snapshot.items().stream()
                              .anyMatch(
                                  i ->
                                      !this.summaries.containsKey(i.path())
                                          && !this.readErrors.containsKey(i.path()))
                          ? "Ищу по названиям опытов…"
                          : "В этом разделе и фильтре опытов нет."));
    for (final Item item : all.subList(start, end)) this.cards.getChildren().add(this.card(item));
    this.previous.setDisable(this.page == 0 || this.running);
    this.more.setDisable(end == all.size() || this.running);
    if (read) this.loadEvidence(this.snapshot.items());
  }

  private Node card(final Item item) {
    final String caption =
        !this.summaries.containsKey(item.path()) && !this.readErrors.containsKey(item.path())
            ? "Читаю название опыта…"
            : this.title(item);
    final Label title = note(caption);
    title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    title.setTooltip(new Tooltip(this.title(item) + "\n" + item.path()));
    final Label state = note(phaseName(item.phase()));
    state.setStyle("-fx-font-weight: bold;");
    final VBox text = new VBox(5, title, note(this.sectionPath(item.sectionId())), state);
    text.setMinWidth(0);
    HBox.setHgrow(text, Priority.ALWAYS);
    if (!item.waitingFor().isBlank())
      text.getChildren().add(note("Ожидание: " + item.waitingFor()));
    text.getChildren()
        .add(
            note(
                item.nextAction().isBlank()
                    ? "Следующее действие не задано"
                    : "Далее: " + item.nextAction()));
    final var evidence = this.summaries.get(item.path());
    final HBox row = new HBox(12);
    row.setMinWidth(0);
    if (evidence != null) {
      if (evidence.preview() != null) row.getChildren().add(this.preview(evidence.preview()));
      text.getChildren()
          .add(
              note(
                  "Записи опыта: шаги выполнены "
                      + evidence.procedure().completed()
                      + ", в работе "
                      + evidence.procedure().running()
                      + "; актуальных анализов "
                      + evidence.analyses().latestCount()
                      + "."));
      if (!evidence.analyses().recent().isEmpty()) {
        final var analysis = evidence.analyses().recent().getFirst();
        text.getChildren()
            .add(
                note(
                    "Последний анализ: "
                        + analysis.method()
                        + " · "
                        + when(analysis.at())
                        + " · "
                        + analysis.origin()));
      } else if (!evidence.recentActivity().isEmpty()) {
        final var recent = evidence.recentActivity().getFirst();
        text.getChildren()
            .add(
                note(
                    "Последняя запись: "
                        + recent.title()
                        + " · "
                        + Objects.toString(recent.origin(), "")
                        + " · "
                        + when(recent.at())));
      }
      if (!evidence.warnings().isEmpty())
        text.getChildren().add(note("Сведения неполны: " + evidence.warnings().getFirst()));
    } else
      text.getChildren()
          .add(
              note(
                  this.readErrors.containsKey(item.path())
                      ? "Не удалось прочитать опыт: " + this.readErrors.get(item.path())
                      : "Читаю записи опыта…"));
    final FlowPane links =
        actions(
            button("Открыть опыт", "open-" + item.id(), () -> this.open(item)),
            button(
                "Статус и следующее действие",
                "workflow-" + item.id(),
                () -> this.editWorkflow(item)));
    if (this.quickAction != null) {
      if (item.phase() == Phase.WAITING) {
        final Button analysis =
            button("Анализы", "analysis-" + item.id(), () -> this.quick(item, "analyses"));
        analysis.getStyleClass().add("laboratory-primary-action");
        links.getChildren().add(analysis);
      }
      final MenuButton more = new MenuButton("Перейти");
      for (final String[] action :
          new String[][] {
            {"Проведение", "procedure"},
            {"Обработка и очистка", "operations"},
            {"Анализы", "analyses"},
            {"Расширенная аналитика", "evidence"},
            {"Образцы", "samples"}
          }) {
        final MenuItem entry = new MenuItem(action[0]);
        entry.setOnAction(e -> this.quick(item, action[1]));
        more.getItems().add(entry);
      }
      links.getChildren().add(more);
    }
    text.getChildren().add(links);
    if (evidence != null) text.getChildren().add(this.evidenceDetails(evidence));
    row.getChildren().add(text);
    row.setPadding(new Insets(12));
    row.getStyleClass().add("laboratory-search-card");
    return row;
  }

  private static String when(final java.time.Instant at) {
    return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .format(at.atZone(ZoneId.systemDefault()));
  }

  private Node evidenceDetails(final LaboratoryNotebookEvidence.Summary evidence) {
    final VBox detail = new VBox(9);
    detail.setMinWidth(0);
    for (final var activity : evidence.recentActivity())
      detail
          .getChildren()
          .add(
              new VBox(
                  3,
                  note(activity.title() + " · " + when(activity.at()) + " · " + activity.origin()),
                  note(activity.detail())));
    if (evidence.recentActivity().isEmpty())
      detail.getChildren().add(note("Выполнение и наблюдения пока не записаны."));
    detail.getChildren().add(note("Образцы: " + evidence.sampleCount()));
    for (final var sample : evidence.samples()) {
      final VBox specimen =
          new VBox(
              3,
              note(sample.code() + " · " + sample.label()),
              sample.remaining() == null
                  ? note("Остаток не указан")
                  : LaboratoryQuantityText.label("Остаток: ", sample.remaining()),
              note(
                  sample.status()
                      + " · "
                      + (sample.location().isBlank() ? "Место не задано" : sample.location())));
      detail.getChildren().add(specimen);
    }
    for (final var analysis : evidence.analyses().recent())
      detail
          .getChildren()
          .add(
              new VBox(
                  3,
                  note(
                      analysis.method()
                          + " · "
                          + analysis.title()
                          + " · "
                          + when(analysis.at())
                          + " · "
                          + analysis.origin()),
                  note("Источник: " + analysis.source())));
    for (final String warning : evidence.warnings()) detail.getChildren().add(note(warning));
    final TitledPane pane = new TitledPane("Записи и образцы", detail);
    pane.setAnimated(false);
    pane.setExpanded(false);
    pane.setMinWidth(0);
    return pane;
  }

  private Node preview(final MoleculeDrawing drawing) {
    final Canvas canvas = new Canvas(170, 120);
    canvas.getStyleClass().add("theme-light");
    final StackPane frame = new StackPane(canvas);
    frame.setMinSize(120, 120);
    frame.setPrefSize(170, 120);
    frame.setMaxSize(170, 120);
    Tooltip.install(frame, new Tooltip(drawing.molecule().displayName()));
    canvas.widthProperty().bind(frame.widthProperty());
    final Runnable paint =
        () -> {
          final var g = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(g);
          final var b = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              Math.min(
                  1.3,
                  Math.min(
                      (canvas.getWidth() - 14) / Math.max(1, b.getWidth()),
                      106 / Math.max(1, b.getHeight())));
          g.save();
          try {
            g.translate(
                (canvas.getWidth() - b.getWidth() * scale) / 2 - b.getMinX() * scale,
                (120 - b.getHeight() * scale) / 2 - b.getMinY() * scale);
            g.scale(scale, scale);
            LaboratoryPreviewTheme.paint(g, drawing);
          } finally {
            g.restore();
          }
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener(o -> paint.run());
    paint.run();
    return frame;
  }

  private void loadEvidence(final List<Item> candidates) {
    ++this.evidenceGeneration;
    if (this.evidenceWorker != null) this.evidenceWorker.interrupt();
    final long token = this.evidenceGeneration;
    final var missing =
        candidates.stream()
            .filter(
                i ->
                    !this.summaries.containsKey(i.path()) && !this.readErrors.containsKey(i.path()))
            .limit(PAGE_SIZE)
            .toList();
    if (missing.isEmpty()) return;
    this.evidenceWorker =
        Thread.ofVirtual()
            .name("laboratory-notebook-evidence")
            .start(
                () -> {
                  final Map<Path, LaboratoryNotebookEvidence.Summary> loaded = new HashMap<>();
                  final Map<Path, String> errors = new HashMap<>();
                  for (final Item item : missing) {
                    if (Thread.currentThread().isInterrupted()) return;
                    try {
                      loaded.put(
                          item.path(),
                          LaboratoryNotebookEvidence.read(
                              item.path(), () -> Thread.currentThread().isInterrupted()));
                    } catch (final Exception error) {
                      if (Thread.currentThread().isInterrupted()) return;
                      errors.put(item.path(), message(error));
                    }
                  }
                  Platform.runLater(
                      () -> {
                        if (this.disposed || token != this.evidenceGeneration) return;
                        this.summaries.putAll(loaded);
                        this.readErrors.putAll(errors);
                        this.renderCards(false);
                        this.loadEvidence(this.snapshot.items());
                      });
                });
  }

  ViewState viewState() {
    return new ViewState(
        this.snapshot == null ? null : this.snapshot.project().id(),
        this.sectionId,
        this.selectedItemId,
        this.filter.getValue().name(),
        this.query.getText());
  }

  void restoreView(final ViewState state) {
    if (state == null || this.pending()) return;
    this.deferredView = state;
    if (!this.running && !this.projects.getItems().isEmpty()) {
      final var project =
          this.projects.getItems().stream()
              .filter(p -> p.id().equals(state.projectId()))
              .findFirst()
              .orElse(null);
      if (project != null) this.loadProject(project);
    }
  }

  private void setFilter(final String value, final String query) {
    this.changing = true;
    try {
      this.filter.setValue(Filter.valueOf(value == null ? Filter.ACTIVE.name() : value));
      this.query.setText(Objects.toString(query, ""));
    } catch (final IllegalArgumentException ignored) {
      this.filter.setValue(Filter.ACTIVE);
    } finally {
      this.changing = false;
    }
  }

  @Override
  boolean canClose() {
    if (this.writing) {
      this.status.setText("Дождитесь завершения записи проекта.");
      return false;
    }
    if (this.pending()) {
      this.status.setText("Сохраните изменения формы или отмените ввод перед выходом.");
      return false;
    }
    if (this.running) this.cancelRead();
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    ++this.generation;
    ++this.evidenceGeneration;
    if (this.worker != null && !this.writing) this.worker.interrupt();
    if (this.evidenceWorker != null) this.evidenceWorker.interrupt();
  }

  private static String nullable(final String value) {
    return value == null || value.isEmpty() ? null : value;
  }

  void draft(final Properties data, final boolean restore) {
    if (restore) {
      this.deferredDraft = new Properties();
      this.deferredDraft.putAll(data);
      if (!this.running) this.refreshProjects();
      return;
    }
    if (this.deferredDraft != null) {
      for (final String key : this.deferredDraft.stringPropertyNames())
        if (key.startsWith("notebook.")) data.setProperty(key, this.deferredDraft.getProperty(key));
      return;
    }
    final ViewState view = this.viewState();
    data.setProperty("notebook.source", this.repository.catalogRoot().toString());
    data.setProperty("notebook.project", Objects.toString(view.projectId(), ""));
    data.setProperty("notebook.section", Objects.toString(view.sectionId(), ""));
    data.setProperty("notebook.item", Objects.toString(view.selectedItemId(), ""));
    data.setProperty("notebook.filter", view.filter());
    data.setProperty("notebook.query", view.query());
    data.setProperty("notebook.revision", this.snapshot == null ? "" : this.snapshot.revision());
    data.setProperty("notebook.form", this.form.name());
    data.setProperty("notebook.projectName", this.projectName.getText());
    data.setProperty("notebook.sectionName", this.sectionName.getText());
    data.setProperty("notebook.experimentName", this.experimentName.getText());
    data.setProperty(
        "notebook.template", Objects.toString(this.template.getValue(), "Пустой опыт"));
    data.setProperty(
        "notebook.phase", this.phase.getValue() == null ? "" : this.phase.getValue().name());
    data.setProperty("notebook.waiting", this.waitingFor.getText());
    data.setProperty("notebook.next", this.nextAction.getText());
    data.setProperty(
        "notebook.itemSection",
        this.itemSection.getValue() == null
            ? ""
            : Objects.toString(this.itemSection.getValue().id(), ""));
    data.setProperty("notebook.conflict", Boolean.toString(this.conflict));
  }

  private void restoreRawDraft() {
    final Properties data = this.deferredDraft;
    this.deferredDraft = null;
    this.changing = true;
    try {
      this.form = Form.valueOf(data.getProperty("notebook.form", "NONE"));
      this.projectName.setText(data.getProperty("notebook.projectName", ""));
      this.sectionName.setText(data.getProperty("notebook.sectionName", ""));
      this.experimentName.setText(data.getProperty("notebook.experimentName", ""));
      this.waitingFor.setText(data.getProperty("notebook.waiting", ""));
      this.nextAction.setText(data.getProperty("notebook.next", ""));
      final String phase = data.getProperty("notebook.phase", "");
      this.phase.setValue(phase.isEmpty() ? null : Phase.valueOf(phase));
      final String selectedTemplate = data.getProperty("notebook.template", "Пустой опыт");
      this.template.setValue(selectedTemplate);
      this.editing =
          this.snapshot == null
              ? null
              : this.snapshot.items().stream()
                  .filter(i -> i.id().equals(data.getProperty("notebook.item")))
                  .findFirst()
                  .orElse(null);
      this.fillItemSections(
          data.containsKey("notebook.itemSection")
              ? nullable(data.getProperty("notebook.itemSection"))
              : this.editing == null ? null : this.editing.sectionId());
      this.editHeading.setText(
          "Восстановленный статус · "
              + (this.editing == null ? "опыт недоступен" : this.title(this.editing)));
      this.conflict =
          Boolean.parseBoolean(data.getProperty("notebook.conflict"))
              || !this.repository
                  .catalogRoot()
                  .toString()
                  .equals(data.getProperty("notebook.source"))
              || this.form != Form.PROJECT
                  && (this.snapshot == null
                      || !this.snapshot.revision().equals(data.getProperty("notebook.revision"))
                      || this.form == Form.WORKFLOW && this.editing == null);
    } catch (final RuntimeException error) {
      this.conflict = true;
      this.status.setText("Черновик требует проверки: " + message(error));
    } finally {
      this.changing = false;
    }
    this.setFilter(data.getProperty("notebook.filter"), data.getProperty("notebook.query"));
    this.updateControls();
    this.renderCards(true);
    this.status.setText(
        this.conflict
            ? "Черновик сохранён для просмотра: проект изменился или недоступен. Отмените ввод и обновите проект перед записью."
            : "Восстановлен незаписанный ввод. Проверьте поля перед сохранением.");
  }
}
