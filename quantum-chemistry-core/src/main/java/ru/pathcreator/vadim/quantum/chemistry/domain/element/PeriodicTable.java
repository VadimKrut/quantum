/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.element;

import java.util.HashMap;
import java.util.Map;

public final class PeriodicTable {

  public static final int MIN_ATOMIC_NUMBER = 1;
  public static final int MAX_ATOMIC_NUMBER = 118;

  private static final ChemicalElement[] ELEMENTS =
      new ChemicalElement[] {
        PeriodicTable.element(1, "H", "Hydrogen", 1.008),
        PeriodicTable.element(2, "He", "Helium", 4.002602),
        PeriodicTable.element(3, "Li", "Lithium", 6.94),
        PeriodicTable.element(4, "Be", "Beryllium", 9.0121831),
        PeriodicTable.element(5, "B", "Boron", 10.81),
        PeriodicTable.element(6, "C", "Carbon", 12.011),
        PeriodicTable.element(7, "N", "Nitrogen", 14.007),
        PeriodicTable.element(8, "O", "Oxygen", 15.999),
        PeriodicTable.element(9, "F", "Fluorine", 18.998403163),
        PeriodicTable.element(10, "Ne", "Neon", 20.1797),
        PeriodicTable.element(11, "Na", "Sodium", 22.98976928),
        PeriodicTable.element(12, "Mg", "Magnesium", 24.305),
        PeriodicTable.element(13, "Al", "Aluminium", 26.9815385),
        PeriodicTable.element(14, "Si", "Silicon", 28.085),
        PeriodicTable.element(15, "P", "Phosphorus", 30.973761998),
        PeriodicTable.element(16, "S", "Sulfur", 32.06),
        PeriodicTable.element(17, "Cl", "Chlorine", 35.45),
        PeriodicTable.element(18, "Ar", "Argon", 39.948),
        PeriodicTable.element(19, "K", "Potassium", 39.0983),
        PeriodicTable.element(20, "Ca", "Calcium", 40.078),
        PeriodicTable.element(21, "Sc", "Scandium", 44.955908),
        PeriodicTable.element(22, "Ti", "Titanium", 47.867),
        PeriodicTable.element(23, "V", "Vanadium", 50.9415),
        PeriodicTable.element(24, "Cr", "Chromium", 51.9961),
        PeriodicTable.element(25, "Mn", "Manganese", 54.938044),
        PeriodicTable.element(26, "Fe", "Iron", 55.845),
        PeriodicTable.element(27, "Co", "Cobalt", 58.933194),
        PeriodicTable.element(28, "Ni", "Nickel", 58.6934),
        PeriodicTable.element(29, "Cu", "Copper", 63.546),
        PeriodicTable.element(30, "Zn", "Zinc", 65.38),
        PeriodicTable.element(31, "Ga", "Gallium", 69.723),
        PeriodicTable.element(32, "Ge", "Germanium", 72.63),
        PeriodicTable.element(33, "As", "Arsenic", 74.921595),
        PeriodicTable.element(34, "Se", "Selenium", 78.971),
        PeriodicTable.element(35, "Br", "Bromine", 79.904),
        PeriodicTable.element(36, "Kr", "Krypton", 83.798),
        PeriodicTable.element(37, "Rb", "Rubidium", 85.4678),
        PeriodicTable.element(38, "Sr", "Strontium", 87.62),
        PeriodicTable.element(39, "Y", "Yttrium", 88.90584),
        PeriodicTable.element(40, "Zr", "Zirconium", 91.224),
        PeriodicTable.element(41, "Nb", "Niobium", 92.90637),
        PeriodicTable.element(42, "Mo", "Molybdenum", 95.95),
        PeriodicTable.element(43, "Tc", "Technetium", 98.0),
        PeriodicTable.element(44, "Ru", "Ruthenium", 101.07),
        PeriodicTable.element(45, "Rh", "Rhodium", 102.9055),
        PeriodicTable.element(46, "Pd", "Palladium", 106.42),
        PeriodicTable.element(47, "Ag", "Silver", 107.8682),
        PeriodicTable.element(48, "Cd", "Cadmium", 112.414),
        PeriodicTable.element(49, "In", "Indium", 114.818),
        PeriodicTable.element(50, "Sn", "Tin", 118.71),
        PeriodicTable.element(51, "Sb", "Antimony", 121.76),
        PeriodicTable.element(52, "Te", "Tellurium", 127.6),
        PeriodicTable.element(53, "I", "Iodine", 126.90447),
        PeriodicTable.element(54, "Xe", "Xenon", 131.293),
        PeriodicTable.element(55, "Cs", "Caesium", 132.90545196),
        PeriodicTable.element(56, "Ba", "Barium", 137.327),
        PeriodicTable.element(57, "La", "Lanthanum", 138.90547),
        PeriodicTable.element(58, "Ce", "Cerium", 140.116),
        PeriodicTable.element(59, "Pr", "Praseodymium", 140.90766),
        PeriodicTable.element(60, "Nd", "Neodymium", 144.242),
        PeriodicTable.element(61, "Pm", "Promethium", 145.0),
        PeriodicTable.element(62, "Sm", "Samarium", 150.36),
        PeriodicTable.element(63, "Eu", "Europium", 151.964),
        PeriodicTable.element(64, "Gd", "Gadolinium", 157.25),
        PeriodicTable.element(65, "Tb", "Terbium", 158.92535),
        PeriodicTable.element(66, "Dy", "Dysprosium", 162.5),
        PeriodicTable.element(67, "Ho", "Holmium", 164.93033),
        PeriodicTable.element(68, "Er", "Erbium", 167.259),
        PeriodicTable.element(69, "Tm", "Thulium", 168.93422),
        PeriodicTable.element(70, "Yb", "Ytterbium", 173.045),
        PeriodicTable.element(71, "Lu", "Lutetium", 174.9668),
        PeriodicTable.element(72, "Hf", "Hafnium", 178.49),
        PeriodicTable.element(73, "Ta", "Tantalum", 180.94788),
        PeriodicTable.element(74, "W", "Tungsten", 183.84),
        PeriodicTable.element(75, "Re", "Rhenium", 186.207),
        PeriodicTable.element(76, "Os", "Osmium", 190.23),
        PeriodicTable.element(77, "Ir", "Iridium", 192.217),
        PeriodicTable.element(78, "Pt", "Platinum", 195.084),
        PeriodicTable.element(79, "Au", "Gold", 196.966569),
        PeriodicTable.element(80, "Hg", "Mercury", 200.592),
        PeriodicTable.element(81, "Tl", "Thallium", 204.38),
        PeriodicTable.element(82, "Pb", "Lead", 207.2),
        PeriodicTable.element(83, "Bi", "Bismuth", 208.9804),
        PeriodicTable.element(84, "Po", "Polonium", 209.0),
        PeriodicTable.element(85, "At", "Astatine", 210.0),
        PeriodicTable.element(86, "Rn", "Radon", 222.0),
        PeriodicTable.element(87, "Fr", "Francium", 223.0),
        PeriodicTable.element(88, "Ra", "Radium", 226.0),
        PeriodicTable.element(89, "Ac", "Actinium", 227.0),
        PeriodicTable.element(90, "Th", "Thorium", 232.0377),
        PeriodicTable.element(91, "Pa", "Protactinium", 231.03588),
        PeriodicTable.element(92, "U", "Uranium", 238.02891),
        PeriodicTable.element(93, "Np", "Neptunium", 237.0),
        PeriodicTable.element(94, "Pu", "Plutonium", 244.0),
        PeriodicTable.element(95, "Am", "Americium", 243.0),
        PeriodicTable.element(96, "Cm", "Curium", 247.0),
        PeriodicTable.element(97, "Bk", "Berkelium", 247.0),
        PeriodicTable.element(98, "Cf", "Californium", 251.0),
        PeriodicTable.element(99, "Es", "Einsteinium", 252.0),
        PeriodicTable.element(100, "Fm", "Fermium", 257.0),
        PeriodicTable.element(101, "Md", "Mendelevium", 258.0),
        PeriodicTable.element(102, "No", "Nobelium", 259.0),
        PeriodicTable.element(103, "Lr", "Lawrencium", 266.0),
        PeriodicTable.element(104, "Rf", "Rutherfordium", 267.0),
        PeriodicTable.element(105, "Db", "Dubnium", 268.0),
        PeriodicTable.element(106, "Sg", "Seaborgium", 269.0),
        PeriodicTable.element(107, "Bh", "Bohrium", 270.0),
        PeriodicTable.element(108, "Hs", "Hassium", 277.0),
        PeriodicTable.element(109, "Mt", "Meitnerium", 278.0),
        PeriodicTable.element(110, "Ds", "Darmstadtium", 281.0),
        PeriodicTable.element(111, "Rg", "Roentgenium", 282.0),
        PeriodicTable.element(112, "Cn", "Copernicium", 285.0),
        PeriodicTable.element(113, "Nh", "Nihonium", 286.0),
        PeriodicTable.element(114, "Fl", "Flerovium", 289.0),
        PeriodicTable.element(115, "Mc", "Moscovium", 290.0),
        PeriodicTable.element(116, "Lv", "Livermorium", 293.0),
        PeriodicTable.element(117, "Ts", "Tennessine", 294.0),
        PeriodicTable.element(118, "Og", "Oganesson", 294.0)
      };
  private static final ChemicalElement[] BY_ATOMIC_NUMBER = PeriodicTable.createByAtomicNumber();
  private static final Map<String, ChemicalElement> BY_SYMBOL = PeriodicTable.createBySymbol();

