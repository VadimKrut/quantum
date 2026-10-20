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
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereoIdentity;

/** Backward preparative demand propagation over an explicitly connected route, using existing quantity arithmetic. */
public final class LaboratoryRoutePlanning {

  private static final MathContext MC = MathContext.DECIMAL128;

  public record StagePlan(
      String stageId,
      String productComponentId,
      String referenceComponentId,
      BigDecimal expectedYieldPercent,
      LaboratoryQuantity desiredProduct,
      BigDecimal desiredProductMoles,
      Map<String, LaboratoryQuantity> planned,
      List<String> warnings) {
    public StagePlan {
      planned = Collections.unmodifiableMap(new LinkedHashMap<>(planned));
      warnings = List.copyOf(warnings);
    }
  }

  public record Result(
      String targetStageId,
      LaboratoryQuantity requestedQuantity,
      List<StagePlan> stages,
      List<String> warnings) {
    public Result {
      stages = List.copyOf(stages);
      warnings = List.copyOf(warnings);
    }
  }

  public record PathYield(
      String basisStageId,
      String basisComponentId,
      List<String> stageIds,
      BigDecimal expectedPercent) {
    public PathYield {
      stageIds = List.copyOf(stageIds);
    }
  }

  public record YieldSummary(
      BigDecimal overallPercent,
      String basisStageId,
      String basisComponentId,
      List<PathYield> paths,
      List<String> notes) {
    public YieldSummary {
      paths = List.copyOf(paths);
      notes = List.copyOf(notes);
    }
  }

  /** A stoichiometrically normalised planning yield, not a measured yield or a finite-stock allocation. */
  public static YieldSummary yieldSummary(
      final LaboratoryExperiment experiment, final String targetStageId) {
    final var route = experiment.route();
    final var target = node(route, targetStageId);
    if (!target.enabled()) {
      throw new IllegalArgumentException("Целевая стадия отключена.");
    }
    final Map<String, List<LaboratorySynthesisRoute.Link>> incoming = new LinkedHashMap<>();
    route
        .activeLinks()
        .forEach(l -> incoming.computeIfAbsent(l.targetStageId(), k -> new ArrayList<>()).add(l));
    record Trace(String stageId, List<String> reversePath, BigDecimal fraction) {}
    final var queue = new ArrayDeque<Trace>();
    queue.add(
        new Trace(
            targetStageId, List.of(targetStageId), target.expectedYieldPercent().movePointLeft(2)));
    final List<PathYield> paths = new ArrayList<>();
    int visits = 0;
    boolean truncated = false;
    while (!queue.isEmpty()) {
      if (++visits > 4096 || paths.size() >= 256) {
        truncated = true;
        break;
      }
      final var trace = queue.removeFirst();
      final var links = incoming.getOrDefault(trace.stageId(), List.of());
      if (links.isEmpty()) {
        final var base = node(route, trace.stageId());
        component(experiment, base.referenceComponentId());
        final var stages = new ArrayList<>(trace.reversePath());
        Collections.reverse(stages);
        paths.add(
            new PathYield(
                base.stageId(),
                base.referenceComponentId(),
                stages,
                trace.fraction().movePointRight(2)));
      } else {
        for (final var link : links) {
          final var previous = node(route, link.sourceStageId());
          final var stages = new ArrayList<>(trace.reversePath());
          stages.add(previous.stageId());
          queue.addLast(
              new Trace(
                  previous.stageId(),
                  List.copyOf(stages),
                  trace
                      .fraction()
                      .multiply(link.transferFraction(), MC)
                      .multiply(previous.expectedYieldPercent().movePointLeft(2), MC)));
        }
      }
    }
    final List<String> notes = new ArrayList<>();
    notes.add(
        "Ожидаемый выход по выбранной начальной базе: произведение выходов стадий и долей сохранения при передаче, с нормировкой по коэффициентам. Это не измеренный выход и не отношение к фактической загрузке; избытки и распределение конечного запаса сюда не включены.");
    if (truncated) {
      notes.add(
          "Показаны первые пути: предел 256 завершённых путей / 4096 переходов. Общий процент не вычислен.");
    }
    if (paths.size() != 1) {
      notes.add(
          "Сходящиеся ветви имеют разные пути и базы расхода. Один общий процент для разветвлённого маршрута не утверждается.");
    }
    final var only = !truncated && paths.size() == 1 ? paths.getFirst() : null;
    return new YieldSummary(
        only == null ? null : only.expectedPercent(),
        only == null ? null : only.basisStageId(),
        only == null ? null : only.basisComponentId(),
        paths,
        notes);
  }

