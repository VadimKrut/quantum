/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.pathcreator.vadim.quantum.chemistry.library.application.ChemistryLibraryService;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryCategory;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryDifficulty;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryQuery;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryRegistry;
import ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.filesystem.ChemistryLibraryFileRepository;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

public final class ChemistryLibraryServiceTest {

  @Test
  public void builtInRegistryContainsSearchableMoleculesAndReactions() {
    final ChemistryLibraryService service = new ChemistryLibraryService();
    final ChemistryLibraryRegistry registry = service.builtInRegistry();
    Assertions.assertTrue(registry.size() >= 8);

    final ChemistryLibraryEntry acetone = registry.require("molecule.acetone");
    Assertions.assertEquals(ChemistryLibraryEntryKind.MOLECULE, acetone.kind());
    Assertions.assertEquals(1, acetone.document().molecules().size());
    Assertions.assertEquals("acetone", acetone.document().molecules().get(0).id().value());

    final List<ChemistryLibraryEntry> russianSearch =
        registry.search(ChemistryLibraryQuery.builder().text("ацетон").build());
    Assertions.assertEquals(List.of(acetone), russianSearch);

    final List<ChemistryLibraryEntry> reactionSearch =
        registry.search(
            ChemistryLibraryQuery.builder()
                .kind(ChemistryLibraryEntryKind.REACTION)
                .requiredTag("balanced")
                .build());
    Assertions.assertEquals(1, reactionSearch.size());
    Assertions.assertEquals("reaction.methane_combustion", reactionSearch.get(0).id());
    Assertions.assertEquals(4, reactionSearch.get(0).document().molecules().size());
    Assertions.assertEquals(1, reactionSearch.get(0).document().reactions().size());
  }

  @Test
  public void queryFiltersByCategoryDifficultyTagsTextAndLimit() {
    final ChemistryLibraryRegistry registry = new ChemistryLibraryService().builtInRegistry();
    final List<ChemistryLibraryEntry> solvents =
        registry.search(
            ChemistryLibraryQuery.builder()
                .category(ChemistryLibraryCategory.SOLVENT)
                .difficulty(ChemistryLibraryDifficulty.STANDARD)
                .requiredTag("solvent")
                .limit(1)
                .build());
    Assertions.assertEquals(1, solvents.size());
    Assertions.assertEquals(ChemistryLibraryCategory.SOLVENT, solvents.get(0).category());

    final List<ChemistryLibraryEntry> absent =
        registry.search(ChemistryLibraryQuery.builder().text("not_existing_entry").build());
    Assertions.assertTrue(absent.isEmpty());
  }

  @Test
  public void registryRejectsDuplicateEntryIds() {
    final ChemistryLibraryEntry acetone =
        new ChemistryLibraryService().requireBuiltIn("molecule.acetone");
    final Executable duplicateRegistryCreation =
        new Executable() {
          public void execute() {
            ChemistryLibraryRegistry.of(List.of(acetone, acetone));
          }
        };
    Assertions.assertThrows(
        IllegalArgumentException.class,
        duplicateRegistryCreation);
  }

  @Test
  public void fileRepositorySavesLoadsAndCombinesUserEntries() throws Exception {
    final ChemistryLibraryService service = new ChemistryLibraryService();
    final Path directory = Files.createTempDirectory("chemistry-library-test");
    final ChemistryLibraryFileRepository repository = new ChemistryLibraryFileRepository(directory);
    final ChemistryLibraryEntry acetone = service.requireBuiltIn("molecule.acetone");
    final ChemistryLibraryEntry custom =
        ChemistryLibraryEntry.of(
            "user.acetone.copy",
            "User acetone copy",
            ChemistryLibraryEntryKind.MOLECULE,
            ChemistryLibraryCategory.ORGANIC,
            ChemistryLibraryDifficulty.STANDARD,
            "User saved acetone entry.",
            List.of("acetone", "user", "ацетон"),
            List.of("local"),
            acetone.document());

    service.save(repository, custom);
    final List<ChemistryLibraryEntry> loaded = repository.loadAll();
    Assertions.assertEquals(1, loaded.size());
    Assertions.assertEquals(custom.id(), loaded.get(0).id());
    Assertions.assertEquals(custom.tags(), loaded.get(0).tags());
    Assertions.assertEquals(custom.references(), loaded.get(0).references());
    Assertions.assertEquals(custom.document().molecules(), loaded.get(0).document().molecules());

    final ChemistryLibraryRegistry combined = service.combinedRegistry(repository);
    Assertions.assertNotNull(combined.find("molecule.water"));
    Assertions.assertNotNull(combined.find("user.acetone.copy"));
  }

  @Test
  public void builtInEntryCanRoundTripThroughStorageFormat() {
    final ChemistryLibraryEntry entry =
        new ChemistryLibraryService().requireBuiltIn("reaction.methane_combustion");
    final ChemistryStorageService storageService = new ChemistryStorageService();
    final ChemistryStorageResult<String> written = storageService.write(entry.document());
    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertFalse(written.value().contains("{"));

    final ChemistryStorageResult<ChemistryStorageDocument> read =
        storageService.read(written.value());
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(entry.document(), read.value());
  }
}