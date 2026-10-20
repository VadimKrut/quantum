/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryYieldCalculation;

/** Workbench adapter for the calculation also used by the exported report. */
final class LaboratoryYieldSummary {
  record Value(BigDecimal percent, String label, String qualification, String explanation) {
    String text() {
      return new LaboratoryYieldCalculation.Value(percent, label, qualification, explanation)
          .text();
    }
  }

  private LaboratoryYieldSummary() {}

  static LaboratoryYieldCalculation.Reference automaticReference(
      LaboratoryExperiment e, LaboratoryReactionStage s, Map<String, BigDecimal> masses) {
    return LaboratoryYieldCalculation.automaticReference(e, s, masses);
  }

  static BigDecimal theoreticalMoles(
      LaboratoryExperiment e,
      LaboratoryReactionStage s,
      String reference,
      String product,
      Map<String, BigDecimal> masses) {
    return LaboratoryYieldCalculation.theoreticalMoles(e, s, reference, product, masses);
  }

  static Value calculate(
      LaboratoryExperiment e,
      LaboratoryReactionStage s,
      String reference,
      String product,
      Map<String, BigDecimal> masses) {
    final var v = LaboratoryYieldCalculation.calculate(e, s, reference, product, masses);
    return new Value(v.percent(), v.label(), v.qualification(), v.explanation());
  }
}
