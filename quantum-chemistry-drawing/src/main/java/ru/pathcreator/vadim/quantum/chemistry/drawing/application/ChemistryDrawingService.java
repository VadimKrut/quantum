/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.edit.ChemistryDrawingEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingDocument;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCorePanel;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingCorePanelFactory;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.DrawingFeatureCoverage;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render.ChemistryDrawingRenderer;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render.DrawingRenderPlan;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.ChemistryDrawingValidator;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.DrawingValidationResult;

/**
 * Фасад drawing-модуля для desktop и workflow: открыть редактор, проверить и построить render-plan.
 */
public final class ChemistryDrawingService {

  private final ChemistryDrawingValidator validator;
  private final ChemistryDrawingRenderer renderer;
  private final DrawingCorePanelFactory panelFactory;

  public ChemistryDrawingService() {
    this.validator = new ChemistryDrawingValidator();
    this.renderer = new ChemistryDrawingRenderer();
    this.panelFactory = new DrawingCorePanelFactory();
  }

  public ChemistryDrawingEditor openEditor(final ChemistryDrawingDocument document) {
    return ChemistryDrawingEditor.open(document);
  }

  public DrawingValidationResult validate(final ChemistryDrawingDocument document) {
    return this.validator.validate(
        document,
        DrawingFeatureCoverage.full()
    );
  }

  public DrawingRenderPlan render(final ChemistryDrawingDocument document) {
    return this.renderer.render(document);
  }

  public DrawingFeatureCoverage featureCoverage() {
    return DrawingFeatureCoverage.full();
  }

  public List<DrawingCorePanel> documentCorePanels(final String documentId) {
    return this.panelFactory.documentPanels(documentId);
  }

  public List<DrawingCorePanel> moleculeCorePanels(final Molecule molecule) {
    return this.panelFactory.moleculePanels(molecule);
  }

  public List<DrawingCorePanel> reactionCorePanels(final Reaction reaction) {
    return this.panelFactory.reactionPanels(reaction);
  }
}