/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping.OpenQasm3GateNames;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.mapping.OpenQasm3GateMapper;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax.ast.OpenQasm3Ast;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax.ast.OpenQasm3AstParser;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax.ast.OpenQasm3AstStatement;

/**
 * Parser поддерживаемого подмножества OpenQASM 3.0 в Quantum IR.
 */
public final class OpenQasm3Parser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^OPENQASM\\s+3(?:\\.0)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^include\\s+\"([^\"]+)\"$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QREG_PATTERN = Pattern.compile("^qubit(?:\\[(.+)])?\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s*=\\s*.+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREG_PATTERN = Pattern.compile("^bit(?:\\[(.+)])?\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s*=\\s*.+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREG_BITSTRING_PATTERN = Pattern.compile("^bit(?:\\[(.+)])?\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(\"[01]+\")$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASSICAL_DECLARATION_PATTERN = Pattern.compile("^(const\\s+)?(int|uint|float|angle|bool|duration|stretch)(?:\\[(.+?)])?\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s*=\\s*(.+))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern MEASURE_PATTERN = Pattern.compile("^measure\\s+(.+)\\s*->\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEASURE_ASSIGNMENT_PATTERN = Pattern.compile("^(.+)\\s*=\\s*measure\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESET_PATTERN = Pattern.compile("^reset\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARRIER_PATTERN = Pattern.compile("^barrier\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*==\\s*(\\d+)\\s*\\)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_BLOCK_PATTERN = Pattern.compile("^if\\s*\\((.+?)\\)\\s*\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FOR_PATTERN = Pattern.compile("^for\\s+(?:(?:int|uint)(?:\\[[^]]+])?\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s+in\\s*\\[\\s*([^:\\]]+)\\s*:\\s*(?:([^:\\]]+)\\s*:)?\\s*([^\\]]+)\\s*]\\s*\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WHILE_PATTERN = Pattern.compile("^while\\s*\\((.+?)\\)\\s*\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DELAY_PATTERN = Pattern.compile("^delay\\s*\\[\\s*(.+)\\s*](?:\\s+(.+))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOX_PATTERN = Pattern.compile("^box(?:\\s*\\[\\s*(.+)\\s*])?\\s*\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LET_PATTERN = Pattern.compile("^let\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ARRAY_DECLARATION_PATTERN = Pattern.compile("^array\\s*\\[\\s*(?:int|uint)(?:\\[[^]]+])?\\s*,\\s*(.+?)]\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s*=\\s*\\{(.*)})?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EXTERN_PATTERN = Pattern.compile("^extern\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*?)\\)\\s*(?:->\\s*(.+))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SUBROUTINE_DEFINITION_PATTERN = Pattern.compile("^def\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*?)\\)\\s*(?:->\\s*([^{}]+?))?\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SUBROUTINE_MIXED_CALL_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*?)\\)\\s+(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SUBROUTINE_CALL_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*(?:\\((.*)\\)|(.*))$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RETURN_PATTERN = Pattern.compile("^return\\s+(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern INTEGER_COMPOUND_ASSIGNMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*([+\\-*/^]|<<|>>)=\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CLASSICAL_ASSIGNMENT_PATTERN = Pattern.compile("^(.+?)\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CLASSICAL_COMPARISON_PATTERN = Pattern.compile("^(.+?)\\s*(==|!=|<=|>=|<|>)\\s*(.+)$");
    private static final Pattern CLASSICAL_CAST_PATTERN = Pattern.compile("^(?:int|uint|bit|bool)(?:\\[[^]]+])?\\((.+)\\)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BOOLEAN_CAST_PATTERN = Pattern.compile("^bool\\((.+)\\)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DURATION_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(dt|ns|us|ms|s)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFCAL_PATTERN = Pattern.compile("^defcal\\s+([A-Za-z_][A-Za-z0-9_]*).*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OPAQUE_PATTERN = Pattern.compile("^opaque\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_DEFINITION_PATTERN = Pattern.compile("^gate\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+([^{}]+)\\s*\\{(.*)}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern GATE_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)(?:\\((.*)\\))?\\s+(.+)$");
    private static final Pattern INDEXED_ARGUMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\[(.+)]$");
    private static final Pattern BRACED_INDEXED_ARGUMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\[\\{(.+)}]$");
    private static final Pattern SLICED_ARGUMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\[(.+)\\s*:\\s*(.+)]$");
    private static final Pattern REGISTER_ARGUMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern PHYSICAL_QUBIT_PATTERN = Pattern.compile("^\\$(\\d+)$");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final String CALIBRATION_BODY_LANGUAGE = "openqasm3";

    private final OpenQasm3AstParser astParser;

    public OpenQasm3Parser() {
        this.astParser = new OpenQasm3AstParser();
    }

    /**
     * Разбирает OpenQASM 3.0 text в Quantum IR.
     *
     * @param source OpenQASM 3.0 text
     * @return результат parser
     */
    public OpenQasm3ParserResult parse(final String source) {
        return parse(
            source,
            Map.of()
        );
    }

    /**
     * Разбирает OpenQASM 3.0 text с доступными include sources.
     *
     * @param source OpenQASM 3.0 text
     * @param includedSources тексты include-файлов по имени include
     * @return результат parser
     */
    public OpenQasm3ParserResult parse(
        final String source,
        final Map<String, String> includedSources
    ) {
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        if (source == null) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.NULL_INPUT,
                "OpenQASM 3 source must not be null."
            ));
            return new OpenQasm3ParserResult(
                null,
                diagnostics
            );
        }
        if (source.isBlank()) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.EMPTY_INPUT,
                "OpenQASM 3 source must not be blank."
            ));
            return new OpenQasm3ParserResult(
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
        final OpenQasm3Ast ast = astParser.parse(normalizeSourceSymbols(source));
        diagnostics.addAll(ast.diagnostics());
        if (hasErrors(diagnostics)) {
            return new OpenQasm3ParserResult(
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
            } else {
                parseStatement(
                    context,
                    statement
                );
            }
        }

        if (context.hasErrors()) {
            return new OpenQasm3ParserResult(
                null,
                diagnostics
            );
        }
        return new OpenQasm3ParserResult(
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

    private static String normalizeSourceSymbols(final String source) {
        return source
            .replace("θ", "theta")
            .replace("Θ", "theta")
            .replace("ϑ", "theta")
            .replace("π", "pi")
            .replace("Π", "pi")
            .replace("𝜋", "pi")
            .replace("φ", "phi")
            .replace("Φ", "phi")
            .replace("λ", "lambda")
            .replace("Λ", "lambda")
            .replace("Оё", "theta")
            .replace("ПЂ", "pi")
            .replace("РћС‘", "theta")
            .replace("РџР‚", "pi");
    }

    private static ArrayList<Statement> statementsFromAst(final OpenQasm3Ast ast) {
        final ArrayList<Statement> statements = new ArrayList<>();
        for (int i = 0; i < ast.statements().size(); i++) {
            final OpenQasm3AstStatement statement = ast.statements().get(i);
            statements.add(new Statement(
                statement.text(),
                statement.line(),
                statement.column()
            ));
        }
        return statements;
    }

    private static IfInlineParts parseInlineIf(final String text) {
        final String trimmed = text.trim();
        if (!trimmed.regionMatches(
            true,
            0,
            "if",
            0,
            2
        )) {
            return null;
        }
        int position = 2;
        while (
            position < trimmed.length()
            && Character.isWhitespace(trimmed.charAt(position))
        ) {
            position++;
        }
        if (
            position >= trimmed.length()
            || trimmed.charAt(position) != '('
        ) {
            return null;
        }
        int depth = 0;
        for (int i = position; i < trimmed.length(); i++) {
            final char current = trimmed.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    final String condition = trimmed.substring(
                        position + 1,
                        i
                    ).trim();
                    final String body = trimmed.substring(i + 1).trim();
                    if (
                        body.isBlank()
                        || body.startsWith("{")
                    ) {
                        return null;
                    }
                    return new IfInlineParts(
                        condition,
                        body
                    );
                }
            }
        }
        return null;
    }

    private static void parseStatement(
        final ParseContext context,
        final Statement statement
    ) {
        final Matcher includeMatcher = INCLUDE_PATTERN.matcher(statement.text());
        final Matcher qregMatcher = QREG_PATTERN.matcher(statement.text());
        final Matcher cregMatcher = CREG_PATTERN.matcher(statement.text());
        final Matcher cregBitstringMatcher = CREG_BITSTRING_PATTERN.matcher(statement.text());
        final Matcher classicalDeclarationMatcher = CLASSICAL_DECLARATION_PATTERN.matcher(statement.text());
        final Matcher measureMatcher = MEASURE_PATTERN.matcher(statement.text());
        final Matcher measureAssignmentMatcher = MEASURE_ASSIGNMENT_PATTERN.matcher(statement.text());
        final Matcher resetMatcher = RESET_PATTERN.matcher(statement.text());
        final Matcher barrierMatcher = BARRIER_PATTERN.matcher(statement.text());
        final Matcher ifMatcher = IF_PATTERN.matcher(statement.text());
        final Matcher ifBlockMatcher = IF_BLOCK_PATTERN.matcher(statement.text());
        final IfInlineParts ifInlineParts = parseInlineIf(statement.text());
        final Matcher forMatcher = FOR_PATTERN.matcher(statement.text());
        final Matcher whileMatcher = WHILE_PATTERN.matcher(statement.text());
        final Matcher delayMatcher = DELAY_PATTERN.matcher(statement.text());
        final Matcher boxMatcher = BOX_PATTERN.matcher(statement.text());
        final Matcher letMatcher = LET_PATTERN.matcher(statement.text());
        final Matcher arrayDeclarationMatcher = ARRAY_DECLARATION_PATTERN.matcher(statement.text());
        final Matcher externMatcher = EXTERN_PATTERN.matcher(statement.text());
        final Matcher subroutineDefinitionMatcher = SUBROUTINE_DEFINITION_PATTERN.matcher(statement.text());
        final Matcher classicalAssignmentMatcher = CLASSICAL_ASSIGNMENT_PATTERN.matcher(statement.text());
        final Matcher opaqueMatcher = OPAQUE_PATTERN.matcher(statement.text());
        final Matcher gateDefinitionMatcher = GATE_DEFINITION_PATTERN.matcher(statement.text());

        if (includeMatcher.matches()) {
            parseInclude(
                context,
                statement,
                includeMatcher.group(1)
            );
        } else if (isOpenQasm2RegisterSyntax(statement.text())) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 import does not support OpenQASM 2 register syntax: " + statement.text() + ".",
                statement
            );
        } else if (opaqueMatcher.matches()) {
            parseOpaqueGateDefinition(
                context,
                statement,
                opaqueMatcher
            );
        } else if (isCalibrationSourceStatement(statement.text())) {
            parseCalibrationDefinition(
                context,
                statement
            );
        } else if (subroutineDefinitionMatcher.matches()) {
            parseSubroutineDefinition(
                context,
                statement,
                subroutineDefinitionMatcher
            );
        } else if (externMatcher.matches()) {
            parseExternDeclaration(
                context,
                statement,
                externMatcher
            );
        } else if (arrayDeclarationMatcher.matches()) {
            parseStaticArrayDeclaration(
                context,
                statement,
                arrayDeclarationMatcher
            );
        } else if (isArrayDeclaration(statement.text())) {
            parseGenericArrayDeclaration(
                context,
                statement
            );
        } else if (letMatcher.matches()) {
            parseCompileTimeLet(
                context,
                statement,
                letMatcher
            );
        } else if (classicalDeclarationMatcher.matches()) {
            parseClassicalDeclaration(
                context,
                statement,
                classicalDeclarationMatcher
            );
        } else if (cregBitstringMatcher.matches()) {
            parseClassicalRegisterWithBitstring(
                context,
                statement,
                cregBitstringMatcher
            );
        } else if (
            classicalAssignmentMatcher.matches()
            && topLevelAssignmentIndex(statement.text()) >= 0
            && !isMeasureAssignment(statement.text())
            && tryParseReturningSubroutineAssignment(
                context,
                statement,
                classicalAssignmentMatcher
            )
        ) {
            return;
        } else if (tryParseCompileTimeIntegerCompoundAssignment(
            context,
            statement
        )) {
            return;
        } else if (tryParseRuntimeIntegerCompoundAssignment(
            context,
            statement
        )) {
            return;
        } else if (tryParseSubroutineCall(
            context,
            statement
        )) {
            return;
        } else if (tryParseGenericCallableInvocation(
            context,
            statement
        )) {
            return;
        } else if (shouldPreserveSourceOperation(statement.text())) {
            rejectUnsupportedSource(
                context,
                statement,
                unsupportedStatementKind(statement.text())
            );
        } else if (shouldPreserveSourceStatement(statement.text())) {
            rejectUnsupportedSource(
                context,
                statement
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
        } else if (
            ifBlockMatcher.matches()
            && shouldPreservePredicate(
                context,
                statement,
                ifBlockMatcher.group(1)
            )
        ) {
            rejectUnsupportedSource(
                context,
                statement,
                unsupportedStatementKind(statement.text())
            );
        } else if (ifBlockMatcher.matches()) {
            parseConditionalBlock(
                context,
                statement,
                ifBlockMatcher
            );
        } else if (
            ifInlineParts != null
            && shouldPreservePredicate(
                context,
                statement,
                ifInlineParts.condition()
            )
        ) {
            rejectUnsupportedSource(
                context,
                statement,
                unsupportedStatementKind(statement.text())
            );
        } else if (ifInlineParts != null) {
            parseInlineConditional(
                context,
                statement,
                ifInlineParts
            );
        } else if (forMatcher.matches()) {
            if (
                !canParseForRange(
                    context,
                    forMatcher
                )
            ) {
                parseSymbolicForLoop(
                    context,
                    statement,
                    forMatcher
                );
                return;
            }
            parseForLoop(
                context,
                statement,
                forMatcher
            );
        } else if (
            whileMatcher.matches()
            && shouldPreservePredicate(
                context,
                statement,
                whileMatcher.group(1)
            )
        ) {
            rejectUnsupportedSource(
                context,
                statement,
                unsupportedStatementKind(statement.text())
            );
        } else if (whileMatcher.matches()) {
            parseWhileLoop(
                context,
                statement,
                whileMatcher
            );
        } else if (delayMatcher.matches()) {
            parseDelay(
                context,
                statement,
                delayMatcher
            );
        } else if (boxMatcher.matches()) {
            parseTimingBox(
                context,
                statement,
                boxMatcher
            );
        } else if (ifMatcher.matches()) {
            parseControlled(
                context,
                statement,
                ifMatcher
            );
        } else if (measureAssignmentMatcher.matches()) {
            parseMeasure(
                context,
                statement,
                measureAssignmentMatcher.group(2),
                measureAssignmentMatcher.group(1)
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
        } else if (
            classicalAssignmentMatcher.matches()
            && topLevelAssignmentIndex(statement.text()) >= 0
            && !isMeasureAssignment(statement.text())
        ) {
            parseClassicalAssignment(
                context,
                statement,
                classicalAssignmentMatcher
            );
        } else if (isUnsupportedStatement(statement.text())) {
            rejectUnsupportedSource(
                context,
                statement
            );
        } else {
            parseGate(
                context,
                statement
            );
        }
    }

    private static boolean shouldPreserveSourceStatement(final String statement) {
        final String trimmed = statement.trim();
        final String lowerCaseStatement = trimmed.toLowerCase();
        return lowerCaseStatement.startsWith("defcalgrammar")
            || lowerCaseStatement.startsWith("defcal ")
            || lowerCaseStatement.startsWith("extern ")
            || lowerCaseStatement.startsWith("def ")
            || lowerCaseStatement.startsWith("input ")
            || lowerCaseStatement.startsWith("output ")
            || lowerCaseStatement.startsWith("pragma ")
            || lowerCaseStatement.startsWith("annotation ")
            || lowerCaseStatement.startsWith("duration ")
            || lowerCaseStatement.startsWith("stretch ");
    }

    private static boolean isCalibrationSourceStatement(final String statement) {
        final String lowerCaseStatement = statement.trim().toLowerCase();
        return lowerCaseStatement.startsWith("defcalgrammar")
            || lowerCaseStatement.startsWith("defcal ");
    }

    private static void parseCalibrationDefinition(
        final ParseContext context,
        final Statement statement
    ) {
        context.program().addCalibrationDefinition(new CalibrationDefinition(
            calibrationTargetName(statement),
            List.of(),
            List.of(),
            CALIBRATION_BODY_LANGUAGE,
            statement.text()
        ));
    }

    private static String calibrationTargetName(final Statement statement) {
        final String trimmed = statement.text().trim();
        if (trimmed.toLowerCase().startsWith("defcalgrammar")) {
            return "defcalgrammar_line_" + statement.line();
        }
        final Matcher matcher = DEFCAL_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "defcal_line_" + statement.line();
    }

    private static boolean shouldPreserveSourceOperation(final String statement) {
        final String trimmed = statement.trim();
        final String lowerCaseStatement = trimmed.toLowerCase();
        if (lowerCaseStatement.contains("measure")) {
            return false;
        }
        if (lowerCaseStatement.startsWith("if")) {
            return false;
        }
        return lowerCaseStatement.startsWith("let ")
            || isFunctionCallStatement(trimmed)
            || lowerCaseStatement.contains("+=")
            || lowerCaseStatement.contains("-=")
            || lowerCaseStatement.contains("*=")
            || lowerCaseStatement.contains("/=")
            || lowerCaseStatement.contains("^=")
            || lowerCaseStatement.contains("<<=")
            || lowerCaseStatement.contains(">>=");
    }

    private static boolean isMeasureAssignment(final String statement) {
        return MEASURE_ASSIGNMENT_PATTERN.matcher(statement).matches();
    }

    private static boolean shouldPreserveClassicalDeclaration(final Matcher matcher) {
        final String value = matcher.group(5);
        if (value == null) {
            return false;
        }
        final String trimmed = value.trim();
        return trimmed.contains("[")
            || trimmed.contains("{")
            || trimmed.contains("sizeof")
            || trimmed.contains(",")
            || isFunctionCallExpression(trimmed);
    }

    private static boolean isFunctionCallAssignment(final String statement) {
        final Matcher matcher = CLASSICAL_ASSIGNMENT_PATTERN.matcher(statement);
        return matcher.matches()
            && isFunctionCallExpression(matcher.group(2).trim());
    }

    private static boolean isArrayAssignment(final String statement) {
        final Matcher matcher = CLASSICAL_ASSIGNMENT_PATTERN.matcher(statement);
        return matcher.matches()
            && (
                matcher.group(1).contains("[")
                || matcher.group(2).contains("[")
                || matcher.group(2).contains("{")
            );
    }

    private static boolean isOpenQasm2RegisterSyntax(final String statement) {
        final String lowerCaseStatement = statement.trim().toLowerCase();
        return lowerCaseStatement.startsWith("qreg ")
            || lowerCaseStatement.startsWith("creg ");
    }

    private static boolean shouldPreservePredicate(final String predicate) {
        return predicate.trim().isBlank();
    }

    private static boolean shouldPreservePredicate(
        final ParseContext context,
        final Statement statement,
        final String predicate
    ) {
        if (evaluateStaticBooleanPredicate(
                context,
                statement,
                predicate
        ) != null) {
            return false;
        }
        return shouldPreservePredicate(predicate);
    }

    private static boolean predicateReferencesKnownClassicalValues(
        final ParseContext context,
        final String predicate
    ) {
        final Matcher matcher = CLASSICAL_COMPARISON_PATTERN.matcher(predicate.trim());
        if (!matcher.matches()) {
            return false;
        }
        return expressionReferencesKnownClassicalValues(
            context,
            matcher.group(1).trim()
        )
            && expressionReferencesKnownClassicalValues(
                context,
                matcher.group(3).trim()
            );
    }

    private static boolean expressionReferencesKnownClassicalValues(
        final ParseContext context,
        final String expression
    ) {
        String value = expression.trim();
        final Matcher castMatcher = CLASSICAL_CAST_PATTERN.matcher(value);
        if (castMatcher.matches()) {
            value = castMatcher.group(1).trim();
        }
        if (value.matches("-?\\d+")) {
            return true;
        }
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(value);
        if (indexedMatcher.matches()) {
            return context.classicalRegister(indexedMatcher.group(1)) != null
                || context.classicalAlias(indexedMatcher.group(1)) != null
                || context.integerArray(indexedMatcher.group(1)) != null
                || context.integerConstant(indexedMatcher.group(1)) != null;
        }
        return context.classicalRegister(value) != null
            || context.classicalAlias(value) != null
            || context.integerConstant(value) != null;
    }

    private static boolean isFunctionCallStatement(final String statement) {
        return Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*\\)$",
            Pattern.DOTALL
        ).matcher(statement).matches();
    }

    private static boolean isFunctionCallExpression(final String value) {
        return Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*\\)$",
            Pattern.DOTALL
        ).matcher(value).matches();
    }

    private static void rejectUnsupportedSource(
        final ParseContext context,
        final Statement statement
    ) {
        rejectUnsupportedSource(
            context,
            statement,
            unsupportedStatementKind(statement.text())
        );
    }

    private static void rejectUnsupportedSource(
        final ParseContext context,
        final Statement statement,
        final String kind
    ) {
        context.addError(
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            "OpenQASM 3 " + kind + " is not represented by Quantum IR: " + statement.text() + ".",
            statement
        );
    }

    private static Operation[] rejectUnsupportedBlockSource(
        final ParseContext context,
        final Statement statement
    ) {
        rejectUnsupportedSource(
            context,
            statement
        );
        return null;
    }

    private static String unsupportedStatementKind(final String statement) {
        final String lowerCaseStatement = statement.trim().toLowerCase();
        if (lowerCaseStatement.startsWith("defcalgrammar")) {
            return "defcalgrammar";
        }
        if (lowerCaseStatement.startsWith("defcal ")) {
            return "defcal";
        }
        if (lowerCaseStatement.startsWith("extern ")) {
            return "extern";
        }
        if (lowerCaseStatement.startsWith("def ")) {
            return "def";
        }
        if (lowerCaseStatement.startsWith("array")) {
            return "array";
        }
        if (lowerCaseStatement.startsWith("let ")) {
            return "let";
        }
        if (lowerCaseStatement.contains("=")) {
            return "classical_statement";
        }
        return "statement";
    }

    private static boolean isUnsupportedStatement(final String statement) {
        final String lowerCaseStatement = statement.toLowerCase();
        return lowerCaseStatement.startsWith("if ")
            || lowerCaseStatement.startsWith("defcal")
            || lowerCaseStatement.startsWith("def ")
            || lowerCaseStatement.startsWith("extern ")
            || lowerCaseStatement.startsWith("input ")
            || lowerCaseStatement.startsWith("output ")
            || lowerCaseStatement.startsWith("duration")
            || lowerCaseStatement.startsWith("stretch")
            || lowerCaseStatement.startsWith("angle")
            || lowerCaseStatement.startsWith("uint")
            || lowerCaseStatement.startsWith("int")
            || lowerCaseStatement.startsWith("float")
            || lowerCaseStatement.startsWith("bool");
    }

    private static void parseInclude(
        final ParseContext context,
        final Statement statement,
        final String includeName
    ) {
        if ("stdgates.inc".equals(includeName)) {
            return;
        }
        final String includedSource = context.includedSource(includeName);
        if (includedSource == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 3 include source is not available: " + includeName + ".",
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
                "OpenQASM 3 include cycle detected: " + includeName + ".",
                includeStatement
            );
            return;
        }
        context.enterInclude(includeName);
        final OpenQasm3Ast ast = new OpenQasm3AstParser().parse(includedSource);
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

    private static void parseSubroutineDefinition(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final SubroutineBodyParts bodyParts = splitSubroutineBody(
            matcher.group(4)
        );
        if (
            bodyParts == null
            || shouldPreserveSubroutineBody(bodyParts.body())
        ) {
            rejectUnsupportedSource(
                context,
                statement,
                "def"
            );
            return;
        }
        final SubroutineArgument[] arguments = parseSubroutineArguments(
            context,
            statement,
            matcher.group(2)
        );
        if (arguments == null) {
            rejectUnsupportedSource(
                context,
                statement,
                "def"
            );
            return;
        }
        context.addSubroutine(new SubroutineDefinition(
            matcher.group(1),
            arguments,
            bodyParts.body(),
            bodyParts.returnExpression(),
            matcher.group(3) == null ? null : matcher.group(3).trim()
        ));
    }

    private static boolean shouldPreserveSubroutineBody(final String body) {
        final String lowerCaseBody = body.toLowerCase();
        return lowerCaseBody.contains("extern ")
            || lowerCaseBody.contains("defcal")
            || lowerCaseBody.contains("input ")
            || lowerCaseBody.contains("output ");
    }

    private static SubroutineBodyParts splitSubroutineBody(final String body) {
        final OpenQasm3Ast ast = new OpenQasm3AstParser().parse(body);
        if (!ast.diagnostics().isEmpty()) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        String returnExpression = null;
        final ArrayList<Statement> statements = statementsFromAst(ast);
        for (int i = 0; i < statements.size(); i++) {
            final String text = statements.get(i).text().trim();
            if (text.isBlank()) {
                continue;
            }
            final Matcher returnMatcher = RETURN_PATTERN.matcher(text);
            if (returnMatcher.matches()) {
                if (
                    returnExpression != null
                    || i != statements.size() - 1
                ) {
                    return null;
                }
                returnExpression = returnMatcher.group(1).trim();
            } else {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(text).append(';');
            }
        }
        return new SubroutineBodyParts(
            builder.toString(),
            returnExpression
        );
    }

    private static SubroutineArgument[] parseSubroutineArguments(
        final ParseContext context,
        final Statement statement,
        final String text
    ) {
        if (text.isBlank()) {
            return new SubroutineArgument[0];
        }
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            text,
            "subroutine arguments"
        );
        if (parts == null) {
            return null;
        }
        final SubroutineArgument[] arguments = new SubroutineArgument[parts.parts().size()];
        for (int i = 0; i < parts.parts().size(); i++) {
            final String argument = parts.parts().get(i).trim();
            final String[] tokens = argument.split("\\s+");
            if (tokens.length < 2) {
                return null;
            }
            final String name = tokens[tokens.length - 1].trim();
            final String type = argument.substring(
                0,
                argument.length() - name.length()
            ).trim().toLowerCase();
            if (!IDENTIFIER_PATTERN.matcher(name).matches()) {
                return null;
            }
            if (type.startsWith("qubit")) {
                arguments[i] = new SubroutineArgument(
                    name,
                    SubroutineArgumentKind.QUANTUM
                );
            } else if (
                type.startsWith("bit")
                || type.startsWith("bool")
                || type.startsWith("float")
                || type.startsWith("angle")
                || type.startsWith("duration")
                || type.startsWith("stretch")
                || type.startsWith("array")
                || type.startsWith("readonly array")
                || type.startsWith("mutable array")
            ) {
                arguments[i] = new SubroutineArgument(
                    name,
                    SubroutineArgumentKind.CLASSICAL
                );
            } else if (
                type.startsWith("int")
                || type.startsWith("uint")
            ) {
                arguments[i] = new SubroutineArgument(
                    name,
                    SubroutineArgumentKind.INTEGER
                );
            } else {
                return null;
            }
        }
        return arguments;
    }

    private static void parseStaticArrayDeclaration(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalArrayDeclarationOperation operation = parseArrayDeclarationOperation(
            context,
            statement
        );
        if (operation != null) {
            final int firstOperationIndex = context.circuit().operationCount();
            context.circuit().classicalArrayDeclaration(operation);
            context.attachMetadataFrom(
                firstOperationIndex,
                statement
            );
        }
        if (
            matcher.group(3) == null
            || matcher.group(1).contains(",")
        ) {
            return;
        }
        final Long size = evaluateIntegerExpression(
            context,
            statement,
            matcher.group(1)
        );
        if (
            size == null
            || size.longValue() <= 0L
            || size.longValue() > Integer.MAX_VALUE
        ) {
            return;
        }
        final ListParts values = parseCommaAwareParts(
            context,
            statement,
            matcher.group(3),
            "array initializer"
        );
        if (
            values == null
            || values.parts().size() != size.intValue()
        ) {
            return;
        }
        final long[] array = new long[values.parts().size()];
        for (int i = 0; i < values.parts().size(); i++) {
            final Long value = evaluateIntegerExpression(
                context,
                statement,
                values.parts().get(i)
            );
            if (value == null) {
                return;
            }
            array[i] = value.longValue();
        }
        context.addIntegerArray(
            matcher.group(2),
            array
        );
    }

    private static boolean isArrayDeclaration(final String statement) {
        return statement.trim().regionMatches(
            true,
            0,
            "array",
            0,
            5
        );
    }

    private static void parseGenericArrayDeclaration(
        final ParseContext context,
        final Statement statement
    ) {
        final ClassicalArrayDeclarationOperation operation = parseArrayDeclarationOperation(
            context,
            statement
        );
        if (operation == null) {
            rejectUnsupportedSource(
                context,
                statement,
                "array"
            );
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().classicalArrayDeclaration(operation);
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseExternDeclaration(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            matcher.group(2),
            "extern arguments"
        );
        if (parts == null) {
            return;
        }
        final ArrayList<CallableArgument> arguments = new ArrayList<>();
        for (int i = 0; i < parts.parts().size(); i++) {
            final String argumentText = parts.parts().get(i).trim();
            if (argumentText.isBlank()) {
                continue;
            }
            final ClassicalType type = parseClassicalTypeText(argumentText);
            if (type == null) {
                rejectUnsupportedSource(
                    context,
                    statement,
                    "extern"
                );
                return;
            }
            arguments.add(CallableArgument.classical(
                "arg" + i,
                type
            ));
        }
        final ClassicalType returnType = matcher.group(3) == null
            ? null
            : parseClassicalTypeText(matcher.group(3).trim());
        if (
            matcher.group(3) != null
            && returnType == null
        ) {
            rejectUnsupportedSource(
                context,
                statement,
                "extern"
            );
            return;
        }
        try {
            context.program().addExternalCallableDeclaration(new ExternalCallableDeclaration(
                matcher.group(1),
                returnType,
                arguments.toArray(new CallableArgument[0])
            ));
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
        }
    }

    private static ClassicalArrayDeclarationOperation parseArrayDeclarationOperation(
        final ParseContext context,
        final Statement statement
    ) {
        final String text = statement.text().trim();
        if (!text.regionMatches(
            true,
            0,
            "array",
            0,
            5
        )) {
            return null;
        }
        final int bracketStart = text.indexOf('[');
        if (bracketStart < 0) {
            return null;
        }
        final int bracketEnd = matchingDelimiter(
            text,
            bracketStart,
            '[',
            ']'
        );
        if (bracketEnd < 0) {
            return null;
        }
        final String header = text.substring(
            bracketStart + 1,
            bracketEnd
        );
        final String rest = text.substring(bracketEnd + 1).trim();
        final int assignmentIndex = topLevelAssignmentIndex(rest);
        final String name = assignmentIndex < 0
            ? rest.trim()
            : rest.substring(
                0,
                assignmentIndex
            ).trim();
        final String initializer = assignmentIndex < 0
            ? null
            : rest.substring(assignmentIndex + 1).trim();
        if (!IDENTIFIER_PATTERN.matcher(name).matches()) {
            return null;
        }
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            header,
            "array declaration"
        );
        if (
            parts == null
            || parts.parts().size() < 2
        ) {
            return null;
        }
        final ClassicalType elementType = parseClassicalTypeText(parts.parts().get(0).trim());
        if (elementType == null) {
            return null;
        }
        final ArrayList<ClassicalExpression> dimensions = new ArrayList<>();
        for (int i = 1; i < parts.parts().size(); i++) {
            dimensions.add(parseClassicalExpression(
                context,
                statement,
                parts.parts().get(i).trim()
            ));
        }
        return new ClassicalArrayDeclarationOperation(
            name,
            elementType,
            dimensions,
            initializer
        );
    }

    private static void parseCompileTimeLet(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final String name = matcher.group(1);
        final String value = matcher.group(2).trim();
        final QuantumOperand quantumOperand = tryParseQuantumAlias(
            context,
            statement,
            value
        );
        if (quantumOperand != null) {
            context.addQuantumAlias(
                name,
                quantumOperand
            );
            return;
        }
        final ClassicalOperand classicalOperand = tryParseClassicalAlias(
            context,
            statement,
            value
        );
        if (classicalOperand != null) {
            context.addClassicalAlias(
                name,
                classicalOperand
            );
            return;
        }
        final Long integerValue = evaluateStaticIntegerExpression(
            context,
            statement,
            value
        );
        if (integerValue != null) {
            context.addIntegerConstant(
                name,
                integerValue.longValue()
            );
            return;
        }
        rejectUnsupportedSource(
            context,
            statement,
            "let"
        );
    }

    private static QuantumOperand tryParseQuantumAlias(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        if (!looksLikeQuantumOperand(
                context,
                value
        )) {
            return null;
        }
        return parseQuantumOperand(
            context,
            statement,
            value
        );
    }

    private static ClassicalOperand tryParseClassicalAlias(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        if (!looksLikeClassicalOperand(
                context,
                value
        )) {
            return null;
        }
        return parseClassicalOperand(
            context,
            statement,
            value
        );
    }

    private static boolean looksLikeQuantumOperand(
        final ParseContext context,
        final String value
    ) {
        final String base = baseName(value);
        return base != null
            && (
                context.quantumRegister(base) != null
                || context.quantumAlias(base) != null
            );
    }

    private static boolean looksLikeClassicalOperand(
        final ParseContext context,
        final String value
    ) {
        final String base = baseName(value);
        return base != null
            && (
                context.classicalRegister(base) != null
                || context.classicalAlias(base) != null
            );
    }

    private static String baseName(final String value) {
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(value.trim());
        if (indexedMatcher.matches()) {
            return indexedMatcher.group(1);
        }
        final Matcher registerMatcher = REGISTER_ARGUMENT_PATTERN.matcher(value.trim());
        if (registerMatcher.matches()) {
            return registerMatcher.group(1);
        }
        return null;
    }

    private static void parseQuantumRegister(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final String name = matcher.group(2);
        if (!canParseRegisterSize(
            context,
            sizeOrOne(matcher.group(1))
        )) {
            context.ensureQuantumRegister(
                name,
                64
            );
            return;
        }
        final int size = parsePositiveIntegerExpression(
            context,
            statement,
            sizeOrOne(matcher.group(1))
        );
        if (size <= 0) {
            return;
        }
        final QuantumRegister existingRegister = context.quantumRegister(name);
        if (
            existingRegister != null
            && existingRegister.size() == size
        ) {
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
        final String name = matcher.group(2);
        if (!canParseRegisterSize(
            context,
            sizeOrOne(matcher.group(1))
        )) {
            context.ensureClassicalRegister(
                name,
                64
            );
            return;
        }
        final int size = parsePositiveIntegerExpression(
            context,
            statement,
            sizeOrOne(matcher.group(1))
        );
        if (size <= 0) {
            return;
        }
        final ClassicalRegister existingRegister = context.classicalRegister(name);
        if (
            existingRegister != null
            && existingRegister.size() == size
        ) {
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

    private static void parseClassicalRegisterWithBitstring(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final int beforeCount = context.circuit().operationCount();
        parseClassicalRegister(
            context,
            statement,
            matcher
        );
        final ClassicalRegister register = context.classicalRegister(matcher.group(2));
        if (register == null) {
            return;
        }
        try {
            final long value = Long.parseLong(
                matcher.group(3).substring(
                    1,
                    matcher.group(3).length() - 1
                ),
                2
            );
            context.circuit().assign(new ClassicalAssignment(
                ClassicalExpression.register(register),
                ClassicalExpression.integer(value)
            ));
            context.attachMetadataFrom(
                beforeCount,
                statement
            );
        } catch (final NumberFormatException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse bitstring initializer.",
                statement
            );
        }
    }

    private static Operation[] parseClassicalRegisterWithBitstringOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        parseClassicalRegister(
            context,
            statement,
            matcher
        );
        final ClassicalRegister register = context.classicalRegister(matcher.group(2));
        if (register == null) {
            return new Operation[0];
        }
        try {
            final long value = Long.parseLong(
                matcher.group(3).substring(
                    1,
                    matcher.group(3).length() - 1
                ),
                2
            );
            return new Operation[] {
                new ClassicalAssignmentOperation(new ClassicalAssignment(
                    ClassicalExpression.register(register),
                    ClassicalExpression.integer(value)
                ))
            };
        } catch (final NumberFormatException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse bitstring initializer.",
                statement
            );
            return new Operation[0];
        }
    }

    private static void parseClassicalDeclaration(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final boolean constant = matcher.group(1) != null;
        final String type = matcher.group(2).toLowerCase();
        final String width = matcher.group(3);
        final String name = matcher.group(4);
        final String value = matcher.group(5);
        if ("duration".equals(type)) {
            if (value != null) {
                final DurationExpression duration = parseDuration(
                    context,
                    statement,
                    value
                );
                if (duration != null) {
                    context.addDurationConstant(
                        name,
                        duration
                    );
                }
            }
            appendClassicalDeclarationOperation(
                context,
                statement,
                name,
                type,
                width,
                value == null ? null : ClassicalExpression.symbolicReference(value)
            );
            return;
        }
        if ("stretch".equals(type)) {
            context.addDurationConstant(
                name,
                DurationExpression.stretch(name)
            );
            appendClassicalDeclarationOperation(
                context,
                statement,
                name,
                type,
                width,
                value == null ? null : ClassicalExpression.symbolicReference(value)
            );
            return;
        }
        if (shouldPreserveClassicalDeclaration(matcher)) {
            final ClassicalType classicalType = parseClassicalType(
                type,
                width
            );
            final ClassicalExpression initializer = value == null
                ? null
                : parseClassicalExpression(
                    context,
                    statement,
                    value
                );
            if (
                classicalType != null
                && (
                    value == null
                    || initializer != null
                )
            ) {
                final int firstOperationIndex = context.circuit().operationCount();
                context.circuit().classicalDeclaration(new ClassicalDeclarationOperation(
                    new ClassicalDeclaration(
                        name,
                        classicalType
                    ),
                    initializer
                ));
                context.attachMetadataFrom(
                    firstOperationIndex,
                    statement
                );
                return;
            }
        }
        if (
            value != null
            && (
                "int".equals(type)
                || "uint".equals(type)
            )
        ) {
            final Long evaluated = evaluateIntegerExpression(
                context,
                statement,
                value
            );
            if (evaluated != null) {
                context.addIntegerConstant(
                    name,
                    evaluated.longValue()
                );
            }
            if (constant) {
                return;
            }
        }
        if ("bool".equals(type)) {
            createClassicalRegisterIfMissing(
                context,
                statement,
                name,
                1
            );
            return;
        }
        if ("angle".equals(type)) {
            createClassicalRegisterIfMissing(
                context,
                statement,
                name,
                width == null
                    ? 1
                    : parsePositiveIntegerExpression(
                        context,
                        statement,
                        width
                    )
            );
            return;
        }
        if (
            "int".equals(type)
            || "uint".equals(type)
        ) {
            createClassicalRegisterIfMissing(
                context,
                statement,
                name,
                1
            );
        }
    }

    private static void appendClassicalDeclarationOperation(
        final ParseContext context,
        final Statement statement,
        final String name,
        final String type,
        final String width,
        final ClassicalExpression initializer
    ) {
        final ClassicalType classicalType = parseClassicalType(
            type,
            width
        );
        if (classicalType == null) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().classicalDeclaration(new ClassicalDeclarationOperation(
            new ClassicalDeclaration(
                name,
                classicalType
            ),
            initializer
        ));
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static ClassicalType parseClassicalType(
        final String type,
        final String width
    ) {
        final ClassicalTypeKind kind = switch (type.toLowerCase()) {
            case "bit" -> ClassicalTypeKind.BIT;
            case "bool" -> ClassicalTypeKind.BOOLEAN;
            case "int" -> ClassicalTypeKind.SIGNED_INTEGER;
            case "uint" -> ClassicalTypeKind.UNSIGNED_INTEGER;
            case "float" -> ClassicalTypeKind.FLOAT;
            case "angle" -> ClassicalTypeKind.ANGLE;
            case "duration" -> ClassicalTypeKind.DURATION;
            case "stretch" -> ClassicalTypeKind.STRETCH;
            default -> null;
        };
        if (kind == null) {
            return null;
        }
        if (width == null || width.isBlank()) {
            if (
                kind == ClassicalTypeKind.SIGNED_INTEGER
                || kind == ClassicalTypeKind.UNSIGNED_INTEGER
                || kind == ClassicalTypeKind.FLOAT
                || kind == ClassicalTypeKind.ANGLE
            ) {
                return ClassicalType.sized(
                    kind,
                    64
                );
            }
            return ClassicalType.of(kind);
        }
        try {
            return ClassicalType.sized(
                kind,
                Integer.parseInt(width.trim())
            );
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private static ClassicalType parseClassicalTypeText(final String text) {
        final Matcher matcher = Pattern.compile("^(int|uint|float|angle|bit|creg|bool|duration|stretch)(?:\\[(.+)])?$", Pattern.CASE_INSENSITIVE)
            .matcher(text.trim());
        if (!matcher.matches()) {
            return null;
        }
        final String type = matcher.group(1).equalsIgnoreCase("creg")
            ? "bit"
            : matcher.group(1);
        if (
            matcher.group(2) != null
            && !matcher.group(2).trim().matches("\\d+")
        ) {
            return parseClassicalType(
                type,
                null
            );
        }
        return parseClassicalType(
            type,
            matcher.group(2)
        );
    }

    private static void createClassicalRegisterIfMissing(
        final ParseContext context,
        final Statement statement,
        final String name,
        final int size
    ) {
        if (context.classicalRegister(name) != null) {
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
            rejectUnsupportedSource(
                context,
                statement,
                "opaque_gate_definition"
            );
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
        if (tryParseGateDefinitionAsInlineMacro(
            context,
            statement,
            matcher
        )) {
            return;
        }
        if (isCanonicalCphaseDefinition(
            context,
            statement,
            matcher
        )) {
            return;
        }
        if (isReservedGateDefinitionName(
            context,
            statement,
            matcher.group(1)
        )) {
            rejectUnsupportedSource(
                context,
                statement,
                "gate_definition"
            );
            return;
        }
        if (shouldPreserveGateDefinitionBody(matcher.group(4))) {
            rejectUnsupportedSource(
                context,
                statement,
                "gate_definition"
            );
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

    private static boolean tryParseGateDefinitionAsInlineMacro(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        if (matcher.group(2) != null) {
            return false;
        }
        if (!matcher.group(4).toLowerCase().contains("for ")) {
            return false;
        }
        if (shouldPreserveSubroutineBody(matcher.group(4))) {
            return false;
        }
        final ListParts qubitNames = parseDeclarationNames(
            context,
            statement,
            matcher.group(3)
        );
        if (
            qubitNames == null
            || qubitNames.parts().isEmpty()
        ) {
            return false;
        }
        final SubroutineArgument[] arguments = new SubroutineArgument[qubitNames.parts().size()];
        for (int i = 0; i < qubitNames.parts().size(); i++) {
            arguments[i] = new SubroutineArgument(
                qubitNames.parts().get(i),
                SubroutineArgumentKind.QUANTUM
            );
        }
        context.addSubroutine(new SubroutineDefinition(
            matcher.group(1),
            arguments,
            matcher.group(4),
            null,
            null
        ));
        return true;
    }

    private static boolean isCanonicalCphaseDefinition(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        if (!"cphase".equalsIgnoreCase(matcher.group(1))) {
            return false;
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
            || parameterNames.parts().size() != 1
            || qubitNames == null
            || qubitNames.parts().size() != 2
        ) {
            return false;
        }
        final String parameterName = parameterNames.parts().get(0);
        final String firstQubit = qubitNames.parts().get(0);
        final String secondQubit = qubitNames.parts().get(1);
        final String normalizedBody = matcher.group(4)
            .replaceAll("\\s+", "")
            .toLowerCase();
        final String expectedBody = (
            "u(0,0," + parameterName + "/2)" + firstQubit + ";"
                + "cx" + firstQubit + "," + secondQubit + ";"
                + "u(0,0,-" + parameterName + "/2)" + secondQubit + ";"
                + "cx" + firstQubit + "," + secondQubit + ";"
                + "u(0,0," + parameterName + "/2)" + secondQubit + ";"
        ).toLowerCase();
        return normalizedBody.equals(expectedBody);
    }

    private static boolean isReservedGateDefinitionName(
        final ParseContext context,
        final Statement statement,
        final String name
    ) {
        if (OpenQasm3GateNames.isReservedStdAlias(name)) {
            return true;
        }
        return false;
    }

    private static boolean shouldPreserveGateDefinitionBody(final String body) {
        final String lowerCaseBody = body.toLowerCase();
        return lowerCaseBody.contains("for ")
            || lowerCaseBody.contains("if(")
            || lowerCaseBody.contains("if (")
            || lowerCaseBody.contains("while ")
            || lowerCaseBody.contains("return ")
            || lowerCaseBody.contains("let ");
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
                "OpenQASM 3 gate body supports gate operations only.",
                statement
            );
            return null;
        }
        final Gate gate = resolveGate(
            context,
            matcher.group(1),
            matcher.group(2)
        );
        if (gate == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 import does not support gate in body: " + matcher.group(1) + ".",
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
            rejectUnsupportedSource(
                context,
                statement,
                unsupportedStatementKind(statement.text())
            );
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        for (int i = 0; i < qubits.size(); i++) {
            context.circuit().measureReference(
                qubits.reference(i),
                bits.bit(i)
            );
        }
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void appendGateOperation(
        final QuantumCircuit circuit,
        final Gate gate,
        final ParameterExpression[] parameters,
        final QuantumReference[] references
    ) {
        if (parameters.length == 0) {
            circuit.gateReferences(
                gate,
                references
            );
        } else {
            circuit.parameterizedGateReferences(
                gate,
                parameters,
                references
            );
        }
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
            context.circuit().resetReference(qubits.reference(i));
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

    private static void parseConditionalBlock(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final Boolean staticResult = evaluateStaticBooleanPredicate(
            context,
            statement,
            matcher.group(1)
        );
        if (staticResult != null) {
            if (staticResult.booleanValue()) {
                parseStaticConditionalBody(
                    context,
                    statement,
                    matcher.group(2)
                );
            }
            return;
        }
        final ClassicalPredicate predicate = parseClassicalPredicate(
            context,
            statement,
            matcher.group(1)
        );
        final BlockParts parts = splitConditionalBody(
            context,
            statement,
            matcher.group(2)
        );
        if (
            predicate == null
            || parts == null
        ) {
            return;
        }
        final OperationBlock thenBlock = parseOperationBlock(
            context,
            statement,
            parts.thenText()
        );
        if (thenBlock == null) {
            return;
        }
        final OperationBlock elseBlock = parts.hasElseText()
            ? parseOperationBlock(
                context,
                statement,
                parts.elseText()
            )
            : null;
        if (
            parts.hasElseText()
            && elseBlock == null
        ) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().conditionalBlock(
            predicate,
            thenBlock,
            elseBlock
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseInlineConditional(
        final ParseContext context,
        final Statement statement,
        final IfInlineParts parts
    ) {
        final Boolean staticResult = evaluateStaticBooleanPredicate(
            context,
            statement,
            parts.condition()
        );
        if (staticResult != null) {
            if (staticResult.booleanValue()) {
                parseStatement(
                    context,
                    new Statement(
                        parts.body(),
                        statement.line(),
                        statement.column()
                    )
                );
            }
            return;
        }
        final ClassicalPredicate predicate = parseClassicalPredicate(
            context,
            statement,
            parts.condition()
        );
        final OperationBlock body = parseOperationBlock(
            context,
            statement,
            parts.body()
        );
        if (
            predicate == null
            || body == null
        ) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().conditionalBlock(
            predicate,
            body,
            null
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseStaticConditionalBody(
        final ParseContext context,
        final Statement parentStatement,
        final String bodyText
    ) {
        final OpenQasm3Ast ast = new OpenQasm3AstParser().parse(bodyText);
        context.addDiagnostics(new ArrayList<>(ast.diagnostics()));
        if (context.hasErrors()) {
            return;
        }
        final ArrayList<Statement> statements = statementsFromAst(ast);
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            parseStatement(
                context,
                statement.text().isBlank()
                    ? parentStatement
                    : statement
            );
            if (context.hasErrors()) {
                return;
            }
        }
    }

    private static void parseForLoop(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final Long startInclusive = evaluateIntegerExpression(
            context,
            statement,
            matcher.group(2)
        );
        final Long step = matcher.group(3) == null
            ? Long.valueOf(1L)
            : evaluateIntegerExpression(
                context,
                statement,
                matcher.group(3)
            );
        final Long endInclusive = evaluateIntegerExpression(
            context,
            statement,
            matcher.group(4)
        );
        if (
            context.hasErrors()
            || startInclusive == null
            || step == null
            || endInclusive == null
            || step.longValue() == 0L
        ) {
            if (
                step != null
                && step.longValue() == 0L
            ) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "OpenQASM 3 for-loop step must not be zero.",
                    statement
                );
            }
            return;
        }
        final OperationBlock body = parseOperationBlock(
            context,
            statement,
            matcher.group(5)
        );
        if (body == null) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().forLoop(
            matcher.group(1),
            startInclusive.longValue(),
            step.longValue(),
            endInclusive.longValue(),
            body
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseSymbolicForLoop(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final Operation[] operations = parseBlockSymbolicForLoopOperation(
            context,
            statement,
            matcher
        );
        if (
            operations == null
            || operations.length == 0
        ) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().symbolicForLoop((SymbolicForLoopOperation) operations[0]);
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseWhileLoop(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalPredicate predicate = parseClassicalPredicate(
            context,
            statement,
            matcher.group(1)
        );
        final OperationBlock body = parseOperationBlock(
            context,
            statement,
            matcher.group(2)
        );
        if (
            predicate == null
            || body == null
        ) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().whileLoop(
            predicate,
            body
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseDelay(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final DurationExpression duration = parseDuration(
            context,
            statement,
            matcher.group(1)
        );
        final ArrayList<QuantumReference> references = parseDelayReferences(
            context,
            statement,
            matcher.group(2)
        );
        if (
            duration == null
            || references == null
        ) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().delayReferences(
            duration,
            references.toArray(new QuantumReference[0])
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseTimingBox(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final DurationExpression duration = matcher.group(1) == null
            ? null
            : parseDuration(
                context,
                statement,
                matcher.group(1)
            );
        final OperationBlock body = parseOperationBlock(
            context,
            statement,
            matcher.group(2)
        );
        if (
            matcher.group(1) != null
            && duration == null
        ) {
            return;
        }
        if (body == null) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().timingBox(
            duration,
            body
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static void parseClassicalAssignment(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalExpression target = parseClassicalAssignmentTarget(
            context,
            statement,
            matcher.group(1).trim()
        );
        final ClassicalExpression value = parseClassicalExpression(
            context,
            statement,
            matcher.group(2).trim()
        );
        if (
            target == null
            || value == null
        ) {
            return;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().assign(new ClassicalAssignment(
            target,
            value
        ));
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
    }

    private static boolean tryParseSubroutineCall(
        final ParseContext context,
        final Statement statement
    ) {
        final SubroutineCallParts callParts = parseSubroutineCallParts(statement.text());
        if (callParts == null) {
            return false;
        }
        final SubroutineDefinition definition = context.subroutine(callParts.name());
        if (definition == null) {
            return false;
        }
        if (context.isSubroutineInlineActive(definition.name())) {
            appendGenericCallableInvocation(
                context,
                statement
            );
            return true;
        }
        final ListParts arguments = parseCommaAwareParts(
            context,
            statement,
            callParts.argumentsText(),
            "subroutine call arguments"
        );
        if (arguments == null) {
            return true;
        }
        if (arguments.parts().size() != definition.argumentCount()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 3 subroutine argument count does not match definition.",
                statement
            );
            return true;
        }
        final AliasScope scope = bindSubroutineArguments(
            context,
            statement,
            definition,
            arguments
        );
        if (scope == null) {
            if (!context.hasErrors()) {
                appendGenericCallableInvocation(
                    context,
                    statement
                );
            }
            return true;
        }
        context.enterSubroutineInline(definition.name());
        try {
            parseStaticConditionalBody(
                context,
                statement,
                definition.body()
            );
        } finally {
            context.exitSubroutineInline(definition.name());
            context.restoreAliasScope(scope);
        }
        return true;
    }

    private static void appendGenericCallableInvocation(
        final ParseContext context,
        final Statement statement
    ) {
        final CallableInvocationOperation operation = parseGenericCallableInvocationOperation(
            context,
            statement,
            statement.text(),
            null
        );
        if (operation == null) {
            rejectUnsupportedSource(
                context,
                statement,
                "statement"
            );
            return;
        }
        context.circuit().callableInvocation(operation);
    }

    private static boolean tryParseGenericCallableInvocation(
        final ParseContext context,
        final Statement statement
    ) {
        final CallableInvocationOperation operation = parseGenericCallableInvocationOperation(
            context,
            statement,
            statement.text(),
            null
        );
        if (operation == null) {
            return false;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().callableInvocation(operation);
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
        return true;
    }

    private static CallableInvocationOperation parseGenericCallableInvocationOperation(
        final ParseContext context,
        final Statement statement,
        final String text,
        final ClassicalExpression target
    ) {
        final SubroutineCallParts callParts = parseSubroutineCallParts(text);
        if (callParts == null) {
            return null;
        }
        if (
            topLevelAssignmentIndex(text) >= 0
            || !context.hasCallable(callParts.name())
        ) {
            return null;
        }
        if (resolveGate(
            context,
            callParts.name()
        ) != null) {
            return null;
        }
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            callParts.argumentsText(),
            "callable invocation arguments"
        );
        if (parts == null) {
            return null;
        }
        final ArrayList<ClassicalExpression> classicalArguments = new ArrayList<>();
        final ArrayList<QuantumReference> quantumArguments = new ArrayList<>();
        for (int i = 0; i < parts.parts().size(); i++) {
            final String part = parts.parts().get(i).trim();
            if (part.isBlank()) {
                continue;
            }
            if (isLikelyQuantumArgument(context, part)) {
                final QuantumOperand operand = parseQuantumOperand(
                    context,
                    statement,
                    part
                );
                if (operand == null) {
                    return null;
                }
                for (int j = 0; j < operand.size(); j++) {
                    quantumArguments.add(operand.reference(j));
                }
            } else {
                final ClassicalExpression expression = parseClassicalExpression(
                    context,
                    statement,
                    part
                );
                if (expression == null) {
                    return null;
                }
                classicalArguments.add(expression);
            }
        }
        return new CallableInvocationOperation(
            callParts.name(),
            target,
            classicalArguments,
            quantumArguments
        );
    }

    private static boolean isLikelyQuantumArgument(
        final ParseContext context,
        final String text
    ) {
        final String trimmed = text.trim();
        if (PHYSICAL_QUBIT_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(trimmed);
        if (indexedMatcher.matches()) {
            return context.quantumRegister(indexedMatcher.group(1)) != null
                || context.quantumAlias(indexedMatcher.group(1)) != null;
        }
        final Matcher slicedMatcher = SLICED_ARGUMENT_PATTERN.matcher(trimmed);
        if (slicedMatcher.matches()) {
            return context.quantumRegister(slicedMatcher.group(1)) != null
                || context.quantumAlias(slicedMatcher.group(1)) != null;
        }
        return context.quantumRegister(trimmed) != null
            || context.quantumAlias(trimmed) != null;
    }

    private static SubroutineCallParts parseSubroutineCallParts(final String text) {
        final Matcher mixedMatcher = SUBROUTINE_MIXED_CALL_PATTERN.matcher(text);
        if (mixedMatcher.matches()) {
            final String leadingArguments = mixedMatcher.group(2).trim();
            final String trailingArguments = mixedMatcher.group(3).trim();
            final String combinedArguments = leadingArguments.isBlank()
                ? trailingArguments
                : leadingArguments + ", " + trailingArguments;
            return new SubroutineCallParts(
                mixedMatcher.group(1),
                combinedArguments
            );
        }
        final Matcher matcher = SUBROUTINE_CALL_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        return new SubroutineCallParts(
            matcher.group(1),
            matcher.group(2) == null
                ? matcher.group(3) == null ? "" : matcher.group(3)
                : matcher.group(2)
        );
    }

    private static boolean tryParseReturningSubroutineAssignment(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ReturningSubroutineCall call = parseReturningSubroutineCall(
            context,
            statement,
            matcher.group(2).trim()
        );
        if (call == null) {
            return false;
        }
        final ClassicalOperand target = parseClassicalOperand(
            context,
            statement,
            matcher.group(1).trim()
        );
        if (target == null) {
            return true;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        inlineReturningSubroutineCall(
            context,
            statement,
            call,
            target
        );
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
        return true;
    }

    private static Operation[] parseReturningSubroutineAssignmentOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ReturningSubroutineCall call = parseReturningSubroutineCall(
            context,
            statement,
            matcher.group(2).trim()
        );
        if (call == null) {
            return null;
        }
        final ClassicalOperand target = parseClassicalOperand(
            context,
            statement,
            matcher.group(1).trim()
        );
        if (target == null) {
            return new Operation[0];
        }
        final ArrayList<Operation> operations = inlineReturningSubroutineCallToOperations(
            context,
            statement,
            call,
            target
        );
        return operations == null ? new Operation[0] : operations.toArray(new Operation[0]);
    }

    private static ReturningSubroutineCall parseReturningSubroutineCall(
        final ParseContext context,
        final Statement statement,
        final String expression
    ) {
        final Matcher matcher = SUBROUTINE_CALL_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            return null;
        }
        final SubroutineDefinition definition = context.subroutine(matcher.group(1));
        if (
            definition == null
            || definition.returnExpression() == null
            || !isBitLikeReturnType(definition.returnType())
            || context.isSubroutineInlineActive(definition.name())
        ) {
            return null;
        }
        final String argumentsText = matcher.group(2) == null
            ? matcher.group(3)
            : matcher.group(2);
        final ListParts arguments = parseCommaAwareParts(
            context,
            statement,
            argumentsText == null ? "" : argumentsText,
            "subroutine call arguments"
        );
        if (arguments == null) {
            return null;
        }
        if (arguments.parts().size() != definition.argumentCount()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 3 subroutine argument count does not match definition.",
                statement
            );
            return null;
        }
        return new ReturningSubroutineCall(
            definition,
            arguments
        );
    }

    private static boolean isBitLikeReturnType(final String returnType) {
        if (returnType == null) {
            return false;
        }
        final String lowerCaseReturnType = returnType.toLowerCase();
        return lowerCaseReturnType.startsWith("bit")
            || lowerCaseReturnType.startsWith("bool");
    }

    private static void inlineReturningSubroutineCall(
        final ParseContext context,
        final Statement statement,
        final ReturningSubroutineCall call,
        final ClassicalOperand target
    ) {
        final AliasScope scope = bindSubroutineArguments(
            context,
            statement,
            call.definition(),
            call.arguments()
        );
        if (scope == null) {
            return;
        }
        context.enterSubroutineInline(call.definition().name());
        try {
            parseStaticConditionalBody(
                context,
                statement,
                call.definition().body()
            );
            if (!context.hasErrors()) {
                appendReturnBinding(
                    context,
                    statement,
                    call.definition().returnExpression(),
                    target
                );
            }
        } finally {
            context.exitSubroutineInline(call.definition().name());
            context.restoreAliasScope(scope);
        }
    }

    private static ArrayList<Operation> inlineReturningSubroutineCallToOperations(
        final ParseContext context,
        final Statement statement,
        final ReturningSubroutineCall call,
        final ClassicalOperand target
    ) {
        final AliasScope scope = bindSubroutineArguments(
            context,
            statement,
            call.definition(),
            call.arguments()
        );
        if (scope == null) {
            return null;
        }
        final ArrayList<Operation> operations = new ArrayList<>();
        context.enterSubroutineInline(call.definition().name());
        final OperationBlock body;
        try {
            body = parseOperationBlock(
                context,
                statement,
                call.definition().body()
            );
        } finally {
            context.exitSubroutineInline(call.definition().name());
        }
        if (body == null) {
            context.restoreAliasScope(scope);
            return null;
        }
        for (int i = 0; i < body.operationCount(); i++) {
            operations.add(body.operation(i));
        }
        final Operation[] returnOperations = returnBindingOperations(
            context,
            statement,
            call.definition().returnExpression(),
            target
        );
        if (returnOperations != null) {
            for (int i = 0; i < returnOperations.length; i++) {
                operations.add(returnOperations[i]);
            }
        }
        context.restoreAliasScope(scope);
        return operations;
    }

    private static AliasScope bindSubroutineArguments(
        final ParseContext context,
        final Statement statement,
        final SubroutineDefinition definition,
        final ListParts arguments
    ) {
        final AliasScope scope = context.openAliasScope(definition.arguments());
        for (int i = 0; i < definition.argumentCount(); i++) {
            final SubroutineArgument argument = definition.argument(i);
            if (argument.kind() == SubroutineArgumentKind.QUANTUM) {
                final QuantumOperand operand = parseQuantumOperand(
                    context,
                    statement,
                    arguments.parts().get(i).trim()
                );
                if (operand == null) {
                    context.restoreAliasScope(scope);
                    return null;
                }
                context.addQuantumAlias(
                    argument.name(),
                    operand
                );
            } else if (argument.kind() == SubroutineArgumentKind.CLASSICAL) {
                final ClassicalOperand operand = parseClassicalOperand(
                    context,
                    statement,
                    arguments.parts().get(i).trim()
                );
                if (operand == null) {
                    context.restoreAliasScope(scope);
                    return null;
                }
                context.addClassicalAlias(
                    argument.name(),
                    operand
                );
            } else {
                if (!canParseIntegerExpression(
                    context,
                    arguments.parts().get(i).trim()
                )) {
                    final ClassicalExpression expression = parseRuntimeIntegerExpression(
                        context,
                        statement,
                        arguments.parts().get(i).trim()
                    );
                    if (expression == null) {
                        context.restoreAliasScope(scope);
                        return null;
                    }
                    context.addIntegerExpression(
                        argument.name(),
                        expression
                    );
                    continue;
                }
                final Long value = evaluateIntegerExpression(
                    context,
                    statement,
                    arguments.parts().get(i).trim()
                );
                if (value == null) {
                    context.restoreAliasScope(scope);
                    return null;
                }
                context.addIntegerConstant(
                    argument.name(),
                    value.longValue()
                );
            }
        }
        return scope;
    }

    private static boolean tryParseCompileTimeIntegerCompoundAssignment(
        final ParseContext context,
        final Statement statement
    ) {
        final Matcher matcher = INTEGER_COMPOUND_ASSIGNMENT_PATTERN.matcher(statement.text());
        if (!matcher.matches()) {
            return false;
        }
        final Long previous = context.integerConstant(matcher.group(1));
        if (previous == null) {
            return false;
        }
        final Long value = evaluateIntegerExpression(
            context,
            statement,
            matcher.group(3)
        );
        if (value == null) {
            return false;
        }
        final long updated;
        if ("+".equals(matcher.group(2))) {
            updated = previous.longValue() + value.longValue();
        } else if ("-".equals(matcher.group(2))) {
            updated = previous.longValue() - value.longValue();
        } else if ("*".equals(matcher.group(2))) {
            updated = previous.longValue() * value.longValue();
        } else {
            if (value.longValue() == 0L) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "OpenQASM 3 integer division assignment must not divide by zero.",
                    statement
                );
                return true;
            }
            updated = previous.longValue() / value.longValue();
        }
        context.addIntegerConstant(
            matcher.group(1),
            updated
        );
        return true;
    }

    private static boolean tryParseRuntimeIntegerCompoundAssignment(
        final ParseContext context,
        final Statement statement
    ) {
        final Operation operation = parseRuntimeIntegerCompoundAssignment(
            context,
            statement
        );
        if (operation == null) {
            return false;
        }
        final int firstOperationIndex = context.circuit().operationCount();
        context.circuit().assign(((ClassicalAssignmentOperation) operation).assignment());
        context.attachMetadataFrom(
            firstOperationIndex,
            statement
        );
        return true;
    }

    private static Operation parseRuntimeIntegerCompoundAssignment(
        final ParseContext context,
        final Statement statement
    ) {
        final Matcher matcher = INTEGER_COMPOUND_ASSIGNMENT_PATTERN.matcher(statement.text());
        if (
            !matcher.matches()
            || topLevelAssignmentIndex(statement.text()) < 0
        ) {
            return null;
        }
        final ClassicalExpression right = parseRuntimeIntegerExpression(
            context,
            statement,
            matcher.group(3)
        );
        if (right == null) {
            return null;
        }
        final ClassicalExpression target = ClassicalExpression.variable(matcher.group(1));
        return new ClassicalAssignmentOperation(new ClassicalAssignment(
            target,
            ClassicalExpression.binary(
                compoundAssignmentOperator(matcher.group(2)),
                target,
                right
            )
        ));
    }

    private static ClassicalBinaryOperator compoundAssignmentOperator(final String value) {
        return switch (value) {
            case "+" -> ClassicalBinaryOperator.ADD;
            case "-" -> ClassicalBinaryOperator.SUBTRACT;
            case "*" -> ClassicalBinaryOperator.MULTIPLY;
            case "/" -> ClassicalBinaryOperator.DIVIDE;
            case "^" -> ClassicalBinaryOperator.BITWISE_XOR;
            case "<<" -> ClassicalBinaryOperator.SHIFT_LEFT;
            case ">>" -> ClassicalBinaryOperator.SHIFT_RIGHT;
            default -> throw new IllegalArgumentException("Unsupported compound assignment operator.");
        };
    }

    private static void appendReturnBinding(
        final ParseContext context,
        final Statement statement,
        final String returnExpression,
        final ClassicalOperand target
    ) {
        final Operation[] operations = returnBindingOperations(
            context,
            statement,
            returnExpression,
            target
        );
        if (operations == null) {
            return;
        }
        for (int i = 0; i < operations.length; i++) {
            appendOperation(
                context,
                statement,
                operations[i]
            );
        }
    }

    private static void appendOperation(
        final ParseContext context,
        final Statement statement,
        final Operation operation
    ) {
        if (operation instanceof MeasureOperation measureOperation) {
            context.circuit().measureReference(
                measureOperation.qubitReference(),
                measureOperation.bit()
            );
        } else if (operation instanceof ClassicalAssignmentOperation assignmentOperation) {
            context.circuit().assign(assignmentOperation.assignment());
        } else {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "Returning subroutine lowering produced unsupported operation.",
                statement
            );
        }
    }

    private static Operation[] returnBindingOperations(
        final ParseContext context,
        final Statement statement,
        final String returnExpression,
        final ClassicalOperand target
    ) {
        final String expression = returnExpression.trim();
        final Matcher measureMatcher = Pattern.compile(
            "^measure\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(expression);
        if (measureMatcher.matches()) {
            return returnMeasureBindingOperations(
                context,
                statement,
                measureMatcher.group(1),
                target
            );
        }
        final ClassicalOperand source = parseClassicalOperand(
            context,
            statement,
            expression
        );
        if (source == null) {
            return null;
        }
        if (source.size() != target.size()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Subroutine return size does not match assignment target.",
                statement
            );
            return null;
        }
        final Operation[] operations = new Operation[source.size()];
        for (int i = 0; i < source.size(); i++) {
            operations[i] = new ClassicalAssignmentOperation(new ClassicalAssignment(
                ClassicalExpression.bit(target.bit(i)),
                ClassicalExpression.bit(source.bit(i))
            ));
        }
        return operations;
    }

    private static Operation[] returnMeasureBindingOperations(
        final ParseContext context,
        final Statement statement,
        final String qubitText,
        final ClassicalOperand target
    ) {
        final QuantumOperand qubits = parseQuantumOperand(
            context,
            statement,
            qubitText.trim()
        );
        if (qubits == null) {
            return null;
        }
        if (qubits.size() != target.size()) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Subroutine measured return size does not match assignment target.",
                statement
            );
            return null;
        }
        final Operation[] operations = new Operation[qubits.size()];
        for (int i = 0; i < qubits.size(); i++) {
            operations[i] = new MeasureOperation(
                qubits.reference(i),
                target.bit(i)
            );
        }
        return operations;
    }

    private static OperationBlock parseOperationBlock(
        final ParseContext context,
        final Statement parentStatement,
        final String bodyText
    ) {
        final OpenQasm3Ast ast = new OpenQasm3AstParser().parse(bodyText);
        context.addDiagnostics(new ArrayList<>(ast.diagnostics()));
        if (context.hasErrors()) {
            return null;
        }
        final ArrayList<Statement> statements = statementsFromAst(ast);
        final ArrayList<Operation> operations = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            final Operation[] parsedOperations = parseBlockOperations(
                context,
                statement.text().isBlank()
                    ? parentStatement
                    : statement
            );
            if (parsedOperations == null) {
                return null;
            }
            for (int j = 0; j < parsedOperations.length; j++) {
                operations.add(parsedOperations[j]);
            }
        }
        return OperationBlock.of(operations.toArray(new Operation[0]));
    }

    private static Operation[] parseBlockOperations(
        final ParseContext context,
        final Statement statement
    ) {
        final Matcher letMatcher = LET_PATTERN.matcher(statement.text());
        if (letMatcher.matches()) {
            parseCompileTimeLet(
                context,
                statement,
                letMatcher
            );
            return new Operation[0];
        }
        if (
            topLevelAssignmentIndex(statement.text()) < 0
            && tryParseSubroutineCall(
                context,
                statement
            )
        ) {
            return new Operation[0];
        }
        final Matcher blockArrayDeclarationMatcher = ARRAY_DECLARATION_PATTERN.matcher(statement.text());
        if (blockArrayDeclarationMatcher.matches()) {
            final ClassicalArrayDeclarationOperation operation = parseArrayDeclarationOperation(
                context,
                statement
            );
            return operation == null
                ? new Operation[0]
                : new Operation[] {operation};
        }
        if (isArrayDeclaration(statement.text())) {
            final ClassicalArrayDeclarationOperation operation = parseArrayDeclarationOperation(
                context,
                statement
            );
            return operation == null
                ? rejectUnsupportedBlockSource(
                    context,
                    statement
                )
                : new Operation[] {operation};
        }
        final Matcher blockClassicalDeclarationMatcher = CLASSICAL_DECLARATION_PATTERN.matcher(statement.text());
        if (
            blockClassicalDeclarationMatcher.matches()
            && shouldPreserveClassicalDeclaration(blockClassicalDeclarationMatcher)
        ) {
            final ClassicalType classicalType = parseClassicalType(
                blockClassicalDeclarationMatcher.group(2),
                blockClassicalDeclarationMatcher.group(3)
            );
            final ClassicalExpression initializer = blockClassicalDeclarationMatcher.group(5) == null
                ? null
                : parseClassicalExpression(
                    context,
                    statement,
                    blockClassicalDeclarationMatcher.group(5)
                );
            if (
                classicalType == null
                || (
                    blockClassicalDeclarationMatcher.group(5) != null
                    && initializer == null
                )
            ) {
                return null;
            }
            return new Operation[] {
                new ClassicalDeclarationOperation(
                    new ClassicalDeclaration(
                        blockClassicalDeclarationMatcher.group(4),
                        classicalType
                    ),
                    initializer
                )
            };
        }
        if (topLevelAssignmentIndex(statement.text()) < 0) {
            final CallableInvocationOperation invocationOperation = parseGenericCallableInvocationOperation(
                context,
                statement,
                statement.text(),
                null
            );
            if (invocationOperation != null) {
                return new Operation[] {invocationOperation};
            }
        }
        final Matcher blockCregBitstringMatcher = CREG_BITSTRING_PATTERN.matcher(statement.text());
        if (blockCregBitstringMatcher.matches()) {
            return parseClassicalRegisterWithBitstringOperation(
                context,
                statement,
                blockCregBitstringMatcher
            );
        }
        final Matcher blockCregMatcher = CREG_PATTERN.matcher(statement.text());
        if (blockCregMatcher.matches()) {
            parseClassicalRegister(
                context,
                statement,
                blockCregMatcher
            );
            return new Operation[0];
        }
        final Matcher earlyAssignmentMatcher = CLASSICAL_ASSIGNMENT_PATTERN.matcher(statement.text());
        if (
            earlyAssignmentMatcher.matches()
            && topLevelAssignmentIndex(statement.text()) >= 0
            && !isMeasureAssignment(statement.text())
        ) {
            final Operation[] returningCall = parseReturningSubroutineAssignmentOperation(
                context,
                statement,
                earlyAssignmentMatcher
            );
            if (returningCall != null) {
                return returningCall;
            }
        }
        final Operation compoundAssignment = parseRuntimeIntegerCompoundAssignment(
            context,
            statement
        );
        if (compoundAssignment != null) {
            return new Operation[] {compoundAssignment};
        }
        if (shouldPreserveSourceStatement(statement.text())) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        if (shouldPreserveSourceOperation(statement.text())) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        final Matcher ifBlockMatcher = IF_BLOCK_PATTERN.matcher(statement.text());
        if (
            ifBlockMatcher.matches()
            && shouldPreservePredicate(
                context,
                statement,
                ifBlockMatcher.group(1)
            )
        ) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        if (ifBlockMatcher.matches()) {
            return parseBlockConditionalOperation(
                context,
                statement,
                ifBlockMatcher
            );
        }
        final IfInlineParts ifInlineParts = parseInlineIf(statement.text());
        if (
            ifInlineParts != null
            && shouldPreservePredicate(
                context,
                statement,
                ifInlineParts.condition()
            )
        ) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        if (ifInlineParts != null) {
            return parseBlockInlineConditionalOperation(
                context,
                statement,
                ifInlineParts
            );
        }
        final Matcher forMatcher = FOR_PATTERN.matcher(statement.text());
        if (forMatcher.matches()) {
            if (
                !canParseForRange(
                    context,
                    forMatcher
                )
            ) {
                return parseBlockSymbolicForLoopOperation(
                    context,
                    statement,
                    forMatcher
                );
            }
            return parseBlockForLoopOperation(
                context,
                statement,
                forMatcher
            );
        }
        final Matcher whileMatcher = WHILE_PATTERN.matcher(statement.text());
        if (
            whileMatcher.matches()
            && shouldPreservePredicate(
                context,
                statement,
                whileMatcher.group(1)
            )
        ) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        if (whileMatcher.matches()) {
            return parseBlockWhileLoopOperation(
                context,
                statement,
                whileMatcher
            );
        }
        final Matcher delayMatcher = DELAY_PATTERN.matcher(statement.text());
        if (delayMatcher.matches()) {
            return parseBlockDelayOperation(
                context,
                statement,
                delayMatcher
            );
        }
        final Matcher boxMatcher = BOX_PATTERN.matcher(statement.text());
        if (boxMatcher.matches()) {
            return parseBlockTimingBoxOperation(
                context,
                statement,
                boxMatcher
            );
        }
        final Matcher measureAssignmentMatcher = MEASURE_ASSIGNMENT_PATTERN.matcher(statement.text());
        if (measureAssignmentMatcher.matches()) {
            return parseMeasureOperations(
                context,
                statement,
                measureAssignmentMatcher.group(2),
                measureAssignmentMatcher.group(1)
            );
        }
        final Matcher measureMatcher = MEASURE_PATTERN.matcher(statement.text());
        if (measureMatcher.matches()) {
            return parseMeasureOperations(
                context,
                statement,
                measureMatcher.group(1),
                measureMatcher.group(2)
            );
        }
        final Matcher resetMatcher = RESET_PATTERN.matcher(statement.text());
        if (resetMatcher.matches()) {
            return parseResetOperations(
                context,
                statement,
                resetMatcher.group(1)
            );
        }
        final Matcher barrierMatcher = BARRIER_PATTERN.matcher(statement.text());
        if (barrierMatcher.matches()) {
            return parseBarrierOperations(
                context,
                statement,
                barrierMatcher.group(1)
            );
        }
        final Matcher assignmentMatcher = CLASSICAL_ASSIGNMENT_PATTERN.matcher(statement.text());
        if (
            assignmentMatcher.matches()
            && topLevelAssignmentIndex(statement.text()) >= 0
            && !isMeasureAssignment(statement.text())
        ) {
            return parseClassicalAssignmentOperation(
                context,
                statement,
                assignmentMatcher
            );
        }
        return parseGateOperations(
            context,
            statement
        );
    }

    private static Operation[] parseBlockConditionalOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final Boolean staticResult = evaluateStaticBooleanPredicate(
            context,
            statement,
            matcher.group(1)
        );
        if (staticResult != null) {
            if (!staticResult.booleanValue()) {
                return new Operation[0];
            }
            final OperationBlock staticBlock = parseOperationBlock(
                context,
                statement,
                matcher.group(2)
            );
            if (staticBlock == null) {
                return null;
            }
            final Operation[] operations = new Operation[staticBlock.operationCount()];
            for (int i = 0; i < staticBlock.operationCount(); i++) {
                operations[i] = staticBlock.operation(i);
            }
            return operations;
        }
        final ClassicalPredicate predicate = parseClassicalPredicate(
            context,
            statement,
            matcher.group(1)
        );
        final BlockParts parts = splitConditionalBody(
            context,
            statement,
            matcher.group(2)
        );
        if (
            predicate == null
            || parts == null
        ) {
            return null;
        }
        final OperationBlock thenBlock = parseOperationBlock(
            context,
            statement,
            parts.thenText()
        );
        final OperationBlock elseBlock = parts.hasElseText()
            ? parseOperationBlock(
                context,
                statement,
                parts.elseText()
            )
            : null;
        if (
            thenBlock == null
            || (
                parts.hasElseText()
                && elseBlock == null
            )
        ) {
            return null;
        }
        return new Operation[] {
            new ConditionalBlockOperation(
                predicate,
                thenBlock,
                elseBlock
            )
        };
    }

    private static Operation[] parseBlockInlineConditionalOperation(
        final ParseContext context,
        final Statement statement,
        final IfInlineParts parts
    ) {
        final Boolean staticResult = evaluateStaticBooleanPredicate(
            context,
            statement,
            parts.condition()
        );
        if (staticResult != null) {
            if (!staticResult.booleanValue()) {
                return new Operation[0];
            }
            return parseBlockOperations(
                context,
                new Statement(
                    parts.body(),
                    statement.line(),
                    statement.column()
                )
            );
        }
        final ClassicalPredicate predicate = parseClassicalPredicate(
            context,
            statement,
            parts.condition()
        );
        final Operation[] operations = parseBlockOperations(
            context,
            new Statement(
                parts.body(),
                statement.line(),
                statement.column()
            )
        );
        if (
            predicate == null
            || operations == null
        ) {
            return null;
        }
        return new Operation[] {
            new ConditionalBlockOperation(
                predicate,
                OperationBlock.of(operations),
                null
            )
        };
    }

    private static Operation[] parseBlockForLoopOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final Long startInclusive = evaluateIntegerExpression(
            context,
            statement,
            matcher.group(2)
        );
        final Long step = matcher.group(3) == null
            ? Long.valueOf(1L)
            : evaluateIntegerExpression(
                context,
                statement,
                matcher.group(3)
            );
        final Long endInclusive = evaluateIntegerExpression(
            context,
            statement,
            matcher.group(4)
        );
        if (
            context.hasErrors()
            || startInclusive == null
            || step == null
            || endInclusive == null
            || step.longValue() == 0L
        ) {
            if (
                step != null
                && step.longValue() == 0L
            ) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "OpenQASM 3 for-loop step must not be zero.",
                    statement
                );
            }
            return null;
        }
        final OperationBlock body = parseForLoopBody(
            context,
            statement,
            matcher.group(1),
            matcher.group(5)
        );
        if (
            body == null
            || context.hasErrors()
        ) {
            return null;
        }
        return new Operation[] {
            new ForLoopOperation(
                matcher.group(1),
                startInclusive.longValue(),
                step.longValue(),
                endInclusive.longValue(),
                body
            )
        };
    }

    private static OperationBlock parseForLoopBody(
        final ParseContext context,
        final Statement statement,
        final String variableName,
        final String bodyText
    ) {
        final ClassicalExpression previous = context.integerExpression(variableName);
        context.addIntegerExpression(
            variableName,
            ClassicalExpression.variable(variableName)
        );
        try {
            return parseOperationBlock(
                context,
                statement,
                bodyText
            );
        } finally {
            context.restoreIntegerExpression(
                variableName,
                previous
            );
        }
    }

    private static Operation[] parseBlockSymbolicForLoopOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalExpression startInclusive = parseClassicalExpression(
            context,
            statement,
            matcher.group(2)
        );
        final ClassicalExpression step = matcher.group(3) == null
            ? ClassicalExpression.integer(1L)
            : parseClassicalExpression(
                context,
                statement,
                matcher.group(3)
            );
        final ClassicalExpression endInclusive = parseClassicalExpression(
            context,
            statement,
            matcher.group(4)
        );
        final OperationBlock body = parseForLoopBody(
            context,
            statement,
            matcher.group(1),
            matcher.group(5)
        );
        if (
            startInclusive == null
            || step == null
            || endInclusive == null
            || body == null
        ) {
            return null;
        }
        return new Operation[] {
            new SymbolicForLoopOperation(
                matcher.group(1),
                null,
                startInclusive,
                step,
                endInclusive,
                body
            )
        };
    }

    private static boolean shouldUnrollForLoop(
        final String bodyText,
        final String variableName
    ) {
        return Pattern.compile("\\b" + Pattern.quote(variableName) + "\\b").matcher(bodyText).find();
    }

    private static void unrollForLoop(
        final ParseContext context,
        final Statement statement,
        final String variableName,
        final long startInclusive,
        final long step,
        final long endInclusive,
        final String bodyText
    ) {
        final OpenQasm3Ast ast = new OpenQasm3AstParser().parse(bodyText);
        context.addDiagnostics(new ArrayList<>(ast.diagnostics()));
        if (context.hasErrors()) {
            return;
        }
        final ArrayList<Statement> statements = statementsFromAst(ast);
        final Long previous = context.integerConstant(variableName);
        for (
            long value = startInclusive;
            step > 0L ? value <= endInclusive : value >= endInclusive;
            value += step
        ) {
            context.addIntegerConstant(
                variableName,
                value
            );
            for (int i = 0; i < statements.size(); i++) {
                final Statement nestedStatement = statements.get(i);
                parseStatement(
                    context,
                    nestedStatement.text().isBlank()
                        ? statement
                        : nestedStatement
                );
                if (context.hasErrors()) {
                    context.restoreIntegerConstant(
                        variableName,
                        previous
                    );
                    return;
                }
            }
        }
        context.restoreIntegerConstant(
            variableName,
            previous
        );
    }

    private static Operation[] unrollBlockForLoop(
        final ParseContext context,
        final Statement statement,
        final String variableName,
        final long startInclusive,
        final long step,
        final long endInclusive,
        final String bodyText
    ) {
        final OpenQasm3Ast ast = new OpenQasm3AstParser().parse(bodyText);
        context.addDiagnostics(new ArrayList<>(ast.diagnostics()));
        if (context.hasErrors()) {
            return null;
        }
        final ArrayList<Statement> statements = statementsFromAst(ast);
        final ArrayList<Operation> operations = new ArrayList<>();
        final Long previous = context.integerConstant(variableName);
        for (
            long value = startInclusive;
            step > 0L ? value <= endInclusive : value >= endInclusive;
            value += step
        ) {
            context.addIntegerConstant(
                variableName,
                value
            );
            for (int i = 0; i < statements.size(); i++) {
                final Statement nestedStatement = statements.get(i);
                final Operation[] parsedOperations = parseBlockOperations(
                    context,
                    nestedStatement.text().isBlank()
                        ? statement
                        : nestedStatement
                );
                if (parsedOperations == null) {
                    context.restoreIntegerConstant(
                        variableName,
                        previous
                    );
                    return null;
                }
                for (int j = 0; j < parsedOperations.length; j++) {
                    operations.add(parsedOperations[j]);
                }
            }
        }
        context.restoreIntegerConstant(
            variableName,
            previous
        );
        return operations.toArray(new Operation[0]);
    }

    private static Operation[] parseBlockWhileLoopOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalPredicate predicate = parseClassicalPredicate(
            context,
            statement,
            matcher.group(1)
        );
        final OperationBlock body = parseOperationBlock(
            context,
            statement,
            matcher.group(2)
        );
        if (
            predicate == null
            || body == null
        ) {
            return null;
        }
        return new Operation[] {
            new WhileLoopOperation(
                predicate,
                body
            )
        };
    }

    private static Operation[] parseBlockDelayOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final DurationExpression duration = parseDuration(
            context,
            statement,
            matcher.group(1)
        );
        final ArrayList<QuantumReference> references = parseDelayReferences(
            context,
            statement,
            matcher.group(2)
        );
        if (
            duration == null
            || references == null
        ) {
            return null;
        }
        return new Operation[] {
            new DelayOperation(
                duration,
                references.toArray(new QuantumReference[0])
            )
        };
    }

    private static Operation[] parseBlockTimingBoxOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final DurationExpression duration = matcher.group(1) == null
            ? null
            : parseDuration(
                context,
                statement,
                matcher.group(1)
            );
        final OperationBlock body = parseOperationBlock(
            context,
            statement,
            matcher.group(2)
        );
        if (
            body == null
            || (
                matcher.group(1) != null
                && duration == null
            )
        ) {
            return null;
        }
        return new Operation[] {
            new TimingBoxOperation(
                duration,
                body
            )
        };
    }

    private static Operation[] parseMeasureOperations(
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
            return null;
        }
        if (qubits.size() != bits.size()) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        final Operation[] operations = new Operation[qubits.size()];
        for (int i = 0; i < qubits.size(); i++) {
            operations[i] = new MeasureOperation(
                qubits.reference(i),
                bits.bit(i)
            );
        }
        return operations;
    }

    private static Operation[] parseResetOperations(
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
            return null;
        }
        final Operation[] operations = new Operation[qubits.size()];
        for (int i = 0; i < qubits.size(); i++) {
            operations[i] = new ResetOperation(qubits.reference(i));
        }
        return operations;
    }

    private static Operation[] parseBarrierOperations(
        final ParseContext context,
        final Statement statement,
        final String qubitText
    ) {
        final ArrayList<Qubit> qubits = parseDelayQubits(
            context,
            statement,
            qubitText
        );
        if (qubits == null) {
            return null;
        }
        return new Operation[] {new BarrierOperation(qubits.toArray(new Qubit[0]))};
    }

    private static Operation[] parseClassicalAssignmentOperation(
        final ParseContext context,
        final Statement statement,
        final Matcher matcher
    ) {
        final ClassicalExpression target = parseClassicalAssignmentTarget(
            context,
            statement,
            matcher.group(1).trim()
        );
        final ClassicalExpression value = parseClassicalExpression(
            context,
            statement,
            matcher.group(2).trim()
        );
        if (
            target == null
            || value == null
        ) {
            return null;
        }
        return new Operation[] {
            new ClassicalAssignmentOperation(new ClassicalAssignment(
                target,
                value
            ))
        };
    }

    private static Operation[] parseGateOperations(
        final ParseContext context,
        final Statement statement
    ) {
        final GateCallParts callParts = parseGateCallParts(
            context,
            statement
        );
        if (callParts == null) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse OpenQASM 3 statement inside block: " + statement.text() + ".",
                statement
            );
            return null;
        }
        Gate gate = callParts.gate();
        if (gate == null) {
            final QuantumOperand[] unknownGateOperands = parseQuantumOperands(
                context,
                statement,
                callParts.operandsText()
            );
            if (unknownGateOperands == null) {
                return null;
            }
            gate = createOpaqueGateDefinition(
                context,
                statement,
                callParts.name(),
                parameterCount(
                    context,
                    statement,
                    callParts.parametersText()
                ),
                unknownGateOperands.length
            );
            if (gate == null) {
                return null;
            }
        }
        if (
            gate.parameterCount() > 0
            && (
                callParts.parametersText() == null
                || shouldPreserveParameterExpression(callParts.parametersText())
            )
        ) {
            return rejectUnsupportedBlockSource(
                context,
                statement
            );
        }
        final ParameterExpression[] parameters = parseParameters(
            context,
            statement,
            callParts.parametersText(),
            gate.parameterCount()
        );
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            callParts.operandsText()
        );
        if (
            parameters == null
            || operands == null
        ) {
            return null;
        }
        return gateOperations(
            context,
            statement,
            gate,
            parameters,
            operands
        );
    }

    private static Operation[] gateOperations(
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
            return null;
        }
        final int expandedSize = operands[0].size();
        for (int i = 1; i < operands.length; i++) {
            if (operands[i].size() != expandedSize) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Register-wide gate operands must have the same size.",
                    statement
                );
                return null;
            }
        }
        final Operation[] operations = new Operation[expandedSize];
        for (int i = 0; i < expandedSize; i++) {
            final QuantumReference[] references = new QuantumReference[operands.length];
            for (int j = 0; j < operands.length; j++) {
                references[j] = operands[j].reference(i);
            }
            operations[i] = GateOperation.parameterizedReferences(
                gate,
                parameters,
                references
            );
        }
        return operations;
    }

    private static GateCallParts parseGateCallParts(
        final ParseContext context,
        final Statement statement
    ) {
        String text = statement.text().trim();
        final ArrayList<GateModifier> modifiers = new ArrayList<>();
        while (true) {
            final int atIndex = topLevelOperatorIndex(
                text,
                "@"
            );
            if (atIndex < 0) {
                break;
            }
            final String modifierText = text.substring(
                0,
                atIndex
            ).trim();
            final GateModifier modifier = parseGateModifier(
                context,
                statement,
                modifierText
            );
            if (modifier == null) {
                break;
            }
            modifiers.add(modifier);
            text = text.substring(atIndex + 1).trim();
        }
        final Matcher matcher = GATE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        if (
            "u".equals(matcher.group(1))
            && matcher.group(2) == null
        ) {
            return new GateCallParts(
                matcher.group(1),
                StandardGate.U,
                "0,0,0",
                matcher.group(3)
            );
        }
        final Gate baseGate = resolveGate(
            context,
            matcher.group(1),
            matcher.group(2)
        );
        if (baseGate == null) {
            return new GateCallParts(
                matcher.group(1),
                null,
                matcher.group(2),
                matcher.group(3)
            );
        }
        final Gate gate = modifiers.isEmpty()
            ? baseGate
            : ModifiedGate.of(
                baseGate,
                modifiers
            );
        return new GateCallParts(
            matcher.group(1),
            gate,
            matcher.group(2),
            matcher.group(3)
        );
    }

    private static GateModifier parseGateModifier(
        final ParseContext context,
        final Statement statement,
        final String text
    ) {
        if ("inv".equalsIgnoreCase(text)) {
            return GateModifier.inverse();
        }
        if ("ctrl".equalsIgnoreCase(text)) {
            return GateModifier.controlled(1);
        }
        if ("negctrl".equalsIgnoreCase(text)) {
            return GateModifier.annotation("negctrl");
        }
        final Matcher powerMatcher = Pattern.compile("^pow\\((.+)\\)$", Pattern.CASE_INSENSITIVE)
            .matcher(text);
        if (powerMatcher.matches()) {
            final String expressionText = powerMatcher.group(1).trim();
            try {
                return GateModifier.power(Double.parseDouble(expressionText));
            } catch (final NumberFormatException exception) {
                final ParameterExpression expression = parseParameter(
                    context,
                    statement,
                    expressionText
                );
                if (expression == null) {
                    return null;
                }
                return GateModifier.power(expression);
            }
        }
        if (
            text.startsWith("@")
            && IDENTIFIER_PATTERN.matcher(text.substring(1)).matches()
        ) {
            return GateModifier.annotation(text.substring(1));
        }
        return null;
    }

    private static ClassicalPredicate parseClassicalPredicate(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = stripBalancedOuterParentheses(value.trim());
        final int orIndex = topLevelOperatorIndex(
            trimmed,
            "||"
        );
        if (orIndex >= 0) {
            final ClassicalPredicate left = parseClassicalPredicate(
                context,
                statement,
                trimmed.substring(
                    0,
                    orIndex
                )
            );
            final ClassicalPredicate right = parseClassicalPredicate(
                context,
                statement,
                trimmed.substring(orIndex + 2)
            );
            if (
                left == null
                || right == null
            ) {
                return null;
            }
            return ClassicalPredicate.or(
                left,
                right
            );
        }
        final int andIndex = topLevelOperatorIndex(
            trimmed,
            "&&"
        );
        if (andIndex >= 0) {
            final ClassicalPredicate left = parseClassicalPredicate(
                context,
                statement,
                trimmed.substring(
                    0,
                    andIndex
                )
            );
            final ClassicalPredicate right = parseClassicalPredicate(
                context,
                statement,
                trimmed.substring(andIndex + 2)
            );
            if (
                left == null
                || right == null
            ) {
                return null;
            }
            return ClassicalPredicate.and(
                left,
                right
            );
        }
        if (
            trimmed.startsWith("!")
            || trimmed.startsWith("~")
        ) {
            final ClassicalPredicate operand = parseClassicalPredicate(
                context,
                statement,
                trimmed.substring(1)
            );
            if (operand == null) {
                return null;
            }
            return ClassicalPredicate.not(operand);
        }
        final Matcher matcher = CLASSICAL_COMPARISON_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return ClassicalPredicate.compare(
                parseClassicalExpression(
                    context,
                    statement,
                    trimmed
                ),
                ClassicalComparisonOperator.NOT_EQUAL,
                ClassicalExpression.integer(0L)
            );
        }
        final ClassicalExpression left = parseClassicalExpression(
            context,
            statement,
            matcher.group(1).trim()
        );
        final ClassicalExpression right = parseClassicalExpression(
            context,
            statement,
            matcher.group(3).trim()
        );
        if (
            left == null
            || right == null
        ) {
            return null;
        }
        return ClassicalPredicate.compare(
            left,
            comparisonOperator(matcher.group(2)),
            right
        );
    }

    private static String stripBalancedOuterParentheses(final String value) {
        String result = value;
        while (
            result.length() >= 2
            && result.charAt(0) == '('
            && result.charAt(result.length() - 1) == ')'
            && enclosesWholeExpression(result)
        ) {
            result = result.substring(
                1,
                result.length() - 1
            ).trim();
        }
        return result;
    }

    private static boolean enclosesWholeExpression(final String value) {
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            final char current = value.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (
                    depth == 0
                    && i < value.length() - 1
                ) {
                    return false;
                }
            }
            if (depth < 0) {
                return false;
            }
        }
        return depth == 0;
    }

    private static int topLevelOperatorIndex(
        final String text,
        final String operator
    ) {
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int i = 0; i <= text.length() - operator.length(); i++) {
            final char current = text.charAt(i);
            if (current == '(') {
                parenthesisDepth++;
            } else if (current == ')') {
                parenthesisDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            }
            if (
                parenthesisDepth == 0
                && bracketDepth == 0
                && braceDepth == 0
                && text.startsWith(
                    operator,
                    i
                )
            ) {
                return i;
            }
        }
        return -1;
    }

    private static Boolean evaluateStaticBooleanPredicate(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = stripBalancedOuterParentheses(value.trim());
        if (
            trimmed.startsWith("!")
            || trimmed.startsWith("~")
            || topLevelOperatorIndex(
                trimmed,
                "||"
            ) >= 0
            || topLevelOperatorIndex(
                trimmed,
                "&&"
            ) >= 0
        ) {
            return null;
        }
        final Matcher booleanCastMatcher = BOOLEAN_CAST_PATTERN.matcher(trimmed);
        if (booleanCastMatcher.matches()) {
            final Long evaluated = evaluateStaticIntegerExpression(
                context,
                statement,
                booleanCastMatcher.group(1).trim()
            );
            return evaluated == null ? null : Boolean.valueOf(evaluated.longValue() != 0L);
        }
        final Matcher comparisonMatcher = CLASSICAL_COMPARISON_PATTERN.matcher(trimmed);
        if (!comparisonMatcher.matches()) {
            return null;
        }
        final Long left = evaluateStaticIntegerExpression(
            context,
            statement,
            comparisonMatcher.group(1).trim()
        );
        final Long right = evaluateStaticIntegerExpression(
            context,
            statement,
            comparisonMatcher.group(3).trim()
        );
        if (
            left == null
            || right == null
        ) {
            return null;
        }
        return Boolean.valueOf(compareStaticIntegers(
            left.longValue(),
            comparisonMatcher.group(2),
            right.longValue()
        ));
    }

    private static boolean compareStaticIntegers(
        final long left,
        final String operator,
        final long right
    ) {
        if ("==".equals(operator)) {
            return left == right;
        }
        if ("!=".equals(operator)) {
            return left != right;
        }
        if ("<".equals(operator)) {
            return left < right;
        }
        if ("<=".equals(operator)) {
            return left <= right;
        }
        if (">".equals(operator)) {
            return left > right;
        }
        return left >= right;
    }

    private static Long evaluateStaticIntegerExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = value.trim();
        final Matcher castMatcher = CLASSICAL_CAST_PATTERN.matcher(trimmed);
        if (castMatcher.matches()) {
            return evaluateStaticIntegerExpression(
                context,
                statement,
                castMatcher.group(1).trim()
            );
        }
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(trimmed);
        if (indexedMatcher.matches()) {
            final long[] integerArray = context.integerArray(indexedMatcher.group(1));
            if (integerArray != null) {
                final Long index = evaluateStaticIntegerExpression(
                    context,
                    statement,
                    indexedMatcher.group(2)
                );
                if (
                    index == null
                    || index.longValue() < 0L
                    || index.longValue() >= integerArray.length
                ) {
                    return null;
                }
                return Long.valueOf(integerArray[index.intValue()]);
            }
            final Long integerConstant = context.integerConstant(indexedMatcher.group(1));
            if (integerConstant == null) {
                return null;
            }
            final Long index = evaluateStaticIntegerExpression(
                context,
                statement,
                indexedMatcher.group(2)
            );
            if (
                index == null
                || index.longValue() < 0L
                || index.longValue() >= Long.SIZE
            ) {
                return null;
            }
            return Long.valueOf((integerConstant.longValue() >> index.intValue()) & 1L);
        }
        if (context.integerConstant(trimmed) != null) {
            return context.integerConstant(trimmed);
        }
        if (trimmed.matches("\"[01]+\"")) {
            return Long.valueOf(Long.parseLong(
                trimmed.substring(
                    1,
                    trimmed.length() - 1
                ),
                2
            ));
        }
        if (
            trimmed.startsWith("0b")
            || trimmed.startsWith("0B")
        ) {
            try {
                return Long.valueOf(Long.parseLong(
                    trimmed.substring(2),
                    2
                ));
            } catch (final NumberFormatException exception) {
                return null;
            }
        }
        if (
            trimmed.matches("-?[0-9+*/()\\s]+")
            || canEvaluateIntegerExpression(
                context,
                trimmed
            )
        ) {
            return evaluateIntegerExpression(
                context,
                statement,
                trimmed
            );
        }
        return null;
    }

    private static boolean canEvaluateIntegerExpression(
        final ParseContext context,
        final String value
    ) {
        int position = 0;
        boolean sawIdentifier = false;
        while (position < value.length()) {
            final char current = value.charAt(position);
            if (
                Character.isLetter(current)
                || current == '_'
            ) {
                final int start = position;
                position++;
                while (
                    position < value.length()
                    && (
                        Character.isLetterOrDigit(value.charAt(position))
                        || value.charAt(position) == '_'
                    )
                ) {
                    position++;
                }
                sawIdentifier = true;
                if (context.integerConstant(value.substring(
                    start,
                    position
                )) == null) {
                    return false;
                }
            } else {
                position++;
            }
        }
        return sawIdentifier;
    }

    private static boolean canParseRegisterSize(
        final ParseContext context,
        final String value
    ) {
        return canParseIntegerExpression(
            context,
            value
        );
    }

    private static boolean canParseForRange(
        final ParseContext context,
        final Matcher matcher
    ) {
        return canParseIntegerExpression(
            context,
            matcher.group(2)
        )
            && (
                matcher.group(3) == null
                || canParseIntegerExpression(
                    context,
                    matcher.group(3)
                )
            )
            && canParseIntegerExpression(
                context,
                matcher.group(4)
            );
    }

    private static boolean canParseIntegerExpression(
        final ParseContext context,
        final String value
    ) {
        return value.trim().matches("-?[0-9+*/()\\s]+")
            || context.integerConstant(value.trim()) != null
            || canEvaluateIntegerExpression(
                context,
                value
            );
    }

    private static ClassicalComparisonOperator comparisonOperator(final String value) {
        if ("==".equals(value)) {
            return ClassicalComparisonOperator.EQUAL;
        }
        if ("!=".equals(value)) {
            return ClassicalComparisonOperator.NOT_EQUAL;
        }
        if ("<".equals(value)) {
            return ClassicalComparisonOperator.LESS_THAN;
        }
        if ("<=".equals(value)) {
            return ClassicalComparisonOperator.LESS_THAN_OR_EQUAL;
        }
        if (">".equals(value)) {
            return ClassicalComparisonOperator.GREATER_THAN;
        }
        return ClassicalComparisonOperator.GREATER_THAN_OR_EQUAL;
    }

    private static ClassicalExpression parseClassicalExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = value.trim();
        final Matcher castMatcher = CLASSICAL_CAST_PATTERN.matcher(trimmed);
        if (castMatcher.matches()) {
            return parseClassicalExpression(
                context,
                statement,
                castMatcher.group(1).trim()
            );
        }
        if (trimmed.matches("\"[01]+\"")) {
            return ClassicalExpression.integer(Long.parseLong(
                trimmed.substring(
                    1,
                    trimmed.length() - 1
                ),
                2
            ));
        }
        if (
            trimmed.length() >= 2
            && trimmed.startsWith("\"")
            && trimmed.endsWith("\"")
        ) {
            return ClassicalExpression.symbolicReference(trimmed);
        }
        if (trimmed.matches("-?\\d+")) {
            return ClassicalExpression.integer(parseSignedLong(
                context,
                statement,
                trimmed
            ));
        }
        if (
            trimmed.startsWith("0b")
            || trimmed.startsWith("0B")
        ) {
            try {
                return ClassicalExpression.integer(Long.parseLong(
                    trimmed.substring(2),
                    2
                ));
            } catch (final NumberFormatException exception) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Cannot parse classical expression: " + value + ".",
                    statement
                );
                return null;
            }
        }
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(trimmed);
        if (indexedMatcher.matches()) {
            final long[] integerArray = context.integerArray(indexedMatcher.group(1));
            if (integerArray != null) {
                if (
                    indexedMatcher.group(2).contains(",")
                    || !canParseIntegerExpression(
                        context,
                        indexedMatcher.group(2)
                    )
                ) {
                    return ClassicalExpression.symbolicReference(trimmed);
                }
                final int index = parseNonNegativeIntegerExpression(
                    context,
                    statement,
                    indexedMatcher.group(2)
                );
                if (
                    index >= 0
                    && index < integerArray.length
                ) {
                    return ClassicalExpression.integer(integerArray[index]);
                }
            }
            final ClassicalRegister register = context.classicalRegister(indexedMatcher.group(1));
            if (
                register == null
                || indexedMatcher.group(2).contains(",")
            ) {
                final Long integerConstant = context.integerConstant(indexedMatcher.group(1));
                if (
                    integerConstant == null
                    || indexedMatcher.group(2).contains(",")
                ) {
                    return ClassicalExpression.symbolicReference(trimmed);
                }
            }
            final int index = parseNonNegativeIntegerExpression(
                context,
                statement,
                indexedMatcher.group(2)
            );
            if (register == null) {
                final Long integerConstant = context.integerConstant(indexedMatcher.group(1));
                if (
                    integerConstant != null
                    && index >= 0
                    && index < Long.SIZE
                ) {
                    return ClassicalExpression.integer((integerConstant.longValue() >> index) & 1L);
                }
                return ClassicalExpression.symbolicReference(trimmed);
            }
            if (index < 0) {
                return ClassicalExpression.symbolicReference(trimmed);
            }
            if (index >= register.size()) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Classical bit index is outside of classical register bounds.",
                    statement
                );
                return null;
            }
            return ClassicalExpression.bit(register.get(index));
        }
        final ClassicalRegister register = context.classicalRegister(trimmed);
        if (register != null) {
            return ClassicalExpression.register(register);
        }
        final ClassicalExpression integerAlias = context.integerExpression(trimmed);
        if (integerAlias != null) {
            return integerAlias;
        }
        if (trimmed.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return ClassicalExpression.variable(trimmed);
        }
        final ClassicalExpression runtimeExpression = parseRuntimeBinaryIntegerExpression(
            context,
            statement,
            trimmed
        );
        if (runtimeExpression != null) {
            return runtimeExpression;
        }
        final ClassicalExpression callExpression = parseClassicalCallExpression(
            context,
            statement,
            trimmed
        );
        if (callExpression != null) {
            return callExpression;
        }
        if (
            CLASSICAL_COMPARISON_PATTERN.matcher(trimmed).matches()
            || trimmed.contains("&&")
            || trimmed.contains("||")
            || trimmed.contains("[")
            || trimmed.contains("]")
            || trimmed.contains("{")
            || trimmed.contains("}")
            || trimmed.contains(".")
            || trimmed.contains("+")
            || trimmed.contains("-")
            || trimmed.contains("*")
            || trimmed.contains("/")
        ) {
            return ClassicalExpression.symbolicReference(trimmed);
        }
        context.addError(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Cannot parse classical expression: " + value + ".",
            statement
        );
        return null;
    }

    private static ClassicalExpression parseClassicalAssignmentTarget(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = value.trim();
        if (
            trimmed.contains("[")
            || trimmed.contains("]")
            || trimmed.contains("{")
            || trimmed.contains("}")
            || trimmed.contains(".")
        ) {
            return ClassicalExpression.symbolicReference(trimmed);
        }
        return parseClassicalExpression(
            context,
            statement,
            trimmed
        );
    }

    private static ClassicalExpression parseClassicalCallExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final int open = value.indexOf('(');
        if (
            open <= 0
            || !value.endsWith(")")
        ) {
            return null;
        }
        final String name = value.substring(
            0,
            open
        ).trim();
        if (!IDENTIFIER_PATTERN.matcher(name).matches()) {
            return null;
        }
        final String argumentsText = value.substring(
            open + 1,
            value.length() - 1
        );
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            argumentsText,
            "classical call arguments"
        );
        if (parts == null) {
            return null;
        }
        final ArrayList<ClassicalExpression> arguments = new ArrayList<>();
        for (int i = 0; i < parts.parts().size(); i++) {
            final String part = parts.parts().get(i).trim();
            if (part.isBlank()) {
                continue;
            }
            final ClassicalExpression argument = parseClassicalExpression(
                context,
                statement,
                part
            );
            if (argument == null) {
                return null;
            }
            arguments.add(argument);
        }
        return ClassicalExpression.call(
            name,
            arguments
        );
    }

    private static DurationExpression parseDuration(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = value.trim();
        final Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            try {
                return DurationExpression.duration(
                    Double.parseDouble(matcher.group(1)),
                    DurationUnit.fromSymbol(matcher.group(2).toLowerCase())
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
        if (IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
            final DurationExpression knownDuration = context.durationConstant(trimmed);
            if (knownDuration != null) {
                return knownDuration;
            }
            return DurationExpression.stretch(trimmed);
        }
        return DurationExpression.expression(trimmed);
    }

    private static ArrayList<Qubit> parseDelayQubits(
        final ParseContext context,
        final Statement statement,
        final String qubitText
    ) {
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            qubitText
        );
        if (operands == null) {
            return null;
        }
        final ArrayList<Qubit> qubits = new ArrayList<>();
        for (int i = 0; i < operands.length; i++) {
            for (int j = 0; j < operands[i].size(); j++) {
                qubits.add(operands[i].qubit(j));
            }
        }
        return qubits;
    }

    private static ArrayList<QuantumReference> parseDelayReferences(
        final ParseContext context,
        final Statement statement,
        final String qubitText
    ) {
        if (
            qubitText == null
            || qubitText.isBlank()
        ) {
            return new ArrayList<>();
        }
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            qubitText
        );
        if (operands == null) {
            return null;
        }
        final ArrayList<QuantumReference> references = new ArrayList<>();
        for (int i = 0; i < operands.length; i++) {
            for (int j = 0; j < operands[i].size(); j++) {
                references.add(operands[i].reference(j));
            }
        }
        return references;
    }

    private static BlockParts splitConditionalBody(
        final ParseContext context,
        final Statement statement,
        final String bodyText
    ) {
        final String marker = "} else {";
        int depth = 0;
        for (int i = 0; i <= bodyText.length() - marker.length(); i++) {
            final char current = bodyText.charAt(i);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                if (
                    depth == 0
                    && bodyText.regionMatches(
                        true,
                        i,
                        marker,
                        0,
                        marker.length()
                    )
                ) {
                    return new BlockParts(
                        bodyText.substring(
                            0,
                            i
                        ),
                        bodyText.substring(i + marker.length())
                    );
                }
                depth--;
            }
        }
        if (bodyText.contains(" else ")) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse OpenQASM 3 else block.",
                statement
            );
            return null;
        }
        return new BlockParts(
            bodyText,
            null
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
        final Matcher measureAssignmentMatcher = MEASURE_ASSIGNMENT_PATTERN.matcher(nestedStatement);
        if (measureAssignmentMatcher.matches()) {
            parseControlledMeasure(
                context,
                statement,
                predicate,
                measureAssignmentMatcher.group(2),
                measureAssignmentMatcher.group(1)
            );
            return;
        }
        final Matcher measureMatcher = MEASURE_PATTERN.matcher(nestedStatement);
        if (measureMatcher.matches()) {
            parseControlledMeasure(
                context,
                statement,
                predicate,
                measureMatcher.group(1),
                measureMatcher.group(2)
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
                    new ResetOperation(operand.reference(i))
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
        final String quantumOperand,
        final String classicalOperand
    ) {
        final QuantumOperand qubits = parseQuantumOperand(
            context,
            statement,
            quantumOperand.trim()
        );
        final ClassicalOperand bits = parseClassicalOperand(
            context,
            statement,
            classicalOperand.trim()
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
                    qubits.reference(i),
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
                "OpenQASM 3 import supports conditional gate/measure/reset/barrier operations only.",
                statement
            );
            return;
        }
        if (isOpenQasmOperationKeyword(matcher.group(1))) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "OpenQASM 3 import supports conditional gate/measure/reset/barrier operations only.",
                statement
            );
            return;
        }
        final Gate gate = resolveGate(
            context,
            matcher.group(1),
            matcher.group(2)
        );
        if (gate == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "OpenQASM 3 import does not support conditional gate: " + matcher.group(1) + ".",
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
            final QuantumReference[] references = new QuantumReference[operands.length];
            for (int j = 0; j < operands.length; j++) {
                references[j] = operands[j].reference(i);
            }
            context.circuit().classicallyControlled(
                predicate,
                GateOperation.parameterizedReferences(
                    gate,
                    parameters,
                    references
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
        final GateCallParts callParts = parseGateCallParts(
            context,
            statement
        );
        if (callParts == null) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Cannot parse OpenQASM 3 statement: " + statement.text() + ".",
                statement
            );
            return;
        }
        Gate gate = callParts.gate();
        if (gate == null) {
            final QuantumOperand[] unknownGateOperands = parseQuantumOperands(
                context,
                statement,
                callParts.operandsText()
            );
            if (unknownGateOperands == null) {
                return;
            }
            gate = createOpaqueGateDefinition(
                context,
                statement,
                callParts.name(),
                parameterCount(
                    context,
                    statement,
                    callParts.parametersText()
                ),
                unknownGateOperands.length
            );
            if (gate == null) {
                return;
            }
        }
        if (
            gate.parameterCount() > 0
            && (
                callParts.parametersText() == null
                || shouldPreserveParameterExpression(callParts.parametersText())
            )
        ) {
            rejectUnsupportedSource(
                context,
                statement,
                unsupportedStatementKind(statement.text())
            );
            return;
        }
        final ParameterExpression[] parameters = parseParameters(
            context,
            statement,
            callParts.parametersText(),
            gate.parameterCount()
        );
        if (parameters == null) {
            return;
        }
        final QuantumOperand[] operands = parseQuantumOperands(
            context,
            statement,
            callParts.operandsText()
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

    private static int parameterCount(
        final ParseContext context,
        final Statement statement,
        final String parameterText
    ) {
        if (
            parameterText == null
            || parameterText.isBlank()
        ) {
            return 0;
        }
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            parameterText,
            "parameter list"
        );
        return parts == null
            ? -1
            : parts.parts().size();
    }

    private static GateDefinition createOpaqueGateDefinition(
        final ParseContext context,
        final Statement statement,
        final String name,
        final int parameterCount,
        final int arity
    ) {
        if (parameterCount < 0) {
            return null;
        }
        final GateDefinition existing = context.gateDefinition(name);
        if (existing != null) {
            return existing;
        }
        try {
            final GateDefinition definition = GateDefinition.opaque(
                name,
                generatedNames(
                    "p",
                    parameterCount
                ),
                generatedNames(
                    "q",
                    arity
                )
            );
            context.program().addGateDefinition(definition);
            context.addGateDefinition(definition);
            return definition;
        } catch (final IllegalArgumentException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                exception.getMessage(),
                statement
            );
            return null;
        }
    }

    private static List<String> generatedNames(
        final String prefix,
        final int count
    ) {
        final ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add(prefix + i);
        }
        return names;
    }

    private static ParameterExpression parseParameter(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        if (containsParameterFunction(value)) {
            final Double evaluated = new ConstantParameterExpressionParser(value).parse();
            if (evaluated != null) {
                return ParameterExpression.of(evaluated.doubleValue());
            }
        }
        final ParameterParser parser = new ParameterParser(value);
        final ParameterExpression expression = parser.parse();
        if (expression == null) {
            context.addError(
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "Cannot parse OpenQASM 3 parameter expression: " + value + ".",
                statement
            );
            return null;
        }
        return expression;
    }

    private static boolean shouldPreserveParameterExpression(final String value) {
        if (value == null) {
            return false;
        }
        final String trimmed = value.trim();
        return containsNonAscii(trimmed)
            || trimmed.contains("?")
            || (
                containsParameterFunction(trimmed)
                && new ConstantParameterExpressionParser(trimmed).parse() == null
            );
    }

    private static boolean containsParameterFunction(final String value) {
        return Pattern.compile(
            "(?<![A-Za-z0-9_])(?:sin|cos|tan|exp|ln|sqrt|arccos|arcsin|arctan)\\s*\\(",
            Pattern.CASE_INSENSITIVE
        ).matcher(value).find();
    }

    private static boolean containsNonAscii(final String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }

    private static ListParts parseCommaAwareParts(
        final ParseContext context,
        final Statement statement,
        final String text,
        final String subject
    ) {
        final ArrayList<String> parts = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            final char current = text.charAt(i);
            if (current == '(') {
                parenthesisDepth++;
            } else if (current == ')') {
                parenthesisDepth--;
                if (parenthesisDepth < 0) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 3 " + subject + " has an unexpected closing parenthesis.",
                        statement
                    );
                    return null;
                }
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
                if (bracketDepth < 0) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 3 " + subject + " has an unexpected closing bracket.",
                        statement
                    );
                    return null;
                }
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
                if (braceDepth < 0) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 3 " + subject + " has an unexpected closing brace.",
                        statement
                    );
                    return null;
                }
            }
            if (
                current == ','
                && parenthesisDepth == 0
                && bracketDepth == 0
                && braceDepth == 0
            ) {
                parts.add(builder.toString());
                builder.setLength(0);
            } else {
                builder.append(current);
            }
        }
        if (
            parenthesisDepth != 0
            || bracketDepth != 0
            || braceDepth != 0
        ) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 3 " + subject + " has an unclosed delimiter.",
                statement
            );
            return null;
        }
        parts.add(builder.toString());
        return new ListParts(parts);
    }

    private static int matchingDelimiter(
        final String text,
        final int start,
        final char open,
        final char close
    ) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            final char current = text.charAt(i);
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int topLevelAssignmentIndex(final String text) {
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            final char current = text.charAt(i);
            if (current == '(') {
                parenthesisDepth++;
            } else if (current == ')') {
                parenthesisDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            } else if (
                current == '='
                && parenthesisDepth == 0
                && bracketDepth == 0
                && braceDepth == 0
            ) {
                return i;
            }
        }
        return -1;
    }

    private static QuantumOperand[] parseQuantumOperands(
        final ParseContext context,
        final Statement statement,
        final String operandText
    ) {
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            operandText,
            "qubit operands"
        );
        if (parts == null) {
            return null;
        }
        final QuantumOperand[] operands = new QuantumOperand[parts.parts().size()];
        for (int i = 0; i < parts.parts().size(); i++) {
            operands[i] = parseQuantumOperand(
                context,
                statement,
                parts.parts().get(i).trim()
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
            final QuantumReference[] references = new QuantumReference[operands.length];
            for (int j = 0; j < operands.length; j++) {
                references[j] = operands[j].reference(i);
            }
            appendGateOperation(
                context.circuit(),
                gate,
                parameters,
                references
            );
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
        return resolveGate(
            context,
            name,
            null
        );
    }

    private static Gate resolveGate(
        final ParseContext context,
        final String name,
        final String parametersText
    ) {
        if (
            "u".equals(name)
            && parametersText == null
        ) {
            return context.gateDefinition(name);
        }
        final Gate standardGate = OpenQasm3GateMapper.fromOpenQasmName(name);
        if (standardGate != null) {
            return standardGate;
        }
        return context.gateDefinition(name);
    }

    private static QuantumOperand parseQuantumOperand(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final QuantumOperand directAlias = context.quantumAlias(value.trim());
        if (directAlias != null) {
            return directAlias;
        }
        final Matcher physicalMatcher = PHYSICAL_QUBIT_PATTERN.matcher(value);
        if (physicalMatcher.matches()) {
            final int index = parseNonNegativeInteger(
                context,
                statement,
                physicalMatcher.group(1)
            );
            if (index < 0) {
                return null;
            }
            return QuantumOperand.single(QuantumReference.hardwareQubit(index));
        }
        final Matcher bracedIndexedMatcher = BRACED_INDEXED_ARGUMENT_PATTERN.matcher(value);
        if (bracedIndexedMatcher.matches()) {
            return parseBracedQuantumOperand(
                context,
                statement,
                bracedIndexedMatcher.group(1),
                bracedIndexedMatcher.group(2)
            );
        }
        final Matcher slicedMatcher = SLICED_ARGUMENT_PATTERN.matcher(value);
        if (slicedMatcher.matches()) {
            final QuantumOperand alias = context.quantumAlias(slicedMatcher.group(1));
            final QuantumRegister register = context.quantumRegister(slicedMatcher.group(1));
            if (
                !canParseIntegerExpression(
                    context,
                    slicedMatcher.group(2)
                )
                || !canParseIntegerExpression(
                    context,
                    slicedMatcher.group(3)
                )
            ) {
                final QuantumOperand symbolicSlice = parseSymbolicTwoElementQuantumSlice(
                    context,
                    slicedMatcher.group(1),
                    slicedMatcher.group(2),
                    slicedMatcher.group(3),
                    alias,
                    register
                );
                if (symbolicSlice != null) {
                    return symbolicSlice;
                }
            }
            final int start = parseNonNegativeIntegerExpression(
                context,
                statement,
                slicedMatcher.group(2)
            );
            final int end = parseNonNegativeIntegerExpression(
                context,
                statement,
                slicedMatcher.group(3)
            );
            if (
                start < 0
                || end < 0
            ) {
                return null;
            }
            if (alias != null) {
                return sliceQuantumAlias(
                    context,
                    statement,
                    alias,
                    start,
                    end
                );
            }
            if (register == null) {
                final QuantumRegister implicitRegister = context.ensureQuantumRegister(
                    slicedMatcher.group(1),
                    Math.max(
                        end + 1,
                        64
                    )
                );
                return QuantumOperand.slice(
                    implicitRegister,
                    start,
                    end
                );
            }
            if (
                start > end
                || end >= register.size()
            ) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Qubit slice is outside of quantum register bounds.",
                    statement
                );
                return null;
            }
            return QuantumOperand.slice(
                register,
                start,
                end
            );
        }
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(value);
        if (indexedMatcher.matches()) {
            final QuantumOperand alias = context.quantumAlias(indexedMatcher.group(1));
            final QuantumRegister register = context.quantumRegister(indexedMatcher.group(1));
            final ClassicalExpression aliasedIndex = context.integerExpression(indexedMatcher.group(2).trim());
            if (aliasedIndex != null) {
                if (alias != null) {
                    final QuantumReference aliasReference = alias.dynamicReference(aliasedIndex);
                    if (aliasReference != null) {
                        return QuantumOperand.single(aliasReference);
                    }
                }
                final QuantumRegister dynamicRegister = register == null
                    ? context.ensureQuantumRegister(
                        indexedMatcher.group(1),
                        64
                    )
                    : register;
                return QuantumOperand.single(QuantumReference.dynamicIndex(
                    dynamicRegister,
                    aliasedIndex
                ));
            }
            if (!canParseIntegerExpression(
                context,
                indexedMatcher.group(2)
            )) {
                final ClassicalExpression indexExpression = ClassicalExpression.symbolicReference(indexedMatcher.group(2));
                if (alias != null) {
                    final QuantumReference aliasReference = alias.dynamicReference(indexExpression);
                    if (aliasReference != null) {
                        return QuantumOperand.single(aliasReference);
                    }
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "Dynamic qubit alias index requires a contiguous static quantum alias.",
                        statement
                    );
                    return null;
                }
                final QuantumRegister dynamicRegister = register == null
                    ? context.ensureQuantumRegister(
                        indexedMatcher.group(1),
                        64
                    )
                    : register;
                return QuantumOperand.single(QuantumReference.dynamicIndex(
                    dynamicRegister,
                    indexExpression
                ));
            }
            final int index = parseNonNegativeIntegerExpression(
                context,
                statement,
                indexedMatcher.group(2)
            );
            if (alias != null) {
                if (index < 0) {
                    return null;
                }
                if (index >= alias.size()) {
                    final QuantumReference aliasReference = alias.dynamicReference(ClassicalExpression.integer(index));
                    if (aliasReference != null) {
                        return QuantumOperand.single(aliasReference);
                    }
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "Qubit alias index is outside of bounds.",
                        statement
                    );
                    return null;
                }
                return QuantumOperand.single(alias.reference(index));
            }
            if (register == null) {
                return QuantumOperand.single(context.ensureQuantumRegister(
                    indexedMatcher.group(1),
                    Math.max(
                        index + 1,
                        64
                    )
                ).get(index));
            }
            if (index < 0) {
                return null;
            }
            if (index >= register.size()) {
                final String indexText = indexedMatcher.group(2).trim();
                if (!indexText.matches("\\d+")) {
                    return QuantumOperand.single(QuantumReference.dynamicIndex(
                        register,
                        ClassicalExpression.symbolicReference(indexText)
                    ));
                }
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
                return QuantumOperand.register(context.ensureQuantumRegister(
                    registerMatcher.group(1),
                    64
                ));
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

    private static QuantumOperand parseSymbolicTwoElementQuantumSlice(
        final ParseContext context,
        final String name,
        final String startText,
        final String endText,
        final QuantumOperand alias,
        final QuantumRegister register
    ) {
        final String start = startText.trim();
        final String end = endText.trim();
        if (!isPlusOneExpression(
            start,
            end
        )) {
            return null;
        }
        final ClassicalExpression startExpression = ClassicalExpression.symbolicReference(start);
        final ClassicalExpression endExpression = ClassicalExpression.symbolicReference(end);
        if (alias != null) {
            final QuantumReference first = alias.dynamicReference(startExpression);
            final QuantumReference second = alias.dynamicReference(endExpression);
            if (
                first == null
                || second == null
            ) {
                return null;
            }
            return new QuantumOperand(new QuantumReference[] {
                first,
                second
            });
        }
        final QuantumRegister dynamicRegister = register == null
            ? context.ensureQuantumRegister(
                name,
                64
            )
            : register;
        return new QuantumOperand(new QuantumReference[] {
            QuantumReference.dynamicIndex(
                dynamicRegister,
                startExpression
            ),
            QuantumReference.dynamicIndex(
                dynamicRegister,
                endExpression
            )
        });
    }

    private static boolean isPlusOneExpression(
        final String start,
        final String end
    ) {
        final String compactStart = start.replaceAll(
            "\\s+",
            ""
        );
        final String compactEnd = end.replaceAll(
            "\\s+",
            ""
        );
        return compactEnd.equals(compactStart + "+1");
    }

    private static ClassicalExpression parseRuntimeIntegerExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final String trimmed = value.trim();
        final Matcher castMatcher = CLASSICAL_CAST_PATTERN.matcher(trimmed);
        if (castMatcher.matches()) {
            return parseRuntimeIntegerExpression(
                context,
                statement,
                castMatcher.group(1).trim()
            );
        }
        if (
            trimmed.startsWith("(")
            && trimmed.endsWith(")")
        ) {
            return parseRuntimeIntegerExpression(
                context,
                statement,
                trimmed.substring(
                    1,
                    trimmed.length() - 1
                )
            );
        }
        final ClassicalExpression alias = context.integerExpression(trimmed);
        if (alias != null) {
            return alias;
        }
        final Long integerConstant = context.integerConstant(trimmed);
        if (integerConstant != null) {
            return ClassicalExpression.integer(integerConstant.longValue());
        }
        if (trimmed.matches("-?\\d+")) {
            return ClassicalExpression.integer(parseSignedLong(
                context,
                statement,
                trimmed
            ));
        }
        if (trimmed.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return ClassicalExpression.variable(trimmed);
        }
        final ClassicalExpression expression = parseRuntimeBinaryIntegerExpression(
            context,
            statement,
            trimmed
        );
        if (expression != null) {
            return expression;
        }
        context.addError(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Cannot parse runtime integer expression: " + value + ".",
            statement
        );
        return null;
    }

    private static ClassicalExpression parseRuntimeBinaryIntegerExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final BinaryExpressionParts parts = splitRuntimeBinaryExpression(value);
        if (parts == null) {
            return null;
        }
        final ClassicalExpression left = parseRuntimeIntegerExpression(
            context,
            statement,
            parts.left()
        );
        final ClassicalExpression right = parseRuntimeIntegerExpression(
            context,
            statement,
            parts.right()
        );
        if (
            left == null
            || right == null
        ) {
            return null;
        }
        return ClassicalExpression.binary(
            parts.operator(),
            left,
            right
        );
    }

    private static BinaryExpressionParts splitRuntimeBinaryExpression(final String value) {
        BinaryExpressionParts shiftParts = splitRuntimeShiftExpression(value);
        if (shiftParts != null) {
            return shiftParts;
        }
        BinaryExpressionParts parts = splitRuntimeBinaryExpression(
            value,
            "|^&"
        );
        if (parts != null) {
            return parts;
        }
        parts = splitRuntimeBinaryExpression(
            value,
            "+-"
        );
        if (parts != null) {
            return parts;
        }
        parts = splitRuntimeBinaryExpression(
            value,
            "*/%"
        );
        if (parts != null) {
            return parts;
        }
        if (
            value.startsWith("(")
            && value.endsWith(")")
        ) {
            return splitRuntimeBinaryExpression(value.substring(
                1,
                value.length() - 1
            ).trim());
        }
        return null;
    }

    private static BinaryExpressionParts splitRuntimeShiftExpression(final String value) {
        int depth = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            final char current = value.charAt(i);
            if (current == ')') {
                depth++;
            } else if (current == '(') {
                depth--;
            } else if (
                depth == 0
                && i > 0
                && i + 1 < value.length()
            ) {
                final String operator = value.substring(
                    i,
                    i + 2
                );
                if (
                    "<<".equals(operator)
                    || ">>".equals(operator)
                ) {
                    return new BinaryExpressionParts(
                        value.substring(
                            0,
                            i
                        ).trim(),
                        "<<".equals(operator)
                            ? ClassicalBinaryOperator.SHIFT_LEFT
                            : ClassicalBinaryOperator.SHIFT_RIGHT,
                        value.substring(i + 2).trim()
                    );
                }
            }
        }
        return null;
    }

    private static BinaryExpressionParts splitRuntimeBinaryExpression(
        final String value,
        final String operators
    ) {
        int depth = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            final char current = value.charAt(i);
            if (current == ')') {
                depth++;
            } else if (current == '(') {
                depth--;
            } else if (
                depth == 0
                && operators.indexOf(current) >= 0
                && i > 0
            ) {
                return new BinaryExpressionParts(
                    value.substring(
                        0,
                        i
                    ).trim(),
                    binaryOperator(current),
                    value.substring(i + 1).trim()
                );
            }
        }
        return null;
    }

    private static ClassicalBinaryOperator binaryOperator(final char value) {
        return switch (value) {
            case '+' -> ClassicalBinaryOperator.ADD;
            case '-' -> ClassicalBinaryOperator.SUBTRACT;
            case '*' -> ClassicalBinaryOperator.MULTIPLY;
            case '/' -> ClassicalBinaryOperator.DIVIDE;
            case '%' -> ClassicalBinaryOperator.MODULO;
            case '&' -> ClassicalBinaryOperator.BITWISE_AND;
            case '|' -> ClassicalBinaryOperator.BITWISE_OR;
            case '^' -> ClassicalBinaryOperator.BITWISE_XOR;
            default -> throw new IllegalArgumentException("Unsupported binary operator.");
        };
    }

    private static QuantumOperand parseBracedQuantumOperand(
        final ParseContext context,
        final Statement statement,
        final String baseName,
        final String indexesText
    ) {
        final QuantumRegister register = context.quantumRegister(baseName);
        final QuantumOperand alias = context.quantumAlias(baseName);
        final ListParts parts = parseCommaAwareParts(
            context,
            statement,
            indexesText,
            "qubit index list"
        );
        if (parts == null) {
            return null;
        }
        for (int i = 0; i < parts.parts().size(); i++) {
            if (!canParseIntegerExpression(
                context,
                parts.parts().get(i)
            )) {
                return parseDynamicBracedQuantumOperand(
                    context,
                    statement,
                    baseName,
                    parts,
                    alias,
                    register
                );
            }
        }
        final Qubit[] qubits = new Qubit[parts.parts().size()];
        for (int i = 0; i < parts.parts().size(); i++) {
            final int index = parseNonNegativeIntegerExpression(
                context,
                statement,
                parts.parts().get(i)
            );
            if (index < 0) {
                return null;
            }
            if (alias != null) {
                if (index >= alias.size()) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "Qubit alias index is outside of bounds.",
                        statement
                    );
                    return null;
                }
                qubits[i] = alias.qubit(index);
            } else {
                final QuantumRegister actualRegister = register == null
                    ? context.ensureQuantumRegister(
                        baseName,
                        Math.max(
                            index + 1,
                            64
                        )
                    )
                    : register;
                if (index >= actualRegister.size()) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "Qubit index is outside of quantum register bounds.",
                        statement
                    );
                    return null;
                }
                qubits[i] = actualRegister.get(index);
            }
        }
        return QuantumOperand.of(qubits);
    }

    private static QuantumOperand parseDynamicBracedQuantumOperand(
        final ParseContext context,
        final Statement statement,
        final String baseName,
        final ListParts parts,
        final QuantumOperand alias,
        final QuantumRegister register
    ) {
        final QuantumReference[] references = new QuantumReference[parts.parts().size()];
        final QuantumRegister dynamicRegister = register == null
            ? context.ensureQuantumRegister(
                baseName,
                64
            )
            : register;
        for (int i = 0; i < parts.parts().size(); i++) {
            final ClassicalExpression expression = parseRuntimeIntegerExpression(
                context,
                statement,
                parts.parts().get(i)
            );
            if (expression == null) {
                return null;
            }
            if (alias != null) {
                final QuantumReference aliasReference = alias.dynamicReference(expression);
                if (aliasReference == null) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "Dynamic qubit alias index requires a contiguous static quantum alias.",
                        statement
                    );
                    return null;
                }
                references[i] = aliasReference;
            } else {
                references[i] = QuantumReference.dynamicIndex(
                    dynamicRegister,
                    expression
                );
            }
        }
        return new QuantumOperand(references);
    }

    private static QuantumOperand sliceQuantumAlias(
        final ParseContext context,
        final Statement statement,
        final QuantumOperand alias,
        final int start,
        final int end
    ) {
        if (
            start > end
            || end >= alias.size()
        ) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Qubit alias slice is outside of bounds.",
                statement
            );
            return null;
        }
        final Qubit[] qubits = new Qubit[end - start + 1];
        for (int i = 0; i < qubits.length; i++) {
            qubits[i] = alias.qubit(start + i);
        }
        return QuantumOperand.of(qubits);
    }

    private static ClassicalOperand parseClassicalOperand(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final ClassicalOperand directAlias = context.classicalAlias(value.trim());
        if (directAlias != null) {
            return directAlias;
        }
        final Matcher slicedMatcher = SLICED_ARGUMENT_PATTERN.matcher(value);
        if (slicedMatcher.matches()) {
            final ClassicalOperand alias = context.classicalAlias(slicedMatcher.group(1));
            final ClassicalRegister register = context.classicalRegister(slicedMatcher.group(1));
            final int start = parseNonNegativeIntegerExpression(
                context,
                statement,
                slicedMatcher.group(2)
            );
            final int end = parseNonNegativeIntegerExpression(
                context,
                statement,
                slicedMatcher.group(3)
            );
            if (
                start < 0
                || end < 0
            ) {
                return null;
            }
            if (alias != null) {
                return sliceClassicalAlias(
                    context,
                    statement,
                    alias,
                    start,
                    end
                );
            }
            if (register == null) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Unknown classical register: " + slicedMatcher.group(1) + ".",
                    statement
                );
                return null;
            }
            if (
                start > end
                || end >= register.size()
            ) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Classical slice is outside of register bounds.",
                    statement
                );
                return null;
            }
            return ClassicalOperand.slice(
                register,
                start,
                end
            );
        }
        final Matcher indexedMatcher = INDEXED_ARGUMENT_PATTERN.matcher(value);
        if (indexedMatcher.matches()) {
            final ClassicalOperand alias = context.classicalAlias(indexedMatcher.group(1));
            final ClassicalRegister register = context.classicalRegister(indexedMatcher.group(1));
            final int index = parseNonNegativeIntegerExpression(
                context,
                statement,
                indexedMatcher.group(2)
            );
            if (alias != null) {
                if (index < 0) {
                    return null;
                }
                if (index >= alias.size()) {
                    context.addError(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "Classical alias index is outside of bounds.",
                        statement
                    );
                    return null;
                }
                return ClassicalOperand.single(alias.bit(index));
            }
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

    private static ClassicalOperand sliceClassicalAlias(
        final ParseContext context,
        final Statement statement,
        final ClassicalOperand alias,
        final int start,
        final int end
    ) {
        if (
            start > end
            || end >= alias.size()
        ) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Classical alias slice is outside of bounds.",
                statement
            );
            return null;
        }
        final ClassicalBit[] bits = new ClassicalBit[end - start + 1];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = alias.bit(start + i);
        }
        return ClassicalOperand.of(bits);
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

    private static int parsePositiveIntegerExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final Long evaluated = evaluateIntegerExpression(
            context,
            statement,
            value
        );
        if (evaluated == null) {
            return -1;
        }
        if (
            evaluated.longValue() <= 0L
            || evaluated.longValue() > Integer.MAX_VALUE
        ) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Register size must be positive and inside Java int range.",
                statement
            );
            return -1;
        }
        return evaluated.intValue();
    }

    private static int parseNonNegativeIntegerExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        if (context.integerExpression(value.trim()) != null) {
            return -1;
        }
        if (
            !value.trim().matches("-?[0-9+*/()\\s]+")
            && !canEvaluateIntegerExpression(
                context,
                value
            )
        ) {
            return -1;
        }
        final Long evaluated = evaluateIntegerExpression(
            context,
            statement,
            value
        );
        if (evaluated == null) {
            return -1;
        }
        if (
            evaluated.longValue() < 0L
            || evaluated.longValue() > Integer.MAX_VALUE
        ) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Integer index is outside non-negative Java int range.",
                statement
            );
            return -1;
        }
        return evaluated.intValue();
    }

    private static Long evaluateIntegerExpression(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        final IntegerExpressionParser parser = new IntegerExpressionParser(
            context,
            statement,
            value
        );
        return parser.parse();
    }

    private static String sizeOrOne(final String value) {
        if (value == null) {
            return "1";
        }
        return value;
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

    private static long parseSignedLong(
        final ParseContext context,
        final Statement statement,
        final String value
    ) {
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException exception) {
            context.addError(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "Expected integer value.",
                statement
            );
            return 0L;
        }
    }

    private record Statement(
        String text,
        int line,
        int column
    ) {
    }

    private record IfInlineParts(
        String condition,
        String body
    ) {
    }

    private record BlockParts(
        String thenText,
        String elseText
    ) {

        private boolean hasElseText() {
            return elseText != null;
        }
    }

    private record ListParts(ArrayList<String> parts) {
    }

    private enum SubroutineArgumentKind {

        QUANTUM,
        CLASSICAL,
        INTEGER
    }

    private record SubroutineArgument(
        String name,
        SubroutineArgumentKind kind
    ) {
    }

    private record SubroutineDefinition(
        String name,
        SubroutineArgument[] arguments,
        String body,
        String returnExpression,
        String returnType
    ) {

        private int argumentCount() {
            return arguments.length;
        }

        private SubroutineArgument argument(final int index) {
            return arguments[index];
        }
    }

    private record SubroutineBodyParts(
        String body,
        String returnExpression
    ) {
    }

    private record ReturningSubroutineCall(
        SubroutineDefinition definition,
        ListParts arguments
    ) {
    }

    private record SubroutineCallParts(
        String name,
        String argumentsText
    ) {
    }

    private record GateCallParts(
        String name,
        Gate gate,
        String parametersText,
        String operandsText
    ) {
    }

    private record BinaryExpressionParts(
        String left,
        ClassicalBinaryOperator operator,
        String right
    ) {
    }

    private record AliasScope(
        LinkedHashMap<String, QuantumOperand> quantumAliases,
        LinkedHashMap<String, ClassicalOperand> classicalAliases,
        LinkedHashMap<String, Long> integerConstants,
        LinkedHashMap<String, ClassicalExpression> integerExpressions
    ) {
    }

    private static final class IntegerExpressionParser {

        private final ParseContext context;
        private final Statement statement;
        private final String source;
        private int position;

        private IntegerExpressionParser(
            final ParseContext context,
            final Statement statement,
            final String source
        ) {
            this.context = context;
            this.statement = statement;
            this.source = source == null ? "" : source.trim();
            this.position = 0;
        }

        private Long parse() {
            final Long value = parseAdditive();
            skipWhitespace();
            if (
                value == null
                || position != source.length()
            ) {
                context.addError(
                    IntegrationDiagnosticCode.PARSE_ERROR,
                    "Cannot evaluate integer expression: " + source + ".",
                    statement
                );
                return null;
            }
            return value;
        }

        private Long parseAdditive() {
            Long value = parseMultiplicative();
            while (value != null) {
                skipWhitespace();
                if (consume('+')) {
                    final Long right = parseMultiplicative();
                    if (right == null) {
                        return null;
                    }
                    value = Long.valueOf(value.longValue() + right.longValue());
                } else if (consume('-')) {
                    final Long right = parseMultiplicative();
                    if (right == null) {
                        return null;
                    }
                    value = Long.valueOf(value.longValue() - right.longValue());
                } else {
                    return value;
                }
            }
            return null;
        }

        private Long parseMultiplicative() {
            Long value = parseUnary();
            while (value != null) {
                skipWhitespace();
                if (consumePowerOperator()) {
                    final Long right = parseUnary();
                    if (
                        right == null
                        || right.longValue() < 0L
                        || right.longValue() > Integer.MAX_VALUE
                    ) {
                        return null;
                    }
                    value = Long.valueOf(integerPower(
                        value.longValue(),
                        right.intValue()
                    ));
                } else if (consume('*')) {
                    final Long right = parseUnary();
                    if (right == null) {
                        return null;
                    }
                    value = Long.valueOf(value.longValue() * right.longValue());
                } else if (consume('/')) {
                    final Long right = parseUnary();
                    if (
                        right == null
                        || right.longValue() == 0L
                    ) {
                        return null;
                    }
                    value = Long.valueOf(value.longValue() / right.longValue());
                } else {
                    return value;
                }
            }
            return null;
        }

        private static long integerPower(
            final long base,
            final int exponent
        ) {
            long result = 1L;
            for (int i = 0; i < exponent; i++) {
                result *= base;
            }
            return result;
        }

        private Long parseUnary() {
            skipWhitespace();
            if (consume('-')) {
                final Long value = parseUnary();
                return value == null ? null : Long.valueOf(-value.longValue());
            }
            return parsePrimary();
        }

        private Long parsePrimary() {
            skipWhitespace();
            if (consume('(')) {
                final Long value = parseAdditive();
                skipWhitespace();
                if (!consume(')')) {
                    return null;
                }
                return value;
            }
            if (position >= source.length()) {
                return null;
            }
            final char current = source.charAt(position);
            if (Character.isDigit(current)) {
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

        private Long parseNumber() {
            final int start = position;
            while (
                position < source.length()
                && (
                    Character.isDigit(source.charAt(position))
                    || source.charAt(position) == 'b'
                    || source.charAt(position) == 'B'
                    || source.charAt(position) == 'x'
                    || source.charAt(position) == 'X'
                    || (
                        source.charAt(position) >= 'a'
                        && source.charAt(position) <= 'f'
                    )
                    || (
                        source.charAt(position) >= 'A'
                        && source.charAt(position) <= 'F'
                    )
                )
            ) {
                position++;
            }
            final String number = source.substring(
                start,
                position
            );
            try {
                if (
                    number.startsWith("0b")
                    || number.startsWith("0B")
                ) {
                    return Long.valueOf(Long.parseLong(
                        number.substring(2),
                        2
                    ));
                }
                if (
                    number.startsWith("0x")
                    || number.startsWith("0X")
                ) {
                    return Long.valueOf(Long.parseLong(
                        number.substring(2),
                        16
                    ));
                }
                return Long.valueOf(Long.parseLong(number));
            } catch (final NumberFormatException exception) {
                return null;
            }
        }

        private Long parseIdentifier() {
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
            return context.integerConstant(source.substring(
                start,
                position
            ));
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

        private boolean consumePowerOperator() {
            skipWhitespace();
            if (
                position + 1 < source.length()
                && source.charAt(position) == '*'
                && source.charAt(position + 1) == '*'
            ) {
                position += 2;
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

    private static final class ConstantParameterExpressionParser {

        private final String source;
        private int position;

        private ConstantParameterExpressionParser(final String source) {
            this.source = source == null ? "" : source.trim();
            this.position = 0;
        }

        private Double parse() {
            final Double value = parseAdditive();
            skipWhitespace();
            if (
                value == null
                || position != source.length()
                || !Double.isFinite(value.doubleValue())
            ) {
                return null;
            }
            return value;
        }

        private Double parseAdditive() {
            Double value = parseMultiplicative();
            while (value != null) {
                skipWhitespace();
                if (consume('+')) {
                    final Double right = parseMultiplicative();
                    if (right == null) {
                        return null;
                    }
                    value = Double.valueOf(value.doubleValue() + right.doubleValue());
                } else if (consume('-')) {
                    final Double right = parseMultiplicative();
                    if (right == null) {
                        return null;
                    }
                    value = Double.valueOf(value.doubleValue() - right.doubleValue());
                } else {
                    return value;
                }
            }
            return null;
        }

        private Double parseMultiplicative() {
            Double value = parseUnary();
            while (value != null) {
                skipWhitespace();
                if (consume('*')) {
                    final Double right = parseUnary();
                    if (right == null) {
                        return null;
                    }
                    value = Double.valueOf(value.doubleValue() * right.doubleValue());
                } else if (consume('/')) {
                    final Double right = parseUnary();
                    if (
                        right == null
                        || right.doubleValue() == 0.0D
                    ) {
                        return null;
                    }
                    value = Double.valueOf(value.doubleValue() / right.doubleValue());
                } else {
                    return value;
                }
            }
            return null;
        }

        private Double parseUnary() {
            skipWhitespace();
            if (consume('-')) {
                final Double value = parseUnary();
                return value == null ? null : Double.valueOf(-value.doubleValue());
            }
            return parsePrimary();
        }

        private Double parsePrimary() {
            skipWhitespace();
            if (consume('(')) {
                final Double value = parseAdditive();
                skipWhitespace();
                if (!consume(')')) {
                    return null;
                }
                return value;
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
                return parseIdentifierOrFunction();
            }
            return null;
        }

        private Double parseNumber() {
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
                return Double.valueOf(Double.parseDouble(source.substring(
                    start,
                    position
                )));
            } catch (final NumberFormatException exception) {
                return null;
            }
        }

        private Double parseIdentifierOrFunction() {
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
                return Double.valueOf(Math.PI);
            }
            skipWhitespace();
            if (!consume('(')) {
                return null;
            }
            final Double argument = parseAdditive();
            skipWhitespace();
            if (
                argument == null
                || !consume(')')
            ) {
                return null;
            }
            return evaluateFunction(
                identifier,
                argument.doubleValue()
            );
        }

        private static Double evaluateFunction(
            final String identifier,
            final double argument
        ) {
            return switch (identifier) {
                case "sin" -> Double.valueOf(Math.sin(argument));
                case "cos" -> Double.valueOf(Math.cos(argument));
                case "tan" -> Double.valueOf(Math.tan(argument));
                case "exp" -> Double.valueOf(Math.exp(argument));
                case "ln" -> argument > 0.0D ? Double.valueOf(Math.log(argument)) : null;
                case "sqrt" -> argument >= 0.0D ? Double.valueOf(Math.sqrt(argument)) : null;
                case "arccos" -> Double.valueOf(Math.acos(argument));
                case "arcsin" -> Double.valueOf(Math.asin(argument));
                case "arctan" -> Double.valueOf(Math.atan(argument));
                default -> null;
            };
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
        private final HashSet<String> activeSubroutineInlines;
        private final LinkedHashMap<String, QuantumRegister> quantumRegisters;
        private final LinkedHashMap<String, ClassicalRegister> classicalRegisters;
        private final LinkedHashMap<String, GateDefinition> gateDefinitions;
        private final LinkedHashMap<String, Long> integerConstants;
        private final LinkedHashMap<String, ClassicalExpression> integerExpressions;
        private final LinkedHashMap<String, long[]> integerArrays;
        private final LinkedHashMap<String, DurationExpression> durationConstants;
        private final LinkedHashMap<String, QuantumOperand> quantumAliases;
        private final LinkedHashMap<String, ClassicalOperand> classicalAliases;
        private final LinkedHashMap<String, SubroutineDefinition> subroutines;

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
            this.activeSubroutineInlines = new HashSet<>();
            this.source = new ExternalSource(
                "openqasm3",
                "OpenQASM 3 import"
            );
            this.quantumRegisters = new LinkedHashMap<>();
            this.classicalRegisters = new LinkedHashMap<>();
            this.gateDefinitions = new LinkedHashMap<>();
            this.integerConstants = new LinkedHashMap<>();
            this.integerExpressions = new LinkedHashMap<>();
            this.integerArrays = new LinkedHashMap<>();
            this.durationConstants = new LinkedHashMap<>();
            this.quantumAliases = new LinkedHashMap<>();
            this.classicalAliases = new LinkedHashMap<>();
            this.subroutines = new LinkedHashMap<>();
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

        private QuantumRegister ensureQuantumRegister(
            final String name,
            final int size
        ) {
            QuantumRegister register = quantumRegisters.get(name);
            if (register == null) {
                register = circuit.createQuantumRegister(
                    name,
                    size
                );
                quantumRegisters.put(
                    name,
                    register
                );
            }
            return register;
        }

        private ClassicalRegister ensureClassicalRegister(
            final String name,
            final int size
        ) {
            ClassicalRegister register = classicalRegisters.get(name);
            if (register == null) {
                register = circuit.createClassicalRegister(
                    name,
                    size
                );
                classicalRegisters.put(
                    name,
                    register
                );
            }
            return register;
        }

        private Qubit physicalQubit(final int index) {
            final String physicalRegisterName = "_physical";
            QuantumRegister register = quantumRegisters.get(physicalRegisterName);
            if (register == null) {
                register = circuit.createQuantumRegister(
                    physicalRegisterName,
                    Math.max(
                        index + 1,
                        64
                    )
                );
                quantumRegisters.put(
                    physicalRegisterName,
                    register
                );
            }
            return register.get(index);
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

        private void addIntegerConstant(
            final String name,
            final long value
        ) {
            integerConstants.put(
                name,
                Long.valueOf(value)
            );
        }

        private Long integerConstant(final String name) {
            return integerConstants.get(name);
        }

        private void addIntegerExpression(
            final String name,
            final ClassicalExpression expression
        ) {
            integerExpressions.put(
                name,
                expression
            );
        }

        private ClassicalExpression integerExpression(final String name) {
            return integerExpressions.get(name);
        }

        private void restoreIntegerExpression(
            final String name,
            final ClassicalExpression previous
        ) {
            if (previous == null) {
                integerExpressions.remove(name);
            } else {
                integerExpressions.put(
                    name,
                    previous
                );
            }
        }

        private void addIntegerArray(
            final String name,
            final long[] values
        ) {
            integerArrays.put(
                name,
                values.clone()
            );
        }

        private long[] integerArray(final String name) {
            final long[] values = integerArrays.get(name);
            return values == null ? null : values.clone();
        }

        private void restoreIntegerConstant(
            final String name,
            final Long previous
        ) {
            if (previous == null) {
                integerConstants.remove(name);
            } else {
                integerConstants.put(
                    name,
                    previous
                );
            }
        }

        private void addDurationConstant(
            final String name,
            final DurationExpression value
        ) {
            durationConstants.put(
                name,
                value
            );
        }

        private DurationExpression durationConstant(final String name) {
            return durationConstants.get(name);
        }

        private void addQuantumAlias(
            final String name,
            final QuantumOperand operand
        ) {
            quantumAliases.put(
                name,
                operand
            );
        }

        private QuantumOperand quantumAlias(final String name) {
            return quantumAliases.get(name);
        }

        private void addClassicalAlias(
            final String name,
            final ClassicalOperand operand
        ) {
            classicalAliases.put(
                name,
                operand
            );
        }

        private ClassicalOperand classicalAlias(final String name) {
            return classicalAliases.get(name);
        }

        private void addSubroutine(final SubroutineDefinition definition) {
            subroutines.put(
                definition.name(),
                definition
            );
        }

        private SubroutineDefinition subroutine(final String name) {
            return subroutines.get(name);
        }

        private boolean hasCallable(final String name) {
            if (subroutines.containsKey(name)) {
                return true;
            }
            for (int i = 0; i < program.callableDefinitionCount(); i++) {
                if (program.callableDefinition(i).name().equals(name)) {
                    return true;
                }
            }
            for (int i = 0; i < program.externalCallableDeclarationCount(); i++) {
                if (program.externalCallableDeclaration(i).name().equals(name)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSubroutineInlineActive(final String name) {
            return activeSubroutineInlines.contains(name);
        }

        private void enterSubroutineInline(final String name) {
            activeSubroutineInlines.add(name);
        }

        private void exitSubroutineInline(final String name) {
            activeSubroutineInlines.remove(name);
        }

        private AliasScope openAliasScope(final SubroutineArgument[] arguments) {
            final LinkedHashMap<String, QuantumOperand> previousQuantumAliases = new LinkedHashMap<>();
            final LinkedHashMap<String, ClassicalOperand> previousClassicalAliases = new LinkedHashMap<>();
            final LinkedHashMap<String, Long> previousIntegerConstants = new LinkedHashMap<>();
            final LinkedHashMap<String, ClassicalExpression> previousIntegerExpressions = new LinkedHashMap<>();
            for (int i = 0; i < arguments.length; i++) {
                previousQuantumAliases.put(
                    arguments[i].name(),
                    quantumAliases.get(arguments[i].name())
                );
                previousClassicalAliases.put(
                    arguments[i].name(),
                    classicalAliases.get(arguments[i].name())
                );
                previousIntegerConstants.put(
                    arguments[i].name(),
                    integerConstants.get(arguments[i].name())
                );
                previousIntegerExpressions.put(
                    arguments[i].name(),
                    integerExpressions.get(arguments[i].name())
                );
            }
            return new AliasScope(
                previousQuantumAliases,
                previousClassicalAliases,
                previousIntegerConstants,
                previousIntegerExpressions
            );
        }

        private void restoreAliasScope(final AliasScope scope) {
            restoreQuantumAliases(scope.quantumAliases());
            restoreClassicalAliases(scope.classicalAliases());
            restoreIntegerConstants(scope.integerConstants());
            restoreIntegerExpressions(scope.integerExpressions());
        }

        private void restoreQuantumAliases(final LinkedHashMap<String, QuantumOperand> previousAliases) {
            for (final String name : previousAliases.keySet()) {
                final QuantumOperand previous = previousAliases.get(name);
                if (previous == null) {
                    quantumAliases.remove(name);
                } else {
                    quantumAliases.put(
                        name,
                        previous
                    );
                }
            }
        }

        private void restoreClassicalAliases(final LinkedHashMap<String, ClassicalOperand> previousAliases) {
            for (final String name : previousAliases.keySet()) {
                final ClassicalOperand previous = previousAliases.get(name);
                if (previous == null) {
                    classicalAliases.remove(name);
                } else {
                    classicalAliases.put(
                        name,
                        previous
                    );
                }
            }
        }

        private void restoreIntegerConstants(final LinkedHashMap<String, Long> previousConstants) {
            for (final String name : previousConstants.keySet()) {
                final Long previous = previousConstants.get(name);
                restoreIntegerConstant(
                    name,
                    previous
                );
            }
        }

        private void restoreIntegerExpressions(
            final LinkedHashMap<String, ClassicalExpression> previousExpressions
        ) {
            for (final String name : previousExpressions.keySet()) {
                final ClassicalExpression previous = previousExpressions.get(name);
                if (previous == null) {
                    integerExpressions.remove(name);
                } else {
                    integerExpressions.put(
                        name,
                        previous
                    );
                }
            }
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

        private final QuantumReference[] references;

        private QuantumOperand(final QuantumReference[] references) {
            this.references = references;
        }

        private static QuantumOperand single(final Qubit qubit) {
            return single(QuantumReference.staticQubit(qubit));
        }

        private static QuantumOperand single(final QuantumReference reference) {
            return new QuantumOperand(new QuantumReference[] {reference});
        }

        private static QuantumOperand of(final Qubit[] qubits) {
            final QuantumReference[] references = new QuantumReference[qubits.length];
            for (int i = 0; i < qubits.length; i++) {
                references[i] = QuantumReference.staticQubit(qubits[i]);
            }
            return new QuantumOperand(references);
        }

        private static QuantumOperand register(final QuantumRegister register) {
            final QuantumReference[] references = new QuantumReference[register.size()];
            for (int i = 0; i < register.size(); i++) {
                references[i] = QuantumReference.staticQubit(register.get(i));
            }
            return new QuantumOperand(references);
        }

        private static QuantumOperand slice(
            final QuantumRegister register,
            final int start,
            final int end
        ) {
            final QuantumReference[] references = new QuantumReference[end - start + 1];
            for (int i = 0; i < references.length; i++) {
                references[i] = QuantumReference.staticQubit(register.get(start + i));
            }
            return new QuantumOperand(references);
        }

        private int size() {
            return references.length;
        }

        private Qubit qubit(final int index) {
            return references[index].qubit();
        }

        private QuantumReference reference(final int index) {
            return references[index];
        }

        private QuantumReference dynamicReference(final ClassicalExpression indexExpression) {
            if (
                references.length == 0
                || references[0].kind() != QuantumReferenceKind.STATIC_QUBIT
            ) {
                return null;
            }
            final Qubit first = references[0].qubit();
            final QuantumRegister register = first.register();
            final int startIndex = first.index();
            for (int i = 1; i < references.length; i++) {
                if (references[i].kind() != QuantumReferenceKind.STATIC_QUBIT) {
                    return null;
                }
                final Qubit current = references[i].qubit();
                if (
                    current.register() != register
                    || current.index() != startIndex + i
                ) {
                    return null;
                }
            }
            return QuantumReference.dynamicIndex(
                register,
                startIndex == 0
                    ? indexExpression
                    : ClassicalExpression.binary(
                        ClassicalBinaryOperator.ADD,
                        ClassicalExpression.integer(startIndex),
                        indexExpression
                    )
            );
        }

        private QuantumReference[] references() {
            return references.clone();
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

        private static ClassicalOperand of(final ClassicalBit[] bits) {
            return new ClassicalOperand(bits.clone());
        }

        private static ClassicalOperand register(final ClassicalRegister register) {
            final ClassicalBit[] bits = new ClassicalBit[register.size()];
            for (int i = 0; i < register.size(); i++) {
                bits[i] = register.get(i);
            }
            return new ClassicalOperand(bits);
        }

        private static ClassicalOperand slice(
            final ClassicalRegister register,
            final int start,
            final int end
        ) {
            final ClassicalBit[] bits = new ClassicalBit[end - start + 1];
            for (int i = 0; i < bits.length; i++) {
                bits[i] = register.get(start + i);
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