/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.CompleteActiveSpaceSelfConsistentFieldExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularElectronicConfiguration;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.ActiveSpaceIntegralProjector;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.DirectMolecularOrbitalWindowTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.ElectronicHamiltonianIntegralBuilder;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralCapability;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralPreflight;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralCapabilityStatus;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianOneElectronIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.IntegralMoleculeFactory;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.MolecularOrbitalIntegralTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.basis.GaussianBasisTextParser;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.basis.StructuredGaussianBasisAdapter;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.DirectGaussianElectronRepulsionIntegralProvider;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.JacobiSymmetricEigenSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.KohnShamDftSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.RestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.RestrictedOpenShellHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.UnrestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf.RohfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

/** Единый Java-native pipeline подготовки электронной структуры для CPU-расчетов и Quantum IR. */
public final class ElectronicStructurePreparationPipeline {

  public static final String STO_3G_NAME = "STO-3G";
  public static final String SIX_31G_NAME = "6-31G";
  private static final String ACTIVE_SPACE_FIRST_ORBITAL_OPTION = "active_space_first_orbital";
  private static final String MAX_DIRECT_MO_PAIR_PRODUCTS_OPTION = "max_direct_mo_pair_products";
  private static final long DEFAULT_MAX_DIRECT_MO_PAIR_PRODUCTS = 5_000_000L;
  private static final String HAMILTONIAN_ORBITAL_OPTIMIZATION_OPTION =
      "hamiltonian_orbital_optimization";
  private static final String HAMILTONIAN_BACKEND_OPTION = "hamiltonian_backend";
  private static final String SCF_INTEGRAL_MODE_OPTION = "scf_integral_mode";
  private static final String DIRECT_ERI_SCREENING_TOLERANCE_OPTION =
      "direct_eri_screening_tolerance";
  private static final String SCF_REFERENCE_MODE_OPTION = "scf_reference_mode";
  private static final String RHF_MAX_ITERATIONS_OPTION = "rhf_max_iterations";
  private static final String RHF_CONVERGENCE_TOLERANCE_OPTION = "rhf_convergence_tolerance";
  private static final String RHF_DENSITY_TOLERANCE_OPTION = "rhf_density_tolerance";
  private static final String RHF_DENSITY_MIXING_OPTION = "rhf_density_mixing";
  private static final String UHF_HAMILTONIAN_ORBITAL_CHANNEL_OPTION =
      "uhf_hamiltonian_orbital_channel";
  private static final String UHF_ALPHA_CHANNEL = "alpha";
  private static final String UHF_BETA_CHANNEL = "beta";
  private static final String UHF_AVERAGE_CHANNEL = "average";
  private static final String DIRECT_INTEGRAL_MODE = "direct";
  private static final String CORE_HAMILTONIAN_REFERENCE_MODE = "core_hamiltonian";
  private static final String PYSCF_BACKEND = "pyscf";
  private static final int DIRECT_ERI_CACHE_SIZE = 262_144;
  private static final int PREPARATION_CACHE_LIMIT = 4;

  private final GaussianIntegralEngine integralEngine;
  private final GaussianOneElectronIntegralEngine oneElectronIntegralEngine;
  private final RestrictedHartreeFockSolver hartreeFockSolver;
  private final RestrictedOpenShellHartreeFockSolver restrictedOpenShellHartreeFockSolver;
  private final UnrestrictedHartreeFockSolver unrestrictedHartreeFockSolver;
  private final KohnShamDftSolver kohnShamDftSolver;
  private final MolecularOrbitalIntegralTransformer orbitalIntegralTransformer;
  private final DirectMolecularOrbitalWindowTransformer directOrbitalWindowTransformer =
      new DirectMolecularOrbitalWindowTransformer();
  private final ActiveSpaceIntegralProjector activeSpaceProjector;
  private final ElectronicHamiltonianIntegralBuilder hamiltonianBuilder;
  private final CompleteActiveSpaceSelfConsistentFieldExecutor casscfExecutor;
  private final GaussianBasisTextParser basisTextParser;
  private final StructuredGaussianBasisAdapter structuredBasisAdapter;
  private final SemiEmpiricalValenceHamiltonianBuilder semiEmpiricalHamiltonianBuilder =
      new SemiEmpiricalValenceHamiltonianBuilder();
  private final PyScfElectronicStructurePreparationExecutor pyScfPreparationExecutor =
      new PyScfElectronicStructurePreparationExecutor();
  private final JacobiSymmetricEigenSolver eigenSolver = new JacobiSymmetricEigenSolver();
  private final LinkedHashMap<ClassicalCalculationRequest, ElectronicStructurePreparationResult>
      preparationCache =
          new LinkedHashMap<ClassicalCalculationRequest, ElectronicStructurePreparationResult>(
              PREPARATION_CACHE_LIMIT, 0.75F, true) {
            protected boolean removeEldestEntry(
                final Map.Entry<ClassicalCalculationRequest, ElectronicStructurePreparationResult>
                    eldest) {
              return this.size() > PREPARATION_CACHE_LIMIT;
            }
          };

  public ElectronicStructurePreparationPipeline() {
    this(
        new GaussianIntegralEngine(),
        new GaussianOneElectronIntegralEngine(),
        new RestrictedHartreeFockSolver(),
        new RestrictedOpenShellHartreeFockSolver(),
        new UnrestrictedHartreeFockSolver(),
        new KohnShamDftSolver(),
        new MolecularOrbitalIntegralTransformer(),
        new ActiveSpaceIntegralProjector(),
        new ElectronicHamiltonianIntegralBuilder(),
        new CompleteActiveSpaceSelfConsistentFieldExecutor(),
        new GaussianBasisTextParser(),
        new StructuredGaussianBasisAdapter());
  }

  public ElectronicStructurePreparationPipeline(
      final GaussianIntegralEngine integralEngine,
      final GaussianOneElectronIntegralEngine oneElectronIntegralEngine,
      final RestrictedHartreeFockSolver hartreeFockSolver,
      final RestrictedOpenShellHartreeFockSolver restrictedOpenShellHartreeFockSolver,
      final UnrestrictedHartreeFockSolver unrestrictedHartreeFockSolver,
      final KohnShamDftSolver kohnShamDftSolver,
      final MolecularOrbitalIntegralTransformer orbitalIntegralTransformer,
      final ActiveSpaceIntegralProjector activeSpaceProjector,
      final ElectronicHamiltonianIntegralBuilder hamiltonianBuilder,
      final CompleteActiveSpaceSelfConsistentFieldExecutor casscfExecutor,
      final GaussianBasisTextParser basisTextParser,
      final StructuredGaussianBasisAdapter structuredBasisAdapter) {
    if (integralEngine == null) {
      throw new IllegalArgumentException(
          "Electronic preparation integral engine must not be null.");
    }
    if (oneElectronIntegralEngine == null) {
      throw new IllegalArgumentException(
          "Electronic preparation one-electron engine must not be null.");
    }
    if (hartreeFockSolver == null) {
      throw new IllegalArgumentException("Electronic preparation RHF solver must not be null.");
    }
    if (restrictedOpenShellHartreeFockSolver == null) {
      throw new IllegalArgumentException("Electronic preparation ROHF solver must not be null.");
    }
    if (unrestrictedHartreeFockSolver == null) {
      throw new IllegalArgumentException("Electronic preparation UHF solver must not be null.");
    }
    if (kohnShamDftSolver == null) {
      throw new IllegalArgumentException(
          "Electronic preparation Kohn-Sham DFT solver must not be null.");
    }
    if (orbitalIntegralTransformer == null) {
      throw new IllegalArgumentException("Electronic preparation MO transformer must not be null.");
    }
    if (activeSpaceProjector == null) {
      throw new IllegalArgumentException(
          "Electronic preparation active-space projector must not be null.");
    }
    if (hamiltonianBuilder == null) {
      throw new IllegalArgumentException(
          "Electronic preparation Hamiltonian builder must not be null.");
    }
    if (casscfExecutor == null) {
      throw new IllegalArgumentException(
          "Electronic preparation CASSCF executor must not be null.");
    }
    if (basisTextParser == null) {
      throw new IllegalArgumentException(
          "Electronic preparation basis text parser must not be null.");
    }
    if (structuredBasisAdapter == null) {
      throw new IllegalArgumentException(
          "Electronic preparation structured basis adapter must not be null.");
    }
    this.integralEngine = integralEngine;
    this.oneElectronIntegralEngine = oneElectronIntegralEngine;
    this.hartreeFockSolver = hartreeFockSolver;
    this.restrictedOpenShellHartreeFockSolver = restrictedOpenShellHartreeFockSolver;
    this.unrestrictedHartreeFockSolver = unrestrictedHartreeFockSolver;
    this.kohnShamDftSolver = kohnShamDftSolver;
    this.orbitalIntegralTransformer = orbitalIntegralTransformer;
    this.activeSpaceProjector = activeSpaceProjector;
    this.hamiltonianBuilder = hamiltonianBuilder;
    this.casscfExecutor = casscfExecutor;
    this.basisTextParser = basisTextParser;
    this.structuredBasisAdapter = structuredBasisAdapter;
  }

