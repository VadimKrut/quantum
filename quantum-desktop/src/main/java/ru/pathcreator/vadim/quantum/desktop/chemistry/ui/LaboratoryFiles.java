/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryFileRepository;

final class LaboratoryFiles {

  private final LaboratoryFileRepository repository = new LaboratoryFileRepository();
  private final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
          .LaboratorySessionPreferences
      session =
          new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratorySessionPreferences();
  private String resumeWarning = "";
  private final LaboratoryFileOperation operation = new LaboratoryFileOperation();
  private java.util.function.Consumer<Exception> problem = failure -> {};

  void activity(final java.util.function.Consumer<javafx.scene.Node> display) {
    this.operation.display(display);
  }

  void problems(final java.util.function.Consumer<Exception> display) {
    this.problem = display;
  }

  boolean busy() {
    return this.operation.busy();
  }

  LaboratoryFileOperation operation() {
    return this.operation;
  }

  void cancelOperation() {
    this.operation.cancel();
  }

  boolean cancelDecisionForClose(final Runnable retryClose) {
    return this.operation.cancelDecisionForClose(retryClose);
  }

  boolean decisionClosePending() {
    return this.operation.decisionClosePending();
  }

  void clearProblem() {
    this.resumeWarning = "";
  }

  private final LaboratoryEditor editor;
  private final Runnable flush;
  private final Runnable refreshed;
  private final Runnable saved;
  private final Runnable previewed;
  private final BooleanSupplier pending;
  private final LaboratoryFileActions actions;
  private java.util.function.BiConsumer<
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
              .LaboratoryCalculationArchive,
          String>
      calculationPreview;

  void calculationPreview(
      final java.util.function.BiConsumer<
              ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                  .LaboratoryCalculationArchive,
              String>
          preview) {
    this.calculationPreview = preview;
  }

  private java.util.function.BiConsumer<
          ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryPredictionArchive,
          String>
      predictionPreview;

  void predictionPreview(
      final java.util.function.BiConsumer<
              ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                  .LaboratoryPredictionArchive,
              String>
          preview) {
    this.predictionPreview = preview;
  }

  private java.util.function.Consumer<
          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment>
      attachmentPreview;
  private java.util.function.Consumer<java.util.List<Path>> attachmentImport;

  void attachmentPreview(
      final java.util.function.Consumer<
              ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment>
          action) {
    this.attachmentPreview = action;
  }

  void attachmentImport(final java.util.function.Consumer<java.util.List<Path>> action) {
    this.attachmentImport = action;
  }

  void prepareAttachments(final java.util.List<Path> paths) {
    this.flush.run();
    this.attachmentImport.accept(paths);
  }

  void openPreview(final LaboratoryArtifactViewer.Preview preview) {
    if (preview.calculation() != null) {
      this.calculationPreview.accept(preview.calculation(), preview.source());
    } else if (preview.prediction() != null) {
      this.predictionPreview.accept(preview.prediction(), preview.source());
    }
  }

  private java.util.function.BiConsumer<
          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment, Path>
      attachmentExport;

  void attachmentExport(
      final java.util.function.BiConsumer<
              ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment, Path>
          action) {
    this.attachmentExport = action;
  }

  void extractTo(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment attachment,
      final Path destination) {
    this.attachmentExport.accept(attachment, destination);
  }

  private Runnable history;

  void history(final Runnable action) {
    this.history = action;
  }

  private Window owner;
  private Path path;
  private Path preferredDirectory;
  private Path recoveredSource;
  private String fingerprint;

  LaboratoryFiles(
      final LaboratoryEditor editor,
      final Runnable flush,
      final Runnable refreshed,
      final BooleanSupplier pending,
      final Runnable saved,
      final Runnable previewed) {
    this.editor = editor;
    this.actions = new LaboratoryFileActions(this, editor);
    this.flush = flush;
    this.refreshed = refreshed;
    this.saved = saved;
    this.previewed = previewed;
    this.pending = pending;
  }

