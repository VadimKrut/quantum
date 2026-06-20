/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingDocument;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCorePanel;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingStructuredField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ManualDrawingField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ReactionDrawing;

/**
 * Строит примитивы отрисовки из drawing-документа без зависимости от конкретного UI.
 */
public final class ChemistryDrawingRenderer {

  public DrawingRenderPlan render(final ChemistryDrawingDocument document) {
    if (document == null) {
      return DrawingRenderPlan.of(List.of());
    }
    final ArrayList<RenderPrimitive> primitives = new ArrayList<RenderPrimitive>();
    for (int i = 0; i < document.molecules().size(); ++i) {
      this.renderMolecule(
          document.molecules().get(i),
          primitives
      );
    }
    for (int i = 0; i < document.reactions().size(); ++i) {
      this.renderReaction(
          document.reactions().get(i),
          primitives
      );
    }
    this.renderCorePanels(
        document.corePanels(),
        primitives
    );
    this.renderManualFields(
        "document",
        document.globalManualFields(),
        primitives
    );
    return DrawingRenderPlan.of(primitives);
  }

  private void renderMolecule(
      final MoleculeDrawing drawing,
      final List<RenderPrimitive> primitives
  ) {
    for (int i = 0; i < drawing.bonds().size(); ++i) {
      final BondDrawing bond = drawing.bonds().get(i);
      primitives.add(
          RenderPrimitive.of(
              this.primitiveKind(bond.style()),
              drawing.moleculeId().value() + ":bond:" + i,
              this.findAtomDrawing(drawing, bond.firstAtomId().value()).point2D(),
              this.findAtomDrawing(drawing, bond.secondAtomId().value()).point2D(),
              bond.style().name()
          )
      );
    }
    for (int i = 0; i < drawing.atoms().size(); ++i) {
      final AtomDrawing atomDrawing = drawing.atoms().get(i);
      primitives.add(
          RenderPrimitive.of(
              RenderPrimitiveKind.ATOM_LABEL,
              drawing.moleculeId().value() + ":atom:" + atomDrawing.atomId().value(),
              atomDrawing.point2D(),
              null,
              this.atomLabel(
                  drawing,
                  atomDrawing
              )
          )
      );
    }
    this.renderManualFields(
        drawing.moleculeId().value(),
        drawing.manualFields(),
        primitives
    );
  }

  private RenderPrimitiveKind primitiveKind(final BondDrawingStyle style) {
    if (
        style == BondDrawingStyle.WEDGE_UP
        || style == BondDrawingStyle.WEDGE_DOWN
    ) {
      return RenderPrimitiveKind.BOND_WEDGE;
    }
    if (
        style == BondDrawingStyle.DASHED
        || style == BondDrawingStyle.WAVY
    ) {
      return RenderPrimitiveKind.BOND_DASH;
    }
    return RenderPrimitiveKind.BOND_LINE;
  }

  private AtomDrawing findAtomDrawing(
      final MoleculeDrawing drawing,
      final String atomId
  ) {
    for (int i = 0; i < drawing.atoms().size(); ++i) {
      final AtomDrawing atomDrawing = drawing.atoms().get(i);
      if (atomDrawing.atomId().value().equals(atomId)) {
        return atomDrawing;
      }
    }
    throw new IllegalStateException("Atom drawing is missing for render plan.");
  }

  private String atomLabel(
      final MoleculeDrawing drawing,
      final AtomDrawing atomDrawing
  ) {
    final List<Atom> atoms = drawing.molecule().atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomDrawing.atomId())) {
        return atom.element().symbol().value();
      }
    }
    return atomDrawing.atomId().value();
  }

  private void renderReaction(
      final ReactionDrawing drawing,
      final List<RenderPrimitive> primitives
  ) {
    primitives.add(
        RenderPrimitive.of(
            RenderPrimitiveKind.REACTION_ARROW,
            drawing.reactionId().value() + ":arrow",
            drawing.arrowStart(),
            drawing.arrowEnd(),
            drawing.arrowKind().name()
        )
    );
    primitives.add(
        RenderPrimitive.of(
            RenderPrimitiveKind.REACTION_CONDITION_LABEL,
            drawing.reactionId().value() + ":conditions",
            drawing.arrowStart().translate(
                1.0,
                -1.0
            ),
            null,
            drawing.reaction().conditions().empty()
                ? ""
                : drawing.reaction().conditions().note()
        )
    );
    this.renderManualFields(
        drawing.reactionId().value(),
        drawing.manualFields(),
        primitives
    );
  }

  private void renderCorePanels(
      final List<DrawingCorePanel> panels,
      final List<RenderPrimitive> primitives
  ) {
    for (int i = 0; i < panels.size(); ++i) {
      final DrawingCorePanel panel = panels.get(i);
      primitives.add(
          RenderPrimitive.of(
              RenderPrimitiveKind.CORE_PANEL,
              panel.area().name() + ":" + panel.owner().id(),
              DrawingPoint2D.of(
                  0.0,
                  4.0 + i
              ),
              null,
              this.panelText(panel)
          )
      );
    }
  }

  private String panelText(final DrawingCorePanel panel) {
    final StringBuilder builder = new StringBuilder();
    builder.append(panel.title());
    builder.append(" [");
    builder.append(panel.area().name());
    builder.append("]");
    for (int i = 0; i < panel.fields().size(); ++i) {
      final DrawingStructuredField field = panel.fields().get(i);
      builder.append('\n');
      builder.append(field.label());
      builder.append(": ");
      builder.append(field.value());
      if (!field.unit().isEmpty()) {
        builder.append(' ');
        builder.append(field.unit());
      }
    }
    return builder.toString();
  }

  private void renderManualFields(
      final String ownerId,
      final List<ManualDrawingField> fields,
      final List<RenderPrimitive> primitives
  ) {
    for (int i = 0; i < fields.size(); ++i) {
      final ManualDrawingField field = fields.get(i);
      primitives.add(
          RenderPrimitive.of(
              RenderPrimitiveKind.MANUAL_FIELD_PANEL,
              ownerId + ":manual:" + i,
              DrawingPoint2D.of(
                  0.0,
                  2.0 + i
              ),
              null,
              field.feature().name() + " " + field.key() + "=" + field.value()
          )
      );
    }
  }
}