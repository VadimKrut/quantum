/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** Read-only yield presentation. Missing measured inputs are never replaced by the plan. */
public final class LaboratoryYieldCalculation {
  public record Value(BigDecimal percent, String label, String qualification, String explanation) {
    public String text() {
      if (this.percent == null) {
        return this.label;
      }
      final var shown = this.percent.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
      return this.label
          + " "
          + (shown.compareTo(this.percent) == 0 ? "" : "≈ ")
          + shown.toPlainString()
          + " %";
    }
  }

  private LaboratoryYieldCalculation() {}

  public record Reference(String id, String explanation) {}

  /** Select by entered reactant amounts / coefficients, only when every input is usable. */
  public static Reference automaticReference(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final Map<String, BigDecimal> masses) {
    if (stage == null) {
      return new Reference(null, "Добавьте стадию реакции.");
    }
    String selected = null;
    BigDecimal smallest = null;
    for (final var p : stage.participants()) {
      if (p.role() != ReactionComponentRole.REACTANT) {
        continue;
      }
      final var c =
          experiment.components().stream()
              .filter(v -> v.id().equals(p.componentId()))
              .findFirst()
              .orElseThrow();
      if (p.coefficient() == null) {
        return new Reference(
            null,
            "Укажите коэффициент исходника «"
                + c.molecule().displayName()
                + "» в участниках стадии.");
      }
      if (c.actual() == null || c.actual().value().signum() <= 0 || !c.evidence().reported()) {
        return new Reference(
            null,
            "Запишите фактическую загрузку «"
                + c.molecule().displayName()
                + "». План для расчёта выхода не используется.");
      }
      final var amount = enteredAmount(c, masses);
      if (amount.moles() == null) {
        return new Reference(null, c.molecule().displayName() + ": " + amount.explanation());
      }
      final var extent =
          amount
              .moles()
              .divide(BigDecimal.valueOf(p.coefficient().value()), MathContext.DECIMAL128);
      if (smallest == null || extent.compareTo(smallest) < 0) {
        smallest = extent;
        selected = c.id();
      }
    }
    return new Reference(
        selected,
        selected == null
            ? "Добавьте исходники в участники стадии."
            : "Автоматически выбран исходник, который ограничивает количество продукта, с учётом записанных количеств и коэффициентов реакции.");
  }

  public static LaboratoryStoichiometry.Amount enteredAmount(
      final LaboratoryComponent c, final Map<String, BigDecimal> masses) {
    final var strict = LaboratoryStoichiometry.amount(c, LaboratoryStoichiometry.Basis.ACTUAL);
    if (strict.moles() != null) {
      return strict;
    }
    final var projection =
        new LaboratoryComponent(
            c.id(), c.molecule(), c.role(), c.actual(), c.actual(), c.evidence(), c.properties());
    return LaboratoryStoichiometry.amount(projection, LaboratoryStoichiometry.Basis.PLAN, masses);
  }

  public static BigDecimal theoreticalMoles(
      final LaboratoryExperiment e,
      final LaboratoryReactionStage stage,
      final String reference,
      final String product,
      final Map<String, BigDecimal> masses) {
    if (reference == null) {
      return null;
    }
    final var a =
        stage.participants().stream()
            .filter(
                p ->
                    p.componentId().equals(reference) && p.role() == ReactionComponentRole.REACTANT)
            .findFirst()
            .orElse(null);
    final var b =
        stage.participants().stream()
            .filter(
                p -> p.componentId().equals(product) && p.role() == ReactionComponentRole.PRODUCT)
            .findFirst()
            .orElse(null);
    final var c =
        e.components().stream().filter(v -> v.id().equals(reference)).findFirst().orElse(null);
    if (a == null
        || b == null
        || a.coefficient() == null
        || b.coefficient() == null
        || c == null
        || !c.evidence().reported()) {
      return null;
    }
    final var n = enteredAmount(c, masses).moles();
    return n == null || n.signum() <= 0
        ? null
        : n.divide(BigDecimal.valueOf(a.coefficient().value()), MathContext.DECIMAL128)
            .multiply(BigDecimal.valueOf(b.coefficient().value()));
  }

