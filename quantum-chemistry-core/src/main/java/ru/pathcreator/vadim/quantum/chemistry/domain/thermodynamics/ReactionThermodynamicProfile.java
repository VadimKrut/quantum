/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class ReactionThermodynamicProfile {

  private static final double GAS_CONSTANT_KILOJOULE_PER_MOLE_KELVIN = 0.00831446261815324;
  private final String id;
  private final Reaction reaction;
  private final Temperature temperature;
  private final List<MolecularThermodynamicData> molecularData;

  private ReactionThermodynamicProfile(
      final String id,
      final Reaction reaction,
      final Temperature temperature,
      final List<MolecularThermodynamicData> molecularData) {
    this.id = id;
    this.reaction = reaction;
    this.temperature = temperature;
    this.molecularData = molecularData;
  }

  public static ReactionThermodynamicProfile of(
      final String id,
      final Reaction reaction,
      final Temperature temperature,
      final List<MolecularThermodynamicData> molecularData) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Reaction thermodynamics id");
    if (reaction == null) {
      throw new IllegalArgumentException("Thermodynamic reaction must not be null.");
    }
    if (!reaction.balance().balanced()) {
      throw new IllegalArgumentException("Thermodynamic profile requires balanced reaction.");
    }
    if (temperature == null) {
      throw new IllegalArgumentException("Thermodynamic profile temperature must not be null.");
    }
    final List<MolecularThermodynamicData> checkedData =
        List.copyOf(ReactionThermodynamicProfile.requireData(reaction, temperature, molecularData));
    return new ReactionThermodynamicProfile(checkedId, reaction, temperature, checkedData);
  }

  public String id() {
    return this.id;
  }

  public Reaction reaction() {
    return this.reaction;
  }

  public Temperature temperature() {
    return this.temperature;
  }

  public List<MolecularThermodynamicData> molecularData() {
    return this.molecularData;
  }

  public double enthalpyDeltaKiloJoulePerMole() {
    return this.deltaKiloJoulePerMole(ThermodynamicQuantitySelector.ENTHALPY);
  }

  public double entropyDeltaJoulePerMoleKelvin() {
    return this.deltaEntropyJoulePerMoleKelvin();
  }

  public double gibbsFreeEnergyDeltaKiloJoulePerMole() {
    if (this.hasDirectGibbsForEveryParticipant()) {
      return this.deltaKiloJoulePerMole(ThermodynamicQuantitySelector.GIBBS_FREE_ENERGY);
    }
    final double enthalpy = this.enthalpyDeltaKiloJoulePerMole();
    final double entropyKiloJoulePerMoleKelvin = this.entropyDeltaJoulePerMoleKelvin() / 1000.0;
    return enthalpy
        - ThermodynamicUnitConverter.temperatureKelvin(this.temperature)
            * entropyKiloJoulePerMoleKelvin;
  }

  public double logEquilibriumConstant() {
    final double temperatureKelvin = ThermodynamicUnitConverter.temperatureKelvin(this.temperature);
    return -this.gibbsFreeEnergyDeltaKiloJoulePerMole()
        / (GAS_CONSTANT_KILOJOULE_PER_MOLE_KELVIN * temperatureKelvin);
  }

  public double equilibriumConstant() {
    final double value = Math.exp(this.logEquilibriumConstant());
    if (!Double.isFinite(value)) {
      throw new IllegalStateException("Equilibrium constant is outside finite double range.");
    }
    return value;
  }

  private static List<MolecularThermodynamicData> requireData(
      final Reaction reaction, final Temperature temperature, final List<MolecularThermodynamicData> molecularData) {
    if (molecularData == null || molecularData.isEmpty()) {
      throw new IllegalArgumentException("Thermodynamic molecular data must not be empty.");
    }
    for (int i = 0; i < molecularData.size(); ++i) {
      MolecularThermodynamicData data = molecularData.get(i);
      if (data == null) {
        throw new IllegalArgumentException("Thermodynamic molecular data must not be null.");
      }
      if (!data.sameTemperature(temperature)) {
        throw new IllegalArgumentException(
            "Thermodynamic molecular data temperature must match profile temperature.");
      }
      if (!ReactionThermodynamicProfile.reactionContainsMolecule(reaction, data.moleculeId())) {
        throw new IllegalArgumentException(
            "Thermodynamic molecular data references molecule outside reaction.");
      }
      for (int j = i + 1; j < molecularData.size(); ++j) {
        MolecularThermodynamicData other = molecularData.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Thermodynamic molecular data must not be null.");
        }
        if (!data.moleculeId().equals(other.moleculeId())) continue;
        throw new IllegalArgumentException(
            "Thermodynamic molecular data contains duplicate molecule id.");
      }
    }
    ReactionThermodynamicProfile.requireCoverage(reaction.reactants(), molecularData);
    ReactionThermodynamicProfile.requireCoverage(reaction.products(), molecularData);
    return molecularData;
  }

  private static void requireCoverage(
      final ReactionSide side, final List<MolecularThermodynamicData> molecularData) {
    List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (ReactionThermodynamicProfile.dataByMoleculeId(
              molecularData, participants.get(i).molecule().id())
          != null) continue;
      throw new IllegalArgumentException(
          "Thermodynamic molecular data must cover every reaction participant.");
    }
  }

  private static boolean reactionContainsMolecule(
      final Reaction reaction,
      final MoleculeId id
  ) {
    return ReactionThermodynamicProfile.sideContainsMolecule(reaction.reactants(), id)
        || ReactionThermodynamicProfile.sideContainsMolecule(reaction.products(), id);
  }

  private static boolean sideContainsMolecule(
      final ReactionSide side,
      final MoleculeId id
  ) {
    List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (!participants.get(i).molecule().id().equals(id)) continue;
      return true;
    }
    return false;
  }

  private static MolecularThermodynamicData dataByMoleculeId(
      final List<MolecularThermodynamicData> molecularData, final MoleculeId id) {
    for (int i = 0; i < molecularData.size(); ++i) {
      MolecularThermodynamicData data = molecularData.get(i);
      if (!data.moleculeId().equals(id)) continue;
      return data;
    }
    return null;
  }

  private double deltaKiloJoulePerMole(final ThermodynamicQuantitySelector selector) {
    return this.sideEnergyKiloJoulePerMole(this.reaction.products(), selector)
        - this.sideEnergyKiloJoulePerMole(this.reaction.reactants(), selector);
  }

  private double sideEnergyKiloJoulePerMole(
      final ReactionSide side, final ThermodynamicQuantitySelector selector) {
    double sum = 0.0;
    List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      ReactionParticipant participant = participants.get(i);
      MolecularThermodynamicData data =
          ReactionThermodynamicProfile.dataByMoleculeId(
              this.molecularData, participant.molecule().id());
      sum +=
          (double) participant.coefficient().value()
              * ThermodynamicUnitConverter.energyKiloJoulePerMole(selector.energy(data));
    }
    return sum;
  }

  private double deltaEntropyJoulePerMoleKelvin() {
    return this.sideEntropyJoulePerMoleKelvin(this.reaction.products())
        - this.sideEntropyJoulePerMoleKelvin(this.reaction.reactants());
  }

  private double sideEntropyJoulePerMoleKelvin(final ReactionSide side) {
    double sum = 0.0;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final MolecularThermodynamicData data =
          ReactionThermodynamicProfile.dataByMoleculeId(
              this.molecularData, participant.molecule().id());
      if (!data.hasEntropy()) {
        throw new IllegalStateException("Thermodynamic entropy is required for every participant.");
      }
      sum +=
          (double) participant.coefficient().value()
              * ThermodynamicUnitConverter.entropyJoulePerMoleKelvin(data.entropy());
    }
    return sum;
  }

  private boolean hasDirectGibbsForEveryParticipant() {
    for (int i = 0; i < this.molecularData.size(); ++i) {
      if (this.molecularData.get(i).hasGibbsFreeEnergy()) continue;
      return false;
    }
    return true;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionThermodynamicProfile)) {
      return false;
    }
    final ReactionThermodynamicProfile profile = (ReactionThermodynamicProfile) other;
    return Objects.equals(this.id, profile.id)
        && Objects.equals(this.reaction, profile.reaction)
        && Objects.equals(this.temperature, profile.temperature)
        && Objects.equals(this.molecularData, profile.molecularData);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.reaction);
    result = ChemistryHash.include(result, this.temperature);
    result = ChemistryHash.include(result, this.molecularData);
    return result;
  }
}