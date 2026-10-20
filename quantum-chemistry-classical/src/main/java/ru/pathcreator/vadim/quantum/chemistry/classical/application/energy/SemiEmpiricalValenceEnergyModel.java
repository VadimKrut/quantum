/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class SemiEmpiricalValenceEnergyModel {

  private static final double HARTREE_PER_KCAL_MOL = 0.001593601437;
  private static final double DEFAULT_BOND_LENGTH_ANGSTROM = 1.45;
  private static final double DEFAULT_VALENCE_ALPHA = -0.020;
  private static final double DEFAULT_VALENCE_BETA = -0.035;

  public SemiEmpiricalValenceEnergyResult evaluate(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Semiempirical molecule must not be null.");
    }
    final HashMap<AtomId, Atom> atomsById = new HashMap<AtomId, Atom>(molecule.atomCount() * 2);
    final List<Atom> atoms = molecule.atoms();
    double atomicEnergy = 0.0;
    double chargePenalty = 0.0;
    double radicalPenalty = 0.0;
    double heteroAtomStabilization = 0.0;
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      atomsById.put(atom.id(), atom);
      final ValenceParameters parameters =
          SemiEmpiricalValenceEnergyModel.parameters(atom.element().symbol().value());
      atomicEnergy += parameters.atomicEnergyHartree();
      chargePenalty += Math.abs(atom.formalCharge().value()) * parameters.chargePenaltyHartree();
      radicalPenalty +=
          atom.radicalState().unpairedElectrons() * parameters.radicalPenaltyHartree();
      if (atom.element().atomicNumber() != 1 && atom.element().atomicNumber() != 6) {
        heteroAtomStabilization += parameters.heteroAtomStabilizationHartree();
      }
    }
    double bondEnergy = 0.0;
    double strainEnergy = 0.0;
    double conjugationEnergy = 0.0;
    int aromaticBondCount = 0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Atom first = atomsById.get(bond.firstAtomId());
      final Atom second = atomsById.get(bond.secondAtomId());
      final double order = SemiEmpiricalValenceEnergyModel.bondOrder(bond.type());
      final ValenceParameters firstParameters =
          SemiEmpiricalValenceEnergyModel.parameters(first.element().symbol().value());
      final ValenceParameters secondParameters =
          SemiEmpiricalValenceEnergyModel.parameters(second.element().symbol().value());
      final double beta =
          0.5 * (firstParameters.bondBetaHartree() + secondParameters.bondBetaHartree());
      bondEnergy += beta * order;
      if (bond.type() == BondType.AROMATIC) {
        ++aromaticBondCount;
        conjugationEnergy -= 0.010;
      } else if (bond.type() == BondType.DOUBLE || bond.type() == BondType.TRIPLE) {
        conjugationEnergy -= 0.004 * order;
      }
      strainEnergy +=
          SemiEmpiricalValenceEnergyModel.lengthStrain(first, second, bond.type(), order);
    }
    final double aromaticStabilization = aromaticBondCount >= 5 ? -0.006 * aromaticBondCount : 0.0;
    final double torsionPenalty = SemiEmpiricalValenceEnergyModel.torsionPenalty(molecule);
    final double formulaScaleCorrection =
        HARTREE_PER_KCAL_MOL * (0.015 * molecule.atomCount() + 0.025 * molecule.bondCount());
    final double electronicEnergy =
        atomicEnergy
            + bondEnergy
            + heteroAtomStabilization
            + conjugationEnergy
            + aromaticStabilization;
    final double totalEnergy =
        electronicEnergy
            + chargePenalty
            + radicalPenalty
            + strainEnergy
            + torsionPenalty
            + formulaScaleCorrection;
    return SemiEmpiricalValenceEnergyResult.of(
        totalEnergy,
        electronicEnergy,
        bondEnergy,
        strainEnergy,
        conjugationEnergy + aromaticStabilization,
        chargePenalty,
        radicalPenalty,
        torsionPenalty,
        molecule.atomCount(),
        molecule.bondCount(),
        aromaticBondCount);
  }

  private static double bondOrder(final BondType type) {
    if (type == null || !type.hasDefinedOrder()) {
      return 1.0;
    }
    return type.orderValue();
  }

  private static double lengthStrain(
      final Atom first, final Atom second, final BondType type, final double order) {
    if (!first.hasCoordinate() || !second.hasCoordinate()) {
      return 0.0;
    }
    final double length =
        SemiEmpiricalValenceEnergyModel.distance(first.coordinate(), second.coordinate());
    final double expected = SemiEmpiricalValenceEnergyModel.expectedLength(type, order);
    final double delta = length - expected;
    return Math.min(0.080, 0.012 * delta * delta);
  }

  private static double expectedLength(final BondType type, final double order) {
    if (type == BondType.AROMATIC) {
      return 1.39;
    }
    if (order >= 2.9) {
      return 1.20;
    }
    if (order >= 1.9) {
      return 1.33;
    }
    return DEFAULT_BOND_LENGTH_ANGSTROM;
  }

  private static double distance(final Coordinate3D first, final Coordinate3D second) {
    final double dx = first.x() - second.x();
    final double dy = first.y() - second.y();
    final double dz = first.z() - second.z();
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static double torsionPenalty(final Molecule molecule) {
    final List<TorsionAngle> torsions = molecule.conformation().torsionAngles();
    double result = 0.0;
    for (int i = 0; i < torsions.size(); ++i) {
      final double radians = Math.toRadians(torsions.get(i).degrees());
      result += 0.0015 * (1.0 + Math.cos(3.0 * radians));
    }
    return result;
  }

  private static ValenceParameters parameters(final String symbol) {
    final String normalized = symbol.toUpperCase(Locale.ROOT);
    if (normalized.equals("H")) {
      return ValenceParameters.of(-0.500, -0.030, 0.018, 0.010, 0.000);
    }
    if (normalized.equals("C")) {
      return ValenceParameters.of(DEFAULT_VALENCE_ALPHA, DEFAULT_VALENCE_BETA, 0.030, 0.016, 0.000);
    }
    if (normalized.equals("N")) {
      return ValenceParameters.of(-0.032, -0.040, 0.040, 0.020, -0.006);
    }
    if (normalized.equals("O")) {
      return ValenceParameters.of(-0.045, -0.045, 0.050, 0.024, -0.009);
    }
    if (normalized.equals("S")) {
      return ValenceParameters.of(-0.038, -0.033, 0.045, 0.022, -0.005);
    }
    if (normalized.equals("P")) {
      return ValenceParameters.of(-0.035, -0.030, 0.045, 0.021, -0.004);
    }
    if (normalized.equals("F")) {
      return ValenceParameters.of(-0.060, -0.050, 0.055, 0.026, -0.011);
    }
    if (normalized.equals("CL")) {
      return ValenceParameters.of(-0.050, -0.038, 0.052, 0.024, -0.008);
    }
    if (normalized.equals("BR")) {
      return ValenceParameters.of(-0.047, -0.034, 0.055, 0.025, -0.007);
    }
    if (normalized.equals("I")) {
      return ValenceParameters.of(-0.043, -0.030, 0.060, 0.027, -0.006);
    }
    if (normalized.equals("B") || normalized.equals("SI") || normalized.equals("SE")) {
      return ValenceParameters.of(-0.026, -0.028, 0.042, 0.021, -0.004);
    }
    return ValenceParameters.of(DEFAULT_VALENCE_ALPHA, DEFAULT_VALENCE_BETA, 0.050, 0.025, -0.002);
  }

  private static final class ValenceParameters {
    private final double atomicEnergyHartree;
    private final double bondBetaHartree;
    private final double chargePenaltyHartree;
    private final double radicalPenaltyHartree;
    private final double heteroAtomStabilizationHartree;

    private ValenceParameters(
        final double atomicEnergyHartree,
        final double bondBetaHartree,
        final double chargePenaltyHartree,
        final double radicalPenaltyHartree,
        final double heteroAtomStabilizationHartree) {
      this.atomicEnergyHartree = atomicEnergyHartree;
      this.bondBetaHartree = bondBetaHartree;
      this.chargePenaltyHartree = chargePenaltyHartree;
      this.radicalPenaltyHartree = radicalPenaltyHartree;
      this.heteroAtomStabilizationHartree = heteroAtomStabilizationHartree;
    }

    static ValenceParameters of(
        final double atomicEnergyHartree,
        final double bondBetaHartree,
        final double chargePenaltyHartree,
        final double radicalPenaltyHartree,
        final double heteroAtomStabilizationHartree) {
      return new ValenceParameters(
          atomicEnergyHartree,
          bondBetaHartree,
          chargePenaltyHartree,
          radicalPenaltyHartree,
          heteroAtomStabilizationHartree);
    }

    double atomicEnergyHartree() {
      return this.atomicEnergyHartree;
    }

    double bondBetaHartree() {
      return this.bondBetaHartree;
    }

    double chargePenaltyHartree() {
      return this.chargePenaltyHartree;
    }

    double radicalPenaltyHartree() {
      return this.radicalPenaltyHartree;
    }

    double heteroAtomStabilizationHartree() {
      return this.heteroAtomStabilizationHartree;
    }
  }
}
