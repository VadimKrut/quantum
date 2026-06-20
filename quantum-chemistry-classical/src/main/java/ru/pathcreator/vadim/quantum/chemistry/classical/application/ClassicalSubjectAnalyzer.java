/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Анализирует размер и геометрическую полноту chemistry-core subject.
 */
public final class ClassicalSubjectAnalyzer {

  public ClassicalSubjectSize analyze(final ChemistrySubject subject) {
    if (subject == null) {
      throw new IllegalArgumentException("Classical subject analyzer subject must not be null.");
    }
    if (subject.subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return this.analyzeMolecule((Molecule) subject);
    }
    if (subject.subjectKind() == ChemistrySubjectKind.REACTION) {
      return this.analyzeReaction((Reaction) subject);
    }
    throw new IllegalArgumentException("Unsupported chemistry subject kind.");
  }

  private ClassicalSubjectSize analyzeMolecule(final Molecule molecule) {
    return ClassicalSubjectSize.of(
        molecule.atomCount(),
        molecule.bondCount(),
        molecule.electronicConfiguration().electronCount(),
        1L,
        this.completeGeometry(molecule));
  }

  private ClassicalSubjectSize analyzeReaction(final Reaction reaction) {
    long atomCount = 0L;
    long bondCount = 0L;
    long electronCount = 0L;
    long participantCount = 0L;
    boolean completeGeometry = true;
    participantCount += reaction.reactants().participantCount();
    participantCount += reaction.products().participantCount();
    final List<ReactionParticipant> reactants = reaction.reactants().participants();
    for (int i = 0; i < reactants.size(); ++i) {
      final ReactionParticipant participant = reactants.get(i);
      final Molecule molecule = participant.molecule();
      final long coefficient = participant.coefficient().value();
      atomCount = Math.addExact(atomCount, Math.multiplyExact(coefficient, molecule.atomCount()));
      bondCount = Math.addExact(bondCount, Math.multiplyExact(coefficient, molecule.bondCount()));
      electronCount = Math.addExact(
          electronCount,
          Math.multiplyExact(
              coefficient,
              molecule.electronicConfiguration().electronCount()));
      completeGeometry = completeGeometry && this.completeGeometry(molecule);
    }
    final List<ReactionParticipant> products = reaction.products().participants();
    for (int i = 0; i < products.size(); ++i) {
      final ReactionParticipant participant = products.get(i);
      final Molecule molecule = participant.molecule();
      final long coefficient = participant.coefficient().value();
      atomCount = Math.addExact(atomCount, Math.multiplyExact(coefficient, molecule.atomCount()));
      bondCount = Math.addExact(bondCount, Math.multiplyExact(coefficient, molecule.bondCount()));
      electronCount = Math.addExact(
          electronCount,
          Math.multiplyExact(
              coefficient,
              molecule.electronicConfiguration().electronCount()));
      completeGeometry = completeGeometry && this.completeGeometry(molecule);
    }
    return ClassicalSubjectSize.of(
        atomCount,
        bondCount,
        electronCount,
        participantCount,
        completeGeometry);
  }

  private boolean completeGeometry(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (atoms.get(i).hasCoordinate()) {
        continue;
      }
      return false;
    }
    return true;
  }
}