  public String unsupportedReason(final ClassicalCalculationRequest request) {
    final ElectronicStructurePreparationPreflight preflight = this.preflight(request);
    if (!preflight.supported()) {
      return preflight.unsupportedReason();
    }
    return null;
  }

  public ElectronicStructurePreparationPreflight preflight(
      final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Electronic preparation request must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return ElectronicStructurePreparationPreflight.of(
          "Electronic preparation requires a molecule subject.", "", null, null);
    }
    if (!this.supportsSpinTreatment(request.method().spinTreatment())) {
      return ElectronicStructurePreparationPreflight.of(
          "Electronic preparation supports restricted closed-shell, restricted open-shell, unrestricted, or unspecified spin treatment.",
          "",
          null,
          null);
    }
    final Molecule molecule = (Molecule) request.subject();
    if (request.method().type() == ElectronicStructureMethodType.SEMI_EMPIRICAL) {
      final int orbitalCount =
          Math.max(
              1,
              molecule.atomCount()
                  - ElectronicStructurePreparationPipeline.hydrogenCount(molecule));
      return ElectronicStructurePreparationPreflight.of(
          "",
          "",
          GaussianIntegralPreflight.of(
              GaussianIntegralCapabilityStatus.SUPPORTED,
              "native-valence-semiempirical",
              orbitalCount,
              1,
              0,
              ElectronicStructurePreparationPipeline.power4(orbitalCount),
              ElectronicStructurePreparationPipeline.packedPairCount(orbitalCount),
              ""),
          GaussianIntegralPreflight.of(
              GaussianIntegralCapabilityStatus.SUPPORTED,
              "native-valence-semiempirical",
              orbitalCount,
              1,
              0,
              ElectronicStructurePreparationPipeline.power4(orbitalCount),
              ElectronicStructurePreparationPipeline.packedPairCount(orbitalCount),
              ""));
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    if (this.pyScfBackendRequested(request)) {
      return this.pyScfBackendPreflight(request, molecule, integralMolecule);
    }
    if (request.basisSet().hasStructuredDefinition()) {
      return this.structuredBasisSummaryPreflight(request, molecule, integralMolecule);
    }
    final GaussianBasisSet basisSet;
    try {
      basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    } catch (final IllegalArgumentException exception) {
      return ElectronicStructurePreparationPreflight.of(exception.getMessage(), "", null, null);
    }
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    final String activeSpaceReason =
        this.unsupportedActiveSpaceReason(
            request,
            molecule.electronicConfiguration(),
            basisSet.orbitalCount(),
            integralMolecule.electronCount());
    final String directActiveSpaceDescription =
        this.directActiveSpacePreflightDescription(
            request,
            fullTensorPreflight.supported()
                ? "Direct active-space Hamiltonian requested."
                : fullTensorPreflight.reason());
    if (directActiveSpaceDescription != null) {
      return ElectronicStructurePreparationPreflight.ofDirectActiveSpace(
          "",
          activeSpaceReason,
          fullTensorPreflight,
          oneElectronPreflight,
          directActiveSpaceDescription);
    }
    return ElectronicStructurePreparationPreflight.of(
        "", activeSpaceReason, fullTensorPreflight, oneElectronPreflight);
  }

  private ElectronicStructurePreparationPreflight structuredBasisSummaryPreflight(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final IntegralMolecule integralMolecule) {
    final BasisSet basisSet = request.basisSet();
    final BasisSetDefinition definition = basisSet.definition();
    int orbitalCount = 0;
    int maxAngularMomentum = 0;
    for (int atomIndex = 0; atomIndex < molecule.atoms().size(); ++atomIndex) {
      final BasisAtomDefinition atomDefinition =
          definition.atomDefinition(molecule.atoms().get(atomIndex).element().symbol());
      if (atomDefinition == null) {
        return ElectronicStructurePreparationPreflight.of(
            "Structured basis definition does not contain atom shell for element "
                + molecule.atoms().get(atomIndex).element().symbol().value()
                + ".",
            "",
            null,
            null);
      }
      final List<BasisShell> shells = atomDefinition.shells();
      for (int shellIndex = 0; shellIndex < shells.size(); ++shellIndex) {
        final BasisShell shell = shells.get(shellIndex);
        orbitalCount = Math.addExact(orbitalCount, shell.cartesianFunctionCount());
        maxAngularMomentum =
            Math.max(maxAngularMomentum, shell.angularMomentum().angularMomentum());
      }
    }
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(
            basisSet.name().value(), orbitalCount, maxAngularMomentum);
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(
            basisSet.name().value(), orbitalCount, maxAngularMomentum);
    final String activeSpaceReason =
        this.unsupportedActiveSpaceReason(
            request,
            molecule.electronicConfiguration(),
            orbitalCount,
            integralMolecule.electronCount());
    final String directActiveSpaceDescription =
        this.directActiveSpacePreflightDescription(
            request,
            fullTensorPreflight.supported()
                ? "Direct active-space Hamiltonian requested."
                : fullTensorPreflight.reason());
    if (directActiveSpaceDescription != null) {
      return ElectronicStructurePreparationPreflight.ofDirectActiveSpace(
          "",
          activeSpaceReason,
          fullTensorPreflight,
          oneElectronPreflight,
          directActiveSpaceDescription);
    }
    return ElectronicStructurePreparationPreflight.of(
        "", activeSpaceReason, fullTensorPreflight, oneElectronPreflight);
  }

  public String restrictedClosedShellUnsupportedReason(final ClassicalCalculationRequest request) {
    final String unsupportedReason = this.unsupportedReason(request);
    if (unsupportedReason != null) {
      return unsupportedReason;
    }
    final Molecule molecule = (Molecule) request.subject();
    final MolecularElectronicConfiguration configuration = molecule.electronicConfiguration();
    if (!configuration.closedShell()) {
      return "Restricted closed-shell electronic calculation requires even paired electrons and singlet spin.";
    }
    return null;
  }

