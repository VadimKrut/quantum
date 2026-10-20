/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.*;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.Nucleus;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;

/** Imports an explicitly processed rectangular intensity matrix, never a raw FID. */
public final class LaboratoryNmr2DReader {
  public static final long MAX_FILE_BYTES = 128L * 1024 * 1024;
  private static final int MAX_LINE = 1_100_000;

  private LaboratoryNmr2DReader() {}

  /** First row: empty/label cell followed by X ppm. Following rows: Y ppm, then all intensities. */
  public static NmrSpectrum2D read(
      final Path file,
      final Nucleus xNucleus,
      final Nucleus yNucleus,
      final String acquisition,
      final BooleanSupplier cancelled)
      throws IOException {
    Objects.requireNonNull(file);
    Objects.requireNonNull(xNucleus, "Укажите ядро оси X.");
    Objects.requireNonNull(yNucleus, "Укажите ядро оси Y.");
    Objects.requireNonNull(cancelled);
    check(cancelled);
    final long size = Files.size(file);
    if (size > MAX_FILE_BYTES) {
      throw new IOException("Матрица 2D больше 128 МиБ.");
    }
    if (size == 0) {
      throw new IOException("Файл матрицы пуст.");
    }
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
    final List<Double> xs = new ArrayList<>(), ys = new ArrayList<>();
    final Values values = new Values();
    char separator;
    try (final var input = new BoundedInput(Files.newInputStream(file), cancelled);
        final var hashed = new DigestInputStream(input, digest);
        final var reader =
            new BufferedReader(
                new InputStreamReader(
                    hashed,
                    StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)),
                32_768)) {
      String line = line(reader, cancelled);
      if (line == null) {
        throw new IOException("Файл матрицы пуст.");
      }
      if (line.startsWith("\ufeff")) {
        line = line.substring(1);
      }
      separator = line.indexOf('\t') >= 0 ? '\t' : ',';
      final var header = cells(line, separator, 1);
      if (header.size() < 3 || header.size() > NmrSpectrum2D.MAX_AXIS + 1) {
        throw new IOException(
            "Первая строка: подпись или пустая ячейка, затем от 2 до "
                + NmrSpectrum2D.MAX_AXIS
                + " значений X в ppm.");
      }
      if (header.getFirst().length() > 256) {
        throw new IOException("Слишком длинная подпись первой ячейки.");
      }
      try {
        Double.parseDouble(header.getFirst());
        throw new IOException(
            "Первая ячейка должна быть пустой или содержать текстовую подпись, например ppm или Y/X. Число в ней не позволяет отличить оси от матрицы без заголовка.");
      } catch (NumberFormatException label) {
        /* An empty/textual corner explicitly separates both axes. */
      }
      for (int i = 1; i < header.size(); i++) {
        xs.add(number(header.get(i), 1, i + 1, true));
      }
      axis(xs, "X");
      int row = 1;
      boolean trailingBlank = false;
      while ((line = line(reader, cancelled)) != null) {
        row++;
        if (line.isBlank()) {
          trailingBlank = true;
          continue;
        }
        if (trailingBlank) {
          throw new IOException("Пустая строка внутри матрицы перед строкой " + row + ".");
        }
        if (ys.size() >= NmrSpectrum2D.MAX_AXIS
            || (long) (ys.size() + 1) * xs.size() > NmrSpectrum2D.MAX_CELLS) {
          throw new IOException(
              "Матрица превышает допустимый размер " + NmrSpectrum2D.MAX_CELLS + " точек.");
        }
        final var cells = cells(line, separator, row);
        if (cells.size() != header.size()) {
          throw new IOException(
              "Строка "
                  + row
                  + ": требуется "
                  + header.size()
                  + " ячеек, получено "
                  + cells.size()
                  + ". Матрица должна быть полной.");
        }
        ys.add(number(cells.getFirst(), row, 1, true));
        for (int column = 1; column < cells.size(); column++) {
          values.add(number(cells.get(column), row, column + 1, false));
        }
      }
    }
    check(cancelled);
    axis(ys, "Y");
    final String name = file.getFileName().toString();
    return NmrSpectrum2D.fromArrays(
        xNucleus,
        yNucleus,
        xs.stream().mapToDouble(Double::doubleValue).toArray(),
        ys.stream().mapToDouble(Double::doubleValue).toArray(),
        values.array(),
        separator == '\t' ? "Processed TSV matrix" : "Processed CSV matrix",
        name,
        Map.of("matrix", HexFormat.of().formatHex(digest.digest())),
        acquisition == null ? "" : acquisition);
  }

  private static double number(
      final String raw, final int row, final int column, final boolean axis) throws IOException {
    if (raw.isBlank()
        || raw.length() > 64
        || !raw.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) {
      throw new IOException(
          "Строка "
              + row
              + ", столбец "
              + column
              + ": требуется число с десятичной точкой, без пропусков.");
    }
    final double value;
    try {
      value = Double.parseDouble(raw);
    } catch (NumberFormatException bad) {
      throw new IOException("Некорректное число: строка " + row + ", столбец " + column + ".", bad);
    }
    if (!Double.isFinite(value)
        || Math.abs(value) > (axis ? 1e6 : 1e100)
        || value == 0 && raw.split("[eE]", 2)[0].matches(".*[1-9].*")) {
      throw new IOException(
          "Строка " + row + ", столбец " + column + ": число вне допустимого диапазона.");
    }
    return value;
  }

  private static void axis(final List<Double> values, final String name) throws IOException {
    if (values.size() < 2) {
      throw new IOException("Ось " + name + " должна содержать минимум 2 точки.");
    }
    final boolean ascending = values.get(1) > values.getFirst();
    for (int i = 1; i < values.size(); i++) {
      if (values.get(i).doubleValue() == values.get(i - 1).doubleValue()
          || (values.get(i) > values.get(i - 1)) != ascending) {
        throw new IOException(
            "Ось "
                + name
                + ": значения ppm должны строго возрастать или убывать без повторений (точка "
                + (i + 1)
                + ").");
      }
    }
  }

  /** One physical line only; quoted numeric cells and escaped quotes are supported, not multiline headers. */
  private static List<String> cells(final String line, final char separator, final int row)
      throws IOException {
    final List<String> result = new ArrayList<>();
    final StringBuilder cell = new StringBuilder();
    boolean quoted = false, closed = false;
    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);
      if (quoted) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            cell.append('"');
            i++;
          } else {
            quoted = false;
            closed = true;
          }
        } else {
          cell.append(c);
        }
      } else if (c == separator) {
        result.add(cell.toString().trim());
        cell.setLength(0);
        closed = false;
        if (result.size() > NmrSpectrum2D.MAX_AXIS) {
          throw new IOException("Слишком много столбцов в строке " + row + ".");
        }
      } else if (c == '"' && cell.toString().isBlank() && !closed) {
        cell.setLength(0);
        quoted = true;
      } else if (closed && !Character.isWhitespace(c) || c == '"') {
        throw new IOException("Неверные кавычки в строке " + row + ".");
      } else {
        cell.append(c);
      }
    }
    if (quoted) {
      throw new IOException(
          "Незакрытые кавычки в строке " + row + ". Многострочные ячейки не поддерживаются.");
    }
    result.add(cell.toString().trim());
    return result;
  }

  private static String line(final Reader reader, final BooleanSupplier cancelled)
      throws IOException {
    final StringBuilder result = new StringBuilder();
    int c;
    while ((c = reader.read()) >= 0) {
      if ((result.length() & 4095) == 0) {
        check(cancelled);
      }
      if (c == '\n') {
        break;
      }
      result.append((char) c);
      if (result.length() > MAX_LINE) {
        throw new IOException("Слишком длинная строка матрицы.");
      }
    }
    if (c < 0 && result.isEmpty()) {
      return null;
    }
    if (!result.isEmpty() && result.charAt(result.length() - 1) == '\r') {
      result.setLength(result.length() - 1);
    }
    return result.toString();
  }

  private static void check(final BooleanSupplier cancelled) {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new CancellationException("Импорт 2D отменён.");
    }
  }

  private static final class Values {
    private double[] data = new double[4096];
    private int size;

    void add(final double value) {
      if (this.size == this.data.length) {
        this.data = Arrays.copyOf(this.data, Math.min(NmrSpectrum2D.MAX_CELLS, this.size * 2));
      }
      this.data[this.size++] = value;
    }

    double[] array() {
      return Arrays.copyOf(this.data, this.size);
    }
  }

  private static final class BoundedInput extends FilterInputStream {
    private long bytes;
    private final BooleanSupplier cancelled;

    BoundedInput(final InputStream input, final BooleanSupplier cancelled) {
      super(input);
      this.cancelled = cancelled;
    }

    private void count(final int n) throws IOException {
      check(this.cancelled);
      if (n > 0 && (this.bytes += n) > MAX_FILE_BYTES) {
        throw new IOException("Матрица 2D больше 128 МиБ.");
      }
    }

    @Override
    public int read() throws IOException {
      final int value = this.in.read();
      this.count(value < 0 ? 0 : 1);
      return value;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
      final int n = this.in.read(buffer, offset, length);
      this.count(n);
      return n;
    }
  }
}
