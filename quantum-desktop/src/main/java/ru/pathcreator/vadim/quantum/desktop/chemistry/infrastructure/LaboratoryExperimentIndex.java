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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CancellationException;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

/** Bounded, read-only index of experiment documents. No JavaFX or implicit file writes. */
public final class LaboratoryExperimentIndex {
  public record Entry(Path path, String title, String search, String problem, boolean directory) {
    public boolean matches(final String query) {
      return Arrays.stream(normalize(query).split(" ")).allMatch(this.search::contains);
    }
  }

  public record Limits(int entries, int depth, long bytes) {
    public Limits {
      if (entries < 1 || depth < 1 || bytes < 1) {
        throw new IllegalArgumentException("Пределы индекса должны быть положительными.");
      }
    }
  }

  public record Snapshot(List<Entry> entries, List<String> diagnostics, int visited) {
    public Snapshot {
      entries = List.copyOf(entries);
      diagnostics = List.copyOf(diagnostics);
    }

    public boolean complete() {
      return this.diagnostics.isEmpty();
    }
  }

  private static final long FILE_LIMIT = LaboratoryFileCodec.MAX_DOCUMENT_BYTES;
  public static final long PROJECT_SCAN_BYTES = 256L * 1024 * 1024;
  private static final Limits DEFAULT_LIMITS = new Limits(10000, 24, PROJECT_SCAN_BYTES);

  public Snapshot scan(final Path directory) throws IOException {
    return this.scan(directory, DEFAULT_LIMITS);
  }

  public Snapshot scan(final Path directory, final Limits limits) throws IOException {
    return this.scan(directory, limits, (path, drawing) -> {});
  }

  public Snapshot scan(
      final Path directory,
      final Limits limits,
      final java.util.function.BiConsumer<
              Path,
              ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing>
          accepted)
      throws IOException {
    final Path root = directory.toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) {
      throw new IOException("Папка недоступна: " + root);
    }
    final List<Entry> entries = new ArrayList<>();
    final Set<String> diagnostics = new LinkedHashSet<>();
    final int[] visited = {0};
    final long[] bytes = {0};
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          private boolean admit() {
            cancelled();
            if (visited[0] >= limits.entries()) {
              diagnostics.add(
                  "Достигнут предел "
                      + limits.entries()
                      + " элементов дерева. Выберите более узкую папку.");
              return false;
            }
            visited[0]++;
            return true;
          }

