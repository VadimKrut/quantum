/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Локальная bonded force-field модель для быстрых геометрических градиентов.
 */
public final class BondedForceFieldEnergyModel implements GeometryEnergyModel {

  private static final double BOND_FORCE_CONSTANT = 320.0;
  private static final double NONBONDED_FORCE_CONSTANT = 0.015;
  private static final double MIN_DISTANCE_ANGSTROM = 0.20;
  private Molecule cachedMolecule;
  private EvaluationContext cachedContext;

  @Override
  public String name() {
    return "bonded_force_field";
  }

  @Override
  public String energyUnit() {
    return "arb";
  }

  @Override
  public String gradientUnit() {
    return "arb/angstrom";
  }

  @Override
  public String diagnosticMessage() {
    return "Local geometry derivative uses deterministic bonded force-field approximation.";
  }

  @Override
  public double energy(final Molecule molecule, final double[] coordinatesAngstrom) {
    if (molecule == null) {
      throw new IllegalArgumentException("Force-field molecule must not be null.");
    }
    if (coordinatesAngstrom == null || coordinatesAngstrom.length != molecule.atomCount() * 3) {
      throw new IllegalArgumentException(
          "Force-field coordinate count must match molecule atom count.");
    }
    final EvaluationContext context = this.context(molecule);
    double energy = 0.0;
    for (int i = 0; i < context.bondCount; ++i) {
      final int firstIndex = context.bondFirstIndices[i];
      final int secondIndex = context.bondSecondIndices[i];
      final double distance =
          BondedForceFieldEnergyModel.distance(coordinatesAngstrom, firstIndex, secondIndex);
      final double delta = distance - context.bondEquilibriumDistances[i];
      energy += BOND_FORCE_CONSTANT * context.bondOrderWeights[i] * delta * delta;
    }
    for (int first = 0; first < molecule.atomCount(); ++first) {
      for (int second = first + 1; second < molecule.atomCount(); ++second) {
        if (context.directlyBonded(first, second)) {
          continue;
        }
        final double distance =
            Math.max(
                MIN_DISTANCE_ANGSTROM,
                BondedForceFieldEnergyModel.distance(coordinatesAngstrom, first, second));
        final double radiusSum = context.covalentRadii[first] + context.covalentRadii[second];
        final double ratio = radiusSum / distance;
        final double ratio6 = Math.pow(ratio, 6.0);
        energy += NONBONDED_FORCE_CONSTANT * ratio6 * ratio6;
      }
    }
    return energy;
  }

  @Override
  public void gradient(
      final Molecule molecule,
      final double[] coordinatesAngstrom,
      final double finiteDifferenceStepAngstrom,
      final double[] gradient) {
    if (!Double.isFinite(finiteDifferenceStepAngstrom) || finiteDifferenceStepAngstrom <= 0.0) {
      throw new IllegalArgumentException("Force-field finite difference step must be positive.");
    }
    if (gradient == null || gradient.length != coordinatesAngstrom.length) {
      throw new IllegalArgumentException("Force-field gradient shape must match coordinates.");
    }
    final double[] shifted = coordinatesAngstrom.clone();
    for (int i = 0; i < coordinatesAngstrom.length; ++i) {
      final double original = shifted[i];
      shifted[i] = original + finiteDifferenceStepAngstrom;
      final double plusEnergy = this.energy(molecule, shifted);
      shifted[i] = original - finiteDifferenceStepAngstrom;
      final double minusEnergy = this.energy(molecule, shifted);
      shifted[i] = original;
      gradient[i] = (plusEnergy - minusEnergy) / (2.0 * finiteDifferenceStepAngstrom);
    }
  }

  private synchronized EvaluationContext context(final Molecule molecule) {
    if (this.cachedMolecule == molecule && this.cachedContext != null) {
      return this.cachedContext;
    }
    final EvaluationContext context = EvaluationContext.of(molecule);
    this.cachedMolecule = molecule;
    this.cachedContext = context;
    return context;
  }

  private static Map<AtomId, Integer> atomIndexById(final List<Atom> atoms) {
    final HashMap<AtomId, Integer> atomIndexById = new HashMap<AtomId, Integer>(atoms.size() * 2);
    for (int i = 0; i < atoms.size(); ++i) {
      atomIndexById.put(atoms.get(i).id(), Integer.valueOf(i));
    }
    return atomIndexById;
  }

  private static double equilibriumDistanceAngstrom(
      final Atom firstAtom, final Atom secondAtom, final BondType bondType) {
    final double radiusSum =
        BondedForceFieldEnergyModel.covalentRadiusAngstrom(firstAtom)
            + BondedForceFieldEnergyModel.covalentRadiusAngstrom(secondAtom);
    if (bondType == BondType.TRIPLE) {
      return radiusSum * 0.78;
    }
    if (bondType == BondType.DOUBLE) {
      return radiusSum * 0.87;
    }
    if (bondType == BondType.AROMATIC) {
      return radiusSum * 0.92;
    }
    return radiusSum;
  }

