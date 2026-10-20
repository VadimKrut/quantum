/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Offline suggestions for explicit human classification, not a reaction recognizer. */
public final class LaboratoryReactionCatalog {
  public enum Kind {
    REACTION,
    MECHANISM
  }

  public record Entry(
      String id,
      Kind kind,
      String nameRu,
      String nameEn,
      List<String> aliases,
      String category,
      String description,
      String sourceUrl) {
    public Entry {
      Objects.requireNonNull(kind);
      for (final String value : List.of(id, nameRu, nameEn, category, sourceUrl)) {
        if (value.isBlank()) {
          throw new IllegalArgumentException("Пустое поле каталога реакций.");
        }
      }
      aliases = List.copyOf(aliases);
      Objects.requireNonNull(description);
      if (!id.matches("[A-Za-z0-9_:-]+") || !sourceUrl.startsWith("https://")) {
        throw new IllegalArgumentException("Некорректный идентификатор или источник каталога.");
      }
    }

    @Override
    public String toString() {
      return this.nameRu;
    }
  }

  private record Indexed(Entry entry, List<String> names, String text, String compact) {}

  private record Match(Entry entry, int score) {}

  private static final String RESOURCE =
      "/ru/pathcreator/vadim/quantum/desktop/chemistry/reaction-catalog.tsv";
  private static final List<Entry> ENTRIES = read();
  private static final List<Indexed> INDEX =
      ENTRIES.stream()
          .map(
              entry -> {
                final var names = new ArrayList<String>();
                names.add(fold(entry.nameRu()));
                names.add(fold(entry.nameEn()));
                entry.aliases().forEach(alias -> names.add(fold(alias)));
                final String text =
                    String.join(" ", names)
                        + " "
                        + fold(entry.category())
                        + " "
                        + fold(entry.description());
                return new Indexed(entry, List.copyOf(names), text, text.replace(" ", ""));
              })
          .toList();

  private LaboratoryReactionCatalog() {}

  /** Stable identifiers and labels; custom user labels need not belong to this list. */
  public static List<Entry> entries() {
    return ENTRIES;
  }

  public static List<String> categories(final Kind kind) {
    return ENTRIES.stream()
        .filter(entry -> kind == null || entry.kind() == kind)
        .map(Entry::category)
        .distinct()
        .sorted()
        .toList();
  }

  /** Null kind or blank category means all; search tolerates accents, Ё, punctuation and subscripts. */
  public static List<Entry> search(final String query, final Kind kind, final String category) {
    final String wanted = fold(Objects.requireNonNullElse(query, ""));
    final String group = fold(Objects.requireNonNullElse(category, ""));
    final String compact = wanted.replace(" ", "");
    final String[] words = wanted.isEmpty() ? new String[0] : wanted.split(" ");
    final List<Match> matches = new ArrayList<>();
    for (final var indexed : INDEX) {
      final var entry = indexed.entry();
      if (kind != null && entry.kind() != kind
          || !group.isEmpty() && !fold(entry.category()).equals(group)) {
        continue;
      }
      int score = wanted.isEmpty() ? 0 : -1;
      if (!wanted.isEmpty()) {
        if (indexed.names().stream()
            .anyMatch(name -> name.equals(wanted) || name.replace(" ", "").equals(compact))) {
          score = 100;
        } else if (indexed.names().stream().anyMatch(name -> name.startsWith(wanted))) {
          score = 80;
        } else if (indexed.names().stream().anyMatch(name -> name.contains(wanted))) {
          score = 60;
        } else if (java.util.Arrays.stream(words).allMatch(indexed.text()::contains)) {
          score = 40;
        } else if (indexed.compact().contains(compact)) {
          score = 20;
        }
      }
      if (score >= 0) {
        matches.add(new Match(entry, score));
      }
    }
    return matches.stream()
        .sorted(
            Comparator.comparingInt(Match::score)
                .reversed()
                .thenComparing(match -> fold(match.entry().nameRu()))
                .thenComparing(match -> match.entry().id()))
        .map(Match::entry)
        .toList();
  }

  private static String fold(final String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replace('ё', 'е')
        .replaceAll("[^\\p{L}\\p{N}]+", " ")
        .strip();
  }

  private static List<Entry> read() {
    final var input = LaboratoryReactionCatalog.class.getResourceAsStream(RESOURCE);
    if (input == null) {
      throw new IllegalStateException("Отсутствует встроенный каталог реакций.");
    }
    final List<Entry> values = new ArrayList<>();
    final var ids = new HashSet<String>();
    try (final var reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String line;
      int number = 0;
      while ((line = reader.readLine()) != null) {
        number++;
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        final String[] fields = line.split("\\t", -1);
        if (fields.length != 8 || line.length() > 16000 || values.size() >= 2000) {
          throw new IllegalStateException("Повреждена строка каталога " + number + ".");
        }
        final var entry =
            new Entry(
                fields[0],
                Kind.valueOf(fields[1]),
                fields[2],
                fields[3],
                fields[4].isEmpty() ? List.of() : List.of(fields[4].split("\\|", -1)),
                fields[5],
                fields[6],
                fields[7]);
        if (!ids.add(entry.id())) {
          throw new IllegalStateException("Повтор идентификатора каталога: " + entry.id());
        }
        values.add(entry);
      }
    } catch (final IOException error) {
      throw new IllegalStateException("Не удалось прочитать каталог реакций.", error);
    }
    return List.copyOf(values);
  }
}
