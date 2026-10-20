/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;

/** Результат автоподбора active space вместе с пересобранной immutable-заявкой расчета. */
public final class ResolvedActiveSpaceRequest {

  private final ClassicalCalculationRequest request;
  private final ActiveSpaceSelectionResult selection;
  private final boolean autoSelected;

  private ResolvedActiveSpaceRequest(
      final ClassicalCalculationRequest request,
      final ActiveSpaceSelectionResult selection,
      final boolean autoSelected) {
    this.request = request;
    this.selection = selection;
    this.autoSelected = autoSelected;
  }

  public static ResolvedActiveSpaceRequest of(
      final ClassicalCalculationRequest request,
      final ActiveSpaceSelectionResult selection,
      final boolean autoSelected) {
    if (request == null) {
      throw new IllegalArgumentException("Resolved active-space request must not be null.");
    }
    if (selection == null) {
      throw new IllegalArgumentException("Resolved active-space selection must not be null.");
    }
    return new ResolvedActiveSpaceRequest(request, selection, autoSelected);
  }

  public ClassicalCalculationRequest request() {
    return this.request;
  }

  public ActiveSpaceSelectionResult selection() {
    return this.selection;
  }

  public boolean autoSelected() {
    return this.autoSelected;
  }
}
