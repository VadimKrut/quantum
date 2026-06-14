/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.gate.DistinctQubitsGateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;

/**
 * Гейт с упорядоченным набором модификаторов поверх базового гейта.
 */
public final class ModifiedGate implements Gate {

    /**
     * Базовый гейт без модификаторов.
     */
    private final Gate baseGate;

    /**
     * Упорядоченный immutable список модификаторов.
     */
    private final List<GateModifier> modifiers;

    /**
     * Итоговая арность после применения модификаторов.
     */
    private final int arity;

    /**
     * Правила валидации итогового модифицированного гейта.
     */
    private final List<GateValidationRule> validationRules;

    private ModifiedGate(
        final Gate baseGate,
        final List<GateModifier> modifiers
    ) {
        this.baseGate = baseGate;
        this.modifiers = modifiers;
        this.arity = calculateArity(
            baseGate,
            modifiers
        );
        this.validationRules = createValidationRules(
            baseGate,
            arity
        );
    }

    /**
     * Создает модифицированный гейт.
     *
     * @param baseGate базовый гейт
     * @param modifiers модификаторы в порядке применения
     * @return модифицированный гейт
     */
    public static ModifiedGate of(
        final Gate baseGate,
        final List<GateModifier> modifiers
    ) {
        if (baseGate == null) {
            throw new IllegalArgumentException("Modified gate base gate must not be null.");
        }
        if (modifiers == null) {
            throw new IllegalArgumentException("Modified gate modifiers must not be null.");
        }
        for (int i = 0; i < modifiers.size(); i++) {
            if (modifiers.get(i) == null) {
                throw new IllegalArgumentException("Modified gate modifier must not be null.");
            }
        }
        if (modifiers.isEmpty()) {
            throw new IllegalArgumentException("Modified gate must have at least one modifier.");
        }
        return new ModifiedGate(
            baseGate,
            List.copyOf(modifiers)
        );
    }

    /**
     * Возвращает базовый гейт без модификаторов.
     *
     * @return базовый гейт
     */
    public Gate baseGate() {
        return baseGate;
    }

    /**
     * Возвращает immutable список модификаторов.
     *
     * @return список модификаторов
     */
    public List<GateModifier> modifiers() {
        return modifiers;
    }

    @Override
    public String gateName() {
        final StringBuilder builder = new StringBuilder(baseGate.gateName());
        for (int i = 0; i < modifiers.size(); i++) {
            builder.append("|")
                .append(modifiers.get(i).kind());
        }
        return builder.toString();
    }

    @Override
    public int arity() {
        return arity;
    }

    @Override
    public int parameterCount() {
        return baseGate.parameterCount();
    }

    @Override
    public List<GateValidationRule> validationRules() {
        return validationRules;
    }

    private static List<GateValidationRule> createValidationRules(
        final Gate baseGate,
        final int arity
    ) {
        final ArrayList<GateValidationRule> rules = new ArrayList<>(baseGate.validationRules());
        if (
            arity > 1
            && !rules.contains(DistinctQubitsGateValidationRule.INSTANCE)
        ) {
            rules.add(DistinctQubitsGateValidationRule.INSTANCE);
        }
        return List.copyOf(rules);
    }

    private static int calculateArity(
        final Gate baseGate,
        final List<GateModifier> modifiers
    ) {
        int value = baseGate.arity();
        for (int i = 0; i < modifiers.size(); i++) {
            final GateModifier modifier = modifiers.get(i);
            if (modifier.kind() == GateModifierKind.CONTROLLED) {
                try {
                    value = Math.addExact(
                        value,
                        modifier.integerValue()
                    );
                } catch (final ArithmeticException exception) {
                    throw new IllegalArgumentException("Modified gate arity is outside Java int range.", exception);
                }
            }
        }
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ModifiedGate gate)) {
            return false;
        }
        return Objects.equals(
            baseGate,
            gate.baseGate
        )
            && Objects.equals(
                modifiers,
                gate.modifiers
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            baseGate,
            modifiers
        );
    }
}