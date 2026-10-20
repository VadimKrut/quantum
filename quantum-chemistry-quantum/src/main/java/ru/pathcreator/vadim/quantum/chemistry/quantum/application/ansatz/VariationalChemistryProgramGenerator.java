/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz;

import java.util.List;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.api.QuantumProgramBuilder;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryExcitationRank;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitationPool;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Создает Quantum IR-программы для quantum chemistry workflows по active-space описанию.
 */
public final class VariationalChemistryProgramGenerator {

  private final FermionicExcitationEvolutionGenerator excitationEvolutionGenerator;

  public VariationalChemistryProgramGenerator() {
    this(new FermionicExcitationEvolutionGenerator());
  }

  public VariationalChemistryProgramGenerator(
      final FermionicExcitationEvolutionGenerator excitationEvolutionGenerator) {
    if (excitationEvolutionGenerator == null) {
      throw new IllegalArgumentException(
          "Variational chemistry excitation evolution generator must not be null.");
    }
    this.excitationEvolutionGenerator = excitationEvolutionGenerator;
  }

  /**
   * Строит программу с настройками по умолчанию.
   *
   * @param activeSpace active-space задача
   * @return Quantum IR-программа
   */
  public QuantumProgram generate(final ActiveSpace activeSpace) {
    return generate(activeSpace, VariationalChemistryOptions.defaults());
  }

