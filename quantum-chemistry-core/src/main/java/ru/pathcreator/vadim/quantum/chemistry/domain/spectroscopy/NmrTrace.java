/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.Nucleus;

/** Immutable acquired points and a reproducible, versioned 1D processing recipe. */
public record NmrTrace(
    Domain domain,
    Nucleus nucleus,
    double frequencyMHz,
    String solvent,
    double spectralWidthHz,
    double carrierPpm,
    double firstPpm,
    double stepPpm,
    double groupDelay,
    String format,
    String sourceName,
    Map<String, String> sourceHashes,
    String acquisition,
    List<Double> real,
    List<Double> imaginary,
    Processing processing) {
  public static final int MAX_POINTS = 262144;

  public enum Domain {
    FID,
    SPECTRUM
  }

  public record Processing(
      int version,
      int zeroFill,
      double lineBroadeningHz,
      double phase0,
      double phase1,
      double pivot,
      double referencePpm,
      boolean baselineEdges,
      boolean removeDelay,
      double firstPoint) {
    public Processing {
      if (version != 1 || zeroFill < 2 || zeroFill > MAX_POINTS) {
        throw new IllegalArgumentException("Неподдержанная версия обработки или размер спектра.");
      }
      finite(lineBroadeningHz);
      finite(phase0);
      finite(phase1);
      finite(pivot);
      finite(referencePpm);
      finite(firstPoint);
      if (lineBroadeningHz < 0
          || lineBroadeningHz > 10000
          || Math.abs(phase0) > 36000
          || Math.abs(phase1) > 36000
          || pivot < 0
          || pivot > 1
          || Math.abs(referencePpm) > 100000
          || firstPoint < 0
          || firstPoint > 1) {
        throw new IllegalArgumentException("Параметры обработки вне допустимого диапазона.");
      }
    }
  }

  public NmrTrace {
    Objects.requireNonNull(domain);
    Objects.requireNonNull(nucleus);
    Objects.requireNonNull(processing);
    solvent = text(solvent, 500);
    format = text(format, 100);
    sourceName = text(sourceName, 500);
    acquisition = text(acquisition, 32000);
    sourceHashes = Map.copyOf(sourceHashes);
    if (sourceHashes.isEmpty()
        || sourceHashes.size() > 16
        || sourceName.isBlank()
        || format.isBlank()) {
      throw new IllegalArgumentException("Укажите источник спектра и контрольные суммы.");
    }
    sourceHashes.forEach(
        (name, hash) -> {
          text(name, 200);
          if (name.isBlank() || !hash.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Некорректная контрольная сумма исходного файла.");
          }
        });
    finite(frequencyMHz);
    finite(spectralWidthHz);
    finite(carrierPpm);
    finite(firstPpm);
    finite(stepPpm);
    finite(groupDelay);
    if (frequencyMHz <= 0
        || frequencyMHz > 10000
        || spectralWidthHz <= 0
        || spectralWidthHz > 1e8
        || Math.abs(carrierPpm) > 1e6
        || Math.abs(firstPpm) > 1e6
        || groupDelay < 0) {
      throw new IllegalArgumentException("Неверная шкала спектра или параметры регистрации.");
    }
    real = List.copyOf(real);
    imaginary = List.copyOf(imaginary);
    if (real.size() < 8
        || real.size() > MAX_POINTS
        || !imaginary.isEmpty() && imaginary.size() != real.size()) {
      throw new IllegalArgumentException(
          "Спектр содержит от 8 до 262144 точек, комплексные каналы должны совпадать.");
    }
    real.forEach(NmrTrace::finite);
    imaginary.forEach(NmrTrace::finite);
    if (domain == Domain.FID) {
      if (imaginary.size() != real.size()
          || groupDelay + 8 >= real.size()
          || processing.zeroFill() < real.size()
          || Integer.bitCount(processing.zeroFill()) != 1) {
        throw new IllegalArgumentException(
            "Для FID нужны комплексные точки и дополнение до степени двойки не меньше исходного размера.");
      }
    } else {
      if (Math.abs(stepPpm * real.size()) > 2e6
          || Math.abs(stepPpm + spectralWidthHz / frequencyMHz / real.size())
              > Math.max(1e-10, Math.abs(stepPpm) * 1e-8)
          || stepPpm >= 0
          || processing.zeroFill() != real.size()
          || processing.lineBroadeningHz() != 0
          || processing.removeDelay()
          || processing.firstPoint() != 1
          || groupDelay != 0) {
        throw new IllegalArgumentException(
            "Готовый спектр имеет убывающую шкалу ppm и не проходит повторное Фурье-преобразование.");
      }
      if (imaginary.isEmpty() && (processing.phase0() != 0 || processing.phase1() != 0)) {
        throw new IllegalArgumentException(
            "Фазировка требует мнимого канала; у этого спектра сохранена только действительная часть.");
      }
    }
  }

  public NmrTrace withProcessing(final Processing value) {
    return new NmrTrace(
        this.domain,
        this.nucleus,
        this.frequencyMHz,
        this.solvent,
        this.spectralWidthHz,
        this.carrierPpm,
        this.firstPpm,
        this.stepPpm,
        this.groupDelay,
        this.format,
        this.sourceName,
        this.sourceHashes,
        this.acquisition,
        this.real,
        this.imaginary,
        value);
  }

  public static Processing defaults(final Domain domain, final int size) {
    int n = 8;
    while (n < size) {
      n <<= 1;
    }
    return new Processing(
        1, domain == Domain.FID ? n : size, 0, 0, 0, .5, 0, false, domain == Domain.FID, 1);
  }

  private static String text(final String value, final int limit) {
    Objects.requireNonNull(value);
    if (value.length() > limit) {
      throw new IllegalArgumentException("Слишком длинное поле спектра.");
    }
    return value;
  }

  private static void finite(final double value) {
    if (!Double.isFinite(value) || Math.abs(value) > 1e100) {
      throw new IllegalArgumentException("Спектр содержит недопустимое число.");
    }
  }
}
