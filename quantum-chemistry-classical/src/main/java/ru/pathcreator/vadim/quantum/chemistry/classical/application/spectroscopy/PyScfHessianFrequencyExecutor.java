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
 * Выполняет opt-in analytic Hessian и harmonic frequency analysis через PySCF.
 */
public final class PyScfHessianFrequencyExecutor {

  private static final String META_PREFIX = "QUANTUM_PYSCF_HESS_META ";
  private static final String HESSIAN_PREFIX = "QUANTUM_PYSCF_HESS_ROW ";
  private static final String FREQUENCY_PREFIX = "QUANTUM_PYSCF_FREQ ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final long DEFAULT_TIMEOUT_SECONDS = 300L;
  private static final double WAVENUMBER_TO_KJ_PER_MOLE = 0.0119626565638697;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF Hessian request must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("PySCF Hessian molecule must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("PySCF Hessian calculation kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("PySCF Hessian diagnostics target must not be null.");
    }
    if (!this.supportedMethod(request)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().type().name()),
              "PySCF Hessian adapter currently supports HF and DFT Hessian workflows."));
      return null;
    }
    final String pythonCode = this.pythonCode(request, molecule);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfHessianFrequencyExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfHessianProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.hessianAvailable()) {
        return this.resultSection(request, molecule, kind, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "PySCF Hessian backend failed for all configured commands: "
                + String.join(" | ", failures)));
    return null;
  }

  private boolean supportedMethod(final ClassicalCalculationRequest request) {
    return request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
        || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ClassicalCalculationKind kind,
      final PyScfHessianProcessResult processResult,
      final List<String> command) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF",
        "pyscf_analytic_hessian",
        kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
            ? "hessian_analysis"
            : "frequency_analysis",
        "Result is produced by an external PySCF process and depends on installed backend convergence.");
    values.add(ClassicalResultValue.text("method", "pyscf_analytic_hessian"));
    values.add(ClassicalResultValue.text("backend", "PySCF"));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(processResult.converged())));
    values.add(ClassicalResultValue.scalar("energy", processResult.energy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("hessian_dimension", 3.0 * molecule.atomCount(), "coordinate"));
    values.add(
        ClassicalResultValue.of(
            "hessian_rows",
            ClassicalResultValueKind.TABLE,
            "hartree/bohr^2",
            processResult.hessianRows()));
    values.add(
        ClassicalResultValue.scalar("mode_count", processResult.frequencies().length, "count"));
    values.add(
        ClassicalResultValue.of(
            "frequencies",
            ClassicalResultValueKind.VECTOR,
            "cm^-1",
            PyScfHessianFrequencyExecutor.frequencyValues(processResult.frequencies())));
    values.add(
        ClassicalResultValue.scalar(
            "zero_point_energy",
            PyScfHessianFrequencyExecutor.zeroPointEnergyKilojoulePerMole(
                processResult.frequencies()),
            "kJ/mol"));
    return ClassicalResultSection.of(
        kind,
        kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
            ? "PySCF analytic Hessian analysis"
            : "PySCF harmonic frequency analysis",
        values);
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfHessianFrequencyExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfHessianFrequencyExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private PyScfHessianProcessResult run(
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
        return PyScfHessianProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfHessianProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfHessianProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfHessianProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(final ClassicalCalculationRequest request, final Molecule molecule) {
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    final boolean densityFunctional =
        request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
    return ""
        + "from pyscf import gto, scf, dft\n"
        + "from pyscf.hessian import thermo\n"
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
        + PyScfHessianFrequencyExecutor.pythonBoolean(densityFunctional)
        + "\n"
        + "if density_functional:\n"
        + "    mf = dft.UKS(mol) if mol.spin else dft.RKS(mol)\n"
        + "    mf.xc = method\n"
        + "    mf.run()\n"
        + "else:\n"
        + "    mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "hess = mf.Hessian().kernel()\n"
        + "freq_info = thermo.harmonic_analysis(mol, hess)\n"
        + "print('"
        + META_PREFIX
        + "' + str(float(mf.e_tot)) + ' ' + str(bool(getattr(mf, 'converged', True))))\n"
        + "for i in range(hess.shape[0]):\n"
        + "    for j in range(hess.shape[1]):\n"
        + "        for a in range(3):\n"
        + "            for b in range(3):\n"
        + "                print('"
        + HESSIAN_PREFIX
        + "' + str(i) + ' ' + str(a) + ' ' + str(j) + ' ' + str(b) + ' ' + str(float(hess[i,j,a,b])))\n"
        + "for value in freq_info.get('freq_wavenumber', []):\n"
        + "    print('"
        + FREQUENCY_PREFIX
        + "' + str(float(value)))\n";
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
        throw new IllegalArgumentException("PySCF Hessian requires complete 3D coordinates.");
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

  private static double zeroPointEnergyKilojoulePerMole(final double[] frequencies) {
    double result = 0.0;
    for (int i = 0; i < frequencies.length; ++i) {
      result += 0.5 * frequencies[i] * WAVENUMBER_TO_KJ_PER_MOLE;
    }
    return result;
  }

  private static List<String> frequencyValues(final double[] frequencies) {
    final ArrayList<String> values = new ArrayList<String>(frequencies.length);
    for (int i = 0; i < frequencies.length; ++i) {
      values.add(Double.toString(frequencies[i]));
    }
    return List.copyOf(values);
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
    final String value = PyScfHessianFrequencyExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Classical calculation option " + name + " must be long.");
    }
  }

  private static final class PyScfHessianProcessResult {

    private final boolean hessianAvailable;
    private final int exitCode;
    private final String message;
    private final double energy;
    private final List<String> hessianRows;
    private final double[] frequencies;
    private final boolean converged;

    private PyScfHessianProcessResult(
        final boolean hessianAvailable,
        final int exitCode,
        final String message,
        final double energy,
        final List<String> hessianRows,
        final double[] frequencies,
        final boolean converged) {
      this.hessianAvailable = hessianAvailable;
      this.exitCode = exitCode;
      this.message = message;
      this.energy = energy;
      this.hessianRows = hessianRows;
      this.frequencies = frequencies;
      this.converged = converged;
    }

    static PyScfHessianProcessResult failed(final String message) {
      return new PyScfHessianProcessResult(
          false, -1, message, Double.NaN, List.of(), new double[0], false);
    }

    static PyScfHessianProcessResult parse(final int exitCode, final String output) {
      double energy = Double.NaN;
      boolean converged = false;
      final ArrayList<String> hessianRows = new ArrayList<String>();
      final ArrayList<Double> frequencies = new ArrayList<Double>();
      final String[] lines = output.split("\\R");
      for (int i = 0; i < lines.length; ++i) {
        final String line = lines[i].trim();
        if (line.startsWith(META_PREFIX)) {
          final String[] parts = line.substring(META_PREFIX.length()).trim().split("\\s+");
          if (parts.length >= 2) {
            energy = Double.parseDouble(parts[0]);
            converged = Boolean.parseBoolean(parts[1]);
          }
          continue;
        }
        if (line.startsWith(HESSIAN_PREFIX)) {
          hessianRows.add(line.substring(HESSIAN_PREFIX.length()).trim().replace(' ', ','));
          continue;
        }
        if (line.startsWith(FREQUENCY_PREFIX)) {
          frequencies.add(Double.valueOf(line.substring(FREQUENCY_PREFIX.length()).trim()));
        }
      }
      if (Double.isNaN(energy) || hessianRows.isEmpty()) {
        return new PyScfHessianProcessResult(
            false, exitCode, output, Double.NaN, List.of(), new double[0], false);
      }
      final double[] frequencyArray = new double[frequencies.size()];
      for (int i = 0; i < frequencies.size(); ++i) {
        frequencyArray[i] = frequencies.get(i).doubleValue();
      }
      return new PyScfHessianProcessResult(
          true, exitCode, output, energy, List.copyOf(hessianRows), frequencyArray, converged);
    }

    boolean hessianAvailable() {
      return this.hessianAvailable;
    }

    double energy() {
      return this.energy;
    }

    List<String> hessianRows() {
      return this.hessianRows;
    }

    double[] frequencies() {
      return this.frequencies.clone();
    }

    boolean converged() {
      return this.converged;
    }

    String summary() {
      return "exit=" + this.exitCode + " " + this.message.replace('\n', ' ').replace('\r', ' ');
    }
  }
}
