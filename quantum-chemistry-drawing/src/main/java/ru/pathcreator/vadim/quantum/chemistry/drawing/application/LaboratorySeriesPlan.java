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
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Independent planned experiments for one selected stage, calculated by the existing quantity core. */
public final class LaboratorySeriesPlan {
  public record Variant(
      String title,
      LaboratoryQuantity reference,
      ReactionConditions conditions,
      Map<String, BigDecimal> equivalents,
      String note) {
    public Variant {
      Objects.requireNonNull(title);
      Objects.requireNonNull(reference);
      Objects.requireNonNull(conditions);
      equivalents = Map.copyOf(equivalents);
      Objects.requireNonNull(note);
    }
  }

  public record Prepared(
      LaboratoryDrawing drawing, LaboratoryStoichiometry.Report theory, List<String> warnings) {
    public Prepared {
      warnings = List.copyOf(warnings);
    }
  }

  private LaboratorySeriesPlan() {}

  /** Scalars use the stage value when present. The UI displays these defaults before any series is written. */
  public static ReactionConditions defaults(
      final LaboratoryDrawing source, final LaboratoryReactionStage stage) {
    final var local = stage.conditions();
    final var common = source.experiment().conditions();
    return ReactionConditions.of(
        local.temperature() == null ? common.temperature() : local.temperature(),
        local.pressure() == null ? common.pressure() : local.pressure(),
        local.duration() == null ? common.duration() : local.duration(),
        local.components().isEmpty() ? common.components() : local.components(),
        local.features().isEmpty() ? common.features() : local.features(),
        local.note() == null ? common.note() : local.note());
  }

  public static Prepared prepare(
      final LaboratoryDrawing source,
      final String stageId,
      final String referenceId,
      final Variant variant,
      final boolean scaleSolvents) {
    if (variant.title().isBlank()) {
      throw new IllegalArgumentException("Назовите вариант опыта.");
    }
    final var stage =
        source.experiment().stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выбранная стадия отсутствует."));
    final Map<String, LaboratoryComponent> components = new LinkedHashMap<>();
    source.experiment().components().forEach(c -> components.put(c.id(), c));
    final var reference = components.get(referenceId);
    if (reference == null
        || stage.participants().stream()
            .noneMatch(
                p ->
                    p.componentId().equals(referenceId)
                        && p.role() == ReactionComponentRole.REACTANT)) {
      throw new IllegalArgumentException("Выберите опорный исходник выбранной стадии.");
    }
    final List<LaboratoryBatchPlan.Target> targets = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    for (final var participant : stage.participants()) {
      if (participant.role() == ReactionComponentRole.PRODUCT
          || participant.componentId().equals(referenceId)) {
        continue;
      }
      final var c = components.get(participant.componentId());
      final var eq = variant.equivalents().get(c.id());
      if (c.planned() == null && eq == null) {
        warnings.add(
            "Не задан план «" + c.molecule().displayName() + "»: количество останется пустым.");
        continue;
      }
      if (eq == null
          && (participant.role() != ReactionComponentRole.SOLVENT || scaleSolvents)
          && reference.planned() == null) {
        throw new IllegalArgumentException(
            "Для пропорционального пересчёта задайте исходный план опорного вещества в шаблоне.");
      }
      targets.add(
          new LaboratoryBatchPlan.Target(
              c.id(),
              eq,
              c.planned() == null ? LaboratoryQuantityUnit.MILLIMOLE : c.planned().unit(),
              null));
    }
    for (final String id : variant.equivalents().keySet()) {
      if (stage.participants().stream()
          .noneMatch(
              p ->
                  p.componentId().equals(id)
                      && !id.equals(referenceId)
                      && p.role() != ReactionComponentRole.PRODUCT
                      && p.role() != ReactionComponentRole.SOLVENT)) {
        throw new IllegalArgumentException(
            "Эквиваленты можно задать только дозируемому участнику, кроме опорного исходника и растворителя.");
      }
    }
    final var batch =
        LaboratoryBatchPlan.calculate(
            source.experiment(), stage, referenceId, variant.reference(), targets, scaleSolvents);
    final List<LaboratoryComponent> planned = new ArrayList<>();
    final Map<String, MoleculeDrawing> layouts = new LinkedHashMap<>();
    for (final var participant : stage.participants()) {
      final var c = components.get(participant.componentId());
      planned.add(
          new LaboratoryComponent(
              c.id(),
              c.molecule(),
              participant.role(),
              participant.role() == ReactionComponentRole.PRODUCT
                  ? null
                  : batch.planned().get(c.id()),
              null,
              LaboratoryEvidence.PLANNED,
              c.properties()));
      layouts.put(c.id(), source.layouts().get(c.id()));
    }
    final var newStage =
        new LaboratoryReactionStage(
            stage.id(),
            stage.title(),
            stage.arrow(),
            variant.conditions(),
            stage.participants(),
            stage.atomMapping(),
            stage.classification());
    final String note =
        "План серии; исходный шаблон: «"
            + source.experiment().title()
            + "» ("
            + source.experiment().id()
            + ").\n"
            + "Включена только стадия «"
            + stage.title()
            + "». Фактические результаты, журнал и вложения шаблона не перенесены.\n"
            + variant.note();
    final var template =
        new LaboratoryExperiment(
            "exp_" + UUID.randomUUID(),
            variant.title(),
            ReactionConditions.EMPTY,
            note,
            planned,
            List.of(),
            List.of(),
            List.of(newStage),
            List.of(),
            List.of());
    final var repeated =
        LaboratoryProcedureService.repeatPlan(source.experiment().procedure(), template, false);
    if (repeated.omittedSteps() > 0) {
      warnings.add(repeated.warning());
    }
    final List<LaboratoryEntry> journal =
        repeated.omittedSteps() == 0
            ? List.of()
            : List.of(
                new LaboratoryEntry(
                    "entry_" + UUID.randomUUID(),
                    java.time.Instant.now(),
                    LaboratoryEntry.Kind.ACTION,
                    repeated.warning(),
                    null));
    final var experiment =
        new LaboratoryExperiment(
            template.id(),
            variant.title(),
            ReactionConditions.EMPTY,
            note,
            planned,
            journal,
            List.of(),
            List.of(newStage),
            List.of(),
            List.of(),
            repeated.procedure(),
            LaboratorySynthesisRoute.EMPTY,
            LaboratoryReactionOperationsService.repeatPlan(
                source.experiment().operations(), template),
            LaboratoryAnalyticalEvidence.EMPTY,
            LaboratoryLiteratureService.withoutAttachmentsForRepeat(
                source.experiment().literature(), Set.of(newStage.id())));
    if (!variant.conditions().components().isEmpty()) {
      warnings.add(
          "Материалы, записанные текстом в условиях, сохранены как условия; их дозировки не пересчитываются. Пересчёт относится к строкам компонентов.");
    }
    final var theory =
        LaboratoryStoichiometry.preparative(
            experiment, newStage, LaboratoryStoichiometry.Basis.PLAN, referenceId);
    return new Prepared(new LaboratoryDrawing(experiment, layouts), theory, warnings);
  }
}
