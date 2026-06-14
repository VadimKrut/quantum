/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

/**
 * Тип квантовой вычислительной модели, для которой строится программа.
 */
public enum QuantumComputationModel {

    /**
     * Модель вентильных квантовых схем. Единственная реализуемая модель текущего этапа.
     */
    GATE_BASED_CIRCUIT,

    /**
     * Квантовый отжиг. Значение зарезервировано для будущих модулей.
     */
    QUANTUM_ANNEALING,

    /**
     * Аналоговая квантовая симуляция. Значение зарезервировано для будущих модулей.
     */
    ANALOG_SIMULATION,

    /**
     * Фотонные вычисления. Значение зарезервировано для будущих модулей.
     */
    PHOTONIC,

    /**
     * Импульсный уровень управления. Значение зарезервировано для будущих модулей.
     */
    PULSE_LEVEL
}