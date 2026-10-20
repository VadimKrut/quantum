/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.vecmath.Point3d;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.silent.*;

/** Untrusted template computation runs in a killable JVM; only a bounded graph/coordinate protocol crosses the boundary. */
public final class CdkInitialGeometryWorker {
  private static final int MAGIC = 0x51473344, VERSION = 1, MAX_BYTES = 131072;
  private static final long TIMEOUT_MILLIS = 20000;

  private CdkInitialGeometryWorker() {}

  static IAtomContainer build(final IAtomContainer graph) {
    return build(graph, TIMEOUT_MILLIS);
  }

  static IAtomContainer build(final IAtomContainer graph, final long timeoutMillis) {
    if (timeoutMillis < 1 || timeoutMillis > TIMEOUT_MILLIS) {
      throw new IllegalArgumentException("Недопустимое ограничение времени 3D-построителя.");
    }
    Path directory = null;
    Process process = null;
    try {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      directory = Files.createTempDirectory("quantum-initial-geometry-");
      final Path input = directory.resolve("input.bin"), output = directory.resolve("output.bin");
      try (final var stream = new DataOutputStream(Files.newOutputStream(input))) {
        stream.writeInt(MAGIC);
        stream.writeInt(VERSION);
        writeGraph(stream, graph, false);
      }
      if (Files.size(input) > MAX_BYTES) {
        throw new IllegalArgumentException("Граф превышает размер протокола 3D-построителя.");
      }
      final boolean windows =
          System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
      final Path executable =
          Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java");
      if (!Files.isRegularFile(executable)) {
        throw new IllegalArgumentException(
            "В runtime отсутствует Java worker для изолированного 3D-построения.");
      }
      String classpath = System.getProperty("java.class.path", "");
      if (classpath.isBlank()) {
        classpath =
            Path.of(
                    CdkInitialGeometryWorker.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI())
                .toString();
      }
      final var command =
          List.of(
              executable.toString(),
              "-Xmx384m",
              "-Dfile.encoding=UTF-8",
              "-Djava.awt.headless=true",
              "-cp",
              classpath,
              CdkInitialGeometryWorker.class.getName(),
              input.toString(),
              output.toString());
      // Third-party logging is not part of the protocol. DISCARD is bounded and cannot fill a pipe
      // or a log file.
      process =
          new ProcessBuilder(command)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
        throw new IllegalArgumentException(
            "3D-построитель превысил время ожидания и остановлен. Введите готовые координаты или используйте другой метод подготовки.");
      }
      if (!Files.isRegularFile(output)
          || Files.size(output) < 9
          || Files.size(output) > MAX_BYTES) {
        throw new IllegalArgumentException(
            "Изолированный 3D-построитель не вернул допустимый результат (код "
                + process.exitValue()
                + ").");
      }
      try (final var stream = new DataInputStream(Files.newInputStream(output))) {
        header(stream);
        if (!stream.readBoolean()) {
          throw new IllegalArgumentException("3D-построитель: " + text(stream, 2000));
        }
        if (process.exitValue() != 0) {
          throw new IllegalArgumentException("Изолированный 3D-построитель завершился с ошибкой.");
        }
        final var result = readGraph(stream, true);
        if (stream.read() != -1) {
          throw new IllegalArgumentException("Лишние данные в ответе 3D-построителя.");
        }
        return result;
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CancellationException("3D-построение отменено.");
    } catch (final CancellationException | IllegalArgumentException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          "Не удалось выполнить изолированное 3D-построение: " + e.getMessage(), e);
    } finally {
      if (process != null) {
        terminate(process);
      }
      if (directory != null) {
        for (final String file : List.of("input.bin", "output.bin")) {
          try {
            Files.deleteIfExists(directory.resolve(file));
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
              "ОС не подтвердила завершение 3D-процесса " + process.pid() + ".");
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
        throw new IllegalArgumentException("Недопустимый входной файл.");
      }
      final IAtomContainer graph;
      try (final var stream = new DataInputStream(Files.newInputStream(input))) {
        header(stream);
        graph = readGraph(stream, false);
        if (stream.read() != -1) {
          throw new IllegalArgumentException("Лишние входные данные.");
        }
      }
      ModelBuilder3D.getInstance(SilentChemObjectBuilder.getInstance())
          .generate3DCoordinates(graph, false);
      try (final var stream = new DataOutputStream(Files.newOutputStream(output))) {
        stream.writeInt(MAGIC);
        stream.writeInt(VERSION);
        stream.writeBoolean(true);
        writeGraph(stream, graph, true);
      }
    } catch (final Throwable e) {
      try (final var stream = new DataOutputStream(Files.newOutputStream(output))) {
        stream.writeInt(MAGIC);
        stream.writeInt(VERSION);
        stream.writeBoolean(false);
        final String message = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
        stream.writeUTF(message.substring(0, Math.min(2000, message.length())));
      } catch (final Exception ignored) {
      }
      System.exit(1);
    }
  }

