/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryLiterature;
import ru.pathcreator.vadim.quantum.desktop.infrastructure.DesktopLocalPreferences;

/** User-edited navigation addresses only; no availability checks or document data. */
public final class LaboratoryArticleLinks {
  private static final String DEFAULTS = "https://sci-hub.ru\nhttps://sci-hub.st";
  private final DesktopLocalPreferences preferences;
  private List<String> mirrors;
  private String warning = "";

  public LaboratoryArticleLinks() {
    this(
        Path.of(
            System.getProperty(
                "quantum.laboratory.articleSettings",
                Path.of(System.getProperty("user.home"), ".quantum", "article-links.properties")
                    .toString())));
  }

  public LaboratoryArticleLinks(final Path path) {
    this.preferences = new DesktopLocalPreferences(path);
    try {
      this.mirrors = parse(this.preferences.text("mirrors", DEFAULTS));
    } catch (final IllegalArgumentException exception) {
      this.mirrors = parse(DEFAULTS);
      this.warning = "Сохранённые адреса не прочитаны. Проверьте список в подробностях источника.";
    }
  }

  public List<String> mirrors() {
    return this.mirrors;
  }

  public String warning() {
    return this.preferences.warning().isBlank() ? this.warning : this.preferences.warning();
  }

  public void saveMirrors(final String text) {
    final var next = parse(text);
    final String previous = this.preferences.text("mirrors", DEFAULTS);
    this.preferences.set("mirrors", String.join("\n", next));
    this.preferences.save();
    if (!this.preferences.warning().isBlank()) {
      this.preferences.set("mirrors", previous);
      throw new IllegalStateException(this.preferences.warning());
    }
    this.mirrors = next;
    this.warning = "";
  }

  public static URI article(final String mirror, final String rawDoi) {
    final String base = base(mirror), doi = LaboratoryLiterature.normalizeDoi(rawDoi);
    if (doi.isBlank()) {
      throw new IllegalArgumentException("Введите DOI публикации, затем выберите адрес Sci-Hub.");
    }
    return URI.create(
        base + "/" + URLEncoder.encode(doi, StandardCharsets.UTF_8).replace("+", "%20"));
  }

  private static List<String> parse(final String text) {
    if (text == null || text.isBlank() || text.length() > 4096) {
      throw new IllegalArgumentException("Введите от 1 до 8 адресов HTTPS, по одному в строке.");
    }
    final var values =
        text.lines()
            .map(String::strip)
            .filter(value -> !value.isEmpty())
            .map(LaboratoryArticleLinks::base)
            .distinct()
            .toList();
    if (values.isEmpty() || values.size() > 8) {
      throw new IllegalArgumentException("Введите от 1 до 8 адресов HTTPS, по одному в строке.");
    }
    return values;
  }

  private static String base(final String value) {
    final URI uri;
    try {
      uri = URI.create(value == null ? "" : value.strip());
    } catch (final IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Проверьте адрес сайта: требуется HTTPS и имя сервера без пути.");
    }
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || uri.getRawUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || uri.getPort() > 65535
        || !(uri.getRawPath().isEmpty() || uri.getRawPath().equals("/"))) {
      throw new IllegalArgumentException(
          "Укажите адрес HTTPS с именем сервера, без логина, пути и параметров.");
    }
    final String result = uri.toASCIIString();
    return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
  }
}
