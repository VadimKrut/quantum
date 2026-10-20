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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySynthesisRoute;

/** Explicit directed route, expected yields and verified structural links in QCHEM 10. */
final class QchemLaboratoryRouteCodec {

  private QchemLaboratoryRouteCodec() {}

  static void write(final StringBuilder out, final LaboratorySynthesisRoute route) {
    if (route.empty()) {
      return;
    }
    out.append("  route-v1");
    field(out, "target", route.targetStageId() == null ? "" : route.targetStageId());
    if (route.targetQuantity() != null) {
      field(out, "quantity", route.targetQuantity().value().toPlainString());
      field(out, "unit", route.targetQuantity().unit().name());
    }
    out.append("\r\n");
    for (final var node : route.nodes()) {
      out.append("  route-node-v1");
      field(out, "stage", node.stageId());
      field(out, "reference", node.referenceComponentId());
      field(out, "product", node.productComponentId());
      field(out, "yield", node.expectedYieldPercent().toPlainString());
      field(out, "enabled", Boolean.toString(node.enabled()));
      out.append("\r\n");
    }
    for (final var link : route.links()) {
      out.append("  route-link-v1");
      field(out, "id", link.id());
      field(out, "source", link.sourceStageId());
      field(out, "target", link.targetStageId());
      field(out, "component", link.targetComponentId());
      field(out, "recovery", link.transferFraction().toPlainString());
      field(out, "sourceSignature", link.sourceSignature());
      field(out, "targetSignature", link.targetSignature());
      field(out, "enabled", Boolean.toString(link.enabled()));
      out.append("\r\n");
    }
  }

  static LaboratorySynthesisRoute read(final List<QchemLine> body) {
    QchemLine header = null;
    final var nodes = new ArrayList<LaboratorySynthesisRoute.Node>();
    final var links = new ArrayList<LaboratorySynthesisRoute.Link>();
    for (final var line : body) {
      if (!line.command().startsWith("route-")) {
        continue;
      }
      switch (line.command()) {
        case "route-v1" -> {
          keys(line, Set.of("target", "quantity", "unit"));
          if (header != null) {
            throw new IllegalArgumentException("Маршрут опыта задан дважды.");
          }
          header = line;
        }
        case "route-node-v1" -> {
          keys(line, Set.of("stage", "reference", "product", "yield", "enabled"));
          if (nodes.size() >= LaboratorySynthesisRoute.MAX_NODES) {
            throw new IllegalArgumentException("Слишком много стадий маршрута.");
          }
          nodes.add(
              new LaboratorySynthesisRoute.Node(
                  value(line, "stage"),
                  value(line, "reference"),
                  value(line, "product"),
                  decimal(line, "yield"),
                  enabled(line)));
        }
        case "route-link-v1" -> {
          keys(
              line,
              Set.of(
                  "id",
                  "source",
                  "target",
                  "component",
                  "recovery",
                  "sourceSignature",
                  "targetSignature",
                  "enabled"));
          if (links.size() >= LaboratorySynthesisRoute.MAX_LINKS) {
            throw new IllegalArgumentException("Слишком много связей маршрута.");
          }
          links.add(
              new LaboratorySynthesisRoute.Link(
                  value(line, "id"),
                  value(line, "source"),
                  value(line, "target"),
                  value(line, "component"),
                  decimal(line, "recovery"),
                  value(line, "sourceSignature"),
                  value(line, "targetSignature"),
                  enabled(line)));
        }
        default ->
            throw new IllegalArgumentException(
                "Неизвестная версия записи маршрута: " + line.command());
      }
    }
    if (header == null) {
      if (!nodes.isEmpty() || !links.isEmpty()) {
        throw new IllegalArgumentException("У маршрута отсутствует заголовок.");
      }
      return LaboratorySynthesisRoute.EMPTY;
    }
    if ((header.optional("quantity") == null) != (header.optional("unit") == null)) {
      throw new IllegalArgumentException("Количество цели маршрута и единица задаются вместе.");
    }
    final String target = value(header, "target");
    final var quantity =
        header.optional("quantity") == null
            ? null
            : new LaboratoryQuantity(
                decimal(header, "quantity"), LaboratoryQuantityUnit.valueOf(value(header, "unit")));
    return new LaboratorySynthesisRoute(nodes, links, target.isEmpty() ? null : target, quantity);
  }

  private static boolean enabled(final QchemLine line) {
    return switch (value(line, "enabled")) {
      case "true" -> true;
      case "false" -> false;
      default ->
          throw new IllegalArgumentException(
              "Активность элемента маршрута должна быть true или false.");
    };
  }

  private static BigDecimal decimal(final QchemLine line, final String key) {
    final String value = value(line, key);
    if (value.length() > 202) {
      throw new IllegalArgumentException("Число в маршруте слишком длинное.");
    }
    return new BigDecimal(value);
  }

  private static String value(final QchemLine line, final String key) {
    final String value = line.required(key);
    if (value.length() > 1202) {
      throw new IllegalArgumentException("Поле маршрута слишком длинное.");
    }
    return QchemEscaper.unquote(value);
  }

  private static void keys(final QchemLine line, final Set<String> allowed) {
    if (!allowed.containsAll(line.values().keySet())) {
      throw new IllegalArgumentException("Неизвестное поле маршрута.");
    }
  }

  private static void field(final StringBuilder out, final String key, final String value) {
    out.append(' ').append(key).append('=').append(QchemEscaper.quote(value));
  }
}
