/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** Bounded native sidecar books with compare-and-swap saves and immutable revision snapshots. */
public final class LaboratoryWorkbenchBooks {
  private static final int MAX = 24 * 1024 * 1024;

  private LaboratoryWorkbenchBooks() {}

  public record Opened<T>(T value, String fingerprint) {}

  public static Book<LaboratorySolutionBook> solutions(final Path folder) {
    return new Book<>(
        folder.resolve("preparations.qprep"),
        "laboratory-preparations-v1",
        LaboratorySolutionBook.EMPTY,
        LaboratoryWorkbenchBooks::encodeSolutions,
        LaboratoryWorkbenchBooks::decodeSolutions,
        LaboratorySolutionBook::requireAppend,
        LaboratoryWorkbenchBooks::solutionSchema);
  }

  public static Book<LaboratoryStorageMap> storage(final Path folder) {
    return new Book<>(
        folder.resolve("storage.qstore"),
        "laboratory-storage-map-v1",
        LaboratoryStorageMap.EMPTY,
        LaboratoryWorkbenchBooks::encodeStorage,
        (values, schema) -> decodeStorage(values),
        LaboratoryStorageMap::requireAppend,
        ignored -> 1);
  }

  public static final class Book<T> {
    private final Path path;
    private final String kind;
    private final T empty;
    private final Function<T, Map<String, String>> encode;
    private final BiFunction<Map<String, String>, Integer, T> decode;
    private final BiConsumer<T, T> append;
    private final ToIntFunction<T> schema;

    Book(
        Path path,
        String kind,
        T empty,
        Function<T, Map<String, String>> encode,
        BiFunction<Map<String, String>, Integer, T> decode,
        BiConsumer<T, T> append,
        ToIntFunction<T> schema) {
      this.path = path.toAbsolutePath().normalize();
      this.kind = kind;
      this.empty = empty;
      this.encode = encode;
      this.decode = decode;
      this.append = append;
      this.schema = schema;
    }

    public Path path() {
      return this.path;
    }

    public Opened<T> read() throws IOException {
      final byte[] bytes;
      try {
        bytes = bytes(this.path);
      } catch (final NoSuchFileException missing) {
        return new Opened<>(this.empty, null);
      }
      try {
        return new Opened<>(this.parse(LaboratoryFileRepository.decode(bytes)), hash(bytes));
      } catch (final RuntimeException invalid) {
        throw new IOException("Книга не прочитана: " + invalid.getMessage(), invalid);
      }
    }

    private T parse(final String content) {
      final var read = new ChemistryStorageService().read(content);
      if (!read.success()) {
        throw new IllegalArgumentException("Повреждена книга QCHEM: " + read.diagnostics());
      }
      final var doc = read.value();
      if (doc.extensions().size() != 1) {
        throw new IllegalArgumentException(
            "В книге должны быть данные одного поддерживаемого вида.");
      }
      final var ext = doc.extensions().getFirst();
      final String schemaText = ext.properties().get("schema");
      final int version =
          "1".equals(schemaText)
              ? 1
              : "2".equals(schemaText) && this.kind.equals("laboratory-preparations-v1") ? 2 : 0;
      if (!ext.kind().equals(this.kind)
          || !ext.id().equals("book")
          || version == 0
          || !ext.properties().equals(Map.of("schema", schemaText))
          || !doc.equals(
              ChemistryStorageDocument.builder("book", this.kind)
                  .version(doc.version())
                  .extension(ext)
                  .build())) {
        throw new IllegalArgumentException("Неизвестные секции или версия книги.");
      }
      if (ext.bodyLines().size() > 300000) {
        throw new IllegalArgumentException("Слишком много полей книги.");
      }
      final Map<String, String> values = new HashMap<>();
      long size = 0;
      for (final String line : ext.bodyLines()) {
        final int tab = line.indexOf('\t');
        if (tab < 1 || tab > 100 || line.length() > 60000) {
          throw new IllegalArgumentException("Неверная строка книги.");
        }
        final String key = line.substring(0, tab),
            value = utf8(Base64.getDecoder().decode(line.substring(tab + 1)));
        size += value.length();
        if (size > 10000000 || values.put(key, value) != null) {
          throw new IllegalArgumentException("Повтор поля или превышение размера книги.");
        }
      }
      final T result = this.decode.apply(values, version);
      if (!values.isEmpty()) {
        throw new IllegalArgumentException(
            "Неизвестные поля книги: " + values.keySet().stream().limit(3).toList());
      }
      return result;
    }

