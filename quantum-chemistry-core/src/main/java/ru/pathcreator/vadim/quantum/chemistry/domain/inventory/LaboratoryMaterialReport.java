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
import java.time.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;

/** Exact reagent movements and ledger balances. No conversion between mass, volume and substance amount. */
public final class LaboratoryMaterialReport {
  public enum Group {
    PROJECT,
    EXPERIMENT,
    MATERIAL,
    BATCH,
    MONTH
  }

  public record Source(String project, LaboratoryInventory inventory) {
    public Source {
      Objects.requireNonNull(project);
      Objects.requireNonNull(inventory);
    }
  }

  public record Filter(
      Instant from,
      Instant until,
      String project,
      String experiment,
      String material,
      String batch) {
    public Filter {
      if (from != null && until != null && !from.isBefore(until)) {
        throw new IllegalArgumentException("Начало периода должно предшествовать его окончанию.");
      }
    }

    public static Filter all() {
      return new Filter(null, null, null, null, null, null);
    }
  }

  public record Entry(
      String project,
      LaboratoryMaterial material,
      LaboratoryBatch batch,
      LaboratoryStockOperation operation,
      LaboratoryQuantityUnit unit,
      BigDecimal quantity) {
    public BigDecimal stockChange() {
      return this.quantity.multiply(BigDecimal.valueOf(this.operation.kind().sign()));
    }
  }

  public record Row(
      String project,
      String groupId,
      String label,
      LaboratoryQuantityUnit unit,
      BigDecimal received,
      BigDecimal used,
      BigDecimal returned,
      BigDecimal increased,
      BigDecimal decreased,
      int operations) {
    public BigDecimal netUsed() {
      return this.used.subtract(this.returned);
    }

    public BigDecimal stockChange() {
      return this.received
          .add(this.returned)
          .add(this.increased)
          .subtract(this.used)
          .subtract(this.decreased);
    }
  }

  public record Balance(
      String project,
      LaboratoryMaterial material,
      LaboratoryBatch batch,
      LaboratoryQuantityUnit unit,
      BigDecimal opening,
      BigDecimal closing) {}

  public record Report(
      List<Entry> entries, List<Row> rows, List<Balance> balances, List<String> warnings) {
    public Report {
      entries = List.copyOf(entries);
      rows = List.copyOf(rows);
      balances = List.copyOf(balances);
      warnings = List.copyOf(warnings);
    }
  }

  private record Key(String project, String id, String label, LaboratoryQuantityUnit unit) {}

  private static final class Total {
    private BigDecimal received = BigDecimal.ZERO,
        used = BigDecimal.ZERO,
        returned = BigDecimal.ZERO,
        increased = BigDecimal.ZERO,
        decreased = BigDecimal.ZERO;
    private int count;
  }

  private LaboratoryMaterialReport() {}

