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
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.math.BigDecimal;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAttachment;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Native QCHEM laboratory section; molecule data uses the existing molecule codec. */
final class QchemLaboratoryCodec {

  private QchemLaboratoryCodec() {}

  static void write(final StringBuilder text, final LaboratoryExperiment experiment) {
    final ReactionConditions conditions = experiment.conditions();
    text.append("laboratory-experiment id=")
        .append(experiment.id())
        .append(" name=")
        .append(QchemEscaper.quote(experiment.title()))
        .append(" notes=")
        .append(QchemEscaper.quote(experiment.notes()));
    writeConditions(text, conditions);
    text.append("\r\n");
    for (final LaboratoryComponent component : experiment.components()) {
      text.append("  component id=")
          .append(component.id())
          .append(" molecule=")
          .append(component.molecule().id().value())
          .append(" role=")
          .append(component.role())
          .append(" evidence=")
          .append(component.evidence().name());
      writeQuantity(text, "planned", component.planned());
      writeQuantity(text, "actual", component.actual());
      text.append("\r\n");
      QchemLaboratoryPropertiesCodec.write(text, component);
    }
    for (final ReactionConditionComponent component : conditions.components()) {
      QchemTextWriter.writeConditionComponent(text, component);
    }
    for (final ReactionConditionFeature feature : conditions.features()) {
      QchemTextWriter.writeConditionFeature(text, feature);
    }
    for (final LaboratoryEntry entry : experiment.journal()) {
      text.append("  entry id=")
          .append(entry.id())
          .append(" time=")
          .append(entry.recordedAt())
          .append(" kind=")
          .append(entry.kind().name())
          .append(" text=")
          .append(QchemEscaper.quote(entry.text()))
          .append(" corrects=")
          .append(entry.correctsId() == null ? "none" : entry.correctsId())
          .append("\r\n");
    }
    for (final LaboratoryAttachment attachment : experiment.attachments()) {
      text.append("  attachment id=")
          .append(attachment.id())
          .append(" name=")
          .append(QchemEscaper.quote(attachment.name()))
          .append(" hash=")
          .append(attachment.sha256())
          .append(" size=")
          .append(attachment.size())
          .append("\r\n");
    }
    QchemLaboratoryStageCodec.write(text, experiment.stages());
    QchemLaboratorySampleCodec.write(text, experiment.samples());
    QchemLaboratoryAnalysisCodec.write(text, experiment.analyses());
    QchemLaboratoryProcedureCodec.write(text, experiment.procedure());
    QchemLaboratoryRouteCodec.write(text, experiment.route());
    QchemLaboratoryOperationsCodec.write(text, experiment.operations());
    QchemLaboratoryEvidenceCodec.write(text, experiment.analyticalEvidence());
    QchemLaboratoryLiteratureCodec.write(text, experiment.literature());
    text.append("end\r\n");
  }

