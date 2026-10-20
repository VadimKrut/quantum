/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Analytical notation and diagnostics are shared by the UI and report generation. */
public final class LaboratoryAnalysisSummary {
  private LaboratoryAnalysisSummary() {}

  public static Molecule snapshot(final Molecule m, final String id) {
    return Molecule.of(
        ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId.of("analysis_" + id),
        m.displayName(),
        m.atoms(),
        m.bonds(),
        m.charge(),
        m.spinMultiplicity(),
        m.stereochemistry(),
        m.conformation(),
        m.opticalRotation(),
        m.symmetry(),
        m.metadata());
  }

  public static String number(final BigDecimal n) {
    return n == null ? "—" : n.stripTrailingZeros().toPlainString();
  }

  public static String text(final LaboratoryAnalysis a) {
    return text(a, true);
  }

  public static String concise(final LaboratoryAnalysis a) {
    return text(a, false);
  }

  private static String text(final LaboratoryAnalysis a, final boolean provenance) {
    final String content =
        switch (a.data()) {
          case Nmr n ->
              n.nucleus()
                  + " NMR ("
                  + (n.frequencyMHz() == null
                      ? "частота не указана"
                      : number(n.frequencyMHz()) + " MHz")
                  + ", "
                  + (n.solvent().isBlank() ? "растворитель не указан" : n.solvent())
                  + ") δ "
                  + String.join(
                      ", ",
                      n.signals().stream()
                          .sorted(Comparator.comparing(Signal::from).reversed())
                          .map(s -> signalText(s, n.integralScale()))
                          .toList());
          case Mass m ->
              (m.technique().isBlank() ? "MS" : m.technique())
                  + " ("
                  + m.ion()
                  + ") "
                  + m.ionFormula()
                  + "; calcd m/z "
                  + number(m.calculatedMz())
                  + ", found "
                  + number(m.foundMz())
                  + (m.ppmError() == null
                      ? ""
                      : "; Δ "
                          + m.ppmError().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                          + " ppm");
          case Tlc t ->
              "TLC ("
                  + t.stationary()
                  + ", "
                  + t.eluent()
                  + "; "
                  + t.visualization()
                  + "): "
                  + String.join(
                      "; ",
                      t.spots().stream()
                          .map(
                              s ->
                                  s.label()
                                      + " Rf = "
                                      + (t.rf(s) == null
                                          ? "—"
                                          : t.rf(s)
                                              .setScale(3, java.math.RoundingMode.HALF_UP)
                                              .stripTrailingZeros()
                                              .toPlainString()))
                          .toList());
        };
    return a.title()
        + " · "
        + a.origin()
        + "\n"
        + a.molecule().displayName()
        + " · "
        + a.molecule().formula().hillNotation()
        + "\n"
        + content
        + (provenance && a.data() instanceof Nmr n && n.trace() != null
            ? "\n" + traceText(n.trace())
            : "")
        + (a.sample().isBlank() ? "" : "\nОбразец: " + a.sample())
        + (a.instrument().isBlank() ? "" : "\nПрибор: " + a.instrument())
        + (a.source().isBlank() ? "" : "\nИсточник: " + a.source())
        + (a.note().isBlank() ? "" : "\n" + a.note());
  }

  private static String signalText(final Signal s, final BigDecimal scale) {
    final List<String> details = new ArrayList<>();
    if (!s.multiplicity().isBlank()) details.add(s.multiplicity());
    if (!s.couplingsHz().isEmpty())
      details.add(
          "J = "
              + String.join(
                  ", ", s.couplingsHz().stream().map(LaboratoryAnalysisSummary::number).toList())
              + " Hz");
    if (s.integral() != null)
      details.add(
          "интеграл " + number(s.integral().multiply(scale).round(new java.math.MathContext(6))));
    if (!s.note().isBlank()) details.add(s.note());
    return number(s.from().setScale(4, java.math.RoundingMode.HALF_UP))
        + (s.from().compareTo(s.to()) == 0
            ? ""
            : "–" + number(s.to().setScale(4, java.math.RoundingMode.HALF_UP)))
        + (details.isEmpty() ? "" : " (" + String.join(", ", details) + ")");
  }

