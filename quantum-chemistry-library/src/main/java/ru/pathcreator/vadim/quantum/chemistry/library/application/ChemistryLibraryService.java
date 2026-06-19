/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library.application;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryQuery;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryRegistry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.repository.ChemistryLibraryRepository;
import ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.builtin.BuiltInChemistryLibrary;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

/**
 * Application facade для поиска, объединения и сохранения химических библиотечных записей.
 */
public final class ChemistryLibraryService {

  private final ChemistryLibraryRegistry builtInRegistry;

  public ChemistryLibraryService() {
    this(BuiltInChemistryLibrary.registry());
  }

  public ChemistryLibraryService(final ChemistryLibraryRegistry builtInRegistry) {
    if (builtInRegistry == null) {
      throw new IllegalArgumentException("Built-in chemistry library registry must not be null.");
    }
    this.builtInRegistry = builtInRegistry;
  }

  public ChemistryLibraryRegistry builtInRegistry() {
    return this.builtInRegistry;
  }

  public ChemistryLibraryRegistry combinedRegistry(final ChemistryLibraryRepository repository) {
    if (repository == null) {
      return this.builtInRegistry;
    }
    return this.builtInRegistry.plusAll(repository.loadAll());
  }

  public List<ChemistryLibraryEntry> search(final ChemistryLibraryQuery query) {
    return this.builtInRegistry.search(query);
  }

  public List<ChemistryLibraryEntry> search(
      final ChemistryLibraryRepository repository,
      final ChemistryLibraryQuery query) {
    return this.combinedRegistry(repository).search(query);
  }

  public ChemistryLibraryEntry requireBuiltIn(final String id) {
    return this.builtInRegistry.require(id);
  }

  public ChemistryStorageDocument document(final String id) {
    return this.requireBuiltIn(id).document();
  }

  public void save(
      final ChemistryLibraryRepository repository,
      final ChemistryLibraryEntry entry) {
    if (repository == null) {
      throw new IllegalArgumentException("Chemistry library repository must not be null.");
    }
    repository.save(entry);
  }
}