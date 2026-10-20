/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** A stage preparation references the notebook core; allocating stock never invents an observation. */
public final class LaboratoryStockPreparation {
  private LaboratoryStockPreparation() {}

  public record Need(
      LaboratoryComponent component,
      LaboratoryStageParticipant participant,
      LaboratoryReservationEvent.Target target) {}

  public record Allocation(
      Need need, String batchId, LaboratoryQuantity quantity, boolean reviewed) {}

  public record Change(String reservationId, LaboratoryQuantity quantity, boolean releaseRest) {}

  public record Command(
      List<LaboratoryReservationEvent> events, List<LaboratoryStockOperation> uses) {
    public Command {
      events = List.copyOf(events);
      uses = List.copyOf(uses);
    }

    public LaboratoryInventory apply(final LaboratoryInventory inventory) {
      return inventory.allocate(this.events, this.uses);
    }

    public boolean committed(final LaboratoryInventory inventory) {
      return !this.events.isEmpty()
          && inventory.reservations().containsAll(this.events)
          && this.uses.stream()
              .allMatch(
                  u ->
                      inventory.operations().stream()
                          .anyMatch(p -> p.valued(null).equals(u.valued(null))));
    }
  }

  public static List<Need> needs(final LaboratoryExperiment experiment, final String stageId) {
    final var stage =
        experiment.stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Стадия не найдена."));
    final List<Need> result = new ArrayList<>();
    for (final var p : stage.participants()) {
      if (p.role() == ReactionComponentRole.PRODUCT) {
        continue;
      }
      final var c =
          experiment.components().stream()
              .filter(v -> v.id().equals(p.componentId()))
              .findFirst()
              .orElseThrow();
      final String data =
          experiment.id()
              + "\n"
              + stage.id()
              + "\n"
              + c.id()
              + "\n"
              + p.role()
              + "\n"
              + p.coefficient()
              + "\n"
              + LaboratoryMaterialProperties.signature(c.molecule())
              + "\n"
              + c.planned()
              + "\n"
              + c.properties();
      result.add(
          new Need(
              c,
              p,
              new LaboratoryReservationEvent.Target(
                  experiment.id(),
                  stage.id(),
                  c.id(),
                  experiment.title() + " / " + stage.title() + " / " + c.molecule().displayName(),
                  hash(data))));
    }
    return List.copyOf(result);
  }

