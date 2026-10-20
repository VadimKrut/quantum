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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipFile;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Each running workspace owns a lock; a crash releases it without deleting its durable draft. */
public final class LaboratoryRecoveryStore implements AutoCloseable {
  private final Path root;
  private final Path directory;
  private final FileChannel channel;
  private final FileLock lock;

  public LaboratoryRecoveryStore(final Path root) throws IOException {
    this.root = root.toAbsolutePath().normalize();
    Files.createDirectories(this.root);
    this.directory = Files.createDirectory(this.root.resolve(UUID.randomUUID().toString()));
    this.channel =
        FileChannel.open(
            this.directory.resolve("session.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE);
    this.lock = this.channel.lock();
  }

  public record Draft(Path archive, String title, String original, java.time.Instant savedAt) {
    public String toString() {
      return this.title
          + " · "
          + this.savedAt
          + (this.original.isBlank() ? "" : " · " + this.original);
    }
  }

  public record Recovered(LaboratoryDrawing drawing, Properties fields, Path source) {}

  public synchronized void checkpoint(
      final LaboratoryDrawing drawing, final Path source, final Properties fields)
      throws IOException {
    final Properties metadata = new Properties();
    metadata.putAll(fields);
    metadata.setProperty("version", "1");
    metadata.setProperty("title", drawing.experiment().title());
    metadata.setProperty("savedAt", java.time.Instant.now().toString());
    new LaboratoryArchive()
        .export(this.directory.resolve("draft.qchempack"), source, drawing, metadata);
  }

  public synchronized void clear() throws IOException {
    Files.deleteIfExists(this.directory.resolve("draft.qchempack"));
  }

  public synchronized void discard() throws IOException {
    if (!this.directory.getParent().equals(this.root) || Files.isSymbolicLink(this.directory)) {
      throw new IOException("Недопустимый каталог сеанса восстановления.");
    }
    // Only this session's generated files, while its exclusive lock is still held.
    try (final var paths = Files.walk(this.directory)) {
      for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        if (!path.equals(this.directory) && !path.equals(this.directory.resolve("session.lock"))) {
          Files.deleteIfExists(path);
        }
      }
    }
  }

  public List<Draft> available() throws IOException {
    final ArrayList<Draft> drafts = new ArrayList<>();
    try (final var paths = Files.list(this.root)) {
      for (final Path candidate : paths.filter(Files::isDirectory).toList()) {
        if (Thread.currentThread().isInterrupted()) {
          throw new java.util.concurrent.CancellationException("Чтение авточерновиков отменено");
        }
        if (candidate.equals(this.directory) || Files.isSymbolicLink(candidate)) {
          continue;
        }
        try (final FileChannel other =
            FileChannel.open(
                candidate.resolve("session.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
          try (final FileLock claim = other.tryLock()) {
            if (claim == null) {
              continue;
            }
            final Path archive = candidate.resolve("draft.qchempack");
            if (!Files.isRegularFile(archive)) {
              continue;
            }
            try {
              final Properties fields = metadata(archive);
              drafts.add(
                  new Draft(
                      archive,
                      fields.getProperty("title", "Опыт"),
                      fields.getProperty("original", ""),
                      Files.getLastModifiedTime(archive).toInstant()));
            } catch (final IOException damaged) {
              drafts.add(
                  new Draft(
                      archive,
                      "Повреждённый черновик — " + candidate.getFileName(),
                      "",
                      Files.getLastModifiedTime(archive).toInstant()));
            }
          } catch (final OverlappingFileLockException active) {
            /* Another workspace in this JVM. */
          }
        }
      }
    }
    drafts.sort(Comparator.comparing(Draft::savedAt).reversed());
    return List.copyOf(drafts);
  }

  public synchronized Recovered recover(final Draft draft) throws IOException {
    final Path candidate = draft.archive().toAbsolutePath().normalize();
    if (!candidate.getParent().getParent().equals(this.root)
        || Files.isSymbolicLink(candidate.getParent())) {
      throw new IOException("Черновик находится вне каталога восстановления.");
    }
    try (final FileChannel other =
            FileChannel.open(
                candidate.getParent().resolve("session.lock"), StandardOpenOption.WRITE);
        final FileLock claim = other.tryLock()) {
      if (claim == null) {
        throw new IOException("Черновик занят другим окном Studio.");
      }
      final Properties fields = metadata(candidate);
      final Path extracted = this.directory.resolve("recovered-" + UUID.randomUUID() + ".qchem");
      final LaboratoryDrawing drawing = new LaboratoryArchive().importTo(candidate, extracted);
      // Keep the old draft until the complete replacement is durable in this session.
      this.checkpoint(drawing, extracted, fields);
      return new Recovered(drawing, fields, extracted);
    }
  }

  /** Acknowledge only after the recovered document and its raw fields are visible in the editor. */
  public void acknowledge(final Draft draft) throws IOException {
    final Path candidate = draft.archive().toAbsolutePath().normalize();
    if (!candidate.getParent().getParent().equals(this.root)
        || Files.isSymbolicLink(candidate.getParent())) {
      throw new IOException("Черновик находится вне каталога восстановления.");
    }
    try (final FileChannel other =
            FileChannel.open(
                candidate.getParent().resolve("session.lock"), StandardOpenOption.WRITE);
        final FileLock claim = other.tryLock()) {
      if (claim != null
          && Files.isRegularFile(candidate)
          && Files.getLastModifiedTime(candidate).toInstant().equals(draft.savedAt())) {
        Files.delete(candidate);
      }
    }
  }

  private static Properties metadata(final Path archive) throws IOException {
    try (final ZipFile zip = new ZipFile(archive.toFile())) {
      final var entry = zip.getEntry("recovery.properties");
      if (entry == null
          || entry.getSize() < 0
          || entry.getSize() > LaboratoryArchive.MAX_RECOVERY_BYTES) {
        throw new IOException("Повреждён черновик: " + archive);
      }
      final Properties result = new Properties();
      try (final var input =
          new BoundedInput(
              new java.io.BufferedInputStream(zip.getInputStream(entry)), entry.getSize())) {
        result.load(input);
        if (input.count != entry.getSize()) {
          throw new IOException("Повреждён размер черновика.");
        }
      }
      if (!"1".equals(result.getProperty("version"))) {
        throw new IOException("Неподдерживаемая версия черновика.");
      }
      return result;
    } catch (final java.util.zip.ZipException | IllegalArgumentException damaged) {
      throw new IOException(
          "Повреждён черновик восстановления. Исходный файл оставлен без изменений.", damaged);
    }
  }

  private static final class BoundedInput extends java.io.FilterInputStream {
    private final long expected;
    private long count;

    BoundedInput(final java.io.InputStream input, final long expected) {
      super(input);
      this.expected = expected;
    }

    private void add(final int n) throws IOException {
      if (n > 0) {
        count += n;
        if (count > expected || count > LaboratoryArchive.MAX_RECOVERY_BYTES) {
          throw new IOException("Размер авточерновика превышает заявленный предел.");
        }
      }
    }

    @Override
    public int read() throws IOException {
      final int result = in.read();
      if (result >= 0) {
        add(1);
      }
      return result;
    }

    @Override
    public int read(final byte[] value, final int offset, final int length) throws IOException {
      final int result = in.read(value, offset, length);
      add(result);
      return result;
    }
  }

  public synchronized void close() throws IOException {
    this.lock.release();
    this.channel.close();
  }
}
