/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;

/**
 * Низкоуровневый kernel примитивных Cartesian Gaussian integrals.
 */
public final class CartesianGaussianPrimitiveIntegralKernel {

  private static final double TWO_PI_TO_POWER_FIVE_HALVES = 2.0 * Math.pow(Math.PI, 2.5);
  private static final double BOYS_SERIES_LIMIT = 1.0;
  private static final double[] BOYS_GL64_NODES =
      new double[] {
        0.00034747913211391479,
        0.0018299416140223901,
        0.0044933142616278565,
        0.0083318730576870115,
        0.013336586105044512,
        0.019495600173973116,
        0.026794312570798617,
        0.035215413934030215,
        0.044738931460748588,
        0.05534227700244293,
        0.067000300922953615,
        0.079685351873709842,
        0.093367342438601231,
        0.10801382052832931,
        0.12359004636973403,
        0.14005907491419456,
        0.15738184347288336,
        0.17551726437267134,
        0.19442232241380336,
        0.214052176898683,
        0.23436026799005272,
        0.25529842714647355,
        0.27681699137326798,
        0.29886492101800421,
        0.32138992083116591,
        0.34433856400489449,
        0.36765641889561629,
        0.39128817812999644,
        0.41517778978800357,
        0.43926859035193971,
        0.46350343910610048,
        0.48782485366828776,
        0.51217514633171224,
        0.53649656089389952,
        0.56073140964806023,
        0.58482221021199643,
        0.6087118218700035,
        0.63234358110438371,
        0.65566143599510551,
        0.67861007916883409,
        0.70113507898199579,
        0.72318300862673202,
        0.74470157285352645,
        0.76563973200994728,
        0.78594782310131706,
        0.80557767758619669,
        0.82448273562732866,
        0.84261815652711669,
        0.85994092508580544,
        0.87640995363026597,
        0.89198617947167069,
        0.90663265756139877,
        0.92031464812629016,
        0.93299969907704638,
        0.94465772299755701,
        0.95526106853925141,
        0.96478458606596984,
        0.97320568742920144,
        0.98050439982602688,
        0.98666341389495549,
        0.99166812694231299,
        0.99550668573837209,
        0.99817005838597761,
        0.99965252086788614
      };
  private static final double[] BOYS_GL64_WEIGHTS =
      new double[] {
        0.00089164036084706997,
        0.0020735166302822496,
        0.0032522289844892512,
        0.0044233799131816985,
        0.0055840697300655138,
        0.0067315239483589301,
        0.0078630152380125152,
        0.0089758578878486421,
        0.010067411576765044,
        0.011135086904191484,
        0.0121763512843556,
        0.013188734857527456,
        0.014169836307129768,
        0.015117328536201277,
        0.016028964177425747,
        0.016902580918570897,
        0.017736106628441151,
        0.018527564270120044,
        0.01927507658930782,
        0.019976870566360272,
        0.020631281621311788,
        0.021236757561826806,
        0.021791862264661774,
        0.022295279081878283,
        0.022745813963709092,
        0.023142398290657236,
        0.023484091408105038,
        0.02377008285741521,
        0.023999694298229211,
        0.024172381117401498,
        0.024287733720751756,
        0.024345478504569907,
        0.024345478504569907,
        0.024287733720751756,
        0.024172381117401498,
        0.023999694298229211,
        0.02377008285741521,
        0.023484091408105038,
        0.023142398290657236,
        0.022745813963709092,
        0.022295279081878283,
        0.021791862264661774,
        0.021236757561826806,
        0.020631281621311788,
        0.019976870566360272,
        0.01927507658930782,
        0.018527564270120044,
        0.017736106628441151,
        0.016902580918570897,
        0.016028964177425747,
        0.015117328536201277,
        0.014169836307129768,
        0.013188734857527456,
        0.0121763512843556,
        0.011135086904191484,
        0.010067411576765044,
        0.0089758578878486421,
        0.0078630152380125152,
        0.0067315239483589301,
        0.0055840697300655138,
        0.0044233799131816985,
        0.0032522289844892512,
        0.0020735166302822496,
        0.00089164036084706997
      };
  private static final TaylorContext NUCLEAR_CONTEXT = TaylorContext.create(6, 8);
  private static final TaylorContext ERI_CONTEXT = TaylorContext.create(12, 4);

