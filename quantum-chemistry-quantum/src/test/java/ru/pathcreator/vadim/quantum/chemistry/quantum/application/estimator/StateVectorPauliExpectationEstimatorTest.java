/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.estimator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;

final class StateVectorPauliExpectationEstimatorTest {

  private static final double TOLERANCE = 1.0E-12;

  @Test
  void estimatorComputesSingleQubitZExpectation() {
    final QuantumProgram zero =
        Quantum.programBuilder().circuit("z_zero").qreg("q", 1).creg("c", 1).build();
    final QuantumProgram one =
        Quantum.programBuilder().circuit("z_one").qreg("q", 1).creg("c", 1).x("q[0]").build();
    final QubitPauliOperator z =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Z), ComplexCoefficient.real(1.0))
            .build();
    final StateVectorPauliExpectationEstimator estimator =
        new StateVectorPauliExpectationEstimator();

    Assertions.assertEquals(1.0, estimator.estimate(zero, z), TOLERANCE);
    Assertions.assertEquals(-1.0, estimator.estimate(one, z), TOLERANCE);
  }

  @Test
  void estimatorComputesXAndYExpectationInPreparedBases() {
    final QuantumProgram plus =
        Quantum.programBuilder().circuit("x_plus").qreg("q", 1).creg("c", 1).h("q[0]").build();
    final QuantumProgram yPlus =
        Quantum.programBuilder()
            .circuit("y_plus")
            .qreg("q", 1)
            .creg("c", 1)
            .h("q[0]")
            .phase(Math.PI / 2.0, "q[0]")
            .build();
    final QubitPauliOperator x =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.X), ComplexCoefficient.real(1.0))
            .build();
    final QubitPauliOperator y =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Y), ComplexCoefficient.real(1.0))
            .build();
    final StateVectorPauliExpectationEstimator estimator =
        new StateVectorPauliExpectationEstimator();

    Assertions.assertEquals(1.0, estimator.estimate(plus, x), TOLERANCE);
    Assertions.assertEquals(1.0, estimator.estimate(yPlus, y), TOLERANCE);
  }

  @Test
  void estimatorComputesBellStatePauliCorrelations() {
    final QuantumProgram bell =
        Quantum.programBuilder()
            .circuit("bell_correlations")
            .qreg("q", 2)
            .creg("c", 2)
            .h("q[0]")
            .cx("q[0]", "q[1]")
            .build();
    final QubitPauliOperator correlations =
        QubitPauliOperator.builder()
            .add(
                QubitPauliTerm.of(new int[] {0, 1}, new PauliAxis[] {PauliAxis.X, PauliAxis.X}),
                ComplexCoefficient.real(2.0))
            .add(
                QubitPauliTerm.of(new int[] {0, 1}, new PauliAxis[] {PauliAxis.Y, PauliAxis.Y}),
                ComplexCoefficient.real(3.0))
            .add(
                QubitPauliTerm.of(new int[] {0, 1}, new PauliAxis[] {PauliAxis.Z, PauliAxis.Z}),
                ComplexCoefficient.real(5.0))
            .build();

    final double expectation =
        new StateVectorPauliExpectationEstimator().estimate(bell, correlations);

    Assertions.assertEquals(4.0, expectation, TOLERANCE);
  }

  @Test
  void estimatorUsesQubitIndexingConsistentWithGateApplication() {
    final QuantumProgram program =
        Quantum.programBuilder()
            .circuit("two_qubit_order")
            .qreg("q", 2)
            .creg("c", 2)
            .x("q[0]")
            .build();
    final QubitPauliOperator z0z1 =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Z), ComplexCoefficient.real(1.0))
            .add(QubitPauliTerm.of(1, PauliAxis.Z), ComplexCoefficient.real(10.0))
            .build();
    final StateVectorPauliExpectationEstimator estimator =
        new StateVectorPauliExpectationEstimator();

    Assertions.assertEquals(9.0, estimator.estimate(program, z0z1), TOLERANCE);
  }

  @Test
  void estimatorBindsVariationalParametersBeforeSimulation() {
    final QuantumProgram program =
        Quantum.programBuilder()
            .circuit("bound_rotation")
            .qreg("q", 1)
            .creg("c", 1)
            .rx(ParameterExpression.named("theta0"), "q[0]")
            .build();
    final QubitPauliOperator z =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.of(0, PauliAxis.Z), ComplexCoefficient.real(1.0))
            .build();
    final ParameterBindings bindings = ParameterBindings.builder().put("theta0", Math.PI).build();

    final double expectation =
        new StateVectorPauliExpectationEstimator().estimate(program, z, bindings, 1);

    Assertions.assertEquals(-1.0, expectation, TOLERANCE);
  }
}
