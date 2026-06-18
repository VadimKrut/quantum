/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.catalog;

import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Immutable-реестр алгоритмов с быстрым доступом по id и фильтрацией.
 */
public final class QuantumAlgorithmRegistry {

    private final List<QuantumAlgorithmEntry> entries;
    private final LinkedHashMap<String, QuantumAlgorithmEntry> entriesById;

    /**
     * Создает реестр алгоритмов.
     *
     * @param entries записи библиотеки
     */
    public QuantumAlgorithmRegistry(final List<QuantumAlgorithmEntry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("Algorithm entries must not be null.");
        }
        final ArrayList<QuantumAlgorithmEntry> entrySnapshot = new ArrayList<>(entries.size());
        final LinkedHashMap<String, QuantumAlgorithmEntry> byId = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            final QuantumAlgorithmEntry entry = entries.get(i);
            if (entry == null) {
                throw new IllegalArgumentException("Algorithm entry must not be null.");
            }
            if (byId.containsKey(entry.descriptor().id())) {
                throw new IllegalArgumentException("Algorithm id is duplicated: " + entry.descriptor().id() + ".");
            }
            entrySnapshot.add(entry);
            byId.put(
                entry.descriptor().id(),
                entry
            );
        }
        this.entries = List.copyOf(entrySnapshot);
        this.entriesById = byId;
    }

    /**
     * Возвращает количество записей.
     *
     * @return количество записей
     */
    public int size() {
        return entries.size();
    }

    /**
     * Возвращает все записи.
     *
     * @return immutable список записей
     */
    public List<QuantumAlgorithmEntry> entries() {
        return entries;
    }

    /**
     * Проверяет наличие записи.
     *
     * @param id идентификатор
     * @return true, если запись есть
     */
    public boolean contains(final String id) {
        return entriesById.containsKey(id);
    }

    /**
     * Возвращает запись по id.
     *
     * @param id идентификатор
     * @return запись библиотеки
     */
    public QuantumAlgorithmEntry get(final String id) {
        final QuantumAlgorithmEntry entry = entriesById.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown algorithm id: " + id + ".");
        }
        return entry;
    }

    /**
     * Ищет записи по запросу.
     *
     * @param query запрос
     * @return найденные записи
     */
    public List<QuantumAlgorithmEntry> search(final QuantumAlgorithmQuery query) {
        final QuantumAlgorithmQuery effectiveQuery = query == null ? QuantumAlgorithmQuery.all() : query;
        final ArrayList<QuantumAlgorithmEntry> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            final QuantumAlgorithmEntry entry = entries.get(i);
            if (effectiveQuery.matches(entry)) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Создает builder реестра.
     *
     * @return builder реестра
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder для расширяемого реестра алгоритмов.
     */
    public static final class Builder {

        private final ArrayList<QuantumAlgorithmEntry> entries;

        private Builder() {
            entries = new ArrayList<>();
        }

        /**
         * Добавляет запись.
         *
         * @param entry запись библиотеки
         * @return текущий builder
         */
        public Builder add(final QuantumAlgorithmEntry entry) {
            entries.add(entry);
            return this;
        }

        /**
         * Добавляет все записи другого реестра.
         *
         * @param registry реестр
         * @return текущий builder
         */
        public Builder addAll(final QuantumAlgorithmRegistry registry) {
            if (registry == null) {
                throw new IllegalArgumentException("Algorithm registry must not be null.");
            }
            for (int i = 0; i < registry.entries().size(); i++) {
                entries.add(registry.entries().get(i));
            }
            return this;
        }

        /**
         * Создает immutable-реестр.
         *
         * @return реестр алгоритмов
         */
        public QuantumAlgorithmRegistry build() {
            return new QuantumAlgorithmRegistry(entries);
        }
    }
}