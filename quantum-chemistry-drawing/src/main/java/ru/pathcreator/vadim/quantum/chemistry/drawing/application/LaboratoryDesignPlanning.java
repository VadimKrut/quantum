/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperimentalDesign.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Reproducible full factorial planning and least-squares effects on coded factors, with explicit rank diagnostics. */
public final class LaboratoryDesignPlanning {

  public record Term(String id, String label, List<String> factors) {
    public Term {
      factors = List.copyOf(factors);
    }
  }

  public record Effect(
      String id, String label, double coefficient, double effect, Double standardError) {}

  public record Fitted(String runId, double observed, double fitted, double residual) {}

  public record Analysis(
      int observations,
      int parameters,
      int rank,
      int residualDf,
      Double residualRmse,
      Double pureErrorSd,
      int pureErrorDf,
      List<Effect> effects,
      List<Fitted> fitted,
      List<String> diagnostics) {
    public Analysis {
      effects = List.copyOf(effects);
      fitted = List.copyOf(fitted);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  private LaboratoryDesignPlanning() {}

  public static LaboratoryExperimentalDesign generate(
      final LaboratoryExperimentalDesign settings,
      final int replicates,
      final int centers,
      final boolean randomize) {
    if (replicates < 1 || replicates > 16 || centers < 0 || centers > 32) {
      throw new IllegalArgumentException("Повторности: 1–16; центральные точки: 0–32.");
    }
    final int combinations = 1 << settings.factors().size();
    if (combinations * replicates + centers > 256) {
      throw new IllegalArgumentException(
          "Матрица превышает 256 запусков. Уменьшите число факторов или повторов.");
    }
    if (centers > 0 && settings.factors().stream().anyMatch(f -> f.type() != Type.NUMERIC)) {
      throw new IllegalArgumentException(
          "Центральные точки допустимы только для полностью числового набора факторов.");
    }
    final var runs = new ArrayList<Run>();
    for (int rep = 1; rep <= replicates; rep++) {
      for (int mask = 0; mask < combinations; mask++) {
        final var levels = new LinkedHashMap<String, String>();
        for (int i = 0; i < settings.factors().size(); i++) {
          final var f = settings.factors().get(i);
          levels.put(f.id(), (mask & 1 << i) == 0 ? f.low() : f.high());
        }
        runs.add(
            new Run(
                "run_" + (mask + 1) + "_" + rep,
                mask + 1,
                rep,
                false,
                levels,
                "",
                "",
                "",
                null,
                ""));
      }
    }
    for (int rep = 1; rep <= centers; rep++) {
      final var levels = new LinkedHashMap<String, String>();
      for (final var f : settings.factors()) {
        levels.put(
            f.id(),
            LaboratoryExperimentalDesign.number(f.low())
                .add(LaboratoryExperimentalDesign.number(f.high()))
                .divide(BigDecimal.valueOf(2))
                .stripTrailingZeros()
                .toPlainString());
      }
      runs.add(new Run("center_" + rep, 0, rep, true, levels, "", "", "", null, ""));
    }
    if (randomize) {
      Collections.shuffle(runs, new Random(settings.seed()));
    }
    final var located = new ArrayList<Run>();
    for (int i = 0; i < runs.size(); i++) {
      final var run = runs.get(i);
      located.add(
          new Run(
              run.id(),
              run.standardOrder(),
              run.replicate(),
              run.center(),
              run.levels(),
              position(i, settings.plateRows(), settings.plateColumns()),
              "",
              "",
              null,
              ""));
    }
    return settings.withRuns(located);
  }

  public static String position(final int index, final int rows, final int columns) {
    final int capacity = rows * columns, local = index % capacity;
    return "P"
        + (index / capacity + 1)
        + "-"
        + (char) ('A' + local / columns)
        + String.format(Locale.ROOT, "%02d", local % columns + 1);
  }

  public static List<LaboratorySeriesPlan.Prepared> prepare(
      final LaboratoryDrawing source, final LaboratoryExperimentalDesign design) {
    return prepare(source, design, () -> false);
  }

  public static List<LaboratorySeriesPlan.Prepared> prepare(
      final LaboratoryDrawing source,
      final LaboratoryExperimentalDesign design,
      final java.util.function.BooleanSupplier cancelled) {
    final var stage =
        source.experiment().stages().stream()
            .filter(s -> s.id().equals(design.stageId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Стадия шаблона не найдена."));
    final var defaults = LaboratorySeriesPlan.defaults(source, stage);
    final var result = new ArrayList<LaboratorySeriesPlan.Prepared>();
    if (design.runs().isEmpty()) {
      throw new IllegalArgumentException("Сначала сформируйте матрицу запусков.");
    }
    for (int i = 0; i < design.runs().size(); i++) {
      if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException(
            "Проверка матрицы остановлена; опыты не создавались.");
      }
      final var run = design.runs().get(i);
      Temperature temperature = defaults.temperature();
      TimeDuration duration = defaults.duration();
      var quantity = design.baseQuantity();
      final var equivalents = new LinkedHashMap<String, BigDecimal>();
      final var notes = new ArrayList<String>();
      for (final var factor : design.factors()) {
        final String level = run.levels().get(factor.id());
        notes.add(
            factor.title() + " = " + level + (factor.unit().isBlank() ? "" : " " + factor.unit()));
        switch (factor.binding()) {
          case TEMPERATURE_C ->
              temperature =
                  Temperature.of(
                      LaboratoryExperimentalDesign.number(level).doubleValue(),
                      TemperatureUnit.CELSIUS);
          case DURATION_HOURS ->
              duration =
                  TimeDuration.of(
                      LaboratoryExperimentalDesign.number(level).doubleValue(),
                      TimeDurationUnit.HOUR);
          case EQUIVALENTS ->
              equivalents.put(factor.componentId(), LaboratoryExperimentalDesign.number(level));
          case LOADING_MULTIPLIER ->
              quantity =
                  new LaboratoryQuantity(
                      quantity.value().multiply(LaboratoryExperimentalDesign.number(level)),
                      quantity.unit());
          case CONDITION_NOTE -> {}
        }
      }
      final String label =
          String.format(Locale.ROOT, "%03d", i + 1)
              + " · "
              + design.title()
              + " · "
              + run.position();
      final String note =
          "План эксперимента "
              + design.id()
              + "; запуск "
              + run.id()
              + "; позиция "
              + run.position()
              + ".\n"
              + String.join("; ", notes);
      final String conditionsNote =
          "Параметры варианта: "
              + String.join("; ", notes)
              + (defaults.note() == null
                  ? ""
                  : "\n\nИсходная запись условий (справочно):\n" + defaults.note());
      final var conditions =
          ReactionConditions.of(
              temperature,
              defaults.pressure(),
              duration,
              defaults.components(),
              defaults.features(),
              conditionsNote);
      final var prepared =
          LaboratorySeriesPlan.prepare(
              source,
              stage.id(),
              design.referenceId(),
              new LaboratorySeriesPlan.Variant(label, quantity, conditions, equivalents, note),
              design.scaleSolvents());
      final var warnings = new ArrayList<>(prepared.warnings());
      if (design.factors().stream().anyMatch(f -> f.binding() == Binding.CONDITION_NOTE)) {
        warnings.add(
            "Факторы условий записаны в методике. Состав и структуры веществ автоматически не заменяются; проверьте их в созданном опыте.");
      }
      result.add(
          new LaboratorySeriesPlan.Prepared(prepared.drawing(), prepared.theory(), warnings));
    }
    return List.copyOf(result);
  }

  public static List<Term> terms(
      final LaboratoryExperimentalDesign design, final boolean interactions) {
    final var result = new ArrayList<Term>();
    for (final var f : design.factors()) {
      result.add(new Term(f.id(), f.title(), List.of(f.id())));
    }
    if (interactions) {
      for (int i = 0; i < design.factors().size(); i++) {
        for (int j = i + 1; j < design.factors().size(); j++) {
          final var a = design.factors().get(i);
          final var b = design.factors().get(j);
          result.add(
              new Term(
                  a.id() + ":" + b.id(), a.title() + " × " + b.title(), List.of(a.id(), b.id())));
        }
      }
    }
    return List.copyOf(result);
  }

  /** QR with reorthogonalization avoids normal-equation inversion. Pure-error uncertainty needs actual replication. */
  public static Analysis analyze(
      final LaboratoryExperimentalDesign design, final List<Term> selected) {
    final var available = new LinkedHashMap<String, Term>();
    terms(design, true).forEach(t -> available.put(t.id(), t));
    final var seen = new HashSet<String>();
    for (final var term : selected) {
      if (!term.equals(available.get(term.id())) || !seen.add(term.id())) {
        throw new IllegalArgumentException("Выбран неизвестный или повторяющийся член модели.");
      }
    }
    final var data = design.runs().stream().filter(r -> r.response() != null).toList();
    final int n = data.size(), p = selected.size() + 1;
    final var diagnostics = new ArrayList<String>();
    diagnostics.add(
        "Линейная модель в кодированных координатах −1…+1. Эффект = 2 × коэффициент; это описание введённых откликов, не доказательство причинности и не найденный оптимум.");
    diagnostics.addAll(LaboratoryDesignResponses.diagnostics(design));
    if (n < p) {
      diagnostics.add(
          "Наблюдений " + n + ", параметров " + p + ": данных недостаточно для выбранной модели.");
      return new Analysis(n, p, 0, n - p, null, null, 0, List.of(), List.of(), diagnostics);
    }
    final var factors = new LinkedHashMap<String, Factor>();
    design.factors().forEach(f -> factors.put(f.id(), f));
    final double[][] x = new double[n][p], q = new double[n][p], r = new double[p][p];
    final double[] y = new double[n];
    final var groups = new LinkedHashMap<List<String>, List<Double>>();
    for (int i = 0; i < n; i++) {
      final var run = data.get(i);
      x[i][0] = 1;
      y[i] = run.response().doubleValue();
      final var grouping = new ArrayList<String>();
      for (final var f : design.factors()) {
        grouping.add(
            f.type() == Type.NUMERIC
                ? LaboratoryExperimentalDesign.number(run.levels().get(f.id())).toPlainString()
                : run.levels().get(f.id()));
      }
      groups.computeIfAbsent(List.copyOf(grouping), key -> new ArrayList<>()).add(y[i]);
      for (int j = 0; j < selected.size(); j++) {
        double value = 1;
        for (final String id : selected.get(j).factors()) {
          value *= factors.get(id).coded(run.levels().get(id)).doubleValue();
        }
        x[i][j + 1] = value;
      }
    }
    int rank = 0;
    for (int j = 0; j < p; j++) {
      final double[] v = new double[n];
      for (int i = 0; i < n; i++) {
        v[i] = x[i][j];
      }
      for (int pass = 0; pass < 2; pass++) {
        for (int k = 0; k < j; k++) {
          double projection = 0;
          for (int i = 0; i < n; i++) {
            projection += q[i][k] * v[i];
          }
          r[k][j] += projection;
          for (int i = 0; i < n; i++) {
            v[i] -= projection * q[i][k];
          }
        }
      }
      double norm = 0;
      for (final double value : v) {
        norm = Math.hypot(norm, value);
      }
      if (norm <= 1e-10 * Math.sqrt(n)) {
        diagnostics.add(
            "Матрица модели не имеет полного ранга: фактор или взаимодействие неотделимы от других членов. Измените модель или дополните наблюдения.");
        return new Analysis(n, p, rank, n - p, null, null, 0, List.of(), List.of(), diagnostics);
      }
      r[j][j] = norm;
      for (int i = 0; i < n; i++) {
        q[i][j] = v[i] / norm;
      }
      rank++;
    }
    final double[] qty = new double[p];
    for (int j = 0; j < p; j++) {
      for (int i = 0; i < n; i++) {
        qty[j] += q[i][j] * y[i];
      }
    }
    final double[] coefficients = solve(r, qty);
    final var fitted = new ArrayList<Fitted>();
    double sse = 0;
    for (int i = 0; i < n; i++) {
      double fit = 0;
      for (int j = 0; j < p; j++) {
        fit += x[i][j] * coefficients[j];
      }
      final double residual = y[i] - fit;
      sse += residual * residual;
      fitted.add(new Fitted(data.get(i).id(), y[i], fit, residual));
    }
    int pureDf = 0;
    double pureSse = 0;
    for (final var values : groups.values()) {
      if (values.size() > 1) {
        final double mean =
            values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        pureDf += values.size() - 1;
        for (final double value : values) {
          pureSse += (value - mean) * (value - mean);
        }
      }
    }
    final Double pureVariance = pureDf == 0 ? null : pureSse / pureDf;
    final Double rmse = n > p ? Math.sqrt(sse / (n - p)) : null;
    if (pureDf == 0) {
      diagnostics.add(
          "Нет повторных откликов при одинаковых уровнях факторов: чистая ошибка и стандартные ошибки эффектов не оценены.");
    } else {
      diagnostics.add(
          "Стандартные ошибки используют разброс независимых повторных запусков при одинаковых условиях и предположение одинаковой дисперсии. Повторное считывание одного образца не является независимым повтором синтеза.");
    }
    if (n == p) {
      diagnostics.add(
          "Насыщенная модель: остаточных степеней свободы нет; точное совпадение с данными не подтверждает модель.");
    }
    if (design.runs().stream().anyMatch(Run::center)) {
      diagnostics.add(
          "Центральные точки включены в линейную модель; отдельные квадратичные коэффициенты по такой матрице не определяются.");
    }
    final double[][] inverse = new double[p][p];
    for (int j = 0; j < p; j++) {
      final double[] basis = new double[p];
      basis[j] = 1;
      final double[] column = solve(r, basis);
      for (int i = 0; i < p; i++) {
        inverse[i][j] = column[i];
      }
    }
    final var effects = new ArrayList<Effect>();
    for (int j = 0; j < p; j++) {
      double diagonal = 0;
      for (int k = 0; k < p; k++) {
        diagonal += inverse[j][k] * inverse[j][k];
      }
      final double scale = j == 0 ? 1 : 2;
      effects.add(
          new Effect(
              j == 0 ? "intercept" : selected.get(j - 1).id(),
              j == 0 ? "Средний уровень (пересечение)" : selected.get(j - 1).label(),
              coefficients[j],
              scale * coefficients[j],
              pureVariance == null ? null : scale * Math.sqrt(pureVariance * diagonal)));
    }
    return new Analysis(
        n,
        p,
        rank,
        n - p,
        rmse,
        pureVariance == null ? null : Math.sqrt(pureVariance),
        pureDf,
        effects,
        fitted,
        diagnostics);
  }

  private static double[] solve(final double[][] r, final double[] right) {
    final double[] result = new double[right.length];
    for (int i = result.length - 1; i >= 0; i--) {
      double value = right[i];
      for (int j = i + 1; j < result.length; j++) {
        value -= r[i][j] * result[j];
      }
      result[i] = value / r[i][i];
    }
    return result;
  }
}
