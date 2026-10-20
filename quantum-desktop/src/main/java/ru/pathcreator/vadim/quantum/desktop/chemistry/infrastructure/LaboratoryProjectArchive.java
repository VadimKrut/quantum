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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Byte-preserving project transport. QCHEM documents, histories and ledgers keep their identities. */
public final class LaboratoryProjectArchive {
  private static final String MANIFEST = "project.manifest";
  private static final int MAX_ENTRIES = 100_000;
  private static final int MAX_MANIFEST = 16 * 1024 * 1024;
  private static final long MAX_FILE = 2L * 1024 * 1024 * 1024;
  private static final long MAX_TOTAL = 10L * 1024 * 1024 * 1024;

  public record Item(String path, boolean directory, long size, String sha256, long modified) {}

  public record Snapshot(String name, List<Item> items) {
    public Snapshot {
      items = List.copyOf(items);
    }

    public long bytes() {
      return this.items.stream().mapToLong(Item::size).sum();
    }

    public long files() {
      return this.items.stream().filter(item -> !item.directory()).count();
    }
  }

  public Snapshot scan(final Path folder, final Consumer<String> progress) throws IOException {
    final Path root = realFolder(folder);
    if (root.getFileName() == null) {
      throw new IOException("Выберите папку проекта, не корень диска.");
    }
    final List<Item> items = new ArrayList<>();
    final long[] total = {0};
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          public FileVisitResult preVisitDirectory(
              final Path path, final BasicFileAttributes attributes) throws IOException {
            checkInterrupted();
            checkRegularPath(root, path, attributes, true);
            if (!path.equals(root)) {
              add(path, true);
            }
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult visitFile(final Path path, final BasicFileAttributes attributes)
              throws IOException {
            checkInterrupted();
            checkRegularPath(root, path, attributes, false);
            add(path, false);
            return FileVisitResult.CONTINUE;
          }

          private void add(final Path path, final boolean directory) throws IOException {
            // Windows directory enumeration can retain older attributes after child creation.
            final var before =
                Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            checkRegularPath(root, path, before, directory);
            if (items.size() >= MAX_ENTRIES) {
              throw new IOException("В проекте более 100 000 файлов и папок.");
            }
            final String relative = root.relativize(path).toString().replace('\\', '/');
            portablePath(relative);
            if (before.size() > MAX_FILE && !directory) {
              throw new IOException("Файл больше 2 ГиБ: " + relative);
            }
            if (!directory) {
              total[0] += before.size();
            }
            if (total[0] > MAX_TOTAL) {
              throw new IOException("Проект превышает поддерживаемые 10 ГиБ.");
            }
            progress.accept("Проверяю: " + relative);
            final String hash = directory ? "-" : hash(path, before.size());
            final var after =
                Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!same(before, after)) {
              throw new IOException("Файл изменился при проверке: " + relative);
            }
            items.add(
                new Item(
                    relative,
                    directory,
                    directory ? 0 : after.size(),
                    hash,
                    after.lastModifiedTime().toMillis()));
          }
        });
    items.sort(Comparator.comparing(Item::path));
    final Snapshot result = new Snapshot(root.getFileName().toString(), items);
    validate(result);
    return result;
  }

  public Snapshot inspect(final Path archive) throws IOException {
    try (final ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
      return readManifest(zip);
    }
  }

  public void export(
      final Path folder,
      final Path archive,
      final Snapshot expected,
      final Consumer<String> progress)
      throws IOException {
    final Path root = realFolder(folder);
    final Path destination = archive.toAbsolutePath().normalize();
    final Path parent = realFolder(destination.getParent());
    if (parent.startsWith(root)) {
      throw new IOException("Сохраните пакет за пределами упаковываемой папки проекта.");
    }
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
        && (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)
            || !destination.toRealPath().equals(destination))) {
      throw new IOException("Файл пакета должен быть обычным файлом, не ссылкой.");
    }
    if (!scan(root, progress).equals(expected)) {
      throw new IOException(
          "Проект изменился после предпросмотра. Проверьте состав пакета заново.");
    }
    final Path temporary = Files.createTempFile(parent, ".qchem-project-", ".tmp");
    try {
      try (final ZipOutputStream zip =
          new ZipOutputStream(Files.newOutputStream(temporary), StandardCharsets.UTF_8)) {
        zip.putNextEntry(new ZipEntry(MANIFEST));
        zip.write(manifest(expected));
        zip.closeEntry();
        for (final Item item : expected.items()) {
          checkInterrupted();
          progress.accept("Упаковываю: " + item.path());
          final ZipEntry entry = new ZipEntry(zipName(item));
          entry.setTime(item.modified());
          zip.putNextEntry(entry);
          if (!item.directory()) {
            final Path source = root.resolve(item.path());
            checkRegularPath(
                root,
                source,
                Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS),
                false);
            try (final InputStream input = Files.newInputStream(source)) {
              copyVerified(input, zip, item);
            }
          }
          zip.closeEntry();
        }
      }
      if (!scan(root, progress).equals(expected)) {
        throw new IOException(
            "Проект изменился во время упаковки. Предыдущий пакет сохранён; повторите экспорт.");
      }
      try (final ZipFile zip = new ZipFile(temporary.toFile(), StandardCharsets.UTF_8)) {
        if (!readManifest(zip).equals(expected)) {
          throw new IOException("Проверка состава пакета не пройдена.");
        }
        verifyContents(zip, expected, progress);
      }
      checkInterrupted();
      try (final FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        output.force(true);
      }
      Files.move(
          temporary,
          destination,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public Snapshot importTo(
      final Path archive,
      final Path folder,
      final Snapshot expected,
      final Consumer<String> progress)
      throws IOException {
    final Path destination = folder.toAbsolutePath().normalize();
    final Path parent = realFolder(destination.getParent());
    portablePath(destination.getFileName().toString());
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Для проекта требуется новая папка. Существующая папка не изменена.");
    }
    final Path staging = Files.createTempDirectory(parent, ".qchem-project-restore-");
    try {
      try (final ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
        final Snapshot actual = readManifest(zip);
        if (!actual.equals(expected)) {
          throw new IOException("Пакет изменился после предпросмотра. Откройте его заново.");
        }
        for (final Item item : actual.items()) {
          checkInterrupted();
          progress.accept("Восстанавливаю: " + item.path());
          final Path target = staging.resolve(item.path()).normalize();
          if (!target.startsWith(staging)) {
            throw new IOException("Путь файла выходит за папку проекта.");
          }
          if (item.directory()) {
            Files.createDirectories(target);
          } else {
            Files.createDirectories(target.getParent());
            try (final InputStream input = zip.getInputStream(zip.getEntry(zipName(item)));
                final OutputStream output =
                    Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
              copyVerified(input, output, item);
            }
            try (final FileChannel output = FileChannel.open(target, StandardOpenOption.WRITE)) {
              output.force(true);
            }
            Files.setLastModifiedTime(target, FileTime.fromMillis(item.modified()));
          }
        }
        // Restore directory dates after creating their descendants.
        for (final Item item : actual.items().reversed()) {
          if (item.directory()) {
            Files.setLastModifiedTime(
                staging.resolve(item.path()), FileTime.fromMillis(item.modified()));
          }
        }
        checkInterrupted();
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
          throw new IOException("Папка назначения появилась во время импорта. Она не изменена.");
        }
        Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
        return actual;
      }
    } finally {
      cleanup(staging);
    }
  }

  private static Snapshot readManifest(final ZipFile zip) throws IOException {
    final var header = zip.getEntry(MANIFEST);
    if (header == null
        || header.isDirectory()
        || header.getSize() < 0
        || header.getSize() > MAX_MANIFEST) {
      throw new IOException("Не найден поддержанный манифест проекта (до 16 МиБ).");
    }
    final String text;
    try (final var input = zip.getInputStream(header)) {
      final byte[] bytes = input.readNBytes(MAX_MANIFEST + 1);
      if (bytes.length != header.getSize()) {
        throw new IOException("Размер манифеста не совпадает.");
      }
      text = utf8(bytes);
    }
    final String[] lines = text.split("\n", -1);
    final String[] first = lines[0].split("\t", -1);
    if (first.length != 3 || !first[0].equals("QCHEM-PROJECT") || !first[1].equals("1")) {
      throw new IOException("Неподдержанная версия пакета проекта.");
    }
    final List<Item> items = new ArrayList<>();
    try {
      for (int i = 1; i < lines.length; i++) {
        checkInterrupted();
        if (items.size() >= MAX_ENTRIES) {
          throw new IOException("В пакете слишком много записей.");
        }
        final String[] fields = lines[i].split("\t", -1);
        if (fields.length != 5 || !Set.of("F", "D").contains(fields[0])) {
          throw new IOException("Повреждена строка манифеста " + (i + 1));
        }
        items.add(
            new Item(
                decode(fields[1]),
                fields[0].equals("D"),
                Long.parseLong(fields[2]),
                fields[3],
                Long.parseLong(fields[4])));
      }
      final Snapshot result = new Snapshot(decode(first[2]), items);
      validate(result);
      final Map<String, Item> expected = new HashMap<>();
      items.forEach(item -> expected.put(zipName(item), item));
      final Set<String> seen = new HashSet<>();
      final var entries = zip.entries();
      while (entries.hasMoreElements()) {
        checkInterrupted();
        final var entry = entries.nextElement();
        if (!seen.add(entry.getName())) {
          throw new IOException("Повтор пути в ZIP: " + entry.getName());
        }
        if (entry.getName().equals(MANIFEST)) {
          continue;
        }
        final Item item = expected.get(entry.getName());
        if (item == null
            || entry.isDirectory() != item.directory()
            || entry.getSize() != item.size()) {
          throw new IOException("Состав ZIP не совпадает с манифестом: " + entry.getName());
        }
      }
      if (seen.size() != items.size() + 1) {
        throw new IOException("В пакете отсутствуют заявленные файлы.");
      }
      return result;
    } catch (final IllegalArgumentException exception) {
      throw new IOException("Повреждены значения манифеста проекта.", exception);
    }
  }

  private static void validate(final Snapshot snapshot) throws IOException {
    portablePath(snapshot.name());
    if (snapshot.name().contains("/")) {
      throw new IOException("Некорректное имя проекта.");
    }
    if (snapshot.items().size() > MAX_ENTRIES) {
      throw new IOException("В проекте слишком много файлов.");
    }
    final Map<String, Item> names = new HashMap<>();
    long total = 0;
    for (final Item item : snapshot.items()) {
      portablePath(item.path());
      if (item.size() < 0
          || item.size() > MAX_FILE
          || item.modified() < -11_644_473_600_000L
          || item.modified() > 253_402_300_799_999L
          || (item.directory()
              ? item.size() != 0 || !item.sha256().equals("-")
              : !item.sha256().matches("[0-9a-f]{64}"))) {
        throw new IOException("Некорректная запись файла: " + item.path());
      }
      final String key = pathKey(item.path());
      if (names.put(key, item) != null) {
        throw new IOException("Неоднозначные имена при переносе: " + item.path());
      }
      total += item.size();
      if (total > MAX_TOTAL) {
        throw new IOException("Пакет поддерживает до 10 ГиБ распакованных данных.");
      }
    }
    for (final Item item : snapshot.items()) {
      final int slash = item.path().lastIndexOf('/');
      if (slash >= 0) {
        final String parent = item.path().substring(0, slash);
        final Item directory = names.get(pathKey(parent));
        if (directory == null || !directory.directory() || !directory.path().equals(parent)) {
          throw new IOException("Отсутствует или неоднозначна родительская папка: " + parent);
        }
      }
    }
  }

  private static String pathKey(final String path) {
    return java.text.Normalizer.normalize(path, java.text.Normalizer.Form.NFC)
        .toLowerCase(Locale.ROOT);
  }

  private static void portablePath(final String path) throws IOException {
    if (path == null || path.isBlank() || path.length() > 1024 || path.split("/", -1).length > 64) {
      throw new IOException("Недопустимая длина пути проекта.");
    }
    for (final String part : path.split("/", -1)) {
      if (part.isEmpty()
          || part.equals(".")
          || part.equals("..")
          || part.endsWith(".")
          || part.endsWith(" ")
          || part.chars().anyMatch(c -> c < 32 || "\\:*?\"<>|".indexOf(c) >= 0)
          || part.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?")) {
        throw new IOException("Имя непереносимо между системами: " + path);
      }
    }
  }

  private static byte[] manifest(final Snapshot snapshot) throws IOException {
    final StringBuilder text =
        new StringBuilder("QCHEM-PROJECT\t1\t").append(encode(snapshot.name()));
    for (final Item item : snapshot.items()) {
      text.append('\n')
          .append(item.directory() ? "D" : "F")
          .append('\t')
          .append(encode(item.path()))
          .append('\t')
          .append(item.size())
          .append('\t')
          .append(item.sha256())
          .append('\t')
          .append(item.modified());
    }
    final byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_MANIFEST) {
      throw new IOException("Манифест проекта превышает 16 МиБ.");
    }
    return bytes;
  }

  private static String encode(final String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(final String value) throws IOException {
    return utf8(Base64.getUrlDecoder().decode(value));
  }

  private static String utf8(final byte[] bytes) throws IOException {
    return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
  }

  private static String zipName(final Item item) {
    return "data/" + item.path() + (item.directory() ? "/" : "");
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (final java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String hash(final Path path, final long expectedSize) throws IOException {
    try (final InputStream input = Files.newInputStream(path)) {
      return transfer(input, OutputStream.nullOutputStream(), expectedSize);
    }
  }

  private static String transfer(
      final InputStream input, final OutputStream output, final long expectedSize)
      throws IOException {
    final MessageDigest digest = digest();
    final byte[] buffer = new byte[64 * 1024];
    long count = 0;
    int read;
    while ((read = input.read(buffer)) != -1) {
      checkInterrupted();
      count += read;
      if (count > expectedSize) {
        throw new IOException("Размер файла изменился или превышает заявленный.");
      }
      output.write(buffer, 0, read);
      digest.update(buffer, 0, read);
    }
    if (count != expectedSize) {
      throw new IOException("Файл короче заявленного размера.");
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void copyVerified(
      final InputStream input, final OutputStream output, final Item item) throws IOException {
    if (!transfer(input, output, item.size()).equals(item.sha256())) {
      throw new IOException("Контрольная сумма не совпадает: " + item.path());
    }
  }

  private static void verifyContents(
      final ZipFile zip, final Snapshot snapshot, final Consumer<String> progress)
      throws IOException {
    for (final Item item : snapshot.items()) {
      checkInterrupted();
      if (item.directory()) {
        continue;
      }
      progress.accept("Проверяю пакет: " + item.path());
      try (final InputStream input = zip.getInputStream(zip.getEntry(zipName(item)))) {
        copyVerified(input, OutputStream.nullOutputStream(), item);
      }
    }
  }

  private static Path realFolder(final Path folder) throws IOException {
    final Path path = folder.toAbsolutePath().normalize();
    final BasicFileAttributes attributes =
        Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attributes.isDirectory()
        || attributes.isSymbolicLink()
        || attributes.isOther()
        || !path.toRealPath().equals(path)) {
      throw new IOException("Выберите обычную папку без символических ссылок: " + path);
    }
    return path;
  }

  private static void checkRegularPath(
      final Path root,
      final Path path,
      final BasicFileAttributes attributes,
      final boolean directory)
      throws IOException {
    if (attributes.isSymbolicLink()
        || attributes.isOther()
        || (directory ? !attributes.isDirectory() : !attributes.isRegularFile())
        || !path.toRealPath().equals(path)
        || !path.startsWith(root)) {
      throw new IOException("Ссылки и специальные файлы не упаковываются: " + path);
    }
  }

  private static boolean same(final BasicFileAttributes before, final BasicFileAttributes after) {
    return (before.isDirectory() || before.size() == after.size())
        && before.lastModifiedTime().equals(after.lastModifiedTime())
        && Objects.equals(before.fileKey(), after.fileKey())
        && before.isDirectory() == after.isDirectory()
        && before.isRegularFile() == after.isRegularFile();
  }

  private static void checkInterrupted() throws IOException {
    if (Thread.currentThread().isInterrupted()) {
      throw new IOException("Перенос отменён. Готовый проект или пакет не заменён.");
    }
  }

  private static void cleanup(final Path staging) throws IOException {
    if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    Files.walkFileTree(
        staging,
        new SimpleFileVisitor<>() {
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult postVisitDirectory(final Path directory, final IOException problem)
              throws IOException {
            if (problem != null) {
              throw problem;
            }
            Files.delete(directory);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