  private LaboratoryRoutePlanning() {}

  /** Binds the two user-selected occurrences to their current graphs; names never establish equivalence. */
  public static LaboratorySynthesisRoute.Link link(
      final LaboratoryExperiment experiment,
      final String id,
      final String sourceStageId,
      final String targetStageId,
      final String targetComponentId,
      final BigDecimal transferFraction,
      final boolean enabled) {
    return link(
        experiment,
        null,
        id,
        sourceStageId,
        targetStageId,
        targetComponentId,
        transferFraction,
        enabled);
  }

  public static LaboratorySynthesisRoute.Link link(
      final LaboratoryDrawing drawing,
      final String id,
      final String sourceStageId,
      final String targetStageId,
      final String targetComponentId,
      final BigDecimal transferFraction,
      final boolean enabled) {
    return link(
        drawing.experiment(),
        drawing,
        id,
        sourceStageId,
        targetStageId,
        targetComponentId,
        transferFraction,
        enabled);
  }

  private static LaboratorySynthesisRoute.Link link(
      final LaboratoryExperiment experiment,
      final LaboratoryDrawing drawing,
      final String id,
      final String sourceStageId,
      final String targetStageId,
      final String targetComponentId,
      final BigDecimal transferFraction,
      final boolean enabled) {
    final var source = node(experiment.route(), sourceStageId);
    node(experiment.route(), targetStageId);
    final var from = component(experiment, source.productComponentId());
    final var to = component(experiment, targetComponentId);
    requireParticipant(stage(experiment, sourceStageId), from.id(), ReactionComponentRole.PRODUCT);
    requireParticipant(stage(experiment, targetStageId), to.id(), ReactionComponentRole.REACTANT);
    requireEquivalent(from, to, drawing);
    return new LaboratorySynthesisRoute.Link(
        id,
        sourceStageId,
        targetStageId,
        targetComponentId,
        transferFraction,
        signature(from, drawing),
        signature(to, drawing),
        enabled);
  }

  /** Stale references are retained in the editable document; this reports why they cannot be calculated. */
  public static List<String> diagnostics(final LaboratoryExperiment experiment) {
    return diagnostics(experiment, null, null);
  }

  public static List<String> diagnostics(final LaboratoryDrawing drawing) {
    return diagnostics(drawing.experiment(), null, drawing);
  }

  private static List<String> diagnostics(
      final LaboratoryExperiment experiment,
      final java.util.Set<String> needed,
      final LaboratoryDrawing drawing) {
    final var errors = new ArrayList<String>();
    final var route = experiment.route();
    for (final var node : route.nodes()) {
      if (node.enabled() && (needed == null || needed.contains(node.stageId()))) {
        try {
          final var stage = stage(experiment, node.stageId());
          component(experiment, node.referenceComponentId());
          component(experiment, node.productComponentId());
          requireParticipant(stage, node.referenceComponentId(), ReactionComponentRole.REACTANT);
          requireParticipant(stage, node.productComponentId(), ReactionComponentRole.PRODUCT);
          for (final var participant : stage.participants()) {
            component(experiment, participant.componentId());
            if ((participant.role() == ReactionComponentRole.REACTANT
                    || participant.role() == ReactionComponentRole.PRODUCT)
                && participant.coefficient() == null) {
              throw new IllegalArgumentException(
                  "Укажите коэффициент «"
                      + component(experiment, participant.componentId()).molecule().displayName()
                      + "» в стадии «"
                      + stage.title()
                      + "».");
            }
          }
        } catch (final IllegalArgumentException exception) {
          errors.add(exception.getMessage());
        }
      }
    }
    for (final var link : route.activeLinks()) {
      if (needed != null && !needed.contains(link.targetStageId())) {
        continue;
      }
      try {
        final var from =
            component(experiment, node(route, link.sourceStageId()).productComponentId());
        final var to = component(experiment, link.targetComponentId());
        requireParticipant(
            stage(experiment, link.sourceStageId()), from.id(), ReactionComponentRole.PRODUCT);
        requireParticipant(
            stage(experiment, link.targetStageId()), to.id(), ReactionComponentRole.REACTANT);
        if (!signatureMatches(from, drawing, link.sourceSignature())
            || !signatureMatches(to, drawing, link.targetSignature())) {
          throw new IllegalArgumentException(
              "Структура в связи «" + link.id() + "» изменена. Подтвердите связь заново.");
        }
        requireEquivalent(from, to, drawing);
      } catch (final IllegalArgumentException exception) {
        errors.add(exception.getMessage());
      }
    }
    return errors.stream().distinct().toList();
  }

