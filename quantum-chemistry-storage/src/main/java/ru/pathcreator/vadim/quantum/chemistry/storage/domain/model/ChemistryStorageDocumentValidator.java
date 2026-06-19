/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Инварианты storage document, которые не относятся к химической валидности самих агрегатов.
 */
final class ChemistryStorageDocumentValidator {

  private ChemistryStorageDocumentValidator() {
  }

  static void requireUniqueMolecules(final List<Molecule> molecules) {
    for (int i = 0; i < molecules.size(); ++i) {
      final String id = molecules.get(i).id().value();
      for (int j = i + 1; j < molecules.size(); ++j) {
        if (!id.equals(molecules.get(j).id().value())) {
          continue;
        }
        throw new IllegalArgumentException("Storage document contains duplicate molecule id.");
      }
    }
  }

  static void requireUniqueReactions(final List<Reaction> reactions) {
    for (int i = 0; i < reactions.size(); ++i) {
      final String id = reactions.get(i).id().value();
      for (int j = i + 1; j < reactions.size(); ++j) {
        if (!id.equals(reactions.get(j).id().value())) {
          continue;
        }
        throw new IllegalArgumentException("Storage document contains duplicate reaction id.");
      }
    }
  }

  static void requireReactionMoleculesPresent(
      final List<Reaction> reactions,
      final List<Molecule> molecules) {
    for (int i = 0; i < reactions.size(); ++i) {
      ChemistryStorageDocumentValidator.requireSideMoleculesPresent(
          reactions.get(i).reactants(),
          molecules);
      ChemistryStorageDocumentValidator.requireSideMoleculesPresent(
          reactions.get(i).products(),
          molecules);
    }
  }

  private static void requireSideMoleculesPresent(
      final ReactionSide side,
      final List<Molecule> molecules) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final String moleculeId = participants.get(i).molecule().id().value();
      if (ChemistryStorageDocumentValidator.containsMolecule(molecules, moleculeId)) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage reaction references molecule missing from document.");
    }
  }

  private static boolean containsMolecule(
      final List<Molecule> molecules,
      final String moleculeId) {
    for (int i = 0; i < molecules.size(); ++i) {
      if (!molecules.get(i).id().value().equals(moleculeId)) {
        continue;
      }
      return true;
    }
    return false;
  }
}