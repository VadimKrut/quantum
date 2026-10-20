/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import java.util.concurrent.atomic.LongAdder;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.AoElectronRepulsionValues;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/**
 * Вычисляет AO ERI quartet напрямую и хранит только ограниченный LRU-cache.
 */
public final class DirectGaussianElectronRepulsionIntegralProvider
    implements ElectronRepulsionIntegralProvider, AoElectronRepulsionValues {

  private static final int DEFAULT_CACHE_SIZE = 65_536;
  private static final double DEFAULT_SCREENING_TOLERANCE = 1.0E-13;

  private final IntegralMolecule molecule;
  private final GaussianBasisSet basisSet;
  private final GaussianIntegralEngine integralEngine;
  private final PrimitiveEriCache cache;
  private final double screeningTolerance;
  private final LongAdder valueQueryCount = new LongAdder();
  private final LongAdder screenedOutQueryCount = new LongAdder();
  private final LongAdder cacheHitCount = new LongAdder();
  private final LongAdder computedIntegralCount = new LongAdder();
  private final LongAdder pairBoundIntegralCount = new LongAdder();
  private final LongAdder pairBoundBuildCount = new LongAdder();
  private volatile double[] pairBounds;

  public DirectGaussianElectronRepulsionIntegralProvider(
      final IntegralMolecule molecule, final GaussianBasisSet basisSet) {
    this(molecule, basisSet, new GaussianIntegralEngine(), DEFAULT_CACHE_SIZE);
  }

  public DirectGaussianElectronRepulsionIntegralProvider(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final GaussianIntegralEngine integralEngine,
      final int cacheSize) {
    this(molecule, basisSet, integralEngine, cacheSize, DEFAULT_SCREENING_TOLERANCE);
  }

  public DirectGaussianElectronRepulsionIntegralProvider(
      final IntegralMolecule molecule,
      final GaussianBasisSet basisSet,
      final GaussianIntegralEngine integralEngine,
      final int cacheSize,
      final double screeningTolerance) {
    if (molecule == null) {
      throw new IllegalArgumentException("Direct Gaussian ERI molecule must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Direct Gaussian ERI basis set must not be null.");
    }
    if (integralEngine == null) {
      throw new IllegalArgumentException("Direct Gaussian ERI integral engine must not be null.");
    }
    if (cacheSize <= 0) {
      throw new IllegalArgumentException("Direct Gaussian ERI cache size must be positive.");
    }
    if (!Double.isFinite(screeningTolerance) || screeningTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Direct Gaussian ERI screening tolerance must be finite and non-negative.");
    }
    this.molecule = molecule;
    this.basisSet = basisSet;
    this.integralEngine = integralEngine;
    this.cache = new PrimitiveEriCache(cacheSize);
    this.screeningTolerance = screeningTolerance;
  }

  public double valueAt(final int first, final int second, final int third, final int fourth) {
    this.valueQueryCount.increment();
    if (this.screenedOut(first, second, third, fourth)) {
      this.screenedOutQueryCount.increment();
      return 0.0;
    }
    final long key =
        DirectGaussianElectronRepulsionIntegralProvider.normalizedKey(first, second, third, fourth);
    final double cached = this.cache.valueOrNaN(key);
    if (!Double.isNaN(cached)) {
      this.cacheHitCount.increment();
      return cached;
    }
    final double value =
        this.integralEngine.electronRepulsionAt(
            this.molecule, this.basisSet, first, second, third, fourth);
    this.cache.put(key, value);
    this.computedIntegralCount.increment();
    return value;
  }

  public double screenedInValueAt(
      final int first, final int second, final int third, final int fourth) {
    final long key =
        DirectGaussianElectronRepulsionIntegralProvider.normalizedKey(first, second, third, fourth);
    final double cached = this.cache.valueOrNaN(key);
    if (!Double.isNaN(cached)) {
      this.cacheHitCount.increment();
      return cached;
    }
    final double value =
        this.integralEngine.electronRepulsionAt(
            this.molecule, this.basisSet, first, second, third, fourth);
    this.cache.put(key, value);
    this.computedIntegralCount.increment();
    return value;
  }

  public boolean screenedOut(final int first, final int second, final int third, final int fourth) {
    if (this.screeningTolerance == 0.0) {
      return false;
    }
    final double[] bounds = this.ensurePairBounds();
    final int firstPair = DirectGaussianElectronRepulsionIntegralProvider.pairIndex(first, second);
    final int secondPair = DirectGaussianElectronRepulsionIntegralProvider.pairIndex(third, fourth);
    return bounds[firstPair] * bounds[secondPair] < this.screeningTolerance;
  }

  public double pairUpperBound(final int first, final int second) {
    final double[] bounds = this.ensurePairBounds();
    return bounds[DirectGaussianElectronRepulsionIntegralProvider.pairIndex(first, second)];
  }

  public DirectGaussianEriStatistics statistics() {
    return DirectGaussianEriStatistics.of(
        this.valueQueryCount.sum(),
        this.screenedOutQueryCount.sum(),
        this.cacheHitCount.sum(),
        this.computedIntegralCount.sum(),
        this.pairBoundIntegralCount.sum(),
        this.pairBoundBuildCount.sum(),
        this.cache.size(),
        this.cache.capacity(),
        this.screeningTolerance);
  }

  private double[] ensurePairBounds() {
    double[] bounds = this.pairBounds;
    if (bounds != null) {
      return bounds;
    }
    synchronized (this) {
      bounds = this.pairBounds;
      if (bounds == null) {
        this.pairBoundBuildCount.increment();
        bounds = this.computePairBounds();
        this.pairBounds = bounds;
      }
      return bounds;
    }
  }

  private double[] computePairBounds() {
    final int orbitalCount = this.basisSet.orbitalCount();
    final double[] bounds = new double[orbitalCount * (orbitalCount + 1) / 2];
    for (int first = 0; first < orbitalCount; ++first) {
      for (int second = 0; second <= first; ++second) {
        final int pair = DirectGaussianElectronRepulsionIntegralProvider.pairIndex(first, second);
        final double diagonal =
            this.integralEngine.electronRepulsionAt(
                this.molecule, this.basisSet, first, second, first, second);
        this.pairBoundIntegralCount.increment();
        bounds[pair] = Math.sqrt(Math.max(0.0, Math.abs(diagonal)));
      }
    }
    return bounds;
  }

  private static long normalizedKey(
      final int first, final int second, final int third, final int fourth) {
    final int firstPair = DirectGaussianElectronRepulsionIntegralProvider.pairIndex(first, second);
    final int secondPair = DirectGaussianElectronRepulsionIntegralProvider.pairIndex(third, fourth);
    final int left = Math.max(firstPair, secondPair);
    final int right = Math.min(firstPair, secondPair);
    return ((long) left << 32) | (right & 0xFFFF_FFFFL);
  }

  private static int pairIndex(final int first, final int second) {
    final int upper = Math.max(first, second);
    final int lower = Math.min(first, second);
    return upper * (upper + 1) / 2 + lower;
  }

  private static final class PrimitiveEriCache {

    private static final double LOAD_FACTOR = 0.65;
    private static final long EMPTY_KEY = Long.MIN_VALUE;

    private final long[] keys;
    private final double[] values;
    private final int mask;
    private final int rebuildThreshold;
    private int size;

    private PrimitiveEriCache(final int requestedSize) {
      final int capacity = PrimitiveEriCache.capacityFor(requestedSize);
      this.keys = new long[capacity];
      this.values = new double[capacity];
      this.mask = capacity - 1;
      this.rebuildThreshold = Math.max(1, (int) Math.floor(capacity * LOAD_FACTOR));
      this.clear();
    }

    private double valueOrNaN(final long key) {
      int index = PrimitiveEriCache.mix(key) & this.mask;
      while (true) {
        final long stored = this.keys[index];
        if (stored == EMPTY_KEY) {
          return Double.NaN;
        }
        if (stored == key) {
          return this.values[index];
        }
        index = (index + 1) & this.mask;
      }
    }

    private void put(final long key, final double value) {
      if (this.size >= this.rebuildThreshold) {
        this.clear();
      }
      int index = PrimitiveEriCache.mix(key) & this.mask;
      while (true) {
        final long stored = this.keys[index];
        if (stored == EMPTY_KEY) {
          this.keys[index] = key;
          this.values[index] = value;
          ++this.size;
          return;
        }
        if (stored == key) {
          this.values[index] = value;
          return;
        }
        index = (index + 1) & this.mask;
      }
    }

    private void clear() {
      for (int i = 0; i < this.keys.length; ++i) {
        this.keys[i] = EMPTY_KEY;
      }
      this.size = 0;
    }

    private int size() {
      return this.size;
    }

    private int capacity() {
      return this.keys.length;
    }

    private static int capacityFor(final int requestedSize) {
      int capacity = 1;
      final int minimum = Math.max(16, requestedSize * 2);
      while (capacity < minimum) {
        capacity <<= 1;
      }
      return capacity;
    }

    private static int mix(final long value) {
      long mixed = value;
      mixed ^= mixed >>> 33;
      mixed *= 0xff51afd7ed558ccdL;
      mixed ^= mixed >>> 33;
      mixed *= 0xc4ceb9fe1a85ec53L;
      mixed ^= mixed >>> 33;
      return (int) mixed;
    }
  }
}
