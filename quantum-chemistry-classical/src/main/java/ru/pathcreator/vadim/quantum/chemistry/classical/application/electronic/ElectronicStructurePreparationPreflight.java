/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralPreflight;

/** Предварительная проверка подготовки электронной структуры до запуска дорогих интегральных расчетов. */
public final class ElectronicStructurePreparationPreflight {

  private final String requestReason;
  private final String activeSpaceReason;
  private final GaussianIntegralPreflight fullTensorPreflight;
  private final GaussianIntegralPreflight oneElectronPreflight;
  private final String directActiveSpaceDescription;
  private final String backendDescription;

  private ElectronicStructurePreparationPreflight(
      final String requestReason,
      final String activeSpaceReason,
      final GaussianIntegralPreflight fullTensorPreflight,
      final GaussianIntegralPreflight oneElectronPreflight,
      final String directActiveSpaceDescription,
      final String backendDescription) {
    this.requestReason = requestReason;
    this.activeSpaceReason = activeSpaceReason;
    this.fullTensorPreflight = fullTensorPreflight;
    this.oneElectronPreflight = oneElectronPreflight;
    this.directActiveSpaceDescription =
        directActiveSpaceDescription == null ? "" : directActiveSpaceDescription;
    this.backendDescription = backendDescription == null ? "" : backendDescription;
  }

  public static ElectronicStructurePreparationPreflight of(
      final String requestReason,
      final String activeSpaceReason,
      final GaussianIntegralPreflight fullTensorPreflight,
      final GaussianIntegralPreflight oneElectronPreflight) {
    return new ElectronicStructurePreparationPreflight(
        requestReason == null ? "" : requestReason,
        activeSpaceReason == null ? "" : activeSpaceReason,
        fullTensorPreflight,
        oneElectronPreflight,
        "",
        "");
  }

  public static ElectronicStructurePreparationPreflight ofDirectActiveSpace(
      final String requestReason,
      final String activeSpaceReason,
      final GaussianIntegralPreflight fullTensorPreflight,
      final GaussianIntegralPreflight oneElectronPreflight,
      final String directActiveSpaceDescription) {
    if (directActiveSpaceDescription == null || directActiveSpaceDescription.isBlank()) {
      throw new IllegalArgumentException(
          "Direct active-space preflight description must not be blank.");
    }
    return new ElectronicStructurePreparationPreflight(
        requestReason == null ? "" : requestReason,
        activeSpaceReason == null ? "" : activeSpaceReason,
        fullTensorPreflight,
        oneElectronPreflight,
        directActiveSpaceDescription,
        "");
  }

  public static ElectronicStructurePreparationPreflight ofBackendHamiltonian(
      final String requestReason, final String activeSpaceReason, final String backendDescription) {
    if (backendDescription == null || backendDescription.isBlank()) {
      throw new IllegalArgumentException(
          "Electronic structure backend preflight description must not be blank.");
    }
    return new ElectronicStructurePreparationPreflight(
        requestReason == null ? "" : requestReason,
        activeSpaceReason == null ? "" : activeSpaceReason,
        null,
        null,
        "",
        backendDescription);
  }

  public boolean supported() {
    return this.requestReason.isBlank()
        && this.activeSpaceReason.isBlank()
        && (this.backendHamiltonianSupported()
            || this.directActiveSpaceSupported()
            || (this.fullTensorPreflight != null && this.fullTensorPreflight.supported()));
  }

  public boolean oneElectronSupported() {
    return this.requestReason.isBlank()
        && this.activeSpaceReason.isBlank()
        && (this.backendHamiltonianSupported()
            || (this.oneElectronPreflight != null && this.oneElectronPreflight.supported()));
  }

  public boolean backendHamiltonianSupported() {
    return !this.backendDescription.isBlank()
        && this.requestReason.isBlank()
        && this.activeSpaceReason.isBlank();
  }

  public boolean directActiveSpaceSupported() {
    return !this.directActiveSpaceDescription.isBlank()
        && this.requestReason.isBlank()
        && this.activeSpaceReason.isBlank();
  }

  public String unsupportedReason() {
    if (!this.requestReason.isBlank()) {
      return this.requestReason;
    }
    if (this.directActiveSpaceSupported()) {
      return "";
    }
    if (this.fullTensorPreflight != null && !this.fullTensorPreflight.supported()) {
      return this.fullTensorPreflight.reason();
    }
    if (!this.activeSpaceReason.isBlank()) {
      return this.activeSpaceReason;
    }
    return "";
  }

  public String oneElectronUnsupportedReason() {
    if (!this.requestReason.isBlank()) {
      return this.requestReason;
    }
    if (this.oneElectronPreflight != null && !this.oneElectronPreflight.supported()) {
      return this.oneElectronPreflight.reason();
    }
    if (!this.activeSpaceReason.isBlank()) {
      return this.activeSpaceReason;
    }
    return "";
  }

  public String requestReason() {
    return this.requestReason;
  }

  public String activeSpaceReason() {
    return this.activeSpaceReason;
  }

  public String backendDescription() {
    return this.backendDescription;
  }

  public String directActiveSpaceDescription() {
    return this.directActiveSpaceDescription;
  }

  public boolean hasFullTensorPreflight() {
    return this.fullTensorPreflight != null;
  }

  public GaussianIntegralPreflight fullTensorPreflight() {
    if (this.fullTensorPreflight == null) {
      throw new IllegalStateException(
          "Electronic structure preflight does not contain full tensor result.");
    }
    return this.fullTensorPreflight;
  }

  public boolean hasOneElectronPreflight() {
    return this.oneElectronPreflight != null;
  }

  public GaussianIntegralPreflight oneElectronPreflight() {
    if (this.oneElectronPreflight == null) {
      throw new IllegalStateException(
          "Electronic structure preflight does not contain one-electron result.");
    }
    return this.oneElectronPreflight;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicStructurePreparationPreflight)) {
      return false;
    }
    final ElectronicStructurePreparationPreflight preflight =
        (ElectronicStructurePreparationPreflight) other;
    return this.requestReason.equals(preflight.requestReason)
        && this.activeSpaceReason.equals(preflight.activeSpaceReason)
        && Objects.equals(this.fullTensorPreflight, preflight.fullTensorPreflight)
        && Objects.equals(this.oneElectronPreflight, preflight.oneElectronPreflight)
        && this.directActiveSpaceDescription.equals(preflight.directActiveSpaceDescription)
        && this.backendDescription.equals(preflight.backendDescription);
  }

  public int hashCode() {
    return Objects.hash(
        this.requestReason,
        this.activeSpaceReason,
        this.fullTensorPreflight,
        this.oneElectronPreflight,
        this.directActiveSpaceDescription,
        this.backendDescription);
  }
}
