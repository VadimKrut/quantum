/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule;

import java.util.List;

/** Неизменяемая библиотека правил органических реакций. */
public final class OrganicReactionRuleLibrary {

  public static final OrganicReactionRuleLibrary EMPTY = new OrganicReactionRuleLibrary(List.of());

  private final List<OrganicReactionRule> rules;

  private OrganicReactionRuleLibrary(final List<OrganicReactionRule> rules) {
    this.rules = rules;
  }

  public static OrganicReactionRuleLibrary of(final List<OrganicReactionRule> rules) {
    if (rules == null) {
      throw new IllegalArgumentException("Organic reaction rule list must not be null.");
    }
    for (int i = 0; i < rules.size(); ++i) {
      if (rules.get(i) == null) {
        throw new IllegalArgumentException("Organic reaction rule must not be null.");
      }
      for (int j = i + 1; j < rules.size(); ++j) {
        if (!rules.get(i).id().equals(rules.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException("Organic reaction rule ids must be unique.");
      }
    }
    if (rules.isEmpty()) {
      return EMPTY;
    }
    return new OrganicReactionRuleLibrary(List.copyOf(rules));
  }

  public List<OrganicReactionRule> rules() {
    return this.rules;
  }

  public boolean empty() {
    return this.rules.isEmpty();
  }

  public int size() {
    return this.rules.size();
  }
}
