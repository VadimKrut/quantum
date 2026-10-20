/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.classical.application.conformation;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryCoordinateSupport;

/** Bounded rigid torsion sampling. Coordinates are candidates, not optimized states or energies. */
public final class ConformerCoordinateEnsemble {
  public record Axis(AtomId first, AtomId second, List<AtomId> moved) {
    public Axis {
      moved = List.copyOf(moved);
    }
  }

  public record Candidate(Molecule geometry, String method) {}

  private ConformerCoordinateEnsemble() {}

  public static List<Axis> axes(final Molecule molecule) {
    check(molecule);
    final var result = new ArrayList<Axis>();
    final Map<AtomId, List<AtomId>> adjacent = new HashMap<>();
    molecule.atoms().forEach(a -> adjacent.put(a.id(), new ArrayList<>()));
    for (final var b : molecule.bonds()) {
      adjacent.get(b.firstAtomId()).add(b.secondAtomId());
      adjacent.get(b.secondAtomId()).add(b.firstAtomId());
    }
    final Map<AtomId, Atom> atoms = new HashMap<>();
    molecule.atoms().forEach(a -> atoms.put(a.id(), a));
    for (final var b : molecule.bonds()) {
      if (b.type() != BondType.SINGLE
          || atoms.get(b.firstAtomId()).element().atomicNumber() == 1
          || atoms.get(b.secondAtomId()).element().atomicNumber() == 1) {
        continue;
      }
      if (adjacent.get(b.firstAtomId()).stream()
              .noneMatch(
                  id -> !id.equals(b.secondAtomId()) && atoms.get(id).element().atomicNumber() != 1)
          || adjacent.get(b.secondAtomId()).stream()
              .noneMatch(
                  id ->
                      !id.equals(b.firstAtomId()) && atoms.get(id).element().atomicNumber() != 1)) {
        continue;
      }
      final Set<AtomId> moved = new LinkedHashSet<>();
      final ArrayDeque<AtomId> queue = new ArrayDeque<>();
      queue.add(b.secondAtomId());
      while (!queue.isEmpty()) {
        final AtomId id = queue.remove();
        if (!moved.add(id)) {
          continue;
        }
        for (final var next : adjacent.get(id)) {
          if (id.equals(b.secondAtomId()) && next.equals(b.firstAtomId())
              || id.equals(b.firstAtomId()) && next.equals(b.secondAtomId())) {
            continue;
          }
          queue.add(next);
        }
      }
      if (!moved.contains(b.firstAtomId())) {
        result.add(new Axis(b.firstAtomId(), b.secondAtomId(), List.copyOf(moved)));
      }
    }
    return List.copyOf(result);
  }

  public static Molecule rotate(final Molecule source, final Axis axis, final double degrees) {
    check(source);
    if (!Double.isFinite(degrees) || Math.abs(degrees) > 360) {
      throw new IllegalArgumentException("Угол поворота должен быть от −360 до 360°.");
    }
    if (!axes(source).contains(axis)) {
      throw new IllegalArgumentException(
          "Ось не является поддержанной одиночной связью вне кольца.");
    }
    final Map<AtomId, Integer> index = new HashMap<>();
    for (int i = 0; i < source.atomCount(); i++) {
      index.put(source.atoms().get(i).id(), i * 3);
    }
    final double[] xyz = GeometryCoordinateSupport.coordinatesAngstrom(source);
    final int a = index.get(axis.first()), b = index.get(axis.second());
    final double ox = xyz[a], oy = xyz[a + 1], oz = xyz[a + 2];
    double ux = xyz[b] - ox, uy = xyz[b + 1] - oy, uz = xyz[b + 2] - oz;
    final double length = Math.sqrt(ux * ux + uy * uy + uz * uz);
    if (length < 1e-8) {
      throw new IllegalArgumentException("Атомы оси совпадают в исходной геометрии.");
    }
    ux /= length;
    uy /= length;
    uz /= length;
    final double angle = Math.toRadians(degrees), c = Math.cos(angle), s = Math.sin(angle);
    for (final var id : axis.moved()) {
      final int i = index.get(id);
      final double x = xyz[i] - ox,
          y = xyz[i + 1] - oy,
          z = xyz[i + 2] - oz,
          dot = ux * x + uy * y + uz * z;
      xyz[i] = ox + x * c + (uy * z - uz * y) * s + ux * dot * (1 - c);
      xyz[i + 1] = oy + y * c + (uz * x - ux * z) * s + uy * dot * (1 - c);
      xyz[i + 2] = oz + z * c + (ux * y - uy * x) * s + uz * dot * (1 - c);
    }
    return GeometryCoordinateSupport.withCoordinatesAngstrom(source, xyz);
  }

  public static List<Candidate> generate(
      final Molecule source, final int requested, final long seed) {
    if (requested < 1 || requested > 64) {
      throw new IllegalArgumentException("Выберите от 1 до 64 кандидатов.");
    }
    final List<Axis> axes = axes(source);
    final var result = new ArrayList<Candidate>();
    result.add(new Candidate(source, "Исходная геометрия; без оптимизации"));
    if (axes.isEmpty()) {
      return result;
    }
    final Random random = new Random(seed);
    final Set<String> choices = new HashSet<>();
    choices.add("0".repeat(axes.size()));
    for (int attempt = 0; result.size() < requested && attempt < requested * 30; attempt++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException();
      }
      final int[] positions = new int[axes.size()];
      final StringBuilder key = new StringBuilder();
      for (int i = 0; i < positions.length; i++) {
        positions[i] = random.nextInt(6);
        key.append(positions[i]);
      }
      if (!choices.add(key.toString())) {
        continue;
      }
      Molecule current = source;
      for (int i = 0; i < axes.size(); i++) {
        if (positions[i] != 0) {
          current = rotate(current, axes.get(i), positions[i] * 60.0);
        }
      }
      result.add(
          new Candidate(
              current,
              "Повороты вне кольца с шагом 60°; seed="
                  + seed
                  + "; сетка="
                  + key
                  + "; без оптимизации и ранжирования по энергии"));
    }
    return List.copyOf(result);
  }

  private static void check(final Molecule source) {
    Objects.requireNonNull(source);
    if (source.atomCount() < 2
        || source.atomCount() > 256
        || source.atoms().stream().anyMatch(a -> a.coordinate() == null)) {
      throw new IllegalArgumentException(
          "Для работы с конформерами нужны полные координаты 2–256 явных атомов.");
    }
  }
}
