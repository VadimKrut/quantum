/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import java.util.Objects;

public final class BasisSet {

  public static final BasisSet STO_3G = new BasisSet(BasisSetName.of("STO-3G"));
  private final BasisSetName name;

  private BasisSet(final BasisSetName name) {
    this.name = name;
  }

  public static BasisSet of(final BasisSetName name) {
    if (name == null) {
      throw new IllegalArgumentException("Basis set name must not be null.");
    }
    return new BasisSet(name);
  }

  public BasisSetName name() {
    return this.name;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BasisSet)) {
      return false;
    }
    final BasisSet basisSet = (BasisSet) other;
    return Objects.equals(this.name, basisSet.name);
  }

  public int hashCode() {
    return this.name.hashCode();
  }
}