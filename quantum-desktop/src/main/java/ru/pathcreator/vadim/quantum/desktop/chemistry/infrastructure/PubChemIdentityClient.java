/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.CdkCompoundIdentifiers;

/** Explicit public-database request, never called on opening a document. Only verified graph matches can be applied. */
public final class PubChemIdentityClient {
  @FunctionalInterface
  public interface Transport {
    String post(String path, String body) throws Exception;
  }

  private static final Object RATE_LOCK = new Object();
  private static long nextRequest;
  private final Transport transport;

  public PubChemIdentityClient() {
    this(PubChemIdentityClient::request);
  }

  public PubChemIdentityClient(final Transport transport) {
    this.transport = Objects.requireNonNull(transport);
  }

  public MolecularIdentity lookup(
      final MoleculeDrawing drawing, final String query, final boolean structure) throws Exception {
    final var identifiers = new CdkCompoundIdentifiers();
    final var local = identifiers.describe(drawing);
    if (!local.identityLookupAllowed()) {
      throw new IllegalArgumentException(local.explanation());
    }
    final String value = structure ? local.smiles() : Objects.requireNonNullElse(query, "").strip();
    if (value.isBlank()
        || value.length() > 16000
        || value.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Введите название или CID PubChem.");
    }
    final String namespace =
        structure ? "smiles" : value.matches("[1-9][0-9]{0,18}") ? "cid" : "name";
    final JsonNode rows =
        json(this.transport.post(
                "compound/" + namespace + "/property/IUPACName,Title,SMILES,InChI,InChIKey/JSON",
                namespace + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)))
            .path("PropertyTable")
            .path("Properties");
    if (!rows.isArray() || rows.isEmpty()) {
      throw new IllegalArgumentException("PubChem не вернул карточку вещества.");
    }
    if (rows.size() > 20) {
      throw new IllegalArgumentException(
          "Название неоднозначно: найдено больше 20 записей. Уточните запрос или введите CID.");
    }
    final List<JsonNode> matches = new ArrayList<>();
    for (final JsonNode row : rows) {
      check();
      final String smiles = field(row, "SMILES", 16000);
      if (!smiles.isBlank() && identifiers.matches(drawing, smiles)) {
        matches.add(row);
      }
    }
    if (matches.isEmpty()) {
      throw new IllegalArgumentException(
          "Запись найдена, но граф, явные H, изотопы, заряд или заданная конфигурация не совпадают. Противоположная, добавленная или утраченная R/S, E/Z-конфигурация не принимается. Названия не применены.");
    }
    if (matches.size() != 1) {
      throw new IllegalArgumentException(
          "Несколько совпадающих записей: CID "
              + String.join(", ", matches.stream().map(n -> field(n, "CID", 20)).toList())
              + ". Укажите конкретный CID.");
    }
    final JsonNode row = matches.getFirst();
    final String cid = field(row, "CID", 20);
    final String iupac = field(row, "IUPACName", 8000);
    if (iupac.isBlank()) {
      throw new IllegalArgumentException(
          "В найденной карточке PubChem нет систематического названия IUPAC. Введённые сведения не изменены.");
    }
    if (!cid.matches("[1-9][0-9]{0,18}")) {
      throw new IllegalArgumentException("Некорректный CID в ответе PubChem.");
    }
    final List<String> synonyms = new ArrayList<>();
    final JsonNode info =
        json(this.transport.post("compound/cid/" + cid + "/synonyms/JSON", ""))
            .path("InformationList")
            .path("Information");
    if (!info.isArray()
        || info.size() != 1
        || !cid.equals(field(info.get(0), "CID", 20))
        || !info.get(0).path("Synonym").isArray()) {
      throw new IllegalArgumentException(
          "Синонимы PubChem не относятся к найденному CID; повторите запрос.");
    }
    for (final JsonNode s : info.get(0).path("Synonym")) {
      if (s.isTextual()
          && !s.asText().isBlank()
          && s.asText().length() <= 2000
          && !s.asText().contains("\n")) {
        synonyms.add(s.asText());
        if (synonyms.size() == 100) {
          break;
        }
      }
    }
    check();
    return new MolecularIdentity(
        field(row, "Title", 2000),
        iupac,
        synonyms,
        "",
        field(row, "SMILES", 16000),
        field(row, "InChI", 32000),
        field(row, "InChIKey", 32),
        cid,
        "https://pubchem.ncbi.nlm.nih.gov/compound/" + cid,
        MolecularIdentity.Origin.PUBCHEM,
        Instant.now(),
        MolecularIdentity.signature(drawing.molecule()));
  }

  private static JsonNode json(final String body) throws Exception {
    check();
    if (body == null || body.length() > 1_048_576) {
      throw new IllegalArgumentException("Ответ PubChem превышает допустимый размер.");
    }
    try {
      final var parsed =
          new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(body);
      if (parsed == null || !parsed.isObject()) {
        throw new IllegalArgumentException("PubChem вернул некорректный JSON.");
      }
      return parsed;
    } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException(
          "PubChem вернул неполный или некорректный JSON. Повторите запрос.", e);
    }
  }

  private static String field(final JsonNode row, final String name, final int limit) {
    final var value = row.path(name);
    if (value.isMissingNode() || value.isNull()) {
      return "";
    }
    if (!value.isTextual() && !value.isIntegralNumber()) {
      throw new IllegalArgumentException("Некорректное поле " + name + " в PubChem.");
    }
    final String text = value.asText();
    if (text.length() > limit) {
      throw new IllegalArgumentException("Поле " + name + " слишком длинное.");
    }
    return text;
  }

  private static void check() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Поиск остановлен.");
    }
  }

  private static String request(final String path, final String body) throws Exception {
    synchronized (RATE_LOCK) {
      final long delay = nextRequest - System.nanoTime();
      if (delay > 0) {
        java.util.concurrent.TimeUnit.NANOSECONDS.sleep(delay);
      }
      nextRequest = System.nanoTime() + 300_000_000L;
    }
    check();
    final HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    final var request =
        HttpRequest.newBuilder(URI.create("https://pubchem.ncbi.nlm.nih.gov/rest/pug/" + path))
            .timeout(Duration.ofSeconds(25))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    java.util.concurrent.CompletableFuture<HttpResponse<byte[]>> responseFuture = null;
    try {
      responseFuture = client.sendAsync(request, info -> new BoundedBody());
      final var response = responseFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);
      if (response.statusCode() == 404) {
        throw new IllegalArgumentException(
            "В PubChem нет подходящей записи. Название можно внести вручную с источником.");
      }
      if (response.statusCode() == 503 || response.statusCode() == 429) {
        throw new IllegalArgumentException("PubChem временно перегружен. Повторите запрос позже.");
      }
      if (response.statusCode() != 200) {
        throw new IllegalArgumentException(
            "PubChem вернул HTTP " + response.statusCode() + ". Карточка не изменена.");
      }
      check();
      return new String(response.body(), StandardCharsets.UTF_8);
    } catch (final java.util.concurrent.TimeoutException e) {
      throw new HttpTimeoutException("PubChem не ответил за 30 секунд.");
    } finally {
      if (responseFuture != null && !responseFuture.isDone()) {
        responseFuture.cancel(true);
      }
      client.shutdownNow();
    }
  }

  private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
    private final java.util.concurrent.CompletableFuture<byte[]> result =
        new java.util.concurrent.CompletableFuture<>();
    private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    private java.util.concurrent.Flow.Subscription subscription;

    @Override
    public java.util.concurrent.CompletionStage<byte[]> getBody() {
      return this.result;
    }

    @Override
    public void onSubscribe(final java.util.concurrent.Flow.Subscription value) {
      this.subscription = value;
      value.request(1);
    }

    @Override
    public void onNext(final List<java.nio.ByteBuffer> buffers) {
      for (final var buffer : buffers) {
        if (this.bytes.size() + (long) buffer.remaining() > 1_048_576) {
          this.subscription.cancel();
          this.result.completeExceptionally(
              new IllegalArgumentException("Ответ PubChem превышает 1 МиБ."));
          return;
        }
        final byte[] copy = new byte[buffer.remaining()];
        buffer.get(copy);
        this.bytes.writeBytes(copy);
      }
      this.subscription.request(1);
    }

    @Override
    public void onError(final Throwable e) {
      this.result.completeExceptionally(e);
    }

    @Override
    public void onComplete() {
      this.result.complete(this.bytes.toByteArray());
    }
  }
}
