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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShellAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.basis.GaussianBasisTextParser;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.basis.StructuredGaussianBasisAdapter;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianShellType;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;

public final class GaussianBasisLibraryCapabilityTest {

  private static final double PRIMITIVE_EXPONENT = 0.7;

  @Test
  public void sto3gCapabilityAcceptsHydrogenAndHeliumOneSShells() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(2, 0.0, 0.0, 1.4)), 0);

    assertNull(GaussianBasisLibrary.unsupportedSto3gReason(molecule));
    assertEquals(2, GaussianBasisLibrary.sto3g(molecule).orbitalCount());
  }

  @Test
  public void sto3gCapabilityBuildsFirstRowMinimalBasis() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0)), 0);

    assertNull(GaussianBasisLibrary.unsupportedSto3gReason(molecule));
    assertEquals(5, GaussianBasisLibrary.sto3g(molecule).orbitalCount());
    assertNull(
        GaussianIntegralEngine.unsupportedAngularMomentumReason(
            GaussianBasisLibrary.sto3g(molecule)));
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));

    assertEquals(5, tensors.orbitalCount());
    assertEquals(-17.76161938898472, tensors.coreHamiltonianAt(0, 0), 1.0E-7);
    assertEquals(-4.717516512260984, tensors.coreHamiltonianAt(1, 1), 1.0E-7);
    assertEquals(-3.670492636861688, tensors.coreHamiltonianAt(2, 2), 1.0E-7);
    assertEquals(0.6728327205216605, tensors.electronRepulsionAt(2, 2, 2, 2), 1.0E-7);
    assertEquals(0.600295437655869, tensors.electronRepulsionAt(2, 2, 3, 3), 1.0E-7);
    assertEquals(0.03626864143289575, tensors.electronRepulsionAt(2, 3, 2, 3), 1.0E-7);
    assertTrue(Double.isFinite(tensors.coreHamiltonianAt(0, 0)));
    assertEquals(tensors.overlapAt(2, 3), tensors.overlapAt(3, 2), 1.0E-12);
    assertEquals(
        tensors.electronRepulsionAt(1, 2, 3, 4), tensors.electronRepulsionAt(3, 4, 1, 2), 1.0E-10);
  }

  @Test
  public void six31gCapabilityBuildsHydrogenOrganicFirstRowAndThirdPeriodSplitValenceBasis() {
    final IntegralMolecule hydrogen =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, 1.4)), 0);
    final IntegralMolecule organicFirstRow =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(6, 0.0, 0.0, 0.0),
                IntegralAtom.of(7, 0.0, 0.0, 2.0),
                IntegralAtom.of(8, 0.0, 0.0, 4.0),
                IntegralAtom.of(9, 0.0, 0.0, 6.0)),
            0);
    final IntegralMolecule thirdPeriodOrganic =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(15, 0.0, 0.0, 0.0),
                IntegralAtom.of(16, 0.0, 0.0, 2.0),
                IntegralAtom.of(17, 0.0, 0.0, 4.0)),
            0);

    assertNull(GaussianBasisLibrary.unsupportedSix31gReason(hydrogen));
    assertNull(GaussianBasisLibrary.unsupportedSix31gReason(organicFirstRow));
    assertNull(GaussianBasisLibrary.unsupportedSix31gReason(thirdPeriodOrganic));
    assertEquals(4, GaussianBasisLibrary.six31g(hydrogen).orbitalCount());
    assertEquals(36, GaussianBasisLibrary.six31g(organicFirstRow).orbitalCount());
    assertEquals(39, GaussianBasisLibrary.six31g(thirdPeriodOrganic).orbitalCount());
    assertNull(
        GaussianIntegralCapability.unsupportedFullTensorReason(
            GaussianBasisLibrary.six31g(hydrogen)));
    assertEquals(
        GaussianIntegralCapabilityStatus.SUPPORTED,
        GaussianIntegralCapability.preflightFullTensor(GaussianBasisLibrary.six31g(hydrogen))
            .status());
  }

  @Test
  public void cartesianShellExpansionSupportsHighAngularMomentumModelBeforeKernelEvaluation() {
    assertEquals(1, GaussianAngularMomentum.cartesianShell(GaussianShellType.S).size());
    assertEquals(3, GaussianAngularMomentum.cartesianShell(GaussianShellType.P).size());
    assertEquals(6, GaussianAngularMomentum.cartesianShell(GaussianShellType.D).size());
    assertEquals(10, GaussianAngularMomentum.cartesianShell(GaussianShellType.F).size());
    assertEquals(15, GaussianAngularMomentum.cartesianShell(GaussianShellType.G).size());
    assertEquals(21, GaussianAngularMomentum.cartesianShell(GaussianShellType.H).size());
    assertEquals(28, GaussianAngularMomentum.cartesianShell(GaussianShellType.I).size());
    assertEquals(36, GaussianAngularMomentum.cartesianShell(GaussianShellType.K).size());
    assertEquals(
        GaussianShellType.G.cartesianFunctionCount(),
        GaussianBasisLibrary.cartesianShellOrbitals(
                0, GaussianShellType.G, new double[] {PRIMITIVE_EXPONENT}, new double[] {1.0})
            .size());
  }

  @Test
  public void gaussianBasisSetReportsShellHistogramAndMaxAngularMomentum() {
    final List<ContractedGaussianOrbital> dOrbitals =
        GaussianBasisLibrary.cartesianShellOrbitals(
            0, GaussianShellType.D, new double[] {PRIMITIVE_EXPONENT}, new double[] {1.0});
    final GaussianBasisSet basisSet = GaussianBasisSet.of("custom-d-shell", dOrbitals);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();

    assertEquals(2, basisSet.maxAngularMomentum());
    assertEquals(Integer.valueOf(6), histogram.get(GaussianShellType.D));
    assertNull(GaussianIntegralEngine.unsupportedAngularMomentumReason(basisSet));
  }

  @Test
  public void fullEriCapabilityUsesPackedResourceCountInsteadOfDenseEquivalent() {
    final List<ContractedGaussianOrbital> orbitals =
        GaussianBasisLibraryCapabilityTest.repeatedSOrbitals(54);
    final GaussianBasisSet basisSet = GaussianBasisSet.of("packed-safe-s-basis", orbitals);

    final String reason = GaussianIntegralCapability.unsupportedFullTensorReason(basisSet);
    final GaussianIntegralPreflight preflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);

    assertNull(reason);
    assertTrue(preflight.supported());
    assertEquals(54, preflight.orbitalCount());
    assertEquals(0, preflight.maxAngularMomentum());
    assertEquals(0, preflight.maxFullEriDerivativeOrder());
    assertEquals(8_503_056L, preflight.denseEriElementCount());
    assertEquals(1_103_355L, preflight.packedEriElementCount());
  }

  @Test
  public void fullEriCapabilityRejectsPackedResourceUnsafeOrbitalCountBeforeAllocation() {
    final List<ContractedGaussianOrbital> orbitals =
        GaussianBasisLibraryCapabilityTest.repeatedSOrbitals(90);
    final GaussianBasisSet basisSet =
        GaussianBasisSet.of("resource-unsafe-packed-s-basis", orbitals);

    final String reason = GaussianIntegralCapability.unsupportedFullTensorReason(basisSet);
    final GaussianIntegralPreflight preflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);

    assertNotNull(reason);
    assertTrue(reason.contains("requires 8386560 packed double elements"));
    assertTrue(reason.contains("dense equivalent is 65610000"));
    assertTrue(reason.contains("in-memory limit"));
    assertEquals(GaussianIntegralCapabilityStatus.UNSUPPORTED_RESOURCE_LIMIT, preflight.status());
    assertTrue(preflight.resourceLimited());
    assertEquals(90, preflight.orbitalCount());
    assertEquals(0, preflight.maxAngularMomentum());
    assertEquals(0, preflight.maxFullEriDerivativeOrder());
    assertEquals(65_610_000L, preflight.denseEriElementCount());
    assertEquals(8_386_560L, preflight.packedEriElementCount());
    assertTrue(preflight.requireSupportedReason().contains("in-memory limit"));
  }

  @Test
  public void normalizedDPrimitiveHasUnitSameCenterOverlap() {
    final CartesianGaussianPrimitiveIntegralKernel kernel =
        new CartesianGaussianPrimitiveIntegralKernel();
    final IntegralAtom atom = IntegralAtom.of(6, 0.0, 0.0, 0.0);
    final List<ContractedGaussianOrbital> orbitals =
        GaussianBasisLibrary.cartesianShellOrbitals(
            0, GaussianShellType.D, new double[] {PRIMITIVE_EXPONENT}, new double[] {1.0});

    for (int i = 0; i < orbitals.size(); ++i) {
      final ContractedGaussianOrbital orbital = orbitals.get(i);
      final GaussianPrimitive primitive = orbital.primitives().get(0);
      final double overlap =
          primitive.coefficient()
              * primitive.coefficient()
              * kernel.overlap(
                  PRIMITIVE_EXPONENT,
                  orbital.angularMomentum(),
                  atom,
                  PRIMITIVE_EXPONENT,
                  orbital.angularMomentum(),
                  atom);
      assertEquals(1.0, overlap, 1.0E-12, orbital.angularMomentum().label());
    }
  }

  @Test
  public void textParserBuildsCustomOrganicBasisWithSpAndDfgShells() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0), IntegralAtom.of(7, 0.0, 0.0, 2.0)), 0);
    final String source =
        String.join(
            "\n",
            "C S",
            "  71.61683735D+00  0.1543289673",
            "  13.04509632D+00  0.5353281423",
            "C SP",
            "  2.941249355  -0.09996722919  0.1559162750",
            "  0.6834830964  0.3995128261  0.6076837186",
            "C D",
            "  0.5500000000  1.0000000000",
            "N S",
            "  99.10616896  0.1543289673",
            "  18.05231239  0.5353281423",
            "N F",
            "  0.4300000000  1.0000000000",
            "N G",
            "  0.2900000000  1.0000000000",
            "****");

    final GaussianBasisSet basisSet =
        new GaussianBasisTextParser().parseForMolecule("custom-organic-dfg", source, molecule);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();

    assertEquals(37, basisSet.orbitalCount());
    assertEquals(4, basisSet.maxAngularMomentum());
    assertEquals(Integer.valueOf(3), histogram.get(GaussianShellType.S));
    assertEquals(Integer.valueOf(3), histogram.get(GaussianShellType.P));
    assertEquals(Integer.valueOf(6), histogram.get(GaussianShellType.D));
    assertEquals(Integer.valueOf(10), histogram.get(GaussianShellType.F));
    assertEquals(Integer.valueOf(15), histogram.get(GaussianShellType.G));
    assertNull(GaussianIntegralEngine.unsupportedAngularMomentumReason(basisSet));
  }

  @Test
  public void textParserBuildsBlockFormattedBasisWithHeavyAtomsDfgShellsAndScaleFactors() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(6, 0.0, 0.0, 0.0),
                IntegralAtom.of(7, 0.0, 0.0, 2.0),
                IntegralAtom.of(46, 3.0, 0.0, 0.0)),
            0);
    final String source =
        String.join(
            "\n",
            "BASIS",
            "C 0",
            "S 2 1.00",
            "  71.61683735D+00  0.1543289673",
            "  13.04509632D+00  0.5353281423",
            "SP 2 1.00",
            "  2.941249355  -0.09996722919  0.1559162750",
            "  0.6834830964  0.3995128261  0.6076837186",
            "D 1 1.00",
            "  0.5500000000  1.0000000000",
            "****",
            "N 0",
            "F 1 1.00",
            "  0.4300000000  1.0000000000",
            "G 1 1.00",
            "  0.2900000000  1.0000000000",
            "****",
            "Pd 0",
            "S 1 2.00",
            "  0.2500000000  1.0000000000",
            "****",
            "END");

    final GaussianBasisSet basisSet =
        new GaussianBasisTextParser()
            .parseForMolecule("block-organometallic-dfg", source, molecule);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();
    final ContractedGaussianOrbital palladiumOrbital =
        basisSet.orbitals().stream()
            .filter((final ContractedGaussianOrbital orbital) -> orbital.atomIndex() == 2)
            .findFirst()
            .orElseThrow();

    assertEquals(37, basisSet.orbitalCount());
    assertEquals(4, basisSet.maxAngularMomentum());
    assertEquals(Integer.valueOf(3), histogram.get(GaussianShellType.S));
    assertEquals(Integer.valueOf(3), histogram.get(GaussianShellType.P));
    assertEquals(Integer.valueOf(6), histogram.get(GaussianShellType.D));
    assertEquals(Integer.valueOf(10), histogram.get(GaussianShellType.F));
    assertEquals(Integer.valueOf(15), histogram.get(GaussianShellType.G));
    assertEquals(1.0, palladiumOrbital.primitives().get(0).exponent(), 1.0E-12);
  }

  @Test
  public void textParserBuildsHighAngularBasisRepresentationAndCapabilityReportsResourceBoundary() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(26, 0.0, 0.0, 0.0)), 0);
    final String source =
        String.join(
            "\n",
            "Fe 0",
            "H 1 1.00",
            "  0.2500000000  1.0000000000",
            "I 1 1.00",
            "  0.1500000000  1.0000000000",
            "K 1 1.00",
            "  0.0900000000  1.0000000000",
            "****");

    final GaussianBasisSet basisSet =
        new GaussianBasisTextParser().parseForMolecule("fe-hik-representation", source, molecule);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();

    assertEquals(85, basisSet.orbitalCount());
    assertEquals(7, basisSet.maxAngularMomentum());
    assertEquals(Integer.valueOf(21), histogram.get(GaussianShellType.H));
    assertEquals(Integer.valueOf(28), histogram.get(GaussianShellType.I));
    assertEquals(Integer.valueOf(36), histogram.get(GaussianShellType.K));
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);

    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, oneElectronPreflight.status());
    assertEquals(
        GaussianIntegralCapabilityStatus.UNSUPPORTED_RESOURCE_LIMIT, fullTensorPreflight.status());
    assertEquals(85, oneElectronPreflight.orbitalCount());
    assertEquals(7, oneElectronPreflight.maxAngularMomentum());
    assertEquals(28, fullTensorPreflight.maxFullEriDerivativeOrder());
    assertNull(GaussianIntegralCapability.unsupportedOneElectronReason(basisSet));
    assertTrue(
        GaussianIntegralCapability.unsupportedFullTensorReason(basisSet)
            .contains("packed double elements"));
  }

  @Test
  public void textParserUsesCorePeriodicTableForActinideAndSuperheavyElements() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(92, 0.0, 0.0, 0.0), IntegralAtom.of(118, 0.0, 0.0, 4.0)), 0);
    final String source =
        String.join(
            "\n",
            "U 0",
            "S 1 1.00",
            "  0.3200000000  1.0000000000",
            "****",
            "Og 0",
            "S 1 1.00",
            "  0.2100000000  1.0000000000",
            "****");

    final GaussianBasisSet basisSet =
        new GaussianBasisTextParser().parseForMolecule("heavy-element-s-basis", source, molecule);
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);

    assertEquals(2, basisSet.orbitalCount());
    assertEquals(0, basisSet.maxAngularMomentum());
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, oneElectronPreflight.status());
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, fullTensorPreflight.status());
    assertNull(GaussianIntegralCapability.unsupportedOneElectronReason(basisSet));
    assertNull(GaussianIntegralCapability.unsupportedFullTensorReason(basisSet));
  }

  @Test
  public void generatedEvenTemperedBasisCoversHalogenatedOrganicAndHeavyElementRoutes() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(6, 0.0, 0.0, 0.0),
                IntegralAtom.of(7, 0.0, 0.0, 2.0),
                IntegralAtom.of(35, 2.4, 0.0, 0.0),
                IntegralAtom.of(53, 4.8, 0.0, 0.0)),
            0);

    final GaussianBasisSet basisSet = GaussianBasisLibrary.generatedEvenTempered(molecule);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();
    final OneElectronIntegralMatrices matrices =
        new GaussianOneElectronIntegralEngine().evaluate(molecule, basisSet);
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);

    assertEquals(GaussianBasisLibrary.GENERATED_EVEN_TEMPERED_NAME, basisSet.name());
    assertTrue(GaussianBasisLibrary.generatedEvenTemperedName("AUTO-GAUSSIAN"));
    assertTrue(GaussianBasisLibrary.generatedEvenTemperedName("ETB"));
    assertTrue(basisSet.orbitalCount() > 60);
    assertEquals(3, basisSet.maxAngularMomentum());
    assertEquals(Integer.valueOf(17), histogram.get(GaussianShellType.S));
    assertEquals(Integer.valueOf(39), histogram.get(GaussianShellType.P));
    assertEquals(Integer.valueOf(42), histogram.get(GaussianShellType.D));
    assertEquals(Integer.valueOf(10), histogram.get(GaussianShellType.F));
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, oneElectronPreflight.status());
    assertEquals(basisSet.orbitalCount(), matrices.orbitalCount());
    assertTrue(Double.isFinite(matrices.overlapAt(0, 0)));
    assertTrue(Double.isFinite(matrices.coreHamiltonianAt(0, 0)));
    assertEquals(matrices.overlapAt(3, 7), matrices.overlapAt(7, 3), 1.0E-12);
  }

  @Test
  public void generatedCompactEvenTemperedBasisCoversHeavyRoutesWithinFullTensorBudget() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(6, 0.0, 0.0, 0.0),
                IntegralAtom.of(7, 0.0, 0.0, 2.0),
                IntegralAtom.of(35, 2.4, 0.0, 0.0),
                IntegralAtom.of(53, 4.8, 0.0, 0.0)),
            0);

    final GaussianBasisSet fullBasisSet = GaussianBasisLibrary.generatedEvenTempered(molecule);
    final GaussianBasisSet compactBasisSet =
        GaussianBasisLibrary.generatedCompactEvenTempered(molecule);
    final OneElectronIntegralMatrices matrices =
        new GaussianOneElectronIntegralEngine().evaluate(molecule, compactBasisSet);
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(compactBasisSet);
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(compactBasisSet);

    assertEquals(GaussianBasisLibrary.GENERATED_COMPACT_EVEN_TEMPERED_NAME, compactBasisSet.name());
    assertTrue(GaussianBasisLibrary.generatedCompactEvenTemperedName("AUTO-COMPACT-GAUSSIAN"));
    assertTrue(GaussianBasisLibrary.generatedCompactEvenTemperedName("CETB"));
    assertTrue(compactBasisSet.orbitalCount() < fullBasisSet.orbitalCount());
    assertTrue(compactBasisSet.maxAngularMomentum() <= 2);
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, oneElectronPreflight.status());
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, fullTensorPreflight.status());
    assertEquals(compactBasisSet.orbitalCount(), matrices.orbitalCount());
    assertTrue(Double.isFinite(matrices.overlapAt(0, 0)));
    assertTrue(Double.isFinite(matrices.coreHamiltonianAt(0, 0)));
  }

  @Test
  public void textParserIgnoresHashAndBangCommentsInBasisFiles() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0)), 0);
    final String source =
        String.join(
            "\n",
            "! comment before basis",
            "BASIS",
            "C 0 ! carbon block",
            "S 1 1.00 # scaled shell header comment",
            "  0.5500000000  1.0000000000 ! primitive comment",
            "****",
            "END # trailing parser directive comment");

    final GaussianBasisSet basisSet =
        new GaussianBasisTextParser().parseForMolecule("commented-carbon-s", source, molecule);

    assertEquals(1, basisSet.orbitalCount());
    assertEquals(0, basisSet.maxAngularMomentum());
    assertNull(GaussianIntegralCapability.unsupportedFullTensorReason(basisSet));
  }

  @Test
  public void textParserRejectsEcpSectionsWithExplicitMessage() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(46, 0.0, 0.0, 0.0)), 0);
    final String source =
        String.join(
            "\n",
            "Pd 0",
            "S 1 1.00",
            "  0.2500000000  1.0000000000",
            "****",
            "Pd 0",
            "Pd-ECP 2 10",
            "f potential",
            "  1  2.0000000000  5.0000000000",
            "****");

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new GaussianBasisTextParser().parseForMolecule("pd-ecp", source, molecule));

    assertTrue(exception.getMessage().contains("ECP"));
    assertTrue(exception.getMessage().contains("Java-native ECP integral engine"));
  }

  @Test
  public void textParserRejectsBlockShellThatEndsBeforeDeclaredPrimitiveCount() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0)), 0);
    final String source = String.join("\n", "C 0", "S 2 1.00", "  1.0 1.0", "****");

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new GaussianBasisTextParser().parseForMolecule("broken-block", source, molecule));

    assertTrue(exception.getMessage().contains("declared primitive count"));
  }

  @Test
  public void fullEriEngineEvaluatesPBasisWithIntegralSymmetry() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0), IntegralAtom.of(7, 0.0, 0.0, 2.0)), 0);
    final GaussianBasisSet basisSet =
        GaussianBasisSet.of(
            "full-eri-p",
            List.of(
                GaussianBasisLibrary.cartesianShellOrbitals(
                        0, GaussianShellType.P, new double[] {0.55}, new double[] {1.0})
                    .get(0),
                GaussianBasisLibrary.cartesianShellOrbitals(
                        0, GaussianShellType.P, new double[] {0.43}, new double[] {1.0})
                    .get(0)));

    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, basisSet);

    assertEquals(2, tensors.orbitalCount());
    assertNull(GaussianIntegralEngine.unsupportedAngularMomentumReason(basisSet));
    assertTrue(Double.isFinite(tensors.overlapAt(0, 1)));
    assertTrue(Double.isFinite(tensors.coreHamiltonianAt(1, 1)));
    assertTrue(Double.isFinite(tensors.coreHamiltonianAt(0, 1)));
    assertTrue(Double.isFinite(tensors.electronRepulsionAt(0, 1, 0, 1)));
    assertEquals(
        tensors.electronRepulsionAt(0, 1, 0, 1), tensors.electronRepulsionAt(1, 0, 1, 0), 1.0E-10);
    assertEquals(
        tensors.electronRepulsionAt(0, 1, 1, 0), tensors.electronRepulsionAt(1, 0, 0, 1), 1.0E-10);
  }

  @Test
  public void
      oneElectronEngineEvaluatesGFunctionsWhileFullEriEngineRejectsHighDerivativeQuartetExplicitly() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0), IntegralAtom.of(7, 0.0, 0.0, 2.0)), 0);
    final GaussianBasisSet basisSet =
        GaussianBasisSet.of(
            "one-electron-g",
            List.of(
                GaussianBasisLibrary.cartesianShellOrbitals(
                        1, GaussianShellType.G, new double[] {0.29}, new double[] {1.0})
                    .get(0)));

    final OneElectronIntegralMatrices matrices =
        new GaussianOneElectronIntegralEngine().evaluate(molecule, basisSet);

    assertEquals(1, matrices.orbitalCount());
    assertEquals(matrices.overlapAt(0, 0), matrices.overlapAt(0, 0), 1.0E-12);
    assertEquals(matrices.kineticAt(0, 0), matrices.kineticAt(0, 0), 1.0E-12);
    assertEquals(matrices.nuclearAttractionAt(0, 0), matrices.nuclearAttractionAt(0, 0), 1.0E-12);
    assertTrue(Double.isFinite(matrices.overlapAt(0, 0)));
    assertTrue(Double.isFinite(matrices.kineticAt(0, 0)));
    assertTrue(Double.isFinite(matrices.nuclearAttractionAt(0, 0)));
    assertTrue(Double.isFinite(matrices.coreHamiltonianAt(0, 0)));
    assertNull(GaussianIntegralEngine.unsupportedAngularMomentumReason(basisSet));
  }

  private static List<ContractedGaussianOrbital> repeatedSOrbitals(final int count) {
    final java.util.ArrayList<ContractedGaussianOrbital> orbitals =
        new java.util.ArrayList<ContractedGaussianOrbital>(count);
    for (int i = 0; i < count; ++i) {
      orbitals.add(
          ContractedGaussianOrbital.of(
              0,
              GaussianAngularMomentum.S,
              List.of(GaussianPrimitive.of(0.4 + 0.001 * (double) i, 1.0))));
    }
    return List.copyOf(orbitals);
  }

  @Test
  public void textParserRejectsMissingElementBlockBeforeIntegralExecution() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(IntegralAtom.of(6, 0.0, 0.0, 0.0), IntegralAtom.of(8, 0.0, 0.0, 2.0)), 0);
    final String source = String.join("\n", "C S", "  1.0 1.0", "****");

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new GaussianBasisTextParser().parseForMolecule("missing-o", source, molecule));

    assertTrue(exception.getMessage().contains("atomic number 8"));
  }

  @Test
  public void structuredBasisAdapterExpandsCoreBasisDefinitionToGaussianOrbitals() {
    final BasisSetDefinition definition =
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("Fe"),
                    List.of(
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.S),
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.P),
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.D),
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.F),
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.G)))));
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(26, 0.0, 0.0, 0.0)), 0);

    final GaussianBasisSet basisSet =
        new StructuredGaussianBasisAdapter()
            .toGaussianBasisSet("custom-fe-spdfg", definition, molecule);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);

    assertEquals(35, basisSet.orbitalCount());
    assertEquals(4, basisSet.maxAngularMomentum());
    assertTrue(oneElectronPreflight.supported());
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, oneElectronPreflight.status());
    assertTrue(oneElectronPreflight.oneElectronFallbackAvailable());
    assertEquals(35, oneElectronPreflight.orbitalCount());
    assertEquals(4, oneElectronPreflight.maxAngularMomentum());
    assertEquals(1, histogram.get(GaussianShellType.S));
    assertEquals(3, histogram.get(GaussianShellType.P));
    assertEquals(6, histogram.get(GaussianShellType.D));
    assertEquals(10, histogram.get(GaussianShellType.F));
    assertEquals(15, histogram.get(GaussianShellType.G));
    assertNull(GaussianIntegralEngine.unsupportedAngularMomentumReason(basisSet));
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);
    assertEquals(GaussianIntegralCapabilityStatus.SUPPORTED, fullTensorPreflight.status());
    assertTrue(fullTensorPreflight.supported());
    assertTrue(fullTensorPreflight.oneElectronFallbackAvailable());
    assertEquals(16, fullTensorPreflight.maxFullEriDerivativeOrder());
  }

  @Test
  public void structuredBasisAdapterPreservesHighAngularCoreBasisAndSupportsOneElectronPath() {
    final BasisSetDefinition definition =
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("Fe"),
                    List.of(
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.H),
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.I),
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.K)))));
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(26, 0.0, 0.0, 0.0)), 0);

    final GaussianBasisSet basisSet =
        new StructuredGaussianBasisAdapter()
            .toGaussianBasisSet("custom-fe-hik", definition, molecule);
    final Map<GaussianShellType, Integer> histogram = basisSet.shellHistogram();

    assertEquals(85, basisSet.orbitalCount());
    assertEquals(7, basisSet.maxAngularMomentum());
    assertEquals(Integer.valueOf(21), histogram.get(GaussianShellType.H));
    assertEquals(Integer.valueOf(28), histogram.get(GaussianShellType.I));
    assertEquals(Integer.valueOf(36), histogram.get(GaussianShellType.K));
    assertNull(GaussianIntegralCapability.unsupportedOneElectronReason(basisSet));
  }

  @Test
  public void structuredBasisAdapterRejectsMissingMoleculeElementDefinition() {
    final BasisSetDefinition definition =
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("H"),
                    List.of(
                        GaussianBasisLibraryCapabilityTest.shell(BasisShellAngularMomentum.S)))));
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(17, 0.0, 0.0, 0.0)), 0);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new StructuredGaussianBasisAdapter()
                    .toGaussianBasisSet("hydrogen-only", definition, molecule));

    assertTrue(exception.getMessage().contains("Cl"));
  }

  private static BasisShell shell(final BasisShellAngularMomentum angularMomentum) {
    return BasisShell.of(
        angularMomentum,
        List.of(BasisPrimitive.of(PRIMITIVE_EXPONENT + angularMomentum.angularMomentum(), 1.0)));
  }
}
