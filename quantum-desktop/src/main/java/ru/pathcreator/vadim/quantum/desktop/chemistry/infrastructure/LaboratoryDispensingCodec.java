/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryDispensingPlan.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryDispensingService;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** Standalone .qdispense: typed planning fields plus a fingerprinted native study snapshot. */
public final class LaboratoryDispensingCodec {
  private static final int MAX_STUDY_ENCODED =
      LaboratoryDesignCodec.base64Limit(LaboratoryDesignCodec.MAX_BYTES);
  public static final int MAX_BYTES = MAX_STUDY_ENCODED + 8 * 1024 * 1024;
  private static final String KIND = "laboratory-dispensing-v1";

  public record Document(LaboratoryDesignCodec.Document study, LaboratoryDispensingPlan plan) {
    public Document {
      Objects.requireNonNull(study);
      Objects.requireNonNull(plan);
      LaboratoryDispensingService.validate(study.source(), study.design(), plan);
      if (!hash(study).equals(plan.designHash()))
        throw new IllegalArgumentException(
            "Контрольная сумма исходного исследования раздачи не совпадает.");
    }
  }

  public static String hash(final LaboratoryDesignCodec.Document study) {
    return LaboratoryDesignCodec.fingerprint(
        new LaboratoryDesignCodec().write(study).getBytes(StandardCharsets.UTF_8));
  }

