/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.ChemistryDrawingService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.draft.MoleculeDraft;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.draft.MoleculeDraftAtom;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.draft.MoleculeDraftBond;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.edit.ChemistryDrawingEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.edit.DrawingEditResult;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint3D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingRectangle2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingDocument;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCoreArea;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCorePanel;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCoreReference;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCoreReferenceKind;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingFieldKind;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingFeature;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingFeatureCoverage;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ManualDrawingField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ReactionArrowKind;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ReactionDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingStructuredField;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render.DrawingRenderPlan;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.ChemistryDrawingValidator;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.DrawingDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.DrawingValidationResult;

final class ChemistryDrawingServiceTest {

  @Test
  void editorBuildsValid2D3DReactionDocument() {
    final ChemistryDrawingService service = new ChemistryDrawingService();
    final MoleculeDraft acetoneDraft = ChemistryDrawingServiceTest.acetoneDraft();
    final Molecule acetone = acetoneDraft.toMolecule(LengthUnit.ANGSTROM);
    final Reaction reaction = Reaction.of(
        ReactionId.of("reaction_acetone_probe"),
        "Acetone drawing probe",
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    acetone,
                    StoichiometricCoefficient.ONE
                )
            )
        ),
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    acetone,
                    StoichiometricCoefficient.ONE
                )
            )
        ),
        ReactionConditions.of(
            null,
            null,
            "acetone",
            "none",
            "manual solvent/reaction condition panel"
        ),
        null
    );
    final ChemistryDrawingEditor editor = service.openEditor(
        ChemistryDrawingDocument.empty(
            "drawing_acetone",
            "Acetone drawing"
        )
    );

    DrawingEditResult result = editor.addMolecule(acetoneDraft.toDrawing(LengthUnit.ANGSTROM));
    Assertions.assertTrue(result.validationResult().valid());

    final List<DrawingCorePanel> documentPanels = service.documentCorePanels("drawing_acetone");
    for (int i = 0; i < documentPanels.size(); ++i) {
      result = editor.addCorePanel(documentPanels.get(i));
    }
    final List<DrawingCorePanel> moleculePanels = service.moleculeCorePanels(acetone);
    for (int i = 0; i < moleculePanels.size(); ++i) {
      result = editor.addCorePanel(moleculePanels.get(i));
    }

    result = editor.addReaction(
        ReactionDrawing.of(
            reaction,
            ReactionArrowKind.EQUILIBRIUM,
            DrawingPoint2D.of(5.0, 0.0),
            DrawingPoint2D.of(9.0, 0.0),
            List.of(
                ManualDrawingField.of(
                    ChemistryDrawingFeature.REACTION_CONDITIONS,
                    "solvent",
                    "acetone"
                )
            )
        )
    );
    Assertions.assertTrue(result.validationResult().valid());

    final List<DrawingCorePanel> reactionPanels = service.reactionCorePanels(reaction);
    for (int i = 0; i < reactionPanels.size(); ++i) {
      result = editor.addCorePanel(reactionPanels.get(i));
    }
    Assertions.assertTrue(result.validationResult().valid());

    result = editor.addManualField(
        ManualDrawingField.of(
            ChemistryDrawingFeature.ELECTRONIC_PROBLEM,
            "basis",
            "sto-3g"
        )
    );
    Assertions.assertTrue(result.validationResult().valid());

    result = editor.selectAtoms(
        DrawingRectangle2D.fromCorners(
            DrawingPoint2D.of(-2.0, -2.0),
            DrawingPoint2D.of(2.0, 2.0)
        )
    );
    Assertions.assertFalse(result.document().selection().empty());

    final DrawingRenderPlan plan = service.render(result.document());
    Assertions.assertTrue(plan.primitiveCount() >= 30);
    Assertions.assertTrue(service.featureCoverage().completeForCurrentCore());
    Assertions.assertTrue(editor.canUndo());
    editor.undo();
    Assertions.assertTrue(editor.canRedo());
    editor.redo();
  }

  @Test
  void validatorRejectsIncompleteCoverageAndDuplicateManualFields() {
    final ChemistryDrawingValidator validator = new ChemistryDrawingValidator();
    final ChemistryDrawingDocument document = ChemistryDrawingDocument.of(
        "manual_probe",
        "Manual probe",
        List.of(),
        List.of(),
        List.of(
            DrawingCorePanel.of(
                DrawingCoreArea.ELECTRONIC_PROBLEM,
                DrawingCoreReference.of(
                    DrawingCoreReferenceKind.DOCUMENT,
                    "manual_probe"
                ),
                "Electronic problem",
                List.of(
                    DrawingStructuredField.of(
                        "basis",
                        "Basis",
                        DrawingFieldKind.TEXT,
                        "",
                        "",
                        true,
                        true,
                        List.of()
                    )
                )
            )
        ),
        List.of(
            ManualDrawingField.of(
                ChemistryDrawingFeature.SPECTROSCOPY,
                "nmr",
                "required"
            ),
            ManualDrawingField.of(
                ChemistryDrawingFeature.SPECTROSCOPY,
                "nmr",
                "required"
            )
        ),
        null
    );

    final DrawingValidationResult result = validator.validate(
        document,
        null
    );

    Assertions.assertFalse(result.valid());
    Assertions.assertTrue(
        ChemistryDrawingServiceTest.containsCode(
            result,
            DrawingDiagnosticCode.FEATURE_COVERAGE_INCOMPLETE
        )
    );
    Assertions.assertTrue(
        ChemistryDrawingServiceTest.containsCode(
            result,
            DrawingDiagnosticCode.CORE_PANEL_REQUIRED_FIELD_MISSING
        )
    );
    Assertions.assertTrue(
        ChemistryDrawingServiceTest.containsCode(
            result,
            DrawingDiagnosticCode.MANUAL_FIELD_DUPLICATE
        )
    );
  }

  private static boolean containsCode(
      final DrawingValidationResult result,
      final DrawingDiagnosticCode code
  ) {
    for (int i = 0; i < result.diagnostics().size(); ++i) {
      if (result.diagnostics().get(i).code() == code) {
        return true;
      }
    }
    return false;
  }

  private static MoleculeDraft acetoneDraft() {
    final MoleculeDraft draft = MoleculeDraft.create(
        "acetone",
        "Acetone"
    );
    draft.addAtom(
        MoleculeDraftAtom.of(
            "c1",
            "C",
            DrawingPoint2D.of(-1.5, 0.0),
            DrawingPoint3D.of(-1.5, 0.0, 0.0)
        )
    );
    draft.addAtom(
        MoleculeDraftAtom.of(
            "c2",
            "C",
            DrawingPoint2D.of(0.0, 0.0),
            DrawingPoint3D.of(0.0, 0.0, 0.0)
        )
    );
    draft.addAtom(
        MoleculeDraftAtom.of(
            "c3",
            "C",
            DrawingPoint2D.of(1.5, 0.0),
            DrawingPoint3D.of(1.5, 0.0, 0.0)
        )
    );
    draft.addAtom(
        MoleculeDraftAtom.of(
            "o1",
            "O",
            DrawingPoint2D.of(0.0, 1.2),
            DrawingPoint3D.of(0.0, 1.2, 0.2)
        )
    );
    draft.addBond(
        MoleculeDraftBond.of(
            "c1",
            "c2",
            BondType.SINGLE,
            BondDrawingStyle.PLAIN
        )
    );
    draft.addBond(
        MoleculeDraftBond.of(
            "c2",
            "c3",
            BondType.SINGLE,
            BondDrawingStyle.PLAIN
        )
    );
    draft.addBond(
        MoleculeDraftBond.of(
            "c2",
            "o1",
            BondType.DOUBLE,
            BondDrawingStyle.WEDGE_UP
        )
    );
    draft.addManualField(
        ManualDrawingField.of(
            ChemistryDrawingFeature.THERMODYNAMICS,
            "enthalpy-note",
            "user form value"
        )
    );
    draft.moveAtom2D(
        AtomId.of("o1"),
        0.0,
        0.1
    );
    draft.rotate3D(
        0.1,
        0.2,
        0.0
    );
    return draft;
  }
}