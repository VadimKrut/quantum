/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compatibility;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Единая compatibility matrix для программы и всех проверенных targets.
 */
public final class ProductCompatibilityMatrix {

    private final ValidationResult validation;
    private final ProgramInspectionResult inspection;
    private final ResourceEstimate resources;
    private final SimulationResult simulation;
    private final List<TargetCompatibilityReport> targets;

    private ProductCompatibilityMatrix(
        final ValidationResult validation,
        final ProgramInspectionResult inspection,
        final ResourceEstimate resources,
        final SimulationResult simulation,
        final List<TargetCompatibilityReport> targets
    ) {
        this.validation = validation;
        this.inspection = inspection;
        this.resources = resources;
        this.simulation = simulation;
        this.targets = targets;
    }

    /**
     * Создает immutable compatibility matrix.
     *
     * @param validation результат общей валидации
     * @param inspection общий inspection report
     * @param resources оценка ресурсов
     * @param simulation общий локальный simulation result
     * @param targets отчеты по внешним targets
     * @return compatibility matrix
     */
    public static ProductCompatibilityMatrix of(
        final ValidationResult validation,
        final ProgramInspectionResult inspection,
        final ResourceEstimate resources,
        final SimulationResult simulation,
        final List<TargetCompatibilityReport> targets
    ) {
        if (validation == null) {
            throw new IllegalArgumentException("Compatibility validation must not be null.");
        }
        if (inspection == null) {
            throw new IllegalArgumentException("Compatibility inspection must not be null.");
        }
        if (resources == null) {
            throw new IllegalArgumentException("Compatibility resources must not be null.");
        }
        if (simulation == null) {
            throw new IllegalArgumentException("Compatibility simulation must not be null.");
        }
        if (targets == null) {
            throw new IllegalArgumentException("Compatibility targets must not be null.");
        }
        return new ProductCompatibilityMatrix(
            validation,
            inspection,
            resources,
            simulation,
            List.copyOf(targets)
        );
    }

    public ValidationResult validation() {
        return validation;
    }

    public ProgramInspectionResult inspection() {
        return inspection;
    }

    public ResourceEstimate resources() {
        return resources;
    }

    public SimulationResult simulation() {
        return simulation;
    }

    public List<TargetCompatibilityReport> targets() {
        return targets;
    }

    public boolean isSuccess() {
        if (!validation.isValid() || !simulation.isSuccess()) {
            return false;
        }
        for (int i = 0; i < targets.size(); i++) {
            if (!targets.get(i).isSuccess()) {
                return false;
            }
        }
        return true;
    }
}