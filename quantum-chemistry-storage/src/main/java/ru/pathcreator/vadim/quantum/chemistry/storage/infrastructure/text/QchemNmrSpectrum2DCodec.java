/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.Nucleus;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;

/** Versioned bounded primitive grid payload; no object deserialization or raw binary duplicate buffer. */
public final class QchemNmrSpectrum2DCodec {
  private static final int MAGIC = 0x514e3231;
  private static final int MAX_BINARY_BYTES =
      8 * (NmrSpectrum2D.MAX_CELLS + 2 * NmrSpectrum2D.MAX_AXIS) + 160000;
  public static final int MAX_ENCODED_LENGTH = ((MAX_BINARY_BYTES + 2) / 3) * 4;

  private QchemNmrSpectrum2DCodec() {}

  public static String encode(final NmrSpectrum2D spectrum) {
    Objects.requireNonNull(spectrum);
    final int estimated =
        ((8 * (spectrum.values().size() + spectrum.width() + spectrum.height()) + 160000 + 2) / 3)
            * 4;
    try {
      final var bytes = new ByteArrayOutputStream(estimated);
      try (final var out = new DataOutputStream(Base64.getEncoder().wrap(bytes))) {
        out.writeInt(MAGIC);
        text(out, spectrum.xNucleus().name(), 40);
        text(out, spectrum.yNucleus().name(), 40);
        out.writeInt(spectrum.width());
        out.writeInt(spectrum.height());
        text(out, spectrum.format(), 400);
        text(out, spectrum.sourceName(), 2000);
        text(out, spectrum.acquisition(), 128000);
        out.writeInt(spectrum.sourceHashes().size());
        for (final var entry : new TreeMap<>(spectrum.sourceHashes()).entrySet()) {
          text(out, entry.getKey(), 800);
          text(out, entry.getValue(), 64);
        }
        for (final double value : spectrum.xPpm()) {
          out.writeDouble(value);
        }
        for (final double value : spectrum.yPpm()) {
          out.writeDouble(value);
        }
        for (int y = 0; y < spectrum.height(); y++) {
          for (int x = 0; x < spectrum.width(); x++) {
            out.writeDouble(spectrum.value(x, y));
          }
        }
      }
      if (bytes.size() > MAX_ENCODED_LENGTH) {
        throw new IllegalArgumentException("Превышен размер сохранённого 2D спектра.");
      }
      return bytes.toString(StandardCharsets.US_ASCII);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static NmrSpectrum2D decode(final String encoded) {
    return decode(encoded, NmrSpectrum2D.MAX_CELLS);
  }

  /** Remaining aggregate cells is checked before any grid allocation. */
  static NmrSpectrum2D decode(final String encoded, final int remainingCells) {
    Objects.requireNonNull(encoded);
    if (encoded.length() > MAX_ENCODED_LENGTH
        || remainingCells < 4
        || remainingCells > NmrSpectrum2D.MAX_CELLS) {
      throw new IllegalArgumentException("Превышен предел 2D спектров опыта.");
    }
    if (encoded.isEmpty() || encoded.length() % 4 != 0) {
      throw new IllegalArgumentException("Повреждена длина Base64 матрицы.");
    }
    final int dataEnd =
        encoded.endsWith("==")
            ? encoded.length() - 2
            : encoded.endsWith("=") ? encoded.length() - 1 : encoded.length();
    for (int i = 0; i < dataEnd; i++) {
      final char c = encoded.charAt(i);
      if (!(c >= 'A' && c <= 'Z'
          || c >= 'a' && c <= 'z'
          || c >= '0' && c <= '9'
          || c == '+'
          || c == '/')) {
        throw new IllegalArgumentException("Повреждён Base64 матрицы 2D ЯМР.");
      }
    }
    try (final var in =
        new DataInputStream(
            new BufferedInputStream(
                Base64.getDecoder().wrap(new AsciiStringInput(encoded)), 65536))) {
      if (in.readInt() != MAGIC) {
        throw new IllegalArgumentException("Неподдержанная версия матрицы 2D ЯМР.");
      }
      final var x = Nucleus.valueOf(text(in, 40));
      final var y = Nucleus.valueOf(text(in, 40));
      final int width = count(in, 2, NmrSpectrum2D.MAX_AXIS),
          height = count(in, 2, NmrSpectrum2D.MAX_AXIS);
      final long cells = (long) width * height;
      if (cells > remainingCells) {
        throw new IllegalArgumentException(
            "Матрица превышает оставшийся предел точек 2D ЯМР опыта.");
      }
      final String format = text(in, 400), source = text(in, 2000), acquisition = text(in, 128000);
      final int size = count(in, 1, 16);
      final Map<String, String> hashes = new LinkedHashMap<>();
      for (int i = 0; i < size; i++) {
        if (hashes.put(text(in, 800), text(in, 64)) != null) {
          throw new IllegalArgumentException("Повтор источника 2D спектра.");
        }
      }
      final double[] xPpm = numbers(in, width),
          yPpm = numbers(in, height),
          values = numbers(in, (int) cells);
      if (in.read() != -1) {
        throw new IllegalArgumentException("Лишние данные после матрицы 2D ЯМР.");
      }
      return NmrSpectrum2D.fromArrays(
          x, y, xPpm, yPpm, values, format, source, hashes, acquisition);
    } catch (final IOException e) {
      throw new IllegalArgumentException("Повреждены данные матрицы 2D ЯМР.", e);
    }
  }

  private static int count(final DataInputStream in, final int min, final int max)
      throws IOException {
    final int value = in.readInt();
    if (value < min || value > max) {
      throw new IllegalArgumentException("Недопустимый размер в записи 2D спектра.");
    }
    return value;
  }

  private static double[] numbers(final DataInputStream in, final int count) throws IOException {
    final double[] result = new double[count];
    for (int i = 0; i < count; i++) {
      result[i] = in.readDouble();
      if (!Double.isFinite(result[i]) || Math.abs(result[i]) > 1e100) {
        throw new IllegalArgumentException("Недопустимое число 2D спектра.");
      }
    }
    return result;
  }

  private static void text(final DataOutputStream out, final String value, final int max)
      throws IOException {
    final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > max) {
      throw new IllegalArgumentException("Слишком длинное поле 2D спектра.");
    }
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String text(final DataInputStream in, final int max) throws IOException {
    final int length = count(in, 0, max);
    final byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) {
      throw new EOFException();
    }
    return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
  }

  private static final class AsciiStringInput extends InputStream {
    private final String value;
    private int offset;

    AsciiStringInput(final String value) {
      this.value = value;
    }

    @Override
    public int read() throws IOException {
      if (offset == value.length()) {
        return -1;
      }
      final char c = value.charAt(offset++);
      if (c > 127) {
        throw new IOException("Base64 содержит не-ASCII символ.");
      }
      return c;
    }

    @Override
    public int read(final byte[] target, final int start, final int length) throws IOException {
      Objects.checkFromIndexSize(start, length, target.length);
      if (length == 0) {
        return 0;
      }
      if (offset == value.length()) {
        return -1;
      }
      final int n = Math.min(length, value.length() - offset);
      for (int i = 0; i < n; i++) {
        target[start + i] = (byte) read();
      }
      return n;
    }
  }
}
