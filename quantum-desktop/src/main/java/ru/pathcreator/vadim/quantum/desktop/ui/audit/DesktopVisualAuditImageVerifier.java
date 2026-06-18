/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import javax.imageio.ImageIO;

/**
 * Проверяет, что screenshot visual audit является настоящим непустым PNG-кадром.
 */
public final class DesktopVisualAuditImageVerifier {

    private static final int MINIMUM_WIDTH = 320;
    private static final int MINIMUM_HEIGHT = 240;
    private static final int MINIMUM_BYTES = 4096;
    private static final int MINIMUM_DISTINCT_COLORS = 16;
    private static final int SAMPLE_STEP = 16;

    public void verify(final DesktopScreenshotArtifact artifact) throws IOException {
        if (!Files.isRegularFile(artifact.path())) {
            throw new IllegalStateException("Visual audit screenshot does not exist: " + artifact.path() + ".");
        }
        final long actualBytes = Files.size(artifact.path());
        if (actualBytes != artifact.bytes()) {
            throw new IllegalStateException("Visual audit screenshot byte size metadata is stale: " + artifact.path() + ".");
        }
        if (actualBytes < MINIMUM_BYTES) {
            throw new IllegalStateException("Visual audit screenshot is too small to be credible: " + artifact.path() + ".");
        }
        final BufferedImage image = ImageIO.read(artifact.path().toFile());
        if (image == null) {
            throw new IllegalStateException("Visual audit screenshot is not a readable PNG: " + artifact.path() + ".");
        }
        verifyDimensions(
            artifact,
            image
        );
        verifyHasVisibleContent(
            artifact,
            image
        );
    }

    private static void verifyDimensions(
        final DesktopScreenshotArtifact artifact,
        final BufferedImage image
    ) {
        if (
            image.getWidth() != artifact.width()
            || image.getHeight() != artifact.height()
        ) {
            throw new IllegalStateException("Visual audit screenshot dimension metadata is stale: " + artifact.path() + ".");
        }
        if (
            image.getWidth() < MINIMUM_WIDTH
            || image.getHeight() < MINIMUM_HEIGHT
        ) {
            throw new IllegalStateException("Visual audit screenshot dimensions are too small: " + artifact.path() + ".");
        }
    }

    private static void verifyHasVisibleContent(
        final DesktopScreenshotArtifact artifact,
        final BufferedImage image
    ) {
        final HashSet<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += SAMPLE_STEP) {
            for (int x = 0; x < image.getWidth(); x += SAMPLE_STEP) {
                colors.add(Integer.valueOf(image.getRGB(
                    x,
                    y
                )));
                if (colors.size() >= MINIMUM_DISTINCT_COLORS) {
                    return;
                }
            }
        }
        throw new IllegalStateException("Visual audit screenshot looks blank or monochrome: " + artifact.path() + ".");
    }
}