/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend;

/**
 * Способ исполнения классического backend.
 */
public enum ClassicalBackendExecutionMode {

  LOCAL_IN_PROCESS,
  LOCAL_EXTERNAL_PROCESS,
  REMOTE_SERVICE,
  MANUAL_OFFLINE,
  PLANNING_ONLY;
}