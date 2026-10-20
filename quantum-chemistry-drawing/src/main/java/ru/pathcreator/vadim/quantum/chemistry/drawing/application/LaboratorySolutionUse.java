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
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;

/** A selected batch's explicitly sourced solute concentration becomes c in n=cV exactly once. */
public final class LaboratorySolutionUse {
  private LaboratorySolutionUse() {}

  public record Prepared(
      LaboratoryBatch batch,
      LaboratorySolutionBook.Standardization standard,
      LaboratoryMaterialProperties properties,
      String explanation) {}

  public static Prepared prepare(
      final LaboratoryInventory inventory,
      final LaboratorySolutionBook book,
      final String batchId,
      final LaboratoryComponent component) {
    final var batch =
        inventory.batches().stream()
            .filter(b -> b.id().equals(batchId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Партия отсутствует."));
    if (!batch.unit().dimension().equals("volume"))
      throw new IllegalArgumentException(
          "Для концентрации раствора выберите партию, учитываемую объёмом.");
    final var material =
        inventory.materials().stream()
            .filter(m -> m.id().equals(batch.materialId()))
            .findFirst()
            .orElseThrow();
    if (material.molecule() == null
        || MolecularGraphEquivalence.compare(material.molecule(), component.molecule())
            != MolecularGraphEquivalence.Result.EQUIVALENT)
      throw new IllegalArgumentException(
          "Структура вещества в партии должна соответствовать компоненту загрузки.");
    final var standard =
        book.latest(batchId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Для выбранной партии нет известной концентрации. Запишите её с единицами и источником в разделе «Растворы → Стандартизация»."));
    final var old =
        component.properties().matches(component.molecule())
            ? component.properties()
            : LaboratoryMaterialProperties.EMPTY;
    BigDecimal molarity = standard.measured().base();
    String conversion = "Молярная концентрация приведена к моль/л.";
    if (standard.measured().unit().family().equals("mass")) {
      if (old.molarMass() == null
          || old.molarMass().signum() <= 0
          || old.molarMass().precision() > 30
          || Math.abs((long) old.molarMass().scale()) > 18)
        throw new IllegalArgumentException(
            "Для перевода массовой концентрации в моль/л задайте подтверждённую молярную массу компонента.");
      molarity = molarity.divide(old.molarMass(), MathContext.DECIMAL128);
      conversion =
          "c = массовая концентрация / M; M = " + old.molarMass().toPlainString() + " г/моль.";
    }
    molarity = molarity.round(new MathContext(15)).stripTrailingZeros();
    final String source =
        "Партия «"
            + batch.label()
            + "» ["
            + batch.id()
            + "]. Концентрация "
            + standard.measured()
            + "; происхождение: "
            + standard.origin()
            + "; запись "
            + standard.id()
            + " от "
            + standard.time()
            + "; "
            + standard.method()
            + "; источник: "
            + standard.source()
            + (old.empty() ? "" : ". Остальные свойства: " + old.source());
    final var properties =
        new LaboratoryMaterialProperties(
            old.molarMass(),
            old.density(),
            old.purityPercent(),
            molarity,
            source,
            LaboratoryMaterialProperties.signature(component.molecule()));
    return new Prepared(
        batch,
        standard,
        properties,
        source
            + "\n"
            + conversion
            + "\nДля объёма используется n = c × V. Чистота навески повторно не умножается; массовые/объёмные проценты не угадываются.");
  }

  public static LaboratoryQuantity plannedVolume(
      final LaboratoryComponent component, final Prepared prepared) {
    final var plan = component.planned();
    if (plan == null)
      throw new IllegalArgumentException(
          "Для пересчёта объёма сначала задайте плановое количество компонента.");
    if (plan.value().precision() > 40 || Math.abs((long) plan.value().scale()) > 24)
      throw new IllegalArgumentException("Плановое количество выходит за пределы расчёта.");
    if (plan.unit().dimension().equals("volume")) return plan.in(prepared.batch().unit());
    final var moles = LaboratoryBatchPlan.moles(component, plan);
    final var withConcentration =
        new LaboratoryComponent(
            component.id(),
            component.molecule(),
            component.role(),
            plan,
            component.actual(),
            component.evidence(),
            prepared.properties());
    return LaboratoryBatchPlan.fromMoles(withConcentration, moles, prepared.batch().unit());
  }

  /** An old portable link may match both namespaces: return both originals explicitly. */
  public static String sourceAnalyses(
      final LaboratoryExperiment experiment, final LaboratorySampleOrigin origin) {
    if (!experiment.id().equals(origin.source().experimentId())
        || experiment.samples().stream().noneMatch(s -> s.id().equals(origin.source().sampleId())))
      throw new IllegalArgumentException("Файл не содержит указанный исходный образец.");
    final StringBuilder text =
        new StringBuilder(origin.sourceTitle() + " · " + origin.sourceCode() + "\n\n");
    for (final String id : origin.analysisIds()) {
      final var legacy =
          experiment.analyses().stream()
              .filter(a -> a.id().equals(id) && origin.source().sampleId().equals(a.sampleId()))
              .findFirst();
      final var advanced =
          experiment.analyticalEvidence().entries().stream()
              .filter(a -> a.id().equals(id) && origin.source().sampleId().equals(a.sampleId()))
              .findFirst();
      if (legacy.isPresent())
        text.append("Анализ · ")
            .append(id)
            .append("\n")
            .append(LaboratoryAnalysisSummary.text(legacy.get()))
            .append("\n\n");
      if (advanced.isPresent())
        text.append("Расширенная аналитика · ")
            .append(id)
            .append("\n")
            .append(LaboratoryEvidenceService.description(advanced.get()))
            .append("\n\n");
      if (legacy.isEmpty() && advanced.isEmpty())
        text.append("Связанная запись отсутствует: ").append(id).append("\n\n");
    }
    if (origin.analysisIds().isEmpty())
      text.append("На момент передачи связанных анализов не было.");
    return text.toString();
  }

  /** Follow typed batch ancestry to the physical sample sources, preserving their original analysis links. */
  public static List<LaboratorySampleOrigin> origins(
      final LaboratoryInventory inventory, final String batchId) {
    final var values = new LinkedHashMap<String, LaboratorySampleOrigin>();
    String id = batchId;
    final Set<String> visited = new HashSet<>();
    while (id != null) {
      if (!visited.add(id)) throw new IllegalArgumentException("Цикл происхождения партий.");
      final String current = id;
      final var batch =
          inventory.batches().stream()
              .filter(b -> b.id().equals(current))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Исходная партия отсутствует."));
      final var origin = batch.origin();
      if (origin == null) break;
      origin.sourceSamples().forEach(s -> values.putIfAbsent(s.source().token(), s));
      id = origin.parentBatchId();
    }
    return List.copyOf(values.values());
  }

  public static String provenance(final LaboratoryInventory inventory, final String batchId) {
    final var origins = origins(inventory, batchId);
    if (origins.isEmpty()) return "Происхождение от физического образца не задано.";
    return String.join(
        "\n",
        origins.stream()
            .map(
                o ->
                    o.sourceTitle()
                        + " · "
                        + o.sourceCode()
                        + " · исходных анализов: "
                        + o.analysisIds().size()
                        + "\n"
                        + o.sourcePathHint()
                        + "\n"
                        + o.source().token())
            .toList());
  }
}
