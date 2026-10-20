/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.*;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;

/** A live projection of persisted stage/component values, with no stock side effects. */
final class LaboratoryStoichiometryWindow extends BorderPane {
  private record Reference(String id, String name) {
    public String toString() {
      return this.name;
    }
  }

  private final LaboratoryEditor editor;
  private final LaboratoryFiles files;
  private final String stageId;
  private final ComboBox<LaboratoryStoichiometry.Basis> basis = new ComboBox<>();
  private final ComboBox<Reference> reference = new ComboBox<>();
  private final LaboratoryStoichiometryTable table =
      new LaboratoryStoichiometryTable(this::edit, this::selectReference);
  private final Label status = new Label(), limiting = new Label();
  private final VBox productSummary = new VBox(8);
  private final TextArea detail = new TextArea();
  private LaboratoryStoichiometry.Report report;
  private LaboratoryQuantificationForm quantityForm;
  private LaboratoryBatchPlanPane batch;
  private javafx.scene.Node reportContent, quantityOrigin;
  private final CheckBox strictBalance = new CheckBox("Требовать полный атомный баланс");
  private Runnable preparation;

  void preparation(final Runnable action) {
    this.preparation = action;
  }

  private final Runnable close;
  private final String returnLabel;

  LaboratoryStoichiometryWindow(
      final LaboratoryEditor editor,
      final LaboratoryFiles files,
      final String stageId,
      final Runnable close) {
    this(editor, files, stageId, "К реакции", close);
  }

  LaboratoryStoichiometryWindow(
      final LaboratoryEditor editor,
      final LaboratoryFiles files,
      final String stageId,
      final String returnLabel,
      final Runnable close) {
    this.editor = editor;
    this.files = files;
    this.stageId = stageId;
    this.returnLabel = returnLabel;
    this.close = close;
    this.build();
  }

  boolean canClose() {
    return this.quantityForm != null
        ? this.quantityForm.canClose()
        : this.batch == null || this.batch.canClose();
  }

  boolean pending() {
    return this.quantityForm != null && this.quantityForm.pending()
        || this.batch != null && this.batch.pending();
  }

  void draft(final Properties fields, final boolean restore) {
    if (restore && fields.containsKey("batch.stage")) {
      this.batch.draft(fields, true);
      return;
    }
    if (!restore && this.batch != null) {
      this.batch.draft(fields, false);
    }
    if (restore) {
      final String id = fields.getProperty("quantityForm.component");
      if (this.stage().participants().stream()
          .noneMatch(participant -> participant.componentId().equals(id))) {
        throw new IllegalArgumentException(
            "Компонент черновика не входит в восстановленную стадию.");
      }
      this.edit(id);
      this.quantityForm.draft(fields, true);
    } else if (this.quantityForm != null && this.quantityForm.pending()) {
      fields.setProperty("quantityForm.stage", this.stageId);
      this.quantityForm.draft(fields, false);
    }
  }

  private void edit(final String id) {
    final javafx.scene.Node summary = this.getCenter();
    this.quantityOrigin = summary;
    final LaboratoryComponent before =
        this.editor.document().experiment().components().stream()
            .filter(component -> component.id().equals(id))
            .findFirst()
            .orElseThrow();
    this.quantityForm =
        new LaboratoryQuantificationForm(
            this.editor,
            id,
            () -> {
              this.quantityForm = null;
              this.batch.reload();
              final LaboratoryComponent after =
                  this.editor.document().experiment().components().stream()
                      .filter(component -> component.id().equals(id))
                      .findFirst()
                      .orElseThrow();
              final boolean contributesToYield =
                  this.stage().participants().stream()
                      .anyMatch(
                          participant ->
                              participant.componentId().equals(id)
                                  && (participant.role() == ReactionComponentRole.REACTANT
                                      || participant.role() == ReactionComponentRole.PRODUCT));
              if (contributesToYield
                  && (!Objects.equals(before.actual(), after.actual())
                      || after.actual() != null && before.evidence() != after.evidence())) {
                this.basis.setValue(LaboratoryStoichiometry.Basis.ACTUAL);
              }
              this.setCenter(summary);
              this.refresh();
            });
    this.setCenter(this.quantityForm);
  }

