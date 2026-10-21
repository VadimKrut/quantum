/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** Opt-in navigation audit. Uses an isolated home and never shows a window or uses a desktop robot. */
public final class LaboratoryNavigationAudit {
  private static Path sandbox;
  private static final List<String> failures = new ArrayList<>();
  private static int checks;

  public static void main(String[] args) throws Exception {
    sandbox = Files.createTempDirectory("quantum-navigation-");
    System.setProperty("user.home", sandbox.toString());
    System.setProperty(
        "quantum.laboratory.viewSettings", sandbox.resolve("view.properties").toString());
    System.setProperty(
        "quantum.laboratory.sessionSettings", sandbox.resolve("session.properties").toString());
    var done = new CompletableFuture<Void>();
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          done.complete(null);
        });
    try {
      done.get(20, TimeUnit.SECONDS);
      if (Arrays.asList(args).contains("--snapshots-only")) snapshots();
      else audit();
    } finally {
      Platform.exit();
    }
    System.out.println(
        "AUDIT: " + checks + " checks; " + failures.size() + " failures; sandbox=" + sandbox);
    if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
  }

  private static void audit() throws Exception {
    var ids =
        fx(
            () -> {
              var probe = fixture(false);
              try {
                return definitions(probe).stream()
                    .map(LaboratoryCommandPalette.Command::id)
                    .toList();
              } finally {
                dispose(probe);
              }
            });
    for (boolean desk : List.of(false, true))
      for (boolean palette : List.of(false, true))
        for (String id : ids) {
          String label =
              (palette ? "Действия" : "Разделы")
                  + " / "
                  + (desk ? "из опыта" : "из редактора")
                  + " / "
                  + id;
          var workspace =
              fx(
                  () -> {
                    var w =
                        fixture(
                            Set.of("calculation", "cip", "relative-stereo", "conformers")
                                .contains(id));
                    if (desk) {
                      var method = w.getClass().getDeclaredMethod("desk", String.class);
                      method.setAccessible(true);
                      method.invoke(w, (String) null);
                      set(w, "selected", null);
                    }
                    return w;
                  });
          pulse();
          try {
            fx(
                () -> {
                  var root = (BorderPane) get(workspace, "root");
                  root.applyCss();
                  root.layout();
                  check(
                      root.getTop().isVisible() && !workMenu(workspace).isDisabled(),
                      label + " доступно");
                  if (palette) {
                    var commands = definitions(workspace);
                    var dialog =
                        LaboratoryCommandPalette.create(
                            (Stage) get(workspace, "owner"),
                            commands,
                            (LaboratoryViewPreferences) get(workspace, "viewPreferences"));
                    dialog.getDialogPane().applyCss();
                    @SuppressWarnings("unchecked")
                    var list =
                        (ListView<LaboratoryCommandPalette.Command>)
                            dialog.getDialogPane().lookup("#chemistry-actions-list");
                    list.getSelectionModel()
                        .select(
                            commands.stream()
                                .filter(c -> c.id().equals(id))
                                .findFirst()
                                .orElseThrow());
                    var execute =
                        dialog.getDialogPane().getButtonTypes().stream()
                            .filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst()
                            .orElseThrow();
                    var selected = dialog.getResultConverter().call(execute);
                    check(selected != null && selected.id().equals(id), label + " выбор в палитре");
                    selected.action().run();
                  } else findId(workMenu(workspace).getItems(), "section-" + id).fire();
                  root.applyCss();
                  root.layout();
                  var error = (Label) get(workspace, "fileProblem");
                  check(
                      root.getCenter() != get(workspace, "workArea") && error.getText().isBlank(),
                      label
                          + " открыто: "
                          + root.getCenter().getClass().getSimpleName()
                          + " "
                          + error.getText());
                  return null;
                });
            waitReady(workspace);
            fx(
                () -> {
                  var root = (BorderPane) get(workspace, "root");
                  if (!Set.of("notebook", "desk", "repeat").contains(id)) {
                    if (id.equals("library") || id.equals("materials")) {
                      var back =
                          (Button)
                              root.lookup(
                                  id.equals("library")
                                      ? "#chemistry-library-back"
                                      : "#chemistry-materials-back");
                      back.fire();
                    } else {
                      var page = (LaboratoryComputationPage) get(workspace, "computationPage");
                      page.finish();
                    }
                    check(
                        desk
                            ? get(workspace, "computationPage") instanceof LaboratoryExperimentDesk
                            : root.getCenter() == get(workspace, "workArea"),
                        label + " возврат");
                  }
                  return null;
                });
          } catch (Throwable e) {
            check(false, label + " => " + e);
          } finally {
            fx(
                () -> {
                  dispose(workspace);
                  return null;
                });
          }
        }
    guardUnsavedInput();
    returnToSeries();
    emptyExperiment(ids);
  }

  private static LaboratoryWorkspace fixture(boolean single) throws Exception {
    var workspace = new LaboratoryWorkspace();
    var stage = new Stage();
    stage.setScene(new Scene((javafx.scene.Parent) workspace.content(stage), 1280, 800));
    stage.getScene().getRoot().getStyleClass().addAll("quantum-root", "theme-light");
    stage
        .getScene()
        .getStylesheets()
        .add(
            LaboratoryWorkspace.class
                .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                .toExternalForm());
    call(workspace, "leaveComputation");
    var editor = (LaboratoryEditor) get(workspace, "editor");
    var sketch = new MoleculeSketchService();
    String first = editor.add(sketch.create(true, 200, 200), ReactionComponentRole.REACTANT);
    String second =
        single ? first : editor.add(sketch.create(false, 500, 200), ReactionComponentRole.PRODUCT);
    editor.stages(
        List.of(
            new LaboratoryReactionStage(
                "stage1",
                "Учебная стадия",
                LaboratoryReactionStage.Arrow.FORWARD,
                ReactionConditions.EMPTY,
                single
                    ? List.of(
                        new LaboratoryStageParticipant(first, ReactionComponentRole.REACTANT, null))
                    : List.of(
                        new LaboratoryStageParticipant(first, ReactionComponentRole.REACTANT, null),
                        new LaboratoryStageParticipant(
                            second, ReactionComponentRole.PRODUCT, null)))));
    editor.details(
        "Учебный опыт — проверка навигации",
        ReactionConditions.EMPTY,
        "Синтетические данные проверки");
    var path = sandbox.resolve("experiments").resolve(UUID.randomUUID() + ".qchem");
    String fingerprint = new LaboratoryFileRepository().write(path, editor.document(), null);
    editor.markSaved();
    Object files = get(workspace, "files");
    set(files, "path", path);
    set(files, "fingerprint", fingerprint);
    set(workspace, "folder", path.getParent());
    call(workspace, "reload");
    set(
        workspace,
        "selected",
        new LaboratoryCanvas.Hit(
            first, editor.document().layouts().get(first).atoms().getFirst().atomId()));
    call(workspace, "refresh");
    stage.getScene().getRoot().applyCss();
    stage.getScene().getRoot().layout();
    return workspace;
  }

  private static MenuButton workMenu(LaboratoryWorkspace w) throws Exception {
    return (MenuButton) ((BorderPane) get(w, "root")).lookup("#chemistry-work-menu");
  }

  @SuppressWarnings("unchecked")
  private static List<LaboratoryCommandPalette.Command> definitions(LaboratoryWorkspace w)
      throws Exception {
    return (List<LaboratoryCommandPalette.Command>) call(w, "navigationCommands");
  }

  private static MenuItem findId(List<MenuItem> items, String id) {
    for (var item : items) {
      if (id.equals(item.getId())) return item;
      if (item instanceof Menu menu) {
        var result = findId(menu.getItems(), id);
        if (result != null) return result;
      }
    }
    return null;
  }

  private static <T> T fx(Callable<T> action) throws Exception {
    var result = new CompletableFuture<T>();
    Platform.runLater(
        () -> {
          try {
            result.complete(action.call());
          } catch (Throwable e) {
            result.completeExceptionally(e);
          }
        });
    return result.get(20, TimeUnit.SECONDS);
  }

  private static void pulse() throws Exception {
    var ready = new CompletableFuture<Void>();
    fx(
        () -> {
          var pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(120));
          pause.setOnFinished(e -> ready.complete(null));
          pause.play();
          return null;
        });
    ready.get(10, TimeUnit.SECONDS);
  }

  private static void waitReady(LaboratoryWorkspace workspace) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (fx(
        () ->
            get(workspace, "computationPage") instanceof LaboratoryComputationPage p
                && p.running)) {
      if (System.nanoTime() >= deadline)
        throw new AssertionError("Страница не завершила загрузку за 20 секунд");
      pulse();
    }
  }

  private static void snapshots() throws Exception {
    var w = fx(() -> fixture(false));
    try {
      fx(
          () -> {
            var method = w.getClass().getDeclaredMethod("desk", String.class);
            method.setAccessible(true);
            method.invoke(w, (String) null);
            return null;
          });
      pulse();
      fx(
          () -> {
            var stage = (Stage) get(w, "owner");
            for (int width : new int[] {1280, 960}) {
              var root = (BorderPane) get(w, "root");
              root.setPrefSize(width, 800);
              stage.getScene().setRoot(new javafx.scene.layout.Pane());
              stage.setScene(new Scene(root, width, 800));
              stage
                  .getScene()
                  .getStylesheets()
                  .add(
                      LaboratoryWorkspace.class
                          .getResource(
                              "/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                          .toExternalForm());
              root.applyCss();
              root.layout();
              ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopScreenshotSupport
                  .saveStageScreenshot(stage, "target/studio-audit/desk-" + width + ".png");
            }
            return null;
          });
    } finally {
      fx(
          () -> {
            dispose(w);
            return null;
          });
    }
  }

  private static void emptyExperiment(List<String> ids) throws Exception {
    for (String id : ids) {
      var w =
          fx(
              () -> {
                var workspace = fixture(false);
                var editor = (LaboratoryEditor) get(workspace, "editor");
                editor.open(new LaboratoryEditor().document());
                call(workspace, "reload");
                var method = workspace.getClass().getDeclaredMethod("desk", String.class);
                method.setAccessible(true);
                method.invoke(workspace, (String) null);
                return workspace;
              });
      try {
        fx(
            () -> {
              definitions(w).stream()
                  .filter(c -> c.id().equals(id))
                  .findFirst()
                  .orElseThrow()
                  .action()
                  .run();
              var root = (BorderPane) get(w, "root");
              var problem = (Label) get(w, "fileProblem");
              check(
                  root.getCenter() != get(w, "workArea") || !problem.getText().isBlank(),
                  "Пустой опыт / " + id + " — страница или объяснение");
              return null;
            });
        waitReady(w);
      } finally {
        fx(
            () -> {
              dispose(w);
              return null;
            });
      }
    }
  }

  private static void returnToSeries() throws Exception {
    var w = fx(() -> fixture(false));
    try {
      var expected =
          fx(
              () -> {
                var repository = (LaboratoryNotebookRepository) get(w, "notebookRepository");
                var project = repository.createProject("Учебный проект");
                var snapshot =
                    repository.addSection(repository.load(project), null, "Учебная серия");
                String section = snapshot.sections().getFirst().id();
                var state =
                    new LaboratoryNotebookPage.ViewState(project.id(), section, null, "ACTIVE", "");
                set(w, "notebookView", state);
                var method = w.getClass().getDeclaredMethod("desk", String.class);
                method.setAccessible(true);
                method.invoke(w, (String) null);
                ((LaboratoryExperimentDesk) get(w, "computationPage")).finish();
                return state;
              });
      waitReady(w);
      fx(
          () -> {
            var page = (LaboratoryNotebookPage) get(w, "computationPage");
            var actual = page.viewState();
            check(
                expected.projectId().equals(actual.projectId())
                    && expected.sectionId().equals(actual.sectionId()),
                "К серии опытов — восстановлены проект и выбранная серия");
            return null;
          });
    } finally {
      fx(
          () -> {
            dispose(w);
            return null;
          });
    }
  }

  private static void guardUnsavedInput() throws Exception {
    fx(
        () -> {
          var w = fixture(false);
          try {
            var method = w.getClass().getDeclaredMethod("desk", String.class);
            method.setAccessible(true);
            method.invoke(w, (String) null);
            var root = (BorderPane) get(w, "root");
            var before = root.getCenter();
            var desk = (LaboratoryExperimentDesk) before;
            ((TextArea) get(desk, "observation")).setText("Незавершённое наблюдение");
            definitions(w).stream()
                .filter(c -> c.id().equals("report"))
                .findFirst()
                .orElseThrow()
                .action()
                .run();
            check(
                root.getCenter() == before
                    && ((TextArea) get(desk, "observation"))
                        .getText()
                        .equals("Незавершённое наблюдение"),
                "Незавершённый ввод сохраняется при попытке перехода");
          } finally {
            dispose(w);
          }
          return null;
        });
  }

  private static void collect(List<MenuItem> items, String prefix, List<String> paths) {
    for (var item : items) {
      String path = prefix + item.getText();
      if (item instanceof Menu menu) collect(menu.getItems(), path + " / ", paths);
      else if (!(item instanceof SeparatorMenuItem)) paths.add(path);
    }
  }

  private static MenuItem find(List<MenuItem> items, String path) {
    for (var item : items) {
      if (item.getText().equals(path)) return item;
      if (item instanceof Menu menu && path.startsWith(item.getText() + " / "))
        return find(menu.getItems(), path.substring(item.getText().length() + 3));
    }
    throw new IllegalArgumentException(path);
  }

  private static void check(boolean ok, String label) {
    checks++;
    System.out.println((ok ? "PASS " : "FAIL ") + label);
    if (!ok) failures.add(label);
  }

  private static void dispose(LaboratoryWorkspace w) throws Exception {
    Object page = get(w, "computationPage");
    if (page instanceof LaboratoryComputationPage p) p.dispose();
    ((javafx.animation.PauseTransition) get(w, "viewSave")).stop();
    ((LaboratoryArchiveBrowser) get(w, "archive")).dispose();
  }

  private static Object get(Object target, String name) throws Exception {
    var f = target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(target);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    var f = target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static Object call(Object target, String name) throws Exception {
    var m = target.getClass().getDeclaredMethod(name);
    m.setAccessible(true);
    return m.invoke(target);
  }
}
