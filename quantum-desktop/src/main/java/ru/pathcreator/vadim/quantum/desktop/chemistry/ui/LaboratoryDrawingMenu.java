/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Context commands use the same editor transaction as drawing gestures and the inspector. */
final class LaboratoryDrawingMenu {
  private LaboratoryDrawingMenu() {}

  static ContextMenu create(
      final LaboratoryCanvas.Hit atom,
      final LaboratoryCanvas.BondHit bond,
      final BiConsumer<LaboratoryCanvas.Hit, UnaryOperator<MoleculeDrawing>> change,
      final Consumer<LaboratoryCanvas.Hit> removeAtom) {
    final ContextMenu menu = new ContextMenu();
    final MoleculeSketchService service = new MoleculeSketchService();
    if (atom != null) {
      final Menu element = new Menu("Изменить элемент");
      for (final String symbol :
          java.util.List.of("C", "H", "N", "O", "P", "S", "F", "Cl", "Br", "I")) {
        element
            .getItems()
            .add(
                item(
                    symbol,
                    () ->
                        change.accept(
                            atom,
                            drawing -> {
                              final var source =
                                  drawing.molecule().atoms().stream()
                                      .filter(value -> value.id().equals(atom.atomId()))
                                      .findFirst()
                                      .orElseThrow();
                              return service.changeAtom(
                                  drawing,
                                  atom.atomId(),
                                  symbol,
                                  source.formalCharge().value(),
                                  source.hasIsotope() ? source.isotope().massNumber() : null);
                            })));
      }
      menu.getItems().add(element);
      for (final int delta : new int[] {1, -1}) {
        menu.getItems()
            .add(
                item(
                    delta > 0 ? "Заряд атома +1" : "Заряд атома −1",
                    () ->
                        change.accept(
                            atom,
                            drawing -> {
                              final var source =
                                  drawing.molecule().atoms().stream()
                                      .filter(value -> value.id().equals(atom.atomId()))
                                      .findFirst()
                                      .orElseThrow();
                              return service.changeAtom(
                                  drawing,
                                  atom.atomId(),
                                  source.element().symbol().value(),
                                  source.formalCharge().value() + delta,
                                  source.hasIsotope() ? source.isotope().massNumber() : null);
                            })));
      }
      menu.getItems()
          .addAll(new SeparatorMenuItem(), item("Удалить атом", () -> removeAtom.accept(atom)));
    } else if (bond != null) {
      final var anchor = new LaboratoryCanvas.Hit(bond.componentId(), bond.first());
      final BondType[] types = {
        BondType.SINGLE, BondType.DOUBLE, BondType.TRIPLE, BondType.AROMATIC
      };
      final String[] names = {
        "Одинарная связь", "Двойная связь", "Тройная связь", "Ароматическая связь"
      };
      for (int index = 0; index < types.length; index++) {
        final BondType type = types[index];
        menu.getItems()
            .add(
                item(
                    names[index],
                    () ->
                        change.accept(
                            anchor,
                            drawing ->
                                service.connect(drawing, bond.first(), bond.second(), type))));
      }
      final Menu style = new Menu("Начертание связи");
      final BondDrawingStyle[] styles = {
        BondDrawingStyle.PLAIN,
        BondDrawingStyle.WEDGE_UP,
        BondDrawingStyle.WEDGE_DOWN,
        BondDrawingStyle.WAVY
      };
      final String[] labels = {
        "Обычная линия", "Сплошной клин", "Штриховой клин", "Волнистая линия"
      };
      for (int index = 0; index < styles.length; index++) {
        final BondDrawingStyle value = styles[index];
        style
            .getItems()
            .add(
                item(
                    labels[index],
                    () ->
                        change.accept(
                            anchor,
                            drawing ->
                                service.styleBond(
                                    drawing, bond.first(), bond.second(), value, false))));
      }
      style
          .getItems()
          .add(
              item(
                  "Развернуть начертание",
                  () ->
                      change.accept(
                          anchor,
                          drawing -> {
                            final var value =
                                drawing.bonds().stream()
                                    .filter(entry -> entry.connects(bond.first(), bond.second()))
                                    .findFirst()
                                    .orElseThrow();
                            return service.styleBond(
                                drawing, bond.first(), bond.second(), value.style(), true);
                          })));
      final MenuItem explanation = new MenuItem("R/S и E/Z задаются отдельно от начертания");
      explanation.setDisable(true);
      style.getItems().add(explanation);
      menu.getItems()
          .addAll(
              style,
              new SeparatorMenuItem(),
              item(
                  "Удалить связь",
                  () ->
                      change.accept(
                          anchor,
                          drawing -> service.disconnect(drawing, bond.first(), bond.second()))));
    }
    return menu.getItems().isEmpty() ? null : menu;
  }

  private static MenuItem item(final String label, final Runnable action) {
    final MenuItem item = new MenuItem(label);
    item.setOnAction(event -> action.run());
    return item;
  }
}
