/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.template;

import java.util.List;

/** Неизменяемая библиотека именованных шаблонов органических реакций. */
public final class OrganicReactionTemplateLibrary {

  public static final OrganicReactionTemplateLibrary EMPTY =
      new OrganicReactionTemplateLibrary(List.of());

  private final List<OrganicReactionTemplate> templates;

  private OrganicReactionTemplateLibrary(final List<OrganicReactionTemplate> templates) {
    this.templates = templates;
  }

  public static OrganicReactionTemplateLibrary of(final List<OrganicReactionTemplate> templates) {
    if (templates == null) {
      throw new IllegalArgumentException("Organic reaction template list must not be null.");
    }
    for (int i = 0; i < templates.size(); ++i) {
      final OrganicReactionTemplate template = templates.get(i);
      if (template == null) {
        throw new IllegalArgumentException("Organic reaction template must not be null.");
      }
      for (int j = i + 1; j < templates.size(); ++j) {
        if (!template.id().equals(templates.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException("Organic reaction template ids must be unique.");
      }
    }
    if (templates.isEmpty()) {
      return EMPTY;
    }
    return new OrganicReactionTemplateLibrary(List.copyOf(templates));
  }

  public List<OrganicReactionTemplate> templates() {
    return this.templates;
  }

  public boolean empty() {
    return this.templates.isEmpty();
  }

  public int size() {
    return this.templates.size();
  }
}
