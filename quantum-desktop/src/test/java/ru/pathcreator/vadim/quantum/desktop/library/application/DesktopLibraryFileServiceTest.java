/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.library.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ru.pathcreator.vadim.quantum.desktop.library.domain.DesktopLibraryAlgorithmFile;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmDescriptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopLibraryFileServiceTest {

    private static final String DSL = """
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("main")
            .qreg("q", 2)
            .creg("c", 2)
            .h("q[0]")
            .cx("q[0]", "q[1]")
            .measure("q[0]", "c[0]")
            .measure("q[1]", "c[1]")
            .build();
        """;

    private final DesktopLibraryFileService service = new DesktopLibraryFileService();

    @TempDir
    private Path tempDirectory;

    @Test
    void roundTripsMetadataAndDslBodyWithoutJson() {
        final DesktopLibraryAlgorithmFile parsed = service.parse(service.render(file()));

        assertEquals(
            "user.bell",
            parsed.descriptor().id()
        );
        assertEquals(
            AlgorithmCategory.ENTANGLEMENT,
            parsed.descriptor().category()
        );
        assertEquals(
            AlgorithmDifficulty.INTRODUCTORY,
            parsed.descriptor().difficulty()
        );
        assertEquals(
            List.of("bell", "entanglement"),
            parsed.descriptor().tags()
        );
        assertTrue(parsed.javaDslSource().contains(".cx(\"q[0]\", \"q[1]\")"));
    }

    @Test
    void writesAndListsUserLibraryFiles() throws Exception {
        final Path target = tempDirectory.resolve(service.fileNameForId("User Bell Example"));

        service.write(
            target,
            file()
        );

        final List<Path> files = service.listFiles(tempDirectory);
        assertEquals(
            1,
            files.size()
        );
        assertEquals(
            target.getFileName(),
            files.get(0).getFileName()
        );
        assertTrue(Files.readString(target).contains("---"));
    }

    @Test
    void rejectsFileWithoutDslDelimiter() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.parse("id: broken")
        );
    }

    private static DesktopLibraryAlgorithmFile file() {
        return new DesktopLibraryAlgorithmFile(
            new QuantumAlgorithmDescriptor(
                "user.bell",
                "User Bell",
                "Local Bell state saved as compact Java DSL.",
                AlgorithmCategory.ENTANGLEMENT,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of("bell", "entanglement"),
                List.of("https://example.test/bell"),
                List.of()
            ),
            DSL
        );
    }
}