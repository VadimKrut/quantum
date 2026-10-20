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
import java.nio.file.Path;
import java.util.List;
import javafx.stage.FileChooser;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryArchive;

final class LaboratoryFileActions {
  private final LaboratoryFiles files;
  private final LaboratoryEditor editor;

  LaboratoryFileActions(final LaboratoryFiles files, final LaboratoryEditor editor) {
    this.files = files;
    this.editor = editor;
  }

  void attach() {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Добавить вложения к опыту");
    final List<File> selected = chooser.showOpenMultipleDialog(this.files.owner());
    if (selected == null) {
      return;
    }
    if (!selected.isEmpty()) {
      this.files.prepareAttachments(selected.stream().map(File::toPath).toList());
    }
  }

  void extract(final LaboratoryAttachment attachment) {
    if (attachment == null) {
      return;
    }
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Сохранить вложение");
    chooser.setInitialFileName(
        Path.of(attachment.name().replace('\\', '/')).getFileName().toString());
    final File target = chooser.showSaveDialog(this.files.owner());
    if (target == null) {
      return;
    }
    this.files.extractTo(attachment, target.toPath());
  }

  void exportArchive() {
    final File selected = packageChooser().showSaveDialog(this.files.owner());
    if (selected != null) {
      this.files.exportArchiveTo(selected.toPath());
    }
  }

  void importArchive() {
    if (!this.files.requestClose()) {
      return;
    }
    final File source = packageChooser().showOpenDialog(this.files.owner());
    if (source == null) {
      return;
    }
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Импортировать как новый опыт QCHEM");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Опыт QCHEM", "*.qchem"));
    chooser.setInitialFileName("imported.qchem");
    final File target = chooser.showSaveDialog(this.files.owner());
    if (target == null) {
      return;
    }
    this.files.importArchiveFrom(source.toPath(), target.toPath());
  }

  private static FileChooser packageChooser() {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Опыт с вложениями");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Пакет опыта", "*.qchempack"));
    chooser.setInitialFileName("experiment.qchempack");
    return chooser;
  }
}
