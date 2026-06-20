/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.AtomDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingDocument;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCoreArea;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCorePanel;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCoreReferenceKind;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingFeatureCoverage;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ManualDrawingField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ReactionDrawing;

/**
 * Проверяет, что drawing-документ синхронизирован с chemistry-core объектами и покрывает все области ядра.
 */
public final class ChemistryDrawingValidator {

  public DrawingValidationResult validate(
      final ChemistryDrawingDocument document,
      final DrawingFeatureCoverage coverage
  ) {
    final ArrayList<DrawingDiagnostic> diagnostics = new ArrayList<DrawingDiagnostic>();
    if (document == null) {
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.ERROR,
              DrawingDiagnosticCode.EMPTY_DOCUMENT,
              "",
              "Drawing document must not be null."
          )
      );
      return DrawingValidationResult.of(diagnostics);
    }
    if (
        document.molecules().isEmpty()
        && document.reactions().isEmpty()
        && document.corePanels().isEmpty()
        && document.globalManualFields().isEmpty()
    ) {
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.WARNING,
              DrawingDiagnosticCode.EMPTY_DOCUMENT,
              document.id(),
              "Drawing document does not contain chemistry objects."
          )
      );
    }
    this.validateFeatureCoverage(
        coverage,
        diagnostics
    );
    this.validateMolecules(
        document,
        diagnostics
    );
    this.validateReactions(
        document,
        diagnostics
    );
    this.validateCorePanels(
        document,
        diagnostics
    );
    this.validateManualFields(
        document.id(),
        document.globalManualFields(),
        diagnostics
    );
    return DrawingValidationResult.of(diagnostics);
  }

  private void validateFeatureCoverage(
      final DrawingFeatureCoverage coverage,
      final List<DrawingDiagnostic> diagnostics
  ) {
    if (
        coverage != null
        && coverage.completeForCurrentCore()
    ) {
      return;
    }
    diagnostics.add(
        DrawingDiagnostic.of(
            DrawingDiagnosticSeverity.ERROR,
            DrawingDiagnosticCode.FEATURE_COVERAGE_INCOMPLETE,
            "coverage",
            "Drawing feature coverage must include every current chemistry-core area."
        )
    );
  }

  private void validateMolecules(
      final ChemistryDrawingDocument document,
      final List<DrawingDiagnostic> diagnostics
  ) {
    final HashSet<String> moleculeIds = new HashSet<String>();
    for (int i = 0; i < document.molecules().size(); ++i) {
      final MoleculeDrawing drawing = document.molecules().get(i);
      if (!moleculeIds.add(drawing.moleculeId().value())) {
        diagnostics.add(
            DrawingDiagnostic.of(
                DrawingDiagnosticSeverity.ERROR,
                DrawingDiagnosticCode.DUPLICATE_MOLECULE_ID,
                drawing.moleculeId().value(),
                "Drawing document contains duplicate molecule drawing id."
            )
        );
      }
      this.validateAtomDrawings(
          drawing,
          diagnostics
      );
      this.validateBondDrawings(
          drawing,
          diagnostics
      );
      this.validateManualFields(
          drawing.moleculeId().value(),
          drawing.manualFields(),
          diagnostics
      );
    }
  }

  private void validateAtomDrawings(
      final MoleculeDrawing drawing,
      final List<DrawingDiagnostic> diagnostics
  ) {
    for (int i = 0; i < drawing.atoms().size(); ++i) {
      final AtomDrawing atomDrawing = drawing.atoms().get(i);
      if (this.hasAtom(drawing, atomDrawing)) {
        continue;
      }
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.ERROR,
              DrawingDiagnosticCode.ATOM_DRAWING_WITHOUT_CORE_ATOM,
              atomDrawing.atomId().value(),
              "Atom drawing references an atom absent from core molecule."
          )
      );
    }
  }

  private boolean hasAtom(
      final MoleculeDrawing drawing,
      final AtomDrawing atomDrawing
  ) {
    final List<Atom> atoms = drawing.molecule().atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (atoms.get(i).id().equals(atomDrawing.atomId())) {
        return true;
      }
    }
    return false;
  }

  private void validateBondDrawings(
      final MoleculeDrawing drawing,
      final List<DrawingDiagnostic> diagnostics
  ) {
    for (int i = 0; i < drawing.bonds().size(); ++i) {
      final BondDrawing bondDrawing = drawing.bonds().get(i);
      if (this.hasBond(drawing, bondDrawing)) {
        continue;
      }
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.ERROR,
              DrawingDiagnosticCode.BOND_DRAWING_WITHOUT_CORE_BOND,
              drawing.moleculeId().value(),
              "Bond drawing references a bond absent from core molecule."
          )
      );
    }
  }

  private boolean hasBond(
      final MoleculeDrawing drawing,
      final BondDrawing bondDrawing
  ) {
    final List<Bond> bonds = drawing.molecule().bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      if (bonds.get(i).connects(bondDrawing.firstAtomId(), bondDrawing.secondAtomId())) {
        return true;
      }
    }
    return false;
  }

  private void validateReactions(
      final ChemistryDrawingDocument document,
      final List<DrawingDiagnostic> diagnostics
  ) {
    final HashSet<String> reactionIds = new HashSet<String>();
    for (int i = 0; i < document.reactions().size(); ++i) {
      final ReactionDrawing drawing = document.reactions().get(i);
      if (!reactionIds.add(drawing.reactionId().value())) {
        diagnostics.add(
            DrawingDiagnostic.of(
                DrawingDiagnosticSeverity.ERROR,
                DrawingDiagnosticCode.DUPLICATE_REACTION_ID,
                drawing.reactionId().value(),
                "Drawing document contains duplicate reaction drawing id."
            )
        );
      }
      this.validateReactionParticipants(
          document,
          drawing,
          diagnostics
      );
      this.validateManualFields(
          drawing.reactionId().value(),
          drawing.manualFields(),
          diagnostics
      );
    }
  }

  private void validateReactionParticipants(
      final ChemistryDrawingDocument document,
      final ReactionDrawing drawing,
      final List<DrawingDiagnostic> diagnostics
  ) {
    this.validateReactionSideParticipants(
        document,
        drawing.reaction().reactants().participants(),
        drawing.reactionId().value(),
        diagnostics
    );
    this.validateReactionSideParticipants(
        document,
        drawing.reaction().products().participants(),
        drawing.reactionId().value(),
        diagnostics
    );
  }

  private void validateReactionSideParticipants(
      final ChemistryDrawingDocument document,
      final List<ReactionParticipant> participants,
      final String reactionId,
      final List<DrawingDiagnostic> diagnostics
  ) {
    for (int i = 0; i < participants.size(); ++i) {
      final MoleculeId moleculeId = participants.get(i).molecule().id();
      if (this.hasMoleculeDrawing(document, moleculeId)) {
        continue;
      }
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.WARNING,
              DrawingDiagnosticCode.REACTION_WITHOUT_DRAWN_PARTICIPANT,
              reactionId,
              "Reaction references a molecule that is not drawn in this document."
          )
      );
    }
  }

  private boolean hasMoleculeDrawing(
      final ChemistryDrawingDocument document,
      final MoleculeId moleculeId
  ) {
    for (int i = 0; i < document.molecules().size(); ++i) {
      if (document.molecules().get(i).moleculeId().equals(moleculeId)) {
        return true;
      }
    }
    return false;
  }

  private void validateCorePanels(
      final ChemistryDrawingDocument document,
      final List<DrawingDiagnostic> diagnostics
  ) {
    final HashSet<String> panelKeys = new HashSet<String>();
    final EnumSet<DrawingCoreArea> coveredAreas = EnumSet.noneOf(DrawingCoreArea.class);
    for (int i = 0; i < document.corePanels().size(); ++i) {
      final DrawingCorePanel panel = document.corePanels().get(i);
      coveredAreas.add(panel.area());
      final String key = panel.area().name()
          + '\u0000'
          + panel.owner().kind().name()
          + '\u0000'
          + panel.owner().id();
      if (!panelKeys.add(key)) {
        diagnostics.add(
            DrawingDiagnostic.of(
                DrawingDiagnosticSeverity.ERROR,
                DrawingDiagnosticCode.CORE_PANEL_DUPLICATE,
                panel.owner().id(),
                "Drawing core panel is duplicated for the same owner and area."
            )
        );
      }
      if (panel.hasMissingRequiredField()) {
        diagnostics.add(
            DrawingDiagnostic.of(
                DrawingDiagnosticSeverity.ERROR,
                DrawingDiagnosticCode.CORE_PANEL_REQUIRED_FIELD_MISSING,
                panel.owner().id(),
                "Drawing core panel has an empty required field."
            )
        );
      }
      this.validatePanelOwner(
          document,
          panel,
          diagnostics
      );
    }
    this.validateCoreAreaCoverage(
        coveredAreas,
        diagnostics
    );
  }

  private void validatePanelOwner(
      final ChemistryDrawingDocument document,
      final DrawingCorePanel panel,
      final List<DrawingDiagnostic> diagnostics
  ) {
    if (panel.owner().kind() == DrawingCoreReferenceKind.DOCUMENT) {
      return;
    }
    if (
        panel.owner().kind() == DrawingCoreReferenceKind.MOLECULE
        && this.hasMoleculeDrawingById(
            document,
            panel.owner().id()
        )
    ) {
      return;
    }
    if (
        panel.owner().kind() == DrawingCoreReferenceKind.REACTION
        && this.hasReactionDrawingById(
            document,
            panel.owner().id()
        )
    ) {
      return;
    }
    diagnostics.add(
        DrawingDiagnostic.of(
            DrawingDiagnosticSeverity.WARNING,
            DrawingDiagnosticCode.CORE_PANEL_OWNER_MISSING,
            panel.owner().id(),
            "Drawing core panel owner is not drawn in this document."
        )
    );
  }

  private boolean hasMoleculeDrawingById(
      final ChemistryDrawingDocument document,
      final String moleculeId
  ) {
    for (int i = 0; i < document.molecules().size(); ++i) {
      if (document.molecules().get(i).moleculeId().value().equals(moleculeId)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasReactionDrawingById(
      final ChemistryDrawingDocument document,
      final String reactionId
  ) {
    for (int i = 0; i < document.reactions().size(); ++i) {
      if (document.reactions().get(i).reactionId().value().equals(reactionId)) {
        return true;
      }
    }
    return false;
  }

  private void validateCoreAreaCoverage(
      final EnumSet<DrawingCoreArea> coveredAreas,
      final List<DrawingDiagnostic> diagnostics
  ) {
    final DrawingCoreArea[] areas = DrawingCoreArea.values();
    for (int i = 0; i < areas.length; ++i) {
      if (coveredAreas.contains(areas[i])) {
        continue;
      }
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.WARNING,
              DrawingDiagnosticCode.CORE_AREA_PANEL_MISSING,
              areas[i].name(),
              "Drawing document does not contain a core panel for this chemistry-core area."
          )
      );
    }
  }

  private void validateManualFields(
      final String ownerId,
      final List<ManualDrawingField> fields,
      final List<DrawingDiagnostic> diagnostics
  ) {
    final HashSet<String> keys = new HashSet<String>();
    for (int i = 0; i < fields.size(); ++i) {
      final ManualDrawingField field = fields.get(i);
      final String key = field.feature().name() + '\u0000' + field.key();
      if (keys.add(key)) {
        continue;
      }
      diagnostics.add(
          DrawingDiagnostic.of(
              DrawingDiagnosticSeverity.ERROR,
              DrawingDiagnosticCode.MANUAL_FIELD_DUPLICATE,
              ownerId,
              "Manual drawing field is duplicated for the same feature."
          )
      );
    }
  }
}