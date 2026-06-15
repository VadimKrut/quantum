/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.ArrayList;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

public final class BackendPreflightChecker {

    public BackendPreflightResult check(
        final QuantumProgram program,
        final BackendDescriptor descriptor
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Backend preflight program must not be null.");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("Backend descriptor must not be null.");
        }
        final ArrayList<BackendDiagnostic> diagnostics = new ArrayList<>();
        final ValidationResult validation = new QuantumProgramValidator().validate(program);
        if (!validation.isValid()) {
            diagnostics.add(BackendDiagnostic.error(
                BackendDiagnosticCode.VALIDATION_FAILED,
                "Program failed domain validation before backend submission."
            ));
        }
        final CapabilityPreflightResult capability = new CapabilityPreflightChecker().check(
            program,
            descriptor.targetProfile()
        );
        if (!capability.isSuccess()) {
            diagnostics.add(BackendDiagnostic.error(
                BackendDiagnosticCode.PREFLIGHT_FAILED,
                "Program cannot be represented by backend target profile."
            ));
        }
        return new BackendPreflightResult(
            validation,
            capability,
            diagnostics
        );
    }
}