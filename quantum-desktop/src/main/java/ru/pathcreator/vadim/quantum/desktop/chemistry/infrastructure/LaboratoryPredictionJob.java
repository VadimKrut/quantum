/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.pathcreator.vadim.quantum.chemistry.api.QuantumChemistry;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionPredictionWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** Immutable stage input, existing public prediction API, and an independently durable result. */
public final class LaboratoryPredictionJob {
  public record Result(
      String requestId,
      Path file,
      String status,
      String report,
      ReactionPredictionConfidenceLevel confidence,
      List<ReactionProductCandidate> candidates,
      String inputReview) {
    public Result {
      candidates = List.copyOf(candidates);
      inputReview = Objects.requireNonNull(inputReview);
    }

    public Result(
        final String requestId,
        final Path file,
        final String status,
        final String report,
        final ReactionPredictionConfidenceLevel confidence,
        final List<ReactionProductCandidate> candidates) {
      this(requestId, file, status, report, confidence, candidates, "");
    }
  }

  private final String id = "prediction_" + UUID.randomUUID();
  private final LaboratoryDrawing input;
  private final LaboratoryReactionStage stage;
  private final List<LaboratoryComponent> sources;
  private final ReactionPredictionRequest request;
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private final AtomicBoolean started = new AtomicBoolean();
  private volatile Thread worker;

  public LaboratoryPredictionJob(
      final LaboratoryDrawing input,
      final LaboratoryReactionStage stage,
      final OrganicReactionDomain domain) {
    this.input = input;
    this.stage = stage;
    if (!input.experiment().stages().contains(stage)) {
      throw new IllegalArgumentException("Стадия изменилась. Откройте прогноз заново.");
    }
    final Set<String> ids = new HashSet<>();
    stage.participants().forEach(participant -> ids.add(participant.componentId()));
    this.sources =
        input.experiment().components().stream()
            .filter(component -> ids.contains(component.id()))
            .toList();
    final List<ReactionParticipant> reactants = new ArrayList<>();
    final List<ReactionConditionComponent> helpers =
        new ArrayList<>(stage.conditions().components());
    for (final var participant : stage.participants()) {
      final var component =
          this.sources.stream()
              .filter(item -> item.id().equals(participant.componentId()))
              .findFirst()
              .orElseThrow();
      if (participant.role() == ReactionComponentRole.REACTANT) {
        if (participant.coefficient() == null) {
          throw new IllegalArgumentException(
              "Укажите коэффициент исходника «"
                  + component.molecule().displayName()
                  + "» в стадии.");
        }
        reactants.add(ReactionParticipant.of(component.molecule(), participant.coefficient()));
      } else if (participant.role() != ReactionComponentRole.PRODUCT) {
        if (helpers.stream()
            .noneMatch(
                helper ->
                    helper.role() == participant.role()
                        && helper.name().equalsIgnoreCase(component.molecule().displayName()))) {
          helpers.add(
              ReactionConditionComponent.named(
                  participant.role(), component.molecule().displayName()));
        }
      }
    }
    if (reactants.isEmpty()) {
      throw new IllegalArgumentException("В стадии нет исходников для прогноза.");
    }
    if (reactants.stream().mapToInt(participant -> participant.molecule().atomCount()).sum()
        > 128) {
      throw new IllegalArgumentException(
          "Прогноз в Studio поддерживает до 128 явных атомов в исходных структурах.");
    }
    final var conditions = stage.conditions();
    this.request =
        ReactionPredictionRequest.of(
            this.id,
            LaboratoryReactionStage.reactionSide(reactants),
            ReactionConditions.of(
                conditions.temperature(),
                conditions.pressure(),
                conditions.duration(),
                helpers,
                conditions.features(),
                conditions.note()),
            List.of(domain),
            List.of(),
            "Прогноз стадии «"
                + stage.title()
                + "». Фактические продукты и количества не назначаются.");
  }

  public void cancel() {
    this.cancelled.set(true);
    final Thread current = this.worker;
    if (current != null) {
      current.interrupt();
    }
  }

  public ReactionPredictionRequest request() {
    return this.request;
  }

