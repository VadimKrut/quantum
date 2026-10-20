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
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Explicit physical relocation, never a copy of a component's reported or planned quantity. */
public final class LaboratorySampleStockTransfer {
  private LaboratorySampleStockTransfer() {}

  public record Prepared(
      String id,
      LaboratoryQuantity quantity,
      LaboratoryDrawing before,
      LaboratoryDrawing after,
      LaboratoryMaterial material,
      LaboratoryBatch batch,
      LaboratoryStockOperation receipt) {
    public LaboratoryInventory received() {
      return LaboratoryInventory.empty().receive(this.material, this.batch, this.receipt);
    }
  }

  public static Prepared prepare(
      final LaboratoryDrawing drawing,
      final String sampleId,
      final LaboratoryQuantity quantity,
      final String label,
      final LaboratoryMaterial.Kind kind,
      final String location,
      final String sourcePath,
      final Instant at) {
    final var e = drawing.experiment();
    final var sample = LaboratorySampleService.sample(e, sampleId);
    final var balance = sample.balance();
    if (quantity == null
        || quantity.value().signum() <= 0
        || quantity.value().precision() > 40
        || Math.abs((long) quantity.value().scale()) > 24)
      throw new IllegalArgumentException(
          "Укажите положительное количество образца в допустимой точности.");
    if (balance.status() != LaboratorySample.Status.AVAILABLE || balance.remaining() == null)
      throw new IllegalArgumentException(
          "Нужен доступный физический образец с известным остатком.");
    if (quantity.in(balance.remaining().unit()).value().compareTo(balance.remaining().value()) > 0)
      throw new IllegalArgumentException("Передача превышает остаток образца.");
    if (label == null
        || label.isBlank()
        || label.length() > 500
        || location == null
        || location.length() > 500)
      throw new IllegalArgumentException("Укажите название партии и место длиной до500 символов.");
    Objects.requireNonNull(kind);
    final String id = "sample_stock_" + UUID.randomUUID();
    final Instant time =
        at.isBefore(sample.events().getLast().time()) ? sample.events().getLast().time() : at;
    final var molecule = LaboratoryAnalysisSummary.snapshot(sample.molecule(), id);
    final var material =
        new LaboratoryMaterial(id + "_material", sample.molecule().displayName(), kind, molecule);
    final var origins = new LinkedHashMap<String, LaboratorySampleOrigin>();
    final var pending = new ArrayDeque<String>();
    final Set<String> visited = new HashSet<>();
    pending.add(sampleId);
    while (!pending.isEmpty()) {
      final String next = pending.removeFirst();
      if (!visited.add(next)) continue;
      final var s = LaboratorySampleService.sample(e, next);
      final List<String> analyses =
          java.util.stream.Stream.concat(
                  e.analyses().stream()
                      .filter(a -> s.id().equals(a.sampleId()))
                      .map(LaboratoryAnalysis::id),
                  e.analyticalEvidence().entries().stream()
                      .filter(a -> s.id().equals(a.sampleId()))
                      .map(a -> a.id()))
              .distinct()
              .toList();
      final var origin =
          new LaboratorySampleOrigin(
              id,
              new LaboratorySampleReference(e.id(), s.id()),
              s.code(),
              e.title(),
              sourcePath,
              analyses);
      origins.putIfAbsent(origin.source().token(), origin);
      if (s.origin() != null) origins.putIfAbsent(s.origin().source().token(), s.origin());
      pending.addAll(s.parents());
      if (origins.size() > 1000)
        throw new IllegalArgumentException("Слишком длинная цепочка происхождения образца.");
    }
    final String detail =
        "Передача физического образца «"
            + sample.code()
            + "» в партию «"
            + label.strip()
            + "». Операция "
            + id
            + ".";
    final var batch =
        new LaboratoryBatch(
            id + "_batch",
            material.id(),
            label.strip(),
            quantity.unit(),
            null,
            "",
            detail,
            location,
            null,
            null,
            e.id(),
            new LaboratoryBatchOrigin(List.copyOf(origins.values()), null, null));
    final var receipt =
        new LaboratoryStockOperation(
            id + "_in",
            batch.id(),
            time,
            LaboratoryStockOperation.Kind.RECEIPT,
            quantity,
            e.id(),
            null,
            detail);
    final var event =
        new LaboratorySample.Event(
            id + "_out", time, LaboratorySample.Action.CONSUME, quantity, "", "", "", detail, null);
    final var samples = new ArrayList<>(e.samples());
    samples.replaceAll(s -> s.id().equals(sampleId) ? s.append(event) : s);
    LaboratorySampleLedger.requireAppend(e.samples(), samples);
    final var journal = new ArrayList<>(e.journal());
    journal.add(
        new LaboratoryEntry(
            id + "_journal",
            time,
            LaboratoryEntry.Kind.ACTION,
            detail + " Количество: " + quantity + ". Дальнейший расход ведётся в учёте партий.",
            null));
    final var after =
        new LaboratoryExperiment(
            e.id(),
            e.title(),
            e.conditions(),
            e.notes(),
            e.components(),
            journal,
            e.attachments(),
            e.stages(),
            e.analyses(),
            samples,
            e.procedure(),
            e.route(),
            e.operations(),
            e.analyticalEvidence(),
            e.literature());
    return new Prepared(
        id,
        quantity,
        drawing,
        new LaboratoryDrawing(after, drawing.layouts()),
        material,
        batch,
        receipt);
  }
}
