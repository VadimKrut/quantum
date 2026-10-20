/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.*;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryDispensingPlan.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Explicit c=n/V or mass concentration, and proportional composition checks before any pooling. */
public final class LaboratoryDispensingService {
  private static final MathContext MC = MathContext.DECIMAL128;
  private static final BigDecimal HUNDRED = new BigDecimal("100"),
      ROUNDING = new BigDecimal("1E-10");

  private LaboratoryDispensingService() {}

  public record Dose(
      String componentId,
      LaboratoryQuantity planned,
      LaboratoryQuantity dispense,
      boolean common,
      String explanation) {}

  public record Vial(
      String runId, String position, LaboratoryQuantity commonAliquot, List<Dose> additions) {
    public Vial {
      additions = List.copyOf(additions);
    }
  }

  public record Total(
      String componentId,
      String label,
      LaboratoryQuantity required,
      LaboratoryQuantity prepare,
      boolean common) {}

  public record Result(
      List<Vial> vials,
      List<Total> totals,
      LaboratoryQuantity distributed,
      LaboratoryQuantity reserve,
      LaboratoryQuantity dead,
      LaboratoryQuantity finalVolume,
      List<String> blockers,
      List<String> warnings,
      String instructions) {
    public Result {
      vials = List.copyOf(vials);
      totals = List.copyOf(totals);
      blockers = List.copyOf(blockers);
      warnings = List.copyOf(warnings);
    }

    public boolean ready() {
      return blockers.isEmpty();
    }
  }

  public static LaboratoryDispensingPlan capture(
      final LaboratoryDrawing source,
      final LaboratoryExperimentalDesign design,
      final List<LaboratorySeriesPlan.Prepared> prepared,
      final String designHash) {
    if (prepared.size() != design.runs().size() || prepared.isEmpty())
      throw new IllegalArgumentException("Сначала рассчитайте всю текущую матрицу исследования.");
    final var stage =
        source.experiment().stages().stream()
            .filter(s -> s.id().equals(design.stageId()))
            .findFirst()
            .orElseThrow();
    final List<Ingredient> ingredients = new ArrayList<>();
    for (final var p : stage.participants()) {
      if (p.role() != ReactionComponentRole.PRODUCT)
        ingredients.add(new Ingredient(p.componentId(), false, null, "", "", ""));
    }
    final Map<String, LaboratoryComponent> originals = components(source);
    final List<Run> runs = new ArrayList<>();
    for (int i = 0; i < prepared.size(); i++) {
      final var run = design.runs().get(i);
      final var experiment = prepared.get(i).drawing().experiment();
      final Map<String, LaboratoryQuantity> amounts = new LinkedHashMap<>();
      for (final var input : ingredients) {
        final var component =
            experiment.components().stream()
                .filter(c -> c.id().equals(input.componentId()))
                .findFirst()
                .orElseThrow();
        final var original = originals.get(component.id());
        if (component.actual() != null
            || component.evidence() != LaboratoryEvidence.PLANNED
            || !component.molecule().equals(original.molecule())
            || !component.properties().equals(original.properties()))
          throw new IllegalArgumentException(
              "Нужны проверенные плановые опыты с теми же структурами и свойствами шаблона.");
        if (component.planned() != null) amounts.put(component.id(), component.planned());
      }
      runs.add(new Run(run.id(), run.position(), true, amounts));
    }
    return new LaboratoryDispensingPlan(
        "dispense_" + UUID.randomUUID(),
        design.title() + " · раздача",
        Instant.now(),
        design.id(),
        designHash,
        runs,
        ingredients,
        runs.getFirst().id(),
        null,
        BigDecimal.ZERO,
        new LaboratoryQuantity(BigDecimal.ZERO, LaboratoryQuantityUnit.MILLILITRE),
        "",
        "",
        "");
  }

  public static void validate(
      final LaboratoryDrawing source,
      final LaboratoryExperimentalDesign design,
      final LaboratoryDispensingPlan plan) {
    if (!plan.designId().equals(design.id()) || plan.runs().size() != design.runs().size())
      throw new IllegalArgumentException("План раздачи относится к другому исследованию.");
    final var stage =
        source.experiment().stages().stream()
            .filter(s -> s.id().equals(design.stageId()))
            .findFirst()
            .orElseThrow();
    final Set<String> expected = new HashSet<>();
    stage.participants().stream()
        .filter(p -> p.role() != ReactionComponentRole.PRODUCT)
        .forEach(p -> expected.add(p.componentId()));
    if (!expected.equals(
        new HashSet<>(plan.ingredients().stream().map(Ingredient::componentId).toList())))
      throw new IllegalArgumentException(
          "Состав раздачи отличается от выбранной стадии исследования.");
    for (int i = 0; i < plan.runs().size(); i++) {
      final var p = plan.runs().get(i);
      final var r = design.runs().get(i);
      if (!p.id().equals(r.id()) || !p.position().equals(r.position()))
        throw new IllegalArgumentException(
            "Позиции раздачи не соответствуют замороженной матрице исследования.");
    }
  }

