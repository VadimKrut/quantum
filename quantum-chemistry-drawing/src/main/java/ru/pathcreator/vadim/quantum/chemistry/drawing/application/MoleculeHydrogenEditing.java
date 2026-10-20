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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValenceAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Explicit, reviewable additions. This does not change the core's explicit-atom formula semantics. */
public final class MoleculeHydrogenEditing {
  public record Suggestion(AtomId atomId, int present, Integer additional, String explanation) {}

  private MoleculeHydrogenEditing() {}

  /** Explicit one-command opt-in for a legacy skeletal drawing. */
  public static MoleculeDrawing normalizeSkeleton(final MoleculeDrawing drawing) {
    return MoleculeSkeletonHydrogens.normalize(drawing);
  }

  public static List<Suggestion> suggest(final MoleculeDrawing drawing) {
    final var molecule = drawing.molecule();
    final var atoms =
        molecule.atoms().stream()
            .collect(java.util.stream.Collectors.toMap(atom -> atom.id(), atom -> atom));
    final var valences =
        MolecularValenceAnalyzer.analyze(molecule).atomValences().stream()
            .collect(java.util.stream.Collectors.toMap(value -> value.atomId(), value -> value));
    final boolean inconsistentCharge =
        molecule.charge().value()
            != molecule.atoms().stream().mapToInt(atom -> atom.formalCharge().value()).sum();
    return molecule.atoms().stream()
        .filter(atom -> !atom.element().symbol().value().equals("H"))
        .map(
            atom -> {
              final var bonds =
                  molecule.bonds().stream()
                      .filter(
                          bond ->
                              bond.firstAtomId().equals(atom.id())
                                  || bond.secondAtomId().equals(atom.id()))
                      .toList();
              final int present =
                  (int)
                      bonds.stream()
                          .filter(
                              bond ->
                                  atoms
                                      .get(
                                          bond.firstAtomId().equals(atom.id())
                                              ? bond.secondAtomId()
                                              : bond.firstAtomId())
                                      .element()
                                      .symbol()
                                      .value()
                                      .equals("H"))
                          .count();
              if (inconsistentCharge
                  || molecule.spinMultiplicity().value() != 1
                  || atom.formalCharge().value() != 0
                  || atom.radicalState().radical()) {
                return new Suggestion(
                    atom.id(), present, null, "Заряд или радикал: число H задайте вручную.");
              }
              if (bonds.stream()
                  .anyMatch(
                      bond ->
                          bond.type() != BondType.SINGLE
                              && bond.type() != BondType.DOUBLE
                              && bond.type() != BondType.TRIPLE)) {
                return new Suggestion(
                    atom.id(),
                    present,
                    null,
                    "Ароматическая, координационная или неизвестная связь: число H задайте вручную.");
              }
              // Ordinary neutral valences only; the core's maximum-valence profile is not a
              // hydrogen rule.
              final int normal =
                  switch (atom.element().symbol().value()) {
                    case "C", "Si" -> 4;
                    case "B", "N" -> 3;
                    case "O", "S" -> 2;
                    case "F", "Cl", "Br", "I" -> 1;
                    default -> -1;
                  };
              if (normal < 0) {
                return new Suggestion(
                    atom.id(),
                    present,
                    null,
                    "Для этого элемента автоматическое предложение не задано.");
              }
              final double used = valences.get(atom.id()).bondOrderSum();
              if (used > normal || used != Math.rint(used)) {
                return new Suggestion(
                    atom.id(),
                    present,
                    null,
                    "Нет однозначного дополнения до обычной нейтральной валентности.");
              }
              return new Suggestion(
                  atom.id(),
                  present,
                  normal - (int) used,
                  "Нейтральный атом: обычная валентность "
                      + normal
                      + ", сумма порядков связей "
                      + (int) used
                      + ".");
            })
        .toList();
  }

  public static MoleculeDrawing add(
      final MoleculeDrawing source, final Map<AtomId, Integer> additions) {
    int total = 0;
    for (final var entry : additions.entrySet()) {
      final var atom =
          source.molecule().atoms().stream()
              .filter(value -> value.id().equals(entry.getKey()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Атом для добавления H отсутствует в структуре."));
      if (atom.element().symbol().value().equals("H")
          || entry.getValue() == null
          || entry.getValue() < 0
          || entry.getValue() > 8) {
        throw new IllegalArgumentException(
            "Добавьте от 0 до 8 H к каждому выбранному тяжёлому атому.");
      }
      total = Math.addExact(total, entry.getValue());
    }
    if (source.atoms().size() + total > 4096) {
      throw new IllegalArgumentException(
          "Команда ограничена 4096 атомами; добавляйте H к меньшему фрагменту.");
    }
    if (total == 0) {
      return source;
    }
    MoleculeDrawing result = source;
    final var sketch = new MoleculeSketchService();
    // Source order makes geometry independent from caller map iteration order.
    for (final var atom : source.molecule().atoms()) {
      final int count = additions.getOrDefault(atom.id(), 0);
      for (int index = 0; index < count; index++) {
        if (Thread.currentThread().isInterrupted()) {
          throw new java.util.concurrent.CancellationException("Подготовка водородов отменена.");
        }
        final var center =
            result.atoms().stream()
                .filter(value -> value.atomId().equals(atom.id()))
                .findFirst()
                .orElseThrow()
                .point2D();
        double bestAngle = 0;
        double bestScore = -Double.MAX_VALUE;
        for (int step = 0; step < 12; step++) {
          final double angle = Math.PI / 6 * step;
          final double x = center.x() + 32 * Math.cos(angle);
          final double y = center.y() + 32 * Math.sin(angle);
          double score = 0;
          for (final var other : result.atoms()) {
            if (other.atomId().equals(atom.id())) {
              continue;
            }
            final double distance = Math.hypot(other.point2D().x() - x, other.point2D().y() - y);
            score -= 1 / Math.max(1, distance * distance);
          }
          if (score > bestScore) {
            bestScore = score;
            bestAngle = angle;
          }
        }
        result =
            sketch.extend(
                result,
                atom.id(),
                "H",
                BondType.SINGLE,
                center.x() + 32 * Math.cos(bestAngle),
                center.y() + 32 * Math.sin(bestAngle));
      }
    }
    return sketch.translate(result, 0, 0);
  }
}
