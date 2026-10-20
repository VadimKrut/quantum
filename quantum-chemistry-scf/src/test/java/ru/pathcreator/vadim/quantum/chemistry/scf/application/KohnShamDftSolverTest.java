/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.validation.ScfResultValidator;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamFunctionalFamily;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationReport;

public final class KohnShamDftSolverTest {

  @Test
  public void solvesHydrogenSto3gClosedShellKohnShamDft() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftResult result = new KohnShamDftSolver().solve(tensors, 2, "B3LYP");

    assertTrue(result.converged());
    assertTrue(result.iterations() <= 16);
    assertEquals(result.iterations(), result.iterationHistory().size());
    assertTrue(result.iterationHistory().get(result.iterationHistory().size() - 1).converged());
    assertTrue(result.iterationHistory().get(0).densityChange() > 0.0);
    assertEquals(KohnShamFunctionalFamily.HYBRID, result.functionalFamily());
    assertEquals(0.2, result.exactExchangeFraction(), 1.0E-12);
    assertEquals(2.0, result.densityTrace(), 1.0E-8);
    assertTrue(result.exchangeEnergy() < 0.0);
    assertTrue(result.correlationEnergy() < 0.0);
    assertTrue(result.totalEnergy() < 0.0);
    assertEquals(4, result.molecularOrbitalCoefficientsCopy().length);
    assertEquals(4, result.densityMatrixCopy().length);
  }

  @Test
  public void validatesConvergedKohnShamDftDensityAndOrbitals() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftResult result = new KohnShamDftSolver().solve(tensors, 2, "B3LYP");

    final ScfValidationReport report =
        new ScfResultValidator()
            .validateKohnSham(result, tensors.overlapCopy(), tensors.orbitalCount());

    assertTrue(report.valid());
    assertTrue(report.diagnostics().isEmpty());
    assertTrue(report.electronTraceDeviation() <= 1.0E-8);
    assertTrue(report.densitySymmetryDeviation() <= 1.0E-12);
    assertTrue(report.molecularOrbitalOrthonormalityDeviation() <= 1.0E-8);
  }

  @Test
  public void functionalFamiliesProduceDistinctEnergies() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftSolver solver = new KohnShamDftSolver();

    final KohnShamDftResult lda = solver.solve(tensors, 2, "SVWN");
    final KohnShamDftResult gga = solver.solve(tensors, 2, "PBE");
    final KohnShamDftResult hybrid = solver.solve(tensors, 2, "B3LYP");

    assertEquals(KohnShamFunctionalFamily.LDA, lda.functionalFamily());
    assertEquals(KohnShamFunctionalFamily.GGA, gga.functionalFamily());
    assertEquals(KohnShamFunctionalFamily.HYBRID, hybrid.functionalFamily());
    assertEquals(lda.iterations(), lda.iterationHistory().size());
    assertEquals(gga.iterations(), gga.iterationHistory().size());
    assertEquals(hybrid.iterations(), hybrid.iterationHistory().size());
    assertNotEquals(lda.totalEnergy(), gga.totalEnergy());
    assertNotEquals(gga.totalEnergy(), hybrid.totalEnergy());
  }

  @Test
  public void ldaUsesAtomCenteredGridWhenMoleculeAndBasisAreAvailable() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftSolver solver = new KohnShamDftSolver();

    final KohnShamDftResult gridResult =
        solver.solve(molecule, basisSet, tensors, 2, "LDA_X", KohnShamDftOptions.DEFAULT);
    final KohnShamDftResult fallbackResult = solver.solve(tensors, 2, "LDA_X");

    assertTrue(gridResult.converged());
    assertEquals(KohnShamFunctionalFamily.LDA, gridResult.functionalFamily());
    assertEquals(2.0, gridResult.densityTrace(), 1.0E-7);
    assertTrue(gridResult.exchangeEnergy() < 0.0);
    assertEquals(0.0, gridResult.correlationEnergy(), 1.0E-14);
    assertEquals(-1.0250111302352929, gridResult.totalEnergy(), 2.0E-6);
    assertNotEquals(fallbackResult.exchangeEnergy(), gridResult.exchangeEnergy());
    assertNotEquals(fallbackResult.totalEnergy(), gridResult.totalEnergy());
  }

  @Test
  public void svwnUsesAtomCenteredGridExchangeAndCorrelationWhenMoleculeAndBasisAreAvailable() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();

    final KohnShamDftResult result =
        new KohnShamDftSolver()
            .solve(molecule, basisSet, tensors, 2, "SVWN", KohnShamDftOptions.DEFAULT);

    assertTrue(result.converged());
    assertEquals(KohnShamFunctionalFamily.LDA, result.functionalFamily());
    assertEquals(2.0, result.densityTrace(), 1.0E-7);
    assertTrue(result.exchangeEnergy() < 0.0);
    assertTrue(result.correlationEnergy() < 0.0);
    assertEquals(-1.121192799173512, result.totalEnergy(), 2.0E-6);
  }

  @Test
  public void pbeUsesAtomCenteredGridGradientCorrectionWhenMoleculeAndBasisAreAvailable() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();

    final KohnShamDftResult result =
        new KohnShamDftSolver()
            .solve(molecule, basisSet, tensors, 2, "PBE", KohnShamDftOptions.DEFAULT);

    assertTrue(result.converged());
    assertEquals(KohnShamFunctionalFamily.GGA, result.functionalFamily());
    assertEquals(2.0, result.densityTrace(), 1.0E-7);
    assertTrue(result.exchangeEnergy() < 0.0);
    assertTrue(result.correlationEnergy() < 0.0);
    assertEquals(-1.1520582601959286, result.totalEnergy(), 3.0E-4);
  }

  @Test
  public void pbe0ScalesSemilocalExchangeBeforeAddingExactExchange() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();

    final KohnShamDftResult result =
        new KohnShamDftSolver()
            .solve(molecule, basisSet, tensors, 2, "PBE0", KohnShamDftOptions.DEFAULT);

    assertTrue(result.converged());
    assertEquals(KohnShamFunctionalFamily.HYBRID, result.functionalFamily());
    assertEquals(0.25, result.exactExchangeFraction(), 1.0E-12);
    assertEquals(2.0, result.densityTrace(), 1.0E-7);
    assertTrue(result.exchangeEnergy() < 0.0);
    assertTrue(result.correlationEnergy() < 0.0);
    assertEquals(-1.1542980543910586, result.totalEnergy(), 3.0E-4);
  }

  @Test
  public void b3lypUsesGridCompatibleHybridPathWhenMoleculeAndBasisAreAvailable() {
    final IntegralMolecule molecule = RestrictedHartreeFockSolverTest.hydrogenIntegralMolecule();
    final GaussianBasisSet basisSet = RestrictedHartreeFockSolverTest.hydrogenSto3gBasis();
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftSolver solver = new KohnShamDftSolver();

    final KohnShamDftResult gridResult =
        solver.solve(molecule, basisSet, tensors, 2, "B3LYP", KohnShamDftOptions.DEFAULT);
    final KohnShamDftResult fallbackResult = solver.solve(tensors, 2, "B3LYP");

    assertTrue(gridResult.converged());
    assertEquals(KohnShamFunctionalFamily.HYBRID, gridResult.functionalFamily());
    assertEquals(0.2, gridResult.exactExchangeFraction(), 1.0E-12);
    assertEquals(2.0, gridResult.densityTrace(), 1.0E-7);
    assertTrue(gridResult.exchangeEnergy() < 0.0);
    assertTrue(gridResult.correlationEnergy() < 0.0);
    assertNotEquals(fallbackResult.exchangeEnergy(), gridResult.exchangeEnergy());
    assertNotEquals(fallbackResult.totalEnergy(), gridResult.totalEnergy());
  }

  @Test
  public void kohnShamConvergenceRequiresBothEnergyAndDensityTolerance() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftOptions options = KohnShamDftOptions.of(2, 1.0E9, 1.0E-300, 0.0);

    final KohnShamDftResult result = new KohnShamDftSolver().solve(tensors, 2, "B3LYP", options);

    assertFalse(result.converged());
    assertEquals(2, result.iterations());
    assertFalse(result.iterationHistory().get(result.iterationHistory().size() - 1).converged());
    assertTrue(
        result.iterationHistory().get(result.iterationHistory().size() - 1).densityChange()
            > 1.0E-300);
  }

  @Test
  public void validationRejectsNonConvergedKohnShamReference() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final KohnShamDftOptions options = KohnShamDftOptions.of(2, 1.0E9, 1.0E-300, 0.0);
    final KohnShamDftResult result = new KohnShamDftSolver().solve(tensors, 2, "B3LYP", options);

    final ScfValidationReport report =
        new ScfResultValidator()
            .validateKohnSham(result, tensors.overlapCopy(), tensors.orbitalCount());

    assertFalse(report.valid());
    assertTrue(
        report.diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ScfDiagnosticCode.RESULT_NOT_CONVERGED));
  }

  @Test
  public void rejectsOpenShellElectronCount() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();

    assertThrows(
        IllegalArgumentException.class, () -> new KohnShamDftSolver().solve(tensors, 1, "LDA"));
  }
}
