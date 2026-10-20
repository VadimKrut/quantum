/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryReactionOperationsService;

/** Stage-scoped text projection shared by local method search and its complete preview. */
public final class LaboratoryReactionMethodText {
  private LaboratoryReactionMethodText() {}

  public static String method(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage) {
    return method(experiment, stage, () -> false);
  }

  public static String method(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final BooleanSupplier cancelled) {
    final var text = new StringBuilder();
    for (final var workup : experiment.operations().activeWorkups()) {
      check(cancelled);
      if (scope(workup, stage)) {
        workup(text, experiment, workup);
      }
    }
    for (final var run : experiment.operations().activePurifications()) {
      check(cancelled);
      if (scope(run, stage)) {
        purification(text, experiment, run);
      }
    }
    return text.toString();
  }

  public static String observations(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage) {
    return observations(experiment, stage, () -> false);
  }

  public static String observations(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final BooleanSupplier cancelled) {
    final var text = new StringBuilder();
    final var operations = experiment.operations();
    final Set<String> active = new HashSet<>();
    operations.activeTimePoints().forEach(r -> active.add(r.id()));
    operations.activeWorkups().forEach(r -> active.add(r.id()));
    operations.activePurifications().forEach(r -> active.add(r.id()));
    for (final var point : operations.timePoints()) {
      check(cancelled);
      if (!scope(point, stage)) {
        continue;
      }
      text.append(active.contains(point.id()) ? "Контрольная проба: " : "Прежняя версия пробы: ")
          .append(point.label())
          .append(" · ")
          .append(point.recordedAt())
          .append("\nВремя от начала: ")
          .append(point.elapsed())
          .append(' ')
          .append(point.elapsedUnit())
          .append(" · ")
          .append(point.method())
          .append("\nКонверсия: ")
          .append(
              point.conversion() == null
                  ? "не указана"
                  : point.conversion() + " " + point.conversionUnit())
          .append("\nИсточник: ")
          .append(point.source())
          .append("\nОбразец: ")
          .append(sample(experiment, point.sampleId()))
          .append("\n");
      observation(text, experiment, point.observation());
      text.append(point.note())
          .append(point.correctsId() == null ? "" : "\nИсправляет запись: " + point.correctsId())
          .append("\n\n");
    }
    for (final var workup : operations.workups()) {
      check(cancelled);
      if (scope(workup, stage) && !active.contains(workup.id())) {
        text.append("Прежняя версия обработки · ").append(workup.id()).append('\n');
        workup(text, experiment, workup);
      }
    }
    for (final var run : operations.purifications()) {
      check(cancelled);
      if (scope(run, stage) && !active.contains(run.id())) {
        text.append("Прежняя версия очистки · ").append(run.id()).append('\n');
        purification(text, experiment, run);
      }
    }
    final String currentOutcome =
        operations.outcomeFor(stage.id()).map(StageOutcome::id).orElse(null);
    for (final var outcome : operations.stageOutcomes()) {
      check(cancelled);
      if (!outcome.stageId().equals(stage.id())) {
        continue;
      }
      final boolean current = outcome.id().equals(currentOutcome);
      text.append(current ? "Исход стадии: " : "Прежняя версия исхода стадии: ")
          .append(outcome.status())
          .append(" · ")
          .append(outcome.recordedAt())
          .append("\nID записи: ")
          .append(outcome.id())
          .append(" · стадия: ")
          .append(outcome.stageTitle())
          .append(" [")
          .append(outcome.stageId())
          .append("]\n");
      outcomeProducts(text, "Цель", outcome.targets());
      outcomeProducts(text, "Побочный продукт", outcome.byproducts());
      observation(text, experiment, outcome.observation());
      text.append("Источник: ")
          .append(outcome.source())
          .append("\nОснование / причина: ")
          .append(outcome.reason());
      if (outcome.correctsId() != null) {
        text.append("\nИсправляет запись: ").append(outcome.correctsId());
      }
      text.append(
          "\nРучное заключение; количество, чистота и конверсия из него не рассчитываются.\n\n");
    }
    return text.toString();
  }

  private static void outcomeProducts(
      final StringBuilder text, final String role, final List<OutcomeProduct> products) {
    for (final var product : products) {
      text.append(role)
          .append(": ")
          .append(product.snapshot().displayName())
          .append(" · ")
          .append(product.snapshot().formula().hillNotation())
          .append(" [")
          .append(product.componentId())
          .append("] · сохранённый снимок ")
          .append(product.snapshot().id().value())
          .append('\n');
    }
  }

  private static boolean scope(final Revision record, final LaboratoryReactionStage stage) {
    return record.stageId() == null || record.stageId().equals(stage.id());
  }

  private static void workup(
      final StringBuilder text, final LaboratoryExperiment experiment, final Workup workup) {
    text.append("Обработка: ")
        .append(workup.title())
        .append(" · ")
        .append(workup.recordedAt())
        .append(workup.stageId() == null ? " · общая операция опыта" : "")
        .append('\n');
    final Map<String, String> phases = new HashMap<>();
    workup
        .phases()
        .forEach(
            p -> {
              phases.put(p.id(), p.label());
              text.append("Фаза: ").append(p.label()).append(" · ").append(p.kind()).append('\n');
            });
    for (final var step : workup.steps()) {
      text.append(step.kind())
          .append(" · ")
          .append(phases.getOrDefault(step.fromPhaseId(), "не задано"))
          .append(" → ")
          .append(phases.getOrDefault(step.toPhaseId(), "не задано"))
          .append(" · ")
          .append(step.material())
          .append(" · повторов: ")
          .append(step.repetitions())
          .append("\nСуммарно, план: ")
          .append(q(step.planned()))
          .append(" · записано: ")
          .append(q(step.actual()))
          .append("\nИсточник: ")
          .append(step.source())
          .append("\n");
      observation(text, experiment, step.observation());
      text.append(step.note()).append('\n');
    }
    losses(text, experiment, workup.losses());
    text.append(workup.note())
        .append(workup.correctsId() == null ? "" : "\nИсправляет запись: " + workup.correctsId())
        .append("\n\n");
  }

