/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Explicit user annotations of reaction names and mechanisms; no classification is inferred from a graph. */
public record LaboratoryReactionClassification(
    List<Term> reactionNames, List<Term> mechanisms, String note) {
  public static final int MAX_TERMS = 64;
  public static final int MAX_TERM_LENGTH = 500;
  public static final int MAX_NOTE_LENGTH = 16000;
  public static final LaboratoryReactionClassification EMPTY =
      new LaboratoryReactionClassification(List.of(), List.of(), "");

  /** A stable catalog reference is optional; text is stored independently so a custom description survives catalog changes. */
  public record Term(String catalogId, String text) {
    public Term {
      text = singleLine(text, MAX_TERM_LENGTH, "Название реакции или механизма");
      if (catalogId != null) {
        catalogId = catalogId.strip();
        if (catalogId.isEmpty()) {
          catalogId = null;
        } else if (catalogId.length() > 120 || !catalogId.matches("[A-Za-z0-9_.:-]+")) {
          throw new IllegalArgumentException("Неверный идентификатор справочного термина.");
        }
      }
    }

    public boolean custom() {
      return this.catalogId == null;
    }

    public String toString() {
      return this.text;
    }
  }

  public LaboratoryReactionClassification {
    reactionNames = terms(reactionNames);
    mechanisms = terms(mechanisms);
    note = note == null ? "" : note.strip();
    if (note.length() > MAX_NOTE_LENGTH
        || note.chars()
            .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
      throw new IllegalArgumentException(
          "Примечание к классификации: не более "
              + MAX_NOTE_LENGTH
              + " символов, без управляющих знаков.");
    }
  }

  public boolean empty() {
    return this.reactionNames.isEmpty() && this.mechanisms.isEmpty() && this.note.isEmpty();
  }

  public boolean isEmpty() {
    return this.empty();
  }

  private static List<Term> terms(final List<Term> values) {
    Objects.requireNonNull(values, "Classification terms");
    if (values.size() > MAX_TERMS) {
      throw new IllegalArgumentException("В одном списке не более " + MAX_TERMS + " названий.");
    }
    final var terms = List.copyOf(values);
    final var ids = new HashSet<String>();
    final var names = new HashSet<String>();
    for (final var term : terms) {
      if (term.catalogId() != null && !ids.add(term.catalogId())
          || !names.add(term.text().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException(
            "Название или справочный термин повторяется в одном списке.");
      }
    }
    return terms;
  }

  private static String singleLine(final String value, final int max, final String field) {
    if (value == null
        || value.isBlank()
        || value.strip().length() > max
        || value.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException(field + ": от 1 до " + max + " символов в одной строке.");
    }
    return value.strip();
  }
}
