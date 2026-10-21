/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

public final class LaboratoryTutorialAudit {
  static Path home;
  static int checks;
  static Stage stage;
  static LaboratoryTutorialPage page;

  static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
    checks++;
    System.out.println("PASS " + message);
  }

  public static void main(String[] args) throws Exception {
    home = Files.createTempDirectory("quantum-tutorial-audit-");
    System.setProperty("user.home", home.toString());
    System.setProperty(
        "quantum.laboratory.viewSettings", home.resolve("view.properties").toString());
    System.setProperty(
        "quantum.laboratory.sessionSettings", home.resolve("session.properties").toString());
    var ready = new CompletableFuture<Void>();
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          ready.complete(null);
        });
    ready.get();
    try {
      storeChecks();
      runCourse();
      pauseAndRestart();
      workspaceIsolation();
      automaticCourse();
      advancedCourse();
      automaticControls();
    } finally {
      Platform.exit();
    }
    System.out.println("TUTORIAL AUDIT: " + checks + " checks passed; home=" + home);
  }

  static void storeChecks() throws Exception {
    Path base = home.resolve("store");
    var s = new LaboratoryTutorialStore(base);
    Path run = s.start();
    Files.writeString(run.resolve("mine.txt"), "tutorial");
    s.progress(12, false);
    var loaded = new LaboratoryTutorialStore(base);
    check(loaded.step() == 12 && loaded.directory().equals(run), "progress survives reopen");
    Path sentinel = home.resolve("real-work.qchem");
    Files.writeString(sentinel, "do not touch");
    Files.writeString(run.resolve(".quantum-tutorial-run"), "wrong");
    boolean refused = false;
    try {
      s.delete();
    } catch (java.io.IOException e) {
      refused = true;
    }
    check(
        refused && Files.exists(run.resolve("mine.txt")),
        "unowned directory rejected before deletion");
    Files.writeString(run.resolve(".quantum-tutorial-run"), run.getFileName().toString());
    s.delete();
    check(
        !Files.exists(run) && Files.readString(sentinel).equals("do not touch"),
        "only owned run deleted");
    Path next = s.start();
    check(!next.equals(run) && s.step() == 0, "restart gets fresh run");
    Path linked = next.resolve("external-link"), outside = home.resolve("external-project");
    Files.createDirectories(outside);
    Files.writeString(outside.resolve("keep.qchem"), "working project");
    Files.writeString(next.resolve("owned.txt"), "training");
    var junction =
        new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            "New-Item -ItemType Junction -Path $env:Q_TUTORIAL_LINK -Target $env:Q_TUTORIAL_TARGET | Out-Null");
    junction.environment().put("Q_TUTORIAL_LINK", linked.toString());
    junction.environment().put("Q_TUTORIAL_TARGET", outside.toString());
    var process = junction.redirectErrorStream(true).start();
    check(
        process.waitFor(15, TimeUnit.SECONDS) && process.exitValue() == 0,
        "junction fixture created");
    refused = false;
    try {
      s.delete();
    } catch (java.io.IOException failure) {
      refused = true;
    }
    check(
        refused
            && Files.exists(next.resolve("owned.txt"))
            && Files.exists(outside.resolve("keep.qchem")),
        "junction rejected before deletion; external project untouched");
    Files.delete(linked);
    s.delete();
    Files.writeString(base.resolve("progress.properties"), "run=../real-work.qchem\n");
    refused = false;
    try {
      new LaboratoryTutorialStore(base);
    } catch (java.io.IOException e) {
      refused = true;
    }
    check(refused, "progress path traversal rejected");
  }

  static void runCourse() throws Exception {
    var store = new LaboratoryTutorialStore(home.resolve("Training"));
    fx(
        () -> {
          stage = new Stage();
          page =
              new LaboratoryTutorialPage(
                  stage,
                  new LaboratoryViewPreferences(home.resolve("offer.properties")),
                  store,
                  () -> {});
          var root = new StackPane(page);
          root.getStyleClass().addAll("quantum-root", "theme-light");
          var scene = new Scene(root, 1280, 860);
          scene
              .getStylesheets()
              .add(
                  LaboratoryWorkspace.class
                      .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                      .toExternalForm());
          stage.setScene(scene);
          shot("welcome");
          fire("tutorial-start");
          if (((ToggleButton) get(page, "automatic")).isSelected()) fire("tutorial-automatic");
          return null;
        });
    for (int i = 0; i < 31; i++) {
      final int index = i;
      await(() -> (int) get(page, "index") == index);
      await(
          () -> {
            page.applyCss();
            page.layout();
            Node target = (Node) call(page, "currentTarget");
            return target != null && !target.isDisabled()
                || target == null && !((Button) get(page, "next")).isDisabled();
          });
      fx(
          () -> {
            call(page, "update");
            Node target = (Node) call(page, "currentTarget");
            System.out.println(
                "STEP "
                    + index
                    + " target="
                    + (target == null ? "null" : target.getId())
                    + " title="
                    + ((Label) get(page, "title")).getText()
                    + " error="
                    + ((Label) get(page, "error")).getText());
            if (target instanceof TextInputControl text)
              text.setText(
                  index == 1
                      ? "Мой учебный проект"
                      : index == 4
                          ? "Моя серия"
                          : index == 21
                              ? "Учебная запись: эксперимент не проводился."
                              : index == 26
                                  ? "Учебное вещество"
                                  : "Мой опыт " + (1 + (index - 7) / 4));
            else if (target instanceof ComboBox<?> combo) combo.getSelectionModel().selectFirst();
            else if (target instanceof TabPane tabs) tabs.getSelectionModel().select(1);
            else if (target instanceof ButtonBase button && !button.isDisabled()) button.fire();
            return null;
          });
      await(
          () -> {
            call(page, "update");
            return !((Button) get(page, "next")).isDisabled();
          });
      await(
          () -> {
            var w = (LaboratoryWorkspace) get(page, "workspace");
            var cp = get(w, "computationPage");
            return !(cp instanceof LaboratoryComputationPage p) || !p.running;
          });
      fx(
          () -> {
            if (Set.of(1, 7, 19, 21, 24, 26, 28).contains(index)) shot("step-" + index);
            fire("tutorial-next");
            check((int) get(page, "index") == index + 1, "course step " + index + " completed");
            return null;
          });
    }
    check(store.completed(), "course marked complete");
    var repo = new LaboratoryNotebookRepository(store.directory().resolve("Projects"));
    var snapshot = repo.load(repo.projects().getFirst());
    check(snapshot.items().size() == 3, "three real experiments persisted");
    check(
        snapshot.sections().size() == 1 && snapshot.project().title().equals("Мой учебный проект"),
        "custom project and series names persisted");
    var drawing = new LaboratoryFileRepository().read(snapshot.items().getFirst().path()).drawing();
    check(!drawing.experiment().journal().isEmpty(), "observation saved to QCHEM");
    try (var files = Files.walk(store.directory().resolve("Library"))) {
      check(
          files.anyMatch(p -> p.toString().endsWith(".qchem")),
          "own substance saved in isolated library");
    }
    check(
        !Files.exists(home.resolve("Documents/Quantum IR Studio/.chemistry-library")),
        "working personal library untouched");
    fx(
        () -> {
          shot("complete");
          var w = (LaboratoryWorkspace) get(page, "workspace");
          for (var c : w.tutorialCommands()) {
            var g = LaboratoryTutorialLessons.forCommand(c.id());
            check(g.texts().length == 5 && g.targets().length == 5, "guide exists: " + c.id());
          }
          fire("tutorial-catalog");
          shot("catalog");
          return null;
        });
    var commands = fx(() -> ((LaboratoryWorkspace) get(page, "workspace")).tutorialCommands());
    for (var command : commands) {
      fx(
          () -> {
            fire("tutorial-guide-" + command.id());
            return null;
          });
      await(() -> ((LaboratoryWorkspace) get(page, "workspace")).tutorialIdle());
      fx(
          () -> {
            var w = (LaboratoryWorkspace) get(page, "workspace");
            var before = w.tutorialEditor().document();
            var actions =
                LaboratoryTutorialActions.collect(
                    (Parent) w.tutorialView(), w.tutorialContextMenus());
            check(
                !actions.isEmpty() && actions.stream().allMatch(a -> !a.explanation().isBlank()),
                "section commands explained " + command.id());
            check(
                before.equals(w.tutorialEditor().document()),
                "section commands inspected without mutation " + command.id());
            System.out.println("FUNCTIONS " + command.id() + "=" + actions.size());
            dumpActions(command.id(), actions);
            check(
                actions.stream().noneMatch(a -> a.explanation().startsWith("Команда «")),
                "section has concrete command help " + command.id());
            return null;
          });
      for (int tip = 0;
          tip < LaboratoryTutorialLessons.forCommand(command.id()).texts().length;
          tip++) {
        final int current = tip;
        fx(
            () -> {
              call(page, "update");
              check(
                  (boolean) get(page, "guide")
                      && command.id().equals(get(page, "guideId"))
                      && (int) get(page, "guideStep") == current,
                  "guide opens " + command.id() + "/" + current);
              Node target = (Node) call(page, "currentTarget");
              String selector =
                  LaboratoryTutorialLessons.forCommand(command.id()).targets()[current];
              System.out.println(
                  "GUIDE "
                      + command.id()
                      + "/"
                      + current
                      + " selector="
                      + selector
                      + " target="
                      + (target == null ? "null" : target.getId())
                      + " error="
                      + ((Label) get(page, "error")).getText());
              check(
                  target != null && target.getScene() != null,
                  "guide target attached " + command.id() + "/" + current);
              fire("tutorial-next");
              return null;
            });
      }
      await(() -> page.lookup("#tutorial-catalog") != null);
    }
    check(
        new LaboratoryTutorialStore(home.resolve("Training")).lessons().size() == 33,
        "all 33 completed guides persist");
    fx(
        () -> {
          fire("tutorial-guide-library");
          return null;
        });
    await(() -> call(page, "currentTarget") != null);
    fx(
        () -> {
          shot("library-guide");
          fire("tutorial-hints");
          check(!((Node) get(page, "spotlight")).isVisible(), "hints can be disabled");
          fire("tutorial-hints");
          return null;
        });
    fx(
        () -> {
          call(page, "catalog");
          fire("tutorial-finish");
          fire("tutorial-delete");
          shot("delete-confirmation");
          fire("tutorial-confirm-delete");
          return null;
        });
    check(!store.exists(), "delete through UI removes session");
    check(Files.exists(home.resolve("real-work.qchem")), "real work preserved after UI cleanup");
  }

  static void mount(LaboratoryTutorialStore store, int width, int height, boolean dark) {
    page =
        new LaboratoryTutorialPage(
            stage,
            new LaboratoryViewPreferences(home.resolve("offer.properties")),
            store,
            () -> {});
    var root = new StackPane(page);
    root.getStyleClass().addAll("quantum-root", dark ? "theme-dark" : "theme-light");
    var scene = new Scene(root, width, height);
    scene
        .getStylesheets()
        .add(
            LaboratoryWorkspace.class
                .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                .toExternalForm());
    stage.setScene(scene);
  }

  static void pauseAndRestart() throws Exception {
    Path base = home.resolve("PauseTraining");
    var store = new LaboratoryTutorialStore(base);
    fx(
        () -> {
          mount(store, 1024, 768, false);
          fire("tutorial-start");
          return null;
        });
    await(() -> page.lookup("#notebook-new-project") != null);
    fx(
        () -> {
          fire("notebook-new-project");
          call(page, "update");
          fire("tutorial-next");
          return null;
        });
    await(() -> page.lookup("#notebook-project-name") != null);
    fx(
        () -> {
          ((TextField) page.lookup("#notebook-project-name")).setText("Незаконченный проект");
          shot("narrow-input");
          fire("tutorial-pause");
          check((boolean) get(page, "ended"), "pause closes unfinished form");
          return null;
        });
    check(
        store.step() == 1
            && store.draft().getProperty("notebook-project-name").equals("Незаконченный проект"),
        "pause stores raw name and step");
    var restored = new LaboratoryTutorialStore(base);
    fx(
        () -> {
          mount(restored, 1024, 768, true);
          fire("tutorial-start");
          return null;
        });
    await(
        () -> {
          call(page, "update");
          return page.lookup("#notebook-project-name") instanceof TextField f
              && f.getText().equals("Незаконченный проект");
        });
    fx(
        () -> {
          shot("dark-resume");
          return null;
        });
    check(
        new LaboratoryNotebookRepository(restored.directory().resolve("Projects"))
            .projects()
            .isEmpty(),
        "resume does not create duplicate project");
    Path old = restored.directory();
    fx(
        () -> {
          fire("tutorial-restart");
          fire("tutorial-cancel-delete");
          check(Files.exists(old), "restart cancellation retains data");
          fire("tutorial-restart");
          fire("tutorial-confirm-delete");
          return null;
        });
    await(() -> page.lookup("#notebook-new-project") != null);
    check(
        !Files.exists(old) && restored.step() == 0 && !restored.directory().equals(old),
        "restart deletes previous run and starts at zero");
    fx(
        () -> {
          fire("tutorial-pause");
          return null;
        });
    restored.delete();
  }

  static void workspaceIsolation() throws Exception {
    fx(
        () -> {
          var original = new LaboratoryWorkspace();
          original.content(stage);
          original
              .tutorialEditor()
              .details(
                  "Несохранённая работа",
                  ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions.EMPTY,
                  "До обучения");
          var before = original.tutorialEditor().document();
          call(original, "startTutorial");
          check(
              original.tutorialEditor().document().equals(before),
              "opening tutorial preserves unsaved original document");
          var undo =
              new javafx.scene.input.KeyEvent(
                  javafx.scene.input.KeyEvent.KEY_PRESSED,
                  "",
                  "",
                  javafx.scene.input.KeyCode.Z,
                  false,
                  true,
                  false,
                  false);
          original.shortcut(undo);
          check(
              undo.isConsumed() && original.tutorialEditor().document().equals(before),
              "tutorial shortcuts cannot alter original work");
          call(original, "endTutorial");
          check(
              original.tutorialEditor().document().equals(before),
              "returning restores original document unchanged");
          return null;
        });
  }

  static void performBasic(int index) throws Exception {
    call(page, "update");
    Node target = (Node) call(page, "currentTarget");
    if (target instanceof TextInputControl text)
      text.setText(
          index == 1
              ? "Автоматический проект"
              : index == 4
                  ? "Автоматическая серия"
                  : index == 21
                      ? "Учебная запись — опыт не проводился."
                      : index == 26 ? "Вещество автокурса" : "Опыт автокурса " + index);
    else if (target instanceof ComboBox<?> combo) combo.getSelectionModel().selectFirst();
    else if (target instanceof TabPane tabs) tabs.getSelectionModel().select(1);
    else if (target instanceof ButtonBase button && !button.isDisabled()) button.fire();
  }

  static void automaticCourse() throws Exception {
    var store = new LaboratoryTutorialStore(home.resolve("AutoTraining"));
    fx(
        () -> {
          mount(store, 1280, 860, false);
          fire("tutorial-start");
          if (!((ToggleButton) get(page, "automatic")).isSelected()) fire("tutorial-automatic");
          return null;
        });
    for (int i = 0; i < 31; i++) {
      final int step = i;
      await(() -> (int) get(page, "index") >= step);
      if (fx(() -> (int) get(page, "index")) > step) continue;
      await(
          () -> {
            call(page, "update");
            Node target = (Node) call(page, "currentTarget");
            return (int) get(page, "index") > step
                || target != null && !target.isDisabled()
                || Set.of(19, 28, 30).contains(step);
          });
      fx(
          () -> {
            if ((int) get(page, "index") == step) performBasic(step);
            return null;
          });
      if (Set.of(19, 28, 30).contains(step)) {
        Thread.sleep(700);
        fx(
            () -> {
              check(
                  (int) get(page, "index") == step,
                  "reading step stays until acknowledged " + step);
              fire("tutorial-next");
              return null;
            });
      }
      await(() -> (int) get(page, "index") > step);
      check(true, "automatic course advances without Next " + step);
    }
    check(store.completed(), "automatic course completes");
    fx(
        () -> {
          fire("tutorial-delete");
          fire("tutorial-confirm-delete");
          return null;
        });
  }

  static void gesture(LaboratoryWorkspace workspace, LaboratoryCanvas.Gesture value)
      throws Exception {
    var method =
        LaboratoryWorkspace.class.getDeclaredMethod("gesture", LaboratoryCanvas.Gesture.class);
    method.setAccessible(true);
    method.invoke(workspace, value);
  }

  static void advancedCourse() throws Exception {
    var store = new LaboratoryTutorialStore(home.resolve("AdvancedTraining"));
    fx(
        () -> {
          mount(store, 1280, 860, false);
          fire("tutorial-advanced-start");
          return null;
        });
    await(() -> page.lookup("#tutorial-catalog") != null);
    check(
        new LaboratoryNotebookRepository(store.directory().resolve("Projects"))
                .load(
                    new LaboratoryNotebookRepository(store.directory().resolve("Projects"))
                        .projects()
                        .getFirst())
                .items()
                .size()
            == 3,
        "advanced mode available directly with isolated practice files");
    fx(
        () -> {
          shot("advanced-catalog");
          fire("tutorial-guide-drawing-practice");
          return null;
        });
    drawingRound();
    fx(
        () -> {
          fire("tutorial-guide-drawing-practice");
          return null;
        });
    drawingRound();
    check(true, "drawing practice can be repeated after selecting oxygen");
    check(store.lessons().contains("drawing-practice"), "drawing lesson completion saved");
    for (var topic : LaboratoryTutorialTopics.ALL) {
      if (topic.id().equals("drawing-practice")) continue;
      fx(
          () -> {
            fire("tutorial-guide-" + topic.id());
            return null;
          });
      await(() -> ((LaboratoryWorkspace) get(page, "workspace")).tutorialIdle());
      for (int i = 0; i < topic.guide().texts().length; i++) {
        final int step = i;
        fx(
            () -> {
              call(page, "update");
              check(
                  (int) get(page, "guideStep") == step && call(page, "currentTarget") != null,
                  "advanced topic target " + topic.id() + "/" + step);
              fire("tutorial-next");
              return null;
            });
      }
    }
    fx(
        () -> {
          fire("tutorial-guide-editor-atoms");
          var w = (LaboratoryWorkspace) get(page, "workspace");
          var before = w.tutorialEditor().document();
          var actions =
              LaboratoryTutorialActions.collect(
                  (Parent) w.tutorialView(), w.tutorialContextMenus());
          check(
              actions.stream().anyMatch(a -> a.path().contains("Заряд атома")),
              "action inventory includes atom context commands");
          check(
              actions.stream().anyMatch(a -> a.path().contains("Импорт")),
              "action inventory includes nested file menus");
          check(
              actions.stream().allMatch(a -> !a.explanation().isBlank()),
              "every discovered action has explanation");
          check(
              before.equals(w.tutorialEditor().document()),
              "action inventory does not execute commands");
          System.out.println("ADVANCED editor actions=" + actions.size());
          dumpActions("drawing", actions);
          check(
              actions.stream().noneMatch(a -> a.explanation().startsWith("Команда «")),
              "editor has concrete command help");
          fire("tutorial-screen-actions");
          shot("advanced-actions");
          ((TextField) page.lookup("#tutorial-action-search")).setText("Заряд атома");
          fire("tutorial-action-0");
          call(page, "update");
          check(
              get(page, "referenceItem") != null, "specific command help highlights real control");
          fire("tutorial-next");
          fire("tutorial-screen-actions");
          fire("tutorial-actions-back");
          fire("tutorial-next");
          fire("tutorial-pause");
          return null;
        });
    var reopened = new LaboratoryTutorialStore(home.resolve("AdvancedTraining"));
    fx(
        () -> {
          mount(reopened, 1024, 768, true);
          fire("tutorial-start");
          return null;
        });
    await(() -> "editor-atoms".equals(get(page, "guideId")) && (int) get(page, "guideStep") == 1);
    check(true, "advanced mode resumes selected lesson and step");
    fx(
        () -> {
          shot("advanced-resume-dark");
          fire("tutorial-pause");
          return null;
        });
    store.delete();
  }

  static void drawingRound() throws Exception {
    for (int i = 0; i < 16; i++) {
      final int step = i;
      await(() -> (int) get(page, "guideStep") >= step);
      if (fx(() -> (int) get(page, "guideStep")) > step) continue;
      await(
          () -> {
            call(page, "update");
            Node target = (Node) call(page, "currentTarget");
            return (int) get(page, "guideStep") > step || target != null && !target.isDisabled();
          });
      fx(
          () -> {
            if ((int) get(page, "guideStep") != step) return null;
            check(
                ((Label) get(page, "chapter")).getText().contains("ШАГ " + (step + 1) + " / 16"),
                "drawing instruction follows step " + step);
            var w = (LaboratoryWorkspace) get(page, "workspace");
            var hit = (LaboratoryCanvas.Hit) get(w, "selected");
            switch (step) {
              case 0, 15 -> fire("tutorial-next");
              case 2 ->
                  gesture(
                      w,
                      new LaboratoryCanvas.Gesture(
                          null, 600, 320, true, null, false, 70, 0, null, null));
              case 4 ->
                  gesture(
                      w,
                      new LaboratoryCanvas.Gesture(
                          hit, 600, 320, false, null, false, 0, 0, null, null));
              case 6 -> {
                var molecule =
                    w.tutorialEditor().document().layouts().get(hit.componentId()).molecule();
                var heavy =
                    molecule.atoms().stream()
                        .filter(a -> a.element().atomicNumber() != 1)
                        .map(a -> a.id())
                        .toList();
                gesture(
                    w,
                    new LaboratoryCanvas.Gesture(
                        null,
                        565,
                        320,
                        false,
                        null,
                        false,
                        0,
                        0,
                        null,
                        new LaboratoryCanvas.BondHit(
                            hit.componentId(), heavy.get(0), heavy.get(1))));
              }
              case 10 ->
                  gesture(
                      w,
                      new LaboratoryCanvas.Gesture(
                          null, 650, 500, false, null, false, 0, 0, null, null));
              case 12 ->
                  gesture(
                      w,
                      new LaboratoryCanvas.Gesture(
                          hit, 675, 510, true, null, false, 25, 10, hit, null));
              default -> {
                Node target = (Node) call(page, "currentTarget");
                if (target instanceof ButtonBase b) b.fire();
              }
            }
            if (step == 4 || step == 12) shot("drawing-practice-" + step);
            return null;
          });
      if (step < 15) await(() -> (int) get(page, "guideStep") > step);
      else await(() -> page.lookup("#tutorial-catalog") != null);
      check(true, "drawing practice real action " + step);
    }
  }

  static void automaticControls() throws Exception {
    var store = new LaboratoryTutorialStore(home.resolve("AutoControls"));
    Files.createDirectories(store.start().resolve("Library/.chemistry-library/.staging-failed"));
    fx(
        () -> {
          mount(store, 1280, 860, false);
          fire("tutorial-start");
          check(
              !(boolean) get(page, "librarySaved"), "failed library staging is not a saved entry");
          if (!((ToggleButton) get(page, "automatic")).isSelected()) fire("tutorial-automatic");
          return null;
        });
    await(() -> page.lookup("#notebook-new-project") != null);
    fx(
        () -> {
          fire("notebook-new-project");
          return null;
        });
    await(() -> (int) get(page, "index") == 1);
    Thread.sleep(850);
    fx(
        () -> {
          check((int) get(page, "index") == 1, "empty required name never auto-advances");
          ((TextField) page.lookup("#notebook-project-name")).setText("Часть");
          return null;
        });
    Thread.sleep(850);
    fx(
        () -> {
          ((TextField) page.lookup("#notebook-project-name")).setText("Полное название проекта");
          return null;
        });
    Thread.sleep(850);
    fx(
        () -> {
          check((int) get(page, "index") == 1, "continued typing restarts debounce");
          return null;
        });
    await(() -> (int) get(page, "index") == 2);
    fx(
        () -> {
          fire("tutorial-automatic");
          fire("notebook-create-project");
          return null;
        });
    await(() -> ((LaboratoryWorkspace) get(page, "workspace")).tutorialIdle());
    Thread.sleep(850);
    fx(
        () -> {
          check((int) get(page, "index") == 2, "auto toggle off retains completed step");
          call(page, "update");
          fire("tutorial-next");
          fire("tutorial-back");
          fire("tutorial-automatic");
          return null;
        });
    Thread.sleep(850);
    fx(
        () -> {
          check((int) get(page, "index") == 2, "Back can review completed action without bounce");
          fire("tutorial-pause");
          return null;
        });
    store.delete();
  }

  static void shot(String name) throws Exception {
    call(page, "update");
    var previous = stage.getScene();
    var root = previous.getRoot();
    var styles = new ArrayList<>(previous.getStylesheets());
    double width = previous.getWidth(), height = previous.getHeight();
    previous.setRoot(new StackPane());
    var scene = new Scene(root, width, height);
    scene.getStylesheets().setAll(styles);
    stage.setScene(scene);
    // Hidden scenes have no regular CSS pulse. Refresh label skins after reattachment.
    refreshLabels(root);
    if (root instanceof Region region) {
      region.setMinSize(width, height);
      region.setPrefSize(width, height);
      region.resize(width, height);
    }
    for (int i = 0; i < 3; i++) {
      requestLayout(root);
      root.applyCss();
      root.layout();
    }
    ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopScreenshotSupport.saveStageScreenshot(
        stage, "target/tutorial-audit/" + name + ".png");
  }

  static void refreshLabels(Parent parent) {
    if (parent instanceof Label label) {
      label.setSkin(null);
      label.setSkin(new javafx.scene.control.skin.LabelSkin(label));
    }
    for (Node child : new ArrayList<>(parent.getChildrenUnmodifiable()))
      if (child instanceof Parent p) refreshLabels(p);
  }

  static void requestLayout(Parent parent) {
    parent.setCache(false);
    parent.requestLayout();
    for (Node child : parent.getChildrenUnmodifiable())
      if (child instanceof Parent p) requestLayout(p);
  }

  static void dumpActions(String section, List<LaboratoryTutorialActions.Item> actions)
      throws Exception {
    var dir = Path.of("target/tutorial-audit/functions");
    Files.createDirectories(dir);
    Files.write(
        dir.resolve(section + ".txt"),
        actions.stream().map(a -> a.path() + "\n" + a.explanation() + "\n").toList());
  }

  static void fire(String id) {
    page.applyCss();
    page.layout();
    var n = page.lookup("#" + id);
    if (!(n instanceof ButtonBase b)) throw new AssertionError("Missing button " + id);
    if (b.isDisabled()) throw new AssertionError("Disabled " + id);
    b.fire();
  }

  static Object get(Object obj, String key) throws Exception {
    for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass())
      try {
        var f = c.getDeclaredField(key);
        f.setAccessible(true);
        return f.get(obj);
      } catch (NoSuchFieldException e) {
      }
    throw new NoSuchFieldException(key);
  }

  static Object call(Object obj, String key) throws Exception {
    var m = obj.getClass().getDeclaredMethod(key);
    m.setAccessible(true);
    return m.invoke(obj);
  }

  static <T> T fx(Callable<T> task) throws Exception {
    var f = new CompletableFuture<T>();
    Platform.runLater(
        () -> {
          try {
            f.complete(task.call());
          } catch (Throwable e) {
            f.completeExceptionally(e);
          }
        });
    return f.get(30, TimeUnit.SECONDS);
  }

  static void await(Callable<Boolean> c) throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
    while (System.nanoTime() < end) {
      if (fx(
          () -> {
            if (stage != null) {
              stage.getScene().getRoot().applyCss();
              stage.getScene().getRoot().layout();
            }
            return c.call();
          })) return;
      Thread.sleep(100);
    }
    throw new AssertionError(
        "Timed out; index="
            + fx(() -> get(page, "index"))
            + " error="
            + fx(() -> ((Label) get(page, "error")).getText()));
  }
}
