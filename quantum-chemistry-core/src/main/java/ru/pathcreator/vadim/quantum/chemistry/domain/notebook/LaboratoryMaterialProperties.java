/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Explicit sample properties; missing purity is never treated as 100 percent. */
public record LaboratoryMaterialProperties(
    BigDecimal molarMass,
    BigDecimal density,
    BigDecimal purityPercent,
    BigDecimal molarity,
    String source,
    String structureSignature) {
  public static final LaboratoryMaterialProperties EMPTY =
      new LaboratoryMaterialProperties(null, null, null, null, "", "");

  public LaboratoryMaterialProperties {
    molarMass = checked(molarMass, "Молярная масса", false);
    density = checked(density, "Плотность", false);
    purityPercent = checked(purityPercent, "Массовая доля", true);
    molarity = checked(molarity, "Молярная концентрация", true);
    if (purityPercent != null && purityPercent.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalArgumentException("Массовая доля должна быть от 0 до 100 %.");
    }
    source = Objects.requireNonNullElse(source, "").strip();
    structureSignature = Objects.requireNonNullElse(structureSignature, "");
    if (molarMass != null
        || density != null
        || purityPercent != null
        || molarity != null
        || !source.isBlank()) {
      if (source.isBlank()) {
        throw new IllegalArgumentException(
            "Укажите источник свойств или основание принятого значения.");
      }
      if (!structureSignature.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException(
            "Свойства должны быть подтверждены для конкретной структуры.");
      }
    } else {
      source = "";
      structureSignature = "";
    }
  }

  public boolean empty() {
    return this.equals(EMPTY);
  }

  public boolean matches(final Molecule molecule) {
    return !this.empty() && this.structureSignature.equals(signature(molecule));
  }

  private static BigDecimal checked(final BigDecimal value, final String name, final boolean zero) {
    if (value == null) {
      return null;
    }
    if (value.signum() < 0 || !zero && value.signum() == 0) {
      throw new IllegalArgumentException(
          name + (zero ? " не может быть отрицательной." : " должна быть больше нуля."));
    }
    return value.stripTrailingZeros();
  }

  /** Stable identity-bound signature, excluding names, layouts and coordinates. Not an isomorphism key. */
  public static String signature(final Molecule molecule) {
    final List<String> parts = new ArrayList<>();
    parts.add("state:" + molecule.charge().value() + ":" + molecule.spinMultiplicity().value());
    molecule
        .atoms()
        .forEach(
            atom ->
                parts.add(
                    "atom:"
                        + atom.id().value()
                        + ":"
                        + atom.element().symbol().value()
                        + ":"
                        + atom.formalCharge().value()
                        + ":"
                        + (atom.hasIsotope() ? atom.isotope().massNumber() : 0)
                        + ":"
                        + atom.radicalState().unpairedElectrons()));
    molecule
        .bonds()
        .forEach(
            bond -> {
              final String a = bond.firstAtomId().value(), b = bond.secondAtomId().value();
              parts.add(
                  "bond:"
                      + (a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a)
                      + ":"
                      + bond.type().name());
            });
    molecule
        .stereochemistry()
        .centers()
        .forEach(
            center ->
                parts.add(
                    "stereo:"
                        + center.kind().name()
                        + ":"
                        + center.descriptor().name()
                        + ":"
                        + center.primaryAtomId().value()
                        + ":"
                        + (center.hasSecondaryAtom() ? center.secondaryAtomId().value() : "")
                        + ":"
                        + center.referenceAtomIds().stream().map(atom -> atom.value()).toList()));
    final String parity =
        molecule
            .metadata()
            .attributes()
            .get(
                ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry
                    .ORDERED_PARITY_ATTRIBUTE);
    if (parity != null) {
      parts.add("ordered-parity:" + parity);
    }
    Collections.sort(parts);
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(String.join("\n", parts).getBytes(StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
