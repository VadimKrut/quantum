/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * UI-независимый документ химического редактора: молекулы, реакции, панели ядра и выборка.
 */
public final class ChemistryDrawingDocument {

  private final String id;
  private final String title;
  private final List<MoleculeDrawing> molecules;
  private final List<ReactionDrawing> reactions;
  private final List<DrawingCorePanel> corePanels;
  private final List<ManualDrawingField> globalManualFields;
  private final DrawingSelection selection;

  private ChemistryDrawingDocument(
      final String id,
      final String title,
      final List<MoleculeDrawing> molecules,
      final List<ReactionDrawing> reactions,
      final List<DrawingCorePanel> corePanels,
      final List<ManualDrawingField> globalManualFields,
      final DrawingSelection selection
  ) {
    this.id = id;
    this.title = title;
    this.molecules = molecules;
    this.reactions = reactions;
    this.corePanels = corePanels;
    this.globalManualFields = globalManualFields;
    this.selection = selection;
  }

  public static ChemistryDrawingDocument of(
      final String id,
      final String title,
      final List<MoleculeDrawing> molecules,
      final List<ReactionDrawing> reactions,
      final List<DrawingCorePanel> corePanels,
      final List<ManualDrawingField> globalManualFields,
      final DrawingSelection selection
  ) {
    return new ChemistryDrawingDocument(
        DrawingText.require(
            id,
            "Drawing document id"
        ),
        DrawingText.require(
            title,
            "Drawing document title"
        ),
        molecules == null ? List.of() : List.copyOf(molecules),
        reactions == null ? List.of() : List.copyOf(reactions),
        corePanels == null ? List.of() : List.copyOf(corePanels),
        globalManualFields == null ? List.of() : List.copyOf(globalManualFields),
        selection == null ? DrawingSelection.EMPTY : selection
    );
  }

  public static ChemistryDrawingDocument of(
      final String id,
      final String title,
      final List<MoleculeDrawing> molecules,
      final List<ReactionDrawing> reactions,
      final List<ManualDrawingField> globalManualFields,
      final DrawingSelection selection
  ) {
    return ChemistryDrawingDocument.of(
        id,
        title,
        molecules,
        reactions,
        List.of(),
        globalManualFields,
        selection
    );
  }

  public static ChemistryDrawingDocument empty(
      final String id,
      final String title
  ) {
    return ChemistryDrawingDocument.of(
        id,
        title,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        DrawingSelection.EMPTY
    );
  }

  public String id() {
    return this.id;
  }

  public String title() {
    return this.title;
  }

  public List<MoleculeDrawing> molecules() {
    return this.molecules;
  }

  public List<ReactionDrawing> reactions() {
    return this.reactions;
  }

  public List<DrawingCorePanel> corePanels() {
    return this.corePanels;
  }

  public List<ManualDrawingField> globalManualFields() {
    return this.globalManualFields;
  }

  public DrawingSelection selection() {
    return this.selection;
  }

  public int moleculeCount() {
    return this.molecules.size();
  }

  public int reactionCount() {
    return this.reactions.size();
  }

  public int corePanelCount() {
    return this.corePanels.size();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryDrawingDocument)) {
      return false;
    }
    final ChemistryDrawingDocument document = (ChemistryDrawingDocument) other;
    return Objects.equals(this.id, document.id)
        && Objects.equals(this.title, document.title)
        && Objects.equals(this.molecules, document.molecules)
        && Objects.equals(this.reactions, document.reactions)
        && Objects.equals(this.corePanels, document.corePanels)
        && Objects.equals(this.globalManualFields, document.globalManualFields)
        && Objects.equals(this.selection, document.selection);
  }

  public int hashCode() {
    int result = this.id.hashCode();
    result = 31 * result + this.title.hashCode();
    result = 31 * result + this.molecules.hashCode();
    result = 31 * result + this.reactions.hashCode();
    result = 31 * result + this.corePanels.hashCode();
    result = 31 * result + this.globalManualFields.hashCode();
    result = 31 * result + this.selection.hashCode();
    return result;
  }
}