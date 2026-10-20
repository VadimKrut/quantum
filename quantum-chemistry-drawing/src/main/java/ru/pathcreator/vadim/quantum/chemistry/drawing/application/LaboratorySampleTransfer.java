/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Plans both explicit physical ledger entries; persistence coordinates the separate files. */
public final class LaboratorySampleTransfer {

  private LaboratorySampleTransfer() {}

  public record Prepared(
      String id,
      String sourceSampleId,
      LaboratoryQuantity quantity,
      LaboratoryDrawing sourceBefore,
      LaboratoryDrawing sourceAfter,
      LaboratoryDrawing pendingTarget,
      LaboratoryDrawing target,
      List<String> warnings) {
    public Prepared {
      warnings = List.copyOf(warnings);
    }

    public LaboratorySample received() {
      return this.target.experiment().samples().getFirst();
    }

    public LaboratorySample.Event consumption() {
      return LaboratorySampleService.sample(this.sourceAfter.experiment(), this.sourceSampleId)
          .events()
          .getLast();
    }
  }

  public static Prepared prepare(
      final LaboratoryDrawing source,
      final String sampleId,
      final LaboratoryQuantity quantity,
      final String title,
      final String sourcePathHint,
      final Instant at) {
    final var experiment = source.experiment();
    final var sample = LaboratorySampleService.sample(experiment, sampleId);
    if (quantity == null || quantity.value().signum() <= 0) {
      throw new IllegalArgumentException("Укажите положительное количество передаваемого образца.");
    }
    if (quantity.value().precision() > 40 || Math.abs((long) quantity.value().scale()) > 24) {
      throw new IllegalArgumentException("Количество образца вне поддержанной точности.");
    }
    final var balance = sample.balance();
    if (balance.status() != LaboratorySample.Status.AVAILABLE || balance.remaining() == null) {
      throw new IllegalArgumentException("Нужен образец в работе с известным доступным остатком.");
    }
    if (quantity.in(balance.remaining().unit()).value().compareTo(balance.remaining().value())
        > 0) {
      throw new IllegalArgumentException("Передача превышает доступный остаток образца.");
    }
    if (title == null || title.isBlank() || title.length() > 1000) {
      throw new IllegalArgumentException("Введите название нового опыта длиной до 1000 символов.");
    }
    final Instant time =
        at.isBefore(sample.events().getLast().time()) ? sample.events().getLast().time() : at;
    final String id = "transfer_" + UUID.randomUUID(),
        targetId = "exp_" + UUID.randomUUID(),
        componentId = "component_" + UUID.randomUUID(),
        receivedId = "sample_" + UUID.randomUUID();
    final var molecule = LaboratoryAnalysisSummary.snapshot(sample.molecule(), receivedId);
    final var component =
        experiment.components().stream()
            .filter(c -> c.id().equals(sample.componentId()))
            .findFirst()
            .orElse(null);
    final var properties =
        component != null && component.properties().matches(sample.molecule())
            ? component.properties()
            : LaboratoryMaterialProperties.EMPTY;
    final List<String> warnings = new ArrayList<>();
    if (properties.empty()) {
      warnings.add(
          "Свойства материала не перенесены: для сохранённой структуры образца нет подтверждённых значений.");
    }
    if (sample.origin() != null || !sample.parents().isEmpty())
      warnings.add(
          "У образца есть предыдущее происхождение. Исходные анализы доступны через сохранённую цепочку; они не назначаются измерениями нового образца.");
    final var origin =
        new LaboratorySampleOrigin(
            id,
            new LaboratorySampleReference(experiment.id(), sample.id()),
            sample.code(),
            experiment.title(),
            sourcePathHint,
            java.util.stream.Stream.concat(
                    experiment.analyses().stream()
                        .filter(a -> sample.id().equals(a.sampleId()))
                        .map(LaboratoryAnalysis::id),
                    experiment.analyticalEvidence().entries().stream()
                        .filter(a -> sample.id().equals(a.sampleId()))
                        .map(a -> a.id()))
                .distinct()
                .toList());
    final String description =
        "Передано из опыта «"
            + experiment.title()
            + "», образец «"
            + sample.code()
            + "». Операция "
            + id
            + ".";
    final var received =
        new LaboratorySample(
            receivedId,
            sample.code(),
            componentId,
            null,
            "",
            LaboratorySample.Kind.TRANSFERRED,
            molecule,
            List.of(),
            List.of(
                new LaboratorySample.Event(
                    id + "_in",
                    time,
                    LaboratorySample.Action.REGISTER,
                    quantity,
                    sample.balance().label(),
                    "",
                    "",
                    description,
                    null)),
            origin);
    final var targetComponent =
        new LaboratoryComponent(
            componentId,
            molecule,
            ReactionComponentRole.REACTANT,
            quantity,
            null,
            LaboratoryEvidence.PLANNED,
            properties);
    final String notes =
        description
            + "\nПолучение образца зарегистрировано отдельно от загрузки реакции. Фактическое количество исходника не назначено."
            + "\nСвязанных анализов в исходном опыте: "
            + origin.analysisIds().size()
            + ". Оригиналы и вложения остаются в исходном опыте.";
    final var targetExperiment =
        new LaboratoryExperiment(
            targetId,
            title.strip(),
            ReactionConditions.EMPTY,
            notes,
            List.of(targetComponent),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(received));
    final var originalLayout = source.layouts().get(sample.componentId());
    final MoleculeDrawing layout =
        originalLayout != null
                && LaboratoryMaterialProperties.signature(originalLayout.molecule())
                    .equals(LaboratoryMaterialProperties.signature(sample.molecule()))
            ? MoleculeDrawing.of(
                molecule,
                originalLayout.atoms(),
                originalLayout.bonds(),
                originalLayout.manualFields())
            : new LaboratoryMoleculePlacement().place(molecule, 200, 200);
    final var target = new LaboratoryDrawing(targetExperiment, Map.of(componentId, layout));
    final var consumption =
        new LaboratorySample.Event(
            id + "_out",
            time,
            LaboratorySample.Action.CONSUME,
            quantity,
            "",
            "",
            "",
            "Передача в новый опыт «"
                + title.strip()
                + "» ("
                + targetId
                + "), образец «"
                + received.code()
                + "». Операция "
                + id
                + ".",
            null);
    final var samples = new ArrayList<>(experiment.samples());
    samples.replaceAll(s -> s.id().equals(sampleId) ? s.append(consumption) : s);
    LaboratorySampleLedger.requireAppend(experiment.samples(), samples);
    final var journal = new ArrayList<>(experiment.journal());
    journal.add(
        new LaboratoryEntry(
            id + "_journal",
            time,
            LaboratoryEntry.Kind.ACTION,
            consumption.note() + " Количество: " + quantity + ".",
            null));
    final var after =
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
            samples,
            experiment.procedure(),
            experiment.route(),
            experiment.operations(),
            experiment.analyticalEvidence(),
            experiment.literature());
    final var pending =
        new LaboratoryExperiment(
            targetId,
            title.strip(),
            ReactionConditions.EMPTY,
            "ПЕРЕДАЧА ЕЩЁ НЕ ЗАВЕРШЕНА. Образец и его получение здесь не зарегистрированы.\n"
                + description
                + "\nВернитесь к передаче образца в исходном опыте для завершения. Запись восстановления сохраняется рядом с исходным QCHEM.",
            List.of());
    return new Prepared(
        id,
        sampleId,
        quantity,
        source,
        new LaboratoryDrawing(after, source.layouts()),
        new LaboratoryDrawing(pending, Map.of()),
        target,
        warnings);
  }
}