          @Override
          public FileVisitResult preVisitDirectory(
              final Path path, final BasicFileAttributes attributes) {
            if (!this.admit()) {
              return FileVisitResult.TERMINATE;
            }
            if (!path.equals(root)) {
              final String name = path.getFileName().toString();
              if (name.startsWith(".") || name.endsWith(".history") || name.endsWith(".assets")) {
                return FileVisitResult.SKIP_SUBTREE;
              }
              if (root.equals(path.getParent())) {
                entries.add(new Entry(path, name, normalize(name), null, true));
              }
              if (root.relativize(path).getNameCount() >= limits.depth()) {
                diagnostics.add(
                    "Достигнут предел глубины "
                        + limits.depth()
                        + ". Вложенные папки просмотрены не полностью.");
                return FileVisitResult.SKIP_SUBTREE;
              }
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(final Path path, final BasicFileAttributes attributes) {
            if (!this.admit()) {
              return FileVisitResult.TERMINATE;
            }
            if (!attributes.isRegularFile()
                || !path.toString().toLowerCase(Locale.ROOT).endsWith(".qchem")) {
              return FileVisitResult.CONTINUE;
            }
            try {
              if (attributes.size() > FILE_LIMIT) {
                throw new IOException("Файл больше поддерживаемых 64 МиБ.");
              }
              if (attributes.size() > limits.bytes() - bytes[0]) {
                diagnostics.add(
                    "Достигнут предел объёма чтения индекса. Выберите более узкую папку.");
                return FileVisitResult.TERMINATE;
              }
              // Bound the actual read too: the file may grow after the directory attributes were
              // read.
              final long remaining = Math.min(FILE_LIMIT, limits.bytes() - bytes[0]);
              final byte[] data;
              try (final var input = Files.newInputStream(path)) {
                data = input.readNBytes((int) remaining + 1);
              }
              bytes[0] += data.length;
              cancelled();
              if (data.length > remaining) {
                throw new IOException("Файл изменился или превышает предел чтения индекса.");
              }
              final String text = LaboratoryFileRepository.decode(data);
              if (!text.contains("laboratory-experiment ")) {
                final var parsed =
                    new ru.pathcreator.vadim.quantum.chemistry.storage.application
                            .ChemistryStorageService()
                        .read(text);
                if (!parsed.success()) {
                  throw new IOException(
                      "Повреждённый или неподдерживаемый QCHEM: "
                          + parsed.diagnostics().stream()
                              .limit(3)
                              .map(diagnostic -> diagnostic.message())
                              .collect(java.util.stream.Collectors.joining("; ")));
                }
                return FileVisitResult.CONTINUE;
              }
              final var drawing = new LaboratoryFileCodec().read(text);
              final var experiment = drawing.experiment();
              cancelled();
              final StringBuilder search =
                  new StringBuilder(path.toString())
                      .append(' ')
                      .append(experiment.id())
                      .append(' ')
                      .append(experiment.title())
                      .append(' ')
                      .append(experiment.notes());
              experiment
                  .components()
                  .forEach(
                      component ->
                          search
                              .append(' ')
                              .append(component.molecule().displayName())
                              .append(' ')
                              .append(component.molecule().formula().hillNotation()));
              experiment
                  .components()
                  .forEach(
                      component ->
                          ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity
                              .read(component.molecule())
                              .filter(
                                  identity ->
                                      new ru.pathcreator.vadim.quantum.chemistry.drawing
                                              .infrastructure.identity.CdkCompoundIdentifiers()
                                          .current(drawing.layouts().get(component.id()), identity))
                              .ifPresent(
                                  identity ->
                                      search.append(' ').append(identity.searchableText())));
              appendConditions(search, experiment.conditions());
              experiment
                  .stages()
                  .forEach(
                      stage -> {
                        search
                            .append(' ')
                            .append(stage.title())
                            .append(' ')
                            .append(
                                LaboratoryReactionClassificationText.searchable(
                                    stage.classification()));
                        appendConditions(search, stage.conditions());
                      });
              experiment.journal().forEach(entry -> search.append(' ').append(entry.text()));
              experiment.attachments().forEach(item -> search.append(' ').append(item.name()));
              experiment
                  .samples()
                  .forEach(
                      item ->
                          search
                              .append(' ')
                              .append(item.code())
                              .append(' ')
                              .append(item.balance().label())
                              .append(' ')
                              .append(item.balance().location())
                              .append(' ')
                              .append(item.balance().container())
                              .append(' ')
                              .append(item.stageTitle()));
              experiment
                  .analyses()
                  .forEach(
                      item ->
                          search
                              .append(' ')
                              .append(item.title())
                              .append(' ')
                              .append(item.data().method())
                              .append(' ')
                              .append(item.sample())
                              .append(' ')
                              .append(item.source())
                              .append(' ')
                              .append(item.note()));
              accepted.accept(path, drawing);
              entries.add(
                  new Entry(path, experiment.title(), normalize(search.toString()), null, false));
            } catch (final CancellationException exception) {
              throw exception;
            } catch (final Exception exception) {
              this.problem(path, exception);
            }
            return FileVisitResult.CONTINUE;
          }

          private void problem(final Path path, final Exception exception) {
            final String problem =
                exception.getMessage() == null ? exception.toString() : exception.getMessage();
            entries.add(
                new Entry(
                    path, "⚠ " + path.getFileName(), normalize(path.toString()), problem, false));
            diagnostics.add(
                "Часть файлов или папок не прочитана. Подробности — в строках с предупреждением.");
          }

          @Override
          public FileVisitResult visitFileFailed(final Path path, final IOException exception) {
            if (!this.admit()) {
              return FileVisitResult.TERMINATE;
            }
            this.problem(path, exception);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(final Path path, final IOException exception) {
            cancelled();
            if (exception != null) {
              this.problem(path, exception);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    cancelled();
    entries.sort(
        Comparator.comparing(Entry::directory)
            .reversed()
            .thenComparing(Entry::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.path().toString()));
    return new Snapshot(entries, List.copyOf(diagnostics), visited[0]);
  }

  public static String normalize(final String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replace('ё', 'е')
        .replaceAll("[\\p{Z}\\s]+", " ")
        .strip();
  }

  private static void cancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Индексация отменена");
    }
  }

  private static void appendConditions(
      final StringBuilder text, final ReactionConditions conditions) {
    if (conditions.note() != null) {
      text.append(' ').append(conditions.note());
    }
    conditions
        .components()
        .forEach(
            material ->
                text.append(' ').append(material.name()).append(' ').append(material.note()));
  }
}
