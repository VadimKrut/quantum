/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend.ClassicalBackend;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;

/**
 * Backend, который не считает числа, а строит валидный план для будущего solver.
 */
public final class PlanningOnlyClassicalBackend implements ClassicalBackend {

  private final ClassicalChemistryService service;
  private final ClassicalBackendProfile profile;

  public PlanningOnlyClassicalBackend() {
    this(
        new ClassicalChemistryService(),
        BuiltInClassicalBackendProfiles.universalPlanning());
  }

  public PlanningOnlyClassicalBackend(
      final ClassicalChemistryService service,
      final ClassicalBackendProfile profile
  ) {
    if (service == null) {
      throw new IllegalArgumentException("Planning-only classical backend service must not be null.");
    }
    if (profile == null) {
      throw new IllegalArgumentException("Planning-only classical backend profile must not be null.");
    }
    this.service = service;
    this.profile = profile;
  }

  public ClassicalBackendProfile profile() {
    return this.profile;
  }

  public ClassicalPreflightResult preflight(final ClassicalCalculationRequest request) {
    return this.service.preflight(request, this.profile);
  }

  public ClassicalCalculationPlan plan(final ClassicalCalculationRequest request) {
    return this.service.plan(request, this.profile);
  }

  public ClassicalCalculationResult execute(final ClassicalCalculationPlan plan) {
    if (plan == null) {
      throw new IllegalArgumentException("Planning-only classical backend plan must not be null.");
    }
    return ClassicalCalculationResult.of(
        plan.request().id(),
        ClassicalCalculationStatus.PLANNED,
        List.of(),
        ClassicalDiagnosticSet.of(
            List.of(
                ClassicalDiagnostic.info(
                    ClassicalDiagnosticCode.PLAN_READY,
                    ClassicalDiagnosticTarget.of(
                        "profile",
                        this.profile.id()),
                    "Planning-only backend produced a validated execution plan."))));
  }
}