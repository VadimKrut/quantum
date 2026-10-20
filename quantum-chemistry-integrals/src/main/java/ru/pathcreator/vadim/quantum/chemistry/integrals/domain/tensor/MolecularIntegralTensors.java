/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor;

/** Хранит AO-интегралы: одноэлектронные матрицы dense, двухэлектронный ERI tensor packed по симметрии. */
public final class MolecularIntegralTensors {

  private final int orbitalCount;
  private final double nuclearRepulsionEnergy;
  private final double[] overlap;
  private final double[] kinetic;
  private final double[] nuclearAttraction;
  private final double[] coreHamiltonian;
  private final PackedElectronRepulsionTensor electronRepulsion;

  private MolecularIntegralTensors(
      final int orbitalCount,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] kinetic,
      final double[] nuclearAttraction,
      final double[] coreHamiltonian,
      final PackedElectronRepulsionTensor electronRepulsion) {
    this.orbitalCount = orbitalCount;
    this.nuclearRepulsionEnergy = nuclearRepulsionEnergy;
    this.overlap = overlap;
    this.kinetic = kinetic;
    this.nuclearAttraction = nuclearAttraction;
    this.coreHamiltonian = coreHamiltonian;
    this.electronRepulsion = electronRepulsion;
  }

  public static MolecularIntegralTensors of(
      final int orbitalCount,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] kinetic,
      final double[] nuclearAttraction,
      final double[] electronRepulsion) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Integral tensor orbital count must be positive.");
    }
    if (!Double.isFinite(nuclearRepulsionEnergy)) {
      throw new IllegalArgumentException("Integral tensor nuclear energy must be finite.");
    }
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final int tensorSize = Math.multiplyExact(matrixSize, matrixSize);
    MolecularIntegralTensors.requireArray(overlap, matrixSize, "Overlap tensor");
    MolecularIntegralTensors.requireArray(kinetic, matrixSize, "Kinetic tensor");
    MolecularIntegralTensors.requireArray(
        nuclearAttraction, matrixSize, "Nuclear attraction tensor");
    MolecularIntegralTensors.requireArray(
        electronRepulsion, tensorSize, "Electron repulsion tensor");
    final PackedElectronRepulsionTensor packedElectronRepulsion =
        PackedElectronRepulsionTensor.fromDense(orbitalCount, electronRepulsion);
    final double[] coreHamiltonian = new double[matrixSize];
    for (int i = 0; i < matrixSize; ++i) {
      coreHamiltonian[i] = kinetic[i] + nuclearAttraction[i];
    }
    return new MolecularIntegralTensors(
        orbitalCount,
        nuclearRepulsionEnergy,
        overlap.clone(),
        kinetic.clone(),
        nuclearAttraction.clone(),
        coreHamiltonian,
        packedElectronRepulsion);
  }

  public static MolecularIntegralTensors ofPacked(
      final int orbitalCount,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] kinetic,
      final double[] nuclearAttraction,
      final PackedElectronRepulsionTensor electronRepulsion) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Integral tensor orbital count must be positive.");
    }
    if (!Double.isFinite(nuclearRepulsionEnergy)) {
      throw new IllegalArgumentException("Integral tensor nuclear energy must be finite.");
    }
    if (electronRepulsion == null || electronRepulsion.orbitalCount() != orbitalCount) {
      throw new IllegalArgumentException(
          "Packed electron repulsion tensor must match orbital count.");
    }
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    MolecularIntegralTensors.requireArray(overlap, matrixSize, "Overlap tensor");
    MolecularIntegralTensors.requireArray(kinetic, matrixSize, "Kinetic tensor");
    MolecularIntegralTensors.requireArray(
        nuclearAttraction, matrixSize, "Nuclear attraction tensor");
    final double[] coreHamiltonian = new double[matrixSize];
    for (int i = 0; i < matrixSize; ++i) {
      coreHamiltonian[i] = kinetic[i] + nuclearAttraction[i];
    }
    return new MolecularIntegralTensors(
        orbitalCount,
        nuclearRepulsionEnergy,
        overlap.clone(),
        kinetic.clone(),
        nuclearAttraction.clone(),
        coreHamiltonian,
        electronRepulsion);
  }

  public int orbitalCount() {
    return this.orbitalCount;
  }

  public double nuclearRepulsionEnergy() {
    return this.nuclearRepulsionEnergy;
  }

  public double[] overlapCopy() {
    return this.overlap.clone();
  }

  public double[] kineticCopy() {
    return this.kinetic.clone();
  }

  public double[] nuclearAttractionCopy() {
    return this.nuclearAttraction.clone();
  }

  public double[] coreHamiltonianCopy() {
    return this.coreHamiltonian.clone();
  }

  public double[] electronRepulsionCopy() {
    return this.electronRepulsion.toDenseArray();
  }

  public int electronRepulsionPackedElementCount() {
    return this.electronRepulsion.packedElementCount();
  }

  public double overlapAt(final int row, final int column) {
    return this.overlap[this.matrixIndex(row, column)];
  }

  public double coreHamiltonianAt(final int row, final int column) {
    return this.coreHamiltonian[this.matrixIndex(row, column)];
  }

  public double electronRepulsionAt(
      final int first, final int second, final int third, final int fourth) {
    return this.electronRepulsion.valueAt(first, second, third, fourth);
  }

  public int matrixIndex(final int row, final int column) {
    this.requireOrbital(row);
    this.requireOrbital(column);
    return row * this.orbitalCount + column;
  }

  public int tensorIndex(final int first, final int second, final int third, final int fourth) {
    this.requireOrbital(first);
    this.requireOrbital(second);
    this.requireOrbital(third);
    this.requireOrbital(fourth);
    return ((first * this.orbitalCount + second) * this.orbitalCount + third) * this.orbitalCount
        + fourth;
  }

  private void requireOrbital(final int orbital) {
    if (orbital < 0 || orbital >= this.orbitalCount) {
      throw new IllegalArgumentException("Integral orbital index is out of bounds.");
    }
  }

  private static void requireArray(
      final double[] values, final int expectedLength, final String subjectName) {
    if (values == null || values.length != expectedLength) {
      throw new IllegalArgumentException(subjectName + " length does not match orbital count.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }
}
