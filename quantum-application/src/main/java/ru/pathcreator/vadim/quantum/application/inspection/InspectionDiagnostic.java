/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.inspection;

import java.util.Objects;

/**
 * Одно структурированное сообщение inspection.
 */
public final class InspectionDiagnostic {

    public static final int NO_INDEX = -1;

    private final InspectionDiagnosticSeverity severity;
    private final InspectionDiagnosticCode code;
    private final String message;
    private final int circuitIndex;
    private final int operationIndex;
    private final String targetName;

    public InspectionDiagnostic(
        final InspectionDiagnosticSeverity severity,
        final InspectionDiagnosticCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex,
        final String targetName
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("Inspection diagnostic severity must not be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Inspection diagnostic code must not be null.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Inspection diagnostic message must not be blank.");
        }
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.circuitIndex = circuitIndex;
        this.operationIndex = operationIndex;
        this.targetName = targetName;
    }

    public static InspectionDiagnostic warning(
        final InspectionDiagnosticCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        return new InspectionDiagnostic(
            InspectionDiagnosticSeverity.WARNING,
            code,
            message,
            circuitIndex,
            operationIndex,
            null
        );
    }

    public static InspectionDiagnostic targetWarning(
        final String message,
        final String targetName
    ) {
        return new InspectionDiagnostic(
            InspectionDiagnosticSeverity.WARNING,
            InspectionDiagnosticCode.TARGET_COMPATIBILITY_WARNING,
            message,
            NO_INDEX,
            NO_INDEX,
            targetName
        );
    }

    public InspectionDiagnosticSeverity severity() {
        return severity;
    }

    public InspectionDiagnosticCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int circuitIndex() {
        return circuitIndex;
    }

    public int operationIndex() {
        return operationIndex;
    }

    public boolean hasTargetName() {
        return targetName != null;
    }

    public String targetName() {
        if (targetName == null) {
            throw new IllegalStateException("Inspection diagnostic does not contain target profile context.");
        }
        return targetName;
    }

    public boolean isError() {
        return severity == InspectionDiagnosticSeverity.ERROR;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InspectionDiagnostic diagnostic)) {
            return false;
        }
        return circuitIndex == diagnostic.circuitIndex
            && operationIndex == diagnostic.operationIndex
            && severity == diagnostic.severity
            && code == diagnostic.code
            && Objects.equals(
                message,
                diagnostic.message
            )
            && Objects.equals(
                targetName,
                diagnostic.targetName
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            severity,
            code,
            message,
            circuitIndex,
            operationIndex,
            targetName
        );
    }
}