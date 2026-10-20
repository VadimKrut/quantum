/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Planned content copied from an editor; observations and actual quantities never propagate. */
public record LaboratoryFragment(
    MoleculeDrawing drawing, ReactionComponentRole role, LaboratoryQuantity planned) {
  public LaboratoryFragment {
    Objects.requireNonNull(drawing);
    Objects.requireNonNull(role);
  }
}
