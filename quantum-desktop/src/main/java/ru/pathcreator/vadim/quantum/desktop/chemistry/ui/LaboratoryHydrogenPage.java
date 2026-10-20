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
import java.util.Map;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

final class LaboratoryHydrogenPage extends LaboratoryStructurePage {
  private final TableView<MoleculeHydrogenEditing.Suggestion> rows = new TableView<>();
  private final Map<AtomId, Integer> counts = new LinkedHashMap<>();
  private final Map<AtomId, Integer> numbers = new LinkedHashMap<>();
  private final LaboratoryStructurePreview preview = new LaboratoryStructurePreview();
  private final Label summary = note("");
  private final Label explanation = note("");
  private final Spinner<Integer> count = new Spinner<>(0, 8, 0);
  private final javafx.animation.PauseTransition debounce =
      new javafx.animation.PauseTransition(javafx.util.Duration.millis(120));
  private final CheckBox unresolved = new CheckBox("Только без предложения");
  private final java.util.List<MoleculeHydrogenEditing.Suggestion> suggestions;

  private record Preview(MoleculeDrawing drawing, java.util.List<String> messages) {}

  private final TitledPane review = new TitledPane();
  private final TextArea reviewText = new TextArea();
  private javafx.concurrent.Task<Preview> loading;
  private MoleculeDrawing proposed;
  private long generation;
  private boolean selecting;

