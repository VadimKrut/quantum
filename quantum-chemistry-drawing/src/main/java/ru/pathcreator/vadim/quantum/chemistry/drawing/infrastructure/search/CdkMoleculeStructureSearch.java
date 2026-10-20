/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.search;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.openscience.cdk.aromaticity.*;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.isomorphism.*;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.fingerprint.Fingerprinter;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStructureSearch;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereoIdentity;

/** CDK 2.12 runs only on disposable graph copies. IDs and coordinates never define identity. */
public final class CdkMoleculeStructureSearch implements MoleculeStructureSearch {
  private record Projection(Molecule molecule, IAtomContainer graph) {}

  private static final class PathFingerprint extends Fingerprinter {
    PathFingerprint() {
      super(1024, 7);
      this.setPathLimit(12000);
    }

    BitSet bits(final IAtomContainer graph) throws org.openscience.cdk.exception.CDKException {
      final BitSet bits = new BitSet(1024);
      this.encodePaths(graph, 7, bits, 1024);
      return bits;
    }
  }

  @Override
  public Query compile(
      final MoleculeDrawing drawing,
      final Set<AtomId> selected,
      final Options options,
      final BooleanSupplier cancelled) {
    return this.compile(configuration(drawing, options, cancelled), selected, options, cancelled);
  }

  private static Molecule configuration(
      final MoleculeDrawing drawing, final Options options, final BooleanSupplier cancelled) {
    Objects.requireNonNull(drawing);
    Objects.requireNonNull(options);
    Objects.requireNonNull(cancelled);
    check(cancelled);
    if (!options.stereo() || options.mode() == Mode.SIMILARITY) {
      return drawing.molecule();
    }
    if (MolecularRelativeStereo.present(drawing.molecule())) {
      throw unresolved(
          "относительные / рацемические группы и неизвестная абсолютная конфигурация не сравниваются этим поиском. Отключите стереофильтр только для поиска по графу.");
    }
    final Molecule verified = CdkStereoIdentity.verifiedMolecule(drawing);
    check(cancelled);
    supportedStereo(verified);
    return verified;
  }

  @Override
  public Query compile(
      final Molecule molecule,
      final Set<AtomId> selected,
      final Options options,
      final BooleanSupplier cancelled) {
    Objects.requireNonNull(molecule);
    Objects.requireNonNull(options);
    Objects.requireNonNull(cancelled);
    check(cancelled);
    final Set<AtomId> chosen = Set.copyOf(selected);
    if (!chosen.isEmpty() && (options.mode() == Mode.EXACT || options.stereo())) {
      throw new IllegalArgumentException(
          "Для выделенного фрагмента используйте подструктуру или сходство без стереофильтра.");
    }
    final boolean suppressHydrogens =
        molecule.atoms().stream()
            .anyMatch(
                a ->
                    a.element().atomicNumber() != 1
                        && (chosen.isEmpty() || chosen.contains(a.id())));
    final Projection full = project(molecule, suppressHydrogens);
    final Projection query = chosen.isEmpty() ? full : fragment(full, chosen);
    if (options.stereo() && options.mode() != Mode.SIMILARITY) {
      supportedStereo(molecule);
    }
    final BitSet queryBits = options.mode() == Mode.SIMILARITY ? fingerprint(query) : null;
    return new Query() {
      private long deadline;
      private int visits;

      private void guard() {
        check(cancelled);
        if (++this.visits > 500000 || System.nanoTime() > this.deadline) {
          throw new IllegalArgumentException(
              "Превышен предел сопоставления одной структуры; результат для неё неизвестен.");
        }
      }

      @Override
      public Match match(final MoleculeDrawing drawing) {
        return this.match(configuration(drawing, options, cancelled));
      }

      @Override
      public Match match(final Molecule molecule) {
        check(cancelled);
        this.deadline = System.nanoTime() + 1_000_000_000L;
        this.visits = 0;
        if (options.mode() == Mode.EXACT
            && (!query.molecule().charge().equals(molecule.charge())
                || !query.molecule().spinMultiplicity().equals(molecule.spinMultiplicity()))) {
          return miss();
        }
        final Projection target = project(molecule, suppressHydrogens);
        this.guard();
        if (options.mode() == Mode.SIMILARITY) {
          final BitSet targetBits = fingerprint(target),
              intersection = (BitSet) queryBits.clone(),
              union = (BitSet) queryBits.clone();
          intersection.and(targetBits);
          union.or(targetBits);
          final double score =
              union.isEmpty() ? 0 : (double) intersection.cardinality() / union.cardinality();
          this.guard();
          return new Match(score >= options.threshold(), score, Set.of());
        }
        final AtomMatcher atoms =
            new AtomMatcher() {
              @Override
              public boolean matches(final IAtom a, final IAtom b) {
                guard();
                return Objects.equals(a.getAtomicNumber(), b.getAtomicNumber())
                    && Objects.equals(a.getFormalCharge(), b.getFormalCharge())
                    && Objects.equals(a.getMassNumber(), b.getMassNumber())
                    && Objects.equals(a.getProperty("core.radical"), b.getProperty("core.radical"))
                    && (options.mode() != Mode.EXACT
                        || Objects.equals(
                            a.getImplicitHydrogenCount(), b.getImplicitHydrogenCount()));
              }
            };
        final BondMatcher bonds =
            new BondMatcher() {
              @Override
              public boolean matches(final IBond a, final IBond b) {
                guard();
                return a.isAromatic() == b.isAromatic()
                    && (a.isAromatic() || a.getOrder() == b.getOrder());
              }
            };
        final Pattern pattern =
            options.mode() == Mode.EXACT
                ? VentoFoggia.findIdentical(query.graph(), atoms, bonds)
                : VentoFoggia.findSubstructure(query.graph(), atoms, bonds);
        for (final int[] mapping : pattern.matchAll(target.graph())) {
          this.guard();
          if (options.stereo() && !stereo(query, target, mapping, options.mode() == Mode.EXACT)) {
            continue;
          }
          final Set<AtomId> matched = new LinkedHashSet<>();
          for (final int index : mapping) {
            matched.add(AtomId.of(target.graph().getAtom(index).getID()));
          }
          return new Match(true, 1, matched);
        }
        return miss();
      }
    };
  }

