/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionSlaterDeterminantExpectationEstimator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation.ErrorMitigationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeConvergenceStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;

final class StateVectorPauliVqeEnergyBackendTest {

  @Test
  void backendEvaluatesParameterizedProgramAgainstPauliHamiltonian() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.IDENTITY, ComplexCoefficient.real(0.25))
            .add(QubitPauliTerm.of(0, PauliAxis.Z), ComplexCoefficient.real(0.75))
            .build();
    final StateVectorPauliVqeEnergyBackend backend =
        new StateVectorPauliVqeEnergyBackend(hamiltonian);

    final VqeEnergyEvaluation evaluation =
        backend.evaluate(
            Quantum.programBuilder()
                .circuit("state")
                .qreg("q", 1)
                .creg("c", 1)
                .rx(ParameterExpression.named("theta0"), "q[0]")
                .build(),
            new double[] {Math.PI},
            1024,
            new ErrorMitigationPlanner()
                .plan(
                    ErrorMitigationRequest.of(
                        ActiveSpace.of(2, 2),
                        QuantumNoiseModel.ideal(),
                        List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                        1024,
                        false,
                        false)));

    Assertions.assertTrue(evaluation.successful());
    Assertions.assertEquals(-0.5, evaluation.energy(), 1.0E-12);
    Assertions.assertEquals(1024, evaluation.shots());
  }

  @Test
  void electronicHamiltonianMapsToQubitHamiltonianAndEvaluatesHartreeFockReference() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 1);
    final ElectronicHamiltonian electronicHamiltonian =
        ElectronicHamiltonian.of(
            activeSpace, 0.0, List.of(OneElectronIntegral.of(0, 0, -1.0)), List.of());
    final QubitPauliOperator qubitHamiltonian =
        new ElectronicHamiltonianQubitMapper()
            .mapVerified(electronicHamiltonian, FermionToQubitMapping.JORDAN_WIGNER, 1.0E-10)
            .qubitOperator();
    final double directFermionEnergy =
        new FermionSlaterDeterminantExpectationEstimator()
            .hartreeFockReferenceEnergy(electronicHamiltonian);
    final StateVectorPauliVqeEnergyBackend backend =
        new StateVectorPauliVqeEnergyBackend(qubitHamiltonian);

    final VqeEnergyEvaluation evaluation =
        backend.evaluate(
            Quantum.programBuilder()
                .circuit("hf_reference")
                .qreg("q", activeSpace.spinOrbitalCount())
                .x("q[0]")
                .x("q[1]")
                .build(),
            new double[] {0.0},
            4096,
            new ErrorMitigationPlanner()
                .plan(
                    ErrorMitigationRequest.of(
                        activeSpace,
                        QuantumNoiseModel.ideal(),
                        List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                        4096,
                        false,
                        false)));

    Assertions.assertTrue(evaluation.successful(), evaluation.diagnostic());
    Assertions.assertTrue(qubitHamiltonian.hermitianWithinTolerance(1.0E-12));
    Assertions.assertEquals(-2.0, evaluation.energy(), 1.0E-12);
    Assertions.assertEquals(directFermionEnergy, evaluation.energy(), 1.0E-12);
  }

  @Test
  void backendEvaluatesLargeVerifiedMappedHamiltonianReferenceState() {
    final ActiveSpace activeSpace = ActiveSpace.of(4, 5);
    final ElectronicHamiltonian electronicHamiltonian =
        ElectronicHamiltonian.of(
            activeSpace,
            3.25,
            List.of(
                OneElectronIntegral.of(0, 0, -1.11),
                OneElectronIntegral.of(1, 1, -0.92),
                OneElectronIntegral.of(2, 2, -0.74),
                OneElectronIntegral.of(3, 3, -0.51),
                OneElectronIntegral.of(4, 4, -0.33),
                OneElectronIntegral.of(0, 1, -0.08),
                OneElectronIntegral.of(1, 2, -0.06),
                OneElectronIntegral.of(2, 3, -0.04),
                OneElectronIntegral.of(3, 4, -0.03)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.72),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.68),
                TwoElectronIntegral.of(2, 2, 2, 2, 0.61),
                TwoElectronIntegral.of(3, 3, 3, 3, 0.55),
                TwoElectronIntegral.of(4, 4, 4, 4, 0.49),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.31),
                TwoElectronIntegral.of(1, 1, 2, 2, 0.28),
                TwoElectronIntegral.of(2, 2, 3, 3, 0.24),
                TwoElectronIntegral.of(3, 3, 4, 4, 0.21),
                TwoElectronIntegral.of(0, 1, 0, 1, 0.07),
                TwoElectronIntegral.of(1, 2, 1, 2, 0.06),
                TwoElectronIntegral.of(2, 3, 2, 3, 0.05),
                TwoElectronIntegral.of(3, 4, 3, 4, 0.04)));
    final QubitPauliOperator qubitHamiltonian =
        new ElectronicHamiltonianQubitMapper()
            .mapVerified(electronicHamiltonian, FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8)
            .qubitOperator();
    final double directFermionEnergy =
        new FermionSlaterDeterminantExpectationEstimator()
            .hartreeFockReferenceEnergy(electronicHamiltonian);
    final StateVectorPauliVqeEnergyBackend backend =
        new StateVectorPauliVqeEnergyBackend(qubitHamiltonian);

    final VqeEnergyEvaluation evaluation =
        backend.evaluate(
            Quantum.programBuilder()
                .circuit("large_hf_reference")
                .qreg("q", activeSpace.spinOrbitalCount())
                .x("q[0]")
                .x("q[1]")
                .x("q[2]")
                .x("q[3]")
                .build(),
            new double[] {0.0},
            2048,
            new ErrorMitigationPlanner()
                .plan(
                    ErrorMitigationRequest.of(
                        activeSpace,
                        QuantumNoiseModel.ideal(),
                        List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                        2048,
                        false,
                        false)));

    Assertions.assertTrue(evaluation.successful(), evaluation.diagnostic());
    Assertions.assertTrue(qubitHamiltonian.hermitianWithinTolerance(1.0E-12));
    Assertions.assertTrue(Double.isFinite(evaluation.energy()));
    Assertions.assertEquals(directFermionEnergy, evaluation.energy(), 1.0E-10);
  }

  @Test
  void optimizerCanUseLocalStateVectorPauliBackend() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Z), ComplexCoefficient.real(1.0))
            .build();
    final VqeOptimizer optimizer =
        new VqeOptimizer(new StateVectorPauliVqeEnergyBackend(hamiltonian));

    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
                    "local_vqe",
                    "q",
                    "c",
                    "theta",
                    1,
                    2,
                    false),
                new double[] {0.0},
                VqeOptimizationOptions.of(12, 256, 0.5, 1.0E-4, 1.0E-5, 1.0E-5),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    256,
                    false,
                    false)));

    Assertions.assertTrue(
        result.status() == VqeConvergenceStatus.CONVERGED
            || result.status() == VqeConvergenceStatus.MAX_ITERATIONS_REACHED,
        result.status() + ": " + result.diagnostic());
    Assertions.assertFalse(result.backendFailed());
    Assertions.assertTrue(result.bestEnergy() <= 1.0);
    Assertions.assertTrue(result.evaluationCount() > 0L);
  }
}
