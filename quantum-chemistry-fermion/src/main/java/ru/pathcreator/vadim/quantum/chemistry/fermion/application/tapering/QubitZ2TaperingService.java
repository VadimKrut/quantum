/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.tapering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering.FixedZ2Symmetry;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering.QubitTaperingResult;

/** Выполняет безопасное tapering Pauli operator по явно заданным Z2-симметриям. */
public final class QubitZ2TaperingService {

  public QubitTaperingResult taperByFixedZ(
      final QubitPauliOperator operator,
      final int originalQubitCount,
      final List<FixedZ2Symmetry> fixedSymmetries) {
    if (operator == null) {
      throw new IllegalArgumentException("Qubit operator for tapering must not be null.");
    }
    if (originalQubitCount < operator.requiredQubitCount()) {
      throw new IllegalArgumentException("Original qubit count is smaller than operator support.");
    }
    final List<FixedZ2Symmetry> symmetries =
        QubitZ2TaperingService.sortedSymmetries(fixedSymmetries);
    QubitZ2TaperingService.requireSymmetriesInsideOriginalSpace(symmetries, originalQubitCount);
    final QubitPauliOperator.Builder builder = QubitPauliOperator.builder();
    for (int i = 0; i < operator.termCount(); ++i) {
      this.addTaperedTerm(
          builder, operator.term(i), operator.coefficient(i), originalQubitCount, symmetries);
    }
    final QubitPauliOperator taperedOperator = builder.build();
    return QubitTaperingResult.of(
        taperedOperator, symmetries, originalQubitCount, originalQubitCount - symmetries.size());
  }

  public QubitTaperingResult taperSpinBlockParity(
      final QubitPauliOperator operator,
      final int spinOrbitalCount,
      final int alphaElectronCount,
      final int betaElectronCount) {
    if ((spinOrbitalCount & 1) != 0) {
      throw new IllegalArgumentException(
          "Spin-block parity tapering requires an even spin orbital count.");
    }
    if (alphaElectronCount < 0 || betaElectronCount < 0) {
      throw new IllegalArgumentException("Spin electron counts must not be negative.");
    }
    final int spatialOrbitalCount = spinOrbitalCount / 2;
    return this.taperByFixedZ(
        operator,
        spinOrbitalCount,
        List.of(
            FixedZ2Symmetry.of(
                spatialOrbitalCount - 1,
                QubitZ2TaperingService.parityEigenvalue(alphaElectronCount)),
            FixedZ2Symmetry.of(
                spinOrbitalCount - 1,
                QubitZ2TaperingService.parityEigenvalue(alphaElectronCount + betaElectronCount))));
  }

  private void addTaperedTerm(
      final QubitPauliOperator.Builder builder,
      final QubitPauliTerm term,
      final ComplexCoefficient coefficient,
      final int originalQubitCount,
      final List<FixedZ2Symmetry> fixedSymmetries) {
    final int[] qubits = new int[term.length()];
    final PauliAxis[] axes = new PauliAxis[term.length()];
    ComplexCoefficient taperedCoefficient = coefficient;
    int length = 0;
    for (int i = 0; i < term.length(); ++i) {
      final int qubit = term.qubit(i);
      if (qubit >= originalQubitCount) {
        throw new IllegalArgumentException(
            "Pauli term references qubit outside original tapering space.");
      }
      final int fixedSymmetryIndex =
          QubitZ2TaperingService.fixedSymmetryIndex(fixedSymmetries, qubit);
      if (fixedSymmetryIndex < 0) {
        qubits[length] = QubitZ2TaperingService.shiftedQubit(qubit, fixedSymmetries);
        axes[length] = term.axis(i);
        ++length;
        continue;
      }
      final FixedZ2Symmetry fixedSymmetry = fixedSymmetries.get(fixedSymmetryIndex);
      if (term.axis(i) != PauliAxis.Z) {
        throw new IllegalArgumentException(
            "Cannot taper qubit "
                + qubit
                + " because Pauli term contains "
                + term.axis(i)
                + " instead of Z/I on fixed symmetry qubit.");
      }
      taperedCoefficient = taperedCoefficient.multiply((double) fixedSymmetry.zEigenvalue());
    }
    builder.add(
        QubitPauliTerm.of(Arrays.copyOf(qubits, length), Arrays.copyOf(axes, length)),
        taperedCoefficient);
  }

  private static List<FixedZ2Symmetry> sortedSymmetries(
      final List<FixedZ2Symmetry> fixedSymmetries) {
    if (fixedSymmetries == null) {
      throw new IllegalArgumentException("Fixed Z2 symmetry list must not be null.");
    }
    final ArrayList<FixedZ2Symmetry> result = new ArrayList<FixedZ2Symmetry>(fixedSymmetries);
    for (int i = 0; i < result.size(); ++i) {
      if (result.get(i) == null) {
        throw new IllegalArgumentException("Fixed Z2 symmetry list must not contain null.");
      }
    }
    result.sort(Comparator.comparingInt(FixedZ2Symmetry::qubit));
    int previousQubit = -1;
    for (int i = 0; i < result.size(); ++i) {
      final FixedZ2Symmetry symmetry = result.get(i);
      if (symmetry.qubit() == previousQubit) {
        throw new IllegalArgumentException("Fixed Z2 symmetry qubits must be unique.");
      }
      previousQubit = symmetry.qubit();
    }
    return List.copyOf(result);
  }

  private static void requireSymmetriesInsideOriginalSpace(
      final List<FixedZ2Symmetry> fixedSymmetries, final int originalQubitCount) {
    for (int i = 0; i < fixedSymmetries.size(); ++i) {
      final FixedZ2Symmetry symmetry = fixedSymmetries.get(i);
      if (symmetry.qubit() >= originalQubitCount) {
        throw new IllegalArgumentException(
            "Fixed Z2 symmetry qubit is outside original tapering space.");
      }
    }
  }

  private static int fixedSymmetryIndex(
      final List<FixedZ2Symmetry> fixedSymmetries, final int qubit) {
    for (int i = 0; i < fixedSymmetries.size(); ++i) {
      final FixedZ2Symmetry symmetry = fixedSymmetries.get(i);
      if (symmetry.qubit() == qubit) {
        return i;
      }
    }
    return -1;
  }

  private static int shiftedQubit(final int qubit, final List<FixedZ2Symmetry> fixedSymmetries) {
    int shift = 0;
    for (int i = 0; i < fixedSymmetries.size(); ++i) {
      if (fixedSymmetries.get(i).qubit() < qubit) {
        ++shift;
      }
    }
    return qubit - shift;
  }

  private static int parityEigenvalue(final int electronCount) {
    return (electronCount & 1) == 0 ? 1 : -1;
  }
}
