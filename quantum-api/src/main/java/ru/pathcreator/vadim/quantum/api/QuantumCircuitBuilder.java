/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import java.util.LinkedHashMap;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;

/**
 * Fluent builder одной gate-based схемы с обращением к битам через ссылки вида `q[0]` и `c[0]`.
 */
public final class QuantumCircuitBuilder {

    private final QuantumProgramBuilder owner;
    private final QuantumProgram program;
    private final QuantumCircuit circuit;
    private final LinkedHashMap<String, QuantumRegister> quantumRegisters;
    private final LinkedHashMap<String, ClassicalRegister> classicalRegisters;

    QuantumCircuitBuilder(
        final QuantumProgramBuilder owner,
        final QuantumProgram program,
        final QuantumCircuit circuit
    ) {
        if (owner == null) {
            throw new IllegalArgumentException("Quantum program builder must not be null.");
        }
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (circuit == null) {
            throw new IllegalArgumentException("Quantum circuit must not be null.");
        }
        this.owner = owner;
        this.program = program;
        this.circuit = circuit;
        this.quantumRegisters = new LinkedHashMap<>();
        this.classicalRegisters = new LinkedHashMap<>();
    }

    /**
     * Создает квантовый регистр.
     *
     * @param name имя регистра
     * @param size количество кубитов
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder qreg(
        final String name,
        final int size
    ) {
        ensureQuantumRegisterNameAvailable(name);
        quantumRegisters.put(
            name,
            circuit.createQuantumRegister(
                name,
                size
            )
        );
        return this;
    }

    /**
     * Создает классический регистр.
     *
     * @param name имя регистра
     * @param size количество битов
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder creg(
        final String name,
        final int size
    ) {
        ensureClassicalRegisterNameAvailable(name);
        classicalRegisters.put(
            name,
            circuit.createClassicalRegister(
                name,
                size
            )
        );
        return this;
    }

    /**
     * Добавляет gate h к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder h(final String qubit) {
        circuit.h(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate x к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder x(final String qubit) {
        circuit.x(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate y к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder y(final String qubit) {
        circuit.y(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate z к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder z(final String qubit) {
        circuit.z(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate s к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder s(final String qubit) {
        circuit.s(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate sdg к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder sdg(final String qubit) {
        circuit.sdg(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate t к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder t(final String qubit) {
        circuit.t(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate tdg к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder tdg(final String qubit) {
        circuit.tdg(qubit(qubit));
        return this;
    }

    /**
     * Добавляет gate rx с числовым углом.
     *
     * @param angle угол поворота
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder rx(
        final double angle,
        final String qubit
    ) {
        return rx(
            ParameterExpression.of(angle),
            qubit
        );
    }

    /**
     * Добавляет gate rx с выражением угла.
     *
     * @param angle выражение угла
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder rx(
        final ParameterExpression angle,
        final String qubit
    ) {
        circuit.rx(
            angle,
            qubit(qubit)
        );
        return this;
    }

    /**
     * Добавляет gate ry с числовым углом.
     *
     * @param angle угол поворота
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder ry(
        final double angle,
        final String qubit
    ) {
        return ry(
            ParameterExpression.of(angle),
            qubit
        );
    }

    /**
     * Добавляет gate ry с выражением угла.
     *
     * @param angle выражение угла
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder ry(
        final ParameterExpression angle,
        final String qubit
    ) {
        circuit.ry(
            angle,
            qubit(qubit)
        );
        return this;
    }

    /**
     * Добавляет gate rz с числовым углом.
     *
     * @param angle угол поворота
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder rz(
        final double angle,
        final String qubit
    ) {
        return rz(
            ParameterExpression.of(angle),
            qubit
        );
    }

    /**
     * Добавляет gate rz с выражением угла.
     *
     * @param angle выражение угла
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder rz(
        final ParameterExpression angle,
        final String qubit
    ) {
        circuit.rz(
            angle,
            qubit(qubit)
        );
        return this;
    }

    /**
     * Добавляет phase gate с числовым углом.
     *
     * @param angle угол фазы
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder phase(
        final double angle,
        final String qubit
    ) {
        return phase(
            ParameterExpression.of(angle),
            qubit
        );
    }

    /**
     * Добавляет phase gate с выражением угла.
     *
     * @param angle выражение угла
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder phase(
        final ParameterExpression angle,
        final String qubit
    ) {
        circuit.phase(
            angle,
            qubit(qubit)
        );
        return this;
    }

    /**
     * Добавляет identity gate к указанному кубиту.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder id(final String qubit) {
        circuit.id(qubit(qubit));
        return this;
    }

    /**
     * Добавляет controlled-x gate.
     *
     * @param control ссылка на управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder cx(
        final String control,
        final String target
    ) {
        circuit.cx(
            qubit(control),
            qubit(target)
        );
        return this;
    }

    /**
     * Добавляет controlled-y gate.
     *
     * @param control ссылка на управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder cy(
        final String control,
        final String target
    ) {
        circuit.cy(
            qubit(control),
            qubit(target)
        );
        return this;
    }

    /**
     * Добавляет controlled-z gate.
     *
     * @param control ссылка на управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder cz(
        final String control,
        final String target
    ) {
        circuit.cz(
            qubit(control),
            qubit(target)
        );
        return this;
    }

    /**
     * Добавляет controlled-h gate.
     *
     * @param control ссылка на управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder ch(
        final String control,
        final String target
    ) {
        circuit.ch(
            qubit(control),
            qubit(target)
        );
        return this;
    }

    /**
     * Добавляет swap gate.
     *
     * @param left ссылка на первый кубит
     * @param right ссылка на второй кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder swap(
        final String left,
        final String right
    ) {
        circuit.swap(
            qubit(left),
            qubit(right)
        );
        return this;
    }

    /**
     * Добавляет ccx gate.
     *
     * @param firstControl ссылка на первый управляющий кубит
     * @param secondControl ссылка на второй управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder ccx(
        final String firstControl,
        final String secondControl,
        final String target
    ) {
        circuit.ccx(
            qubit(firstControl),
            qubit(secondControl),
            qubit(target)
        );
        return this;
    }

    /**
     * Добавляет controlled phase gate с числовым углом.
     *
     * @param angle угол фазы
     * @param control ссылка на управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder cphase(
        final double angle,
        final String control,
        final String target
    ) {
        return cphase(
            ParameterExpression.of(angle),
            control,
            target
        );
    }

    /**
     * Добавляет controlled phase gate с выражением угла.
     *
     * @param angle выражение угла
     * @param control ссылка на управляющий кубит
     * @param target ссылка на целевой кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder cphase(
        final ParameterExpression angle,
        final String control,
        final String target
    ) {
        circuit.parameterizedGate(
            StandardGate.CPHASE,
            new ParameterExpression[] {angle},
            qubit(control),
            qubit(target)
        );
        return this;
    }

    /**
     * Добавляет универсальный однокубитный gate u.
     *
     * @param theta первый параметр
     * @param phi второй параметр
     * @param lambda третий параметр
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder u(
        final ParameterExpression theta,
        final ParameterExpression phi,
        final ParameterExpression lambda,
        final String qubit
    ) {
        circuit.parameterizedGate(
            StandardGate.U,
            new ParameterExpression[] {
                theta,
                phi,
                lambda
            },
            qubit(qubit)
        );
        return this;
    }

    /**
     * Добавляет произвольный непараметризованный gate.
     *
     * @param gate gate из доменной модели
     * @param qubits ссылки на кубиты
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder gate(
        final Gate gate,
        final String... qubits
    ) {
        circuit.gate(
            gate,
            qubits(qubits)
        );
        return this;
    }

    /**
     * Добавляет произвольный параметризованный gate.
     *
     * @param gate gate из доменной модели
     * @param parameters параметры gate
     * @param qubits ссылки на кубиты
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder parameterizedGate(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String... qubits
    ) {
        circuit.parameterizedGate(
            gate,
            parameters,
            qubits(qubits)
        );
        return this;
    }

    /**
     * Добавляет произвольный gate через универсальные quantum references.
     *
     * @param gate gate из доменной модели
     * @param references ссылки на quantum-аргументы
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder gateReferences(
        final Gate gate,
        final QuantumReference... references
    ) {
        circuit.gateReferences(
            gate,
            references
        );
        return this;
    }

    /**
     * Добавляет произвольный параметризованный gate через универсальные quantum references.
     *
     * @param gate gate из доменной модели
     * @param parameters параметры gate
     * @param references ссылки на quantum-аргументы
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder parameterizedGateReferences(
        final Gate gate,
        final ParameterExpression[] parameters,
        final QuantumReference... references
    ) {
        circuit.parameterizedGateReferences(
            gate,
            parameters,
            references
        );
        return this;
    }

    /**
     * Добавляет измерение кубита в классический бит.
     *
     * @param qubit ссылка на кубит
     * @param bit ссылка на классический бит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder measure(
        final String qubit,
        final String bit
    ) {
        circuit.measure(
            qubit(qubit),
            bit(bit)
        );
        return this;
    }

    /**
     * Добавляет измерение quantum reference в классический бит.
     *
     * @param reference quantum reference
     * @param bit ссылка на классический бит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder measureReference(
        final QuantumReference reference,
        final String bit
    ) {
        circuit.measureReference(
            reference,
            bit(bit)
        );
        return this;
    }

    /**
     * Добавляет reset указанного кубита.
     *
     * @param qubit ссылка на кубит
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder reset(final String qubit) {
        circuit.reset(qubit(qubit));
        return this;
    }

    /**
     * Добавляет reset quantum reference.
     *
     * @param reference quantum reference
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder resetReference(final QuantumReference reference) {
        circuit.resetReference(reference);
        return this;
    }

    /**
     * Добавляет barrier для указанных кубитов.
     *
     * @param qubits ссылки на кубиты
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder barrier(final String... qubits) {
        circuit.barrier(qubits(qubits));
        return this;
    }

    /**
     * Добавляет операцию с простым классическим условием.
     *
     * @param condition классическое условие
     * @param operation операция под условием
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder controlled(
        final ClassicalCondition condition,
        final Operation operation
    ) {
        circuit.controlled(
            condition,
            operation
        );
        return this;
    }

    /**
     * Добавляет классическое присваивание.
     *
     * @param assignment классическое присваивание
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder assign(final ClassicalAssignment assignment) {
        circuit.assign(assignment);
        return this;
    }

    /**
     * Добавляет локальное классическое объявление.
     *
     * @param operation операция объявления
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder classicalDeclaration(final ClassicalDeclarationOperation operation) {
        circuit.classicalDeclaration(operation);
        return this;
    }

    /**
     * Добавляет локальное объявление классического массива.
     *
     * @param operation операция объявления массива
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder classicalArrayDeclaration(final ClassicalArrayDeclarationOperation operation) {
        circuit.classicalArrayDeclaration(operation);
        return this;
    }

    /**
     * Добавляет вызов callable/subroutine/extern.
     *
     * @param operation операция вызова
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder callableInvocation(final CallableInvocationOperation operation) {
        circuit.callableInvocation(operation);
        return this;
    }

    /**
     * Добавляет операцию с произвольным классическим предикатом.
     *
     * @param predicate классический предикат
     * @param operation операция под предикатом
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder classicallyControlled(
        final ClassicalPredicate predicate,
        final Operation operation
    ) {
        circuit.classicallyControlled(
            predicate,
            operation
        );
        return this;
    }

    /**
     * Добавляет scoped block.
     *
     * @param body тело блока
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder block(final OperationBlock body) {
        circuit.block(body);
        return this;
    }

    /**
     * Добавляет условный блок.
     *
     * @param predicate классический предикат
     * @param thenBlock then-блок
     * @param elseBlock else-блок или null
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder conditionalBlock(
        final ClassicalPredicate predicate,
        final OperationBlock thenBlock,
        final OperationBlock elseBlock
    ) {
        circuit.conditionalBlock(
            predicate,
            thenBlock,
            elseBlock
        );
        return this;
    }

    /**
     * Добавляет integer-range for loop.
     *
     * @param variableName имя переменной цикла
     * @param startInclusive начало диапазона
     * @param step шаг
     * @param endInclusive конец диапазона
     * @param body тело цикла
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder forLoop(
        final String variableName,
        final long startInclusive,
        final long step,
        final long endInclusive,
        final OperationBlock body
    ) {
        circuit.forLoop(
            variableName,
            startInclusive,
            step,
            endInclusive,
            body
        );
        return this;
    }

    /**
     * Добавляет symbolic/runtime for loop.
     *
     * @param operation операция цикла
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder symbolicForLoop(final SymbolicForLoopOperation operation) {
        circuit.symbolicForLoop(operation);
        return this;
    }

    /**
     * Добавляет while loop.
     *
     * @param predicate предикат продолжения
     * @param body тело цикла
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder whileLoop(
        final ClassicalPredicate predicate,
        final OperationBlock body
    ) {
        circuit.whileLoop(
            predicate,
            body
        );
        return this;
    }

    /**
     * Добавляет delay для статических кубитов.
     *
     * @param duration длительность
     * @param qubits ссылки на кубиты
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder delay(
        final DurationExpression duration,
        final String... qubits
    ) {
        circuit.delay(
            duration,
            qubits(qubits)
        );
        return this;
    }

    /**
     * Добавляет delay для универсальных quantum references.
     *
     * @param duration длительность
     * @param references quantum references
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder delayReferences(
        final DurationExpression duration,
        final QuantumReference... references
    ) {
        circuit.delayReferences(
            duration,
            references
        );
        return this;
    }

    /**
     * Добавляет timing box.
     *
     * @param duration длительность или null
     * @param body тело блока
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder timingBox(
        final DurationExpression duration,
        final OperationBlock body
    ) {
        circuit.timingBox(
            duration,
            body
        );
        return this;
    }

    /**
     * Добавляет label operation.
     *
     * @param name имя label
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder label(final String name) {
        circuit.label(name);
        return this;
    }

    /**
     * Добавляет branch operation.
     *
     * @param operation операция перехода
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder branch(final BranchOperation operation) {
        circuit.branch(operation);
        return this;
    }

    /**
     * Добавляет halt operation.
     *
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder halt() {
        circuit.halt();
        return this;
    }

    /**
     * Добавляет wait operation.
     *
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder waitInstruction() {
        circuit.waitInstruction();
        return this;
    }

    /**
     * Заменяет metadata операции.
     *
     * @param index индекс операции
     * @param metadata metadata операции
     * @return текущий builder схемы
     */
    public QuantumCircuitBuilder setOperationMetadata(
        final int index,
        final OperationMetadata metadata
    ) {
        circuit.setOperationMetadata(
            index,
            metadata
        );
        return this;
    }

