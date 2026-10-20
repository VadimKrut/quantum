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
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;

/** Explicit typed QCHEM records for monitoring, workup and purification; no opaque serialized payloads. */
final class QchemLaboratoryOperationsCodec {
  private QchemLaboratoryOperationsCodec() {}

  static void write(final StringBuilder out, final LaboratoryReactionOperations operations) {
    for (final var p : operations.timePoints()) {
      line(out, "timepoint");
      revision(out, p);
      f(out, "elapsed", p.elapsed());
      f(out, "elapsedUnit", p.elapsedUnit().name());
      f(out, "sample", p.sampleId());
      f(out, "label", p.label());
      f(out, "method", p.method().name());
      f(out, "conversion", p.conversion());
      f(out, "conversionUnit", p.conversionUnit().name());
      f(out, "source", p.source());
      f(out, "note", p.note());
      end(out);
      observation(out, p.id(), Scope.TIMEPOINT, "", p.observation());
    }
    for (final var w : operations.workups()) {
      line(out, "workup");
      revision(out, w);
      f(out, "title", w.title());
      f(out, "note", w.note());
      end(out);
      for (final var p : w.phases()) {
        line(out, "phase");
        f(out, "owner", w.id());
        f(out, "id", p.id());
        f(out, "label", p.label());
        f(out, "kind", p.kind().name());
        end(out);
      }
      for (final var s : w.steps()) {
        line(out, "workup-step");
        f(out, "owner", w.id());
        f(out, "id", s.id());
        f(out, "kind", s.kind().name());
        f(out, "from", s.fromPhaseId());
        f(out, "to", s.toPhaseId());
        f(out, "material", s.material());
        f(out, "repetitions", s.repetitions());
        q(out, "planned", s.planned());
        q(out, "actual", s.actual());
        f(out, "source", s.source());
        f(out, "note", s.note());
        end(out);
        observation(out, w.id(), Scope.STEP, s.id(), s.observation());
      }
      losses(out, w.id(), w.losses());
    }
    for (final var p : operations.purifications()) {
      line(out, "purification");
      revision(out, p);
      f(out, "title", p.title());
      f(out, "kind", p.kind().name());
      f(out, "inputSample", p.inputSampleId());
      q(out, "inputPlanned", p.inputPlanned());
      q(out, "inputActual", p.inputActual());
      f(out, "stationary", p.stationaryPhase());
      q(out, "sorbent", p.sorbentMass());
      f(out, "solventA", p.solventA());
      f(out, "solventB", p.solventB());
      f(out, "axis", p.axis().name());
      f(out, "source", p.source());
      f(out, "note", p.note());
      end(out);
      observation(out, p.id(), Scope.INPUT, "", p.observation());
      for (final var g : p.gradient()) {
        line(out, "gradient");
        f(out, "owner", p.id());
        f(out, "position", g.position());
        f(out, "bPercent", g.solventBPercent());
        f(out, "flow", g.flowMlPerMinute());
        end(out);
      }
      for (final var r : p.fractions()) {
        line(out, "fraction");
        f(out, "owner", p.id());
        f(out, "id", r.id());
        f(out, "label", r.label());
        f(out, "start", r.start());
        f(out, "end", r.end());
        q(out, "planned", r.planned());
        q(out, "actual", r.actual());
        f(out, "purity", r.purityPercent());
        f(out, "sample", r.sampleId());
        f(out, "source", r.source());
        f(out, "note", r.note());
        end(out);
        observation(out, p.id(), Scope.FRACTION, r.id(), r.observation());
      }
      for (final var pool : p.pools()) {
        line(out, "pool");
        f(out, "owner", p.id());
        f(out, "id", pool.id());
        f(out, "label", pool.label());
        f(out, "fractions", String.join(",", pool.fractionIds()));
        q(out, "actual", pool.actual());
        f(out, "sample", pool.sampleId());
        f(out, "source", pool.source());
        f(out, "note", pool.note());
        end(out);
        observation(out, p.id(), Scope.POOL, pool.id(), pool.observation());
      }
      losses(out, p.id(), p.losses());
    }
    for (final var outcome : operations.stageOutcomes()) {
      line(out, "stage-outcome");
      revision(out, outcome);
      f(out, "status", outcome.status().name());
      f(out, "source", outcome.source());
      f(out, "reason", outcome.reason());
      end(out);
      observation(out, outcome.id(), Scope.STAGE_OUTCOME, "", outcome.observation());
      outcomeProducts(out, outcome.id(), "TARGET", outcome.targets());
      outcomeProducts(out, outcome.id(), "BYPRODUCT", outcome.byproducts());
    }
  }