  private LaboratoryReactionStage stage() {
    return this.editor.document().experiment().stages().stream()
        .filter(item -> item.id().equals(this.stageId))
        .findFirst()
        .orElseThrow();
  }

  private void build() {
    this.setId("chemistry-stoichiometry-page");
    this.basis.getItems().setAll(LaboratoryStoichiometry.Basis.values());
    final boolean hasActual =
        this.stage().participants().stream()
            .filter(p -> p.role() == ReactionComponentRole.REACTANT)
            .anyMatch(
                p ->
                    this.editor.document().experiment().components().stream()
                        .anyMatch(c -> c.id().equals(p.componentId()) && c.actual() != null));
    this.basis.setValue(
        hasActual ? LaboratoryStoichiometry.Basis.ACTUAL : LaboratoryStoichiometry.Basis.PLAN);
    this.basis.setId("chemistry-stoichiometry-basis");
    this.basis.setConverter(
        new javafx.util.StringConverter<>() {
          public String toString(final LaboratoryStoichiometry.Basis value) {
            return value == LaboratoryStoichiometry.Basis.ACTUAL
                ? "Внесённые количества"
                : "Плановые количества";
          }

          public LaboratoryStoichiometry.Basis fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    for (final var participant : this.stage().participants()) {
      if (participant.role() == ReactionComponentRole.REACTANT) {
        final var component =
            this.editor.document().experiment().components().stream()
                .filter(item -> item.id().equals(participant.componentId()))
                .findFirst()
                .orElseThrow();
        this.reference.getItems().add(new Reference(component.id(), this.componentName(component)));
      }
    }
    this.reference.getSelectionModel().selectFirst();
    this.reference.setId("chemistry-stoichiometry-reference");
    this.reference.setPrefWidth(230);
    this.basis.setOnAction(event -> this.refresh());
    this.reference.setOnAction(event -> this.refresh());
    this.table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, before, row) ->
                this.detail.setText(
                    row == null
                        ? "Выберите строку для формулы пересчёта и диагностики."
                        : row.component().molecule().displayName() + "\n" + row.diagnostic()));
    this.detail.setEditable(false);
    this.detail.setWrapText(true);
    this.detail.setPrefRowCount(4);
    this.detail.setId("chemistry-stoichiometry-detail");
    this.status.setWrapText(true);
    this.status.setId("chemistry-stoichiometry-status");
    this.limiting.setWrapText(true);
    final Button edit = new Button("Количество и свойства…");
    edit.setId("chemistry-selected-quantity");
    edit.setTooltip(new Tooltip("Двойной щелчок по строке, Enter или F2."));
    edit.disableProperty().bind(this.table.getSelectionModel().selectedItemProperty().isNull());
    edit.setOnAction(
        event -> this.edit(this.table.getSelectionModel().getSelectedItem().component().id()));
    final MenuItem editReference = new MenuItem("Количество исходника за 1 эквивалент…");
    editReference.setId("chemistry-reference-quantity");
    editReference.disableProperty().bind(this.reference.valueProperty().isNull());
    editReference.setOnAction(event -> this.edit(this.reference.getValue().id()));
    final MenuItem copy = new MenuItem("Копировать расчёт");
    copy.setOnAction(
        event -> {
          final ClipboardContent content = new ClipboardContent();
          content.putString(this.text());
          Clipboard.getSystemClipboard().setContent(content);
        });
    final MenuItem record = new MenuItem("Записать расчёт в журнал");
    record.setOnAction(
        event -> {
          this.editor.record(LaboratoryEntry.Kind.ACTION, this.text(), null);
          this.status.setText(
              "Снимок расчёта добавлен в журнал. Сохраните опыт, чтобы записать его в файл.");
        });
    final Button save = new Button("Сохранить опыт");
    save.setOnAction(
        event -> {
          if (this.files.save(false)) {
            this.status.setText("Опыт сохранён вместе с количествами и свойствами.");
          }
        });
    final Label note =
        new Label(
            "В препаративном режиме теория — количество продукта при 100 % превращении выбранного исходника по коэффициентам стадии. Полный атомный баланс проверяется отдельно. Эквиваленты загрузки не заменяют стехиометрические коэффициенты. План и внесённые результаты независимы. Для процента выхода укажите происхождение количеств: наблюдение, литература или модель. Смешанные источники отмечаются отдельно. Общие компоненты нескольких стадий имеют общее количество.");
    note.setWrapText(true);
    final TitledPane details =
        new TitledPane(
            "Основание и подробности расчёта", new VBox(8, this.strictBalance, this.detail, note));
    details.setExpanded(false);
    this.strictBalance.setOnAction(event -> this.refresh());
    final MenuButton actions =
        new MenuButton("Ещё", null, editReference, new SeparatorMenuItem(), copy, record);
    final CheckMenuItem columns =
        new CheckMenuItem("Коэффициенты и теоретические количества в таблице");
    columns.setId("chemistry-stoichiometry-columns");
    columns.setOnAction(event -> this.table.details(columns.isSelected()));
    actions.getItems().addAll(new SeparatorMenuItem(), columns);
    final VBox controls =
        new VBox(
            8,
            new FlowPane(8, 6, this.basis, new Label("За 1 эквивалент"), this.reference),
            new FlowPane(8, 6, edit, save, actions));
    final VBox content =
        new VBox(8, controls, this.productSummary, this.limiting, this.table, this.status, details);
    for (final Region section :
        List.of(controls, this.productSummary, this.limiting, this.status, details)) {
      section.setMinHeight(Region.USE_PREF_SIZE);
    }
    VBox.setVgrow(this.table, Priority.ALWAYS);
    content.setPadding(new javafx.geometry.Insets(12));
    final Label heading = new Label(this.stage().title());
    heading.setWrapText(true);
    heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    final Button back = new Button(this.returnLabel);
    back.setOnAction(
        event -> {
          if (this.canClose()) {
            this.close.run();
          }
        });
    final FlowPane header = new FlowPane(12, 8, back, heading);
    header.setPadding(new javafx.geometry.Insets(12));
    final ScrollPane reportScroll = new ScrollPane(content);
    reportScroll.setFitToWidth(true);
    reportScroll.setFitToHeight(true);
    reportScroll.setId("chemistry-stoichiometry-scroll");
    this.reportContent = reportScroll;
    this.batch = new LaboratoryBatchPlanPane(this.editor, this.stage(), this::edit, this::refresh);
    final ToggleButton loading = new ToggleButton("Рассчитать загрузку"),
        yield = new ToggleButton("Количества и выход");
    final ToggleGroup tabs = new ToggleGroup();
    loading.setToggleGroup(tabs);
    yield.setToggleGroup(tabs);
    loading.setSelected(true);
    loading.setId("chemistry-loading-tab");
    yield.setId("chemistry-yield-tab");
    final Runnable syncTabs =
        () -> {
          final javafx.scene.Node current =
              this.quantityForm == null ? this.getCenter() : this.quantityOrigin;
          loading.setSelected(current == this.batch);
          yield.setSelected(current == this.reportContent);
        };
    this.centerProperty().addListener((observable, before, after) -> syncTabs.run());
    loading.setOnAction(
        event -> {
          if (this.canClose()) {
            this.quantityForm = null;
            this.setCenter(this.batch);
          }
          syncTabs.run();
        });
    yield.setOnAction(
        event -> {
          if (this.canClose()) {
            this.quantityForm = null;
            this.refresh();
            this.setCenter(this.reportContent);
          }
          syncTabs.run();
        });
    final Button prepare = new Button("Подготовить реагенты");
    prepare.setId("chemistry-loading-prepare");
    prepare.setOnAction(
        e -> {
          if (this.canClose() && this.preparation != null) {
            this.preparation.run();
          }
        });
    final FlowPane navigation = new FlowPane(8, 6, loading, yield, prepare);
    navigation.setPadding(new javafx.geometry.Insets(0, 12, 8, 12));
    this.setTop(new VBox(0, header, navigation));
    this.setCenter(this.batch);
    this.refresh();
  }

