/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

public interface QuantumBackend {

    BackendDescriptor descriptor();

    BackendSubmissionResult submit(
        QuantumProgram program,
        BackendJobOptions options
    );

    BackendStatusResult status(BackendJobId jobId);

    BackendExecutionResult result(BackendJobId jobId);

    BackendStatusResult cancel(BackendJobId jobId);
}