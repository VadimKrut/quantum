/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Map;

/** Display labels only; original computation evidence remains in its QCHEM attachment. */
final class LaboratoryCalculationPresentation {
  private LaboratoryCalculationPresentation() {}

  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("formula", "Формула"), Map.entry("atom_count", "Всего явных атомов"),
          Map.entry("heavy_atom_count", "Тяжёлых атомов"),
              Map.entry("hetero_atom_count", "Гетероатомов"),
          Map.entry("bond_count", "Связей"), Map.entry("electron_count", "Электронов"),
          Map.entry("formal_charge", "Формальный заряд"), Map.entry("molar_mass", "Молярная масса"),
          Map.entry("converged", "Сходимость"), Map.entry("iterations", "Итераций"));

  static String report(final String source) {
    final StringBuilder text = new StringBuilder();
    for (final String line : source.split("\\R", -1)) {
      final int separator = line.indexOf(" = ");
      if (separator > 0 && LABELS.containsKey(line.substring(0, separator))) {
        text.append(LABELS.get(line.substring(0, separator)))
            .append(": ")
            .append(line.substring(separator + 3).replaceFirst(" count$", ""));
      } else {
        text.append(
            switch (line) {
              case "Статус: SUCCEEDED" -> "Статус: расчёт завершён";
              case "Статус: PARTIAL" -> "Статус: выполнен с ограничениями";
              case "Статус: CANCELLED" -> "Статус: расчёт остановлен (CANCELLED)";
              case "Статус: FAILED" -> "Статус: ошибка расчёта";
              case "Задача: DESCRIPTOR_ANALYSIS" -> "Задача: состав и дескрипторы";
              case "Задача: SINGLE_POINT_ENERGY" -> "Задача: энергия заданной геометрии";
              case "Задача: GEOMETRY_OPTIMIZATION" -> "Задача: релаксация геометрии";
              case "Покрытие: EXACT" -> "Покрытие: точное в пределах выбранной модели";
              case "Molecular descriptors" -> "Состав молекулярного графа";
              default -> line;
            });
      }
      text.append('\n');
    }
    return text.toString();
  }
}
