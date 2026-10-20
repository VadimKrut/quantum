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
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Versioned, strict, human-readable observations; graph snapshots use the existing molecule codec. */
final class QchemLaboratoryAnalysisCodec {
  private QchemLaboratoryAnalysisCodec() {}

  static void write(final StringBuilder out, final List<LaboratoryAnalysis> analyses) {
    for (final var a : analyses) {
      out.append("  analysis-v1");
      field(out, "id", a.id());
      field(out, "component", a.componentId());
      field(out, "title", a.title());
      field(out, "sample", a.sample());
      if (a.sampleId() != null) {
        field(out, "sampleRef", a.sampleId());
      }
      field(out, "time", a.recordedAt().toString());
      field(out, "origin", a.origin().name());
      field(out, "instrument", a.instrument());
      field(out, "source", a.source());
      field(out, "note", a.note());
      field(out, "molecule", a.molecule().id().value());
      field(out, "attachments", String.join(",", a.attachmentIds()));
      field(out, "corrects", a.correctsId() == null ? "" : a.correctsId());
      switch (a.data()) {
        case Nmr n -> {
          field(out, "type", "NMR");
          field(out, "nucleus", n.nucleus().name());
          number(out, "frequency", n.frequencyMHz());
          field(out, "solvent", n.solvent());
          number(out, "scale", n.integralScale());
        }
        case Mass m -> {
          field(out, "type", "MS");
          field(out, "technique", m.technique());
          field(out, "ion", m.ion());
          field(out, "formula", m.ionFormula());
          number(out, "calculated", m.calculatedMz());
          number(out, "found", m.foundMz());
          field(out, "calculation", m.calculationSource());
        }
        case Tlc t -> {
          field(out, "type", "TLC");
          field(out, "stationary", t.stationary());
          field(out, "eluent", t.eluent());
          field(out, "visualization", t.visualization());
          number(out, "front", t.frontMm());
        }
      }
      out.append("\r\n");
      if (a.data() instanceof Nmr n) {
        if (n.trace() != null) {
          out.append("  analysis-trace-v1");
          field(out, "analysis", a.id());
          field(out, "payload", QchemNmrTraceCodec.encode(n.trace()));
          out.append("\r\n");
        }
        for (final var signal : n.signals()) {
          out.append("  analysis-signal-v1");
          field(out, "analysis", a.id());
          number(out, "from", signal.from());
          number(out, "to", signal.to());
          field(out, "multiplicity", signal.multiplicity());
          number(out, "integral", signal.integral());
          field(
              out,
              "couplings",
              String.join(
                  ",", signal.couplingsHz().stream().map(BigDecimal::toPlainString).toList()));
          field(
              out,
              "atoms",
              String.join(",", signal.atoms().stream().map(AtomId::value).sorted().toList()));
          field(out, "note", signal.note());
          if (signal.spectralIntegral()) {
            field(out, "integrated", "true");
          }
          out.append("\r\n");
        }
      } else if (a.data() instanceof Tlc t) {
        for (final var spot : t.spots()) {
          out.append("  analysis-spot-v1");
          field(out, "analysis", a.id());
          field(out, "label", spot.label());
          number(out, "distance", spot.distanceMm());
          number(out, "rf", spot.reportedRf());
          out.append("\r\n");
        }
      }
    }
  }

