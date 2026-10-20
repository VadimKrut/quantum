/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.*;
import java.util.stream.Collectors;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionClassification;

/** Display stored user choices; catalogue aliases only broaden local search. */
public final class LaboratoryReactionClassificationText {
  private LaboratoryReactionClassificationText() {}

  public static String summary(final LaboratoryReactionClassification value) {
    final List<String> parts = new ArrayList<>();
    if (!value.reactionNames().isEmpty()) {
      parts.add("Название реакции: " + terms(value.reactionNames()));
    }
    if (!value.mechanisms().isEmpty()) {
      parts.add("Механизмы: " + terms(value.mechanisms()));
    }
    return String.join("\n", parts);
  }

  public static String searchable(final LaboratoryReactionClassification value) {
    if (value.empty()) {
      return "";
    }
    final StringBuilder text = new StringBuilder(summary(value)).append(' ').append(value.note());
    java.util.stream.Stream.concat(value.reactionNames().stream(), value.mechanisms().stream())
        .forEach(
            term -> {
              final var entry =
                  term.catalogId() == null ? null : Catalog.ENTRIES.get(term.catalogId());
              if (entry != null) {
                text.append(' ')
                    .append(entry.nameRu())
                    .append(' ')
                    .append(entry.nameEn())
                    .append(' ')
                    .append(String.join(" ", entry.aliases()));
              }
            });
    return text.toString();
  }

  private static String terms(final List<LaboratoryReactionClassification.Term> values) {
    return values.stream()
        .map(LaboratoryReactionClassification.Term::text)
        .collect(Collectors.joining(" · "));
  }

  private static final class Catalog {
    private static final Map<String, LaboratoryReactionCatalog.Entry> ENTRIES =
        LaboratoryReactionCatalog.entries().stream()
            .collect(Collectors.toUnmodifiableMap(LaboratoryReactionCatalog.Entry::id, e -> e));
  }
}
