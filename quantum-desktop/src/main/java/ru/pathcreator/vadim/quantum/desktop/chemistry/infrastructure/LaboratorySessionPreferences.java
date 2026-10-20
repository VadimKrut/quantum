/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.desktop.infrastructure.DesktopLocalPreferences;

/** Paths are navigation hints only; documents are always re-read by the repository. */
public final class LaboratorySessionPreferences {
  private final DesktopLocalPreferences values =
      new DesktopLocalPreferences(
          Path.of(
              System.getProperty(
                  "quantum.laboratory.sessionSettings",
                  Path.of(
                          System.getProperty("user.home"),
                          ".quantum",
                          "laboratory-session.properties")
                      .toString())));
  private String invalidPath = "";

  public Path current() {
    return this.path("current");
  }

  public Path directory() {
    return this.path("directory");
  }

  public boolean resume() {
    return !"false".equals(this.values.text("resume", "false"));
  }

  public void resume(final boolean enabled) {
    this.values.set("resume", enabled);
    this.values.save();
  }

  public String component() {
    return this.values.text("component", "");
  }

  public void component(final String id) {
    this.values.set("component", id);
    this.values.save();
  }

  public String warning() {
    return this.values.warning().isBlank() ? this.invalidPath : this.values.warning();
  }

  public List<Path> recent() {
    final List<Path> result = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      final Path path = this.path("recent." + index);
      if (path != null && !result.contains(path)) {
        result.add(path);
      }
    }
    return List.copyOf(result);
  }

  public void opened(final Path path) {
    final Path normalized = path.toAbsolutePath().normalize();
    if (!normalized.equals(this.current())) {
      this.values.set("component", null);
    }
    final List<Path> recent = new ArrayList<>(this.recent());
    recent.remove(normalized);
    recent.addFirst(normalized);
    for (int index = 0; index < 10; index++) {
      this.values.set("recent." + index, index < recent.size() ? recent.get(index) : null);
    }
    this.values.set("current", normalized);
    this.values.set("directory", normalized.getParent());
    this.values.save();
  }

  public void newDocument() {
    this.values.set("current", null);
    this.values.set("component", null);
    this.values.save();
  }

  private Path path(final String key) {
    final String text = this.values.text(key, "");
    if (text.isBlank()) {
      return null;
    }
    try {
      final Path path = Path.of(text);
      if (!path.isAbsolute()) {
        throw new IllegalArgumentException("Ожидался полный путь");
      }
      return path.normalize();
    } catch (final IllegalArgumentException exception) {
      this.invalidPath = "В настройках сеанса пропущен некорректный путь";
      return null;
    }
  }
}
