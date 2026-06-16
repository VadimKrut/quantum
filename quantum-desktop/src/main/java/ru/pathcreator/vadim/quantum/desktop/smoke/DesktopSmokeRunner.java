/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowResult;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowService;

/**
 * Выполняет headless smoke-проверку desktop workflow без запуска JavaFX окна.
 */
public final class DesktopSmokeRunner {

    private static final String BELL_OPENQASM_2 = """
        OPENQASM 2.0;
        include "qelib1.inc";
        qreg q[2];
        creg c[2];
        h q[0];
        cx q[0],q[1];
        measure q[0] -> c[0];
        measure q[1] -> c[1];
        """;

    private final DesktopWorkflowService service;

    public DesktopSmokeRunner() {
        this(new DesktopWorkflowService());
    }

    public DesktopSmokeRunner(final DesktopWorkflowService service) {
        if (service == null) {
            throw new IllegalArgumentException("Desktop smoke workflow service must not be null.");
        }
        this.service = service;
    }

    public DesktopSmokeReport run(
        final Path projectRoot,
        final Path corpusRoot
    ) {
        final ArrayList<DesktopSmokeStep> steps = new ArrayList<>();
        final Path outputRoot = projectRoot.resolve("target/desktop-smoke-output");
        final Path effectiveCorpusRoot;
        try {
            effectiveCorpusRoot = effectiveCorpusRoot(
                corpusRoot,
                outputRoot.resolve("smoke-corpus")
            );
        } catch (final IOException exception) {
            steps.add(new DesktopSmokeStep(
                "prepare-smoke-corpus",
                false,
                "EXCEPTION",
                exception.getMessage() == null
                    ? exception.toString()
                    : exception.getMessage()
            ));
            return new DesktopSmokeReport(steps);
        }
        steps.add(step(
            "import-openqasm2",
            service.importProgram(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2
            )
        ));
        steps.add(step(
            "validate-openqasm2",
            service.validate(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2
            )
        ));
        steps.add(step(
            "inspect-openqasm2-to-openqasm3",
            service.inspect(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                IntegrationFormat.OPENQASM_3
            )
        ));
        steps.add(step(
            "resources-openqasm2",
            service.resources(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                20
            )
        ));
        steps.add(step(
            "circuit-openqasm2",
            service.circuit(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2
            )
        ));
        steps.add(step(
            "preflight-openqasm3",
            service.preflight(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                IntegrationFormat.OPENQASM_3
            )
        ));
        steps.add(step(
            "simulate-openqasm2",
            service.simulate(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                128,
                7L
            )
        ));
        steps.add(step(
            "compile-openqasm3",
            service.compile(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                IntegrationFormat.OPENQASM_3
            )
        ));
        steps.add(step(
            "workflow-openqasm3",
            service.workflow(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                IntegrationFormat.OPENQASM_3,
                64,
                5L
            )
        ));
        steps.add(step(
            "benchmark-openqasm3",
            service.benchmark(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                IntegrationFormat.OPENQASM_3,
                64,
                5L
            )
        ));
        steps.add(step(
            "compatibility-openqasm2",
            service.compatibility(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                64,
                5L
            )
        ));
        steps.add(step(
            "cross-format-openqasm2",
            service.verifyCrossFormat(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                64,
                5L
            )
        ));
        steps.add(step(
            "backend-dry-run-openqasm3",
            service.backendDryRun(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2,
                IntegrationFormat.OPENQASM_3,
                64,
                5L
            )
        ));
        steps.add(step(
            "json-openqasm2",
            service.json(
                IntegrationFormat.OPENQASM_2,
                BELL_OPENQASM_2
            )
        ));
        try {
            steps.add(step(
                "regression-corpus",
                service.corpusRegression(
                    effectiveCorpusRoot,
                    64,
                    5L
                )
            ));
            steps.add(step(
                "release-readiness",
                service.releaseReadiness(
                    effectiveCorpusRoot,
                    IntegrationFormat.OPENQASM_3,
                    64,
                    5L
                )
            ));
            steps.add(step(
                "doctor",
                service.doctor(projectRoot)
            ));
            steps.add(step(
                "product-audit",
                service.productAudit(
                    projectRoot,
                    effectiveCorpusRoot,
                    IntegrationFormat.OPENQASM_3,
                    64,
                    5L
                )
            ));
            steps.add(step(
                "product-report",
                service.productReport(
                    projectRoot,
                    effectiveCorpusRoot,
                    outputRoot,
                    IntegrationFormat.OPENQASM_3,
                    64,
                    5L
                )
            ));
            if (hasPackagedDistributionInputs(projectRoot)) {
                steps.add(step(
                    "product-distribution",
                    service.productDistribution(
                        projectRoot,
                        outputRoot
                    )
                ));
            } else {
                steps.add(new DesktopSmokeStep(
                    "product-distribution",
                    true,
                    "SKIPPED",
                    "Packaged CLI/desktop jars are not both present; run full mvn package for distribution bundle smoke."
                ));
            }
        } catch (final Exception exception) {
            steps.add(new DesktopSmokeStep(
                "desktop-product-suite",
                false,
                "EXCEPTION",
                exception.getMessage() == null
                    ? exception.toString()
                    : exception.getMessage()
            ));
        }
        return new DesktopSmokeReport(steps);
    }

