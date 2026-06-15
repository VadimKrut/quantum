/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

/**
 * Стабильный код диагностики conservative transformation layer.
 */
public enum TransformationDiagnosticCode {

    UNBOUND_PARAMETER_SYMBOL,
    UNKNOWN_PARAMETER_CONSTANT,
    NON_STATIC_COMPOSITE_GATE_OPERATION,
    MISSING_GATE_DECOMPOSITION_RULE,
    TARGET_LOWERING_REQUIRED,
    TARGET_UNSUPPORTED_WITHOUT_LOSS,
    UNSUPPORTED_TRANSFORMATION_OPERATION
}