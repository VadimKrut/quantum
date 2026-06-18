/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecomposition;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRule;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpressionKind;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicateKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BranchConditionKind;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
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
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindingResult;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterExpressionEvaluator;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Консервативный application-сервис трансформации Quantum IR без мутации исходной программы.
 */
public final class QuantumProgramTransformer {

    private final ParameterExpressionEvaluator parameterEvaluator;
    private final CapabilityPreflightChecker preflightChecker;

    /**
     * Создает transformer с доменными сервисами по умолчанию.
     */
    public QuantumProgramTransformer() {
        this(
            new ParameterExpressionEvaluator(),
            new CapabilityPreflightChecker()
        );
    }

    /**
     * Создает transformer с явно переданными сервисами.
     *
     * @param parameterEvaluator сервис параметрических выражений
     * @param preflightChecker сервис target preflight
     */
    public QuantumProgramTransformer(
        final ParameterExpressionEvaluator parameterEvaluator,
        final CapabilityPreflightChecker preflightChecker
    ) {
        if (parameterEvaluator == null) {
            throw new IllegalArgumentException("Parameter expression evaluator must not be null.");
        }
        if (preflightChecker == null) {
            throw new IllegalArgumentException("Capability preflight checker must not be null.");
        }
        this.parameterEvaluator = parameterEvaluator;
        this.preflightChecker = preflightChecker;
    }

