/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

/** Метод электронной структуры с типом, названием, spin treatment и опциональным DFT-функционалом. */
public final class ElectronicStructureMethod {

  public static final ElectronicStructureMethod HARTREE_FOCK =
      new ElectronicStructureMethod(
          ElectronicStructureMethodType.HARTREE_FOCK,
          ElectronicStructureMethodName.of("Hartree-Fock"),
          ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL,
          null);
  public static final ElectronicStructureMethod SEMI_EMPIRICAL =
      new ElectronicStructureMethod(
          ElectronicStructureMethodType.SEMI_EMPIRICAL,
          ElectronicStructureMethodName.of("Native semiempirical valence model"),
          ElectronicStructureSpinTreatment.UNSPECIFIED,
          null);
  private final ElectronicStructureMethodType type;
  private final ElectronicStructureMethodName name;
  private final ElectronicStructureSpinTreatment spinTreatment;
  private final DensityFunctionalDefinition densityFunctionalDefinition;

  private ElectronicStructureMethod(
      final ElectronicStructureMethodType type,
      final ElectronicStructureMethodName name,
      final ElectronicStructureSpinTreatment spinTreatment,
      final DensityFunctionalDefinition densityFunctionalDefinition) {
    this.type = type;
    this.name = name;
    this.spinTreatment = spinTreatment;
    this.densityFunctionalDefinition = densityFunctionalDefinition;
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
    return new ElectronicStructureMethod(type, name, spinTreatment, null);
  }

  public static ElectronicStructureMethod densityFunctional(final String functionalName) {
    return ElectronicStructureMethod.densityFunctional(
        DensityFunctionalDefinition.resolve(functionalName));
  }

  public static ElectronicStructureMethod densityFunctional(
      final DensityFunctionalDefinition definition) {
    if (definition == null) {
      throw new IllegalArgumentException("Density functional definition must not be null.");
    }
    return new ElectronicStructureMethod(
        ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY,
        ElectronicStructureMethodName.of(definition.name()),
        ElectronicStructureSpinTreatment.UNSPECIFIED,
        definition);
  }

  public ElectronicStructureMethod withSpinTreatment(
      final ElectronicStructureSpinTreatment spinTreatment) {
    if (spinTreatment == null) {
      throw new IllegalArgumentException("Electronic structure spin treatment must not be null.");
    }
    return new ElectronicStructureMethod(
        this.type, this.name, spinTreatment, this.densityFunctionalDefinition);
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

  public boolean hasDensityFunctionalDefinition() {
    return this.densityFunctionalDefinition != null;
  }

  public DensityFunctionalDefinition densityFunctionalDefinition() {
    if (this.densityFunctionalDefinition == null) {
      throw new IllegalStateException(
          "Electronic structure method does not contain DFT functional definition.");
    }
    return this.densityFunctionalDefinition;
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
        && this.spinTreatment == method.spinTreatment
        && ElectronicStructureMethod.equalsNullable(
            this.densityFunctionalDefinition, method.densityFunctionalDefinition);
  }

  public int hashCode() {
    int result = this.type.hashCode();
    result = 31 * result + this.name.hashCode();
    result = 31 * result + this.spinTreatment.hashCode();
    result =
        31 * result
            + (this.densityFunctionalDefinition == null
                ? 0
                : this.densityFunctionalDefinition.hashCode());
    return result;
  }

  private static boolean equalsNullable(
      final DensityFunctionalDefinition first, final DensityFunctionalDefinition second) {
    if (first == null) {
      return second == null;
    }
    return first.equals(second);
  }
}
