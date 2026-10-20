/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;

/** QCHEM 11 typed advanced analytical records; unknown fields or methods are rejected. */
final class QchemLaboratoryEvidenceCodec {

  private QchemLaboratoryEvidenceCodec() {}

  static void write(final StringBuilder out, final LaboratoryAnalyticalEvidence aggregate) {
    final Map<NmrSpectrum2D, String> spectra = new HashMap<>();
    for (final var e : aggregate.entries()) {
      line(
          out,
          "evidence-v1",
          "id",
          e.id(),
          "component",
          e.componentId(),
          "sample",
          empty(e.sampleId()),
          "title",
          e.title(),
          "time",
          e.recordedAt().toString(),
          "origin",
          e.origin().name(),
          "source",
          e.source(),
          "note",
          e.note(),
          "molecule",
          e.molecule().id().value(),
          "attachments",
          String.join(",", e.attachmentIds()),
          "corrects",
          empty(e.correctsId()));
      switch (e.data()) {
        case Correlations c -> {
          line(
              out,
              "evidence-2d-v1",
              "entry",
              e.id(),
              "experiment",
              c.experiment().name(),
              "xNucleus",
              c.xNucleus().name(),
              "yNucleus",
              c.yNucleus().name(),
              "solvent",
              c.solvent());
          if (c.spectrum() != null) {
            final String previous = spectra.putIfAbsent(c.spectrum(), e.id());
            if (previous == null) {
              line(
                  out,
                  "evidence-2d-spectrum-v1",
                  "entry",
                  e.id(),
                  "data",
                  QchemNmrSpectrum2DCodec.encode(c.spectrum()));
            } else {
              line(out, "evidence-2d-spectrum-ref-v1", "entry", e.id(), "spectrum", previous);
            }
          }
          c.correlations()
              .forEach(
                  p ->
                      line(
                          out,
                          "evidence-correlation-v1",
                          "entry",
                          e.id(),
                          "id",
                          p.id(),
                          "x",
                          num(p.xPpm()),
                          "y",
                          num(p.yPpm()),
                          "intensity",
                          num(p.intensity()),
                          "xAtoms",
                          atoms(p.xAtoms()),
                          "yAtoms",
                          atoms(p.yAtoms()),
                          "note",
                          p.note()));
        }
        case Multiplets m -> {
          line(
              out,
              "evidence-multiplets-v1",
              "entry",
              e.id(),
              "nucleus",
              m.nucleus().name(),
              "frequency",
              num(m.frequencyMHz()));
          if (m.analysisId() != null) {
            line(out, "evidence-multiplets-source-v1", "entry", e.id(), "analysis", m.analysisId());
          }
          m.multiplets()
              .forEach(
                  p ->
                      line(
                          out,
                          "evidence-multiplet-v1",
                          "entry",
                          e.id(),
                          "id",
                          p.id(),
                          "center",
                          num(p.centerPpm()),
                          "integral",
                          num(p.integral()),
                          "lines",
                          nums(p.linesPpm()),
                          "couplings",
                          nums(p.couplingsHz()),
                          "atoms",
                          atoms(p.atoms()),
                          "label",
                          p.label(),
                          "note",
                          p.note()));
        }
        case QuantitativeNmr q ->
            line(
                out,
                "evidence-qnmr-v1",
                "entry",
                e.id(),
                "standard",
                q.standardName(),
                "sampleIntegral",
                num(q.sampleIntegral()),
                "sampleNuclei",
                Integer.toString(q.sampleNuclei()),
                "sampleMolarMass",
                num(q.sampleMolarMass()),
                "sampleMass",
                num(q.sampleMass().value()),
                "sampleUnit",
                q.sampleMass().unit().name(),
                "standardIntegral",
                num(q.standardIntegral()),
                "standardNuclei",
                Integer.toString(q.standardNuclei()),
                "standardMolarMass",
                num(q.standardMolarMass()),
                "standardMass",
                num(q.standardMass().value()),
                "standardUnit",
                q.standardMass().unit().name(),
                "purity",
                num(q.standardPurityPercent()),
                "preparation",
                q.preparation());
        case Chromatogram c -> {
          line(
              out,
              "evidence-chrom-v1",
              "entry",
              e.id(),
              "technique",
              c.technique(),
              "detector",
              c.detector(),
              "calibration",
              c.calibration() == null ? "" : c.calibration().source(),
              "peak",
              empty(c.quantifiedPeakId()),
              "sampleMass",
              c.representedSampleMass() == null ? "" : num(c.representedSampleMass().value()),
              "sampleUnit",
              c.representedSampleMass() == null ? "" : c.representedSampleMass().unit().name(),
              "purity",
              num(c.reportedPurityPercent()),
              "puritySource",
              c.reportedPuritySource(),
              "preparation",
              c.preparation());
          c.trace()
              .forEach(
                  p ->
                      line(
                          out,
                          "evidence-chrom-point-v1",
                          "entry",
                          e.id(),
                          "time",
                          num(p.timeMin()),
                          "response",
                          num(p.response())));
          c.peaks()
              .forEach(
                  p ->
                      line(
                          out,
                          "evidence-chrom-peak-v1",
                          "entry",
                          e.id(),
                          "id",
                          p.id(),
                          "label",
                          p.label(),
                          "time",
                          num(p.retentionMin()),
                          "from",
                          num(p.fromMin()),
                          "to",
                          num(p.toMin()),
                          "area",
                          num(p.area()),
                          "included",
                          Boolean.toString(p.included()),
                          "reason",
                          p.exclusionReason()));
          if (c.calibration() != null) {
            c.calibration()
                .points()
                .forEach(
                    p ->
                        line(
                            out,
                            "evidence-calibration-v1",
                            "entry",
                            e.id(),
                            "mass",
                            num(p.mass().value()),
                            "unit",
                            p.mass().unit().name(),
                            "area",
                            num(p.area())));
          }
        }
        case Dossier d -> {
          line(
              out,
              "evidence-dossier-v1",
              "entry",
              e.id(),
              "analyses",
              String.join(",", d.analysisIds()),
              "evidence",
              String.join(",", d.evidenceIds()),
              "tolerance",
              num(d.massTolerancePpm()),
              "conclusion",
              d.conclusion().name(),
              "text",
              d.conclusionText());
          d.findings()
              .forEach(
                  f ->
                      line(
                          out,
                          "evidence-finding-v1",
                          "entry",
                          e.id(),
                          "category",
                          f.category(),
                          "severity",
                          f.severity().name(),
                          "text",
                          f.text(),
                          "resolved",
                          Boolean.toString(f.resolved())));
        }
      }
    }
  }

