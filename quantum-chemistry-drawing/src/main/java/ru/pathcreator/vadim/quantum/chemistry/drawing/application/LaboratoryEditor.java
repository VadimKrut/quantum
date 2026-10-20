/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryProcedure;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryLiterature;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionClassification;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** One history for document fields and graphical commands; selection is not a document edit. */
public final class LaboratoryEditor {

  private final ArrayDeque<LaboratoryDrawing> undo = new ArrayDeque<>();
  private final ArrayDeque<LaboratoryDrawing> redo = new ArrayDeque<>();
  private LaboratoryDrawing document;
  private LaboratoryDrawing saved;

  public LaboratoryEditor() {
    this.open(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                "exp_" + UUID.randomUUID(), "Новый опыт", ReactionConditions.EMPTY, "", List.of()),
            Map.of()));
  }

  public void open(final LaboratoryDrawing drawing) {
    this.document = drawing;
    this.saved = drawing;
    this.undo.clear();
    this.redo.clear();
  }

  public LaboratoryDrawing document() {
    return this.document;
  }

  public boolean dirty() {
    return !this.document.equals(this.saved);
  }

  public boolean canUndo() {
    return !this.undo.isEmpty();
  }

  public boolean canRedo() {
    return !this.redo.isEmpty();
  }

  public void markSaved() {
    this.saved = this.document;
  }

  public void markUnsaved() {
    this.saved = null;
  }

  public void details(final String title, final ReactionConditions conditions, final String notes) {
    final LaboratoryExperiment experiment = this.document.experiment();
    this.commit(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                title,
                conditions,
                notes,
                experiment.components(),
                experiment.journal(),
                experiment.attachments(),
                experiment.stages(),
                experiment.analyses(),
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts()));
  }

  public String add(final MoleculeDrawing drawing, final ReactionComponentRole role) {
    return this.add(drawing, role, null);
  }

  /** Opt-in placement for imported participants; one undo step, no rearrangement of existing structures. */
  public String addPlaced(final MoleculeDrawing drawing, final ReactionComponentRole role) {
    return this.add(
        LaboratoryInsertionPlacement.place(drawing, this.document.layouts().values()), role);
  }

  public String add(
      final MoleculeDrawing drawing,
      final ReactionComponentRole role,
      final LaboratoryQuantity planned) {
    final String id = "component_" + UUID.randomUUID();
    final ArrayList<LaboratoryComponent> components =
        new ArrayList<>(this.document.experiment().components());
    components.add(
        new LaboratoryComponent(
            id, drawing.molecule(), role, planned, null, LaboratoryEvidence.PLANNED));
    final LinkedHashMap<String, MoleculeDrawing> layouts =
        new LinkedHashMap<>(this.document.layouts());
    layouts.put(id, drawing);
    this.components(components, layouts);
    return id;
  }

  /** Imports a reviewed batch as one undo operation; quantities and evidence remain unset/planned. */
  public List<String> importStructures(
      final List<MoleculeDrawing> drawings, final List<ReactionComponentRole> roles) {
    if (drawings.isEmpty()
        || drawings.size() > 100
        || drawings.size() != roles.size()
        || roles.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Выберите 1–100 структур и роль для каждой.");
    }
    final var components = new ArrayList<>(this.document.experiment().components());
    final var layouts = new LinkedHashMap<>(this.document.layouts());
    final var ids = new ArrayList<String>();
    double y =
        layouts.values().stream()
                .flatMap(d -> d.atoms().stream())
                .mapToDouble(a -> a.point2D().y())
                .max()
                .orElse(50)
            + 130;
    for (int i = 0; i < drawings.size(); i++) {
      final var drawing = Objects.requireNonNull(drawings.get(i));
      final double
          minX = drawing.atoms().stream().mapToDouble(a -> a.point2D().x()).min().orElse(0),
          minY = drawing.atoms().stream().mapToDouble(a -> a.point2D().y()).min().orElse(0);
      final var placed = new MoleculeSketchService().translate(drawing, 120 - minX, y - minY);
      final String id = "component_" + UUID.randomUUID();
      ids.add(id);
      components.add(
          new LaboratoryComponent(
              id, placed.molecule(), roles.get(i), null, null, LaboratoryEvidence.PLANNED));
      layouts.put(id, placed);
      y = placed.atoms().stream().mapToDouble(a -> a.point2D().y()).max().orElse(y) + 130;
    }
    this.components(components, layouts);
    this.record(
        LaboratoryEntry.Kind.ACTION,
        "Импортировано структур: "
            + drawings.size()
            + ". Количества и фактические результаты не назначены.",
        null);
    return List.copyOf(ids);
  }

  /** Import the complete reviewed reaction as a single undo step. */
  public String importReaction(final ReactionExchange.Entry reaction) {
    final LaboratoryEditor draft = new LaboratoryEditor();
    draft.open(this.document);
    final var ids =
        draft.importStructures(
            reaction.participants().stream().map(p -> p.structure().drawing()).toList(),
            reaction.participants().stream().map(ReactionExchange.Participant::role).toList());
    final var members =
        new ArrayList<
            ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryStageParticipant>();
    final var labels =
        new ArrayList<
            ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAtomMapping.Label>();
    final Map<String, String> signatures = new LinkedHashMap<>();
    for (int i = 0; i < ids.size(); i++) {
      final String id = ids.get(i);
      final var participant = reaction.participants().get(i);
      members.add(
          new ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryStageParticipant(
              id, participant.role(), null));
      participant
          .atomNumbers()
          .forEach(
              (atom, number) ->
                  labels.add(
                      new ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                          .LaboratoryAtomMapping.Label(id, atom, number)));
      if (!participant.atomNumbers().isEmpty()) {
        signatures.put(
            id,
            ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties
                .signature(participant.structure().drawing().molecule()));
      }
    }
    final var mapping =
        new ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAtomMapping(
            labels, signatures);
    final var stage =
        new ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage(
            "stage_" + UUID.randomUUID(),
            reaction.title(),
            ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage.Arrow
                .FORWARD,
            ReactionConditions.EMPTY,
            members,
            mapping);
    final var problems = mapping.problems(draft.document().experiment(), stage);
    if (!problems.isEmpty()) {
      throw new IllegalArgumentException(String.join(" ", problems));
    }
    final var stages = new ArrayList<>(draft.document().experiment().stages());
    stages.add(stage);
    draft.stages(stages);
    draft.record(
        LaboratoryEntry.Kind.ACTION,
        "Импортирована схема «"
            + reaction.title()
            + "». Количества, коэффициенты и результаты не назначены; происхождение карты — исходный файл.",
        null);
    this.commit(draft.document());
    return stage.id();
  }

  public void importChemDraw(
      final LaboratoryDrawing expected,
      final ChemDrawExchange.Document document,
      final Map<String, ReactionComponentRole> selected,
      final java.util.Set<String> stages,
      final Map<String, String> names,
      final LaboratoryAttachment attachment) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Откройте импорт заново.");
    }
    this.commit(
        ChemDrawExchange.importDocument(expected, document, selected, stages, names, attachment));
  }

  public void replace(
      final String id, final MoleculeDrawing drawing, final ReactionComponentRole role) {
    final boolean sharedIdentityChanged =
        this.document.experiment().components().stream()
            .anyMatch(
                component ->
                    !component.id().equals(id)
                        && component.molecule().id().equals(drawing.moleculeId())
                        && !component.molecule().equals(drawing.molecule()));
    final MoleculeDrawing replacement =
        sharedIdentityChanged ? new MoleculeSketchService().independentCopy(drawing) : drawing;
    final List<LaboratoryComponent> components =
        this.document.experiment().components().stream()
            .map(
                component ->
                    component.id().equals(id)
                        ? component.withMolecule(replacement.molecule(), role)
                        : component)
            .toList();
    final LinkedHashMap<String, MoleculeDrawing> layouts =
        new LinkedHashMap<>(this.document.layouts());
    layouts.put(id, replacement);
    this.components(components, layouts);
  }

  public void joinDrafts(
      final String firstId,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId firstAtom,
      final String secondId,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId secondAtom,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType type) {
    if (firstId.equals(secondId)) {
      throw new IllegalArgumentException("Выберите два разных фрагмента.");
    }
    final var components = this.document.experiment().components();
    final var first =
        components.stream().filter(item -> item.id().equals(firstId)).findFirst().orElseThrow();
    final var second =
        components.stream().filter(item -> item.id().equals(secondId)).findFirst().orElseThrow();
    if (first.role() != second.role()) {
      throw new IllegalArgumentException(
          "У фрагментов разные роли в опыте. Сначала назначьте одинаковую роль.");
    }
    final boolean firstHasData = this.hasComponentData(first),
        secondHasData = this.hasComponentData(second);
    if (firstHasData && secondHasData) {
      throw new IllegalArgumentException(
          "Оба компонента имеют количества, свойства или ссылки на стадии. Их записи нельзя объединить без потери смысла. Присоедините свободный фрагмент без этих данных.");
    }
    // Preserve the populated occurrence and every reference to it, regardless of gesture direction.
    final var retained = secondHasData ? second : first;
    final var fragment = secondHasData ? first : second;
    final var retainedAtom = secondHasData ? secondAtom : firstAtom;
    final var fragmentAtom = secondHasData ? firstAtom : secondAtom;
    final var joined =
        MoleculeDraftJoin.join(
            this.document.layouts().get(retained.id()),
            retainedAtom,
            this.document.layouts().get(fragment.id()),
            fragmentAtom,
            type);
    this.editComponents(Map.of(retained.id(), joined), java.util.Set.of(fragment.id()));
    this.record(
        LaboratoryEntry.Kind.ACTION,
        "К компоненту «"
            + retained.molecule().displayName()
            + "» присоединён 2D-фрагмент «"
            + fragment.molecule().displayName()
            + "». Структура изменена; количества сохранены без пересчёта. Проверьте свойства материала и химическую корректность рисунка.",
        null);
  }

  private boolean hasComponentData(final LaboratoryComponent component) {
    return component.planned() != null
        || component.actual() != null
        || component.evidence() != LaboratoryEvidence.PLANNED
        || !component.properties().empty()
        || this.document.experiment().procedure().steps().stream()
            .anyMatch(s -> component.id().equals(s.componentId()))
        || this.document.experiment().samples().stream()
            .anyMatch(s -> s.componentId().equals(component.id()))
        || this.document.experiment().analyses().stream()
            .anyMatch(a -> a.componentId().equals(component.id()))
        || this.document.experiment().stages().stream()
            .flatMap(stage -> stage.participants().stream())
            .anyMatch(participant -> participant.componentId().equals(component.id()));
  }

  public void remove(final String id) {
    final List<LaboratoryComponent> components =
        this.document.experiment().components().stream()
            .filter(component -> !component.id().equals(id))
            .toList();
    final LinkedHashMap<String, MoleculeDrawing> layouts =
        new LinkedHashMap<>(this.document.layouts());
    layouts.remove(id);
    this.components(components, layouts);
  }

  public void undo() {
    if (this.canUndo()) {
      this.redo.push(this.document);
      this.document = this.keepRecords(this.undo.pop());
    }
  }

  public void redo() {
    if (this.canRedo()) {
      this.undo.push(this.document);
      this.document = this.keepRecords(this.redo.pop());
    }
  }

  public void quantities(
      final String componentId,
      final LaboratoryQuantity planned,
      final LaboratoryQuantity actual,
      final LaboratoryEvidence evidence) {
    final var properties =
        this.document.experiment().components().stream()
            .filter(component -> component.id().equals(componentId))
            .findFirst()
            .orElseThrow()
            .properties();
    this.quantify(componentId, planned, actual, evidence, properties);
  }

  public void quantify(
      final String componentId,
      final LaboratoryQuantity planned,
      final LaboratoryQuantity actual,
      final LaboratoryEvidence evidence,
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties
          properties) {
    final LaboratoryComponent old =
        this.document.experiment().components().stream()
            .filter(component -> component.id().equals(componentId))
            .findFirst()
            .orElseThrow();
    final List<LaboratoryComponent> components =
        this.document.experiment().components().stream()
            .map(
                component ->
                    component.id().equals(componentId)
                        ? new LaboratoryComponent(
                            componentId,
                            component.molecule(),
                            component.role(),
                            planned,
                            actual,
                            evidence,
                            properties)
                        : component)
            .toList();
    this.components(components, this.document.layouts());
    if (!old.properties().equals(properties)) {
      this.record(
          LaboratoryEntry.Kind.ACTION,
          old.molecule().displayName()
              + ": свойства для пересчёта количества обновлены; источник: "
              + (properties.empty() ? "не задан" : properties.source()),
          null);
    }
    if (!Objects.equals(old.actual(), actual) || old.evidence() != evidence) {
      this.record(
          LaboratoryEntry.Kind.ACTION,
          old.molecule().displayName()
              + ": записанное количество · "
              + old.evidence().quantityLabel()
              + ": "
              + (old.actual() == null ? "не указан" : old.actual())
              + " → "
              + evidence.quantityLabel()
              + ": "
              + (actual == null ? "не указан" : actual)
              + "; "
              + evidence,
          null);
    }
  }

  /** One undo step for the entire loading plan; observations and inventory stay independent. */
  public void planBatch(
      final LaboratoryDrawing expected, final Map<String, LaboratoryQuantity> quantities) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException(
          "Опыт изменился во время расчёта. Откройте загрузку заново.");
    }
    final var known =
        this.document.experiment().components().stream()
            .map(LaboratoryComponent::id)
            .collect(java.util.stream.Collectors.toSet());
    if (!known.containsAll(quantities.keySet())
        || quantities.values().stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(
          "План содержит неизвестные компоненты или пустые значения.");
    }
    final var components =
        this.document.experiment().components().stream()
            .map(
                c ->
                    quantities.containsKey(c.id())
                        ? new LaboratoryComponent(
                            c.id(),
                            c.molecule(),
                            c.role(),
                            quantities.get(c.id()),
                            c.actual(),
                            c.evidence(),
                            c.properties())
                        : c)
            .toList();
    this.components(components, this.document.layouts());
  }

  /** Explicitly reviewed stock withdrawals become one undoable loading update. */
  public void actualBatch(
      final LaboratoryDrawing expected, final Map<String, LaboratoryQuantity> quantities) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Просмотрите перенос факта заново.");
    }
    final var known =
        this.document.experiment().components().stream()
            .map(LaboratoryComponent::id)
            .collect(java.util.stream.Collectors.toSet());
    if (quantities.isEmpty()
        || !known.containsAll(quantities.keySet())
        || quantities.values().stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Неверный состав фактической загрузки.");
    }
    final var components =
        this.document.experiment().components().stream()
            .map(
                c ->
                    quantities.containsKey(c.id())
                        ? new LaboratoryComponent(
                            c.id(),
                            c.molecule(),
                            c.role(),
                            c.planned(),
                            quantities.get(c.id()),
                            LaboratoryEvidence.OBSERVED,
                            c.properties())
                        : c)
            .toList();
    if (components.equals(this.document.experiment().components())) {
      return;
    }
    this.components(components, this.document.layouts());
    this.record(
        LaboratoryEntry.Kind.ACTION,
        "Фактическая загрузка перенесена из проверенного расхода партий: " + quantities,
        null);
  }

  public void record(final LaboratoryEntry.Kind kind, final String text, final String correctsId) {
    final LaboratoryExperiment experiment = this.document.experiment();
    final ArrayList<LaboratoryEntry> journal = new ArrayList<>(experiment.journal());
    journal.add(
        new LaboratoryEntry("entry_" + UUID.randomUUID(), Instant.now(), kind, text, correctsId));
    this.document =
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                experiment.components(),
                journal,
                experiment.attachments(),
                experiment.stages(),
                experiment.analyses(),
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts());
  }

  public void attach(final LaboratoryAttachment attachment) {
    final LaboratoryExperiment experiment = this.document.experiment();
    if (experiment.attachments().stream()
        .anyMatch(
            item ->
                item.sha256().equals(attachment.sha256())
                    && item.name().equals(attachment.name()))) {
      return;
    }
    final ArrayList<LaboratoryAttachment> attachments = new ArrayList<>(experiment.attachments());
    attachments.add(attachment);
    this.document =
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                experiment.components(),
                experiment.journal(),
                attachments,
                experiment.stages(),
                experiment.analyses(),
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts());
  }

  /** Append-only measured records survive ordinary undo; corrections retain the earlier result. */
  public void analysis(
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis analysis) {
    final LaboratoryExperiment experiment = this.document.experiment();
    final var existing =
        experiment.analyses().stream().filter(a -> a.id().equals(analysis.id())).findFirst();
    if (existing.isPresent()) {
      if (existing.get().equals(analysis)) {
        return;
      }
      throw new IllegalArgumentException(
          "Эта аналитическая запись уже существует. Создайте исправление.");
    }
    final var analyses = new ArrayList<>(experiment.analyses());
    analyses.add(analysis);
    this.document =
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                experiment.components(),
                experiment.journal(),
                experiment.attachments(),
                experiment.stages(),
                analyses,
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts());
    this.record(
        LaboratoryEntry.Kind.ACTION,
        "Аналитика: "
            + analysis.title()
            + " · "
            + analysis.origin()
            + (analysis.correctsId() == null ? "" : " · исправление " + analysis.correctsId()),
        null);
  }

  public void samples(
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample> expected,
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample> next,
      final String reason) {
    final var experiment = this.document.experiment();
    if (!experiment.samples().equals(expected)) {
      throw new IllegalArgumentException("Учёт образцов изменился. Откройте операцию заново.");
    }
    ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleLedger.requireAppend(
        expected, next);
    this.document =
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                experiment.components(),
                experiment.journal(),
                experiment.attachments(),
                experiment.stages(),
                experiment.analyses(),
                next,
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts());
    this.record(LaboratoryEntry.Kind.ACTION, reason, null);
  }

  private List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample>
      mergeSamples(
          final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample> old,
          final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample>
              current) {
    final var merged =
        new LinkedHashMap<
            String, ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample>();
    old.forEach(s -> merged.put(s.id(), s));
    current.forEach(s -> merged.put(s.id(), s));
    final var result =
        merged.values().stream()
            .sorted(
                java.util.Comparator.comparing(
                    ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySample
                        ::createdAt))
            .toList();
    ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleLedger.validate(result);
    return result;
  }

  public void restore(final LaboratoryDrawing revision) {
    this.revise(revision, "Восстановлена предыдущая версия документа; журнал сохранён.");
  }

  public void revise(final LaboratoryDrawing revision, final String reason) {
    if (!this.document.experiment().id().equals(revision.experiment().id())) {
      throw new IllegalArgumentException("Версия принадлежит другому опыту.");
    }
    this.commit(this.keepRecords(revision));
    this.record(LaboratoryEntry.Kind.ACTION, reason, null);
  }

  public void repeat() {
    this.open(plannedCopy(this.document, this.document.experiment().title() + " — повтор"));
    this.saved = null;
  }

  /** A new experiment identity; observations, attachments and measured quantities never become new facts. */
  public static LaboratoryDrawing plannedCopy(final LaboratoryDrawing source, final String title) {
    return plannedCopy(source, title, LaboratoryRepeatContext.Source.UNLOCATED);
  }

  /** Keeps source context as quoted reference notes, never as new execution records. */
  public static LaboratoryDrawing plannedCopy(
      final LaboratoryDrawing source,
      final String title,
      final LaboratoryRepeatContext.Source reference) {
    final LaboratoryExperiment previous = source.experiment();
    final List<LaboratoryComponent> planned =
        previous.components().stream()
            .map(
                component ->
                    new LaboratoryComponent(
                        component.id(),
                        component.molecule(),
                        component.role(),
                        component.planned(),
                        null,
                        LaboratoryEvidence.PLANNED,
                        component.properties()))
            .toList();
    final var template =
        new LaboratoryExperiment(
            "exp_" + UUID.randomUUID(),
            title,
            previous.conditions(),
            "",
            planned,
            List.of(),
            List.of(),
            previous.stages(),
            List.of(),
            List.of());
    final var repeated =
        LaboratoryProcedureService.repeatPlan(previous.procedure(), template, true);
    final List<LaboratoryEntry> journal =
        repeated.omittedSteps() == 0
            ? List.of()
            : List.of(
                new LaboratoryEntry(
                    "entry_" + UUID.randomUUID(),
                    Instant.now(),
                    LaboratoryEntry.Kind.ACTION,
                    repeated.warning(),
                    null));
    return new LaboratoryDrawing(
        new LaboratoryExperiment(
            template.id(),
            title,
            template.conditions(),
            LaboratoryRepeatContext.notes(source, reference),
            planned,
            journal,
            List.of(),
            template.stages(),
            List.of(),
            List.of(),
            repeated.procedure(),
            previous.route(),
            LaboratoryReactionOperationsService.repeatPlan(previous.operations(), template),
            LaboratoryAnalyticalEvidence.EMPTY,
            LaboratoryLiteratureService.withoutAttachmentsForRepeat(
                previous.literature(),
                template.stages().stream()
                    .map(LaboratoryReactionStage::id)
                    .collect(java.util.stream.Collectors.toSet()))),
        source.layouts());
  }

  private LaboratoryDrawing keepRecords(final LaboratoryDrawing target) {
    final LaboratoryExperiment current = this.document.experiment();
    final LaboratoryExperiment previous = target.experiment();
    final LinkedHashMap<String, LaboratoryEntry> journal = new LinkedHashMap<>();
    previous.journal().forEach(entry -> journal.put(entry.id(), entry));
    current.journal().forEach(entry -> journal.put(entry.id(), entry));
    final LinkedHashMap<String, LaboratoryAttachment> attachments = new LinkedHashMap<>();
    previous.attachments().forEach(item -> attachments.put(item.id(), item));
    current.attachments().forEach(item -> attachments.put(item.id(), item));
    final LinkedHashMap<
            String, ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis>
        analyses = new LinkedHashMap<>();
    previous.analyses().forEach(item -> analyses.put(item.id(), item));
    current.analyses().forEach(item -> analyses.put(item.id(), item));
    return new LaboratoryDrawing(
        new LaboratoryExperiment(
            previous.id(),
            previous.title(),
            previous.conditions(),
            previous.notes(),
            previous.components(),
            List.copyOf(journal.values()),
            List.copyOf(attachments.values()),
            previous.stages(),
            List.copyOf(analyses.values()),
            this.mergeSamples(previous.samples(), current.samples()),
            LaboratoryProcedureService.keepRecords(previous.procedure(), current.procedure()),
            previous.route(),
            LaboratoryReactionOperations.merge(previous.operations(), current.operations()),
            LaboratoryAnalyticalEvidence.merge(
                previous.analyticalEvidence(), current.analyticalEvidence()),
            previous.literature()),
        target.layouts());
  }

  private void components(
      final List<LaboratoryComponent> components, final Map<String, MoleculeDrawing> layouts) {
    final LaboratoryExperiment experiment = this.document.experiment();
    this.commit(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                components,
                experiment.journal(),
                experiment.attachments(),
                experiment.stages(),
                experiment.analyses(),
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            layouts));
  }

  public void applyGeometry(
      final String componentId,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule expected,
      final ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule geometry,
      final String reason) {
    final LaboratoryComponent component =
        this.document.experiment().components().stream()
            .filter(item -> item.id().equals(componentId))
            .findFirst()
            .orElseThrow();
    if (!component.molecule().equals(expected)) {
      throw new IllegalArgumentException(
          "Структура изменилась после запуска. Повторите расчёт для текущего состояния.");
    }
    if (!expected.bonds().equals(geometry.bonds())
        || !expected.atoms().stream()
            .map(atom -> atom.id())
            .toList()
            .equals(geometry.atoms().stream().map(atom -> atom.id()).toList())) {
      throw new IllegalArgumentException("Геометрия относится к другой структуре.");
    }
    final var layout = this.document.layouts().get(componentId);
    for (int index = 0; index < expected.atoms().size(); index++) {
      final var input = expected.atoms().get(index);
      final var output = geometry.atoms().get(index);
      if (output.coordinate() == null
          || !ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom.of(
                  input.id(),
                  input.element().symbol(),
                  output.coordinate(),
                  input.formalCharge(),
                  input.isotope(),
                  input.radicalState(),
                  input.metadata())
              .equals(output)) {
        throw new IllegalArgumentException(
            "Геометрия изменила свойства атомов или содержит неполные координаты.");
      }
    }
    if (!expected.charge().equals(geometry.charge())
        || !expected.spinMultiplicity().equals(geometry.spinMultiplicity())
        || !expected.stereochemistry().equals(geometry.stereochemistry())
        || !expected.conformation().equals(geometry.conformation())) {
      throw new IllegalArgumentException(
          "Геометрия изменила электронное или стереохимическое состояние.");
    }
    this.replace(
        componentId,
        MoleculeDrawing.of(geometry, layout.atoms(), layout.bonds(), layout.manualFields()),
        component.role());
    this.record(LaboratoryEntry.Kind.ACTION, reason, null);
  }

  public void arrange(final double width) {
    this.editComponents(
        new LaboratorySheetArrangement().arrange(this.document, width), java.util.Set.of());
  }

  public void editComponents(
      final Map<String, MoleculeDrawing> replacements, final java.util.Set<String> removed) {
    final LaboratoryEditor draft = new LaboratoryEditor();
    draft.open(this.document);
    // All validation happens on a temporary document; any failure leaves the live history
    // untouched.
    for (final var component : this.document.experiment().components()) {
      if (removed.contains(component.id())) {
        draft.remove(component.id());
      } else if (replacements.containsKey(component.id())) {
        draft.replace(component.id(), replacements.get(component.id()), component.role());
      }
    }
    this.commit(draft.document());
  }

  public List<String> paste(final List<LaboratoryFragment> fragments) {
    final LaboratoryEditor draft = new LaboratoryEditor();
    draft.open(this.document);
    final ArrayList<String> ids = new ArrayList<>();
    final MoleculeSketchService sketch = new MoleculeSketchService();
    for (final LaboratoryFragment fragment : fragments) {
      ids.add(
          draft.add(
              sketch.independentCopy(fragment.drawing()), fragment.role(), fragment.planned()));
    }
    this.commit(draft.document());
    return List.copyOf(ids);
  }

  public void stages(
      final List<ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage>
          stages) {
    final LaboratoryExperiment experiment = this.document.experiment();
    this.commit(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                experiment.components(),
                experiment.journal(),
                experiment.attachments(),
                stages,
                experiment.analyses(),
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts()));
  }

  /** Change only explicit classification while preserving concurrently edited stage fields. */
  public void classification(
      final String stageId,
      final LaboratoryReactionClassification expected,
      final LaboratoryReactionClassification next) {
    final var stage =
        this.document.experiment().stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Стадия отсутствует."));
    if (!stage.classification().equals(expected)) {
      throw new IllegalArgumentException(
          "Названия или механизмы стадии изменились. Откройте классификацию заново.");
    }
    this.classification(stage, next, stage.title());
  }

  /** Classification and an optional caller-selected title are one reversible edit with one explicit journal entry. */
  public void classification(
      final LaboratoryReactionStage expected,
      final LaboratoryReactionClassification next,
      final String title) {
    Objects.requireNonNull(expected);
    Objects.requireNonNull(next);
    final var experiment = this.document.experiment();
    final var current =
        experiment.stages().stream()
            .filter(s -> s.id().equals(expected.id()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Стадия отсутствует."));
    if (!current.equals(expected)) {
      throw new IllegalArgumentException(
          "Стадия изменилась. Откройте названия и механизмы заново.");
    }
    final var changed =
        new LaboratoryReactionStage(
            current.id(),
            title,
            current.arrow(),
            current.conditions(),
            current.participants(),
            current.atomMapping(),
            next);
    if (changed.equals(current)) {
      return;
    }
    final var stages = new ArrayList<>(experiment.stages());
    stages.set(stages.indexOf(current), changed);
    final var journal = new ArrayList<>(experiment.journal());
    journal.add(
        new LaboratoryEntry(
            "entry_" + UUID.randomUUID(),
            Instant.now(),
            LaboratoryEntry.Kind.ACTION,
            "Пользователь изменил названия и механизмы стадии «"
                + current.title()
                + "»"
                + (current.title().equals(changed.title()) ? "" : " → «" + changed.title() + "»")
                + ". Названия: "
                + classificationTerms(next.reactionNames())
                + ". Механизмы: "
                + classificationTerms(next.mechanisms())
                + (next.note().isEmpty() ? "." : ". Примечание: " + next.note()),
            null));
    this.commit(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                experiment.id(),
                experiment.title(),
                experiment.conditions(),
                experiment.notes(),
                experiment.components(),
                journal,
                experiment.attachments(),
                stages,
                experiment.analyses(),
                experiment.samples(),
                experiment.procedure(),
                experiment.route(),
                experiment.operations(),
                experiment.analyticalEvidence(),
                experiment.literature()),
            this.document.layouts()));
  }

  private static String classificationTerms(
      final List<LaboratoryReactionClassification.Term> values) {
    return values.isEmpty()
        ? "не указаны"
        : values.stream()
            .map(LaboratoryReactionClassification.Term::text)
            .collect(java.util.stream.Collectors.joining("; "));
  }

  /** Change future operations atomically; recorded operations remain immutable. */
  public void procedurePlan(
      final LaboratoryDrawing expected, final List<LaboratoryProcedure.Step> steps) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Откройте план операций заново.");
    }
    final var experiment = this.document.experiment();
    final var procedure = LaboratoryProcedureService.updatePlan(experiment, steps);
    this.commit(
        new LaboratoryDrawing(withProcedure(experiment, procedure), this.document.layouts()));
  }

  /** Append a performed event without changing stock, analytical records or measured component quantities. */
  public LaboratoryProcedure.Event procedureEvent(
      final LaboratoryDrawing expected,
      final String stepId,
      final LaboratoryProcedure.Action action,
      final String text,
      final LaboratoryQuantity actualQuantity,
      final String correctsId,
      final Instant at) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException(
          "Опыт изменился. Проверьте текущее состояние операции и повторите действие.");
    }
    final var experiment = this.document.experiment();
    final var event =
        LaboratoryProcedureService.event(
            experiment, stepId, action, text, actualQuantity, correctsId, at);
    final var events = new ArrayList<>(experiment.procedure().events());
    events.add(event);
    final var procedure = new LaboratoryProcedure(experiment.procedure().steps(), events);
    this.document =
        new LaboratoryDrawing(withProcedure(experiment, procedure), this.document.layouts());
    return event;
  }

  private static LaboratoryExperiment withProcedure(
      final LaboratoryExperiment experiment, final LaboratoryProcedure procedure) {
    return new LaboratoryExperiment(
        experiment.id(),
        experiment.title(),
        experiment.conditions(),
        experiment.notes(),
        experiment.components(),
        experiment.journal(),
        experiment.attachments(),
        experiment.stages(),
        experiment.analyses(),
        experiment.samples(),
        procedure,
        experiment.route(),
        experiment.operations(),
        experiment.analyticalEvidence(),
        experiment.literature());
  }

  /** Record an explicit synthesis plan as one reversible document edit. */
  public void literature(final LaboratoryDrawing expected, final LaboratoryLiterature literature) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Откройте литературу заново.");
    }
    final var e = this.document.experiment();
    this.commit(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                e.id(),
                e.title(),
                e.conditions(),
                e.notes(),
                e.components(),
                e.journal(),
                e.attachments(),
                e.stages(),
                e.analyses(),
                e.samples(),
                e.procedure(),
                e.route(),
                e.operations(),
                e.analyticalEvidence(),
                literature),
            this.document.layouts()));
  }

  public void operations(
      final LaboratoryDrawing expected,
      final LaboratoryReactionOperations operations,
      final String reason) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Откройте операции заново.");
    }
    final var e = this.document.experiment();
    LaboratoryReactionOperations.requireAppend(e.operations(), operations);
    LaboratoryReactionOperationsService.validateNewLinks(e, e.operations(), operations);
    this.document =
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                e.id(),
                e.title(),
                e.conditions(),
                e.notes(),
                e.components(),
                e.journal(),
                e.attachments(),
                e.stages(),
                e.analyses(),
                e.samples(),
                e.procedure(),
                e.route(),
                operations,
                e.analyticalEvidence(),
                e.literature()),
            this.document.layouts());
    if (reason != null && !reason.isBlank()) {
      this.record(LaboratoryEntry.Kind.ACTION, reason, null);
    }
  }

  public void analyticalEvidence(
      final LaboratoryDrawing expected, final LaboratoryAnalyticalEvidence.Entry entry) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Откройте аналитику заново.");
    }
    final var e = this.document.experiment();
    this.document =
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                e.id(),
                e.title(),
                e.conditions(),
                e.notes(),
                e.components(),
                e.journal(),
                e.attachments(),
                e.stages(),
                e.analyses(),
                e.samples(),
                e.procedure(),
                e.route(),
                e.operations(),
                e.analyticalEvidence().append(entry),
                e.literature()),
            this.document.layouts());
  }

  public void route(
      final LaboratoryDrawing expected,
      final ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySynthesisRoute route) {
    if (!this.document.equals(expected)) {
      throw new IllegalArgumentException("Опыт изменился. Откройте маршрут заново.");
    }
    final var e = this.document.experiment();
    this.commit(
        new LaboratoryDrawing(
            new LaboratoryExperiment(
                e.id(),
                e.title(),
                e.conditions(),
                e.notes(),
                e.components(),
                e.journal(),
                e.attachments(),
                e.stages(),
                e.analyses(),
                e.samples(),
                e.procedure(),
                route,
                e.operations(),
                e.analyticalEvidence(),
                e.literature()),
            this.document.layouts()));
  }

  private void commit(final LaboratoryDrawing next) {
    if (!next.equals(this.document)) {
      this.undo.push(this.document);
      if (this.undo.size() > 200) {
        this.undo.removeLast();
      }
      this.redo.clear();
      this.document = next;
    }
  }
}
