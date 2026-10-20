/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryAnalysisSummary;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEvidenceService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Read-only ancestry, never analytical evidence assigned to a descendant specimen. */
public final class LaboratorySampleProvenance {
  public record Analysis(
      String kind,
      String id,
      String title,
      String method,
      LaboratoryAnalysis.Origin origin,
      LaboratorySampleReference specimen,
      String sampleCode,
      String experimentTitle,
      Path path,
      boolean inherited,
      boolean savedReference,
      LaboratoryAnalysis legacy,
      LaboratoryAnalyticalEvidence.Entry evidence) {
    public String report() {
      return this.legacy != null
          ? LaboratoryAnalysisSummary.text(this.legacy)
          : LaboratoryEvidenceService.description(this.evidence);
    }

    public String source() {
      return this.legacy != null ? this.legacy.source() : this.evidence.source();
    }

    public String basis() {
      return this.savedReference
          ? "Версия из сохранённой ссылки передачи"
          : this.inherited
              ? "Текущая запись родительского образца"
              : "Текущая запись этого образца";
    }
  }

  public record Missing(LaboratorySampleOrigin origin, String message) {}

  public record Result(List<Analysis> analyses, List<String> diagnostics, List<Missing> missing) {
    public Result {
      analyses = List.copyOf(analyses);
      diagnostics = List.copyOf(diagnostics);
      missing = List.copyOf(missing);
    }
  }

  private final Map<LaboratorySampleReference, Path> replacements;
  private final LinkedHashMap<String, Analysis> analyses = new LinkedHashMap<>();
  private final LinkedHashSet<String> diagnostics = new LinkedHashSet<>();
  private final LinkedHashMap<LaboratorySampleReference, Missing> missing = new LinkedHashMap<>();
  private final Set<LaboratorySampleReference> visited = new HashSet<>();
  private final Map<Path, LaboratoryDrawing> files = new HashMap<>();
  private long bytes;

  private LaboratorySampleProvenance(final Map<LaboratorySampleReference, Path> replacements) {
    this.replacements = Map.copyOf(replacements);
  }

  public static Result resolve(
      final LaboratoryDrawing current,
      final String sampleId,
      final Path currentPath,
      final Map<LaboratorySampleReference, Path> replacements) {
    final var read = new LaboratorySampleProvenance(replacements);
    read.visit(current, sampleId, currentPath, false, null, new LinkedHashSet<>(), 0);
    return new Result(
        new ArrayList<>(read.analyses.values()),
        new ArrayList<>(read.diagnostics),
        new ArrayList<>(read.missing.values()));
  }