  public Result execute(final Path directory) throws java.io.IOException {
    if (!this.started.compareAndSet(false, true)) {
      throw new IllegalStateException("Для повторного прогноза создайте новый запрос.");
    }
    this.worker = Thread.currentThread();
    OrganicReactionPredictionWorkflowResult result = null;
    String failure = null;
    try {
      if (!this.cancelled.get()) {
        result = QuantumChemistry.predictOrganicReaction(this.request);
      }
    } catch (final RuntimeException exception) {
      failure = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    } finally {
      this.worker = null;
      Thread.interrupted();
    }
    final String status =
        this.cancelled.get() ? "CANCELLED" : result == null ? "FAILED" : result.status().name();
    final List<ReactionProductCandidate> candidates =
        this.cancelled.get() || result == null || !result.hasPredictionResult()
            ? List.of()
            : result.predictionResult().candidates();
    final String report = this.report(status, result, failure);
    final String inputReview =
        !this.cancelled.get() && result != null && result.hasPredictionResult()
            ? LaboratoryPredictionRequirements.describe(result.predictionResult())
            : "Прогноз не завершён; структурные варианты не доступны.";
    final var snapshot =
        new LaboratoryExperiment(
            this.input.experiment().id(),
            this.input.experiment().title(),
            this.input.experiment().conditions(),
            this.input.experiment().notes(),
            this.sources,
            List.of(),
            List.of(),
            List.of(this.stage));
    final var builder =
        ChemistryStorageDocument.builder(this.id, "Прогноз · " + this.stage.title())
            .laboratoryExperiment(snapshot)
            .metadata("prediction.request_note", this.request.note())
            .metadata("prediction.stage", this.stage.id())
            .metadata("prediction.domain", this.request.domains().getFirst().name())
            .metadata("prediction.status", status)
            .metadata("prediction.adapter_version", "studio-prediction-1")
            .metadata("prediction.input_review", inputReview)
            .metadata("prediction.finished", java.time.Instant.now().toString())
            .metadata("prediction.report", report)
            .metadata(
                "prediction.confidence",
                (result != null && result.hasExplanation()
                        ? result.explanation().confidenceLevel()
                        : ReactionPredictionConfidenceLevel.NONE)
                    .name());
    final String engineVersion = QuantumChemistry.class.getPackage().getImplementationVersion();
    builder.metadata(
        "prediction.engine_version", engineVersion == null ? "unknown" : engineVersion);
    final Map<String, Molecule> molecules = new LinkedHashMap<>();
    this.sources.forEach(
        component -> {
          molecules.put(component.molecule().id().value(), component.molecule());
          builder.extension(
              LaboratoryLayoutCodec.write(
                  component.id(), this.input.layouts().get(component.id())));
        });
    if (result != null && result.hasPredictionResult() && !this.cancelled.get()) {
      final var original =
          new ChemistryStorageService()
              .predictionRecord(
                  this.id + "_result",
                  0,
                  status.equals("FAILED")
                      ? ChemistryStorageReactionRecordStatus.FAILED
                      : ChemistryStorageReactionRecordStatus.SIMULATED_CLASSICAL,
                  result.predictionResult(),
                  "Вес и условный выход модели не являются измеренной вероятностью или экспериментальным выходом.");
      final List<ChemistryStoragePredictionCandidateRecord> records = new ArrayList<>();
      for (int rank = 0; rank < candidates.size(); rank++) {
        final var candidate = candidates.get(rank);
        final var record = original.candidates().get(rank);
        final List<String> productIds = new ArrayList<>(), signatures = new ArrayList<>();
        int index = 0;
        for (final var participant : candidate.products().participants()) {
          final Molecule copy =
              this.copyProduct(
                  participant.molecule(), rank, this.id + "_c" + rank + "_p" + index++);
          molecules.put(copy.id().value(), copy);
          productIds.add(copy.id().value());
          signatures.add(
              participant.coefficient().value()
                  + "*"
                  + copy.id().value()
                  + ":"
                  + copy.formula().hillNotation());
        }
        records.add(
            ChemistryStoragePredictionCandidateRecord.of(
                record.rank(),
                productIds,
                record.productFormulaSignatures(),
                signatures,
                record.probability(),
                record.expectedYieldPercent(),
                record.mechanismIds(),
                record.mechanismDescriptors(),
                record.scoreComponents(),
                record.evidence(),
                record.note()));
      }
      builder.predictionRecord(
          ChemistryStoragePredictionRecord.of(
              original.id(),
              original.requestId(),
              0,
              original.methodType(),
              original.status(),
              records,
              original.diagnostics(),
              original.note()));
    }
    molecules.values().forEach(builder::molecule);
    final Path file = LaboratoryComputationFiles.write(directory, this.id, builder.build());
    return new Result(
        this.id,
        file,
        status,
        report,
        result != null && result.hasExplanation()
            ? result.explanation().confidenceLevel()
            : ReactionPredictionConfidenceLevel.NONE,
        candidates,
        inputReview);
  }