  static LaboratoryAnalyticalEvidence read(
      final List<QchemLine> body, final Map<String, Molecule> molecules) {
    final Map<String, QchemLine> headers = new LinkedHashMap<>(), methods = new LinkedHashMap<>();
    final Map<String, List<QchemLine>> details = new LinkedHashMap<>();
    for (final var line : body) {
      if (line.command().startsWith("evidence-")) {
        switch (line.command()) {
          case "evidence-v1" -> {
            keys(
                line,
                "id",
                "component",
                "sample",
                "title",
                "time",
                "origin",
                "source",
                "note",
                "molecule",
                "attachments",
                "corrects");
            if (headers.put(value(line, "id"), line) != null) {
              throw new IllegalArgumentException("Повтор заголовка аналитического подтверждения.");
            }
          }
          case "evidence-2d-v1",
              "evidence-multiplets-v1",
              "evidence-qnmr-v1",
              "evidence-chrom-v1",
              "evidence-dossier-v1" -> {
            if (methods.put(value(line, "entry"), line) != null) {
              throw new IllegalArgumentException("Несколько методов одной аналитической записи.");
            }
          }
          case "evidence-2d-spectrum-v1",
              "evidence-2d-spectrum-ref-v1",
              "evidence-multiplets-source-v1",
              "evidence-correlation-v1",
              "evidence-multiplet-v1",
              "evidence-chrom-point-v1",
              "evidence-chrom-peak-v1",
              "evidence-calibration-v1",
              "evidence-finding-v1" ->
              details.computeIfAbsent(value(line, "entry"), k -> new ArrayList<>()).add(line);
          default ->
              throw new IllegalArgumentException(
                  "Неизвестная версия аналитической записи: " + line.command());
        }
      }
    }
    if (!headers.keySet().equals(methods.keySet())
        || !headers.keySet().containsAll(details.keySet())) {
      throw new IllegalArgumentException(
          "Аналитические строки не соответствуют заголовкам и методам.");
    }
    if (headers.size() > 2000) {
      throw new IllegalArgumentException("Слишком много аналитических записей.");
    }
    final Map<String, NmrSpectrum2D> spectra = new HashMap<>();
    int cells = 0;
    for (final String id : headers.keySet()) {
      boolean found = false;
      for (final var row : details.getOrDefault(id, List.of())) {
        if (!Set.of("evidence-2d-spectrum-v1", "evidence-2d-spectrum-ref-v1")
            .contains(row.command())) {
          continue;
        }
        if (found || !methods.get(id).command().equals("evidence-2d-v1")) {
          throw new IllegalArgumentException(
              "Матрица 2D спектра повторена или относится к другому методу.");
        }
        found = true;
        final NmrSpectrum2D spectrum;
        if (row.command().equals("evidence-2d-spectrum-v1")) {
          keys(row, "entry", "data");
          spectrum =
              QchemNmrSpectrum2DCodec.decode(value(row, "data"), NmrSpectrum2D.MAX_CELLS - cells);
          cells += spectrum.values().size();
        } else {
          keys(row, "entry", "spectrum");
          spectrum = spectra.get(value(row, "spectrum"));
          if (spectrum == null) {
            throw new IllegalArgumentException(
                "Ссылка на 2D спектр должна указывать ранее сохранённую матрицу.");
          }
        }
        spectra.put(id, spectrum);
      }
    }
    final List<Entry> entries = new ArrayList<>();
    for (final var h : headers.values()) {
      final String id = value(h, "id");
      final Molecule molecule = molecules.get(value(h, "molecule"));
      if (molecule == null) {
        throw new IllegalArgumentException("Отсутствует снимок структуры аналитической записи.");
      }
      entries.add(
          new Entry(
              id,
              value(h, "component"),
              nullable(value(h, "sample")),
              value(h, "title"),
              Instant.parse(value(h, "time")),
              LaboratoryAnalysis.Origin.valueOf(value(h, "origin")),
              value(h, "source"),
              value(h, "note"),
              molecule,
              list(value(h, "attachments")),
              nullable(value(h, "corrects")),
              data(methods.get(id), details.getOrDefault(id, List.of()), spectra.get(id))));
    }
    return new LaboratoryAnalyticalEvidence(entries);
  }

