/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

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
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Выполняет opt-in single-point расчеты через локально установленный PySCF backend. */
public final class PyScfSinglePointEnergyExecutor {

  private static final String RESULT_PREFIX = "QUANTUM_PYSCF_RESULT ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final String SOLVENT_MODEL_OPTION = "solvent_model";
  private static final String SOLVENT_SHORT_OPTION = "solvent";
  private static final String SOLVENT_DIELECTRIC_OPTION = "solvent_dielectric";
  private static final double DEFAULT_WATER_DIELECTRIC = 78.3553;
  private static final long DEFAULT_TIMEOUT_SECONDS = 300L;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF single-point request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("PySCF diagnostics target must not be null.");
    }
    if (!(request.subject() instanceof Molecule)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.REQUEST,
              "PySCF single-point backend requires molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (this.casscfMethod(request) && !request.hasActiveSpace()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
              "PySCF CASSCF single-point backend requires active space."));
      return null;
    }
    final String pythonCode = this.pythonCode(request, molecule);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfSinglePointEnergyExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.energyAvailable()) {
        return this.resultSection(request, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
            "PySCF backend failed for all configured commands: " + String.join(" | ", failures)));
    return null;
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final PyScfProcessResult processResult,
      final List<String> command) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF",
        "pyscf_single_point",
        "external_reference_single_point_energy",
        "Opt-in local external backend; result depends on installed PySCF version and command environment.");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("backend", "PySCF"));
    values.add(ClassicalResultValue.text("backend_contract", "external_opt_in_reference_backend"));
    values.add(ClassicalResultValue.text("java_native_calculation", Boolean.toString(false)));
    values.add(
        ClassicalResultValue.text("external_package_parity_claimed", Boolean.toString(true)));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(ClassicalResultValue.text("solvent_model", this.solventModel(request)));
    values.add(
        ClassicalResultValue.scalar(
            "solvent_dielectric", this.solventDielectric(request), "relative_permittivity"));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(processResult.converged())));
    values.add(
        ClassicalResultValue.scalar(
            "reference_total_energy", processResult.referenceEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "correlation_energy", processResult.correlationEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", processResult.totalEnergy(), "hartree"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "PySCF single-point energy", values);
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfSinglePointEnergyExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfSinglePointEnergyExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private PyScfProcessResult run(
      final List<String> command, final String pythonCode, final Duration timeout) {
    final ArrayList<String> fullCommand = new ArrayList<String>(command.size() + 2);
    fullCommand.addAll(command);
    fullCommand.add("-c");
    fullCommand.add(pythonCode);
    try {
      final Process process = new ProcessBuilder(fullCommand).redirectErrorStream(true).start();
      final StringBuilder output = new StringBuilder(512);
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
        return PyScfProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(final ClassicalCalculationRequest request, final Molecule molecule) {
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    final boolean densityFunctional =
        request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
    final String solventModel = this.solventModel(request);
    final double solventDielectric = this.solventDielectric(request);
    return ""
        + "from pyscf import gto, scf, dft, mp, cc, ci, fci, solvent\n"
        + "from pyscf import mcscf\n"
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
        + PyScfSinglePointEnergyExecutor.pythonBoolean(densityFunctional)
        + "\n"
        + "if density_functional:\n"
        + "    mf = dft.UKS(mol) if mol.spin else dft.RKS(mol)\n"
        + "    mf.xc = method\n"
        + "    mf.run()\n"
        + "else:\n"
        + "    mf = scf.UHF(mol).run() if mol.spin else scf.RHF(mol).run()\n"
        + "solvent_model = "
        + this.pythonString(solventModel)
        + "\n"
        + "if solvent_model == 'ddcosmo' or solvent_model == 'cosmo':\n"
        + "    mf = solvent.ddCOSMO(mf)\n"
        + "    mf.with_solvent.eps = "
        + solventDielectric
        + "\n"
        + "    mf.run()\n"
        + "ref = float(mf.e_tot)\n"
        + "corr = 0.0\n"
        + "total = ref\n"
        + "conv = bool(getattr(mf, 'converged', True))\n"
        + "if density_functional:\n"
        + "    pass\n"
        + "elif method == 'mp2' or 'moller' in method or 'moeller' in method:\n"
        + "    m = mp.UMP2(mf).run() if mol.spin else mp.MP2(mf).run()\n"
        + "    corr = float(m.e_corr)\n"
        + "    total = float(m.e_tot)\n"
        + "elif method == 'ccsd' or 'coupled cluster singles' in method or 'coupled-cluster singles' in method:\n"
        + "    m = cc.UCCSD(mf).run() if mol.spin else cc.CCSD(mf).run()\n"
        + "    corr = float(m.e_corr)\n"
        + "    total = float(m.e_tot)\n"
        + "    conv = bool(getattr(m, 'converged', True))\n"
        + "elif method == 'cisd' or 'configuration interaction singles doubles' in method or 'configuration-interaction singles-doubles' in method:\n"
        + "    m = ci.UCISD(mf).run() if mol.spin else ci.CISD(mf).run()\n"
        + "    corr = float(m.e_corr)\n"
        + "    total = float(m.e_tot)\n"
        + "    conv = bool(getattr(m, 'converged', True))\n"
        + "elif method == 'fci' or 'full configuration interaction' in method:\n"
        + "    solver = fci.FCI(mol, mf.mo_coeff)\n"
        + "    e, _ = solver.kernel()\n"
        + "    total = float(e)\n"
        + "    corr = total - ref\n"
        + "elif method == 'casscf' or 'mcscf' in method or 'multi configurational' in method or 'multi-configurational' in method:\n"
        + "    ncas = "
        + this.activeSpaceOrbitalCountForScript(request)
        + "\n"
        + "    nelecas = "
        + this.activeSpaceElectronCountForScript(request)
        + "\n"
        + "    m = mcscf.CASSCF(mf, ncas, nelecas).run()\n"
        + "    total = float(m.e_tot)\n"
        + "    corr = total - ref\n"
        + "    conv = bool(getattr(m, 'converged', True))\n"
        + "elif method == 'rhf' or method == 'hf' or 'hartree' in method:\n"
        + "    pass\n"
        + "else:\n"
        + "    raise ValueError('Unsupported PySCF method ' + method)\n"
        + "print('"
        + RESULT_PREFIX
        + "' + str(ref) + ' ' + str(corr) + ' ' + str(total) + ' ' + str(conv))\n";
  }

  private boolean casscfMethod(final ClassicalCalculationRequest request) {
    if (request.method().type()
        != ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return false;
    }
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    return method.equals("casscf")
        || method.equals("mcscf")
        || method.contains("multi configurational")
        || method.contains("multi-configurational");
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
        throw new IllegalArgumentException("PySCF backend requires complete 3D coordinates.");
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

  private int activeSpaceElectronCountForScript(final ClassicalCalculationRequest request) {
    final ActiveSpace activeSpace = this.optionalActiveSpaceForScript(request);
    return activeSpace == null ? 0 : activeSpace.electronCount();
  }

  private int activeSpaceOrbitalCountForScript(final ClassicalCalculationRequest request) {
    final ActiveSpace activeSpace = this.optionalActiveSpaceForScript(request);
    return activeSpace == null ? 0 : activeSpace.orbitalCount();
  }

  private ActiveSpace optionalActiveSpaceForScript(final ClassicalCalculationRequest request) {
    if (!request.hasActiveSpace()) {
      return null;
    }
    return request.activeSpace();
  }

  private String pyscfBasisName(final ClassicalCalculationRequest request) {
    return request.basisSet().name().value().toLowerCase(Locale.ROOT);
  }

  private String solventModel(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfSinglePointEnergyExecutor.textOption(
            request,
            SOLVENT_MODEL_OPTION,
            PyScfSinglePointEnergyExecutor.textOption(request, SOLVENT_SHORT_OPTION, "none"));
    final String normalized = configured.toLowerCase(Locale.ROOT);
    if (normalized.equals("water")) {
      return "ddcosmo";
    }
    if (normalized.equals("none")
        || normalized.equals("gas")
        || normalized.equals("vacuum")
        || normalized.equals("ddcosmo")
        || normalized.equals("cosmo")) {
      return normalized;
    }
    throw new IllegalArgumentException(
        "PySCF single-point solvent_model currently supports none, gas, vacuum, ddcosmo, cosmo and water.");
  }

  private double solventDielectric(final ClassicalCalculationRequest request) {
    final String solventModel = this.solventModel(request);
    if (solventModel.equals("none")
        || solventModel.equals("gas")
        || solventModel.equals("vacuum")) {
      return 1.0;
    }
    return PyScfSinglePointEnergyExecutor.doubleOption(
        request, SOLVENT_DIELECTRIC_OPTION, DEFAULT_WATER_DIELECTRIC);
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
    final String value = PyScfSinglePointEnergyExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Classical calculation option " + name + " must be long.");
    }
  }

  private static double doubleOption(
      final ClassicalCalculationRequest request, final String name, final double defaultValue) {
    final String value = PyScfSinglePointEnergyExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Classical calculation option " + name + " must be double.");
    }
  }

  private static final class PyScfProcessResult {

    private final boolean energyAvailable;
    private final int exitCode;
    private final String message;
    private final double referenceEnergy;
    private final double correlationEnergy;
    private final double totalEnergy;
    private final boolean converged;

    private PyScfProcessResult(
        final boolean energyAvailable,
        final int exitCode,
        final String message,
        final double referenceEnergy,
        final double correlationEnergy,
        final double totalEnergy,
        final boolean converged) {
      this.energyAvailable = energyAvailable;
      this.exitCode = exitCode;
      this.message = message;
      this.referenceEnergy = referenceEnergy;
      this.correlationEnergy = correlationEnergy;
      this.totalEnergy = totalEnergy;
      this.converged = converged;
    }

    static PyScfProcessResult failed(final String message) {
      return new PyScfProcessResult(false, -1, message, Double.NaN, Double.NaN, Double.NaN, false);
    }

    static PyScfProcessResult parse(final int exitCode, final String output) {
      final String[] lines = output.split("\\R");
      for (int i = lines.length - 1; i >= 0; --i) {
        final String line = lines[i];
        if (!line.startsWith(RESULT_PREFIX)) {
          continue;
        }
        final String[] parts = line.substring(RESULT_PREFIX.length()).trim().split("\\s+");
        if (parts.length < 4) {
          break;
        }
        return new PyScfProcessResult(
            true,
            exitCode,
            output,
            Double.parseDouble(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Boolean.parseBoolean(parts[3]));
      }
      return new PyScfProcessResult(
          false, exitCode, output, Double.NaN, Double.NaN, Double.NaN, false);
    }

    boolean energyAvailable() {
      return this.energyAvailable;
    }

    double referenceEnergy() {
      return this.referenceEnergy;
    }

    double correlationEnergy() {
      return this.correlationEnergy;
    }

    double totalEnergy() {
      return this.totalEnergy;
    }

    boolean converged() {
      return this.converged;
    }

    String summary() {
      return "exit=" + this.exitCode + " " + this.message.replace('\n', ' ').replace('\r', ' ');
    }
  }
}
