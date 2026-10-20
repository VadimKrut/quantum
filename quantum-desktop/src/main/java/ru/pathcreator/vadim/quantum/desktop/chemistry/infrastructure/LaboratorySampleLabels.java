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
import java.time.*;
import java.util.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.rendering.PDFRenderer;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratorySampleReference;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratorySampleCatalog.Row;

/** Offline vector labels. QR contains only a stable reference, never a path or chemical data. */
public final class LaboratorySampleLabels {
  public enum Format {
    A4("A4 · 14 этикеток 90 × 35 мм"),
    ROLL("Отдельные этикетки 60 × 30 мм");
    private final String title;

    Format(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return this.title;
    }
  }

  public record Options(Format format, int copies, int skip) {
    public Options {
      Objects.requireNonNull(format);
      if (copies < 1
          || copies > 20
          || skip < 0
          || skip > 13
          || format == Format.ROLL && skip != 0) {
        throw new IllegalArgumentException(
            "Копии: 1–20. Пропуск мест на A4: 0–13; на отдельных этикетках: 0.");
      }
    }
  }

  public record Bundle(
      byte[] pdf,
      int pages,
      int labels,
      List<String> tokens,
      List<String> warnings,
      Instant capturedAt) {
    public Bundle {
      pdf = pdf.clone();
      tokens = List.copyOf(tokens);
      warnings = List.copyOf(warnings);
    }

    @Override
    public byte[] pdf() {
      return this.pdf.clone();
    }
  }

  private static final float MM = 72f / 25.4f;

  private LaboratorySampleLabels() {}

  public static BitMatrix qr(final String token) {
    if (!LaboratorySampleReference.isToken(token)) {
      throw new IllegalArgumentException("Неверная ссылка образца.");
    }
    try {
      return new QRCodeWriter()
          .encode(
              token,
              BarcodeFormat.QR_CODE,
              0,
              0,
              Map.of(
                  EncodeHintType.ERROR_CORRECTION,
                  ErrorCorrectionLevel.M,
                  EncodeHintType.MARGIN,
                  4));
    } catch (final WriterException error) {
      throw new IllegalArgumentException("Не удалось построить QR образца.", error);
    }
  }

