/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.update;

import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** Small detached helper: wait until the application releases its JAR before starting the MSI. */
public final class StudioUpdateInstaller {
  private StudioUpdateInstaller() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4 || !System.getProperty("os.name", "").startsWith("Windows")) return;
    Path installer = Path.of(args[1]).toAbsolutePath();
    try {
      var parent = ProcessHandle.of(Long.parseLong(args[0]));
      if (parent.isPresent()) parent.get().onExit().get(120, TimeUnit.SECONDS);
      StudioUpdateService.verify(installer, Long.parseLong(args[2]), args[3]);
      new ProcessBuilder("msiexec.exe", "/i", installer.toString(), "/norestart").start();
    } catch (Exception e) {
      Files.writeString(installer.resolveSibling("installation-error.txt"), e.toString());
      throw e;
    }
  }
}
