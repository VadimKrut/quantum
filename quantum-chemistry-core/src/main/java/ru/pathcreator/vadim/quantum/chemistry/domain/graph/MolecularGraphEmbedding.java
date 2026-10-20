/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.graph;

import java.util.*;
import java.util.function.ToDoubleFunction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Bounded deterministic graph embedding. It is a starting geometry, not a conformer search. */
public final class MolecularGraphEmbedding {
  private record Edge(int first, int second, double length, BondType type) {}

  private final List<Edge> edges = new ArrayList<>();
  private final List<List<Integer>> neighbors = new ArrayList<>();
  private final List<int[]> rings = new ArrayList<>();
  private final double[][] xyz;
  private final boolean[] placed;
  private final double[] radii;
  private final boolean planar;
  private final boolean[] hydrogen;

  private MolecularGraphEmbedding(
      final Molecule molecule,
      final ToDoubleFunction<Bond> length,
      final ToDoubleFunction<Atom> radius,
      final boolean planar) {
    this.planar = planar;
    this.xyz = new double[molecule.atomCount()][3];
    this.placed = new boolean[molecule.atomCount()];
    this.radii = molecule.atoms().stream().mapToDouble(radius).toArray();
    this.hydrogen = new boolean[molecule.atomCount()];
    for (int i = 0; i < this.hydrogen.length; i++) {
      this.hydrogen[i] = molecule.atoms().get(i).element().atomicNumber() == 1;
    }
    final Map<AtomId, Integer> index = new HashMap<>();
    for (int i = 0; i < molecule.atomCount(); i++) {
      index.put(molecule.atoms().get(i).id(), i);
      this.neighbors.add(new ArrayList<>());
    }
    for (final Bond bond : molecule.bonds()) {
      final int a = index.get(bond.firstAtomId()), b = index.get(bond.secondAtomId());
      this.edges.add(new Edge(a, b, length.applyAsDouble(bond), bond.type()));
      this.neighbors.get(a).add(b);
      this.neighbors.get(b).add(a);
    }
    this.findRings();
  }

  public static double[] spatialCoordinates(
      final Molecule molecule,
      final ToDoubleFunction<Bond> length,
      final ToDoubleFunction<Atom> radius) {
    final MolecularGraphEmbedding seed =
        new MolecularGraphEmbedding(molecule, length, radius, false);
    seed.place();
    final double[] result = new double[molecule.atomCount() * 3];
    for (int i = 0; i < seed.xyz.length; i++) {
      System.arraycopy(seed.xyz[i], 0, result, i * 3, 3);
    }
    return result;
  }

  public static double[] planarCoordinates(final Molecule molecule, final double bondLength) {
    if (!Double.isFinite(bondLength) || bondLength <= 0) {
      throw new IllegalArgumentException("Drawing bond length must be positive.");
    }
    final MolecularGraphEmbedding seed =
        new MolecularGraphEmbedding(molecule, bond -> bondLength, atom -> bondLength / 2, true);
    seed.place();
    final double[] result = new double[molecule.atomCount() * 2];
    for (int i = 0; i < seed.xyz.length; i++) {
      result[i * 2] = seed.xyz[i][0];
      result[i * 2 + 1] = seed.xyz[i][1];
    }
    return result;
  }

  private void findRings() {
    // One shortest alternative path per edge: polynomial work, no enumeration of all simple cycles.
    final Set<String> known = new HashSet<>();
    for (final Edge excluded : this.edges) {
      final int[] parent = new int[this.xyz.length];
      Arrays.fill(parent, -1);
      parent[excluded.first] = excluded.first;
      final ArrayDeque<Integer> queue = new ArrayDeque<>();
      queue.add(excluded.first);
      while (!queue.isEmpty() && parent[excluded.second] < 0) {
        final int current = queue.remove();
        for (final int next : this.neighbors.get(current)) {
          if (current == excluded.first && next == excluded.second
              || current == excluded.second && next == excluded.first
              || parent[next] >= 0) {
            continue;
          }
          parent[next] = current;
          queue.add(next);
        }
      }
      if (parent[excluded.second] < 0) {
        continue;
      }
      final List<Integer> path = new ArrayList<>();
      int at = excluded.second;
      while (at != excluded.first) {
        path.add(at);
        at = parent[at];
      }
      path.add(at);
      final int[] ring = path.stream().mapToInt(Integer::intValue).toArray();
      final int[] key = ring.clone();
      Arrays.sort(key);
      if (known.add(Arrays.toString(key))) {
        this.rings.add(ring);
      }
    }
    this.rings.sort(Comparator.comparingInt(ring -> ring.length));
  }

