/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.qpe;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.api.QuantumProgramBuilder;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.qpe.ChemistryPhaseEstimationOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/** Строит Quantum Phase Estimation circuit для mapped chemistry Hamiltonian. */
public final class ChemistryPhaseEstimationProgramGenerator {

  private static final double HERMITIAN_TOLERANCE = 1.0E-10;

  private final ElectronicHamiltonianQubitMapper qubitMapper;

  public ChemistryPhaseEstimationProgramGenerator() {
    this(new ElectronicHamiltonianQubitMapper());
  }

  public ChemistryPhaseEstimationProgramGenerator(
      final ElectronicHamiltonianQubitMapper qubitMapper) {
    if (qubitMapper == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian qubit mapper must not be null.");
    }
    this.qubitMapper = qubitMapper;
  }

  public QuantumProgram generate(
      final ElectronicHamiltonian hamiltonian, final ChemistryPhaseEstimationOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    final ChemistryPhaseEstimationOptions checkedOptions =
        options == null ? ChemistryPhaseEstimationOptions.defaults() : options;
    final QubitPauliOperator qubitHamiltonian =
        this.qubitMapper.map(hamiltonian, checkedOptions.mapping());
    return this.generate(hamiltonian, qubitHamiltonian, checkedOptions);
  }

  public QuantumProgram generate(
      final ElectronicHamiltonian hamiltonian,
      final QubitPauliOperator qubitHamiltonian,
      final ChemistryPhaseEstimationOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    if (qubitHamiltonian == null) {
      throw new IllegalArgumentException("QPE mapped qubit Hamiltonian must not be null.");
    }
    final ChemistryPhaseEstimationOptions checkedOptions =
        options == null ? ChemistryPhaseEstimationOptions.defaults() : options;
    if (!qubitHamiltonian.hermitianWithinTolerance(HERMITIAN_TOLERANCE)) {
      throw new IllegalArgumentException("QPE Hamiltonian must be Hermitian.");
    }
    final int systemQubitCount =
        Math.max(hamiltonian.spinOrbitalCount(), qubitHamiltonian.requiredQubitCount());
    final QuantumProgramBuilder programBuilder = Quantum.programBuilder();
    final QuantumCircuitBuilder circuitBuilder =
        programBuilder
            .circuit(checkedOptions.circuitName())
            .qreg(checkedOptions.systemRegisterName(), systemQubitCount)
            .qreg(checkedOptions.phaseRegisterName(), checkedOptions.phaseQubitCount());
    if (checkedOptions.includeMeasurements()) {
      circuitBuilder.creg(checkedOptions.classicalRegisterName(), checkedOptions.phaseQubitCount());
    }
    this.appendReferenceState(circuitBuilder, hamiltonian, checkedOptions);
    this.appendPhaseSuperposition(circuitBuilder, checkedOptions);
    this.appendControlledEvolutions(circuitBuilder, qubitHamiltonian, checkedOptions);
    if (checkedOptions.includeInverseQft()) {
      this.appendInverseQft(circuitBuilder, checkedOptions);
    }
    this.appendMeasurements(circuitBuilder, checkedOptions);
    return circuitBuilder.endCircuit().build();
  }

  private void appendReferenceState(
      final QuantumCircuitBuilder circuitBuilder,
      final ElectronicHamiltonian hamiltonian,
      final ChemistryPhaseEstimationOptions options) {
    if (!options.prepareHartreeFockReference()) {
      return;
    }
    for (int electronIndex = 0;
        electronIndex < hamiltonian.activeSpace().electronCount();
        ++electronIndex) {
      circuitBuilder.x(this.systemQubit(options, electronIndex));
    }
  }

  private void appendPhaseSuperposition(
      final QuantumCircuitBuilder circuitBuilder, final ChemistryPhaseEstimationOptions options) {
    for (int phaseIndex = 0; phaseIndex < options.phaseQubitCount(); ++phaseIndex) {
      circuitBuilder.h(this.phaseQubit(options, phaseIndex));
    }
  }

  private void appendControlledEvolutions(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliOperator hamiltonian,
      final ChemistryPhaseEstimationOptions options) {
    final double stepTime = options.evolutionTime() / (double) options.trotterSteps();
    for (int phaseIndex = 0; phaseIndex < options.phaseQubitCount(); ++phaseIndex) {
      final int repetitionCount = Math.toIntExact(1L << phaseIndex);
      final String controlQubit = this.phaseQubit(options, phaseIndex);
      for (int repetitionIndex = 0; repetitionIndex < repetitionCount; ++repetitionIndex) {
        for (int trotterStep = 0; trotterStep < options.trotterSteps(); ++trotterStep) {
          this.appendControlledTrotterStep(
              circuitBuilder, hamiltonian, options, controlQubit, stepTime);
        }
      }
    }
  }

