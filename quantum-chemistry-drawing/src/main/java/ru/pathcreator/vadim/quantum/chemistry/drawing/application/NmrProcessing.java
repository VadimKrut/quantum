/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace;

/** Reproducible 1D FT/phase/baseline operations; no inference of purity or molecular identity. */
public final class NmrProcessing {
  private NmrProcessing() {}

  public record Spectrum(
      double firstPpm, double stepPpm, List<Double> real, List<Double> imaginary) {
    public Spectrum {
      real = List.copyOf(real);
      imaginary = List.copyOf(imaginary);
    }

    public double ppm(final int index) {
      return this.firstPpm + index * this.stepPpm;
    }

    public double lastPpm() {
      return this.ppm(this.real.size() - 1);
    }

    public double area(final double low, final double high) {
      if (!Double.isFinite(low)
          || !Double.isFinite(high)
          || low >= high
          || low < this.lastPpm()
          || high > this.firstPpm) {
        throw new IllegalArgumentException(
            "Область интегрирования должна находиться внутри спектра.");
      }
      double sum = 0;
      for (int i = 0; i < this.real.size() - 1; i++) {
        final double left = this.ppm(i + 1),
            right = this.ppm(i),
            a = Math.max(low, left),
            b = Math.min(high, right);
        if (a < b) {
          final double y0 = this.real.get(i + 1), slope = (this.real.get(i) - y0) / (right - left);
          sum += (b - a) * (y0 + slope * ((a + b) / 2 - left));
        }
      }
      return sum;
    }

    public int peak(final double center, final double radius) {
      int best = -1;
      for (int i = 1; i < this.real.size() - 1; i++) {
        if (Math.abs(this.ppm(i) - center) <= radius
            && this.real.get(i) >= this.real.get(i - 1)
            && this.real.get(i) >= this.real.get(i + 1)
            && (best < 0 || this.real.get(i) > this.real.get(best))) {
          best = i;
        }
      }
      return best;
    }
  }

  public static Spectrum process(final NmrTrace trace) {
    check();
    final var p = trace.processing();
    double[] re = array(trace.real()),
        im = trace.imaginary().isEmpty() ? new double[re.length] : array(trace.imaginary());
    double first = trace.firstPpm(), step = trace.stepPpm();
    if (trace.domain() == NmrTrace.Domain.FID) {
      if (p.removeDelay() && trace.groupDelay() > 0) {
        final double[][] corrected = removeDigitalDelay(re, im, trace.groupDelay());
        re = corrected[0];
        im = corrected[1];
      }
      for (int i = 0; i < re.length; i++) {
        final double weight =
            Math.exp(-Math.PI * p.lineBroadeningHz() * i / trace.spectralWidthHz())
                * (i == 0 ? p.firstPoint() : 1);
        re[i] *= weight;
        im[i] *= weight;
      }
      re = Arrays.copyOf(re, p.zeroFill());
      im = Arrays.copyOf(im, p.zeroFill());
      fft(re, im, false);
      final int n = re.length;
      final double[] r = new double[n], q = new double[n];
      for (int i = 0; i < n; i++) {
        final int j = (n / 2 - 1 - i + n) % n;
        r[i] = re[j];
        q[i] = im[j];
      }
      re = r;
      im = q;
      step = -trace.spectralWidthHz() / trace.frequencyMHz() / n;
      first = trace.carrierPpm() + trace.spectralWidthHz() / trace.frequencyMHz() / 2 + step;
    }
    for (int i = 0; i < re.length; i++) {
      final double
          angle =
              Math.toRadians(p.phase0() + p.phase1() * ((double) i / (re.length - 1) - p.pivot())),
          c = Math.cos(angle),
          s = Math.sin(angle),
          a = re[i];
      re[i] = a * c - im[i] * s;
      im[i] = a * s + im[i] * c;
    }
    if (p.baselineEdges()) {
      final int count = Math.max(1, re.length / 100);
      double a = 0, b = 0;
      for (int i = 0; i < count; i++) {
        a += re[i] / count;
        b += re[re.length - count + i] / count;
      }
      final double x0 = (count - 1) / 2.0, x1 = re.length - 1 - x0;
      for (int i = 0; i < re.length; i++) {
        re[i] -= a + (b - a) * (i - x0) / (x1 - x0);
      }
    }
    check();
    for (int i = 0; i < re.length; i++) {
      if (!Double.isFinite(re[i]) || !Double.isFinite(im[i])) {
        throw new IllegalArgumentException("Численное переполнение при обработке спектра.");
      }
    }
    return new Spectrum(first + p.referencePpm(), step, list(re), list(im));
  }

