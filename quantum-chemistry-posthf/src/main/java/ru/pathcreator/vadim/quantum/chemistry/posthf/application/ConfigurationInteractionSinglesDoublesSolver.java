/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import java.util.Arrays;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianFermionMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant.DeterminantSpaceGenerator;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant.DeterminantSpaceHamiltonianProjector;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.ci.CisdResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.JacobiSymmetricEigenSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;

public final class ConfigurationInteractionSinglesDoublesSolver {

  private final ElectronicHamiltonianFermionMapper fermionMapper;
  private final DeterminantSpaceHamiltonianProjector hamiltonianProjector;
  private final JacobiSymmetricEigenSolver eigenSolver;

  public ConfigurationInteractionSinglesDoublesSolver() {
    this(
        new ElectronicHamiltonianFermionMapper(),
        new DeterminantSpaceHamiltonianProjector(),
        new JacobiSymmetricEigenSolver());
  }

  public ConfigurationInteractionSinglesDoublesSolver(
      final ElectronicHamiltonianFermionMapper fermionMapper,
      final DeterminantSpaceHamiltonianProjector hamiltonianProjector,
      final JacobiSymmetricEigenSolver eigenSolver) {
    if (fermionMapper == null) {
      throw new IllegalArgumentException("CISD fermion mapper must not be null.");
    }
    if (hamiltonianProjector == null) {
      throw new IllegalArgumentException("CISD Hamiltonian projector must not be null.");
    }
    if (eigenSolver == null) {
      throw new IllegalArgumentException("CISD eigen solver must not be null.");
    }
    this.fermionMapper = fermionMapper;
    this.hamiltonianProjector = hamiltonianProjector;
    this.eigenSolver = eigenSolver;
  }

