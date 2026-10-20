/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import java.util.List;
import java.util.Objects;

/** Contracted basis shell с типом angular momentum и списком Gaussian primitives. */
public final class BasisShell {

  private final BasisShellAngularMomentum angularMomentum;
  private final List<BasisPrimitive> primitives;

  private BasisShell(
      final BasisShellAngularMomentum angularMomentum, final List<BasisPrimitive> primitives) {
    this.angularMomentum = angularMomentum;
    this.primitives = primitives;
  }

  public static BasisShell of(
      final BasisShellAngularMomentum angularMomentum, final List<BasisPrimitive> primitives) {
    if (angularMomentum == null) {
      throw new IllegalArgumentException("Basis shell angular momentum must not be null.");
    }
    final List<BasisPrimitive> checkedPrimitives =
        List.copyOf(BasisShell.requirePrimitives(primitives));
    return new BasisShell(angularMomentum, checkedPrimitives);
  }

  public BasisShellAngularMomentum angularMomentum() {
    return this.angularMomentum;
  }

  public List<BasisPrimitive> primitives() {
    return this.primitives;
  }

  public int primitiveCount() {
    return this.primitives.size();
  }

  public int cartesianFunctionCount() {
    return this.angularMomentum.cartesianFunctionCount();
  }

  private static List<BasisPrimitive> requirePrimitives(final List<BasisPrimitive> primitives) {
    if (primitives == null) {
      throw new IllegalArgumentException("Basis shell primitives must not be null.");
    }
    if (primitives.isEmpty()) {
      throw new IllegalArgumentException("Basis shell must contain at least one primitive.");
    }
    for (int i = 0; i < primitives.size(); ++i) {
      if (primitives.get(i) == null) {
        throw new IllegalArgumentException("Basis shell primitive must not be null.");
      }
    }
    return primitives;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BasisShell)) {
      return false;
    }
    final BasisShell shell = (BasisShell) other;
    return this.angularMomentum == shell.angularMomentum
        && Objects.equals(this.primitives, shell.primitives);
  }

  public int hashCode() {
    int result = this.angularMomentum.hashCode();
    result = 31 * result + this.primitives.hashCode();
    return result;
  }
}