  private static double bondOrderWeight(final BondType bondType) {
    if (!bondType.hasDefinedOrder()) {
      return 1.0;
    }
    return bondType.orderValue();
  }

  private static double covalentRadiusAngstrom(final Atom atom) {
    switch (atom.element().atomicNumber()) {
      case 1:
        return 0.31;
      case 5:
        return 0.84;
      case 6:
        return 0.76;
      case 7:
        return 0.71;
      case 8:
        return 0.66;
      case 9:
        return 0.57;
      case 15:
        return 1.07;
      case 16:
        return 1.05;
      case 17:
        return 1.02;
      case 35:
        return 1.20;
      case 53:
        return 1.39;
      default:
        return Math.max(0.35, 0.25 + 0.012 * (double) atom.element().atomicNumber());
    }
  }

  private static double distance(
      final double[] coordinatesAngstrom, final int firstIndex, final int secondIndex) {
    final int firstOffset = firstIndex * 3;
    final int secondOffset = secondIndex * 3;
    final double dx = coordinatesAngstrom[firstOffset] - coordinatesAngstrom[secondOffset];
    final double dy = coordinatesAngstrom[firstOffset + 1] - coordinatesAngstrom[secondOffset + 1];
    final double dz = coordinatesAngstrom[firstOffset + 2] - coordinatesAngstrom[secondOffset + 2];
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static final class EvaluationContext {

    private final int atomCount;
    private final int bondCount;
    private final int[] bondFirstIndices;
    private final int[] bondSecondIndices;
    private final double[] bondEquilibriumDistances;
    private final double[] bondOrderWeights;
    private final double[] covalentRadii;
    private final boolean[] bondedPairs;

    private EvaluationContext(
        final int atomCount,
        final int bondCount,
        final int[] bondFirstIndices,
        final int[] bondSecondIndices,
        final double[] bondEquilibriumDistances,
        final double[] bondOrderWeights,
        final double[] covalentRadii,
        final boolean[] bondedPairs) {
      this.atomCount = atomCount;
      this.bondCount = bondCount;
      this.bondFirstIndices = bondFirstIndices;
      this.bondSecondIndices = bondSecondIndices;
      this.bondEquilibriumDistances = bondEquilibriumDistances;
      this.bondOrderWeights = bondOrderWeights;
      this.covalentRadii = covalentRadii;
      this.bondedPairs = bondedPairs;
    }

    static EvaluationContext of(final Molecule molecule) {
      final List<Atom> atoms = molecule.atoms();
      final List<Bond> bonds = molecule.bonds();
      final Map<AtomId, Integer> atomIndexById = BondedForceFieldEnergyModel.atomIndexById(atoms);
      final int atomCount = atoms.size();
      final int bondCount = bonds.size();
      final int[] bondFirstIndices = new int[bondCount];
      final int[] bondSecondIndices = new int[bondCount];
      final double[] bondEquilibriumDistances = new double[bondCount];
      final double[] bondOrderWeights = new double[bondCount];
      final double[] covalentRadii = new double[atomCount];
      final boolean[] bondedPairs = new boolean[atomCount * atomCount];
      for (int i = 0; i < atomCount; ++i) {
        covalentRadii[i] = BondedForceFieldEnergyModel.covalentRadiusAngstrom(atoms.get(i));
      }
      for (int i = 0; i < bondCount; ++i) {
        final Bond bond = bonds.get(i);
        final int firstIndex = atomIndexById.get(bond.firstAtomId()).intValue();
        final int secondIndex = atomIndexById.get(bond.secondAtomId()).intValue();
        bondFirstIndices[i] = firstIndex;
        bondSecondIndices[i] = secondIndex;
        bondEquilibriumDistances[i] =
            BondedForceFieldEnergyModel.equilibriumDistanceAngstrom(
                atoms.get(firstIndex), atoms.get(secondIndex), bond.type());
        bondOrderWeights[i] = BondedForceFieldEnergyModel.bondOrderWeight(bond.type());
        bondedPairs[firstIndex * atomCount + secondIndex] = true;
        bondedPairs[secondIndex * atomCount + firstIndex] = true;
      }
      return new EvaluationContext(
          atomCount,
          bondCount,
          bondFirstIndices,
          bondSecondIndices,
          bondEquilibriumDistances,
          bondOrderWeights,
          covalentRadii,
          bondedPairs);
    }

    boolean directlyBonded(final int firstIndex, final int secondIndex) {
      return this.bondedPairs[firstIndex * this.atomCount + secondIndex];
    }
  }
}
