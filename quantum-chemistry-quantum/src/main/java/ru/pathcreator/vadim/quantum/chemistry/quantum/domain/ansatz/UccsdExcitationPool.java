/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/**
 * Детерминированный spin-conserving operator pool для UCCSD и ADAPT-VQE.
 */
public final class UccsdExcitationPool {

  private final List<UccsdExcitation> excitations;
  private final int singleExcitationCount;
  private final int doubleExcitationCount;

  private UccsdExcitationPool(
      final List<UccsdExcitation> excitations,
      final int singleExcitationCount,
      final int doubleExcitationCount) {
    this.excitations = excitations;
    this.singleExcitationCount = singleExcitationCount;
    this.doubleExcitationCount = doubleExcitationCount;
  }

  public static UccsdExcitationPool spinConserving(final ActiveSpace activeSpace) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("UCCSD active space must not be null.");
    }
    final ArrayList<UccsdExcitation> excitations = new ArrayList<UccsdExcitation>();
    int singleCount = 0;
    int doubleCount = 0;
    final int occupiedEnd = activeSpace.electronCount();
    final int virtualEnd = activeSpace.spinOrbitalCount();
    for (int occupied = 0; occupied < occupiedEnd; ++occupied) {
      for (int virtual = occupiedEnd; virtual < virtualEnd; ++virtual) {
        if (UccsdExcitationPool.sameSpin(occupied, virtual)) {
          excitations.add(UccsdExcitation.single(occupied, virtual));
          ++singleCount;
        }
      }
    }
    for (int firstOccupied = 0; firstOccupied < occupiedEnd - 1; ++firstOccupied) {
      for (int secondOccupied = firstOccupied + 1; secondOccupied < occupiedEnd; ++secondOccupied) {
        for (int firstVirtual = occupiedEnd; firstVirtual < virtualEnd - 1; ++firstVirtual) {
          for (int secondVirtual = firstVirtual + 1; secondVirtual < virtualEnd; ++secondVirtual) {
            if (UccsdExcitationPool.sameSpinSet(
                firstOccupied, secondOccupied, firstVirtual, secondVirtual)) {
              excitations.add(
                  UccsdExcitation.doubleExcitation(
                      firstOccupied, secondOccupied, firstVirtual, secondVirtual));
              ++doubleCount;
            }
          }
        }
      }
    }
    return new UccsdExcitationPool(List.copyOf(excitations), singleCount, doubleCount);
  }

  public int excitationCount() {
    return this.excitations.size();
  }

  public int singleExcitationCount() {
    return this.singleExcitationCount;
  }

  public int doubleExcitationCount() {
    return this.doubleExcitationCount;
  }

  public UccsdExcitation excitation(final int index) {
    return this.excitations.get(index);
  }

  public List<UccsdExcitation> excitations() {
    return this.excitations;
  }

  public List<UccsdExcitation> limited(final int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("UCCSD excitation pool limit must be positive.");
    }
    if (limit >= this.excitations.size()) {
      return this.excitations;
    }
    return this.excitations.subList(0, limit);
  }

  private static boolean sameSpin(final int firstSpinOrbital, final int secondSpinOrbital) {
    return (firstSpinOrbital & 1) == (secondSpinOrbital & 1);
  }

  private static boolean sameSpinSet(
      final int firstOccupied,
      final int secondOccupied,
      final int firstVirtual,
      final int secondVirtual) {
    return UccsdExcitationPool.alphaCount(firstOccupied, secondOccupied)
        == UccsdExcitationPool.alphaCount(firstVirtual, secondVirtual);
  }

  private static int alphaCount(final int firstSpinOrbital, final int secondSpinOrbital) {
    int count = 0;
    if ((firstSpinOrbital & 1) == 0) {
      ++count;
    }
    if ((secondSpinOrbital & 1) == 0) {
      ++count;
    }
    return count;
  }
}
