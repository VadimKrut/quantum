/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.draft;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint3D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ManualDrawingField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/**
 * Mutable draft для графического и ручного создания молекулы перед фиксацией в immutable core.
 */
public final class MoleculeDraft {

  private final MoleculeId moleculeId;
  private final String displayName;
  private final ArrayList<MoleculeDraftAtom> atoms;
  private final ArrayList<MoleculeDraftBond> bonds;
  private final ArrayList<ManualDrawingField> manualFields;
  private MolecularCharge charge;
  private SpinMultiplicity spinMultiplicity;
  private Stereochemistry stereochemistry;
  private MolecularConformation conformation;
  private OpticalRotation opticalRotation;
  private MolecularSymmetry symmetry;
  private ChemistryMetadata metadata;

  private MoleculeDraft(
      final MoleculeId moleculeId,
      final String displayName
  ) {
    this.moleculeId = moleculeId;
    this.displayName = displayName;
    this.atoms = new ArrayList<MoleculeDraftAtom>();
    this.bonds = new ArrayList<MoleculeDraftBond>();
    this.manualFields = new ArrayList<ManualDrawingField>();
    this.charge = MolecularCharge.NEUTRAL;
    this.spinMultiplicity = SpinMultiplicity.SINGLET;
    this.stereochemistry = Stereochemistry.EMPTY;
    this.conformation = MolecularConformation.EMPTY;
    this.opticalRotation = OpticalRotation.UNKNOWN;
    this.symmetry = MolecularSymmetry.C1;
    this.metadata = ChemistryMetadata.EMPTY;
  }

  public static MoleculeDraft create(
      final String moleculeId,
      final String displayName
  ) {
    return new MoleculeDraft(
        MoleculeId.of(moleculeId),
        MoleculeDraft.requireText(displayName, "Molecule draft display name")
    );
  }

  public MoleculeDraft addAtom(final MoleculeDraftAtom atom) {
    if (atom == null) {
      throw new IllegalArgumentException("Draft atom must not be null.");
    }
    this.requireUniqueAtom(atom.atomId());
    this.atoms.add(atom);
    return this;
  }

  public MoleculeDraft addBond(final MoleculeDraftBond bond) {
    if (bond == null) {
      throw new IllegalArgumentException("Draft bond must not be null.");
    }
    this.requireKnownAtom(bond.firstAtomId());
    this.requireKnownAtom(bond.secondAtomId());
    this.bonds.add(bond);
    return this;
  }

  public MoleculeDraft addManualField(final ManualDrawingField field) {
    if (field == null) {
      throw new IllegalArgumentException("Manual field must not be null.");
    }
    this.manualFields.add(field);
    return this;
  }

  public MoleculeDraft richCoreFields(
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity,
      final Stereochemistry stereochemistry,
      final MolecularConformation conformation,
      final OpticalRotation opticalRotation,
      final MolecularSymmetry symmetry,
      final ChemistryMetadata metadata
  ) {
    this.charge = charge == null ? MolecularCharge.NEUTRAL : charge;
    this.spinMultiplicity = spinMultiplicity == null ? SpinMultiplicity.SINGLET : spinMultiplicity;
    this.stereochemistry = stereochemistry == null ? Stereochemistry.EMPTY : stereochemistry;
    this.conformation = conformation == null ? MolecularConformation.EMPTY : conformation;
    this.opticalRotation = opticalRotation == null ? OpticalRotation.UNKNOWN : opticalRotation;
    this.symmetry = symmetry == null ? MolecularSymmetry.C1 : symmetry;
    this.metadata = metadata == null ? ChemistryMetadata.EMPTY : metadata;
    return this;
  }

  public MoleculeDraft moveAtom2D(
      final AtomId atomId,
      final double deltaX,
      final double deltaY
  ) {
    for (int i = 0; i < this.atoms.size(); ++i) {
      final MoleculeDraftAtom atom = this.atoms.get(i);
      if (!atom.atomId().equals(atomId)) {
        continue;
      }
      this.atoms.set(
          i,
          atom.withPoint2D(atom.point2D().translate(deltaX, deltaY))
      );
      return this;
    }
    throw new IllegalArgumentException("Draft atom not found.");
  }

  public MoleculeDraft rotate3D(
      final double yawRadians,
      final double pitchRadians,
      final double rollRadians
  ) {
    for (int i = 0; i < this.atoms.size(); ++i) {
      final MoleculeDraftAtom atom = this.atoms.get(i);
      this.atoms.set(
          i,
          atom.withPoint3D(atom.point3D().rotate(yawRadians, pitchRadians, rollRadians))
      );
    }
    return this;
  }

  public MoleculeDraft mirror2DX(final double axisX) {
    for (int i = 0; i < this.atoms.size(); ++i) {
      final MoleculeDraftAtom atom = this.atoms.get(i);
      this.atoms.set(
          i,
          atom.withPoint2D(atom.point2D().mirrorX(axisX))
      );
    }
    return this;
  }

  public Molecule toMolecule(final LengthUnit unit) {
    final ArrayList<Atom> moleculeAtoms = new ArrayList<Atom>(this.atoms.size());
    for (int i = 0; i < this.atoms.size(); ++i) {
      moleculeAtoms.add(this.atoms.get(i).toAtom(unit));
    }
    final ArrayList<Bond> moleculeBonds = new ArrayList<Bond>(this.bonds.size());
    for (int i = 0; i < this.bonds.size(); ++i) {
      moleculeBonds.add(this.bonds.get(i).toBond());
    }
    return Molecule.of(
        this.moleculeId,
        this.displayName,
        List.copyOf(moleculeAtoms),
        List.copyOf(moleculeBonds),
        this.charge,
        this.spinMultiplicity,
        this.stereochemistry,
        this.conformation,
        this.opticalRotation,
        this.symmetry,
        this.metadata
    );
  }

  public MoleculeDrawing toDrawing(final LengthUnit unit) {
    final ArrayList<AtomDrawing> atomDrawings = new ArrayList<AtomDrawing>(this.atoms.size());
    for (int i = 0; i < this.atoms.size(); ++i) {
      atomDrawings.add(this.atoms.get(i).toDrawing());
    }
    final ArrayList<BondDrawing> bondDrawings = new ArrayList<BondDrawing>(this.bonds.size());
    for (int i = 0; i < this.bonds.size(); ++i) {
      bondDrawings.add(this.bonds.get(i).toDrawing());
    }
    return MoleculeDrawing.of(
        this.toMolecule(unit),
        List.copyOf(atomDrawings),
        List.copyOf(bondDrawings),
        List.copyOf(this.manualFields)
    );
  }

  private void requireUniqueAtom(final AtomId atomId) {
    for (int i = 0; i < this.atoms.size(); ++i) {
      if (!this.atoms.get(i).atomId().equals(atomId)) {
        continue;
      }
      throw new IllegalArgumentException("Draft atom id is duplicated.");
    }
  }

  private void requireKnownAtom(final AtomId atomId) {
    for (int i = 0; i < this.atoms.size(); ++i) {
      if (this.atoms.get(i).atomId().equals(atomId)) {
        return;
      }
    }
    throw new IllegalArgumentException("Draft bond references unknown atom.");
  }

  private static String requireText(
      final String value,
      final String subject
  ) {
    if (value == null) {
      throw new IllegalArgumentException(subject + " must not be null.");
    }
    final String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(subject + " must not be blank.");
    }
    return trimmed;
  }
}