/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryCategory;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryDifficulty;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.repository.ChemistryLibraryRepository;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.filesystem.ChemistryStorageFileRepository;

/**
 * Файловый repository пользовательской библиотеки: одна запись хранится как один .qchem file.
 */
public final class ChemistryLibraryFileRepository implements ChemistryLibraryRepository {

  private static final String EXTENSION = ".qchem";
  private static final String KEY_ID = "library.entry.id";
  private static final String KEY_KIND = "library.entry.kind";
  private static final String KEY_CATEGORY = "library.entry.category";
  private static final String KEY_DIFFICULTY = "library.entry.difficulty";
  private static final String KEY_SUMMARY = "library.entry.summary";
  private static final String KEY_TAGS = "library.entry.tags";
  private static final String KEY_REFERENCES = "library.entry.references";

  private final Path directory;
  private final ChemistryStorageFileRepository storageRepository;

  public ChemistryLibraryFileRepository(final Path directory) {
    this(directory, new ChemistryStorageFileRepository());
  }

  public ChemistryLibraryFileRepository(
      final Path directory,
      final ChemistryStorageFileRepository storageRepository) {
    if (directory == null) {
      throw new IllegalArgumentException("Chemistry library directory must not be null.");
    }
    if (storageRepository == null) {
      throw new IllegalArgumentException("Chemistry storage repository must not be null.");
    }
    this.directory = directory;
    this.storageRepository = storageRepository;
  }

  public List<ChemistryLibraryEntry> loadAll() {
    final ArrayList<ChemistryLibraryEntry> entries = new ArrayList<ChemistryLibraryEntry>();
    if (!Files.exists(this.directory)) {
      return List.of();
    }
    try (DirectoryStream<Path> paths = Files.newDirectoryStream(this.directory)) {
      final ArrayList<Path> files = new ArrayList<Path>();
      for (final Path path : paths) {
        if (!Files.isRegularFile(path) || !ChemistryLibraryFileRepository.qchemFile(path)) {
          continue;
        }
        files.add(path);
      }
      files.sort(Comparator.naturalOrder());
      for (int i = 0; i < files.size(); ++i) {
        entries.add(this.load(files.get(i)));
      }
    } catch (final IOException exception) {
      throw new IllegalStateException("Chemistry library directory cannot be read.", exception);
    }
    return List.copyOf(entries);
  }

  public ChemistryLibraryEntry load(final Path path) {
    final ChemistryStorageResult<ChemistryStorageDocument> result =
        this.storageRepository.read(path);
    if (!result.success()) {
      throw new IllegalStateException("Chemistry library entry cannot be read: " + path);
    }
    return ChemistryLibraryFileRepository.entryFromDocument(result.value());
  }

  public void save(final ChemistryLibraryEntry entry) {
    if (entry == null) {
      throw new IllegalArgumentException("Chemistry library entry must not be null.");
    }
    final Path path = this.directory.resolve(ChemistryLibraryFileRepository.fileName(entry.id()));
    final ChemistryStorageResult<Path> result =
        this.storageRepository.write(
            path,
            ChemistryLibraryFileRepository.documentWithDescriptor(entry));
    if (!result.success()) {
      throw new IllegalStateException("Chemistry library entry cannot be saved: " + entry.id());
    }
  }

  private static ChemistryLibraryEntry entryFromDocument(final ChemistryStorageDocument document) {
    final Map<String, String> metadata = document.metadata();
    return ChemistryLibraryEntry.of(
        metadata.getOrDefault(KEY_ID, document.projectId()),
        document.displayName(),
        ChemistryLibraryEntryKind.valueOf(
            metadata.getOrDefault(KEY_KIND, ChemistryLibraryEntryKind.MIXED_PROJECT.name())),
        ChemistryLibraryCategory.valueOf(
            metadata.getOrDefault(KEY_CATEGORY, ChemistryLibraryCategory.GENERAL.name())),
        ChemistryLibraryDifficulty.valueOf(
            metadata.getOrDefault(KEY_DIFFICULTY, ChemistryLibraryDifficulty.STANDARD.name())),
        metadata.getOrDefault(KEY_SUMMARY, "User chemistry library entry."),
        ChemistryLibraryFileRepository.split(metadata.get(KEY_TAGS)),
        ChemistryLibraryFileRepository.split(metadata.get(KEY_REFERENCES)),
        document);
  }

  private static ChemistryStorageDocument documentWithDescriptor(final ChemistryLibraryEntry entry) {
    final ChemistryStorageDocument source = entry.document();
    final ChemistryStorageDocument.Builder builder =
        ChemistryStorageDocument.builder(source.projectId(), source.displayName());
    for (final Map.Entry<String, String> metadata : source.metadata().entrySet()) {
      builder.metadata(metadata.getKey(), metadata.getValue());
    }
    builder.metadata(KEY_ID, entry.id());
    builder.metadata(KEY_KIND, entry.kind().name());
    builder.metadata(KEY_CATEGORY, entry.category().name());
    builder.metadata(KEY_DIFFICULTY, entry.difficulty().name());
    builder.metadata(KEY_SUMMARY, entry.summary());
    builder.metadata(KEY_TAGS, ChemistryLibraryFileRepository.join(entry.tags()));
    builder.metadata(KEY_REFERENCES, ChemistryLibraryFileRepository.join(entry.references()));
    for (int i = 0; i < source.molecules().size(); ++i) {
      builder.molecule(source.molecules().get(i));
    }
    for (int i = 0; i < source.reactions().size(); ++i) {
      builder.reaction(source.reactions().get(i));
    }
    for (int i = 0; i < source.extensions().size(); ++i) {
      builder.extension(source.extensions().get(i));
    }
    return builder.build();
  }

  private static List<String> split(final String value) {
    if (value == null || value.isEmpty() || "none".equals(value)) {
      return List.of();
    }
    final String[] parts = value.split("\\|", -1);
    final ArrayList<String> result = new ArrayList<String>(parts.length);
    for (int i = 0; i < parts.length; ++i) {
      if (parts[i].isEmpty()) {
        continue;
      }
      result.add(parts[i]);
    }
    return List.copyOf(result);
  }

  private static String join(final List<String> values) {
    if (values == null || values.isEmpty()) {
      return "none";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.size(); ++i) {
      if (i > 0) {
        builder.append('|');
      }
      builder.append(values.get(i));
    }
    return builder.toString();
  }

  private static boolean qchemFile(final Path path) {
    return path.getFileName().toString().endsWith(EXTENSION);
  }

  private static String fileName(final String id) {
    final StringBuilder builder = new StringBuilder(id.length() + EXTENSION.length());
    for (int i = 0; i < id.length(); ++i) {
      final char current = id.charAt(i);
      if (Character.isLetterOrDigit(current) || current == '-' || current == '_') {
        builder.append(current);
      } else {
        builder.append('_');
      }
    }
    builder.append(EXTENSION);
    return builder.toString();
  }
}