/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.stereo;

public enum StereochemicalDescriptor {

  R,
  S,
  R_A,
  S_A,
  E,
  Z,
  CIS,
  TRANS,
  P,
  M,
  DELTA,
  LAMBDA,
  D,
  L,
  UNKNOWN;

  public StereochemicalDescriptorFamily family() {
    switch (this) {
      case R:
      case S:
        return StereochemicalDescriptorFamily.ABSOLUTE_TETRAHEDRAL_CONFIGURATION;
      case D:
      case L:
        return StereochemicalDescriptorFamily.RELATIVE_CONFIGURATION;
      case E:
      case Z:
      case CIS:
      case TRANS:
        return StereochemicalDescriptorFamily.GEOMETRIC_CONFIGURATION;
      case R_A:
      case S_A:
        return StereochemicalDescriptorFamily.AXIAL_CONFIGURATION;
      case P:
      case M:
      case DELTA:
      case LAMBDA:
        return StereochemicalDescriptorFamily.HELICAL_CONFIGURATION;
      case UNKNOWN:
        return StereochemicalDescriptorFamily.UNKNOWN;
      default:
        throw new IllegalStateException("Unsupported stereochemical descriptor.");
    }
  }

  public boolean absoluteConfiguration() {
    return this.family() == StereochemicalDescriptorFamily.ABSOLUTE_TETRAHEDRAL_CONFIGURATION
        || this.family() == StereochemicalDescriptorFamily.AXIAL_CONFIGURATION
        || this.family() == StereochemicalDescriptorFamily.HELICAL_CONFIGURATION;
  }

  public boolean geometricConfiguration() {
    return this.family() == StereochemicalDescriptorFamily.GEOMETRIC_CONFIGURATION;
  }

  public boolean relativeConfiguration() {
    return this.family() == StereochemicalDescriptorFamily.RELATIVE_CONFIGURATION;
  }
}