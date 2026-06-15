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

public final class CircuitTimeline {

    private final String circuitName;
    private final List<String> quantumWires;
    private final List<String> classicalWires;
    private final List<CircuitTimelineStep> steps;

    public CircuitTimeline(
        final String circuitName,
        final List<String> quantumWires,
        final List<String> classicalWires,
        final List<CircuitTimelineStep> steps
    ) {
        if (
            circuitName == null
            || circuitName.isBlank()
        ) {
            throw new IllegalArgumentException("Circuit timeline name must not be blank.");
        }
        if (quantumWires == null) {
            throw new IllegalArgumentException("Circuit timeline quantum wires must not be null.");
        }
        if (classicalWires == null) {
            throw new IllegalArgumentException("Circuit timeline classical wires must not be null.");
        }
        if (steps == null) {
            throw new IllegalArgumentException("Circuit timeline steps must not be null.");
        }
        this.circuitName = circuitName;
        this.quantumWires = List.copyOf(quantumWires);
        this.classicalWires = List.copyOf(classicalWires);
        this.steps = List.copyOf(steps);
    }

    public String circuitName() {
        return circuitName;
    }

    public List<String> quantumWires() {
        return quantumWires;
    }

    public List<String> classicalWires() {
        return classicalWires;
    }

    public List<CircuitTimelineStep> steps() {
        return steps;
    }
}