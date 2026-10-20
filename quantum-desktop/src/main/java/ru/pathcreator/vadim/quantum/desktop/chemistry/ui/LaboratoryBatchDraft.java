/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Properties;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Identity-bound batch inputs; unrelated observations, journal entries and layouts may evolve. */
final class LaboratoryBatchDraft {
  private LaboratoryBatchDraft() {}

  static String signature(final LaboratoryDrawing drawing, final String stageId) {
    final var experiment = drawing.experiment();
    final var stage =
        experiment.stages().stream().filter(s -> s.id().equals(stageId)).findFirst().orElse(null);
    if (stage == null) {
      return "";
    }
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      add(digest, "batch-input-v1", experiment.id(), stage.id(), stage.title(), stage.arrow());
      for (final var participant : stage.participants()) {
        final var component =
            experiment.components().stream()
                .filter(c -> c.id().equals(participant.componentId()))
                .findFirst()
                .orElseThrow();
        final var properties = component.properties();
        add(
            digest,
            component.id(),
            component.molecule().id().value(),
            component.molecule().displayName(),
            LaboratoryMaterialProperties.signature(component.molecule()),
            component.role(),
            participant.role(),
            participant.coefficient() == null ? null : participant.coefficient().value(),
            component.planned(),
            properties.molarMass(),
            properties.density(),
            properties.purityPercent(),
            properties.molarity(),
            properties.source(),
            properties.structureSignature(),
            ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryPlanStructureMasses
                .molarMass(component.molecule()));
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (final java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void add(final MessageDigest digest, final Object... values) {
    for (final var value : values) {
      final byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
      digest.update(bytes);
    }
  }

  static Properties copy(final Properties source) {
    final var result = new Properties();
    source.stringPropertyNames().stream()
        .filter(key -> key.startsWith("batch."))
        .forEach(key -> result.setProperty(key, source.getProperty(key)));
    return result;
  }

  static String review(final Properties fields) {
    if (fields.containsKey("batch.review")) {
      return fields.getProperty("batch.review");
    }
    final var text = new StringBuilder("Сохранённые поля прежнего черновика:\n\n");
    fields.stringPropertyNames().stream()
        .filter(key -> key.startsWith("batch."))
        .sorted()
        .forEach(
            key -> text.append(key).append(" = ").append(fields.getProperty(key)).append('\n'));
    return text.toString();
  }
}