  void owner(final Window window) {
    this.owner = window;
  }

  Window owner() {
    return this.owner;
  }

  Path path() {
    return this.path;
  }

  void preferredDirectory(final Path directory) {
    this.preferredDirectory = directory.toAbsolutePath().normalize();
  }

  ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySessionPreferences
      session() {
    return this.session;
  }

  String warning() {
    return this.resumeWarning.isBlank() ? this.session.warning() : this.resumeWarning;
  }

  boolean restoreLast() {
    if (!this.session.resume()
        || this.path != null
        || this.editor.dirty()
        || this.pending.getAsBoolean()) {
      return false;
    }
    final Path last = this.session.current();
    return last != null && this.load(last, false);
  }

  Path assetPath() {
    return this.path == null ? this.recoveredSource : this.path;
  }

  void recover(
      final ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryRecoveryStore
              .Recovered
          recovered) {
    this.editor.open(recovered.drawing());
    this.editor.markUnsaved();
    this.path = null;
    this.fingerprint = null;
    this.recoveredSource = recovered.source();
    this.session.newDocument();
    this.resumeWarning = "";
    this.refreshed.run();
  }

  boolean requestClose() {
    if (this.busy()) {
      return false;
    }
    if (!this.editor.dirty() && !this.pending.getAsBoolean()) {
      return true;
    }
    final String choice = this.operation.confirm();
    return choice.equals("discard") || choice.equals("save") && this.save(false);
  }

  void attach() {
    this.actions.attach();
  }

  void extract(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment
          attachment) {
    this.actions.extract(attachment);
  }

  void preview(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment
          attachment) {
    try {
      this.flush.run();
      try {
        if (attachment != null) {
          this.attachmentPreview.accept(attachment);
        }
      } finally {
        this.previewed.run();
      }
    } catch (final Exception exception) {
      this.error(exception);
    }
  }

  void exportArchive() {
    this.actions.exportArchive();
  }

  void importArchive() {
    this.actions.importArchive();
  }

  void history() {
    if (this.history == null) {
      throw new IllegalStateException("История не подключена к рабочему месту.");
    }
    this.history.run();
  }

  void flushPending() {
    this.flush.run();
  }

  @FunctionalInterface
  interface Operation {
    void run() throws Exception;
  }

  void perform(final Operation action) {
    try {
      this.flush.run();
      try {
        action.run();
      } finally {
        this.refreshed.run();
      }
    } catch (final Exception exception) {
      this.error(exception);
    }
  }

  void repeat() {
    if (!this.requestClose()) {
      return;
    }
    this.editor.repeat();
    this.path = null;
    this.fingerprint = null;
    this.recoveredSource = null;
    this.session.newDocument();
    this.resumeWarning = "";
    this.refreshed.run();
  }

  void create() {
    this.createForProject();
  }

  boolean createForProject() {
    if (!this.requestClose()) {
      return false;
    }
    this.editor.open(new LaboratoryEditor().document());
    this.recoveredSource = null;
    this.path = null;
    this.fingerprint = null;
    this.session.newDocument();
    this.resumeWarning = "";
    this.refreshed.run();
    return true;
  }

  boolean fromTemplate(
      final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing
          drawing) {
    if (!this.requestClose()) {
      return false;
    }
    this.editor.open(drawing);
    this.editor.markUnsaved();
    this.recoveredSource = null;
    this.path = null;
    this.fingerprint = null;
    this.session.newDocument();
    this.resumeWarning = "";
    this.refreshed.run();
    return true;
  }

  boolean openDialog() {
    final File file = this.chooser().showOpenDialog(this.owner);
    return file != null && this.openChecked(file.toPath());
  }

  void open(final Path next) {
    this.openChecked(next);
  }

  boolean openChecked(final Path next) {
    return this.requestClose() && this.load(next, true);
  }

