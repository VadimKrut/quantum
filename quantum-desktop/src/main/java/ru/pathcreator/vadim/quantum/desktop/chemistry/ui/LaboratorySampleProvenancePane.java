/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleReference;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySampleProvenance;

/** On-demand ancestry reader; it never changes the notebook or assigns inherited analyses. */
final class LaboratorySampleProvenancePane extends TitledPane {
  private final LaboratoryDrawing current;
  private final String sampleId;
  private final Path sourcePath;
  private final Map<LaboratorySampleReference, Path> replacements = new LinkedHashMap<>();
  private final VBox body = new VBox(8);
  private Thread worker;
  private int version;
  private boolean loaded, disposed;

  LaboratorySampleProvenancePane(final LaboratoryDrawing current, final String sampleId) {
    this(current, sampleId, null);
  }

  LaboratorySampleProvenancePane(
      final LaboratoryDrawing current, final String sampleId, final Path sourcePath) {
    super();
    this.current = current;
    this.sampleId = sampleId;
    this.sourcePath = sourcePath;
    this.setText("Исходные анализы · данные родителей");
    this.setId("chemistry-sample-provenance");
    this.setContent(this.body);
    this.setExpanded(false);
    this.setAnimated(false);
    this.body
        .getChildren()
        .add(
            text(
                "Исходные файлы будут проверены при открытии. Анализы предков не считаются измерениями этого дочернего образца."));
    this.expandedProperty()
        .addListener(
            (o, old, value) -> {
              if (value && !this.loaded) this.load();
            });
  }

  private void load() {
    final int request = ++this.version;
    this.loaded = true;
    if (this.worker != null) this.worker.interrupt();
    this.body.getChildren().setAll(text("Читаю происхождение и точные версии исходных анализов…"));
    final var paths = Map.copyOf(this.replacements);
    this.worker =
        Thread.startVirtualThread(
            () -> {
              try {
                final var result =
                    LaboratorySampleProvenance.resolve(
                        this.current, this.sampleId, this.sourcePath, paths);
                Platform.runLater(
                    () -> {
                      if (!this.disposed && request == this.version) this.show(result);
                    });
              } catch (final Exception error) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && request == this.version) {
                        this.body
                            .getChildren()
                            .setAll(
                                text(
                                    "Не удалось прочитать происхождение: "
                                        + Objects.toString(
                                            error.getMessage(), error.getClass().getSimpleName())),
                                button("Повторить чтение", this::load));
                      }
                    });
              }
            });
  }

  void show(final LaboratorySampleProvenance.Result result) {
    final var inherited =
        result.analyses().stream().filter(LaboratorySampleProvenance.Analysis::inherited).toList();
    this.body
        .getChildren()
        .setAll(
            text(
                "Анализы родительских образцов · "
                    + inherited.size()
                    + ". Они не назначаются этому образцу."));
    if (inherited.isEmpty())
      this.body
          .getChildren()
          .add(
              text(
                  result.diagnostics().isEmpty()
                      ? "В доступной цепочке нет связанных исходных анализов."
                      : "Часть происхождения не подтверждена. Отсутствие найденных записей не доказывает отсутствие анализов."));
    for (final String diagnostic : result.diagnostics())
      this.body.getChildren().add(text("• " + diagnostic));
    for (final var missing : result.missing())
      this.body
          .getChildren()
          .add(
              button(
                  "Найти опыт «" + missing.origin().sourceTitle() + "»…",
                  () -> {
                    final var chooser = new FileChooser();
                    chooser.setTitle("Исходный опыт · проверка по ID");
                    chooser
                        .getExtensionFilters()
                        .add(new FileChooser.ExtensionFilter("Опыт QCHEM", "*.qchem"));
                    final var file =
                        chooser.showOpenDialog(
                            this.getScene() == null ? null : this.getScene().getWindow());
                    if (file != null) {
                      this.replacements.put(missing.origin().source(), file.toPath());
                      this.load();
                    }
                  }));
    for (final var analysis : inherited) {
      final var details =
          new TitledPane(
              "«"
                  + analysis.sampleCode()
                  + "» · "
                  + analysis.method()
                  + " · "
                  + analysis.title()
                  + " · "
                  + analysis.origin(),
              null);
      details.setExpanded(false);
      details.setAnimated(false);
      details
          .expandedProperty()
          .addListener(
              (o, old, expanded) -> {
                if (expanded && details.getContent() == null) {
                  final String header =
                      analysis.basis()
                          + "\nОпыт: "
                          + analysis.experimentTitle()
                          + "\nОбразец: "
                          + analysis.sampleCode()
                          + "\n"
                          + analysis.specimen().token()
                          + "\n"
                          + analysis.kind()
                          + " · "
                          + analysis.id()
                          + "\nФайл: "
                          + Objects.toString(analysis.path(), "Текущий открытый опыт")
                          + "\n\n";
                  final var report = new TextArea(header + analysis.report());
                  report.setEditable(false);
                  report.setWrapText(true);
                  report.setPrefRowCount(9);
                  details.setContent(
                      new VBox(
                          6, text(analysis.basis() + " · не измерение дочернего образца"), report));
                }
              });
      this.body.getChildren().add(details);
    }
    this.body.getChildren().add(button("Обновить исходные записи", this::load));
  }

  void dispose() {
    this.disposed = true;
    this.version++;
    if (this.worker != null) this.worker.interrupt();
  }

  private static Label text(final String value) {
    final var label = new Label(value);
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }

  private static Button button(final String title, final Runnable action) {
    final var button = new Button(title);
    button.setWrapText(true);
    button.setOnAction(e -> action.run());
    return button;
  }
}
