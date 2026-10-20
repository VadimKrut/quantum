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
import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;

/** A component-bound draft. Only explicit Apply mutates the editor; preview uses the core arithmetic. */
final class LaboratoryQuantificationForm extends BorderPane {
  private final LaboratoryEditor editor;
  private final LaboratoryComponent original;
  private final String experimentId;
  private final Runnable close;
  private final LaboratoryQuantityFields quantities;
  private final TextField mass = field("chemistry-molar-mass"),
      density = field("chemistry-density"),
      purity = field("chemistry-purity"),
      concentration = field("chemistry-molarity");
  private final TextArea source = new TextArea();
  private final Label preview = new Label(), error = new Label();
  private final TextArea calculationDetails = new TextArea();
  private final TitledPane previewDetails = new TitledPane();
  private final FlowPane confirmation = new FlowPane(8, 8);
  private List<String> initial;
  private boolean loading = true;
  private boolean applied;
  private boolean recoveredConflict;

  LaboratoryQuantificationForm(
      final LaboratoryEditor editor, final String componentId, final Runnable close) {
    this.editor = editor;
    this.close = close;
    this.experimentId = editor.document().experiment().id();
    this.original =
        editor.document().experiment().components().stream()
            .filter(c -> c.id().equals(componentId))
            .findFirst()
            .orElseThrow();
    final var properties = this.original.properties();
    this.quantities = new LaboratoryQuantityFields(this::changed);
    this.quantities.load(this.original);
    LaboratoryDecimalField.display(this.mass, properties.molarMass());
    LaboratoryDecimalField.display(this.density, properties.density());
    LaboratoryDecimalField.display(this.purity, properties.purityPercent());
    LaboratoryDecimalField.display(this.concentration, properties.molarity());
    this.source.setText(properties.source());
    this.source.setId("chemistry-property-source");
    this.source.setPrefRowCount(2);
    this.source.setWrapText(true);
    this.source.setPromptText("Сертификат, измерение или основание принятого значения");
    for (final var field : List.of(this.mass, this.density, this.purity, this.concentration)) {
      ((LaboratoryDecimalField) field)
          .valueRevisionProperty()
          .addListener((o, a, b) -> this.changed());
    }
    this.source.textProperty().addListener((o, a, b) -> this.changed());
    final Label heading = label("Количества · " + this.original.molecule().displayName());
    heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    final Label hint =
        label(
            "План и факт независимы. Пустое поле — неизвестное значение. Изменения применяются одной командой; складской расход здесь не проводится.");
    final Label stale =
        label(
            !properties.empty() && !properties.matches(this.original.molecule())
                ? "Структура изменилась. Старые свойства требуют проверки; кнопка применения подтвердит введённые значения для текущей структуры."
                : "Плотность указана в г/мл, концентрация — в моль/л. Массовая доля не подставляется автоматически.");
    final GridPane fields = new GridPane();
    fields.setHgap(14);
    fields.setVgap(10);
    for (int i = 0; i < 2; i++) {
      final ColumnConstraints column = new ColumnConstraints();
      column.setPercentWidth(50);
      column.setHgrow(Priority.ALWAYS);
      fields.getColumnConstraints().add(column);
    }
    fields.add(new VBox(4, label("Молярная масса, г/моль"), this.mass), 0, 0);
    fields.add(new VBox(4, label("Плотность материала, г/мл"), this.density), 1, 0);
    fields.add(new VBox(4, label("Массовая доля вещества, %"), this.purity), 0, 1);
    fields.add(new VBox(4, label("Концентрация вещества, моль/л"), this.concentration), 1, 1);
    final Button graph = new Button("Молярная масса из явных атомов…");
    graph.setOnAction(
        event -> {
          LaboratoryDecimalField.display(
              this.mass,
              BigDecimal.valueOf(
                  this.original.molecule().composition().mass().averageAtomicMass()));
          this.source.setText(
              "Явные атомы структуры "
                  + this.original.molecule().formula().hillNotation()
                  + "; неявные H не добавлялись. "
                  + this.source.getText());
        });
    final Button clear = new Button("Очистить свойства пересчёта");
    clear.setOnAction(
        event -> {
          List.of(this.mass, this.density, this.purity, this.concentration)
              .forEach(TextField::clear);
          this.source.clear();
        });
    this.preview.setId("chemistry-quantity-preview");
    this.preview.setWrapText(true);
    this.preview.setMinHeight(Region.USE_PREF_SIZE);
    this.calculationDetails.setId("chemistry-quantity-calculation-details");
    this.calculationDetails.setEditable(false);
    this.calculationDetails.setWrapText(true);
    this.calculationDetails.setPrefRowCount(7);
    this.previewDetails.setId("chemistry-quantity-details");
    this.previewDetails.setText("Как рассчитано · точные значения и источник");
    this.previewDetails.setContent(this.calculationDetails);
    this.previewDetails.setExpanded(false);
    this.previewDetails.setAnimated(false);
    final VBox body =
        new VBox(
            10,
            heading,
            hint,
            this.quantities,
            new Separator(),
            stale,
            fields,
            graph,
            label(
                "Для неполного рисунка сумма масс атомов может не соответствовать веществу. Проверьте H; для смеси или полимера укажите обоснованное значение вручную."),
            label("Источник свойств и условия измерения"),
            this.source,
            clear,
            new Separator(),
            label("Предпросмотр после применения и подтверждения свойств"),
            this.preview,
            this.previewDetails);
    body.setPadding(new javafx.geometry.Insets(18));
    body.setMaxWidth(740);
    body.setMinHeight(Region.USE_PREF_SIZE);
    final StackPane sheet = new StackPane(body);
    StackPane.setAlignment(body, javafx.geometry.Pos.TOP_CENTER);
    final ScrollPane scroll = new ScrollPane(sheet);
    scroll.setFitToWidth(true);
    this.setCenter(scroll);
    this.setId("chemistry-quantification-form");
    this.error.setId("chemistry-quantification-error");
    this.error.setWrapText(true);
    this.error.setStyle("-fx-text-fill: #a52a2a;");
    this.error.managedProperty().bind(this.error.textProperty().isNotEmpty());
    this.error.visibleProperty().bind(this.error.managedProperty());
    final Button apply = new Button("Применить и подтвердить свойства");
    apply.setId("chemistry-quantification-apply");
    apply.getStyleClass().add("laboratory-save");
    apply.setOnAction(
        event -> {
          if (this.applied) {
            return;
          }
          try {
            if (this.recoveredConflict || !this.originalCurrent()) {
              throw new IllegalArgumentException(
                  "Компонент изменён во время ввода. Вернитесь к опыту и откройте форму заново.");
            }
            final var next = this.draft();
            this.editor.quantify(
                next.id(), next.planned(), next.actual(), next.evidence(), next.properties());
            this.applied = true;
            apply.setDisable(true);
            this.close.run();
          } catch (final RuntimeException exception) {
            this.message(exception.getMessage(), true);
          }
        });
    final Button back = new Button("Назад");
    back.setOnAction(event -> this.back());
    final Button discard = new Button("Отменить ввод");
    discard.setOnAction(
        event -> {
          this.applied = true;
          this.close.run();
        });
    final Button keep = new Button("Продолжить заполнение");
    keep.setOnAction(
        event -> {
          this.confirmation.setManaged(false);
          this.confirmation.setVisible(false);
          this.error.setText("");
        });
    this.confirmation.getChildren().setAll(label("Изменения ещё не применены."), discard, keep);
    this.confirmation.setVisible(false);
    this.confirmation.setManaged(false);
    final VBox footer =
        new VBox(8, this.error, this.confirmation, new FlowPane(10, 8, apply, back));
    footer.setPadding(new javafx.geometry.Insets(12, 18, 12, 18));
    footer.setMaxWidth(740);
    this.setBottom(new StackPane(footer));
    this.initial = this.values();
    this.loading = false;
    this.preview();
  }

