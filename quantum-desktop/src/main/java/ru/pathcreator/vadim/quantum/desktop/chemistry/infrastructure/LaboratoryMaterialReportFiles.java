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
import java.time.ZoneId;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryMaterialReport;

/** Read-only project discovery and explicit CSV exports. Invalid ledgers are reported, never treated as empty. */
public final class LaboratoryMaterialReportFiles {
  public record Scan(
      Path root,
      List<LaboratoryMaterialReport.Source> sources,
      Map<String, String> experiments,
      List<String> warnings) {
    public Scan {
      sources = List.copyOf(sources);
      experiments = Map.copyOf(experiments);
      warnings = List.copyOf(warnings);
    }

    public String experiment(final String project, final String id) {
      return id == null || id.isEmpty()
          ? "Без привязки к опыту"
          : this.experiments.getOrDefault(project + "\n" + id, id);
    }
  }

  private record Named(Path path, String id, String title) {}

  private LaboratoryMaterialReportFiles() {}

  public static Scan scan(final Path directory) throws IOException {
    final Path root = directory.toAbsolutePath().normalize();
    final List<LaboratoryMaterialReport.Source> sources = new ArrayList<>();
    final List<Named> named = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    final int[] visited = {0};
    final long[] bytesRead = {0};
    Files.walkFileTree(
        root,
        EnumSet.noneOf(FileVisitOption.class),
        24,
        new SimpleFileVisitor<>() {
          private FileVisitResult limit() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
              throw new java.io.InterruptedIOException("Чтение сводки отменено.");
            }
            if (++visited[0] > 10000) {
              warnings.add("Прочитана только часть папки: достигнут предел 10000 элементов.");
              return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult preVisitDirectory(
              final Path path, final BasicFileAttributes attributes) throws IOException {
            final var decision = limit();
            if (decision != FileVisitResult.CONTINUE) {
              return decision;
            }
            final String name = Objects.toString(path.getFileName(), "");
            return !path.equals(root)
                    && (name.startsWith(".")
                        || name.endsWith(".assets")
                        || name.endsWith(".history"))
                ? FileVisitResult.SKIP_SUBTREE
                : FileVisitResult.CONTINUE;
          }

          public FileVisitResult visitFile(final Path path, final BasicFileAttributes attributes)
              throws IOException {
            final var decision = limit();
            if (decision != FileVisitResult.CONTINUE) {
              return decision;
            }
            if (attributes.isDirectory()) {
              warnings.add("Предел глубины: " + path);
              return FileVisitResult.CONTINUE;
            }
            if (!attributes.isRegularFile()
                || !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".qchem")) {
              return FileVisitResult.CONTINUE;
            }
            try {
              if (attributes.size() > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
                throw new IOException("Больше поддерживаемых 64 МиБ; не включён в сводку.");
              }
              final long remaining =
                  Math.min(
                      LaboratoryFileCodec.MAX_DOCUMENT_BYTES,
                      LaboratoryExperimentIndex.PROJECT_SCAN_BYTES - bytesRead[0]);
              if (attributes.size() > remaining) {
                warnings.add("Достигнут предел 256 МиБ чтения сводки. Выберите более узкую папку.");
                return FileVisitResult.TERMINATE;
              }
              final byte[] bytes;
              try (final var input = Files.newInputStream(path)) {
                bytes = input.readNBytes((int) remaining + 1);
              }
              bytesRead[0] += bytes.length;
              if (bytes.length > remaining) {
                throw new IOException("Файл вырос сверх допустимого размера чтения сводки.");
              }
              final String text = LaboratoryFileRepository.decode(bytes);
              if (path.getFileName().toString().equalsIgnoreCase("materials.qchem")) {
                sources.add(
                    new LaboratoryMaterialReport.Source(
                        path.getParent().toString(), new LaboratoryInventoryCodec().read(text)));
              } else {
                if (text.contains("laboratory-experiment ")
                    && !text.contains("key=prediction.adapter_version")) {
                  final var experiment = new LaboratoryFileCodec().read(text).experiment();
                  named.add(new Named(path, experiment.id(), experiment.title()));
                }
              }
            } catch (final Exception exception) {
              warnings.add(path + ": " + exception.getMessage());
            }
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult visitFileFailed(final Path path, final IOException exception) {
            warnings.add(path + ": " + exception.getMessage());
            return FileVisitResult.CONTINUE;
          }
        });
    sources.sort(Comparator.comparing(LaboratoryMaterialReport.Source::project));
    final Map<String, String> names = new HashMap<>();
    for (final var item : named) {
      final var project =
          sources.stream()
              .filter(source -> item.path().startsWith(Path.of(source.project())))
              .max(Comparator.comparingInt(source -> Path.of(source.project()).getNameCount()));
      if (project.isPresent()) {
        final String key = project.get().project() + "\n" + item.id();
        names.merge(
            key,
            item.title(),
            (before, after) ->
                before.equals(after) ? before : item.id() + " (разные названия в копиях)");
      }
    }
    return new Scan(root, sources, names, warnings);
  }

