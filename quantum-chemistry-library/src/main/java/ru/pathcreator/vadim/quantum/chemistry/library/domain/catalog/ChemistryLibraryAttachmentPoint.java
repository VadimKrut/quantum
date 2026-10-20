/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;

/** An existing atom receives a new bond; no atom is implicitly removed or substituted. */
public record ChemistryLibraryAttachmentPoint(AtomId atomId, String label, BondType bondType) {
  public ChemistryLibraryAttachmentPoint {
    Objects.requireNonNull(atomId, "Attachment atom");
    label = TextValue.requireText(label, "Название точки присоединения");
    if (bondType != BondType.SINGLE && bondType != BondType.DOUBLE && bondType != BondType.TRIPLE) {
      throw new IllegalArgumentException(
          "Для точки присоединения выберите одинарную, двойную или тройную связь.");
    }
  }
}
