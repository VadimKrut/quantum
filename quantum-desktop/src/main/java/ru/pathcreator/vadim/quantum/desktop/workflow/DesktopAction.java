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
 * Набор действий, которые desktop workflow может выполнить и показать пользователю.
 */
public enum DesktopAction {

    IMPORT,
    VALIDATE,
    INSPECT,
    RESOURCES,
    CIRCUIT,
    PREFLIGHT,
    SIMULATE,
    COMPILE,
    WORKFLOW,
    BENCHMARK,
    COMPATIBILITY,
    CROSS_FORMAT,
    REGRESSION,
    READINESS,
    DOCTOR,
    BACKEND_DRY_RUN,
    JSON,
    PRODUCT_AUDIT,
    PRODUCT_REPORT,
    PRODUCT_DISTRIBUTION
}