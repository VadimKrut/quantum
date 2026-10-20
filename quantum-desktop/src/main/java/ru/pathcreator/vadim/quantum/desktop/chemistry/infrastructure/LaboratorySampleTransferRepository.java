/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySampleTransfer;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Durable, reconcilable two-document transfer. Each document has its own checked save; no cross-file transaction is claimed. */
public final class LaboratorySampleTransferRepository {

  private static final long MAX_FILE = 134217728;
  private final LaboratoryFileRepository files = new LaboratoryFileRepository();
  private final LaboratoryFileCodec codec = new LaboratoryFileCodec();

  public enum Saved {
    YES,
    NO,
    UNKNOWN
  }

  public record Intent(
      Path directory,
      String id,
      Path source,
      Path target,
      String sourceFingerprint,
      LaboratoryDrawing before,
      LaboratoryDrawing after,
      LaboratoryDrawing pending,
      LaboratoryDrawing received) {}

  public record Progress(
      Saved sourceSaved, Saved targetSaved, LaboratoryDrawing sourceDrawing, String detail) {
    public boolean complete() {
      return this.sourceSaved == Saved.YES && this.targetSaved == Saved.YES;
    }

    public String summary() {
      return "Расход в исходном опыте: "
          + label(this.sourceSaved)
          + ". Получение в новом опыте: "
          + label(this.targetSaved)
          + "."
          + (this.detail.isBlank() ? "" : "\n" + this.detail);
    }

    private static String label(final Saved value) {
      return switch (value) {
        case YES -> "сохранён";
        case NO -> "не сохранён";
        case UNKNOWN -> "состояние не подтверждено";
      };
    }
  }

  public static final class TransferException extends IOException {
    private final Intent intent;
    private final Progress progress;

    TransferException(
        final String message, final Throwable cause, final Intent intent, final Progress progress) {
      super(message + "\n" + progress.summary(), cause);
      this.intent = intent;
      this.progress = progress;
    }

    public Intent intent() {
      return this.intent;
    }

    public Progress progress() {
      return this.progress;
    }
  }

  public Intent stage(
      final Path sourcePath,
      final Path targetPath,
      final LaboratorySampleTransfer.Prepared prepared)
      throws IOException {
    final Path source = sourcePath.toRealPath(), target = canonicalNewTarget(targetPath);
    if (source.equals(target) || Files.exists(target) && Files.isSameFile(source, target)) {
      throw new IOException("Новый опыт должен находиться в другом файле.");
    }
    if (Files.exists(target)) {
      throw new IOException(
          "Выбранный файл уже существует. Укажите новое имя; перезапись не выполняется.");
    }
    final var opened = this.readSource(source);
    if (!opened.drawing().equals(prepared.sourceBefore())) {
      throw new IOException("Исходный файл изменился. Обновите опыт и заново проверьте передачу.");
    }
    final Path parent = directory(source);
    Files.createDirectories(parent);
    final Path folder = parent.resolve(prepared.id());
    Files.createDirectory(folder);
    final Properties properties = new Properties();
    properties.setProperty("format", "1");
    properties.setProperty("id", prepared.id());
    properties.setProperty("source", source.toString());
    properties.setProperty("target", target.toString());
    properties.setProperty("sourceFingerprint", opened.fingerprint());
    this.snapshot(folder, "before", prepared.sourceBefore(), properties);
    this.snapshot(folder, "after", prepared.sourceAfter(), properties);
    this.snapshot(folder, "pending", prepared.pendingTarget(), properties);
    this.snapshot(folder, "received", prepared.target(), properties);
    final StringWriter text = new StringWriter();
    properties.store(text, "Quantum sample transfer intent 1");
    writeNew(folder.resolve("intent.properties"), LaboratoryFileRepository.encode(text.toString()));
    return this.read(folder, source);
  }