  /**
   * Строит Quantum IR-программу для выбранного семейства chemistry ansatz.
   *
   * @param activeSpace active-space задача
   * @param options настройки генерации
   * @return Quantum IR-программа
   */
  public QuantumProgram generate(
      final ActiveSpace activeSpace, final VariationalChemistryOptions options) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Active space must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("Variational chemistry options must not be null.");
    }
    final QuantumProgramBuilder programBuilder = Quantum.programBuilder();
    final QuantumCircuitBuilder circuitBuilder =
        programBuilder
            .circuit(options.circuitName())
            .qreg(options.quantumRegisterName(), activeSpace.spinOrbitalCount());
    if (options.ansatzKind() == ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION) {
      circuitBuilder.qreg(options.phaseRegisterName(), options.phaseQubitCount());
    }
    if (options.includeMeasurements()) {
      circuitBuilder.creg(
          options.classicalRegisterName(), classicalRegisterSize(activeSpace, options));
    }
    appendHartreeFockReference(circuitBuilder, activeSpace, options);
    this.appendVariationalLayer(circuitBuilder, activeSpace, options);
    appendMeasurements(circuitBuilder, activeSpace, options);
    return circuitBuilder.endCircuit().build();
  }

  /**
   * Строит ADAPT/UCC ansatz только из явно выбранных возбуждений.
   *
   * @param activeSpace active-space задача
   * @param options настройки регистров и измерений
   * @param selectedExcitations выбранные single/double возбуждения
   * @return Quantum IR-программа выбранного ansatz
   */
  public QuantumProgram generateSelectedExcitations(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options,
      final List<UccsdExcitation> selectedExcitations) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("Active space must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("Variational chemistry options must not be null.");
    }
    if (selectedExcitations == null) {
      throw new IllegalArgumentException("Selected excitation list must not be null.");
    }
    final QuantumProgramBuilder programBuilder = Quantum.programBuilder();
    final QuantumCircuitBuilder circuitBuilder =
        programBuilder
            .circuit(options.circuitName())
            .qreg(options.quantumRegisterName(), activeSpace.spinOrbitalCount());
    if (options.includeMeasurements()) {
      circuitBuilder.creg(options.classicalRegisterName(), activeSpace.spinOrbitalCount());
    }
    appendHartreeFockReference(circuitBuilder, activeSpace, options);
    for (int i = 0; i < selectedExcitations.size(); ++i) {
      final UccsdExcitation excitation = selectedExcitations.get(i);
      requireExcitationInsideActiveSpace(activeSpace, excitation);
      this.appendExcitation(circuitBuilder, activeSpace, options, excitation);
    }
    appendMeasurements(circuitBuilder, activeSpace, options);
    return circuitBuilder.endCircuit().build();
  }

  private static void appendHartreeFockReference(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options) {
    for (int electronIndex = 0; electronIndex < activeSpace.electronCount(); ++electronIndex) {
      circuitBuilder.x(qubit(options, electronIndex));
    }
  }

  private void appendVariationalLayer(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options) {
    if (options.ansatzKind() == ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION) {
      appendQuantumPhaseEstimation(circuitBuilder, activeSpace, options);
      return;
    }
    if (options.ansatzKind() == ChemistryAnsatzKind.HARTREE_FOCK_REFERENCE) {
      return;
    }
    if (options.ansatzKind() == ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE) {
      appendHardwareEfficientLayers(circuitBuilder, activeSpace, options);
      return;
    }
    if (options.ansatzKind() == ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL) {
      this.appendExcitationPool(circuitBuilder, activeSpace, options, options.adaptOperatorLimit());
      return;
    }
    this.appendExcitationPool(circuitBuilder, activeSpace, options, Integer.MAX_VALUE);
  }

  private static void appendHardwareEfficientLayers(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options) {
    final int qubitCount = activeSpace.spinOrbitalCount();
    for (int layerIndex = 0; layerIndex < options.layerCount(); ++layerIndex) {
      for (int qubitIndex = 0; qubitIndex < qubitCount; ++qubitIndex) {
        circuitBuilder.ry(
            parameter(options, "he", layerIndex, qubitIndex, "y"), qubit(options, qubitIndex));
        circuitBuilder.rz(
            parameter(options, "he", layerIndex, qubitIndex, "z"), qubit(options, qubitIndex));
      }
      for (int qubitIndex = 0; qubitIndex < qubitCount - 1; ++qubitIndex) {
        circuitBuilder.cx(qubit(options, qubitIndex), qubit(options, qubitIndex + 1));
      }
    }
  }

  private static void appendQuantumPhaseEstimation(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options) {
    for (int phaseIndex = 0; phaseIndex < options.phaseQubitCount(); ++phaseIndex) {
      circuitBuilder.h(phaseQubit(options, phaseIndex));
    }
    for (int phaseIndex = 0; phaseIndex < options.phaseQubitCount(); ++phaseIndex) {
      appendControlledEvolution(circuitBuilder, activeSpace, options, phaseIndex);
    }
    if (options.includeInverseQft()) {
      appendInverseQft(circuitBuilder, options);
    }
  }

  private static void appendControlledEvolution(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options,
      final int phaseIndex) {
    final int repetitionCount =
        Math.toIntExact((long) options.controlledEvolutionRepetitions() << phaseIndex);
    for (int repetitionIndex = 0; repetitionIndex < repetitionCount; ++repetitionIndex) {
      for (int systemIndex = 0; systemIndex < activeSpace.spinOrbitalCount(); ++systemIndex) {
        circuitBuilder.cx(phaseQubit(options, phaseIndex), qubit(options, systemIndex));
        circuitBuilder.rz(
            parameter(options, "qpe_evolution", phaseIndex, repetitionIndex, systemIndex),
            qubit(options, systemIndex));
        circuitBuilder.cx(phaseQubit(options, phaseIndex), qubit(options, systemIndex));
      }
    }
  }

  private static void appendInverseQft(
      final QuantumCircuitBuilder circuitBuilder, final VariationalChemistryOptions options) {
    final int phaseQubitCount = options.phaseQubitCount();
    for (int left = 0; left < phaseQubitCount / 2; ++left) {
      circuitBuilder.swap(
          phaseQubit(options, left), phaseQubit(options, phaseQubitCount - left - 1));
    }
    for (int target = 0; target < phaseQubitCount; ++target) {
      for (int control = 0; control < target; ++control) {
        circuitBuilder.cx(phaseQubit(options, control), phaseQubit(options, target));
        circuitBuilder.rz(
            ParameterExpression.of(-Math.PI / (1L << (target - control))),
            phaseQubit(options, target));
        circuitBuilder.cx(phaseQubit(options, control), phaseQubit(options, target));
      }
      circuitBuilder.h(phaseQubit(options, target));
    }
  }

  private void appendExcitationPool(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options,
      final int operatorLimit) {
    final UccsdExcitationPool pool = UccsdExcitationPool.spinConserving(activeSpace);
    if (pool.excitationCount() == 0) {
      return;
    }
    final List<UccsdExcitation> excitations =
        pool.limited(Math.min(operatorLimit, pool.excitationCount()));
    for (int excitationIndex = 0; excitationIndex < excitations.size(); ++excitationIndex) {
      this.appendExcitation(circuitBuilder, activeSpace, options, excitations.get(excitationIndex));
    }
  }

  private void appendExcitation(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options,
      final UccsdExcitation excitation) {
    this.excitationEvolutionGenerator.appendExcitationEvolution(
        circuitBuilder,
        excitation,
        options.fermionToQubitMapping(),
        activeSpace.spinOrbitalCount(),
        parameter(options, excitation),
        options.quantumRegisterName());
  }

  private static void appendMeasurements(
      final QuantumCircuitBuilder circuitBuilder,
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions options) {
    if (!options.includeMeasurements()) {
      return;
    }
    if (options.ansatzKind() == ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION) {
      for (int phaseIndex = 0; phaseIndex < options.phaseQubitCount(); ++phaseIndex) {
        circuitBuilder.measure(phaseQubit(options, phaseIndex), classicalBit(options, phaseIndex));
      }
      return;
    }
    for (int qubitIndex = 0; qubitIndex < activeSpace.spinOrbitalCount(); ++qubitIndex) {
      circuitBuilder.measure(qubit(options, qubitIndex), classicalBit(options, qubitIndex));
    }
  }

  private static void requireExcitationInsideActiveSpace(
      final ActiveSpace activeSpace, final UccsdExcitation excitation) {
    if (excitation == null) {
      throw new IllegalArgumentException("Selected UCCSD excitation must not be null.");
    }
    for (int i = 0; i < excitation.occupiedCount(); ++i) {
      requireSpinOrbitalInsideActiveSpace(
          activeSpace, excitation.occupiedSpinOrbital(i), "occupied spin-orbital");
    }
    for (int i = 0; i < excitation.virtualCount(); ++i) {
      requireSpinOrbitalInsideActiveSpace(
          activeSpace, excitation.virtualSpinOrbital(i), "virtual spin-orbital");
    }
  }

  private static void requireSpinOrbitalInsideActiveSpace(
      final ActiveSpace activeSpace, final int spinOrbital, final String subjectName) {
    if (spinOrbital < 0 || spinOrbital >= activeSpace.spinOrbitalCount()) {
      throw new IllegalArgumentException(
          "Selected UCCSD " + subjectName + " must be inside active-space spin-orbital range.");
    }
  }

  private static int classicalRegisterSize(
      final ActiveSpace activeSpace, final VariationalChemistryOptions options) {
    if (options.ansatzKind() == ChemistryAnsatzKind.QUANTUM_PHASE_ESTIMATION) {
      return options.phaseQubitCount();
    }
    return activeSpace.spinOrbitalCount();
  }

  private static String qubit(final VariationalChemistryOptions options, final int index) {
    return options.quantumRegisterName() + "[" + index + "]";
  }

  private static String phaseQubit(final VariationalChemistryOptions options, final int index) {
    return options.phaseRegisterName() + "[" + index + "]";
  }

  private static String classicalBit(final VariationalChemistryOptions options, final int index) {
    return options.classicalRegisterName() + "[" + index + "]";
  }

  private static ParameterExpression parameter(
      final VariationalChemistryOptions options, final UccsdExcitation excitation) {
    if (excitation.rank() == ChemistryExcitationRank.SINGLE) {
      return parameter(
          options, "single", excitation.occupiedSpinOrbital(0), excitation.virtualSpinOrbital(0));
    }
    return parameter(
        options,
        "double",
        excitation.occupiedSpinOrbital(0),
        excitation.occupiedSpinOrbital(1),
        excitation.virtualSpinOrbital(0),
        excitation.virtualSpinOrbital(1));
  }

  private static ParameterExpression parameter(
      final VariationalChemistryOptions options,
      final String family,
      final int first,
      final int second) {
    return ParameterExpression.named(
        options.parameterPrefix() + "_" + family + "_" + first + "_" + second);
  }

  private static ParameterExpression parameter(
      final VariationalChemistryOptions options,
      final String family,
      final int first,
      final int second,
      final String suffix) {
    return ParameterExpression.named(
        options.parameterPrefix() + "_" + family + "_" + first + "_" + second + "_" + suffix);
  }

  private static ParameterExpression parameter(
      final VariationalChemistryOptions options,
      final String family,
      final int first,
      final int second,
      final int third) {
    return ParameterExpression.named(
        options.parameterPrefix() + "_" + family + "_" + first + "_" + second + "_" + third);
  }

  private static ParameterExpression parameter(
      final VariationalChemistryOptions options,
      final String family,
      final int first,
      final int second,
      final int third,
      final int fourth) {
    return ParameterExpression.named(
        options.parameterPrefix()
            + "_"
            + family
            + "_"
            + first
            + "_"
            + second
            + "_"
            + third
            + "_"
            + fourth);
  }
}
