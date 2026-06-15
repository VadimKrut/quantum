/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.Objects;
import java.util.UUID;

public final class BackendJobId {

    private final String value;

    private BackendJobId(final String value) {
        if (
            value == null
            || value.isBlank()
        ) {
            throw new IllegalArgumentException("Backend job id must not be blank.");
        }
        this.value = value;
    }

    public static BackendJobId random() {
        return of(UUID.randomUUID().toString());
    }

    public static BackendJobId of(final String value) {
        return new BackendJobId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BackendJobId jobId)) {
            return false;
        }
        return Objects.equals(
            value,
            jobId.value
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}