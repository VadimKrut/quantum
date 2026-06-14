/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.composition;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBooleanOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Сервис копирования и добавления операций схемы с явным сопоставлением регистров и битов.
 */
public final class CircuitComposer {

    /**
     * Копирует схему в указанную программу без aliasing регистров.
     *
     * @param source исходная схема
     * @param targetProgram целевая программа
     * @param targetCircuitName имя новой схемы
     * @return новая схема
     */
    public QuantumCircuit copyCircuit(
        final QuantumCircuit source,
        final QuantumProgram targetProgram,
        final String targetCircuitName
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source circuit must not be null.");
        }
        if (targetProgram == null) {
            throw new IllegalArgumentException("Target program must not be null.");
        }
        final QuantumCircuit target = targetProgram.createCircuit(targetCircuitName);
        final Remap remap = copyRegisters(
            source,
            target
        );
        appendOperations(
            source,
            target,
            remap
        );
        return target;
    }

    /**
     * Добавляет операции исходной схемы в целевую через готовое сопоставление.
     *
     * @param source исходная схема
     * @param target целевая схема
     * @param remap сопоставление регистров и битов
     */
    public void appendOperations(
        final QuantumCircuit source,
        final QuantumCircuit target,
        final Remap remap
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source circuit must not be null.");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target circuit must not be null.");
        }
        if (remap == null) {
            throw new IllegalArgumentException("Circuit remap must not be null.");
        }
        for (int i = 0; i < source.operationCount(); i++) {
            appendOperation(
                target,
                source.operation(i),
                remap
            );
        }
    }

    private static Remap copyRegisters(
        final QuantumCircuit source,
        final QuantumCircuit target
    ) {
        final Remap.Builder builder = Remap.builder();
        for (int i = 0; i < source.quantumRegisterCount(); i++) {
            final QuantumRegister sourceRegister = source.quantumRegister(i);
            final QuantumRegister targetRegister = target.createQuantumRegister(
                sourceRegister.name().value(),
                sourceRegister.size()
            );
            builder.mapQuantumRegister(
                sourceRegister,
                targetRegister
            );
        }
        for (int i = 0; i < source.classicalRegisterCount(); i++) {
            final ClassicalRegister sourceRegister = source.classicalRegister(i);
            final ClassicalRegister targetRegister = target.createClassicalRegister(
                sourceRegister.name().value(),
                sourceRegister.size()
            );
            builder.mapClassicalRegister(
                sourceRegister,
                targetRegister
            );
        }
        return builder.build();
    }

    private static void appendOperation(
        final QuantumCircuit target,
        final Operation operation,
        final Remap remap
    ) {
        if (operation instanceof GateOperation gateOperation) {
            appendGateOperation(
                target,
                gateOperation,
                remap
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            target.measure(
                remap.qubit(measureOperation.qubit()),
                remap.classicalBit(measureOperation.bit())
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            target.reset(remap.qubit(resetOperation.qubit()));
        } else if (operation instanceof BarrierOperation barrierOperation) {
            final Qubit[] qubits = new Qubit[barrierOperation.qubitCount()];
            for (int i = 0; i < barrierOperation.qubitCount(); i++) {
                qubits[i] = remap.qubit(barrierOperation.qubit(i));
            }
            target.barrier(qubits);
        } else if (operation instanceof ControlledOperation controlledOperation) {
            target.controlled(
                ClassicalCondition.equalTo(
                    remap.classicalRegister(controlledOperation.condition().register()),
                    controlledOperation.condition().expectedValue()
                ),
                remapNestedOperation(
                    controlledOperation.operation(),
                    remap
                )
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            target.assign(remapAssignment(
                assignmentOperation.assignment(),
                remap
            ));
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            target.classicallyControlled(
                remapPredicate(
                    controlledOperation.predicate(),
                    remap
                ),
                remapNestedOperation(
                    controlledOperation.operation(),
                    remap
                )
            );
        } else {
            throw new IllegalArgumentException("Unsupported operation for circuit composition.");
        }
    }

    private static void appendGateOperation(
        final QuantumCircuit target,
        final GateOperation operation,
        final Remap remap
    ) {
        final Qubit[] qubits = new Qubit[operation.qubitCount()];
        for (int i = 0; i < operation.qubitCount(); i++) {
            qubits[i] = remap.qubit(operation.qubit(i));
        }
        if (operation.parameterCount() == 0) {
            target.gate(
                operation.gate(),
                qubits
            );
        } else {
            target.parameterizedGate(
                operation.gate(),
                operation.parameters(),
                qubits
            );
        }
    }

    private static Operation remapNestedOperation(
        final Operation operation,
        final Remap remap
    ) {
        if (operation instanceof GateOperation gateOperation) {
            final Qubit[] qubits = new Qubit[gateOperation.qubitCount()];
            for (int i = 0; i < gateOperation.qubitCount(); i++) {
                qubits[i] = remap.qubit(gateOperation.qubit(i));
            }
            return GateOperation.parameterized(
                gateOperation.gate(),
                gateOperation.parameters(),
                qubits
            );
        }
        if (operation instanceof ResetOperation resetOperation) {
            return new ResetOperation(remap.qubit(resetOperation.qubit()));
        }
        if (operation instanceof MeasureOperation measureOperation) {
            return new MeasureOperation(
                remap.qubit(measureOperation.qubit()),
                remap.classicalBit(measureOperation.bit())
            );
        }
        if (operation instanceof BarrierOperation barrierOperation) {
            final Qubit[] qubits = new Qubit[barrierOperation.qubitCount()];
            for (int i = 0; i < barrierOperation.qubitCount(); i++) {
                qubits[i] = remap.qubit(barrierOperation.qubit(i));
            }
            return new BarrierOperation(qubits);
        }
        if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            return new ClassicalAssignmentOperation(remapAssignment(
                assignmentOperation.assignment(),
                remap
            ));
        }
        throw new IllegalArgumentException("Unsupported nested operation for circuit composition.");
    }

    private static ClassicalAssignment remapAssignment(
        final ClassicalAssignment assignment,
        final Remap remap
    ) {
        return new ClassicalAssignment(
            remapExpression(
                assignment.target(),
                remap
            ),
            remapExpression(
                assignment.value(),
                remap
            )
        );
    }

    private static ClassicalPredicate remapPredicate(
        final ClassicalPredicate predicate,
        final Remap remap
    ) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            return ClassicalPredicate.compare(
                remapExpression(
                    predicate.leftExpression(),
                    remap
                ),
                predicate.comparisonOperator(),
                remapExpression(
                    predicate.rightExpression(),
                    remap
                )
            );
        }
        if (predicate.kind() == ClassicalPredicateKind.NOT) {
            return ClassicalPredicate.not(remapPredicate(
                predicate.leftPredicate(),
                remap
            ));
        }
        if (predicate.booleanOperator() == ClassicalBooleanOperator.AND) {
            return ClassicalPredicate.and(
                remapPredicate(
                    predicate.leftPredicate(),
                    remap
                ),
                remapPredicate(
                    predicate.rightPredicate(),
                    remap
                )
            );
        }
        return ClassicalPredicate.or(
            remapPredicate(
                predicate.leftPredicate(),
                remap
            ),
            remapPredicate(
                predicate.rightPredicate(),
                remap
            )
        );
    }

    private static ClassicalExpression remapExpression(
        final ClassicalExpression expression,
        final Remap remap
    ) {
        if (expression.kind() == ClassicalExpressionKind.INTEGER) {
            return ClassicalExpression.integer(expression.integerValue());
        }
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            return ClassicalExpression.bit(remap.classicalBit(expression.bit()));
        }
        return ClassicalExpression.register(remap.classicalRegister(expression.register()));
    }

    /**
     * Неизменяемое сопоставление битов и регистров для композиции схем.
     */
    public static final class Remap {

        private final Map<Qubit, Qubit> qubits;
        private final Map<ClassicalBit, ClassicalBit> classicalBits;
        private final Map<ClassicalRegister, ClassicalRegister> classicalRegisters;

        private Remap(
            final Map<Qubit, Qubit> qubits,
            final Map<ClassicalBit, ClassicalBit> classicalBits,
            final Map<ClassicalRegister, ClassicalRegister> classicalRegisters
        ) {
            this.qubits = qubits;
            this.classicalBits = classicalBits;
            this.classicalRegisters = classicalRegisters;
        }

        public static Builder builder() {
            return new Builder();
        }

        private Qubit qubit(final Qubit source) {
            final Qubit target = qubits.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing qubit remap.");
            }
            return target;
        }

        private ClassicalBit classicalBit(final ClassicalBit source) {
            final ClassicalBit target = classicalBits.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing classical bit remap.");
            }
            return target;
        }

        private ClassicalRegister classicalRegister(final ClassicalRegister source) {
            final ClassicalRegister target = classicalRegisters.get(source);
            if (target == null) {
                throw new IllegalArgumentException("Missing classical register remap.");
            }
            return target;
        }

        /**
         * Изменяемый builder для создания неизменяемого сопоставления.
         */
        public static final class Builder {

            private final LinkedHashMap<Qubit, Qubit> qubits;
            private final LinkedHashMap<ClassicalBit, ClassicalBit> classicalBits;
            private final LinkedHashMap<ClassicalRegister, ClassicalRegister> classicalRegisters;

            private Builder() {
                this.qubits = new LinkedHashMap<>();
                this.classicalBits = new LinkedHashMap<>();
                this.classicalRegisters = new LinkedHashMap<>();
            }

            public Builder mapQuantumRegister(
                final QuantumRegister source,
                final QuantumRegister target
            ) {
                if (source.size() != target.size()) {
                    throw new IllegalArgumentException("Quantum register remap sizes must match.");
                }
                for (int i = 0; i < source.size(); i++) {
                    qubits.put(
                        source.get(i),
                        target.get(i)
                    );
                }
                return this;
            }

            public Builder mapClassicalRegister(
                final ClassicalRegister source,
                final ClassicalRegister target
            ) {
                if (source.size() != target.size()) {
                    throw new IllegalArgumentException("Classical register remap sizes must match.");
                }
                classicalRegisters.put(
                    source,
                    target
                );
                for (int i = 0; i < source.size(); i++) {
                    classicalBits.put(
                        source.get(i),
                        target.get(i)
                    );
                }
                return this;
            }

            public Remap build() {
                return new Remap(
                    Map.copyOf(qubits),
                    Map.copyOf(classicalBits),
                    Map.copyOf(classicalRegisters)
                );
            }
        }
    }
}