    private static boolean hasPackagedDistributionInputs(final Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("quantum-cli").resolve("target").resolve("quantum-cli-0.1.0.jar"))
            && Files.isRegularFile(projectRoot.resolve("quantum-desktop").resolve("target").resolve("quantum-desktop-0.1.0.jar"));
    }

    private static Path effectiveCorpusRoot(
        final Path requestedCorpusRoot,
        final Path generatedCorpusRoot
    ) throws IOException {
        if (
            requestedCorpusRoot != null
            && Files.isDirectory(requestedCorpusRoot)
        ) {
            return requestedCorpusRoot;
        }
        writeGeneratedCorpus(generatedCorpusRoot);
        return generatedCorpusRoot;
    }

    private static void writeGeneratedCorpus(final Path corpusRoot) throws IOException {
        Files.createDirectories(corpusRoot.resolve("openqasm2"));
        Files.createDirectories(corpusRoot.resolve("openqasm3"));
        Files.createDirectories(corpusRoot.resolve("quil"));
        Files.writeString(
            corpusRoot.resolve("README.md"),
            "Generated desktop smoke corpus."
        );
        Files.writeString(
            corpusRoot.resolve("openqasm2").resolve("bell.qasm"),
            BELL_OPENQASM_2
        );
        Files.writeString(
            corpusRoot.resolve("openqasm3").resolve("ghz.qasm"),
            "OPENQASM 3.0;" + System.lineSeparator()
                + "include \"stdgates.inc\";" + System.lineSeparator()
                + "qubit[3] q;" + System.lineSeparator()
                + "bit[3] c;" + System.lineSeparator()
                + "h q[0];" + System.lineSeparator()
                + "cx q[0], q[1];" + System.lineSeparator()
                + "cx q[1], q[2];" + System.lineSeparator()
                + "c[0] = measure q[0];" + System.lineSeparator()
                + "c[1] = measure q[1];" + System.lineSeparator()
                + "c[2] = measure q[2];" + System.lineSeparator()
        );
        Files.writeString(
            corpusRoot.resolve("quil").resolve("bell.quil"),
            "DECLARE ro BIT[2]" + System.lineSeparator()
                + "H 0" + System.lineSeparator()
                + "CNOT 0 1" + System.lineSeparator()
                + "MEASURE 0 ro[0]" + System.lineSeparator()
                + "MEASURE 1 ro[1]" + System.lineSeparator()
        );
    }

    private static DesktopSmokeStep step(
        final String name,
        final DesktopWorkflowResult result
    ) {
        return new DesktopSmokeStep(
            name,
            result.isSuccess(),
            result.status(),
            result.summary()
        );
    }
}