  private static Data data(
      final QchemLine header, final List<QchemLine> rows, final NmrSpectrum2D spectrum) {
    return switch (header.command()) {
      case "evidence-2d-v1" -> {
        keys(header, "entry", "experiment", "xNucleus", "yNucleus", "solvent");
        final List<Correlation> values = new ArrayList<>();
        for (final var p : rows) {
          if (p.command().equals("evidence-2d-spectrum-v1")
              || p.command().equals("evidence-2d-spectrum-ref-v1")) {
            continue;
          }
          command(p, "evidence-correlation-v1");
          keys(p, "entry", "id", "x", "y", "intensity", "xAtoms", "yAtoms", "note");
          values.add(
              new Correlation(
                  value(p, "id"),
                  number(p, "x"),
                  number(p, "y"),
                  number(p, "intensity"),
                  atoms(value(p, "xAtoms")),
                  atoms(value(p, "yAtoms")),
                  value(p, "note")));
        }
        yield new Correlations(
            Experiment2D.valueOf(value(header, "experiment")),
            LaboratoryAnalysis.Nucleus.valueOf(value(header, "xNucleus")),
            LaboratoryAnalysis.Nucleus.valueOf(value(header, "yNucleus")),
            value(header, "solvent"),
            values,
            spectrum);
      }
      case "evidence-multiplets-v1" -> {
        keys(header, "entry", "nucleus", "frequency");
        final List<Multiplet> values = new ArrayList<>();
        String analysisId = null;
        for (final var p : rows) {
          if (p.command().equals("evidence-multiplets-source-v1")) {
            keys(p, "entry", "analysis");
            if (analysisId != null) {
              throw new IllegalArgumentException("Повтор исходного анализа мультиплетов.");
            }
            analysisId = value(p, "analysis");
            if (analysisId.isEmpty()) {
              throw new IllegalArgumentException("Пустая ссылка на исходный ЯМР-анализ.");
            }
            continue;
          }
          command(p, "evidence-multiplet-v1");
          keys(
              p,
              "entry",
              "id",
              "center",
              "integral",
              "lines",
              "couplings",
              "atoms",
              "label",
              "note");
          values.add(
              new Multiplet(
                  value(p, "id"),
                  number(p, "center"),
                  number(p, "integral"),
                  nums(value(p, "lines")),
                  nums(value(p, "couplings")),
                  atoms(value(p, "atoms")),
                  value(p, "label"),
                  value(p, "note")));
        }
        yield new Multiplets(
            LaboratoryAnalysis.Nucleus.valueOf(value(header, "nucleus")),
            number(header, "frequency"),
            values,
            analysisId);
      }
      case "evidence-qnmr-v1" -> {
        keys(
            header,
            "entry",
            "standard",
            "sampleIntegral",
            "sampleNuclei",
            "sampleMolarMass",
            "sampleMass",
            "sampleUnit",
            "standardIntegral",
            "standardNuclei",
            "standardMolarMass",
            "standardMass",
            "standardUnit",
            "purity",
            "preparation");
        if (!rows.isEmpty()) {
          throw new IllegalArgumentException("Лишние строки расчёта qNMR.");
        }
        yield new QuantitativeNmr(
            value(header, "standard"),
            number(header, "sampleIntegral"),
            Integer.parseInt(value(header, "sampleNuclei")),
            number(header, "sampleMolarMass"),
            quantity(header, "sampleMass", "sampleUnit"),
            number(header, "standardIntegral"),
            Integer.parseInt(value(header, "standardNuclei")),
            number(header, "standardMolarMass"),
            quantity(header, "standardMass", "standardUnit"),
            number(header, "purity"),
            value(header, "preparation"));
      }
      case "evidence-chrom-v1" -> {
        keys(
            header,
            "entry",
            "technique",
            "detector",
            "calibration",
            "peak",
            "sampleMass",
            "sampleUnit",
            "purity",
            "puritySource",
            "preparation");
        final List<ChromPoint> trace = new ArrayList<>();
        final List<ChromPeak> peaks = new ArrayList<>();
        final List<CalibrationPoint> calibration = new ArrayList<>();
        for (final var p : rows) {
          switch (p.command()) {
            case "evidence-chrom-point-v1" -> {
              keys(p, "entry", "time", "response");
              trace.add(new ChromPoint(number(p, "time"), number(p, "response")));
            }
            case "evidence-chrom-peak-v1" -> {
              keys(p, "entry", "id", "label", "time", "from", "to", "area", "included", "reason");
              peaks.add(
                  new ChromPeak(
                      value(p, "id"),
                      value(p, "label"),
                      number(p, "time"),
                      number(p, "from"),
                      number(p, "to"),
                      number(p, "area"),
                      bool(p, "included"),
                      value(p, "reason")));
            }
            case "evidence-calibration-v1" -> {
              keys(p, "entry", "mass", "unit", "area");
              calibration.add(new CalibrationPoint(quantity(p, "mass", "unit"), number(p, "area")));
            }
            default -> throw new IllegalArgumentException("Строка не относится к хроматографии.");
          }
        }
        final String source = value(header, "calibration");
        if (source.isBlank() != calibration.isEmpty()) {
          throw new IllegalArgumentException("Калибровка требует точек и источника одновременно.");
        }
        yield new Chromatogram(
            value(header, "technique"),
            value(header, "detector"),
            trace,
            peaks,
            calibration.isEmpty() ? null : new MassCalibration(calibration, source),
            nullable(value(header, "peak")),
            quantity(header, "sampleMass", "sampleUnit"),
            number(header, "purity"),
            value(header, "puritySource"),
            value(header, "preparation"));
      }
      case "evidence-dossier-v1" -> {
        keys(header, "entry", "analyses", "evidence", "tolerance", "conclusion", "text");
        final List<Finding> findings = new ArrayList<>();
        for (final var p : rows) {
          command(p, "evidence-finding-v1");
          keys(p, "entry", "category", "severity", "text", "resolved");
          findings.add(
              new Finding(
                  value(p, "category"),
                  Severity.valueOf(value(p, "severity")),
                  value(p, "text"),
                  bool(p, "resolved")));
        }
        yield new Dossier(
            list(value(header, "analyses")),
            list(value(header, "evidence")),
            number(header, "tolerance"),
            findings,
            Conclusion.valueOf(value(header, "conclusion")),
            value(header, "text"));
      }
      default -> throw new IllegalArgumentException("Неизвестный аналитический метод.");
    };
  }