  boolean canClose() {
    if (!this.pending()) {
      return true;
    }
    this.confirmation.setVisible(true);
    this.confirmation.setManaged(true);
    this.message("Примените изменения или явно отмените ввод перед закрытием.", true);
    return false;
  }

  boolean pending() {
    return !this.applied && (this.recoveredConflict || !this.initial.equals(this.values()));
  }

  private boolean originalCurrent() {
    return this.experimentId.equals(this.editor.document().experiment().id())
        && this.editor.document().experiment().components().stream()
            .anyMatch(this.original::equals);
  }

  void draft(final java.util.Properties fields, final boolean restore) {
    if (restore) {
      if (!this.original.id().equals(fields.getProperty("quantityForm.component"))
          || !this.experimentId.equals(fields.getProperty("quantityForm.experiment"))) {
        throw new IllegalArgumentException(
            "Черновик количеств относится к другому компоненту или опыту.");
      }
      this.loading = true;
    } else {
      fields.setProperty("quantityForm.component", this.original.id());
      fields.setProperty("quantityForm.experiment", this.experimentId);
      fields.setProperty(
          "quantityForm.conflict",
          Boolean.toString(this.recoveredConflict || !this.originalCurrent()));
    }
    try {
      this.quantities.draft(fields, "quantityForm.quantities", restore);
      LaboratoryFormDraft.fields(
          fields,
          "quantityForm.properties",
          restore,
          java.util.Map.of(
              "mass",
              this.mass,
              "density",
              this.density,
              "purity",
              this.purity,
              "concentration",
              this.concentration,
              "source",
              this.source));
      if (restore) {
        this.recoveredConflict =
            Boolean.parseBoolean(fields.getProperty("quantityForm.conflict", "false"));
      }
    } finally {
      if (restore) {
        this.loading = false;
        this.preview();
      }
    }
    if (restore) {
      this.message(
          this.recoveredConflict
              ? "Компонент изменился до сохранения черновика. Ввод восстановлен для просмотра; скопируйте нужные значения и откройте новую форму для проверки."
              : this.pending()
                  ? "Восстановлен незавершённый ввод. Проверьте поля и примените изменения."
                  : "Восстановленные значения совпадают с текущими данными опыта.",
          this.recoveredConflict);
    }
  }

