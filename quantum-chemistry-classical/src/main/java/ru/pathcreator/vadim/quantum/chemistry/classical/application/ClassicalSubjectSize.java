/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application;

import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Сжатый размер химического объекта для preflight и оценки ресурсов.
 */
public final class ClassicalSubjectSize {

  private final long atomCount;
  private final long bondCount;
  private final long electronCount;
  private final long participantCount;
  private final boolean completeGeometry;

  private ClassicalSubjectSize(
      final long atomCount,
      final long bondCount,
      final long electronCount,
      final long participantCount,
      final boolean completeGeometry
  ) {
    this.atomCount = atomCount;
    this.bondCount = bondCount;
    this.electronCount = electronCount;
    this.participantCount = participantCount;
    this.completeGeometry = completeGeometry;
  }

  public static ClassicalSubjectSize of(
      final long atomCount,
      final long bondCount,
      final long electronCount,
      final long participantCount,
      final boolean completeGeometry
  ) {
    ClassicalSubjectSize.requireNonNegative(atomCount, "atom count");
    ClassicalSubjectSize.requireNonNegative(bondCount, "bond count");
    ClassicalSubjectSize.requireNonNegative(electronCount, "electron count");
    ClassicalSubjectSize.requireNonNegative(participantCount, "participant count");
    return new ClassicalSubjectSize(
        atomCount,
        bondCount,
        electronCount,
        participantCount,
        completeGeometry);
  }

  public long atomCount() {
    return this.atomCount;
  }

  public long bondCount() {
    return this.bondCount;
  }

  public long electronCount() {
    return this.electronCount;
  }

  public long participantCount() {
    return this.participantCount;
  }

  public boolean completeGeometry() {
    return this.completeGeometry;
  }

  private static void requireNonNegative(
      final long value,
      final String name
  ) {
    if (value < 0L) {
      throw new IllegalArgumentException("Classical subject " + name + " must be non-negative.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalSubjectSize)) {
      return false;
    }
    final ClassicalSubjectSize size = (ClassicalSubjectSize) other;
    return this.atomCount == size.atomCount
        && this.bondCount == size.bondCount
        && this.electronCount == size.electronCount
        && this.participantCount == size.participantCount
        && this.completeGeometry == size.completeGeometry;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.atomCount);
    result = ChemistryHash.include(result, this.bondCount);
    result = ChemistryHash.include(result, this.electronCount);
    result = ChemistryHash.include(result, this.participantCount);
    result = ChemistryHash.include(result, this.completeGeometry);
    return result;
  }
}