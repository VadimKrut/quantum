/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;

public final class ReactionConditionComponent {

  private final ReactionComponentRole role;
  private final String name;
  private final SubstanceAmount amount;
  private final MolarConcentration concentration;
  private final StoichiometricEquivalent equivalent;
  private final ReactionComponentLoading loading;
  private final ReactionComponentPurity purity;
  private final ReactionPhase phase;
  private final String note;

  private ReactionConditionComponent(
      final ReactionComponentRole role,
      final String name,
      final SubstanceAmount amount,
      final MolarConcentration concentration,
      final StoichiometricEquivalent equivalent,
      final ReactionComponentLoading loading,
      final ReactionComponentPurity purity,
      final ReactionPhase phase,
      final String note) {
    this.role = role;
    this.name = name;
    this.amount = amount;
    this.concentration = concentration;
    this.equivalent = equivalent;
    this.loading = loading;
    this.purity = purity;
    this.phase = phase;
    this.note = note;
  }

  public static ReactionConditionComponent of(
      final ReactionComponentRole role,
      final String name,
      final SubstanceAmount amount,
      final MolarConcentration concentration,
      final ReactionPhase phase,
      final String note) {
    return ReactionConditionComponent.of(
        role, name, amount, concentration, null, null, null, phase, note);
  }

  public static ReactionConditionComponent of(
      final ReactionComponentRole role,
      final String name,
      final SubstanceAmount amount,
      final MolarConcentration concentration,
      final StoichiometricEquivalent equivalent,
      final ReactionComponentLoading loading,
      final ReactionComponentPurity purity,
      final ReactionPhase phase,
      final String note) {
    if (role == null) {
      throw new IllegalArgumentException("Reaction condition component role must not be null.");
    }
    final String checkedName = TextValue.requireText(name, "Reaction condition component name");
    final String checkedNote =
        ReactionConditionComponent.optionalText(note, "Reaction condition component note");
    return new ReactionConditionComponent(
        role,
        checkedName,
        amount,
        concentration,
        equivalent,
        loading,
        purity,
        phase == null ? ReactionPhase.UNKNOWN : phase,
        checkedNote);
  }

  public static ReactionConditionComponent named(
      final ReactionComponentRole role,
      final String name
  ) {
    return ReactionConditionComponent.of(role, name, null, null, ReactionPhase.UNKNOWN, null);
  }

  public ReactionComponentRole role() {
    return this.role;
  }

  public String name() {
    return this.name;
  }

  public SubstanceAmount amount() {
    return this.amount;
  }

  public boolean hasAmount() {
    return this.amount != null;
  }

  public MolarConcentration concentration() {
    return this.concentration;
  }

  public boolean hasConcentration() {
    return this.concentration != null;
  }

  public StoichiometricEquivalent equivalent() {
    return this.equivalent;
  }

  public boolean hasEquivalent() {
    return this.equivalent != null;
  }

  public ReactionComponentLoading loading() {
    return this.loading;
  }

  public boolean hasLoading() {
    return this.loading != null;
  }

  public ReactionComponentPurity purity() {
    return this.purity;
  }

  public boolean hasPurity() {
    return this.purity != null;
  }

  public ReactionPhase phase() {
    return this.phase;
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  private static String optionalText(
      final String value,
      final String subjectName
  ) {
    if (value == null) {
      return null;
    }
    return TextValue.requireText(value, subjectName);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionComponent)) {
      return false;
    }
    final ReactionConditionComponent component = (ReactionConditionComponent) other;
    return this.role == component.role
        && this.phase == component.phase
        && Objects.equals(this.name, component.name)
        && Objects.equals(this.amount, component.amount)
        && Objects.equals(this.concentration, component.concentration)
        && Objects.equals(this.equivalent, component.equivalent)
        && Objects.equals(this.loading, component.loading)
        && Objects.equals(this.purity, component.purity)
        && Objects.equals(this.note, component.note);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {
          this.role,
          this.name,
          this.amount,
          this.concentration,
          this.equivalent,
          this.loading,
          this.purity,
          this.phase,
          this.note
        });
  }
}