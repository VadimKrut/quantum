/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.BiConsumer;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkInitialGeometry;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryComputationFiles;

final class LaboratoryInitialGeometryWindow extends LaboratoryComputationPage {
  private final Task<CdkInitialGeometry.Result> preparation;
  private Molecule geometry;
  private String method;

  LaboratoryInitialGeometryWindow(
      final LaboratoryComponent component,
      final String experimentId,
      final Path directory,
      final BiConsumer<Path, Molecule> apply,
      final Runnable close) {
    this(
        component,
        MoleculeDrawing.of(
            component.molecule(), java.util.List.of(), java.util.List.of(), java.util.List.of()),
        experimentId,
        directory,
        apply,
        close);
  }

  LaboratoryInitialGeometryWindow(
      final LaboratoryComponent component,
      final MoleculeDrawing drawing,
      final String experimentId,
      final Path directory,
      final BiConsumer<Path, Molecule> apply,
      final Runnable close) {
    super("Начальная 3D-геометрия", close);
    this.setId("chemistry-initial-geometry-page");
    if (drawing == null || !drawing.molecule().equals(component.molecule())) {
      throw new IllegalArgumentException("Рисунок относится к другой версии структуры.");
    }
    if (component.molecule().atoms().size() > 256) {
      throw new IllegalArgumentException(
          "Начальная 3D-раскладка в Studio поддерживает до 256 явных атомов.");
    }
    final String id = "geometry_" + UUID.randomUUID();
    final Label note =
        note(
            "Проверяемый кандидат начальной 3D-геометрии, без оптимизации и расчёта энергий. Заданная стереохимия проверяется по координатам. Явные атомы, связи и 2D-рисунок сохраняются; водороды не добавляются.");
    final LaboratoryGeometryPane preview = new LaboratoryGeometryPane(atom -> {});
    final Button accept = new Button("Применить начальные координаты");
    accept.setId("chemistry-initial-geometry-apply");
    accept.setDisable(true);
    accept.getStyleClass().add("laboratory-save");
    final Button cancel = new Button("Остановить");
    cancel.setId("chemistry-initial-geometry-cancel");
    final VBox content =
        new VBox(8, note(component.molecule().displayName()), note, scroll(preview));
    content.setPadding(new javafx.geometry.Insets(0, 16, 0, 16));
    VBox.setVgrow(content.getChildren().getLast(), Priority.ALWAYS);
    this.setCenter(content);
    final VBox footer = new VBox(8, this.status, new FlowPane(8, 6, accept, cancel));
    footer.setPadding(new javafx.geometry.Insets(12, 16, 14, 16));
    this.setBottom(footer);
    this.preparation =
        new Task<>() {
          protected CdkInitialGeometry.Result call() {
            return CdkInitialGeometry.prepare(drawing);
          }
        };
    this.preparation.setOnSucceeded(
        event -> {
          if (this.disposed) {
            return;
          }
          this.busy(false);
          cancel.setDisable(true);
          this.method = this.preparation.getValue().method();
          final Molecule prepared = this.preparation.getValue().molecule();
          final boolean changed = !prepared.equals(component.molecule());
          this.geometry =
              changed
                  ? LaboratoryComputationFiles.geometrySnapshot(
                      prepared, id + "_output", "CDK_INITIAL_STEREO_VERIFIED")
                  : prepared;
          preview.load(this.geometry, null);
          accept.setDisable(!changed);
          this.status.setText(
              changed
                  ? "Начальная геометрия готова; заданная конфигурация проверена. Просмотрите её перед применением."
                  : "У структуры уже есть пригодные координаты. Они сохранены без изменений; для релаксации используйте отдельную команду.");
          note.setText(this.method);
        });
    this.preparation.setOnFailed(
        event -> {
          if (!this.disposed) {
            this.busy(false);
            cancel.setDisable(true);
            this.status.setText(
                "Подготовка не завершена: " + this.preparation.getException().getMessage());
          }
        });
    this.preparation.setOnCancelled(
        event -> {
          if (!this.disposed) {
            this.busy(false);
            cancel.setDisable(true);
            this.status.setText("Подготовка отменена. Координаты опыта не изменены.");
          }
        });
    cancel.setOnAction(event -> this.preparation.cancel());
    accept.setOnAction(
        event -> {
          if (this.running || this.geometry == null || accept.isDisabled()) {
            return;
          }
          try {
            CdkInitialGeometry.verify(drawing, this.geometry);
            final var document =
                ChemistryStorageDocument.builder(
                        id, "Начальная геометрия " + component.molecule().displayName())
                    .molecule(component.molecule())
                    .molecule(this.geometry)
                    .metadata("calculation.kind", "INITIAL_GEOMETRY")
                    .metadata("calculation.status", "PREPARED")
                    .metadata("calculation.method", "CDK_INITIAL_STEREO_VERIFIED")
                    .metadata("calculation.method_version", "CDK-2.12-process-stereo-v1")
                    .metadata("calculation.experiment", experimentId)
                    .metadata("calculation.component", component.id())
                    .metadata("calculation.input_molecule", component.molecule().id().value())
                    .metadata("calculation.output_molecule", this.geometry.id().value())
                    .metadata("calculation.finished", java.time.Instant.now().toString())
                    .metadata("calculation.note", this.method)
                    .build();
            apply.accept(LaboratoryComputationFiles.write(directory, id, document), this.geometry);
            this.finish();
          } catch (final Exception exception) {
            this.status.setText(exception.getMessage());
          }
        });
    this.busy(true);
    this.status.setText("Подготовка координат…");
    final Thread worker = new Thread(this.preparation, "laboratory-initial-geometry");
    worker.setDaemon(true);
    worker.start();
  }

  @Override
  void dispose() {
    super.dispose();
    this.preparation.cancel();
  }
}
