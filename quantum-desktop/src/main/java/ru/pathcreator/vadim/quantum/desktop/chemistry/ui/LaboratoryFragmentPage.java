/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeFragmentPlacement;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryAttachmentPoint;

/** Preview placement on an immutable source, then use the workspace's single guarded graph command. */
final class LaboratoryFragmentPage extends LaboratoryComputationPage {
  private record Result(MoleculeDrawing drawing, String diagnostics) {}

  private final ComboBox<ChemistryLibraryAttachmentPoint> point = new ComboBox<>();
  private final ComboBox<Integer> direction = new ComboBox<>();
  private final LaboratoryStructurePreview preview = new LaboratoryStructurePreview();
  private final TextArea diagnostics = new TextArea();
  private final Button apply = new Button("Присоединить");
  private final MoleculeDrawing target, fragment;
  private final AtomId targetAtom;
  private final javafx.animation.PauseTransition debounce =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
  private javafx.concurrent.Task<Result> task;
  private final java.util.concurrent.ExecutorService worker =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          task -> {
            final Thread t = new Thread(task, "chemistry-fragment-preview");
            t.setDaemon(true);
            return t;
          });
  private long generation;
  private MoleculeDrawing proposed;

  LaboratoryFragmentPage(
      final MoleculeDrawing target,
      final AtomId targetAtom,
      final MoleculeDrawing fragment,
      final List<ChemistryLibraryAttachmentPoint> points,
      final Consumer<MoleculeDrawing> attach,
      final Runnable close) {
    super("Присоединить фрагмент", close);
    this.setId("chemistry-fragment-page");
    this.setMinSize(0, 0);
    this.target = target;
    this.targetAtom = targetAtom;
    this.fragment = fragment;
    ((Button) this.getTop().lookup("#chemistry-computation-back")).setText("← К библиотеке");
    this.point.setId("chemistry-fragment-point");
    this.point.getItems().setAll(points);
    this.point.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final ChemistryLibraryAttachmentPoint p) {
            return p == null
                ? ""
                : p.label() + " · " + LaboratoryStructureTable.bondName(p.bondType());
          }

          public ChemistryLibraryAttachmentPoint fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.point.getSelectionModel().selectFirst();
    this.direction.setId("chemistry-fragment-direction");
    this.direction.getItems().setAll(0, 45, 90, 135, 180, 225, 270, 315);
    this.direction.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final Integer angle) {
            return angle == null
                ? ""
                : switch (angle) {
                  case 0 -> "Вправо";
                  case 45 -> "Вниз-вправо";
                  case 90 -> "Вниз";
                  case 135 -> "Вниз-влево";
                  case 180 -> "Влево";
                  case 225 -> "Вверх-влево";
                  case 270 -> "Вверх";
                  default -> "Вверх-вправо";
                };
          }

          public Integer fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.direction.setValue(MoleculeFragmentPlacement.suggestedDirection(target, targetAtom));
    final Button turn = new Button("Повернуть направление ↻");
    turn.setId("chemistry-fragment-turn");
    turn.setOnAction(e -> this.direction.setValue((this.direction.getValue() + 45) % 360));
    this.diagnostics.setId("chemistry-fragment-diagnostics");
    this.diagnostics.setEditable(false);
    this.diagnostics.setWrapText(true);
    this.diagnostics.setPrefRowCount(3);
    this.diagnostics.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");
    final VBox body =
        new VBox(
            8,
            note(
                "К выбранному атому «"
                    + target.molecule().displayName()
                    + "» добавляется новая связь. Атомы и H автоматически не заменяются. План, факт и участие в стадиях сохраняются; свойства вещества потребуется проверить."),
            new FlowPane(
                8,
                6,
                note("Точка фрагмента"),
                this.point,
                note("Направление"),
                this.direction,
                turn),
            this.preview,
            this.diagnostics);
    body.setPadding(new javafx.geometry.Insets(0, 16, 8, 16));
    VBox.setVgrow(this.preview, Priority.ALWAYS);
    this.setCenter(scroll(body));
    this.apply.setId("chemistry-fragment-apply");
    this.apply.setDisable(true);
    this.apply.setOnAction(
        e -> {
          if (this.disposed || this.proposed == null || this.running) {
            return;
          }
          try {
            attach.accept(this.proposed);
            this.dispose();
          } catch (final RuntimeException failure) {
            this.status.setText(failure.getMessage());
          }
        });
    final VBox footer = new VBox(7, this.status, this.apply);
    footer.setPadding(new javafx.geometry.Insets(8, 16, 12, 16));
    this.setBottom(footer);
    this.point.valueProperty().addListener((o, a, b) -> this.schedule());
    this.direction.valueProperty().addListener((o, a, b) -> this.schedule());
    this.debounce.setOnFinished(e -> this.calculate());
    this.preview.show(target, targetAtom);
    this.schedule();
  }

  private void schedule() {
    if (this.disposed) {
      return;
    }
    this.generation++;
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.proposed = null;
    this.apply.setDisable(true);
    this.status.setText("Подготовка предпросмотра…");
    this.busy(true);
    this.debounce.playFromStart();
  }

  private void calculate() {
    if (this.disposed) {
      return;
    }
    final long version = this.generation;
    final var point = this.point.getValue();
    final var angle = this.direction.getValue();
    if (point == null || angle == null) {
      this.busy(false);
      this.status.setText("Выберите точку и направление.");
      return;
    }
    final var loading =
        new javafx.concurrent.Task<Result>() {
          protected Result call() {
            final var drawing =
                MoleculeFragmentPlacement.attach(
                    target, targetAtom, fragment, point.atomId(), point.bondType(), angle);
            if (this.isCancelled()) {
              throw new java.util.concurrent.CancellationException();
            }
            final var issues =
                new java.util.ArrayList<>(
                    new ru.pathcreator.vadim.quantum.chemistry.domain.validation
                            .ChemistryCoreValidator()
                        .validateMolecule(drawing.molecule()).diagnostics().stream()
                            .filter(
                                d ->
                                    d.severity()
                                            == ru.pathcreator.vadim.quantum.chemistry.domain
                                                .diagnostic.ChemistryDiagnosticSeverity.ERROR
                                        || d.code()
                                            == ru.pathcreator.vadim.quantum.chemistry.domain
                                                .diagnostic.ChemistryDiagnosticCode
                                                .MOLECULE_VALENCE_EXCEEDS_PROFILE)
                            .map(d -> new LaboratoryStructureCheck.Issue("", "", d).title())
                            .toList());
            if (MoleculeFragmentPlacement.overlaps(target, drawing)) {
              issues.addFirst(
                  "Фрагмент перекрывает исходную структуру. Выберите другое направление.");
            }
            return new Result(
                drawing,
                issues.isEmpty()
                    ? "Ошибок графа и превышения типичной валентности не обнаружено. Это не подтверждение химической осуществимости."
                    : "Черновик требует проверки:\n" + String.join("\n", issues));
          }
        };
    this.task = loading;
    loading.setOnSucceeded(
        e -> {
          if (this.disposed || this.generation != version) {
            return;
          }
          this.task = null;
          this.busy(false);
          this.proposed = loading.getValue().drawing();
          this.preview.show(this.proposed, this.targetAtom);
          this.diagnostics.setText(loading.getValue().diagnostics());
          this.status.setText(
              "Предпросмотр готов. Изменение структуры выполняется одной операцией.");
          this.apply.setDisable(false);
        });
    loading.setOnFailed(
        e -> {
          if (this.disposed || this.generation != version) {
            return;
          }
          this.task = null;
          this.busy(false);
          this.preview.show(this.target, this.targetAtom);
          this.diagnostics.setText(loading.getException().getMessage());
          this.status.setText("Присоединение недоступно; исходная структура сохранена.");
        });
    this.worker.submit(loading);
  }

  @Override
  boolean canClose() {
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    this.generation++;
    this.debounce.stop();
    if (this.task != null) {
      this.task.cancel(true);
    }
    this.worker.shutdownNow();
    this.busy(false);
  }
}
