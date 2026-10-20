/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability;

import java.util.List;
import java.util.Objects;

/**
 * Полная карта готовности Java-native chemistry stack для конкретной расчетной постановки.
 */
public final class ChemistryCapabilityReport {

  private final String subject;
  private final List<ChemistryCapabilityItem> items;

  private ChemistryCapabilityReport(
      final String subject, final List<ChemistryCapabilityItem> items) {
    this.subject = subject;
    this.items = items;
  }

  public static ChemistryCapabilityReport of(
      final String subject, final List<ChemistryCapabilityItem> items) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Chemistry capability report subject must not be blank.");
    }
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("Chemistry capability report items must not be empty.");
    }
    return new ChemistryCapabilityReport(subject, List.copyOf(items));
  }

  public String subject() {
    return this.subject;
  }

  public List<ChemistryCapabilityItem> items() {
    return this.items;
  }

  public int supportedCount() {
    return this.count(ChemistryCapabilityStatus.SUPPORTED);
  }

  public int partialCount() {
    return this.count(ChemistryCapabilityStatus.PARTIAL);
  }

  public int unsupportedCount() {
    return this.count(ChemistryCapabilityStatus.UNSUPPORTED);
  }

  public boolean fullySupported() {
    return this.unsupportedCount() == 0 && this.partialCount() == 0;
  }

  public boolean hasBlockingUnsupportedItems() {
    return this.unsupportedCount() > 0;
  }

  public ChemistryCapabilityItem requireItem(final String key) {
    for (int i = 0; i < this.items.size(); ++i) {
      final ChemistryCapabilityItem item = this.items.get(i);
      if (item.key().equals(key)) {
        return item;
      }
    }
    throw new IllegalArgumentException("Chemistry capability report does not contain item: " + key);
  }

  private int count(final ChemistryCapabilityStatus status) {
    int count = 0;
    for (int i = 0; i < this.items.size(); ++i) {
      if (this.items.get(i).status() == status) {
        ++count;
      }
    }
    return count;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryCapabilityReport)) {
      return false;
    }
    final ChemistryCapabilityReport report = (ChemistryCapabilityReport) other;
    return Objects.equals(this.subject, report.subject) && Objects.equals(this.items, report.items);
  }

  public int hashCode() {
    return Objects.hash(this.subject, this.items);
  }
}
