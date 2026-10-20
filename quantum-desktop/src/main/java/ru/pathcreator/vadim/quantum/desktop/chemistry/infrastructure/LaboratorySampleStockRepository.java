/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.inventory.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratorySampleStockTransfer;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Two independent checked saves with a durable intent. There is no cross-file atomicity claim. */
public final class LaboratorySampleStockRepository {
  private final LaboratoryFileRepository files = new LaboratoryFileRepository();
  private final LaboratoryFileCodec drawings = new LaboratoryFileCodec();
  private final LaboratoryInventoryRepository inventory = new LaboratoryInventoryRepository();
  private final LaboratoryInventoryCodec stocks = new LaboratoryInventoryCodec();

  public enum Saved {
    YES,
    NO,
    UNKNOWN
  }

  public record Intent(
      Path directory,
      String id,
      Path source,
      Path ledger,
      String sourceHash,
      String inventoryHash,
      LaboratoryDrawing before,
      LaboratoryDrawing after,
      LaboratoryInventory receipt) {
    public LaboratoryBatch batch() {
      return this.receipt.batches().getFirst();
    }
  }

  public record Progress(
      Saved sourceSaved, Saved stockSaved, LaboratoryDrawing source, String detail) {
    public boolean complete() {
      return this.sourceSaved == Saved.YES && this.stockSaved == Saved.YES;
    }

    public String summary() {
      return "Расход образца: "
          + label(this.sourceSaved)
          + ". Приход партии: "
          + label(this.stockSaved)
          + "."
          + (this.detail.isBlank() ? "" : "\n" + this.detail);
    }

    private static String label(Saved s) {
      return switch (s) {
        case YES -> "сохранён";
        case NO -> "не сохранён";
        case UNKNOWN -> "не подтверждён";
      };
    }
  }

  public static final class TransferException extends IOException {
    private final Intent intent;
    private final Progress progress;

    TransferException(Throwable cause, Intent intent, Progress progress) {
      super(
          Objects.toString(cause.getMessage(), "Передача не завершена") + "\n" + progress.summary(),
          cause);
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
      final Path ledgerPath,
      final LaboratorySampleStockTransfer.Prepared prepared)
      throws IOException {
    final Path source = sourcePath.toRealPath(), ledger = ledgerPath.toAbsolutePath().normalize();
    if (!ledger.equals(
        LaboratoryInventoryRepository.projectFolder(source).resolve("materials.qchem")))
      throw new IOException("Партия должна поступить в учёт материалов исходного проекта.");
    final var current = this.source(source);
    if (!current.drawing().equals(prepared.before()))
      throw new IOException("Исходный опыт изменился. Заново проверьте передачу.");
    final var stock = this.stock(ledger);
    prepared.received();
    stock.inventory().receive(prepared.material(), prepared.batch(), prepared.receipt());
    Files.createDirectories(directory(source));
    final Path folder = directory(source).resolve(prepared.id());
    Files.createDirectory(folder);
    final var p = new Properties();
    p.setProperty("format", "1");
    p.setProperty("id", prepared.id());
    p.setProperty("source", source.toString());
    p.setProperty("ledger", ledger.toString());
    p.setProperty("sourceHash", current.fingerprint());
    p.setProperty("inventoryHash", Objects.toString(stock.fingerprint(), ""));
    snapshot(folder, "before", this.drawings.write(prepared.before()), p);
    snapshot(folder, "after", this.drawings.write(prepared.after()), p);
    snapshot(folder, "receipt", this.stocks.write(prepared.received()), p);
    final var out = new StringWriter();
    p.store(out, "Quantum physical sample to stock intent1");
    writeNew(folder.resolve("intent.properties"), LaboratoryFileRepository.encode(out.toString()));
    return this.read(folder, source);
  }

