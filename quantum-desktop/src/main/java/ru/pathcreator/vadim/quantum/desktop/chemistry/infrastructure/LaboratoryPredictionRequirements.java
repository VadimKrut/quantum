/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;

/** Compact presentation of known input diagnostics; the original explanations are retained separately. */
final class LaboratoryPredictionRequirements {
  private LaboratoryPredictionRequirements() {}

  private static final Pattern EXTERNAL =
      Pattern.compile("No explicit reactant source of ([A-Z][a-z]?) is supplied");
  private static final Pattern EXCESS =
      Pattern.compile("requires additional explicit reactant atoms of (.*?)\\. Conditions");
  private static final Pattern DONOR =
      Pattern.compile("Requires an explicit connected reactant graph of (.*?);");

  static String describe(final ReactionPredictionResult result) {
    final var atoms = new LinkedHashSet<String>();
    final var donors = new LinkedHashSet<String>();
    for (final var evidence : result.diagnostics()) {
      final String text = evidence.description();
      var match = EXTERNAL.matcher(text);
      while (match.find()) {
        atoms.add(match.group(1));
      }
      match = EXCESS.matcher(text);
      while (match.find()) {
        atoms.add(match.group(1).replace("required", "нужно").replace("supplied", "задано"));
      }
      match = DONOR.matcher(text);
      while (match.find()) {
        donors.add(match.group(1));
      }
      if (text.contains("requires a separate connected neutral R-C(=O)-O-O-H")) {
        donors.add("R-C(=O)-O-O-H с явным H");
      }
      if (text.contains("hydrogenation requires a separate connected H-H reactant")) {
        donors.add("H2 и условия катализа");
      }
    }
    final StringBuilder review =
        new StringBuilder("Структурных вариантов: ")
            .append(result.candidates().size())
            .append(". Каналов без построенного продукта: ")
            .append(
                result.mechanismChannels().stream()
                    .filter(channel -> !channel.explicitProductGraphAvailable())
                    .count())
            .append(".\n");
    if (!atoms.isEmpty()) {
      review
          .append("Для части правил не хватает явных источников атомов: ")
          .append(String.join("; ", atoms))
          .append(".\n");
    }
    if (!donors.isEmpty()) {
      review
          .append("Некоторые правила требуют отдельных связных структур реагентов: ")
          .append(String.join("; ", donors))
          .append(".\n");
    }
    if (!atoms.isEmpty() || !donors.isEmpty()) {
      review.append(
          "Это альтернативные правила, а не общий список загрузки. Проверяйте только условия своей предполагаемой реакции.\n");
    }
    review.append(
        "Атомы для прогноза берутся из участников стадии с ролью «Исходник» и коэффициентом. Названия в условиях и вспомогательные роли не передают атомы.\n");
    if (!result.hasCandidates()) {
      review.append(
          "Готовых структурных вариантов нет. Это не доказывает отсутствие реакции и не гарантирует появление варианта после дополнения входа.\n");
    }
    review.append(
        "Диагностический канал не является продуктом или подтверждением осуществимости. Полные объяснения сохранены в диагностике.");
    return review.toString();
  }
}
