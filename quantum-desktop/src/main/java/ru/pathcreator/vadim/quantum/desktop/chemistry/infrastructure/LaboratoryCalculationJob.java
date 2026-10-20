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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.pathcreator.vadim.quantum.chemistry.api.QuantumChemistry;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalCoverageItemRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;

/** Executes an immutable molecular snapshot through the public chemistry facade and retains its provenance. */
public final class LaboratoryCalculationJob {
  public record Result(
      Path file, String status, String report, Molecule geometry, boolean geometryConverged) {}

  private final String id = "calc_" + UUID.randomUUID();
  private final Instant started = Instant.now();
  private final Molecule molecule;
  private final String experimentId;
  private final String componentId;
  private final ClassicalCalculationKind kind;
  private final int geometryIterations;
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private volatile Thread thread;

  public LaboratoryCalculationJob(
      final Molecule molecule,
      final String experimentId,
      final String componentId,
      final ClassicalCalculationKind kind) {
    this(molecule, experimentId, componentId, kind, 128);
  }

  public LaboratoryCalculationJob(
      final Molecule molecule,
      final String experimentId,
      final String componentId,
      final ClassicalCalculationKind kind,
      final int geometryIterations) {
    if (geometryIterations < 1 || geometryIterations > 4096) {
      throw new IllegalArgumentException("Число итераций релаксации должно быть от 1 до 4096.");
    }
    this.geometryIterations = geometryIterations;
    if (kind != ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
        && kind != ClassicalCalculationKind.SINGLE_POINT_ENERGY
        && kind != ClassicalCalculationKind.GEOMETRY_OPTIMIZATION) {
      throw new IllegalArgumentException("Выберите поддержанную команду расчёта.");
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION) {
      if (molecule.atoms().size() > 128) {
        throw new IllegalArgumentException(
            "Локальная релаксация в Studio поддерживает до 128 явных атомов.");
      }
      if (!molecule.stereochemistry().empty() || !molecule.conformation().empty()) {
        throw new IllegalArgumentException(
            "Приближённая релаксация не учитывает заданные стереоцентры и торсионные ограничения.");
      }
    }
    if (kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY
        || kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION) {
      if (kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY && molecule.atoms().size() > 24) {
        throw new IllegalArgumentException(
            "Эта команда Studio рассчитана на молекулы до 24 явно заданных атомов.");
      }
      if (molecule.atoms().stream().anyMatch(atom -> atom.coordinate() == null)) {
        throw new IllegalArgumentException("Для расчёта нужны 3D-координаты всех атомов.");
      }
    }
    this.molecule = molecule;
    this.experimentId = experimentId;
    this.componentId = componentId;
    this.kind = kind;
  }

  public synchronized void cancel() {
    this.cancelled.set(true);
    final Thread running = this.thread;
    if (running != null) {
      running.interrupt();
    }
  }

  public Result execute(final Path directory) throws IOException {
    try {
      this.store(directory, this.document("RUNNING").build());
      synchronized (this) {
        this.thread = Thread.currentThread();
      }
      final var request =
          ClassicalCalculationRequest.of(
              ClassicalCalculationId.of(this.id),
              this.molecule,
              ElectronicStructureMethod.HARTREE_FOCK,
              BasisSet.STO_3G,
              null,
              List.of(this.kind),
              List.of(
                  ClassicalCalculationOption.text("single_point_backend", "java_native"),
                  ClassicalCalculationOption.integer("rhf_max_iterations", 100),
                  ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field"),
                  ClassicalCalculationOption.integer(
                      "geometry_max_iterations", this.geometryIterations)),
              ChemistryMetadata.EMPTY);
      ClassicalCalculationResult result = null;
      String failure = null;
      try {
        if (!this.cancelled.get()) {
          result = QuantumChemistry.runClassicalCalculation(request);
        }
      } catch (final RuntimeException exception) {
        failure = exception.toString();
      }
      // The kernel may finish its current step after cancellation. Never publish that result as a
      // completed calculation.
      final boolean wasCancelled;
      synchronized (this) {
        this.thread = null;
        Thread.interrupted();
        wasCancelled = this.cancelled.get();
      }
      final String status =
          wasCancelled ? "CANCELLED" : result == null ? "FAILED" : result.status().name();
      final var document =
          this.document(status).metadata("calculation.finished", Instant.now().toString());
      final StringBuilder report =
          new StringBuilder("Статус: ")
              .append(status)
              .append("\n")
              .append("Задача: ")
              .append(this.kind)
              .append("\n")
              .append(
                  this.kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
                      ? "Метод: анализ явного молекулярного графа\n"
                      : this.kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
                          ? "Метод: приближённая связевая силовая модель; BFGS; до "
                              + this.geometryIterations
                              + " итераций; энергия в условных единицах\n"
                          : "Метод: HF; базис: STO-3G; backend: Java native\n")
              .append("Вход: ")
              .append(this.molecule.displayName())
              .append(" · ")
              .append(this.molecule.atoms().size())
              .append(" явных атомов\n\n");
      Molecule geometry = null;
      boolean converged = false;
      if (failure != null) {
        report.append(failure).append('\n');
        document.metadata("calculation.failure", failure);
      }
      if (result != null && !wasCancelled) {
        this.addResult(document, result);
        report.append("Покрытие: ").append(result.coverageReport().status()).append("\n");
        result
            .diagnostics()
            .diagnostics()
            .forEach(
                diagnostic ->
                    report
                        .append(diagnostic.severity())
                        .append(" · ")
                        .append(diagnostic.code())
                        .append(" · ")
                        .append(diagnostic.message())
                        .append('\n'));
        for (final var section : result.sections()) {
          report.append("\n").append(section.title()).append("\n");
          for (final var value : section.values()) {
            report
                .append(value.name())
                .append(" = ")
                .append(
                    String.join(value.kind().name().equals("TABLE") ? "\n" : ", ", value.values()))
                .append(" ")
                .append(value.unit())
                .append("\n");
          }
        }
      }
      if (result != null
          && !wasCancelled
          && this.kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION) {
        try {
          final Molecule optimized = QuantumChemistry.optimizedGeometry(this.molecule, result);
          if (optimized != null) {
            converged =
                result.sections().stream()
                    .filter(section -> section.kind() == this.kind)
                    .flatMap(section -> section.values().stream())
                    .anyMatch(
                        value ->
                            value.name().equals("converged")
                                && value.values().equals(List.of("true")));
            geometry =
                LaboratoryComputationFiles.geometrySnapshot(
                    optimized, this.id + "_geometry", "BONDED_FORCE_FIELD", converged);
            document
                .molecule(geometry)
                .metadata("calculation.output_molecule", geometry.id().value())
                .metadata("calculation.geometry_converged", Boolean.toString(converged));
            report
                .append("\nСходимость геометрии: ")
                .append(converged ? "достигнута" : "НЕ достигнута; доступна последняя итерация")
                .append("\n");
          }
        } catch (final RuntimeException exception) {
          document.metadata("calculation.geometry_error", exception.toString());
          report.append("\nГеометрия не применима: ").append(exception.getMessage());
        }
      }
      document.metadata("calculation.report", report.toString());
      return new Result(
          this.store(directory, document.build()), status, report.toString(), geometry, converged);
    } finally {
      this.thread = null;
    }
  }

