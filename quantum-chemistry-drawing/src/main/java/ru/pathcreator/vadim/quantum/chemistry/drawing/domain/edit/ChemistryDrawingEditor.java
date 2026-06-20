/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.edit;

import java.util.ArrayList;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingRectangle2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingDocument;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCorePanel;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingFeatureCoverage;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingSelection;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingSelectionItem;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingSelectionKind;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ManualDrawingField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ReactionDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.ChemistryDrawingValidator;

/**
 * Mutable editor session с history, selection и immutable snapshots для UI.
 */
public final class ChemistryDrawingEditor {

  private final ChemistryDrawingValidator validator;
  private final ArrayList<ChemistryDrawingDocument> undoStack;
  private final ArrayList<ChemistryDrawingDocument> redoStack;
  private ChemistryDrawingDocument document;

  private ChemistryDrawingEditor(
      final ChemistryDrawingDocument document,
      final ChemistryDrawingValidator validator
  ) {
    this.document = document;
    this.validator = validator;
    this.undoStack = new ArrayList<ChemistryDrawingDocument>();
    this.redoStack = new ArrayList<ChemistryDrawingDocument>();
  }

  public static ChemistryDrawingEditor open(final ChemistryDrawingDocument document) {
    return new ChemistryDrawingEditor(
        document,
        new ChemistryDrawingValidator()
    );
  }

  public DrawingEditResult addMolecule(final MoleculeDrawing drawing) {
    final ArrayList<MoleculeDrawing> molecules =
        new ArrayList<MoleculeDrawing>(this.document.molecules());
    molecules.add(drawing);
    return this.replaceDocument(
        ChemistryDrawingDocument.of(
            this.document.id(),
            this.document.title(),
            molecules,
            this.document.reactions(),
            this.document.corePanels(),
            this.document.globalManualFields(),
            this.document.selection()
        )
    );
  }

  public DrawingEditResult addReaction(final ReactionDrawing drawing) {
    final ArrayList<ReactionDrawing> reactions =
        new ArrayList<ReactionDrawing>(this.document.reactions());
    reactions.add(drawing);
    return this.replaceDocument(
        ChemistryDrawingDocument.of(
            this.document.id(),
            this.document.title(),
            this.document.molecules(),
            reactions,
            this.document.corePanels(),
            this.document.globalManualFields(),
            this.document.selection()
        )
    );
  }

  public DrawingEditResult addCorePanel(final DrawingCorePanel panel) {
    if (panel == null) {
      throw new IllegalArgumentException("Drawing core panel must not be null.");
    }
    final ArrayList<DrawingCorePanel> panels =
        new ArrayList<DrawingCorePanel>(this.document.corePanels());
    panels.add(panel);
    return this.replaceDocument(
        ChemistryDrawingDocument.of(
            this.document.id(),
            this.document.title(),
            this.document.molecules(),
            this.document.reactions(),
            panels,
            this.document.globalManualFields(),
            this.document.selection()
        )
    );
  }

  public DrawingEditResult addManualField(final ManualDrawingField field) {
    final ArrayList<ManualDrawingField> fields =
        new ArrayList<ManualDrawingField>(this.document.globalManualFields());
    fields.add(field);
    return this.replaceDocument(
        ChemistryDrawingDocument.of(
            this.document.id(),
            this.document.title(),
            this.document.molecules(),
            this.document.reactions(),
            this.document.corePanels(),
            fields,
            this.document.selection()
        )
    );
  }

  public DrawingEditResult selectAtoms(final DrawingRectangle2D rectangle) {
    if (rectangle == null) {
      throw new IllegalArgumentException("Selection rectangle must not be null.");
    }
    final ArrayList<DrawingSelectionItem> items = new ArrayList<DrawingSelectionItem>();
    for (int i = 0; i < this.document.molecules().size(); ++i) {
      final MoleculeDrawing molecule = this.document.molecules().get(i);
      for (int j = 0; j < molecule.atoms().size(); ++j) {
        final AtomDrawing atom = molecule.atoms().get(j);
        if (!rectangle.contains(atom.point2D())) {
          continue;
        }
        items.add(
            DrawingSelectionItem.of(
                DrawingSelectionKind.ATOM,
                molecule.moleculeId().value() + ":" + atom.atomId().value()
            )
        );
      }
    }
    return this.replaceDocument(
        ChemistryDrawingDocument.of(
            this.document.id(),
            this.document.title(),
            this.document.molecules(),
            this.document.reactions(),
            this.document.corePanels(),
            this.document.globalManualFields(),
            DrawingSelection.of(items)
        )
    );
  }

  public DrawingEditResult clearSelection() {
    return this.replaceDocument(
        ChemistryDrawingDocument.of(
            this.document.id(),
            this.document.title(),
            this.document.molecules(),
            this.document.reactions(),
            this.document.corePanels(),
            this.document.globalManualFields(),
            DrawingSelection.EMPTY
        )
    );
  }

  public boolean canUndo() {
    return !this.undoStack.isEmpty();
  }

  public boolean canRedo() {
    return !this.redoStack.isEmpty();
  }

  public ChemistryDrawingDocument undo() {
    if (this.undoStack.isEmpty()) {
      return this.document;
    }
    this.redoStack.add(this.document);
    this.document = this.undoStack.remove(this.undoStack.size() - 1);
    return this.document;
  }

  public ChemistryDrawingDocument redo() {
    if (this.redoStack.isEmpty()) {
      return this.document;
    }
    this.undoStack.add(this.document);
    this.document = this.redoStack.remove(this.redoStack.size() - 1);
    return this.document;
  }

  public ChemistryDrawingDocument document() {
    return this.document;
  }

  private DrawingEditResult replaceDocument(final ChemistryDrawingDocument nextDocument) {
    this.undoStack.add(this.document);
    this.redoStack.clear();
    this.document = nextDocument;
    return DrawingEditResult.of(
        this.document,
        this.validator.validate(
            this.document,
            DrawingFeatureCoverage.full()
        )
    );
  }
}