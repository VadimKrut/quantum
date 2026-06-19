/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Arrays;
import java.util.List;

/**
 * Каноническая подпись стороны реакции: molecule id + стехиометрический коэффициент без зависимости
 * от порядка участников.
 */
public final class ReactionSideSignature {

  private final String[] moleculeIds;
  private final int[] coefficients;
  private final int hashCode;

  private ReactionSideSignature(
      final String[] moleculeIds,
      final int[] coefficients
  ) {
    this.moleculeIds = moleculeIds;
    this.coefficients = coefficients;
    this.hashCode = 31 * Arrays.hashCode(moleculeIds) + Arrays.hashCode(coefficients);
  }

  public static ReactionSideSignature of(final ReactionSide side) {
    if (side == null) {
      throw new IllegalArgumentException("Reaction side must not be null.");
    }
    return ReactionSideSignature.fromParticipants(side.participants());
  }

  static ReactionSideSignature fromParticipants(final List<ReactionParticipant> participants) {
    if (participants == null || participants.isEmpty()) {
      throw new IllegalArgumentException("Reaction side participants must not be empty.");
    }
    final String[] moleculeIds = new String[participants.size()];
    final int[] coefficients = new int[participants.size()];
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      if (participant == null) {
        throw new IllegalArgumentException("Reaction participant must not be null.");
      }
      moleculeIds[i] = participant.molecule().id().value();
      coefficients[i] = participant.coefficient().value();
    }
    ReactionSideSignature.sortByMoleculeId(moleculeIds, coefficients);
    ReactionSideSignature.requireUniqueMoleculeIds(moleculeIds);
    return new ReactionSideSignature(moleculeIds, coefficients);
  }

  public int participantCount() {
    return moleculeIds.length;
  }

  public boolean sameParticipantsAs(final ReactionSideSignature other) {
    if (other == null || moleculeIds.length != other.moleculeIds.length) {
      return false;
    }
    return Arrays.equals(moleculeIds, other.moleculeIds)
        && Arrays.equals(coefficients, other.coefficients);
  }

  private static void sortByMoleculeId(
      final String[] moleculeIds,
      final int[] coefficients
  ) {
    ReactionSideSignature.quickSort(moleculeIds, coefficients, 0, moleculeIds.length - 1);
  }

  private static void quickSort(
      final String[] moleculeIds, final int[] coefficients, final int left, final int right) {
    int low = left;
    int high = right;
    final String pivot = moleculeIds[left + (right - left) / 2];
    while (low <= high) {
      while (moleculeIds[low].compareTo(pivot) < 0) {
        ++low;
      }
      while (moleculeIds[high].compareTo(pivot) > 0) {
        --high;
      }
      if (low <= high) {
        ReactionSideSignature.swap(moleculeIds, coefficients, low, high);
        ++low;
        --high;
      }
    }
    if (left < high) {
      ReactionSideSignature.quickSort(moleculeIds, coefficients, left, high);
    }
    if (low < right) {
      ReactionSideSignature.quickSort(moleculeIds, coefficients, low, right);
    }
  }

  private static void swap(
      final String[] moleculeIds, final int[] coefficients, final int first, final int second) {
    if (first == second) {
      return;
    }
    final String moleculeId = moleculeIds[first];
    moleculeIds[first] = moleculeIds[second];
    moleculeIds[second] = moleculeId;
    final int coefficient = coefficients[first];
    coefficients[first] = coefficients[second];
    coefficients[second] = coefficient;
  }

  private static void requireUniqueMoleculeIds(final String[] moleculeIds) {
    for (int i = 1; i < moleculeIds.length; ++i) {
      if (!moleculeIds[i - 1].equals(moleculeIds[i])) {
        continue;
      }
      throw new IllegalArgumentException(
          "Reaction side must not contain duplicate molecule ids; increase coefficient instead.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionSideSignature)) {
      return false;
    }
    final ReactionSideSignature signature = (ReactionSideSignature) other;
    return sameParticipantsAs(signature);
  }

  public int hashCode() {
    return hashCode;
  }
}