/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin;

import ru.pathcreator.vadim.quantum.library.builtin.arithmetic.ArithmeticAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.chemistry.ChemistryAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.diagnostic.DiagnosticAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.education.EntanglementAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.errorcorrection.ErrorCorrectionAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.oracle.OracleAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.protocol.ProtocolAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.search.SearchAlgorithms;
import ru.pathcreator.vadim.quantum.library.builtin.transform.TransformAlgorithms;
import ru.pathcreator.vadim.quantum.library.catalog.QuantumAlgorithmRegistry;

/**
 * Собирает встроенный каталог алгоритмов из тематических provider-классов.
 */
public final class BuiltInQuantumAlgorithmLibrary {

    private BuiltInQuantumAlgorithmLibrary() {
    }

    /**
     * Создает встроенный immutable-реестр алгоритмов.
     *
     * @return встроенный реестр
     */
    public static QuantumAlgorithmRegistry create() {
        return QuantumAlgorithmRegistry.builder()
            .add(EntanglementAlgorithms.bellState())
            .add(EntanglementAlgorithms.ghzState())
            .add(EntanglementAlgorithms.repetitionEncodingProbe())
            .add(TransformAlgorithms.quantumFourierTransform())
            .add(TransformAlgorithms.inverseQuantumFourierTransform())
            .add(TransformAlgorithms.phaseEstimation())
            .add(TransformAlgorithms.basisPreparation())
            .add(TransformAlgorithms.qftRoundTrip())
            .add(TransformAlgorithms.cliffordEcho())
            .add(OracleAlgorithms.deutschJozsa())
            .add(OracleAlgorithms.bernsteinVazirani())
            .add(OracleAlgorithms.deutschSingleBit())
            .add(OracleAlgorithms.parityKickback())
            .add(SearchAlgorithms.twoQubitGrover())
            .add(SearchAlgorithms.parameterizedTwoQubitGrover())
            .add(ProtocolAlgorithms.superdenseCoding())
            .add(ProtocolAlgorithms.basisStateTeleportation())
            .add(ArithmeticAlgorithms.halfAdder())
            .add(ArithmeticAlgorithms.parityChecker())
            .add(ErrorCorrectionAlgorithms.bitFlipCodeProbe())
            .add(ErrorCorrectionAlgorithms.phaseFlipEncodingProbe())
            .add(ChemistryAlgorithms.hydrogenVariationalAnsatz())
            .add(DiagnosticAlgorithms.standardGateSurfaceProbe())
            .add(DiagnosticAlgorithms.resetAndIdentityProbe())
            .build();
    }
}