/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz.VariationalChemistryProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModelKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

final class ErrorMitigationPlannerTest {

  @Test
  void idealNoiseModelDoesNotAddMitigationTechniques() {
    final ErrorMitigationPlan plan =
        new ErrorMitigationPlanner()
            .plan(
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    1024,
                    true,
                    true));

    Assertions.assertEquals(ErrorMitigationStatus.NOT_REQUIRED, plan.status());
    Assertions.assertTrue(plan.techniques().isEmpty());
    Assertions.assertEquals(1024, plan.recommendedShots());
    Assertions.assertTrue(
        ErrorMitigationPlannerTest.hasDiagnostic(
            plan, ErrorMitigationDiagnosticCode.IDEAL_NOISE_MODEL));
  }

  @Test
  void noisyChemistryActiveSpacePlansReadoutZneAndSymmetryMitigation() {
    final ErrorMitigationPlan plan =
        new ErrorMitigationPlanner()
            .plan(
                ErrorMitigationRequest.of(
                    ActiveSpace.of(4, 4),
                    QuantumNoiseModel.of(QuantumNoiseModelKind.DEPOLARIZING, 0.002, 0.02, 0.03),
                    List.of(
                        ErrorMitigationTechnique.READOUT_CALIBRATION,
                        ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION,
                        ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION,
                        ErrorMitigationTechnique.SPIN_PARITY_POST_SELECTION,
                        ErrorMitigationTechnique.SYMMETRY_VERIFICATION,
                        ErrorMitigationTechnique.PAULI_TWIRLING),
                    1000,
                    true,
                    true));

    Assertions.assertEquals(ErrorMitigationStatus.PLANNED, plan.status());
    Assertions.assertEquals(12000, plan.recommendedShots());
    Assertions.assertTrue(plan.techniques().contains(ErrorMitigationTechnique.READOUT_CALIBRATION));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.SPIN_PARITY_POST_SELECTION));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.SYMMETRY_VERIFICATION));
    Assertions.assertTrue(plan.techniques().contains(ErrorMitigationTechnique.PAULI_TWIRLING));
    Assertions.assertFalse(plan.hasErrors());
  }

  @Test
  void missingAllowedTechniqueProducesPartialPlan() {
    final ErrorMitigationPlan plan =
        new ErrorMitigationPlanner()
            .plan(
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.of(QuantumNoiseModelKind.READOUT_BIASED, 0.0, 0.0, 0.08),
                    List.of(ErrorMitigationTechnique.SYMMETRY_VERIFICATION),
                    512,
                    true,
                    false));

    Assertions.assertEquals(ErrorMitigationStatus.PARTIAL, plan.status());
    Assertions.assertFalse(
        plan.techniques().contains(ErrorMitigationTechnique.READOUT_CALIBRATION));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.SYMMETRY_VERIFICATION));
    Assertions.assertEquals(1024, plan.recommendedShots());
    Assertions.assertTrue(
        ErrorMitigationPlannerTest.hasDiagnostic(
            plan, ErrorMitigationDiagnosticCode.TECHNIQUE_UNSUPPORTED));
  }

  @Test
  void mitigationPlannerWorksAlongsideGeneratedQpeChemistryProgram() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 2);
    final QuantumProgram program =
        new VariationalChemistryProgramGenerator()
            .generate(
                activeSpace,
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION,
                    "qpe_noise_program",
                    "system",
                    "phase_readout",
                    "tau",
                    1,
                    8,
                    true,
                    "phase",
                    3,
                    1,
                    true));
    final ErrorMitigationPlan plan =
        new ErrorMitigationPlanner()
            .plan(
                ErrorMitigationRequest.of(
                    activeSpace,
                    QuantumNoiseModel.of(QuantumNoiseModelKind.HARDWARE_UNKNOWN, 0.0, 0.0, 0.0),
                    List.of(
                        ErrorMitigationTechnique.SHOT_REBALANCING,
                        ErrorMitigationTechnique.SYMMETRY_VERIFICATION,
                        ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION),
                    2048,
                    true,
                    false));

    Assertions.assertTrue(Quantum.validate(program).isValid());
    Assertions.assertEquals(ErrorMitigationStatus.PLANNED, plan.status());
    Assertions.assertEquals(8192, plan.recommendedShots());
    Assertions.assertTrue(plan.techniques().contains(ErrorMitigationTechnique.SHOT_REBALANCING));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.SYMMETRY_VERIFICATION));
    Assertions.assertTrue(
        plan.techniques().contains(ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION));
  }

  private static boolean hasDiagnostic(
      final ErrorMitigationPlan plan, final ErrorMitigationDiagnosticCode code) {
    return plan.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code() == code);
  }
}
