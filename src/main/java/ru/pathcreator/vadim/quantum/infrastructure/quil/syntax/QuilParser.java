/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.syntax;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.infrastructure.quil.mapping.QuilGateMapper;

/**
 * Parser Quil в Quantum IR для instruction subset, который имеет честное отображение в gate-based IR.
 */
public final class QuilParser {

    private static final String RAW_MEMORY_REGISTER_BASE_NAME = "ro";
    private static final String CALIBRATION_BODY_LANGUAGE = "quil";

    private static final Pattern DECLARE_PATTERN = Pattern.compile("^DECLARE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\[(\\d+)])?(?:\\s+.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFGATE_PATTERN = Pattern.compile("^DEFGATE\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?:$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEASURE_PATTERN = Pattern.compile("^MEASURE\\s+(\\d+)(?:\\s+(?:([A-Za-z_][A-Za-z0-9_]*)(?:\\[(\\d+)])?|(\\d+)|\\[(\\d+)\\]))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESET_PATTERN = Pattern.compile("^RESET(?:\\s+(\\d+))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABEL_PATTERN = Pattern.compile("^LABEL\\s+@?([A-Za-z_][A-Za-z0-9_]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUMP_PATTERN = Pattern.compile("^JUMP\\s+@?([A-Za-z_][A-Za-z0-9_]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONDITIONAL_JUMP_PATTERN = Pattern.compile("^JUMP-(WHEN|UNLESS)\\s+@?([A-Za-z_][A-Za-z0-9_]*)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALIBRATION_HEADER_PATTERN = Pattern.compile("^(DEFCAL|DEFFRAME|DEFWAVEFORM)\\s+([A-Za-z_][A-Za-z0-9_]*).*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+(.+)$");
    public QuilParserResult parse(final String source) {
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        if (source == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.NULL_INPUT,
                "Quil source must not be null."
            ));
            return new QuilParserResult(
                null,
                diagnostics
            );
        }
        if (source.isBlank()) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.EMPTY_INPUT,
                "Quil source must not be blank."
            ));
            return new QuilParserResult(
                null,
                diagnostics
            );
        }
        final ArrayList<Statement> statements = statements(source);
        final QuantumProgram program = QuantumProgram.gateBased();
        final LinkedHashMap<String, GateDefinition> gateDefinitions = parseGateDefinitions(
            statements,
            program,
            diagnostics
        );
        final ParsePlan plan = buildPlan(
            statements,
            gateDefinitions,
            diagnostics
        );
        if (hasErrors(diagnostics)) {
            return new QuilParserResult(
                null,
                diagnostics
            );
        }
        final QuantumCircuit circuit = program.createCircuit("main");
        circuit.reserveOperationCapacity(statements.size());
        final QuantumRegister qubits = plan.maxQubitIndex() < 0
            ? null
            : circuit.createQuantumRegister(
                "q",
                plan.maxQubitIndex() + 1
            );
        final LinkedHashMap<String, ClassicalRegister> classicalRegisters = new LinkedHashMap<>();
        for (final String name : plan.classicalRegisterSizes().keySet()) {
            classicalRegisters.put(
                name,
                circuit.createClassicalRegister(
                    name,
                    plan.classicalRegisterSizes().get(name).intValue()
                )
            );
        }
        preserveProgramSourceBlocks(
            statements,
            program
        );
        parseOperations(
            statements,
            program,
            circuit,
            qubits,
            classicalRegisters,
            plan.rawMemoryRegisterName(),
            gateDefinitions,
            diagnostics
        );
        if (hasErrors(diagnostics)) {
            return new QuilParserResult(
                null,
                diagnostics
            );
        }
        return new QuilParserResult(
            program,
            diagnostics
        );
    }

    private static ArrayList<Statement> statements(final String source) {
        final ArrayList<Statement> statements = new ArrayList<>();
        final String[] lines = splitLines(source);
        for (int i = 0; i < lines.length; i++) {
            final String text = stripComment(lines[i]).trim();
            if (!text.isBlank()) {
                if (isBlockHeader(text)) {
                    final StringBuilder block = new StringBuilder(rstrip(lines[i]));
                    final int line = i + 1;
                    while (i + 1 < lines.length) {
                        final String nextLine = lines[i + 1];
                        final String nextText = stripComment(nextLine).trim();
                        if (
                            !nextText.isBlank()
                            && !startsWithWhitespace(nextLine)
                        ) {
                            break;
                        }
                        i++;
                        block.append('\n')
                            .append(rstrip(nextLine));
                    }
                    statements.add(new Statement(
                        text,
                        block.toString().stripTrailing(),
                        line,
                        true
                    ));
                    continue;
                }
                statements.add(new Statement(
                    text,
                    text,
                    i + 1,
                    shouldPreserveSourceStatement(text)
                ));
            }
        }
        return statements;
    }

