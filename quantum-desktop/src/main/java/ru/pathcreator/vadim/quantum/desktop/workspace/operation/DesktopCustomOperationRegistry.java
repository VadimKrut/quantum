/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace.operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;

/**
 * Хранит desktop-макросы custom operation и раскрывает их перед сборкой IR.
 */
public final class DesktopCustomOperationRegistry {

    private static final String CUSTOM_PREFIX = "CUSTOM:";
    private final LinkedHashMap<String, List<DesktopIrOperationSpec>> definitions = new LinkedHashMap<>();

    public String define(
        final String requestedName,
        final List<DesktopIrOperationSpec> body
    ) {
        if (
            requestedName == null
            || requestedName.isBlank()
        ) {
            throw new IllegalArgumentException("Custom desktop operation name must not be blank.");
        }
        if (
            body == null
            || body.isEmpty()
        ) {
            throw new IllegalArgumentException("Custom desktop operation body must not be empty.");
        }
        final String name = uniqueName(requestedName.trim());
        definitions.put(
            name,
            List.copyOf(body)
        );
        return name;
    }

    public DesktopIrOperationSpec reference(final String name) {
        final List<DesktopIrOperationSpec> body = definitions.get(name);
        if (body == null) {
            throw new IllegalArgumentException("Unknown custom desktop operation: " + name + ".");
        }
        final DesktopIrOperationSpec first = body.get(0);
        return new DesktopIrOperationSpec(
            CUSTOM_PREFIX + name,
            first.primaryQubit(),
            first.secondaryQubit(),
            first.tertiaryQubit(),
            first.classicalBit(),
            first.angle()
        );
    }

    public List<DesktopIrOperationSpec> expand(final List<DesktopIrOperationSpec> operations) {
        final ArrayList<DesktopIrOperationSpec> expanded = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            final DesktopIrOperationSpec operation = operations.get(i);
            if (operation.gate().startsWith(CUSTOM_PREFIX)) {
                final String name = operation.gate().substring(CUSTOM_PREFIX.length());
                final List<DesktopIrOperationSpec> body = definitions.get(name);
                if (body == null) {
                    throw new IllegalArgumentException("Unknown custom desktop operation: " + name + ".");
                }
                expanded.addAll(body);
            } else {
                expanded.add(operation);
            }
        }
        return List.copyOf(expanded);
    }

    public Map<String, List<DesktopIrOperationSpec>> definitions() {
        return Map.copyOf(definitions);
    }

    public int size() {
        return definitions.size();
    }

    private String uniqueName(final String requestedName) {
        if (!definitions.containsKey(requestedName)) {
            return requestedName;
        }
        int suffix = 2;
        while (definitions.containsKey(requestedName + "_" + suffix)) {
            suffix++;
        }
        return requestedName + "_" + suffix;
    }
}