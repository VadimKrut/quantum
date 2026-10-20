/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEntry;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryEvidence;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryRepeatContext;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Local organization of ordinary QCHEM files. Workflow labels never assert scientific results. */
public final class LaboratoryNotebookRepository {
  public static final String MANIFEST_NAME = ".qchem-notebook.properties";
  public static final int MAX_MANIFEST_BYTES = 8 * 1024 * 1024;
  public static final int MAX_PROJECTS = 1000;
  public static final int MAX_SECTIONS = 256;
  public static final int MAX_ITEMS = 4096;
  private static final int MAX_DEPTH = 16;
  private static final String ZERO_REVISION = "0".repeat(64);
  private final Path root;
  private final LaboratoryFileRepository files = new LaboratoryFileRepository();

  /** All values describe the user's organization, independently of QCHEM observations and yields. */
  public enum Phase {
    PLANNED,
    IN_PROGRESS,
    WORKUP,
    PURIFICATION,
    WAITING,
    REVIEW,
    COMPLETED,
    PAUSED
  }

  public record Project(String id, String title, Path root) {
    public Project {
      id = identifier(id);
      title = name(title);
      root = absolute(root);
    }
  }

  public record Section(String id, String parentId, String title) {
    public Section {
      id = identifier(id);
      parentId = optionalId(parentId);
      title = name(title);
    }
  }

  public record Item(
      String id, Path path, String sectionId, Phase phase, String waitingFor, String nextAction) {
    public Item {
      id = identifier(id);
      path = absolute(path);
      sectionId = optionalId(sectionId);
      Objects.requireNonNull(phase, "Статус работы");
      waitingFor = note(waitingFor);
      nextAction = note(nextAction);
      if (phase == Phase.WAITING && waitingFor.isBlank()) {
        throw new IllegalArgumentException("Укажите, чего ожидаете: например, спектр ЯМР.");
      }
    }
  }

  public record Snapshot(
      Project project, List<Section> sections, List<Item> items, String revision) {
    public Snapshot {
      Objects.requireNonNull(project);
      sections = List.copyOf(sections);
      items = List.copyOf(items);
      if (revision == null || !revision.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException("Отсутствует версия проекта.");
      }
      validate(project, sections, items);
    }
  }

  public LaboratoryNotebookRepository() {
    this(defaultRoot());
  }

  public LaboratoryNotebookRepository(final Path root) {
    this.root = absolute(root.toAbsolutePath().normalize());
  }

  public static Path defaultRoot() {
    return Path.of(System.getProperty("user.home"), "Documents", "Quantum IR Studio", "Projects")
        .toAbsolutePath()
        .normalize();
  }

  public Path catalogRoot() {
    return this.root;
  }