  public Progress resume(final Intent intent) throws TransferException {
    try (final FileChannel channel =
            FileChannel.open(
                intent.directory().resolve("operation.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        final var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("Эта передача уже выполняется другим окном.");
      }
      final Intent checked = this.read(intent.directory(), intent.source());
      if (!checked.equals(intent)) {
        throw new IOException("Запись восстановления изменилась. Откройте её заново.");
      }
      this.reserveTarget(intent);
      var source = this.readSource(intent.source());
      if (!sourceReceipt(source.drawing(), intent)) {
        if (!source.fingerprint().equals(intent.sourceFingerprint())
            || !source.drawing().equals(intent.before())) {
          throw new IOException(
              "Исходный опыт изменён после подготовки, а расход этой передачи отсутствует. Автоматически изменять новый остаток нельзя.");
        }
        this.files.write(intent.source(), intent.after(), source.fingerprint());
        source = this.readSource(intent.source());
        if (!sourceReceipt(source.drawing(), intent)) {
          throw new IOException("Не удалось подтвердить сохранённый расход исходного образца.");
        }
      }
      final var target = this.readSource(intent.target());
      if (!targetReceipt(target.drawing(), intent)) {
        if (!target.drawing().equals(intent.pending())) {
          throw new IOException(
              "Новый файл изменён после резервирования. Расход уже сохранён; восстановление не перезапишет чужие изменения.");
        }
        this.files.write(intent.target(), intent.received(), target.fingerprint());
      }
      final var progress = this.inspect(intent);
      if (!progress.complete()) {
        throw new IOException("Не удалось подтвердить обе записи передачи.");
      }
      if (!Files.exists(intent.directory().resolve("complete"))) {
        writeNew(
            intent.directory().resolve("complete"),
            Instant.now().toString().getBytes(StandardCharsets.UTF_8));
      }
      return progress;
    } catch (final Exception exception) {
      final var progress = this.inspect(intent);
      throw new TransferException(
          exception.getMessage() == null ? "Передача не завершена." : exception.getMessage(),
          exception,
          intent,
          progress);
    }
  }

  /** Read the actual documents rather than trusting a phase marker after a crash. */
  public Progress inspect(final Intent intent) {
    Saved source = Saved.UNKNOWN, target = Saved.UNKNOWN;
    LaboratoryDrawing drawing = null;
    final List<String> details = new ArrayList<>();
    try {
      drawing = this.readSource(intent.source()).drawing();
      source = sourceReceipt(drawing, intent) ? Saved.YES : Saved.NO;
    } catch (final Exception error) {
      details.add("Источник: " + error.getMessage());
    }
    try {
      if (!Files.exists(intent.target())) {
        target = Saved.NO;
      } else {
        final var value = this.readSource(intent.target()).drawing();
        target =
            targetReceipt(value, intent)
                ? Saved.YES
                : value.equals(intent.pending()) ? Saved.NO : Saved.UNKNOWN;
        if (target == Saved.UNKNOWN) {
          details.add("Новый файл отличается от подготовленного документа передачи.");
        }
      }
    } catch (final Exception error) {
      details.add("Новый опыт: " + error.getMessage());
    }
    return new Progress(source, target, drawing, String.join("\n", details));
  }

  public List<Intent> pending(final Path sourcePath) throws IOException {
    final Path source = sourcePath.toRealPath(), folder = directory(source);
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    final List<Intent> result = new ArrayList<>();
    int count = 0;
    try (final var entries = Files.newDirectoryStream(folder)) {
      for (final var entry : entries) {
        if (!Files.isDirectory(entry)
            || !Files.exists(entry.resolve("intent.properties"))
            || Files.exists(entry.resolve("complete"))) {
          continue;
        }
        if (++count > 1000) {
          throw new IOException("Слишком много незавершённых записей передачи рядом с опытом.");
        }
        result.add(this.read(entry, source));
      }
    }
    return List.copyOf(result);
  }

  public Intent read(final Path folder, final Path expectedSource) throws IOException {
    final Path absolute = folder.toAbsolutePath().normalize();
    final var properties = new Properties();
    properties.load(
        new StringReader(
            LaboratoryFileRepository.decode(bytes(absolute.resolve("intent.properties"), 32768))));
    final Set<String> keys =
        Set.of(
            "format",
            "id",
            "source",
            "target",
            "sourceFingerprint",
            "beforeHash",
            "afterHash",
            "pendingHash",
            "receivedHash");
    if (!properties.stringPropertyNames().equals(keys)
        || !"1".equals(properties.getProperty("format"))) {
      throw new IOException("Неподдерживаемая запись восстановления передачи.");
    }
    final String id = properties.getProperty("id");
    if (!id.matches("transfer_[0-9a-f-]{36}") || !absolute.getFileName().toString().equals(id)) {
      throw new IOException("Неверный идентификатор передачи.");
    }
    final Path source = Path.of(properties.getProperty("source")).toAbsolutePath().normalize(),
        target = Path.of(properties.getProperty("target")).toAbsolutePath().normalize();
    if (!source.equals(expectedSource.toRealPath())
        || !absolute.getParent().equals(directory(source))
        || target.equals(source)
        || !target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".qchem")) {
      throw new IOException("Запись восстановления относится к другим путям опыта.");
    }
    final String fingerprint = properties.getProperty("sourceFingerprint");
    if (!fingerprint.matches("[0-9a-f]{64}")) {
      throw new IOException("Повреждён отпечаток исходного файла.");
    }
    final Intent intent =
        new Intent(
            absolute,
            id,
            source,
            target,
            fingerprint,
            this.snapshot(absolute, "before", properties),
            this.snapshot(absolute, "after", properties),
            this.snapshot(absolute, "pending", properties),
            this.snapshot(absolute, "received", properties));
    validate(intent);
    return intent;
  }

