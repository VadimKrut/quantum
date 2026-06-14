/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.decomposition;

import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;

/**
 * Immutable registry правил разложения gate operations.
 */
public final class GateDecompositionRegistry {

    /**
     * Правила в порядке приоритета.
     */
    private final List<GateDecompositionRule> rules;

    private GateDecompositionRegistry(final List<GateDecompositionRule> rules) {
        this.rules = rules;
    }

    /**
     * Создает пустой registry.
     *
     * @return пустой registry
     */
    public static GateDecompositionRegistry empty() {
        return new GateDecompositionRegistry(List.of());
    }

    /**
     * Создает registry из правил.
     *
     * @param rules правила разложения
     * @return registry
     */
    public static GateDecompositionRegistry of(final List<GateDecompositionRule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Gate decomposition rules must not be null.");
        }
        for (GateDecompositionRule rule : rules) {
            if (rule == null) {
                throw new IllegalArgumentException("Gate decomposition rule must not be null.");
            }
        }
        return new GateDecompositionRegistry(List.copyOf(rules));
    }

    /**
     * Ищет первое правило для gate.
     *
     * @param gate gate для разложения
     * @return правило или null
     */
    public GateDecompositionRule findRule(final Gate gate) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate must not be null.");
        }
        for (int i = 0; i < rules.size(); i++) {
            final GateDecompositionRule rule = rules.get(i);
            if (rule.supports(gate)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Возвращает количество правил.
     *
     * @return количество правил
     */
    public int ruleCount() {
        return rules.size();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateDecompositionRegistry registry)) {
            return false;
        }
        return Objects.equals(
            rules,
            registry.rules
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules);
    }
}