  private void message(final String text, final boolean problem) {
    this.error.setStyle(
        problem ? "-fx-text-fill: #a52a2a;" : "-fx-text-fill: -fx-text-base-color;");
    this.error.setText(text);
  }

  private void back() {
    if (this.canClose()) {
      this.close.run();
    }
  }

  private void changed() {
    if (this.loading) {
      return;
    }
    this.error.setText("");
    this.confirmation.setManaged(false);
    this.confirmation.setVisible(false);
    this.preview();
  }

  private LaboratoryComponent draft() {
    final var properties =
        new LaboratoryMaterialProperties(
            number(this.mass),
            number(this.density),
            number(this.purity),
            number(this.concentration),
            this.source.getText(),
            LaboratoryMaterialProperties.signature(this.original.molecule()));
    return new LaboratoryComponent(
        this.original.id(),
        this.original.molecule(),
        this.original.role(),
        this.quantities.planned(),
        this.quantities.actual(),
        this.quantities.evidence(),
        properties);
  }

  private void preview() {
    this.preview.setTooltip(null);
    this.calculationDetails.clear();
    this.previewDetails.setDisable(true);
    if (this.recoveredConflict) {
      this.preview.setText(
          "Пересчёт недоступен: компонент изменился до сохранения черновика. Восстановленные свойства требуют проверки в новой форме.");
      return;
    }
    try {
      final var component = this.draft();
      final StringBuilder text = new StringBuilder(),
          exact = new StringBuilder(),
          details = new StringBuilder();
      for (final var basis : LaboratoryStoichiometry.Basis.values()) {
        final var amount = LaboratoryStoichiometry.amount(component, basis);
        final String heading =
            basis == LaboratoryStoichiometry.Basis.PLAN
                ? "План: "
                : component.evidence().quantityLabel() + ": ";
        final String precise =
            amount.moles() == null
                ? "не определено"
                : LaboratoryQuantityText.format(
                    amount.moles().movePointRight(3), LaboratoryQuantityUnit.MILLIMOLE, false);
        text.append(heading)
            .append(
                amount.moles() == null
                    ? "не определено · " + amount.explanation()
                    : LaboratoryQuantityText.format(
                        amount.moles().movePointRight(3), LaboratoryQuantityUnit.MILLIMOLE, true))
            .append('\n');
        exact.append(heading).append(precise).append('\n');
        details
            .append(heading)
            .append(precise)
            .append('\n')
            .append(amount.explanation())
            .append("\n\n");
      }
      this.preview.setText(text.toString().strip());
      this.preview.setTooltip(new Tooltip(exact.toString().strip()));
      this.calculationDetails.setText(details.toString().strip());
      this.previewDetails.setDisable(false);
    } catch (final RuntimeException exception) {
      this.preview.setText("Пересчёт пока недоступен: " + exception.getMessage());
    }
  }

  private List<String> values() {
    final var values = new java.util.ArrayList<>(this.quantities.values());
    values.addAll(
        List.of(
            LaboratoryDecimalField.snapshot(this.mass),
            LaboratoryDecimalField.snapshot(this.density),
            LaboratoryDecimalField.snapshot(this.purity),
            LaboratoryDecimalField.snapshot(this.concentration),
            this.source.getText()));
    return List.copyOf(values);
  }

  private static Label label(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMinHeight(Region.USE_PREF_SIZE);
    return label;
  }

  private static TextField field(final String id) {
    final TextField field = new LaboratoryDecimalField();
    field.setId(id);
    field.setPromptText("Не указано");
    return field;
  }

  private static BigDecimal number(final TextField field) {
    try {
      return LaboratoryDecimalField.value(field);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException("Введите число: " + field.getText());
    }
  }
}
