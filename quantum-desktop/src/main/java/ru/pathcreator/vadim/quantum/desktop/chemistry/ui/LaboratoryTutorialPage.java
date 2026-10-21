/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Duration;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** An isolated, resumable course using the same workbench and handlers as ordinary work. */
final class LaboratoryTutorialPage extends BorderPane {
  private record Step(
      String chapter,
      String title,
      String text,
      Supplier<Node> target,
      BooleanSupplier ready,
      Runnable prepare,
      String example) {}

  private final Window owner;
  private final LaboratoryViewPreferences preferences;
  private final LaboratoryTutorialStore store;
  private final Runnable close;
  private final StackPane stage = new StackPane();
  private final LaboratoryTutorialOverlay spotlight = new LaboratoryTutorialOverlay();
  private final VBox coach = new VBox(14);
  private final ScrollPane coachScroll = new ScrollPane(coach);
  private final VBox coachActions = new VBox(10);
  private final BorderPane coachPanel = new BorderPane();
  private final Label chapter = label("", "tutorial-eyebrow"),
      title = label("", "tutorial-step-title"),
      explanation = label("", "tutorial-description"),
      state = label("", "tutorial-state"),
      error = label("", "tutorial-error");
  private final Region progressFill = new Region();
  private final StackPane progress = new StackPane(progressFill);
  private final javafx.beans.property.DoubleProperty progressValue =
      new javafx.beans.property.SimpleDoubleProperty();
  private final Button next = new Button("Далее →"),
      back = new Button("← Назад"),
      example = new Button("Подставить пример");
  private final ToggleButton automatic = new ToggleButton("Автопереход");
  private long readySince, inputAt, advanceGeneration;
  private boolean advanceQueued, advancing;
  private boolean inputEdited, inputCommitted, reviewing;
  private TextInputControl observedInput;
  private final javafx.beans.value.ChangeListener<String> inputListener =
      (o, a, b) -> {
        inputEdited = true;
        inputAt = System.nanoTime();
        reviewing = false;
      };
  private final ToggleButton hints = new ToggleButton("Подсказки включены");
  private final List<Step> steps = new ArrayList<>();
  private final Timeline pulse = new Timeline(new KeyFrame(Duration.millis(200), e -> update()));
  private LaboratoryWorkspace workspace;
  private LaboratoryNotebookRepository.Snapshot snapshot;
  private int index;
  private boolean guide, ended, librarySaved, targetFallback;
  private String guideId;
  private LaboratoryTutorialPractice.State practiceBefore;
  private LaboratoryTutorialActions.Item referenceItem;
  private Properties pendingInput = new Properties();
  private long prepareAt;
  private Node lastTarget;
  private static final List<String> INPUTS =
      List.of(
          "notebook-project-name",
          "notebook-section-name",
          "notebook-experiment-name",
          "notebook-template",
          "desk-observation",
          "chemistry-library-name",
          "chemistry-library-summary",
          "chemistry-library-tags",
          "chemistry-library-references");
  private int guideStep;
  private LaboratoryTutorialLessons.Guide guideContent;

  LaboratoryTutorialPage(Window owner, LaboratoryViewPreferences preferences, Runnable close)
      throws java.io.IOException {
    this(owner, preferences, new LaboratoryTutorialStore(), close);
  }

