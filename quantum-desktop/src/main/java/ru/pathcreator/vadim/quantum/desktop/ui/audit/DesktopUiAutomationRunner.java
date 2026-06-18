/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Управляет автоматическими desktop-прогонами без смешивания таймеров с UI-кодом приложения.
 */
public final class DesktopUiAutomationRunner {

    private static final double SMOKE_START_DELAY_MILLIS = 700.0;
    private static final double SMOKE_VERIFY_DELAY_SECONDS = 3.2;
    private static final double SCREENSHOT_START_DELAY_SECONDS = 1.2;
    private static final double SCREENSHOT_CAPTURE_DELAY_SECONDS = 1.0;
    private static final double VISUAL_AUDIT_START_DELAY_SECONDS = 1.2;
    private static final double VISUAL_AUDIT_CAPTURE_DELAY_SECONDS = 0.9;
    private static final int FAILURE_EXIT_CODE = 2;

    private DesktopUiAutomationRunner() {
    }

    public static void scheduleSmoke(
        final Runnable beforeAsyncActions,
        final Runnable afterAsyncActions
    ) {
        final PauseTransition startDelay = new PauseTransition(Duration.millis(SMOKE_START_DELAY_MILLIS));
        startDelay.setOnFinished(event -> runOrExit(() -> {
            beforeAsyncActions.run();
            final PauseTransition verifyDelay = new PauseTransition(Duration.seconds(SMOKE_VERIFY_DELAY_SECONDS));
            verifyDelay.setOnFinished(verifyEvent -> runOrExit(afterAsyncActions::run));
            verifyDelay.play();
        }));
        startDelay.play();
    }

    public static void scheduleScreenshot(
        final Stage stage,
        final Runnable prepare,
        final CheckedRunnable capture
    ) {
        final PauseTransition startDelay = new PauseTransition(Duration.seconds(SCREENSHOT_START_DELAY_SECONDS));
        startDelay.setOnFinished(event -> runOrExit(() -> {
            prepare.run();
            final PauseTransition captureDelay = new PauseTransition(Duration.seconds(SCREENSHOT_CAPTURE_DELAY_SECONDS));
            captureDelay.setOnFinished(captureEvent -> runOrExit(() -> {
                capture.run();
                stage.close();
                Platform.exit();
            }));
            captureDelay.play();
        }));
        startDelay.play();
    }

    public static <T> void scheduleVisualAudit(
        final Stage stage,
        final List<T> scenarios,
        final CheckedConsumer<T> prepare,
        final CheckedConsumer<T> capture,
        final CheckedRunnable complete
    ) {
        final PauseTransition startDelay = new PauseTransition(Duration.seconds(VISUAL_AUDIT_START_DELAY_SECONDS));
        startDelay.setOnFinished(event -> runVisualAuditStep(
            stage,
            scenarios,
            prepare,
            capture,
            complete,
            0
        ));
        startDelay.play();
    }

    private static <T> void runVisualAuditStep(
        final Stage stage,
        final List<T> scenarios,
        final CheckedConsumer<T> prepare,
        final CheckedConsumer<T> capture,
        final CheckedRunnable complete,
        final int index
    ) {
        if (index >= scenarios.size()) {
            runOrExit(() -> {
                complete.run();
                stage.close();
                Platform.exit();
            });
            return;
        }
        final T scenario = scenarios.get(index);
        runOrExit(() -> {
            prepare.accept(scenario);
            final PauseTransition captureDelay = new PauseTransition(Duration.seconds(VISUAL_AUDIT_CAPTURE_DELAY_SECONDS));
            captureDelay.setOnFinished(captureEvent -> runOrExit(() -> {
                capture.accept(scenario);
                runVisualAuditStep(
                    stage,
                    scenarios,
                    prepare,
                    capture,
                    complete,
                    index + 1
                );
            }));
            captureDelay.play();
        });
    }

    private static void runOrExit(final CheckedRunnable action) {
        try {
            action.run();
        } catch (final Exception exception) {
            exception.printStackTrace(System.err);
            Platform.exit();
            System.exit(FAILURE_EXIT_CODE);
        }
    }

    @FunctionalInterface
    public interface CheckedRunnable {

        void run() throws Exception;
    }

    @FunctionalInterface
    public interface CheckedConsumer<T> {

        void accept(T value) throws Exception;
    }
}