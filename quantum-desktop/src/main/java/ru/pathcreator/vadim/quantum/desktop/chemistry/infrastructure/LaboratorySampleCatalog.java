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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CancellationException;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Read-only folder projection over existing samples. No second sample database or inferred stock. */
public final class LaboratorySampleCatalog {
  public record Current(Path path, LaboratoryDrawing drawing, boolean unsaved) {
    public Current {
      if (path != null) {
        path = path.toAbsolutePath().normalize();
      }
      Objects.requireNonNull(drawing);
    }
  }

  public record Analysis(
      String id, String title, String method, LaboratoryAnalysis.Origin origin) {}

  public record Row(
      Path path,
      String experimentTitle,
      LaboratorySampleReference reference,
      LaboratorySample sample,
      LaboratorySample.Balance balance,
      List<Analysis> analyses,
      String search,
      boolean unsaved,
      int copies,
      int sameCode) {
    public Row {
      analyses = List.copyOf(analyses);
    }

    public String key() {
      return Objects.toString(this.path, "@current") + "#" + this.reference.token();
    }

    public String source() {
      return this.path == null ? "Текущий несохранённый опыт" : this.path.toString();
    }

    public String quantity() {
      final var q = this.balance.remaining();
      return q == null
          ? "Не измерено"
          : q.value().stripTrailingZeros().toPlainString() + " " + q.unit();
    }

    public boolean matches(final String query) {
      return Arrays.stream(LaboratoryExperimentIndex.normalize(query).split(" "))
          .allMatch(this.search::contains);
    }

    public String warning() {
      return (this.unsaved ? "Есть несохранённые данные. " : "")
          + (this.copies > 1 ? "Одна ссылка образца найдена в " + this.copies + " файлах. " : "")
          + (this.sameCode > 1 ? "Код повторяется у разных образцов. " : "");
    }

    public boolean labelAllowed() {
      return !this.unsaved && this.copies == 1;
    }
  }

  public record Snapshot(
      Path folder, List<Row> rows, List<String> diagnostics, Instant capturedAt) {
    public Snapshot {
      folder = folder.toAbsolutePath().normalize();
      rows = List.copyOf(rows);
      diagnostics = List.copyOf(diagnostics);
    }

    public boolean complete() {
      return this.diagnostics.isEmpty();
    }

    public List<Row> resolve(final String scanned) {
      final String text = scanned.strip();
      if (text.matches("QCS[0-9]+-.*") && !LaboratorySampleReference.isToken(text)) {
        throw new IllegalArgumentException("Повреждённый или неподдерживаемый код этикетки.");
      }
      return this.rows.stream()
          .filter(
              row ->
                  LaboratorySampleReference.isToken(text)
                      ? row.reference().token().equals(text)
                      : row.sample().code().equalsIgnoreCase(text))
          .toList();
    }
  }

