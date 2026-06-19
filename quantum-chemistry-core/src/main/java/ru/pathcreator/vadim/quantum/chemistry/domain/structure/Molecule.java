/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.structure;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularComposition;
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularCompositionAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraph;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValence;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValenceAnalyzer;

/**
 * Полная доменная модель молекулы: атомы, связи, заряд, spin, stereo, conformation и metadata.
 */
public final class Molecule implements ChemistrySubject {

  private final MoleculeId id;
  private final String displayName;
  private final List<Atom> atoms;
  private final List<Bond> bonds;
  private final MolecularCharge charge;
  private final SpinMultiplicity spinMultiplicity;
  private final Stereochemistry stereochemistry;
  private final MolecularConformation conformation;
  private final OpticalRotation opticalRotation;
  private final MolecularSymmetry symmetry;
  private final ChemistryMetadata metadata;

  private Molecule(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final Stereochemistry stereochemistry,
      final MolecularConformation conformation,
      final OpticalRotation opticalRotation,
      final MolecularSymmetry symmetry,
      final ChemistryMetadata metadata) {
    this.id = id;
    this.displayName = displayName;
    this.atoms = atoms;
    this.bonds = bonds;
    this.charge = charge;
    this.spinMultiplicity = spinMultiplicity;
    this.stereochemistry = stereochemistry;
    this.conformation = conformation;
    this.opticalRotation = opticalRotation;
    this.symmetry = symmetry;
    this.metadata = metadata;
  }

  public static Molecule of(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity) {
    return Molecule.of(
        id,
        displayName,
        atoms,
        bonds,
        charge,
        spinMultiplicity,
        Stereochemistry.EMPTY,
        MolecularConformation.EMPTY,
        OpticalRotation.UNKNOWN,
        MolecularSymmetry.C1,
        ChemistryMetadata.EMPTY);
  }

  public static Molecule of(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final ChemistryMetadata metadata) {
    return Molecule.of(
        id,
        displayName,
        atoms,
        bonds,
        charge,
        spinMultiplicity,
        Stereochemistry.EMPTY,
        MolecularConformation.EMPTY,
        OpticalRotation.UNKNOWN,
        MolecularSymmetry.C1,
        metadata);
  }

  public static Molecule of(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final Stereochemistry stereochemistry,
      final ChemistryMetadata metadata) {
    return Molecule.of(
        id,
        displayName,
        atoms,
        bonds,
        charge,
        spinMultiplicity,
        stereochemistry,
        MolecularConformation.EMPTY,
        OpticalRotation.UNKNOWN,
        MolecularSymmetry.C1,
        metadata);
  }

  public static Molecule of(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final Stereochemistry stereochemistry,
      final MolecularConformation conformation,
      final ChemistryMetadata metadata) {
    return Molecule.of(
        id,
        displayName,
        atoms,
        bonds,
        charge,
        spinMultiplicity,
        stereochemistry,
        conformation,
        OpticalRotation.UNKNOWN,
        MolecularSymmetry.C1,
        metadata);
  }

  public static Molecule of(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final Stereochemistry stereochemistry,
      final MolecularConformation conformation,
      final OpticalRotation opticalRotation,
      final ChemistryMetadata metadata) {
    return Molecule.of(
        id,
        displayName,
        atoms,
        bonds,
        charge,
        spinMultiplicity,
        stereochemistry,
        conformation,
        opticalRotation,
        MolecularSymmetry.C1,
        metadata);
  }

