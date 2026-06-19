/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

public final class Isotope {

  private static final int MIN_MASS_NUMBER = 1;
  private static final int MAX_MASS_NUMBER = 300;
  private final int massNumber;

  private Isotope(final int massNumber) {
    this.massNumber = massNumber;
  }

  public static Isotope of(final int massNumber) {
    if (massNumber < MIN_MASS_NUMBER || massNumber > MAX_MASS_NUMBER) {
      throw new IllegalArgumentException("Isotope mass number must be between 1 and 300.");
    }
    return new Isotope(massNumber);
  }

  public int massNumber() {
    return this.massNumber;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Isotope)) {
      return false;
    }
    final Isotope isotope = (Isotope) other;
    return this.massNumber == isotope.massNumber;
  }

  public int hashCode() {
    return this.massNumber;
  }
}