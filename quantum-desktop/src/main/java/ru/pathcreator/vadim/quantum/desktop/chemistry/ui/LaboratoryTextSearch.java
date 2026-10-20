/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryExperimentIndex;

/** Search presentation only: original scientific names and stored text are never rewritten. */
final class LaboratoryTextSearch {
  private LaboratoryTextSearch() {}

  static boolean matches(final String text, final String query) {
    final String normalized = LaboratoryExperimentIndex.normalize(text);
    return java.util.Arrays.stream(LaboratoryExperimentIndex.normalize(query).split(" "))
        .allMatch(normalized::contains);
  }
}
