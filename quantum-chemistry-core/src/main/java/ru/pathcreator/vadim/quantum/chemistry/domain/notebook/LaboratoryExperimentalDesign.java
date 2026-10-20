/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.AnalysisRef;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.Origin;

/** A local experimental design. Responses retain explicit provenance and are never generated from plans. */
public record LaboratoryExperimentalDesign(
    String id,
    String title,
    String stageId,
    String referenceId,
    LaboratoryQuantity baseQuantity,
    List<Factor> factors,
    List<Run> runs,
    long seed,
    String responseName,
    String responseUnit,
    boolean scaleSolvents,
    int plateRows,
    int plateColumns) {

  public enum Type {
    NUMERIC,
    CATEGORICAL
  }

  public enum Binding {
    TEMPERATURE_C("Температура, °C"),
    DURATION_HOURS("Время, ч"),
    EQUIVALENTS("Эквиваленты вещества"),
    LOADING_MULTIPLIER("Масштаб загрузки"),
    CONDITION_NOTE("Условие / категория");
    private final String label;

    Binding(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public record Factor(
      String id,
      String title,
      Type type,
      Binding binding,
      String componentId,
      String unit,
      String low,
      String high) {
    public Factor {
      id = identifier(id);
      title = text(title, 200, false);
      Objects.requireNonNull(type);
      Objects.requireNonNull(binding);
      componentId = componentId == null ? null : identifier(componentId);
      unit = text(unit, 50, true);
      low = text(low, 300, false);
      high = text(high, 300, false);
      if ((binding == Binding.EQUIVALENTS) != (componentId != null)) {
        throw new IllegalArgumentException(
            "Для фактора эквивалентов выберите компонент; другим факторам связь с компонентом не нужна.");
      }
      if (type == Type.CATEGORICAL && binding != Binding.CONDITION_NOTE) {
        throw new IllegalArgumentException(
            "Категориальный фактор записывается как условие. Он не заменяет структуру вещества автоматически.");
      }
      if (type == Type.NUMERIC) {
        final BigDecimal a = number(low), b = number(high);
        if (a.compareTo(b) >= 0) {
          throw new IllegalArgumentException(
              "Низкий числовой уровень должен быть меньше высокого.");
        }
        validateLevel(binding, a);
        validateLevel(binding, b);
        low = a.toPlainString();
        high = b.toPlainString();
      } else if (low.equals(high)) {
        throw new IllegalArgumentException("Два категориальных уровня должны различаться.");
      }
    }

    public BigDecimal coded(final String value) {
      if (this.type == Type.CATEGORICAL) {
        if (this.low.equals(value)) {
          return BigDecimal.ONE.negate();
        }
        if (this.high.equals(value)) {
          return BigDecimal.ONE;
        }
        throw new IllegalArgumentException(
            "«" + this.title + "»: выберите один из двух заданных уровней.");
      }
      final BigDecimal x = number(value), a = number(this.low), b = number(this.high);
      if (x.compareTo(a) < 0 || x.compareTo(b) > 0) {
        throw new IllegalArgumentException(
            "«" + this.title + "»: значение должно быть между заданными уровнями.");
      }
      return x.multiply(BigDecimal.valueOf(2))
          .subtract(a)
          .subtract(b)
          .divide(b.subtract(a), MathContext.DECIMAL128);
    }
  }

  public record Run(
      String id,
      int standardOrder,
      int replicate,
      boolean center,
      Map<String, String> levels,
      String position,
      String experimentFile,
      String experimentId,
      BigDecimal response,
      String responseNote,
      Origin responseOrigin,
      String responseSource,
      AnalysisRef responseAnalysis) {
    /** Historical response values have unknown provenance; loading them does not turn them into measurements. */
    public Run(
        final String id,
        final int standardOrder,
        final int replicate,
        final boolean center,
        final Map<String, String> levels,
        final String position,
        final String experimentFile,
        final String experimentId,
        final BigDecimal response,
        final String responseNote) {
      this(
          id,
          standardOrder,
          replicate,
          center,
          levels,
          position,
          experimentFile,
          experimentId,
          response,
          responseNote,
          Origin.UNSPECIFIED,
          "",
          null);
    }

    public Run {
      id = identifier(id);
      if (standardOrder < 0 || standardOrder > 64 || replicate < 1 || replicate > 256) {
        throw new IllegalArgumentException("Неверный номер точки или повторности.");
      }
      levels = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(levels));
      position = text(position, 40, true);
      experimentFile = text(experimentFile, 100, true);
      experimentId = text(experimentId, 200, true);
      responseNote = text(responseNote, 10000, true);
      if (experimentFile.isEmpty() != experimentId.isEmpty()) {
        throw new IllegalArgumentException(
            "Файл и идентификатор созданного опыта задаются вместе.");
      }
      if (!experimentFile.isEmpty()
          && (!experimentFile.matches("[A-Za-z0-9_-]+\\.qchem")
              || experimentFile.startsWith("."))) {
        throw new IllegalArgumentException("Неверное локальное имя опыта матрицы.");
      }
      if (!experimentId.isEmpty()) {
        experimentId = identifier(experimentId);
      }
      if (response != null) {
        response = number(response.toString());
      }
      Objects.requireNonNull(responseOrigin, "Происхождение отклика");
      responseSource = text(responseSource, 10000, true);
      if (response != null && responseOrigin != Origin.UNSPECIFIED && responseSource.isBlank()) {
        throw new IllegalArgumentException(
            "Укажите источник отклика: измерение, литературная запись или модельный расчёт.");
      }
      if (responseAnalysis != null && experimentId.isEmpty()) {
        throw new IllegalArgumentException(
            "Точная ссылка на анализ требует связанного опыта этого запуска.");
      }
    }
  }

  public LaboratoryExperimentalDesign {
    id = identifier(id);
    title = text(title, 300, false);
    stageId = identifier(stageId);
    referenceId = identifier(referenceId);
    Objects.requireNonNull(baseQuantity);
    if (baseQuantity.value().signum() <= 0) {
      throw new IllegalArgumentException("Базовая загрузка должна быть положительной.");
    }
    number(baseQuantity.value().toString());
    factors = List.copyOf(factors);
    runs = List.copyOf(runs);
    responseName = text(responseName, 200, false);
    responseUnit = text(responseUnit, 50, false);
    if (factors.isEmpty() || factors.size() > 6 || runs.size() > 256) {
      throw new IllegalArgumentException("Поддерживаются 1–6 факторов и до 256 запусков.");
    }
    if (plateRows < 1 || plateRows > 26 || plateColumns < 1 || plateColumns > 48) {
      throw new IllegalArgumentException("Размер штатива: 1–26 рядов и 1–48 колонок.");
    }
    final var factorIds = new HashSet<String>();
    final var mapped = new HashSet<String>();
    for (final var factor : factors) {
      if (!factorIds.add(factor.id())) {
        throw new IllegalArgumentException("Идентификатор фактора повторяется.");
      }
      if (factor.binding() != Binding.CONDITION_NOTE
          && !mapped.add(factor.binding().name() + ":" + factor.componentId())) {
        throw new IllegalArgumentException(
            "Один параметр методики нельзя менять двумя факторами одновременно.");
      }
    }
    final var runIds = new HashSet<String>();
    final var positions = new HashSet<String>();
    final var files = new HashSet<String>();
    for (final var run : runs) {
      if (!runIds.add(run.id())) {
        throw new IllegalArgumentException("Повтор идентификатора запуска.");
      }
      if (!run.position().isEmpty() && !positions.add(run.position())) {
        throw new IllegalArgumentException("Позиция «" + run.position() + "» занята дважды.");
      }
      if (!run.experimentFile().isEmpty() && !files.add(run.experimentFile())) {
        throw new IllegalArgumentException("Один опыт связан с несколькими запусками.");
      }
      if (!run.levels().keySet().equals(factorIds)) {
        throw new IllegalArgumentException(
            "У каждого запуска должен быть уровень каждого фактора.");
      }
      for (final var factor : factors) {
        final var coded = factor.coded(run.levels().get(factor.id()));
        if (run.center() && (factor.type() != Type.NUMERIC || coded.signum() != 0)) {
          throw new IllegalArgumentException(
              "Центральная точка должна содержать середины всех числовых факторов.");
        }
      }
    }
  }

  public LaboratoryExperimentalDesign withRuns(final List<Run> values) {
    return new LaboratoryExperimentalDesign(
        this.id,
        this.title,
        this.stageId,
        this.referenceId,
        this.baseQuantity,
        this.factors,
        values,
        this.seed,
        this.responseName,
        this.responseUnit,
        this.scaleSolvents,
        this.plateRows,
        this.plateColumns);
  }

  public static BigDecimal number(final String text) {
    try {
      if (text == null || text.length() > 100) {
        throw new NumberFormatException();
      }
      final var value = new BigDecimal(text.strip().replace(',', '.'));
      if (value.precision() > 34
          || Math.abs((long) value.scale()) > 16
          || value.abs().compareTo(new BigDecimal("1E12")) > 0) {
        throw new NumberFormatException();
      }
      return value.stripTrailingZeros();
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Введите конечное число: до 34 значащих цифр, модуль не более 10¹².");
    }
  }

  private static void validateLevel(final Binding binding, final BigDecimal value) {
    if (binding == Binding.TEMPERATURE_C
            && (value.compareTo(new BigDecimal("-273.15")) < 0
                || value.compareTo(new BigDecimal("10000")) > 0)
        || binding == Binding.DURATION_HOURS
            && (value.signum() <= 0 || value.compareTo(new BigDecimal("8760")) > 0)
        || binding == Binding.LOADING_MULTIPLIER && value.signum() <= 0
        || binding == Binding.EQUIVALENTS && value.signum() < 0) {
      throw new IllegalArgumentException(
          "Уровень фактора выходит за допустимый диапазон параметра.");
    }
  }

  private static String identifier(final String value) {
    if (value == null || value.length() > 200) {
      throw new IllegalArgumentException("Неверный идентификатор плана эксперимента.");
    }
    return IdentifierValue.requireIdentifier(value, "Идентификатор плана эксперимента");
  }

  private static String text(final String value, final int limit, final boolean empty) {
    Objects.requireNonNull(value);
    final String result = value.strip();
    if ((!empty && result.isBlank()) || result.length() > limit) {
      throw new IllegalArgumentException("Пустое или слишком длинное поле плана эксперимента.");
    }
    return result;
  }
}