    private static boolean isBlockHeader(final String text) {
        return text.endsWith(":")
            && (
                startsWithIgnoreCase(
                    text,
                    "DEFGATE "
                )
                || startsWithIgnoreCase(
                    text,
                    "DEFCAL "
                )
                || startsWithIgnoreCase(
                    text,
                    "DEFCIRCUIT "
                )
                || startsWithIgnoreCase(
                    text,
                    "DEFFRAME "
                )
                || startsWithIgnoreCase(
                    text,
                    "DEFWAVEFORM "
                )
            );
    }

    private static boolean startsWithWhitespace(final String line) {
        return !line.isEmpty()
            && Character.isWhitespace(line.charAt(0));
    }

    private static boolean startsWithIgnoreCase(
        final String value,
        final String prefix
    ) {
        return value.length() >= prefix.length()
            && value.regionMatches(
                true,
                0,
                prefix,
                0,
                prefix.length()
            );
    }

    private static String rstrip(final String value) {
        int end = value.length();
        while (
            end > 0
            && Character.isWhitespace(value.charAt(end - 1))
        ) {
            end--;
        }
        return value.substring(
            0,
            end
        );
    }

    private static boolean shouldPreserveSourceStatement(final String text) {
        return startsWithIgnoreCase(
            text,
            "PRAGMA "
        )
            || startsWithIgnoreCase(
                text,
                "INCLUDE "
            )
            || startsWithIgnoreCase(
                text,
                "DELAY "
            )
            || startsWithIgnoreCase(
                text,
                "FENCE"
            )
            || startsWithIgnoreCase(
                text,
                "PULSE "
            )
            || startsWithIgnoreCase(
                text,
                "CAPTURE "
            )
            || startsWithIgnoreCase(
                text,
                "RAW-CAPTURE "
            )
            || startsWithIgnoreCase(
                text,
                "SET-"
            )
            || startsWithIgnoreCase(
                text,
                "SHIFT-"
            )
            || startsWithIgnoreCase(
                text,
                "SWAP-PHASES "
            )
            || startsWithIgnoreCase(
                text,
                "NONBLOCKING "
            );
    }

    private static String stripComment(final String line) {
        final int position = line.indexOf('#');
        if (position < 0) {
            return line;
        }
        return line.substring(
            0,
            position
        );
    }

