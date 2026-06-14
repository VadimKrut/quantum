/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRuleErrorCollector;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SourceFragmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.register.RegisterName;

/**
 * Доменный валидатор квантовой программы для текущей gate-based модели.
 */
public final class QuantumProgramValidator {

    private static final String PI_CONSTANT_NAME = "pi";

    /**
     * Проверяет программу и возвращает все найденные ошибки.
     *
     * @param program валидируемая программа
     * @return результат валидации
     */
    public ValidationResult validate(final QuantumProgram program) {
        final ArrayList<ValidationError> errors = new ArrayList<>();

        if (program == null) {
            addError(
                errors,
                ValidationErrorCode.NULL_PROGRAM,
                "Quantum program must not be null.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            );
            return new ValidationResult(errors);
        }

        if (program.computationModel() != QuantumComputationModel.GATE_BASED_CIRCUIT) {
            addError(
                errors,
                ValidationErrorCode.UNSUPPORTED_COMPUTATION_MODEL,
                "Only GATE_BASED_CIRCUIT computation model is supported at this stage.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            );
        }

        final LinkedHashMap<String, GateDefinition> definitionsByName = validateGateDefinitions(
            program,
            errors
        );

        for (int i = 0; i < program.circuitCount(); i++) {
            validateCircuit(
                program,
                definitionsByName,
                program.circuit(i),
                i,
                errors
            );
        }

        return new ValidationResult(errors);
    }

    private LinkedHashMap<String, GateDefinition> validateGateDefinitions(
        final QuantumProgram program,
        final ArrayList<ValidationError> errors
    ) {
        final LinkedHashMap<String, GateDefinition> definitionsByName = new LinkedHashMap<>();
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final GateDefinition definition = program.gateDefinition(i);
            validateGateDefinitionName(
                definition,
                definitionsByName,
                errors
            );
            definitionsByName.put(
                definition.gateName(),
                definition
            );
        }
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            final GateDefinition definition = program.gateDefinition(i);
            if (definition.kind() == GateDefinitionKind.COMPOSITE) {
                validateCompositeGateDefinition(
                    definition,
                    definitionsByName,
                    errors
                );
            }
        }
        validateGateDefinitionCycles(
            definitionsByName,
            errors
        );
        return definitionsByName;
    }

    private void validateGateDefinitionName(
        final GateDefinition definition,
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final ArrayList<ValidationError> errors
    ) {
        if (definitionsByName.containsKey(definition.gateName())) {
            addError(
                errors,
                ValidationErrorCode.INVALID_GATE_DEFINITION,
                "Gate definition name is duplicated.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            );
        }
        for (int i = 0; i < StandardGate.values().length; i++) {
            if (StandardGate.values()[i].gateName().equals(definition.gateName())) {
                addError(
                    errors,
                    ValidationErrorCode.GATE_DEFINITION_NAME_CONFLICT,
                    "Gate definition name conflicts with a standard gate.",
                    ValidationError.NO_INDEX,
                    ValidationError.NO_INDEX
                );
            }
        }
    }

    private void validateCompositeGateDefinition(
        final GateDefinition definition,
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            final GateBodyOperation operation = definition.bodyOperations().get(i);
            validateGateBodyQubits(
                definition,
                operation,
                errors
            );
            validateGateBodyParameters(
                definition,
                operation,
                errors
            );
            validateGateReference(
                operation.gate(),
                definitionsByName,
                errors
            );
            validateGateBodyArity(
                operation,
                errors
            );
            validateGateBodyParameterCount(
                operation,
                errors
            );
        }
    }

