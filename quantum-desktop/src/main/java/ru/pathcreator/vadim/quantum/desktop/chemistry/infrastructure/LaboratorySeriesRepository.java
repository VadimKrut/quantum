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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySeriesPlan.Prepared;

/** Writes and verifies a private staging folder, then publishes it with a no-replace rename. */
public final class LaboratorySeriesRepository {
  public record Result(Path folder, List<Path> documents) {
    public Result {
      documents = List.copyOf(documents);
    }
  }

  public static String folderName(final String text) {
    final String name = text.strip();
    if (name.isEmpty()
        || name.length() > 100
        || name.chars().anyMatch(c -> Character.isISOControl(c) || "<>:\"/\\|?*".indexOf(c) >= 0)
        || name.endsWith(".")
        || name.equals("..")
        || name.startsWith(".")
        || name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) {
      throw new IllegalArgumentException(
          "Укажите имя новой папки: до 100 символов, без разделителей пути и служебных имён.");
    }
    return name;
  }

  public Result write(
      final Path parent,
      final String name,
      final List<Prepared> variants,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    if (variants.isEmpty() || variants.size() > 48) {
      throw new IllegalArgumentException("Серия должна содержать от 1 до 48 вариантов.");
    }
    if (variants.stream().map(p -> p.drawing().experiment().id()).distinct().count()
        != variants.size()) {
      throw new IllegalArgumentException(
          "У каждого варианта должен быть независимый идентификатор опыта.");
    }
    for (final var p : variants) {
      final var e = p.drawing().experiment();
      if (!e.attachments().isEmpty()
          || !e.journal().isEmpty()
          || e.components().stream()
              .anyMatch(
                  c ->
                      c.actual() != null
                          || c.evidence()
                              != ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                  .LaboratoryEvidence.PLANNED)) {
        throw new IllegalArgumentException(
            "Серия должна содержать только планы без прежних фактических результатов.");
      }
    }
    final Path root = parent.toAbsolutePath().normalize().toRealPath();
    if (!Files.isDirectory(root)) {
      throw new IOException("Папка назначения недоступна.");
    }
    final Path target = root.resolve(folderName(name));
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      throw new FileAlreadyExistsException("Папка уже существует. Выберите другое имя: " + target);
    }
    check(cancelled);
    final Path staging = Files.createTempDirectory(root, ".qchem-series-");
    boolean published = false;
    final List<String> names = new ArrayList<>();
    try {
      final var codec = new LaboratoryFileCodec();
      long total = 0;
      for (int i = 0; i < variants.size(); i++) {
        check(cancelled);
        final String filename = String.format(Locale.ROOT, "%02d.qchem", i + 1);
        names.add(filename);
        final var drawing = variants.get(i).drawing();
        progress.accept(
            "Готовлю " + (i + 1) + " / " + variants.size() + ": " + drawing.experiment().title());
        final byte[] data = codec.write(drawing).getBytes(StandardCharsets.UTF_8);
        total += data.length;
        if (total > 64L * 1024 * 1024) {
          throw new IOException("Серия превышает 64 МиБ. Разделите её на несколько папок.");
        }
        final Path file = staging.resolve(filename);
        try (final var channel =
            FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
          final var bytes = ByteBuffer.wrap(data);
          while (bytes.hasRemaining()) {
            check(cancelled);
            channel.write(bytes);
          }
          channel.force(true);
        }
        if (!new LaboratoryFileRepository().read(file).drawing().equals(drawing)) {
          throw new IOException(
              "Повторное чтение варианта не совпало с предпросмотром: " + filename);
        }
      }
      check(cancelled);
      progress.accept("Публикую проверенную серию…");
      check(cancelled);
      Files.move(staging, target);
      published = true;
      return new Result(target, names.stream().map(target::resolve).toList());
    } finally {
      if (!published) {
        // Only paths allocated by this invocation are eligible for cleanup. No recursive traversal.
        for (final String filename : names) {
          Files.deleteIfExists(staging.resolve(filename));
        }
        Files.deleteIfExists(staging);
      }
    }
  }

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Создание серии остановлено; готовая папка не опубликована.");
    }
  }
}
