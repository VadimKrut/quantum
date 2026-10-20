/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryBatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryInventory;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryMaterial;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryStockOperation;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryQuantityUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

final class LaboratoryInventoryForms {
  private LaboratoryInventoryForms() {}

  static LaboratoryStockForm receipt(
      final LaboratoryInventory inventory,
      final String experimentId,
      final Molecule molecule,
      final Consumer<LaboratoryInventory> save,
      final Runnable close) {
    return receipt(inventory, experimentId, molecule, save, close, null);
  }

  static LaboratoryStockForm receipt(
      final LaboratoryInventory inventory,
      final String experimentId,
      final Molecule molecule,
      final Consumer<LaboratoryInventory> save,
      final Runnable close,
      final Properties restored) {
    final String materialId = LaboratoryStockDraft.id(restored, "material");
    final String batchId = LaboratoryStockDraft.id(restored, "batch");
    final String operationId = LaboratoryStockDraft.id(restored, "operation");
    final Instant[] recordedAt = {null};
    final ComboBox<LaboratoryMaterial> existing = new ComboBox<>();
    existing.getItems().setAll(inventory.materials());
    existing.setPromptText("Новый материал");
    existing.setConverter(
        new StringConverter<>() {
          public String toString(final LaboratoryMaterial value) {
            return value == null ? "Новый материал" : value.name();
          }

          public LaboratoryMaterial fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    final TextField name = field("Название материала");
    final ComboBox<LaboratoryMaterial.Kind> kind = new ComboBox<>();
    kind.getItems().setAll(LaboratoryMaterial.Kind.values());
    kind.setValue(LaboratoryMaterial.Kind.SUBSTANCE);
    final CheckBox structure = new CheckBox("Привязать структуру выбранного компонента");
    structure
        .disableProperty()
        .bind(
            existing
                .valueProperty()
                .isNotNull()
                .or(new javafx.beans.property.SimpleBooleanProperty(molecule == null)));
    final CheckBox product = new CheckBox("Партия получена в текущем опыте");
    final TextField label = field("Номер или обозначение партии");
    final TextField amount = field("Полученное количество");
    final ComboBox<LaboratoryQuantityUnit> unit = units(LaboratoryQuantityUnit.GRAM);
    final TextField purity = field("Чистота, % · необязательно");
    final TextField concentration = field("Концентрация с единицей · необязательно");
    final TextField source = field("Источник / поставщик");
    final TextField location = field("Место хранения");
    final TextField reason = field("Основание прихода");
    name.setId("chemistry-stock-material-name");
    label.setId("chemistry-stock-batch-label");
    amount.setId("chemistry-stock-receipt-amount");
    unit.setId("chemistry-stock-receipt-unit");
    name.disableProperty().bind(existing.valueProperty().isNotNull());
    kind.disableProperty().bind(existing.valueProperty().isNotNull());
    final Button clear = new Button("Создать новый материал");
    clear.setOnAction(event -> existing.setValue(null));
    final GridPane details = new GridPane();
    details.setHgap(12);
    details.setVgap(8);
    for (int i = 0; i < 2; i++) {
      final ColumnConstraints column = new ColumnConstraints();
      column.setPercentWidth(50);
      column.setHgrow(Priority.ALWAYS);
      details.getColumnConstraints().add(column);
    }
    details.add(new VBox(4, new Label("Чистота, % · необязательно"), purity), 0, 0);
    details.add(new VBox(4, new Label("Концентрация с единицей"), concentration), 1, 0);
    details.add(new VBox(4, new Label("Источник / поставщик"), source), 0, 1);
    details.add(new VBox(4, new Label("Место хранения"), location), 1, 1);
    final Label preview = new Label();
    preview.setId("chemistry-stock-receipt-preview");
    preview.setWrapText(true);
    LaboratoryQuantityText.bind(
        preview,
        compact -> {
          try {
            final var quantity = LaboratoryQuantityFields.quantity(amount, unit);
            if (quantity == null) {
              return "Для жидкости выберите мл, мкл или л. Единица партии сохранится в учёте.";
            }
            if (quantity.value().signum() <= 0) {
              return "Приходное количество должно быть больше нуля.";
            }
            final var displayUnit =
                ru.pathcreator.vadim.quantum.chemistry.domain.inventory.LaboratoryMaterialReport
                    .unit(quantity.unit());
            return "Начальный остаток: "
                + LaboratoryQuantityText.format(quantity, compact)
                + (displayUnit == quantity.unit()
                    ? ""
                    : " = " + LaboratoryQuantityText.format(quantity.in(displayUnit), compact));
          } catch (final RuntimeException exception) {
            return "Проверьте количество: " + exception.getMessage();
          }
        },
        amount.textProperty(),
        unit.valueProperty());
    existing.setMaxWidth(Double.MAX_VALUE);
    kind.setMaxWidth(Double.MAX_VALUE);
    final VBox identity =
        new VBox(7, new Label("Название и вид нового материала"), name, kind, structure);
    identity.visibleProperty().bind(existing.valueProperty().isNull());
    identity.managedProperty().bind(identity.visibleProperty());
    final javafx.scene.control.TitledPane choose =
        new javafx.scene.control.TitledPane(
            "Выбрать существующий материал", new VBox(7, existing, clear));
    choose.setExpanded(false);
    choose.setAnimated(false);
    choose.setId("chemistry-stock-existing");
    choose.setVisible(!inventory.materials().isEmpty());
    choose.setManaged(choose.isVisible());
    existing
        .valueProperty()
        .addListener(
            (o, a, b) ->
                choose.setText(
                    b == null ? "Выбрать существующий материал" : "Материал: " + b.name()));
    final javafx.scene.control.TitledPane properties =
        new javafx.scene.control.TitledPane("Чистота, концентрация, источник и хранение", details);
    properties.setExpanded(false);
    properties.setAnimated(false);
    properties.setId("chemistry-stock-properties");
    final VBox content =
        new VBox(
            7,
            choose,
            identity,
            new Label("Обозначение партии"),
            label,
            new Label("Приходное количество и единица учёта"),
            LaboratoryQuantityFields.row(amount, unit),
            preview,
            product,
            new Label("Основание прихода"),
            reason,
            properties);
    final Map<String, javafx.scene.control.Control> fields =
        Map.ofEntries(
            Map.entry("name", name),
            Map.entry("kind", kind),
            Map.entry("structure", structure),
            Map.entry("product", product),
            Map.entry("label", label),
            Map.entry("amount", amount),
            Map.entry("unit", unit),
            Map.entry("purity", purity),
            Map.entry("concentration", concentration),
            Map.entry("source", source),
            Map.entry("location", location),
            Map.entry("reason", reason));
    if (restored != null) {
      LaboratoryStockDraft.fields(restored, true, fields);
      LaboratoryStockDraft.choice(restored, "existing", true, existing, LaboratoryMaterial::id);
      if (structure.isSelected() && molecule == null && existing.getValue() == null) {
        throw new IllegalArgumentException(
            "Структура для привязки партии не найдена в восстановленном опыте.");
      }
    }
    final Label conversion = LaboratoryQuantityConversion.bind(amount, unit, () -> false);
    content.getChildren().add(content.getChildren().indexOf(preview), conversion);
    return new LaboratoryStockForm(
            "Приход / получение продукта",
            content,
            () -> {
              if (existing.getValue() == null) {
                required(name, "Укажите название материала.");
              }
              required(label, "Укажите обозначение партии.");
              required(reason, "Укажите основание прихода.");
              final var material =
                  existing.getValue() == null
                      ? new LaboratoryMaterial(
                          materialId,
                          name.getText(),
                          kind.getValue(),
                          structure.isSelected() ? molecule : null)
                      : existing.getValue();
              final var batch =
                  new LaboratoryBatch(
                      batchId,
                      material.id(),
                      label.getText(),
                      unit.getValue(),
                      number(purity),
                      concentration.getText(),
                      source.getText(),
                      location.getText(),
                      null,
                      null,
                      product.isSelected() ? experimentId : null);
              final var quantity = LaboratoryQuantityFields.quantity(amount, unit);
              if (quantity == null) {
                throw new IllegalArgumentException("Укажите приходное количество.");
              }
              final Instant timestamp = recordedAt[0] == null ? Instant.now() : recordedAt[0];
              final var receipt =
                  new LaboratoryStockOperation(
                      operationId,
                      batch.id(),
                      timestamp,
                      LaboratoryStockOperation.Kind.RECEIPT,
                      quantity,
                      product.isSelected() ? experimentId : null,
                      null,
                      reason.getText());
              final var next = inventory.receive(material, batch, receipt);
              recordedAt[0] = timestamp;
              save.accept(next);
            },
            close)
        .recovery(
            data -> {
              data.setProperty("stockForm.kind", LaboratoryStockOperation.Kind.RECEIPT.name());
              data.setProperty("stockForm.material", materialId);
              data.setProperty("stockForm.batch", batchId);
              data.setProperty("stockForm.operation", operationId);
              LaboratoryStockDraft.fields(data, false, fields);
              LaboratoryStockDraft.choice(
                  data, "existing", false, existing, LaboratoryMaterial::id);
            },
            restored != null);
  }

  static LaboratoryStockForm operation(
      final LaboratoryInventory inventory,
      final LaboratoryBatch batch,
      final String experimentId,
      final LaboratoryStockOperation.Kind kind,
      final Consumer<LaboratoryInventory> save,
      final Runnable close) {
    return operation(inventory, batch, experimentId, kind, save, close, null);
  }

  static LaboratoryStockForm operation(
      final LaboratoryInventory inventory,
      final LaboratoryBatch batch,
      final String experimentId,
      final LaboratoryStockOperation.Kind kind,
      final Consumer<LaboratoryInventory> save,
      final Runnable close,
      final Properties restored) {
    return operation(
        inventory, batch, experimentId, kind, save, close, restored, java.util.List.of());
  }

  static LaboratoryStockForm operation(
      final LaboratoryInventory inventory,
      final LaboratoryBatch batch,
      final String experimentId,
      final LaboratoryStockOperation.Kind kind,
      final Consumer<LaboratoryInventory> save,
      final Runnable close,
      final Properties restored,
      final java.util.List<
              ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent>
          components) {
    final String operationId = LaboratoryStockDraft.id(restored, "operation");
    final Instant[] recordedAt = {null};
    final TextField amount = new LaboratoryDecimalField();
    amount.setPromptText("Количество");
    final ComboBox<LaboratoryQuantityUnit> unit = units(batch.unit());
    unit.getItems().removeIf(value -> !value.dimension().equals(batch.unit().dimension()));
    final TextField reason = field("Основание операции / стадия опыта");
    amount.setId("chemistry-stock-operation-amount");
    unit.setId("chemistry-stock-operation-unit");
    final java.util.Map<String, BigDecimal> returnedAmounts = new java.util.HashMap<>();
    for (final var operation : inventory.operations()) {
      if (operation.batchId().equals(batch.id())
          && operation.kind() == LaboratoryStockOperation.Kind.RETURN) {
        returnedAmounts.merge(
            operation.returnsOperationId(),
            operation.quantity().in(batch.unit()).value(),
            BigDecimal::add);
      }
    }
    final java.util.function.Function<LaboratoryStockOperation, BigDecimal> available =
        operation ->
            operation
                .quantity()
                .in(batch.unit())
                .value()
                .subtract(returnedAmounts.getOrDefault(operation.id(), BigDecimal.ZERO));
    final ComboBox<LaboratoryStockOperation> returned = new ComboBox<>();
    returned.setMinWidth(0);
    returned.setMaxWidth(Double.MAX_VALUE);
    returned
        .getItems()
        .setAll(
            inventory.operations().stream()
                .filter(
                    value ->
                        value.batchId().equals(batch.id())
                            && value.kind() == LaboratoryStockOperation.Kind.USE
                            && (available.apply(value).signum() > 0
                                || restored != null
                                    && value
                                        .id()
                                        .equals(restored.getProperty("stockForm.returned"))))
                .toList());
    returned.setConverter(
        new StringConverter<>() {
          public String toString(final LaboratoryStockOperation value) {
            if (value == null) {
              return "Выберите исходный расход";
            }
            final String context =
                java.util.Objects.equals(experimentId, value.experimentId())
                    ? "текущий опыт"
                    : value.experimentId() == null ? "без привязки к опыту" : "другой опыт";
            return java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(value.recordedAt())
                + " · "
                + context
                + " · можно вернуть "
                + available.apply(value).stripTrailingZeros().toPlainString()
                + " "
                + batch.unit()
                + " · "
                + value.reason();
          }

          public LaboratoryStockOperation fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    returned.setCellFactory(
        list ->
            new ListCell<>() {
              {
                this.prefWidthProperty().bind(returned.widthProperty().subtract(24));
                this.setMaxWidth(USE_PREF_SIZE);
                this.setWrapText(true);
              }

              protected void updateItem(final LaboratoryStockOperation value, final boolean empty) {
                super.updateItem(value, empty);
                this.setText(
                    empty || value == null ? null : returned.getConverter().toString(value));
                this.setTooltip(
                    empty || value == null
                        ? null
                        : new Tooltip(
                            value.id()
                                + "\nОпыт: "
                                + java.util.Objects.toString(
                                    value.experimentId(), "без привязки")));
              }
            });
    final Label originalDetails = new Label();
    originalDetails.setWrapText(true);
    originalDetails
        .textProperty()
        .bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                  final var value = returned.getValue();
                  return value == null
                      ? "Возврат будет отнесён к опыту выбранного расхода."
                      : returned.getConverter().toString(value)
                          + (java.util.Objects.equals(experimentId, value.experimentId())
                              ? ""
                              : "\nОпыт: "
                                  + java.util.Objects.toString(
                                      value.experimentId(), "без привязки"));
                },
                returned.valueProperty()));
    final VBox content =
        new VBox(
            8,
            LaboratoryQuantityText.label(batch.label() + " · Остаток ", inventory.balance(batch)),
            new Label("Количество и единица"),
            LaboratoryQuantityFields.row(amount, unit),
            new Label("Основание операции / стадия"),
            reason);
    final LaboratoryStockQuantityPicker quantities =
        kind == LaboratoryStockOperation.Kind.USE && !components.isEmpty()
            ? new LaboratoryStockQuantityPicker(
                inventory.materials().stream()
                    .filter(material -> material.id().equals(batch.materialId()))
                    .findFirst()
                    .orElseThrow(),
                components,
                amount,
                unit,
                reason)
            : null;
    if (quantities != null) {
      content.getChildren().add(1, quantities);
    }
    if (kind == LaboratoryStockOperation.Kind.RETURN) {
      content.getChildren().addAll(new Label("Исходный расход"), returned, originalDetails);
    }
    final Label preview = new Label();
    preview.setWrapText(true);
    preview.setId("chemistry-stock-quantity-preview");
    LaboratoryQuantityText.bind(
        preview,
        compact -> {
          try {
            if (kind == LaboratoryStockOperation.Kind.RETURN && returned.getValue() == null) {
              return returned.getItems().isEmpty()
                  ? "Нет расходов с доступным количеством для возврата."
                  : "Выберите исходный расход.";
            }
            final var quantity = LaboratoryQuantityFields.quantity(amount, unit);
            if (quantity == null) {
              return "Введите количество для расчёта остатка.";
            }
            final var value = quantity.in(batch.unit()).value();
            if (value.signum() <= 0) {
              return "Количество должно быть больше нуля.";
            }
            if (kind == LaboratoryStockOperation.Kind.RETURN
                && value.compareTo(available.apply(returned.getValue())) > 0) {
              return "Возврат превышает невозвращённое количество исходного расхода.";
            }
            final var balance =
                inventory
                    .balance(batch)
                    .value()
                    .add(value.multiply(BigDecimal.valueOf(kind.sign())));
            final var free = balance.subtract(inventory.reserved(batch).value());
            if (free.signum() < 0) {
              return "Недостаточно свободного материала: часть партии в резерве. Свободно сейчас "
                  + LaboratoryQuantityText.format(inventory.available(batch), compact);
            }
            return balance.signum() < 0
                ? "Недостаточно материала: не хватает "
                    + LaboratoryQuantityText.format(balance.abs(), batch.unit(), compact)
                : "Физически после операции: "
                    + LaboratoryQuantityText.format(balance, batch.unit(), compact)
                    + "; свободно: "
                    + LaboratoryQuantityText.format(free, batch.unit(), compact);
          } catch (final RuntimeException exception) {
            return "Проверьте количество: " + exception.getMessage();
          }
        },
        amount.textProperty(),
        ((LaboratoryDecimalField) amount).valueRevisionProperty(),
        unit.valueProperty(),
        returned.valueProperty());
    content.getChildren().add(preview);
    final Map<String, javafx.scene.control.Control> fields =
        Map.of("amount", amount, "unit", unit, "reason", reason);
    if (restored != null) {
      LaboratoryStockDraft.fields(restored, true, fields);
      LaboratoryStockDraft.choice(
          restored, "returned", true, returned, LaboratoryStockOperation::id);
      if (quantities != null) {
        quantities.draft(restored, true);
      }
    }
    final Label conversion = LaboratoryQuantityConversion.bind(amount, unit, () -> false);
    content.getChildren().add(content.getChildren().indexOf(preview), conversion);
    return new LaboratoryStockForm(
            kind.toString(),
            content,
            () -> {
              required(reason, "Укажите основание операции.");
              final var quantity = LaboratoryQuantityFields.quantity(amount, unit);
              if (quantity == null) {
                throw new IllegalArgumentException("Укажите количество операции.");
              }
              final var original = returned.getValue();
              if (kind == LaboratoryStockOperation.Kind.RETURN && original == null) {
                throw new IllegalArgumentException("Выберите исходный расход.");
              }
              final Instant timestamp = recordedAt[0] == null ? Instant.now() : recordedAt[0];
              final var operation =
                  new LaboratoryStockOperation(
                      operationId,
                      batch.id(),
                      timestamp,
                      kind,
                      quantity,
                      kind == LaboratoryStockOperation.Kind.USE
                          ? experimentId
                          : original == null ? null : original.experimentId(),
                      original == null ? null : original.id(),
                      reason.getText());
              final var next = inventory.post(operation);
              recordedAt[0] = timestamp;
              save.accept(next);
            },
            close)
        .recovery(
            data -> {
              data.setProperty("stockForm.kind", kind.name());
              data.setProperty("stockForm.batch", batch.id());
              data.setProperty("stockForm.operation", operationId);
              LaboratoryStockDraft.fields(data, false, fields);
              LaboratoryStockDraft.choice(
                  data, "returned", false, returned, LaboratoryStockOperation::id);
              if (quantities != null) {
                quantities.draft(data, false);
              }
            },
            restored != null);
  }

  private static TextField field(final String prompt) {
    final TextField field = new TextField();
    field.setPromptText(prompt);
    return field;
  }

  private static void required(final TextField field, final String message) {
    if (field.getText().isBlank()) {
      field.requestFocus();
      throw new IllegalArgumentException(message);
    }
  }

  private static BigDecimal number(final TextField field) {
    if (field.getText().isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(field.getText().trim().replace(',', '.'));
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Ожидается число: " + field.getText());
    }
  }

  private static ComboBox<LaboratoryQuantityUnit> units(final LaboratoryQuantityUnit initial) {
    final ComboBox<LaboratoryQuantityUnit> result = new ComboBox<>();
    result.getItems().setAll(LaboratoryQuantityUnit.values());
    result.setValue(initial);
    return result;
  }
}
