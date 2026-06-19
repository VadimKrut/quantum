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
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

/**
 * Immutable запись химической библиотеки с descriptor metadata и полноценным storage document.
 */
public final class ChemistryLibraryEntry {

  private final String id;
  private final String displayName;
  private final ChemistryLibraryEntryKind kind;
  private final ChemistryLibraryCategory category;
  private final ChemistryLibraryDifficulty difficulty;
  private final String summary;
  private final List<String> tags;
  private final List<String> references;
  private final ChemistryStorageDocument document;

  private ChemistryLibraryEntry(
      final String id,
      final String displayName,
      final ChemistryLibraryEntryKind kind,
      final ChemistryLibraryCategory category,
      final ChemistryLibraryDifficulty difficulty,
      final String summary,
      final List<String> tags,
      final List<String> references,
      final ChemistryStorageDocument document) {
    this.id = id;
    this.displayName = displayName;
    this.kind = kind;
    this.category = category;
    this.difficulty = difficulty;
    this.summary = summary;
    this.tags = tags;
    this.references = references;
    this.document = document;
  }

  public static ChemistryLibraryEntry of(
      final String id,
      final String displayName,
      final ChemistryLibraryEntryKind kind,
      final ChemistryLibraryCategory category,
      final ChemistryLibraryDifficulty difficulty,
      final String summary,
      final List<String> tags,
      final List<String> references,
      final ChemistryStorageDocument document) {
    if (kind == null) {
      throw new IllegalArgumentException("Chemistry library entry kind must not be null.");
    }
    if (category == null) {
      throw new IllegalArgumentException("Chemistry library entry category must not be null.");
    }
    if (difficulty == null) {
      throw new IllegalArgumentException("Chemistry library entry difficulty must not be null.");
    }
    if (document == null) {
      throw new IllegalArgumentException("Chemistry library entry document must not be null.");
    }
    return new ChemistryLibraryEntry(
        IdentifierValue.requireIdentifier(id, "Chemistry library entry id"),
        TextValue.requireText(displayName, "Chemistry library entry display name"),
        kind,
        category,
        difficulty,
        TextValue.requireText(summary, "Chemistry library entry summary"),
        ChemistryLibraryEntry.copyTexts(tags, "Chemistry library entry tag"),
        ChemistryLibraryEntry.copyTexts(references, "Chemistry library entry reference"),
        document);
  }

  public String id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
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

  public String summary() {
    return this.summary;
  }

  public List<String> tags() {
    return this.tags;
  }

  public List<String> references() {
    return this.references;
  }

  public ChemistryStorageDocument document() {
    return this.document;
  }

  public boolean hasTag(final String tag) {
    final String checkedTag = TextValue.requireText(tag, "Chemistry library tag");
    for (int i = 0; i < this.tags.size(); ++i) {
      if (!this.tags.get(i).equalsIgnoreCase(checkedTag)) {
        continue;
      }
      return true;
    }
    return false;
  }

  public boolean matchesText(final String text) {
    if (text == null || text.trim().isEmpty()) {
      return true;
    }
    final String normalized = text.trim().toLowerCase();
    if (this.id.toLowerCase().contains(normalized)
        || this.displayName.toLowerCase().contains(normalized)
        || this.summary.toLowerCase().contains(normalized)) {
      return true;
    }
    for (int i = 0; i < this.tags.size(); ++i) {
      if (this.tags.get(i).toLowerCase().contains(normalized)) {
        return true;
      }
    }
    return false;
  }

  private static List<String> copyTexts(
      final List<String> values,
      final String subjectName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> result = new ArrayList<String>(values.size());
    for (int i = 0; i < values.size(); ++i) {
      result.add(TextValue.requireText(values.get(i), subjectName));
    }
    return List.copyOf(result);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryLibraryEntry)) {
      return false;
    }
    final ChemistryLibraryEntry entry = (ChemistryLibraryEntry) other;
    return Objects.equals(this.id, entry.id)
        && Objects.equals(this.displayName, entry.displayName)
        && this.kind == entry.kind
        && this.category == entry.category
        && this.difficulty == entry.difficulty
        && Objects.equals(this.summary, entry.summary)
        && Objects.equals(this.tags, entry.tags)
        && Objects.equals(this.references, entry.references)
        && Objects.equals(this.document, entry.document);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.displayName,
        this.kind,
        this.category,
        this.difficulty,
        this.summary,
        this.tags,
        this.references,
        this.document);
  }
}