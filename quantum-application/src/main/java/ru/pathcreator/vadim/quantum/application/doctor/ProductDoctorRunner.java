/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.doctor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import ru.pathcreator.vadim.quantum.application.product.ProductArtifactLocator;

/**
 * Проверяет локальную структуру продукта перед smoke/release запуском.
 */
public final class ProductDoctorRunner {

    private static final String[] REQUIRED_MODULES = new String[] {
        "quantum-core",
        "quantum-simulation",
        "quantum-application",
        "quantum-json",
        "quantum-openqasm2",
        "quantum-openqasm3",
        "quantum-quil",
        "quantum-api",
        "quantum-cli",
        "quantum-desktop"
    };

    private static final String[] PACKAGED_JAR_MODULES = new String[] {
        "quantum-cli",
        "quantum-desktop"
    };

    public ProductDoctorReport run(final Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("Product doctor project root must not be null.");
        }
        final Path root = projectRoot.toAbsolutePath().normalize();
        final ArrayList<ProductDoctorCheck> checks = new ArrayList<>();
        checks.add(checkFile(
            root,
            "pom",
            "pom.xml",
            true
        ));
        checks.add(checkFile(
            root,
            "license",
            "LICENSE",
            true
        ));
        checks.add(checkPomModules(root));
        checks.add(checkModules(root));
        checks.add(checkGitIgnore(root));
        checks.add(checkLocalToolsPolicy(root));
        checks.add(checkPackagedJars(root));
        return ProductDoctorReport.of(
            root,
            checks
        );
    }

    private static ProductDoctorCheck checkModules(final Path root) {
        final ArrayList<String> missing = new ArrayList<>();
        for (int i = 0; i < REQUIRED_MODULES.length; i++) {
            if (!Files.isDirectory(root.resolve(REQUIRED_MODULES[i]))) {
                missing.add(REQUIRED_MODULES[i]);
            }
        }
        if (!missing.isEmpty()) {
            return ProductDoctorCheck.of(
                "modules",
                ProductDoctorCheckStatus.FAIL,
                "Missing module(s): " + String.join(
                    ", ",
                    missing
                ) + "."
            );
        }
        return ProductDoctorCheck.of(
            "modules",
            ProductDoctorCheckStatus.PASS,
            "All required Maven modules are present."
        );
    }

    private static ProductDoctorCheck checkPomModules(final Path root) {
        final Path pom = root.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            return ProductDoctorCheck.of(
                "pom-modules",
                ProductDoctorCheckStatus.FAIL,
                "pom.xml is missing, so reactor modules cannot be verified."
            );
        }
        try {
            final String content = Files.readString(pom);
            final ArrayList<String> missing = new ArrayList<>();
            for (int i = 0; i < REQUIRED_MODULES.length; i++) {
                final String module = "<module>" + REQUIRED_MODULES[i] + "</module>";
                if (!content.contains(module)) {
                    missing.add(REQUIRED_MODULES[i]);
                }
            }
            if (!missing.isEmpty()) {
                return ProductDoctorCheck.of(
                    "pom-modules",
                    ProductDoctorCheckStatus.FAIL,
                    "Root pom.xml does not declare module(s): " + String.join(
                        ", ",
                        missing
                    ) + "."
                );
            }
            return ProductDoctorCheck.of(
                "pom-modules",
                ProductDoctorCheckStatus.PASS,
                "Root pom.xml declares all required Maven modules."
            );
        } catch (final IOException exception) {
            return ProductDoctorCheck.of(
                "pom-modules",
                ProductDoctorCheckStatus.FAIL,
                "Cannot read pom.xml for reactor modules: " + exception.getMessage()
            );
        }
    }

    private static ProductDoctorCheck checkRequiredFiles(
        final Path root,
        final String name,
        final String[] paths
    ) {
        final ArrayList<String> missing = new ArrayList<>();
        for (int i = 0; i < paths.length; i++) {
            if (!Files.isRegularFile(root.resolve(paths[i]))) {
                missing.add(paths[i]);
            }
        }
        if (!missing.isEmpty()) {
            return ProductDoctorCheck.of(
                name,
                ProductDoctorCheckStatus.FAIL,
                "Missing required file(s): " + String.join(
                    ", ",
                    missing
                ) + "."
            );
        }
        return ProductDoctorCheck.of(
            name,
            ProductDoctorCheckStatus.PASS,
            "All required " + name + " are present."
        );
    }

    private static ProductDoctorCheck checkGitIgnore(final Path root) {
        final Path gitIgnore = root.resolve(".gitignore");
        if (!Files.isRegularFile(gitIgnore)) {
            return ProductDoctorCheck.of(
                "gitignore",
                ProductDoctorCheckStatus.FAIL,
                ".gitignore is missing."
            );
        }
        try {
            final String content = Files.readString(gitIgnore);
            final boolean ignoresDocs = content.contains("docs/");
            final boolean ignoresTools = content.contains("/tools/");
            final boolean ignoresTarget = content.contains("/target/");
            final boolean ignoresIdea = content.contains(".idea/");
            if (!ignoresDocs || !ignoresTools || !ignoresTarget || !ignoresIdea) {
                return ProductDoctorCheck.of(
                    "gitignore",
                    ProductDoctorCheckStatus.FAIL,
                    ".gitignore must ignore docs/, /tools/, /target/, and .idea/."
                );
            }
            return ProductDoctorCheck.of(
                "gitignore",
                ProductDoctorCheckStatus.PASS,
                ".gitignore keeps local docs, local tools and build outputs out of git."
            );
        } catch (final IOException exception) {
            return ProductDoctorCheck.of(
                "gitignore",
                ProductDoctorCheckStatus.FAIL,
                "Cannot read .gitignore: " + exception.getMessage()
            );
        }
    }

    private static ProductDoctorCheck checkLocalToolsPolicy(final Path root) {
        if (Files.exists(root.resolve("tools"))) {
            return ProductDoctorCheck.of(
                "local-tools-policy",
                ProductDoctorCheckStatus.WARN,
                "tools/ exists locally but is ignored; distribution scripts are generated into release bundles."
            );
        }
        return ProductDoctorCheck.of(
            "local-tools-policy",
            ProductDoctorCheckStatus.PASS,
            "tools/ is not required in the source checkout; distribution scripts are generated."
        );
    }

    private static ProductDoctorCheck checkPackagedJars(final Path root) {
        final ArrayList<String> missing = new ArrayList<>();
        for (int i = 0; i < PACKAGED_JAR_MODULES.length; i++) {
            try {
                if (!ProductArtifactLocator.hasPackagedJar(
                    root,
                    PACKAGED_JAR_MODULES[i]
                )) {
                    missing.add(PACKAGED_JAR_MODULES[i] + "/target/" + PACKAGED_JAR_MODULES[i] + "-*.jar");
                }
            } catch (final IOException exception) {
                missing.add(PACKAGED_JAR_MODULES[i] + "/target/" + PACKAGED_JAR_MODULES[i] + "-*.jar");
            }
        }
        if (!missing.isEmpty()) {
            return ProductDoctorCheck.of(
                "packaged-jars",
                ProductDoctorCheckStatus.WARN,
                "Packaged jar(s) are not present yet: " + String.join(
                    ", ",
                    missing
                ) + ". Run mvn package before distribution smoke."
            );
        }
        return ProductDoctorCheck.of(
            "packaged-jars",
            ProductDoctorCheckStatus.PASS,
            "CLI and desktop UI packaged jars are present."
        );
    }

    private static ProductDoctorCheck checkFile(
        final Path root,
        final String name,
        final String path,
        final boolean required
    ) {
        if (Files.isRegularFile(root.resolve(path))) {
            return ProductDoctorCheck.of(
                name,
                ProductDoctorCheckStatus.PASS,
                path + " is present."
            );
        }
        return ProductDoctorCheck.of(
            name,
            required
                ? ProductDoctorCheckStatus.FAIL
                : ProductDoctorCheckStatus.WARN,
            path + " is missing."
        );
    }
}