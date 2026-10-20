/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

/**
 * Описывает происхождение chemistry Hamiltonian, из которого создан пакет Quantum IR программ.
 */
public final class ChemistryQuantumPreparationProvenance {

  public static final ChemistryQuantumPreparationProvenance UNKNOWN =
      new ChemistryQuantumPreparationProvenance(
          "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "", 0, 0, 0, null, false);

  private final String referenceMethod;
  private final String preparationMode;
  private final String spinSector;
  private final String basisSetName;
  private final String activeSpaceDescription;
  private final String sourceCalculationId;
  private final int electronCount;
  private final int spinOrbitalCount;
  private final int spatialOrbitalCount;
  private final FermionToQubitMapping requestedMapping;
  private final boolean orbitalOptimized;

  private ChemistryQuantumPreparationProvenance(
      final String referenceMethod,
      final String preparationMode,
      final String spinSector,
      final String basisSetName,
      final String activeSpaceDescription,
      final String sourceCalculationId,
      final int electronCount,
      final int spinOrbitalCount,
      final int spatialOrbitalCount,
      final FermionToQubitMapping requestedMapping,
      final boolean orbitalOptimized) {
    this.referenceMethod = referenceMethod;
    this.preparationMode = preparationMode;
    this.spinSector = spinSector;
    this.basisSetName = basisSetName;
    this.activeSpaceDescription = activeSpaceDescription;
    this.sourceCalculationId = sourceCalculationId;
    this.electronCount = electronCount;
    this.spinOrbitalCount = spinOrbitalCount;
    this.spatialOrbitalCount = spatialOrbitalCount;
    this.requestedMapping = requestedMapping;
    this.orbitalOptimized = orbitalOptimized;
  }

  public static ChemistryQuantumPreparationProvenance of(
      final String referenceMethod,
      final String preparationMode,
      final String spinSector,
      final String basisSetName,
      final String activeSpaceDescription,
      final String sourceCalculationId,
      final int electronCount,
      final int spinOrbitalCount,
      final int spatialOrbitalCount,
      final FermionToQubitMapping requestedMapping,
      final boolean orbitalOptimized) {
    if (electronCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry quantum provenance electron count must not be negative.");
    }
    if (spinOrbitalCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry quantum provenance spin orbital count must not be negative.");
    }
    if (spatialOrbitalCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry quantum provenance spatial orbital count must not be negative.");
    }
    return new ChemistryQuantumPreparationProvenance(
        ChemistryQuantumPreparationProvenance.requiredText(referenceMethod, "reference method"),
        ChemistryQuantumPreparationProvenance.requiredText(preparationMode, "preparation mode"),
        ChemistryQuantumPreparationProvenance.requiredText(spinSector, "spin sector"),
        ChemistryQuantumPreparationProvenance.requiredText(basisSetName, "basis set name"),
        ChemistryQuantumPreparationProvenance.requiredText(
            activeSpaceDescription, "active space description"),
        sourceCalculationId == null ? "" : sourceCalculationId,
        electronCount,
        spinOrbitalCount,
        spatialOrbitalCount,
        requestedMapping,
        orbitalOptimized);
  }

  public String referenceMethod() {
    return this.referenceMethod;
  }

  public String preparationMode() {
    return this.preparationMode;
  }

  public String spinSector() {
    return this.spinSector;
  }

  public String basisSetName() {
    return this.basisSetName;
  }

  public String activeSpaceDescription() {
    return this.activeSpaceDescription;
  }

  public String sourceCalculationId() {
    return this.sourceCalculationId;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public int spinOrbitalCount() {
    return this.spinOrbitalCount;
  }

  public int spatialOrbitalCount() {
    return this.spatialOrbitalCount;
  }

  public FermionToQubitMapping requestedMapping() {
    return this.requestedMapping;
  }

  public boolean orbitalOptimized() {
    return this.orbitalOptimized;
  }

  public boolean known() {
    return this != UNKNOWN && !"UNKNOWN".equals(this.referenceMethod);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryQuantumPreparationProvenance)) {
      return false;
    }
    final ChemistryQuantumPreparationProvenance provenance =
        (ChemistryQuantumPreparationProvenance) other;
    return this.electronCount == provenance.electronCount
        && this.spinOrbitalCount == provenance.spinOrbitalCount
        && this.spatialOrbitalCount == provenance.spatialOrbitalCount
        && this.orbitalOptimized == provenance.orbitalOptimized
        && Objects.equals(this.referenceMethod, provenance.referenceMethod)
        && Objects.equals(this.preparationMode, provenance.preparationMode)
        && Objects.equals(this.spinSector, provenance.spinSector)
        && Objects.equals(this.basisSetName, provenance.basisSetName)
        && Objects.equals(this.activeSpaceDescription, provenance.activeSpaceDescription)
        && Objects.equals(this.sourceCalculationId, provenance.sourceCalculationId)
        && this.requestedMapping == provenance.requestedMapping;
  }

  public int hashCode() {
    return Objects.hash(
        this.referenceMethod,
        this.preparationMode,
        this.spinSector,
        this.basisSetName,
        this.activeSpaceDescription,
        this.sourceCalculationId,
        this.electronCount,
        this.spinOrbitalCount,
        this.spatialOrbitalCount,
        this.requestedMapping,
        this.orbitalOptimized);
  }

  private static String requiredText(final String value, final String subject) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "Chemistry quantum provenance " + subject + " must not be blank.");
    }
    return value;
  }
}
