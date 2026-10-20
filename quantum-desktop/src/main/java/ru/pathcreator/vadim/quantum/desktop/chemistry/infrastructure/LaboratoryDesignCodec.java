/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperimentalDesign.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.AnalysisKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.AnalysisRef;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.Origin;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** A .qdesign is a native QCHEM extension with an immutable, fingerprinted notebook template. */
public final class LaboratoryDesignCodec {

  public static final int MAX_BYTES = LaboratoryFileCodec.MAX_DOCUMENT_BYTES * 3 / 2;
  private static final int MAX_TEMPLATE_ENCODED =
      base64Limit(LaboratoryFileCodec.MAX_DOCUMENT_BYTES);
  private static final String KIND = "laboratory-design-v1";

  public record Document(LaboratoryDrawing source, LaboratoryExperimentalDesign design) {
    public Document {
      Objects.requireNonNull(source);
      Objects.requireNonNull(design);
    }
  }

  public String write(final Document document) {
    final var d = document.design();
    final var values = new TreeMap<String, String>();
    // Preserve canonical legacy bytes used by existing QDISPENSE snapshot hashes when no new
    // provenance exists.
    final boolean provenance =
        d.runs().stream()
            .anyMatch(
                r ->
                    r.responseOrigin() != Origin.UNSPECIFIED
                        || !r.responseSource().isBlank()
                        || r.responseAnalysis() != null);
    final String template = new LaboratoryFileCodec().write(document.source());
    final byte[] templateBytes = template.getBytes(StandardCharsets.UTF_8);
    if (templateBytes.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
      throw new IllegalArgumentException("Исходный опыт превышает 64 МиБ.");
    }
    put(values, "id", d.id());
    put(values, "title", d.title());
    put(values, "stage", d.stageId());
    put(values, "reference", d.referenceId());
    put(values, "base", d.baseQuantity().value());
    put(values, "baseUnit", d.baseQuantity().unit().name());
    put(values, "seed", d.seed());
    put(values, "response", d.responseName());
    put(values, "responseUnit", d.responseUnit());
    put(values, "solvents", d.scaleSolvents());
    put(values, "rows", d.plateRows());
    put(values, "columns", d.plateColumns());
    put(values, "factors", d.factors().size());
    put(values, "runs", d.runs().size());
    for (int i = 0; i < d.factors().size(); i++) {
      final var f = d.factors().get(i);
      final String p = "factor." + i + ".";
      put(values, p + "id", f.id());
      put(values, p + "title", f.title());
      put(values, p + "type", f.type().name());
      put(values, p + "binding", f.binding().name());
      put(values, p + "component", Objects.toString(f.componentId(), ""));
      put(values, p + "unit", f.unit());
      put(values, p + "low", f.low());
      put(values, p + "high", f.high());
    }
    for (int i = 0; i < d.runs().size(); i++) {
      final var run = d.runs().get(i);
      final String p = "run." + i + ".";
      put(values, p + "id", run.id());
      put(values, p + "standard", run.standardOrder());
      put(values, p + "replicate", run.replicate());
      put(values, p + "center", run.center());
      put(values, p + "position", run.position());
      put(values, p + "file", run.experimentFile());
      put(values, p + "experiment", run.experimentId());
      put(values, p + "response", run.response() == null ? "" : run.response().toPlainString());
      put(values, p + "note", run.responseNote());
      if (provenance) {
        put(values, p + "origin", run.responseOrigin().name());
        put(values, p + "source", run.responseSource());
        put(
            values,
            p + "analysisKind",
            run.responseAnalysis() == null ? "" : run.responseAnalysis().kind().name());
        put(
            values,
            p + "analysisId",
            run.responseAnalysis() == null ? "" : run.responseAnalysis().id());
      }
      for (int j = 0; j < d.factors().size(); j++) {
        put(values, p + "level." + j, run.levels().get(d.factors().get(j).id()));
      }
    }
    final var lines = new ArrayList<String>();
    final var encoder = Base64.getEncoder();
    values.forEach(
        (key, value) ->
            lines.add(key + "\t" + encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8))));
    final String encoded = encoder.encodeToString(templateBytes);
    for (int i = 0; i < encoded.length(); i += 65536) {
      lines.add("template\t" + encoded.substring(i, Math.min(encoded.length(), i + 65536)));
    }
    final var extension =
        ChemistryStorageExtension.of(
            KIND,
            d.id(),
            Map.of("schema", provenance ? "2" : "1", "templateHash", fingerprint(templateBytes)),
            lines);
    final var result =
        new ChemistryStorageService()
            .write(
                ChemistryStorageDocument.builder(d.id(), d.title()).extension(extension).build());
    if (!result.success()) {
      throw new IllegalArgumentException(
          "Не удалось записать план эксперимента: " + result.diagnostics());
    }
    requireUtf8Limit(result.value(), MAX_BYTES, "План эксперимента");
    return result.value();
  }

  public Document read(final String content) {
    requireUtf8Limit(content, MAX_BYTES, "План эксперимента");
    final var decoded = new ChemistryStorageService().read(content);
    if (!decoded.success()) {
      throw new IllegalArgumentException("QDESIGN: " + decoded.diagnostics());
    }
    final var storage = decoded.value();
    if (storage.extensions().size() != 1 || !storage.extensions().getFirst().kind().equals(KIND)) {
      throw new IllegalArgumentException("Файл не содержит поддержанный план эксперимента.");
    }
    final var extension = storage.extensions().getFirst();
    final String schema = extension.properties().get("schema");
    if (!extension.properties().keySet().equals(Set.of("schema", "templateHash"))
        || !("1".equals(schema) || "2".equals(schema))) {
      throw new IllegalArgumentException("Неизвестные поля или версия плана эксперимента.");
    }
    final var expected =
        ChemistryStorageDocument.builder(storage.projectId(), storage.displayName())
            .version(storage.version())
            .extension(extension)
            .build();
    if (!expected.equals(storage)) {
      throw new IllegalArgumentException(
          "Файл содержит дополнительные секции, которые этот редактор не должен удалять.");
    }
    if (extension.bodyLines().size() > 10000) {
      throw new IllegalArgumentException("Слишком много полей плана эксперимента.");
    }
    final var values = new HashMap<String, String>();
    final var template = new StringBuilder();
    for (final String line : extension.bodyLines()) {
      final int tab = line.indexOf('\t');
      if (tab < 1 || line.length() > 65550) {
        throw new IllegalArgumentException("Неверная строка плана эксперимента.");
      }
      final String key = line.substring(0, tab), encoded = line.substring(tab + 1);
      if (key.equals("template")) {
        if ((long) template.length() + encoded.length() > MAX_TEMPLATE_ENCODED) {
          throw new IllegalArgumentException("Шаблон превышает допустимый размер.");
        }
        template.append(encoded);
      } else {
        final String value = utf8(Base64.getDecoder().decode(encoded));
        if (values.put(key, value) != null) {
          throw new IllegalArgumentException("Повтор поля плана эксперимента.");
        }
      }
    }
    final byte[] templateBytes = Base64.getDecoder().decode(template.toString());
    if (templateBytes.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
      throw new IllegalArgumentException("Исходный опыт превышает 64 МиБ.");
    }
    if (!fingerprint(templateBytes).equals(extension.properties().get("templateHash"))) {
      throw new IllegalArgumentException("Контрольная сумма шаблона не совпадает.");
    }
    final var source = new LaboratoryFileCodec().read(utf8(templateBytes));
    final String id = take(values, "id"),
        title = take(values, "title"),
        stage = take(values, "stage"),
        reference = take(values, "reference");
    final var base =
        new LaboratoryQuantity(
            LaboratoryExperimentalDesign.number(take(values, "base")),
            LaboratoryQuantityUnit.valueOf(take(values, "baseUnit")));
    final long seed = Long.parseLong(take(values, "seed"));
    final String response = take(values, "response"), responseUnit = take(values, "responseUnit");
    final boolean solvents = bool(take(values, "solvents"));
    final int rows = Integer.parseInt(take(values, "rows")),
        columns = Integer.parseInt(take(values, "columns"));
    final int factorCount = count(values, "factors", 6), runCount = count(values, "runs", 256);
    final var factors = new ArrayList<Factor>();
    for (int i = 0; i < factorCount; i++) {
      final String p = "factor." + i + ".",
          fid = take(values, p + "id"),
          name = take(values, p + "title");
      final var type = Type.valueOf(take(values, p + "type"));
      final var binding = Binding.valueOf(take(values, p + "binding"));
      final String component = take(values, p + "component");
      factors.add(
          new Factor(
              fid,
              name,
              type,
              binding,
              component.isEmpty() ? null : component,
              take(values, p + "unit"),
              take(values, p + "low"),
              take(values, p + "high")));
    }
    final var runs = new ArrayList<Run>();
    for (int i = 0; i < runCount; i++) {
      final String p = "run." + i + ".", rid = take(values, p + "id");
      final int order = Integer.parseInt(take(values, p + "standard")),
          replicate = Integer.parseInt(take(values, p + "replicate"));
      final boolean center = bool(take(values, p + "center"));
      final String position = take(values, p + "position"),
          file = take(values, p + "file"),
          experiment = take(values, p + "experiment"),
          value = take(values, p + "response"),
          note = take(values, p + "note");
      final Origin origin =
          "2".equals(schema) ? Origin.valueOf(take(values, p + "origin")) : Origin.UNSPECIFIED;
      final String provenanceSource = "2".equals(schema) ? take(values, p + "source") : "";
      AnalysisRef analysis = null;
      if ("2".equals(schema)) {
        final String kind = take(values, p + "analysisKind"),
            analysisId = take(values, p + "analysisId");
        if (kind.isEmpty() != analysisId.isEmpty()) {
          throw new IllegalArgumentException("Неполная аналитическая ссылка отклика.");
        }
        if (!kind.isEmpty()) {
          analysis = new AnalysisRef(AnalysisKind.valueOf(kind), analysisId);
        }
      }
      final var levels = new LinkedHashMap<String, String>();
      for (int j = 0; j < factors.size(); j++) {
        levels.put(factors.get(j).id(), take(values, p + "level." + j));
      }
      runs.add(
          new Run(
              rid,
              order,
              replicate,
              center,
              levels,
              position,
              file,
              experiment,
              value.isEmpty() ? null : LaboratoryExperimentalDesign.number(value),
              note,
              origin,
              provenanceSource,
              analysis));
    }
    if (!values.isEmpty()
        || !id.equals(extension.id())
        || !id.equals(storage.projectId())
        || !title.equals(storage.displayName())) {
      throw new IllegalArgumentException(
          "Неизвестные поля или несовпадающее имя плана эксперимента.");
    }
    return new Document(
        source,
        new LaboratoryExperimentalDesign(
            id,
            title,
            stage,
            reference,
            base,
            factors,
            runs,
            seed,
            response,
            responseUnit,
            solvents,
            rows,
            columns));
  }

  static int base64Limit(final int bytes) {
    return Math.toIntExact(((long) bytes + 2) / 3 * 4);
  }

  /** Enforce the same UTF-8 byte budget for direct codec input and files without allocating a second large buffer. */
  static void requireUtf8Limit(final String text, final int maximum, final String label) {
    Objects.requireNonNull(text);
    long bytes = 0;
    if (text.length() > maximum) {
      throw new IllegalArgumentException(label + " превышает " + maximum / (1024 * 1024) + " МиБ.");
    }
    for (int i = 0; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (c < 0x80) {
        bytes++;
      } else if (c < 0x800) {
        bytes += 2;
      } else if (Character.isHighSurrogate(c)) {
        if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(++i))) {
          throw new IllegalArgumentException(label + ": повреждён текст Unicode.");
        }
        bytes += 4;
      } else if (Character.isLowSurrogate(c)) {
        throw new IllegalArgumentException(label + ": повреждён текст Unicode.");
      } else {
        bytes += 3;
      }
      if (bytes > maximum) {
        throw new IllegalArgumentException(
            label + " превышает " + maximum / (1024 * 1024) + " МиБ.");
      }
    }
  }

  public static String fingerprint(final byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (final java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String utf8(final byte[] bytes) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    } catch (final java.nio.charset.CharacterCodingException exception) {
      throw new IllegalArgumentException("Повреждённый UTF-8 плана эксперимента.", exception);
    }
  }

  private static boolean bool(final String value) {
    return switch (value) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalArgumentException("Неверное логическое поле плана.");
    };
  }

  private static int count(final Map<String, String> values, final String key, final int maximum) {
    final int value = Integer.parseInt(take(values, key));
    if (value < 0 || value > maximum) {
      throw new IllegalArgumentException("Неверное число элементов плана.");
    }
    return value;
  }

  private static void put(final Map<String, String> values, final String key, final Object value) {
    values.put(key, value.toString());
  }

  private static String take(final Map<String, String> values, final String key) {
    final String value = values.remove(key);
    if (value == null) {
      throw new IllegalArgumentException("Нет поля плана: " + key);
    }
    return value;
  }
}
