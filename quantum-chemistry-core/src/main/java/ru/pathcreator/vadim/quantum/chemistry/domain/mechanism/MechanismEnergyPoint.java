/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.mechanism;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

public final class MechanismEnergyPoint {

  private final String id;
  private final MechanismPointKind kind;
  private final ReactionCoordinateValue coordinate;
  private final EnergyValue relativeEnergy;
  private final MoleculeId representativeMoleculeId;

  private MechanismEnergyPoint(
      final String id,
      final MechanismPointKind kind,
      final ReactionCoordinateValue coordinate,
      final EnergyValue relativeEnergy,
      final MoleculeId representativeMoleculeId) {
    this.id = id;
    this.kind = kind;
    this.coordinate = coordinate;
    this.relativeEnergy = relativeEnergy;
    this.representativeMoleculeId = representativeMoleculeId;
  }

  public static MechanismEnergyPoint of(
      final String id,
      final MechanismPointKind kind,
      final ReactionCoordinateValue coordinate,
      final EnergyValue relativeEnergy,
      final MoleculeId representativeMoleculeId) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Mechanism energy point id");
    if (kind == null) {
      throw new IllegalArgumentException("Mechanism energy point kind must not be null.");
    }
    if (coordinate == null) {
      throw new IllegalArgumentException("Mechanism energy point coordinate must not be null.");
    }
    if (relativeEnergy == null) {
      throw new IllegalArgumentException(
          "Mechanism energy point relative energy must not be null.");
    }
    return new MechanismEnergyPoint(
        checkedId, kind, coordinate, relativeEnergy, representativeMoleculeId);
  }

  public String id() {
    return this.id;
  }

  public MechanismPointKind kind() {
    return this.kind;
  }

  public ReactionCoordinateValue coordinate() {
    return this.coordinate;
  }

  public EnergyValue relativeEnergy() {
    return this.relativeEnergy;
  }

  public MoleculeId representativeMoleculeId() {
    return this.representativeMoleculeId;
  }

  public boolean hasRepresentativeMolecule() {
    return this.representativeMoleculeId != null;
  }

  public boolean transitionState() {
    return this.kind == MechanismPointKind.TRANSITION_STATE;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MechanismEnergyPoint)) {
      return false;
    }
    final MechanismEnergyPoint point = (MechanismEnergyPoint) other;
    return Objects.equals(this.id, point.id)
        && this.kind == point.kind
        && Objects.equals(this.coordinate, point.coordinate)
        && Objects.equals(this.relativeEnergy, point.relativeEnergy)
        && Objects.equals(this.representativeMoleculeId, point.representativeMoleculeId);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {
          this.id, this.kind, this.coordinate, this.relativeEnergy, this.representativeMoleculeId
        });
  }
}