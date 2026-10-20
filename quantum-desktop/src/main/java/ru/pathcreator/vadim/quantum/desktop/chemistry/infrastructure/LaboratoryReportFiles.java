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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryExperimentReport.*;

/** Offline PDF/HTML generation and atomic export of the exact previewed snapshot. */
public final class LaboratoryReportFiles {
  public record Bundle(byte[] pdf, String html, int pages, String fingerprint, Instant createdAt) {
    public Bundle {
      pdf = pdf.clone();
    }

    @Override
    public byte[] pdf() {
      return this.pdf.clone();
    }
  }

  private LaboratoryReportFiles() {}

  public static Bundle generate(
      final Document report,
      final Map<String, byte[]> figures,
      final String fingerprint,
      final Instant createdAt)
      throws IOException {
    if (!fingerprint.matches("[a-f0-9]{64}")) {
      throw new IOException("Нет контрольной суммы исходного снимка.");
    }
    if (report.blocks().size() > 12000
        || report.blocks().stream().mapToLong(b -> b.toString().length()).sum() > 2_000_000) {
      throw new IOException("Отчёт слишком велик. Выберите меньше разделов или исключите журнал.");
    }
    try (final var pdf = new PDDocument()) {
      pdf.getDocumentInformation().setTitle(report.title());
      pdf.getDocumentInformation().setCreator("Quantum IR Studio");
      pdf.getDocumentInformation()
          .setSubject("Снимок опыта " + report.experimentId() + "; SHA-256 " + fingerprint);
      final Layout layout = new Layout(pdf);
      layout.newPage();
      layout.paragraph("ЛАБОРАТОРНЫЙ ОТЧЁТ", 9, true, true);
      layout.paragraph(report.title(), 22, true, false);
      layout.paragraph("Дата отчёта: " + reportDate(createdAt), 9, false, true);
      for (final var block : report.blocks()) {
        interrupted();
        switch (block) {
          case Heading h -> {
            layout.ensure(h.level() == 1 ? 150 : 70);
            layout.y -= 9;
            layout.paragraph(h.text(), h.level() == 1 ? 15 : 11, true, false);
          }
          case Paragraph p ->
              layout.paragraph(p.text(), p.subdued() ? 8.5f : 9.5f, false, p.subdued());
          case Table t -> layout.table(t);
          case Figure f -> {
            final byte[] image = figures.get(f.key());
            if (image == null) {
              throw new IOException("Не подготовлена схема: " + f.key());
            }
            layout.figure(image, f.caption());
          }
        }
      }

      layout.closeStream();
      for (int i = 0; i < pdf.getNumberOfPages(); i++) {
        interrupted();
        try (final var stream =
            new PDPageContentStream(
                pdf, pdf.getPage(i), PDPageContentStream.AppendMode.APPEND, true)) {
          stream.setStrokingColor(0.8f, 0.84f, 0.88f);
          stream.moveTo(42, 35);
          stream.lineTo(553, 35);
          stream.stroke();
          stream.beginText();
          stream.setFont(layout.regular, 8);
          stream.setNonStrokingColor(0.36f, 0.42f, 0.49f);
          stream.newLineAtOffset(42, 23);
          stream.showText("Quantum IR Studio · " + (i + 1) + " / " + pdf.getNumberOfPages());
          stream.endText();
        }
      }
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      pdf.save(bytes);
      interrupted();
      return new Bundle(
          bytes.toByteArray(),
          html(report, figures, fingerprint, createdAt),
          pdf.getNumberOfPages(),
          fingerprint,
          createdAt);
    } catch (final IllegalArgumentException e) {
      throw new IOException("Отчёт не создан: " + e.getMessage(), e);
    }
  }

  public static byte[] preview(final Bundle bundle, final int page, final float dpi)
      throws IOException {
    if (page < 0 || page >= bundle.pages() || dpi < 36 || dpi > 150) {
      throw new IllegalArgumentException("Недопустимый номер страницы или масштаб.");
    }
    interrupted();
    try (final var pdf = Loader.loadPDF(bundle.pdf())) {
      final var image = new PDFRenderer(pdf).renderImageWithDPI(page, dpi);
      final ByteArrayOutputStream result = new ByteArrayOutputStream();
      javax.imageio.ImageIO.write(image, "png", result);
      interrupted();
      return result.toByteArray();
    }
  }

