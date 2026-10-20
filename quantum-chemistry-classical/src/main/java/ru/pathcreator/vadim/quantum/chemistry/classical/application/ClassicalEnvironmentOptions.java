/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;

/** Separates standalone environment analyses from options applied to an electronic calculation. */
public final class ClassicalEnvironmentOptions {

  private ClassicalEnvironmentOptions() {
    throw new AssertionError("Environment option validation must not be instantiated.");
  }

  public static List<ClassicalDiagnostic> validate(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (!electronicCalculation(kind)) {
      return List.of();
    }
    final var diagnostics = new ArrayList<ClassicalDiagnostic>();
    final String solvent = option(request, "solvent_model", option(request, "solvent", "none"));
    if (enabled(solvent)) {
      final boolean implemented =
          kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY
              && option(request, "single_point_backend", "").equals("pyscf")
              && List.of("water", "ddcosmo", "cosmo").contains(solvent);
      if (!implemented) {
        diagnostics.add(
            error(
                ClassicalDiagnosticCode.SOLVENT_MODEL_UNSUPPORTED,
                kind,
                "solvent_model",
                "Requested solvent model is not applied by this calculation route. Standalone solvation analysis "
                    + "does not modify electronic energies; supported single-point solvent models require the explicit PySCF route."));
      }
    }
    if (enabled(option(request, "periodic_boundary_conditions", "false"))
        || enabled(option(request, "periodic", "false"))) {
      diagnostics.add(
          error(
              ClassicalDiagnosticCode.PERIODIC_MODEL_UNSUPPORTED,
              kind,
              "periodic_boundary_conditions",
              "Periodic cell analysis is available separately, but this electronic calculation does not use periodic boundary conditions."));
    }
    if (enabled(option(request, "relativistic", "false"))
        || enabled(option(request, "relativistic_model", "none"))) {
      diagnostics.add(
          error(
              ClassicalDiagnosticCode.RELATIVISTIC_MODEL_UNSUPPORTED,
              kind,
              "relativistic_model",
              "Standalone relativistic correction does not alter the Hamiltonian of this electronic calculation."));
    }
    return List.copyOf(diagnostics);
  }

  private static ClassicalDiagnostic error(
      final ClassicalDiagnosticCode code,
      final ClassicalCalculationKind kind,
      final String option,
      final String message) {
    return ClassicalDiagnostic.error(
        code,
        ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
        option + ": " + message);
  }

  private static String option(
      final ClassicalCalculationRequest request, final String name, final String fallback) {
    return request.options().stream()
        .filter(option -> option.name().equals(name))
        .findFirst()
        .map(option -> option.value().strip().toLowerCase(Locale.ROOT))
        .orElse(fallback);
  }

  private static boolean enabled(final String value) {
    return !List.of("", "none", "false", "off", "0", "gas", "vacuum").contains(value);
  }

  private static boolean electronicCalculation(final ClassicalCalculationKind kind) {
    return switch (kind) {
      case SINGLE_POINT_ENERGY,
          GEOMETRY_GRADIENT,
          GEOMETRY_OPTIMIZATION,
          HESSIAN_ANALYSIS,
          FREQUENCY_ANALYSIS,
          THERMODYNAMIC_ANALYSIS,
          IR_RAMAN_PREDICTION,
          SPECTROSCOPY_PREDICTION,
          EXCITED_STATE_ANALYSIS,
          ORBITAL_OPTIMIZATION,
          MP2_CORRELATION,
          CISD_CORRELATION,
          CCSD_CORRELATION,
          FCI_DIAGONALIZATION,
          POST_HF_CORRELATION,
          ELECTRONIC_STRUCTURE_PREPARATION,
          HAMILTONIAN_PREPARATION,
          QUANTUM_PROGRAM_PREPARATION,
          QUANTUM_VQE_OPTIMIZATION,
          REACTION_ENERGY,
          REACTION_PATH,
          IRC_ANALYSIS,
          TRANSITION_STATE_SEARCH,
          BASIS_SET_CONVERGENCE,
          METHOD_BENCHMARK ->
          true;
      default -> false;
    };
  }
}
