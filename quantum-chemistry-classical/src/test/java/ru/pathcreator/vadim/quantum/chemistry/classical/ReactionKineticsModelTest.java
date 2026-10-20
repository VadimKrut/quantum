/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.ReactionKineticsModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionBondChangeSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionKineticEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model.ReactionPathwayEstimate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;

public final class ReactionKineticsModelTest {

  @Test
  void estimatesRateConstantsHalfLifeAndDurationConversion() {
    final ReactionPathwayEstimate pathway =
        ReactionKineticsModelTest.pathway(318.15, 42.0, 65.0, 0.02, 0.0003);
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(318.15, TemperatureUnit.KELVIN),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            java.util.List.of(),
            java.util.List.of(),
            null);

    final ReactionKineticEstimate estimate =
        new ReactionKineticsModel().estimate(pathway, conditions);

    Assertions.assertEquals(42.0, estimate.forwardBarrierKilojoulePerMole(), 1.0E-12);
    Assertions.assertEquals(65.0, estimate.reverseBarrierKilojoulePerMole(), 1.0E-12);
    Assertions.assertEquals(0.02, estimate.relativeForwardRate(), 1.0E-12);
    Assertions.assertEquals(0.0003, estimate.relativeReverseRate(), 1.0E-12);
    Assertions.assertTrue(estimate.forwardReverseRateRatio() > 1.0);
    Assertions.assertTrue(estimate.arrheniusForwardRateConstant() > 0.0);
    Assertions.assertTrue(estimate.eyringForwardRateConstant() > 0.0);
    Assertions.assertTrue(estimate.firstOrderHalfLifeSeconds() > 0.0);
    Assertions.assertTrue(estimate.hasDurationConversion());
    Assertions.assertTrue(estimate.durationConversionFraction() >= 0.0);
    Assertions.assertTrue(estimate.durationConversionFraction() <= 1.0);
  }

  @Test
  void omitsDurationConversionWhenNoReactionDurationExists() {
    final ReactionPathwayEstimate pathway =
        ReactionKineticsModelTest.pathway(298.15, 80.0, 90.0, 0.001, 0.0001);

    final ReactionKineticEstimate estimate =
        new ReactionKineticsModel().estimate(pathway, ReactionConditions.EMPTY);

    Assertions.assertFalse(estimate.hasDurationConversion());
    Assertions.assertThrows(IllegalStateException.class, estimate::durationConversionFraction);
  }

  private static ReactionPathwayEstimate pathway(
      final double temperatureKelvin,
      final double forwardBarrier,
      final double reverseBarrier,
      final double forwardRate,
      final double reverseRate) {
    return ReactionPathwayEstimate.of(
        ReactionBondChangeSet.of(2, 2, 1, 1, 350.0, 360.0),
        null,
        temperatureKelvin,
        4.0,
        forwardBarrier,
        reverseBarrier,
        -10.0,
        2.7,
        forwardRate,
        reverseRate,
        "accessible");
  }
}