  public double overlap(
      final double firstExponent,
      final GaussianAngularMomentum firstAngularMomentum,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final GaussianAngularMomentum secondAngularMomentum,
      final IntegralAtom secondAtom) {
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(firstExponent, "First exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(
        secondExponent, "Second exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(firstAngularMomentum, "First");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(
        secondAngularMomentum, "Second");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(firstAtom, "First atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(secondAtom, "Second atom");
    return CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
            firstAngularMomentum.xPower(),
            secondAngularMomentum.xPower(),
            firstExponent,
            secondExponent,
            firstAtom.xBohr(),
            secondAtom.xBohr())
        * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
            firstAngularMomentum.yPower(),
            secondAngularMomentum.yPower(),
            firstExponent,
            secondExponent,
            firstAtom.yBohr(),
            secondAtom.yBohr())
        * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
            firstAngularMomentum.zPower(),
            secondAngularMomentum.zPower(),
            firstExponent,
            secondExponent,
            firstAtom.zBohr(),
            secondAtom.zBohr());
  }

  public double kinetic(
      final double firstExponent,
      final GaussianAngularMomentum firstAngularMomentum,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final GaussianAngularMomentum secondAngularMomentum,
      final IntegralAtom secondAtom) {
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(firstExponent, "First exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(
        secondExponent, "Second exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(firstAngularMomentum, "First");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(
        secondAngularMomentum, "Second");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(firstAtom, "First atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(secondAtom, "Second atom");
    final double x =
        CartesianGaussianPrimitiveIntegralKernel.kineticAxis(
                firstAngularMomentum.xPower(),
                secondAngularMomentum.xPower(),
                firstExponent,
                secondExponent,
                firstAtom.xBohr(),
                secondAtom.xBohr())
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstAngularMomentum.yPower(),
                secondAngularMomentum.yPower(),
                firstExponent,
                secondExponent,
                firstAtom.yBohr(),
                secondAtom.yBohr())
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstAngularMomentum.zPower(),
                secondAngularMomentum.zPower(),
                firstExponent,
                secondExponent,
                firstAtom.zBohr(),
                secondAtom.zBohr());
    final double y =
        CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstAngularMomentum.xPower(),
                secondAngularMomentum.xPower(),
                firstExponent,
                secondExponent,
                firstAtom.xBohr(),
                secondAtom.xBohr())
            * CartesianGaussianPrimitiveIntegralKernel.kineticAxis(
                firstAngularMomentum.yPower(),
                secondAngularMomentum.yPower(),
                firstExponent,
                secondExponent,
                firstAtom.yBohr(),
                secondAtom.yBohr())
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstAngularMomentum.zPower(),
                secondAngularMomentum.zPower(),
                firstExponent,
                secondExponent,
                firstAtom.zBohr(),
                secondAtom.zBohr());
    final double z =
        CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstAngularMomentum.xPower(),
                secondAngularMomentum.xPower(),
                firstExponent,
                secondExponent,
                firstAtom.xBohr(),
                secondAtom.xBohr())
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstAngularMomentum.yPower(),
                secondAngularMomentum.yPower(),
                firstExponent,
                secondExponent,
                firstAtom.yBohr(),
                secondAtom.yBohr())
            * CartesianGaussianPrimitiveIntegralKernel.kineticAxis(
                firstAngularMomentum.zPower(),
                secondAngularMomentum.zPower(),
                firstExponent,
                secondExponent,
                firstAtom.zBohr(),
                secondAtom.zBohr());
    return -0.5 * (x + y + z);
  }

  public double dipoleMoment(
      final int axis,
      final double firstExponent,
      final GaussianAngularMomentum firstAngularMomentum,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final GaussianAngularMomentum secondAngularMomentum,
      final IntegralAtom secondAtom) {
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(firstExponent, "First exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(
        secondExponent, "Second exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(firstAngularMomentum, "First");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(
        secondAngularMomentum, "Second");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(firstAtom, "First atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(secondAtom, "Second atom");
    if (axis < 0 || axis > 2) {
      throw new IllegalArgumentException("Dipole axis must be 0, 1 or 2.");
    }
    final double x =
        CartesianGaussianPrimitiveIntegralKernel.axisMoment(
            axis,
            0,
            firstAngularMomentum.xPower(),
            secondAngularMomentum.xPower(),
            firstExponent,
            secondExponent,
            firstAtom.xBohr(),
            secondAtom.xBohr());
    final double y =
        CartesianGaussianPrimitiveIntegralKernel.axisMoment(
            axis,
            1,
            firstAngularMomentum.yPower(),
            secondAngularMomentum.yPower(),
            firstExponent,
            secondExponent,
            firstAtom.yBohr(),
            secondAtom.yBohr());
    final double z =
        CartesianGaussianPrimitiveIntegralKernel.axisMoment(
            axis,
            2,
            firstAngularMomentum.zPower(),
            secondAngularMomentum.zPower(),
            firstExponent,
            secondExponent,
            firstAtom.zBohr(),
            secondAtom.zBohr());
    return x * y * z;
  }

  public double nuclearAttraction(
      final double firstExponent,
      final GaussianAngularMomentum firstAngularMomentum,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final GaussianAngularMomentum secondAngularMomentum,
      final IntegralAtom secondAtom,
      final IntegralAtom nucleus) {
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(firstExponent, "First exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(
        secondExponent, "Second exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(firstAngularMomentum, "First");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(
        secondAngularMomentum, "Second");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(firstAtom, "First atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(secondAtom, "Second atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(nucleus, "Nucleus");
    final int[] derivativeExponents =
        new int[] {
          firstAngularMomentum.xPower(),
          firstAngularMomentum.yPower(),
          firstAngularMomentum.zPower(),
          secondAngularMomentum.xPower(),
          secondAngularMomentum.yPower(),
          secondAngularMomentum.zPower()
        };
    final int derivativeOrder =
        firstAngularMomentum.totalPower() + secondAngularMomentum.totalPower();
    final double derivative =
        derivativeOrder <= NUCLEAR_CONTEXT.maxOrder()
            ? CartesianGaussianPrimitiveIntegralKernel.nuclearAttractionS(
                    firstExponent, firstAtom, secondExponent, secondAtom, nucleus)
                .derivative(derivativeExponents)
            : CartesianGaussianPrimitiveIntegralKernel.nuclearAttractionSparseS(
                    derivativeOrder,
                    derivativeExponents,
                    firstExponent,
                    firstAtom,
                    secondExponent,
                    secondAtom,
                    nucleus)
                .derivative(derivativeExponents);
    return derivative
        * CartesianGaussianPrimitiveIntegralKernel.derivativeScale(
            firstExponent, firstAngularMomentum)
        * CartesianGaussianPrimitiveIntegralKernel.derivativeScale(
            secondExponent, secondAngularMomentum);
  }

  public double electronRepulsion(
      final double firstExponent,
      final GaussianAngularMomentum firstAngularMomentum,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final GaussianAngularMomentum secondAngularMomentum,
      final IntegralAtom secondAtom,
      final double thirdExponent,
      final GaussianAngularMomentum thirdAngularMomentum,
      final IntegralAtom thirdAtom,
      final double fourthExponent,
      final GaussianAngularMomentum fourthAngularMomentum,
      final IntegralAtom fourthAtom) {
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(firstExponent, "First exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(
        secondExponent, "Second exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(thirdExponent, "Third exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireFinitePositive(
        fourthExponent, "Fourth exponent");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(firstAngularMomentum, "First");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(
        secondAngularMomentum, "Second");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(thirdAngularMomentum, "Third");
    CartesianGaussianPrimitiveIntegralKernel.requireAngularMomentum(
        fourthAngularMomentum, "Fourth");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(firstAtom, "First atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(secondAtom, "Second atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(thirdAtom, "Third atom");
    CartesianGaussianPrimitiveIntegralKernel.requireAtom(fourthAtom, "Fourth atom");
    final int[] derivativeExponents =
        new int[] {
          firstAngularMomentum.xPower(),
          firstAngularMomentum.yPower(),
          firstAngularMomentum.zPower(),
          secondAngularMomentum.xPower(),
          secondAngularMomentum.yPower(),
          secondAngularMomentum.zPower(),
          thirdAngularMomentum.xPower(),
          thirdAngularMomentum.yPower(),
          thirdAngularMomentum.zPower(),
          fourthAngularMomentum.xPower(),
          fourthAngularMomentum.yPower(),
          fourthAngularMomentum.zPower()
        };
    final int derivativeOrder =
        firstAngularMomentum.totalPower()
            + secondAngularMomentum.totalPower()
            + thirdAngularMomentum.totalPower()
            + fourthAngularMomentum.totalPower();
    final double derivative =
        derivativeOrder <= ERI_CONTEXT.maxOrder()
            ? CartesianGaussianPrimitiveIntegralKernel.electronRepulsionS(
                    firstExponent,
                    firstAtom,
                    secondExponent,
                    secondAtom,
                    thirdExponent,
                    thirdAtom,
                    fourthExponent,
                    fourthAtom)
                .derivative(derivativeExponents)
            : CartesianGaussianPrimitiveIntegralKernel.electronRepulsionSparseS(
                    derivativeOrder,
                    derivativeExponents,
                    firstExponent,
                    firstAtom,
                    secondExponent,
                    secondAtom,
                    thirdExponent,
                    thirdAtom,
                    fourthExponent,
                    fourthAtom)
                .derivative(derivativeExponents);
    return derivative
        * CartesianGaussianPrimitiveIntegralKernel.derivativeScale(
            firstExponent, firstAngularMomentum)
        * CartesianGaussianPrimitiveIntegralKernel.derivativeScale(
            secondExponent, secondAngularMomentum)
        * CartesianGaussianPrimitiveIntegralKernel.derivativeScale(
            thirdExponent, thirdAngularMomentum)
        * CartesianGaussianPrimitiveIntegralKernel.derivativeScale(
            fourthExponent, fourthAngularMomentum);
  }

  private static TaylorPolynomial nuclearAttractionS(
      final double firstExponent,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final IntegralAtom secondAtom,
      final IntegralAtom nucleus) {
    final TaylorContext context = NUCLEAR_CONTEXT;
    final TaylorPolynomial ax = context.variable(0, firstAtom.xBohr());
    final TaylorPolynomial ay = context.variable(1, firstAtom.yBohr());
    final TaylorPolynomial az = context.variable(2, firstAtom.zBohr());
    final TaylorPolynomial bx = context.variable(3, secondAtom.xBohr());
    final TaylorPolynomial by = context.variable(4, secondAtom.yBohr());
    final TaylorPolynomial bz = context.variable(5, secondAtom.zBohr());
    final double exponentSum = firstExponent + secondExponent;
    final double reducedExponent = firstExponent * secondExponent / exponentSum;
    final TaylorPolynomial productX =
        ax.scale(firstExponent).add(bx.scale(secondExponent)).scale(1.0 / exponentSum);
    final TaylorPolynomial productY =
        ay.scale(firstExponent).add(by.scale(secondExponent)).scale(1.0 / exponentSum);
    final TaylorPolynomial productZ =
        az.scale(firstExponent).add(bz.scale(secondExponent)).scale(1.0 / exponentSum);
    final TaylorPolynomial abSquared =
        ax.subtract(bx).square().add(ay.subtract(by).square()).add(az.subtract(bz).square());
    final TaylorPolynomial pcSquared =
        productX
            .subtract(context.constant(nucleus.xBohr()))
            .square()
            .add(productY.subtract(context.constant(nucleus.yBohr())).square())
            .add(productZ.subtract(context.constant(nucleus.zBohr())).square());
    return abSquared
        .scale(-reducedExponent)
        .exp()
        .multiply(CartesianGaussianPrimitiveIntegralKernel.boysZero(pcSquared.scale(exponentSum)))
        .scale(-2.0 * Math.PI * (double) nucleus.atomicNumber() / exponentSum);
  }

  private static SparseTaylorPolynomial nuclearAttractionSparseS(
      final int maxOrder,
      final int[] derivativeExponents,
      final double firstExponent,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final IntegralAtom secondAtom,
      final IntegralAtom nucleus) {
    final SparseTaylorContext context =
        SparseTaylorContext.create(6, maxOrder, derivativeExponents);
    final SparseTaylorPolynomial ax = context.variable(0, firstAtom.xBohr());
    final SparseTaylorPolynomial ay = context.variable(1, firstAtom.yBohr());
    final SparseTaylorPolynomial az = context.variable(2, firstAtom.zBohr());
    final SparseTaylorPolynomial bx = context.variable(3, secondAtom.xBohr());
    final SparseTaylorPolynomial by = context.variable(4, secondAtom.yBohr());
    final SparseTaylorPolynomial bz = context.variable(5, secondAtom.zBohr());
    final double exponentSum = firstExponent + secondExponent;
    final double reducedExponent = firstExponent * secondExponent / exponentSum;
    final SparseTaylorPolynomial productX =
        ax.scale(firstExponent).add(bx.scale(secondExponent)).scale(1.0 / exponentSum);
    final SparseTaylorPolynomial productY =
        ay.scale(firstExponent).add(by.scale(secondExponent)).scale(1.0 / exponentSum);
    final SparseTaylorPolynomial productZ =
        az.scale(firstExponent).add(bz.scale(secondExponent)).scale(1.0 / exponentSum);
    final SparseTaylorPolynomial abSquared =
        ax.subtract(bx).square().add(ay.subtract(by).square()).add(az.subtract(bz).square());
    final SparseTaylorPolynomial pcSquared =
        productX
            .subtract(context.constant(nucleus.xBohr()))
            .square()
            .add(productY.subtract(context.constant(nucleus.yBohr())).square())
            .add(productZ.subtract(context.constant(nucleus.zBohr())).square());
    return abSquared
        .scale(-reducedExponent)
        .exp()
        .multiply(
            CartesianGaussianPrimitiveIntegralKernel.boysZeroSparse(pcSquared.scale(exponentSum)))
        .scale(-2.0 * Math.PI * (double) nucleus.atomicNumber() / exponentSum);
  }

  private static TaylorPolynomial electronRepulsionS(
      final double firstExponent,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final IntegralAtom secondAtom,
      final double thirdExponent,
      final IntegralAtom thirdAtom,
      final double fourthExponent,
      final IntegralAtom fourthAtom) {
    final TaylorContext context = ERI_CONTEXT;
    final TaylorPolynomial ax = context.variable(0, firstAtom.xBohr());
    final TaylorPolynomial ay = context.variable(1, firstAtom.yBohr());
    final TaylorPolynomial az = context.variable(2, firstAtom.zBohr());
    final TaylorPolynomial bx = context.variable(3, secondAtom.xBohr());
    final TaylorPolynomial by = context.variable(4, secondAtom.yBohr());
    final TaylorPolynomial bz = context.variable(5, secondAtom.zBohr());
    final TaylorPolynomial cx = context.variable(6, thirdAtom.xBohr());
    final TaylorPolynomial cy = context.variable(7, thirdAtom.yBohr());
    final TaylorPolynomial cz = context.variable(8, thirdAtom.zBohr());
    final TaylorPolynomial dx = context.variable(9, fourthAtom.xBohr());
    final TaylorPolynomial dy = context.variable(10, fourthAtom.yBohr());
    final TaylorPolynomial dz = context.variable(11, fourthAtom.zBohr());
    final double firstExponentSum = firstExponent + secondExponent;
    final double secondExponentSum = thirdExponent + fourthExponent;
    final double firstReduced = firstExponent * secondExponent / firstExponentSum;
    final double secondReduced = thirdExponent * fourthExponent / secondExponentSum;
    final double productReduced =
        firstExponentSum * secondExponentSum / (firstExponentSum + secondExponentSum);
    final TaylorPolynomial firstProductX =
        ax.scale(firstExponent).add(bx.scale(secondExponent)).scale(1.0 / firstExponentSum);
    final TaylorPolynomial firstProductY =
        ay.scale(firstExponent).add(by.scale(secondExponent)).scale(1.0 / firstExponentSum);
    final TaylorPolynomial firstProductZ =
        az.scale(firstExponent).add(bz.scale(secondExponent)).scale(1.0 / firstExponentSum);
    final TaylorPolynomial secondProductX =
        cx.scale(thirdExponent).add(dx.scale(fourthExponent)).scale(1.0 / secondExponentSum);
    final TaylorPolynomial secondProductY =
        cy.scale(thirdExponent).add(dy.scale(fourthExponent)).scale(1.0 / secondExponentSum);
    final TaylorPolynomial secondProductZ =
        cz.scale(thirdExponent).add(dz.scale(fourthExponent)).scale(1.0 / secondExponentSum);
    final TaylorPolynomial abSquared =
        ax.subtract(bx).square().add(ay.subtract(by).square()).add(az.subtract(bz).square());
    final TaylorPolynomial cdSquared =
        cx.subtract(dx).square().add(cy.subtract(dy).square()).add(cz.subtract(dz).square());
    final TaylorPolynomial pqSquared =
        firstProductX
            .subtract(secondProductX)
            .square()
            .add(firstProductY.subtract(secondProductY).square())
            .add(firstProductZ.subtract(secondProductZ).square());
    return abSquared
        .scale(-firstReduced)
        .add(cdSquared.scale(-secondReduced))
        .exp()
        .multiply(
            CartesianGaussianPrimitiveIntegralKernel.boysZero(pqSquared.scale(productReduced)))
        .scale(
            TWO_PI_TO_POWER_FIVE_HALVES
                / (firstExponentSum
                    * secondExponentSum
                    * Math.sqrt(firstExponentSum + secondExponentSum)));
  }

  private static SparseTaylorPolynomial electronRepulsionSparseS(
      final int maxOrder,
      final int[] derivativeExponents,
      final double firstExponent,
      final IntegralAtom firstAtom,
      final double secondExponent,
      final IntegralAtom secondAtom,
      final double thirdExponent,
      final IntegralAtom thirdAtom,
      final double fourthExponent,
      final IntegralAtom fourthAtom) {
    final SparseTaylorContext context =
        SparseTaylorContext.create(12, maxOrder, derivativeExponents);
    final SparseTaylorPolynomial ax = context.variable(0, firstAtom.xBohr());
    final SparseTaylorPolynomial ay = context.variable(1, firstAtom.yBohr());
    final SparseTaylorPolynomial az = context.variable(2, firstAtom.zBohr());
    final SparseTaylorPolynomial bx = context.variable(3, secondAtom.xBohr());
    final SparseTaylorPolynomial by = context.variable(4, secondAtom.yBohr());
    final SparseTaylorPolynomial bz = context.variable(5, secondAtom.zBohr());
    final SparseTaylorPolynomial cx = context.variable(6, thirdAtom.xBohr());
    final SparseTaylorPolynomial cy = context.variable(7, thirdAtom.yBohr());
    final SparseTaylorPolynomial cz = context.variable(8, thirdAtom.zBohr());
    final SparseTaylorPolynomial dx = context.variable(9, fourthAtom.xBohr());
    final SparseTaylorPolynomial dy = context.variable(10, fourthAtom.yBohr());
    final SparseTaylorPolynomial dz = context.variable(11, fourthAtom.zBohr());
    final double firstExponentSum = firstExponent + secondExponent;
    final double secondExponentSum = thirdExponent + fourthExponent;
    final double firstReduced = firstExponent * secondExponent / firstExponentSum;
    final double secondReduced = thirdExponent * fourthExponent / secondExponentSum;
    final double productReduced =
        firstExponentSum * secondExponentSum / (firstExponentSum + secondExponentSum);
    final SparseTaylorPolynomial firstProductX =
        ax.scale(firstExponent).add(bx.scale(secondExponent)).scale(1.0 / firstExponentSum);
    final SparseTaylorPolynomial firstProductY =
        ay.scale(firstExponent).add(by.scale(secondExponent)).scale(1.0 / firstExponentSum);
    final SparseTaylorPolynomial firstProductZ =
        az.scale(firstExponent).add(bz.scale(secondExponent)).scale(1.0 / firstExponentSum);
    final SparseTaylorPolynomial secondProductX =
        cx.scale(thirdExponent).add(dx.scale(fourthExponent)).scale(1.0 / secondExponentSum);
    final SparseTaylorPolynomial secondProductY =
        cy.scale(thirdExponent).add(dy.scale(fourthExponent)).scale(1.0 / secondExponentSum);
    final SparseTaylorPolynomial secondProductZ =
        cz.scale(thirdExponent).add(dz.scale(fourthExponent)).scale(1.0 / secondExponentSum);
    final SparseTaylorPolynomial abSquared =
        ax.subtract(bx).square().add(ay.subtract(by).square()).add(az.subtract(bz).square());
    final SparseTaylorPolynomial cdSquared =
        cx.subtract(dx).square().add(cy.subtract(dy).square()).add(cz.subtract(dz).square());
    final SparseTaylorPolynomial pqSquared =
        firstProductX
            .subtract(secondProductX)
            .square()
            .add(firstProductY.subtract(secondProductY).square())
            .add(firstProductZ.subtract(secondProductZ).square());
    return abSquared
        .scale(-firstReduced)
        .add(cdSquared.scale(-secondReduced))
        .exp()
        .multiply(
            CartesianGaussianPrimitiveIntegralKernel.boysZeroSparse(
                pqSquared.scale(productReduced)))
        .scale(
            TWO_PI_TO_POWER_FIVE_HALVES
                / (firstExponentSum
                    * secondExponentSum
                    * Math.sqrt(firstExponentSum + secondExponentSum)));
  }

  private static TaylorPolynomial boysZero(final TaylorPolynomial value) {
    final double scalar = value.scalar();
    final double[] boysValues =
        CartesianGaussianPrimitiveIntegralKernel.boysValues(value.context().maxOrder(), scalar);
    final TaylorPolynomial delta = value.withoutScalar();
    TaylorPolynomial result = value.context().zero();
    TaylorPolynomial deltaPower = value.context().constant(1.0);
    double factorial = 1.0;
    for (int order = 0; order <= value.context().maxOrder(); ++order) {
      final double derivative = ((order & 1) == 0 ? 1.0 : -1.0) * boysValues[order];
      result = result.add(deltaPower.scale(derivative / factorial));
      deltaPower = deltaPower.multiply(delta);
      factorial *= (double) (order + 1);
    }
    return result;
  }

  private static SparseTaylorPolynomial boysZeroSparse(final SparseTaylorPolynomial value) {
    final double scalar = value.scalar();
    final double[] boysValues =
        CartesianGaussianPrimitiveIntegralKernel.boysValues(value.context().maxOrder(), scalar);
    final SparseTaylorPolynomial delta = value.withoutScalar();
    SparseTaylorPolynomial result = value.context().zero();
    SparseTaylorPolynomial deltaPower = value.context().constant(1.0);
    double factorial = 1.0;
    for (int order = 0; order <= value.context().maxOrder(); ++order) {
      final double derivative = ((order & 1) == 0 ? 1.0 : -1.0) * boysValues[order];
      result = result.add(deltaPower.scale(derivative / factorial));
      deltaPower = deltaPower.multiply(delta);
      factorial *= (double) (order + 1);
    }
    return result;
  }

  private static double[] boysValues(final int maxOrder, final double value) {
    if (value < 0.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException("Boys function argument must be finite and non-negative.");
    }
    if (value < BOYS_SERIES_LIMIT) {
      return CartesianGaussianPrimitiveIntegralKernel.boysGaussLegendreValues(maxOrder, value);
    }
    final double[] values = new double[maxOrder + 1];
    values[0] =
        0.5
            * Math.sqrt(Math.PI / value)
            * CartesianGaussianPrimitiveIntegralKernel.erf(Math.sqrt(value));
    final double exponential = Math.exp(-value);
    for (int n = 0; n < maxOrder; ++n) {
      values[n + 1] = ((double) (2 * n + 1) * values[n] - exponential) / (2.0 * value);
    }
    return values;
  }

  private static double[] boysGaussLegendreValues(final int maxOrder, final double value) {
    final double[] values = new double[maxOrder + 1];
    for (int i = 0; i < BOYS_GL64_NODES.length; ++i) {
      final double node = BOYS_GL64_NODES[i];
      final double nodeSquared = node * node;
      final double weighted = BOYS_GL64_WEIGHTS[i] * Math.exp(-value * nodeSquared);
      double power = 1.0;
      for (int order = 0; order <= maxOrder; ++order) {
        values[order] += weighted * power;
        power *= nodeSquared;
      }
    }
    return values;
  }

  private static double boys(final int order, final double value) {
    if (value < 0.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException("Boys function argument must be finite and non-negative.");
    }
    if (value < BOYS_SERIES_LIMIT) {
      return CartesianGaussianPrimitiveIntegralKernel.boysSeries(order, value);
    }
    double current =
        0.5
            * Math.sqrt(Math.PI / value)
            * CartesianGaussianPrimitiveIntegralKernel.erf(Math.sqrt(value));
    final double exponential = Math.exp(-value);
    for (int n = 0; n < order; ++n) {
      current = ((double) (2 * n + 1) * current - exponential) / (2.0 * value);
    }
    return current;
  }

  private static double[] boysSeriesValues(final int maxOrder, final double value) {
    final double[] sums = new double[maxOrder + 1];
    final double[] compensations = new double[maxOrder + 1];
    double term = 1.0;
    for (int order = 0; order <= maxOrder; ++order) {
      sums[order] = term / (double) (2 * order + 1);
    }
    for (int k = 1; k < 256; ++k) {
      term *= -value / (double) k;
      double maxContribution = 0.0;
      for (int order = 0; order <= maxOrder; ++order) {
        final double next = term / (double) (2 * order + 2 * k + 1);
        final double corrected = next - compensations[order];
        final double updated = sums[order] + corrected;
        compensations[order] = (updated - sums[order]) - corrected;
        sums[order] = updated;
        maxContribution = Math.max(maxContribution, Math.abs(next));
      }
      if (maxContribution <= 1.0E-16) {
        break;
      }
    }
    return sums;
  }

  private static double boysSeries(final int order, final double value) {
    final double firstDenominator = (double) (2 * order + 1);
    double term = 1.0;
    double sum = term / firstDenominator;
    double compensation = 0.0;
    for (int k = 1; k < 256; ++k) {
      term *= -value / (double) k;
      final double next = term / (double) (2 * order + 2 * k + 1);
      final double corrected = next - compensation;
      final double updated = sum + corrected;
      compensation = (updated - sum) - corrected;
      sum = updated;
      if (Math.abs(next) <= 1.0E-16 * Math.max(1.0, Math.abs(sum))) {
        break;
      }
    }
    return sum;
  }

  private static double erf(final double value) {
    final double sign = value < 0.0 ? -1.0 : 1.0;
    final double x = Math.abs(value);
    final double t = 1.0 / (1.0 + 0.3275911 * x);
    final double polynomial =
        (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t
                + 0.254829592)
            * t;
    return sign * (1.0 - polynomial * Math.exp(-x * x));
  }

  private static double derivativeScale(
      final double exponent, final GaussianAngularMomentum angularMomentum) {
    return CartesianGaussianPrimitiveIntegralKernel.inversePower(
        2.0 * exponent, angularMomentum.totalPower());
  }

  private static double kineticAxis(
      final int firstPower,
      final int secondPower,
      final double firstExponent,
      final double secondExponent,
      final double firstCenter,
      final double secondCenter) {
    double value =
        -2.0
            * secondExponent
            * (double) (2 * secondPower + 1)
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstPower, secondPower, firstExponent, secondExponent, firstCenter, secondCenter);
    value +=
        4.0
            * secondExponent
            * secondExponent
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstPower,
                secondPower + 2,
                firstExponent,
                secondExponent,
                firstCenter,
                secondCenter);
    if (secondPower >= 2) {
      value +=
          (double) (secondPower * (secondPower - 1))
              * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                  firstPower,
                  secondPower - 2,
                  firstExponent,
                  secondExponent,
                  firstCenter,
                  secondCenter);
    }
    return value;
  }

  private static double overlapAxis(
      final int firstPower,
      final int secondPower,
      final double firstExponent,
      final double secondExponent,
      final double firstCenter,
      final double secondCenter) {
    final double exponentSum = firstExponent + secondExponent;
    final double reducedExponent = firstExponent * secondExponent / exponentSum;
    final double productCenter =
        (firstExponent * firstCenter + secondExponent * secondCenter) / exponentSum;
    final double firstShift = productCenter - firstCenter;
    final double secondShift = productCenter - secondCenter;
    final double exponential =
        Math.exp(
            -reducedExponent
                * CartesianGaussianPrimitiveIntegralKernel.square(firstCenter - secondCenter));
    double value = 0.0;
    for (int i = 0; i <= firstPower; ++i) {
      final double firstCoefficient =
          (double) CartesianGaussianPrimitiveIntegralKernel.binomial(firstPower, i)
              * CartesianGaussianPrimitiveIntegralKernel.power(firstShift, firstPower - i);
      for (int j = 0; j <= secondPower; ++j) {
        final int momentPower = i + j;
        if ((momentPower & 1) != 0) {
          continue;
        }
        value +=
            firstCoefficient
                * (double) CartesianGaussianPrimitiveIntegralKernel.binomial(secondPower, j)
                * CartesianGaussianPrimitiveIntegralKernel.power(secondShift, secondPower - j)
                * CartesianGaussianPrimitiveIntegralKernel.evenGaussianMoment(
                    momentPower, exponentSum);
      }
    }
    return exponential * value;
  }

  private static double axisMoment(
      final int requestedAxis,
      final int currentAxis,
      final int firstPower,
      final int secondPower,
      final double firstExponent,
      final double secondExponent,
      final double firstCenter,
      final double secondCenter) {
    if (requestedAxis != currentAxis) {
      return CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
          firstPower, secondPower, firstExponent, secondExponent, firstCenter, secondCenter);
    }
    return firstCenter
            * CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
                firstPower, secondPower, firstExponent, secondExponent, firstCenter, secondCenter)
        + CartesianGaussianPrimitiveIntegralKernel.overlapAxis(
            firstPower + 1, secondPower, firstExponent, secondExponent, firstCenter, secondCenter);
  }

  private static double evenGaussianMoment(final int power, final double exponent) {
    int doubleFactorial = 1;
    for (int value = power - 1; value > 1; value -= 2) {
      doubleFactorial *= value;
    }
    final int halfPower = power / 2;
    return (double) doubleFactorial
        * Math.sqrt(Math.PI)
        / (Math.pow(2.0, halfPower) * Math.pow(exponent, halfPower + 0.5));
  }

  private static int binomial(final int n, final int k) {
    if (k < 0 || k > n) {
      return 0;
    }
    if (k == 0 || k == n) {
      return 1;
    }
    int value = 1;
    for (int i = 1; i <= k; ++i) {
      value = value * (n - i + 1) / i;
    }
    return value;
  }

  private static int factorial(final int value) {
    int result = 1;
    for (int i = 2; i <= value; ++i) {
      result *= i;
    }
    return result;
  }

  private static double power(final double value, final int exponent) {
    double result = 1.0;
    for (int i = 0; i < exponent; ++i) {
      result *= value;
    }
    return result;
  }

  private static double inversePower(final double value, final int exponent) {
    double result = 1.0;
    for (int i = 0; i < exponent; ++i) {
      result /= value;
    }
    return result;
  }

  private static double square(final double value) {
    return value * value;
  }

  private static void requireFinitePositive(final double value, final String subject) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(subject + " must be finite and positive.");
    }
  }

  private static void requireAngularMomentum(
      final GaussianAngularMomentum angularMomentum, final String subject) {
    if (angularMomentum == null) {
      throw new IllegalArgumentException(subject + " angular momentum must not be null.");
    }
  }

  private static void requireAtom(final IntegralAtom atom, final String subject) {
    if (atom == null) {
      throw new IllegalArgumentException(subject + " must not be null.");
    }
  }

  private static final class TaylorContext {

    private final int variableCount;
    private final int maxOrder;
    private final int[][] exponents;
    private final int[] degrees;
    private final double[] derivativeFactors;
    private final int[][] multiplyIndexes;

    private TaylorContext(
        final int variableCount,
        final int maxOrder,
        final int[][] exponents,
        final int[] degrees,
        final double[] derivativeFactors,
        final int[][] multiplyIndexes) {
      this.variableCount = variableCount;
      this.maxOrder = maxOrder;
      this.exponents = exponents;
      this.degrees = degrees;
      this.derivativeFactors = derivativeFactors;
      this.multiplyIndexes = multiplyIndexes;
    }

    static TaylorContext create(final int variableCount, final int maxOrder) {
      final int count = TaylorContext.countMonomials(variableCount, maxOrder);
      final int[][] exponents = new int[count][variableCount];
      final int[] degrees = new int[count];
      final int[] cursor = new int[] {0};
      TaylorContext.fillMonomials(exponents, degrees, cursor, new int[variableCount], 0, maxOrder);
      final Map<Long, Integer> indexByKey = new HashMap<>(count * 2);
      for (int i = 0; i < count; ++i) {
        indexByKey.put(Long.valueOf(TaylorContext.key(exponents[i])), Integer.valueOf(i));
      }
      final double[] derivativeFactors = new double[count];
      for (int i = 0; i < count; ++i) {
        double factor = 1.0;
        for (int variable = 0; variable < variableCount; ++variable) {
          factor *= (double) TaylorContext.factorial(exponents[i][variable]);
        }
        derivativeFactors[i] = factor;
      }
      final int[][] multiplyIndexes = new int[count][count];
      final int[] sum = new int[variableCount];
      for (int i = 0; i < count; ++i) {
        for (int j = 0; j < count; ++j) {
          int degree = 0;
          for (int variable = 0; variable < variableCount; ++variable) {
            sum[variable] = exponents[i][variable] + exponents[j][variable];
            degree += sum[variable];
          }
          multiplyIndexes[i][j] =
              degree > maxOrder
                  ? -1
                  : indexByKey.get(Long.valueOf(TaylorContext.key(sum))).intValue();
        }
      }
      return new TaylorContext(
          variableCount, maxOrder, exponents, degrees, derivativeFactors, multiplyIndexes);
    }

    TaylorPolynomial zero() {
      return TaylorPolynomial.of(this, new double[this.exponents.length]);
    }

    TaylorPolynomial constant(final double value) {
      final double[] coefficients = new double[this.exponents.length];
      coefficients[0] = value;
      return TaylorPolynomial.of(this, coefficients);
    }

    TaylorPolynomial variable(final int variable, final double value) {
      final double[] coefficients = new double[this.exponents.length];
      coefficients[0] = value;
      final int[] exponent = new int[this.variableCount];
      exponent[variable] = 1;
      coefficients[TaylorContext.indexOf(this.exponents, exponent)] = 1.0;
      return TaylorPolynomial.of(this, coefficients);
    }

    int maxOrder() {
      return this.maxOrder;
    }

    int indexOf(final int... exponents) {
      return TaylorContext.indexOf(this.exponents, exponents);
    }

    private static int countMonomials(final int variableCount, final int maxOrder) {
      return TaylorContext.binomial(variableCount + maxOrder, maxOrder);
    }

    private static void fillMonomials(
        final int[][] exponents,
        final int[] degrees,
        final int[] cursor,
        final int[] current,
        final int variable,
        final int remaining) {
      if (variable == current.length) {
        final int index = cursor[0]++;
        int degree = 0;
        for (int i = 0; i < current.length; ++i) {
          exponents[index][i] = current[i];
          degree += current[i];
        }
        degrees[index] = degree;
        return;
      }
      for (int power = 0; power <= remaining; ++power) {
        current[variable] = power;
        TaylorContext.fillMonomials(
            exponents, degrees, cursor, current, variable + 1, remaining - power);
      }
      current[variable] = 0;
    }

    private static int indexOf(final int[][] exponents, final int[] target) {
      for (int i = 0; i < exponents.length; ++i) {
        boolean same = true;
        for (int j = 0; j < target.length; ++j) {
          if (exponents[i][j] != target[j]) {
            same = false;
            break;
          }
        }
        if (same) {
          return i;
        }
      }
      return -1;
    }

    private static long key(final int[] exponents) {
      long key = 0L;
      for (int i = 0; i < exponents.length; ++i) {
        key |= (long) exponents[i] << (i * 4);
      }
      return key;
    }

    private static int binomial(final int n, final int k) {
      int value = 1;
      for (int i = 1; i <= k; ++i) {
        value = value * (n - i + 1) / i;
      }
      return value;
    }

    private static int factorial(final int value) {
      int result = 1;
      for (int i = 2; i <= value; ++i) {
        result *= i;
      }
      return result;
    }
  }

  private static final class TaylorPolynomial {

    private final TaylorContext context;
    private final double[] coefficients;
    private final int[] nonZeroIndexes;

    private TaylorPolynomial(
        final TaylorContext context, final double[] coefficients, final int[] nonZeroIndexes) {
      this.context = context;
      this.coefficients = coefficients;
      this.nonZeroIndexes = nonZeroIndexes;
    }

    static TaylorPolynomial of(final TaylorContext context, final double[] coefficients) {
      int count = 0;
      for (int i = 0; i < coefficients.length; ++i) {
        if (coefficients[i] != 0.0) {
          ++count;
        }
      }
      final int[] nonZeroIndexes = new int[count];
      int cursor = 0;
      for (int i = 0; i < coefficients.length; ++i) {
        if (coefficients[i] != 0.0) {
          nonZeroIndexes[cursor++] = i;
        }
      }
      return new TaylorPolynomial(context, coefficients, nonZeroIndexes);
    }

    TaylorContext context() {
      return this.context;
    }

    double scalar() {
      return this.coefficients[0];
    }

    TaylorPolynomial withoutScalar() {
      final double[] result = this.coefficients.clone();
      result[0] = 0.0;
      return TaylorPolynomial.of(this.context, result);
    }

    TaylorPolynomial add(final TaylorPolynomial other) {
      final double[] result = new double[this.coefficients.length];
      for (int i = 0; i < result.length; ++i) {
        result[i] = this.coefficients[i] + other.coefficients[i];
      }
      return TaylorPolynomial.of(this.context, result);
    }

    TaylorPolynomial subtract(final TaylorPolynomial other) {
      final double[] result = new double[this.coefficients.length];
      for (int i = 0; i < result.length; ++i) {
        result[i] = this.coefficients[i] - other.coefficients[i];
      }
      return TaylorPolynomial.of(this.context, result);
    }

    TaylorPolynomial scale(final double factor) {
      final double[] result = new double[this.coefficients.length];
      for (int i = 0; i < result.length; ++i) {
        result[i] = factor * this.coefficients[i];
      }
      return TaylorPolynomial.of(this.context, result);
    }

    TaylorPolynomial square() {
      return this.multiply(this);
    }

    TaylorPolynomial multiply(final TaylorPolynomial other) {
      final double[] result = new double[this.coefficients.length];
      for (int left = 0; left < this.nonZeroIndexes.length; ++left) {
        final int i = this.nonZeroIndexes[left];
        final double first = this.coefficients[i];
        for (int right = 0; right < other.nonZeroIndexes.length; ++right) {
          final int j = other.nonZeroIndexes[right];
          final double second = other.coefficients[j];
          final int targetIndex = this.context.multiplyIndexes[i][j];
          if (targetIndex >= 0) {
            result[targetIndex] += first * second;
          }
        }
      }
      return TaylorPolynomial.of(this.context, result);
    }

    TaylorPolynomial exp() {
      final double scalar = this.scalar();
      final TaylorPolynomial delta = this.withoutScalar();
      TaylorPolynomial result = this.context.zero();
      TaylorPolynomial deltaPower = this.context.constant(1.0);
      double factorial = 1.0;
      final double scalarExponential = Math.exp(scalar);
      for (int order = 0; order <= this.context.maxOrder; ++order) {
        result = result.add(deltaPower.scale(scalarExponential / factorial));
        deltaPower = deltaPower.multiply(delta);
        factorial *= (double) (order + 1);
      }
      return result;
    }

    double derivative(final int... exponents) {
      final int index = this.context.indexOf(exponents);
      if (index < 0) {
        return 0.0;
      }
      return this.coefficients[index] * this.context.derivativeFactors[index];
    }
  }

  private static final class SparseTaylorContext {

    private static final int EXPONENT_BITS = 5;
    private static final int EXPONENT_MASK = 31;

    private final int variableCount;
    private final int maxOrder;
    private final int[] maxExponents;

    private SparseTaylorContext(
        final int variableCount, final int maxOrder, final int[] maxExponents) {
      this.variableCount = variableCount;
      this.maxOrder = maxOrder;
      this.maxExponents = maxExponents;
    }

    static SparseTaylorContext create(
        final int variableCount, final int maxOrder, final int[] maxExponents) {
      if (variableCount <= 0) {
        throw new IllegalArgumentException("Sparse Taylor variable count must be positive.");
      }
      if (maxOrder < 0) {
        throw new IllegalArgumentException("Sparse Taylor order must not be negative.");
      }
      if (maxExponents == null || maxExponents.length != variableCount) {
        throw new IllegalArgumentException(
            "Sparse Taylor max exponents must match variable count.");
      }
      if (variableCount * EXPONENT_BITS > Long.SIZE) {
        throw new IllegalArgumentException(
            "Sparse Taylor context cannot encode this many variables.");
      }
      final int[] checkedMaxExponents = maxExponents.clone();
      int checkedMaxOrder = 0;
      for (int i = 0; i < checkedMaxExponents.length; ++i) {
        if (checkedMaxExponents[i] < 0 || checkedMaxExponents[i] > EXPONENT_MASK) {
          throw new IllegalArgumentException("Sparse Taylor max exponent is out of range.");
        }
        checkedMaxOrder += checkedMaxExponents[i];
      }
      if (checkedMaxOrder != maxOrder) {
        throw new IllegalArgumentException(
            "Sparse Taylor max order must equal target derivative order.");
      }
      return new SparseTaylorContext(variableCount, maxOrder, checkedMaxExponents);
    }

    int maxOrder() {
      return this.maxOrder;
    }

    SparseTaylorPolynomial zero() {
      return SparseTaylorPolynomial.of(this, Map.of());
    }

    SparseTaylorPolynomial constant(final double value) {
      if (value == 0.0) {
        return this.zero();
      }
      return SparseTaylorPolynomial.of(this, Map.of(Long.valueOf(0L), Double.valueOf(value)));
    }

    SparseTaylorPolynomial variable(final int variable, final double value) {
      if (variable < 0 || variable >= this.variableCount) {
        throw new IllegalArgumentException("Sparse Taylor variable index is out of range.");
      }
      final Map<Long, Double> coefficients = new LinkedHashMap<>(2);
      if (value != 0.0) {
        coefficients.put(Long.valueOf(0L), Double.valueOf(value));
      }
      if (this.maxExponents[variable] > 0) {
        coefficients.put(Long.valueOf(1L << (variable * EXPONENT_BITS)), Double.valueOf(1.0));
      }
      return SparseTaylorPolynomial.of(this, coefficients);
    }

    long key(final int[] exponents) {
      if (exponents.length != this.variableCount) {
        throw new IllegalArgumentException("Sparse Taylor derivative dimension mismatch.");
      }
      long key = 0L;
      for (int i = 0; i < exponents.length; ++i) {
        final int exponent = exponents[i];
        if (exponent < 0 || exponent > EXPONENT_MASK) {
          throw new IllegalArgumentException("Sparse Taylor exponent is out of range.");
        }
        if (exponent > this.maxExponents[i]) {
          throw new IllegalArgumentException(
              "Sparse Taylor exponent exceeds target derivative bound.");
        }
        key |= (long) exponent << (i * EXPONENT_BITS);
      }
      return key;
    }

    long multiplyKey(final long first, final long second) {
      long key = 0L;
      int degree = 0;
      for (int i = 0; i < this.variableCount; ++i) {
        final int exponent =
            (int) ((first >> (i * EXPONENT_BITS)) & EXPONENT_MASK)
                + (int) ((second >> (i * EXPONENT_BITS)) & EXPONENT_MASK);
        if (exponent > EXPONENT_MASK) {
          return -1L;
        }
        if (exponent > this.maxExponents[i]) {
          return -1L;
        }
        degree += exponent;
        if (degree > this.maxOrder) {
          return -1L;
        }
        key |= (long) exponent << (i * EXPONENT_BITS);
      }
      return key;
    }

    double derivativeFactor(final long key) {
      double factor = 1.0;
      for (int i = 0; i < this.variableCount; ++i) {
        final int exponent = (int) ((key >> (i * EXPONENT_BITS)) & EXPONENT_MASK);
        factor *= (double) CartesianGaussianPrimitiveIntegralKernel.factorial(exponent);
      }
      return factor;
    }
  }

  private static final class SparseTaylorPolynomial {

    private final SparseTaylorContext context;
    private final Map<Long, Double> coefficients;

    private SparseTaylorPolynomial(
        final SparseTaylorContext context, final Map<Long, Double> coefficients) {
      this.context = context;
      this.coefficients = coefficients;
    }

    static SparseTaylorPolynomial of(
        final SparseTaylorContext context, final Map<Long, Double> coefficients) {
      final Map<Long, Double> checked = new LinkedHashMap<>(coefficients.size());
      for (final Map.Entry<Long, Double> entry : coefficients.entrySet()) {
        final double value = entry.getValue().doubleValue();
        if (value != 0.0) {
          checked.put(entry.getKey(), Double.valueOf(value));
        }
      }
      return new SparseTaylorPolynomial(context, checked);
    }

    SparseTaylorContext context() {
      return this.context;
    }

    double scalar() {
      final Double value = this.coefficients.get(Long.valueOf(0L));
      return value == null ? 0.0 : value.doubleValue();
    }

    SparseTaylorPolynomial withoutScalar() {
      final Map<Long, Double> result = new LinkedHashMap<>(this.coefficients);
      result.remove(Long.valueOf(0L));
      return SparseTaylorPolynomial.of(this.context, result);
    }

    SparseTaylorPolynomial add(final SparseTaylorPolynomial other) {
      final Map<Long, Double> result = new LinkedHashMap<>(this.coefficients);
      for (final Map.Entry<Long, Double> entry : other.coefficients.entrySet()) {
        result.merge(entry.getKey(), entry.getValue(), Double::sum);
      }
      return SparseTaylorPolynomial.of(this.context, result);
    }

    SparseTaylorPolynomial subtract(final SparseTaylorPolynomial other) {
      final Map<Long, Double> result = new LinkedHashMap<>(this.coefficients);
      for (final Map.Entry<Long, Double> entry : other.coefficients.entrySet()) {
        result.merge(entry.getKey(), Double.valueOf(-entry.getValue().doubleValue()), Double::sum);
      }
      return SparseTaylorPolynomial.of(this.context, result);
    }

    SparseTaylorPolynomial scale(final double factor) {
      if (factor == 0.0) {
        return this.context.zero();
      }
      final Map<Long, Double> result = new LinkedHashMap<>(this.coefficients.size());
      for (final Map.Entry<Long, Double> entry : this.coefficients.entrySet()) {
        result.put(entry.getKey(), Double.valueOf(factor * entry.getValue().doubleValue()));
      }
      return SparseTaylorPolynomial.of(this.context, result);
    }

    SparseTaylorPolynomial square() {
      return this.multiply(this);
    }

    SparseTaylorPolynomial multiply(final SparseTaylorPolynomial other) {
      final Map<Long, Double> result = new LinkedHashMap<>();
      for (final Map.Entry<Long, Double> left : this.coefficients.entrySet()) {
        for (final Map.Entry<Long, Double> right : other.coefficients.entrySet()) {
          final long targetKey =
              this.context.multiplyKey(left.getKey().longValue(), right.getKey().longValue());
          if (targetKey >= 0L) {
            final double value = left.getValue().doubleValue() * right.getValue().doubleValue();
            result.merge(Long.valueOf(targetKey), Double.valueOf(value), Double::sum);
          }
        }
      }
      return SparseTaylorPolynomial.of(this.context, result);
    }

    SparseTaylorPolynomial exp() {
      final double scalar = this.scalar();
      final SparseTaylorPolynomial delta = this.withoutScalar();
      SparseTaylorPolynomial result = this.context.zero();
      SparseTaylorPolynomial deltaPower = this.context.constant(1.0);
      double factorial = 1.0;
      final double scalarExponential = Math.exp(scalar);
      for (int order = 0; order <= this.context.maxOrder(); ++order) {
        result = result.add(deltaPower.scale(scalarExponential / factorial));
        deltaPower = deltaPower.multiply(delta);
        factorial *= (double) (order + 1);
      }
      return result;
    }

    double derivative(final int... exponents) {
      final long key = this.context.key(exponents);
      final Double coefficient = this.coefficients.get(Long.valueOf(key));
      return coefficient == null
          ? 0.0
          : coefficient.doubleValue() * this.context.derivativeFactor(key);
    }
  }
}
