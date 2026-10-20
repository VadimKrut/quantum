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
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;

/** Replays allocations and physical movements together; reserved stock cannot be spent elsewhere. */
public final class LaboratoryReservationBook {
  private LaboratoryReservationBook() {}

  public record State(
      LaboratoryReservationEvent opened,
      LaboratoryQuantity remaining,
      LaboratoryQuantity consumed,
      LaboratoryQuantity released) {
    public boolean active() {
      return this.remaining.value().signum() > 0;
    }
  }

  public static List<State> replay(
      final List<LaboratoryBatch> batches,
      final List<LaboratoryStockOperation> operations,
      final List<LaboratoryReservationEvent> events) {
    final Map<String, LaboratoryBatch> byBatch = new HashMap<>();
    batches.forEach(b -> byBatch.put(b.id(), b));
    final Map<String, BigDecimal> stock = new HashMap<>(), held = new HashMap<>();
    final Map<String, State> states = new LinkedHashMap<>();
    final Set<String> ids = new HashSet<>(), consumed = new HashSet<>();
    int previousIndex = -1;
    final Map<String, java.time.Instant> lastReservationTime = new HashMap<>(),
        lastStockTime = new HashMap<>();
    if (events.size() > 100000) {
      throw new IllegalArgumentException("Слишком много событий резервирования.");
    }
    for (final var e : events) {
      if (!ids.add(e.id())
          || e.operationIndex() < previousIndex
          || e.operationIndex() > operations.size()) {
        throw new IllegalArgumentException("Нарушена последовательность событий резерва.");
      }
      previousIndex = e.operationIndex();
    }
    int cursor = 0;
    for (int index = 0; index <= operations.size(); index++) {
      while (cursor < events.size() && events.get(cursor).operationIndex() == index) {
        final var e = events.get(cursor++);
        final var batch = byBatch.get(e.batchId());
        if (batch == null) {
          throw new IllegalArgumentException("Партия резерва не найдена.");
        }
        final BigDecimal quantity = e.quantity().in(batch.unit()).value();
        final var previous = states.get(e.reservationId());
        if (lastReservationTime.containsKey(e.reservationId())
                && e.recordedAt().isBefore(lastReservationTime.get(e.reservationId()))
            || lastStockTime.containsKey(batch.id())
                && e.recordedAt().isBefore(lastStockTime.get(batch.id()))) {
          throw new IllegalArgumentException(
              "Дата события резерва предшествует последней операции резерва или партии.");
        }
        lastReservationTime.put(e.reservationId(), e.recordedAt());
        if (e.kind() == LaboratoryReservationEvent.Kind.RESERVE) {
          if (previous != null) {
            throw new IllegalArgumentException("Идентификатор резерва уже существует.");
          }
          final var zero = new LaboratoryQuantity(BigDecimal.ZERO, batch.unit());
          states.put(
              e.reservationId(),
              new State(e, new LaboratoryQuantity(quantity, batch.unit()), zero, zero));
          held.merge(batch.id(), quantity, BigDecimal::add);
        } else {
          if (previous == null
              || !previous.opened().batchId().equals(batch.id())
              || !previous.opened().target().equals(e.target())
              || e.recordedAt().isBefore(previous.opened().recordedAt())) {
            throw new IllegalArgumentException("Событие не соответствует исходному резерву.");
          }
          if (quantity.compareTo(previous.remaining().value()) > 0) {
            throw new IllegalArgumentException("Количество больше оставшегося резерва.");
          }
          if (e.kind() == LaboratoryReservationEvent.Kind.CONSUME) {
            if (index == operations.size()) {
              throw new IllegalArgumentException("Нет расхода для использования резерва.");
            }
            final var use = operations.get(index);
            if (!consumed.add(use.id())
                || !use.id().equals(e.useOperationId())
                || use.kind() != LaboratoryStockOperation.Kind.USE
                || !use.batchId().equals(batch.id())
                || !Objects.equals(use.experimentId(), e.target().experimentId())
                || !use.recordedAt().equals(e.recordedAt())
                || use.quantity().in(batch.unit()).value().compareTo(quantity) != 0) {
              throw new IllegalArgumentException(
                  "Резерв и фактический расход должны соответствовать одной операции.");
            }
          }
          states.put(
              e.reservationId(),
              new State(
                  previous.opened(),
                  new LaboratoryQuantity(
                      previous.remaining().value().subtract(quantity), batch.unit()),
                  new LaboratoryQuantity(
                      previous
                          .consumed()
                          .value()
                          .add(
                              e.kind() == LaboratoryReservationEvent.Kind.CONSUME
                                  ? quantity
                                  : BigDecimal.ZERO),
                      batch.unit()),
                  new LaboratoryQuantity(
                      previous
                          .released()
                          .value()
                          .add(
                              e.kind() == LaboratoryReservationEvent.Kind.RELEASE
                                  ? quantity
                                  : BigDecimal.ZERO),
                      batch.unit())));
          held.merge(batch.id(), quantity.negate(), BigDecimal::add);
        }
        requireAvailable(batch, stock, held);
      }
      if (index < operations.size()) {
        final var operation = operations.get(index);
        final var batch = byBatch.get(operation.batchId());
        if (batch == null) {
          throw new IllegalArgumentException("Партия движения не найдена.");
        }
        lastStockTime.merge(batch.id(), operation.recordedAt(), (a, b) -> a.isAfter(b) ? a : b);
        stock.merge(
            batch.id(),
            operation
                .quantity()
                .in(batch.unit())
                .value()
                .multiply(BigDecimal.valueOf(operation.kind().sign())),
            BigDecimal::add);
        requireAvailable(batch, stock, held);
      }
    }
    return List.copyOf(states.values());
  }

  private static void requireAvailable(
      final LaboratoryBatch batch,
      final Map<String, BigDecimal> stock,
      final Map<String, BigDecimal> held) {
    if (stock
            .getOrDefault(batch.id(), BigDecimal.ZERO)
            .subtract(held.getOrDefault(batch.id(), BigDecimal.ZERO))
            .signum()
        < 0) {
      throw new IllegalArgumentException(
          "Недостаточно свободного количества в партии «"
              + batch.label()
              + "»: часть материала зарезервирована. Сначала снимите соответствующий резерв.");
    }
  }
}