  private static void command(final QchemLine line, final String expected) {
    if (!line.command().equals(expected)) {
      throw new IllegalArgumentException("Строка не соответствует методу анализа.");
    }
  }

  private static void keys(final QchemLine line, final String... names) {
    if (!line.values().keySet().equals(Set.of(names))) {
      throw new IllegalArgumentException(
          "Неизвестное или отсутствующее поле аналитической записи " + line.command() + ".");
    }
  }

  private static String value(final QchemLine line, final String name) {
    return QchemEscaper.unquote(line.required(name));
  }

  private static BigDecimal number(final QchemLine line, final String name) {
    final String value = value(line, name);
    return value.isEmpty() ? null : new BigDecimal(value);
  }

  private static LaboratoryQuantity quantity(
      final QchemLine line, final String amount, final String unit) {
    final var number = number(line, amount);
    final var value = value(line, unit);
    if ((number == null) != value.isEmpty()) {
      throw new IllegalArgumentException("Количество и единица задаются вместе.");
    }
    return number == null
        ? null
        : new LaboratoryQuantity(number, LaboratoryQuantityUnit.valueOf(value));
  }

  private static boolean bool(final QchemLine line, final String name) {
    final String value = value(line, name);
    if (!value.equals("true") && !value.equals("false")) {
      throw new IllegalArgumentException("Некорректный логический признак анализа.");
    }
    return Boolean.parseBoolean(value);
  }

