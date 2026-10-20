/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.qpe;

import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

/** Настройки phase estimation для chemistry Hamiltonian. */
public final class ChemistryPhaseEstimationOptions {

  private static final int DEFAULT_PHASE_QUBIT_COUNT = 4;
  private static final int DEFAULT_TROTTER_STEPS = 1;
  private static final double DEFAULT_EVOLUTION_TIME = 1.0;
  private static final int MAX_PHASE_QUBIT_COUNT = 30;
  private static final int MAX_CONTROLLED_EVOLUTION_OPERATION_REPETITIONS = 1_000_000;

  private final String circuitName;
  private final String systemRegisterName;
  private final String phaseRegisterName;
  private final String classicalRegisterName;
  private final FermionToQubitMapping mapping;
  private final int phaseQubitCount;
  private final int trotterSteps;
  private final double evolutionTime;
  private final boolean prepareHartreeFockReference;
  private final boolean includeInverseQft;
  private final boolean includeMeasurements;

  private ChemistryPhaseEstimationOptions(
      final String circuitName,
      final String systemRegisterName,
      final String phaseRegisterName,
      final String classicalRegisterName,
      final FermionToQubitMapping mapping,
      final int phaseQubitCount,
      final int trotterSteps,
      final double evolutionTime,
      final boolean prepareHartreeFockReference,
      final boolean includeInverseQft,
      final boolean includeMeasurements) {
    this.circuitName = circuitName;
    this.systemRegisterName = systemRegisterName;
    this.phaseRegisterName = phaseRegisterName;
    this.classicalRegisterName = classicalRegisterName;
    this.mapping = mapping;
    this.phaseQubitCount = phaseQubitCount;
    this.trotterSteps = trotterSteps;
    this.evolutionTime = evolutionTime;
    this.prepareHartreeFockReference = prepareHartreeFockReference;
    this.includeInverseQft = includeInverseQft;
    this.includeMeasurements = includeMeasurements;
  }

  public static ChemistryPhaseEstimationOptions defaults() {
    return ChemistryPhaseEstimationOptions.of(
        "chemistry_qpe",
        "q",
        "phase",
        "c",
        FermionToQubitMapping.SPIN_BLOCK_PARITY,
        DEFAULT_PHASE_QUBIT_COUNT,
        DEFAULT_TROTTER_STEPS,
        DEFAULT_EVOLUTION_TIME,
        true,
        true,
        true);
  }

  public static ChemistryPhaseEstimationOptions of(
      final String circuitName,
      final String systemRegisterName,
      final String phaseRegisterName,
      final String classicalRegisterName,
      final FermionToQubitMapping mapping,
      final int phaseQubitCount,
      final int trotterSteps,
      final double evolutionTime,
      final boolean prepareHartreeFockReference,
      final boolean includeInverseQft,
      final boolean includeMeasurements) {
    ChemistryPhaseEstimationOptions.requireIdentifier(circuitName, "QPE circuit name");
    ChemistryPhaseEstimationOptions.requireIdentifier(
        systemRegisterName, "QPE system register name");
    ChemistryPhaseEstimationOptions.requireIdentifier(phaseRegisterName, "QPE phase register name");
    ChemistryPhaseEstimationOptions.requireIdentifier(
        classicalRegisterName, "QPE classical register name");
    if (systemRegisterName.equals(phaseRegisterName)
        || systemRegisterName.equals(classicalRegisterName)
        || phaseRegisterName.equals(classicalRegisterName)) {
      throw new IllegalArgumentException("QPE register names must be distinct.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("QPE fermion-to-qubit mapping must not be null.");
    }
    if (phaseQubitCount <= 0 || phaseQubitCount > MAX_PHASE_QUBIT_COUNT) {
      throw new IllegalArgumentException("QPE phase qubit count must be in [1, 30].");
    }
    if (trotterSteps <= 0) {
      throw new IllegalArgumentException("QPE trotter step count must be positive.");
    }
    if (!Double.isFinite(evolutionTime)) {
      throw new IllegalArgumentException("QPE evolution time must be finite.");
    }
    ChemistryPhaseEstimationOptions.requireSafeControlledEvolutionExpansion(
        phaseQubitCount, trotterSteps);
    return new ChemistryPhaseEstimationOptions(
        circuitName,
        systemRegisterName,
        phaseRegisterName,
        classicalRegisterName,
        mapping,
        phaseQubitCount,
        trotterSteps,
        evolutionTime,
        prepareHartreeFockReference,
        includeInverseQft,
        includeMeasurements);
  }

  public String circuitName() {
    return this.circuitName;
  }

  public String systemRegisterName() {
    return this.systemRegisterName;
  }

  public String phaseRegisterName() {
    return this.phaseRegisterName;
  }

  public String classicalRegisterName() {
    return this.classicalRegisterName;
  }

  public FermionToQubitMapping mapping() {
    return this.mapping;
  }

  public int phaseQubitCount() {
    return this.phaseQubitCount;
  }

  public int trotterSteps() {
    return this.trotterSteps;
  }

  public double evolutionTime() {
    return this.evolutionTime;
  }

  public boolean prepareHartreeFockReference() {
    return this.prepareHartreeFockReference;
  }

  public boolean includeInverseQft() {
    return this.includeInverseQft;
  }

  public boolean includeMeasurements() {
    return this.includeMeasurements;
  }

  private static void requireIdentifier(final String value, final String subject) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(subject + " must not be blank.");
    }
    final char first = value.charAt(0);
    if (!Character.isLetter(first) && first != '_') {
      throw new IllegalArgumentException(subject + " must start with a letter or underscore.");
    }
    for (int index = 1; index < value.length(); ++index) {
      final char character = value.charAt(index);
      if (!Character.isLetterOrDigit(character) && character != '_') {
        throw new IllegalArgumentException(
            subject + " must contain only letters, digits, or underscores.");
      }
    }
  }

  private static void requireSafeControlledEvolutionExpansion(
      final int phaseQubitCount, final int trotterSteps) {
    final long largestPhaseRepetition = 1L << (phaseQubitCount - 1);
    final long largestTrotterOperationRepetition;
    try {
      largestTrotterOperationRepetition = Math.multiplyExact(largestPhaseRepetition, trotterSteps);
    } catch (final ArithmeticException exception) {
      throw new IllegalArgumentException(
          "QPE controlled evolution expansion is too large.", exception);
    }
    if (largestTrotterOperationRepetition > MAX_CONTROLLED_EVOLUTION_OPERATION_REPETITIONS) {
      throw new IllegalArgumentException("QPE controlled evolution expansion is too large.");
    }
  }
}