  private void appendControlledTrotterStep(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliOperator hamiltonian,
      final ChemistryPhaseEstimationOptions options,
      final String controlQubit,
      final double stepTime) {
    for (int termIndex = 0; termIndex < hamiltonian.termCount(); ++termIndex) {
      final QubitPauliTerm term = hamiltonian.term(termIndex);
      final ComplexCoefficient coefficient = hamiltonian.coefficient(termIndex);
      if (!coefficient.effectivelyReal()) {
        throw new IllegalArgumentException("QPE Pauli term coefficient must be real.");
      }
      final double angle = 2.0 * coefficient.real() * stepTime;
      if (term.identity()) {
        circuitBuilder.phase(angle, controlQubit);
      } else {
        this.appendControlledPauliRotation(circuitBuilder, options, controlQubit, term, angle);
      }
    }
  }

  private void appendControlledPauliRotation(
      final QuantumCircuitBuilder circuitBuilder,
      final ChemistryPhaseEstimationOptions options,
      final String controlQubit,
      final QubitPauliTerm term,
      final double angle) {
    this.applyBasisChange(circuitBuilder, options, term);
    for (int i = 0; i < term.length() - 1; ++i) {
      circuitBuilder.cx(
          this.systemQubit(options, term.qubit(i)), this.systemQubit(options, term.qubit(i + 1)));
    }
    circuitBuilder.cphase(
        angle, controlQubit, this.systemQubit(options, term.qubit(term.length() - 1)));
    for (int i = term.length() - 2; i >= 0; --i) {
      circuitBuilder.cx(
          this.systemQubit(options, term.qubit(i)), this.systemQubit(options, term.qubit(i + 1)));
    }
    this.undoBasisChange(circuitBuilder, options, term);
  }

  private void applyBasisChange(
      final QuantumCircuitBuilder circuitBuilder,
      final ChemistryPhaseEstimationOptions options,
      final QubitPauliTerm term) {
    for (int i = 0; i < term.length(); ++i) {
      final String qubit = this.systemQubit(options, term.qubit(i));
      if (term.axis(i) == PauliAxis.X) {
        circuitBuilder.h(qubit);
      } else if (term.axis(i) == PauliAxis.Y) {
        circuitBuilder.rx(Math.PI / 2.0, qubit);
      }
    }
  }

  private void undoBasisChange(
      final QuantumCircuitBuilder circuitBuilder,
      final ChemistryPhaseEstimationOptions options,
      final QubitPauliTerm term) {
    for (int i = term.length() - 1; i >= 0; --i) {
      final String qubit = this.systemQubit(options, term.qubit(i));
      if (term.axis(i) == PauliAxis.X) {
        circuitBuilder.h(qubit);
      } else if (term.axis(i) == PauliAxis.Y) {
        circuitBuilder.rx(-Math.PI / 2.0, qubit);
      }
    }
  }

  private void appendInverseQft(
      final QuantumCircuitBuilder circuitBuilder, final ChemistryPhaseEstimationOptions options) {
    for (int left = 0; left < options.phaseQubitCount() / 2; ++left) {
      circuitBuilder.swap(
          this.phaseQubit(options, left),
          this.phaseQubit(options, options.phaseQubitCount() - left - 1));
    }
    for (int target = 0; target < options.phaseQubitCount(); ++target) {
      for (int control = 0; control < target; ++control) {
        circuitBuilder.cphase(
            -Math.PI / (double) (1L << (target - control)),
            this.phaseQubit(options, control),
            this.phaseQubit(options, target));
      }
      circuitBuilder.h(this.phaseQubit(options, target));
    }
  }

  private void appendMeasurements(
      final QuantumCircuitBuilder circuitBuilder, final ChemistryPhaseEstimationOptions options) {
    if (!options.includeMeasurements()) {
      return;
    }
    for (int phaseIndex = 0; phaseIndex < options.phaseQubitCount(); ++phaseIndex) {
      circuitBuilder.measure(
          this.phaseQubit(options, phaseIndex),
          options.classicalRegisterName() + "[" + phaseIndex + "]");
    }
  }

  private String systemQubit(final ChemistryPhaseEstimationOptions options, final int index) {
    return options.systemRegisterName() + "[" + index + "]";
  }

  private String phaseQubit(final ChemistryPhaseEstimationOptions options, final int index) {
    return options.phaseRegisterName() + "[" + index + "]";
  }
}
