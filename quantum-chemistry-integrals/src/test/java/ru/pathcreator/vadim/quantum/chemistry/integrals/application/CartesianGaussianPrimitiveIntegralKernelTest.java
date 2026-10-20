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

import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;

public final class CartesianGaussianPrimitiveIntegralKernelTest {

  private static final double EXPONENT = 0.7;
  private static final IntegralAtom CENTER = IntegralAtom.of(6, 0.0, 0.0, 0.0);
  private static final IntegralAtom SHIFTED_CENTER = IntegralAtom.of(6, 0.4, -0.2, 0.3);

  @Test
  public void normalizedSameCenterOverlapRespectsCartesianAngularOrthogonality() {
    final CartesianGaussianPrimitiveIntegralKernel kernel =
        new CartesianGaussianPrimitiveIntegralKernel();
    final double sNorm = CartesianGaussianPrimitiveIntegralKernelTest.sNormalization(EXPONENT);
    final double pNorm = CartesianGaussianPrimitiveIntegralKernelTest.pNormalization(EXPONENT);

    assertEquals(
        1.0,
        sNorm
            * sNorm
            * kernel.overlap(
                EXPONENT,
                GaussianAngularMomentum.S,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.S,
                CENTER),
        1.0E-12);
    assertEquals(
        1.0,
        pNorm
            * pNorm
            * kernel.overlap(
                EXPONENT,
                GaussianAngularMomentum.PX,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.PX,
                CENTER),
        1.0E-12);
    assertEquals(
        0.0,
        pNorm
            * pNorm
            * kernel.overlap(
                EXPONENT,
                GaussianAngularMomentum.PX,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.PY,
                CENTER),
        1.0E-12);
    assertEquals(
        0.0,
        sNorm
            * pNorm
            * kernel.overlap(
                EXPONENT,
                GaussianAngularMomentum.S,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.PZ,
                CENTER),
        1.0E-12);
  }

  @Test
  public void kineticMatchesClosedFormSameCenterPrimitiveExpectations() {
    final CartesianGaussianPrimitiveIntegralKernel kernel =
        new CartesianGaussianPrimitiveIntegralKernel();
    final double sNorm = CartesianGaussianPrimitiveIntegralKernelTest.sNormalization(EXPONENT);
    final double pNorm = CartesianGaussianPrimitiveIntegralKernelTest.pNormalization(EXPONENT);
    final double sKinetic =
        sNorm
            * sNorm
            * kernel.kinetic(
                EXPONENT,
                GaussianAngularMomentum.S,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.S,
                CENTER);
    final double pxKinetic =
        pNorm
            * pNorm
            * kernel.kinetic(
                EXPONENT,
                GaussianAngularMomentum.PX,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.PX,
                CENTER);
    final double pyKinetic =
        pNorm
            * pNorm
            * kernel.kinetic(
                EXPONENT,
                GaussianAngularMomentum.PY,
                CENTER,
                EXPONENT,
                GaussianAngularMomentum.PY,
                CENTER);

    assertEquals(1.5 * EXPONENT, sKinetic, 1.0E-12);
    assertEquals(2.5 * EXPONENT, pxKinetic, 1.0E-12);
    assertEquals(pxKinetic, pyKinetic, 1.0E-12);
  }

  @Test
  public void translatedOverlapAndKineticStayFiniteAndSymmetric() {
    final CartesianGaussianPrimitiveIntegralKernel kernel =
        new CartesianGaussianPrimitiveIntegralKernel();
    final double firstOverlap =
        kernel.overlap(
            EXPONENT,
            GaussianAngularMomentum.PX,
            CENTER,
            1.1,
            GaussianAngularMomentum.S,
            SHIFTED_CENTER);
    final double secondOverlap =
        kernel.overlap(
            1.1,
            GaussianAngularMomentum.S,
            SHIFTED_CENTER,
            EXPONENT,
            GaussianAngularMomentum.PX,
            CENTER);
    final double firstKinetic =
        kernel.kinetic(
            EXPONENT,
            GaussianAngularMomentum.PX,
            CENTER,
            1.1,
            GaussianAngularMomentum.S,
            SHIFTED_CENTER);
    final double secondKinetic =
        kernel.kinetic(
            1.1,
            GaussianAngularMomentum.S,
            SHIFTED_CENTER,
            EXPONENT,
            GaussianAngularMomentum.PX,
            CENTER);

    assertEquals(firstOverlap, secondOverlap, 1.0E-12);
    assertEquals(firstKinetic, secondKinetic, 1.0E-12);
    assertTrue(Double.isFinite(firstOverlap));
    assertTrue(Double.isFinite(firstKinetic));
  }

  @Test
  public void smallArgumentBoysRegressionMatchesPyScfForCoreValencePzPzEri() {
    final CartesianGaussianPrimitiveIntegralKernel kernel =
        new CartesianGaussianPrimitiveIntegralKernel();
    final IntegralAtom hydrogen = IntegralAtom.of(1, 0.0, 0.0, 0.0);
    final IntegralAtom fluorine = IntegralAtom.of(9, 0.0, 0.0, 0.9168 * 1.8897259886);
    final double pExponent = 0.358151393;
    final double hDiffuseExponent = 0.1612778;
    final double fCoreExponent = 1051.36609;
    final double normalized =
        CartesianGaussianPrimitiveIntegralKernelTest.pNormalization(pExponent)
            * CartesianGaussianPrimitiveIntegralKernelTest.pNormalization(pExponent)
            * CartesianGaussianPrimitiveIntegralKernelTest.sNormalization(fCoreExponent)
            * CartesianGaussianPrimitiveIntegralKernelTest.sNormalization(hDiffuseExponent)
            * kernel.electronRepulsion(
                pExponent,
                GaussianAngularMomentum.PZ,
                fluorine,
                pExponent,
                GaussianAngularMomentum.PZ,
                fluorine,
                fCoreExponent,
                GaussianAngularMomentum.S,
                fluorine,
                hDiffuseExponent,
                GaussianAngularMomentum.S,
                hydrogen);

    assertEquals(0.0015293941990031715, normalized, 1.5E-10);
  }

  private static double sNormalization(final double exponent) {
    return Math.pow(2.0 * exponent / Math.PI, 0.75);
  }

  private static double pNormalization(final double exponent) {
    return CartesianGaussianPrimitiveIntegralKernelTest.sNormalization(exponent)
        * 2.0
        * Math.sqrt(exponent);
  }
}