  private void refresh() {
    final String selected =
        this.table.getSelectionModel().getSelectedItem() == null
            ? null
            : this.table.getSelectionModel().getSelectedItem().component().id();
    this.report =
        this.strictBalance.isSelected()
            ? LaboratoryStoichiometry.calculate(
                this.editor.document().experiment(),
                this.stage(),
                this.basis.getValue(),
                this.reference.getValue() == null ? null : this.reference.getValue().id())
            : LaboratoryStoichiometry.preparative(
                this.editor.document().experiment(),
                this.stage(),
                this.basis.getValue(),
                this.reference.getValue() == null ? null : this.reference.getValue().id());
    this.table.basis(this.report.basis());
    this.table.getItems().setAll(this.report.rows());
    this.table.sort();
    this.table
        .getSelectionModel()
        .select(
            this.report.rows().stream()
                .filter(row -> row.component().id().equals(selected))
                .findFirst()
                .orElse(this.report.rows().isEmpty() ? null : this.report.rows().getFirst()));
    final String names =
        this.report.rows().stream()
            .filter(row -> this.report.limitingIds().contains(row.component().id()))
            .map(row -> this.componentName(row.component()))
            .collect(java.util.stream.Collectors.joining(", "));
    this.limiting.setText(
        "Лимитирующие исходники по коэффициентам: "
            + (names.isEmpty() ? "недостаточно данных" : names));
    this.productSummary.getChildren().clear();
    for (final var row : this.report.rows()) {
      if (row.participant().role() != ReactionComponentRole.PRODUCT) {
        continue;
      }
      final Label name = new Label(row.component().molecule().displayName());
      name.setWrapText(true);
      name.setMaxWidth(600);
      name.setMinHeight(Region.USE_PREF_SIZE);
      name.setStyle("-fx-font-weight: bold;");
      final Label amounts =
          new Label(
              "Теория 100 %: "
                  + this.theoretical(row)
                  + (this.report.basis() == LaboratoryStoichiometry.Basis.ACTUAL
                      ? "     " + row.component().evidence().quantityLabel() + ": "
                      : "     План продукта: ")
                  + (row.input() == null
                      ? "—"
                      : number(row.input().value()) + " " + row.input().unit()));
      amounts.setWrapText(true);
      amounts.setMaxWidth(650);
      amounts.setMinHeight(Region.USE_PREF_SIZE);
      final boolean excessive =
          row.yieldPercent() != null && row.yieldPercent().compareTo(new BigDecimal("100")) > 0;
      final Label percent =
          new Label(
              this.report.basis() == LaboratoryStoichiometry.Basis.PLAN
                  ? "Плановый расчёт"
                  : row.yieldPercent() == null
                      ? "Выход не рассчитан"
                      : LaboratoryStoichiometry.yieldLabel(this.report, row)
                          + ": "
                          + row.yieldPercent()
                              .setScale(2, RoundingMode.HALF_UP)
                              .stripTrailingZeros()
                              .toPlainString()
                          + " %");
      percent.setId("chemistry-product-yield-" + row.component().id());
      percent.setStyle(
          "-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: "
              + (excessive ? "-app-danger-text" : "-app-accent")
              + ";");
      final Label hint = new Label(this.yieldHint(row));
      hint.setId("chemistry-product-hint-" + row.component().id());
      hint.setWrapText(true);
      hint.setMaxWidth(600);
      hint.setMinHeight(Region.USE_PREF_SIZE);
      hint.setVisible(!hint.getText().isBlank());
      hint.setManaged(hint.isVisible());
      final Button actual = new Button("Количество и источник данных…");
      actual.setId("chemistry-product-quantity-" + row.component().id());
      actual.setOnAction(event -> this.edit(row.component().id()));
      final VBox card = new VBox(6, name, amounts, percent, hint, actual);
      card.setPadding(new javafx.geometry.Insets(14));
      card.setMaxWidth(740);
      card.setMinHeight(Region.USE_PREF_SIZE);
      card.getStyleClass().add("laboratory-batch-anchor");
      this.productSummary.getChildren().add(card);
    }
    this.status.setText(
        this.report.diagnostics().isEmpty()
            ? "Пересчёт выполнен по введённым значениям. Складские операции не создаются."
            : String.join("\n", this.report.diagnostics()));
  }

