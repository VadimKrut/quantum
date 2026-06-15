/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.doctor;

/**
 * Один пункт отчета product doctor.
 */
public final class ProductDoctorCheck {

    private final String name;
    private final ProductDoctorCheckStatus status;
    private final String message;

    private ProductDoctorCheck(
        final String name,
        final ProductDoctorCheckStatus status,
        final String message
    ) {
        this.name = name;
        this.status = status;
        this.message = message;
    }

    public static ProductDoctorCheck of(
        final String name,
        final ProductDoctorCheckStatus status,
        final String message
    ) {
        if (
            name == null
            || name.isBlank()
        ) {
            throw new IllegalArgumentException("Product doctor check name must not be blank.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Product doctor check status must not be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Product doctor check message must not be null.");
        }
        return new ProductDoctorCheck(
            name,
            status,
            message
        );
    }

    public String name() {
        return name;
    }

    public ProductDoctorCheckStatus status() {
        return status;
    }

    public boolean isFail() {
        return status == ProductDoctorCheckStatus.FAIL;
    }

    public boolean isWarn() {
        return status == ProductDoctorCheckStatus.WARN;
    }

    public String message() {
        return message;
    }
}