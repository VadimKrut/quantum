/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

/**
 * Рендерит краткую desktop-документацию по gates, доступным в visual builder.
 */
public final class DesktopGateInfoRenderer {

    public String render(
        final String gate,
        final boolean russian
    ) {
        if (russian) {
            return renderRussian(gate);
        }
        return renderEnglish(gate);
    }

    private static String renderEnglish(final String gate) {
        return switch (gate) {
            case "H" -> gateInfo(
                "H",
                "Hadamard gate. Creates superposition on one qubit.",
                "1 qubit",
                "No parameters"
            );
            case "X" -> gateInfo(
                "X",
                "Pauli-X bit-flip gate.",
                "1 qubit",
                "No parameters"
            );
            case "Y" -> gateInfo(
                "Y",
                "Pauli-Y gate.",
                "1 qubit",
                "No parameters"
            );
            case "Z" -> gateInfo(
                "Z",
                "Pauli-Z phase-flip gate.",
                "1 qubit",
                "No parameters"
            );
            case "S" -> gateInfo(
                "S",
                "Quarter-turn phase gate.",
                "1 qubit",
                "No parameters"
            );
            case "T" -> gateInfo(
                "T",
                "Eighth-turn phase gate.",
                "1 qubit",
                "No parameters"
            );
            case "RX", "RY", "RZ", "PHASE" -> gateInfo(
                gate,
                "Parameterized one-qubit rotation/phase operation.",
                "1 qubit",
                "Uses Angle field"
            );
            case "CX", "CY", "CZ", "CH" -> gateInfo(
                gate,
                "Controlled two-qubit operation. In graphical placement, click control first and target second.",
                "2 qubits",
                "No parameters"
            );
            case "SWAP" -> gateInfo(
                "SWAP",
                "Swaps two qubit states.",
                "2 qubits",
                "No parameters"
            );
            case "CCX" -> gateInfo(
                "CCX",
                "Toffoli gate. In graphical placement, click two controls and then target.",
                "3 qubits",
                "No parameters"
            );
            case "MEASURE" -> gateInfo(
                "MEASURE",
                "Measures a qubit into the selected classical bit.",
                "1 qubit + 1 classical bit",
                "No parameters"
            );
            case "RESET" -> gateInfo(
                "RESET",
                "Resets one qubit to the zero state.",
                "1 qubit",
                "No parameters"
            );
            case "BARRIER" -> gateInfo(
                "BARRIER",
                "Visualization/scheduling barrier for selected qubits.",
                "2 qubits in current desktop shortcut",
                "No parameters"
            );
            default -> gateInfo(
                gate,
                "Unknown desktop gate.",
                "Unknown",
                "Unknown"
            );
        };
    }

    private static String renderRussian(final String gate) {
        return switch (gate) {
            case "H" -> gateInfo(
                "H",
                "Hadamard. Создает суперпозицию одного кубита.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "X" -> gateInfo(
                "X",
                "Pauli-X. Инвертирует вычислительное состояние кубита.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "Y" -> gateInfo(
                "Y",
                "Pauli-Y. Выполняет bit-flip с фазовым множителем.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "Z" -> gateInfo(
                "Z",
                "Pauli-Z. Меняет фазу состояния |1>.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "S" -> gateInfo(
                "S",
                "Фазовый поворот на pi/2.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "T" -> gateInfo(
                "T",
                "Фазовый поворот на pi/4.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "RX", "RY", "RZ", "PHASE" -> gateInfo(
                gate,
                "Параметризованный поворот или фазовая операция одного кубита.",
                "1 кубит",
                "Использует поле Angle",
                true
            );
            case "CX", "CY", "CZ", "CH" -> gateInfo(
                gate,
                "Управляемая двухкубитная операция. В визуальном режиме сначала выбирается control, затем target.",
                "2 кубита",
                "Без параметров",
                true
            );
            case "SWAP" -> gateInfo(
                "SWAP",
                "Меняет местами состояния двух кубитов.",
                "2 кубита",
                "Без параметров",
                true
            );
            case "CCX" -> gateInfo(
                "CCX",
                "Toffoli. Два control-кубита и один target-кубит.",
                "3 кубита",
                "Без параметров",
                true
            );
            case "MEASURE" -> gateInfo(
                "MEASURE",
                "Измеряет кубит и записывает результат в выбранный classical bit.",
                "1 кубит + 1 classical bit",
                "Без параметров",
                true
            );
            case "RESET" -> gateInfo(
                "RESET",
                "Сбрасывает кубит в состояние |0>.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "BARRIER" -> gateInfo(
                "BARRIER",
                "Барьер визуализации и планирования для выбранных кубитов.",
                "Текущая desktop-команда применяет 2 кубита",
                "Без параметров",
                true
            );
            default -> gateInfo(
                gate,
                "Неизвестный gate desktop-палитры.",
                "Неизвестно",
                "Неизвестно",
                true
            );
        };
    }

    private static String gateInfo(
        final String gate,
        final String description,
        final String arity,
        final String parameters
    ) {
        return gateInfo(
            gate,
            description,
            arity,
            parameters,
            false
        );
    }

    private static String gateInfo(
        final String gate,
        final String description,
        final String arity,
        final String parameters,
        final boolean russian
    ) {
        if (russian) {
            return "Gate: " + gate + System.lineSeparator()
                + "Описание: " + description + System.lineSeparator()
                + "Арность: " + arity + System.lineSeparator()
                + "Параметры: " + parameters + System.lineSeparator()
                + "Модель: родная операция Quantum IR, создаваемая через Java DSL или визуальный builder.";
        }
        return "Gate: " + gate + System.lineSeparator()
            + "Description: " + description + System.lineSeparator()
            + "Arity: " + arity + System.lineSeparator()
            + "Parameters: " + parameters + System.lineSeparator()
            + "Model: native Quantum IR operation generated through Java DSL.";
    }
}