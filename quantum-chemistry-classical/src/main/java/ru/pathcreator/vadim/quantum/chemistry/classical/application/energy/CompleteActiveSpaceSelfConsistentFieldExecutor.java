/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.ActiveSpaceIntegralProjector;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.ElectronicHamiltonianIntegralBuilder;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.MolecularOrbitalIntegralTransformer;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.FullConfigurationInteractionSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfOrbitalRotationSpace;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.OrbitalRotationOptimizer;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalRotationPair;

public final class CompleteActiveSpaceSelfConsistentFieldExecutor {

  private static final String ACTIVE_SPACE_FIRST_ORBITAL_OPTION = "active_space_first_orbital";
  private static final String CASSCF_MAX_ITERATIONS_OPTION = "casscf_max_iterations";
  private static final String CASSCF_INITIAL_STEP_OPTION = "casscf_initial_step";
  private static final String CASSCF_MINIMUM_STEP_OPTION = "casscf_minimum_step";
  private static final String CASSCF_ENERGY_TOLERANCE_OPTION = "casscf_energy_tolerance";
  private static final String CASSCF_MAX_ORBITAL_PAIRS_OPTION = "casscf_max_orbital_pairs";
  private static final String CASSCF_ORBITAL_ROTATION_SPACE_OPTION =
      "casscf_orbital_rotation_space";
  private static final String FCI_MAX_DETERMINANTS_OPTION = "fci_max_determinants";
  private static final String FCI_ROOT_COUNT_OPTION = "fci_root_count";
  private static final String FCI_ALPHA_ELECTRONS_OPTION = "fci_alpha_electrons";
  private static final String FCI_BETA_ELECTRONS_OPTION = "fci_beta_electrons";

  private final MolecularOrbitalIntegralTransformer orbitalIntegralTransformer;
  private final ActiveSpaceIntegralProjector activeSpaceProjector;
  private final ElectronicHamiltonianIntegralBuilder hamiltonianBuilder;
  private final FullConfigurationInteractionSolver fciSolver;
  private final OrbitalRotationOptimizer orbitalRotationOptimizer;

  public CompleteActiveSpaceSelfConsistentFieldExecutor() {
    this(
        new MolecularOrbitalIntegralTransformer(),
        new ActiveSpaceIntegralProjector(),
        new ElectronicHamiltonianIntegralBuilder(),
        new FullConfigurationInteractionSolver(),
        new OrbitalRotationOptimizer());
  }

  public CompleteActiveSpaceSelfConsistentFieldExecutor(
      final MolecularOrbitalIntegralTransformer orbitalIntegralTransformer,
      final ActiveSpaceIntegralProjector activeSpaceProjector,
      final ElectronicHamiltonianIntegralBuilder hamiltonianBuilder,
      final FullConfigurationInteractionSolver fciSolver,
      final OrbitalRotationOptimizer orbitalRotationOptimizer) {
    if (orbitalIntegralTransformer == null) {
      throw new IllegalArgumentException("CASSCF MO transformer must not be null.");
    }
    if (activeSpaceProjector == null) {
      throw new IllegalArgumentException("CASSCF active-space projector must not be null.");
    }
    if (hamiltonianBuilder == null) {
      throw new IllegalArgumentException("CASSCF Hamiltonian builder must not be null.");
    }
    if (fciSolver == null) {
      throw new IllegalArgumentException("CASSCF FCI solver must not be null.");
    }
    if (orbitalRotationOptimizer == null) {
      throw new IllegalArgumentException("CASSCF orbital optimizer must not be null.");
    }
    this.orbitalIntegralTransformer = orbitalIntegralTransformer;
    this.activeSpaceProjector = activeSpaceProjector;
    this.hamiltonianBuilder = hamiltonianBuilder;
    this.fciSolver = fciSolver;
    this.orbitalRotationOptimizer = orbitalRotationOptimizer;
  }

