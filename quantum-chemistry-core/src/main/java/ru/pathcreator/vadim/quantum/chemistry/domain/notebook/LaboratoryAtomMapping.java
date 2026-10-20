/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;

/** Imported atom labels belong to a stage, not to the identity of a molecule. */
public record LaboratoryAtomMapping(List<Label> labels, Map<String, String> signatures) {
  public static final LaboratoryAtomMapping EMPTY = new LaboratoryAtomMapping(List.of(), Map.of());

  public record Label(String componentId, AtomId atomId, int number) {
    public Label {
      componentId = IdentifierValue.requireIdentifier(componentId, "Mapped component");
      Objects.requireNonNull(atomId);
      if (number < 1 || number > 999999) {
        throw new IllegalArgumentException("Номер атомной карты: 1–999999.");
      }
    }
  }

  public LaboratoryAtomMapping {
    labels = List.copyOf(labels);
    signatures = Map.copyOf(signatures);
    if (labels.size() > 10000) {
      throw new IllegalArgumentException("Не более 10000 отмеченных атомов.");
    }
    final Set<String> atoms = new HashSet<>(), components = new HashSet<>();
    for (final Label label : labels) {
      if (!atoms.add(label.componentId() + ":" + label.atomId().value())) {
        throw new IllegalArgumentException("Атому назначено несколько номеров карты.");
      }
      components.add(label.componentId());
    }
    if (!components.equals(signatures.keySet())
        || signatures.values().stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException(
          "Карта должна хранить подписи всех и только отмеченных компонентов.");
    }
  }

  public boolean empty() {
    return this.labels.isEmpty();
  }

  /** Stale maps remain in recoverable drafts, but may not be exported as current mappings. */
  public List<String> problems(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage) {
    final List<String> problems = new ArrayList<>();
    final Map<String, LaboratoryComponent> components = new HashMap<>();
    experiment.components().forEach(c -> components.put(c.id(), c));
    final Map<String, ReactionComponentRole> roles = new HashMap<>();
    stage.participants().forEach(p -> roles.put(p.componentId(), p.role()));
    for (final var source : this.signatures.entrySet()) {
      final var c = components.get(source.getKey());
      if (c == null
          || !roles.containsKey(source.getKey())
          || !LaboratoryMaterialProperties.signature(c.molecule()).equals(source.getValue())) {
        problems.add(
            "Карта устарела: изменён или исключён компонент "
                + (c == null ? source.getKey() : c.molecule().displayName())
                + ".");
      }
    }
    if (!problems.isEmpty()) {
      return List.copyOf(problems);
    }
    final Map<String, Atom> seen = new HashMap<>();
    final Set<Integer> agentNumbers = new HashSet<>(), sideNumbers = new HashSet<>();
    for (final Label label : this.labels) {
      final var c = components.get(label.componentId());
      final var atom =
          c.molecule().atoms().stream().filter(a -> a.id().equals(label.atomId())).findFirst();
      if (atom.isEmpty()) {
        problems.add("В карте отсутствует атом " + label.atomId().value() + ".");
        continue;
      }
      final var role = roles.get(label.componentId());
      final String side =
          role == ReactionComponentRole.REACTANT
              ? "R"
              : role == ReactionComponentRole.PRODUCT ? "P" : "A";
      if (seen.putIfAbsent(side + label.number(), atom.get()) != null) {
        problems.add("Номер " + label.number() + " повторяется на одной стороне реакции.");
      }
      if (side.equals("A")) {
        agentNumbers.add(label.number());
      } else {
        sideNumbers.add(label.number());
      }
    }
    for (final int n : sideNumbers) {
      if (agentNumbers.contains(n)) {
        problems.add("Номер " + n + " назначен одновременно агенту и участнику реакции.");
      }
      final Atom a = seen.get("R" + n), b = seen.get("P" + n);
      if (a != null
          && b != null
          && (!a.element().symbol().equals(b.element().symbol())
              || !Objects.equals(a.isotope(), b.isotope()))) {
        problems.add("Номер " + n + " связывает разные элементы или изотопы.");
      }
    }
    return List.copyOf(new LinkedHashSet<>(problems));
  }
}
