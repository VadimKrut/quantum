/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.property;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class PyScfBenchmarkExecutor {

  private static final String RESULT_PREFIX = "QUANTUM_PYSCF_BENCHMARK ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final String BASIS_LIST_OPTION = "basis_set_list";
  private static final String METHOD_LIST_OPTION = "method_list";
  private static final long DEFAULT_TIMEOUT_SECONDS = 600L;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF benchmark request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("PySCF benchmark kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("PySCF benchmark diagnostics target must not be null.");
    }
    if (!(request.subject() instanceof Molecule)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.REQUEST,
              "PySCF benchmark backend requires molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    final String pythonCode = this.pythonCode(request, molecule, kind);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfBenchmarkExecutor.longOption(request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfBenchmarkProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.successful()) {
        return this.resultSection(request, kind, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "PySCF benchmark backend failed for all configured commands: "
                + String.join(" | ", failures)));
    return null;
  }

  public static boolean pyScfBackendRequested(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE) {
      return PyScfBenchmarkExecutor.optionEquals(request, "basis_convergence_backend", "pyscf")
          || PyScfBenchmarkExecutor.optionEquals(request, "benchmark_backend", "pyscf");
    }
    if (kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
      return PyScfBenchmarkExecutor.optionEquals(request, "method_benchmark_backend", "pyscf")
          || PyScfBenchmarkExecutor.optionEquals(request, "benchmark_backend", "pyscf");
    }
    return false;
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final PyScfBenchmarkProcessResult processResult,
      final List<String> command) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF",
        "pyscf_batch_single_point_benchmark",
        kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
            ? "basis_set_convergence"
            : "method_benchmark",
        "Benchmark rows are produced by an external PySCF process and depend on installed backend convergence.");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("backend", "PySCF"));
    values.add(ClassicalResultValue.text("backend_contract", "external_opt_in_reference_backend"));
    values.add(ClassicalResultValue.text("java_native_calculation", "false"));
    values.add(ClassicalResultValue.text("external_reference_route", "true"));
    values.add(ClassicalResultValue.text("external_package_parity_claimed", "true"));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.scalar("row_count", processResult.rows().size(), "count"));
    values.add(
        ClassicalResultValue.of(
            kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
                ? "pyscf_basis_convergence_rows"
                : "pyscf_method_benchmark_rows",
            ClassicalResultValueKind.TABLE,
            "axis,method,basis,total_energy,delta_from_first,converged",
            processResult.rows()));
    return ClassicalResultSection.of(kind, this.title(kind), values);
  }

  private PyScfBenchmarkProcessResult run(
      final List<String> command, final String pythonCode, final Duration timeout) {
    final ArrayList<String> fullCommand = new ArrayList<String>(command.size() + 2);
    fullCommand.addAll(command);
    fullCommand.add("-c");
    fullCommand.add(pythonCode);
    try {
      final Process process = new ProcessBuilder(fullCommand).redirectErrorStream(true).start();
      final StringBuilder output = new StringBuilder(1024);
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
        return PyScfBenchmarkProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfBenchmarkProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfBenchmarkProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfBenchmarkProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ClassicalCalculationKind kind) {
    final String baseMethod = request.method().name().value().toLowerCase(java.util.Locale.ROOT);
    final String basisList =
        kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
            ? PyScfBenchmarkExecutor.textOption(request, BASIS_LIST_OPTION, "sto-3g,3-21g,6-31g")
            : request.basisSet().name().value();
    final String methodList =
        kind == ClassicalCalculationKind.METHOD_BENCHMARK
            ? PyScfBenchmarkExecutor.textOption(request, METHOD_LIST_OPTION, "hf,mp2,ccsd")
            : baseMethod;
    return ""
        + "from pyscf import gto, scf, dft, mp, cc\n"
        + "atom = "
        + this.pythonAtomList(molecule)
        + "\n"
        + "basis_list = "
        + this.pythonStringList(basisList)
        + "\n"
        + "method_list = "
        + this.pythonStringList(methodList)
        + "\n"
        + "base_charge = "
        + molecule.charge().value()
        + "\n"
        + "base_spin = "
        + (molecule.spinMultiplicity().value() - 1)
        + "\n"
        + "def run_energy(method, basis):\n"
        + "    mol = gto.M(atom=atom, basis=basis, charge=base_charge, spin=base_spin, unit='Angstrom', verbose=0)\n"
        + "    m = method.lower()\n"
        + "    if m in ('hf', 'rhf', 'hartree-fock', 'hartree_fock'):\n"
        + "        mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "        return float(mf.e_tot), bool(getattr(mf, 'converged', True))\n"
        + "    if m in ('mp2', 'moller-plesset', 'moeller-plesset'):\n"
        + "        mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "        post = mp.UMP2(mf).run() if mol.spin else mp.MP2(mf).run()\n"
        + "        return float(post.e_tot), bool(getattr(mf, 'converged', True))\n"
        + "    if m == 'ccsd':\n"
        + "        mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "        post = cc.UCCSD(mf).run() if mol.spin else cc.CCSD(mf).run()\n"
        + "        return float(post.e_tot), bool(getattr(post, 'converged', True))\n"
        + "    mf = dft.UKS(mol) if mol.spin else dft.RKS(mol)\n"
        + "    mf.xc = method\n"
        + "    mf.run()\n"
        + "    return float(mf.e_tot), bool(getattr(mf, 'converged', True))\n"
        + "first = None\n"
        + "axis = 'basis' if len(basis_list) > 1 else 'method'\n"
        + "for method in method_list:\n"
        + "    for basis in basis_list:\n"
        + "        energy, converged = run_energy(method, basis)\n"
        + "        if first is None:\n"
        + "            first = energy\n"
        + "        print('"
        + RESULT_PREFIX
        + "ROW ' + axis + ' ' + method + ' ' + basis + ' ' + str(energy) + ' ' + str(energy - first) + ' ' + str(converged))\n";
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
            "PySCF benchmark backend requires complete 3D coordinates.");
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

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured = PyScfBenchmarkExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfBenchmarkExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private String title(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        ? "PySCF basis-set convergence"
        : "PySCF method benchmark";
  }

  private String pythonStringList(final String value) {
    final String[] parts = value.split(",");
    final StringBuilder builder = new StringBuilder(parts.length * 16);
    builder.append('[');
    for (int i = 0; i < parts.length; ++i) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(this.pythonString(parts[i].trim()));
    }
    builder.append(']');
    return builder.toString();
  }

  private String pythonString(final String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }

  private static boolean optionEquals(
      final ClassicalCalculationRequest request, final String name, final String expected) {
    final String value = PyScfBenchmarkExecutor.textOption(request, name, "");
    return expected.equalsIgnoreCase(value);
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
    final String value = PyScfBenchmarkExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    return Long.parseLong(value);
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

  private static final class PyScfBenchmarkProcessResult {

    private final boolean successful;
    private final List<String> rows;
    private final String summary;

    private PyScfBenchmarkProcessResult(
        final boolean successful, final List<String> rows, final String summary) {
      this.successful = successful;
      this.rows = List.copyOf(rows);
      this.summary = summary;
    }

    static PyScfBenchmarkProcessResult parse(final int exitCode, final String output) {
      if (exitCode != 0) {
        return PyScfBenchmarkProcessResult.failed(
            "PySCF benchmark exited with " + exitCode + ": " + output);
      }
      final ArrayList<String> rows = new ArrayList<String>();
      final String[] lines = output.split("\\R");
      for (int i = 0; i < lines.length; ++i) {
        final String line = lines[i];
        if (!line.startsWith(RESULT_PREFIX)) {
          continue;
        }
        final String[] parts = line.substring(RESULT_PREFIX.length()).trim().split("\\s+");
        if (parts.length >= 7 && parts[0].equals("ROW")) {
          rows.add(
              parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4] + "," + parts[5] + ","
                  + parts[6]);
        }
      }
      if (rows.isEmpty()) {
        return PyScfBenchmarkProcessResult.failed(
            "PySCF benchmark output did not contain rows: " + output);
      }
      return new PyScfBenchmarkProcessResult(true, rows, "ok");
    }

    static PyScfBenchmarkProcessResult failed(final String summary) {
      return new PyScfBenchmarkProcessResult(false, List.of(), summary);
    }

    boolean successful() {
      return this.successful;
    }

    List<String> rows() {
      return this.rows;
    }

    String summary() {
      return this.summary;
    }
  }
}
