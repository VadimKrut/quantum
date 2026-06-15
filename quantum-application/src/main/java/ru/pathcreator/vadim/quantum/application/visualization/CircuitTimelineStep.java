/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.visualization;

import java.util.List;

import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

public final class CircuitTimelineStep {

    private final int operationIndex;
    private final OperationKind operationKind;
    private final String label;
    private final List<String> quantumWires;
    private final List<String> classicalWires;
    private final boolean staticPlacement;

    public CircuitTimelineStep(
        final int operationIndex,
        final OperationKind operationKind,
        final String label,
        final List<String> quantumWires,
        final List<String> classicalWires,
        final boolean staticPlacement
    ) {
        if (operationKind == null) {
            throw new IllegalArgumentException("Circuit timeline operation kind must not be null.");
        }
        if (
            label == null
            || label.isBlank()
        ) {
            throw new IllegalArgumentException("Circuit timeline label must not be blank.");
        }
        if (quantumWires == null) {
            throw new IllegalArgumentException("Circuit timeline quantum wires must not be null.");
        }
        if (classicalWires == null) {
            throw new IllegalArgumentException("Circuit timeline classical wires must not be null.");
        }
        this.operationIndex = operationIndex;
        this.operationKind = operationKind;
        this.label = label;
        this.quantumWires = List.copyOf(quantumWires);
        this.classicalWires = List.copyOf(classicalWires);
        this.staticPlacement = staticPlacement;
    }

    public int operationIndex() {
        return operationIndex;
    }

    public OperationKind operationKind() {
        return operationKind;
    }

    public String label() {
        return label;
    }

    public List<String> quantumWires() {
        return quantumWires;
    }

    public List<String> classicalWires() {
        return classicalWires;
    }

    public boolean isStaticPlacement() {
        return staticPlacement;
    }
}