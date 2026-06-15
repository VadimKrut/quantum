/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRegistry;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;

/**
 * Immutable настройки conservative transformation layer.
 */
public final class TransformationOptions {

    private final boolean bindParameters;
    private final boolean requireCompleteParameterBinding;
    private final boolean canonicalizeParameterExpressions;
    private final boolean removeIdentityGates;
    private final boolean inlineCompositeGates;
    private final boolean applyDeclaredDecompositions;
    private final boolean targetAwareLowering;
    private final ParameterBindings parameterBindings;
    private final GateDecompositionRegistry decompositionRegistry;
    private final IntegrationCapabilityProfile targetProfile;

    private TransformationOptions(final Builder builder) {
        this.bindParameters = builder.bindParameters;
        this.requireCompleteParameterBinding = builder.requireCompleteParameterBinding;
        this.canonicalizeParameterExpressions = builder.canonicalizeParameterExpressions;
        this.removeIdentityGates = builder.removeIdentityGates;
        this.inlineCompositeGates = builder.inlineCompositeGates;
        this.applyDeclaredDecompositions = builder.applyDeclaredDecompositions;
        this.targetAwareLowering = builder.targetAwareLowering;
        this.parameterBindings = builder.parameterBindings;
        this.decompositionRegistry = builder.decompositionRegistry;
        this.targetProfile = builder.targetProfile;
    }

    public static TransformationOptions none() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean bindParameters() {
        return bindParameters;
    }

    public boolean requireCompleteParameterBinding() {
        return requireCompleteParameterBinding;
    }

    public boolean canonicalizeParameterExpressions() {
        return canonicalizeParameterExpressions;
    }

    public boolean removeIdentityGates() {
        return removeIdentityGates;
    }

    public boolean inlineCompositeGates() {
        return inlineCompositeGates;
    }

    public boolean applyDeclaredDecompositions() {
        return applyDeclaredDecompositions;
    }

    public boolean targetAwareLowering() {
        return targetAwareLowering;
    }

    public ParameterBindings parameterBindings() {
        return parameterBindings;
    }

    public GateDecompositionRegistry decompositionRegistry() {
        return decompositionRegistry;
    }

    public boolean hasTargetProfile() {
        return targetProfile != null;
    }

    public IntegrationCapabilityProfile targetProfile() {
        if (targetProfile == null) {
            throw new IllegalStateException("Transformation options do not have target profile.");
        }
        return targetProfile;
    }

    /**
     * Builder immutable transformation options.
     */
    public static final class Builder {

        private boolean bindParameters;
        private boolean requireCompleteParameterBinding;
        private boolean canonicalizeParameterExpressions;
        private boolean removeIdentityGates;
        private boolean inlineCompositeGates;
        private boolean applyDeclaredDecompositions;
        private boolean targetAwareLowering;
        private ParameterBindings parameterBindings;
        private GateDecompositionRegistry decompositionRegistry;
        private IntegrationCapabilityProfile targetProfile;

        private Builder() {
            this.parameterBindings = ParameterBindings.empty();
            this.decompositionRegistry = GateDecompositionRegistry.empty();
        }

        public Builder bindParameters(final ParameterBindings bindings) {
            if (bindings == null) {
                throw new IllegalArgumentException("Transformation parameter bindings must not be null.");
            }
            this.bindParameters = true;
            this.parameterBindings = bindings;
            return this;
        }

        public Builder requireCompleteParameterBinding() {
            this.requireCompleteParameterBinding = true;
            return this;
        }

        public Builder canonicalizeParameterExpressions() {
            this.canonicalizeParameterExpressions = true;
            return this;
        }

        public Builder removeIdentityGates() {
            this.removeIdentityGates = true;
            return this;
        }

        public Builder inlineCompositeGates() {
            this.inlineCompositeGates = true;
            return this;
        }

        public Builder applyDeclaredDecompositions(final GateDecompositionRegistry registry) {
            if (registry == null) {
                throw new IllegalArgumentException("Transformation decomposition registry must not be null.");
            }
            this.applyDeclaredDecompositions = true;
            this.decompositionRegistry = registry;
            return this;
        }

        public Builder targetAwareLowering(final IntegrationCapabilityProfile profile) {
            if (profile == null) {
                throw new IllegalArgumentException("Transformation target profile must not be null.");
            }
            this.targetAwareLowering = true;
            this.targetProfile = profile;
            return this;
        }

        public TransformationOptions build() {
            if (
                targetAwareLowering
                && targetProfile == null
            ) {
                throw new IllegalArgumentException("Target-aware lowering requires target profile.");
            }
            return new TransformationOptions(this);
        }
    }
}