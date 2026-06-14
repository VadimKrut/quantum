/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import java.util.List;

import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Fluent builder для полного program-level Quantum IR из обычного Java-кода.
 */
public final class QuantumProgramBuilder {

    private final QuantumProgram program;

    private QuantumProgramBuilder(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        this.program = program;
    }

    /**
     * Создает builder для gate-based Quantum IR программы.
     *
     * @return builder программы
     */
    public static QuantumProgramBuilder gateBased() {
        return new QuantumProgramBuilder(QuantumProgram.gateBased());
    }

    /**
     * Создает builder для программы указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель
     * @return builder программы
     */
    public static QuantumProgramBuilder create(final QuantumComputationModel computationModel) {
        return new QuantumProgramBuilder(QuantumProgram.create(computationModel));
    }

    /**
     * Добавляет готовое описание gate на уровень программы.
     *
     * @param definition описание gate
     * @return текущий builder программы
     */
    public QuantumProgramBuilder gateDefinition(final GateDefinition definition) {
        program.addGateDefinition(definition);
        return this;
    }

    /**
     * Добавляет intrinsic gate definition.
     *
     * @param name имя gate
     * @param arity количество quantum-аргументов
     * @param parameterCount количество параметров
     * @return текущий builder программы
     */
    public QuantumProgramBuilder intrinsicGate(
        final String name,
        final int arity,
        final int parameterCount
    ) {
        return gateDefinition(GateDefinition.of(
            name,
            arity,
            parameterCount
        ));
    }

    /**
     * Добавляет intrinsic gate definition с правилами валидации.
     *
     * @param name имя gate
     * @param arity количество quantum-аргументов
     * @param parameterCount количество параметров
     * @param validationRules правила валидации
     * @return текущий builder программы
     */
    public QuantumProgramBuilder intrinsicGate(
        final String name,
        final int arity,
        final int parameterCount,
        final List<GateValidationRule> validationRules
    ) {
        return gateDefinition(GateDefinition.of(
            name,
            arity,
            parameterCount,
            validationRules
        ));
    }

    /**
     * Добавляет opaque/external gate declaration.
     *
     * @param name имя gate
     * @param parameterNames имена параметров
     * @param qubitNames имена quantum-аргументов
     * @return текущий builder программы
     */
    public QuantumProgramBuilder opaqueGate(
        final String name,
        final List<String> parameterNames,
        final List<String> qubitNames
    ) {
        return gateDefinition(GateDefinition.opaque(
            name,
            parameterNames,
            qubitNames
        ));
    }

    /**
     * Добавляет composite gate definition.
     *
     * @param name имя gate
     * @param parameterNames имена параметров
     * @param qubitNames имена quantum-аргументов
     * @param bodyOperations операции тела gate
     * @return текущий builder программы
     */
    public QuantumProgramBuilder compositeGate(
        final String name,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final List<GateBodyOperation> bodyOperations
    ) {
        return gateDefinition(GateDefinition.composite(
            name,
            parameterNames,
            qubitNames,
            bodyOperations
        ));
    }

    /**
     * Добавляет matrix gate definition.
     *
     * @param name имя gate
     * @param parameterNames имена параметров
     * @param qubitNames имена quantum-аргументов
     * @param matrix матрица gate
     * @return текущий builder программы
     */
    public QuantumProgramBuilder matrixGate(
        final String name,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final GateMatrix matrix
    ) {
        return gateDefinition(GateDefinition.matrix(
            name,
            parameterNames,
            qubitNames,
            matrix
        ));
    }

    /**
     * Добавляет program-level classical declaration.
     *
     * @param declaration классическое объявление
     * @return текущий builder программы
     */
    public QuantumProgramBuilder classicalDeclaration(final ClassicalDeclaration declaration) {
        program.addClassicalDeclaration(declaration);
        return this;
    }

    /**
     * Добавляет program-level classical declaration.
     *
     * @param name имя объявления
     * @param type тип объявления
     * @return текущий builder программы
     */
    public QuantumProgramBuilder classicalDeclaration(
        final String name,
        final ClassicalType type
    ) {
        return classicalDeclaration(new ClassicalDeclaration(
            name,
            type
        ));
    }

    /**
     * Добавляет callable definition.
     *
     * @param definition callable definition
     * @return текущий builder программы
     */
    public QuantumProgramBuilder callableDefinition(final CallableDefinition definition) {
        program.addCallableDefinition(definition);
        return this;
    }

    /**
     * Добавляет callable definition.
     *
     * @param name имя callable
     * @param body тело callable
     * @param arguments аргументы callable
     * @return текущий builder программы
     */
    public QuantumProgramBuilder callableDefinition(
        final String name,
        final CallableOperationBlock body,
        final CallableArgument... arguments
    ) {
        return callableDefinition(new CallableDefinition(
            name,
            body,
            arguments
        ));
    }

    /**
     * Добавляет external callable declaration.
     *
     * @param declaration external callable declaration
     * @return текущий builder программы
     */
    public QuantumProgramBuilder externalCallableDeclaration(final ExternalCallableDeclaration declaration) {
        program.addExternalCallableDeclaration(declaration);
        return this;
    }

    /**
     * Добавляет external callable declaration.
     *
     * @param name имя external callable
     * @param returnType возвращаемый тип или null
     * @param arguments аргументы external callable
     * @return текущий builder программы
     */
    public QuantumProgramBuilder externalCallableDeclaration(
        final String name,
        final ClassicalType returnType,
        final CallableArgument... arguments
    ) {
        return externalCallableDeclaration(new ExternalCallableDeclaration(
            name,
            returnType,
            arguments
        ));
    }

    /**
     * Добавляет calibration definition.
     *
     * @param definition calibration definition
     * @return текущий builder программы
     */
    public QuantumProgramBuilder calibrationDefinition(final CalibrationDefinition definition) {
        program.addCalibrationDefinition(definition);
        return this;
    }

    /**
     * Добавляет calibration definition.
     *
     * @param targetName имя калибруемой операции
     * @param parameterNames имена параметров
     * @param qubitNames имена quantum-аргументов
     * @param bodyLanguage язык тела
     * @param body тело calibration
     * @return текущий builder программы
     */
    public QuantumProgramBuilder calibrationDefinition(
        final String targetName,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final String bodyLanguage,
        final String body
    ) {
        return calibrationDefinition(new CalibrationDefinition(
            targetName,
            parameterNames,
            qubitNames,
            bodyLanguage,
            body
        ));
    }

    /**
     * Создает новую схему внутри программы и возвращает builder этой схемы.
     *
     * @param name имя схемы
     * @return builder схемы
     */
    public QuantumCircuitBuilder circuit(final String name) {
        return new QuantumCircuitBuilder(
            this,
            program,
            program.createCircuit(name)
        );
    }

    /**
     * Возвращает собранную Quantum IR программу.
     *
     * @return Quantum IR программа
     */
    public QuantumProgram build() {
        return program;
    }
}