  public static Result calculate(
      final LaboratoryDrawing source,
      final LaboratoryExperimentalDesign design,
      final LaboratoryDispensingPlan plan) {
    validate(source, design, plan);
    final var components = components(source);
    final var chosen = plan.runs().stream().filter(Run::included).toList();
    final var common = plan.ingredients().stream().filter(Ingredient::common).toList();
    final List<String> blockers = new ArrayList<>(), warnings = new ArrayList<>();
    final Map<String, List<Dose>> doses = new LinkedHashMap<>();
    if (chosen.isEmpty()) blockers.add("Выберите хотя бы один запуск для раздачи.");
    for (final var run : plan.runs()) {
      final List<Dose> row = new ArrayList<>();
      for (final var input : plan.ingredients()) {
        final var component = components.get(input.componentId());
        final var quantity = run.amounts().get(input.componentId());
        LaboratoryQuantity dispense = null;
        String explanation = "";
        try {
          if (quantity == null) throw new IllegalArgumentException("плановое количество не задано");
          LaboratoryDispensingPlan.calculatedQuantity(quantity);
          if (input.stock() == null) {
            dispense = quantity;
            explanation = "Непосредственная добавка в исходной единице плана.";
          } else {
            final var volume = stockVolume(component, quantity, input);
            dispense = volume.quantity();
            explanation = volume.explanation();
          }
        } catch (final IllegalArgumentException error) {
          explanation = error.getMessage();
          if (run.included())
            blockers.add(
                run.position() + " · " + component.molecule().displayName() + ": " + explanation);
        }
        row.add(new Dose(component.id(), quantity, dispense, input.common(), explanation));
      }
      doses.put(run.id(), row);
    }
    final Map<String, BigDecimal> factors = new LinkedHashMap<>();
    BigDecimal distributed = BigDecimal.ZERO,
        referenceVolume = BigDecimal.ZERO,
        finalVolume = BigDecimal.ZERO,
        reserve = BigDecimal.ZERO,
        dead = BigDecimal.ZERO;
    if (!common.isEmpty()) {
      final var reference =
          plan.runs().stream()
              .filter(r -> r.id().equals(plan.referenceRunId()))
              .findFirst()
              .orElseThrow();
      referenceVolume = plan.referenceAliquot().in(LaboratoryQuantityUnit.LITRE).value();
      final var referenceDoses = doses.get(reference.id());
      final Map<String, BigDecimal> referenceStocks = new HashMap<>();
      for (final var input : common) {
        final var dose = find(referenceDoses, input.componentId());
        if (dose.dispense() == null || dose.dispense().value().signum() <= 0)
          blockers.add(
              "Опорная позиция "
                  + reference.position()
                  + " должна иметь положительное количество каждого компонента общей смеси.");
        else referenceStocks.put(input.componentId(), dose.dispense().baseValue());
      }
      if (referenceStocks.size() == common.size()) {
        final BigDecimal summed =
            referenceStocks.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (referenceVolume.compareTo(summed) < 0)
          blockers.add(
              "Объём опорной аликвоты меньше суммы отбираемых исходных растворов ("
                  + format(ml(summed))
                  + " мл). Для этого способа приготовления задайте больший конечный объём.");
        final String anchor = common.getFirst().componentId();
        for (final var run : chosen) {
          final var row = doses.get(run.id());
          final var anchorDose = find(row, anchor);
          if (anchorDose.dispense() == null) continue;
          final BigDecimal factor =
              anchorDose.dispense().baseValue().divide(referenceStocks.get(anchor), MC);
          boolean match = true;
          for (final var input : common) {
            final var dose = find(row, input.componentId());
            if (dose.dispense() == null) {
              match = false;
              continue;
            }
            final var actual = dose.dispense().baseValue();
            final var expected = referenceStocks.get(input.componentId()).multiply(factor, MC);
            if (!close(actual, expected)) {
              match = false;
              blockers.add(
                  run.position()
                      + " · "
                      + components.get(input.componentId()).molecule().displayName()
                      + ": относительный состав отличается; этот компонент нельзя включить в одну общую смесь для выбранных запусков.");
            }
          }
          if (match) {
            factors.put(run.id(), factor);
            distributed = distributed.add(referenceVolume.multiply(factor, MC), MC);
          }
        }
      }
      reserve = distributed.multiply(plan.reservePercent()).divide(HUNDRED, MC);
      dead = plan.deadVolume().in(LaboratoryQuantityUnit.LITRE).value();
      finalVolume = distributed.add(reserve).add(dead);
      if (chosen.stream().allMatch(r -> factors.containsKey(r.id())) && distributed.signum() == 0)
        blockers.add("Все выбранные аликвоты нулевые: общая смесь для них не нужна.");
      warnings.add(
          "Пропорциональность проверена по количествам с относительным допуском 10⁻¹⁰ только для численного округления. Это не оценка химической совместимости.");
      warnings.add(
          "Растворитель общей смеси: "
              + plan.diluent()
              + ". Довести до конечного объёма; разница с суммой отборов не является точным объёмом добавляемого растворителя.");
    }
    final List<Vial> vials = new ArrayList<>();
    for (final var run : chosen) {
      vials.add(
          new Vial(
              run.id(),
              run.position(),
              common.isEmpty()
                  ? null
                  : factors.containsKey(run.id())
                      ? volume(referenceVolume.multiply(factors.get(run.id()), MC))
                      : null,
              doses.get(run.id())));
    }
    final List<Total> totals = new ArrayList<>();
    for (final var input : plan.ingredients()) {
      LaboratoryQuantity sum = null;
      for (final var vial : vials) {
        final var q = find(vial.additions(), input.componentId()).dispense();
        if (q == null) continue;
        if (sum == null) sum = q;
        else {
          sum = new LaboratoryQuantity(sum.value().add(q.in(sum.unit()).value()), sum.unit());
        }
      }
      if (sum != null) {
        LaboratoryQuantity prepare = sum;
        if (input.common() && distributed.signum() > 0) {
          prepare =
              new LaboratoryQuantity(
                  sum.value().multiply(finalVolume.divide(distributed, MC), MC), sum.unit());
        }
        totals.add(
            new Total(
                input.componentId(),
                input.stock() == null
                    ? components.get(input.componentId()).molecule().displayName()
                    : input.label(),
                sum,
                prepare,
                input.common()));
      }
    }
    if (plan.ingredients().stream().anyMatch(i -> i.stock() != null)) {
      warnings.add(
          "Концентрации относятся к активному веществу. Объёмы растворителей в опытах не уменьшаются автоматически на растворители исходных растворов и общей смеси; проверьте концентрацию реакции.");
    }
    if (design.factors().stream()
        .anyMatch(f -> f.binding() == LaboratoryExperimentalDesign.Binding.CONDITION_NOTE))
      warnings.add(
          "Текстовые факторы условий не меняют вещества автоматически. Проверьте указанные растворители и реагенты перед приготовлением.");
    warnings.add(
        "Резерв и мёртвый объём включены только в общую смесь; индивидуальные добавки рассчитаны без запаса.");
    warnings.add(
        "Все количества — план. Фактическая загрузка, приготовленные партии и расход склада здесь не регистрируются.");
    final String instructions =
        instructions(
            plan,
            components,
            vials,
            totals,
            volume(distributed),
            volume(reserve),
            volume(dead),
            volume(finalVolume),
            blockers,
            warnings);
    return new Result(
        vials,
        totals,
        volume(distributed),
        volume(reserve),
        volume(dead),
        volume(finalVolume),
        blockers,
        warnings,
        instructions);
  }

