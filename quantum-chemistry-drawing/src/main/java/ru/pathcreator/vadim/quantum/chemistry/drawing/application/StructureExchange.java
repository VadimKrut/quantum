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
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** External structure exchange returns reviewable values without editing the notebook. */
public interface StructureExchange {
  enum Format {
    AUTO,
    SMILES,
    MOL,
    SDF
  }

  record Entry(MoleculeDrawing drawing, Map<String, String> fields, List<String> notes) {
    public Entry {
      fields = Map.copyOf(fields);
      notes = List.copyOf(notes);
    }
  }

  List<Entry> read(String text, Format format, String source);

  String write(List<MoleculeDrawing> structures, Format format);
}
