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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

final class LaboratoryInspector extends VBox {

  private final TextField name = new TextField();
  private final Label identityHint = new Label();
  private final TextField molecularCharge = new TextField("0");
  private final TextField multiplicity = new TextField("1");
  private final Label stereoHint = new Label();
  private final Label fragmentHint = new Label();
  private final ComboBox<ReactionComponentRole> role = new ComboBox<>();
  private final TextField element = new TextField("C");
  private final TextField charge = new TextField("0");
  private final TextField isotope = new TextField();
  private final TextField radical = new TextField("0");
  private final LaboratoryCoordinateFields coordinates;
  private final ComboBox<BondType> bond = new ComboBox<>();
  private final LaboratoryQuantityFields quantities;
  private List<String> displayed = List.of();

  LaboratoryInspector(final Runnable apply, final Runnable changed) {
    this.setSpacing(7);
    this.quantities = new LaboratoryQuantityFields(changed);
    this.coordinates = new LaboratoryCoordinateFields(changed);
    this.role.getItems().setAll(ReactionComponentRole.values());
    this.role.setConverter(
        new StringConverter<>() {
          public String toString(final ReactionComponentRole value) {
            return roleText(value);
          }

          public ReactionComponentRole fromString(final String value) {
            throw new UnsupportedOperationException();
          }
        });
    this.role.setValue(ReactionComponentRole.REACTANT);
    this.bond
        .getItems()
        .setAll(BondType.SINGLE, BondType.DOUBLE, BondType.TRIPLE, BondType.AROMATIC);
    this.bond.setValue(BondType.SINGLE);
    this.bond.setConverter(
        new StringConverter<>() {
          public String toString(final BondType type) {
            if (type == null) {
              return "";
            }
            return switch (type) {
              case SINGLE -> "Одинарная";
              case DOUBLE -> "Двойная";
              case TRIPLE -> "Тройная";
              case AROMATIC -> "Ароматическая";
              case COORDINATE -> "Координационная";
              case UNKNOWN -> "Не определена";
            };
          }

          public BondType fromString(final String text) {
            throw new UnsupportedOperationException();
          }
        });
    this.fragmentHint.setWrapText(true);
    this.stereoHint.setWrapText(true);
    this.element.setId("chemistry-element");
    this.isotope.setPromptText("Не задан");
    final Button button = new Button("Применить свойства");
    button.setOnAction(event -> apply.run());
    this.getChildren()
        .addAll(
            new Label("Подпись на схеме"),
            this.name,
            this.identityHint,
            this.role,
            this.fragmentHint,
            this.stereoHint,
            new Label("Заряд всей молекулы"),
            this.molecularCharge,
            new Label("Спиновая мультиплетность"),
            this.multiplicity,
            new Label("Атом · символ элемента"),
            this.element,
            new Label("Формальный заряд"),
            this.charge,
            new Label("Массовое число изотопа"),
            this.isotope,
            new Label("Неспаренных электронов"),
            this.radical,
            this.coordinates,
            this.quantities,
            button,
            new Label("Инструмент связи"),
            this.bond,
            new Label(
                "Пустое количество — не указано; 0 — явно записанный ноль. Происхождение задаётся отдельно."));
    for (final TextField field :
        List.of(
            this.name,
            this.element,
            this.charge,
            this.isotope,
            this.radical,
            this.molecularCharge,
            this.multiplicity)) {
      field.textProperty().addListener((observable, previous, value) -> changed.run());
    }
    this.role.valueProperty().addListener((observable, previous, value) -> changed.run());
  }

  void load(final LaboratoryComponent component, final Atom atom) {
    this.load(component, atom, null);
  }

