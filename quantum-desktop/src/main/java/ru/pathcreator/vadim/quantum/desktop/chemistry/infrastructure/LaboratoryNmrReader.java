/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrTrace;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis.Nucleus;

/** Read-only adapters. Files are never inferred to be the user's measured result. */
public final class LaboratoryNmrReader {
  public enum Format {
    BRUKER_FID,
    BRUKER_SPECTRUM,
    CSV
  }

  public NmrTrace read(
      final Path path,
      final Format format,
      final Nucleus nucleus,
      final double frequency,
      final String solvent)
      throws IOException {
    return switch (format) {
      case BRUKER_FID -> this.bruker(path, false);
      case BRUKER_SPECTRUM -> this.bruker(path, true);
      case CSV -> this.csv(path, nucleus, frequency, solvent);
    };
  }

  private NmrTrace bruker(final Path path, final boolean processed) throws IOException {
    final Path folder = Files.isDirectory(path) ? path : path.toAbsolutePath().getParent();
    final String param = processed ? "procs" : "acqus";
    final Map<String, String> hashes = new TreeMap<>();
    final byte[] metadata = read(folder.resolve(param), 1024 * 1024);
    hashes.put(param, hash(metadata));
    final var parameters = parameters(metadata);
    if (processed && (Files.exists(folder.resolve("proc2s")) || Files.exists(folder.resolve("2rr")))
        || !processed
            && (Files.exists(folder.resolve("acqu2s")) || Files.exists(folder.resolve("ser")))) {
      throw new IllegalArgumentException(
          "Выберите одномерный эксперимент; многомерные данные не интерпретируются как 1D.");
    }
    if (integer(parameters, processed ? "PPARMOD" : "PARMODE", 0) != 0
        || integer(parameters, "FnTYPE", 0) == 2) {
      throw new IllegalArgumentException("Многомерные и NUS-данные требуют другого обработчика.");
    }
    final int size = integer(parameters, processed ? "SI" : "TD"),
        type = integer(parameters, processed ? "DTYPP" : "DTYPA"),
        order = integer(parameters, processed ? "BYTORDP" : "BYTORDA");
    if (order != 0 && order != 1 || type != 0 && type != 2) {
      throw new IllegalArgumentException(
          "Поддерживаются int32/float64 и явно заданный порядок байтов 0/1.");
    }
    if (!processed && integer(parameters, "AQ_mod") != 1 && integer(parameters, "AQ_mod") != 3) {
      throw new IllegalArgumentException(
          "Для FID поддержаны комплексные режимы QSIM/DQD (AQ_mod 1/3).");
    }
    if (size < 8
        || size > NmrTrace.MAX_POINTS * (processed ? 1 : 2)
        || !processed && size % 2 != 0) {
      throw new IllegalArgumentException("Неподдержанный размер массива ЯМР.");
    }
    final String binary = processed ? "1r" : "fid";
    final byte[] bytes = read(folder.resolve(binary), 8 * 1024 * 1024);
    hashes.put(binary, hash(bytes));
    final List<Double> values = binary(bytes, size, order, type);
    List<Double> real = new ArrayList<>(), imaginary = new ArrayList<>();
    if (processed) {
      if (parameters.getOrDefault("REVERSE", "no").equals("yes")
          || integer(parameters, "AXTYPE", 0) != 0) {
        throw new IllegalArgumentException(
            "Перевёрнутая или нестандартная ось Bruker требует явного экспорта ppm/intensity.");
      }
      if (integer(parameters, "STSR", 0) != 0
          || integer(parameters, "STSI", 0) != 0 && integer(parameters, "STSI", 0) != size) {
        throw new IllegalArgumentException(
            "Обрезанные STSR/STSI-спектры пока не поддержаны; экспортируйте полную шкалу либо CSV.");
      }
      final double factor = Math.scalb(1, integer(parameters, "NC_proc", 0));
      for (final double value : values) {
        real.add(value * factor);
      }
      if (Files.exists(folder.resolve("1i"))) {
        final byte[] b = read(folder.resolve("1i"), 8 * 1024 * 1024);
        hashes.put("1i", hash(b));
        for (final double value : binary(b, size, order, type)) {
          imaginary.add(value * factor);
        }
      }
    } else {
      for (int i = 0; i < values.size(); i += 2) {
        real.add(values.get(i));
        imaginary.add(values.get(i + 1));
      }
    }
    final Map<String, String> acquisition = new LinkedHashMap<>(parameters);
    if (processed && !parameters.containsKey("AXNUC")) {
      final Path acq = folder.resolve("../../acqus").normalize();
      if (!Files.isRegularFile(acq)) {
        throw new IllegalArgumentException(
            "Не найдено ядро AXNUC; нужен соответствующий acqus двумя уровнями выше pdata.");
      }
      final byte[] a = read(acq, 1024 * 1024);
      hashes.put("acqus", hash(a));
      acquisition.putAll(parameters(a));
    }
    final Nucleus nucleus =
        nucleus(
            processed
                ? parameters.getOrDefault("AXNUC", acquisition.get("NUC1"))
                : parameters.get("NUC1"));
    final double obs = number(parameters, processed ? "SF" : "SFO1"),
        sw = number(parameters, processed ? "SW_p" : "SW_h");
    final double first = processed ? number(parameters, "OFFSET") : 0,
        carrier = processed ? first - sw / obs / 2 : number(parameters, "O1") / obs,
        delay = processed ? 0 : delay(parameters);
    final String solvent = acquisition.getOrDefault("SOLVENT", "");
    final var domain = processed ? NmrTrace.Domain.SPECTRUM : NmrTrace.Domain.FID;
    final String information =
        String.join(
            "\n",
            new TreeMap<>(parameters)
                .entrySet().stream()
                    .filter(
                        e ->
                            Set.of(
                                    "TD", "SI", "DTYPA", "DTYPP", "BYTORDA", "BYTORDP", "SFO1",
                                    "SF", "SW_h", "SW_p", "O1", "OFFSET", "NUC1", "AXNUC", "GRPDLY",
                                    "DSPFVS", "DECIM", "DIGMOD", "NC_proc", "AQ_mod", "PULPROG",
                                    "SOLVENT", "PHC0", "PHC1", "LB", "WDW", "FT_mod", "PKNL")
                                .contains(e.getKey()))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .toList());
    return new NmrTrace(
        domain,
        nucleus,
        obs,
        solvent,
        sw,
        carrier,
        first,
        processed ? -sw / obs / size : 0,
        delay,
        processed ? "Bruker 1r/1i" : "Bruker fid/acqus",
        folder.getFileName().toString(),
        hashes,
        information,
        real,
        imaginary,
        NmrTrace.defaults(domain, real.size()));
  }

