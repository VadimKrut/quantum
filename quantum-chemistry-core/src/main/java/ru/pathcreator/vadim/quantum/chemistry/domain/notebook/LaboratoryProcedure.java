/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Editable method and append-only execution evidence. Plan values are never measurements. */
public record LaboratoryProcedure(List<Step> steps, List<Event> events) {

  public static final int MAX_STEPS = 2000;
  public static final int MAX_EVENTS = 100000;
  public static final long MAX_DURATION_SECONDS = 31536000;
  public static final LaboratoryProcedure EMPTY = new LaboratoryProcedure(List.of(), List.of());

  public enum Kind {
    ADD("Добавление"),
    STIR("Перемешивание"),
    HEAT("Нагревание"),
    COOL("Охлаждение"),
    WAIT("Выдержка"),
    SAMPLE("Отбор пробы"),
    WORKUP("Обработка"),
    PURIFY("Очистка"),
    OTHER("Действие");
    private final String label;

    Kind(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum Action {
    START("Начато"),
    COMPLETE("Выполнено"),
    RECORD_COMPLETE("Выполнено ранее · время не указано"),
    SKIP("Пропущено"),
    OBSERVE("Наблюдение"),
    CORRECT("Исправление");
    private final String label;

    Action(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum Status {
    PLANNED("Запланировано"),
    RUNNING("Выполняется"),
    COMPLETED("Выполнено"),
    SKIPPED("Пропущено");
    private final String label;

    Status(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public record Step(
      String id,
      String stageId,
      Kind kind,
      String title,
      String instruction,
      String componentId,
      BigDecimal portion,
      long durationSeconds,
      BigDecimal temperatureC) {
    public Step {
      id = identifier(id, "Идентификатор действия");
      stageId = optionalIdentifier(stageId, "Стадия действия");
      Objects.requireNonNull(kind, "Вид действия");
      title =
          boundedText(TextValue.requireText(title, "Название действия"), 500, "Название действия");
      instruction = boundedText(instruction, 100000, "Инструкция действия");
      componentId = optionalIdentifier(componentId, "Компонент действия");
      Objects.requireNonNull(portion, "Доля загрузки");
      if (portion.signum() <= 0
          || portion.compareTo(BigDecimal.ONE) > 0
          || portion.precision() > 30
          || Math.abs((long) portion.scale()) > 30) {
        throw new IllegalArgumentException(
            "Доля загрузки должна быть больше нуля и не больше единицы (до 30 знаков).");
      }
      portion = portion.stripTrailingZeros();
      if (durationSeconds < 0 || durationSeconds > MAX_DURATION_SECONDS) {
        throw new IllegalArgumentException("Длительность действия должна быть от 0 до 365 дней.");
      }
      if (temperatureC != null) {
        if (temperatureC.compareTo(new BigDecimal("-273.15")) < 0
            || temperatureC.compareTo(new BigDecimal("10000")) > 0
            || temperatureC.precision() > 30
            || Math.abs((long) temperatureC.scale()) > 30) {
          throw new IllegalArgumentException(
              "Температура действия должна быть от −273,15 до 10000 °C (до 30 знаков).");
        }
        temperatureC = temperatureC.stripTrailingZeros();
      }
    }
  }

  public record Event(
      String id,
      String stepId,
      Instant recordedAt,
      Action action,
      String text,
      Step snapshot,
      String componentName,
      LaboratoryQuantity plannedQuantity,
      LaboratoryQuantity actualQuantity,
      String structureSignature,
      String correctsId) {
    public Event {
      id = identifier(id, "Идентификатор выполнения");
      stepId = identifier(stepId, "Действие выполнения");
      Objects.requireNonNull(recordedAt, "Время выполнения");
      Objects.requireNonNull(action, "Запись выполнения");
      text = boundedText(text, 100000, "Текст выполнения");
      Objects.requireNonNull(snapshot, "Снимок действия");
      if (!stepId.equals(snapshot.id())) {
        throw new IllegalArgumentException("Снимок относится к другому действию.");
      }
      componentName = boundedText(componentName, 10000, "Название компонента в записи");
      structureSignature = boundedText(structureSignature, 100000, "Снимок структуры в записи");
      correctsId = optionalIdentifier(correctsId, "Исправляемая запись выполнения");
      if ((action == Action.CORRECT) != (correctsId != null)) {
        throw new IllegalArgumentException(
            "Исправление должно ссылаться на исходную запись выполнения.");
      }
      if ((action == Action.CORRECT || action == Action.OBSERVE) && text.isBlank()) {
        throw new IllegalArgumentException("Введите текст наблюдения или причину исправления.");
      }
      if (snapshot.componentId() == null
          && (!componentName.isEmpty()
              || !structureSignature.isEmpty()
              || plannedQuantity != null
              || actualQuantity != null)) {
        throw new IllegalArgumentException(
            "Количество и снимок вещества требуют связанного компонента действия.");
      }
      if (snapshot.componentId() != null
          && (componentName.isBlank() || structureSignature.isBlank())) {
        throw new IllegalArgumentException(
            "Связанное вещество требует сохранённого названия и структуры.");
      }
      if (action == Action.SKIP && actualQuantity != null) {
        throw new IllegalArgumentException(
            "Пропущенное действие не содержит фактического количества.");
      }
      boundedQuantity(plannedQuantity);
      boundedQuantity(actualQuantity);
    }
  }

  public LaboratoryProcedure {
    Objects.requireNonNull(steps, "Действия методики");
    Objects.requireNonNull(events, "Записи выполнения");
    if (steps.size() > MAX_STEPS || events.size() > MAX_EVENTS) {
      throw new IllegalArgumentException(
          "Поддерживается до 2000 действий и 100000 записей выполнения в одном опыте.");
    }
    steps = List.copyOf(steps);
    events = List.copyOf(events);
    final var stepIds = new HashSet<String>();
    final var plan = new HashMap<String, Step>();
    long textSize = 0;
    for (final var step : steps) {
      if (!stepIds.add(step.id())) {
        throw new IllegalArgumentException("Повтор идентификатора действия методики.");
      }
      plan.put(step.id(), step);
      textSize += step.title().length() + step.instruction().length();
    }
    final var known = new HashMap<String, Event>();
    final var snapshots = new HashMap<String, Step>();
    final var states = new HashMap<String, Status>();
    final var contexts = new HashMap<String, Event>();
    final var times = new HashMap<String, Instant>();
    for (final var event : events) {
      if (known.containsKey(event.id())) {
        throw new IllegalArgumentException("Повтор идентификатора записи выполнения.");
      }
      final var snapshot = snapshots.putIfAbsent(event.stepId(), event.snapshot());
      if (snapshot != null && !snapshot.equals(event.snapshot())) {
        throw new IllegalArgumentException(
            "Все записи одного действия должны сохранять один и тот же снимок методики.");
      }
      if (plan.containsKey(event.stepId()) && !plan.get(event.stepId()).equals(event.snapshot())) {
        throw new IllegalArgumentException(
            "Действие с записями выполнения нельзя изменять задним числом.");
      }
      final var priorTime = times.put(event.stepId(), event.recordedAt());
      if (priorTime != null && event.recordedAt().isBefore(priorTime)) {
        throw new IllegalArgumentException(
            "Время записи выполнения не может предшествовать предыдущей записи этого действия.");
      }
      final var state = states.getOrDefault(event.stepId(), Status.PLANNED);
      switch (event.action()) {
        case START -> {
          if (state != Status.PLANNED) {
            throw new IllegalArgumentException("Действие уже начато или завершено.");
          }
          states.put(event.stepId(), Status.RUNNING);
          contexts.put(event.stepId(), event);
        }
        case COMPLETE -> {
          if (state != Status.RUNNING) {
            throw new IllegalArgumentException("Перед завершением начните действие.");
          }
          sameContext(contexts.get(event.stepId()), event);
          states.put(event.stepId(), Status.COMPLETED);
        }
        case RECORD_COMPLETE -> {
          if (state != Status.PLANNED) {
            throw new IllegalArgumentException(
                "Отметить выполненным можно только ещё не начатый шаг.");
          }
          // A retrospective acknowledgement records entry time, never a fabricated start or
          // duration.
          states.put(event.stepId(), Status.COMPLETED);
          contexts.put(event.stepId(), event);
        }
        case SKIP -> {
          if (state == Status.COMPLETED || state == Status.SKIPPED) {
            throw new IllegalArgumentException("Действие уже завершено или пропущено.");
          }
          if (contexts.containsKey(event.stepId())) {
            sameContext(contexts.get(event.stepId()), event);
          }
          states.put(event.stepId(), Status.SKIPPED);
        }
        case OBSERVE -> {
          if (contexts.containsKey(event.stepId())) {
            sameContext(contexts.get(event.stepId()), event);
          }
        }
        case CORRECT -> {
          final var original = known.get(event.correctsId());
          if (original == null || !original.stepId().equals(event.stepId())) {
            throw new IllegalArgumentException(
                "Исправление должно ссылаться на более раннюю запись того же действия.");
          }
          sameContext(original, event);
        }
      }
      known.put(event.id(), event);
      textSize +=
          event.text().length()
              + event.componentName().length()
              + event.structureSignature().length()
              + event.snapshot().title().length()
              + event.snapshot().instruction().length();
      if (textSize > 10000000) {
        throw new IllegalArgumentException(
            "Методика и записи выполнения превышают 10 миллионов символов.");
      }
    }
    if (textSize > 10000000) {
      throw new IllegalArgumentException("Методика превышает 10 миллионов символов.");
    }
  }

  public Status status(final String stepId) {
    Status result = Status.PLANNED;
    for (final var event : this.events) {
      if (event.stepId().equals(stepId)) {
        result =
            switch (event.action()) {
              case START -> Status.RUNNING;
              case COMPLETE, RECORD_COMPLETE -> Status.COMPLETED;
              case SKIP -> Status.SKIPPED;
              case OBSERVE, CORRECT -> result;
            };
      }
    }
    return result;
  }

  public Event latestStart(final String stepId) {
    for (int i = this.events.size() - 1; i >= 0; --i) {
      final var event = this.events.get(i);
      if (event.stepId().equals(stepId) && event.action() == Action.START) {
        return event;
      }
    }
    return null;
  }

  public List<Event> eventsFor(final String stepId) {
    final var result = new ArrayList<Event>();
    for (final var event : this.events) {
      if (event.stepId().equals(stepId)) {
        result.add(event);
      }
    }
    return List.copyOf(result);
  }

  private static void sameContext(final Event original, final Event next) {
    if (!original.snapshot().equals(next.snapshot())
        || !original.componentName().equals(next.componentName())
        || !Objects.equals(original.plannedQuantity(), next.plannedQuantity())
        || !original.structureSignature().equals(next.structureSignature())) {
      throw new IllegalArgumentException(
          "Снимок выполняемого действия и его исходной загрузки нельзя изменять задним числом.");
    }
  }

  private static String identifier(final String value, final String label) {
    return boundedText(IdentifierValue.requireIdentifier(value, label), 200, label);
  }

  private static String optionalIdentifier(final String value, final String label) {
    return value == null ? null : identifier(value, label);
  }

  private static String boundedText(final String value, final int limit, final String label) {
    Objects.requireNonNull(value, label);
    if (value.length() > limit) {
      throw new IllegalArgumentException(label + ": превышена допустимая длина " + limit + ".");
    }
    return value;
  }

  private static void boundedQuantity(final LaboratoryQuantity quantity) {
    if (quantity != null
        && (quantity.value().precision() > 100
            || Math.abs((long) quantity.value().scale()) > 100)) {
      throw new IllegalArgumentException(
          "Количество в записи выполнения содержит слишком много знаков.");
    }
  }
}
