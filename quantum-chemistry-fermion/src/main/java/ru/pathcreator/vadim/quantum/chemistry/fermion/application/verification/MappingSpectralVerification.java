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
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionOperatorAlgebra;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.matrix.DenseComplexMatrix;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.matrix.FermionOperatorMatrixProjector;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.matrix.QubitPauliOperatorMatrixProjector;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;

/**
 * Проверяет, что fermion-to-qubit mapping сохраняет спектральные инварианты оператора.
 */
public final class MappingSpectralVerification {

  private static final int MAX_DIAGNOSTIC_MOMENT_COUNT = 3;

  private final FermionToQubitMapper mapper;
  private final FermionOperatorAlgebra algebra;
  private final FermionOperatorMatrixProjector fermionProjector;
  private final QubitPauliOperatorMatrixProjector qubitProjector;

  public MappingSpectralVerification() {
    this(
        new FermionToQubitMapper(),
        new FermionOperatorAlgebra(),
        new FermionOperatorMatrixProjector(),
        new QubitPauliOperatorMatrixProjector());
  }

  public MappingSpectralVerification(
      final FermionToQubitMapper mapper,
      final FermionOperatorAlgebra algebra,
      final FermionOperatorMatrixProjector fermionProjector,
      final QubitPauliOperatorMatrixProjector qubitProjector) {
    if (mapper == null) {
      throw new IllegalArgumentException("Mapping verifier mapper must not be null.");
    }
    if (fermionProjector == null) {
      throw new IllegalArgumentException("Mapping verifier fermion projector must not be null.");
    }
    if (algebra == null) {
      throw new IllegalArgumentException("Mapping verifier fermion algebra must not be null.");
    }
    if (qubitProjector == null) {
      throw new IllegalArgumentException("Mapping verifier qubit projector must not be null.");
    }
    this.mapper = mapper;
    this.algebra = algebra;
    this.fermionProjector = fermionProjector;
    this.qubitProjector = qubitProjector;
  }

