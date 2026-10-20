/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Read-only views of saved observations. No resampling, chemical matching or inferred difference spectrum. */
public final class LaboratorySpectralComparison {
  private LaboratorySpectralComparison() {}

  public enum Kind {
    NMR_TRACE,
    NMR_LIST,
    CORRELATIONS,
    CHROM_TRACE,
    CHROM_LIST
  }

  public record Candidate(
      String key,
      String title,
      Instant recordedAt,
      String method,
      String origin,
      String source,
      String sample,
      Molecule molecule,
      LaboratoryAnalysis analysis,
      Entry evidence,
      LaboratoryAnalysis linkedAnalysis) {
    public Candidate(
        final String key,
        final String title,
        final Instant recordedAt,
        final String method,
        final String origin,
        final String source,
        final String sample,
        final Molecule molecule,
        final LaboratoryAnalysis analysis,
        final Entry evidence) {
      this(
          key,
          title,
          recordedAt,
          method,
          origin,
          source,
          sample,
          molecule,
          analysis,
          evidence,
          null);
    }

    public Candidate {
      if ((analysis == null) == (evidence == null)) {
        throw new IllegalArgumentException("Выберите одну сохранённую запись.");
      }
    }

    public String correctsId() {
      return this.analysis == null ? this.evidence.correctsId() : this.analysis.correctsId();
    }

    @Override
    public String toString() {
      return this.title + " · " + recordedTime(this) + " · " + this.method + " · " + this.sample;
    }
  }

