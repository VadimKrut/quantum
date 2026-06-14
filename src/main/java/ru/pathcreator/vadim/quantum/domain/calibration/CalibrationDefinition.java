/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.calibration;

import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Универсальное описание калибровки для внешнего target-уровня.
 */
public final class CalibrationDefinition {

    /**
     * Имя калибруемой операции или gate.
     */
    private final IdentifierName targetName;

    /**
     * Имена параметров калибровки.
     */
    private final List<String> parameterNames;

    /**
     * Имена квантовых аргументов калибровки.
     */
    private final List<String> qubitNames;

    /**
     * Идентификатор языка или backend-представления тела.
     */
    private final IdentifierName bodyLanguage;

    /**
     * Тело калибровки как непрозрачный текст выбранного языка.
     */
    private final String body;

    /**
     * Создает калибровку.
     *
     * @param targetName имя калибруемой операции
     * @param parameterNames имена параметров
     * @param qubitNames имена квантовых аргументов
     * @param bodyLanguage язык тела
     * @param body тело калибровки
     */
    public CalibrationDefinition(
        final String targetName,
        final List<String> parameterNames,
        final List<String> qubitNames,
        final String bodyLanguage,
        final String body
    ) {
        if (parameterNames == null) {
            throw new IllegalArgumentException("Calibration parameter names must not be null.");
        }
        if (qubitNames == null) {
            throw new IllegalArgumentException("Calibration qubit names must not be null.");
        }
        if (body == null) {
            throw new IllegalArgumentException("Calibration body must not be null.");
        }
        this.targetName = IdentifierName.of(
            targetName,
            "Calibration target"
        );
        this.parameterNames = validateNames(
            parameterNames,
            "Calibration parameter"
        );
        this.qubitNames = validateNames(
            qubitNames,
            "Calibration qubit"
        );
        this.bodyLanguage = IdentifierName.of(
            bodyLanguage,
            "Calibration body language"
        );
        this.body = body;
    }

    /**
     * Возвращает имя калибруемой операции.
     *
     * @return имя операции
     */
    public String targetName() {
        return targetName.value();
    }

    /**
     * Возвращает имена параметров.
     *
     * @return имена параметров
     */
    public List<String> parameterNames() {
        return parameterNames;
    }

    /**
     * Возвращает имена квантовых аргументов.
     *
     * @return имена квантовых аргументов
     */
    public List<String> qubitNames() {
        return qubitNames;
    }

    /**
     * Возвращает язык тела.
     *
     * @return язык тела
     */
    public String bodyLanguage() {
        return bodyLanguage.value();
    }

    /**
     * Возвращает тело калибровки.
     *
     * @return тело калибровки
     */
    public String body() {
        return body;
    }

    private static List<String> validateNames(
        final List<String> names,
        final String subject
    ) {
        final String[] values = new String[names.size()];
        for (int i = 0; i < names.size(); i++) {
            values[i] = IdentifierName.of(
                names.get(i),
                subject
            ).value();
        }
        return List.of(values);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CalibrationDefinition definition)) {
            return false;
        }
        return Objects.equals(
            targetName,
            definition.targetName
        )
            && Objects.equals(
                parameterNames,
                definition.parameterNames
            )
            && Objects.equals(
                qubitNames,
                definition.qubitNames
            )
            && Objects.equals(
                bodyLanguage,
                definition.bodyLanguage
            )
            && Objects.equals(
                body,
                definition.body
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            targetName,
            parameterNames,
            qubitNames,
            bodyLanguage,
            body
        );
    }
}