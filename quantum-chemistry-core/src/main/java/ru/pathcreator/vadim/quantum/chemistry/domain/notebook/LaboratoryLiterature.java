/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Explicit bibliographic evidence. A reported literature yield is never an experimental fact. */
public record LaboratoryLiterature(List<Reference> references, List<Method> methods) {
  public static final LaboratoryLiterature EMPTY = new LaboratoryLiterature(List.of(), List.of());
  public static final int MAX_REFERENCES = 1000;
  public static final int MAX_METHODS = 2000;

  public record Reference(
      String id,
      String doi,
      String title,
      String authors,
      int year,
      String url,
      String supplementUrl,
      List<String> attachmentIds,
      String notes,
      String copyNote) {
    public Reference(
        final String id,
        final String doi,
        final String title,
        final String authors,
        final int year,
        final String url,
        final String supplementUrl,
        final List<String> attachmentIds,
        final String notes) {
      this(id, doi, title, authors, year, url, supplementUrl, attachmentIds, notes, "");
    }

    public Reference(
        final String id,
        final String doi,
        final String title,
        final String authors,
        final int year,
        final String url,
        final String supplementUrl,
        final List<String> attachmentIds) {
      this(id, doi, title, authors, year, url, supplementUrl, attachmentIds, "");
    }

    public Reference {
      id = identifier(id);
      doi = normalizeDoi(doi);
      title = text(title, 2000, true);
      authors = text(authors, 10000, false);
      if (year != 0 && (year < 1500 || year > 2200)) {
        throw new IllegalArgumentException("Год публикации: 1500–2200 или пустое поле.");
      }
      url = webUrl(url);
      supplementUrl = webUrl(supplementUrl);
      notes = text(notes, 20000, false);
      copyNote = text(copyNote, 24000, false);
      attachmentIds = List.copyOf(attachmentIds);
      if (attachmentIds.size() > 100
          || new HashSet<>(attachmentIds).size() != attachmentIds.size()) {
        throw new IllegalArgumentException(
            "Повтор или слишком много локальных приложений источника.");
      }
      attachmentIds.forEach(LaboratoryLiterature::identifier);
    }

    public String citation() {
      return title + (year == 0 ? "" : " (" + year + ")") + (doi.isEmpty() ? "" : "; DOI: " + doi);
    }

    @Override
    public String toString() {
      return this.citation();
    }
  }

  public record Method(
      String id,
      String referenceId,
      String stageId,
      String title,
      String procedureText,
      BigDecimal literatureYieldPercent,
      String yieldBasis,
      String notes,
      String copyNote) {
    public Method(
        final String id,
        final String referenceId,
        final String stageId,
        final String title,
        final String procedureText,
        final BigDecimal literatureYieldPercent,
        final String yieldBasis,
        final String notes) {
      this(
          id,
          referenceId,
          stageId,
          title,
          procedureText,
          literatureYieldPercent,
          yieldBasis,
          notes,
          "");
    }

    public Method {
      id = identifier(id);
      referenceId = identifier(referenceId);
      stageId = stageId == null ? null : identifier(stageId);
      title = text(title, 500, true);
      procedureText = text(procedureText, 100000, true);
      yieldBasis = text(yieldBasis, 2000, false);
      notes = text(notes, 20000, false);
      copyNote = text(copyNote, 1000, false);
      if (literatureYieldPercent != null) {
        if (literatureYieldPercent.precision() > 12
            || Math.abs((long) literatureYieldPercent.scale()) > 8
            || literatureYieldPercent.signum() < 0
            || literatureYieldPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
          throw new IllegalArgumentException("Литературный выход должен быть от 0 до 100%.");
        }
        if (yieldBasis.isBlank()) {
          throw new IllegalArgumentException(
              "Укажите основание литературного выхода: выделенный продукт, ЯМР, ВЭЖХ и т. п.");
        }
      }
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public LaboratoryLiterature {
    references = List.copyOf(references);
    methods = List.copyOf(methods);
    if (references.size() > MAX_REFERENCES || methods.size() > MAX_METHODS) {
      throw new IllegalArgumentException("Слишком много литературных записей в одном опыте.");
    }
    final var ids = new HashSet<String>();
    final var dois = new HashSet<String>();
    for (final var reference : references) {
      if (!ids.add(reference.id())) {
        throw new IllegalArgumentException("Повтор идентификатора источника.");
      }
      if (!reference.doi().isEmpty() && !dois.add(reference.doi())) {
        throw new IllegalArgumentException("Этот DOI уже добавлен в опыт.");
      }
    }
    final var methodIds = new HashSet<String>();
    for (final var method : methods) {
      if (!methodIds.add(method.id()) || !ids.contains(method.referenceId())) {
        throw new IllegalArgumentException(
            "Повтор методики или отсутствует её литературный источник.");
      }
    }
  }

  public boolean empty() {
    return references.isEmpty() && methods.isEmpty();
  }

  public void validateLinks(
      final List<LaboratoryReactionStage> stages, final List<LaboratoryAttachment> attachments) {
    final Set<String> stageIds = new HashSet<>(), attachmentIds = new HashSet<>();
    stages.forEach(s -> stageIds.add(s.id()));
    attachments.forEach(a -> attachmentIds.add(a.id()));
    for (final var method : methods) {
      if (method.stageId() != null && !stageIds.contains(method.stageId())) {
        throw new IllegalArgumentException("Методика ссылается на отсутствующую стадию.");
      }
    }
    for (final var reference : references) {
      if (!attachmentIds.containsAll(reference.attachmentIds())) {
        throw new IllegalArgumentException(
            "Литературный источник ссылается на отсутствующее вложение.");
      }
    }
  }

  public static String normalizeDoi(final String value) {
    String result = text(value, 1000, false).strip();
    result =
        result
            .replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/", "")
            .replaceFirst("(?i)^doi:\\s*", "")
            .toLowerCase(Locale.ROOT);
    if (!result.isEmpty() && !result.matches("10\\.[0-9]{4,9}/[^\\s\\p{Cntrl}]+")) {
      throw new IllegalArgumentException("DOI должен иметь вид 10.xxxx/идентификатор.");
    }
    return result;
  }

  private static String webUrl(final String value) {
    final String result = text(value, 4000, false).strip();
    if (result.isEmpty()) {
      return result;
    }
    final URI uri = URI.create(result);
    if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null) {
      throw new IllegalArgumentException(
          "Ссылка должна быть HTTP или HTTPS без имени пользователя.");
    }
    return result;
  }

  private static String identifier(final String value) {
    if (value == null || value.length() > 200) {
      throw new IllegalArgumentException("Некорректный идентификатор литературы.");
    }
    return IdentifierValue.requireIdentifier(value, "Literature id");
  }

  private static String text(final String value, final int bound, final boolean required) {
    final String result = Objects.requireNonNullElse(value, "").strip();
    if (result.length() > bound || result.indexOf('\0') >= 0 || required && result.isBlank()) {
      throw new IllegalArgumentException("Проверьте обязательное поле литературы и длину текста.");
    }
    return result;
  }
}
