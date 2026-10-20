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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;

/** Exact labelled-graph comparison, independent of IDs, names, coordinates and provenance metadata.
 * Explicit atoms, bond types, isotopes, charge, spin and stored stereocenter references are retained.
 * A search limit is not evidence of difference. This does not normalize tautomers or aromatic forms.
 */
public final class MolecularGraphEquivalence {
  public enum Result {
    EQUIVALENT,
    DISTINCT,
    UNRESOLVED
  }

  private MolecularGraphEquivalence() {}

  public static Result compare(final Molecule first, final Molecule second) {
    return compare(first, second, 20000);
  }

  public static Result compare(final Molecule first, final Molecule second, final int searchLimit) {
    Objects.requireNonNull(first);
    Objects.requireNonNull(second);
    if (searchLimit < 1) {
      throw new IllegalArgumentException("Search limit must be positive.");
    }
    try {
      MolecularStereoParity.read(first);
      MolecularStereoParity.read(second);
      final var relativeA = MolecularRelativeStereo.read(first);
      final var relativeB = MolecularRelativeStereo.read(second);
      if (relativeA.isPresent()) {
        relativeA.get().validate(first);
      }
      if (relativeB.isPresent()) {
        relativeB.get().validate(second);
      }
      if (relativeA.isPresent() != relativeB.isPresent()) {
        return Result.DISTINCT;
      }
    } catch (final RuntimeException ex) {
      return Result.UNRESOLVED;
    }
    if (first.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)
        || second.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)) {
      return Result.UNRESOLVED;
    }
    if (first == second || first.equals(second)) {
      return Result.EQUIVALENT;
    }
    if (first.atomCount() != second.atomCount()
        || first.bondCount() != second.bondCount()
        || !first.charge().equals(second.charge())
        || !first.spinMultiplicity().equals(second.spinMultiplicity())
        || first.stereochemistry().centers().size() != second.stereochemistry().centers().size()) {
      return Result.DISTINCT;
    }
    if (first.atomCount() > 256 || Thread.currentThread().isInterrupted()) {
      return Result.UNRESOLVED;
    }
    final var search = new Search(first, second, searchLimit);
    if (!search.refine()) {
      return Result.DISTINCT;
    }
    if (search.match(0)) {
      return Result.EQUIVALENT;
    }
    return search.exhausted ? Result.UNRESOLVED : Result.DISTINCT;
  }

  private static final class Graph {
    final Molecule molecule;
    final int[][] edges;
    final Map<AtomId, MolecularStereoParity.Tetrahedron> parity;
    final MolecularRelativeStereo relative;
    final Set<AtomId> relativeAtoms;
    final Map<AtomId, Integer> indexes = new HashMap<>();

    Graph(final Molecule molecule) {
      this.molecule = molecule;
      this.parity = MolecularStereoParity.read(molecule);
      this.relative = MolecularRelativeStereo.read(molecule).orElse(null);
      this.relativeAtoms = this.relative == null ? Set.of() : this.relative.atoms();
      this.edges = new int[molecule.atomCount()][molecule.atomCount()];
      for (int i = 0; i < molecule.atomCount(); i++) {
        this.indexes.put(molecule.atoms().get(i).id(), i);
      }
      for (final var bond : molecule.bonds()) {
        final int a = this.indexes.get(bond.firstAtomId()),
            b = this.indexes.get(bond.secondAtomId());
        this.edges[a][b] = this.edges[b][a] = bond.type().ordinal() + 1;
      }
    }

    String label(final int i) {
      final var atom = this.molecule.atoms().get(i);
      final List<String> stereoRoles = new ArrayList<>();
      for (final var center : this.molecule.stereochemistry().centers()) {
        if (this.relativeAtoms.contains(center.primaryAtomId())) {
          continue;
        }
        final String prefix = center.kind() + ":" + center.descriptor() + ":";
        if (atom.id().equals(center.primaryAtomId())) {
          stereoRoles.add(prefix + "primary");
        }
        if (atom.id().equals(center.secondaryAtomId())) {
          stereoRoles.add(prefix + "secondary");
        }
        final int reference = center.referenceAtomIds().indexOf(atom.id());
        if (reference >= 0) {
          stereoRoles.add(
              prefix
                  + "reference"
                  + (center.kind() == StereocenterKind.TETRAHEDRAL_ATOM ? "" : reference));
        }
      }
      if (this.relative != null) {
        for (final var g : this.relative.groups()) {
          if (g.members().stream().anyMatch(m -> m.atom().equals(atom.id()))) {
            stereoRoles.add("relative:" + g.kind() + ":" + g.members().size());
          }
        }
      }
      Collections.sort(stereoRoles);
      return atom.element().symbol().value()
          + ":"
          + (atom.isotope() == null ? "natural" : atom.isotope().massNumber())
          + ":"
          + atom.formalCharge().value()
          + ":"
          + atom.radicalState().unpairedElectrons()
          + ":"
          + stereoRoles;
    }
  }

  private static final class Search {
    final Graph first, second;
    final int[] mapping;
    final boolean[] used;
    int[] a, b;
    int remaining;
    boolean exhausted;

    Search(final Molecule first, final Molecule second, final int limit) {
      this.first = new Graph(first);
      this.second = new Graph(second);
      this.remaining = limit;
      this.mapping = new int[first.atomCount()];
      Arrays.fill(this.mapping, -1);
      this.used = new boolean[first.atomCount()];
    }

    boolean refine() {
      final int n = this.mapping.length;
      final String[] labels = new String[n * 2];
      for (int i = 0; i < n; i++) {
        labels[i] = this.first.label(i);
        labels[n + i] = this.second.label(i);
      }
      int[] colors = colors(labels);
      for (int round = 0; round <= n; round++) {
        this.a = Arrays.copyOfRange(colors, 0, n);
        this.b = Arrays.copyOfRange(colors, n, n * 2);
        final int[] sortedA = this.a.clone(), sortedB = this.b.clone();
        Arrays.sort(sortedA);
        Arrays.sort(sortedB);
        if (!Arrays.equals(sortedA, sortedB)) {
          return false;
        }
        final int previousCount = (int) Arrays.stream(colors).distinct().count();
        for (int i = 0; i < n; i++) {
          labels[i] = neighbors(this.first, this.a, i);
          labels[n + i] = neighbors(this.second, this.b, i);
        }
        colors = colors(labels);
        if (Arrays.stream(colors).distinct().count() == previousCount) {
          break;
        }
      }
      return true;
    }

    static String neighbors(final Graph graph, final int[] colors, final int atom) {
      final List<String> neighbors = new ArrayList<>();
      for (int i = 0; i < colors.length; i++) {
        if (graph.edges[atom][i] != 0) {
          neighbors.add(graph.edges[atom][i] + ":" + colors[i]);
        }
      }
      Collections.sort(neighbors);
      return colors[atom] + "/" + String.join(",", neighbors);
    }

    static int[] colors(final String[] labels) {
      final var values = new TreeMap<String, Integer>();
      for (final String label : labels) {
        values.put(label, 0);
      }
      int index = 0;
      for (final String label : values.keySet()) {
        values.put(label, index++);
      }
      return Arrays.stream(labels).mapToInt(values::get).toArray();
    }

    boolean compatible(final int source, final int target) {
      if (this.used[target] || this.a[source] != this.b[target]) {
        return false;
      }
      if (this.remaining-- <= 0 || Thread.currentThread().isInterrupted()) {
        this.exhausted = true;
        return false;
      }
      for (int i = 0; i < this.mapping.length; i++) {
        if (this.mapping[i] >= 0
            && this.first.edges[source][i] != this.second.edges[target][this.mapping[i]]) {
          return false;
        }
      }
      return true;
    }

    boolean match(final int count) {
      if (this.remaining-- <= 0 || Thread.currentThread().isInterrupted()) {
        this.exhausted = true;
        return false;
      }
      if (count == this.mapping.length) {
        return this.stereoMatches();
      }
      int source = -1, smallest = Integer.MAX_VALUE;
      for (int i = 0; i < this.mapping.length; i++) {
        if (this.mapping[i] >= 0) {
          continue;
        }
        int available = 0;
        for (int j = 0; j < this.mapping.length; j++) {
          if (this.compatible(i, j)) {
            available++;
          }
        }
        if (available == 0) {
          return false;
        }
        if (available < smallest) {
          source = i;
          smallest = available;
        }
      }
      for (int target = 0; target < this.mapping.length; target++) {
        if (!this.compatible(source, target)) {
          continue;
        }
        this.mapping[source] = target;
        this.used[target] = true;
        if (this.match(count + 1)) {
          return true;
        }
        this.mapping[source] = -1;
        this.used[target] = false;
        if (this.exhausted) {
          return false;
        }
      }
      return false;
    }

    AtomId mapped(final AtomId id) {
      return id == null
          ? null
          : this.second.molecule.atoms().get(this.mapping[this.first.indexes.get(id)]).id();
    }

    boolean stereoMatches() {
      if (this.first.parity.size() != this.second.parity.size()) {
        return false;
      }
      for (final var center : this.first.molecule.stereochemistry().centers()) {
        if (this.first.relativeAtoms.contains(center.primaryAtomId())) {
          continue;
        }
        if (center.kind() == StereocenterKind.TETRAHEDRAL_ATOM) {
          final var target =
              this.second.molecule.stereochemistry().centers().stream()
                  .filter(
                      c ->
                          c.kind() == center.kind()
                              && c.primaryAtomId().equals(mapped(center.primaryAtomId())))
                  .findFirst()
                  .orElse(null);
          if (target == null
              || target.descriptor() != center.descriptor()
              || !new HashSet<>(center.referenceAtomIds().stream().map(this::mapped).toList())
                  .equals(new HashSet<>(target.referenceAtomIds()))) {
            return false;
          }
          final var a = this.first.parity.get(center.primaryAtomId());
          final var b = this.second.parity.get(target.primaryAtomId());
          if ((a == null) != (b == null)) {
            return false;
          }
          if (a != null
              && a.sign()
                      * MolecularStereoParity.permutationSign(
                          a.references().stream().map(this::mapped).toList(), b.references())
                  != b.sign()) {
            return false;
          }
        } else {
          final var mapped =
              Stereocenter.of(
                  center.kind(),
                  center.descriptor(),
                  this.mapped(center.primaryAtomId()),
                  this.mapped(center.secondaryAtomId()),
                  center.referenceAtomIds().stream().map(this::mapped).toList());
          if (!this.second.molecule.stereochemistry().centers().contains(mapped)) {
            return false;
          }
        }
      }
      if (this.first.relative != null) {
        if (this.second.relative == null
            || this.first.relative.groups().size() != this.second.relative.groups().size()) {
          return false;
        }
        for (final var g : this.first.relative.groups()) {
          final Set<AtomId> members =
              g.members().stream()
                  .map(m -> mapped(m.atom()))
                  .collect(java.util.stream.Collectors.toSet());
          final var other =
              this.second.relative.groups().stream()
                  .filter(
                      h ->
                          h.kind() == g.kind()
                              && h.members().stream()
                                  .map(MolecularRelativeStereo.Member::atom)
                                  .collect(java.util.stream.Collectors.toSet())
                                  .equals(members))
                  .findFirst()
                  .orElse(null);
          if (other == null) {
            return false;
          }
          Boolean inverse = null;
          for (final var m : g.members()) {
            final var descriptor =
                other.members().stream()
                    .filter(n -> n.atom().equals(mapped(m.atom())))
                    .findFirst()
                    .orElseThrow()
                    .descriptor();
            if (m.descriptor() == MolecularRelativeStereo.Descriptor.UNKNOWN
                || descriptor == MolecularRelativeStereo.Descriptor.UNKNOWN) {
              if (m.descriptor() != descriptor) {
                return false;
              }
              continue;
            }
            final boolean flipped = m.descriptor() != descriptor;
            if (inverse != null && inverse != flipped) {
              return false;
            }
            inverse = flipped;
          }
        }
      }
      return true;
    }
  }
}