  void load(
      final LaboratoryComponent component,
      final Atom atom,
      final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing drawing) {
    this.name.setText(component == null ? "" : component.molecule().displayName());
    this.identityHint.setWrapText(true);
    this.identityHint.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    String identityText = "Названия и идентификаторы — в карточке вещества.";
    if (component != null) {
      try {
        final var saved =
            ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.read(
                component.molecule());
        if (saved.isPresent()) {
          final var identity = saved.get();
          final var identifiers =
              new ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity
                  .CdkCompoundIdentifiers();
          final boolean current =
              drawing == null
                  ? identifiers.current(component.molecule(), identity)
                  : identifiers.current(drawing, identity);
          identityText =
              !current
                  ? "Карточка вещества требует проверки: структура, конфигурация или SMILES не согласованы."
                  : !identity.iupacName().isBlank()
                      ? "IUPAC: " + identity.iupacName()
                      : !identity.commonName().isBlank()
                          ? "Название: " + identity.commonName()
                          : !identity.synonyms().isEmpty()
                              ? "Записанное имя: " + identity.synonyms().getFirst()
                              : "Идентификаторы и источник сохранены в карточке вещества.";
        }
      } catch (final RuntimeException e) {
        identityText = "Карточка не прочитана: " + e.getMessage();
      }
    }
    this.identityHint.setText(
        identityText.length() > 180 ? identityText.substring(0, 177) + "…" : identityText);
    this.identityHint.setTooltip(new javafx.scene.control.Tooltip(identityText));
    this.molecularCharge.setText(
        component == null ? "0" : Integer.toString(component.molecule().charge().value()));
    this.multiplicity.setText(
        component == null
            ? "1"
            : Integer.toString(component.molecule().spinMultiplicity().value()));
    final boolean fragment =
        component != null
            && "partial"
                .equals(component.molecule().metadata().attributes().get("laboratory.fragment"));
    this.fragmentHint.setText(
        fragment
            ? "Частичный фрагмент. Стереодескрипторы и свойства целой исходной молекулы не перенесены; заряд и спин задаются для новой структуры."
            : "");
    this.fragmentHint.setVisible(fragment);
    this.fragmentHint.setManaged(fragment);
    final String review =
        component == null
            ? null
            : component
                .molecule()
                .metadata()
                .attributes()
                .get(
                    ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry
                        .REVIEW_ATTRIBUTE);
    this.stereoHint.setText(
        component == null
            ? ""
            : "Стереозаписей: "
                + component.molecule().stereochemistry().centers().size()
                + (review == null ? " · Правка → Стереохимия" : " · Требуют проверки. " + review));
    this.role.setValue(component == null ? ReactionComponentRole.REACTANT : component.role());
    this.element.setText(atom == null ? "C" : atom.element().symbol().value());
    this.charge.setText(atom == null ? "0" : Integer.toString(atom.formalCharge().value()));
    this.isotope.setText(
        atom != null && atom.hasIsotope() ? Integer.toString(atom.isotope().massNumber()) : "");
    this.radical.setText(
        atom == null ? "0" : Integer.toString(atom.radicalState().unpairedElectrons()));
    this.coordinates.load(atom == null ? null : atom.coordinate());
    this.quantities.load(component);
    this.setDisable(component == null);
    this.displayed = this.values();
  }

  LaboratoryQuantityFields quantities() {
    return this.quantities;
  }

  boolean pending() {
    return !this.isDisabled() && !this.displayed.equals(this.values());
  }

  String element() {
    return this.element.getText();
  }

  BondType bond() {
    return this.bond.getValue();
  }

  ReactionComponentRole role() {
    return this.role.getValue();
  }

  MoleculeDrawing apply(
      final MoleculeSketchService service,
      final MoleculeDrawing drawing,
      final LaboratoryCanvas.Hit selection) {
    final MoleculeDrawing renamed =
        service.molecularState(
            service.rename(drawing, this.name.getText()),
            Integer.parseInt(this.molecularCharge.getText().trim()),
            Integer.parseInt(this.multiplicity.getText().trim()));
    final var coordinate = this.coordinates.coordinate();
    final var radical =
        ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState.of(
            Integer.parseInt(this.radical.getText().trim()));
    final var changed =
        service.changeAtom(
            renamed,
            selection.atomId(),
            this.element.getText(),
            Integer.parseInt(this.charge.getText().trim()),
            this.isotope.getText().isBlank()
                ? null
                : Integer.valueOf(this.isotope.getText().trim()));
    return service.spatialAtom(changed, selection.atomId(), coordinate, radical);
  }

  static String roleText(final ReactionComponentRole role) {
    if (role == null) {
      return "";
    }
    return switch (role) {
      case REACTANT -> "Исходник";
      case PRODUCT -> "Продукт";
      case SOLVENT -> "Растворитель";
      case CATALYST -> "Катализатор";
      case ADDITIVE -> "Добавка";
      case ACID -> "Кислота";
      case BASE -> "Основание";
      case LIGAND -> "Лиганд";
      case ATMOSPHERE -> "Атмосфера";
      case WORKUP -> "Обработка";
      case OTHER -> "Другое";
    };
  }

  private List<String> values() {
    return List.of(
        this.name.getText(),
        this.element.getText(),
        this.charge.getText(),
        this.isotope.getText(),
        String.valueOf(this.role.getValue()),
        this.quantities.values().toString(),
        this.radical.getText(),
        this.coordinates.values().toString(),
        this.molecularCharge.getText(),
        this.multiplicity.getText());
  }

  void draft(final java.util.Properties data, final boolean restore) {
    LaboratoryFormDraft.fields(
        data,
        "inspector",
        restore,
        java.util.Map.of(
            "name",
            this.name,
            "role",
            this.role,
            "element",
            this.element,
            "charge",
            this.charge,
            "isotope",
            this.isotope,
            "radical",
            this.radical,
            "bond",
            this.bond,
            "molecularCharge",
            this.molecularCharge,
            "multiplicity",
            this.multiplicity));
    this.coordinates.draft(data, restore);
    this.quantities.draft(data, restore);
  }
}