    private void validateGateBodyQubits(
        final GateDefinition definition,
        final GateBodyOperation operation,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (!definition.qubitNames().contains(operation.qubitName(i))) {
                addError(
                    errors,
                    ValidationErrorCode.INVALID_GATE_BODY_QUBIT,
                    "Composite gate body uses an undeclared qubit argument.",
                    ValidationError.NO_INDEX,
                    ValidationError.NO_INDEX
                );
            }
        }
    }

    private void validateGateBodyParameters(
        final GateDefinition definition,
        final GateBodyOperation operation,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < operation.parameterCount(); i++) {
            validateParameterExpressionSymbols(
                definition,
                operation.parameter(i),
                errors
            );
        }
    }

    private void validateParameterExpressionSymbols(
        final GateDefinition definition,
        final ParameterExpression expression,
        final ArrayList<ValidationError> errors
    ) {
        if (expression.kind() == ParameterExpressionKind.NAMED) {
            if (!definition.parameterNames().contains(expression.name())) {
                addError(
                    errors,
                    ValidationErrorCode.INVALID_GATE_BODY_PARAMETER,
                    "Composite gate body uses an undeclared parameter symbol.",
                    ValidationError.NO_INDEX,
                    ValidationError.NO_INDEX
                );
            }
        } else if (expression.kind() == ParameterExpressionKind.KNOWN_CONSTANT) {
            if (!isSupportedKnownConstant(expression.name())) {
                addError(
                    errors,
                    ValidationErrorCode.INVALID_GATE_BODY_PARAMETER,
                    "Composite gate body uses an unsupported known parameter constant.",
                    ValidationError.NO_INDEX,
                    ValidationError.NO_INDEX
                );
            }
        } else if (expression.kind() == ParameterExpressionKind.UNARY) {
            validateParameterExpressionSymbols(
                definition,
                expression.left(),
                errors
            );
        } else if (expression.kind() == ParameterExpressionKind.BINARY) {
            validateParameterExpressionSymbols(
                definition,
                expression.left(),
                errors
            );
            validateParameterExpressionSymbols(
                definition,
                expression.right(),
                errors
            );
        }
    }

    private void validateGateReference(
        final Gate gate,
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final ArrayList<ValidationError> errors
    ) {
        if (gate instanceof StandardGate) {
            return;
        }
        if (gate instanceof ModifiedGate modifiedGate) {
            validateGateReference(
                modifiedGate.baseGate(),
                definitionsByName,
                errors
            );
            return;
        }
        if (gate instanceof GateDefinition definition) {
            if (definition.kind() == GateDefinitionKind.INTRINSIC) {
                return;
            }
            if (definitionsByName.containsKey(definition.gateName())) {
                return;
            }
        }
        addError(
            errors,
            ValidationErrorCode.UNDECLARED_GATE_DEFINITION,
            "Composite gate body references a custom gate that is not declared in the program.",
            ValidationError.NO_INDEX,
            ValidationError.NO_INDEX
        );
    }

    private void validateGateBodyArity(
        final GateBodyOperation operation,
        final ArrayList<ValidationError> errors
    ) {
        if (operation.qubitCount() != operation.gate().arity()) {
            addError(
                errors,
                ValidationErrorCode.INVALID_GATE_ARITY,
                "Composite gate body qubit count does not match gate arity.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            );
        }
    }

    private void validateGateBodyParameterCount(
        final GateBodyOperation operation,
        final ArrayList<ValidationError> errors
    ) {
        if (operation.parameterCount() != operation.gate().parameterCount()) {
            addError(
                errors,
                ValidationErrorCode.INVALID_GATE_PARAMETER_COUNT,
                "Composite gate body parameter count does not match gate definition.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            );
        }
    }

    private void validateGateDefinitionCycles(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final ArrayList<ValidationError> errors
    ) {
        final HashSet<String> visited = new HashSet<>();
        final HashSet<String> visiting = new HashSet<>();
        final String[] names = definitionsByName.keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            validateGateDefinitionCycle(
                names[i],
                definitionsByName,
                visited,
                visiting,
                errors
            );
        }
    }

    private void validateGateDefinitionCycle(
        final String name,
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final HashSet<String> visited,
        final HashSet<String> visiting,
        final ArrayList<ValidationError> errors
    ) {
        if (visited.contains(name)) {
            return;
        }
        if (visiting.contains(name)) {
            addError(
                errors,
                ValidationErrorCode.CYCLIC_GATE_DEFINITION,
                "Gate definitions contain a cycle.",
                ValidationError.NO_INDEX,
                ValidationError.NO_INDEX
            );
            return;
        }
        final GateDefinition definition = definitionsByName.get(name);
        if (
            definition == null
            || definition.kind() != GateDefinitionKind.COMPOSITE
        ) {
            return;
        }
        visiting.add(name);
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            final Gate gate = definition.bodyOperations().get(i).gate();
            final List<String> referencedNames = referencedGateDefinitionNames(gate);
            for (int j = 0; j < referencedNames.size(); j++) {
                validateGateDefinitionCycle(
                    referencedNames.get(j),
                    definitionsByName,
                    visited,
                    visiting,
                    errors
                );
            }
        }
        visiting.remove(name);
        visited.add(name);
    }

    private void validateCircuit(
        final QuantumProgram program,
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final int circuitIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (circuit.program() != program) {
            addError(
                errors,
                ValidationErrorCode.CIRCUIT_DOES_NOT_BELONG_TO_PROGRAM,
                "Circuit does not belong to validated program.",
                circuitIndex,
                ValidationError.NO_INDEX
            );
        }

        validateQuantumRegisters(
            circuit,
            circuitIndex,
            errors
        );
        validateClassicalRegisters(
            circuit,
            circuitIndex,
            errors
        );
        validateRegisterNameConflicts(
            circuit,
            circuitIndex,
            errors
        );
        validateOperations(
            definitionsByName,
            circuit,
            circuitIndex,
            errors
        );
    }

    private void validateQuantumRegisters(
        final QuantumCircuit circuit,
        final int circuitIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final QuantumRegister register = circuit.quantumRegister(i);
            if (register.size() <= 0) {
                addError(
                    errors,
                    ValidationErrorCode.INVALID_REGISTER_SIZE,
                    "Quantum register size must be positive.",
                    circuitIndex,
                    ValidationError.NO_INDEX
                );
            }
        }
    }

    private void validateClassicalRegisters(
        final QuantumCircuit circuit,
        final int circuitIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final ClassicalRegister register = circuit.classicalRegister(i);
            if (register.size() <= 0) {
                addError(
                    errors,
                    ValidationErrorCode.INVALID_REGISTER_SIZE,
                    "Classical register size must be positive.",
                    circuitIndex,
                    ValidationError.NO_INDEX
                );
            }
        }
    }

    private void validateRegisterNameConflicts(
        final QuantumCircuit circuit,
        final int circuitIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            final RegisterName quantumName = circuit.quantumRegister(i).name();
            for (int j = i + 1; j < circuit.quantumRegisterCount(); j++) {
                validateRegisterNamePair(
                    quantumName,
                    circuit.quantumRegister(j).name(),
                    circuitIndex,
                    errors
                );
            }
            for (int j = 0; j < circuit.classicalRegisterCount(); j++) {
                validateRegisterNamePair(
                    quantumName,
                    circuit.classicalRegister(j).name(),
                    circuitIndex,
                    errors
                );
            }
        }

        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            final RegisterName classicalName = circuit.classicalRegister(i).name();
            for (int j = i + 1; j < circuit.classicalRegisterCount(); j++) {
                validateRegisterNamePair(
                    classicalName,
                    circuit.classicalRegister(j).name(),
                    circuitIndex,
                    errors
                );
            }
        }
    }

    private void validateRegisterNamePair(
        final RegisterName left,
        final RegisterName right,
        final int circuitIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (left.equals(right)) {
            addError(
                errors,
                ValidationErrorCode.DUPLICATE_REGISTER_NAME,
                "Register name is duplicated inside circuit.",
                circuitIndex,
                ValidationError.NO_INDEX
            );
        }
    }

    private void validateOperations(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final int circuitIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.operationCount(); i++) {
            final Operation operation = circuit.operation(i);
            if (operation instanceof GateOperation gateOperation) {
                validateGateOperation(
                    definitionsByName,
                    circuit,
                    gateOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof MeasureOperation measureOperation) {
                validateMeasureOperation(
                    circuit,
                    measureOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ResetOperation resetOperation) {
                validateResetOperation(
                    circuit,
                    resetOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof BarrierOperation barrierOperation) {
                validateBarrierOperation(
                    circuit,
                    barrierOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ControlledOperation controlledOperation) {
                validateControlledOperation(
                    definitionsByName,
                    circuit,
                    controlledOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
                validateClassicalAssignmentOperation(
                    circuit,
                    assignmentOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
                validateClassicallyControlledOperation(
                    definitionsByName,
                    circuit,
                    controlledOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof BlockOperation blockOperation) {
                validateOperationBlock(
                    definitionsByName,
                    circuit,
                    blockOperation.body(),
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
                validateConditionalBlockOperation(
                    definitionsByName,
                    circuit,
                    conditionalOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ForLoopOperation loopOperation) {
                validateOperationBlock(
                    definitionsByName,
                    circuit,
                    loopOperation.body(),
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof WhileLoopOperation loopOperation) {
                validateClassicalPredicate(
                    circuit,
                    loopOperation.predicate(),
                    circuitIndex,
                    i,
                    errors
                );
                validateOperationBlock(
                    definitionsByName,
                    circuit,
                    loopOperation.body(),
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof DelayOperation delayOperation) {
                validateDelayOperation(
                    circuit,
                    delayOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
                validateOptionalClassicalExpression(
                    circuit,
                    declarationOperation.hasInitializer() ? declarationOperation.initializer() : null,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
                validateClassicalArrayDeclarationOperation(
                    circuit,
                    arrayOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof CallableInvocationOperation invocationOperation) {
                validateCallableInvocationOperation(
                    circuit,
                    invocationOperation,
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
                validateClassicalExpression(
                    circuit,
                    loopOperation.startInclusive(),
                    circuitIndex,
                    i,
                    errors
                );
                validateOptionalClassicalExpression(
                    circuit,
                    loopOperation.step(),
                    circuitIndex,
                    i,
                    errors
                );
                validateClassicalExpression(
                    circuit,
                    loopOperation.endInclusive(),
                    circuitIndex,
                    i,
                    errors
                );
                validateOperationBlock(
                    definitionsByName,
                    circuit,
                    loopOperation.body(),
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof TimingBoxOperation boxOperation) {
                validateOperationBlock(
                    definitionsByName,
                    circuit,
                    boxOperation.body(),
                    circuitIndex,
                    i,
                    errors
                );
            } else if (operation instanceof SourceFragmentOperation) {
                continue;
            } else {
                addError(
                    errors,
                    ValidationErrorCode.OPERATION_NOT_SUPPORTED_BY_GATE_BASED_MODEL,
                    "Operation is not supported by gate-based circuit model.",
                    circuitIndex,
                    i
                );
            }
        }
    }

    private void validateGateOperation(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final GateOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        final Gate gate = operation.gate();
        validateGateReference(
            gate,
            definitionsByName,
            errors
        );
        if (operation.qubitCount() != gate.arity()) {
            addError(
                errors,
                ValidationErrorCode.INVALID_GATE_ARITY,
                "Gate operation qubit count does not match gate arity.",
                circuitIndex,
                operationIndex
            );
        }
        if (operation.parameterCount() != gate.parameterCount()) {
            addError(
                errors,
                ValidationErrorCode.INVALID_GATE_PARAMETER_COUNT,
                "Gate operation parameter count does not match gate definition.",
                circuitIndex,
                operationIndex
            );
        }
        validateGateOperationParameters(
            operation,
            circuitIndex,
            operationIndex,
            errors
        );
        for (int i = 0; i < operation.qubitCount(); i++) {
            validateQuantumReferenceOwnership(
                circuit,
                operation.qubitReference(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
        validateGateRules(
            operation,
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateGateOperationParameters(
        final GateOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < operation.parameterCount(); i++) {
            validateOperationParameterExpression(
                operation.parameter(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateOperationParameterExpression(
        final ParameterExpression expression,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (expression.kind() == ParameterExpressionKind.KNOWN_CONSTANT) {
            if (!isSupportedKnownConstant(expression.name())) {
                addError(
                    errors,
                    ValidationErrorCode.INVALID_GATE_PARAMETER,
                    "Gate operation uses an unsupported known parameter constant.",
                    circuitIndex,
                    operationIndex
                );
            }
        } else if (expression.kind() == ParameterExpressionKind.UNARY) {
            validateOperationParameterExpression(
                expression.left(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (expression.kind() == ParameterExpressionKind.BINARY) {
            validateOperationParameterExpression(
                expression.left(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateOperationParameterExpression(
                expression.right(),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateGateRules(
        final GateOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        final Gate gate = operation.gate();
        final GateValidationRuleErrorCollector collector = new ValidatorGateRuleErrorCollector(
            errors,
            circuitIndex,
            operationIndex
        );
        for (int i = 0; i < gate.validationRules().size(); i++) {
            final GateValidationRule rule = gate.validationRules().get(i);
            rule.validate(
                operation,
                collector
            );
        }
    }

    private void validateMeasureOperation(
        final QuantumCircuit circuit,
        final MeasureOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateQuantumReferenceOwnership(
            circuit,
            operation.qubitReference(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateClassicalBitOwnership(
            circuit,
            operation.bit(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateResetOperation(
        final QuantumCircuit circuit,
        final ResetOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateQuantumReferenceOwnership(
            circuit,
            operation.qubitReference(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateBarrierOperation(
        final QuantumCircuit circuit,
        final BarrierOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            validateQubitOwnership(
                circuit,
                operation.qubit(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateControlledOperation(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final ControlledOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateClassicalRegisterOwnership(
            circuit,
            operation.condition().register(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateClassicalConditionRange(
            operation,
            circuitIndex,
            operationIndex,
            errors
        );
        validateNestedOperation(
            definitionsByName,
            circuit,
            operation.operation(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateNestedOperation(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final Operation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (operation instanceof GateOperation gateOperation) {
            validateGateOperation(
                definitionsByName,
                circuit,
                gateOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            validateMeasureOperation(
                circuit,
                measureOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            validateResetOperation(
                circuit,
                resetOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof BarrierOperation barrierOperation) {
            validateBarrierOperation(
                circuit,
                barrierOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            validateClassicalAssignmentOperation(
                circuit,
                assignmentOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            validateOperationBlock(
                definitionsByName,
                circuit,
                blockOperation.body(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            validateConditionalBlockOperation(
                definitionsByName,
                circuit,
                conditionalOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            validateOperationBlock(
                definitionsByName,
                circuit,
                loopOperation.body(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            validateClassicalPredicate(
                circuit,
                loopOperation.predicate(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateOperationBlock(
                definitionsByName,
                circuit,
                loopOperation.body(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            validateDelayOperation(
                circuit,
                delayOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            validateOptionalClassicalExpression(
                circuit,
                declarationOperation.hasInitializer() ? declarationOperation.initializer() : null,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            validateClassicalArrayDeclarationOperation(
                circuit,
                arrayOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            validateCallableInvocationOperation(
                circuit,
                invocationOperation,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            validateClassicalExpression(
                circuit,
                loopOperation.startInclusive(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateOptionalClassicalExpression(
                circuit,
                loopOperation.step(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateClassicalExpression(
                circuit,
                loopOperation.endInclusive(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateOperationBlock(
                definitionsByName,
                circuit,
                loopOperation.body(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            validateOperationBlock(
                definitionsByName,
                circuit,
                boxOperation.body(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (operation instanceof SourceFragmentOperation) {
            return;
        } else {
            addError(
                errors,
                ValidationErrorCode.OPERATION_NOT_SUPPORTED_BY_GATE_BASED_MODEL,
                "Controlled operation body is not supported by gate-based circuit model.",
                circuitIndex,
                operationIndex
            );
        }
    }

    private void validateClassicalAssignmentOperation(
        final QuantumCircuit circuit,
        final ClassicalAssignmentOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateClassicalAssignment(
            circuit,
            operation.assignment(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateClassicallyControlledOperation(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final ClassicallyControlledOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateClassicalPredicate(
            circuit,
            operation.predicate(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateNestedOperation(
            definitionsByName,
            circuit,
            operation.operation(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateConditionalBlockOperation(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final ConditionalBlockOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateClassicalPredicate(
            circuit,
            operation.predicate(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateOperationBlock(
            definitionsByName,
            circuit,
            operation.thenBlock(),
            circuitIndex,
            operationIndex,
            errors
        );
        if (operation.hasElseBlock()) {
            validateOperationBlock(
                definitionsByName,
                circuit,
                operation.elseBlock(),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateOperationBlock(
        final LinkedHashMap<String, GateDefinition> definitionsByName,
        final QuantumCircuit circuit,
        final OperationBlock block,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < block.operationCount(); i++) {
            validateNestedOperation(
                definitionsByName,
                circuit,
                block.operation(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateDelayOperation(
        final QuantumCircuit circuit,
        final DelayOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            validateQuantumReferenceOwnership(
                circuit,
                operation.reference(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateClassicalArrayDeclarationOperation(
        final QuantumCircuit circuit,
        final ClassicalArrayDeclarationOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < operation.dimensionCount(); i++) {
            validateClassicalExpression(
                circuit,
                operation.dimension(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateCallableInvocationOperation(
        final QuantumCircuit circuit,
        final CallableInvocationOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (operation.hasTarget()) {
            validateClassicalExpression(
                circuit,
                operation.target(),
                circuitIndex,
                operationIndex,
                errors
            );
        }
        for (int i = 0; i < operation.quantumArguments().size(); i++) {
            validateQuantumReferenceOwnership(
                circuit,
                operation.quantumArguments().get(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
        for (int i = 0; i < operation.classicalArguments().size(); i++) {
            validateClassicalExpression(
                circuit,
                operation.classicalArguments().get(i),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateOptionalClassicalExpression(
        final QuantumCircuit circuit,
        final ClassicalExpression expression,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (expression == null) {
            return;
        }
        validateClassicalExpression(
            circuit,
            expression,
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateClassicalAssignment(
        final QuantumCircuit circuit,
        final ClassicalAssignment assignment,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        validateClassicalExpression(
            circuit,
            assignment.target(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateClassicalExpression(
            circuit,
            assignment.value(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateClassicalPredicate(
        final QuantumCircuit circuit,
        final ClassicalPredicate predicate,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            validateClassicalExpression(
                circuit,
                predicate.leftExpression(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateClassicalExpression(
                circuit,
                predicate.rightExpression(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateClassicalPredicateComparisonRange(
                predicate,
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (predicate.kind() == ClassicalPredicateKind.NOT) {
            validateClassicalPredicate(
                circuit,
                predicate.leftPredicate(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (predicate.kind() == ClassicalPredicateKind.BOOLEAN) {
            validateClassicalPredicate(
                circuit,
                predicate.leftPredicate(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateClassicalPredicate(
                circuit,
                predicate.rightPredicate(),
                circuitIndex,
                operationIndex,
                errors
            );
        }
    }

    private void validateClassicalPredicateComparisonRange(
        final ClassicalPredicate predicate,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (predicate.comparisonOperator() != ClassicalComparisonOperator.EQUAL) {
            return;
        }
        validateClassicalPredicateComparisonRangeSide(
            predicate.leftExpression(),
            predicate.rightExpression(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateClassicalPredicateComparisonRangeSide(
            predicate.rightExpression(),
            predicate.leftExpression(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateClassicalPredicateComparisonRangeSide(
        final ClassicalExpression registerExpression,
        final ClassicalExpression integerExpression,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (
            registerExpression.kind() != ClassicalExpressionKind.REGISTER_REFERENCE
            || integerExpression.kind() != ClassicalExpressionKind.INTEGER
        ) {
            return;
        }
        final ClassicalRegister register = registerExpression.register();
        if (
            register.size() < Long.SIZE - 1
            && integerExpression.integerValue() >= (1L << register.size())
        ) {
            addError(
                errors,
                ValidationErrorCode.CLASSICAL_CONDITION_VALUE_OUT_OF_RANGE,
                "Classical predicate value is outside of register value range.",
                circuitIndex,
                operationIndex
            );
        }
    }

    private void validateClassicalExpression(
        final QuantumCircuit circuit,
        final ClassicalExpression expression,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            validateClassicalBitOwnership(
                circuit,
                expression.bit(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (expression.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            validateClassicalRegisterOwnership(
                circuit,
                expression.register(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (expression.kind() == ClassicalExpressionKind.BINARY_OPERATION) {
            validateClassicalExpression(
                circuit,
                expression.leftExpression(),
                circuitIndex,
                operationIndex,
                errors
            );
            validateClassicalExpression(
                circuit,
                expression.rightExpression(),
                circuitIndex,
                operationIndex,
                errors
            );
        } else if (expression.kind() == ClassicalExpressionKind.CALL) {
            for (int i = 0; i < expression.callArgumentCount(); i++) {
                validateClassicalExpression(
                    circuit,
                    expression.callArgument(i),
                    circuitIndex,
                    operationIndex,
                    errors
                );
            }
        }
    }

    private void validateQuantumReferenceOwnership(
        final QuantumCircuit circuit,
        final QuantumReference reference,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        if (reference.kind() == QuantumReferenceKind.STATIC_QUBIT) {
            validateQubitOwnership(
                circuit,
                reference.qubit(),
                circuitIndex,
                operationIndex,
                errors
            );
            return;
        }
        if (reference.kind() == QuantumReferenceKind.HARDWARE_QUBIT) {
            return;
        }
        validateQuantumRegisterOwnership(
            circuit,
            reference.register(),
            circuitIndex,
            operationIndex,
            errors
        );
        validateClassicalExpression(
            circuit,
            reference.indexExpression(),
            circuitIndex,
            operationIndex,
            errors
        );
    }

    private void validateClassicalConditionRange(
        final ControlledOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        final ClassicalRegister register = operation.condition().register();
        if (
            register.size() < Long.SIZE - 1
            && operation.condition().expectedValue() >= (1L << register.size())
        ) {
            addError(
                errors,
                ValidationErrorCode.CLASSICAL_CONDITION_VALUE_OUT_OF_RANGE,
                "Classical condition value is outside of register value range.",
                circuitIndex,
                operationIndex
            );
        }
    }

    private void validateQubitOwnership(
        final QuantumCircuit circuit,
        final Qubit qubit,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            if (circuit.quantumRegister(i) == qubit.register()) {
                return;
            }
        }
        addError(
            errors,
            ValidationErrorCode.QUBIT_DOES_NOT_BELONG_TO_CIRCUIT,
            "Qubit does not belong to current circuit.",
            circuitIndex,
            operationIndex
        );
    }

    private void validateQuantumRegisterOwnership(
        final QuantumCircuit circuit,
        final QuantumRegister register,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
            if (circuit.quantumRegister(i) == register) {
                return;
            }
        }
        addError(
            errors,
            ValidationErrorCode.QUBIT_DOES_NOT_BELONG_TO_CIRCUIT,
            "Quantum register does not belong to current circuit.",
            circuitIndex,
            operationIndex
        );
    }

    private void validateClassicalBitOwnership(
        final QuantumCircuit circuit,
        final ClassicalBit bit,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            if (circuit.classicalRegister(i) == bit.register()) {
                return;
            }
        }
        addError(
            errors,
            ValidationErrorCode.CLASSICAL_BIT_DOES_NOT_BELONG_TO_CIRCUIT,
            "Classical bit does not belong to current circuit.",
            circuitIndex,
            operationIndex
        );
    }

    private void validateClassicalRegisterOwnership(
        final QuantumCircuit circuit,
        final ClassicalRegister register,
        final int circuitIndex,
        final int operationIndex,
        final ArrayList<ValidationError> errors
    ) {
        for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
            if (circuit.classicalRegister(i) == register) {
                return;
            }
        }
        addError(
            errors,
            ValidationErrorCode.CLASSICAL_REGISTER_DOES_NOT_BELONG_TO_CIRCUIT,
            "Classical register does not belong to current circuit.",
            circuitIndex,
            operationIndex
        );
    }

    private void addError(
        final ArrayList<ValidationError> errors,
        final ValidationErrorCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        errors.add(new ValidationError(
            code,
            message,
            circuitIndex,
            operationIndex
        ));
    }

    private List<String> referencedGateDefinitionNames(final Gate gate) {
        final ArrayList<String> names = new ArrayList<>();
        collectReferencedGateDefinitionNames(
            gate,
            names
        );
        return names;
    }

    private void collectReferencedGateDefinitionNames(
        final Gate gate,
        final ArrayList<String> names
    ) {
        if (gate instanceof GateDefinition definition) {
            names.add(definition.gateName());
        } else if (gate instanceof ModifiedGate modifiedGate) {
            collectReferencedGateDefinitionNames(
                modifiedGate.baseGate(),
                names
            );
        }
    }

    private boolean isSupportedKnownConstant(final String name) {
        return PI_CONSTANT_NAME.equals(name);
    }

    private final class ValidatorGateRuleErrorCollector implements GateValidationRuleErrorCollector {

        private final ArrayList<ValidationError> errors;
        private final int circuitIndex;
        private final int operationIndex;

        private ValidatorGateRuleErrorCollector(
            final ArrayList<ValidationError> errors,
            final int circuitIndex,
            final int operationIndex
        ) {
            this.errors = errors;
            this.circuitIndex = circuitIndex;
            this.operationIndex = operationIndex;
        }

        @Override
        public void addError(
            final ValidationErrorCode code,
            final String message
        ) {
            QuantumProgramValidator.this.addError(
                errors,
                code,
                message,
                circuitIndex,
                operationIndex
            );
        }
    }
}