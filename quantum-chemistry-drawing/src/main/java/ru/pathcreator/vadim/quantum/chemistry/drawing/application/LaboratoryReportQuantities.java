/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryExperimentReport.*;

/** Human-readable quantities with the same explicit pure-structure estimate as the workbench. */
final class LaboratoryReportQuantities {
  private LaboratoryReportQuantities() {}

  static String decimal(final BigDecimal n) {
    if (n == null) {
      return "не рассчитано";
    }
    final var shown = n.round(new MathContext(5, RoundingMode.HALF_UP)).stripTrailingZeros();
    return shown.toPlainString().replace('.', ',');
  }

  static String quantity(final LaboratoryQuantity q) {
    return q == null ? "не записано" : decimal(q.value()) + " " + q.unit();
  }

  private static LaboratoryComponent component(final LaboratoryExperiment e, final String id) {
    return e.components().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
  }

  private static boolean same(final LaboratoryQuantity a, final LaboratoryQuantity b) {
    return a == null && b == null
        || a != null
            && b != null
            && a.unit().dimension().equals(b.unit().dimension())
            && a.value()
                    .multiply(a.unit().factor())
                    .compareTo(b.value().multiply(b.unit().factor()))
                == 0;
  }

  static void append(
      final LaboratoryExperiment e,
      final LaboratoryReactionStage stage,
      final Options options,
      final Map<String, BigDecimal> masses,
      final List<Block> blocks) {
    final var reactants =
        stage.participants().stream()
            .filter(p -> p.role() == ReactionComponentRole.REACTANT)
            .map(p -> component(e, p.componentId()))
            .toList();
    final String chosen = options.references().get(stage.id());
    final var auto = LaboratoryYieldCalculation.automaticReference(e, stage, masses);
    final String requestedReference = chosen == null ? auto.id() : chosen.isBlank() ? null : chosen;
    final var source =
        reactants.stream().filter(c -> c.id().equals(requestedReference)).findFirst().orElse(null);
    final String reference = source == null ? null : source.id();
    final boolean actual = reactants.stream().anyMatch(c -> c.actual() != null);
    final String planReference =
        source != null ? source.id() : reactants.isEmpty() ? null : reactants.getFirst().id();
    final var basis =
        actual ? LaboratoryStoichiometry.Basis.ACTUAL : LaboratoryStoichiometry.Basis.PLAN;
    final Map<String, LaboratoryStoichiometry.Amount> amounts = new LinkedHashMap<>();
    for (final var c : reactants) {
      amounts.put(
          c.id(),
          actual
              ? LaboratoryYieldCalculation.enteredAmount(c, masses)
              : LaboratoryStoichiometry.amount(c, basis, masses));
    }
    final var refAmount = amounts.get(actual ? reference : planReference);
    final boolean showMoles = amounts.values().stream().anyMatch(a -> a.moles() != null);
    final boolean showEq =
        refAmount != null && refAmount.moles() != null && refAmount.moles().signum() > 0;
    final boolean showPlan =
        actual
            && reactants.stream()
                .anyMatch(c -> c.planned() != null && !same(c.planned(), c.actual()));
    boolean estimate = false;
    if (!reactants.isEmpty()) {
      blocks.add(new Heading(actual ? "Загрузка" : "План загрузки", 2));
      final List<String> headers = new ArrayList<>(List.of("Вещество"));
      if (showPlan) {
        headers.add("План");
      }
      headers.add(actual ? "Внесено" : "Количество");
      if (showMoles) {
        headers.add("n, ммоль");
      }
      if (showEq) {
        headers.add("Экв.");
      }
      final List<List<String>> rows = new ArrayList<>();
      final Set<String> issues = new LinkedHashSet<>();
      for (final var c : reactants) {
        final var input = actual ? c.actual() : c.planned();
        final var a = amounts.get(c.id());
        final List<String> row = new ArrayList<>(List.of(c.molecule().displayName()));
        if (showPlan) {
          row.add(quantity(c.planned()));
        }
        row.add(
            quantity(input)
                + (actual && input != null && c.evidence() != LaboratoryEvidence.OBSERVED
                    ? "\n" + c.evidence().quantityLabel()
                    : ""));
        if (showMoles) {
          row.add(
              a.moles() == null
                  ? input == null ? "не записано" : "не рассчитано"
                  : decimal(a.moles().movePointRight(3)));
        }
        if (showEq) {
          row.add(
              a.moles() == null
                  ? "не рассчитано"
                  : decimal(a.moles().divide(refAmount.moles(), MathContext.DECIMAL128)));
        }
        rows.add(row);
        if (input != null && a.moles() == null) {
          issues.add(c.molecule().displayName() + ": " + a.explanation());
        }
        if (input != null
            && a.moles() != null
            && c.properties().empty()
            && input.unit().dimension().equals("mass")) {
          estimate = true;
        }
      }
      blocks.add(new Table(headers, rows));
      issues.forEach(v -> blocks.add(new Paragraph(v, true)));
    }
    final var products =
        stage.participants().stream()
            .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
            .toList();
    if (products.isEmpty()) {
      if (estimate) {
        blocks.add(
            new Paragraph(
                "Расчёт по молярным массам структур, без поправки на неизвестную чистоту.", true));
      }
      return;
    }
    final var strict =
        options.strict()
            ? LaboratoryStoichiometry.calculate(
                e, stage, LaboratoryStoichiometry.Basis.ACTUAL, reference)
            : null;
    blocks.add(new Heading("Результат", 2));
    final Set<String> qualifications = new LinkedHashSet<>();
    for (final var p : products) {
      final var c = component(e, p.componentId());
      blocks.add(new Paragraph(c.molecule().displayName(), false));
      final var mass = LaboratoryBatchPlan.planMolarMass(c, masses);
      blocks.add(
          new Paragraph(
              c.molecule().formula().hillNotation()
                  + (mass == null ? "" : "; M = " + decimal(mass) + " г/моль"),
              true));
      final var n = LaboratoryYieldCalculation.enteredAmount(c, masses).moles();
      final var value = LaboratoryYieldCalculation.calculate(e, stage, reference, c.id(), masses);
      final var strictRow =
          strict == null
              ? null
              : strict.rows().stream()
                  .filter(v -> v.component().id().equals(c.id()))
                  .findFirst()
                  .orElse(null);
      final BigDecimal percent =
          options.strict() ? strictRow == null ? null : strictRow.yieldPercent() : value.percent();
      final String provenance =
          options.strict() && strictRow != null
              ? LaboratoryStoichiometry.yieldLabel(strict, strictRow)
              : value.label();
      String result =
          c.actual() == null
              ? "Полученное количество не записано."
              : (c.evidence() == LaboratoryEvidence.OBSERVED
                      ? "Получено: "
                      : c.evidence().quantityLabel() + ": ")
                  + quantity(c.actual())
                  + (n == null ? "" : " (" + decimal(n.movePointRight(3)) + " ммоль)");
      if (percent != null) {
        result +=
            "; "
                + (provenance.equals("Выход") || provenance.equals("Фактический выход")
                    ? "выход"
                    : provenance.toLowerCase(Locale.ROOT))
                + " "
                + percent
                    .setScale(1, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
                    .replace('.', ',')
                + " %.";
      }
      blocks.add(new Paragraph(result, false));
      final BigDecimal theory =
          options.strict()
              ? strictRow == null ? null : strictRow.theoreticalMoles()
              : LaboratoryYieldCalculation.theoreticalMoles(e, stage, reference, c.id(), masses);
      if (theory != null) {
        blocks.add(
            new Paragraph(
                "Расчётное количество: "
                    + (mass == null
                        ? decimal(theory.movePointRight(3)) + " ммоль"
                        : decimal(theory.multiply(mass)) + " г")
                    + ".",
                true));
      }
      if (percent == null && c.actual() != null) {
        final String reason =
            reference == null
                ? chosen == null ? auto.explanation() : "Не выбран исходник для расчёта."
                : options.strict()
                    ? "Проверьте баланс реакции и свойства веществ."
                    : value.explanation().split("\\n", 2)[0];
        qualifications.add("Выход не рассчитан. " + reason);
      }
      if (!options.strict() && percent != null && !value.qualification().isBlank()) {
        if (value.qualification().contains("неизвестной чистоты")) {
          estimate = true;
        }
        if (percent.compareTo(new BigDecimal("100")) > 0) {
          qualifications.add("Выход выше 100 %: проверьте количества и чистоту образца.");
        }
      }
    }
    if (source != null && source.actual() != null) {
      blocks.add(
          new Paragraph(
              "Выход рассчитан по "
                  + source.molecule().displayName()
                  + " ("
                  + quantity(source.actual())
                  + ").",
              true));
    }
    if (estimate) {
      blocks.add(
          new Paragraph(
              "Расчёт по молярным массам структур, без поправки на неизвестную чистоту.", true));
    }
    qualifications.forEach(v -> blocks.add(new Paragraph(v, true)));
    if (options.strict() && strict != null && !strict.diagnostics().isEmpty()) {
      strict.diagnostics().forEach(v -> blocks.add(new Paragraph(v, true)));
    }
    if (options.diagnostics()) {
      blocks.add(new Heading("Подробности расчёта", 2));
      for (final var c : reactants) {
        blocks.add(
            new Paragraph(
                c.molecule().displayName() + ": " + amounts.get(c.id()).explanation(), true));
      }
    }
  }
}