  private void place() {
    final boolean[] visited = new boolean[this.xyz.length];
    double offset = 0;
    for (int root = 0; root < this.xyz.length; root++) {
      if (visited[root]) {
        continue;
      }
      final List<Integer> component = new ArrayList<>();
      final ArrayDeque<Integer> queue = new ArrayDeque<>();
      queue.add(root);
      visited[root] = true;
      while (!queue.isEmpty()) {
        final int current = queue.remove();
        component.add(current);
        for (final int next : this.neighbors.get(current)) {
          if (!visited[next]) {
            visited[next] = true;
            queue.add(next);
          }
        }
      }
      final Set<Integer> members = new HashSet<>(component);
      final List<int[]> localRings =
          this.rings.stream().filter(ring -> members.contains(ring[0])).toList();
      if (!localRings.isEmpty()) {
        this.placeRing(localRings.getFirst(), component);
      } else {
        this.placed[root] = true;
      }
      while (component.stream().anyMatch(index -> !this.placed[index])) {
        boolean progress = false;
        for (final int[] ring : localRings) {
          final long count = Arrays.stream(ring).filter(index -> this.placed[index]).count();
          if (count > 0 && count < ring.length && this.placeRing(ring, component)) {
            progress = true;
          }
        }
        if (progress) {
          continue;
        }
        final boolean heavyPending =
            this.planar
                && component.stream()
                    .filter(index -> this.placed[index])
                    .flatMap(index -> this.neighbors.get(index).stream())
                    .anyMatch(index -> !this.placed[index] && !this.hydrogen[index]);
        for (final int current : component) {
          if (!this.placed[current]) {
            continue;
          }
          final Integer next =
              this.neighbors.get(current).stream()
                  .filter(index -> !this.placed[index] && (!heavyPending || !this.hydrogen[index]))
                  .findFirst()
                  .orElse(null);
          if (next != null) {
            this.placeNeighbor(current, next, component);
            progress = true;
            break;
          }
        }
        if (!progress) {
          throw new IllegalStateException("Connected component could not be embedded.");
        }
      }
      this.closeBonds(component, members);
      final double minimum =
          component.stream().mapToDouble(index -> this.xyz[index][0]).min().orElse(0);
      final double maximum =
          component.stream().mapToDouble(index -> this.xyz[index][0]).max().orElse(0);
      for (final int index : component) {
        this.xyz[index][0] += offset - minimum;
      }
      offset += maximum - minimum + (this.planar ? 88 : 4);
    }
  }

  private boolean placeRing(final int[] original, final List<Integer> component) {
    int start = 0;
    boolean sharedEdge = false;
    for (int i = 0; i < original.length; i++) {
      if (this.placed[original[i]]) {
        start = i;
        if (this.placed[original[(i + 1) % original.length]]) {
          sharedEdge = true;
          break;
        }
      }
    }
    final int[] ring = new int[original.length];
    for (int i = 0; i < ring.length; i++) {
      ring[i] = original[(start + i) % ring.length];
    }
    double length = 0;
    for (int i = 0; i < ring.length; i++) {
      length += this.edge(ring[i], ring[(i + 1) % ring.length]).length;
    }
    length /= ring.length;
    double angle = 0;
    if (sharedEdge) {
      final double[] a = this.xyz[ring[0]], b = this.xyz[ring[1]];
      if (Math.abs(a[2] - b[2]) > 0.1) {
        return false;
      }
      length = Math.hypot(b[0] - a[0], b[1] - a[1]);
      angle = Math.atan2(b[1] - a[1], b[0] - a[0]);
    }
    double best = Double.POSITIVE_INFINITY;
    double[][] chosen = null;
    for (int direction : new int[] {-1, 1}) {
      for (int rotation = 0; rotation < (sharedEdge ? 1 : 24); rotation++) {
        final double[][] candidate = new double[ring.length][3];
        candidate[0] = this.xyz[ring[0]].clone();
        for (int i = 1; i < ring.length; i++) {
          final double turn =
              angle + rotation * Math.PI / 12 + direction * (i - 1) * Math.PI * 2 / ring.length;
          candidate[i][0] = candidate[i - 1][0] + length * Math.cos(turn);
          candidate[i][1] = candidate[i - 1][1] + length * Math.sin(turn);
          candidate[i][2] = candidate[0][2];
        }
        double score = 0;
        boolean compatible = true;
        for (int i = 0; i < ring.length; i++) {
          if (this.placed[ring[i]]) {
            if (distance(candidate[i], this.xyz[ring[i]]) > 0.2) {
              compatible = false;
              break;
            }
          } else {
            score += this.collision(candidate[i], component);
          }
        }
        if (compatible && score < best) {
          best = score;
          chosen = candidate;
        }
      }
    }
    if (chosen == null) {
      return false;
    }
    for (int i = 0; i < ring.length; i++) {
      if (!this.placed[ring[i]]) {
        this.xyz[ring[i]] = chosen[i];
        this.placed[ring[i]] = true;
      }
    }
    return true;
  }

