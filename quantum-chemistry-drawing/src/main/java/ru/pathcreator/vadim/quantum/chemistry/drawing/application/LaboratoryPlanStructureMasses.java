/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Opt-in PLAN model, from a complete single neutral explicit graph; never sample purity or a measured property. */
public final class LaboratoryPlanStructureMasses {
  private LaboratoryPlanStructureMasses() {}

  public static Map<String, BigDecimal> resolve(final LaboratoryDrawing drawing) {
    final Map<String, BigDecimal> masses = new LinkedHashMap<>();
    for (final var component : drawing.experiment().components()) {
      final BigDecimal mass = molarMass(component.molecule());
      if (mass != null) {
        masses.put(component.id(), mass);
      }
    }
    return Collections.unmodifiableMap(masses);
  }

  /** Null means manual properties are required, not that the molecule has zero mass. */
  public static BigDecimal molarMass(final Molecule molecule) {
    if (molecule.atoms().isEmpty()
        || molecule.atoms().size() > 4096
        || molecule.charge().value() != 0
        || molecule.spinMultiplicity().value() != 1
        || !molecule
            .metadata()
            .attributes()
            .getOrDefault("drawing.skeletal-hydrogens.unsupported", "0")
            .equals("0")) {
      return null;
    }
    final Map<AtomId, Integer> orders = new HashMap<>();
    final Map<AtomId, List<AtomId>> neighbours = new HashMap<>();
    for (final Atom atom : molecule.atoms()) {
      orders.put(atom.id(), 0);
      neighbours.put(atom.id(), new ArrayList<>());
    }
    for (final Bond bond : molecule.bonds()) {
      final int order =
          switch (bond.type()) {
            case SINGLE -> 1;
            case DOUBLE -> 2;
            case TRIPLE -> 3;
            default -> -1;
          };
      if (order < 0) {
        return null;
      }
      orders.merge(bond.firstAtomId(), order, Integer::sum);
      orders.merge(bond.secondAtomId(), order, Integer::sum);
      neighbours.get(bond.firstAtomId()).add(bond.secondAtomId());
      neighbours.get(bond.secondAtomId()).add(bond.firstAtomId());
    }
    for (final Atom atom : molecule.atoms()) {
      if (atom.formalCharge().value() != 0 || atom.radicalState().radical()) {
        return null;
      }
      final int normal =
          switch (atom.element().symbol().value()) {
            case "C", "Si" -> 4;
            case "B", "N" -> 3;
            case "O", "S" -> 2;
            case "H", "F", "Cl", "Br", "I" -> 1;
            default -> -1;
          };
      if (normal < 0 || orders.get(atom.id()) != normal) {
        return null;
      }
    }
    final Set<AtomId> visited = new HashSet<>();
    final ArrayDeque<AtomId> queue = new ArrayDeque<>();
    queue.add(molecule.atoms().getFirst().id());
    while (!queue.isEmpty()) {
      final AtomId id = queue.removeFirst();
      if (visited.add(id)) {
        queue.addAll(neighbours.get(id));
      }
    }
    if (visited.size() != molecule.atoms().size()) {
      return null;
    }
    final double mass = molecule.composition().mass().averageAtomicMass();
    return Double.isFinite(mass) && mass > 0 ? BigDecimal.valueOf(mass) : null;
  }
}
