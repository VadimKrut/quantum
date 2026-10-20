/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application.basis;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShellAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianShellType;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/** Переводит структурное chemistry-core basis definition в расчетный Gaussian basis. */
public final class StructuredGaussianBasisAdapter {

  public GaussianBasisSet toGaussianBasisSet(
      final String basisName,
      final BasisSetDefinition definition,
      final IntegralMolecule molecule) {
    if (basisName == null || basisName.isBlank()) {
      throw new IllegalArgumentException("Structured Gaussian basis name must not be blank.");
    }
    if (definition == null) {
      throw new IllegalArgumentException("Structured Gaussian basis definition must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("Structured Gaussian basis molecule must not be null.");
    }
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(
            Math.max(1, molecule.atomCount() * definition.cartesianFunctionCount()));
    final List<IntegralAtom> atoms = molecule.atoms();
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      final ElementSymbol symbol =
          PeriodicTable.requireAtomicNumber(atoms.get(atomIndex).atomicNumber()).symbol();
      final BasisAtomDefinition atomDefinition = definition.atomDefinition(symbol);
      if (atomDefinition == null) {
        throw new IllegalArgumentException(
            "Structured basis definition does not contain atom shell for element "
                + symbol.value()
                + ".");
      }
      this.addAtomShells(atomIndex, atomDefinition, orbitals);
    }
    return GaussianBasisSet.of(basisName, orbitals);
  }

  private void addAtomShells(
      final int atomIndex,
      final BasisAtomDefinition atomDefinition,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    final List<BasisShell> shells = atomDefinition.shells();
    for (int i = 0; i < shells.size(); ++i) {
      this.addShell(atomIndex, shells.get(i), orbitals);
    }
  }

  private void addShell(
      final int atomIndex,
      final BasisShell shell,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    final double[] exponents = new double[shell.primitiveCount()];
    final double[] coefficients = new double[shell.primitiveCount()];
    final List<BasisPrimitive> primitives = shell.primitives();
    for (int i = 0; i < primitives.size(); ++i) {
      exponents[i] = primitives.get(i).exponent();
      coefficients[i] = primitives.get(i).coefficient();
    }
    final GaussianShellType shellType =
        StructuredGaussianBasisAdapter.shellType(shell.angularMomentum());
    final List<GaussianAngularMomentum> momenta = GaussianAngularMomentum.cartesianShell(shellType);
    for (int i = 0; i < momenta.size(); ++i) {
      final GaussianAngularMomentum momentum = momenta.get(i);
      orbitals.add(
          ContractedGaussianOrbital.of(
              atomIndex,
              momentum,
              GaussianBasisLibrary.normalizedPrimitives(exponents, coefficients, momentum)));
    }
  }

  private static GaussianShellType shellType(final BasisShellAngularMomentum angularMomentum) {
    return GaussianShellType.ofAngularMomentum(angularMomentum.angularMomentum());
  }
}