  static LaboratoryExperiment read(
      final QchemLine header, final List<QchemLine> body, final Map<String, Molecule> molecules) {
    final ArrayList<LaboratoryEntry> journal = new ArrayList<>();
    final ArrayList<LaboratoryAttachment> attachments = new ArrayList<>();
    final ArrayList<LaboratoryComponent> components = new ArrayList<>();
    final ArrayList<ReactionConditionComponent> conditions = new ArrayList<>();
    final ArrayList<ReactionConditionFeature> features = new ArrayList<>();
    final var properties = QchemLaboratoryPropertiesCodec.read(body);
    for (final QchemLine line : body) {
      if (line.command().startsWith("literature-")) {
        continue;
      }
      if (line.command().startsWith("operations-") || line.command().startsWith("evidence-")) {
        continue;
      }
      if (line.command().startsWith("procedure-")) {
        continue;
      }
      if (line.command().startsWith("route-")) {
        continue;
      }
      if (line.command().startsWith("sample-")) {
        continue;
      }
      if (line.command().startsWith("analysis-")) {
        continue;
      }
      if (line.command().equals("component-properties-v1")
          || line.command().equals("stage")
          || line.command().startsWith("stage-")) {
        continue;
      }
      switch (line.command()) {
        case "component" -> {
          final Molecule molecule = molecules.get(line.required("molecule"));
          if (molecule == null) {
            throw new IllegalArgumentException("Unknown laboratory molecule.");
          }
          components.add(
              new LaboratoryComponent(
                  line.required("id"),
                  molecule,
                  ReactionComponentRole.valueOf(line.required("role")),
                  readQuantity(line, "planned"),
                  readQuantity(line, "actual"),
                  line.optional("evidence") == null
                      ? LaboratoryEvidence.PLANNED
                      : LaboratoryEvidence.valueOf(line.required("evidence")),
                  properties.getOrDefault(
                      line.required("id"),
                      ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                          .LaboratoryMaterialProperties.EMPTY)));
        }
        case "entry" ->
            journal.add(
                new LaboratoryEntry(
                    line.required("id"),
                    Instant.parse(line.required("time")),
                    LaboratoryEntry.Kind.valueOf(line.required("kind")),
                    QchemEscaper.unquote(line.required("text")),
                    line.required("corrects").equals("none") ? null : line.required("corrects")));
        case "attachment" ->
            attachments.add(
                new LaboratoryAttachment(
                    line.required("id"),
                    QchemEscaper.unquote(line.required("name")),
                    line.required("hash"),
                    Long.parseLong(line.required("size"))));
        case "condition" -> conditions.add(QchemTextReader.readCondition(line));
        case "feature" -> features.add(QchemTextReader.readConditionFeature(line));
        default ->
            throw new IllegalArgumentException("Unknown laboratory field: " + line.command());
      }
    }
    if (properties.keySet().stream()
        .anyMatch(id -> components.stream().noneMatch(component -> component.id().equals(id)))) {
      throw new IllegalArgumentException("Свойства ссылаются на отсутствующий компонент.");
    }
    return new LaboratoryExperiment(
        header.required("id"),
        QchemEscaper.unquote(header.required("name")),
        QchemTextReader.readConditions(header, conditions, features),
        QchemEscaper.unquote(header.required("notes")),
        components,
        journal,
        attachments,
        QchemLaboratoryStageCodec.read(body),
        QchemLaboratoryAnalysisCodec.read(body, molecules),
        QchemLaboratorySampleCodec.read(body, molecules),
        QchemLaboratoryProcedureCodec.read(body),
        QchemLaboratoryRouteCodec.read(body),
        QchemLaboratoryOperationsCodec.read(body, molecules),
        QchemLaboratoryEvidenceCodec.read(body, molecules),
        QchemLaboratoryLiteratureCodec.read(body));
  }

  private static void writeQuantity(
      final StringBuilder text, final String key, final LaboratoryQuantity quantity) {
    if (quantity != null) {
      text.append(" ")
          .append(key)
          .append("=")
          .append(quantity.value().toPlainString())
          .append(" ")
          .append(key)
          .append("Unit=")
          .append(quantity.unit().name());
    }
  }

  private static LaboratoryQuantity readQuantity(final QchemLine line, final String key) {
    return line.optional(key) == null
        ? null
        : new LaboratoryQuantity(
            new BigDecimal(line.required(key)),
            LaboratoryQuantityUnit.valueOf(line.required(key + "Unit")));
  }

  static void writeConditions(final StringBuilder text, final ReactionConditions conditions) {
    text.append(" note=").append(QchemEscaper.quote(conditions.note()));
    if (conditions.temperature() != null) {
      text.append(" temperature=")
          .append(conditions.temperature().value())
          .append(" temperatureUnit=")
          .append(conditions.temperature().unit());
    }
    if (conditions.pressure() != null) {
      text.append(" pressure=")
          .append(conditions.pressure().value())
          .append(" pressureUnit=")
          .append(conditions.pressure().unit());
    }
    if (conditions.duration() != null) {
      text.append(" duration=")
          .append(conditions.duration().value())
          .append(" durationUnit=")
          .append(conditions.duration().unit());
    }
  }
}
