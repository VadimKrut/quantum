/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api.geometry;

import java.util.HashMap;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryCoordinateSupport;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.MoleculeGeometryRegularizer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Checked adapters for the existing geometry preparation and calculation executors. */
public final class ChemistryGeometrySupport {
  public static final String INITIAL_METHOD_VERSION = MoleculeGeometryRegularizer.ALGORITHM_VERSION;

  private ChemistryGeometrySupport() {}

  public static Molecule prepareInitial(final Molecule molecule) {
    final var result = MoleculeGeometryRegularizer.regularizeForElectronicPreparation(molecule);
    if (result.regularized()
        && (!molecule.stereochemistry().empty() || !molecule.conformation().empty())) {
      throw new IllegalArgumentException(
          "Начальная раскладка не поддерживает заданные стереоцентры и торсионные ограничения. Задайте согласованные 3D-координаты вручную или импортируйте их.");
    }
    return result.molecule();
  }

  public static Molecule optimized(final Molecule input, final ClassicalCalculationResult result) {
    if (!result.status().name().equals("SUCCEEDED") && !result.status().name().equals("PARTIAL")) {
      return null;
    }
    final var section =
        result.sections().stream()
            .filter(item -> item.kind() == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION)
            .findFirst()
            .orElse(null);
    if (section == null) {
      return null;
    }
    final var coordinates =
        section.values().stream()
            .filter(item -> item.name().equals("optimized_coordinates"))
            .findFirst()
            .orElse(null);
    if (coordinates == null) {
      return null;
    }
    if (!coordinates.unit().equals("angstrom")
        || coordinates.values().size() != input.atoms().size()) {
      throw new IllegalArgumentException(
          "Результат геометрии имеет неподдерживаемые единицы или число атомов.");
    }
    final HashMap<String, String[]> rows = new HashMap<>();
    for (final String text : coordinates.values()) {
      final String[] row = text.split(",", -1);
      if (row.length != 5 || rows.putIfAbsent(row[0], row) != null) {
        throw new IllegalArgumentException("Некорректные строки координат результата.");
      }
    }
    final double[] xyz = new double[input.atoms().size() * 3];
    for (int index = 0; index < input.atoms().size(); index++) {
      final var atom = input.atoms().get(index);
      final String[] row = rows.get(atom.id().value());
      if (row == null || !row[1].equals(atom.element().symbol().value())) {
        throw new IllegalArgumentException("Атомы результата не соответствуют входному снимку.");
      }
      for (int axis = 0; axis < 3; axis++) {
        final double value = Double.parseDouble(row[axis + 2]);
        if (!Double.isFinite(value)) {
          throw new IllegalArgumentException("Нечисловая координата результата.");
        }
        xyz[index * 3 + axis] = value;
      }
    }
    return GeometryCoordinateSupport.withCoordinatesAngstrom(input, xyz);
  }
}
