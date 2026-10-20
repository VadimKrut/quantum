/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;

/** Preparation recipes and explicitly sourced stock concentrations; never a second quantity ledger. */
public record LaboratorySolutionBook(List<Recipe> recipes, List<Standardization> standardizations) {
  public static final LaboratorySolutionBook EMPTY =
      new LaboratorySolutionBook(List.of(), List.of());

  public enum Mode {
    SOLID("Из навески"),
    DILUTION("Разведение раствора");
    private final String text;

    Mode(String text) {
      this.text = text;
    }

    public String toString() {
      return this.text;
    }
  }

  public enum ConcentrationUnit {
    MOL_PER_LITRE("моль/л", "molar", "1"),
    MMOL_PER_LITRE("ммоль/л", "molar", "0.001"),
    MICROMOL_PER_LITRE("мкмоль/л", "molar", "0.000001"),
    GRAM_PER_LITRE("г/л", "mass", "1"),
    MILLIGRAM_PER_MILLILITRE("мг/мл", "mass", "1"),
    GRAM_PER_100ML("г/100 мл (% m/V)", "mass", "10");
    private final String label, family;
    private final BigDecimal factor;

    ConcentrationUnit(String label, String family, String factor) {
      this.label = label;
      this.family = family;
      this.factor = new BigDecimal(factor);
    }

    public String family() {
      return this.family;
    }

    public BigDecimal factor() {
      return this.factor;
    }

    public String toString() {
      return this.label;
    }
  }

  public record Concentration(BigDecimal value, ConcentrationUnit unit) {
    public Concentration {
      value = positive(value);
      Objects.requireNonNull(unit);
    }

    public BigDecimal base() {
      return this.value.multiply(this.unit.factor());
    }

    public String toString() {
      return this.value.toPlainString() + " " + this.unit;
    }
  }

  public record DilutionStep(String label, Concentration target, LaboratoryQuantity finalVolume) {
    public DilutionStep {
      label = text(label, 300, true);
      Objects.requireNonNull(target);
      volume(finalVolume);
    }
  }

  public record Recipe(
      String id,
      Instant time,
      String title,
      String batchId,
      Mode mode,
      String solvent,
      Concentration target,
      LaboratoryQuantity finalVolume,
      BigDecimal molarMass,
      BigDecimal purityPercent,
      Concentration stock,
      List<DilutionStep> series,
      String note,
      String correctsId) {
    public Recipe {
      id = identifier(id);
      Objects.requireNonNull(time);
      title = text(title, 300, true);
      batchId = optional(batchId);
      Objects.requireNonNull(mode);
      solvent = text(solvent, 1000, true);
      Objects.requireNonNull(target);
      volume(finalVolume);
      if (molarMass != null) {
        molarMass = positive(molarMass);
      }
      if (purityPercent != null) {
        purityPercent = positive(purityPercent);
        if (purityPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
          throw new IllegalArgumentException("Чистота не может превышать 100 %.");
        }
      }
      if (mode == Mode.SOLID
          && (purityPercent == null
              || target.unit().family().equals("molar") && molarMass == null)) {
        throw new IllegalArgumentException(
            "Для навески задайте чистоту, а для молярности также молярную массу.");
      }
      if (mode == Mode.DILUTION && stock == null) {
        throw new IllegalArgumentException("Для разведения укажите исходную концентрацию.");
      }
      series = List.copyOf(series);
      if (series.size() > 100) {
        throw new IllegalArgumentException("Поддерживается до 100 последовательных разведений.");
      }
      note = text(note, 10000, false);
      correctsId = optional(correctsId);
    }
  }

  public enum Origin {
    UNSPECIFIED("Происхождение не указано"),
    MEASURED("Собственное измерение"),
    LITERATURE("Литературные данные"),
    SIMULATED("Расчёт / модель");
    private final String label;

