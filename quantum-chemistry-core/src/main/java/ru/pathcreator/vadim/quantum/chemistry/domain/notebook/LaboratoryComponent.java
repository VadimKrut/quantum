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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** An occurrence of a core molecule with independent plan, fact and provenance. */
public record LaboratoryComponent(
    String id,
    Molecule molecule,
    ReactionComponentRole role,
    LaboratoryQuantity planned,
    LaboratoryQuantity actual,
    LaboratoryEvidence evidence,
    LaboratoryMaterialProperties properties) {
  public LaboratoryComponent(
      final String id,
      final Molecule molecule,
      final ReactionComponentRole role,
      final LaboratoryQuantity planned,
      final LaboratoryQuantity actual,
      final LaboratoryEvidence evidence) {
    this(id, molecule, role, planned, actual, evidence, LaboratoryMaterialProperties.EMPTY);
  }

  public LaboratoryComponent(
      final String id, final Molecule molecule, final ReactionComponentRole role) {
    this(id, molecule, role, null, null, LaboratoryEvidence.PLANNED);
  }

  public LaboratoryComponent {
    id = IdentifierValue.requireIdentifier(id, "Laboratory component id");
    Objects.requireNonNull(molecule, "Component molecule");
    Objects.requireNonNull(role, "Component role");
    Objects.requireNonNull(evidence, "Component evidence");
    Objects.requireNonNull(properties, "Component material properties");
  }

  public LaboratoryComponent withMolecule(
      final Molecule replacement, final ReactionComponentRole nextRole) {
    return new LaboratoryComponent(
        this.id, replacement, nextRole, this.planned, this.actual, this.evidence, this.properties);
  }
}
