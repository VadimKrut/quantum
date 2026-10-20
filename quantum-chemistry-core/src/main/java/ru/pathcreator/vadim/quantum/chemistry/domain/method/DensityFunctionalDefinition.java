/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Структурное описание DFT-функционала без привязки к конкретному расчетному backend. */
public final class DensityFunctionalDefinition {

  private static final double MIN_FRACTION = 0.0;
  private static final double MAX_FRACTION = 1.0;

  private final String name;
  private final DensityFunctionalFamily family;
  private final double exactExchangeFraction;
  private final double perturbativeCorrelationFraction;
  private final boolean gradientCorrected;
  private final boolean dispersionCorrected;
  private final String description;

  private DensityFunctionalDefinition(
      final String name,
      final DensityFunctionalFamily family,
      final double exactExchangeFraction,
      final double perturbativeCorrelationFraction,
      final boolean gradientCorrected,
      final boolean dispersionCorrected,
      final String description) {
    this.name = name;
    this.family = family;
    this.exactExchangeFraction = exactExchangeFraction;
    this.perturbativeCorrelationFraction = perturbativeCorrelationFraction;
    this.gradientCorrected = gradientCorrected;
    this.dispersionCorrected = dispersionCorrected;
    this.description = description;
  }

  public static DensityFunctionalDefinition of(
      final String name,
      final DensityFunctionalFamily family,
      final double exactExchangeFraction,
      final double perturbativeCorrelationFraction,
      final boolean gradientCorrected,
      final boolean dispersionCorrected,
      final String description) {
    final String checkedName = TextValue.requireText(name, "DFT functional name");
    if (family == null) {
      throw new IllegalArgumentException("DFT functional family must not be null.");
    }
    DensityFunctionalDefinition.requireFraction(
        exactExchangeFraction, "DFT exact exchange fraction");
    DensityFunctionalDefinition.requireFraction(
        perturbativeCorrelationFraction, "DFT perturbative correlation fraction");
    final String checkedDescription =
        description == null || description.isBlank()
            ? checkedName + " density functional"
            : TextValue.requireText(description, "DFT functional description");
    return new DensityFunctionalDefinition(
        checkedName,
        family,
        exactExchangeFraction,
        perturbativeCorrelationFraction,
        gradientCorrected,
        dispersionCorrected,
        checkedDescription);
  }

  public static DensityFunctionalDefinition localDensity(final String name) {
    return DensityFunctionalDefinition.of(
        name,
        DensityFunctionalFamily.LOCAL_DENSITY,
        0.0,
        0.0,
        false,
        false,
        "Local-density approximation functional");
  }

  public static DensityFunctionalDefinition generalizedGradient(final String name) {
    return DensityFunctionalDefinition.of(
        name,
        DensityFunctionalFamily.GENERALIZED_GRADIENT,
        0.0,
        0.0,
        true,
        false,
        "Generalized-gradient approximation functional");
  }

  public static DensityFunctionalDefinition hybridGga(
      final String name, final double exactExchangeFraction) {
    return DensityFunctionalDefinition.of(
        name,
        DensityFunctionalFamily.HYBRID_GGA,
        exactExchangeFraction,
        0.0,
        true,
        false,
        "Hybrid generalized-gradient approximation functional");
  }

  public static DensityFunctionalDefinition resolve(final String functionalName) {
    final String checkedName = TextValue.requireText(functionalName, "DFT functional name");
    final String normalized = checkedName.toLowerCase(Locale.ROOT).replace("-", "");
    if (normalized.equals("lda")
        || normalized.equals("ldax")
        || normalized.equals("lda_x")
        || normalized.equals("svwn")
        || normalized.equals("slater")) {
      return DensityFunctionalDefinition.localDensity(checkedName);
    }
    if (normalized.equals("pbe") || normalized.equals("blyp") || normalized.equals("bp86")) {
      return DensityFunctionalDefinition.generalizedGradient(checkedName);
    }
    if (normalized.equals("b3lyp")) {
      return DensityFunctionalDefinition.hybridGga(checkedName, 0.20);
    }
    if (normalized.equals("pbe0") || normalized.equals("pbeh")) {
      return DensityFunctionalDefinition.hybridGga(checkedName, 0.25);
    }
    if (normalized.equals("m062x")) {
      return DensityFunctionalDefinition.of(
          checkedName,
          DensityFunctionalFamily.HYBRID_META_GGA,
          0.54,
          0.0,
          true,
          false,
          "Hybrid meta-GGA Minnesota-family functional");
    }
    if (normalized.equals("wb97x")
        || normalized.equals("cam-b3lyp")
        || normalized.equals("camb3lyp")) {
      return DensityFunctionalDefinition.of(
          checkedName,
          DensityFunctionalFamily.RANGE_SEPARATED_HYBRID,
          0.22,
          0.0,
          true,
          false,
          "Range-separated hybrid density functional");
    }
    if (normalized.equals("b2plyp")) {
      return DensityFunctionalDefinition.of(
          checkedName,
          DensityFunctionalFamily.DOUBLE_HYBRID,
          0.53,
          0.27,
          true,
          false,
          "Double-hybrid density functional");
    }
    return DensityFunctionalDefinition.of(
        checkedName,
        DensityFunctionalFamily.CUSTOM,
        0.0,
        0.0,
        true,
        false,
        "Custom density functional without backend-specific parameters");
  }

  public String name() {
    return this.name;
  }

  public DensityFunctionalFamily family() {
    return this.family;
  }

  public double exactExchangeFraction() {
    return this.exactExchangeFraction;
  }

  public double perturbativeCorrelationFraction() {
    return this.perturbativeCorrelationFraction;
  }

  public boolean gradientCorrected() {
    return this.gradientCorrected;
  }

  public boolean dispersionCorrected() {
    return this.dispersionCorrected;
  }

  public String description() {
    return this.description;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DensityFunctionalDefinition)) {
      return false;
    }
    final DensityFunctionalDefinition definition = (DensityFunctionalDefinition) other;
    return this.name.equals(definition.name)
        && this.family == definition.family
        && Double.compare(this.exactExchangeFraction, definition.exactExchangeFraction) == 0
        && Double.compare(
                this.perturbativeCorrelationFraction, definition.perturbativeCorrelationFraction)
            == 0
        && this.gradientCorrected == definition.gradientCorrected
        && this.dispersionCorrected == definition.dispersionCorrected
        && this.description.equals(definition.description);
  }

  public int hashCode() {
    int result = this.name.hashCode();
    result = 31 * result + this.family.hashCode();
    result = 31 * result + Double.hashCode(this.exactExchangeFraction);
    result = 31 * result + Double.hashCode(this.perturbativeCorrelationFraction);
    result = 31 * result + Boolean.hashCode(this.gradientCorrected);
    result = 31 * result + Boolean.hashCode(this.dispersionCorrected);
    result = 31 * result + this.description.hashCode();
    return result;
  }

  private static void requireFraction(final double value, final String subjectName) {
    if (!Double.isFinite(value)
        || value < DensityFunctionalDefinition.MIN_FRACTION
        || value > DensityFunctionalDefinition.MAX_FRACTION) {
      throw new IllegalArgumentException(subjectName + " must be finite and in [0, 1].");
    }
  }
}
