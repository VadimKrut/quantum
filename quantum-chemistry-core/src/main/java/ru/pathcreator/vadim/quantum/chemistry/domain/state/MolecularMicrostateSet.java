/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.state;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormulaTerm;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.SolutionEnvironment;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class MolecularMicrostateSet {

  private static final ElementSymbol HYDROGEN = ElementSymbol.of("H");
  private final String id;
  private final MolecularMicrostate reference;
  private final List<MolecularMicrostate> states;
  private final SolutionEnvironment environment;

  private MolecularMicrostateSet(
      final String id,
      final MolecularMicrostate reference,
      final List<MolecularMicrostate> states,
      final SolutionEnvironment environment) {
    this.id = id;
    this.reference = reference;
    this.states = states;
    this.environment = environment;
  }

  public static MolecularMicrostateSet of(
      final String id,
      final List<MolecularMicrostate> states
  ) {
    return MolecularMicrostateSet.of(id, states, null);
  }

  public static MolecularMicrostateSet of(
      final String id, final List<MolecularMicrostate> states, final SolutionEnvironment environment) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Molecular microstate set id");
    final List<MolecularMicrostate> checkedStates =
        List.copyOf(MolecularMicrostateSet.requireStates(states));
    final MolecularMicrostate reference = MolecularMicrostateSet.referenceState(checkedStates);
    for (int i = 0; i < checkedStates.size(); ++i) {
      MolecularMicrostateSet.validateAgainstReference(reference, checkedStates.get(i));
    }
    MolecularMicrostateSet.validateEnvironment(checkedStates, environment);
    return new MolecularMicrostateSet(checkedId, reference, checkedStates, environment);
  }

  public String id() {
    return this.id;
  }

  public MolecularMicrostate reference() {
    return this.reference;
  }

  public List<MolecularMicrostate> states() {
    return this.states;
  }

  public SolutionEnvironment environment() {
    return this.environment;
  }

  public boolean hasEnvironment() {
    return this.environment != null;
  }

  private static List<MolecularMicrostate> requireStates(final List<MolecularMicrostate> states) {
    if (states == null || states.isEmpty()) {
      throw new IllegalArgumentException("Molecular microstate set must not be empty.");
    }
    boolean hasReference = false;
    for (int i = 0; i < states.size(); ++i) {
      MolecularMicrostate state = states.get(i);
      if (state == null) {
        throw new IllegalArgumentException("Molecular microstate must not be null.");
      }
      if (state.kind() == MolecularMicrostateKind.REFERENCE) {
        if (hasReference) {
          throw new IllegalArgumentException(
              "Molecular microstate set must contain exactly one reference state.");
        }
        hasReference = true;
      }
      for (int j = i + 1; j < states.size(); ++j) {
        MolecularMicrostate other = states.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Molecular microstate must not be null.");
        }
        if (state.label().equals(other.label())) {
          throw new IllegalArgumentException("Molecular microstate labels must be unique.");
        }
        if (!state.molecule().id().equals(other.molecule().id())) continue;
        throw new IllegalArgumentException("Molecular microstate molecule ids must be unique.");
      }
    }
    if (!hasReference) {
      throw new IllegalArgumentException(
          "Molecular microstate set must contain exactly one reference state.");
    }
    return states;
  }

  private static MolecularMicrostate referenceState(final List<MolecularMicrostate> states) {
    for (int i = 0; i < states.size(); ++i) {
      MolecularMicrostate state = states.get(i);
      if (state.kind() != MolecularMicrostateKind.REFERENCE) continue;
      return state;
    }
    throw new IllegalStateException("Molecular microstate set contains no reference state.");
  }

  private static void validateAgainstReference(
      final MolecularMicrostate reference, final MolecularMicrostate state) {
    if (state.kind() == MolecularMicrostateKind.REFERENCE) {
      return;
    }
    final Molecule referenceMolecule = reference.molecule();
    final Molecule molecule = state.molecule();
    switch (state.kind()) {
      case RESONANCE_FORM:
        {
          MolecularMicrostateSet.requireSameFormula(referenceMolecule, molecule, "Resonance form");
          MolecularMicrostateSet.requireSameCharge(referenceMolecule, molecule, "Resonance form");
          MolecularMicrostateSet.requireSameSpin(referenceMolecule, molecule, "Resonance form");
          break;
        }
      case TAUTOMER:
        {
          MolecularMicrostateSet.requireSameFormula(referenceMolecule, molecule, "Tautomer");
          MolecularMicrostateSet.requireSameCharge(referenceMolecule, molecule, "Tautomer");
          break;
        }
      case PROTOMER:
        {
          MolecularMicrostateSet.requireSameFormula(referenceMolecule, molecule, "Protomer");
          MolecularMicrostateSet.requireSameCharge(referenceMolecule, molecule, "Protomer");
          break;
        }
      case IONIZATION_STATE:
        {
          MolecularMicrostateSet.requireSameNonHydrogenFormula(
              referenceMolecule.formula(), molecule.formula());
          MolecularMicrostateSet.requireHydrogenChargeCoupling(referenceMolecule, molecule);
          break;
        }
      case REDOX_STATE:
        {
          MolecularMicrostateSet.requireSameFormula(referenceMolecule, molecule, "Redox state");
          break;
        }
      case REFERENCE:
        {
          break;
        }
      default:
        {
          throw new IllegalStateException("Unsupported molecular microstate kind.");
        }
    }
  }

  private static void validateEnvironment(
      final List<MolecularMicrostate> states, final SolutionEnvironment environment) {
    if (environment == null || !MolecularMicrostateSet.containsAcidBaseState(states)) {
      return;
    }
    if (!environment.hasPH()) {
      throw new IllegalArgumentException("Acid-base microstate set environment must define pH.");
    }
  }

  private static boolean containsAcidBaseState(final List<MolecularMicrostate> states) {
    for (int i = 0; i < states.size(); ++i) {
      final MolecularMicrostate state = states.get(i);
      if (state.kind() != MolecularMicrostateKind.PROTOMER
          && state.kind() != MolecularMicrostateKind.IONIZATION_STATE) continue;
      return true;
    }
    return false;
  }

  private static void requireSameFormula(
      final Molecule reference, final Molecule molecule, final String subjectName) {
    if (!reference.formula().equals(molecule.formula())) {
      throw new IllegalArgumentException(subjectName + " must keep molecular formula.");
    }
  }

  private static void requireSameCharge(
      final Molecule reference,
      final Molecule molecule,
      final String subjectName
  ) {
    if (!reference.charge().equals(molecule.charge())) {
      throw new IllegalArgumentException(subjectName + " must keep molecular charge.");
    }
  }

  private static void requireSameSpin(
      final Molecule reference,
      final Molecule molecule,
      final String subjectName
  ) {
    if (!reference.spinMultiplicity().equals(molecule.spinMultiplicity())) {
      throw new IllegalArgumentException(subjectName + " must keep spin multiplicity.");
    }
  }

  private static void requireSameNonHydrogenFormula(
      final MolecularFormula reference, final MolecularFormula formula) {
    final List<MolecularFormulaTerm> referenceTerms = reference.terms();
    for (int i = 0; i < referenceTerms.size(); ++i) {
      MolecularFormulaTerm term = referenceTerms.get(i);
      if (term.symbol().equals(HYDROGEN) || term.count() == formula.countOf(term.symbol()))
        continue;
      throw new IllegalArgumentException(
          "Ionization state must keep non-hydrogen molecular formula.");
    }
    final List<MolecularFormulaTerm> terms = formula.terms();
    for (int i = 0; i < terms.size(); ++i) {
      final MolecularFormulaTerm term = terms.get(i);
      if (term.symbol().equals(HYDROGEN) || reference.countOf(term.symbol()) == term.count())
        continue;
      throw new IllegalArgumentException(
          "Ionization state must keep non-hydrogen molecular formula.");
    }
  }

  private static void requireHydrogenChargeCoupling(
      final Molecule reference,
      final Molecule molecule
  ) {
    int hydrogenDelta =
        molecule.formula().countOf(HYDROGEN) - reference.formula().countOf(HYDROGEN);
    final int chargeDelta = molecule.charge().value() - reference.charge().value();
    if (hydrogenDelta == 0 || chargeDelta != hydrogenDelta) {
      throw new IllegalArgumentException(
          "Ionization state hydrogen delta must match molecular charge delta.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularMicrostateSet)) {
      return false;
    }
    final MolecularMicrostateSet set = (MolecularMicrostateSet) other;
    return Objects.equals(this.id, set.id)
        && Objects.equals(this.reference, set.reference)
        && Objects.equals(this.states, set.states)
        && Objects.equals(this.environment, set.environment);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.reference);
    result = ChemistryHash.include(result, this.states);
    result = ChemistryHash.include(result, this.environment);
    return result;
  }
}