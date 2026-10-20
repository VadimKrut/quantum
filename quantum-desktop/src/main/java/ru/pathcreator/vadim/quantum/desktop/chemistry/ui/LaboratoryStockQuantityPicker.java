/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.List;
import java.util.Properties;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryMaterial;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantity;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;

/** Copies a reviewed quantity into an unposted operation; never changes an experiment or ledger. */
final class LaboratoryStockQuantityPicker extends VBox {
  private final ComboBox<LaboratoryComponent> source = new ComboBox<>();
  private final Button plan = new Button("Из плана"), actual = new Button("Из факта");
  private final Label details = new Label();
  private final LaboratoryMaterial material;
  private final TextField amount, reason;
  private final ComboBox<LaboratoryQuantityUnit> unit;
  private MolecularGraphEquivalence.Result relationship =
      MolecularGraphEquivalence.Result.UNRESOLVED;
  private String suggestedReason;

  LaboratoryStockQuantityPicker(
      final LaboratoryMaterial material,
      final List<LaboratoryComponent> components,
      final TextField amount,
      final ComboBox<LaboratoryQuantityUnit> unit,
      final TextField reason) {
    super(6);
    this.material = material;
    this.amount = amount;
    this.unit = unit;
    this.reason = reason;
    this.source.setId("chemistry-stock-source-component");
    this.source.getItems().setAll(components);
    this.source.setMaxWidth(Double.MAX_VALUE);
    this.source.setPromptText("Выберите компонент текущего опыта");
    this.source.setConverter(
        new StringConverter<>() {
          public String toString(final LaboratoryComponent component) {
            return component == null
                ? ""
                : component.molecule().displayName()
                    + " · "
                    + LaboratoryInspector.roleText(component.role())
                    + " · план: "
                    + quantity(component.planned());
          }

          public LaboratoryComponent fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.source.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.setWrapText(true);
                this.prefWidthProperty().bind(source.widthProperty().subtract(24));
                this.setMaxWidth(USE_PREF_SIZE);
              }

              protected void updateItem(final LaboratoryComponent component, final boolean empty) {
                super.updateItem(component, empty);
                this.setText(
                    empty || component == null ? null : source.getConverter().toString(component));
              }
            });
    this.plan.setId("chemistry-stock-copy-plan");
    this.actual.setId("chemistry-stock-copy-actual");
    this.details.setId("chemistry-stock-source-details");
    this.details.setWrapText(true);
    this.plan.setOnAction(event -> this.copy(false));
    this.actual.setOnAction(event -> this.copy(true));
    this.source
        .valueProperty()
        .addListener(
            (o, previous, component) -> {
              this.relationship =
                  component == null || material.molecule() == null
                      ? MolecularGraphEquivalence.Result.UNRESOLVED
                      : MolecularGraphEquivalence.compare(
                          material.molecule(), component.molecule());
              this.refresh();
            });
    this.unit.valueProperty().addListener((o, previous, selected) -> this.refresh());
    this.getChildren()
        .setAll(
            new Label("Подставить количество из опыта"),
            this.source,
            new FlowPane(8, 6, this.plan, this.actual),
            this.details);
    if (components.size() == 1) {
      this.source.setValue(components.getFirst());
    }
    this.refresh();
  }

  void draft(final Properties fields, final boolean restore) {
    LaboratoryStockDraft.choice(
        fields, "quantitySource", restore, this.source, LaboratoryComponent::id);
    if (restore) {
      this.suggestedReason = fields.getProperty("stockForm.quantityReason");
    } else if (this.suggestedReason != null) {
      fields.setProperty("stockForm.quantityReason", this.suggestedReason);
    }
  }

  private boolean available(final LaboratoryQuantity quantity) {
    return this.relationship == MolecularGraphEquivalence.Result.EQUIVALENT
        && quantity != null
        && quantity.value().signum() > 0
        && this.unit.getValue() != null
        && quantity.unit().dimension().equals(this.unit.getValue().dimension());
  }

  private void refresh() {
    final var component = this.source.getValue();
    this.plan.setDisable(component == null || !this.available(component.planned()));
    this.actual.setDisable(
        component == null
            || component.evidence()
                != ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEvidence
                    .OBSERVED
            || !this.available(component.actual()));
    if (this.material.molecule() == null) {
      this.details.setText(
          "У материала партии нет структуры для сопоставления. Укажите расход вручную.");
    } else if (component == null) {
      this.details.setText(
          "Выберите компонент: программа проверит структуру и единицу количества.");
    } else if (this.relationship != MolecularGraphEquivalence.Result.EQUIVALENT) {
      this.details.setText(
          this.relationship == MolecularGraphEquivalence.Result.DISTINCT
              ? "Структура компонента отличается от материала партии. Подстановка недоступна."
              : "Соответствие структур не подтверждено. Проверьте материал и введите расход вручную.");
    } else {
      this.details.setText(
          "План: "
              + quantity(component.planned())
              + " · "
              + component.evidence()
              + ": "
              + quantity(component.actual())
              + ". Переносятся только положительные количества той же размерности."
              + "\nПроверьте чистоту и концентрацию партии: состав материала при подстановке не пересчитывается.");
    }
  }

  private void copy(final boolean fromActual) {
    final var component = this.source.getValue();
    if (component == null) {
      return;
    }
    if (fromActual
        && component.evidence()
            != ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEvidence.OBSERVED) {
      this.details.setText(
          "Для подстановки факта нужна наблюдённая загрузка. Литературные и модельные количества не подтверждают физический расход.");
      return;
    }
    final var quantity = fromActual ? component.actual() : component.planned();
    if (!this.available(quantity)) {
      return;
    }
    LaboratoryDecimalField.display(this.amount, quantity.in(this.unit.getValue()).value());
    if (this.reason.getText().isBlank() || this.reason.getText().equals(this.suggestedReason)) {
      this.suggestedReason =
          (fromActual ? "По фактической загрузке: " : "По плановой загрузке: ")
              + component.molecule().displayName();
      this.reason.setText(this.suggestedReason);
    }
  }

  private static String quantity(final LaboratoryQuantity quantity) {
    return quantity == null ? "не задан" : LaboratoryQuantityText.format(quantity, true);
  }
}
