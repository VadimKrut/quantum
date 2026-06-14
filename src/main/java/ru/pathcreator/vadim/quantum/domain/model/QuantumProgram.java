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

import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.source.ProgramSourceFragment;

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
    private final ArrayList<ClassicalDeclaration> classicalDeclarations;
    private final ArrayList<CallableDefinition> callableDefinitions;
    private final ArrayList<ExternalCallableDeclaration> externalCallableDeclarations;
    private final ArrayList<CalibrationDefinition> calibrationDefinitions;
    private final ArrayList<ProgramSourceFragment> sourceFragments;

    private QuantumProgram(final QuantumComputationModel computationModel) {
        this.computationModel = computationModel;
        this.circuits = new ArrayList<>();
        this.gateDefinitions = new ArrayList<>();
        this.classicalDeclarations = new ArrayList<>();
        this.callableDefinitions = new ArrayList<>();
        this.externalCallableDeclarations = new ArrayList<>();
        this.calibrationDefinitions = new ArrayList<>();
        this.sourceFragments = new ArrayList<>();
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
        ensureUniqueProgramSymbol(definition.gateName());
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
     * Добавляет классическое объявление уровня программы.
     *
     * @param declaration классическое объявление
     * @return текущая программа
     */
    public QuantumProgram addClassicalDeclaration(final ClassicalDeclaration declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("Classical declaration must not be null.");
        }
        ensureUniqueProgramSymbol(declaration.name());
        classicalDeclarations.add(declaration);
        return this;
    }

    /**
     * Возвращает количество классических объявлений.
     *
     * @return количество объявлений
     */
    public int classicalDeclarationCount() {
        return classicalDeclarations.size();
    }

    /**
     * Возвращает классическое объявление по индексу.
     *
     * @param index индекс объявления
     * @return классическое объявление
     */
    public ClassicalDeclaration classicalDeclaration(final int index) {
        validateIndex(
            index,
            classicalDeclarations.size(),
            "Classical declaration"
        );
        return classicalDeclarations.get(index);
    }

    /**
     * Возвращает неизменяемый снимок классических объявлений.
     *
     * @return объявления
     */
    public List<ClassicalDeclaration> classicalDeclarations() {
        return List.copyOf(classicalDeclarations);
    }

    /**
     * Добавляет подпрограмму уровня программы.
     *
     * @param definition подпрограмма
     * @return текущая программа
     */
    public QuantumProgram addCallableDefinition(final CallableDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Callable definition must not be null.");
        }
        ensureUniqueProgramSymbol(definition.name());
        callableDefinitions.add(definition);
        return this;
    }

    /**
     * Возвращает количество подпрограмм.
     *
     * @return количество подпрограмм
     */
    public int callableDefinitionCount() {
        return callableDefinitions.size();
    }

    /**
     * Возвращает подпрограмму по индексу.
     *
     * @param index индекс подпрограммы
     * @return подпрограмма
     */
    public CallableDefinition callableDefinition(final int index) {
        validateIndex(
            index,
            callableDefinitions.size(),
            "Callable definition"
        );
        return callableDefinitions.get(index);
    }

    /**
     * Возвращает неизменяемый снимок подпрограмм.
     *
     * @return подпрограммы
     */
    public List<CallableDefinition> callableDefinitions() {
        return List.copyOf(callableDefinitions);
    }

    /**
     * Добавляет внешнее объявление.
     *
     * @param declaration внешнее объявление
     * @return текущая программа
     */
    public QuantumProgram addExternalCallableDeclaration(final ExternalCallableDeclaration declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("External callable declaration must not be null.");
        }
        ensureUniqueProgramSymbol(declaration.name());
        externalCallableDeclarations.add(declaration);
        return this;
    }

    /**
     * Возвращает количество внешних объявлений.
     *
     * @return количество объявлений
     */
    public int externalCallableDeclarationCount() {
        return externalCallableDeclarations.size();
    }

    /**
     * Возвращает внешнее объявление по индексу.
     *
     * @param index индекс объявления
     * @return внешнее объявление
     */
    public ExternalCallableDeclaration externalCallableDeclaration(final int index) {
        validateIndex(
            index,
            externalCallableDeclarations.size(),
            "External callable declaration"
        );
        return externalCallableDeclarations.get(index);
    }

    /**
     * Возвращает неизменяемый снимок внешних объявлений.
     *
     * @return внешние объявления
     */
    public List<ExternalCallableDeclaration> externalCallableDeclarations() {
        return List.copyOf(externalCallableDeclarations);
    }

    /**
     * Добавляет калибровку.
     *
     * @param definition калибровка
     * @return текущая программа
     */
    public QuantumProgram addCalibrationDefinition(final CalibrationDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Calibration definition must not be null.");
        }
        calibrationDefinitions.add(definition);
        return this;
    }

    /**
     * Возвращает количество калибровок.
     *
     * @return количество калибровок
     */
    public int calibrationDefinitionCount() {
        return calibrationDefinitions.size();
    }

    /**
     * Возвращает калибровку по индексу.
     *
     * @param index индекс калибровки
     * @return калибровка
     */
    public CalibrationDefinition calibrationDefinition(final int index) {
        validateIndex(
            index,
            calibrationDefinitions.size(),
            "Calibration definition"
        );
        return calibrationDefinitions.get(index);
    }

    /**
     * Возвращает неизменяемый снимок калибровок.
     *
     * @return калибровки
     */
    public List<CalibrationDefinition> calibrationDefinitions() {
        return List.copyOf(calibrationDefinitions);
    }

    /**
     * Добавляет сохраненный фрагмент внешнего представления.
     *
     * @param fragment фрагмент внешнего представления
     * @return текущая программа
     */
    public QuantumProgram addSourceFragment(final ProgramSourceFragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("Program source fragment must not be null.");
        }
        sourceFragments.add(fragment);
        return this;
    }

    /**
     * Возвращает количество сохраненных фрагментов внешнего представления.
     *
     * @return количество фрагментов
     */
    public int sourceFragmentCount() {
        return sourceFragments.size();
    }

    /**
     * Возвращает сохраненный фрагмент внешнего представления по индексу.
     *
     * @param index индекс фрагмента
     * @return фрагмент внешнего представления
     */
    public ProgramSourceFragment sourceFragment(final int index) {
        validateIndex(
            index,
            sourceFragments.size(),
            "Program source fragment"
        );
        return sourceFragments.get(index);
    }

    /**
     * Возвращает неизменяемый снимок сохраненных фрагментов внешнего представления.
     *
     * @return сохраненные фрагменты
     */
    public List<ProgramSourceFragment> sourceFragments() {
        return List.copyOf(sourceFragments);
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
        validateIndex(
            index,
            gateDefinitions.size(),
            "Gate definition"
        );
    }

    private void validateIndex(
        final int index,
        final int size,
        final String subject
    ) {
        if (
            index < 0
            || index >= size
        ) {
            throw new IllegalArgumentException(subject + " index is outside of program bounds.");
        }
    }

    private void ensureUniqueProgramSymbol(final String name) {
        for (int i = 0; i < gateDefinitions.size(); i++) {
            if (gateDefinitions.get(i).gateName().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
        for (int i = 0; i < classicalDeclarations.size(); i++) {
            if (classicalDeclarations.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
        for (int i = 0; i < callableDefinitions.size(); i++) {
            if (callableDefinitions.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
        for (int i = 0; i < externalCallableDeclarations.size(); i++) {
            if (externalCallableDeclarations.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
    }
}