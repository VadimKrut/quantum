/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;

/**
 * Выполняет fermion-to-qubit mapping без внешнего OpenFermion runtime.
 */
public final class FermionToQubitMapper {

  private static final ComplexCoefficient HALF = ComplexCoefficient.real(0.5);
  private static final ComplexCoefficient POSITIVE_HALF_I = ComplexCoefficient.of(0.0, 0.5);
  private static final ComplexCoefficient NEGATIVE_HALF_I = ComplexCoefficient.of(0.0, -0.5);

  public QubitPauliOperator map(
      final FermionOperator operator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount) {
    if (operator == null) {
      throw new IllegalArgumentException("Fermion operator must not be null.");
    }
    if (mapping == null) {
      throw new IllegalArgumentException("Fermion-to-qubit mapping must not be null.");
    }
    if (spinOrbitalCount <= 0) {
      throw new IllegalArgumentException("Spin orbital count must be positive.");
    }
    return this.mapOperator(operator, mapping, spinOrbitalCount);
  }

  private QubitPauliOperator mapOperator(
      final FermionOperator operator,
      final FermionToQubitMapping mapping,
      final int spinOrbitalCount) {
    final QubitPauliOperator.Builder builder = QubitPauliOperator.builder();
    for (int i = 0; i < operator.termCount(); ++i) {
      QubitPauliOperator mappedTerm = QubitPauliOperator.identity(operator.coefficient(i));
      final FermionTerm term = operator.term(i);
      for (int j = 0; j < term.length(); ++j) {
        mappedTerm =
            mappedTerm.multiply(this.ladderOperator(term.operator(j), mapping, spinOrbitalCount));
      }
      builder.add(mappedTerm);
    }
    return builder.build();
  }

  private QubitPauliOperator ladderOperator(
      final long encodedOperator, final FermionToQubitMapping mapping, final int spinOrbitalCount) {
    final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
    if (spinOrbital >= spinOrbitalCount) {
      throw new IllegalArgumentException(
          "Fermion term references spin orbital outside active space.");
    }
    if (mapping == FermionToQubitMapping.SPIN_BLOCK_JORDAN_WIGNER) {
      return this.jordanWignerLadderOperator(
          this.spinBlockEncodedOperator(encodedOperator, spinOrbitalCount));
    }
    if (mapping == FermionToQubitMapping.SPIN_BLOCK_PARITY) {
      return this.parityLadderOperator(
          this.spinBlockEncodedOperator(encodedOperator, spinOrbitalCount), spinOrbitalCount);
    }
    if (mapping == FermionToQubitMapping.SPIN_BLOCK_BRAVYI_KITAEV) {
      return this.bravyiKitaevLadderOperator(
          this.spinBlockEncodedOperator(encodedOperator, spinOrbitalCount), spinOrbitalCount);
    }
    if (mapping == FermionToQubitMapping.REVERSED_JORDAN_WIGNER) {
      return this.reversedJordanWignerLadderOperator(encodedOperator, spinOrbitalCount);
    }
    if (mapping == FermionToQubitMapping.PARITY) {
      return this.parityLadderOperator(encodedOperator, spinOrbitalCount);
    }
    if (mapping == FermionToQubitMapping.BRAVYI_KITAEV) {
      return this.bravyiKitaevLadderOperator(encodedOperator, spinOrbitalCount);
    }
    return this.jordanWignerLadderOperator(encodedOperator);
  }

  private long spinBlockEncodedOperator(final long encodedOperator, final int spinOrbitalCount) {
    if ((spinOrbitalCount & 1) != 0) {
      throw new IllegalArgumentException(
          "Spin-block fermion mapping requires an even spin orbital count.");
    }
    final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
    final int spatialOrbitalCount = spinOrbitalCount / 2;
    final int spatialOrbital = spinOrbital / 2;
    final int spinProjection = spinOrbital & 1;
    final int remappedSpinOrbital =
        spinProjection == 0 ? spatialOrbital : spatialOrbitalCount + spatialOrbital;
    return FermionTerm.encode(remappedSpinOrbital, FermionTerm.action(encodedOperator));
  }

