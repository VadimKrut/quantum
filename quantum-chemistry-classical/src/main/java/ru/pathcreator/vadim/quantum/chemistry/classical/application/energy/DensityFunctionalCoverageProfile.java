/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalFamily;

/** Описывает честный Java-native контракт поддержки конкретного DFT functional. */
public final class DensityFunctionalCoverageProfile {

  private final String solverModel;
  private final String parityLevel;
  private final String approximationScope;
  private final String javaNativeGridContract;
  private final boolean gridModelAvailable;
  private final boolean completeLocalDensityGrid;
  private final boolean externalPackageParityClaimed;

  private DensityFunctionalCoverageProfile(
      final String solverModel,
      final String parityLevel,
      final String approximationScope,
      final String javaNativeGridContract,
      final boolean gridModelAvailable,
      final boolean completeLocalDensityGrid,
      final boolean externalPackageParityClaimed) {
    this.solverModel = solverModel;
    this.parityLevel = parityLevel;
    this.approximationScope = approximationScope;
    this.javaNativeGridContract = javaNativeGridContract;
    this.gridModelAvailable = gridModelAvailable;
    this.completeLocalDensityGrid = completeLocalDensityGrid;
    this.externalPackageParityClaimed = externalPackageParityClaimed;
  }

  public static DensityFunctionalCoverageProfile of(
      final DensityFunctionalDefinition functionalDefinition) {
    if (functionalDefinition == null) {
      throw new IllegalArgumentException("DFT functional definition must not be null.");
    }
    final boolean slaterExchangeOnly =
        DensityFunctionalCoverageProfile.slaterExchangeOnly(functionalDefinition);
    final boolean svwn = DensityFunctionalCoverageProfile.svwn(functionalDefinition);
    final boolean pbe = DensityFunctionalCoverageProfile.pbe(functionalDefinition);
    final boolean b3lyp = DensityFunctionalCoverageProfile.b3lyp(functionalDefinition);
    final boolean gridModelAvailable = slaterExchangeOnly || svwn || pbe || b3lyp;
    final boolean completeLocalDensityGrid = slaterExchangeOnly || svwn;
    return new DensityFunctionalCoverageProfile(
        DensityFunctionalCoverageProfile.solverModel(slaterExchangeOnly, svwn, pbe, b3lyp),
        DensityFunctionalCoverageProfile.parityLevel(
            functionalDefinition, completeLocalDensityGrid, pbe, b3lyp),
        DensityFunctionalCoverageProfile.approximationScope(
            functionalDefinition, completeLocalDensityGrid, pbe, b3lyp),
        DensityFunctionalCoverageProfile.javaNativeGridContract(
            completeLocalDensityGrid, gridModelAvailable),
        gridModelAvailable,
        completeLocalDensityGrid,
        false);
  }

  public String solverModel() {
    return this.solverModel;
  }

  public String parityLevel() {
    return this.parityLevel;
  }

  public String approximationScope() {
    return this.approximationScope;
  }

  public String javaNativeGridContract() {
    return this.javaNativeGridContract;
  }

  public boolean gridModelAvailable() {
    return this.gridModelAvailable;
  }

  public boolean completeLocalDensityGrid() {
    return this.completeLocalDensityGrid;
  }

  public boolean externalPackageParityClaimed() {
    return this.externalPackageParityClaimed;
  }

  public boolean resultPartialDiagnosticRequired() {
    return !this.completeLocalDensityGrid;
  }

  public String partialDiagnosticMessage(final DensityFunctionalDefinition functionalDefinition) {
    if (functionalDefinition == null) {
      throw new IllegalArgumentException("DFT functional definition must not be null.");
    }
    final DensityFunctionalFamily family = functionalDefinition.family();
    if (family == DensityFunctionalFamily.RANGE_SEPARATED_HYBRID
        || family == DensityFunctionalFamily.DOUBLE_HYBRID) {
      return "Java-native DFT uses an effective local AO Kohn-Sham model for this advanced"
          + " functional family.";
    }
    return "Java-native DFT uses atom-centered numerical grid only for local-density"
        + " functionals as complete local-grid models; this functional is evaluated by an"
        + " explicitly marked approximate model.";
  }

