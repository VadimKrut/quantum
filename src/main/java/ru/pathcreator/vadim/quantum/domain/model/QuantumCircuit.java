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

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.HaltOperation;
import ru.pathcreator.vadim.quantum.domain.operation.LabelOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WaitOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.register.RegisterName;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;

/**
 * Изменяемый builder квантовой схемы с регистрами и операциями.
 */
public final class QuantumCircuit {

    /**
     * Программа, которой принадлежит схема.
     */
    private final QuantumProgram program;

    /**
     * Имя схемы.
     */
    private final CircuitName name;

    /**
     * Квантовые регистры схемы.
     */
    private final ArrayList<QuantumRegister> quantumRegisters;

    /**
     * Классические регистры схемы.
     */
    private final ArrayList<ClassicalRegister> classicalRegisters;

    /**
     * Операции схемы в порядке добавления.
     */
    private final ArrayList<Operation> operations;

    /**
     * Metadata для операций по тем же индексам, что и operations.
     */
    private final ArrayList<OperationMetadata> operationMetadata;

    private QuantumCircuit(
        final QuantumProgram program,
        final CircuitName name
    ) {
        this.program = program;
        this.name = name;
        this.quantumRegisters = new ArrayList<>();
        this.classicalRegisters = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.operationMetadata = new ArrayList<>();
    }

    static QuantumCircuit create(
        final QuantumProgram program,
        final CircuitName name
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (name == null) {
            throw new IllegalArgumentException("Circuit name must not be null.");
        }
        return new QuantumCircuit(
            program,
            name
        );
    }

    /**
     * Возвращает программу, которой принадлежит схема.
     *
     * @return программа-владелец
     */
    public QuantumProgram program() {
        return program;
    }

    /**
     * Возвращает имя схемы.
     *
     * @return имя схемы
     */
    public CircuitName name() {
        return name;
    }

    /**
     * Создает квантовый регистр внутри схемы.
     *
     * @param name имя регистра
     * @param size количество кубитов
     * @return созданный квантовый регистр
     */
    public QuantumRegister createQuantumRegister(
        final String name,
        final int size
    ) {
        final RegisterName registerName = RegisterName.of(name);
        ensureRegisterNameAvailable(registerName);
        final QuantumRegister register = QuantumRegister.create(
            registerName,
            size
        );
        quantumRegisters.add(register);
        return register;
    }

    /**
     * Создает классический регистр внутри схемы.
     *
     * @param name имя регистра
     * @param size количество битов
     * @return созданный классический регистр
     */
    public ClassicalRegister createClassicalRegister(
        final String name,
        final int size
    ) {
        final RegisterName registerName = RegisterName.of(name);
        ensureRegisterNameAvailable(registerName);
        final ClassicalRegister register = ClassicalRegister.create(
            registerName,
            size
        );
        classicalRegisters.add(register);
        return register;
    }

    /**
     * Возвращает количество квантовых регистров.
     *
     * @return количество квантовых регистров
     */
    public int quantumRegisterCount() {
        return quantumRegisters.size();
    }

    /**
     * Возвращает квантовый регистр по индексу.
     *
     * @param index индекс регистра
     * @return квантовый регистр
     */
    public QuantumRegister quantumRegister(final int index) {
        validateQuantumRegisterIndex(index);
        return quantumRegisters.get(index);
    }

    /**
     * Возвращает неизменяемый снимок квантовых регистров.
     *
     * @return список квантовых регистров
     */
    public List<QuantumRegister> quantumRegisters() {
        return List.copyOf(quantumRegisters);
    }

    /**
     * Возвращает количество классических регистров.
     *
     * @return количество классических регистров
     */
    public int classicalRegisterCount() {
        return classicalRegisters.size();
    }

    /**
     * Возвращает классический регистр по индексу.
     *
     * @param index индекс регистра
     * @return классический регистр
     */
    public ClassicalRegister classicalRegister(final int index) {
        validateClassicalRegisterIndex(index);
        return classicalRegisters.get(index);
    }

    /**
     * Возвращает неизменяемый снимок классических регистров.
     *
     * @return список классических регистров
     */
    public List<ClassicalRegister> classicalRegisters() {
        return List.copyOf(classicalRegisters);
    }

