/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Создает типизированные панели для полного покрытия chemistry-core в редакторе.
 */
public final class DrawingCorePanelFactory {

  public List<DrawingCorePanel> documentPanels(final String documentId) {
    final DrawingCoreReference owner = DrawingCoreReference.of(
        DrawingCoreReferenceKind.DOCUMENT,
        documentId
    );
    final ArrayList<DrawingCorePanel> panels = new ArrayList<DrawingCorePanel>();
    final DrawingCoreArea[] areas = DrawingCoreArea.values();
    for (int i = 0; i < areas.length; ++i) {
      panels.add(
          this.panelForArea(
              areas[i],
              owner,
              areas[i].name(),
              List.of(
                  DrawingStructuredField.of(
                      "enabled",
                      "Enabled",
                      DrawingFieldKind.BOOLEAN,
                      "true",
                      "",
                      true,
                      true,
                      List.of()
                  )
              )
          )
      );
    }
    return List.copyOf(panels);
  }

  public List<DrawingCorePanel> moleculePanels(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecule must not be null.");
    }
    final DrawingCoreReference owner = DrawingCoreReference.of(
        DrawingCoreReferenceKind.MOLECULE,
        molecule.id().value()
    );
    final ArrayList<DrawingCorePanel> panels = new ArrayList<DrawingCorePanel>();
    panels.add(
        this.panelForArea(
            DrawingCoreArea.STRUCTURE,
            owner,
            "Molecule structure",
            List.of(
                DrawingStructuredField.of(
                    "atom_count",
                    "Atom count",
                    DrawingFieldKind.INTEGER,
                    Integer.toString(molecule.atomCount()),
                    "",
                    true,
                    false,
                    List.of()
                ),
                DrawingStructuredField.of(
                    "bond_count",
                    "Bond count",
                    DrawingFieldKind.INTEGER,
                    Integer.toString(molecule.bondCount()),
                    "",
                    true,
                    false,
                    List.of()
                )
            )
        )
    );
    panels.add(
        this.panelForArea(
            DrawingCoreArea.STEREO,
            owner,
            "Stereochemistry",
            List.of(
                DrawingStructuredField.of(
                    "stereocenters",
                    "Stereocenters",
                    DrawingFieldKind.LIST,
                    Integer.toString(molecule.stereochemistry().centers().size()),
                    "",
                    true,
                    true,
                    List.of()
                )
            )
        )
    );
    panels.add(
        this.panelForArea(
            DrawingCoreArea.CONFORMATION,
            owner,
            "Conformation",
            List.of(
                DrawingStructuredField.of(
                    "torsions",
                    "Torsion count",
                    DrawingFieldKind.INTEGER,
                    Integer.toString(molecule.conformation().torsionAngles().size()),
                    "",
                    true,
                    true,
                    List.of()
                )
            )
        )
    );
    panels.add(
        this.panelForArea(
            DrawingCoreArea.SYMMETRY,
            owner,
            "Symmetry",
            List.of(
                DrawingStructuredField.of(
                    "point_group",
                    "Point group",
                    DrawingFieldKind.TEXT,
                    molecule.symmetry().pointGroupName().value(),
                    "",
                    true,
                    true,
                    List.of()
                )
            )
        )
    );
    return List.copyOf(panels);
  }

  public List<DrawingCorePanel> reactionPanels(final Reaction reaction) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction must not be null.");
    }
    final DrawingCoreReference owner = DrawingCoreReference.of(
        DrawingCoreReferenceKind.REACTION,
        reaction.id().value()
    );
    return List.of(
        this.panelForArea(
            DrawingCoreArea.REACTION,
            owner,
            "Reaction",
            List.of(
                DrawingStructuredField.of(
                    "reactants",
                    "Reactants",
                    DrawingFieldKind.INTEGER,
                    Integer.toString(reaction.reactants().participantCount()),
                    "",
                    true,
                    false,
                    List.of()
                ),
                DrawingStructuredField.of(
                    "products",
                    "Products",
                    DrawingFieldKind.INTEGER,
                    Integer.toString(reaction.products().participantCount()),
                    "",
                    true,
                    false,
                    List.of()
                )
            )
        ),
        this.panelForArea(
            DrawingCoreArea.THERMODYNAMICS,
            owner,
            "Reaction thermodynamics",
            List.of(
                DrawingStructuredField.of(
                    "profile",
                    "Profile",
                    DrawingFieldKind.TABLE,
                    "not calculated",
                    "",
                    false,
                    true,
                    List.of()
                )
            )
        ),
        this.panelForArea(
            DrawingCoreArea.KINETICS,
            owner,
            "Reaction kinetics",
            List.of(
                DrawingStructuredField.of(
                    "rate_law",
                    "Rate law",
                    DrawingFieldKind.FREE_TEXT,
                    "not specified",
                    "",
                    false,
                    true,
                    List.of()
                )
            )
        )
    );
  }

  private DrawingCorePanel panelForArea(
      final DrawingCoreArea area,
      final DrawingCoreReference owner,
      final String title,
      final List<DrawingStructuredField> fields
  ) {
    return DrawingCorePanel.of(
        area,
        owner,
        title,
        fields
    );
  }
}