  private NmrTrace csv(
      final Path path, final Nucleus nucleus, final double frequency, final String solvent)
      throws IOException {
    final byte[] bytes = read(path, 16 * 1024 * 1024);
    final String text =
        StandardCharsets.UTF_8
            .newDecoder()
            .decode(ByteBuffer.wrap(bytes))
            .toString()
            .replace("\uFEFF", "");
    final List<Double> x = new ArrayList<>(), y = new ArrayList<>();
    for (final String line : text.lines().toList()) {
      final String row = line.strip();
      if (row.isEmpty() || row.startsWith("#")) {
        continue;
      }
      if (x.isEmpty()
          && row.toLowerCase(Locale.ROOT)
              .matches("ppm[;,\\t ]+(intensity|intensities|amplitude).*")) {
        continue;
      }
      final String[] parts =
          row.split(
              row.contains(";")
                  ? ";"
                  : row.contains("\t") ? "\t" : row.contains(",") ? "," : "\\s+");
      if (parts.length != 2) {
        throw new IllegalArgumentException(
            "CSV: нужны две колонки ppm и intensity без дополнительных полей.");
      }
      try {
        x.add(Double.parseDouble(parts[0].strip().replace(',', '.')));
        y.add(Double.parseDouble(parts[1].strip().replace(',', '.')));
      } catch (final NumberFormatException e) {
        throw new IllegalArgumentException("CSV: неверное число в строке " + (x.size() + 1));
      }
      if (x.size() > NmrTrace.MAX_POINTS) {
        throw new IllegalArgumentException("Слишком много точек CSV.");
      }
    }
    if (x.size() < 8) {
      throw new IllegalArgumentException("CSV содержит меньше 8 точек.");
    }
    if (x.getFirst() < x.getLast()) {
      Collections.reverse(x);
      Collections.reverse(y);
    }
    final double step = (x.getLast() - x.getFirst()) / (x.size() - 1);
    if (!Double.isFinite(step) || step >= 0) {
      throw new IllegalArgumentException("CSV требует равномерную монотонную шкалу ppm.");
    }
    for (int i = 0; i < x.size(); i++) {
      if (!Double.isFinite(x.get(i))
          || Math.abs(x.get(i) - x.getFirst() - i * step) > Math.max(1e-8, Math.abs(step) * .01)) {
        throw new IllegalArgumentException(
            "CSV: шаг ppm неравномерен; данные не интерполированы автоматически.");
      }
    }
    return new NmrTrace(
        NmrTrace.Domain.SPECTRUM,
        nucleus,
        frequency,
        solvent,
        -step * x.size() * frequency,
        (x.getFirst() + x.getLast()) / 2,
        x.getFirst(),
        step,
        0,
        "CSV ppm,intensity",
        path.getFileName().toString(),
        Map.of(path.getFileName().toString(), hash(bytes)),
        "Imported real spectrum; axis supplied in ppm",
        y,
        List.of(),
        NmrTrace.defaults(NmrTrace.Domain.SPECTRUM, y.size()));
  }

