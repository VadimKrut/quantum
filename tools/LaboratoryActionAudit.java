/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.OpsinChemicalName;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** End-to-end form actions on synthetic data, without a desktop robot or user files. */
public final class LaboratoryActionAudit {
  private static Path home;
  private static int passed;
  private static final List<String> failures = new ArrayList<>();
  private static final List<LaboratoryComputationPage> pages = new ArrayList<>();

  record Fixture(LaboratoryEditor editor, String reactant, String product, Path folder) {}

  @FunctionalInterface
  interface Scenario {
    void run() throws Exception;
  }

  public static void main(String[] args) throws Exception {
    home = Files.createTempDirectory("quantum-actions-");
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
    ready.get(15, TimeUnit.SECONDS);
    try {
      scenario("name-structure", LaboratoryActionAudit::nameStructure);
      scenario("library", LaboratoryActionAudit::library);
      scenario("cip", LaboratoryActionAudit::cip);
      scenario("conformers", LaboratoryActionAudit::conformers);
      scenario("predicted-nmr", LaboratoryActionAudit::nmr);
      scenario("reaction-classification", LaboratoryActionAudit::classification);
      scenario("literature", LaboratoryActionAudit::literature);
      scenario("procedure", LaboratoryActionAudit::procedure);
      scenario("samples", LaboratoryActionAudit::samples);
      scenario("operations", LaboratoryActionAudit::operations);
      scenario("control-sample", LaboratoryActionAudit::controlSample);
      scenario("repeat-purification", LaboratoryActionAudit::repeatPurification);
      scenario("solutions", LaboratoryActionAudit::solutions);
      scenario("registry", LaboratoryActionAudit::registry);
      scenario("series", LaboratoryActionAudit::series);
      scenario("report", LaboratoryActionAudit::report);
      scenario("calculation", LaboratoryActionAudit::calculation);
      scenario("relative-stereo", LaboratoryActionAudit::relativeStereo);
      scenario("scaffolds", LaboratoryActionAudit::scaffolds);
      scenario("repeat", LaboratoryActionAudit::repeat);
      scenario("structure-search", LaboratoryActionAudit::structureSearch);
      scenario("reaction-search", LaboratoryActionAudit::reactionSearch);
      scenario("overview", LaboratoryActionAudit::overview);
      scenario("route", LaboratoryActionAudit::route);
      scenario("design", LaboratoryActionAudit::design);
      scenario("analyses", LaboratoryActionAudit::analyses);
      scenario("evidence", LaboratoryActionAudit::evidence);
      scenario("transfer", LaboratoryActionAudit::transfer);
      scenario("sample-catalog", LaboratoryActionAudit::catalog);
      scenario("storage", LaboratoryActionAudit::storage);
      scenario("materials", LaboratoryActionAudit::materials);
      scenario("notebook", LaboratoryActionAudit::notebook);
      scenario("desk", LaboratoryActionAudit::desk);
      scenario("action-picker", LaboratoryActionAudit::actionPicker);
    } finally {
      Platform.exit();
    }
    System.out.println(
        "ACTION AUDIT: "
            + passed
            + " scenarios passed; "
            + failures.size()
            + " failures; data="
            + home);
    if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
  }

  private static void scenario(String id, Scenario action) throws Exception {
    try {
      action.run();
      passed++;
      System.out.println("PASS ACTION " + id);
    } catch (Throwable e) {
      failures.add(id + ": " + e);
      System.out.println("FAIL ACTION " + id);
      e.printStackTrace(System.out);
    } finally {
      fx(
          () -> {
            if (!pages.isEmpty()) snapshot(pages.getLast(), id);
            pages.forEach(LaboratoryComputationPage::dispose);
            pages.clear();
            return null;
          });
    }
  }

  private static Fixture fixture(String molecule) throws Exception {
    var editor = new LaboratoryEditor();
    var names = new OpsinChemicalName();
    String a = editor.add(names.parse(molecule).drawing(), ReactionComponentRole.REACTANT);
    // Reproduce the real workflow: product role is assigned in the stage, not globally.
    String b = editor.add(names.parse("ethanal").drawing(), ReactionComponentRole.REACTANT);
    editor.stages(
        List.of(
            new LaboratoryReactionStage(
                "stage1",
                "Учебная стадия",
                LaboratoryReactionStage.Arrow.FORWARD,
                ReactionConditions.EMPTY,
                List.of(
                    new LaboratoryStageParticipant(
                        a, ReactionComponentRole.REACTANT, StoichiometricCoefficient.ONE),
                    new LaboratoryStageParticipant(
                        b, ReactionComponentRole.PRODUCT, StoichiometricCoefficient.ONE)))));
    editor.details(
        "Синтетический опыт для проверки функций",
        ReactionConditions.EMPTY,
        "Не лабораторные измерения");
    editor.quantities(
        a,
        new LaboratoryQuantity(new java.math.BigDecimal("1"), LaboratoryQuantityUnit.MILLIMOLE),
        null,
        LaboratoryEvidence.PLANNED);
    Path directory = Files.createTempDirectory(home, "project-");
    return new Fixture(editor, a, b, directory);
  }

  private static <T extends LaboratoryComputationPage> T show(T page) {
    var stage = new Stage();
    var wrapper = new BorderPane(page);
    wrapper.setId("chemistry-workspace");
    stage.setScene(new Scene(wrapper, 1200, 800));
    wrapper.getStyleClass().addAll("quantum-root", "theme-light");
    stage
        .getScene()
        .getStylesheets()
        .add(
            LaboratoryWorkspace.class
                .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                .toExternalForm());
    page.applyCss();
    page.layout();
    pages.add(page);
    return page;
  }

  private static void nameStructure() throws Exception {
    var f = fixture("ethanol");
    var p = fx(() -> show(new LaboratoryNameStructurePage(f.editor(), () -> {})));
    fx(
        () -> {
          text(p, "input", "ethanol");
          fire(p, "chemical-name-parse");
          return null;
        });
    idle(p);
    fx(
        () -> {
          require(get(p, "result") != null, p.status.getText());
          fire(p, "chemical-name-apply");
          return null;
        });
    require(f.editor().document().experiment().components().size() == 3, "Structure was not added");
    roundTrip(f);
  }