  private PeriodicTable() {}

  public static ChemicalElement require(final ElementSymbol symbol) {
    if (symbol == null) {
      throw new IllegalArgumentException("Element symbol must not be null.");
    }
    final ChemicalElement element = BY_SYMBOL.get(symbol.value());
    if (element == null) {
      throw new IllegalArgumentException("Unsupported element symbol: " + symbol.value() + ".");
    }
    return element;
  }

  public static ChemicalElement requireAtomicNumber(final int atomicNumber) {
    if (!PeriodicTable.containsAtomicNumber(atomicNumber)) {
      throw new IllegalArgumentException("Unsupported atomic number: " + atomicNumber + ".");
    }
    return BY_ATOMIC_NUMBER[atomicNumber];
  }

  public static boolean contains(final ElementSymbol symbol) {
    if (symbol == null) {
      return false;
    }
    return BY_SYMBOL.containsKey(symbol.value());
  }

  public static boolean containsAtomicNumber(final int atomicNumber) {
    return atomicNumber >= MIN_ATOMIC_NUMBER
        && atomicNumber <= MAX_ATOMIC_NUMBER
        && BY_ATOMIC_NUMBER[atomicNumber] != null;
  }

  public static int atomicNumberOf(final ElementSymbol symbol) {
    return PeriodicTable.require(symbol).atomicNumber();
  }

