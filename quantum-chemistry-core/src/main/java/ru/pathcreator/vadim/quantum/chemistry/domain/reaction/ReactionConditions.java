/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class ReactionConditions {

  public static final ReactionConditions EMPTY =
      new ReactionConditions(null, null, List.of(), null);
  private final Temperature temperature;
  private final Pressure pressure;
  private final List<ReactionConditionComponent> components;
  private final String note;

  private ReactionConditions(
      final Temperature temperature,
      final Pressure pressure,
      final List<ReactionConditionComponent> components,
      final String note) {
    this.temperature = temperature;
    this.pressure = pressure;
    this.components = components;
    this.note = note;
  }

  public static ReactionConditions of(
      final Temperature temperature, final Pressure pressure, final String solvent, final String catalyst, final String note) {
    return ReactionConditions.of(
        temperature, pressure, ReactionConditions.componentsFromAliases(solvent, catalyst), note);
  }

  public static ReactionConditions of(
      final Temperature temperature,
      final Pressure pressure,
      final List<ReactionConditionComponent> components,
      final String note) {
    final List<ReactionConditionComponent> checkedComponents =
        ReactionConditions.checkedComponents(components);
    String checkedNote = ReactionConditions.optionalText(note, "Reaction note");
    if (temperature == null
        && pressure == null
        && checkedComponents.isEmpty()
        && checkedNote == null) {
      return EMPTY;
    }
    return new ReactionConditions(temperature, pressure, checkedComponents, checkedNote);
  }

  public Temperature temperature() {
    return this.temperature;
  }

  public Pressure pressure() {
    return this.pressure;
  }

  public List<ReactionConditionComponent> components() {
    return this.components;
  }

  public List<ReactionConditionComponent> componentsByRole(final ReactionComponentRole role) {
    if (role == null) {
      return List.of();
    }
    ArrayList<ReactionConditionComponent> result = new ArrayList<ReactionConditionComponent>();
    for (int i = 0; i < this.components.size(); ++i) {
      ReactionConditionComponent component = this.components.get(i);
      if (component.role() != role) continue;
      result.add(component);
    }
    return List.copyOf(result);
  }

  public String solvent() {
    return this.firstComponentName(ReactionComponentRole.SOLVENT);
  }

  public String catalyst() {
    return this.firstComponentName(ReactionComponentRole.CATALYST);
  }

  public String note() {
    return this.note;
  }

  public boolean empty() {
    return this.temperature == null
        && this.pressure == null
        && this.components.isEmpty()
        && this.note == null;
  }

  private String firstComponentName(final ReactionComponentRole role) {
    for (int i = 0; i < this.components.size(); ++i) {
      ReactionConditionComponent component = this.components.get(i);
      if (component.role() != role) continue;
      return component.name();
    }
    return null;
  }

  private static List<ReactionConditionComponent> componentsFromAliases(
      final String solvent, final String catalyst) {
    ArrayList<ReactionConditionComponent> result = new ArrayList<ReactionConditionComponent>();
    final String checkedSolvent = ReactionConditions.optionalText(solvent, "Reaction solvent");
    final String checkedCatalyst = ReactionConditions.optionalText(catalyst, "Reaction catalyst");
    if (checkedSolvent != null) {
      result.add(ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, checkedSolvent));
    }
    if (checkedCatalyst != null) {
      result.add(ReactionConditionComponent.named(ReactionComponentRole.CATALYST, checkedCatalyst));
    }
    return List.copyOf(result);
  }

  private static List<ReactionConditionComponent> checkedComponents(
      final List<ReactionConditionComponent> components) {
    if (components == null || components.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionComponent> result = new ArrayList<ReactionConditionComponent>();
    for (int i = 0; i < components.size(); ++i) {
      ReactionConditionComponent component = components.get(i);
      if (component == null) {
        throw new IllegalArgumentException("Reaction condition component must not be null.");
      }
      for (int j = 0; j < result.size(); ++j) {
        if (!component.equals(result.get(j))) continue;
        throw new IllegalArgumentException("Reaction conditions contain duplicate component.");
      }
      result.add(component);
    }
    return List.copyOf(result);
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
    if (!(other instanceof ReactionConditions)) {
      return false;
    }
    final ReactionConditions conditions = (ReactionConditions) other;
    return Objects.equals(this.temperature, conditions.temperature)
        && Objects.equals(this.pressure, conditions.pressure)
        && Objects.equals(this.components, conditions.components)
        && Objects.equals(this.note, conditions.note);
  }

  public int hashCode() {
    return Objects.hash(this.temperature, this.pressure, this.components, this.note);
  }
}