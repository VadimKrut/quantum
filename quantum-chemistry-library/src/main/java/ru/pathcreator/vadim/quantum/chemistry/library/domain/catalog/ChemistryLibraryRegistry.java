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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable registry библиотечных записей с детерминированным порядком и быстрым lookup по id.
 */
public final class ChemistryLibraryRegistry {

  private final List<ChemistryLibraryEntry> entries;
  private final Map<String, ChemistryLibraryEntry> byId;

  private ChemistryLibraryRegistry(
      final List<ChemistryLibraryEntry> entries,
      final Map<String, ChemistryLibraryEntry> byId) {
    this.entries = entries;
    this.byId = byId;
  }

  public static ChemistryLibraryRegistry of(final List<ChemistryLibraryEntry> entries) {
    if (entries == null) {
      throw new IllegalArgumentException("Chemistry library entries must not be null.");
    }
    final ArrayList<ChemistryLibraryEntry> checkedEntries =
        new ArrayList<ChemistryLibraryEntry>(entries.size());
    final LinkedHashMap<String, ChemistryLibraryEntry> byId =
        new LinkedHashMap<String, ChemistryLibraryEntry>();
    for (int i = 0; i < entries.size(); ++i) {
      final ChemistryLibraryEntry entry = entries.get(i);
      if (entry == null) {
        throw new IllegalArgumentException("Chemistry library entry must not be null.");
      }
      if (byId.put(entry.id(), entry) != null) {
        throw new IllegalArgumentException("Chemistry library contains duplicate entry id.");
      }
      checkedEntries.add(entry);
    }
    return new ChemistryLibraryRegistry(List.copyOf(checkedEntries), Map.copyOf(byId));
  }

  public List<ChemistryLibraryEntry> entries() {
    return this.entries;
  }

  public int size() {
    return this.entries.size();
  }

  public boolean empty() {
    return this.entries.isEmpty();
  }

  public ChemistryLibraryEntry require(final String id) {
    final ChemistryLibraryEntry entry = this.find(id);
    if (entry == null) {
      throw new IllegalArgumentException("Chemistry library entry was not found.");
    }
    return entry;
  }

  public ChemistryLibraryEntry find(final String id) {
    if (id == null) {
      return null;
    }
    return this.byId.get(id);
  }

  public List<ChemistryLibraryEntry> search(final ChemistryLibraryQuery query) {
    final ChemistryLibraryQuery checkedQuery = query == null ? ChemistryLibraryQuery.ALL : query;
    final ArrayList<ChemistryLibraryEntry> result = new ArrayList<ChemistryLibraryEntry>();
    for (int i = 0; i < this.entries.size(); ++i) {
      final ChemistryLibraryEntry entry = this.entries.get(i);
      if (!checkedQuery.matches(entry)) {
        continue;
      }
      result.add(entry);
      if (checkedQuery.limit() > 0 && result.size() >= checkedQuery.limit()) {
        break;
      }
    }
    return List.copyOf(result);
  }

  public ChemistryLibraryRegistry plus(final ChemistryLibraryEntry entry) {
    final ArrayList<ChemistryLibraryEntry> merged =
        new ArrayList<ChemistryLibraryEntry>(this.entries.size() + 1);
    merged.addAll(this.entries);
    merged.add(entry);
    return ChemistryLibraryRegistry.of(merged);
  }

  public ChemistryLibraryRegistry plusAll(final List<ChemistryLibraryEntry> additionalEntries) {
    if (additionalEntries == null || additionalEntries.isEmpty()) {
      return this;
    }
    final ArrayList<ChemistryLibraryEntry> merged =
        new ArrayList<ChemistryLibraryEntry>(this.entries.size() + additionalEntries.size());
    merged.addAll(this.entries);
    merged.addAll(additionalEntries);
    return ChemistryLibraryRegistry.of(merged);
  }
}