  private QubitPauliOperator jordanWignerLadderOperator(final long encodedOperator) {
    final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
    final FermionLadderAction action = FermionTerm.action(encodedOperator);
    final QubitPauliTerm zPrefix = this.zPrefix(spinOrbital);
    final QubitPauliOperator xPart =
        QubitPauliOperator.builder()
            .add(zPrefix.multiply(QubitPauliTerm.of(spinOrbital, PauliAxis.X)).term(), HALF)
            .build();
    final ComplexCoefficient yCoefficient =
        action == FermionLadderAction.CREATION ? NEGATIVE_HALF_I : POSITIVE_HALF_I;
    final QubitPauliOperator yPart =
        QubitPauliOperator.builder()
            .add(zPrefix.multiply(QubitPauliTerm.of(spinOrbital, PauliAxis.Y)).term(), yCoefficient)
            .build();
    return xPart.add(yPart);
  }

  private QubitPauliOperator reversedJordanWignerLadderOperator(
      final long encodedOperator, final int spinOrbitalCount) {
    final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
    final int qubit = spinOrbitalCount - 1 - spinOrbital;
    final FermionLadderAction action = FermionTerm.action(encodedOperator);
    final QubitPauliTerm zPrefix = this.reversedZPrefix(qubit, spinOrbitalCount);
    final QubitPauliOperator xPart =
        QubitPauliOperator.builder()
            .add(zPrefix.multiply(QubitPauliTerm.of(qubit, PauliAxis.X)).term(), HALF)
            .build();
    final ComplexCoefficient yCoefficient =
        action == FermionLadderAction.CREATION ? NEGATIVE_HALF_I : POSITIVE_HALF_I;
    final QubitPauliOperator yPart =
        QubitPauliOperator.builder()
            .add(zPrefix.multiply(QubitPauliTerm.of(qubit, PauliAxis.Y)).term(), yCoefficient)
            .build();
    return xPart.add(yPart);
  }

  private QubitPauliOperator parityLadderOperator(
      final long encodedOperator, final int spinOrbitalCount) {
    final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
    final FermionLadderAction action = FermionTerm.action(encodedOperator);
    final QubitPauliTerm xSuffix = this.xSuffix(spinOrbital + 1, spinOrbitalCount);
    final QubitPauliTerm xParityTerm =
        xSuffix
            .multiply(this.previousParityZ(spinOrbital))
            .term()
            .multiply(QubitPauliTerm.of(spinOrbital, PauliAxis.X))
            .term();
    final QubitPauliTerm yParityTerm =
        xSuffix.multiply(QubitPauliTerm.of(spinOrbital, PauliAxis.Y)).term();
    final ComplexCoefficient yCoefficient =
        action == FermionLadderAction.CREATION ? NEGATIVE_HALF_I : POSITIVE_HALF_I;
    return QubitPauliOperator.builder()
        .add(xParityTerm, HALF)
        .add(yParityTerm, yCoefficient)
        .build();
  }

  private QubitPauliOperator bravyiKitaevLadderOperator(
      final long encodedOperator, final int spinOrbitalCount) {
    final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
    final FermionLadderAction action = FermionTerm.action(encodedOperator);
    final boolean[] updateSet = this.bravyiKitaevUpdateSet(spinOrbital, spinOrbitalCount);
    final boolean[] paritySet = this.bravyiKitaevParitySet(spinOrbital, spinOrbitalCount);
    final boolean[] secondTermZSet =
        (spinOrbital & 1) == 0
            ? paritySet
            : this.bravyiKitaevRemainderSet(spinOrbital, spinOrbitalCount, paritySet);
    final QubitPauliOperator.Builder builder = QubitPauliOperator.builder();
    this.addBravyiKitaevTerm(builder, HALF, updateSet, spinOrbital, PauliAxis.X, paritySet);
    final ComplexCoefficient yCoefficient =
        action == FermionLadderAction.CREATION ? NEGATIVE_HALF_I : POSITIVE_HALF_I;
    this.addBravyiKitaevTerm(
        builder, yCoefficient, updateSet, spinOrbital, PauliAxis.Y, secondTermZSet);
    return builder.build();
  }

  private QubitPauliTerm zPrefix(final int upperExclusive) {
    if (upperExclusive == 0) {
      return QubitPauliTerm.IDENTITY;
    }
    final int[] qubits = new int[upperExclusive];
    final PauliAxis[] axes = new PauliAxis[upperExclusive];
    for (int i = 0; i < upperExclusive; ++i) {
      qubits[i] = i;
      axes[i] = PauliAxis.Z;
    }
    return QubitPauliTerm.of(qubits, axes);
  }

