/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

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
 * Запускает opt-in analytic gradient через установленный PySCF backend и возвращает градиент в
 * единицах Hartree/Angstrom, совместимых с Java-native geometry results.
 */
public final class PyScfGeometryGradientExecutor {

  private static final String META_PREFIX = "QUANTUM_PYSCF_GRAD_META ";
  private static final String ROW_PREFIX = "QUANTUM_PYSCF_GRAD_ROW ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final double BOHR_TO_ANGSTROM = 0.529177210903;
  private static final long DEFAULT_TIMEOUT_SECONDS = 300L;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF gradient request must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("PySCF gradient molecule must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("PySCF gradient diagnostics target must not be null.");
    }
    if (!this.supportedMethod(request)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().type().name()),
              "PySCF analytic gradient adapter currently supports HF, DFT, MP2 and CCSD gradients."));
      return null;
    }
    final String pythonCode = this.pythonCode(request, molecule);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfGeometryGradientExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfGradientProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.gradientAvailable()) {
        return this.resultSection(request, molecule, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.GEOMETRY_GRADIENT.name()),
            "PySCF analytic gradient backend failed for all configured commands: "
                + String.join(" | ", failures)));
    return null;
  }

  private boolean supportedMethod(final ClassicalCalculationRequest request) {
    return request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
        || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        || (request.method().type() == ElectronicStructureMethodType.PERTURBATION_THEORY
            && this.mp2Method(request.method().name().value()))
        || (request.method().type() == ElectronicStructureMethodType.COUPLED_CLUSTER
            && this.ccsdMethod(request.method().name().value()));
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final PyScfGradientProcessResult processResult,
      final List<String> command) {
    final double[] coordinates = GeometryCoordinateSupport.coordinatesAngstrom(molecule);
    final double[] gradient = processResult.gradient();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF",
        "pyscf_analytic_gradient",
        "geometry_gradient",
        "Result is produced by an external PySCF process and depends on the installed PySCF version and backend convergence.");
    values.add(ClassicalResultValue.text("method", "pyscf_analytic_gradient"));
    values.add(ClassicalResultValue.text("backend", "PySCF"));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(processResult.converged())));
    values.add(ClassicalResultValue.scalar("energy", processResult.energy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "gradient_norm", GeometryCoordinateSupport.norm(gradient), "hartree/angstrom"));
    values.add(
        ClassicalResultValue.scalar(
            "max_gradient_component",
            GeometryCoordinateSupport.maxAbsolute(gradient),
            "hartree/angstrom"));
    values.add(
        ClassicalResultValue.of(
            "gradient_rows",
            ClassicalResultValueKind.TABLE,
            "hartree/angstrom",
            PyScfGeometryGradientExecutor.gradientRows(molecule, coordinates, gradient)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.GEOMETRY_GRADIENT, "PySCF analytic geometry gradient", values);
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfGeometryGradientExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfGeometryGradientExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private PyScfGradientProcessResult run(
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
        return PyScfGradientProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfGradientProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfGradientProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfGradientProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(final ClassicalCalculationRequest request, final Molecule molecule) {
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    final boolean densityFunctional =
        request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
    return ""
        + "from pyscf import gto, scf, dft, mp, cc\n"
        + "BOHR_TO_ANGSTROM = "
        + BOHR_TO_ANGSTROM
        + "\n"
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
        + PyScfGeometryGradientExecutor.pythonBoolean(densityFunctional)
        + "\n"
        + "if density_functional:\n"
        + "    mf = dft.UKS(mol) if mol.spin else dft.RKS(mol)\n"
        + "    mf.xc = method\n"
        + "    mf.run()\n"
        + "else:\n"
        + "    mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "grad_source = mf\n"
        + "if not density_functional and (method == 'mp2' or 'moller' in method or 'moeller' in method):\n"
        + "    grad_source = mp.UMP2(mf).run() if mol.spin else mp.MP2(mf).run()\n"
        + "elif not density_functional and method == 'ccsd':\n"
        + "    grad_source = cc.UCCSD(mf).run() if mol.spin else cc.CCSD(mf).run()\n"
        + "grad = grad_source.nuc_grad_method().kernel() / BOHR_TO_ANGSTROM\n"
        + "print('"
        + META_PREFIX
        + "' + str(float(grad_source.e_tot)) + ' ' + str(bool(getattr(grad_source, 'converged', True))))\n"
        + "for idx, row in enumerate(grad):\n"
        + "    print('"
        + ROW_PREFIX
        + "' + str(idx) + ' ' + str(float(row[0])) + ' ' + str(float(row[1])) + ' ' + str(float(row[2])))\n";
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
            "PySCF analytic gradient requires complete 3D coordinates.");
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

  private boolean mp2Method(final String methodName) {
    final String normalized = methodName.toLowerCase(Locale.ROOT);
    return normalized.equals("mp2")
        || normalized.contains("moller")
        || normalized.contains("moeller")
        || normalized.contains("second order");
  }

  private boolean ccsdMethod(final String methodName) {
    return methodName.toLowerCase(Locale.ROOT).equals("ccsd");
  }

  private String pythonString(final String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }

  private static List<String> gradientRows(
      final Molecule molecule, final double[] coordinates, final double[] gradient) {
    final ArrayList<String> rows = new ArrayList<String>(molecule.atomCount());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final int offset = i * 3;
      final double atomNorm =
          Math.sqrt(
              gradient[offset] * gradient[offset]
                  + gradient[offset + 1] * gradient[offset + 1]
                  + gradient[offset + 2] * gradient[offset + 2]);
      rows.add(
          atoms.get(i).id().value()
              + ","
              + atoms.get(i).element().symbol().value()
              + ","
              + coordinates[offset]
              + ","
              + coordinates[offset + 1]
              + ","
              + coordinates[offset + 2]
              + ","
              + gradient[offset]
              + ","
              + gradient[offset + 1]
              + ","
              + gradient[offset + 2]
              + ","
              + atomNorm);
    }
    return List.copyOf(rows);
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
    final String value = PyScfGeometryGradientExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Classical calculation option " + name + " must be long.");
    }
  }

  private static final class PyScfGradientProcessResult {

    private final boolean gradientAvailable;
    private final int exitCode;
    private final String message;
    private final double energy;
    private final double[] gradient;
    private final boolean converged;

    private PyScfGradientProcessResult(
        final boolean gradientAvailable,
        final int exitCode,
        final String message,
        final double energy,
        final double[] gradient,
        final boolean converged) {
      this.gradientAvailable = gradientAvailable;
      this.exitCode = exitCode;
      this.message = message;
      this.energy = energy;
      this.gradient = gradient;
      this.converged = converged;
    }

    static PyScfGradientProcessResult failed(final String message) {
      return new PyScfGradientProcessResult(false, -1, message, Double.NaN, new double[0], false);
    }

    static PyScfGradientProcessResult parse(final int exitCode, final String output) {
      double energy = Double.NaN;
      boolean converged = false;
      final ArrayList<double[]> rows = new ArrayList<double[]>();
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
        if (!line.startsWith(ROW_PREFIX)) {
          continue;
        }
        final String[] parts = line.substring(ROW_PREFIX.length()).trim().split("\\s+");
        if (parts.length >= 4) {
          rows.add(
              new double[] {
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
              });
        }
      }
      if (Double.isNaN(energy) || rows.isEmpty()) {
        return new PyScfGradientProcessResult(
            false, exitCode, output, Double.NaN, new double[0], false);
      }
      final double[] gradient = new double[rows.size() * 3];
      for (int i = 0; i < rows.size(); ++i) {
        final double[] row = rows.get(i);
        final int offset = i * 3;
        gradient[offset] = row[0];
        gradient[offset + 1] = row[1];
        gradient[offset + 2] = row[2];
      }
      return new PyScfGradientProcessResult(true, exitCode, output, energy, gradient, converged);
    }

    boolean gradientAvailable() {
      return this.gradientAvailable;
    }

    double energy() {
      return this.energy;
    }

    double[] gradient() {
      return this.gradient.clone();
    }

    boolean converged() {
      return this.converged;
    }

    String summary() {
      return "exit=" + this.exitCode + " " + this.message.replace('\n', ' ').replace('\r', ' ');
    }
  }
}
