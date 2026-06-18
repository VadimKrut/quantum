/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopVisualAuditManifestWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    void writesManifestWithScenarioAndImageMetadata() throws Exception {
        final DesktopVisualAuditScenario scenario = new DesktopVisualAuditScenario(
            "dense",
            "dense-spectrum",
            "Visual Circuit",
            12,
            12,
            tempDir.resolve("dense.png").toString()
        );
        final DesktopScreenshotArtifact artifact = new DesktopScreenshotArtifact(
            tempDir.resolve("dense.png"),
            1440,
            900,
            123456L
        );
        final DesktopVisualAuditManifest manifest = DesktopVisualAuditManifest.from(List.of(
            DesktopVisualAuditManifestEntry.from(
                scenario,
                artifact,
                345L
            )
        ));

        new DesktopVisualAuditManifestWriter().write(
            tempDir.toString(),
            manifest
        );

        final String json = Files.readString(tempDir.resolve("manifest.json"));
        assertTrue(json.contains("\"name\" : \"dense\""));
        assertTrue(json.contains("\"fixtureName\" : \"dense-spectrum\""));
        assertTrue(json.contains("\"tabName\" : \"Visual Circuit\""));
        assertTrue(json.contains("\"width\" : 1440"));
        assertTrue(json.contains("\"height\" : 900"));
        assertTrue(json.contains("\"bytes\" : 123456"));
        assertTrue(json.contains("\"durationMillis\" : 345"));
    }
}