/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.distribution;

/**
 * One integrity issue found while verifying a product distribution bundle.
 */
public final class ProductDistributionVerificationIssue {

    private final String code;
    private final String path;
    private final String message;

    private ProductDistributionVerificationIssue(
        final String code,
        final String path,
        final String message
    ) {
        this.code = code;
        this.path = path;
        this.message = message;
    }

    public static ProductDistributionVerificationIssue of(
        final String code,
        final String path,
        final String message
    ) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product distribution verification issue code must not be blank.");
        }
        if (path == null) {
            throw new IllegalArgumentException("Product distribution verification issue path must not be null.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Product distribution verification issue message must not be blank.");
        }
        return new ProductDistributionVerificationIssue(
            code,
            path,
            message
        );
    }

    public String code() {
        return code;
    }

    public String path() {
        return path;
    }

    public String message() {
        return message;
    }
}