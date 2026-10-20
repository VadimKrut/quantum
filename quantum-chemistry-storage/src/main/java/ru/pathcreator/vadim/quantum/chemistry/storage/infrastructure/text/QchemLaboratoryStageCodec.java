/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionStage;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAtomMapping;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionClassification;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryStageParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;

final class QchemLaboratoryStageCodec {
  private QchemLaboratoryStageCodec() {}

  static void write(final StringBuilder text, final List<LaboratoryReactionStage> stages) {
    for (final var stage : stages) {
      text.append("  stage id=")
          .append(stage.id())
          .append(" name=")
          .append(QchemEscaper.quote(stage.title()))
          .append(" arrow=")
          .append(stage.arrow().name());
      QchemLaboratoryCodec.writeConditions(text, stage.conditions());
      text.append("\r\n");
      writeClassification(text, stage);
      for (final var participant : stage.participants()) {
        text.append("  stage-member stage=")
            .append(stage.id())
            .append(" component=")
            .append(participant.componentId())
            .append(" role=")
            .append(participant.role().name());
        if (participant.coefficient() != null) {
          text.append(" coefficient=").append(participant.coefficient().value());
        }
        text.append("\r\n");
      }
      for (final var source :
          new java.util.TreeMap<>(stage.atomMapping().signatures()).entrySet()) {
        text.append("  stage-map-source stage=")
            .append(stage.id())
            .append(" component=")
            .append(source.getKey())
            .append(" signature=")
            .append(QchemEscaper.quote(source.getValue()))
            .append("\r\n");
      }
      for (final var label : stage.atomMapping().labels()) {
        text.append("  stage-map-atom stage=")
            .append(stage.id())
            .append(" component=")
            .append(label.componentId())
            .append(" atom=")
            .append(label.atomId().value())
            .append(" number=")
            .append(label.number())
            .append("\r\n");
      }
      for (final var condition : stage.conditions().components()) {
        final StringBuilder line = new StringBuilder();
        QchemTextWriter.writeConditionComponent(line, condition);
        text.append(
            line.toString()
                .replaceFirst("condition ", "stage-condition stage=" + stage.id() + " "));
      }
      for (final var feature : stage.conditions().features()) {
        final StringBuilder line = new StringBuilder();
        QchemTextWriter.writeConditionFeature(line, feature);
        text.append(
            line.toString().replaceFirst("feature ", "stage-feature stage=" + stage.id() + " "));
      }
    }
  }

