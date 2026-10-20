/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Project-local stable compound identities. Relationships never make two graphs interchangeable. */
public record LaboratoryCompoundRegistry(
    int nextNumber, List<Compound> compounds, List<Relation> relations) {
  public static final int MAX_COMPOUNDS = 2000;
  public static final int MAX_RELATIONS = 10000;
  public static final LaboratoryCompoundRegistry EMPTY =
      new LaboratoryCompoundRegistry(1, List.of(), List.of());

  public record Compound(
      String id,
      String displayName,
      Molecule molecule,
      List<String> aliases,
      String source,
      Instant registeredAt) {
    public Compound {
      if (id == null || !id.matches("QC[0-9]{7}")) {
        throw new IllegalArgumentException("Идентификатор реестра должен иметь вид QC0000001.");
      }
      displayName = text(displayName, 1000, true);
      Objects.requireNonNull(molecule);
      Objects.requireNonNull(registeredAt);
      if (molecule.atomCount() == 0 || molecule.atomCount() > 2000 || molecule.bondCount() > 8000) {
        throw new IllegalArgumentException("Структура реестра должна содержать 1–2000 атомов.");
      }
      aliases = List.copyOf(aliases);
      if (aliases.size() > 100) {
        throw new IllegalArgumentException("У соединения слишком много названий.");
      }
      final var names = new HashSet<String>();
      for (final String alias : aliases) {
        if (!names.add(text(alias, 1000, true).toLowerCase(Locale.ROOT))) {
          throw new IllegalArgumentException("Повтор альтернативного названия.");
        }
      }
      source = text(source, 20000, true);
    }

    @Override
    public String toString() {
      return id + " · " + displayName;
    }
  }

  public enum Kind {
    SALT_OF("Соль соединения"),
    SOLVATE_OF("Сольват соединения"),
    TAUTOMER_OF("Таутомер"),
    STEREOISOMER_OF("Стереоизомер"),
    PARENT_GROUP("Одна родительская группа");
    private final String title;

    Kind(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public record Relation(String id, String firstId, String secondId, Kind kind, String note) {
    public Relation {
      if (id == null || id.length() > 200) {
        throw new IllegalArgumentException("Некорректный идентификатор связи.");
      }
      id = IdentifierValue.requireIdentifier(id, "Registry relation");
      Objects.requireNonNull(firstId);
      Objects.requireNonNull(secondId);
      Objects.requireNonNull(kind);
      if (firstId.equals(secondId)) {
        throw new IllegalArgumentException("Связь должна соединять две разные записи.");
      }
      note = text(note, 10000, true);
    }
  }

  public LaboratoryCompoundRegistry {
    compounds = List.copyOf(compounds);
    relations = List.copyOf(relations);
    if (nextNumber < 1
        || nextNumber > 10000000
        || compounds.size() > MAX_COMPOUNDS
        || relations.size() > MAX_RELATIONS) {
      throw new IllegalArgumentException("Достигнут предел локального реестра.");
    }
    final var ids = new HashSet<String>();
    final var moleculeIds = new HashSet<String>();
    for (final var compound : compounds) {
      if (!ids.add(compound.id())
          || !moleculeIds.add(compound.molecule().id().value())
          || Integer.parseInt(compound.id().substring(2)) >= nextNumber) {
        throw new IllegalArgumentException(
            "Повтор или неверная последовательность идентификаторов реестра.");
      }
    }
    final var relationIds = new HashSet<String>();
    final var connections = new HashSet<String>();
    for (final var relation : relations) {
      if (!relationIds.add(relation.id())
          || !ids.contains(relation.firstId())
          || !ids.contains(relation.secondId())) {
        throw new IllegalArgumentException(
            "Связь реестра содержит отсутствующую запись или повтор ID.");
      }
      final boolean symmetric =
          relation.kind() != Kind.SALT_OF && relation.kind() != Kind.SOLVATE_OF;
      final String first =
          symmetric && relation.firstId().compareTo(relation.secondId()) > 0
              ? relation.secondId()
              : relation.firstId();
      final String second =
          first.equals(relation.firstId()) ? relation.secondId() : relation.firstId();
      if (!connections.add(first + "/" + second + "/" + relation.kind())) {
        throw new IllegalArgumentException("Такая связь уже существует.");
      }
    }
  }

  public Compound compound(final String id) {
    return compounds.stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Соединение отсутствует в реестре."));
  }

  private static String text(final String value, final int maximum, final boolean required) {
    final String text = Objects.requireNonNullElse(value, "").strip();
    if (text.length() > maximum || text.indexOf('\0') >= 0 || required && text.isBlank()) {
      throw new IllegalArgumentException("Заполните поле реестра и проверьте длину текста.");
    }
    return text;
  }
}
