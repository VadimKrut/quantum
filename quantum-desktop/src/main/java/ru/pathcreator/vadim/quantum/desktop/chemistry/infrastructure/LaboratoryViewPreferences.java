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
import java.util.Properties;

/** Local presentation choices only: never part of a molecule, experiment or its history. */
public final class LaboratoryViewPreferences {
  private final Path path;
  private final Properties values = new Properties();
  private String warning = "";

  public LaboratoryViewPreferences() {
    this(
        Path.of(
            System.getProperty(
                "quantum.laboratory.viewSettings",
                Path.of(System.getProperty("user.home"), ".quantum", "laboratory-view.properties")
                    .toString())));
  }

  public LaboratoryViewPreferences(final Path path) {
    this.path = path.toAbsolutePath().normalize();
    try {
      if (Files.exists(this.path)) {
        if (Files.size(this.path) > 65536) {
          throw new IOException("Файл настроек слишком большой");
        }
        try (final var reader = Files.newBufferedReader(this.path)) {
          this.values.load(reader);
        }
      }
    } catch (final IOException | IllegalArgumentException | SecurityException exception) {
      this.values.clear();
      this.warning = "Настройки вида не прочитаны: " + exception.getMessage();
    }
  }

  public boolean flag(final String key, final boolean fallback) {
    final String value = this.values.getProperty(key);
    return "true".equals(value) || !"false".equals(value) && fallback;
  }

  public String choice(final String key, final String fallback, final String... allowed) {
    final String value = this.values.getProperty(key);
    return java.util.Arrays.asList(allowed).contains(value) ? value : fallback;
  }

  public double fraction(final String key, final double fallback) {
    try {
      final double value = Double.parseDouble(this.values.getProperty(key, ""));
      return Double.isFinite(value) && value >= 0.2 && value <= 0.8 ? value : fallback;
    } catch (final NumberFormatException exception) {
      return fallback;
    }
  }

  public void set(final String key, final Object value) {
    this.values.setProperty(key, String.valueOf(value));
  }

  /** A presentation choice scoped to the exact local file, experiment and stage. */
  public String yieldReference(final Path file, final String experimentId, final String stageId) {
    return file == null || stageId == null
        ? null
        : this.values.getProperty(yieldKey(file, experimentId, stageId));
  }

  public void yieldReference(
      final Path file, final String experimentId, final String stageId, final String componentId) {
    if (file == null || stageId == null || componentId == null || componentId.length() > 512) {
      return;
    }
    final String key = yieldKey(file, experimentId, stageId);
    if (!this.values.containsKey(key)) {
      final var keys =
          this.values.stringPropertyNames().stream()
              .filter(k -> k.startsWith("yield.reference."))
              .sorted()
              .toList();
      if (keys.size() >= 64) {
        this.values.remove(keys.getFirst());
      }
    }
    this.values.setProperty(key, componentId);
  }

  private static String yieldKey(final Path file, final String experimentId, final String stageId) {
    String path = file.toAbsolutePath().normalize().toString();
    if (System.getProperty("os.name", "").startsWith("Windows")) {
      path = path.toLowerCase(java.util.Locale.ROOT);
    }
    final String scope =
        path.length()
            + ":"
            + path
            + experimentId.length()
            + ":"
            + experimentId
            + stageId.length()
            + ":"
            + stageId;
    try {
      return "yield.reference."
          + java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(scope.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  public String warning() {
    return this.warning;
  }

  public void save() {
    Path temporary = null;
    try {
      Files.createDirectories(this.path.getParent());
      temporary = Files.createTempFile(this.path.getParent(), ".laboratory-view-", ".tmp");
      try (final var writer = Files.newBufferedWriter(temporary)) {
        this.values.store(writer, "Quantum chemistry presentation");
      }
      try {
        Files.move(
            temporary,
            this.path,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (final java.nio.file.AtomicMoveNotSupportedException exception) {
        Files.move(temporary, this.path, StandardCopyOption.REPLACE_EXISTING);
      }
      this.warning = "";
    } catch (final IOException | SecurityException exception) {
      this.warning = "Настройки вида не сохранены: " + exception.getMessage();
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (final IOException ignored) {
          /* Best-effort temporary cleanup. */
        }
      }
    }
  }
}
