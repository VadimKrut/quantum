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
 * Запускает opt-in geometry optimization через PySCF + geomeTRIC.
 */
public final class PyScfGeometryOptimizationExecutor {

  private static final String META_PREFIX = "QUANTUM_PYSCF_GEOM_META ";
  private static final String COORDINATE_PREFIX = "QUANTUM_PYSCF_GEOM_COORD ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final long DEFAULT_TIMEOUT_SECONDS = 600L;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final GeometryOptimizationOptions options,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF optimization request must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("PySCF optimization molecule must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("PySCF optimization options must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("PySCF optimization diagnostics target must not be null.");
    }
    if (!this.supportedMethod(request)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().type().name()),
              "PySCF geometry optimization adapter currently supports HF and DFT workflows."));
      return null;
    }
    final String pythonCode = this.pythonCode(request, molecule, options);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfGeometryOptimizationExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfGeometryOptimizationProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.optimizationAvailable()) {
        return this.resultSection(request, molecule, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.GEOMETRY_OPTIMIZATION.name()),
            "PySCF geometry optimization backend failed for all configured commands: "
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
      final PyScfGeometryOptimizationProcessResult processResult,
      final List<String> command) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF/geomeTRIC",
        "pyscf_geometric_optimization",
        "geometry_optimization",
        "Result is produced by external PySCF/geomeTRIC processes and depends on installed backend convergence.");
    values.add(ClassicalResultValue.text("method", "pyscf_geometric_optimization"));
    values.add(ClassicalResultValue.text("backend", "PySCF/geomeTRIC"));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(processResult.converged())));
    values.add(
        ClassicalResultValue.scalar("initial_energy", processResult.initialEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("final_energy", processResult.finalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("iteration_count", processResult.iterationCount(), "count"));
    values.add(
        ClassicalResultValue.of(
            "optimized_coordinates",
            ClassicalResultValueKind.TABLE,
            "angstrom",
            PyScfGeometryOptimizationExecutor.coordinateRows(
                molecule, processResult.coordinates())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.GEOMETRY_OPTIMIZATION, "PySCF geometry optimization", values);
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfGeometryOptimizationExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfGeometryOptimizationExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private PyScfGeometryOptimizationProcessResult run(
      final List<String> command, final String pythonCode, final Duration timeout) {
    final ArrayList<String> fullCommand = new ArrayList<String>(command.size() + 2);
    fullCommand.addAll(command);
    fullCommand.add("-c");
    fullCommand.add(pythonCode);
    try {
      final Process process = new ProcessBuilder(fullCommand).redirectErrorStream(true).start();
      final StringBuilder output = new StringBuilder(4096);
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
        return PyScfGeometryOptimizationProcessResult.failed(
            String.join(" ", command) + " timed out.");
      }
      return PyScfGeometryOptimizationProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfGeometryOptimizationProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfGeometryOptimizationProcessResult.failed(
          String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final GeometryOptimizationOptions options) {
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    final boolean densityFunctional =
        request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
    return ""
        + "from pyscf import gto, scf, dft\n"
        + "from pyscf.geomopt.geometric_solver import optimize\n"
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
        + PyScfGeometryOptimizationExecutor.pythonBoolean(densityFunctional)
        + "\n"
        + "if density_functional:\n"
        + "    mf = dft.UKS(mol) if mol.spin else dft.RKS(mol)\n"
        + "    mf.xc = method\n"
        + "    mf.run()\n"
        + "else:\n"
        + "    mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "initial = float(mf.e_tot)\n"
        + "optmol = optimize(mf, maxsteps="
        + options.maxIterations()
        + ")\n"
        + "if density_functional:\n"
        + "    mf2 = dft.UKS(optmol) if optmol.spin else dft.RKS(optmol)\n"
        + "    mf2.xc = method\n"
        + "    mf2.run()\n"
        + "else:\n"
        + "    mf2 = scf.UHF(optmol).run() if optmol.spin else scf.RHF(optmol).run()\n"
        + "coords = optmol.atom_coords(unit='Angstrom')\n"
        + "print('"
        + META_PREFIX
        + "' + str(initial) + ' ' + str(float(mf2.e_tot)) + ' ' + str(bool(getattr(mf2, 'converged', True))))\n"
        + "for idx, row in enumerate(coords):\n"
        + "    print('"
        + COORDINATE_PREFIX
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
            "PySCF geometry optimization requires complete 3D coordinates.");
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

  private static List<String> coordinateRows(final Molecule molecule, final double[] coordinates) {
    final ArrayList<String> rows = new ArrayList<String>(molecule.atomCount());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final int offset = i * 3;
      rows.add(
          atoms.get(i).id().value()
              + ","
              + atoms.get(i).element().symbol().value()
              + ","
              + coordinates[offset]
              + ","
              + coordinates[offset + 1]
              + ","
              + coordinates[offset + 2]);
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
    final String value = PyScfGeometryOptimizationExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Classical calculation option " + name + " must be long.");
    }
  }

  private static final class PyScfGeometryOptimizationProcessResult {

    private final boolean optimizationAvailable;
    private final int exitCode;
    private final String message;
    private final double initialEnergy;
    private final double finalEnergy;
    private final double[] coordinates;
    private final int iterationCount;
    private final boolean converged;

    private PyScfGeometryOptimizationProcessResult(
        final boolean optimizationAvailable,
        final int exitCode,
        final String message,
        final double initialEnergy,
        final double finalEnergy,
        final double[] coordinates,
        final int iterationCount,
        final boolean converged) {
      this.optimizationAvailable = optimizationAvailable;
      this.exitCode = exitCode;
      this.message = message;
      this.initialEnergy = initialEnergy;
      this.finalEnergy = finalEnergy;
      this.coordinates = coordinates;
      this.iterationCount = iterationCount;
      this.converged = converged;
    }

    static PyScfGeometryOptimizationProcessResult failed(final String message) {
      return new PyScfGeometryOptimizationProcessResult(
          false, -1, message, Double.NaN, Double.NaN, new double[0], 0, false);
    }

    static PyScfGeometryOptimizationProcessResult parse(final int exitCode, final String output) {
      double initialEnergy = Double.NaN;
      double finalEnergy = Double.NaN;
      boolean converged = false;
      int iterationCount = 0;
      final ArrayList<double[]> rows = new ArrayList<double[]>();
      final String[] lines = output.split("\\R");
      for (int i = 0; i < lines.length; ++i) {
        final String line = lines[i].trim();
        if (line.startsWith("Step ")) {
          ++iterationCount;
        }
        if (line.startsWith(META_PREFIX)) {
          final String[] parts = line.substring(META_PREFIX.length()).trim().split("\\s+");
          if (parts.length >= 3) {
            initialEnergy = Double.parseDouble(parts[0]);
            finalEnergy = Double.parseDouble(parts[1]);
            converged = Boolean.parseBoolean(parts[2]);
          }
          continue;
        }
        if (!line.startsWith(COORDINATE_PREFIX)) {
          continue;
        }
        final String[] parts = line.substring(COORDINATE_PREFIX.length()).trim().split("\\s+");
        if (parts.length >= 4) {
          rows.add(
              new double[] {
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
              });
        }
      }
      if (Double.isNaN(initialEnergy) || Double.isNaN(finalEnergy) || rows.isEmpty()) {
        return new PyScfGeometryOptimizationProcessResult(
            false, exitCode, output, Double.NaN, Double.NaN, new double[0], 0, false);
      }
      final double[] coordinates = new double[rows.size() * 3];
      for (int i = 0; i < rows.size(); ++i) {
        final double[] row = rows.get(i);
        final int offset = i * 3;
        coordinates[offset] = row[0];
        coordinates[offset + 1] = row[1];
        coordinates[offset + 2] = row[2];
      }
      return new PyScfGeometryOptimizationProcessResult(
          true,
          exitCode,
          output,
          initialEnergy,
          finalEnergy,
          coordinates,
          iterationCount,
          converged);
    }

    boolean optimizationAvailable() {
      return this.optimizationAvailable;
    }

    double initialEnergy() {
      return this.initialEnergy;
    }

    double finalEnergy() {
      return this.finalEnergy;
    }

    double[] coordinates() {
      return this.coordinates.clone();
    }

    boolean converged() {
      return this.converged;
    }

    int iterationCount() {
      return this.iterationCount;
    }

    String summary() {
      return "exit=" + this.exitCode + " " + this.message.replace('\n', ' ').replace('\r', ' ');
    }
  }
}
