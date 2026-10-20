/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Bounded, independently stereo-verified coordinates; no OCL chemical model replaces the source graph. */
public final class OclConformerGenerator {
  public static final String IMPLEMENTATION = "OpenChemLib 2026.10.1", FORCE_FIELD = "MMFF94s+";
  public static final int MAX_CANDIDATES = 64, MAX_ATOMS = 128;
  public static final long DEFAULT_TIMEOUT_MILLIS = 60000, MAX_TIMEOUT_MILLIS = 120000;

  public record Candidate(Molecule geometry, double energyKcalMol, String method) {
    public Candidate {
      Objects.requireNonNull(geometry);
      Objects.requireNonNull(method);
      if (!Double.isFinite(energyKcalMol)) {
        throw new IllegalArgumentException("Энергия конформера не определена.");
      }
    }
  }

  public record Result(
      List<Candidate> candidates, List<String> diagnostics, int requested, int attempted) {
    public Result {
      candidates = List.copyOf(candidates);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  private OclConformerGenerator() {}

  public static Result generate(
      final MoleculeDrawing drawing, final int requested, final long seed) {
    return generate(drawing, requested, seed, DEFAULT_TIMEOUT_MILLIS, () -> false);
  }

  /** Cancellation and deadline terminate the child JVM, including unresponsive third-party minimization. */
  public static Result generate(
      final MoleculeDrawing drawing,
      final int requested,
      final long seed,
      final long timeoutMillis,
      final BooleanSupplier cancellation) {
    Objects.requireNonNull(drawing);
    Objects.requireNonNull(cancellation);
    cancelled(cancellation);
    if (requested < 1
        || requested > MAX_CANDIDATES
        || timeoutMillis < 1
        || timeoutMillis > MAX_TIMEOUT_MILLIS) {
      throw new IllegalArgumentException("Поиск: 1–64 конформера и не более 120 секунд.");
    }
    final var molecule = drawing.molecule();
    if (molecule.atomCount() < 2
        || molecule.atomCount() > MAX_ATOMS
        || molecule.bonds().size() > 512) {
      throw new IllegalArgumentException(
          "OpenChemLib: поддерживаются 2–128 исходных атомов и до 512 связей.");
    }
    if (!molecule.conformation().empty()) {
      throw new IllegalArgumentException(
          "MMFF-поиск не поддерживает заданные торсионные ограничения; они не будут молча отброшены.");
    }
    try {
      final var graph = CdkStereochemicalProjection.representative(drawing);
      if (!ConnectivityChecker.isConnected(graph)) {
        throw new IllegalArgumentException(
            "Поиск конформеров выполняется для одной связной структуры. Соли и отдельные фрагменты не удаляются автоматически.");
      }
      for (int i = 0; i < graph.getAtomCount(); i++) {
        final var atom = graph.getAtom(i);
        atom.setMapIdx(i + 1);
        atom.setPoint3d(null);
      }
      graph.setTitle("Quantum mapped conformer input");
      new StructureDiagramGenerator().generateCoordinates(graph);
      cancelled(cancellation);
      for (int i = 0; i < graph.getAtomCount(); i++) {
        if (!molecule.atoms().get(i).id().value().equals(graph.getAtom(i).getID())
            || graph.getAtom(i).getMapIdx() != i + 1) {
          throw new IllegalArgumentException("Изменилась атомная карта при подготовке структуры.");
        }
      }
      final StringWriter text = new StringWriter();
      try (final var writer = new MDLV3000Writer(text)) {
        writer.write(graph);
      }
      final var response =
          OclConformerWorker.generate(
              graph, text.toString(), requested, seed == 0 ? 1 : seed, timeoutMillis, cancellation);
      final List<Candidate> candidates = new ArrayList<>();
      final List<String> diagnostics = new ArrayList<>(response.diagnostics());
      int rejected = 0;
      for (final var candidate : response.candidates()) {
        cancelled(cancellation);
        final List<Atom> atoms = new ArrayList<>();
        for (int i = 0; i < molecule.atomCount(); i++) {
          final var original = molecule.atoms().get(i);
          final double[] p = candidate.coordinates()[i];
          atoms.add(
              Atom.of(
                  original.id(),
                  original.element().symbol(),
                  Coordinate3D.of(p[0], p[1], p[2], LengthUnit.ANGSTROM),
                  original.formalCharge(),
                  original.isotope(),
                  original.radicalState(),
                  original.metadata()));
        }
        final var geometry =
            Molecule.of(
                molecule.id(),
                molecule.displayName(),
                atoms,
                molecule.bonds(),
                molecule.charge(),
                molecule.spinMultiplicity(),
                molecule.stereochemistry(),
                molecule.conformation(),
                molecule.opticalRotation(),
                molecule.symmetry(),
                molecule.metadata());
        try {
          CdkInitialGeometry.verify(drawing, geometry);
        } catch (final IllegalArgumentException invalid) {
          rejected++;
          if (rejected <= 3) {
            diagnostics.add(
                "Кандидат отклонён независимой проверкой геометрии/стереохимии: "
                    + invalid.getMessage());
          }
          continue;
        }
        candidates.add(
            new Candidate(
                geometry,
                candidate.energy(),
                IMPLEMENTATION
                    + "; "
                    + FORCE_FIELD
                    + "; минимизация сошлась; вакуум, ε=1, cutoff 100 Å; seed="
                    + (seed == 0 ? 1 : seed)
                    + ". Виртуальные H не добавлены в исходный граф. "
                    + CdkStereochemicalProjection.annotation(molecule)));
      }
      if (rejected > 3) {
        diagnostics.add(
            "Дополнительно отклонено проверкой геометрии/стереохимии: " + (rejected - 3) + ".");
      }
      candidates.sort(Comparator.comparingDouble(Candidate::energyKcalMol));
      diagnostics.add(
          "Расчётные локальные минимумы MMFF94s+, не измерение и не доказательство глобального минимума. Энергии сопоставимы только для того же состава, протонирования и параметров модели. Незаданные стереоцентры не назначаются исходной структуре.");
      if (candidates.isEmpty()) {
        diagnostics.add(
            "Ни один сошедшийся конформер не прошёл все проверки; исходная структура сохранена без изменений.");
      }
      return new Result(candidates, diagnostics, requested, response.attempted());
    } catch (final CancellationException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          "Поиск конформеров OpenChemLib: "
              + Objects.toString(e.getMessage(), e.getClass().getSimpleName()),
          e);
    }
  }

  private static void cancelled(final BooleanSupplier cancellation) {
    if (Thread.currentThread().isInterrupted() || cancellation.getAsBoolean()) {
      throw new CancellationException("Поиск конформеров отменён.");
    }
  }
}