    Origin(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public record Standardization(
      String id,
      String batchId,
      Instant time,
      Concentration measured,
      String method,
      String source,
      String calculation,
      String correctsId,
      Origin origin) {
    /** Legacy callers carry no typed provenance; their source text is never interpreted as a measurement. */
    public Standardization(
        String id,
        String batchId,
        Instant time,
        Concentration measured,
        String method,
        String source,
        String calculation,
        String correctsId) {
      this(
          id, batchId, time, measured, method, source, calculation, correctsId, Origin.UNSPECIFIED);
    }

    public Standardization {
      Objects.requireNonNull(origin);
      id = identifier(id);
      batchId = identifier(batchId);
      Objects.requireNonNull(time);
      Objects.requireNonNull(measured);
      method = text(method, 500, true);
      source = text(source, 3000, true);
      calculation = text(calculation, 10000, false);
      correctsId = optional(correctsId);
    }
  }

  public LaboratorySolutionBook {
    recipes = List.copyOf(recipes);
    standardizations = List.copyOf(standardizations);
    if (recipes.size() > 2000 || standardizations.size() > 10000) {
      throw new IllegalArgumentException(
          "Книга растворов превышает 2000 версий методик или 10000 стандартизаций.");
    }
    final Set<String> ids = new HashSet<>(), corrected = new HashSet<>();
    final Map<String, Recipe> old = new HashMap<>();
    long characters = 0;
    for (final var r : recipes) {
      if (!ids.add(r.id())) {
        throw new IllegalArgumentException("Повтор идентификатора методики.");
      }
      if (r.correctsId() != null
          && (!old.containsKey(r.correctsId())
              || !corrected.add(r.correctsId())
              || r.time().isBefore(old.get(r.correctsId()).time()))) {
        throw new IllegalArgumentException(
            "Новая версия должна ссылаться на последнюю предыдущую методику.");
      }
      old.put(r.id(), r);
      characters += r.title().length() + r.solvent().length() + r.note().length();
      for (final var step : r.series()) {
        characters += step.label().length();
      }
    }
    final Map<String, Standardization> facts = new HashMap<>();
    corrected.clear();
    for (final var s : standardizations) {
      if (!ids.add(s.id())) {
        throw new IllegalArgumentException("Повтор идентификатора стандартизации.");
      }
      if (s.correctsId() != null) {
        final var previous = facts.get(s.correctsId());
        if (previous == null
            || !previous.batchId().equals(s.batchId())
            || !corrected.add(s.correctsId())
            || s.time().isBefore(previous.time())) {
          throw new IllegalArgumentException(
              "Исправление относится к предыдущей стандартизации той же партии.");
        }
      }
      facts.put(s.id(), s);
      characters += s.method().length() + s.source().length() + s.calculation().length();
    }
    if (characters > 8000000) {
      throw new IllegalArgumentException("Книга растворов превышает 8 миллионов символов.");
    }
  }

  public List<Recipe> activeRecipes() {
    final Set<String> old = new HashSet<>();
    this.recipes.forEach(
        r -> {
          if (r.correctsId() != null) {
            old.add(r.correctsId());
          }
        });
    return this.recipes.stream().filter(r -> !old.contains(r.id())).toList();
  }

  public Optional<Standardization> latest(final String batch) {
    final Set<String> old = new HashSet<>();
    this.standardizations.forEach(
        r -> {
          if (r.correctsId() != null) {
            old.add(r.correctsId());
          }
        });
    return this.standardizations.stream()
        .filter(s -> s.batchId().equals(batch) && !old.contains(s.id()))
        .max(Comparator.comparing(Standardization::time));
  }

  public LaboratorySolutionBook append(final Recipe value) {
    final var next = new ArrayList<>(this.recipes);
    next.add(value);
    return new LaboratorySolutionBook(next, this.standardizations);
  }

  public LaboratorySolutionBook append(final Standardization value) {
    final var next = new ArrayList<>(this.standardizations);
    next.add(value);
    return new LaboratorySolutionBook(this.recipes, next);
  }

  public static void requireAppend(
      final LaboratorySolutionBook previous, final LaboratorySolutionBook next) {
    if (next.recipes.size() < previous.recipes.size()
        || !next.recipes.subList(0, previous.recipes.size()).equals(previous.recipes)
        || next.standardizations.size() < previous.standardizations.size()
        || !next.standardizations
            .subList(0, previous.standardizations.size())
            .equals(previous.standardizations)) {
      throw new IllegalArgumentException("Историю растворов нельзя переписать или удалить.");
    }
  }

  public static BigDecimal positive(final BigDecimal value) {
    Objects.requireNonNull(value);
    if (value.signum() <= 0 || value.precision() > 30 || Math.abs((long) value.scale()) > 18) {
      throw new IllegalArgumentException("Введите положительное число в поддерживаемом диапазоне.");
    }
    final var n = value.stripTrailingZeros();
    if (Math.abs((long) n.scale()) > 18) {
      throw new IllegalArgumentException("Слишком большой показатель степени.");
    }
    return n;
  }

  public static void volume(final LaboratoryQuantity value) {
    if (value == null || !value.unit().dimension().equals("volume")) {
      throw new IllegalArgumentException("Конечное количество раствора задаётся объёмом.");
    }
    positive(value.value());
  }

  private static String identifier(final String value) {
    final var result = IdentifierValue.requireIdentifier(value, "Идентификатор раствора");
    if (result.length() > 160) {
      throw new IllegalArgumentException("Слишком длинный идентификатор.");
    }
    return result;
  }

  private static String optional(final String value) {
    return value == null ? null : identifier(value);
  }

  private static String text(final String value, final int max, final boolean required) {
    Objects.requireNonNull(value);
    if (value.length() > max || required && value.isBlank()) {
      throw new IllegalArgumentException("Заполните поле раствора в допустимой длине.");
    }
    return value.strip();
  }
}
