/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.io.*;
import java.util.*;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.*;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryMoleculePlacement;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Uses the existing stereochemical renderer on an offscreen canvas; never opens a window. */
final class LaboratoryReportFigures {
  private LaboratoryReportFigures() {}

  static byte[] create(final LaboratoryDrawing document, final String key) throws IOException {
    final var experiment = document.experiment();
    if (key.startsWith("component:")) {
      final Canvas canvas = white(720, 310);
      structure(
          canvas.getGraphicsContext2D(), document.layouts().get(key.substring(10)), 0, 0, 720, 310);
      return png(canvas);
    }
    if (key.startsWith("analysis:")) {
      final var analysis =
          experiment.analyses().stream()
              .filter(a -> key.equals("analysis:" + a.id()))
              .findFirst()
              .orElseThrow();
      final Canvas canvas = white(900, 360);
      structure(
          canvas.getGraphicsContext2D(),
          new LaboratoryMoleculePlacement().place(analysis.molecule(), 0, 0),
          0,
          0,
          340,
          360);
      final var plot = new LaboratoryAnalysisPlot();
      plot.resize(550, 300);
      plot.massHint("Исходная точность m/z сохранена в QCHEM");
      plot.data(analysis.data());
      plot.layout();
      final SnapshotParameters parameters = new SnapshotParameters();
      parameters.setTransform(javafx.scene.transform.Transform.scale(2, 2));
      canvas.getGraphicsContext2D().drawImage(plot.snapshot(parameters, null), 350, 30, 550, 300);
      return png(canvas);
    }
    final var stage =
        experiment.stages().stream()
            .filter(s -> key.equals("stage:" + s.id()))
            .findFirst()
            .orElseThrow();
    final var left =
        stage.participants().stream()
            .filter(p -> p.role() == ReactionComponentRole.REACTANT)
            .toList();
    final var right =
        stage.participants().stream()
            .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
            .toList();
    if (left.size() + right.size() > 24) {
      throw new IOException("В схеме больше 24 участников. Отключите схемы для текстового отчёта.");
    }
    final boolean compact = left.size() + right.size() <= 3 && !left.isEmpty() && !right.isEmpty();
    final boolean paired =
        !compact && !left.isEmpty() && left.size() <= 6 && !right.isEmpty() && right.size() <= 2;
    final int leftRows = Math.max(1, (left.size() + 2) / 3),
        rightRows = Math.max(1, (right.size() + 2) / 3);
    final Canvas canvas =
        white(
            1000,
            compact
                ? 280
                : paired
                    ? Math.max(2, (left.size() + 1) / 2) * 230
                    : (leftRows + rightRows) * 250 + 65);
    final var g = canvas.getGraphicsContext2D();
    if (compact) {
      final double cell = 900.0 / (left.size() + right.size());
      double x = 0;
      for (final var participant : left) {
        if (x > 0) {
          plus(g, x, 125);
        }
        participant(document, g, participant, x, 0, cell, 280);
        x += cell;
      }
      arrow(g, x + 5, 125, 90, stage.arrow() == LaboratoryReactionStage.Arrow.EQUILIBRIUM);
      x += 100;
      for (int i = 0; i < right.size(); i++) {
        if (i > 0) {
          plus(g, x, 125);
        }
        participant(document, g, right.get(i), x, 0, cell, 280);
        x += cell;
      }
    } else if (paired) {
      for (int i = 0; i < left.size(); i++) {
        if (i % 2 == 1) {
          plus(g, 300, i / 2 * 230 + 100);
        }
        participant(document, g, left.get(i), i % 2 * 300, i / 2 * 230, 300, 230);
      }
      arrow(
          g,
          610,
          canvas.getHeight() / 2 - 20,
          60,
          stage.arrow() == LaboratoryReactionStage.Arrow.EQUILIBRIUM);
      for (int i = 0; i < right.size(); i++) {
        participant(
            document,
            g,
            right.get(i),
            680,
            i * canvas.getHeight() / right.size(),
            320,
            canvas.getHeight() / right.size());
      }
    } else {
      side(document, g, left, 0);
      arrow(
          g,
          400,
          leftRows * 250 + 28,
          200,
          stage.arrow() == LaboratoryReactionStage.Arrow.EQUILIBRIUM);
      side(document, g, right, leftRows * 250 + 65);
    }
    return png(canvas);
  }