  /* The circular shift / tail folding method follows nmrglue's rm_dig_filter.
   * Copyright (c) 2010-2015 Jonathan J. Helmus. BSD-3-Clause notice is retained
   * in licenses/nmrglue-LICENSE.txt. Exact (non-truncated) group delay is used. */
  private static double[][] removeDigitalDelay(
      final double[] real, final double[] imaginary, final double delay) {
    final int n = real.length;
    double[] r = new double[n], q = new double[n];
    for (int i = 0; i < n; i++) {
      r[i] = real[(i + n / 2) % n];
      q[i] = imaginary[(i + n / 2) % n];
    }
    fft(r, q, false);
    for (int i = 0; i < n; i++) {
      final double a = 2 * Math.PI * delay * i / n, c = Math.cos(a), s = Math.sin(a), v = r[i];
      r[i] = v * c - q[i] * s;
      q[i] = v * s + q[i] * c;
    }
    fft(r, q, true);
    final double[] a = new double[n], b = new double[n];
    for (int i = 0; i < n; i++) {
      a[i] = r[(i + (n + 1) / 2) % n];
      b[i] = q[(i + (n + 1) / 2) % n];
    }
    final int skip = (int) Math.floor(delay + 2), add = Math.max(skip - 6, 0);
    for (int i = 0; i < add; i++) {
      a[i] += a[n - 1 - i];
      b[i] += b[n - 1 - i];
    }
    return new double[][] {Arrays.copyOf(a, n - skip), Arrays.copyOf(b, n - skip)};
  }

  /** Negative-sign DFT, radix 2 or Bluestein for arbitrary acquisition length. */
  static void fft(final double[] re, final double[] im, final boolean inverse) {
    check();
    final int n = re.length;
    if (inverse) {
      for (int i = 0; i < n; i++) {
        im[i] = -im[i];
      }
      fft(re, im, false);
      for (int i = 0; i < n; i++) {
        re[i] /= n;
        im[i] = -im[i] / n;
      }
      return;
    }
    if (Integer.bitCount(n) != 1) {
      int size = 1;
      while (size < 2 * n - 1) {
        size <<= 1;
      }
      final double[] ar = new double[size],
          ai = new double[size],
          br = new double[size],
          bi = new double[size];
      for (int i = 0; i < n; i++) {
        final double angle = Math.PI * ((long) i * i % (2L * n)) / n,
            c = Math.cos(angle),
            s = Math.sin(angle);
        ar[i] = re[i] * c + im[i] * s;
        ai[i] = im[i] * c - re[i] * s;
        br[i] = c;
        bi[i] = s;
        if (i != 0) {
          br[size - i] = c;
          bi[size - i] = s;
        }
      }
      fft(ar, ai, false);
      fft(br, bi, false);
      for (int i = 0; i < size; i++) {
        final double a = ar[i];
        ar[i] = a * br[i] - ai[i] * bi[i];
        ai[i] = a * bi[i] + ai[i] * br[i];
      }
      fft(ar, ai, true);
      for (int i = 0; i < n; i++) {
        final double angle = Math.PI * ((long) i * i % (2L * n)) / n,
            c = Math.cos(angle),
            s = Math.sin(angle);
        re[i] = ar[i] * c + ai[i] * s;
        im[i] = ai[i] * c - ar[i] * s;
      }
      return;
    }
    for (int i = 1, j = 0; i < n; i++) {
      int bit = n >> 1;
      while ((j & bit) != 0) {
        j ^= bit;
        bit >>= 1;
      }
      j ^= bit;
      if (i < j) {
        double a = re[i];
        re[i] = re[j];
        re[j] = a;
        a = im[i];
        im[i] = im[j];
        im[j] = a;
      }
    }
    for (int len = 2; len <= n; len <<= 1) {
      check();
      final double angle = -2 * Math.PI / len, wr = Math.cos(angle), wi = Math.sin(angle);
      for (int start = 0; start < n; start += len) {
        double c = 1, s = 0;
        for (int j = 0; j < len / 2; j++) {
          final int u = start + j, v = u + len / 2;
          final double a = re[v] * c - im[v] * s, b = re[v] * s + im[v] * c;
          re[v] = re[u] - a;
          im[v] = im[u] - b;
          re[u] += a;
          im[u] += b;
          final double next = c * wr - s * wi;
          s = c * wi + s * wr;
          c = next;
        }
      }
    }
  }

  private static double[] array(final List<Double> values) {
    return values.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private static List<Double> list(final double[] values) {
    return Arrays.stream(values).boxed().toList();
  }

  private static void check() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Обработка остановлена.");
    }
  }
}
