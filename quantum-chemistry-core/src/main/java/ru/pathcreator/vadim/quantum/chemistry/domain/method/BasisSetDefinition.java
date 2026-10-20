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

/** Структурное basis set definition с atom shells и Gaussian primitives. */
public final class BasisSetDefinition {

  private final List<BasisAtomDefinition> atoms;
  private final int shellCount;
  private final int primitiveCount;
  private final int cartesianFunctionCount;

  private BasisSetDefinition(
      final List<BasisAtomDefinition> atoms,
      final int shellCount,
      final int primitiveCount,
      final int cartesianFunctionCount) {
    this.atoms = atoms;
    this.shellCount = shellCount;
    this.primitiveCount = primitiveCount;
    this.cartesianFunctionCount = cartesianFunctionCount;
  }

  public static BasisSetDefinition of(final List<BasisAtomDefinition> atoms) {
    final List<BasisAtomDefinition> checkedAtoms =
        List.copyOf(BasisSetDefinition.requireAtoms(atoms));
    int shellCount = 0;
    int primitiveCount = 0;
    int cartesianFunctionCount = 0;
    for (int i = 0; i < checkedAtoms.size(); ++i) {
      final BasisAtomDefinition atom = checkedAtoms.get(i);
      shellCount = Math.addExact(shellCount, atom.shellCount());
      primitiveCount = Math.addExact(primitiveCount, atom.primitiveCount());
      cartesianFunctionCount = Math.addExact(cartesianFunctionCount, atom.cartesianFunctionCount());
    }
    return new BasisSetDefinition(checkedAtoms, shellCount, primitiveCount, cartesianFunctionCount);
  }

  public List<BasisAtomDefinition> atoms() {
    return this.atoms;
  }

  public int atomDefinitionCount() {
    return this.atoms.size();
  }

  public int shellCount() {
    return this.shellCount;
  }

  public int primitiveCount() {
    return this.primitiveCount;
  }

  public int cartesianFunctionCount() {
    return this.cartesianFunctionCount;
  }

  public boolean supports(final ElementSymbol element) {
    return this.atomDefinition(element) != null;
  }

  public BasisAtomDefinition atomDefinition(final ElementSymbol element) {
    if (element == null) {
      throw new IllegalArgumentException("Basis atom lookup element must not be null.");
    }
    for (int i = 0; i < this.atoms.size(); ++i) {
      final BasisAtomDefinition atom = this.atoms.get(i);
      if (atom.element().equals(element)) {
        return atom;
      }
    }
    return null;
  }

  private static List<BasisAtomDefinition> requireAtoms(final List<BasisAtomDefinition> atoms) {
    if (atoms == null) {
      throw new IllegalArgumentException("Basis set atom definitions must not be null.");
    }
    if (atoms.isEmpty()) {
      throw new IllegalArgumentException("Basis set definition must contain at least one atom.");
    }
    for (int i = 0; i < atoms.size(); ++i) {
      final BasisAtomDefinition atom = atoms.get(i);
      if (atom == null) {
        throw new IllegalArgumentException("Basis set atom definition must not be null.");
      }
      for (int j = i + 1; j < atoms.size(); ++j) {
        if (atom.element().equals(atoms.get(j).element())) {
          throw new IllegalArgumentException(
              "Basis set atom definitions must not duplicate element.");
        }
      }
    }
    return atoms;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BasisSetDefinition)) {
      return false;
    }
    final BasisSetDefinition definition = (BasisSetDefinition) other;
    return Objects.equals(this.atoms, definition.atoms);
  }

  public int hashCode() {
    return this.atoms.hashCode();
  }
}
