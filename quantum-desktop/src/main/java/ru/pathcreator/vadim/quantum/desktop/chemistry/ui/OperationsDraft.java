/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import javafx.scene.control.TableView;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;

/** Typed local row buffers; recovery never commits them as experimental observations. */
final class OperationsDraft {
  private static final String PREFIX = "operations.buffer.";

  private OperationsDraft() {}

  static void write(
      final Properties data,
      final List<Phase> phases,
      final List<WorkupStep> steps,
      final List<Loss> workupLosses,
      final List<GradientPoint> gradient,
      final List<Fraction> fractions,
      final List<Pool> pools,
      final List<Loss> runLosses) {
    new ArrayList<>(data.stringPropertyNames())
        .stream().filter(k -> k.startsWith(PREFIX)).forEach(data::remove);
    data.setProperty(PREFIX + "version", "2");
    rows(
        data,
        "phases",
        phases,
        (row, value) -> {
          row.put("id", value.id());
          row.put("label", value.label());
          row.put("kind", value.kind());
        });
    rows(
        data,
        "steps",
        steps,
        (row, value) -> {
          row.put("id", value.id());
          row.put("kind", value.kind());
          row.put("from", value.fromPhaseId());
          row.put("to", value.toPhaseId());
          row.put("material", value.material());
          row.put("repetitions", value.repetitions());
          row.quantity("planned", value.planned());
          row.quantity("actual", value.actual());
          row.put("source", value.source());
          row.put("note", value.note());
          row.observation(value.observation());
        });
    rows(data, "workupLosses", workupLosses, OperationsDraft::loss);
    rows(
        data,
        "gradient",
        gradient,
        (row, value) -> {
          row.put("position", value.position());
          row.put("percent", value.solventBPercent());
          row.put("flow", value.flowMlPerMinute());
        });
    rows(
        data,
        "fractions",
        fractions,
        (row, value) -> {
          row.put("id", value.id());
          row.put("label", value.label());
          row.put("start", value.start());
          row.put("end", value.end());
          row.quantity("planned", value.planned());
          row.quantity("actual", value.actual());
          row.put("purity", value.purityPercent());
          row.put("sample", value.sampleId());
          row.put("source", value.source());
          row.put("note", value.note());
          row.observation(value.observation());
        });
    rows(
        data,
        "pools",
        pools,
        (row, value) -> {
          row.put("id", value.id());
          row.put("label", value.label());
          row.put("fractions", String.join(",", value.fractionIds()));
          row.quantity("actual", value.actual());
          row.put("sample", value.sampleId());
          row.put("source", value.source());
          row.put("note", value.note());
          row.observation(value.observation());
        });
    rows(data, "runLosses", runLosses, OperationsDraft::loss);
  }

  static void read(
      final Properties data,
      final TableView<Phase> phases,
      final TableView<WorkupStep> steps,
      final TableView<Loss> workupLosses,
      final TableView<GradientPoint> gradient,
      final TableView<Fraction> fractions,
      final TableView<Pool> pools,
      final TableView<Loss> runLosses) {
    final var version = data.getProperty(PREFIX + "version");
    if (version == null) {
      return;
    }
    if (!version.equals("1") && !version.equals("2")) {
      throw new IllegalArgumentException("Неизвестная версия черновика обработки.");
    }
    long length = 0;
    int properties = 0;
    for (final var key : data.stringPropertyNames()) {
      if (key.startsWith(PREFIX)) {
        length += data.getProperty(key).length();
        if (++properties > 150000 || length > 12000000) {
          throw new IllegalArgumentException("Черновик обработки превышает допустимый размер.");
        }
      }
    }
    final var phaseRows =
        rows(
            data,
            "phases",
            200,
            row ->
                new Phase(
                    row.text("id"), row.text("label"), row.enumeration("kind", PhaseKind.class)));
    final var stepRows =
        rows(
            data,
            "steps",
            2000,
            row ->
                new WorkupStep(
                    row.text("id"),
                    row.enumeration("kind", WorkupKind.class),
                    row.optional("from"),
                    row.optional("to"),
                    row.text("material"),
                    row.integer("repetitions"),
                    row.quantity("planned"),
                    row.quantity("actual"),
                    row.text("source"),
                    row.text("note"),
                    row.observation()));
    final var workupLossRows = rows(data, "workupLosses", 2000, OperationsDraft::loss);
    final var gradientRows =
        rows(
            data,
            "gradient",
            1000,
            row ->
                new GradientPoint(
                    row.decimal("position"), row.decimal("percent"), row.decimal("flow")));
    final var fractionRows =
        rows(
            data,
            "fractions",
            2000,
            row ->
                new Fraction(
                    row.text("id"),
                    row.text("label"),
                    row.decimal("start"),
                    row.decimal("end"),
                    row.quantity("planned"),
                    row.quantity("actual"),
                    row.decimal("purity"),
                    row.optional("sample"),
                    row.text("source"),
                    row.text("note"),
                    row.observation()));
    final var poolRows =
        rows(
            data,
            "pools",
            1000,
            row ->
                new Pool(
                    row.text("id"),
                    row.text("label"),
                    List.of(row.text("fractions").split(",", -1)),
                    row.quantity("actual"),
                    row.optional("sample"),
                    row.text("source"),
                    row.text("note"),
                    row.observation()));
    final var runLossRows = rows(data, "runLosses", 2000, OperationsDraft::loss);
    // Header placeholders validate only row relationships; editable header fields live separately.
    final var workup =
        new Workup(
            "draft_workup",
            null,
            "",
            Instant.EPOCH,
            "Черновик",
            phaseRows,
            stepRows,
            workupLossRows,
            "",
            null);
    final var run =
        new Purification(
            "draft_run",
            null,
            "",
            Instant.EPOCH,
            "Черновик",
            PurificationKind.CUSTOM,
            null,
            null,
            null,
            "",
            null,
            "A",
            "B",
            GradientAxis.MILLILITRES,
            gradientRows,
            fractionRows,
            poolRows,
            runLossRows,
            "",
            "",
            null);
    new LaboratoryReactionOperations(List.of(), List.of(workup), List.of(run));
    phases.getItems().setAll(phaseRows);
    steps.getItems().setAll(stepRows);
    workupLosses.getItems().setAll(workupLossRows);
    gradient.getItems().setAll(gradientRows);
    fractions.getItems().setAll(fractionRows);
    pools.getItems().setAll(poolRows);
    runLosses.getItems().setAll(runLossRows);
  }