  private static void validate(final Intent intent) throws IOException {
    final var before = intent.before().experiment();
    final var after = intent.after().experiment();
    final var target = intent.received().experiment();
    if (!before.id().equals(after.id())
        || before.id().equals(target.id())
        || !intent.pending().experiment().id().equals(target.id())
        || target.samples().size() != 1
        || target.components().size() != 1
        || !intent.pending().experiment().samples().isEmpty()
        || !intent.pending().experiment().components().isEmpty()) {
      throw new IOException("Нарушены документы передачи.");
    }
    final var received = target.samples().getFirst();
    final var origin = received.origin();
    if (origin == null
        || !origin.transferId().equals(intent.id())
        || !origin.source().experimentId().equals(before.id())) {
      throw new IOException("Происхождение нового образца не соответствует передаче.");
    }
    final var source =
        after.samples().stream()
            .filter(s -> s.id().equals(origin.source().sampleId()))
            .findFirst()
            .orElseThrow(() -> new IOException("Нет исходного образца передачи."));
    final var consumed =
        source.events().stream()
            .filter(e -> e.id().equals(intent.id() + "_out"))
            .findFirst()
            .orElseThrow(() -> new IOException("Нет расхода исходного образца."));
    final var registered = received.events().getFirst();
    if (consumed.action() != LaboratorySample.Action.CONSUME
        || !registered.id().equals(intent.id() + "_in")
        || !consumed.quantity().equals(registered.quantity())
        || !consumed.time().equals(registered.time())
        || !LaboratoryMaterialProperties.signature(source.molecule())
            .equals(LaboratoryMaterialProperties.signature(received.molecule()))) {
      throw new IOException("Количество или структура сторон передачи не совпадают.");
    }
    LaboratorySampleLedger.requireAppend(before.samples(), after.samples());
  }

  private void reserveTarget(final Intent intent) throws IOException {
    if (Files.exists(intent.target())) {
      final var value = this.readSource(intent.target()).drawing();
      if (!value.equals(intent.pending()) && !targetReceipt(value, intent)) {
        throw new IOException(
            "Выбранный файл уже занят другим документом. Исходный расход не выполнялся этой попыткой.");
      }
      return;
    }
    final Path lockPath = intent.target().resolveSibling(intent.target().getFileName() + ".lock");
    try (final var channel =
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        final var lock = channel.tryLock()) {
      if (lock == null) {
        throw new IOException("Новый файл используется другим окном.");
      }
      writeNew(
          intent.target(), LaboratoryFileRepository.encode(this.codec.write(intent.pending())));
    }
    if (!this.readSource(intent.target()).drawing().equals(intent.pending())) {
      throw new IOException(
          "Проверка резервирования нового файла не пройдена. Исходник не списан.");
    }
  }

