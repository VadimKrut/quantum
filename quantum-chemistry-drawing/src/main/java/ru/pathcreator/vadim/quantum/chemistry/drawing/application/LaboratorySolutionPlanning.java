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
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;

/** Explicit dilution arithmetic and a single existing-ledger transaction for explicitly recorded preparation. */
public final class LaboratorySolutionPlanning {
  private static final MathContext MC = MathContext.DECIMAL128;

  private LaboratorySolutionPlanning() {}

  public record Row(
      int number,
      String label,
      Concentration target,
      LaboratoryQuantity take,
      LaboratoryQuantity finalVolume,
      LaboratoryQuantity remaining) {}

  public record Plan(List<Row> rows, List<String> warnings) {
    public Plan {
      rows = List.copyOf(rows);
      warnings = List.copyOf(warnings);
    }
  }

  public record Measured(LaboratoryQuantity taken, LaboratoryQuantity finalVolume) {
    public Measured {
      Objects.requireNonNull(taken);
      LaboratorySolutionBook.positive(taken.value());
      LaboratorySolutionBook.volume(finalVolume);
    }
  }

  public record Posted(LaboratoryInventory inventory, List<String> batchIds) {
    public Posted {
      batchIds = List.copyOf(batchIds);
    }
  }

  public static Plan plan(final Recipe recipe) {
    final List<Row> rows = new ArrayList<>();
    final List<DilutionStep> stages = new ArrayList<>();
    stages.add(new DilutionStep(recipe.title(), recipe.target(), recipe.finalVolume()));
    stages.addAll(recipe.series());
    Concentration stock = recipe.stock();
    for (int i = 0; i < stages.size(); i++) {
      final var step = stages.get(i);
      final BigDecimal litres = step.finalVolume().in(LaboratoryQuantityUnit.LITRE).value();
      final LaboratoryQuantity take;
      if (i == 0 && recipe.mode() == Mode.SOLID) {
        BigDecimal grams = step.target().base().multiply(litres);
        if (step.target().unit().family().equals("molar")) {
          grams = grams.multiply(recipe.molarMass());
        }
        take =
            new LaboratoryQuantity(
                grams.multiply(BigDecimal.valueOf(100)).divide(recipe.purityPercent(), MC),
                LaboratoryQuantityUnit.GRAM);
      } else {
        if (!stock.unit().family().equals(step.target().unit().family())) {
          throw new IllegalArgumentException(
              "Исходная и конечная концентрации должны иметь одну основу: молярную или массовую.");
        }
        if (step.target().base().compareTo(stock.base()) > 0) {
          throw new IllegalArgumentException("Разведением нельзя увеличить концентрацию.");
        }
        take =
            new LaboratoryQuantity(
                    step.target().base().multiply(litres).divide(stock.base(), MC),
                    LaboratoryQuantityUnit.LITRE)
                .in(LaboratoryQuantityUnit.MILLILITRE);
        if (i > 0
            && take.in(stages.get(i - 1).finalVolume().unit())
                    .value()
                    .compareTo(stages.get(i - 1).finalVolume().value())
                > 0) {
          throw new IllegalArgumentException(
              "Для следующей ступени недостаточно объёма предыдущего раствора.");
        }
      }
      rows.add(
          new Row(
              i + 1, step.label(), step.target(), take, step.finalVolume(), step.finalVolume()));
      stock = step.target();
    }
    for (int i = 0; i + 1 < rows.size(); i++) {
      final var r = rows.get(i);
      final var remaining =
          new LaboratoryQuantity(
              r.finalVolume()
                  .value()
                  .subtract(rows.get(i + 1).take().in(r.finalVolume().unit()).value()),
              r.finalVolume().unit());
      rows.set(i, new Row(r.number(), r.label(), r.target(), r.take(), r.finalVolume(), remaining));
    }
    return new Plan(
        rows,
        List.of(
            "Доведите раствор до конечного объёма; разница объёмов не является точным объёмом добавляемого растворителя.",
            "Все числа здесь — план. Стандартизация и фактические количества записываются отдельно.",
            "В серийном разведении остаток учитывает отбор на следующую ступень."));
  }

  public static Standardization titrate(
      final String batchId,
      final LaboratoryQuantity primaryMass,
      final BigDecimal molarMass,
      final BigDecimal purityPercent,
      final LaboratoryQuantity titrantVolume,
      final LaboratoryQuantity blank,
      final BigDecimal analytePerStandard,
      final String source,
      final String correction) {
    return titrate(
        batchId,
        primaryMass,
        molarMass,
        purityPercent,
        titrantVolume,
        blank,
        analytePerStandard,
        source,
        correction,
        Origin.UNSPECIFIED);
  }

