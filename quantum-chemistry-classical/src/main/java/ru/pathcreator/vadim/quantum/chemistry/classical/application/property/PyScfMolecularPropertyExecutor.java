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

/** Выполняет opt-in PySCF расчет молекулярных свойств: диполь, Mulliken charges и orbital frontier. */
public final class PyScfMolecularPropertyExecutor {

  private static final String RESULT_PREFIX = "QUANTUM_PYSCF_PROPERTY ";
  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final String VERTICAL_IP_SPIN_OPTION = "vertical_ip_spin";
  private static final String VERTICAL_EA_SPIN_OPTION = "vertical_ea_spin";
  private static final long DEFAULT_TIMEOUT_SECONDS = 300L;
  private static final double HARTREE_TO_EV = 27.211386245988;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF property request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("PySCF property diagnostics target must not be null.");
    }
    if (!(request.subject() instanceof Molecule)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.REQUEST,
              "PySCF molecular property backend requires molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    final String pythonCode = this.pythonCode(request, molecule);
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfMolecularPropertyExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfPropertyProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.successful()) {
        return this.resultSection(request, processResult, commands.get(i));
      }
      failures.add(processResult.summary());
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS.name()),
            "PySCF molecular property backend failed for all configured commands: "
                + String.join(" | ", failures)));
    return null;
  }

  public static boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    return PyScfMolecularPropertyExecutor.optionEquals(request, "property_backend", "pyscf")
        || PyScfMolecularPropertyExecutor.optionEquals(
            request, "molecular_property_backend", "pyscf")
        || PyScfMolecularPropertyExecutor.optionEquals(request, "dipole_backend", "pyscf")
        || PyScfMolecularPropertyExecutor.optionEquals(request, "charge_backend", "pyscf");
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final PyScfPropertyProcessResult processResult,
      final List<String> command) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExternal(
        values,
        "PySCF",
        "pyscf_molecular_property_analysis",
        "molecular_property_analysis",
        "Result is produced by an external PySCF process and depends on installed backend convergence.");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("backend", "PySCF"));
    values.add(ClassicalResultValue.text("backend_command", String.join(" ", command)));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(processResult.converged())));
    values.add(ClassicalResultValue.scalar("total_energy", processResult.totalEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("dipole_x", processResult.dipoleX(), "debye"));
    values.add(ClassicalResultValue.scalar("dipole_y", processResult.dipoleY(), "debye"));
    values.add(ClassicalResultValue.scalar("dipole_z", processResult.dipoleZ(), "debye"));
    values.add(ClassicalResultValue.scalar("dipole_norm", processResult.dipoleNorm(), "debye"));
    values.add(ClassicalResultValue.scalar("homo_energy", processResult.homoEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("lumo_energy", processResult.lumoEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("homo_lumo_gap", processResult.homoLumoGap(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_cation_energy", processResult.verticalCationEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_anion_energy", processResult.verticalAnionEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_ionization_potential",
            processResult.verticalIonizationPotential(),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_ionization_potential_ev",
            processResult.verticalIonizationPotentialEv(),
            "electronvolt"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_electron_affinity", processResult.verticalElectronAffinity(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_electron_affinity_ev",
            processResult.verticalElectronAffinityEv(),
            "electronvolt"));
    values.add(
        ClassicalResultValue.of(
            "mulliken_charge_rows",
            ClassicalResultValueKind.TABLE,
            "atomId,symbol,population,charge",
            processResult.chargeRows()));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS, "PySCF molecular properties", values);
  }

  private PyScfPropertyProcessResult run(
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
        return PyScfPropertyProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfPropertyProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfPropertyProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfPropertyProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(final ClassicalCalculationRequest request, final Molecule molecule) {
    final String method = request.method().name().value().toLowerCase(Locale.ROOT);
    final boolean densityFunctional =
        request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY;
    return ""
        + "import numpy as np\n"
        + "from pyscf import gto, scf, dft\n"
        + "atom = "
        + this.pythonAtomList(molecule)
        + "\n"
        + "atom_ids = "
        + this.pythonAtomIdList(molecule)
        + "\n"
        + "atom_symbols = "
        + this.pythonAtomSymbolList(molecule)
        + "\n"
        + "base_charge = "
        + molecule.charge().value()
        + "\n"
        + "base_spin = "
        + (molecule.spinMultiplicity().value() - 1)
        + "\n"
        + "ip_spin = "
        + this.verticalIonizationSpin(request, molecule)
        + "\n"
        + "ea_spin = "
        + this.verticalElectronAffinitySpin(request, molecule)
        + "\n"
        + "mol = gto.M(atom=atom, basis="
        + this.pythonString(this.pyscfBasisName(request))
        + ", charge=base_charge"
        + ", spin=base_spin"
        + ", unit='Angstrom', verbose=0)\n"
        + "method = "
        + this.pythonString(method)
        + "\n"
        + "density_functional = "
        + PyScfMolecularPropertyExecutor.pythonBoolean(densityFunctional)
        + "\n"
        + "def run_mf(charge, spin):\n"
        + "    m = gto.M(atom=atom, basis="
        + this.pythonString(this.pyscfBasisName(request))
        + ", charge=charge, spin=spin, unit='Angstrom', verbose=0)\n"
        + "    if density_functional:\n"
        + "        f = dft.UKS(m) if m.spin else dft.RKS(m)\n"
        + "        f.xc = method\n"
        + "    else:\n"
        + "        f = scf.UHF(m) if m.spin else scf.RHF(m)\n"
        + "    f.run()\n"
        + "    return m, f\n"
        + "mol, mf = run_mf(base_charge, base_spin)\n"
        + "_, cation_mf = run_mf(base_charge + 1, ip_spin)\n"
        + "_, anion_mf = run_mf(base_charge - 1, ea_spin)\n"
        + "dm = mf.make_rdm1()\n"
        + "dip = mf.dip_moment(mol, dm=dm, unit='Debye', verbose=0)\n"
        + "pop, chg = mf.mulliken_pop(mol, dm, verbose=0)\n"
        + "mo_energy = mf.mo_energy\n"
        + "mo_occ = mf.mo_occ\n"
        + "if isinstance(mo_energy, tuple) or isinstance(mo_energy, list):\n"
        + "    energies = np.concatenate([np.asarray(x, dtype=float) for x in mo_energy])\n"
        + "    occs = np.concatenate([np.asarray(x, dtype=float) for x in mo_occ])\n"
        + "else:\n"
        + "    energies = np.asarray(mo_energy, dtype=float)\n"
        + "    occs = np.asarray(mo_occ, dtype=float)\n"
        + "occupied = energies[occs > 1.0e-9]\n"
        + "virtual = energies[occs <= 1.0e-9]\n"
        + "homo = float(np.max(occupied)) if occupied.size else float('nan')\n"
        + "lumo = float(np.min(virtual)) if virtual.size else float('nan')\n"
        + "gap = lumo - homo if np.isfinite(homo) and np.isfinite(lumo) else float('nan')\n"
        + "norm = float(np.linalg.norm(dip))\n"
        + "cation_energy = float(cation_mf.e_tot)\n"
        + "anion_energy = float(anion_mf.e_tot)\n"
        + "ip = cation_energy - float(mf.e_tot)\n"
        + "ea = float(mf.e_tot) - anion_energy\n"
        + "print('"
        + RESULT_PREFIX
        + "ENERGY ' + str(float(mf.e_tot)) + ' ' + str(bool(getattr(mf, 'converged', True))))\n"
        + "print('"
        + RESULT_PREFIX
        + "DIPOLE ' + str(float(dip[0])) + ' ' + str(float(dip[1])) + ' ' + str(float(dip[2])) + ' ' + str(norm))\n"
        + "print('"
        + RESULT_PREFIX
        + "FRONTIER ' + str(homo) + ' ' + str(lumo) + ' ' + str(gap))\n"
        + "print('"
        + RESULT_PREFIX
        + "VERTICAL_ION ' + str(cation_energy) + ' ' + str(anion_energy) + ' ' + str(ip) + ' ' + str(ip * "
        + HARTREE_TO_EV
        + ") + ' ' + str(ea) + ' ' + str(ea * "
        + HARTREE_TO_EV
        + "))\n"
        + "for i in range(len(atom_ids)):\n"
        + "    population = float(pop[i]) if np.ndim(pop) == 1 else float(np.sum(pop[:, i]))\n"
        + "    charge = float(chg[i])\n"
        + "    print('"
        + RESULT_PREFIX
        + "MULLIKEN ' + atom_ids[i] + ' ' + atom_symbols[i] + ' ' + str(population) + ' ' + str(charge))\n";
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfMolecularPropertyExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfMolecularPropertyExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
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
            "PySCF molecular property backend requires complete 3D coordinates.");
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

  private int verticalIonizationSpin(
      final ClassicalCalculationRequest request, final Molecule molecule) {
    final int defaultSpin =
        molecule.spinMultiplicity().value() == 1
            ? 1
            : Math.max(0, molecule.spinMultiplicity().value() - 2);
    return PyScfMolecularPropertyExecutor.integerOption(
        request, VERTICAL_IP_SPIN_OPTION, defaultSpin);
  }

  private int verticalElectronAffinitySpin(
      final ClassicalCalculationRequest request, final Molecule molecule) {
    final int defaultSpin =
        molecule.spinMultiplicity().value() == 1
            ? 1
            : Math.max(0, molecule.spinMultiplicity().value() - 2);
    return PyScfMolecularPropertyExecutor.integerOption(
        request, VERTICAL_EA_SPIN_OPTION, defaultSpin);
  }

  private String pythonAtomIdList(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    final StringBuilder builder = new StringBuilder(atoms.size() * 16);
    builder.append('[');
    for (int i = 0; i < atoms.size(); ++i) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(this.pythonString(atoms.get(i).id().value()));
    }
    builder.append(']');
    return builder.toString();
  }

  private String pythonAtomSymbolList(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    final StringBuilder builder = new StringBuilder(atoms.size() * 8);
    builder.append('[');
    for (int i = 0; i < atoms.size(); ++i) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(this.pythonString(atoms.get(i).element().symbol().value()));
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

  private static boolean optionEquals(
      final ClassicalCalculationRequest request, final String name, final String expected) {
    final String value = PyScfMolecularPropertyExecutor.textOption(request, name, "");
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
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name)) {
        return Long.parseLong(option.value());
      }
    }
    return defaultValue;
  }

  private static int integerOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name)) {
        return Integer.parseInt(option.value());
      }
    }
    return defaultValue;
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
}
