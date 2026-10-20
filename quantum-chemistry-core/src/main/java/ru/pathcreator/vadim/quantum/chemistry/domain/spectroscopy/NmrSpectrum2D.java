/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.Nucleus;

/** Immutable processed 2D grid; explicit ppm axes, values in row-major y * width + x order. */
public record NmrSpectrum2D(
    Nucleus xNucleus,
    Nucleus yNucleus,
    List<Double> xPpm,
    List<Double> yPpm,
    List<Double> values,
    String format,
    String sourceName,
    Map<String, String> sourceHashes,
    String acquisition) {
  public static final int MAX_AXIS = 8192, MAX_CELLS = 4_194_304;

  public NmrSpectrum2D {
    Objects.requireNonNull(xNucleus);
    Objects.requireNonNull(yNucleus);
    xPpm = axis(xPpm);
    yPpm = axis(yPpm);
    final long cells = (long) xPpm.size() * yPpm.size();
    if (cells > MAX_CELLS || Objects.requireNonNull(values).size() != cells) {
      throw new IllegalArgumentException(
          "2D ЯМР: размер матрицы должен соответствовать осям и не превышать 4 194 304 точки.");
    }
    values = values instanceof DenseValues ? values : new DenseValues(values);
    format = text(format, 100);
    sourceName = text(sourceName, 500);
    acquisition = text(acquisition, 32000);
    if (format.isBlank()
        || sourceName.isBlank()
        || Objects.requireNonNull(sourceHashes).isEmpty()
        || sourceHashes.size() > 16) {
      throw new IllegalArgumentException(
          "Укажите формат, источник и 1–16 контрольных сумм 2D спектра.");
    }
    sourceHashes = Map.copyOf(sourceHashes);
    sourceHashes.forEach(
        (name, hash) -> {
          text(name, 200);
          if (name.isBlank() || !hash.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException(
                "Некорректная контрольная сумма источника 2D спектра.");
          }
        });
  }

  /** Copies primitive input arrays without retaining boxed grids; callers retain no mutable access. */
  public static NmrSpectrum2D fromArrays(
      final Nucleus x,
      final Nucleus y,
      final double[] xPpm,
      final double[] yPpm,
      final double[] values,
      final String format,
      final String sourceName,
      final Map<String, String> hashes,
      final String acquisition) {
    Objects.requireNonNull(xPpm);
    Objects.requireNonNull(yPpm);
    Objects.requireNonNull(values);
    if (xPpm.length < 2
        || xPpm.length > MAX_AXIS
        || yPpm.length < 2
        || yPpm.length > MAX_AXIS
        || (long) xPpm.length * yPpm.length > MAX_CELLS
        || (long) xPpm.length * yPpm.length != values.length) {
      throw new IllegalArgumentException("Недопустимый размер матрицы 2D ЯМР.");
    }
    return new NmrSpectrum2D(
        x,
        y,
        Arrays.stream(xPpm).boxed().toList(),
        Arrays.stream(yPpm).boxed().toList(),
        new DenseValues(values),
        format,
        sourceName,
        hashes,
        acquisition);
  }

  public int width() {
    return this.xPpm.size();
  }

  public int height() {
    return this.yPpm.size();
  }

  public double value(final int x, final int y) {
    Objects.checkIndex(x, this.width());
    Objects.checkIndex(y, this.height());
    return ((DenseValues) this.values).data[y * this.width() + x];
  }

  private static List<Double> axis(final List<Double> input) {
    Objects.requireNonNull(input);
    if (input.size() < 2 || input.size() > MAX_AXIS) {
      throw new IllegalArgumentException("Ось 2D ЯМР должна содержать 2–8192 точки.");
    }
    final List<Double> result = List.copyOf(input);
    int direction = 0;
    double previous = 0;
    for (int i = 0; i < result.size(); i++) {
      final double value = result.get(i);
      if (!Double.isFinite(value) || Math.abs(value) > 1e6) {
        throw new IllegalArgumentException("Недопустимая координата ppm.");
      }
      if (i > 0) {
        final int current = value > previous ? 1 : value < previous ? -1 : 0;
        if (current == 0 || direction != 0 && current != direction) {
          throw new IllegalArgumentException(
              "Оси 2D ЯМР должны быть строго монотонными, без повторных ppm.");
        }
        direction = current;
      }
      previous = value;
    }
    return result;
  }

  private static String text(final String value, final int max) {
    Objects.requireNonNull(value);
    if (value.length() > max || value.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("Слишком длинное или недопустимое поле 2D спектра.");
    }
    return value;
  }

  private static void finite(final double value) {
    if (!Double.isFinite(value) || Math.abs(value) > 1e100) {
      throw new IllegalArgumentException("Матрица 2D спектра содержит недопустимое число.");
    }
  }

  /** Inherited mutators throw; no array accessor is exposed. Hash is cached for exact grid sharing in history. */
  private static final class DenseValues extends AbstractList<Double> implements RandomAccess {
    private final double[] data;
    private final int hash;

    DenseValues(final List<Double> source) {
      this.data = new double[source.size()];
      int index = 0;
      for (final Double value : source) {
        data[index] = Objects.requireNonNull(value);
        finite(data[index++]);
      }
      this.hash = Arrays.hashCode(data);
    }

    DenseValues(final double[] source) {
      this.data = source.clone();
      for (final double value : data) {
        finite(value);
      }
      this.hash = Arrays.hashCode(data);
    }

    @Override
    public Double get(final int index) {
      return data[Objects.checkIndex(index, data.length)];
    }

    @Override
    public int size() {
      return data.length;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      return other instanceof DenseValues d ? Arrays.equals(data, d.data) : super.equals(other);
    }
  }
}
