/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModelKind;

/**
 * Строит backend-neutral error-mitigation plan для quantum chemistry запуска.
 */
public final class ErrorMitigationPlanner {

  private static final double READOUT_THRESHOLD = 0.005;
  private static final double GATE_NOISE_THRESHOLD = 0.001;
  private static final int SHOT_SCALE_FOR_READOUT = 2;
  private static final int SHOT_SCALE_FOR_ZERO_NOISE = 3;
  private static final int SHOT_SCALE_FOR_UNKNOWN_HARDWARE = 2;

  public ErrorMitigationPlan plan(final ErrorMitigationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Error mitigation request must not be null.");
    }
    final ArrayList<ErrorMitigationTechnique> techniques =
        new ArrayList<ErrorMitigationTechnique>();
    final ArrayList<ErrorMitigationDiagnostic> diagnostics =
        new ArrayList<ErrorMitigationDiagnostic>();
    final QuantumNoiseModel noiseModel = request.noiseModel();
    int recommendedShots = request.baseShots();
    if (noiseModel.isIdeal()) {
      diagnostics.add(
          ErrorMitigationDiagnostic.info(
              ErrorMitigationDiagnosticCode.IDEAL_NOISE_MODEL,
              "noiseModel",
              "Noise model is ideal; mitigation is not required."));
      return ErrorMitigationPlan.of(
          ErrorMitigationStatus.NOT_REQUIRED, techniques, recommendedShots, diagnostics);
    }
    recommendedShots =
        this.planReadoutMitigation(request, techniques, diagnostics, recommendedShots);
    recommendedShots =
        this.planGateNoiseMitigation(request, techniques, diagnostics, recommendedShots);
    recommendedShots =
        this.planChemistrySymmetryMitigation(request, techniques, diagnostics, recommendedShots);
    recommendedShots =
        this.planUnknownHardwareMitigation(request, techniques, diagnostics, recommendedShots);
    diagnostics.add(
        ErrorMitigationDiagnostic.info(
            ErrorMitigationDiagnosticCode.PLAN_READY,
            "plan",
            "Error mitigation plan was built for the requested noise model."));
    return ErrorMitigationPlan.of(
        this.status(techniques, diagnostics), techniques, recommendedShots, diagnostics);
  }

  private int planReadoutMitigation(
      final ErrorMitigationRequest request,
      final List<ErrorMitigationTechnique> techniques,
      final List<ErrorMitigationDiagnostic> diagnostics,
      final int currentShots) {
    if (request.noiseModel().readoutErrorRate() <= ErrorMitigationPlanner.READOUT_THRESHOLD
        && request.noiseModel().kind() != QuantumNoiseModelKind.READOUT_BIASED) {
      return currentShots;
    }
    final boolean added =
        this.addTechnique(
            request,
            techniques,
            diagnostics,
            ErrorMitigationTechnique.READOUT_CALIBRATION,
            ErrorMitigationDiagnosticCode.READOUT_CALIBRATION_REQUIRED,
            "readout",
            "Readout error rate requires readout calibration mitigation.");
    if (!added) {
      return currentShots;
    }
    return this.multiplyShots(currentShots, ErrorMitigationPlanner.SHOT_SCALE_FOR_READOUT);
  }

  private int planGateNoiseMitigation(
      final ErrorMitigationRequest request,
      final List<ErrorMitigationTechnique> techniques,
      final List<ErrorMitigationDiagnostic> diagnostics,
      final int currentShots) {
    if (request.noiseModel().singleQubitErrorRate() <= ErrorMitigationPlanner.GATE_NOISE_THRESHOLD
        && request.noiseModel().twoQubitErrorRate() <= ErrorMitigationPlanner.GATE_NOISE_THRESHOLD
        && request.noiseModel().kind() != QuantumNoiseModelKind.COHERENT_OVER_ROTATION) {
      return currentShots;
    }
    int recommendedShots = currentShots;
    if (this.addTechnique(
        request,
        techniques,
        diagnostics,
        ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION,
        ErrorMitigationDiagnosticCode.ZERO_NOISE_EXTRAPOLATION_RECOMMENDED,
        "gateNoise",
        "Gate error rate recommends zero-noise extrapolation.")) {
      recommendedShots =
          this.multiplyShots(recommendedShots, ErrorMitigationPlanner.SHOT_SCALE_FOR_ZERO_NOISE);
    }
    this.addTechnique(
        request,
        techniques,
        diagnostics,
        ErrorMitigationTechnique.PAULI_TWIRLING,
        ErrorMitigationDiagnosticCode.ZERO_NOISE_EXTRAPOLATION_RECOMMENDED,
        "gateNoise",
        "Coherent or depolarizing noise recommends Pauli twirling where backend supports it.");
    return recommendedShots;
  }

  private int planChemistrySymmetryMitigation(
      final ErrorMitigationRequest request,
      final List<ErrorMitigationTechnique> techniques,
      final List<ErrorMitigationDiagnostic> diagnostics,
      final int currentShots) {
    int recommendedShots = currentShots;
    if (request.requireParticleNumberConservation()) {
      this.addTechnique(
          request,
          techniques,
          diagnostics,
          ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION,
          ErrorMitigationDiagnosticCode.SYMMETRY_CHECK_RECOMMENDED,
          "particleNumber",
          "Chemistry active space requires particle-number post-selection.");
    }
    if (request.requireSpinParityConservation()) {
      this.addTechnique(
          request,
          techniques,
          diagnostics,
          ErrorMitigationTechnique.SPIN_PARITY_POST_SELECTION,
          ErrorMitigationDiagnosticCode.SYMMETRY_CHECK_RECOMMENDED,
          "spinParity",
          "Chemistry active space requires spin-parity post-selection.");
    }
    if (request.requireParticleNumberConservation() || request.requireSpinParityConservation()) {
      if (this.addTechnique(
          request,
          techniques,
          diagnostics,
          ErrorMitigationTechnique.SYMMETRY_VERIFICATION,
          ErrorMitigationDiagnosticCode.SYMMETRY_CHECK_RECOMMENDED,
          "symmetry",
          "Symmetry verification is recommended for chemistry observables.")) {
        recommendedShots = this.multiplyShots(recommendedShots, 2);
      }
    }
    return recommendedShots;
  }

  private int planUnknownHardwareMitigation(
      final ErrorMitigationRequest request,
      final List<ErrorMitigationTechnique> techniques,
      final List<ErrorMitigationDiagnostic> diagnostics,
      final int currentShots) {
    if (request.noiseModel().kind() != QuantumNoiseModelKind.HARDWARE_UNKNOWN) {
      return currentShots;
    }
    diagnostics.add(
        ErrorMitigationDiagnostic.warning(
            ErrorMitigationDiagnosticCode.HARDWARE_NOISE_UNKNOWN,
            "noiseModel",
            "Hardware noise is unknown; plan is conservative and should be refined with calibration data."));
    if (!techniques.contains(ErrorMitigationTechnique.SHOT_REBALANCING)
        && request.allowedTechniques().contains(ErrorMitigationTechnique.SHOT_REBALANCING)) {
      techniques.add(ErrorMitigationTechnique.SHOT_REBALANCING);
      diagnostics.add(
          ErrorMitigationDiagnostic.info(
              ErrorMitigationDiagnosticCode.SHOT_COUNT_INCREASED,
              "shots",
              "Unknown hardware noise recommends additional shot budget."));
      return this.multiplyShots(
          currentShots, ErrorMitigationPlanner.SHOT_SCALE_FOR_UNKNOWN_HARDWARE);
    }
    return currentShots;
  }

  private boolean addTechnique(
      final ErrorMitigationRequest request,
      final List<ErrorMitigationTechnique> techniques,
      final List<ErrorMitigationDiagnostic> diagnostics,
      final ErrorMitigationTechnique technique,
      final ErrorMitigationDiagnosticCode diagnosticCode,
      final String target,
      final String message) {
    if (!request.allowedTechniques().contains(technique)) {
      diagnostics.add(
          ErrorMitigationDiagnostic.warning(
              ErrorMitigationDiagnosticCode.TECHNIQUE_UNSUPPORTED,
              target,
              "Requested mitigation technique is not allowed by profile: "
                  + technique.name()
                  + "."));
      return false;
    }
    if (!techniques.contains(technique)) {
      techniques.add(technique);
      diagnostics.add(ErrorMitigationDiagnostic.info(diagnosticCode, target, message));
    }
    return true;
  }

  private ErrorMitigationStatus status(
      final List<ErrorMitigationTechnique> techniques,
      final List<ErrorMitigationDiagnostic> diagnostics) {
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i).error()) {
        return ErrorMitigationStatus.UNSUPPORTED;
      }
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i).code() == ErrorMitigationDiagnosticCode.TECHNIQUE_UNSUPPORTED) {
        return ErrorMitigationStatus.PARTIAL;
      }
    }
    if (techniques.isEmpty()) {
      return ErrorMitigationStatus.NOT_REQUIRED;
    }
    return ErrorMitigationStatus.PLANNED;
  }

  private int multiplyShots(final int shots, final int scale) {
    final long scaled = (long) shots * (long) scale;
    if (scaled > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) scaled;
  }
}
