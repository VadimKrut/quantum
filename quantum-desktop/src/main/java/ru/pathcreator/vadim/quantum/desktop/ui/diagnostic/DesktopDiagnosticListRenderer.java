/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.diagnostic;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Формирует строки диагностической панели без зависимости от JavaFX controls.
 */
public final class DesktopDiagnosticListRenderer {

    public List<String> render(
        final ValidationResult validation,
        final IntegrationFormat targetFormat,
        final String preflightStatus,
        final int preflightDiagnosticCount,
        final boolean activeJsonProgram,
        final boolean operationListDeferred,
        final List<DesktopIrOperationSpec> operations
    ) {
        final ArrayList<String> items = new ArrayList<>();
        items.add(validation.isValid()
            ? "VALIDATION OK"
            : "VALIDATION ERRORS " + validation.errorCount());
        for (int i = 0; i < validation.errors().size(); i++) {
            items.add("VALIDATION #" + i + " " + validation.error(i).message());
        }
        items.add("PREFLIGHT " + targetFormat + " " + preflightStatus
            + " diagnostics=" + preflightDiagnosticCount);
        if (activeJsonProgram) {
            items.add("ACTIVE JSON Source: native JSON text");
        } else if (operationListDeferred) {
            items.add("OP LIST DEFERRED " + operations.size() + " operation(s)");
        } else {
            for (int i = 0; i < operations.size(); i++) {
                items.add("OP #" + i + " " + operations.get(i).label());
            }
        }
        return items;
    }

    public int operationIndexFromItem(final String item) {
        if (
            item == null
            || !item.startsWith("OP #")
        ) {
            return -1;
        }
        final int start = 4;
        final int end = item.indexOf(
            ' ',
            start
        );
        if (end <= start) {
            return -1;
        }
        try {
            return Integer.parseInt(item.substring(
                start,
                end
            ));
        } catch (final NumberFormatException exception) {
            return -1;
        }
    }
}