  public enum Export {
    SUMMARY,
    OPERATIONS,
    BALANCES
  }

  public static void export(
      final Path target,
      final Scan scan,
      final LaboratoryMaterialReport.Report report,
      final LaboratoryMaterialReport.Filter filter,
      final LaboratoryMaterialReport.Group group,
      final ZoneId zone,
      final Export kind)
      throws IOException {
    if (kind == Export.BALANCES ? report.balances().isEmpty() : report.entries().isEmpty()) {
      throw new IOException("Нет данных для экспорта. Проверьте фильтры и диагностику чтения.");
    }
    if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new IOException("Для отчёта выберите файл с расширением .csv.");
    }
    final StringBuilder csv = new StringBuilder("\ufeff");
    final List<String> warnings = new ArrayList<>(scan.warnings());
    warnings.addAll(report.warnings());
    final String warning = String.join(" | ", warnings);
    final Object[] context = {
      filter.from(),
      filter.until(),
      zone.toString(),
      warning,
      filter.project(),
      filter.experiment(),
      filter.material(),
      filter.batch(),
      scan.root().toString()
    };
    final Object[] contextNames = {
      "Начало UTC",
      "Конец UTC (не включён)",
      "Часовой пояс",
      "Предупреждения",
      "Фильтр проекта",
      "Фильтр опыта (только движения)",
      "Фильтр материала",
      "Фильтр партии",
      "Корень сканирования"
    };
    switch (kind) {
      case OPERATIONS -> {
        line(
            csv,
            contextNames,
            "Проект",
            "Опыт",
            "ID опыта",
            "Материал",
            "ID материала",
            "Партия",
            "ID партии",
            "Операция",
            "ID операции",
            "Время UTC",
            "Исходный расход",
            "Количество",
            "Единица",
            "Изменение остатка",
            "Основание");
        for (final var entry : report.entries()) {
          final var operation = entry.operation();
          line(
              csv,
              context,
              entry.project(),
              scan.experiment(entry.project(), operation.experimentId()),
              operation.experimentId(),
              entry.material().name(),
              entry.material().id(),
              entry.batch().label(),
              entry.batch().id(),
              operation.kind().toString(),
              operation.id(),
              operation.recordedAt().toString(),
              operation.returnsOperationId(),
              entry.quantity(),
              entry.unit().toString(),
              entry.stockChange(),
              operation.reason());
        }
      }
      case SUMMARY -> {
        line(
            csv,
            contextNames,
            "Проект",
            "Группировка",
            "Группа",
            "ID группы",
            "Приход",
            "Взято",
            "Возвращено",
            "Чистый расход в опыты",
            "Корректировка +",
            "Корректировка −",
            "Изменение остатка",
            "Единица",
            "Операций");
        for (final var row : report.rows()) {
          line(
              csv,
              context,
              row.project(),
              group.name(),
              group == LaboratoryMaterialReport.Group.EXPERIMENT
                  ? scan.experiment(row.project(), row.groupId())
                  : row.label(),
              row.groupId(),
              row.received(),
              row.used(),
              row.returned(),
              row.netUsed(),
              row.increased(),
              row.decreased(),
              row.stockChange(),
              row.unit().toString(),
              row.operations());
        }
      }
      case BALANCES -> {
        line(
            csv,
            contextNames,
            "Проект",
            "Материал",
            "ID материала",
            "Партия",
            "ID партии",
            "Место",
            "Остаток на начало",
            "Остаток на конец",
            "Единица");
        for (final var row : report.balances()) {
          line(
              csv,
              context,
              row.project(),
              row.material().name(),
              row.material().id(),
              row.batch().label(),
              row.batch().id(),
              row.batch().location(),
              row.opening(),
              row.closing(),
              row.unit().toString());
        }
      }
    }
    final Path absolute = target.toAbsolutePath().normalize();
    Files.createDirectories(absolute.getParent());
    final Path temporary = Files.createTempFile(absolute.getParent(), ".material-report-", ".tmp");
    try {
      interrupted();
      Files.writeString(temporary, csv);
      try (final var channel =
          java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        channel.force(true);
      }
      interrupted();
      Files.move(
          temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void interrupted() throws java.io.InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.io.InterruptedIOException("Экспорт сводки отменён.");
    }
  }

  private static void line(
      final StringBuilder target, final Object[] context, final Object... values)
      throws java.io.InterruptedIOException {
    interrupted();
    final List<Object> combined = new ArrayList<>(Arrays.asList(values));
    combined.addAll(Arrays.asList(context));
    for (int index = 0; index < combined.size(); index++) {
      if (index > 0) {
        target.append(';');
      }
      final Object value = combined.get(index);
      String text =
          value == null
              ? ""
              : value instanceof java.math.BigDecimal number
                  ? number.toPlainString()
                  : value.toString();
      if (value instanceof String
          && !text.stripLeading().isEmpty()
          && "=+-@\t\r\n".indexOf(text.stripLeading().charAt(0)) >= 0) {
        text = "'" + text;
      }
      target.append('"').append(text.replace("\"", "\"\"")).append('"');
    }
    target.append("\r\n");
  }
}
