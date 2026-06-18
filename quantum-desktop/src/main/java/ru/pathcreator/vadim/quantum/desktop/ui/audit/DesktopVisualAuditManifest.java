/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Manifest одного полного matrix visual audit desktop-приложения.
 */
public record DesktopVisualAuditManifest(
    String createdAt,
    List<DesktopVisualAuditManifestEntry> entries
) {

    public DesktopVisualAuditManifest {
        entries = List.copyOf(entries);
    }

    public static DesktopVisualAuditManifest from(final List<DesktopVisualAuditManifestEntry> entries) {
        return new DesktopVisualAuditManifest(
            Instant.now().toString(),
            new ArrayList<>(entries)
        );
    }
}