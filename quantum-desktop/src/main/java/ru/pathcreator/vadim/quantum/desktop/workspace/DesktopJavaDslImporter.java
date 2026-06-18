/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Безопасно восстанавливает визуальный workspace из Java DSL, который ранее сгенерировала desktop-студия.
 */
public final class DesktopJavaDslImporter {

    private static final Pattern CIRCUIT = Pattern.compile(
        "\\.circuit\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern QREG = Pattern.compile(
        "\\.qreg\\(\"([^\"]+)\",\\s*(\\d+)\\)"
    );
    private static final Pattern CREG = Pattern.compile(
        "\\.creg\\(\"([^\"]+)\",\\s*(\\d+)\\)"
    );
    private static final Pattern SINGLE_QUBIT = Pattern.compile(
        "\\.(h|x|y|z|s|sdg|t|tdg|id|reset)\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern ROTATION = Pattern.compile(
        "\\.(rx|ry|rz|phase)\\(([-+0-9.Ee]+),\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern CONTROLLED = Pattern.compile(
        "\\.(cx|cy|cz|ch|swap)\\(\"([^\"]+)\",\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern CPHASE = Pattern.compile(
        "\\.cphase\\(([-+0-9.Ee]+),\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern CCX = Pattern.compile(
        "\\.ccx\\(\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern MEASURE = Pattern.compile(
        "\\.measure\\(\"([^\"]+)\",\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern BARRIER = Pattern.compile(
        "\\.barrier\\(\"([^\"]+)\",\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern LABEL = Pattern.compile(
        "\\.label\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern DELAY = Pattern.compile(
        "\\.delay\\(.*duration\\(([-+0-9.Ee]+),\\s*.*DurationUnit\\.([A-Z]+)\\),"
            + "\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"\\)"
    );
    private static final Pattern U_GATE = Pattern.compile(
        "\\.u\\(.*?of\\(([-+0-9.Ee]+)\\).*?of\\(([-+0-9.Ee]+)\\)"
            + ".*?of\\(([-+0-9.Ee]+)\\).*?\"([^\"]+)\"\\)"
    );
    private static final Pattern BRANCH = Pattern.compile(
        "\\.branch\\(.*always\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern ASSIGN = Pattern.compile(
        "\\.assign\\(.*bit\\(bit\\(\"([^\"]+)\"\\)\\).*integer\\(([-+0-9]+)L\\)"
    );
    private static final Pattern DECLARE = Pattern.compile(
        "\\.classicalDeclaration\\(.*ClassicalDeclaration\\(\"([^\"]+)\""
            + ".*integer\\(([-+0-9]+)L\\)"
    );
    private static final Pattern ARRAY = Pattern.compile(
        "\\.classicalArrayDeclaration\\(.*Operation\\(\"([^\"]+)\""
            + ".*integer\\(([-+0-9]+)L\\)"
    );
    private static final Pattern CALL = Pattern.compile(
        "\\.callableInvocation\\(.*Operation\\(\"([^\"]+)\""
    );
    private static final Pattern IF_X = Pattern.compile(
        "\\.classicallyControlled\\(.*bit\\(bit\\(\"([^\"]+)\"\\)\\)"
            + ".*integer\\(([-+0-9]+)L\\).*StandardGate\\.X,"
            + "\\s*qubit\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern CTRL_X = Pattern.compile(
        "\\.controlled\\(.*classicalRegister\\(\"([^\"]+)\"\\),\\s*([-+0-9]+)L\\)"
            + ".*StandardGate\\.X,\\s*qubit\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern FOR_LOOP = Pattern.compile(
        "\\.forLoop\\(\"([^\"]+)\",\\s*0L,\\s*1L,\\s*([-+0-9]+)L"
    );
    private static final Pattern SYMBOLIC_FOR = Pattern.compile(
        "\\.symbolicForLoop\\(.*Operation\\(\"([^\"]+)\".*integer\\(([-+0-9]+)L\\)"
    );
    private static final Pattern WHILE = Pattern.compile(
        "\\.whileLoop\\(.*bit\\(bit\\(\"([^\"]+)\"\\)\\).*integer\\(([-+0-9]+)L\\)"
    );
    private static final Pattern TIMING_BOX = Pattern.compile(
        "\\.timingBox\\(.*duration\\(([-+0-9.Ee]+),\\s*.*DurationUnit\\.([A-Z]+)\\)"
    );

    public DesktopJavaDslImportResult importDsl(final String source) {
        final ArrayList<String> diagnostics = new ArrayList<>();
        final String circuitName = requiredString(
            source,
            CIRCUIT,
            "circuit",
            "main",
            diagnostics
        );
        final Register qreg = requiredRegister(
            source,
            QREG,
            "qreg",
            "q",
            diagnostics
        );
        final Register creg = requiredRegister(
            source,
            CREG,
            "creg",
            "c",
            diagnostics
        );
        final ArrayList<DesktopIrOperationSpec> operations = new ArrayList<>();
        final String[] lines = source.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            parseLine(
                lines[i].trim(),
                operations,
                diagnostics
            );
        }
        return new DesktopJavaDslImportResult(
            circuitName,
            qreg.name(),
            qreg.size(),
            creg.name(),
            creg.size(),
            operations,
            diagnostics
        );
    }

    private static void parseLine(
        final String line,
        final List<DesktopIrOperationSpec> operations,
        final List<String> diagnostics
    ) {
        if (
            line.isBlank()
            || line.startsWith("final ")
            || line.startsWith(".circuit(")
            || line.startsWith(".qreg(")
            || line.startsWith(".creg(")
            || line.startsWith(".build(")
        ) {
            return;
        }
        if (appendSingleQubit(line, operations)) {
            return;
        }
        if (appendRotation(line, operations)) {
            return;
        }
        if (appendControlled(line, operations)) {
            return;
        }
        if (appendSpecialOperation(line, operations)) {
            return;
        }
        diagnostics.add("Unsupported saved Java DSL line: " + line);
    }

    private static boolean appendSingleQubit(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = SINGLE_QUBIT.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(spec(
            gateName(matcher.group(1)),
            matcher.group(2)
        ));
        return true;
    }

    private static boolean appendRotation(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = ROTATION.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            gateName(matcher.group(1)),
            matcher.group(3),
            matcher.group(3),
            matcher.group(3),
            "c[0]",
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendControlled(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = CONTROLLED.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(spec(
            gateName(matcher.group(1)),
            matcher.group(2),
            matcher.group(3)
        ));
        return true;
    }

    private static boolean appendSpecialOperation(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        return appendCphase(line, operations)
            || appendCcx(line, operations)
            || appendMeasure(line, operations)
            || appendBarrier(line, operations)
            || appendDelay(line, operations)
            || appendU(line, operations)
            || appendLabel(line, operations)
            || appendBranch(line, operations)
            || appendAssign(line, operations)
            || appendDeclare(line, operations)
            || appendArray(line, operations)
            || appendCall(line, operations)
            || appendIfX(line, operations)
            || appendCtrlX(line, operations)
            || appendStructural(line, operations);
    }

    private static boolean appendCphase(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = CPHASE.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "CPHASE",
            matcher.group(2),
            matcher.group(3),
            matcher.group(2),
            "c[0]",
            parseDouble(matcher.group(1))
        ));
        return true;
    }

    private static boolean appendCcx(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = CCX.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "CCX",
            matcher.group(1),
            matcher.group(2),
            matcher.group(3),
            "c[0]",
            Math.PI / 2.0
        ));
        return true;
    }

    private static boolean appendMeasure(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = MEASURE.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "MEASURE",
            matcher.group(1),
            matcher.group(1),
            matcher.group(1),
            matcher.group(2),
            Math.PI / 2.0
        ));
        return true;
    }

    private static boolean appendBarrier(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = BARRIER.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(spec(
            "BARRIER",
            matcher.group(1),
            matcher.group(2)
        ));
        return true;
    }

    private static boolean appendDelay(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = DELAY.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "DELAY",
            matcher.group(3),
            matcher.group(4),
            matcher.group(3),
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            parseDouble(matcher.group(1)),
            matcher.group(2),
            "entry"
        ));
        return true;
    }

    private static boolean appendU(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = U_GATE.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "U",
            matcher.group(4),
            matcher.group(4),
            matcher.group(4),
            "c[0]",
            parseDouble(matcher.group(1)),
            parseDouble(matcher.group(2)),
            parseDouble(matcher.group(3)),
            20.0,
            "NS",
            "entry"
        ));
        return true;
    }

    private static boolean appendLabel(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = LABEL.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "LABEL",
            matcher.group(1),
            0.0
        ));
        return true;
    }

    private static boolean appendBranch(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = BRANCH.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "BRANCH",
            matcher.group(1),
            0.0
        ));
        return true;
    }

    private static boolean appendAssign(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = ASSIGN.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "ASSIGN",
            "q[0]",
            "q[0]",
            "q[0]",
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendDeclare(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = DECLARE.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "DECLARE",
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendArray(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = ARRAY.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "ARRAY",
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendCall(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = CALL.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "CALL",
            matcher.group(1),
            0.0
        ));
        return true;
    }

    private static boolean appendIfX(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = IF_X.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "IF_X",
            matcher.group(3),
            matcher.group(3),
            matcher.group(3),
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendCtrlX(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = CTRL_X.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "CTRL_X",
            matcher.group(3),
            matcher.group(3),
            matcher.group(3),
            matcher.group(1) + "[0]",
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendStructural(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        if (line.startsWith(".block(")) {
            operations.add(spec("BLOCK", "q[0]"));
            return true;
        }
        if (line.startsWith(".conditionalBlock(")) {
            operations.add(spec("IF_BLOCK", "q[0]"));
            return true;
        }
        if (appendFor(line, operations)) {
            return true;
        }
        if (appendSymbolicFor(line, operations)) {
            return true;
        }
        if (appendWhile(line, operations)) {
            return true;
        }
        if (appendTimingBox(line, operations)) {
            return true;
        }
        if (line.startsWith(".halt(")) {
            operations.add(spec("HALT", "q[0]"));
            return true;
        }
        if (line.startsWith(".waitInstruction(")) {
            operations.add(spec("WAIT", "q[0]"));
            return true;
        }
        return false;
    }

    private static boolean appendFor(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = FOR_LOOP.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "FOR",
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendSymbolicFor(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = SYMBOLIC_FOR.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(labeledSpec(
            "SYM_FOR",
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendWhile(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = WHILE.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "WHILE",
            "q[0]",
            "q[0]",
            "q[0]",
            matcher.group(1),
            parseDouble(matcher.group(2))
        ));
        return true;
    }

    private static boolean appendTimingBox(
        final String line,
        final List<DesktopIrOperationSpec> operations
    ) {
        final Matcher matcher = TIMING_BOX.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        operations.add(new DesktopIrOperationSpec(
            "TIMING_BOX",
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0,
            0.0,
            0.0,
            parseDouble(matcher.group(1)),
            matcher.group(2),
            "entry"
        ));
        return true;
    }

    private static DesktopIrOperationSpec spec(
        final String gate,
        final String primaryQubit
    ) {
        return spec(
            gate,
            primaryQubit,
            primaryQubit
        );
    }

    private static DesktopIrOperationSpec spec(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            primaryQubit,
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec labeledSpec(
        final String gate,
        final String labelName,
        final double angle
    ) {
        return new DesktopIrOperationSpec(
            gate,
            "q[0]",
            "q[0]",
            "q[0]",
            "c[0]",
            angle,
            0.0,
            0.0,
            20.0,
            "NS",
            labelName
        );
    }

    private static String gateName(final String methodName) {
        return switch (methodName) {
            case "phase" -> "PHASE";
            default -> methodName.toUpperCase(Locale.ROOT);
        };
    }

    private static String requiredString(
        final String source,
        final Pattern pattern,
        final String name,
        final String fallback,
        final List<String> diagnostics
    ) {
        final Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        diagnostics.add("Saved Java DSL does not contain ." + name + "(...).");
        return fallback;
    }

    private static Register requiredRegister(
        final String source,
        final Pattern pattern,
        final String name,
        final String fallbackName,
        final List<String> diagnostics
    ) {
        final Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return new Register(
                matcher.group(1),
                Integer.parseInt(matcher.group(2))
            );
        }
        diagnostics.add("Saved Java DSL does not contain ." + name + "(...).");
        return new Register(
            fallbackName,
            1
        );
    }

    private static double parseDouble(final String value) {
        return Double.parseDouble(value);
    }

    private record Register(
        String name,
        int size
    ) {
    }
}