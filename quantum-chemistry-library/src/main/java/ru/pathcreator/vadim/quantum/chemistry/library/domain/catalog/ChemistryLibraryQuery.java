/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Immutable запрос к библиотеке с фильтрами, которые одинаково подходят UI, CLI и API.
 */
public final class ChemistryLibraryQuery {

  public static final ChemistryLibraryQuery ALL =
      new ChemistryLibraryQuery(null, null, null, null, List.of(), 0);

  private final String text;
  private final ChemistryLibraryEntryKind kind;
  private final ChemistryLibraryCategory category;
  private final ChemistryLibraryDifficulty difficulty;
  private final List<String> requiredTags;
  private final int limit;

  private ChemistryLibraryQuery(
      final String text,
      final ChemistryLibraryEntryKind kind,
      final ChemistryLibraryCategory category,
      final ChemistryLibraryDifficulty difficulty,
      final List<String> requiredTags,
      final int limit) {
    this.text = text;
    this.kind = kind;
    this.category = category;
    this.difficulty = difficulty;
    this.requiredTags = requiredTags;
    this.limit = limit;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String text() {
    return this.text;
  }

  public ChemistryLibraryEntryKind kind() {
    return this.kind;
  }

  public ChemistryLibraryCategory category() {
    return this.category;
  }

  public ChemistryLibraryDifficulty difficulty() {
    return this.difficulty;
  }

  public List<String> requiredTags() {
    return this.requiredTags;
  }

  public int limit() {
    return this.limit;
  }

  public boolean matches(final ChemistryLibraryEntry entry) {
    if (entry == null) {
      return false;
    }
    if (this.kind != null && entry.kind() != this.kind) {
      return false;
    }
    if (this.category != null && entry.category() != this.category) {
      return false;
    }
    if (this.difficulty != null && entry.difficulty() != this.difficulty) {
      return false;
    }
    if (!entry.matchesText(this.text)) {
      return false;
    }
    for (int i = 0; i < this.requiredTags.size(); ++i) {
      if (entry.hasTag(this.requiredTags.get(i))) {
        continue;
      }
      return false;
    }
    return true;
  }

  public static final class Builder {

    private String text;
    private ChemistryLibraryEntryKind kind;
    private ChemistryLibraryCategory category;
    private ChemistryLibraryDifficulty difficulty;
    private final ArrayList<String> requiredTags = new ArrayList<String>();
    private int limit;

    public Builder text(final String text) {
      this.text = text == null || text.trim().isEmpty() ? null : text.trim();
      return this;
    }

    public Builder kind(final ChemistryLibraryEntryKind kind) {
      this.kind = kind;
      return this;
    }

    public Builder category(final ChemistryLibraryCategory category) {
      this.category = category;
      return this;
    }

    public Builder difficulty(final ChemistryLibraryDifficulty difficulty) {
      this.difficulty = difficulty;
      return this;
    }

    public Builder requiredTag(final String tag) {
      this.requiredTags.add(TextValue.requireText(tag, "Chemistry library required tag"));
      return this;
    }

    public Builder limit(final int limit) {
      if (limit < 0) {
        throw new IllegalArgumentException("Chemistry library query limit must not be negative.");
      }
      this.limit = limit;
      return this;
    }

    public ChemistryLibraryQuery build() {
      return new ChemistryLibraryQuery(
          this.text,
          this.kind,
          this.category,
          this.difficulty,
          List.copyOf(this.requiredTags),
          this.limit);
    }
  }
}