  private ChemistryStorageDocument.Builder document(final String status) {
    return ChemistryStorageDocument.builder(this.id, "Расчёт " + this.molecule.displayName())
        .molecule(this.molecule)
        .metadata("calculation.input_molecule", this.molecule.id().value())
        .metadata("calculation.experiment", this.experimentId)
        .metadata("calculation.component", this.componentId)
        .metadata("calculation.kind", this.kind.name())
        .metadata(
            "calculation.method",
            this.kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
                ? "BONDED_FORCE_FIELD"
                : this.kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
                    ? "GRAPH_ANALYSIS"
                    : "HARTREE_FOCK")
        .metadata(
            "calculation.basis",
            this.kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY ? "STO-3G" : "not_applicable")
        .metadata("calculation.geometry_energy_model", "bonded_force_field")
        .metadata("calculation.geometry_max_iterations", Integer.toString(this.geometryIterations))
        .metadata("calculation.backend", "java_native")
        .metadata("calculation.rhf_max_iterations", "100")
        .metadata("calculation.started", this.started.toString())
        .metadata("calculation.status", status)
        .metadata(
            "calculation.application_version",
            java.util.Objects.requireNonNullElse(
                LaboratoryCalculationJob.class.getPackage().getImplementationVersion(),
                "development"));
  }

  private void addResult(
      final ChemistryStorageDocument.Builder document, final ClassicalCalculationResult result) {
    final var diagnostics = result.diagnostics().diagnostics();
    document.classicalResultRecord(
        ChemistryStorageClassicalResultRecord.of(
            this.id,
            this.id,
            0,
            result.status().name(),
            diagnostics.size(),
            (int) diagnostics.stream().filter(item -> item.error()).count(),
            (int)
                diagnostics.stream()
                    .filter(item -> item.severity().name().equals("WARNING"))
                    .count(),
            result.coverageReport().status().name(),
            result.coverageReport().items().stream()
                .map(
                    item ->
                        ChemistryStorageClassicalCoverageItemRecord.of(
                            item.kind().name(), item.status().name(), item.model()))
                .toList(),
            result.sections().stream()
                .map(
                    section ->
                        ChemistryStorageClassicalResultSectionRecord.of(
                            section.kind().name(),
                            section.title(),
                            section.values().stream()
                                .map(
                                    value ->
                                        ChemistryStorageClassicalResultValueRecord.of(
                                            value.name(),
                                            value.kind().name(),
                                            value.unit(),
                                            value.values()))
                                .toList()))
                .toList(),
            "Расчёт по сохранённому входному снимку; не экспериментальный результат."));
    for (int index = 0; index < diagnostics.size(); index++) {
      final var diagnostic = diagnostics.get(index);
      document.extension(
          ChemistryStorageExtension.of(
              "laboratory-calculation-diagnostic-v1",
              "diagnostic_" + index,
              Map.of(
                  "severity",
                  diagnostic.severity().name(),
                  "code",
                  diagnostic.code().name(),
                  "message",
                  diagnostic.message(),
                  "target",
                  String.valueOf(diagnostic.target())),
              List.of()));
    }
  }

  private Path store(final Path directory, final ChemistryStorageDocument document)
      throws IOException {
    return LaboratoryComputationFiles.write(directory, this.id, document);
  }
}