  private void placeNeighbor(final int from, final int next, final List<Integer> component) {
    final List<Integer> occupied =
        this.neighbors.get(from).stream().filter(index -> this.placed[index]).toList();
    final List<Edge> incident =
        this.edges.stream().filter(edge -> edge.first == from || edge.second == from).toList();
    final boolean linear =
        incident.stream().anyMatch(edge -> edge.type == BondType.TRIPLE)
            || incident.stream().filter(edge -> edge.type == BondType.DOUBLE).count() >= 2;
    final boolean planar =
        !linear
            && incident.stream()
                .anyMatch(edge -> edge.type == BondType.DOUBLE || edge.type == BondType.AROMATIC);
    final double target = linear ? -1 : this.planar || planar ? -0.5 : -1.0 / 3;
    final double length = this.edge(from, next).length;
    double score = Double.POSITIVE_INFINITY;
    double[] chosen = null;
    final List<double[]> directions = new ArrayList<>();
    for (int i = 0; i < 72; i++) {
      directions.add(new double[] {Math.cos(i * Math.PI / 36), Math.sin(i * Math.PI / 36), 0});
    }
    if (!planar && !this.planar) {
      for (int i = 0; i < 72; i++) {
        final double z = 1 - 2 * (i + 0.5) / 72;
        final double radius = Math.sqrt(1 - z * z);
        final double angle = i * 2.399963229728653;
        directions.add(new double[] {radius * Math.cos(angle), radius * Math.sin(angle), z});
      }
    }
    for (final int other : occupied) {
      final double distance = distance(this.xyz[from], this.xyz[other]);
      if (distance > 1e-8) {
        directions.add(
            new double[] {
              (this.xyz[from][0] - this.xyz[other][0]) / distance,
              (this.xyz[from][1] - this.xyz[other][1]) / distance,
              (this.xyz[from][2] - this.xyz[other][2]) / distance
            });
      }
    }
    for (final double[] direction : directions) {
      final double[] point = new double[3];
      for (int axis = 0; axis < 3; axis++) {
        point[axis] = this.xyz[from][axis] + length * direction[axis];
      }
      double cost = 0.05 * this.collision(point, component);
      for (final int other : occupied) {
        double dot = 0;
        for (int axis = 0; axis < 3; axis++) {
          dot += direction[axis] * (this.xyz[other][axis] - this.xyz[from][axis]);
        }
        dot /= Math.max(1e-8, distance(this.xyz[other], this.xyz[from]));
        cost += 10 * Math.pow(dot - target, 2);
      }
      if (cost < score) {
        score = cost;
        chosen = point;
      }
    }
    this.xyz[next] = chosen;
    this.placed[next] = true;
  }

