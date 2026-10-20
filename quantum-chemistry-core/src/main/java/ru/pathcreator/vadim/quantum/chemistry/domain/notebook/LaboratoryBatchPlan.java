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
import java.math.MathContext;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** Batch arithmetic over notebook quantities. No prediction, measured values or stock mutations. */
public final class LaboratoryBatchPlan {
  private static final MathContext MC = MathContext.DECIMAL128;

  public record Target(
      String componentId,
      BigDecimal equivalents,
      LaboratoryQuantityUnit unit,
      LaboratoryQuantity fixed) {
    public Target {
      Objects.requireNonNull(componentId);
      Objects.requireNonNull(unit);
    }
  }

  public record Result(Map<String, LaboratoryQuantity> planned, BigDecimal referenceMoles) {
    public Result {
      planned = Collections.unmodifiableMap(new LinkedHashMap<>(planned));
    }
  }

  private LaboratoryBatchPlan() {}

  public static BigDecimal moles(
      final LaboratoryComponent component, final LaboratoryQuantity quantity) {
    return moles(component, quantity, Map.of());
  }

  /** Explicit plan-only structural mass model; never changes material properties. */
  public static BigDecimal moles(
      final LaboratoryComponent component,
      final LaboratoryQuantity quantity,
      final Map<String, BigDecimal> structuralMasses) {
    final var draft =
        new LaboratoryComponent(
            component.id(),
            component.molecule(),
            component.role(),
            quantity,
            component.actual(),
            component.evidence(),
            component.properties());
    final var amount =
        LaboratoryStoichiometry.amount(draft, LaboratoryStoichiometry.Basis.PLAN, structuralMasses);
    if (amount.moles() == null) {
      throw new IllegalArgumentException(
          component.molecule().displayName() + ": " + amount.explanation());
    }
    return amount.moles();
  }

  public static LaboratoryQuantity fromMoles(
      final LaboratoryComponent component,
      final BigDecimal moles,
      final LaboratoryQuantityUnit unit) {
    return fromMoles(component, moles, unit, Map.of());
  }

  public static LaboratoryQuantity fromMoles(
      final LaboratoryComponent component,
      final BigDecimal moles,
      final LaboratoryQuantityUnit unit,
      final Map<String, BigDecimal> structuralMasses) {
    Objects.requireNonNull(moles);
    Objects.requireNonNull(unit);
    if (moles.signum() < 0) {
      throw new IllegalArgumentException("Количество вещества не может быть отрицательным.");
    }
    if (unit.dimension().equals("amount")) {
      return new LaboratoryQuantity(moles.divide(unit.factor(), MC), unit);
    }
    final var p = component.properties();
    if (p.empty() && unit.dimension().equals("mass")) {
      final BigDecimal mass = planMolarMass(component, structuralMasses);
      if (mass != null) {
        return new LaboratoryQuantity(moles.multiply(mass, MC).divide(unit.factor(), MC), unit);
      }
    }
    if (!p.matches(component.molecule())) {
      throw new IllegalArgumentException(
          "Подтвердите свойства «" + component.molecule().displayName() + "».");
    }
    if (unit.dimension().equals("volume") && p.molarity() != null) {
      if (p.molarity().signum() == 0) {
        throw new IllegalArgumentException("Концентрация должна быть больше нуля.");
      }
      return new LaboratoryQuantity(moles.divide(p.molarity(), MC).divide(unit.factor(), MC), unit);
    }
    if (p.molarMass() == null || p.purityPercent() == null || p.purityPercent().signum() == 0) {
      throw new IllegalArgumentException(
          "Для «"
              + component.molecule().displayName()
              + "» нужны молярная масса и ненулевая массовая доля.");
    }
    final BigDecimal grams =
        moles
            .multiply(p.molarMass(), MC)
            .multiply(new BigDecimal("100"))
            .divide(p.purityPercent(), MC);
    if (unit.dimension().equals("mass")) {
      return new LaboratoryQuantity(grams.divide(unit.factor(), MC), unit);
    }
    if (p.density() == null) {
      throw new IllegalArgumentException(
          "Для объёма «"
              + component.molecule().displayName()
              + "» нужна плотность или концентрация.");
    }
    return new LaboratoryQuantity(
        grams.divide(p.density(), MC).movePointLeft(3).divide(unit.factor(), MC), unit);
  }

