/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** CAS saves, stable immutable structural identities and content-addressed revision snapshots. */
public final class LaboratoryRegistryRepository {
  public record Opened(LaboratoryRegistryCodec.Document document, String fingerprint) {}

  private final LaboratoryRegistryCodec codec = new LaboratoryRegistryCodec();

  public static Path file(final Path folder) {
    return folder.toAbsolutePath().normalize().resolve("compound-registry.qchem");
  }

  public Opened read(final Path file) throws IOException {
    final byte[] data = bytes(file);
    return new Opened(
        codec.read(LaboratoryFileRepository.decode(data)), LaboratoryDesignCodec.fingerprint(data));
  }

  public List<Path> history(final Path file) throws IOException {
    final Path folder =
        file.toAbsolutePath().normalize().resolveSibling(file.getFileName() + ".history");
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    try (final var stream = Files.list(folder)) {
      return stream
          .filter(p -> p.getFileName().toString().matches("[a-f0-9]{64}\\.qchem"))
          .limit(1001)
          .sorted(
              Comparator.comparing(
                      (Path p) -> {
                        try {
                          return Files.getLastModifiedTime(p).toInstant();
                        } catch (final IOException exception) {
                          return java.time.Instant.MIN;
                        }
                      })
                  .reversed())
          .toList();
    }
  }

  public String write(
      final Path file,
      final LaboratoryRegistryCodec.Document document,
      final String expectedFingerprint)
      throws IOException {
    final Path target = file.toAbsolutePath().normalize();
    Files.createDirectories(target.getParent());
    try (final var channel =
            FileChannel.open(
                target.resolveSibling(target.getFileName() + ".lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        final var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("Реестр сохраняется другим окном.");
      }
      byte[] previous = null;
      if (Files.exists(target)) {
        previous = bytes(target);
        if (expectedFingerprint == null
            || !expectedFingerprint.equals(LaboratoryDesignCodec.fingerprint(previous))) {
          throw new IOException(
              "Реестр изменён другим окном. Загрузите актуальную версию; текущие данные не перезаписаны.");
        }
        final var old = codec.read(LaboratoryFileRepository.decode(previous)).registry();
        final var next = document.registry();
        if (next.nextNumber() < old.nextNumber()) {
          throw new IOException("Ранее выданные номера реестра нельзя использовать заново.");
        }
        for (final var compound : old.compounds()) {
          final var replacement =
              next.compounds().stream()
                  .filter(c -> c.id().equals(compound.id()))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IOException(
                              "Зарегистрированные соединения нельзя удалить: сохраните историю и связи."));
          if (!replacement.molecule().equals(compound.molecule())
              || !replacement.registeredAt().equals(compound.registeredAt())) {
            throw new IOException(
                "Структура зарегистрированного соединения неизменна; зарегистрируйте другую структуру отдельным номером.");
          }
        }
      } else if (expectedFingerprint != null) {
        throw new IOException("Реестр удалён или перемещён. Проверьте папку проекта.");
      }
      final byte[] data = LaboratoryFileRepository.encode(codec.write(document));
      if (!codec.read(LaboratoryFileRepository.decode(data)).equals(document)) {
        throw new IOException("Контрольное чтение реестра не совпало с исходными данными.");
      }
      if (previous != null && Arrays.equals(previous, data)) {
        return expectedFingerprint;
      }
      final Path temporary = Files.createTempFile(target.getParent(), ".registry-", ".tmp");
      try {
        durable(temporary, data);
        if (previous != null) {
          final Path history = target.resolveSibling(target.getFileName() + ".history");
          Files.createDirectories(history);
          final Path revision = history.resolve(expectedFingerprint + ".qchem");
          if (!Files.exists(revision)) {
            final Path snapshot = Files.createTempFile(history, ".revision-", ".tmp");
            try {
              durable(snapshot, previous);
              try {
                Files.move(snapshot, revision);
              } catch (final FileAlreadyExistsException ignored) {
              }
            } finally {
              Files.deleteIfExists(snapshot);
            }
          }
          if (!LaboratoryDesignCodec.fingerprint(bytes(target)).equals(expectedFingerprint)) {
            throw new IOException("Реестр изменился во время сохранения; правки не перезаписаны.");
          }
          Files.move(
              temporary,
              target,
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } else {
          Files.move(temporary, target);
        }
        return LaboratoryDesignCodec.fingerprint(data);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (final java.nio.channels.OverlappingFileLockException exception) {
      throw new IOException("Реестр уже сохраняется в другом окне.", exception);
    }
  }

  private static byte[] bytes(final Path file) throws IOException {
    if (Files.size(file) > LaboratoryRegistryCodec.MAX_BYTES) {
      throw new IOException("Реестр превышает 32 МиБ.");
    }
    try (final var stream = Files.newInputStream(file)) {
      final byte[] data = stream.readNBytes(LaboratoryRegistryCodec.MAX_BYTES + 1);
      if (data.length > LaboratoryRegistryCodec.MAX_BYTES) {
        throw new IOException("Реестр вырос сверх допустимого размера.");
      }
      return data;
    }
  }

  private static void durable(final Path file, final byte[] data) throws IOException {
    try (final var channel =
        FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
      final var buffer = ByteBuffer.wrap(data);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
  }
}
