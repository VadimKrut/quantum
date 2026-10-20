/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;

/** Marching squares on the original processed grid; neither peak picking nor integration. */
public final class NmrContours2D {
  public static final int MAX_SEGMENTS = 500_000;

  public record Segment(double x1, double y1, double x2, double y2) {}

  public record Level(double intensity, List<Segment> segments) {
    public Level {
      segments = List.copyOf(segments);
    }
  }

  public record Contours(double maxAbsolute, List<Level> levels) {
    public Contours {
      levels = List.copyOf(levels);
    }
  }

  private NmrContours2D() {}

  /** Geometric levels from the selected fraction of absolute maximum up to 90% (or the larger threshold). */
  public static Contours calculate(
      final NmrSpectrum2D spectrum,
      final double fraction,
      final int count,
      final BooleanSupplier cancelled) {
    Objects.requireNonNull(spectrum);
    Objects.requireNonNull(cancelled);
    if (!Double.isFinite(fraction)
        || fraction < .001
        || fraction > .99
        || count < 1
        || count > 12) {
      throw new IllegalArgumentException("Порог контуров: 0,1–99% максимума; число уровней: 1–12.");
    }
    double maximum = 0;
    int index = 0;
    for (final double value : spectrum.values()) {
      if ((index++ & 4095) == 0) {
        check(cancelled);
      }
      maximum = Math.max(maximum, Math.abs(value));
    }
    if (maximum == 0) {
      return new Contours(0, List.of());
    }
    final List<Level> result = new ArrayList<>();
    int segments = 0;
    for (int n = 0; n < count; n++) {
      final double magnitude =
          maximum
              * fraction
              * (count == 1
                  ? 1
                  : Math.pow(Math.max(fraction, .9) / fraction, (double) n / (count - 1)));
      if (magnitude == 0) {
        throw new IllegalArgumentException(
            "Интенсивность слишком мала для выбранного порога контуров. Увеличьте порог; исходные значения не меняются.");
      }
      for (final int sign : new int[] {1, -1}) {
        check(cancelled);
        final double level = sign * magnitude;
        final List<Segment> lines = new ArrayList<>();
        for (int y = 0; y < spectrum.height() - 1; y++) {
          check(cancelled);
          for (int x = 0; x < spectrum.width() - 1; x++) {
            final double a = spectrum.value(x, y),
                b = spectrum.value(x + 1, y),
                c = spectrum.value(x + 1, y + 1),
                d = spectrum.value(x, y + 1);
            final double minimum = Math.min(Math.min(a, b), Math.min(c, d)),
                max = Math.max(Math.max(a, b), Math.max(c, d));
            if (level < minimum || level >= max) {
              continue;
            }
            final double x0 = spectrum.xPpm().get(x),
                x1 = spectrum.xPpm().get(x + 1),
                y0 = spectrum.yPpm().get(y),
                y1 = spectrum.yPpm().get(y + 1);
            final double[] px = new double[4], py = new double[4];
            final int[] edges = new int[4];
            int hits = 0;
            if ((a > level) != (b > level)) {
              final double t = (level - a) / (b - a);
              px[0] = x0 + t * (x1 - x0);
              py[0] = y0;
              edges[hits++] = 0;
            }
            if ((b > level) != (c > level)) {
              final double t = (level - b) / (c - b);
              px[1] = x1;
              py[1] = y0 + t * (y1 - y0);
              edges[hits++] = 1;
            }
            if ((c > level) != (d > level)) {
              final double t = (level - c) / (d - c);
              px[2] = x1 + t * (x0 - x1);
              py[2] = y1;
              edges[hits++] = 2;
            }
            if ((d > level) != (a > level)) {
              final double t = (level - d) / (a - d);
              px[3] = x0;
              py[3] = y1 + t * (y0 - y1);
              edges[hits++] = 3;
            }
            if (hits == 2) {
              lines.add(segment(px, py, edges[0], edges[1]));
              segments++;
            } else if (hits == 4) {
              // Asymptotic decider preserves saddle topology; equal saddle uses a stable diagonal.
              final double determinant =
                  ((a - level) / maximum) * ((c - level) / maximum)
                      - ((b - level) / maximum) * ((d - level) / maximum);
              if (determinant >= 0) {
                lines.add(segment(px, py, 0, 1));
                lines.add(segment(px, py, 2, 3));
              } else {
                lines.add(segment(px, py, 0, 3));
                lines.add(segment(px, py, 1, 2));
              }
              segments += 2;
            }
            if (segments > MAX_SEGMENTS) {
              throw new IllegalArgumentException(
                  "Контуры слишком подробны. Увеличьте порог или уменьшите число уровней; исходная матрица сохранена полностью.");
            }
          }
        }
        result.add(new Level(level, lines));
      }
    }
    check(cancelled);
    return new Contours(maximum, result);
  }

  private static Segment segment(final double[] x, final double[] y, final int a, final int b) {
    return new Segment(x[a], y[a], x[b], y[b]);
  }

  private static void check(final BooleanSupplier cancelled) {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new CancellationException("Построение контуров отменено.");
    }
  }
}
