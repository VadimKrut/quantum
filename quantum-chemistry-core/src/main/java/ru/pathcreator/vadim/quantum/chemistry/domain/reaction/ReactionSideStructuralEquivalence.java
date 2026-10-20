/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence.Result;

/** Compares complete product sides without altering their participants or provenance. */
public final class ReactionSideStructuralEquivalence {
  private ReactionSideStructuralEquivalence() {}

  public static Result compare(final ReactionSide first, final ReactionSide second) {
    Objects.requireNonNull(first);
    Objects.requireNonNull(second);
    final int size = first.participants().size();
    if (size != second.participants().size()) {
      return Result.DISTINCT;
    }
    if (size > 16 || Thread.currentThread().isInterrupted()) {
      return Result.UNRESOLVED;
    }
    final Result[][] matches = new Result[size][size];
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        final var a = first.participants().get(i);
        final var b = second.participants().get(j);
        matches[i][j] =
            a.coefficient().equals(b.coefficient())
                ? MolecularGraphEquivalence.compare(a.molecule(), b.molecule())
                : Result.DISTINCT;
      }
    }
    if (perfect(matches, false)) {
      return Result.EQUIVALENT;
    }
    return perfect(matches, true) ? Result.UNRESOLVED : Result.DISTINCT;
  }

  private static boolean perfect(final Result[][] matches, final boolean allowUnknown) {
    final int[] assigned = new int[matches.length];
    Arrays.fill(assigned, -1);
    for (int i = 0; i < matches.length; i++) {
      if (!augment(i, matches, allowUnknown, assigned, new boolean[matches.length])) {
        return false;
      }
    }
    return true;
  }

  private static boolean augment(
      final int source,
      final Result[][] matches,
      final boolean allowUnknown,
      final int[] assigned,
      final boolean[] seen) {
    for (int target = 0; target < matches.length; target++) {
      if (seen[target]
          || matches[source][target] == Result.DISTINCT
          || (!allowUnknown && matches[source][target] != Result.EQUIVALENT)) {
        continue;
      }
      seen[target] = true;
      if (assigned[target] < 0
          || augment(assigned[target], matches, allowUnknown, assigned, seen)) {
        assigned[target] = source;
        return true;
      }
    }
    return false;
  }
}
