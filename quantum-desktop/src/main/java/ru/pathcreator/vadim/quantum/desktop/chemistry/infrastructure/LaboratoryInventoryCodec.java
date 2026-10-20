/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryBatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryBatchOrigin;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleOrigin;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleReference;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryInventory;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryMaterial;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryReservationEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;

/** Versioned QCHEM adapters for the inventory aggregate; molecular structures use the native codec. */
public final class LaboratoryInventoryCodec {
  private final ChemistryStorageService storage = new ChemistryStorageService();

  public String write(final LaboratoryInventory inventory) {
    final var builder =
        ChemistryStorageDocument.builder("laboratory_inventory", "Материалы и партии");
    final Map<String, Molecule> molecules = new LinkedHashMap<>();
    for (final var material : inventory.materials()) {
      final Map<String, String> fields = new LinkedHashMap<>();
      fields.put("name", material.name());
      fields.put("type", material.kind().name());
      if (material.molecule() != null) {
        final var previous =
            molecules.putIfAbsent(material.molecule().id().value(), material.molecule());
        if (previous != null && !previous.equals(material.molecule())) {
          throw new IllegalArgumentException("Разные структуры с одним идентификатором.");
        }
        fields.put("molecule", material.molecule().id().value());
      }
      builder.extension(
          ChemistryStorageExtension.of("laboratory-material-v1", material.id(), fields, List.of()));
    }
    molecules.values().forEach(builder::molecule);
    for (final var batch : inventory.batches()) {
      final Map<String, String> fields = new LinkedHashMap<>();
      fields.put("material", batch.materialId());
      fields.put("label", batch.label());
      fields.put("unit", batch.unit().name());
      put(fields, "purity", batch.purityPercent());
      put(fields, "concentration", batch.concentration());
      put(fields, "source", batch.source());
      put(fields, "location", batch.location());
      put(fields, "price", batch.pricePerUnit());
      put(fields, "currency", batch.currency());
      put(fields, "experiment", batch.sourceExperimentId());
      if (batch.origin() != null) writeOrigin(fields, batch.origin());
      builder.extension(
          ChemistryStorageExtension.of(
              batch.origin() == null ? "laboratory-batch-v1" : "laboratory-batch-v2",
              batch.id(),
              fields,
              List.of()));
    }
    for (final var operation : inventory.operations()) {
      final Map<String, String> fields = new LinkedHashMap<>();
      fields.put("batch", operation.batchId());
      fields.put("time", operation.recordedAt().toString());
      fields.put("type", operation.kind().name());
      fields.put("value", operation.quantity().value().toPlainString());
      fields.put("unit", operation.quantity().unit().name());
      fields.put("reason", operation.reason());
      put(fields, "experiment", operation.experimentId());
      put(fields, "returns", operation.returnsOperationId());
      if (operation.valuation() != null) {
        put(fields, "price-unit", operation.valuation().unit().name());
        put(fields, "price", operation.valuation().pricePerUnit());
        put(fields, "currency", operation.valuation().currency());
      }
      builder.extension(
          ChemistryStorageExtension.of(
              operation.valuation() == null
                  ? "laboratory-stock-operation-v1"
                  : "laboratory-stock-operation-v2",
              operation.id(),
              fields,
              List.of()));
    }
    for (final var event : inventory.reservations()) {
      final Map<String, String> p = new LinkedHashMap<>();
      p.put("reservation", event.reservationId());
      p.put("time", event.recordedAt().toString());
      p.put("type", event.kind().name());
      p.put("index", Integer.toString(event.operationIndex()));
      p.put("batch", event.batchId());
      p.put("experiment", event.target().experimentId());
      p.put("stage", event.target().stageId());
      p.put("component", event.target().componentId());
      p.put("description", event.target().description());
      p.put("signature", event.target().signature());
      p.put("value", event.quantity().value().toPlainString());
      p.put("unit", event.quantity().unit().name());
      p.put("reason", event.reason());
      put(p, "use", event.useOperationId());
      builder.extension(
          ChemistryStorageExtension.of("laboratory-reservation-v1", event.id(), p, List.of()));
    }
    final var written = this.storage.write(builder.build());
    if (!written.success()) {
      throw new IllegalArgumentException("QCHEM: " + written.diagnostics());
    }
    return written.value();
  }

