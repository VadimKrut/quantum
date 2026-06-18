/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.domain;

import java.util.List;

/**
 * Полное описание записи библиотеки без самой исполняемой логики генерации.
 */
public final class QuantumAlgorithmDescriptor {

    private final String id;
    private final String displayName;
    private final String summary;
    private final AlgorithmCategory category;
    private final AlgorithmDifficulty difficulty;
    private final List<String> tags;
    private final List<String> referenceUris;
    private final List<AlgorithmParameterDefinition> parameters;

    /**
     * Создает описание алгоритма.
     *
     * @param id стабильный идентификатор
     * @param displayName пользовательское имя
     * @param summary краткое описание
     * @param category категория
     * @param difficulty уровень сложности
     * @param tags теги поиска
     * @param parameters параметры генератора
     */
    public QuantumAlgorithmDescriptor(
        final String id,
        final String displayName,
        final String summary,
        final AlgorithmCategory category,
        final AlgorithmDifficulty difficulty,
        final List<String> tags,
        final List<AlgorithmParameterDefinition> parameters
    ) {
        this(
            id,
            displayName,
            summary,
            category,
            difficulty,
            tags,
            List.of(),
            parameters
        );
    }

    /**
     * Создает описание алгоритма с внешними reference-ссылками.
     *
     * @param id стабильный идентификатор
     * @param displayName пользовательское имя
     * @param summary краткое описание
     * @param category категория
     * @param difficulty уровень сложности
     * @param tags теги поиска
     * @param referenceUris ссылки на reference-источники
     * @param parameters параметры генератора
     */
    public QuantumAlgorithmDescriptor(
        final String id,
        final String displayName,
        final String summary,
        final AlgorithmCategory category,
        final AlgorithmDifficulty difficulty,
        final List<String> tags,
        final List<String> referenceUris,
        final List<AlgorithmParameterDefinition> parameters
    ) {
        validateText(
            id,
            "Algorithm id"
        );
        validateText(
            displayName,
            "Algorithm display name"
        );
        validateText(
            summary,
            "Algorithm summary"
        );
        if (category == null) {
            throw new IllegalArgumentException("Algorithm category must not be null.");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("Algorithm difficulty must not be null.");
        }
        validateStrings(
            tags,
            "Algorithm tag",
            "Algorithm tags"
        );
        validateStrings(
            referenceUris,
            "Algorithm reference URI",
            "Algorithm reference URIs"
        );
        validateParameters(parameters);
        this.id = id;
        this.displayName = displayName;
        this.summary = summary;
        this.category = category;
        this.difficulty = difficulty;
        this.tags = List.copyOf(tags);
        this.referenceUris = List.copyOf(referenceUris);
        this.parameters = List.copyOf(parameters);
    }

    /**
     * Возвращает стабильный идентификатор.
     *
     * @return идентификатор
     */
    public String id() {
        return id;
    }

    /**
     * Возвращает пользовательское имя.
     *
     * @return пользовательское имя
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Возвращает краткое описание.
     *
     * @return краткое описание
     */
    public String summary() {
        return summary;
    }

    /**
     * Возвращает категорию.
     *
     * @return категория
     */
    public AlgorithmCategory category() {
        return category;
    }

    /**
     * Возвращает уровень сложности.
     *
     * @return уровень сложности
     */
    public AlgorithmDifficulty difficulty() {
        return difficulty;
    }

    /**
     * Возвращает immutable список тегов.
     *
     * @return теги
     */
    public List<String> tags() {
        return tags;
    }

    /**
     * Возвращает immutable список reference-ссылок.
     *
     * @return reference-ссылки
     */
    public List<String> referenceUris() {
        return referenceUris;
    }

    /**
     * Возвращает immutable список параметров.
     *
     * @return параметры
     */
    public List<AlgorithmParameterDefinition> parameters() {
        return parameters;
    }

    /**
     * Возвращает параметр по имени.
     *
     * @param name имя параметра
     * @return описание параметра
     */
    public AlgorithmParameterDefinition parameter(final String name) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).name().equals(name)) {
                return parameters.get(i);
            }
        }
        throw new IllegalArgumentException("Unknown algorithm parameter: " + name + ".");
    }

    private static void validateStrings(
        final List<String> values,
        final String itemSubject,
        final String listSubject
    ) {
        if (values == null) {
            throw new IllegalArgumentException(listSubject + " must not be null.");
        }
        for (int i = 0; i < values.size(); i++) {
            validateText(
                values.get(i),
                itemSubject
            );
        }
    }

    private static void validateParameters(final List<AlgorithmParameterDefinition> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Algorithm parameters must not be null.");
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) == null) {
                throw new IllegalArgumentException("Algorithm parameter definition must not be null.");
            }
            for (int j = i + 1; j < parameters.size(); j++) {
                if (parameters.get(i).name().equals(parameters.get(j).name())) {
                    throw new IllegalArgumentException("Algorithm parameter name is duplicated.");
                }
            }
        }
    }

    private static void validateText(
        final String value,
        final String subject
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            throw new IllegalArgumentException(subject + " must not be blank.");
        }
    }
}