  private static String empty(final String value) {
    return value == null ? "" : value;
  }

  private static String nullable(final String value) {
    return value.isEmpty() ? null : value;
  }

  private static String num(final BigDecimal value) {
    return value == null ? "" : value.toPlainString();
  }

  private static String nums(final List<BigDecimal> values) {
    return String.join(",", values.stream().map(QchemLaboratoryEvidenceCodec::num).toList());
  }

  private static List<BigDecimal> nums(final String value) {
    return list(value).stream().map(BigDecimal::new).toList();
  }

  private static List<String> list(final String value) {
    return value.isEmpty() ? List.of() : List.of(value.split(",", -1));
  }

  private static String atoms(final Set<Assignment> values) {
    return String.join(
        ",",
        values.stream()
            .map(a -> (a.attachedProton() ? "H@" : "") + a.atomId().value())
            .sorted()
            .toList());
  }

  private static Set<Assignment> atoms(final String value) {
    final Set<Assignment> result = new LinkedHashSet<>();
    for (final var text : list(value)) {
      final boolean h = text.startsWith("H@");
      if (!result.add(new Assignment(AtomId.of(h ? text.substring(2) : text), h))) {
        throw new IllegalArgumentException("Повтор назначения атома.");
      }
    }
    return Set.copyOf(result);
  }

  private static void line(final StringBuilder out, final String command, final String... fields) {
    out.append("  ").append(command);
    for (int i = 0; i < fields.length; i += 2) {
      out.append(' ').append(fields[i]).append('=').append(QchemEscaper.quote(fields[i + 1]));
    }
    out.append("\r\n");
  }
}
