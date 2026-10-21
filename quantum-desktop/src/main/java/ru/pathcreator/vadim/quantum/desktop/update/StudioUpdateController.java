/* Copyright 2026 Vadim Aleksandrovich Zaletaev. SPDX-License-Identifier: MPL-2.0 */
package ru.pathcreator.vadim.quantum.desktop.update;

import java.nio.file.*;
import java.util.Optional;
import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryViewPreferences;

/** Non-modal update panel; networking and verification never run on the FX thread. */
public final class StudioUpdateController {
  private final StudioUpdateService service = new StudioUpdateService();
  private final LaboratoryViewPreferences preferences;
  private final String current = StudioUpdateService.currentVersion();
  private final Button entry = new Button("Обновления");
  private final Label status = new Label("Нажмите «Проверить сейчас»."),
      version = new Label("Установлена: " + current);
  private final Button check = new Button("Проверить сейчас"),
      download = new Button("Скачать обновление"),
      install = new Button("Установить и закрыть Studio"),
      cancel = new Button("Отменить скачивание");
  private final TextArea notes = new TextArea();
  private final ProgressBar progress = new ProgressBar();
  private final Timeline schedule =
      new Timeline(new KeyFrame(Duration.hours(6), e -> this.check(false)));
  private Task<?> operation;
  private StudioUpdateService.Release release;
  private Path installer;
  private Window owner;
  private Dialog<Void> dialog;
  private boolean started;

  public StudioUpdateController(LaboratoryViewPreferences preferences) {
    this.preferences = preferences;
    this.entry.setId("studio-updates");
    this.entry.setOnAction(e -> this.show());
    this.entry.setTooltip(
        new Tooltip("Проверка, скачивание и установка обновлений Quantum IR Studio"));
    this.check.setOnAction(e -> this.check(true));
    this.download.setOnAction(e -> this.download());
    this.install.setOnAction(e -> this.install());
    this.cancel.setOnAction(
        e -> {
          if (this.operation != null) this.operation.cancel(true);
        });
    this.schedule.setCycleCount(Timeline.INDEFINITE);
  }

  public Button button() {
    return this.entry;
  }

  public void attach(Window owner) {
    if (this.owner != null) return;
    this.owner = owner;
    owner
        .showingProperty()
        .addListener(
            (o, before, showing) -> {
              if (showing) this.start();
              else {
                this.schedule.stop();
                if (this.operation != null) this.operation.cancel(true);
              }
            });
    if (owner.isShowing()) this.start();
  }

  private void start() {
    if (this.started) return;
    this.started = true;
    if (this.preferences.flag("updates.automatic", true)) this.check(false);
    this.schedule.play();
  }

  private void show() {
    if (this.dialog == null) {
      this.dialog = new Dialog<>();
      this.dialog.initOwner(this.owner);
      this.dialog.initModality(Modality.NONE);
      this.dialog.setTitle("Обновления Quantum IR Studio");
      this.dialog.setResizable(true);
      this.dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
      var automatic =
          new CheckBox("Автоматически проверять обновления при запуске и каждые 6 часов");
      automatic.setSelected(this.preferences.flag("updates.automatic", true));
      automatic.setOnAction(
          e -> {
            this.preferences.set("updates.automatic", automatic.isSelected());
            this.preferences.save();
            if (automatic.isSelected()) this.check(false);
          });
      this.status.setWrapText(true);
      this.notes.setEditable(false);
      this.notes.setWrapText(true);
      this.notes.setPrefRowCount(12);
      this.notes.setPromptText("Описание новой версии появится после проверки.");
      this.progress.setMaxWidth(Double.MAX_VALUE);
      var body =
          new VBox(
              12,
              this.version,
              automatic,
              this.status,
              this.progress,
              new FlowPane(8, 8, this.check, this.download, this.cancel, this.install),
              this.notes);
      body.setPrefWidth(660);
      body.setPadding(new Insets(16));
      this.dialog.getDialogPane().setContent(body);
    }
    this.controls();
    this.dialog.show();
  }

  private void controls() {
    boolean busy = this.operation != null;
    this.check.setDisable(busy);
    this.download.setDisable(busy || this.release == null || !windowsX64());
    this.install.setDisable(
        busy
            || this.installer == null
            || !StudioUpdateService.validVersion(this.current)
            || !windowsX64());
    this.cancel.setVisible(busy && this.operation.getTitle().equals("download"));
    this.cancel.setManaged(this.cancel.isVisible());
    this.progress.setVisible(busy);
    this.progress.setManaged(busy);
  }

  private static boolean windowsX64() {
    return System.getProperty("os.name", "").startsWith("Windows")
        && java.util.Set.of("amd64", "x86_64").contains(System.getProperty("os.arch", ""));
  }