  private static Match miss() {
    return new Match(false, 0, Set.of());
  }

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Структурный поиск остановлен.");
    }
  }

  private static BitSet fingerprint(final Projection projection) {
    try {
      final BitSet bits = new PathFingerprint().bits(projection.graph());
      if (bits.isEmpty()) {
        throw new IllegalArgumentException(
            "Структурный отпечаток пуст; сходство для этой структуры не определено.");
      }
      return bits;
    } catch (final org.openscience.cdk.exception.CDKException error) {
      throw new IllegalArgumentException(
          "Не удалось рассчитать структурный отпечаток: " + error.getMessage(), error);
    }
  }

  private static Projection fragment(final Projection full, final Set<AtomId> selected) {
    if (full.molecule().atoms().stream().filter(a -> selected.contains(a.id())).count()
        != selected.size()) {
      throw new IllegalArgumentException("Выделенный фрагмент ссылается на отсутствующие атомы.");
    }
    final IAtomContainer graph = new AtomContainer();
    for (final IAtom atom : full.graph().atoms()) {
      if (selected.contains(AtomId.of(atom.getID()))) {
        graph.addAtom(atom);
      }
    }
    for (final IBond bond : full.graph().bonds()) {
      if (graph.contains(bond.getBegin()) && graph.contains(bond.getEnd())) {
        graph.addBond(bond);
      }
    }
    if (graph.getAtomCount() == 0) {
      throw new IllegalArgumentException("Выберите непустой фрагмент структуры.");
    }
    return new Projection(full.molecule(), graph);
  }

  private static Projection project(final Molecule molecule, final boolean suppressHydrogens) {
    if (molecule.atomCount() == 0
        || molecule.atomCount() > 1000
        || molecule.atoms().stream().filter(a -> a.element().atomicNumber() != 1).count() > 256) {
      throw new IllegalArgumentException(
          "Поддерживаются непустые структуры до 256 тяжёлых и 1000 всех атомов.");
    }
    if (molecule.bonds().stream()
        .anyMatch(b -> b.type() == BondType.UNKNOWN || b.type() == BondType.COORDINATE)) {
      throw new IllegalArgumentException(
          "Неопределённые и координационные связи требуют отдельного поискового правила.");
    }
    final Map<AtomId, List<Bond>> incident = new HashMap<>();
    molecule.atoms().forEach(a -> incident.put(a.id(), new ArrayList<>()));
    molecule
        .bonds()
        .forEach(
            b -> {
              incident.get(b.firstAtomId()).add(b);
              incident.get(b.secondAtomId()).add(b);
            });
    final Map<AtomId, Atom> core = new HashMap<>();
    molecule.atoms().forEach(a -> core.put(a.id(), a));
    final Set<AtomId> hidden = new HashSet<>();
    final Map<AtomId, Integer> hydrogens = new HashMap<>();
    for (final Atom atom : molecule.atoms()) {
      final var links = incident.get(atom.id());
      if (!suppressHydrogens
          || atom.element().atomicNumber() != 1
          || atom.isotope() != null
          || atom.formalCharge().value() != 0
          || atom.radicalState().radical()
          || links.size() != 1
          || links.getFirst().type() != BondType.SINGLE) {
        continue;
      }
      final Bond bond = links.getFirst();
      final AtomId parent =
          bond.firstAtomId().equals(atom.id()) ? bond.secondAtomId() : bond.firstAtomId();
      if (core.get(parent).element().atomicNumber() != 1) {
        hidden.add(atom.id());
        hydrogens.merge(parent, 1, Integer::sum);
      }
    }
    final IAtomContainer graph = new AtomContainer();
    final Map<AtomId, IAtom> atoms = new HashMap<>();
    for (final Atom atom : molecule.atoms()) {
      if (hidden.contains(atom.id())) {
        continue;
      }
      final IAtom copy = new org.openscience.cdk.silent.Atom(atom.element().symbol().value());
      copy.setID(atom.id().value());
      copy.setFormalCharge(atom.formalCharge().value());
      copy.setImplicitHydrogenCount(hydrogens.getOrDefault(atom.id(), 0));
      copy.setProperty("core.radical", atom.radicalState().unpairedElectrons());
      if (atom.isotope() != null) {
        copy.setMassNumber(atom.isotope().massNumber());
      }
      graph.addAtom(copy);
      atoms.put(atom.id(), copy);
    }
    boolean aromatic = false;
    for (final Bond bond : molecule.bonds()) {
      if (hidden.contains(bond.firstAtomId()) || hidden.contains(bond.secondAtomId())) {
        continue;
      }
      final IBond copy =
          new org.openscience.cdk.silent.Bond(
              atoms.get(bond.firstAtomId()),
              atoms.get(bond.secondAtomId()),
              switch (bond.type()) {
                case DOUBLE -> IBond.Order.DOUBLE;
                case TRIPLE -> IBond.Order.TRIPLE;
                default -> IBond.Order.SINGLE;
              });
      if (bond.type() == BondType.AROMATIC) {
        aromatic = true;
        copy.setIsAromatic(true);
        copy.getBegin().setIsAromatic(true);
        copy.getEnd().setIsAromatic(true);
      }
      graph.addBond(copy);
    }
    try {
      if (aromatic) {
        Kekulization.kekulize(graph);
      }
      Cycles.markRingAtomsAndBonds(graph);
      if (!Aromaticity.apply(Aromaticity.Model.Daylight, graph)) {
        throw new IllegalArgumentException(
            "Ароматичность сложной кольцевой системы не определена полностью; структура пропущена.");
      }
    } catch (final org.openscience.cdk.exception.CDKException error) {
      throw new IllegalArgumentException(
          "Не удалось нормализовать ароматические связи: " + error.getMessage(), error);
    }
    return new Projection(molecule, graph);
  }

  private static IllegalArgumentException unresolved(final String reason) {
    return new IllegalArgumentException("Конфигурация не сопоставлена: " + reason);
  }

  private static void supportedStereo(final Molecule molecule) {
    if (MolecularRelativeStereo.present(molecule)) {
      throw unresolved(
          "относительные / рацемические группы и неизвестная абсолютная конфигурация требуют отдельного правила сопоставления. Отключите стереофильтр только для поиска по графу.");
    }
    if (molecule.metadata().attributes().containsKey(Stereochemistry.ORDERED_PARITY_ATTRIBUTE)) {
      throw unresolved(
          "импортированная чётность требует полного рисунка и проверки CIP. Отключите стереофильтр только для поиска по графу.");
    }
    if (molecule.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)) {
      throw unresolved(
          "стереохимия помечена ядром для проверки. Подтвердите её в исходном опыте либо явно отключите стереофильтр.");
    }
    for (final var center : molecule.stereochemistry().centers()) {
      if (!Set.of(
              StereochemicalDescriptor.R,
              StereochemicalDescriptor.S,
              StereochemicalDescriptor.E,
              StereochemicalDescriptor.Z)
          .contains(center.descriptor())) {
        throw unresolved(
            "дескриптор "
                + center.descriptor()
                + " не подтверждает поддержанную абсолютную R/S или E/Z-конфигурацию. Отключите стереофильтр только для поиска по графу.");
      }
    }
  }

  private static boolean stereo(
      final Projection query, final Projection target, final int[] mapping, final boolean exact) {
    supportedStereo(target.molecule());
    final var q = query.molecule().stereochemistry().centers();
    final var t = target.molecule().stereochemistry().centers();
    if (exact && q.size() != t.size()) {
      return false;
    }
    final Map<AtomId, AtomId> mapped = new HashMap<>();
    for (int i = 0; i < mapping.length; i++) {
      mapped.put(
          AtomId.of(query.graph().getAtom(i).getID()),
          AtomId.of(target.graph().getAtom(mapping[i]).getID()));
    }
    for (final var center : q) {
      final AtomId first = mapped.get(center.primaryAtomId()),
          second = center.secondaryAtomId() == null ? null : mapped.get(center.secondaryAtomId());
      if (first == null || center.secondaryAtomId() != null && second == null) {
        return false;
      }
      if (t.stream()
          .noneMatch(
              c ->
                  c.kind() == center.kind()
                      && c.descriptor() == center.descriptor()
                      && (c.primaryAtomId().equals(first)
                              && Objects.equals(c.secondaryAtomId(), second)
                          || second != null
                              && c.primaryAtomId().equals(second)
                              && Objects.equals(c.secondaryAtomId(), first)))) {
        return false;
      }
    }
    return true;
  }
}
