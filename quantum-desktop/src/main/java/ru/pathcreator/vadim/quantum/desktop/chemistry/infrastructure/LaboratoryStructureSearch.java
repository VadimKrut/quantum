/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStructureSearch;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.search.CdkMoleculeStructureSearch;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Searches saved project snapshots once per component, without mutating source documents. */
public final class LaboratoryStructureSearch {
  public record Hit(
      Path path,
      LaboratoryDrawing drawing,
      LaboratoryComponent component,
      List<LaboratoryProjectOverview.Row> stages,
      MoleculeStructureSearch.Match match) {
    public Hit {
      stages = List.copyOf(stages);
    }

    public String key() {
      return this.path + "#" + this.component.id();
    }
  }

  public record Result(List<Hit> hits, List<String> diagnostics, int documents, int examined) {
    public Result {
      hits = List.copyOf(hits);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  public Result search(
      final Path folder,
      final Molecule query,
      final MoleculeStructureSearch.Options options,
      final ReactionComponentRole role,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    return this.search(folder, query, Set.of(), options, role, cancelled, progress);
  }

  public Result search(
      final Path folder,
      final Molecule query,
      final Set<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId> selected,
      final MoleculeStructureSearch.Options options,
      final ReactionComponentRole role,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    return this.search(
        folder,
        new CdkMoleculeStructureSearch().compile(query, selected, options, cancelled),
        role,
        cancelled,
        progress);
  }

  public Result search(
      final Path folder,
      final MoleculeDrawing query,
      final Set<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId> selected,
      final MoleculeStructureSearch.Options options,
      final ReactionComponentRole role,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    return this.search(
        folder,
        new CdkMoleculeStructureSearch().compile(query, selected, options, cancelled),
        role,
        cancelled,
        progress);
  }

  private Result search(
      final Path folder,
      final MoleculeStructureSearch.Query pattern,
      final ReactionComponentRole role,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    check(cancelled);
    progress.accept("Читаю сохранённые опыты…");
    final var snapshot = new LaboratoryProjectOverview().scan(folder);
    check(cancelled);
    final Map<Path, List<LaboratoryProjectOverview.Row>> documents = new LinkedHashMap<>();
    snapshot
        .rows()
        .forEach(row -> documents.computeIfAbsent(row.path(), p -> new ArrayList<>()).add(row));
    final List<Hit> hits = new ArrayList<>();
    final List<String> diagnostics = new ArrayList<>(snapshot.diagnostics());
    int examined = 0;
    boolean limited = false;
    outer:
    for (final var entry : documents.entrySet()) {
      final var drawing = entry.getValue().getFirst().drawing();
      for (final var component : drawing.experiment().components()) {
        check(cancelled);
        final var stages =
            entry.getValue().stream()
                .filter(
                    row ->
                        row.stage() == null
                            ? (role == null || component.role() == role)
                            : row.stage().participants().stream()
                                .anyMatch(
                                    p ->
                                        p.componentId().equals(component.id())
                                            && (role == null || p.role() == role)))
                .toList();
        if (role != null && stages.isEmpty()) {
          continue;
        }
        if (++examined > 4000) {
          limited = true;
          examined--;
          break outer;
        }
        if (examined % 8 == 1) {
          progress.accept(
              "Сопоставляю структуру " + examined + " · " + entry.getKey().getFileName());
        }
        try {
          final var match = pattern.match(drawing.layouts().get(component.id()));
          if (match.matched()) {
            hits.add(new Hit(entry.getKey(), drawing, component, stages, match));
          }
        } catch (final CancellationException stop) {
          throw stop;
        } catch (final RuntimeException problem) {
          diagnostics.add(
              entry.getKey().getFileName()
                  + " · "
                  + component.molecule().displayName()
                  + ": "
                  + problem.getMessage());
        }
      }
    }
    if (limited) {
      diagnostics.add(
          "Поиск ограничен 4000 компонентами. Выберите более узкую папку; отсутствие результата за пределом поиска неизвестно.");
    }
    hits.sort(
        Comparator.<Hit>comparingDouble(h -> h.match().score())
            .reversed()
            .thenComparing(
                h -> h.component().molecule().displayName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Hit::key));
    check(cancelled);
    return new Result(hits, diagnostics, documents.size(), examined);
  }

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Поиск остановлен.");
    }
  }
}