    /**
     * Добавляет gate h.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit h(final Qubit qubit) {
        appendGate(
            StandardGate.H,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate x.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit x(final Qubit qubit) {
        appendGate(
            StandardGate.X,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate y.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit y(final Qubit qubit) {
        appendGate(
            StandardGate.Y,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate z.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit z(final Qubit qubit) {
        appendGate(
            StandardGate.Z,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate s.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit s(final Qubit qubit) {
        appendGate(
            StandardGate.S,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate sdg.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit sdg(final Qubit qubit) {
        appendGate(
            StandardGate.SDG,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate t.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit t(final Qubit qubit) {
        appendGate(
            StandardGate.T,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate tdg.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit tdg(final Qubit qubit) {
        appendGate(
            StandardGate.TDG,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate rx.
     *
     * @param angle параметр угла
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit rx(
        final ParameterExpression angle,
        final Qubit qubit
    ) {
        appendParameterizedGate(
            StandardGate.RX,
            angle,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate ry.
     *
     * @param angle параметр угла
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit ry(
        final ParameterExpression angle,
        final Qubit qubit
    ) {
        appendParameterizedGate(
            StandardGate.RY,
            angle,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate rz.
     *
     * @param angle параметр угла
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit rz(
        final ParameterExpression angle,
        final Qubit qubit
    ) {
        appendParameterizedGate(
            StandardGate.RZ,
            angle,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate cx.
     *
     * @param control управляющий кубит
     * @param target целевой кубит
     * @return текущая схема
     */
    public QuantumCircuit cx(
        final Qubit control,
        final Qubit target
    ) {
        appendGate(
            StandardGate.CX,
            control,
            target
        );
        return this;
    }

    /**
     * Добавляет gate cy.
     *
     * @param control управляющий кубит
     * @param target целевой кубит
     * @return текущая схема
     */
    public QuantumCircuit cy(
        final Qubit control,
        final Qubit target
    ) {
        appendGate(
            StandardGate.CY,
            control,
            target
        );
        return this;
    }

    /**
     * Добавляет gate cz.
     *
     * @param control управляющий кубит
     * @param target целевой кубит
     * @return текущая схема
     */
    public QuantumCircuit cz(
        final Qubit control,
        final Qubit target
    ) {
        appendGate(
            StandardGate.CZ,
            control,
            target
        );
        return this;
    }

    /**
     * Добавляет gate ch.
     *
     * @param control управляющий кубит
     * @param target целевой кубит
     * @return текущая схема
     */
    public QuantumCircuit ch(
        final Qubit control,
        final Qubit target
    ) {
        appendGate(
            StandardGate.CH,
            control,
            target
        );
        return this;
    }

    /**
     * Добавляет gate swap.
     *
     * @param left первый кубит
     * @param right второй кубит
     * @return текущая схема
     */
    public QuantumCircuit swap(
        final Qubit left,
        final Qubit right
    ) {
        appendGate(
            StandardGate.SWAP,
            left,
            right
        );
        return this;
    }

    /**
     * Добавляет gate ccx.
     *
     * @param firstControl первый управляющий кубит
     * @param secondControl второй управляющий кубит
     * @param target целевой кубит
     * @return текущая схема
     */
    public QuantumCircuit ccx(
        final Qubit firstControl,
        final Qubit secondControl,
        final Qubit target
    ) {
        appendGate(
            StandardGate.CCX,
            firstControl,
            secondControl,
            target
        );
        return this;
    }

    /**
     * Добавляет gate phase.
     *
     * @param angle параметр угла
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit phase(
        final ParameterExpression angle,
        final Qubit qubit
    ) {
        appendParameterizedGate(
            StandardGate.PHASE,
            angle,
            qubit
        );
        return this;
    }

    /**
     * Добавляет gate id.
     *
     * @param qubit кубит операции
     * @return текущая схема
     */
    public QuantumCircuit id(final Qubit qubit) {
        appendGate(
            StandardGate.ID,
            qubit
        );
        return this;
    }

    /**
     * Добавляет произвольный непараметризованный gate-based гейт.
     *
     * @param gate описание гейта
     * @param qubits кубиты операции
     * @return текущая схема
     */
    public QuantumCircuit gate(
        final Gate gate,
        final Qubit... qubits
    ) {
        appendGate(
            gate,
            qubits
        );
        return this;
    }