  private static boolean sourceReceipt(final LaboratoryDrawing drawing, final Intent intent)
      throws IOException {
    if (!drawing.experiment().id().equals(intent.before().experiment().id())) {
      throw new IOException("На месте исходного файла находится другой опыт.");
    }
    final var expectedSample =
        intent.after().experiment().samples().stream()
            .filter(s -> s.events().stream().anyMatch(e -> e.id().equals(intent.id() + "_out")))
            .findFirst()
            .orElseThrow();
    final var expected =
        expectedSample.events().stream()
            .filter(e -> e.id().equals(intent.id() + "_out"))
            .findFirst()
            .orElseThrow();
    for (final var sample : drawing.experiment().samples()) {
      for (final var event : sample.events()) {
        if (event.id().equals(expected.id())) {
          if (!sample.id().equals(expectedSample.id()) || !event.equals(expected)) {
            throw new IOException("Расход с этим идентификатором содержит другие данные.");
          }
          return true;
        }
      }
    }
    return false;
  }

  private static boolean targetReceipt(final LaboratoryDrawing drawing, final Intent intent)
      throws IOException {
    if (!drawing.experiment().id().equals(intent.received().experiment().id())) {
      return false;
    }
    final var expected = intent.received().experiment().samples().getFirst();
    for (final var sample : drawing.experiment().samples()) {
      if (sample.id().equals(expected.id())) {
        if (!java.util.Objects.equals(sample.origin(), expected.origin())
            || !sample.molecule().equals(expected.molecule())
            || !sample.events().getFirst().equals(expected.events().getFirst())) {
          throw new IOException("Полученный образец с этим идентификатором изменён.");
        }
        return true;
      }
    }
    return false;
  }

  private LaboratoryFileRepository.Opened readSource(final Path path) throws IOException {
    if (Files.size(path) > MAX_FILE) {
      throw new IOException("Файл опыта превышает 128 МиБ для передачи образца.");
    }
    return this.files.read(path);
  }

  private void snapshot(
      final Path directory,
      final String name,
      final LaboratoryDrawing drawing,
      final Properties properties)
      throws IOException {
    final byte[] value = LaboratoryFileRepository.encode(this.codec.write(drawing));
    if (value.length > MAX_FILE
        || !this.codec.read(LaboratoryFileRepository.decode(value)).equals(drawing)) {
      throw new IOException("Снимок передачи не прошёл проверку сохранения.");
    }
    writeNew(directory.resolve(name + ".snapshot"), value);
    properties.setProperty(name + "Hash", hash(value));
  }

  private LaboratoryDrawing snapshot(
      final Path directory, final String name, final Properties properties) throws IOException {
    final byte[] value = bytes(directory.resolve(name + ".snapshot"), MAX_FILE);
    if (!hash(value).equals(properties.getProperty(name + "Hash"))) {
      throw new IOException("Повреждён снимок восстановления «" + name + "».");
    }
    return this.codec.read(LaboratoryFileRepository.decode(value));
  }

  private static Path directory(final Path source) {
    return source.resolveSibling(source.getFileName() + ".transfers");
  }

  private static Path canonicalNewTarget(final Path target) throws IOException {
    final Path absolute = target.toAbsolutePath().normalize();
    if (absolute.getParent() == null
        || !absolute
            .getFileName()
            .toString()
            .toLowerCase(java.util.Locale.ROOT)
            .endsWith(".qchem")) {
      throw new IOException("Выберите новый файл с расширением .qchem.");
    }
    return absolute.getParent().toRealPath().resolve(absolute.getFileName());
  }

  private static byte[] bytes(final Path path, final long limit) throws IOException {
    if (Files.size(path) > limit) {
      throw new IOException("Запись восстановления слишком велика.");
    }
    final byte[] value = Files.readAllBytes(path);
    if (value.length > limit) {
      throw new IOException("Запись восстановления слишком велика.");
    }
    return value;
  }

  private static String hash(final byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (final java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  /** The final move intentionally omits REPLACE_EXISTING: a newly occupied path is never overwritten. */
  private static void writeNew(final Path target, final byte[] value) throws IOException {
    final Path temporary = Files.createTempFile(target.getParent(), ".sample-transfer-", ".tmp");
    try {
      try (final var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        final var buffer = ByteBuffer.wrap(value);
        while (buffer.hasRemaining()) {
          channel.write(buffer);
        }
        channel.force(true);
      }
      Files.move(temporary, target);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