  private void closeBonds(final List<Integer> component, final Set<Integer> members) {
    final List<Edge> local =
        this.edges.stream().filter(edge -> members.contains(edge.first)).toList();
    final List<int[]> nonbonded = new ArrayList<>();
    boolean strained =
        local.stream()
            .anyMatch(
                edge ->
                    Math.abs(distance(this.xyz[edge.first], this.xyz[edge.second]) - edge.length)
                        > 0.25);
    for (int i = 0; i < component.size(); i++) {
      for (int j = i + 1; j < component.size(); j++) {
        final int a = component.get(i), b = component.get(j);
        if (!this.neighbors.get(a).contains(b)) {
          nonbonded.add(new int[] {a, b});
          if (distance(this.xyz[a], this.xyz[b]) < 0.6 * (this.radii[a] + this.radii[b])) {
            strained = true;
          }
        }
      }
    }
    // Bridges cannot always be embedded in a plane. Break that symmetry only for a strained seed.
    if (strained && !this.planar) {
      this.spatialSeed(component, local);
    }
    for (int iteration = 0; iteration < 480; iteration++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException("Geometry preparation interrupted.");
      }
      double residual = 0;
      for (final Edge edge : local) {
        residual =
            Math.max(
                residual,
                Math.abs(distance(this.xyz[edge.first], this.xyz[edge.second]) - edge.length));
        this.constrain(edge.first, edge.second, edge.length, 0.35);
      }
      for (final int[] pair : nonbonded) {
        final double minimum = Math.max(0.25, 0.75 * (this.radii[pair[0]] + this.radii[pair[1]]));
        final double gap = minimum - distance(this.xyz[pair[0]], this.xyz[pair[1]]);
        if (gap > 0) {
          residual = Math.max(residual, gap);
          this.constrain(pair[0], pair[1], minimum, 0.35);
        }
      }
      if (residual < 0.0001) {
        break;
      }
    }
    if (!this.planar
        && (local.stream()
                .anyMatch(
                    edge ->
                        Math.abs(
                                distance(this.xyz[edge.first], this.xyz[edge.second]) - edge.length)
                            > edge.length * 0.15)
            || nonbonded.stream()
                .anyMatch(pair -> distance(this.xyz[pair[0]], this.xyz[pair[1]]) < 0.20))) {
      throw new IllegalArgumentException(
          "Не удалось согласовать начальную геометрию связей. Задайте координаты вручную или импортируйте подготовленную структуру.");
    }
  }

  // Three spatial directions from double-centred graph distances, then all bond constraints are
  // enforced.
  private void spatialSeed(final List<Integer> component, final List<Edge> edges) {
    final int size = component.size();
    final Map<Integer, Integer> index = new HashMap<>();
    final double[][] distance = new double[size][size];
    for (int i = 0; i < size; i++) {
      index.put(component.get(i), i);
      Arrays.fill(distance[i], Double.POSITIVE_INFINITY);
      distance[i][i] = 0;
    }
    for (final Edge edge : edges) {
      final int a = index.get(edge.first), b = index.get(edge.second);
      distance[a][b] = edge.length;
      distance[b][a] = edge.length;
    }
    for (int k = 0; k < size; k++) {
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          distance[i][j] = Math.min(distance[i][j], distance[i][k] + distance[k][j]);
        }
      }
    }
    final double[] mean = new double[size];
    double total = 0;
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        distance[i][j] *= distance[i][j];
        mean[i] += distance[i][j] / size;
      }
      total += mean[i] / size;
    }
    final double[][] matrix = new double[size][size];
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        matrix[i][j] = -0.5 * (distance[i][j] - mean[i] - mean[j] + total);
      }
    }
    final double[][] basis = new double[3][size];
    for (int axis = 0; axis < 3; axis++) {
      double[] vector = new double[size];
      for (int i = 0; i < size; i++) {
        vector[i] = Math.sin((i + 1) * (axis + 1) * 1.137);
      }
      for (int step = 0; step < 120; step++) {
        final double[] next = new double[size];
        for (int i = 0; i < size; i++) {
          for (int j = 0; j < size; j++) {
            next[i] += matrix[i][j] * vector[j];
          }
        }
        for (int previous = 0; previous < axis; previous++) {
          double dot = 0;
          for (int i = 0; i < size; i++) {
            dot += next[i] * basis[previous][i];
          }
          for (int i = 0; i < size; i++) {
            next[i] -= dot * basis[previous][i];
          }
        }
        double norm = 0;
        for (final double value : next) {
          norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm < 1e-10) {
          break;
        }
        for (int i = 0; i < size; i++) {
          next[i] /= norm;
        }
        vector = next;
      }
      basis[axis] = vector;
      double eigenvalue = 0;
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          eigenvalue += vector[i] * matrix[i][j] * vector[j];
        }
      }
      for (int i = 0; i < size; i++) {
        this.xyz[component.get(i)][axis] =
            eigenvalue > 1e-8
                ? vector[i] * Math.sqrt(eigenvalue)
                : 0.1 * Math.sin((i + 1) * (axis + 1));
      }
    }
  }

  private void constrain(final int a, final int b, final double target, final double weight) {
    double length = distance(this.xyz[a], this.xyz[b]);
    if (length < 1e-8) {
      this.xyz[b][this.planar ? 1 : 2] += 0.05;
      length = distance(this.xyz[a], this.xyz[b]);
    }
    final double ratio = (length - target) / length * weight;
    for (int axis = 0; axis < 3; axis++) {
      final double change = (this.xyz[b][axis] - this.xyz[a][axis]) * ratio;
      this.xyz[a][axis] += change;
      this.xyz[b][axis] -= change;
    }
  }

  private double collision(final double[] point, final List<Integer> component) {
    double sum = 0;
    for (final int index : component) {
      if (this.placed[index]) {
        sum += 1 / Math.pow(Math.max(0.05, distance(point, this.xyz[index])), 4);
      }
    }
    return sum;
  }

  private Edge edge(final int a, final int b) {
    return this.edges.stream()
        .filter(edge -> edge.first == a && edge.second == b || edge.first == b && edge.second == a)
        .findFirst()
        .orElseThrow();
  }

  private static double distance(final double[] a, final double[] b) {
    return Math.sqrt(
        Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
  }
}
