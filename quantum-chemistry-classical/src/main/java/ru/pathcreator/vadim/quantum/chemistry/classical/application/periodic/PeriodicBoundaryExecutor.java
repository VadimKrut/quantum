/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.periodic;

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
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Выполняет переносимый анализ периодической ячейки и supercell-постановки. */
public final class PeriodicBoundaryExecutor {

  private static final double DEFAULT_CELL_LENGTH_ANGSTROM = 20.0;
  private static final double DEFAULT_CELL_ANGLE_DEGREES = 90.0;
  private static final double FIT_MARGIN_ANGSTROM = 2.0;
  private static final double BOHR_PER_ANGSTROM = 1.8897261246257702;

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Periodic boundary request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Periodic boundary diagnostics must not be null.");
    }
    final PeriodicBoundaryModel model =
        PeriodicBoundaryModel.fromText(
            PeriodicBoundaryExecutor.optionValue(request.options(), "periodic_model"));
    if (request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return this.moleculeSection(
          (Molecule) request.subject(),
          this.resolveModel(model),
          this.cell(request.options()),
          this.supercell(request.options(), "a"),
          this.supercell(request.options(), "b"),
          this.supercell(request.options(), "c"),
          this.kMesh(request.options(), "a"),
          this.kMesh(request.options(), "b"),
          this.kMesh(request.options(), "c"));
    }
    if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      return this.reactionSection(
          (Reaction) request.subject(), this.resolveModel(model), request.options());
    }
    diagnostics.add(
        ClassicalDiagnostic.error(
            ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
            "Periodic boundary analysis supports molecule and reaction subjects only."));
    return null;
  }

  public PeriodicBoundaryResult evaluateMolecule(
      final Molecule molecule,
      final PeriodicBoundaryModel model,
      final PeriodicCell cell,
      final int supercellA,
      final int supercellB,
      final int supercellC,
      final int kMeshA,
      final int kMeshB,
      final int kMeshC) {
    if (molecule == null) {
      throw new IllegalArgumentException("Periodic boundary molecule must not be null.");
    }
    if (model == null) {
      throw new IllegalArgumentException("Periodic boundary model must not be null.");
    }
    if (cell == null) {
      throw new IllegalArgumentException("Periodic boundary cell must not be null.");
    }
    final long imageCount = (long) supercellA * (long) supercellB * (long) supercellC;
    final long expandedAtomCount = Math.multiplyExact((long) molecule.atomCount(), imageCount);
    final double supercellVolume = cell.volumeAngstrom3() * (double) imageCount;
    final double density = (double) expandedAtomCount / supercellVolume;
    final double minimumImageCutoff = cell.shortestLengthAngstrom() * 0.5;
    return PeriodicBoundaryResult.of(
        model,
        cell,
        supercellA,
        supercellB,
        supercellC,
        expandedAtomCount,
        density,
        minimumImageCutoff,
        this.fitsCell(molecule, cell),
        this.kPointSampling(cell, kMeshA, kMeshB, kMeshC));
  }

  private ClassicalResultSection moleculeSection(
      final Molecule molecule,
      final PeriodicBoundaryModel model,
      final PeriodicCell cell,
      final int supercellA,
      final int supercellB,
      final int supercellC,
      final int kMeshA,
      final int kMeshB,
      final int kMeshC) {
    final PeriodicBoundaryResult result =
        this.evaluateMolecule(
            molecule, model, cell, supercellA, supercellB, supercellC, kMeshA, kMeshB, kMeshC);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "periodic_cell_supercell_metadata_analysis",
        "periodic_boundary_analysis");
    this.addValues(values, "", result);
    return ClassicalResultSection.of(
        ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS, "Periodic boundary analysis", values);
  }

  private ClassicalResultSection reactionSection(
      final Reaction reaction,
      final PeriodicBoundaryModel model,
      final List<ClassicalCalculationOption> options) {
    final PeriodicCell cell = this.cell(options);
    final int supercellA = this.supercell(options, "a");
    final int supercellB = this.supercell(options, "b");
    final int supercellC = this.supercell(options, "c");
    final int kMeshA = this.kMesh(options, "a");
    final int kMeshB = this.kMesh(options, "b");
    final int kMeshC = this.kMesh(options, "c");
    final PeriodicBoundaryResult reactantResult =
        this.evaluateSide(
            reaction.reactants(),
            model,
            cell,
            supercellA,
            supercellB,
            supercellC,
            kMeshA,
            kMeshB,
            kMeshC);
    final PeriodicBoundaryResult productResult =
        this.evaluateSide(
            reaction.products(),
            model,
            cell,
            supercellA,
            supercellB,
            supercellC,
            kMeshA,
            kMeshB,
            kMeshC);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "periodic_reaction_cell_supercell_metadata_analysis",
        "periodic_boundary_analysis");
    this.addValues(values, "reactant_", reactantResult);
    this.addValues(values, "product_", productResult);
    values.add(
        ClassicalResultValue.scalar(
            "delta_expanded_atom_count",
            productResult.expandedAtomCount() - reactantResult.expandedAtomCount(),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "reaction_cell_balanced",
            Boolean.toString(
                productResult.expandedAtomCount() == reactantResult.expandedAtomCount())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS,
        "Reaction periodic boundary analysis",
        values);
  }

  private PeriodicBoundaryResult evaluateSide(
      final ReactionSide side,
      final PeriodicBoundaryModel model,
      final PeriodicCell cell,
      final int supercellA,
      final int supercellB,
      final int supercellC,
      final int kMeshA,
      final int kMeshB,
      final int kMeshC) {
    long atomCount = 0L;
    boolean fitsCell = true;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final PeriodicBoundaryResult result =
          this.evaluateMolecule(
              participant.molecule(),
              model,
              cell,
              supercellA,
              supercellB,
              supercellC,
              kMeshA,
              kMeshB,
              kMeshC);
      atomCount += (long) participant.coefficient().value() * result.expandedAtomCount();
      fitsCell = fitsCell && result.finiteMoleculeFitsCell();
    }
    final long imageCount = (long) supercellA * (long) supercellB * (long) supercellC;
    return PeriodicBoundaryResult.of(
        model,
        cell,
        supercellA,
        supercellB,
        supercellC,
        atomCount,
        (double) atomCount / (cell.volumeAngstrom3() * (double) imageCount),
        cell.shortestLengthAngstrom() * 0.5,
        fitsCell,
        this.kPointSampling(cell, kMeshA, kMeshB, kMeshC));
  }

  private void addValues(
      final List<ClassicalResultValue> values,
      final String prefix,
      final PeriodicBoundaryResult result) {
    final PeriodicCell cell = result.cell();
    values.add(ClassicalResultValue.text(prefix + "model", result.model().name()));
    values.add(ClassicalResultValue.scalar(prefix + "cell_a", cell.aAngstrom(), "angstrom"));
    values.add(ClassicalResultValue.scalar(prefix + "cell_b", cell.bAngstrom(), "angstrom"));
    values.add(ClassicalResultValue.scalar(prefix + "cell_c", cell.cAngstrom(), "angstrom"));
    values.add(ClassicalResultValue.scalar(prefix + "cell_alpha", cell.alphaDegrees(), "degree"));
    values.add(ClassicalResultValue.scalar(prefix + "cell_beta", cell.betaDegrees(), "degree"));
    values.add(ClassicalResultValue.scalar(prefix + "cell_gamma", cell.gammaDegrees(), "degree"));
    values.add(
        ClassicalResultValue.scalar(prefix + "cell_volume", cell.volumeAngstrom3(), "angstrom^3"));
    values.add(ClassicalResultValue.scalar(prefix + "supercell_a", result.supercellA(), "count"));
    values.add(ClassicalResultValue.scalar(prefix + "supercell_b", result.supercellB(), "count"));
    values.add(ClassicalResultValue.scalar(prefix + "supercell_c", result.supercellC(), "count"));
    values.add(ClassicalResultValue.scalar(prefix + "image_count", result.imageCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "expanded_atom_count", result.expandedAtomCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "density_atoms_per_angstrom3",
            result.densityAtomsPerAngstrom3(),
            "atoms/angstrom^3"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "minimum_image_cutoff", result.minimumImageCutoffAngstrom(), "angstrom"));
    values.add(
        ClassicalResultValue.text(
            prefix + "finite_molecule_fits_cell",
            Boolean.toString(result.finiteMoleculeFitsCell())));
    final PeriodicKPointSamplingResult kPointSampling = result.kPointSamplingResult();
    values.add(ClassicalResultValue.scalar(prefix + "k_mesh_a", kPointSampling.meshA(), "count"));
    values.add(ClassicalResultValue.scalar(prefix + "k_mesh_b", kPointSampling.meshB(), "count"));
    values.add(ClassicalResultValue.scalar(prefix + "k_mesh_c", kPointSampling.meshC(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "k_point_count", kPointSampling.kPointCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "reciprocal_a", kPointSampling.reciprocalAInverseAngstrom(), "1/angstrom"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "reciprocal_b", kPointSampling.reciprocalBInverseAngstrom(), "1/angstrom"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "reciprocal_c", kPointSampling.reciprocalCInverseAngstrom(), "1/angstrom"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "free_electron_band_minimum",
            kPointSampling.freeElectronBandMinimumHartree(),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "free_electron_band_maximum",
            kPointSampling.freeElectronBandMaximumHartree(),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            prefix + "free_electron_band_width",
            kPointSampling.freeElectronBandWidthHartree(),
            "hartree"));
  }

  private boolean fitsCell(final Molecule molecule, final PeriodicCell cell) {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Coordinate3D coordinate = atoms.get(i).coordinate();
      if (coordinate == null) {
        return false;
      }
      final double x = coordinate.unit().toAngstrom(coordinate.x());
      final double y = coordinate.unit().toAngstrom(coordinate.y());
      final double z = coordinate.unit().toAngstrom(coordinate.z());
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      minZ = Math.min(minZ, z);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
      maxZ = Math.max(maxZ, z);
    }
    return maxX - minX + FIT_MARGIN_ANGSTROM <= cell.aAngstrom()
        && maxY - minY + FIT_MARGIN_ANGSTROM <= cell.bAngstrom()
        && maxZ - minZ + FIT_MARGIN_ANGSTROM <= cell.cAngstrom();
  }

  private PeriodicCell cell(final List<ClassicalCalculationOption> options) {
    return PeriodicCell.of(
        this.doubleOption(options, "periodic_a_angstrom", DEFAULT_CELL_LENGTH_ANGSTROM),
        this.doubleOption(options, "periodic_b_angstrom", DEFAULT_CELL_LENGTH_ANGSTROM),
        this.doubleOption(options, "periodic_c_angstrom", DEFAULT_CELL_LENGTH_ANGSTROM),
        this.doubleOption(options, "periodic_alpha_degrees", DEFAULT_CELL_ANGLE_DEGREES),
        this.doubleOption(options, "periodic_beta_degrees", DEFAULT_CELL_ANGLE_DEGREES),
        this.doubleOption(options, "periodic_gamma_degrees", DEFAULT_CELL_ANGLE_DEGREES));
  }

  private int supercell(final List<ClassicalCalculationOption> options, final String axis) {
    return Math.max(1, this.intOption(options, "periodic_supercell_" + axis, 1));
  }

  private int kMesh(final List<ClassicalCalculationOption> options, final String axis) {
    return Math.max(1, this.intOption(options, "periodic_k_mesh_" + axis, 1));
  }

  private PeriodicKPointSamplingResult kPointSampling(
      final PeriodicCell cell, final int kMeshA, final int kMeshB, final int kMeshC) {
    final double reciprocalA = 2.0 * Math.PI / cell.aAngstrom();
    final double reciprocalB = 2.0 * Math.PI / cell.bAngstrom();
    final double reciprocalC = 2.0 * Math.PI / cell.cAngstrom();
    double bandMinimum = Double.POSITIVE_INFINITY;
    double bandMaximum = 0.0;
    for (int a = 0; a < kMeshA; ++a) {
      final double ka = PeriodicBoundaryExecutor.centeredKCoordinate(a, kMeshA) * reciprocalA;
      for (int b = 0; b < kMeshB; ++b) {
        final double kb = PeriodicBoundaryExecutor.centeredKCoordinate(b, kMeshB) * reciprocalB;
        for (int c = 0; c < kMeshC; ++c) {
          final double kc = PeriodicBoundaryExecutor.centeredKCoordinate(c, kMeshC) * reciprocalC;
          final double energyHartree =
              0.5
                  * ((ka / BOHR_PER_ANGSTROM) * (ka / BOHR_PER_ANGSTROM)
                      + (kb / BOHR_PER_ANGSTROM) * (kb / BOHR_PER_ANGSTROM)
                      + (kc / BOHR_PER_ANGSTROM) * (kc / BOHR_PER_ANGSTROM));
          bandMinimum = Math.min(bandMinimum, energyHartree);
          bandMaximum = Math.max(bandMaximum, energyHartree);
        }
      }
    }
    return PeriodicKPointSamplingResult.of(
        kMeshA, kMeshB, kMeshC, reciprocalA, reciprocalB, reciprocalC, bandMinimum, bandMaximum);
  }

  private static double centeredKCoordinate(final int index, final int mesh) {
    return ((double) index + 0.5) / (double) mesh - 0.5;
  }

  private PeriodicBoundaryModel resolveModel(final PeriodicBoundaryModel model) {
    if (model == PeriodicBoundaryModel.AUTO) {
      return PeriodicBoundaryModel.MINIMUM_IMAGE;
    }
    return model;
  }

  private double doubleOption(
      final List<ClassicalCalculationOption> options,
      final String name,
      final double defaultValue) {
    final String value = PeriodicBoundaryExecutor.optionValue(options, name);
    if (value.isBlank()) {
      return defaultValue;
    }
    return Double.parseDouble(value);
  }

  private int intOption(
      final List<ClassicalCalculationOption> options, final String name, final int defaultValue) {
    final String value = PeriodicBoundaryExecutor.optionValue(options, name);
    if (value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
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
