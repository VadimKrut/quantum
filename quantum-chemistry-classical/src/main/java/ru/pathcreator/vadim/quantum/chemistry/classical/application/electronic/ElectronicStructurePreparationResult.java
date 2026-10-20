/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.DirectGaussianEriStatistics;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf.RohfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

/** Снимок подготовки электронной задачи: basis, интегралы, optional SCF-reference и Hamiltonian. */
public final class ElectronicStructurePreparationResult {

  private final GaussianBasisSet basisSet;
  private final IntegralMolecule integralMolecule;
  private final String basisSetName;
  private final int basisOrbitalCount;
  private final MolecularIntegralTensors atomicOrbitalTensors;
  private final OneElectronIntegralMatrices oneElectronMatrices;
  private final int electronCount;
  private final RhfResult rhfResult;
  private final RohfResult rohfResult;
  private final UhfResult uhfResult;
  private final KohnShamDftResult kohnShamDftResult;
  private final CasscfResult casscfResult;
  private final MolecularIntegralTensors molecularOrbitalTensors;
  private final ElectronicHamiltonian hamiltonian;
  private final ElectronicStructurePreparationMode mode;
  private final String limitationReason;
  private final DirectGaussianEriStatistics directEriStatistics;

  private ElectronicStructurePreparationResult(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final String basisSetName,
      final int basisOrbitalCount,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RhfResult rhfResult,
      final UhfResult uhfResult,
      final KohnShamDftResult kohnShamDftResult,
      final CasscfResult casscfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final ElectronicStructurePreparationMode mode,
      final String limitationReason) {
    this(
        basisSet,
        integralMolecule,
        basisSetName,
        basisOrbitalCount,
        atomicOrbitalTensors,
        oneElectronMatrices,
        electronCount,
        rhfResult,
        null,
        uhfResult,
        kohnShamDftResult,
        casscfResult,
        molecularOrbitalTensors,
        hamiltonian,
        mode,
        limitationReason);
  }

  private ElectronicStructurePreparationResult(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final String basisSetName,
      final int basisOrbitalCount,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RhfResult rhfResult,
      final RohfResult rohfResult,
      final UhfResult uhfResult,
      final KohnShamDftResult kohnShamDftResult,
      final CasscfResult casscfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final ElectronicStructurePreparationMode mode,
      final String limitationReason) {
    this.basisSet = basisSet;
    this.integralMolecule = integralMolecule;
    this.basisSetName = ElectronicStructurePreparationResult.requireBasisSetName(basisSetName);
    this.basisOrbitalCount =
        ElectronicStructurePreparationResult.requireBasisOrbitalCount(basisOrbitalCount);
    this.atomicOrbitalTensors = atomicOrbitalTensors;
    this.oneElectronMatrices = oneElectronMatrices;
    this.electronCount = electronCount;
    this.rhfResult = rhfResult;
    this.rohfResult = rohfResult;
    this.uhfResult = uhfResult;
    this.kohnShamDftResult = kohnShamDftResult;
    this.casscfResult = casscfResult;
    this.molecularOrbitalTensors = molecularOrbitalTensors;
    this.hamiltonian = hamiltonian;
    this.mode = ElectronicStructurePreparationResult.requireMode(mode);
    this.limitationReason = limitationReason == null ? "" : limitationReason;
    this.directEriStatistics = null;
  }

  private ElectronicStructurePreparationResult(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final String basisSetName,
      final int basisOrbitalCount,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RhfResult rhfResult,
      final UhfResult uhfResult,
      final KohnShamDftResult kohnShamDftResult,
      final CasscfResult casscfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final ElectronicStructurePreparationMode mode,
      final String limitationReason,
      final DirectGaussianEriStatistics directEriStatistics) {
    this(
        basisSet,
        integralMolecule,
        basisSetName,
        basisOrbitalCount,
        atomicOrbitalTensors,
        oneElectronMatrices,
        electronCount,
        rhfResult,
        null,
        uhfResult,
        kohnShamDftResult,
        casscfResult,
        molecularOrbitalTensors,
        hamiltonian,
        mode,
        limitationReason,
        directEriStatistics);
  }