  private static List<Double> binary(
      final byte[] bytes, final int count, final int order, final int type) {
    final int width = type == 0 ? 4 : 8, expected = count * width;
    if (bytes.length < expected || bytes.length > ((expected + 1023) / 1024) * 1024) {
      throw new IllegalArgumentException(
          "Размер бинарного файла не соответствует TD/SI; многомерные или обрезанные данные не принимаются.");
    }
    final var b =
        ByteBuffer.wrap(bytes).order(order == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    final List<Double> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(type == 0 ? (double) b.getInt() : b.getDouble());
    }
    return result;
  }

  private static Map<String, String> parameters(final byte[] bytes) {
    final String text = new String(bytes, StandardCharsets.ISO_8859_1);
    final Map<String, String> result = new LinkedHashMap<>();
    for (final String line : text.lines().toList()) {
      if (!line.startsWith("##$")) {
        continue;
      }
      final int equal = line.indexOf('=');
      if (equal < 4) {
        throw new IllegalArgumentException("Повреждён файл параметров Bruker.");
      }
      final String key = line.substring(3, equal).strip(),
          raw = line.substring(equal + 1).split("\\$\\$", 2)[0].strip();
      if (raw.startsWith("(")) {
        continue;
      }
      final String value =
          raw.startsWith("<") && raw.endsWith(">") ? raw.substring(1, raw.length() - 1) : raw;
      if (result.put(key, value) != null) {
        throw new IllegalArgumentException("Повтор параметра Bruker: " + key);
      }
    }
    return result;
  }

  private static Nucleus nucleus(final String value) {
    if (value == null) {
      throw new IllegalArgumentException("Ядро спектра не задано.");
    }
    return switch (value.replace(" ", "").toUpperCase(Locale.ROOT)) {
      case "1H", "H1" -> Nucleus.H1;
      case "13C", "C13" -> Nucleus.C13;
      case "19F", "F19" -> Nucleus.F19;
      case "31P", "P31" -> Nucleus.P31;
      case "15N", "N15" -> Nucleus.N15;
      default -> throw new IllegalArgumentException("Пока не поддержано ядро: " + value);
    };
  }

  private static double number(final Map<String, String> p, final String key) {
    try {
      final double v = Double.parseDouble(Objects.requireNonNull(p.get(key)));
      if (!Double.isFinite(v)) {
        throw new NumberFormatException();
      }
      return v;
    } catch (final RuntimeException e) {
      throw new IllegalArgumentException("Нет корректного параметра Bruker " + key);
    }
  }

  private static int integer(final Map<String, String> p, final String key) {
    final double v = number(p, key);
    if (v != Math.rint(v) || v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Параметр " + key + " должен быть целым.");
    }
    return (int) v;
  }

  private static int integer(final Map<String, String> p, final String key, final int fallback) {
    return p.containsKey(key) ? integer(p, key) : fallback;
  }

