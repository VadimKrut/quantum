/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Выполняет opt-in TDHF/TDDFT excited-state расчет через PySCF.
 */
public final class PyScfExcitedStateExecutor {

  private static final String STATE_PREFIX = "QUANTUM_PYSCF_EXCITED_STATE ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final String EXCITED_STATE_COUNT_OPTION = "excited_state_count";
  private static final long DEFAULT_TIMEOUT_SECONDS = 300L;
  private static final int DEFAULT_EXCITED_STATE_COUNT = 5;
  private static final double HARTREE_TO_EV = 27.211386245988;
  private static final double EV_NM_PRODUCT = 1239.8419843320026;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF excited-state request must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("PySCF excited-state molecule must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("PySCF excited-state calculation kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "PySCF excited-state diagnostics target must not be null.");
    }
    if (!this.supportedMethod(request)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().type().name()),
              "PySCF excited-state adapter supports TDHF and TDDFT workflows."));
      return null;
    }
    final String pythonCode = this.pythonCode(request, molecule);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfExcitedStateExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfExcitedStateProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.stateAvailable()) {
        return this.resultSection(request, kind, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "PySCF excited-state backend failed for all configured commands: "
                + String.join(" | ", failures)));
    return null;
  }

  private boolean supportedMethod(final ClassicalCalculationRequest request) {
    return request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
        || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final PyScfExcitedStateProcessResult processResult,
      final List<String> command) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF",
        this.methodName(request),
        "excited_state_analysis",
        "Result is produced by an external PySCF TD-HF/TD-DFT process and depends on installed backend convergence.");
    values.add(ClassicalResultValue.text("method", this.methodName(request)));
    values.add(ClassicalResultValue.text("backend", "PySCF"));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(ClassicalResultValue.scalar("state_count", processResult.rows().size(), "count"));
    values.add(
        ClassicalResultValue.of(
            "excited_state_rows",
            ClassicalResultValueKind.TABLE,
            "state,energy_hartree,energy_ev,wavelength_nm,oscillator_strength",
            processResult.rows()));
    return ClassicalResultSection.of(
        kind,
        kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
            ? "PySCF excited-state analysis"
            : "PySCF electronic spectroscopy prediction",
        values);
  }

  private String methodName(final ClassicalCalculationRequest request) {
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      return "pyscf_tddft_" + request.method().name().value();
    }
    return "pyscf_tdhf";
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured = PyScfExcitedStateExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfExcitedStateExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private PyScfExcitedStateProcessResult run(
      final List<String> command, final String pythonCode, final Duration timeout) {
    final ArrayList<String> fullCommand = new ArrayList<String>(command.size() + 2);
    fullCommand.addAll(command);
    fullCommand.add("-c");
    fullCommand.add(pythonCode);
    try {
      final Process process = new ProcessBuilder(fullCommand).redirectErrorStream(true).start();
      final StringBuilder output = new StringBuilder(2048);
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line = reader.readLine();
        while (line != null) {
          output.append(line).append('\n');
          line = reader.readLine();
        }
      }
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        return PyScfExcitedStateProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfExcitedStateProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfExcitedStateProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfExcitedStateProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(final ClassicalCalculationRequest request, final Molecule molecule) {
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    final boolean densityFunctional =
        request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
    final int stateCount =
        PyScfExcitedStateExecutor.integerOption(
            request, EXCITED_STATE_COUNT_OPTION, DEFAULT_EXCITED_STATE_COUNT);
    return ""
        + "from pyscf import gto, scf, dft, tdscf\n"
        + "atom = "
        + this.pythonAtomList(molecule)
        + "\n"
        + "mol = gto.M(atom=atom, basis="
        + this.pythonString(this.pyscfBasisName(request))
        + ", charge="
        + molecule.charge().value()
        + ", spin="
        + (molecule.spinMultiplicity().value() - 1)
        + ", unit='Angstrom', verbose=0)\n"
        + "method = "
        + this.pythonString(method)
        + "\n"
        + "density_functional = "
        + PyScfExcitedStateExecutor.pythonBoolean(densityFunctional)
        + "\n"
        + "if density_functional:\n"
        + "    mf = dft.UKS(mol) if mol.spin else dft.RKS(mol)\n"
        + "    mf.xc = method\n"
        + "    mf.run()\n"
        + "    td = mf.TDDFT()\n"
        + "else:\n"
        + "    mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "    td = tdscf.TDHF(mf)\n"
        + "td.nstates = "
        + stateCount
        + "\n"
        + "energies = td.kernel()[0]\n"
        + "try:\n"
        + "    oscillator = td.oscillator_strength()\n"
        + "except Exception:\n"
        + "    oscillator = [float('nan')] * len(energies)\n"
        + "for idx, energy in enumerate(energies):\n"
        + "    ev = float(energy) * "
        + HARTREE_TO_EV
        + "\n"
        + "    wavelength = "
        + EV_NM_PRODUCT
        + " / ev if ev > 0.0 else float('nan')\n"
        + "    osc = float(oscillator[idx]) if idx < len(oscillator) else float('nan')\n"
        + "    print('"
        + STATE_PREFIX
        + "' + str(idx + 1) + ' ' + str(float(energy)) + ' ' + str(ev) + ' ' + str(wavelength) + ' ' + str(osc))\n";
  }

  private String pythonAtomList(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    final StringBuilder builder = new StringBuilder(atoms.size() * 48);
    builder.append('[');
    for (int i = 0; i < atoms.size(); ++i) {
      if (i > 0) {
        builder.append(',');
      }
      final Atom atom = atoms.get(i);
      if (!atom.hasCoordinate()) {
        throw new IllegalArgumentException(
            "PySCF excited-state analysis requires complete 3D coordinates.");
      }
      final Coordinate3D coordinate = atom.coordinate();
      builder
          .append('(')
          .append(this.pythonString(atom.element().symbol().value()))
          .append(",(")
          .append(coordinate.unit().toAngstrom(coordinate.x()))
          .append(',')
          .append(coordinate.unit().toAngstrom(coordinate.y()))
          .append(',')
          .append(coordinate.unit().toAngstrom(coordinate.z()))
          .append("))");
    }
    builder.append(']');
    return builder.toString();
  }

  private String pyscfBasisName(final ClassicalCalculationRequest request) {
    return request.basisSet().name().value().toLowerCase(Locale.ROOT);
  }

  private String pythonString(final String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }

  private static String pythonBoolean(final boolean value) {
    return value ? "True" : "False";
  }

  private static List<String> splitCommand(final String command) {
    final String[] parts = command.trim().split("\\s+");
    final ArrayList<String> result = new ArrayList<String>(parts.length);
    for (int i = 0; i < parts.length; ++i) {
      if (!parts[i].isBlank()) {
        result.add(parts[i]);
      }
    }
    return List.copyOf(result);
  }

  private static String textOption(
      final ClassicalCalculationRequest request, final String name, final String defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name)) {
        return option.value();
      }
    }
    return defaultValue;
  }

  private static long longOption(
      final ClassicalCalculationRequest request, final String name, final long defaultValue) {
    final String value = PyScfExcitedStateExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Classical calculation option " + name + " must be long.");
    }
  }

  private static int integerOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final String value = PyScfExcitedStateExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Classical calculation option " + name + " must be integer.");
    }
  }

  private static final class PyScfExcitedStateProcessResult {

    private final boolean stateAvailable;
    private final int exitCode;
    private final String message;
    private final List<String> rows;

    private PyScfExcitedStateProcessResult(
        final boolean stateAvailable,
        final int exitCode,
        final String message,
        final List<String> rows) {
      this.stateAvailable = stateAvailable;
      this.exitCode = exitCode;
      this.message = message;
      this.rows = rows;
    }

    static PyScfExcitedStateProcessResult failed(final String message) {
      return new PyScfExcitedStateProcessResult(false, -1, message, List.of());
    }

    static PyScfExcitedStateProcessResult parse(final int exitCode, final String output) {
      final ArrayList<String> rows = new ArrayList<String>();
      final String[] lines = output.split("\\R");
      for (int i = 0; i < lines.length; ++i) {
        final String line = lines[i].trim();
        if (!line.startsWith(STATE_PREFIX)) {
          continue;
        }
        rows.add(line.substring(STATE_PREFIX.length()).trim().replace(' ', ','));
      }
      if (rows.isEmpty()) {
        return new PyScfExcitedStateProcessResult(false, exitCode, output, List.of());
      }
      return new PyScfExcitedStateProcessResult(true, exitCode, output, List.copyOf(rows));
    }

    boolean stateAvailable() {
      return this.stateAvailable;
    }

    List<String> rows() {
      return this.rows;
    }

    String summary() {
      return "exit=" + this.exitCode + " " + this.message.replace('\n', ' ').replace('\r', ' ');
    }
  }
}
