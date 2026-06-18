/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.catalog;

import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmDescriptor;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;

/**
 * Неизменяемый фильтр поиска по библиотеке алгоритмов.
 */
public final class QuantumAlgorithmQuery {

    private final String text;
    private final AlgorithmCategory category;
    private final AlgorithmDifficulty difficulty;
    private final String tag;

    private QuantumAlgorithmQuery(
        final String text,
        final AlgorithmCategory category,
        final AlgorithmDifficulty difficulty,
        final String tag
    ) {
        this.text = text == null ? "" : text.strip().toLowerCase();
        this.category = category;
        this.difficulty = difficulty;
        this.tag = tag == null ? "" : tag.strip().toLowerCase();
    }

    /**
     * Создает пустой запрос.
     *
     * @return пустой запрос
     */
    public static QuantumAlgorithmQuery all() {
        return new QuantumAlgorithmQuery(
            "",
            null,
            null,
            ""
        );
    }

    /**
     * Создает запрос по тексту.
     *
     * @param text текст поиска
     * @return запрос
     */
    public static QuantumAlgorithmQuery text(final String text) {
        return all().withText(text);
    }

    /**
     * Возвращает копию с текстом поиска.
     *
     * @param value текст поиска
     * @return запрос
     */
    public QuantumAlgorithmQuery withText(final String value) {
        return new QuantumAlgorithmQuery(
            value,
            category,
            difficulty,
            tag
        );
    }

    /**
     * Возвращает копию с категорией.
     *
     * @param value категория
     * @return запрос
     */
    public QuantumAlgorithmQuery withCategory(final AlgorithmCategory value) {
        return new QuantumAlgorithmQuery(
            text,
            value,
            difficulty,
            tag
        );
    }

    /**
     * Возвращает копию с уровнем сложности.
     *
     * @param value уровень сложности
     * @return запрос
     */
    public QuantumAlgorithmQuery withDifficulty(final AlgorithmDifficulty value) {
        return new QuantumAlgorithmQuery(
            text,
            category,
            value,
            tag
        );
    }

    /**
     * Возвращает копию с тегом.
     *
     * @param value тег
     * @return запрос
     */
    public QuantumAlgorithmQuery withTag(final String value) {
        return new QuantumAlgorithmQuery(
            text,
            category,
            difficulty,
            value
        );
    }

    /**
     * Проверяет запись библиотеки на соответствие фильтрам.
     *
     * @param entry запись библиотеки
     * @return true, если запись подходит
     */
    public boolean matches(final QuantumAlgorithmEntry entry) {
        final QuantumAlgorithmDescriptor descriptor = entry.descriptor();
        return matchesCategory(descriptor)
            && matchesDifficulty(descriptor)
            && matchesTag(descriptor)
            && matchesText(descriptor);
    }

    private boolean matchesCategory(final QuantumAlgorithmDescriptor descriptor) {
        return category == null || descriptor.category() == category;
    }

    private boolean matchesDifficulty(final QuantumAlgorithmDescriptor descriptor) {
        return difficulty == null || descriptor.difficulty() == difficulty;
    }

    private boolean matchesTag(final QuantumAlgorithmDescriptor descriptor) {
        if (tag.isEmpty()) {
            return true;
        }
        for (int i = 0; i < descriptor.tags().size(); i++) {
            if (descriptor.tags().get(i).toLowerCase().equals(tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesText(final QuantumAlgorithmDescriptor descriptor) {
        if (text.isEmpty()) {
            return true;
        }
        if (descriptor.id().toLowerCase().contains(text)) {
            return true;
        }
        if (descriptor.displayName().toLowerCase().contains(text)) {
            return true;
        }
        if (descriptor.summary().toLowerCase().contains(text)) {
            return true;
        }
        for (int i = 0; i < descriptor.tags().size(); i++) {
            if (descriptor.tags().get(i).toLowerCase().contains(text)) {
                return true;
            }
        }
        return false;
    }
}