  private record Conversion(LaboratoryQuantity quantity, String explanation) {}

  private static Conversion stockVolume(
      final LaboratoryComponent original,
      final LaboratoryQuantity quantity,
      final Ingredient ingredient) {
    if (quantity.value().signum() == 0)
      return new Conversion(volume(BigDecimal.ZERO), "Нулевой план — отбор не требуется.");
    final var properties = original.properties();
    for (final BigDecimal p :
        Arrays.asList(
            properties.molarMass(),
            properties.purityPercent(),
            properties.density(),
            properties.molarity())) {
      if (p != null) LaboratoryDispensingPlan.calculatedNumber(p);
    }
    final var component =
        new LaboratoryComponent(
            original.id(),
            original.molecule(),
            original.role(),
            quantity,
            null,
            LaboratoryEvidence.PLANNED,
            properties);
    final boolean molar = ingredient.stock().unit().family().equals("molar");
    BigDecimal required;
    String basis;
    if (!molar && quantity.unit().dimension().equals("mass")) {
      if (!properties.matches(original.molecule()) || properties.purityPercent() == null)
        throw new IllegalArgumentException(
            "для массовой концентрации требуется подтверждённая массовая доля вещества в навеске; 100 % не предполагается");
      required =
          quantity
              .in(LaboratoryQuantityUnit.GRAM)
              .value()
              .multiply(properties.purityPercent())
              .divide(HUNDRED, MC);
      basis = "масса активного вещества = навеска × массовая доля";
    } else {
      final var amount =
          LaboratoryStoichiometry.amount(component, LaboratoryStoichiometry.Basis.PLAN);
      if (amount.moles() == null) throw new IllegalArgumentException(amount.explanation());
      required = amount.moles();
      basis = amount.explanation();
      if (!molar) {
        if (!properties.matches(original.molecule()) || properties.molarMass() == null)
          throw new IllegalArgumentException(
              "для массовой концентрации нужна подтверждённая молярная масса");
        required = required.multiply(properties.molarMass(), MC);
      }
    }
    final BigDecimal litres = required.divide(ingredient.stock().base(), MC);
    return new Conversion(
        volume(litres),
        basis
            + "; V = "
            + (molar ? "n/c" : "m/γ")
            + "; исходный раствор «"
            + ingredient.label()
            + "»: "
            + ingredient.stock()
            + "; "
            + ingredient.source());
  }

