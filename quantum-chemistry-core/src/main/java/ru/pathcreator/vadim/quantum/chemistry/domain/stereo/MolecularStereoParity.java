/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.stereo;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Validated ordered tetrahedral orientation. A permutation of neighbours changes parity by its sign. */
public final class MolecularStereoParity {
  public record Tetrahedron(AtomId center, List<AtomId> references, int sign) {
    public Tetrahedron {
      Objects.requireNonNull(center);
      references = List.copyOf(references);
      if (references.size() != 4 || new HashSet<>(references).size() != 4 || Math.abs(sign) != 1) {
        throw new IllegalArgumentException("Некорректная тетраэдрическая чётность.");
      }
    }
  }

  private MolecularStereoParity() {}

  public static Map<AtomId, Tetrahedron> read(final Molecule m) {
    final String raw = m.metadata().attributes().get(Stereochemistry.ORDERED_PARITY_ATTRIBUTE);
    if (raw == null || raw.isEmpty()) {
      return Map.of();
    }
    if (raw.length() > 1000000) {
      throw new IllegalArgumentException("Слишком большая запись чётности.");
    }
    final Map<AtomId, Tetrahedron> result = new HashMap<>();
    final Set<AtomId> atoms =
        m.atoms().stream().map(Atom::id).collect(java.util.stream.Collectors.toSet());
    for (final String record : raw.split(";", -1)) {
      final String[] p = record.split(":", -1);
      if (p.length != 3
          || result.size() >= 1000
          || !Set.of("CLOCKWISE", "ANTI_CLOCKWISE").contains(p[1])) {
        throw new IllegalArgumentException("Повреждена тетраэдрическая чётность.");
      }
      final AtomId id = AtomId.of(p[0]);
      final var refs = Arrays.stream(p[2].split(",", -1)).map(AtomId::of).toList();
      final var value = new Tetrahedron(id, refs, p[1].equals("CLOCKWISE") ? 1 : -1);
      if (!atoms.contains(id)
          || !atoms.containsAll(refs)
          || refs.contains(id)
          || result.put(id, value) != null) {
        throw new IllegalArgumentException("Повреждены атомы записи чётности.");
      }
      final var center =
          m.stereochemistry().centers().stream()
              .filter(
                  c ->
                      c.kind() == StereocenterKind.TETRAHEDRAL_ATOM && c.primaryAtomId().equals(id))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Нет центра для тетраэдрической чётности."));
      if (!center.referenceAtomIds().equals(refs)) {
        throw new IllegalArgumentException(
            "Изменился порядок заместителей тетраэдрической чётности.");
      }
    }
    return Map.copyOf(result);
  }

  public static int permutationSign(final List<AtomId> first, final List<AtomId> second) {
    if (first.size() != second.size() || !new HashSet<>(first).equals(new HashSet<>(second))) {
      return 0;
    }
    int sign = 1;
    for (int i = 0; i < first.size(); i++) {
      for (int j = i + 1; j < first.size(); j++) {
        if (second.indexOf(first.get(i)) > second.indexOf(first.get(j))) {
          sign = -sign;
        }
      }
    }
    return sign;
  }
}
