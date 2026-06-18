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
            case "S", "SDG" -> gateInfo(
                gate,
                "Quarter-turn phase gate. SDG is the inverse S gate.",
                "1 qubit",
                "No parameters"
            );
            case "T", "TDG" -> gateInfo(
                gate,
                "Eighth-turn phase gate. TDG is the inverse T gate.",
                "1 qubit",
                "No parameters"
            );
            case "ID" -> gateInfo(
                "ID",
                "Identity gate. Preserves the qubit state without changing it.",
                "1 qubit",
                "No parameters"
            );
            case "RX", "RY", "RZ", "PHASE" -> gateInfo(
                gate,
                "Parameterized one-qubit rotation/phase operation.",
                "1 qubit",
                "Uses Angle field"
            );
            case "U" -> gateInfo(
                "U",
                "Generic one-qubit unitary gate.",
                "1 qubit",
                "Uses Angle, Phi and Lambda fields"
            );
            case "CX", "CY", "CZ", "CH" -> gateInfo(
                gate,
                "Controlled two-qubit operation. In graphical placement, click control first and target second.",
                "2 qubits",
                "No parameters"
            );
            case "CPHASE" -> gateInfo(
                "CPHASE",
                "Controlled phase operation. Qubit A is control, Qubit B is target.",
                "2 qubits",
                "Uses Angle field"
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
            case "DELAY" -> gateInfo(
                "DELAY",
                "Timing delay on selected qubits. Ideal state-vector simulation treats it as time metadata.",
                "1 or more qubits in IR; desktop shortcut uses Qubit A/B",
                "Uses Duration and unit"
            );
            case "LABEL", "WAIT", "HALT" -> gateInfo(
                gate,
                "Instruction-level control-flow operation. Some export targets reject it without semantic loss.",
                "No quantum effect",
                "LABEL uses Label field; WAIT/HALT have no parameters"
            );
            case "BRANCH" -> gateInfo(
                "BRANCH",
                "Instruction-level branch to a named label.",
                "No quantum effect",
                "Uses Label field as branch target"
            );
            case "TIMING_BOX" -> gateInfo(
                "TIMING_BOX",
                "Timing box with explicit duration. Desktop shortcut creates an empty timing region that can be represented in native IR.",
                "No direct quantum effect until body operations are added through JSON/API",
                "Uses Duration and unit"
            );
            case "ASSIGN" -> gateInfo(
                "ASSIGN",
                "Classical assignment shortcut. Writes the rounded Angle value into the selected classical bit.",
                "Classical operation",
                "Classical selects the target bit; Angle is rounded to an integer value"
            );
            case "DECLARE" -> gateInfo(
                "DECLARE",
                "Local classical variable declaration shortcut.",
                "Classical operation",
                "Label is the variable name; Angle is the initializer"
            );
            case "ARRAY" -> gateInfo(
                "ARRAY",
                "Local classical array declaration shortcut.",
                "Classical operation",
                "Label is the array name; Angle is rounded to the first dimension"
            );
            case "CALL" -> gateInfo(
                "CALL",
                "Callable invocation shortcut without return value and without arguments.",
                "Callable operation",
                "Label is the callable name"
            );
            case "IF_X" -> gateInfo(
                "IF_X",
                "Predicate-controlled X gate shortcut.",
                "1 qubit + 1 classical bit",
                "Classical selects the predicate bit; Angle is the expected integer value"
            );
            case "CTRL_X" -> gateInfo(
                "CTRL_X",
                "Register-condition controlled X gate shortcut.",
                "1 qubit + classical register condition",
                "Classical selects the register through its bit reference; Angle is the expected register value"
            );
            case "BLOCK" -> gateInfo(
                "BLOCK",
                "Empty scoped operation block. Non-empty blocks are preserved through native JSON/API.",
                "Structured operation",
                "No parameters"
            );
            case "IF_BLOCK" -> gateInfo(
                "IF_BLOCK",
                "Empty conditional block shortcut. Non-empty then/else blocks are preserved through native JSON/API.",
                "Structured operation",
                "Classical selects the predicate bit; Angle is the expected integer value"
            );
            case "FOR" -> gateInfo(
                "FOR",
                "Empty integer for-loop shortcut.",
                "Structured operation",
                "Label is the loop variable; Angle is rounded to the inclusive end value"
            );
            case "SYM_FOR" -> gateInfo(
                "SYM_FOR",
                "Empty symbolic/runtime for-loop shortcut with expression bounds.",
                "Structured operation",
                "Label is the loop variable; Angle is converted to the end expression"
            );
            case "WHILE" -> gateInfo(
                "WHILE",
                "While-loop shortcut. Classical selects the condition bit; Angle sets the expected integer value.",
                "Structured operation",
                "Uses Classical and Angle fields"
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
            case "S", "SDG" -> gateInfo(
                gate,
                "Фазовый поворот на pi/2. SDG является обратной операцией S.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "T", "TDG" -> gateInfo(
                gate,
                "Фазовый поворот на pi/4. TDG является обратной операцией T.",
                "1 кубит",
                "Без параметров",
                true
            );
            case "ID" -> gateInfo(
                "ID",
                "Identity gate. Сохраняет состояние кубита без изменения.",
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
            case "U" -> gateInfo(
                "U",
                "Общая однокубитная unitary-операция.",
                "1 кубит",
                "Использует поля Угол, Phi и Lambda",
                true
            );
            case "CX", "CY", "CZ", "CH" -> gateInfo(
                gate,
                "Управляемая двухкубитная операция. В визуальном режиме сначала выбирается control, затем target.",
                "2 кубита",
                "Без параметров",
                true
            );
            case "CPHASE" -> gateInfo(
                "CPHASE",
                "Управляемая фазовая операция. Qubit A — control, Qubit B — target.",
                "2 кубита",
                "Использует поле Угол",
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
            case "DELAY" -> gateInfo(
                "DELAY",
                "Timing delay на выбранных кубитах. Идеальная state-vector симуляция учитывает его как временную метку без изменения состояния.",
                "В IR 1 или больше кубитов; desktop shortcut использует Qubit A/B",
                "Использует длительность и единицу времени",
                true
            );
            case "LABEL", "WAIT", "HALT" -> gateInfo(
                gate,
                "Операция instruction-level control flow. Некоторые target-форматы честно отклоняют её без потери семантики.",
                "Не меняет quantum state",
                "LABEL использует поле Label; WAIT/HALT без параметров",
                true
            );
            case "BRANCH" -> gateInfo(
                "BRANCH",
                "Instruction-level переход к именованному label.",
                "Не меняет quantum state",
                "Использует поле Label как цель перехода",
                true
            );
            case "TIMING_BOX" -> gateInfo(
                "TIMING_BOX",
                "Timing box с явной длительностью. Desktop shortcut создает пустую временную область, представимую в native IR.",
                "Не меняет quantum state, пока тело box пустое",
                "Использует длительность и единицу времени",
                true
            );
            case "ASSIGN" -> gateInfo(
                "ASSIGN",
                "Classical assignment shortcut. Записывает округленное значение поля Angle в выбранный classical bit.",
                "Classical operation",
                "Classical выбирает целевой bit; Angle округляется до integer",
                true
            );
            case "DECLARE" -> gateInfo(
                "DECLARE",
                "Локальное classical declaration для native IR.",
                "Classical operation",
                "Label задает имя; Angle задает initializer",
                true
            );
            case "ARRAY" -> gateInfo(
                "ARRAY",
                "Локальное classical array declaration.",
                "Classical operation",
                "Label задает имя массива; Angle задает размер",
                true
            );
            case "CALL" -> gateInfo(
                "CALL",
                "Вызов callable/subroutine/extern без return value и без аргументов.",
                "Callable operation",
                "Label задает имя callable",
                true
            );
            case "IF_X" -> gateInfo(
                "IF_X",
                "Classically controlled X shortcut: X выполняется, когда predicate истинен.",
                "1 qubit + 1 classical bit",
                "Classical выбирает bit; Angle задает ожидаемое значение",
                true
            );
            case "CTRL_X" -> gateInfo(
                "CTRL_X",
                "Register-condition controlled X shortcut.",
                "1 qubit + classical register condition",
                "Classical выбирает register через bit reference; Angle задает expected value",
                true
            );
            case "BLOCK" -> gateInfo(
                "BLOCK",
                "Пустой scoped block. Непустые block-тела сохраняются через native JSON/API.",
                "Structured operation",
                "Без параметров",
                true
            );
            case "IF_BLOCK" -> gateInfo(
                "IF_BLOCK",
                "Пустой conditional block. Непустые then/else тела сохраняются через native JSON/API.",
                "Structured operation",
                "Classical выбирает bit; Angle задает expected value",
                true
            );
            case "FOR" -> gateInfo(
                "FOR",
                "Пустой integer for-loop shortcut.",
                "Structured operation",
                "Label задает имя переменной; Angle задает inclusive end",
                true
            );
            case "SYM_FOR" -> gateInfo(
                "SYM_FOR",
                "Пустой symbolic/runtime for-loop с expression-границами.",
                "Structured operation",
                "Label задает имя переменной; Angle задает end expression",
                true
            );
            case "WHILE" -> gateInfo(
                "WHILE",
                "While-loop shortcut. Classical выбирает bit условия; Angle задает ожидаемое целое значение.",
                "Structured operation",
                "Использует поля Classical и Angle",
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