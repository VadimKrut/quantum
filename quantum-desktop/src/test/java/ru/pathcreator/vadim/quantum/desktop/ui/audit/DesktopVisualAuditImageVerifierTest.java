/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopVisualAuditImageVerifierTest {

    @TempDir
    private Path tempDir;

    @Test
    void acceptsReadableScreenshotWithVisibleContent() throws Exception {
        final Path path = tempDir.resolve("visible.png");
        final BufferedImage image = new BufferedImage(
            640,
            480,
            BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D graphics = image.createGraphics();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                graphics.setColor(new Color(
                    (x * 17) & 0xFF,
                    (y * 19) & 0xFF,
                    ((x + y) * 23) & 0xFF
                ));
                graphics.fillRect(
                    x,
                    y,
                    1,
                    1
                );
            }
        }
        graphics.dispose();
        ImageIO.write(
            image,
            "png",
            path.toFile()
        );
        final DesktopScreenshotArtifact artifact = new DesktopScreenshotArtifact(
            path,
            image.getWidth(),
            image.getHeight(),
            Files.size(path)
        );

        assertDoesNotThrow(() -> new DesktopVisualAuditImageVerifier().verify(artifact));
    }

    @Test
    void rejectsMonochromeScreenshot() throws Exception {
        final Path path = tempDir.resolve("blank.png");
        final BufferedImage image = new BufferedImage(
            640,
            480,
            BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(
            0,
            0,
            image.getWidth(),
            image.getHeight()
        );
        graphics.dispose();
        ImageIO.write(
            image,
            "png",
            path.toFile()
        );
        final DesktopScreenshotArtifact artifact = new DesktopScreenshotArtifact(
            path,
            image.getWidth(),
            image.getHeight(),
            Files.size(path)
        );

        assertThrows(
            IllegalStateException.class,
            () -> new DesktopVisualAuditImageVerifier().verify(artifact)
        );
    }
}