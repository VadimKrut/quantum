/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.update;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Deterministic update checks, with an optional read-only query to the real release endpoint. */
public final class StudioUpdateAudit {
  private static int checks;
  private static final byte[] INSTALLER =
      "synthetic installer bytes; never executable"
          .getBytes(java.nio.charset.StandardCharsets.UTF_8);
  private static final ObjectMapper JSON = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    check(StudioUpdateService.compareVersions("1.0.172", "1.0.99") > 0, "numeric version ordering");
    check(StudioUpdateService.compareVersions("2.0.0", "1.99.999") > 0, "major version ordering");
    check(
        !StudioUpdateService.validVersion("1.0.172-beta")
            && !StudioUpdateService.validVersion("01.0.1"),
        "only stable numeric versions");
    var json = releaseJson();
    check(StudioUpdateService.parseRelease(json, "1.0.171").isPresent(), "newer release detected");
    check(StudioUpdateService.parseRelease(json, "1.0.172").isEmpty(), "same release ignored");
    check(StudioUpdateService.parseRelease(json, "1.0.173").isEmpty(), "downgrade ignored");
    check(
        StudioUpdateService.parseRelease(json.deepCopy().put("draft", true), "1.0.171").isEmpty(),
        "draft ignored");
    check(
        StudioUpdateService.parseRelease(json.deepCopy().put("prerelease", true), "1.0.171")
            .isEmpty(),
        "prerelease ignored");
    var bad = json.deepCopy();
    asset(bad).put("browser_download_url", "https://example.com/installer.msi");
    fails(() -> StudioUpdateService.parseRelease(bad, "1.0.171"), "foreign installer rejected");
    var noDigest = json.deepCopy();
    asset(noDigest).remove("digest");
    fails(() -> StudioUpdateService.parseRelease(noDigest, "1.0.171"), "missing checksum rejected");
    var empty = json.deepCopy();
    empty.putArray("assets");
    fails(() -> StudioUpdateService.parseRelease(empty, "1.0.171"), "incomplete release handled");
    var wrongArchitecture = json.deepCopy();
    asset(wrongArchitecture).put("name", "quantum-ir-studio-1.0.172-windows-arm64.msi");
    fails(
        () -> StudioUpdateService.parseRelease(wrongArchitecture, "1.0.171"),
        "wrong architecture rejected");
    var ci = json.deepCopy();
    asset(ci).put("name", "quantum-ir-studio-1.0.172-windows.msi");
    asset(ci)
        .put(
            "browser_download_url",
            "https://github.com/VadimKrut/quantum/releases/download/v1.0.172/quantum-ir-studio-1.0.172-windows.msi");
    check(
        StudioUpdateService.parseRelease(ci, "1.0.171").isPresent(),
        "CI and local packaging names supported");
    var service =
        new StudioUpdateService(
            uri ->
                new ByteArrayInputStream(
                    uri.getHost().equals("api.github.com")
                        ? JSON.writeValueAsBytes(json)
                        : INSTALLER));
    var release = service.latest("1.0.171").orElseThrow();
    Path directory = Files.createTempDirectory("quantum-update-audit-");
    var count = new AtomicLong();
    Path file = service.download(release, directory, count::set, () -> false);
    check(
        Arrays.equals(Files.readAllBytes(file), INSTALLER) && count.get() == INSTALLER.length,
        "download and progress");
    StudioUpdateService.verify(file, release.bytes(), release.sha256());
    check(true, "verification before installer start");
    var corrupt =
        new StudioUpdateService(uri -> new ByteArrayInputStream(new byte[INSTALLER.length]));
    fails(
        () -> corrupt.download(release, directory, n -> {}, () -> false),
        "checksum mismatch rejected");
    check(
        Arrays.equals(Files.readAllBytes(file), INSTALLER),
        "failed download preserves verified installer");
    var truncated = new StudioUpdateService(uri -> new ByteArrayInputStream(new byte[2]));
    fails(
        () -> truncated.download(release, directory, n -> {}, () -> false),
        "truncated download rejected");
    var oversized =
        new StudioUpdateService(uri -> new ByteArrayInputStream(new byte[INSTALLER.length + 1]));
    fails(
        () -> oversized.download(release, directory, n -> {}, () -> false),
        "oversized download rejected");
    fails(() -> service.download(release, directory, n -> {}, () -> true), "cancellation");
    try (var files = Files.list(directory)) {
      check(files.noneMatch(p -> p.toString().endsWith(".part")), "partial files cleaned up");
    }
    var offline =
        new StudioUpdateService(
            uri -> {
              throw new java.net.SocketTimeoutException("offline");
            });
    fails(() -> offline.latest("1.0.171"), "offline and timeouts handled");
    Files.write(file, new byte[INSTALLER.length]);
    fails(
        () -> StudioUpdateService.verify(file, release.bytes(), release.sha256()),
        "installer changed after download rejected");
    if (Arrays.asList(args).contains("--live")) {
      var live = new StudioUpdateService().latest("0.0.0").orElseThrow();
      check(
          live.bytes() > 0 && live.sha256().length() == 64,
          "live GitHub metadata: " + live.version() + " / " + live.filename());
    }
    System.out.println("UPDATE AUDIT: " + checks + " checks passed; no installer launched");
  }

  private static ObjectNode releaseJson() throws Exception {
    var json = JSON.createObjectNode();
    json.put("tag_name", "v1.0.172")
        .put("body", "Synthetic release fixture")
        .put("draft", false)
        .put("prerelease", false);
    json.putArray("assets")
        .addObject()
        .put("name", "quantum-ir-studio-1.0.172-windows-x64.msi")
        .put("state", "uploaded")
        .put("size", INSTALLER.length)
        .put(
            "digest",
            "sha256:"
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(INSTALLER)))
        .put(
            "browser_download_url",
            "https://github.com/VadimKrut/quantum/releases/download/v1.0.172/quantum-ir-studio-1.0.172-windows-x64.msi");
    return json;
  }

  private static ObjectNode asset(ObjectNode json) {
    return (ObjectNode) json.path("assets").get(0);
  }

  @FunctionalInterface
  private interface Action {
    void run() throws Exception;
  }

  private static void fails(Action action, String label) throws Exception {
    try {
      action.run();
    } catch (IOException e) {
      check(true, label);
      return;
    }
    throw new AssertionError(label);
  }

  private static void check(boolean passed, String label) {
    if (!passed) throw new AssertionError(label);
    checks++;
    System.out.println("PASS " + label);
  }
}
