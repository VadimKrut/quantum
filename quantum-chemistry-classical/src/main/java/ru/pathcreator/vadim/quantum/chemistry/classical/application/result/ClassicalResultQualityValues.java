/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.result;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;

/** Добавляет машинно-читаемое описание качества и маршрута classical chemistry результата. */
public final class ClassicalResultQualityValues {

  private ClassicalResultQualityValues() {
    throw new AssertionError("Classical result quality utility must not be instantiated.");
  }

  public static void addExact(
      final List<ClassicalResultValue> values,
      final String backend,
      final String model,
      final String scope) {
    ClassicalResultQualityValues.add(values, "exact", backend, model, scope, "none");
  }

  public static void addApproximate(
      final List<ClassicalResultValue> values,
      final String backend,
      final String model,
      final String scope,
      final String limitation) {
    ClassicalResultQualityValues.add(values, "approximate", backend, model, scope, limitation);
  }

  public static void addExternal(
      final List<ClassicalResultValue> values,
      final String backend,
      final String model,
      final String scope,
      final String limitation) {
    ClassicalResultQualityValues.add(values, "external", backend, model, scope, limitation);
  }

  public static void addFallback(
      final List<ClassicalResultValue> values,
      final String backend,
      final String model,
      final String scope,
      final String limitation) {
    ClassicalResultQualityValues.add(values, "fallback", backend, model, scope, limitation);
  }

  private static void add(
      final List<ClassicalResultValue> values,
      final String level,
      final String backend,
      final String model,
      final String scope,
      final String limitation) {
    if (values == null) {
      throw new IllegalArgumentException(
          "Classical result quality values target must not be null.");
    }
    values.add(ClassicalResultValue.text("quality_level", level));
    values.add(ClassicalResultValue.text("quality_backend", backend));
    values.add(ClassicalResultValue.text("quality_model", model));
    values.add(ClassicalResultValue.text("quality_scope", scope));
    values.add(
        ClassicalResultValue.text(
            "quality_limitation",
            limitation == null || limitation.isBlank() ? "none" : limitation));
  }
}
