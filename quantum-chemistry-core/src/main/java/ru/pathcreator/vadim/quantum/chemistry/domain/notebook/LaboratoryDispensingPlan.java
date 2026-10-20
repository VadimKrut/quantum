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
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.Concentration;

/** A frozen dispensing proposal; it contains no physical stock balance or measured operation. */
public record LaboratoryDispensingPlan(
    String id,
    String title,
    Instant createdAt,
    String designId,
    String designHash,
    List<Run> runs,
    List<Ingredient> ingredients,
    String referenceRunId,
    LaboratoryQuantity referenceAliquot,
    BigDecimal reservePercent,
    LaboratoryQuantity deadVolume,
    String diluent,
    String compatibilityBasis,
    String note) {
  public record Run(
      String id, String position, boolean included, Map<String, LaboratoryQuantity> amounts) {
    public Run {
      id = identifier(id);
      position = text(position, 200, true);
      amounts = Collections.unmodifiableMap(new LinkedHashMap<>(amounts));
      if (amounts.size() > 128)
        throw new IllegalArgumentException("Не более 128 компонентов в раздаче.");
      amounts.forEach(
          (key, value) -> {
            identifier(key);
            calculatedQuantity(value);
          });
    }
  }

  public record Ingredient(
      String componentId,
      boolean common,
      Concentration stock,
      String label,
      String solvent,
      String source) {
    public Ingredient {
      componentId = identifier(componentId);
      label = text(label, 500, stock != null);
      solvent = text(solvent, 1000, stock != null);
      source = text(source, 4000, stock != null);
      if (common && stock == null)
        throw new IllegalArgumentException(
            "Общая смесь требует известную концентрацию исходного раствора.");
    }
  }

  public LaboratoryDispensingPlan {
    id = identifier(id);
    title = text(title, 500, true);
    Objects.requireNonNull(createdAt);
    designId = identifier(designId);
    if (designHash == null || !designHash.matches("[0-9a-f]{64}"))
      throw new IllegalArgumentException("Нужна контрольная сумма исходного исследования.");
    runs = List.copyOf(runs);
    ingredients = List.copyOf(ingredients);
    if (runs.isEmpty() || runs.size() > 256 || ingredients.isEmpty() || ingredients.size() > 128)
      throw new IllegalArgumentException("План содержит 1–256 запусков и 1–128 компонентов.");
    unique(runs.stream().map(Run::id).toList());
    unique(runs.stream().map(Run::position).toList());
    unique(ingredients.stream().map(Ingredient::componentId).toList());
    final Set<String> keys =
        new HashSet<>(ingredients.stream().map(Ingredient::componentId).toList());
    for (final var run : runs) {
      if (!keys.containsAll(run.amounts().keySet()))
        throw new IllegalArgumentException(
            "Количество ссылается на неизвестный компонент раздачи.");
    }
    referenceRunId = identifier(referenceRunId);
    final String reference = referenceRunId;
    if (runs.stream().noneMatch(run -> run.id().equals(reference)))
      throw new IllegalArgumentException("Опорный запуск отсутствует в матрице.");
    reservePercent = number(reservePercent);
    if (reservePercent.signum() < 0 || reservePercent.compareTo(new BigDecimal("1000")) > 0)
      throw new IllegalArgumentException("Резерв задаётся от 0 до 1000 % объёма раздачи.");
    volume(deadVolume, false);
    if (referenceAliquot != null) volume(referenceAliquot, true);
    diluent = text(diluent, 1000, false);
    compatibilityBasis = text(compatibilityBasis, 8000, false);
    note = text(note, 10000, false);
    if (ingredients.stream().anyMatch(Ingredient::common)
        && (referenceAliquot == null || diluent.isBlank() || compatibilityBasis.isBlank()))
      throw new IllegalArgumentException(
          "Для общей смеси укажите объём опорной аликвоты, растворитель доведения и основание совместного приготовления.");
  }

  public static BigDecimal number(final BigDecimal value) {
    Objects.requireNonNull(value);
    if (value.precision() > 30 || Math.abs((long) value.scale()) > 18)
      throw new IllegalArgumentException("Число вне поддерживаемой точности раздачи.");
    return value.stripTrailingZeros();
  }

  public static void quantity(final LaboratoryQuantity value) {
    Objects.requireNonNull(value);
    number(value.value());
  }

  /** Frozen planner output keeps its exact decimal value; it is not manual dispensing input. */
  public static BigDecimal calculatedNumber(final BigDecimal value) {
    Objects.requireNonNull(value);
    if (value.precision() > 100 || Math.abs((long) value.scale()) > 100)
      throw new IllegalArgumentException("Расчётное число вне поддерживаемой точности раздачи.");
    return value;
  }

  public static void calculatedQuantity(final LaboratoryQuantity value) {
    Objects.requireNonNull(value);
    calculatedNumber(value.value());
  }

  private static void volume(final LaboratoryQuantity value, final boolean positive) {
    quantity(value);
    if (!value.unit().dimension().equals("volume") || positive && value.value().signum() <= 0)
      throw new IllegalArgumentException(
          "Введите " + (positive ? "положительный " : "") + "объём.");
  }

  private static String identifier(final String value) {
    final String id = IdentifierValue.requireIdentifier(value, "Идентификатор раздачи");
    if (id.length() > 200) throw new IllegalArgumentException("Слишком длинный идентификатор.");
    return id;
  }

  private static String text(final String value, final int max, final boolean required) {
    Objects.requireNonNull(value);
    if (value.length() > max || required && value.isBlank())
      throw new IllegalArgumentException("Заполните поле плана раздачи в допустимой длине.");
    return value.strip();
  }

  private static void unique(final List<String> ids) {
    if (new HashSet<>(ids).size() != ids.size())
      throw new IllegalArgumentException("Повтор идентификатора или позиции в раздаче.");
  }
}
