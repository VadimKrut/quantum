/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy.MolecularSpectroscopyPredictionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.*;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryExchangeFiles;

/** A read-only structural guide. Nothing in this page is an observed spectrum or analytical evidence. */
final class LaboratoryExpectedNmrPage extends LaboratoryComputationPage {
  record Band(String title, double low, double high) {}

  record Prediction(List<Band> bands, List<String> notes, String report) {
    Prediction {
      bands = List.copyOf(bands);
      notes = List.copyOf(notes);
    }
  }

  private static final String MODEL = "feature_based_ir_nmr_uv_prediction";
  private static final String LIMITS =
      "Это области структурных признаков, не рассчитанный спектр. Отдельные сдвиги δ, число сигналов, интегралы, мультиплетность, J и ¹³C модель не рассчитывает. Растворитель, примеси и доли смеси не учитываются.";
  private final LaboratoryEditor editor;
  private final LaboratoryDrawing snapshot;
  private final String componentId;
  private final VBox result = new VBox(10);
  private final Button copy = new Button("Копировать ориентир"),
      export = new Button("Сохранить отдельный TXT…");
  private final Task<Prediction> calculation;
  private Prediction prediction;

  LaboratoryExpectedNmrPage(
      final LaboratoryEditor editor, final String componentId, final Runnable close) {
    super("Ожидаемые области ¹H ЯМР", close);
    this.editor = Objects.requireNonNull(editor);
    this.snapshot = editor.document();
    this.componentId = componentId;
    final var component =
        this.snapshot.experiment().components().stream()
            .filter(c -> c.id().equals(componentId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Выбранная структура отсутствует в опыте."));
    this.setId("chemistry-expected-nmr");
    final Label provenance = note("Расчётный ориентир · не измерение");
    provenance.setStyle("-fx-font-weight: bold; -fx-text-fill: -app-warning-text;");
    provenance.setId("chemistry-expected-nmr-origin");
    final Label name = LaboratoryComponentNaming.label(component.molecule().displayName());
    name.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
    final Label limitation = note(LIMITS);
    limitation.setId("chemistry-expected-nmr-limitations");
    final VBox body =
        new VBox(
            12,
            provenance,
            name,
            note(component.molecule().formula().hillNotation() + " · снимок выбранной структуры"),
            limitation,
            this.result);
    body.setPadding(new javafx.geometry.Insets(18));
    body.setMaxWidth(Double.MAX_VALUE);
    body.setMinWidth(0);
    this.result.setMinWidth(0);
    this.setCenter(scroll(body));
    this.copy.setId("chemistry-expected-nmr-copy");
    this.export.setId("chemistry-expected-nmr-export");
    this.copy.setDisable(true);
    this.export.setDisable(true);
    this.copy.setOnAction(
        e -> {
          try {
            final var content = new ClipboardContent();
            content.putString(this.report());
            if (!Clipboard.getSystemClipboard().setContent(content))
              throw new IllegalStateException("Буфер обмена недоступен.");
            this.status.setText(
                "Расчётный ориентир скопирован; аналитические результаты не изменены.");
          } catch (RuntimeException ex) {
            this.status.setText(ex.getMessage());
          }
        });
    this.export.setOnAction(e -> this.export());
    final VBox footer = new VBox(8, this.status, new FlowPane(8, 8, this.copy, this.export));
    footer.setPadding(new javafx.geometry.Insets(12));
    this.setBottom(footer);
    this.calculation =
        new Task<>() {
          @Override
          protected Prediction call() {
            return calculate(LaboratoryExpectedNmrPage.this.snapshot, componentId);
          }
        };
    this.calculation.setOnSucceeded(
        e -> {
          if (this.disposed) return;
          this.busy(false);
          this.prediction = this.calculation.getValue();
          this.show();
        });
    this.calculation.setOnFailed(
        e -> {
          if (this.disposed) return;
          this.busy(false);
          this.status.setText(
              "Ориентир недоступен: "
                  + Objects.toString(
                      this.calculation.getException().getMessage(), "проверьте структуру."));
        });
    this.busy(true);
    this.status.setText("Определяю структурные признаки локально…");
    final Thread worker = new Thread(this.calculation, "laboratory-expected-nmr");
    worker.setDaemon(true);
    worker.start();
  }

  static Prediction calculate(final LaboratoryDrawing source, final String componentId) {
    final var component =
        source.experiment().components().stream()
            .filter(c -> c.id().equals(componentId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Структура отсутствует."));
    final var drawing = source.layouts().get(componentId);
    final Molecule molecule = component.molecule();
    if (molecule.atoms().size() > 512
        || molecule.atoms().stream().filter(a -> !a.element().symbol().value().equals("H")).count()
            > 128)
      throw new IllegalArgumentException(
          "Локальный ориентир ограничен 128 тяжёлыми атомами и 512 атомами всего.");
    if (drawing == null
        || MoleculeSkeletonHydrogens.unsupportedCount(drawing) > 0
        || MoleculeHydrogenEditing.suggest(drawing).stream()
            .anyMatch(s -> s.additional() != null && s.additional() > 0))
      throw new IllegalArgumentException(
          "Сначала проверьте явные H и валентность выбранной структуры. Рисунок здесь не исправляется автоматически.");
    final var request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("expected_nmr_" + UUID.randomUUID()),
            molecule,
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.CUSTOM, ElectronicStructureMethodName.of(MODEL)),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.NMR_PREDICTION));
    final List<ClassicalDiagnostic> diagnostics = new ArrayList<>();
    final var section =
        new MolecularSpectroscopyPredictionExecutor()
            .execute(request, ClassicalCalculationKind.NMR_PREDICTION, diagnostics);
    if (section == null || !MODEL.equals(section.requireFirstValue("model")))
      throw new IllegalArgumentException("Локальная модель не вернула поддерживаемый ориентир.");
    final List<Band> bands = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    for (final String row : section.requireValues("spectroscopy_rows")) {
      final String[] fields = row.split("\t", -1);
      if (fields.length != 5 || !fields[0].startsWith("NMR")) continue;
      switch (fields[2]) {
        case "aromatic proton and pi-pi* region" -> {
          if (fields[1].equals("6.0-8.5 ppm / 200-300 nm"))
            bands.add(new Band("Ароматический признак", 6, 8.5));
        }
        case "C=C and vinyl proton region" -> {
          if (fields[1].equals("4.5-6.5 ppm / 1620-1680 cm^-1"))
            bands.add(new Band("Алкеновый признак", 4.5, 6.5));
        }
        case "heterocycle electronic anisotropy" ->
            notes.add(
                "Гетероцикл: модель отмечает влияние окружения, но не даёт численного сдвига.");
        case "halogen substituent signature" ->
            notes.add(
                "Галогензамещённая структура: модель отмечает влияние заместителя без численного сдвига.");
        default -> notes.add("Признак модели: " + fields[2] + " · " + fields[1]);
      }
    }
    if (molecule.atoms().stream().noneMatch(a -> a.element().symbol().value().equals("H"))) {
      bands.clear();
      notes.add("В явном составе структуры нет H; области протонных сигналов не показаны.");
    }
    if (bands.isEmpty())
      notes.add(
          "Численных областей ¹H эта модель для структуры не вернула. Это не означает отсутствие сигналов в реальном спектре.");
    notes.add(
        "Область относится к классу структурных признаков, а не к назначению конкретного протона; её ширина не является шириной линии или доверительным интервалом.");
    final StringBuilder report =
        new StringBuilder("РАСЧЁТНЫЙ ОРИЕНТИР · НЕ ИЗМЕРЕНИЕ\n")
            .append(LIMITS)
            .append("\nМодель: ")
            .append(MODEL)
            .append("\nДата расчёта: ")
            .append(Instant.now())
            .append("\nОпыт: ")
            .append(source.experiment().title())
            .append("\nОпыт ID: ")
            .append(source.experiment().id())
            .append("\nКомпонент ID: ")
            .append(componentId)
            .append("\nВещество: ")
            .append(molecule.displayName())
            .append("\nФормула: ")
            .append(molecule.formula().hillNotation())
            .append("\nСтруктурная подпись: ")
            .append(MolecularIdentity.signature(molecule))
            .append(
                "\nМетод не использует квантовый расчёт или базис; параметры обязательной обёртки запроса не являются физической моделью.\n\nОбласти, ppm:\n");
    bands.forEach(
        b ->
            report
                .append(b.title())
                .append('\t')
                .append(b.low())
                .append("–")
                .append(b.high())
                .append('\n'));
    notes.forEach(n -> report.append(n).append('\n'));
    report.append("\nПолный вывод таблицы существующей модели (IR/UV не являются ЯМР):\n");
    section.requireValues("spectroscopy_rows").forEach(row -> report.append(row).append('\n'));
    diagnostics.forEach(d -> report.append(d.message()).append('\n'));
    report.append("\nИсходная связность, только для происхождения отчёта:\n");
    molecule
        .atoms()
        .forEach(
            a ->
                report
                    .append(a.id().value())
                    .append('\t')
                    .append(a.element().symbol().value())
                    .append('\t')
                    .append(a.formalCharge().value())
                    .append('\t')
                    .append(a.hasIsotope() ? a.isotope().massNumber() : "")
                    .append('\n'));
    molecule
        .bonds()
        .forEach(
            b ->
                report
                    .append(b.firstAtomId().value())
                    .append('\t')
                    .append(b.secondAtomId().value())
                    .append('\t')
                    .append(b.type())
                    .append('\n'));
    return new Prediction(bands, notes, report.toString());
  }

  private void show() {
    this.result.getChildren().clear();
    if (!this.prediction.bands().isEmpty()) {
      final Label title = note("Области признаков · не спектр");
      title.setStyle("-fx-font-weight: bold;");
      this.result.getChildren().add(title);
      final Canvas canvas = new Canvas(760, 190);
      canvas.setId("chemistry-expected-nmr-regions");
      canvas.setAccessibleText(
          "Диапазонная диаграмма ориентировочных областей ¹H ЯМР; пики и интенсивности не рассчитаны.");
      final StackPane frame = new StackPane(canvas);
      frame.setMinWidth(0);
      frame.setPrefWidth(1);
      frame.setMinHeight(190);
      frame.setMaxHeight(190);
      canvas.widthProperty().bind(frame.widthProperty());
      final Runnable paint = () -> this.paint(canvas);
      LaboratoryPreviewTheme.track(canvas, paint);
      canvas.widthProperty().addListener(o -> paint.run());
      this.result.getChildren().add(frame);
    }
    for (final var band : this.prediction.bands())
      this.result
          .getChildren()
          .add(note(band.title() + ": " + band.low() + "–" + band.high() + " ppm"));
    this.prediction.notes().forEach(n -> this.result.getChildren().add(note(n)));
    final TextArea full = new TextArea(this.prediction.report());
    full.setEditable(false);
    full.setWrapText(true);
    full.setPrefRowCount(10);
    final TitledPane details = new TitledPane("Модель, происхождение и полный вывод", full);
    details.setExpanded(false);
    details.setAnimated(false);
    this.result.getChildren().add(details);
    this.copy.setDisable(false);
    this.export.setDisable(false);
    this.status.setText(
        this.current()
            ? "Локальный ориентир готов. Анализы и фактические результаты опыта не изменены."
            : "Структура в опыте изменилась. Показан прежний снимок; откройте ориентир заново для текущей структуры.");
  }

  private void paint(final Canvas canvas) {
    final var g = canvas.getGraphicsContext2D();
    LaboratoryPreviewTheme.clear(g);
    final var palette = LaboratoryPreviewTheme.palette(canvas);
    g.setFill(palette.ink());
    g.setStroke(palette.ink());
    final double left = 38, width = Math.max(1, canvas.getWidth() - 70), axis = 158;
    g.strokeLine(left, axis, left + width, axis);
    for (int tick = 0; tick <= 12; tick += 2) {
      final double x = left + width * (12 - tick) / 12;
      g.strokeLine(x, axis, x, axis + 5);
      g.fillText(Integer.toString(tick), x - 4, axis + 20);
    }
    g.fillText("δ, ppm", Math.max(left, canvas.getWidth() - 70), 20);
    int i = 0;
    for (final Band b : this.prediction.bands()) {
      final double y = 44 + i++ * 48;
      g.setFill(palette.selectedBackground());
      g.fillRoundRect(
          left + width * (12 - b.high()) / 12, y, width * (b.high() - b.low()) / 12, 22, 5, 5);
      g.setStroke(palette.selection());
      g.strokeRoundRect(
          left + width * (12 - b.high()) / 12, y, width * (b.high() - b.low()) / 12, 22, 5, 5);
      g.setFill(palette.ink());
      g.fillText(b.title(), left, y - 8);
    }
  }

  private boolean current() {
    return this.editor.document().experiment().id().equals(this.snapshot.experiment().id())
        && this.editor.document().experiment().components().stream()
            .anyMatch(
                c ->
                    c.id().equals(this.componentId)
                        && c.molecule()
                            .equals(this.snapshot.layouts().get(this.componentId).molecule()));
  }

  String report() {
    if (this.prediction == null) throw new IllegalStateException("Дождитесь завершения расчёта.");
    return this.prediction.report()
        + (this.current()
            ? ""
            : "\nВНИМАНИЕ: текущая структура отличается от снимка этого ориентира.\n");
  }

  private void export() {
    try {
      final String text = this.report();
      final FileChooser chooser = new FileChooser();
      chooser.setTitle("Сохранить расчётный ориентир отдельно от анализов");
      chooser.setInitialFileName("expected-nmr.txt");
      chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текст UTF-8", "*.txt"));
      final var selected =
          chooser.showSaveDialog(this.getScene() == null ? null : this.getScene().getWindow());
      if (selected == null) return;
      final Path path = selected.toPath();
      if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
        throw new IllegalArgumentException(
            "Выберите отдельный файл .txt; файл опыта не заменяется.");
      LaboratoryExchangeFiles.write(path, text);
      this.status.setText(
          "Отдельный расчётный ориентир сохранён. В аналитические результаты он не добавлен.");
    } catch (Exception ex) {
      this.status.setText("Не удалось сохранить ориентир: " + ex.getMessage());
    }
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.calculation.cancel(true);
  }
}
