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

/** Fingerprint-checked study saves and all-or-nothing publication of a prepared run matrix. */
public final class LaboratoryDesignRepository {

  private static final long MAX_RUN_BYTES = 2L * LaboratoryFileCodec.MAX_DOCUMENT_BYTES;
  public static final long MAX_PUBLICATION_BYTES = MAX_RUN_BYTES + LaboratoryDesignCodec.MAX_BYTES;

  public record Opened(LaboratoryDesignCodec.Document document, String fingerprint) {}

  public record Published(Path folder, Path designFile, List<Path> experiments, Opened opened) {
    public Published {
      experiments = List.copyOf(experiments);
    }
  }

  private final LaboratoryDesignCodec codec = new LaboratoryDesignCodec();

  public Opened read(final Path file) throws IOException {
    final byte[] bytes = bytes(file);
    return new Opened(
        this.codec.read(LaboratoryFileRepository.decode(bytes)),
        LaboratoryDesignCodec.fingerprint(bytes));
  }

  public String write(
      final Path file,
      final LaboratoryDesignCodec.Document document,
      final String expectedFingerprint)
      throws IOException {
    final Path target = file.toAbsolutePath().normalize();
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
      final Path temporary = Files.createTempFile(target.getParent(), ".qdesign-", ".tmp");
      try {
        durable(temporary, data, false);
        if (previous != null) {
          final Path history = target.resolveSibling(target.getFileName() + ".history");
          Files.createDirectories(history);
          final Path revision =
              history.resolve(LaboratoryDesignCodec.fingerprint(previous) + ".qdesign");
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

  public Published publish(
      final Path parent,
      final String folderName,
      final LaboratoryDesignCodec.Document document,
      final List<LaboratorySeriesPlan.Prepared> plans,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    if (plans.isEmpty() || plans.size() > 256 || plans.size() != document.design().runs().size()) {
      throw new IllegalArgumentException("Проверьте полную матрицу перед созданием опытов.");
    }
    if (document.design().runs().stream().anyMatch(r -> !r.experimentFile().isEmpty())) {
      throw new IllegalArgumentException(
          "Для этой матрицы опыты уже созданы. Откройте их или создайте новый независимый план.");
    }
    if (plans.stream().map(p -> p.drawing().experiment().id()).distinct().count() != plans.size()) {
      throw new IllegalArgumentException(
          "У запусков должны быть независимые идентификаторы опытов.");
    }
    for (final var plan : plans) {
      final var e = plan.drawing().experiment();
      if (!e.journal().isEmpty()
          || !e.analyses().isEmpty()
          || !e.samples().isEmpty()
          || !e.attachments().isEmpty()
          || !e.procedure().events().isEmpty()
          || !e.analyticalEvidence().entries().isEmpty()
          || !plannedOperations(e.operations())
          || e.components().stream()
              .anyMatch(
                  c ->
                      c.actual() != null
                          || c.evidence()
                              != ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                  .LaboratoryEvidence.PLANNED)) {
        throw new IllegalArgumentException(
            "В новые опыты переносятся только планы; фактические данные шаблона должны быть исключены.");
      }
    }
    final Path root = parent.toAbsolutePath().normalize().toRealPath();
    final Path target = root.resolve(LaboratorySeriesRepository.folderName(folderName));
    if (!Files.isDirectory(root) || Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Выберите существующий проект и новое имя папки исследования.");
    }
    final Path staging = Files.createTempDirectory(root, ".qdesign-series-");
    final var allocated = new ArrayList<Path>();
    final var runs = new ArrayList<Run>();
    boolean published = false;
    try {
      final var fileCodec = new LaboratoryFileCodec();
      long total = 0;
      for (int i = 0; i < plans.size(); i++) {
        check(cancelled);
        progress.accept("Подготавливаю запуск " + (i + 1) + " из " + plans.size());
        final String name = String.format(Locale.ROOT, "run_%03d.qchem", i + 1);
        final var drawing = plans.get(i).drawing();
        final byte[] data = LaboratoryFileRepository.encode(fileCodec.write(drawing));
        if (data.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
          throw new IOException("Опыт превышает 64 МиБ.");
        }
        total += data.length;
        if (total > MAX_RUN_BYTES) {
          throw new IOException("Опыты превышают 128 МиБ. Разделите исследование.");
        }
        final Path file = staging.resolve(name);
        allocated.add(file);
        durable(file, data, true);
        if (!fileCodec
            .read(
                LaboratoryFileRepository.decode(
                    bytes(file, LaboratoryFileCodec.MAX_DOCUMENT_BYTES)))
            .equals(drawing)) {
          throw new IOException("Контрольное чтение опыта не совпало с предпросмотром.");
        }
        final var run = document.design().runs().get(i);
        runs.add(
            new Run(
                run.id(),
                run.standardOrder(),
                run.replicate(),
                run.center(),
                run.levels(),
                run.position(),
                name,
                drawing.experiment().id(),
                run.response(),
                run.responseNote(),
                run.responseOrigin(),
                run.responseSource(),
                run.responseAnalysis()));
      }
      final var linked =
          new LaboratoryDesignCodec.Document(document.source(), document.design().withRuns(runs));
      final Path study = staging.resolve("study.qdesign");
      allocated.add(study);
      final byte[] data = LaboratoryFileRepository.encode(this.codec.write(linked));
      if (total + data.length > MAX_PUBLICATION_BYTES) {
        throw new IOException(
            "Общий объём исследования превышает "
                + MAX_PUBLICATION_BYTES / (1024 * 1024)
                + " МиБ.");
      }
      durable(study, data, true);
      final var opened = this.read(study);
      if (!opened.document().equals(linked)) {
        throw new IOException("Контрольное чтение исследования не совпало с планом.");
      }
      check(cancelled);
      Files.move(staging, target);
      published = true;
      return new Published(
          target,
          target.resolve("study.qdesign"),
          runs.stream().map(r -> target.resolve(r.experimentFile())).toList(),
          opened);
    } finally {
      if (!published) {
        for (final var file : allocated) {
          Files.deleteIfExists(file);
        }
        Files.deleteIfExists(staging);
      }
    }
  }

  private static boolean plannedOperations(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations
          operations) {
    if (!operations.timePoints().isEmpty()) {
      return false;
    }
    for (final var workup : operations.workups()) {
      if (workup.correctsId() != null
          || workup.steps().stream().anyMatch(s -> s.actual() != null || !s.source().isBlank())
          || workup.losses().stream().anyMatch(l -> l.actual() != null || !l.source().isBlank())) {
        return false;
      }
    }
    for (final var run : operations.purifications()) {
      if (run.correctsId() != null
          || run.inputActual() != null
          || run.inputSampleId() != null
          || !run.source().isBlank()
          || !run.pools().isEmpty()
          || run.fractions().stream()
              .anyMatch(
                  f ->
                      f.actual() != null
                          || f.purityPercent() != null
                          || f.sampleId() != null
                          || !f.source().isBlank())
          || run.losses().stream().anyMatch(l -> l.actual() != null || !l.source().isBlank())) {
        return false;
      }
    }
    return true;
  }

  public Path experiment(final Path designFile, final Run run) throws IOException {
    if (run.experimentFile().isEmpty()) {
      throw new IOException("Для этого запуска опыт ещё не создан.");
    }
    final Path root = designFile.toAbsolutePath().normalize().getParent().toRealPath();
    final Path file = root.resolve(run.experimentFile()).toRealPath();
    if (!file.startsWith(root)
        || !new LaboratoryFileRepository()
            .read(file)
            .drawing()
            .experiment()
            .id()
            .equals(run.experimentId())) {
      throw new IOException(
          "Связанный опыт перемещён или заменён. Проверьте файл перед открытием.");
    }
    return file;
  }

  private static byte[] bytes(final Path file) throws IOException {
    return bytes(file, LaboratoryDesignCodec.MAX_BYTES);
  }

  private static byte[] bytes(final Path file, final int maximum) throws IOException {
    if (Files.size(file) > maximum) {
      throw new IOException("Файл превышает " + maximum / (1024 * 1024) + " МиБ.");
    }
    try (final var input = Files.newInputStream(file)) {
      final byte[] bytes = input.readNBytes(maximum + 1);
      if (bytes.length > maximum) {
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

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException(
          "Создание исследования остановлено до публикации.");
    }
  }
}