  private static void outcomeProducts(
      final StringBuilder out,
      final String owner,
      final String role,
      final List<OutcomeProduct> products) {
    for (final var product : products) {
      line(out, "outcome-product");
      f(out, "owner", owner);
      f(out, "role", role);
      f(out, "component", product.componentId());
      f(out, "molecule", product.snapshot().id().value());
      end(out);
    }
  }

  private static void losses(final StringBuilder out, final String owner, final List<Loss> losses) {
    for (final var loss : losses) {
      line(out, "loss");
      f(out, "owner", owner);
      f(out, "id", loss.id());
      f(out, "label", loss.label());
      q(out, "planned", loss.planned());
      q(out, "actual", loss.actual());
      f(out, "source", loss.source());
      end(out);
      observation(out, owner, Scope.LOSS, loss.id(), loss.observation());
    }
  }

  private enum Scope {
    TIMEPOINT,
    INPUT,
    STEP,
    LOSS,
    FRACTION,
    POOL,
    STAGE_OUTCOME
  }

  private record Target(String owner, Scope scope, String item) {
    Target {
      owner = identifier(owner);
      Objects.requireNonNull(scope);
      if (scope == Scope.TIMEPOINT || scope == Scope.INPUT || scope == Scope.STAGE_OUTCOME) {
        if (!item.isEmpty()) {
          throw new IllegalArgumentException(
              "У происхождения основной записи не должно быть ID строки.");
        }
      } else {
        item = identifier(item);
      }
    }
  }

  private static String identifier(final String value) {
    final var id =
        ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue.requireIdentifier(
            value, "Идентификатор происхождения операции");
    if (id.length() > 200) {
      throw new IllegalArgumentException("Слишком длинный идентификатор происхождения.");
    }
    return id;
  }

  private static void target(
      final StringBuilder out, final String owner, final Scope scope, final String item) {
    f(out, "owner", owner);
    f(out, "scope", scope.name());
    f(out, "item", item);
  }

  private static void observation(
      final StringBuilder out,
      final String owner,
      final Scope scope,
      final String item,
      final Observation value) {
    if (value.equals(Observation.EMPTY)) {
      return;
    }
    line(out, "observation");
    target(out, owner, scope, item);
    f(out, "origin", value.origin().name());
    end(out);
    for (final var ref : value.analyses()) {
      line(out, "analysis-ref");
      target(out, owner, scope, item);
      f(out, "kind", ref.kind().name());
      f(out, "id", ref.id());
      end(out);
    }
  }

  private static final class Metadata {
    private final Map<Target, Observation> values = new LinkedHashMap<>();

    Metadata(final List<QchemLine> body) {
      final Map<Target, Origin> origins = new LinkedHashMap<>();
      final Map<Target, List<AnalysisRef>> references = new LinkedHashMap<>();
      int total = 0;
      for (final var line : body) {
        if (!line.command().equals("operations-observation-v1")
            && !line.command().equals("operations-analysis-ref-v1")) {
          continue;
        }
        keys(
            line,
            line.command().equals("operations-observation-v1")
                ? "owner scope item origin"
                : "owner scope item kind id");
        final var key =
            new Target(v(line, "owner"), Scope.valueOf(v(line, "scope")), v(line, "item"));
        if (line.command().equals("operations-observation-v1")) {
          if (origins.size() >= 122000
              || origins.putIfAbsent(key, Origin.valueOf(v(line, "origin"))) != null) {
            throw new IllegalArgumentException(
                "Дубликат или слишком много записей происхождения операций.");
          }
        } else {
          if (++total > 100000) {
            throw new IllegalArgumentException("Слишком много аналитических ссылок операций.");
          }
          final var list = references.computeIfAbsent(key, k -> new ArrayList<>());
          if (list.size() >= 64) {
            throw new IllegalArgumentException(
                "У одной записи поддерживается до 64 аналитических ссылок.");
          }
          list.add(new AnalysisRef(AnalysisKind.valueOf(v(line, "kind")), v(line, "id")));
        }
      }
      if (!origins.keySet().containsAll(references.keySet())) {
        throw new IllegalArgumentException(
            "Аналитическая ссылка операции не имеет записи происхождения.");
      }
      origins.forEach(
          (key, origin) ->
              this.values.put(
                  key, new Observation(origin, references.getOrDefault(key, List.of()))));
    }

