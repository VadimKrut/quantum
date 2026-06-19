/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Молекулярная orbital с global index, optional active-space index, энергией и occupation. */
public final class MolecularOrbital {

  private final SpatialOrbitalIndex index;
  private final SpatialOrbitalIndex activeSpaceIndex;
  private final double energyHartree;
  private final MolecularOrbitalOccupation occupation;
  private final MolecularOrbitalRole role;
  private final String symmetryLabel;

  private MolecularOrbital(
      final SpatialOrbitalIndex index,
      final SpatialOrbitalIndex activeSpaceIndex,
      final double energyHartree,
      final MolecularOrbitalOccupation occupation,
      final MolecularOrbitalRole role,
      final String symmetryLabel) {
    this.index = index;
    this.activeSpaceIndex = activeSpaceIndex;
    this.energyHartree = energyHartree;
    this.occupation = occupation;
    this.role = role;
    this.symmetryLabel = symmetryLabel;
  }

  public static MolecularOrbital of(
      final int index,
      final double energyHartree,
      final MolecularOrbitalOccupation occupation,
      final MolecularOrbitalRole role) {
    return MolecularOrbital.of(
        SpatialOrbitalIndex.of(index),
        MolecularOrbital.defaultActiveSpaceIndex(SpatialOrbitalIndex.of(index), role),
        energyHartree,
        occupation,
        role,
        null);
  }

  public static MolecularOrbital of(
      final int index,
      final int activeSpaceIndex,
      final double energyHartree,
      final MolecularOrbitalOccupation occupation,
      final MolecularOrbitalRole role,
      final String symmetryLabel) {
    return MolecularOrbital.of(
        SpatialOrbitalIndex.of(index),
        SpatialOrbitalIndex.of(activeSpaceIndex),
        energyHartree,
        occupation,
        role,
        symmetryLabel);
  }

  public static MolecularOrbital of(
      final int index,
      final double energyHartree,
      final MolecularOrbitalOccupation occupation,
      final MolecularOrbitalRole role,
      final String symmetryLabel) {
    return MolecularOrbital.of(
        SpatialOrbitalIndex.of(index),
        MolecularOrbital.defaultActiveSpaceIndex(SpatialOrbitalIndex.of(index), role),
        energyHartree,
        occupation,
        role,
        symmetryLabel);
  }

  public static MolecularOrbital of(
      final SpatialOrbitalIndex index,
      final SpatialOrbitalIndex activeSpaceIndex,
      final double energyHartree,
      final MolecularOrbitalOccupation occupation,
      final MolecularOrbitalRole role,
      final String symmetryLabel) {
    if (index == null) {
      throw new IllegalArgumentException("Molecular orbital index must not be null.");
    }
    if (!Double.isFinite(energyHartree)) {
      throw new IllegalArgumentException("Molecular orbital energy must be finite.");
    }
    if (occupation == null) {
      throw new IllegalArgumentException("Molecular orbital occupation must not be null.");
    }
    if (role == null) {
      throw new IllegalArgumentException("Molecular orbital role must not be null.");
    }
    if (role == MolecularOrbitalRole.ACTIVE && activeSpaceIndex == null) {
      throw new IllegalArgumentException(
          "Active molecular orbital must define active space index.");
    }
    if (role != MolecularOrbitalRole.ACTIVE && activeSpaceIndex != null) {
      throw new IllegalArgumentException(
          "Inactive molecular orbital must not define active space index.");
    }
    return new MolecularOrbital(
        index,
        activeSpaceIndex,
        energyHartree,
        occupation,
        role,
        TextValue.optionalText(symmetryLabel));
  }

  public SpatialOrbitalIndex index() {
    return index;
  }

  public SpatialOrbitalIndex activeSpaceIndex() {
    return activeSpaceIndex;
  }

  public boolean active() {
    return role == MolecularOrbitalRole.ACTIVE;
  }

  public double energyHartree() {
    return energyHartree;
  }

  public MolecularOrbitalOccupation occupation() {
    return occupation;
  }

  public MolecularOrbitalRole role() {
    return role;
  }

  public String symmetryLabel() {
    return symmetryLabel;
  }

  public boolean hasSymmetryLabel() {
    return symmetryLabel != null;
  }

  private static SpatialOrbitalIndex defaultActiveSpaceIndex(
      final SpatialOrbitalIndex index, final MolecularOrbitalRole role) {
    if (role == MolecularOrbitalRole.ACTIVE) {
      return index;
    }
    return null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularOrbital)) {
      return false;
    }
    final MolecularOrbital orbital = (MolecularOrbital) other;
    return Double.compare(energyHartree, orbital.energyHartree) == 0
        && Objects.equals(index, orbital.index)
        && Objects.equals(activeSpaceIndex, orbital.activeSpaceIndex)
        && Objects.equals(occupation, orbital.occupation)
        && role == orbital.role
        && Objects.equals(symmetryLabel, orbital.symmetryLabel);
  }

  public int hashCode() {
    return Objects.hash(index, activeSpaceIndex, energyHartree, occupation, role, symmetryLabel);
  }
}