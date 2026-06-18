/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopAction;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowResult;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrWorkspaceService;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Формирует быстрый compatibility preview для screenshot-аудита без запуска тяжелого workflow.
 */
public final class DesktopCompatibilityScreenshotPreview {

    private final DesktopIrWorkspaceService workspaceService;

    public DesktopCompatibilityScreenshotPreview(final DesktopIrWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public DesktopWorkflowResult preview(
        final QuantumProgram program,
        final int operationCount
    ) {
        final StringBuilder content = new StringBuilder();
        int diagnostics = 0;
        boolean exportable = true;
        content.append("Compatibility screenshot preflight")
            .append(System.lineSeparator())
            .append("program operations: ")
            .append(operationCount)
            .append(System.lineSeparator());
        for (final IntegrationFormat format : IntegrationFormat.values()) {
            final CapabilityPreflightResult preflight = workspaceService.preflight(
                program,
                format
            );
            diagnostics += preflight.diagnostics().size();
            exportable = exportable && preflight.isSuccess();
            content.append(format.name())
                .append(" -> ")
                .append(preflight.status().name())
                .append(" diagnostics=")
                .append(preflight.diagnostics().size())
                .append(System.lineSeparator());
        }
        return DesktopWorkflowResult.of(
            DesktopAction.COMPATIBILITY,
            exportable,
            exportable ? "COMPATIBLE" : "INCOMPATIBLE",
            "Preflight diagnostics: " + diagnostics,
            content.toString()
        );
    }
}