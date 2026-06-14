/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

/**
 * Итоговая категория preflight-проверки перед export в конкретный target.
 */
public enum CapabilityPreflightStatus {

    /**
     * IR можно экспортировать без дополнительных adapter-level преобразований.
     */
    EXPORTABLE,

    /**
     * IR можно экспортировать, но adapter должен применить lowering/decomposition.
     */
    LOWERING_REQUIRED,

    /**
     * Target не поддерживает нужные возможности, но это не ошибка самого IR.
     */
    UNSUPPORTED_BY_TARGET,

    /**
     * Экспорт потребовал бы потери семантики, поэтому adapter должен отказать.
     */
    UNSUPPORTED_WITHOUT_LOSS
}