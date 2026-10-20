/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Собирает сторону продуктов из основного продукта transform и всех явно созданных coproducts. */
final class ReactionProductSideFactory {

  private ReactionProductSideFactory() {}

  static ReactionSide fromTransformProduct(final MolecularTransformProduct product) {
    if (product == null) {
      throw new IllegalArgumentException("Molecular transform product must not be null.");
    }
    final ArrayList<ReactionParticipant> participants =
        new ArrayList<ReactionParticipant>(1 + product.coproducts().size());
    participants.add(ReactionParticipant.of(product.product(), StoichiometricCoefficient.ONE));
    for (int i = 0; i < product.coproducts().size(); ++i) {
      participants.add(
          ReactionParticipant.of(product.coproducts().get(i), StoichiometricCoefficient.ONE));
    }
    return ReactionSide.of(participants);
  }

  static ReactionSide fromSingleProduct(final Molecule product) {
    if (product == null) {
      throw new IllegalArgumentException("Reaction product molecule must not be null.");
    }
    return ReactionSide.of(List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE)));
  }
}
