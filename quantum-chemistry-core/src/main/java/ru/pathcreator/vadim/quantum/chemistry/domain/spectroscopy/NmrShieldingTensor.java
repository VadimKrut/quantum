/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

public final class NmrShieldingTensor {

  private final double sigma11Ppm;
  private final double sigma22Ppm;
  private final double sigma33Ppm;

  private NmrShieldingTensor(
      final double sigma11Ppm,
      final double sigma22Ppm,
      final double sigma33Ppm
  ) {
    this.sigma11Ppm = sigma11Ppm;
    this.sigma22Ppm = sigma22Ppm;
    this.sigma33Ppm = sigma33Ppm;
  }

  public static NmrShieldingTensor of(
      final double sigma11Ppm,
      final double sigma22Ppm,
      final double sigma33Ppm
  ) {
    if (!(Double.isFinite(sigma11Ppm)
        && Double.isFinite(sigma22Ppm)
        && Double.isFinite(sigma33Ppm))) {
      throw new IllegalArgumentException("NMR shielding tensor components must be finite.");
    }
    return new NmrShieldingTensor(sigma11Ppm, sigma22Ppm, sigma33Ppm);
  }

  public double sigma11Ppm() {
    return this.sigma11Ppm;
  }

  public double sigma22Ppm() {
    return this.sigma22Ppm;
  }

  public double sigma33Ppm() {
    return this.sigma33Ppm;
  }

  public double isotropicPpm() {
    return (this.sigma11Ppm + this.sigma22Ppm + this.sigma33Ppm) / 3.0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NmrShieldingTensor)) {
      return false;
    }
    final NmrShieldingTensor tensor = (NmrShieldingTensor) other;
    return Double.compare(this.sigma11Ppm, tensor.sigma11Ppm) == 0
        && Double.compare(this.sigma22Ppm, tensor.sigma22Ppm) == 0
        && Double.compare(this.sigma33Ppm, tensor.sigma33Ppm) == 0;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.sigma11Ppm);
    result = ChemistryHash.include(result, this.sigma22Ppm);
    result = ChemistryHash.include(result, this.sigma33Ppm);
    return result;
  }
}