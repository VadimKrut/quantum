/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Owns only marked tutorial runs. No project paths are accepted from progress files. */
public final class LaboratoryTutorialStore {
  private static final String MARKER = ".quantum-tutorial-run";
  private final Path root;
  private String run;
  private int step;
  private boolean completed;
  private final Set<String> lessons = new TreeSet<>();

  public LaboratoryTutorialStore() throws IOException {
    this(Path.of(System.getProperty("user.home"), "Documents", "Quantum IR Studio", "Training"));
  }

  public LaboratoryTutorialStore(Path root) throws IOException {
    this.root = root.toAbsolutePath().normalize();
    Path state = this.root.resolve("progress.properties");
    if (Files.exists(state, LinkOption.NOFOLLOW_LINKS)) {
      regular(state);
      if (Files.size(state) > 8192) throw new IOException("Повреждён файл прогресса обучения.");
      Properties p = new Properties();
      try (var reader = Files.newBufferedReader(state)) {
        p.load(reader);
      }
      String id = p.getProperty("run", "");
      if (!id.isEmpty()) {
        if (!id.matches("run-[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))
          throw new IOException("Некорректная учебная сессия.");
        this.run = id;
        if (Files.exists(directory(), LinkOption.NOFOLLOW_LINKS)) validate();
        else this.run = null;
      }
      try {
        this.step = Math.max(0, Math.min(500, Integer.parseInt(p.getProperty("step", "0"))));
      } catch (NumberFormatException ignored) {
        this.step = 0;
      }
      this.completed = Boolean.parseBoolean(p.getProperty("completed", "false"));
      for (String lesson :
          ("2".equals(p.getProperty("lessonsVersion")) ? p.getProperty("lessons", "") : "")
              .split(",")) if (lesson.matches("[a-z-]{1,64}")) lessons.add(lesson);
    }
  }

  public boolean exists() {
    return this.run != null;
  }

  public int step() {
    return this.step;
  }

  public boolean completed() {
    return this.completed;
  }

  public Set<String> lessons() {
    return Set.copyOf(lessons);
  }

  public void lessonCompleted(String id) throws IOException {
    validate();
    if (!id.matches("[a-z-]{1,64}")) throw new IllegalArgumentException("Invalid lesson");
    lessons.add(id);
    save();
  }

  public Properties draft() throws IOException {
    Properties value = new Properties();
    if (!exists()) return value;
    validate();
    Path path = directory().resolve("input.properties");
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      regular(path);
      if (Files.size(path) > 131072) throw new IOException("Учебный черновик слишком большой.");
      try (var reader = Files.newBufferedReader(path)) {
        value.load(reader);
      }
    }
    return value;
  }

  public void draft(Properties value) throws IOException {
    validate();
    Path temp = Files.createTempFile(directory(), ".input-", ".tmp");
    try {
      try (var writer = Files.newBufferedWriter(temp)) {
        value.store(writer, "Unsubmitted tutorial input");
      }
      Files.move(
          temp, directory().resolve("input.properties"), StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  public Path directory() {
    if (this.run == null) throw new IllegalStateException("Обучение ещё не начато.");
    return this.root.resolve(this.run);
  }

  public Path start() throws IOException {
    if (this.run != null) {
      validate();
      return directory();
    }
    Files.createDirectories(this.root);
    this.run = "run-" + UUID.randomUUID();
    Files.createDirectory(directory());
    Files.writeString(directory().resolve(MARKER), this.run, StandardOpenOption.CREATE_NEW);
    this.step = 0;
    this.completed = false;
    this.lessons.clear();
    save();
    return directory();
  }

  public void progress(int step, boolean completed) throws IOException {
    validate();
    this.step = Math.max(0, step);
    this.completed = completed;
    save();
  }

  public void delete() throws IOException {
    if (this.run == null) return;
    validate();
    Path target = directory();
    Path real = target.toRealPath();
    List<Path> owned = new ArrayList<>();
    // Preflight the entire tree before deleting anything. Never follow links/junctions.
    Files.walkFileTree(
        target,
        new SimpleFileVisitor<>() {
          private void check(Path path, BasicFileAttributes attrs) throws IOException {
            if (attrs.isSymbolicLink() || attrs.isOther() || !path.toRealPath().startsWith(real))
              throw new IOException("Учебная папка содержит ссылку. Очистка остановлена.");
            if (owned.size() >= 20000)
              throw new IOException("Слишком много учебных файлов для автоматической очистки.");
            owned.add(path);
          }

          @Override
          public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
              throws IOException {
            check(path, attrs);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
              throws IOException {
            check(path, attrs);
            return FileVisitResult.CONTINUE;
          }
        });
    owned.sort(Comparator.comparingInt(Path::getNameCount).reversed());
    owned.remove(target);
    owned.remove(target.resolve(MARKER));
    for (Path path : owned) Files.delete(path);
    Files.delete(target.resolve(MARKER));
    Files.delete(target);
    this.run = null;
    this.step = 0;
    this.completed = false;
    this.lessons.clear();
    save();
  }

  private void validate() throws IOException {
    Path target = directory();
    BasicFileAttributes attrs =
        Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attrs.isDirectory()
        || attrs.isSymbolicLink()
        || attrs.isOther()
        || !target.toRealPath().getParent().equals(this.root.toRealPath()))
      throw new IOException("Учебная папка не прошла проверку расположения.");
    Path marker = target.resolve(MARKER);
    regular(marker);
    if (Files.size(marker) > 100 || !Files.readString(marker).equals(this.run))
      throw new IOException("Эта папка не принадлежит режиму обучения.");
  }

  private static void regular(Path path) throws IOException {
    var attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attrs.isRegularFile() || attrs.isSymbolicLink() || attrs.isOther())
      throw new IOException("Ожидался обычный файл обучения: " + path.getFileName());
  }

  private void save() throws IOException {
    Files.createDirectories(this.root);
    Properties p = new Properties();
    p.setProperty("run", this.run == null ? "" : this.run);
    p.setProperty("step", Integer.toString(this.step));
    p.setProperty("completed", Boolean.toString(this.completed));
    p.setProperty("lessons", String.join(",", this.lessons));
    p.setProperty("lessonsVersion", "2");
    Path temp = Files.createTempFile(this.root, ".progress-", ".tmp");
    try {
      try (var writer = Files.newBufferedWriter(temp)) {
        p.store(writer, "Quantum IR Studio tutorial");
      }
      try {
        Files.move(
            temp,
            this.root.resolve("progress.properties"),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(
            temp, this.root.resolve("progress.properties"), StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