  static List<LaboratoryReactionStage> read(final List<QchemLine> body) {
    final LinkedHashMap<String, QchemLine> headers = new LinkedHashMap<>();
    for (final QchemLine line : body) {
      if (line.command().equals("stage")
          && headers.putIfAbsent(line.required("id"), line) != null) {
        throw new IllegalArgumentException("Duplicate stage id.");
      }
    }
    for (final QchemLine line : body) {
      if (line.command().startsWith("stage-") && !headers.containsKey(line.required("stage"))) {
        throw new IllegalArgumentException("Unknown stage reference.");
      }
    }
    final ArrayList<LaboratoryReactionStage> stages = new ArrayList<>();
    headers.forEach(
        (id, header) -> {
          final ArrayList<LaboratoryStageParticipant> participants = new ArrayList<>();
          final ArrayList<LaboratoryReactionClassification.Term> names = new ArrayList<>(),
              mechanisms = new ArrayList<>();
          String classificationNote = null;
          final ArrayList<ReactionConditionComponent> conditions = new ArrayList<>();
          final ArrayList<ReactionConditionFeature> features = new ArrayList<>();
          final ArrayList<LaboratoryAtomMapping.Label> mapped = new ArrayList<>();
          final LinkedHashMap<String, String> signatures = new LinkedHashMap<>();
          for (final QchemLine line : body) {
            if (!line.command().startsWith("stage-") || !id.equals(line.required("stage"))) {
              continue;
            }
            switch (line.command()) {
              case "stage-classification-v1" -> {
                if (classificationNote != null) {
                  throw new IllegalArgumentException("Duplicate stage classification.");
                }
                requireKeys(line, java.util.Set.of("stage", "note"));
                classificationNote = QchemEscaper.unquote(line.required("note"));
              }
              case "stage-reaction-name-v1" -> readTerm(line, names);
              case "stage-mechanism-v1" -> readTerm(line, mechanisms);
              case "stage-member" ->
                  participants.add(
                      new LaboratoryStageParticipant(
                          line.required("component"),
                          ReactionComponentRole.valueOf(line.required("role")),
                          line.optional("coefficient") == null
                              ? null
                              : StoichiometricCoefficient.of(
                                  Integer.parseInt(line.required("coefficient")))));
              case "stage-map-source" -> {
                if (signatures.put(
                        line.required("component"),
                        QchemEscaper.unquote(line.required("signature")))
                    != null) {
                  throw new IllegalArgumentException("Duplicate mapped component signature.");
                }
              }
              case "stage-map-atom" ->
                  mapped.add(
                      new LaboratoryAtomMapping.Label(
                          line.required("component"),
                          AtomId.of(line.required("atom")),
                          Integer.parseInt(line.required("number"))));
              case "stage-condition" -> conditions.add(QchemTextReader.readCondition(line));
              case "stage-feature" -> features.add(QchemTextReader.readConditionFeature(line));
              default ->
                  throw new IllegalArgumentException("Unknown stage field: " + line.command());
            }
          }
          if (classificationNote == null && (!names.isEmpty() || !mechanisms.isEmpty())) {
            throw new IllegalArgumentException(
                "Classification terms require a stage-classification-v1 header.");
          }
          final var classification =
              classificationNote == null
                  ? LaboratoryReactionClassification.EMPTY
                  : new LaboratoryReactionClassification(names, mechanisms, classificationNote);
          stages.add(
              new LaboratoryReactionStage(
                  id,
                  QchemEscaper.unquote(header.required("name")),
                  LaboratoryReactionStage.Arrow.valueOf(header.required("arrow")),
                  QchemTextReader.readConditions(header, conditions, features),
                  participants,
                  new LaboratoryAtomMapping(mapped, signatures),
                  classification));
        });
    return List.copyOf(stages);
  }

  private static void writeClassification(
      final StringBuilder text, final LaboratoryReactionStage stage) {
    final var value = stage.classification();
    if (value.empty()) {
      return;
    }
    text.append("  stage-classification-v1 stage=")
        .append(stage.id())
        .append(" note=")
        .append(QchemEscaper.quote(value.note()))
        .append("\r\n");
    for (final var term : value.reactionNames()) {
      writeTerm(text, "stage-reaction-name-v1", stage.id(), term);
    }
    for (final var term : value.mechanisms()) {
      writeTerm(text, "stage-mechanism-v1", stage.id(), term);
    }
  }

  private static void writeTerm(
      final StringBuilder text,
      final String command,
      final String stage,
      final LaboratoryReactionClassification.Term term) {
    text.append("  ")
        .append(command)
        .append(" stage=")
        .append(stage)
        .append(" text=")
        .append(QchemEscaper.quote(term.text()));
    if (term.catalogId() != null) {
      text.append(" catalog=").append(QchemEscaper.quote(term.catalogId()));
    }
    text.append("\r\n");
  }

  private static void readTerm(
      final QchemLine line, final List<LaboratoryReactionClassification.Term> values) {
    if (values.size() >= LaboratoryReactionClassification.MAX_TERMS) {
      throw new IllegalArgumentException("Too many classification terms.");
    }
    requireKeys(
        line,
        line.optional("catalog") == null
            ? java.util.Set.of("stage", "text")
            : java.util.Set.of("stage", "text", "catalog"));
    values.add(
        new LaboratoryReactionClassification.Term(
            line.optional("catalog") == null
                ? null
                : QchemEscaper.unquote(line.required("catalog")),
            QchemEscaper.unquote(line.required("text"))));
  }

  private static void requireKeys(final QchemLine line, final java.util.Set<String> expected) {
    if (!line.values().keySet().equals(expected)) {
      throw new IllegalArgumentException("Unsupported classification fields.");
    }
  }
}
