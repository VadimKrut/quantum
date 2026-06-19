/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.redox;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostate;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class RedoxModel {

  private final String id;
  private final MolecularMicrostateSet microstateSet;
  private final List<ElectronTransferTransition> transitions;

  private RedoxModel(
      final String id,
      final MolecularMicrostateSet microstateSet,
      final List<ElectronTransferTransition> transitions) {
    this.id = id;
    this.microstateSet = microstateSet;
    this.transitions = transitions;
  }

  public static RedoxModel of(
      final String id,
      final MolecularMicrostateSet microstateSet,
      final List<ElectronTransferTransition> transitions) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Redox model id");
    if (microstateSet == null) {
      throw new IllegalArgumentException("Redox model microstate set must not be null.");
    }
    final List<ElectronTransferTransition> checkedTransitions =
        List.copyOf(RedoxModel.requireTransitions(microstateSet, transitions));
    return new RedoxModel(checkedId, microstateSet, checkedTransitions);
  }

  public String id() {
    return this.id;
  }

  public MolecularMicrostateSet microstateSet() {
    return this.microstateSet;
  }

  public List<ElectronTransferTransition> transitions() {
    return this.transitions;
  }

  private static List<ElectronTransferTransition> requireTransitions(
      final MolecularMicrostateSet microstateSet, final List<ElectronTransferTransition> transitions) {
    if (transitions == null || transitions.isEmpty()) {
      throw new IllegalArgumentException("Redox model transitions must not be empty.");
    }
    for (int i = 0; i < transitions.size(); ++i) {
      ElectronTransferTransition transition = transitions.get(i);
      if (transition == null) {
        throw new IllegalArgumentException("Redox transition must not be null.");
      }
      RedoxModel.validateTransition(microstateSet, transition);
      for (int j = i + 1; j < transitions.size(); ++j) {
        ElectronTransferTransition other = transitions.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Redox transition must not be null.");
        }
        if (!RedoxModel.sameTransition(transition, other)) continue;
        throw new IllegalArgumentException("Redox model contains duplicate transition.");
      }
    }
    return transitions;
  }

  private static void validateTransition(
      final MolecularMicrostateSet microstateSet, final ElectronTransferTransition transition) {
    final MolecularMicrostate reduced =
        RedoxModel.stateByLabel(microstateSet, transition.reducedStateLabel());
    final MolecularMicrostate oxidized =
        RedoxModel.stateByLabel(microstateSet, transition.oxidizedStateLabel());
    final Molecule reducedMolecule = reduced.molecule();
    final Molecule oxidizedMolecule = oxidized.molecule();
    if (!reducedMolecule.formula().equals(oxidizedMolecule.formula())) {
      throw new IllegalArgumentException("Redox transition must keep molecular formula.");
    }
    final int chargeDelta = oxidizedMolecule.charge().value() - reducedMolecule.charge().value();
    if (chargeDelta != transition.electronCount()) {
      throw new IllegalArgumentException(
          "Redox transition charge delta must match transferred electron count.");
    }
    RedoxModel.requireCenterAtomsPresent(reducedMolecule, transition.center());
    RedoxModel.requireCenterAtomsPresent(oxidizedMolecule, transition.center());
  }

  private static MolecularMicrostate stateByLabel(
      final MolecularMicrostateSet microstateSet, final String label) {
    final List<MolecularMicrostate> states = microstateSet.states();
    for (int i = 0; i < states.size(); ++i) {
      final MolecularMicrostate state = states.get(i);
      if (!state.label().equals(label)) continue;
      return state;
    }
    throw new IllegalArgumentException("Redox transition references unknown microstate.");
  }

  private static void requireCenterAtomsPresent(
      final Molecule molecule,
      final RedoxCenter center
  ) {
    final List<Atom> atoms = molecule.atoms();
    final List<AtomId> atomIds = center.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      boolean found = false;
      for (int j = 0; j < atoms.size(); ++j) {
        if (!atoms.get(j).id().equals(atomIds.get(i))) continue;
        found = true;
        break;
      }
      if (found) continue;
      throw new IllegalArgumentException("Redox center atom is absent from microstate molecule.");
    }
  }

  private static boolean sameTransition(
      final ElectronTransferTransition first, final ElectronTransferTransition second) {
    return first.center().equals(second.center())
        && first.reducedStateLabel().equals(second.reducedStateLabel())
        && first.oxidizedStateLabel().equals(second.oxidizedStateLabel());
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RedoxModel)) {
      return false;
    }
    final RedoxModel model = (RedoxModel) other;
    return Objects.equals(this.id, model.id)
        && Objects.equals(this.microstateSet, model.microstateSet)
        && Objects.equals(this.transitions, model.transitions);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.microstateSet);
    result = ChemistryHash.include(result, this.transitions);
    return result;
  }
}