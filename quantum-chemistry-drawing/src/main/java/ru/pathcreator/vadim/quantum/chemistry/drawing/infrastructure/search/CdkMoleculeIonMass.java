/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.search;

import java.math.*;
import org.openscience.cdk.config.Isotopes;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeIonMass;

/** Isotope-aware composition arithmetic, not a prediction of the ion that an instrument will detect. */
public final class CdkMoleculeIonMass implements MoleculeIonMass {
  // CODATA 2022: https://physics.nist.gov/cgi-bin/cuu/Value?meu
  private static final BigDecimal ELECTRON = new BigDecimal("0.0005485799090441");

  @Override
  public Result calculate(final Molecule molecule, final Ion ion) {
    if (molecule.atomCount() == 0 || molecule.atomCount() > 1000) {
      throw new IllegalArgumentException("Расчёт поддерживает от 1 до 1000 явно заданных атомов.");
    }
    if (molecule.charge().value() != 0
        || molecule.atoms().stream().mapToInt(a -> a.formalCharge().value()).sum() != 0) {
      throw new IllegalArgumentException(
          "Для этой кнопки нужна нейтральная молекула M. Для готового иона введите m/z и источник вручную.");
    }
    try {
      final Isotopes isotopes = Isotopes.getInstance();
      BigDecimal mass = BigDecimal.ZERO;
      for (final Atom atom : molecule.atoms()) {
        final var isotope =
            atom.isotope() == null
                ? isotopes.getMajorIsotope(atom.element().symbol().value())
                : isotopes.getIsotope(atom.element().symbol().value(), atom.isotope().massNumber());
        if (isotope == null
            || isotope.getExactMass() == null
            || !Double.isFinite(isotope.getExactMass())) {
          throw new IllegalArgumentException(
              "Нет точной массы изотопа для " + atom.element().symbol().value());
        }
        mass = mass.add(BigDecimal.valueOf(isotope.getExactMass()));
      }
      final BigDecimal h = BigDecimal.valueOf(isotopes.getIsotope("H", 1).getExactMass());
      int charge = 1;
      String addition = "";
      switch (ion) {
        case PROTONATED -> {
          mass = mass.add(h);
          addition = " + H";
        }
        case DEPROTONATED -> {
          if (molecule.atoms().stream()
              .noneMatch(
                  a ->
                      a.element().atomicNumber() == 1
                          && (a.isotope() == null || a.isotope().massNumber() == 1))) {
            throw new IllegalArgumentException(
                "В структуре нет обычного H для удаления. Изотопный аддукт задайте вручную.");
          }
          mass = mass.subtract(h);
          charge = -1;
          addition = " − H";
        }
        case SODIATED -> {
          mass = mass.add(BigDecimal.valueOf(isotopes.getIsotope("Na", 23).getExactMass()));
          addition = " + Na";
        }
        case POTASSIATED -> {
          mass = mass.add(BigDecimal.valueOf(isotopes.getIsotope("K", 39).getExactMass()));
          addition = " + K";
        }
        case DOUBLE_PROTONATED -> {
          mass = mass.add(h.multiply(BigDecimal.TWO));
          charge = 2;
          addition = " + 2H";
        }
        case MOLECULAR_CATION -> {}
      }
      final String labels =
          String.join(
              ", ",
              molecule.atoms().stream()
                  .filter(a -> a.isotope() != null)
                  .map(a -> a.isotope().massNumber() + a.element().symbol().value())
                  .toList());
      return new Result(
          mass.subtract(ELECTRON.multiply(BigDecimal.valueOf(charge)))
              .divide(BigDecimal.valueOf(Math.abs(charge)), MathContext.DECIMAL128)
              .stripTrailingZeros(),
          molecule.formula().hillNotation()
              + (labels.isEmpty() ? "" : " [метки: " + labels + "]")
              + addition
              + "; z="
              + (charge > 0 ? "+" : "")
              + charge,
          "Состав из снимка ядра; массы изотопов CDK 2.12, электрон CODATA 2022. Учтены только явно заданные атомы; выбор аддукта не предсказывает спектр.");
    } catch (final java.io.IOException e) {
      throw new IllegalArgumentException("Таблица масс изотопов недоступна.", e);
    }
  }
}