  public static Value calculate(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final String referenceId,
      final String productId,
      final Map<String, BigDecimal> structuralMasses) {
    if (referenceId == null) {
      return unavailable("Выберите исходник для расчёта выхода.");
    }
    final var source =
        experiment.components().stream()
            .filter(c -> c.id().equals(referenceId))
            .findFirst()
            .orElse(null);
    final var product =
        experiment.components().stream()
            .filter(c -> c.id().equals(productId))
            .findFirst()
            .orElse(null);
    if (source == null
        || product == null
        || stage.participants().stream()
            .noneMatch(
                p ->
                    p.componentId().equals(productId)
                        && p.role() == ReactionComponentRole.PRODUCT)) {
      return unavailable("Выберите исходник и продукт этой стадии.");
    }
    if (source.actual() == null || source.actual().value().signum() <= 0) {
      return unavailable("Запишите фактическую загрузку выбранного исходника.");
    }
    if (product.actual() == null) {
      return unavailable("Запишите полученное количество продукта.");
    }
    if (!product.evidence().reported() || !source.evidence().reported()) {
      return unavailable("Укажите происхождение внесённых количеств.");
    }
    final var observed =
        LaboratoryStoichiometry.preparative(
            experiment, stage, LaboratoryStoichiometry.Basis.ACTUAL, referenceId);
    final var measured =
        observed.rows().stream()
            .filter(r -> r.component().id().equals(productId))
            .findFirst()
            .orElseThrow();
    final String basis =
        "По внесённой загрузке: "
            + source.molecule().displayName()
            + " · "
            + source.actual().value().stripTrailingZeros().toPlainString()
            + " "
            + source.actual().unit();
    final String provenance = LaboratoryStoichiometry.yieldLabel(observed, measured);
    if (measured.yieldPercent() != null) {
      return result(
          measured.yieldPercent(),
          provenance.equals("Фактический выход") ? "Выход" : provenance,
          "",
          basis);
    }
    // Project only entered quantities into the existing pure-substance estimator. This projection
    // is never stored and cannot borrow any planned input when an actual quantity is absent.
    final var values =
        experiment.components().stream()
            .map(
                c ->
                    new LaboratoryComponent(
                        c.id(),
                        c.molecule(),
                        c.role(),
                        c.actual(),
                        c.actual(),
                        c.evidence(),
                        c.properties()))
            .toList();
    final var projection =
        new LaboratoryExperiment(
            experiment.id(),
            experiment.title(),
            experiment.conditions(),
            experiment.notes(),
            values,
            experiment.journal(),
            experiment.attachments(),
            experiment.stages(),
            experiment.analyses(),
            experiment.samples(),
            experiment.procedure(),
            experiment.route(),
            experiment.operations(),
            experiment.analyticalEvidence(),
            experiment.literature());
    final var estimate =
        LaboratoryStoichiometry.preparative(
            projection, stage, LaboratoryStoichiometry.Basis.PLAN, referenceId, structuralMasses);
    final var row =
        estimate.rows().stream()
            .filter(r -> r.component().id().equals(productId))
            .findFirst()
            .orElseThrow();
    if (row.theoreticalMoles() == null
        || row.theoreticalMoles().signum() <= 0
        || row.amount().moles() == null) {
      return unavailable(
          "Проверьте коэффициенты стадии, молярные массы и свойства веществ.\n"
              + String.join("\n", observed.diagnostics()));
    }
    final BigDecimal percent =
        row.amount()
            .moles()
            .divide(row.theoreticalMoles(), MathContext.DECIMAL128)
            .movePointRight(2);
    return result(
        percent,
        provenance.equals("Фактический выход") ? "Выход" : provenance,
        "Оценка без учёта неизвестной чистоты",
        basis
            + "\nМолярные массы для веществ без заданных свойств рассчитаны по структурам; их чистота не измерена.");
  }

  private static Value result(
      final BigDecimal percent,
      final String label,
      final String qualification,
      final String explanation) {
    final String warning =
        percent.compareTo(new BigDecimal("100")) > 0
            ? "Выше 100 % — проверьте количества и свойства."
            : "";
    return new Value(
        percent,
        label,
        qualification + (qualification.isEmpty() || warning.isEmpty() ? "" : "\n") + warning,
        explanation);
  }

  private static Value unavailable(final String explanation) {
    return new Value(null, "Выход не рассчитан", "", explanation);
  }
}
