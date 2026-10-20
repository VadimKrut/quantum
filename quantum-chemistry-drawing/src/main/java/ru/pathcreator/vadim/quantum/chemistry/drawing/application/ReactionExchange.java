/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Reviewable external reaction; import never creates experimental quantities or outcomes. */
public interface ReactionExchange {
  enum Format {
    AUTO("Определить"),
    RXN_V2000("RXN · V2000"),
    RXN_V3000("RXN · V3000"),
    SMILES("Реакционный SMILES");
    private final String title;

    Format(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return this.title;
    }
  }

  record Participant(
      StructureExchange.Entry structure,
      ReactionComponentRole role,
      Map<AtomId, Integer> atomNumbers) {
    public Participant {
      Objects.requireNonNull(structure);
      Objects.requireNonNull(role);
      atomNumbers = Map.copyOf(atomNumbers);
    }
  }

  record Entry(String title, List<Participant> participants, List<String> notes) {
    public Entry {
      if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Укажите название стадии.");
      }
      participants = List.copyOf(participants);
      notes = List.copyOf(notes);
      if (participants.isEmpty() || participants.size() > 100) {
        throw new IllegalArgumentException("Реакция содержит 1–100 участников.");
      }
    }
  }

  record Export(String text, List<String> warnings) {
    public Export {
      warnings = List.copyOf(warnings);
    }
  }

  Entry read(String text, Format format, String source);

  Export write(LaboratoryDrawing drawing, String stageId, Format format, boolean includeMapping);
}