    Observation take(final String owner, final Scope scope, final String item) {
      final var value = this.values.remove(new Target(owner, scope, item));
      return value == null ? Observation.EMPTY : value;
    }

    void complete() {
      if (!this.values.isEmpty()) {
        throw new IllegalArgumentException(
            "Происхождение ссылается на отсутствующую запись или строку операции.");
      }
    }
  }

  private static void revision(final StringBuilder out, final Revision revision) {
    f(out, "id", revision.id());
    f(out, "stage", revision.stageId());
    f(out, "stageTitle", revision.stageTitle());
    f(out, "time", revision.recordedAt());
    f(out, "corrects", revision.correctsId());
  }

  private static void line(final StringBuilder out, final String type) {
    out.append("  operations-").append(type).append("-v1");
  }

  private static void end(final StringBuilder out) {
    out.append("\r\n");
  }

  private static void f(final StringBuilder out, final String key, final Object value) {
    out.append(' ')
        .append(key)
        .append('=')
        .append(
            QchemEscaper.quote(
                value == null
                    ? ""
                    : value instanceof BigDecimal n ? n.toPlainString() : value.toString()));
  }

  private static void q(final StringBuilder out, final String key, final LaboratoryQuantity value) {
    if (value != null) {
      f(out, key, value.value());
      f(out, key + "Unit", value.unit().name());
    }
  }

