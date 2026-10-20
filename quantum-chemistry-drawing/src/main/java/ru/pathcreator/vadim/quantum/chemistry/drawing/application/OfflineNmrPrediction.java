/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import java.util.zip.GZIPInputStream;
import java.security.DigestInputStream;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereoIdentity;

/** Offline database predictions, using a bounded, cancellable standard HOSE lookup; never changes the source drawing. */
public final class OfflineNmrPrediction {
  private static final String ROOT = "/META-INF/quantum-nmr/";

  private OfflineNmrPrediction() {}

  public enum Nucleus {
    PROTON("¹H", "H", "proton.csv.gz"),
    CARBON("¹³C", "C", "carbon.csv.gz");
    private final String title, symbol, artifact;

    Nucleus(String title, String symbol, String artifact) {
      this.title = title;
      this.symbol = symbol;
      this.artifact = artifact;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public enum Solvent {
    UNSPECIFIED("Не задан · общая база", "Unreported"),
    CDCL3("CDCl₃", "Chloroform-D1 (CDCl3)"),
    DMSO_D6("DMSO-d₆", "Dimethylsulphoxide-D6 (DMSO-D6, C2D6SO)"),
    CD3OD("CD₃OD", "Methanol-D4 (CD3OD)");
    private final String title, database;

    Solvent(String title, String database) {
      this.title = title;
      this.database = database;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public record Shift(String atomId, String label, double ppm, double minPpm, double maxPpm) {
    public Shift {
      Objects.requireNonNull(atomId);
      Objects.requireNonNull(label);
      if (!Double.isFinite(ppm)
          || !Double.isFinite(minPpm)
          || !Double.isFinite(maxPpm)
          || minPpm > ppm
          || ppm > maxPpm) {
        throw new IllegalArgumentException("Некорректный диапазон прогноза ЯМР.");
      }
    }
  }

  public record Prediction(
      List<Shift> shifts, int requestedAtoms, List<String> warnings, String provenance) {
    public Prediction {
      shifts = List.copyOf(shifts);
      warnings = List.copyOf(warnings);
      Objects.requireNonNull(provenance);
    }
  }

  private record Candidate(int atomIndex, int spheres) {}

  private record Value(double min, double mean, double max, int spheres) {}

  public static Prediction predict(
      MoleculeDrawing drawing, Nucleus nucleus, Solvent solvent, BooleanSupplier cancelled)
      throws Exception {
    Objects.requireNonNull(drawing);
    Objects.requireNonNull(nucleus);
    Objects.requireNonNull(solvent);
    final BooleanSupplier stop = cancelled == null ? () -> false : cancelled;
    check(stop);
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
    final var molecule = drawing.molecule();
    if (molecule.atomCount() == 0 || molecule.atomCount() > 256) {
      throw new IllegalArgumentException("Локальный прогноз ограничен 1–256 явными атомами.");
    }
    if (molecule.atoms().stream().anyMatch(a -> a.hasIsotope())) {
      throw new IllegalArgumentException(
          "Изотопно-меченные структуры пока не поддержаны этой базой прогноза.");
    }
    if (MolecularRelativeStereo.present(molecule)) {
      throw new IllegalArgumentException(
          "Относительная, рацемическая или неизвестная конфигурация пока не поддержана этим локальным прогнозом.");
    }
    if (molecule.spinMultiplicity().value() != 1
        || molecule.atoms().stream().anyMatch(a -> a.radicalState().radical())) {
      throw new IllegalArgumentException(
          "Радикалы и открытая оболочка не поддержаны локальным прогнозом.");
    }
    if (MoleculeSkeletonHydrogens.unsupportedCount(drawing) > 0
        || MoleculeHydrogenEditing.suggest(drawing).stream()
            .anyMatch(s -> s.additional() != null && s.additional() > 0)) {
      throw new IllegalArgumentException(
          "Сначала уточните водороды и валентности структуры; прогноз не добавляет H автоматически.");
    }
    final var graph = CdkStereoIdentity.identityGraph(drawing);
    if (graph.getAtomCount() != molecule.atomCount()) {
      throw new IllegalArgumentException("Не удалось сохранить атомную карту прогноза.");
    }
    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(graph);
    Aromaticity.cdkLegacy().apply(graph);
    final Map<String, List<Candidate>> requestedCodes = new HashMap<>();
    final List<Integer> requested = new ArrayList<>();
    final var generator = new HOSECodeGenerator();
    for (int i = 0; i < graph.getAtomCount(); i++) {
      check(stop);
      final var atom = graph.getAtom(i);
      if (!atom.getSymbol().equals(molecule.atoms().get(i).element().symbol().value())) {
        throw new IllegalArgumentException("Нарушен порядок атомов прогноза.");
      }
      if (!atom.getSymbol().equals(nucleus.symbol)) {
        continue;
      }
      requested.add(i);
      // CDK emits six shells using (, /, /, ), /, /. The database also stores each shell prefix.
      // Keep the actual shell terminator, not the padding CDK adds to a short requested code.
      final String code = generator.getHOSECode(graph, atom, 6, false);
      int shells = 0;
      for (int end = 0; end < code.length() && shells < 6; end++) {
        if ("()/".indexOf(code.charAt(end)) < 0) {
          continue;
        }
        shells++;
        requestedCodes
            .computeIfAbsent(code.substring(0, end + 1), unused -> new ArrayList<>())
            .add(new Candidate(i, shells));
      }
    }
    if (requested.isEmpty()) {
      throw new IllegalArgumentException("В структуре нет ядер " + nucleus + " для прогноза.");
    }
    final String expectedHash = dataHash(nucleus.artifact);
    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
    final Map<Integer, Value> results = new HashMap<>();
    boolean solventSeen = false;
    long characters = 0, lines = 0;
    final InputStream resource =
        OfflineNmrPrediction.class.getResourceAsStream(ROOT + nucleus.artifact);
    if (resource == null) {
      throw new IOException("Офлайн-база ЯМР отсутствует в установке.");
    }
    try (resource;
        var compressed = new DigestInputStream(resource, digest);
        var zipped = new GZIPInputStream(compressed, 65536);
        var reader =
            new BufferedReader(new InputStreamReader(zipped, StandardCharsets.UTF_8), 65536)) {
      String line;
      while ((line = reader.readLine()) != null) {
        characters += line.length();
        if (line.length() > 16384 || characters > 512L * 1024 * 1024 || ++lines > 10_000_000) {
          throw new IOException("Офлайн-база ЯМР превысила допустимый размер.");
        }
        if ((lines & 255) == 0) {
          check(stop);
          if (System.nanoTime() > deadline) {
            throw new IOException("Локальный прогноз превысил 45 секунд.");
          }
        }
        if (line.isEmpty() || line.startsWith("#") || line.equals("///")) {
          continue;
        }
        final int first = line.indexOf('_'), second = line.indexOf('_', first + 1);
        if (first < 0
            || second < 0
            || !line.substring(first + 1, second).equals(solvent.database)) {
          continue;
        }
        solventSeen = true;
        final int third = line.indexOf('_', second + 1);
        if (third < 0) {
          throw new IOException("Повреждена строка базы ЯМР.");
        }
        final var candidates = requestedCodes.get(line.substring(second + 1, third));
        if (candidates == null) {
          continue;
        }
        final String[] values = line.substring(third + 1).split("_", -1);
        if (values.length != 4) {
          throw new IOException("Повреждены числовые данные базы ЯМР.");
        }
        final double min = Double.parseDouble(values[0]),
            mean = Double.parseDouble(values[1]),
            max = Double.parseDouble(values[2]);
        final long count = Long.parseLong(values[3]);
        if (!Double.isFinite(min)
            || !Double.isFinite(mean)
            || !Double.isFinite(max)
            || min > mean
            || mean > max
            || count < 1) {
          throw new IOException("Некорректный диапазон базы ЯМР.");
        }
        for (Candidate candidate : candidates) {
          final Value previous = results.get(candidate.atomIndex);
          if (previous == null || previous.spheres < candidate.spheres) {
            results.put(candidate.atomIndex, new Value(min, mean, max, candidate.spheres));
          } else if (previous.spheres == candidate.spheres
              && (previous.min != min || previous.mean != mean || previous.max != max)) {
            throw new IOException("Противоречивые записи базы ЯМР.");
          }
        }
      }
      // Consume any final compressed bytes so the stored artifact checksum covers the whole
      // resource.
      while (compressed.read() != -1) {
        check(stop);
      }
    }
    check(stop);
    if (!HexFormat.of().formatHex(digest.digest()).equals(expectedHash)) {
      throw new IOException("Не совпала контрольная сумма офлайн-базы ЯМР.");
    }
    if (!solventSeen) {
      throw new IOException("Выбранный растворитель отсутствует в офлайн-базе ЯМР.");
    }
    final List<Shift> shifts = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    int weak = 0, wide = 0;
    for (int index : requested) {
      final String id = molecule.atoms().get(index).id().value();
      final Value value = results.get(index);
      if (value == null) {
        warnings.add(
            nucleus.symbol
                + " · "
                + id
                + ": нет поддержанного окружения в базе выбранного растворителя.");
        continue;
      }
      shifts.add(new Shift(id, nucleus.symbol + " · " + id, value.mean, value.min, value.max));
      if (value.spheres < 3) {
        weak++;
      }
      if (value.max - value.min > (nucleus == Nucleus.PROTON ? 2 : 30)) {
        wide++;
      }
    }
    if (weak > 0) {
      warnings.add(
          "Для " + weak + " атомов совпало менее трёх слоёв окружения: прогноз приблизительный.");
    }
    if (wide > 0) {
      warnings.add(
          "Для "
              + wide
              + " атомов диапазон исходной базы широкий. Min/max — разброс примеров, не доверительный интервал.");
    }
    warnings.add(
        "Расчётная модель, не измерение. J и форма мультиплетов этой базой не рассчитываются.");
    warnings.add(
        "Стандартный HOSE: стереохимическое окружение и различия диастереотопных H отдельно не учитываются.");
    if (nucleus == Nucleus.PROTON) {
      warnings.add(
          "Обменные NH/OH и влияние температуры, концентрации и pH отдельно не моделируются.");
    }
    if (solvent == Solvent.UNSPECIFIED) {
      warnings.add("Использована общая база Unreported; растворитель не установлен.");
    }
    final String provenance =
        "nmrshiftdb2 · CSV из официального predictor JAR 2023-10-03; CDK 2.12 standard HOSE; "
            + nucleus
            + "; "
            + solvent.database
            + "; gzip SHA-256 "
            + expectedHash
            + ". 1–6 сфер, без подмены отсутствующих значений. Contains information from nmrshiftdb2, available under the nmrshiftdb2 Database License: https://nmrshiftdb.nmr.uni-koeln.de/nmrshiftdbhtml/nmrshiftdb2datalicense.txt";
    return new Prediction(shifts, requested.size(), warnings, provenance);
  }

  private static void check(BooleanSupplier stop) {
    if (Thread.currentThread().isInterrupted() || stop.getAsBoolean()) {
      throw new CancellationException("Прогноз ЯМР отменён.");
    }
  }

  private static String dataHash(String name) throws IOException {
    try (var stream = OfflineNmrPrediction.class.getResourceAsStream(ROOT + "data.sha256")) {
      if (stream == null) {
        throw new IOException("Отсутствует манифест офлайн-базы ЯМР.");
      }
      final byte[] bytes = stream.readNBytes(4097);
      if (bytes.length > 4096) {
        throw new IOException("Манифест офлайн-базы ЯМР слишком большой.");
      }
      for (String line : new String(bytes, StandardCharsets.UTF_8).lines().toList()) {
        final String[] pair = line.split("  ", 2);
        if (pair.length == 2 && pair[1].equals(name) && pair[0].matches("[a-f0-9]{64}")) {
          return pair[0];
        }
      }
      throw new IOException("База не найдена в манифесте ЯМР.");
    }
  }
}
