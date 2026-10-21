/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;

/** Completion is checked against the actual graph, selection or view, never a button click alone. */
final class LaboratoryTutorialPractice {
  record State(
      LaboratoryDrawing drawing,
      String tool,
      String element,
      BondType bond,
      String selection,
      boolean hydrogens) {
    long components() {
      return drawing.layouts().size();
    }

    long oxygen() {
      return drawing.layouts().values().stream()
          .flatMap(m -> m.molecule().atoms().stream())
          .filter(a -> a.element().atomicNumber() == 8)
          .count();
    }

    long doubles() {
      return drawing.layouts().values().stream()
          .flatMap(m -> m.molecule().bonds().stream())
          .filter(b -> b.type() == BondType.DOUBLE)
          .count();
    }
  }

  static boolean reading(int step) {
    return step == 0 || step == 15;
  }

  static boolean ready(int step, State before, State now) {
    return switch (step) {
      case 0, 15 -> true;
      case 1 -> now.tool().equals("Связь") && now.bond() == BondType.SINGLE;
      case 2 -> now.components() > before.components();
      case 3 -> now.tool().equals("Атом") && now.element().equals("O");
      case 4 -> now.oxygen() > before.oxygen();
      case 5 -> now.tool().equals("Связь") && now.bond() == BondType.DOUBLE;
      case 6, 8 -> now.doubles() > before.doubles();
      case 7 -> now.doubles() < before.doubles();
      case 9 -> now.tool().equals("Кольцо");
      case 10 -> now.components() > before.components();
      case 11 -> now.tool().equals("Выбор");
      case 12 -> !now.drawing().layouts().equals(before.drawing().layouts());
      case 13 -> now.hydrogens() != before.hydrogens();
      case 14 -> !now.tool().isBlank();
      default -> false;
    };
  }

  static LaboratoryTutorialLessons.Guide guide() {
    return new LaboratoryTutorialLessons.Guide(
        new String[] {
          "#chemistry-canvas",
          "#chemistry-tool-bond",
          "#chemistry-canvas",
          "#chemistry-element-O",
          "#chemistry-canvas",
          "#chemistry-tool-double",
          "#chemistry-canvas",
          "#chemistry-undo",
          "#chemistry-redo",
          "#chemistry-tool-ring",
          "#chemistry-canvas",
          "#chemistry-tool-select",
          "#chemistry-canvas",
          "#chemistry-show-carbon-hydrogens",
          "#chemistry-save",
          "#chemistry-canvas"
        },
        new String[] {
          "Практика: нарисуем отдельную молекулу, изменим атом и связь, отменим правку и добавим кольцо. Используйте свободное место рядом с учебной схемой. Задания проверяют настоящий рисунок; после действия шаг сменится сам. Нажмите «Далее», когда готовы.",
          "Выберите одинарную связь слева (или нажмите 1). Инструмент только задаёт режим рисования — сама молекула пока не изменяется.",
          "На свободном месте зажмите левую кнопку и протяните короткую линию. Получится отдельная связь C–C: концы скелетной линии — атомы углерода. Не начинайте на существующей молекуле: сейчас нужен новый компонент.",
          "Нажмите O справа (или клавишу O). Теперь щелчок по атому заменит его кислородом. Выбор O сам по себе ничего не заменяет.",
          "Нажмите на один конец только что нарисованной связи C–C. Он станет O; проверьте метку OH. Лучше менять именно новый компонент, чтобы исходная учебная схема оставалась прежней.",
          "Выберите двойную связь слева (или нажмите 2). Этот инструмент задаёт двойную связь явно, а обычный щелчок инструментом 1 циклически меняет кратность.",
          "Нажмите на середину новой связи C–O. Она станет двойной: появятся две линии. Переход произойдёт только после изменения графа, а не после выбора инструмента.",
          "Нажмите «Отменить» сверху или Ctrl+Z. Двойная связь должна снова стать одинарной. Отменяется последнее изменение, а не весь опыт.",
          "Нажмите «Повторить» или Ctrl+Y. Двойная связь вернётся. Так можно безопасно сравнить правку до и после.",
          "Выберите шестичленное кольцо слева. Нажатие в пустом месте создаёт кольцо; на атоме — присоединяет; на связи — добавляет конденсированное кольцо.",
          "Нажмите на свободном месте, чтобы добавить отдельное кольцо. Бензольное и пятичленное кольца доступны соседними инструментами; сложные системы — через «Каркасы».",
          "Выберите стрелку «Выбор» (V). Теперь щелчок выделяет атом, рамка — несколько атомов; Shift добавляет к выделению. Режим рисования выключен.",
          "Перетащите атом нового кольца на небольшое расстояние. Изменится расположение выбранной части, а не химический состав. Для целого фрагмента используйте «Фрагмент»; для листа целиком — «Руку» или пробел.",
          "Переключите «Все H». Водороды развернутся или свернутся, но формула и молярная масса останутся теми же. Это настройка отображения.",
          "Закрепляем результат: при переходе обучение сохраняет рисунок в учебный опыт. В обычной работе используйте «Сохранить» сверху или Ctrl+S. При ошибке сохранения переход остановится и появится сообщение.",
          "Рисование освоено. Дальше в углублённом каталоге есть отдельные темы по стереохимии, 3D, копированию, свойствам атомов, стадиям, количествам, импорту и экспорту. «Все функции экрана» показывает конкретные доступные команды, включая вложенные меню."
        });
  }
}
