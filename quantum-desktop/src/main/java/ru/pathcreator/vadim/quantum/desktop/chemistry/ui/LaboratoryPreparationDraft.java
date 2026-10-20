/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryStockPreparation.Command;

/** Exact command identities survive a crash between durable stock write and the UI callback. */
final class LaboratoryPreparationDraft {
  private LaboratoryPreparationDraft() {}

  static void write(final Properties p, final Command command) {
    if (command == null) {
      return;
    }
    p.setProperty("preparation.events", "" + command.events().size());
    int i = 0;
    for (final var e : command.events()) {
      final String k = "preparation.event." + i++ + ".";
      p.setProperty(k + "id", e.id());
      p.setProperty(k + "reservation", e.reservationId());
      p.setProperty(k + "time", e.recordedAt().toString());
      p.setProperty(k + "kind", e.kind().name());
      p.setProperty(k + "index", "" + e.operationIndex());
      p.setProperty(k + "batch", e.batchId());
      p.setProperty(k + "experiment", e.target().experimentId());
      p.setProperty(k + "stage", e.target().stageId());
      p.setProperty(k + "component", e.target().componentId());
      p.setProperty(k + "description", e.target().description());
      p.setProperty(k + "signature", e.target().signature());
      p.setProperty(k + "value", e.quantity().value().toPlainString());
      p.setProperty(k + "unit", e.quantity().unit().name());
      p.setProperty(k + "reason", e.reason());
      if (e.useOperationId() != null) {
        p.setProperty(k + "use", e.useOperationId());
      }
    }
  }

  static Command read(final Properties p) {
    if (!p.containsKey("preparation.events")) {
      return null;
    }
    final int count = Integer.parseInt(p.getProperty("preparation.events"));
    if (count < 1 || count > 200) {
      throw new IllegalArgumentException("Повреждён состав восстанавливаемой операции.");
    }
    final List<LaboratoryReservationEvent> events = new ArrayList<>();
    final List<LaboratoryStockOperation> uses = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final String k = "preparation.event." + i + ".";
      final var target =
          new LaboratoryReservationEvent.Target(
              p.getProperty(k + "experiment"),
              p.getProperty(k + "stage"),
              p.getProperty(k + "component"),
              p.getProperty(k + "description"),
              p.getProperty(k + "signature"));
      final var e =
          new LaboratoryReservationEvent(
              p.getProperty(k + "id"),
              p.getProperty(k + "reservation"),
              Instant.parse(p.getProperty(k + "time")),
              LaboratoryReservationEvent.Kind.valueOf(p.getProperty(k + "kind")),
              Integer.parseInt(p.getProperty(k + "index")),
              p.getProperty(k + "batch"),
              target,
              new LaboratoryQuantity(
                  new BigDecimal(p.getProperty(k + "value")),
                  LaboratoryQuantityUnit.valueOf(p.getProperty(k + "unit"))),
              p.getProperty(k + "use"),
              p.getProperty(k + "reason"));
      events.add(e);
      if (e.kind() == LaboratoryReservationEvent.Kind.CONSUME) {
        uses.add(
            new LaboratoryStockOperation(
                e.useOperationId(),
                e.batchId(),
                e.recordedAt(),
                LaboratoryStockOperation.Kind.USE,
                e.quantity(),
                target.experimentId(),
                null,
                e.reason()));
      }
    }
    return new Command(events, uses);
  }
}
