/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryLiterature;

/** Explicit DOI metadata lookup; no article, SI, procedure or experimental result is inferred. */
public final class LaboratoryCrossrefClient {
  public LaboratoryLiterature.Reference lookup(final String suppliedDoi)
      throws IOException, InterruptedException {
    final String doi = LaboratoryLiterature.normalizeDoi(suppliedDoi);
    if (doi.isEmpty()) {
      throw new IllegalArgumentException("Введите DOI источника.");
    }
    final URI uri =
        URI.create(
            "https://api.crossref.org/works/"
                + URLEncoder.encode(doi, StandardCharsets.UTF_8).replace("+", "%20"));
    final HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(18))
            .header("Accept", "application/json")
            .header("User-Agent", "Quantum-Laboratory/1.0 (explicit DOI metadata lookup)")
            .GET()
            .build();
    try (final HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {
      final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (final var stream = response.body()) {
        if (response.statusCode() != 200) {
          throw new IOException(
              response.statusCode() == 404
                  ? "DOI не найден в Crossref. Добавьте источник вручную или проверьте DOI."
                  : "Crossref временно недоступен: HTTP " + response.statusCode());
        }
        final byte[] bytes = stream.readNBytes(2 * 1024 * 1024 + 1);
        if (bytes.length > 2 * 1024 * 1024) {
          throw new IOException("Ответ Crossref превышает 2 МиБ.");
        }
        final JsonNode json =
            new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readTree(bytes)
                .path("message");
        if (!doi.equals(LaboratoryLiterature.normalizeDoi(json.path("DOI").asText()))) {
          throw new IOException("Crossref вернул другой DOI; данные не применены.");
        }
        final String title = titleText(json.path("title").path(0).asText(""));
        final var authors = new ArrayList<String>();
        for (final var author : json.path("author")) {
          final String name =
              (author.path("given").asText("") + " " + author.path("family").asText("")).strip();
          if (!name.isBlank()) {
            authors.add(name);
          }
          if (authors.size() > 200) {
            throw new IOException("Слишком большой список авторов; добавьте метаданные вручную.");
          }
        }
        final int year = json.path("issued").path("date-parts").path(0).path(0).asInt(0);
        return new LaboratoryLiterature.Reference(
            "reference_" + UUID.randomUUID(),
            doi,
            title,
            String.join("; ", authors),
            year,
            "https://doi.org/" + doi,
            "",
            List.of(),
            "Библиографические метаданные Crossref; загружены "
                + LocalDate.now()
                + ". Методика и выход из этого запроса не получены.");
      }
    }
  }

  /** Crossref titles may contain JATS inline markup and indentation around italic ring locants. */
  static String titleText(final String supplied) {
    String text =
        supplied
            .replaceAll("(?is)<\\s*(?:br\\s*/?|/p|/div)\\s*>", " ")
            .replaceAll("<[^>]{1,1000}>", "");
    final var entities =
        Map.of("amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "nbsp", " ");
    final var matcher =
        java.util.regex.Pattern.compile(
                "&(#(?:[xX][0-9a-fA-F]{1,6}|[0-9]{1,7})|amp|lt|gt|quot|apos|nbsp);")
            .matcher(text);
    final StringBuilder decoded = new StringBuilder();
    while (matcher.find()) {
      final String entity = matcher.group(1);
      String replacement = entities.get(entity);
      if (replacement == null) {
        try {
          final boolean hex =
              entity.length() > 2 && (entity.charAt(1) == 'x' || entity.charAt(1) == 'X');
          final int code = Integer.parseInt(entity.substring(hex ? 2 : 1), hex ? 16 : 10);
          replacement =
              Character.isValidCodePoint(code)
                      && !(code >= 0xD800 && code <= 0xDFFF)
                      && !Character.isISOControl(code)
                  ? new String(Character.toChars(code))
                  : matcher.group();
        } catch (final IllegalArgumentException invalid) {
          replacement = matcher.group();
        }
      }
      matcher.appendReplacement(decoded, java.util.regex.Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(decoded);
    text = decoded.toString().replaceAll("[\\s\\p{Z}]+", " ").strip();
    // Only bracketed fusion locants are joined; spaces between ordinary title words are preserved.
    final var locants =
        java.util.regex.Pattern.compile(
                "\\[\\s*([0-9]+(?:\\s*,\\s*[0-9]+)*\\s*[-:]\\s*[a-zA-Z]+)\\s*\\]")
            .matcher(text);
    final StringBuilder normalized = new StringBuilder();
    while (locants.find()) {
      locants.appendReplacement(
          normalized,
          java.util.regex.Matcher.quoteReplacement(
              "[" + locants.group(1).replaceAll("\\s+", "") + "]"));
    }
    locants.appendTail(normalized);
    return normalized.toString();
  }
}
