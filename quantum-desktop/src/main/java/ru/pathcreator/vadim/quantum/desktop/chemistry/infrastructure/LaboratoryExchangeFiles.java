/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStructureExchange;

/** Explicitly chosen encodings; bounded reads and atomic publication of a completed export. */
public final class LaboratoryExchangeFiles {
  private LaboratoryExchangeFiles() {}

  public static String read(final Path path, final Charset encoding) throws IOException {
    try (final var stream = Files.newInputStream(path)) {
      final byte[] bytes = stream.readNBytes(CdkStructureExchange.MAX_TEXT * 4 + 1);
      if (bytes.length > CdkStructureExchange.MAX_TEXT * 4) {
        throw new IOException("Файл больше 16 МБ.");
      }
      final String text =
          encoding
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();
      if (text.length() > CdkStructureExchange.MAX_TEXT) {
        throw new IOException("В файле больше 4 млн символов.");
      }
      return text;
    } catch (final CharacterCodingException e) {
      throw new IOException(
          "Не удалось прочитать кодировку. Выберите UTF-8 или Windows-1251 и откройте файл снова.",
          e);
    }
  }

  public static void writeReactionCopy(final Path target, final String text) throws IOException {
    if (text.length() > CdkStructureExchange.MAX_TEXT) {
      throw new IOException("Экспорт слишком большой.");
    }
    final Path path = target.toAbsolutePath().normalize();
    final String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
    if (!name.matches(".+\\.(rxn|rsmi|smi|smiles|txt)")) {
      throw new IOException("Выберите расширение .rxn или .rsmi.");
    }
    final Path temporary = Files.createTempFile(path.getParent(), ".reaction-", ".tmp");
    try {
      Files.writeString(temporary, text, StandardCharsets.UTF_8);
      Files.move(temporary, path);
    } catch (final FileAlreadyExistsException e) {
      throw new IOException("Файл уже существует. Выберите новое имя копии.", e);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public static void write(final Path target, final String text) throws IOException {
    if (text.length() > CdkStructureExchange.MAX_TEXT) {
      throw new IOException("Экспорт слишком большой.");
    }
    final Path path = target.toAbsolutePath().normalize();
    final Path temporary = Files.createTempFile(path.getParent(), ".structure-", ".tmp");
    try {
      Files.writeString(temporary, text, StandardCharsets.UTF_8);
      Files.move(
          temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
