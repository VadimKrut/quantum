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
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** Read a saved prediction without rerunning the engine or reconstructing missing mechanism objects. */
public final class LaboratoryPredictionArchive {
  public record Candidate(
      ReactionSide products, ChemistryStoragePredictionCandidateRecord record) {}

  private final ChemistryStorageDocument document;
  private final LaboratoryReactionStage stage;
  private final List<Candidate> candidates;
  private final Path file;

  public LaboratoryPredictionArchive(final ChemistryStorageDocument document, final Path file) {
    this.document = Objects.requireNonNull(document);
    this.file = Objects.requireNonNull(file);
    if (!"studio-prediction-1".equals(document.metadata().get("prediction.adapter_version"))) {
      throw new IllegalArgumentException(
          "Версия сохранённого прогноза не поддержана. Можно просмотреть исходный QCHEM.");
    }
    if (document.laboratoryExperiment() == null
        || document.laboratoryExperiment().stages().size() != 1) {
      throw new IllegalArgumentException(
          "В прогнозе отсутствует однозначный снимок исходной стадии.");
    }
    this.stage = document.laboratoryExperiment().stages().getFirst();
    if (!this.stage.id().equals(document.metadata().get("prediction.stage"))
        || !Set.of("SUCCESS", "PARTIAL", "FAILED", "CANCELLED").contains(this.status())
        || !document.metadata().containsKey("prediction.report")
        || document.predictionRecords().size() > 1) {
      throw new IllegalArgumentException(
          "Сохранённый прогноз содержит противоречивые служебные данные.");
    }
    final Map<String, Molecule> molecules = new HashMap<>();
    document.molecules().forEach(molecule -> molecules.put(molecule.id().value(), molecule));
    final List<Candidate> restored = new ArrayList<>();
    for (final var prediction : document.predictionRecords()) {
      if (!document.projectId().equals(prediction.requestId())) {
        throw new IllegalArgumentException("Запись прогноза относится к другому запросу.");
      }
      final var expectedStatus =
          this.status().equals("FAILED")
              ? ChemistryStorageReactionRecordStatus.FAILED
              : ChemistryStorageReactionRecordStatus.SIMULATED_CLASSICAL;
      if (this.status().equals("CANCELLED") || prediction.status() != expectedStatus) {
        throw new IllegalArgumentException(
            "Статус записи не соответствует статусу сохранённого прогноза.");
      }
      for (final var candidate : prediction.candidates()) {
        if (candidate.rank() != restored.size()
            || candidate.productMoleculeIds().isEmpty()
            || candidate.productMoleculeIds().size()
                != candidate.productStoichiometrySignatures().size()) {
          throw new IllegalArgumentException(
              "Вариант прогноза имеет неполные продукты или коэффициенты.");
        }
        final List<ReactionParticipant> products = new ArrayList<>();
        for (int index = 0; index < candidate.productMoleculeIds().size(); index++) {
          final String id = candidate.productMoleculeIds().get(index);
          final Molecule molecule = molecules.get(id);
          if (molecule == null
              || !document
                  .projectId()
                  .equals(molecule.metadata().attributes().get("laboratory.prediction.source"))
              || !Integer.toString(candidate.rank() + 1)
                  .equals(molecule.metadata().attributes().get("laboratory.prediction.rank"))) {
            throw new IllegalArgumentException(
                "Граф продукта отсутствует или относится к другому варианту.");
          }
          final String signature = candidate.productStoichiometrySignatures().get(index);
          final String suffix = "*" + id + ":" + molecule.formula().hillNotation();
          if (!signature.endsWith(suffix)) {
            throw new IllegalArgumentException(
                "Коэффициент продукта не соответствует сохранённому графу.");
          }
          final String value = signature.substring(0, signature.length() - suffix.length());
          if (!value.matches("[1-9][0-9]{0,6}")) {
            throw new IllegalArgumentException("В прогнозе недопустимый коэффициент продукта.");
          }
          products.add(
              ReactionParticipant.of(
                  molecule, StoichiometricCoefficient.of(Integer.parseInt(value))));
        }
        restored.add(new Candidate(ReactionSide.of(products), candidate));
      }
    }
    this.candidates = List.copyOf(restored);
  }

  public String title() {
    return this.document.displayName();
  }

  public String requestId() {
    return this.document.projectId();
  }

  public String status() {
    return this.document.metadata().getOrDefault("prediction.status", "");
  }

  public String inputReview() {
    return this.document
        .metadata()
        .getOrDefault(
            "prediction.input_review",
            "Краткая проверка входа не сохранена в этой версии. Прочтите исходную стадию и полную диагностику.");
  }

  public String report() {
    return this.document.metadata().get("prediction.report");
  }

  public LaboratoryExperiment input() {
    return this.document.laboratoryExperiment();
  }

  public LaboratoryReactionStage stage() {
    return this.stage;
  }

  public List<Candidate> candidates() {
    return this.candidates;
  }

  public boolean hasConfidence() {
    return Arrays.stream(ReactionPredictionConfidenceLevel.values())
        .anyMatch(
            value -> value.name().equals(this.document.metadata().get("prediction.confidence")));
  }

  public ReactionPredictionConfidenceLevel confidence() {
    try {
      return ReactionPredictionConfidenceLevel.valueOf(
          this.document.metadata().getOrDefault("prediction.confidence", "NONE"));
    } catch (final IllegalArgumentException exception) {
      return ReactionPredictionConfidenceLevel.NONE;
    }
  }

  public boolean current(final LaboratoryDrawing drawing) {
    return LaboratoryPredictionJob.current(
        drawing, this.document.laboratoryExperiment(), this.stage);
  }

  public boolean applicable(final LaboratoryDrawing drawing) {
    return (this.status().equals("SUCCESS") || this.status().equals("PARTIAL"))
        && !this.candidates.isEmpty()
        && this.current(drawing);
  }

  public void addToPlan(final LaboratoryEditor editor, final int rank) {
    if (!this.applicable(editor.document()) || rank < 0 || rank >= this.candidates.size()) {
      throw new IllegalArgumentException(
          "Этот прогноз нельзя применить: проверьте его статус и соответствие исходной стадии текущему опыту.");
    }
    LaboratoryPredictionJob.applyPlan(
        editor,
        this.document.laboratoryExperiment(),
        this.stage,
        this.requestId(),
        this.file,
        this.candidates.get(rank).products(),
        rank);
  }
}