  public CisdResult solve(final ElectronicHamiltonian hamiltonian) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("CISD Hamiltonian must not be null.");
    }
    final long[] determinants =
        DeterminantSpaceGenerator.cisd(hamiltonian.spinOrbitalCount(), hamiltonian.electronCount());
    final FermionOperator operator = this.fermionMapper.map(hamiltonian);
    final double[] matrix = this.hamiltonianProjector.projectSymmetric(operator, determinants);
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(determinants.length, matrix));
    final double totalEnergy = decomposition.eigenvalue(0);
    return CisdResult.of(
        determinants.length, totalEnergy - hamiltonian.nuclearRepulsionEnergy(), totalEnergy);
  }

  private static long[] generateCisdDeterminants(
      final int spinOrbitalCount, final int electronCount) {
    if (spinOrbitalCount <= 0 || spinOrbitalCount > Long.SIZE - 1) {
      throw new IllegalArgumentException("CISD spin orbital count must be in [1, 63].");
    }
    if (electronCount <= 0 || electronCount > spinOrbitalCount) {
      throw new IllegalArgumentException("CISD electron count is outside spin orbital bounds.");
    }
    final long reference =
        ConfigurationInteractionSinglesDoublesSolver.referenceDeterminant(electronCount);
    final LongArrayBuilder determinants = new LongArrayBuilder();
    determinants.add(reference);
    for (int occupied = 0; occupied < electronCount; ++occupied) {
      for (int virtual = electronCount; virtual < spinOrbitalCount; ++virtual) {
        determinants.add((reference & ~(1L << occupied)) | (1L << virtual));
      }
    }
    for (int firstOccupied = 0; firstOccupied < electronCount; ++firstOccupied) {
      for (int secondOccupied = firstOccupied + 1;
          secondOccupied < electronCount;
          ++secondOccupied) {
        for (int firstVirtual = electronCount; firstVirtual < spinOrbitalCount; ++firstVirtual) {
          for (int secondVirtual = firstVirtual + 1;
              secondVirtual < spinOrbitalCount;
              ++secondVirtual) {
            determinants.add(
                (reference & ~(1L << firstOccupied) & ~(1L << secondOccupied))
                    | (1L << firstVirtual)
                    | (1L << secondVirtual));
          }
        }
      }
    }
    final long[] unique = determinants.sortedUniqueArray();
    if (unique.length == 0) {
      throw new IllegalArgumentException("CISD determinant space is empty.");
    }
    return unique;
  }

  private static long referenceDeterminant(final int electronCount) {
    long determinant = 0L;
    for (int orbital = 0; orbital < electronCount; ++orbital) {
      determinant |= 1L << orbital;
    }
    return determinant;
  }

  private static double[] buildHamiltonianMatrix(
      final FermionOperator operator,
      final long[] determinants,
      final LongDeterminantIndex determinantIndex) {
    final int size = determinants.length;
    final double[] matrix = new double[size * size];
    for (int column = 0; column < size; ++column) {
      final long ket = determinants[column];
      for (int termIndex = 0; termIndex < operator.termCount(); ++termIndex) {
        final FermionTerm term = operator.term(termIndex);
        final ComplexCoefficient coefficient = operator.coefficient(termIndex);
        if (!coefficient.effectivelyReal()) {
          throw new IllegalArgumentException(
              "CISD solver requires a real Hermitian fermion Hamiltonian.");
        }
        final AppliedTerm applied = ConfigurationInteractionSinglesDoublesSolver.apply(term, ket);
        if (!applied.nonZero()) {
          continue;
        }
        final int row = determinantIndex.indexOf(applied.determinant());
        if (row < 0) {
          continue;
        }
        matrix[row * size + column] += applied.phase() * coefficient.real();
      }
    }
    return matrix;
  }

  private static AppliedTerm apply(final FermionTerm term, final long determinant) {
    long state = determinant;
    double phase = 1.0;
    for (int i = term.length() - 1; i >= 0; --i) {
      final long encodedOperator = term.operator(i);
      final int orbital = FermionTerm.spinOrbital(encodedOperator);
      final long mask = 1L << orbital;
      final boolean occupied = (state & mask) != 0L;
      if (FermionTerm.action(encodedOperator) == FermionLadderAction.CREATION) {
        if (occupied) {
          return AppliedTerm.zero();
        }
        phase *= ConfigurationInteractionSinglesDoublesSolver.fermionPhase(state, orbital);
        state |= mask;
      } else {
        if (!occupied) {
          return AppliedTerm.zero();
        }
        phase *= ConfigurationInteractionSinglesDoublesSolver.fermionPhase(state, orbital);
        state &= ~mask;
      }
    }
    return AppliedTerm.of(state, phase);
  }

  private static double fermionPhase(final long determinant, final int orbital) {
    final long lowerMask = (1L << orbital) - 1L;
    return (Long.bitCount(determinant & lowerMask) & 1) == 0 ? 1.0 : -1.0;
  }

  private static final class AppliedTerm {

    private static final AppliedTerm ZERO = new AppliedTerm(0L, 0.0, false);
    private final long determinant;
    private final double phase;
    private final boolean nonZero;

    private AppliedTerm(final long determinant, final double phase, final boolean nonZero) {
      this.determinant = determinant;
      this.phase = phase;
      this.nonZero = nonZero;
    }

    private static AppliedTerm zero() {
      return ZERO;
    }

    private static AppliedTerm of(final long determinant, final double phase) {
      return new AppliedTerm(determinant, phase, true);
    }

    private long determinant() {
      return this.determinant;
    }

    private double phase() {
      return this.phase;
    }

    private boolean nonZero() {
      return this.nonZero;
    }
  }

  private static final class LongArrayBuilder {

    private long[] values = new long[32];
    private int size;

    private void add(final long value) {
      if (this.size == this.values.length) {
        this.values = Arrays.copyOf(this.values, this.values.length << 1);
      }
      this.values[this.size] = value;
      ++this.size;
    }

    private long[] sortedUniqueArray() {
      final long[] copy = Arrays.copyOf(this.values, this.size);
      Arrays.sort(copy);
      int uniqueCount = 0;
      for (int i = 0; i < copy.length; ++i) {
        if (i > 0 && copy[i] == copy[i - 1]) {
          continue;
        }
        copy[uniqueCount] = copy[i];
        ++uniqueCount;
      }
      return Arrays.copyOf(copy, uniqueCount);
    }
  }

  private static final class LongDeterminantIndex {

    private static final float LOAD_FACTOR = 0.65F;
    private final long[] keys;
    private final int[] values;
    private final boolean[] used;
    private final int mask;

    private LongDeterminantIndex(
        final long[] keys, final int[] values, final boolean[] used, final int mask) {
      this.keys = keys;
      this.values = values;
      this.used = used;
      this.mask = mask;
    }

    private static LongDeterminantIndex of(final long[] determinants) {
      int capacity = 1;
      while ((int) ((float) capacity * LOAD_FACTOR) < determinants.length) {
        capacity <<= 1;
      }
      final LongDeterminantIndex index =
          new LongDeterminantIndex(
              new long[capacity], new int[capacity], new boolean[capacity], capacity - 1);
      for (int i = 0; i < determinants.length; ++i) {
        index.put(determinants[i], i);
      }
      return index;
    }

    private void put(final long key, final int value) {
      int slot = this.slot(key);
      while (this.used[slot]) {
        if (this.keys[slot] == key) {
          this.values[slot] = value;
          return;
        }
        slot = (slot + 1) & this.mask;
      }
      this.used[slot] = true;
      this.keys[slot] = key;
      this.values[slot] = value;
    }

    private int indexOf(final long key) {
      int slot = this.slot(key);
      while (this.used[slot]) {
        if (this.keys[slot] == key) {
          return this.values[slot];
        }
        slot = (slot + 1) & this.mask;
      }
      return -1;
    }

    private int slot(final long key) {
      long mixed = key;
      mixed ^= mixed >>> 33;
      mixed *= 0xff51afd7ed558ccdL;
      mixed ^= mixed >>> 33;
      mixed *= 0xc4ceb9fe1a85ec53L;
      mixed ^= mixed >>> 33;
      return (int) mixed & this.mask;
    }
  }
}
