/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/**
 * Выбирает переносимое active-space окно по frontier orbital energies.
 */
public final class ActiveSpaceSelector {

  public ActiveSpaceSelectionResult select(
      final int electronCount,
      final double[] orbitalEnergies,
      final ActiveSpaceSelectionOptions options) {
    return this.selectWithSpinReference(
        electronCount, orbitalEnergies, (electronCount + 1) / 2, electronCount / 2, false, options);
  }

  public ActiveSpaceSelectionResult selectSpinResolved(
      final int electronCount,
      final double[] orbitalEnergies,
      final int alphaElectronCount,
      final int betaElectronCount,
      final ActiveSpaceSelectionOptions options) {
    return this.selectWithSpinReference(
        electronCount, orbitalEnergies, alphaElectronCount, betaElectronCount, true, options);
  }

  private ActiveSpaceSelectionResult selectWithSpinReference(
      final int electronCount,
      final double[] orbitalEnergies,
      final int alphaElectronCount,
      final int betaElectronCount,
      final boolean spinResolved,
      final ActiveSpaceSelectionOptions options) {
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Active-space selection electron count must be positive.");
    }
    if (alphaElectronCount < 0 || betaElectronCount < 0) {
      throw new IllegalArgumentException(
          "Active-space selection spin electron counts must be non-negative.");
    }
    if (alphaElectronCount + betaElectronCount != electronCount) {
      throw new IllegalArgumentException(
          "Active-space selection spin electron counts must sum to total electrons.");
    }
    if (orbitalEnergies == null || orbitalEnergies.length == 0) {
      throw new IllegalArgumentException(
          "Active-space selection orbital energies must not be empty.");
    }
    for (int orbitalIndex = 0; orbitalIndex < orbitalEnergies.length; ++orbitalIndex) {
      if (!Double.isFinite(orbitalEnergies[orbitalIndex])) {
        throw new IllegalArgumentException("Active-space selection orbital energy must be finite.");
      }
    }
    final ActiveSpaceSelectionOptions checkedOptions =
        options == null ? ActiveSpaceSelectionOptions.DEFAULT : options;
    final int occupiedOrbitalCount = Math.max(1, Math.max(alphaElectronCount, betaElectronCount));
    final int homoOrbital = Math.min(occupiedOrbitalCount - 1, orbitalEnergies.length - 1);
    final int lumoOrbital = Math.min(homoOrbital + 1, orbitalEnergies.length - 1);
    int start = Math.max(0, occupiedOrbitalCount - checkedOptions.frontierPairCount());
    int end =
        Math.min(orbitalEnergies.length, occupiedOrbitalCount + checkedOptions.frontierPairCount());
    start =
        this.extendDownByEnergyWindow(
            start,
            orbitalEnergies,
            orbitalEnergies[homoOrbital],
            checkedOptions.energyWindowHartree());
    end =
        this.extendUpByEnergyWindow(
            end,
            orbitalEnergies,
            orbitalEnergies[lumoOrbital],
            checkedOptions.energyWindowHartree());
    final int singlyOccupiedStart = Math.min(alphaElectronCount, betaElectronCount);
    final int singlyOccupiedEnd = Math.max(alphaElectronCount, betaElectronCount);
    if (singlyOccupiedEnd > singlyOccupiedStart) {
      start = Math.min(start, singlyOccupiedStart);
      end = Math.max(end, Math.min(orbitalEnergies.length, singlyOccupiedEnd + 1));
    }
    start =
        this.extendDownByAdjacentDegeneracy(
            start, orbitalEnergies, checkedOptions.energyWindowHartree());
    end =
        this.extendUpByAdjacentDegeneracy(
            end, orbitalEnergies, checkedOptions.energyWindowHartree());
    final int[] boundedWindow =
        this.boundWindow(
            start,
            end,
            occupiedOrbitalCount,
            orbitalEnergies.length,
            checkedOptions.minOrbitalCount(),
            checkedOptions.maxOrbitalCount(),
            singlyOccupiedStart,
            singlyOccupiedEnd);
    final int firstActiveOrbital = boundedWindow[0];
    final int exclusiveLastActiveOrbital = boundedWindow[1];
    final int activeOrbitalCount = exclusiveLastActiveOrbital - firstActiveOrbital;
    final int activeElectronCount =
        this.activeElectronCount(
            firstActiveOrbital, exclusiveLastActiveOrbital, alphaElectronCount, betaElectronCount);
    final int selectedOccupiedOrbitalCount = occupiedOrbitalCount - firstActiveOrbital;
    final int selectedVirtualOrbitalCount = exclusiveLastActiveOrbital - occupiedOrbitalCount;
    final double activeElectronFraction =
        Math.min(1.0, (double) activeElectronCount / (double) electronCount);
    final double activeOrbitalFraction =
        (double) activeOrbitalCount / (double) orbitalEnergies.length;
    final double selectionQualityScore =
        this.selectionQualityScore(
            selectedOccupiedOrbitalCount,
            selectedVirtualOrbitalCount,
            checkedOptions.frontierPairCount(),
            activeOrbitalFraction);
    final ActiveSpace activeSpace = ActiveSpace.of(activeElectronCount, activeOrbitalCount);
    return ActiveSpaceSelectionResult.of(
        activeSpace,
        firstActiveOrbital,
        exclusiveLastActiveOrbital,
        homoOrbital,
        lumoOrbital,
        orbitalEnergies[homoOrbital],
        orbitalEnergies[lumoOrbital],
        orbitalEnergies[lumoOrbital] - orbitalEnergies[homoOrbital],
        selectedOccupiedOrbitalCount,
        selectedVirtualOrbitalCount,
        activeElectronFraction,
        activeOrbitalFraction,
        selectionQualityScore,
        this.selectionQualityBand(selectionQualityScore),
        this.selectionDiagnostics(
            selectedOccupiedOrbitalCount,
            selectedVirtualOrbitalCount,
            checkedOptions.frontierPairCount(),
            activeOrbitalFraction,
            activeOrbitalCount,
            orbitalEnergies.length,
            spinResolved,
            singlyOccupiedStart,
            singlyOccupiedEnd,
            firstActiveOrbital,
            exclusiveLastActiveOrbital),
        this.orbitalRows(
            orbitalEnergies,
            firstActiveOrbital,
            exclusiveLastActiveOrbital,
            homoOrbital,
            lumoOrbital,
            alphaElectronCount,
            betaElectronCount));
  }

  private int extendDownByEnergyWindow(
      final int start,
      final double[] orbitalEnergies,
      final double homoEnergy,
      final double energyWindowHartree) {
    int result = start;
    while (result > 0 && homoEnergy - orbitalEnergies[result - 1] <= energyWindowHartree) {
      --result;
    }
    return result;
  }

  private int extendUpByEnergyWindow(
      final int end,
      final double[] orbitalEnergies,
      final double lumoEnergy,
      final double energyWindowHartree) {
    int result = end;
    while (result < orbitalEnergies.length
        && orbitalEnergies[result] - lumoEnergy <= energyWindowHartree) {
      ++result;
    }
    return result;
  }

  private int[] boundWindow(
      final int start,
      final int end,
      final int occupiedOrbitalCount,
      final int totalOrbitalCount,
      final int minOrbitalCount,
      final int maxOrbitalCount,
      final int singlyOccupiedStart,
      final int singlyOccupiedEnd) {
    int boundedStart = start;
    int boundedEnd = end;
    if (singlyOccupiedEnd > singlyOccupiedStart) {
      boundedStart = Math.min(boundedStart, singlyOccupiedStart);
      boundedEnd = Math.max(boundedEnd, Math.min(totalOrbitalCount, singlyOccupiedEnd + 1));
    }
    while (boundedEnd - boundedStart < minOrbitalCount) {
      if (boundedStart > 0) {
        --boundedStart;
      } else if (boundedEnd < totalOrbitalCount) {
        ++boundedEnd;
      } else {
        break;
      }
    }
    while (boundedEnd - boundedStart > maxOrbitalCount) {
      final boolean leftRemovesSinglyOccupied =
          boundedStart >= singlyOccupiedStart && boundedStart < singlyOccupiedEnd;
      final boolean rightRemovesSinglyOccupied =
          boundedEnd - 1 >= singlyOccupiedStart && boundedEnd - 1 < singlyOccupiedEnd;
      final int removeLeftDistance = Math.max(0, occupiedOrbitalCount - boundedStart);
      final int removeRightDistance = Math.max(0, boundedEnd - occupiedOrbitalCount);
      if (!rightRemovesSinglyOccupied
          && removeRightDistance > removeLeftDistance
          && boundedEnd > occupiedOrbitalCount) {
        --boundedEnd;
      } else if (!leftRemovesSinglyOccupied && boundedStart < occupiedOrbitalCount) {
        ++boundedStart;
      } else if (!rightRemovesSinglyOccupied) {
        --boundedEnd;
      } else if (!leftRemovesSinglyOccupied) {
        ++boundedStart;
      } else {
        break;
      }
    }
    if (boundedStart >= occupiedOrbitalCount) {
      boundedStart = Math.max(0, occupiedOrbitalCount - 1);
    }
    if (boundedEnd <= occupiedOrbitalCount) {
      boundedEnd = Math.min(totalOrbitalCount, occupiedOrbitalCount + 1);
    }
    return new int[] {boundedStart, boundedEnd};
  }

  private int extendDownByAdjacentDegeneracy(
      final int start, final double[] orbitalEnergies, final double energyWindowHartree) {
    int result = start;
    while (result > 0
        && orbitalEnergies[result] - orbitalEnergies[result - 1] <= energyWindowHartree) {
      --result;
    }
    return result;
  }

  private int extendUpByAdjacentDegeneracy(
      final int end, final double[] orbitalEnergies, final double energyWindowHartree) {
    int result = end;
    while (result < orbitalEnergies.length
        && orbitalEnergies[result] - orbitalEnergies[result - 1] <= energyWindowHartree) {
      ++result;
    }
    return result;
  }

  private int activeElectronCount(
      final int firstActiveOrbital,
      final int exclusiveLastActiveOrbital,
      final int alphaElectronCount,
      final int betaElectronCount) {
    int result = 0;
    for (int orbitalIndex = firstActiveOrbital;
        orbitalIndex < exclusiveLastActiveOrbital;
        ++orbitalIndex) {
      if (orbitalIndex < alphaElectronCount) {
        ++result;
      }
      if (orbitalIndex < betaElectronCount) {
        ++result;
      }
    }
    return result;
  }

  private double selectionQualityScore(
      final int selectedOccupiedOrbitalCount,
      final int selectedVirtualOrbitalCount,
      final int requestedFrontierPairCount,
      final double activeOrbitalFraction) {
    final double occupiedCoverage =
        Math.min(1.0, (double) selectedOccupiedOrbitalCount / (double) requestedFrontierPairCount);
    final double virtualCoverage =
        Math.min(1.0, (double) selectedVirtualOrbitalCount / (double) requestedFrontierPairCount);
    final double compactness = 1.0 - Math.max(0.0, activeOrbitalFraction - 0.75);
    return Math.max(
        0.0, Math.min(1.0, 0.4 * occupiedCoverage + 0.4 * virtualCoverage + 0.2 * compactness));
  }

  private String selectionQualityBand(final double selectionQualityScore) {
    if (selectionQualityScore >= 0.9) {
      return "HIGH";
    }
    if (selectionQualityScore >= 0.65) {
      return "MEDIUM";
    }
    return "LOW";
  }

  private List<String> selectionDiagnostics(
      final int selectedOccupiedOrbitalCount,
      final int selectedVirtualOrbitalCount,
      final int requestedFrontierPairCount,
      final double activeOrbitalFraction,
      final int activeOrbitalCount,
      final int totalOrbitalCount,
      final boolean spinResolved,
      final int singlyOccupiedStart,
      final int singlyOccupiedEnd,
      final int firstActiveOrbital,
      final int exclusiveLastActiveOrbital) {
    final ArrayList<String> diagnostics = new ArrayList<String>();
    diagnostics.add(
        "frontier_occupied_coverage="
            + selectedOccupiedOrbitalCount
            + "/"
            + requestedFrontierPairCount);
    diagnostics.add(
        "frontier_virtual_coverage="
            + selectedVirtualOrbitalCount
            + "/"
            + requestedFrontierPairCount);
    if (selectedOccupiedOrbitalCount < requestedFrontierPairCount) {
      diagnostics.add("active_space_truncated_below_homo");
    }
    if (selectedVirtualOrbitalCount < requestedFrontierPairCount) {
      diagnostics.add("active_space_truncated_above_lumo");
    }
    if (activeOrbitalFraction > 0.75) {
      diagnostics.add(
          "active_space_large_fraction=" + activeOrbitalCount + "/" + totalOrbitalCount);
    }
    if (spinResolved) {
      diagnostics.add("spin_resolved_frontier_reference");
    }
    if (singlyOccupiedEnd > singlyOccupiedStart) {
      diagnostics.add(
          "singly_occupied_orbital_window=" + singlyOccupiedStart + ".." + (singlyOccupiedEnd - 1));
      if (firstActiveOrbital <= singlyOccupiedStart
          && exclusiveLastActiveOrbital >= singlyOccupiedEnd) {
        diagnostics.add("singly_occupied_orbitals_retained");
      } else {
        diagnostics.add("singly_occupied_orbitals_truncated_by_max_window");
      }
    }
    return diagnostics;
  }

  private ArrayList<String> orbitalRows(
      final double[] orbitalEnergies,
      final int firstActiveOrbital,
      final int exclusiveLastActiveOrbital,
      final int homoOrbital,
      final int lumoOrbital,
      final int alphaElectronCount,
      final int betaElectronCount) {
    final ArrayList<String> rows = new ArrayList<String>(orbitalEnergies.length);
    for (int orbitalIndex = 0; orbitalIndex < orbitalEnergies.length; ++orbitalIndex) {
      final String role =
          orbitalIndex < firstActiveOrbital
              ? "CORE"
              : orbitalIndex < exclusiveLastActiveOrbital ? "ACTIVE" : "VIRTUAL";
      final String frontier =
          orbitalIndex == homoOrbital ? "HOMO" : orbitalIndex == lumoOrbital ? "LUMO" : "";
      rows.add(
          orbitalIndex
              + "|"
              + role
              + "|"
              + frontier
              + "|"
              + this.occupationLabel(orbitalIndex, alphaElectronCount, betaElectronCount)
              + "|"
              + orbitalEnergies[orbitalIndex]);
    }
    return rows;
  }

  private String occupationLabel(
      final int orbitalIndex, final int alphaElectronCount, final int betaElectronCount) {
    final boolean alphaOccupied = orbitalIndex < alphaElectronCount;
    final boolean betaOccupied = orbitalIndex < betaElectronCount;
    if (alphaOccupied && betaOccupied) {
      return "DOUBLY_OCCUPIED";
    }
    if (alphaOccupied) {
      return "SINGLY_OCCUPIED_ALPHA";
    }
    if (betaOccupied) {
      return "SINGLY_OCCUPIED_BETA";
    }
    return "UNOCCUPIED";
  }
}