  static LaboratoryReactionOperations read(
      final List<QchemLine> body, final Map<String, Molecule> molecules) {
    final Metadata metadata = new Metadata(body);
    final List<TimePoint> points = new ArrayList<>();
    final Map<String, QchemLine> workups = new LinkedHashMap<>(),
        runs = new LinkedHashMap<>(),
        outcomes = new LinkedHashMap<>();
    final Map<String, List<OutcomeProduct>> targets = new LinkedHashMap<>(),
        byproducts = new LinkedHashMap<>();
    final Map<String, List<Phase>> phases = new LinkedHashMap<>();
    final Map<String, List<WorkupStep>> steps = new LinkedHashMap<>();
    final Map<String, List<Loss>> losses = new LinkedHashMap<>();
    final Map<String, List<GradientPoint>> gradients = new LinkedHashMap<>();
    final Map<String, List<Fraction>> fractions = new LinkedHashMap<>();
    final Map<String, List<Pool>> pools = new LinkedHashMap<>();
    long characters = 0;
    int count = 0;
    for (final var l : body) {
      if (!l.command().startsWith("operations-")) {
        continue;
      }
      if (++count > 344000) {
        throw new IllegalArgumentException("Слишком много записей обработки и очистки.");
      }
      for (final var value : l.values().values()) {
        characters += value.length();
      }
      if (characters > 30000000) {
        throw new IllegalArgumentException("Записи обработки превышают допустимый размер.");
      }
      switch (l.command()) {
        case "operations-timepoint-v1" -> {
          keys(
              l,
              "id stage stageTitle time corrects elapsed elapsedUnit sample label method conversion conversionUnit source note");
          if (points.size() >= 10000) {
            throw new IllegalArgumentException("Слишком много проб.");
          }
          points.add(
              new TimePoint(
                  v(l, "id"),
                  n(l, "stage"),
                  v(l, "stageTitle"),
                  time(l),
                  decimal(l, "elapsed"),
                  ElapsedUnit.valueOf(v(l, "elapsedUnit")),
                  n(l, "sample"),
                  v(l, "label"),
                  AnalysisMethod.valueOf(v(l, "method")),
                  decimal(l, "conversion"),
                  ConversionUnit.valueOf(v(l, "conversionUnit")),
                  v(l, "source"),
                  v(l, "note"),
                  n(l, "corrects"),
                  metadata.take(v(l, "id"), Scope.TIMEPOINT, "")));
        }
        case "operations-stage-outcome-v1" -> {
          keys(l, "id stage stageTitle time corrects status source reason");
          header(outcomes, l, 10000);
        }
        case "operations-outcome-product-v1" -> {
          keys(l, "owner role component molecule");
          final var molecule = molecules.get(v(l, "molecule"));
          if (molecule == null) {
            throw new IllegalArgumentException("Снимок продукта исхода стадии отсутствует.");
          }
          final Map<String, List<OutcomeProduct>> destination =
              switch (v(l, "role")) {
                case "TARGET" -> targets;
                case "BYPRODUCT" -> byproducts;
                default ->
                    throw new IllegalArgumentException(
                        "Неизвестное назначение продукта исхода стадии.");
              };
          final String owner = v(l, "owner");
          final var values = destination.computeIfAbsent(owner, key -> new ArrayList<>());
          if (values.size() >= 200) {
            throw new IllegalArgumentException("Слишком много продуктов одного исхода стадии.");
          }
          values.add(new OutcomeProduct(v(l, "component"), molecule));
        }
        case "operations-workup-v1" -> {
          keys(l, "id stage stageTitle time corrects title note");
          header(workups, l);
        }
        case "operations-purification-v1" -> {
          keys(
              l,
              "id stage stageTitle time corrects title kind inputSample inputPlanned inputPlannedUnit inputActual inputActualUnit stationary sorbent sorbentUnit solventA solventB axis source note");
          header(runs, l);
        }
        case "operations-phase-v1" -> {
          keys(l, "owner id label kind");
          add(
              phases,
              v(l, "owner"),
              new Phase(v(l, "id"), v(l, "label"), PhaseKind.valueOf(v(l, "kind"))));
        }
        case "operations-workup-step-v1" -> {
          keys(
              l,
              "owner id kind from to material repetitions planned plannedUnit actual actualUnit source note");
          add(
              steps,
              v(l, "owner"),
              new WorkupStep(
                  v(l, "id"),
                  WorkupKind.valueOf(v(l, "kind")),
                  n(l, "from"),
                  n(l, "to"),
                  v(l, "material"),
                  Integer.parseInt(v(l, "repetitions")),
                  quantity(l, "planned"),
                  quantity(l, "actual"),
                  v(l, "source"),
                  v(l, "note"),
                  metadata.take(v(l, "owner"), Scope.STEP, v(l, "id"))));
        }
        case "operations-loss-v1" -> {
          keys(l, "owner id label planned plannedUnit actual actualUnit source");
          add(
              losses,
              v(l, "owner"),
              new Loss(
                  v(l, "id"),
                  v(l, "label"),
                  quantity(l, "planned"),
                  quantity(l, "actual"),
                  v(l, "source"),
                  metadata.take(v(l, "owner"), Scope.LOSS, v(l, "id"))));
        }
        case "operations-gradient-v1" -> {
          keys(l, "owner position bPercent flow");
          add(
              gradients,
              v(l, "owner"),
              new GradientPoint(
                  decimal(l, "position"), decimal(l, "bPercent"), decimal(l, "flow")));
        }
        case "operations-fraction-v1" -> {
          keys(
              l,
              "owner id label start end planned plannedUnit actual actualUnit purity sample source note");
          add(
              fractions,
              v(l, "owner"),
              new Fraction(
                  v(l, "id"),
                  v(l, "label"),
                  decimal(l, "start"),
                  decimal(l, "end"),
                  quantity(l, "planned"),
                  quantity(l, "actual"),
                  decimal(l, "purity"),
                  n(l, "sample"),
                  v(l, "source"),
                  v(l, "note"),
                  metadata.take(v(l, "owner"), Scope.FRACTION, v(l, "id"))));
        }
        case "operations-pool-v1" -> {
          keys(l, "owner id label fractions actual actualUnit sample source note");
          add(
              pools,
              v(l, "owner"),
              new Pool(
                  v(l, "id"),
                  v(l, "label"),
                  List.of(v(l, "fractions").split(",", -1)),
                  quantity(l, "actual"),
                  n(l, "sample"),
                  v(l, "source"),
                  v(l, "note"),
                  metadata.take(v(l, "owner"), Scope.POOL, v(l, "id"))));
        }
        case "operations-observation-v1", "operations-analysis-ref-v1" -> {}
        default ->
            throw new IllegalArgumentException(
                "Неизвестная версия записи обработки: " + l.command());
      }
    }
    owners(targets.keySet(), outcomes.keySet());
    owners(byproducts.keySet(), outcomes.keySet());
    owners(phases.keySet(), workups.keySet());
    owners(steps.keySet(), workups.keySet());
    owners(gradients.keySet(), runs.keySet());
    owners(fractions.keySet(), runs.keySet());
    owners(pools.keySet(), runs.keySet());
    final Set<String> all = new HashSet<>(workups.keySet());
    all.addAll(runs.keySet());
    owners(losses.keySet(), all);
    final List<Workup> workupRecords = new ArrayList<>();
    for (final var entry : workups.entrySet()) {
      final var l = entry.getValue();
      final String id = entry.getKey();
      workupRecords.add(
          new Workup(
              id,
              n(l, "stage"),
              v(l, "stageTitle"),
              time(l),
              v(l, "title"),
              phases.getOrDefault(id, List.of()),
              steps.getOrDefault(id, List.of()),
              losses.getOrDefault(id, List.of()),
              v(l, "note"),
              n(l, "corrects")));
    }
    final List<Purification> runRecords = new ArrayList<>();
    for (final var entry : runs.entrySet()) {
      final var l = entry.getValue();
      final String id = entry.getKey();
      runRecords.add(
          new Purification(
              id,
              n(l, "stage"),
              v(l, "stageTitle"),
              time(l),
              v(l, "title"),
              PurificationKind.valueOf(v(l, "kind")),
              n(l, "inputSample"),
              quantity(l, "inputPlanned"),
              quantity(l, "inputActual"),
              v(l, "stationary"),
              quantity(l, "sorbent"),
              v(l, "solventA"),
              v(l, "solventB"),
              GradientAxis.valueOf(v(l, "axis")),
              gradients.getOrDefault(id, List.of()),
              fractions.getOrDefault(id, List.of()),
              pools.getOrDefault(id, List.of()),
              losses.getOrDefault(id, List.of()),
              v(l, "source"),
              v(l, "note"),
              n(l, "corrects"),
              metadata.take(id, Scope.INPUT, "")));
    }
    final List<StageOutcome> outcomeRecords = new ArrayList<>();
    for (final var entry : outcomes.entrySet()) {
      final var l = entry.getValue();
      final String id = entry.getKey();
      outcomeRecords.add(
          new StageOutcome(
              id,
              v(l, "stage"),
              v(l, "stageTitle"),
              time(l),
              OutcomeStatus.valueOf(v(l, "status")),
              targets.getOrDefault(id, List.of()),
              byproducts.getOrDefault(id, List.of()),
              metadata.take(id, Scope.STAGE_OUTCOME, ""),
              v(l, "source"),
              v(l, "reason"),
              n(l, "corrects")));
    }
    metadata.complete();
    return new LaboratoryReactionOperations(points, workupRecords, runRecords, outcomeRecords);
  }

