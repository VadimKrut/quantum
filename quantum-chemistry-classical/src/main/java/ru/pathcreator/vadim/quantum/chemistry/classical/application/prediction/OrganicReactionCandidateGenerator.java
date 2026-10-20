/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.GeneratedReactionCandidateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;

/** Генерирует product candidates из reaction request без выполнения ranking. */
public interface OrganicReactionCandidateGenerator {

  GeneratedReactionCandidateSet generate(ReactionPredictionRequest request);
}