  public static Molecule of(
      final MoleculeId id,
      final String displayName,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final Stereochemistry stereochemistry,
      final MolecularConformation conformation,
      final OpticalRotation opticalRotation,
      final MolecularSymmetry symmetry,
      final ChemistryMetadata metadata) {
    if (id == null) {
      throw new IllegalArgumentException("Molecule id must not be null.");
    }
    final String checkedName = TextValue.requireText(displayName, "Molecule display name");
    final List<Atom> checkedAtoms = List.copyOf(Molecule.requireAtoms(atoms));
    final List<Bond> checkedBonds = List.copyOf(Molecule.requireBonds(bonds, checkedAtoms));
    final MolecularCharge checkedCharge = charge == null ? MolecularCharge.NEUTRAL : charge;
    final SpinMultiplicity checkedSpin =
        spinMultiplicity == null ? SpinMultiplicity.SINGLET : spinMultiplicity;
    final Stereochemistry checkedStereochemistry =
        Molecule.requireStereochemistry(stereochemistry, checkedAtoms, checkedBonds);
    final MolecularConformation checkedConformation =
        Molecule.requireConformation(conformation, checkedAtoms, checkedBonds);
    final OpticalRotation checkedOpticalRotation =
        opticalRotation == null ? OpticalRotation.UNKNOWN : opticalRotation;
    final MolecularSymmetry checkedSymmetry = symmetry == null ? MolecularSymmetry.C1 : symmetry;
    final ChemistryMetadata checkedMetadata = metadata == null ? ChemistryMetadata.EMPTY : metadata;
    return new Molecule(
        id,
        checkedName,
        checkedAtoms,
        checkedBonds,
        checkedCharge,
        checkedSpin,
        checkedStereochemistry,
        checkedConformation,
        checkedOpticalRotation,
        checkedSymmetry,
        checkedMetadata);
  }

  public MoleculeId id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public List<Atom> atoms() {
    return this.atoms;
  }

  public List<Bond> bonds() {
    return this.bonds;
  }

  public MolecularCharge charge() {
    return this.charge;
  }

  public SpinMultiplicity spinMultiplicity() {
    return this.spinMultiplicity;
  }

  public Stereochemistry stereochemistry() {
    return this.stereochemistry;
  }

  public MolecularConformation conformation() {
    return this.conformation;
  }

  public OpticalRotation opticalRotation() {
    return this.opticalRotation;
  }

  public MolecularSymmetry symmetry() {
    return this.symmetry;
  }

  public ChemistryMetadata metadata() {
    return this.metadata;
  }

  public MolecularElectronicConfiguration electronicConfiguration() {
    return MolecularElectronicConfiguration.of(this.atoms, this.charge, this.spinMultiplicity);
  }

  public MolecularFormula formula() {
    return MolecularFormula.fromMolecule(this);
  }

  public MolecularComposition composition() {
    return MolecularCompositionAnalyzer.analyze(this);
  }

  public MolecularGraph graph() {
    return MolecularGraphAnalyzer.analyze(this);
  }

  public MolecularValence valence() {
    return MolecularValenceAnalyzer.analyze(this);
  }

  public int atomCount() {
    return this.atoms.size();
  }

  public int bondCount() {
    return this.bonds.size();
  }

  @Override
  public ChemistrySubjectKind subjectKind() {
    return ChemistrySubjectKind.MOLECULE;
  }

  @Override
  public String stableId() {
    return this.id.value();
  }

  private static List<Atom> requireAtoms(final List<Atom> atoms) {
    if (atoms == null) {
      throw new IllegalArgumentException("Molecule atoms must not be null.");
    }
    if (atoms.isEmpty()) {
      throw new IllegalArgumentException("Molecule must contain at least one atom.");
    }
    final HashSet<AtomId> ids = new HashSet<AtomId>();
    for (int i = 0; i < atoms.size(); ++i) {
      Atom atom = atoms.get(i);
      if (atom == null) {
        throw new IllegalArgumentException("Molecule atom must not be null.");
      }
      if (ids.add(atom.id())) continue;
      throw new IllegalArgumentException("Molecule contains duplicate atom id.");
    }
    return atoms;
  }

