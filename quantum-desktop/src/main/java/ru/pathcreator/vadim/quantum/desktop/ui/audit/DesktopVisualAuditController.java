/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiAutomationRunner.scheduleScreenshot;
import static ru.pathcreator.vadim.quantum.desktop.ui.audit.DesktopUiAutomationRunner.scheduleVisualAudit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.stage.Stage;

/**
 * Управляет single screenshot и matrix visual audit без смешивания таймеров с основным UI-классом.
 */
public final class DesktopVisualAuditController {

    private static final String DEFAULT_VISUAL_AUDIT_DIRECTORY = "target/visual-audit/matrix";

    private final DesktopVisualAuditManifestWriter manifestWriter = new DesktopVisualAuditManifestWriter();
    private final DesktopVisualAuditImageVerifier imageVerifier = new DesktopVisualAuditImageVerifier();

    public void scheduleIfRequested(
        final Stage stage,
        final List<String> rawParameters,
        final Map<String, String> namedParameters,
        final DesktopScreenshotStatePreparer statePreparer,
        final DesktopScreenshotCapturer screenshotCapturer
    ) {
        scheduleScreenshotIfRequested(
            stage,
            rawParameters,
            namedParameters,
            statePreparer,
            screenshotCapturer
        );
        scheduleMatrixAuditIfRequested(
            stage,
            rawParameters,
            namedParameters,
            statePreparer,
            screenshotCapturer
        );
    }

    private void scheduleScreenshotIfRequested(
        final Stage stage,
        final List<String> rawParameters,
        final Map<String, String> namedParameters,
        final DesktopScreenshotStatePreparer statePreparer,
        final DesktopScreenshotCapturer screenshotCapturer
    ) {
        if (!rawParameters.contains("--ui-screenshot")) {
            return;
        }
        final DesktopScreenshotRequest request = DesktopScreenshotRequest.from(namedParameters);
        scheduleScreenshot(
            stage,
            () -> statePreparer.prepare(
                request.fixtureName(),
                request.tabName(),
                request.inspectStep(),
                request.operationIndex()
            ),
            () -> {
                final DesktopScreenshotArtifact artifact = screenshotCapturer.capture(request.outputPath());
                imageVerifier.verify(artifact);
            }
        );
    }

    private void scheduleMatrixAuditIfRequested(
        final Stage stage,
        final List<String> rawParameters,
        final Map<String, String> namedParameters,
        final DesktopScreenshotStatePreparer statePreparer,
        final DesktopScreenshotCapturer screenshotCapturer
    ) {
        if (!rawParameters.contains("--ui-visual-audit")) {
            return;
        }
        final String outputDirectory = namedParameters.getOrDefault(
            "visual-audit-dir",
            DEFAULT_VISUAL_AUDIT_DIRECTORY
        );
        final ArrayList<DesktopVisualAuditManifestEntry> entries = new ArrayList<>();
        final HashMap<String, Long> startedNanos = new HashMap<>();
        scheduleVisualAudit(
            stage,
            DesktopVisualAuditScenario.defaults(outputDirectory),
            scenario -> {
                startedNanos.put(
                    scenario.name(),
                    Long.valueOf(System.nanoTime())
                );
                statePreparer.prepare(
                    scenario.fixtureName(),
                    scenario.tabName(),
                    scenario.inspectStep(),
                    scenario.operationIndex()
                );
            },
            scenario -> {
                final DesktopScreenshotArtifact artifact = screenshotCapturer.capture(scenario.outputPath());
                imageVerifier.verify(artifact);
                entries.add(DesktopVisualAuditManifestEntry.from(
                    scenario,
                    artifact,
                    durationMillis(
                        startedNanos.remove(scenario.name())
                    )
                ));
            },
            () -> manifestWriter.write(
                outputDirectory,
                DesktopVisualAuditManifest.from(entries)
            )
        );
    }

    private static long durationMillis(final Long startedNano) {
        if (startedNano == null) {
            return -1L;
        }
        return Math.max(
            0L,
            (System.nanoTime() - startedNano.longValue()) / 1_000_000L
        );
    }
    @FunctionalInterface
    public interface DesktopScreenshotStatePreparer {

        void prepare(
            String fixtureName,
            String tabName,
            Integer inspectStep,
            Integer operationIndex
        );
    }

    @FunctionalInterface
    public interface DesktopScreenshotCapturer {

        DesktopScreenshotArtifact capture(String outputPath) throws Exception;
    }
}