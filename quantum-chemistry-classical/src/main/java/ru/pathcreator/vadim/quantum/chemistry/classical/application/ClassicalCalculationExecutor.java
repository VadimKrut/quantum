/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation.ConformerSearchExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceSelectionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.HamiltonianPreparationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.OrbitalOptimizationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.PostHartreeFockExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.QuantumProgramPreparationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.QuantumVqeOptimizationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.BondedForceFieldEnergyModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryGradientExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryOptimizationExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.LineSearchGeometryOptimizer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.periodic.PeriodicBoundaryExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionProductPredictionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.property.AdvancedChemistryPropertyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.property.NativeBenchmarkExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.property.NativeMolecularPropertyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.property.PyScfBenchmarkExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.property.PyScfMolecularPropertyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.ReactionEnergeticsExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.ReactionPathExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.relativistic.RelativisticCorrectionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy.HarmonicFrequencyThermodynamicsExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy.MolecularSpectroscopyPredictionExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeature;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularComposition;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionBalance;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Выполняет локальные Java-native classical chemistry расчеты без внешнего backend.
 */
public final class ClassicalCalculationExecutor {

  private final ClassicalSubjectAnalyzer subjectAnalyzer;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final SinglePointEnergyExecutor singlePointEnergyExecutor;
  private final GeometryGradientExecutor geometryGradientExecutor;
  private final GeometryOptimizationExecutor geometryOptimizationExecutor;
  private final ActiveSpaceSelectionExecutor activeSpaceSelectionExecutor;
  private final HamiltonianPreparationExecutor hamiltonianPreparationExecutor;
  private final OrbitalOptimizationExecutor orbitalOptimizationExecutor;
  private final PostHartreeFockExecutor postHartreeFockExecutor;
  private final QuantumProgramPreparationExecutor quantumProgramPreparationExecutor;
  private final QuantumVqeOptimizationExecutor quantumVqeOptimizationExecutor;
  private final ReactionProductPredictionExecutor reactionProductPredictionExecutor;
  private final HarmonicFrequencyThermodynamicsExecutor frequencyThermodynamicsExecutor;
  private final MolecularSpectroscopyPredictionExecutor spectroscopyPredictionExecutor;
  private final ReactionEnergeticsExecutor reactionEnergeticsExecutor;
  private final ReactionPathExecutor reactionPathExecutor;
  private final ConformerSearchExecutor conformerSearchExecutor;
  private final RelativisticCorrectionExecutor relativisticCorrectionExecutor;
  private final PeriodicBoundaryExecutor periodicBoundaryExecutor;
  private final AdvancedChemistryPropertyExecutor advancedChemistryPropertyExecutor;
  private final NativeMolecularPropertyExecutor nativeMolecularPropertyExecutor;
  private final PyScfMolecularPropertyExecutor pyScfMolecularPropertyExecutor;
  private final NativeBenchmarkExecutor nativeBenchmarkExecutor;
  private final PyScfBenchmarkExecutor pyScfBenchmarkExecutor;

  public ClassicalCalculationExecutor() {
    this(new ElectronicStructurePreparationPipeline());
  }

