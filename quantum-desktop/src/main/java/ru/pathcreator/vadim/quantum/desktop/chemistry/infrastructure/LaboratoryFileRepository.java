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
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Checked replacement, recovery copy and detection of modifications since opening. */
public final class LaboratoryFileRepository {

  private final LaboratoryFileCodec codec = new LaboratoryFileCodec();

  public record Opened(LaboratoryDrawing drawing, String fingerprint) {}

  public Opened read(final Path path) throws IOException {
    final byte[] bytes = documentBytes(path);
    return new Opened(this.codec.read(decode(bytes)), fingerprint(bytes));
  }

  private static byte[] documentBytes(final Path path) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
      throw new IOException("Файл опыта отсутствует или превышает 64 МиБ.");
    }
    try (final var input = Files.newInputStream(path)) {
      final byte[] bytes = input.readNBytes(LaboratoryFileCodec.MAX_DOCUMENT_BYTES + 1);
      if (bytes.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
        throw new IOException("Файл опыта увеличился сверх 64 МиБ.");
      }
      return bytes;
    }
  }

  static String decode(final byte[] bytes) throws IOException {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    } catch (final java.nio.charset.CharacterCodingException exception) {
      throw new IOException(
          "Файл содержит повреждённый текст UTF-8. Исходный файл не изменён.", exception);
    }
  }

  static byte[] encode(final String text) throws IOException {
    try {
      final ByteBuffer encoded =
          StandardCharsets.UTF_8.newEncoder().encode(java.nio.CharBuffer.wrap(text));
      final byte[] bytes = new byte[encoded.remaining()];
      encoded.get(bytes);
      return bytes;
    } catch (final java.nio.charset.CharacterCodingException exception) {
      throw new IOException(
          "Текст содержит незавершённый символ Unicode. Исправьте текст перед сохранением.",
          exception);
    }
  }

  public String write(
      final Path path, final LaboratoryDrawing drawing, final String expectedFingerprint)
      throws IOException {
    final Path target = path.toAbsolutePath().normalize();
    Files.createDirectories(target.getParent());
    final Path lockPath = target.resolveSibling(target.getFileName() + ".lock");
    try (final FileChannel channel =
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        final java.nio.channels.FileLock lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("Другой экземпляр Studio сохраняет этот опыт. Повторите сохранение.");
      }
      return this.writeLocked(target, drawing, expectedFingerprint);
    } catch (final java.nio.channels.OverlappingFileLockException exception) {
      throw new IOException("Другое окно сохраняет этот опыт. Повторите сохранение.", exception);
    }
  }

  private String writeLocked(
      final Path path, final LaboratoryDrawing drawing, final String expectedFingerprint)
      throws IOException {
    final Path target = path.toAbsolutePath().normalize();
    final byte[] bytes = encode(this.codec.write(drawing));
    if (bytes.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
      throw new IOException("Файл опыта превышает 64 МиБ.");
    }
    if (!this.codec.read(decode(bytes)).equals(drawing)) {
      throw new IOException("Проверка сохранения не пройдена: данные изменились при записи.");
    }
    Files.createDirectories(target.getParent());
    final Path temporary = Files.createTempFile(target.getParent(), ".qchem-", ".tmp");
    try {
      try (final FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) {
          channel.write(buffer);
        }
        channel.force(true);
      }
      if (Files.exists(target)) {
        final byte[] previous = documentBytes(target);
        final String current = fingerprint(previous);
        if (expectedFingerprint == null || !expectedFingerprint.equals(current)) {
          throw new IOException(
              "Файл изменён вне Studio. Откройте его заново или сохраните под другим именем.");
        }
        // Map iteration order can differ between JVMs; unchanged data must not create a revision.
        if (this.codec.read(decode(previous)).equals(drawing)) {
          return current;
        }
        final Path history = historyDirectory(target);
        Files.createDirectories(history);
        final Path revision = history.resolve(current + ".qchem");
        if (!Files.exists(revision)) {
          final Path snapshot = Files.createTempFile(history, ".revision-", ".tmp");
          try {
            Files.copy(target, snapshot, StandardCopyOption.REPLACE_EXISTING);
            Files.move(snapshot, revision, StandardCopyOption.ATOMIC_MOVE);
          } finally {
            Files.deleteIfExists(snapshot);
          }
        }
        Files.copy(
            target,
            target.resolveSibling(target.getFileName() + ".bak"),
            StandardCopyOption.REPLACE_EXISTING);
      } else if (expectedFingerprint != null) {
        throw new IOException("Исходный файл удалён вне Studio. Сохраните под другим именем.");
      }
      // If the filesystem cannot replace atomically, keep the previous document intact.
      Files.move(
          temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      return fingerprint(bytes);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public static Path historyDirectory(final Path document) {
    final Path absolute = document.toAbsolutePath().normalize();
    return absolute.resolveSibling(absolute.getFileName() + ".history");
  }

  private static String fingerprint(final byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (final NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