  public MappingSpectralVerificationResult verify(
      final FermionOperator operator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount,
      final int momentCount,
      final double tolerance) {
    if (operator == null) {
      throw new IllegalArgumentException("Mapping verifier fermion operator must not be null.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("Mapping verifier mapping must not be null.");
    }
    if (spinOrbitalCount <= 0) {
      throw new IllegalArgumentException("Mapping verifier spin orbital count must be positive.");
    }
    if (momentCount <= 0) {
      throw new IllegalArgumentException("Mapping verifier moment count must be positive.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Mapping verifier tolerance must be finite and non-negative.");
    }
    final DenseComplexMatrix fermionMatrix =
        this.fermionProjector.project(operator, spinOrbitalCount);
    final QubitPauliOperator qubitOperator = this.mapper.map(operator, mapping, spinOrbitalCount);
    final DenseComplexMatrix qubitMatrix =
        this.qubitProjector.project(qubitOperator, spinOrbitalCount);
    final int diagnosticMomentCount =
        Math.min(momentCount, MappingSpectralVerification.MAX_DIAGNOSTIC_MOMENT_COUNT);
    final double[] fermionMoments = fermionMatrix.realSpectralMoments(diagnosticMomentCount);
    final double[] qubitMoments = qubitMatrix.realSpectralMoments(diagnosticMomentCount);
    final double fermionHermitianDeviation = fermionMatrix.maxHermitianDeviation();
    final double qubitHermitianDeviation = qubitMatrix.maxHermitianDeviation();
    double maxDifference = 0.0;
    for (int i = 0; i < diagnosticMomentCount; ++i) {
      maxDifference = Math.max(maxDifference, Math.abs(fermionMoments[i] - qubitMoments[i]));
    }
    return MappingSpectralVerificationResult.of(
        mapping,
        spinOrbitalCount,
        momentCount,
        maxDifference,
        maxDifference <= tolerance
            && fermionHermitianDeviation <= tolerance
            && qubitHermitianDeviation <= tolerance,
        fermionHermitianDeviation,
        qubitHermitianDeviation,
        fermionMoments,
        qubitMoments);
  }

  public MappingSpectralVerificationResult verifyExhaustiveSmallSpace(
      final FermionOperator operator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount,
      final double tolerance) {
    if (spinOrbitalCount <= 0 || spinOrbitalCount > 8) {
      throw new IllegalArgumentException(
          "Exhaustive small-space mapping verification supports 1..8 spin orbitals.");
    }
    return this.verify(operator, mapping, spinOrbitalCount, 1 << spinOrbitalCount, tolerance);
  }

  public MappingSpectralVerificationResult verifyStructuralLargeSpace(
      final FermionOperator operator,
      final QubitPauliOperator qubitOperator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount,
      final double tolerance) {
    if (operator == null) {
      throw new IllegalArgumentException(
          "Large-space mapping verifier fermion operator must not be null.");
    }
    if (qubitOperator == null) {
      throw new IllegalArgumentException(
          "Large-space mapping verifier qubit operator must not be null.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("Large-space mapping verifier mapping must not be null.");
    }
    if (spinOrbitalCount <= 8) {
      throw new IllegalArgumentException(
          "Large-space structural verification is intended for more than 8 spin orbitals.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Large-space mapping verifier tolerance must be finite and non-negative.");
    }
    final boolean fermionHermitian = this.algebra.hermitianWithinTolerance(operator, tolerance);
    final boolean qubitHermitian = qubitOperator.hermitianWithinTolerance(tolerance);
    final double fermionHermitianDeviation = fermionHermitian ? 0.0 : Double.POSITIVE_INFINITY;
    final double qubitHermitianDeviation = qubitHermitian ? 0.0 : Double.POSITIVE_INFINITY;
    return MappingSpectralVerificationResult.of(
        mapping,
        spinOrbitalCount,
        0,
        0.0,
        fermionHermitian && qubitHermitian,
        fermionHermitianDeviation,
        qubitHermitianDeviation,
        new double[0],
        new double[0],
        "STRUCTURAL_LARGE_SPACE");
  }

  public static final class MappingSpectralVerificationResult {

    private final FermionToQubitMapping mapping;
    private final int spinOrbitalCount;
    private final int momentCount;
    private final double maxDifference;
    private final boolean equivalentWithinTolerance;
    private final double fermionHermitianDeviation;
    private final double qubitHermitianDeviation;
    private final double[] fermionMoments;
    private final double[] qubitMoments;
    private final String verificationMode;

    private MappingSpectralVerificationResult(
        final FermionToQubitMapping mapping,
        final int spinOrbitalCount,
        final int momentCount,
        final double maxDifference,
        final boolean equivalentWithinTolerance,
        final double fermionHermitianDeviation,
        final double qubitHermitianDeviation,
        final double[] fermionMoments,
        final double[] qubitMoments,
        final String verificationMode) {
      this.mapping = mapping;
      this.spinOrbitalCount = spinOrbitalCount;
      this.momentCount = momentCount;
      this.maxDifference = maxDifference;
      this.equivalentWithinTolerance = equivalentWithinTolerance;
      this.fermionHermitianDeviation = fermionHermitianDeviation;
      this.qubitHermitianDeviation = qubitHermitianDeviation;
      this.fermionMoments = fermionMoments;
      this.qubitMoments = qubitMoments;
      this.verificationMode = verificationMode;
    }

    private static MappingSpectralVerificationResult of(
        final FermionToQubitMapping mapping,
        final int spinOrbitalCount,
        final int momentCount,
        final double maxDifference,
        final boolean equivalentWithinTolerance,
        final double fermionHermitianDeviation,
        final double qubitHermitianDeviation,
        final double[] fermionMoments,
        final double[] qubitMoments) {
      if (mapping == null) {
        throw new IllegalArgumentException(
            "Mapping spectral verification mapping must not be null.");
      }
      if (!Double.isFinite(maxDifference) || maxDifference < 0.0) {
        throw new IllegalArgumentException(
            "Mapping spectral verification difference must be finite.");
      }
      if (!Double.isFinite(fermionHermitianDeviation) || fermionHermitianDeviation < 0.0) {
        throw new IllegalArgumentException("Fermion Hermitian deviation must be finite.");
      }
      if (!Double.isFinite(qubitHermitianDeviation) || qubitHermitianDeviation < 0.0) {
        throw new IllegalArgumentException("Qubit Hermitian deviation must be finite.");
      }
      return MappingSpectralVerificationResult.of(
          mapping,
          spinOrbitalCount,
          momentCount,
          maxDifference,
          equivalentWithinTolerance,
          fermionHermitianDeviation,
          qubitHermitianDeviation,
          fermionMoments,
          qubitMoments,
          "EXHAUSTIVE_SPECTRAL_MOMENTS");
    }

    private static MappingSpectralVerificationResult of(
        final FermionToQubitMapping mapping,
        final int spinOrbitalCount,
        final int momentCount,
        final double maxDifference,
        final boolean equivalentWithinTolerance,
        final double fermionHermitianDeviation,
        final double qubitHermitianDeviation,
        final double[] fermionMoments,
        final double[] qubitMoments,
        final String verificationMode) {
      if (mapping == null) {
        throw new IllegalArgumentException(
            "Mapping spectral verification mapping must not be null.");
      }
      if (!Double.isFinite(maxDifference) || maxDifference < 0.0) {
        throw new IllegalArgumentException(
            "Mapping spectral verification difference must be finite.");
      }
      if ((!Double.isFinite(fermionHermitianDeviation)
              && !Double.isInfinite(fermionHermitianDeviation))
          || fermionHermitianDeviation < 0.0) {
        throw new IllegalArgumentException(
            "Fermion Hermitian deviation must be finite or infinite.");
      }
      if ((!Double.isFinite(qubitHermitianDeviation) && !Double.isInfinite(qubitHermitianDeviation))
          || qubitHermitianDeviation < 0.0) {
        throw new IllegalArgumentException("Qubit Hermitian deviation must be finite or infinite.");
      }
      if (verificationMode == null || verificationMode.isBlank()) {
        throw new IllegalArgumentException("Mapping verification mode must not be blank.");
      }
      return new MappingSpectralVerificationResult(
          mapping,
          spinOrbitalCount,
          momentCount,
          maxDifference,
          equivalentWithinTolerance,
          fermionHermitianDeviation,
          qubitHermitianDeviation,
          Arrays.copyOf(fermionMoments, fermionMoments.length),
          Arrays.copyOf(qubitMoments, qubitMoments.length),
          verificationMode);
    }

    public FermionToQubitMapping mapping() {
      return this.mapping;
    }

    public int spinOrbitalCount() {
      return this.spinOrbitalCount;
    }

    public int momentCount() {
      return this.momentCount;
    }

    public double maxDifference() {
      return this.maxDifference;
    }

    public boolean equivalentWithinTolerance() {
      return this.equivalentWithinTolerance;
    }

    public double fermionHermitianDeviation() {
      return this.fermionHermitianDeviation;
    }

    public double qubitHermitianDeviation() {
      return this.qubitHermitianDeviation;
    }

    public double[] fermionMoments() {
      return Arrays.copyOf(this.fermionMoments, this.fermionMoments.length);
    }

    public double[] qubitMoments() {
      return Arrays.copyOf(this.qubitMoments, this.qubitMoments.length);
    }

    public String verificationMode() {
      return this.verificationMode;
    }
  }
}
