/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.library.application;

import ru.pathcreator.vadim.quantum.desktop.library.domain.DesktopLibraryAlgorithmFile;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmDescriptor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Сохраняет и читает пользовательские алгоритмы desktop-библиотеки в компактном DSL-формате.
 */
public final class DesktopLibraryFileService {

    private static final String HEADER = "# Quantum IR Studio Library Entry";
    private static final String DELIMITER = "---";

    /**
     * Возвращает стандартную локальную папку пользовательской библиотеки.
     *
     * @return путь к пользовательской библиотеке
     */
    public Path defaultLibraryDirectory() {
        return Path.of(
            System.getProperty("user.home"),
            ".quantum-ir-studio",
            "library"
        );
    }

    /**
     * Сохраняет запись библиотеки.
     *
     * @param path целевой файл
     * @param file запись библиотеки
     * @return путь к сохраненному файлу
     * @throws IOException если запись невозможна
     */
    public Path write(
        final Path path,
        final DesktopLibraryAlgorithmFile file
    ) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Library file path must not be null.");
        }
        if (file == null) {
            throw new IllegalArgumentException("Library file must not be null.");
        }
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
            path,
            render(file)
        );
        return path;
    }

    /**
     * Читает запись библиотеки.
     *
     * @param path исходный файл
     * @return запись библиотеки
     * @throws IOException если чтение невозможно
     */
    public DesktopLibraryAlgorithmFile read(final Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Library file path must not be null.");
        }
        return parse(Files.readString(path));
    }

    /**
     * Возвращает пользовательские .qdsl файлы.
     *
     * @param directory папка библиотеки
     * @return отсортированный список файлов
     * @throws IOException если чтение папки невозможно
     */
    public List<Path> listFiles(final Path directory) throws IOException {
        final ArrayList<Path> files = new ArrayList<>();
        if (
            directory == null
            || !Files.isDirectory(directory)
        ) {
            return List.of();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.qdsl")) {
            for (final Path path : stream) {
                files.add(path);
            }
        }
        files.sort(Path::compareTo);
        return List.copyOf(files);
    }

    /**
     * Формирует безопасное имя файла по id алгоритма.
     *
     * @param id идентификатор алгоритма
     * @return имя .qdsl файла
     */
    public String fileNameForId(final String id) {
        if (
            id == null
            || id.isBlank()
        ) {
            throw new IllegalArgumentException("Algorithm id must not be blank.");
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            final char ch = id.charAt(i);
            if (
                Character.isLetterOrDigit(ch)
                || ch == '-'
                || ch == '_'
            ) {
                builder.append(Character.toLowerCase(ch));
            } else {
                builder.append('-');
            }
        }
        return builder + ".qdsl";
    }

    /**
     * Сериализует запись библиотеки в человекочитаемый DSL-файл.
     *
     * @param file запись библиотеки
     * @return содержимое файла
     */
    public String render(final DesktopLibraryAlgorithmFile file) {
        if (file == null) {
            throw new IllegalArgumentException("Library file must not be null.");
        }
        final QuantumAlgorithmDescriptor descriptor = file.descriptor();
        final StringBuilder builder = new StringBuilder();
        builder.append(HEADER).append(System.lineSeparator());
        appendHeader(
            builder,
            "id",
            descriptor.id()
        );
        appendHeader(
            builder,
            "name",
            descriptor.displayName()
        );
        appendHeader(
            builder,
            "summary",
            descriptor.summary()
        );
        appendHeader(
            builder,
            "category",
            descriptor.category().name()
        );
        appendHeader(
            builder,
            "difficulty",
            descriptor.difficulty().name()
        );
        appendHeader(
            builder,
            "tags",
            String.join(
                ", ",
                descriptor.tags()
            )
        );
        appendHeader(
            builder,
            "references",
            String.join(
                ", ",
                descriptor.referenceUris()
            )
        );
        builder.append(DELIMITER).append(System.lineSeparator());
        builder.append(file.javaDslSource().strip()).append(System.lineSeparator());
        return builder.toString();
    }

    /**
     * Разбирает содержимое .qdsl файла.
     *
     * @param content содержимое файла
     * @return запись библиотеки
     */
    public DesktopLibraryAlgorithmFile parse(final String content) {
        if (
            content == null
            || content.isBlank()
        ) {
            throw new IllegalArgumentException("Library file content must not be blank.");
        }
        final String[] parts = content.split("\\R---\\R", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Library file must contain metadata delimiter: ---.");
        }
        final Header header = parseHeader(parts[0]);
        final QuantumAlgorithmDescriptor descriptor = new QuantumAlgorithmDescriptor(
            header.required("id"),
            header.required("name"),
            header.required("summary"),
            AlgorithmCategory.valueOf(header.required("category")),
            AlgorithmDifficulty.valueOf(header.required("difficulty")),
            splitList(header.optional("tags")),
            splitList(header.optional("references")),
            List.of()
        );
        return new DesktopLibraryAlgorithmFile(
            descriptor,
            parts[1].strip()
        );
    }

    private static Header parseHeader(final String source) {
        final Header header = new Header();
        final String[] lines = source.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (
                line.isBlank()
                || line.equals(HEADER)
            ) {
                continue;
            }
            final int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid library metadata line: " + line + ".");
            }
            header.put(
                line.substring(0, separator).trim(),
                line.substring(separator + 1).trim()
            );
        }
        return header;
    }

    private static List<String> splitList(final String value) {
        if (
            value == null
            || value.isBlank()
        ) {
            return List.of();
        }
        final ArrayList<String> result = new ArrayList<>();
        final String[] items = value.split(",");
        for (int i = 0; i < items.length; i++) {
            final String item = items[i].trim();
            if (!item.isBlank()) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    private static void appendHeader(
        final StringBuilder builder,
        final String name,
        final String value
    ) {
        builder.append(name).append(": ").append(value == null ? "" : value).append(System.lineSeparator());
    }

    /**
     * Маленькая map без stream API для строгого code style проекта.
     */
    private static final class Header {

        private final ArrayList<String> names = new ArrayList<>();
        private final ArrayList<String> values = new ArrayList<>();

        private void put(
            final String name,
            final String value
        ) {
            names.add(name);
            values.add(value);
        }

        private String required(final String name) {
            final String value = optional(name);
            if (
                value == null
                || value.isBlank()
            ) {
                throw new IllegalArgumentException("Required library metadata is missing: " + name + ".");
            }
            return value;
        }

        private String optional(final String name) {
            for (int i = 0; i < names.size(); i++) {
                if (names.get(i).equals(name)) {
                    return values.get(i);
                }
            }
            return "";
        }
    }
}