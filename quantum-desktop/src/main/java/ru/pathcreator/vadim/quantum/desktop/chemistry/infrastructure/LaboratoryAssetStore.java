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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Immutable blobs may be written before the atomic document commit without corrupting the old document. */
public final class LaboratoryAssetStore {
  private final Path cache =
      Path.of(System.getProperty("java.io.tmpdir"), "quantum-laboratory-assets");

  public LaboratoryAttachment stage(final Path source) throws IOException {
    Files.createDirectories(this.cache);
    final Path temporary = Files.createTempFile(this.cache, "attachment-", ".tmp");
    try {
      copy(source, temporary);
      final String hash = hash(temporary);
      final Path blob = this.cache.resolve(hash);
      if (!Files.isRegularFile(blob) || !hash.equals(hash(blob))) {
        Files.move(
            temporary, blob, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
      return new LaboratoryAttachment(
          "attachment_" + UUID.randomUUID(),
          source.getFileName().toString(),
          hash,
          Files.size(blob));
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public LaboratoryAttachment stage(final byte[] bytes, final String name) throws IOException {
    if (bytes.length > 8_000_000) {
      throw new IOException("Исходный документ больше 8 МБ.");
    }
    Files.createDirectories(this.cache);
    final Path temporary = Files.createTempFile(this.cache, "source-", ".tmp");
    try {
      Files.write(temporary, bytes);
      final String hash = hash(temporary);
      final Path blob = this.cache.resolve(hash);
      if (!Files.isRegularFile(blob) || !hash.equals(hash(blob))) {
        Files.move(
            temporary, blob, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
      return new LaboratoryAttachment("attachment_" + UUID.randomUUID(), name, hash, bytes.length);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public void transfer(
      final LaboratoryDrawing document, final Path sourceDocument, final Path targetDocument)
      throws IOException {
    if (document.experiment().attachments().isEmpty()) {
      return;
    }
    final Path directory = directory(targetDocument);
    Files.createDirectories(directory);
    for (final LaboratoryAttachment attachment : document.experiment().attachments()) {
      final Path destination = directory.resolve(attachment.sha256());
      if (Files.exists(destination)) {
        verify(destination, attachment);
        continue;
      }
      final Path source = this.locate(sourceDocument, attachment);
      final Path temporary = Files.createTempFile(directory, ".copy-", ".tmp");
      try {
        copy(source, temporary);
        verify(temporary, attachment);
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporary);
      }
    }
  }

  public Path locate(final Path document, final LaboratoryAttachment attachment)
      throws IOException {
    final Path local = document == null ? null : directory(document).resolve(attachment.sha256());
    final Path path =
        local != null && Files.isRegularFile(local)
            ? local
            : this.cache.resolve(attachment.sha256());
    verify(path, attachment);
    return path;
  }

  public static Path directory(final Path document) {
    final Path absolute = document.toAbsolutePath().normalize();
    return absolute.resolveSibling(absolute.getFileName() + ".assets");
  }

  public static void verify(final Path path, final LaboratoryAttachment attachment)
      throws IOException {
    if (!Files.isRegularFile(path)
        || Files.size(path) != attachment.size()
        || !hash(path).equals(attachment.sha256())) {
      throw new IOException("Вложение отсутствует или повреждено: " + attachment.name());
    }
  }

  public static void extract(
      final Path document, final LaboratoryAttachment attachment, final Path target)
      throws IOException {
    final Path destination = target.toAbsolutePath().normalize();
    final Path source = new LaboratoryAssetStore().locate(document, attachment);
    if (Files.exists(destination) && Files.isSameFile(source, destination)) {
      return;
    }
    final Path temporary = Files.createTempFile(destination.getParent(), ".attachment-", ".tmp");
    try {
      copy(source, temporary);
      verify(temporary, attachment);
      interrupted();
      Files.move(
          temporary,
          destination,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void interrupted() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Операция с вложением отменена.");
    }
  }

  public static void copy(final Path source, final Path target) throws IOException {
    interrupted();
    try (final var input = Files.newInputStream(source);
        final var output = Files.newOutputStream(target)) {
      final byte[] buffer = new byte[65536];
      int count;
      while ((count = input.read(buffer)) >= 0) {
        interrupted();
        output.write(buffer, 0, count);
      }
    }
  }

  public static String hash(final Path path) throws IOException {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (final InputStream input = Files.newInputStream(path)) {
        final byte[] buffer = new byte[65536];
        int count;
        while ((count = input.read(buffer)) >= 0) {
          interrupted();
          digest.update(buffer, 0, count);
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (final NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
