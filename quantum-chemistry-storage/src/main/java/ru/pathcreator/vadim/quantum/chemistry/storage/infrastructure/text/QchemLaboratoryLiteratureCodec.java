/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryLiterature;

/** Bounded, lossless bibliography and explicitly qualified literature methods in QCHEM 12. */
final class QchemLaboratoryLiteratureCodec {
  private QchemLaboratoryLiteratureCodec() {}

  static void write(final StringBuilder out, final LaboratoryLiterature literature) {
    for (final var r : literature.references()) {
      out.append("  literature-reference-v1");
      field(out, "id", r.id());
      field(out, "doi", r.doi());
      field(out, "title", r.title());
      field(out, "authors", r.authors());
      field(out, "year", Integer.toString(r.year()));
      field(out, "url", r.url());
      field(out, "supplement", r.supplementUrl());
      field(out, "attachments", String.join(",", r.attachmentIds()));
      field(out, "notes", r.notes());
      if (!r.copyNote().isEmpty()) {
        field(out, "copyNote", r.copyNote());
      }
      out.append("\r\n");
    }
    for (final var m : literature.methods()) {
      out.append("  literature-method-v1");
      field(out, "id", m.id());
      field(out, "reference", m.referenceId());
      field(out, "stage", Objects.toString(m.stageId(), ""));
      field(out, "title", m.title());
      field(out, "procedure", m.procedureText());
      field(
          out,
          "yield",
          m.literatureYieldPercent() == null ? "" : m.literatureYieldPercent().toPlainString());
      field(out, "basis", m.yieldBasis());
      field(out, "notes", m.notes());
      if (!m.copyNote().isEmpty()) {
        field(out, "copyNote", m.copyNote());
      }
      out.append("\r\n");
    }
  }

  static LaboratoryLiterature read(final List<QchemLine> body) {
    final var references = new ArrayList<LaboratoryLiterature.Reference>();
    final var methods = new ArrayList<LaboratoryLiterature.Method>();
    for (final var line : body) {
      if (!line.command().startsWith("literature-")) {
        continue;
      }
      switch (line.command()) {
        case "literature-reference-v1" -> {
          keys(
              line,
              Set.of(
                  "id",
                  "doi",
                  "title",
                  "authors",
                  "year",
                  "url",
                  "supplement",
                  "attachments",
                  "notes"));
          if (references.size() >= LaboratoryLiterature.MAX_REFERENCES) {
            throw new IllegalArgumentException("Слишком много источников.");
          }
          final String attachments = value(line, "attachments", 20100),
              year = value(line, "year", 4);
          references.add(
              new LaboratoryLiterature.Reference(
                  value(line, "id", 200),
                  value(line, "doi", 1000),
                  value(line, "title", 2000),
                  value(line, "authors", 10000),
                  Integer.parseInt(year),
                  value(line, "url", 4000),
                  value(line, "supplement", 4000),
                  attachments.isEmpty() ? List.of() : List.of(attachments.split(",", -1)),
                  value(line, "notes", 20000),
                  line.values().containsKey("copyNote") ? value(line, "copyNote", 24000) : ""));
        }
        case "literature-method-v1" -> {
          keys(
              line,
              Set.of("id", "reference", "stage", "title", "procedure", "yield", "basis", "notes"));
          if (methods.size() >= LaboratoryLiterature.MAX_METHODS) {
            throw new IllegalArgumentException("Слишком много методик.");
          }
          final String stage = value(line, "stage", 200), yield = value(line, "yield", 30);
          methods.add(
              new LaboratoryLiterature.Method(
                  value(line, "id", 200),
                  value(line, "reference", 200),
                  stage.isEmpty() ? null : stage,
                  value(line, "title", 500),
                  value(line, "procedure", 100000),
                  yield.isEmpty() ? null : new BigDecimal(yield),
                  value(line, "basis", 2000),
                  value(line, "notes", 20000),
                  line.values().containsKey("copyNote") ? value(line, "copyNote", 1000) : ""));
        }
        default -> throw new IllegalArgumentException("Неизвестная версия литературной записи.");
      }
    }
    return new LaboratoryLiterature(references, methods);
  }

  private static String value(final QchemLine line, final String key, final int maximum) {
    final String raw = line.required(key);
    if (raw.length() > maximum * 6 + 2) {
      throw new IllegalArgumentException("Поле литературы слишком длинное.");
    }
    final String value = QchemEscaper.unquote(raw);
    if (value.length() > maximum) {
      throw new IllegalArgumentException("Поле литературы слишком длинное.");
    }
    return value;
  }

  private static void keys(final QchemLine line, final Set<String> keys) {
    final var actual = new HashSet<>(line.values().keySet());
    actual.remove("copyNote");
    if (!actual.equals(keys)) {
      throw new IllegalArgumentException("Неверные поля литературной записи.");
    }
  }

  private static void field(final StringBuilder out, final String key, final String value) {
    out.append(' ').append(key).append('=').append(QchemEscaper.quote(value));
  }
}