    /**
     * Завершает описание текущей схемы и возвращает builder программы.
     *
     * @return builder программы
     */
    public QuantumProgramBuilder endCircuit() {
        return owner;
    }

    /**
     * Возвращает собранную Quantum IR программу.
     *
     * @return Quantum IR программа
     */
    public QuantumProgram build() {
        return program;
    }

    private Qubit qubit(final String reference) {
        final ReferenceParts parts = parseReference(
            reference,
            "Qubit reference"
        );
        final QuantumRegister register = quantumRegisters.get(parts.name());
        if (register == null) {
            throw new IllegalArgumentException("Unknown quantum register in reference: " + reference + ".");
        }
        return register.get(parts.index());
    }

    private ClassicalBit bit(final String reference) {
        final ReferenceParts parts = parseReference(
            reference,
            "Classical bit reference"
        );
        final ClassicalRegister register = classicalRegisters.get(parts.name());
        if (register == null) {
            throw new IllegalArgumentException("Unknown classical register in reference: " + reference + ".");
        }
        return register.get(parts.index());
    }

    private Qubit[] qubits(final String[] references) {
        if (references == null) {
            throw new IllegalArgumentException("Qubit references must not be null.");
        }
        final Qubit[] result = new Qubit[references.length];
        for (int i = 0; i < references.length; i++) {
            result[i] = qubit(references[i]);
        }
        return result;
    }