  public Progress resume(final Intent intent) throws TransferException {
    try (final var channel =
            FileChannel.open(
                intent.directory().resolve("operation.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        final var lock = channel.tryLock()) {
      if (lock == null) throw new IOException("Эта передача выполняется другим окном.");
      if (!this.read(intent.directory(), intent.source()).equals(intent))
        throw new IOException("Снимок восстановления изменился.");
      var source = this.source(intent.source());
      var stock = this.stock(intent.ledger());
      if (intent.inventoryHash() != null && stock.fingerprint() == null)
        throw new IOException(
            "Прежний файл материалов отсутствует. Восстановите его; пустая книга не заменит прежний учёт.");
      if (!sourceReceipt(source.drawing(), intent)) {
        if (stockReceipt(stock.inventory(), intent))
          throw new IOException(
              "Приход есть, но расход образца отсутствует. Проверьте восстановление исходного опыта; повторный расход автоматически не проводится.");
        if (!Objects.equals(stock.fingerprint(), intent.inventoryHash()))
          throw new IOException(
              "Учёт материалов изменился до расхода. Подготовьте передачу заново; образец не списан.");
        if (!source.fingerprint().equals(intent.sourceHash())
            || !source.drawing().equals(intent.before()))
          throw new IOException(
              "Исходный опыт изменился до расхода. Передача не будет применять устаревший остаток.");
        this.files.write(intent.source(), intent.after(), source.fingerprint());
        source = this.source(intent.source());
        if (!sourceReceipt(source.drawing(), intent))
          throw new IOException("Расход образца не подтверждён.");
      }
      stock = this.stock(intent.ledger());
      if (intent.inventoryHash() != null && stock.fingerprint() == null)
        throw new IOException(
            "Учёт материалов исчез после расхода. Восстановите исходную книгу перед продолжением.");
      if (!stockReceipt(stock.inventory(), intent)) {
        final var receipt = intent.receipt();
        final var next =
            stock
                .inventory()
                .receive(
                    receipt.materials().getFirst(),
                    receipt.batches().getFirst(),
                    receipt.operations().getFirst());
        this.inventory.write(intent.ledger(), next, stock.fingerprint());
      }
      final var progress = this.inspect(intent);
      if (!progress.complete())
        throw new IOException("Не удалось подтвердить обе стороны передачи.");
      if (!Files.exists(intent.directory().resolve("complete")))
        writeNew(
            intent.directory().resolve("complete"),
            LaboratoryFileRepository.encode(Instant.now().toString()));
      return progress;
    } catch (final Exception error) {
      throw new TransferException(error, intent, this.inspect(intent));
    }
  }

  public Progress inspect(final Intent intent) {
    Saved from = Saved.UNKNOWN, to = Saved.UNKNOWN;
    LaboratoryDrawing source = null;
    final List<String> errors = new ArrayList<>();
    try {
      source = this.source(intent.source()).drawing();
      from = sourceReceipt(source, intent) ? Saved.YES : Saved.NO;
    } catch (final Exception error) {
      errors.add("Образец: " + error.getMessage());
    }
    try {
      final var stock = this.stock(intent.ledger());
      if (stock.fingerprint() == null && intent.inventoryHash() != null)
        throw new IOException("прежняя книга отсутствует");
      to = stockReceipt(stock.inventory(), intent) ? Saved.YES : Saved.NO;
    } catch (final Exception error) {
      errors.add("Материалы: " + error.getMessage());
    }
    return new Progress(from, to, source, String.join("\n", errors));
  }

  public record Pending(List<Intent> intents, List<String> errors) {
    public Pending {
      intents = List.copyOf(intents);
      errors = List.copyOf(errors);
    }
  }

  public List<Intent> pending(final Path sourcePath) throws IOException {
    return this.discover(sourcePath).intents();
  }

  public Pending discover(final Path sourcePath) throws IOException {
    final Path source = sourcePath.toRealPath(), dir = directory(source);
    if (!Files.isDirectory(dir)) return new Pending(List.of(), List.of());
    final List<Intent> result = new ArrayList<>();
    final List<String> errors = new ArrayList<>();
    int count = 0;
    try (final var entries = Files.newDirectoryStream(dir)) {
      for (final var path : entries) {
        if (!Files.isDirectory(path)
            || !Files.exists(path.resolve("intent.properties"))
            || Files.exists(path.resolve("complete"))) continue;
        if (++count > 1000) {
          errors.add(
              "Показаны первые1000 записей восстановления; остальные требуют проверки папки "
                  + dir);
          break;
        }
        try {
          result.add(this.read(path, source));
        } catch (Exception invalid) {
          errors.add(
              "Не прочитано восстановление "
                  + path.getFileName()
                  + ": "
                  + Objects.toString(invalid.getMessage(), "повреждённая запись")
                  + ". Файлы сохранены для проверки.");
        }
      }
    }
    return new Pending(result, errors);
  }

  public Intent read(final Path folder, final Path expectedSource) throws IOException {
    final Path dir = folder.toAbsolutePath().normalize();
    final var p = new Properties();
    p.load(
        new StringReader(
            LaboratoryFileRepository.decode(bytes(dir.resolve("intent.properties"), 32768))));
    if (!p.stringPropertyNames()
            .equals(
                Set.of(
                    "format",
                    "id",
                    "source",
                    "ledger",
                    "sourceHash",
                    "inventoryHash",
                    "beforeHash",
                    "afterHash",
                    "receiptHash"))
        || !"1".equals(p.getProperty("format")))
      throw new IOException("Неподдерживаемая запись восстановления.");
    final String id = p.getProperty("id");
    final Path source = Path.of(p.getProperty("source")).toAbsolutePath().normalize(),
        ledger = Path.of(p.getProperty("ledger")).toAbsolutePath().normalize();
    if (!id.matches("sample_stock_[0-9a-f-]{36}")
        || !dir.getFileName().toString().equals(id)
        || !source.equals(expectedSource.toRealPath())
        || !dir.getParent().equals(directory(source))
        || !ledger.getFileName().toString().equals("materials.qchem")
        || ledger.equals(source))
      throw new IOException("Неверные пути или идентификатор передачи.");
    if (!ledger.equals(
        LaboratoryInventoryRepository.projectFolder(source).resolve("materials.qchem")))
      throw new IOException(
          "Учёт материалов больше не соответствует исходному проекту. Восстановите исходное расположение файлов.");
    final String sourceHash = p.getProperty("sourceHash"),
        inventoryHash = p.getProperty("inventoryHash");
    if (!sourceHash.matches("[0-9a-f]{64}")
        || !inventoryHash.isEmpty() && !inventoryHash.matches("[0-9a-f]{64}"))
      throw new IOException("Неверные отпечатки файлов.");
    final var result =
        new Intent(
            dir,
            id,
            source,
            ledger,
            sourceHash,
            inventoryHash.isEmpty() ? null : inventoryHash,
            this.drawings.read(snapshot(dir, "before", p)),
            this.drawings.read(snapshot(dir, "after", p)),
            this.stocks.read(snapshot(dir, "receipt", p)));
    validate(result);
    return result;
  }

  private static void validate(final Intent intent) throws IOException {
    final var receipt = intent.receipt();
    if (receipt.materials().size() != 1
        || receipt.batches().size() != 1
        || receipt.operations().size() != 1
        || !receipt.reservations().isEmpty())
      throw new IOException("В передаче должна быть ровно одна новая партия.");
    final var batch = intent.batch();
    final var operation = receipt.operations().getFirst();
    final var origin = batch.origin();
    if (origin == null
        || origin.sourceSamples().isEmpty()
        || !origin.sourceSamples().getFirst().transferId().equals(intent.id())
        || !operation.id().equals(intent.id() + "_in")
        || operation.kind() != LaboratoryStockOperation.Kind.RECEIPT)
      throw new IOException("Приход не соответствует происхождению.");
    final var source = origin.sourceSamples().getFirst().source();
    final var before = intent.before().experiment();
    final var after = intent.after().experiment();
    if (!before.id().equals(source.experimentId()) || !after.id().equals(before.id()))
      throw new IOException("Исходный опыт не совпадает.");
    LaboratorySampleLedger.requireAppend(before.samples(), after.samples());
    final var old =
        before.samples().stream()
            .filter(s -> s.id().equals(source.sampleId()))
            .findFirst()
            .orElseThrow();
    final var changed =
        after.samples().stream()
            .filter(s -> s.id().equals(source.sampleId()))
            .findFirst()
            .orElseThrow();
    if (changed.events().size() != old.events().size() + 1
        || before.samples().size() != after.samples().size())
      throw new IOException("Передача изменяет больше одного события образца.");
    final var event = changed.events().getLast();
    if (!event.id().equals(intent.id() + "_out")
        || event.action() != LaboratorySample.Action.CONSUME
        || !event.quantity().equals(operation.quantity())
        || !event.time().equals(operation.recordedAt())
        || !LaboratoryMaterialProperties.signature(old.molecule())
            .equals(
                LaboratoryMaterialProperties.signature(receipt.materials().getFirst().molecule())))
      throw new IOException("Количество или структура сторон не совпадает.");
    for (final var s : before.samples())
      if (!s.id().equals(old.id()) && !after.samples().contains(s))
        throw new IOException("Изменён другой образец.");
    if (after.journal().size() != before.journal().size() + 1
        || !after.journal().subList(0, before.journal().size()).equals(before.journal())
        || !after.journal().getLast().id().equals(intent.id() + "_journal"))
      throw new IOException("Неверная запись журнала передачи.");
    final var expected =
        new LaboratoryExperiment(
            before.id(),
            before.title(),
            before.conditions(),
            before.notes(),
            before.components(),
            after.journal(),
            before.attachments(),
            before.stages(),
            before.analyses(),
            after.samples(),
            before.procedure(),
            before.route(),
            before.operations(),
            before.analyticalEvidence(),
            before.literature());
    if (!expected.equals(after) || !intent.before().layouts().equals(intent.after().layouts()))
      throw new IOException("Передача содержит постороннее изменение опыта.");
  }

  private static boolean sourceReceipt(final LaboratoryDrawing drawing, final Intent intent)
      throws IOException {
    if (!drawing.experiment().id().equals(intent.before().experiment().id()))
      throw new IOException("На месте источника другой опыт.");
    final String sid = intent.batch().origin().sourceSamples().getFirst().source().sampleId();
    final var expected =
        intent.after().experiment().samples().stream()
            .filter(s -> s.id().equals(sid))
            .findFirst()
            .orElseThrow()
            .events()
            .getLast();
    for (final var s : drawing.experiment().samples())
      for (final var event : s.events())
        if (event.id().equals(expected.id())) {
          if (!s.id().equals(sid) || !event.equals(expected))
            throw new IOException("Расход с этим идентификатором отличается.");
          return true;
        }
    return false;
  }

  private static boolean stockReceipt(final LaboratoryInventory inventory, final Intent intent)
      throws IOException {
    final var expected = intent.receipt();
    final var event = expected.operations().getFirst();
    final var found =
        inventory.operations().stream().filter(o -> o.id().equals(event.id())).findFirst();
    if (found.isPresent()) {
      if (!found.get().equals(event)
          || !inventory.materials().contains(expected.materials().getFirst())
          || !inventory.batches().contains(expected.batches().getFirst()))
        throw new IOException("Приход с этим идентификатором отличается.");
      return true;
    }
    if (inventory.batches().stream().anyMatch(b -> b.id().equals(intent.batch().id()))
        || inventory.materials().stream()
            .anyMatch(m -> m.id().equals(expected.materials().getFirst().id())))
      throw new IOException("Идентификатор новой партии уже занят.");
    return false;
  }

  private LaboratoryFileRepository.Opened source(Path path) throws IOException {
    if (Files.size(path) > 134217728) throw new IOException("Опыт превышает128 МиБ.");
    return this.files.read(path);
  }

  private LaboratoryInventoryRepository.Opened stock(Path path) throws IOException {
    if (Files.exists(path) && Files.size(path) > 33554432)
      throw new IOException("Учёт материалов превышает32 МиБ.");
    return this.inventory.read(path);
  }

  private static Path directory(Path source) {
    return source.resolveSibling(source.getFileName() + ".stock-transfers");
  }

  private static void snapshot(Path directory, String name, String text, Properties p)
      throws IOException {
    final byte[] data = LaboratoryFileRepository.encode(text);
    if (data.length > 134217728) throw new IOException("Снимок слишком велик.");
    writeNew(directory.resolve(name + ".snapshot"), data);
    p.setProperty(name + "Hash", hash(data));
  }

  private static String snapshot(Path directory, String name, Properties p) throws IOException {
    final byte[] data = bytes(directory.resolve(name + ".snapshot"), 134217728);
    if (!hash(data).equals(p.getProperty(name + "Hash")))
      throw new IOException("Повреждён снимок передачи.");
    return LaboratoryFileRepository.decode(data);
  }

  private static byte[] bytes(Path path, long max) throws IOException {
    if (Files.size(path) > max) throw new IOException("Снимок слишком велик.");
    final byte[] data = Files.readAllBytes(path);
    if (data.length > max) throw new IOException("Снимок слишком велик.");
    return data;
  }

  private static String hash(byte[] data) {
    try {
      return HexFormat.of()
          .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void writeNew(Path target, byte[] bytes) throws IOException {
    final Path temp = Files.createTempFile(target.getParent(), ".sample-stock-", ".tmp");
    try {
      try (final var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
        final var b = ByteBuffer.wrap(bytes);
        while (b.hasRemaining()) channel.write(b);
        channel.force(true);
      }
      Files.move(temp, target);
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
