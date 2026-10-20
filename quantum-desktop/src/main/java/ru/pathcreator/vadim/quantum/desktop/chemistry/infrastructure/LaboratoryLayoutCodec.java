/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.ArrayList;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint3D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingFeature;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ManualDrawingField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;

/** Versioned drawing data within the existing QCHEM extension mechanism. */
final class LaboratoryLayoutCodec {

  static final String KIND = "laboratory-layout-v1";

  private LaboratoryLayoutCodec() {}

  static ChemistryStorageExtension write(final String componentId, final MoleculeDrawing drawing) {
    final ArrayList<String> lines = new ArrayList<>();
    for (final AtomDrawing atom : drawing.atoms()) {
      lines.add(
          "atom "
              + atom.atomId().value()
              + " "
              + atom.point2D().x()
              + " "
              + atom.point2D().y()
              + " "
              + atom.point3D().x()
              + " "
              + atom.point3D().y()
              + " "
              + atom.point3D().z()
              + " "
              + atom.labelVisible());
    }
    for (final BondDrawing bond : drawing.bonds()) {
      lines.add(
          "bond "
              + bond.firstAtomId().value()
              + " "
              + bond.secondAtomId().value()
              + " "
              + bond.style());
    }
    // Manual values remain separate quoted QCHEM properties, including embedded newlines.
    final java.util.LinkedHashMap<String, String> properties = new java.util.LinkedHashMap<>();
    properties.put("molecule", drawing.moleculeId().value());
    properties.put("manualCount", Integer.toString(drawing.manualFields().size()));
    for (int index = 0; index < drawing.manualFields().size(); index++) {
      final ManualDrawingField field = drawing.manualFields().get(index);
      properties.put("feature" + index, field.feature().name());
      properties.put("key" + index, field.key());
      properties.put("value" + index, field.value());
    }
    return ChemistryStorageExtension.of(KIND, componentId, properties, lines);
  }

  static MoleculeDrawing read(final ChemistryStorageExtension extension, final Molecule molecule) {
    if (!molecule.id().value().equals(extension.properties().get("molecule"))) {
      throw new IllegalArgumentException("Layout references a different molecule.");
    }
    final ArrayList<AtomDrawing> atoms = new ArrayList<>();
    final ArrayList<BondDrawing> bonds = new ArrayList<>();
    for (final String line : extension.bodyLines()) {
      final String[] fields = line.split(" ");
      if (fields.length == 8 && fields[0].equals("atom")) {
        if (!fields[7].equals("true") && !fields[7].equals("false")) {
          throw new IllegalArgumentException("Invalid atom label visibility.");
        }
        atoms.add(
            AtomDrawing.of(
                AtomId.of(fields[1]),
                DrawingPoint2D.of(Double.parseDouble(fields[2]), Double.parseDouble(fields[3])),
                DrawingPoint3D.of(
                    Double.parseDouble(fields[4]),
                    Double.parseDouble(fields[5]),
                    Double.parseDouble(fields[6])),
                Boolean.parseBoolean(fields[7])));
      } else if (fields.length == 4 && fields[0].equals("bond")) {
        bonds.add(
            BondDrawing.of(
                AtomId.of(fields[1]), AtomId.of(fields[2]), BondDrawingStyle.valueOf(fields[3])));
      } else {
        throw new IllegalArgumentException("Unknown or damaged layout entry.");
      }
    }
    final Map<String, String> properties = extension.properties();
    final int count = Integer.parseInt(properties.get("manualCount"));
    final ArrayList<ManualDrawingField> manual = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      manual.add(
          ManualDrawingField.of(
              ChemistryDrawingFeature.valueOf(properties.get("feature" + index)),
              properties.get("key" + index),
              properties.get("value" + index)));
    }
    return MoleculeDrawing.of(molecule, atoms, bonds, manual);
  }
}