  void openApproved(final Path next) {
    this.load(next, true);
  }

  private boolean load(final Path next, final boolean report) {
    if (this.busy()) {
      return false;
    }
    final var original = this.editor.document();
    try {
      final var opened =
          this.operation.run(
              "Открыть опыт",
              next.toAbsolutePath().normalize().toString(),
              true,
              () -> this.repository.read(next));
      if (!this.editor.document().equals(original)) {
        throw new IllegalStateException("Опыт изменился во время чтения. Открытие не применено.");
      }
      this.editor.open(opened.drawing());
      this.recoveredSource = null;
      this.path = next.toAbsolutePath().normalize();
      this.fingerprint = opened.fingerprint();
      this.session.opened(this.path);
      this.resumeWarning = "";
      this.refreshed.run();
      return true;
    } catch (final java.util.concurrent.CancellationException cancelled) {
      this.resumeWarning = cancelled.getMessage();
      this.previewed.run();
      return false;
    } catch (final Exception exception) {
      if (report) {
        this.error(exception);
      } else {
        this.resumeWarning =
            "Последний опыт не открыт: " + next.getFileName() + " · " + exception.getMessage();
        this.refreshed.run();
      }
      return false;
    }
  }

  boolean save(final boolean saveAs) {
    if (this.busy()) {
      return false;
    }
    Path target = this.path;
    boolean replace = false;
    if (target == null || saveAs) {
      final File selected = this.chooser().showSaveDialog(this.owner);
      if (selected == null) {
        return false;
      }
      target = selected.toPath();
      replace = true;
    }
    return this.saveTo(target, replace);
  }

  boolean saveTo(final Path targetPath, final boolean replace) {
    if (this.busy()) {
      return false;
    }
    try {
      this.flush.run();
      final var drawing = this.editor.document();
      final Path source = this.assetPath();
      final Path target = targetPath.toAbsolutePath().normalize();
      final Path originalPath = this.path;
      final String expected = this.fingerprint;
      final String written =
          this.operation.run(
              "Сохранить опыт",
              target.toString(),
              false,
              () -> {
                final String version =
                    target.equals(originalPath)
                        ? expected
                        : replace && Files.exists(target)
                            ? this.repository.read(target).fingerprint()
                            : null;
                new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                        .LaboratoryAssetStore()
                    .transfer(drawing, source, target);
                return this.repository.write(target, drawing, version);
              });
      if (!this.editor.document().experiment().id().equals(drawing.experiment().id())) {
        this.path = null;
        this.fingerprint = null;
        this.recoveredSource = null;
        this.editor.markUnsaved();
        this.session.newDocument();
        this.error(
            new IllegalStateException(
                "Исходный снимок записан, но текущий опыт сменился. Новый опыт не отмечен сохранённым."));
        return false;
      }
      this.path = target;
      this.recoveredSource = null;
      this.fingerprint = written;
      final boolean unchanged = this.editor.document().equals(drawing);
      if (unchanged) {
        this.editor.markSaved();
      } else {
        this.editor.markUnsaved();
      }
      this.session.opened(target);
      this.resumeWarning =
          unchanged ? "" : "Снимок записан; более поздние изменения ещё не сохранены.";
      this.saved.run();
      return unchanged;
    } catch (final Exception exception) {
      this.error(exception);
      return false;
    }
  }

