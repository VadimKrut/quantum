/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.Locale;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Reference context for a repeat. Original journal entries never become records of the new experiment. */
public final class LaboratoryRepeatContext {
  public static final int MAX_REFERENCE_LENGTH = 100000;

  private LaboratoryRepeatContext() {}

  /** The caller supplies the file that was actually read and its fingerprint; this helper performs no I/O. */
  public record Source(String path, String sha256) {
    public static final Source UNLOCATED = new Source("", "");

    public Source {
      path = Objects.requireNonNull(path, "Путь исходного опыта").strip();
      sha256 =
          Objects.requireNonNull(sha256, "Версия исходного опыта").strip().toLowerCase(Locale.ROOT);
      if (path.length() > 4096 || path.chars().anyMatch(Character::isISOControl)) {
        throw new IllegalArgumentException(
            "Путь исходного опыта должен быть одной строкой до 4096 символов.");
      }
      if (!sha256.isEmpty() && !sha256.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException("Некорректный SHA-256 исходного опыта.");
      }
    }
  }

  public static String notes(final LaboratoryDrawing source, final Source reference) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(reference);
    final var experiment = source.experiment();
    final var text = new Excerpt();
    text.append("ПОВТОР ОПЫТА — СПРАВОЧНЫЕ СВЕДЕНИЯ ОБ ИСТОЧНИКЕ\n");
    text.append(
        "Ниже приведены прежние сведения для планирования. Это не наблюдения и не результаты нового опыта.\n");
    text.append("ID исходного опыта: ").append(experiment.id());
    text.append("\nФайл источника: ")
        .append(
            reference.path().isEmpty()
                ? "не указан; повтор из документа в памяти"
                : reference.path());
    text.append("\nSHA-256 прочитанного файла: ")
        .append(reference.sha256().isEmpty() ? "не зафиксирован" : reference.sha256());
    text.append(
        "\nСсылка описывает исходную версию; при изменении файла сверяйте SHA-256. Исходный файл не изменялся.\n");
    text.append("Исходный опыт: ").append(experiment.title()).append("\n");
    if (!experiment.operations().stageOutcomes().isEmpty()) {
      text.append(
          "\nИСХОДЫ СТАДИЙ ИСХОДНОГО ОПЫТА (справочные записи; новый опыт ещё не оценён)\n");
      final var active =
          experiment.operations().activeStageOutcomes().stream()
              .map(value -> value.id())
              .collect(java.util.stream.Collectors.toSet());
      for (final var outcome : experiment.operations().stageOutcomes()) {
        if (text.full()) {
          break;
        }
        text.append(
                active.contains(outcome.id())
                    ? "\nПоследнее заключение: "
                    : "\nПрежняя версия заключения: ")
            .append(outcome.id());
        text.append("\nСтадия: ")
            .append(outcome.stageTitle())
            .append(" · ")
            .append(outcome.stageId())
            .append("\n");
        text.append(outcome.recordedAt().toString())
            .append(" · ")
            .append(outcome.status().toString())
            .append(" · ")
            .append(outcome.observation().origin().toString())
            .append("\n");
        if (outcome.correctsId() != null) {
          text.append("Исправляет исходное заключение: ").append(outcome.correctsId()).append("\n");
        }
        for (final var target : outcome.targets()) {
          if (text.full()) {
            break;
          }
          text.append("Целевой продукт: ")
              .append(target.snapshot().displayName())
              .append(" · ")
              .append(target.componentId())
              .append("\n");
        }
        for (final var byproduct : outcome.byproducts()) {
          if (text.full()) {
            break;
          }
          text.append("Побочный продукт: ")
              .append(byproduct.snapshot().displayName())
              .append(" · ")
              .append(byproduct.componentId())
              .append("\n");
        }
        text.append("Источник заключения:\n");
        text.quote(outcome.source());
        text.append("Основание:\n");
        text.quote(outcome.reason());
        for (final var analysis : outcome.observation().analyses()) {
          if (text.full()) {
            break;
          }
          text.append("Анализ в исходном опыте: ")
              .append(analysis.kind().toString())
              .append(" · ")
              .append(analysis.id())
              .append("\n");
        }
      }
    }
    if (!experiment.notes().isBlank()) {
      text.append("\nЗАМЕТКИ И РЕКОМЕНДАЦИИ ИСХОДНОГО ОПЫТА (цитата)\n");
      text.quote(experiment.notes());
    }
    if (!experiment.journal().isEmpty()) {
      text.append("\nЖУРНАЛ ИСХОДНОГО ОПЫТА (цитаты; не журнал выполнения повтора)\n");
      for (final var entry : experiment.journal()) {
        if (text.full()) {
          break;
        }
        text.append("\nИсходная запись: ")
            .append(entry.recordedAt().toString())
            .append(" · ")
            .append(entry.kind().toString())
            .append(" · ID ")
            .append(entry.id());
        if (entry.correctsId() != null) {
          text.append("\nИсправляет исходную запись: ").append(entry.correctsId());
        }
        text.append("\n");
        text.quote(entry.text());
      }
    }
    return text.finish();
  }

  private static final class Excerpt {
    private static final String CUT =
        "\n[Справочная выдержка сокращена до 100000 символов. Полные заметки и историю смотрите в исходном опыте по указанным ID/файлу.]";
    private final StringBuilder text = new StringBuilder();
    private boolean truncated;

    boolean full() {
      return this.truncated;
    }

    Excerpt append(final String value) {
      if (this.truncated) {
        return this;
      }
      final int remaining = MAX_REFERENCE_LENGTH - CUT.length() - this.text.length();
      if (value.length() > remaining) {
        int end = Math.max(0, remaining);
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
          end--;
        }
        this.text.append(value, 0, end);
        this.truncated = true;
      } else {
        this.text.append(value);
      }
      return this;
    }

    void quote(final String value) {
      int start = 0;
      while (start < value.length() && !this.truncated) {
        final int newline = value.indexOf('\n', start);
        final int end = newline < 0 ? value.length() : newline;
        this.append("> ");
        // Bound each slice before copying; hostile long notes never allocate an unbounded
        // duplicate.
        final int remaining = Math.max(0, MAX_REFERENCE_LENGTH - CUT.length() - this.text.length());
        if (end - start > remaining) {
          this.append(value.substring(start, Math.min(end, start + remaining + 1)));
        } else {
          this.append(value.substring(start, end)).append("\n");
        }
        start = newline < 0 ? value.length() : newline + 1;
      }
    }

    String finish() {
      if (this.truncated) {
        this.text.append(CUT);
      }
      return this.text.toString();
    }
  }
}