  private static void library() throws Exception {
    var f = fixture("ethanol");
    var library =
        fx(
            () -> {
              var l = new LaboratoryLibraryWindow();
              // No selected drawing and no project folder: the user's own compound must still be
              // savable.
              var node =
                  (Parent)
                      l.view(
                          null,
                          null,
                          null,
                          f.editor().document(),
                          d -> f.editor().add(d, ReactionComponentRole.REACTANT),
                          d -> false,
                          null,
                          d -> {},
                          () -> {});
              new Scene(node, 1200, 800);
              node.applyCss();
              node.layout();
              return l;
            });
    try {
      await(
          () -> {
            var task = (javafx.concurrent.Task<?>) get(library, "loading");
            return task != null && task.isDone();
          });
      fx(
          () -> {
            ((TabPane) get(library, "tabs")).getSelectionModel().select(1);
            snapshot((Parent) get(library, "page"), "library-add");
            text(library, "search", "no matching entries");
            ((ComboBox<?>) get(library, "scope")).getSelectionModel().select(3);
            var save = (Button) get(library, "saveStructure");
            require(
                !save.isDisabled(),
                "Adding own compound is disabled without a drawing selection/project folder");
            save.fire();
            var form = get(library, "entryPage");
            text(form, "name", "Мой этанол — проверка");
            text(form, "tags", "растворитель\nсвой синоним");
            ((Button) get(form, "save")).fire();
            return null;
          });
      await(
          () ->
              !(boolean) get(library, "saving")
                  && get(library, "entryPage") == null
                  && ((javafx.concurrent.Task<?>) get(library, "loading")).isDone());
      fx(
          () -> {
            require(
                ((TabPane) get(library, "tabs")).getSelectionModel().getSelectedIndex() == 0,
                "Library did not return to results");
            require(
                ((TextField) get(library, "search")).getText().isBlank(),
                "Saved entry hidden behind previous search");
            require(
                ((ListView<?>) get(library, "entries")).getSelectionModel().getSelectedItem()
                    != null,
                "Saved entry not selected");
            return null;
          });
      for (String kind : List.of("Template", "Fragment")) {
        fx(
            () -> {
              ((Button) get(library, "save" + kind)).fire();
              var form = get(library, "entryPage");
              text(form, "name", "Учебный " + kind);
              if (kind.equals("Fragment")) {
                ((ComboBox<?>) get(form, "atom")).getSelectionModel().selectFirst();
                invoke(form, "point", new Class<?>[] {boolean.class}, false);
              }
              ((Button) get(form, "save")).fire();
              return null;
            });
        await(
            () ->
                !(boolean) get(library, "saving")
                    && get(library, "entryPage") == null
                    && ((javafx.concurrent.Task<?>) get(library, "loading")).isDone());
      }
      fx(
          () -> {
            text(library, "search", "свой синоним");
            var list = (ListView<?>) get(library, "entries");
            require(
                list.getItems().size() == 1,
                "Own entry not found by synonym: " + ((Label) get(library, "status")).getText());
            var insert = (Button) get(library, "insertButton");
            require(!insert.isDisabled(), "Saved compound cannot be inserted");
            insert.fire();
            return null;
          });
      require(
          f.editor().document().experiment().components().size() == 3,
          "Saved compound was not inserted");
      var saved =
          new ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.filesystem
                  .ChemistryLibraryFileRepository(
                  home.resolve("Documents/Quantum IR Studio/.chemistry-library"))
              .scan();
      require(
          saved.entries().stream().anyMatch(e -> e.displayName().equals("Мой этанол — проверка")),
          "Own entry was not persisted");
      require(
          saved.entries().stream().anyMatch(e -> e.displayName().equals("Учебный Template"))
              && saved.entries().stream().anyMatch(e -> e.displayName().equals("Учебный Fragment")),
          "Own fragment/template not persisted");
      fx(
          () -> {
            snapshot((Parent) get(library, "page"), "library");
            return null;
          });
      roundTrip(f);
    } finally {
      fx(
          () -> {
            library.dispose();
            return null;
          });
    }
  }

  private static void cip() throws Exception {
    for (String name : List.of("(2R)-butan-2-ol", "(E)-but-2-ene")) {
      var f = fixture(name);
      var p = fx(() -> show(new LaboratoryCipPage(f.editor(), f.reactant(), () -> {})));
      fx(
          () -> {
            fire(p, "cip-calculate");
            return null;
          });
      idle(p);
      fx(
          () -> {
            var labels = (ListView<?>) get(p, "labels");
            require(
                !labels.getItems().isEmpty(),
                name + ": " + p.status.getText() + " " + ((Label) get(p, "warnings")).getText());
            require(
                labels.getItems().toString().contains(name.contains("2R") ? "R" : "E"),
                "Wrong descriptor: " + labels.getItems());
            fire(p, "cip-apply");
            return null;
          });
      roundTrip(f);
    }
    var plain = fixture("ethanol");
    var empty = fx(() -> show(new LaboratoryCipPage(plain.editor(), plain.reactant(), () -> {})));
    fx(
        () -> {
          fire(empty, "cip-calculate");
          return null;
        });
    idle(empty);
    fx(
        () -> {
          require(
              ((ListView<?>) get(empty, "labels")).getItems().isEmpty()
                  && empty.status.getText().contains("без дескрипторов"),
              "Empty CIP result has no explanation");
          return null;
        });
  }

  private static void conformers() throws Exception {
    var f = fixture("butane");
    var p =
        fx(
            () ->
                show(
                    new LaboratoryConformerPage(
                        f.editor(),
                        f.reactant(),
                        f.folder().resolve("conformers"),
                        null,
                        () -> {})));
    fx(
        () -> {
          text(p, "count", "3");
          fire(p, "conformer-generate");
          return null;
        });
    idle(p);
    fx(
        () -> {
          require(!((List<?>) get(p, "frames")).isEmpty(), p.status.getText());
          call(p, "save");
          require(get(p, "saved") != null, p.status.getText());
          call(p, "apply");
          return null;
        });
    roundTrip(f);
  }

  private static void nmr() throws Exception {
    var f = fixture("ethanol");
    var p = fx(() -> show(new LaboratoryNmrPredictionPage(f.editor(), f.reactant(), () -> {})));
    for (int nucleus : List.of(0, 1)) {
      fx(
          () -> {
            ((ComboBox<?>) get(p, "nucleus")).getSelectionModel().select(nucleus);
            fire(p, "nmr-model-calculate");
            return null;
          });
      idle(p);
      fx(
          () -> {
            require(!((List<?>) get(p, "calculated")).isEmpty(), p.status.getText());
            require(get(p, "simulation") != null, "No NMR simulation");
            require(!((Button) get(p, "saveCsv")).isDisabled(), "NMR result cannot be exported");
            return null;
          });
    }
  }

