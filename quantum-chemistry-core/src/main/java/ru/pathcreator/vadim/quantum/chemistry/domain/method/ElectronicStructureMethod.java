/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

/** Метод электронной структуры с типом, названием и spin treatment. */
public final class ElectronicStructureMethod {

  public static final ElectronicStructureMethod HARTREE_FOCK =
      new ElectronicStructureMethod(
          ElectronicStructureMethodType.HARTREE_FOCK,
          ElectronicStructureMethodName.of("Hartree-Fock"),
          ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL);
  private final ElectronicStructureMethodType type;
  private final ElectronicStructureMethodName name;
  private final ElectronicStructureSpinTreatment spinTreatment;

  private ElectronicStructureMethod(
      final ElectronicStructureMethodType type,
      final ElectronicStructureMethodName name,
      final ElectronicStructureSpinTreatment spinTreatment) {
    this.type = type;
    this.name = name;
    this.spinTreatment = spinTreatment;
  }

  public static ElectronicStructureMethod of(
      final ElectronicStructureMethodType type, final ElectronicStructureMethodName name) {
    return ElectronicStructureMethod.of(type, name, ElectronicStructureSpinTreatment.UNSPECIFIED);
  }

  public static ElectronicStructureMethod of(
      final ElectronicStructureMethodType type,
      final ElectronicStructureMethodName name,
      final ElectronicStructureSpinTreatment spinTreatment) {
    if (type == null) {
      throw new IllegalArgumentException("Electronic structure method type must not be null.");
    }
    if (name == null) {
      throw new IllegalArgumentException("Electronic structure method name must not be null.");
    }
    if (spinTreatment == null) {
      throw new IllegalArgumentException("Electronic structure spin treatment must not be null.");
    }
    return new ElectronicStructureMethod(type, name, spinTreatment);
  }

  public static ElectronicStructureMethod densityFunctional(final String functionalName) {
    return ElectronicStructureMethod.of(
        ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY,
        ElectronicStructureMethodName.of(functionalName));
  }

  public ElectronicStructureMethod withSpinTreatment(
      final ElectronicStructureSpinTreatment spinTreatment) {
    return ElectronicStructureMethod.of(this.type, this.name, spinTreatment);
  }

  public ElectronicStructureMethodType type() {
    return this.type;
  }

  public ElectronicStructureMethodName name() {
    return this.name;
  }

  public ElectronicStructureSpinTreatment spinTreatment() {
    return this.spinTreatment;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicStructureMethod)) {
      return false;
    }
    final ElectronicStructureMethod method = (ElectronicStructureMethod) other;
    return this.type == method.type
        && this.name.equals(method.name)
        && this.spinTreatment == method.spinTreatment;
  }

  public int hashCode() {
    int result = this.type.hashCode();
    result = 31 * result + this.name.hashCode();
    result = 31 * result + this.spinTreatment.hashCode();
    return result;
  }
}