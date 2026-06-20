/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile;

/**
 * Итог предварительной проверки расчета под выбранный backend.
 */
public enum ClassicalPreflightStatus {

  SUPPORTED,
  NEEDS_APPROXIMATION,
  UNSUPPORTED,
  INVALID;
}