  private String report(
      final String status,
      final OrganicReactionPredictionWorkflowResult result,
      final String failure) {
    final StringBuilder text =
        new StringBuilder("Стадия: ")
            .append(this.stage.title())
            .append("\nСтатус: ")
            .append(status)
            .append("\nОбласть: ")
            .append(this.request.domains())
            .append("\n\nИсходники:\n");
    this.request
        .reactants()
        .participants()
        .forEach(
            participant ->
                text.append(participant.coefficient().value())
                    .append(" × ")
                    .append(participant.molecule().displayName())
                    .append(" · ")
                    .append(participant.molecule().formula().hillNotation())
                    .append('\n'));
    text.append(
        "\nЭто кандидаты модели. Вес не является экспериментальной вероятностью; выход и конверсия не измерены.\n");
    if (failure != null) {
      text.append("\nОшибка: ").append(failure).append('\n');
    }
    if (result == null) {
      return text.toString();
    }
    text.append("\nДиагностика ядра:\n");
    result
        .validationResult()
        .diagnostics()
        .forEach(
            diagnostic ->
                text.append(diagnostic.severity())
                    .append(" · ")
                    .append(diagnostic.message())
                    .append('\n'));
    if (result.hasPredictionResult()) {
      text.append("\nМетод: ").append(result.predictionResult().methodType()).append('\n');
      result
          .predictionResult()
          .diagnostics()
          .forEach(
              evidence ->
                  text.append(evidence.kind())
                      .append(" · ")
                      .append(evidence.description())
                      .append('\n'));
      if (result.predictionResult().hasMechanismChannels()) {
        text.append("\nГипотезы без построенного продукта:\n")
            .append(
                "Эти варианты нельзя добавить в план как структуры. Условия сами по себе не задают источник новых атомов; добавьте соответствующий реагент или явную структуру предполагаемого продукта.\n");
        result
            .predictionResult()
            .mechanismChannels()
            .forEach(
                channel ->
                    text.append("\n• ")
                        .append(channel.displayName())
                        .append(" [")
                        .append(channel.channelId())
                        .append("]\n")
                        .append(channel.explanation())
                        .append('\n')
                        .append(channel.materializationRequirement())
                        .append('\n'));
      }
      if (!result.predictionResult().hasCandidates()) {
        text.append("Кандидаты не найдены. Это не доказывает отсутствие реакции.\n");
      }
    }
    if (result.hasExplanation()) {
      text.append("\nОценка уверенности модели: ")
          .append(result.explanation().confidenceLevel())
          .append('\n');
      result
          .explanation()
          .riskEvidence()
          .forEach(
              evidence -> text.append("Проверить: ").append(evidence.description()).append('\n'));
    }
    return text.toString();
  }

  public boolean current(final LaboratoryDrawing document) {
    if (!document.experiment().id().equals(this.input.experiment().id())
        || !document.experiment().stages().contains(this.stage)) {
      return false;
    }
    return this.sources.stream()
        .allMatch(source -> document.experiment().components().contains(source));
  }

