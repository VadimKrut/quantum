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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Portable document plus attachments. Entry paths are selected by the manifest, never extracted blindly. */
public final class LaboratoryArchive {
  // A draft may contain a 64 MiB document embedded in design and dispensing snapshots.
  public static final int MAX_RECOVERY_BYTES = 4 * LaboratoryFileCodec.MAX_DOCUMENT_BYTES;
  private final LaboratoryFileCodec codec = new LaboratoryFileCodec();
  private final LaboratoryAssetStore assets = new LaboratoryAssetStore();

  public void export(final Path archive, final Path source, final LaboratoryDrawing drawing)
      throws IOException {
    this.export(archive, source, drawing, new java.util.Properties());
  }

  public void export(
      final Path archive,
      final Path source,
      final LaboratoryDrawing drawing,
      final java.util.Properties recovery)
      throws IOException {
    final Path target = archive.toAbsolutePath().normalize();
    if (source != null
        && (target.equals(source.toAbsolutePath().normalize())
            || Files.exists(target) && Files.exists(source) && Files.isSameFile(target, source))) {
      throw new IOException("Выберите отдельный файл пакета, чтобы сохранить исходный QCHEM.");
    }
    Files.createDirectories(target.getParent());
    final byte[] manifest = LaboratoryFileRepository.encode(this.codec.write(drawing));
    if (manifest.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
      throw new IOException("Документ в пакете превышает поддерживаемые 64 МиБ.");
    }
    if (!this.codec.read(LaboratoryFileRepository.decode(manifest)).equals(drawing)) {
      throw new IOException("Проверка документа перед созданием пакета не пройдена.");
    }
    // Count bounded serialized Properties before opening a file; avoid a second 256 MiB metadata
    // buffer.
    if (!recovery.isEmpty()) {
      recovery.store(
          new BoundedOutput(java.io.OutputStream.nullOutputStream(), MAX_RECOVERY_BYTES),
          "Quantum laboratory recovery v1");
    }
    final Path temporary = Files.createTempFile(target.getParent(), ".archive-", ".tmp");
    try {
      try (final ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
        zip.putNextEntry(new ZipEntry("experiment.qchem"));
        zip.write(manifest);
        zip.closeEntry();
        if (!recovery.isEmpty()) {
          zip.putNextEntry(new ZipEntry("recovery.properties"));
          recovery.store(
              new BoundedOutput(zip, MAX_RECOVERY_BYTES), "Quantum laboratory recovery v1");
          zip.closeEntry();
        }
        final java.util.HashSet<String> included = new java.util.HashSet<>();
        for (final LaboratoryAttachment attachment : drawing.experiment().attachments()) {
          if (!included.add(attachment.sha256())) {
            continue;
          }
          zip.putNextEntry(new ZipEntry("assets/" + attachment.sha256()));
          Files.copy(this.assets.locate(source, attachment), zip);
          zip.closeEntry();
        }
      }
      try (final var channel =
          java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) {
        channel.force(true);
      }
      Files.move(
          temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static final class BoundedOutput extends java.io.FilterOutputStream {
    private final long maximum;
    private long count;

    BoundedOutput(final java.io.OutputStream output, final long maximum) {
      super(output);
      this.maximum = maximum;
    }

    private void add(final int n) throws IOException {
      if (n < 0 || count + n > maximum) {
        throw new IOException("Текст авточерновика превышает 256 МиБ.");
      }
      count += n;
    }

    @Override
    public void write(final int value) throws IOException {
      add(1);
      out.write(value);
    }

    @Override
    public void write(final byte[] value, final int offset, final int length) throws IOException {
      java.util.Objects.checkFromIndexSize(offset, length, value.length);
      add(length);
      out.write(value, offset, length);
    }

    @Override
    public void close() throws IOException {
      flush();
    }
  }

  public LaboratoryDrawing importTo(final Path archive, final Path target) throws IOException {
    if (Files.exists(target)) {
      throw new IOException("Для импорта выберите новое имя файла.");
    }
    try (final ZipFile zip = new ZipFile(archive.toFile())) {
      final ZipEntry manifest = zip.getEntry("experiment.qchem");
      if (manifest == null
          || manifest.getSize() < 0
          || manifest.getSize() > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
        throw new IOException("Пакет не содержит допустимый документ QCHEM.");
      }
      final LaboratoryDrawing drawing;
      try (final var input = zip.getInputStream(manifest)) {
        final byte[] bytes = input.readNBytes(LaboratoryFileCodec.MAX_DOCUMENT_BYTES + 1);
        if (bytes.length != manifest.getSize()
            || bytes.length > LaboratoryFileCodec.MAX_DOCUMENT_BYTES) {
          throw new IOException("Размер документа в пакете не соответствует описанию.");
        }
        drawing = this.codec.read(LaboratoryFileRepository.decode(bytes));
      }
      final Path directory = LaboratoryAssetStore.directory(target);
      if (!drawing.experiment().attachments().isEmpty()) {
        Files.createDirectories(directory);
      }
      for (final LaboratoryAttachment attachment : drawing.experiment().attachments()) {
        final ZipEntry entry = zip.getEntry("assets/" + attachment.sha256());
        if (entry == null || entry.getSize() != attachment.size()) {
          throw new IOException("В пакете отсутствует вложение: " + attachment.name());
        }
        final Path destination = directory.resolve(attachment.sha256());
        if (Files.exists(destination)) {
          LaboratoryAssetStore.verify(destination, attachment);
          continue;
        }
        final Path temporary = Files.createTempFile(directory, ".import-", ".tmp");
        try {
          try (final var input = zip.getInputStream(entry);
              final var output = Files.newOutputStream(temporary)) {
            final byte[] buffer = new byte[65536];
            long count = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
              count += read;
              if (count > attachment.size()) {
                throw new IOException("Размер вложения превышает заявленный.");
              }
              output.write(buffer, 0, read);
            }
          }
          LaboratoryAssetStore.verify(temporary, attachment);
          Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
          Files.deleteIfExists(temporary);
        }
      }
      new LaboratoryFileRepository().write(target, drawing, null);
      return drawing;
    } catch (final java.util.zip.ZipException exception) {
      throw new IOException(
          "Пакет повреждён или имеет неподдерживаемый формат. Выберите целый файл .qchempack.",
          exception);
    }
  }
}
