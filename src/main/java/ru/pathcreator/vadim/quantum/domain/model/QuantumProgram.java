/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;

/**
 * Квантовая программа, содержащая вычислительную модель и набор схем.
 */
public final class QuantumProgram {

    /**
     * Вычислительная модель программы.
     */
    private final QuantumComputationModel computationModel;

    /**
     * Схемы, созданные внутри программы.
     */
    private final ArrayList<QuantumCircuit> circuits;

    /**
     * Пользовательские gate definitions, доступные схемам программы.
     */
    private final ArrayList<GateDefinition> gateDefinitions;

    private QuantumProgram(final QuantumComputationModel computationModel) {
        this.computationModel = computationModel;
        this.circuits = new ArrayList<>();
        this.gateDefinitions = new ArrayList<>();
    }

    /**
     * Создает программу для gate-based quantum circuits.
     *
     * @return gate-based квантовая программа
     */
    public static QuantumProgram gateBased() {
        return create(QuantumComputationModel.GATE_BASED_CIRCUIT);
    }

    /**
     * Создает программу для указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель программы
     * @return квантовая программа
     */
    public static QuantumProgram create(final QuantumComputationModel computationModel) {
        if (computationModel == null) {
            throw new IllegalArgumentException("Quantum computation model must not be null.");
        }
        return new QuantumProgram(computationModel);
    }

    /**
     * Возвращает вычислительную модель программы.
     *
     * @return вычислительная модель
     */
    public QuantumComputationModel computationModel() {
        return computationModel;
    }

    /**
     * Создает схему внутри программы.
     *
     * @param name имя схемы
     * @return созданная схема
     */
    public QuantumCircuit createCircuit(final String name) {
        ensureGateBased();
        final QuantumCircuit circuit = QuantumCircuit.create(
            this,
            CircuitName.of(name)
        );
        circuits.add(circuit);
        return circuit;
    }

    /**
     * Добавляет gate definition в программу.
     *
     * @param definition описание гейта
     * @return текущая программа
     */
    public QuantumProgram addGateDefinition(final GateDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Gate definition must not be null.");
        }
        for (int i = 0; i < gateDefinitions.size(); i++) {
            if (gateDefinitions.get(i).name().equals(definition.name())) {
                throw new IllegalArgumentException("Gate definition name already exists in program.");
            }
        }
        gateDefinitions.add(definition);
        return this;
    }

    /**
     * Возвращает количество gate definitions.
     *
     * @return количество gate definitions
     */
    public int gateDefinitionCount() {
        return gateDefinitions.size();
    }

    /**
     * Возвращает gate definition по индексу.
     *
     * @param index индекс gate definition
     * @return gate definition
     */
    public GateDefinition gateDefinition(final int index) {
        validateGateDefinitionIndex(index);
        return gateDefinitions.get(index);
    }

    /**
     * Возвращает неизменяемый снимок описаний гейтов.
     *
     * @return список gate definitions
     */
    public List<GateDefinition> gateDefinitions() {
        return List.copyOf(gateDefinitions);
    }

    /**
     * Возвращает количество схем в программе.
     *
     * @return количество схем
     */
    public int circuitCount() {
        return circuits.size();
    }

    /**
     * Возвращает схему по индексу.
     *
     * @param index индекс схемы
     * @return схема
     */
    public QuantumCircuit circuit(final int index) {
        validateCircuitIndex(index);
        return circuits.get(index);
    }

    /**
     * Возвращает неизменяемый снимок списка схем.
     *
     * @return список схем
     */
    public List<QuantumCircuit> circuits() {
        return List.copyOf(circuits);
    }

    private void ensureGateBased() {
        if (computationModel != QuantumComputationModel.GATE_BASED_CIRCUIT) {
            throw new IllegalStateException("Only GATE_BASED_CIRCUIT programs can create quantum circuits at this stage.");
        }
    }

    private void validateCircuitIndex(final int index) {
        if (
            index < 0
            || index >= circuits.size()
        ) {
            throw new IllegalArgumentException("Circuit index is outside of program bounds.");
        }
    }

    private void validateGateDefinitionIndex(final int index) {
        if (
            index < 0
            || index >= gateDefinitions.size()
        ) {
            throw new IllegalArgumentException("Gate definition index is outside of program bounds.");
        }
    }
}