/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;

/** Условие выбора атома реагента, к которому можно применить структурное преобразование. */
public final class ReactantAtomSelector {

  public static final ReactantAtomSelector ANY = new ReactantAtomSelector(null, List.of(), null);

  private final ElementSymbol requiredElement;
  private final List<ElementSymbol> allowedElements;
  private final String explanation;

  private ReactantAtomSelector(
      final ElementSymbol requiredElement,
      final List<ElementSymbol> allowedElements,
      final String explanation) {
    this.requiredElement = requiredElement;
    this.allowedElements = allowedElements;
    this.explanation = explanation;
  }

  public static ReactantAtomSelector element(final String elementSymbol) {
    final ElementSymbol checkedElement = ElementSymbol.of(elementSymbol);
    return new ReactantAtomSelector(
        checkedElement, List.of(checkedElement), "atom element is " + checkedElement.value());
  }

  public static ReactantAtomSelector element(
      final ElementSymbol elementSymbol, final String explanation) {
    if (elementSymbol == null) {
      throw new IllegalArgumentException("Reactant atom selector element must not be null.");
    }
    return new ReactantAtomSelector(
        elementSymbol, List.of(elementSymbol), TextValue.optionalText(explanation));
  }

  public static ReactantAtomSelector elements(
      final List<ElementSymbol> elementSymbols, final String explanation) {
    if (elementSymbols == null || elementSymbols.isEmpty()) {
      throw new IllegalArgumentException("Reactant atom selector element list must not be empty.");
    }
    for (int i = 0; i < elementSymbols.size(); ++i) {
      if (elementSymbols.get(i) == null) {
        throw new IllegalArgumentException(
            "Reactant atom selector element list must not contain null.");
      }
    }
    return new ReactantAtomSelector(
        null, List.copyOf(elementSymbols), TextValue.optionalText(explanation));
  }

  public static ReactantAtomSelector halogen() {
    return ReactantAtomSelector.elements(
        List.of(
            ElementSymbol.of("F"),
            ElementSymbol.of("Cl"),
            ElementSymbol.of("Br"),
            ElementSymbol.of("I")),
        "atom element is a halogen leaving group");
  }

  public boolean matches(final Atom atom) {
    if (atom == null) {
      throw new IllegalArgumentException("Reactant atom selector atom must not be null.");
    }
    if (this.requiredElement != null) {
      return this.requiredElement.equals(atom.element().symbol());
    }
    return this.allowedElements.isEmpty() || this.allowedElements.contains(atom.element().symbol());
  }

  public boolean hasRequiredElement() {
    return this.requiredElement != null;
  }

  public ElementSymbol requiredElement() {
    if (this.requiredElement == null) {
      throw new IllegalStateException("Reactant atom selector does not require an element.");
    }
    return this.requiredElement;
  }

  public List<ElementSymbol> allowedElements() {
    return this.allowedElements;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean hasExplanation() {
    return this.explanation != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactantAtomSelector)) {
      return false;
    }
    final ReactantAtomSelector selector = (ReactantAtomSelector) other;
    return Objects.equals(this.requiredElement, selector.requiredElement)
        && Objects.equals(this.allowedElements, selector.allowedElements)
        && Objects.equals(this.explanation, selector.explanation);
  }

  public int hashCode() {
    return Objects.hash(this.requiredElement, this.allowedElements, this.explanation);
  }
}