  /** Reconcile a confirmed external sample operation without destroying its recovery page. */
  void refreshExternalSave(final Path source) {
    if (this.path == null
        || !this.path.equals(source.toAbsolutePath().normalize())
        || this.busy()) {
      throw new IllegalStateException(
          "Открыт другой опыт; заново откройте исходный файл передачи.");
    }
    try {
      final var opened = this.repository.read(this.path);
      if (!opened.drawing().experiment().id().equals(this.editor.document().experiment().id())) {
        throw new IllegalStateException("Исходный файл заменён другим опытом.");
      }
      if (this.editor.dirty()) {
        throw new IllegalStateException(
            "В текущем опыте появились новые правки. Сохранённый расход образца доступен в файле; откройте его после сохранения своих правок.");
      }
      this.editor.open(opened.drawing());
      this.fingerprint = opened.fingerprint();
      this.recoveredSource = null;
      this.resumeWarning = "";
      this.saved.run();
    } catch (final java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  /** Reattach a recovered transfer only while its exact recovered document is still open. */
  void refreshRecoveredExternalSave(
      final Path source,
      final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing
          recovered) {
    if (this.path != null) {
      this.refreshExternalSave(source);
      return;
    }
    final Path normalized = source.toAbsolutePath().normalize();
    if (this.busy()
        || this.recoveredSource == null
        || !normalized.equals(this.recoveredSource.toAbsolutePath().normalize())
        || !this.editor.document().equals(recovered)) {
      throw new IllegalStateException(
          "Восстановленный опыт изменён. Сохранённый расход находится в исходном файле; откройте его после сохранения своих правок.");
    }
    try {
      final var opened = this.repository.read(normalized);
      if (!opened.drawing().experiment().id().equals(recovered.experiment().id())) {
        throw new IllegalStateException("Исходный файл заменён другим опытом.");
      }
      this.editor.open(opened.drawing());
      this.path = normalized;
      this.fingerprint = opened.fingerprint();
      this.recoveredSource = null;
      this.session.opened(normalized);
      this.resumeWarning = "";
      this.saved.run();
    } catch (final java.io.IOException failure) {
      throw new java.io.UncheckedIOException(failure);
    }
  }

  boolean exportArchiveTo(final Path target) {
    if (this.busy()) {
      return false;
    }
    try {
      this.flush.run();
      final var drawing = this.editor.document();
      final Path source = this.assetPath();
      this.operation.run(
          "Пакет опыта с вложениями",
          target.toAbsolutePath().normalize().toString(),
          false,
          () -> {
            new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryArchive()
                .export(target, source, drawing);
            return null;
          });
      this.resumeWarning = "Пакет записан: " + target.getFileName();
      this.previewed.run();
      return true;
    } catch (final Exception exception) {
      this.error(exception);
      return false;
    }
  }

  boolean importArchiveFrom(final Path source, final Path target) {
    if (this.busy()) {
      return false;
    }
    final var original = this.editor.document();
    try {
      final var opened =
          this.operation.run(
              "Восстановить опыт из пакета",
              source.toAbsolutePath().normalize() + "\n→ " + target.toAbsolutePath().normalize(),
              false,
              () -> {
                new ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure
                        .LaboratoryArchive()
                    .importTo(source, target);
                return this.repository.read(target);
              });
      if (!this.editor.document().equals(original)) {
        throw new IllegalStateException(
            "Пакет восстановлен на диске. Текущий опыт изменился; импортированный файл не открыт автоматически.");
      }
      this.editor.open(opened.drawing());
      this.path = target.toAbsolutePath().normalize();
      this.fingerprint = opened.fingerprint();
      this.recoveredSource = null;
      this.session.opened(this.path);
      this.resumeWarning = "";
      this.refreshed.run();
      return true;
    } catch (final Exception exception) {
      this.error(exception);
      return false;
    }
  }

  void error(final Exception exception) {
    this.resumeWarning =
        "Действие не выполнено: "
            + (exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage());
    this.problem.accept(exception);
  }

  private FileChooser chooser() {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Лабораторный опыт");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Опыт QCHEM", "*.qchem"));
    chooser.setInitialFileName(
        this.path == null ? "experiment.qchem" : this.path.getFileName().toString());
    final Path directory =
        this.path == null
            ? this.preferredDirectory == null ? this.session.directory() : this.preferredDirectory
            : this.path.getParent();
    if (directory != null && Files.isDirectory(directory)) {
      chooser.setInitialDirectory(directory.toFile());
    }
    return chooser;
  }
}
