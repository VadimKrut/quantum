/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

/**
 * Версия собственного текстового формата хранения химических проектов.
 */
public final class ChemistryStorageFormatVersion {

  public static final ChemistryStorageFormatVersion CURRENT = new ChemistryStorageFormatVersion(1);
  private static final int MIN_VERSION = 1;
  private static final int MAX_VERSION = 1;
  private final int number;

  private ChemistryStorageFormatVersion(final int number) {
    this.number = number;
  }

  public static ChemistryStorageFormatVersion of(final int number) {
    if (number < MIN_VERSION || number > MAX_VERSION) {
      throw new IllegalArgumentException("Unsupported chemistry storage format version.");
    }
    if (number == CURRENT.number) {
      return CURRENT;
    }
    return new ChemistryStorageFormatVersion(number);
  }

  public int number() {
    return this.number;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageFormatVersion)) {
      return false;
    }
    final ChemistryStorageFormatVersion version = (ChemistryStorageFormatVersion) other;
    return this.number == version.number;
  }

  public int hashCode() {
    return this.number;
  }
}