  public Snapshot scan(final Path folder, final Current current) throws IOException {
    final Path root = folder.toAbsolutePath().normalize();
    final List<Row> rows = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    final int[] documents = {0}, skipped = {0};
    final boolean overlay =
        current != null
            && (current.unsaved() || current.path() == null)
            && (current.path() == null || current.path().startsWith(root));
    final var index =
        new LaboratoryExperimentIndex()
            .scan(
                root,
                new LaboratoryExperimentIndex.Limits(20000, 24, 128L * 1024 * 1024),
                (path, drawing) -> {
                  if (++documents[0] > 1000) {
                    skipped[0]++;
                    return;
                  }
                  if (overlay && path.equals(current.path())) {
                    return;
                  }
                  append(rows, notes, path, drawing, false);
                });
    notes.addAll(index.diagnostics());
    index.entries().stream()
        .filter(e -> e.problem() != null)
        .forEach(e -> notes.add(root.relativize(e.path()) + ": " + e.problem()));
    if (overlay) {
      append(
          rows,
          notes,
          current.path(),
          current.drawing(),
          current.unsaved() || current.path() == null);
    } else if (current != null && current.unsaved()) {
      notes.add("Несохранённый текущий опыт находится вне выбранной папки и не включён.");
    }
    if (skipped[0] > 0) {
      notes.add(
          "Каталог ограничен 1000 опытами; часть файлов не включена. Выберите более узкую папку.");
    }
    final Map<LaboratorySampleReference, Integer> references = new HashMap<>();
    final Map<String, Set<LaboratorySampleReference>> codes = new HashMap<>();
    for (final Row row : rows) {
      cancelled();
      references.merge(row.reference(), 1, Integer::sum);
      codes
          .computeIfAbsent(
              LaboratoryExperimentIndex.normalize(row.sample().code()), k -> new HashSet<>())
          .add(row.reference());
    }
    final List<Row> result = new ArrayList<>();
    for (final Row row : rows) {
      result.add(
          new Row(
              row.path(),
              row.experimentTitle(),
              row.reference(),
              row.sample(),
              row.balance(),
              row.analyses(),
              row.search(),
              row.unsaved(),
              references.get(row.reference()),
              codes.get(LaboratoryExperimentIndex.normalize(row.sample().code())).size()));
    }
    result.sort(
        Comparator.comparing((Row row) -> row.sample().code(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Row::key));
    cancelled();
    return new Snapshot(root, result, new ArrayList<>(new LinkedHashSet<>(notes)), Instant.now());
  }

  private static void append(
      final List<Row> rows,
      final List<String> notes,
      final Path path,
      final LaboratoryDrawing drawing,
      final boolean unsaved) {
    final var experiment = drawing.experiment();
    final Set<String> corrected = new HashSet<>();
    experiment
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                corrected.add(a.correctsId());
              }
            });
    final Map<String, List<Analysis>> analyses = new HashMap<>();
    experiment.analyses().stream()
        .filter(a -> a.sampleId() != null && !corrected.contains(a.id()))
        .forEach(
            a ->
                analyses
                    .computeIfAbsent(a.sampleId(), k -> new ArrayList<>())
                    .add(new Analysis(a.id(), a.title(), a.data().method(), a.origin())));
    experiment.analyticalEvidence().latest().stream()
        .filter(a -> a.sampleId() != null)
        .forEach(
            a ->
                analyses
                    .computeIfAbsent(a.sampleId(), k -> new ArrayList<>())
                    .add(new Analysis(a.id(), a.title(), a.data().method(), a.origin())));
    for (final var sample : experiment.samples()) {
      cancelled();
      if (rows.size() >= 20000) {
        notes.add("Каталог ограничен 20000 образцами; выберите более узкую папку.");
        return;
      }
      final var balance = sample.balance();
      final var ref = new LaboratorySampleReference(experiment.id(), sample.id());
      final var found = analyses.getOrDefault(sample.id(), List.of());
      final String identity =
          ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.read(
                  sample.molecule())
              .filter(
                  i ->
                      new ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity
                              .CdkCompoundIdentifiers()
                          .current(sample.molecule(), i))
              .map(i -> i.searchableText())
              .orElse("");
      final String search =
          LaboratoryExperimentIndex.normalize(
              Objects.toString(path, "")
                  + " "
                  + experiment.title()
                  + " "
                  + sample.code()
                  + " "
                  + sample.molecule().displayName()
                  + " "
                  + sample.molecule().formula().hillNotation()
                  + " "
                  + identity
                  + " "
                  + sample.stageTitle()
                  + " "
                  + balance.label()
                  + " "
                  + balance.location()
                  + " "
                  + balance.container()
                  + " "
                  + ref.token());
      rows.add(
          new Row(path, experiment.title(), ref, sample, balance, found, search, unsaved, 1, 1));
    }
  }

  public static byte[] csv(final List<Row> rows) {
    if (rows.isEmpty() || rows.size() > 20000) {
      throw new IllegalArgumentException("Выберите от 1 до 20000 строк.");
    }
    final StringBuilder out =
        new StringBuilder(
            "\ufeffКод;Подпись;Вещество;Формула;Остаток;Единица;Статус;Место;Контейнер;Опыт;Файл;Анализы;Ссылка;Примечание\r\n");
    for (final Row row : rows) {
      cancelled();
      final var q = row.balance().remaining();
      final List<String> values =
          List.of(
              row.sample().code(),
              row.balance().label(),
              row.sample().molecule().displayName(),
              row.sample().molecule().formula().hillNotation(),
              q == null ? "" : q.value().toPlainString(),
              q == null ? "" : q.unit().toString(),
              row.balance().status().toString(),
              row.balance().location(),
              row.balance().container(),
              row.experimentTitle(),
              row.source(),
              String.join(
                  ", ", row.analyses().stream().map(a -> a.method() + " · " + a.origin()).toList()),
              row.reference().token(),
              row.warning());
      out.append(String.join(";", values.stream().map(LaboratorySampleCatalog::cell).toList()))
          .append("\r\n");
    }
    return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String cell(final String value) {
    final String text = value.stripLeading();
    final String safe =
        !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0 ? "'" + value : value;
    return "\"" + safe.replace("\"", "\"\"") + "\"";
  }

  public static void writeNew(final Path target, final byte[] bytes, final String extension)
      throws IOException {
    final Path absolute = target.toAbsolutePath().normalize();
    if (!absolute.toString().toLowerCase(Locale.ROOT).endsWith(extension)
        || !Files.isDirectory(absolute.getParent())) {
      throw new IOException("Выберите новый файл " + extension + " в существующей папке.");
    }
    final Path temp = Files.createTempFile(absolute.getParent(), ".sample-export-", ".tmp");
    try {
      cancelled();
      Files.write(temp, bytes);
      cancelled();
      Files.move(temp, absolute);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private static void cancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Чтение каталога образцов отменено.");
    }
  }
}
