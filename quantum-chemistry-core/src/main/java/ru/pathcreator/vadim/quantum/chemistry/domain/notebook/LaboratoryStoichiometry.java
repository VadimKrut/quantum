/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** Read-only stage arithmetic. Plan and entered results are independent; results retain their evidence origin. */
public final class LaboratoryStoichiometry {
  public enum Basis {
    PLAN,
    ACTUAL
  }

  public record Amount(BigDecimal moles, String explanation) {}

  public record Row(
      LaboratoryComponent component,
      LaboratoryStageParticipant participant,
      LaboratoryQuantity input,
      Amount amount,
      BigDecimal equivalents,
      BigDecimal theoreticalMoles,
      BigDecimal yieldPercent,
      String diagnostic) {}

  public record Report(
      Basis basis,
      String stageId,
      String referenceId,
      List<Row> rows,
      List<String> limitingIds,
      List<String> diagnostics) {
    public Report {
      rows = List.copyOf(rows);
      limitingIds = List.copyOf(limitingIds);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  private static final MathContext PRECISION = MathContext.DECIMAL128;

  private LaboratoryStoichiometry() {}

  /** Conservative qualification: a mixed set of sources is never presented as a measured yield. */
  public static String yieldLabel(final Report report, final Row product) {
    if (report.basis() == Basis.PLAN) {
      return "Плановый расчёт";
    }
    final Set<LaboratoryEvidence> origins = new HashSet<>();
    origins.add(product.component().evidence());
    report.rows().stream()
        .filter(r -> r.participant().role() == ReactionComponentRole.REACTANT && r.input() != null)
        .forEach(r -> origins.add(r.component().evidence()));
    if (origins.size() != 1) {
      return "Выход · смешанные источники";
    }
    return switch (origins.iterator().next()) {
      case OBSERVED -> "Фактический выход";
      case LITERATURE -> "Выход по литературным количествам";
      case SIMULATED -> "Модельный выход";
      case PLANNED -> "Выход · источник не подтверждён";
    };
  }

  public static Amount amount(final LaboratoryComponent component, final Basis basis) {
    return amount(component, basis, Map.of());
  }

  /** The opt-in model is only for PLAN and only for otherwise unspecified material properties. */
  public static Amount amount(
      final LaboratoryComponent component,
      final Basis basis,
      final Map<String, BigDecimal> structuralMasses) {
    final var quantity = basis == Basis.PLAN ? component.planned() : component.actual();
    if (quantity == null) {
      return new Amount(null, "Количество не указано.");
    }
    if (quantity.unit().dimension().equals("amount")) {
      return new Amount(
          quantity.in(LaboratoryQuantityUnit.MOLE).value(),
          "Количество вещества введено напрямую.");
    }
    final var properties = component.properties();
    if (!properties.empty() && !properties.matches(component.molecule())) {
      return new Amount(null, "Структура изменена: проверьте и подтвердите свойства материала.");
    }
    if (quantity.unit().dimension().equals("volume") && properties.molarity() != null) {
      return new Amount(
          quantity
              .in(LaboratoryQuantityUnit.LITRE)
              .value()
              .multiply(properties.molarity(), PRECISION),
          "n = c × V; c = "
              + properties.molarity().toPlainString()
              + " моль/л; V = "
              + quantity.in(LaboratoryQuantityUnit.LITRE).value().toPlainString()
              + " л. Концентрация относится к этому веществу, массовая доля повторно не применяется. Источник: "
              + properties.source());
    }
    if (basis == Basis.PLAN && properties.empty() && quantity.unit().dimension().equals("mass")) {
      final BigDecimal mass = LaboratoryBatchPlan.planMolarMass(component, structuralMasses);
      if (mass != null) {
        return new Amount(
            quantity.in(LaboratoryQuantityUnit.GRAM).value().divide(mass, PRECISION),
            "Расчёт по структуре для чистого вещества: n = m / M; M = "
                + mass.toPlainString()
                + " г/моль. Чистота образца не измерена и не записана.");
      }
    }
    final List<String> missing = new ArrayList<>();
    if (properties.molarMass() == null) {
      missing.add("молярная масса (г/моль)");
    }
    if (properties.purityPercent() == null) {
      missing.add("массовая доля (%)");
    }
    if (quantity.unit().dimension().equals("volume") && properties.density() == null) {
      missing.add("плотность (г/мл) либо молярная концентрация (моль/л)");
    }
    if (!missing.isEmpty()) {
      return new Amount(null, "Не указаны: " + String.join(", ", missing) + ".");
    }
    final BigDecimal mass =
        quantity.unit().dimension().equals("mass")
            ? quantity.in(LaboratoryQuantityUnit.GRAM).value()
            : quantity
                .in(LaboratoryQuantityUnit.MILLILITRE)
                .value()
                .multiply(properties.density(), PRECISION);
    final BigDecimal moles =
        mass.multiply(properties.purityPercent(), PRECISION)
            .movePointLeft(2)
            .divide(properties.molarMass(), PRECISION);
    return new Amount(
        moles,
        (quantity.unit().dimension().equals("mass") ? "n = m × w / M" : "n = V × ρ × w / M")
            + "; M = "
            + properties.molarMass().toPlainString()
            + " г/моль; w = "
            + properties.purityPercent().toPlainString()
            + " %; m = "
            + mass.toPlainString()
            + " г"
            + (quantity.unit().dimension().equals("volume")
                ? "; ρ = " + properties.density().toPlainString() + " г/мл"
                : "")
            + ". Источник: "
            + properties.source());
  }

  /** Preparative yield explicitly relative to one selected substrate and entered product ratio. */
  public static Report preparative(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final Basis basis,
      final String referenceId) {
    return preparative(experiment, stage, basis, referenceId, Map.of());
  }

  public static Report preparative(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final Basis basis,
      final String referenceId,
      final Map<String, BigDecimal> structuralMasses) {
    final Report strict = calculate(experiment, stage, basis, referenceId, structuralMasses);
    final Row reference =
        strict.rows().stream()
            .filter(r -> r.component().id().equals(referenceId))
            .findFirst()
            .orElse(null);
    final List<String> notes = new ArrayList<>();
    notes.add(
        "Препаративный расчёт относительно выбранного исходника и коэффициентов стадии. Это предел 100 %, не прогноз и не подтверждение полного баланса.");
    final BigDecimal extent =
        reference == null
                || reference.amount().moles() == null
                || reference.amount().moles().signum() <= 0
                || reference.participant().coefficient() == null
            ? null
            : reference
                .amount()
                .moles()
                .divide(
                    BigDecimal.valueOf(reference.participant().coefficient().value()), PRECISION);
    if (extent == null) {
      if (reference == null) {
        notes.add("Для выхода выберите опорный исходник.");
      } else if (reference.input() == null) {
        notes.add(
            "Для выхода укажите "
                + (basis == Basis.ACTUAL ? "внесённое" : "плановое")
                + " количество опорного исходника.");
      } else if (reference.amount().moles() == null) {
        notes.add(
            "Количество опорного исходника «"
                + reference.component().molecule().displayName()
                + "» не пересчитано: "
                + reference.amount().explanation());
      } else if (reference.amount().moles().signum() <= 0) {
        notes.add("Для выхода количество опорного исходника должно быть больше нуля.");
      } else {
        notes.add("Для выхода укажите стехиометрический коэффициент опорного исходника в стадии.");
      }
    }
    if (extent != null) {
      for (final Row row : strict.rows()) {
        if (row.participant().role() == ReactionComponentRole.REACTANT
            && row.participant().coefficient() != null
            && row.amount().moles() != null
            && row.amount()
                    .moles()
                    .divide(BigDecimal.valueOf(row.participant().coefficient().value()), PRECISION)
                    .compareTo(extent)
                < 0) {
          notes.add(
              "Меньший стехиометрический предел у «"
                  + row.component().molecule().displayName()
                  + "». Выберите его опорным для выхода по лимитирующему исходнику.");
        }
      }
    }
    final List<Row> rows =
        strict.rows().stream()
            .map(
                row -> {
                  if (row.participant().role() != ReactionComponentRole.PRODUCT) {
                    return row;
                  }
                  final BigDecimal theoretical =
                      extent == null || row.participant().coefficient() == null
                          ? null
                          : extent.multiply(
                              BigDecimal.valueOf(row.participant().coefficient().value()),
                              PRECISION);
                  final BigDecimal yield =
                      basis == Basis.ACTUAL
                              && row.component().evidence().reported()
                              && theoretical != null
                              && row.amount().moles() != null
                          ? row.amount().moles().divide(theoretical, PRECISION).movePointRight(2)
                          : null;
                  final String warning =
                      yield != null && yield.compareTo(new BigDecimal("100")) > 0
                          ? " Выход выше 100 %: проверьте исходные значения."
                          : "";
                  return new Row(
                      row.component(),
                      row.participant(),
                      row.input(),
                      row.amount(),
                      row.equivalents(),
                      theoretical,
                      yield,
                      row.diagnostic() + warning);
                })
            .toList();
    return new Report(basis, stage.id(), referenceId, rows, strict.limitingIds(), notes);
  }

  public static Report calculate(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final Basis basis,
      final String referenceId) {
    return calculate(experiment, stage, basis, referenceId, Map.of());
  }

  public static Report calculate(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final Basis basis,
      final String referenceId,
      final Map<String, BigDecimal> structuralMasses) {
    Objects.requireNonNull(basis);
    if (!experiment.stages().contains(stage)) {
      throw new IllegalArgumentException("Стадия не относится к текущему опыту.");
    }
    final Map<String, LaboratoryComponent> components = new HashMap<>();
    experiment.components().forEach(component -> components.put(component.id(), component));
    final Map<String, Amount> amounts = new HashMap<>();
    stage
        .participants()
        .forEach(
            participant ->
                amounts.put(
                    participant.componentId(),
                    amount(components.get(participant.componentId()), basis, structuralMasses)));
    final var reactants =
        stage.participants().stream()
            .filter(participant -> participant.role() == ReactionComponentRole.REACTANT)
            .toList();
    if (referenceId != null
        && reactants.stream()
            .noneMatch(participant -> participant.componentId().equals(referenceId))) {
      throw new IllegalArgumentException(
          "За один эквивалент можно выбрать только исходник этой стадии.");
    }
    final Amount reference = referenceId == null ? null : amounts.get(referenceId);
    final BigDecimal referenceMoles = reference == null ? null : reference.moles();
    final List<String> diagnostics = new ArrayList<>(), limiting = new ArrayList<>();
    if (referenceMoles == null || referenceMoles.signum() == 0) {
      diagnostics.add(
          "Эквиваленты недоступны: выберите исходник с известным ненулевым количеством вещества.");
    }
    BigDecimal extent = null;
    boolean complete = !reactants.isEmpty();
    final Map<String, List<LaboratoryStageParticipant>> loads = new LinkedHashMap<>();
    for (final var participant : reactants) {
      loads
          .computeIfAbsent(
              components.get(participant.componentId()).molecule().id().value(),
              ignored -> new ArrayList<>())
          .add(participant);
    }
    for (final var group : loads.values()) {
      BigDecimal moles = BigDecimal.ZERO, coefficient = BigDecimal.ZERO;
      boolean known = true;
      for (final var participant : group) {
        final var amount = amounts.get(participant.componentId());
        if (amount.moles() == null || participant.coefficient() == null) {
          known = false;
          continue;
        }
        moles = moles.add(amount.moles(), PRECISION);
        coefficient = coefficient.add(BigDecimal.valueOf(participant.coefficient().value()));
      }
      if (!known) {
        complete = false;
        continue;
      }
      final var possible = moles.divide(coefficient, PRECISION);
      final var ids = group.stream().map(LaboratoryStageParticipant::componentId).toList();
      if (extent == null || possible.compareTo(extent) < 0) {
        extent = possible;
        limiting.clear();
        limiting.addAll(ids);
      } else if (possible.compareTo(extent) == 0) {
        limiting.addAll(ids);
      }
    }
    if (loads.values().stream().anyMatch(group -> group.size() > 1)) {
      diagnostics.add(
          "Загрузки одной молекулы объединены для стехиометрического предела: складываются количества вещества и коэффициенты. Строки плана и факта сохраняются отдельно.");
    }
    if (!complete) {
      extent = null;
      limiting.clear();
      diagnostics.add(
          "Теоретическое количество недоступно: нужны количества вещества и коэффициенты всех исходников.");
    }
    boolean balanced = false;
    try {
      final var balance = stage.toReaction(experiment).balance();
      balanced = balance.atomBalanced() && balance.chargeBalanced() && balance.massNumberBalanced();
      if (!balanced) {
        diagnostics.add(
            "Теоретическое количество и выход отключены: не соблюдён баланс явных атомов, заряда или изотопных массовых чисел.");
      }
    } catch (final IllegalArgumentException exception) {
      diagnostics.add("Теоретическое количество и выход: " + exception.getMessage());
    }
    if (stage.arrow() == LaboratoryReactionStage.Arrow.EQUILIBRIUM) {
      diagnostics.add(
          "Равновесная стадия: теоретическое количество — стехиометрический предел, без расчёта равновесия.");
    }
    final List<Row> rows = new ArrayList<>();
    for (final var participant : stage.participants()) {
      final var component = components.get(participant.componentId());
      final var amount = amounts.get(component.id());
      final BigDecimal equivalents =
          amount.moles() == null || referenceMoles == null || referenceMoles.signum() == 0
              ? null
              : amount.moles().divide(referenceMoles, PRECISION);
      final BigDecimal theoretical =
          participant.role() == ReactionComponentRole.PRODUCT
                  && balanced
                  && extent != null
                  && participant.coefficient() != null
              ? extent.multiply(BigDecimal.valueOf(participant.coefficient().value()), PRECISION)
              : null;
      final BigDecimal yield =
          basis == Basis.ACTUAL
                  && component.evidence().reported()
                  && theoretical != null
                  && theoretical.signum() > 0
                  && amount.moles() != null
              ? amount
                  .moles()
                  .divide(theoretical, PRECISION)
                  .multiply(new BigDecimal("100"), PRECISION)
              : null;
      String diagnostic =
          (basis == Basis.ACTUAL ? component.evidence() + ". " : "") + amount.explanation();
      if (participant.coefficient() == null
          && (participant.role() == ReactionComponentRole.REACTANT
              || participant.role() == ReactionComponentRole.PRODUCT)) {
        diagnostic += " Коэффициент не указан.";
      }
      if (participant.role() == ReactionComponentRole.PRODUCT
          && basis == Basis.ACTUAL
          && !component.evidence().reported()) {
        diagnostic +=
            " Происхождение результата продукта не подтверждено: процент выхода не рассчитывается.";
      }
      if (theoretical != null && theoretical.signum() == 0) {
        diagnostic += " Теоретическое количество нулевое; процент выхода не определён.";
      }
      if (yield != null && yield.compareTo(new BigDecimal("100")) > 0) {
        diagnostic +=
            " Выход выше 100 %: проверьте количества, массовую долю, структуру и коэффициенты.";
      }
      rows.add(
          new Row(
              component,
              participant,
              basis == Basis.PLAN ? component.planned() : component.actual(),
              amount,
              equivalents,
              theoretical,
              yield,
              diagnostic));
    }
    return new Report(basis, stage.id(), referenceId, rows, limiting, diagnostics);
  }
}
