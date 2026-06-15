/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.List;

public final class BackendJobHistory {

    private final List<BackendJobRecord> records;

    public BackendJobHistory(final List<BackendJobRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("Backend job history records must not be null.");
        }
        this.records = List.copyOf(records);
    }

    public List<BackendJobRecord> records() {
        return records;
    }

    public int count() {
        return records.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }
}