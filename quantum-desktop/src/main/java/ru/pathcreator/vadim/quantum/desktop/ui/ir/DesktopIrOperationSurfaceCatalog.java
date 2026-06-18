/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.ir;

import java.util.EnumMap;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

/**
 * Описывает полную поверхность операций Quantum IR, которую desktop обязан показывать пользователю.
 */
public final class DesktopIrOperationSurfaceCatalog {

    private final EnumMap<OperationKind, String> russianDescriptions = new EnumMap<>(OperationKind.class);
    private final EnumMap<OperationKind, String> englishDescriptions = new EnumMap<>(OperationKind.class);

    public DesktopIrOperationSurfaceCatalog() {
        put(
            OperationKind.GATE,
            "Квантовый gate: стандартный, custom, parameterized или modified.",
            "Quantum gate: standard, custom, parameterized or modified."
        );
        put(
            OperationKind.MEASURE,
            "Измерение qubit в classical bit.",
            "Measures a qubit into a classical bit."
        );
        put(
            OperationKind.RESET,
            "Сброс qubit в состояние |0>.",
            "Resets a qubit to |0>."
        );
        put(
            OperationKind.BARRIER,
            "Барьер для группы quantum references.",
            "Barrier for a group of quantum references."
        );
        put(
            OperationKind.CONTROLLED,
            "Quantum-controlled операция поверх вложенной операции.",
            "Quantum-controlled wrapper over a nested operation."
        );
        put(
            OperationKind.CLASSICAL_ASSIGNMENT,
            "Присваивание в classical expression target.",
            "Assignment into a classical expression target."
        );
        put(
            OperationKind.CLASSICAL_DECLARATION,
            "Локальное classical declaration внутри operation stream.",
            "Local classical declaration inside an operation stream."
        );
        put(
            OperationKind.CLASSICAL_ARRAY_DECLARATION,
            "Локальное declaration classical array.",
            "Local classical array declaration."
        );
        put(
            OperationKind.CALLABLE_INVOCATION,
            "Вызов callable, subroutine или extern declaration.",
            "Invocation of a callable, subroutine or extern declaration."
        );
        put(
            OperationKind.CLASSICALLY_CONTROLLED,
            "Операция под classical predicate.",
            "Operation guarded by a classical predicate."
        );
        put(
            OperationKind.BLOCK,
            "Лексический block с вложенным operation block.",
            "Lexical block with nested operations."
        );
        put(
            OperationKind.CONDITIONAL_BLOCK,
            "If/else block по classical predicate.",
            "If/else block guarded by a classical predicate."
        );
        put(
            OperationKind.FOR_LOOP,
            "Цикл по дискретному числовому диапазону.",
            "Loop over a discrete numeric range."
        );
        put(
            OperationKind.SYMBOLIC_FOR_LOOP,
            "Цикл с symbolic/runtime границами.",
            "Loop with symbolic or runtime bounds."
        );
        put(
            OperationKind.WHILE_LOOP,
            "Цикл с classical predicate продолжения.",
            "Loop with a classical continuation predicate."
        );
        put(
            OperationKind.DELAY,
            "Timing delay на quantum references.",
            "Timing delay on quantum references."
        );
        put(
            OperationKind.TIMING_BOX,
            "Timing box с вложенными операциями и опциональной duration.",
            "Timing box with nested operations and optional duration."
        );
        put(
            OperationKind.LABEL,
            "Label для branch/control-flow навигации.",
            "Label for branch and control-flow navigation."
        );
        put(
            OperationKind.BRANCH,
            "Branch к label, включая conditional branch.",
            "Branch to a label, including conditional branch."
        );
        put(
            OperationKind.HALT,
            "Остановка выполнения программы.",
            "Stops program execution."
        );
        put(
            OperationKind.WAIT,
            "Ожидание runtime/backend события.",
            "Waits for a runtime or backend event."
        );
    }

    public String description(final OperationKind kind) {
        return description(
            kind,
            true
        );
    }

    public String description(
        final OperationKind kind,
        final boolean russian
    ) {
        final String description = descriptions(russian).get(kind);
        if (description == null) {
            throw new IllegalStateException("Desktop IR operation surface is missing description for " + kind + ".");
        }
        return description;
    }

    public Map<OperationKind, String> descriptions() {
        return descriptions(true);
    }

    public Map<OperationKind, String> descriptions(final boolean russian) {
        return Map.copyOf(russian
            ? russianDescriptions
            : englishDescriptions);
    }

    public String render() {
        return render(true);
    }

    public String render(final boolean russian) {
        final StringBuilder text = new StringBuilder();
        text.append(russian
            ? "Полная поверхность Quantum IR"
            : "Full Quantum IR Surface").append(System.lineSeparator());
        text.append(russian
            ? "  Все операции ниже поддерживаются desktop через Native JSON/API workflow."
            : "  Every operation below is supported by the desktop through the Native JSON/API workflow.")
            .append(System.lineSeparator());
        text.append(russian
            ? "  Gate-flow canvas остается интерактивной проекцией, а не ограничением IR."
            : "  The gate-flow canvas is an interactive projection, not a limit of the IR.")
            .append(System.lineSeparator());
        text.append(System.lineSeparator());
        final OperationKind[] kinds = OperationKind.values();
        for (int i = 0; i < kinds.length; i++) {
            text.append(kinds[i].name())
                .append(System.lineSeparator())
                .append("  ")
                .append(description(
                    kinds[i],
                    russian
                ))
                .append(System.lineSeparator());
        }
        return text.toString();
    }

    private void put(
        final OperationKind kind,
        final String russianDescription,
        final String englishDescription
    ) {
        russianDescriptions.put(
            kind,
            russianDescription
        );
        englishDescriptions.put(
            kind,
            englishDescription
        );
    }
}