  private String yieldHint(final LaboratoryStoichiometry.Row row) {
    if (this.report.basis() == LaboratoryStoichiometry.Basis.PLAN) {
      return "Показана теория по плану. Для выхода выберите «Внесённые количества»; нужны количества исходника и продукта с указанным происхождением.";
    }
    if (row.input() == null) {
      return "Укажите количество продукта и источник данных.";
    }
    if (!row.component().evidence().reported()) {
      return "Количество указано без источника результата. В форме выберите наблюдение, литературные или модельные данные.";
    }
    if (row.amount().moles() == null) {
      return row.amount().explanation();
    }
    if (row.participant().coefficient() == null) {
      return "Укажите стехиометрический коэффициент продукта в стадии.";
    }
    final var referenceRow =
        this.report.rows().stream()
            .filter(item -> item.component().id().equals(this.report.referenceId()))
            .findFirst()
            .orElse(null);
    if (referenceRow == null) {
      return "Выберите исходник за 1 эквивалент.";
    }
    if (referenceRow.input() == null) {
      return "Укажите внесённое количество исходника «"
          + referenceRow.component().molecule().displayName()
          + "».";
    }
    if (referenceRow.amount().moles() == null) {
      return "Исходник «"
          + referenceRow.component().molecule().displayName()
          + "»: "
          + referenceRow.amount().explanation();
    }
    if (referenceRow.participant().coefficient() == null) {
      return "Укажите стехиометрический коэффициент опорного исходника в стадии.";
    }
    if (referenceRow.amount().moles().signum() == 0) {
      return "Внесённое количество опорного исходника равно нулю; процент выхода не определён.";
    }
    if (row.theoreticalMoles() == null) {
      return String.join("\n", this.report.diagnostics());
    }
    if (row.yieldPercent() != null && row.yieldPercent().compareTo(new BigDecimal("100")) > 0) {
      return "Выход выше 100 %. Проверьте количества, массовую долю, структуру и коэффициенты.";
    }
    return row.yieldPercent() == null
        ? ""
        : LaboratoryStoichiometry.yieldLabel(this.report, row)
            + ". Источник продукта: "
            + row.component().evidence()
            + ".";
  }