  private static void loss(final Row row, final Loss value) {
    row.put("id", value.id());
    row.put("label", value.label());
    row.quantity("planned", value.planned());
    row.quantity("actual", value.actual());
    row.put("source", value.source());
    row.observation(value.observation());
  }

  private static Loss loss(final Row row) {
    return new Loss(
        row.text("id"),
        row.text("label"),
        row.quantity("planned"),
        row.quantity("actual"),
        row.text("source"),
        row.observation());
  }

  private static <T> void rows(
      final Properties data,
      final String name,
      final List<T> values,
      final BiConsumer<Row, T> writer) {
    data.setProperty(PREFIX + name + ".count", Integer.toString(values.size()));
    for (int i = 0; i < values.size(); i++) {
      writer.accept(new Row(data, PREFIX + name + "." + i + "."), values.get(i));
    }
  }

  private static <T> List<T> rows(
      final Properties data, final String name, final int maximum, final Function<Row, T> reader) {
    final int count = Integer.parseInt(data.getProperty(PREFIX + name + ".count", "0"));
    if (count < 0 || count > maximum) {
      throw new IllegalArgumentException("Слишком много строк в черновике обработки.");
    }
    final List<T> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      result.add(reader.apply(new Row(data, PREFIX + name + "." + i + ".")));
    }
    return List.copyOf(result);
  }

  static void observation(final Properties data, final String prefix, final Observation value) {
    data.setProperty(prefix + "origin", value.origin().name());
    data.setProperty(prefix + "count", Integer.toString(value.analyses().size()));
    for (int i = 0; i < value.analyses().size(); i++) {
      final var ref = value.analyses().get(i);
      data.setProperty(prefix + i + ".kind", ref.kind().name());
      data.setProperty(prefix + i + ".id", ref.id());
    }
  }

  static Observation observation(final Properties data, final String prefix) {
    if (!data.containsKey(prefix + "origin")) {
      if (data.stringPropertyNames().stream().anyMatch(k -> k.startsWith(prefix))) {
        throw new IllegalArgumentException(
            "В черновике связей отсутствует происхождение; данные сохранены для восстановления.");
      }
      return Observation.EMPTY;
    }
    final int count = Integer.parseInt(data.getProperty(prefix + "count", "0"));
    if (count < 0 || count > 64) {
      throw new IllegalArgumentException("Слишком много связанных анализов в черновике.");
    }
    final List<AnalysisRef> refs = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      refs.add(
          new AnalysisRef(
              AnalysisKind.valueOf(data.getProperty(prefix + i + ".kind", "")),
              data.getProperty(prefix + i + ".id", "")));
    }
    return new Observation(Origin.valueOf(data.getProperty(prefix + "origin")), refs);
  }

  private record Row(Properties data, String prefix) {
    void observation(final Observation value) {
      OperationsDraft.observation(this.data, this.prefix + "observation.", value);
    }

    Observation observation() {
      return OperationsDraft.observation(this.data, this.prefix + "observation.");
    }

    void put(final String key, final Object value) {
      this.data.setProperty(
          this.prefix + key,
          value == null
              ? ""
              : value instanceof Enum<?> e
                  ? e.name()
                  : value instanceof BigDecimal n ? n.toPlainString() : value.toString());
    }

    String text(final String key) {
      final var value = this.data.getProperty(this.prefix + key);
      if (value == null || value.length() > 402000) {
        throw new IllegalArgumentException("Повреждено поле черновика обработки.");
      }
      return value;
    }

    String optional(final String key) {
      final var value = this.text(key);
      return value.isEmpty() ? null : value;
    }

    int integer(final String key) {
      return Integer.parseInt(this.text(key));
    }

    BigDecimal decimal(final String key) {
      final String value = this.optional(key);
      if (value == null) {
        return null;
      }
      if (value.length() > 100) {
        throw new IllegalArgumentException("Слишком длинное число в черновике.");
      }
      return new BigDecimal(value);
    }

    <E extends Enum<E>> E enumeration(final String key, final Class<E> type) {
      return Enum.valueOf(type, this.text(key));
    }

    void quantity(final String key, final LaboratoryQuantity value) {
      this.put(key, value == null ? null : value.value());
      this.put(key + "Unit", value == null ? null : value.unit());
    }

    LaboratoryQuantity quantity(final String key) {
      final var number = this.decimal(key);
      final String unit = this.optional(key + "Unit");
      if ((number == null) != (unit == null)) {
        throw new IllegalArgumentException("В количестве отсутствует число или единица.");
      }
      return number == null
          ? null
          : new LaboratoryQuantity(number, LaboratoryQuantityUnit.valueOf(unit));
    }
  }
}