  private void check(boolean manual) {
    if (this.operation != null || !manual && !this.preferences.flag("updates.automatic", true))
      return;
    if (!manual && !StudioUpdateService.validVersion(this.current)) return;
    this.status.setText("Проверяю опубликованные версии…");
    var task =
        new Task<Optional<StudioUpdateService.Release>>() {
          @Override
          protected Optional<StudioUpdateService.Release> call() throws Exception {
            return service.latest(current);
          }
        };
    task.setOnSucceeded(
        e -> {
          var found = task.getValue().orElse(null);
          if (!java.util.Objects.equals(found, this.release)) this.installer = null;
          this.release = found;
          this.operation = null;
          if (found == null) {
            this.entry.setText("Обновления");
            this.status.setText("Установлена актуальная версия " + this.current + ".");
            this.notes.clear();
          } else {
            this.entry.setText("Доступна версия " + found.version());
            this.status.setText(
                "Доступна версия "
                    + found.version()
                    + " · "
                    + String.format(java.util.Locale.ROOT, "%.1f", found.bytes() / 1048576.0)
                    + " МБ."
                    + (!StudioUpdateService.validVersion(this.current)
                        ? " Запущена локальная сборка: сравнение версий и установка отключены."
                        : "")
                    + (!windowsX64() ? " Установка из программы доступна для Windows x64." : ""));
            this.notes.setText(found.notes());
          }
          this.controls();
        });
    this.start(task);
  }

  private void download() {
    if (this.operation != null || this.release == null) return;
    final var selected = this.release;
    this.installer = null;
    this.status.setText("Скачиваю обновление. Можно продолжать работу с опытом.");
    var task =
        new Task<Path>() {
          @Override
          protected Path call() throws Exception {
            updateTitle("download");
            return service.download(
                selected,
                Path.of(System.getProperty("user.home"), ".quantum", "updates"),
                count -> updateProgress(count, selected.bytes()),
                this::isCancelled);
          }
        };
    task.setOnSucceeded(
        e -> {
          this.installer = task.getValue();
          this.operation = null;
          this.status.setText(
              "Обновление скачано, SHA-256 проверена. Перед установкой Studio предложит сохранить незавершённую работу.");
          this.controls();
        });
    task.titleProperty().addListener((o, a, b) -> this.controls());
    this.start(task);
  }

  private void start(Task<?> task) {
    this.operation = task;
    this.progress.progressProperty().unbind();
    this.progress.progressProperty().bind(task.progressProperty());
    task.setOnFailed(
        e -> {
          this.operation = null;
          this.status.setText(
              "Не удалось выполнить обновление: "
                  + java.util.Objects.toString(task.getException().getMessage(), "нет соединения")
                  + " Можно повторить позже; работа офлайн доступна.");
          this.controls();
        });
    task.setOnCancelled(
        e -> {
          this.operation = null;
          this.status.setText("Скачивание отменено. Можно запустить его снова.");
          this.controls();
        });
    this.controls();
    Thread.ofPlatform().daemon().name("studio-update").start(task);
  }

  private void install() {
    if (this.operation != null || this.installer == null || this.release == null) return;
    final var file = this.installer;
    final var selected = this.release;
    this.status.setText("Проверяю установщик перед запуском…");
    var task =
        new Task<ProcessBuilder>() {
          @Override
          protected ProcessBuilder call() throws Exception {
            StudioUpdateService.verify(file, selected.bytes(), selected.sha256());
            Path java = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
            Path jar =
                Path.of(
                    StudioUpdateInstaller.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI());
            return new ProcessBuilder(
                java.toString(),
                "-cp",
                jar.toString(),
                StudioUpdateInstaller.class.getName(),
                Long.toString(ProcessHandle.current().pid()),
                file.toString(),
                Long.toString(selected.bytes()),
                selected.sha256());
          }
        };
    task.setOnSucceeded(
        e -> {
          this.operation = null;
          this.controls();
          this.dialog.hide();
          this.owner.fireEvent(new WindowEvent(this.owner, WindowEvent.WINDOW_CLOSE_REQUEST));
          if (this.owner.isShowing()) {
            this.status.setText(
                "Закрытие отменено. Примените или сохраните ввод и повторите установку.");
            this.dialog.show();
            return;
          }
          try {
            task.getValue().start();
          } catch (java.io.IOException failure) {
            var message =
                new Alert(
                    Alert.AlertType.ERROR,
                    "Не удалось запустить обновление. Установщик сохранён: "
                        + file
                        + "\n"
                        + failure.getMessage(),
                    ButtonType.OK);
            message.showAndWait();
          }
        });
    this.start(task);
  }
}
