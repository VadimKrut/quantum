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
import java.math.MathContext;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** An analytical observation keeps its own core structure snapshot and provenance. */
public record LaboratoryAnalysis(
    String id,
    String componentId,
    String title,
    String sample,
    Instant recordedAt,
    Origin origin,
    String instrument,
    String source,
    String note,
    Molecule molecule,
    List<String> attachmentIds,
    String correctsId,
    Data data,
    String sampleId) {
  public LaboratoryAnalysis(
      final String id,
      final String componentId,
      final String title,
      final String sample,
      final Instant recordedAt,
      final Origin origin,
      final String instrument,
      final String source,
      final String note,
      final Molecule molecule,
      final List<String> attachmentIds,
      final String correctsId,
      final Data data) {
    this(
        id,
        componentId,
        title,
        sample,
        recordedAt,
        origin,
        instrument,
        source,
        note,
        molecule,
        attachmentIds,
        correctsId,
        data,
        null);
  }

  public enum Origin {
    MEASURED("Измерено"),
    LITERATURE("Литература"),
    SIMULATED("Расчёт / модель");
    private final String label;

    Origin(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public enum Nucleus {
    H1("¹H", 1, 1),
    C13("¹³C", 6, 13),
    F19("¹⁹F", 9, 19),
    P31("³¹P", 15, 31),
    N15("¹⁵N", 7, 15);
    private final String label;
    private final int element, isotope;

    Nucleus(final String label, final int element, final int isotope) {
      this.label = label;
      this.element = element;
      this.isotope = isotope;
    }

    public int element() {
      return this.element;
    }

    public int isotope() {
      return this.isotope;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public sealed interface Data permits Nmr, Mass, Tlc {
    String method();
  }

  public record Signal(
      BigDecimal from,
      BigDecimal to,
      String multiplicity,
      BigDecimal integral,
      List<BigDecimal> couplingsHz,
      Set<AtomId> atoms,
      String note,
      boolean spectralIntegral) {
    public Signal(
        final BigDecimal from,
        final BigDecimal to,
        final String multiplicity,
        final BigDecimal integral,
        final List<BigDecimal> couplingsHz,
        final Set<AtomId> atoms,
        final String note) {
      this(from, to, multiplicity, integral, couplingsHz, atoms, note, false);
    }

    public Signal {
      from = decimal(Objects.requireNonNull(from));
      to = to == null ? from : decimal(to);
      integral = decimal(integral);
      if (from.compareTo(to) > 0) {
        throw new IllegalArgumentException("Начало диапазона δ должно быть не больше конца.");
      }
      Objects.requireNonNull(multiplicity);
      Objects.requireNonNull(note);
      nonnegative(integral, "Интеграл");
      couplingsHz = couplingsHz.stream().map(LaboratoryAnalysis::decimal).toList();
      atoms = Set.copyOf(atoms);
      couplingsHz.forEach(j -> positive(Objects.requireNonNull(j), "J, Гц"));
    }
  }

  public record Nmr(
      Nucleus nucleus,
      BigDecimal frequencyMHz,
      String solvent,
      BigDecimal integralScale,
      List<Signal> signals,
      ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace trace)
      implements Data {
    public Nmr(
        final Nucleus nucleus,
        final BigDecimal frequencyMHz,
        final String solvent,
        final BigDecimal integralScale,
        final List<Signal> signals) {
      this(nucleus, frequencyMHz, solvent, integralScale, signals, null);
    }

    public Nmr {
      Objects.requireNonNull(nucleus);
      frequencyMHz = decimal(frequencyMHz);
      positive(frequencyMHz, "Частота");
      Objects.requireNonNull(solvent);
      integralScale = integralScale == null ? BigDecimal.ONE : decimal(integralScale);
      positive(integralScale, "Множитель интегралов");
      signals = List.copyOf(signals);
      if (trace == null && signals.stream().anyMatch(Signal::spectralIntegral)) {
        throw new IllegalArgumentException("Для интеграла спектра необходимы исходные данные.");
      }
      if (trace != null
          && (trace.nucleus() != nucleus
              || frequencyMHz == null
              || Double.compare(frequencyMHz.doubleValue(), trace.frequencyMHz()) != 0)) {
        throw new IllegalArgumentException(
            "Ядро и частота должны соответствовать исходному спектру.");
      }
      if (signals.size() > 2000) {
        throw new IllegalArgumentException("Не более 2000 сигналов в одной записи.");
      }
    }

    @Override
    public String method() {
      return this.nucleus + " ЯМР";
    }

    public BigDecimal integralSum() {
      return this.signals.stream()
          .map(Signal::integral)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .multiply(this.integralScale);
    }
  }

  public record Mass(
      String technique,
      String ion,
      String ionFormula,
      BigDecimal calculatedMz,
      BigDecimal foundMz,
      String calculationSource)
      implements Data {
    public Mass {
      Objects.requireNonNull(technique);
      Objects.requireNonNull(ion);
      Objects.requireNonNull(ionFormula);
      Objects.requireNonNull(calculationSource);
      calculatedMz = decimal(calculatedMz);
      foundMz = decimal(foundMz);
      positive(calculatedMz, "Расчётное m/z");
      positive(foundMz, "Найденное m/z");
      if (calculatedMz != null && calculationSource.isBlank()) {
        throw new IllegalArgumentException("Укажите источник расчётного m/z.");
      }
    }

    @Override
    public String method() {
      return "МС";
    }

    public BigDecimal ppmError() {
      return this.calculatedMz == null || this.foundMz == null
          ? null
          : this.foundMz
              .subtract(this.calculatedMz)
              .multiply(new BigDecimal("1000000"))
              .divide(this.calculatedMz, MathContext.DECIMAL128);
    }
  }

  public record Spot(String label, BigDecimal distanceMm, BigDecimal reportedRf) {
    public Spot {
      Objects.requireNonNull(label);
      distanceMm = decimal(distanceMm);
      reportedRf = decimal(reportedRf);
      nonnegative(distanceMm, "Расстояние пятна");
      nonnegative(reportedRf, "Rf");
      if (distanceMm != null && reportedRf != null) {
        throw new IllegalArgumentException(
            "Задайте расстояние пятна или Rf вручную, не оба сразу.");
      }
      if (reportedRf != null && reportedRf.compareTo(BigDecimal.ONE) > 0) {
        throw new IllegalArgumentException("Rf должен быть от 0 до 1.");
      }
    }
  }

  public record Tlc(
      String stationary, String eluent, String visualization, BigDecimal frontMm, List<Spot> spots)
      implements Data {
    public Tlc {
      Objects.requireNonNull(stationary);
      Objects.requireNonNull(eluent);
      Objects.requireNonNull(visualization);
      frontMm = decimal(frontMm);
      positive(frontMm, "Фронт элюента");
      spots = List.copyOf(spots);
      if (spots.size() > 2000) {
        throw new IllegalArgumentException("Не более 2000 пятен в одной записи.");
      }
      for (final Spot spot : spots) {
        if (spot.distanceMm() != null
            && (frontMm == null || spot.distanceMm().compareTo(frontMm) > 0)) {
          throw new IllegalArgumentException(
              "Для расстояния пятна задайте фронт не меньше этого расстояния.");
        }
      }
    }

    @Override
    public String method() {
      return "ТСХ";
    }

    public BigDecimal rf(final Spot spot) {
      if (!this.spots.contains(spot)) {
        throw new IllegalArgumentException("Пятно не принадлежит этой пластинке.");
      }
      return spot.reportedRf() != null
          ? spot.reportedRf()
          : spot.distanceMm() == null
              ? null
              : spot.distanceMm().divide(this.frontMm, MathContext.DECIMAL128);
    }
  }

  public LaboratoryAnalysis {
    id = IdentifierValue.requireIdentifier(id, "Analysis id");
    componentId = IdentifierValue.requireIdentifier(componentId, "Source component id");
    title = TextValue.requireText(title, "Analysis title");
    Objects.requireNonNull(sample);
    Objects.requireNonNull(recordedAt);
    Objects.requireNonNull(origin);
    Objects.requireNonNull(instrument);
    Objects.requireNonNull(source);
    Objects.requireNonNull(note);
    Objects.requireNonNull(molecule);
    Objects.requireNonNull(data);
    attachmentIds = List.copyOf(attachmentIds);
    if (attachmentIds.size() != new HashSet<>(attachmentIds).size()) {
      throw new IllegalArgumentException("Повторяющиеся файлы анализа.");
    }
    if (origin != Origin.MEASURED && source.isBlank()) {
      throw new IllegalArgumentException("Укажите литературный источник или метод расчёта.");
    }
    if (sampleId != null) {
      IdentifierValue.requireIdentifier(sampleId, "Analysis sample id");
    }
    if (correctsId != null) {
      IdentifierValue.requireIdentifier(correctsId, "Corrected analysis id");
    }
    if (data instanceof Nmr nmr) {
      final Map<AtomId, Atom> atoms = new HashMap<>();
      molecule.atoms().forEach(a -> atoms.put(a.id(), a));
      for (final Signal signal : nmr.signals()) {
        for (final AtomId atom : signal.atoms()) {
          if (!atoms.containsKey(atom)
              || atoms.get(atom).element().atomicNumber() != nmr.nucleus().element()
              || atoms.get(atom).isotope() != null
                  && atoms.get(atom).isotope().massNumber() != nmr.nucleus().isotope()) {
            throw new IllegalArgumentException(
                "Назначение сигнала не соответствует атому и ядру в снимке структуры: "
                    + atom.value());
          }
        }
      }
    }
  }

  private static BigDecimal decimal(final BigDecimal value) {
    if (value == null) {
      return null;
    }
    if (Math.abs((long) value.scale()) > 64 || value.precision() > 64) {
      throw new IllegalArgumentException("Число вне поддержанного диапазона точности.");
    }
    return value.stripTrailingZeros();
  }

  private static void nonnegative(final BigDecimal value, final String name) {
    if (value != null && value.signum() < 0) {
      throw new IllegalArgumentException(name + " не может быть отрицательным.");
    }
  }

  private static void positive(final BigDecimal value, final String name) {
    if (value != null && value.signum() <= 0) {
      throw new IllegalArgumentException(name + " должно быть больше нуля.");
    }
  }
}