  private static String display(final double value) {
    return number(BigDecimal.valueOf(value).round(new java.math.MathContext(8)));
  }

  public static String traceText(
      final ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace trace) {
    final var p = trace.processing();
    return "Сохранённые данные: "
        + trace.format()
        + " · "
        + trace.sourceName()
        + " · "
        + trace.real().size()
        + " точек\n"
        + "Обработка v"
        + p.version()
        + ": "
        + (trace.domain()
                == ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace.Domain.FID
            ? "FFT "
                + p.zeroFill()
                + "; LB="
                + display(p.lineBroadeningHz())
                + " Hz; цифровая задержка "
                + (p.removeDelay() ? display(trace.groupDelay()) : "не снята")
                + "; вес первой точки "
                + display(p.firstPoint())
                + "; "
            : "")
        + "PH0="
        + display(p.phase0())
        + "°; PH1="
        + display(p.phase1())
        + "°; опора="
        + display(p.pivot())
        + "; сдвиг шкалы="
        + display(p.referencePpm())
        + " ppm; фон="
        + (p.baselineEdges() ? "линия по краям 1 %" : "без коррекции")
        + "\nИсходные контрольные суммы SHA-256: "
        + String.join(
            "; ",
            new TreeMap<>(trace.sourceHashes())
                .entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList())
        + "\nСохранены числовые точки и параметры; полный каталог прибора переносится отдельно как вложение. Интенсивность и интегралы не определяют чистоту автоматически.";
  }

  public static List<String> diagnostics(
      final LaboratoryAnalysis a, final LaboratoryExperiment experiment) {
    final List<String> notes = new ArrayList<>();
    final var current =
        experiment.components().stream().filter(c -> c.id().equals(a.componentId())).findFirst();
    if (current.isEmpty()) {
      notes.add("Исходный компонент удалён; показан сохранённый снимок измерения.");
    } else if (!LaboratoryMaterialProperties.signature(a.molecule())
        .equals(LaboratoryMaterialProperties.signature(current.get().molecule()))) {
      notes.add(
          "Структура компонента изменилась; назначения относятся только к сохранённому снимку.");
    }
    if (a.attachmentIds().isEmpty() && !(a.data() instanceof Nmr n && n.trace() != null)) {
      notes.add(
          "Исходные файлы не связаны. Введённый список сигналов не заменяет приборные данные.");
    }
    if (a.data() instanceof Nmr n) {
      final long nuclei =
          a.molecule().atoms().stream()
              .filter(
                  atom ->
                      atom.element().atomicNumber() == n.nucleus().element()
                          && (atom.isotope() == null
                              || atom.isotope().massNumber() == n.nucleus().isotope()))
              .count();
      notes.add(
          "В снимке: "
              + nuclei
              + " атомов подходящего элемента/изотопа; сигналов: "
              + n.signals().size()
              + ". Симметрия, обмен и перекрытие могут менять число сигналов.");
      if (n.nucleus() == Nucleus.H1) {
        notes.add(
            "Сумма введённых интегралов × множитель: "
                + number(n.integralSum().round(new java.math.MathContext(6)))
                + ". Без нормировки и учёта обменных H это не автоматическое подтверждение структуры.");
      } else {
        notes.add(
            "Интенсивности этого спектра не интерпретируются автоматически как число атомов.");
      }
      if (n.signals().stream().anyMatch(s -> s.atoms().isEmpty())) {
        notes.add("Часть сигналов не назначена атомам.");
      }
      final Set<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId> assigned =
          new HashSet<>();
      if (n.signals().stream().flatMap(s -> s.atoms().stream()).anyMatch(id -> !assigned.add(id))) {
        notes.add("Некоторые атомы назначены нескольким сигналам — проверьте расшифровку.");
      }
    } else if (a.data() instanceof Mass m && (m.foundMz() == null || m.calculatedMz() == null)) {
      notes.add("Для ошибки ppm нужны оба значения m/z.");
    }
    return List.copyOf(notes);
  }
}
