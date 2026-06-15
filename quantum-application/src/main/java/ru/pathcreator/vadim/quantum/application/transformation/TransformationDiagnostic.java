/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

import java.util.Objects;

/**
 * Immutable диагностика conservative transformation layer.
 */
public final class TransformationDiagnostic {

    public static final int NO_INDEX = -1;

    private final TransformationDiagnosticSeverity severity;
    private final TransformationDiagnosticCode code;
    private final TransformationStep step;
    private final String message;
    private final int circuitIndex;
    private final int operationIndex;
    private final String targetName;

    private TransformationDiagnostic(
        final TransformationDiagnosticSeverity severity,
        final TransformationDiagnosticCode code,
        final TransformationStep step,
        final String message,
        final int circuitIndex,
        final int operationIndex,
        final String targetName
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("Transformation diagnostic severity must not be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Transformation diagnostic code must not be null.");
        }
        if (step == null) {
            throw new IllegalArgumentException("Transformation diagnostic step must not be null.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Transformation diagnostic message must not be blank.");
        }
        this.severity = severity;
        this.code = code;
        this.step = step;
        this.message = message;
        this.circuitIndex = circuitIndex;
        this.operationIndex = operationIndex;
        this.targetName = targetName;
    }

    public static TransformationDiagnostic info(
        final TransformationDiagnosticCode code,
        final TransformationStep step,
        final String message
    ) {
        return new TransformationDiagnostic(
            TransformationDiagnosticSeverity.INFO,
            code,
            step,
            message,
            NO_INDEX,
            NO_INDEX,
            null
        );
    }

    public static TransformationDiagnostic warning(
        final TransformationDiagnosticCode code,
        final TransformationStep step,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        return new TransformationDiagnostic(
            TransformationDiagnosticSeverity.WARNING,
            code,
            step,
            message,
            circuitIndex,
            operationIndex,
            null
        );
    }

    public static TransformationDiagnostic targetWarning(
        final TransformationDiagnosticCode code,
        final TransformationStep step,
        final String message,
        final String targetName
    ) {
        return new TransformationDiagnostic(
            TransformationDiagnosticSeverity.WARNING,
            code,
            step,
            message,
            NO_INDEX,
            NO_INDEX,
            targetName
        );
    }

    public static TransformationDiagnostic error(
        final TransformationDiagnosticCode code,
        final TransformationStep step,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        return new TransformationDiagnostic(
            TransformationDiagnosticSeverity.ERROR,
            code,
            step,
            message,
            circuitIndex,
            operationIndex,
            null
        );
    }

    public TransformationDiagnosticSeverity severity() {
        return severity;
    }

    public TransformationDiagnosticCode code() {
        return code;
    }

    public TransformationStep step() {
        return step;
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
            throw new IllegalStateException("Transformation diagnostic does not have target name.");
        }
        return targetName;
    }

    public boolean isError() {
        return severity == TransformationDiagnosticSeverity.ERROR;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransformationDiagnostic diagnostic)) {
            return false;
        }
        return circuitIndex == diagnostic.circuitIndex
            && operationIndex == diagnostic.operationIndex
            && severity == diagnostic.severity
            && code == diagnostic.code
            && step == diagnostic.step
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
            step,
            message,
            circuitIndex,
            operationIndex,
            targetName
        );
    }
}