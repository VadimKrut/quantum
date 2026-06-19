/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic;

/**
 * Стабильный код storage-диагностики для UI, CLI и тестов.
 */
public enum ChemistryStorageDiagnosticCode {
  EMPTY_INPUT,
  INVALID_HEADER,
  UNSUPPORTED_VERSION,
  INVALID_LINE,
  DUPLICATE_SECTION,
  UNKNOWN_SECTION,
  MISSING_PROJECT,
  MISSING_END,
  UNKNOWN_MOLECULE_REFERENCE,
  DOMAIN_REJECTED_VALUE,
  IO_FAILURE
}