    /**
     * Выполняет включенные пользователем консервативные трансформации.
     *
     * @param program исходная программа
     * @param options immutable опции трансформации
     * @return результат трансформации
     */
    public TransformationResult transform(
        final QuantumProgram program,
        final TransformationOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Transformation options must not be null.");
        }
        final TransformationContext context = new TransformationContext(
            options,
            program
        );
        context.recordDisabledSteps();
        context.recordTargetPreflight();
        final QuantumProgram transformedProgram = copyProgram(
            program,
            context
        );
        return TransformationResult.of(
            program,
            transformedProgram,
            context.appliedSteps,
            context.skippedSteps,
            context.diagnostics
        );
    }

    private QuantumProgram copyProgram(
        final QuantumProgram program,
        final TransformationContext context
    ) {
        final QuantumProgram result = QuantumProgram.create(program.computationModel());
        for (int i = 0; i < program.gateDefinitionCount(); i++) {
            result.addGateDefinition(program.gateDefinition(i));
        }
        for (int i = 0; i < program.classicalDeclarationCount(); i++) {
            result.addClassicalDeclaration(program.classicalDeclaration(i));
        }
        for (int i = 0; i < program.callableDefinitionCount(); i++) {
            result.addCallableDefinition(program.callableDefinition(i));
        }
        for (int i = 0; i < program.externalCallableDeclarationCount(); i++) {
            result.addExternalCallableDeclaration(program.externalCallableDeclaration(i));
        }
        for (int i = 0; i < program.calibrationDefinitionCount(); i++) {
            result.addCalibrationDefinition(program.calibrationDefinition(i));
        }
        for (int i = 0; i < program.circuitCount(); i++) {
            copyCircuit(
                program.circuit(i),
                result.createCircuit(program.circuit(i).name().value()),
                i,
                context
            );
        }
        return result;
    }

    private void copyCircuit(
        final QuantumCircuit source,
        final QuantumCircuit target,
        final int circuitIndex,
        final TransformationContext context
    ) {
        final CircuitReferenceMap references = new CircuitReferenceMap();
        target.reserveOperationCapacity(source.operationCount());
        for (int i = 0; i < source.quantumRegisterCount(); i++) {
            final QuantumRegister sourceRegister = source.quantumRegister(i);
            references.quantumRegisters.put(
                sourceRegister,
                target.createQuantumRegister(
                    sourceRegister.name().value(),
                    sourceRegister.size()
                )
            );
        }
        for (int i = 0; i < source.classicalRegisterCount(); i++) {
            final ClassicalRegister sourceRegister = source.classicalRegister(i);
            references.classicalRegisters.put(
                sourceRegister,
                target.createClassicalRegister(
                    sourceRegister.name().value(),
                    sourceRegister.size()
                )
            );
        }
        for (int i = 0; i < source.operationCount(); i++) {
            final int before = target.operationCount();
            final List<Operation> operations = transformOperation(
                source.operation(i),
                references,
                circuitIndex,
                i,
                context
            );
            appendOperations(
                target,
                operations
            );
            for (int j = before; j < target.operationCount(); j++) {
                target.setOperationMetadata(
                    j,
                    source.operationMetadata(i)
                );
            }
        }
    }

    private List<Operation> transformOperation(
        final Operation operation,
        final CircuitReferenceMap references,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        if (operation instanceof GateOperation gateOperation) {
            return transformGateOperation(
                gateOperation,
                references,
                circuitIndex,
                operationIndex,
                context,
                true
            );
        }
        final Operation remapped = remapNonGateOperation(
            operation,
            references,
            circuitIndex,
            operationIndex,
            context
        );
        return List.of(remapped);
    }

    private List<Operation> transformGateOperation(
        final GateOperation operation,
        final CircuitReferenceMap references,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context,
        final boolean allowDecomposition
    ) {
        final GateOperation remapped = GateOperation.parameterizedReferences(
            operation.gate(),
            bindParameters(
                operation.parameters(),
                circuitIndex,
                operationIndex,
                context
            ),
            remapQuantumReferences(
                operation.qubitReferences(),
                references
            )
        );
        if (
            context.options.removeIdentityGates()
            && remapped.gate() == StandardGate.ID
        ) {
            context.applied(
                TransformationStep.IDENTITY_GATE_REMOVAL,
                "Removed explicit identity gate at circuit " + circuitIndex + ", operation " + operationIndex + "."
            );
            return List.of();
        }
        if (
            context.options.inlineCompositeGates()
            && remapped.gate() instanceof GateDefinition definition
            && definition.kind() == GateDefinitionKind.COMPOSITE
        ) {
            return inlineCompositeGate(
                remapped,
                definition,
                references,
                circuitIndex,
                operationIndex,
                context
            );
        }
        if (
            allowDecomposition
            && context.options.applyDeclaredDecompositions()
        ) {
            final GateDecompositionRule rule = context.options.decompositionRegistry().findRule(remapped.gate());
            if (rule != null) {
                context.applied(
                    TransformationStep.DECLARED_GATE_DECOMPOSITION,
                    "Applied declared decomposition for gate " + remapped.gate().gateName() + "."
                );
                return transformDecomposition(
                    rule.decompose(remapped),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                );
            }
        }
        if (
            allowDecomposition
            && context.options.targetAwareLowering()
            && context.options.hasTargetProfile()
            && !context.options.targetProfile().supportsNativeGate(remapped.gate().gateName())
        ) {
            final GateDecompositionRule rule = context.options.decompositionRegistry().findRule(remapped.gate());
            if (rule == null) {
                context.diagnostics.add(TransformationDiagnostic.error(
                    TransformationDiagnosticCode.MISSING_GATE_DECOMPOSITION_RULE,
                    TransformationStep.TARGET_AWARE_LOWERING,
                    "Target " + context.options.targetProfile().targetName()
                        + " does not support gate " + remapped.gate().gateName()
                        + " and no declared lowering rule exists.",
                    circuitIndex,
                    operationIndex
                ));
                context.skipped(
                    TransformationStep.TARGET_AWARE_LOWERING,
                    "No declared lowering rule for gate " + remapped.gate().gateName() + "."
                );
                return List.of(remapped);
            }
            context.applied(
                TransformationStep.TARGET_AWARE_LOWERING,
                "Lowered gate " + remapped.gate().gateName()
                    + " for target " + context.options.targetProfile().targetName() + "."
            );
            return transformDecomposition(
                rule.decompose(remapped),
                references,
                circuitIndex,
                operationIndex,
                context
            );
        }
        return List.of(remapped);
    }

    private List<Operation> inlineCompositeGate(
        final GateOperation operation,
        final GateDefinition definition,
        final CircuitReferenceMap references,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (!operation.qubitReference(i).isStatic()) {
                context.diagnostics.add(TransformationDiagnostic.warning(
                    TransformationDiagnosticCode.NON_STATIC_COMPOSITE_GATE_OPERATION,
                    TransformationStep.COMPOSITE_GATE_INLINING,
                    "Composite gate inlining requires static qubit references.",
                    circuitIndex,
                    operationIndex
                ));
                context.skipped(
                    TransformationStep.COMPOSITE_GATE_INLINING,
                    "Composite gate " + definition.gateName() + " has non-static qubit reference."
                );
                return List.of(operation);
            }
        }
        final ArrayList<Operation> result = new ArrayList<>();
        for (int i = 0; i < definition.bodyOperations().size(); i++) {
            final GateBodyOperation bodyOperation = definition.bodyOperations().get(i);
            final GateOperation expanded = expandBodyOperation(
                operation,
                definition,
                bodyOperation
            );
            result.addAll(transformGateOperation(
                expanded,
                references,
                circuitIndex,
                operationIndex,
                context,
                true
            ));
        }
        context.applied(
            TransformationStep.COMPOSITE_GATE_INLINING,
            "Inlined composite gate " + definition.gateName() + "."
        );
        return result;
    }

    private List<Operation> transformDecomposition(
        final GateDecomposition decomposition,
        final CircuitReferenceMap references,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        final ArrayList<Operation> result = new ArrayList<>();
        for (int i = 0; i < decomposition.operationCount(); i++) {
            result.addAll(transformGateOperation(
                decomposition.operation(i),
                references,
                circuitIndex,
                operationIndex,
                context,
                false
            ));
        }
        return result;
    }

    private GateOperation expandBodyOperation(
        final GateOperation operation,
        final GateDefinition definition,
        final GateBodyOperation bodyOperation
    ) {
        final HashMap<String, ParameterExpression> parameterMap = new HashMap<>();
        for (int i = 0; i < definition.parameterNames().size(); i++) {
            parameterMap.put(
                definition.parameterNames().get(i),
                operation.parameter(i)
            );
        }
        final ParameterExpression[] parameters = new ParameterExpression[bodyOperation.parameterCount()];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = substituteParameter(
                bodyOperation.parameter(i),
                parameterMap
            );
        }
        final QuantumReference[] qubits = new QuantumReference[bodyOperation.qubitCount()];
        for (int i = 0; i < qubits.length; i++) {
            qubits[i] = operation.qubitReference(definition.qubitNames().indexOf(bodyOperation.qubitName(i)));
        }
        return GateOperation.parameterizedReferences(
            bodyOperation.gate(),
            parameters,
            qubits
        );
    }

    private static ParameterExpression substituteParameter(
        final ParameterExpression expression,
        final Map<String, ParameterExpression> parameterMap
    ) {
        if (
            expression.isNamed()
            && parameterMap.containsKey(expression.name())
        ) {
            return parameterMap.get(expression.name());
        }
        if (
            expression.isNumeric()
            || expression.isKnownConstant()
            || expression.isNamed()
        ) {
            return expression;
        }
        if (expression.isUnary()) {
            return ParameterExpression.negate(substituteParameter(
                expression.left(),
                parameterMap
            ));
        }
        final ParameterExpression left = substituteParameter(
            expression.left(),
            parameterMap
        );
        final ParameterExpression right = substituteParameter(
            expression.right(),
            parameterMap
        );
        return switch (expression.binaryOperator()) {
            case ADD -> ParameterExpression.add(
                left,
                right
            );
            case SUBTRACT -> ParameterExpression.subtract(
                left,
                right
            );
            case MULTIPLY -> ParameterExpression.multiply(
                left,
                right
            );
            case DIVIDE -> ParameterExpression.divide(
                left,
                right
            );
        };
    }

    private ParameterExpression[] bindParameters(
        final ParameterExpression[] parameters,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        final ParameterExpression[] result = new ParameterExpression[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            result[i] = bindParameter(
                parameters[i],
                circuitIndex,
                operationIndex,
                context
            );
        }
        return result;
    }

    private ParameterExpression bindParameter(
        final ParameterExpression parameter,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        if (
            !context.options.bindParameters()
            && !context.options.canonicalizeParameterExpressions()
        ) {
            return parameter;
        }
        try {
            final ParameterBindingResult result = parameterEvaluator.bind(
                parameter,
                context.options.bindParameters()
                    ? context.options.parameterBindings()
                    : ParameterBindings.empty()
            );
            if (
                context.options.requireCompleteParameterBinding()
                && !result.isComplete()
            ) {
                context.diagnostics.add(TransformationDiagnostic.error(
                    TransformationDiagnosticCode.UNBOUND_PARAMETER_SYMBOL,
                    TransformationStep.PARAMETER_BINDING,
                    "Parameter expression has unbound symbols: " + result.missingSymbols() + ".",
                    circuitIndex,
                    operationIndex
                ));
            }
            if (!result.expression().equals(parameter)) {
                context.applied(
                    context.options.bindParameters()
                        ? TransformationStep.PARAMETER_BINDING
                        : TransformationStep.PARAMETER_CANONICALIZATION,
                    "Transformed parameter expression at circuit " + circuitIndex
                        + ", operation " + operationIndex + "."
                );
            }
            return result.expression();
        } catch (final IllegalArgumentException exception) {
            context.diagnostics.add(TransformationDiagnostic.error(
                TransformationDiagnosticCode.UNKNOWN_PARAMETER_CONSTANT,
                TransformationStep.PARAMETER_CANONICALIZATION,
                exception.getMessage(),
                circuitIndex,
                operationIndex
            ));
            return parameter;
        }
    }

    private Operation remapNonGateOperation(
        final Operation operation,
        final CircuitReferenceMap references,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        if (operation instanceof MeasureOperation measureOperation) {
            return new MeasureOperation(
                remapQuantumReference(
                    measureOperation.qubitReference(),
                    references
                ),
                remapClassicalBit(
                    measureOperation.bit(),
                    references
                )
            );
        }
        if (operation instanceof ResetOperation resetOperation) {
            return new ResetOperation(remapQuantumReference(
                resetOperation.qubitReference(),
                references
            ));
        }
        if (operation instanceof BarrierOperation barrierOperation) {
            return new BarrierOperation(remapQubits(
                barrierOperation.qubits(),
                references
            ));
        }
        if (operation instanceof ControlledOperation controlledOperation) {
            return new ControlledOperation(
                remapClassicalCondition(
                    controlledOperation.condition(),
                    references
                ),
                collapseNestedOperations(transformOperation(
                    controlledOperation.operation(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                ))
            );
        }
        if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            return new ClassicallyControlledOperation(
                remapPredicate(
                    controlledOperation.predicate(),
                    references
                ),
                collapseNestedOperations(transformOperation(
                    controlledOperation.operation(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                ))
            );
        }
        if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            return new ClassicalAssignmentOperation(new ClassicalAssignment(
                remapExpression(
                    assignmentOperation.assignment().target(),
                    references
                ),
                remapExpression(
                    assignmentOperation.assignment().value(),
                    references
                )
            ));
        }
        if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            return new ClassicalDeclarationOperation(
                declarationOperation.declaration(),
                declarationOperation.hasInitializer()
                    ? remapExpression(
                        declarationOperation.initializer(),
                        references
                    )
                    : null
            );
        }
        if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            final ArrayList<ClassicalExpression> dimensions = new ArrayList<>();
            for (int i = 0; i < arrayOperation.dimensionCount(); i++) {
                dimensions.add(remapExpression(
                    arrayOperation.dimension(i),
                    references
                ));
            }
            return new ClassicalArrayDeclarationOperation(
                arrayOperation.name(),
                arrayOperation.elementType(),
                dimensions,
                arrayOperation.hasInitializerText()
                    ? arrayOperation.initializerText()
                    : null
            );
        }
        if (operation instanceof CallableInvocationOperation invocationOperation) {
            final ArrayList<ClassicalExpression> classicalArguments = new ArrayList<>();
            final ArrayList<QuantumReference> quantumArguments = new ArrayList<>();
            for (int i = 0; i < invocationOperation.classicalArguments().size(); i++) {
                classicalArguments.add(remapExpression(
                    invocationOperation.classicalArguments().get(i),
                    references
                ));
            }
            for (int i = 0; i < invocationOperation.quantumArguments().size(); i++) {
                quantumArguments.add(remapQuantumReference(
                    invocationOperation.quantumArguments().get(i),
                    references
                ));
            }
            return new CallableInvocationOperation(
                invocationOperation.callableName(),
                invocationOperation.hasTarget()
                    ? remapExpression(
                        invocationOperation.target(),
                        references
                    )
                    : null,
                classicalArguments,
                quantumArguments
            );
        }
        if (operation instanceof BlockOperation blockOperation) {
            return new BlockOperation(remapBlock(
                blockOperation.body(),
                references,
                circuitIndex,
                operationIndex,
                context
            ));
        }
        if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            return new ConditionalBlockOperation(
                remapPredicate(
                    conditionalOperation.predicate(),
                    references
                ),
                remapBlock(
                    conditionalOperation.thenBlock(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                ),
                conditionalOperation.hasElseBlock()
                    ? remapBlock(
                        conditionalOperation.elseBlock(),
                        references,
                        circuitIndex,
                        operationIndex,
                        context
                    )
                    : null
            );
        }
        if (operation instanceof ForLoopOperation loopOperation) {
            return new ForLoopOperation(
                loopOperation.variableName(),
                loopOperation.startInclusive(),
                loopOperation.step(),
                loopOperation.endInclusive(),
                remapBlock(
                    loopOperation.body(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                )
            );
        }
        if (operation instanceof SymbolicForLoopOperation loopOperation) {
            return new SymbolicForLoopOperation(
                loopOperation.variableName(),
                loopOperation.hasVariableTypeText()
                    ? loopOperation.variableTypeText()
                    : null,
                remapExpression(
                    loopOperation.startInclusive(),
                    references
                ),
                remapExpression(
                    loopOperation.step(),
                    references
                ),
                remapExpression(
                    loopOperation.endInclusive(),
                    references
                ),
                remapBlock(
                    loopOperation.body(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                )
            );
        }
        if (operation instanceof WhileLoopOperation loopOperation) {
            return new WhileLoopOperation(
                remapPredicate(
                    loopOperation.predicate(),
                    references
                ),
                remapBlock(
                    loopOperation.body(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                )
            );
        }
        if (operation instanceof DelayOperation delayOperation) {
            return new DelayOperation(
                delayOperation.duration(),
                remapQuantumReferences(
                    delayOperation.references(),
                    references
                )
            );
        }
        if (operation instanceof TimingBoxOperation boxOperation) {
            return new TimingBoxOperation(
                boxOperation.hasDuration()
                    ? boxOperation.duration()
                    : null,
                remapBlock(
                    boxOperation.body(),
                    references,
                    circuitIndex,
                    operationIndex,
                    context
                )
            );
        }
        if (operation instanceof BranchOperation branchOperation) {
            return remapBranchOperation(
                branchOperation,
                references
            );
        }
        if (operation instanceof LabelOperation labelOperation) {
            return new LabelOperation(labelOperation.name());
        }
        if (operation instanceof HaltOperation) {
            return HaltOperation.INSTANCE;
        }
        if (operation instanceof WaitOperation) {
            return WaitOperation.INSTANCE;
        }
        context.diagnostics.add(TransformationDiagnostic.warning(
            TransformationDiagnosticCode.UNSUPPORTED_TRANSFORMATION_OPERATION,
            TransformationStep.PARAMETER_CANONICALIZATION,
            "Operation is copied unchanged because transformer does not know its concrete type: "
                + operation.getClass().getName() + ".",
            circuitIndex,
            operationIndex
        ));
        return operation;
    }

    private OperationBlock remapBlock(
        final OperationBlock body,
        final CircuitReferenceMap references,
        final int circuitIndex,
        final int operationIndex,
        final TransformationContext context
    ) {
        final ArrayList<Operation> operations = new ArrayList<>();
        for (int i = 0; i < body.operationCount(); i++) {
            operations.addAll(transformOperation(
                body.operation(i),
                references,
                circuitIndex,
                operationIndex,
                context
            ));
        }
        return OperationBlock.of(operations);
    }

    private static Operation collapseNestedOperations(final List<Operation> operations) {
        if (operations.isEmpty()) {
            return new BlockOperation(OperationBlock.of(List.of()));
        }
        if (operations.size() == 1) {
            return operations.get(0);
        }
        return new BlockOperation(OperationBlock.of(operations));
    }

    private static BranchOperation remapBranchOperation(
        final BranchOperation operation,
        final CircuitReferenceMap references
    ) {
        if (operation.conditionKind() == BranchConditionKind.ALWAYS) {
            return BranchOperation.always(operation.targetLabel());
        }
        if (operation.conditionKind() == BranchConditionKind.WHEN_TRUE) {
            return BranchOperation.whenTrue(
                operation.targetLabel(),
                remapExpression(
                    operation.condition(),
                    references
                )
            );
        }
        return BranchOperation.whenFalse(
            operation.targetLabel(),
            remapExpression(
                operation.condition(),
                references
            )
        );
    }

    private static ClassicalPredicate remapPredicate(
        final ClassicalPredicate predicate,
        final CircuitReferenceMap references
    ) {
        if (predicate.kind() == ClassicalPredicateKind.COMPARISON) {
            return ClassicalPredicate.compare(
                remapExpression(
                    predicate.leftExpression(),
                    references
                ),
                predicate.comparisonOperator(),
                remapExpression(
                    predicate.rightExpression(),
                    references
                )
            );
        }
        if (predicate.kind() == ClassicalPredicateKind.NOT) {
            return ClassicalPredicate.not(remapPredicate(
                predicate.leftPredicate(),
                references
            ));
        }
        return predicate.booleanOperator().name().equals("AND")
            ? ClassicalPredicate.and(
                remapPredicate(
                    predicate.leftPredicate(),
                    references
                ),
                remapPredicate(
                    predicate.rightPredicate(),
                    references
                )
            )
            : ClassicalPredicate.or(
                remapPredicate(
                    predicate.leftPredicate(),
                    references
                ),
                remapPredicate(
                    predicate.rightPredicate(),
                    references
                )
            );
    }

    private static ClassicalExpression remapExpression(
        final ClassicalExpression expression,
        final CircuitReferenceMap references
    ) {
        if (expression.kind() == ClassicalExpressionKind.INTEGER) {
            return ClassicalExpression.integer(expression.integerValue());
        }
        if (expression.kind() == ClassicalExpressionKind.VARIABLE_REFERENCE) {
            return ClassicalExpression.variable(expression.variableName());
        }
        if (expression.kind() == ClassicalExpressionKind.BIT_REFERENCE) {
            return ClassicalExpression.bit(remapClassicalBit(
                expression.bit(),
                references
            ));
        }
        if (expression.kind() == ClassicalExpressionKind.REGISTER_REFERENCE) {
            final ClassicalRegister register = references.classicalRegisters.get(expression.register());
            return ClassicalExpression.register(register == null
                ? expression.register()
                : register);
        }
        if (expression.kind() == ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            return ClassicalExpression.symbolicReference(expression.symbolicText());
        }
        if (expression.kind() == ClassicalExpressionKind.CALL) {
            final ArrayList<ClassicalExpression> arguments = new ArrayList<>();
            for (int i = 0; i < expression.callArgumentCount(); i++) {
                arguments.add(remapExpression(
                    expression.callArgument(i),
                    references
                ));
            }
            return ClassicalExpression.call(
                expression.callableName(),
                arguments
            );
        }
        return ClassicalExpression.binary(
            expression.binaryOperator(),
            remapExpression(
                expression.leftExpression(),
                references
            ),
            remapExpression(
                expression.rightExpression(),
                references
            )
        );
    }

    private static ClassicalCondition remapClassicalCondition(
        final ClassicalCondition condition,
        final CircuitReferenceMap references
    ) {
        final ClassicalRegister register = references.classicalRegisters.get(condition.register());
        return ClassicalCondition.equalTo(
            register == null
                ? condition.register()
                : register,
            condition.expectedValue()
        );
    }

    private static QuantumReference[] remapQuantumReferences(
        final QuantumReference[] source,
        final CircuitReferenceMap references
    ) {
        final QuantumReference[] result = new QuantumReference[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = remapQuantumReference(
                source[i],
                references
            );
        }
        return result;
    }

    private static QuantumReference remapQuantumReference(
        final QuantumReference reference,
        final CircuitReferenceMap references
    ) {
        if (reference.kind() == QuantumReferenceKind.HARDWARE_QUBIT) {
            return QuantumReference.hardwareQubit(reference.hardwareIndex());
        }
        if (reference.kind() == QuantumReferenceKind.DYNAMIC_REGISTER_INDEX) {
            final QuantumRegister register = references.quantumRegisters.get(reference.register());
            return QuantumReference.dynamicIndex(
                register == null
                    ? reference.register()
                    : register,
                remapExpression(
                    reference.indexExpression(),
                    references
                )
            );
        }
        return QuantumReference.staticQubit(remapQubit(
            reference.qubit(),
            references
        ));
    }

    private static Qubit[] remapQubits(
        final Qubit[] source,
        final CircuitReferenceMap references
    ) {
        final Qubit[] result = new Qubit[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = remapQubit(
                source[i],
                references
            );
        }
        return result;
    }

    private static Qubit remapQubit(
        final Qubit qubit,
        final CircuitReferenceMap references
    ) {
        final QuantumRegister register = references.quantumRegisters.get(qubit.register());
        if (register == null) {
            return qubit;
        }
        return register.get(qubit.index());
    }

    private static ClassicalBit remapClassicalBit(
        final ClassicalBit bit,
        final CircuitReferenceMap references
    ) {
        final ClassicalRegister register = references.classicalRegisters.get(bit.register());
        if (register == null) {
            return bit;
        }
        return register.get(bit.index());
    }

    private static void appendOperations(
        final QuantumCircuit circuit,
        final List<Operation> operations
    ) {
        for (int i = 0; i < operations.size(); i++) {
            appendOperation(
                circuit,
                operations.get(i)
            );
        }
    }

    private static void appendOperation(
        final QuantumCircuit circuit,
        final Operation operation
    ) {
        if (operation instanceof GateOperation gateOperation) {
            circuit.parameterizedGateReferences(
                gateOperation.gate(),
                gateOperation.parameters(),
                gateOperation.qubitReferences()
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            circuit.measureReference(
                measureOperation.qubitReference(),
                measureOperation.bit()
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            circuit.resetReference(resetOperation.qubitReference());
        } else if (operation instanceof BarrierOperation barrierOperation) {
            circuit.barrier(barrierOperation.qubits());
        } else if (operation instanceof ControlledOperation controlledOperation) {
            circuit.controlled(
                controlledOperation.condition(),
                controlledOperation.operation()
            );
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            circuit.classicallyControlled(
                controlledOperation.predicate(),
                controlledOperation.operation()
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            circuit.assign(assignmentOperation.assignment());
        } else if (operation instanceof ClassicalDeclarationOperation declarationOperation) {
            circuit.classicalDeclaration(declarationOperation);
        } else if (operation instanceof ClassicalArrayDeclarationOperation arrayOperation) {
            circuit.classicalArrayDeclaration(arrayOperation);
        } else if (operation instanceof CallableInvocationOperation invocationOperation) {
            circuit.callableInvocation(invocationOperation);
        } else if (operation instanceof BlockOperation blockOperation) {
            circuit.block(blockOperation.body());
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            circuit.conditionalBlock(
                conditionalOperation.predicate(),
                conditionalOperation.thenBlock(),
                conditionalOperation.hasElseBlock()
                    ? conditionalOperation.elseBlock()
                    : null
            );
        } else if (operation instanceof ForLoopOperation loopOperation) {
            circuit.forLoop(
                loopOperation.variableName(),
                loopOperation.startInclusive(),
                loopOperation.step(),
                loopOperation.endInclusive(),
                loopOperation.body()
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            circuit.symbolicForLoop(loopOperation);
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            circuit.whileLoop(
                loopOperation.predicate(),
                loopOperation.body()
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            circuit.delayReferences(
                delayOperation.duration(),
                delayOperation.references()
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            circuit.timingBox(
                boxOperation.hasDuration()
                    ? boxOperation.duration()
                    : null,
                boxOperation.body()
            );
        } else if (operation instanceof BranchOperation branchOperation) {
            circuit.branch(branchOperation);
        } else if (operation instanceof LabelOperation labelOperation) {
            circuit.label(labelOperation.name());
        } else if (operation instanceof HaltOperation) {
            circuit.halt();
        } else if (operation instanceof WaitOperation) {
            circuit.waitInstruction();
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + operation.getClass().getName());
        }
    }

    private final class TransformationContext {

        private final TransformationOptions options;
        private final QuantumProgram originalProgram;
        private final ArrayList<TransformationStepRecord> appliedSteps;
        private final ArrayList<TransformationStepRecord> skippedSteps;
        private final ArrayList<TransformationDiagnostic> diagnostics;

        private TransformationContext(
            final TransformationOptions options,
            final QuantumProgram originalProgram
        ) {
            this.options = options;
            this.originalProgram = originalProgram;
            this.appliedSteps = new ArrayList<>();
            this.skippedSteps = new ArrayList<>();
            this.diagnostics = new ArrayList<>();
        }

        private void applied(
            final TransformationStep step,
            final String message
        ) {
            appliedSteps.add(TransformationStepRecord.of(
                step,
                message
            ));
        }

        private void skipped(
            final TransformationStep step,
            final String message
        ) {
            skippedSteps.add(TransformationStepRecord.of(
                step,
                message
            ));
        }

        private void recordDisabledSteps() {
            if (!options.bindParameters()) {
                skipped(
                    TransformationStep.PARAMETER_BINDING,
                    "Parameter binding is disabled."
                );
            }
            if (!options.canonicalizeParameterExpressions()) {
                skipped(
                    TransformationStep.PARAMETER_CANONICALIZATION,
                    "Parameter expression canonicalization is disabled."
                );
            }
            if (!options.removeIdentityGates()) {
                skipped(
                    TransformationStep.IDENTITY_GATE_REMOVAL,
                    "Explicit identity gate removal is disabled."
                );
            }
            if (!options.inlineCompositeGates()) {
                skipped(
                    TransformationStep.COMPOSITE_GATE_INLINING,
                    "Composite gate inlining is disabled."
                );
            }
            if (!options.applyDeclaredDecompositions()) {
                skipped(
                    TransformationStep.DECLARED_GATE_DECOMPOSITION,
                    "Declared gate decomposition is disabled."
                );
            }
            if (!options.targetAwareLowering()) {
                skipped(
                    TransformationStep.TARGET_AWARE_LOWERING,
                    "Target-aware lowering is disabled."
                );
            }
        }

        private void recordTargetPreflight() {
            if (
                !options.targetAwareLowering()
                || !options.hasTargetProfile()
            ) {
                return;
            }
            final CapabilityPreflightResult result = preflightChecker.check(
                originalProgram,
                options.targetProfile()
            );
            if (result.status() == CapabilityPreflightStatus.EXPORTABLE) {
                skipped(
                    TransformationStep.TARGET_AWARE_LOWERING,
                    "Target " + options.targetProfile().targetName() + " accepts the original program."
                );
            } else if (result.status() == CapabilityPreflightStatus.LOWERING_REQUIRED) {
                diagnostics.add(TransformationDiagnostic.targetWarning(
                    TransformationDiagnosticCode.TARGET_LOWERING_REQUIRED,
                    TransformationStep.TARGET_AWARE_LOWERING,
                    "Target " + options.targetProfile().targetName() + " requires lowering before export.",
                    options.targetProfile().targetName()
                ));
            } else {
                diagnostics.add(TransformationDiagnostic.targetWarning(
                    TransformationDiagnosticCode.TARGET_UNSUPPORTED_WITHOUT_LOSS,
                    TransformationStep.TARGET_AWARE_LOWERING,
                    "Target " + options.targetProfile().targetName()
                        + " cannot represent all original IR features without loss.",
                    options.targetProfile().targetName()
                ));
            }
        }
    }

    private static final class CircuitReferenceMap {

        private final IdentityHashMap<QuantumRegister, QuantumRegister> quantumRegisters = new IdentityHashMap<>();
        private final IdentityHashMap<ClassicalRegister, ClassicalRegister> classicalRegisters = new IdentityHashMap<>();
    }
}