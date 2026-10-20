/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

/**
 * Настройки генерации пакета Quantum IR программ из chemistry Hamiltonian.
 */
public final class ChemistryQuantumProgramPackageOptions {

  public static final ChemistryQuantumProgramPackageOptions FULL =
      ChemistryQuantumProgramPackageOptions.of(
          Arrays.asList(FermionToQubitMapping.values()),
          EnumSet.allOf(ChemistryQuantumProgramKind.class),
          1.0E-8);

  private static final int DEFAULT_ADAPT_OPERATOR_LIMIT = Integer.MAX_VALUE;

  private final List<FermionToQubitMapping> mappings;
  private final Set<ChemistryQuantumProgramKind> programKinds;
  private final double verificationTolerance;
  private final int adaptOperatorLimit;

  private ChemistryQuantumProgramPackageOptions(
      final List<FermionToQubitMapping> mappings,
      final Set<ChemistryQuantumProgramKind> programKinds,
      final double verificationTolerance,
      final int adaptOperatorLimit) {
    this.mappings = mappings;
    this.programKinds = programKinds;
    this.verificationTolerance = verificationTolerance;
    this.adaptOperatorLimit = adaptOperatorLimit;
  }

  public static ChemistryQuantumProgramPackageOptions of(
      final List<FermionToQubitMapping> mappings,
      final Set<ChemistryQuantumProgramKind> programKinds,
      final double verificationTolerance) {
    return ChemistryQuantumProgramPackageOptions.of(
        mappings, programKinds, verificationTolerance, DEFAULT_ADAPT_OPERATOR_LIMIT);
  }

  public static ChemistryQuantumProgramPackageOptions of(
      final List<FermionToQubitMapping> mappings,
      final Set<ChemistryQuantumProgramKind> programKinds,
      final double verificationTolerance,
      final int adaptOperatorLimit) {
    final List<FermionToQubitMapping> checkedMappings =
        ChemistryQuantumProgramPackageOptions.checkedMappings(mappings);
    final Set<ChemistryQuantumProgramKind> checkedKinds =
        ChemistryQuantumProgramPackageOptions.checkedKinds(programKinds);
    if (!Double.isFinite(verificationTolerance) || verificationTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Chemistry quantum package verification tolerance must be finite and non-negative.");
    }
    if (adaptOperatorLimit <= 0) {
      throw new IllegalArgumentException(
          "Chemistry quantum package ADAPT operator limit must be positive.");
    }
    return new ChemistryQuantumProgramPackageOptions(
        checkedMappings, checkedKinds, verificationTolerance, adaptOperatorLimit);
  }

  public static ChemistryQuantumProgramPackageOptions requestedMappingOnly(
      final FermionToQubitMapping mapping,
      final Set<ChemistryQuantumProgramKind> programKinds,
      final double verificationTolerance) {
    if (mapping == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package requested mapping must not be null.");
    }
    return ChemistryQuantumProgramPackageOptions.of(
        List.of(mapping), programKinds, verificationTolerance);
  }

  public static ChemistryQuantumProgramPackageOptions requestedMappingOnly(
      final FermionToQubitMapping mapping,
      final Set<ChemistryQuantumProgramKind> programKinds,
      final double verificationTolerance,
      final int adaptOperatorLimit) {
    if (mapping == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum package requested mapping must not be null.");
    }
    return ChemistryQuantumProgramPackageOptions.of(
        List.of(mapping), programKinds, verificationTolerance, adaptOperatorLimit);
  }

  public List<FermionToQubitMapping> mappings() {
    return this.mappings;
  }

  public boolean includes(final ChemistryQuantumProgramKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Chemistry quantum package kind must not be null.");
    }
    return this.programKinds.contains(kind);
  }

  public double verificationTolerance() {
    return this.verificationTolerance;
  }

  public int adaptOperatorLimit() {
    return this.adaptOperatorLimit;
  }

  private static List<FermionToQubitMapping> checkedMappings(
      final List<FermionToQubitMapping> mappings) {
    if (mappings == null || mappings.isEmpty()) {
      throw new IllegalArgumentException("Chemistry quantum package mappings must not be empty.");
    }
    final ArrayList<FermionToQubitMapping> checked =
        new ArrayList<FermionToQubitMapping>(mappings.size());
    for (int i = 0; i < mappings.size(); ++i) {
      final FermionToQubitMapping mapping = mappings.get(i);
      if (mapping == null) {
        throw new IllegalArgumentException("Chemistry quantum package mapping list contains null.");
      }
      if (!checked.contains(mapping)) {
        checked.add(mapping);
      }
    }
    return List.copyOf(checked);
  }

  private static Set<ChemistryQuantumProgramKind> checkedKinds(
      final Set<ChemistryQuantumProgramKind> programKinds) {
    if (programKinds == null || programKinds.isEmpty()) {
      throw new IllegalArgumentException(
          "Chemistry quantum package program kinds must not be empty.");
    }
    final EnumSet<ChemistryQuantumProgramKind> checked =
        EnumSet.noneOf(ChemistryQuantumProgramKind.class);
    for (final ChemistryQuantumProgramKind kind : programKinds) {
      if (kind == null) {
        throw new IllegalArgumentException(
            "Chemistry quantum package program kind list contains null.");
      }
      checked.add(kind);
    }
    return EnumSet.copyOf(checked);
  }
}
