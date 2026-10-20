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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryProcedure;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;

/** Typed method and immutable execution snapshots, introduced in QCHEM 9. */
final class QchemLaboratoryProcedureCodec {

  private static final Set<String> STEP_KEYS =
      Set.of(
          "id",
          "stage",
          "kind",
          "title",
          "instruction",
          "component",
          "portion",
          "duration",
          "temperature");
  private static final Set<String> EVENT_KEYS =
      Set.of(
          "id",
          "step",
          "time",
          "action",
          "text",
          "componentName",
          "signature",
          "corrects",
          "planned",
          "plannedUnit",
          "actual",
          "actualUnit",
          "snapshotId",
          "snapshotStage",
          "snapshotKind",
          "snapshotTitle",
          "snapshotInstruction",
          "snapshotComponent",
          "snapshotPortion",
          "snapshotDuration",
          "snapshotTemperature");

  private QchemLaboratoryProcedureCodec() {}

  static void write(final StringBuilder out, final LaboratoryProcedure procedure) {
    for (final var step : procedure.steps()) {
      out.append("  procedure-step-v1");
      writeStep(out, step, false);
      out.append("\r\n");
    }
    for (final var event : procedure.events()) {
      out.append("  procedure-event-v1");
      field(out, "id", event.id());
      field(out, "step", event.stepId());
      field(out, "time", event.recordedAt().toString());
      field(out, "action", event.action().name());
      field(out, "text", event.text());
      field(out, "componentName", event.componentName());
      field(out, "signature", event.structureSignature());
      field(out, "corrects", event.correctsId() == null ? "" : event.correctsId());
      writeQuantity(out, "planned", event.plannedQuantity());
      writeQuantity(out, "actual", event.actualQuantity());
      writeStep(out, event.snapshot(), true);
      out.append("\r\n");
    }
  }

  static LaboratoryProcedure read(final List<QchemLine> body) {
    final var steps = new ArrayList<LaboratoryProcedure.Step>();
    final var events = new ArrayList<LaboratoryProcedure.Event>();
    long characters = 0;
    for (final var line : body) {
      if (!line.command().startsWith("procedure-")) {
        continue;
      }
      for (final var value : line.values().values()) {
        characters += value.length();
      }
      if (characters > 30000000) {
        throw new IllegalArgumentException("Записи методики превышают допустимый размер.");
      }
      switch (line.command()) {
        case "procedure-step-v1" -> {
          keys(line, STEP_KEYS);
          if (steps.size() >= LaboratoryProcedure.MAX_STEPS) {
            throw new IllegalArgumentException("Слишком много действий методики.");
          }
          steps.add(readStep(line, false));
        }
        case "procedure-event-v1" -> {
          keys(line, EVENT_KEYS);
          if (events.size() >= LaboratoryProcedure.MAX_EVENTS) {
            throw new IllegalArgumentException("Слишком много записей выполнения.");
          }
          events.add(
              new LaboratoryProcedure.Event(
                  value(line, "id"),
                  value(line, "step"),
                  Instant.parse(value(line, "time")),
                  LaboratoryProcedure.Action.valueOf(value(line, "action")),
                  value(line, "text"),
                  readStep(line, true),
                  value(line, "componentName"),
                  readQuantity(line, "planned"),
                  readQuantity(line, "actual"),
                  value(line, "signature"),
                  nullable(value(line, "corrects"))));
        }
        default ->
            throw new IllegalArgumentException(
                "Неизвестная версия записи методики: " + line.command());
      }
    }
    return steps.isEmpty() && events.isEmpty()
        ? LaboratoryProcedure.EMPTY
        : new LaboratoryProcedure(steps, events);
  }

  private static void writeStep(
      final StringBuilder out, final LaboratoryProcedure.Step step, final boolean snapshot) {
    field(out, key("id", snapshot), step.id());
    field(out, key("stage", snapshot), step.stageId() == null ? "" : step.stageId());
    field(out, key("kind", snapshot), step.kind().name());
    field(out, key("title", snapshot), step.title());
    field(out, key("instruction", snapshot), step.instruction());
    field(out, key("component", snapshot), step.componentId() == null ? "" : step.componentId());
    field(out, key("portion", snapshot), step.portion().toPlainString());
    field(out, key("duration", snapshot), Long.toString(step.durationSeconds()));
    if (step.temperatureC() != null) {
      field(out, key("temperature", snapshot), step.temperatureC().toPlainString());
    }
  }

  private static LaboratoryProcedure.Step readStep(final QchemLine line, final boolean snapshot) {
    return new LaboratoryProcedure.Step(
        value(line, key("id", snapshot)),
        nullable(value(line, key("stage", snapshot))),
        LaboratoryProcedure.Kind.valueOf(value(line, key("kind", snapshot))),
        value(line, key("title", snapshot)),
        value(line, key("instruction", snapshot)),
        nullable(value(line, key("component", snapshot))),
        decimal(line, key("portion", snapshot)),
        Long.parseLong(value(line, key("duration", snapshot))),
        line.optional(key("temperature", snapshot)) == null
            ? null
            : decimal(line, key("temperature", snapshot)));
  }

  private static void writeQuantity(
      final StringBuilder out, final String key, final LaboratoryQuantity quantity) {
    if (quantity != null) {
      field(out, key, quantity.value().toPlainString());
      field(out, key + "Unit", quantity.unit().name());
    }
  }

  private static LaboratoryQuantity readQuantity(final QchemLine line, final String key) {
    if ((line.optional(key) == null) != (line.optional(key + "Unit") == null)) {
      throw new IllegalArgumentException("Количество и единица записи выполнения задаются вместе.");
    }
    return line.optional(key) == null
        ? null
        : new LaboratoryQuantity(
            decimal(line, key), LaboratoryQuantityUnit.valueOf(value(line, key + "Unit")));
  }

  private static BigDecimal decimal(final QchemLine line, final String key) {
    final var value = value(line, key);
    if (value.length() > 202) {
      throw new IllegalArgumentException("Число в методике содержит слишком много знаков.");
    }
    return new BigDecimal(value);
  }

  private static String key(final String field, final boolean snapshot) {
    return snapshot
        ? "snapshot" + Character.toUpperCase(field.charAt(0)) + field.substring(1)
        : field;
  }

  private static void keys(final QchemLine line, final Set<String> allowed) {
    if (!allowed.containsAll(line.values().keySet())) {
      throw new IllegalArgumentException("Неизвестное поле методики.");
    }
  }

  private static String value(final QchemLine line, final String name) {
    final var value = line.required(name);
    if (value.length() > 600002) {
      throw new IllegalArgumentException("Поле методики превышает допустимую длину.");
    }
    return QchemEscaper.unquote(value);
  }

  private static String nullable(final String value) {
    return value.isEmpty() ? null : value;
  }

  private static void field(final StringBuilder out, final String key, final String value) {
    out.append(' ').append(key).append('=').append(QchemEscaper.quote(value));
  }
}