  private static void purification(
      final StringBuilder text, final LaboratoryExperiment experiment, final Purification run) {
    text.append("Очистка: ")
        .append(run.title())
        .append(" · ")
        .append(run.kind())
        .append(" · ")
        .append(run.recordedAt())
        .append(run.stageId() == null ? " · общая операция опыта" : "")
        .append("\nЗагрузка, план: ")
        .append(q(run.inputPlanned()))
        .append(" · записано: ")
        .append(q(run.inputActual()))
        .append("\nИсходный образец: ")
        .append(sample(experiment, run.inputSampleId()))
        .append("\nИсточник загрузки: ")
        .append(run.source())
        .append('\n');
    observation(text, experiment, run.observation());
    text.append("Неподвижная фаза: ")
        .append(run.stationaryPhase())
        .append(" · сорбент: ")
        .append(q(run.sorbentMass()))
        .append("\nЭлюент A: ")
        .append(run.solventA())
        .append(" · B: ")
        .append(run.solventB())
        .append("\nОсь градиента: ")
        .append(run.axis())
        .append('\n');
    for (final var g : run.gradient()) {
      text.append("Градиент: ")
          .append(g.position())
          .append(" · B ")
          .append(g.solventBPercent())
          .append(" % · поток ")
          .append(g.flowMlPerMinute() == null ? "не задан" : g.flowMlPerMinute() + " мл/мин")
          .append('\n');
    }
    final Map<String, String> fractions = new HashMap<>();
    for (final var f : run.fractions()) {
      fractions.put(f.id(), f.label());
      text.append("Фракция: ")
          .append(f.label())
          .append(" · ")
          .append(f.start() == null ? "интервал не задан" : f.start() + "–" + f.end())
          .append("\nПлан: ")
          .append(q(f.planned()))
          .append(" · записано: ")
          .append(q(f.actual()))
          .append(" · чистота: ")
          .append(f.purityPercent() == null ? "не указана" : f.purityPercent() + " %")
          .append("\nИсточник: ")
          .append(f.source())
          .append("\n");
      observation(text, experiment, f.observation());
      text.append(f.note()).append('\n');
    }
    for (final var pool : run.pools()) {
      text.append("Объединение: ")
          .append(pool.label())
          .append(" · ")
          .append(
              String.join(
                  ", ",
                  pool.fractionIds().stream().map(id -> fractions.getOrDefault(id, id)).toList()))
          .append("\nКоличество после объединения: ")
          .append(q(pool.actual()))
          .append("\nИсточник: ")
          .append(pool.source())
          .append("\n");
      observation(text, experiment, pool.observation());
      text.append(pool.note()).append('\n');
    }
    losses(text, experiment, run.losses());
    text.append(run.note())
        .append(run.correctsId() == null ? "" : "\nИсправляет запись: " + run.correctsId())
        .append("\n\n");
  }

  private static void losses(
      final StringBuilder text, final LaboratoryExperiment experiment, final List<Loss> losses) {
    for (final var loss : losses) {
      text.append("Потеря / поток: ")
          .append(loss.label())
          .append(" · план: ")
          .append(q(loss.planned()))
          .append(" · записано: ")
          .append(q(loss.actual()))
          .append("\nИсточник: ")
          .append(loss.source())
          .append('\n');
      observation(text, experiment, loss.observation());
    }
  }

  private static void observation(
      final StringBuilder text,
      final LaboratoryExperiment experiment,
      final Observation observation) {
    text.append("Происхождение значений: ")
        .append(
            observation.origin() == Origin.UNSPECIFIED
                ? "не указано; измерение не подтверждено"
                : LaboratoryReactionOperationsService.originLabel(observation.origin()))
        .append('\n');
    for (final var reference : observation.analyses()) {
      text.append("Связанный анализ [")
          .append(reference.kind())
          .append(':')
          .append(reference.id())
          .append("]: ");
      try {
        final var analysis =
            LaboratoryReactionOperationsService.resolveAnalysis(experiment, reference);
        text.append(analysis.title())
            .append(" · ")
            .append(analysis.method())
            .append(" · ")
            .append(analysis.recordedAt())
            .append(" · ")
            .append(analysis.origin())
            .append(analysis.historical() ? " · прежняя версия (сохранённая ссылка)" : "")
            .append("\nОбразец анализа: ")
            .append(sample(experiment, analysis.sampleId()))
            .append("\nИсточник анализа: ")
            .append(analysis.source())
            .append('\n');
      } catch (final IllegalArgumentException missing) {
        text.append("недоступен — ")
            .append(missing.getMessage())
            .append("; ссылка не заменена другой версией.\n");
      }
    }
    if (!observation.analyses().isEmpty()) {
      text.append("Аналитические ссылки сами по себе не рассчитывают конверсию или количество.\n");
    }
  }

  private static String sample(final LaboratoryExperiment experiment, final String id) {
    if (id == null) {
      return "без отдельного образца";
    }
    return experiment.samples().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .map(s -> s.code() + " [" + id + "]")
        .orElse(id);
  }

  private static String q(final LaboratoryQuantity quantity) {
    return quantity == null
        ? "не задано"
        : quantity.value().stripTrailingZeros().toPlainString() + " " + quantity.unit();
  }

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Поиск методики остановлен.");
    }
  }
}