  private static void side(
      final LaboratoryDrawing d,
      final GraphicsContext g,
      final List<LaboratoryStageParticipant> side,
      final double y) {
    if (side.isEmpty()) {
      g.setFill(Color.web("#536477"));
      g.setFont(Font.font("Arial", 17));
      g.fillText("Участники не заданы", 395, y + 100);
    }
    for (int i = 0; i < side.size(); i++) {
      final int rowCount = Math.min(3, side.size() - i / 3 * 3);
      final double width = 1000.0 / rowCount;
      if (i % 3 != 0) {
        plus(g, (i % 3) * width, y + i / 3 * 250 + 100);
      }
      participant(d, g, side.get(i), (i % 3) * width, y + i / 3 * 250, width, 250);
    }
  }

  private static void participant(
      final LaboratoryDrawing d,
      final GraphicsContext g,
      final LaboratoryStageParticipant p,
      final double x,
      final double y,
      final double w,
      final double h) {
    final var drawing = d.layouts().get(p.componentId());
    structure(g, drawing, x, y, w, h - 46);
    g.setFill(Color.web("#22364e"));
    g.setFont(Font.font("Arial", 14));
    g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
    final String name = drawing.molecule().displayName();
    final int length = Math.max(12, (int) (w / 8));
    g.fillText(
        name.length() <= length ? name : name.substring(0, length - 1) + "…",
        x + w / 2,
        y + h - 25);
    g.setFont(Font.font("Arial", 12));
    g.fillText(
        ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryExperimentReport.role(
            p.role()),
        x + w / 2,
        y + h - 8);
    g.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
  }

  private static Canvas white(final double width, final double height) {
    final Canvas c = new Canvas(width, height);
    c.getGraphicsContext2D().setFill(Color.WHITE);
    c.getGraphicsContext2D().fillRect(0, 0, width, height);
    return c;
  }

  private static void plus(final GraphicsContext g, final double x, final double y) {
    g.setFill(Color.web("#536477"));
    g.setFont(Font.font("Arial", 22));
    g.fillText("+", x - 6, y);
  }

  private static void structure(
      final GraphicsContext g,
      final MoleculeDrawing drawing,
      final double x,
      final double y,
      final double w,
      final double h) {
    if (drawing == null || drawing.atoms().isEmpty()) {
      return;
    }
    final var bounds = LaboratoryCanvas.previewBounds(drawing);
    final double scale =
        Math.max(
            .01,
            Math.min(
                1.5,
                Math.min(
                    (w - 30) / Math.max(1, bounds.getWidth()),
                    (h - 30) / Math.max(1, bounds.getHeight()))));
    g.save();
    try {
      g.translate(
          x + w / 2 - (bounds.getMinX() + bounds.getWidth() / 2) * scale,
          y + h / 2 - (bounds.getMinY() + bounds.getHeight() / 2) * scale);
      g.scale(scale, scale);
      LaboratoryCanvas.paintPreview(g, drawing);
    } finally {
      g.restore();
    }
  }

  private static void arrow(
      final GraphicsContext g,
      final double x,
      final double y,
      final double width,
      final boolean equilibrium) {
    g.setStroke(Color.web("#253e59"));
    g.setLineWidth(2.5);
    g.strokeLine(x, y, x + width, y);
    g.strokeLine(x + width, y, x + width - 12, y - 7);
    g.strokeLine(x + width, y, x + width - 12, y + 7);
    if (equilibrium) {
      g.strokeLine(x, y + 15, x + width, y + 15);
      g.strokeLine(x, y + 15, x + 12, y + 8);
      g.strokeLine(x, y + 15, x + 12, y + 22);
    }
  }

  private static byte[] png(final Canvas canvas) throws IOException {
    final SnapshotParameters parameters = new SnapshotParameters();
    parameters.setTransform(javafx.scene.transform.Transform.scale(2, 2));
    parameters.setFill(Color.WHITE);
    final WritableImage image = canvas.snapshot(parameters, null);
    final int width = (int) image.getWidth(), height = (int) image.getHeight();
    final int[] pixels = new int[width * height];
    image
        .getPixelReader()
        .getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
    final var buffered =
        new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
    buffered.setRGB(0, 0, width, height, pixels, 0, width);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    javax.imageio.ImageIO.write(buffered, "png", bytes);
    return bytes.toByteArray();
  }
}