  private static List<Bond> requireBonds(
      final List<Bond> bonds,
      final List<Atom> atoms
  ) {
    int i;
    if (bonds == null) {
      return List.of();
    }
    HashSet<AtomId> atomIds = new HashSet<AtomId>();
    for (i = 0; i < atoms.size(); ++i) {
      atomIds.add(atoms.get(i).id());
    }
    final HashSet<String> bondEndpointKeys = new HashSet<String>();
    for (i = 0; i < bonds.size(); ++i) {
      Bond bond = bonds.get(i);
      if (bond == null) {
        throw new IllegalArgumentException("Molecule bond must not be null.");
      }
      if (!atomIds.contains(bond.firstAtomId()) || !atomIds.contains(bond.secondAtomId())) {
        throw new IllegalArgumentException("Molecule bond references an unknown atom.");
      }
      if (bondEndpointKeys.add(bond.canonicalEndpointKey())) {
        continue;
      }
      throw new IllegalArgumentException("Molecule contains duplicate bond.");
    }
    return bonds;
  }

  private static Stereochemistry requireStereochemistry(
      final Stereochemistry stereochemistry, final List<Atom> atoms, final List<Bond> bonds) {
    if (stereochemistry == null) {
      return Stereochemistry.EMPTY;
    }
    if (stereochemistry.empty()) {
      return Stereochemistry.EMPTY;
    }
    HashSet<AtomId> atomIds = new HashSet<AtomId>();
    for (int i = 0; i < atoms.size(); ++i) {
      atomIds.add(atoms.get(i).id());
    }
    final List<Stereocenter> centers = stereochemistry.centers();
    for (int i = 0; i < centers.size(); ++i) {
      final Stereocenter center = centers.get(i);
      if (!atomIds.contains(center.primaryAtomId())) {
        throw new IllegalArgumentException(
            "Molecule stereocenter references an unknown primary atom.");
      }
      if (center.hasSecondaryAtom() && !atomIds.contains(center.secondaryAtomId())) {
        throw new IllegalArgumentException(
            "Molecule stereocenter references an unknown secondary atom.");
      }
      List<AtomId> referenceAtomIds = center.referenceAtomIds();
      for (int j = 0; j < referenceAtomIds.size(); ++j) {
        if (atomIds.contains(referenceAtomIds.get(j))) continue;
        throw new IllegalArgumentException(
            "Molecule stereocenter references an unknown reference atom.");
      }
      Molecule.validateStereocenterAgainstBonds(center, bonds);
    }
    return stereochemistry;
  }

  private static void validateStereocenterAgainstBonds(
      final Stereocenter center,
      final List<Bond> bonds
  ) {
    switch (center.kind()) {
      case TETRAHEDRAL_ATOM:
        {
          Molecule.validateTetrahedralStereocenter(center, bonds);
          break;
        }
      case DOUBLE_BOND:
        {
          Molecule.validateDoubleBondStereocenter(center, bonds);
          break;
        }
      case AXIAL:
        {
          Molecule.validateAxialStereocenter(center, bonds);
          break;
        }
      case HELICAL:
        {
          Molecule.validateHelicalStereocenter(center, bonds);
          break;
        }
      case PLANAR:
        {
          Molecule.validatePlanarStereocenter(center, bonds);
        }
    }
  }

  private static void validateTetrahedralStereocenter(
      final Stereocenter center,
      final List<Bond> bonds
  ) {
    List<AtomId> referenceAtomIds = center.referenceAtomIds();
    for (int i = 0; i < referenceAtomIds.size(); ++i) {
      if (Molecule.hasBond(bonds, center.primaryAtomId(), referenceAtomIds.get(i))) continue;
      throw new IllegalArgumentException(
          "Tetrahedral stereocenter reference atom is not bonded to center.");
    }
  }

  private static void validateAxialStereocenter(
      final Stereocenter center,
      final List<Bond> bonds
  ) {
    if (!Molecule.connectedByBonds(bonds, center.primaryAtomId(), center.secondaryAtomId())) {
      throw new IllegalArgumentException(
          "Axial stereocenter axis atoms must belong to one bonded component.");
    }
    if (!Molecule.hasBond(bonds, center.primaryAtomId(), center.referenceAtomIds().get(0))) {
      throw new IllegalArgumentException(
          "Axial stereocenter first reference atom is not bonded to axis.");
    }
    if (!Molecule.hasBond(bonds, center.secondaryAtomId(), center.referenceAtomIds().get(1))) {
      throw new IllegalArgumentException(
          "Axial stereocenter second reference atom is not bonded to axis.");
    }
  }

