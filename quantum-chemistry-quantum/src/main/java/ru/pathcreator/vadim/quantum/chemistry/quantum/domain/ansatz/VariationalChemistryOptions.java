/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz;

import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

/**
 * Неизменяемые настройки построения quantum chemistry circuit.
 */
public final class VariationalChemistryOptions {

  private static final int DEFAULT_LAYER_COUNT = 1;
  private static final int DEFAULT_ADAPT_OPERATOR_LIMIT = 8;
  private static final int DEFAULT_PHASE_QUBIT_COUNT = 4;
  private static final int DEFAULT_CONTROLLED_EVOLUTION_REPETITIONS = 1;
  private static final int MAX_PHASE_QUBIT_COUNT = 30;
  private static final int MAX_CONTROLLED_EVOLUTION_OPERATION_REPETITIONS = 1_000_000;

  private final ChemistryAnsatzKind ansatzKind;
  private final String circuitName;
  private final String quantumRegisterName;
  private final String classicalRegisterName;
  private final String parameterPrefix;
  private final int layerCount;
  private final int adaptOperatorLimit;
  private final boolean includeMeasurements;
  private final String phaseRegisterName;
  private final int phaseQubitCount;
  private final int controlledEvolutionRepetitions;
  private final boolean includeInverseQft;
  private final FermionToQubitMapping fermionToQubitMapping;

  private VariationalChemistryOptions(
      final ChemistryAnsatzKind ansatzKind,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final String parameterPrefix,
      final int layerCount,
      final int adaptOperatorLimit,
      final boolean includeMeasurements,
      final String phaseRegisterName,
      final int phaseQubitCount,
      final int controlledEvolutionRepetitions,
      final boolean includeInverseQft,
      final FermionToQubitMapping fermionToQubitMapping) {
    this.ansatzKind = ansatzKind;
    this.circuitName = circuitName;
    this.quantumRegisterName = quantumRegisterName;
    this.classicalRegisterName = classicalRegisterName;
    this.parameterPrefix = parameterPrefix;
    this.layerCount = layerCount;
    this.adaptOperatorLimit = adaptOperatorLimit;
    this.includeMeasurements = includeMeasurements;
    this.phaseRegisterName = phaseRegisterName;
    this.phaseQubitCount = phaseQubitCount;
    this.controlledEvolutionRepetitions = controlledEvolutionRepetitions;
    this.includeInverseQft = includeInverseQft;
    this.fermionToQubitMapping = fermionToQubitMapping;
  }

  public static VariationalChemistryOptions defaults() {
    return VariationalChemistryOptions.of(
        ChemistryAnsatzKind.UCCSD,
        "chemistry_ansatz",
        "q",
        "c",
        "theta",
        DEFAULT_LAYER_COUNT,
        DEFAULT_ADAPT_OPERATOR_LIMIT,
        true);
  }

  public static VariationalChemistryOptions of(
      final ChemistryAnsatzKind ansatzKind,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final String parameterPrefix,
      final int layerCount,
      final int adaptOperatorLimit,
      final boolean includeMeasurements,
      final FermionToQubitMapping fermionToQubitMapping) {
    return VariationalChemistryOptions.of(
        ansatzKind,
        circuitName,
        quantumRegisterName,
        classicalRegisterName,
        parameterPrefix,
        layerCount,
        adaptOperatorLimit,
        includeMeasurements,
        "phase",
        DEFAULT_PHASE_QUBIT_COUNT,
        DEFAULT_CONTROLLED_EVOLUTION_REPETITIONS,
        true,
        fermionToQubitMapping);
  }

  public static VariationalChemistryOptions of(
      final ChemistryAnsatzKind ansatzKind,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final String parameterPrefix,
      final int layerCount,
      final int adaptOperatorLimit,
      final boolean includeMeasurements) {
    return VariationalChemistryOptions.of(
        ansatzKind,
        circuitName,
        quantumRegisterName,
        classicalRegisterName,
        parameterPrefix,
        layerCount,
        adaptOperatorLimit,
        includeMeasurements,
        "phase",
        DEFAULT_PHASE_QUBIT_COUNT,
        DEFAULT_CONTROLLED_EVOLUTION_REPETITIONS,
        true,
        FermionToQubitMapping.JORDAN_WIGNER);
  }

  public static VariationalChemistryOptions of(
      final ChemistryAnsatzKind ansatzKind,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final String parameterPrefix,
      final int layerCount,
      final int adaptOperatorLimit,
      final boolean includeMeasurements,
      final String phaseRegisterName,
      final int phaseQubitCount,
      final int controlledEvolutionRepetitions,
      final boolean includeInverseQft) {
    return VariationalChemistryOptions.of(
        ansatzKind,
        circuitName,
        quantumRegisterName,
        classicalRegisterName,
        parameterPrefix,
        layerCount,
        adaptOperatorLimit,
        includeMeasurements,
        phaseRegisterName,
        phaseQubitCount,
        controlledEvolutionRepetitions,
        includeInverseQft,
        FermionToQubitMapping.JORDAN_WIGNER);
  }

