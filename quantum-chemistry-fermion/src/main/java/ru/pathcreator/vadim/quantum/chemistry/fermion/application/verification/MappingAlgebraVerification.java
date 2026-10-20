/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification;

import java.util.Arrays;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionToQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;

/**
 * Проверяет, что fermion-to-qubit mapping сохраняет каноническую фермионную алгебру.
 */
public final class MappingAlgebraVerification {

  private final FermionToQubitMapper mapper;

  public MappingAlgebraVerification() {
    this(new FermionToQubitMapper());
  }

  public MappingAlgebraVerification(final FermionToQubitMapper mapper) {
    if (mapper == null) {
      throw new IllegalArgumentException("Mapping algebra verifier mapper must not be null.");
    }
    this.mapper = mapper;
  }

  public MappingAlgebraVerificationResult verify(
      final FermionToQubitMapping mapping, final int spinOrbitalCount, final double tolerance) {
    if (mapping == null) {
      throw new IllegalArgumentException("Mapping algebra verifier mapping must not be null.");
    }
    if (spinOrbitalCount <= 0 || spinOrbitalCount > 32) {
      throw new IllegalArgumentException(
          "Mapping algebra verifier spin orbital count must be in [1, 32].");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Mapping algebra verifier tolerance must be finite and non-negative.");
    }
    double maxAnnihilationAnnihilationResidual = 0.0;
    double maxCreationCreationResidual = 0.0;
    double maxMixedResidual = 0.0;
    for (int first = 0; first < spinOrbitalCount; ++first) {
      final QubitPauliOperator annihilationFirst =
          this.ladder(first, false, mapping, spinOrbitalCount);
      final QubitPauliOperator creationFirst = this.ladder(first, true, mapping, spinOrbitalCount);
      for (int second = 0; second < spinOrbitalCount; ++second) {
        final QubitPauliOperator annihilationSecond =
            this.ladder(second, false, mapping, spinOrbitalCount);
        final QubitPauliOperator creationSecond =
            this.ladder(second, true, mapping, spinOrbitalCount);
        maxAnnihilationAnnihilationResidual =
            Math.max(
                maxAnnihilationAnnihilationResidual,
                MappingAlgebraVerification.maxCoefficientMagnitude(
                    MappingAlgebraVerification.anticommutator(
                        annihilationFirst, annihilationSecond)));
        maxCreationCreationResidual =
            Math.max(
                maxCreationCreationResidual,
                MappingAlgebraVerification.maxCoefficientMagnitude(
                    MappingAlgebraVerification.anticommutator(creationFirst, creationSecond)));
        maxMixedResidual =
            Math.max(
                maxMixedResidual,
                MappingAlgebraVerification.maxCoefficientMagnitude(
                    MappingAlgebraVerification.subtractExpectedDelta(
                        MappingAlgebraVerification.anticommutator(
                            annihilationFirst, creationSecond),
                        first == second)));
      }
    }
    final double maxResidual =
        Math.max(
            maxMixedResidual,
            Math.max(maxAnnihilationAnnihilationResidual, maxCreationCreationResidual));
    return MappingAlgebraVerificationResult.of(
        mapping,
        spinOrbitalCount,
        maxAnnihilationAnnihilationResidual,
        maxCreationCreationResidual,
        maxMixedResidual,
        maxResidual <= tolerance);
  }

  private QubitPauliOperator ladder(
      final int spinOrbital,
      final boolean creation,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount) {
    return this.mapper.map(
        FermionOperator.builder()
            .add(
                FermionTerm.of(
                    creation
                        ? FermionTerm.creation(spinOrbital)
                        : FermionTerm.annihilation(spinOrbital)),
                ComplexCoefficient.ONE)
            .build(),
        mapping,
        spinOrbitalCount);
  }

  private static QubitPauliOperator anticommutator(
      final QubitPauliOperator first, final QubitPauliOperator second) {
    return first.multiply(second).add(second.multiply(first));
  }

  private static QubitPauliOperator subtractExpectedDelta(
      final QubitPauliOperator operator, final boolean expectedIdentity) {
    if (!expectedIdentity) {
      return operator;
    }
    return operator.add(QubitPauliOperator.identity(ComplexCoefficient.ONE.negate()));
  }

  private static double maxCoefficientMagnitude(final QubitPauliOperator operator) {
    double max = 0.0;
    for (int i = 0; i < operator.termCount(); ++i) {
      final ComplexCoefficient coefficient = operator.coefficient(i);
      max = Math.max(max, Math.hypot(coefficient.real(), coefficient.imaginary()));
    }
    return max;
  }

  /**
   * Итог проверки антикоммутационной алгебры для одного mapping.
   */
  public static final class MappingAlgebraVerificationResult {

    private final FermionToQubitMapping mapping;
    private final int spinOrbitalCount;
    private final double annihilationAnnihilationResidual;
    private final double creationCreationResidual;
    private final double mixedResidual;
    private final boolean validWithinTolerance;

    private MappingAlgebraVerificationResult(
        final FermionToQubitMapping mapping,
        final int spinOrbitalCount,
        final double annihilationAnnihilationResidual,
        final double creationCreationResidual,
        final double mixedResidual,
        final boolean validWithinTolerance) {
      this.mapping = mapping;
      this.spinOrbitalCount = spinOrbitalCount;
      this.annihilationAnnihilationResidual = annihilationAnnihilationResidual;
      this.creationCreationResidual = creationCreationResidual;
      this.mixedResidual = mixedResidual;
      this.validWithinTolerance = validWithinTolerance;
    }

    private static MappingAlgebraVerificationResult of(
        final FermionToQubitMapping mapping,
        final int spinOrbitalCount,
        final double annihilationAnnihilationResidual,
        final double creationCreationResidual,
        final double mixedResidual,
        final boolean validWithinTolerance) {
      if (mapping == null) {
        throw new IllegalArgumentException(
            "Mapping algebra verification mapping must not be null.");
      }
      final double[] residuals =
          new double[] {annihilationAnnihilationResidual, creationCreationResidual, mixedResidual};
      for (int i = 0; i < residuals.length; ++i) {
        if (!Double.isFinite(residuals[i]) || residuals[i] < 0.0) {
          throw new IllegalArgumentException(
              "Mapping algebra residual must be finite and non-negative.");
        }
      }
      return new MappingAlgebraVerificationResult(
          mapping,
          spinOrbitalCount,
          annihilationAnnihilationResidual,
          creationCreationResidual,
          mixedResidual,
          validWithinTolerance);
    }

    public FermionToQubitMapping mapping() {
      return this.mapping;
    }

    public int spinOrbitalCount() {
      return this.spinOrbitalCount;
    }

    public double annihilationAnnihilationResidual() {
      return this.annihilationAnnihilationResidual;
    }

    public double creationCreationResidual() {
      return this.creationCreationResidual;
    }

    public double mixedResidual() {
      return this.mixedResidual;
    }

    public double maxResidual() {
      return Arrays.stream(
              new double[] {
                this.annihilationAnnihilationResidual,
                this.creationCreationResidual,
                this.mixedResidual
              })
          .max()
          .orElse(0.0);
    }

    public boolean validWithinTolerance() {
      return this.validWithinTolerance;
    }
  }
}
