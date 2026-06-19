/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class ReactionParticipant {

  private final Molecule molecule;
  private final StoichiometricCoefficient coefficient;

  private ReactionParticipant(
      final Molecule molecule,
      final StoichiometricCoefficient coefficient
  ) {
    this.molecule = molecule;
    this.coefficient = coefficient;
  }

  public static ReactionParticipant of(
      final Molecule molecule,
      final StoichiometricCoefficient coefficient
  ) {
    if (molecule == null) {
      throw new IllegalArgumentException("Reaction participant molecule must not be null.");
    }
    final StoichiometricCoefficient checkedCoefficient =
        coefficient == null ? StoichiometricCoefficient.ONE : coefficient;
    return new ReactionParticipant(molecule, checkedCoefficient);
  }

  public Molecule molecule() {
    return this.molecule;
  }

  public StoichiometricCoefficient coefficient() {
    return this.coefficient;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionParticipant)) {
      return false;
    }
    final ReactionParticipant participant = (ReactionParticipant) other;
    return Objects.equals(this.molecule, participant.molecule)
        && Objects.equals(this.coefficient, participant.coefficient);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.molecule);
    result = ChemistryHash.include(result, this.coefficient);
    return result;
  }
}