  LaboratoryHydrogenPage(
      final LaboratoryEditor editor, final String componentId, final Runnable close) {
    super(editor, componentId, "hydrogens", "Явные водороды", close);
    this.apply.setText("Добавить H в структуру");
    this.suggestions = MoleculeHydrogenEditing.suggest(this.source);
    this.proposed = this.source;
    for (int i = 0; i < this.source.molecule().atoms().size(); i++) {
      this.numbers.put(this.source.molecule().atoms().get(i).id(), i + 1);
    }
    this.suggestions.forEach(
        s -> this.counts.put(s.atomId(), s.additional() == null ? 0 : s.additional()));
    this.rows.setId("chemistry-hydrogen-rows");
    this.rows.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    this.column("Атом", s -> this.label(s.atomId()), 100);
    this.column("Уже H", s -> Integer.toString(s.present()), 60);
    this.column("Добавить", s -> Integer.toString(this.counts.get(s.atomId())), 75);
    this.column(
        "Предложение",
        s -> s.additional() == null ? "Задать вручную" : Integer.toString(s.additional()),
        120);
    this.rows.setPlaceholder(note("Атомов для этой выборки нет"));
    this.rows.setMinHeight(130);
    this.rows.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> this.select(s));
    this.unresolved.setId("chemistry-hydrogen-unresolved");
    this.unresolved.setOnAction(event -> this.filter());
    this.count.setId("chemistry-hydrogen-selected-count");
    this.count.setPrefWidth(90);
    this.count.setDisable(true);
    this.count
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              final var selected = this.rows.getSelectionModel().getSelectedItem();
              if (this.selecting || selected == null) {
                return;
              }
              this.counts.put(selected.atomId(), b);
              this.rows.refresh();
              this.schedule();
            });
    final var controls =
        new FlowPane(
            8,
            6,
            this.unresolved,
            button("Вернуть предложения", () -> this.bulk(true)),
            button("Ничего не добавлять", () -> this.bulk(false)));
    final VBox table =
        new VBox(
            8,
            controls,
            this.rows,
            new FlowPane(8, 6, note("Добавить H к выбранному атому"), this.count),
            this.explanation);
    table.setMinWidth(0);
    VBox.setVgrow(this.rows, Priority.ALWAYS);
    final SplitPane split = new SplitPane(table, this.preview);
    split.setDividerPositions(0.52);
    split
        .widthProperty()
        .addListener(
            (o, a, b) ->
                split.setOrientation(
                    b.doubleValue() < 740
                        ? javafx.geometry.Orientation.VERTICAL
                        : javafx.geometry.Orientation.HORIZONTAL));
    this.preview.onPick(
        id -> {
          this.unresolved.setSelected(false);
          this.filter();
          this.suggestions.stream()
              .filter(s -> s.atomId().equals(id))
              .findFirst()
              .ifPresent(
                  s -> {
                    this.rows.getSelectionModel().select(s);
                    this.rows.scrollTo(s);
                  });
        });
    this.summary.setId("chemistry-hydrogen-summary");
    this.review.setId("chemistry-hydrogen-review");
    this.review.setAnimated(false);
    this.review.setExpanded(false);
    this.review.setContent(this.reviewText);
    this.reviewText.setEditable(false);
    this.reviewText.setWrapText(true);
    this.reviewText.setPrefRowCount(3);
    this.reviewText.setId("chemistry-hydrogen-review-text");
    this.review.setVisible(false);
    this.review.setManaged(false);
    final VBox body =
        new VBox(
            8,
            note(
                "Предложения основаны на обычной нейтральной валентности. Заряженные, радикальные и неоднозначные атомы требуют ручного выбора. 0 в такой строке не подтверждает отсутствие H."),
            this.summary,
            this.review,
            split);
    body.setPadding(new javafx.geometry.Insets(4, 16, 4, 16));
    VBox.setVgrow(split, Priority.ALWAYS);
    this.content(body);
    this.debounce.setOnFinished(event -> this.calculate());
    this.filter();
    if (!this.rows.getItems().isEmpty()) {
      this.rows.getSelectionModel().selectFirst();
    }
    this.seal();
    this.schedule();
  }

  private String label(final AtomId id) {
    return this.source.molecule().atoms().stream()
            .filter(a -> a.id().equals(id))
            .findFirst()
            .orElseThrow()
            .element()
            .symbol()
            .value()
        + " · "
        + this.numbers.get(id);
  }

  private void column(
      final String name,
      final java.util.function.Function<MoleculeHydrogenEditing.Suggestion, String> text,
      final int width) {
    final TableColumn<MoleculeHydrogenEditing.Suggestion, String> column = new TableColumn<>(name);
    column.setCellValueFactory(c -> new ReadOnlyStringWrapper(text.apply(c.getValue())));
    column.setPrefWidth(width);
    column.setMinWidth(Math.min(width, 70));
    this.rows.getColumns().add(column);
  }

  private void filter() {
    final var selected = this.rows.getSelectionModel().getSelectedItem();
    this.rows
        .getItems()
        .setAll(
            this.suggestions.stream()
                .filter(s -> !this.unresolved.isSelected() || s.additional() == null)
                .toList());
    if (selected != null && this.rows.getItems().contains(selected)) {
      this.rows.getSelectionModel().select(selected);
    }
  }

  private void select(final MoleculeHydrogenEditing.Suggestion selected) {
    this.selecting = true;
    try {
      this.count.setDisable(selected == null);
      this.count
          .getValueFactory()
          .setValue(selected == null ? 0 : this.counts.get(selected.atomId()));
    } finally {
      this.selecting = false;
    }
    this.explanation.setText(
        selected == null
            ? "Выберите атом для ручного количества."
            : this.label(selected.atomId()) + " · " + selected.explanation());
    this.preview.show(this.proposed, selected == null ? null : selected.atomId());
  }

  private void bulk(final boolean suggestions) {
    this.suggestions.forEach(
        s ->
            this.counts.put(
                s.atomId(), suggestions && s.additional() != null ? s.additional() : 0));
    this.rows.refresh();
    this.select(this.rows.getSelectionModel().getSelectedItem());
    this.schedule();
  }

  private void schedule() {
    if (this.restoring || this.disposed) {
      return;
    }
    this.generation++;
    if (this.loading != null) {
      this.loading.cancel(true);
    }
    this.apply.setDisable(true);
    this.summary.setText("Подготовка предпросмотра…");
    this.debounce.playFromStart();
  }

  private void calculate() {
    if (this.disposed) {
      return;
    }
    final long version = this.generation;
    final var counts = Map.copyOf(this.counts);
    final var task =
        new javafx.concurrent.Task<Preview>() {
          protected Preview call() {
            final var drawing = MoleculeHydrogenEditing.add(source, counts);
            if (this.isCancelled()) {
              throw new java.util.concurrent.CancellationException();
            }
            final var messages = new java.util.ArrayList<String>();
            final long unknown = suggestions.stream().filter(s -> s.additional() == null).count();
            if (unknown > 0) {
              messages.add(
                  "Атомов без автоматического предложения: "
                      + unknown
                      + ". Проверьте их вручную; 0 не означает подтверждённое отсутствие H.");
            }
            new ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator()
                .validateMolecule(drawing.molecule()).diagnostics().stream()
                    .filter(
                        d ->
                            d.severity()
                                    == ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic
                                        .ChemistryDiagnosticSeverity.ERROR
                                || d.code()
                                    == ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic
                                        .ChemistryDiagnosticCode.MOLECULE_VALENCE_EXCEEDS_PROFILE
                                || d.code()
                                    == ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic
                                        .ChemistryDiagnosticCode
                                        .MOLECULE_STEREOCHEMISTRY_NEEDS_REVIEW)
                    .forEach(
                        d -> messages.add(new LaboratoryStructureCheck.Issue("", "", d).title()));
            return new Preview(drawing, java.util.List.copyOf(messages));
          }
        };
    this.loading = task;
    task.setOnSucceeded(
        event -> {
          if (this.disposed || version != this.generation) {
            return;
          }
          this.loading = null;
          this.proposed = task.getValue().drawing();
          final int added =
              this.proposed.molecule().atomCount() - this.source.molecule().atomCount();
          this.summary.setText(
              this.source.molecule().formula().hillNotation()
                  + " → "
                  + this.proposed.molecule().formula().hillNotation()
                  + " · добавить H: "
                  + added
                  + " · "
                  + java.math.BigDecimal.valueOf(
                          this.proposed.molecule().composition().mass().averageAtomicMass())
                      .setScale(4, java.math.RoundingMode.HALF_UP)
                      .stripTrailingZeros()
                      .toPlainString()
                  + " г/моль");
          final var messages = task.getValue().messages();
          this.review.setVisible(!messages.isEmpty());
          this.review.setManaged(!messages.isEmpty());
          this.review.setText("Проверить перед применением · " + messages.size());
          this.reviewText.setText(String.join("\n", messages));
          this.select(this.rows.getSelectionModel().getSelectedItem());
          this.apply.setDisable(added == 0);
        });
    task.setOnFailed(
        event -> {
          if (!this.disposed && version == this.generation) {
            this.loading = null;
            this.error.setText(task.getException().getMessage());
            this.summary.setText("Предпросмотр не построен; введённые количества сохранены.");
          }
        });
    final Thread worker = new Thread(task, "chemistry-hydrogen-preview");
    worker.setDaemon(true);
    worker.start();
  }

  protected Map<String, String> values() {
    final var values = new LinkedHashMap<String, String>();
    this.counts.forEach((id, value) -> values.put(id.value(), value.toString()));
    return values;
  }

  protected void restoreValues(final Map<String, String> values) {
    if (!values.keySet().equals(this.values().keySet())) {
      throw new IllegalArgumentException(
          "Атомы черновика водородов не совпадают с исходной структурой.");
    }
    final var parsed = new LinkedHashMap<AtomId, Integer>();
    values.forEach(
        (id, value) -> {
          final int number = Integer.parseInt(value);
          if (number < 0 || number > 8) {
            throw new IllegalArgumentException("Число добавляемых H должно быть от 0 до 8.");
          }
          parsed.put(AtomId.of(id), number);
        });
    this.counts.putAll(parsed);
    this.rows.refresh();
    this.select(this.rows.getSelectionModel().getSelectedItem());
    this.generation++;
    this.apply.setDisable(true);
    this.debounce.stop();
    if (this.loading != null) {
      this.loading.cancel(true);
    }
    javafx.application.Platform.runLater(this::schedule);
  }

  protected MoleculeDrawing build() {
    if (this.loading != null
        || this.debounce.getStatus() == javafx.animation.Animation.Status.RUNNING
        || this.apply.isDisabled()) {
      throw new IllegalArgumentException("Дождитесь готового предпросмотра с добавляемыми H.");
    }
    return this.proposed;
  }

  @Override
  void dispose() {
    super.dispose();
    this.generation++;
    this.debounce.stop();
    if (this.loading != null) {
      this.loading.cancel(true);
    }
  }
}
