/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

public enum BackendDiagnosticCode {

    VALIDATION_FAILED,
    PREFLIGHT_FAILED,
    COMPILATION_FAILED,
    SIMULATION_FAILED,
    JOB_NOT_FOUND,
    JOB_NOT_CANCELLABLE,
    JOB_NOT_FINISHED
}