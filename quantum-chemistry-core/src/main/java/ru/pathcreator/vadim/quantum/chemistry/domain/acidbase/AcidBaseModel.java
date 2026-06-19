/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.acidbase;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostate;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class AcidBaseModel {

  private static final ElementSymbol HYDROGEN = ElementSymbol.of("H");
  private final String id;
  private final MolecularMicrostateSet microstateSet;
  private final List<ProtonationTransition> transitions;

  private AcidBaseModel(
      final String id, final MolecularMicrostateSet microstateSet, final List<ProtonationTransition> transitions) {
    this.id = id;
    this.microstateSet = microstateSet;
    this.transitions = transitions;
  }

  public static AcidBaseModel of(
      final String id, final MolecularMicrostateSet microstateSet, final List<ProtonationTransition> transitions) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Acid-base model id");
    if (microstateSet == null) {
      throw new IllegalArgumentException("Acid-base model microstate set must not be null.");
    }
    final List<ProtonationTransition> checkedTransitions =
        List.copyOf(AcidBaseModel.requireTransitions(microstateSet, transitions));
    if (microstateSet.hasEnvironment() && !microstateSet.environment().hasPH()) {
      throw new IllegalArgumentException("Acid-base model environment must define pH.");
    }
    return new AcidBaseModel(checkedId, microstateSet, checkedTransitions);
  }

  public String id() {
    return this.id;
  }

  public MolecularMicrostateSet microstateSet() {
    return this.microstateSet;
  }

  public List<ProtonationTransition> transitions() {
    return this.transitions;
  }

  private static List<ProtonationTransition> requireTransitions(
      final MolecularMicrostateSet microstateSet, final List<ProtonationTransition> transitions) {
    if (transitions == null || transitions.isEmpty()) {
      throw new IllegalArgumentException("Acid-base model transitions must not be empty.");
    }
    for (int i = 0; i < transitions.size(); ++i) {
      ProtonationTransition transition = transitions.get(i);
      if (transition == null) {
        throw new IllegalArgumentException("Acid-base transition must not be null.");
      }
      AcidBaseModel.validateTransition(microstateSet, transition);
      for (int j = i + 1; j < transitions.size(); ++j) {
        ProtonationTransition other = transitions.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Acid-base transition must not be null.");
        }
        if (!AcidBaseModel.sameTransition(transition, other)) continue;
        throw new IllegalArgumentException("Acid-base model contains duplicate transition.");
      }
    }
    return transitions;
  }

  private static void validateTransition(
      final MolecularMicrostateSet microstateSet, final ProtonationTransition transition) {
    final MolecularMicrostate protonated =
        AcidBaseModel.stateByLabel(microstateSet, transition.protonatedStateLabel());
    final MolecularMicrostate deprotonated =
        AcidBaseModel.stateByLabel(microstateSet, transition.deprotonatedStateLabel());
    AcidBaseModel.requireSiteAtomPresent(protonated.molecule(), transition.site());
    AcidBaseModel.requireSiteAtomPresent(deprotonated.molecule(), transition.site());
    AcidBaseModel.requireDeprotonationStoichiometry(protonated.molecule(), deprotonated.molecule());
  }

  private static MolecularMicrostate stateByLabel(
      final MolecularMicrostateSet microstateSet, final String label) {
    final List<MolecularMicrostate> states = microstateSet.states();
    for (int i = 0; i < states.size(); ++i) {
      final MolecularMicrostate state = states.get(i);
      if (!state.label().equals(label)) continue;
      return state;
    }
    throw new IllegalArgumentException("Acid-base transition references unknown microstate.");
  }

  private static void requireSiteAtomPresent(
      final Molecule molecule,
      final AcidBaseSite site
  ) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).id().equals(site.atomId())) continue;
      return;
    }
    throw new IllegalArgumentException("Acid-base site atom is absent from microstate molecule.");
  }

  private static void requireDeprotonationStoichiometry(
      final Molecule protonated, final Molecule deprotonated) {
    final int hydrogenDelta =
        protonated.formula().countOf(HYDROGEN) - deprotonated.formula().countOf(HYDROGEN);
    final int chargeDelta = protonated.charge().value() - deprotonated.charge().value();
    if (hydrogenDelta != 1 || chargeDelta != 1) {
      throw new IllegalArgumentException(
          "Protonation transition must differ by exactly one proton and one charge unit.");
    }
  }

  private static boolean sameTransition(
      final ProtonationTransition first,
      final ProtonationTransition second
  ) {
    return first.site().equals(second.site())
        && first.protonatedStateLabel().equals(second.protonatedStateLabel())
        && first.deprotonatedStateLabel().equals(second.deprotonatedStateLabel());
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AcidBaseModel)) {
      return false;
    }
    final AcidBaseModel model = (AcidBaseModel) other;
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