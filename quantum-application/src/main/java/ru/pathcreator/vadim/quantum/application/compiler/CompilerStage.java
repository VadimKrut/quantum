/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compiler;

/**
 * Именованный этап compiler pipeline.
 */
public enum CompilerStage {

    INITIAL_VALIDATION,
    INITIAL_INSPECTION,
    INITIAL_PREFLIGHT,
    TRANSFORMATION,
    TRANSFORMED_VALIDATION,
    TRANSFORMED_INSPECTION,
    TRANSFORMED_PREFLIGHT,
    EXPORT
}