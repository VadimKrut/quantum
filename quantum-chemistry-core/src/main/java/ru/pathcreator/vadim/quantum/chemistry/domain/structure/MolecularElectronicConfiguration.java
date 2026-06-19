/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

import java.util.List;
import java.util.Objects;

public final class MolecularElectronicConfiguration {

  private final int nuclearCharge;
  private final int electronCount;
  private final MolecularCharge molecularCharge;
  private final int formalChargeSum;
  private final int explicitUnpairedElectronCount;
  private final SpinMultiplicity spinMultiplicity;

  private MolecularElectronicConfiguration(
      final int nuclearCharge,
      final int electronCount,
      final MolecularCharge molecularCharge,
      final int formalChargeSum,
      final int explicitUnpairedElectronCount,
      final SpinMultiplicity spinMultiplicity) {
    this.nuclearCharge = nuclearCharge;
    this.electronCount = electronCount;
    this.molecularCharge = molecularCharge;
    this.formalChargeSum = formalChargeSum;
    this.explicitUnpairedElectronCount = explicitUnpairedElectronCount;
    this.spinMultiplicity = spinMultiplicity;
  }

  public static MolecularElectronicConfiguration of(
      final List<Atom> atoms, final MolecularCharge charge, final SpinMultiplicity spinMultiplicity) {
    if (atoms == null) {
      throw new IllegalArgumentException("Atoms must not be null.");
    }
    if (atoms.isEmpty()) {
      throw new IllegalArgumentException("Electronic configuration requires at least one atom.");
    }
    if (charge == null) {
      throw new IllegalArgumentException("Molecular charge must not be null.");
    }
    if (spinMultiplicity == null) {
      throw new IllegalArgumentException("Spin multiplicity must not be null.");
    }
    int checkedNuclearCharge = 0;
    int checkedFormalChargeSum = 0;
    int checkedExplicitUnpairedElectronCount = 0;
    for (int i = 0; i < atoms.size(); ++i) {
      Atom atom = atoms.get(i);
      if (atom == null) {
        throw new IllegalArgumentException("Atom must not be null.");
      }
      checkedNuclearCharge = Math.addExact(checkedNuclearCharge, atom.element().atomicNumber());
      checkedFormalChargeSum = Math.addExact(checkedFormalChargeSum, atom.formalCharge().value());
      checkedExplicitUnpairedElectronCount =
          Math.addExact(
              checkedExplicitUnpairedElectronCount, atom.radicalState().unpairedElectrons());
    }
    final int checkedElectronCount = Math.subtractExact(checkedNuclearCharge, charge.value());
    if (checkedElectronCount < 0) {
      throw new IllegalArgumentException(
          "Molecular charge removes more electrons than atoms provide.");
    }
    return new MolecularElectronicConfiguration(
        checkedNuclearCharge,
        checkedElectronCount,
        charge,
        checkedFormalChargeSum,
        checkedExplicitUnpairedElectronCount,
        spinMultiplicity);
  }

  public int nuclearCharge() {
    return this.nuclearCharge;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public MolecularCharge molecularCharge() {
    return this.molecularCharge;
  }

  public int formalChargeSum() {
    return this.formalChargeSum;
  }

  public int explicitUnpairedElectronCount() {
    return this.explicitUnpairedElectronCount;
  }

  public SpinMultiplicity spinMultiplicity() {
    return this.spinMultiplicity;
  }

  public int minimumUnpairedElectronCountForSpin() {
    return this.spinMultiplicity.value() - 1;
  }

  public boolean closedShell() {
    return this.spinMultiplicity.value() == 1
        && this.explicitUnpairedElectronCount == 0
        && this.electronCount % 2 == 0;
  }

  public boolean openShell() {
    return !this.closedShell();
  }

  public boolean spinMultiplicityPossible() {
    if (this.electronCount == 0) {
      return this.spinMultiplicity.value() == 1;
    }
    if (this.spinMultiplicity.value() > this.electronCount + 1) {
      return false;
    }
    final int unpairedElectronParity = this.spinMultiplicity.value() - 1;
    return this.electronCount % 2 == unpairedElectronParity % 2;
  }

  public boolean formalChargesMatchMolecularCharge() {
    return this.formalChargeSum == this.molecularCharge.value();
  }

  public boolean explicitRadicalsCompatibleWithSpin() {
    if (this.explicitUnpairedElectronCount == 0) {
      return true;
    }
    final int spinUnpaired = this.minimumUnpairedElectronCountForSpin();
    return this.explicitUnpairedElectronCount >= spinUnpaired
        && this.explicitUnpairedElectronCount % 2 == spinUnpaired % 2;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularElectronicConfiguration)) {
      return false;
    }
    final MolecularElectronicConfiguration configuration = (MolecularElectronicConfiguration) other;
    return this.nuclearCharge == configuration.nuclearCharge
        && this.electronCount == configuration.electronCount
        && this.formalChargeSum == configuration.formalChargeSum
        && this.explicitUnpairedElectronCount == configuration.explicitUnpairedElectronCount
        && Objects.equals(this.molecularCharge, configuration.molecularCharge)
        && Objects.equals(this.spinMultiplicity, configuration.spinMultiplicity);
  }

  public int hashCode() {
    return Objects.hash(
        this.nuclearCharge,
        this.electronCount,
        this.molecularCharge,
        this.formalChargeSum,
        this.explicitUnpairedElectronCount,
        this.spinMultiplicity);
  }
}