  private static void header(final DataInputStream stream) throws IOException {
    if (stream.readInt() != MAGIC || stream.readInt() != VERSION) {
      throw new IllegalArgumentException("Неизвестная версия 3D-протокола.");
    }
  }

  private static String text(final DataInputStream stream, final int max) throws IOException {
    final String value = stream.readUTF();
    if (value.length() > max) {
      throw new IllegalArgumentException("Слишком длинное поле 3D-протокола.");
    }
    return value;
  }

  private static int count(final DataInputStream stream, final int min, final int max)
      throws IOException {
    final int value = stream.readInt();
    if (value < min || value > max) {
      throw new IllegalArgumentException("Недопустимое число элементов 3D-протокола.");
    }
    return value;
  }

  private static void writeGraph(
      final DataOutputStream stream, final IAtomContainer graph, final boolean coordinates)
      throws IOException {
    if (graph.getAtomCount() < 2
        || graph.getAtomCount() > 128
        || graph.getBondCount() < 1
        || graph.getBondCount() > 512) {
      throw new IllegalArgumentException("3D worker: предел 128 атомов и 512 связей.");
    }
    stream.writeInt(graph.getAtomCount());
    stream.writeInt(graph.getBondCount());
    for (final var a : graph.atoms()) {
      if (a.getID() == null
          || a.getID().isBlank()
          || a.getID().length() > 200
          || a.getSymbol() == null
          || a.getSymbol().length() > 3) {
        throw new IllegalArgumentException("Неверное обозначение атома.");
      }
      stream.writeUTF(a.getID());
      stream.writeUTF(a.getSymbol());
      stream.writeInt(a.getFormalCharge());
      stream.writeInt(a.getMassNumber() == null ? 0 : a.getMassNumber());
      stream.writeBoolean(a.isAromatic());
      if (coordinates) {
        final var p = a.getPoint3d();
        if (p == null) {
          throw new IllegalArgumentException("Неполные координаты worker.");
        }
        stream.writeDouble(p.x);
        stream.writeDouble(p.y);
        stream.writeDouble(p.z);
      }
    }
    for (final var b : graph.bonds()) {
      stream.writeInt(graph.indexOf(b.getBegin()));
      stream.writeInt(graph.indexOf(b.getEnd()));
      stream.writeInt(b.getOrder().numeric());
      stream.writeBoolean(b.isAromatic());
      stream.writeUTF(b.getStereo().name());
    }
  }

  private static IAtomContainer readGraph(final DataInputStream stream, final boolean coordinates)
      throws IOException {
    final int atoms = count(stream, 2, 128), bonds = count(stream, 1, 512);
    final IAtomContainer graph = new AtomContainer();
    final Set<String> ids = new HashSet<>();
    for (int i = 0; i < atoms; i++) {
      final String id = text(stream, 200), symbol = text(stream, 3);
      if (id.isBlank() || !ids.add(id) || !symbol.matches("[A-Z][a-z]{0,2}")) {
        throw new IllegalArgumentException("Повтор или неверное обозначение атома.");
      }
      final int charge = count(stream, -8, 8), isotope = count(stream, 0, 400);
      final var a = new Atom(symbol);
      a.setID(id);
      a.setFormalCharge(charge);
      a.setImplicitHydrogenCount(0);
      if (isotope > 0) {
        a.setMassNumber(isotope);
      }
      a.setIsAromatic(stream.readBoolean());
      if (coordinates) {
        final double x = stream.readDouble(), y = stream.readDouble(), z = stream.readDouble();
        if (!Double.isFinite(x)
            || !Double.isFinite(y)
            || !Double.isFinite(z)
            || Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) > 1000) {
          throw new IllegalArgumentException("Недопустимые координаты worker.");
        }
        a.setPoint3d(new Point3d(x, y, z));
      }
      graph.addAtom(a);
    }
    final Set<String> edges = new HashSet<>();
    for (int i = 0; i < bonds; i++) {
      final int a = count(stream, 0, atoms - 1),
          b = count(stream, 0, atoms - 1),
          order = count(stream, 1, 3);
      if (a == b || !edges.add(Math.min(a, b) + ":" + Math.max(a, b))) {
        throw new IllegalArgumentException("Повтор или неверная связь.");
      }
      final var bond =
          new Bond(
              graph.getAtom(a),
              graph.getAtom(b),
              order == 1
                  ? IBond.Order.SINGLE
                  : order == 2 ? IBond.Order.DOUBLE : IBond.Order.TRIPLE);
      bond.setIsAromatic(stream.readBoolean());
      bond.setStereo(IBond.Stereo.valueOf(text(stream, 40)));
      graph.addBond(bond);
    }
    return graph;
  }
}
