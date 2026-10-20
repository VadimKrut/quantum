/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryBatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratorySolutionBook.Standardization;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryWorkbenchBooks;

/** Read-only stock display; a recipe or an old batch label never substitutes for typed provenance. */
record LaboratoryStockConcentrations(Map<String, Standardization> latest, String problem) {
  LaboratoryStockConcentrations {
    latest = Map.copyOf(latest);
  }

  static LaboratoryStockConcentrations unread() {
    return new LaboratoryStockConcentrations(Map.of(), "Записи концентрации ещё не прочитаны.");
  }

  static LaboratoryStockConcentrations read(final Path folder) {
    try {
      return from(LaboratoryWorkbenchBooks.solutions(folder).read().value());
    } catch (final Exception error) {
      return new LaboratoryStockConcentrations(
          Map.of(), "Не удалось прочитать концентрации: " + error.getMessage());
    }
  }

  static LaboratoryStockConcentrations from(final LaboratorySolutionBook book) {
    final var superseded = new HashSet<String>();
    book.standardizations()
        .forEach(
            value -> {
              if (value.correctsId() != null) {
                superseded.add(value.correctsId());
              }
            });
    final var latest = new HashMap<String, Standardization>();
    for (final var value : book.standardizations()) {
      if (!superseded.contains(value.id())) {
        latest.merge(
            value.batchId(),
            value,
            (previous, next) -> next.time().isAfter(previous.time()) ? next : previous);
      }
    }
    return new LaboratoryStockConcentrations(latest, "");
  }

  String summary(final LaboratoryBatch batch) {
    if (!this.problem.isBlank()) {
      return "Записи недоступны";
    }
    final var value = this.latest.get(batch.id());
    if (value != null) {
      return value.measured() + " · " + value.origin();
    }
    return batch.concentration().isBlank()
        ? "Нет записи концентрации"
        : "При создании (происхождение не указано): " + batch.concentration();
  }

  String details(final LaboratoryBatch batch) {
    if (!this.problem.isBlank()) {
      return this.problem + " Обновите учёт перед использованием концентрации.";
    }
    final var value = this.latest.get(batch.id());
    if (value == null) {
      return "Концентрация: нет отдельной записи."
          + (batch.concentration().isBlank()
              ? ""
              : " При создании партии (происхождение не указано): " + batch.concentration());
    }
    return "Концентрация: "
        + value.measured()
        + " · "
        + value.origin()
        + " · "
        + value.method()
        + "\nИсточник: "
        + value.source()
        + "\nЗаписано: "
        + value.time()
        + " · запись "
        + value.id();
  }
}
