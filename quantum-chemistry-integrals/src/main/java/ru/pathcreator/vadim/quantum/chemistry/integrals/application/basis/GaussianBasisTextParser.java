/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application.basis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianBasisLibrary;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianShellType;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/** Разбирает переносимое текстовое описание Gaussian basis в Java-native basis-set модель. */
public final class GaussianBasisTextParser {

  public GaussianBasisSet parseForMolecule(
      final String basisName, final String source, final IntegralMolecule molecule) {
    if (basisName == null || basisName.isBlank()) {
      throw new IllegalArgumentException("Gaussian basis text name must not be blank.");
    }
    if (source == null || source.isBlank()) {
      throw new IllegalArgumentException("Gaussian basis text source must not be blank.");
    }
    if (molecule == null) {
      throw new IllegalArgumentException("Gaussian basis text molecule must not be null.");
    }
    final Map<Integer, List<ShellBlock>> blocksByAtomicNumber =
        this.parseBlocksByAtomicNumber(source);
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(molecule.atomCount() * 8);
    final List<IntegralAtom> atoms = molecule.atoms();
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      final int atomicNumber = atoms.get(atomIndex).atomicNumber();
      final List<ShellBlock> blocks = blocksByAtomicNumber.get(Integer.valueOf(atomicNumber));
      if (blocks == null || blocks.isEmpty()) {
        throw new IllegalArgumentException(
            "Gaussian basis text does not contain shell block for atomic number "
                + atomicNumber
                + ".");
      }
      this.addAtomShells(atomIndex, blocks, orbitals);
    }
    return GaussianBasisSet.of(basisName, orbitals);
  }

  private Map<Integer, List<ShellBlock>> parseBlocksByAtomicNumber(final String source) {
    final String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n");
    final HashMap<Integer, List<ShellBlock>> blocksByAtomicNumber =
        new HashMap<Integer, List<ShellBlock>>();
    int cursor = 0;
    while (cursor < lines.length) {
      final String line = GaussianBasisTextParser.clean(lines[cursor]);
      ++cursor;
      if (line.isEmpty() || line.equals("****") || GaussianBasisTextParser.skipDirective(line)) {
        continue;
      }
      if (GaussianBasisTextParser.ecpDirective(line)) {
        throw new IllegalArgumentException(
            "Gaussian basis text contains ECP or pseudopotential section; Java-native ECP integral "
                + "engine is not available yet.");
      }
      final String[] header = line.split("\\s+");
      if (header.length < 2) {
        throw new IllegalArgumentException(
            "Gaussian basis shell header must contain element and shell type.");
      }
      final int atomicNumber = GaussianBasisTextParser.atomicNumber(header[0]);
      if (GaussianBasisTextParser.shellToken(header[1])) {
        final ShellReadResult result = this.readLegacyElementShell(header[1], lines, cursor);
        cursor = result.cursor();
        this.addBlock(blocksByAtomicNumber, atomicNumber, result.block());
        continue;
      }
      cursor = this.readElementBlock(atomicNumber, lines, cursor, blocksByAtomicNumber);
    }
    return Map.copyOf(blocksByAtomicNumber);
  }

  private ShellReadResult readLegacyElementShell(
      final String shellType, final String[] lines, final int startCursor) {
    final ArrayList<double[]> primitiveRows = new ArrayList<double[]>();
    int cursor = startCursor;
    while (cursor < lines.length) {
      final String primitiveLine = GaussianBasisTextParser.clean(lines[cursor]);
      if (primitiveLine.isEmpty()) {
        ++cursor;
        continue;
      }
      if (primitiveLine.equals("****")) {
        ++cursor;
        break;
      }
      final String[] primitiveParts = primitiveLine.split("\\s+");
      if (primitiveParts.length >= 2
          && GaussianBasisTextParser.atomicSymbolToken(primitiveParts[0])
          && GaussianBasisTextParser.shellToken(primitiveParts[1])) {
        break;
      }
      primitiveRows.add(
          GaussianBasisTextParser.parsePrimitiveRow(
              shellType.toUpperCase(Locale.ROOT), primitiveParts, 1.0));
      ++cursor;
    }
    return ShellReadResult.of(
        ShellBlock.of(shellType.toUpperCase(Locale.ROOT), primitiveRows), cursor);
  }

  private int readElementBlock(
      final int atomicNumber,
      final String[] lines,
      final int startCursor,
      final Map<Integer, List<ShellBlock>> blocksByAtomicNumber) {
    int cursor = startCursor;
    while (cursor < lines.length) {
      final String line = GaussianBasisTextParser.clean(lines[cursor]);
      ++cursor;
      if (line.isEmpty()) {
        continue;
      }
      if (line.equals("****")) {
        break;
      }
      if (GaussianBasisTextParser.ecpDirective(line)) {
        throw new IllegalArgumentException(
            "Gaussian basis element block contains ECP or pseudopotential section; Java-native ECP "
                + "integral engine is not available yet.");
      }
      final String[] shellHeader = line.split("\\s+");
      if (shellHeader.length < 2 || !GaussianBasisTextParser.shellToken(shellHeader[0])) {
        throw new IllegalArgumentException(
            "Gaussian basis element block must contain shell headers.");
      }
      final ShellHeader parsedHeader = GaussianBasisTextParser.parseShellHeader(shellHeader);
      final ArrayList<double[]> primitiveRows =
          new ArrayList<double[]>(parsedHeader.primitiveCount());
      while (primitiveRows.size() < parsedHeader.primitiveCount()) {
        if (cursor >= lines.length) {
          throw new IllegalArgumentException(
              "Gaussian basis shell ended before declared primitive count.");
        }
        final String primitiveLine = GaussianBasisTextParser.clean(lines[cursor]);
        ++cursor;
        if (primitiveLine.isEmpty()) {
          continue;
        }
        if (primitiveLine.equals("****")) {
          throw new IllegalArgumentException(
              "Gaussian basis shell ended before declared primitive count.");
        }
        primitiveRows.add(
            GaussianBasisTextParser.parsePrimitiveRow(
                parsedHeader.shellType(), primitiveLine.split("\\s+"), parsedHeader.scale()));
      }
      this.addBlock(
          blocksByAtomicNumber,
          atomicNumber,
          ShellBlock.of(parsedHeader.shellType(), primitiveRows));
    }
    return cursor;
  }

  private void addBlock(
      final Map<Integer, List<ShellBlock>> blocksByAtomicNumber,
      final int atomicNumber,
      final ShellBlock block) {
    blocksByAtomicNumber
        .computeIfAbsent(
            Integer.valueOf(atomicNumber), (final Integer ignored) -> new ArrayList<ShellBlock>())
        .add(block);
  }

  private void addAtomShells(
      final int atomIndex,
      final List<ShellBlock> blocks,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    for (int i = 0; i < blocks.size(); ++i) {
      final ShellBlock block = blocks.get(i);
      if ("SP".equals(block.shellType())) {
        this.addSpShell(atomIndex, block, orbitals);
      } else {
        this.addCartesianShell(
            atomIndex, block, GaussianBasisTextParser.shellType(block.shellType()), orbitals);
      }
    }
  }

  private void addSpShell(
      final int atomIndex,
      final ShellBlock block,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    final double[] exponents = block.column(0);
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                exponents, block.column(1), GaussianAngularMomentum.S)));
    final List<GaussianAngularMomentum> pMomenta =
        GaussianAngularMomentum.cartesianShell(GaussianShellType.P);
    for (int i = 0; i < pMomenta.size(); ++i) {
      final GaussianAngularMomentum momentum = pMomenta.get(i);
      orbitals.add(
          ContractedGaussianOrbital.of(
              atomIndex,
              momentum,
              GaussianBasisLibrary.normalizedPrimitives(exponents, block.column(2), momentum)));
    }
  }

  private void addCartesianShell(
      final int atomIndex,
      final ShellBlock block,
      final GaussianShellType shellType,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    final double[] exponents = block.column(0);
    final double[] coefficients = block.column(1);
    final List<GaussianAngularMomentum> momenta = GaussianAngularMomentum.cartesianShell(shellType);
    for (int i = 0; i < momenta.size(); ++i) {
      final GaussianAngularMomentum momentum = momenta.get(i);
      final List<GaussianPrimitive> primitives =
          GaussianBasisLibrary.normalizedPrimitives(exponents, coefficients, momentum);
      orbitals.add(ContractedGaussianOrbital.of(atomIndex, momentum, primitives));
    }
  }

  private static String clean(final String line) {
    final int hashCommentStart = line.indexOf('#');
    final int bangCommentStart = line.indexOf('!');
    final int commentStart =
        GaussianBasisTextParser.firstCommentStart(hashCommentStart, bangCommentStart);
    final String cleaned = commentStart >= 0 ? line.substring(0, commentStart) : line;
    return cleaned.trim();
  }

  private static int firstCommentStart(final int first, final int second) {
    if (first < 0) {
      return second;
    }
    if (second < 0) {
      return first;
    }
    return Math.min(first, second);
  }

  private static boolean skipDirective(final String line) {
    final String normalized = line.toUpperCase(Locale.ROOT);
    return normalized.equals("BASIS")
        || normalized.equals("END")
        || normalized.startsWith("BASIS ")
        || normalized.startsWith("END ");
  }

  private static boolean ecpDirective(final String line) {
    final String normalized = line.toUpperCase(Locale.ROOT);
    return normalized.equals("ECP")
        || normalized.equals("$ECP")
        || normalized.startsWith("ECP ")
        || normalized.startsWith("$ECP ")
        || normalized.contains("-ECP")
        || normalized.endsWith(" POTENTIAL")
        || normalized.contains(" PSEUDOPOTENTIAL");
  }

  private static double[] parsePrimitiveRow(
      final String shellType, final String[] parts, final double scale) {
    final int expected = "SP".equals(shellType) ? 3 : 2;
    if (parts.length != expected) {
      throw new IllegalArgumentException(
          "Gaussian basis primitive row for shell "
              + shellType
              + " must contain "
              + expected
              + " numeric values.");
    }
    final double[] row = new double[parts.length];
    row[0] = GaussianBasisTextParser.parseDouble(parts[0]) * scale * scale;
    for (int i = 1; i < parts.length; ++i) {
      row[i] = GaussianBasisTextParser.parseDouble(parts[i]);
    }
    return row;
  }

  private static ShellHeader parseShellHeader(final String[] parts) {
    final String shellType = parts[0].toUpperCase(Locale.ROOT);
    final int primitiveCount;
    try {
      primitiveCount = Integer.parseInt(parts[1]);
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Gaussian basis shell primitive count is invalid.", exception);
    }
    if (primitiveCount <= 0) {
      throw new IllegalArgumentException("Gaussian basis shell primitive count must be positive.");
    }
    final double scale = parts.length >= 3 ? GaussianBasisTextParser.parseDouble(parts[2]) : 1.0;
    if (!Double.isFinite(scale) || scale <= 0.0) {
      throw new IllegalArgumentException(
          "Gaussian basis shell scale factor must be positive finite.");
    }
    return ShellHeader.of(shellType, primitiveCount, scale);
  }

  private static double parseDouble(final String value) {
    try {
      return Double.parseDouble(value.replace('D', 'E').replace('d', 'e'));
    } catch (final NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Gaussian basis numeric value is invalid: " + value, exception);
    }
  }

  private static boolean shellToken(final String value) {
    final String normalized = value.toUpperCase(Locale.ROOT);
    return normalized.equals("SP") || GaussianShellType.shellSymbol(normalized);
  }

  private static boolean atomicSymbolToken(final String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      return PeriodicTable.contains(ElementSymbol.of(value));
    } catch (final IllegalArgumentException exception) {
      return false;
    }
  }

  private static GaussianShellType shellType(final String value) {
    return GaussianShellType.ofSymbol(value);
  }

  private static int atomicNumber(final String symbol) {
    final ElementSymbol elementSymbol;
    try {
      elementSymbol = ElementSymbol.of(symbol);
    } catch (final IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Gaussian basis element symbol is unsupported: " + symbol + ".", exception);
    }
    if (!PeriodicTable.contains(elementSymbol)) {
      throw new IllegalArgumentException(
          "Gaussian basis element symbol is unsupported: " + symbol + ".");
    }
    return PeriodicTable.atomicNumberOf(elementSymbol);
  }

  private static final class ShellHeader {

    private final String shellType;
    private final int primitiveCount;
    private final double scale;

    private ShellHeader(final String shellType, final int primitiveCount, final double scale) {
      this.shellType = shellType;
      this.primitiveCount = primitiveCount;
      this.scale = scale;
    }

    static ShellHeader of(final String shellType, final int primitiveCount, final double scale) {
      return new ShellHeader(shellType, primitiveCount, scale);
    }

    String shellType() {
      return this.shellType;
    }

    int primitiveCount() {
      return this.primitiveCount;
    }

    double scale() {
      return this.scale;
    }
  }

  private static final class ShellReadResult {

    private final ShellBlock block;
    private final int cursor;

    private ShellReadResult(final ShellBlock block, final int cursor) {
      this.block = block;
      this.cursor = cursor;
    }

    static ShellReadResult of(final ShellBlock block, final int cursor) {
      return new ShellReadResult(block, cursor);
    }

    ShellBlock block() {
      return this.block;
    }

    int cursor() {
      return this.cursor;
    }
  }

  private static final class ShellBlock {

    private final String shellType;
    private final double[][] rows;

    private ShellBlock(final String shellType, final double[][] rows) {
      this.shellType = shellType;
      this.rows = rows;
    }

    static ShellBlock of(final String shellType, final List<double[]> primitiveRows) {
      if (primitiveRows == null || primitiveRows.isEmpty()) {
        throw new IllegalArgumentException(
            "Gaussian basis shell block must contain primitive rows.");
      }
      final double[][] rows = new double[primitiveRows.size()][];
      for (int i = 0; i < primitiveRows.size(); ++i) {
        rows[i] = primitiveRows.get(i).clone();
      }
      return new ShellBlock(shellType, rows);
    }

    String shellType() {
      return this.shellType;
    }

    double[] column(final int columnIndex) {
      final double[] values = new double[this.rows.length];
      for (int i = 0; i < this.rows.length; ++i) {
        values[i] = this.rows[i][columnIndex];
      }
      return values;
    }
  }
}
