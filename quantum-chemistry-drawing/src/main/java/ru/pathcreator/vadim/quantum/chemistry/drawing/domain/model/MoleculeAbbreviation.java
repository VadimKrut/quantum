/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Typed display projection over explicit atoms; no chemical atom is replaced by the label. */
public record MoleculeAbbreviation(
    String id, String label, AtomId anchor, List<AtomId> atoms, String structureSignature) {
  public MoleculeAbbreviation {
    if (id == null || id.length() > 100) {
      throw new IllegalArgumentException("Некорректный идентификатор сокращённой группы.");
    }
    id = IdentifierValue.requireIdentifier(id, "Abbreviation id");
    label = Objects.requireNonNull(label).strip();
    if (!label.matches("[\\p{L}\\p{N}()+'−-]{1,24}")) {
      throw new IllegalArgumentException(
          "Подпись группы: 1–24 буквы, цифры, скобки или знаки +/−.");
    }
    Objects.requireNonNull(anchor);
    atoms = List.copyOf(atoms);
    if (atoms.isEmpty()
        || atoms.size() > 500
        || !atoms.contains(anchor)
        || new HashSet<>(atoms).size() != atoms.size()) {
      throw new IllegalArgumentException(
          "Группа должна содержать 1–500 разных атомов и точку присоединения.");
    }
    if (structureSignature == null || !structureSignature.matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("Нет подписи явного графа сокращённой группы.");
    }
  }
}