  private static void validateHelicalStereocenter(
      final Stereocenter center,
      final List<Bond> bonds
  ) {
    AtomId previousAtomId = center.primaryAtomId();
    List<AtomId> referenceAtomIds = center.referenceAtomIds();
    for (int i = 0; i < referenceAtomIds.size(); ++i) {
      final AtomId referenceAtomId = referenceAtomIds.get(i);
      if (!Molecule.hasBond(bonds, previousAtomId, referenceAtomId)) {
        throw new IllegalArgumentException(
            "Helical stereocenter reference atoms must form a bonded path.");
      }
      previousAtomId = referenceAtomId;
    }
    if (!Molecule.hasBond(bonds, previousAtomId, center.secondaryAtomId())) {
      throw new IllegalArgumentException(
          "Helical stereocenter terminal atom must close the bonded path.");
    }
  }

  private static void validatePlanarStereocenter(
      final Stereocenter center,
      final List<Bond> bonds
  ) {
    final List<AtomId> referenceAtomIds = center.referenceAtomIds();
    for (int i = 0; i < referenceAtomIds.size(); ++i) {
      if (Molecule.hasBond(bonds, center.primaryAtomId(), referenceAtomIds.get(i))) continue;
      throw new IllegalArgumentException(
          "Planar stereocenter reference atom is not bonded to center.");
    }
  }

  private static void validateDoubleBondStereocenter(
      final Stereocenter center,
      final List<Bond> bonds
  ) {
    if (!Molecule.hasBondOfType(
        bonds, center.primaryAtomId(), center.secondaryAtomId(), BondType.DOUBLE)) {
      throw new IllegalArgumentException(
          "Double-bond stereocenter must reference an existing double bond.");
    }
    if (!Molecule.hasBond(bonds, center.primaryAtomId(), center.referenceAtomIds().get(0))) {
      throw new IllegalArgumentException(
          "Double-bond stereocenter first reference atom is not bonded.");
    }
    if (!Molecule.hasBond(bonds, center.secondaryAtomId(), center.referenceAtomIds().get(1))) {
      throw new IllegalArgumentException(
          "Double-bond stereocenter second reference atom is not bonded.");
    }
  }

  private static boolean hasBond(
      final List<Bond> bonds,
      final AtomId firstAtomId,
      final AtomId secondAtomId
  ) {
    for (int i = 0; i < bonds.size(); ++i) {
      if (!bonds.get(i).connects(firstAtomId, secondAtomId)) continue;
      return true;
    }
    return false;
  }

  private static boolean hasBondOfType(
      final List<Bond> bonds, final AtomId firstAtomId, final AtomId secondAtomId, final BondType type) {
    for (int i = 0; i < bonds.size(); ++i) {
      Bond bond = bonds.get(i);
      if (!bond.connects(firstAtomId, secondAtomId) || bond.type() != type) continue;
      return true;
    }
    return false;
  }

  private static boolean connectedByBonds(
      final List<Bond> bonds, final AtomId firstAtomId, final AtomId secondAtomId) {
    final HashSet<AtomId> visited = new HashSet<AtomId>();
    final HashSet<AtomId> frontier = new HashSet<AtomId>();
    frontier.add(firstAtomId);
    while (!frontier.isEmpty()) {
      final AtomId currentAtomId = Molecule.removeFirst(frontier);
      if (currentAtomId.equals(secondAtomId)) {
        return true;
      }
      if (!visited.add(currentAtomId)) continue;
      Molecule.addBondedNeighbors(bonds, currentAtomId, visited, frontier);
    }
    return false;
  }

