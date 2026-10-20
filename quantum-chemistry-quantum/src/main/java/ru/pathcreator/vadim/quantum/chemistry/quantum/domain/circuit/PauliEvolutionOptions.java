/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.circuit;

/** Immutable настройки построения Pauli evolution circuit. */
public final class PauliEvolutionOptions {

  private static final int DEFAULT_SHOTS = 1024;

  private final String circuitName;
  private final String quantumRegisterName;
  private final String classicalRegisterName;
  private final int trotterSteps;
  private final boolean includeMeasurements;
  private final int shots;

  private PauliEvolutionOptions(
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final int trotterSteps,
      final boolean includeMeasurements,
      final int shots) {
    this.circuitName = circuitName;
    this.quantumRegisterName = quantumRegisterName;
    this.classicalRegisterName = classicalRegisterName;
    this.trotterSteps = trotterSteps;
    this.includeMeasurements = includeMeasurements;
    this.shots = shots;
  }

  public static PauliEvolutionOptions defaults() {
    return PauliEvolutionOptions.of("chemistry", "q", "c", 1, true, DEFAULT_SHOTS);
  }

  public static PauliEvolutionOptions of(
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final int trotterSteps,
      final boolean includeMeasurements,
      final int shots) {
    if (circuitName == null || circuitName.isBlank()) {
      throw new IllegalArgumentException("Pauli evolution circuit name must not be blank.");
    }
    if (quantumRegisterName == null || quantumRegisterName.isBlank()) {
      throw new IllegalArgumentException(
          "Pauli evolution quantum register name must not be blank.");
    }
    if (classicalRegisterName == null || classicalRegisterName.isBlank()) {
      throw new IllegalArgumentException(
          "Pauli evolution classical register name must not be blank.");
    }
    if (trotterSteps <= 0) {
      throw new IllegalArgumentException("Pauli evolution trotter step count must be positive.");
    }
    if (shots <= 0) {
      throw new IllegalArgumentException("Pauli evolution shot count must be positive.");
    }
    return new PauliEvolutionOptions(
        circuitName,
        quantumRegisterName,
        classicalRegisterName,
        trotterSteps,
        includeMeasurements,
        shots);
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

  public int trotterSteps() {
    return this.trotterSteps;
  }

  public boolean includeMeasurements() {
    return this.includeMeasurements;
  }

  public int shots() {
    return this.shots;
  }
}
