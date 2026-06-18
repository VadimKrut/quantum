/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.integration;

import ru.pathcreator.vadim.quantum.library.api.QuantumAlgorithmLibrary;
import ru.pathcreator.vadim.quantum.library.catalog.QuantumAlgorithmQuery;
import ru.pathcreator.vadim.quantum.library.catalog.QuantumAlgorithmRegistry;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumAlgorithmLibraryTest {

    @Test
    void builtInRegistryContainsStableUniqueEntries() {
        final QuantumAlgorithmRegistry registry = QuantumAlgorithmLibrary.builtIn();
        final HashSet<String> ids = new HashSet<>();

        assertTrue(registry.size() >= 20);
        for (int i = 0; i < registry.entries().size(); i++) {
            final QuantumAlgorithmEntry entry = registry.entries().get(i);
            assertTrue(ids.add(entry.descriptor().id()));
            assertFalse(entry.descriptor().displayName().isBlank());
            assertFalse(entry.descriptor().summary().isBlank());
            assertFalse(entry.descriptor().tags().isEmpty());
            assertFalse(entry.descriptor().referenceUris().isEmpty());
        }
    }

    @Test
    void builtInAlgorithmsGenerateValidPrograms() {
        final QuantumAlgorithmRegistry registry = QuantumAlgorithmLibrary.builtIn();

        for (int i = 0; i < registry.entries().size(); i++) {
            final QuantumProgram program = registry.entries().get(i).generate();
            final ValidationResult validationResult = Quantum.validate(program);

            assertTrue(
                validationResult.isValid(),
                registry.entries().get(i).descriptor().id()
            );
            assertEquals(
                1,
                program.circuitCount()
            );
            assertTrue(program.circuit(0).operationCount() > 0);
        }
    }

    @Test
    void builtInAlgorithmsExportToOpenQasm3WhereSupported() {
        final QuantumAlgorithmRegistry registry = QuantumAlgorithmLibrary.builtIn();

        for (int i = 0; i < registry.entries().size(); i++) {
            final QuantumProgram program = registry.entries().get(i).generate();
            final ExportResult exportResult = Quantum.exportOpenQasm3(program);

            assertTrue(
                exportResult.isSuccess(),
                registry.entries().get(i).descriptor().id()
            );
            assertTrue(exportResult.content().startsWith("OPENQASM 3.0;"));
        }
    }

    @Test
    void queryFindsAlgorithmsByCategoryTagAndText() {
        final QuantumAlgorithmRegistry registry = QuantumAlgorithmLibrary.builtIn();
        final List<QuantumAlgorithmEntry> chemistry = registry.search(
            QuantumAlgorithmQuery.all().withCategory(AlgorithmCategory.CHEMISTRY)
        );
        final List<QuantumAlgorithmEntry> oracle = registry.search(
            QuantumAlgorithmQuery.all().withTag("oracle")
        );
        final List<QuantumAlgorithmEntry> fourier = registry.search(
            QuantumAlgorithmQuery.text("fourier")
        );

        assertEquals(
            1,
            chemistry.size()
        );
        assertTrue(oracle.size() >= 2);
        assertTrue(fourier.size() >= 2);
    }

    @Test
    void ghzParameterChangesCircuitShape() {
        final QuantumAlgorithmEntry entry = QuantumAlgorithmLibrary.builtIn().get("education.ghz-state");
        final QuantumProgram program = entry.generate(AlgorithmParameterSet.builder()
            .integer("qubits", 5)
            .build());
        final QuantumCircuit circuit = program.circuit(0);

        assertEquals(
            1,
            circuit.quantumRegisterCount()
        );
        assertEquals(
            1 + 4 + 5,
            circuit.operationCount()
        );
    }

    @Test
    void invalidParameterIsRejectedBeforeInvalidProgramIsProduced() {
        final QuantumAlgorithmEntry entry = QuantumAlgorithmLibrary.builtIn().get("oracle.bernstein-vazirani");

        assertThrows(
            IllegalArgumentException.class,
            () -> entry.generate(AlgorithmParameterSet.builder()
                .integer("qubits", 3)
                .longInteger("secretMask", 16L)
                .build())
        );
    }

    @Test
    void unknownParameterIsRejectedInsteadOfBeingIgnored() {
        final QuantumAlgorithmEntry entry = QuantumAlgorithmLibrary.builtIn().get("education.ghz-state");

        assertThrows(
            IllegalArgumentException.class,
            () -> entry.generate(AlgorithmParameterSet.builder()
                .integer("qubits", 4)
                .integer("unused", 1)
                .build())
        );
    }
}