  LaboratoryTutorialPage(
      Window owner,
      LaboratoryViewPreferences preferences,
      LaboratoryTutorialStore store,
      Runnable close) {
    this.owner = owner;
    this.preferences = preferences;
    this.store = store;
    this.close = close;
    setId("chemistry-tutorial");
    getStyleClass().add("tutorial-root");
    setMinSize(0, 0);
    pulse.setCycleCount(Timeline.INDEFINITE);
    hints.setId("tutorial-hints");
    hints.setSelected(preferences.flag("tutorial.hints", true));
    hints.setText(hints.isSelected() ? "Подсказки включены" : "Подсказки скрыты");
    hints.setOnAction(
        e -> {
          hints.setText(hints.isSelected() ? "Подсказки включены" : "Подсказки скрыты");
          preferences.set("tutorial.hints", hints.isSelected());
          preferences.save();
          arrange();
          update();
        });
    automatic.setId("tutorial-automatic");
    automatic.setSelected(preferences.flag("tutorial.automatic", true));
    automatic.setOnAction(
        e -> {
          preferences.set("tutorial.automatic", automatic.isSelected());
          preferences.save();
          resetAdvance();
          update();
        });
    addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        e -> {
          if (e.getCode() == KeyCode.ENTER
              && observedInput instanceof TextField
              && observedInput.isFocused()) {
            inputCommitted = true;
            e.consume();
            update();
          }
        });
    next.setId("tutorial-next");
    next.getStyleClass().add("laboratory-primary-action");
    next.setMaxWidth(Double.MAX_VALUE);
    next.setOnAction(e -> advance());
    back.setId("tutorial-back");
    back.setOnAction(e -> previous());
    example.setId("tutorial-example");
    example.setOnAction(
        e -> {
          Node target = currentTarget();
          if (target instanceof TextInputControl text && !guide) {
            text.setText(steps.get(index).example());
            text.requestFocus();
            update();
          }
        });
    progress.setMaxWidth(Double.MAX_VALUE);
    progress.setMinHeight(6);
    progress.setPrefHeight(6);
    progress.setMaxHeight(6);
    progress.setAlignment(Pos.CENTER_LEFT);
    progress.getStyleClass().add("tutorial-progress-track");
    progressFill.getStyleClass().add("tutorial-progress-fill");
    progressFill.setMinWidth(0);
    progressFill.maxWidthProperty().bind(progress.widthProperty().multiply(progressValue));
    progress.setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
    progress.setId("tutorial-progress");
    error.visibleProperty().bind(error.textProperty().isNotEmpty());
    error.managedProperty().bind(error.visibleProperty());
    Button locate = new Button("Показать нужное место");
    locate.setId("tutorial-locate");
    locate.setOnAction(
        e -> {
          if (!guide) steps.get(index).prepare().run();
          reveal(currentTarget());
          update();
        });
    coach.getChildren().setAll(chapter, progress, title, explanation, error);
    coachActions
        .getChildren()
        .setAll(
            state,
            example,
            next,
            new FlowPane(8, 8, back, locate),
            button("Все функции экрана", "screen-actions", this::screenActions));
    coachActions.setPadding(new Insets(18, 22, 22, 22));
    coachActions.getStyleClass().add("tutorial-coach-actions");
    coachPanel.getStyleClass().add("tutorial-coach-panel");
    coachPanel.setMinSize(0, 0);
    coachScroll.setFitToWidth(true);
    coachScroll.setMinSize(0, 0);
    coachScroll.getStyleClass().add("tutorial-coach-scroll");
    coachPanel.setCenter(coachScroll);
    coach.setPadding(new Insets(22));
    coach.getStyleClass().add("tutorial-coach");
    coach.setMinWidth(0);
    widthProperty().addListener((o, a, b) -> arrange());
    landing();
  }

  private static Label label(String text, String style) {
    Label l = new Label(text);
    l.setWrapText(true);
    l.getStyleClass().add(style);
    l.setMinHeight(Region.USE_PREF_SIZE);
    return l;
  }

  private Button button(String text, String id, Runnable action) {
    Button b = new Button(text);
    b.setId("tutorial-" + id);
    b.setOnAction(e -> attempt(action));
    return b;
  }

  private void attempt(Runnable action) {
    try {
      error.setText("");
      action.run();
    } catch (Exception e) {
      error.setText(e.getMessage() == null ? "Не удалось выполнить действие." : e.getMessage());
    }
  }

  private void landing() {
    pulse.stop();
    resetAdvance();
    setTop(null);
    setRight(null);
    setBottom(null);
    Label eyebrow = label("QUANTUM IR STUDIO · ОБУЧЕНИЕ", "tutorial-eyebrow");
    Label heading = label("Освойтесь в Studio.\nОдин шаг за раз.", "tutorial-hero-title");
    Label intro =
        label(
            "Стрелки покажут, куда нажать. Вы назовёте свою серию, создадите три учебных опыта и сохраните первые результаты.",
            "tutorial-intro");
    HBox stats =
        new HBox(
            12, pill("01  Создать серию"), pill("02  Освоить опыт"), pill("03  Изучить разделы"));
    stats.setAlignment(Pos.CENTER_LEFT);
    Button start =
        button(
            store.exists()
                ? (store.completed() ? "Повторить знакомые разделы" : "Продолжить обучение →")
                : "Начать обучение →",
            "start",
            () -> start(false));
    start.getStyleClass().add("laboratory-primary-action");
    Button restart = button("Начать заново…", "restart", () -> confirmCleanup(true));
    restart.setVisible(store.exists());
    restart.setManaged(store.exists());
    Button remove = button("Удалить учебные данные…", "delete", () -> confirmCleanup(false));
    remove.setVisible(store.exists());
    remove.setManaged(store.exists());
    CheckBox offer = new CheckBox("Предлагать обучение на начальном экране");
    offer.setSelected(!preferences.flag("tutorial.offer.dismissed", false));
    offer.setOnAction(
        e -> {
          preferences.set("tutorial.offer.dismissed", !offer.isSelected());
          preferences.save();
        });
    VBox card =
        new VBox(
            20,
            eyebrow,
            heading,
            intro,
            stats,
            label("Настоящие формы, отдельная учебная папка", "tutorial-section-title"),
            label(
                "Вы работаете с обычными проектами и опытами в папке Training. Ваш текущий опыт остаётся на своём месте. В конце учебные файлы можно оставить или удалить.",
                "tutorial-description"),
            new FlowPane(
                10,
                10,
                start,
                button("Углублённое обучение →", "advanced-start", this::advancedStart),
                restart,
                remove),
            label(
                "Основы — серия и три своих опыта. Углублённое — рисование, все разделы и команды; недостающие учебные опыты подготовятся автоматически.",
                "tutorial-description"),
            offer,
            button("← Вернуться к работе", "exit", this::exit),
            error);
    card.setMaxWidth(880);
    card.setPadding(new Insets(36));
    card.getStyleClass().add("tutorial-welcome-card");
    StackPane aligned = new StackPane(card);
    aligned.setPadding(new Insets(28));
    aligned.setAlignment(Pos.TOP_CENTER);
    ScrollPane scroll = new ScrollPane(aligned);
    scroll.setFitToWidth(true);
    scroll.setId("tutorial-home");
    setCenter(scroll);
  }

  private static Label pill(String text) {
    return label(text, "tutorial-pill");
  }

  private void start(boolean fresh) {
    attempt(
        () -> {
          try {
            Path run = store.start();
            workspace = new LaboratoryWorkspace(run);
            Node view = workspace.content(owner);
            LaboratoryNotebookRepository repository =
                new LaboratoryNotebookRepository(run.resolve("Projects"));
            var projects = repository.projects();
            snapshot = projects.isEmpty() ? null : repository.load(projects.getFirst());
            pendingInput = store.draft();
            librarySaved = savedLibraryEntry(run);
            workspace.tutorialProjects(snapshot);
            buildSteps();
            index = fresh ? 0 : Math.min(store.step(), steps.size() - 1);
            // Names typed into an unfinished form are not yet an experiment: resume at its form.
            if (count() < 3)
              index =
                  Math.min(
                      index,
                      snapshot == null ? 2 : snapshot.sections().isEmpty() ? 5 : 9 + count() * 4);
            stage.getChildren().setAll(view, spotlight);
            stage.setMinSize(0, 0);
            setCenter(stage);
            setTop(toolbar());
            arrange();
            if (index >= 18 && snapshot != null && !snapshot.items().isEmpty())
              workspace.tutorialOpen(snapshot.items().getFirst().path());
            guide = false;
            ended = false;
            String resumeLesson = pendingInput.getProperty("_lesson", "");
            String resumeStep = pendingInput.getProperty("_lessonStep", "0");
            boolean advanced = Boolean.parseBoolean(pendingInput.getProperty("_advanced", "false"));
            pendingInput.remove("_lesson");
            pendingInput.remove("_lessonStep");
            pendingInput.remove("_advanced");
            if (advanced && lessonCommands().stream().anyMatch(c -> c.id().equals(resumeLesson))) {
              guide(resumeLesson);
              if (!practice()) {
                try {
                  guideStep =
                      Math.max(
                          0,
                          Math.min(guideContent.texts().length - 1, Integer.parseInt(resumeStep)));
                } catch (NumberFormatException ignored) {
                  guideStep = 0;
                }
                showGuideStep();
              }
            } else if (advanced || store.completed()) catalog();
            else enter();
            pulse.play();
          } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
          }
        });
  }

  private static boolean savedLibraryEntry(Path run) throws java.io.IOException {
    Path library = run.resolve("Library/.chemistry-library");
    if (!Files.isDirectory(library)) return false;
    // A failed save can leave the directory behind. Only a committed entry completes the lesson.
    try (var files = Files.list(library)) {
      return files.anyMatch(
          p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".qchem"));
    }
  }

  private Node toolbar() {
    Label badge = pill("УЧЕБНЫЙ РЕЖИМ");
    Button contents = button("Углублённое", "catalog", this::catalog);
    Button pause = button("Пауза и выход", "pause", this::exit);
    Button restart = button("Заново…", "restart", () -> confirmCleanup(true));
    FlowPane bar = new FlowPane(12, 8, badge, hints, automatic, contents, restart, pause);
    bar.getStyleClass().add("tutorial-toolbar");
    bar.setPadding(new Insets(10, 16, 10, 16));
    return bar;
  }

  private void arrange() {
    if (getCenter() != stage) return;
    if (!hints.isSelected()) {
      setRight(null);
      setBottom(null);
      return;
    }
    setRight(null);
    setBottom(null);
    if (!coach.getChildren().contains(error)) coach.getChildren().add(error);
    coachPanel.setRight(null);
    coachPanel.setBottom(null);
    if (getWidth() < 1100 && getWidth() > 0) {
      coachPanel.setPrefHeight(245);
      coachPanel.setMinWidth(0);
      coachActions.setPrefWidth(310);
      coachActions.setMinWidth(290);
      coachPanel.setRight(coachActions);
      setBottom(coachPanel);
    } else {
      coachPanel.setPrefWidth(340);
      coachPanel.setMinWidth(300);
      coachActions.setMinWidth(0);
      coachPanel.setBottom(coachActions);
      setRight(coachPanel);
    }
  }

  private void add(
      String chapter,
      String title,
      String text,
      String selector,
      BooleanSupplier ready,
      Runnable prepare,
      String example) {
    steps.add(new Step(chapter, title, text, () -> find(selector), ready, prepare, example));
  }

  private boolean project() {
    return snapshot != null;
  }

  private int count() {
    return snapshot == null ? 0 : snapshot.items().size();
  }

  private boolean section() {
    return snapshot != null && !snapshot.sections().isEmpty();
  }

  private void notebook() {
    if (workspace.tutorialNotebook() == null) workspace.tutorialProjects(snapshot);
  }

  private void form(String id, String field) {
    notebook();
    if (find(field) == null) fire(id);
  }

  private boolean filled(String id) {
    return find(id) instanceof TextInputControl t && !t.getText().isBlank();
  }

  private boolean visible(String id) {
    return find(id) != null;
  }

  private boolean planned() {
    return find("#notebook-template") instanceof ComboBox<?> c
        && "Из текущего плана".equals(c.getValue());
  }

  private void buildSteps() {
    steps.clear();
    add(
        "1 · ПРОЕКТ И СЕРИЯ",
        "Начните с проекта",
        "Нажмите «Новый проект». Проект объединяет работу над одной задачей; внутри будут серии и отдельные опыты.",
        "#notebook-new-project",
        () -> project() || visible("#notebook-project-name"),
        this::notebook,
        null);
    add(
        "1 · ПРОЕКТ И СЕРИЯ",
        "Назовите проект",
        "Напишите своё название. Например, «Знакомство со Studio». Это организационная подпись, её не нужно делать химическим названием.",
        "#notebook-project-name",
        () -> project() || filled("#notebook-project-name"),
        () -> {
          if (!project()) form("#notebook-new-project", "#notebook-project-name");
        },
        "Знакомство со Studio");
    add(
        "1 · ПРОЕКТ И СЕРИЯ",
        "Сохраните проект",
        "Нажмите «Создать проект». Дождитесь появления разделов: проект действительно записывается на диск.",
        "#notebook-create-project",
        this::project,
        () -> {
          if (!project()) form("#notebook-new-project", "#notebook-project-name");
        },
        null);
    add(
        "1 · ПРОЕКТ И СЕРИЯ",
        "Создайте свою серию",
        "Нажмите «+ Раздел». Раздел — это папка внутри проекта. Здесь соберём три варианта опыта в одну серию.",
        "#notebook-new-section",
        () -> section() || visible("#notebook-section-name"),
        this::notebook,
        null);
    add(
        "1 · ПРОЕКТ И СЕРИЯ",
        "Дайте серии понятное имя",
        "Напишите, что объединяет эти опыты. Например, «Серия A · первые опыты».",
        "#notebook-section-name",
        () -> section() || filled("#notebook-section-name"),
        () -> {
          if (!section()) form("#notebook-new-section", "#notebook-section-name");
        },
        "Серия A · первые опыты");
    add(
        "1 · ПРОЕКТ И СЕРИЯ",
        "Подтвердите создание серии",
        "Нажмите «Создать». Новый раздел выберется слева. Следующие опыты попадут именно в него.",
        "#notebook-create-section",
        this::section,
        () -> {
          if (!section()) form("#notebook-new-section", "#notebook-section-name");
        },
        null);
    for (int n = 1; n <= 3; n++) {
      final int number = n;
      String group = "2 · ТРИ ОПЫТА · " + n + " / 3";
      add(
          group,
          "Добавьте опыт №" + n,
          "Нажмите «Новый опыт». У каждого варианта будет отдельный файл, свой журнал и результаты.",
          "#notebook-new-experiment",
          () -> count() >= number || visible("#notebook-experiment-name"),
          this::notebook,
          null);
      add(
          group,
          "Назовите опыт №" + n,
          "Введите своё имя. Номер или короткое отличие помогают быстро найти нужный вариант в серии.",
          "#notebook-experiment-name",
          () -> count() >= number || filled("#notebook-experiment-name"),
          () -> {
            if (count() < number) form("#notebook-new-experiment", "#notebook-experiment-name");
          },
          "Опыт " + n + " · учебный");
      add(
          group,
          "Начните с учебного плана",
          "В списке «Начать» выберите «Из текущего плана». Для обучения подготовлена условная схема с двумя веществами. Это пример интерфейса, а не инструкция по синтезу.",
          "#notebook-template",
          () -> count() >= number || planned(),
          () -> {
            if (count() < number) form("#notebook-new-experiment", "#notebook-experiment-name");
          },
          null);
      add(
          group,
          "Создайте опыт №" + n,
          "Нажмите «Создать опыт». Копируется только план: измерения и выполненные действия не появляются сами собой.",
          "#notebook-create-experiment",
          () -> count() >= number,
          () -> {
            if (count() < number) form("#notebook-new-experiment", "#notebook-experiment-name");
          },
          null);
    }
    steps.add(
        new Step(
            "3 · РАБОТА С ОПЫТОМ",
            "Откройте один из трёх опытов",
            "Нажмите «Открыть опыт» на первой карточке. Два остальных останутся в серии: вы сможете вернуться к ним в любой момент.",
            () ->
                snapshot == null || snapshot.items().isEmpty()
                    ? null
                    : find("#notebook-open-" + snapshot.items().getFirst().id()),
            () -> visible("#experiment-desk"),
            this::notebook,
            null));
    add(
        "3 · РАБОТА С ОПЫТОМ",
        "Пять вкладок одного опыта",
        "Реакция — схема. Загрузка — количества. Методика — шаги. Наблюдения — журнал. Результат — фактические данные. План и факт вводятся отдельно.",
        ".laboratory-desk-navigation",
        () -> true,
        () -> route("desk"),
        null);
    add(
        "3 · РАБОТА С ОПЫТОМ",
        "Перейдите к наблюдениям",
        "Нажмите «Наблюдения». Здесь записывают, что произошло в опыте, и связывают запись с шагом методики.",
        "#desk-jump-3",
        () -> visible("#desk-observation"),
        () -> route("desk"),
        null);
    add(
        "3 · РАБОТА С ОПЫТОМ",
        "Введите учебную запись",
        "Напишите короткую заметку. Явно пометьте её как учебную: данные этого проекта не являются лабораторными измерениями.",
        "#desk-observation",
        () -> hasNote() || filled("#desk-observation"),
        () -> {
          if (!visible("#desk-observation")) {
            route("desk");
            fire("#desk-jump-3");
          }
        },
        "Учебная запись: изучаю журнал, эксперимент не проводился.");
    add(
        "3 · РАБОТА С ОПЫТОМ",
        "Запишите наблюдение в журнал",
        "Нажмите «Записать наблюдение». Просто набрать текст недостаточно: запись должна появиться в журнале.",
        "#desk-record-note",
        this::hasNote,
        () -> {
          if (!visible("#desk-observation")) {
            route("desk");
            fire("#desk-jump-3");
          }
        },
        null);
    add(
        "3 · РАБОТА С ОПЫТОМ",
        "Сохраните опыт",
        "Нажмите «Сохранить». Журнал и схема сохраняются в одном файле QCHEM. При переходе дальше обучение также проверит сохранение.",
        "#desk-save",
        () -> !workspace.tutorialEditor().dirty(),
        () -> {},
        null);
    add(
        "4 · СВОЯ БИБЛИОТЕКА",
        "Откройте добавление вещества",
        "Перейдите на вкладку «Добавить своё вещество». Библиотечная запись сохраняет структуру, имя и синонимы для следующих опытов.",
        "#chemistry-library-tabs",
        () ->
            find("#chemistry-library-tabs") instanceof TabPane p
                && p.getSelectionModel().getSelectedIndex() == 1,
        () -> route("library"),
        null);
    add(
        "4 · СВОЯ БИБЛИОТЕКА",
        "Выберите вещество и продолжите",
        "Проверьте структуру в предпросмотре. Кнопка снизу откроет форму названия. В учебном режиме даже «Моя библиотека» хранится отдельно от вашей рабочей библиотеки.",
        "#chemistry-library-save-structure",
        () -> visible("#chemistry-library-name"),
        () -> {
          if (!visible("#chemistry-library-tabs") && !visible("#chemistry-library-name"))
            route("library");
          if (find("#chemistry-library-tabs") instanceof TabPane p) p.getSelectionModel().select(1);
        },
        null);
    add(
        "4 · СВОЯ БИБЛИОТЕКА",
        "Назовите библиотечную запись",
        "Дайте веществу имя, которое легко найти. В полях ниже можно добавить синонимы, описание и источник.",
        "#chemistry-library-name",
        () -> librarySaved || filled("#chemistry-library-name"),
        this::libraryForm,
        "Моё учебное вещество");
    add(
        "4 · СВОЯ БИБЛИОТЕКА",
        "Сохраните вещество",
        "Нажмите «Сохранить». Новая запись появится в списке. В обычной работе вы сможете вставлять её в другие опыты.",
        "#chemistry-library-entry-save",
        () -> librarySaved,
        this::libraryForm,
        null);
    add(
        "5 · ОТЧЁТ И ВОЗВРАТ",
        "Посмотрите отчёт",
        "Здесь собираются схема, план и сохранённые записи. «Обновить отчёт» перестроит предпросмотр после изменений; PDF и HTML сохраняются отдельными кнопками.",
        "#chemistry-report-generate",
        () -> true,
        () -> route("report"),
        null);
    add(
        "5 · ОТЧЁТ И ВОЗВРАТ",
        "Вернитесь к своей серии",
        "Нажмите «К серии опытов». Все три опыта остаются в проекте, а журнал сохранён внутри первого. Вы уже прошли полный круг: серия → опыт → запись → библиотека → отчёт.",
        "#chemistry-projects-open",
        () -> workspace.tutorialNotebook() != null,
        () -> {},
        null);
    add(
        "5 · ОТЧЁТ И ВОЗВРАТ",
        "Вы готовы продолжать самостоятельно",
        "Основной маршрут завершён. Дальше доступны 45 углублённых тем: практика рисования, работа с разделами и пояснения к командам. Можно продолжить или закончить и удалить учебные файлы.",
        "#notebook-sections",
        () -> true,
        this::notebook,
        null);
  }

  private boolean hasNote() {
    return workspace.tutorialEditor().document().experiment().journal().stream()
        .anyMatch(e -> e.kind() == LaboratoryEntry.Kind.OBSERVATION);
  }

  private void libraryForm() {
    if (librarySaved || visible("#chemistry-library-name")) return;
    if (!visible("#chemistry-library-tabs")) route("library");
    if (find("#chemistry-library-tabs") instanceof TabPane p) {
      p.getSelectionModel().select(1);
      fire("#chemistry-library-save-structure");
    }
  }

  private void route(String id) {
    if (workspace == null) return;
    if (id.equals("desk") && visible("#experiment-desk")) return;
    workspace.tutorialCommand(id);
  }

  private void enter() {
    attempt(
        () -> {
          if (index >= steps.size()) {
            finish();
            return;
          }
          resetAdvance();
          Step step = steps.get(index);
          if (!coach.getChildren().contains(error)) coach.getChildren().add(error);
          step.prepare().run();
          prepareAt = System.nanoTime();
          chapter.setText(step.chapter() + "  ·  ШАГ " + (index + 1) + " / " + steps.size());
          title.setText(step.title());
          explanation.setText(step.text());
          progressValue.set((double) index / steps.size());
          back.setDisable(index == 0);
          example.setVisible(step.example() != null);
          example.setManaged(example.isVisible());
          Platform.runLater(
              () -> {
                reveal(currentTarget());
                update();
              });
        });
  }

  private Node currentTarget() {
    return workspace == null
        ? null
        : referenceItem != null
            ? referenceItem.target()
            : guide ? guideTarget() : index < steps.size() ? steps.get(index).target().get() : null;
  }

  private Node guideTarget() {
    targetFallback = false;
    Node target = find(guideContent.targets()[guideStep]);
    if (target != null) return target;
    Parent root = (Parent) workspace.tutorialView();
    // Some controls belong to a collapsed panel or another tab. Open that view, never run its
    // action.
    for (Node node : new ArrayList<>(root.lookupAll(".tab-pane"))) {
      if (node instanceof TabPane tabs)
        for (Tab tab : tabs.getTabs()) {
          if (tab.getContent() instanceof Parent content
              && content.lookup(guideContent.targets()[guideStep]) != null) {
            tabs.getSelectionModel().select(tab);
            break;
          }
        }
    }
    for (Node node : new ArrayList<>(root.lookupAll(".titled-pane")))
      if (node instanceof TitledPane pane
          && pane.getContent() instanceof Parent content
          && content.lookup(guideContent.targets()[guideStep]) != null) pane.setExpanded(true);
    root.applyCss();
    root.layout();
    target = find(guideContent.targets()[guideStep]);
    if (target != null) return target;
    targetFallback = true;
    return root instanceof BorderPane pane ? pane.getCenter() : root;
  }

  private void update() {
    if (workspace == null || ended || getCenter() != stage) return;
    var notebook = workspace.tutorialNotebook();
    if (notebook != null && notebook.tutorialSnapshot() != null)
      snapshot = notebook.tutorialSnapshot();
    for (String key : new ArrayList<>(pendingInput.stringPropertyNames())) {
      if (find("#" + key) instanceof TextInputControl text) {
        text.setText(pendingInput.getProperty(key));
        pendingInput.remove(key);
      } else if (find("#" + key) instanceof ComboBox<?> combo) {
        String saved = pendingInput.getProperty(key);
        for (int i = 0; i < combo.getItems().size(); i++)
          if (Objects.toString(combo.getItems().get(i), "").equals(saved))
            combo.getSelectionModel().select(i);
        pendingInput.remove(key);
      }
    }
    Node status = find("#chemistry-library-page .laboratory-page-status");
    if (status instanceof Label l && l.getText().contains("Запись сохранена")) librarySaved = true;
    boolean ready =
        referenceItem != null
            || (guide
                ? !practice()
                    || LaboratoryTutorialPractice.ready(
                        guideStep, practiceBefore, workspace.tutorialDrawingState())
                : steps.get(index).ready().getAsBoolean());
    next.setDisable(!ready);
    next.setText(
        referenceItem != null
            ? "← К уроку"
            : guide
                ? guideStep == guideContent.texts().length - 1
                    ? "Завершить урок ✓"
                    : ready ? "Далее →" : "Выполните задание на экране"
                : index == steps.size() - 1
                    ? "Завершить маршрут ✓"
                    : ready ? "Готово — дальше →" : "Выполните шаг на экране");
    state.setText(ready ? "✓ Можно продолжать" : "Выполните выделенное действие");
    Node target = currentTarget();
    if (referenceItem == null && guide && targetFallback)
      state.setText(
          "Нужный элемент появится после выбора записи или открытия формы. Команды — в «Все функции экрана».");
    else if (readingStep() || guide && !practice())
      state.setText("Прочитайте пояснение и попробуйте действие. Переход — когда будете готовы.");
    if (target != lastTarget) {
      lastTarget = target;
      if (hints.isSelected()) reveal(target);
    }
    spotlight.pointTo(hints.isSelected() ? target : null);
    if (!guide && target instanceof TextInputControl text && observedInput != text) {
      unobserve();
      observedInput = text;
      text.textProperty().addListener(inputListener);
    }
    autoAdvance(ready, target);
    if (target == null && !ready) {
      state.setText("Открываю нужное поле…");
      if (!guide
          && hints.isSelected()
          && workspace.tutorialIdle()
          && System.nanoTime() - prepareAt > 1_000_000_000L) {
        prepareAt = System.nanoTime();
        long generation = advanceGeneration;
        Platform.runLater(
            () -> {
              if (generation == advanceGeneration && !ended && getCenter() == stage && !guide)
                attempt(() -> steps.get(index).prepare().run());
            });
      }
    }
  }

  private void unobserve() {
    if (observedInput != null) observedInput.textProperty().removeListener(inputListener);
    observedInput = null;
  }

  private void resetAdvance() {
    unobserve();
    advanceGeneration++;
    advanceQueued = false;
    readySince = 0;
    inputAt = System.nanoTime();
    inputEdited = false;
    inputCommitted = false;
  }

  private boolean readingStep() {
    return !guide && (index == 19 || index == 28 || index == 30);
  }

  private void autoAdvance(boolean ready, Node target) {
    if (referenceItem != null
        || (guide && (!practice() || LaboratoryTutorialPractice.reading(guideStep)))
        || readingStep()
        || reviewing
        || !automatic.isSelected()
        || !hints.isSelected()) {
      readySince = 0;
      return;
    }
    if (!ready || !workspace.tutorialIdle() || !error.getText().isEmpty()) {
      readySince = 0;
      return;
    }
    long now = System.nanoTime();
    if (target instanceof TextInputControl
        && !(inputCommitted || inputEdited && now - inputAt >= 1_500_000_000L)) {
      readySince = 0;
      return;
    }
    if (readySince == 0) readySince = now;
    state.setText("✓ Выполнено · перехожу дальше…");
    if (now - readySince >= 350_000_000L && !advanceQueued && !advancing) {
      advanceQueued = true;
      long generation = advanceGeneration;
      // File operations start a nested FX loop: never start them inside a Timeline/layout pulse.
      Platform.runLater(
          () -> {
            if (generation != advanceGeneration) return;
            advanceQueued = false;
            if (ended
                || getCenter() != stage
                || !automatic.isSelected()
                || !hints.isSelected()
                || reviewing
                || referenceItem != null
                || !workspace.tutorialIdle()) return;
            boolean stillReady =
                guide
                    ? practice()
                        && LaboratoryTutorialPractice.ready(
                            guideStep, practiceBefore, workspace.tutorialDrawingState())
                    : steps.get(index).ready().getAsBoolean();
            if (stillReady) advance();
          });
    }
  }

  private void advance() {
    if (advancing) return;
    advancing = true;
    try {
      attempt(
          () -> {
            if (next.isDisabled()) return;
            if (referenceItem != null) {
              referenceItem = null;
              if (guide) describeGuide();
              else enter();
              return;
            }
            if (guide) {
              if (practice() && !workspace.tutorialSave()) {
                error.setText(
                    "Не удалось сохранить учебный рисунок. Проверьте сообщение и повторите переход.");
                return;
              }
              if (guideStep == guideContent.texts().length - 1) {
                try {
                  store.lessonCompleted(guideId);
                } catch (java.io.IOException e) {
                  throw new java.io.UncheckedIOException(e);
                }
                catalog();
                return;
              }
              guideStep++;
              showGuideStep();
              return;
            }
            if (!workspace.tutorialSave()) {
              error.setText("Дождитесь сохранения опыта.");
              return;
            }
            reviewing = false;
            index++;
            try {
              store.progress(index, index >= steps.size());
              store.draft(new Properties());
              pendingInput.clear();
            } catch (java.io.IOException e) {
              index--;
              throw new java.io.UncheckedIOException(e);
            }
            if (index >= steps.size()) finish();
            else enter();
          });
    } finally {
      advancing = false;
    }
  }

  private void previous() {
    if (guide) {
      guideStep = Math.max(0, guideStep - 1);
      showGuideStep();
      reviewing = true;
    } else if (index > 0) {
      reviewing = true;
      index--;
      enter();
    }
  }

  private void advancedStart() {
    attempt(
        () -> {
          try {
            Path run = store.start();
            var repo = new LaboratoryNotebookRepository(run.resolve("Projects"));
            var projects = repo.projects();
            var project =
                projects.isEmpty()
                    ? repo.createProject("Углублённое обучение")
                    : projects.getFirst();
            var data = repo.load(project);
            if (data.sections().isEmpty()) data = repo.addSection(data, null, "Учебная серия");
            while (data.items().size() < 3)
              data =
                  repo.addExperiment(
                      data,
                      data.sections().getFirst().id(),
                      "Практика " + (data.items().size() + 1),
                      examplePlan());
            if (store.step() < 18) store.progress(18, false);
            Properties mode = new Properties();
            mode.setProperty("_advanced", "true");
            store.draft(mode);
            start(false);
            catalog();
          } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
          }
        });
  }

  private void screenActions() {
    if (workspace == null) return;
    // Read controls without running any action; unfinished forms stay mounted in stage.
    unobserve();
    var actions =
        LaboratoryTutorialActions.collect(
            (Parent) workspace.tutorialView(), workspace.tutorialContextMenus());
    TextField search = new TextField();
    search.setId("tutorial-action-search");
    search.setPromptText("Найти команду: сохранить, заряд, импорт…");
    VBox rows = new VBox(10);
    Runnable render =
        () -> {
          rows.getChildren().clear();
          String query = search.getText().strip().toLowerCase(Locale.ROOT).replace('ё', 'е');
          int number = 0;
          for (var action : actions) {
            if (!(action.path() + " " + action.explanation())
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .contains(query)) continue;
            Button show =
                button(
                    "Показать, где находится →",
                    "action-" + number++,
                    () -> {
                      referenceItem = action;
                      action.reveal().run();
                      setCenter(stage);
                      arrange();
                      chapter.setText("УГЛУБЛЁННОЕ · КОМАНДА ЭКРАНА");
                      title.setText(action.path());
                      explanation.setText(
                          action.explanation()
                              + (action.disabled()
                                  ? " Сейчас команда недоступна: проверьте выбранную запись, обязательные поля и выполняющееся действие."
                                  : "")
                              + "\n\nПопробуйте команду в учебном опыте. После открытия новой формы снова выберите «Все функции экрана» для её команд.");
                      example.setVisible(false);
                      example.setManaged(false);
                      back.setDisable(true);
                      Platform.runLater(
                          () -> {
                            reveal(action.target());
                            update();
                          });
                    });
            VBox row =
                new VBox(
                    8,
                    label(action.path(), "tutorial-section-title"),
                    label(action.explanation(), "tutorial-description"),
                    show);
            row.getStyleClass().add("tutorial-guide-card");
            rows.getChildren().add(row);
          }
          if (rows.getChildren().isEmpty())
            rows.getChildren()
                .add(label("Нет совпадений. Попробуйте другое слово.", "tutorial-description"));
        };
    search.textProperty().addListener(o -> render.run());
    render.run();
    VBox body =
        new VBox(
            18,
            label("Все функции текущего экрана", "tutorial-hero-title"),
            label(
                actions.size()
                    + " команд и групп. Здесь также перечислены вкладки, раскрывающиеся блоки и вложенные меню. Команды открывают настоящие формы; в них список можно открыть снова.",
                "tutorial-description"),
            search,
            button(
                "← К уроку",
                "actions-back",
                () -> {
                  referenceItem = null;
                  setCenter(stage);
                  arrange();
                  if (guide) describeGuide();
                  else enter();
                }),
            rows);
    body.setPadding(new Insets(24));
    body.setMaxWidth(1000);
    var scroll = new ScrollPane(new StackPane(body));
    scroll.setFitToWidth(true);
    scroll.setId("tutorial-action-catalog");
    spotlight.pointTo(null);
    setRight(null);
    setBottom(null);
    setCenter(scroll);
  }

  private void catalog() {
    if (workspace == null) return;
    referenceItem = null;
    if (count() < 3) {
      error.setText(
          "Сначала создайте три учебных опыта. Затем откроются экскурсии по всем разделам.");
      return;
    }
    if (!workspace.tutorialCanLeave()) {
      error.setText("Сохраните или отмените ввод в текущей форме, затем откройте разделы.");
      return;
    }
    spotlight.pointTo(null);
    setRight(null);
    setBottom(null);
    TextField search = new TextField();
    search.setId("tutorial-guide-search");
    search.setPromptText("Найти раздел: структура, образцы, отчёт…");
    VBox rows = new VBox(10);
    Runnable render =
        () -> {
          rows.getChildren().clear();
          String q = search.getText().strip().toLowerCase(Locale.ROOT).replace('ё', 'е');
          for (var command : lessonCommands()) {
            if (!(command.name() + " " + command.hint() + " " + command.section())
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .contains(q)) continue;
            Button open =
                button(
                    store.lessons().contains(command.id())
                        ? "✓ Пройдено · повторить"
                        : "Открыть экскурсию →",
                    "guide-" + command.id(),
                    () -> guide(command.id()));
            VBox row =
                new VBox(
                    6,
                    label(command.section(), "tutorial-eyebrow"),
                    label(command.name(), "tutorial-section-title"),
                    label(command.hint(), "tutorial-description"),
                    open);
            row.getStyleClass().add("tutorial-guide-card");
            rows.getChildren().add(row);
          }
          if (rows.getChildren().isEmpty())
            rows.getChildren()
                .add(label("Ничего не найдено. Попробуйте другое слово.", "tutorial-description"));
        };
    search.textProperty().addListener(o -> render.run());
    render.run();
    VBox content =
        new VBox(
            18,
            label("Углублённое обучение", "tutorial-hero-title"),
            label(
                "Пройдено разделов: "
                    + store.lessons().size()
                    + " из "
                    + lessonCommands().size()
                    + ". Практика рисования, назначение разделов и разбор каждой доступной команды через «Все функции экрана». Уроки можно повторять в любом порядке.",
                "tutorial-intro"),
            search,
            new FlowPane(
                10,
                8,
                button(
                    "← Основы: серия и опыты",
                    "resume",
                    () -> {
                      guide = false;
                      referenceItem = null;
                      index = Math.min(index, steps.size() - 1);
                      setCenter(stage);
                      arrange();
                      enter();
                    }),
                button("Завершить обучение…", "finish", this::finish)),
            rows,
            error);
    content.setPadding(new Insets(24));
    content.setMaxWidth(1000);
    StackPane aligned = new StackPane(content);
    ScrollPane scroll = new ScrollPane(aligned);
    scroll.setFitToWidth(true);
    scroll.setId("tutorial-catalog");
    setCenter(scroll);
  }

  private void guide(String id) {
    attempt(
        () -> {
          if (workspace.tutorialFile() == null && snapshot != null && !snapshot.items().isEmpty())
            workspace.tutorialOpen(snapshot.items().getFirst().path());
          if (!workspace.tutorialCanLeave()) return;
          guide = true;
          guideId = id;
          guideStep = 0;
          referenceItem = null;
          guideContent =
              LaboratoryTutorialTopics.find(id)
                  .map(LaboratoryTutorialTopics.Topic::guide)
                  .orElseGet(() -> LaboratoryTutorialLessons.forCommand(id));
          if (!id.equals("notebook") && workspace.tutorialNotebook() != null)
            workspace.tutorialOpen(snapshot.items().getFirst().path());
          if (LaboratoryTutorialTopics.find(id).isPresent()) workspace.tutorialDrawing();
          else route(id.equals("repeat") ? "desk" : id);
          if (practice()) workspace.tutorialPrepareDrawingPractice();
          setCenter(stage);
          arrange();
          showGuideStep();
        });
  }

  private boolean practice() {
    return guide && "drawing-practice".equals(guideId);
  }

  private List<LaboratoryCommandPalette.Command> lessonCommands() {
    var commands = new ArrayList<>(LaboratoryTutorialTopics.commands());
    commands.addAll(workspace.tutorialCommands());
    return List.copyOf(commands);
  }

  private void showGuideStep() {
    resetAdvance();
    reviewing = false;
    practiceBefore = workspace.tutorialDrawingState();
    describeGuide();
  }

  private void describeGuide() {
    var command =
        lessonCommands().stream().filter(c -> c.id().equals(guideId)).findFirst().orElseThrow();
    chapter.setText(
        command.section() + " · ШАГ " + (guideStep + 1) + " / " + guideContent.texts().length);
    title.setText(command.name());
    explanation.setText(guideContent.texts()[guideStep]);
    progressValue.set((double) guideStep / guideContent.texts().length);
    example.setVisible(false);
    example.setManaged(false);
    back.setDisable(guideStep == 0);
    Platform.runLater(
        () -> {
          reveal(currentTarget());
          update();
        });
  }

  private void finish() {
    if (workspace != null && !workspace.tutorialCanLeave()) {
      error.setText("Завершите или отмените ввод в форме перед выходом.");
      return;
    }
    spotlight.pointTo(null);
    setRight(null);
    setBottom(null);
    preferences.set("tutorial.offer.dismissed", true);
    preferences.save();
    VBox content =
        new VBox(
            18,
            pill(store.completed() ? "ОСНОВЫ ОСВОЕНЫ" : "ОБУЧЕНИЕ НА ПАУЗЕ"),
            label(
                store.completed()
                    ? "Теперь это ваш рабочий инструмент."
                    : "Можно продолжить в другой раз.",
                "tutorial-hero-title"),
            label(
                store.completed()
                    ? "Вы создали проект, серию и три опыта; прошли запись наблюдения, библиотеку и отчёт. Учебные данные можно оставить для практики или удалить."
                    : "Прогресс сохранён. Вернитесь к основному маршруту, оставьте учебные данные для практики или удалите это прохождение.",
                "tutorial-intro"),
            button("Изучить другие разделы", "catalog", this::catalog),
            button("Оставить данные и вернуться к работе", "keep", this::exit),
            button("Удалить учебные данные…", "delete", () -> confirmCleanup(false)),
            button("Пройти заново…", "restart", () -> confirmCleanup(true)),
            error);
    content.setPadding(new Insets(32));
    content.setMaxWidth(850);
    StackPane aligned = new StackPane(content);
    ScrollPane scroll = new ScrollPane(aligned);
    scroll.setFitToWidth(true);
    scroll.setId("tutorial-complete");
    setCenter(scroll);
  }

  private void confirmCleanup(boolean restart) {
    if (workspace != null && !workspace.tutorialIdle()) {
      error.setText("Дождитесь завершения сохранения или расчёта, затем повторите.");
      return;
    }
    pulse.stop();
    spotlight.pointTo(null);
    setRight(null);
    setBottom(null);
    Label path =
        label(
            store.exists() ? store.directory().toString() : "Учебных файлов пока нет.",
            "tutorial-path");
    Button confirm =
        button(
            restart ? "Удалить учебное и начать заново" : "Удалить только учебные данные",
            "confirm-delete",
            () -> {
              if (workspace != null) {
                if (!workspace.tutorialIdle()) return;
                workspace.tutorialDispose();
                workspace = null;
              }
              try {
                store.delete();
                snapshot = null;
                librarySaved = false;
                if (restart) start(true);
                else {
                  ended = true;
                  close.run();
                }
              } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
              }
            });
    confirm.getStyleClass().add("tutorial-danger");
    VBox content =
        new VBox(
            18,
            label(
                restart ? "Начать с чистого листа?" : "Удалить учебную работу?",
                "tutorial-hero-title"),
            label(
                "Будут удалены только файлы текущего учебного прохождения: проекты, опыты, учебная библиотека и сохранённые результаты. Это действие нельзя отменить.",
                "tutorial-description"),
            path,
            new FlowPane(
                10,
                8,
                confirm,
                button(
                    "Отмена",
                    "cancel-delete",
                    () -> {
                      if (workspace == null) landing();
                      else {
                        setCenter(stage);
                        arrange();
                        pulse.play();
                        if (guide) showGuideStep();
                        else {
                          index = Math.min(index, steps.size() - 1);
                          enter();
                        }
                      }
                    })),
            error);
    content.setPadding(new Insets(32));
    content.setMaxWidth(850);
    setCenter(new StackPane(content));
  }

  void shortcut(javafx.scene.input.KeyEvent event) {
    if (workspace != null && !ended && getCenter() == stage) workspace.shortcut(event);
    else if (event.isShortcutDown()) event.consume();
  }

  boolean closeForApplication() {
    try {
      if (workspace != null) {
        if (!workspace.tutorialIdle()) {
          error.setText("Дождитесь завершения текущего действия.");
          return false;
        }
        if (guide && !workspace.tutorialCanLeave()) {
          error.setText("Сохраните или отмените ввод в открытой форме, затем выйдите.");
          return false;
        }
        if (!workspace.tutorialSave()) return false;
        Properties raw = new Properties();
        if (!guide)
          for (String id : INPUTS)
            if (find("#" + id) instanceof TextInputControl text)
              raw.setProperty(id, text.getText());
            else if (find("#" + id) instanceof ComboBox<?> combo)
              raw.setProperty(id, Objects.toString(combo.getValue(), ""));
        if (guide || getCenter() != null && "tutorial-catalog".equals(getCenter().getId())) {
          raw.setProperty("_advanced", "true");
          if (guide && getCenter() == stage) {
            raw.setProperty("_lesson", guideId);
            raw.setProperty("_lessonStep", Integer.toString(practice() ? 0 : guideStep));
          }
        }
        store.draft(raw);
        store.progress(Math.min(index, steps.size()), store.completed());
        workspace.tutorialDispose();
      }
      pulse.stop();
      unobserve();
      ended = true;
      return true;
    } catch (Exception failure) {
      error.setText("Не удалось сохранить учебное прохождение: " + failure.getMessage());
      return false;
    }
  }

  private void exit() {
    if (closeForApplication()) close.run();
  }

  private Node find(String selector) {
    if (workspace == null || selector == null) return null;
    return ((Parent) workspace.tutorialView())
        .lookupAll(selector).stream().filter(this::onScreen).findFirst().orElse(null);
  }

  private boolean onScreen(Node n) {
    for (Node p = n; p != null; p = p.getParent()) if (!p.isVisible()) return false;
    return n.getScene() != null;
  }

  private void fire(String selector) {
    if (find(selector) instanceof ButtonBase b && !b.isDisabled()) b.fire();
  }

  private void reveal(Node target) {
    if (target == null) return;
    for (Node p = target.getParent(); p != null; p = p.getParent()) {
      if (p instanceof TitledPane t) t.setExpanded(true);
      if (p instanceof ScrollPane scroll && scroll.getContent() != null) {
        var content = scroll.getContent();
        var b = content.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
        double excess =
            content.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight();
        if (excess > 0) scroll.setVvalue(Math.max(0, Math.min(1, (b.getMinY() - 24) / excess)));
      }
    }
  }

  static LaboratoryDrawing examplePlan() {
    var editor = new LaboratoryEditor();
    var parser =
        new ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity
            .OpsinChemicalName();
    String a = editor.add(parser.parse("ethanol").drawing(), ReactionComponentRole.REACTANT);
    String b = editor.add(parser.parse("ethanal").drawing(), ReactionComponentRole.PRODUCT);
    editor.stages(
        List.of(
            new LaboratoryReactionStage(
                "tutorial-stage",
                "Учебная схема",
                LaboratoryReactionStage.Arrow.FORWARD,
                ReactionConditions.EMPTY,
                List.of(
                    new LaboratoryStageParticipant(
                        a, ReactionComponentRole.REACTANT, StoichiometricCoefficient.ONE),
                    new LaboratoryStageParticipant(
                        b, ReactionComponentRole.PRODUCT, StoichiometricCoefficient.ONE)))));
    editor.details(
        "Учебный план",
        ReactionConditions.EMPTY,
        "Условная схема для обучения интерфейсу. Не методика синтеза; опыт не проводился.");
    editor.quantities(
        a,
        new LaboratoryQuantity(new java.math.BigDecimal("1"), LaboratoryQuantityUnit.MILLIMOLE),
        null,
        LaboratoryEvidence.PLANNED);
    return editor.document();
  }
}
