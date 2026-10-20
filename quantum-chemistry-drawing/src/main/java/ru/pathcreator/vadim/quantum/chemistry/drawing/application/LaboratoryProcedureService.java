/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryProcedure;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** Resolves planned procedure amounts from notebook quantities and freezes performed operations. */
public final class LaboratoryProcedureService {

  private LaboratoryProcedureService() {}

  public record Resolved(
      LaboratoryProcedure.Step step,
      String stageTitle,
      String componentName,
      LaboratoryQuantity plannedQuantity,
      String structureSignature,
      String narrative,
      List<String> warnings) {
    public Resolved {
      warnings = List.copyOf(warnings);
    }
  }

  /** Started/skipped operations retain their recorded quantities; unstarted operations follow the live plan. */
  public static Resolved resolve(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Step step) {
    final var procedure = experiment.procedure();
    final var start = procedure.latestStart(step.id());
    final var fixed =
        start != null
            ? start
            : procedure.eventsFor(step.id()).stream()
                .filter(
                    e ->
                        e.action() == LaboratoryProcedure.Action.SKIP
                            || e.action() == LaboratoryProcedure.Action.RECORD_COMPLETE)
                .findFirst()
                .orElse(null);
    if (fixed != null) {
      return frozen(experiment, fixed);
    }
    final var live = live(experiment, step);
    if (!live.warnings().isEmpty()
        && step.componentId() != null
        && component(experiment, step.componentId()) == null) {
      final var first = procedure.eventsFor(step.id()).stream().findFirst().orElse(null);
      if (first != null) {
        return frozen(experiment, first);
      }
    }
    return live;
  }

  public static LaboratoryQuantity resolveQuantity(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Step step) {
    return resolve(experiment, step).plannedQuantity();
  }

  public static String describe(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Step step) {
    return resolve(experiment, step).narrative();
  }

  private static Resolved live(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Step step) {
    final List<String> warnings = new ArrayList<>();
    final String stage = stageTitle(experiment, step);
    if (step.stageId() != null
        && experiment.stages().stream().noneMatch(s -> s.id().equals(step.stageId()))) {
      warnings.add("Связанная стадия отсутствует в текущем плане.");
    }
    final var component = component(experiment, step.componentId());
    final String name = component == null ? "" : component.molecule().displayName();
    final String signature =
        component == null ? "" : LaboratoryMaterialProperties.signature(component.molecule());
    final LaboratoryQuantity quantity = scaledQuantity(component, step);
    if (step.componentId() != null && component == null) {
      warnings.add("Связанный компонент отсутствует в текущем плане.");
    } else if (component != null && quantity == null) {
      warnings.add("Плановое количество компонента не задано.");
    }
    if (step.stageId() != null
        && component != null
        && experiment.stages().stream()
            .filter(s -> s.id().equals(step.stageId()))
            .anyMatch(
                s ->
                    s.participants().stream()
                        .noneMatch(p -> p.componentId().equals(component.id())))) {
      warnings.add("Компонент больше не входит в связанную стадию.");
    }
    return resolved(step, stage, name, quantity, signature, warnings);
  }

  private static LaboratoryQuantity scaledQuantity(
      final LaboratoryComponent component, final LaboratoryProcedure.Step step) {
    if (component == null || component.planned() == null) {
      return null;
    }
    requireBoundedQuantity(component.planned());
    final var result =
        new LaboratoryQuantity(
            component.planned().value().multiply(step.portion()), component.planned().unit());
    requireBoundedQuantity(result);
    return result;
  }

  private static void requireBoundedQuantity(final LaboratoryQuantity quantity) {
    if (quantity.value().precision() > 100 || Math.abs((long) quantity.value().scale()) > 100) {
      throw new IllegalArgumentException(
          "Плановое количество содержит слишком много знаков. Укажите значение с точностью и порядком не более 100 знаков.");
    }
  }