    private String text(final T value) {
      final var values = new TreeMap<>(this.encode.apply(value));
      final List<String> lines = new ArrayList<>();
      values.forEach(
          (k, v) ->
              lines.add(
                  k
                      + "\t"
                      + Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8))));
      final var result =
          new ChemistryStorageService()
              .write(
                  ChemistryStorageDocument.builder("book", this.kind)
                      .extension(
                          ChemistryStorageExtension.of(
                              this.kind,
                              "book",
                              Map.of("schema", Integer.toString(this.schema.applyAsInt(value))),
                              lines))
                      .build());
      if (!result.success()) {
        throw new IllegalArgumentException("Не удалось записать книгу: " + result.diagnostics());
      }
      return result.value();
    }

    public Opened<T> write(final T next, final String expected) throws IOException {
      Files.createDirectories(this.path.getParent());
      final byte[] data = LaboratoryFileRepository.encode(this.text(next));
      if (data.length > MAX || !this.parse(LaboratoryFileRepository.decode(data)).equals(next)) {
        throw new IOException(
            "Книга превышает допустимый размер или проверка сохранения не пройдена.");
      }
      final Path lockPath = this.path.resolveSibling(this.path.getFileName() + ".lock");
      try (final var channel =
              FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
          final var lock = channel.tryLock()) {
        if (lock == null) {
          throw new IOException("Книга сохраняется другим окном.");
        }
        final var current = this.read();
        if (!Objects.equals(expected, current.fingerprint())) {
          throw new IOException(
              "Книга изменилась. Обновите данные; текущая версия не перезаписана.");
        }
        this.append.accept(current.value(), next);
        final Path temporary = Files.createTempFile(this.path.getParent(), ".workbench-", ".tmp");
        try {
          durable(temporary, data);
          if (current.fingerprint() != null) {
            final Path history = this.path.resolveSibling(this.path.getFileName() + ".history");
            Files.createDirectories(history);
            final Path snapshot = history.resolve(current.fingerprint() + ".qchem");
            if (!Files.exists(snapshot, LinkOption.NOFOLLOW_LINKS)) {
              final Path temp = Files.createTempFile(history, ".revision-", ".tmp");
              try {
                durable(temp, bytes(this.path));
                Files.move(temp, snapshot);
              } finally {
                Files.deleteIfExists(temp);
              }
            }
            if (!hash(bytes(this.path)).equals(expected)) {
              throw new IOException("Книга изменилась во время сохранения.");
            }
            Files.move(
                temporary,
                this.path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
          } else {
            Files.move(temporary, this.path);
          }
          return new Opened<>(next, hash(data));
        } finally {
          Files.deleteIfExists(temporary);
        }
      } catch (final java.nio.channels.OverlappingFileLockException busy) {
        throw new IOException("Книга сохраняется другим окном.", busy);
      }
    }
  }

  private static byte[] bytes(final Path file) throws IOException {
    final var attributes =
        Files.readAttributes(
            file, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attributes.isRegularFile() || attributes.size() > MAX) {
      throw new IOException("Книга должна быть обычным файлом размером до 24 МиБ.");
    }
    try (final var input = Files.newInputStream(file)) {
      final byte[] bytes = input.readNBytes(MAX + 1);
      if (bytes.length > MAX) {
        throw new IOException("Книга превышает 24 МиБ.");
      }
      return bytes;
    }
  }

  private static void durable(final Path path, final byte[] bytes) throws IOException {
    try (final var channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
      final var buffer = ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
  }

  private static String hash(final byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (final NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static String utf8(final byte[] bytes) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    } catch (final java.nio.charset.CharacterCodingException bad) {
      throw new IllegalArgumentException("Повреждён UTF-8 книги.");
    }
  }

  private static int solutionSchema(final LaboratorySolutionBook book) {
    return book.standardizations().stream().anyMatch(s -> s.origin() != Origin.UNSPECIFIED) ? 2 : 1;
  }

  private static Map<String, String> encodeSolutions(final LaboratorySolutionBook book) {
    final boolean typed = solutionSchema(book) >= 2;
    final Map<String, String> values = new HashMap<>();
    put(values, "recipes", book.recipes().size());
    put(values, "standards", book.standardizations().size());
    for (int i = 0; i < book.recipes().size(); i++) {
      final var r = book.recipes().get(i);
      final String p = "r." + i + ".";
      put(values, p + "id", r.id());
      put(values, p + "time", r.time());
      put(values, p + "title", r.title());
      put(values, p + "batch", r.batchId());
      put(values, p + "mode", r.mode());
      put(values, p + "solvent", r.solvent());
      concentration(values, p + "target", r.target());
      quantity(values, p + "volume", r.finalVolume());
      put(values, p + "mass", r.molarMass());
      put(values, p + "purity", r.purityPercent());
      concentration(values, p + "stock", r.stock());
      put(values, p + "note", r.note());
      put(values, p + "corrects", r.correctsId());
      put(values, p + "steps", r.series().size());
      for (int j = 0; j < r.series().size(); j++) {
        final var step = r.series().get(j);
        final String q = p + "s." + j + ".";
        put(values, q + "label", step.label());
        concentration(values, q + "target", step.target());
        quantity(values, q + "volume", step.finalVolume());
      }
    }
    for (int i = 0; i < book.standardizations().size(); i++) {
      final var s = book.standardizations().get(i);
      final String p = "s." + i + ".";
      put(values, p + "id", s.id());
      put(values, p + "batch", s.batchId());
      put(values, p + "time", s.time());
      concentration(values, p + "measured", s.measured());
      put(values, p + "method", s.method());
      put(values, p + "source", s.source());
      put(values, p + "calculation", s.calculation());
      put(values, p + "corrects", s.correctsId());
      if (typed) {
        put(values, p + "origin", s.origin());
      }
    }
    return values;
  }

  private static LaboratorySolutionBook decodeSolutions(
      final Map<String, String> values, final int schema) {
    final List<Recipe> recipes = new ArrayList<>();
    final List<Standardization> standards = new ArrayList<>();
    final int count = count(values, "recipes", 2000), facts = count(values, "standards", 10000);
    for (int i = 0; i < count; i++) {
      final String p = "r." + i + ".", id = take(values, p + "id");
      final Instant time = Instant.parse(take(values, p + "time"));
      final String title = take(values, p + "title"), batch = nullable(take(values, p + "batch"));
      final Mode mode = Mode.valueOf(take(values, p + "mode"));
      final String solvent = take(values, p + "solvent");
      final var target = concentration(values, p + "target");
      final var volume = quantity(values, p + "volume");
      final var mass = decimal(take(values, p + "mass"));
      final var purity = decimal(take(values, p + "purity"));
      final var stock = concentration(values, p + "stock");
      final String note = take(values, p + "note"),
          correction = nullable(take(values, p + "corrects"));
      final int steps = count(values, p + "steps", 100);
      final List<DilutionStep> series = new ArrayList<>();
      for (int j = 0; j < steps; j++) {
        final String q = p + "s." + j + ".";
        series.add(
            new DilutionStep(
                take(values, q + "label"),
                concentration(values, q + "target"),
                quantity(values, q + "volume")));
      }
      recipes.add(
          new Recipe(
              id,
              time,
              title,
              batch,
              mode,
              solvent,
              target,
              volume,
              mass,
              purity,
              stock,
              series,
              note,
              correction));
    }
    for (int i = 0; i < facts; i++) {
      final String p = "s." + i + ".";
      standards.add(
          new Standardization(
              take(values, p + "id"),
              take(values, p + "batch"),
              Instant.parse(take(values, p + "time")),
              concentration(values, p + "measured"),
              take(values, p + "method"),
              take(values, p + "source"),
              take(values, p + "calculation"),
              nullable(take(values, p + "corrects")),
              schema >= 2 ? Origin.valueOf(take(values, p + "origin")) : Origin.UNSPECIFIED));
    }
    return new LaboratorySolutionBook(recipes, standards);
  }

  private static Map<String, String> encodeStorage(final LaboratoryStorageMap book) {
    final Map<String, String> values = new HashMap<>();
    put(values, "places", book.places().size());
    put(values, "events", book.events().size());
    for (int i = 0; i < book.places().size(); i++) {
      final var p = book.places().get(i);
      final String k = "p." + i + ".";
      put(values, k + "id", p.id());
      put(values, k + "parent", p.parentId());
      put(values, k + "kind", p.kind());
      put(values, k + "label", p.label());
      put(values, k + "rows", p.rows());
      put(values, k + "columns", p.columns());
    }
    for (int i = 0; i < book.events().size(); i++) {
      final var e = book.events().get(i);
      final String k = "e." + i + ".";
      put(values, k + "id", e.id());
      put(values, k + "itemKind", e.item().kind());
      put(values, k + "itemId", e.item().id());
      put(values, k + "time", e.time());
      put(values, k + "action", e.action());
      put(values, k + "place", e.slot() == null ? null : e.slot().placeId());
      put(values, k + "row", e.slot() == null ? 0 : e.slot().row());
      put(values, k + "column", e.slot() == null ? 0 : e.slot().column());
      put(values, k + "date", e.date());
      put(values, k + "note", e.note());
    }
    return values;
  }

  private static LaboratoryStorageMap decodeStorage(final Map<String, String> values) {
    final List<LaboratoryStorageMap.Place> places = new ArrayList<>();
    final List<LaboratoryStorageMap.Event> events = new ArrayList<>();
    final int count = count(values, "places", 2000), facts = count(values, "events", 50000);
    for (int i = 0; i < count; i++) {
      final String p = "p." + i + ".";
      places.add(
          new LaboratoryStorageMap.Place(
              take(values, p + "id"),
              nullable(take(values, p + "parent")),
              LaboratoryStorageMap.Kind.valueOf(take(values, p + "kind")),
              take(values, p + "label"),
              count(values, p + "rows", 26),
              count(values, p + "columns", 24)));
    }
    for (int i = 0; i < facts; i++) {
      final String p = "e." + i + ".", id = take(values, p + "id");
      final var item =
          new LaboratoryStorageMap.Item(
              LaboratoryStorageMap.ItemKind.valueOf(take(values, p + "itemKind")),
              take(values, p + "itemId"));
      final var time = Instant.parse(take(values, p + "time"));
      final var action = LaboratoryStorageMap.Action.valueOf(take(values, p + "action"));
      final String place = nullable(take(values, p + "place"));
      final int row = count(values, p + "row", 26), column = count(values, p + "column", 24);
      final String date = nullable(take(values, p + "date"));
      if (place == null && (row != 0 || column != 0)) {
        throw new IllegalArgumentException("Координаты без места хранения.");
      }
      events.add(
          new LaboratoryStorageMap.Event(
              id,
              item,
              time,
              action,
              place == null ? null : new LaboratoryStorageMap.Slot(place, row, column),
              date == null ? null : LocalDate.parse(date),
              take(values, p + "note")));
    }
    return new LaboratoryStorageMap(places, events);
  }

  private static void put(final Map<String, String> values, final String key, final Object value) {
    values.put(
        key,
        value == null
            ? ""
            : value instanceof Enum<?> e
                ? e.name()
                : value instanceof BigDecimal n ? n.toPlainString() : value.toString());
  }

  private static String take(final Map<String, String> values, final String key) {
    final String value = values.remove(key);
    if (value == null) {
      throw new IllegalArgumentException("Нет поля книги: " + key);
    }
    return value;
  }

  private static String nullable(final String value) {
    return value.isEmpty() ? null : value;
  }

  private static BigDecimal decimal(final String text) {
    if (text.isEmpty()) {
      return null;
    }
    if (text.length() > 80) {
      throw new IllegalArgumentException("Слишком длинное число.");
    }
    return new BigDecimal(text);
  }

  private static int count(final Map<String, String> values, final String key, final int max) {
    final int n = Integer.parseInt(take(values, key));
    if (n < 0 || n > max) {
      throw new IllegalArgumentException("Неверное число записей.");
    }
    return n;
  }

  private static void concentration(
      final Map<String, String> values, final String key, final Concentration value) {
    put(values, key, value == null ? null : value.value());
    put(values, key + "Unit", value == null ? null : value.unit());
  }

  private static Concentration concentration(final Map<String, String> values, final String key) {
    final var n = decimal(take(values, key));
    final String unit = nullable(take(values, key + "Unit"));
    if ((n == null) != (unit == null)) {
      throw new IllegalArgumentException("Концентрация и единица задаются вместе.");
    }
    return n == null ? null : new Concentration(n, ConcentrationUnit.valueOf(unit));
  }

  private static void quantity(
      final Map<String, String> values, final String key, final LaboratoryQuantity value) {
    put(values, key, value == null ? null : value.value());
    put(values, key + "Unit", value == null ? null : value.unit());
  }

  private static LaboratoryQuantity quantity(final Map<String, String> values, final String key) {
    final var n = decimal(take(values, key));
    final String unit = nullable(take(values, key + "Unit"));
    if ((n == null) != (unit == null)) {
      throw new IllegalArgumentException("Количество и единица задаются вместе.");
    }
    return n == null ? null : new LaboratoryQuantity(n, LaboratoryQuantityUnit.valueOf(unit));
  }
}
