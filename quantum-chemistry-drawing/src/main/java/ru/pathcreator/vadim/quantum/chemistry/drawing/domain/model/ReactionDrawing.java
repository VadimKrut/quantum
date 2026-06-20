/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;

/**
 * Визуальное представление реакции: участники, стрелка, условия и ручные поля.
 */
public final class ReactionDrawing {

  private final Reaction reaction;
  private final ReactionArrowKind arrowKind;
  private final DrawingPoint2D arrowStart;
  private final DrawingPoint2D arrowEnd;
  private final List<ManualDrawingField> manualFields;

  private ReactionDrawing(
      final Reaction reaction,
      final ReactionArrowKind arrowKind,
      final DrawingPoint2D arrowStart,
      final DrawingPoint2D arrowEnd,
      final List<ManualDrawingField> manualFields
  ) {
    this.reaction = reaction;
    this.arrowKind = arrowKind;
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
    this.manualFields = manualFields;
  }

  public static ReactionDrawing of(
      final Reaction reaction,
      final ReactionArrowKind arrowKind,
      final DrawingPoint2D arrowStart,
      final DrawingPoint2D arrowEnd,
      final List<ManualDrawingField> manualFields
  ) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction drawing reaction must not be null.");
    }
    return new ReactionDrawing(
        reaction,
        arrowKind == null ? ReactionArrowKind.FORWARD : arrowKind,
        arrowStart == null ? DrawingPoint2D.ORIGIN : arrowStart,
        arrowEnd == null ? DrawingPoint2D.of(8.0, 0.0) : arrowEnd,
        manualFields == null ? List.of() : List.copyOf(manualFields)
    );
  }

  public Reaction reaction() {
    return this.reaction;
  }

  public ReactionId reactionId() {
    return this.reaction.id();
  }

  public ReactionArrowKind arrowKind() {
    return this.arrowKind;
  }

  public DrawingPoint2D arrowStart() {
    return this.arrowStart;
  }

  public DrawingPoint2D arrowEnd() {
    return this.arrowEnd;
  }

  public List<ManualDrawingField> manualFields() {
    return this.manualFields;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionDrawing)) {
      return false;
    }
    final ReactionDrawing drawing = (ReactionDrawing) other;
    return Objects.equals(this.reaction, drawing.reaction)
        && this.arrowKind == drawing.arrowKind
        && Objects.equals(this.arrowStart, drawing.arrowStart)
        && Objects.equals(this.arrowEnd, drawing.arrowEnd)
        && Objects.equals(this.manualFields, drawing.manualFields);
  }

  public int hashCode() {
    int result = this.reaction.hashCode();
    result = 31 * result + this.arrowKind.hashCode();
    result = 31 * result + this.arrowStart.hashCode();
    result = 31 * result + this.arrowEnd.hashCode();
    result = 31 * result + this.manualFields.hashCode();
    return result;
  }
}