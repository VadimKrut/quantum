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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.validation.ScfResultValidator;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationReport;

public final class ScfResultValidatorTest {

  @Test
  public void restrictedHydrogenResultSatisfiesMetricAwareScfInvariants() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final RhfResult result = new RestrictedHartreeFockSolver().solve(tensors, 2);

    final ScfValidationReport report =
        new ScfResultValidator()
            .validateRestricted(result, tensors.overlapCopy(), tensors.orbitalCount(), 2);

    assertTrue(report.valid());
    assertTrue(report.diagnostics().isEmpty());
    assertTrue(report.electronTraceDeviation() <= 1.0E-8);
    assertTrue(report.densitySymmetryDeviation() <= 1.0E-12);
    assertTrue(report.molecularOrbitalOrthonormalityDeviation() <= 1.0E-10);
  }

  @Test
  public void restrictedValidatorRejectsDensityWithWrongElectronTrace() {
    final MolecularIntegralTensors tensors =
        RestrictedHartreeFockSolverTest.hydrogenAtomicOrbitalTensors();
    final RhfResult source = new RestrictedHartreeFockSolver().solve(tensors, 2);
    final double[] corruptedDensity = source.densityMatrixCopy();
    corruptedDensity[0] *= 0.25;
    final RhfResult corrupted =
        RhfResult.of(
            source.converged(),
            source.iterations(),
            source.electronicEnergy(),
            source.totalEnergy(),
            source.orbitalEnergiesCopy(),
            source.molecularOrbitalCoefficientsCopy(),
            corruptedDensity,
            source.iterationHistory());

    final ScfValidationReport report =
        new ScfResultValidator()
            .validateRestricted(corrupted, tensors.overlapCopy(), tensors.orbitalCount(), 2);

    assertFalse(report.valid());
    assertTrue(report.electronTraceDeviation() > 1.0E-3);
    assertEquals(ScfDiagnosticCode.DENSITY_TRACE_MISMATCH, report.diagnostics().get(0).code());
  }

  @Test
  public void unrestrictedLithiumResultSatisfiesSpinResolvedScfInvariants() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(3, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final UhfResult result = new UnrestrictedHartreeFockSolver().solve(tensors, 3, 2);

    final ScfValidationReport report =
        new ScfResultValidator()
            .validateUnrestricted(result, tensors.overlapCopy(), tensors.orbitalCount());

    assertTrue(report.valid());
    assertTrue(report.diagnostics().isEmpty());
    assertEquals(2, result.alphaElectronCount());
    assertEquals(1, result.betaElectronCount());
    assertTrue(report.electronTraceDeviation() <= 1.0E-8);
    assertTrue(report.molecularOrbitalOrthonormalityDeviation() <= 1.0E-8);
  }
}
