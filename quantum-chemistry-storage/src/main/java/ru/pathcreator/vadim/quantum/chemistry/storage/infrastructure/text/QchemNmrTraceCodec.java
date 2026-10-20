/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.Nucleus;

/** A bounded binary payload for dense numeric arrays; no Java object deserialization. */
public final class QchemNmrTraceCodec {
  private QchemNmrTraceCodec() {}

  public static String encode(final NmrTrace t) {
    try {
      final var bytes = new ByteArrayOutputStream();
      try (var out = new DataOutputStream(bytes)) {
        out.writeInt(0x514e5431);
        text(out, t.domain().name());
        text(out, t.nucleus().name());
        out.writeDouble(t.frequencyMHz());
        text(out, t.solvent());
        out.writeDouble(t.spectralWidthHz());
        out.writeDouble(t.carrierPpm());
        out.writeDouble(t.firstPpm());
        out.writeDouble(t.stepPpm());
        out.writeDouble(t.groupDelay());
        text(out, t.format());
        text(out, t.sourceName());
        text(out, t.acquisition());
        out.writeInt(t.sourceHashes().size());
        for (final var e : new TreeMap<>(t.sourceHashes()).entrySet()) {
          text(out, e.getKey());
          text(out, e.getValue());
        }
        numbers(out, t.real());
        numbers(out, t.imaginary());
        final var p = t.processing();
        out.writeInt(p.version());
        out.writeInt(p.zeroFill());
        out.writeDouble(p.lineBroadeningHz());
        out.writeDouble(p.phase0());
        out.writeDouble(p.phase1());
        out.writeDouble(p.pivot());
        out.writeDouble(p.referencePpm());
        out.writeBoolean(p.baselineEdges());
        out.writeBoolean(p.removeDelay());
        out.writeDouble(p.firstPoint());
      }
      return Base64.getEncoder().encodeToString(bytes.toByteArray());
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static NmrTrace decode(final String encoded) {
    if (encoded.length() > 6_000_000) {
      throw new IllegalArgumentException("Данные спектра превышают предел.");
    }
    try (var in =
        new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
      if (in.readInt() != 0x514e5431) {
        throw new IllegalArgumentException("Неподдержанная версия массива ЯМР.");
      }
      final var domain = NmrTrace.Domain.valueOf(text(in));
      final var nucleus = Nucleus.valueOf(text(in));
      final double frequency = in.readDouble();
      final String solvent = text(in);
      final double sw = in.readDouble(),
          carrier = in.readDouble(),
          first = in.readDouble(),
          step = in.readDouble(),
          delay = in.readDouble();
      final String format = text(in), source = text(in), acquisition = text(in);
      final int count = in.readInt();
      if (count < 1 || count > 16) {
        throw new IllegalArgumentException("Неверное число источников спектра.");
      }
      final Map<String, String> hashes = new HashMap<>();
      for (int i = 0; i < count; i++) {
        if (hashes.put(text(in), text(in)) != null) {
          throw new IllegalArgumentException("Повтор источника спектра.");
        }
      }
      final List<Double> real = numbers(in), imaginary = numbers(in);
      final var processing =
          new NmrTrace.Processing(
              in.readInt(),
              in.readInt(),
              in.readDouble(),
              in.readDouble(),
              in.readDouble(),
              in.readDouble(),
              in.readDouble(),
              in.readBoolean(),
              in.readBoolean(),
              in.readDouble());
      if (in.read() != -1) {
        throw new IllegalArgumentException("Неизвестные данные после массива спектра.");
      }
      return new NmrTrace(
          domain,
          nucleus,
          frequency,
          solvent,
          sw,
          carrier,
          first,
          step,
          delay,
          format,
          source,
          hashes,
          acquisition,
          real,
          imaginary,
          processing);
    } catch (final IOException e) {
      throw new IllegalArgumentException("Повреждены данные спектра.", e);
    }
  }

  private static void text(final DataOutputStream out, final String value) throws IOException {
    final byte[] b = value.getBytes(StandardCharsets.UTF_8);
    out.writeInt(b.length);
    out.write(b);
  }

  private static String text(final DataInputStream in) throws IOException {
    final int length = in.readInt();
    if (length < 0 || length > 128000) {
      throw new IllegalArgumentException("Недопустимая длина поля спектра.");
    }
    final byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) {
      throw new EOFException();
    }
    return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
  }

  private static void numbers(final DataOutputStream out, final List<Double> values)
      throws IOException {
    out.writeInt(values.size());
    for (final double v : values) {
      out.writeDouble(v);
    }
  }

  private static List<Double> numbers(final DataInputStream in) throws IOException {
    final int count = in.readInt();
    if (count < 0 || count > NmrTrace.MAX_POINTS) {
      throw new IllegalArgumentException("Недопустимое число точек спектра.");
    }
    final List<Double> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(in.readDouble());
    }
    return result;
  }
}
