/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryCategory;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryDifficulty;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryRegistry;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;

/**
 * Встроенная химическая библиотека, которую можно безопасно объединять с пользовательскими каталогами.
 */
public final class BuiltInChemistryLibrary {

  private BuiltInChemistryLibrary() {
  }

  public static ChemistryLibraryRegistry registry() {
    final ArrayList<ChemistryLibraryEntry> entries = new ArrayList<ChemistryLibraryEntry>();
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.water(),
        ChemistryLibraryCategory.SOLVENT,
        ChemistryLibraryDifficulty.INTRODUCTORY,
        "Water molecule with bent geometry and two O-H bonds.",
        List.of("water", "solvent", "polar", "benchmark", "вода")));
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.methane(),
        ChemistryLibraryCategory.ORGANIC,
        ChemistryLibraryDifficulty.INTRODUCTORY,
        "Methane molecule with tetrahedral C-H framework.",
        List.of("methane", "hydrocarbon", "tetrahedral", "метан")));
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.ammonia(),
        ChemistryLibraryCategory.INORGANIC,
        ChemistryLibraryDifficulty.INTRODUCTORY,
        "Ammonia molecule with trigonal-pyramidal nitrogen center.",
        List.of("ammonia", "base", "nitrogen", "аммиак")));
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.carbonDioxide(),
        ChemistryLibraryCategory.INORGANIC,
        ChemistryLibraryDifficulty.INTRODUCTORY,
        "Carbon dioxide molecule with linear O=C=O connectivity.",
        List.of("carbon dioxide", "co2", "linear", "углекислый газ")));
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.oxygen(),
        ChemistryLibraryCategory.INORGANIC,
        ChemistryLibraryDifficulty.STANDARD,
        "Triplet oxygen reference molecule for reaction examples.",
        List.of("oxygen", "o2", "triplet", "кислород")));
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.acetone(),
        ChemistryLibraryCategory.ORGANIC,
        ChemistryLibraryDifficulty.STANDARD,
        "Acetone carbonyl solvent molecule with approximate 3D geometry.",
        List.of("acetone", "carbonyl", "ketone", "solvent", "ацетон")));
    entries.add(BuiltInChemistryLibrary.moleculeEntry(
        BuiltInMoleculeCatalog.ethanol(),
        ChemistryLibraryCategory.SOLVENT,
        ChemistryLibraryDifficulty.STANDARD,
        "Ethanol molecule for alcohol and oxidation examples.",
        List.of("ethanol", "alcohol", "solvent", "этанол")));
    entries.add(BuiltInChemistryLibrary.combustionMethaneEntry());
    return ChemistryLibraryRegistry.of(entries);
  }

  private static ChemistryLibraryEntry moleculeEntry(
      final Molecule molecule,
      final ChemistryLibraryCategory category,
      final ChemistryLibraryDifficulty difficulty,
      final String summary,
      final List<String> tags) {
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder(
                "library_" + molecule.id().value(),
                molecule.displayName())
            .metadata("library.kind", ChemistryLibraryEntryKind.MOLECULE.name())
            .metadata("library.category", category.name())
            .metadata("library.difficulty", difficulty.name())
            .molecule(molecule)
            .build();
    return ChemistryLibraryEntry.of(
        "molecule." + molecule.id().value(),
        molecule.displayName(),
        ChemistryLibraryEntryKind.MOLECULE,
        category,
        difficulty,
        summary,
        tags,
        List.of("Built-in Quantum chemistry library"),
        document);
  }

  private static ChemistryLibraryEntry combustionMethaneEntry() {
    final Molecule methane = BuiltInMoleculeCatalog.methane();
    final Molecule oxygen = BuiltInMoleculeCatalog.oxygen();
    final Molecule carbonDioxide = BuiltInMoleculeCatalog.carbonDioxide();
    final Molecule water = BuiltInMoleculeCatalog.water();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("methane_combustion"),
            "Methane combustion",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(methane, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(oxygen, StoichiometricCoefficient.of(2)))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(carbonDioxide, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(water, StoichiometricCoefficient.of(2)))));
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder("library_methane_combustion", "Methane combustion")
            .metadata("library.kind", ChemistryLibraryEntryKind.REACTION.name())
            .metadata("library.category", ChemistryLibraryCategory.REACTION.name())
            .metadata("library.difficulty", ChemistryLibraryDifficulty.STANDARD.name())
            .molecule(methane)
            .molecule(oxygen)
            .molecule(carbonDioxide)
            .molecule(water)
            .reaction(reaction)
            .extension(
                ChemistryStorageExtension.of(
                    "stoichiometry_note",
                    "methane_combustion_note",
                    Map.of("balanced", "true"),
                    List.of("CH4 + 2 O2 produces CO2 + 2 H2O")))
            .build();
    return ChemistryLibraryEntry.of(
        "reaction.methane_combustion",
        "Methane combustion",
        ChemistryLibraryEntryKind.REACTION,
        ChemistryLibraryCategory.REACTION,
        ChemistryLibraryDifficulty.STANDARD,
        "Balanced combustion reaction with molecule payload for each side.",
        List.of("reaction", "combustion", "methane", "balanced", "горение"),
        List.of("Built-in Quantum chemistry library"),
        document);
  }
}