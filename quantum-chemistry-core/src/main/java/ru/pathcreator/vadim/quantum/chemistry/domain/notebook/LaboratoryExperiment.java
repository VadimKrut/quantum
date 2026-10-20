/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

/** A laboratory notebook allows incomplete plans without inventing products or measurements. */
public record LaboratoryExperiment(
    String id,
    String title,
    ReactionConditions conditions,
    String notes,
    List<LaboratoryComponent> components,
    List<LaboratoryEntry> journal,
    List<LaboratoryAttachment> attachments,
    List<LaboratoryReactionStage> stages,
    List<LaboratoryAnalysis> analyses,
    List<LaboratorySample> samples,
    LaboratoryProcedure procedure,
    LaboratorySynthesisRoute route,
    LaboratoryReactionOperations operations,
    LaboratoryAnalyticalEvidence analyticalEvidence,
    LaboratoryLiterature literature) {
  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments,
      final List<LaboratoryReactionStage> stages,
      final List<LaboratoryAnalysis> analyses,
      final List<LaboratorySample> samples,
      final LaboratoryProcedure procedure,
      final LaboratorySynthesisRoute route,
      final LaboratoryReactionOperations operations,
      final LaboratoryAnalyticalEvidence analyticalEvidence) {
    this(
        id,
        title,
        conditions,
        notes,
        components,
        journal,
        attachments,
        stages,
        analyses,
        samples,
        procedure,
        route,
        operations,
        analyticalEvidence,
        LaboratoryLiterature.EMPTY);
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments,
      final List<LaboratoryReactionStage> stages,
      final List<LaboratoryAnalysis> analyses,
      final List<LaboratorySample> samples,
      final LaboratoryProcedure procedure,
      final LaboratorySynthesisRoute route) {
    this(
        id,
        title,
        conditions,
        notes,
        components,
        journal,
        attachments,
        stages,
        analyses,
        samples,
        procedure,
        route,
        LaboratoryReactionOperations.EMPTY,
        LaboratoryAnalyticalEvidence.EMPTY);
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments,
      final List<LaboratoryReactionStage> stages,
      final List<LaboratoryAnalysis> analyses,
      final List<LaboratorySample> samples,
      final LaboratoryProcedure procedure) {
    this(
        id,
        title,
        conditions,
        notes,
        components,
        journal,
        attachments,
        stages,
        analyses,
        samples,
        procedure,
        LaboratorySynthesisRoute.EMPTY);
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments,
      final List<LaboratoryReactionStage> stages,
      final List<LaboratoryAnalysis> analyses,
      final List<LaboratorySample> samples) {
    this(
        id,
        title,
        conditions,
        notes,
        components,
        journal,
        attachments,
        stages,
        analyses,
        samples,
        LaboratoryProcedure.EMPTY);
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments,
      final List<LaboratoryReactionStage> stages,
      final List<LaboratoryAnalysis> analyses) {
    this(
        id,
        title,
        conditions,
        notes,
        components,
        journal,
        attachments,
        stages,
        analyses,
        List.of());
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments,
      final List<LaboratoryReactionStage> stages) {
    this(id, title, conditions, notes, components, journal, attachments, stages, List.of());
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components,
      final List<LaboratoryEntry> journal,
      final List<LaboratoryAttachment> attachments) {
    this(id, title, conditions, notes, components, journal, attachments, List.of());
  }

  public LaboratoryExperiment(
      final String id,
      final String title,
      final ReactionConditions conditions,
      final String notes,
      final List<LaboratoryComponent> components) {
    this(id, title, conditions, notes, components, List.of(), List.of());
  }

  public LaboratoryExperiment {
    id = IdentifierValue.requireIdentifier(id, "Laboratory experiment id");
    title = TextValue.requireText(title, "Laboratory experiment title");
    Objects.requireNonNull(conditions, "Experiment conditions");
    Objects.requireNonNull(notes, "Experiment notes");
    components = List.copyOf(components);
    journal = List.copyOf(journal);
    attachments = List.copyOf(attachments);
    stages = List.copyOf(stages);
    analyses = List.copyOf(analyses);
    samples = List.copyOf(samples);
    LaboratorySampleLedger.validate(samples);
    Objects.requireNonNull(procedure, "Методика опыта");
    Objects.requireNonNull(route, "Маршрут синтеза");
    Objects.requireNonNull(operations, "Контроль и обработка реакции");
    Objects.requireNonNull(analyticalEvidence, "Аналитические данные");
    analyticalEvidence.validateLinks(samples, analyses, attachments);
    operations.validateLinks(samples, analyses, analyticalEvidence);
    Objects.requireNonNull(literature, "Литература");
    literature.validateLinks(stages, attachments);
    for (final var sample : samples) {
      if (sample.origin() != null && sample.origin().source().experimentId().equals(id)) {
        throw new IllegalArgumentException(
            "Полученный образец должен ссылаться на другой исходный опыт.");
      }
    }
    if (analyses.stream()
            .mapToLong(
                a ->
                    a.data() instanceof LaboratoryAnalysis.Nmr n && n.trace() != null
                        ? n.trace().real().size()
                        : 0)
            .sum()
        > 524288) {
      throw new IllegalArgumentException(
          "В одном опыте поддерживается до 524288 исходных точек ЯМР с учётом истории исправлений.");
    }
    final HashSet<String> componentIds = new HashSet<>();
    for (final LaboratoryComponent component : components) {
      if (!componentIds.add(component.id())) {
        throw new IllegalArgumentException("Duplicate laboratory component id.");
      }
    }
    final HashSet<String> stageIds = new HashSet<>();
    for (final var stage : stages) {
      if (!stageIds.add(stage.id())) {
        throw new IllegalArgumentException("Duplicate reaction stage id.");
      }
      for (final var participant : stage.participants()) {
        if (!componentIds.contains(participant.componentId())) {
          throw new IllegalArgumentException(
              "Компонент используется в стадии «"
                  + stage.title()
                  + "». Сначала измените состав этой стадии.");
        }
      }
    }
    final HashSet<String> entryIds = new HashSet<>();
    final HashSet<String> recordedSteps = new HashSet<>();
    for (final var event : procedure.events()) {
      recordedSteps.add(event.stepId());
    }
    for (final var step : procedure.steps()) {
      if (recordedSteps.contains(step.id())) {
        continue;
      }
      if (step.componentId() != null && !componentIds.contains(step.componentId())) {
        throw new IllegalArgumentException(
            "Компонент используется в действии «" + step.title() + "». Сначала измените методику.");
      }
      if (step.stageId() != null && !stageIds.contains(step.stageId())) {
        throw new IllegalArgumentException(
            "Стадия используется в действии «" + step.title() + "». Сначала измените методику.");
      }
      if (step.componentId() != null
          && step.stageId() != null
          && stages.stream()
              .filter(stage -> stage.id().equals(step.stageId()))
              .noneMatch(
                  stage ->
                      stage.participants().stream()
                          .anyMatch(
                              participant ->
                                  participant.componentId().equals(step.componentId())))) {
        throw new IllegalArgumentException(
            "Компонент действия «" + step.title() + "» не входит в выбранную стадию.");
      }
    }
    for (final LaboratoryEntry entry : journal) {
      if (entry.correctsId() != null && !entryIds.contains(entry.correctsId())) {
        throw new IllegalArgumentException(
            "Corrected journal entry does not exist before correction.");
      }
      if (!entryIds.add(entry.id())) {
        throw new IllegalArgumentException("Duplicate journal entry id.");
      }
    }
    final HashSet<String> attachmentIds = new HashSet<>();
    for (final LaboratoryAttachment attachment : attachments) {
      if (!attachmentIds.add(attachment.id())) {
        throw new IllegalArgumentException("Duplicate attachment id.");
      }
    }
    final HashSet<String> analysisIds = new HashSet<>();
    for (final LaboratoryAnalysis analysis : analyses) {
      if (analysis.correctsId() != null && !analysisIds.contains(analysis.correctsId())) {
        throw new IllegalArgumentException(
            "Исправляемая аналитическая запись должна существовать раньше исправления.");
      }
      if (analysis.correctsId() != null
          && analyses.stream()
              .filter(a -> a.id().equals(analysis.correctsId()))
              .anyMatch(a -> !a.componentId().equals(analysis.componentId()))) {
        throw new IllegalArgumentException(
            "Исправление должно относиться к тому же исходному компоненту.");
      }

      if (analysis.sampleId() != null) {
        final var sample =
            samples.stream()
                .filter(s -> s.id().equals(analysis.sampleId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException("Анализ ссылается на отсутствующий образец."));
        if (!sample.componentId().equals(analysis.componentId())
            || !LaboratoryMaterialProperties.signature(sample.molecule())
                .equals(LaboratoryMaterialProperties.signature(analysis.molecule()))) {
          throw new IllegalArgumentException(
              "Снимок структуры анализа не соответствует связанному образцу.");
        }
      }
      if (analysis.correctsId() != null
          && analyses.stream()
              .filter(a -> a.id().equals(analysis.correctsId()))
              .anyMatch(a -> !Objects.equals(a.sampleId(), analysis.sampleId()))) {
        throw new IllegalArgumentException("Исправление должно относиться к тому же образцу.");
      }
      if (!analysisIds.add(analysis.id())) {
        throw new IllegalArgumentException("Duplicate analysis id.");
      }
      if (!attachmentIds.containsAll(analysis.attachmentIds())) {
        throw new IllegalArgumentException("Анализ ссылается на отсутствующее вложение.");
      }
    }
  }
}
