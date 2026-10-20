/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;

/** Counts explicit atoms, including isotope identity and participant coefficients. */
final class ReactionAtomInventory {
  private ReactionAtomInventory() {}

  static Map<String, Long> of(final ReactionSide side) {
    final Map<String, Long> counts = new TreeMap<>();
    side.participants()
        .forEach(
            participant ->
                participant
                    .molecule()
                    .atoms()
                    .forEach(
                        atom -> {
                          final String key =
                              atom.element().symbol().value()
                                  + (atom.isotope() == null
                                      ? ""
                                      : "-" + atom.isotope().massNumber());
                          counts.merge(
                              key, (long) participant.coefficient().value(), Math::addExact);
                        }));
    return Map.copyOf(counts);
  }

  static List<String> excess(final Map<String, Long> supplied, final ReactionSide products) {
    return new TreeMap<>(of(products))
        .entrySet().stream()
            .filter(entry -> entry.getValue() > supplied.getOrDefault(entry.getKey(), 0L))
            .map(
                entry ->
                    entry.getKey()
                        + " (required "
                        + entry.getValue()
                        + ", supplied "
                        + supplied.getOrDefault(entry.getKey(), 0L)
                        + ")")
            .toList();
  }
}
