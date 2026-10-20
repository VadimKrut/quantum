/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.ArrayList;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** A read-only report of the existing chemistry core's diagnostics for the current draft. */
final class LaboratoryStructureCheck {
  record Issue(String componentId, String name, ChemistryDiagnostic diagnostic) {
    String title() {
      return switch (this.diagnostic.code()) {
        case MOLECULE_HAS_NO_BONDS -> "Атомы не соединены связями";
        case MOLECULE_HAS_NO_GEOMETRY -> "3D-геометрия ещё не задана";
        case MOLECULE_CHARGE_DOES_NOT_MATCH_ATOMS ->
            "Заряд молекулы отличается от суммы зарядов атомов";
        case MOLECULE_RADICAL_SPIN_NEEDS_REVIEW -> "Проверьте спиновое состояние радикала";
        case MOLECULE_RADICAL_SPIN_MULTIPLICITY_MISMATCH ->
            "Радикалы и спиновая мультиплетность не согласованы";
        case MOLECULE_SPIN_MULTIPLICITY_INCOMPATIBLE_WITH_ELECTRONS ->
            "Спиновая мультиплетность не согласована с числом электронов";
        case MOLECULE_GRAPH_IS_DISCONNECTED -> "В структуре есть несвязанные фрагменты";
        case MOLECULE_STEREOCHEMISTRY_NEEDS_REVIEW -> "Стереозаписи требуют повторной проверки";
        case MOLECULE_STEREOCHEMISTRY_NEEDS_GEOMETRY ->
            "Для проверки стереохимии нужна 3D-геометрия";
        case MOLECULE_CONFORMATION_NEEDS_GEOMETRY -> "Для проверки конформации нужна 3D-геометрия";
        case MOLECULE_OPTICAL_ROTATION_WITHOUT_STEREOCHEMISTRY ->
            "Оптическое вращение задано без стереохимии";
        case MOLECULE_UNKNOWN_BOND_ORDER -> "Есть связь с неизвестным порядком";
        case MOLECULE_VALENCE_EXCEEDS_PROFILE -> "Валентность атома превышает обычный профиль";
        default -> this.diagnostic.message();
      };
    }

    String level() {
      return switch (this.diagnostic.severity()) {
        case ERROR -> "Ошибка";
        case WARNING -> "Проверить";
        case INFO -> "Сведения";
      };
    }
  }

  static java.util.List<Issue> inspect(final LaboratoryDrawing document) {
    final ArrayList<Issue> issues = new ArrayList<>();
    final ChemistryCoreValidator validator = new ChemistryCoreValidator();
    document
        .experiment()
        .components()
        .forEach(
            component ->
                validator
                    .validateMolecule(component.molecule())
                    .diagnostics()
                    .forEach(
                        diagnostic ->
                            issues.add(
                                new Issue(
                                    component.id(),
                                    component.molecule().displayName(),
                                    diagnostic))));
    issues.sort(
        java.util.Comparator.comparingInt((Issue issue) -> issue.diagnostic().severity().ordinal())
            .reversed());
    return java.util.List.copyOf(issues);
  }
}