  public String write(final Document document) {
    final var plan = document.plan();
    final var fields = new TreeMap<String, String>();
    put(fields, "id", plan.id());
    put(fields, "title", plan.title());
    put(fields, "time", plan.createdAt());
    put(fields, "design", plan.designId());
    put(fields, "hash", plan.designHash());
    put(fields, "reference", plan.referenceRunId());
    quantity(fields, "aliquot", plan.referenceAliquot());
    put(fields, "reserve", plan.reservePercent());
    quantity(fields, "dead", plan.deadVolume());
    put(fields, "diluent", plan.diluent());
    put(fields, "compatibility", plan.compatibilityBasis());
    put(fields, "note", plan.note());
    put(fields, "ingredients", plan.ingredients().size());
    put(fields, "runs", plan.runs().size());
    for (int i = 0; i < plan.ingredients().size(); i++) {
      final var ingredient = plan.ingredients().get(i);
      final String p = "ingredient." + i + ".";
      put(fields, p + "id", ingredient.componentId());
      put(fields, p + "common", ingredient.common());
      put(
          fields,
          p + "concentration",
          ingredient.stock() == null ? "" : ingredient.stock().value());
      put(fields, p + "unit", ingredient.stock() == null ? "" : ingredient.stock().unit().name());
      put(fields, p + "label", ingredient.label());
      put(fields, p + "solvent", ingredient.solvent());
      put(fields, p + "source", ingredient.source());
    }
    for (int i = 0; i < plan.runs().size(); i++) {
      final var run = plan.runs().get(i);
      final String p = "run." + i + ".";
      put(fields, p + "id", run.id());
      put(fields, p + "position", run.position());
      put(fields, p + "included", run.included());
      for (int j = 0; j < plan.ingredients().size(); j++) {
        quantity(
            fields, p + "amount." + j, run.amounts().get(plan.ingredients().get(j).componentId()));
      }
    }
    final var lines = new ArrayList<String>();
    final var encoder = Base64.getEncoder();
    fields.forEach(
        (key, value) ->
            lines.add(key + "\t" + encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8))));
    final String study =
        encoder.encodeToString(
            new LaboratoryDesignCodec().write(document.study()).getBytes(StandardCharsets.UTF_8));
    for (int i = 0; i < study.length(); i += 65536)
      lines.add("study\t" + study.substring(i, Math.min(i + 65536, study.length())));
    final var extension =
        ChemistryStorageExtension.of(
            KIND, plan.id(), Map.of("schema", "1", "studyHash", plan.designHash()), lines);
    final var result =
        new ChemistryStorageService()
            .write(
                ChemistryStorageDocument.builder(plan.id(), plan.title())
                    .extension(extension)
                    .build());
    if (!result.success()) throw new IllegalArgumentException("QDISPENSE: " + result.diagnostics());
    LaboratoryDesignCodec.requireUtf8Limit(result.value(), MAX_BYTES, "План раздачи");
    return result.value();
  }

  public Document read(final String text) {
    LaboratoryDesignCodec.requireUtf8Limit(text, MAX_BYTES, "План раздачи");
    final var result = new ChemistryStorageService().read(text);
    if (!result.success()) throw new IllegalArgumentException("QDISPENSE: " + result.diagnostics());
    final var storage = result.value();
    if (storage.extensions().size() != 1 || !storage.extensions().getFirst().kind().equals(KIND))
      throw new IllegalArgumentException("Файл не содержит поддержанный план раздачи.");
    final var extension = storage.extensions().getFirst();
    if (!extension.properties().keySet().equals(Set.of("schema", "studyHash"))
        || !"1".equals(extension.properties().get("schema")))
      throw new IllegalArgumentException("Неизвестная версия или поля раздачи.");
    if (!ChemistryStorageDocument.builder(storage.projectId(), storage.displayName())
        .version(storage.version())
        .extension(extension)
        .build()
        .equals(storage))
      throw new IllegalArgumentException("Файл содержит дополнительные секции.");
    if (extension.bodyLines().size() > 100000)
      throw new IllegalArgumentException("Слишком много полей раздачи.");
    final var fields = new HashMap<String, String>();
    final var encodedStudy = new StringBuilder();
    for (final String line : extension.bodyLines()) {
      final int tab = line.indexOf('\t');
      if (tab < 1 || line.length() > 65550)
        throw new IllegalArgumentException("Повреждена строка раздачи.");
      final String key = line.substring(0, tab), value = line.substring(tab + 1);
      if (key.equals("study")) {
        if ((long) encodedStudy.length() + value.length() > MAX_STUDY_ENCODED)
          throw new IllegalArgumentException("Слишком большой снимок исследования.");
        encodedStudy.append(value);
      } else if (fields.put(key, utf8(Base64.getDecoder().decode(value))) != null)
        throw new IllegalArgumentException("Повтор поля раздачи.");
    }
    final byte[] studyBytes = Base64.getDecoder().decode(encodedStudy.toString());
    if (studyBytes.length > LaboratoryDesignCodec.MAX_BYTES
        || !LaboratoryDesignCodec.fingerprint(studyBytes)
            .equals(extension.properties().get("studyHash")))
      throw new IllegalArgumentException("Не совпадает контрольная сумма исследования.");
    final var study = new LaboratoryDesignCodec().read(utf8(studyBytes));
    final String id = take(fields, "id"), title = take(fields, "title");
    final var time = Instant.parse(take(fields, "time"));
    final String design = take(fields, "design"),
        hash = take(fields, "hash"),
        reference = take(fields, "reference");
    final var aliquot = quantity(fields, "aliquot");
    final var reserve = new BigDecimal(take(fields, "reserve"));
    final var dead = quantity(fields, "dead");
    final String diluent = take(fields, "diluent"),
        compatibility = take(fields, "compatibility"),
        note = take(fields, "note");
    final int ingredientCount = count(fields, "ingredients", 128),
        runCount = count(fields, "runs", 256);
    final var ingredients = new ArrayList<Ingredient>();
    for (int i = 0; i < ingredientCount; i++) {
      final String p = "ingredient." + i + ".", cid = take(fields, p + "id");
      final boolean common = bool(take(fields, p + "common"));
      final String concentration = take(fields, p + "concentration"),
          unit = take(fields, p + "unit");
      if (concentration.isEmpty() != unit.isEmpty())
        throw new IllegalArgumentException("Неполная концентрация исходного раствора.");
      ingredients.add(
          new Ingredient(
              cid,
              common,
              concentration.isEmpty()
                  ? null
                  : new Concentration(
                      new BigDecimal(concentration), ConcentrationUnit.valueOf(unit)),
              take(fields, p + "label"),
              take(fields, p + "solvent"),
              take(fields, p + "source")));
    }
    final var runs = new ArrayList<Run>();
    for (int i = 0; i < runCount; i++) {
      final String p = "run." + i + ".",
          rid = take(fields, p + "id"),
          position = take(fields, p + "position");
      final boolean included = bool(take(fields, p + "included"));
      final Map<String, LaboratoryQuantity> amounts = new LinkedHashMap<>();
      for (int j = 0; j < ingredients.size(); j++) {
        final var q = quantity(fields, p + "amount." + j);
        if (q != null) amounts.put(ingredients.get(j).componentId(), q);
      }
      runs.add(new Run(rid, position, included, amounts));
    }
    if (!fields.isEmpty()
        || !id.equals(storage.projectId())
        || !id.equals(extension.id())
        || !title.equals(storage.displayName())
        || !hash.equals(extension.properties().get("studyHash")))
      throw new IllegalArgumentException(
          "Неизвестные поля или несовпадающие идентификаторы раздачи.");
    return new Document(
        study,
        new LaboratoryDispensingPlan(
            id,
            title,
            time,
            design,
            hash,
            runs,
            ingredients,
            reference,
            aliquot,
            reserve,
            dead,
            diluent,
            compatibility,
            note));
  }

  private static void quantity(
      final Map<String, String> fields, final String key, final LaboratoryQuantity quantity) {
    put(fields, key, quantity == null ? "" : quantity.value());
    put(fields, key + "Unit", quantity == null ? "" : quantity.unit().name());
  }

  private static LaboratoryQuantity quantity(final Map<String, String> fields, final String key) {
    final String value = take(fields, key), unit = take(fields, key + "Unit");
    if (value.isEmpty() != unit.isEmpty())
      throw new IllegalArgumentException("Неполное количество в раздаче.");
    return value.isEmpty()
        ? null
        : new LaboratoryQuantity(new BigDecimal(value), LaboratoryQuantityUnit.valueOf(unit));
  }

  private static void put(final Map<String, String> fields, final String key, final Object value) {
    fields.put(key, value.toString());
  }

  private static String take(final Map<String, String> fields, final String key) {
    final String value = fields.remove(key);
    if (value == null) throw new IllegalArgumentException("Нет поля раздачи: " + key);
    return value;
  }

  private static int count(final Map<String, String> fields, final String key, final int max) {
    final int n = Integer.parseInt(take(fields, key));
    if (n < 0 || n > max) throw new IllegalArgumentException("Неверный размер плана раздачи.");
    return n;
  }

  private static boolean bool(final String value) {
    return switch (value) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalArgumentException("Неверное логическое поле раздачи.");
    };
  }

  private static String utf8(final byte[] bytes) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    } catch (final java.nio.charset.CharacterCodingException error) {
      throw new IllegalArgumentException("Повреждён UTF-8 плана раздачи.", error);
    }
  }
}