  private ElectronicStructurePreparationResult(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final String basisSetName,
      final int basisOrbitalCount,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RhfResult rhfResult,
      final RohfResult rohfResult,
      final UhfResult uhfResult,
      final KohnShamDftResult kohnShamDftResult,
      final CasscfResult casscfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final ElectronicStructurePreparationMode mode,
      final String limitationReason,
      final DirectGaussianEriStatistics directEriStatistics) {
    this.basisSet = basisSet;
    this.integralMolecule = integralMolecule;
    this.basisSetName = ElectronicStructurePreparationResult.requireBasisSetName(basisSetName);
    this.basisOrbitalCount =
        ElectronicStructurePreparationResult.requireBasisOrbitalCount(basisOrbitalCount);
    this.atomicOrbitalTensors = atomicOrbitalTensors;
    this.oneElectronMatrices = oneElectronMatrices;
    this.electronCount = electronCount;
    this.rhfResult = rhfResult;
    this.rohfResult = rohfResult;
    this.uhfResult = uhfResult;
    this.kohnShamDftResult = kohnShamDftResult;
    this.casscfResult = casscfResult;
    this.molecularOrbitalTensors = molecularOrbitalTensors;
    this.hamiltonian = hamiltonian;
    this.mode = ElectronicStructurePreparationResult.requireMode(mode);
    this.limitationReason = limitationReason == null ? "" : limitationReason;
    this.directEriStatistics = directEriStatistics;
  }