  private static boolean close(final BigDecimal a, final BigDecimal b) {
    if (a.compareTo(b) == 0) return true;
    final BigDecimal scale = a.abs().max(b.abs());
    return scale.signum() > 0 && a.subtract(b).abs().compareTo(scale.multiply(ROUNDING)) <= 0;
  }

  private static Dose find(final List<Dose> values, final String id) {
    return values.stream().filter(v -> v.componentId().equals(id)).findFirst().orElseThrow();
  }

  private static Map<String, LaboratoryComponent> components(final LaboratoryDrawing source) {
    final Map<String, LaboratoryComponent> result = new LinkedHashMap<>();
    source.experiment().components().forEach(c -> result.put(c.id(), c));
    return result;
  }

  private static LaboratoryQuantity volume(final BigDecimal litres) {
    return new LaboratoryQuantity(
        litres.multiply(new BigDecimal("1000")), LaboratoryQuantityUnit.MILLILITRE);
  }

  private static BigDecimal ml(final BigDecimal litres) {
    return litres.multiply(new BigDecimal("1000"));
  }

  public static String format(final BigDecimal value) {
    final var shown = value.round(new MathContext(5)).stripTrailingZeros();
    return (shown.compareTo(value) == 0 ? "" : "≈") + shown.toPlainString();
  }

  public static String label(final LaboratoryQuantity quantity) {
    if (quantity == null) return "Не рассчитано";
    final var q =
        quantity.unit().dimension().equals("volume")
            ? quantity.in(
                quantity.baseValue().abs().compareTo(new BigDecimal("0.001")) < 0
                    ? LaboratoryQuantityUnit.MICROLITRE
                    : LaboratoryQuantityUnit.MILLILITRE)
            : quantity;
    return format(q.value()) + " " + q.unit();
  }

  private static String instructions(
      final LaboratoryDispensingPlan plan,
      final Map<String, LaboratoryComponent> components,
      final List<Vial> vials,
      final List<Total> totals,
      final LaboratoryQuantity distributed,
      final LaboratoryQuantity reserve,
      final LaboratoryQuantity dead,
      final LaboratoryQuantity finalVolume,
      final List<String> blockers,
      final List<String> warnings) {
    final StringBuilder out = new StringBuilder(plan.title()).append("\n");
    if (!blockers.isEmpty()) {
      out.append("ПЛАН НЕ ГОТОВ К ПРИГОТОВЛЕНИЮ\n");
      blockers.forEach(b -> out.append("• ").append(b).append("\n"));
      out.append("\nРасчёт ниже частичный; общую смесь не готовить по этим значениям.\n");
    }
    if (plan.ingredients().stream().anyMatch(Ingredient::common)) {
      out.append("\nОбщая смесь\nОснование совместного приготовления: ")
          .append(plan.compatibilityBasis())
          .append("\n");
      totals.stream()
          .filter(Total::common)
          .forEach(
              t ->
                  out.append("Отобрать ")
                      .append(label(t.prepare()))
                      .append(" раствора «")
                      .append(t.label())
                      .append("».\n"));
      out.append("Довести растворителем «")
          .append(plan.diluent())
          .append("» до ")
          .append(label(finalVolume))
          .append(" и обеспечить однородность по принятой методике.\nРаздача ")
          .append(label(distributed))
          .append(" + резерв ")
          .append(label(reserve))
          .append(" + мёртвый объём ")
          .append(label(dead))
          .append(".\n");
    }
    out.append("\nРаздача по позициям\n");
    for (final var vial : vials) {
      out.append(vial.position()).append(" · ").append(vial.runId()).append(": ");
      if (vial.commonAliquot() != null)
        out.append("общая смесь ").append(label(vial.commonAliquot())).append("; ");
      for (final var dose : vial.additions()) {
        if (!dose.common())
          out.append(components.get(dose.componentId()).molecule().displayName())
              .append(" ")
              .append(label(dose.dispense()))
              .append("; ");
      }
      out.append("\n");
    }
    out.append("\n");
    warnings.forEach(w -> out.append("• ").append(w).append("\n"));
    if (!plan.note().isBlank()) out.append("\n").append(plan.note());
    return out.toString();
  }
}
