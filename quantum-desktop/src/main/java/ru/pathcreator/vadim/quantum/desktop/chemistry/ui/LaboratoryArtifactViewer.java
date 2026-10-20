/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.*;

/** File verification and parsing run in the attachment worker, never in the UI handler. */
final class LaboratoryArtifactViewer {
  record Preview(
      String text,
      byte[] image,
      String source,
      LaboratoryCalculationArchive calculation,
      LaboratoryPredictionArchive prediction,
      String notice) {}

  private LaboratoryArtifactViewer() {}

  static Preview read(final Path document, final LaboratoryAttachment attachment)
      throws java.io.IOException {
    final Path path = new LaboratoryAssetStore().locate(document, attachment);
    final String name = attachment.name().toLowerCase(Locale.ROOT);
    final boolean image = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    final boolean qchem = name.endsWith(".qchem");
    final boolean text =
        qchem
            || name.endsWith(".txt")
            || name.endsWith(".csv")
            || name.endsWith(".log")
            || name.endsWith(".md");
    if (!image && !text) {
      return new Preview(
          "Встроенный просмотр этого формата не подключён. Исходный файл проверен и сохранён без изменений. Используйте «Сохранить как…» для просмотра в подходящей программе.",
          null,
          null,
          null,
          null,
          "Файл проверен · " + attachment.name());
    }
    final long limit = image || qchem ? 32L * 1024 * 1024 : 2L * 1024 * 1024;
    if (attachment.size() > limit) {
      return new Preview(
          "Файл больше "
              + limit / (1024 * 1024)
              + " МиБ. Сохраните вложение отдельно для полного просмотра.",
          null,
          null,
          null,
          null,
          "Исходный файл проверен; размер просмотра ограничен.");
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
    final byte[] bytes;
    try (var input = Files.newInputStream(path)) {
      bytes = input.readNBytes((int) limit + 1);
    }
    try {
      final String hash =
          HexFormat.of()
              .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
      if (bytes.length != attachment.size() || !hash.equals(attachment.sha256())) {
        throw new java.io.IOException("Вложение изменилось во время чтения: " + attachment.name());
      }
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
    if (image) {
      return new Preview(
          null,
          bytes,
          null,
          null,
          null,
          "Изображение · предпросмотр до 2048 пикселей, масштаб относительно него. Исходный файл не изменяется.");
    }
    final String source;
    try {
      source =
          StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    } catch (final CharacterCodingException failure) {
      return new Preview(
          "Текст не удалось прочитать как UTF-8. Исходные байты сохранены; используйте «Сохранить как…» и выберите кодировку во внешнем редакторе.",
          null,
          null,
          null,
          null,
          "Кодировка текста не поддержана");
    }
    String notice = "Текст UTF-8 · только чтение";
    if (qchem) {
      final var parsed = new ChemistryStorageService().read(source);
      if (!parsed.success()) {
        notice = "QCHEM не разобран: показан исходный текст.";
      } else {
        try {
          if (parsed.value().metadata().containsKey("calculation.kind")) {
            return new Preview(
                null,
                null,
                source,
                new LaboratoryCalculationArchive(parsed.value()),
                null,
                "Сохранённый расчёт");
          }
          if (parsed.value().metadata().containsKey("prediction.adapter_version")) {
            return new Preview(
                null,
                null,
                source,
                null,
                new LaboratoryPredictionArchive(parsed.value(), Path.of(attachment.name())),
                "Сохранённый прогноз");
          }
        } catch (final IllegalArgumentException failure) {
          notice = "Снимок не открыт: " + failure.getMessage() + " Показан исходный QCHEM.";
        }
      }
    }
    final int maximum = 250_000;
    final String visible = source.length() > maximum ? source.substring(0, maximum) : source;
    if (visible.length() < source.length()) {
      notice +=
          " Показаны первые " + maximum + " символов. Полный файл доступен через «Сохранить как…».";
    }
    return new Preview(visible, null, null, null, null, notice);
  }
}