  public ClassicalCalculationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(ClassicalCalculationExecutor.wiring(preparationPipeline));
  }

  private ClassicalCalculationExecutor(final ExecutorWiring wiring) {
    this(
        new ClassicalSubjectAnalyzer(),
        new MolecularFeatureAnalyzer(),
        wiring.singlePointEnergyExecutor(),
        new GeometryGradientExecutor(
            new BondedForceFieldEnergyModel(), wiring.singlePointEnergyExecutor()),
        new GeometryOptimizationExecutor(
            new LineSearchGeometryOptimizer(),
            new BondedForceFieldEnergyModel(),
            wiring.singlePointEnergyExecutor()),
        new ActiveSpaceSelectionExecutor(),
        new HamiltonianPreparationExecutor(wiring.preparationPipeline()),
        new OrbitalOptimizationExecutor(wiring.preparationPipeline()),
        new PostHartreeFockExecutor(wiring.preparationPipeline()),
        new QuantumProgramPreparationExecutor(wiring.preparationPipeline()),
        new QuantumVqeOptimizationExecutor(
            wiring.preparationPipeline(),
            new ru.pathcreator.vadim.quantum.chemistry.fermion.application
                .ElectronicHamiltonianQubitMapper()),
        ClassicalCalculationExecutor.reactionProductPredictionExecutor(wiring),
        new HarmonicFrequencyThermodynamicsExecutor(wiring.singlePointEnergyExecutor()),
        new MolecularSpectroscopyPredictionExecutor(
            new MolecularFeatureAnalyzer(),
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy
                .PyScfExcitedStateExecutor(),
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy
                .NativeExcitedStateExecutor(wiring.preparationPipeline())),
        new ReactionEnergeticsExecutor(wiring.singlePointEnergyExecutor()),
        new ReactionPathExecutor(),
        new ConformerSearchExecutor(),
        new RelativisticCorrectionExecutor(),
        new PeriodicBoundaryExecutor(),
        new AdvancedChemistryPropertyExecutor(
            new ClassicalSubjectAnalyzer(),
            new MolecularFeatureAnalyzer(),
            new QuantumProgramPreparationExecutor(wiring.preparationPipeline())),
        new NativeMolecularPropertyExecutor(wiring.preparationPipeline()),
        new PyScfMolecularPropertyExecutor(),
        new NativeBenchmarkExecutor(
            wiring.singlePointEnergyExecutor(), wiring.preparationPipeline()),
        new PyScfBenchmarkExecutor());
  }

  private static ExecutorWiring wiring(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Classical calculation preparation pipeline must not be null.");
    }
    return new ExecutorWiring(
        preparationPipeline, new SinglePointEnergyExecutor(preparationPipeline));
  }

  private static ReactionProductPredictionExecutor reactionProductPredictionExecutor(
      final ExecutorWiring wiring) {
    return new ReactionProductPredictionExecutor(
        new ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
            .OrganicReactionPredictionService(
            new MolecularFeatureAnalyzer(),
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
                .RuleBasedOrganicReactionCandidateGenerator(
                ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin
                    .BuiltInOrganicReactionRuleLibraries.comprehensiveOrganicProductPrediction()),
            new ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
                .OrganicReactionCandidateScorer(new MolecularFeatureAnalyzer())),
        new MolecularFeatureAnalyzer(),
        new ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
            .ReactionPredictionExplainer(),
        ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin
            .BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry(),
        new ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction
            .ReactionPredictionElectronicReranker(
            new ReactionEnergeticsExecutor(wiring.singlePointEnergyExecutor())),
        new QuantumProgramPreparationExecutor(wiring.preparationPipeline()),
        ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin
            .BuiltInReactionOutcomeCalibrationLibraries.heterocycleProductDistributions());
  }

  public ClassicalCalculationExecutor(
      final ClassicalSubjectAnalyzer subjectAnalyzer,
      final MolecularFeatureAnalyzer featureAnalyzer,
      final SinglePointEnergyExecutor singlePointEnergyExecutor,
      final GeometryGradientExecutor geometryGradientExecutor,
      final GeometryOptimizationExecutor geometryOptimizationExecutor,
      final ActiveSpaceSelectionExecutor activeSpaceSelectionExecutor,
      final HamiltonianPreparationExecutor hamiltonianPreparationExecutor,
      final OrbitalOptimizationExecutor orbitalOptimizationExecutor,
      final PostHartreeFockExecutor postHartreeFockExecutor,
      final QuantumProgramPreparationExecutor quantumProgramPreparationExecutor,
      final QuantumVqeOptimizationExecutor quantumVqeOptimizationExecutor,
      final ReactionProductPredictionExecutor reactionProductPredictionExecutor,
      final HarmonicFrequencyThermodynamicsExecutor frequencyThermodynamicsExecutor,
      final MolecularSpectroscopyPredictionExecutor spectroscopyPredictionExecutor,
      final ReactionEnergeticsExecutor reactionEnergeticsExecutor,
      final ReactionPathExecutor reactionPathExecutor,
      final ConformerSearchExecutor conformerSearchExecutor,
      final RelativisticCorrectionExecutor relativisticCorrectionExecutor,
      final PeriodicBoundaryExecutor periodicBoundaryExecutor,
      final AdvancedChemistryPropertyExecutor advancedChemistryPropertyExecutor,
      final NativeMolecularPropertyExecutor nativeMolecularPropertyExecutor,
      final PyScfMolecularPropertyExecutor pyScfMolecularPropertyExecutor,
      final NativeBenchmarkExecutor nativeBenchmarkExecutor,
      final PyScfBenchmarkExecutor pyScfBenchmarkExecutor) {
    if (subjectAnalyzer == null) {
      throw new IllegalArgumentException("Classical subject analyzer must not be null.");
    }
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Molecular feature analyzer must not be null.");
    }
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException("Single-point energy executor must not be null.");
    }
    if (geometryGradientExecutor == null) {
      throw new IllegalArgumentException("Geometry gradient executor must not be null.");
    }
    if (geometryOptimizationExecutor == null) {
      throw new IllegalArgumentException("Geometry optimization executor must not be null.");
    }
    if (activeSpaceSelectionExecutor == null) {
      throw new IllegalArgumentException("Active-space selection executor must not be null.");
    }
    if (hamiltonianPreparationExecutor == null) {
      throw new IllegalArgumentException("Hamiltonian preparation executor must not be null.");
    }
    if (orbitalOptimizationExecutor == null) {
      throw new IllegalArgumentException("Orbital optimization executor must not be null.");
    }
    if (postHartreeFockExecutor == null) {
      throw new IllegalArgumentException("Post-HF executor must not be null.");
    }
    if (quantumProgramPreparationExecutor == null) {
      throw new IllegalArgumentException("Quantum program preparation executor must not be null.");
    }
    if (quantumVqeOptimizationExecutor == null) {
      throw new IllegalArgumentException("Quantum VQE optimization executor must not be null.");
    }
    if (reactionProductPredictionExecutor == null) {
      throw new IllegalArgumentException("Reaction product prediction executor must not be null.");
    }
    if (frequencyThermodynamicsExecutor == null) {
      throw new IllegalArgumentException("Frequency thermodynamics executor must not be null.");
    }
    if (spectroscopyPredictionExecutor == null) {
      throw new IllegalArgumentException("Spectroscopy prediction executor must not be null.");
    }
    if (reactionEnergeticsExecutor == null) {
      throw new IllegalArgumentException("Reaction energetics executor must not be null.");
    }
    if (reactionPathExecutor == null) {
      throw new IllegalArgumentException("Reaction path executor must not be null.");
    }
    if (conformerSearchExecutor == null) {
      throw new IllegalArgumentException("Conformer search executor must not be null.");
    }
    if (relativisticCorrectionExecutor == null) {
      throw new IllegalArgumentException("Relativistic correction executor must not be null.");
    }
    if (periodicBoundaryExecutor == null) {
      throw new IllegalArgumentException("Periodic boundary executor must not be null.");
    }
    if (advancedChemistryPropertyExecutor == null) {
      throw new IllegalArgumentException("Advanced chemistry property executor must not be null.");
    }
    if (nativeMolecularPropertyExecutor == null) {
      throw new IllegalArgumentException("Native molecular property executor must not be null.");
    }
    if (pyScfMolecularPropertyExecutor == null) {
      throw new IllegalArgumentException("PySCF molecular property executor must not be null.");
    }
    if (nativeBenchmarkExecutor == null) {
      throw new IllegalArgumentException("Native benchmark executor must not be null.");
    }
    if (pyScfBenchmarkExecutor == null) {
      throw new IllegalArgumentException("PySCF benchmark executor must not be null.");
    }
    this.subjectAnalyzer = subjectAnalyzer;
    this.featureAnalyzer = featureAnalyzer;
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    this.geometryGradientExecutor = geometryGradientExecutor;
    this.geometryOptimizationExecutor = geometryOptimizationExecutor;
    this.activeSpaceSelectionExecutor = activeSpaceSelectionExecutor;
    this.hamiltonianPreparationExecutor = hamiltonianPreparationExecutor;
    this.orbitalOptimizationExecutor = orbitalOptimizationExecutor;
    this.postHartreeFockExecutor = postHartreeFockExecutor;
    this.quantumProgramPreparationExecutor = quantumProgramPreparationExecutor;
    this.quantumVqeOptimizationExecutor = quantumVqeOptimizationExecutor;
    this.reactionProductPredictionExecutor = reactionProductPredictionExecutor;
    this.frequencyThermodynamicsExecutor = frequencyThermodynamicsExecutor;
    this.spectroscopyPredictionExecutor = spectroscopyPredictionExecutor;
    this.reactionEnergeticsExecutor = reactionEnergeticsExecutor;
    this.reactionPathExecutor = reactionPathExecutor;
    this.conformerSearchExecutor = conformerSearchExecutor;
    this.relativisticCorrectionExecutor = relativisticCorrectionExecutor;
    this.periodicBoundaryExecutor = periodicBoundaryExecutor;
    this.advancedChemistryPropertyExecutor = advancedChemistryPropertyExecutor;
    this.nativeMolecularPropertyExecutor = nativeMolecularPropertyExecutor;
    this.pyScfMolecularPropertyExecutor = pyScfMolecularPropertyExecutor;
    this.nativeBenchmarkExecutor = nativeBenchmarkExecutor;
    this.pyScfBenchmarkExecutor = pyScfBenchmarkExecutor;
  }

  public ClassicalCalculationResult execute(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Classical calculation request must not be null.");
    }
    final ArrayList<ClassicalResultSection> sections = new ArrayList<ClassicalResultSection>();
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      final var environmentDiagnostics =
          ClassicalEnvironmentOptions.validate(request, kinds.get(i));
      if (!environmentDiagnostics.isEmpty()) {
        diagnostics.addAll(environmentDiagnostics);
        continue;
      }
      this.executeKind(request, kinds.get(i), sections, diagnostics);
    }
    final ClassicalDiagnosticSet diagnosticSet = ClassicalDiagnosticSet.of(diagnostics);
    return ClassicalCalculationResult.of(
        request.id(),
        ClassicalCalculationExecutor.status(sections, diagnosticSet),
        sections,
        diagnosticSet,
        ClassicalCalculationExecutor.coverageReport(request, sections, diagnosticSet));
  }

  public boolean supports(final ClassicalCalculationKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical calculation kind must not be null.");
    }
    return kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
        || kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY
        || kind == ClassicalCalculationKind.ORBITAL_OPTIMIZATION
        || kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        || kind == ClassicalCalculationKind.GEOMETRY_GRADIENT
        || kind == ClassicalCalculationKind.ACTIVE_SPACE_SELECTION
        || PostHartreeFockExecutor.supportsKind(kind)
        || kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        || kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION
        || kind == ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION
        || kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
        || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS
        || kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION
        || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_ENERGY
        || kind == ClassicalCalculationKind.KINETIC_ANALYSIS
        || kind == ClassicalCalculationKind.CONFORMER_SEARCH
        || kind == ClassicalCalculationKind.RELATIVISTIC_CORRECTION
        || kind == ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS
        || kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS
        || kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        || kind == ClassicalCalculationKind.METHOD_BENCHMARK
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH
        || this.advancedChemistryPropertyExecutor.supports(kind);
  }

  private void executeKind(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalResultSection> sections,
      final List<ClassicalDiagnostic> diagnostics) {
    final ChemistrySubjectKind subjectKind = request.subject().subjectKind();
    final boolean moleculeThermodynamics =
        kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS
            && subjectKind == ChemistrySubjectKind.MOLECULE;
    final boolean reactionThermodynamics =
        kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS
            && subjectKind == ChemistrySubjectKind.REACTION;
    if (kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS) {
      sections.add(this.descriptorSection(request.subject()));
      return;
    }
    if (kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY) {
      final ClassicalResultSection section =
          this.singlePointEnergyExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.ORBITAL_OPTIMIZATION) {
      final ClassicalResultSection section =
          this.orbitalOptimizationExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (PostHartreeFockExecutor.supportsKind(kind)) {
      final ClassicalResultSection section =
          this.postHartreeFockExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION) {
      final ClassicalResultSection section =
          this.geometryOptimizationExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_GRADIENT) {
      final ClassicalResultSection section =
          this.geometryGradientExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.ACTIVE_SPACE_SELECTION) {
      final ClassicalResultSection section =
          this.activeSpaceSelectionExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        || kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION) {
      final ClassicalResultSection section =
          this.hamiltonianPreparationExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION) {
      final ClassicalResultSection section =
          this.quantumProgramPreparationExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
      final ClassicalResultSection section =
          this.quantumVqeOptimizationExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION) {
      final ClassicalResultSection section =
          this.reactionProductPredictionExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
        || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || moleculeThermodynamics) {
      final ClassicalResultSection section =
          this.frequencyThermodynamicsExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION
        || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS) {
      final ClassicalResultSection section =
          this.spectroscopyPredictionExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_ENERGY
        || reactionThermodynamics
        || kind == ClassicalCalculationKind.KINETIC_ANALYSIS) {
      final ClassicalResultSection section =
          this.reactionEnergeticsExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.CONFORMER_SEARCH) {
      final ClassicalResultSection section =
          this.conformerSearchExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.RELATIVISTIC_CORRECTION) {
      final ClassicalResultSection section =
          this.relativisticCorrectionExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS) {
      final ClassicalResultSection section =
          this.periodicBoundaryExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS) {
      final ClassicalResultSection section =
          PyScfMolecularPropertyExecutor.pyScfBackendRequested(request)
              ? this.pyScfMolecularPropertyExecutor.execute(request, diagnostics)
              : this.nativeMolecularPropertyExecutor.execute(request, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        || kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
      final ClassicalResultSection section =
          PyScfBenchmarkExecutor.pyScfBackendRequested(request, kind)
              ? this.pyScfBenchmarkExecutor.execute(request, kind, diagnostics)
              : this.nativeBenchmarkExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      final ClassicalResultSection section =
          this.reactionPathExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    if (this.advancedChemistryPropertyExecutor.supports(kind)) {
      final ClassicalResultSection section =
          this.advancedChemistryPropertyExecutor.execute(request, kind, diagnostics);
      if (section != null) {
        sections.add(section);
      }
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "Local Java-native executor has no first-principles implementation for this calculation kind yet."));
  }

  private ClassicalResultSection descriptorSection(final ChemistrySubject subject) {
    if (subject.subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return this.moleculeDescriptorSection((Molecule) subject);
    }
    if (subject.subjectKind() == ChemistrySubjectKind.REACTION) {
      return this.reactionDescriptorSection((Reaction) subject);
    }
    throw new IllegalArgumentException("Unsupported chemistry subject kind for descriptors.");
  }

  private ClassicalResultSection moleculeDescriptorSection(final Molecule molecule) {
    final MolecularFormula formula = molecule.formula();
    final MolecularComposition composition = molecule.composition();
    final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(ClassicalResultValue.text("formula", formula.hillNotation()));
    values.add(ClassicalResultValue.scalar("atom_count", composition.atomCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("heavy_atom_count", composition.heavyAtomCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("hetero_atom_count", composition.heteroAtomCount(), "count"));
    values.add(ClassicalResultValue.scalar("bond_count", molecule.bondCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electron_count", molecule.electronicConfiguration().electronCount(), "count"));
    values.add(ClassicalResultValue.scalar("formal_charge", molecule.charge().value(), "charge"));
    values.add(
        ClassicalResultValue.scalar(
            "spin_multiplicity", molecule.spinMultiplicity().value(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "molecular_mass", composition.mass().averageAtomicMass(), "Da"));
    values.add(
        ClassicalResultValue.text(
            "features", ClassicalCalculationExecutor.featureSummary(features)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.DESCRIPTOR_ANALYSIS, "Molecular descriptors", values);
  }

  private ClassicalResultSection reactionDescriptorSection(final Reaction reaction) {
    final ClassicalSubjectSize size = this.subjectAnalyzer.analyze(reaction);
    final ReactionBalance balance = reaction.balance();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(ClassicalResultValue.scalar("participant_count", size.participantCount(), "count"));
    values.add(ClassicalResultValue.scalar("atom_count", size.atomCount(), "count"));
    values.add(ClassicalResultValue.scalar("bond_count", size.bondCount(), "count"));
    values.add(ClassicalResultValue.scalar("electron_count", size.electronCount(), "count"));
    values.add(
        ClassicalResultValue.text("atom_balanced", Boolean.toString(balance.atomBalanced())));
    values.add(
        ClassicalResultValue.text("charge_balanced", Boolean.toString(balance.chargeBalanced())));
    values.add(
        ClassicalResultValue.text(
            "mass_number_balanced", Boolean.toString(balance.massNumberBalanced())));
    values.add(
        ClassicalResultValue.text("reactant_formulas", this.formulaSummary(reaction.reactants())));
    values.add(
        ClassicalResultValue.text("product_formulas", this.formulaSummary(reaction.products())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.DESCRIPTOR_ANALYSIS, "Reaction descriptors", values);
  }

  private String formulaSummary(final ReactionSide side) {
    final StringBuilder builder = new StringBuilder();
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (i > 0) {
        builder.append(" + ");
      }
      final ReactionParticipant participant = participants.get(i);
      builder.append(participant.coefficient().value());
      builder.append("*");
      builder.append(participant.molecule().formula().hillNotation());
    }
    return builder.toString();
  }

  private static String featureSummary(final MolecularFeatureSet features) {
    final StringBuilder builder = new StringBuilder();
    final List<MolecularFeature> items = features.features();
    for (int i = 0; i < items.size(); ++i) {
      final MolecularFeature feature = items.get(i);
      if (!feature.present()) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(feature.kind().name());
      builder.append("=");
      builder.append(feature.count());
    }
    if (builder.length() == 0) {
      return "none";
    }
    return builder.toString();
  }

  private static ClassicalCalculationStatus status(
      final List<ClassicalResultSection> sections, final ClassicalDiagnosticSet diagnostics) {
    if (diagnostics.hasErrors()) {
      return sections.isEmpty()
          ? ClassicalCalculationStatus.FAILED
          : ClassicalCalculationStatus.PARTIAL;
    }
    if (sections.isEmpty()) {
      return ClassicalCalculationStatus.UNSUPPORTED;
    }
    if (ClassicalCalculationExecutor.hasPartialDiagnostic(diagnostics)) {
      return ClassicalCalculationStatus.PARTIAL;
    }
    return ClassicalCalculationStatus.SUCCEEDED;
  }

  private static boolean hasPartialDiagnostic(final ClassicalDiagnosticSet diagnostics) {
    final List<ClassicalDiagnostic> diagnosticList = diagnostics.diagnostics();
    for (int i = 0; i < diagnosticList.size(); ++i) {
      if (ClassicalCalculationExecutor.partialDiagnostic(diagnosticList.get(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean partialDiagnostic(final ClassicalDiagnostic diagnostic) {
    if (diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL) {
      return "calculationKind".equals(diagnostic.target().kind())
          || "method".equals(diagnostic.target().kind())
          || "functional".equals(diagnostic.target().kind());
    }
    if (diagnostic.code() != ClassicalDiagnosticCode.APPROXIMATION_REQUIRED) {
      return false;
    }
    if ("method".equals(diagnostic.target().kind())) {
      return true;
    }
    if (ClassicalCalculationExecutor.activeSpacePreparationDiagnostic(diagnostic)) {
      return false;
    }
    return "calculationKind".equals(diagnostic.target().kind());
  }

  private static boolean activeSpacePreparationDiagnostic(final ClassicalDiagnostic diagnostic) {
    final String message = diagnostic.message();
    return message.contains("Active space was selected automatically")
        || message.contains("Reference SCF orbitals are used for active-space selection")
        || message.contains("Direct SCF orbital reference is used");
  }

  private static ClassicalCalculationCoverageReport coverageReport(
      final ClassicalCalculationRequest request,
      final List<ClassicalResultSection> sections,
      final ClassicalDiagnosticSet diagnostics) {
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    final ArrayList<ClassicalCalculationCoverageItem> items =
        new ArrayList<ClassicalCalculationCoverageItem>(kinds.size());
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      final ClassicalResultSection section =
          ClassicalCalculationExecutor.firstSectionOrNull(sections, kind);
      final ArrayList<ClassicalDiagnostic> itemDiagnostics =
          ClassicalCalculationExecutor.diagnosticsForKind(diagnostics, kind);
      if (section != null && ClassicalCalculationExecutor.partialBenchmarkRows(kind, section)) {
        itemDiagnostics.add(
            ClassicalDiagnostic.warning(
                ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
                ClassicalDiagnosticTarget.of("coverage", kind.name()),
                "Java-native benchmark coverage is mixed because at least one benchmark row is unavailable."));
      }
      final ClassicalCoverageStatus status =
          ClassicalCalculationExecutor.coverageStatus(kind, section, itemDiagnostics);
      items.add(
          ClassicalCalculationCoverageItem.of(
              kind,
              status,
              ClassicalCalculationExecutor.coverageModel(kind, section, itemDiagnostics),
              itemDiagnostics));
    }
    return ClassicalCalculationCoverageReport.of(items);
  }

  private static ClassicalResultSection firstSectionOrNull(
      final List<ClassicalResultSection> sections, final ClassicalCalculationKind kind) {
    for (int i = 0; i < sections.size(); ++i) {
      final ClassicalResultSection section = sections.get(i);
      if (section.kind() == kind) {
        return section;
      }
    }
    return null;
  }

  private static ArrayList<ClassicalDiagnostic> diagnosticsForKind(
      final ClassicalDiagnosticSet diagnostics, final ClassicalCalculationKind kind) {
    final ArrayList<ClassicalDiagnostic> result = new ArrayList<ClassicalDiagnostic>();
    final List<ClassicalDiagnostic> diagnosticList = diagnostics.diagnostics();
    for (int i = 0; i < diagnosticList.size(); ++i) {
      final ClassicalDiagnostic diagnostic = diagnosticList.get(i);
      if (ClassicalCalculationExecutor.diagnosticAppliesToKind(diagnostic, kind)) {
        result.add(diagnostic);
      }
    }
    return result;
  }

  private static boolean diagnosticAppliesToKind(
      final ClassicalDiagnostic diagnostic, final ClassicalCalculationKind kind) {
    if ("calculationKind".equals(diagnostic.target().kind())) {
      return kind.name().equals(diagnostic.target().id());
    }
    if (kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY
        && ("method".equals(diagnostic.target().kind())
            || "functional".equals(diagnostic.target().kind()))) {
      return true;
    }
    return diagnostic.code() == ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED
        && kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY;
  }

  private static ClassicalCoverageStatus coverageStatus(
      final ClassicalCalculationKind kind,
      final ClassicalResultSection section,
      final List<ClassicalDiagnostic> diagnostics) {
    if (section == null) {
      return ClassicalCoverageStatus.UNSUPPORTED;
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      final ClassicalDiagnostic diagnostic = diagnostics.get(i);
      if (diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL
          || diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED) {
        return ClassicalCoverageStatus.APPROXIMATE;
      }
    }
    if (ClassicalCalculationExecutor.partialBenchmarkRows(kind, section)) {
      return ClassicalCoverageStatus.APPROXIMATE;
    }
    return ClassicalCoverageStatus.EXACT;
  }

  private static boolean partialBenchmarkRows(
      final ClassicalCalculationKind kind, final ClassicalResultSection section) {
    if (kind != ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        && kind != ClassicalCalculationKind.METHOD_BENCHMARK) {
      return false;
    }
    final List<ClassicalResultValue> values = section.values();
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalResultValue value = values.get(i);
      final String name = value.name();
      if (!name.endsWith("_rows")) {
        continue;
      }
      final List<String> rows = value.values();
      for (int rowIndex = 0; rowIndex < rows.size(); ++rowIndex) {
        if (rows.get(rowIndex).contains("unavailable")) {
          return true;
        }
      }
    }
    return false;
  }

  private static String coverageModel(
      final ClassicalCalculationKind kind,
      final ClassicalResultSection section,
      final List<ClassicalDiagnostic> diagnostics) {
    if (section == null) {
      final String diagnosticModel =
          ClassicalCalculationExecutor.diagnosticCoverageModel(diagnostics);
      if (!diagnosticModel.isBlank()) {
        return diagnosticModel;
      }
      return "not_executed_" + kind.name().toLowerCase(java.util.Locale.ROOT);
    }
    return section.title();
  }

  private static String diagnosticCoverageModel(final List<ClassicalDiagnostic> diagnostics) {
    for (int i = 0; i < diagnostics.size(); ++i) {
      final ClassicalDiagnostic diagnostic = diagnostics.get(i);
      if (diagnostic.code() == ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED
          && diagnostic.message().contains("open-shell CCSD")) {
        return "open_shell_coupled_cluster_requires_spin_orbital_solver";
      }
      if (diagnostic.code() == ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED
          && diagnostic
              .message()
              .contains(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION.name())) {
        return "quantum_backend_execution_required";
      }
    }
    return "";
  }

  private static final class ExecutorWiring {

    private final ElectronicStructurePreparationPipeline preparationPipeline;
    private final SinglePointEnergyExecutor singlePointEnergyExecutor;

    private ExecutorWiring(
        final ElectronicStructurePreparationPipeline preparationPipeline,
        final SinglePointEnergyExecutor singlePointEnergyExecutor) {
      this.preparationPipeline = preparationPipeline;
      this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    }

    private ElectronicStructurePreparationPipeline preparationPipeline() {
      return this.preparationPipeline;
    }

    private SinglePointEnergyExecutor singlePointEnergyExecutor() {
      return this.singlePointEnergyExecutor;
    }
  }
}
