/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

/**
 * Код ошибки доменной валидации Quantum IR.
 */
public enum ValidationErrorCode {

    /**
     * Программа не передана валидатору.
     */
    NULL_PROGRAM,

    /**
     * Вычислительная модель пока не поддерживается валидатором gate-based схем.
     */
    UNSUPPORTED_COMPUTATION_MODEL,

    /**
     * Схема не принадлежит валидируемой программе.
     */
    CIRCUIT_DOES_NOT_BELONG_TO_PROGRAM,

    /**
     * В схеме найден конфликт имен регистров.
     */
    DUPLICATE_REGISTER_NAME,

    /**
     * Имя gate definition конфликтует с уже известным gate.
     */
    GATE_DEFINITION_NAME_CONFLICT,

    /**
     * Gate definition некорректен.
     */
    INVALID_GATE_DEFINITION,

    /**
     * Тело composite gate использует не объявленный qubit argument.
     */
    INVALID_GATE_BODY_QUBIT,

    /**
     * Тело composite gate использует не объявленный parameter symbol.
     */
    INVALID_GATE_BODY_PARAMETER,

    /**
     * Тело composite gate ссылается на не объявленный custom gate.
     */
    UNDECLARED_GATE_DEFINITION,

    /**
     * Gate definitions образуют цикл.
     */
    CYCLIC_GATE_DEFINITION,

    /**
     * Размер регистра некорректен.
     */
    INVALID_REGISTER_SIZE,

    /**
     * Кубит операции не принадлежит текущей схеме.
     */
    QUBIT_DOES_NOT_BELONG_TO_CIRCUIT,

    /**
     * Классический бит операции не принадлежит текущей схеме.
     */
    CLASSICAL_BIT_DOES_NOT_BELONG_TO_CIRCUIT,

    /**
     * Классический регистр условия не принадлежит текущей схеме.
     */
    CLASSICAL_REGISTER_DOES_NOT_BELONG_TO_CIRCUIT,

    /**
     * Значение классического условия выходит за размер регистра.
     */
    CLASSICAL_CONDITION_VALUE_OUT_OF_RANGE,

    /**
     * Количество кубитов операции не совпадает с arity гейта.
     */
    INVALID_GATE_ARITY,

    /**
     * Количество параметров операции не совпадает с описанием гейта.
     */
    INVALID_GATE_PARAMETER_COUNT,

    /**
     * Параметрическое выражение операции гейта некорректно.
     */
    INVALID_GATE_PARAMETER,

    UNDECLARED_CALLABLE,

    INVALID_CALLABLE_ARGUMENT_COUNT,

    INVALID_CALLABLE_TARGET,

    /**
     * Операция гейта использует один и тот же кубит в несовместимых ролях.
     */
    DUPLICATE_QUBIT_IN_GATE_OPERATION,

    /**
     * Операция не поддерживается gate-based моделью.
     */
    OPERATION_NOT_SUPPORTED_BY_GATE_BASED_MODEL
}