  /** Local display only; exact keys and instants remain unchanged. */
  public static String recordedTime(final Candidate record) {
    return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("ru"))
        .format(record.recordedAt().atZone(ZoneId.systemDefault()));
  }

  public record Point(double x, double y) {}

  public record Mark(double from, double to, double y, String label) {}

  public record Line(double ppm, double y, String multipletId, String label) {}

  public record Series(
      Candidate record,
      Kind kind,
      String compatibility,
      String xLabel,
      String yLabel,
      boolean reverseX,
      boolean reverseY,
      List<Point> points,
      List<Mark> marks,
      String details,
      List<Line> lines,
      Correlations correlations) {
    public Series(
        final Candidate record,
        final Kind kind,
        final String compatibility,
        final String xLabel,
        final String yLabel,
        final boolean reverseX,
        final boolean reverseY,
        final List<Point> points,
        final List<Mark> marks,
        final String details) {
      this(
          record,
          kind,
          compatibility,
          xLabel,
          yLabel,
          reverseX,
          reverseY,
          points,
          marks,
          details,
          List.of(),
          null);
    }

    public Series {
      points = List.copyOf(points);
      marks = List.copyOf(marks);
      lines = List.copyOf(lines);
    }

    public boolean line() {
      return this.kind == Kind.NMR_TRACE || this.kind == Kind.CHROM_TRACE;
    }

    public boolean positionsOnly() {
      return this.kind == Kind.NMR_LIST;
    }
  }

  public static List<Candidate> records(
      final LaboratoryExperiment experiment, final boolean history) {
    final List<Candidate> result = new ArrayList<>();
    final Set<String> corrected = new HashSet<>();
    experiment
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                corrected.add(a.correctsId());
              }
            });
    for (final var a : experiment.analyses()) {
      if (a.data() instanceof LaboratoryAnalysis.Nmr && (history || !corrected.contains(a.id()))) {
        result.add(
            new Candidate(
                "analysis:" + a.id(),
                a.title(),
                a.recordedAt(),
                a.data().method(),
                a.origin().toString(),
                a.source(),
                sample(experiment, a.sampleId()),
                a.molecule(),
                a,
                null));
      }
    }
    for (final var a :
        history
            ? experiment.analyticalEvidence().entries()
            : experiment.analyticalEvidence().latest()) {
      if (a.data() instanceof Correlations
          || a.data() instanceof Multiplets
          || a.data() instanceof Chromatogram) {
        result.add(
            new Candidate(
                "evidence:" + a.id(),
                a.title(),
                a.recordedAt(),
                a.data().method(),
                a.origin().toString(),
                a.source(),
                sample(experiment, a.sampleId()),
                a.molecule(),
                null,
                a,
                LaboratoryEvidenceService.exactSource(experiment, a).orElse(null)));
      }
    }
    return result.stream().sorted(Comparator.comparing(Candidate::recordedAt).reversed()).toList();
  }

  private static String sample(final LaboratoryExperiment experiment, final String id) {
    return id == null
        ? "Без отдельного образца"
        : experiment.samples().stream()
            .filter(s -> s.id().equals(id))
            .map(LaboratorySample::code)
            .findFirst()
            .orElse("Сохранённый образец");
  }

  private static String normalized(final String text) {
    return text.strip().toLowerCase(Locale.ROOT);
  }

  private static String normalizedTechnique(final String text) {
    return switch (normalized(text)) {
      case "hplc", "вэжх" -> "hplc";
      default -> normalized(text);
    };
  }

  public static String compatibility(final Candidate record) {
    if (record.analysis() != null) {
      final var n = (LaboratoryAnalysis.Nmr) record.analysis().data();
      return (n.trace() == null ? "nmr-list:" : "nmr-trace:") + n.nucleus().name();
    }
    return switch (record.evidence().data()) {
      case Multiplets m ->
          (record.linkedAnalysis() == null ? "nmr-list:" : "nmr-trace:") + m.nucleus().name();
      case Correlations c ->
          "2d:" + c.experiment() + ":" + c.xNucleus().name() + ":" + c.yNucleus().name();
      case Chromatogram c ->
          (c.trace().isEmpty() ? "chrom-list:" : "chrom-trace:")
              + normalizedTechnique(c.technique())
              + ":"
              + normalized(c.detector());
      default ->
          throw new IllegalArgumentException("Для этой записи нет спектрального представления.");
    };
  }

  public static boolean compatible(final Candidate first, final Candidate second) {
    return first != null
        && second != null
        && !first.key().equals(second.key())
        && compatibility(first).equals(compatibility(second));
  }

  public static Series series(final Candidate candidate) {
    final List<Point> points = new ArrayList<>();
    final List<Mark> marks = new ArrayList<>();
    final List<Line> lines = new ArrayList<>();
    Correlations correlations = null;
    Kind kind;
    String xLabel, yLabel, details;
    boolean reverseX = false, reverseY = false;
    if (candidate.analysis() != null) {
      final var record = candidate.analysis();
      final var n = (LaboratoryAnalysis.Nmr) record.data();
      reverseX = true;
      xLabel = n.nucleus() + " · ppm";
      if (n.trace() != null) {
        kind = Kind.NMR_TRACE;
        yLabel = "Интенсивность · единицы сохранённого спектра";
        final var spectrum = NmrProcessing.process(n.trace());
        for (int i = 0; i < spectrum.real().size(); i++) {
          points.add(
              new Point(spectrum.firstPpm() + i * spectrum.stepPpm(), spectrum.real().get(i)));
        }
      } else {
        kind = Kind.NMR_LIST;
        yLabel = "Положения сигналов · без моделирования формы";
        for (final var s : n.signals()) {
          marks.add(
              new Mark(
                  s.from().doubleValue(),
                  s.to().doubleValue(),
                  0,
                  "δ "
                      + s.from()
                      + (s.from().compareTo(s.to()) == 0 ? "" : "–" + s.to())
                      + " · "
                      + s.multiplicity()
                      + " · интеграл "
                      + LaboratoryEvidenceService.format(s.integral())));
        }
      }
      details = LaboratoryAnalysisSummary.text(record);
    } else {
      final var record = candidate.evidence();
      details = LaboratoryEvidenceService.description(record);
      switch (record.data()) {
        case Multiplets m -> {
          final var source = candidate.linkedAnalysis();
          final NmrProcessing.Spectrum spectrum;
          if (m.analysisId() != null && (source == null || !source.id().equals(m.analysisId()))) {
            throw new IllegalArgumentException(
                "Для сравнения связанного мультиплета нужна точная исходная запись 1D ЯМР.");
          }
          if (source != null) {
            spectrum = NmrProcessing.process(((LaboratoryAnalysis.Nmr) source.data()).trace());
            kind = Kind.NMR_TRACE;
            for (int i = 0; i < spectrum.real().size(); i++) {
              points.add(new Point(spectrum.ppm(i), spectrum.real().get(i)));
            }
            details += "\nТрасса связанной записи:\n" + LaboratoryAnalysisSummary.text(source);
          } else {
            spectrum = null;
            kind = Kind.NMR_LIST;
          }
          reverseX = true;
          xLabel = m.nucleus() + " · ppm";
          yLabel =
              spectrum == null
                  ? "Положения линий · без моделирования формы"
                  : "Сохранённая трасса и отмеченные линии";
          int outside = 0;
          for (final var p : m.multiplets()) {
            final double center = p.centerPpm().doubleValue();
            final Double height = spectrum == null ? Double.valueOf(0) : sample(spectrum, center);
            if (height != null) {
              marks.add(
                  new Mark(
                      center,
                      center,
                      height,
                      "Центр δ "
                          + p.centerPpm()
                          + " · "
                          + p.label()
                          + " · интеграл "
                          + LaboratoryEvidenceService.format(p.integral())));
            }
            for (final var line : p.linesPpm()) {
              final Double y =
                  spectrum == null ? Double.valueOf(0) : sample(spectrum, line.doubleValue());
              if (y == null) {
                outside++;
              } else {
                lines.add(
                    new Line(line.doubleValue(), y, p.id(), "Линия δ " + line + " · " + p.label()));
              }
            }
          }
          if (outside > 0) {
            details +=
                "\nЛиний вне диапазона исходной трассы: "
                    + outside
                    + ". Они сохранены в данных, но не нанесены на трассу.\n";
          }
        }
        case Correlations c -> {
          correlations = c;
          kind = Kind.CORRELATIONS;
          reverseX = true;
          reverseY = true;
          xLabel = c.xNucleus() + " · ppm";
          yLabel = c.yNucleus() + " · ppm";
          for (final var p : c.correlations()) {
            marks.add(
                new Mark(
                    p.xPpm().doubleValue(),
                    p.xPpm().doubleValue(),
                    p.yPpm().doubleValue(),
                    p.xPpm() + " / " + p.yPpm() + " ppm"));
          }
        }
        case Chromatogram c -> {
          kind = c.trace().isEmpty() ? Kind.CHROM_LIST : Kind.CHROM_TRACE;
          xLabel = "Время · мин";
          yLabel =
              c.trace().isEmpty() ? "Площадь · единицы записи" : "Отклик · единицы исходного файла";
          for (final var p : c.trace()) {
            points.add(new Point(p.timeMin().doubleValue(), p.response().doubleValue()));
          }
          if (c.trace().isEmpty()) {
            for (final var p : c.peaks()) {
              marks.add(
                  new Mark(
                      p.retentionMin().doubleValue(),
                      p.retentionMin().doubleValue(),
                      p.area().doubleValue(),
                      p.label() + " · " + p.retentionMin() + " мин · площадь " + p.area()));
            }
          }
        }
        default -> throw new IllegalArgumentException("Запись не содержит спектральных данных.");
      }
    }
    if (points.isEmpty()
        && marks.isEmpty()
        && lines.isEmpty()
        && (correlations == null || correlations.spectrum() == null)) {
      throw new IllegalArgumentException("В выбранной записи нет сохранённых точек или сигналов.");
    }
    return new Series(
        candidate,
        kind,
        compatibility(candidate),
        xLabel,
        yLabel,
        reverseX,
        reverseY,
        decimate(points),
        marks,
        details,
        lines,
        correlations);
  }

  /** The nearest stored sample value; not a peak finder and not an inferred line amplitude. */
  public static Double sample(final NmrProcessing.Spectrum spectrum, final double ppm) {
    final double position = (ppm - spectrum.firstPpm()) / spectrum.stepPpm();
    if (!Double.isFinite(position) || position < 0 || position > spectrum.real().size() - 1) {
      return null;
    }
    return spectrum.real().get((int) Math.round(position));
  }

  /** Retain extrema in each display bucket. The saved full-resolution data remains untouched. */
  private static List<Point> decimate(final List<Point> source) {
    if (source.size() <= 6000) {
      return source;
    }
    final List<Point> result = new ArrayList<>();
    final int stride = (source.size() + 2999) / 3000;
    result.add(source.getFirst());
    for (int start = 0; start < source.size(); start += stride) {
      int low = start, high = start;
      for (int i = start + 1; i < Math.min(source.size(), start + stride); i++) {
        if (source.get(i).y() < source.get(low).y()) {
          low = i;
        }
        if (source.get(i).y() > source.get(high).y()) {
          high = i;
        }
      }
      result.add(source.get(Math.min(low, high)));
      if (low != high) {
        result.add(source.get(Math.max(low, high)));
      }
    }
    result.add(source.getLast());
    return result;
  }

  public static String provenance(final Candidate record) {
    return record.title()
        + "\n"
        + record.method()
        + " · "
        + record.origin()
        + " · "
        + record.recordedAt()
        + "\n"
        + record.molecule().displayName()
        + " · "
        + record.sample()
        + "\nИсточник: "
        + (record.source().isBlank() ? "не указан" : record.source());
  }
}
