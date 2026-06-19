/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.valence;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;

public final class ValenceProfile {

  public static final ValenceProfile COMMON =
      ValenceProfile.of(
          List.of(
              ValenceRule.of(ElementSymbol.of("H"), 1.0),
              ValenceRule.of(ElementSymbol.of("B"), 3.0),
              ValenceRule.of(ElementSymbol.of("C"), 4.0),
              ValenceRule.of(ElementSymbol.of("N"), 4.0),
              ValenceRule.of(ElementSymbol.of("O"), 3.0),
              ValenceRule.of(ElementSymbol.of("F"), 1.0),
              ValenceRule.of(ElementSymbol.of("P"), 5.0),
              ValenceRule.of(ElementSymbol.of("S"), 6.0),
              ValenceRule.of(ElementSymbol.of("Cl"), 7.0),
              ValenceRule.of(ElementSymbol.of("Br"), 7.0),
              ValenceRule.of(ElementSymbol.of("I"), 7.0),
              ValenceRule.of(ElementSymbol.of("Si"), 4.0),
              ValenceRule.of(ElementSymbol.of("Li"), 1.0),
              ValenceRule.of(ElementSymbol.of("Na"), 1.0),
              ValenceRule.of(ElementSymbol.of("K"), 1.0),
              ValenceRule.of(ElementSymbol.of("Mg"), 2.0),
              ValenceRule.of(ElementSymbol.of("Ca"), 2.0),
              ValenceRule.of(ElementSymbol.of("Al"), 3.0)));
  private final List<ValenceRule> rules;

  private ValenceProfile(final List<ValenceRule> rules) {
    this.rules = rules;
  }

  public static ValenceProfile of(final List<ValenceRule> rules) {
    if (rules == null) {
      throw new IllegalArgumentException("Valence profile rules must not be null.");
    }
    for (int i = 0; i < rules.size(); ++i) {
      ValenceRule rule = rules.get(i);
      if (rule == null) {
        throw new IllegalArgumentException("Valence profile rule must not be null.");
      }
      for (int j = i + 1; j < rules.size(); ++j) {
        if (!rule.symbol().equals(rules.get(j).symbol())) continue;
        throw new IllegalArgumentException("Valence profile contains duplicate element rule.");
      }
    }
    return new ValenceProfile(List.copyOf(rules));
  }

  public List<ValenceRule> rules() {
    return this.rules;
  }

  public boolean hasRuleFor(final ElementSymbol symbol) {
    return this.ruleFor(symbol) != null;
  }

  public ValenceRule ruleFor(final ElementSymbol symbol) {
    if (symbol == null) {
      throw new IllegalArgumentException("Valence profile element symbol must not be null.");
    }
    for (int i = 0; i < this.rules.size(); ++i) {
      final ValenceRule rule = this.rules.get(i);
      if (!rule.symbol().equals(symbol)) continue;
      return rule;
    }
    return null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ValenceProfile)) {
      return false;
    }
    final ValenceProfile profile = (ValenceProfile) other;
    return Objects.equals(this.rules, profile.rules);
  }

  public int hashCode() {
    return this.rules.hashCode();
  }
}