  private String theoretical(final LaboratoryStoichiometry.Row row) {
    if (row.theoreticalMoles() == null) {
      return "—";
    }
    final var p = row.component().properties();
    return mmol(row.theoreticalMoles())
        + " ммоль"
        + (p.matches(row.component().molecule()) && p.molarMass() != null
            ? " / "
                + number(row.theoreticalMoles().multiply(p.molarMass()).movePointRight(3))
                + " мг"
            : "");
  }

  private String componentName(final LaboratoryComponent component) {
    return component.molecule().displayName();
  }

  private String text() {
    final StringBuilder text =
        new StringBuilder("Количества стадии «")
            .append(this.stage().title())
            .append("» · ")
            .append(this.basis.getConverter().toString(this.report.basis()))
            .append("\nЭталон эквивалентов: ")
            .append(this.report.referenceId())
            .append("\n");
    for (final var row : this.report.rows()) {
      text.append(row.component().molecule().displayName())
          .append(" [")
          .append(row.component().id())
          .append("] · ")
          .append(row.participant().role())
          .append(" · коэффициент: ")
          .append(
              row.participant().coefficient() == null
                  ? "не указан"
                  : row.participant().coefficient().value())
          .append(" · источник: ")
          .append(row.component().evidence())
          .append(" · введено: ")
          .append(Objects.toString(row.input(), "не указано"))
          .append(" · n, моль: ")
          .append(exact(row.amount().moles()))
          .append(" · экв.: ")
          .append(exact(row.equivalents()))
          .append(" · теория, моль: ")
          .append(exact(row.theoreticalMoles()))
          .append(" · ")
          .append(LaboratoryStoichiometry.yieldLabel(this.report, row))
          .append(", %: ")
          .append(exact(row.yieldPercent()))
          .append("\n")
          .append(row.diagnostic())
          .append("\n");
    }
    text.append("Лимитирующие исходники: ")
        .append(this.report.limitingIds())
        .append("\n")
        .append(String.join("\n", this.report.diagnostics()));
    return text.toString();
  }

  private static String exact(final BigDecimal value) {
    return value == null ? "не определено" : value.stripTrailingZeros().toPlainString();
  }

  private static String number(final BigDecimal value) {
    return LaboratoryStoichiometryTable.display(value);
  }

  private static String mmol(final BigDecimal value) {
    return number(value == null ? null : value.movePointRight(3));
  }

  private void selectReference(final String id) {
    this.reference.getItems().stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .ifPresent(this.reference::setValue);
  }
}
