/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SemiEmpiricalValenceEnergyModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryCoordinateSupport;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Выполняет локальный дискретный поиск конформеров по вращаемым одинарным связям молекулы. */
public final class ConformerSearchExecutor {

  private static final double HARTREE_TO_KCAL_PER_MOL = 627.5094740631;
  private static final double BOLTZMANN_KCAL_PER_MOL_KELVIN = 0.00198720425864083;
  private static final double DEFAULT_TEMPERATURE_KELVIN = 298.15;
  private static final double[] DEFAULT_SCAN_ANGLES_DEGREES =
      new double[] {-180.0, -120.0, -60.0, 0.0, 60.0, 120.0, 180.0};

  private final SemiEmpiricalValenceEnergyModel energyModel;

  public ConformerSearchExecutor() {
    this(new SemiEmpiricalValenceEnergyModel());
  }

  public ConformerSearchExecutor(final SemiEmpiricalValenceEnergyModel energyModel) {
    if (energyModel == null) {
      throw new IllegalArgumentException("Conformer search energy model must not be null.");
    }
    this.energyModel = energyModel;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Conformer search request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Conformer search diagnostics must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Conformer search requires molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    final String coordinateError = ConformerSearchExecutor.coordinateError(molecule);
    if (coordinateError != null) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of("molecule", molecule.id().value()),
              coordinateError));
      return null;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.CONFORMER_SEARCH.name()),
            "Conformer search uses deterministic torsion scan with Java-native semiempirical conformer rescoring."));
    final List<RotatableTorsion> torsions = ConformerSearchExecutor.rotatableTorsions(molecule);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "discrete_torsion_semiempirical_scan",
        "conformer_search",
        "Conformer search uses deterministic torsion grids and semi-empirical rescoring, not exhaustive continuous conformational sampling.");
    values.add(ClassicalResultValue.text("method", "discrete_torsion_semiempirical_scan"));
    values.add(ClassicalResultValue.text("energy_model", "native_valence_graph_semiempirical"));
    values.add(ClassicalResultValue.scalar("rotatable_bond_count", torsions.size(), "count"));
    values.add(
        ClassicalResultValue.scalar("angle_count", DEFAULT_SCAN_ANGLES_DEGREES.length, "count"));
    values.add(
        ClassicalResultValue.of(
            "conformer_rows",
            ClassicalResultValueKind.TABLE,
            "relative_score",
            this.conformerRows(molecule, torsions)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.CONFORMER_SEARCH, "Local Java-native conformer search", values);
  }

  private List<String> conformerRows(
      final Molecule molecule, final List<RotatableTorsion> torsions) {
    final ArrayList<String> rows =
        new ArrayList<String>(
            Math.max(1, torsions.size() * DEFAULT_SCAN_ANGLES_DEGREES.length + 1));
    rows.add(
        "rank\tbond\tatoms\tangle_degrees\trelative_score\ttorsion_score\tsteric_score\telectronic_energy_hartree\trelative_energy_hartree\trelative_energy_kcal_mol\tboltzmann_weight");
    if (torsions.isEmpty()) {
      final double energy = this.energyModel.evaluate(molecule).totalEnergyHartree();
      rows.add("1\tnone\tnone\t0.0\t0.0\t0.0\t0.0\t" + energy + "\t0.0\t0.0\t1.0");
      return List.copyOf(rows);
    }
    final ArrayList<ConformerCandidate> candidates =
        new ArrayList<ConformerCandidate>(torsions.size() * DEFAULT_SCAN_ANGLES_DEGREES.length);
    for (int torsionIndex = 0; torsionIndex < torsions.size(); ++torsionIndex) {
      final RotatableTorsion torsion = torsions.get(torsionIndex);
      for (int angleIndex = 0; angleIndex < DEFAULT_SCAN_ANGLES_DEGREES.length; ++angleIndex) {
        final double angle = DEFAULT_SCAN_ANGLES_DEGREES[angleIndex];
        final double stericScore = ConformerSearchExecutor.stericScore(molecule, torsion, angle);
        final double torsionScore = ConformerSearchExecutor.torsionPreferenceScore(angle);
        final Molecule conformer =
            GeometryCoordinateSupport.withCoordinatesAngstrom(
                molecule, ConformerSearchExecutor.rotatedCoordinates(molecule, torsion, angle));
        final double electronicEnergy = this.energyModel.evaluate(conformer).totalEnergyHartree();
        candidates.add(
            ConformerCandidate.of(
                torsion,
                angle,
                torsionScore + stericScore + electronicEnergy,
                torsionScore,
                stericScore,
                electronicEnergy));
      }
    }
    final List<ConformerCandidate> ranked =
        ConformerSearchExecutor.withThermodynamicRanking(candidates);
    final double bestScore = ranked.get(0).score();
    for (int i = 0; i < ranked.size(); ++i) {
      final ConformerCandidate candidate = ranked.get(i);
      rows.add(
          i
              + 1
              + "\t"
              + candidate.torsion().bondName()
              + "\t"
              + candidate.torsion().atomPathName()
              + "\t"
              + candidate.angleDegrees()
              + "\t"
              + (candidate.score() - bestScore)
              + "\t"
              + candidate.torsionScore()
              + "\t"
              + candidate.stericScore()
              + "\t"
              + candidate.electronicEnergyHartree()
              + "\t"
              + candidate.relativeEnergyHartree()
              + "\t"
              + candidate.relativeEnergyHartree() * HARTREE_TO_KCAL_PER_MOL
              + "\t"
              + candidate.boltzmannWeight());
    }
    return List.copyOf(rows);
  }

  private static List<ConformerCandidate> withThermodynamicRanking(
      final List<ConformerCandidate> candidates) {
    candidates.sort(ConformerCandidate::compareByScore);
    double minEnergy = Double.POSITIVE_INFINITY;
    for (int i = 0; i < candidates.size(); ++i) {
      minEnergy = Math.min(minEnergy, candidates.get(i).electronicEnergyHartree());
    }
    double partition = 0.0;
    final double[] weights = new double[candidates.size()];
    for (int i = 0; i < candidates.size(); ++i) {
      final double relativeKcal =
          (candidates.get(i).electronicEnergyHartree() - minEnergy) * HARTREE_TO_KCAL_PER_MOL;
      final double exponent =
          -relativeKcal / (BOLTZMANN_KCAL_PER_MOL_KELVIN * DEFAULT_TEMPERATURE_KELVIN);
      weights[i] = Math.exp(Math.max(-745.0, exponent));
      partition += weights[i];
    }
    final ArrayList<ConformerCandidate> ranked =
        new ArrayList<ConformerCandidate>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      ranked.add(
          candidates
              .get(i)
              .withThermodynamicRanking(
                  candidates.get(i).electronicEnergyHartree() - minEnergy, weights[i] / partition));
    }
    ranked.sort(ConformerCandidate::compareByScore);
    return List.copyOf(ranked);
  }

  private static double torsionPreferenceScore(final double angleDegrees) {
    final double radians = Math.toRadians(angleDegrees);
    return 0.5 * (1.0 + Math.cos(3.0 * radians));
  }

  private static double stericScore(
      final Molecule molecule, final RotatableTorsion torsion, final double angleDegrees) {
    double score = 0.0;
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (torsion.referencesCentralAtom(atoms.get(i).id())) {
        continue;
      }
      final double distance =
          ConformerSearchExecutor.distanceAngstrom(atoms.get(i), torsion.oppositeOuterAtom());
      final double phase = Math.abs(Math.sin(Math.toRadians(angleDegrees)));
      score += phase / Math.max(0.35, distance * distance);
    }
    return score;
  }

  private static List<RotatableTorsion> rotatableTorsions(final Molecule molecule) {
    final List<Bond> bonds = molecule.bonds();
    final ArrayList<RotatableTorsion> torsions = new ArrayList<RotatableTorsion>();
    final Map<AtomId, Atom> atomById = ConformerSearchExecutor.atomById(molecule.atoms());
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE) {
        continue;
      }
      final AtomId left = bond.firstAtomId();
      final AtomId right = bond.secondAtomId();
      final AtomId leftNeighbor = ConformerSearchExecutor.firstNeighborExcept(bonds, left, right);
      final AtomId rightNeighbor = ConformerSearchExecutor.firstNeighborExcept(bonds, right, left);
      if (leftNeighbor == null || rightNeighbor == null) {
        continue;
      }
      torsions.add(
          RotatableTorsion.of(
              atomById.get(leftNeighbor),
              atomById.get(left),
              atomById.get(right),
              atomById.get(rightNeighbor)));
    }
    return List.copyOf(torsions);
  }

  private static double[] rotatedCoordinates(
      final Molecule molecule, final RotatableTorsion torsion, final double angleDegrees) {
    final double[] coordinates = GeometryCoordinateSupport.coordinatesAngstrom(molecule);
    final List<Atom> atoms = molecule.atoms();
    final Map<AtomId, Integer> atomIndexById = ConformerSearchExecutor.atomIndexById(atoms);
    final Set<AtomId> rotatedAtomIds =
        ConformerSearchExecutor.rotatedAtomIds(
            molecule.bonds(), torsion.secondCentralAtom().id(), torsion.firstCentralAtom().id());
    final int axisStartOffset = atomIndexById.get(torsion.firstCentralAtom().id()).intValue() * 3;
    final int axisEndOffset = atomIndexById.get(torsion.secondCentralAtom().id()).intValue() * 3;
    final double axisX = coordinates[axisEndOffset] - coordinates[axisStartOffset];
    final double axisY = coordinates[axisEndOffset + 1] - coordinates[axisStartOffset + 1];
    final double axisZ = coordinates[axisEndOffset + 2] - coordinates[axisStartOffset + 2];
    final double axisNorm = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
    if (axisNorm == 0.0) {
      return coordinates;
    }
    final double unitX = axisX / axisNorm;
    final double unitY = axisY / axisNorm;
    final double unitZ = axisZ / axisNorm;
    final double radians = Math.toRadians(angleDegrees);
    final double cos = Math.cos(radians);
    final double sin = Math.sin(radians);
    for (int i = 0; i < atoms.size(); ++i) {
      if (!rotatedAtomIds.contains(atoms.get(i).id())) {
        continue;
      }
      final int offset = i * 3;
      final double relativeX = coordinates[offset] - coordinates[axisStartOffset];
      final double relativeY = coordinates[offset + 1] - coordinates[axisStartOffset + 1];
      final double relativeZ = coordinates[offset + 2] - coordinates[axisStartOffset + 2];
      final double dot = unitX * relativeX + unitY * relativeY + unitZ * relativeZ;
      final double crossX = unitY * relativeZ - unitZ * relativeY;
      final double crossY = unitZ * relativeX - unitX * relativeZ;
      final double crossZ = unitX * relativeY - unitY * relativeX;
      coordinates[offset] =
          coordinates[axisStartOffset] + relativeX * cos + crossX * sin + unitX * dot * (1.0 - cos);
      coordinates[offset + 1] =
          coordinates[axisStartOffset + 1]
              + relativeY * cos
              + crossY * sin
              + unitY * dot * (1.0 - cos);
      coordinates[offset + 2] =
          coordinates[axisStartOffset + 2]
              + relativeZ * cos
              + crossZ * sin
              + unitZ * dot * (1.0 - cos);
    }
    return coordinates;
  }

  private static Set<AtomId> rotatedAtomIds(
      final List<Bond> bonds, final AtomId start, final AtomId blocked) {
    final HashSet<AtomId> visited = new HashSet<AtomId>();
    final ArrayDeque<AtomId> queue = new ArrayDeque<AtomId>();
    visited.add(blocked);
    visited.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      final AtomId current = queue.removeFirst();
      for (int i = 0; i < bonds.size(); ++i) {
        final Bond bond = bonds.get(i);
        final AtomId next;
        if (bond.firstAtomId().equals(current)) {
          next = bond.secondAtomId();
        } else if (bond.secondAtomId().equals(current)) {
          next = bond.firstAtomId();
        } else {
          continue;
        }
        if (visited.contains(next)) {
          continue;
        }
        visited.add(next);
        queue.add(next);
      }
    }
    visited.remove(blocked);
    return visited;
  }

  private static Map<AtomId, Integer> atomIndexById(final List<Atom> atoms) {
    final HashMap<AtomId, Integer> result = new HashMap<AtomId, Integer>(atoms.size() * 2);
    for (int i = 0; i < atoms.size(); ++i) {
      result.put(atoms.get(i).id(), Integer.valueOf(i));
    }
    return result;
  }

  private static AtomId firstNeighborExcept(
      final List<Bond> bonds, final AtomId atomId, final AtomId excludedAtomId) {
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.firstAtomId().equals(atomId) && !bond.secondAtomId().equals(excludedAtomId)) {
        return bond.secondAtomId();
      }
      if (bond.secondAtomId().equals(atomId) && !bond.firstAtomId().equals(excludedAtomId)) {
        return bond.firstAtomId();
      }
    }
    return null;
  }

  private static Map<AtomId, Atom> atomById(final List<Atom> atoms) {
    final HashMap<AtomId, Atom> result = new HashMap<AtomId, Atom>(atoms.size() * 2);
    for (int i = 0; i < atoms.size(); ++i) {
      result.put(atoms.get(i).id(), atoms.get(i));
    }
    return result;
  }

  private static String coordinateError(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).hasCoordinate()) {
        return "Conformer search requires coordinates for atom " + atoms.get(i).id().value() + ".";
      }
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      if (!bonds.get(i).type().hasDefinedOrder()) {
        return "Conformer search requires defined bond orders; unknown bond found.";
      }
    }
    return null;
  }

  private static double distanceAngstrom(final Atom first, final Atom second) {
    final double dx =
        first.coordinate().unit().toAngstrom(first.coordinate().x())
            - second.coordinate().unit().toAngstrom(second.coordinate().x());
    final double dy =
        first.coordinate().unit().toAngstrom(first.coordinate().y())
            - second.coordinate().unit().toAngstrom(second.coordinate().y());
    final double dz =
        first.coordinate().unit().toAngstrom(first.coordinate().z())
            - second.coordinate().unit().toAngstrom(second.coordinate().z());
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
