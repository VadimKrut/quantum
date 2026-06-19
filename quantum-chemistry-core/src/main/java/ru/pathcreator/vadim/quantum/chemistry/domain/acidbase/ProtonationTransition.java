/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.acidbase;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.PHValue;

public final class ProtonationTransition {

  private final AcidBaseSite site;
  private final String protonatedStateLabel;
  private final String deprotonatedStateLabel;
  private final PKaValue pka;

  private ProtonationTransition(
      final AcidBaseSite site, final String protonatedStateLabel, final String deprotonatedStateLabel, final PKaValue pka) {
    this.site = site;
    this.protonatedStateLabel = protonatedStateLabel;
    this.deprotonatedStateLabel = deprotonatedStateLabel;
    this.pka = pka;
  }

  public static ProtonationTransition of(
      final AcidBaseSite site, final String protonatedStateLabel, final String deprotonatedStateLabel, final PKaValue pka) {
    String checkedDeprotonatedLabel;
    if (site == null) {
      throw new IllegalArgumentException("Protonation transition site must not be null.");
    }
    final String checkedProtonatedLabel =
        TextValue.requireText(protonatedStateLabel, "Protonated state label");
    if (checkedProtonatedLabel.equals(
        checkedDeprotonatedLabel =
            TextValue.requireText(deprotonatedStateLabel, "Deprotonated state label"))) {
      throw new IllegalArgumentException("Protonation transition state labels must be different.");
    }
    if (pka == null) {
      throw new IllegalArgumentException("Protonation transition pKa must not be null.");
    }
    return new ProtonationTransition(site, checkedProtonatedLabel, checkedDeprotonatedLabel, pka);
  }

  public AcidBaseSite site() {
    return this.site;
  }

  public String protonatedStateLabel() {
    return this.protonatedStateLabel;
  }

  public String deprotonatedStateLabel() {
    return this.deprotonatedStateLabel;
  }

  public PKaValue pka() {
    return this.pka;
  }

  public double deprotonatedToProtonatedRatio(final PHValue ph) {
    if (ph == null) {
      throw new IllegalArgumentException("pH value must not be null.");
    }
    return Math.pow(10.0, ph.value() - this.pka.value());
  }

  public double deprotonatedFraction(final PHValue ph) {
    final double ratio = this.deprotonatedToProtonatedRatio(ph);
    return ratio / (1.0 + ratio);
  }

  public double protonatedFraction(final PHValue ph) {
    return 1.0 - this.deprotonatedFraction(ph);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ProtonationTransition)) {
      return false;
    }
    final ProtonationTransition transition = (ProtonationTransition) other;
    return Objects.equals(this.site, transition.site)
        && Objects.equals(this.protonatedStateLabel, transition.protonatedStateLabel)
        && Objects.equals(this.deprotonatedStateLabel, transition.deprotonatedStateLabel)
        && Objects.equals(this.pka, transition.pka);
  }

  public int hashCode() {
    return Objects.hash(
        this.site, this.protonatedStateLabel, this.deprotonatedStateLabel, this.pka);
  }
}