/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend.ClassicalBackendExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;

/**
 * Описание возможностей реального или виртуального классического backend.
 */
public final class ClassicalBackendProfile {

  private final String id;
  private final String displayName;
  private final String version;
  private final ClassicalBackendExecutionMode executionMode;
  private final List<ChemistrySubjectKind> supportedSubjectKinds;
  private final List<ClassicalCalculationKind> supportedCalculationKinds;
  private final List<ElectronicStructureMethodType> supportedMethodTypes;
  private final List<String> supportedBasisSets;
  private final List<ClassicalCapability> capabilities;
  private final long maxAtoms;
  private final long maxBonds;
  private final long maxElectrons;
  private final long maxActiveOrbitals;

  private ClassicalBackendProfile(
      final String id,
      final String displayName,
      final String version,
      final ClassicalBackendExecutionMode executionMode,
      final List<ChemistrySubjectKind> supportedSubjectKinds,
      final List<ClassicalCalculationKind> supportedCalculationKinds,
      final List<ElectronicStructureMethodType> supportedMethodTypes,
      final List<String> supportedBasisSets,
      final List<ClassicalCapability> capabilities,
      final long maxAtoms,
      final long maxBonds,
      final long maxElectrons,
      final long maxActiveOrbitals
  ) {
    this.id = id;
    this.displayName = displayName;
    this.version = version;
    this.executionMode = executionMode;
    this.supportedSubjectKinds = supportedSubjectKinds;
    this.supportedCalculationKinds = supportedCalculationKinds;
    this.supportedMethodTypes = supportedMethodTypes;
    this.supportedBasisSets = supportedBasisSets;
    this.capabilities = capabilities;
    this.maxAtoms = maxAtoms;
    this.maxBonds = maxBonds;
    this.maxElectrons = maxElectrons;
    this.maxActiveOrbitals = maxActiveOrbitals;
  }

  public static ClassicalBackendProfile of(
      final String id,
      final String displayName,
      final String version,
      final ClassicalBackendExecutionMode executionMode,
      final List<ChemistrySubjectKind> supportedSubjectKinds,
      final List<ClassicalCalculationKind> supportedCalculationKinds,
      final List<ElectronicStructureMethodType> supportedMethodTypes,
      final List<String> supportedBasisSets,
      final List<ClassicalCapability> capabilities,
      final long maxAtoms,
      final long maxBonds,
      final long maxElectrons,
      final long maxActiveOrbitals
  ) {
    if (executionMode == null) {
      throw new IllegalArgumentException("Classical backend execution mode must not be null.");
    }
    ClassicalBackendProfile.requirePositiveLimit(maxAtoms, "max atoms");
    ClassicalBackendProfile.requirePositiveLimit(maxBonds, "max bonds");
    ClassicalBackendProfile.requirePositiveLimit(maxElectrons, "max electrons");
    ClassicalBackendProfile.requirePositiveLimit(maxActiveOrbitals, "max active orbitals");
    return new ClassicalBackendProfile(
        IdentifierValue.requireIdentifier(
            id,
            "Classical backend profile id"),
        TextValue.requireText(
            displayName,
            "Classical backend profile display name"),
        TextValue.requireText(
            version,
            "Classical backend profile version"),
        executionMode,
        List.copyOf(ClassicalBackendProfile.requireList(supportedSubjectKinds, "subject kinds")),
        List.copyOf(ClassicalBackendProfile.requireList(supportedCalculationKinds, "calculation kinds")),
        List.copyOf(ClassicalBackendProfile.requireList(supportedMethodTypes, "method types")),
        List.copyOf(ClassicalBackendProfile.requireBasisSets(supportedBasisSets)),
        List.copyOf(ClassicalBackendProfile.requireList(capabilities, "capabilities")),
        maxAtoms,
        maxBonds,
        maxElectrons,
        maxActiveOrbitals);
  }

  public String id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public String version() {
    return this.version;
  }

  public ClassicalBackendExecutionMode executionMode() {
    return this.executionMode;
  }

