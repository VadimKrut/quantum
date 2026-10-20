/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** A document is reviewed before any mutation; unsupported fragments remain explicit. */
public interface ChemDrawExchange {
  record Fragment(
      String id,
      String title,
      StructureExchange.Entry entry,
      Map<String, AtomId> atomIds,
      String problem) {
    public Fragment {
      atomIds = Map.copyOf(atomIds);
    }

    public boolean supported() {
      return this.entry != null;
    }
  }

  record Member(String fragmentId, ReactionComponentRole role, Map<AtomId, Integer> atomNumbers) {
    public Member {
      atomNumbers = Map.copyOf(atomNumbers);
    }
  }

  record Step(
      String id,
      String title,
      List<Member> members,
      LaboratoryReactionStage.Arrow arrow,
      String note,
      String problem) {
    public Step {
      members = List.copyOf(members);
    }

    public boolean supported() {
      return this.problem.isEmpty();
    }
  }

  record Document(List<Fragment> fragments, List<Step> steps, List<String> notes) {
    public Document {
      fragments = List.copyOf(fragments);
      steps = List.copyOf(steps);
      notes = List.copyOf(notes);
    }
  }

  record Export(String text, List<String> notes) {
    public Export {
      notes = List.copyOf(notes);
    }
  }

  Document read(byte[] bytes, String source);

  Export write(LaboratoryDrawing drawing, Set<String> components, Set<String> stages);

  /** One immutable edit for structures, stages, names, provenance and the original attachment. */
  static LaboratoryDrawing importDocument(
      final LaboratoryDrawing original,
      final Document document,
      final Map<String, ReactionComponentRole> selected,
      final Set<String> stageIds,
      final Map<String, String> names,
      final LaboratoryAttachment attachment) {
    final var chosen = new LinkedHashMap<>(selected);
    final Map<String, Fragment> fragments = new LinkedHashMap<>();
    document
        .fragments()
        .forEach(
            f -> {
              if (fragments.put(f.id(), f) != null) {
                throw new IllegalArgumentException("Повторный идентификатор фрагмента.");
              }
            });
    final var steps = document.steps().stream().filter(s -> stageIds.contains(s.id())).toList();
    if (steps.size() != stageIds.size()) {
      throw new IllegalArgumentException("Выбранная стадия отсутствует.");
    }
    for (final var s : steps) {
      if (!s.supported()) {
        throw new IllegalArgumentException(s.problem());
      }
      s.members().forEach(m -> chosen.putIfAbsent(m.fragmentId(), m.role()));
    }
    if (chosen.isEmpty() || chosen.size() > 100) {
      throw new IllegalArgumentException("Выберите от 1 до 100 веществ или стадии с участниками.");
    }
    final LaboratoryEditor draft = new LaboratoryEditor();
    draft.open(original);
    final var drawings =
        new ArrayList<
            ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing>();
    final var roles = new ArrayList<ReactionComponentRole>();
    for (final var e : chosen.entrySet()) {
      final var f = fragments.get(e.getKey());
      if (f == null || !f.supported()) {
        throw new IllegalArgumentException("Не удалось перенести фрагмент " + e.getKey());
      }
      drawings.add(
          new MoleculeSketchService()
              .rename(f.entry().drawing(), names.getOrDefault(f.id(), f.title())));
      roles.add(Objects.requireNonNull(e.getValue()));
    }
    final var ids = draft.importStructures(drawings, roles);
    final Map<String, String> components = new LinkedHashMap<>();
    int index = 0;
    for (final String id : chosen.keySet()) {
      components.put(id, ids.get(index++));
    }
    final var stages = new ArrayList<>(draft.document().experiment().stages());
    for (final var step : steps) {
      if (step.members().stream().noneMatch(m -> m.role() == ReactionComponentRole.REACTANT)
          || step.members().stream().noneMatch(m -> m.role() == ReactionComponentRole.PRODUCT)) {
        throw new IllegalArgumentException("В стадии нужны исходник и продукт. Проверьте роли.");
      }
      final var members = new ArrayList<LaboratoryStageParticipant>();
      final var labels = new ArrayList<LaboratoryAtomMapping.Label>();
      final Map<String, String> signatures = new LinkedHashMap<>();
      for (final var member : step.members()) {
        final String id = components.get(member.fragmentId());
        members.add(new LaboratoryStageParticipant(id, member.role(), null));
        member
            .atomNumbers()
            .forEach((a, n) -> labels.add(new LaboratoryAtomMapping.Label(id, a, n)));
        if (!member.atomNumbers().isEmpty()) {
          signatures.put(
              id,
              LaboratoryMaterialProperties.signature(
                  fragments.get(member.fragmentId()).entry().drawing().molecule()));
        }
      }
      final var map = new LaboratoryAtomMapping(labels, signatures);
      final var conditions =
          step.note().isBlank()
              ? ReactionConditions.EMPTY
              : ReactionConditions.of(null, null, null, List.of(), List.of(), step.note());
      final var stage =
          new LaboratoryReactionStage(
              "stage_" + UUID.randomUUID(),
              names.getOrDefault("stage:" + step.id(), step.title()),
              step.arrow(),
              conditions,
              members,
              map);
      final var problems = map.problems(draft.document().experiment(), stage);
      if (!problems.isEmpty()) {
        throw new IllegalArgumentException(String.join(" ", problems));
      }
      stages.add(stage);
    }
    draft.stages(stages);
    if (attachment != null) {
      draft.attach(attachment);
    }
    draft.record(
        LaboratoryEntry.Kind.ACTION,
        "Импорт ChemDraw: веществ "
            + chosen.size()
            + ", стадий "
            + steps.size()
            + ". Количества, коэффициенты и результаты не назначены. "
            + String.join(" ", document.notes()),
        null);
    return draft.document();
  }
}