  public String oneElectronUnsupportedReason(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Electronic preparation request must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return "One-electron electronic preparation requires a molecule subject.";
    }
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet;
    try {
      basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    } catch (final IllegalArgumentException exception) {
      return exception.getMessage();
    }
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    if (!oneElectronPreflight.supported()) {
      return oneElectronPreflight.reason();
    }
    if (request.hasActiveSpace()
        && request.activeSpace().orbitalCount() != basisSet.orbitalCount()) {
      return "One-electron electronic preparation requires full active orbital space.";
    }
    return null;
  }

  public boolean canPrepareDirectRestrictedActiveSpaceHamiltonian(
      final ClassicalCalculationRequest request) {
    return this.directRestrictedActiveSpaceHamiltonianAvailable(
        request,
        this.directIntegralModeRequested(request)
            ? "Direct active-space Hamiltonian requested by scf_integral_mode=direct."
            : this.unsupportedReason(request));
  }

  public boolean canPrepareDirectActiveSpaceHamiltonian(final ClassicalCalculationRequest request) {
    final String reason =
        this.directIntegralModeRequested(request)
            ? "Direct active-space Hamiltonian requested by scf_integral_mode=direct."
            : this.fullTensorLimitationReason(request);
    if (reason == null) {
      return false;
    }
    return this.directCoreOrbitalActiveSpaceHamiltonianAvailable(request, reason)
        || this.directUnrestrictedActiveSpaceHamiltonianAvailable(request, reason)
        || this.directRestrictedActiveSpaceHamiltonianAvailable(request, reason);
  }

  private String directActiveSpacePreflightDescription(
      final ClassicalCalculationRequest request, final String reason) {
    if (reason == null) {
      return null;
    }
    try {
      if (this.directCoreOrbitalActiveSpaceHamiltonianAvailable(request, reason)) {
        return "Exact direct active-space Hamiltonian is available through core-Hamiltonian orbitals; "
            + reason;
      }
      if (this.directUnrestrictedActiveSpaceHamiltonianAvailable(request, reason)) {
        return "Exact direct active-space Hamiltonian is available through UHF orbitals; " + reason;
      }
      if (this.directRestrictedActiveSpaceHamiltonianAvailable(request, reason)) {
        return "Exact direct active-space Hamiltonian is available through RHF orbitals; " + reason;
      }
    } catch (final IllegalArgumentException exception) {
      return null;
    }
    return null;
  }

  private String fullTensorLimitationReason(final ClassicalCalculationRequest request) {
    if (request == null || request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return null;
    }
    final IntegralMolecule integralMolecule =
        IntegralMoleculeFactory.fromMolecule((Molecule) request.subject());
    final GaussianBasisSet basisSet;
    try {
      basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    } catch (final IllegalArgumentException exception) {
      return exception.getMessage();
    }
    final GaussianIntegralPreflight fullTensorPreflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);
    if (fullTensorPreflight.supported()) {
      return null;
    }
    return fullTensorPreflight.reason();
  }

  public ElectronicStructurePreparationResult prepare(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Electronic preparation request must not be null.");
    }
    synchronized (this.preparationCache) {
      final ElectronicStructurePreparationResult cached = this.preparationCache.get(request);
      if (cached != null) {
        return cached;
      }
    }
    final ElectronicStructurePreparationResult prepared = this.prepareUncached(request);
    synchronized (this.preparationCache) {
      this.preparationCache.put(request, prepared);
    }
    return prepared;
  }

  public int cachedPreparationCount() {
    synchronized (this.preparationCache) {
      return this.preparationCache.size();
    }
  }

  public void clearPreparationCache() {
    synchronized (this.preparationCache) {
      this.preparationCache.clear();
    }
  }

  private ElectronicStructurePreparationResult prepareUncached(
      final ClassicalCalculationRequest request) {
    if (request.method().type() == ElectronicStructureMethodType.SEMI_EMPIRICAL) {
      if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
        throw new IllegalArgumentException(
            "Semiempirical electronic preparation requires a molecule subject.");
      }
      return this.semiEmpiricalHamiltonianBuilder.prepare((Molecule) request.subject());
    }
    if (this.pyScfBackendRequested(request)) {
      return this.pyScfPreparationExecutor.prepare(request);
    }
    if (this.directIntegralModeRequested(request)) {
      final String directReason =
          "Direct active-space Hamiltonian requested by scf_integral_mode=direct.";
      if (this.directHartreeFockAvailable(request, directReason)) {
        return this.prepareDirectHartreeFock(request);
      }
      if (this.coreHamiltonianReferenceModeRequested(request)) {
        final String directCoreReason =
            this.unsupportedDirectCoreOrbitalActiveSpaceHamiltonianReason(request, directReason);
        if (directCoreReason == null) {
          return this.prepareDirectCoreOrbitalActiveSpaceHamiltonian(request, directReason);
        }
        throw new IllegalArgumentException(directCoreReason);
      }
      if (this.directUnrestrictedActiveSpaceHamiltonianAvailable(request, directReason)) {
        return this.prepareDirectUnrestrictedActiveSpaceHamiltonian(request, directReason);
      }
      if (this.directRestrictedActiveSpaceHamiltonianAvailable(request, directReason)) {
        return this.prepareDirectRestrictedActiveSpaceHamiltonian(request, directReason);
      }
    }
    if (!this.casscfOrbitalOptimizationRequested(request)) {
      final String activeSpaceDirectReason =
          "Active-space Hamiltonian requested; direct windowed ERI transform avoids full four-index tensor materialization.";
      if (this.directUnrestrictedActiveSpaceHamiltonianAvailable(
          request, activeSpaceDirectReason)) {
        return this.prepareDirectUnrestrictedActiveSpaceHamiltonian(
            request, activeSpaceDirectReason);
      }
      if (this.directRestrictedActiveSpaceHamiltonianAvailable(request, activeSpaceDirectReason)) {
        return this.prepareDirectRestrictedActiveSpaceHamiltonian(request, activeSpaceDirectReason);
      }
    }
    final String unsupportedReason = this.unsupportedReason(request);
    if (unsupportedReason != null) {
      if (this.coreHamiltonianReferenceModeRequested(request)
          && this.directCoreOrbitalActiveSpaceHamiltonianAvailable(request, unsupportedReason)) {
        return this.prepareDirectCoreOrbitalActiveSpaceHamiltonian(request, unsupportedReason);
      }
      if (this.directUnrestrictedActiveSpaceHamiltonianAvailable(request, unsupportedReason)) {
        return this.prepareDirectUnrestrictedActiveSpaceHamiltonian(request, unsupportedReason);
      }
      if (this.directRestrictedActiveSpaceHamiltonianAvailable(request, unsupportedReason)) {
        return this.prepareDirectRestrictedActiveSpaceHamiltonian(request, unsupportedReason);
      }
      if (this.directHartreeFockAvailable(request, unsupportedReason)) {
        return this.prepareDirectHartreeFock(request);
      }
      throw new IllegalArgumentException(unsupportedReason);
    }
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final MolecularIntegralTensors atomicOrbitalTensors =
        this.integralEngine.evaluate(integralMolecule, basisSet);
    final ActiveSpace activeSpace =
        request.hasActiveSpace()
            ? request.activeSpace()
            : ActiveSpace.of(integralMolecule.electronCount(), basisSet.orbitalCount());
    final int fullElectronCount = integralMolecule.electronCount();
    final int firstActiveOrbital = this.firstActiveOrbital(request, activeSpace, fullElectronCount);
    if (!molecule.electronicConfiguration().closedShell()) {
      if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
        final DensityFunctionalDefinition functionalDefinition =
            request.method().hasDensityFunctionalDefinition()
                ? request.method().densityFunctionalDefinition()
                : DensityFunctionalDefinition.resolve(request.method().name().value());
        final KohnShamDftResult dftResult =
            this.kohnShamDftSolver.solveUnrestricted(
                integralMolecule,
                basisSet,
                atomicOrbitalTensors,
                fullElectronCount,
                molecule.electronicConfiguration().spinMultiplicity().value(),
                functionalDefinition,
                KohnShamDftOptions.DEFAULT);
        final double[] dftHamiltonianCoefficients =
            this.unrestrictedDftHamiltonianCoefficients(
                request, dftResult, basisSet.orbitalCount());
        final MolecularIntegralTensors dftMolecularOrbitalTensors =
            this.transformForRequestedActiveSpace(
                atomicOrbitalTensors, dftHamiltonianCoefficients, activeSpace, firstActiveOrbital);
        final MolecularIntegralTensors dftHamiltonianTensors =
            this.projectIfNeeded(dftMolecularOrbitalTensors, activeSpace, firstActiveOrbital);
        final ElectronicHamiltonian dftHamiltonian =
            this.hamiltonianBuilder.build(activeSpace.electronCount(), dftHamiltonianTensors);
        return ElectronicStructurePreparationResult.ofKohnShamDft(
            basisSet,
            integralMolecule,
            atomicOrbitalTensors,
            fullElectronCount,
            dftResult,
            dftMolecularOrbitalTensors,
            dftHamiltonian);
      }
      if (request.method().spinTreatment()
          == ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
        final RohfResult rohfResult =
            this.restrictedOpenShellHartreeFockSolver.solve(
                atomicOrbitalTensors,
                fullElectronCount,
                molecule.electronicConfiguration().spinMultiplicity().value(),
                this.rhfOptions(request));
        final MolecularIntegralTensors molecularOrbitalTensors =
            this.transformForRequestedActiveSpace(
                atomicOrbitalTensors,
                rohfResult.molecularOrbitalCoefficientsCopy(),
                activeSpace,
                firstActiveOrbital);
        final MolecularIntegralTensors hamiltonianTensors =
            this.projectIfNeeded(molecularOrbitalTensors, activeSpace, firstActiveOrbital);
        final ElectronicHamiltonian hamiltonian =
            this.hamiltonianBuilder.build(activeSpace.electronCount(), hamiltonianTensors);
        return ElectronicStructurePreparationResult.ofRestrictedOpenShell(
            basisSet,
            integralMolecule,
            atomicOrbitalTensors,
            activeSpace.electronCount(),
            rohfResult,
            hamiltonianTensors,
            hamiltonian);
      }
      final UhfResult uhfResult =
          this.unrestrictedHartreeFockSolver.solve(
              atomicOrbitalTensors,
              fullElectronCount,
              molecule.electronicConfiguration().spinMultiplicity().value());
      final double[] uhfHamiltonianCoefficients =
          this.uhfHamiltonianCoefficients(request, uhfResult, basisSet.orbitalCount());
      final MolecularIntegralTensors molecularOrbitalTensors =
          this.transformForRequestedActiveSpace(
              atomicOrbitalTensors, uhfHamiltonianCoefficients, activeSpace, firstActiveOrbital);
      final MolecularIntegralTensors hamiltonianTensors =
          this.projectIfNeeded(molecularOrbitalTensors, activeSpace, firstActiveOrbital);
      final ElectronicHamiltonian hamiltonian =
          this.hamiltonianBuilder.build(activeSpace.electronCount(), hamiltonianTensors);
      return ElectronicStructurePreparationResult.ofUnrestricted(
          basisSet,
          integralMolecule,
          atomicOrbitalTensors,
          activeSpace.electronCount(),
          uhfResult,
          hamiltonianTensors,
          hamiltonian);
    }
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      final DensityFunctionalDefinition functionalDefinition =
          request.method().hasDensityFunctionalDefinition()
              ? request.method().densityFunctionalDefinition()
              : DensityFunctionalDefinition.resolve(request.method().name().value());
      final KohnShamDftResult dftResult =
          this.kohnShamDftSolver.solve(
              integralMolecule,
              basisSet,
              atomicOrbitalTensors,
              fullElectronCount,
              functionalDefinition,
              KohnShamDftOptions.DEFAULT);
      final MolecularIntegralTensors dftMolecularOrbitalTensors =
          this.transformForRequestedActiveSpace(
              atomicOrbitalTensors,
              dftResult.molecularOrbitalCoefficientsCopy(),
              activeSpace,
              firstActiveOrbital);
      final MolecularIntegralTensors dftHamiltonianTensors =
          this.projectIfNeeded(dftMolecularOrbitalTensors, activeSpace, firstActiveOrbital);
      final ElectronicHamiltonian dftHamiltonian =
          this.hamiltonianBuilder.build(activeSpace.electronCount(), dftHamiltonianTensors);
      return ElectronicStructurePreparationResult.ofKohnShamDft(
          basisSet,
          integralMolecule,
          atomicOrbitalTensors,
          fullElectronCount,
          dftResult,
          dftMolecularOrbitalTensors,
          dftHamiltonian);
    }
    final RhfResult rhfResult =
        this.hartreeFockSolver.solve(atomicOrbitalTensors, fullElectronCount);
    final MolecularIntegralTensors molecularOrbitalTensors =
        this.transformForRequestedActiveSpace(
            atomicOrbitalTensors,
            rhfResult.molecularOrbitalCoefficientsCopy(),
            activeSpace,
            firstActiveOrbital);
    final MolecularIntegralTensors hamiltonianTensors =
        this.projectIfNeeded(molecularOrbitalTensors, activeSpace, firstActiveOrbital);
    final ElectronicHamiltonian hamiltonian =
        this.hamiltonianBuilder.build(activeSpace.electronCount(), hamiltonianTensors);
    final ElectronicStructurePreparationResult baselineResult =
        ElectronicStructurePreparationResult.of(
            basisSet,
            integralMolecule,
            atomicOrbitalTensors,
            fullElectronCount,
            rhfResult,
            molecularOrbitalTensors,
            hamiltonian);
    if (!this.casscfOrbitalOptimizationRequested(request)) {
      return baselineResult;
    }
    final CasscfResult casscfResult = this.casscfExecutor.solve(request, baselineResult);
    final MolecularIntegralTensors casscfMolecularOrbitalTensors =
        this.transformForRequestedActiveSpace(
            atomicOrbitalTensors,
            casscfResult.optimizedOrbitalCoefficientsCopy(),
            activeSpace,
            firstActiveOrbital);
    final MolecularIntegralTensors casscfHamiltonianTensors =
        this.projectIfNeeded(casscfMolecularOrbitalTensors, activeSpace, firstActiveOrbital);
    final ElectronicHamiltonian casscfHamiltonian =
        this.hamiltonianBuilder.build(activeSpace.electronCount(), casscfHamiltonianTensors);
    return ElectronicStructurePreparationResult.ofCasscfOptimized(
        basisSet,
        integralMolecule,
        atomicOrbitalTensors,
        fullElectronCount,
        rhfResult,
        casscfResult,
        casscfMolecularOrbitalTensors,
        casscfHamiltonian);
  }

  public ElectronicStructurePreparationResult prepareOneElectron(
      final ClassicalCalculationRequest request) {
    final String unsupportedReason = this.oneElectronUnsupportedReason(request);
    if (unsupportedReason != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final ActiveSpace activeSpace =
        request.hasActiveSpace()
            ? request.activeSpace()
            : ActiveSpace.of(integralMolecule.electronCount(), basisSet.orbitalCount());
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final ElectronicHamiltonian hamiltonian =
        this.hamiltonianBuilder.buildOneElectronOnly(activeSpace.electronCount(), matrices);
    final String fullTensorReason = this.unsupportedReason(request);
    return ElectronicStructurePreparationResult.ofOneElectron(
        basisSet,
        integralMolecule,
        matrices,
        activeSpace.electronCount(),
        hamiltonian,
        fullTensorReason == null
            ? "Full two-electron preparation was not requested for this one-electron result."
            : fullTensorReason);
  }

  public ElectronicStructurePreparationResult prepareDirectRestrictedHartreeFock(
      final ClassicalCalculationRequest request) {
    final String oneElectronReason = this.oneElectronUnsupportedReason(request);
    if (oneElectronReason != null) {
      throw new IllegalArgumentException(oneElectronReason);
    }
    final Molecule molecule = (Molecule) request.subject();
    final MolecularElectronicConfiguration configuration = molecule.electronicConfiguration();
    if (!configuration.closedShell()) {
      throw new IllegalArgumentException("Direct RHF requires closed-shell molecule.");
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final DirectGaussianElectronRepulsionIntegralProvider electronRepulsionProvider =
        this.directElectronRepulsionProvider(request, integralMolecule, basisSet);
    final RhfResult rhfResult =
        this.hartreeFockSolver.solve(
            matrices,
            electronRepulsionProvider,
            integralMolecule.electronCount(),
            this.rhfOptions(request));
    final String fullTensorReason = this.unsupportedReason(request);
    return ElectronicStructurePreparationResult.ofDirectRestrictedHartreeFock(
            basisSet,
            integralMolecule,
            matrices,
            integralMolecule.electronCount(),
            rhfResult,
            fullTensorReason == null
                ? "Direct RHF skipped full ERI tensor materialization by request."
                : fullTensorReason)
        .withDirectEriStatistics(electronRepulsionProvider.statistics());
  }

  public boolean canPrepareDirectHartreeFock(final ClassicalCalculationRequest request) {
    final String reason =
        this.directIntegralModeRequested(request)
            ? "Direct Hartree-Fock requested by scf_integral_mode=direct."
            : this.fullTensorLimitationReason(request);
    return reason != null && this.directHartreeFockAvailable(request, reason);
  }

  public ElectronicStructurePreparationResult prepareDirectHartreeFock(
      final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Direct Hartree-Fock request must not be null.");
    }
    if (request.method().type() != ElectronicStructureMethodType.HARTREE_FOCK) {
      throw new IllegalArgumentException(
          "Direct Hartree-Fock preparation requires Hartree-Fock method.");
    }
    if (request.method().spinTreatment()
        == ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
      return this.prepareDirectRestrictedOpenShellHartreeFock(request);
    }
    if (request.method().spinTreatment() == ElectronicStructureSpinTreatment.UNRESTRICTED) {
      return this.prepareDirectUnrestrictedHartreeFock(request);
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      throw new IllegalArgumentException("Direct Hartree-Fock requires a molecule subject.");
    }
    final Molecule molecule = (Molecule) request.subject();
    if (molecule.electronicConfiguration().openShell()) {
      return this.prepareDirectUnrestrictedHartreeFock(request);
    }
    return this.prepareDirectRestrictedHartreeFock(request);
  }

  public ElectronicStructurePreparationResult prepareDirectUnrestrictedHartreeFock(
      final ClassicalCalculationRequest request) {
    final String oneElectronReason = this.oneElectronUnsupportedReason(request);
    if (oneElectronReason != null) {
      throw new IllegalArgumentException(oneElectronReason);
    }
    final Molecule molecule = (Molecule) request.subject();
    final MolecularElectronicConfiguration configuration = molecule.electronicConfiguration();
    if (!configuration.openShell()
        && request.method().spinTreatment() != ElectronicStructureSpinTreatment.UNRESTRICTED) {
      throw new IllegalArgumentException(
          "Direct UHF requires open-shell molecule or unrestricted spin treatment.");
    }
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      throw new IllegalArgumentException("Direct UHF preparation does not support Kohn-Sham DFT.");
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final DirectGaussianElectronRepulsionIntegralProvider electronRepulsionProvider =
        this.directElectronRepulsionProvider(request, integralMolecule, basisSet);
    final UhfResult uhfResult =
        this.unrestrictedHartreeFockSolver.solve(
            matrices,
            electronRepulsionProvider,
            integralMolecule.electronCount(),
            configuration.spinMultiplicity().value(),
            this.rhfOptions(request));
    final String fullTensorReason = this.unsupportedReason(request);
    return ElectronicStructurePreparationResult.ofDirectUnrestrictedHartreeFock(
            basisSet,
            integralMolecule,
            matrices,
            integralMolecule.electronCount(),
            uhfResult,
            fullTensorReason == null
                ? "Direct UHF skipped full ERI tensor materialization by request."
                : fullTensorReason)
        .withDirectEriStatistics(electronRepulsionProvider.statistics());
  }

  private boolean directHartreeFockAvailable(
      final ClassicalCalculationRequest request, final String reason) {
    if (reason == null
        || request == null
        || request.method().type() != ElectronicStructureMethodType.HARTREE_FOCK
        || request.hasActiveSpace()
        || request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return false;
    }
    final String oneElectronReason;
    try {
      oneElectronReason = this.oneElectronUnsupportedReason(request);
    } catch (final IllegalArgumentException exception) {
      return false;
    }
    if (oneElectronReason != null) {
      return false;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (request.method().spinTreatment()
        == ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL) {
      return molecule.electronicConfiguration().closedShell();
    }
    if (request.method().spinTreatment()
        == ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
      return molecule.electronicConfiguration().openShell();
    }
    if (request.method().spinTreatment() == ElectronicStructureSpinTreatment.UNRESTRICTED) {
      return true;
    }
    return request.method().spinTreatment() == ElectronicStructureSpinTreatment.UNSPECIFIED;
  }

  public ElectronicStructurePreparationResult prepareDirectRestrictedOpenShellHartreeFock(
      final ClassicalCalculationRequest request) {
    final String oneElectronReason = this.oneElectronUnsupportedReason(request);
    if (oneElectronReason != null) {
      throw new IllegalArgumentException(oneElectronReason);
    }
    final Molecule molecule = (Molecule) request.subject();
    final MolecularElectronicConfiguration configuration = molecule.electronicConfiguration();
    if (!configuration.openShell()
        || request.method().spinTreatment()
            != ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
      throw new IllegalArgumentException(
          "Direct ROHF requires open-shell molecule and restricted open-shell spin treatment.");
    }
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      throw new IllegalArgumentException("Direct ROHF preparation does not support Kohn-Sham DFT.");
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final DirectGaussianElectronRepulsionIntegralProvider electronRepulsionProvider =
        this.directElectronRepulsionProvider(request, integralMolecule, basisSet);
    final RohfResult rohfResult =
        this.restrictedOpenShellHartreeFockSolver.solve(
            matrices,
            electronRepulsionProvider,
            integralMolecule.electronCount(),
            configuration.spinMultiplicity().value(),
            this.rhfOptions(request));
    final String fullTensorReason = this.unsupportedReason(request);
    return ElectronicStructurePreparationResult.ofDirectRestrictedOpenShellHartreeFock(
            basisSet,
            integralMolecule,
            matrices,
            integralMolecule.electronCount(),
            rohfResult,
            fullTensorReason == null
                ? "Direct ROHF skipped full ERI tensor materialization by request."
                : fullTensorReason)
        .withDirectEriStatistics(electronRepulsionProvider.statistics());
  }

  private boolean directRestrictedActiveSpaceHamiltonianAvailable(
      final ClassicalCalculationRequest request, final String unsupportedReason) {
    if (unsupportedReason == null) {
      throw new IllegalArgumentException("Direct restricted active-space reason must not be null.");
    }
    if (!request.hasActiveSpace()) {
      return false;
    }
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        || request.method().type()
            == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return false;
    }
    if (!request
            .method()
            .spinTreatment()
            .equals(ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL)
        && !request.method().spinTreatment().equals(ElectronicStructureSpinTreatment.UNSPECIFIED)) {
      return false;
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return false;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (!molecule.electronicConfiguration().closedShell()) {
      return false;
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet;
    try {
      basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    } catch (final IllegalArgumentException exception) {
      return false;
    }
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    if (!oneElectronPreflight.supported()) {
      return false;
    }
    final ActiveSpace activeSpace = request.activeSpace();
    if (activeSpace.orbitalCount() >= basisSet.orbitalCount()) {
      return false;
    }
    final int firstActiveOrbital =
        this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount());
    if (Math.addExact(firstActiveOrbital, activeSpace.orbitalCount()) > basisSet.orbitalCount()) {
      return false;
    }
    return this.directActiveSpaceTransformCostReason(
            request, basisSet, activeSpace, "Direct RHF active-space Hamiltonian")
        == null;
  }

  private boolean directUnrestrictedActiveSpaceHamiltonianAvailable(
      final ClassicalCalculationRequest request, final String unsupportedReason) {
    if (unsupportedReason == null) {
      throw new IllegalArgumentException(
          "Direct unrestricted active-space reason must not be null.");
    }
    if (!request.hasActiveSpace()) {
      return false;
    }
    if (request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        || request.method().type()
            == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return false;
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return false;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (!molecule.electronicConfiguration().openShell()
        && request.method().spinTreatment() != ElectronicStructureSpinTreatment.UNRESTRICTED) {
      return false;
    }
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet;
    try {
      basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    } catch (final IllegalArgumentException exception) {
      return false;
    }
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    if (!oneElectronPreflight.supported()) {
      return false;
    }
    final ActiveSpace activeSpace = request.activeSpace();
    if (activeSpace.orbitalCount() >= basisSet.orbitalCount()) {
      return false;
    }
    final int firstActiveOrbital =
        this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount());
    if (Math.addExact(firstActiveOrbital, activeSpace.orbitalCount()) > basisSet.orbitalCount()) {
      return false;
    }
    return this.directActiveSpaceTransformCostReason(
            request, basisSet, activeSpace, "Direct UHF active-space Hamiltonian")
        == null;
  }

  private boolean directCoreOrbitalActiveSpaceHamiltonianAvailable(
      final ClassicalCalculationRequest request, final String reason) {
    return this.unsupportedDirectCoreOrbitalActiveSpaceHamiltonianReason(request, reason) == null;
  }

  private String unsupportedDirectCoreOrbitalActiveSpaceHamiltonianReason(
      final ClassicalCalculationRequest request, final String reason) {
    if (reason == null || !request.hasActiveSpace()) {
      return "Direct core-orbital active-space Hamiltonian requires an explicit active space.";
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return "Direct core-orbital active-space Hamiltonian requires a molecule subject.";
    }
    final IntegralMolecule integralMolecule =
        IntegralMoleculeFactory.fromMolecule((Molecule) request.subject());
    final GaussianBasisSet basisSet;
    try {
      basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    } catch (final IllegalArgumentException exception) {
      return exception.getMessage();
    }
    final GaussianIntegralPreflight oneElectronPreflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    if (!oneElectronPreflight.supported()) {
      return oneElectronPreflight.reason();
    }
    final ActiveSpace activeSpace = request.activeSpace();
    if (activeSpace.orbitalCount() >= basisSet.orbitalCount()) {
      return "Direct core-orbital active-space Hamiltonian is intended for reduced active spaces.";
    }
    final int firstActiveOrbital =
        this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount());
    if (Math.addExact(firstActiveOrbital, activeSpace.orbitalCount()) > basisSet.orbitalCount()) {
      return "Direct core-orbital active-space window exceeds resolved basis orbital count.";
    }
    return this.directActiveSpaceTransformCostReason(
        request, basisSet, activeSpace, "Direct core-orbital active-space Hamiltonian");
  }

  private ElectronicStructurePreparationResult prepareDirectRestrictedActiveSpaceHamiltonian(
      final ClassicalCalculationRequest request, final String limitationReason) {
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final ActiveSpace activeSpace = request.activeSpace();
    final int firstActiveOrbital =
        this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount());
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final DirectGaussianElectronRepulsionIntegralProvider electronRepulsionProvider =
        this.directElectronRepulsionProvider(request, integralMolecule, basisSet);
    final RhfResult rhfResult =
        this.hartreeFockSolver.solve(
            matrices,
            electronRepulsionProvider,
            integralMolecule.electronCount(),
            this.rhfOptions(request));
    final MolecularIntegralTensors molecularOrbitalWindow =
        this.directOrbitalWindowTransformer.transformOrbitalWindow(
            matrices,
            rhfResult.molecularOrbitalCoefficientsCopy(),
            firstActiveOrbital,
            activeSpace.orbitalCount(),
            electronRepulsionProvider);
    final ElectronicHamiltonian hamiltonian =
        this.hamiltonianBuilder.build(activeSpace.electronCount(), molecularOrbitalWindow);
    return ElectronicStructurePreparationResult.ofDirectRestrictedActiveSpaceHamiltonian(
            basisSet,
            integralMolecule,
            matrices,
            integralMolecule.electronCount(),
            rhfResult,
            molecularOrbitalWindow,
            hamiltonian,
            "Direct active-space Hamiltonian skipped full AO ERI tensor: " + limitationReason)
        .withDirectEriStatistics(electronRepulsionProvider.statistics());
  }

  private ElectronicStructurePreparationResult prepareDirectUnrestrictedActiveSpaceHamiltonian(
      final ClassicalCalculationRequest request, final String limitationReason) {
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final ActiveSpace activeSpace = request.activeSpace();
    final int firstActiveOrbital =
        this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount());
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final DirectGaussianElectronRepulsionIntegralProvider electronRepulsionProvider =
        this.directElectronRepulsionProvider(request, integralMolecule, basisSet);
    final UhfResult uhfResult =
        this.unrestrictedHartreeFockSolver.solve(
            matrices,
            electronRepulsionProvider,
            integralMolecule.electronCount(),
            molecule.electronicConfiguration().spinMultiplicity().value(),
            this.rhfOptions(request));
    final double[] coefficients =
        this.uhfHamiltonianCoefficients(request, uhfResult, basisSet.orbitalCount());
    final MolecularIntegralTensors molecularOrbitalWindow =
        this.directOrbitalWindowTransformer.transformOrbitalWindow(
            matrices,
            coefficients,
            firstActiveOrbital,
            activeSpace.orbitalCount(),
            electronRepulsionProvider);
    final ElectronicHamiltonian hamiltonian =
        this.hamiltonianBuilder.build(activeSpace.electronCount(), molecularOrbitalWindow);
    return ElectronicStructurePreparationResult.ofDirectUnrestrictedActiveSpaceHamiltonian(
            basisSet,
            integralMolecule,
            matrices,
            activeSpace.electronCount(),
            uhfResult,
            molecularOrbitalWindow,
            hamiltonian,
            "Direct UHF active-space Hamiltonian skipped full AO ERI tensor: " + limitationReason)
        .withDirectEriStatistics(electronRepulsionProvider.statistics());
  }

  private ElectronicStructurePreparationResult prepareDirectCoreOrbitalActiveSpaceHamiltonian(
      final ClassicalCalculationRequest request, final String limitationReason) {
    final Molecule molecule = (Molecule) request.subject();
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final GaussianBasisSet basisSet = this.resolveBasisSet(request.basisSet(), integralMolecule);
    final ActiveSpace activeSpace = request.activeSpace();
    final int firstActiveOrbital =
        this.firstActiveOrbital(request, activeSpace, integralMolecule.electronCount());
    final OneElectronIntegralMatrices matrices =
        this.oneElectronIntegralEngine.evaluate(integralMolecule, basisSet);
    final DirectGaussianElectronRepulsionIntegralProvider electronRepulsionProvider =
        this.directElectronRepulsionProvider(request, integralMolecule, basisSet);
    final double[] coreOrbitalCoefficients = this.coreHamiltonianOrbitalCoefficients(matrices);
    final MolecularIntegralTensors molecularOrbitalWindow =
        this.directOrbitalWindowTransformer.transformOrbitalWindow(
            matrices,
            coreOrbitalCoefficients,
            firstActiveOrbital,
            activeSpace.orbitalCount(),
            electronRepulsionProvider);
    final ElectronicHamiltonian hamiltonian =
        this.hamiltonianBuilder.build(activeSpace.electronCount(), molecularOrbitalWindow);
    return ElectronicStructurePreparationResult.ofDirectCoreOrbitalActiveSpaceHamiltonian(
            basisSet,
            integralMolecule,
            matrices,
            activeSpace.electronCount(),
            molecularOrbitalWindow,
            hamiltonian,
            "Direct active-space Hamiltonian used core-Hamiltonian orbitals without SCF reference: "
                + limitationReason)
        .withDirectEriStatistics(electronRepulsionProvider.statistics());
  }

  private String directActiveSpaceTransformCostReason(
      final ClassicalCalculationRequest request,
      final GaussianBasisSet basisSet,
      final ActiveSpace activeSpace,
      final String routeName) {
    final long pairCount = (long) basisSet.orbitalCount() * (basisSet.orbitalCount() + 1L) / 2L;
    final long targetPairCount =
        (long) activeSpace.orbitalCount() * (activeSpace.orbitalCount() + 1L) / 2L;
    final long estimatedPairProducts =
        ElectronicStructurePreparationPipeline.multiplySaturated(
            targetPairCount,
            ElectronicStructurePreparationPipeline.multiplySaturated(pairCount, pairCount));
    final long allowedPairProducts =
        this.longOption(
            request, MAX_DIRECT_MO_PAIR_PRODUCTS_OPTION, DEFAULT_MAX_DIRECT_MO_PAIR_PRODUCTS);
    if (estimatedPairProducts <= allowedPairProducts) {
      return null;
    }
    return routeName
        + " estimates "
        + estimatedPairProducts
        + " AO-pair products, above configured limit "
        + allowedPairProducts
        + ". Increase "
        + MAX_DIRECT_MO_PAIR_PRODUCTS_OPTION
        + " only when this exact direct transform cost is intentional.";
  }

  private static long multiplySaturated(final long first, final long second) {
    try {
      return Math.multiplyExact(first, second);
    } catch (final ArithmeticException exception) {
      return Long.MAX_VALUE;
    }
  }

  private String unsupportedActiveSpaceReason(
      final ClassicalCalculationRequest request,
      final MolecularElectronicConfiguration configuration,
      final int fullOrbitalCount,
      final int fullElectronCount) {
    if (!request.hasActiveSpace()) {
      return null;
    }
    final ActiveSpace activeSpace = request.activeSpace();
    if (activeSpace.orbitalCount() > fullOrbitalCount) {
      return "Active space cannot contain more orbitals than the resolved basis.";
    }
    if (activeSpace.electronCount() > fullElectronCount) {
      return "Active space cannot contain more electrons than the molecule.";
    }
    if (activeSpace.orbitalCount() == fullOrbitalCount) {
      return null;
    }
    final int frozenElectronCount = fullElectronCount - activeSpace.electronCount();
    if (frozenElectronCount < 0 || frozenElectronCount % 2 != 0) {
      return "Reduced active-space projection requires an even non-negative frozen electron count.";
    }
    try {
      final int firstActiveOrbital =
          this.firstActiveOrbital(request, activeSpace, fullElectronCount);
      if (Math.addExact(firstActiveOrbital, activeSpace.orbitalCount()) > fullOrbitalCount) {
        return "Active-space orbital window exceeds resolved basis orbital count.";
      }
    } catch (final IllegalArgumentException exception) {
      return exception.getMessage();
    }
    return null;
  }

  private ElectronicStructurePreparationPreflight pyScfBackendPreflight(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final IntegralMolecule integralMolecule) {
    final String activeSpaceReason =
        this.unsupportedBackendActiveSpaceReason(request, integralMolecule.electronCount());
    return ElectronicStructurePreparationPreflight.ofBackendHamiltonian(
        "",
        activeSpaceReason,
        "PySCF active-space Hamiltonian backend for basis " + request.basisSet().name().value());
  }

  private String unsupportedBackendActiveSpaceReason(
      final ClassicalCalculationRequest request, final int fullElectronCount) {
    if (!request.hasActiveSpace()) {
      return null;
    }
    final ActiveSpace activeSpace = request.activeSpace();
    if (activeSpace.electronCount() > fullElectronCount) {
      return "Active space cannot contain more electrons than the molecule.";
    }
    final int frozenElectronCount = fullElectronCount - activeSpace.electronCount();
    if (frozenElectronCount < 0 || frozenElectronCount % 2 != 0) {
      return "Reduced active-space projection requires an even non-negative frozen electron count.";
    }
    try {
      this.firstActiveOrbital(request, activeSpace, fullElectronCount);
    } catch (final IllegalArgumentException exception) {
      return exception.getMessage();
    }
    return null;
  }

  private MolecularIntegralTensors projectIfNeeded(
      final MolecularIntegralTensors molecularOrbitalTensors,
      final ActiveSpace activeSpace,
      final int firstActiveOrbital) {
    if (activeSpace.orbitalCount() == molecularOrbitalTensors.orbitalCount()) {
      return molecularOrbitalTensors;
    }
    return this.activeSpaceProjector.project(
        molecularOrbitalTensors, activeSpace, firstActiveOrbital);
  }

  private MolecularIntegralTensors transformForRequestedActiveSpace(
      final MolecularIntegralTensors atomicOrbitalTensors,
      final double[] molecularOrbitalCoefficients,
      final ActiveSpace activeSpace,
      final int firstActiveOrbital) {
    if (activeSpace.orbitalCount() == atomicOrbitalTensors.orbitalCount()) {
      return this.orbitalIntegralTransformer.transform(
          atomicOrbitalTensors, molecularOrbitalCoefficients);
    }
    return this.orbitalIntegralTransformer.transformOrbitalWindow(
        atomicOrbitalTensors,
        molecularOrbitalCoefficients,
        firstActiveOrbital,
        activeSpace.orbitalCount());
  }

  private int firstActiveOrbital(
      final ClassicalCalculationRequest request,
      final ActiveSpace activeSpace,
      final int fullElectronCount) {
    final Integer explicitFirstOrbital =
        this.integerOption(
            request.options(),
            ElectronicStructurePreparationPipeline.ACTIVE_SPACE_FIRST_ORBITAL_OPTION);
    final int inferredFirstOrbital =
        Math.floorDiv(fullElectronCount - activeSpace.electronCount(), 2);
    final int firstActiveOrbital =
        explicitFirstOrbital == null ? inferredFirstOrbital : explicitFirstOrbital.intValue();
    if (firstActiveOrbital < 0) {
      throw new IllegalArgumentException("Active-space first orbital must not be negative.");
    }
    final int exclusiveLastActiveOrbital =
        Math.addExact(firstActiveOrbital, activeSpace.orbitalCount());
    if (exclusiveLastActiveOrbital < firstActiveOrbital) {
      throw new IllegalArgumentException("Active-space orbital window overflowed.");
    }
    return firstActiveOrbital;
  }

  private Integer integerOption(
      final List<ClassicalCalculationOption> options, final String optionName) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(optionName)) {
        continue;
      }
      try {
        return Integer.valueOf(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException("Active-space first orbital option must be an integer.");
      }
    }
    return null;
  }

  private long longOption(
      final ClassicalCalculationRequest request, final String optionName, final long defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(optionName)) {
        continue;
      }
      try {
        final long value = Long.parseLong(option.value());
        if (value <= 0L) {
          throw new IllegalArgumentException(optionName + " must be positive.");
        }
        return value;
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(optionName + " must be a long integer.");
      }
    }
    return defaultValue;
  }

  private DirectGaussianElectronRepulsionIntegralProvider directElectronRepulsionProvider(
      final ClassicalCalculationRequest request,
      final IntegralMolecule integralMolecule,
      final GaussianBasisSet basisSet) {
    return new DirectGaussianElectronRepulsionIntegralProvider(
        integralMolecule,
        basisSet,
        this.integralEngine,
        DIRECT_ERI_CACHE_SIZE,
        this.directEriScreeningTolerance(request));
  }

  private double directEriScreeningTolerance(final ClassicalCalculationRequest request) {
    final Double configured =
        this.floatingOption(
            request.options(),
            ElectronicStructurePreparationPipeline.DIRECT_ERI_SCREENING_TOLERANCE_OPTION);
    if (configured == null) {
      return 1.0E-13;
    }
    if (!Double.isFinite(configured.doubleValue()) || configured.doubleValue() < 0.0) {
      throw new IllegalArgumentException(
          ElectronicStructurePreparationPipeline.DIRECT_ERI_SCREENING_TOLERANCE_OPTION
              + " option must be finite and non-negative.");
    }
    return configured.doubleValue();
  }

  private double[] uhfHamiltonianCoefficients(
      final ClassicalCalculationRequest request,
      final UhfResult uhfResult,
      final int orbitalCount) {
    final String requestedChannel =
        this.stringOption(
            request.options(),
            ElectronicStructurePreparationPipeline.UHF_HAMILTONIAN_ORBITAL_CHANNEL_OPTION);
    final String normalizedChannel =
        requestedChannel == null
            ? ElectronicStructurePreparationPipeline.UHF_ALPHA_CHANNEL
            : requestedChannel.trim().toLowerCase(java.util.Locale.ROOT);
    if (normalizedChannel.equals(ElectronicStructurePreparationPipeline.UHF_ALPHA_CHANNEL)) {
      return uhfResult.alphaMolecularOrbitalCoefficientsCopy();
    }
    if (normalizedChannel.equals(ElectronicStructurePreparationPipeline.UHF_BETA_CHANNEL)) {
      return uhfResult.betaMolecularOrbitalCoefficientsCopy();
    }
    if (normalizedChannel.equals(ElectronicStructurePreparationPipeline.UHF_AVERAGE_CHANNEL)) {
      return ElectronicStructurePreparationPipeline.averageNormalizedUhfCoefficients(
          uhfResult.alphaMolecularOrbitalCoefficientsCopy(),
          uhfResult.betaMolecularOrbitalCoefficientsCopy(),
          orbitalCount);
    }
    throw new IllegalArgumentException(
        "UHF Hamiltonian orbital channel must be alpha, beta, or average.");
  }

  private double[] unrestrictedDftHamiltonianCoefficients(
      final ClassicalCalculationRequest request,
      final KohnShamDftResult dftResult,
      final int orbitalCount) {
    final String requestedChannel =
        this.stringOption(
            request.options(),
            ElectronicStructurePreparationPipeline.UHF_HAMILTONIAN_ORBITAL_CHANNEL_OPTION);
    final String normalizedChannel =
        requestedChannel == null
            ? ElectronicStructurePreparationPipeline.UHF_ALPHA_CHANNEL
            : requestedChannel.trim().toLowerCase(java.util.Locale.ROOT);
    if (normalizedChannel.equals(ElectronicStructurePreparationPipeline.UHF_ALPHA_CHANNEL)) {
      return dftResult.alphaMolecularOrbitalCoefficientsCopy();
    }
    if (normalizedChannel.equals(ElectronicStructurePreparationPipeline.UHF_BETA_CHANNEL)) {
      return dftResult.betaMolecularOrbitalCoefficientsCopy();
    }
    if (normalizedChannel.equals(ElectronicStructurePreparationPipeline.UHF_AVERAGE_CHANNEL)) {
      return ElectronicStructurePreparationPipeline.averageNormalizedUhfCoefficients(
          dftResult.alphaMolecularOrbitalCoefficientsCopy(),
          dftResult.betaMolecularOrbitalCoefficientsCopy(),
          orbitalCount);
    }
    throw new IllegalArgumentException(
        "Unrestricted DFT Hamiltonian orbital channel must be alpha, beta, or average.");
  }

  private static double[] averageNormalizedUhfCoefficients(
      final double[] alphaCoefficients, final double[] betaCoefficients, final int orbitalCount) {
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    if (alphaCoefficients == null || alphaCoefficients.length != matrixSize) {
      throw new IllegalArgumentException(
          "UHF alpha coefficient matrix length must match orbital count.");
    }
    if (betaCoefficients == null || betaCoefficients.length != matrixSize) {
      throw new IllegalArgumentException(
          "UHF beta coefficient matrix length must match orbital count.");
    }
    final double[] coefficients = new double[matrixSize];
    for (int column = 0; column < orbitalCount; ++column) {
      double norm = 0.0;
      for (int row = 0; row < orbitalCount; ++row) {
        final int index = row * orbitalCount + column;
        final double value = 0.5 * (alphaCoefficients[index] + betaCoefficients[index]);
        coefficients[index] = value;
        norm += value * value;
      }
      if (norm <= 0.0 || !Double.isFinite(norm)) {
        throw new IllegalArgumentException(
            "Averaged UHF coefficient column has zero or non-finite norm.");
      }
      final double factor = 1.0 / Math.sqrt(norm);
      for (int row = 0; row < orbitalCount; ++row) {
        coefficients[row * orbitalCount + column] *= factor;
      }
    }
    return coefficients;
  }

  private double[] coreHamiltonianOrbitalCoefficients(final OneElectronIntegralMatrices matrices) {
    final int size = matrices.orbitalCount();
    final double[] orthogonalizer = this.symmetricOrthogonalizer(matrices.overlapCopy(), size);
    final double[] transformedCore =
        ElectronicStructurePreparationPipeline.transformSymmetric(
            size, orthogonalizer, matrices.coreHamiltonianCopy());
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(size, transformedCore));
    return ElectronicStructurePreparationPipeline.multiply(
        size, orthogonalizer, decomposition.eigenvectorsCopy());
  }

  private double[] symmetricOrthogonalizer(final double[] overlap, final int size) {
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(size, overlap));
    final double[] vectors = decomposition.eigenvectorsCopy();
    final double[] scaled = new double[Math.multiplyExact(size, size)];
    for (int column = 0; column < size; ++column) {
      final double eigenvalue = decomposition.eigenvalue(column);
      if (eigenvalue <= 1.0E-12) {
        throw new IllegalArgumentException("Overlap matrix is not positive definite.");
      }
      final double factor = 1.0 / Math.sqrt(eigenvalue);
      for (int row = 0; row < size; ++row) {
        scaled[row * size + column] = vectors[row * size + column] * factor;
      }
    }
    return ElectronicStructurePreparationPipeline.multiplyTransposedRight(size, scaled, vectors);
  }

  private static double[] transformSymmetric(
      final int size, final double[] transform, final double[] matrix) {
    final double[] temporary =
        ElectronicStructurePreparationPipeline.multiply(size, matrix, transform);
    return ElectronicStructurePreparationPipeline.multiplyTransposedLeft(
        size, transform, temporary);
  }

  private static double[] multiply(final int size, final double[] left, final double[] right) {
    final double[] result = new double[Math.multiplyExact(size, size)];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[row * size + k] * right[k * size + column];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private static double[] multiplyTransposedLeft(
      final int size, final double[] left, final double[] right) {
    final double[] result = new double[Math.multiplyExact(size, size)];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[k * size + row] * right[k * size + column];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private static double[] multiplyTransposedRight(
      final int size, final double[] left, final double[] right) {
    final double[] result = new double[Math.multiplyExact(size, size)];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[row * size + k] * right[column * size + k];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private RhfOptions rhfOptions(final ClassicalCalculationRequest request) {
    final Integer maxIterations =
        this.integerOption(
            request.options(), ElectronicStructurePreparationPipeline.RHF_MAX_ITERATIONS_OPTION);
    final Double convergenceTolerance =
        this.floatingOption(
            request.options(),
            ElectronicStructurePreparationPipeline.RHF_CONVERGENCE_TOLERANCE_OPTION);
    final Double densityTolerance =
        this.floatingOption(
            request.options(), ElectronicStructurePreparationPipeline.RHF_DENSITY_TOLERANCE_OPTION);
    final Double densityMixing =
        this.floatingOption(
            request.options(), ElectronicStructurePreparationPipeline.RHF_DENSITY_MIXING_OPTION);
    if (maxIterations == null
        && convergenceTolerance == null
        && densityTolerance == null
        && densityMixing == null) {
      return RhfOptions.DEFAULT;
    }
    return RhfOptions.of(
        maxIterations == null ? RhfOptions.DEFAULT.maxIterations() : maxIterations.intValue(),
        convergenceTolerance == null
            ? RhfOptions.DEFAULT.convergenceTolerance()
            : convergenceTolerance.doubleValue(),
        densityTolerance == null
            ? RhfOptions.DEFAULT.densityTolerance()
            : densityTolerance.doubleValue(),
        densityMixing == null ? RhfOptions.DEFAULT.densityMixing() : densityMixing.doubleValue());
  }

  private Double floatingOption(
      final List<ClassicalCalculationOption> options, final String optionName) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(optionName)) {
        continue;
      }
      try {
        return Double.valueOf(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(optionName + " option must be a finite number.");
      }
    }
    return null;
  }

  private boolean casscfOrbitalOptimizationRequested(final ClassicalCalculationRequest request) {
    if (request.method().type()
        == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return true;
    }
    final String value =
        this.stringOption(
            request.options(),
            ElectronicStructurePreparationPipeline.HAMILTONIAN_ORBITAL_OPTIMIZATION_OPTION);
    if (value == null) {
      return false;
    }
    final String normalized = value.trim().toLowerCase();
    if (normalized.equals("casscf")
        || normalized.equals("true")
        || normalized.equals("enabled")
        || normalized.equals("on")) {
      return true;
    }
    if (normalized.equals("false") || normalized.equals("disabled") || normalized.equals("off")) {
      return false;
    }
    throw new IllegalArgumentException(
        "Hamiltonian orbital optimization option must be casscf, true, false, enabled, or disabled.");
  }

  private String stringOption(
      final List<ClassicalCalculationOption> options, final String optionName) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(optionName)) {
        return option.value();
      }
    }
    return null;
  }

  private boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Electronic preparation request must not be null.");
    }
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(HAMILTONIAN_BACKEND_OPTION)) {
        return PYSCF_BACKEND.equalsIgnoreCase(option.value());
      }
    }
    return false;
  }

  private static int hydrogenCount(final Molecule molecule) {
    int count = 0;
    final List<ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom> atoms =
        molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (atoms.get(i).element().atomicNumber() == 1) {
        ++count;
      }
    }
    return count;
  }

  private static long power4(final int value) {
    final long longValue = value;
    return ElectronicStructurePreparationPipeline.multiplySaturated(
        ElectronicStructurePreparationPipeline.multiplySaturated(longValue, longValue),
        ElectronicStructurePreparationPipeline.multiplySaturated(longValue, longValue));
  }

  private static long packedPairCount(final int orbitalCount) {
    final long count = orbitalCount;
    final long pairCount =
        ElectronicStructurePreparationPipeline.multiplySaturated(count, count + 1L) / 2L;
    return ElectronicStructurePreparationPipeline.multiplySaturated(pairCount, pairCount);
  }

  private boolean directIntegralModeRequested(final ClassicalCalculationRequest request) {
    final String value =
        this.stringOption(
            request.options(), ElectronicStructurePreparationPipeline.SCF_INTEGRAL_MODE_OPTION);
    return value != null
        && ElectronicStructurePreparationPipeline.DIRECT_INTEGRAL_MODE.equalsIgnoreCase(value);
  }

  private boolean coreHamiltonianReferenceModeRequested(final ClassicalCalculationRequest request) {
    final String value =
        this.stringOption(
            request.options(), ElectronicStructurePreparationPipeline.SCF_REFERENCE_MODE_OPTION);
    return value != null
        && ElectronicStructurePreparationPipeline.CORE_HAMILTONIAN_REFERENCE_MODE.equalsIgnoreCase(
            value);
  }

  private boolean supportsSpinTreatment(final ElectronicStructureSpinTreatment spinTreatment) {
    return spinTreatment == ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL
        || spinTreatment == ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL
        || spinTreatment == ElectronicStructureSpinTreatment.UNRESTRICTED
        || spinTreatment == ElectronicStructureSpinTreatment.UNSPECIFIED;
  }

  private GaussianBasisSet resolveBasisSet(
      final BasisSet basisSet, final IntegralMolecule integralMolecule) {
    if (basisSet.hasStructuredDefinition()) {
      return this.structuredBasisAdapter.toGaussianBasisSet(
          basisSet.name().value(), basisSet.definition(), integralMolecule);
    }
    if (basisSet.hasDefinitionText()) {
      return this.basisTextParser.parseForMolecule(
          basisSet.name().value(), basisSet.definitionText(), integralMolecule);
    }
    if (ElectronicStructurePreparationPipeline.STO_3G_NAME.equalsIgnoreCase(
        basisSet.name().value())) {
      final String unsupportedBasisReason =
          GaussianBasisLibrary.unsupportedSto3gReason(integralMolecule);
      if (unsupportedBasisReason != null) {
        throw new IllegalArgumentException(unsupportedBasisReason);
      }
      return GaussianBasisLibrary.sto3g(integralMolecule);
    }
    if (ElectronicStructurePreparationPipeline.SIX_31G_NAME.equalsIgnoreCase(
        basisSet.name().value())) {
      final String unsupportedBasisReason =
          GaussianBasisLibrary.unsupportedSix31gReason(integralMolecule);
      if (unsupportedBasisReason != null) {
        throw new IllegalArgumentException(unsupportedBasisReason);
      }
      return GaussianBasisLibrary.six31g(integralMolecule);
    }
    if (GaussianBasisLibrary.generatedEvenTemperedName(basisSet.name().value())) {
      return GaussianBasisLibrary.generatedEvenTempered(integralMolecule);
    }
    if (GaussianBasisLibrary.generatedCompactEvenTemperedName(basisSet.name().value())) {
      return GaussianBasisLibrary.generatedCompactEvenTempered(integralMolecule);
    }
    throw new IllegalArgumentException(
        "Electronic preparation requires STO-3G, 6-31G, AUTO-EVEN-TEMPERED, AUTO-EVEN-TEMPERED-COMPACT or a self-contained Gaussian basis definition.");
  }
}
