/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;

/** Explicit optional sample properties. Old readers reject the new command rather than discard it. */
final class QchemLaboratoryPropertiesCodec {
  private QchemLaboratoryPropertiesCodec() {}

  static void write(final StringBuilder text, final LaboratoryComponent component) {
    final var properties = component.properties();
    if (properties.empty()) {
      return;
    }
    text.append("  component-properties-v1 id=")
        .append(component.id())
        .append(" structure=")
        .append(properties.structureSignature())
        .append(" source=")
        .append(QchemEscaper.quote(properties.source()));
    field(text, "molarMass", properties.molarMass());
    field(text, "density", properties.density());
    field(text, "purity", properties.purityPercent());
    field(text, "molarity", properties.molarity());
    text.append("\r\n");
  }

  static Map<String, LaboratoryMaterialProperties> read(final List<QchemLine> body) {
    final Map<String, LaboratoryMaterialProperties> result = new LinkedHashMap<>();
    for (final var line : body) {
      if (!line.command().equals("component-properties-v1")) {
        continue;
      }
      if (!Set.of("id", "structure", "source", "molarMass", "density", "purity", "molarity")
          .containsAll(line.values().keySet())) {
        throw new IllegalArgumentException("Неизвестное поле свойств материала.");
      }
      final var properties =
          new LaboratoryMaterialProperties(
              number(line, "molarMass"),
              number(line, "density"),
              number(line, "purity"),
              number(line, "molarity"),
              QchemEscaper.unquote(line.required("source")),
              line.required("structure"));
      if (properties.empty()) {
        throw new IllegalArgumentException("Пустая запись свойств материала.");
      }
      if (result.put(line.required("id"), properties) != null) {
        throw new IllegalArgumentException("Повтор свойств компонента.");
      }
    }
    return result;
  }

  private static void field(final StringBuilder text, final String name, final BigDecimal value) {
    if (value != null) {
      text.append(' ').append(name).append('=').append(value.toPlainString());
    }
  }

  private static BigDecimal number(final QchemLine line, final String name) {
    return line.optional(name) == null ? null : new BigDecimal(line.required(name));
  }
}
