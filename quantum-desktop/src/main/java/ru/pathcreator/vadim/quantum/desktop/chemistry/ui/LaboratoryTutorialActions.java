/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.*;
import javafx.scene.*;
import javafx.scene.control.*;

/** Inventories the actual controls, including unopened tabs and nested menus. Does not execute commands. */
final class LaboratoryTutorialActions {
  record Item(String path, String explanation, Node target, Runnable reveal, boolean disabled) {}

  private final List<Item> items = new ArrayList<>();
  private final Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());

  static List<Item> collect(Parent root, Map<String, ContextMenu> context) {
    var scan = new LaboratoryTutorialActions();
    scan.visit(root, "Экран", () -> {});
    Node canvas = root.lookup("#chemistry-canvas");
    if (canvas != null)
      context.forEach((name, menu) -> scan.menu(menu.getItems(), name, canvas, () -> {}));
    return List.copyOf(scan.items);
  }

  private void visit(Node node, String path, Runnable reveal) {
    if (!seen.add(node)) return;
    if (node instanceof TabPane tabs) {
      for (Tab tab : tabs.getTabs()) {
        Runnable open =
            () -> {
              reveal.run();
              tabs.getSelectionModel().select(tab);
            };
        items.add(
            new Item(
                path + " → Вкладка «" + tab.getText() + "»",
                "Переключает рабочую область на «"
                    + tab.getText()
                    + "». Данные других вкладок остаются в опыте.",
                tabs,
                open,
                tab.isDisable()));
        if (tab.getContent() != null) visit(tab.getContent(), path + " → " + tab.getText(), open);
      }
      return;
    }
    if (node instanceof TitledPane pane) {
      Runnable open =
          () -> {
            reveal.run();
            pane.setExpanded(true);
          };
      items.add(
          new Item(
              path + " → " + pane.getText(),
              "Раскрывает группу «"
                  + pane.getText()
                  + "» с дополнительными полями и действиями. Раскрытие само по себе не меняет данные.",
              pane,
              open,
              pane.isDisabled()));
      if (pane.getContent() != null) visit(pane.getContent(), path + " → " + pane.getText(), open);
      return;
    }
    if (node instanceof MenuButton menu) {
      String name = caption(menu);
      menu(menu.getItems(), path + " → " + name, menu, reveal);
    } else if (node instanceof ButtonBase button) {
      String name = caption(button);
      if (!name.isBlank()) {
        String tooltip = button.getTooltip() == null ? "" : button.getTooltip().getText();
        items.add(
            new Item(
                path + " → " + name,
                tooltip.isBlank() || tooltip.equalsIgnoreCase(name) ? explain(name) : tooltip,
                button,
                reveal,
                button.isDisabled()));
      }
    }
    if (node instanceof Control control && control.getContextMenu() != null)
      menu(control.getContextMenu().getItems(), path + " → Правая кнопка", control, reveal);
    if (node instanceof Parent parent)
      for (Node child : parent.getChildrenUnmodifiable()) visit(child, path, reveal);
  }

  private void menu(List<MenuItem> children, String path, Node anchor, Runnable reveal) {
    for (MenuItem item : children) {
      if (item instanceof SeparatorMenuItem) continue;
      if (item instanceof Menu nested)
        menu(nested.getItems(), path + " → " + item.getText(), anchor, reveal);
      else if (item.getOnAction() != null) {
        String caption = item.getText().replace("_", "");
        String hint = explain(caption);
        if (item.getId() != null && item.getId().startsWith("section-")) {
          var lesson = LaboratoryTutorialLessons.forCommand(item.getId().substring(8));
          hint = lesson.texts()[0] + " " + lesson.texts()[1];
        }
        if (item.getAccelerator() != null)
          hint += " Сочетание: " + item.getAccelerator().getDisplayText() + ".";
        items.add(
            new Item(
                path + " → " + caption,
                hint + " Откройте указанное меню и выберите этот пункт.",
                anchor,
                reveal,
                item.isDisable() || anchor.isDisabled()));
      }
    }
  }

  private static String caption(Labeled control) {
    if (control.getText() != null && !control.getText().isBlank())
      return control.getText().replace("_", "");
    if (control.getAccessibleText() != null && !control.getAccessibleText().isBlank())
      return control.getAccessibleText();
    if (control instanceof Control c && c.getTooltip() != null) return c.getTooltip().getText();
    return "";
  }

  static String explain(String caption) {
    String specific = LaboratoryTutorialActionHelp.find(caption);
    if (specific != null) return specific;
    String s = caption.toLowerCase(Locale.ROOT).replace('ё', 'е');
    if (s.matches("c|h|n|o|p|s|f|cl|br|i"))
      return "Выбирает элемент "
          + caption
          + ". В палитре это только выбор инструмента; замена выполняется щелчком по атому. В контекстном меню изменяется атом, на котором открыто меню.";
    if (s.contains("заряд"))
      return "Меняет формальный заряд выбранного атома. Это химическое свойство, влияющее на структуру; после изменения проверьте валентность и формулу.";
    if (s.contains("клин")
        || s.contains("начертание")
        || s.contains("волнист")
        || s.equals("обычная линия"))
      return "Меняет начертание выбранной связи. Направление клина имеет значение; R/S и E/Z задаются и проверяются отдельно от рисунка.";
    if (s.contains("связь"))
      return "Редактирует выбранную связь или включает инструмент её построения. Для рисования протяните от атома; для изменения существующей связи нажмите на неё. Проверяйте кратность и валентность.";
    if (s.contains("приход"))
      return "Регистрирует поступление материала в конкретную партию: укажите количество, единицу и источник. После подтверждения проверьте остаток и журнал движений.";
    if (s.contains("расход"))
      return "Записывает фактический расход выбранной партии или образца. Укажите количество и назначение; проверьте единицу и доступный остаток. Плановое количество не является расходом.";
    if (s.contains("возврат"))
      return "Возвращает количество в учёт выбранной партии. Укажите основание и проверьте историю: возврат должен соответствовать фактическому движению материала.";
    if (s.contains("аликвот"))
      return "Выделяет порцию исходного образца. Задайте количество, единицу и код новой порции; проверьте уменьшение исходного остатка и происхождение дочернего образца.";
    if (s.contains("объедин"))
      return "Объединяет выбранные совместимые объекты в новую запись. Для физических образцов проверьте вещество, единицы и количества; история исходных порций должна сохраниться.";
    if (s.contains("разделить") || s.contains("фракци"))
      return "Регистрирует дочерние порции или фракции образца. Укажите их коды и количества, проверьте баланс относительно исходного остатка и сохранение происхождения.";
    if (s.contains("этикет") || s.contains("метк"))
      return "Готовит маркировку выбранного объекта. Проверьте код, название и область печати/экспорта; этикетка должна соответствовать конкретному образцу или партии.";
    if (s.contains("концентрац"))
      return "Работает с концентрацией раствора. Проверьте тип концентрации, единицы, объём и исходную партию; план приготовления и фактическое приготовление учитываются отдельно.";
    if (s.contains("изменить") || s.contains("редактир") || s.contains("переимен"))
      return "Открывает редактирование выбранной записи. Исправьте нужные поля, затем примените или отмените форму. Для фактических данных проверьте сохранение истории и источника изменения.";
    if (s.contains("перемест") || s.contains("перенести"))
      return "Переносит выбранный объект в указанное место или область. Проверьте источник и назначение. Перемещение хранения не меняет количество; передача образца в другой опыт отдельно учитывает расход.";
    if (s.contains("восстанов"))
      return "Открывает восстановление сохранённого состояния или черновика. Сравните версии и текущие изменения перед подтверждением; после восстановления проверьте и сохраните нужный документ.";
    if (s.contains("архив"))
      return "Работает с сохранёнными или архивными записями. Проверьте область и фильтр; архивирование и полное удаление — разные действия.";
    if (s.contains("проверить") || s.contains("диагност"))
      return "Проверяет текущий ввод или структуру и показывает диагностику. Прочитайте сообщение, исправьте причину и повторите проверку; отсутствие диагностических ошибок не доказывает экспериментальный результат.";
    if (s.contains("сохранить как"))
      return "Сохраняет текущий документ по выбранному пути. Проверьте имя и папку: это отдельная копия, дальнейшие изменения относятся к текущему открытому файлу.";
    if (s.contains("экспорт")
        || s.contains("pdf")
        || s.contains("html")
        || s.contains("cdxml")
        || s.contains("csv"))
      return "Создаёт файл в выбранном формате. Сначала проверьте данные и область экспорта, затем укажите путь. Таблица или рисунок могут содержать меньше сведений, чем полный QCHEM.";
    if (s.contains("сохран") || s.contains("записать") || s.contains("применить"))
      return "Подтверждает введённые значения или подготовленный результат. Сначала заполните обязательные поля и проверьте единицы; после нажатия проверьте сообщение об успехе и появление записи. Ошибка ввода не означает сохранение.";
    if (s.contains("отмен") || s.contains("назад") || s.contains("вернуться") || s.startsWith("←"))
      return "Возвращает к предыдущему виду или отменяет текущий ввод. Если есть незавершённая форма, сначала сохраните её или явно откажитесь от изменений. В редакторе «Отменить» выполняет отмену последней правки.";
    if (s.contains("удал") || s.contains("списать"))
      return "Удаляет выбранный объект или записывает списание в рамках этой формы. Проверьте выбор и показанное подтверждение. Списание физического остатка отличается от удаления структуры с рисунка.";
    if (s.contains("добав") || s.contains("создат") || s.contains("новый") || s.contains("новая"))
      return "Начинает создание новой записи или добавляет заполненную запись. Укажите имя, связи с опытом и обязательные значения; затем подтвердите форму. Новая запись должна появиться в соответствующем списке.";
    if (s.contains("выдел") || s.contains("выбрат"))
      return "Выбирает объект для дальнейшего действия. Проверьте выделение и карточку сведений: изменение выбора само по себе не записывает результат опыта.";
    if (s.contains("рассчит")
        || s.contains("расчет")
        || s.contains("построить")
        || s.contains("прогноз"))
      return "Запускает построение или расчёт по текущему вводу. Проверьте структуру, метод и единицы, дождитесь статуса завершения, затем изучите результат и ограничения. Расчёт не становится измерением.";
    if (s.contains("поиск")
        || s.contains("найти")
        || s.contains("обновить")
        || s.contains("перечитать"))
      return "Обновляет список или выполняет поиск с текущими фильтрами. Проверьте область поиска и сохранение исходных файлов; пустой список может означать отсутствие совпадений.";
    if (s.contains("импорт")
        || s.contains("загрузить")
        || s.contains("открыть")
        || s.contains("выбрать файл"))
      return "Открывает файл, запись или форму выбора. Укажите источник и проверьте результат загрузки. При импорте отдельно проверьте структуру, роли и перенесённые поля.";
    if (s.contains("копир") || s.contains("дублир") || s.contains("повтор"))
      return "Создаёт копию или повтор выбранного объекта. У повторного опыта копируется план; его фактические записи вводятся заново. В редакторе «Повторить» возвращает отменённую правку.";
    if (s.contains("начать")
        || s.contains("завершить")
        || s.contains("пропустить")
        || s.contains("выполн"))
      return "Фиксирует переход состояния текущего шага. Проверьте выбранную методику, время и фактические значения; выполненный шаг должен появиться в истории.";
    if (s.contains("раздел")
        || s.contains("структур")
        || s.contains("реакци")
        || s.contains("пакет"))
      return "Открывает работу с указанным объектом или форматом. Для переноса полного опыта используйте QCHEM или пакет; при переносе структуры проверьте ограничения выбранного формата.";
    if (s.contains("все")
        || s.contains("показ")
        || s.contains("скры")
        || s.contains("вид")
        || s.contains("панел"))
      return "Меняет отображение или состав видимых записей. Проверьте активный фильтр: скрытая запись не обязательно удалена. Настройка отображения не является химическим результатом.";
    if (s.contains("связать") || s.contains("назначить") || s.contains("сопостав"))
      return "Связывает выбранные записи. Перед подтверждением проверьте обе стороны, вещество, стадию или образец; связь должна отражать реальное происхождение данных.";
    return "Команда «"
        + caption
        + "» относится к текущему разделу. Выберите нужную запись и откройте команду: поля, единицы и результат описаны в форме. Ниже доступны пояснения раздела; для вложенных команд повторно откройте список функций после перехода.";
  }
}
