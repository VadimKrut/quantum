/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimeline;
import ru.pathcreator.vadim.quantum.application.visualization.CircuitTimelineStep;
import ru.pathcreator.vadim.quantum.application.visualization.ProgramTimeline;

/**
 * Рендерит timeline summaries для desktop circuit tab.
 */
public final class DesktopTimelineRenderer {

    public String renderSummary(final ProgramTimeline timeline) {
        final StringBuilder summary = new StringBuilder();
        for (int i = 0; i < timeline.circuits().size(); i++) {
            final CircuitTimeline circuit = timeline.circuits().get(i);
            summary.append("Circuit ")
                .append(circuit.circuitName())
                .append(System.lineSeparator());
            for (int j = 0; j < circuit.steps().size(); j++) {
                final CircuitTimelineStep step = circuit.steps().get(j);
                summary.append("  #")
                    .append(step.operationIndex())
                    .append(" ")
                    .append(step.operationKind())
                    .append(" ")
                    .append(step.label())
                    .append(System.lineSeparator());
            }
        }
        return summary.toString();
    }
}