  private void visit(
      final LaboratoryDrawing document,
      final String sampleId,
      final Path path,
      final boolean inherited,
      final List<String> exactIds,
      final Set<LaboratorySampleReference> ancestors,
      final int depth) {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Чтение происхождения отменено.");
    }
    final var experiment = document.experiment();
    final var reference = new LaboratorySampleReference(experiment.id(), sampleId);
    if (ancestors.contains(reference)) {
      this.diagnostics.add(
          "Цикл происхождения у ссылки " + reference.token() + "; повторный переход остановлен.");
      return;
    }
    if (depth > 16 || this.visited.size() >= 256) {
      this.diagnostics.add(
          "Цепочка происхождения ограничена 16 переходами и 256 образцами; обзор неполон.");
      return;
    }
    final var sample =
        experiment.samples().stream().filter(s -> s.id().equals(sampleId)).findFirst().orElse(null);
    if (sample == null) {
      this.diagnostics.add(
          "В опыте «" + experiment.title() + "» отсутствует исходный образец " + sampleId + ".");
      return;
    }
    if (exactIds == null) {
      final Set<String> corrected = new HashSet<>();
      experiment
          .analyses()
          .forEach(
              a -> {
                if (a.correctsId() != null) corrected.add(a.correctsId());
              });
      experiment.analyses().stream()
          .filter(a -> sampleId.equals(a.sampleId()) && !corrected.contains(a.id()))
          .forEach(a -> this.add(experiment, sample, path, inherited, false, a, null));
      experiment.analyticalEvidence().latest().stream()
          .filter(a -> sampleId.equals(a.sampleId()))
          .forEach(a -> this.add(experiment, sample, path, inherited, false, null, a));
    } else {
      final Map<String, LaboratoryAnalysis> legacyById = new HashMap<>();
      experiment.analyses().forEach(a -> legacyById.put(a.id(), a));
      final Map<String, LaboratoryAnalyticalEvidence.Entry> evidenceById = new HashMap<>();
      experiment.analyticalEvidence().entries().forEach(a -> evidenceById.put(a.id(), a));
      for (final String id : exactIds) {
        final var legacy = legacyById.get(id);
        final var evidence = evidenceById.get(id);
        if (legacy == null && evidence == null) {
          this.diagnostics.add(
              "Связанная версия анализа отсутствует: " + id + " · " + experiment.title());
          continue;
        }
        if (legacy != null && evidence != null) {
          this.diagnostics.add(
              "Неоднозначный ID анализа в двух типах записей: " + id + ". Версия не подменена.");
          continue;
        }
        if (!sampleId.equals(legacy != null ? legacy.sampleId() : evidence.sampleId())) {
          this.diagnostics.add(
              "Связанная версия " + id + " принадлежит другому образцу; она не принята.");
          continue;
        }
        this.add(experiment, sample, path, inherited, true, legacy, evidence);
      }
    }
    if (!this.visited.add(reference)) {
      return;
    }
    final var branch = new LinkedHashSet<>(ancestors);
    branch.add(reference);
    for (final String parent : sample.parents())
      this.visit(document, parent, path, true, null, branch, depth + 1);
    if (sample.origin() != null) {
      final var origin = sample.origin();
      try {
        final Path source = this.source(origin, path);
        final var previous = this.read(source);
        if (!previous.experiment().id().equals(origin.source().experimentId())) {
          throw new IllegalArgumentException(
              "Файл содержит другой опыт; совпадение названия недостаточно.");
        }
        final var specimen =
            previous.experiment().samples().stream()
                .filter(s -> s.id().equals(origin.source().sampleId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "В файле отсутствует указанный исходный образец."));
        if (!MolecularIdentity.signature(specimen.molecule())
            .equals(MolecularIdentity.signature(sample.molecule()))) {
          throw new IllegalArgumentException(
              "Сохранённая структура исходного образца изменилась; аналитика не подставлена.");
        }
        this.visit(previous, specimen.id(), source, true, origin.analysisIds(), branch, depth + 1);
      } catch (final Exception error) {
        if (error instanceof java.util.concurrent.CancellationException cancelled) throw cancelled;
        final String message =
            "Источник «"
                + origin.sourceTitle()
                + "», образец «"
                + origin.sourceCode()
                + "»: "
                + Objects.toString(error.getMessage(), error.getClass().getSimpleName());
        this.diagnostics.add(message);
        this.missing.putIfAbsent(origin.source(), new Missing(origin, message));
      }
    }
  }

  private void add(
      final LaboratoryExperiment experiment,
      final LaboratorySample sample,
      final Path path,
      final boolean inherited,
      final boolean savedReference,
      final LaboratoryAnalysis legacy,
      final LaboratoryAnalyticalEvidence.Entry evidence) {
    if (this.analyses.size() >= 512) {
      this.diagnostics.add("Показано не более 512 аналитических записей; обзор неполон.");
      return;
    }
    final String kind = legacy != null ? "LEGACY" : "EVIDENCE",
        id = legacy != null ? legacy.id() : evidence.id();
    final String key = experiment.id() + "/" + kind + "/" + id;
    final var value =
        new Analysis(
            kind,
            id,
            legacy != null ? legacy.title() : evidence.title(),
            legacy != null ? legacy.data().method() : evidence.data().method(),
            legacy != null ? legacy.origin() : evidence.origin(),
            new LaboratorySampleReference(experiment.id(), sample.id()),
            sample.code(),
            experiment.title(),
            path,
            inherited,
            savedReference,
            legacy,
            evidence);
    final var previous = this.analyses.get(key);
    if (previous == null) this.analyses.put(key, value);
    else if (!Objects.equals(previous.legacy(), legacy)
        || !Objects.equals(previous.evidence(), evidence))
      this.diagnostics.add(
          "Разные файлы содержат разные версии одного ID анализа: "
              + id
              + ". Первая версия не заменена.");
  }

  private Path source(final LaboratorySampleOrigin origin, final Path referring) {
    Path path = this.replacements.get(origin.source());
    if (path == null) {
      if (origin.sourcePathHint().isBlank())
        throw new IllegalArgumentException("Путь не указан. Найдите исходный QCHEM.");
      path = Path.of(origin.sourcePathHint());
      if (!path.isAbsolute()) {
        if (referring == null || referring.getParent() == null)
          throw new IllegalArgumentException(
              "Относительный путь нельзя проверить без исходной папки. Найдите QCHEM.");
        path = referring.getParent().resolve(path);
      }
    }
    return path.toAbsolutePath().normalize();
  }

  private LaboratoryDrawing read(final Path path) throws Exception {
    if (this.files.containsKey(path)) return this.files.get(path);
    if (this.files.size() >= 32)
      throw new IllegalArgumentException("Достигнут предел 32 исходных файлов; обзор неполон.");
    if (!Files.isRegularFile(path))
      throw new IllegalArgumentException("Исходный файл недоступен. Найдите QCHEM.");
    final long size = Files.size(path);
    if (size > 128L * 1024 * 1024 || this.bytes + size > 256L * 1024 * 1024)
      throw new IllegalArgumentException(
          "Превышен предел чтения происхождения (128 МиБ на файл, 256 МиБ суммарно).");
    this.bytes += size;
    final var result = new LaboratoryFileRepository().read(path).drawing();
    this.files.put(path, result);
    return result;
  }
}