  public static Standardization titrate(
      final String batchId,
      final LaboratoryQuantity primaryMass,
      final BigDecimal molarMass,
      final BigDecimal purityPercent,
      final LaboratoryQuantity titrantVolume,
      final LaboratoryQuantity blank,
      final BigDecimal analytePerStandard,
      final String source,
      final String correction,
      final Origin origin) {
    Objects.requireNonNull(origin);
    if (!primaryMass.unit().dimension().equals("mass")) {
      throw new IllegalArgumentException("Первичный стандарт задаётся массой.");
    }
    LaboratorySolutionBook.positive(primaryMass.value());
    LaboratorySolutionBook.positive(molarMass);
    LaboratorySolutionBook.positive(purityPercent);
    LaboratorySolutionBook.positive(analytePerStandard);
    if (purityPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException("Чистота больше 100 %.");
    }
    LaboratorySolutionBook.volume(titrantVolume);
    if (blank != null
        && (!blank.unit().dimension().equals("volume")
            || blank.value().precision() > 30
            || Math.abs((long) blank.value().scale()) > 18)) {
      throw new IllegalArgumentException(
          "Холостой опыт задаётся объёмом в допустимом числовом диапазоне.");
    }
    final BigDecimal corrected =
        titrantVolume
            .in(LaboratoryQuantityUnit.LITRE)
            .value()
            .subtract(
                blank == null ? BigDecimal.ZERO : blank.in(LaboratoryQuantityUnit.LITRE).value());
    if (corrected.signum() <= 0) {
      throw new IllegalArgumentException(
          "Объём титрования после вычета холостого опыта должен быть положительным.");
    }
    final BigDecimal moles =
        primaryMass
            .in(LaboratoryQuantityUnit.GRAM)
            .value()
            .multiply(purityPercent)
            .divide(BigDecimal.valueOf(100), MC)
            .divide(molarMass, MC)
            .multiply(analytePerStandard);
    final var result =
        new Concentration(round(moles.divide(corrected, MC)), ConcentrationUnit.MOL_PER_LITRE);
    return new Standardization(
        id("standard"),
        batchId,
        Instant.now(),
        result,
        "Титрование по первичному стандарту",
        source,
        "m = "
            + primaryMass
            + "; M = "
            + molarMass
            + " г/моль; чистота = "
            + purityPercent
            + " %; V = "
            + titrantVolume
            + "; холостой опыт = "
            + Objects.toString(blank, "0 мл")
            + "; моль определяемого / моль стандарта = "
            + analytePerStandard
            + ". C = (m·P/M)·отношение/(V−Vхол).",
        correction,
        origin);
  }

  /** All source uses and receipts are validated as one replacement of materials.qchem by its repository. */
  public static Posted post(
      final LaboratoryInventory inventory,
      final Recipe recipe,
      final List<Measured> actuals,
      final String sourceBatchId,
      final String experimentId,
      final String token,
      final String location,
      final String reason) {
    final var plan = plan(recipe);
    if (actuals.size() != plan.rows().size()
        || reason.isBlank()
        || reason.length() > 10000
        || location.length() > 500) {
      throw new IllegalArgumentException(
          "Введите записываемые количества каждой ступени, основание до 10000 символов и место до 500 символов.");
    }
    final var source =
        inventory.batches().stream()
            .filter(b -> b.id().equals(sourceBatchId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выберите исходную складскую партию."));
    final String dimension = recipe.mode() == Mode.SOLID ? "mass" : "volume";
    if (!source.unit().dimension().equals(dimension)
        || !actuals.getFirst().taken().unit().dimension().equals(dimension)) {
      throw new IllegalArgumentException(
          "Исходная партия и записываемый отбор должны иметь размерность "
              + (recipe.mode() == Mode.SOLID ? "массы." : "объёма."));
    }
    final List<String> batchIds = new ArrayList<>();
    final Instant time = Instant.now();
    LaboratoryInventory next = inventory;
    String from = source.id();
    final var originalMaterial =
        inventory.materials().stream()
            .filter(m -> m.id().equals(source.materialId()))
            .findFirst()
            .orElseThrow();
    if (inventory.operations().stream().anyMatch(o -> o.id().startsWith(token + "_"))) {
      throw new IllegalArgumentException(
          "Эта запись приготовления уже есть в складской истории. Обновите данные перед новым приготовлением.");
    }
    for (int i = 0; i < actuals.size(); i++) {
      final var measured = actuals.get(i);
      if (i > 0 && !measured.taken().unit().dimension().equals("volume")) {
        throw new IllegalArgumentException("Отбор между ступенями задаётся объёмом.");
      }
      final String detail =
          "Приготовление «"
              + recipe.title()
              + "», методика "
              + recipe.id()
              + ", ступень "
              + (i + 1)
              + ". "
              + reason;
      next =
          next.post(
              new LaboratoryStockOperation(
                  token + "_use_" + i,
                  from,
                  time,
                  LaboratoryStockOperation.Kind.USE,
                  measured.taken(),
                  experimentId,
                  null,
                  detail));
      final String materialId = token + "_material_" + i, batchId = token + "_batch_" + i;
      final var material =
          new LaboratoryMaterial(
              materialId,
              plan.rows().get(i).label() + " в " + recipe.solvent(),
              LaboratoryMaterial.Kind.SOLUTION,
              originalMaterial.molecule());
      final var batch =
          new LaboratoryBatch(
              batchId,
              materialId,
              plan.rows().get(i).label(),
              measured.finalVolume().unit(),
              null,
              "План: "
                  + plan.rows().get(i).target()
                  + "; фактическая концентрация не стандартизована",
              detail,
              location,
              null,
              null,
              experimentId,
              new LaboratoryBatchOrigin(List.of(), from, recipe.id()));
      next =
          next.receive(
              material,
              batch,
              new LaboratoryStockOperation(
                  token + "_receipt_" + i,
                  batchId,
                  time,
                  LaboratoryStockOperation.Kind.RECEIPT,
                  measured.finalVolume(),
                  experimentId,
                  null,
                  detail));
      from = batchId;
      batchIds.add(batchId);
    }
    return new Posted(next, batchIds);
  }

  public static String id(final String prefix) {
    return prefix + "_" + UUID.randomUUID();
  }

  private static BigDecimal round(final BigDecimal value) {
    return value.round(new MathContext(15, RoundingMode.HALF_EVEN)).stripTrailingZeros();
  }
}
