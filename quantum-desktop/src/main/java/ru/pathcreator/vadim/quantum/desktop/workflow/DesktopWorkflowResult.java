/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workflow;

/**
 * Текстовый результат desktop workflow без JavaFX-зависимостей.
 */
public final class DesktopWorkflowResult {

    private final DesktopAction action;
    private final boolean success;
    private final String status;
    private final String summary;
    private final String content;
    private final String generatedContent;

    private DesktopWorkflowResult(
        final DesktopAction action,
        final boolean success,
        final String status,
        final String summary,
        final String content,
        final String generatedContent
    ) {
        if (action == null) {
            throw new IllegalArgumentException("Desktop action must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Desktop workflow status must not be null.");
        }
        if (summary == null) {
            throw new IllegalArgumentException("Desktop workflow summary must not be null.");
        }
        if (content == null) {
            throw new IllegalArgumentException("Desktop workflow content must not be null.");
        }
        if (generatedContent == null) {
            throw new IllegalArgumentException("Desktop workflow generated content must not be null.");
        }
        this.action = action;
        this.success = success;
        this.status = status;
        this.summary = summary;
        this.content = content;
        this.generatedContent = generatedContent;
    }

    public static DesktopWorkflowResult of(
        final DesktopAction action,
        final boolean success,
        final String status,
        final String summary,
        final String content
    ) {
        return of(
            action,
            success,
            status,
            summary,
            content,
            ""
        );
    }

    public static DesktopWorkflowResult of(
        final DesktopAction action,
        final boolean success,
        final String status,
        final String summary,
        final String content,
        final String generatedContent
    ) {
        return new DesktopWorkflowResult(
            action,
            success,
            status,
            summary,
            content,
            generatedContent
        );
    }

    public DesktopAction action() {
        return action;
    }

    public boolean isSuccess() {
        return success;
    }

    public String status() {
        return status;
    }

    public String summary() {
        return summary;
    }

    public String content() {
        return content;
    }

    public String generatedContent() {
        return generatedContent;
    }
}