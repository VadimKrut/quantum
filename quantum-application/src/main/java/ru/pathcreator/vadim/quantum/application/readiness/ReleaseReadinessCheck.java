/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.readiness;

/**
 * Один пункт release-readiness отчета.
 */
public final class ReleaseReadinessCheck {

    private final String name;
    private final ReleaseReadinessCheckStatus status;
    private final String message;

    private ReleaseReadinessCheck(
        final String name,
        final ReleaseReadinessCheckStatus status,
        final String message
    ) {
        this.name = name;
        this.status = status;
        this.message = message;
    }

    public static ReleaseReadinessCheck of(
        final String name,
        final ReleaseReadinessCheckStatus status,
        final String message
    ) {
        if (
            name == null
            || name.isBlank()
        ) {
            throw new IllegalArgumentException("Release readiness check name must not be blank.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Release readiness check status must not be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Release readiness check message must not be null.");
        }
        return new ReleaseReadinessCheck(
            name,
            status,
            message
        );
    }

    public String name() {
        return name;
    }

    public ReleaseReadinessCheckStatus status() {
        return status;
    }

    public boolean isPass() {
        return status == ReleaseReadinessCheckStatus.PASS;
    }

    public boolean isFail() {
        return status == ReleaseReadinessCheckStatus.FAIL;
    }

    public String message() {
        return message;
    }
}