    /**
     * Добавляет произвольный параметризованный gate-based гейт.
     *
     * @param gate описание гейта
     * @param parameters параметры гейта
     * @param qubits кубиты операции
     * @return текущая схема
     */
    public QuantumCircuit parameterizedGate(
        final Gate gate,
        final ParameterExpression[] parameters,
        final Qubit... qubits
    ) {
        ensureQubitsBelongToCircuit(qubits);
        addOperation(GateOperation.parameterized(
            gate,
            parameters,
            qubits
        ));
        return this;
    }

    /**
     * Добавляет произвольный gate-based gate с qubit references.
     *
     * @param gate описание gate
     * @param qubitReferences ссылки на qubits операции
     * @return текущая схема
     */
    public QuantumCircuit gateReferences(
        final Gate gate,
        final QuantumReference... qubitReferences
    ) {
        ensureQuantumReferencesBelongToCircuit(qubitReferences);
        addOperation(GateOperation.ofReferences(
            gate,
            qubitReferences
        ));
        return this;
    }

    /**
     * Добавляет произвольный параметризованный gate-based gate с qubit references.
     *
     * @param gate описание gate
     * @param parameters параметры gate
     * @param qubitReferences ссылки на qubits операции
     * @return текущая схема
     */
    public QuantumCircuit parameterizedGateReferences(
        final Gate gate,
        final ParameterExpression[] parameters,
        final QuantumReference... qubitReferences
    ) {
        ensureQuantumReferencesBelongToCircuit(qubitReferences);
        addOperation(GateOperation.parameterizedReferences(
            gate,
            parameters,
            qubitReferences
        ));
        return this;
    }

    /**
     * Добавляет измерение кубита в классический бит.
     *
     * @param qubit измеряемый кубит
     * @param bit классический бит результата
     * @return текущая схема
     */
    public QuantumCircuit measure(
        final Qubit qubit,
        final ClassicalBit bit
    ) {
        ensureQubitBelongsToCircuit(qubit);
        ensureClassicalBitBelongsToCircuit(bit);
        addOperation(new MeasureOperation(
            qubit,
            bit
        ));
        return this;
    }

    /**
     * Добавляет измерение qubit reference в classical bit.
     *
     * @param qubitReference измеряемая ссылка на qubit
     * @param bit classical bit результата
     * @return текущая схема
     */
    public QuantumCircuit measureReference(
        final QuantumReference qubitReference,
        final ClassicalBit bit
    ) {
        ensureQuantumReferenceBelongsToCircuit(qubitReference);
        ensureClassicalBitBelongsToCircuit(bit);
        addOperation(new MeasureOperation(
            qubitReference,
            bit
        ));
        return this;
    }

    /**
     * Добавляет reset кубита.
     *
     * @param qubit сбрасываемый кубит
     * @return текущая схема
     */
    public QuantumCircuit reset(final Qubit qubit) {
        ensureQubitBelongsToCircuit(qubit);
        addOperation(new ResetOperation(qubit));
        return this;
    }

    /**
     * Добавляет reset qubit reference.
     *
     * @param qubitReference сбрасываемая ссылка на qubit
     * @return текущая схема
     */
    public QuantumCircuit resetReference(final QuantumReference qubitReference) {
        ensureQuantumReferenceBelongsToCircuit(qubitReference);
        addOperation(new ResetOperation(qubitReference));
        return this;
    }

    /**
     * Добавляет barrier для кубитов.
     *
     * @param qubits кубиты барьера
     * @return текущая схема
     */
    public QuantumCircuit barrier(final Qubit... qubits) {
        ensureQubitsBelongToCircuit(qubits);
        addOperation(new BarrierOperation(qubits));
        return this;
    }

    /**
     * Добавляет операцию с классическим условием выполнения.
     *
     * @param condition классическое условие
     * @param operation операция под условием
     * @return текущая схема
     */
    public QuantumCircuit controlled(
        final ClassicalCondition condition,
        final Operation operation
    ) {
        if (condition == null) {
            throw new IllegalArgumentException("Classical condition must not be null.");
        }
        ensureClassicalRegisterBelongsToCircuit(condition.register());
        ensureOperationBelongsToCircuit(operation);
        addOperation(new ControlledOperation(
            condition,
            operation
        ));
        return this;
    }

