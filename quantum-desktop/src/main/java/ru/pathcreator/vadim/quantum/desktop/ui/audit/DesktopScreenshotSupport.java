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
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

/**
 * Сохраняет контрольные снимки desktop-окна для ручного визуального аудита.
 */
public final class DesktopScreenshotSupport {

    private DesktopScreenshotSupport() {
    }

    public static DesktopScreenshotArtifact saveStageScreenshot(
        final Stage stage,
        final String outputPath
    ) throws java.io.IOException {
        final WritableImage image = stage.getScene().snapshot(null);
        final PixelReader reader = image.getPixelReader();
        final int width = (int) image.getWidth();
        final int height = (int) image.getHeight();
        final BufferedImage bufferedImage = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB
        );
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(
                    x,
                    y,
                    reader.getArgb(
                        x,
                        y
                    )
                );
            }
        }
        final Path output = Path.of(outputPath);
        Files.createDirectories(output.getParent());
        ImageIO.write(
            bufferedImage,
            "png",
            output.toFile()
        );
        return new DesktopScreenshotArtifact(
            output,
            width,
            height,
            Files.size(output)
        );
    }
}