  public LaboratoryInventory read(final String text) {
    final var parsed = this.storage.read(text);
    if (!parsed.success()) {
      throw new IllegalArgumentException("QCHEM: " + parsed.diagnostics());
    }
    final var source = parsed.value();
    final Map<String, Molecule> molecules = new LinkedHashMap<>();
    source.molecules().forEach(molecule -> molecules.put(molecule.id().value(), molecule));
    final List<LaboratoryMaterial> materials = new ArrayList<>();
    final List<LaboratoryBatch> batches = new ArrayList<>();
    final List<LaboratoryStockOperation> operations = new ArrayList<>();
    final List<LaboratoryReservationEvent> reservations = new ArrayList<>();
    for (final var extension : source.extensions()) {
      final var p = extension.properties();
      switch (extension.kind()) {
        case "laboratory-material-v1" -> {
          final Molecule molecule =
              p.containsKey("molecule") ? molecules.get(p.get("molecule")) : null;
          if (p.containsKey("molecule") && molecule == null) {
            throw new IllegalArgumentException("Структура материала не найдена.");
          }
          materials.add(
              new LaboratoryMaterial(
                  extension.id(),
                  p.get("name"),
                  LaboratoryMaterial.Kind.valueOf(p.get("type")),
                  molecule));
        }
        case "laboratory-batch-v1", "laboratory-batch-v2" ->
            batches.add(
                new LaboratoryBatch(
                    extension.id(),
                    p.get("material"),
                    p.get("label"),
                    LaboratoryQuantityUnit.valueOf(p.get("unit")),
                    decimal(p, "purity"),
                    p.get("concentration"),
                    p.get("source"),
                    p.get("location"),
                    decimal(p, "price"),
                    p.get("currency"),
                    p.get("experiment"),
                    extension.kind().endsWith("-v2") ? readOrigin(p) : null));
        case "laboratory-stock-operation-v1", "laboratory-stock-operation-v2" ->
            operations.add(
                new LaboratoryStockOperation(
                    extension.id(),
                    p.get("batch"),
                    Instant.parse(p.get("time")),
                    LaboratoryStockOperation.Kind.valueOf(p.get("type")),
                    new LaboratoryQuantity(
                        new BigDecimal(p.get("value")),
                        LaboratoryQuantityUnit.valueOf(p.get("unit"))),
                    p.get("experiment"),
                    p.get("returns"),
                    p.get("reason"),
                    extension.kind().endsWith("-v1")
                        ? null
                        : new LaboratoryStockOperation.Valuation(
                            decimal(p, "price"),
                            LaboratoryQuantityUnit.valueOf(p.get("price-unit")),
                            p.get("currency"))));
        case "laboratory-reservation-v1" ->
            reservations.add(
                new LaboratoryReservationEvent(
                    extension.id(),
                    p.get("reservation"),
                    Instant.parse(p.get("time")),
                    LaboratoryReservationEvent.Kind.valueOf(p.get("type")),
                    Integer.parseInt(p.get("index")),
                    p.get("batch"),
                    new LaboratoryReservationEvent.Target(
                        p.get("experiment"),
                        p.get("stage"),
                        p.get("component"),
                        p.get("description"),
                        p.get("signature")),
                    new LaboratoryQuantity(
                        new BigDecimal(p.get("value")),
                        LaboratoryQuantityUnit.valueOf(p.get("unit"))),
                    p.get("use"),
                    p.get("reason")));
        default ->
            throw new IllegalArgumentException("Неизвестный раздел учёта: " + extension.kind());
      }
    }
    final var inventory = new LaboratoryInventory(materials, batches, operations, reservations);
    final var rebuilt = this.storage.read(this.write(inventory));
    if (!rebuilt.success() || !source.equals(rebuilt.value())) {
      throw new IllegalArgumentException("Файл учёта содержит неподдерживаемые данные.");
    }
    return inventory;
  }

  private static void writeOrigin(
      final Map<String, String> fields, final LaboratoryBatchOrigin origin) {
    if (origin.parentBatchId() != null) {
      fields.put("origin-parent", origin.parentBatchId());
      fields.put("origin-recipe", origin.recipeId());
      return;
    }
    fields.put("origin-count", Integer.toString(origin.sourceSamples().size()));
    for (int i = 0; i < origin.sourceSamples().size(); i++) {
      final var v = origin.sourceSamples().get(i);
      final String p = "origin-" + i + "-";
      fields.put(p + "transfer", v.transferId());
      fields.put(p + "experiment", v.source().experimentId());
      fields.put(p + "sample", v.source().sampleId());
      fields.put(p + "code", v.sourceCode());
      fields.put(p + "title", v.sourceTitle());
      fields.put(p + "path", v.sourcePathHint());
      fields.put(p + "analyses", Integer.toString(v.analysisIds().size()));
      for (int j = 0; j < v.analysisIds().size(); j++)
        fields.put(p + "analysis-" + j, v.analysisIds().get(j));
    }
  }

  private static LaboratoryBatchOrigin readOrigin(final Map<String, String> fields) {
    if (fields.containsKey("origin-parent"))
      return new LaboratoryBatchOrigin(
          List.of(), fields.get("origin-parent"), fields.get("origin-recipe"));
    final int count = Integer.parseInt(fields.get("origin-count"));
    if (count < 1 || count > 1000)
      throw new IllegalArgumentException("Неверное число исходных образцов партии.");
    final List<LaboratorySampleOrigin> samples = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final String p = "origin-" + i + "-";
      final int n = Integer.parseInt(fields.get(p + "analyses"));
      if (n < 0 || n > 10000)
        throw new IllegalArgumentException("Неверное число исходных анализов.");
      final List<String> analyses = new ArrayList<>();
      for (int j = 0; j < n; j++) analyses.add(fields.get(p + "analysis-" + j));
      samples.add(
          new LaboratorySampleOrigin(
              fields.get(p + "transfer"),
              new LaboratorySampleReference(fields.get(p + "experiment"), fields.get(p + "sample")),
              fields.get(p + "code"),
              fields.get(p + "title"),
              fields.get(p + "path"),
              analyses));
    }
    return new LaboratoryBatchOrigin(samples, null, null);
  }

  private static BigDecimal decimal(final Map<String, String> map, final String key) {
    return map.containsKey(key) ? new BigDecimal(map.get(key)) : null;
  }

  private static void put(final Map<String, String> map, final String key, final Object value) {
    if (value != null && !value.toString().isBlank()) {
      map.put(key, value instanceof BigDecimal number ? number.toPlainString() : value.toString());
    }
  }
}