  public static VariationalChemistryOptions of(
      final ChemistryAnsatzKind ansatzKind,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final String parameterPrefix,
      final int layerCount,
      final int adaptOperatorLimit,
      final boolean includeMeasurements,
      final String phaseRegisterName,
      final int phaseQubitCount,
      final int controlledEvolutionRepetitions,
      final boolean includeInverseQft,
      final FermionToQubitMapping fermionToQubitMapping) {
    if (ansatzKind == null) {
      throw new IllegalArgumentException("Chemistry ansatz kind must not be null.");
    }
    if (fermionToQubitMapping == null) {
      throw new IllegalArgumentException("Fermion-to-qubit mapping must not be null.");
    }
    VariationalChemistryOptions.requireIdentifier(circuitName, "Chemistry ansatz circuit name");
    VariationalChemistryOptions.requireIdentifier(
        quantumRegisterName, "Chemistry ansatz quantum register name");
    VariationalChemistryOptions.requireIdentifier(
        classicalRegisterName, "Chemistry ansatz classical register name");
    VariationalChemistryOptions.requireIdentifier(
        parameterPrefix, "Chemistry ansatz parameter prefix");
    VariationalChemistryOptions.requireIdentifier(
        phaseRegisterName, "Chemistry ansatz phase register name");
    if (quantumRegisterName.equals(phaseRegisterName)) {
      throw new IllegalArgumentException(
          "Chemistry ansatz phase register name must differ from system register name.");
    }
    if (layerCount <= 0) {
      throw new IllegalArgumentException("Chemistry ansatz layer count must be positive.");
    }
    if (adaptOperatorLimit <= 0) {
      throw new IllegalArgumentException("ADAPT-VQE operator limit must be positive.");
    }
    if (phaseQubitCount <= 0) {
      throw new IllegalArgumentException("QPE phase qubit count must be positive.");
    }
    if (phaseQubitCount > MAX_PHASE_QUBIT_COUNT) {
      throw new IllegalArgumentException(
          "QPE phase qubit count must not exceed "
              + MAX_PHASE_QUBIT_COUNT
              + " to keep controlled evolution repetition arithmetic safe.");
    }
    if (controlledEvolutionRepetitions <= 0) {
      throw new IllegalArgumentException("QPE controlled evolution repetitions must be positive.");
    }
    VariationalChemistryOptions.requireSafeControlledEvolutionSize(
        phaseQubitCount, controlledEvolutionRepetitions);
    return new VariationalChemistryOptions(
        ansatzKind,
        circuitName,
        quantumRegisterName,
        classicalRegisterName,
        parameterPrefix,
        layerCount,
        adaptOperatorLimit,
        includeMeasurements,
        phaseRegisterName,
        phaseQubitCount,
        controlledEvolutionRepetitions,
        includeInverseQft,
        fermionToQubitMapping);
  }

  public ChemistryAnsatzKind ansatzKind() {
    return this.ansatzKind;
  }

  public String circuitName() {
    return this.circuitName;
  }

  public String quantumRegisterName() {
    return this.quantumRegisterName;
  }

  public String classicalRegisterName() {
    return this.classicalRegisterName;
  }

  public String parameterPrefix() {
    return this.parameterPrefix;
  }

  public int layerCount() {
    return this.layerCount;
  }

  public int adaptOperatorLimit() {
    return this.adaptOperatorLimit;
  }

  public boolean includeMeasurements() {
    return this.includeMeasurements;
  }

  public String phaseRegisterName() {
    return this.phaseRegisterName;
  }

  public int phaseQubitCount() {
    return this.phaseQubitCount;
  }

  public int controlledEvolutionRepetitions() {
    return this.controlledEvolutionRepetitions;
  }

  public boolean includeInverseQft() {
    return this.includeInverseQft;
  }

  public FermionToQubitMapping fermionToQubitMapping() {
    return this.fermionToQubitMapping;
  }

  private static void requireIdentifier(final String value, final String subject) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(subject + " must not be blank.");
    }
    final char first = value.charAt(0);
    if (!Character.isLetter(first) && first != '_') {
      throw new IllegalArgumentException(subject + " must start with a letter or underscore.");
    }
    for (int i = 1; i < value.length(); ++i) {
      final char symbol = value.charAt(i);
      if (!Character.isLetterOrDigit(symbol) && symbol != '_') {
        throw new IllegalArgumentException(
            subject + " must contain only letters, digits or underscore.");
      }
    }
  }

  private static void requireSafeControlledEvolutionSize(
      final int phaseQubitCount, final int controlledEvolutionRepetitions) {
    final long largestRepetitionCount =
        Math.multiplyExact((long) controlledEvolutionRepetitions, 1L << (phaseQubitCount - 1));
    if (largestRepetitionCount > MAX_CONTROLLED_EVOLUTION_OPERATION_REPETITIONS) {
      throw new IllegalArgumentException(
          "QPE controlled evolution repetitions produce "
              + largestRepetitionCount
              + " repetitions on the largest phase qubit; configured safety limit is "
              + MAX_CONTROLLED_EVOLUTION_OPERATION_REPETITIONS
              + ".");
    }
  }
}
