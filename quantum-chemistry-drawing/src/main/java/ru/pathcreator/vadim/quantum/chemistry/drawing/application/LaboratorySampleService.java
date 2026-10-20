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
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample.*;

/** Pure commands prepare a complete ledger revision, including both sides of material transfers. */
public final class LaboratorySampleService {
  public record Card(String code, String label, String location, String container, String note) {}

  public List<LaboratorySample> register(
      final LaboratoryExperiment experiment,
      final String componentId,
      final String stageId,
      final Kind kind,
      final Card card,
      final LaboratoryQuantity quantity) {
    if (kind != Kind.CRUDE && kind != Kind.ISOLATED) {
      throw new IllegalArgumentException(
          "Для фракций, аликвот и объединения выберите соответствующую операцию.");
    }
    final var component =
        experiment.components().stream()
            .filter(c -> c.id().equals(componentId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выберите соединение."));
    final var stage =
        stageId == null
            ? null
            : experiment.stages().stream()
                .filter(s -> s.id().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Стадия отсутствует."));
    if (stage != null
        && stage.participants().stream().noneMatch(p -> p.componentId().equals(componentId))) {
      throw new IllegalArgumentException("Соединение не участвует в выбранной стадии.");
    }
    final String id = id("sample");
    final var molecule = LaboratoryAnalysisSummary.snapshot(component.molecule(), id);
    final Instant time = Instant.now();
    final var sample =
        new LaboratorySample(
            id,
            card.code(),
            componentId,
            stageId,
            stage == null ? "" : stage.title(),
            kind,
            molecule,
            List.of(),
            List.of(registerEvent(time, card, quantity)));
    final var next = new ArrayList<>(experiment.samples());
    next.add(sample);
    return checked(experiment, next);
  }

  public List<LaboratorySample> event(
      final LaboratoryExperiment experiment,
      final String id,
      final Action action,
      final Card card,
      final LaboratoryQuantity quantity) {
    if (action == Action.REGISTER) {
      throw new IllegalArgumentException("Используйте регистрацию образца.");
    }
    final var selected = sample(experiment, id);
    final var event =
        new Event(
            id("sample_event"),
            time(selected),
            action,
            quantity,
            action == Action.DETAILS ? card.label() : "",
            action == Action.DETAILS ? card.location() : "",
            action == Action.DETAILS ? card.container() : "",
            card.note(),
            null);
    return checked(experiment, replace(experiment.samples(), selected.append(event)));
  }

  public List<LaboratorySample> isolate(
      final LaboratoryExperiment experiment,
      final String parentId,
      final Card card,
      final LaboratoryQuantity quantity,
      final boolean processed) {
    final var parent = sample(experiment, parentId);
    if (parent.balance().status() != Status.AVAILABLE) {
      throw new IllegalArgumentException("Выделение регистрируется от образца в работе.");
    }
    final Instant time = time(parent);
    var next = new ArrayList<>(experiment.samples());
    if (processed) {
      next =
          replace(
              next,
              parent.append(
                  new Event(
                      id("sample_event"),
                      time,
                      Action.FINISH,
                      null,
                      "",
                      "",
                      "",
                      "Выделение образца «" + card.code() + "». " + card.note(),
                      null)));
    }
    next.add(
        new LaboratorySample(
            id("sample"),
            card.code(),
            parent.componentId(),
            parent.stageId(),
            parent.stageTitle(),
            Kind.ISOLATED,
            parent.molecule(),
            List.of(parentId),
            List.of(registerEvent(time, card, quantity))));
    return checked(experiment, next);
  }

  public List<LaboratorySample> aliquot(
      final LaboratoryExperiment experiment,
      final String parentId,
      final Card card,
      final LaboratoryQuantity quantity) {
    final var parent = sample(experiment, parentId);
    final String id = id("sample");
    final Instant time = time(parent);
    final var next =
        replace(
            experiment.samples(),
            parent.append(
                new Event(
                    id("sample_event"),
                    time,
                    Action.CONSUME,
                    quantity,
                    "",
                    "",
                    "",
                    "Аликвота «" + card.code() + "». " + card.note(),
                    id)));
    next.add(
        new LaboratorySample(
            id,
            card.code(),
            parent.componentId(),
            parent.stageId(),
            parent.stageTitle(),
            Kind.ALIQUOT,
            parent.molecule(),
            List.of(parent.id()),
            List.of(registerEvent(time, card, quantity))));
    return checked(experiment, next);
  }

  public List<LaboratorySample> pool(
      final LaboratoryExperiment experiment, final List<String> parentIds, final Card card) {
    if (parentIds.size() < 2
        || parentIds.size() > 100
        || Set.copyOf(parentIds).size() != parentIds.size()) {
      throw new IllegalArgumentException("Выберите от 2 до 100 разных образцов.");
    }
    final var parents = parentIds.stream().map(id -> sample(experiment, id)).toList();
    final var first = parents.getFirst();
    final var initial = first.balance().remaining();
    if (initial == null) {
      throw new IllegalArgumentException(
          "Для объединения у всех образцов должен быть известен остаток одной размерности.");
    }
    BigDecimal sum = BigDecimal.ZERO;
    Instant time = Instant.now();
    for (final var parent : parents) {
      if (time(parent).isAfter(time)) {
        time = time(parent);
      }
      final var q = parent.balance().remaining();
      if (q == null || q.value().signum() <= 0 || parent.balance().status() != Status.AVAILABLE) {
        throw new IllegalArgumentException(
            "Объединяются только образцы в работе с положительным известным остатком.");
      }
      sum = sum.add(q.in(initial.unit()).value());
    }
    final String id = id("sample");
    var next = new ArrayList<>(experiment.samples());
    for (final var parent : parents) {
      next =
          replace(
              next,
              parent.append(
                  new Event(
                      id("sample_event"),
                      time,
                      Action.CONSUME,
                      parent.balance().remaining(),
                      "",
                      "",
                      "",
                      "Объединение в «" + card.code() + "». " + card.note(),
                      id)));
    }
    next.add(
        new LaboratorySample(
            id,
            card.code(),
            first.componentId(),
            first.stageId(),
            first.stageTitle(),
            Kind.POOLED,
            first.molecule(),
            parentIds,
            List.of(registerEvent(time, card, new LaboratoryQuantity(sum, initial.unit())))));
    return checked(experiment, next);
  }

  public List<LaboratorySample> fractions(
      final LaboratoryExperiment experiment,
      final String parentId,
      final String prefix,
      final int start,
      final int count,
      final Card card,
      final LaboratoryQuantity each,
      final boolean processed) {
    if (start < 1 || count < 1 || count > 100 || start > 100000) {
      throw new IllegalArgumentException("Задайте 1–100 фракций и начальный номер от 1 до 100000.");
    }
    final var parent = sample(experiment, parentId);
    if (parent.balance().status() != Status.AVAILABLE) {
      throw new IllegalArgumentException("Фракции регистрируются от образца в работе.");
    }
    final Instant time = time(parent);
    var next = new ArrayList<>(experiment.samples());
    if (processed) {
      next =
          replace(
              next,
              parent.append(
                  new Event(
                      id("sample_event"),
                      time,
                      Action.FINISH,
                      null,
                      "",
                      "",
                      "",
                      "Разделение на фракции. " + card.note(),
                      null)));
    }
    for (int i = 0; i < count; i++) {
      final String code = prefix + String.format(Locale.ROOT, "%02d", start + i);
      final var c =
          new Card(
              code,
              card.label().isBlank() ? "Фракция " + (start + i) : card.label() + " " + (start + i),
              card.location(),
              code,
              card.note());
      next.add(
          new LaboratorySample(
              id("sample"),
              code,
              parent.componentId(),
              parent.stageId(),
              parent.stageTitle(),
              Kind.FRACTION,
              parent.molecule(),
              List.of(parentId),
              List.of(registerEvent(time, c, each))));
    }
    return checked(experiment, next);
  }

  public static LaboratorySample sample(final LaboratoryExperiment experiment, final String id) {
    return experiment.samples().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Образец отсутствует."));
  }

  private static Instant time(final LaboratorySample sample) {
    final Instant now = Instant.now(), last = sample.events().getLast().time();
    return now.isBefore(last) ? last : now;
  }

  private static Event registerEvent(
      final Instant time, final Card card, final LaboratoryQuantity quantity) {
    return new Event(
        id("sample_event"),
        time,
        Action.REGISTER,
        quantity,
        card.label(),
        card.location(),
        card.container(),
        card.note(),
        null);
  }

  private static String id(final String prefix) {
    return prefix + "_" + UUID.randomUUID();
  }

  private static ArrayList<LaboratorySample> replace(
      final List<LaboratorySample> all, final LaboratorySample updated) {
    final var next = new ArrayList<LaboratorySample>();
    for (final var sample : all) {
      next.add(sample.id().equals(updated.id()) ? updated : sample);
    }
    return next;
  }

  private static List<LaboratorySample> checked(
      final LaboratoryExperiment old, final List<LaboratorySample> next) {
    LaboratorySampleLedger.requireAppend(old.samples(), next);
    return List.copyOf(next);
  }
}
