/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

/** Public releases only. No account credentials, experiment data or telemetry are sent. */
public final class StudioUpdateService {
  public static final URI RELEASES = URI.create("https://github.com/VadimKrut/quantum/releases");
  private static final URI LATEST =
      URI.create("https://api.github.com/repos/VadimKrut/quantum/releases/latest");
  private static final long MAX_INSTALLER = 1024L * 1024 * 1024;
  private final Source source;

  @FunctionalInterface
  interface Source {
    InputStream open(URI uri) throws IOException;
  }

  public record Release(
      String version,
      String notes,
      URI page,
      URI download,
      String filename,
      long bytes,
      String sha256) {}

  public StudioUpdateService() {
    this(StudioUpdateService::open);
  }

  StudioUpdateService(Source source) {
    this.source = source;
  }

  public static String currentVersion() {
    String configured = System.getProperty("quantum.studio.version", "");
    if (validVersion(configured)) return configured;
    String packaged = StudioUpdateService.class.getPackage().getImplementationVersion();
    return validVersion(packaged) ? packaged : "локальная сборка";
  }

  public static boolean validVersion(String version) {
    return version != null
        && version.matches("(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})");
  }

  public static int compareVersions(String left, String right) {
    if (!validVersion(left) || !validVersion(right))
      throw new IllegalArgumentException("Некорректная версия.");
    String[] a = left.split("\\."), b = right.split("\\.");
    for (int i = 0; i < 3; i++) {
      int compared = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
      if (compared != 0) return compared;
    }
    return 0;
  }

  public Optional<Release> latest(String current) throws IOException {
    try (var input = this.source.open(LATEST)) {
      byte[] body = input.readNBytes(2 * 1024 * 1024 + 1);
      if (body.length > 2 * 1024 * 1024)
        throw new IOException("Ответ сервера обновлений слишком большой.");
      return parseRelease(new ObjectMapper().readTree(body), current);
    }
  }

  static Optional<Release> parseRelease(JsonNode json, String current) throws IOException {
    String tag = json.path("tag_name").asText();
    String version = tag.startsWith("v") ? tag.substring(1) : tag;
    if (json.path("draft").asBoolean() || json.path("prerelease").asBoolean())
      return Optional.empty();
    if (!validVersion(version)) throw new IOException("Сервер вернул неизвестный формат версии.");
    if (validVersion(current) && compareVersions(version, current) <= 0) return Optional.empty();
    String prefix = "quantum-ir-studio-" + version + "-windows";
    for (var asset : json.path("assets")) {
      String name = asset.path("name").asText();
      if (!(name.equals(prefix + "-x64.msi") || name.equals(prefix + ".msi"))) continue;
      if (!"uploaded".equals(asset.path("state").asText())) continue;
      long bytes = asset.path("size").asLong();
      String digest = asset.path("digest").asText();
      if (bytes <= 0 || bytes > MAX_INSTALLER || !digest.matches("sha256:[a-fA-F0-9]{64}")) {
        throw new IOException(
            "Установщик ещё не готов или не содержит контрольную сумму SHA-256. Повторите проверку позже.");
      }
      URI download;
      try {
        download = URI.create(asset.path("browser_download_url").asText());
      } catch (IllegalArgumentException e) {
        throw new IOException("Некорректная ссылка установщика.", e);
      }
      String expected =
          "https://github.com/VadimKrut/quantum/releases/download/" + tag + "/" + name;
      if (!expected.equals(download.toString()))
        throw new IOException("Установщик должен принадлежать официальному релизу Quantum.");
      return Optional.of(
          new Release(
              version,
              json.path("body").asText(""),
              URI.create(RELEASES + "/tag/" + tag),
              download,
              name,
              bytes,
              digest.substring(7).toLowerCase(Locale.ROOT)));
    }
    throw new IOException(
        "В релизе " + version + " ещё нет установщика Windows x64. Повторите проверку позже.");
  }

  public Path download(
      Release release, Path directory, LongConsumer progress, BooleanSupplier cancelled)
      throws IOException {
    Files.createDirectories(directory);
    Path temporary = Files.createTempFile(directory, "quantum-update-", ".part");
    Path destination = directory.resolve(release.filename());
    try {
      MessageDigest digest = digest();
      long count = 0;
      try (var input = this.source.open(release.download());
          var output = Files.newOutputStream(temporary)) {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
          if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Скачивание отменено.");
          count += read;
          if (count > release.bytes())
            throw new IOException("Размер установщика не совпадает с опубликованным.");
          output.write(buffer, 0, read);
          digest.update(buffer, 0, read);
          progress.accept(count);
        }
      }
      if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
        throw new InterruptedIOException("Скачивание отменено.");
      if (count != release.bytes()
          || !HexFormat.of().formatHex(digest.digest()).equals(release.sha256())) {
        throw new IOException("Проверка установщика не пройдена. Скачайте обновление ещё раз.");
      }
      Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      return destination;
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public static void verify(Path file, long bytes, String expected) throws IOException {
    if (Files.size(file) != bytes) throw new IOException("Изменился размер установщика.");
    MessageDigest digest = digest();
    try (var input = Files.newInputStream(file)) {
      byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
    }
    if (!HexFormat.of().formatHex(digest.digest()).equals(expected))
      throw new IOException("Изменилась контрольная сумма установщика.");
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static InputStream open(URI uri) throws IOException {
    for (int redirects = 0; redirects < 6; redirects++) {
      if (!"https".equals(uri.getScheme())
          || uri.getUserInfo() != null
          || uri.getPort() != -1
          || !Set.of(
                  "api.github.com",
                  "github.com",
                  "release-assets.githubusercontent.com",
                  "objects.githubusercontent.com")
              .contains(uri.getHost())) {
        throw new IOException("Недопустимый адрес сервера обновлений.");
      }
      var connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(20000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("User-Agent", "Quantum-IR-Studio-Updater");
      connection.setRequestProperty(
          "Accept",
          uri.getHost().equals("api.github.com")
              ? "application/vnd.github+json"
              : "application/octet-stream");
      final int status;
      try {
        status = connection.getResponseCode();
      } catch (IOException e) {
        connection.disconnect();
        throw e;
      }
      if (Set.of(301, 302, 303, 307, 308).contains(status)) {
        String location = connection.getHeaderField("Location");
        connection.disconnect();
        if (location == null) throw new IOException("Сервер вернул пустое перенаправление.");
        uri = uri.resolve(location);
        continue;
      }
      if (status != 200) {
        connection.disconnect();
        throw new IOException(
            status == 403 || status == 429
                ? "GitHub временно ограничил проверки. Повторите позже."
                : "Сервер обновлений недоступен (HTTP " + status + ").");
      }
      return new FilterInputStream(connection.getInputStream()) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } finally {
            connection.disconnect();
          }
        }
      };
    }
    throw new IOException("Слишком много перенаправлений сервера обновлений.");
  }
}
