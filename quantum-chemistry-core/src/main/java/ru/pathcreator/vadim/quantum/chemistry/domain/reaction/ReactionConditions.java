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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;

public final class ReactionConditions {

  public static final ReactionConditions EMPTY =
      new ReactionConditions(null, null, null, List.of(), List.of(), null);
  private final Temperature temperature;
  private final Pressure pressure;
  private final TimeDuration duration;
  private final List<ReactionConditionComponent> components;
  private final List<ReactionConditionFeature> features;
  private final String note;

  private ReactionConditions(
      final Temperature temperature,
      final Pressure pressure,
      final TimeDuration duration,
      final List<ReactionConditionComponent> components,
      final List<ReactionConditionFeature> features,
      final String note) {
    this.temperature = temperature;
    this.pressure = pressure;
    this.duration = duration;
    this.components = components;
    this.features = features;
    this.note = note;
  }

  public static ReactionConditions of(
      final Temperature temperature,
      final Pressure pressure,
      final String solvent,
      final String catalyst,
      final String note) {
    return ReactionConditions.of(
        temperature,
        pressure,
        null,
        ReactionConditions.componentsFromAliases(solvent, catalyst),
        List.of(),
        note);
  }

  public static ReactionConditions of(
      final Temperature temperature,
      final Pressure pressure,
      final List<ReactionConditionComponent> components,
      final String note) {
    return ReactionConditions.of(temperature, pressure, null, components, List.of(), note);
  }

  public static ReactionConditions of(
      final Temperature temperature,
      final Pressure pressure,
      final TimeDuration duration,
      final List<ReactionConditionComponent> components,
      final List<ReactionConditionFeature> features,
      final String note) {
    final List<ReactionConditionComponent> checkedComponents =
        ReactionConditions.checkedComponents(components);
    final List<ReactionConditionFeature> checkedFeatures =
        ReactionConditions.checkedFeatures(features);
    final String checkedNote = ReactionConditions.optionalText(note, "Reaction note");
    if (temperature == null
        && pressure == null
        && duration == null
        && checkedComponents.isEmpty()
        && checkedFeatures.isEmpty()
        && checkedNote == null) {
      return EMPTY;
    }
    return new ReactionConditions(
        temperature, pressure, duration, checkedComponents, checkedFeatures, checkedNote);
  }

  public Temperature temperature() {
    return this.temperature;
  }

  public Pressure pressure() {
    return this.pressure;
  }

  public TimeDuration duration() {
    return this.duration;
  }

  public boolean hasDuration() {
    return this.duration != null;
  }

  public List<ReactionConditionComponent> components() {
    return this.components;
  }

  public List<ReactionConditionFeature> features() {
    return this.features;
  }

  public boolean hasFeature(final ReactionConditionFeatureKind kind) {
    if (kind == null) {
      return false;
    }
    for (int i = 0; i < this.features.size(); ++i) {
      if (this.features.get(i).kind() != kind) continue;
      return true;
    }
    return false;
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
        && this.duration == null
        && this.components.isEmpty()
        && this.features.isEmpty()
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

  private static List<ReactionConditionFeature> checkedFeatures(
      final List<ReactionConditionFeature> features) {
    if (features == null || features.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionFeature> result = new ArrayList<ReactionConditionFeature>();
    for (int i = 0; i < features.size(); ++i) {
      final ReactionConditionFeature feature = features.get(i);
      if (feature == null) {
        throw new IllegalArgumentException("Reaction condition feature must not be null.");
      }
      for (int j = 0; j < result.size(); ++j) {
        if (!feature.equals(result.get(j))) continue;
        throw new IllegalArgumentException("Reaction conditions contain duplicate feature.");
      }
      result.add(feature);
    }
    return List.copyOf(result);
  }

  private static List<ReactionConditionComponent> checkedComponents(
      final List<ReactionConditionComponent> components) {
    if (components == null || components.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionComponent> result =
        new ArrayList<ReactionConditionComponent>();
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

  private static String optionalText(final String value, final String subjectName) {
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
        && Objects.equals(this.duration, conditions.duration)
        && Objects.equals(this.components, conditions.components)
        && Objects.equals(this.features, conditions.features)
        && Objects.equals(this.note, conditions.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.temperature, this.pressure, this.duration, this.components, this.features, this.note);
  }
}
