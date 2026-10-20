/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySynthesisRoute;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Selects an explicitly configured source without inventing or reconfirming links. */
public final class LaboratoryRouteSelection {
  private LaboratoryRouteSelection() {}

  public record Selection(
      LaboratorySynthesisRoute route, List<String> selectedLinkIds, List<String> disabledLinkIds) {
    public Selection {
      Objects.requireNonNull(route);
      selectedLinkIds = List.copyOf(selectedLinkIds);
      disabledLinkIds = List.copyOf(disabledLinkIds);
    }
  }

  private record Input(String stageId, String componentId) {}

  /** Returns a completely validated candidate; the caller commits it once using the same source snapshot. */
  public static Selection useStage(final LaboratoryDrawing source, final String stageId) {
    Objects.requireNonNull(source);
    final var route = source.experiment().route();
    if (route.nodes().stream().noneMatch(n -> n.stageId().equals(stageId))) {
      throw new IllegalArgumentException(
          "Сначала добавьте выбранную стадию в маршрут и запишите её параметры.");
    }
    final var selected =
        route.links().stream().filter(l -> l.sourceStageId().equals(stageId)).toList();
    if (selected.isEmpty()) {
      throw new IllegalArgumentException(
          "У стадии пока нет исходящих связей. Укажите их вручную на вкладке «Связи».");
    }
    final Set<Input> inputs = new HashSet<>();
    for (final var link : selected) {
      if (!inputs.add(new Input(link.targetStageId(), link.targetComponentId()))) {
        throw new IllegalArgumentException(
            "У выбранной стадии несколько вариантов передачи одному исходнику. Сначала оставьте один вариант связи вручную.");
      }
      if (route.nodes().stream()
          .noneMatch(n -> n.stageId().equals(link.targetStageId()) && n.enabled())) {
        throw new IllegalArgumentException(
            "Получающая стадия выключена: "
                + stageName(source, link.targetStageId())
                + ". Включите её явно перед выбором источника.");
      }
    }
    final var nodes =
        route.nodes().stream()
            .map(
                n ->
                    n.stageId().equals(stageId)
                        ? new LaboratorySynthesisRoute.Node(
                            n.stageId(),
                            n.referenceComponentId(),
                            n.productComponentId(),
                            n.expectedYieldPercent(),
                            true)
                        : n)
            .toList();
    final var disabled =
        route.links().stream()
            .filter(
                l ->
                    !l.sourceStageId().equals(stageId)
                        && l.enabled()
                        && inputs.contains(new Input(l.targetStageId(), l.targetComponentId())))
            .map(LaboratorySynthesisRoute.Link::id)
            .toList();
    final var links =
        route.links().stream()
            .map(
                l -> {
                  final boolean enabled =
                      l.sourceStageId().equals(stageId)
                          || (!inputs.contains(new Input(l.targetStageId(), l.targetComponentId()))
                              && l.enabled());
                  return enabled == l.enabled()
                      ? l
                      : new LaboratorySynthesisRoute.Link(
                          l.id(),
                          l.sourceStageId(),
                          l.targetStageId(),
                          l.targetComponentId(),
                          l.transferFraction(),
                          l.sourceSignature(),
                          l.targetSignature(),
                          enabled);
                })
            .toList();
    final var candidate =
        new LaboratorySynthesisRoute(nodes, links, route.targetStageId(), route.targetQuantity());
    final var preview = new LaboratoryEditor();
    preview.open(source);
    preview.route(source, candidate);
    final var errors = LaboratoryRoutePlanning.diagnostics(preview.document());
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Источник не переключён. " + String.join("\n", errors));
    }
    return new Selection(
        candidate, selected.stream().map(LaboratorySynthesisRoute.Link::id).toList(), disabled);
  }

  private static String stageName(final LaboratoryDrawing source, final String id) {
    return source.experiment().stages().stream()
        .filter(s -> s.id().equals(id))
        .map(s -> s.title())
        .findFirst()
        .orElse(id);
  }
}