    private void ensureQuantumRegisterNameAvailable(final String name) {
        if (quantumRegisters.containsKey(name)) {
            throw new IllegalArgumentException("Quantum register is already declared in builder: " + name + ".");
        }
    }

    private void ensureClassicalRegisterNameAvailable(final String name) {
        if (classicalRegisters.containsKey(name)) {
            throw new IllegalArgumentException("Classical register is already declared in builder: " + name + ".");
        }
    }

    private static ReferenceParts parseReference(
        final String reference,
        final String subject
    ) {
        if (
            reference == null
            || reference.isBlank()
        ) {
            throw new IllegalArgumentException(subject + " must not be blank.");
        }
        final int openBracket = reference.indexOf('[');
        final int closeBracket = reference.indexOf(']');
        if (
            openBracket <= 0
            || closeBracket != reference.length() - 1
            || openBracket + 1 >= closeBracket
        ) {
            throw new IllegalArgumentException(subject + " must have form name[index]: " + reference + ".");
        }
        final String name = reference.substring(
            0,
            openBracket
        );
        final long longIndex;
        try {
            longIndex = Long.parseLong(reference.substring(
                openBracket + 1,
                closeBracket
            ));
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException(subject + " index must be an integer: " + reference + ".");
        }
        if (
            longIndex < 0
            || longIndex > Integer.MAX_VALUE
        ) {
            throw new IllegalArgumentException(subject + " index is outside Java int range: " + reference + ".");
        }
        return new ReferenceParts(
            name,
            (int) longIndex
        );
    }

    private record ReferenceParts(
        String name,
        int index
    ) {
    }
}