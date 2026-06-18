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

    private final EnumMap<OperationKind, String> descriptions = new EnumMap<>(OperationKind.class);

    public DesktopIrOperationSurfaceCatalog() {
        descriptions.put(
            OperationKind.GATE,
            "Квантовый gate: стандартный, custom, parameterized или modified."
        );
        descriptions.put(
            OperationKind.MEASURE,
            "Измерение qubit в classical bit."
        );
        descriptions.put(
            OperationKind.RESET,
            "Сброс qubit в состояние |0>."
        );
        descriptions.put(
            OperationKind.BARRIER,
            "Барьер для группы quantum references."
        );
        descriptions.put(
            OperationKind.CONTROLLED,
            "Quantum-controlled операция поверх вложенной операции."
        );
        descriptions.put(
            OperationKind.CLASSICAL_ASSIGNMENT,
            "Присваивание в classical expression target."
        );
        descriptions.put(
            OperationKind.CLASSICAL_DECLARATION,
            "Локальное classical declaration внутри operation stream."
        );
        descriptions.put(
            OperationKind.CLASSICAL_ARRAY_DECLARATION,
            "Локальное declaration classical array."
        );
        descriptions.put(
            OperationKind.CALLABLE_INVOCATION,
            "Вызов callable, subroutine или extern declaration."
        );
        descriptions.put(
            OperationKind.CLASSICALLY_CONTROLLED,
            "Операция под classical predicate."
        );
        descriptions.put(
            OperationKind.BLOCK,
            "Лексический block с вложенным operation block."
        );
        descriptions.put(
            OperationKind.CONDITIONAL_BLOCK,
            "If/else block по classical predicate."
        );
        descriptions.put(
            OperationKind.FOR_LOOP,
            "Цикл по дискретному числовому диапазону."
        );
        descriptions.put(
            OperationKind.SYMBOLIC_FOR_LOOP,
            "Цикл с symbolic/runtime границами."
        );
        descriptions.put(
            OperationKind.WHILE_LOOP,
            "Цикл с classical predicate продолжения."
        );
        descriptions.put(
            OperationKind.DELAY,
            "Timing delay на quantum references."
        );
        descriptions.put(
            OperationKind.TIMING_BOX,
            "Timing box с вложенными операциями и опциональной duration."
        );
        descriptions.put(
            OperationKind.LABEL,
            "Label для branch/control-flow навигации."
        );
        descriptions.put(
            OperationKind.BRANCH,
            "Branch к label, включая conditional branch."
        );
        descriptions.put(
            OperationKind.HALT,
            "Остановка выполнения программы."
        );
        descriptions.put(
            OperationKind.WAIT,
            "Ожидание runtime/backend события."
        );
    }

    public String description(final OperationKind kind) {
        final String description = descriptions.get(kind);
        if (description == null) {
            throw new IllegalStateException("Desktop IR operation surface is missing description for " + kind + ".");
        }
        return description;
    }

    public Map<OperationKind, String> descriptions() {
        return Map.copyOf(descriptions);
    }

    public String render() {
        final StringBuilder text = new StringBuilder();
        text.append("Полная поверхность Quantum IR").append(System.lineSeparator());
        text.append("  Все операции ниже поддерживаются desktop через Native JSON/API workflow.").append(System.lineSeparator());
        text.append("  Gate-flow canvas остается интерактивной проекцией, а не ограничением IR.").append(System.lineSeparator());
        text.append(System.lineSeparator());
        final OperationKind[] kinds = OperationKind.values();
        for (int i = 0; i < kinds.length; i++) {
            text.append(kinds[i].name())
                .append(System.lineSeparator())
                .append("  ")
                .append(description(kinds[i]))
                .append(System.lineSeparator());
        }
        return text.toString();
    }
}