  public static Report summarize(
      final List<Source> sources, final Filter filter, final Group group, final ZoneId zone) {
    Objects.requireNonNull(filter);
    Objects.requireNonNull(group);
    Objects.requireNonNull(zone);
    final Set<String> projects = new HashSet<>();
    final List<Entry> entries = new ArrayList<>();
    final List<Balance> balances = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    final Map<Key, Total> grouped = new LinkedHashMap<>();
    for (final var source : sources) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException("Пересчёт сводки отменён.");
      }
      if (!projects.add(source.project())) {
        throw new IllegalArgumentException("Один проект включён в сводку несколько раз.");
      }
      if (filter.project() != null && !filter.project().equals(source.project())) {
        continue;
      }
      final Map<String, LaboratoryMaterial> materials = new HashMap<>();
      source.inventory().materials().forEach(item -> materials.put(item.id(), item));
      final Map<String, LaboratoryBatch> batches = new HashMap<>();
      source.inventory().batches().forEach(item -> batches.put(item.id(), item));
      final Map<String, BigDecimal> opening = new HashMap<>(), closing = new HashMap<>();
      final Set<String> existed = new HashSet<>(), warned = new HashSet<>();
      final Map<String, BigDecimal> chronological = new HashMap<>();
      final Map<String, LaboratoryStockOperation> byId = new HashMap<>();
      source.inventory().operations().forEach(operation -> byId.put(operation.id(), operation));
      for (final var operation :
          source.inventory().operations().stream()
              .sorted(Comparator.comparing(LaboratoryStockOperation::recordedAt))
              .toList()) {
        if (Thread.currentThread().isInterrupted()) {
          throw new java.util.concurrent.CancellationException("Пересчёт сводки отменён.");
        }
        final var batch = batches.get(operation.batchId());
        final var material = materials.get(batch.materialId());
        if (filter.material() != null && !filter.material().equals(material.id())
            || filter.batch() != null && !filter.batch().equals(batch.id())) {
          continue;
        }
        if (filter.until() != null && !operation.recordedAt().isBefore(filter.until())) {
          continue;
        }
        final var unit = unit(batch.unit());
        final BigDecimal quantity = operation.quantity().in(unit).value();
        final BigDecimal change = quantity.multiply(BigDecimal.valueOf(operation.kind().sign()));
        final BigDecimal historical = chronological.merge(batch.id(), change, BigDecimal::add);
        final boolean earlyReturn =
            operation.kind() == LaboratoryStockOperation.Kind.RETURN
                && operation
                    .recordedAt()
                    .isBefore(byId.get(operation.returnsOperationId()).recordedAt());
        if ((historical.signum() < 0 || earlyReturn) && warned.add(batch.id())) {
          warnings.add(
              source.project()
                  + " · партия «"
                  + batch.label()
                  + "» ["
                  + batch.id()
                  + "]: нарушен порядок дат движений; исторические остатки требуют проверки. Операция "
                  + operation.id());
        }
        existed.add(batch.id());
        closing.merge(batch.id(), change, BigDecimal::add);
        if (filter.from() != null && operation.recordedAt().isBefore(filter.from())) {
          opening.merge(batch.id(), change, BigDecimal::add);
          continue;
        }
        if (filter.experiment() != null && !filter.experiment().equals(operation.experimentId())) {
          continue;
        }
        final var entry = new Entry(source.project(), material, batch, operation, unit, quantity);
        entries.add(entry);
        final String id = groupId(entry, group, zone);
        final String label =
            switch (group) {
              case PROJECT, MONTH -> id;
              case EXPERIMENT -> operation.experimentId() == null ? "Без привязки к опыту" : id;
              case MATERIAL -> material.name();
              case BATCH -> material.name() + " · " + batch.label();
            };
        final Total total =
            grouped.computeIfAbsent(
                new Key(source.project(), id, label, unit), ignored -> new Total());
        switch (operation.kind()) {
          case RECEIPT -> total.received = total.received.add(quantity);
          case USE -> total.used = total.used.add(quantity);
          case RETURN -> total.returned = total.returned.add(quantity);
          case INCREASE -> total.increased = total.increased.add(quantity);
          case DECREASE -> total.decreased = total.decreased.add(quantity);
        }
        total.count++;
      }
      for (final var batch : source.inventory().batches()) {
        if (Thread.currentThread().isInterrupted()) {
          throw new java.util.concurrent.CancellationException("Пересчёт сводки отменён.");
        }
        if (existed.contains(batch.id())) {
          balances.add(
              new Balance(
                  source.project(),
                  materials.get(batch.materialId()),
                  batch,
                  unit(batch.unit()),
                  opening.getOrDefault(batch.id(), BigDecimal.ZERO),
                  closing.getOrDefault(batch.id(), BigDecimal.ZERO)));
        }
      }
    }
    entries.sort(
        Comparator.comparing((Entry entry) -> entry.operation().recordedAt())
            .thenComparing(Entry::project)
            .thenComparing(entry -> entry.operation().id()));
    final List<Row> rows = new ArrayList<>();
    grouped.forEach(
        (key, total) ->
            rows.add(
                new Row(
                    key.project(),
                    key.id(),
                    key.label(),
                    key.unit(),
                    total.received,
                    total.used,
                    total.returned,
                    total.increased,
                    total.decreased,
                    total.count)));
    rows.sort(
        Comparator.comparing(Row::project)
            .thenComparing(Row::label)
            .thenComparing(row -> row.unit().name()));
    balances.sort(
        Comparator.comparing(Balance::project)
            .thenComparing(value -> value.material().name())
            .thenComparing(value -> value.batch().label()));
    return new Report(entries, rows, balances, warnings);
  }

  public static List<Entry> entriesFor(
      final Report report, final Row row, final Group group, final ZoneId zone) {
    if (!report.rows().contains(row)) {
      throw new IllegalArgumentException("Строка не относится к текущей сводке.");
    }
    return report.entries().stream()
        .filter(
            entry ->
                entry.project().equals(row.project())
                    && entry.unit() == row.unit()
                    && groupId(entry, group, zone).equals(row.groupId()))
        .toList();
  }

  private static String groupId(final Entry entry, final Group group, final ZoneId zone) {
    return switch (group) {
      case PROJECT -> entry.project();
      case EXPERIMENT -> Objects.toString(entry.operation().experimentId(), "");
      case MATERIAL -> entry.material().id();
      case BATCH -> entry.batch().id();
      case MONTH -> YearMonth.from(entry.operation().recordedAt().atZone(zone)).toString();
    };
  }

  public static LaboratoryQuantityUnit unit(final LaboratoryQuantityUnit unit) {
    return switch (unit.dimension()) {
      case "mass" -> LaboratoryQuantityUnit.GRAM;
      case "volume" -> LaboratoryQuantityUnit.MILLILITRE;
      case "amount" -> LaboratoryQuantityUnit.MILLIMOLE;
      default -> throw new IllegalArgumentException("Неизвестная размерность количества.");
    };
  }
}
