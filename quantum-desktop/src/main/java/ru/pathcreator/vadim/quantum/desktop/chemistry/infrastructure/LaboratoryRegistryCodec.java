/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryCompoundRegistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryCompoundRegistry.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** Native molecules plus versioned registry metadata and existing native drawing layouts. */
public final class LaboratoryRegistryCodec {
  public static final int MAX_BYTES = 32 * 1024 * 1024;
  private static final String KIND = "laboratory-compound-registry-v1";

  public record Document(
      LaboratoryCompoundRegistry registry, Map<String, MoleculeDrawing> layouts) {
    public Document {
      Objects.requireNonNull(registry);
      layouts = Map.copyOf(layouts);
      if (layouts.size() != registry.compounds().size()) {
        throw new IllegalArgumentException("Каждой записи реестра требуется одна структура.");
      }
      for (final var compound : registry.compounds()) {
        final var drawing = layouts.get(compound.id());
        if (drawing == null
            || !drawing.molecule().equals(compound.molecule())
            || drawing.atoms().size() != compound.molecule().atomCount()
            || drawing.atoms().stream().map(a -> a.atomId()).distinct().count()
                != compound.molecule().atomCount()
            || !drawing.atoms().stream()
                .map(a -> a.atomId())
                .collect(java.util.stream.Collectors.toSet())
                .equals(
                    compound.molecule().atoms().stream()
                        .map(a -> a.id())
                        .collect(java.util.stream.Collectors.toSet()))
            || drawing.manualFields().size() > 100
            || drawing.bonds().size() > 8000) {
          throw new IllegalArgumentException(
              "Карточка реестра содержит повреждённое расположение атомов.");
        }
      }
    }

    public static Document empty() {
      return new Document(LaboratoryCompoundRegistry.EMPTY, Map.of());
    }
  }

