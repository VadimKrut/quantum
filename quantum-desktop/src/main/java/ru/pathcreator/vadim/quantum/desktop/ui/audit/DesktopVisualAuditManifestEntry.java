/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

/**
 * Одна строка manifest: сценарий, fixture, вкладка, image metadata и длительность проверки.
 */
public record DesktopVisualAuditManifestEntry(
    String name,
    String fixtureName,
    String tabName,
    Integer inspectStep,
    Integer operationIndex,
    String outputPath,
    int width,
    int height,
    long bytes,
    long durationMillis
) {

    public static DesktopVisualAuditManifestEntry from(
        final DesktopVisualAuditScenario scenario,
        final DesktopScreenshotArtifact artifact
    ) {
        return from(
            scenario,
            artifact,
            -1L
        );
    }

    public static DesktopVisualAuditManifestEntry from(
        final DesktopVisualAuditScenario scenario,
        final DesktopScreenshotArtifact artifact,
        final long durationMillis
    ) {
        return new DesktopVisualAuditManifestEntry(
            scenario.name(),
            scenario.fixtureName(),
            scenario.tabName(),
            scenario.inspectStep(),
            scenario.operationIndex(),
            artifact.path().toString(),
            artifact.width(),
            artifact.height(),
            artifact.bytes(),
            durationMillis
        );
    }
}