/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.ElectronicHamiltonianIntegralBuilder;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.MolecularOrbitalIntegralTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.ci.CisdResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.UnrestrictedMp2Result;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.RestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.UnrestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

public final class PostHartreeFockConsistencyTest {

  private static final double H2_DISTANCE_BOHR = 1.4018897259885788;

  @Test
  public void cisdMatchesFciForTwoElectronHydrogenMinimalBasis() {
    final HydrogenProblem problem = PostHartreeFockConsistencyTest.hydrogenProblem();
    final FciResult fciResult =
        new FullConfigurationInteractionSolver()
            .solve(problem.hamiltonian(), FciOptions.spinSector(20_000, 2, 1, 1));
    final CisdResult cisdResult =
        new ConfigurationInteractionSinglesDoublesSolver().solve(problem.hamiltonian());

    assertEquals(4, fciResult.determinantCount());
    assertEquals(6, cisdResult.determinantCount());
    assertEquals(fciResult.totalEnergy(), cisdResult.totalEnergy(), 1.0E-8);
    assertEquals(fciResult.electronicEnergy(), cisdResult.electronicEnergy(), 1.0E-8);
  }

  @Test
  public void determinantSpaceCcsdLowersRhfEnergyAndTracksFciForHydrogenMinimalBasis() {
    final HydrogenProblem problem = PostHartreeFockConsistencyTest.hydrogenProblem();
    final FciResult fciResult =
        new FullConfigurationInteractionSolver()
            .solve(problem.hamiltonian(), FciOptions.spinSector(20_000, 2, 1, 1));
    final CoupledClusterSinglesDoublesResult ccsdResult =
        new DeterminantSpaceCoupledClusterSinglesDoublesSolver()
            .solve(
                problem.hamiltonian(),
                problem.rhfResult(),
                CoupledClusterSinglesDoublesOptions.of(128, 1.0E-10, 0.35, 1.0E-12, true));

    assertTrue(ccsdResult.converged());
    assertTrue(ccsdResult.correlationEnergy() < 0.0);
    assertTrue(ccsdResult.totalEnergy() < problem.rhfResult().totalEnergy());
    assertEquals(fciResult.totalEnergy(), ccsdResult.totalEnergy(), 5.0E-5);
    assertEquals(ccsdResult.iterations(), ccsdResult.iterationHistory().size());
  }

  @Test
  public void generatedHydrogenHamiltonianMapsToEquivalentQubitSpectraForEveryMapping() {
    final HydrogenProblem problem = PostHartreeFockConsistencyTest.hydrogenProblem();
    final FciResult fciResult =
        new FullConfigurationInteractionSolver()
            .solve(problem.hamiltonian(), FciOptions.spinSector(20_000, 2, 1, 1));
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    assertTrue(fciResult.totalEnergy() < problem.rhfResult().totalEnergy());
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping mapped =
          mapper.mapVerified(problem.hamiltonian(), mapping, 1.0E-8);

      assertEquals(mapping, mapped.mapping());
      assertTrue(mapped.fermionOperator().termCount() > 0, mapping.name());
      assertTrue(mapped.qubitOperator().termCount() > 0, mapping.name());
      assertTrue(mapped.algebraResult().validWithinTolerance(), mapping.name());
      assertTrue(mapped.spectralResult().equivalentWithinTolerance(), mapping.name());
      assertEquals("EXHAUSTIVE_SPECTRAL_MOMENTS", mapped.spectralResult().verificationMode());
      assertTrue(mapped.verified(), mapping.name());
    }
  }

  @Test
  public void openShellLithiumHamiltonianMapsToVerifiedLargeSpaceQubitOperators() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(List.of(IntegralAtom.of(3, 0.0, 0.0, 0.0)), 0);
    final MolecularIntegralTensors tensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final RhfOptions options = RhfOptions.of(64, 1.0E-10, 1.0E-8, 0.0);
    final UhfResult uhfResult = new UnrestrictedHartreeFockSolver().solve(tensors, 3, 2, options);
    final UnrestrictedMp2Result ump2Result = new UnrestrictedMp2Solver().solve(tensors, uhfResult);
    final ElectronicHamiltonian hamiltonian =
        new ElectronicHamiltonianIntegralBuilder().build(3, tensors);
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    assertTrue(uhfResult.converged());
    assertEquals(2, uhfResult.alphaElectronCount());
    assertEquals(1, uhfResult.betaElectronCount());
    assertEquals(0.75, uhfResult.spinSquared(), 1.0E-6);
    assertEquals(0.0, uhfResult.spinContamination(), 1.0E-6);
    assertTrue(ump2Result.correlationEnergy() < 0.0);
    assertTrue(ump2Result.totalEnergy() < uhfResult.totalEnergy());
    assertEquals(
        ump2Result.correlationEnergy(),
        ump2Result.sameSpinCorrelationEnergy() + ump2Result.oppositeSpinCorrelationEnergy(),
        1.0E-12);
    assertEquals(2, ump2Result.alphaOccupiedOrbitalCount());
    assertEquals(1, ump2Result.betaOccupiedOrbitalCount());
    assertEquals(3, hamiltonian.electronCount());
    assertTrue(hamiltonian.spinOrbitalCount() > 8);
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping mapped =
          mapper.mapVerified(hamiltonian, mapping, 1.0E-8);

      assertTrue(mapped.hamiltonianAudit().readyForFermionMapping(), mapping.name());
      assertTrue(mapped.fermionOperator().termCount() > 0, mapping.name());
      assertTrue(mapped.qubitOperator().termCount() > 0, mapping.name());
      assertTrue(mapped.algebraResult().validWithinTolerance(), mapping.name());
      assertTrue(mapped.spectralResult().equivalentWithinTolerance(), mapping.name());
      assertEquals("STRUCTURAL_LARGE_SPACE", mapped.spectralResult().verificationMode());
      assertTrue(mapped.verified(), mapping.name());
    }
  }

  private static HydrogenProblem hydrogenProblem() {
    final IntegralMolecule molecule =
        IntegralMolecule.of(
            List.of(
                IntegralAtom.of(1, 0.0, 0.0, 0.0), IntegralAtom.of(1, 0.0, 0.0, H2_DISTANCE_BOHR)),
            0);
    final MolecularIntegralTensors atomicOrbitalTensors =
        new GaussianIntegralEngine().evaluate(molecule, GaussianBasisLibrary.sto3g(molecule));
    final RhfResult rhfResult = new RestrictedHartreeFockSolver().solve(atomicOrbitalTensors, 2);
    final MolecularIntegralTensors molecularOrbitalTensors =
        new MolecularOrbitalIntegralTransformer()
            .transform(atomicOrbitalTensors, rhfResult.molecularOrbitalCoefficientsCopy());
    final ElectronicHamiltonian hamiltonian =
        new ElectronicHamiltonianIntegralBuilder().build(2, molecularOrbitalTensors);
    return new HydrogenProblem(hamiltonian, rhfResult);
  }

  private static final class HydrogenProblem {

    private final ElectronicHamiltonian hamiltonian;
    private final RhfResult rhfResult;

    private HydrogenProblem(final ElectronicHamiltonian hamiltonian, final RhfResult rhfResult) {
      this.hamiltonian = hamiltonian;
      this.rhfResult = rhfResult;
    }

    private ElectronicHamiltonian hamiltonian() {
      return this.hamiltonian;
    }

    private RhfResult rhfResult() {
      return this.rhfResult;
    }
  }
}
