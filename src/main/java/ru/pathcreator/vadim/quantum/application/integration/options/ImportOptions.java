/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Настройки import из внешнего формата в Quantum IR.
 */
public final class ImportOptions {

    /**
     * Нужно ли запускать доменную валидацию после import.
     */
    private final boolean validateAfterImport;

    /**
     * Нужно ли считать предупреждения ошибками результата.
     */
    private final boolean failOnWarnings;
    private final Map<String, String> includedSources;
    private final List<String> includeDirectories;

    private ImportOptions(
        final boolean validateAfterImport,
        final boolean failOnWarnings,
        final Map<String, String> includedSources,
        final List<String> includeDirectories
    ) {
        this.validateAfterImport = validateAfterImport;
        this.failOnWarnings = failOnWarnings;
        this.includedSources = validateAndCopyIncludedSources(includedSources);
        this.includeDirectories = validateAndCopyIncludeDirectories(includeDirectories);
    }

    /**
     * Создает настройки import.
     *
     * @param validateAfterImport запускать ли доменную валидацию после import
     * @param failOnWarnings считать ли предупреждения ошибками
     * @return настройки import
     */
    public static ImportOptions of(
        final boolean validateAfterImport,
        final boolean failOnWarnings
    ) {
        return new ImportOptions(
            validateAfterImport,
            failOnWarnings,
            Map.of(),
            List.of()
        );
    }

    /**
     * Создает настройки import с известными include sources.
     *
     * @param validateAfterImport запускать ли доменную валидацию после import
     * @param failOnWarnings считать ли предупреждения ошибками
     * @param includedSources тексты include-файлов по имени include
     * @return настройки import
     */
    public static ImportOptions of(
        final boolean validateAfterImport,
        final boolean failOnWarnings,
        final Map<String, String> includedSources
    ) {
        return new ImportOptions(
            validateAfterImport,
            failOnWarnings,
            includedSources,
            List.of()
        );
    }

    /**
     * Создает настройки import с include sources и директориями include.
     *
     * @param validateAfterImport запускать ли доменную валидацию после import
     * @param failOnWarnings считать ли предупреждения ошибками
     * @param includedSources тексты include-файлов по имени include
     * @param includeDirectories директории, из которых adapter может читать include-файлы
     * @return настройки import
     */
    public static ImportOptions of(
        final boolean validateAfterImport,
        final boolean failOnWarnings,
        final Map<String, String> includedSources,
        final List<String> includeDirectories
    ) {
        return new ImportOptions(
            validateAfterImport,
            failOnWarnings,
            includedSources,
            includeDirectories
        );
    }

    /**
     * Создает настройки import по умолчанию.
     *
     * @return настройки import по умолчанию
     */
    public static ImportOptions defaults() {
        return new ImportOptions(
            true,
            false,
            Map.of(),
            List.of()
        );
    }

    /**
     * Возвращает копию настроек с добавленным include source.
     *
     * @param includeName имя include-файла
     * @param source текст include-файла
     * @return новые настройки import
     */
    public ImportOptions withIncludedSource(
        final String includeName,
        final String source
    ) {
        final LinkedHashMap<String, String> sources = new LinkedHashMap<>(includedSources);
        sources.put(
            includeName,
            source
        );
        return new ImportOptions(
            validateAfterImport,
            failOnWarnings,
            sources,
            includeDirectories
        );
    }

    /**
     * Возвращает копию настроек с добавленной include-директорией.
     *
     * @param directory путь к директории include
     * @return новые настройки import
     */
    public ImportOptions withIncludeDirectory(final String directory) {
        final ArrayList<String> directories = new ArrayList<>(includeDirectories);
        directories.add(directory);
        return new ImportOptions(
            validateAfterImport,
            failOnWarnings,
            includedSources,
            directories
        );
    }

    /**
     * Проверяет, нужно ли запускать доменную валидацию после import.
     *
     * @return true, если нужна валидация после import
     */
    public boolean validateAfterImport() {
        return validateAfterImport;
    }

    /**
     * Проверяет, нужно ли считать предупреждения ошибками.
     *
     * @return true, если предупреждения должны провалить import
     */
    public boolean failOnWarnings() {
        return failOnWarnings;
    }

    /**
     * Возвращает include sources, доступные importer.
     *
     * @return immutable include sources
     */
    public Map<String, String> includedSources() {
        return includedSources;
    }

    /**
     * Возвращает количество include sources.
     *
     * @return количество include sources
     */
    public int includedSourceCount() {
        return includedSources.size();
    }

    /**
     * Возвращает include-директории, доступные importer.
     *
     * @return immutable include-директории
     */
    public List<String> includeDirectories() {
        return includeDirectories;
    }

    /**
     * Возвращает количество include-директорий.
     *
     * @return количество include-директорий
     */
    public int includeDirectoryCount() {
        return includeDirectories.size();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImportOptions options)) {
            return false;
        }
        return validateAfterImport == options.validateAfterImport
            && failOnWarnings == options.failOnWarnings
            && Objects.equals(
                includedSources,
                options.includedSources
            )
            && Objects.equals(
                includeDirectories,
                options.includeDirectories
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            validateAfterImport,
            failOnWarnings,
            includedSources,
            includeDirectories
        );
    }

    private static Map<String, String> validateAndCopyIncludedSources(final Map<String, String> includedSources) {
        if (includedSources == null) {
            throw new IllegalArgumentException("Included sources must not be null.");
        }
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        final String[] names = includedSources.keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            final String source = includedSources.get(name);
            if (name == null) {
                throw new IllegalArgumentException("Included source name must not be null.");
            }
            if (name.isBlank()) {
                throw new IllegalArgumentException("Included source name must not be blank.");
            }
            if (source == null) {
                throw new IllegalArgumentException("Included source text must not be null.");
            }
            result.put(
                name,
                source
            );
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<String> validateAndCopyIncludeDirectories(final List<String> includeDirectories) {
        if (includeDirectories == null) {
            throw new IllegalArgumentException("Include directories must not be null.");
        }
        final ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < includeDirectories.size(); i++) {
            final String directory = includeDirectories.get(i);
            if (directory == null) {
                throw new IllegalArgumentException("Include directory must not be null.");
            }
            if (directory.isBlank()) {
                throw new IllegalArgumentException("Include directory must not be blank.");
            }
            result.add(directory);
        }
        return List.copyOf(result);
    }
}