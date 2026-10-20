/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;

/** Stage-local role and optional stoichiometry; neither is an inventory operation. */
public record LaboratoryStageParticipant(
    String componentId, ReactionComponentRole role, StoichiometricCoefficient coefficient) {
  public LaboratoryStageParticipant {
    componentId = IdentifierValue.requireIdentifier(componentId, "Stage component id");
    Objects.requireNonNull(role, "Stage component role");
    if (coefficient != null
        && role != ReactionComponentRole.REACTANT
        && role != ReactionComponentRole.PRODUCT) {
      throw new IllegalArgumentException(
          "Стехиометрический коэффициент задаётся для исходника или продукта.");
    }
  }
}