  static List<LaboratoryAnalysis> read(
      final List<QchemLine> body, final Map<String, Molecule> molecules) {
    final Map<String, QchemLine> headers = new LinkedHashMap<>();
    final Map<String, ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace> traces =
        new LinkedHashMap<>();
    final Map<String, List<Signal>> signals = new LinkedHashMap<>();
    final Map<String, List<Spot>> spots = new LinkedHashMap<>();
    for (final var line : body) {
      if (!line.command().startsWith("analysis-")) {
        continue;
      }
      switch (line.command()) {
        case "analysis-v1" -> {
          final Set<String> allowed =
              new HashSet<>(
                  List.of(
                      "id",
                      "component",
                      "title",
                      "sample",
                      "sampleRef",
                      "time",
                      "origin",
                      "instrument",
                      "source",
                      "note",
                      "molecule",
                      "attachments",
                      "corrects",
                      "type"));
          allowed.addAll(
              switch (value(line, "type")) {
                case "NMR" -> List.of("nucleus", "frequency", "solvent", "scale");
                case "MS" ->
                    List.of("technique", "ion", "formula", "calculated", "found", "calculation");
                case "TLC" -> List.of("stationary", "eluent", "visualization", "front");
                default -> throw new IllegalArgumentException("Неизвестный метод анализа.");
              });
          keys(line, allowed);
          if (headers.put(value(line, "id"), line) != null) {
            throw new IllegalArgumentException("Повтор аналитической записи.");
          }
        }
        case "analysis-trace-v1" -> {
          keys(line, Set.of("analysis", "payload"));
          if (traces.put(value(line, "analysis"), QchemNmrTraceCodec.decode(value(line, "payload")))
              != null) {
            throw new IllegalArgumentException("Повтор массива спектра.");
          }
        }
        case "analysis-signal-v1" -> {
          keys(
              line,
              Set.of(
                  "analysis",
                  "from",
                  "to",
                  "multiplicity",
                  "integral",
                  "couplings",
                  "atoms",
                  "note",
                  "integrated"));
          signals
              .computeIfAbsent(value(line, "analysis"), k -> new ArrayList<>())
              .add(
                  new Signal(
                      requiredNumber(line, "from"),
                      requiredNumber(line, "to"),
                      value(line, "multiplicity"),
                      number(line, "integral"),
                      list(value(line, "couplings")).stream().map(BigDecimal::new).toList(),
                      new LinkedHashSet<>(
                          list(value(line, "atoms")).stream().map(AtomId::of).toList()),
                      value(line, "note"),
                      integrated(line)));
        }
        case "analysis-spot-v1" -> {
          keys(line, Set.of("analysis", "label", "distance", "rf"));
          spots
              .computeIfAbsent(value(line, "analysis"), k -> new ArrayList<>())
              .add(new Spot(value(line, "label"), number(line, "distance"), number(line, "rf")));
        }
        default ->
            throw new IllegalArgumentException(
                "Неизвестная версия поля анализа: " + line.command());
      }
    }
    if (!headers.keySet().containsAll(traces.keySet())
        || !headers.keySet().containsAll(signals.keySet())
        || !headers.keySet().containsAll(spots.keySet())) {
      throw new IllegalArgumentException("Сигнал или пятно ссылается на отсутствующий анализ.");
    }
    final List<LaboratoryAnalysis> result = new ArrayList<>();
    for (final var entry : headers.entrySet()) {
      final var line = entry.getValue();
      final String id = entry.getKey(), type = value(line, "type");
      if (!type.equals("NMR") && (signals.containsKey(id) || traces.containsKey(id))
          || !type.equals("TLC") && spots.containsKey(id)) {
        throw new IllegalArgumentException("Данные не соответствуют методу анализа.");
      }
      final Data data =
          switch (type) {
            case "NMR" ->
                new Nmr(
                    Nucleus.valueOf(value(line, "nucleus")),
                    number(line, "frequency"),
                    value(line, "solvent"),
                    requiredNumber(line, "scale"),
                    signals.getOrDefault(id, List.of()),
                    traces.get(id));
            case "MS" ->
                new Mass(
                    value(line, "technique"),
                    value(line, "ion"),
                    value(line, "formula"),
                    number(line, "calculated"),
                    number(line, "found"),
                    value(line, "calculation"));
            case "TLC" ->
                new Tlc(
                    value(line, "stationary"),
                    value(line, "eluent"),
                    value(line, "visualization"),
                    number(line, "front"),
                    spots.getOrDefault(id, List.of()));
            default -> throw new IllegalArgumentException("Неизвестный метод анализа.");
          };
      final Molecule molecule = molecules.get(value(line, "molecule"));
      if (molecule == null) {
        throw new IllegalArgumentException("Нет снимка структуры анализа.");
      }
      final String corrects = value(line, "corrects");
      result.add(
          new LaboratoryAnalysis(
              id,
              value(line, "component"),
              value(line, "title"),
              value(line, "sample"),
              Instant.parse(value(line, "time")),
              Origin.valueOf(value(line, "origin")),
              value(line, "instrument"),
              value(line, "source"),
              value(line, "note"),
              molecule,
              list(value(line, "attachments")),
              corrects.isEmpty() ? null : corrects,
              data,
              line.optional("sampleRef") == null ? null : value(line, "sampleRef")));
    }
    return List.copyOf(result);
  }

  private static boolean integrated(final QchemLine line) {
    if (line.optional("integrated") == null) {
      return false;
    }
    final String text = value(line, "integrated");
    if (!text.equals("true") && !text.equals("false")) {
      throw new IllegalArgumentException("Неверный признак интеграла спектра.");
    }
    return Boolean.parseBoolean(text);
  }

  private static void keys(final QchemLine line, final Set<String> allowed) {
    if (!allowed.containsAll(line.values().keySet())) {
      throw new IllegalArgumentException("Неизвестное поле анализа.");
    }
  }

  private static List<String> list(final String text) {
    return text.isEmpty() ? List.of() : List.of(text.split(",", -1));
  }

  private static String value(final QchemLine line, final String name) {
    return QchemEscaper.unquote(line.required(name));
  }

  private static BigDecimal number(final QchemLine line, final String name) {
    return line.optional(name) == null ? null : requiredNumber(line, name);
  }

  private static BigDecimal requiredNumber(final QchemLine line, final String name) {
    return new BigDecimal(line.required(name));
  }

  private static void field(final StringBuilder out, final String name, final String value) {
    out.append(' ').append(name).append('=').append(QchemEscaper.quote(value));
  }

  private static void number(final StringBuilder out, final String name, final BigDecimal value) {
    if (value != null) {
      out.append(' ').append(name).append('=').append(value.toPlainString());
    }
  }
}
