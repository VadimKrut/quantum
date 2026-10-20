/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

/** Семейство density functional approximation для DFT-методов. */
public enum DensityFunctionalFamily {
  LOCAL_DENSITY,
  GENERALIZED_GRADIENT,
  META_GGA,
  HYBRID_GGA,
  HYBRID_META_GGA,
  RANGE_SEPARATED_HYBRID,
  DOUBLE_HYBRID,
  CUSTOM;
}