  /** A confirmed manual mass wins; stale properties are never bypassed. */
  public static BigDecimal planMolarMass(
      final LaboratoryComponent component, final Map<String, BigDecimal> structuralMasses) {
    final var properties = component.properties();
    if (!properties.empty() && !properties.matches(component.molecule())) {
      throw new IllegalArgumentException(
          "Структура изменена: проверьте свойства «" + component.molecule().displayName() + "».");
    }
    if (properties.molarMass() != null) {
      return properties.molarMass();
    }
    if (!properties.empty()) {
      return null;
    }
    final BigDecimal mass = Objects.requireNonNull(structuralMasses).get(component.id());
    if (mass != null && mass.signum() <= 0) {
      throw new IllegalArgumentException("Структурная молярная масса должна быть больше нуля.");
    }
    return mass;
  }

  public static Result calculate(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final String referenceId,
      final LaboratoryQuantity referenceQuantity,
      final List<Target> targets,
      final boolean scaleSolvents) {
    return calculate(
        experiment, stage, referenceId, referenceQuantity, targets, scaleSolvents, Map.of());
  }

  public static Result calculate(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final String referenceId,
      final LaboratoryQuantity referenceQuantity,
      final List<Target> targets,
      final boolean scaleSolvents,
      final Map<String, BigDecimal> structuralMasses) {
    if (!experiment.stages().contains(stage)) {
      throw new IllegalArgumentException("Стадия не относится к опыту.");
    }
    final Map<String, LaboratoryComponent> components = new LinkedHashMap<>();
    experiment.components().forEach(c -> components.put(c.id(), c));
    if (stage.participants().stream()
        .noneMatch(
            p ->
                p.componentId().equals(referenceId)
                    && p.role() == ReactionComponentRole.REACTANT)) {
      throw new IllegalArgumentException("Выберите исходник этой стадии для задания масштаба.");
    }
    final var reference = components.get(referenceId);
    final BigDecimal base =
        moles(reference, Objects.requireNonNull(referenceQuantity), structuralMasses);
    if (base.signum() <= 0) {
      throw new IllegalArgumentException("Загрузка опорного исходника должна быть больше нуля.");
    }
    final Map<String, LaboratoryQuantity> result = new LinkedHashMap<>();
    result.put(referenceId, referenceQuantity);
    for (final Target target : targets) {
      final var participant =
          stage.participants().stream()
              .filter(p -> p.componentId().equals(target.componentId()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Компонент не входит в стадию."));
      if (participant.role() == ReactionComponentRole.PRODUCT
          || target.componentId().equals(referenceId)) {
        throw new IllegalArgumentException(
            "Продукт и опорный исходник не входят в список дозирования.");
      }
      if (result.containsKey(target.componentId())) {
        throw new IllegalArgumentException("Компонент указан дважды.");
      }
      final var component = components.get(target.componentId());
      if (target.fixed() != null) {
        result.put(component.id(), target.fixed());
        continue;
      }
      if (participant.role() == ReactionComponentRole.SOLVENT) {
        if (component.planned() == null) {
          continue;
        }
        if (!scaleSolvents) {
          result.put(component.id(), component.planned().in(target.unit()));
          continue;
        }
        final BigDecimal oldBase = moles(reference, reference.planned(), structuralMasses);
        if (oldBase.signum() <= 0) {
          throw new IllegalArgumentException(
              "Для масштабирования растворителя нужен исходный ненулевой план.");
        }
        result.put(
            component.id(),
            new LaboratoryQuantity(
                    component.planned().value().multiply(base, MC).divide(oldBase, MC),
                    component.planned().unit())
                .in(target.unit()));
      } else if (target.equivalents() == null && component.planned() != null) {
        final BigDecimal oldBase = moles(reference, reference.planned(), structuralMasses);
        if (oldBase.signum() <= 0) {
          throw new IllegalArgumentException(
              "Для пропорционального пересчёта нужен исходный ненулевой план.");
        }
        result.put(
            component.id(),
            new LaboratoryQuantity(
                    component.planned().value().multiply(base, MC).divide(oldBase, MC),
                    component.planned().unit())
                .in(target.unit()));
      } else {
        if (target.equivalents() == null || target.equivalents().signum() < 0) {
          throw new IllegalArgumentException(
              "Укажите неотрицательные эквиваленты для «"
                  + component.molecule().displayName()
                  + "».");
        }
        result.put(
            component.id(),
            fromMoles(
                component,
                base.multiply(target.equivalents(), MC),
                target.unit(),
                structuralMasses));
      }
    }
    return new Result(result, base);
  }
}