  private static String hash(final String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean current(
      final LaboratoryExperiment experiment, final LaboratoryReservationEvent.Target target) {
    if (!experiment.id().equals(target.experimentId())
        || experiment.stages().stream().noneMatch(s -> s.id().equals(target.stageId()))) {
      return false;
    }
    return needs(experiment, target.stageId()).stream()
        .anyMatch(
            n ->
                n.component().id().equals(target.componentId())
                    && n.target().signature().equals(target.signature()));
  }

  public static boolean matches(
      final LaboratoryInventory inventory, final LaboratoryBatch batch, final Need need) {
    final var material =
        inventory.materials().stream()
            .filter(m -> m.id().equals(batch.materialId()))
            .findFirst()
            .orElseThrow();
    return material.molecule() != null
        && MolecularGraphEquivalence.compare(material.molecule(), need.component().molecule())
            == MolecularGraphEquivalence.Result.EQUIVALENT;
  }

  /** Only a unit conversion; purity, concentration and density must be checked against the chosen lot. */
  public static LaboratoryQuantity suggested(final Need need, final LaboratoryBatch batch) {
    final var q = need.component().planned();
    return q != null && q.unit().dimension().equals(batch.unit().dimension())
        ? q.in(batch.unit())
        : null;
  }

  public static Command reserve(
      final LaboratoryInventory inventory,
      final List<Allocation> rows,
      final String token,
      final Instant time,
      final String reason) {
    requireCount(rows.size());
    final List<LaboratoryReservationEvent> events = new ArrayList<>();
    int i = 0;
    for (final var row : rows) {
      if (!row.reviewed()) {
        throw new IllegalArgumentException(
            "Проверьте состав, концентрацию и количество каждой выбранной партии.");
      }
      final String id = token + "_" + i++;
      events.add(
          new LaboratoryReservationEvent(
              id,
              id,
              time,
              LaboratoryReservationEvent.Kind.RESERVE,
              inventory.operations().size(),
              row.batchId(),
              row.need().target(),
              row.quantity(),
              null,
              reason));
    }
    final var command = new Command(events, List.of());
    command.apply(inventory);
    return command;
  }

  public static Command change(
      final LaboratoryInventory inventory,
      final List<Change> rows,
      final boolean consume,
      final String token,
      final Instant time,
      final String reason) {
    requireCount(rows.size());
    final List<LaboratoryReservationEvent> events = new ArrayList<>();
    final List<LaboratoryStockOperation> uses = new ArrayList<>();
    final Set<String> seen = new HashSet<>();
    int i = 0;
    final var states = inventory.reservationStates();
    for (final var row : rows) {
      if (!seen.add(row.reservationId())) {
        throw new IllegalArgumentException("Резерв выбран дважды.");
      }
      final var state =
          states.stream()
              .filter(s -> s.opened().reservationId().equals(row.reservationId()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Резерв не найден."));
      final var opened = state.opened();
      final var q = row.quantity().in(state.remaining().unit());
      final String id = token + "_" + i++;
      final String use = consume ? id + "_use" : null;
      events.add(
          new LaboratoryReservationEvent(
              id,
              opened.reservationId(),
              time,
              consume
                  ? LaboratoryReservationEvent.Kind.CONSUME
                  : LaboratoryReservationEvent.Kind.RELEASE,
              inventory.operations().size() + uses.size(),
              opened.batchId(),
              opened.target(),
              q,
              use,
              reason));
      if (consume) {
        uses.add(
            new LaboratoryStockOperation(
                use,
                opened.batchId(),
                time,
                LaboratoryStockOperation.Kind.USE,
                q,
                opened.target().experimentId(),
                null,
                reason));
        final BigDecimal rest = state.remaining().value().subtract(q.value());
        if (row.releaseRest() && rest.signum() > 0) {
          events.add(
              new LaboratoryReservationEvent(
                  id + "_rest",
                  opened.reservationId(),
                  time,
                  LaboratoryReservationEvent.Kind.RELEASE,
                  inventory.operations().size() + uses.size(),
                  opened.batchId(),
                  opened.target(),
                  new LaboratoryQuantity(rest, q.unit()),
                  null,
                  reason));
        }
      }
    }
    final var command = new Command(events, uses);
    command.apply(inventory);
    return command;
  }

  private static void requireCount(final int count) {
    if (count < 1 || count > 100) {
      throw new IllegalArgumentException("Выберите от 1 до 100 строк для одной операции.");
    }
  }

  /** Net, recorded use for this stage, without turning reservations or unrelated withdrawals into loading. */
  public static Map<String, LaboratoryQuantity> actuals(
      final LaboratoryInventory inventory,
      final LaboratoryExperiment experiment,
      final String stageId) {
    final Map<String, LaboratoryQuantity> result = new LinkedHashMap<>();
    for (final var e : inventory.reservations()) {
      if (e.kind() != LaboratoryReservationEvent.Kind.CONSUME
          || !e.target().experimentId().equals(experiment.id())
          || !e.target().stageId().equals(stageId)) {
        continue;
      }
      if (!current(experiment, e.target())) {
        throw new IllegalArgumentException(
            "План или структура после расхода изменились. Сопоставьте фактическую загрузку вручную.");
      }
      if (experiment.stages().stream()
              .filter(
                  s ->
                      s.participants().stream()
                          .anyMatch(p -> p.componentId().equals(e.target().componentId())))
              .count()
          > 1) {
        throw new IllegalArgumentException(
            "Компонент общий для нескольких стадий. Укажите его общую фактическую загрузку вручную.");
      }
      final BigDecimal returned =
          inventory.operations().stream()
              .filter(
                  o ->
                      o.kind() == LaboratoryStockOperation.Kind.RETURN
                          && e.useOperationId().equals(o.returnsOperationId()))
              .map(o -> o.quantity().in(e.quantity().unit()).value())
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      final var q =
          new LaboratoryQuantity(e.quantity().value().subtract(returned), e.quantity().unit());
      final var previous = result.get(e.target().componentId());
      try {
        result.put(
            e.target().componentId(),
            previous == null
                ? q
                : new LaboratoryQuantity(
                    previous.value().add(q.in(previous.unit()).value()), previous.unit()));
      } catch (final IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "Расход одного компонента записан в разных размерностях. Перенесите фактическую загрузку вручную.",
            ex);
      }
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Для этой стадии ещё не проведён расход из резервов.");
    }
    return Collections.unmodifiableMap(result);
  }
}
