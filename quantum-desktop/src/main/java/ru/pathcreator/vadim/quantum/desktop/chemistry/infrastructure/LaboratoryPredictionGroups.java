/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence.Result;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;

/** Display-only grouping. Original ranks, weights, graphs and atom provenance remain intact. */
public record LaboratoryPredictionGroups(List<List<Integer>> groups, int unresolvedComparisons) {
  public LaboratoryPredictionGroups {
    groups = groups.stream().map(List::copyOf).toList();
  }

  public static LaboratoryPredictionGroups of(final List<ReactionSide> candidates) {
    final List<List<Integer>> groups = new ArrayList<>();
    int unresolved = 0, comparisons = 0;
    for (int rank = 0; rank < candidates.size(); rank++) {
      List<Integer> found = null;
      for (final var group : groups) {
        if (Thread.currentThread().isInterrupted()) {
          throw new java.util.concurrent.CancellationException();
        }
        final var comparison =
            comparisons++ < 2000
                ? ReactionSideStructuralEquivalence.compare(
                    candidates.get(group.getFirst()), candidates.get(rank))
                : Result.UNRESOLVED;
        if (comparison == Result.EQUIVALENT) {
          found = group;
          break;
        }
        if (comparison == Result.UNRESOLVED) {
          unresolved++;
        }
      }
      if (found == null) {
        found = new ArrayList<>();
        groups.add(found);
      }
      found.add(rank);
    }
    return new LaboratoryPredictionGroups(groups, unresolved);
  }
}