  public List<Project> projects() throws IOException {
    checkCancelled();
    guardPath(this.root);
    if (!Files.exists(this.root, LinkOption.NOFOLLOW_LINKS)) {
      return List.of();
    }
    if (!Files.isDirectory(this.root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Каталог проектов должен быть папкой.");
    }
    final List<Project> projects = new ArrayList<>();
    int scanned = 0;
    try (final var children = Files.newDirectoryStream(this.root)) {
      for (final var child : children) {
        checkCancelled();
        if (++scanned > 10000) {
          throw new IOException(
              "В каталоге слишком много папок. Выберите отдельный каталог проектов.");
        }
        if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)
            || !Files.exists(child.resolve(MANIFEST_NAME), LinkOption.NOFOLLOW_LINKS)) {
          continue;
        }
        guardPath(child);
        final Snapshot snapshot = this.readAt(child);
        if (projects.stream()
            .anyMatch(
                p ->
                    p.id().equals(snapshot.project().id())
                        || p.title().equalsIgnoreCase(snapshot.project().title()))) {
          throw new IOException(
              "Не удалось однозначно открыть список: в каталоге обнаружена копия проекта с тем же именем или ID. Данные не изменены. Папка: "
                  + child);
        }
        projects.add(snapshot.project());
        if (projects.size() > MAX_PROJECTS) {
          throw new IOException("В каталоге поддерживается до " + MAX_PROJECTS + " проектов.");
        }
      }
    }
    projects.sort(
        Comparator.comparing(Project::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Project::id));
    return List.copyOf(projects);
  }

  public Project createProject(final String title) throws IOException {
    final String checkedTitle = name(title);
    checkCancelled();
    createDirectories(this.root);
    return locked(
        this.root.resolve(".qchem-notebook-catalog.lock"),
        () -> {
          final var projects = this.projects();
          if (projects.size() >= MAX_PROJECTS) {
            throw new IOException("Достигнуто ограничение числа проектов.");
          }
          if (projects.stream().anyMatch(p -> p.title().equalsIgnoreCase(checkedTitle))) {
            throw new IllegalArgumentException(
                "Проект с таким названием уже существует. Откройте его или задайте другое название.");
          }
          final String id = "project_" + UUID.randomUUID();
          final Path directory = this.root.resolve(fileStem(checkedTitle) + "-" + id.substring(8));
          checkCancelled();
          guardPath(directory);
          Files.createDirectory(directory);
          final Project project = new Project(id, checkedTitle, directory);
          this.commit(null, new Snapshot(project, List.of(), List.of(), ZERO_REVISION));
          return project;
        });
  }

  public Snapshot openProject(final String title) throws IOException {
    final String checkedTitle = name(title);
    final var matches =
        this.projects().stream().filter(p -> p.title().equalsIgnoreCase(checkedTitle)).toList();
    if (matches.size() != 1) {
      throw new IOException("Проект «" + checkedTitle + "» не найден в каталоге.");
    }
    return this.load(matches.getFirst());
  }

  public Snapshot load(final Project project) throws IOException {
    this.requireProject(project);
    checkCancelled();
    final Snapshot snapshot = this.readAt(project.root());
    if (!snapshot.project().equals(project)) {
      throw new IOException("Проект заменён или переименован. Обновите список проектов.");
    }
    return snapshot;
  }

  public Snapshot addSection(final Snapshot expected, final String parentId, final String title)
      throws IOException {
    final String checkedParent = optionalId(parentId), checkedTitle = name(title);
    return this.change(
        expected,
        current -> {
          requireSection(current, checkedParent);
          if (current.sections().stream()
              .anyMatch(
                  s ->
                      Objects.equals(s.parentId(), checkedParent)
                          && s.title().equalsIgnoreCase(checkedTitle))) {
            throw new IllegalArgumentException(
                "В этом разделе уже есть подраздел с таким названием.");
          }
          final var sections = new ArrayList<>(current.sections());
          sections.add(new Section("section_" + UUID.randomUUID(), checkedParent, checkedTitle));
          return this.commit(
              current,
              new Snapshot(current.project(), sections, current.items(), current.revision()));
        });
  }

  public Snapshot addExperiment(final Snapshot expected, final String sectionId, final String title)
      throws IOException {
    return this.addExperiment(expected, sectionId, title, null);
  }

  /** An explicit new plan: the existing repeat factory keeps source context, but never copies execution. */
  public Snapshot addExperiment(
      final Snapshot expected,
      final String sectionId,
      final String title,
      final LaboratoryDrawing template)
      throws IOException {
    return this.addExperiment(
        expected, sectionId, title, template, LaboratoryRepeatContext.Source.UNLOCATED);
  }

  public Snapshot addExperiment(
      final Snapshot expected,
      final String sectionId,
      final String title,
      final LaboratoryDrawing template,
      final LaboratoryRepeatContext.Source source)
      throws IOException {
    final String checkedSection = optionalId(sectionId), checkedTitle = name(title);
    Objects.requireNonNull(source);
    checkCancelled();
    final LaboratoryDrawing drawing;
    if (template == null) {
      final var editor = new LaboratoryEditor();
      editor.details(checkedTitle, editor.document().experiment().conditions(), "");
      drawing = editor.document();
    } else {
      drawing = LaboratoryEditor.plannedCopy(template, checkedTitle, source);
    }
    requirePlan(drawing);
    return this.change(
        expected,
        current -> {
          requireSection(current, checkedSection);
          if (current.items().size() >= MAX_ITEMS) {
            throw new IllegalArgumentException(
                "В проекте поддерживается до " + MAX_ITEMS + " опытов.");
          }
          final String id = "item_" + UUID.randomUUID();
          final Path directory = current.project().root().resolve("experiments");
          createDirectories(directory);
          final Path path =
              directory.resolve(fileStem(checkedTitle) + "-" + id.substring(5) + ".qchem");
          guardPath(path);
          guardPath(path.resolveSibling(path.getFileName() + ".lock"));
          final var items = new ArrayList<>(current.items());
          items.add(new Item(id, path, checkedSection, Phase.PLANNED, "", ""));
          final var candidate =
              new Snapshot(current.project(), current.sections(), items, current.revision());
          this.encode(candidate);
          checkCancelled();
          // Once publication begins, finish manifest registration even if a late cancellation
          // arrives.
          this.files.write(path, drawing, null);
          try {
            return this.commit(current, candidate);
          } catch (final IOException | RuntimeException exception) {
            throw new IOException(
                "Опыт создан: "
                    + path
                    + ". Не удалось добавить его в список проекта; обновите проект и добавьте этот файл. Существующие записи не перезаписаны.",
                exception);
          }
        });
  }

  /** Registers an already existing file inside this project; it never imports or moves that file. */
  public Snapshot referenceExperiment(
      final Snapshot expected, final String sectionId, final Path existingQchem)
      throws IOException {
    final String checkedSection = optionalId(sectionId);
    final Path path =
        existingQchem.isAbsolute()
            ? absolute(existingQchem)
            : expected.project().root().resolve(existingQchem).normalize();
    return this.change(
        expected,
        current -> {
          requireSection(current, checkedSection);
          requireItemPath(current.project(), path);
          guardPath(path);
          if (current.items().stream().anyMatch(item -> item.path().equals(path))) {
            throw new IllegalArgumentException("Этот опыт уже добавлен в проект.");
          }
          this.files.read(path);
          checkCancelled();
          final var items = new ArrayList<>(current.items());
          items.add(
              new Item("item_" + UUID.randomUUID(), path, checkedSection, Phase.PLANNED, "", ""));
          return this.commit(
              current,
              new Snapshot(current.project(), current.sections(), items, current.revision()));
        });
  }

  public Snapshot updateWorkflow(
      final Snapshot expected,
      final String itemId,
      final Phase phase,
      final String waitingFor,
      final String nextAction)
      throws IOException {
    final String checkedId = identifier(itemId);
    final var item =
        expected.items().stream()
            .filter(i -> i.id().equals(checkedId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Опыт отсутствует в проекте. Обновите список."));
    return this.updateWorkflow(
        expected, checkedId, item.sectionId(), phase, waitingFor, nextAction);
  }

  /** Section and workflow edits from one form are committed together, using one snapshot CAS. */
  public Snapshot updateWorkflow(
      final Snapshot expected,
      final String itemId,
      final String sectionId,
      final Phase phase,
      final String waitingFor,
      final String nextAction)
      throws IOException {
    final String checkedId = identifier(itemId), checkedSection = optionalId(sectionId);
    return this.change(
        expected,
        current -> {
          requireSection(current, checkedSection);
          final var previous =
              current.items().stream()
                  .filter(i -> i.id().equals(checkedId))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Опыт отсутствует в проекте. Обновите список."));
          final var updated =
              new Item(
                  previous.id(), previous.path(), checkedSection, phase, waitingFor, nextAction);
          if (updated.equals(previous)) {
            return current;
          }
          final var items =
              current.items().stream()
                  .map(item -> item.id().equals(checkedId) ? updated : item)
                  .toList();
          return this.commit(
              current,
              new Snapshot(current.project(), current.sections(), items, current.revision()));
        });
  }

  /** Changes only the logical section; the experiment file and its workflow notes stay in place. */
  public Snapshot moveExperiment(
      final Snapshot expected, final String itemId, final String sectionId) throws IOException {
    final String checkedId = identifier(itemId), checkedSection = optionalId(sectionId);
    return this.change(
        expected,
        current -> {
          requireSection(current, checkedSection);
          final var previous =
              current.items().stream()
                  .filter(i -> i.id().equals(checkedId))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Опыт отсутствует в проекте. Обновите список."));
          if (Objects.equals(previous.sectionId(), checkedSection)) {
            return current;
          }
          final var updated =
              new Item(
                  previous.id(),
                  previous.path(),
                  checkedSection,
                  previous.phase(),
                  previous.waitingFor(),
                  previous.nextAction());
          final var items =
              current.items().stream()
                  .map(item -> item.id().equals(checkedId) ? updated : item)
                  .toList();
          return this.commit(
              current,
              new Snapshot(current.project(), current.sections(), items, current.revision()));
        });
  }

  private Snapshot change(final Snapshot expected, final IOFunction<Snapshot, Snapshot> action)
      throws IOException {
    Objects.requireNonNull(expected);
    this.requireProject(expected.project());
    checkCancelled();
    return locked(
        expected.project().root().resolve(MANIFEST_NAME + ".lock"),
        () -> {
          final Snapshot current = this.load(expected.project());
          if (!current.equals(expected)) {
            throw new IOException(
                "Проект изменён в другом окне. Обновите список; ваши изменения не перезаписали новую версию.");
          }
          checkCancelled();
          return action.apply(current);
        });
  }

  private void requireProject(final Project project) throws IOException {
    Objects.requireNonNull(project);
    if (!Objects.equals(project.root().getParent(), this.root)) {
      throw new IOException(
          "Проект должен находиться непосредственно в выбранном каталоге проектов.");
    }
    guardPath(project.root());
    if (!Files.isDirectory(project.root(), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Папка проекта отсутствует.");
    }
  }

  private Snapshot readAt(final Path directory) throws IOException {
    if (!Objects.equals(directory.getParent(), this.root)) {
      throw new IOException("Проект находится вне каталога.");
    }
    guardPath(directory);
    final Path file = directory.resolve(MANIFEST_NAME);
    final byte[] data = bytes(file);
    try {
      final Map<String, String> fields = new HashMap<>();
      for (final String line : LaboratoryFileRepository.decode(data).split("\n", -1)) {
        if (line.isEmpty()) {
          continue;
        }
        final int separator = line.indexOf('=');
        if (separator < 1 || line.indexOf('\r') >= 0) {
          throw new IllegalArgumentException("Повреждена строка проекта.");
        }
        final String key = line.substring(0, separator);
        if (!key.matches("[a-z0-9.]+")
            || fields.putIfAbsent(key, line.substring(separator + 1)) != null) {
          throw new IllegalArgumentException("Повторяющееся или неизвестное поле проекта.");
        }
      }
      if (!take(fields, "format").equals("quantum-notebook-1")) {
        throw new IllegalArgumentException("Версия проекта не поддерживается.");
      }
      final Project project =
          new Project(
              take(fields, "project.id"), decoded(take(fields, "project.title")), directory);
      final int sectionCount = count(take(fields, "sections"), MAX_SECTIONS),
          itemCount = count(take(fields, "items"), MAX_ITEMS);
      final List<Section> sections = new ArrayList<>();
      final List<Item> items = new ArrayList<>();
      for (int i = 0; i < sectionCount; i++) {
        final String prefix = "section." + i + ".";
        sections.add(
            new Section(
                take(fields, prefix + "id"),
                nullable(take(fields, prefix + "parent")),
                decoded(take(fields, prefix + "title"))));
      }
      for (int i = 0; i < itemCount; i++) {
        final String prefix = "item." + i + ".";
        final String id = take(fields, prefix + "id"),
            relative = decoded(take(fields, prefix + "path"));
        if (relative.isEmpty()
            || relative.contains("\\")
            || relative.contains(":")
            || relative.startsWith("/")
            || relative.split("/", -1).length > 32) {
          throw new IllegalArgumentException("Некорректный относительный путь опыта.");
        }
        for (final String part : relative.split("/", -1)) {
          if (part.isEmpty() || part.equals(".") || part.equals("..")) {
            throw new IllegalArgumentException("Путь опыта должен оставаться внутри проекта.");
          }
        }
        final Path path = directory.resolve(relative);
        requireItemPath(project, path);
        guardPath(path);
        items.add(
            new Item(
                id,
                path,
                nullable(take(fields, prefix + "section")),
                Phase.valueOf(take(fields, prefix + "phase")),
                decoded(take(fields, prefix + "waiting")),
                decoded(take(fields, prefix + "next"))));
      }
      if (!fields.isEmpty()) {
        throw new IllegalArgumentException(
            "В проекте есть неизвестные поля. Откройте его совместимой версией программы.");
      }
      return new Snapshot(project, sections, items, fingerprint(data));
    } catch (final IllegalArgumentException exception) {
      throw new IOException(
          "Не удалось прочитать проект «"
              + directory.getFileName()
              + "»: "
              + exception.getMessage(),
          exception);
    }
  }

  private Snapshot commit(final Snapshot previous, final Snapshot candidate) throws IOException {
    final Path file = candidate.project().root().resolve(MANIFEST_NAME);
    guardPath(file);
    final byte[] data = this.encode(candidate);
    if (previous != null && !fingerprint(bytes(file)).equals(previous.revision())) {
      throw new IOException("Проект изменился во время сохранения. Обновите список.");
    }
    if (previous == null && Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Файл проекта уже существует.");
    }
    final Path temporary = Files.createTempFile(file.getParent(), ".notebook-", ".tmp");
    try {
      try (final var channel =
          FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
        final var buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
          channel.write(buffer);
        }
        channel.force(true);
      }
      guardPath(file);
      if (previous != null && !fingerprint(bytes(file)).equals(previous.revision())) {
        throw new IOException("Проект изменился во время сохранения. Обновите список.");
      }
      if (previous == null && Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Файл проекта уже существует.");
      }
      Files.move(
          temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      return new Snapshot(
          candidate.project(), candidate.sections(), candidate.items(), fingerprint(data));
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private byte[] encode(final Snapshot snapshot) throws IOException {
    final Map<String, String> fields = new LinkedHashMap<>();
    fields.put("format", "quantum-notebook-1");
    fields.put("project.id", snapshot.project().id());
    fields.put("project.title", encoded(snapshot.project().title()));
    fields.put("sections", Integer.toString(snapshot.sections().size()));
    fields.put("items", Integer.toString(snapshot.items().size()));
    for (int i = 0; i < snapshot.sections().size(); i++) {
      final String prefix = "section." + i + ".";
      final var section = snapshot.sections().get(i);
      fields.put(prefix + "id", section.id());
      fields.put(prefix + "parent", empty(section.parentId()));
      fields.put(prefix + "title", encoded(section.title()));
    }
    for (int i = 0; i < snapshot.items().size(); i++) {
      final String prefix = "item." + i + ".";
      final var item = snapshot.items().get(i);
      fields.put(prefix + "id", item.id());
      fields.put(
          prefix + "path",
          encoded(snapshot.project().root().relativize(item.path()).toString().replace('\\', '/')));
      fields.put(prefix + "section", empty(item.sectionId()));
      fields.put(prefix + "phase", item.phase().name());
      fields.put(prefix + "waiting", encoded(item.waitingFor()));
      fields.put(prefix + "next", encoded(item.nextAction()));
    }
    final StringBuilder text = new StringBuilder();
    fields.forEach((key, value) -> text.append(key).append('=').append(value).append('\n'));
    final byte[] data = LaboratoryFileRepository.encode(text.toString());
    if (data.length > MAX_MANIFEST_BYTES) {
      throw new IOException(
          "Список проекта превышает 8 МиБ. Сократите организационные заметки или разделите проект.");
    }
    return data;
  }

  private static void validate(
      final Project project, final List<Section> sections, final List<Item> items) {
    if (sections.size() > MAX_SECTIONS || items.size() > MAX_ITEMS) {
      throw new IllegalArgumentException("Превышено число разделов или опытов проекта.");
    }
    final Map<String, Section> byId = new HashMap<>();
    final HashSet<String> siblingNames = new HashSet<>();
    for (final var section : sections) {
      if (byId.putIfAbsent(section.id(), section) != null
          || !siblingNames.add(
              empty(section.parentId()) + "\n" + section.title().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Повторяющийся раздел проекта.");
      }
    }
    for (final var section : sections) {
      final HashSet<String> ancestors = new HashSet<>();
      String current = section.id();
      while (current != null) {
        if (!ancestors.add(current) || ancestors.size() > MAX_DEPTH || !byId.containsKey(current)) {
          throw new IllegalArgumentException(
              "Раздел ссылается на отсутствующего родителя, цикл или вложен глубже "
                  + MAX_DEPTH
                  + " уровней.");
        }
        current = byId.get(current).parentId();
      }
    }
    final HashSet<String> ids = new HashSet<>();
    final HashSet<Path> paths = new HashSet<>();
    for (final var item : items) {
      requireItemPath(project, item.path());
      if (!ids.add(item.id())
          || !paths.add(item.path())
          || item.sectionId() != null && !byId.containsKey(item.sectionId())) {
        throw new IllegalArgumentException("Повторяющийся опыт или отсутствующий раздел проекта.");
      }
    }
  }

  private static void requirePlan(final LaboratoryDrawing drawing) {
    final var e = drawing.experiment();
    final var operations = e.operations();
    if (!e.analyses().isEmpty()
        || !e.samples().isEmpty()
        || !e.attachments().isEmpty()
        || !e.procedure().events().isEmpty()
        || !e.analyticalEvidence().entries().isEmpty()
        || !operations.timePoints().isEmpty()
        || !operations.stageOutcomes().isEmpty()
        || e.journal().stream().anyMatch(j -> j.kind() != LaboratoryEntry.Kind.ACTION)
        || e.components().stream()
            .anyMatch(c -> c.actual() != null || c.evidence() != LaboratoryEvidence.PLANNED)
        || operations.workups().stream()
            .anyMatch(
                w ->
                    w.correctsId() != null
                        || w.steps().stream()
                            .anyMatch(s -> s.actual() != null || !s.source().isBlank())
                        || w.losses().stream()
                            .anyMatch(l -> l.actual() != null || !l.source().isBlank()))
        || operations.purifications().stream()
            .anyMatch(
                p ->
                    p.correctsId() != null
                        || p.inputActual() != null
                        || p.inputSampleId() != null
                        || !p.source().isBlank()
                        || !p.pools().isEmpty()
                        || p.fractions().stream()
                            .anyMatch(
                                f ->
                                    f.actual() != null
                                        || f.purityPercent() != null
                                        || f.sampleId() != null
                                        || !f.source().isBlank())
                        || p.losses().stream()
                            .anyMatch(l -> l.actual() != null || !l.source().isBlank()))) {
      throw new IllegalArgumentException(
          "Не удалось отделить план от фактических записей исходного опыта. Новый файл не создан.");
    }
  }

  private static void requireSection(final Snapshot snapshot, final String id) {
    if (id != null && snapshot.sections().stream().noneMatch(s -> s.id().equals(id))) {
      throw new IllegalArgumentException("Раздел отсутствует в актуальном проекте.");
    }
  }

  private static void requireItemPath(final Project project, final Path path) {
    if (!path.isAbsolute()
        || !path.equals(path.normalize())
        || !path.startsWith(project.root())
        || path.equals(project.root())
        || !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".qchem")
        || path.toString().length() > 4096) {
      throw new IllegalArgumentException("Файл QCHEM должен находиться внутри папки проекта.");
    }
  }

  private static String identifier(final String value) {
    if (value == null || !value.matches("[a-z][a-z0-9_-]{1,79}")) {
      throw new IllegalArgumentException("Некорректный идентификатор проекта, раздела или опыта.");
    }
    return value;
  }

  private static String optionalId(final String value) {
    return value == null ? null : identifier(value);
  }

  private static String name(final String value) {
    final String text = Objects.requireNonNull(value, "Название").strip();
    if (text.isEmpty() || text.length() > 200 || text.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException(
          "Название должно содержать 1–200 символов в одной строке.");
    }
    return text;
  }

  private static String note(final String value) {
    Objects.requireNonNull(value, "Организационная заметка");
    if (value.length() > 4096
        || value
            .chars()
            .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
      throw new IllegalArgumentException(
          "Организационная заметка должна содержать не более 4096 символов.");
    }
    return value;
  }

  private static Path absolute(final Path path) {
    Objects.requireNonNull(path, "Путь");
    if (!path.isAbsolute() || !path.equals(path.normalize())) {
      throw new IllegalArgumentException("Требуется нормализованный абсолютный путь.");
    }
    return path;
  }

  private static String nullable(final String value) {
    return value.isEmpty() ? null : value;
  }

  private static String empty(final String value) {
    return value == null ? "" : value;
  }

  private static String take(final Map<String, String> fields, final String key) {
    final String value = fields.remove(key);
    if (value == null) {
      throw new IllegalArgumentException("Отсутствует поле " + key + ".");
    }
    return value;
  }

  private static int count(final String text, final int maximum) {
    if (!text.matches("0|[1-9][0-9]{0,4}")) {
      throw new IllegalArgumentException("Некорректное число записей проекта.");
    }
    final int value = Integer.parseInt(text);
    if (value > maximum) {
      throw new IllegalArgumentException("Превышено число записей проекта.");
    }
    return value;
  }

  private static String encoded(final String text) throws IOException {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(LaboratoryFileRepository.encode(text));
  }

  private static String decoded(final String text) throws IOException {
    if (text.length() > 22000 || !text.matches("[A-Za-z0-9_-]*")) {
      throw new IOException("Повреждён текст проекта.");
    }
    try {
      return LaboratoryFileRepository.decode(Base64.getUrlDecoder().decode(text));
    } catch (final IllegalArgumentException exception) {
      throw new IOException("Повреждён текст проекта.", exception);
    }
  }

  private static String fileStem(final String title) {
    final StringBuilder result = new StringBuilder();
    title
        .codePoints()
        .limit(60)
        .forEach(
            c ->
                result.appendCodePoint(
                    Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ' ' ? c : '_'));
    return result.isEmpty() ? "experiment" : result.toString().strip();
  }

  private static String fingerprint(final byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (final NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static byte[] bytes(final Path path) throws IOException {
    guardPath(path);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || Files.size(path) > MAX_MANIFEST_BYTES) {
      throw new IOException("Файл проекта отсутствует или превышает 8 МиБ.");
    }
    try (final var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
      final byte[] data = input.readNBytes(MAX_MANIFEST_BYTES + 1);
      if (data.length > MAX_MANIFEST_BYTES) {
        throw new IOException("Файл проекта вырос сверх 8 МиБ.");
      }
      return data;
    }
  }

  private static void guardPath(final Path path) throws IOException {
    final Path normalized = absolute(path);
    Path current = normalized.getRoot();
    for (final Path part : normalized) {
      current = current.resolve(part);
      if (Files.isSymbolicLink(current)) {
        throw new IOException(
            "Ссылки и перенаправления папок не поддерживаются внутри каталога проектов: "
                + current);
      }
      if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
          && !current.toRealPath().equals(current)) {
        throw new IOException("Путь проекта перенаправлен за пределы исходной папки: " + current);
      }
    }
  }

  private static void createDirectories(final Path directory) throws IOException {
    guardPath(directory);
    final Path parent = directory.getParent();
    if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
      if (parent != null) {
        createDirectories(parent);
      }
      Files.createDirectory(directory);
    }
    guardPath(directory);
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Ожидалась папка проекта: " + directory);
    }
  }

  private static <T> T locked(final Path path, final IOAction<T> action) throws IOException {
    guardPath(path);
    try (final var channel =
            FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS);
        final var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("Проект сохраняется другим окном. Повторите позже.");
      }
      return action.run();
    } catch (final java.nio.channels.OverlappingFileLockException exception) {
      throw new IOException("Проект уже сохраняется другим окном.", exception);
    }
  }

  private static void checkCancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException(
          "Операция с проектом отменена до записи.");
    }
  }

  @FunctionalInterface
  private interface IOAction<T> {
    T run() throws IOException;
  }

  @FunctionalInterface
  private interface IOFunction<T, R> {
    R apply(T value) throws IOException;
  }
}
