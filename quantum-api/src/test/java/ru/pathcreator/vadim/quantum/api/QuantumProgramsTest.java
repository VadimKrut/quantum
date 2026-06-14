/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantumProgramsTest {

    @Test
    void createsGateBasedProgram() {
        final QuantumProgram program = QuantumPrograms.gateBased();

        assertEquals(
            QuantumComputationModel.GATE_BASED_CIRCUIT,
            program.computationModel()
        );
    }

    @Test
    void createsProgramForExplicitModel() {
        final QuantumProgram program = QuantumPrograms.create(QuantumComputationModel.PHOTONIC);

        assertEquals(
            QuantumComputationModel.PHOTONIC,
            program.computationModel()
        );
    }
}