  public static void export(final Path target, final Bundle bundle, final boolean html)
      throws IOException {
    final String extension = html ? ".html" : ".pdf";
    if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
      throw new IOException("Выберите расширение " + extension + ".");
    }
    final Path absolute = target.toAbsolutePath().normalize();
    if (!Files.isDirectory(absolute.getParent())) {
      throw new IOException("Папка назначения отсутствует.");
    }
    final Path temporary =
        Files.createTempFile(absolute.getParent(), ".experiment-report-", ".tmp");
    try {
      interrupted();
      Files.write(temporary, html ? bundle.html().getBytes(StandardCharsets.UTF_8) : bundle.pdf());
      try (final var channel =
          java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        channel.force(true);
      }
      interrupted();
      Files.move(
          temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void interrupted() throws InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedIOException("Подготовка отчёта отменена.");
    }
  }

  private static InputStream font(final String name) throws IOException {
    final var input =
        LaboratoryReportFiles.class.getResourceAsStream(
            "/chemistry/report/NotoSans-" + name + ".ttf");
    if (input == null) {
      throw new IOException("В сборке отсутствует шрифт отчёта.");
    }
    return input;
  }

  private static final class Layout {
    private final PDDocument pdf;
    private final PDFont regular, bold, math;
    private PDPageContentStream stream;
    private float y;
    private static final float WIDTH = 511, BOTTOM = 52;

    Layout(final PDDocument pdf) throws IOException {
      this.pdf = pdf;
      try (final var r = font("Regular");
          final var b = font("Bold");
          final var m =
              LaboratoryReportFiles.class.getResourceAsStream(
                  "/chemistry/report/NotoSansMath-Regular.ttf")) {
        if (m == null) {
          throw new IOException("В сборке отсутствует математический шрифт отчёта.");
        }
        this.regular = PDType0Font.load(pdf, r);
        this.bold = PDType0Font.load(pdf, b);
        this.math = PDType0Font.load(pdf, m);
      }
    }

    void closeStream() throws IOException {
      if (this.stream != null) {
        this.stream.close();
        this.stream = null;
      }
    }

    void newPage() throws IOException {
      interrupted();
      if (this.pdf.getNumberOfPages() >= 200) {
        throw new IOException("Отчёт превысил 200 страниц. Уменьшите состав отчёта.");
      }
      this.closeStream();
      final var page = new PDPage(PDRectangle.A4);
      this.pdf.addPage(page);
      this.stream = new PDPageContentStream(this.pdf, page);
      this.y = 797;
    }

    void ensure(final float height) throws IOException {
      if (this.y - height < BOTTOM) {
        this.newPage();
      }
    }

    void text(
        final String text,
        final float x,
        final float baseline,
        final float size,
        final boolean bold,
        final boolean muted)
        throws IOException {
      this.stream.setNonStrokingColor(
          muted ? 0.35f : 0.09f, muted ? 0.4f : 0.16f, muted ? 0.46f : 0.24f);
      this.stream.beginText();
      this.stream.newLineAtOffset(x, baseline);
      PDFont active = null;
      final StringBuilder run = new StringBuilder();
      for (int offset = 0; offset < text.length(); ) {
        final int cp = text.codePointAt(offset);
        offset += Character.charCount(cp);
        final String character = new String(Character.toChars(cp));
        final PDFont selected = this.glyph(character, bold);
        if (active != null && active != selected) {
          this.stream.setFont(active, size);
          this.stream.showText(run.toString());
          run.setLength(0);
        }
        active = selected;
        run.append(character);
      }
      if (active != null) {
        this.stream.setFont(active, size);
        this.stream.showText(run.toString());
      }
      this.stream.endText();
    }

    PDFont glyph(final String character, final boolean bold) throws IOException {
      final PDFont primary = bold ? this.bold : this.regular;
      try {
        primary.getStringWidth(character);
        return primary;
      } catch (final IllegalArgumentException missing) {
        try {
          this.math.getStringWidth(character);
          return this.math;
        } catch (final IllegalArgumentException unsupported) {
          throw new IOException(
              "Шрифт PDF не поддерживает символ U+"
                  + Integer.toHexString(character.codePointAt(0)).toUpperCase(Locale.ROOT)
                  + ". Исправьте текст перед экспортом.");
        }
      }
    }

    float width(final String text, final float size, final boolean bold) throws IOException {
      float width = 0;
      for (int offset = 0; offset < text.length(); ) {
        final int cp = text.codePointAt(offset);
        offset += Character.charCount(cp);
        final String c = new String(Character.toChars(cp));
        width += this.glyph(c, bold).getStringWidth(c) * size / 1000;
      }
      return width;
    }

