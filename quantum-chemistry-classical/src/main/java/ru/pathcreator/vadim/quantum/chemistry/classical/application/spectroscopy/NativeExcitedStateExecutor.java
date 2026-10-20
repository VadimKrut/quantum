/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPreflight;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.JacobiSymmetricEigenSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

/** Выполняет Java-native excited-state оценку по SCF orbital manifold без внешнего backend. */
public final class NativeExcitedStateExecutor {

  private static final String EXCITED_STATE_COUNT_OPTION = "excited_state_count";
  private static final String MAX_ORBITALS_OPTION = "native_excited_state_max_orbitals";
  private static final int DEFAULT_EXCITED_STATE_COUNT = 5;
  private static final int DEFAULT_MAX_ORBITALS = 8;
  private static final int MAX_EXCITED_STATE_COUNT = 256;
  private static final double HARTREE_TO_EV = 27.211386245988;
  private static final double EV_NANOMETER_PRODUCT = 1239.8419843320026;

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final JacobiSymmetricEigenSolver eigenSolver;

  public NativeExcitedStateExecutor() {
    this(new ElectronicStructurePreparationPipeline(), new JacobiSymmetricEigenSolver());
  }

  public NativeExcitedStateExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(preparationPipeline, new JacobiSymmetricEigenSolver());
  }

  public NativeExcitedStateExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final JacobiSymmetricEigenSolver eigenSolver) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Native excited-state preparation pipeline must not be null.");
    }
    if (eigenSolver == null) {
      throw new IllegalArgumentException("Native excited-state eigen solver must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.eigenSolver = eigenSolver;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Native excited-state request must not be null.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("Native excited-state molecule must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Native excited-state kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Native excited-state diagnostics must not be null.");
    }
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    if (unsupportedReason != null) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              unsupportedReason));
      return null;
    }
    final ClassicalResultSection resourceBoundarySection =
        this.resourceBoundarySection(request, kind, diagnostics);
    if (resourceBoundarySection != null) {
      return resourceBoundarySection;
    }
    final ElectronicStructurePreparationResult preparation =
        this.preparationPipeline.prepare(request);
    final int stateCount = NativeExcitedStateExecutor.stateCount(request.options());
    final List<ExcitedStateCandidate> candidates =
        preparation.hasRhfReference()
            ? this.rhfCandidates(preparation, stateCount)
            : this.uhfCandidates(preparation, stateCount);
    if (candidates.isEmpty()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              "Native excited-state analysis requires at least one occupied and one virtual orbital."));
      return null;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "Native excited-state analysis uses CIS/TDA-style single-excitation estimates over the Java-native SCF reference."));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "java_native_cis_tda_excited_state",
        "excited_state_analysis",
        "CIS/TDA single-excitation analysis is not full linear-response or multireference excited-state theory.");
    values.add(ClassicalResultValue.text("model", "java_native_cis_tda_excited_state"));
    values.add(
        ClassicalResultValue.text("method", NativeExcitedStateExecutor.methodName(preparation)));
    values.add(
        ClassicalResultValue.text(
            "excitation_model",
            preparation.hasRhfReference()
                ? "cis_tda_matrix_diagonalization"
                : "unrestricted_spin_block_tda_matrix_diagonalization"));
    values.add(ClassicalResultValue.text("basis_set", preparation.basisSetName()));
    values.add(
        ClassicalResultValue.scalar(
            "basis_orbital_count", preparation.basisOrbitalCount(), "count"));
    values.add(ClassicalResultValue.scalar("electron_count", preparation.electronCount(), "count"));
    values.add(ClassicalResultValue.scalar("requested_state_count", stateCount, "count"));
    values.add(ClassicalResultValue.scalar("state_count", candidates.size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "lowest_excitation_energy_hartree",
            NativeExcitedStateExecutor.lowestEnergy(candidates),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "highest_reported_excitation_energy_hartree",
            NativeExcitedStateExecutor.highestEnergy(candidates),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "lowest_excitation_energy_ev",
            NativeExcitedStateExecutor.lowestEnergy(candidates) * HARTREE_TO_EV,
            "eV"));
    values.add(
        ClassicalResultValue.scalar(
            "oscillator_strength_sum",
            NativeExcitedStateExecutor.oscillatorStrengthSum(candidates),
            "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "bright_state_count",
            NativeExcitedStateExecutor.brightStateCount(candidates),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "excited_states_sorted_by_energy",
            Boolean.toString(NativeExcitedStateExecutor.sortedByEnergy(candidates))));
    values.add(
        ClassicalResultValue.scalar(
            "single_excitation_dimension",
            NativeExcitedStateExecutor.singleExcitationDimension(preparation),
            "count"));
    values.add(
        ClassicalResultValue.of(
            "excited_state_rows",
            ClassicalResultValueKind.TABLE,
            "state",
            NativeExcitedStateExecutor.rows(candidates)));
    return ClassicalResultSection.of(kind, "Java-native CIS/TDA excited-state analysis", values);
  }

  private ClassicalResultSection resourceBoundarySection(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(request);
    if (!preflight.hasFullTensorPreflight()) {
      return null;
    }
    final int maxOrbitals =
        NativeExcitedStateExecutor.integerOption(
            request.options(), MAX_ORBITALS_OPTION, DEFAULT_MAX_ORBITALS);
    if (preflight.fullTensorPreflight().orbitalCount() <= maxOrbitals) {
      return null;
    }
    final String message =
        "Native excited-state analysis skipped full CIS/TDA matrix because orbital count "
            + preflight.fullTensorPreflight().orbitalCount()
            + " exceeds configured "
            + MAX_ORBITALS_OPTION
            + "="
            + maxOrbitals
            + ".";
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            message));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addFallback(
        values,
        "java_native",
        "java_native_cis_tda_excited_state_resource_boundary",
        "excited_state_analysis",
        message);
    values.add(
        ClassicalResultValue.text("model", "java_native_cis_tda_excited_state_resource_boundary"));
    values.add(ClassicalResultValue.text("method", "resource_preflight"));
    values.add(ClassicalResultValue.text("basis_set", request.basisSet().name().value()));
    values.add(
        ClassicalResultValue.scalar(
            "basis_orbital_count", preflight.fullTensorPreflight().orbitalCount(), "count"));
    values.add(ClassicalResultValue.scalar("state_count", 0.0, "count"));
    values.add(ClassicalResultValue.text("resource_boundary", message));
    values.add(
        ClassicalResultValue.of(
            "excited_state_rows",
            ClassicalResultValueKind.TABLE,
            "state",
            List.of("resource_boundary\tunavailable\t" + message)));
    return ClassicalResultSection.of(
        kind, "Java-native CIS/TDA excited-state analysis resource boundary", values);
  }

  private List<ExcitedStateCandidate> rhfCandidates(
      final ElectronicStructurePreparationResult preparation, final int stateCount) {
    final RhfResult reference = preparation.rhfResult();
    final double[] orbitalEnergies = reference.orbitalEnergiesCopy();
    final int occupiedCount = Math.max(0, preparation.electronCount() / 2);
    final MolecularIntegralTensors tensors = preparation.molecularOrbitalTensors();
    final ArrayList<SingleExcitation> excitations = new ArrayList<SingleExcitation>();
    for (int occupied = 0; occupied < occupiedCount; ++occupied) {
      for (int virtual = occupiedCount; virtual < orbitalEnergies.length; ++virtual) {
        final double orbitalGap = orbitalEnergies[virtual] - orbitalEnergies[occupied];
        if (orbitalGap <= 0.0 || !Double.isFinite(orbitalGap)) {
          continue;
        }
        excitations.add(SingleExcitation.of(occupied, virtual, orbitalGap));
      }
    }
    if (excitations.isEmpty()) {
      return List.of();
    }
    final int dimension = excitations.size();
    final double[] matrix = new double[Math.multiplyExact(dimension, dimension)];
    for (int row = 0; row < dimension; ++row) {
      final SingleExcitation rowExcitation = excitations.get(row);
      for (int column = row; column < dimension; ++column) {
        final SingleExcitation columnExcitation = excitations.get(column);
        final double value =
            NativeExcitedStateExecutor.cisTdaMatrixElement(
                rowExcitation, columnExcitation, orbitalEnergies, tensors);
        matrix[row * dimension + column] = value;
        matrix[column * dimension + row] = value;
      }
    }
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(dimension, matrix));
    final int rootCount = Math.min(stateCount, dimension);
    final double[] eigenvectors = decomposition.eigenvectorsCopy();
    final ArrayList<ExcitedStateCandidate> candidates =
        new ArrayList<ExcitedStateCandidate>(rootCount);
    for (int root = 0; root < rootCount; ++root) {
      final DominantExcitation dominant =
          NativeExcitedStateExecutor.dominantExcitation(excitations, eigenvectors, dimension, root);
      final double energyHartree = Math.max(1.0E-8, decomposition.eigenvalue(root));
      candidates.add(
          ExcitedStateCandidate.of(
              "S",
              dominant.occupiedOrbital(),
              dominant.virtualOrbital(),
              energyHartree,
              NativeExcitedStateExecutor.oscillatorStrength(
                  energyHartree, dominant.weight(), dominant.collectiveWeight())));
    }
    return List.copyOf(candidates);
  }

  private static double cisTdaMatrixElement(
      final SingleExcitation rowExcitation,
      final SingleExcitation columnExcitation,
      final double[] orbitalEnergies,
      final MolecularIntegralTensors tensors) {
    final int occupied = rowExcitation.occupiedOrbital();
    final int virtual = rowExcitation.virtualOrbital();
    final int otherOccupied = columnExcitation.occupiedOrbital();
    final int otherVirtual = columnExcitation.virtualOrbital();
    double value = 0.0;
    if (occupied == otherOccupied && virtual == otherVirtual) {
      value += orbitalEnergies[virtual] - orbitalEnergies[occupied];
    }
    value += 2.0 * tensors.electronRepulsionAt(virtual, occupied, otherVirtual, otherOccupied);
    value -= tensors.electronRepulsionAt(virtual, otherVirtual, otherOccupied, occupied);
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Native CIS/TDA matrix contains non-finite value.");
    }
    return value;
  }

  private static DominantExcitation dominantExcitation(
      final List<SingleExcitation> excitations,
      final double[] eigenvectors,
      final int dimension,
      final int root) {
    int dominantIndex = 0;
    double dominantWeight = -1.0;
    double collectiveWeight = 0.0;
    for (int index = 0; index < dimension; ++index) {
      final double coefficient = eigenvectors[index * dimension + root];
      final double weight = coefficient * coefficient;
      collectiveWeight += weight;
      if (weight > dominantWeight) {
        dominantWeight = weight;
        dominantIndex = index;
      }
    }
    final SingleExcitation excitation = excitations.get(dominantIndex);
    return DominantExcitation.of(
        excitation.occupiedOrbital(),
        excitation.virtualOrbital(),
        Math.max(0.0, dominantWeight),
        Math.max(0.0, collectiveWeight));
  }

  private List<ExcitedStateCandidate> uhfCandidates(
      final ElectronicStructurePreparationResult preparation, final int stateCount) {
    final UhfResult reference = preparation.uhfResult();
    final ArrayList<ExcitedStateCandidate> candidates = new ArrayList<ExcitedStateCandidate>();
    this.addSpinBlockCandidates(
        candidates,
        "A",
        reference.alphaElectronCount(),
        reference.alphaOrbitalEnergiesCopy(),
        preparation.molecularOrbitalTensors());
    this.addSpinBlockCandidates(
        candidates,
        "B",
        reference.betaElectronCount(),
        reference.betaOrbitalEnergiesCopy(),
        preparation.molecularOrbitalTensors());
    return NativeExcitedStateExecutor.firstStates(candidates, stateCount);
  }

  private void addSpinBlockCandidates(
      final List<ExcitedStateCandidate> candidates,
      final String spin,
      final int occupiedCount,
      final double[] orbitalEnergies,
      final MolecularIntegralTensors tensors) {
    final ArrayList<SingleExcitation> excitations = new ArrayList<SingleExcitation>();
    for (int occupied = 0; occupied < occupiedCount; ++occupied) {
      for (int virtual = occupiedCount; virtual < orbitalEnergies.length; ++virtual) {
        final double energyHartree = orbitalEnergies[virtual] - orbitalEnergies[occupied];
        if (energyHartree <= 0.0 || !Double.isFinite(energyHartree)) {
          continue;
        }
        excitations.add(SingleExcitation.of(occupied, virtual, energyHartree));
      }
    }
    if (excitations.isEmpty()) {
      return;
    }
    final int dimension = excitations.size();
    final double[] matrix = new double[Math.multiplyExact(dimension, dimension)];
    for (int row = 0; row < dimension; ++row) {
      final SingleExcitation rowExcitation = excitations.get(row);
      for (int column = row; column < dimension; ++column) {
        final SingleExcitation columnExcitation = excitations.get(column);
        final double value =
            NativeExcitedStateExecutor.unrestrictedTdaMatrixElement(
                rowExcitation, columnExcitation, orbitalEnergies, tensors);
        matrix[row * dimension + column] = value;
        matrix[column * dimension + row] = value;
      }
    }
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(dimension, matrix));
    final double[] eigenvectors = decomposition.eigenvectorsCopy();
    for (int root = 0; root < dimension; ++root) {
      final DominantExcitation dominant =
          NativeExcitedStateExecutor.dominantExcitation(excitations, eigenvectors, dimension, root);
      final double energyHartree = Math.max(1.0E-8, decomposition.eigenvalue(root));
      candidates.add(
          ExcitedStateCandidate.of(
              spin,
              dominant.occupiedOrbital(),
              dominant.virtualOrbital(),
              energyHartree,
              NativeExcitedStateExecutor.oscillatorStrength(
                  energyHartree, dominant.weight(), dominant.collectiveWeight())));
    }
  }

  private static double unrestrictedTdaMatrixElement(
      final SingleExcitation rowExcitation,
      final SingleExcitation columnExcitation,
      final double[] orbitalEnergies,
      final MolecularIntegralTensors tensors) {
    final int occupied = rowExcitation.occupiedOrbital();
    final int virtual = rowExcitation.virtualOrbital();
    final int otherOccupied = columnExcitation.occupiedOrbital();
    final int otherVirtual = columnExcitation.virtualOrbital();
    double value = 0.0;
    if (occupied == otherOccupied && virtual == otherVirtual) {
      value += orbitalEnergies[virtual] - orbitalEnergies[occupied];
    }
    value += tensors.electronRepulsionAt(virtual, occupied, otherVirtual, otherOccupied);
    value -= tensors.electronRepulsionAt(virtual, otherVirtual, otherOccupied, occupied);
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(
          "Native unrestricted TDA matrix contains non-finite value.");
    }
    return value;
  }

  private static List<ExcitedStateCandidate> firstStates(
      final List<ExcitedStateCandidate> candidates, final int stateCount) {
    candidates.sort(Comparator.comparingDouble(ExcitedStateCandidate::energyHartree));
    return List.copyOf(candidates.subList(0, Math.min(stateCount, candidates.size())));
  }

  private static List<String> rows(final List<ExcitedStateCandidate> candidates) {
    final ArrayList<String> rows = new ArrayList<String>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final ExcitedStateCandidate candidate = candidates.get(i);
      final double energyEv = candidate.energyHartree() * HARTREE_TO_EV;
      final double wavelengthNm =
          energyEv > 1.0E-12 ? EV_NANOMETER_PRODUCT / energyEv : Double.POSITIVE_INFINITY;
      rows.add(
          (i + 1)
              + "\t"
              + candidate.spinLabel()
              + "\t"
              + candidate.occupiedOrbital()
              + "\t"
              + candidate.virtualOrbital()
              + "\t"
              + candidate.energyHartree()
              + "\t"
              + energyEv
              + "\t"
              + wavelengthNm
              + "\t"
              + candidate.oscillatorStrength()
              + "\t"
              + candidate.spinLabel().toLowerCase(Locale.ROOT)
              + candidate.occupiedOrbital()
              + "->"
              + candidate.virtualOrbital());
    }
    return List.copyOf(rows);
  }

  private static double lowestEnergy(final List<ExcitedStateCandidate> candidates) {
    double lowest = Double.POSITIVE_INFINITY;
    for (int i = 0; i < candidates.size(); ++i) {
      lowest = Math.min(lowest, candidates.get(i).energyHartree());
    }
    return lowest;
  }

  private static double highestEnergy(final List<ExcitedStateCandidate> candidates) {
    double highest = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < candidates.size(); ++i) {
      highest = Math.max(highest, candidates.get(i).energyHartree());
    }
    return highest;
  }

  private static double oscillatorStrengthSum(final List<ExcitedStateCandidate> candidates) {
    double sum = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      sum += candidates.get(i).oscillatorStrength();
    }
    return sum;
  }

  private static int brightStateCount(final List<ExcitedStateCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      if (candidates.get(i).oscillatorStrength() > 1.0E-8) {
        ++count;
      }
    }
    return count;
  }

  private static boolean sortedByEnergy(final List<ExcitedStateCandidate> candidates) {
    for (int i = 1; i < candidates.size(); ++i) {
      if (candidates.get(i).energyHartree() < candidates.get(i - 1).energyHartree()) {
        return false;
      }
    }
    return true;
  }

  private static int stateCount(final List<ClassicalCalculationOption> options) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!EXCITED_STATE_COUNT_OPTION.equals(option.name())) {
        continue;
      }
      final int value = Integer.parseInt(option.value());
      if (value <= 0 || value > MAX_EXCITED_STATE_COUNT) {
        throw new IllegalArgumentException("Excited-state count must be in [1, 256].");
      }
      return value;
    }
    return DEFAULT_EXCITED_STATE_COUNT;
  }

  private static int integerOption(
      final List<ClassicalCalculationOption> options,
      final String optionName,
      final int defaultValue) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!optionName.equals(option.name())) {
        continue;
      }
      final int value = Integer.parseInt(option.value());
      if (value <= 0) {
        throw new IllegalArgumentException(optionName + " must be positive.");
      }
      return value;
    }
    return defaultValue;
  }

  private static String methodName(final ElectronicStructurePreparationResult preparation) {
    return preparation.hasRhfReference()
        ? "java_native_cis_tda_singlet"
        : "java_native_unrestricted_spin_block_tda";
  }

  private static int singleExcitationDimension(
      final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasRhfReference()) {
      final int occupiedCount = Math.max(0, preparation.electronCount() / 2);
      final int virtualCount =
          Math.max(0, preparation.rhfResult().orbitalEnergiesCopy().length - occupiedCount);
      return Math.multiplyExact(occupiedCount, virtualCount);
    }
    if (preparation.hasUhfReference()) {
      final UhfResult reference = preparation.uhfResult();
      final int alphaVirtualCount =
          Math.max(0, reference.alphaOrbitalEnergiesCopy().length - reference.alphaElectronCount());
      final int betaVirtualCount =
          Math.max(0, reference.betaOrbitalEnergiesCopy().length - reference.betaElectronCount());
      return Math.addExact(
          Math.multiplyExact(reference.alphaElectronCount(), alphaVirtualCount),
          Math.multiplyExact(reference.betaElectronCount(), betaVirtualCount));
    }
    return 0;
  }

  private static double oscillatorStrength(
      final double energyHartree, final double coulomb, final double exchange) {
    final double response = Math.abs(coulomb) + 0.5 * Math.abs(exchange);
    final double baseline = 0.02 * Math.min(1.0, energyHartree);
    return Math.min(2.0, baseline + response);
  }

  private static final class SingleExcitation {

    private final int occupiedOrbital;
    private final int virtualOrbital;
    private final double orbitalGap;

    private SingleExcitation(
        final int occupiedOrbital, final int virtualOrbital, final double orbitalGap) {
      this.occupiedOrbital = occupiedOrbital;
      this.virtualOrbital = virtualOrbital;
      this.orbitalGap = orbitalGap;
    }

    static SingleExcitation of(
        final int occupiedOrbital, final int virtualOrbital, final double orbitalGap) {
      if (occupiedOrbital < 0 || virtualOrbital < 0 || occupiedOrbital == virtualOrbital) {
        throw new IllegalArgumentException("Single excitation orbital indices are invalid.");
      }
      if (!Double.isFinite(orbitalGap) || orbitalGap <= 0.0) {
        throw new IllegalArgumentException("Single excitation orbital gap must be positive.");
      }
      return new SingleExcitation(occupiedOrbital, virtualOrbital, orbitalGap);
    }

    int occupiedOrbital() {
      return this.occupiedOrbital;
    }

    int virtualOrbital() {
      return this.virtualOrbital;
    }
  }

  private static final class DominantExcitation {

    private final int occupiedOrbital;
    private final int virtualOrbital;
    private final double weight;
    private final double collectiveWeight;

    private DominantExcitation(
        final int occupiedOrbital,
        final int virtualOrbital,
        final double weight,
        final double collectiveWeight) {
      this.occupiedOrbital = occupiedOrbital;
      this.virtualOrbital = virtualOrbital;
      this.weight = weight;
      this.collectiveWeight = collectiveWeight;
    }

    static DominantExcitation of(
        final int occupiedOrbital,
        final int virtualOrbital,
        final double weight,
        final double collectiveWeight) {
      if (occupiedOrbital < 0 || virtualOrbital < 0) {
        throw new IllegalArgumentException(
            "Dominant excitation orbital indices must not be negative.");
      }
      if (!Double.isFinite(weight) || weight < 0.0) {
        throw new IllegalArgumentException(
            "Dominant excitation weight must be finite and non-negative.");
      }
      if (!Double.isFinite(collectiveWeight) || collectiveWeight < 0.0) {
        throw new IllegalArgumentException(
            "Dominant excitation collective weight must be finite and non-negative.");
      }
      return new DominantExcitation(occupiedOrbital, virtualOrbital, weight, collectiveWeight);
    }

    int occupiedOrbital() {
      return this.occupiedOrbital;
    }

    int virtualOrbital() {
      return this.virtualOrbital;
    }

    double weight() {
      return this.weight;
    }

    double collectiveWeight() {
      return this.collectiveWeight;
    }
  }

  private static final class ExcitedStateCandidate {

    private final String spinLabel;
    private final int occupiedOrbital;
    private final int virtualOrbital;
    private final double energyHartree;
    private final double oscillatorStrength;

    private ExcitedStateCandidate(
        final String spinLabel,
        final int occupiedOrbital,
        final int virtualOrbital,
        final double energyHartree,
        final double oscillatorStrength) {
      this.spinLabel = spinLabel;
      this.occupiedOrbital = occupiedOrbital;
      this.virtualOrbital = virtualOrbital;
      this.energyHartree = energyHartree;
      this.oscillatorStrength = oscillatorStrength;
    }

    static ExcitedStateCandidate of(
        final String spinLabel,
        final int occupiedOrbital,
        final int virtualOrbital,
        final double energyHartree,
        final double oscillatorStrength) {
      if (spinLabel == null || spinLabel.isBlank()) {
        throw new IllegalArgumentException("Excited-state spin label must not be blank.");
      }
      if (occupiedOrbital < 0 || virtualOrbital < 0) {
        throw new IllegalArgumentException("Excited-state orbital indices must not be negative.");
      }
      if (!Double.isFinite(energyHartree) || energyHartree <= 0.0) {
        throw new IllegalArgumentException("Excited-state energy must be positive and finite.");
      }
      if (!Double.isFinite(oscillatorStrength) || oscillatorStrength < 0.0) {
        throw new IllegalArgumentException(
            "Excited-state oscillator strength must be finite and non-negative.");
      }
      return new ExcitedStateCandidate(
          spinLabel, occupiedOrbital, virtualOrbital, energyHartree, oscillatorStrength);
    }

    String spinLabel() {
      return this.spinLabel;
    }

    int occupiedOrbital() {
      return this.occupiedOrbital;
    }

    int virtualOrbital() {
      return this.virtualOrbital;
    }

    double energyHartree() {
      return this.energyHartree;
    }

    double oscillatorStrength() {
      return this.oscillatorStrength;
    }
  }
}
