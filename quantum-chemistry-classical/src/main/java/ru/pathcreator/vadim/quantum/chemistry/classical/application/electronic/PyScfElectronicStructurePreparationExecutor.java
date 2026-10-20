/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.IntegralMoleculeFactory;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/** Готовит active-space Hamiltonian через локальный PySCF backend и возвращает доменную модель проекта. */
public final class PyScfElectronicStructurePreparationExecutor {

  private static final String COMMAND_OPTION = "pyscf_command";
  private static final String TIMEOUT_SECONDS_OPTION = "pyscf_timeout_seconds";
  private static final String RESULT_PREFIX = "QPYSCF ";
  private static final long DEFAULT_TIMEOUT_SECONDS = 900L;

  public ElectronicStructurePreparationResult prepare(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("PySCF Hamiltonian preparation request must not be null.");
    }
    if (!(request.subject() instanceof Molecule)) {
      throw new IllegalArgumentException(
          "PySCF Hamiltonian preparation requires molecule subject.");
    }
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final ActiveSpace activeSpace = request.hasActiveSpace() ? request.activeSpace() : null;
    final String pythonCode =
        this.pythonCode(
            request,
            molecule,
            activeSpace,
            this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount()));
    final List<List<String>> commands = this.candidateCommands(request);
    final long timeoutSeconds =
        PyScfElectronicStructurePreparationExecutor.longOption(
            request, TIMEOUT_SECONDS_OPTION, DEFAULT_TIMEOUT_SECONDS);
    final ArrayList<String> failures = new ArrayList<String>(commands.size());
    for (int i = 0; i < commands.size(); ++i) {
      final PyScfHamiltonianProcessResult processResult =
          this.run(commands.get(i), pythonCode, Duration.ofSeconds(timeoutSeconds));
      if (processResult.hamiltonianAvailable()) {
        return ElectronicStructurePreparationResult.ofBackendHamiltonian(
            request.basisSet().name().value(),
            processResult.basisOrbitalCount(),
            processResult.hamiltonian().electronCount(),
            processResult.hamiltonian(),
            "PySCF backend command: " + String.join(" ", commands.get(i)));
      }
      failures.add(processResult.summary());
    }
    throw new IllegalStateException(
        "PySCF Hamiltonian backend failed for all configured commands: "
            + String.join(" | ", failures));
  }

  private int firstActiveOrbital(
      final ClassicalCalculationRequest request,
      final ActiveSpace activeSpace,
      final int fullElectronCount) {
    if (activeSpace == null) {
      return 0;
    }
    final Integer explicitFirstOrbital =
        PyScfElectronicStructurePreparationExecutor.integerOption(
            request.options(), "active_space_first_orbital");
    final int inferredFirstOrbital =
        Math.floorDiv(fullElectronCount - activeSpace.electronCount(), 2);
    final int firstActiveOrbital =
        explicitFirstOrbital == null ? inferredFirstOrbital : explicitFirstOrbital.intValue();
    if (firstActiveOrbital < 0) {
      throw new IllegalArgumentException("Active-space first orbital must not be negative.");
    }
    return firstActiveOrbital;
  }

  private List<List<String>> candidateCommands(final ClassicalCalculationRequest request) {
    final String configured =
        PyScfElectronicStructurePreparationExecutor.textOption(request, COMMAND_OPTION, null);
    if (configured != null) {
      return List.of(PyScfElectronicStructurePreparationExecutor.splitCommand(configured));
    }
    return List.of(List.of("python"), List.of("python3"), List.of("wsl", "python3"));
  }

  private PyScfHamiltonianProcessResult run(
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
        return PyScfHamiltonianProcessResult.failed(String.join(" ", command) + " timed out.");
      }
      return PyScfHamiltonianProcessResult.parse(process.exitValue(), output.toString());
    } catch (final IOException exception) {
      return PyScfHamiltonianProcessResult.failed(
          String.join(" ", command) + " failed to start: " + exception.getMessage());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return PyScfHamiltonianProcessResult.failed(String.join(" ", command) + " was interrupted.");
    }
  }

  private String pythonCode(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ActiveSpace activeSpace,
      final int firstActiveOrbital) {
    final int requestedActiveOrbitalCount = activeSpace == null ? -1 : activeSpace.orbitalCount();
    final int requestedActiveElectronCount = activeSpace == null ? -1 : activeSpace.electronCount();
    return ""
        + "import numpy as np\n"
        + "from pyscf import gto, scf, ao2mo\n"
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
        + "mf = scf.RHF(mol).run() if mol.spin == 0 else scf.ROHF(mol).run()\n"
        + "mo = mf.mo_coeff\n"
        + "full_orbital_count = int(mo.shape[1])\n"
        + "first = "
        + firstActiveOrbital
        + "\n"
        + "requested_nact = "
        + requestedActiveOrbitalCount
        + "\n"
        + "requested_nelec = "
        + requestedActiveElectronCount
        + "\n"
        + "nact = full_orbital_count if requested_nact < 0 else requested_nact\n"
        + "nelec = int(mol.nelectron) if requested_nelec < 0 else requested_nelec\n"
        + "last = first + nact\n"
        + "if first < 0 or nact <= 0 or last > full_orbital_count:\n"
        + "    raise ValueError('Active-space orbital window is outside PySCF molecular orbital space: first=' + str(first) + ' nact=' + str(nact) + ' full=' + str(full_orbital_count))\n"
        + "if nelec <= 0 or nelec > int(mol.nelectron):\n"
        + "    raise ValueError('Active-space electron count is outside PySCF molecule electron count: nelec=' + str(nelec) + ' full=' + str(int(mol.nelectron)))\n"
        + "h1 = mo[:, :last].T @ mf.get_hcore() @ mo[:, :last]\n"
        + "eri = ao2mo.incore.full(mol.intor('int2e'), mo[:, :last], compact=False).reshape(last, last, last, last)\n"
        + "ecore = float(mol.energy_nuc())\n"
        + "for i in range(first):\n"
        + "    ecore += float(2.0 * h1[i, i])\n"
        + "    for j in range(first):\n"
        + "        ecore += float(2.0 * eri[i, i, j, j] - eri[i, j, j, i])\n"
        + "print('"
        + RESULT_PREFIX
        + "META ' + str(nelec) + ' ' + str(nact) + ' ' + str(full_orbital_count) + ' ' + repr(ecore))\n"
        + "for p in range(nact):\n"
        + "    P = first + p\n"
        + "    for q in range(p, nact):\n"
        + "        Q = first + q\n"
        + "        value = float(h1[P, Q])\n"
        + "        for core in range(first):\n"
        + "            value += float(2.0 * eri[P, Q, core, core] - eri[P, core, core, Q])\n"
        + "        print('"
        + RESULT_PREFIX
        + "H ' + str(p) + ' ' + str(q) + ' ' + repr(value))\n"
        + "def pair(a, b):\n"
        + "    lo = min(a, b)\n"
        + "    hi = max(a, b)\n"
        + "    return hi * (hi + 1) // 2 + lo\n"
        + "for p in range(nact):\n"
        + "    P = first + p\n"
        + "    for q in range(p, nact):\n"
        + "        Q = first + q\n"
        + "        left = pair(p, q)\n"
        + "        for r in range(nact):\n"
        + "            R = first + r\n"
        + "            for s in range(r, nact):\n"
        + "                if left > pair(r, s):\n"
        + "                    continue\n"
        + "                S = first + s\n"
        + "                value = float(eri[P, Q, R, S])\n"
        + "                print('"
        + RESULT_PREFIX
        + "G ' + str(p) + ' ' + str(q) + ' ' + str(r) + ' ' + str(s) + ' ' + repr(value))\n";
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
            "PySCF Hamiltonian backend requires complete 3D coordinates.");
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

  private static Integer integerOption(
      final List<ClassicalCalculationOption> options, final String name) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(name)) {
        continue;
      }
      try {
        return Integer.valueOf(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(
            "Classical calculation option " + name + " must be integer.");
      }
    }
    return null;
  }

  private static long longOption(
      final ClassicalCalculationRequest request, final String name, final long defaultValue) {
    final String value =
        PyScfElectronicStructurePreparationExecutor.textOption(request, name, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Classical calculation option " + name + " must be long.");
    }
  }

  private static final class PyScfHamiltonianProcessResult {

    private final boolean hamiltonianAvailable;
    private final int exitCode;
    private final String message;
    private final ElectronicHamiltonian hamiltonian;
    private final int basisOrbitalCount;

    private PyScfHamiltonianProcessResult(
        final boolean hamiltonianAvailable,
        final int exitCode,
        final String message,
        final ElectronicHamiltonian hamiltonian,
        final int basisOrbitalCount) {
      this.hamiltonianAvailable = hamiltonianAvailable;
      this.exitCode = exitCode;
      this.message = message;
      this.hamiltonian = hamiltonian;
      this.basisOrbitalCount = basisOrbitalCount;
    }

    static PyScfHamiltonianProcessResult failed(final String message) {
      return new PyScfHamiltonianProcessResult(false, -1, message, null, -1);
    }

    static PyScfHamiltonianProcessResult parse(final int exitCode, final String output) {
      final String[] lines = output.split("\\R");
      int electronCount = -1;
      int orbitalCount = -1;
      int basisOrbitalCount = -1;
      double nuclearEnergy = Double.NaN;
      final ArrayList<OneElectronIntegral> oneElectronIntegrals =
          new ArrayList<OneElectronIntegral>();
      final ArrayList<TwoElectronIntegral> twoElectronIntegrals =
          new ArrayList<TwoElectronIntegral>();
      for (int i = 0; i < lines.length; ++i) {
        final String line = lines[i];
        if (!line.startsWith(RESULT_PREFIX)) {
          continue;
        }
        final String[] parts = line.substring(RESULT_PREFIX.length()).trim().split("\\s+");
        if (parts.length == 5 && "META".equals(parts[0])) {
          electronCount = Integer.parseInt(parts[1]);
          orbitalCount = Integer.parseInt(parts[2]);
          basisOrbitalCount = Integer.parseInt(parts[3]);
          nuclearEnergy = Double.parseDouble(parts[4]);
        } else if (parts.length == 4 && "H".equals(parts[0])) {
          oneElectronIntegrals.add(
              OneElectronIntegral.of(
                  Integer.parseInt(parts[1]),
                  Integer.parseInt(parts[2]),
                  Double.parseDouble(parts[3])));
        } else if (parts.length == 6 && "G".equals(parts[0])) {
          twoElectronIntegrals.add(
              TwoElectronIntegral.of(
                  Integer.parseInt(parts[1]),
                  Integer.parseInt(parts[2]),
                  Integer.parseInt(parts[3]),
                  Integer.parseInt(parts[4]),
                  Double.parseDouble(parts[5])));
        }
      }
      if (electronCount <= 0
          || orbitalCount <= 0
          || basisOrbitalCount <= 0
          || !Double.isFinite(nuclearEnergy)) {
        return new PyScfHamiltonianProcessResult(false, exitCode, output, null, -1);
      }
      return new PyScfHamiltonianProcessResult(
          true,
          exitCode,
          output,
          ElectronicHamiltonian.of(
              ActiveSpace.of(electronCount, orbitalCount),
              nuclearEnergy,
              List.copyOf(oneElectronIntegrals),
              List.copyOf(twoElectronIntegrals)),
          basisOrbitalCount);
    }

    boolean hamiltonianAvailable() {
      return this.hamiltonianAvailable;
    }

    ElectronicHamiltonian hamiltonian() {
      if (this.hamiltonian == null) {
        throw new IllegalStateException("PySCF Hamiltonian result is not available.");
      }
      return this.hamiltonian;
    }

    int basisOrbitalCount() {
      if (this.basisOrbitalCount <= 0) {
        throw new IllegalStateException("PySCF basis orbital count is not available.");
      }
      return this.basisOrbitalCount;
    }

    String summary() {
      return "exit=" + this.exitCode + " " + this.message.replace('\n', ' ').replace('\r', ' ');
    }
  }
}