    List<String> wrap(final String input, final float width, final float size, final boolean bold)
        throws IOException {
      final List<String> lines = new ArrayList<>();
      final String normalized = input.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');
      for (final String paragraph : normalized.split("\n", -1)) {
        final StringBuilder line = new StringBuilder();
        float used = 0;
        int breakAt = -1;
        for (int offset = 0; offset < paragraph.length(); ) {
          interrupted();
          final int cp = paragraph.codePointAt(offset);
          offset += Character.charCount(cp);
          final String character = new String(Character.toChars(cp));
          final float next = this.width(character, size, bold);
          if (used + next > width && !line.isEmpty()) {
            if (breakAt > 0) {
              lines.add(line.substring(0, breakAt).stripTrailing());
              final String tail = line.substring(breakAt).stripLeading();
              line.setLength(0);
              line.append(tail);
              used = this.width(tail, size, bold);
            } else {
              lines.add(line.toString());
              line.setLength(0);
              used = 0;
            }
            breakAt = -1;
          }
          if (cp == ' ' || cp == '-' || cp == ',') {
            breakAt = line.length() + 1;
          }
          line.append(character);
          used += next;
        }
        lines.add(line.toString());
      }
      return lines;
    }

    void paragraph(final String text, final float size, final boolean bold, final boolean muted)
        throws IOException {
      final float leading = size * 1.45f;
      for (final var line : this.wrap(text, WIDTH, size, bold)) {
        this.ensure(leading);
        this.text(line, 42, this.y - size, size, bold, muted);
        this.y -= leading;
      }
      this.y -= 7;
    }

    void figure(final byte[] bytes, final String caption) throws IOException {
      if (bytes.length > 16 * 1024 * 1024) {
        throw new IOException("Слишком большая схема отчёта.");
      }
      final var image = PDImageXObject.createFromByteArray(this.pdf, bytes, "scheme");
      final float scale = Math.min(WIDTH / image.getWidth(), 380f / image.getHeight());
      final float width = image.getWidth() * scale, height = image.getHeight() * scale;
      this.ensure(height + (caption.isBlank() ? 10 : 42));
      this.stream.drawImage(image, 42 + (WIDTH - width) / 2, this.y - height, width, height);
      this.y -= height + 5;
      if (!caption.isBlank()) {
        this.paragraph(caption, 8, false, true);
      }
    }

    void table(final Table table) throws IOException {
      if (table.rows().isEmpty()) {
        this.paragraph("Нет записей.", 9, false, true);
        return;
      }
      final int count = table.headers().size();
      final float[] widths = new float[count];
      for (int i = 0; i < count; i++) {
        widths[i] = count == 1 ? WIDTH : i == 0 ? WIDTH * .30f : WIDTH * .70f / (count - 1);
      }
      if (count == 3 && table.headers().getLast().equals("SHA-256")) {
        widths[0] = 180;
        widths[1] = 60;
        widths[2] = WIDTH - 240;
      }
      if (table.headers().getFirst().equals("Вещество") && count > 1) {
        widths[0] = WIDTH * .51f;
        for (int i = 1; i < count; i++) {
          widths[i] = WIDTH * .49f / (count - 1);
        }
      }
      this.ensure(76);
      this.tableRow(table.headers(), widths, true, null);
      for (final var row : table.rows()) {
        this.tableRow(row, widths, false, table.headers());
      }
      this.y -= 10;
    }

    void tableRow(
        final List<String> cells,
        final float[] widths,
        final boolean header,
        final List<String> repeated)
        throws IOException {
      final List<List<String>> lines = new ArrayList<>();
      int total = 0;
      for (int i = 0; i < cells.size(); i++) {
        final var wrapped = this.wrap(cells.get(i), widths[i] - 12, 8, header);
        lines.add(wrapped);
        total = Math.max(total, wrapped.size());
      }
      int start = 0;
      while (start < total) {
        interrupted();
        int capacity = (int) ((this.y - BOTTOM - 12) / 12);
        if (capacity < Math.min(2, total - start)) {
          this.newPage();
          if (!header && repeated != null) {
            this.tableRow(repeated, widths, true, null);
          }
          capacity = (int) ((this.y - BOTTOM - 12) / 12);
        }
        final int take = Math.min(capacity, total - start);
        final float height = take * 12 + 12;
        if (header) {
          this.stream.setNonStrokingColor(.91f, .94f, .97f);
          this.stream.addRect(42, this.y - height, WIDTH, height);
          this.stream.fill();
        }
        float x = 42;
        for (int col = 0; col < cells.size(); col++) {
          for (int line = 0; line < take && start + line < lines.get(col).size(); line++) {
            this.text(
                lines.get(col).get(start + line), x + 6, this.y - 14 - line * 12, 8, header, false);
          }
          x += widths[col];
        }
        this.stream.setStrokingColor(.83f, .87f, .91f);
        this.stream.setLineWidth(.4f);
        this.stream.moveTo(42, this.y - height);
        this.stream.lineTo(553, this.y - height);
        this.stream.stroke();
        this.y -= height;
        start += take;
      }
    }
  }

