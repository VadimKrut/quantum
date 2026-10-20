/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.adapt;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryExcitationRank;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

final class AdaptVqeSelectorTest {

  @Test
  void adaptVqeSelectsEnergyLoweringExcitationAndReturnsSelectedIrProgram() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 2);
    final AdaptVqeSelector selector = new AdaptVqeSelector(new StructureAwareBackend());

    final AdaptVqeSelectionResult result =
        selector.select(
            activeSpace,
            adaptOptions(true),
            AdaptVqeSelectionOptions.of(2, 3, 512, 0.125, 1.0E-5, 0),
            mitigation(activeSpace));

    Assertions.assertEquals(AdaptVqeSelectionStatus.CONVERGED, result.status());
    Assertions.assertTrue(result.converged());
    Assertions.assertFalse(result.backendFailed());
    Assertions.assertEquals(1, result.selectedExcitations().size());
    Assertions.assertEquals(
        ChemistryExcitationRank.DOUBLE, result.selectedExcitations().get(0).rank());
    Assertions.assertEquals(0, result.selectedExcitations().get(0).occupiedSpinOrbital(0));
    Assertions.assertEquals(1, result.selectedExcitations().get(0).occupiedSpinOrbital(1));
    Assertions.assertEquals(2, result.selectedExcitations().get(0).virtualSpinOrbital(0));
    Assertions.assertEquals(3, result.selectedExcitations().get(0).virtualSpinOrbital(1));
    Assertions.assertArrayEquals(new double[] {0.125}, result.selectedParametersCopy(), 1.0E-15);
    Assertions.assertTrue(result.bestEnergy() < result.referenceEnergy());
    Assertions.assertTrue(result.energyImprovement() > 0.0);
    Assertions.assertEquals(2, result.iterations().size());
    Assertions.assertTrue(result.iterations().get(0).accepted());
    Assertions.assertFalse(result.iterations().get(1).accepted());
    Assertions.assertEquals(3L, result.iterations().get(0).evaluatedCandidateCount());
    Assertions.assertEquals(2L, result.iterations().get(1).evaluatedCandidateCount());
    Assertions.assertEquals(6L, result.evaluationCount());
    Assertions.assertEquals(0L, result.failedEvaluationCount());
    Assertions.assertEquals(6L * 512L, result.totalShots());
    Assertions.assertEquals(1, result.selectedProgram().circuitCount());
    Assertions.assertTrue(result.selectedProgram().circuit(0).operationCount() > 0);
    Assertions.assertTrue(Quantum.validate(result.selectedProgram()).isValid());
    Assertions.assertTrue(result.mitigationPlan().recommendedShots() >= 512);
  }

  @Test
  void adaptVqeStopsAtSelectedOperatorLimitWhenEveryCandidateImprovesEnergy() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 3);
    final AdaptVqeSelector selector =
        new AdaptVqeSelector(
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.success(-parameters.length, 0.0, shots));

    final AdaptVqeSelectionResult result =
        selector.select(
            activeSpace,
            adaptOptions(false),
            AdaptVqeSelectionOptions.of(3, 5, 256, 0.25, 1.0E-8, 0),
            mitigation(activeSpace));

    Assertions.assertEquals(AdaptVqeSelectionStatus.MAX_OPERATORS_SELECTED, result.status());
    Assertions.assertEquals(3, result.selectedExcitations().size());
    Assertions.assertEquals(3, result.selectedParametersCopy().length);
    Assertions.assertEquals(3, result.iterations().size());
    Assertions.assertTrue(result.energyImprovement() >= 3.0);
    Assertions.assertEquals(1 + 5 + 4 + 3, result.evaluationCount());
  }

  @Test
  void adaptVqeReportsBackendFailureWithoutThrowingNormalWorkflowException() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 2);
    final AdaptVqeSelector selector =
        new AdaptVqeSelector(
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.failure("energy backend unavailable"));

    final AdaptVqeSelectionResult result =
        selector.select(
            activeSpace,
            adaptOptions(true),
            AdaptVqeSelectionOptions.of(1, 3, 128, 0.1, 1.0E-5, 2),
            mitigation(activeSpace));

    Assertions.assertEquals(AdaptVqeSelectionStatus.BACKEND_FAILED, result.status());
    Assertions.assertTrue(result.backendFailed());
    Assertions.assertTrue(result.hasFailedEvaluations());
    Assertions.assertEquals(3L, result.evaluationCount());
    Assertions.assertEquals(3L, result.failedEvaluationCount());
    Assertions.assertEquals(384L, result.totalShots());
    Assertions.assertEquals("energy backend unavailable", result.diagnostic());
    Assertions.assertTrue(result.selectedExcitations().isEmpty());
  }

  @Test
  void adaptVqeRejectsMismatchedMitigationActiveSpace() {
    final AdaptVqeSelector selector = new AdaptVqeSelector(new StructureAwareBackend());
    final IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                selector.select(
                    ActiveSpace.of(2, 2),
                    adaptOptions(true),
                    AdaptVqeSelectionOptions.defaults(),
                    mitigation(ActiveSpace.of(2, 3))));

    Assertions.assertTrue(exception.getMessage().contains("must match"));
  }

  private static VariationalChemistryOptions adaptOptions(final boolean includeMeasurements) {
    return VariationalChemistryOptions.of(
        ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL,
        "adapt_vqe",
        "q",
        "c",
        "theta",
        1,
        8,
        includeMeasurements);
  }

  private static ErrorMitigationRequest mitigation(final ActiveSpace activeSpace) {
    return ErrorMitigationRequest.of(
        activeSpace,
        QuantumNoiseModel.ideal(),
        List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
        512,
        true,
        true);
  }

  private static final class StructureAwareBackend implements VqeEnergyBackend {

    @Override
    public VqeEnergyEvaluation evaluate(
        final QuantumProgram program,
        final double[] parameters,
        final int shots,
        final ErrorMitigationPlan mitigationPlan) {
      if (parameters.length == 0) {
        return VqeEnergyEvaluation.success(0.0, 0.0, shots);
      }
      if (StructureAwareBackend.containsDoubleExcitationParameter(program)) {
        return VqeEnergyEvaluation.success(-0.25, 0.0, shots);
      }
      return VqeEnergyEvaluation.success(-0.01, 0.0, shots);
    }

    private static boolean containsDoubleExcitationParameter(final QuantumProgram program) {
      for (int circuitIndex = 0; circuitIndex < program.circuitCount(); ++circuitIndex) {
        for (int operationIndex = 0;
            operationIndex < program.circuit(circuitIndex).operationCount();
            ++operationIndex) {
          final Operation operation = program.circuit(circuitIndex).operation(operationIndex);
          if (operation.kind() != OperationKind.GATE) {
            continue;
          }
          final GateOperation gateOperation = (GateOperation) operation;
          for (int parameterIndex = 0;
              parameterIndex < gateOperation.parameterCount();
              ++parameterIndex) {
            if (StructureAwareBackend.containsDoubleSymbol(
                gateOperation.parameter(parameterIndex))) {
              return true;
            }
          }
        }
      }
      return false;
    }

    private static boolean containsDoubleSymbol(final ParameterExpression expression) {
      if (expression.isNamed()) {
        return expression.name().contains("_double_");
      }
      if (expression.isUnary()) {
        return StructureAwareBackend.containsDoubleSymbol(expression.left());
      }
      if (expression.isBinary()) {
        return StructureAwareBackend.containsDoubleSymbol(expression.left())
            || StructureAwareBackend.containsDoubleSymbol(expression.right());
      }
      return false;
    }
  }
}