    /**
     * Добавляет присваивание в классической части IR.
     *
     * @param assignment классическое присваивание
     * @return текущая схема
     */
    public QuantumCircuit assign(final ClassicalAssignment assignment) {
        ensureClassicalAssignmentBelongsToCircuit(assignment);
        addOperation(new ClassicalAssignmentOperation(assignment));
        return this;
    }

    /**
     * Добавляет локальное классическое объявление в поток операций.
     *
     * @param operation операция объявления
     * @return текущая схема
     */
    public QuantumCircuit classicalDeclaration(final ClassicalDeclarationOperation operation) {
        ensureOperationBelongsToCircuit(operation);
        addOperation(operation);
        return this;
    }

    /**
     * Добавляет объявление классического массива в поток операций.
     *
     * @param operation операция объявления массива
     * @return текущая схема
     */
    public QuantumCircuit classicalArrayDeclaration(final ClassicalArrayDeclarationOperation operation) {
        ensureOperationBelongsToCircuit(operation);
        addOperation(operation);
        return this;
    }

    /**
     * Добавляет вызов callable/subroutine/extern в поток операций.
     *
     * @param operation операция вызова
     * @return текущая схема
     */
    public QuantumCircuit callableInvocation(final CallableInvocationOperation operation) {
        ensureOperationBelongsToCircuit(operation);
        addOperation(operation);
        return this;
    }

    /**
     * Добавляет операцию с произвольным классическим предикатом.
     *
     * @param predicate предикат выполнения
     * @param operation операция под предикатом
     * @return текущая схема
     */
    public QuantumCircuit classicallyControlled(
        final ClassicalPredicate predicate,
        final Operation operation
    ) {
        ensureClassicalPredicateBelongsToCircuit(predicate);
        ensureOperationBelongsToCircuit(operation);
        addOperation(new ClassicallyControlledOperation(
            predicate,
            operation
        ));
        return this;
    }

    /**
     * Добавляет scoped-блок операций.
     *
     * @param body тело блока
     * @return текущая схема
     */
    public QuantumCircuit block(final OperationBlock body) {
        ensureOperationBlockBelongsToCircuit(body);
        addOperation(new BlockOperation(body));
        return this;
    }

    /**
     * Добавляет блочное ветвление.
     *
     * @param predicate предикат ветвления
     * @param thenBlock then-блок
     * @param elseBlock else-блок или null
     * @return текущая схема
     */
    public QuantumCircuit conditionalBlock(
        final ClassicalPredicate predicate,
        final OperationBlock thenBlock,
        final OperationBlock elseBlock
    ) {
        ensureClassicalPredicateBelongsToCircuit(predicate);
        ensureOperationBlockBelongsToCircuit(thenBlock);
        if (elseBlock != null) {
            ensureOperationBlockBelongsToCircuit(elseBlock);
        }
        addOperation(new ConditionalBlockOperation(
            predicate,
            thenBlock,
            elseBlock
        ));
        return this;
    }

    /**
     * Добавляет цикл по диапазону.
     *
     * @param variableName имя переменной цикла
     * @param startInclusive начало диапазона
     * @param step шаг
     * @param endInclusive конец диапазона
     * @param body тело цикла
     * @return текущая схема
     */
    public QuantumCircuit forLoop(
        final String variableName,
        final long startInclusive,
        final long step,
        final long endInclusive,
        final OperationBlock body
    ) {
        ensureOperationBlockBelongsToCircuit(body);
        addOperation(new ForLoopOperation(
            variableName,
            startInclusive,
            step,
            endInclusive,
            body
        ));
        return this;
    }

    /**
     * Добавляет цикл по runtime/symbolic диапазону.
     *
     * @param operation операция цикла
     * @return текущая схема
     */
    public QuantumCircuit symbolicForLoop(final SymbolicForLoopOperation operation) {
        ensureOperationBelongsToCircuit(operation);
        addOperation(operation);
        return this;
    }

    /**
     * Добавляет while-цикл.
     *
     * @param predicate предикат продолжения
     * @param body тело цикла
     * @return текущая схема
     */
    public QuantumCircuit whileLoop(
        final ClassicalPredicate predicate,
        final OperationBlock body
    ) {
        ensureClassicalPredicateBelongsToCircuit(predicate);
        ensureOperationBlockBelongsToCircuit(body);
        addOperation(new WhileLoopOperation(
            predicate,
            body
        ));
        return this;
    }

