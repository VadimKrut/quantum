/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.openscience.cdk.interfaces.IAtomContainer;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.gen.RigidFragmentCache;

/** Disposable bounded process. No third-party code or logging can keep a cancelled search alive. */
public final class OclConformerWorker {
  private static final int MAGIC = 0x514F434C,
      VERSION = 1,
      MAX_BYTES = 1048576,
      MAX_MOL_BYTES = 200000,
      MAX_TOTAL_ATOMS = 384;

  record Coordinates(double energy, double[][] coordinates) {}

  record Response(List<Coordinates> candidates, List<String> diagnostics, int attempted) {}

  private record AtomSpec(int number, int charge, int isotope) {}

  private record BondSpec(int first, int second, int order, boolean aromatic) {}

  private record Input(
      String molfile,
      List<AtomSpec> atoms,
      List<BondSpec> bonds,
      int requested,
      long seed,
      long timeout) {}

  private OclConformerWorker() {}

  static Response generate(
      final IAtomContainer graph,
      final String molfile,
      final int requested,
      final long seed,
      final long timeout,
      final BooleanSupplier cancellation) {
    Path directory = null;
    Process process = null;
    try {
      cancel(cancellation);
      directory = Files.createTempDirectory("quantum-ocl-conformers-");
      final Path input = directory.resolve("input.bin"), output = directory.resolve("output.bin");
      try (final var stream = new DataOutputStream(Files.newOutputStream(input))) {
        header(stream);
        stream.writeInt(requested);
        stream.writeLong(seed);
        stream.writeLong(timeout);
        writeText(stream, molfile, MAX_MOL_BYTES);
        stream.writeInt(graph.getAtomCount());
        for (final var atom : graph.atoms()) {
          stream.writeInt(atom.getAtomicNumber());
          stream.writeInt(atom.getFormalCharge());
          stream.writeInt(atom.getMassNumber() == null ? 0 : atom.getMassNumber());
        }
        stream.writeInt(graph.getBondCount());
        for (final var bond : graph.bonds()) {
          stream.writeInt(graph.indexOf(bond.getBegin()));
          stream.writeInt(graph.indexOf(bond.getEnd()));
          stream.writeInt(bond.getOrder().numeric());
          stream.writeBoolean(bond.isAromatic());
        }
      }
      if (Files.size(input) > MAX_BYTES) {
        throw new IllegalArgumentException("Превышен размер входного протокола.");
      }
      final boolean windows =
          System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
      final Path executable =
          Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java");
      if (!Files.isRegularFile(executable)) {
        throw new IllegalArgumentException(
            "В runtime отсутствует Java worker для поиска конформеров.");
      }
      String classpath = System.getProperty("java.class.path", "");
      if (classpath.isBlank()) {
        classpath =
            Path.of(
                    OclConformerWorker.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI())
                .toString();
      }
      process =
          new ProcessBuilder(
                  executable.toString(),
                  "-Xmx512m",
                  "-Dfile.encoding=UTF-8",
                  "-Djava.awt.headless=true",
                  "-cp",
                  classpath,
                  OclConformerWorker.class.getName(),
                  input.toString(),
                  output.toString())
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
      while (true) {
        cancel(cancellation);
        final long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          throw new IllegalArgumentException(
              "Истекло время поиска конформеров; вычислительный процесс остановлен. Сократите число кандидатов или выберите меньшую структуру.");
        }
        if (process.waitFor(
            Math.min(100, Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining))),
            TimeUnit.MILLISECONDS)) {
          break;
        }
      }
      cancel(cancellation);
      if (!Files.isRegularFile(output)
          || Files.size(output) < 9
          || Files.size(output) > MAX_BYTES) {
        throw new IllegalArgumentException(
            "Worker не вернул допустимый результат (код " + process.exitValue() + ").");
      }
      try (final var stream = new DataInputStream(Files.newInputStream(output))) {
        checkHeader(stream);
        if (!stream.readBoolean()) {
          throw new IllegalArgumentException(text(stream, 4000));
        }
        if (process.exitValue() != 0) {
          throw new IllegalArgumentException("Worker завершился с ошибкой.");
        }
        final int attempted = count(stream, 0, 256), number = count(stream, 0, requested);
        final List<Coordinates> candidates = new ArrayList<>();
        for (int i = 0; i < number; i++) {
          final double energy = stream.readDouble();
          if (!Double.isFinite(energy) || Math.abs(energy) > 1e9) {
            throw new IllegalArgumentException("Недопустимая энергия worker.");
          }
          if (count(stream, 2, 128) != graph.getAtomCount()) {
            throw new IllegalArgumentException("Изменилось число исходных атомов.");
          }
          final double[][] coordinates = new double[graph.getAtomCount()][3];
          final Set<Integer> mapped = new HashSet<>();
          for (int j = 0; j < coordinates.length; j++) {
            final int map = count(stream, 1, coordinates.length) - 1;
            if (!mapped.add(map)) {
              throw new IllegalArgumentException("Повтор атомной карты worker.");
            }
            for (int k = 0; k < 3; k++) {
              coordinates[map][k] = coordinate(stream.readDouble());
            }
          }
          candidates.add(new Coordinates(energy, coordinates));
        }
        final int notes = count(stream, 0, 24);
        final List<String> diagnostics = new ArrayList<>();
        for (int i = 0; i < notes; i++) {
          diagnostics.add(text(stream, 4000));
        }
        if (stream.read() != -1) {
          throw new IllegalArgumentException("Лишние данные в ответе worker.");
        }
        return new Response(List.copyOf(candidates), List.copyOf(diagnostics), attempted);
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CancellationException("Поиск конформеров отменён.");
    } catch (final CancellationException | IllegalArgumentException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          "Не удалось выполнить изолированный поиск: " + e.getMessage(), e);
    } finally {
      if (process != null) {
        terminate(process);
      }
      if (directory != null) {
        for (final String name : List.of("input.bin", "output.bin")) {
          try {
            Files.deleteIfExists(directory.resolve(name));
          } catch (final IOException ignored) {
          }
        }
        try {
          Files.deleteIfExists(directory);
        } catch (final IOException ignored) {
        }
      }
    }
  }

  private static void cancel(final BooleanSupplier cancellation) {
    if (Thread.currentThread().isInterrupted() || cancellation.getAsBoolean()) {
      throw new CancellationException("Поиск конформеров отменён.");
    }
  }

  private static void terminate(final Process process) {
    if (!process.isAlive()) {
      return;
    }
    final boolean interrupted = Thread.interrupted();
    try {
      process.descendants().forEach(ProcessHandle::destroyForcibly);
      process.destroyForcibly();
      final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (process.isAlive()) {
        try {
          if (process.waitFor(
              Math.max(1, TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime())),
              TimeUnit.MILLISECONDS)) {
            break;
          }
        } catch (final InterruptedException ignored) {
        }
        if (System.nanoTime() >= end && process.isAlive()) {
          throw new IllegalStateException(
              "ОС не подтвердила остановку процесса конформеров " + process.pid() + ".");
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static void main(final String[] args) {
    if (args.length != 2) {
      System.exit(2);
      return;
    }
    final Path input = Path.of(args[0]), output = Path.of(args[1]);
    try {
      if (!Files.isRegularFile(input) || Files.size(input) > MAX_BYTES) {
        throw new IllegalArgumentException("Недопустимый размер входного файла.");
      }
      final Input request;
      try (final var stream = new DataInputStream(Files.newInputStream(input))) {
        request = readInput(stream);
        if (stream.read() != -1) {
          throw new IllegalArgumentException("Лишние входные данные.");
        }
      }
      final var response = calculate(request);
      try (final var stream = new DataOutputStream(Files.newOutputStream(output))) {
        header(stream);
        stream.writeBoolean(true);
        stream.writeInt(response.attempted());
        stream.writeInt(response.candidates().size());
        for (final var candidate : response.candidates()) {
          stream.writeDouble(candidate.energy());
          stream.writeInt(candidate.coordinates().length);
          for (int i = 0; i < candidate.coordinates().length; i++) {
            stream.writeInt(i + 1);
            for (final double v : candidate.coordinates()[i]) {
              stream.writeDouble(v);
            }
          }
        }
        stream.writeInt(response.diagnostics().size());
        for (final String note : response.diagnostics()) {
          writeText(stream, note, 4000);
        }
      }
    } catch (final Throwable e) {
      try (final var stream = new DataOutputStream(Files.newOutputStream(output))) {
        header(stream);
        stream.writeBoolean(false);
        final String message = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
        writeText(stream, message.substring(0, Math.min(900, message.length())), 4000);
      } catch (final Exception ignored) {
      }
      System.exit(1);
    }
  }

  private static Input readInput(final DataInputStream stream) throws IOException {
    checkHeader(stream);
    final int requested = count(stream, 1, 64);
    final long seed = stream.readLong(), timeout = stream.readLong();
    if (timeout < 1 || timeout > 120000) {
      throw new IllegalArgumentException("Недопустимый срок вычисления.");
    }
    final String molfile = text(stream, MAX_MOL_BYTES);
    final int atoms = count(stream, 2, 128);
    final List<AtomSpec> atomSpecs = new ArrayList<>();
    for (int i = 0; i < atoms; i++) {
      atomSpecs.add(
          new AtomSpec(count(stream, 1, 118), count(stream, -8, 8), count(stream, 0, 400)));
    }
    final int bonds = count(stream, 1, 512);
    final List<BondSpec> bondSpecs = new ArrayList<>();
    final Set<String> edges = new HashSet<>();
    for (int i = 0; i < bonds; i++) {
      final int a = count(stream, 0, atoms - 1),
          b = count(stream, 0, atoms - 1),
          order = count(stream, 1, 3);
      if (a == b || !edges.add(edge(a, b))) {
        throw new IllegalArgumentException("Недопустимая связь.");
      }
      bondSpecs.add(new BondSpec(a, b, order, stream.readBoolean()));
    }
    return new Input(molfile, atomSpecs, bondSpecs, requested, seed, timeout);
  }

  private static Response calculate(final Input input) throws Exception {
    final long deadline =
        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(1, input.timeout() - 1000));
    final StereoMolecule molecule = new StereoMolecule();
    molecule.setHydrogenProtection(true);
    if (!new MolfileParser().parse(molecule, input.molfile())) {
      throw new IllegalArgumentException("OpenChemLib не прочитал проверенный граф.");
    }
    requireMapping(input, molecule, false);
    molecule.ensureHelperArrays(com.actelion.research.chem.Molecule.cHelperCIP);
    final int[] fragment = new int[molecule.getAllAtoms()];
    if (molecule.getFragmentNumbers(fragment, false, true) != 1) {
      throw new IllegalArgumentException("Требуется одна связная структура.");
    }
    ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
    RigidFragmentCache.getDefaultInstance().loadDefaultCache();
    final ConformerGenerator generator = new ConformerGenerator(input.seed(), false);
    generator.setTimeOut(Math.max(1, input.timeout() - 1000));
    generator.setThreadMaster(() -> System.nanoTime() >= deadline);
    if (!generator.initializeConformers(
        molecule,
        ConformerGenerator.STRATEGY_LIKELY_RANDOM,
        Math.min(8192, 1000 * (int) Math.ceil(Math.sqrt(input.requested()))),
        false)) {
      throw new IllegalArgumentException(
          "OpenChemLib не смог подготовить структуру для поиска конформеров; состав и протонирование не изменены.");
    }
    requireMapping(input, molecule, true);
    final List<Coordinates> candidates = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    int attempted = 0, notConverged = 0, duplicate = 0, invalid = 0;
    final int attempts = Math.min(256, input.requested() * 4);
    while (attempted < attempts
        && candidates.size() < input.requested()
        && System.nanoTime() < deadline) {
      final var conformer = generator.getNextConformer();
      if (conformer == null) {
        break;
      }
      attempted++;
      final var current = conformer.toMolecule(null);
      requireMapping(input, current, true);
      final var forceField = new ForceFieldMMFF94(current, ForceFieldMMFF94.MMFF94SPLUS);
      if (forceField.minimise(4000, 1e-4, 1e-6) != 0) {
        notConverged++;
        continue;
      }
      final double energy = forceField.getTotalEnergy();
      if (!Double.isFinite(energy) || Math.abs(energy) > 1e9) {
        invalid++;
        continue;
      }
      final int[] mapping = requireMapping(input, current, true);
      final double[][] coordinates = new double[mapping.length][3];
      for (int i = 0; i < mapping.length; i++) {
        final int atom = mapping[i];
        coordinates[i][0] = coordinate(current.getAtomX(atom));
        coordinates[i][1] = coordinate(current.getAtomY(atom));
        coordinates[i][2] = coordinate(current.getAtomZ(atom));
      }
      if (candidates.stream().anyMatch(c -> sameShape(c.coordinates(), coordinates))) {
        duplicate++;
        continue;
      }
      candidates.add(new Coordinates(energy, coordinates));
    }
    if (System.nanoTime() >= deadline) {
      notes.add("Достигнут срок поиска; возвращены только уже сошедшиеся кандидаты.");
    }
    notes.add(
        "Поиск OpenChemLib: гибкие связи и геометрия циклических фрагментов; MMFF94s+ минимизация до 4000 итераций (grad 1e-4, energy 1e-6). Неисчерпывающий набор, до "
            + attempts
            + " попыток.");
    if (notConverged > 0) {
      notes.add("Не сошлась минимизация: " + notConverged + "; эти кандидаты не возвращены.");
    }
    if (duplicate > 0) {
      notes.add(
          "Повторяющиеся геометрии после минимизации исключены: "
              + duplicate
              + " (RMS попарных расстояний исходных атомов < 0,03 Å).");
    }
    if (invalid > 0) {
      notes.add("Отклонены недопустимые энергии: " + invalid + ".");
    }
    if (candidates.size() < input.requested()) {
      notes.add(
          "Получено "
              + candidates.size()
              + " из запрошенных "
              + input.requested()
              + "; доступные кандидаты или предел попыток исчерпаны.");
    }
    return new Response(candidates, notes, attempted);
  }

  /** Original atoms remain mapped, unchanged and bonded identically; only terminal neutral virtual H may be added. */
  private static int[] requireMapping(
      final Input input, final StereoMolecule molecule, final boolean virtualHydrogens) {
    if (molecule.getAllAtoms() < input.atoms().size()
        || molecule.getAllAtoms() > MAX_TOTAL_ATOMS
        || (!virtualHydrogens && molecule.getAllAtoms() != input.atoms().size())) {
      throw new IllegalArgumentException(
          "Построитель изменил число исходных атомов или превысил 384 атома с виртуальными H.");
    }
    molecule.ensureHelperArrays(com.actelion.research.chem.Molecule.cHelperRings);
    final int[] mapping = new int[input.atoms().size()];
    Arrays.fill(mapping, -1);
    for (int i = 0; i < molecule.getAllAtoms(); i++) {
      final int map = molecule.getAtomMapNo(i);
      if (map == 0 && virtualHydrogens) {
        if (molecule.getAtomicNo(i) != 1
            || molecule.getAtomCharge(i) != 0
            || molecule.getAtomMass(i) != 0
            || molecule.getAllConnAtoms(i) != 1
            || molecule.getAtomMapNo(molecule.getConnAtom(i, 0)) < 1) {
          throw new IllegalArgumentException(
              "Добавлен атом, не являющийся виртуальным терминальным H.");
        }
        continue;
      }
      if (map < 1 || map > mapping.length || mapping[map - 1] != -1) {
        throw new IllegalArgumentException("Потеряна или повторена атомная карта.");
      }
      final var expected = input.atoms().get(map - 1);
      if (molecule.getAtomicNo(i) != expected.number()
          || molecule.getAtomCharge(i) != expected.charge()
          || molecule.getAtomMass(i) != expected.isotope()) {
        throw new IllegalArgumentException("Изменились элемент, заряд или изотоп исходного атома.");
      }
      mapping[map - 1] = i;
    }
    for (final int atom : mapping) {
      if (atom < 0) {
        throw new IllegalArgumentException("Исходный атом отсутствует в результате.");
      }
    }
    final Map<String, BondSpec> expectedBonds = new HashMap<>();
    input.bonds().forEach(b -> expectedBonds.put(edge(b.first(), b.second()), b));
    int originalBonds = 0;
    for (int i = 0; i < molecule.getAllBonds(); i++) {
      final int a = molecule.getAtomMapNo(molecule.getBondAtom(0, i)),
          b = molecule.getAtomMapNo(molecule.getBondAtom(1, i));
      if (a == 0 || b == 0) {
        if (!virtualHydrogens || molecule.getBondOrder(i) != 1) {
          throw new IllegalArgumentException("Недопустимая связь виртуального H.");
        }
        continue;
      }
      final var expected = expectedBonds.get(edge(a - 1, b - 1));
      if (expected == null
          || (expected.aromatic()
              ? !molecule.isAromaticBond(i)
              : molecule.getBondOrder(i) != expected.order())) {
        throw new IllegalArgumentException("Изменились связи исходной структуры.");
      }
      originalBonds++;
    }
    if (originalBonds != expectedBonds.size()) {
      throw new IllegalArgumentException("Потеряны связи исходной структуры.");
    }
    return mapping;
  }

  private static boolean sameShape(final double[][] first, final double[][] second) {
    double sum = 0;
    int n = 0;
    for (int i = 0; i < first.length; i++) {
      for (int j = i + 1; j < first.length; j++) {
        final double delta = distance(first[i], first[j]) - distance(second[i], second[j]);
        sum += delta * delta;
        n++;
      }
    }
    return n > 0 && Math.sqrt(sum / n) < .03;
  }

  private static double distance(final double[] a, final double[] b) {
    return Math.sqrt(
        Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
  }

  private static String edge(final int a, final int b) {
    return Math.min(a, b) + ":" + Math.max(a, b);
  }

  private static double coordinate(final double value) {
    if (!Double.isFinite(value) || Math.abs(value) > 1000) {
      throw new IllegalArgumentException("Недопустимая координата конформера.");
    }
    return value;
  }

  private static void header(final DataOutputStream stream) throws IOException {
    stream.writeInt(MAGIC);
    stream.writeInt(VERSION);
  }

  private static void checkHeader(final DataInputStream stream) throws IOException {
    if (stream.readInt() != MAGIC || stream.readInt() != VERSION) {
      throw new IllegalArgumentException("Неизвестный протокол конформеров.");
    }
  }

  private static int count(final DataInputStream stream, final int min, final int max)
      throws IOException {
    final int count = stream.readInt();
    if (count < min || count > max) {
      throw new IllegalArgumentException("Недопустимый размер в протоколе конформеров.");
    }
    return count;
  }

  private static String text(final DataInputStream stream, final int max) throws IOException {
    final int length = count(stream, 0, max);
    final byte[] bytes = stream.readNBytes(length);
    if (bytes.length != length) {
      throw new EOFException();
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static void writeText(final DataOutputStream stream, final String value, final int max)
      throws IOException {
    final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > max) {
      throw new IllegalArgumentException("Слишком длинный текст протокола конформеров.");
    }
    stream.writeInt(bytes.length);
    stream.write(bytes);
  }
}