  private static String solverModel(
      final boolean slaterExchangeOnly,
      final boolean svwn,
      final boolean pbe,
      final boolean b3lyp) {
    if (slaterExchangeOnly) {
      return "ATOM_CENTERED_GRID_SLATER_EXCHANGE_KOHN_SHAM";
    }
    if (svwn) {
      return "ATOM_CENTERED_GRID_SVWN_KOHN_SHAM";
    }
    if (pbe) {
      return "ATOM_CENTERED_GRID_PBE_GGA_KOHN_SHAM";
    }
    if (b3lyp) {
      return "ATOM_CENTERED_GRID_B3LYP_B88_LYP_HYBRID_KOHN_SHAM";
    }
    return "LOCAL_AO_EFFECTIVE_KOHN_SHAM";
  }

  private static String parityLevel(
      final DensityFunctionalDefinition functionalDefinition,
      final boolean completeLocalDensityGrid,
      final boolean pbe,
      final boolean b3lyp) {
    if (completeLocalDensityGrid) {
      return "JAVA_NATIVE_LOCAL_DENSITY_GRID_COMPLETE";
    }
    if (pbe && functionalDefinition.exactExchangeFraction() <= 0.0) {
      return "JAVA_NATIVE_GGA_APPROXIMATE";
    }
    if (pbe || b3lyp) {
      return "JAVA_NATIVE_HYBRID_APPROXIMATE";
    }
    return "JAVA_NATIVE_EFFECTIVE_AO_FALLBACK";
  }

  private static String approximationScope(
      final DensityFunctionalDefinition functionalDefinition,
      final boolean completeLocalDensityGrid,
      final boolean pbe,
      final boolean b3lyp) {
    if (completeLocalDensityGrid) {
      return "local-density exchange/correlation is evaluated by the Java-native"
          + " atom-centered grid model; external package bit-for-bit parity is not claimed";
    }
    if (pbe && functionalDefinition.exactExchangeFraction() <= 0.0) {
      return "GGA density-gradient terms are represented by the Java-native approximate grid model";
    }
    if (pbe || b3lyp) {
      return "hybrid exact-exchange and gradient-correlation terms are represented by the"
          + " Java-native approximate grid model";
    }
    return "advanced functional terms are represented by the Java-native effective AO fallback model";
  }

  private static String javaNativeGridContract(
      final boolean completeLocalDensityGrid, final boolean gridModelAvailable) {
    if (completeLocalDensityGrid) {
      return "complete_local_density_grid";
    }
    if (gridModelAvailable) {
      return "approximate_semilocal_or_hybrid_grid";
    }
    return "effective_ao_fallback";
  }

  private static boolean slaterExchangeOnly(
      final DensityFunctionalDefinition functionalDefinition) {
    final String normalized = DensityFunctionalCoverageProfile.normalizedName(functionalDefinition);
    return normalized.equals("ldax") || normalized.equals("slater");
  }

  private static boolean svwn(final DensityFunctionalDefinition functionalDefinition) {
    final String normalized = DensityFunctionalCoverageProfile.normalizedName(functionalDefinition);
    return normalized.equals("svwn") || normalized.equals("vwn") || normalized.equals("lda");
  }

  private static boolean pbe(final DensityFunctionalDefinition functionalDefinition) {
    final String normalized = DensityFunctionalCoverageProfile.normalizedName(functionalDefinition);
    return normalized.equals("pbe") || normalized.equals("pbe0") || normalized.equals("pbeh");
  }

  private static boolean b3lyp(final DensityFunctionalDefinition functionalDefinition) {
    return DensityFunctionalCoverageProfile.normalizedName(functionalDefinition).equals("b3lyp");
  }

  private static String normalizedName(final DensityFunctionalDefinition functionalDefinition) {
    return functionalDefinition.name().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
  }
}