  private static AtomId removeFirst(final Set<AtomId> atomIds) {
    if (atomIds.isEmpty()) {
      throw new IllegalStateException("Atom id set must not be empty.");
    }
    final AtomId atomId = atomIds.iterator().next();
    atomIds.remove(atomId);
    return atomId;
  }

  private static void addBondedNeighbors(
      final List<Bond> bonds, final AtomId atomId, final Set<AtomId> visited, final Set<AtomId> frontier) {
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.firstAtomId().equals(atomId)) {
        Molecule.addFrontierAtom(bond.secondAtomId(), visited, frontier);
        continue;
      }
      if (!bond.secondAtomId().equals(atomId)) continue;
      Molecule.addFrontierAtom(bond.firstAtomId(), visited, frontier);
    }
  }

  private static void addFrontierAtom(
      final AtomId atomId,
      final Set<AtomId> visited,
      final Set<AtomId> frontier
  ) {
    if (!visited.contains(atomId)) {
      frontier.add(atomId);
    }
  }

  private static MolecularConformation requireConformation(
      final MolecularConformation conformation, final List<Atom> atoms, final List<Bond> bonds) {
    if (conformation == null) {
      return MolecularConformation.EMPTY;
    }
    if (conformation.empty()) {
      return MolecularConformation.EMPTY;
    }
    final HashSet<AtomId> atomIds = new HashSet<AtomId>();
    for (int i = 0; i < atoms.size(); ++i) {
      atomIds.add(atoms.get(i).id());
    }
    final List<TorsionAngle> torsionAngles = conformation.torsionAngles();
    for (int i = 0; i < torsionAngles.size(); ++i) {
      final TorsionAngle angle = torsionAngles.get(i);
      if (!(atomIds.contains(angle.firstAtomId())
          && atomIds.contains(angle.secondAtomId())
          && atomIds.contains(angle.thirdAtomId())
          && atomIds.contains(angle.fourthAtomId()))) {
        throw new IllegalArgumentException("Molecule torsion angle references an unknown atom.");
      }
      if (Molecule.hasTorsionBondPath(bonds, angle)) continue;
      throw new IllegalArgumentException("Molecule torsion angle atoms must form a bonded path.");
    }
    return conformation;
  }

  private static boolean hasTorsionBondPath(
      final List<Bond> bonds,
      final TorsionAngle angle
  ) {
    return Molecule.hasBond(bonds, angle.firstAtomId(), angle.secondAtomId())
        && Molecule.hasBond(bonds, angle.secondAtomId(), angle.thirdAtomId())
        && Molecule.hasBond(bonds, angle.thirdAtomId(), angle.fourthAtomId());
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Molecule)) {
      return false;
    }
    final Molecule molecule = (Molecule) other;
    return Objects.equals(this.id, molecule.id)
        && Objects.equals(this.displayName, molecule.displayName)
        && Objects.equals(this.atoms, molecule.atoms)
        && Objects.equals(this.bonds, molecule.bonds)
        && Objects.equals(this.charge, molecule.charge)
        && Objects.equals(this.spinMultiplicity, molecule.spinMultiplicity)
        && Objects.equals(this.stereochemistry, molecule.stereochemistry)
        && Objects.equals(this.conformation, molecule.conformation)
        && Objects.equals(this.opticalRotation, molecule.opticalRotation)
        && Objects.equals(this.symmetry, molecule.symmetry)
        && Objects.equals(this.metadata, molecule.metadata);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.displayName);
    result = ChemistryHash.include(result, this.atoms);
    result = ChemistryHash.include(result, this.bonds);
    result = ChemistryHash.include(result, this.charge);
    result = ChemistryHash.include(result, this.spinMultiplicity);
    result = ChemistryHash.include(result, this.stereochemistry);
    result = ChemistryHash.include(result, this.conformation);
    result = ChemistryHash.include(result, this.opticalRotation);
    result = ChemistryHash.include(result, this.symmetry);
    result = ChemistryHash.include(result, this.metadata);
    return result;
  }
}