  public static Result plan(final LaboratoryExperiment experiment) {
    final var route = experiment.route();
    if (route.targetStageId() == null || route.targetQuantity() == null) {
      throw new IllegalArgumentException(
          "Выберите конечную стадию и задайте нужную массу или количество конечного продукта.");
    }
    return plan(experiment, route.targetStageId(), route.targetQuantity());
  }

  public static Result plan(
      final LaboratoryExperiment experiment,
      final String targetStageId,
      final LaboratoryQuantity quantity) {
    return plan(experiment, null, targetStageId, quantity);
  }

  public static Result plan(final LaboratoryDrawing drawing) {
    final var route = drawing.experiment().route();
    if (route.targetStageId() == null || route.targetQuantity() == null) {
      throw new IllegalArgumentException("Выберите конечную стадию и количество.");
    }
    return plan(drawing.experiment(), drawing, route.targetStageId(), route.targetQuantity());
  }

  public static Result plan(
      final LaboratoryDrawing drawing,
      final String targetStageId,
      final LaboratoryQuantity quantity) {
    return plan(drawing.experiment(), drawing, targetStageId, quantity);
  }

  private static Result plan(
      final LaboratoryExperiment experiment,
      final LaboratoryDrawing drawing,
      final String targetStageId,
      final LaboratoryQuantity quantity) {
    Objects.requireNonNull(experiment);
    Objects.requireNonNull(quantity, "Количество конечного продукта");
    bounded(quantity.value(), true, "Количество конечного продукта");
    if (quantity.unit().dimension().equals("volume")) {
      throw new IllegalArgumentException("Цель маршрута задаётся массой или количеством вещества.");
    }
    final var route = experiment.route();
    final var target = node(route, targetStageId);
    if (!target.enabled()) {
      throw new IllegalArgumentException(
          "Целевая стадия отключена. Включите выбранный вариант маршрута.");
    }
    final var incoming = new LinkedHashMap<String, List<LaboratorySynthesisRoute.Link>>();
    for (final var link : route.activeLinks()) {
      incoming.computeIfAbsent(link.targetStageId(), key -> new ArrayList<>()).add(link);
    }
    final var needed = new HashSet<String>();
    final var queue = new ArrayDeque<String>();
    queue.add(targetStageId);
    while (!queue.isEmpty()) {
      final String id = queue.removeFirst();
      if (needed.add(id)) {
        for (final var link : incoming.getOrDefault(id, List.of())) {
          queue.addLast(link.sourceStageId());
        }
      }
    }
    final var errors = diagnostics(experiment, needed, drawing);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join("\n", errors));
    }
    final var targetComponent = component(experiment, target.productComponentId());
    final var initialMoles = moles(targetComponent, quantity);
    bounded(initialMoles, true, "Количество конечного продукта");
    final var demands = new LinkedHashMap<String, BigDecimal>();
    demands.put(targetStageId, initialMoles);
    final var byStage = new LinkedHashMap<String, StagePlan>();
    final var warnings = new ArrayList<String>();
    warnings.add(
        "План по явно заданным ожидаемым выходам. Это расчёт потребности, а не прогноз реакции и не фактический результат.");
    warnings.add(
        "Доли передачи описывают потери при переносе. Потребности ветвей складываются; распределение одной партии по долям здесь не задаётся.");
    for (final String id : route.topologicalStageIds().reversed()) {
      if (!needed.contains(id)) {
        continue;
      }
      final var stagePlan =
          stagePlan(
              experiment,
              node(route, id),
              demands.get(id),
              id.equals(targetStageId) ? quantity : null);
      byStage.put(id, stagePlan);
      for (final var link : incoming.getOrDefault(id, List.of())) {
        final var required = stagePlan.planned().get(link.targetComponentId());
        if (required == null) {
          throw new IllegalArgumentException(
              "Для связанного исходника не рассчитана потребность: " + link.targetComponentId());
        }
        final var consumed = moles(component(experiment, link.targetComponentId()), required);
        final var upstream = consumed.divide(link.transferFraction(), MC);
        bounded(upstream, true, "Потребность промежуточного продукта");
        final var total =
            demands.getOrDefault(link.sourceStageId(), BigDecimal.ZERO).add(upstream, MC);
        bounded(total, true, "Суммарная потребность промежуточного продукта");
        demands.put(link.sourceStageId(), total);
      }
    }
    final var stages =
        route.topologicalStageIds().stream().filter(needed::contains).map(byStage::get).toList();
    if (route.nodes().stream().anyMatch(n -> n.enabled() && !needed.contains(n.stageId()))) {
      warnings.add("Стадии, не ведущие к выбранному продукту, в эту загрузку не включены.");
    }
    return new Result(targetStageId, quantity, stages, warnings);
  }

  /** One component occurrence cannot store different per-stage allocations in the notebook's shared quantity field. */
  public static Map<String, LaboratoryQuantity> combinedPlan(final Result result) {
    final var quantities = new LinkedHashMap<String, LaboratoryQuantity>();
    for (final var stage : result.stages()) {
      for (final var entry : stage.planned().entrySet()) {
        if (quantities.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
          throw new IllegalArgumentException(
              "Компонент «"
                  + entry.getKey()
                  + "» используется в нескольких стадиях расчёта. Создайте отдельные строки для его загрузок; просмотр потребности по стадиям доступен.");
        }
      }
    }
    return Collections.unmodifiableMap(quantities);
  }

  private static StagePlan stagePlan(
      final LaboratoryExperiment experiment,
      final LaboratorySynthesisRoute.Node node,
      final BigDecimal productMoles,
      final LaboratoryQuantity requested) {
    bounded(productMoles, true, "Потребность продукта стадии");
    final var stage = stage(experiment, node.stageId());
    final var reference = component(experiment, node.referenceComponentId());
    final var product = component(experiment, node.productComponentId());
    final var referenceMember =
        requireParticipant(stage, reference.id(), ReactionComponentRole.REACTANT);
    final var productMember =
        requireParticipant(stage, product.id(), ReactionComponentRole.PRODUCT);
    final var referenceMoles =
        productMoles
            .divide(node.expectedYieldPercent().movePointLeft(2), MC)
            .multiply(BigDecimal.valueOf(referenceMember.coefficient().value()), MC)
            .divide(BigDecimal.valueOf(productMember.coefficient().value()), MC);
    bounded(referenceMoles, true, "Загрузка опорного исходника");
    final var warnings = new ArrayList<String>();
    warnings.add(
        "Ожидаемый выход "
            + node.expectedYieldPercent().toPlainString()
            + " % задан пользователем относительно выбранного опорного исходника.");
    final var referenceQuantity =
        quantity(reference, referenceMoles, preferred(reference), warnings);
    BigDecimal oldReferenceMoles = null;
    if (reference.planned() != null && reference.planned().value().signum() > 0) {
      try {
        oldReferenceMoles = moles(reference, reference.planned());
        if (oldReferenceMoles.signum() <= 0) {
          oldReferenceMoles = null;
        }
      } catch (final IllegalArgumentException exception) {
        warnings.add(
            "Исходный масштаб для избытков и вспомогательных веществ недоступен: "
                + exception.getMessage());
      }
    }
    final var targets = new ArrayList<LaboratoryBatchPlan.Target>();
    for (final var participant : stage.participants()) {
      if (participant.componentId().equals(reference.id())
          || participant.role() == ReactionComponentRole.PRODUCT) {
        continue;
      }
      final var material = component(experiment, participant.componentId());
      if (participant.role() == ReactionComponentRole.REACTANT) {
        final var minimum =
            BigDecimal.valueOf(participant.coefficient().value())
                .divide(BigDecimal.valueOf(referenceMember.coefficient().value()), MC);
        BigDecimal equivalents = minimum;
        if (oldReferenceMoles != null && material.planned() != null) {
          try {
            final var ratio = moles(material, material.planned()).divide(oldReferenceMoles, MC);
            if (ratio.compareTo(minimum) > 0) {
              equivalents = ratio;
              warnings.add(
                  "Сохранён заданный в исходном плане избыток «"
                      + material.molecule().displayName()
                      + "»: "
                      + ratio.stripTrailingZeros().toPlainString()
                      + " экв.");
            } else if (ratio.compareTo(minimum) < 0) {
              warnings.add(
                  "«"
                      + material.molecule().displayName()
                      + "»: исходный план ниже стехиометрической потребности; использовано соотношение коэффициентов стадии.");
            }
          } catch (final IllegalArgumentException exception) {
            warnings.add(
                "Избыток «"
                    + material.molecule().displayName()
                    + "» не определён; использовано соотношение коэффициентов. "
                    + exception.getMessage());
          }
        }
        final var selected =
            quantity(
                material, referenceMoles.multiply(equivalents, MC), preferred(material), warnings);
        targets.add(
            new LaboratoryBatchPlan.Target(material.id(), equivalents, selected.unit(), null));
      } else if (material.planned() != null && oldReferenceMoles != null) {
        bounded(material.planned().value(), false, "Исходное количество вспомогательного вещества");
        targets.add(
            new LaboratoryBatchPlan.Target(material.id(), null, material.planned().unit(), null));
      } else {
        warnings.add(
            "Количество «"
                + material.molecule().displayName()
                + "» не назначено: для масштабирования нужен его исходный план и известная загрузка опорного исходника.");
      }
    }
    final var result =
        LaboratoryBatchPlan.calculate(
            experiment, stage, reference.id(), referenceQuantity, targets, true);
    final var planned = new LinkedHashMap<>(result.planned());
    for (final var value : planned.values()) {
      bounded(value.value(), false, "Рассчитанное количество");
    }
    final var desiredProduct =
        requested == null
            ? quantity(product, productMoles, preferred(product), warnings)
            : requested;
    planned.put(product.id(), desiredProduct);
    if (stage.participants().stream().filter(p -> p.role() == ReactionComponentRole.PRODUCT).count()
        > 1) {
      warnings.add(
          "Потребность рассчитана для выбранного продукта. Выходы и количества других продуктов не назначены.");
    }
    if (stage.arrow() == LaboratoryReactionStage.Arrow.EQUILIBRIUM) {
      warnings.add(
          "Равновесная стадия: расчёт использует заданный ожидаемый выход и не моделирует положение равновесия.");
    }
    return new StagePlan(
        stage.id(),
        product.id(),
        reference.id(),
        node.expectedYieldPercent(),
        desiredProduct,
        productMoles,
        planned,
        warnings);
  }

  private static LaboratoryQuantityUnit preferred(final LaboratoryComponent component) {
    return component.planned() == null
        ? LaboratoryQuantityUnit.MILLIMOLE
        : component.planned().unit();
  }

  private static LaboratoryQuantity quantity(
      final LaboratoryComponent component,
      final BigDecimal moles,
      final LaboratoryQuantityUnit preferred,
      final List<String> warnings) {
    bounded(moles, false, "Количество вещества");
    validateProperties(component);
    try {
      final var value = LaboratoryBatchPlan.fromMoles(component, moles, preferred);
      bounded(value.value(), false, "Количество материала");
      return value;
    } catch (final IllegalArgumentException exception) {
      if (preferred.dimension().equals("amount")) {
        throw exception;
      }
      warnings.add(
          "«"
              + component.molecule().displayName()
              + "» показан в ммоль: "
              + exception.getMessage());
      final var value =
          LaboratoryBatchPlan.fromMoles(component, moles, LaboratoryQuantityUnit.MILLIMOLE);
      bounded(value.value(), false, "Количество материала");
      return value;
    }
  }

  private static BigDecimal moles(
      final LaboratoryComponent component, final LaboratoryQuantity quantity) {
    bounded(quantity.value(), false, "Количество материала");
    validateProperties(component);
    final var value = LaboratoryBatchPlan.moles(component, quantity);
    bounded(value, false, "Количество вещества");
    return value;
  }

  private static void validateProperties(final LaboratoryComponent component) {
    final var properties = component.properties();
    for (final var value :
        new BigDecimal[] {
          properties.molarMass(),
          properties.density(),
          properties.purityPercent(),
          properties.molarity()
        }) {
      if (value != null) {
        bounded(value, false, "Свойство материала");
      }
    }
  }

  private static void bounded(final BigDecimal value, final boolean positive, final String label) {
    if (value == null
        || value.signum() < 0
        || positive && value.signum() == 0
        || value.precision() > 100
        || Math.abs((long) value.scale()) > 100) {
      throw new IllegalArgumentException(
          label + ": недопустимое значение или слишком большой порядок числа.");
    }
  }

  private static LaboratorySynthesisRoute.Node node(
      final LaboratorySynthesisRoute route, final String id) {
    return route.nodes().stream()
        .filter(node -> node.stageId().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Стадия не включена в маршрут: " + id));
  }

  private static LaboratoryReactionStage stage(
      final LaboratoryExperiment experiment, final String id) {
    return experiment.stages().stream()
        .filter(stage -> stage.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Стадия маршрута удалена из опыта: " + id));
  }

  private static LaboratoryComponent component(
      final LaboratoryExperiment experiment, final String id) {
    return experiment.components().stream()
        .filter(component -> component.id().equals(id))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Компонент маршрута удалён из опыта: " + id));
  }

  private static LaboratoryStageParticipant requireParticipant(
      final LaboratoryReactionStage stage, final String id, final ReactionComponentRole role) {
    return stage.participants().stream()
        .filter(participant -> participant.componentId().equals(id) && participant.role() == role)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "В стадии «"
                        + stage.title()
                        + "» компонент «"
                        + id
                        + "» должен иметь роль «"
                        + role
                        + "»."));
  }

  private static String signature(
      final LaboratoryComponent component, final LaboratoryDrawing drawing) {
    return drawing == null
        ? LaboratoryMaterialProperties.signature(component.molecule())
        : CdkStereoIdentity.signature(
            Objects.requireNonNull(
                drawing.layouts().get(component.id()), "Нет рисунка участника маршрута."));
  }

  private static boolean signatureMatches(
      final LaboratoryComponent component, final LaboratoryDrawing drawing, final String expected) {
    // Older route records bind the core graph; their depiction is still rechecked on every
    // calculation.
    return expected.equals(signature(component, drawing))
        || expected.equals(LaboratoryMaterialProperties.signature(component.molecule()));
  }

  private static void requireEquivalent(
      final LaboratoryComponent first,
      final LaboratoryComponent second,
      final LaboratoryDrawing drawing) {
    final var result =
        drawing == null
            ? MolecularGraphEquivalence.compare(first.molecule(), second.molecule())
            : CdkStereoIdentity.compare(
                Objects.requireNonNull(drawing.layouts().get(first.id())),
                Objects.requireNonNull(drawing.layouts().get(second.id())));
    if (result != MolecularGraphEquivalence.Result.EQUIVALENT) {
      throw new IllegalArgumentException(
          "Связь «"
              + first.molecule().displayName()
              + "» → «"
              + second.molecule().displayName()
              + "»: "
              + (result == MolecularGraphEquivalence.Result.DISTINCT
                  ? "структуры различаются."
                  : "эквивалентность структур не удалось надёжно установить."));
    }
  }
}
