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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryInventory;

/** One local ledger per workspace. Every operation is durable before it appears in the UI. */
public final class LaboratoryInventoryRepository {
  private final LaboratoryInventoryCodec codec = new LaboratoryInventoryCodec();

  public record Opened(LaboratoryInventory inventory, String fingerprint) {}

  /** The nearest ledger owns nested experiments; browsing another folder never changes ownership. */
  public static Path projectFolder(final Path document) throws IOException {
    if (document == null) {
      throw new IOException("Сначала сохраните опыт, чтобы определить его учёт материалов.");
    }
    final Path parent = document.toAbsolutePath().normalize().getParent().toRealPath();
    for (Path folder = parent; folder != null; folder = folder.getParent()) {
      final Path ledger = folder.resolve("materials.qchem");
      try {
        final var attributes =
            Files.readAttributes(
                ledger,
                java.nio.file.attribute.BasicFileAttributes.class,
                java.nio.file.LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) {
          throw new IOException("Учёт материалов должен быть обычным файлом: " + ledger);
        }
        return folder;
      } catch (final java.nio.file.NoSuchFileException missing) {
        // No ledger at this level. A damaged or inaccessible ledger must not create an empty
        // replacement.
      }
    }
    return parent;
  }

  public Opened read(final Path file) throws IOException {
    try {
      final var attributes =
          Files.readAttributes(
              file,
              java.nio.file.attribute.BasicFileAttributes.class,
              java.nio.file.LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isRegularFile()) {
        throw new IOException("Файл учёта должен быть обычным файлом: " + file);
      }
    } catch (final java.nio.file.NoSuchFileException missing) {
      return new Opened(LaboratoryInventory.empty(), null);
    }
    final byte[] bytes = Files.readAllBytes(file);
    try {
      return new Opened(
          this.codec.read(LaboratoryFileRepository.decode(bytes)),
          java.util.HexFormat.of()
              .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
    } catch (final java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public String write(final Path file, final LaboratoryInventory inventory, final String expected)
      throws IOException {
    final Path target = file.toAbsolutePath().normalize();
    Files.createDirectories(target.getParent());
    final String text = this.codec.write(inventory);
    final byte[] encoded = LaboratoryFileRepository.encode(text);
    if (!this.codec.read(LaboratoryFileRepository.decode(encoded)).equals(inventory)) {
      throw new IOException("Проверка сохранения учёта не пройдена.");
    }
    final Path lock = target.resolveSibling(target.getFileName() + ".lock");
    try (final FileChannel channel =
            FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        final var held = channel.tryLock()) {
      if (held == null) {
        throw new IOException("Файл учёта открыт для записи другой операцией.");
      }
      if (Files.exists(target)
          ? expected == null || !expected.equals(LaboratoryAssetStore.hash(target))
          : expected != null) {
        throw new IOException("Учёт изменён вне этого окна. Закройте и откройте материалы заново.");
      }
      if (Files.exists(target)) {
        final var previous = this.read(target).inventory();
        if (!inventory.materials().containsAll(previous.materials())
            || !inventory.batches().containsAll(previous.batches())
            || inventory.operations().size() < previous.operations().size()
            || !inventory
                .operations()
                .subList(0, previous.operations().size())
                .equals(previous.operations())
            || inventory.reservations().size() < previous.reservations().size()
            || !inventory
                .reservations()
                .subList(0, previous.reservations().size())
                .equals(previous.reservations())) {
          throw new IOException(
              "Проведённые операции и партии неизменяемы. Используйте возврат, корректировку или новую партию.");
        }
      }
      final Path temporary = Files.createTempFile(target.getParent(), ".inventory-", ".tmp");
      try {
        try (final FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
          final ByteBuffer bytes = ByteBuffer.wrap(encoded);
          while (bytes.hasRemaining()) {
            output.write(bytes);
          }
          output.force(true);
        }
        final String fingerprint = LaboratoryAssetStore.hash(temporary);
        if (Files.exists(target)) {
          final Path history = LaboratoryFileRepository.historyDirectory(target);
          Files.createDirectories(history);
          final Path snapshot = history.resolve(expected + ".qchem");
          if (!Files.exists(snapshot)) {
            final Path previous = Files.createTempFile(history, ".revision-", ".tmp");
            try {
              Files.copy(target, previous, StandardCopyOption.REPLACE_EXISTING);
              Files.move(previous, snapshot, StandardCopyOption.ATOMIC_MOVE);
            } finally {
              Files.deleteIfExists(previous);
            }
          }
        }
        Files.move(
            temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return fingerprint;
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (final java.nio.channels.OverlappingFileLockException exception) {
      throw new IOException(
          "Другое окно сохраняет учёт материалов. Повторите операцию после завершения записи.",
          exception);
    }
  }
}