  private static void classification() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(() -> show(new LaboratoryReactionClassificationPage(f.editor(), "stage1", () -> {})));
    fx(
        () -> {
          text(p, "customText", "Учебное превращение");
          ((Button) get(p, "custom")).fire();
          ((Button) get(p, "apply")).fire();
          return null;
        });
    require(
        !f.editor()
            .document()
            .experiment()
            .stages()
            .getFirst()
            .classification()
            .equals(LaboratoryReactionClassification.EMPTY),
        "Reaction classification not saved");
    roundTrip(f);
  }

  private static void literature() throws Exception {
    var f = fixture("ethanol");
    var p = fx(() -> show(new LaboratoryLiteraturePage(f.editor(), "stage1", () -> {}, () -> {})));
    fx(
        () -> {
          text(p, "title", "Учебный источник");
          text(p, "authors", "Проверка");
          text(p, "year", "2026");
          fire(p, "literature-save-source");
          return null;
        });
    require(
        f.editor().document().experiment().literature().references().size() == 1,
        "Source not saved: " + fx(() -> p.status.getText()));
    fx(
        () -> {
          fire(p, "literature-next-method");
          text(p, "methodTitle", "Учебная методика");
          text(p, "procedure", "Добавить растворитель. Перемешать.");
          fire(p, "literature-save-method");
          return null;
        });
    require(
        f.editor().document().experiment().literature().methods().size() == 1,
        "Method not saved: " + fx(() -> p.status.getText()));
    fx(
        () -> {
          fire(p, "literature-reuse-method");
          return null;
        });
    require(
        !f.editor().document().experiment().procedure().steps().isEmpty(),
        "Method not added to plan: " + fx(() -> p.status.getText()));
    roundTrip(f);
  }

  private static void procedure() throws Exception {
    var f = fixture("ethanol");
    var p = fx(() -> show(new LaboratoryProcedurePage(f.editor(), "stage1", () -> {}, () -> {})));
    fx(
        () -> {
          fire(p, "procedure-new");
          text(p, "title", "Перемешивание");
          text(p, "instruction", "Учебный шаг");
          fire(p, "procedure-apply");
          return null;
        });
    require(
        f.editor().document().experiment().procedure().steps().size() == 1,
        "Step not saved: " + fx(() -> p.status.getText()));
    fx(
        () -> {
          fire(p, "procedure-start");
          fire(p, "procedure-complete");
          return null;
        });
    require(
        f.editor().document().experiment().procedure().events().size() >= 2,
        "Step execution not recorded: " + fx(() -> p.status.getText()));
    roundTrip(f);
  }

  private static void samples() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(
            () ->
                show(
                    new LaboratorySamplePage(
                        f.editor(), "stage1", f.product(), () -> true, () -> {})));
    fx(
        () -> {
          selectId((ComboBox<?>) get(p, "component"), f.product());
          @SuppressWarnings("unchecked")
          var kind = (ComboBox<LaboratorySample.Kind>) get(p, "kind");
          kind.setValue(LaboratorySample.Kind.ISOLATED);
          @SuppressWarnings("unchecked")
          var fields = (Map<String, TextField>) get(p, "fields");
          fields.get("code").setText("S1");
          fields.get("quantity").setText("100");
          fire(p, "chemistry-sample-preview");
          fire(p, "chemistry-sample-apply");
          return null;
        });
    require(
        f.editor().document().experiment().samples().size() == 1,
        "Sample not registered: " + fx(() -> p.status.getText()));
    fx(
        () -> {
          var operation = Class.forName(LaboratorySamplePage.class.getName() + "$Operation");
          @SuppressWarnings({"unchecked", "rawtypes"})
          Object product = Enum.valueOf((Class) operation, "PRODUCT");
          var begin = LaboratorySamplePage.class.getDeclaredMethod("begin", operation);
          begin.setAccessible(true);
          begin.invoke(p, product);
          fire(p, "chemistry-sample-apply");
          return null;
        });
    require(
        f.editor().document().experiment().components().stream()
                .filter(c -> c.id().equals(f.product()))
                .findFirst()
                .orElseThrow()
                .actual()
            != null,
        "Stage product sample cannot become actual yield: " + fx(() -> p.status.getText()));
    roundTrip(f);
    sampleOperations();
  }

  private static void sampleOperations() throws Exception {
    for (String operation :
        List.of(
            "ALIQUOT",
            "FRACTIONS",
            "ISOLATE",
            "CONSUME",
            "MEASURE",
            "DETAILS",
            "FINISH",
            "ARCHIVE",
            "DISCARD",
            "RESTORE",
            "POOL")) {
      var f = sampleFixture();
      var p =
          fx(() -> show(new LaboratorySamplePage(f.editor(), null, null, () -> true, () -> {})));
      fx(
          () -> {
            @SuppressWarnings("unchecked")
            var table = (TableView<LaboratorySample>) get(p, "table");
            table.getSelectionModel().selectFirst();
            if (operation.equals("RESTORE")) {
              sampleOperation(p, "ARCHIVE", "", "", "");
              ((CheckBox) get(p, "inactive")).setSelected(true);
              table.getSelectionModel().selectFirst();
            }
            if (operation.equals("POOL")) {
              sampleOperation(p, "ALIQUOT", "25", "A1", "");
              table.getSelectionModel().clearSelection();
              table.getSelectionModel().selectFirst();
              sampleOperation(p, "ALIQUOT", "25", "A2", "");
              table.getSelectionModel().clearSelection();
              for (var s : table.getItems())
                if (s.code().startsWith("A")) table.getSelectionModel().select(s);
            }
            var before = f.editor().document();
            sampleOperation(
                p,
                operation,
                operation.equals("MEASURE") ? "90" : "10",
                "NEW",
                operation.equals("FRACTIONS") ? "2" : "");
            require(!before.equals(f.editor().document()), operation + ": " + p.status.getText());
            return null;
          });
      roundTrip(f);
      System.out.println("PASS SUBACTION samples/" + operation);
    }
  }

  private static void sampleOperation(
      LaboratorySamplePage p, String name, String amount, String code, String count)
      throws Exception {
    var type = Class.forName(LaboratorySamplePage.class.getName() + "$Operation");
    @SuppressWarnings({"unchecked", "rawtypes"})
    var op = Enum.valueOf((Class) type, name);
    invoke(p, "begin", new Class<?>[] {type}, op);
    @SuppressWarnings("unchecked")
    var fields = (Map<String, TextField>) get(p, "fields");
    if (!amount.isEmpty()) fields.get("quantity").setText(amount);
    if (!code.isEmpty()) fields.get("code").setText(code);
    if (!count.isEmpty()) fields.get("count").setText(count);
    text(p, "note", "Синтетическая операция проверки");
    if (name.equals("DETAILS")) fields.get("location").setText("Учебный шкаф");
    call(p, "preview");
    call(p, "apply");
    require(get(p, "operation") == null, name + ": " + p.status.getText());
  }

  private static void controlSample() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(() -> show(new LaboratoryOperationsPage(f.editor(), "stage1", () -> true, () -> {})));
    fx(
        () -> {
          p.openAction("sample");
          text(p, "pointLabel", "Учебная проба");
          text(p, "elapsed", "30");
          text(p, "conversion", "50");
          text(p, "pointSource", "Синтетические данные проверки");
          ((ComboBox<?>) get(get(p, "pointObservation"), "origin")).getSelectionModel().select(3);
          call(p, "commitPoint");
          return null;
        });
    require(
        f.editor().document().experiment().operations().timePoints().size() == 1,
        "Control sample not saved: " + fx(() -> p.status.getText()));
    roundTrip(f);
  }

  private static Fixture savedPurification() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(() -> show(new LaboratoryOperationsPage(f.editor(), "stage1", () -> true, () -> {})));
    fx(
        () -> {
          p.openAction("purification");
          text(p, "runTitle", "Учебная очистка");
          text(p, "solventA", "Этилацетат");
          text(p, "stationary", "Силикагель");
          call(p, "commitRun");
          return null;
        });
    require(
        f.editor().document().experiment().operations().purifications().size() == 1,
        "Purification not saved: " + fx(() -> p.status.getText()));
    return f;
  }

  private static void operations() throws Exception {
    var f = savedPurification();
    var p =
        fx(() -> show(new LaboratoryOperationsPage(f.editor(), "stage1", () -> true, () -> {})));
    fx(
        () -> {
          text(p, "workupTitle", "Учебная экстракция");
          text(p, "workupNote", "Синтетическая методика обработки");
          call(p, "commitWorkup");
          return null;
        });
    require(
        f.editor().document().experiment().operations().workups().size() == 1,
        "Workup not recorded: " + fx(() -> p.status.getText()));
    fx(
        () -> {
          text(p, "workupNote", "Уточнение учебной обработки");
          call(p, "commitWorkup");
          return null;
        });
    require(
        f.editor().document().experiment().operations().workups().size() == 2,
        "Workup correction overwrites history");
    roundTrip(f);
  }

  private static void repeatPurification() throws Exception {
    var f = savedPurification();
    var p =
        fx(() -> show(new LaboratoryOperationsPage(f.editor(), "stage1", () -> true, () -> {})));
    fx(
        () -> {
          ((ListView<?>) get(p, "runs")).getSelectionModel().selectFirst();
          p.openAction("repeat-purification");
          call(p, "commitRun");
          return null;
        });
    require(
        f.editor().document().experiment().operations().purifications().size() == 2,
        "Repeated purification not saved: " + fx(() -> p.status.getText()));
    roundTrip(f);
  }

  private static void solutions() throws Exception {
    var f = materialFixture();
    var p = fx(() -> show(new LaboratorySolutionPage(f.editor(), f.folder(), () -> {})));
    idle(p);
    fx(
        () -> {
          text(p, "title", "Учебный раствор");
          text(p, "solvent", "Вода");
          text(p, "target", "0.1");
          text(p, "molarMass", "46.07");
          text(p, "purity", "100");
          text(get(p, "volume"), "value", "100");
          call(p, "calculate");
          return null;
        });
    fx(
        () -> {
          require(!((TableView<?>) get(p, "calculation")).getItems().isEmpty(), p.status.getText());
          var row = ((TableView<?>) get(p, "calculation")).getItems().getFirst();
          var take = (LaboratoryQuantity) call(row, "take");
          require(
              take.in(LaboratoryQuantityUnit.GRAM)
                      .value()
                      .compareTo(new java.math.BigDecimal("0.4607"))
                  == 0,
              "Wrong solution mass");
          call(p, "saveRecipe");
          return null;
        });
    idle(p);
    fx(
        () -> {
          require(!((TableView<?>) get(p, "recipes")).getItems().isEmpty(), p.status.getText());
          call(p, "post");
          var input = get(p, "preparationInput");
          ((ComboBox<?>) get(input, "source")).getSelectionModel().selectFirst();
          text(((List<?>) get(input, "taken")).getFirst(), "value", "460.7");
          text(((List<?>) get(input, "volumes")).getFirst(), "value", "100");
          text(input, "reason", "Синтетическое приготовление для проверки");
          call(p, "postActual");
          return null;
        });
    idle(p);
    var stored = new LaboratoryInventoryRepository().read(f.folder().resolve("materials.qchem"));
    require(
        stored.inventory().batches().size() == 2,
        "Prepared solution not recorded: " + fx(() -> p.status.getText()));
  }

  private static void registry() throws Exception {
    var f = fixture("ethanol");
    var p = fx(() -> show(new LaboratoryRegistryPage(f.editor(), f.folder(), null, () -> {})));
    idle(p);
    fx(
        () -> {
          p.forComponent(f.reactant());
          text(p, "registrationName", "Учебный этанол");
          text(p, "registrationSource", "Синтетический пример");
          call(p, "register");
          return null;
        });
    idle(p);
    fx(
        () -> {
          require(((ListView<?>) get(p, "compounds")).getItems().size() == 1, p.status.getText());
          fire(p, "registry-use-card");
          return null;
        });
    require(
        f.editor().document().experiment().components().size() == 3, "Registry entry not inserted");
    roundTrip(f);
  }

  private static void series() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(
            () ->
                show(
                    new LaboratorySeriesPage(
                        f.editor().document(), f.folder(), path -> {}, () -> {})));
    fx(
        () -> {
          text(p, "folder", "Учебная серия");
          require(!((Button) get(p, "create")).isDisabled(), ((Label) get(p, "summary")).getText());
          fire(p, "chemistry-series-create");
          return null;
        });
    idle(p);
    Path result = fx(() -> (Path) get(p, "result"));
    require(result != null, "Series not created: " + fx(() -> p.status.getText()));
    try (var files = Files.walk(result)) {
      var experiments = files.filter(v -> v.toString().endsWith(".qchem")).toList();
      require(!experiments.isEmpty(), "No series files");
      for (var file : experiments) new LaboratoryFileRepository().read(file);
    }
  }

  private static void report() throws Exception {
    var f = fixture("ethanol");
    var p = fx(() -> show(new LaboratoryReportPage(f.editor().document(), () -> {})));
    idle(p);
    var bundle = fx(() -> (LaboratoryReportFiles.Bundle) get(p, "bundle"));
    require(bundle != null, "No report: " + fx(() -> p.status.getText()));
    require(
        bundle.html().contains("Учебная стадия") && bundle.pdf().length > 1000,
        "Incomplete report");
    try (var pdf = org.apache.pdfbox.Loader.loadPDF(bundle.pdf())) {
      require(
          new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).contains("Учебная стадия"),
          "PDF content missing");
    }
  }

  private static void calculation() throws Exception {
    var f = fixture("ethanol");
    var result = new AtomicReference<LaboratoryCalculationJob.Result>();
    var p =
        fx(
            () ->
                show(
                    new LaboratoryCalculationWindow(
                        f.editor().document().experiment().components().getFirst(),
                        f.editor().document().experiment().id(),
                        f.folder(),
                        result::set,
                        m -> {},
                        ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                            .ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
                        () -> {})));
    fx(
        () -> {
          fire(p, "chemistry-calculation-start");
          return null;
        });
    idle(p);
    require(
        result.get() != null && Files.isRegularFile(result.get().file()),
        "Calculation not archived: " + fx(() -> p.status.getText()));
    require(
        fx(() -> ((TextArea) get(p, "report")).getText()).length() > 100,
        "Empty descriptor report");
  }

  private static void relativeStereo() throws Exception {
    var f = fixture("(2R,3R)-butane-2,3-diol");
    var p =
        fx(() -> show(new LaboratoryRelativeStereoPage(f.editor(), f.reactant(), null, () -> {})));
    fx(
        () -> {
          text(p, "source", "Ручное учебное назначение");
          ((ComboBox<?>) get(p, "atom")).getSelectionModel().select(1);
          fire(p, "relative-member-add");
          ((ComboBox<?>) get(p, "atom")).getSelectionModel().select(2);
          fire(p, "relative-member-add");
          fire(p, "relative-save");
          return null;
        });
    require(
        fx(() -> ((ListView<?>) get(p, "groups")).getItems().size()) == 1,
        "Relative stereo not saved: " + fx(() -> p.status.getText()));
    roundTrip(f);
  }

  private static void scaffolds() throws Exception {
    var f = fixture("ethanol");
    var result = new AtomicReference<MoleculeDrawing>();
    var p =
        fx(
            () ->
                show(
                    new LaboratoryScaffoldPage(
                        f.editor().document(), null, null, result::set, () -> {})));
    idle(p);
    fx(
        () -> {
          ((ListView<?>) get(p, "palette")).getSelectionModel().selectFirst();
          return null;
        });
    idle(p);
    fx(
        () -> {
          fire(p, "chemistry-scaffold-apply");
          return null;
        });
    require(
        result.get() != null && result.get().molecule().atoms().size() > 3,
        "Scaffold was not built");
    f.editor().add(result.get(), ReactionComponentRole.REACTANT);
    roundTrip(f);
  }

  private static void repeat() throws Exception {
    var f = fixture("ethanol");
    String before = f.editor().document().experiment().id();
    f.editor().repeat();
    require(!f.editor().document().experiment().id().equals(before), "Repeat reuses experiment ID");
    require(f.editor().document().experiment().stages().size() == 1, "Repeat loses stages");
    roundTrip(f);
  }

  private static void selectId(ComboBox<?> box, String id) throws Exception {
    for (int i = 0; i < box.getItems().size(); i++) {
      Object value = box.getItems().get(i);
      var m = value.getClass().getDeclaredMethod("id");
      m.setAccessible(true);
      if (id.equals(m.invoke(value))) {
        box.getSelectionModel().select(i);
        return;
      }
    }
    throw new AssertionError("Choice not found: " + id);
  }

  private static void structureSearch() throws Exception {
    var f = fixture("ethanol");
    roundTrip(f);
    var opened = new AtomicReference<Path>();
    var p =
        fx(
            () ->
                show(
                    new LaboratoryStructureSearchPage(
                        f.editor().document(),
                        f.reactant(),
                        Set.of(),
                        f.folder(),
                        (path, id) -> opened.set(path),
                        (path, snapshot) -> {},
                        () -> {})));
    fx(
        () -> {
          fire(p, "chemistry-search-run");
          return null;
        });
    idle(p);
    fx(
        () -> {
          var table = (TableView<?>) get(p, "table");
          require(!table.getItems().isEmpty(), p.status.getText());
          table.getSelectionModel().selectFirst();
          fire(p, "chemistry-search-open");
          return null;
        });
    require(opened.get() != null, "Found structure cannot open its experience");
  }

  private static void reactionSearch() throws Exception {
    var f = fixture("ethanol");
    roundTrip(f);
    var opened = new AtomicReference<Path>();
    var p =
        fx(
            () ->
                show(
                    new LaboratoryReactionSearchPage(
                        f.editor().document(),
                        f.folder(),
                        (path, id) -> opened.set(path),
                        path -> {},
                        () -> {})));
    fx(
        () -> {
          fire(p, "reaction-search-run");
          return null;
        });
    idle(p);
    fx(
        () -> {
          var table = (ListView<?>) get(p, "results");
          require(!table.getItems().isEmpty(), p.status.getText());
          table.getSelectionModel().selectFirst();
          fire(p, "reaction-search-open");
          return null;
        });
    require(opened.get() != null, "Reaction cannot be opened");
  }

  private static void overview() throws Exception {
    var f = fixture("ethanol");
    roundTrip(f);
    var opened = new AtomicReference<Path>();
    var p = fx(() -> show(new LaboratoryOverviewPage(f.folder(), opened::set, () -> {})));
    idle(p);
    fx(
        () -> {
          var table = (TableView<?>) get(p, "table");
          require(!table.getItems().isEmpty(), p.status.getText());
          table.getSelectionModel().selectFirst();
          fire(p, "chemistry-overview-open");
          return null;
        });
    require(opened.get() != null, "Overview cannot open experience");
  }

  private static void route() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(() -> show(new LaboratoryRoutePage(f.editor(), "stage1", () -> {}, id -> {}, () -> {})));
    fx(
        () -> {
          text(p, "yield", "80");
          call(p, "putNode");
          text(p, "amount", "1");
          @SuppressWarnings("unchecked")
          var unit = (ComboBox<LaboratoryQuantityUnit>) get(p, "unit");
          unit.setValue(LaboratoryQuantityUnit.MILLIMOLE);
          call(p, "calculate");
          return null;
        });
    idle(p);
    fx(
        () -> {
          require(
              get(p, "calculated") != null,
              p.status.getText() + " " + ((Label) get(p, "planSummary")).getText());
          call(p, "applyPlan");
          return null;
        });
    require(!f.editor().document().experiment().route().nodes().isEmpty(), "Route not saved");
    roundTrip(f);
  }

  private static void design() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(
            () ->
                show(
                    new LaboratoryDesignPage(
                        f.editor().document(), f.folder(), path -> {}, () -> {})));
    fx(
        () -> {
          text(p, "title", "Учебное сравнение температур");
          text(p, "folder", "Температуры");
          fire(p, "chemistry-design-factor-add");
          fire(p, "chemistry-design-generate");
          fire(p, "chemistry-design-review");
          return null;
        });
    idle(p);
    fx(
        () -> {
          require(!((List<?>) get(p, "prepared")).isEmpty(), p.status.getText());
          fire(p, "chemistry-design-create");
          return null;
        });
    idle(p);
    Path path = fx(() -> (Path) get(p, "file"));
    require(
        path != null && Files.isRegularFile(path),
        "Design not persisted: " + fx(() -> p.status.getText()));
  }

  private static void analyses() throws Exception {
    for (String method : List.of("NMR", "MS", "TLC")) {
      var f = fixture("ethanol");
      var p = fx(() -> show(new LaboratoryAnalysisPage(f.editor(), f.reactant(), () -> {})));
      fx(
          () -> {
            invoke(p, "begin", new Class<?>[] {String.class}, method);
            ((ComboBox<?>) get(p, "origin")).getSelectionModel().select(2);
            text(p, "source", "Синтетические данные проверки");
            @SuppressWarnings("unchecked")
            var fields = (Map<String, TextField>) get(p, "fields");
            if (method.equals("MS")) {
              fields.get("found").setText("47.0491");
              fields.get("ionText").setText("[M+H]+");
              call(p, "calculateMass");
            } else {
              fire(p, "chemistry-analysis-add-row");
              var row = ((TableView<?>) get(p, "rows")).getItems().getFirst();
              @SuppressWarnings("unchecked")
              var cells = (List<javafx.beans.property.StringProperty>) get(row, "fields");
              if (method.equals("NMR")) {
                fields.get("frequency").setText("400");
                fields.get("solvent").setText("CDCl3");
                cells.get(0).set("1.2");
                cells.get(1).set("1.2");
                cells.get(2).set("t");
                cells.get(3).set("3");
                cells.get(4).set("7.0");
              } else {
                fields.get("front").setText("50");
                fields.get("eluent").setText("Гексан / этилацетат 1:1");
                cells.get(0).set("Вещество");
                cells.get(1).set("20");
              }
            }
            fire(p, "chemistry-analysis-check");
            require(
                !((Button) get(p, "apply")).isDisabled(),
                p.status.getText() + " " + ((Label) get(p, "feedback")).getText());
            fire(p, "chemistry-analysis-apply");
            return null;
          });
      require(
          f.editor().document().experiment().analyses().size() == 1,
          method + " analysis not saved");
      roundTrip(f);
    }
  }

  private static Object invoke(Object target, String name, Class<?>[] types, Object... values)
      throws Exception {
    var m = target.getClass().getDeclaredMethod(name, types);
    m.setAccessible(true);
    return m.invoke(target, values);
  }

  private static void evidence() throws Exception {
    for (String method : List.of("CORRELATIONS", "MULTIPLETS", "QNMR", "CHROM", "DOSSIER")) {
      var f = fixture("ethanol");
      var p = fx(() -> show(new LaboratoryEvidencePage(f.editor(), f.reactant(), null, () -> {})));
      fx(
          () -> {
            var type = Class.forName(LaboratoryEvidencePage.class.getName() + "$Method");
            @SuppressWarnings({"unchecked", "rawtypes"})
            var value = Enum.valueOf((Class) type, method);
            invoke(p, "begin", new Class<?>[] {type}, value);
            ((ComboBox<?>) get(p, "origin")).getSelectionModel().select(2);
            text(p, "source", "Синтетические данные проверки");
            @SuppressWarnings("unchecked")
            var fields = (Map<String, TextField>) get(p, "fields");
            switch (method) {
              case "CORRELATIONS" ->
                  ((LaboratoryEvidenceTable) get(p, "correlations"))
                      .add("signal1", List.of("1.2", "20", "1", "", "", "Учебная корреляция"));
              case "MULTIPLETS" -> {
                fields.get("frequency").setText("400");
                ((LaboratoryEvidenceTable) get(p, "multiplets"))
                    .add(
                        "signal1",
                        List.of(
                            "1.2", "3", "t", "1.1825;1.2;1.2175", "7", "", "Учебный мультиплет"));
              }
              case "QNMR" -> {
                text(p, "preparation", "Синтетический раствор для проверки формулы");
                for (var entry :
                    Map.ofEntries(
                            Map.entry("standard", "Учебный стандарт"),
                            Map.entry("sampleIntegral", "1"),
                            Map.entry("sampleNuclei", "1"),
                            Map.entry("sampleMolarMass", "46.07"),
                            Map.entry("sampleMass", "46.07"),
                            Map.entry("standardIntegral", "1"),
                            Map.entry("standardNuclei", "1"),
                            Map.entry("standardMolarMass", "100"),
                            Map.entry("standardMass", "100"),
                            Map.entry("standardPurity", "100"))
                        .entrySet()) fields.get(entry.getKey()).setText(entry.getValue());
              }
              case "CHROM" -> {
                fields.get("detector").setText("UV");
                ((LaboratoryEvidenceTable) get(p, "peaks"))
                    .add("peak1", List.of("Учебный пик", "1.5", "1", "2", "100", "да", ""));
              }
              case "DOSSIER" ->
                  text(
                      p,
                      "conclusionText",
                      "Учебная сводка: данных недостаточно для подтверждения.");
            }
            call(p, "preview");
            require(get(p, "prepared") != null, p.status.getText());
            call(p, "apply");
            return null;
          });
      require(
          f.editor().document().experiment().analyticalEvidence().entries().size() == 1,
          method + " not saved");
      roundTrip(f);
      System.out.println("PASS SUBACTION evidence/" + method);
    }
  }

  private static Fixture sampleFixture() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(
            () ->
                show(
                    new LaboratorySamplePage(
                        f.editor(), "stage1", f.product(), () -> true, () -> {})));
    fx(
        () -> {
          @SuppressWarnings("unchecked")
          var fields = (Map<String, TextField>) get(p, "fields");
          fields.get("code").setText("S1");
          fields.get("quantity").setText("100");
          fire(p, "chemistry-sample-preview");
          fire(p, "chemistry-sample-apply");
          return null;
        });
    require(f.editor().document().experiment().samples().size() == 1, p.status.getText());
    roundTrip(f);
    return f;
  }

  private static void transfer() throws Exception {
    var f = sampleFixture();
    Path source = f.folder().resolve("verified.qchem"),
        target = f.folder().resolve("received.qchem");
    f.editor().open(new LaboratoryFileRepository().read(source).drawing());
    var p =
        fx(
            () ->
                show(
                    new LaboratorySampleTransferPage(
                        f.editor(), source, null, path -> {}, () -> {})));
    idle(p);
    fx(
        () -> {
          ((ComboBox<?>) get(p, "sample")).getSelectionModel().selectFirst();
          text(p, "amount", "25");
          text(p, "title", "Следующий учебный опыт");
          set(p, "targetPath", target);
          call(p, "preview");
          call(p, "perform");
          return null;
        });
    idle(p);
    require(
        Files.isRegularFile(target), "Transfer target missing: " + fx(() -> p.status.getText()));
    var repository = new LaboratoryFileRepository();
    var received = repository.read(target).drawing().experiment().samples().getFirst();
    var left = repository.read(source).drawing().experiment().samples().getFirst();
    require(
        received.balance().remaining().value().compareTo(new java.math.BigDecimal("25")) == 0
            && left.balance().remaining().value().compareTo(new java.math.BigDecimal("75")) == 0,
        "Transfer balances differ");
  }

  private static void catalog() throws Exception {
    var f = sampleFixture();
    var opened = new AtomicReference<String>();
    var p =
        fx(
            () ->
                show(
                    new LaboratorySampleCatalogPage(
                        f.folder(),
                        () ->
                            new LaboratorySampleCatalog.Current(
                                f.folder().resolve("verified.qchem"), f.editor().document(), false),
                        (row, action) -> opened.set(action),
                        () -> {})));
    idle(p);
    fx(
        () -> {
          var table = (TableView<?>) get(p, "table");
          require(table.getItems().size() == 1, p.status.getText());
          text(p, "search", "S1");
          require(table.getItems().size() == 1, "Sample cannot be found by code");
          return null;
        });
  }

  private static void storage() throws Exception {
    var f = sampleFixture();
    var repo = LaboratoryWorkbenchBooks.storage(f.folder());
    var before = repo.read();
    var place =
        new ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStorageMap.Place(
            "cabinet1",
            null,
            ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStorageMap.Kind
                .CABINET,
            "Учебный шкаф",
            0,
            0);
    repo.write(before.value().append(place), before.fingerprint());
    var p =
        fx(
            () ->
                show(
                    new LaboratoryStoragePage(
                        f.editor(), f.folder(), (path, id) -> {}, () -> {}, () -> {})));
    idle(p);
    fx(
        () -> {
          var table = (TableView<?>) get(p, "table");
          require(!table.getItems().isEmpty(), p.status.getText());
          table.getSelectionModel().selectFirst();
          text(p, "reason", "Учебное перемещение");
          var row = table.getSelectionModel().getSelectedItem();
          var slot =
              new ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStorageMap.Slot(
                  "cabinet1", 0, 0);
          invoke(p, "relocate", new Class<?>[] {row.getClass(), slot.getClass()}, row, slot);
          return null;
        });
    require(
        f.editor()
            .document()
            .experiment()
            .samples()
            .getFirst()
            .balance()
            .location()
            .contains("Учебный шкаф"),
        "Location not recorded");
    roundTrip(f);
  }

  private static void materials() throws Exception {
    materialFixture();
  }

  private static Fixture materialFixture() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(
            () -> {
              var page =
                  new LaboratoryInventoryWindow(
                      f.folder(),
                      f.editor().document().experiment().id(),
                      operation -> {},
                      f.editor().document().experiment().components());
              var node =
                  (Parent)
                      page.view(
                          null,
                          f.editor().document().experiment().components().getFirst().molecule(),
                          "Учебный опыт",
                          () -> {});
              new Scene(node, 1200, 800);
              node.applyCss();
              node.layout();
              return page;
            });
    try {
      await(() -> (boolean) get(p, "loaded") && !(boolean) get(p, "loading"));
      fx(
          () -> {
            var root = (Parent) get(p, "page");
            findButton(root, "Приход / продукт…").fire();
            root.applyCss();
            root.layout();
            ((TextField) root.lookup("#chemistry-stock-material-name")).setText("Учебный этанол");
            ((TextField) root.lookup("#chemistry-stock-batch-label")).setText("Batch1");
            ((TextField) root.lookup("#chemistry-stock-receipt-amount")).setText("10");
            findText(root, "Основание прихода").setText("Синтетический приход для проверки");
            fire(root, "chemistry-stock-post");
            return null;
          });
      await(() -> !(boolean) get(p, "saving"));
      fx(
          () -> {
            require(
                ((TableView<?>) get(p, "batches")).getItems().size() == 1,
                ((Label) get(p, "message")).getText());
            return null;
          });
      require(
          Files.isRegularFile(f.folder().resolve("materials.qchem")), "Inventory not persisted");
      for (var kind :
          List.of(
              ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation.Kind
                  .USE,
              ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation.Kind
                  .RETURN,
              ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation.Kind
                  .INCREASE,
              ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation.Kind
                  .DECREASE)) {
        fx(
            () -> {
              var root = (Parent) get(p, "page");
              findButton(root, kind + "…").fire();
              root.applyCss();
              root.layout();
              ((TextField) root.lookup("#chemistry-stock-operation-amount"))
                  .setText(kind.name().equals("USE") ? "2" : "1");
              findText(root, "Основание операции / стадия опыта").setText("Синтетическая операция");
              if (kind.name().equals("RETURN"))
                descendants(root).stream()
                    .filter(
                        n ->
                            n instanceof ComboBox<?> box
                                && !box.getItems().isEmpty()
                                && box.getItems().getFirst()
                                    instanceof
                                    ru.pathcreator.vadim.quantum.chemistry.domain.inventory
                                        .LaboratoryStockOperation)
                    .map(n -> (ComboBox<?>) n)
                    .findFirst()
                    .orElseThrow()
                    .getSelectionModel()
                    .selectFirst();
              fire(root, "chemistry-stock-post");
              return null;
            });
        await(() -> !(boolean) get(p, "saving"));
        fx(
            () -> {
              require(get(p, "form") == null, "Inventory operation did not finish: " + kind);
              return null;
            });
      }
      var saved = new LaboratoryInventoryRepository().read(f.folder().resolve("materials.qchem"));
      require(
          saved
                  .inventory()
                  .balance(saved.inventory().batches().getFirst())
                  .value()
                  .compareTo(new java.math.BigDecimal("9"))
              == 0,
          "Stock balance must be 9 g after receipt/use/return/corrections");
      return f;
    } finally {
      fx(
          () -> {
            call(p, "dispose");
            return null;
          });
    }
  }

  private static void notebook() throws Exception {
    var repo = new LaboratoryNotebookRepository(home.resolve("notebook"));
    var p = fx(() -> show(new LaboratoryNotebookPage(repo, path -> {}, () -> {})));
    idle(p);
    fx(
        () -> {
          text(p, "projectName", "Учебный проект");
          fire(p, "notebook-create-project");
          return null;
        });
    idle(p);
    fx(
        () -> {
          fire(p, "notebook-new-section");
          text(p, "sectionName", "Учебная серия");
          fire(p, "notebook-create-section");
          return null;
        });
    idle(p);
    fx(
        () -> {
          text(p, "experimentName", "Учебный опыт");
          call(p, "createExperiment");
          return null;
        });
    idle(p);
    var snapshot = fx(() -> (LaboratoryNotebookRepository.Snapshot) get(p, "snapshot"));
    require(
        snapshot.items().size() == 1, "Experience not created: " + fx(() -> p.status.getText()));
    require(repo.load(snapshot.project()).items().size() == 1, "Notebook not persisted");
  }

  private static void desk() throws Exception {
    var f = fixture("ethanol");
    var p =
        fx(
            () ->
                show(
                    new LaboratoryExperimentDesk(
                        f.editor(), "stage1", () -> {}, action -> {}, () -> {})));
    int before = f.editor().document().experiment().journal().size();
    fx(
        () -> {
          text(p, "observation", "Учебное наблюдение");
          call(p, "recordObservation");
          return null;
        });
    require(
        f.editor().document().experiment().journal().size() > before, "Observation not recorded");
    roundTrip(f);
    fx(
        () -> {
          var table = new TableView<LaboratoryComponent>();
          var pane =
              new LaboratoryComponentsPane(table, id -> {}, id -> {}, id -> {}, id -> {}, v -> {});
          pane.load(
              f.editor().document().experiment().components(),
              f.product(),
              f.editor().document().experiment().stages());
          pane.expanded(true);
          ((ComboBox<?>) get(pane, "roles")).getSelectionModel().select(2);
          require(
              table.getItems().size() == 1 && table.getItems().getFirst().id().equals(f.product()),
              "Stage product missing from product filter");
          return null;
        });
  }

  private static List<Node> descendants(Parent root) {
    var list = new ArrayList<Node>();
    for (var node : root.getChildrenUnmodifiable()) {
      list.add(node);
      if (node instanceof Parent parent) list.addAll(descendants(parent));
    }
    return list;
  }

  private static void actionPicker() throws Exception {
    fx(
        () -> {
          var preferences = new LaboratoryViewPreferences(home.resolve("picker.properties"));
          var commands =
              List.of(
                  new LaboratoryCommandPalette.Command(
                      "calculate", "Расчёт свойств", "Аналитика", "Масса и формула", () -> {}),
                  new LaboratoryCommandPalette.Command(
                      "library",
                      "Библиотека веществ",
                      "Структура",
                      "Добавить своё вещество",
                      () -> {}),
                  new LaboratoryCommandPalette.Command(
                      "samples", "Образцы и фракции", "Образцы", "Аликвота и остаток", () -> {}));
          var dialog = LaboratoryCommandPalette.create(null, commands, preferences);
          var pane = dialog.getDialogPane();
          var search = (TextField) pane.lookup("#chemistry-actions-search");
          var section = (ComboBox<?>) pane.lookup("#chemistry-actions-section");
          var list = (ListView<?>) pane.lookup("#chemistry-actions-list");
          search.setText("расчет");
          require(list.getItems().size() == 1, "Search does not normalize ё/е");
          section.getSelectionModel().select(2);
          require(list.getItems().isEmpty(), "Section filter ignored");
          ((Button) list.getPlaceholder().lookup("#chemistry-actions-reset")).fire();
          require(list.getItems().size() == 3, "Reset does not restore all actions");
          preferences.set("favorite.library", true);
          ((CheckBox) pane.lookup("#chemistry-actions-favorites")).setSelected(true);
          require(
              list.getItems().size() == 1
                  && ((LaboratoryCommandPalette.Command) list.getItems().getFirst())
                      .id()
                      .equals("library"),
              "Favorite filter failed");
          ((CheckBox) pane.lookup("#chemistry-actions-favorites")).setSelected(false);
          dialog.setDialogPane(new DialogPane());
          if (pane.getParent() instanceof Pane parent) parent.getChildren().remove(pane);
          new Scene(pane, 800, 640);
          snapshot(pane, "action-picker");
          if (pane.getParent() instanceof Pane parent) parent.getChildren().remove(pane);
          else pane.getScene().setRoot(new Pane());
          dialog.setDialogPane(pane);
          var selected = list.getSelectionModel().getSelectedItem();
          list.fireEvent(
              new javafx.scene.input.KeyEvent(
                  javafx.scene.input.KeyEvent.KEY_PRESSED,
                  "",
                  "",
                  javafx.scene.input.KeyCode.ENTER,
                  false,
                  false,
                  false,
                  false));
          require(
              dialog.getResult() == selected && selected != null,
              "Enter does not select an action");
          dialog.close();
          return null;
        });
  }

  private static Button findButton(Parent root, String label) {
    return descendants(root).stream()
        .filter(n -> n instanceof Button b && b.getText().equals(label))
        .map(n -> (Button) n)
        .findFirst()
        .orElseThrow();
  }

  private static TextField findText(Parent root, String prompt) {
    return descendants(root).stream()
        .filter(n -> n instanceof TextField t && t.getPromptText().equals(prompt))
        .map(n -> (TextField) n)
        .findFirst()
        .orElseThrow();
  }

  private static void set(Object target, String field, Object value) throws Exception {
    var f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static void snapshot(Parent node, String name) throws Exception {
    if (!Boolean.getBoolean("quantum.audit.snapshots")) return;
    var scene = node.getScene();
    var stage = scene.getWindow() instanceof Stage s ? s : new Stage();
    if (stage.getScene() == null) {
      var root = scene.getRoot();
      scene.setRoot(new Pane());
      var wrapper = new BorderPane(root);
      wrapper.setId("chemistry-workspace");
      wrapper.getStyleClass().addAll("quantum-root", "theme-light");
      stage.setScene(new Scene(wrapper, 1200, 800));
      stage
          .getScene()
          .getStylesheets()
          .add(
              LaboratoryWorkspace.class
                  .getResource("/ru/pathcreator/vadim/quantum/desktop/ui/quantum-desktop.css")
                  .toExternalForm());
    }
    final var original = stage.getScene();
    final var root = original.getRoot();
    final var styles = List.copyOf(original.getStylesheets());
    for (int width : new int[] {1200, 800}) {
      if (node instanceof DialogPane dialogPane) {
        // Hidden Dialogs have no native window size. Fix the test viewport explicitly.
        dialogPane.setMinSize(width, width == 800 ? 640 : 800);
        dialogPane.setPrefSize(width, width == 800 ? 640 : 800);
      }
      stage.getScene().setRoot(new Pane());
      var resized = new Scene(root, width, width == 800 ? 640 : 800);
      resized.getStylesheets().setAll(styles);
      stage.setScene(resized);
      root.applyCss();
      root.layout();
      ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopScreenshotSupport.saveStageScreenshot(
          stage, "target/usability-audit/" + name + "-" + width + ".png");
      if (name.equals("library") || name.equals("library-add") || name.equals("action-picker")) {
        var themed = node instanceof DialogPane ? node : root;
        themed.getStyleClass().remove("theme-light");
        themed.getStyleClass().add("theme-dark");
        root.applyCss();
        root.layout();
        ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopScreenshotSupport.saveStageScreenshot(
            stage, "target/usability-audit/" + name + "-" + width + "-dark.png");
        themed.getStyleClass().remove("theme-dark");
        themed.getStyleClass().add("theme-light");
        root.applyCss();
        root.layout();
      }
      if (node instanceof LaboratoryComputationPage page
          && page.getBottom() != null
          && page.getBottom().isManaged()) {
        var bounds = page.getBottom().localToScene(page.getBottom().getBoundsInLocal());
        require(
            bounds.getMaxY() <= resized.getHeight() + 2
                && bounds.getMaxX() <= resized.getWidth() + 2,
            "Footer exceeds viewport: " + name + " " + width + " " + bounds);
      }
    }
    stage.getScene().setRoot(new Pane());
    original.setRoot(root);
    stage.setScene(original);
    root.applyCss();
    root.layout();
  }

  private static void roundTrip(Fixture fixture) throws Exception {
    var repository = new LaboratoryFileRepository();
    Path path = fixture.folder().resolve("verified.qchem");
    repository.write(
        path,
        fixture.editor().document(),
        Files.exists(path) ? repository.read(path).fingerprint() : null);
    require(
        repository.read(path).drawing().equals(fixture.editor().document()),
        "Saved experiment differs after reopening");
  }

  private static void fire(Parent page, String id) {
    page.applyCss();
    page.layout();
    var node = page.lookup("#" + id);
    if (!(node instanceof ButtonBase button)) throw new AssertionError("Missing button " + id);
    require(!button.isDisabled(), "Disabled button " + id);
    button.fire();
  }

  private static void text(Object target, String name, String value) throws Exception {
    ((TextInputControl) get(target, name)).setText(value);
  }

  private static Object get(Object target, String name) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
      } catch (NoSuchFieldException ignored) {
      }
    }
    throw new NoSuchFieldException(name);
  }

  private static Object call(Object target, String name) throws Exception {
    var m = target.getClass().getDeclaredMethod(name);
    m.setAccessible(true);
    return m.invoke(target);
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
    return result.get(30, TimeUnit.SECONDS);
  }

  private static void idle(LaboratoryComputationPage page) throws Exception {
    await(() -> !page.running);
  }

  private static void await(Callable<Boolean> condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
    while (!fx(condition)) {
      if (System.nanoTime() > deadline) throw new AssertionError("Operation timed out");
      Thread.sleep(40);
    }
  }

  private static void require(boolean condition, String text) {
    if (!condition) throw new AssertionError(text);
  }
}