    /**
     * Добавляет временную задержку.
     *
     * @param duration длительность
     * @param qubits кубиты задержки
     * @return текущая схема
     */
    public QuantumCircuit delay(
        final DurationExpression duration,
        final Qubit... qubits
    ) {
        ensureQubitsBelongToCircuit(qubits);
        addOperation(new DelayOperation(
            duration,
            qubits
        ));
        return this;
    }

    /**
     * Добавляет временную задержку по универсальным quantum references.
     *
     * @param duration длительность
     * @param references quantum references
     * @return текущая схема
     */
    public QuantumCircuit delayReferences(
        final DurationExpression duration,
        final QuantumReference... references
    ) {
        ensureQuantumReferencesBelongToCircuit(references);
        addOperation(new DelayOperation(
            duration,
            references
        ));
        return this;
    }

    /**
     * Добавляет timing box-блок.
     *
     * @param duration длительность или null
     * @param body тело блока
     * @return текущая схема
     */
    public QuantumCircuit timingBox(
        final DurationExpression duration,
        final OperationBlock body
    ) {
        ensureOperationBlockBelongsToCircuit(body);
        addOperation(new TimingBoxOperation(
            duration,
            body
        ));
        return this;
    }

    public QuantumCircuit label(final String name) {
        addOperation(new LabelOperation(name));
        return this;
    }