  private static Resolved frozen(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Event event) {
    final List<String> warnings = new ArrayList<>();
    Resolved current = null;
    try {
      current = live(experiment, event.snapshot());
    } catch (final IllegalArgumentException exception) {
      warnings.add("Текущее плановое количество недоступно: " + exception.getMessage());
    }
    if (current == null
        || !Objects.equals(current.plannedQuantity(), event.plannedQuantity())
        || !current.structureSignature().equals(event.structureSignature())
        || !current.componentName().equals(event.componentName())) {
      warnings.add(
          "Текущий план изменён. Показаны состав и количество, сохранённые при выполнении операции.");
    }
    if (event.snapshot().componentId() != null && event.plannedQuantity() == null) {
      warnings.add("При выполнении операции плановое количество не было задано.");
    }
    return resolved(
        event.snapshot(),
        stageTitle(experiment, event.snapshot()),
        event.componentName(),
        event.plannedQuantity(),
        event.structureSignature(),
        warnings);
  }

  private static Resolved resolved(
      final LaboratoryProcedure.Step step,
      final String stage,
      final String component,
      final LaboratoryQuantity quantity,
      final String signature,
      final List<String> warnings) {
    final StringBuilder text = new StringBuilder(step.title());
    if (!component.isBlank()) {
      text.append(" · ")
          .append(component)
          .append(": ")
          .append(quantity == null ? "количество не задано" : quantity);
      if (step.portion().compareTo(BigDecimal.ONE) != 0) {
        text.append(" (")
            .append(
                step.portion()
                    .multiply(BigDecimal.valueOf(100))
                    .stripTrailingZeros()
                    .toPlainString())
            .append("% загрузки)");
      }
    }
    if (step.temperatureC() != null) {
      text.append(" · ")
          .append(step.temperatureC().stripTrailingZeros().toPlainString())
          .append(" °C");
    }
    if (step.durationSeconds() > 0) {
      text.append(" · ").append(duration(step.durationSeconds()));
    }
    if (!step.instruction().isBlank()) {
      text.append("\n").append(step.instruction());
    }
    return new Resolved(step, stage, component, quantity, signature, text.toString(), warnings);
  }

  public static String duration(final long seconds) {
    if (seconds < 0) {
      throw new IllegalArgumentException("Длительность не может быть отрицательной.");
    }
    final long hours = seconds / 3600, minutes = seconds % 3600 / 60, rest = seconds % 60;
    final List<String> parts = new ArrayList<>();
    if (hours > 0) {
      parts.add(hours + " ч");
    }
    if (minutes > 0) {
      parts.add(minutes + " мин");
    }
    if (rest > 0 || parts.isEmpty()) {
      parts.add(rest + " с");
    }
    return String.join(" ", parts);
  }

  /** A persisted start and duration define the deadline after reopening; passing it never completes a step. */
  public static Instant deadline(final LaboratoryProcedure procedure, final String stepId) {
    if (procedure.status(stepId) != LaboratoryProcedure.Status.RUNNING) {
      return null;
    }
    final var start = procedure.latestStart(stepId);
    if (start == null || start.snapshot().durationSeconds() == 0) {
      return null;
    }
    try {
      return start.recordedAt().plusSeconds(start.snapshot().durationSeconds());
    } catch (final java.time.DateTimeException | ArithmeticException exception) {
      throw new IllegalArgumentException(
          "Время начала выходит за допустимый диапазон таймера.", exception);
    }
  }

  public static LaboratoryProcedure updatePlan(
      final LaboratoryExperiment experiment, final List<LaboratoryProcedure.Step> steps) {
    final var next = new LaboratoryProcedure(steps, experiment.procedure().events());
    for (final var previous : experiment.procedure().steps()) {
      if (!experiment.procedure().eventsFor(previous.id()).isEmpty()
          && next.steps().stream().noneMatch(s -> s.equals(previous))) {
        throw new IllegalArgumentException(
            "Операция «"
                + previous.title()
                + "» уже имеет записи. Сохраните её и создайте новую операцию для изменения методики.");
      }
    }
    for (final var step : next.steps()) {
      if (next.eventsFor(step.id()).isEmpty()) {
        requireLiveLinks(experiment, step);
      }
    }
    return next;
  }

