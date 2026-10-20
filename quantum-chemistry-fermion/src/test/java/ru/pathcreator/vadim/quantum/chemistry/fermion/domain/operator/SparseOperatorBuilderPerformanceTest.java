/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;

public final class SparseOperatorBuilderPerformanceTest {

  @Test
  public void fermionBuilderHandlesCancelledSlotsAndRegrowth() {
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () -> {
          final FermionOperator.Builder builder = FermionOperator.builder();
          for (int i = 0; i < 4096; ++i) {
            final FermionTerm term =
                FermionTerm.of(
                    FermionTerm.creation(i & 63), FermionTerm.annihilation((i + 7) & 63));
            builder.add(term, ComplexCoefficient.real(1.0));
            builder.add(term, ComplexCoefficient.real(-1.0));
            if ((i & 7) == 0) {
              builder.add(term, ComplexCoefficient.real(0.25));
            }
          }
          final FermionOperator operator = builder.build();
          assertFalse(operator.empty());
          assertEquals(8, operator.termCount());
        });
  }

  @Test
  public void pauliBuilderHandlesCancelledSlotsAndRegrowth() {
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () -> {
          final QubitPauliOperator.Builder builder = QubitPauliOperator.builder();
          for (int i = 0; i < 4096; ++i) {
            final QubitPauliTerm term =
                QubitPauliTerm.of(
                    new int[] {i & 127, (i + 11) & 127, (i + 29) & 127},
                    new PauliAxis[] {PauliAxis.X, PauliAxis.Y, PauliAxis.Z});
            builder.add(term, ComplexCoefficient.of(1.0, -0.5));
            builder.add(term, ComplexCoefficient.of(-1.0, 0.5));
            if ((i & 15) == 0) {
              builder.add(term, ComplexCoefficient.of(0.125, 0.25));
            }
          }
          final QubitPauliOperator operator = builder.build();
          assertEquals(8, operator.termCount());
          assertEquals(126, operator.requiredQubitCount());
        });
  }
}
