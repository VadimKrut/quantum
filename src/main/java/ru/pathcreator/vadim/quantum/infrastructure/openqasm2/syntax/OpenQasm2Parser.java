/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.mapping.OpenQasm2GateNames;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.mapping.OpenQasm2GateMapper;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast.OpenQasm2Ast;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast.OpenQasm2AstParser;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast.OpenQasm2AstStatement;

/**
 * Parser поддерживаемого подмножества OpenQASM 2.0 в Quantum IR.
 */
public final class OpenQasm2Parser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^OPENQASM\\s+2(?:\\.0)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^include\\s+\"([^\"]+)\"$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QREG_PATTERN = Pattern.compile("^qreg\\s+([A-Za-z_][A-Za-z0-9_]*)\\[(\\d+)]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREG_PATTERN = Pattern.compile("^creg\\s+([A-Za-z_][A-Za-z0-9_]*)\\[(\\d+)]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEASURE_PATTERN = Pattern.compile("^measure\\s+(.+)\\s*->\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESET_PATTERN = Pattern.compile("^reset\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARRIER_PATTERN = Pattern.compile("^barrier\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*==\\s*(\\d+)\\s*\\)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPAQUE_PATTERN = Pattern.compile("^opaque\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_DEFINITION_PATTERN = Pattern.compile("^gate\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+([^{}]+)\\s*\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern GATE_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+(.+)$");
    private static final Pattern INDEXED_ARGUMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\[(\\d+)]$");
    private static final Pattern REGISTER_ARGUMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final OpenQasm2AstParser astParser;

    public OpenQasm2Parser() {
        this.astParser = new OpenQasm2AstParser();
    }

    /**
     * Разбирает OpenQASM 2.0 text в Quantum IR.
     *
     * @param source OpenQASM 2.0 text
     * @return результат parser
     */
    public OpenQasm2ParserResult parse(final String source) {
        return parse(
            source,
            Map.of()
        );
    }

    /**
     * Разбирает OpenQASM 2.0 text с доступными include sources.
     *
     * @param source OpenQASM 2.0 text
     * @param includedSources тексты include-файлов по имени include
     * @return результат parser
     */
    public OpenQasm2ParserResult parse(
        final String source,
        final Map<String, String> includedSources
    ) {
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        if (source == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.NULL_INPUT,
                "OpenQASM 2 source must not be null."
            ));
            return new OpenQasm2ParserResult(
                null,
                diagnostics
            );
        }
        if (source.isBlank()) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.EMPTY_INPUT,
                "OpenQASM 2 source must not be blank."
            ));
            return new OpenQasm2ParserResult(
                null,
                diagnostics
            );
        }
        if (includedSources == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.INVALID_OPTIONS,
                "Included sources must not be null."
            ));
            return new OpenQasm2ParserResult(
                null,
                diagnostics
            );
        }

        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final ParseContext context = new ParseContext(
            program,
            circuit,
            diagnostics,
            includedSources
        );
        final OpenQasm2Ast ast = astParser.parse(source);
        diagnostics.addAll(ast.diagnostics());
        if (hasErrors(diagnostics)) {
            return new OpenQasm2ParserResult(
                null,
                diagnostics
            );
        }
        final ArrayList<Statement> statements = statementsFromAst(ast);
        boolean versionSeen = false;

        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            if (statement.text().isBlank()) {
                continue;
            }
            if (VERSION_PATTERN.matcher(statement.text()).matches()) {
                versionSeen = true;
            } else if (!versionSeen) {
                diagnostics.add(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "OpenQASM 2 source must start with OPENQASM 2.0.",
                    statement.line(),
                    statement.column()
                ));
            } else {
                parseStatement(
                    context,
                    statement
                );
            }
        }

        if (!versionSeen) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 2 version declaration is missing."
            ));
        }
        if (context.hasErrors()) {
            return new OpenQasm2ParserResult(
                null,
                diagnostics
            );
        }
        return new OpenQasm2ParserResult(
            program,
            diagnostics
        );
    }

    private static boolean hasErrors(final ArrayList<IntegrationDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<Statement> statementsFromAst(final OpenQasm2Ast ast) {
        final ArrayList<Statement> statements = new ArrayList<>();
        for (int i = 0; i < ast.statements().size(); i++) {
            final OpenQasm2AstStatement statement = ast.statements().get(i);
            statements.add(new Statement(
                statement.text(),
                statement.line(),
                statement.column()
            ));
        }
        return statements;
    }

    private static void parseStatement(
        final ParseContext context,
        final Statement statement
    ) {
        final Matcher includeMatcher = INCLUDE_PATTERN.matcher(statement.text());
        final Matcher qregMatcher = QREG_PATTERN.matcher(statement.text());
        final Matcher cregMatcher = CREG_PATTERN.matcher(statement.text());
        final Matcher measureMatcher = MEASURE_PATTERN.matcher(statement.text());
        final Matcher resetMatcher = RESET_PATTERN.matcher(statement.text());
        final Matcher barrierMatcher = BARRIER_PATTERN.matcher(statement.text());
        final Matcher ifMatcher = IF_PATTERN.matcher(statement.text());
        final Matcher opaqueMatcher = OPAQUE_PATTERN.matcher(statement.text());
        final Matcher gateDefinitionMatcher = GATE_DEFINITION_PATTERN.matcher(statement.text());

        if (includeMatcher.matches()) {
            parseInclude(
                context,
                statement,
                includeMatcher.group(1)
            );
        } else if (opaqueMatcher.matches()) {
            parseOpaqueGateDefinition(
                context,
                statement,
                opaqueMatcher
            );
        } else if (gateDefinitionMatcher.matches()) {
            parseCompositeGateDefinition(
                context,
                statement,
                gateDefinitionMatcher
            );
        } else if (qregMatcher.matches()) {
            parseQuantumRegister(
                context,
                statement,
                qregMatcher
            );
        } else if (cregMatcher.matches()) {
            parseClassicalRegister(
                context,
                statement,
                cregMatcher
            );
        } else if (ifMatcher.matches()) {
            parseControlled(
                context,
                statement,
                ifMatcher
            );
        } else if (measureMatcher.matches()) {
            parseMeasure(
                context,
                statement,
                measureMatcher.group(1),
                measureMatcher.group(2)
            );
        } else if (resetMatcher.matches()) {
            parseReset(
                context,
                statement,
                resetMatcher.group(1)
            );
        } else if (barrierMatcher.matches()) {
            parseBarrier(
                context,
                statement,
                barrierMatcher.group(1)
            );
        } else if (isUnsupportedStatement(statement.text())) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 2 import does not support this statement yet: " + statement.text() + ".",
                statement
            );
        } else {
            parseGate(
                context,
                statement
            );
        }
    }

    private static boolean isUnsupportedStatement(final String statement) {
        final String lowerCaseStatement = statement.toLowerCase();
        return lowerCaseStatement.startsWith("if ");
    }

    private static void parseInclude(
        final ParseContext context,
        final Statement statement,
        final String includeName
    ) {
        if ("qelib1.inc".equals(includeName)) {
            return;
        }
        final String includedSource = context.includedSource(includeName);
        if (includedSource == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 2 include source is not available: " + includeName + ".",
                statement
            );
            return;
        }
        parseIncludedSource(
            context,
            statement,
            includeName,
            includedSource
        );
    }

    private static void parseIncludedSource(
        final ParseContext context,
        final Statement includeStatement,
        final String includeName,
        final String includedSource
    ) {
        if (context.isIncludeActive(includeName)) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 2 include cycle detected: " + includeName + ".",
                includeStatement
            );
            return;
        }
        context.enterInclude(includeName);
        final OpenQasm2Ast ast = new OpenQasm2AstParser().parse(includedSource);
        context.addDiagnostics(new ArrayList<>(ast.diagnostics()));
        if (context.hasErrors()) {
            context.exitInclude(includeName);
            return;
        }
        final ArrayList<Statement> statements = statementsFromAst(ast);
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            if (
                statement.text().isBlank()
                || VERSION_PATTERN.matcher(statement.text()).matches()
            ) {
                continue;
            }
            parseStatement(
                context,
                statement
            );
            if (context.hasErrors()) {
                context.exitInclude(includeName);
                return;
            }
        }
        context.exitInclude(includeName);
    }

    private static void parseQuantumRegister(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final String name = matcher.group(1);
        final int size = parsePositiveInteger(
            context,
            statement,
            matcher.group(2)
        );
        if (size <= 0) {
            return;
        }
        try {
            context.addQuantumRegister(
                name,
                context.circuit().createQuantumRegister(
                    name,
                    size
                )
            );
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
        }
    }

    private static void parseClassicalRegister(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final String name = matcher.group(1);
        final int size = parsePositiveInteger(
            context,
            statement,
            matcher.group(2)
        );
        if (size <= 0) {
            return;
        }
        try {
            context.addClassicalRegister(
                name,
                context.circuit().createClassicalRegister(
                    name,
                    size
                )
            );
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
        }
    }

    private static void parseOpaqueGateDefinition(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        if (isReservedGateDefinitionName(
            context,
            statement,
            matcher.group(1)
        )) {
            return;
        }
        final ListParts parameterNames = parseDeclarationNames(
            context,
            statement,
            matcher.group(2)
        );
        final ListParts qubitNames = parseDeclarationNames(
            context,
            statement,
            matcher.group(3)
        );
        if (
            parameterNames == null
            || qubitNames == null
        ) {
            return;
        }
        try {
            final GateDefinition definition = GateDefinition.opaque(
                matcher.group(1),
                parameterNames.parts(),
                qubitNames.parts()
            );
            context.program().addGateDefinition(definition);
            context.addGateDefinition(definition);
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
        }
    }

    private static void parseCompositeGateDefinition(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        if (isReservedGateDefinitionName(
            context,
            statement,
            matcher.group(1)
        )) {
            return;
        }
        final ListParts parameterNames = parseDeclarationNames(
            context,
            statement,
            matcher.group(2)
        );
        final ListParts qubitNames = parseDeclarationNames(
            context,
            statement,
            matcher.group(3)
        );
        if (
            parameterNames == null
            || qubitNames == null
        ) {
            return;
        }
        final ArrayList<GateBodyOperation> bodyOperations = parseGateBodyOperations(
            context,
            statement,
            matcher.group(4)
        );
        if (bodyOperations == null) {
            return;
        }
        try {
            final GateDefinition definition = GateDefinition.composite(
                matcher.group(1),
                parameterNames.parts(),
                qubitNames.parts(),
                bodyOperations
            );
            context.program().addGateDefinition(definition);
            context.addGateDefinition(definition);
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
        }
    }

    private static boolean isReservedGateDefinitionName(
        final ParseContext context,
        final Statement statement,
        final String name
    ) {
        if (OpenQasm2GateNames.isReservedQelibAlias(name)) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 2 input declares a custom gate with reserved qelib1 name: " + name + ".",
                statement
            );
            return true;
        }
        return false;
    }

    private static ListParts parseDeclarationNames(
        final ParseContext context,
        final Statement statement,
        final String text
    ) {
        if (
            text == null
            || text.isBlank()
        ) {
            return new ListParts(new ArrayList<>());
        }
        final ArrayList<String> names = new ArrayList<>();
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            text,
            "declaration list"
        );
        if (parts == null) {
            return null;
        }
        for (int i = 0; i < parts.parts().size(); i++) {
            final String name = parts.parts().get(i).trim();
            if (!IDENTIFIER_PATTERN.matcher(name).matches()) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Invalid gate declaration identifier: " + name + ".",
                    statement
                );
                return null;
            }
            names.add(name);
        }
        return new ListParts(names);
    }

    private static ArrayList<GateBodyOperation> parseGateBodyOperations(
        final ParseContext context,
        final Statement statement,
        final String body
    ) {
        final ArrayList<GateBodyOperation> operations = new ArrayList<>();
        final String[] statements = body.split(";");
        for (int i = 0; i < statements.length; i++) {
            final String bodyStatement = statements[i].trim();
            if (bodyStatement.isBlank()) {
                continue;
            }
            final GateBodyOperation operation = parseGateBodyOperation(
                context,
                statement,
                bodyStatement
            );
            if (operation == null) {
                return null;
            }
            operations.add(operation);
        }
        return operations;
    }

    private static GateBodyOperation parseGateBodyOperation(
        final ParseContext context,
        final Statement statement,
        final String bodyStatement
    ) {
        final Matcher matcher = GATE_PATTERN.matcher(bodyStatement);
        if (!matcher.matches()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse gate body operation: " + bodyStatement + ".",
                statement
            );
            return null;
        }
        if (isOpenQasmOperationKeyword(matcher.group(1))) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 2 gate body supports gate operations only.",
                statement
            );
            return null;
        }
        final Gate gate = resolveGate(
            context,
            matcher.group(1)
        );
        if (gate == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 2 import does not support gate in body: " + matcher.group(1) + ".",
                statement
            );
            return null;
        }
        final ParameterExpression[] parameters = parseParameters(
            context,
            statement,
            matcher.group(2),
            gate.parameterCount()
        );
        if (parameters == null) {
            return null;
        }
        final ListParts qubitNames = parseDeclarationNames(
            context,
            statement,
            matcher.group(3)
        );
        if (qubitNames == null) {
            return null;
        }
        try {
            return GateBodyOperation.of(
                gate,
                parameters,
                qubitNames.parts().toArray(new String[0])
            );
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
            return null;
        }
    }

    private static void parseMeasure(
        final ParseContext context,
        final Statement statement,
        final String qubitText,
        final String bitText
    ) {
        final QuantumOperand qubits = parseQuantumOperand(
            context,
            statement,
            qubitText.trim()
        );
        final ClassicalOperand bits = parseClassicalOperand(
            context,
            statement,
            bitText.trim()
        );
        if (
            qubits == null
            || bits == null
        ) {
            return;
        }
        if (qubits.size() != bits.size()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Measure source and target sizes must match.",
                statement
            );
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        for (int i = 0; i < qubits.size(); i++) {
            context.circuit().measure(
                qubits.qubit(i),
                bits.bit(i)
            );
        }
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseReset(
        final ParseContext context,
        final Statement statement,
        final String qubitText
    ) {
        final QuantumOperand qubits = parseQuantumOperand(
            context,
            statement,
            qubitText.trim()
        );
        if (qubits == null) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        for (int i = 0; i < qubits.size(); i++) {
            context.circuit().reset(qubits.qubit(i));
        }
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseBarrier(
        final ParseContext context,
        final Statement statement,
        final String qubitText
    ) {
        final ArrayList<Qubit> qubits = new ArrayList<>();
        final String[] parts = qubitText.split(",");
        for (int i = 0; i < parts.length; i++) {
            final QuantumOperand operand = parseQuantumOperand(
                context,
                statement,
                parts[i].trim()
            );
            if (operand == null) {
                return;
            }
            for (int j = 0; j < operand.size(); j++) {
                qubits.add(operand.qubit(j));
            }
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().barrier(qubits.toArray(new Qubit[0]));
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseControlled(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalRegister register = context.classicalRegister(matcher.group(1));
        if (register == null) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Unknown classical register in condition: " + matcher.group(1) + ".",
                statement
            );
            return;
        }
        final long expectedValue = parseNonNegativeLong(
            context,
            statement,
            matcher.group(2)
        );
        if (expectedValue < 0) {
            return;
        }
        final ClassicalPredicate predicate = ClassicalPredicate.compare(
            ClassicalExpression.register(register),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(expectedValue)
        );
        final String nestedStatement = matcher.group(3).trim();
        final Matcher measureMatcher = MEASURE_PATTERN.matcher(nestedStatement);
        if (measureMatcher.matches()) {
            parseControlledMeasure(
                context,
                statement,
                predicate,
                measureMatcher
            );
            return;
        }
        final Matcher resetMatcher = RESET_PATTERN.matcher(nestedStatement);
        if (resetMatcher.matches()) {
            final QuantumOperand operand = parseQuantumOperand(
                context,
                statement,
                resetMatcher.group(1).trim()
            );
            if (operand == null) {
                return;
            }
            final int firstOperationIndex = context.circuit().operationCount();
            for (int i = 0; i < operand.size(); i++) {
                context.circuit().classicallyControlled(
                    predicate,
                    new ResetOperation(operand.qubit(i))
                );
            }
            context.attachMetadataFrom(
                firstOperationIndex,
                statement
            );
            return;
        }
        final Matcher barrierMatcher = BARRIER_PATTERN.matcher(nestedStatement);
        if (barrierMatcher.matches()) {
            parseControlledBarrier(
                context,
                statement,
                predicate,
                barrierMatcher
            );
            return;
        }
        parseControlledGate(
            context,
            statement,
            predicate,
            nestedStatement
        );
    }

    private static void parseControlledMeasure(
        final ParseContext context,
        final Statement statement,
        final ClassicalPredicate predicate,
        final Matcher matcher
    ) {
        final QuantumOperand qubits = parseQuantumOperand(
            context,
            statement,
            matcher.group(1).trim()
        );
        final ClassicalOperand bits = parseClassicalOperand(
            context,
            statement,
            matcher.group(2).trim()
        );
        if (
            qubits == null
            || bits == null
        ) {
            return;
        }
        if (qubits.size() != bits.size()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Conditional measurement quantum and classical operands must have the same size.",
                statement
            );
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        for (int i = 0; i < qubits.size(); i++) {
            context.circuit().classicallyControlled(
                predicate,
                new MeasureOperation(
                    qubits.qubit(i),
                    bits.bit(i)
                )
            );
        }
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseControlledBarrier(
        final ParseContext context,
        final Statement statement,
        final ClassicalPredicate predicate,
        final Matcher matcher
    ) {
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            matcher.group(1)
        );
        if (operands == null) {
            return;
        }
        final ArrayList<Qubit> qubits = new ArrayList<>();
        for (int i = 0; i < operands.length; i++) {
            for (int j = 0; j < operands[i].size(); j++) {
                qubits.add(operands[i].qubit(j));
            }
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().classicallyControlled(
            predicate,
            new BarrierOperation(qubits.toArray(new Qubit[0]))
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseControlledGate(
        final ParseContext context,
        final Statement statement,
        final ClassicalPredicate predicate,
        final String nestedStatement
    ) {
        final Matcher matcher = GATE_PATTERN.matcher(nestedStatement);
        if (!matcher.matches()) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 2 import supports conditional gate/measure/reset/barrier operations only.",
                statement
            );
            return;
        }
        if (isOpenQasmOperationKeyword(matcher.group(1))) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 2 import supports conditional gate/measure/reset/barrier operations only.",
                statement
            );
            return;
        }
        final Gate gate = resolveGate(
            context,
            matcher.group(1)
        );
        if (gate == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 2 import does not support conditional gate: " + matcher.group(1) + ".",
                statement
            );
            return;
        }
        final ParameterExpression[] parameters = parseParameters(
            context,
            statement,
            matcher.group(2),
            gate.parameterCount()
        );
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            matcher.group(3)
        );
        if (
            parameters == null
            || operands == null
        ) {
            return;
        }
        if (operands.length != gate.arity()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Conditional gate qubit count does not match supported gate.",
                statement
            );
            return;
        }
        final int expandedSize = operands[0].size();
        for (int i = 1; i < operands.length; i++) {
            if (operands[i].size() != expandedSize) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Register-wide conditional gate operands must have the same size.",
                    statement
                );
                return;
            }
        }
        final int firstOperationIndex = context.circuit().operationCount();
        for (int i = 0; i < expandedSize; i++) {
            final Qubit[] qubits = new Qubit[operands.length];
            for (int j = 0; j < operands.length; j++) {
                qubits[j] = operands[j].qubit(i);
            }
            context.circuit().classicallyControlled(
                predicate,
                GateOperation.parameterized(
                    gate,
                    parameters,
                    qubits
                )
            );
        }
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseGate(
        final ParseContext context,
        final Statement statement
    ) {
        final Matcher matcher = GATE_PATTERN.matcher(statement.text());
        if (!matcher.matches()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse OpenQASM 2 statement: " + statement.text() + ".",
                statement
            );
            return;
        }
        final Gate gate = resolveGate(
            context,
            matcher.group(1)
        );
        if (gate == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 2 import does not support gate: " + matcher.group(1) + ".",
                statement
            );
            return;
        }
        final ParameterExpression[] parameters = parseParameters(
            context,
            statement,
            matcher.group(2),
            gate.parameterCount()
        );
        if (parameters == null) {
            return;
        }
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            matcher.group(3)
        );
        if (operands == null) {
            return;
        }
        appendGateOperations(
            context,
            statement,
            gate,
            parameters,
            operands
        );
    }

    private static boolean isOpenQasmOperationKeyword(final String name) {
        return "measure".equalsIgnoreCase(name)
            || "barrier".equalsIgnoreCase(name)
            || "qreg".equalsIgnoreCase(name)
            || "creg".equalsIgnoreCase(name)
            || "if".equalsIgnoreCase(name)
            || "opaque".equalsIgnoreCase(name)
            || "gate".equalsIgnoreCase(name);
    }

    private static ParameterExpression[] parseParameters(
        final ParseContext context,
        final Statement statement,
        final String parameterText,
        final int expectedCount
    ) {
        if (expectedCount == 0) {
            if (
                parameterText != null
                && !parameterText.isBlank()
            ) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Gate does not accept parameters.",
                    statement
                );
                return null;
            }
            return new ParameterExpression[0];
        }
        if (parameterText == null) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Gate requires parameters.",
                statement
            );
            return null;
        }
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            parameterText,
            "parameter list"
        );
        if (parts == null) {
            return null;
        }
        if (parts.parts().size() != expectedCount) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Gate parameter count does not match supported gate.",
                statement
            );
            return null;
        }
        final ParameterExpression[] parameters = new ParameterExpression[parts.parts().size()];
        for (int i = 0; i < parts.parts().size(); i++) {
            final ParameterExpression parameter = parseParameter(
                context,
                statement,
                parts.parts().get(i).trim()
            );
            if (parameter == null) {
                return null;
            }
            parameters[i] = parameter;
        }
        return parameters;
    }

    private static ParameterExpression parseParameter(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final ParameterParser parser = new ParameterParser(value);
        final ParameterExpression expression = parser.parse();
        if (expression == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "Cannot parse OpenQASM 2 parameter expression: " + value + ".",
                statement
            );
            return null;
        }
        return expression;
    }

    private static ListParts parseCommaAwareParts(
        final ParseContext context,
        final Statement statement,
        final String text,
        final String subject
    ) {
        final ArrayList<String> parts = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            final char current = text.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth < 0) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 2 " + subject + " has an unexpected closing parenthesis.",
                        statement
                    );
                    return null;
                }
            }
            if (
                current == ','
                && depth == 0
            ) {
                parts.add(builder.toString());
                builder.setLength(0);
            } else {
                builder.append(current);
            }
        }
        if (depth != 0) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 2 " + subject + " has an unclosed parenthesis.",
                statement
            );
            return null;
        }
        parts.add(builder.toString());
        return new ListParts(parts);
    }

    private static QuantumOperand[] parseQuantumOperands(
        final ParseContext context,
        final Statement statement,
        final String operandText
    ) {
        final String[] parts = operandText.split(",");
        final QuantumOperand[] operands = new QuantumOperand[parts.length];
        for (int i = 0; i < parts.length; i++) {
            operands[i] = parseQuantumOperand(
                context,
                statement,
                parts[i].trim()
            );
            if (operands[i] == null) {
                return null;
            }
        }
        return operands;
    }

    private static void appendGateOperations(
        final ParseContext context,
        final Statement statement,
        final Gate gate,
        final ParameterExpression[] parameters,
        final QuantumOperand[] operands
    ) {
        if (operands.length != gate.arity()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Gate qubit count does not match supported gate.",
                statement
            );
            return;
        }
        final int expandedSize = operands[0].size();
        for (int i = 1; i < operands.length; i++) {
            if (operands[i].size() != expandedSize) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Register-wide gate operands must have the same size.",
                    statement
                );
                return;
            }
        }
        final int firstOperationIndex = context.circuit().operationCount();
        for (int i = 0; i < expandedSize; i++) {
            final Qubit[] qubits = new Qubit[operands.length];
            for (int j = 0; j < operands.length; j++) {
                qubits[j] = operands[j].qubit(i);
            }
            if (parameters.length == 0) {
                context.circuit().gate(
                    gate,
                    qubits
                );
            } else {
                context.circuit().parameterizedGate(
                    gate,
                    parameters,
                    qubits
                );
            }
        }
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static Gate resolveGate(
        final ParseContext context,
        final String name
    ) {
        final Gate qelibGate = OpenQasm2GateMapper.fromOpenQasmName(name);
        if (qelibGate != null) {
            return qelibGate;
        }
        return context.gateDefinition(name);
    }

    private static QuantumOperand parseQuantumOperand(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(value);
        if (indexedMatcher.matches()) {
            final QuantumRegister register = context.quantumRegister(indexedMatcher.group(1));
            final int index = parseNonNegativeInteger(
                context,
                statement,
                indexedMatcher.group(2)
            );
            if (register == null) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Unknown quantum register: " + indexedMatcher.group(1) + ".",
                    statement
                );
                return null;
            }
            if (index < 0) {
                return null;
            }
            if (index >= register.size()) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Qubit index is outside of quantum register bounds.",
                    statement
                );
                return null;
            }
            return QuantumOperand.single(register.get(index));
        }
        final Matcher registerMatcher = REGISTER_ARGUMENT_PATTERN.matcher(value);
        if (registerMatcher.matches()) {
            final QuantumRegister register = context.quantumRegister(registerMatcher.group(1));
            if (register == null) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Unknown quantum register: " + registerMatcher.group(1) + ".",
                    statement
                );
                return null;
            }
            return QuantumOperand.register(register);
        }
        context.addError(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Invalid quantum argument: " + value + ".",
            statement
        );
        return null;
    }

    private static ClassicalOperand parseClassicalOperand(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(value);
        if (indexedMatcher.matches()) {
            final ClassicalRegister register = context.classicalRegister(indexedMatcher.group(1));
            final int index = parseNonNegativeInteger(
                context,
                statement,
                indexedMatcher.group(2)
            );
            if (register == null) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Unknown classical register: " + indexedMatcher.group(1) + ".",
                    statement
                );
                return null;
            }
            if (index < 0) {
                return null;
            }
            if (index >= register.size()) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Classical bit index is outside of classical register bounds.",
                    statement
                );
                return null;
            }
            return ClassicalOperand.single(register.get(index));
        }
        final Matcher registerMatcher = REGISTER_ARGUMENT_PATTERN.matcher(value);
        if (registerMatcher.matches()) {
            final ClassicalRegister register = context.classicalRegister(registerMatcher.group(1));
            if (register == null) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Unknown classical register: " + registerMatcher.group(1) + ".",
                    statement
                );
                return null;
            }
            return ClassicalOperand.register(register);
        }
        context.addError(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Invalid classical argument: " + value + ".",
            statement
        );
        return null;
    }

    private static int parsePositiveInteger(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final int parsed = parseNonNegativeInteger(
            context,
            statement,
            value
        );
        if (parsed <= 0) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Register size must be positive.",
                statement
            );
            return -1;
        }
        return parsed;
    }

    private static int parseNonNegativeInteger(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final long parsed = parseNonNegativeLong(
            context,
            statement,
            value
        );
        if (parsed < 0) {
            return -1;
        }
        if (parsed > Integer.MAX_VALUE) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Integer index or size is outside Java int range.",
                statement
            );
            return -1;
        }
        return (int) parsed;
    }

    private static long parseNonNegativeLong(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        try {
            final long parsed = Long.parseLong(value);
            if (parsed < 0) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Expected non-negative integer.",
                    statement
                );
                return -1L;
            }
            return parsed;
        } catch (final NumberFormatException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Expected non-negative integer.",
                statement
            );
            return -1L;
        }
    }

    private record Statement(
        String text,
        int line,
        int column
    ) {
    }

    private record ListParts(ArrayList<String> parts) {
    }

    private static final class ParameterParser {

        private final String source;
        private int position;

        private ParameterParser(final String source) {
            this.source = source;
            this.position = 0;
        }

        private ParameterExpression parse() {
            final ParameterExpression expression = parseAdditive();
            skipWhitespace();
            if (position != source.length()) {
                return null;
            }
            return expression;
        }

        private ParameterExpression parseAdditive() {
            ParameterExpression expression = parseMultiplicative();
            while (expression != null) {
                skipWhitespace();
                if (consume('+')) {
                    final ParameterExpression right = parseMultiplicative();
                    if (right == null) {
                        return null;
                    }
                    expression = ParameterExpression.add(
                        expression,
                        right
                    );
                } else if (consume('-')) {
                    final ParameterExpression right = parseMultiplicative();
                    if (right == null) {
                        return null;
                    }
                    expression = ParameterExpression.subtract(
                        expression,
                        right
                    );
                } else {
                    return expression;
                }
            }
            return null;
        }

        private ParameterExpression parseMultiplicative() {
            ParameterExpression expression = parseUnary();
            while (expression != null) {
                skipWhitespace();
                if (consume('*')) {
                    final ParameterExpression right = parseUnary();
                    if (right == null) {
                        return null;
                    }
                    expression = ParameterExpression.multiply(
                        expression,
                        right
                    );
                } else if (consume('/')) {
                    final ParameterExpression right = parseUnary();
                    if (right == null) {
                        return null;
                    }
                    expression = ParameterExpression.divide(
                        expression,
                        right
                    );
                } else {
                    return expression;
                }
            }
            return null;
        }

        private ParameterExpression parseUnary() {
            skipWhitespace();
            if (consume('-')) {
                final ParameterExpression expression = parseUnary();
                if (expression == null) {
                    return null;
                }
                return ParameterExpression.negate(expression);
            }
            return parsePrimary();
        }

        private ParameterExpression parsePrimary() {
            skipWhitespace();
            if (consume('(')) {
                final ParameterExpression expression = parseAdditive();
                skipWhitespace();
                if (!consume(')')) {
                    return null;
                }
                return expression;
            }
            if (position >= source.length()) {
                return null;
            }
            final char current = source.charAt(position);
            if (
                Character.isDigit(current)
                || current == '.'
            ) {
                return parseNumber();
            }
            if (
                Character.isLetter(current)
                || current == '_'
            ) {
                return parseIdentifier();
            }
            return null;
        }

        private ParameterExpression parseNumber() {
            final int start = position;
            while (
                position < source.length()
                && (
                    Character.isDigit(source.charAt(position))
                    || source.charAt(position) == '.'
                    || source.charAt(position) == 'e'
                    || source.charAt(position) == 'E'
                    || source.charAt(position) == '+'
                    || source.charAt(position) == '-'
                )
            ) {
                if (
                    (
                        source.charAt(position) == '+'
                        || source.charAt(position) == '-'
                    )
                    && position > start
                    && source.charAt(position - 1) != 'e'
                    && source.charAt(position - 1) != 'E'
                ) {
                    break;
                }
                position++;
            }
            try {
                return ParameterExpression.of(Double.parseDouble(source.substring(
                    start,
                    position
                )));
            } catch (final NumberFormatException exception) {
                return null;
            }
        }

        private ParameterExpression parseIdentifier() {
            final int start = position;
            while (
                position < source.length()
                && (
                    Character.isLetterOrDigit(source.charAt(position))
                    || source.charAt(position) == '_'
                )
            ) {
                position++;
            }
            final String identifier = source.substring(
                start,
                position
            );
            if ("pi".equals(identifier)) {
                return ParameterExpression.pi();
            }
            if (IDENTIFIER_PATTERN.matcher(identifier).matches()) {
                return ParameterExpression.named(identifier);
            }
            return null;
        }

        private boolean consume(final char expected) {
            skipWhitespace();
            if (
                position < source.length()
                && source.charAt(position) == expected
            ) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (
                position < source.length()
                && Character.isWhitespace(source.charAt(position))
            ) {
                position++;
            }
        }
    }

    private static final class ParseContext {

        private final QuantumProgram program;
        private final QuantumCircuit circuit;
        private final ArrayList<IntegrationDiagnostic> diagnostics;
        private final ExternalSource source;
        private final Map<String, String> includedSources;
        private final HashSet<String> activeIncludes;
        private final LinkedHashMap<String, QuantumRegister> quantumRegisters;
        private final LinkedHashMap<String, ClassicalRegister> classicalRegisters;
        private final LinkedHashMap<String, GateDefinition> gateDefinitions;

        private ParseContext(
            final QuantumProgram program,
            final QuantumCircuit circuit,
            final ArrayList<IntegrationDiagnostic> diagnostics,
            final Map<String, String> includedSources
        ) {
            this.program = program;
            this.circuit = circuit;
            this.diagnostics = diagnostics;
            this.includedSources = includedSources;
            this.activeIncludes = new HashSet<>();
            this.source = new ExternalSource(
                "openqasm2",
                "OpenQASM 2 import"
            );
            this.quantumRegisters = new LinkedHashMap<>();
            this.classicalRegisters = new LinkedHashMap<>();
            this.gateDefinitions = new LinkedHashMap<>();
        }

        private QuantumProgram program() {
            return program;
        }

        private QuantumCircuit circuit() {
            return circuit;
        }

        private void addQuantumRegister(
            final String name,
            final QuantumRegister register
        ) {
            quantumRegisters.put(
                name,
                register
            );
        }

        private void addClassicalRegister(
            final String name,
            final ClassicalRegister register
        ) {
            classicalRegisters.put(
                name,
                register
            );
        }

        private QuantumRegister quantumRegister(final String name) {
            return quantumRegisters.get(name);
        }

        private ClassicalRegister classicalRegister(final String name) {
            return classicalRegisters.get(name);
        }

        private void addGateDefinition(final GateDefinition definition) {
            gateDefinitions.put(
                definition.gateName(),
                definition
            );
        }

        private GateDefinition gateDefinition(final String name) {
            return gateDefinitions.get(name);
        }

        private String includedSource(final String includeName) {
            return includedSources.get(includeName);
        }

        private boolean isIncludeActive(final String includeName) {
            return activeIncludes.contains(includeName);
        }

        private void enterInclude(final String includeName) {
            activeIncludes.add(includeName);
        }

        private void exitInclude(final String includeName) {
            activeIncludes.remove(includeName);
        }

        private void addDiagnostics(final ArrayList<IntegrationDiagnostic> newDiagnostics) {
            diagnostics.addAll(newDiagnostics);
        }

        private void addError(
            final IntegrationDiagnosticCode code,
            final String message,
            final Statement statement
        ) {
            diagnostics.add(IntegrationDiagnostic.error(
                code,
                message,
                statement.line(),
                statement.column()
            ));
        }

        private void attachMetadataFrom(
            final int firstOperationIndex,
            final Statement statement
        ) {
            for (int i = firstOperationIndex; i < circuit.operationCount(); i++) {
                circuit.setOperationMetadata(
                    i,
                    new OperationMetadata(
                        source,
                        new SourceLocation(
                            statement.line(),
                            statement.column()
                        )
                    )
                );
            }
        }

        private boolean hasErrors() {
            for (int i = 0; i < diagnostics.size(); i++) {
                if (diagnostics.get(i).isError()) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class QuantumOperand {

        private final Qubit[] qubits;

        private QuantumOperand(final Qubit[] qubits) {
            this.qubits = qubits;
        }

        private static QuantumOperand single(final Qubit qubit) {
            return new QuantumOperand(new Qubit[] {qubit});
        }

        private static QuantumOperand register(final QuantumRegister register) {
            final Qubit[] qubits = new Qubit[register.size()];
            for (int i = 0; i < register.size(); i++) {
                qubits[i] = register.get(i);
            }
            return new QuantumOperand(qubits);
        }

        private int size() {
            return qubits.length;
        }

        private Qubit qubit(final int index) {
            return qubits[index];
        }
    }

    private static final class ClassicalOperand {

        private final ClassicalBit[] bits;

        private ClassicalOperand(final ClassicalBit[] bits) {
            this.bits = bits;
        }

        private static ClassicalOperand single(final ClassicalBit bit) {
            return new ClassicalOperand(new ClassicalBit[] {bit});
        }

        private static ClassicalOperand register(final ClassicalRegister register) {
            final ClassicalBit[] bits = new ClassicalBit[register.size()];
            for (int i = 0; i < register.size(); i++) {
                bits[i] = register.get(i);
            }
            return new ClassicalOperand(bits);
        }

        private int size() {
            return bits.length;
        }

        private ClassicalBit bit(final int index) {
            return bits[index];
        }
    }
}