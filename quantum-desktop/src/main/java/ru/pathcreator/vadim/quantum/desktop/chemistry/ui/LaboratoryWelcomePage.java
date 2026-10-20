/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNotebookRepository;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryNotebookRepository.Project;

/** Navigation only: opening the start page never creates or replaces scientific records. */
final class LaboratoryWelcomePage extends LaboratoryComputationPage {
  record Actions(
      Runnable createProject,
      Runnable openExperiment,
      Runnable blankExperiment,
      Runnable projects,
      Consumer<Project> openProject,
      Consumer<Path> openRecent,
      Runnable recover,
      Runnable current) {}

  private final VBox projectList = new VBox(8), recoveryCard = new VBox(8);
  private final Label recoveryText = note("");
  private Thread reader;

  LaboratoryWelcomePage(
      final LaboratoryNotebookRepository repository,
      final List<Path> recent,
      final Actions actions) {
    super("Начало", () -> {});
    this.setId("chemistry-welcome");
    this.setTop(null);
    this.getStyleClass().add("laboratory-welcome");
    final Label eyebrow = text("ЛАБОРАТОРНЫЙ ЖУРНАЛ", "welcome-eyebrow");
    final Label title = text("От идеи — к следующему синтезу", "welcome-title");
    final Label intro =
        text(
            "Соберите опыты в проект: планируйте реакции, записывайте ход работы и возвращайтесь к результатам.",
            "welcome-intro");
    final VBox hero = new VBox(12, eyebrow, title, intro);
    hero.getStyleClass().add("welcome-hero");
    final TilePane choices = new TilePane(14, 14);
    choices.setPrefColumns(3);
    choices.setPrefTileWidth(306);
    choices.setTileAlignment(Pos.TOP_LEFT);
    choices
        .getChildren()
        .addAll(
            card(
                "Создать проект",
                "Серия синтезов, разделы и связанные опыты",
                "create-project",
                actions.createProject(),
                true),
            card(
                "Открыть опыт",
                "Продолжить работу с сохранённым файлом",
                "open-experiment",
                actions.openExperiment(),
                false),
            card(
                "Быстрый опыт",
                "Начать с реакции без создания проекта",
                "blank-experiment",
                actions.blankExperiment(),
                false));
    final Label hint =
        text(
            "Начинаете впервые? Создайте проект — например, «Весенние синтезы». Затем добавьте первый опыт.",
            "welcome-hint");
    this.recoveryCard.getStyleClass().add("welcome-recovery");
    this.recoveryCard
        .getChildren()
        .setAll(
            text("Есть незавершённая работа", "welcome-section-title"),
            this.recoveryText,
            action("Посмотреть черновики", "recovery", actions.recover()));
    this.pendingDrafts(0);
    final VBox projects = section("Ваши проекты", this.projectList);
    this.projectList.getChildren().setAll(text("Читаю проекты…", "welcome-hint"));
    projects.getChildren().add(action("Все проекты и опыты", "projects", actions.projects()));
    final VBox recentList = new VBox(8);
    final VBox recents = section("Недавние опыты", recentList);
    if (recent.isEmpty()) {
      recentList
          .getChildren()
          .add(
              text(
                  "Здесь появятся опыты, которые вы открывали. Новая работа начинается с кнопок выше.",
                  "welcome-hint"));
    } else {
      for (final Path path : recent.stream().limit(5).toList()) {
        final String filename =
            path.getFileName() == null ? path.toString() : path.getFileName().toString();
        final String name =
            filename.replaceFirst(
                "(?i)(-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})?\\.qchem$", "");
        final Button button =
            action(
                name,
                "recent-" + recentList.getChildren().size(),
                () -> actions.openRecent().accept(path));
        button.getStyleClass().add("welcome-list-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setTooltip(new Tooltip(path.toString()));
        recentList.getChildren().add(button);
      }
    }
    final TilePane lists = new TilePane(22, 18);
    lists.setPrefColumns(2);
    lists.setPrefTileWidth(470);
    lists.setTileAlignment(Pos.TOP_LEFT);
    projects.setPrefWidth(470);
    recents.setPrefWidth(470);
    lists.getChildren().addAll(projects, recents);
    final VBox content = new VBox(24, hero, choices, hint, this.recoveryCard, lists);
    content.setMinWidth(0);
    choices.setMinWidth(0);
    lists.setMinWidth(0);
    content.setMaxWidth(1010);
    content.setPadding(new Insets(38, 28, 32, 28));
    if (actions.current() != null) {
      content
          .getChildren()
          .add(1, action("Вернуться к текущему опыту", "current", actions.current()));
    }
    final StackPane aligned = new StackPane(content);
    aligned.setMinWidth(0);
    aligned.setAlignment(Pos.TOP_CENTER);
    final ScrollPane viewport = scroll(aligned);
    viewport.setFitToHeight(false);
    viewport.getStyleClass().add("welcome-scroll");
    this.setCenter(viewport);
    viewport
        .viewportBoundsProperty()
        .addListener(
            (o, before, after) -> {
              final double width =
                  Math.floor(Math.max(220, Math.min(954, after.getWidth() - 56)) - 2);
              choices.setPrefTileWidth(width < 650 ? width : Math.floor((width - 28) / 3));
              choices
                  .getChildren()
                  .forEach(n -> ((Region) n).setPrefWidth(choices.getPrefTileWidth()));
              final double column = width < 760 ? width : Math.floor((width - 22) / 2);
              lists.setPrefTileWidth(column);
              projects.setPrefWidth(column);
              recents.setPrefWidth(column);
            });
    this.reader =
        Thread.ofVirtual()
            .name("laboratory-welcome-projects")
            .start(
                () -> {
                  try {
                    final var available = repository.projects();
                    Platform.runLater(
                        () -> {
                          if (this.disposed) {
                            return;
                          }
                          this.projectList.getChildren().clear();
                          if (available.isEmpty()) {
                            this.projectList
                                .getChildren()
                                .add(
                                    text(
                                        "Пока нет проектов. Каждый проект объединяет опыты одной исследовательской задачи.",
                                        "welcome-hint"));
                          }
                          for (final Project project : available.stream().limit(5).toList()) {
                            final Button button =
                                action(
                                    project.title(),
                                    "project-" + this.projectList.getChildren().size(),
                                    () -> actions.openProject().accept(project));
                            button.getStyleClass().add("welcome-list-item");
                            button.setAlignment(Pos.CENTER_LEFT);
                            button.setMaxWidth(Double.MAX_VALUE);
                            this.projectList.getChildren().add(button);
                          }
                        });
                  } catch (final Exception failure) {
                    Platform.runLater(
                        () -> {
                          if (!this.disposed) {
                            this.projectList
                                .getChildren()
                                .setAll(
                                    text(
                                        "Не удалось прочитать проекты: "
                                            + java.util.Objects.toString(
                                                failure.getMessage(), "ошибка чтения"),
                                        "welcome-hint"));
                          }
                        });
                  }
                });
  }

  void pendingDrafts(final int count) {
    this.recoveryText.setText(
        "Сохранённых черновиков: "
            + count
            + ". Можно восстановить работу сейчас или вернуться к ней позже через меню «Файл».");
    this.recoveryCard.setVisible(count > 0);
    this.recoveryCard.setManaged(count > 0);
  }

  @Override
  void dispose() {
    super.dispose();
    if (this.reader != null) {
      this.reader.interrupt();
    }
  }

  private static Label text(final String value, final String style) {
    final Label label = note(value);
    label.getStyleClass().add(style);
    label.setMinWidth(0);
    return label;
  }

  private static VBox section(final String title, final Node body) {
    final VBox box = new VBox(12, text(title, "welcome-section-title"), body);
    box.setMinWidth(0);
    return box;
  }

  private static Button action(final String title, final String id, final Runnable callback) {
    final Button button = new Button(title);
    button.setId("welcome-" + id);
    button.setWrapText(true);
    button.setMinHeight(Region.USE_PREF_SIZE);
    button.setOnAction(e -> callback.run());
    return button;
  }

  private static Button card(
      final String title,
      final String detail,
      final String id,
      final Runnable callback,
      final boolean primary) {
    final Button button = action("", id, callback);
    button.setGraphic(
        new VBox(
            10, text(title + "  →", "welcome-card-title"), text(detail, "welcome-card-detail")));
    button.setAlignment(Pos.TOP_LEFT);
    button.setPrefWidth(306);
    button.setMinWidth(0);
    button.setMinHeight(130);
    button.setMaxHeight(Double.MAX_VALUE);
    button.getStyleClass().add(primary ? "welcome-card-primary" : "welcome-card");
    button.setAccessibleText(title + ". " + detail);
    return button;
  }
}
