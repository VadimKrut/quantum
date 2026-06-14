/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.options;

/**
 * Режим текстового export.
 */
public enum ExportTextMode {

    /**
     * Стабильный canonical output adapter.
     */
    CANONICAL,

    /**
     * Source-preserving output, если importer/IR сохранил достаточный source context.
     */
    LOSSLESS_WHEN_AVAILABLE
}