  private QubitPauliTerm reversedZPrefix(final int lowerExclusive, final int spinOrbitalCount) {
    if (lowerExclusive + 1 >= spinOrbitalCount) {
      return QubitPauliTerm.IDENTITY;
    }
    final int length = spinOrbitalCount - lowerExclusive - 1;
    final int[] qubits = new int[length];
    final PauliAxis[] axes = new PauliAxis[length];
    for (int i = 0; i < length; ++i) {
      qubits[i] = lowerExclusive + 1 + i;
      axes[i] = PauliAxis.Z;
    }
    return QubitPauliTerm.of(qubits, axes);
  }

  private QubitPauliTerm xSuffix(final int lowerInclusive, final int spinOrbitalCount) {
    if (lowerInclusive >= spinOrbitalCount) {
      return QubitPauliTerm.IDENTITY;
    }
    final int length = spinOrbitalCount - lowerInclusive;
    final int[] qubits = new int[length];
    final PauliAxis[] axes = new PauliAxis[length];
    for (int i = 0; i < length; ++i) {
      qubits[i] = lowerInclusive + i;
      axes[i] = PauliAxis.X;
    }
    return QubitPauliTerm.of(qubits, axes);
  }

  private QubitPauliTerm previousParityZ(final int spinOrbital) {
    if (spinOrbital == 0) {
      return QubitPauliTerm.IDENTITY;
    }
    return QubitPauliTerm.of(spinOrbital - 1, PauliAxis.Z);
  }

  private boolean[] bravyiKitaevUpdateSet(final int spinOrbital, final int spinOrbitalCount) {
    final boolean[] result = new boolean[spinOrbitalCount];
    int index = spinOrbital + 1;
    while (index <= spinOrbitalCount) {
      if (index - 1 != spinOrbital) {
        result[index - 1] = true;
      }
      index += Integer.lowestOneBit(index);
    }
    return result;
  }

  private boolean[] bravyiKitaevParitySet(final int spinOrbital, final int spinOrbitalCount) {
    final boolean[] result = new boolean[spinOrbitalCount];
    int index = spinOrbital;
    while (index > 0) {
      result[index - 1] = !result[index - 1];
      index -= Integer.lowestOneBit(index);
    }
    return result;
  }

  private boolean[] bravyiKitaevRemainderSet(
      final int spinOrbital, final int spinOrbitalCount, final boolean[] paritySet) {
    final boolean[] flipSet = this.bravyiKitaevFlipSet(spinOrbital, spinOrbitalCount);
    final boolean[] result = new boolean[spinOrbitalCount];
    for (int i = 0; i < spinOrbitalCount; ++i) {
      result[i] = paritySet[i] && !flipSet[i];
    }
    return result;
  }

  private boolean[] bravyiKitaevFlipSet(final int spinOrbital, final int spinOrbitalCount) {
    final int oneBased = spinOrbital + 1;
    final int rangeStart = oneBased - Integer.lowestOneBit(oneBased);
    final boolean[] upperPrefix = this.bravyiKitaevParitySet(spinOrbital, spinOrbitalCount);
    final boolean[] lowerPrefix = this.bravyiKitaevParitySet(rangeStart, spinOrbitalCount);
    final boolean[] result = new boolean[spinOrbitalCount];
    for (int i = 0; i < spinOrbitalCount; ++i) {
      result[i] = upperPrefix[i] != lowerPrefix[i];
    }
    return result;
  }

  private void addBravyiKitaevTerm(
      final QubitPauliOperator.Builder builder,
      final ComplexCoefficient coefficient,
      final boolean[] updateSet,
      final int localQubit,
      final PauliAxis localAxis,
      final boolean[] zSet) {
    QubitPauliTerm term = QubitPauliTerm.IDENTITY;
    ComplexCoefficient phase = coefficient;
    for (int i = 0; i < updateSet.length; ++i) {
      if (updateSet[i]) {
        final QubitPauliTerm.Product product = term.multiply(QubitPauliTerm.of(i, PauliAxis.X));
        term = product.term();
        phase = phase.multiply(product.phase());
      }
    }
    final QubitPauliTerm.Product localProduct =
        term.multiply(QubitPauliTerm.of(localQubit, localAxis));
    term = localProduct.term();
    phase = phase.multiply(localProduct.phase());
    for (int i = 0; i < zSet.length; ++i) {
      if (zSet[i]) {
        final QubitPauliTerm.Product product = term.multiply(QubitPauliTerm.of(i, PauliAxis.Z));
        term = product.term();
        phase = phase.multiply(product.phase());
      }
    }
    builder.add(term, phase);
  }
}
