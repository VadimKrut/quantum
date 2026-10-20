/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * Неизменяемый каталог механистических шаблонов для lookup, объяснений и UI-подсказок.
 */
public final class ReactionMechanismPatternCatalog {

  private final List<ReactionMechanismPattern> patterns;
  private final ReactionMechanismPattern[] patternsByFamilyOrdinal;

  private ReactionMechanismPatternCatalog(
      final List<ReactionMechanismPattern> patterns,
      final ReactionMechanismPattern[] patternsByFamilyOrdinal) {
    this.patterns = patterns;
    this.patternsByFamilyOrdinal = patternsByFamilyOrdinal;
  }

  public static ReactionMechanismPatternCatalog of(final List<ReactionMechanismPattern> patterns) {
    if (patterns == null || patterns.isEmpty()) {
      throw new IllegalArgumentException("Reaction mechanism pattern catalog must not be empty.");
    }
    final ArrayList<ReactionMechanismPattern> checked =
        new ArrayList<ReactionMechanismPattern>(patterns.size());
    final ReactionMechanismPattern[] byFamilyOrdinal =
        new ReactionMechanismPattern[ReactionMechanismFamily.values().length];
    for (int i = 0; i < patterns.size(); ++i) {
      final ReactionMechanismPattern pattern = patterns.get(i);
      if (pattern == null) {
        throw new IllegalArgumentException("Reaction mechanism pattern must not be null.");
      }
      final int ordinal = pattern.family().ordinal();
      if (byFamilyOrdinal[ordinal] != null) {
        throw new IllegalArgumentException("Reaction mechanism pattern family must be unique.");
      }
      byFamilyOrdinal[ordinal] = pattern;
      checked.add(pattern);
    }
    return new ReactionMechanismPatternCatalog(List.copyOf(checked), byFamilyOrdinal.clone());
  }

  public List<ReactionMechanismPattern> patterns() {
    return this.patterns;
  }

  public ReactionMechanismPattern require(final ReactionMechanismFamily family) {
    final ReactionMechanismPattern pattern = this.find(family);
    if (pattern != null) {
      return pattern;
    }
    throw new IllegalArgumentException("Reaction mechanism pattern is not registered.");
  }

  public ReactionMechanismPattern find(final ReactionMechanismFamily family) {
    if (family == null) {
      return null;
    }
    return this.patternsByFamilyOrdinal[family.ordinal()];
  }
}
