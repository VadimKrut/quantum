/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;

public final class MolecularOrbitalIntegralTransformerTest {

  private static final double H2_DISTANCE_BOHR = 1.4018897259885788;
  private static final double[] H2_RHF_MO_COEFFICIENTS =
      new double[] {0.5490422918358915, -1.210302519830917, 0.5490422918358911, 1.2103025198309167};

  @Test
  public void transformsHydrogenSto3gAtomicOrbitalIntegralsToMolecularOrbitalIntegrals() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        MolecularOrbitalIntegralTransformerTest.hydrogenAtomicOrbitalTensors();
    final MolecularIntegralTensors molecularOrbitalTensors =
        new MolecularOrbitalIntegralTransformer()
            .transform(atomicOrbitalTensors, H2_RHF_MO_COEFFICIENTS);

    assertEquals(2, molecularOrbitalTensors.orbitalCount());
    assertEquals(1.0, molecularOrbitalTensors.overlapAt(0, 0), 1.0E-8);
    assertEquals(0.0, molecularOrbitalTensors.overlapAt(0, 1), 1.0E-8);
    assertEquals(1.0, molecularOrbitalTensors.overlapAt(1, 1), 1.0E-8);
    assertEquals(-1.252192846122027, molecularOrbitalTensors.coreHamiltonianAt(0, 0), 1.0E-6);
    assertEquals(-0.476229994177529, molecularOrbitalTensors.coreHamiltonianAt(1, 1), 1.0E-6);
    assertEquals(
        0.674403227651317, molecularOrbitalTensors.electronRepulsionAt(0, 0, 0, 0), 1.0E-7);
    assertEquals(
        molecularOrbitalTensors.electronRepulsionAt(0, 0, 1, 1),
        molecularOrbitalTensors.electronRepulsionAt(1, 1, 0, 0),
        1.0E-12);
    assertTrue(Math.abs(molecularOrbitalTensors.coreHamiltonianAt(0, 1)) < 1.0E-10);
  }

  @Test
  public void leadingOrbitalWindowMatchesFullTransformPrefix() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        MolecularOrbitalIntegralTransformerTest.hydrogenAtomicOrbitalTensors();
    final MolecularOrbitalIntegralTransformer transformer =
        new MolecularOrbitalIntegralTransformer();
    final MolecularIntegralTensors full =
        transformer.transform(atomicOrbitalTensors, H2_RHF_MO_COEFFICIENTS);
    final MolecularIntegralTensors window =
        transformer.transformLeadingOrbitalWindow(atomicOrbitalTensors, H2_RHF_MO_COEFFICIENTS, 1);

    assertEquals(1, window.orbitalCount());
    assertEquals(full.overlapAt(0, 0), window.overlapAt(0, 0), 1.0E-12);
    assertEquals(full.coreHamiltonianAt(0, 0), window.coreHamiltonianAt(0, 0), 1.0E-12);
    assertEquals(
        full.electronRepulsionAt(0, 0, 0, 0), window.electronRepulsionAt(0, 0, 0, 0), 1.0E-12);
  }

  @Test
  public void leadingOrbitalWindowMatchesFullTransformPrefixWhenSourceHasMoreOrbitals() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        MolecularOrbitalIntegralTransformerTest.syntheticFourOrbitalTensors();
    final double[] coefficients =
        new double[] {
          0.72, -0.31, 0.18, 0.07,
          0.14, 0.66, -0.29, 0.11,
          -0.23, 0.19, 0.74, -0.17,
          0.09, -0.22, 0.16, 0.69
        };
    final MolecularOrbitalIntegralTransformer transformer =
        new MolecularOrbitalIntegralTransformer();
    final MolecularIntegralTensors full = transformer.transform(atomicOrbitalTensors, coefficients);
    final MolecularIntegralTensors window =
        transformer.transformLeadingOrbitalWindow(atomicOrbitalTensors, coefficients, 2);

    assertEquals(full.overlapAt(0, 0), window.overlapAt(0, 0), 1.0E-12);
    assertEquals(full.overlapAt(0, 1), window.overlapAt(0, 1), 1.0E-12);
    assertEquals(full.overlapAt(1, 1), window.overlapAt(1, 1), 1.0E-12);
    for (int p = 0; p < 2; ++p) {
      for (int q = 0; q < 2; ++q) {
        for (int r = 0; r < 2; ++r) {
          for (int s = 0; s < 2; ++s) {
            assertEquals(
                full.electronRepulsionAt(p, q, r, s),
                window.electronRepulsionAt(p, q, r, s),
                1.0E-12);
          }
        }
      }
    }
  }

  @Test
  public void orbitalWindowMatchesFullTransformInteriorActiveSpace() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        MolecularOrbitalIntegralTransformerTest.syntheticFourOrbitalTensors();
    final double[] coefficients =
        new double[] {
          0.72, -0.31, 0.18, 0.07,
          0.14, 0.66, -0.29, 0.11,
          -0.23, 0.19, 0.74, -0.17,
          0.09, -0.22, 0.16, 0.69
        };
    final int firstOrbital = 1;
    final int orbitalWindowCount = 2;
    final MolecularOrbitalIntegralTransformer transformer =
        new MolecularOrbitalIntegralTransformer();
    final MolecularIntegralTensors full = transformer.transform(atomicOrbitalTensors, coefficients);
    final MolecularIntegralTensors window =
        transformer.transformOrbitalWindow(
            atomicOrbitalTensors, coefficients, firstOrbital, orbitalWindowCount);

    assertEquals(orbitalWindowCount, window.orbitalCount());
    for (int p = 0; p < orbitalWindowCount; ++p) {
      final int sourceP = firstOrbital + p;
      for (int q = 0; q < orbitalWindowCount; ++q) {
        final int sourceQ = firstOrbital + q;
        assertEquals(full.overlapAt(sourceP, sourceQ), window.overlapAt(p, q), 1.0E-12);
        assertEquals(
            full.coreHamiltonianAt(sourceP, sourceQ), window.coreHamiltonianAt(p, q), 1.0E-12);
        for (int r = 0; r < orbitalWindowCount; ++r) {
          final int sourceR = firstOrbital + r;
          for (int s = 0; s < orbitalWindowCount; ++s) {
            final int sourceS = firstOrbital + s;
            assertEquals(
                full.electronRepulsionAt(sourceP, sourceQ, sourceR, sourceS),
                window.electronRepulsionAt(p, q, r, s),
                1.0E-12);
          }
        }
      }
    }
  }

  @Test
  public void directLeadingOrbitalWindowMatchesFullTransformPrefixWithoutFullAoTensorInput() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        MolecularOrbitalIntegralTransformerTest.syntheticFourOrbitalTensors();
    final double[] coefficients =
        new double[] {
          0.72, -0.31, 0.18, 0.07,
          0.14, 0.66, -0.29, 0.11,
          -0.23, 0.19, 0.74, -0.17,
          0.09, -0.22, 0.16, 0.69
        };
    final MolecularIntegralTensors full =
        new MolecularOrbitalIntegralTransformer().transform(atomicOrbitalTensors, coefficients);
    final DirectMolecularOrbitalWindowTransformer transformer =
        new DirectMolecularOrbitalWindowTransformer();
    final MolecularIntegralTensors window =
        transformer.transformLeadingOrbitalWindow(
            MolecularOrbitalIntegralTransformerTest.atomicOrbitalTensorsToOneElectron(
                atomicOrbitalTensors),
            coefficients,
            3,
            atomicOrbitalTensors::electronRepulsionAt);

    assertEquals(3, window.orbitalCount());
    for (int p = 0; p < 3; ++p) {
      for (int q = 0; q < 3; ++q) {
        assertEquals(full.overlapAt(p, q), window.overlapAt(p, q), 1.0E-12);
        assertEquals(full.coreHamiltonianAt(p, q), window.coreHamiltonianAt(p, q), 1.0E-12);
        for (int r = 0; r < 3; ++r) {
          for (int s = 0; s < 3; ++s) {
            assertEquals(
                full.electronRepulsionAt(p, q, r, s),
                window.electronRepulsionAt(p, q, r, s),
                1.0E-12);
          }
        }
      }
    }
  }

  @Test
  public void directOrbitalWindowMatchesFullTransformInteriorActiveSpaceWithoutFullAoTensorInput() {
    final MolecularIntegralTensors atomicOrbitalTensors =
        MolecularOrbitalIntegralTransformerTest.syntheticFourOrbitalTensors();
    final double[] coefficients =
        new double[] {
          0.72, -0.31, 0.18, 0.07,
          0.14, 0.66, -0.29, 0.11,
          -0.23, 0.19, 0.74, -0.17,
          0.09, -0.22, 0.16, 0.69
        };
    final int firstOrbital = 1;
    final int orbitalWindowCount = 2;
    final MolecularIntegralTensors full =
        new MolecularOrbitalIntegralTransformer().transform(atomicOrbitalTensors, coefficients);
    final DirectMolecularOrbitalWindowTransformer transformer =
        new DirectMolecularOrbitalWindowTransformer();
    final MolecularIntegralTensors window =
        transformer.transformOrbitalWindow(
            MolecularOrbitalIntegralTransformerTest.atomicOrbitalTensorsToOneElectron(
                atomicOrbitalTensors),
            coefficients,
            firstOrbital,
            orbitalWindowCount,
            atomicOrbitalTensors::electronRepulsionAt);

    assertEquals(orbitalWindowCount, window.orbitalCount());
    for (int p = 0; p < orbitalWindowCount; ++p) {
      final int sourceP = firstOrbital + p;
      for (int q = 0; q < orbitalWindowCount; ++q) {
        final int sourceQ = firstOrbital + q;
        assertEquals(full.overlapAt(sourceP, sourceQ), window.overlapAt(p, q), 1.0E-12);
        assertEquals(
            full.coreHamiltonianAt(sourceP, sourceQ), window.coreHamiltonianAt(p, q), 1.0E-12);
        for (int r = 0; r < orbitalWindowCount; ++r) {
          final int sourceR = firstOrbital + r;
          for (int s = 0; s < orbitalWindowCount; ++s) {
            final int sourceS = firstOrbital + s;
            assertEquals(
                full.electronRepulsionAt(sourceP, sourceQ, sourceR, sourceS),
                window.electronRepulsionAt(p, q, r, s),
                1.0E-12);
          }
        }
      }
    }
  }

  @Test
  public void directLeadingOrbitalWindowSkipsScreenedAoQuartetsBeforeValueLookup() {
    final int[] lookupCount = new int[1];
    final AoElectronRepulsionValues screenedValues =
        new AoElectronRepulsionValues() {
          public double valueAt(
              final int first, final int second, final int third, final int fourth) {
            ++lookupCount[0];
            if (first == 0 && second == 0 && third == 0 && fourth == 0) {
              return 0.7;
            }
            throw new AssertionError("Screened AO quartet reached value lookup.");
          }

          public boolean screenedOut(
              final int first, final int second, final int third, final int fourth) {
            return first != 0 || second != 0 || third != 0 || fourth != 0;
          }
        };
    final MolecularIntegralTensors window =
        new DirectMolecularOrbitalWindowTransformer()
            .transformLeadingOrbitalWindow(
                ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor
                    .OneElectronIntegralMatrices.of(
                    2,
                    0.0,
                    new double[] {1.0, 0.0, 0.0, 1.0},
                    new double[] {0.2, 0.0, 0.0, 0.3},
                    new double[] {-0.1, 0.0, 0.0, -0.1}),
                new double[] {1.0, 0.0, 0.0, 1.0},
                1,
                screenedValues);

    assertEquals(1, lookupCount[0]);
    assertEquals(0.7, window.electronRepulsionAt(0, 0, 0, 0), 1.0E-12);
  }

  static MolecularIntegralTensors hydrogenAtomicOrbitalTensors() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, H2_DISTANCE_BOHR)),
            0);
    return new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
  }

  private static ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor
          .OneElectronIntegralMatrices
      atomicOrbitalTensorsToOneElectron(final MolecularIntegralTensors tensors) {
    return ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor
        .OneElectronIntegralMatrices.of(
        tensors.orbitalCount(),
        tensors.nuclearRepulsionEnergy(),
        tensors.overlapCopy(),
        tensors.coreHamiltonianCopy(),
        new double[Math.multiplyExact(tensors.orbitalCount(), tensors.orbitalCount())]);
  }

  private static MolecularIntegralTensors syntheticFourOrbitalTensors() {
    final int orbitalCount = 4;
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final double[] overlap = new double[matrixSize];
    final double[] kinetic = new double[matrixSize];
    final double[] nuclearAttraction = new double[matrixSize];
    final double[] electronRepulsion = new double[Math.multiplyExact(matrixSize, matrixSize)];
    for (int row = 0; row < orbitalCount; ++row) {
      for (int column = 0; column < orbitalCount; ++column) {
        final int matrixIndex = row * orbitalCount + column;
        overlap[matrixIndex] = row == column ? 1.0 : 0.02 * (row + column + 1);
        kinetic[matrixIndex] = row == column ? 0.3 + row : 0.01 * (row + column + 1);
        nuclearAttraction[matrixIndex] =
            row == column ? -0.7 - 0.1 * row : -0.015 * (row + column + 1);
      }
    }
    for (int p = 0; p < orbitalCount; ++p) {
      for (int q = 0; q <= p; ++q) {
        final int firstPair = MolecularOrbitalIntegralTransformerTest.pairIndex(p, q);
        for (int r = 0; r < orbitalCount; ++r) {
          for (int s = 0; s <= r; ++s) {
            final int secondPair = MolecularOrbitalIntegralTransformerTest.pairIndex(r, s);
            if (secondPair > firstPair) {
              continue;
            }
            final double value = 0.03 + 0.007 * (1 + firstPair * 11 + secondPair);
            MolecularOrbitalIntegralTransformerTest.setEriSymmetry(
                electronRepulsion, orbitalCount, p, q, r, s, value);
          }
        }
      }
    }
    return MolecularIntegralTensors.of(
        orbitalCount, 0.25, overlap, kinetic, nuclearAttraction, electronRepulsion);
  }

  private static int pairIndex(final int first, final int second) {
    final int upper = Math.max(first, second);
    final int lower = Math.min(first, second);
    return upper * (upper + 1) / 2 + lower;
  }

  private static void setEriSymmetry(
      final double[] values,
      final int orbitalCount,
      final int p,
      final int q,
      final int r,
      final int s,
      final double value) {
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, p, q, r, s, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, q, p, r, s, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, p, q, s, r, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, q, p, s, r, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, r, s, p, q, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, s, r, p, q, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, r, s, q, p, value);
    MolecularOrbitalIntegralTransformerTest.setEri(values, orbitalCount, s, r, q, p, value);
  }

  private static void setEri(
      final double[] values,
      final int orbitalCount,
      final int p,
      final int q,
      final int r,
      final int s,
      final double value) {
    values[((p * orbitalCount + q) * orbitalCount + r) * orbitalCount + s] = value;
  }
}