  public void addToPlan(final LaboratoryEditor editor, final Result result, final int rank) {
    if (!this.id.equals(result.requestId())) {
      throw new IllegalArgumentException("Результат относится к другому запросу.");
    }
    if (!this.current(editor.document())) {
      throw new IllegalArgumentException(
          "Исходники или стадия изменились после прогноза. Запустите прогноз заново; прежний результат сохранён.");
    }
    if (result.status().equals("CANCELLED")
        || result.status().equals("FAILED")
        || rank < 0
        || rank >= result.candidates().size()) {
      throw new IllegalArgumentException("Выберите доступный вариант завершённого прогноза.");
    }
    applyPlan(
        editor,
        new LaboratoryExperiment(
            this.input.experiment().id(),
            this.input.experiment().title(),
            this.input.experiment().conditions(),
            this.input.experiment().notes(),
            this.sources,
            List.of(),
            List.of(),
            List.of(this.stage)),
        this.stage,
        this.id,
        result.file(),
        result.candidates().get(rank).products(),
        rank);
  }

  static boolean current(
      final LaboratoryDrawing document,
      final LaboratoryExperiment input,
      final LaboratoryReactionStage stage) {
    return document.experiment().id().equals(input.id())
        && document.experiment().stages().contains(stage)
        && input.components().stream()
            .allMatch(source -> document.experiment().components().contains(source));
  }

  static void applyPlan(
      final LaboratoryEditor editor,
      final LaboratoryExperiment input,
      final LaboratoryReactionStage stage,
      final String requestId,
      final Path file,
      final ReactionSide products,
      final int rank) {
    if (!current(editor.document(), input, stage)) {
      throw new IllegalArgumentException(
          "Исходники или стадия изменились. Сохранённый прогноз доступен для просмотра; для применения выполните новый.");
    }
    final LaboratoryEditor draft = new LaboratoryEditor();
    draft.open(editor.document());
    final List<LaboratoryStageParticipant> participants =
        new ArrayList<>(
            stage.participants().stream()
                .filter(participant -> participant.role() != ReactionComponentRole.PRODUCT)
                .toList());
    for (final var product : products.participants()) {
      final Molecule molecule =
          copyProduct(requestId, product.molecule(), rank, "m_" + UUID.randomUUID());
      final int count = draft.document().experiment().components().size();
      final var drawing =
          new LaboratoryMoleculePlacement()
              .place(molecule, 180 + count % 4 * 280, 170 + count / 4 * 280);
      final String component = draft.add(drawing, ReactionComponentRole.PRODUCT);
      participants.add(
          new LaboratoryStageParticipant(
              component, ReactionComponentRole.PRODUCT, product.coefficient()));
    }
    final List<LaboratoryReactionStage> stages =
        new ArrayList<>(draft.document().experiment().stages());
    stages.add(
        stages.indexOf(stage) + 1,
        new LaboratoryReactionStage(
            "stage_" + UUID.randomUUID(),
            stage.title() + " — вариант " + (rank + 1),
            stage.arrow(),
            stage.conditions(),
            participants,
            LaboratoryAtomMapping.EMPTY,
            stage.classification()));
    draft.stages(stages);
    editor.revise(
        draft.document(),
        "Добавлен плановый вариант "
            + (rank + 1)
            + " из "
            + file.getFileName()
            + ". Это гипотеза модели; фактические количества и склад не изменены.");
  }

  private Molecule copyProduct(final Molecule source, final int rank, final String id) {
    return copyProduct(this.id, source, rank, id);
  }

  private static Molecule copyProduct(
      final String requestId, final Molecule source, final int rank, final String id) {
    final Map<String, String> metadata = new LinkedHashMap<>(source.metadata().attributes());
    metadata.put("laboratory.prediction.source", requestId);
    metadata.put("laboratory.prediction.rank", Integer.toString(rank + 1));
    metadata.putIfAbsent("laboratory.prediction.original_molecule", source.id().value());
    return Molecule.of(
        MoleculeId.of(id),
        source.displayName(),
        source.atoms(),
        source.bonds(),
        source.charge(),
        source.spinMultiplicity(),
        source.stereochemistry(),
        source.conformation(),
        source.opticalRotation(),
        source.symmetry(),
        ChemistryMetadata.of(source.metadata().source(), source.metadata().location(), metadata));
  }
}
