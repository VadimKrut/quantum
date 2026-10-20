/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryConformerSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.*;

/** Coordinate collections use ordinary QCHEM molecule snapshots and a strict versioned view extension. */
public final class LaboratoryConformerCodec {
  private static final String KIND = "laboratory-conformers-v1",
      KIND_V2 = "laboratory-conformers-v2";

  public ChemistryStorageDocument document(final LaboratoryConformerSet set, final String id) {
    final boolean enhanced =
        set.frames().stream().anyMatch(f -> f.energy() != null)
            || !set.referenceId().equals(set.frames().getFirst().geometry().id().value());
    final var lines = new ArrayList<String>();
    for (final var frame : set.frames()) {
      final var v = frame.view();
      String line =
          String.join(
              "\t",
              frame.geometry().id().value(),
              encode(frame.title()),
              encode(frame.method()),
              Double.toString(v.yaw()),
              Double.toString(v.pitch()),
              Double.toString(v.zoom()),
              Double.toString(v.panX()),
              Double.toString(v.panY()),
              encode(v.representation()),
              Boolean.toString(v.labels()));
      if (enhanced) {
        final var energy = frame.energy();
        line +=
            "\t"
                + (energy == null
                    ? "\t\t"
                    : Double.toString(energy.kcalPerMol())
                        + "\t"
                        + encode(energy.forceField())
                        + "\t"
                        + encode(energy.implementation()));
      }
      lines.add(line);
    }
    final var properties = new LinkedHashMap<String, String>();
    properties.put("schema", enhanced ? "2" : "1");
    properties.put("experiment", set.experimentId());
    properties.put("component", set.componentId());
    properties.put("source", set.source().id().value());
    if (enhanced) {
      properties.put("reference", set.referenceId());
    }
    final var extension =
        ChemistryStorageExtension.of(enhanced ? KIND_V2 : KIND, id, properties, lines);
    final var builder =
        ChemistryStorageDocument.builder(id, "Конформеры · " + set.source().displayName())
            .molecule(set.source())
            .extension(extension);
    set.frames().forEach(f -> builder.molecule(f.geometry()));
    return builder.build();
  }

  public Path save(final Path directory, final LaboratoryConformerSet set)
      throws java.io.IOException {
    final String id = "conformers_" + UUID.randomUUID();
    return LaboratoryComputationFiles.write(directory, id, this.document(set, id));
  }

  public LaboratoryConformerSet read(final Path path) throws java.io.IOException {
    if (Files.size(path) > 32 * 1024 * 1024) {
      throw new java.io.IOException("Набор конформеров превышает 32 МиБ.");
    }
    return this.read(Files.readString(path));
  }

  public LaboratoryConformerSet read(final String text) {
    if (text.length() > 32 * 1024 * 1024) {
      throw new IllegalArgumentException("Слишком большой набор конформеров.");
    }
    final var decoded = new ChemistryStorageService().read(text);
    if (!decoded.success()) {
      throw new IllegalArgumentException(
          "Не удалось прочитать конформеры: " + decoded.diagnostics());
    }
    final var d = decoded.value();
    if (d.extensions().size() != 1
        || !Set.of(KIND, KIND_V2).contains(d.extensions().getFirst().kind())) {
      throw new IllegalArgumentException("Файл не содержит сохранённый набор конформеров.");
    }
    final var extension = d.extensions().getFirst();
    final var p = extension.properties();
    final boolean enhanced = extension.kind().equals(KIND_V2);
    final var keys =
        enhanced
            ? Set.of("schema", "experiment", "component", "source", "reference")
            : Set.of("schema", "experiment", "component", "source");
    if (!p.keySet().equals(keys)
        || !(enhanced ? "2" : "1").equals(p.get("schema"))
        || !extension.id().equals(d.projectId())) {
      throw new IllegalArgumentException("Неподдержанные поля набора конформеров.");
    }
    if (extension.bodyLines().isEmpty() || extension.bodyLines().size() > 128) {
      throw new IllegalArgumentException("Неподдержанное количество конформеров.");
    }
    final Map<String, Molecule> molecules = new HashMap<>();
    d.molecules().forEach(m -> molecules.put(m.id().value(), m));
    final List<LaboratoryConformerSet.Frame> frames = new ArrayList<>();
    for (final String line : extension.bodyLines()) {
      final String[] f = line.split("\t", -1);
      if (f.length != (enhanced ? 13 : 10) || !Set.of("true", "false").contains(f[9])) {
        throw new IllegalArgumentException("Повреждена запись конформера.");
      }
      final var view =
          new LaboratoryConformerSet.View(
              Double.parseDouble(f[3]),
              Double.parseDouble(f[4]),
              Double.parseDouble(f[5]),
              Double.parseDouble(f[6]),
              Double.parseDouble(f[7]),
              decode(f[8]),
              Boolean.parseBoolean(f[9]));
      LaboratoryConformerSet.Energy energy = null;
      if (enhanced) {
        if (f[10].isEmpty()) {
          if (!f[11].isEmpty() || !f[12].isEmpty()) {
            throw new IllegalArgumentException("Метод энергии указан без расчётного значения.");
          }
        } else {
          energy =
              new LaboratoryConformerSet.Energy(
                  Double.parseDouble(f[10]), decode(f[11]), decode(f[12]));
        }
      }
      frames.add(
          new LaboratoryConformerSet.Frame(
              decode(f[1]),
              decode(f[2]),
              Objects.requireNonNull(molecules.get(f[0]), "Отсутствует геометрия конформера"),
              view,
              energy));
    }
    final var set =
        new LaboratoryConformerSet(
            p.get("experiment"),
            p.get("component"),
            Objects.requireNonNull(
                molecules.get(p.get("source")), "Отсутствует исходная структура"),
            frames,
            enhanced ? p.get("reference") : null);
    if (!this.document(set, d.projectId()).equals(d)) {
      throw new IllegalArgumentException(
          "Набор содержит неизвестные данные. Они не будут удалены при сохранении.");
    }
    return set;
  }

  private static String encode(final String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(final String value) {
    return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
  }
}