  public CasscfResult solve(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    if (request == null) {
      throw new IllegalArgumentException("CASSCF request must not be null.");
    }
    if (preparationResult == null) {
      throw new IllegalArgumentException("CASSCF preparation result must not be null.");
    }
    if (!request.hasActiveSpace()) {
      throw new IllegalArgumentException("CASSCF requires active space.");
    }
    if (!preparationResult.hasAtomicOrbitalTensors() || !preparationResult.hasRhfReference()) {
      throw new IllegalArgumentException(
          "CASSCF requires Java-native AO tensors and RHF reference.");
    }
    final ActiveSpace activeSpace = request.activeSpace();
    final MolecularIntegralTensors atomicOrbitalTensors = preparationResult.atomicOrbitalTensors();
    final int fullOrbitalCount = atomicOrbitalTensors.orbitalCount();
    final int firstActiveOrbital =
        CompleteActiveSpaceSelfConsistentFieldExecutor.firstActiveOrbital(
            request, activeSpace, preparationResult.electronCount());
    final int exclusiveLastActiveOrbital =
        Math.addExact(firstActiveOrbital, activeSpace.orbitalCount());
    if (exclusiveLastActiveOrbital > fullOrbitalCount) {
      throw new IllegalArgumentException(
          "CASSCF active orbital window exceeds full orbital count.");
    }
    final CasscfOptions options = CompleteActiveSpaceSelfConsistentFieldExecutor.options(request);
    final FciOptions fciOptions =
        CompleteActiveSpaceSelfConsistentFieldExecutor.fciOptions(request, activeSpace, options);
    final double[] overlap = atomicOrbitalTensors.overlapCopy();
    double[] coefficients = preparationResult.rhfResult().molecularOrbitalCoefficientsCopy();
    OrbitalRotationOptimizer.orthonormalizeColumns(
        coefficients, overlap, fullOrbitalCount, 1.0E-10);
    CasscfEvaluation best =
        this.evaluate(
            atomicOrbitalTensors, coefficients, activeSpace, firstActiveOrbital, fciOptions);
    final List<OrbitalRotationPair> rotationPairs =
        CompleteActiveSpaceSelfConsistentFieldExecutor.rotationPairs(
            options.orbitalRotationSpace(),
            firstActiveOrbital,
            exclusiveLastActiveOrbital,
            fullOrbitalCount);
    if (rotationPairs.isEmpty()) {
      return CasscfResult.of(
          true,
          0,
          0,
          0,
          0,
          firstActiveOrbital,
          activeSpace.electronCount(),
          activeSpace.orbitalCount(),
          options.orbitalRotationSpace().name(),
          0.0,
          best.totalEnergy(),
          best.electronicEnergy(),
          best.totalEnergy(),
          OrbitalRotationOptimizer.maxOrthonormalityError(coefficients, overlap, fullOrbitalCount),
          0.0,
          0.0,
          1.0,
          "TIGHT",
          "NO_NON_REDUNDANT_ROTATION_PAIR",
          coefficients,
          List.of(),
          best.fciResult());
    }
    final OrbitalOptimizationOptions orbitalOptions =
        OrbitalOptimizationOptions.of(
            Math.multiplyExact(
                options.maxIterations(),
                Math.max(
                    1,
                    (rotationPairs.size() + options.maxOrbitalPairs() - 1)
                        / options.maxOrbitalPairs())),
            options.maxOrbitalPairs(),
            options.initialStepRadians(),
            options.minimumStepRadians(),
            options.energyTolerance(),
            1.0E-10);
    final OrbitalOptimizationResult orbitalOptimizationResult =
        options.maxOrbitalPairs() >= rotationPairs.size()
            ? this.orbitalRotationOptimizer.optimizeGradientRanked(
                coefficients,
                overlap,
                fullOrbitalCount,
                rotationPairs,
                trialCoefficients ->
                    this.evaluate(
                            atomicOrbitalTensors,
                            trialCoefficients,
                            activeSpace,
                            firstActiveOrbital,
                            fciOptions)
                        .totalEnergy(),
                orbitalOptions)
            : this.orbitalRotationOptimizer.optimize(
                coefficients,
                overlap,
                fullOrbitalCount,
                rotationPairs,
                trialCoefficients ->
                    this.evaluate(
                            atomicOrbitalTensors,
                            trialCoefficients,
                            activeSpace,
                            firstActiveOrbital,
                            fciOptions)
                        .totalEnergy(),
                orbitalOptions);
    coefficients = orbitalOptimizationResult.optimizedCoefficientsCopy();
    best =
        this.evaluate(
            atomicOrbitalTensors, coefficients, activeSpace, firstActiveOrbital, fciOptions);
    return CasscfResult.of(
        orbitalOptimizationResult.converged(),
        orbitalOptimizationResult.iterations(),
        orbitalOptimizationResult.evaluatedRotationCount(),
        orbitalOptimizationResult.acceptedRotationCount(),
        rotationPairs.size(),
        firstActiveOrbital,
        activeSpace.electronCount(),
        activeSpace.orbitalCount(),
        options.orbitalRotationSpace().name(),
        orbitalOptimizationResult.finalStepRadians(),
        orbitalOptimizationResult.initialEnergy(),
        best.electronicEnergy(),
        best.totalEnergy(),
        OrbitalRotationOptimizer.maxOrthonormalityError(coefficients, overlap, fullOrbitalCount),
        orbitalOptimizationResult.maxResidualEnergyImprovement(),
        orbitalOptimizationResult.maxResidualOrbitalGradient(),
        CompleteActiveSpaceSelfConsistentFieldExecutor.rotationCoverageRatio(
            orbitalOptimizationResult.evaluatedRotationCount(), rotationPairs.size()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.optimizationQuality(
            orbitalOptimizationResult.converged(),
            orbitalOptimizationResult.maxResidualEnergyImprovement(),
            orbitalOptimizationResult.maxResidualOrbitalGradient(),
            OrbitalRotationOptimizer.maxOrthonormalityError(
                coefficients, overlap, fullOrbitalCount)),
        CompleteActiveSpaceSelfConsistentFieldExecutor.stopReason(
            orbitalOptimizationResult.converged(),
            orbitalOptimizationResult.acceptedRotationCount(),
            orbitalOptimizationResult.finalStepRadians(),
            orbitalOptions.minimumStepRadians()),
        coefficients,
        orbitalOptimizationResult.iterationHistory(),
        best.fciResult());
  }

  private static double rotationCoverageRatio(
      final int evaluatedRotationCount, final int availableRotationPairCount) {
    if (availableRotationPairCount <= 0) {
      return 1.0;
    }
    return Math.min(1.0, (double) evaluatedRotationCount / (double) availableRotationPairCount);
  }

  private static String optimizationQuality(
      final boolean converged,
      final double maxResidualEnergyImprovement,
      final double maxResidualOrbitalGradient,
      final double maxOrthonormalityError) {
    if (!converged) {
      return "NOT_CONVERGED";
    }
    if (maxResidualOrbitalGradient <= 1.0E-6
        && maxResidualEnergyImprovement <= 1.0E-8
        && maxOrthonormalityError <= 1.0E-9) {
      return "TIGHT";
    }
    if (maxResidualOrbitalGradient <= 1.0E-4
        && maxResidualEnergyImprovement <= 1.0E-6
        && maxOrthonormalityError <= 1.0E-8) {
      return "PRODUCTION";
    }
    return "LOOSE";
  }

  private static String stopReason(
      final boolean converged,
      final int acceptedRotationCount,
      final double finalStepRadians,
      final double minimumStepRadians) {
    if (converged) {
      return "RESIDUAL_AND_ENERGY_CONVERGED";
    }
    if (acceptedRotationCount == 0) {
      return "NO_DESCENDING_ROTATION_ACCEPTED";
    }
    if (finalStepRadians < minimumStepRadians) {
      return "STEP_EXHAUSTED";
    }
    return "ITERATION_LIMIT_OR_RESIDUAL_REFINEMENT";
  }

  private CasscfEvaluation evaluate(
      final MolecularIntegralTensors atomicOrbitalTensors,
      final double[] coefficients,
      final ActiveSpace activeSpace,
      final int firstActiveOrbital,
      final FciOptions fciOptions) {
    final MolecularIntegralTensors windowTensors =
        this.orbitalIntegralTransformer.transformOrbitalWindow(
            atomicOrbitalTensors, coefficients, firstActiveOrbital, activeSpace.orbitalCount());
    final ElectronicHamiltonian hamiltonian =
        this.hamiltonianBuilder.build(activeSpace.electronCount(), windowTensors);
    final FciResult fciResult = this.fciSolver.solve(hamiltonian, fciOptions);
    return CasscfEvaluation.of(fciResult);
  }

  private static List<OrbitalRotationPair> rotationPairs(
      final CasscfOrbitalRotationSpace orbitalRotationSpace,
      final int firstActiveOrbital,
      final int exclusiveLastActiveOrbital,
      final int fullOrbitalCount) {
    final ArrayList<OrbitalRotationPair> pairs = new ArrayList<OrbitalRotationPair>();
    if (orbitalRotationSpace == CasscfOrbitalRotationSpace.ACTIVE_EXTERNAL
        || orbitalRotationSpace == CasscfOrbitalRotationSpace.ALL_NON_REDUNDANT) {
      CompleteActiveSpaceSelfConsistentFieldExecutor.addPairs(
          pairs,
          firstActiveOrbital,
          exclusiveLastActiveOrbital,
          exclusiveLastActiveOrbital,
          fullOrbitalCount);
    }
    if (orbitalRotationSpace == CasscfOrbitalRotationSpace.INACTIVE_ACTIVE
        || orbitalRotationSpace == CasscfOrbitalRotationSpace.ALL_NON_REDUNDANT) {
      CompleteActiveSpaceSelfConsistentFieldExecutor.addPairs(
          pairs, 0, firstActiveOrbital, firstActiveOrbital, exclusiveLastActiveOrbital);
    }
    if (orbitalRotationSpace == CasscfOrbitalRotationSpace.INACTIVE_EXTERNAL
        || orbitalRotationSpace == CasscfOrbitalRotationSpace.ALL_NON_REDUNDANT) {
      CompleteActiveSpaceSelfConsistentFieldExecutor.addPairs(
          pairs, 0, firstActiveOrbital, exclusiveLastActiveOrbital, fullOrbitalCount);
    }
    return List.copyOf(pairs);
  }

  private static void addPairs(
      final ArrayList<OrbitalRotationPair> pairs,
      final int firstLeft,
      final int exclusiveLastLeft,
      final int firstRight,
      final int exclusiveLastRight) {
    for (int left = firstLeft; left < exclusiveLastLeft; ++left) {
      for (int right = firstRight; right < exclusiveLastRight; ++right) {
        pairs.add(OrbitalRotationPair.of(left, right));
      }
    }
  }

  private static CasscfOptions options(final ClassicalCalculationRequest request) {
    final CasscfOptions defaults = CasscfOptions.defaults();
    return CasscfOptions.of(
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, CASSCF_MAX_ITERATIONS_OPTION, defaults.maxIterations()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, FCI_MAX_DETERMINANTS_OPTION, defaults.maxDeterminants()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, FCI_ROOT_COUNT_OPTION, defaults.rootCount()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, CASSCF_MAX_ORBITAL_PAIRS_OPTION, defaults.maxOrbitalPairs()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.doubleOption(
            request, CASSCF_INITIAL_STEP_OPTION, defaults.initialStepRadians()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.doubleOption(
            request, CASSCF_MINIMUM_STEP_OPTION, defaults.minimumStepRadians()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.doubleOption(
            request, CASSCF_ENERGY_TOLERANCE_OPTION, defaults.energyTolerance()),
        CompleteActiveSpaceSelfConsistentFieldExecutor.rotationSpaceOption(
            request, defaults.orbitalRotationSpace()));
  }

  private static FciOptions fciOptions(
      final ClassicalCalculationRequest request,
      final ActiveSpace activeSpace,
      final CasscfOptions options) {
    final int alphaElectronCount =
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, FCI_ALPHA_ELECTRONS_OPTION, -1);
    final int betaElectronCount =
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, FCI_BETA_ELECTRONS_OPTION, -1);
    if (alphaElectronCount >= 0 || betaElectronCount >= 0) {
      if (alphaElectronCount < 0 || betaElectronCount < 0) {
        throw new IllegalArgumentException(
            "CASSCF spin-sector options require both alpha and beta electron counts.");
      }
      return FciOptions.spinSector(
          options.maxDeterminants(), options.rootCount(), alphaElectronCount, betaElectronCount);
    }
    return FciOptions.of(
        options.maxDeterminants(), Math.min(options.rootCount(), activeSpace.spinOrbitalCount()));
  }

  private static int firstActiveOrbital(
      final ClassicalCalculationRequest request,
      final ActiveSpace activeSpace,
      final int fullElectronCount) {
    final int inferredFirstOrbital =
        Math.floorDiv(fullElectronCount - activeSpace.electronCount(), 2);
    final int firstActiveOrbital =
        CompleteActiveSpaceSelfConsistentFieldExecutor.integerOption(
            request, ACTIVE_SPACE_FIRST_ORBITAL_OPTION, inferredFirstOrbital);
    if (firstActiveOrbital < 0) {
      throw new IllegalArgumentException("CASSCF first active orbital must not be negative.");
    }
    return firstActiveOrbital;
  }

  private static int integerOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(name)) {
        continue;
      }
      try {
        return Integer.parseInt(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException("CASSCF option " + name + " must be an integer.");
      }
    }
    return defaultValue;
  }

  private static double doubleOption(
      final ClassicalCalculationRequest request, final String name, final double defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(name)) {
        continue;
      }
      try {
        return Double.parseDouble(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException("CASSCF option " + name + " must be numeric.");
      }
    }
    return defaultValue;
  }

  private static CasscfOrbitalRotationSpace rotationSpaceOption(
      final ClassicalCalculationRequest request, final CasscfOrbitalRotationSpace defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(CASSCF_ORBITAL_ROTATION_SPACE_OPTION)) {
        return CasscfOrbitalRotationSpace.parse(option.value());
      }
    }
    return defaultValue;
  }

  private static final class CasscfEvaluation {

    private final FciResult fciResult;

    private CasscfEvaluation(final FciResult fciResult) {
      this.fciResult = fciResult;
    }

    static CasscfEvaluation of(final FciResult fciResult) {
      return new CasscfEvaluation(fciResult);
    }

    double electronicEnergy() {
      return this.fciResult.electronicEnergy();
    }

    double totalEnergy() {
      return this.fciResult.totalEnergy();
    }

    FciResult fciResult() {
      return this.fciResult;
    }
  }
}
