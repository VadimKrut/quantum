/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class SemiEmpiricalValenceHamiltonianBuilder {

  private static final double DEFAULT_ONSITE_HARTREE = -0.020;
  private static final double DEFAULT_HOPPING_HARTREE = -0.035;
  private static final double DEFAULT_REPULSION_HARTREE = 0.055;

  public ElectronicStructurePreparationResult prepare(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Semiempirical Hamiltonian molecule must not be null.");
    }
    final List<Atom> orbitalAtoms = SemiEmpiricalValenceHamiltonianBuilder.orbitalAtoms(molecule);
    final HashMap<AtomId, Integer> orbitalByAtomId =
        SemiEmpiricalValenceHamiltonianBuilder.orbitalByAtomId(orbitalAtoms);
    final HashMap<AtomId, Atom> atomById =
        SemiEmpiricalValenceHamiltonianBuilder.atomById(molecule.atoms());
    final int orbitalCount = orbitalAtoms.size();
    final int electronCount =
        SemiEmpiricalValenceHamiltonianBuilder.activeElectronCount(
            molecule, orbitalByAtomId, orbitalCount);
    final ArrayList<OneElectronIntegral> oneElectronIntegrals =
        new ArrayList<OneElectronIntegral>(orbitalCount + molecule.bondCount());
    final ArrayList<TwoElectronIntegral> twoElectronIntegrals =
        new ArrayList<TwoElectronIntegral>(orbitalCount);
    for (int i = 0; i < orbitalAtoms.size(); ++i) {
      final ValenceHamiltonianParameters parameters =
          SemiEmpiricalValenceHamiltonianBuilder.parameters(
              orbitalAtoms.get(i).element().symbol().value());
      oneElectronIntegrals.add(OneElectronIntegral.of(i, i, parameters.onsiteHartree()));
      twoElectronIntegrals.add(TwoElectronIntegral.of(i, i, i, i, parameters.repulsionHartree()));
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Integer firstOrbital = orbitalByAtomId.get(bond.firstAtomId());
      final Integer secondOrbital = orbitalByAtomId.get(bond.secondAtomId());
      if (firstOrbital == null || secondOrbital == null || firstOrbital.equals(secondOrbital)) {
        continue;
      }
      final double hopping =
          SemiEmpiricalValenceHamiltonianBuilder.hoppingHartree(
              bond.type(), atomById.get(bond.firstAtomId()), atomById.get(bond.secondAtomId()));
      oneElectronIntegrals.add(
          OneElectronIntegral.of(firstOrbital.intValue(), secondOrbital.intValue(), hopping));
    }
    final ActiveSpace activeSpace = ActiveSpace.of(electronCount, orbitalCount);
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            activeSpace,
            SemiEmpiricalValenceHamiltonianBuilder.nuclearGraphOffset(molecule),
            oneElectronIntegrals,
            twoElectronIntegrals);
    return ElectronicStructurePreparationResult.ofSemiEmpiricalValence(
        "native-valence-semiempirical",
        orbitalCount,
        electronCount,
        hamiltonian,
        "Parameterized valence Hamiltonian generated from molecular graph; no Gaussian basis was required.");
  }

  private static List<Atom> orbitalAtoms(final Molecule molecule) {
    final ArrayList<Atom> atoms = new ArrayList<Atom>(molecule.atomCount());
    final List<Atom> sourceAtoms = molecule.atoms();
    for (int i = 0; i < sourceAtoms.size(); ++i) {
      if (sourceAtoms.get(i).element().atomicNumber() != 1) {
        atoms.add(sourceAtoms.get(i));
      }
    }
    if (!atoms.isEmpty()) {
      return List.copyOf(atoms);
    }
    return List.copyOf(sourceAtoms);
  }

  private static HashMap<AtomId, Integer> orbitalByAtomId(final List<Atom> atoms) {
    final HashMap<AtomId, Integer> result = new HashMap<AtomId, Integer>(atoms.size() * 2);
    for (int i = 0; i < atoms.size(); ++i) {
      result.put(atoms.get(i).id(), Integer.valueOf(i));
    }
    return result;
  }

  private static HashMap<AtomId, Atom> atomById(final List<Atom> atoms) {
    final HashMap<AtomId, Atom> result = new HashMap<AtomId, Atom>(atoms.size() * 2);
    for (int i = 0; i < atoms.size(); ++i) {
      result.put(atoms.get(i).id(), atoms.get(i));
    }
    return result;
  }

  private static int activeElectronCount(
      final Molecule molecule,
      final HashMap<AtomId, Integer> orbitalByAtomId,
      final int orbitalCount) {
    int electronCount = molecule.electronicConfiguration().explicitUnpairedElectronCount();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!orbitalByAtomId.containsKey(bond.firstAtomId())
          || !orbitalByAtomId.containsKey(bond.secondAtomId())) {
        continue;
      }
      if (bond.type() == BondType.AROMATIC) {
        electronCount += 1;
      } else if (bond.type() == BondType.DOUBLE) {
        electronCount += 2;
      } else if (bond.type() == BondType.TRIPLE) {
        electronCount += 4;
      }
    }
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!orbitalByAtomId.containsKey(atom.id())) {
        continue;
      }
      if (SemiEmpiricalValenceHamiltonianBuilder.heteroAtomContributesLonePair(atom)) {
        electronCount += 2;
      }
    }
    if (electronCount <= 0) {
      electronCount = Math.min(2, orbitalCount * 2);
    }
    return Math.max(1, Math.min(electronCount, orbitalCount * 2));
  }

  private static boolean heteroAtomContributesLonePair(final Atom atom) {
    final int atomicNumber = atom.element().atomicNumber();
    return atomicNumber == 7
        || atomicNumber == 8
        || atomicNumber == 15
        || atomicNumber == 16
        || atomicNumber == 34;
  }

  private static double hoppingHartree(final BondType type, final Atom first, final Atom second) {
    final double order = type.hasDefinedOrder() ? type.orderValue() : 1.0;
    final double firstBeta =
        SemiEmpiricalValenceHamiltonianBuilder.parameters(first.element().symbol().value())
            .hoppingHartree();
    final double secondBeta =
        SemiEmpiricalValenceHamiltonianBuilder.parameters(second.element().symbol().value())
            .hoppingHartree();
    return 0.5 * (firstBeta + secondBeta) * Math.sqrt(order);
  }

  private static double nuclearGraphOffset(final Molecule molecule) {
    return 0.001 * molecule.atomCount() + 0.002 * molecule.bondCount();
  }

  private static ValenceHamiltonianParameters parameters(final String symbol) {
    final String normalized = symbol.toUpperCase(Locale.ROOT);
    if (normalized.equals("C")) {
      return ValenceHamiltonianParameters.of(-0.020, -0.035, 0.055);
    }
    if (normalized.equals("N")) {
      return ValenceHamiltonianParameters.of(-0.032, -0.040, 0.070);
    }
    if (normalized.equals("O")) {
      return ValenceHamiltonianParameters.of(-0.045, -0.045, 0.080);
    }
    if (normalized.equals("S") || normalized.equals("SE")) {
      return ValenceHamiltonianParameters.of(-0.038, -0.033, 0.074);
    }
    if (normalized.equals("P")) {
      return ValenceHamiltonianParameters.of(-0.035, -0.030, 0.070);
    }
    if (normalized.equals("F")
        || normalized.equals("CL")
        || normalized.equals("BR")
        || normalized.equals("I")) {
      return ValenceHamiltonianParameters.of(-0.052, -0.028, 0.090);
    }
    return ValenceHamiltonianParameters.of(
        DEFAULT_ONSITE_HARTREE, DEFAULT_HOPPING_HARTREE, DEFAULT_REPULSION_HARTREE);
  }

  private static final class ValenceHamiltonianParameters {
    private final double onsiteHartree;
    private final double hoppingHartree;
    private final double repulsionHartree;

    private ValenceHamiltonianParameters(
        final double onsiteHartree, final double hoppingHartree, final double repulsionHartree) {
      this.onsiteHartree = onsiteHartree;
      this.hoppingHartree = hoppingHartree;
      this.repulsionHartree = repulsionHartree;
    }

    static ValenceHamiltonianParameters of(
        final double onsiteHartree, final double hoppingHartree, final double repulsionHartree) {
      return new ValenceHamiltonianParameters(onsiteHartree, hoppingHartree, repulsionHartree);
    }

    double onsiteHartree() {
      return this.onsiteHartree;
    }

    double hoppingHartree() {
      return this.hoppingHartree;
    }

    double repulsionHartree() {
      return this.repulsionHartree;
    }
  }
}
