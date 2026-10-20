/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.history;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;

/**
 * Упорядоченная история одной реакции для просмотра, отката и сравнения пользовательских записей.
 */
public final class ChemistryStorageReactionTimeline {

  private static final Comparator<ChemistryStorageReactionRecord> HISTORY_ORDER =
      Comparator.comparingInt(ChemistryStorageReactionRecord::historyIndex)
          .thenComparing(ChemistryStorageReactionRecord::id);
  private static final Comparator<ChemistryStorageReactionHistoryEvent> EVENT_HISTORY_ORDER =
      Comparator.comparingInt(ChemistryStorageReactionHistoryEvent::historyIndex)
          .thenComparing(ChemistryStorageReactionHistoryEvent::id);

  private final String reactionId;
  private final List<ChemistryStorageReactionRecord> records;
  private final List<ChemistryStorageReactionHistoryEvent> events;

  private ChemistryStorageReactionTimeline(
      final String reactionId,
      final List<ChemistryStorageReactionRecord> records,
      final List<ChemistryStorageReactionHistoryEvent> events) {
    this.reactionId = reactionId;
    this.records = records;
    this.events = events;
  }

  public static ChemistryStorageReactionTimeline of(
      final String reactionId, final List<ChemistryStorageReactionRecord> records) {
    return ChemistryStorageReactionTimeline.of(reactionId, records, List.of());
  }

  public static ChemistryStorageReactionTimeline of(
      final String reactionId,
      final List<ChemistryStorageReactionRecord> records,
      final List<ChemistryStorageReactionHistoryEvent> events) {
    final String checkedReactionId =
        IdentifierValue.requireIdentifier(reactionId, "Storage reaction timeline reaction id");
    final ArrayList<ChemistryStorageReactionRecord> checkedRecords =
        new ArrayList<ChemistryStorageReactionRecord>(records == null ? 0 : records.size());
    if (records != null) {
      for (int i = 0; i < records.size(); ++i) {
        final ChemistryStorageReactionRecord record = records.get(i);
        if (record == null) {
          throw new IllegalArgumentException("Storage reaction timeline record must not be null.");
        }
        if (!checkedReactionId.equals(record.reaction().id().value())) {
          throw new IllegalArgumentException(
              "Storage reaction timeline record belongs to another reaction.");
        }
        checkedRecords.add(record);
      }
    }
    checkedRecords.sort(HISTORY_ORDER);
    final ArrayList<ChemistryStorageReactionHistoryEvent> checkedEvents =
        new ArrayList<ChemistryStorageReactionHistoryEvent>(events == null ? 0 : events.size());
    if (events != null) {
      for (int i = 0; i < events.size(); ++i) {
        final ChemistryStorageReactionHistoryEvent event = events.get(i);
        if (event == null) {
          throw new IllegalArgumentException("Storage reaction timeline event must not be null.");
        }
        if (!checkedReactionId.equals(event.reactionId())) {
          throw new IllegalArgumentException(
              "Storage reaction timeline event belongs to another reaction.");
        }
        checkedEvents.add(event);
      }
    }
    checkedEvents.sort(EVENT_HISTORY_ORDER);
    return new ChemistryStorageReactionTimeline(
        checkedReactionId, List.copyOf(checkedRecords), List.copyOf(checkedEvents));
  }

  public String reactionId() {
    return this.reactionId;
  }

  public List<ChemistryStorageReactionRecord> records() {
    return this.records;
  }

  public List<ChemistryStorageReactionHistoryEvent> events() {
    return this.events;
  }

  public boolean empty() {
    return this.records.isEmpty() && this.events.isEmpty();
  }

  public int size() {
    return this.records.size();
  }

  public int eventCount() {
    return this.events.size();
  }

  public ChemistryStorageReactionRecord first() {
    if (this.records.isEmpty()) {
      throw new IllegalStateException("Storage reaction timeline is empty.");
    }
    return this.records.get(0);
  }

  public ChemistryStorageReactionRecord latest() {
    if (this.records.isEmpty()) {
      throw new IllegalStateException("Storage reaction timeline is empty.");
    }
    return this.records.get(this.records.size() - 1);
  }

  public ChemistryStorageReactionRecord requireHistoryIndex(final int historyIndex) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException(
          "Storage reaction timeline history index must be non-negative.");
    }
    for (int i = 0; i < this.records.size(); ++i) {
      final ChemistryStorageReactionRecord record = this.records.get(i);
      if (record.historyIndex() == historyIndex) {
        return record;
      }
    }
    throw new IllegalArgumentException("Storage reaction timeline history index does not exist.");
  }

  public ChemistryStorageReactionHistoryEvent requireEvent(final String eventId) {
    final String checkedEventId =
        IdentifierValue.requireIdentifier(eventId, "Storage reaction timeline event id");
    for (int i = 0; i < this.events.size(); ++i) {
      final ChemistryStorageReactionHistoryEvent event = this.events.get(i);
      if (event.id().equals(checkedEventId)) {
        return event;
      }
    }
    throw new IllegalArgumentException("Storage reaction timeline event does not exist.");
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageReactionTimeline)) {
      return false;
    }
    final ChemistryStorageReactionTimeline timeline = (ChemistryStorageReactionTimeline) other;
    return Objects.equals(this.reactionId, timeline.reactionId)
        && Objects.equals(this.records, timeline.records)
        && Objects.equals(this.events, timeline.events);
  }

  public int hashCode() {
    return Objects.hash(this.reactionId, this.records, this.events);
  }
}
