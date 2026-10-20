/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;

/** Immutable append-only ledger. Reopening a document or undoing its layout never changes stock. */
public record LaboratoryInventory(
    List<LaboratoryMaterial> materials,
    List<LaboratoryBatch> batches,
    List<LaboratoryStockOperation> operations,
    List<LaboratoryReservationEvent> reservations) {
  public LaboratoryInventory(
      final List<LaboratoryMaterial> materials,
      final List<LaboratoryBatch> batches,
      final List<LaboratoryStockOperation> operations) {
    this(materials, batches, operations, List.of());
  }

  public LaboratoryInventory {
    materials = List.copyOf(materials);
    batches = List.copyOf(batches);
    operations = List.copyOf(operations);
    reservations = List.copyOf(reservations);
    final Map<String, LaboratoryMaterial> materialIndex = new HashMap<>();
    for (final var material : materials) {
      if (materialIndex.put(material.id(), material) != null) {
        throw new IllegalArgumentException("Повтор материала.");
      }
    }
    final Map<String, LaboratoryBatch> batchIndex = new HashMap<>();
    for (final var batch : batches) {
      if (!materialIndex.containsKey(batch.materialId())) {
        throw new IllegalArgumentException("Материал партии не найден.");
      }
      if (batch.origin() != null) {
        final var origin = batch.origin();
        if (origin.parentBatchId() != null && !batchIndex.containsKey(origin.parentBatchId()))
          throw new IllegalArgumentException(
              "Исходная партия приготовления должна уже существовать в истории.");
        if (origin.parentBatchId() == null
            && !Objects.equals(
                batch.sourceExperimentId(),
                origin.sourceSamples().getFirst().source().experimentId()))
          throw new IllegalArgumentException(
              "Опыт происхождения партии не соответствует исходному образцу.");
      }
      if (batchIndex.put(batch.id(), batch) != null) {
        throw new IllegalArgumentException("Повтор партии.");
      }
    }
    final Map<String, LaboratoryStockOperation> seen = new HashMap<>();
    final Map<String, BigDecimal> balances = new HashMap<>();
    final Map<String, BigDecimal> returns = new HashMap<>();
    for (final var operation : operations) {
      final var batch = batchIndex.get(operation.batchId());
      if (batch == null) {
        throw new IllegalArgumentException("Партия операции не найдена.");
      }
      final BigDecimal value = operation.quantity().in(batch.unit()).value();
      if (seen.containsKey(operation.id())) {
        throw new IllegalArgumentException("Повтор идентификатора операции.");
      }
      if (operation.kind() == LaboratoryStockOperation.Kind.RETURN) {
        final var original = seen.get(operation.returnsOperationId());
        if (original == null
            || original.kind() != LaboratoryStockOperation.Kind.USE
            || !original.batchId().equals(operation.batchId())
            || !Objects.equals(original.experimentId(), operation.experimentId())) {
          throw new IllegalArgumentException("Возврат не соответствует исходному расходу.");
        }
        if (operation.valuation() != null
            && !operation
                .valuation()
                .equals(
                    original.valuation() == null
                        ? new LaboratoryStockOperation.Valuation(
                            batch.pricePerUnit(), batch.unit(), batch.currency())
                        : original.valuation())) {
          throw new IllegalArgumentException("Возврат должен сохранять цену исходного расхода.");
        }
        final BigDecimal returned = returns.merge(original.id(), value, BigDecimal::add);
        if (returned.compareTo(original.quantity().in(batch.unit()).value()) > 0) {
          throw new IllegalArgumentException("Нельзя вернуть больше исходного расхода.");
        }
      }
      final BigDecimal balance =
          balances.merge(
              batch.id(),
              value.multiply(BigDecimal.valueOf(operation.kind().sign())),
              BigDecimal::add);
      if (balance.signum() < 0) {
        throw new IllegalArgumentException(
            "Недостаточно материала в партии «" + batch.label() + "».");
      }
      seen.put(operation.id(), operation);
    }
    LaboratoryReservationBook.replay(batches, operations, reservations);
  }

  public static LaboratoryInventory empty() {
    return new LaboratoryInventory(List.of(), List.of(), List.of());
  }

  public LaboratoryInventory receive(
      final LaboratoryMaterial material,
      final LaboratoryBatch batch,
      final LaboratoryStockOperation receipt) {
    if (receipt.kind() != LaboratoryStockOperation.Kind.RECEIPT
        || !receipt.batchId().equals(batch.id())) {
      throw new IllegalArgumentException("Новая партия требует приходную операцию.");
    }
    if (!Objects.equals(batch.sourceExperimentId(), receipt.experimentId())) {
      throw new IllegalArgumentException(
          "Опыт получения партии должен совпадать с приходной операцией.");
    }
    if (receipt.valuation() != null
        && !receipt
            .valuation()
            .equals(
                new LaboratoryStockOperation.Valuation(
                    batch.pricePerUnit(), batch.unit(), batch.currency()))) {
      throw new IllegalArgumentException("Цена прихода не соответствует новой партии.");
    }
    final var previousReceipt =
        this.operations.stream().filter(value -> value.id().equals(receipt.id())).findFirst();
    if (previousReceipt.isPresent()) {
      if ((previousReceipt.get().equals(receipt)
              || receipt.valuation() == null && previousReceipt.get().valued(null).equals(receipt))
          && this.materials.contains(material)
          && this.batches.contains(batch)) {
        return this;
      }
      throw new IllegalArgumentException("Идентификатор приходной операции уже занят.");
    }
    final var nextMaterials = new ArrayList<>(this.materials);
    final var existing =
        this.materials.stream().filter(value -> value.id().equals(material.id())).findFirst();
    if (existing.isEmpty()) {
      nextMaterials.add(material);
    } else if (!existing.get().equals(material)) {
      throw new IllegalArgumentException("Материал с этим идентификатором отличается.");
    }
    final var nextBatches = new ArrayList<>(this.batches);
    nextBatches.add(batch);
    final var nextOperations = new ArrayList<>(this.operations);
    nextOperations.add(
        receipt.valuation() == null
            ? receipt.valued(
                new LaboratoryStockOperation.Valuation(
                    batch.pricePerUnit(), batch.unit(), batch.currency()))
            : receipt);
    return new LaboratoryInventory(nextMaterials, nextBatches, nextOperations, this.reservations);
  }

  public LaboratoryInventory post(final LaboratoryStockOperation operation) {
    final var existing =
        this.operations.stream().filter(value -> value.id().equals(operation.id())).findFirst();
    if (existing.isPresent()) {
      if (!existing.get().equals(operation)
          && !(operation.valuation() == null && existing.get().valued(null).equals(operation))) {
        throw new IllegalArgumentException("Идентификатор операции уже занят.");
      }
      return this;
    }
    final var next = new ArrayList<>(this.operations);
    final var batch =
        this.batches.stream()
            .filter(value -> value.id().equals(operation.batchId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Партия операции не найдена."));
    if (!operation.quantity().unit().dimension().equals(batch.unit().dimension())) {
      throw new IllegalArgumentException(
          "Единица количества не соответствует партии: ожидается размерность "
              + batch.unit()
              + ".");
    }
    if (this.operations.stream()
        .anyMatch(
            value ->
                value.batchId().equals(batch.id())
                    && value.recordedAt().isAfter(operation.recordedAt()))) {
      throw new IllegalArgumentException(
          "Дата новой операции предшествует последнему движению этой партии. Проверьте системные часы.");
    }
    final var original =
        operation.kind() == LaboratoryStockOperation.Kind.RETURN
            ? this.operations.stream()
                .filter(value -> value.id().equals(operation.returnsOperationId()))
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException("Исходный расход для возврата не найден."))
            : null;
    final var valuation =
        original == null
            ? new LaboratoryStockOperation.Valuation(
                batch.pricePerUnit(), batch.unit(), batch.currency())
            : this.valuation(original);
    if (operation.valuation() != null && !operation.valuation().equals(valuation)) {
      throw new IllegalArgumentException(
          "Цена новой операции не соответствует партии или исходному расходу.");
    }
    next.add(operation.valued(valuation));
    return new LaboratoryInventory(this.materials, this.batches, next, this.reservations);
  }

  public List<LaboratoryReservationBook.State> reservationStates() {
    return LaboratoryReservationBook.replay(this.batches, this.operations, this.reservations);
  }

  public LaboratoryQuantity reserved(final LaboratoryBatch batch) {
    return new LaboratoryQuantity(
        this.reservationStates().stream()
            .filter(s -> s.opened().batchId().equals(batch.id()))
            .map(s -> s.remaining().in(batch.unit()).value())
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        batch.unit());
  }

  public LaboratoryQuantity available(final LaboratoryBatch batch) {
    return new LaboratoryQuantity(
        this.balance(batch).value().subtract(this.reserved(batch).value()), batch.unit());
  }

  /** One atomic change for all allocations and their corresponding physical consumptions. */
  public LaboratoryInventory allocate(
      final List<LaboratoryReservationEvent> events, final List<LaboratoryStockOperation> uses) {
    if (events.isEmpty()) {
      throw new IllegalArgumentException("Нет выбранных изменений резервов.");
    }
    if (events.stream().map(LaboratoryReservationEvent::id).distinct().count() != events.size()) {
      throw new IllegalArgumentException("Повтор события в команде.");
    }
    final var linked =
        events.stream()
            .filter(e -> e.kind() == LaboratoryReservationEvent.Kind.CONSUME)
            .map(LaboratoryReservationEvent::useOperationId)
            .toList();
    if (linked.size() != uses.size()
        || linked.stream().distinct().count() != linked.size()
        || uses.stream().map(LaboratoryStockOperation::id).distinct().count() != uses.size()
        || !new java.util.HashSet<>(linked)
            .equals(
                uses.stream()
                    .map(LaboratoryStockOperation::id)
                    .collect(java.util.stream.Collectors.toSet()))) {
      throw new IllegalArgumentException(
          "Все расходы должны соответствовать событиям использования резервов.");
    }
    final long found =
        events.stream()
            .filter(e -> this.reservations.stream().anyMatch(p -> p.id().equals(e.id())))
            .count();
    if (found > 0) {
      if (found == events.size()
          && this.reservations.containsAll(events)
          && uses.stream()
              .allMatch(
                  u ->
                      this.operations.stream()
                          .anyMatch(
                              p ->
                                  p.equals(u)
                                      || u.valuation() == null && p.valued(null).equals(u)))) {
        return this;
      }
      throw new IllegalArgumentException(
          "Идентификатор команды резерва уже использован с другим содержимым.");
    }
    if (events.stream().anyMatch(e -> e.operationIndex() < this.operations.size())) {
      throw new IllegalArgumentException(
          "Новую операцию нельзя вставить перед уже проведёнными движениями.");
    }
    final var newUses = new ArrayList<>(this.operations);
    for (final var use : uses) {
      if (use.kind() != LaboratoryStockOperation.Kind.USE
          || newUses.stream().anyMatch(p -> p.id().equals(use.id()))) {
        throw new IllegalArgumentException("Нужен новый фактический расход.");
      }
      final var batch =
          this.batches.stream().filter(b -> b.id().equals(use.batchId())).findFirst().orElseThrow();
      if (newUses.stream()
          .anyMatch(
              p -> p.batchId().equals(batch.id()) && p.recordedAt().isAfter(use.recordedAt()))) {
        throw new IllegalArgumentException("Дата расхода предшествует последнему движению партии.");
      }
      final var value =
          new LaboratoryStockOperation.Valuation(
              batch.pricePerUnit(), batch.unit(), batch.currency());
      if (use.valuation() != null && !use.valuation().equals(value)) {
        throw new IllegalArgumentException("Оценка расхода не соответствует партии.");
      }
      newUses.add(use.valued(value));
    }
    final var all = new ArrayList<>(this.reservations);
    all.addAll(events);
    return new LaboratoryInventory(this.materials, this.batches, newUses, all);
  }

  public record Stock(
      LaboratoryQuantity physical, LaboratoryQuantity reserved, LaboratoryQuantity available) {}

  /** One projection for a complete screen, avoiding replay for every table cell. */
  public Map<String, Stock> stock() {
    final Map<String, LaboratoryBatch> batches = new HashMap<>();
    this.batches.forEach(b -> batches.put(b.id(), b));
    final Map<String, BigDecimal> physical = new HashMap<>(), held = new HashMap<>();
    this.operations.forEach(
        o ->
            physical.merge(
                o.batchId(),
                o.quantity()
                    .in(batches.get(o.batchId()).unit())
                    .value()
                    .multiply(BigDecimal.valueOf(o.kind().sign())),
                BigDecimal::add));
    this.reservationStates()
        .forEach(s -> held.merge(s.opened().batchId(), s.remaining().value(), BigDecimal::add));
    final Map<String, Stock> result = new LinkedHashMap<>();
    for (final var b : this.batches) {
      final var p = physical.getOrDefault(b.id(), BigDecimal.ZERO);
      final var h = held.getOrDefault(b.id(), BigDecimal.ZERO);
      result.put(
          b.id(),
          new Stock(
              new LaboratoryQuantity(p, b.unit()),
              new LaboratoryQuantity(h, b.unit()),
              new LaboratoryQuantity(p.subtract(h), b.unit())));
    }
    return java.util.Collections.unmodifiableMap(result);
  }

  public LaboratoryQuantity balance(final LaboratoryBatch batch) {
    final BigDecimal value =
        this.operations.stream()
            .filter(operation -> operation.batchId().equals(batch.id()))
            .map(
                operation ->
                    operation
                        .quantity()
                        .in(batch.unit())
                        .value()
                        .multiply(BigDecimal.valueOf(operation.kind().sign())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new LaboratoryQuantity(value, batch.unit());
  }

  public LaboratoryStockOperation.Valuation valuation(final LaboratoryStockOperation operation) {
    if (operation.valuation() != null) {
      return operation.valuation();
    }
    final var batch =
        this.batches.stream()
            .filter(item -> item.id().equals(operation.batchId()))
            .findFirst()
            .orElseThrow();
    return new LaboratoryStockOperation.Valuation(
        batch.pricePerUnit(), batch.unit(), batch.currency());
  }

  public record Cost(Map<String, BigDecimal> byCurrency, int unpricedUses) {
    public Cost {
      byCurrency = Map.copyOf(byCurrency);
    }
  }

  public Cost cost(final String experimentId) {
    final Map<String, BigDecimal> totals = new LinkedHashMap<>();
    int unknown = 0;
    final Map<String, LaboratoryBatch> batchesById = new HashMap<>();
    this.batches.forEach(batch -> batchesById.put(batch.id(), batch));
    final Map<String, BigDecimal> returnedByUse = new HashMap<>();
    for (final var returned : this.operations) {
      if (returned.kind() == LaboratoryStockOperation.Kind.RETURN) {
        returnedByUse.merge(
            returned.returnsOperationId(),
            returned.quantity().in(batchesById.get(returned.batchId()).unit()).value(),
            BigDecimal::add);
      }
    }
    for (final var operation : this.operations) {
      if (!Objects.equals(experimentId, operation.experimentId())
          || operation.kind() != LaboratoryStockOperation.Kind.USE) {
        continue;
      }
      final var batch = batchesById.get(operation.batchId());
      final BigDecimal net =
          operation
              .quantity()
              .in(batch.unit())
              .value()
              .subtract(returnedByUse.getOrDefault(operation.id(), BigDecimal.ZERO));
      if (net.signum() == 0) {
        continue;
      }
      final var valuation =
          operation.valuation() == null
              ? new LaboratoryStockOperation.Valuation(
                  batch.pricePerUnit(), batch.unit(), batch.currency())
              : operation.valuation();
      if (valuation.pricePerUnit() == null) {
        unknown++;
      } else {
        totals.merge(
            valuation.currency(),
            new LaboratoryQuantity(net, batch.unit())
                .in(valuation.unit())
                .value()
                .multiply(valuation.pricePerUnit()),
            BigDecimal::add);
      }
    }
    return new Cost(totals, unknown);
  }
}