  public static ElectronicStructurePreparationResult of(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final int electronCount,
      final RhfResult rhfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Electronic preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral molecule must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electronic preparation electron count must be positive.");
    }
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException("Electronic preparation MO tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic preparation Hamiltonian must not be null.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        atomicOrbitalTensors,
        null,
        electronCount,
        rhfResult,
        null,
        null,
        null,
        molecularOrbitalTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        "");
  }

  public static ElectronicStructurePreparationResult ofKohnShamDft(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final int electronCount,
      final KohnShamDftResult kohnShamDftResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Electronic preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral molecule must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electronic preparation electron count must be positive.");
    }
    if (kohnShamDftResult == null) {
      throw new IllegalArgumentException(
          "Electronic preparation Kohn-Sham DFT reference must not be null.");
    }
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException(
          "Electronic preparation Kohn-Sham MO tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException(
          "Electronic preparation Kohn-Sham Hamiltonian must not be null.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        atomicOrbitalTensors,
        null,
        electronCount,
        null,
        null,
        kohnShamDftResult,
        null,
        molecularOrbitalTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        "Hamiltonian prepared with Kohn-Sham DFT orbitals.");
  }

  public static ElectronicStructurePreparationResult ofCasscfOptimized(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final int electronCount,
      final RhfResult rhfResult,
      final CasscfResult casscfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Electronic preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral molecule must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electronic preparation electron count must be positive.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException("Electronic preparation RHF reference must not be null.");
    }
    if (casscfResult == null) {
      throw new IllegalArgumentException("Electronic preparation CASSCF result must not be null.");
    }
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException(
          "Electronic preparation CASSCF MO tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException(
          "Electronic preparation CASSCF Hamiltonian must not be null.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        atomicOrbitalTensors,
        null,
        electronCount,
        rhfResult,
        null,
        null,
        casscfResult,
        molecularOrbitalTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        "Hamiltonian prepared with CASSCF orbital optimization.");
  }

  public static ElectronicStructurePreparationResult ofUnrestricted(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final int electronCount,
      final UhfResult uhfResult,
      final MolecularIntegralTensors hamiltonianTensors,
      final ElectronicHamiltonian hamiltonian) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Electronic preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral molecule must not be null.");
    }
    if (atomicOrbitalTensors == null) {
      throw new IllegalArgumentException("Electronic preparation AO tensors must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electronic preparation electron count must be positive.");
    }
    if (uhfResult == null) {
      throw new IllegalArgumentException("Electronic preparation UHF result must not be null.");
    }
    if (hamiltonianTensors == null) {
      throw new IllegalArgumentException(
          "Electronic preparation UHF Hamiltonian tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic preparation Hamiltonian must not be null.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        atomicOrbitalTensors,
        null,
        electronCount,
        null,
        uhfResult,
        null,
        null,
        hamiltonianTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        "");
  }

  public static ElectronicStructurePreparationResult ofRestrictedOpenShell(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final MolecularIntegralTensors atomicOrbitalTensors,
      final int electronCount,
      final RohfResult rohfResult,
      final MolecularIntegralTensors hamiltonianTensors,
      final ElectronicHamiltonian hamiltonian) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Electronic preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral molecule must not be null.");
    }
    if (atomicOrbitalTensors == null) {
      throw new IllegalArgumentException("Electronic preparation AO tensors must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electronic preparation electron count must be positive.");
    }
    if (rohfResult == null) {
      throw new IllegalArgumentException("Electronic preparation ROHF result must not be null.");
    }
    if (hamiltonianTensors == null) {
      throw new IllegalArgumentException(
          "Electronic preparation ROHF Hamiltonian tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic preparation Hamiltonian must not be null.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        atomicOrbitalTensors,
        null,
        electronCount,
        null,
        rohfResult,
        null,
        null,
        null,
        hamiltonianTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        "");
  }

  public static ElectronicStructurePreparationResult ofBackendHamiltonian(
      final String basisSetName,
      final int basisOrbitalCount,
      final int electronCount,
      final ElectronicHamiltonian hamiltonian,
      final String backendDescription) {
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Electronic preparation backend electron count must be positive.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException(
          "Electronic preparation backend Hamiltonian must not be null.");
    }
    if (backendDescription == null || backendDescription.isBlank()) {
      throw new IllegalArgumentException(
          "Electronic preparation backend description must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        null,
        null,
        basisSetName,
        basisOrbitalCount,
        null,
        null,
        electronCount,
        null,
        null,
        null,
        null,
        null,
        hamiltonian,
        ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN,
        backendDescription);
  }

  public static ElectronicStructurePreparationResult ofSemiEmpiricalValence(
      final String basisSetName,
      final int basisOrbitalCount,
      final int electronCount,
      final ElectronicHamiltonian hamiltonian,
      final String limitationReason) {
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Semiempirical preparation electron count must be positive.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Semiempirical preparation Hamiltonian must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Semiempirical preparation limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        null,
        null,
        basisSetName,
        basisOrbitalCount,
        null,
        null,
        electronCount,
        null,
        null,
        null,
        null,
        null,
        hamiltonian,
        ElectronicStructurePreparationMode.SEMI_EMPIRICAL_VALENCE,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofOneElectron(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final ElectronicHamiltonian hamiltonian) {
    return ElectronicStructurePreparationResult.ofOneElectron(
        basisSet,
        integralMolecule,
        oneElectronMatrices,
        electronCount,
        hamiltonian,
        "Full two-electron integral tensor is unavailable for the requested calculation.");
  }

  public static ElectronicStructurePreparationResult ofOneElectron(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final ElectronicHamiltonian hamiltonian,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Electronic preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Electronic preparation one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Electronic preparation electron count must be positive.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic preparation Hamiltonian must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "One-electron preparation limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        null,
        null,
        null,
        null,
        null,
        hamiltonian,
        ElectronicStructurePreparationMode.ONE_ELECTRON_FALLBACK,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofDirectRestrictedHartreeFock(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RhfResult rhfResult,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Direct RHF preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Direct RHF preparation integral molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Direct RHF preparation one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Direct RHF preparation electron count must be positive.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException("Direct RHF preparation result must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Direct RHF preparation limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        rhfResult,
        null,
        null,
        null,
        null,
        null,
        ElectronicStructurePreparationMode.DIRECT_HARTREE_FOCK,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofDirectRestrictedActiveSpaceHamiltonian(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RhfResult rhfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian electron count must be positive.");
    }
    if (rhfResult == null) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian RHF result must not be null.");
    }
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Direct active-space Hamiltonian must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Direct active-space Hamiltonian limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        rhfResult,
        null,
        null,
        null,
        molecularOrbitalTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofDirectUnrestrictedActiveSpaceHamiltonian(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final UhfResult uhfResult,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian electron count must be positive.");
    }
    if (uhfResult == null) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian UHF result must not be null.");
    }
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Direct UHF active-space Hamiltonian must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Direct UHF active-space Hamiltonian limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        null,
        uhfResult,
        null,
        null,
        molecularOrbitalTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofDirectCoreOrbitalActiveSpaceHamiltonian(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ElectronicHamiltonian hamiltonian,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian electron count must be positive.");
    }
    if (molecularOrbitalTensors == null) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian tensors must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Direct core-orbital active-space Hamiltonian limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        null,
        null,
        null,
        null,
        molecularOrbitalTensors,
        hamiltonian,
        ElectronicStructurePreparationMode.FULL_TWO_ELECTRON,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofDirectUnrestrictedHartreeFock(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final UhfResult uhfResult,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Direct UHF preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Direct UHF preparation integral molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Direct UHF preparation one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException("Direct UHF preparation electron count must be positive.");
    }
    if (uhfResult == null) {
      throw new IllegalArgumentException("Direct UHF preparation result must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Direct UHF preparation limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        null,
        uhfResult,
        null,
        null,
        null,
        null,
        ElectronicStructurePreparationMode.DIRECT_HARTREE_FOCK,
        limitationReason);
  }

  public static ElectronicStructurePreparationResult ofDirectRestrictedOpenShellHartreeFock(
      final GaussianBasisSet basisSet,
      final IntegralMolecule integralMolecule,
      final OneElectronIntegralMatrices oneElectronMatrices,
      final int electronCount,
      final RohfResult rohfResult,
      final String limitationReason) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Direct ROHF preparation basis set must not be null.");
    }
    if (integralMolecule == null) {
      throw new IllegalArgumentException(
          "Direct ROHF preparation integral molecule must not be null.");
    }
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException(
          "Direct ROHF preparation one-electron matrices must not be null.");
    }
    if (electronCount <= 0) {
      throw new IllegalArgumentException(
          "Direct ROHF preparation electron count must be positive.");
    }
    if (rohfResult == null) {
      throw new IllegalArgumentException("Direct ROHF preparation result must not be null.");
    }
    if (limitationReason == null || limitationReason.isBlank()) {
      throw new IllegalArgumentException(
          "Direct ROHF preparation limitation reason must not be blank.");
    }
    return new ElectronicStructurePreparationResult(
        basisSet,
        integralMolecule,
        basisSet.name(),
        basisSet.orbitalCount(),
        null,
        oneElectronMatrices,
        electronCount,
        null,
        rohfResult,
        null,
        null,
        null,
        null,
        null,
        ElectronicStructurePreparationMode.DIRECT_HARTREE_FOCK,
        limitationReason);
  }

  public GaussianBasisSet basisSet() {
    if (this.basisSet == null) {
      throw new IllegalStateException(
          "Electronic preparation does not contain Java-native Gaussian basis set.");
    }
    return this.basisSet;
  }

  public IntegralMolecule integralMolecule() {
    if (this.integralMolecule == null) {
      throw new IllegalStateException(
          "Electronic preparation does not contain Java-native integral molecule.");
    }
    return this.integralMolecule;
  }

  public boolean hasIntegralMolecule() {
    return this.integralMolecule != null;
  }

  public boolean hasJavaNativeBasisSet() {
    return this.basisSet != null;
  }

  public String basisSetName() {
    return this.basisSetName;
  }

  public int basisOrbitalCount() {
    return this.basisOrbitalCount;
  }

  public MolecularIntegralTensors atomicOrbitalTensors() {
    if (this.atomicOrbitalTensors == null) {
      throw new IllegalStateException("Electronic preparation does not contain full AO tensors.");
    }
    return this.atomicOrbitalTensors;
  }

  public boolean hasAtomicOrbitalTensors() {
    return this.atomicOrbitalTensors != null;
  }

  public OneElectronIntegralMatrices oneElectronMatrices() {
    if (this.oneElectronMatrices == null) {
      throw new IllegalStateException(
          "Electronic preparation does not contain one-electron matrices.");
    }
    return this.oneElectronMatrices;
  }

  public boolean hasOneElectronMatrices() {
    return this.oneElectronMatrices != null;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public RhfResult rhfResult() {
    if (this.rhfResult == null) {
      throw new IllegalStateException("Electronic preparation does not contain RHF reference.");
    }
    return this.rhfResult;
  }

  public boolean hasRhfReference() {
    return this.rhfResult != null;
  }

  public RohfResult rohfResult() {
    if (this.rohfResult == null) {
      throw new IllegalStateException("Electronic preparation does not contain ROHF reference.");
    }
    return this.rohfResult;
  }

  public boolean hasRohfReference() {
    return this.rohfResult != null;
  }

  public UhfResult uhfResult() {
    if (this.uhfResult == null) {
      throw new IllegalStateException("Electronic preparation does not contain UHF reference.");
    }
    return this.uhfResult;
  }

  public boolean hasUhfReference() {
    return this.uhfResult != null;
  }

  public KohnShamDftResult kohnShamDftResult() {
    if (this.kohnShamDftResult == null) {
      throw new IllegalStateException(
          "Electronic preparation does not contain Kohn-Sham DFT reference.");
    }
    return this.kohnShamDftResult;
  }

  public boolean hasKohnShamDftReference() {
    return this.kohnShamDftResult != null;
  }

  public CasscfResult casscfResult() {
    if (this.casscfResult == null) {
      throw new IllegalStateException(
          "Electronic preparation does not contain CASSCF orbital optimization result.");
    }
    return this.casscfResult;
  }

  public boolean hasCasscfResult() {
    return this.casscfResult != null;
  }

  public MolecularIntegralTensors molecularOrbitalTensors() {
    if (this.molecularOrbitalTensors == null) {
      throw new IllegalStateException("Electronic preparation does not contain MO tensors.");
    }
    return this.molecularOrbitalTensors;
  }

  public boolean hasMolecularOrbitalTensors() {
    return this.molecularOrbitalTensors != null;
  }

  public ElectronicHamiltonian hamiltonian() {
    return this.hamiltonian;
  }

  public boolean twoElectronIntegralsAvailable() {
    return this.mode == ElectronicStructurePreparationMode.FULL_TWO_ELECTRON
        || this.mode == ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN
        || this.mode == ElectronicStructurePreparationMode.SEMI_EMPIRICAL_VALENCE;
  }

  public ElectronicStructurePreparationMode mode() {
    return this.mode;
  }

  public String limitationReason() {
    return this.limitationReason;
  }

  public boolean hasLimitationReason() {
    return !this.limitationReason.isBlank();
  }

  public ElectronicStructurePreparationResult withDirectEriStatistics(
      final DirectGaussianEriStatistics directEriStatistics) {
    if (directEriStatistics == null) {
      throw new IllegalArgumentException("Direct ERI statistics must not be null.");
    }
    return new ElectronicStructurePreparationResult(
        this.basisSet,
        this.integralMolecule,
        this.basisSetName,
        this.basisOrbitalCount,
        this.atomicOrbitalTensors,
        this.oneElectronMatrices,
        this.electronCount,
        this.rhfResult,
        this.rohfResult,
        this.uhfResult,
        this.kohnShamDftResult,
        this.casscfResult,
        this.molecularOrbitalTensors,
        this.hamiltonian,
        this.mode,
        this.limitationReason,
        directEriStatistics);
  }

  public DirectGaussianEriStatistics directEriStatistics() {
    if (this.directEriStatistics == null) {
      throw new IllegalStateException(
          "Electronic preparation does not contain direct ERI statistics.");
    }
    return this.directEriStatistics;
  }

  public boolean hasDirectEriStatistics() {
    return this.directEriStatistics != null;
  }

  public boolean coreHamiltonianOrbitalReference() {
    return this.mode == ElectronicStructurePreparationMode.FULL_TWO_ELECTRON
        && this.rhfResult == null
        && this.rohfResult == null
        && this.uhfResult == null
        && this.kohnShamDftResult == null
        && this.casscfResult == null
        && this.molecularOrbitalTensors != null
        && this.limitationReason.contains("core-Hamiltonian orbitals");
  }

  private static ElectronicStructurePreparationMode requireMode(
      final ElectronicStructurePreparationMode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("Electronic preparation mode must not be null.");
    }
    return mode;
  }

  private static String requireBasisSetName(final String basisSetName) {
    if (basisSetName == null || basisSetName.isBlank()) {
      throw new IllegalArgumentException(
          "Electronic preparation basis set name must not be blank.");
    }
    return basisSetName;
  }

  private static int requireBasisOrbitalCount(final int basisOrbitalCount) {
    if (basisOrbitalCount <= 0) {
      throw new IllegalArgumentException(
          "Electronic preparation basis orbital count must be positive.");
    }
    return basisOrbitalCount;
  }
}
