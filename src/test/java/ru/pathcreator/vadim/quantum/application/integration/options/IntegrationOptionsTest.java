/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.options;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationOptionsTest {

    @Test
    void createsDefaultExportOptions() {
        final ExportOptions options = ExportOptions.defaults();

        assertTrue(options.validateBeforeExport());
        assertFalse(options.failOnWarnings());
        assertEquals(
            UnsupportedGatePolicy.OPAQUE_IF_POSSIBLE,
            options.unsupportedGatePolicy()
        );
        assertEquals(
            ExportTextMode.CANONICAL,
            options.textMode()
        );
        assertEquals(
            0,
            options.gateDecompositionRegistry().ruleCount()
        );
    }

    @Test
    void createsCustomExportOptions() {
        final ExportOptions options = ExportOptions.of(
            false,
            true
        );

        assertFalse(options.validateBeforeExport());
        assertTrue(options.failOnWarnings());
    }

    @Test
    void createsExtendedExportOptions() {
        final GateDecompositionRegistry registry = GateDecompositionRegistry.empty();
        final ExportOptions options = ExportOptions.of(
            false,
            true,
            UnsupportedGatePolicy.REQUIRE_DECOMPOSITION,
            ExportTextMode.LOSSLESS_WHEN_AVAILABLE,
            registry
        );

        assertFalse(options.validateBeforeExport());
        assertTrue(options.failOnWarnings());
        assertEquals(
            UnsupportedGatePolicy.REQUIRE_DECOMPOSITION,
            options.unsupportedGatePolicy()
        );
        assertEquals(
            ExportTextMode.LOSSLESS_WHEN_AVAILABLE,
            options.textMode()
        );
        assertEquals(
            registry,
            options.gateDecompositionRegistry()
        );
    }

    @Test
    void createsDefaultImportOptions() {
        final ImportOptions options = ImportOptions.defaults();

        assertTrue(options.validateAfterImport());
        assertFalse(options.failOnWarnings());
        assertEquals(
            0,
            options.includedSourceCount()
        );
        assertEquals(
            0,
            options.includeDirectoryCount()
        );
    }

    @Test
    void createsCustomImportOptions() {
        final ImportOptions options = ImportOptions.of(
            false,
            true
        );

        assertFalse(options.validateAfterImport());
        assertTrue(options.failOnWarnings());
    }

    @Test
    void createsImportOptionsWithIncludedSources() {
        final ImportOptions options = ImportOptions.of(
            true,
            false,
            Map.of(
                "custom.inc",
                "gate myh a { h a; }"
            ),
            List.of("C:\\includes")
        );

        assertEquals(
            1,
            options.includedSourceCount()
        );
        assertEquals(
            "gate myh a { h a; }",
            options.includedSources().get("custom.inc")
        );
        assertEquals(
            1,
            options.includeDirectoryCount()
        );
    }

    @Test
    void addsIncludedSourceImmutably() {
        final ImportOptions baseOptions = ImportOptions.defaults();
        final ImportOptions options = baseOptions.withIncludedSource(
            "custom.inc",
            "gate myh a { h a; }"
        );

        assertEquals(
            0,
            baseOptions.includedSourceCount()
        );
        assertEquals(
            1,
            options.includedSourceCount()
        );
        final ImportOptions withDirectory = baseOptions.withIncludeDirectory("C:\\includes");
        assertEquals(
            0,
            baseOptions.includeDirectoryCount()
        );
        assertEquals(
            1,
            withDirectory.includeDirectoryCount()
        );
    }

    @Test
    void rejectsInvalidIncludedSources() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportOptions.of(
                true,
                false,
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportOptions.defaults().withIncludedSource(
                "",
                "gate myh a { h a; }"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportOptions.defaults().withIncludedSource(
                "custom.inc",
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportOptions.defaults().withIncludeDirectory("")
        );
    }

    @Test
    void comparesOptionsByFlags() {
        assertEquals(
            ExportOptions.of(
                true,
                false
            ),
            ExportOptions.defaults()
        );
        assertEquals(
            ImportOptions.of(
                true,
                false
            ).hashCode(),
            ImportOptions.defaults().hashCode()
        );
        assertNotEquals(
            ExportOptions.of(
                true,
                false,
                UnsupportedGatePolicy.FAIL_FAST,
                ExportTextMode.CANONICAL,
                GateDecompositionRegistry.empty()
            ),
            ExportOptions.defaults()
        );
        assertNotEquals(
            ImportOptions.of(
                false,
                false
            ),
            ImportOptions.defaults()
        );
    }
}