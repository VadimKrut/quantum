/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity;

/**
 * Стабильный код причины, по которой связка классического и квантового расчета неполная.
 */
public enum ChemistryComputationParityDiagnosticCode {
  MISSING_MECHANISM,
  MISSING_PATHWAY_STEPS,
  REACTION_PREDICTION_LOW_CONFIDENCE,
  REACTION_PREDICTION_INCOMPLETE_PROBABILITY_MASS,
  MISSING_CLASSICAL_PLAN,
  MISSING_CLASSICAL_RESULT,
  CLASSICAL_RESULT_NOT_SUCCEEDED,
  CLASSICAL_COVERAGE_APPROXIMATE,
  CLASSICAL_COVERAGE_UNSUPPORTED,
  MISSING_QUANTUM_PROGRAMS,
  QUANTUM_PROGRAM_SKIPPED,
  QUANTUM_PROGRAM_WITHOUT_MAPPING,
  QUANTUM_PROGRAM_EMPTY,
  QUANTUM_MAPPING_MISMATCH,
  QUANTUM_PACKAGE_METADATA_INCOMPLETE,
  ADAPT_VQE_SELECTION_NOT_EXECUTED
}