    public QuantumCircuit branch(final BranchOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Branch operation must not be null.");
        }
        addOperation(operation);
        return this;
    }

    public QuantumCircuit halt() {
        addOperation(HaltOperation.INSTANCE);
        return this;
    }

    public QuantumCircuit waitInstruction() {
        addOperation(WaitOperation.INSTANCE);
        return this;
    }

    /**
     * Возвращает количество операций.
     *
     * @return количество операций
     */
    public int operationCount() {
        return operations.size();
    }

    /**
     * Возвращает операцию по индексу.
     *
     * @param index индекс операции
     * @return операция
     */
    public Operation operation(final int index) {
        validateOperationIndex(index);
        return operations.get(index);
    }

    /**
     * Возвращает метаданные операции.
     *
     * @param index индекс операции
     * @return метаданные операции
     */
    public OperationMetadata operationMetadata(final int index) {
        validateOperationIndex(index);
        return operationMetadata.get(index);
    }

    /**
     * Заменяет метаданные операции без изменения самой операции.
     *
     * @param index индекс операции
     * @param metadata метаданные операции
     * @return текущая схема
     */
    public QuantumCircuit setOperationMetadata(
        final int index,
        final OperationMetadata metadata
    ) {
        validateOperationIndex(index);
        if (metadata == null) {
            throw new IllegalArgumentException("Operation metadata must not be null.");
        }
        operationMetadata.set(
            index,
            metadata
        );
        return this;
    }

    /**
     * Возвращает неизменяемый снимок операций.
     *
     * @return список операций
     */
    public List<Operation> operations() {
        return List.copyOf(operations);
    }

    private void appendGate(
        final Gate gate,
        final Qubit... qubits
    ) {
        ensureQubitsBelongToCircuit(qubits);
        addOperation(GateOperation.of(
            gate,
            qubits
        ));
    }

    private void appendParameterizedGate(
        final StandardGate gate,
        final ParameterExpression parameter,
        final Qubit... qubits
    ) {
        ensureQubitsBelongToCircuit(qubits);
        addOperation(GateOperation.parameterized(
            gate,
            new ParameterExpression[] {parameter},
            qubits
        ));
    }

    private void addOperation(final Operation operation) {
        operations.add(operation);
        operationMetadata.add(OperationMetadata.empty());
    }

    private void ensureRegisterNameAvailable(final RegisterName name) {
        for (int i = 0; i < quantumRegisters.size(); i++) {
            if (quantumRegisters.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Register name already exists in circuit.");
            }
        }
        for (int i = 0; i < classicalRegisters.size(); i++) {
            if (classicalRegisters.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Register name already exists in circuit.");
            }
        }
    }

    private void ensureQubitsBelongToCircuit(final Qubit[] qubits) {
        if (qubits == null) {
            throw new IllegalArgumentException("Qubits must not be null.");
        }
        for (int i = 0; i < qubits.length; i++) {
            ensureQubitBelongsToCircuit(qubits[i]);
        }
    }

    private void ensureQuantumReferencesBelongToCircuit(final QuantumReference[] references) {
        if (references == null) {
            throw new IllegalArgumentException("Quantum references must not be null.");
        }
        for (int i = 0; i < references.length; i++) {
            ensureQuantumReferenceBelongsToCircuit(references[i]);
        }
    }

    private void ensureQuantumReferenceBelongsToCircuit(final QuantumReference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("Quantum reference must not be null.");
        }
        if (reference.kind() == QuantumReferenceKind.STATIC_QUBIT) {
            ensureQubitBelongsToCircuit(reference.qubit());
        } else if (reference.kind() == QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            ensureQuantumRegisterBelongsToCircuit(reference.register());
            ensureClassicalExpressionBelongsToCircuit(reference.indexExpression());
        }
    }

    private void ensureQubitBelongsToCircuit(final Qubit qubit) {
        if (qubit == null) {
            throw new IllegalArgumentException("Qubit must not be null.");
        }
        for (int i = 0; i < quantumRegisters.size(); i++) {
            if (quantumRegisters.get(i) == qubit.register()) {
                return;
            }
        }
        throw new IllegalArgumentException("Qubit does not belong to this circuit.");
    }

    private void ensureQuantumRegisterBelongsToCircuit(final QuantumRegister register) {
        if (register == null) {
            throw new IllegalArgumentException("Quantum register must not be null.");
        }
        for (int i = 0; i < quantumRegisters.size(); i++) {
            if (quantumRegisters.get(i) == register) {
                return;
            }
        }
        throw new IllegalArgumentException("Quantum register does not belong to this circuit.");
    }

    private void ensureClassicalBitBelongsToCircuit(final ClassicalBit bit) {
        if (bit == null) {
            throw new IllegalArgumentException("Classical bit must not be null.");
        }
        for (int i = 0; i < classicalRegisters.size(); i++) {
            if (classicalRegisters.get(i) == bit.register()) {
                return;
            }
        }
        throw new IllegalArgumentException("Classical bit does not belong to this circuit.");
    }

    private void ensureClassicalRegisterBelongsToCircuit(final ClassicalRegister register) {
        if (register == null) {
            throw new IllegalArgumentException("Classical register must not be null.");
        }
        for (int i = 0; i < classicalRegisters.size(); i++) {
            if (classicalRegisters.get(i) == register) {
                return;
            }
        }
        throw new IllegalArgumentException("Classical register does not belong to this circuit.");
    }

    private void ensureClassicalAssignmentBelongsToCircuit(final ClassicalAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Classical assignment must not be null.");
        }
        ensureClassicalExpressionBelongsToCircuit(assignment.target());
        ensureClassicalExpressionBelongsToCircuit(assignment.value());
    }

    private void ensureClassicalPredicateBelongsToCircuit(final ClassicalPredicate predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Classical predicate must not be null.");
        }
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            ensureClassicalExpressionBelongsToCircuit(predicate.leftExpression());
            ensureClassicalExpressionBelongsToCircuit(predicate.rightExpression());
        } else if (predicate.kind() == ClassicalPredicateKind.NOT) {
            ensureClassicalPredicateBelongsToCircuit(predicate.leftPredicate());
        } else if (predicate.kind() == ClassicalPredicateKind.BOOLEAN) {
            ensureClassicalPredicateBelongsToCircuit(predicate.leftPredicate());
            ensureClassicalPredicateBelongsToCircuit(predicate.rightPredicate());
        }
    }

    private void ensureClassicalExpressionBelongsToCircuit(final ClassicalExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Classical expression must not be null.");
        }
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            ensureClassicalBitBelongsToCircuit(expression.bit());
        } else if (expression.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            ensureClassicalRegisterBelongsToCircuit(expression.register());
        } else if (expression.kind() == ClassicalExpressionKind.BINARY_OPERATION) {
            ensureClassicalExpressionBelongsToCircuit(expression.leftExpression());
            ensureClassicalExpressionBelongsToCircuit(expression.rightExpression());
        } else if (expression.kind() == ClassicalExpressionKind.CALL) {
            for (int i = 0; i < expression.callArgumentCount(); i++) {
                ensureClassicalExpressionBelongsToCircuit(expression.callArgument(i));
            }
        }
    }

    private void ensureOperationBelongsToCircuit(final Operation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation must not be null.");
        }
        if (operation instanceof GateOperation gateOperation) {
            ensureQuantumReferencesBelongToCircuit(gateOperation.qubitReferences());
        } else if (operation instanceof MeasureOperation measureOperation) {
            ensureQuantumReferenceBelongsToCircuit(measureOperation.qubitReference());
            ensureClassicalBitBelongsToCircuit(measureOperation.bit());
        } else if (operation instanceof ResetOperation resetOperation) {
            ensureQuantumReferenceBelongsToCircuit(resetOperation.qubitReference());
        } else if (operation instanceof BarrierOperation barrierOperation) {
            ensureQubitsBelongToCircuit(barrierOperation.qubits());
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            ensureClassicalAssignmentBelongsToCircuit(assignmentOperation.assignment());
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            if (declarationOperation.hasInitializer()) {
                ensureClassicalExpressionBelongsToCircuit(declarationOperation.initializer());
            }
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            for (int i = 0; i < arrayOperation.dimensionCount(); i++) {
                ensureClassicalExpressionBelongsToCircuit(arrayOperation.dimension(i));
            }
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            if (invocationOperation.hasTarget()) {
                ensureClassicalExpressionBelongsToCircuit(invocationOperation.target());
            }
            for (int i = 0; i < invocationOperation.classicalArguments().size(); i++) {
                ensureClassicalExpressionBelongsToCircuit(invocationOperation.classicalArguments().get(i));
            }
            for (int i = 0; i < invocationOperation.quantumArguments().size(); i++) {
                ensureQuantumReferenceBelongsToCircuit(invocationOperation.quantumArguments().get(i));
            }
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            ensureClassicalPredicateBelongsToCircuit(controlledOperation.predicate());
            ensureOperationBelongsToCircuit(controlledOperation.operation());
        } else if (operation instanceof BlockOperation blockOperation) {
            ensureOperationBlockBelongsToCircuit(blockOperation.body());
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            ensureClassicalPredicateBelongsToCircuit(conditionalOperation.predicate());
            ensureOperationBlockBelongsToCircuit(conditionalOperation.thenBlock());
            if (conditionalOperation.hasElseBlock()) {
                ensureOperationBlockBelongsToCircuit(conditionalOperation.elseBlock());
            }
        } else if (operation instanceof ForLoopOperation loopOperation) {
            ensureOperationBlockBelongsToCircuit(loopOperation.body());
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            ensureClassicalExpressionBelongsToCircuit(loopOperation.startInclusive());
            ensureClassicalExpressionBelongsToCircuit(loopOperation.step());
            ensureClassicalExpressionBelongsToCircuit(loopOperation.endInclusive());
            ensureOperationBlockBelongsToCircuit(loopOperation.body());
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            ensureClassicalPredicateBelongsToCircuit(loopOperation.predicate());
            ensureOperationBlockBelongsToCircuit(loopOperation.body());
        } else if (operation instanceof DelayOperation delayOperation) {
            for (int i = 0; i < delayOperation.qubitCount(); i++) {
                ensureQuantumReferenceBelongsToCircuit(delayOperation.reference(i));
            }
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            ensureOperationBlockBelongsToCircuit(boxOperation.body());
        } else {
            throw new IllegalArgumentException("Operation is not supported by this circuit.");
        }
    }

    private void ensureOperationBlockBelongsToCircuit(final OperationBlock block) {
        if (block == null) {
            throw new IllegalArgumentException("Operation block must not be null.");
        }
        for (int i = 0; i < block.operationCount(); i++) {
            ensureOperationBelongsToCircuit(block.operation(i));
        }
    }

    private void validateQuantumRegisterIndex(final int index) {
        if (
            index < 0
            || index >= quantumRegisters.size()
        ) {
            throw new IllegalArgumentException("Quantum register index is outside of circuit bounds.");
        }
    }

    private void validateClassicalRegisterIndex(final int index) {
        if (
            index < 0
            || index >= classicalRegisters.size()
        ) {
            throw new IllegalArgumentException("Classical register index is outside of circuit bounds.");
        }
    }

    private void validateOperationIndex(final int index) {
        if (
            index < 0
            || index >= operations.size()
        ) {
            throw new IllegalArgumentException("Operation index is outside of circuit bounds.");
        }
    }
}