/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.util.*;

/** Cross-sample lineage and transfer invariants are enforced again when a QCHEM file is read. */
public final class LaboratorySampleLedger {
  private LaboratorySampleLedger() {}

  public static void validate(final List<LaboratorySample> samples) {
    if (samples.size() > 5000) {
      throw new IllegalArgumentException("Не более 5000 образцов в одном опыте.");
    }
    final Map<String, LaboratorySample> known = new LinkedHashMap<>();
    final Set<String> codes = new HashSet<>(), events = new HashSet<>();
    for (final var sample : samples) {
      if (known.containsKey(sample.id()) || !codes.add(sample.code().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Код или ID образца уже существует: " + sample.code());
      }
      for (final String id : sample.parents()) {
        final var parent = known.get(id);
        if (parent == null
            || parent.createdAt().isAfter(sample.createdAt())
            || !parent.componentId().equals(sample.componentId())
            || !LaboratoryMaterialProperties.signature(parent.molecule())
                .equals(LaboratoryMaterialProperties.signature(sample.molecule()))) {
          throw new IllegalArgumentException(
              "Исходный образец отсутствует, создан позже или связан с другой структурой.");
        }
      }
      for (final var e : sample.events()) {
        if (!events.add(e.id())) {
          throw new IllegalArgumentException("Повтор события образца.");
        }
      }
      known.put(sample.id(), sample);
    }
    final Map<String, Map<String, LaboratoryQuantity>> transfers = new HashMap<>();
    for (final var parent : samples) {
      for (final var e : parent.events()) {
        if (e.relatedSampleId() != null) {
          final var child = known.get(e.relatedSampleId());
          if (child == null
              || !child.parents().contains(parent.id())
              || child.kind() != LaboratorySample.Kind.ALIQUOT
                  && child.kind() != LaboratorySample.Kind.POOLED
              || !e.time().equals(child.createdAt())) {
            throw new IllegalArgumentException(
                "Передача должна ссылаться на одновременно созданную аликвоту или объединённый образец.");
          }
          if (transfers
                  .computeIfAbsent(child.id(), k -> new HashMap<>())
                  .put(parent.id(), e.quantity())
              != null) {
            throw new IllegalArgumentException("Повтор передачи из исходного образца.");
          }
        }
      }
    }
    for (final var child : samples) {
      if (child.kind() == LaboratorySample.Kind.ALIQUOT
          || child.kind() == LaboratorySample.Kind.POOLED) {
        final var amount = child.events().getFirst().quantity();
        final var sources = transfers.getOrDefault(child.id(), Map.of());
        if (amount == null || !sources.keySet().equals(Set.copyOf(child.parents()))) {
          throw new IllegalArgumentException(
              "Для аликвоты или объединения нужны связанные расходы всех исходных образцов.");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (final var q : sources.values()) {
          sum = sum.add(q.in(amount.unit()).value());
        }
        if (sum.compareTo(amount.value()) != 0) {
          throw new IllegalArgumentException(
              "Сумма переносов не совпадает с количеством нового образца.");
        }
      }
    }
  }

  public static void requireAppend(
      final List<LaboratorySample> previous, final List<LaboratorySample> next) {
    validate(next);
    final Map<String, LaboratorySample> map = new HashMap<>();
    next.forEach(s -> map.put(s.id(), s));
    for (final var old : previous) {
      final var now = map.get(old.id());
      if (now == null
          || now.events().size() < old.events().size()
          || !now.events().subList(0, old.events().size()).equals(old.events())
          || !new LaboratorySample(
                  now.id(),
                  now.code(),
                  now.componentId(),
                  now.stageId(),
                  now.stageTitle(),
                  now.kind(),
                  now.molecule(),
                  now.parents(),
                  old.events(),
                  now.origin())
              .equals(old)) {
        throw new IllegalArgumentException(
            "Историю образца нельзя удалить или переписать. Уточните остаток, место или статус новой операцией.");
      }
    }
  }
}