  public static int elementCount() {
    return ELEMENTS.length;
  }

  private static ChemicalElement element(
      final int atomicNumber,
      final String symbol,
      final String englishName,
      final double atomicMass) {
    return ChemicalElement.of(atomicNumber, ElementSymbol.of(symbol), englishName, atomicMass);
  }

  private static ChemicalElement[] createByAtomicNumber() {
    final ChemicalElement[] result = new ChemicalElement[MAX_ATOMIC_NUMBER + 1];
    for (int i = 0; i < ELEMENTS.length; ++i) {
      final ChemicalElement element = ELEMENTS[i];
      if (result[element.atomicNumber()] != null) {
        throw new IllegalStateException(
            "Duplicate periodic table atomic number: " + element.atomicNumber() + ".");
      }
      result[element.atomicNumber()] = element;
    }
    return result;
  }

  private static Map<String, ChemicalElement> createBySymbol() {
    final HashMap<String, ChemicalElement> result = new HashMap<String, ChemicalElement>();
    for (int i = 0; i < ELEMENTS.length; ++i) {
      final ChemicalElement element = ELEMENTS[i];
      if (result.containsKey(element.symbol().value())) {
        throw new IllegalStateException(
            "Duplicate periodic table element symbol: " + element.symbol().value() + ".");
      }
      result.put(element.symbol().value(), element);
    }
    return Map.copyOf(result);
  }
}