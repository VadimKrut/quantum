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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Explicit directed synthesis plan. Expected yields and transfer recoveries are assumptions, never measurements. */
public record LaboratorySynthesisRoute(
    List<Node> nodes, List<Link> links, String targetStageId, LaboratoryQuantity targetQuantity) {

  public static final int MAX_NODES = 500;
  public static final int MAX_LINKS = 2000;
  public static final LaboratorySynthesisRoute EMPTY =
      new LaboratorySynthesisRoute(List.of(), List.of(), null, null);

  public record Node(
      String stageId,
      String referenceComponentId,
      String productComponentId,
      BigDecimal expectedYieldPercent,
      boolean enabled) {
    public Node {
      stageId = identifier(stageId);
      referenceComponentId = identifier(referenceComponentId);
      productComponentId = identifier(productComponentId);
      if (referenceComponentId.equals(productComponentId)) {
        throw new IllegalArgumentException(
            "Опорный исходник и продукт маршрута должны быть разными строками компонентов.");
      }
      expectedYieldPercent = positive(expectedYieldPercent, "Ожидаемый выход");
      if (expectedYieldPercent.compareTo(new BigDecimal("100")) > 0) {
        throw new IllegalArgumentException(
            "Ожидаемый выход должен быть больше 0 и не больше 100 %.");
      }
    }
  }

  /** transferFraction is the recovered fraction, not allocation of a shared batch between branches. */
  public record Link(
      String id,
      String sourceStageId,
      String targetStageId,
      String targetComponentId,
      BigDecimal transferFraction,
      String sourceSignature,
      String targetSignature,
      boolean enabled) {
    public Link {
      id = identifier(id);
      sourceStageId = identifier(sourceStageId);
      targetStageId = identifier(targetStageId);
      targetComponentId = identifier(targetComponentId);
      if (sourceStageId.equals(targetStageId)) {
        throw new IllegalArgumentException(
            "Стадия не может получать промежуточный продукт сама от себя.");
      }
      transferFraction = positive(transferFraction, "Сохранение при передаче");
      if (transferFraction.compareTo(BigDecimal.ONE) > 0) {
        throw new IllegalArgumentException(
            "До следующей стадии может доходить больше 0 и не больше 100 % вещества.");
      }
      sourceSignature = signature(sourceSignature);
      targetSignature = signature(targetSignature);
    }
  }

  public LaboratorySynthesisRoute {
    Objects.requireNonNull(nodes, "Стадии маршрута");
    Objects.requireNonNull(links, "Связи маршрута");
    if (nodes.size() > MAX_NODES || links.size() > MAX_LINKS) {
      throw new IllegalArgumentException("В маршруте поддерживается до 500 стадий и 2000 связей.");
    }
    nodes = List.copyOf(nodes);
    links = List.copyOf(links);
    final var stageIds = new HashSet<String>();
    final var linkIds = new HashSet<String>();
    for (final var node : nodes) {
      if (!stageIds.add(node.stageId())) {
        throw new IllegalArgumentException("Одна стадия указана в маршруте дважды.");
      }
    }
    for (final var link : links) {
      if (!linkIds.add(link.id())) {
        throw new IllegalArgumentException("Повтор идентификатора связи маршрута.");
      }
      if (!stageIds.contains(link.sourceStageId()) || !stageIds.contains(link.targetStageId())) {
        throw new IllegalArgumentException("Связь ссылается на стадию, не включённую в маршрут.");
      }
    }
    if (targetStageId != null) {
      targetStageId = identifier(targetStageId);
      if (!stageIds.contains(targetStageId)) {
        throw new IllegalArgumentException("Целевая стадия не включена в маршрут.");
      }
    }
    if (targetQuantity != null) {
      if (targetStageId == null) {
        throw new IllegalArgumentException(
            "Для количества конечного продукта выберите целевую стадию.");
      }
      positive(targetQuantity.value(), "Количество конечного продукта");
      if (targetQuantity.unit().dimension().equals("volume")) {
        throw new IllegalArgumentException(
            "Цель маршрута задаётся массой или количеством вещества.");
      }
    }
    topological(nodes, links);
  }

  public boolean empty() {
    return this.nodes.isEmpty()
        && this.links.isEmpty()
        && this.targetStageId == null
        && this.targetQuantity == null;
  }

  public List<Link> activeLinks() {
    return active(this.nodes, this.links);
  }

  public List<String> topologicalStageIds() {
    return topological(this.nodes, this.links);
  }

  private static List<Link> active(final List<Node> nodes, final List<Link> links) {
    final var enabled = new HashSet<String>();
    for (final var node : nodes) {
      if (node.enabled()) {
        enabled.add(node.stageId());
      }
    }
    return links.stream()
        .filter(
            link ->
                link.enabled()
                    && enabled.contains(link.sourceStageId())
                    && enabled.contains(link.targetStageId()))
        .toList();
  }

  private static List<String> topological(final List<Node> nodes, final List<Link> links) {
    final var indegrees = new LinkedHashMap<String, Integer>();
    final var outgoing = new LinkedHashMap<String, List<String>>();
    for (final var node : nodes) {
      if (node.enabled()) {
        indegrees.put(node.stageId(), 0);
        outgoing.put(node.stageId(), new ArrayList<>());
      }
    }
    final var producers = new HashSet<String>();
    for (final var link : active(nodes, links)) {
      if (!producers.add(link.targetStageId() + "\n" + link.targetComponentId())) {
        throw new IllegalArgumentException(
            "Для одного исходника стадии включено несколько источников. Оставьте один вариант связи.");
      }
      indegrees.compute(link.targetStageId(), (key, value) -> value + 1);
      outgoing.get(link.sourceStageId()).add(link.targetStageId());
    }
    final var queue = new ArrayDeque<String>();
    indegrees.forEach(
        (id, count) -> {
          if (count == 0) {
            queue.add(id);
          }
        });
    final var result = new ArrayList<String>();
    while (!queue.isEmpty()) {
      final String id = queue.removeFirst();
      result.add(id);
      for (final String target : outgoing.get(id)) {
        final int count = indegrees.compute(target, (key, value) -> value - 1);
        if (count == 0) {
          queue.addLast(target);
        }
      }
    }
    if (result.size() != indegrees.size()) {
      throw new IllegalArgumentException(
          "Активный маршрут содержит цикл. Отключите альтернативную связь или измените направление стадий.");
    }
    return List.copyOf(result);
  }

  private static String identifier(final String value) {
    Objects.requireNonNull(value, "Идентификатор маршрута");
    if (value.length() > 200) {
      throw new IllegalArgumentException("Идентификатор маршрута слишком длинный.");
    }
    return IdentifierValue.requireIdentifier(value, "Идентификатор маршрута");
  }

  private static String signature(final String value) {
    if (value == null || !value.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(
          "Связь маршрута требует подтверждённых подписей обеих структур.");
    }
    return value;
  }

  private static BigDecimal positive(final BigDecimal value, final String label) {
    Objects.requireNonNull(value, label);
    if (value.signum() <= 0 || value.precision() > 34 || Math.abs((long) value.scale()) > 100) {
      throw new IllegalArgumentException(
          label + ": нужно положительное число до 34 значащих цифр в допустимом диапазоне.");
    }
    return value.stripTrailingZeros();
  }
}