  private static double delay(final Map<String, String> p) {
    if (integer(p, "DIGMOD", 1) == 0) {
      return 0;
    }
    if (p.containsKey("GRPDLY") && number(p, "GRPDLY") > 0) {
      return number(p, "GRPDLY");
    }
    final int firmware = integer(p, "DSPFVS");
    if (firmware >= 14) {
      return 0;
    }
    // Legacy DSP values from nmrglue; BSD-3-Clause license is bundled in resources/licenses.
    return switch (firmware + ":" + integer(p, "DECIM")) {
      case "10:2" -> 44.75;
      case "10:3" -> 33.5;
      case "10:4" -> 66.625;
      case "10:6" -> 59.083333333333336;
      case "10:8" -> 68.5625;
      case "10:12" -> 60.375;
      case "10:16" -> 69.53125;
      case "10:24" -> 61.020833333333336;
      case "10:32" -> 70.015625;
      case "10:48" -> 61.34375;
      case "10:64" -> 70.2578125;
      case "10:96" -> 61.505208333333336;
      case "10:128" -> 70.37890625;
      case "10:192" -> 61.5859375;
      case "10:256" -> 70.439453125;
      case "10:384" -> 61.626302083333336;
      case "10:512" -> 70.4697265625;
      case "10:768" -> 61.646484375;
      case "10:1024" -> 70.48486328125;
      case "10:1536" -> 61.656575520833336;
      case "10:2048" -> 70.492431640625;
      case "11:2" -> 46.0;
      case "11:3" -> 36.5;
      case "11:4" -> 48.0;
      case "11:6" -> 50.166666666666664;
      case "11:8" -> 53.25;
      case "11:12" -> 69.5;
      case "11:16" -> 72.25;
      case "11:24" -> 70.16666666666667;
      case "11:32" -> 72.75;
      case "11:48" -> 70.5;
      case "11:64" -> 73.0;
      case "11:96" -> 70.66666666666667;
      case "11:128" -> 72.5;
      case "11:192" -> 71.33333333333333;
      case "11:256" -> 72.25;
      case "11:384" -> 71.66666666666667;
      case "11:512" -> 72.125;
      case "11:768" -> 71.83333333333333;
      case "11:1024" -> 72.0625;
      case "11:1536" -> 71.91666666666667;
      case "11:2048" -> 72.03125;
      case "12:2" -> 46.0;
      case "12:3" -> 36.5;
      case "12:4" -> 48.0;
      case "12:6" -> 50.166666666666664;
      case "12:8" -> 53.25;
      case "12:12" -> 69.5;
      case "12:16" -> 71.625;
      case "12:24" -> 70.16666666666667;
      case "12:32" -> 72.125;
      case "12:48" -> 70.5;
      case "12:64" -> 72.375;
      case "12:96" -> 70.66666666666667;
      case "12:128" -> 72.5;
      case "12:192" -> 71.33333333333333;
      case "12:256" -> 72.25;
      case "12:384" -> 71.66666666666667;
      case "12:512" -> 72.125;
      case "12:768" -> 71.83333333333333;
      case "12:1024" -> 72.0625;
      case "12:1536" -> 71.91666666666667;
      case "12:2048" -> 72.03125;
      case "13:2" -> 2.75;
      case "13:3" -> 2.8333333333333335;
      case "13:4" -> 2.875;
      case "13:6" -> 2.9166666666666665;
      case "13:8" -> 2.9375;
      case "13:12" -> 2.9583333333333335;
      case "13:16" -> 2.96875;
      case "13:24" -> 2.9791666666666665;
      case "13:32" -> 2.984375;
      case "13:48" -> 2.9895833333333335;
      case "13:64" -> 2.9921875;
      case "13:96" -> 2.9947916666666665;
      default ->
          throw new IllegalArgumentException(
              "Неизвестная цифровая задержка DSPFVS/DECIM; обработка не выполнена.");
    };
  }

  private static byte[] read(final Path path, final int limit) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) > limit) {
      throw new IOException("Файл отсутствует или превышает предел: " + path.getFileName());
    }
    try (var in = Files.newInputStream(path)) {
      final byte[] bytes = in.readNBytes(limit + 1);
      if (bytes.length > limit) {
        throw new IOException("Файл увеличился сверх предела.");
      }
      return bytes;
    }
  }

  private static String hash(final byte[] bytes) {
    try {
      return HexFormat.of()
          .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (final java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
