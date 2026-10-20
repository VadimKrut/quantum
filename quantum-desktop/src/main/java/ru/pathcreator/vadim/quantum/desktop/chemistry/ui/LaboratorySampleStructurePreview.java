/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularStereoParity;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereochemicalProjection;

/** A frozen sample is never rewritten to obtain a screen depiction. */
final class LaboratorySampleStructurePreview {
  record Projection(MoleculeDrawing drawing, String annotation, boolean topologyOnly) {}

  private LaboratorySampleStructurePreview() {}

  static Node create(final LaboratorySample sample) {
    final Projection projection;
    try {
      projection = project(sample.molecule());
    } catch (final IllegalArgumentException ex) {
      final Label warning =
          LaboratoryComputationPage.note(
              "Стереорисунок недоступен: "
                  + ex.getMessage()
                  + " Снимок структуры сохранён без изменений.");
      warning.setId("chemistry-sample-stereo-warning");
      warning.setWrapText(true);
      return warning;
    }
    final var drawing = projection.drawing();
    final double height = LaboratoryCanvas.previewHeight(drawing, 240, 320);
    final Canvas canvas = new Canvas(360, height);
    canvas.setId("chemistry-sample-structure");
    final StackPane frame = new StackPane(canvas);
    frame.setMinSize(0, height);
    frame.setMaxHeight(height);
    frame.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 8;");
    canvas.widthProperty().bind(frame.widthProperty());
    final var clip = new javafx.scene.shape.Rectangle();
    clip.widthProperty().bind(frame.widthProperty());
    clip.heightProperty().bind(frame.heightProperty());
    frame.setClip(clip);
    final Runnable paint =
        () -> {
          final var graphics = canvas.getGraphicsContext2D();
          LaboratoryPreviewTheme.clear(graphics);
          if (canvas.getWidth() < 20) {
            return;
          }
          final var bounds = LaboratoryCanvas.previewBounds(drawing);
          final double scale =
              Math.min(
                  2,
                  Math.min(
                      (canvas.getWidth() - 32) / Math.max(1, bounds.getWidth()),
                      (height - 32) / Math.max(1, bounds.getHeight())));
          graphics.save();
          try {
            graphics.translate(
                (canvas.getWidth() - bounds.getWidth() * scale) / 2 - bounds.getMinX() * scale,
                (height - bounds.getHeight() * scale) / 2 - bounds.getMinY() * scale);
            graphics.scale(scale, scale);
            LaboratoryPreviewTheme.paint(graphics, drawing, !projection.topologyOnly());
          } finally {
            graphics.restore();
          }
        };
    LaboratoryPreviewTheme.track(canvas, paint);
    canvas.widthProperty().addListener(o -> paint.run());
    paint.run();
    canvas.setAccessibleText(sample.molecule().displayName() + ". " + projection.annotation());
    if (projection.annotation().isBlank()) {
      return frame;
    }
    final Label annotation = LaboratoryComputationPage.note(projection.annotation());
    annotation.setId("chemistry-sample-stereo-note");
    annotation.setWrapText(true);
    return projection.topologyOnly()
        ? new VBox(6, annotation, frame)
        : new VBox(6, frame, annotation);
  }

  static Projection project(final Molecule original) {
    try {
      final var depiction = CdkStereochemicalProjection.depict(original);
      return new Projection(depiction.drawing(), depiction.annotation(), false);
    } catch (final IllegalArgumentException failure) {
      final var optional = MolecularRelativeStereo.read(original);
      if (optional.isEmpty()) {
        throw failure;
      }
      final var relative = optional.get();
      relative.validate(original);
      // Only absent orientation is safe here. Never mask stale, partial or inconsistent stereo
      // evidence.
      if (original.atomCount() > 256
          || !original.stereochemistry().empty()
          || !MolecularStereoParity.read(original).isEmpty()
          || original.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)
          || relative.groups().stream()
              .anyMatch(g -> g.kind() == MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE)) {
        throw failure;
      }
      final Molecule topology = MolecularRelativeStereo.clear(original);
      final var depiction = CdkStereochemicalProjection.depict(topology);
      if (!original.atoms().equals(depiction.drawing().molecule().atoms())
          || !original.bonds().equals(depiction.drawing().molecule().bonds())) {
        throw new IllegalArgumentException("Не удалось сохранить точную связность снимка образца.");
      }
      final List<String> kinds =
          relative.groups().stream()
              .map(
                  g ->
                      g.kind() == MolecularRelativeStereo.Kind.RACEMIC
                          ? "Рацемат"
                          : "Относительная конфигурация")
              .distinct()
              .toList();
      return new Projection(
          depiction.drawing(),
          String.join("; ", kinds)
              + "; представитель не задан. Схема связности, стереоконфигурация не показана.",
          true);
    }
  }
}