  private static String reportDate(final Instant at) {
    return java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu")
        .withZone(java.time.ZoneId.systemDefault())
        .format(at);
  }

  private static String html(
      final Document report, final Map<String, byte[]> figures, final String hash, final Instant at)
      throws IOException {
    final StringBuilder out =
        new StringBuilder(
            "<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'\"><title>");
    out.append(escape(report.title()))
        .append(
            "</title><style>body{font:15px/1.55 Arial,sans-serif;color:#17293d;background:#edf1f5;margin:0}main{max-width:960px;margin:28px auto;background:white;padding:48px}h1{font-size:30px;line-height:1.2}h2{border-bottom:1px solid #d5dfe9;padding-bottom:8px;margin-top:32px}h3{margin-top:22px}p{white-space:pre-wrap;overflow-wrap:anywhere}.muted,figcaption{font-size:12px;color:#596b7d}table{border-collapse:collapse;width:100%;font-size:12px;table-layout:fixed}th,td{padding:9px 7px;border-bottom:1px solid #d5dfe9;text-align:left;vertical-align:top;overflow-wrap:anywhere;white-space:pre-wrap}th{background:#eaf0f7}th:first-child{width:32%}table.quantities th:first-child{width:49%}figure{margin:20px 0;break-inside:avoid}img{max-width:100%;max-height:620px;display:block;margin:auto}footer{border-top:1px solid #ddd;margin-top:32px}@media(max-width:650px){main{padding:20px;margin:0}table{font-size:10px}th,td{padding:5px 3px}}@page{size:A4;margin:16mm}@media print{body{background:white}main{margin:0;padding:0}thead{display:table-header-group}tr{break-inside:avoid}h2,h3{break-after:avoid}img{max-height:150mm}}</style></head><body><main><p class=\"muted\">ЛАБОРАТОРНЫЙ ОТЧЁТ</p><h1>")
        .append(escape(report.title()))
        .append("</h1><p class=\"muted\">Дата отчёта: ")
        .append(reportDate(at))
        .append("</p>");
    for (final var block : report.blocks()) {
      interrupted();
      switch (block) {
        case Heading h ->
            out.append(h.level() == 1 ? "<h2>" : "<h3>")
                .append(escape(h.text()))
                .append(h.level() == 1 ? "</h2>" : "</h3>");
        case Paragraph p ->
            out.append(p.subdued() ? "<p class=\"muted\">" : "<p>")
                .append(escape(p.text()))
                .append("</p>");
        case Figure f ->
            out.append("<figure><img alt=\"")
                .append(escape(f.caption()))
                .append("\" src=\"data:image/png;base64,")
                .append(Base64.getEncoder().encodeToString(figures.get(f.key())))
                .append("\"><figcaption>")
                .append(escape(f.caption()))
                .append("</figcaption></figure>");
        case Table t -> {
          out.append(
              t.headers().getFirst().equals("Вещество")
                  ? "<table class=\"quantities\"><thead><tr>"
                  : "<table><thead><tr>");
          t.headers().forEach(h -> out.append("<th>").append(escape(h)).append("</th>"));
          out.append("</tr></thead><tbody>");
          for (final var row : t.rows()) {
            out.append("<tr>");
            row.forEach(cell -> out.append("<td>").append(escape(cell)).append("</td>"));
            out.append("</tr>");
          }
          out.append("</tbody></table>");
        }
      }
    }
    return out.append(
            "<footer><p class=\"muted\">Quantum IR Studio</p></footer><!-- QCHEM SHA-256: ")
        .append(hash)
        .append(" --></main></body></html>")
        .toString();
  }

  private static String escape(final String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
