/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.stereo;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;

/** Derived CIP labels retain their input fingerprint and provenance independently of authored stereo declarations. */
public record MolecularCipLabels(String inputSignature, String method, List<Label> labels) {
  private static final String KEY = "laboratory.cip.v1.";

  public record Label(AtomId first, AtomId second, String descriptor) {
    public Label {
      Objects.requireNonNull(first);
      if (!Set.of("R", "S", "r", "s", "E", "Z", "M", "P", "m", "p", "seqCis", "seqTrans")
          .contains(descriptor)) {
        throw new IllegalArgumentException("Неподдержанный дескриптор CIP.");
      }
    }
  }

  public MolecularCipLabels {
    if (inputSignature == null || !inputSignature.matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("Отсутствует подпись входа расчёта CIP.");
    }
    if (method == null || method.isBlank() || method.length() > 1000) {
      throw new IllegalArgumentException("Укажите источник расчёта CIP.");
    }
    labels = List.copyOf(labels);
    if (labels.size() > 1000) {
      throw new IllegalArgumentException("Слишком много дескрипторов CIP.");
    }
  }

  public Molecule apply(final Molecule m) {
    final var ids = m.atoms().stream().map(Atom::id).collect(java.util.stream.Collectors.toSet());
    for (final var label : this.labels) {
      if (!ids.contains(label.first()) || label.second() != null && !ids.contains(label.second())) {
        throw new IllegalArgumentException("Метка CIP относится к отсутствующему атому.");
      }
    }
    final Map<String, String> attributes = new LinkedHashMap<>(m.metadata().attributes());
    attributes.keySet().removeIf(k -> k.startsWith(KEY));
    attributes.put(KEY + "signature", this.inputSignature);
    attributes.put(KEY + "method", this.method);
    attributes.put(KEY + "count", Integer.toString(this.labels.size()));
    for (int i = 0; i < this.labels.size(); i++) {
      final var label = this.labels.get(i);
      attributes.put(
          KEY + "label." + i,
          label.first().value()
              + ":"
              + (label.second() == null ? "" : label.second().value())
              + ":"
              + label.descriptor());
    }
    return Molecule.of(
        m.id(),
        m.displayName(),
        m.atoms(),
        m.bonds(),
        m.charge(),
        m.spinMultiplicity(),
        m.stereochemistry(),
        m.conformation(),
        m.opticalRotation(),
        m.symmetry(),
        ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), attributes));
  }

  public static Optional<MolecularCipLabels> read(final Molecule m) {
    final var p = m.metadata().attributes();
    if (!p.containsKey(KEY + "signature")) {
      return Optional.empty();
    }
    final int count = Integer.parseInt(p.get(KEY + "count"));
    if (count < 0 || count > 1000) {
      throw new IllegalArgumentException("Повреждён список меток CIP.");
    }
    final List<Label> labels = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final String[] fields = Objects.requireNonNull(p.get(KEY + "label." + i)).split(":", -1);
      if (fields.length != 3) {
        throw new IllegalArgumentException("Повреждена метка CIP.");
      }
      labels.add(
          new Label(
              AtomId.of(fields[0]), fields[1].isEmpty() ? null : AtomId.of(fields[1]), fields[2]));
    }
    return Optional.of(
        new MolecularCipLabels(p.get(KEY + "signature"), p.get(KEY + "method"), labels));
  }
}
