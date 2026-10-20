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
      final Path directory, final ChemistryStorageFileRepository storageRepository) {
    if (directory == null) {
      throw new IllegalArgumentException("Chemistry library directory must not be null.");
    }
    if (storageRepository == null) {
      throw new IllegalArgumentException("Chemistry storage repository must not be null.");
    }
    this.directory = directory;
    this.storageRepository = storageRepository;
  }

  public record Issue(Path path, String message) {}

  public record Scan(List<ChemistryLibraryEntry> entries, List<Issue> issues) {
    public Scan {
      entries = List.copyOf(entries);
      issues = List.copyOf(issues);
    }
  }

  /** Isolates damaged entries; callers must display issues instead of presenting an empty library. */
  public Scan scan() {
    final var entries = new ArrayList<ChemistryLibraryEntry>();
    final var issues = new ArrayList<Issue>();
    final var ids = new java.util.HashSet<String>();
    if (Files.notExists(this.directory)) {
      return new Scan(entries, issues);
    }
    if (Files.isSymbolicLink(this.directory)) {
      return new Scan(
          entries,
          List.of(new Issue(this.directory, "Папка библиотеки является символической ссылкой.")));
    }
    try (final var paths = Files.newDirectoryStream(this.directory)) {
      final var files = new ArrayList<Path>();
      int visited = 0;
      for (final Path path : paths) {
        if (++visited > 10000) {
          issues.add(
              new Issue(
                  this.directory,
                  "Достигнут предел 10 000 элементов папки; показана только часть библиотеки."));
          break;
        }
        if (qchemFile(path)) {
          files.add(path);
        }
      }
      files.sort(Comparator.naturalOrder());
      long totalBytes = 0;
      for (final Path file : files) {
        if (Thread.currentThread().isInterrupted()) {
          throw new java.util.concurrent.CancellationException();
        }
        try {
          if (!Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(
                "Запись должна быть обычным файлом, без символической ссылки.");
          }
          final long size = Files.size(file);
          if (size > 16L * 1024 * 1024) {
            throw new IllegalArgumentException("Запись превышает предел 16 МиБ.");
          }
          if (totalBytes + size > 64L * 1024 * 1024) {
            issues.add(
                new Issue(
                    this.directory,
                    "Достигнут предел чтения 64 МиБ; показана только часть библиотеки."));
            break;
          }
          totalBytes += size;
          final var entry = this.load(file);
          if (!ids.add(entry.id())) {
            throw new IllegalArgumentException("Повтор идентификатора записи: " + entry.id());
          }
          entries.add(entry);
        } catch (final IOException | RuntimeException exception) {
          issues.add(new Issue(file, exception.getMessage()));
        }
      }
    } catch (final IOException exception) {
      issues.add(
          new Issue(this.directory, "Не удалось прочитать папку: " + exception.getMessage()));
    }
    return new Scan(entries, issues);
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
      throw new IllegalStateException(
          "Не удалось прочитать QCHEM: "
              + result.diagnostics().stream()
                  .map(
                      diagnostic ->
                          (diagnostic.line() > 0 ? "строка " + diagnostic.line() + ": " : "")
                              + diagnostic.message())
                  .collect(java.util.stream.Collectors.joining("; ")));
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
            path, ChemistryLibraryFileRepository.documentWithDescriptor(entry));
    if (!result.success()) {
      throw new IllegalStateException("Chemistry library entry cannot be saved: " + entry.id());
    }
  }

  private static ChemistryLibraryEntry entryFromDocument(final ChemistryStorageDocument document) {
    final Map<String, String> metadata = document.metadata();
    return ChemistryLibraryEntry.of(
        metadata.getOrDefault(KEY_ID, document.projectId()),
        metadata.getOrDefault("library.entry.name", document.displayName()),
        ChemistryLibraryEntryKind.valueOf(
            metadata.getOrDefault(KEY_KIND, ChemistryLibraryEntryKind.MIXED_PROJECT.name())),
        ChemistryLibraryCategory.valueOf(
            metadata.getOrDefault(KEY_CATEGORY, ChemistryLibraryCategory.GENERAL.name())),
        ChemistryLibraryDifficulty.valueOf(
            metadata.getOrDefault(KEY_DIFFICULTY, ChemistryLibraryDifficulty.STANDARD.name())),
        metadata.getOrDefault(KEY_SUMMARY, "User chemistry library entry."),
        decodeList(metadata, KEY_TAGS),
        decodeList(metadata, KEY_REFERENCES),
        document);
  }

  private static ChemistryStorageDocument documentWithDescriptor(
      final ChemistryLibraryEntry entry) {
    final ChemistryStorageDocument source = entry.document();
    final ChemistryStorageDocument.Builder builder = source.toBuilder();
    builder.metadata("library.entry.name", entry.displayName());
    builder.metadata(KEY_ID, entry.id());
    builder.metadata(KEY_KIND, entry.kind().name());
    builder.metadata(KEY_CATEGORY, entry.category().name());
    builder.metadata(KEY_DIFFICULTY, entry.difficulty().name());
    builder.metadata(KEY_SUMMARY, entry.summary());
    builder.metadata(KEY_TAGS, ChemistryLibraryFileRepository.join(entry.tags()));
    builder.metadata(KEY_REFERENCES, ChemistryLibraryFileRepository.join(entry.references()));
    builder.metadata(KEY_TAGS + ".v2", encodeList(entry.tags()));
    builder.metadata(KEY_REFERENCES + ".v2", encodeList(entry.references()));
    return builder.build();
  }

  /** Descriptor metadata is the only part removed when opening a library notebook. */
  public static ChemistryStorageDocument payload(final ChemistryLibraryEntry entry) {
    final var builder = entry.document().toBuilder();
    entry.document().metadata().keySet().stream()
        .filter(key -> key.startsWith("library.entry."))
        .forEach(builder::removeMetadata);
    return builder.build();
  }

  private static String encodeList(final List<String> values) {
    if (values.isEmpty()) {
      return "-";
    }
    return values.stream()
        .map(
            value ->
                java.util.Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        .collect(java.util.stream.Collectors.joining("."));
  }

  private static List<String> decodeList(final Map<String, String> metadata, final String key) {
    final String encoded = metadata.get(key + ".v2");
    if (encoded == null) {
      return split(metadata.get(key));
    }
    if (encoded.equals("-")) {
      return List.of();
    }
    return java.util.Arrays.stream(encoded.split("\\.", -1))
        .map(
            value ->
                new String(
                    java.util.Base64.getUrlDecoder().decode(value),
                    java.nio.charset.StandardCharsets.UTF_8))
        .toList();
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
    return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(EXTENSION);
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