    private static LinkedHashMap<String, GateDefinition> parseGateDefinitions(
        final ArrayList<Statement> statements,
        final QuantumProgram program,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final LinkedHashMap<String, GateDefinition> definitions = new LinkedHashMap<>();
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            final Matcher matcher = DEFGATE_PATTERN.matcher(statement.text());
            if (!matcher.matches()) {
                continue;
            }
            try {
                final GateDefinition definition = GateDefinition.matrix(
                    matcher.group(1),
                    parseDefgateParameterNames(matcher.group(2)),
                    defgateQubitNames(statement),
                    parseDefgateMatrix(statement)
                );
                program.addGateDefinition(definition);
                definitions.put(
                    definition.gateName(),
                    definition
                );
            } catch (final IllegalArgumentException exception) {
                diagnostics.add(error(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    exception.getMessage(),
                    statement
                ));
            }
        }
        return definitions;
    }

    private static List<String> parseDefgateParameterNames(final String text) {
        if (
            text == null
            || text.isBlank()
        ) {
            return List.of();
        }
        final String[] parts = splitComma(text);
        final String[] names = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            final String trimmed = parts[i].trim();
            names[i] = trimmed.startsWith("%")
                ? trimmed.substring(1)
                : trimmed;
        }
        return List.of(names);
    }

    private static List<String> defgateQubitNames(final Statement statement) {
        final int matrixSize = parseDefgateMatrix(statement).rowCount();
        int arity = 0;
        int size = matrixSize;
        while (size > 1) {
            arity++;
            size /= 2;
        }
        final String[] names = new String[arity];
        for (int i = 0; i < names.length; i++) {
            names[i] = "q" + i;
        }
        return List.of(names);
    }

    private static GateMatrix parseDefgateMatrix(final Statement statement) {
        final String[] lines = splitLines(statement.content());
        final ArrayList<String[]> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            final String line = stripComment(lines[i]).trim();
            if (line.isBlank()) {
                continue;
            }
            final String[] parts = splitComma(line);
            for (int j = 0; j < parts.length; j++) {
                parts[j] = parts[j].trim();
            }
            rows.add(parts);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Quil DEFGATE matrix must not be empty.");
        }
        return GateMatrix.of(rows.toArray(new String[0][]));
    }

    private static ParsePlan buildPlan(
        final ArrayList<Statement> statements,
        final LinkedHashMap<String, GateDefinition> gateDefinitions,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        int maxQubitIndex = -1;
        int maxRawMemoryIndex = -1;
        final LinkedHashMap<String, Integer> classicalRegisterSizes = new LinkedHashMap<>();
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            final Matcher declareMatcher = DECLARE_PATTERN.matcher(statement.text());
            final Matcher measureMatcher = MEASURE_PATTERN.matcher(statement.text());
            if (statement.sourceOnly()) {
                continue;
            }
            if (declareMatcher.matches()) {
                if (isBitDeclaration(declareMatcher)) {
                    classicalRegisterSizes.put(
                        declareMatcher.group(1),
                        Integer.valueOf(parseDeclaredBitSize(
                            declareMatcher,
                            statement,
                            diagnostics
                        ))
                    );
                }
            } else if (measureMatcher.matches()) {
                maxQubitIndex = Math.max(
                    maxQubitIndex,
                    parseNonNegativeInt(
                        measureMatcher.group(1),
                        statement,
                        diagnostics
                    )
                );
                if (rawMemoryIndex(measureMatcher) != null) {
                    maxRawMemoryIndex = Math.max(
                        maxRawMemoryIndex,
                        parseNonNegativeInt(
                            rawMemoryIndex(measureMatcher),
                            statement,
                            diagnostics
                        )
                    );
                }
            } else {
                maxQubitIndex = Math.max(
                    maxQubitIndex,
                    maxGateQubitIndex(statement.text())
                );
            }
        }
        final String rawMemoryRegisterName = maxRawMemoryIndex < 0
            ? null
            : rawMemoryRegisterName(classicalRegisterSizes);
        if (rawMemoryRegisterName != null) {
            classicalRegisterSizes.put(
                rawMemoryRegisterName,
                Integer.valueOf(maxRawMemoryIndex + 1)
            );
        }
        return new ParsePlan(
            maxQubitIndex,
            classicalRegisterSizes,
            rawMemoryRegisterName
        );
    }

    private static String rawMemoryRegisterName(final LinkedHashMap<String, Integer> classicalRegisterSizes) {
        if (!classicalRegisterSizes.containsKey(RAW_MEMORY_REGISTER_BASE_NAME)) {
            return RAW_MEMORY_REGISTER_BASE_NAME;
        }
        int suffix = 1;
        while (classicalRegisterSizes.containsKey(RAW_MEMORY_REGISTER_BASE_NAME + "_" + suffix)) {
            suffix++;
        }
        return RAW_MEMORY_REGISTER_BASE_NAME + "_" + suffix;
    }

    private static int maxQubitIndex(final String text) {
        int result = -1;
        int tokenIndex = 0;
        int index = 0;
        while (index < text.length()) {
            while (
                index < text.length()
                && isQuilTokenSeparator(text.charAt(index))
            ) {
                index++;
            }
            final int start = index;
            long parsed = 0L;
            boolean digitsOnly = start < text.length();
            while (
                index < text.length()
                && !isQuilTokenSeparator(text.charAt(index))
            ) {
                final char character = text.charAt(index);
                if (
                    character < '0'
                    || character > '9'
                ) {
                    digitsOnly = false;
                } else if (digitsOnly) {
                    parsed = parsed * 10L + character - '0';
                    if (parsed > Integer.MAX_VALUE) {
                        digitsOnly = false;
                    }
                }
                index++;
            }
            if (
                tokenIndex > 0
                && digitsOnly
                && start < index
            ) {
                result = Math.max(
                    result,
                    (int) parsed
                );
            }
            tokenIndex++;
        }
        return result;
    }

    private static int maxGateQubitIndex(final String text) {
        final Matcher matcher = GATE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return -1;
        }
        return maxQubitIndex(text);
    }

    private static void parseOperations(
        final ArrayList<Statement> statements,
        final QuantumProgram program,
        final QuantumCircuit circuit,
        final QuantumRegister qubits,
        final LinkedHashMap<String, ClassicalRegister> classicalRegisters,
        final String rawMemoryRegisterName,
        final LinkedHashMap<String, GateDefinition> gateDefinitions,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            if (DEFGATE_PATTERN.matcher(statement.text()).matches()) {
                continue;
            }
            if (isProgramSourceBlock(statement)) {
                continue;
            }
            if (isCalibrationSourceBlock(statement)) {
                continue;
            }
            if (statement.sourceOnly()) {
                diagnostics.add(error(
                    IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                    "Quil block is not represented by Quantum IR: " + statement.text() + ".",
                    statement
                ));
                continue;
            }
            final Matcher declareMatcher = DECLARE_PATTERN.matcher(statement.text());
            if (declareMatcher.matches()) {
                if (!isBitDeclaration(declareMatcher)) {
                    parseClassicalArrayDeclaration(
                        statement,
                        circuit,
                        declareMatcher,
                        diagnostics
                    );
                }
                continue;
            }
            final Matcher measureMatcher = MEASURE_PATTERN.matcher(statement.text());
            final Matcher resetMatcher = RESET_PATTERN.matcher(statement.text());
            if (measureMatcher.matches()) {
                parseMeasure(
                    statement,
                    circuit,
                    qubits,
                    classicalRegisters,
                    rawMemoryRegisterName,
                    measureMatcher,
                    diagnostics
                );
            } else if (resetMatcher.matches()) {
                parseReset(
                    statement,
                    circuit,
                    qubits,
                    resetMatcher,
                    diagnostics
                );
            } else if (parseControlFlow(
                statement,
                circuit
            )) {
                continue;
            } else if (parseClassicalInstruction(
                statement,
                circuit,
                diagnostics
            )) {
                continue;
            } else {
                parseGate(
                    statement,
                    program,
                    circuit,
                    qubits,
                    gateDefinitions,
                    diagnostics
                );
            }
        }
    }

    private static void preserveProgramSourceBlocks(
        final ArrayList<Statement> statements,
        final QuantumProgram program
    ) {
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            if (isCalibrationSourceBlock(statement)) {
                program.addCalibrationDefinition(calibrationDefinition(statement));
            } else if (isProgramSourceBlock(statement)) {
                program.addExternalCallableDeclaration(externalCallableDeclaration(statement));
            }
        }
    }

    private static boolean isCalibrationSourceBlock(final Statement statement) {
        if (!statement.sourceOnly()) {
            return false;
        }
        return startsWithIgnoreCase(
            statement.text(),
            "DEFCAL "
        )
            || startsWithIgnoreCase(
                statement.text(),
                "DEFFRAME "
            )
            || startsWithIgnoreCase(
                statement.text(),
                "DEFWAVEFORM "
            );
    }

    private static boolean isProgramSourceBlock(final Statement statement) {
        if (!statement.sourceOnly()) {
            return false;
        }
        return startsWithIgnoreCase(
            statement.text(),
            "DEFCIRCUIT "
        );
    }

    private static void parseMeasure(
        final Statement statement,
        final QuantumCircuit circuit,
        final QuantumRegister qubits,
        final LinkedHashMap<String, ClassicalRegister> classicalRegisters,
        final String rawMemoryRegisterName,
        final Matcher matcher,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final int qubitIndex = parseNonNegativeInt(
            matcher.group(1),
            statement,
            diagnostics
        );
        if (matcher.group(2) == null) {
            if (rawMemoryIndex(matcher) != null) {
                final ClassicalRegister register = classicalRegisters.get(rawMemoryRegisterName);
                final int bitIndex = parseNonNegativeInt(
                    rawMemoryIndex(matcher),
                    statement,
                    diagnostics
                );
                final ru.pathcreator.vadim.quantum.domain.bit.Qubit qubit = qubitAt(
                    qubits,
                    qubitIndex,
                    statement,
                    diagnostics
                );
                if (qubit == null) {
                    return;
                }
                circuit.measure(
                    qubit,
                    register.get(bitIndex)
                );
            }
            return;
        }
        final ClassicalRegister register = classicalRegisters.get(matcher.group(2));
        if (register == null) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Unknown Quil memory region: " + matcher.group(2) + ".",
                statement
            ));
            return;
        }
        final int bitIndex = parseNonNegativeInt(
            matcher.group(3) == null
                ? "0"
                : matcher.group(3),
            statement,
            diagnostics
        );
        if (bitIndex >= register.size()) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Quil memory reference is outside declared region.",
                statement
            ));
            return;
        }
        final ru.pathcreator.vadim.quantum.domain.bit.Qubit qubit = qubitAt(
            qubits,
            qubitIndex,
            statement,
            diagnostics
        );
        if (qubit == null) {
            return;
        }
        circuit.measure(
            qubit,
            register.get(bitIndex)
        );
    }

    private static String rawMemoryIndex(final Matcher matcher) {
        if (matcher.group(4) != null) {
            return matcher.group(4);
        }
        return matcher.group(5);
    }

    private static void parseReset(
        final Statement statement,
        final QuantumCircuit circuit,
        final QuantumRegister qubits,
        final Matcher matcher,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (matcher.group(1) == null) {
            for (int i = 0; i < qubits.size(); i++) {
                circuit.reset(qubits.get(i));
            }
            return;
        }
        final ru.pathcreator.vadim.quantum.domain.bit.Qubit qubit = qubitAt(
            qubits,
            parseNonNegativeInt(
                matcher.group(1),
                statement,
                diagnostics
            ),
            statement,
            diagnostics
        );
        if (qubit == null) {
            return;
        }
        circuit.reset(qubit);
    }

    private static void parseClassicalArrayDeclaration(
        final Statement statement,
        final QuantumCircuit circuit,
        final Matcher matcher,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final ClassicalType type = classicalType(
            matcher.group(2),
            statement,
            diagnostics
        );
        final ArrayList<ClassicalExpression> dimensions = new ArrayList<>();
        dimensions.add(ClassicalExpression.integer(
            matcher.group(3) == null
                ? 1L
                : parsePositiveInt(
                    matcher.group(3),
                    statement,
                    diagnostics
                )
        ));
        circuit.classicalArrayDeclaration(new ClassicalArrayDeclarationOperation(
            matcher.group(1),
            type,
            dimensions,
            null
        ));
    }

    private static ClassicalType classicalType(
        final String quilType,
        final Statement statement,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if ("REAL".equalsIgnoreCase(quilType)) {
            return ClassicalType.sized(
                ClassicalTypeKind.FLOAT,
                64
            );
        }
        if ("INTEGER".equalsIgnoreCase(quilType)) {
            return ClassicalType.sized(
                ClassicalTypeKind.SIGNED_INTEGER,
                64
            );
        }
        if ("OCTET".equalsIgnoreCase(quilType)) {
            return ClassicalType.sized(
                ClassicalTypeKind.UNSIGNED_INTEGER,
                8
            );
        }
        diagnostics.add(error(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Unsupported Quil memory type: " + quilType + ".",
            statement
        ));
        return ClassicalType.sized(
            ClassicalTypeKind.UNSIGNED_INTEGER,
            1
        );
    }

    private static boolean parseControlFlow(
        final Statement statement,
        final QuantumCircuit circuit
    ) {
        final Matcher labelMatcher = LABEL_PATTERN.matcher(statement.text());
        if (labelMatcher.matches()) {
            circuit.label(labelMatcher.group(1));
            return true;
        }
        final Matcher jumpMatcher = JUMP_PATTERN.matcher(statement.text());
        if (jumpMatcher.matches()) {
            circuit.branch(BranchOperation.always(jumpMatcher.group(1)));
            return true;
        }
        final Matcher conditionalMatcher = CONDITIONAL_JUMP_PATTERN.matcher(statement.text());
        if (conditionalMatcher.matches()) {
            final ClassicalExpression condition = classicalOperand(conditionalMatcher.group(3));
            if ("WHEN".equalsIgnoreCase(conditionalMatcher.group(1))) {
                circuit.branch(BranchOperation.whenTrue(
                    conditionalMatcher.group(2),
                    condition
                ));
            } else {
                circuit.branch(BranchOperation.whenFalse(
                    conditionalMatcher.group(2),
                    condition
                ));
            }
            return true;
        }
        if ("HALT".equalsIgnoreCase(statement.text())) {
            circuit.halt();
            return true;
        }
        if ("WAIT".equalsIgnoreCase(statement.text())) {
            circuit.waitInstruction();
            return true;
        }
        return "NOP".equalsIgnoreCase(statement.text());
    }

    private static boolean parseClassicalInstruction(
        final Statement statement,
        final QuantumCircuit circuit,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final String[] parts = splitWhitespace(statement.text());
        if (parts.length == 0) {
            return false;
        }
        final String instruction = parts[0];
        if ("MOVE".equalsIgnoreCase(instruction) && parts.length == 3) {
            assign(
                circuit,
                parts[1],
                classicalOperand(parts[2])
            );
            return true;
        }
        if ("EXCHANGE".equalsIgnoreCase(instruction) && parts.length == 3) {
            assign(
                circuit,
                parts[1],
                ClassicalExpression.call(
                    "exchange",
                    List.of(
                        classicalOperand(parts[1]),
                        classicalOperand(parts[2])
                    )
                )
            );
            assign(
                circuit,
                parts[2],
                ClassicalExpression.call(
                    "exchange",
                    List.of(
                        classicalOperand(parts[2]),
                        classicalOperand(parts[1])
                    )
                )
            );
            return true;
        }
        if ("NEG".equalsIgnoreCase(instruction) && parts.length == 3) {
            assignCall(
                circuit,
                "neg",
                parts[1],
                parts[2]
            );
            return true;
        }
        if ("NOT".equalsIgnoreCase(instruction) && parts.length == 3) {
            assignCall(
                circuit,
                "not",
                parts[1],
                parts[2]
            );
            return true;
        }
        if (parts.length == 4) {
            final ClassicalBinaryOperator operator = binaryOperator(instruction);
            if (operator != null) {
                assign(
                    circuit,
                    parts[1],
                    ClassicalExpression.binary(
                        operator,
                        classicalOperand(parts[2]),
                        classicalOperand(parts[3])
                    )
                );
                return true;
            }
            if (isClassicalCallInstruction(instruction)) {
                assign(
                    circuit,
                    parts[1],
                    ClassicalExpression.call(
                        classicalCallName(instruction),
                        List.of(
                            classicalOperand(parts[2]),
                            classicalOperand(parts[3])
                        )
                    )
                );
                return true;
            }
        }
        if (
            (
                "LOAD".equalsIgnoreCase(instruction)
                || "STORE".equalsIgnoreCase(instruction)
                || "CONVERT".equalsIgnoreCase(instruction)
            )
            && parts.length >= 3
        ) {
            final ArrayList<ClassicalExpression> arguments = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                arguments.add(classicalOperand(parts[i]));
            }
            assign(
                circuit,
                parts[1],
                ClassicalExpression.call(
                    classicalCallName(instruction),
                    arguments
                )
            );
            return true;
        }
        if (isClassicalVmInstruction(instruction)) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Malformed Quil classical instruction: " + statement.text() + ".",
                statement
            ));
            return true;
        }
        return false;
    }

    private static boolean isClassicalVmInstruction(final String instruction) {
        return "MOVE".equalsIgnoreCase(instruction)
            || "EXCHANGE".equalsIgnoreCase(instruction)
            || "CONVERT".equalsIgnoreCase(instruction)
            || "LOAD".equalsIgnoreCase(instruction)
            || "STORE".equalsIgnoreCase(instruction)
            || "ADD".equalsIgnoreCase(instruction)
            || "SUB".equalsIgnoreCase(instruction)
            || "MUL".equalsIgnoreCase(instruction)
            || "DIV".equalsIgnoreCase(instruction)
            || "NEG".equalsIgnoreCase(instruction)
            || "EQ".equalsIgnoreCase(instruction)
            || "GT".equalsIgnoreCase(instruction)
            || "GE".equalsIgnoreCase(instruction)
            || "LT".equalsIgnoreCase(instruction)
            || "LE".equalsIgnoreCase(instruction)
            || "AND".equalsIgnoreCase(instruction)
            || "IOR".equalsIgnoreCase(instruction)
            || "XOR".equalsIgnoreCase(instruction)
            || "NOT".equalsIgnoreCase(instruction);
    }

    private static boolean isClassicalCallInstruction(final String instruction) {
        return "EQ".equalsIgnoreCase(instruction)
            || "GT".equalsIgnoreCase(instruction)
            || "GE".equalsIgnoreCase(instruction)
            || "LT".equalsIgnoreCase(instruction)
            || "LE".equalsIgnoreCase(instruction);
    }

    private static ClassicalBinaryOperator binaryOperator(final String instruction) {
        if ("ADD".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.ADD;
        }
        if ("SUB".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.SUBTRACT;
        }
        if ("MUL".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.MULTIPLY;
        }
        if ("DIV".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.DIVIDE;
        }
        if ("AND".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.BITWISE_AND;
        }
        if ("IOR".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.BITWISE_OR;
        }
        if ("XOR".equalsIgnoreCase(instruction)) {
            return ClassicalBinaryOperator.BITWISE_XOR;
        }
        return null;
    }

    private static String classicalCallName(final String instruction) {
        if ("EQ".equalsIgnoreCase(instruction)) {
            return "eq";
        }
        if ("GT".equalsIgnoreCase(instruction)) {
            return "gt";
        }
        if ("GE".equalsIgnoreCase(instruction)) {
            return "ge";
        }
        if ("LT".equalsIgnoreCase(instruction)) {
            return "lt";
        }
        if ("LE".equalsIgnoreCase(instruction)) {
            return "le";
        }
        if ("LOAD".equalsIgnoreCase(instruction)) {
            return "load";
        }
        if ("STORE".equalsIgnoreCase(instruction)) {
            return "store";
        }
        if ("CONVERT".equalsIgnoreCase(instruction)) {
            return "convert";
        }
        return instruction;
    }

    private static void assignCall(
        final QuantumCircuit circuit,
        final String callName,
        final String target,
        final String value
    ) {
        assign(
            circuit,
            target,
            ClassicalExpression.call(
                callName,
                List.of(classicalOperand(value))
            )
        );
    }

    private static void assign(
        final QuantumCircuit circuit,
        final String target,
        final ClassicalExpression value
    ) {
        circuit.assign(new ClassicalAssignment(
            classicalOperand(target),
            value
        ));
    }

    private static ClassicalExpression classicalOperand(final String token) {
        final String trimmed = token.trim();
        if (trimmed.matches("-?\\d+")) {
            return ClassicalExpression.integer(Long.parseLong(trimmed));
        }
        return ClassicalExpression.symbolicReference(trimmed);
    }

    private static void parseGate(
        final Statement statement,
        final QuantumProgram program,
        final QuantumCircuit circuit,
        final QuantumRegister qubits,
        final LinkedHashMap<String, GateDefinition> gateDefinitions,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final Matcher matcher = GATE_PATTERN.matcher(statement.text());
        if (!matcher.matches()) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse Quil instruction: " + statement.text() + ".",
                statement
            ));
            return;
        }
        Gate gate = QuilGateMapper.fromQuilName(matcher.group(1));
        if (gate == null) {
            gate = gateDefinitions.get(matcher.group(1));
        }
        final String[] qubitTokens = splitWhitespaceWithComma(matcher.group(3));
        if (gate == null) {
            gate = createExternalGateDefinition(
                program,
                gateDefinitions,
                matcher.group(1),
                qubitTokens.length,
                parameterCount(matcher.group(2))
            );
        }
        if (qubitTokens.length != gate.arity()) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Quil gate arity mismatch for " + matcher.group(1) + ".",
                statement
            ));
            return;
        }
        final ParameterExpression[] parameters = parseParameters(
            matcher.group(2),
            gate.parameterCount(),
            statement
        );
        if (parameters == null) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse Quil gate parameters: " + statement.text() + ".",
                statement
            ));
            return;
        }
        final ru.pathcreator.vadim.quantum.domain.bit.Qubit[] operationQubits = new ru.pathcreator.vadim.quantum.domain.bit.Qubit[qubitTokens.length];
        for (int i = 0; i < qubitTokens.length; i++) {
            operationQubits[i] = qubitAt(
                qubits,
                parseNonNegativeInt(
                    qubitTokens[i],
                    statement,
                    diagnostics
                ),
                statement,
                diagnostics
            );
            if (operationQubits[i] == null) {
                return;
            }
        }
        circuit.parameterizedGate(
            gate,
            parameters,
            operationQubits
        );
    }

    private static GateDefinition createExternalGateDefinition(
        final QuantumProgram program,
        final LinkedHashMap<String, GateDefinition> gateDefinitions,
        final String name,
        final int arity,
        final int parameterCount
    ) {
        final String[] parameterNames = new String[parameterCount];
        for (int i = 0; i < parameterNames.length; i++) {
            parameterNames[i] = "p" + i;
        }
        final String[] qubitNames = new String[arity];
        for (int i = 0; i < qubitNames.length; i++) {
            qubitNames[i] = "q" + i;
        }
        final GateDefinition definition = GateDefinition.opaque(
            name,
            List.of(parameterNames),
            List.of(qubitNames)
        );
        program.addGateDefinition(definition);
        gateDefinitions.put(
            definition.gateName(),
            definition
        );
        return definition;
    }

    private static ru.pathcreator.vadim.quantum.domain.bit.Qubit qubitAt(
        final QuantumRegister qubits,
        final int index,
        final Statement statement,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (qubits == null) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Quil operation references a qubit, but no quantum register was planned.",
                statement
            ));
            return null;
        }
        if (
            index < 0
            || index >= qubits.size()
        ) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Quil qubit reference is outside planned quantum register.",
                statement
            ));
            return null;
        }
        return qubits.get(index);
    }

    private static int parameterCount(final String text) {
        if (
            text == null
            || text.isBlank()
        ) {
            return 0;
        }
        return commaSeparatedPartCount(text);
    }

    private static ParameterExpression[] parseParameters(
        final String text,
        final int expectedCount,
        final Statement statement
    ) {
        if (expectedCount == 0) {
            if (text != null) {
                return null;
            }
            return new ParameterExpression[0];
        }
        if (text == null) {
            return null;
        }
        final String[] parts = splitComma(text);
        if (parts.length != expectedCount) {
            return null;
        }
        final ParameterExpression[] parameters = new ParameterExpression[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                parameters[i] = QuilParameterParser.parse(parts[i]);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
        return parameters;
    }

    private static boolean isBitDeclaration(final Matcher matcher) {
        return "BIT".equalsIgnoreCase(matcher.group(2));
    }

    private static int parseDeclaredBitSize(
        final Matcher matcher,
        final Statement statement,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        if (matcher.group(3) == null) {
            return 1;
        }
        return parsePositiveInt(
            matcher.group(3),
            statement,
            diagnostics
        );
    }

    private static CalibrationDefinition calibrationDefinition(final Statement statement) {
        return new CalibrationDefinition(
            calibrationTargetName(statement),
            List.of(),
            List.of(),
            CALIBRATION_BODY_LANGUAGE,
            statement.content()
        );
    }

    private static String calibrationTargetName(final Statement statement) {
        final Matcher matcher = CALIBRATION_HEADER_PATTERN.matcher(statement.text());
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return "quil_calibration_line_" + statement.line();
    }

    private static ExternalCallableDeclaration externalCallableDeclaration(
        final Statement statement
    ) {
        return new ExternalCallableDeclaration(
            externalCallableName(statement),
            null
        );
    }

    private static String externalCallableName(final Statement statement) {
        final String header = statement.text();
        final int spaceIndex = header.indexOf(' ');
        final int colonIndex = header.indexOf(':');
        if (
            spaceIndex >= 0
            && colonIndex > spaceIndex
        ) {
            return header.substring(
                spaceIndex + 1,
                colonIndex
            ).trim();
        }
        return "quil_defcircuit_line_" + statement.line();
    }

    private static int parsePositiveInt(
        final String value,
        final Statement statement,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        final int parsed = parseNonNegativeInt(
            value,
            statement,
            diagnostics
        );
        if (parsed <= 0) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Quil size must be positive.",
                statement
            ));
        }
        return parsed;
    }

    private static int parseNonNegativeInt(
        final String value,
        final Statement statement,
        final ArrayList<IntegrationDiagnostic> diagnostics
    ) {
        try {
            final long parsed = Long.parseLong(value);
            if (
                parsed < 0L
                || parsed > Integer.MAX_VALUE
            ) {
                diagnostics.add(error(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Quil integer is outside non-negative Java int range.",
                    statement
                ));
                return 0;
            }
            return (int) parsed;
        } catch (final NumberFormatException exception) {
            diagnostics.add(error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Expected Quil integer.",
                statement
            ));
            return 0;
        }
    }

    private static String[] splitLines(final String text) {
        final int lineCount = lineCount(text);
        final String[] lines = new String[lineCount];
        int start = 0;
        int line = 0;
        for (int i = 0; i < text.length(); i++) {
            final char character = text.charAt(i);
            if (
                character == '\n'
                || character == '\r'
            ) {
                lines[line] = text.substring(
                    start,
                    i
                );
                line++;
                if (
                    character == '\r'
                    && i + 1 < text.length()
                    && text.charAt(i + 1) == '\n'
                ) {
                    i++;
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines[line] = text.substring(start);
        }
        return lines;
    }

    private static String[] splitComma(final String text) {
        final int partCount = commaSeparatedPartCount(text);
        final String[] parts = new String[partCount];
        if (partCount == 0) {
            return parts;
        }
        int start = 0;
        int part = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ',') {
                if (part == partCount - 1) {
                    parts[part] = text.substring(
                        start,
                        i
                    );
                    return parts;
                }
                parts[part] = text.substring(
                    start,
                    i
                );
                part++;
                start = i + 1;
            }
        }
        parts[part] = text.substring(start);
        return parts;
    }

    private static String[] splitWhitespace(final String text) {
        final String[] parts = new String[separatorTokenCount(
            text,
            false
        )];
        int index = 0;
        int part = 0;
        while (index < text.length()) {
            while (
                index < text.length()
                && Character.isWhitespace(text.charAt(index))
            ) {
                index++;
            }
            final int start = index;
            while (
                index < text.length()
                && !Character.isWhitespace(text.charAt(index))
            ) {
                index++;
            }
            if (start < index) {
                parts[part] = text.substring(
                    start,
                    index
                );
                part++;
            }
        }
        return parts;
    }

    private static String[] splitWhitespaceWithComma(final String text) {
        final String[] parts = new String[separatorTokenCount(
            text,
            true
        )];
        int index = 0;
        int part = 0;
        while (index < text.length()) {
            while (
                index < text.length()
                && isQuilTokenSeparator(text.charAt(index))
            ) {
                index++;
            }
            final int start = index;
            while (
                index < text.length()
                && !isQuilTokenSeparator(text.charAt(index))
            ) {
                index++;
            }
            if (start < index) {
                parts[part] = text.substring(
                    start,
                    index
                );
                part++;
            }
        }
        return parts;
    }

    private static int lineCount(final String text) {
        int count = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            final char character = text.charAt(i);
            if (
                character == '\n'
                || character == '\r'
            ) {
                count++;
                if (
                    character == '\r'
                    && i + 1 < text.length()
                    && text.charAt(i + 1) == '\n'
                ) {
                    i++;
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            count++;
        }
        return count;
    }

    private static int separatorTokenCount(
        final String text,
        final boolean commaIsSeparator
    ) {
        int count = 0;
        int index = 0;
        while (index < text.length()) {
            while (
                index < text.length()
                && isTokenSeparator(
                    text.charAt(index),
                    commaIsSeparator
                )
            ) {
                index++;
            }
            final int start = index;
            while (
                index < text.length()
                && !isTokenSeparator(
                    text.charAt(index),
                    commaIsSeparator
                )
            ) {
                index++;
            }
            if (start < index) {
                count++;
            }
        }
        return count;
    }

    private static boolean isTokenSeparator(
        final char character,
        final boolean commaIsSeparator
    ) {
        return Character.isWhitespace(character)
            || (
                commaIsSeparator
                && character == ','
            );
    }

    private static boolean isQuilTokenSeparator(final char character) {
        return character == ','
            || Character.isWhitespace(character);
    }

    private static int commaSeparatedPartCount(final String text) {
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ',') {
                count++;
            }
        }
        int lastIndex = text.length() - 1;
        while (
            count > 0
            && lastIndex >= 0
            && text.charAt(lastIndex) == ','
        ) {
            count--;
            lastIndex--;
        }
        return count;
    }

    private static IntegrationDiagnostic error(
        final IntegrationDiagnosticCode code,
        final String message,
        final Statement statement
    ) {
        return IntegrationDiagnostic.error(
            code,
            message,
            statement.line(),
            1
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

    private record Statement(
        String text,
        String content,
        int line,
        boolean sourceOnly
    ) {
    }

    private record ParsePlan(
        int maxQubitIndex,
        LinkedHashMap<String, Integer> classicalRegisterSizes,
        String rawMemoryRegisterName
    ) {
    }
}