/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Strict sample/operation records since QCHEM 6; portable external provenance requires QCHEM 10. */
final class QchemLaboratorySampleCodec {
  private QchemLaboratorySampleCodec() {}

  static void write(final StringBuilder out, final List<LaboratorySample> samples) {
    for (final var s : samples) {
      out.append("  sample-v1");
      field(out, "id", s.id());
      field(out, "code", s.code());
      field(out, "component", s.componentId());
      field(out, "stage", s.stageId() == null ? "" : s.stageId());
      field(out, "stageTitle", s.stageTitle());
      field(out, "kind", s.kind().name());
      field(out, "molecule", s.molecule().id().value());
      field(out, "parents", String.join(",", s.parents()));
      out.append("\r\n");
      if (s.origin() != null) {
        final var o = s.origin();
        out.append("  sample-origin-v1");
        field(out, "sample", s.id());
        field(out, "transfer", o.transferId());
        field(out, "experiment", o.source().experimentId());
        field(out, "sourceSample", o.source().sampleId());
        field(out, "sourceCode", o.sourceCode());
        field(out, "sourceTitle", o.sourceTitle());
        field(out, "pathHint", o.sourcePathHint());
        field(out, "analyses", String.join(",", o.analysisIds()));
        out.append("\r\n");
      }
      for (final var e : s.events()) {
        out.append("  sample-event-v1");
        field(out, "sample", s.id());
        field(out, "id", e.id());
        field(out, "time", e.time().toString());
        field(out, "action", e.action().name());
        field(out, "label", e.label());
        field(out, "location", e.location());
        field(out, "container", e.container());
        field(out, "note", e.note());
        field(out, "related", e.relatedSampleId() == null ? "" : e.relatedSampleId());
        if (e.quantity() != null) {
          field(out, "quantity", e.quantity().value().toPlainString());
          field(out, "unit", e.quantity().unit().name());
        }
        out.append("\r\n");
      }
    }
  }

  static List<LaboratorySample> read(
      final List<QchemLine> body, final Map<String, Molecule> molecules) {
    final Map<String, QchemLine> headers = new LinkedHashMap<>();
    final Map<String, List<Event>> events = new LinkedHashMap<>();
    final Map<String, LaboratorySampleOrigin> origins = new LinkedHashMap<>();
    for (final var line : body) {
      if (line.command().startsWith("sample-")) {
        switch (line.command()) {
          case "sample-v1" -> {
            keys(
                line,
                Set.of(
                    "id",
                    "code",
                    "component",
                    "stage",
                    "stageTitle",
                    "kind",
                    "molecule",
                    "parents"));
            if (headers.put(value(line, "id"), line) != null) {
              throw new IllegalArgumentException("Повтор образца.");
            }
          }
          case "sample-origin-v1" -> {
            keys(
                line,
                Set.of(
                    "sample",
                    "transfer",
                    "experiment",
                    "sourceSample",
                    "sourceCode",
                    "sourceTitle",
                    "pathHint",
                    "analyses"));
            final String ids = value(line, "analyses");
            final var origin =
                new LaboratorySampleOrigin(
                    value(line, "transfer"),
                    new LaboratorySampleReference(
                        value(line, "experiment"), value(line, "sourceSample")),
                    value(line, "sourceCode"),
                    value(line, "sourceTitle"),
                    value(line, "pathHint"),
                    ids.isEmpty() ? List.of() : List.of(ids.split(",", -1)));
            if (origins.put(value(line, "sample"), origin) != null) {
              throw new IllegalArgumentException("Повтор происхождения образца.");
            }
          }
          case "sample-event-v1" -> {
            keys(
                line,
                Set.of(
                    "sample",
                    "id",
                    "time",
                    "action",
                    "label",
                    "location",
                    "container",
                    "note",
                    "related",
                    "quantity",
                    "unit"));
            if ((line.optional("quantity") == null) != (line.optional("unit") == null)) {
              throw new IllegalArgumentException("Количество и единица образца задаются вместе.");
            }
            final var quantity =
                line.optional("quantity") == null
                    ? null
                    : new LaboratoryQuantity(
                        new BigDecimal(value(line, "quantity")),
                        LaboratoryQuantityUnit.valueOf(value(line, "unit")));
            events
                .computeIfAbsent(value(line, "sample"), k -> new ArrayList<>())
                .add(
                    new Event(
                        value(line, "id"),
                        Instant.parse(value(line, "time")),
                        Action.valueOf(value(line, "action")),
                        quantity,
                        value(line, "label"),
                        value(line, "location"),
                        value(line, "container"),
                        value(line, "note"),
                        nullable(value(line, "related"))));
          }
          default ->
              throw new IllegalArgumentException(
                  "Неизвестная версия записи образца: " + line.command());
        }
      }
    }
    if (!headers.keySet().containsAll(events.keySet())
        || !headers.keySet().containsAll(origins.keySet())) {
      throw new IllegalArgumentException("Событие ссылается на отсутствующий образец.");
    }
    final List<LaboratorySample> result = new ArrayList<>();
    for (final var h : headers.values()) {
      final var molecule = molecules.get(value(h, "molecule"));
      if (molecule == null) {
        throw new IllegalArgumentException("Нет снимка структуры образца.");
      }
      result.add(
          new LaboratorySample(
              value(h, "id"),
              value(h, "code"),
              value(h, "component"),
              nullable(value(h, "stage")),
              value(h, "stageTitle"),
              Kind.valueOf(value(h, "kind")),
              molecule,
              value(h, "parents").isEmpty()
                  ? List.of()
                  : List.of(value(h, "parents").split(",", -1)),
              events.getOrDefault(value(h, "id"), List.of()),
              origins.get(value(h, "id"))));
    }
    return List.copyOf(result);
  }

  private static String nullable(final String text) {
    return text.isEmpty() ? null : text;
  }

  private static void keys(final QchemLine line, final Set<String> allowed) {
    if (!allowed.containsAll(line.values().keySet())) {
      throw new IllegalArgumentException("Неизвестное поле образца.");
    }
  }

  private static String value(final QchemLine line, final String name) {
    return QchemEscaper.unquote(line.required(name));
  }

  private static void field(final StringBuilder out, final String name, final String value) {
    out.append(' ').append(name).append('=').append(QchemEscaper.quote(value));
  }
}
