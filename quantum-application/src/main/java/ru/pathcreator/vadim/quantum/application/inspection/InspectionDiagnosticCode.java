/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.inspection;

/**
 * Стабильный код диагностики inspection.
 */
public enum InspectionDiagnosticCode {

    NULL_PROGRAM,

    DYNAMIC_QUBIT_REFERENCE_DEPTH_APPROXIMATION,

    CONTROL_FLOW_DEPTH_APPROXIMATION,

    TIMING_DEPTH_APPROXIMATION,

    UNSUPPORTED_OPERATION_FOR_PRECISE_DEPTH,

    TARGET_COMPATIBILITY_WARNING
}