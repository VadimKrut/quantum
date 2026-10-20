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
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperimentalDesign.Run;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySeriesPlan;

/** Fingerprint-checked standalone dispensing proposals with previous revision preservation. */
public final class LaboratoryDispensingRepository {

  public record Opened(LaboratoryDispensingCodec.Document document, String fingerprint) {}

  private final LaboratoryDispensingCodec codec = new LaboratoryDispensingCodec();

  public Opened read(final Path file) throws IOException {
    final byte[] bytes = bytes(file);
    return new Opened(
        this.codec.read(LaboratoryFileRepository.decode(bytes)),
        LaboratoryDesignCodec.fingerprint(bytes));
  }

  public String write(
      final Path file,
      final LaboratoryDispensingCodec.Document document,
      final String expectedFingerprint)
      throws IOException {
    final Path target = file.toAbsolutePath().normalize();
    if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".qdispense"))
      throw new IOException("Используйте расширение .qdispense.");
    Files.createDirectories(target.getParent());
    final Path lockPath = target.resolveSibling(target.getFileName() + ".lock");
    try (final var channel =
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        final var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("План сохраняется другим окном. Повторите позже.");
      }
      final byte[] data = LaboratoryFileRepository.encode(this.codec.write(document));
      if (!this.codec.read(LaboratoryFileRepository.decode(data)).equals(document)) {
        throw new IOException("Проверка сохранения плана не пройдена.");
      }
      byte[] previous = null;
      if (Files.exists(target)) {
        previous = bytes(target);
        final String current = LaboratoryDesignCodec.fingerprint(previous);
        if (expectedFingerprint == null || !expectedFingerprint.equals(current)) {
          throw new IOException(
              "План изменён другим окном или программой. Откройте актуальный файл; ваши изменения не перезаписали его.");
        }
        if (this.codec.read(LaboratoryFileRepository.decode(previous)).equals(document)) {
          return current;
        }
      } else if (expectedFingerprint != null) {
        throw new IOException("Исходный план удалён. Сохраните копию под другим именем.");
      }
      final Path temporary = Files.createTempFile(target.getParent(), ".qdispense-", ".tmp");
      try {
        durable(temporary, data, false);
        if (previous != null) {
          final Path history = target.resolveSibling(target.getFileName() + ".history");
          Files.createDirectories(history);
          final Path revision =
              history.resolve(LaboratoryDesignCodec.fingerprint(previous) + ".qdispense");
          if (!Files.exists(revision)) {
            final Path snapshot = Files.createTempFile(history, ".revision-", ".tmp");
            try {
              durable(snapshot, previous, false);
              try {
                Files.move(snapshot, revision);
              } catch (final FileAlreadyExistsException ignored) {
              }
            } finally {
              Files.deleteIfExists(snapshot);
            }
          }
          if (!LaboratoryDesignCodec.fingerprint(bytes(target)).equals(expectedFingerprint)) {
            throw new IOException(
                "План изменился во время сохранения. Предыдущая версия сохранена в истории.");
          }
        } else if (Files.exists(target)) {
          throw new FileAlreadyExistsException("Путь уже занят другим планом.");
        }
        if (previous == null) {
          Files.move(temporary, target);
        } else {
          Files.move(
              temporary,
              target,
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        }
        return LaboratoryDesignCodec.fingerprint(data);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (final java.nio.channels.OverlappingFileLockException exception) {
      throw new IOException("План уже сохраняется в другом окне.", exception);
    }
  }

  private static byte[] bytes(final Path file) throws IOException {
    if (Files.size(file) > LaboratoryDispensingCodec.MAX_BYTES) {
      throw new IOException(
          "Файл превышает " + LaboratoryDispensingCodec.MAX_BYTES / (1024 * 1024) + " МиБ.");
    }
    try (final var input = Files.newInputStream(file)) {
      final byte[] bytes = input.readNBytes(LaboratoryDispensingCodec.MAX_BYTES + 1);
      if (bytes.length > LaboratoryDispensingCodec.MAX_BYTES) {
        throw new IOException("Файл вырос сверх допустимого размера.");
      }
      return bytes;
    }
  }

  private static void durable(final Path file, final byte[] data, final boolean create)
      throws IOException {
    try (final var channel =
        FileChannel.open(
            file,
            create
                ? new StandardOpenOption[] {StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE}
                : new StandardOpenOption[] {
                  StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
                })) {
      final var buffer = ByteBuffer.wrap(data);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
  }
}
