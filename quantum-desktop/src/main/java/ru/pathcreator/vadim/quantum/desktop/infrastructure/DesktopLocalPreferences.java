/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Small local UI settings. Never stores laboratory data or changes a document. */
public final class DesktopLocalPreferences {
  private final Path path;
  private final Properties values = new Properties();
  private String warning = "";

  public DesktopLocalPreferences(final Path path) {
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
      this.warning = "Настройки сеанса не прочитаны: " + exception.getMessage();
    }
  }

  public String text(final String key, final String fallback) {
    return this.values.getProperty(key, fallback);
  }

  public void set(final String key, final Object value) {
    if (value == null) {
      this.values.remove(key);
    } else {
      this.values.setProperty(key, value.toString());
    }
  }

  public String warning() {
    return this.warning;
  }

  public void save() {
    Path temporary = null;
    try {
      Files.createDirectories(this.path.getParent());
      temporary = Files.createTempFile(this.path.getParent(), ".desktop-settings-", ".tmp");
      try (final var writer = Files.newBufferedWriter(temporary)) {
        this.values.store(writer, "Quantum local session");
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
      this.warning = "Настройки сеанса не сохранены: " + exception.getMessage();
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (final IOException ignored) {
          /* Best-effort cleanup. */
        }
      }
    }
  }
}
