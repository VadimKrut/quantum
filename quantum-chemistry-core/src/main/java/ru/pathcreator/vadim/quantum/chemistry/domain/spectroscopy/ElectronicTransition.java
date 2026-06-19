/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.DipoleMomentVector;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

public final class ElectronicTransition {

  private final String id;
  private final int stateIndex;
  private final ExcitedStateKind kind;
  private final EnergyValue energy;
  private final Wavelength wavelength;
  private final OscillatorStrength oscillatorStrength;
  private final DipoleMomentVector transitionDipoleMoment;

  private ElectronicTransition(
      final String id,
      final int stateIndex,
      final ExcitedStateKind kind,
      final EnergyValue energy,
      final Wavelength wavelength,
      final OscillatorStrength oscillatorStrength,
      final DipoleMomentVector transitionDipoleMoment) {
    this.id = id;
    this.stateIndex = stateIndex;
    this.kind = kind;
    this.energy = energy;
    this.wavelength = wavelength;
    this.oscillatorStrength = oscillatorStrength;
    this.transitionDipoleMoment = transitionDipoleMoment;
  }

  public static ElectronicTransition of(
      final String id,
      final int stateIndex,
      final ExcitedStateKind kind,
      final EnergyValue energy,
      final Wavelength wavelength,
      final OscillatorStrength oscillatorStrength,
      final DipoleMomentVector transitionDipoleMoment) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Electronic transition id");
    if (stateIndex < 1) {
      throw new IllegalArgumentException("Electronic transition state index must be positive.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Electronic transition kind must not be null.");
    }
    if (energy == null) {
      throw new IllegalArgumentException("Electronic transition energy must not be null.");
    }
    if (wavelength == null) {
      throw new IllegalArgumentException("Electronic transition wavelength must not be null.");
    }
    if (oscillatorStrength == null) {
      throw new IllegalArgumentException(
          "Electronic transition oscillator strength must not be null.");
    }
    return new ElectronicTransition(
        checkedId,
        stateIndex,
        kind,
        energy,
        wavelength,
        oscillatorStrength,
        transitionDipoleMoment);
  }

  public String id() {
    return this.id;
  }

  public int stateIndex() {
    return this.stateIndex;
  }

  public ExcitedStateKind kind() {
    return this.kind;
  }

  public EnergyValue energy() {
    return this.energy;
  }

  public Wavelength wavelength() {
    return this.wavelength;
  }

  public OscillatorStrength oscillatorStrength() {
    return this.oscillatorStrength;
  }

  public DipoleMomentVector transitionDipoleMoment() {
    return this.transitionDipoleMoment;
  }

  public boolean hasTransitionDipoleMoment() {
    return this.transitionDipoleMoment != null;
  }

  public boolean opticallyAllowed() {
    return this.oscillatorStrength.opticallyAllowed();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicTransition)) {
      return false;
    }
    final ElectronicTransition transition = (ElectronicTransition) other;
    return this.stateIndex == transition.stateIndex
        && Objects.equals(this.id, transition.id)
        && this.kind == transition.kind
        && Objects.equals(this.energy, transition.energy)
        && Objects.equals(this.wavelength, transition.wavelength)
        && Objects.equals(this.oscillatorStrength, transition.oscillatorStrength)
        && Objects.equals(this.transitionDipoleMoment, transition.transitionDipoleMoment);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {
          this.id,
          this.stateIndex,
          this.kind,
          this.energy,
          this.wavelength,
          this.oscillatorStrength,
          this.transitionDipoleMoment
        });
  }
}