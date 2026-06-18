/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;

class DesktopOperationLabelRendererTest {

    private final DesktopOperationLabelRenderer renderer = new DesktopOperationLabelRenderer();

    @Test
    void rendersThreeQubitGateSummary() {
        assertEquals(
            "CCX q[0], q[1], q[2]",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "CCX",
                "q[0]",
                "q[1]",
                "q[2]",
                "c[0]",
                0.5
            ))
        );
    }

    @Test
    void rendersMeasurementClassicalTarget() {
        assertEquals(
            "MEASURE q[3] -> c[2]",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "MEASURE",
                "q[3]",
                "q[0]",
                "q[0]",
                "c[2]",
                0.5
            ))
        );
    }

    @Test
    void rendersStructuredShortcutSummaries() {
        assertEquals(
            "IF c[2] == 7 THEN body 0 ELSE body 0",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "IF_BLOCK",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[2]",
                7.0
            ))
        );
        assertEquals(
            "SYM_FOR k 0..expr(5) body 0",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "SYM_FOR",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                5.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "k"
            ))
        );
    }

    @Test
    void rendersComplexClassicalPredicateSummary() {
        final ClassicalPredicate predicate = ClassicalPredicate.not(ClassicalPredicate.or(
            ClassicalPredicate.compare(
                ClassicalExpression.symbolicReference("flag[0]"),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1L)
            ),
            ClassicalPredicate.compare(
                ClassicalExpression.variable("counter"),
                ClassicalComparisonOperator.GREATER_THAN_OR_EQUAL,
                ClassicalExpression.integer(3L)
            )
        ));

        assertEquals(
            "WHILE not ((flag[0] == 1) or (counter >= 3)) body 0",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "WHILE",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[0]",
                1.0,
                0.0,
                0.0,
                20.0,
                "NS",
                "while",
                predicate,
                java.util.List.of(),
                java.util.List.of()
            ))
        );
    }

    @Test
    void rendersEditableWhileShortcutPredicateSummary() {
        assertEquals(
            "WHILE c[1] == 3 body 0",
            renderer.renderSummary(new DesktopIrOperationSpec(
                "WHILE",
                "q[0]",
                "q[0]",
                "q[0]",
                "c[1]",
                3.0
            ))
        );
    }
}