  public String write(final Document document) {
    final var registry = document.registry();
    final var fields = new LinkedHashMap<String, String>();
    put(fields, "next", registry.nextNumber());
    put(fields, "compounds", registry.compounds().size());
    put(fields, "relations", registry.relations().size());
    final var builder =
        ChemistryStorageDocument.builder("compound_registry", "Реестр соединений проекта");
    for (int i = 0; i < registry.compounds().size(); i++) {
      final var c = registry.compounds().get(i);
      final String p = "compound." + i + ".";
      put(fields, p + "id", c.id());
      put(fields, p + "molecule", c.molecule().id().value());
      put(fields, p + "name", c.displayName());
      put(fields, p + "source", c.source());
      put(fields, p + "at", c.registeredAt());
      put(fields, p + "aliases", c.aliases().size());
      for (int j = 0; j < c.aliases().size(); j++) {
        put(fields, p + "alias." + j, c.aliases().get(j));
      }
      builder.molecule(c.molecule());
      builder.extension(LaboratoryLayoutCodec.write(c.id(), document.layouts().get(c.id())));
    }
    for (int i = 0; i < registry.relations().size(); i++) {
      final var r = registry.relations().get(i);
      final String p = "relation." + i + ".";
      put(fields, p + "id", r.id());
      put(fields, p + "first", r.firstId());
      put(fields, p + "second", r.secondId());
      put(fields, p + "kind", r.kind().name());
      put(fields, p + "note", r.note());
    }
    builder.extension(
        ChemistryStorageExtension.of(
            KIND,
            "registry_data",
            Map.of("schema", "1"),
            fields.entrySet().stream()
                .map(
                    e ->
                        e.getKey()
                            + "\t"
                            + Base64.getEncoder()
                                .encodeToString(e.getValue().getBytes(StandardCharsets.UTF_8)))
                .toList()));
    final var written = new ChemistryStorageService().write(builder.build());
    if (!written.success()) {
      throw new IllegalArgumentException("Не удалось записать реестр: " + written.diagnostics());
    }
    final String text = written.value();
    if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
      throw new IllegalArgumentException("Реестр превышает 32 МиБ; разделите проект.");
    }
    return text;
  }

  public Document read(final String text) {
    if (text.length() > MAX_BYTES || text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
      throw new IllegalArgumentException("Реестр превышает 32 МиБ.");
    }
    final var parsed = new ChemistryStorageService().read(text);
    if (!parsed.success()) {
      throw new IllegalArgumentException("Повреждённый реестр: " + parsed.diagnostics());
    }
    final var storage = parsed.value();
    final var data = storage.extensions().stream().filter(e -> e.kind().equals(KIND)).toList();
    if (data.size() != 1
        || !data.getFirst().id().equals("registry_data")
        || !data.getFirst().properties().equals(Map.of("schema", "1"))) {
      throw new IllegalArgumentException("Файл не является поддержанным реестром соединений.");
    }
    if (storage.molecules().size() > LaboratoryCompoundRegistry.MAX_COMPOUNDS
        || storage.extensions().size() != storage.molecules().size() + 1
        || data.getFirst().bodyLines().size() > 270003) {
      throw new IllegalArgumentException(
          "Слишком много записей или неизвестные расширения реестра.");
    }
    final var expected =
        ChemistryStorageDocument.builder("compound_registry", "Реестр соединений проекта")
            .version(storage.version());
    storage.molecules().forEach(expected::molecule);
    storage.extensions().forEach(expected::extension);
    if (!expected.build().equals(storage)) {
      throw new IllegalArgumentException(
          "В файле есть посторонние данные; редактор реестра не должен удалять их.");
    }
    final var fields = new HashMap<String, String>();
    for (final String line : data.getFirst().bodyLines()) {
      final int tab = line.indexOf('\t');
      if (tab < 1 || tab > 80 || line.length() > 120100) {
        throw new IllegalArgumentException("Повреждённая строка реестра.");
      }
      final String value;
      try {
        value =
            StandardCharsets.UTF_8
                .newDecoder()
                .decode(
                    java.nio.ByteBuffer.wrap(Base64.getDecoder().decode(line.substring(tab + 1))))
                .toString();
      } catch (final java.nio.charset.CharacterCodingException exception) {
        throw new IllegalArgumentException("Неверный UTF-8 реестра.", exception);
      }
      if (fields.put(line.substring(0, tab), value) != null) {
        throw new IllegalArgumentException("Поле реестра повторено.");
      }
    }
    final int next = count(fields, "next", 10000000),
        compoundCount = count(fields, "compounds", LaboratoryCompoundRegistry.MAX_COMPOUNDS),
        relationCount = count(fields, "relations", LaboratoryCompoundRegistry.MAX_RELATIONS);
    final var molecules =
        new HashMap<String, ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule>();
    storage.molecules().forEach(m -> molecules.put(m.id().value(), m));
    final var layoutRecords = new HashMap<String, ChemistryStorageExtension>();
    for (final var extension : storage.extensions()) {
      if (extension.kind().equals(KIND)) {
        continue;
      }
      if (!extension.kind().equals(LaboratoryLayoutCodec.KIND)
          || layoutRecords.put(extension.id(), extension) != null
          || extension.bodyLines().size() > 10000) {
        throw new IllegalArgumentException(
            "Повтор или неизвестное расположение структуры реестра.");
      }
      final int manual = Integer.parseInt(extension.properties().getOrDefault("manualCount", "-1"));
      if (manual < 0 || manual > 100 || extension.properties().size() != 2 + manual * 3) {
        throw new IllegalArgumentException("Неверные дополнительные поля структуры.");
      }
    }
    final var compounds = new ArrayList<Compound>();
    final var layouts = new LinkedHashMap<String, MoleculeDrawing>();
    for (int i = 0; i < compoundCount; i++) {
      final String p = "compound." + i + ".",
          id = take(fields, p + "id"),
          moleculeId = take(fields, p + "molecule"),
          name = take(fields, p + "name"),
          source = take(fields, p + "source"),
          at = take(fields, p + "at");
      final int aliasCount = count(fields, p + "aliases", 100);
      final var aliases = new ArrayList<String>();
      for (int j = 0; j < aliasCount; j++) {
        aliases.add(take(fields, p + "alias." + j));
      }
      final var molecule = molecules.remove(moleculeId);
      final var layout = layoutRecords.remove(id);
      if (molecule == null || layout == null) {
        throw new IllegalArgumentException("Отсутствует структура карточки реестра.");
      }
      compounds.add(new Compound(id, name, molecule, aliases, source, Instant.parse(at)));
      layouts.put(id, LaboratoryLayoutCodec.read(layout, molecule));
    }
    final var relations = new ArrayList<Relation>();
    for (int i = 0; i < relationCount; i++) {
      final String p = "relation." + i + ".";
      relations.add(
          new Relation(
              take(fields, p + "id"),
              take(fields, p + "first"),
              take(fields, p + "second"),
              Kind.valueOf(take(fields, p + "kind")),
              take(fields, p + "note")));
    }
    if (!fields.isEmpty() || !molecules.isEmpty() || !layoutRecords.isEmpty()) {
      throw new IllegalArgumentException("Неизвестные поля или структуры реестра.");
    }
    return new Document(new LaboratoryCompoundRegistry(next, compounds, relations), layouts);
  }

  private static void put(final Map<String, String> fields, final String key, final Object value) {
    fields.put(key, value.toString());
  }

  private static String take(final Map<String, String> fields, final String key) {
    final String value = fields.remove(key);
    if (value == null) {
      throw new IllegalArgumentException("Нет поля реестра: " + key);
    }
    return value;
  }

  private static int count(final Map<String, String> fields, final String key, final int maximum) {
    final String raw = take(fields, key);
    if (raw.length() > 8) {
      throw new IllegalArgumentException("Число записей реестра слишком велико.");
    }
    final int value = Integer.parseInt(raw);
    if (value < 0 || value > maximum) {
      throw new IllegalArgumentException("Недопустимое число записей реестра.");
    }
    return value;
  }
}