  public static LaboratoryProcedure.Event event(
      final LaboratoryExperiment experiment,
      final String stepId,
      final LaboratoryProcedure.Action action,
      final String text,
      final LaboratoryQuantity actualQuantity,
      final String correctsId,
      final Instant at) {
    Objects.requireNonNull(action, "Procedure action");
    Objects.requireNonNull(at, "Procedure event time");
    final var procedure = experiment.procedure();
    final var step =
        procedure.steps().stream()
            .filter(s -> s.id().equals(stepId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Операция отсутствует в текущем плане."));
    if (action == LaboratoryProcedure.Action.CORRECT) {
      final var target =
          procedure.events().stream()
              .filter(e -> e.id().equals(correctsId) && e.stepId().equals(stepId))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Выберите существующую запись этой операции для исправления."));
      return checked(
          procedure,
          new LaboratoryProcedure.Event(
              "procedure_event_" + UUID.randomUUID(),
              stepId,
              at,
              action,
              text,
              target.snapshot(),
              target.componentName(),
              target.plannedQuantity(),
              actualQuantity,
              target.structureSignature(),
              correctsId));
    }
    if (correctsId != null) {
      throw new IllegalArgumentException(
          "Ссылка на исправление разрешена только действию «Исправить запись».");
    }
    final var start = procedure.latestStart(stepId);
    final var fixed =
        start != null
            ? start
            : procedure.eventsFor(stepId).stream()
                .filter(
                    e ->
                        e.action() == LaboratoryProcedure.Action.SKIP
                            || e.action() == LaboratoryProcedure.Action.RECORD_COMPLETE)
                .findFirst()
                .orElse(null);
    if (fixed == null) {
      requireLiveLinks(experiment, step);
    }
    final var value = fixed == null ? live(experiment, step) : frozen(experiment, fixed);
    final var result =
        new LaboratoryProcedure.Event(
            "procedure_event_" + UUID.randomUUID(),
            stepId,
            at,
            action,
            text,
            value.step(),
            value.componentName(),
            value.plannedQuantity(),
            actualQuantity,
            value.structureSignature(),
            null);
    return checked(procedure, result);
  }

  private static LaboratoryProcedure.Event checked(
      final LaboratoryProcedure procedure, final LaboratoryProcedure.Event event) {
    final var events = new ArrayList<>(procedure.events());
    events.add(event);
    final var next = new LaboratoryProcedure(procedure.steps(), events);
    if (event.action() == LaboratoryProcedure.Action.START) {
      deadline(next, event.stepId());
    }
    return event;
  }

  private static void requireLiveLinks(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Step step) {
    if (step.stageId() != null
        && experiment.stages().stream().noneMatch(s -> s.id().equals(step.stageId()))) {
      throw new IllegalArgumentException("Связанная стадия отсутствует. Исправьте план операции.");
    }
    if (step.componentId() != null && component(experiment, step.componentId()) == null) {
      throw new IllegalArgumentException(
          "Связанный компонент отсутствует. Исправьте план операции.");
    }
    if (step.stageId() != null
        && step.componentId() != null
        && experiment.stages().stream()
            .filter(s -> s.id().equals(step.stageId()))
            .anyMatch(
                s ->
                    s.participants().stream()
                        .noneMatch(p -> p.componentId().equals(step.componentId())))) {
      throw new IllegalArgumentException("Выбранный компонент не входит в связанную стадию.");
    }
  }

  private static LaboratoryComponent component(
      final LaboratoryExperiment experiment, final String id) {
    return id == null
        ? null
        : experiment.components().stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
  }

  private static String stageTitle(
      final LaboratoryExperiment experiment, final LaboratoryProcedure.Step step) {
    return step.stageId() == null
        ? "Общая операция"
        : experiment.stages().stream()
            .filter(s -> s.id().equals(step.stageId()))
            .map(s -> s.title())
            .findFirst()
            .orElse("Стадия из истории опыта");
  }

  /** Deliberately supplies no temperature, duration or chemical instructions: the chemist supplies the method. */
  public static List<LaboratoryProcedure.Step> stageStarter(
      final LaboratoryExperiment experiment, final String stageId) {
    final var stage =
        experiment.stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Выберите стадию для подготовки плана операций."));
    final List<LaboratoryProcedure.Step> steps = new ArrayList<>();
    for (final var participant : stage.participants()) {
      if (participant.role() == ReactionComponentRole.PRODUCT) {
        continue;
      }
      steps.add(
          new LaboratoryProcedure.Step(
              "procedure_step_" + UUID.randomUUID(),
              stageId,
              LaboratoryProcedure.Kind.ADD,
              "Добавление",
              "",
              participant.componentId(),
              BigDecimal.ONE,
              0,
              null));
    }
    return List.copyOf(steps);
  }

  public record Repeated(LaboratoryProcedure procedure, int omittedSteps, String warning) {}

  /** Retain only links valid in the new plan, including actual stage membership, never execution evidence. */
  public static Repeated repeatPlan(
      final LaboratoryProcedure source,
      final LaboratoryExperiment target,
      final boolean includeUnboundGeneral) {
    final List<LaboratoryProcedure.Step> retained = new ArrayList<>();
    for (final var step : source.steps()) {
      if (!includeUnboundGeneral && step.stageId() == null && step.componentId() == null) {
        continue;
      }
      if (step.componentId() != null && component(target, step.componentId()) == null) {
        continue;
      }
      if (step.stageId() != null) {
        final var stage =
            target.stages().stream()
                .filter(s -> s.id().equals(step.stageId()))
                .findFirst()
                .orElse(null);
        if (stage == null
            || step.componentId() != null
                && stage.participants().stream()
                    .noneMatch(p -> p.componentId().equals(step.componentId()))) {
          continue;
        }
      }
      retained.add(independentStep(step));
    }
    final int omitted = source.steps().size() - retained.size();
    final String warning =
        omitted == 0
            ? ""
            : "В новый план не перенесено операций: "
                + omitted
                + ". Проверьте операции, связанные с отсутствующими компонентами, стадиями или изменённым составом стадии."
                + (includeUnboundGeneral
                    ? ""
                    : " Общие операции без связи со стадией или её компонентами также не перенесены.");
    return new Repeated(new LaboratoryProcedure(retained, List.of()), omitted, warning);
  }

  public static LaboratoryProcedure.Step independentStep(final LaboratoryProcedure.Step step) {
    return new LaboratoryProcedure.Step(
        "procedure_step_" + UUID.randomUUID(),
        step.stageId(),
        step.kind(),
        step.title(),
        step.instruction(),
        step.componentId(),
        step.portion(),
        step.durationSeconds(),
        step.temperatureC());
  }

  /** Plan undo/revision may change future work, but cannot erase or rewrite performed work. */
  public static LaboratoryProcedure keepRecords(
      final LaboratoryProcedure target, final LaboratoryProcedure current) {
    final var events = new LinkedHashMap<String, LaboratoryProcedure.Event>();
    for (final var event : target.events()) {
      events.put(event.id(), event);
    }
    for (final var event : current.events()) {
      final var existing = events.putIfAbsent(event.id(), event);
      if (existing != null && !existing.equals(event)) {
        throw new IllegalArgumentException(
            "Версия содержит изменённую запись выполнения. История операции не может быть переписана.");
      }
    }
    final var ordered =
        events.size() == current.events().size()
            ? current.events()
            : events.values().stream()
                .sorted(Comparator.comparing(LaboratoryProcedure.Event::recordedAt))
                .toList();
    final var locked = new LinkedHashMap<String, LaboratoryProcedure.Step>();
    for (final var event : ordered) {
      final var previous = locked.putIfAbsent(event.stepId(), event.snapshot());
      if (previous != null && !previous.equals(event.snapshot())) {
        throw new IllegalArgumentException(
            "История содержит разные планы одной выполненной операции.");
      }
    }
    final var steps = new LinkedHashMap<String, LaboratoryProcedure.Step>();
    for (final var step : target.steps()) {
      steps.put(step.id(), locked.getOrDefault(step.id(), step));
    }
    for (final var step : current.steps()) {
      if (locked.containsKey(step.id())) {
        steps.putIfAbsent(step.id(), locked.get(step.id()));
      }
    }
    locked.forEach(steps::putIfAbsent);
    return new LaboratoryProcedure(List.copyOf(steps.values()), ordered);
  }
}