  public static Bundle generate(
      final List<Row> selected, final Options options, final Instant capturedAt)
      throws IOException {
    if (selected.isEmpty() || (long) selected.size() * options.copies() > 500) {
      throw new IllegalArgumentException("Выберите от 1 до 500 этикеток с учётом копий.");
    }
    if (selected.stream().anyMatch(r -> !r.labelAllowed())) {
      throw new IllegalArgumentException(
          "Сначала сохраните опыт и устраните дубликаты ссылок. Этикетки для неоднозначных или несохранённых образцов не созданы.");
    }
    final Set<String> warnings = new LinkedHashSet<>();
    final List<String> tokens = new ArrayList<>();
    try (final PDDocument pdf = new PDDocument();
        final InputStream regularInput = font("Regular");
        final InputStream boldInput = font("Bold")) {
      final PDFont regular = PDType0Font.load(pdf, regularInput),
          bold = PDType0Font.load(pdf, boldInput);
      pdf.getDocumentInformation().setTitle("Этикетки образцов");
      pdf.getDocumentInformation().setCreator("Quantum IR Studio");
      pdf.getDocumentInformation()
          .setSubject("Снимок " + capturedAt + "; печатать 100 %, без подгонки страницы");
      int cell = options.skip();
      PDPage page = null;
      for (final Row row : selected) {
        for (int copy = 0; copy < options.copies(); copy++) {
          interrupted();
          final boolean sheet = options.format() == Format.A4;
          if (page == null || !sheet || cell % 14 == 0) {
            page = new PDPage(sheet ? PDRectangle.A4 : new PDRectangle(60 * MM, 30 * MM));
            pdf.addPage(page);
          }
          final float width = (sheet ? 90 : 60) * MM, height = (sheet ? 35 : 30) * MM;
          final float x = sheet ? (12 + cell % 2 * 96) * MM : 0,
              y =
                  sheet
                      ? page.getMediaBox().getHeight() - (12 + cell % 14 / 2 * 38) * MM - height
                      : 0;
          try (final var stream =
              new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.APPEND, true)) {
            label(stream, row, x, y, width, height, regular, bold, capturedAt, warnings, sheet);
          }
          tokens.add(row.reference().token());
          cell++;
        }
      }
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      pdf.save(bytes);
      return new Bundle(
          bytes.toByteArray(),
          pdf.getNumberOfPages(),
          tokens.size(),
          tokens,
          new ArrayList<>(warnings),
          capturedAt);
    }
  }

  private static void label(
      final PDPageContentStream stream,
      final Row row,
      final float x,
      final float y,
      final float width,
      final float height,
      final PDFont regular,
      final PDFont bold,
      final Instant time,
      final Set<String> warnings,
      final boolean sheet)
      throws IOException {
    final float pad = 2 * MM,
        qrSize = 22 * MM,
        left = x + pad + qrSize + 1.5f * MM,
        available = x + width - pad - left;
    if (sheet) {
      stream.setStrokingColor(.82f);
      stream.setLineWidth(.3f);
      stream.addRect(x, y, width, height);
      stream.stroke();
    }
    final BitMatrix matrix = qr(row.reference().token());
    final float module = qrSize / matrix.getWidth();
    stream.setNonStrokingColor(0f);
    final float qx = x + pad, qy = y + height - pad - qrSize;
    for (int j = 0; j < matrix.getHeight(); j++) {
      for (int i = 0; i < matrix.getWidth(); i++) {
        if (matrix.get(i, j)) {
          stream.addRect(
              qx + i * module, qy + (matrix.getHeight() - j - 1) * module, module, module);
        }
      }
    }
    stream.fill();
    float size = sheet ? 10 : 8;
    List<String> code;
    while (true) {
      code = lines(row.sample().code(), bold, size, available);
      if (code.size() <= 2 || size <= 6) {
        break;
      }
      size -= .5f;
    }
    if (code.size() > 2) {
      throw new IOException(
          "Код «"
              + row.sample().code()
              + "» не помещается целиком. Выберите A4 или сократите код образца.");
    }
    float top = y + height - pad - size;
    for (final String line : code) {
      text(stream, bold, size, left, top, line);
      top -= size + 1;
    }
    top -= 2;
    final float small = sheet ? 7.5f : 6.5f;
    for (final String line :
        fit(row.balance().label(), regular, small, available, 2, warnings, "подпись")) {
      text(stream, regular, small, left, top, line);
      top -= small + 1;
    }
    for (final String value :
        List.of(
            row.sample().molecule().formula().hillNotation(),
            row.balance().container() + " · " + row.balance().location(),
            row.experimentTitle())) {
      text(
          stream,
          regular,
          6,
          left,
          top,
          fit(value, regular, 6, available, 1, warnings, "поле этикетки").getFirst());
      top -= 7;
    }
    text(
        stream,
        regular,
        5.5f,
        x + pad,
        y + pad,
        time.atZone(ZoneOffset.UTC).toLocalDate() + " · " + row.balance().status());
    if (row.sameCode() > 1) {
      warnings.add(
          "Некоторые коды повторяются у разных образцов. Для однозначного поиска используйте QR; сверяйте название опыта.");
    }
  }

  private static List<String> fit(
      final String text,
      final PDFont font,
      final float size,
      final float width,
      final int count,
      final Set<String> warnings,
      final String field)
      throws IOException {
    final List<String> lines = lines(text, font, size, width);
    if (lines.size() <= count) {
      return lines;
    }
    warnings.add("Длинные подписи сокращены на этикетках; полная запись доступна по QR.");
    final List<String> result = new ArrayList<>(lines.subList(0, count));
    String last = result.getLast();
    while (!last.isEmpty() && font.getStringWidth(last + "…") * size / 1000 > width) {
      last = last.substring(0, last.offsetByCodePoints(last.length(), -1));
    }
    result.set(result.size() - 1, last + "…");
    return result;
  }

  private static List<String> lines(
      final String value, final PDFont font, final float size, final float width)
      throws IOException {
    final String cleaned = value.replaceAll("[\\p{Cntrl}\\s]+", " ").strip();
    final List<String> lines = new ArrayList<>();
    String line = "";
    for (final int cp : cleaned.codePoints().toArray()) {
      final String ch = new String(Character.toChars(cp));
      try {
        font.encode(ch);
      } catch (final IllegalArgumentException unsupported) {
        throw new IOException(
            "Шрифт этикеток не поддерживает символ U+"
                + Integer.toHexString(cp).toUpperCase(Locale.ROOT)
                + ". Измените подпись перед печатью.");
      }
      if (!line.isEmpty() && font.getStringWidth(line + ch) * size / 1000 > width) {
        lines.add(line.strip());
        line = "";
      }
      line += ch;
    }
    if (!line.isEmpty() || lines.isEmpty()) {
      lines.add(line.strip());
    }
    return lines;
  }

  private static void text(
      final PDPageContentStream stream,
      final PDFont font,
      final float size,
      final float x,
      final float y,
      final String value)
      throws IOException {
    stream.beginText();
    stream.setFont(font, size);
    stream.setNonStrokingColor(0f);
    stream.newLineAtOffset(x, y);
    stream.showText(value);
    stream.endText();
  }

  private static InputStream font(final String name) throws IOException {
    final var result =
        LaboratorySampleLabels.class.getResourceAsStream(
            "/chemistry/report/NotoSans-" + name + ".ttf");
    if (result == null) {
      throw new IOException("Шрифт этикеток отсутствует.");
    }
    return result;
  }

  public static byte[] preview(final Bundle bundle, final int page) throws IOException {
    if (page < 0 || page >= bundle.pages()) {
      throw new IllegalArgumentException("Нет такой страницы этикеток.");
    }
    interrupted();
    try (final var document = Loader.loadPDF(bundle.pdf())) {
      final var image = new PDFRenderer(document).renderImageWithDPI(page, 130);
      final var output = new ByteArrayOutputStream();
      javax.imageio.ImageIO.write(image, "png", output);
      interrupted();
      return output.toByteArray();
    }
  }

  private static void interrupted() throws InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedIOException("Подготовка этикеток остановлена.");
    }
  }
}