  private static void header(final Map<String, QchemLine> map, final QchemLine line) {
    header(map, line, 1000);
  }

  private static void header(
      final Map<String, QchemLine> map, final QchemLine line, final int maximum) {
    if (map.size() >= maximum || map.putIfAbsent(v(line, "id"), line) != null) {
      throw new IllegalArgumentException("Дубликат или слишком много версий обработки.");
    }
  }

  private static <T> void add(final Map<String, List<T>> map, final String owner, final T value) {
    final var values = map.computeIfAbsent(owner, k -> new ArrayList<>());
    if (values.size() >= 2000) {
      throw new IllegalArgumentException("Слишком много строк одной операции обработки.");
    }
    values.add(value);
  }

  private static void owners(final Set<String> found, final Set<String> known) {
    if (!known.containsAll(found)) {
      throw new IllegalArgumentException("Строка обработки ссылается на отсутствующую запись.");
    }
  }

  private static void keys(final QchemLine line, final String allowed) {
    if (!new HashSet<>(Arrays.asList(allowed.split(" "))).containsAll(line.values().keySet())) {
      throw new IllegalArgumentException("Неизвестное поле записи обработки.");
    }
  }

  private static String v(final QchemLine line, final String key) {
    final var value = line.required(key);
    if (value.length() > 600002) {
      throw new IllegalArgumentException("Поле обработки слишком велико.");
    }
    return QchemEscaper.unquote(value);
  }

  private static String n(final QchemLine line, final String key) {
    final String value = v(line, key);
    return value.isEmpty() ? null : value;
  }

  private static Instant time(final QchemLine line) {
    return Instant.parse(v(line, "time"));
  }

  private static BigDecimal decimal(final QchemLine line, final String key) {
    final var value = v(line, key);
    if (value.isEmpty()) {
      return null;
    }
    if (value.length() > 100) {
      throw new IllegalArgumentException("Слишком длинное число обработки.");
    }
    return new BigDecimal(value);
  }

  private static LaboratoryQuantity quantity(final QchemLine line, final String key) {
    if ((line.optional(key) == null) != (line.optional(key + "Unit") == null)) {
      throw new IllegalArgumentException("Количество и единица обработки задаются вместе.");
    }
    return line.optional(key) == null
        ? null
        : new LaboratoryQuantity(
            decimal(line, key), LaboratoryQuantityUnit.valueOf(v(line, key + "Unit")));
  }
}
