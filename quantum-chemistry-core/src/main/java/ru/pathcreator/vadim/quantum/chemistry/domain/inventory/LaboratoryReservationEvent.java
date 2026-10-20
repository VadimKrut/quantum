/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.time.Instant;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;

/** Append-only allocation events interleaved at a precise position in the physical stock ledger. */
public record LaboratoryReservationEvent(
    String id,
    String reservationId,
    Instant recordedAt,
    Kind kind,
    int operationIndex,
    String batchId,
    Target target,
    LaboratoryQuantity quantity,
    String useOperationId,
    String reason) {
  public enum Kind {
    RESERVE,
    RELEASE,
    CONSUME
  }

  public record Target(
      String experimentId,
      String stageId,
      String componentId,
      String description,
      String signature) {
    public Target {
      experimentId = IdentifierValue.requireIdentifier(experimentId, "Reservation experiment");
      stageId = IdentifierValue.requireIdentifier(stageId, "Reservation stage");
      componentId = IdentifierValue.requireIdentifier(componentId, "Reservation component");
      description = TextValue.requireText(description, "Название назначения");
      signature = TextValue.requireText(signature, "Подпись плана");
    }
  }

  public LaboratoryReservationEvent {
    id = IdentifierValue.requireIdentifier(id, "Reservation event");
    reservationId = IdentifierValue.requireIdentifier(reservationId, "Reservation id");
    batchId = IdentifierValue.requireIdentifier(batchId, "Reservation batch");
    Objects.requireNonNull(recordedAt);
    Objects.requireNonNull(kind);
    Objects.requireNonNull(target);
    Objects.requireNonNull(quantity);
    if (operationIndex < 0 || quantity.value().signum() <= 0) {
      throw new IllegalArgumentException(
          "Для резерва нужно положительное количество и допустимая позиция в учёте.");
    }
    if ((kind == Kind.CONSUME) != (useOperationId != null)) {
      throw new IllegalArgumentException(
          "Использование резерва должно ссылаться на проведённый расход.");
    }
    if (useOperationId != null) {
      useOperationId = IdentifierValue.requireIdentifier(useOperationId, "Reserved use");
    }
    reason = TextValue.requireText(reason, "Основание изменения резерва");
  }
}
