/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.validation;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.acidbase.AcidBaseModel;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.kinetics.ReactionKineticProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonianSummary;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicStructureProblem;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.MolecularPropertySet;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionBalance;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.redox.ElectronTransferTransition;
import ru.pathcreator.vadim.quantum.chemistry.domain.redox.RedoxModel;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.MolecularSpectroscopySet;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostate;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotationDirection;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularElectronicConfiguration;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.MolecularThermodynamicData;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ReactionThermodynamicProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.AtomValence;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValence;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValenceAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.ValenceProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.ValenceRule;

public final class ChemistryCoreValidator {

  public ChemistryValidationResult validateMolecule(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecule must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target(ChemistrySubjectKind.MOLECULE.name(), molecule.stableId());
    if (molecule.bondCount() == 0 && molecule.atomCount() > 1) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULE_HAS_NO_BONDS,
              "Molecule contains multiple atoms but has no bonds.",
              target));
    }
    if (!ChemistryCoreValidator.hasCompleteGeometry(molecule)) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULE_HAS_NO_GEOMETRY,
              "Molecule has atoms without 3D coordinates.",
              target));
    }
    ChemistryCoreValidator.validateMoleculeGraph(molecule, diagnostics, target);
    ChemistryCoreValidator.validateStereochemicalContext(molecule, diagnostics, target);
    ChemistryCoreValidator.validateConformationContext(molecule, diagnostics, target);
    ChemistryCoreValidator.validateOpticalRotationContext(molecule, diagnostics, target);
    ChemistryCoreValidator.validateAtomFormalCharges(molecule, diagnostics, target);
    ChemistryCoreValidator.validateRadicalSpin(molecule, diagnostics, target);
    ChemistryCoreValidator.validateElectronicConfiguration(molecule, diagnostics, target);
    ChemistryCoreValidator.validateValence(molecule, diagnostics, target);
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateReaction(final Reaction reaction) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    final ReactionBalance balance = reaction.balance();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target(ChemistrySubjectKind.REACTION.name(), reaction.stableId());
    if (!balance.atomBalanced()) {
      diagnostics.add(
          ChemistryCoreValidator.error(
              ChemistryDiagnosticCode.REACTION_ATOMS_NOT_BALANCED,
              "Reaction atom counts are not balanced.",
              target));
    }
    if (!balance.chargeBalanced()) {
      diagnostics.add(
          ChemistryCoreValidator.error(
              ChemistryDiagnosticCode.REACTION_CHARGE_NOT_BALANCED,
              "Reaction charge is not balanced.",
              target));
    }
    this.validateReactionSideMolecules(reaction.reactants(), diagnostics);
    this.validateReactionSideMolecules(reaction.products(), diagnostics);
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateReactionMechanism(final ReactionMechanism mechanism) {
    if (mechanism == null) {
      throw new IllegalArgumentException("Reaction mechanism must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("REACTION_MECHANISM", mechanism.id());
    diagnostics.addAll(this.validateReaction(mechanism.overallReaction()).diagnostics());
    if (!mechanism.hasEnergyProfile()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.REACTION_MECHANISM_HAS_NO_ENERGY_PROFILE,
              "Reaction mechanism has no energy profile.",
              target));
    }
    this.validateReactionMechanismSteps(mechanism, diagnostics, target);
    ChemistryCoreValidator.validateReactionMechanismEnergyProfile(mechanism, diagnostics, target);
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateReactionThermodynamicProfile(
      final ReactionThermodynamicProfile profile) {
    if (profile == null) {
      throw new IllegalArgumentException("Reaction thermodynamic profile must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("REACTION_THERMODYNAMIC_PROFILE", profile.id());
    diagnostics.addAll(this.validateReaction(profile.reaction()).diagnostics());
    boolean hasMissingGibbs = false;
    boolean hasMissingEntropy = false;
    boolean hasMissingZeroPointEnergy = false;
    final List<MolecularThermodynamicData> molecularData = profile.molecularData();
    for (int i = 0; i < molecularData.size(); ++i) {
      final MolecularThermodynamicData data = molecularData.get(i);
      if (!data.hasGibbsFreeEnergy()) {
        hasMissingGibbs = true;
      }
      if (!data.hasEntropy()) {
        hasMissingEntropy = true;
      }
      if (data.hasZeroPointEnergy()) continue;
      hasMissingZeroPointEnergy = true;
    }
    if (hasMissingGibbs) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.THERMODYNAMIC_PROFILE_HAS_NO_DIRECT_GIBBS_DATA,
              "Thermodynamic profile has no direct Gibbs free energy for every participant.",
              target));
    }
    if (hasMissingEntropy) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.THERMODYNAMIC_PROFILE_HAS_NO_ENTROPY_DATA,
              "Thermodynamic profile has no entropy data for every participant.",
              target));
    }
    if (hasMissingZeroPointEnergy) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.THERMODYNAMIC_PROFILE_HAS_NO_ZERO_POINT_ENERGY_DATA,
              "Thermodynamic profile has no zero-point energy data for every participant.",
              target));
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateReactionKineticProfile(
      final ReactionKineticProfile profile) {
    if (profile == null) {
      throw new IllegalArgumentException("Reaction kinetic profile must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("REACTION_KINETIC_PROFILE", profile.id());
    diagnostics.addAll(this.validateReaction(profile.reaction()).diagnostics());
    if (profile.measurements().isEmpty()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.KINETIC_PROFILE_HAS_NO_MEASUREMENTS,
              "Kinetic profile has no experimental measurements.",
              target));
    }
    for (int i = 0; i < profile.measurements().size(); ++i) {
      if (profile.residualMolePerLiterSecond(i) == 0.0) continue;
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.KINETIC_PROFILE_MEASUREMENT_RESIDUAL_DETECTED,
              "Kinetic profile has measurement residual between observed and predicted rate.",
              target));
      break;
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateMolecularMicrostateSet(final MolecularMicrostateSet set) {
    if (set == null) {
      throw new IllegalArgumentException("Molecular microstate set must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("MOLECULAR_MICROSTATE_SET", set.id());
    final List<MolecularMicrostate> states = set.states();
    for (int i = 0; i < states.size(); ++i) {
      diagnostics.addAll(this.validateMolecule(states.get(i).molecule()).diagnostics());
    }
    if (!set.hasEnvironment() && ChemistryCoreValidator.containsAcidBaseMicrostate(states)) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MICROSTATE_SET_HAS_NO_ENVIRONMENT,
              "Acid-base microstate set has no solution environment.",
              target));
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateAcidBaseModel(final AcidBaseModel model) {
    if (model == null) {
      throw new IllegalArgumentException("Acid-base model must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target = ChemistryCoreValidator.target("ACID_BASE_MODEL", model.id());
    diagnostics.addAll(this.validateMolecularMicrostateSet(model.microstateSet()).diagnostics());
    if (!model.microstateSet().hasEnvironment() || !model.microstateSet().environment().hasPH()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ACID_BASE_MODEL_HAS_NO_PH_CONTEXT,
              "Acid-base model has no pH context for fractions and dominant state estimates.",
              target));
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateRedoxModel(final RedoxModel model) {
    if (model == null) {
      throw new IllegalArgumentException("Redox model must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target = ChemistryCoreValidator.target("REDOX_MODEL", model.id());
    diagnostics.addAll(this.validateMolecularMicrostateSet(model.microstateSet()).diagnostics());
    if (!ChemistryCoreValidator.hasFormalPotential(model.transitions())) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.REDOX_MODEL_HAS_NO_FORMAL_POTENTIALS,
              "Redox model has no formal potentials.",
              target));
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateMolecularPropertySet(final MolecularPropertySet set) {
    if (set == null) {
      throw new IllegalArgumentException("Molecular property set must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("MOLECULAR_PROPERTY_SET", set.id());
    if (!set.hasDipoleMoment()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_NO_DIPOLE_MOMENT,
              "Molecular property set has no dipole moment.",
              target));
    }
    if (set.hasDipoleMoment() && !set.hasPartialChargeModel()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_NO_PARTIAL_CHARGE_MODEL,
              "Molecular property set has a dipole moment but no partial-charge model.",
              target));
    }
    if (set.vibrationalModes().isEmpty()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_NO_VIBRATIONAL_MODES,
              "Molecular property set has no vibrational modes.",
              target));
    }
    if (set.imaginaryFrequencyCount() > 0) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_IMAGINARY_FREQUENCIES,
              "Molecular property set contains imaginary vibrational frequencies.",
              target));
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateMolecularSpectroscopySet(
      final MolecularSpectroscopySet set) {
    if (set == null) {
      throw new IllegalArgumentException("Molecular spectroscopy set must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("MOLECULAR_SPECTROSCOPY_SET", set.id());
    if (set.electronicTransitions().isEmpty()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_ELECTRONIC_TRANSITIONS,
              "Molecular spectroscopy set has no electronic transitions.",
              target));
    }
    if (!set.electronicTransitions().isEmpty() && set.opticallyAllowedTransitionCount() == 0) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_OPTICALLY_ALLOWED_TRANSITIONS,
              "Molecular spectroscopy set has electronic transitions but none is optically"
                  + " allowed.",
              target));
    }
    if (set.nmrChemicalShifts().isEmpty() && set.spinSpinCouplings().isEmpty()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_NMR_OBSERVABLES,
              "Molecular spectroscopy set has no NMR chemical shifts or spin-spin couplings.",
              target));
    }
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateExperiment(final ChemistryExperiment experiment) {
    if (experiment == null) {
      throw new IllegalArgumentException("Chemistry experiment must not be null.");
    }
    ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    if (experiment.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      diagnostics.addAll(this.validateMolecule((Molecule) experiment.subject()).diagnostics());
    } else if (experiment.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      diagnostics.addAll(this.validateReaction((Reaction) experiment.subject()).diagnostics());
    }
    ChemistryCoreValidator.validateTaskSubject(experiment, diagnostics);
    ChemistryCoreValidator.validateActiveSpace(experiment, diagnostics);
    ChemistryCoreValidator.validateActiveSpaceAgainstMolecule(experiment, diagnostics);
    ChemistryCoreValidator.validateMethodAgainstSubject(experiment, diagnostics);
    return ChemistryValidationResult.of(diagnostics);
  }

  public ChemistryValidationResult validateElectronicStructureProblem(
      final ElectronicStructureProblem problem) {
    if (problem == null) {
      throw new IllegalArgumentException("Electronic structure problem must not be null.");
    }
    final ArrayList<ChemistryDiagnostic> diagnostics = new ArrayList<ChemistryDiagnostic>();
    diagnostics.addAll(this.validateMolecule(problem.molecule()).diagnostics());
    ChemistryCoreValidator.validateElectronicProblemTask(problem, diagnostics);
    ChemistryCoreValidator.validateMethodAgainstMolecule(
        problem.molecule(), problem.task().method().spinTreatment(), diagnostics);
    ChemistryCoreValidator.validateElectronicProblemTerms(problem, diagnostics);
    ChemistryCoreValidator.validateElectronicHamiltonianShape(problem, diagnostics);
    ChemistryCoreValidator.validateElectronicProblemOrbitalBasis(problem, diagnostics);
    ChemistryCoreValidator.validateElectronicProblemActiveSpace(problem, diagnostics);
    return ChemistryValidationResult.of(diagnostics);
  }

  private static void validateTaskSubject(
      final ChemistryExperiment experiment, final List<ChemistryDiagnostic> diagnostics) {
    ChemistryDiagnosticTarget subjectTarget =
        ChemistryCoreValidator.target(
            experiment.subject().subjectKind().name(), experiment.subject().stableId());
    if (experiment.task().type() == ChemistryTaskType.REACTION_ENERGY
        && experiment.subject().subjectKind() != ChemistrySubjectKind.REACTION) {
      diagnostics.add(
          ChemistryCoreValidator.error(
              ChemistryDiagnosticCode.REACTION_TASK_REQUIRES_REACTION_SUBJECT,
              "Reaction energy task requires reaction subject.",
              subjectTarget));
    }
    if (experiment.task().type() != ChemistryTaskType.REACTION_ENERGY
        && experiment.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ChemistryCoreValidator.error(
              ChemistryDiagnosticCode.MOLECULE_TASK_REQUIRES_MOLECULE_SUBJECT,
              "Selected task requires molecule subject.",
              subjectTarget));
    }
  }

  private static void validateElectronicProblemTerms(
      final ElectronicStructureProblem problem, final List<ChemistryDiagnostic> diagnostics) {
    if (problem.hasElectronicTerms()) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.error(
            ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_HAS_NO_ELECTRONIC_TERMS,
            "Electronic structure problem has no one-electron or two-electron terms.",
            ChemistryCoreValidator.target("ELECTRONIC_STRUCTURE_PROBLEM", problem.id().value())));
  }

  private static void validateElectronicProblemTask(
      final ElectronicStructureProblem problem, final List<ChemistryDiagnostic> diagnostics) {
    if (problem.task().type() != ChemistryTaskType.REACTION_ENERGY) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.error(
            ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_TASK_REQUIRES_MOLECULE_SCOPE,
            "Electronic structure problem must use a molecule-scoped task.",
            ChemistryCoreValidator.target("ELECTRONIC_STRUCTURE_PROBLEM", problem.id().value())));
  }

  private void validateReactionMechanismSteps(
      final ReactionMechanism mechanism,
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticTarget target) {
    List<ElementaryReactionStep> steps = mechanism.steps();
    for (int i = 0; i < steps.size(); ++i) {
      final ElementaryReactionStep step = steps.get(i);
      diagnostics.addAll(this.validateReaction(step.reaction()).diagnostics());
      if (step.hasTransitionState()) {
        diagnostics.addAll(this.validateMolecule(step.transitionState()).diagnostics());
      } else {
        diagnostics.add(
            ChemistryCoreValidator.warning(
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_TRANSITION_STATE,
                "Reaction mechanism elementary step has no transition state.",
                target));
      }
      if (!step.hasForwardBarrier()) {
        diagnostics.add(
            ChemistryCoreValidator.warning(
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_FORWARD_BARRIER,
                "Reaction mechanism elementary step has no forward barrier.",
                target));
      }
      if (!step.hasReverseBarrier()) {
        diagnostics.add(
            ChemistryCoreValidator.warning(
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_REVERSE_BARRIER,
                "Reaction mechanism elementary step has no reverse barrier.",
                target));
      }
      if (step.hasReactionEnergy()) continue;
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_REACTION_ENERGY,
              "Reaction mechanism elementary step has no reaction energy.",
              target));
    }
  }

  private static void validateReactionMechanismEnergyProfile(
      final ReactionMechanism mechanism,
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticTarget target) {
    int stepTransitionStateCount = 0;
    final List<ElementaryReactionStep> steps = mechanism.steps();
    for (int i = 0; i < steps.size(); ++i) {
      if (!steps.get(i).hasTransitionState()) continue;
      ++stepTransitionStateCount;
    }
    if (mechanism.hasEnergyProfile()
        && mechanism.transitionStateCount() != stepTransitionStateCount) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.REACTION_MECHANISM_ENERGY_PROFILE_TRANSITION_STATE_MISMATCH,
              "Reaction mechanism energy profile transition-state count does not match steps.",
              target));
    }
  }

  private static void validateElectronicHamiltonianShape(
      final ElectronicStructureProblem problem, final List<ChemistryDiagnostic> diagnostics) {
    final ElectronicHamiltonianSummary summary = problem.hamiltonian().summary();
    ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("ELECTRONIC_HAMILTONIAN", problem.id().value());
    if (summary.zeroIntegralTermCount() > 0) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ELECTRONIC_HAMILTONIAN_HAS_ZERO_INTEGRAL_TERMS,
              "Electronic Hamiltonian contains explicitly stored zero integral terms.",
              target));
    }
    if (summary.hasElectronicTerms() && !summary.hasOrbitalCouplings()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ELECTRONIC_HAMILTONIAN_HAS_NO_ORBITAL_COUPLINGS,
              "Electronic Hamiltonian contains no orbital coupling or exchange-like terms.",
              target));
    }
    if (summary.rawSpatialTensorDensity() > 0.75) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ELECTRONIC_HAMILTONIAN_IS_DENSE_TENSOR,
              "Electronic Hamiltonian stores more than 75 percent of raw spatial tensor slots.",
              target));
    }
  }

  private static void validateElectronicProblemOrbitalBasis(
      final ElectronicStructureProblem problem, final List<ChemistryDiagnostic> diagnostics) {
    final ChemistryDiagnosticTarget target =
        ChemistryCoreValidator.target("ELECTRONIC_STRUCTURE_PROBLEM", problem.id().value());
    if (!problem.hasOrbitalBasis()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_HAS_NO_ORBITAL_BASIS,
              "Electronic structure problem has no molecular orbital basis metadata.",
              target));
      return;
    }
    if (problem.orbitalBasis().hasFractionalOccupation()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_HAS_FRACTIONAL_ORBITAL_OCCUPATION,
              "Molecular orbital basis contains fractional orbital occupation.",
              target));
    }
    if (Math.abs(problem.orbitalBasis().occupiedElectronCount()
            - problem.hamiltonian().electronCount())
        > 1.0E-9) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_ORBITAL_OCCUPATION_MISMATCH,
              "Molecular orbital basis occupied electron count does not match Hamiltonian active"
                  + " electron count.",
              target));
    }
  }

  private static void validateElectronicProblemActiveSpace(
      final ElectronicStructureProblem problem, final List<ChemistryDiagnostic> diagnostics) {
    final int moleculeElectronCount = problem.molecule().electronicConfiguration().electronCount();
    if (problem.hamiltonian().electronCount() <= moleculeElectronCount) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.error(
            ChemistryDiagnosticCode.ELECTRONIC_PROBLEM_ACTIVE_SPACE_EXCEEDS_MOLECULE_ELECTRONS,
            "Electronic structure problem active electron count exceeds molecule electron count.",
            ChemistryCoreValidator.target("ELECTRONIC_STRUCTURE_PROBLEM", problem.id().value())));
  }

  private void validateReactionSideMolecules(
      final ReactionSide side, final List<ChemistryDiagnostic> diagnostics) {
    List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      diagnostics.addAll(this.validateMolecule(participants.get(i).molecule()).diagnostics());
    }
  }

  private static void validateActiveSpace(
      final ChemistryExperiment experiment, final List<ChemistryDiagnostic> diagnostics) {
    if (experiment.task().hasActiveSpace()) {
      return;
    }
    final ChemistryDiagnosticTarget subjectTarget =
        ChemistryCoreValidator.target(
            experiment.subject().subjectKind().name(), experiment.subject().stableId());
    diagnostics.add(
        ChemistryCoreValidator.warning(
            ChemistryDiagnosticCode.TASK_HAS_NO_ACTIVE_SPACE,
            "Chemistry task has no active space.",
            subjectTarget));
    if (experiment.executionMode() == ChemistryExecutionMode.QUANTUM_PROGRAM_ONLY
        || experiment.executionMode() == ChemistryExecutionMode.CLASSICAL_AND_QUANTUM) {
      diagnostics.add(
          ChemistryCoreValidator.error(
              ChemistryDiagnosticCode.QUANTUM_ROUTE_REQUIRES_ACTIVE_SPACE,
              "Quantum chemistry program generation requires active space.",
              subjectTarget));
    }
  }

  private static void validateActiveSpaceAgainstMolecule(
      final ChemistryExperiment experiment, final List<ChemistryDiagnostic> diagnostics) {
    if (!experiment.task().hasActiveSpace()) {
      return;
    }
    if (experiment.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      if (experiment.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
        ChemistryCoreValidator.validateActiveSpaceAgainstReaction(experiment, diagnostics);
      }
      return;
    }
    final Molecule molecule = (Molecule) experiment.subject();
    ChemistryCoreValidator.validateActiveSpaceAgainstMolecule(
        molecule, experiment.task().activeSpace().electronCount(), diagnostics);
  }

  private static void validateActiveSpaceAgainstReaction(
      final ChemistryExperiment experiment, final List<ChemistryDiagnostic> diagnostics) {
    Reaction reaction = (Reaction) experiment.subject();
    ChemistryCoreValidator.validateActiveSpaceAgainstReactionSide(
        reaction.reactants(), experiment.task().activeSpace().electronCount(), diagnostics);
    ChemistryCoreValidator.validateActiveSpaceAgainstReactionSide(
        reaction.products(), experiment.task().activeSpace().electronCount(), diagnostics);
  }

  private static void validateActiveSpaceAgainstReactionSide(
      final ReactionSide side, final int activeElectronCount, final List<ChemistryDiagnostic> diagnostics) {
    List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      ChemistryCoreValidator.validateActiveSpaceAgainstMolecule(
          participants.get(i).molecule(), activeElectronCount, diagnostics);
    }
  }

  private static void validateActiveSpaceAgainstMolecule(
      final Molecule molecule, final int activeElectronCount, final List<ChemistryDiagnostic> diagnostics) {
    MolecularElectronicConfiguration configuration = molecule.electronicConfiguration();
    if (activeElectronCount <= configuration.electronCount()) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.error(
            ChemistryDiagnosticCode.ACTIVE_SPACE_HAS_TOO_MANY_ELECTRONS,
            "Active space electron count exceeds molecule electron count.",
            ChemistryCoreValidator.target(
                ChemistrySubjectKind.MOLECULE.name(), molecule.stableId())));
  }

  private static void validateMethodAgainstSubject(
      final ChemistryExperiment experiment, final List<ChemistryDiagnostic> diagnostics) {
    if (experiment.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      ChemistryCoreValidator.validateMethodAgainstReaction(experiment, diagnostics);
      return;
    }
    if (experiment.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return;
    }
    ChemistryCoreValidator.validateMethodAgainstMolecule(
        (Molecule) experiment.subject(), experiment.task().method().spinTreatment(), diagnostics);
  }

  private static void validateMethodAgainstReaction(
      final ChemistryExperiment experiment, final List<ChemistryDiagnostic> diagnostics) {
    final Reaction reaction = (Reaction) experiment.subject();
    ChemistryCoreValidator.validateMethodAgainstReactionSide(
        reaction.reactants(), experiment.task().method().spinTreatment(), diagnostics);
    ChemistryCoreValidator.validateMethodAgainstReactionSide(
        reaction.products(), experiment.task().method().spinTreatment(), diagnostics);
  }

  private static void validateMethodAgainstReactionSide(
      final ReactionSide side,
      final ElectronicStructureSpinTreatment spinTreatment,
      final List<ChemistryDiagnostic> diagnostics) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      ChemistryCoreValidator.validateMethodAgainstMolecule(
          participants.get(i).molecule(), spinTreatment, diagnostics);
    }
  }

  private static void validateMethodAgainstMolecule(
      final Molecule molecule,
      final ElectronicStructureSpinTreatment spinTreatment,
      final List<ChemistryDiagnostic> diagnostics) {
    if (spinTreatment != ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL
        || molecule.electronicConfiguration().closedShell()) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.error(
            ChemistryDiagnosticCode.METHOD_SPIN_TREATMENT_INCOMPATIBLE_WITH_SUBJECT,
            "Restricted closed-shell method is not compatible with open-shell molecule.",
            ChemistryCoreValidator.target(
                ChemistrySubjectKind.MOLECULE.name(), molecule.stableId())));
  }

  private static void validateAtomFormalCharges(
      final Molecule molecule, final List<ChemistryDiagnostic> diagnostics, final ChemistryDiagnosticTarget target) {
    if (!molecule.electronicConfiguration().formalChargesMatchMolecularCharge()) {
      diagnostics.add(
          ChemistryCoreValidator.error(
              ChemistryDiagnosticCode.MOLECULE_CHARGE_DOES_NOT_MATCH_ATOMS,
              "Molecule charge does not match the sum of atom formal charges.",
              target));
    }
  }

  private static void validateMoleculeGraph(
      final Molecule molecule,
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticTarget target) {
    if (molecule.atomCount() <= 1 || molecule.graph().connected()) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.warning(
            ChemistryDiagnosticCode.MOLECULE_GRAPH_IS_DISCONNECTED,
            "Molecule graph contains more than one connected component.",
            target));
  }

  private static void validateStereochemicalContext(
      final Molecule molecule,
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticTarget target) {
    if (molecule.stereochemistry().empty() || ChemistryCoreValidator.hasCompleteGeometry(molecule)) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.warning(
            ChemistryDiagnosticCode.MOLECULE_STEREOCHEMISTRY_NEEDS_GEOMETRY,
            "Molecule has stereochemistry but incomplete 3D geometry.",
            target));
  }

  private static void validateConformationContext(
      final Molecule molecule,
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticTarget target) {
    if (molecule.conformation().empty() || ChemistryCoreValidator.hasCompleteGeometry(molecule)) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.warning(
            ChemistryDiagnosticCode.MOLECULE_CONFORMATION_NEEDS_GEOMETRY,
            "Molecule has conformation data but incomplete 3D geometry.",
            target));
  }

  private static void validateOpticalRotationContext(
      final Molecule molecule,
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticTarget target) {
    if (molecule.opticalRotation().direction() == OpticalRotationDirection.UNKNOWN
        || !molecule.stereochemistry().empty()) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.warning(
            ChemistryDiagnosticCode.MOLECULE_OPTICAL_ROTATION_WITHOUT_STEREOCHEMISTRY,
            "Molecule has optical rotation data but no stereochemical assignment.",
            target));
  }

  private static void validateRadicalSpin(
      final Molecule molecule, final List<ChemistryDiagnostic> diagnostics, final ChemistryDiagnosticTarget target) {
    final MolecularElectronicConfiguration configuration = molecule.electronicConfiguration();
    final int unpairedElectrons = configuration.explicitUnpairedElectronCount();
    if (unpairedElectrons > 0 && molecule.spinMultiplicity().value() == 1) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULE_RADICAL_SPIN_NEEDS_REVIEW,
              "Molecule contains radical atoms but uses singlet spin multiplicity.",
              target));
    }
    if (!configuration.explicitRadicalsCompatibleWithSpin()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULE_RADICAL_SPIN_MULTIPLICITY_MISMATCH,
              "Explicit radical electron count is not compatible with spin multiplicity.",
              target));
    }
  }

  private static void validateElectronicConfiguration(
      final Molecule molecule, final List<ChemistryDiagnostic> diagnostics, final ChemistryDiagnosticTarget target) {
    if (molecule.electronicConfiguration().spinMultiplicityPossible()) {
      return;
    }
    diagnostics.add(
        ChemistryCoreValidator.error(
            ChemistryDiagnosticCode.MOLECULE_SPIN_MULTIPLICITY_INCOMPATIBLE_WITH_ELECTRONS,
            "Molecule spin multiplicity is incompatible with electron count.",
            target));
  }

  private static void validateValence(
      final Molecule molecule, final List<ChemistryDiagnostic> diagnostics, final ChemistryDiagnosticTarget target) {
    final MolecularValence valence = MolecularValenceAnalyzer.analyze(molecule);
    if (valence.hasUnknownBondOrder()) {
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULE_UNKNOWN_BOND_ORDER,
              "Molecule contains bonds with unknown bond order.",
              target));
    }
    final List<AtomValence> atomValences = valence.atomValences();
    for (int i = 0; i < atomValences.size(); ++i) {
      ValenceRule rule;
      final AtomValence atomValence = atomValences.get(i);
      if (!atomValence.exceeds(rule = ValenceProfile.COMMON.ruleFor(atomValence.symbol())))
        continue;
      diagnostics.add(
          ChemistryCoreValidator.warning(
              ChemistryDiagnosticCode.MOLECULE_VALENCE_EXCEEDS_PROFILE,
              "Atom valence exceeds common valence profile.",
              target));
    }
  }

  private static boolean hasCompleteGeometry(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (atoms.get(i).hasCoordinate()) continue;
      return false;
    }
    return true;
  }

  private static boolean containsAcidBaseMicrostate(final List<MolecularMicrostate> states) {
    for (int i = 0; i < states.size(); ++i) {
      final MolecularMicrostateKind kind = states.get(i).kind();
      if (kind != MolecularMicrostateKind.PROTOMER
          && kind != MolecularMicrostateKind.IONIZATION_STATE) continue;
      return true;
    }
    return false;
  }

  private static boolean hasFormalPotential(final List<ElectronTransferTransition> transitions) {
    for (int i = 0; i < transitions.size(); ++i) {
      if (!transitions.get(i).hasFormalPotential()) continue;
      return true;
    }
    return false;
  }

  private static ChemistryDiagnostic warning(
      final ChemistryDiagnosticCode code, final String message, final ChemistryDiagnosticTarget target) {
    return ChemistryDiagnostic.of(ChemistryDiagnosticSeverity.WARNING, code, message, target);
  }

  private static ChemistryDiagnostic error(
      final ChemistryDiagnosticCode code, final String message, final ChemistryDiagnosticTarget target) {
    return ChemistryDiagnostic.of(ChemistryDiagnosticSeverity.ERROR, code, message, target);
  }

  private static ChemistryDiagnosticTarget target(
      final String kind,
      final String id
  ) {
    return ChemistryDiagnosticTarget.of(kind, id);
  }
}