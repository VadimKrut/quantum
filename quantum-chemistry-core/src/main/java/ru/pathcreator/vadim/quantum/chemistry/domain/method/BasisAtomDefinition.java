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
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;

/** Basis definition для одного chemical element. */
public final class BasisAtomDefinition {

  private final ElementSymbol element;
  private final List<BasisShell> shells;

  private BasisAtomDefinition(final ElementSymbol element, final List<BasisShell> shells) {
    this.element = element;
    this.shells = shells;
  }

  public static BasisAtomDefinition of(final ElementSymbol element, final List<BasisShell> shells) {
    if (element == null) {
      throw new IllegalArgumentException("Basis atom element must not be null.");
    }
    final List<BasisShell> checkedShells = List.copyOf(BasisAtomDefinition.requireShells(shells));
    return new BasisAtomDefinition(element, checkedShells);
  }

  public ElementSymbol element() {
    return this.element;
  }

  public List<BasisShell> shells() {
    return this.shells;
  }

  public int shellCount() {
    return this.shells.size();
  }

  public int primitiveCount() {
    int count = 0;
    for (int i = 0; i < this.shells.size(); ++i) {
      count = Math.addExact(count, this.shells.get(i).primitiveCount());
    }
    return count;
  }

  public int cartesianFunctionCount() {
    int count = 0;
    for (int i = 0; i < this.shells.size(); ++i) {
      count = Math.addExact(count, this.shells.get(i).cartesianFunctionCount());
    }
    return count;
  }

  private static List<BasisShell> requireShells(final List<BasisShell> shells) {
    if (shells == null) {
      throw new IllegalArgumentException("Basis atom shells must not be null.");
    }
    if (shells.isEmpty()) {
      throw new IllegalArgumentException("Basis atom must contain at least one shell.");
    }
    for (int i = 0; i < shells.size(); ++i) {
      if (shells.get(i) == null) {
        throw new IllegalArgumentException("Basis atom shell must not be null.");
      }
    }
    return shells;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BasisAtomDefinition)) {
      return false;
    }
    final BasisAtomDefinition definition = (BasisAtomDefinition) other;
    return Objects.equals(this.element, definition.element)
        && Objects.equals(this.shells, definition.shells);
  }

  public int hashCode() {
    int result = this.element.hashCode();
    result = 31 * result + this.shells.hashCode();
    return result;
  }
}
