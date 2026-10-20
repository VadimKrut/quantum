/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;

/** Результат удаления qubit по фиксированным Z2-симметриям. */
public final class QubitTaperingResult {

  private final QubitPauliOperator operator;
  private final List<FixedZ2Symmetry> fixedSymmetries;
  private final int originalQubitCount;
  private final int taperedQubitCount;

  private QubitTaperingResult(
      final QubitPauliOperator operator,
      final List<FixedZ2Symmetry> fixedSymmetries,
      final int originalQubitCount,
      final int taperedQubitCount) {
    this.operator = operator;
    this.fixedSymmetries = fixedSymmetries;
    this.originalQubitCount = originalQubitCount;
    this.taperedQubitCount = taperedQubitCount;
  }

  public static QubitTaperingResult of(
      final QubitPauliOperator operator,
      final List<FixedZ2Symmetry> fixedSymmetries,
      final int originalQubitCount,
      final int taperedQubitCount) {
    if (operator == null) {
      throw new IllegalArgumentException("Tapered qubit operator must not be null.");
    }
    if (fixedSymmetries == null) {
      throw new IllegalArgumentException("Fixed Z2 symmetry list must not be null.");
    }
    if (originalQubitCount < 0 || taperedQubitCount < 0) {
      throw new IllegalArgumentException("Qubit counts must not be negative.");
    }
    if (taperedQubitCount > originalQubitCount) {
      throw new IllegalArgumentException("Tapered qubit count must not exceed original count.");
    }
    return new QubitTaperingResult(
        operator, List.copyOf(fixedSymmetries), originalQubitCount, taperedQubitCount);
  }

  public QubitPauliOperator operator() {
    return this.operator;
  }

  public List<FixedZ2Symmetry> fixedSymmetries() {
    return this.fixedSymmetries;
  }

  public int originalQubitCount() {
    return this.originalQubitCount;
  }

  public int taperedQubitCount() {
    return this.taperedQubitCount;
  }

  public int removedQubitCount() {
    return this.originalQubitCount - this.taperedQubitCount;
  }
}
