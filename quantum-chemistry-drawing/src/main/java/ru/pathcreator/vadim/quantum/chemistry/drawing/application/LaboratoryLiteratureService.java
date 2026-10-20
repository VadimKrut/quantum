/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;

/** Reuses explicitly entered source text as a plan, never as evidence of execution. */
public final class LaboratoryLiteratureService {
  private LaboratoryLiteratureService() {}

  public static LaboratoryLiterature withoutAttachmentsForRepeat(
      final LaboratoryLiterature source, final Set<String> stageIds) {
    final var references =
        source.references().stream()
            .map(
                r ->
                    r.attachmentIds().isEmpty()
                        ? r
                        : new LaboratoryLiterature.Reference(
                            r.id(),
                            r.doi(),
                            r.title(),
                            r.authors(),
                            r.year(),
                            r.url(),
                            r.supplementUrl(),
                            List.of(),
                            r.notes(),
                            "Локальные файлы источника не перенесены при повторении опыта: "
                                + String.join(", ", r.attachmentIds())
                                + "."))
            .toList();
    final var methods =
        source.methods().stream()
            .map(
                m ->
                    m.stageId() == null || stageIds.contains(m.stageId())
                        ? m
                        : new LaboratoryLiterature.Method(
                            m.id(),
                            m.referenceId(),
                            null,
                            m.title(),
                            m.procedureText(),
                            m.literatureYieldPercent(),
                            m.yieldBasis(),
                            m.notes(),
                            "Исходная стадия "
                                + m.stageId()
                                + " не входит в повтор; связь со стадией снята."))
            .toList();
    return new LaboratoryLiterature(references, methods);
  }

  public static List<LaboratoryProcedure.Step> appendMethod(
      final LaboratoryExperiment experiment,
      final LaboratoryLiterature literature,
      final String methodId,
      final String targetStageId) {
    final var method =
        literature.methods().stream()
            .filter(m -> m.id().equals(methodId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выберите методику."));
    if (targetStageId != null
        && experiment.stages().stream().noneMatch(s -> s.id().equals(targetStageId))) {
      throw new IllegalArgumentException("Выберите существующую стадию для методики.");
    }
    final var reference =
        literature.references().stream()
            .filter(r -> r.id().equals(method.referenceId()))
            .findFirst()
            .orElseThrow();
    final String provenance =
        "Литературная методика: "
            + reference.citation()
            + "\nПлан перенесён из источника; проверьте соответствие веществ, масштаба и условий.\n\n";
    final var steps = new ArrayList<>(experiment.procedure().steps());
    final String content = provenance + method.procedureText();
    // Procedure step text is bounded independently. Splitting preserves every supplied character.
    for (int start = 0, index = 1; start < content.length(); start += 16000, index++) {
      final String title =
          method.title().substring(0, Math.min(method.title().length(), 160))
              + (content.length() > 16000 ? " · " + index : "");
      steps.add(
          new LaboratoryProcedure.Step(
              "method_" + UUID.randomUUID(),
              targetStageId,
              LaboratoryProcedure.Kind.OTHER,
              title,
              content.substring(start, Math.min(content.length(), start + 16000)),
              null,
              BigDecimal.ONE,
              0,
              null));
    }
    return List.copyOf(steps);
  }
}