  public List<ChemistrySubjectKind> supportedSubjectKinds() {
    return this.supportedSubjectKinds;
  }

  public List<ClassicalCalculationKind> supportedCalculationKinds() {
    return this.supportedCalculationKinds;
  }

  public List<ElectronicStructureMethodType> supportedMethodTypes() {
    return this.supportedMethodTypes;
  }

  public List<String> supportedBasisSets() {
    return this.supportedBasisSets;
  }

  public List<ClassicalCapability> capabilities() {
    return this.capabilities;
  }

  public long maxAtoms() {
    return this.maxAtoms;
  }

  public long maxBonds() {
    return this.maxBonds;
  }

  public long maxElectrons() {
    return this.maxElectrons;
  }

  public long maxActiveOrbitals() {
    return this.maxActiveOrbitals;
  }

  public boolean supportsSubjectKind(final ChemistrySubjectKind kind) {
    return this.supportedSubjectKinds.contains(kind);
  }

  public boolean supportsCalculationKind(final ClassicalCalculationKind kind) {
    return this.supportedCalculationKinds.contains(kind);
  }

  public boolean supportsMethodType(final ElectronicStructureMethodType type) {
    return this.supportedMethodTypes.contains(type);
  }

  public boolean supportsBasisSet(final String basisSetName) {
    return this.supportedBasisSets.isEmpty() || this.supportedBasisSets.contains(basisSetName);
  }

  public boolean hasCapability(final ClassicalCapability capability) {
    return this.capabilities.contains(capability);
  }

  private static void requirePositiveLimit(
      final long value,
      final String name
  ) {
    if (value <= 0L) {
      throw new IllegalArgumentException("Classical backend " + name + " must be positive.");
    }
  }

  private static <T> List<T> requireList(
      final List<T> values,
      final String subjectName
  ) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("Classical backend " + subjectName + " must not be empty.");
    }
    for (int i = 0; i < values.size(); ++i) {
      if (values.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Classical backend " + subjectName + " contains null value.");
    }
    return values;
  }

  private static List<String> requireBasisSets(final List<String> basisSets) {
    if (basisSets == null) {
      return List.of();
    }
    for (int i = 0; i < basisSets.size(); ++i) {
      TextValue.requireText(
          basisSets.get(i),
          "Classical backend basis set");
    }
    return basisSets;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalBackendProfile)) {
      return false;
    }
    final ClassicalBackendProfile profile = (ClassicalBackendProfile) other;
    return this.maxAtoms == profile.maxAtoms
        && this.maxBonds == profile.maxBonds
        && this.maxElectrons == profile.maxElectrons
        && this.maxActiveOrbitals == profile.maxActiveOrbitals
        && Objects.equals(this.id, profile.id)
        && Objects.equals(this.displayName, profile.displayName)
        && Objects.equals(this.version, profile.version)
        && this.executionMode == profile.executionMode
        && Objects.equals(this.supportedSubjectKinds, profile.supportedSubjectKinds)
        && Objects.equals(this.supportedCalculationKinds, profile.supportedCalculationKinds)
        && Objects.equals(this.supportedMethodTypes, profile.supportedMethodTypes)
        && Objects.equals(this.supportedBasisSets, profile.supportedBasisSets)
        && Objects.equals(this.capabilities, profile.capabilities);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.displayName);
    result = ChemistryHash.include(result, this.version);
    result = ChemistryHash.include(result, this.executionMode);
    result = ChemistryHash.include(result, this.supportedSubjectKinds);
    result = ChemistryHash.include(result, this.supportedCalculationKinds);
    result = ChemistryHash.include(result, this.supportedMethodTypes);
    result = ChemistryHash.include(result, this.supportedBasisSets);
    result = ChemistryHash.include(result, this.capabilities);
    result = ChemistryHash.include(result, this.maxAtoms);
    result = ChemistryHash.include(result, this.maxBonds);
    result = ChemistryHash.include(result, this.maxElectrons);
    result = ChemistryHash.include(result, this.maxActiveOrbitals);
    return result;
  }
}