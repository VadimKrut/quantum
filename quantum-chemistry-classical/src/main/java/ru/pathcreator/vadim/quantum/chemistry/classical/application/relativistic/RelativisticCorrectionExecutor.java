/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.relativistic;

import java.util.ArrayList;
import java.util.List;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Выполняет переносимую скалярную релятивистскую оценку для молекул и реакций. */
public final class RelativisticCorrectionExecutor {

  private static final double FINE_STRUCTURE_CONSTANT = 7.2973525693E-3;
  private static final double HARTREE_TO_KILOJOULE_PER_MOLE = 2625.499638;
  private static final int HEAVY_ATOM_THRESHOLD = 18;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Relativistic correction request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Relativistic correction diagnostics must not be null.");
    }
    final RelativisticCorrectionModel model =
        RelativisticCorrectionModel.fromText(
            RelativisticCorrectionExecutor.optionValue(request.options(), "relativistic_model"));
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.RELATIVISTIC_CORRECTION.name()),
            "Relativistic correction is scalar and spin-free; spin-orbit and four-component Dirac effects require backend execution."));
    if (request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return this.moleculeSection((Molecule) request.subject(), model);
    }
    if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      return this.reactionSection((Reaction) request.subject(), model);
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
            "Relativistic correction supports molecule and reaction subjects only."));
    return null;
  }

  public RelativisticCorrectionResult evaluateMolecule(
      final Molecule molecule, final RelativisticCorrectionModel model) {
    if (molecule == null) {
      throw new IllegalArgumentException("Relativistic correction molecule must not be null.");
    }
    if (model == null) {
      throw new IllegalArgumentException("Relativistic correction model must not be null.");
    }
    final List<Atom> atoms = molecule.atoms();
    final ArrayList<RelativisticAtomContribution> contributions =
        new ArrayList<RelativisticAtomContribution>(atoms.size());
    final int electronCount = Math.max(molecule.electronicConfiguration().electronCount(), 1);
    double totalCorrection = 0.0;
    int heavyAtomCount = 0;
    int maxAtomicNumber = 1;
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final int atomicNumber = atom.element().atomicNumber();
      maxAtomicNumber = Math.max(maxAtomicNumber, atomicNumber);
      if (atomicNumber >= HEAVY_ATOM_THRESHOLD) {
        ++heavyAtomCount;
      }
      final double electronWeight = (double) atomicNumber / (double) electronCount;
      final double correction = this.atomCorrectionHartree(atomicNumber, electronWeight, model);
      totalCorrection += correction;
      contributions.add(
          RelativisticAtomContribution.of(
              atom.id().value(),
              atom.element().symbol().value(),
              atomicNumber,
              electronWeight,
              correction));
    }
    return RelativisticCorrectionResult.of(
        model,
        totalCorrection,
        (double) heavyAtomCount / (double) atoms.size(),
        maxAtomicNumber,
        contributions);
  }

  private ClassicalResultSection moleculeSection(
      final Molecule molecule, final RelativisticCorrectionModel model) {
    final RelativisticCorrectionResult result = this.evaluateMolecule(molecule, model);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "scalar_spin_free_relativistic_screening",
        "relativistic_correction",
        "Scalar spin-free screening excludes spin-orbit and four-component relativistic effects.");
    this.addMoleculeValues(values, "", result);
    values.add(
        ClassicalResultValue.of(
            "atom_contribution_rows",
            ClassicalResultValueKind.TABLE,
            "atom,symbol,Z,electron_weight,hartree",
            this.atomRows(result.atomContributions())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.RELATIVISTIC_CORRECTION, "Scalar relativistic correction", values);
  }

  private ClassicalResultSection reactionSection(
      final Reaction reaction, final RelativisticCorrectionModel model) {
    final double reactants = this.sideCorrection(reaction.reactants(), model);
    final double products = this.sideCorrection(reaction.products(), model);
    final double delta = products - reactants;
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "scalar_spin_free_reaction_relativistic_screening",
        "relativistic_correction",
        "Reaction correction is scalar and spin-free; spin-orbit and four-component workflows require a dedicated backend.");
    values.add(ClassicalResultValue.text("model", model.name()));
    values.add(ClassicalResultValue.text("model_scope", "scalar_spin_free_reaction_correction"));
    values.add(ClassicalResultValue.scalar("reactant_correction", reactants, "hartree"));
    values.add(ClassicalResultValue.scalar("product_correction", products, "hartree"));
    values.add(ClassicalResultValue.scalar("delta_correction", delta, "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "delta_correction_kj_per_mol", delta * HARTREE_TO_KILOJOULE_PER_MOLE, "kJ/mol"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.RELATIVISTIC_CORRECTION,
        "Reaction scalar relativistic correction",
        values);
  }

  private void addMoleculeValues(
      final List<ClassicalResultValue> values,
      final String prefix,
      final RelativisticCorrectionResult result) {
    values.add(ClassicalResultValue.text(prefix + "model", result.model().name()));
    values.add(
        ClassicalResultValue.text(prefix + "model_scope", "scalar_spin_free_atomic_screening"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "total_correction", result.totalCorrectionHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "total_correction_kj_per_mol",
            result.totalCorrectionHartree() * HARTREE_TO_KILOJOULE_PER_MOLE,
            "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "heavy_atom_fraction", result.heavyAtomFraction(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "max_atomic_number", result.maxAtomicNumber(), "count"));
  }

  private List<String> atomRows(final List<RelativisticAtomContribution> contributions) {
    final ArrayList<String> rows = new ArrayList<String>(contributions.size());
    for (int i = 0; i < contributions.size(); ++i) {
      final RelativisticAtomContribution contribution = contributions.get(i);
      rows.add(
          contribution.atomId()
              + ","
              + contribution.elementSymbol()
              + ","
              + contribution.atomicNumber()
              + ","
              + contribution.electronWeight()
              + ","
              + contribution.correctionHartree());
    }
    return rows;
  }

  private double sideCorrection(final ReactionSide side, final RelativisticCorrectionModel model) {
    double result = 0.0;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      result +=
          participant.coefficient().value()
              * this.evaluateMolecule(participant.molecule(), model).totalCorrectionHartree();
    }
    return result;
  }

  private double atomCorrectionHartree(
      final int atomicNumber,
      final double electronWeight,
      final RelativisticCorrectionModel model) {
    final double alphaSquared = FINE_STRUCTURE_CONSTANT * FINE_STRUCTURE_CONSTANT;
    final double z = (double) atomicNumber;
    final double shellDamping = 1.0 / Math.sqrt(Math.max(z, 1.0));
    final double base = -0.5 * alphaSquared * z * z * electronWeight * shellDamping;
    if (model == RelativisticCorrectionModel.MASS_VELOCITY_DARWIN) {
      return base;
    }
    if (model == RelativisticCorrectionModel.SCALAR_ZORA) {
      return base * (1.0 + 0.08 * Math.log1p(z));
    }
    if (model == RelativisticCorrectionModel.DKH2_SCALAR) {
      return base * (1.0 + 0.04 * Math.sqrt(z));
    }
    if (atomicNumber >= 54) {
      return base * (1.0 + 0.04 * Math.sqrt(z));
    }
    if (atomicNumber >= HEAVY_ATOM_THRESHOLD) {
      return base * (1.0 + 0.08 * Math.log1p(z));
    }
    return base;
  }

  private static String optionValue(
      final List<ClassicalCalculationOption> options, final String name) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (name.equals(option.name())) {
        return option.value();
      }
    }
    return "";
  }
}
