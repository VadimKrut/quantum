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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Rotates rigid spiro blocks and branches to reduce crossings in display coordinates only. */
final class LaboratoryDrawingUntangler {
  private record Edge(int a, int b) {}

  private record Branch(int pivot, int[] moving) {}

  private record Exterior(int atom, int parent, int first, int second) {}

  private LaboratoryDrawingUntangler() {}

  static void improve(final Molecule molecule, final double[] points, final double bondLength) {
    final Map<AtomId, Integer> indices = new HashMap<>();
    final List<List<Integer>> neighbors = new ArrayList<>();
    final List<Integer> heavy = new ArrayList<>();
    for (int i = 0; i < molecule.atomCount(); i++) {
      indices.put(molecule.atoms().get(i).id(), i);
      neighbors.add(new ArrayList<>());
      if (molecule.atoms().get(i).element().atomicNumber() != 1) {
        heavy.add(i);
      }
    }
    // Bound the display cleanup independently of the chemical graph's size.
    if (heavy.size() < 4 || heavy.size() > 160) {
      return;
    }
    final Set<Integer> heavySet = new HashSet<>(heavy);
    final List<Edge> edges = new ArrayList<>();
    final List<Edge> rotatable = new ArrayList<>();
    for (final var bond : molecule.bonds()) {
      final int a = indices.get(bond.firstAtomId()), b = indices.get(bond.secondAtomId());
      neighbors.get(a).add(b);
      neighbors.get(b).add(a);
      if (heavySet.contains(a) && heavySet.contains(b)) {
        final var edge = new Edge(a, b);
        edges.add(edge);
        // A terminal carbonyl oxygen has no E/Z configuration. Moving this display branch
        // also resolves an exocyclic C=O incorrectly drawn through a neighboring ring.
        if (bond.type() == BondType.SINGLE
            || bond.type() == BondType.DOUBLE
                && (terminalOxygen(molecule, a) || terminalOxygen(molecule, b))) {
          rotatable.add(edge);
        }
      }
    }
    if (edges.size() > 200) {
      return;
    }
    final List<Exterior> exterior = new ArrayList<>();
    for (final int atom : heavy) {
      if (!terminalOxygen(molecule, atom) || neighbors.get(atom).size() != 1) {
        continue;
      }
      final int parent = neighbors.get(atom).getFirst();
      if (molecule.bonds().stream()
          .noneMatch(
              b ->
                  b.type() == BondType.DOUBLE
                      && (indices.get(b.firstAtomId()) == atom
                          || indices.get(b.secondAtomId()) == atom))) {
        continue;
      }
      final var ringNeighbors =
          neighbors.get(parent).stream().filter(n -> n != atom && heavySet.contains(n)).toList();
      if (ringNeighbors.size() == 2
          && connectedWithout(ringNeighbors.get(0), ringNeighbors.get(1), parent, neighbors)) {
        exterior.add(new Exterior(atom, parent, ringNeighbors.get(0), ringNeighbors.get(1)));
      }
    }
    double best = score(points, heavy, edges, exterior, bondLength);
    if (best < 0.001) {
      return;
    }
    final List<Branch> branches = new ArrayList<>();
    // Separate rings sharing exactly one atom before rotating terminal substituents.
    // A bridge-only search cannot move either spiro ring and may instead put a carbonyl inside a
    // ring.
    for (final int pivot : heavy) {
      if (neighbors.get(pivot).stream().filter(heavySet::contains).count() != 4
          || molecule.bonds().stream()
              .anyMatch(
                  b ->
                      (indices.get(b.firstAtomId()) == pivot
                              || indices.get(b.secondAtomId()) == pivot)
                          && b.type() != BondType.SINGLE)) {
        continue;
      }
      final Set<Integer> visited = new HashSet<>();
      visited.add(pivot);
      final List<int[]> rings = new ArrayList<>();
      for (final int neighbor : neighbors.get(pivot)) {
        if (visited.contains(neighbor)) {
          continue;
        }
        final Set<Integer> block = new LinkedHashSet<>();
        final ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(neighbor);
        visited.add(neighbor);
        while (!queue.isEmpty()) {
          final int current = queue.remove();
          block.add(current);
          for (final int next : neighbors.get(current)) {
            if (visited.add(next)) {
              queue.add(next);
            }
          }
        }
        if (block.stream().filter(heavySet::contains).count() >= 2
            && neighbors.get(pivot).stream().filter(block::contains).count() == 2) {
          rings.add(block.stream().mapToInt(Integer::intValue).toArray());
        }
      }
      if (rings.size() == 2) {
        for (final int[] ring : rings) {
          branches.add(new Branch(pivot, ring));
        }
      }
    }
    for (final var edge : rotatable) {
      final var moving = side(edge.b(), edge.a(), neighbors);
      if (moving.contains(edge.a())) {
        continue;
      }
      final var opposite = side(edge.a(), edge.b(), neighbors);
      final long size = moving.stream().filter(heavySet::contains).count();
      final long otherSize = opposite.stream().filter(heavySet::contains).count();
      if (size <= otherSize) {
        branches.add(new Branch(edge.a(), moving.stream().mapToInt(Integer::intValue).toArray()));
      } else {
        branches.add(new Branch(edge.b(), opposite.stream().mapToInt(Integer::intValue).toArray()));
      }
    }
    int evaluations = 0;
    for (int sweep = 0; sweep < 4; sweep++) {
      boolean improved = false;
      for (final var branch : branches) {
        final double[] original = points.clone();
        double[] chosen = null;
        for (int step = 1; step < 12; step++) {
          if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("Drawing layout interrupted.");
          }
          if (++evaluations > 4096) {
            return;
          }
          final double[] candidate = original.clone();
          final double angle = step * Math.PI / 6, cosine = Math.cos(angle), sine = Math.sin(angle);
          final double px = original[branch.pivot() * 2], py = original[branch.pivot() * 2 + 1];
          for (final int atom : branch.moving()) {
            final double dx = original[atom * 2] - px, dy = original[atom * 2 + 1] - py;
            candidate[atom * 2] = px + cosine * dx - sine * dy;
            candidate[atom * 2 + 1] = py + sine * dx + cosine * dy;
          }
          final double value = score(candidate, heavy, edges, exterior, bondLength);
          if (value < best - 1e-7) {
            best = value;
            chosen = candidate;
          }
        }
        if (chosen != null) {
          System.arraycopy(chosen, 0, points, 0, points.length);
          improved = true;
        }
      }
      if (!improved || best < 0.001) {
        break;
      }
    }
  }

  private static Set<Integer> side(
      final int root, final int blocked, final List<List<Integer>> neighbors) {
    final Set<Integer> visited = new LinkedHashSet<>();
    final ArrayDeque<Integer> queue = new ArrayDeque<>();
    visited.add(root);
    queue.add(root);
    while (!queue.isEmpty()) {
      final int current = queue.remove();
      for (final int next : neighbors.get(current)) {
        if (current == root && next == blocked) {
          continue;
        }
        if (visited.add(next)) {
          queue.add(next);
        }
      }
    }
    return visited;
  }

  private static boolean connectedWithout(
      final int start, final int target, final int blocked, final List<List<Integer>> neighbors) {
    final Set<Integer> seen = new HashSet<>();
    seen.add(blocked);
    seen.add(start);
    final ArrayDeque<Integer> queue = new ArrayDeque<>();
    queue.add(start);
    while (!queue.isEmpty()) {
      for (final int next : neighbors.get(queue.remove())) {
        if (next == target) {
          return true;
        }
        if (seen.add(next)) {
          queue.add(next);
        }
      }
    }
    return false;
  }

  private static boolean terminalOxygen(final Molecule molecule, final int index) {
    final var atom = molecule.atoms().get(index);
    return atom.element().atomicNumber() == 8
        && molecule.bonds().stream()
                .filter(
                    b -> b.firstAtomId().equals(atom.id()) || b.secondAtomId().equals(atom.id()))
                .count()
            == 1;
  }

  private static double score(
      final double[] p,
      final List<Integer> heavy,
      final List<Edge> edges,
      final List<Exterior> exterior,
      final double length) {
    double cost = 0;
    // A terminal carbonyl on a ring belongs outside its local ring angle, not inside its empty
    // face.
    for (final var site : exterior) {
      final double ox = p[site.atom() * 2] - p[site.parent() * 2],
          oy = p[site.atom() * 2 + 1] - p[site.parent() * 2 + 1];
      final double dx = p[site.parent() * 2] - (p[site.first() * 2] + p[site.second() * 2]) / 2,
          dy = p[site.parent() * 2 + 1] - (p[site.first() * 2 + 1] + p[site.second() * 2 + 1]) / 2;
      final double norm = Math.hypot(ox, oy) * Math.hypot(dx, dy);
      if (norm > 1e-12) {
        final double gap = .25 - (ox * dx + oy * dy) / norm;
        if (gap > 0) {
          cost += 100 * gap * gap;
        }
      }
    }
    for (int i = 0; i < heavy.size(); i++) {
      final int a = heavy.get(i);
      for (int j = i + 1; j < heavy.size(); j++) {
        final int b = heavy.get(j);
        final double gap =
            0.85 - Math.hypot(p[a * 2] - p[b * 2], p[a * 2 + 1] - p[b * 2 + 1]) / length;
        if (gap > 0) {
          cost += 20 * gap * gap;
        }
      }
      for (final var edge : edges) {
        if (a == edge.a() || a == edge.b()) {
          continue;
        }
        final double gap = 0.4 - pointToSegment(p, a, edge) / length;
        if (gap > 0) {
          cost += 100 * gap * gap;
        }
      }
    }
    for (int i = 0; i < edges.size(); i++) {
      final var a = edges.get(i);
      for (int j = i + 1; j < edges.size(); j++) {
        final var b = edges.get(j);
        if (a.a() == b.a() || a.a() == b.b() || a.b() == b.a() || a.b() == b.b()) {
          continue;
        }
        if (cross(p, a.a(), a.b(), b.a()) * cross(p, a.a(), a.b(), b.b()) < -1e-8
            && cross(p, b.a(), b.b(), a.a()) * cross(p, b.a(), b.b(), a.b()) < -1e-8) {
          cost += 1000;
        }
      }
    }
    return cost;
  }

  private static double cross(final double[] p, final int a, final int b, final int c) {
    return (p[b * 2] - p[a * 2]) * (p[c * 2 + 1] - p[a * 2 + 1])
        - (p[b * 2 + 1] - p[a * 2 + 1]) * (p[c * 2] - p[a * 2]);
  }

  private static double pointToSegment(final double[] p, final int point, final Edge edge) {
    final double ax = p[edge.a() * 2], ay = p[edge.a() * 2 + 1];
    final double dx = p[edge.b() * 2] - ax, dy = p[edge.b() * 2 + 1] - ay;
    final double t =
        Math.max(
            0,
            Math.min(
                1,
                ((p[point * 2] - ax) * dx + (p[point * 2 + 1] - ay) * dy)
                    / Math.max(1e-12, dx * dx + dy * dy)));
    return Math.hypot(p[point * 2] - ax - t * dx, p[point * 2 + 1] - ay - t * dy);
  }
}
