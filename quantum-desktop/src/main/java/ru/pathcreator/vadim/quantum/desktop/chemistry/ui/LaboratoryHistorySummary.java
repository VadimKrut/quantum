/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.LinkedHashMap;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** A bounded human-readable preview; restoration always uses the complete revision. */
final class LaboratoryHistorySummary {
  private LaboratoryHistorySummary() {}

  static String describe(final LaboratoryDrawing revision, final LaboratoryDrawing current) {
    final var previous = revision.experiment();
    final var present = current.experiment();
    if (!previous.id().equals(present.id())) {
      throw new IllegalArgumentException("Версия принадлежит другому опыту.");
    }
    final StringBuilder result =
        new StringBuilder("Выбранная версия: ")
            .append(previous.title())
            .append("\nКомпонентов: ")
            .append(previous.components().size())
            .append("; стадий: ")
            .append(previous.stages().size())
            .append("; записей: ")
            .append(previous.journal().size())
            .append("; вложений: ")
            .append(previous.attachments().size())
            .append("\n\nПлан и записанные количества в выбранной версии:\n");
    int shown = 0;
    for (final var component : previous.components()) {
      if (shown++ == 40) {
        result.append("Остальные компоненты не показаны в кратком просмотре.\n");
        break;
      }
      result
          .append(shortText(component.molecule().displayName(), 160))
          .append(" · ")
          .append(LaboratoryInspector.roleText(component.role()))
          .append("\n  План: ")
          .append(Objects.toString(component.planned(), "не указан"))
          .append("; ")
          .append(component.evidence().quantityLabel())
          .append(": ")
          .append(Objects.toString(component.actual(), "не указан"))
          .append(" · ")
          .append(component.evidence())
          .append('\n');
    }
    if (previous.components().isEmpty()) {
      result.append("Компонентов нет.\n");
    }
    final var before = new LinkedHashMap<String, LaboratoryComponent>();
    final var now = new LinkedHashMap<String, LaboratoryComponent>();
    previous.components().forEach(component -> before.put(component.id(), component));
    present.components().forEach(component -> now.put(component.id(), component));
    final long added = before.keySet().stream().filter(id -> !now.containsKey(id)).count();
    final long removed = now.keySet().stream().filter(id -> !before.containsKey(id)).count();
    final long changed =
        before.entrySet().stream()
            .filter(
                entry ->
                    now.containsKey(entry.getKey())
                        && !entry.getValue().equals(now.get(entry.getKey())))
            .count();
    result
        .append("\nПри восстановлении относительно редактора:\nДобавятся компоненты: ")
        .append(added)
        .append("; исчезнут из схемы: ")
        .append(removed)
        .append("; изменятся: ")
        .append(changed)
        .append('.');
    if (!previous.title().equals(present.title())) {
      result.append("\nИзменится название опыта.");
    }
    if (!previous.conditions().equals(present.conditions())) {
      result.append("\nИзменятся общие условия опыта.");
    }
    if (!previous.stages().equals(present.stages())) {
      result.append("\nИзменятся стадии реакции и их состав.");
    }
    if (!revision.layouts().equals(current.layouts())) {
      result.append("\nИзменятся структуры или их расположение.");
    }
    if (!previous.notes().equals(present.notes())) {
      result.append("\nИзменятся заметки опыта.");
    }
    result
        .append("\n\nЗаметки выбранной версии:\n")
        .append(previous.notes().isBlank() ? "Не указаны." : shortText(previous.notes(), 2000));
    result.append(
        "\n\nПоздние записи журнала и вложения сохранятся. Складские движения не откатываются. Восстановление изменит редактор; файл запишется только после сохранения.");
    return result.toString();
  }

  private static String shortText(final String text, final int limit) {
    return text.length() <= limit ? text : text.substring(0, limit) + "… [сокращено в просмотре]";
  }
}
